package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object FullstackJvmPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires = JvmPlugin && FullstackPlugin

  object autoImport {
    val publicFolder = settingKey[String](
      "public folder"
    )
  }

  import autoImport._
  import FullstackPlugin.autoImport.fullstackJsProject

  override lazy val projectSettings = Seq(
    publicFolder := "public"
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
                      fullstackJsProject.value.base
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
  )
}
