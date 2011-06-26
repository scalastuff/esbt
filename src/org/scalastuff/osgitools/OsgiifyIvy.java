package org.scalastuff.osgitools;

import java.io.File;
import java.io.IOException;

public class OsgiifyIvy {

	public static void osgiifyIvy() throws IOException {
		File userHome = new File(System.getProperty("user.home"));
		File ivyHome = new File(userHome, ".ivy2");
		osgiifyIvy(new File(ivyHome, "cache"), new File(ivyHome, "osgi"));
	}
	
	public static void osgiifyIvy(File ivyDir, File targetDir) throws IOException {
		for (File orgDir : ivyDir.listFiles()) {
			if (orgDir.isDirectory()) {
				for (File nameDir : orgDir.listFiles()) {
					if (nameDir.isDirectory()) {
						String symbolicName = orgDir.getName() + "." + nameDir.getName();
						File jarsDir = new File(nameDir, "jars");
						if (jarsDir.isDirectory()) {
							for (File jar : jarsDir.listFiles()) {
								if (jar.getName().startsWith(nameDir.getName() + "-") && jar.getName().endsWith(".jar")) {
									String version = jar.getName().substring(nameDir.getName().length() + 1, jar.getName().length() - ".jar".length());
									Osgiify.osgiify(jar, symbolicName, version, targetDir);
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		osgiifyIvy();
	}
}
