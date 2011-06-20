package org.scalastuff.esbt;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.CoreException;

public class ManifestFile extends AbstractFile {

	private final ProjectInfo project;

	protected ManifestFile(ProjectInfo project) {
	  super(project.getProject().getFile("META-INF/MANIFEST.MF"));
		this.project = project;
  }

	public void write(Set<ProjectInfo> projectDeps, List<Dependency> deps) throws CoreException, IOException {
		
		boolean foundSbtTag = false;
		List<String> lines = new ArrayList<String>(getContent());
		for (int i = lines.size() - 1; i >= 0; i--) {
			if (lines.get(i).trim().startsWith("Generated-From-SBT:")) {
				foundSbtTag = true;
			}
			if (lines.get(i).trim().isEmpty()) {
				lines.remove(i);
			}
		}
		if (!lines.isEmpty() && !foundSbtTag) return;
		
		
		set(lines, "Generated-From-SBT", asList("true"), false);
		set(lines, "Bundle-ManifestVersion", asList("2"), false);
		set(lines, "Bundle-ManifestVersion", asList("2"), false);
		set(lines, "Bundle-Name", asList(project.getSbtFile().getName()), true);
		set(lines, "Bundle-SymbolicName", asList(getSymbolicName()), true);
		set(lines, "Bundle-Version", asList(getVersion()), true);
		
		List<String> depLines = new ArrayList<String>();
		for (Dependency sbtDep : project.getSbtFile().getLibraryDependencies()) {
			for (Dependency dep : deps) {
				if (checkDepJar(new File(dep.jar), sbtDep.organization, sbtDep.name, sbtDep.version)) {
					depLines.add(dep.organization + "." + dep.name + ";bundle-version=\"" + dep.version + "\"");
				}
			}
		}
		for (ProjectInfo prj : projectDeps) {
			if (checkDepJar(prj.getProjectDir(), prj.getSbtFile().getOrganization(), prj.getSbtFile().getName(), prj.getSbtFile().getVersion())) {
				depLines.add(prj.getSbtFile().getOrganization() + "." + prj.getSbtFile().getName() + ";bundle-version=\"" + prj.getSbtFile().getVersion() + "\"");
			}
		}
		set(lines, "Require-Bundle", depLines, true);
		super.doWrite(lines);
	}

	private String getVersion() {
	  return project.getSbtFile().getVersion();
  }

	private String getSymbolicName() {
	  return project.getSbtFile().getOrganization() + "." + project.getSbtFile().getName();
  }
	
	private boolean checkDepJar(File root, String organization, String name, String version) {
		try {
			if (root.isFile()) {
				JarFile jarFile = new JarFile(root);
				try {
					Manifest manifest = jarFile.getManifest();
					return checkDepJar(manifest, organization, name, version);
				} finally {
					jarFile.close();
				}
			} else if (root.isDirectory()) {
				Manifest manifest = new Manifest(new FileInputStream(new File("META-INF/MANIFEST.MF")));
				return checkDepJar(manifest, organization, name, version);
			}
		  return false;
		} catch (IOException e) {
			return false;
		}
  }

	private boolean checkDepJar(Manifest manifest, String organization, String name, String version) {
	  if (manifest != null) {
	  	Attributes attrs = manifest.getMainAttributes();
	  	System.out.println(" Searching OSGi dependency: " + attrs.getValue("Bundle-SymbolicName"));
	  	System.out.println(" Searching OSGi dependency: " + attrs.getValue("Bundle-Version"));
	  	if ((organization + "." + name).equals(attrs.getValue("Bundle-SymbolicName"))
	  			&& version.equals(attrs.getValue("Bundle-Version"))) {
	  		System.out.println("Found OSGi dependency: " + attrs.getValue("Bundle-Name"));
	  		return true;
	  	}
	  }
	  return false;
  }

	private static List<String> set(List<String> lines, String property, List<String> values, boolean replaceExisting) {
		List<String> result = new ArrayList<String>();
		int index = 0;
		for (; index < lines.size(); index++) {
			String line = lines.get(index);
			if (line.startsWith(property)) {
				line = line.substring(property.length()).trim();
				if (line.startsWith(":")) {
					if (!replaceExisting) {
						return result;
					}
					String stringResult = "";
					line = "  " + line.substring(1);
					while (line.isEmpty() || Character.isWhitespace(line.charAt(0))) {
						stringResult += line;
						lines.remove(index);
						if (index < lines.size()) {
							line = lines.get(index);
						} else {
							line = "QQ";
						}
					}
					for (String resultValue : stringResult.split(",")) {
						resultValue = resultValue.trim();
						if (!resultValue.isEmpty()) {
							result.add(resultValue);
						}
					}
					break;
				}
			}
		}
		if (values.size() == 1) {
			lines.add(index, property + ": " + values.get(0));
		} else if (values.size() > 1) {
			lines.add(index++, property + ":");
			for (int i = 0; i < values.size(); i++) {
				lines.add(index++, "  " + values.get(i)+ (i < values.size() - 1 ? "," : ""));
			}
		}
		return result;
	}
}
