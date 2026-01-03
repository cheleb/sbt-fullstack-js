package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object FullstackPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: JvmPlugin.type = JvmPlugin

  object autoImport {
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
  )

  override lazy val buildSettings = Seq(
    fullstackJvmProject := None,
    fullstackSetup :=
      OnLoad.setup(
        (thisProject / fullstackJsProject / scalaVersion).value,
        (ThisBuild / baseDirectory).value,
        (thisProject / fullstackJsProject).value,
        (thisProject / fullstackJvmProject).value
      ),
    fullstackServer := OnLoad.server((ThisBuild / baseDirectory).value),
    fullstackScriptsTemplates := DefaultTemplates.defaultTemplates,
    fullstackScriptsVariables := {
      Map(
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
          ScriptManager.writeScript(scriptsDir, name, content)
          log.info(s"Updated $name")
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
