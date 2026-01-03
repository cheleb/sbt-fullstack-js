package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object FullstackPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: JvmPlugin.type = JvmPlugin

  object autoImport {
    val fullstackPublicFolder = settingKey[String](
      "public folder"
    )
    val fullstackSetup = taskKey[Unit]("setup")
    val fullstackServer = taskKey[Unit]("server")
    val fullstackStartupTransition: State => State = { s: State =>
      sys.env.get("INIT") match {
        case Some("setup") =>
          "fullstackSetup" :: "fullstackScripts" :: s
        case Some("server") =>
          "fullstackServer" :: s
        case _ => s
      }
    }
    val fullstackJsModules: SettingKey[String] =
      settingKey[String]("Client project module folder")
        .withRank(KeyRanks.Invisible)
    val fullstackJsProject: SettingKey[Project] =
      settingKey[Project]("Client projects")
        .withRank(KeyRanks.Invisible)
    val fullstackJvmProject: SettingKey[Option[Project]] =
      settingKey[Option[Project]]("Server projects")
        .withRank(KeyRanks.Invisible)
    val fullstackScripts =
      taskKey[Unit]("Generate helper scripts for fullstack development")
    val fullstackScriptsTemplates =
      settingKey[Map[String, String]]("Custom templates for fullstack scripts")
    val fullstackScriptsVariables =
      taskKey[Map[String, String]](
        "Variables to substitute in fullstack script templates"
      )
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    fullstackPublicFolder := "public"
  ) ++ npmBuild

  private def npmBuild =
    sys.env.getOrElse("INIT", "") match {
      case "FullStack" | "Docker" =>
        Seq(
          (Compile / resourceGenerators) += Def
            .taskDyn[Seq[File]] {
              val rootFolder =
                (Compile / resourceManaged).value / fullstackPublicFolder.value
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

  override lazy val buildSettings = Seq(
    fullstackJsModules := "modules",
    fullstackJvmProject := None,
    fullstackSetup :=
      OnLoad.setup(
        (thisProject / fullstackJsProject / scalaVersion).value,
        (ThisBuild / baseDirectory).value,
        (thisProject / fullstackJsProject).value,
        (thisProject / fullstackJvmProject).value
      ),
    fullstackServer := OnLoad.server((ThisBuild / baseDirectory).value),
    fullstackScriptsTemplates := DefaultTemplates.templates(
      (thisProject / fullstackJvmProject).value
    ),
    fullstackScriptsVariables := {
      Map(
        "modules" -> fullstackJsModules.value,
        "appProjectId" -> fullstackJsProject.value.id
      ) ++ fullstackJvmProject.value.map(p => "serverProjectId" -> p.id)
    },
    fullstackScripts := {
      val log = streams.value.log
      val base = (ThisBuild / baseDirectory).value
      val scriptsDir = base / "scripts"
      val templates = fullstackScriptsTemplates.value
      val variables = fullstackScriptsVariables.value

      templates.foreach { case (name, template) =>
        val targetFile = scriptsDir / name
        if (ScriptManager.isManaged(targetFile)) {
          val content = ScriptManager.substitute(template, variables)
          ScriptManager.writeScript(scriptsDir, name, content, log)
        } else {
          log.warn(s"Skipped $name (unmanaged or custom)")
        }
      }
    }
  )

  override lazy val globalSettings = Seq(
    Global / onLoad := {
      val old = (Global / onLoad).value
      // compose the new transition on top of the existing one
      // in case your plugins are using this hook.
      fullstackStartupTransition compose old
    }
  )
}
