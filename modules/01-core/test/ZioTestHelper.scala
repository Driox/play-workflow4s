package test

import effect.sorus.Fail
import play.api.Logging

import scala.concurrent.Future

import zio.*

trait ZioTestHelper extends Logging {

  def unsafe_run[ERR, A](effect: ZIO[Any, ERR, A]): Exit[ERR, A] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(effect)
    }

  def run[E, A](effect: ZIO[Any, E, A]): Either[E, A] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(effect.either).getOrThrowFiberFailure()
    }

  def runInSuccess[ERR, A](effect: ZIO[Any, ERR, A]): A = {
    val maybeResult =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.run(effect.either).getOrThrowFiberFailure()
      }

    maybeResult.swap
      .map {
        case fail: Fail => fail.getRootException()
            .map(ex => logger.error(fail.userMessage(), ex))
            .getOrElse(logger.error(fail.userMessage()))
        case err        => logger.error(s"Error for test :\n  ${err}\n")
      }
    maybeResult.toOption.get
  }

  def runToFuture[A](effect: ZIO[Any, Fail, A]): Future[Either[Fail, A]] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.runToFuture(effect.either)
    }

}
