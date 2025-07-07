package effect.play

import effect.sorus.Fail
import play.api.data.Form
import play.api.libs.json.{ JsPath, JsResult, JsonValidationError }
import play.api.mvc.{ Result, Results }

import zio.ZIO

case class FailWithResult(
  override val message: String,
  val result:           Result,
  override val cause:   Option[Either[Throwable, Fail]] = None
) extends Fail(message, cause) {
  override def withEx(fail: Fail): FailWithResult = new FailWithResult(this.message, result, Some(Right(fail)))
}

/**
 * Convert json validator / Form / etc... to Fail in ZIO effect
 */
trait ZioPlayHelper extends ZioJson {

  export ZioForm.*
  export effect.play.ZioActionBuilderOps.*

  /**
   * Allow this kind of mapping with result on the left
   *
   * criteria <- eventSearchForm.bindFromRequest ?| (formWithErrors => Ok(views.html.analyzer.index(formWithErrors)))
   */
  given result2Fail: Conversion[Result, FailWithResult] = (result: Result) => FailWithResult("result from ctrl", result)

  /**
   * Allow this kind of mapping with result on the left
   *
   * user <- user_repo.findByUuid(uuid) ?| NotFound
   */
  given status2Fail: Conversion[Results#Status, FailWithResult] = (status: Results#Status) => FailWithResult("result from ctrl", status)
}

trait ZioJson {
  export ZioJsonValidation.*
}

object ZioJsonValidation:
  type JsErrorContent = scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]

  private def default_failure_handler(err: JsErrorContent): Fail = {
    val msg = err.map(x => x._1.toString() + " : " + x._2.mkString(", ")).mkString("\n")
    new Fail(msg)
  }

  extension [A](json: JsResult[A])
    def ?|(failureHandler: (JsErrorContent) => Fail): ZIO[Any, Fail, A] = {
      ZIO
        .fromEither(json.asEither)
        .mapError(failureHandler)
    }

    def ?|(unit: Unit): ZIO[Any, Fail, A] = {
      ?|(f => default_failure_handler(f))
    }

    def ?|(msg: String): ZIO[Any, Fail, A] = {
      ?|(f => default_failure_handler(f).withEx(msg))
    }

object ZioForm:
  extension [T](form: Form[T])
    def ?|(failureHandler: (Form[T]) => Fail): ZIO[Any, Fail, T] = {
      ZIO.fromEither(
        form.fold(failureHandler andThen Left.apply, Right.apply)
      )
    }

    def ?|(unit: Unit): ZIO[Any, Fail, T] = ?|(f => default_failure_handler(f))
    private def default_failure_handler[A](formWithErrors: Form[A]): Fail = {
      val msg = formWithErrors.errors.map(_.message).mkString("\n")
      Fail(s"Error in your input data : $msg")
    }
