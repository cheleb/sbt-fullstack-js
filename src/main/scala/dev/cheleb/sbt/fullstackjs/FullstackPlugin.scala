package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object FullstackPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = noTrigger
  override def requires: JvmPlugin.type = JvmPlugin

  object autoImport {
    val setup = taskKey[Unit]("setup")
    val startupTransition: State => State = { s: State =>
      "setup" :: s
    }
    val fullstackJsProject: SettingKey[Project] =
      settingKey[Project]("Client projects")
        .withRank(KeyRanks.Invisible)
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    setup := {
      OnLoad.apply(
        (thisProject / fullstackJsProject / scalaVersion).value,
        (ThisBuild / baseDirectory).value,
        (thisProject / fullstackJsProject).value
      )
    }
  )

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
