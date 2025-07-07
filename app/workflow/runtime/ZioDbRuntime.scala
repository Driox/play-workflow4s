package workflow.runtime

import effect.sorus.*
import play.api.Logging
import play.api.db.DBApi
import workflow.domain.*
import workflow.domain.KycWorkflow.*
import workflows4s.anorm.postgres.{ PostgresWorkflowStorage, WorkflowId }
import workflows4s.anorm.{ ByteCodec, DatabaseRuntime, WorkflowStorage }
import workflows4s.runtime.WorkflowInstance
import workflows4s.runtime.wakeup.{ KnockerUpper, NoOpKnockerUpper }
import workflows4s.wio.SignalDef
import workflows4s.wio.model.WIOExecutionProgress

import javax.inject.*
import scala.util.{ Failure, Success, Try }

import zio.*
import zio.json.*

/**
 * This looks like ZioWrapperDbRuntime but really it only wrap `ZIO[Any, Throwable, X]` into `ZIO[Any, Fail, X]`
 * If you keep Throwable as error channel, you don't need getProgress or deliverSignal
 */
@Singleton
class ZioDbRuntime @Inject() (dbapi: DBApi) extends ZioSorus with Logging {

  protected val db = dbapi.database("default")

  /**
   * To read it in DB
   * select event_id, workflow_id, encode(event_data, 'escape') from  workflow_journal
   */
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

  def getRuntime(): DatabaseRuntime[KycContext.Ctx, WorkflowId] = {
    val initialState: KycState                               = KycState.Empty
    val storage: WorkflowStorage[WorkflowId, KycEvent]       = new PostgresWorkflowStorage[KycEvent](db)(using eventCodec)
    val knockerUpper: KnockerUpper.Agent[WorkflowId]         = NoOpKnockerUpper.Agent
    val runtime: DatabaseRuntime[KycContext.Ctx, WorkflowId] = DatabaseRuntime.default(workflow, initialState, db, knockerUpper, storage)
    runtime
  }

  def createInstance(workflow_id: WorkflowId): ZIO[Any, Fail, WorkflowInstance[Task, KycWorkflow.KycContext.State]] = {
    getRuntime().createInstance(workflow_id) ?|> "Error loading instance"
  }

  def getProgress(workflow_id: WorkflowId): ZIO[Any, Fail, WIOExecutionProgress[KycWorkflow.KycContext.State]] = {
    getRuntime()
      .createInstance(workflow_id)
      .flatMap(_.getProgress) ?|> "Error loading progress"
  }

  def deliverSignal[Req, Resp](
    workflow_id: WorkflowId,
    signalDef:   SignalDef[Req, Resp],
    req:         Req
  ): ZIO[Any, Fail, Either[WorkflowInstance.UnexpectedSignal, Resp]] = {
    getRuntime()
      .createInstance(workflow_id)
      .flatMap(_.deliverSignal(signalDef, req)) ?|> "Error delivering signal"
  }
}
