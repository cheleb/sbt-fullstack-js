# sbt-fullstack-js Script Management Implementation Plan

This document outlines the self-contained components required to implement the Script Management feature for the `sbt-fullstack-js` plugin.

**Task 1**: [x] Implement Managed Header Recognition and Ownership Check
**Description**: Create a utility to determine if a script file is managed by the plugin. This involves checking if the file exists and if its first line contains the specific "managed" header string.
**Purpose**: To respect developer autonomy and avoid overwriting custom scripts as per the "header-based" ownership model.
**Behavior**: 
* `isManaged(file: java.io.File): Boolean`
* Returns `true` if the file does not exist (new file).
* Returns `true` if the file exists and contains `# DO NOT EDIT: This file is managed by sbt-fullstack-js plugin`.
* Returns `false` if the file exists and does not contain the header.
**Requirements**: 
* Use `sbt.io.IO.readLines` to check the first line.
* Handle empty files gracefully.
**Testing criteria**:
* Happy path: New file returns `true`.
* Happy path: File with header returns `true`.
* Edge case: File without header returns `false`.
* Edge case: Empty file returns `false`.

**Task 2**: Implement Template Substitution Engine
**Description**: Create a simple string replacement engine that substitutes placeholders in templates with project-specific variables.
**Purpose**: To allow dynamic generation of scripts based on the sbt project configuration (e.g., module names, main classes).
**Behavior**: 
* `substitute(template: String, variables: Map[String, String]): String`
* Replaces all occurrences of `{{VARIABLE_NAME}}` with the corresponding value from the map.
**Requirements**: 
* Support at least the following variables: `{{serverProjectId}}`, `{{appProjectId}}`, `{{mainClass}}`.
* Use regex for robust substitution.
**Testing criteria**:
* Happy path: Multiple placeholders are correctly replaced.
* Edge case: Placeholders missing from the map remain unchanged or result in a warning.
* Edge case: Variables with special shell characters are handled (though mostly these will be alphanumeric).

**Task 3**: Define Default Script Templates
**Description**: Provide the default content for the suite of managed scripts: `run`, `build`, `docker`, and `setup.sc`.
**Purpose**: To provide a standardized "batteries-included" developer experience.
**Behavior**: 
* A set of strings or resource files containing the Bash/Ammonite code for each script.
* Each template must start with the "managed" header.
**Requirements**: 
* `setup.sc`: Responsible for environment initialization.
* `run`: Fullstack application execution.
* `build`: Production asset compilation.
* `docker`: Docker image publication logic.
* Use Bash for `run`, `build`, `docker`.
* Use Ammonite/Scala for `setup.sc`.
**Testing criteria**:
* Verification that all templates contain the mandatory header.
* Verification that all templates use the defined placeholder syntax.

**Task 4**: Implement Filesystem Operations and Permissions Handler
**Description**: Create a module to handle the physical writing of scripts to the `scripts/` directory and setting the executable bit.
**Purpose**: To automate file management and ensure scripts are ready to use.
**Behavior**: 
* `writeScript(scriptsDir: java.io.File, name: String, content: String): Unit`
* Creates the directory if it doesn't exist.
* Writes the content to the file.
* Sets file permissions to `rwxr-xr-x`.
**Requirements**: 
* Use `sbt.io.IO.write` for writing.
* Use `sbt.io.IO.setPermissions` or `java.io.File.setExecutable(true)` for permissions.
**Testing criteria**:
* Happy path: File is created in the correct location with correct content.
* Happy path: File is executable after writing.
* Edge case: Attempting to write to a read-only directory should fail with a descriptive error.

**Task 5**: Implement sbt Plugin Task and Settings
**Description**: Integrate the logic into the sbt plugin by defining the `fullstackScripts` task and associated settings.
**Purpose**: To expose the script management functionality to the user via the sbt CLI.
**Behavior**: 
* Define `fullstackScripts` task.
* Define `fullstackScripts / templates` setting (Map of script name to template string).
* Define `fullstackScripts / variables` setting (Map of variable name to value).
* When run, the task collects variables, checks ownership for each script, substitutes templates, and writes them to disk.
**Requirements**: 
* Task should log which files are updated and which are skipped.
* Task should determine the project root directory correctly.
**Testing criteria**:
* Happy path: Running `fullstackScripts` in a fresh project populates the `scripts/` directory.
* Happy path: Renaming a project in `build.sbt` and rerunning the task updates the scripts.
* Edge case: Overriding a template in `build.sbt` results in the custom template being used.
