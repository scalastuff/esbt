package org.scalastuff.esbt;

import static java.util.Arrays.asList;
import static org.scalastuff.esbt.Utils.qname;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ManifestFile extends AbstractFile {

	private final ProjectInfo project;

	protected ManifestFile(ProjectInfo project) {
	  super(project.getProject().getFile("META-INF/MANIFEST.MF"));
		this.project = project;
  }

	public List<Dependency> write(Set<ProjectInfo> projectDeps, List<Dependency> deps) throws CoreException, IOException {
		
		List<String> lines = new ArrayList<String>(getContent());
		
		if (!isTrue(lines, "Generate-From-SBT")) return deps;
		
		for (int i = lines.size() - 1; i >= 0; i--) {
			String line = lines.get(i).trim();
			if (line.isEmpty()) {
				lines.remove(i);
			}
		}
		
		boolean embedDependencies = isTrue(lines, "Embed-Dependencies");
		
		
		set(lines, "Generate-From-SBT", asList("true"), true);
		set(lines, "Bundle-ManifestVersion", asList("2"), false);
		set(lines, "Bundle-ManifestVersion", asList("2"), false);
		set(lines, "Bundle-Name", asList(project.getSbtFile().getName()), true);
		set(lines, "Bundle-SymbolicName", asList(getSymbolicName()), true);
		set(lines, "Bundle-Version", asList(getVersion()), true);
		
		// remove old libs
		IFolder libDir = project.getProject().getProject().getFolder("lib");
		if (libDir.exists()) {
			for (IResource resource : libDir.members()) {
				if (resource.getName().startsWith("embedded.")) {
					resource.delete(true, null);
				}
			}
		}
		
		List<String> depLines = new ArrayList<String>();
		for (Dependency dep : deps) {
			boolean depFound = false;
			for (ProjectInfo prjDep : projectDeps) {
				if (checkDepJar(prjDep.getProjectDir(), dep)) {
					depLines.add(dep.osgiSymbolicName + ";bundle-version=\"" + dep.osgiBundleVersion + "\""+(embedDependencies ? ";visibility:=reexport" : ""));
					depFound = true;
					break;
				}
			}
			if (!depFound) {
				if (checkDepJar(new File(dep.jar), dep)) {
					depLines.add(dep.osgiSymbolicName + ";bundle-version=\"" + dep.osgiBundleVersion + "\""+(embedDependencies ? ";visibility:=reexport" : ""));
					depFound = true;
				}
			}
			if (!depFound && embedDependencies) {
				File lib = new File(project.getProjectDir(), "lib");
				lib.mkdirs();
				Utils.copyStream(new FileInputStream(dep.jar), new FileOutputStream(new File(lib, "embedded." + Utils.qname(dep.organization, dep.name, dep.version, dep.qualifier) + ".jar")));
				libDir.refreshLocal(0, null);
			}
		}
		libDir.refreshLocal(1, null);
		set(lines, "Require-Bundle", depLines, true);
		
		// add libs
		List<String> cp = new ArrayList<String>();
		cp.add(".");
		if (libDir.exists()) {
			for (IResource resource : libDir.members()) {
				if (resource.getName().endsWith(".jar")) {
					cp.add("lib/" + resource.getName());
				}
			}
		}
		set(lines, "Bundle-ClassPath", cp, true);
		
		super.doWrite(lines);
		return deps;
	}

	private String getVersion() {
	  return project.getSbtFile().getVersion();
  }

	private String getSymbolicName() {
	  return project.getSbtFile().getOrganization() + "." + project.getSbtFile().getName();
  }
	
	private boolean checkDepJar(File root, Dependency dep) {
		try {
			if (root.isFile()) {
				JarFile jarFile = new JarFile(root);
				try {
					Manifest manifest = jarFile.getManifest();
					return checkDepJar(manifest, dep);
				} finally {
					jarFile.close();
				}
			} else if (root.isDirectory()) {
				Manifest manifest = new Manifest(new FileInputStream(new File("META-INF/MANIFEST.MF")));
				return checkDepJar(manifest, dep);
			}
		  return false;
		} catch (IOException e) {
			return false;
		}
  }

	private boolean checkDepJar(Manifest manifest, Dependency dep) {
	  if (manifest != null) {
	  	Attributes attrs = manifest.getMainAttributes();
	  	String symbolicName = attrs.getValue("Bundle-SymbolicName");
			String qname = qname(dep.organization, dep.name, "", "");
			String version = dep.version.replace('-', '.');
			String bundleVersion = attrs.getValue("Bundle-Version");
			if (((dep.organization + "." + dep.name).equals(symbolicName) || qname.equals(symbolicName) || dep.name.equals(symbolicName))
	  			&& version.equals(bundleVersion)) {
	  		System.out.println("Found OSGi dependency: " + attrs.getValue("Bundle-Name"));
	  		dep.osgiSymbolicName = symbolicName;
	  		dep.osgiBundleVersion = bundleVersion;
	  		return true;
	  	}
	  }
	  return false;
  }

	private static boolean isTrue(List<String> lines, String property) {
		List<String> list = get(lines, property);
		return (!list.isEmpty() && list.get(0).toLowerCase().equals("true"));
	}
	
	private static List<String> get(List<String> lines, String property) {
		List<String> result = new ArrayList<String>();
		for (int index = 0; index < lines.size(); index++) {
			String line = lines.get(index);
			if (line.startsWith(property)) {
				line = line.substring(property.length()).trim();
				if (line.startsWith(":")) {
					result.add(line.substring(1).trim());
				}
			}
		}
		return result;
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
			lines.add(index++, property + ": ");
			for (int i = 0; i < values.size(); i++) {
				lines.add(index++, "  " + values.get(i)+ (i < values.size() - 1 ? "," : ""));
			}
		}
		return result;
	}
}
