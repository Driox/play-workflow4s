package workflows

import _root_.workflow.RenderHelper
import play.api.Logging
import workflow.domain.*
import workflow.domain.KycWorkflow.*
import workflows4s.mermaid.MermaidRenderer
import workflows4s.runtime.InMemorySyncRuntime

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class KycWorkflowTest extends AnyWordSpec with Matchers with Logging {

  def runtime() = InMemorySyncRuntime.default[KycContext.Ctx, String](workflow, KycState.Empty)

  "KycWorkflow" should {
    "run in memory" in {

      // exec
      val runtime     = InMemorySyncRuntime.default[KycContext.Ctx, String](workflow, KycState.Empty)
      val wf_instance = runtime.createInstance("kyc-123")

      logger.info("=== Kyc Workflow ===\n")
      wf_instance.deliverSignal(
        Signals.create_kyc,
        Signals.CreateKyc(
          User(
            email = "user@gmail.com"
          ),
          KycKind.Person
        )
      )
      logger.info(wf_instance.queryState().toString())
      logger.info("")

      wf_instance.queryState().isInstanceOf[KycState.Ongoing] mustBe true

      wf_instance.deliverSignal(
        Signals.update_data,
        Signals.UpdateKyc(
          KycDataInput(first_name = Some("Jean"))
        )
      )
      logger.info(wf_instance.queryState().toString())

      wf_instance.queryState().isInstanceOf[KycState.Rejected] mustBe true

      val mermaid = MermaidRenderer.renderWorkflow(wf_instance.getProgress)
      val img     = RenderHelper.toImg(mermaid)
      logger.info("Copy in browser : \n" + img)
    }
  }
}
