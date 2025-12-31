# sbt-fullstack-js

An sbt plugin to simplify the development and build process of full-stack Scala.js applications.

## Overview

`sbt-fullstack-js` streamlines the integration between an sbt-based backend and a Scala.js-based frontend, often bundled with tools like Vite. It provides utilities for generating build environments and integrating `npm` build processes into the sbt lifecycle.

## Features

- **Automated Setup**: Generates `scripts/target/build-env.sh` to share build-time information (like Scala version and output paths) with external scripts or VS Code tasks.
- **Resource Integration**: Automatically runs `npm run build` and includes the output in the backend's managed resources when building for production or Docker.
- **On-Load Hooks**: Automatically triggers setup tasks when sbt starts, ensuring your development environment is always up-to-date.

## Installation

Add the following to your `project/plugins.sbt`:

```scala
addSbtPlugin("dev.cheleb" % "sbt-fullstack-js" % "VERSION")
```

Replace `VERSION` with the latest version.

## Configuration

In your `build.sbt`, enable the plugin and configure the required settings:

```scala
lazy val root = (project in file("."))
  .enablePlugins(FullstackJsPlugin)
  .settings(
    scalaJsProject := client, // The Scala.js project
    publicFolder := "public"   // The folder where npm build outputs files (default: "public")
  )

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  // ... other Scala.js settings
```

### Settings

- `scalaJsProject`: (Required) The sbt project representing the Scala.js frontend.
- `publicFolder`: (Default: `"public"`) The sub-directory within `resourceManaged` where static files from `npm run build` will be placed.

## Usage

### Environment Variables

The plugin's behavior can be toggled using the `INIT` environment variable:

- `INIT=setup`: Triggers the generation of `scripts/target/build-env.sh`. This is typically used by development scripts or VS Code tasks.
- `INIT=server`: Creates a `target/dev-server-running.marker` file, indicating the server has started.
- `INIT=FullStack` or `INIT=Docker`: Enables the `resourceGenerators` that run `npm run build` and include the results in the backend classpath.

### Tasks

- `setup`: Manually triggers the generation of the build environment script.

## Development

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).

## License

This project is licensed under the Apache-2.0 License.
