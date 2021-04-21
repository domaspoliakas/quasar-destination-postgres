import scala.collection.Seq

ThisBuild / scalaVersion := "2.12.10"

ThisBuild / githubRepository := "quasar-destination-postgres"

performMavenCentralSync in ThisBuild := false   // basically just ignores all the sonatype sync parts of things

publishAsOSSProject in ThisBuild := true

homepage in ThisBuild := Some(url("https://github.com/precog/quasar-destination-postgres"))

scmInfo in ThisBuild := Some(ScmInfo(
  url("https://github.com/precog/quasar-destination-postgres"),
  "scm:git@github.com:precog/quasar-destination-postgres.git"))

ThisBuild / githubWorkflowBuildMatrixAdditions +=
  "postgres" -> List("9", "10", "11")

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Run(
    List("docker-compose up -d postgres${{ matrix.postgres }}"),
    name = Some("Start postgres ${{ matrix.postgres }} container"))

val DoobieVersion = "0.8.8"

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "quasar-destination-postgres",

    quasarPluginName := "postgres",

    quasarPluginQuasarVersion := managedVersions.value("precog-quasar"),

    quasarPluginDestinationFqcn := Some("quasar.plugin.postgres.PostgresDestinationModule$"),

    /** Specify managed dependencies here instead of with `libraryDependencies`.
      * Do not include quasar libs, they will be included based on the value of
      * `quasarPluginQuasarVersion`.
      */
    quasarPluginDependencies ++= Seq(
      "org.slf4s"    %% "slf4s-api"       % "1.7.25",
      "org.tpolecat" %% "doobie-core"     % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari"   % DoobieVersion,
      // Some trickery to be able to use a lower version in quasarPluginDependencies
      // With a normal libraryDependencies just adding
      // `"org.postgresql" % "postgresql" % "42.2.8" force()` should work
      // Now doing an exclude and readd instead.
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion  exclude("org.postgresql", "postgresql"),
      // can't use 42.2.9 because it includes https://github.com/pgjdbc/pgjdbc/pull/1612
      // which at least needs https://github.com/pgjdbc/pgjdbc/pull/1658

      // Note that it looks like setSeconds is still incorrect even in current master
      // https://github.com/pgjdbc/pgjdbc/blob/2972add8e47d747655585fc423ac75c609f21c11/pgjdbc/src/main/java/org/postgresql/util/PGInterval.java#L369-L387
      // (i.e. after PR 1658), given the stacktrace
      // https://gist.github.com/rintcius/c09bde9e5a6a6efec7461617e7fe4ca9
      "org.postgresql" % "postgresql" % "42.2.8"
    ),

    libraryDependencies ++= Seq(
      "com.github.tototoshi" %% "scala-csv" % "1.3.6" % Test,
      "com.precog" %% "quasar-lib-jdbc" % managedVersions.value("precog-quasar-lib-jdbc"),
      "com.precog" %% "qdata-core" % managedVersions.value("precog-qdata") % Test,
      "com.precog" %% "quasar-foundation" % quasarPluginQuasarVersion.value % "test->test" classifier "tests",
      "io.argonaut" %% "argonaut-scalaz" % "6.2.3" % Test
    ))
  .enablePlugins(QuasarPlugin)
  .evictToLocal("QUASAR_PATH", "api", true)
  .evictToLocal("QUASAR_PATH", "connector", true)
  .evictToLocal("QUASAR_LIB_JDBC_PATH", "core", true)
