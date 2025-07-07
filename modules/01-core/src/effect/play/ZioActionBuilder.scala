package effect.play

import effect.sorus.Fail
import play.api.Logging
import play.api.mvc.*
import utils.StringUtils

import java.util.UUID
import scala.concurrent.Future
import scala.util.control.NonFatal

import zio.logging.LogAnnotation
import zio.{ Cause, Runtime, Unsafe, ZIO }

object ZioActionBuilderOps:
  extension [R[_], B](actionBuilder: ActionBuilder[R, B]) {
    def zio[Conf](zioActionBody: R[B] => ZIO[Conf, Fail, Result])(using
      runtime: Runtime[Conf]
    ): Action[B] = {
      ZioActionBuilder.zio[Conf, R, B](actionBuilder)(zioActionBody)
    }

    def zio[Conf, A](
      bp:            BodyParser[A]
    )(
      zioActionBody: R[A] => ZIO[Conf, Fail, Result]
    )(using runtime: Runtime[Conf]): Action[A] = {
      ZioActionBuilder.zio[Conf, A, R, B](actionBuilder)(bp)(zioActionBody)
    }
  }

object ZioActionBuilder extends Logging {

  def zio[Conf, R[_], B](
    actionBuilder: ActionBuilder[R, B]
  )(zioActionBody: R[B] => ZIO[Conf, Fail, Result])(using runtime: Runtime[Conf]): Action[B] = {
    actionBuilder.async {
      case req: Request[?] => runToFuture(zioActionBody(req), s"[${req.method} ${req.path}]")
      case req             => runToFuture(zioActionBody(req))
    }
  }

  def zio[Conf, A, R[_], B](
    actionBuilder: ActionBuilder[R, B]
  )(
    bp:            BodyParser[A]
  )(
    zioActionBody: R[A] => ZIO[Conf, Fail, Result]
  )(using runtime: Runtime[Conf]): Action[A] = {
    actionBuilder(bp).async {
      case req: Request[?] => runToFuture(zioActionBody(req), s"[${req.method} ${req.path}]")
      case req             => runToFuture(zioActionBody(req))
    }
  }

  /**
    * This run a ZIO effect from outside of Play, like a batch
    * It's not linked to HTTP
    */
  def runEffectToFuture[E, A](
    effect:        ZIO[E, Fail, A]
  )(
    using runtime: Runtime[E]
  ): Future[Either[Fail, A]] = {
    val effect_with_log =
      effect
        .tapError(fail => log_error(fail))
        .either @@ LogAnnotation.TraceId(UUID.randomUUID())

    val result = Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.runToFuture(effect_with_log)
    }

    result.recover {
      case NonFatal(throwable) => Left(Fail("Unknown exception during execution").withEx(throwable))
    }(scala.concurrent.ExecutionContext.global)
  }

  /**
    * This run a Play effect in ctrl i.e. i reaction of an http request
    * This handle additional error from controller or filter that are outside of ZIO
    */
  def runToFuture[Conf](
    effect:         => ZIO[Conf, Fail, Result],
    log_decoration: String = "INTERNAL"
  )(
    using runtime:  Runtime[Conf]
  ): Future[Result] = {
    val code            = StringUtils.randomAlphanumericString(8)
    val effect_with_log =
      (
        catchNonEffectError(effect)
          .tapError(fail => log_error(fail, code))
      ) @@ requestLogAnnotation(log_decoration) @@ LogAnnotation.TraceId(UUID.randomUUID())

    val result = Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.runToFuture(appEffectToResult(code, effect_with_log))
    }

    result.recover {
      case NonFatal(throwable) => fail2result(code, Fail("Unknown exception during execution").withEx(throwable))
    }(scala.concurrent.ExecutionContext.global)
  }

  val requestLogAnnotation = LogAnnotation[String](
    name    = "api",
    combine = (a, b) => a + b,
    render  = _.toString()
  )

  /**
    * Catch error thrown outside of ZIO effect, for instance in the action builder i.e. Play ctrl
    * Or inside a Play filter
    */
  private def catchNonEffectError[Conf](effect: => ZIO[Conf, Fail, Result]) = {
    try {
      effect
    } catch {
      case t: Throwable => ZIO.fail(Fail("Unexpected error building result").withEx(t))
    }
  }

  private def appEffectToResult[Conf, E](
    code:   String,
    effect: ZIO[Conf, Fail, Result]
  ): ZIO[Conf, Throwable, Result] = {
    effect
      .catchAll {
        case fail: Fail => ZIO.succeed(fail2result(code, fail))
      }
  }

  private def log_error(fail: Fail, code: String = StringUtils.randomAlphanumericString(8)) = {
    fail
      .getRootException()
      .map { err =>
        ZIO.logErrorCause(s"[$code] ${fail.userMessage()}", Cause.fail(err))
      }.getOrElse {
        ZIO.logError(s"[$code] ${fail.userMessage()}")
      }
  }

  private def fail2result(code: String, fail: Fail): Result = {
    fail match {
      case fail_with_result: FailWithResult => fail_with_result.result
      case fail: Fail                       => Results.BadRequest(s"[#$code]${fail.message}")
    }
  }

}
