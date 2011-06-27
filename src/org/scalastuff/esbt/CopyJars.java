package org.scalastuff.esbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CopyJars {

	private static File userHome;
	private static File jarsDir;
	
	private static File getJarsDir() throws IOException {
		if (jarsDir == null) {
			userHome = new File(System.getProperty("user.home"));
			jarsDir = new File(userHome, ".sbt/jars");
			jarsDir.mkdirs();
		}
		return jarsDir;
	}

	public static void copyJars(Dependency dep) throws FileNotFoundException, IOException {
		dep.jar = copyFile(dep, dep.jar);
		dep.srcJar = copyFile(dep, dep.srcJar);
	}
	
	private static String copyFile(Dependency dep, String file) throws FileNotFoundException, IOException {
		if (!file.trim().isEmpty()) {
			return copyFile(new File(dep.jar), new File(getJarsDir(), dep.organization + "-" + new File(file).getName())).getCanonicalPath();
		}
		return file;
	}
	
	private static File copyFile(File source, File dest) throws FileNotFoundException, IOException {
		int attempt = 0;
		String destBase = dest.getPath();
		while (!dest.exists()
				||source.lastModified() != dest.lastModified()
				|| source.length() != dest.lastModified()) {
			try {
				Utils.copyStream(new FileInputStream(source), new FileOutputStream(dest));
				dest.setLastModified(source.lastModified());
				break;
			} catch (IOException e) {
				if (attempt++ > 50) {
					throw new IOException("Couldn't copy file " + source + " to " + dest + ": " + e.getMessage(), e);
				}
				dest = new File(destBase + "-" + attempt);
			}
		}
		return dest;
	}
}
