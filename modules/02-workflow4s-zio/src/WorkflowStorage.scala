package workflows4s.anorm

import play.api.db.Database

import zio.*
import zio.stream.ZStream

trait WorkflowStorage[Id, Event] {

  def getEvents(id: Id): ZStream[Database, Throwable, Event]
  def saveEvent(id: Id, event: Event): ZIO[Database, Throwable, Unit]

  // Resource because some locking mechanisms might require an explicit release
  def lockWorkflow(id: Id): ZIO[Scope, Throwable, Unit]

}
