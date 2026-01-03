package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.io.IO
import scala.util.matching.Regex

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

  def substitute(template: String, variables: Map[String, String]): String = {
    """\{\{([^}]+)\}\}""".r.replaceAllIn(
      template,
      m => {
        val name = m.group(1)
        variables.get(name).map(Regex.quoteReplacement).getOrElse(m.matched)
      }
    )
  }

  def writeScript(scriptsDir: File, name: String, content: String): Unit = {
    if (!scriptsDir.exists()) {
      IO.createDirectory(scriptsDir)
    }
    val file = scriptsDir / name
    IO.write(file, content)
    file.setExecutable(true)
  }
}
