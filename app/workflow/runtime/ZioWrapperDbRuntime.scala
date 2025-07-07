package workflow.runtime

import cats.effect.IO
import doobie.util.transactor.Transactor
import effect.sorus.*
import play.api.Logging
import workflow.domain.*
import workflow.domain.KycWorkflow.*
import workflows4s.doobie.postgres.{ PostgresWorkflowStorage, WorkflowId }
import workflows4s.doobie.{ ByteCodec, DatabaseRuntime, WorkflowStorage }
import workflows4s.runtime.wakeup.{ KnockerUpper, NoOpKnockerUpper }
import workflows4s.wio.SignalDef

import scala.util.{ Failure, Success, Try }

import zio.*
import zio.interop.catz.*
import zio.json.*
import workflows4s.runtime.WorkflowInstance
import workflows4s.wio.model.WIOExecutionProgress
import play.api.Configuration
import javax.inject.*

/**
 * Main goal : encapsulate the mapping Cats <> ZIO
 *
 * The main issue is createInstance leak an IO, hence we need to write custom getProgress / deliverSignal
 */
@Singleton
class ZioWrapperDbRuntime @Inject() (config: Configuration) extends ZioSorus with Logging {

  // Cats <> ZIO interrop
  import cats.effect.unsafe.IORuntime
  given cats_runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  given eventCodec: ByteCodec[KycEvent] = new ByteCodec[KycEvent] {

    def read(bytes: IArray[Byte]): Try[KycEvent] = {
      val str = new String(IArray.genericWrapArray(bytes).toArray, "UTF-8")
      str.fromJson[KycEvent].fold(
        err => Failure(new Exception(err.toString)),
        success => Success(success)
      )
    }
    def write(event: KycEvent): IArray[Byte]     = {
      val json  = event.toJson
      val array = json.toString().getBytes("UTF-8")
      IArray.from(array)
    }
  }

  // WorkflowId is a Long => WorkflowId(1L)
  def getRuntime(): DatabaseRuntime[KycContext.Ctx, WorkflowId] = {
    val initialState: KycState                               = KycState.Empty
    val transactor: Transactor[IO]                           = Transactor.fromDriverManager[IO](
      driver     = config.get[String]("db.default.driver"),
      url        = config.get[String]("db.default.url"),
      user       = config.get[String]("db.default.username"),
      password   = config.get[String]("db.default.password"),
      logHandler = None
    )
    val storage: WorkflowStorage[WorkflowId, KycEvent]       = new PostgresWorkflowStorage[KycEvent]()(using eventCodec)
    val knockerUpper: KnockerUpper.Agent[WorkflowId]         = NoOpKnockerUpper.Agent
    val runtime: DatabaseRuntime[KycContext.Ctx, WorkflowId] = DatabaseRuntime.default(workflow, initialState, transactor, knockerUpper, storage)
    runtime
  }

  def createInstance(workflow_id: WorkflowId): ZIO[Any, Fail, WorkflowInstance[IO, KycWorkflow.KycContext.State]] = {
    val instanceIO =
      getRuntime()
        .createInstance(workflow_id)
        .onError {
          case t: Throwable => {
            logger.error("Error running IO", t)
            IO(())
          }
        }

    instanceIO.to[Task] ?|> "Error loading instance"
  }

  def getProgress(workflow_id: WorkflowId): ZIO[Any, Fail, WIOExecutionProgress[KycWorkflow.KycContext.State]] = {
    val progressIO =
      getRuntime()
        .createInstance(workflow_id)
        .flatMap(_.getProgress)
        .onError {
          case t: Throwable => {
            logger.error("Error running IO", t)
            IO(())
          }
        }

    progressIO.to[Task] ?|> "Error loading progress"
  }

  def deliverSignal[Req, Resp](
    workflow_id: WorkflowId,
    signalDef:   SignalDef[Req, Resp],
    req:         Req
  ): ZIO[Any, Fail, Either[WorkflowInstance.UnexpectedSignal, Resp]] = {
    val rez = getRuntime()
      .createInstance(workflow_id)
      .flatMap(instance => instance.deliverSignal(signalDef, req))
      .onError {
        case t: Throwable => {
          logger.error("Error running IO", t)
          IO(())
        }
      }

    rez.to[Task] ?|> "Error delivering signal"
  }
}
