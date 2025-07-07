package wiring

import play.api.Configuration

import javax.inject.{ Inject, Singleton }

import zio.*
import zio.logging.backend.SLF4J

@Singleton
final class ZioRuntime @Inject() (
  configuration: Configuration
) {

  lazy val layer = ZLayer.make[ZioRuntime.AppContext](
    Runtime.removeDefaultLoggers,
    SLF4J.slf4j,
    ZLayer.succeed(configuration)
  )

  lazy val runtime: Runtime[ZioRuntime.AppContext] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe.fromLayer(layer)
    }
}

object ZioRuntime {
  type AppContext = Configuration
}
