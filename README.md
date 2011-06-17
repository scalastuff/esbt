
Eclipse SBT Plugin
==================

# Features:

- Based on SBT 0.10
- automatically adds dependencies to .classpath
- resolves dependencies to workspace projects
- excludes scala library when scala IDE is installed
- non-intrusive: respects existing content in .classpath
- graceful: no menu entries are shown when project doesn't contain build.sbt

# Installation

Update site:

	https://raw.github.com/scalastuff/updatesite/master

# Notes

 - Custom scala source paths (other than e.g. /src/main/scala) or
   not supported. The plugin doesn't get this information from SBT as of yet. 
   Help on how to get this information from within an SBT plugin would be appreciated!
   
