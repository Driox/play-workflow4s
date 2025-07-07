package workflows4s.anorm.postgres

import play.api.db.Database

import java.sql.Connection

import zio.ZIO

trait AnormZio {

  final type Query[A] = Connection => A

  /**
    * Alternative implem with DI in ZIO
    *
    * Here we use ZIO blocking thread pool
    * and put Database in ZIO deps
    */
  final def tzio[A](q: => Query[A]): ZIO[Database, Throwable, A] = {
    for {
      db     <- ZIO.service[Database]
      result <- ZIO.attemptBlocking(
                  db.withConnection { implicit c =>
                    q(c)
                  }
                )
    } yield {
      result
    }
  }

  final def tzio_with_transaction[A](q: => Query[A]): ZIO[Database, Throwable, A] = {
    for {
      db     <- ZIO.service[Database]
      result <- ZIO.attemptBlocking(
                  db.withTransaction { implicit c =>
                    q(c)
                  }
                )
    } yield {
      result
    }
  }
}
