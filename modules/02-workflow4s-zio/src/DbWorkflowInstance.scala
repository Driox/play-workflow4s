package workflows4s.anorm

import cats.Monad
import cats.effect.{ IO, LiftIO }
import play.api.db.Database
import workflows4s.runtime.WorkflowInstanceBase
import workflows4s.runtime.wakeup.KnockerUpper
import workflows4s.wio.*

import java.time.{ Clock, Instant }

import com.typesafe.scalalogging.StrictLogging

import zio.*
import zio.interop.catz.*

private type Result[T] = Task[T]

class DbWorkflowInstance[Ctx <: WorkflowContext, Id](
  id:                  Id,
  baseWorkflow:        ActiveWorkflow[Ctx],
  storage:             WorkflowStorage[Id, WCEvent[Ctx]],
  protected val clock: Clock,
  knockerUpperForId:   KnockerUpper.Agent[Id],
  database:            Database
) extends WorkflowInstanceBase[Result, Ctx]
    with StrictLogging {

  import cats.effect.unsafe.IORuntime
  given cats_runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  override protected def fMonad: Monad[Result]                         = summon
  override protected lazy val knockerUpper: KnockerUpper.Agent.Curried = knockerUpperForId.curried(id)

  override protected def getWorkflow: Result[ActiveWorkflow[Ctx]] = {
    def recoveredState(now: Instant): ZIO[Database, Throwable, ActiveWorkflow[Ctx]] =
      storage
        .getEvents(id)
        .runFold(baseWorkflow)((state, event) =>
          state.handleEvent(event, now) match {
            case Some(value) => value
            case None        => {
              logger.warn(s"Ignored event ${}")
              state
            }
          }
        )

    val effect = for {
      now    <- currentTime
      result <- recoveredState(now)
    } yield result

    effect.provide(ZLayer.succeed(database))
  }

  override protected def lockAndUpdateState[T](update: ActiveWorkflow[Ctx] => Result[LockOutcome[T]]): Result[StateUpdate[T]] = {
    val effect = for {
      oldState    <- getWorkflow
      lockResult  <- update(oldState)
      now         <- ZIO.succeed(clock.instant())
      stateUpdate <- lockResult match {
                       case LockOutcome.NewEvent(event, result) => storage.saveEvent(id, event)
                           .map { _ =>
                             val newState = processLiveEvent(event, oldState, now)
                             StateUpdate.Updated(oldState, newState, result)
                           }
                       case LockOutcome.NoOp(result)            => ZIO.succeed(StateUpdate.NoOp(oldState, result))
                     }
    } yield stateUpdate

    effect.provide(ZLayer.succeed(database))
  }

  override protected def liftIO: LiftIO[Result] = new LiftIO[Result] {
    override def liftIO[A](ioa: IO[A]): Result[A] = ioa.to[Task]
  }
}
