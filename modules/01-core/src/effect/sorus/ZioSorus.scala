package effect.sorus

import zio.ZIO

/**
 * This allow the stacking of error's messages in the Fail
 */
trait ZioSorus extends ZioExtension {

  extension [R, A](effect: Sorus[R, A]) {
    def ?|(err_msg:   String): Sorus[R, A]       = effect.mapError(fail => new Fail(err_msg).withEx(fail))
    def ?|(new_fail:  Fail): Sorus[R, A]         = effect.mapError(fail => new_fail.withEx(fail))
    def ?|(fail_func: Fail => Fail): Sorus[R, A] = effect.mapError(fail => fail_func(fail))
    def ?|(err:       Throwable): Sorus[R, A]    = effect.mapError(fail => fail.withEx(err))
  }

  extension [R, E <: Throwable, A](effect: ZIO[R, E, A]) {
    def ?|>(err_msg: String): Sorus[R, A] = effect.mapError(err => new Fail(err_msg).withEx(err))
  }
}

object Sorus {
  @inline def success[T](x:      T): Sorus[Any, T]      = ZIO.succeed(x)
  @inline def fail[T](fail:      Fail): Sorus[Any, T]   = ZIO.fail(fail)
  @inline def fail[T](error_msg: String): Sorus[Any, T] = ZIO.fail(new Fail(error_msg))
}
