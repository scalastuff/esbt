package org.scalastuff.esbt;

import static org.scalastuff.esbt.Utils.copy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.scalastuff.osgitools.OsgiManifest;
import org.scalastuff.osgitools.OsgiManifest.Attribute;
import org.scalastuff.osgitools.OsgiManifest.Value;
import org.scalastuff.osgitools.OsgiifyIvy;

public class ManifestFile extends FileContent {

	private final ProjectInfo project;
	private OsgiManifest manifest;
	
	protected ManifestFile(ProjectInfo project) {
	  super(project.getProject().getFile("META-INF/MANIFEST.MF"));
		this.project = project;
  }
	
	@Override
	protected void setLines(List<String> lines) {
		super.setLines(lines);
		manifest = OsgiManifest.read(lines);
	}

	public List<Dependency> write(Set<ProjectInfo> projectDeps, List<Dependency> deps) throws CoreException, IOException {
		
		List<String> lines = new ArrayList<String>(getContent());
		
		if (!isTrue(lines, "Allow-ESBT")) return deps;
		
		for (int i = lines.size() - 1; i >= 0; i--) {
			String line = lines.get(i).trim();
			if (line.isEmpty()) {
				lines.remove(i);
			}
		}
		
//		boolean embedDependencies = isTrue(lines, "Embed-Dependencies");
		
		manifest.getAttribute("Allow-ESBT").setValue("true");
		manifest.getAttribute("Bundle-ManifestVersion").setValue("2", false);
		manifest.getAttribute("Bundle-Name").setValue(project.getName());
		manifest.getAttribute("Bundle-SymbolicName").setValue(getSymbolicName());
		manifest.getAttribute("Bundle-Version").setValue(getVersion());


		// add direct dependencies
		List<String> depLines = new ArrayList<String>();
		Attribute requireBundles = manifest.getAttribute("Require-Bundle");
		for (Dependency dep : project.getDependencies()) {
			String version = dep.version.replace('-', '.');
			String symbolicName = dep.organization + "." + dep.name;
			Value value = requireBundles.addUnique(symbolicName);
			value.setAnnotation("bundle-version", version);
//			depLines.add(symbolicName + ";bundle-version=\"" + version + "\""+(embedDependencies ? ";visibility:=reexport" : ""));
		}
		
		// remove org.osgi.core
		requireBundles.removeValue("org.osgi.core");
		Attribute importPackage = manifest.getAttribute("Import-Package");
		importPackage.addUnique("org.osgi.framework");
		
		
		// copy dep extent into osgi dir
		for (Dependency dep : deps) {
			OsgiifyIvy.osgiify(new File(dep.jar), dep.organization + "." + dep.name, dep.version, false);
		}
		// copy eclipse.osgi bundles
		File pluginsDir = new File(new File(URI.create(System.getProperty("eclipse.home.location"))), "plugins");
		if (pluginsDir.isDirectory()) {
			for (File jar : pluginsDir.listFiles()) {
				if (jar.getName().startsWith("org.eclipse.osgi.") && jar.getName().endsWith(".jar")) {
					copy(jar, new File(OsgiifyIvy.targetDir, jar.getName()), true);
				}
			}
		}

		set(lines, "Require-Bundle", depLines, true);
		
		// add libs
//		List<String> cp = new ArrayList<String>();
//		cp.add(".");
//		libDir.refreshLocal(1, null);
//		if (libDir.exists()) {
//			for (IResource resource : libDir.members()) {
//				if (resource.getName().endsWith(".jar")) {
//					cp.add("lib/" + resource.getName());
//				}
//			}
//		}
//		set(lines, "Bundle-ClassPath", cp, true);
//		super.doWrite(asList(manifest.toString(",\n  ", new StringBuilder()).toString().split("\n")));
		super.doWrite(manifest.write());
//		super.doWrite(lines);
		return deps;
	}

	private String getVersion() {
	  return project.getVersion().replace('-', '.');
  }

	private String getSymbolicName() {
	  return project.getOrganization() + "." + project.getName();
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
