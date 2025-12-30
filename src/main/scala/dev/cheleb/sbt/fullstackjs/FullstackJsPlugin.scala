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
    val exampleTask =
      taskKey[String]("A task that is automatically imported to the build")
    val scalaJsProject: SettingKey[Project] =
      settingKey[Project]("Client projects")
        .withRank(KeyRanks.Invisible)
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    publicFolder := "public",
    exampleTask := "computed from example setting: " + publicFolder.value,
    (Compile / resourceGenerators) += Def
      .taskDyn[Seq[File]] {
        val rootFolder = (Compile / resourceManaged).value / publicFolder.value
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

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()
}
