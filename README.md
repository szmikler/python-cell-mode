# Python Cell Mode

This plugin provides actions to allow executing Python code cells in JetBrains IDEs, much like in Jupyter Notebooks.

A "code cell" is a block of lines, typically delimited by `# %%`, for example:

```
# %%

print("foo")

if True:
    print("bar")

# %%
```

## Changes in this fork

Compared to the original plugin, this fork adds:

New featues:
- Makes every cell foldable, the delimiter line staying visible.
- Draws a horizontal separator above every cell delimiter.
- Marks the active cell with a thin vertical bar.

Other changes:
- Builds with Gradle and the IntelliJ Platform Gradle Plugin against PyCharm 2026.2. Removed old API usages that no longer exist.
- Renamed the plugin to "Python Cell Mode" since it works in any JetBrains IDE with the Python plugin, not only PyCharm.
- Uses `# %%` as the default cell delimiter (Spyder, VS Code and Jupytext style) instead of `##`.
- Changes the default shortcuts: Run Cell And Move To Next is `Shift+Ctrl+Enter`, Run Cell is `Ctrl+Alt+Enter`.
- Fixes the missing-icon error logged at startup for the Run Cell And Move To Next action.

## Description

The plugin options allow you to specify your own regular expression to delimits code cells. 

This plugin provides 3 actions under the Code menu, and you can assign keyboard shortcuts to each:

- Run Cell: run the current cell
- Run Cell and Move Next: run the current cell and advance cursor to next cell
- Run Line under the caret

When using `Run Cell and Move Next` and there is no next cell, a delimiter (which can be specified in the options, but which is unrelated to the above described regular expression) will be inserted in the source code.

The cell can be sent to either :

- the internal IPython console
- an external IPython kernel running in a tmux

The second option allows you to have a working interactive matplotlib in an external IPython process.

Check the "Python Cell Mode" settings in the preferences to switch between the two modes.

## Installation

Install from a locally built zip:

1. Run `./gradlew buildPlugin` and take `build/distributions/python-cellmode-<version>.zip`
2. In the IDE, go to "Preferences", search for "plugin". Click on "Install from disk" and choose the zip
3. Restart the IDE and use the new actions in the "Code" menu

## Developing the plugin

The build uses Gradle with the [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html).
The target PyCharm version is declared in `build.gradle.kts` and downloaded automatically. A JDK 21 or newer is
required (the JetBrains Runtime shipped with any JetBrains IDE works: set `JAVA_HOME` to its `jbr` directory).

- `./gradlew buildPlugin` builds the plugin zip into `build/distributions/`
- `./gradlew runIde` starts a sandboxed PyCharm with the plugin installed
- `./gradlew verifyPlugin` checks API compatibility with the target IDE

Opening the repo in IntelliJ IDEA imports the Gradle project directly.

