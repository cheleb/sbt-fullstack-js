package dev.cheleb.sbt.fullstackjs

import sbt.Project

object DefaultTemplates {

  val ciBuild = s"""
set -e

./{{managed}}/setup.sc -- {{appProjectId}}

sbt -mem 4096 "{{serverProjectId}}/compile"

cd {{modules}}/{{appProjectFolder}}

{{jsPackageManager}} run build
"""

  val dockerPublish = s"""
set -e

./{{managed}}/setup.sc -- {{appProjectId}}

sbt -mem 4096 "project {{serverProjectId}}; set fullstackNpmBuild := true; Docker/publish"
"""

  val dockerPublishLocal = s"""
set -e

./{{managed}}/setup.sc -- {{appProjectId}}

sbt -mem 4096 "project {{serverProjectId}}; set fullstackNpmBuild := true; Docker/publishLocal"
"""

  val fastLink = s"""
if [ -e ./{{managed}}/build-env.sh ]; then
 . ./{{managed}}/build-env.sh
else
 echo "Error: build-env.sh not found. Please run ./{{managed}}/setup.sc first."
 exit 1
fi

echo -n "Waiting for {{jsPackageManager}} dev server to start."

until [ -e $$NPM_DEV_PATH ]; do
    echo -n "."
    sleep 2
done

echo "  ✅"
echo "{{jsPackageManager}} dev server started."
echo "Waiting for client-fastopt/main.js to be generated."

until [ -e $$MAIN_JS_PATH ]; do
    echo -n "."
    sleep 2
done
echo "  ✅"
echo "⏱️ Watching client-fastopt/main.js for changes..."

sbt --batch -Dsbt.supershell=false '~{{appProjectId}}/fastLinkJS'
"""

  val fullstackRun = s"""
set -e
#
# This script is used to run the fullstack server
#
./{{managed}}/setup.sc -- {{appProjectId}}

docker-compose up -d

sbt -mem 4096 "project {{serverProjectId}}; set fullstackNpmBuild := true; run"
"""

  val npmDev = s"""
if [ -e ./{{managed}}/build-env.sh ]; then
 . ./{{managed}}/build-env.sh
else
 echo "Error: build-env.sh not found. Please run ./{{managed}}/setup.sc first."
 exit 1
fi

echo -n "Waiting for dev server to start."

until [ -e $$SERVER_DEV_PATH ]; do
    echo -n "."
    sleep 2
done

echo ✅

echo "Starting npm dev server for client"
echo " * SCALA_VERSION=$$SCALA_VERSION"
rm -f $$MAIN_JS_PATH
touch $$NPM_DEV_PATH

cd {{modules}}/{{appProjectFolder}}
{{jsPackageManager}} run dev
"""

  val serverRun = s"""
set -e

sbt 'fullstackServer; ~{{serverProjectId}}/reStart'
"""

  val setupSc = s"""
// using javaOptions "--sun-misc-unsafe-memory-access=allow" // Example option to set maximum heap size
//> using dep "com.lihaoyi::os-lib:0.11.8"

import os.*

// First we remove started marked semaphor file.
// This allows to avoid confict when many (3) instances of sbt starts in the same time in the same folder:
//
// - server
// - vite init
// - fastLink
//
removeStartedMarker()

//
//Expected the project id of the ScalaJs application.
//
val app = args.headOption.getOrElse("{{appProjectId}}")

given client: Path = os.pwd / "{{modules}}" / app

if buildSbt isYoungerThan buildEnv then
  println(s"Importing project settings into build-env.sh ($$buildEnv)...")
  os.proc("sbt", "fullstackInit")
    .call(
      cwd = os.pwd,
      stdout = os.ProcessOutput.Readlines(line => println(s"  $$line"))
    )

npmCommand foreach: command =>
  println(s"✨ Installing ($$command) node modules...")
  os.proc("{{jsPackageManager}}", command).call(cwd = client)
  println("Node modules installation complete.")

// Utils && Helpers
def npmCommand(using client: Path): Option[String] =
  if packageLockJson.isMissing then
    println("✨\t- First install")
    Some("install")
  else if packageJson isYoungerThan packageLockJson then
    println("⏫\t- package.json has been modified since the last installation.")
    Some("install")
  else if nodeModule.isOlderThanAWeek then {
    print(s"\\t- 🔎 Node modules already installed but old")
    println("\\n\\t\\t- ⚠️\\t Not installed recently ( > 7 days). Consider reinstalling if issues arise.")
    None
  } else if nodeModule.isMissing then {
    println("🟢 CI")
    Some("ci")
  } else {
    println("\\t- ✅ {{jsPackageManager}} are deps uptodate.")
    None
  }

def buildSbt                            = os.pwd / "build.sbt"
def buildEnv                            = os.pwd / "{{managed}}" / "build-env.sh"
def devMarker                           = os.pwd / "target" / "dev-server-running.marker"
def npmDevMarker                        = os.pwd / "target" / "npm-dev-server-running.marker"
def nodeModule(using client: Path)      = client / "node_modules"
def packageJson(using client: Path)     = client / "package.json"
def packageLockJson(using client: Path) = client / "{{jsPackageManagerLockFile}}"

/** Delete semaphore files to sync multiple sbt launchs.
  */
def removeStartedMarker() =
  if os.exists(devMarker) then os.remove(devMarker)
  if os.exists(npmDevMarker) then os.remove(npmDevMarker)

import scala.math.Ordered.orderingToOrdered

extension (path: Path)
  /** True if something must be reprocessed.
    */
  infix def isYoungerThan(that: Path) =
    if os.exists(that) then
      os.stat(path).mtime > os.stat(that).mtime
    else true

  def exists: Boolean = os.exists(path)

  def isMissing: Boolean = !os.exists(path)

  def isOlderThanAWeek: Boolean =
    os.exists(path) && os.stat(path).mtime.toInstant < java.time.Instant.now().minus(
      7,
      java.time.temporal.ChronoUnit.DAYS
    )
"""

  private val serverTemplate = Map(
    "ci-build.sh" -> ciBuild,
    "dockerPublish.sh" -> dockerPublish,
    "dockerPublishLocal.sh" -> dockerPublishLocal,
    "fullstackRun.sh" -> fullstackRun,
    "serverRun.sh" -> serverRun
  )

  def templates(server: Option[Project]): Map[String, String] = server match {
    case Some(_) => defaultTemplates ++ serverTemplate
    case None    => defaultTemplates
  }

  private val defaultTemplates: Map[String, String] = Map(
    "setup.sc" -> setupSc,
    "fastLink.sh" -> fastLink,
    "npmDev.sh" -> npmDev
  )
}
