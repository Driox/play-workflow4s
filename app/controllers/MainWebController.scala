package controllers

import effect.play.ZioController
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.*
import wiring.ZioRuntime

import javax.inject.Inject
import scala.annotation.nowarn

import zio.Runtime

trait MainWebController
  extends MainController[ZioRuntime.AppContext] {

  @nowarn
  @Inject() private var _runtime: ZioRuntime                = scala.compiletime.uninitialized // scalafix:ok
  implicit lazy val runtime: Runtime[ZioRuntime.AppContext] = _runtime.runtime

}

trait MainController[ZAPP]
  extends BaseController
    with I18nSupport
    with ZioController[ZAPP]
    with Logging {

  /**
    * scala3: we can't use InjectedController with scala3, see Play migration guide :
    * https://www.playframework.com/documentation/3.0.x/Scala3Migration#Potential-runtime-error-due-to-Scala-3-guice-regression
    *
    * NB: Can't make it private without making testing a huge pain
    * We need to do that in test
    *
    * val controller = new Application(userRepository, configuration)
    * controller._controllerComponents = Helpers.stubControllerComponents()
    */
  @nowarn
  @Inject() var _controllerComponents: ControllerComponents    = scala.compiletime.uninitialized // scalafix:ok
  implicit lazy val controllerComponents: ControllerComponents = _controllerComponents

  given runtime: Runtime[ZAPP]

}
