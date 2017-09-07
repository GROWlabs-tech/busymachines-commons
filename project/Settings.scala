import sbt._
import Keys._

object Settings {
  lazy val organizationName: String = "com.busymachines"

  lazy val bmCommonsHomepage: String = "https://github.com/busymachines/busymachines-commons"

  def commonSettings: Seq[Setting[_]] =
    Seq(
      organization in ThisBuild := organizationName,
      homepage := Some(url(bmCommonsHomepage)),
      scalaVersion := Dependencies.`scala_2.12`,
      crossScalaVersions := Dependencies.seqOfCrossScalaVersions
    ) ++
      scalaCompilerSettings

  def scalaCompilerSettings: Seq[Setting[_]] = Nil
}
