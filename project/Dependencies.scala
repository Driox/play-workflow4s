import sbt.*
import play.sbt.PlayImport.*

object Dependencies {

  val scala_version = "3.7.0"

  lazy val combined_resolvers = Seq(Resolver.mavenCentral)

  lazy val deps_all = deps_common ++ deps_workflow4s_zio ++ deps_zio ++ deps_pekko ++ deps_db

  val play_version     = "3.0.7"
  lazy val deps_common = Seq(
    guice,
    caffeine,
    filters,
    "org.playframework"      %% "play-json"          % "3.0.4" withSources (),
    "org.scalatest"          %% "scalatest"          % "3.2.19" % Test withSources (),
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1"  % Test withSources () excludeAll ExclusionRule(
      organization = "org.mockito"
    )
  )

  // Workflow4s
  lazy val deps_workflow4s_zio = Seq(
    "org.business4s" %% "workflows4s-core"   % "0.1.2" withSources (),
    "org.business4s" %% "workflows4s-doobie" % "0.1.2" withSources (),
    "org.business4s" %% "workflows4s-pekko"  % "0.1.2" withSources (),
    "dev.zio"        %% "zio-interop-cats"   % "3.3.0" withSources ()
  )

  val zio_version     = "2.1.16"
  val zio_log_version = "2.5.0"

  lazy val deps_zio = Seq(
    "dev.zio" %% "zio"                         % zio_version withSources (),
    "dev.zio" %% "zio-streams"                 % zio_version withSources (),
    "dev.zio" %% "zio-macros"                  % zio_version withSources (),
    "dev.zio" %% "zio-logging"                 % zio_log_version withSources (),
    "dev.zio" %% "zio-logging-slf4j"           % zio_log_version withSources (),
    "dev.zio" %% "zio-interop-reactivestreams" % "2.0.2" withSources (),
    "dev.zio" %% "zio-json"                    % "0.7.44" withSources (),
    "dev.zio" %% "zio-s3"                      % "0.4.4" withSources (),
    "dev.zio" %% "zio-test"                    % zio_version % Test withSources ()
  )

  val pekkoVersion    = "1.1.3"
  lazy val deps_pekko = Seq(
    "org.apache.pekko" %% "pekko-actor"                 % pekkoVersion withSources (),
    "org.apache.pekko" %% "pekko-actor-typed"           % pekkoVersion withSources (),
    "org.apache.pekko" %% "pekko-stream"                % pekkoVersion withSources (),
    "org.apache.pekko" %% "pekko-slf4j"                 % pekkoVersion withSources (),
    "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion withSources (),
    "org.apache.pekko" %% "pekko-cluster-typed"         % pekkoVersion withSources (),
    "org.apache.pekko" %% "pekko-persistence-typed"     % pekkoVersion withSources (),
    "org.apache.pekko" %% "pekko-persistence-jdbc"      % "1.1.0" withSources (),
    "org.apache.pekko" %% "pekko-persistence-testkit"   % pekkoVersion % Test withSources (),
    "org.apache.pekko" %% "pekko-testkit"               % pekkoVersion % Test withSources (),
    "org.apache.pekko" %% "pekko-connectors-csv"        % "1.0.2" withSources (),
    "org.apache.pekko" %% "pekko-connectors-file"       % "1.0.2" withSources ()
  )

  lazy val deps_db = Seq(
    evolutions,
    jdbc,
    "org.postgresql"           % "postgresql"        % "42.7.7" withSources (),
    "org.playframework.anorm" %% "anorm"             % "2.7.0" withSources (),
    "org.playframework.anorm" %% "anorm-postgres"    % "2.7.0" withSources (),
    "io.zonky.test"            % "embedded-postgres" % "2.1.0" % Test withSources ()
  )

}
