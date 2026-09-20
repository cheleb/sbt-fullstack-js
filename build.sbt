name := """sbt-fullstack-js"""
organization := "dev.cheleb"

val scala212 = "3.9.0"
val scala3 = "3.7.4"

inThisBuild(
  List(
    organization := "dev.cheleb",
    homepage := Some(url("https://github.com/cheleb/sbt-fullstack-js")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "cheleb",
        "Olivier NOUGUIER",
        "olivier.nouguier@gmail.com",
        url("https://cheleb.dev")
      )
    ),
    publishTo := {
      val centralSnapshots =
        "https://central.sonatype.com/repository/maven-snapshots/"
      if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
      else localStaging.value
    },
    versionScheme := Some("early-semver"),
    crossScalaVersions := Seq(scala212, scala3),
    scalaVersion := scala212,
    sbtPluginPublishLegacyMavenStyle := false
  )
)

console / initialCommands := """import dev.cheleb.sbt.fullstackjs._"""

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin, ScriptedPlugin)
  .settings(
    moduleName := "sbt-fullstack-js",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % Test,
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.12.0"
        case _      => "2.0.0-RC8"
      }
    },
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.12.0"
        case _      => (pluginCrossBuild / sbtVersion).value
      }
    },
    // set up 'scripted; sbt plugin for testing sbt plugins
    scriptedLaunchOpts ++=
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
  )
