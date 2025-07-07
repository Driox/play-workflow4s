val project_name = "play-workflow4s"

name := project_name

Global / lintUnusedKeysOnLoad := false

ThisBuild / scalaVersion := Dependencies.scala_version

lazy val commonSettings = Seq(
  organization                                   := "com.driox",
  version                                        := "1.0.0",
  scalaVersion                                   := Dependencies.scala_version,
  resolvers ++= Dependencies.combined_resolvers,
  scalacOptions                                  := scalacOptions.value.distinct, // remove warning caused by Play
  libraryDependencies ++= Dependencies.deps_all,
  excludeDependencies += "org.scala-lang.modules" % "scala-collection-compat_2.13",
  Compile / doc / sources                        := Seq.empty,
  Compile / run / fork                           := true,                         // to run in a separate process
  Compile / parallelExecution                    := true,
  Test / parallelExecution                       := true
)

lazy val core: Project = (project in file("modules/01-core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys    := Seq[BuildInfoKey](
      "name" -> project_name,
      version,
      scalaVersion,
      sbtVersion
    ),
    buildInfoPackage := "core.build"
  )
  .settings(commonSettings: _*)

lazy val workflow4s_zio: Project = (project in file("modules/02-workflow4s-zio"))
  .settings(commonSettings: _*)
  .dependsOn(core % "test->test;compile->compile")

lazy val root: Project = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .aggregate(core, workflow4s_zio)
  .dependsOn(core % "test->test;compile->compile", workflow4s_zio)

import com.typesafe.sbt.packager.MappingsHelper.directory
Universal / mappings ++= directory(baseDirectory.value / "public")

// Check Dependancy CVSS config
ThisBuild / dependencyCheckFailBuildOnCVSS         := 1.0f
ThisBuild / dependencyCheckFormats                 := Seq("XML", "HTML")
ThisBuild / dependencyCheckAssemblyAnalyzerEnabled := Option(false)
ThisBuild / dependencyCheckSuppressionFiles        := Seq(baseDirectory.value / "dependency-check-suppressions.xml")
dependencyCheckOutputDirectory                     := Some(baseDirectory.value / "target/security-reports")
