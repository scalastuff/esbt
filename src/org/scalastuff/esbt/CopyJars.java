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
			jarsDir = new File(userHome, ".esbt/jars");
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
			return copyFile(new File(file), new File(getJarsDir(), dep.organization + "-" + new File(file).getName())).getCanonicalPath();
		}
		return file;
	}
	
	private static File copyFile(File source, File dest) throws FileNotFoundException, IOException {
		int attempt = 0;
		String destPrefix = dest.getPath();
		String destPostfix = "";
		int i = destPrefix.lastIndexOf('.');
		if (i >= 0) {
			destPostfix = destPrefix.substring(i);
			destPrefix = destPrefix.substring(0, i);
		}
		while (!dest.exists()
				||source.lastModified() != dest.lastModified()
				|| source.length() != dest.lastModified()) {
			try {
				Utils.copy(new FileInputStream(source), new FileOutputStream(dest));
				dest.setLastModified(source.lastModified());
				break;
			} catch (IOException e) {
				if (attempt++ > 50) {
					throw new IOException("Couldn't copy file " + source + " to " + dest + ": " + e.getMessage(), e);
				}
				dest = new File(destPrefix + "-" + attempt + destPostfix);
			}
		}
		return dest;
	}
}
