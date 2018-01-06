import sbt._
import Keys._

lazy val currentSnapshotVersion = "0.2.0-SNAPSHOT"

addCommandAlias("setSnapshotVersion", s"""set version in ThisBuild := "$currentSnapshotVersion"""")

addCommandAlias("ci", ";clean;update;compile;Test/compile;test")

addCommandAlias("doLocal", ";clean;update;compile;publishLocal")

addCommandAlias("doSnapshotLocal", ";clean;update;compile;setSnapshotVersion;publishLocal")

/**
  * Use with care. Releases a snapshot to sonatype repository.
  *
  * Currently this will not work properly because of an SBT bug where
  * the artifacts are not overriden in the SONATYPE repo:
  * https://github.com/sbt/sbt/issues/3725
  *
  * All instructions for publishing to sonatype can be found in
  * ``z-publishing-artifcats/README.md``.
  */
addCommandAlias("doSnapshotRelease", ";ci;setSnapshotVersion;publishSigned")

/**
  * Use with care.
  *
  * All instructions for publishing to sonatype can be found in
  * ``z-publishing-artifcats/README.md``.
  */
addCommandAlias("doRelease", ";ci;publishSigned;sonatypeRelease")

/**
  * this is a phantom project that is simply supposed to aggregate all modules for convenience,
  * it is NOT published as an artifact. It doesn't have any source files, it is just a convenient
  * way to propagate all commands to the modules via the aggregation
  */
lazy val root = Project(
  id = "busymachines-commons",
  base = file("."))
  .settings(PublishingSettings.noPublishSettings)
  .settings(Settings.commonSettings)
  .aggregate(
    core,
    json,
    `rest-core`,
    `rest-core-testkit`,
    `rest-json`,
    `rest-json-testkit`,
    
    `semver`,
    `semver-parsers`,
  )

lazy val core = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-core",
    libraryDependencies += Dependencies.scalaTest % Test withSources()
  )

lazy val json = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-json",
    libraryDependencies ++=
      Dependencies.circe.map(c => c withSources()) ++ Seq(
        Dependencies.shapeless withSources(),
        Dependencies.catsCore withSources(),

        Dependencies.scalaTest % Test withSources()
      )
  )
  .dependsOn(
    core
  )

lazy val `rest-core` = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-rest-core",
    libraryDependencies ++= Seq(
      Dependencies.akkaHttp withSources(),
      Dependencies.akkaActor withSources(),

      /**
        * http://doc.akka.io/docs/akka-http/current/scala/http/introduction.html#using-akka-http
        * {{{
        * Only when running against Akka 2.5 explicitly depend on akka-streams in same version as akka-actor
        * }}}
        */
      Dependencies.akkaStream withSources(),

      //used for building the WebServerIO helpers
      Dependencies.catsEffects withSources(),
    )
  )
  .dependsOn(
    core
  )

lazy val `rest-core-testkit` = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-rest-core-testkit",
    libraryDependencies ++= Seq(
      Dependencies.akkaHttpTestKit withSources(),
      Dependencies.scalaTest withSources(),
      Dependencies.scalaTest % Test withSources()
    )
  )
  .dependsOn(
    core,
    `rest-core`
  )

lazy val `rest-json` = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-rest-json",
    libraryDependencies ++= Seq(
      Dependencies.akkaHttpCirceIntegration withSources()
    )
  )
  .dependsOn(
    core,
    json,
    `rest-core`,
  )

lazy val `rest-json-testkit` = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-rest-json-testkit",
    libraryDependencies ++= Seq(
      Dependencies.scalaTest % Test withSources(),
    )
  )
  .dependsOn(
    core,
    json,
    `rest-core`,
    `rest-json`,
    `rest-core-testkit`,
  )

lazy val `semver` = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-semver",
    libraryDependencies ++= Seq(
      Dependencies.scalaTest % Test withSources(),
    )
  )
  .dependsOn()

lazy val `semver-parsers` = project
  .settings(Settings.commonSettings)
  .settings(PublishingSettings.sonatypeSettings)
  .settings(
    name in ThisProject := "busymachines-commons-semver-parsers",
    libraryDependencies ++= Seq(
      Dependencies.attoParser withSources(),

      Dependencies.scalaTest % Test withSources(),
      Dependencies.scalaCheck % Test withSources(),
    )
  )
  .dependsOn(
    core,
    `semver`
  )
