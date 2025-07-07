package controllers

import workflow.{ RenderHelper, WorkflowDbRuntime }
import workflow.domain.*
import workflow.domain.KycWorkflow.*
import workflow.*
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

@Singleton
final class WorkflowController @Inject() (
  val user_repo:     UserRepository,
  workflowDbRuntime: WorkflowDbRuntime
)(using system: ActorSystem) extends MainWebController {

  def create_kyc(user_id: String) = Action.zio { implicit req =>
    for {
      maybe_user <- user_repo.findById(user_id)
      user       <- maybe_user ?| NotFound(s"No user found for id $user_id")
      signal      = Signals.CreateKyc(User(email = user.email), KycKind.Person)
      w_id        = WorkflowId(NumberUtils.random())
      result     <- workflowDbRuntime.deliverSignal(w_id, Signals.create_kyc, signal)
    } yield {
      Ok(Html(s"""
                 |<html>
                 |<body>
                 |  <h1>Create KYC</h1>
                 |  <p> signal : $signal</p>
                 |  <p> result : $result</p>
                 |  <p> w_id : $w_id</p>
                 |  <a href="${routes.WorkflowController.render(w_id.value.toString())}">Render</a>
                 |  <a href="${routes.WorkflowController.update_data(w_id.value.toString())}">Update Data</a>
                 |</body>
                 |</html>
        """.stripMargin))
    }
  }

  def update_data(w_id: String) = Action.zio { implicit req =>
    val data   = KycDataInput(
      first_name = Some("John " + StringUtils.randomAlphanumericString(4)),
      last_name  = Some("Doe"),
      address    = Some(KycAddress(
        number      = "23",
        street      = "rue des bois",
        postal_code = "75012",
        city        = "Paris",
        country     = "FR"
      )),
      phone      = Some("123-456-7890"),
      email      = Some("john.doe@example.com")
    )
    val signal = Signals.UpdateKyc(data)

    for {
      result <- workflowDbRuntime.deliverSignal(WorkflowId(w_id.toLong), Signals.update_data, signal)
    } yield {
      Ok(Html(s"""
                 |<html>
                 |<body>
                 |  <h1>Data Updated</h1>
                 |  <p> signal : $signal</p>
                 |  <p> result : $result</p>
                 |  <p> w_id : $w_id</p>
                 |  <a href="${routes.WorkflowController.render(w_id)}">Render</a>
                 |  <a href="${routes.WorkflowController.create_kyc("usr-0001")}">Create a new KYC</a>
                 |</body>
                 |</html>
        """.stripMargin))
    }
  }

  def render(id: String) = Action.zio { implicit req =>
    val w_id = WorkflowId(id.toLong)
    for {
      progress <- workflowDbRuntime.getProgress(w_id)
    } yield {
      val mermaid = MermaidRenderer.renderWorkflow(progress)
      Ok(Html(s"""
                 |<html>
                 |<head></head>
                 |<body>
                 |  <h1> Workflow ${w_id}</h1>
                 |  <img src="${RenderHelper.toImg(mermaid)}" />
                 |  </body>
                 |</html>
      """.stripMargin))
    }
  }
}

/**
 * In memory mock
 */
class UserRepository {
  def findById(id: String): ZIO[Any, Fail, Option[User]] = {
    if (id == "usr-0001") {
      ZIO.succeed(Some(User(id = "usr-0001", email = "jean.dupont@gmail.com")))
    } else {
      ZIO.succeed(None)
    }
  }
}
