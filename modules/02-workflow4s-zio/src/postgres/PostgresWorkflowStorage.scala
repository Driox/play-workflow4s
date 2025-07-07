package workflows4s.anorm.postgres

import anorm.*
import anorm.SqlParser.*
import play.api.Logging
import play.api.db.Database
import workflows4s.anorm.{ ByteCodec, WorkflowStorage }

import zio.*
import zio.stream.ZStream

class PostgresWorkflowStorage[Event](
  database:  Database,
  tableName: String = "workflow_journal"
)(using evenCodec: ByteCodec[Event])
  extends WorkflowStorage[WorkflowId, Event] with AnormZio with Logging {

  override def getEvents(id: WorkflowId): ZStream[Database, Throwable, Event] = ZStream.unwrap {
    tzio { implicit c =>
      val rows: Iterator[Array[Byte]] =
        SQL"SELECT event_data FROM #${tableName} WHERE workflow_id = ${id.value}"
          .as(scalar[Array[Byte]].*)
          .iterator

      ZStream.fromIterator(rows)
        .map(bytes => evenCodec.read(IArray.unsafeFromArray(bytes)))
        .mapZIO {
          _.fold(
            err => ZIO.fail(err),
            result => ZIO.succeed(result)
          )
        }
    }
  }

  override def saveEvent(id: WorkflowId, event: Event): ZIO[Database, Throwable, Unit] = tzio { implicit c =>
    val bytes = IArray.genericWrapArray(evenCodec.write(event)).toArray
    SQL"INSERT INTO #${tableName} (workflow_id, event_data) VALUES (${id.value}, $bytes)".executeInsert()
  }.map(_ => ())

  override def lockWorkflow(id: WorkflowId): ZIO[Scope, Throwable, Unit] = {

    /**
     * `pg_try_advisory_xact_lock` is a lock available at the transaction level.
     * It is released when the transaction is committed or rolled back.
     *
     * Query may be
     *   `SQL"select pg_try_advisory_xact_lock(${id.value})".as(scalar[Boolean].single)``
     */
    def acquire(id: WorkflowId) = tzio { implicit c =>
      SQL"SELECT pg_advisory_lock(${id.value})".as(scalar[Boolean].single)
    }
      .flatMap {
        case true  => ZIO.unit
        case false => ZIO.fail(new Exception(s"Couldn't acquire lock for ${id}"))
      }
      .provide(ZLayer.succeed(database))

    def release(id: WorkflowId): ZIO[Scope, Nothing, Any] = tzio { implicit c =>
      val result = SQL"SELECT pg_advisory_unlock(${id.value})".as(scalar[Boolean].single)

      if (!result) {
        logger.warn(s"No lock to release for ${id}")
      } else {
        logger.info(s"Lock released for ${id}")
      }
      ()
    }.catchAll {
      case t: Throwable => {
        logger.error(s"Error releasing lock for ${id}", t)
        ZIO.unit
      }
    }
      .provide(ZLayer.succeed(database))

    ZIO.acquireRelease(acquire(id))(_ => release(id))
  }
}
