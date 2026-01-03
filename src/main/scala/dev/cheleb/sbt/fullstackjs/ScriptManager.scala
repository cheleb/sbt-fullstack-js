package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.io.IO

object ScriptManager {
  val ManagedHeader =
    "# DO NOT EDIT: This file is managed by sbt-fullstack-js plugin"

  def isManaged(file: File): Boolean = {
    if (!file.exists()) {
      true
    } else {
      val lines = IO.readLines(file)
      lines.headOption.exists(_.trim == ManagedHeader)
    }
  }
}
