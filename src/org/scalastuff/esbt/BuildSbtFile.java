package org.scalastuff.esbt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuildSbtFile extends AbstractFile {

	protected BuildSbtFile(ProjectInfo project) {
	  super(project.getProject().getFile("build.sbt"));
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
	

	public Set<ProjectInfo> getProjectDependencies() {
		Set<ProjectInfo> result = new HashSet<ProjectInfo>();
		for (Dependency dep : getLibraryDependencies()) {
			ProjectInfo depPrj = WorkspaceInfo.findProject(dep.organization, dep.name, dep.version);
			if (depPrj != null) result.add(depPrj);
		}
		return result;
	}

	public List<Dependency> getLibraryDependencies() {
		List<Dependency> result = new ArrayList<Dependency>();
		for (int i = 0; i < getContent().size(); i++) {
			String line = getContent().get(i);
			if (line.trim().startsWith("libraryDependencies")) {
				for (int braces = countBraces(line), j = 0; braces > 0 && i < getContent().size(); i++, j++) {
					if (j > 0) line = line + getContent().get(i);
				}
				
				String[] depStrings = line.split(",");
				if (depStrings.length > 0) {
					for (int count = countBraces(depStrings[0]); count > 0; count--) {
						depStrings[depStrings.length - 1] = removeLast(depStrings[depStrings.length - 1], ')'); 
					}
					for (String depString : depStrings) {
						result.add(getLibraryDependency(depString));
					}
				}
			}
		}
		return result;
	}	

	public static Dependency getLibraryDependency(String depString) {
		List<String> values = readStrings(depString);
		Dependency dependency = new Dependency();
		dependency.crossCompiled = depString.contains("%%");
		for (int i = 0; i < values.size(); i++) {
			switch (i) {
			case 0: dependency.organization = values.get(i).trim(); break;
			case 1: dependency.name = values.get(i).trim(); break;
			case 2: dependency.version = values.get(i).trim(); break;
			case 3: dependency.qualifier = values.get(i).trim(); break;
			}
		}
		dependency.rest = afterLast(depString, '"').trim(); 
		return dependency;
	}
	
	public List<String> getContentWithoutProjectDependencies() {
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < getContent().size(); i++) {
			String line = getContent().get(i);
			if (line.trim().startsWith("libraryDependencies")) {
				for (int braces = countBraces(line); braces > 0 && i < getContent().size(); i++) {
					line = line + getContent().get(i);
				}
			} else {
				result.add(line);
			}
		}
		for (Dependency dep : getLibraryDependencies()) {
			if (WorkspaceInfo.findProject(dep.organization, dep.name, dep.version) == null) {
				result.add("");
				result.add("libraryDependencies += \"" + dep.organization + "\" " + (dep.crossCompiled ? "%%" : "%") + " \"" + dep.name + "\" % \"" + dep.version + (!dep.qualifier.equals("") ? "\" % \"" + dep.qualifier + "\"" : "\"") + " " + dep.rest);
			}
		}
		return result;
	}
	
	private String getSbtPropertyValue(String property) {
		for (String line : getContent()) {
			line = getSbtPropertyLine(line, property);
				if (line.startsWith("\"") && line.endsWith("\"")) {
				return line.substring(1, line.length() - 1);
			}
		}
		return "";
	}
	
	private static String getSbtPropertyLine(String line, String property) {
		line = line.trim();
		if (line.startsWith(property)) {
			line = line.substring(property.length()).trim();
			if (line.startsWith(":=") || line.startsWith("+=")) {
				return line.substring(2).trim();
			}
		}
		return "";
	}

	
	private static List<String> readStrings(String line) {
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
	
	private static int countBraces(String line) {
		int count = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '(') count++;
			else if (line.charAt(i) == ')') count--;
		}
		return count;
	}
	
	private static String afterLast(String s, char c) {
		int index = s.lastIndexOf(c);
		if (index < 0) return "";
		else return s.substring(index + 1);
	}

	private static String removeLast(String s, char c) {
		int index = s.lastIndexOf(c);
		if (index < 0) return s;
		else return s.substring(0, index) + s.substring(index + 1);
  }
}
