# sbt-fullstack-js Script Management Design

## Overview
The `sbt-fullstack-js` plugin provides a built-in mechanism to generate and maintain a standardized suite of bash scripts within a `scripts/` directory at the project root. These scripts facilitate common developer workflows such as local environment setup, running the fullstack application, building production assets, and publishing Docker images. This feature ensures that the helper scripts stay in sync with the sbt project configuration while allowing developers to opt-out of management for specific scripts or provide their own templates.

## User Flows

### Initialization and Script Generation
The primary entry point is a new sbt task (e.g., `fullstackScripts`). When executed, the plugin identifies the root of the project and creates a `scripts/` directory if it doesn't already exist. It then populates this directory with a set of default scripts: `run`, `build`, `docker`, and `setup`. Each generated script includes a mandatory header comment: `# DO NOT EDIT: This file is managed by sbt-fullstack-js plugin`.

### Configuration-Aware Updates
The generated scripts are dynamic. For example, the `setup.sc` script is generated with the correct Scala-JS project ID pre-filled. If a developer renames the server or app modules in their `build.sbt`, running the generation task again will automatically update the affected scripts to reflect the new module names.

### Ownership and Customization
The plugin respects developer autonomy through a "header-based" ownership model. If a developer wishes to manually take control of a specific script, they simply remove the "managed" header comment. From that point on, the plugin will skip that file during subsequent generation runs, preventing any accidental overwrites. For more structured customization, developers can provide their own script templates directly within the sbt settings, allowing them to extend or replace the default behavior project-wide.

### Permissions Management
The plugin automatically handles file system permissions, ensuring that all generated scripts are marked as executable (`chmod +x`). This eliminates the common friction of having to manually fix permissions after script generation or update.

## Behavior

*   **Skip Strategy:** The plugin will never overwrite a file that does not contain the "managed" header, ensuring that existing custom scripts are safe by default.
*   **Template Injection:** Custom templates provided via sbt settings can use a placeholder syntax to inject project variables like module IDs, main classes, and Scala versions.
*   **Shell Support:** All generated scripts are targeted at the Bash shell for maximum compatibility across Linux and macOS environments.
*   **Setup Integration:** The managed `setup.sc` continues to be responsible for generating local environment files (like `build-env.sh`), acting as a bridge between the sbt configuration and the shell environment.

## Usability Summary
This feature significantly reduces the "boilerplate" of setting up a new fullstack project by providing a "batteries-included" set of dev-ops scripts. It balances automation with flexibility: the plugin does the heavy lifting of keeping scripts up-to-date with the sbt model, but gracefully steps aside as soon as a developer decides to customize a script manually. The use of sbt settings for template customization provides a clean, version-controlled way to manage scripts across different team members and environments.

## Decisions
- Use a "managed" header comment to track file ownership.
- Skip files without the header (no "force" overwrite).
- Manage `run`, `build`, `docker`, and `setup` scripts by default.
- Use Bash as the target shell.
- Provide template customization via sbt settings.

## Open Questions
- What is the exact syntax for variable substitution in templates?
- Should the plugin warn when it skips a file that it "expected" to manage?
- Are there any other scripts (like `test` or `lint`) that should be added to the default set?
