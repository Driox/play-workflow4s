package workflow

import workflow.domain.*
import workflow.domain.KycWorkflow.*

import effect.sorus.*
import play.api.Logging
import play.api.db.DBApi
import play.twirl.api.Html
import utils.{ NumberUtils, StringUtils }
import workflows4s.anorm.postgres.{ PostgresWorkflowStorage, WorkflowId }
import workflows4s.anorm.{ ByteCodec, DatabaseRuntime, WorkflowStorage }
import workflows4s.mermaid.MermaidRenderer
import workflows4s.runtime.wakeup.{ KnockerUpper, NoOpKnockerUpper }
import workflows4s.wio.SignalDef

import javax.inject.*
import scala.util.{ Failure, Success, Try }

import org.apache.pekko.actor.ActorSystem

import zio.*
import zio.json.*

/**
 * Main goal : Zio wrapper
 */
@Singleton
class WorkflowDbRuntime @Inject() (dbapi: DBApi) extends ZioSorus with Logging {

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

  // WorkflowId is a Long => WorkflowId(1L)
  def get_runtime() = {
    val initialState: KycState                               = KycState.Empty
    val storage: WorkflowStorage[WorkflowId, KycEvent]       = new PostgresWorkflowStorage[KycEvent](db)(using eventCodec)
    val knockerUpper: KnockerUpper.Agent[WorkflowId]         = NoOpKnockerUpper.Agent
    val runtime: DatabaseRuntime[KycContext.Ctx, WorkflowId] = DatabaseRuntime.default(workflow, initialState, db, knockerUpper, storage)
    runtime
  }

  def createInstance(workflow_id: WorkflowId) = {
    get_runtime().createInstance(workflow_id) ?|> "Error loading instance"
  }

  def getProgress(workflow_id: WorkflowId) = {
    get_runtime()
      .createInstance(workflow_id)
      .flatMap(_.getProgress) ?|> "Error loading progress"
  }

  def deliverSignal[Req, Resp](workflow_id: WorkflowId, signalDef: SignalDef[Req, Resp], req: Req) = {
    get_runtime()
      .createInstance(workflow_id)
      .flatMap(_.deliverSignal(signalDef, req)) ?|> "Error delivering signal"
  }
}
