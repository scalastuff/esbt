package org.scalastuff.esbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CopyJars {

	public static final File JARS_DIR = new File(Esbt.ESBT_HOME, "jars");

	public static String copyFile(Dependency dep, File baseDir, String file) throws FileNotFoundException, IOException {
		if (!file.trim().isEmpty()) {
			if (!new File(file).isAbsolute()) {
				file = new File(baseDir, file).toString();
			}
			return copyFile(new File(file), new File(JARS_DIR, dep.organization + "-" + new File(file).getName())).getCanonicalPath();
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
				dest.getParentFile().mkdirs();
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
