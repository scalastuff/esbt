
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

 - The plugin doesn't get path information from SBT as of yet. 
   It will add the default (maven-inspired) paths when they exist.
   Help on how to do this nicely would be appreciated!
   
