Eclipse SBT Plugin
==================

# Features:

- Based on SBT 0.10.1
- automatically downloads dependencies using SBT
- automatically updates eclipse project configuration
- attaches sources of dependencies that have withSources()
- resolves dependencies between workspace projects
- plays nice with scala IDE (e.g. excludes scala library)
- non-intrusive: respects existing content in .classpath
- graceful: no menu entries are shown when project doesn't contain SBT files
- smart copying of jars to allow updating locked jars

# Installation

Update site:

	https://raw.github.com/scalastuff/updatesite/master

# Usage

Create an SBT configuration for your project (build.sbt or project/X.scala). 
Any change to SBT files will trigger esbt to update dependencies and reconfigure the eclipse project. 
One can manually trigger the update by choosing "SBT Update Project Configuration" from the project context menu.

One can issue an SBT command by choosing "SBT Command..." from the project context menu.

# Release Notes

## 0.11.0

- Based on SBT 0.11
- Included fix from Pablo Lalloni

## 0.10.9

- Based on SBT 0.10.1
- build.sbt is no longer required nor parsed
- Project settings (name, version , source directories) are now correctly obtained through SBT
- Project is renamed automatically to organization.name. Useful for fresh 'trunk' checkouts
- Automatically adds scala nature, builder and classpath entries

## 0.10.7

- Fixed bug with incomplete projects being copied to tmp dir
- Only copy project dir when needed (when project dependencies are detected)
- Robust against jar files locked by eclipse
- Provisional OSGi support (create META-INF/MANIFEST.MF with line Allow-ESBT: true)

## 0.10.3

- Added support for library sources
- Dependencies are now ordered by name

