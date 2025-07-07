package workflows4s.anorm.postgres.testing

import _root_.io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import cats.effect.IO
import cats.implicits.toTraverseOps
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import play.api.Logging
import play.api.db.{ Database, Databases }
import workflows4s.anorm.postgres.testing.CustomPostgresBinaryResolver

import scala.jdk.CollectionConverters.*

import org.scalatest.{ BeforeAndAfterAll, Suite }

// run before and after the whole test suite
// for a run per test, use BeforeAndAfterEach
trait PostgresServerEmbedded extends BeforeAndAfterAll with Logging { self: Suite =>

  private var server: EmbeddedPostgres = null

  protected val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver     = "org.postgresql.Driver",
    url        = "jdbc:postgresql://localhost:9033/postgres",
    user       = "postgres",
    password   = "postgres",
    logHandler = None
  )

  protected val db: Database = Databases(
    driver = "org.postgresql.Driver",
    url    = "jdbc:postgresql://localhost:9033/postgres",
    config = Map(
      "username" -> "postgres",
      "password" -> "postgres"
    )
  )

  protected override def beforeAll() = {
    super.beforeAll()

    val input            = "postgres-darwin-x86_64.txz"
    val resourceLocation = input.toLowerCase.replace(' ', '_').toLowerCase()
    val classLoader      = this.getClass().getClassLoader()
    val urls             = classLoader.getResources(resourceLocation).asScala.mkString("\n\n")

    server = EmbeddedPostgres.builder()
      .setPgBinaryResolver((system, machineHardware) => CustomPostgresBinaryResolver.INSTANCE.getPgBinary(system, machineHardware))
      .setPort(9033)
      .start()

    import cats.effect.unsafe.implicits.global
    createSchema(xa).unsafeRunSync()
  }

  protected override def afterAll() = {
    db.shutdown()
    if (server != null) server.close()
    super.afterAll()
  }

  private def createSchema(xa: Transactor[IO]): IO[Unit] = {
    val schemaSql  = scala.io.Source.fromResource("schema/postgres-schema.sql").mkString
    // Split the script into individual statements (if necessary) and execute them
    val statements = schemaSql.split(";").map(_.trim).filter(_.nonEmpty)
    val actions    = statements.toList.traverse(sql => Fragment.const(sql).update.run)
    actions.transact(xa).map(_ => ())
  }
}
