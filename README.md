
Eclipse SBT Plugin
==================

# Features:

- Based on SBT 0.10
- automatically adds dependencies to .classpath
- adds dependency sources
- resolves dependencies to workspace projects
- excludes scala library when scala IDE is installed
- non-intrusive: respects existing content in .classpath
- graceful: no menu entries are shown when project doesn't contain build.sbt

# Installation

Update site:

	https://raw.github.com/scalastuff/updatesite/master

# Release Notes

## 0.10.7

- Fixed bug with incomplete projects being copied to tmp dir
- Only copy project dir when needed (when project dependencies are detected)
- Robust against jar files locked by eclipse
- Provisional OSGi support (create META-INF/MANIFEST.MF with line Allow-ESBT: true)

## 0.10.3

- Added support for library sources
- Dependencies are now ordered by name

