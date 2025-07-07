package workflows4s.anorm.postgres

import cats.effect.IO
import test.ZioTestHelper
import workflows4s.anorm.DatabaseRuntime
import workflows4s.anorm.postgres.testing.PostgresServerEmbedded
import workflows4s.runtime.wakeup.NoOpKnockerUpper
import workflows4s.wio.WorkflowContext

import scala.concurrent.duration.DurationInt
import scala.util.Try

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresRuntimeTest extends AnyWordSpec with Matchers with PostgresServerEmbedded with ZioTestHelper {

  "DbWorkflowInstance" should {
    "work for long-running IO" in {
      import TestCtx.*

      val wio: WIO.Initial = WIO
        .runIO[Any](_ => IO.sleep(1.second) *> IO(Event()))
        .handleEvent((_, _) => State())
        .done

      val storage          = new PostgresWorkflowStorage(db)(using noopCodec(Event()))
      val runtime          = DatabaseRuntime.default(wio, State(), db, NoOpKnockerUpper.Agent, storage)
      val workflowInstance = runInSuccess(runtime.createInstance(WorkflowId(1)))

      // this used to throw due to leaked LiftIO
      val result = run(workflowInstance.wakeup())
      result mustBe Right(())
    }
  }

  object TestCtx extends WorkflowContext {
    case class State()
    case class Event()
  }

  def noopCodec[E](evt: E) = new workflows4s.anorm.ByteCodec[E] {
    override def write(event: E): IArray[Byte]      = IArray.empty
    override def read(bytes:  IArray[Byte]): Try[E] = scala.util.Success(evt)
  }
}
