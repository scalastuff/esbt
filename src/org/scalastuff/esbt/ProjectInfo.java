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

import static org.scalastuff.esbt.Utils.read;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class ProjectInfo {

	private static final String SBT_FILE = "build.sbt";
	private static final String CLASSPATH_FILE = ".classpath";
	
	private final IProject project;
	private List<String> sbtFile = Collections.emptyList();
	private List<String> classpathFile = Collections.emptyList();
	
	private List<String> cachedSbtFile;

	public ProjectInfo(IProject project) {
		this.project = project;
	}
	
	public IProject getProject() {
		return project;
	}
	
	public File getProjectDir() {
		return new File(project.getLocationURI());
	}
	
	public IFile getSbtFile() {
		return project.getFile(SBT_FILE);
	}
	
	public IFile getClassPathFile() {
		return project.getFile(CLASSPATH_FILE);
	}
	
	public String getOrganization() {
		return getSbtPropertyValue("organization");
	}
	
	public String getName() {
		return getSbtPropertyValue("name");
	}
	
	public String getVersion() {
		return getSbtPropertyValue("version");
	}
	
	public List<String> getSbtFileWithoutProjectDependencies() {
		List<String> result = new ArrayList<String>();
		for (String line : readSbtFile()) {
			Dependency dep = getLibraryDependency(line);
			if (dep != null) {
				ProjectInfo depPrj = WorkspaceInfo.findProject(dep.organization, dep.name, dep.version);
				if (depPrj != null) continue;
			}
			result.add(line);
		}
		return result;
	}
	
	public Set<ProjectInfo> getSbtFileProjectDependencies() {
		Set<ProjectInfo> result = new HashSet<ProjectInfo>();
		for (String line : readSbtFile()) {
			Dependency dep = getLibraryDependency(line);
			if (dep != null) {
				ProjectInfo depPrj = WorkspaceInfo.findProject(dep.organization, dep.name, dep.version);
				if (depPrj != null) result.add(depPrj);
			}
		}
		return result;
	}
	
	public List<Dependency> getLibraryDependencies() {
		List<Dependency> result = new ArrayList<Dependency>();
		for (String line : readSbtFile()) {
			Dependency deps = getLibraryDependency(line);
			if (deps != null) {
				result.add(deps);
			}
		}
		return result;
	}
	
	public Dependency getLibraryDependency(String line) {
		line = getSbtPropertyLine(line, "libraryDependencies");
		if (line.isEmpty()) return null;
		List<String> values = readStrings(line);
		Dependency dependency = new Dependency();
		for (int i = 0; i < values.size(); i++) {
			switch (i) {
			case 0: dependency.organization = values.get(i).trim(); break;
			case 1: dependency.name = values.get(i).trim(); break;
			case 2: dependency.version = values.get(i).trim(); break;
			}
		}
		return dependency;
	}
	
	public boolean isUnderSbtControl() {
		return !readSbtFile().isEmpty() && ClasspathFile.isUnderSbtControl(read(project.getFile(CLASSPATH_FILE)));
	}
	
	public boolean checkUpToDate() {
		cachedSbtFile = null;
		return sbtFile.equals(readSbtFile())
		  &&   classpathFile.equals(read(project.getFile(CLASSPATH_FILE)));
	}

	public void update(InvokeSbt sbt) throws CoreException {
		IFile file = project.getFile(CLASSPATH_FILE);
		List<String> lines = read(file);
		
		// combine sbt project deps with sbt project deps
		Set<ProjectInfo> projectDeps = getSbtFileProjectDependencies();
		List<Dependency> deps = new ArrayList<Dependency>();
		for (Dependency dep : sbt.getDependencies()) {
			ProjectInfo depProject = WorkspaceInfo.findProject(dep.organization, dep.name, dep.version);
			if (depProject != null) {
				projectDeps.add(depProject);
			} else {
				deps.add(dep);
			}
		}
		this.classpathFile = ClasspathFile.update(this, lines, projectDeps, deps);
		if (!lines.equals(classpathFile)) {
			if (file.exists()) {
				file.setContents(Utils.linesInputStream(classpathFile), 0, null);
			} else {
				file.create(Utils.linesInputStream(classpathFile), 0, null);
			}
		}
		this.sbtFile = readSbtFile();
		this.cachedSbtFile = null;
	}
	
	public List<String> readSbtFile() {
		if (cachedSbtFile == null) {
			cachedSbtFile = read(project.getFile(SBT_FILE));
		}
		return cachedSbtFile;
	}
	
	private String getSbtPropertyValue(String property) {
		for (String line : readSbtFile()) {
			line = getSbtPropertyLine(line, property);
				if (line.startsWith("\"") && line.endsWith("\"")) {
				return line.substring(1, line.length() - 1);
			}
		}
		return "";
	}
	
	private String getSbtPropertyLine(String line, String property) {
		line = line.trim();
		if (line.startsWith(property)) {
			line = line.substring(property.length()).trim();
			if (line.startsWith(":=") || line.startsWith("+=")) {
				return line.substring(2).trim();
			}
		}
		return "";
	}
	
	private List<String> readStrings(String line) {
		List<String> result = new ArrayList<String>();
		boolean insideString = false;
		StringBuilder out = new StringBuilder();
		for (int pos = 0; pos < line.length(); pos++) {
			char c = line.charAt(pos);
			if (c == '"') {
				if (insideString) {
					result.add(out.toString());
					out.setLength(0);
				}
				insideString = !insideString;
			} else {
				if (insideString) {
					out.append(c);
				}
			}
		}
		return result;
	}
}
