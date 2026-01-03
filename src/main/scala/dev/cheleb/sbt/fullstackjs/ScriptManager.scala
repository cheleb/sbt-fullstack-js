package dev.cheleb.sbt.fullstackjs

import sbt._
import sbt.io.IO
import scala.util.matching.Regex

object ScriptManager {
  val ManagedHeader =
    "-- DO NOT EDIT: This file is managed by sbt-fullstack-js plugin --"

  private def fileExtension(file: File): Option[String] =
    file.getName.lastIndexOf(".") match {
      case -1 => None
      case i  => Some(file.getName.substring(i + 1))
    }

  def isManaged(file: File): Boolean = {
    if (!file.exists()) {
      true
    } else {
      val lines = IO.readLines(file)
      val comment =
        fileExtension(file) match {
          case Some("sh") => "#"
          case Some("sc") => "//"
          case Some(ext)  =>
            throw new IllegalStateException(s"Unsupported extension [$ext]")
          case None =>
            throw new IllegalStateException(s"Not extension")
        }
      lines.slice(1, 2).headOption.exists(_.trim == s"$comment$ManagedHeader")
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

    val header = fileExtension(file) match {
      case Some("sh") =>
        s"""|#!/usr/bin/env bash
            |#$ManagedHeader"""
      case Some("sc") =>
        s"""|#!/usr/bin/env -S scala-cli --scala-version 3.8.0-RC5
            |//$ManagedHeader"""
      case Some(ext) =>
        throw new IllegalStateException(s"Unsupported extension [$ext]")
      case None =>
        throw new IllegalStateException(s"Not extension")
    }

    IO.writeLines(file, List(header.stripMargin, content))
    file.setExecutable(true)
  }
}
