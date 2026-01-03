package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.Keys._
import java.nio.charset.StandardCharsets

object OnLoad {

  def server(root: File) = IO.write(
    serverMarkerFile(root),
    "started",
    StandardCharsets.UTF_8
  )

  def setup(
      scalaVersion: String,
      root: File,
      client: Project,
      server: Option[Project]
  ) = {
    val outputFile = root / "scripts" / "target" / "build-env.sh"
    println(s"🍺 Generating build-env.sh at $outputFile")

    val MAIN_JS_PATH =
      client.base.getAbsoluteFile / "target" / s"scala-$scalaVersion" / s"${client.id}-fastopt/main.js"

    val NPM_DEV_PATH =
      root / "target" / "npm-dev-server-running.marker"

    IO.writeLines(
      outputFile,
      List(s"""|# Marker file to indicate that npm dev server has been started
               |MAIN_JS_PATH="${MAIN_JS_PATH}"
               |# Marker file to indicate that npm dev server has been started
               |NPM_DEV_PATH="${NPM_DEV_PATH}"""".stripMargin) ::: server
        .map(_ => s"""|# Marker file to indicate that server has been started
              |SERVER_DEV_PATH="${serverMarkerFile(root)}"""".stripMargin)
        .toList,
      StandardCharsets.UTF_8
    )
  }
  def serverMarkerFile(server: File) =
    server / "target" / "dev-server-running.marker"

}
