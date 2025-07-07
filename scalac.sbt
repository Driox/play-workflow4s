// ~~~~~~~~~~~~~~~~~
// Compiler config
// doc : https://docs.scala-lang.org/scala3/guides/migration/options-new.html
import java.io.PrintStream

// sbt and compiler option
lazy val compiler_option = Seq(
  // Scala 3 new feautre
  // "-source:future",                // Enable future language features and warn about deprecated features.
  "-source:3.7",
  "-deprecation",                  // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8",                         // Specify character encoding used by source files.
  "-explain-types",                // Explain type errors in more detail.
  "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
  // "-no-indent",                    // Require classical {…} syntax, indentation is not significant.
  "-language:postfixOps",
  "-language:existentials",        // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds",         // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-Wsafe-init",                   // Wrap field accessors to throw an exception on uninitialized access.
  "-Wunused:imports,privates",     // Warn if imports are unused.
  // "-Ycheck-all-patmat"             // Check exhaustivity and redundancy of all pattern matching (used for testing the algorithm).
  // "-Yexplicit-nulls"               // Make reference types non-nullable. Nullable types can be expressed with unions: e.g. String|Null.
  "-Yretain-trees",                // for scala3 default case class value, cf. https://zio.dev/zio-json/
  // "-Ykind-projector",
  // "-Wvalue-discard",
  // "-Wnonunit-statement",
  // "-Wunused:implicits",
  // "-Wunused:imports",
  // "-Wunused:locals",
  // "-Wunused:params",
  // "-Wunused:privates"

  // Allow to use Scala 2.x implicit parameters without using `using` clause
  "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s",

  // Silent Play warning about unused imports in views and routes
  "-Wconf:src=views/.*&msg=unused import:silent",
  "-Wconf:src=.*routes&msg=unused import:s",
  "-Wconf:src=.*routes&msg=match may not:s"
)

ThisBuild / scalacOptions ++= compiler_option

// deactivate for now because of https://github.com/sbt/sbt/pull/5847
// or https://github.com/sbt/sbt/issues/7973
val _ = System.setErr(new PrintStream(System.err) {
  override def println(x: Any) = {
    x match {
      case e: java.util.concurrent.CancellationException =>
        ()
        e.setStackTrace(Array())
      case _                                             =>
        super.println(x)
    }
  }
})

ThisBuild / usePipelining := true // make multi-module compilation faster
ThisBuild / turbo         := true // compile all that is ready as soon as possible, cf. https://www.scala-sbt.org/1.x/docs/sbt-1.3-Release-Notes.html
