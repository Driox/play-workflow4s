package effect.sorus

import scala.concurrent.Future

import zio.*

object ZioBoolean:
  extension (boolean: Boolean)
    def ?|(msg:  String): ZIO[Any, Fail, Boolean] = if (boolean) ZIO.succeed(true) else ZIO.fail(Fail(msg))
    def ?|(fail: Fail): ZIO[Any, Fail, Boolean]   = if (boolean) ZIO.succeed(true) else ZIO.fail(fail)

object ZioOption:
  extension [A](opt: Option[A])
    def ?|(err_msg: String): ZIO[Any, Fail, A] = ZIO.fromOption(opt).mapError(_ => Fail(err_msg))
    def ?|(fail:    Fail): ZIO[Any, Fail, A]   = ZIO.fromOption(opt).mapError(_ => fail)

object ZioFuture:
  extension [A](future: => Future[A])
    def ?|(err_msg: String): ZIO[Any, Fail, A] = ZIO.fromFuture(_ => future).mapError(throwable => Fail(err_msg).withEx(throwable))
    def ?|(fail:    Fail): ZIO[Any, Fail, A]   = ZIO.fromFuture(_ => future).mapError(throwable => fail.withEx(throwable))

object ZioEither:
  extension [E, A](either: Either[E, A])
    def ?|(unit:    Unit): ZIO[Any, Fail, A]   = ZIO.fromEither(either).mapError(err => Fail(err.toString))
    def ?|(err_msg: String): ZIO[Any, Fail, A] = ZIO.fromEither(either).mapError(err => Fail(err.toString).withEx(err_msg))
    def ?|(fail:    Fail): ZIO[Any, Fail, A]   = ZIO.fromEither(either).mapError(err => Fail(err.toString).withEx(fail))

object ZioFutureEither:
  extension [B](future: Future[Either[Fail, B]])
    def ?|(err_msg: String): ZIO[Any, Fail, B] = {
      for {
        x1 <- ZioFuture.?|(future)(s"Unexpected error in Future : $err_msg")
        x2 <- ZioEither.?|(x1)(err_msg)
      } yield {
        x2
      }
    }

    def ?|(fail: Fail): ZIO[Any, Fail, B] = {
      val fail2 = fail.withEx("Unexpected error in Future")
      for {
        x1 <- ZioFuture.?|(future)(fail2)
        x2 <- ZioEither.?|(x1)(fail)
      } yield {
        x2
      }
    }

    def ?|(unit: Unit): ZIO[Any, Fail, B] = {
      for {
        x1 <- ZioFuture.?|(future)("Unexpected error in Future")
        x2 <- ZioEither.?|(x1)(())
      } yield {
        x2
      }
    }

object ZioExtension extends ZioExtension
trait ZioExtension {
  export ZioBoolean.*
  export ZioOption.*
  export ZioFuture.*
  export ZioEither.*
  export ZioFutureEither.*
}
