package workflows4s.anorm

import play.api.db.Database
import workflows4s.runtime.wakeup.KnockerUpper
import workflows4s.runtime.{ WorkflowInstance, WorkflowRuntime }
import workflows4s.wio.WIO.Initial
import workflows4s.wio.{ ActiveWorkflow, WCEvent, WCState, WorkflowContext }

import java.time.Clock

import zio.*

class DatabaseRuntime[Ctx <: WorkflowContext, WorkflowId](
  workflow:     Initial[Ctx],
  initialState: WCState[Ctx],
  database:     Database,
  knockerUpper: KnockerUpper.Agent[WorkflowId],
  storage:      WorkflowStorage[WorkflowId, WCEvent[Ctx]],
  clock:        Clock
) extends WorkflowRuntime[Task, Ctx, WorkflowId] {

  override def createInstance(id: WorkflowId): Task[WorkflowInstance[Task, WCState[Ctx]]] = {
    val instance: WorkflowInstance[Task, WCState[Ctx]] = createInstance_(id)
    ZIO.succeed(instance)
  }

  def createInstance_(id: WorkflowId): WorkflowInstance[Task, WCState[Ctx]] = {
    new DbWorkflowInstance(
      id,
      ActiveWorkflow(workflow, initialState),
      storage,
      clock,
      knockerUpper,
      database
    )
  }
}

object DatabaseRuntime {
  def default[Ctx <: WorkflowContext, WorkflowId](
    workflow:     Initial[Ctx],
    initialState: WCState[Ctx],
    database:     Database,
    knockerUpper: KnockerUpper.Agent[WorkflowId],
    storage:      WorkflowStorage[WorkflowId, WCEvent[Ctx]],
    clock:        Clock = Clock.systemUTC()
  ) = new DatabaseRuntime[Ctx, WorkflowId](workflow, initialState, database, knockerUpper, storage, clock)
}
