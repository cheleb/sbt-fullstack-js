package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object FullstackJsPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: JvmPlugin.type = JvmPlugin

  object autoImport {
    val publicFolder = settingKey[String](
      "public folder"
    )
    val setup = taskKey[Unit]("setup")
    val startupTransition: State => State = { s: State =>
      "setup" :: s
    }
    val scalaJsProject: SettingKey[Project] =
      settingKey[Project]("Client projects")
        .withRank(KeyRanks.Invisible)
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    publicFolder := "public",
    setup := {
      OnLoad.apply(
        (thisProject / scalaJsProject / scalaVersion).value,
        (ThisBuild / baseDirectory).value,
        (thisProject / scalaJsProject).value
      )
    }
  ) ++ npmBuild

  private def npmBuild =
    sys.env.getOrElse("INIT", "") match {
      case "FullStack" | "Docker" =>
        Seq(
          (Compile / resourceGenerators) += Def
            .taskDyn[Seq[File]] {
              val rootFolder =
                (Compile / resourceManaged).value / publicFolder.value
              rootFolder.mkdirs()

              Def.task {

                streams.value.log
                  .info(
                    s"Generating static files in <${projectID.value.name}>/${rootFolder.relativeTo(baseDirectory.value).getOrElse(rootFolder)}"
                  )
                if (
                  scala.sys.process
                    .Process(
                      List(
                        "npm",
                        "run",
                        "build",
                        "--",
                        "--emptyOutDir",
                        "--outDir",
                        rootFolder.getAbsolutePath
                      ),
                      scalaJsProject.value.base
                    )
                    .! == 0
                ) {
                  (rootFolder ** "*.*").get
                } else {
                  throw new IllegalStateException("Vite build failed")
                }

              }

            }
            .taskValue
        )
      case _ => Seq()
    }

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq(
    Global / onLoad := {
      val old = (Global / onLoad).value
      // compose the new transition on top of the existing one
      // in case your plugins are using this hook.
      startupTransition compose old
    }
  )
}
