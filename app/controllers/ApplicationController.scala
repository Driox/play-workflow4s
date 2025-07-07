package controllers

import play.api.Configuration
import play.api.i18n.*

import javax.inject.*
import zio.ZIO

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
final class ApplicationController @Inject() (configuration: Configuration) extends MainWebController {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def home() = Action.zio { implicit req =>
    ZIO.succeed(Ok(views.html.index()))
  }

  def ping() = Action {
    Ok("Ok")
  }
}
