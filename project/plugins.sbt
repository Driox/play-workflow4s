resolvers += Classpaths.sbtPluginReleases
resolvers += Resolver.jcenterRepo

// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.7")

// code plugins

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.3")

// run sbt dependencyCheckAnyProject
// doc generated in /target/scala-2.13/dependency-check-report.html
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "5.1.0")
