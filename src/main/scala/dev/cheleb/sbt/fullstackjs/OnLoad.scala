package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.Keys._
import java.nio.charset.StandardCharsets

object OnLoad {

  def apply(scalaVersion: String, root: File, client: Project) =
    sys.env.get("INIT") match {
      case Some("setup")  => setup(scalaVersion, root, client)
      case Some("server") =>
        IO.write(
          serverMarkerFile(root),
          "started",
          StandardCharsets.UTF_8
        )
      case _ =>

    }

  def setup(
      scalaVersion: String,
      root: File,
      client: Project
  ) = {
    val outputFile = root / "scripts" / "target" / "build-env.sh"
    println(s"🍺 Generating build-env.sh at $outputFile")

    val MAIN_JS_PATH =
      client.base.getAbsoluteFile / "target" / s"scala-$scalaVersion" / s"${client.id}-fastopt/main.js"

    val NPM_DEV_PATH =
      root / "target" / "npm-dev-server-running.marker"

    IO.writeLines(
      outputFile,
      s"""
         |# Generated file see build.sbt
         |SCALA_VERSION="$scalaVersion"
         |# Marker file to indicate that server has bin started
         |SERVER_DEV_PATH=${serverMarkerFile(root)}
         |# Marker file to indicate that npm dev server has been started
         |MAIN_JS_PATH="${MAIN_JS_PATH}"
         |# Marker file to indicate that npm dev server has been started
         |NPM_DEV_PATH="${NPM_DEV_PATH}"
         |""".stripMargin.split("\n").toList,
      StandardCharsets.UTF_8
    )
  }
  def serverMarkerFile(server: File) =
    server / "target" / "dev-server-running.marker"

}
