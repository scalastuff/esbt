/**
 * Copyright (c) 2011 ScalaStuff.org (joint venture of Alexander Dvorkovyy and Ruud Diterwich)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.scalastuff.esbt;

import static org.scalastuff.esbt.Utils.indexOfLineContaining;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClasspathFile {

	private static final String PLUGIN_INDICATION = "<!-- Eclipse SBT Plugin -->";
	
	public static boolean isUnderSbtControl(List<String> lines) {
		return true;//lines.isEmpty() || lines.contains(PLUGIN_INDICATION);
	}
	
	public static List<String> update(List<String> lines, Set<ProjectInfo> projectDeps, List<Dependency> deps) {
		lines = new ArrayList<String>(lines);
		if (lines.isEmpty()) {
			lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			lines.add(PLUGIN_INDICATION);
			lines.add("<classpath>");
			lines.add("	<classpathentry kind=\"output\" path=\"bin\"/>");
			lines.add("</classpath>");
		}
		for (Dependency dep : deps) {
			if (dep.jar.trim().equals("")) continue;
			int line = indexOfLineContaining(lines, dep.jar);
			if (line != -1) {
				lines.remove(line);
			}
		}
		for (int i = lines.size() - 1; i >= 0; i--) {
			if (lines.get(i).contains("from=\"sbt\"")) {
				lines.remove(i);
			}
		}
		
		int line = indexOfLineContaining(lines, "</classpath>");
		if (line < 0) {
			line = lines.size();
		}
		boolean scalaContainer = indexOfLineContaining(lines, "SCALA_CONTAINER") != -1;
		for (Dependency dep : deps) {
			if (scalaContainer && dep.name.equals("scala-library")) continue;
			lines.add(line, "  <classpathentry from=\"sbt\" kind=\"lib\" path=\"" + dep.jar.trim() + "\"" + (!dep.srcJar.trim().isEmpty()? " sourcepath=\"" + dep.srcJar.trim() + "\"" : "") + "/>");
			line++;
		}
		for (ProjectInfo dep : projectDeps) {
			//	<classpathentry combineaccessrules="false" kind="src" path="/MyLibrary"/>

			lines.add(line, "  <classpathentry from=\"sbt\" combineaccessrules=\"false\" kind=\"src\" path=\"/" + dep.getProject().getName() + "\"/>");
			line++;
		}
		
		return lines;
	}
 }
