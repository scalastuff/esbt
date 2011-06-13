package org.scalastuff.esbt;

import static org.scalastuff.esbt.Utils.read;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SbtPlugin {
	
	private static File userHome;
	
	public static void createSbtPlugin() throws IOException {
		// for debugging purposes
		if (userHome == null || true) {
			userHome = new File(System.getProperty("user.home"));
			userHome.delete();
			File pluginDir = new File(userHome, ".sbt/plugins");
			pluginDir.mkdirs();
			createBuildSbt(pluginDir);
			createSbtEclipsePluginFile(pluginDir);
		}
	}
	
	private static void createBuildSbt(File pluginDir) throws IOException {
		File file = new File(pluginDir, "build.sbt");
		if (!file.exists()) {
			file.createNewFile();
		}
		List<String> lines = read(new FileInputStream(file));
		for (String line : lines) {
			if (line.startsWith(line)) {
				return;
			}
		}
		lines.add("");
		lines.add("sbtPlugin := true");
		Utils.write(new FileOutputStream(file), lines);
	}
	
	private static void createSbtEclipsePluginFile(File pluginDir) throws IOException {
		InputStream is = SbtPlugin.class.getResourceAsStream("SbtEclipsePlugin.scala.source");
		if (is == null) {
			throw new IOException("Coulnd't find SbtEclipsePlugin.scala.source");
		}
		File destFile = new File(pluginDir, "SbtEclipsePlugin.scala");
		OutputStream os = new FileOutputStream(destFile);
		try {
			byte[] b = new byte[2000];
			int read;
			while ((read = is.read(b)) != -1) {
				os.write(b, 0, read);
			}
		} finally {
			is.close();
			os.close();
		}
	}
}
