package org.scalastuff.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.scalastuff.esbt.Utils;

public class Osgiify {

	public static File osgiify(File sourceFile, String symbolicName, String version, File targetDir, boolean release) throws IOException {
		
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		// version should contain dots only
		version = version.replace('-', '.');
		
		// construct targetFile
		File targetFile = new File(targetDir, symbolicName + "-" + version + ".jar");

		// if target file exists and is up to date, we're done
		if (targetFile.exists() && sourceFile.lastModified() == targetFile.lastModified()) {
			return targetFile;
		}
		
		JarFile sourceJarFile = new JarFile(sourceFile);
		try {
			Manifest manifest = sourceJarFile.getManifest();
			if (manifest != null) {

				// prevent overwrite released bundles
				if (targetFile.exists() && release && !version.endsWith("SNAPSHOT")) {
					return targetFile;
				}
			} else {
				manifest = new Manifest();
			}
			
			if (symbolicName.equals(getValue(manifest, "Bundle-SymbolicName"))
	  	&&  version.equals(getValue(manifest, "Bundle-Version"))) {
	  		Utils.copyStream(new FileInputStream(sourceFile), new FileOutputStream(targetFile));
	  		return targetFile;
			} else {
				manifest.getMainAttributes().putValue("Bundle-SymbolicName", symbolicName);
				manifest.getMainAttributes().putValue("Bundle-Version", version);
				if (manifest.getMainAttributes().getValue("Bundle-ManifestVersion") == null) {
					manifest.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
				}
				if (getValue(manifest, "Export-Package") == null) {
					StringBuilder out = new StringBuilder();
					for (String pkg : findPackages(sourceFile)) {
						out.append(out.length() == 0 ? "" : ",");
						out.append(pkg).append(";version=\"" + version + "\"");
					}
					manifest.getMainAttributes().putValue("Export-Package", out.toString());
				}
				copyJar(sourceFile, targetFile, manifest);
				return targetFile;
			}
		} finally {
			sourceJarFile.close();
		}
	}
	
	private static TreeSet<String> findPackages(File file) throws IOException {
		TreeSet<String> result = new TreeSet<String>();
		JarFile jarFile = new JarFile(file);
		try {
			for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
				JarEntry entry = e.nextElement();
				if (entry.getName().endsWith(".class")) {
					int index = entry.getName().lastIndexOf('/');
					if (index > 0) {
						String packageName = entry.getName().substring(0, index).replace('/', '.');
						result.add(packageName);
					}
				}
			}
		} finally {
			jarFile.close();
		}
		return result;
	}
	
	private static void copyJar(File sourceFile, File targetFile, Manifest manifest) throws IOException {
		JarFile sourceJarFile = new JarFile(sourceFile);
		try {
			System.out.println("Creating jar: " + targetFile);
	    byte buffer[] = new byte[10240];
	    FileOutputStream os = new FileOutputStream(targetFile);
	    JarOutputStream out = new JarOutputStream(os, manifest);
	    try {
				for (Enumeration<JarEntry> e = sourceJarFile.entries(); e.hasMoreElements(); ) {
					JarEntry entry = e.nextElement();
					
					// skip dirs
					if (entry.isDirectory()) continue;
					
					// skip manifest
					if (entry.getName().equals("META-INF/MANIFEST.MF")) continue;
					
		      // Add archive entry
	        JarEntry jarAdd = new JarEntry(entry.getName());
	        jarAdd.setTime(entry.getTime());
	        jarAdd.setComment(entry.getComment());
	        jarAdd.setCompressedSize(entry.getCompressedSize());
	        jarAdd.setCrc(entry.getCrc());
	        jarAdd.setSize(entry.getSize());
	        jarAdd.setExtra(entry.getExtra());
	        out.putNextEntry(jarAdd);
	
	        // Write file to archive
	        InputStream in = sourceJarFile.getInputStream(entry);
	        try {
		        while (true) {
		          int nRead = in.read(buffer, 0, buffer.length);
		          if (nRead <= 0)
		            break;
		          out.write(buffer, 0, nRead);
		        }
	        } finally {
	        	in.close();
	        }
				}
			} finally {
				out.close();
				os.close();
				targetFile.setLastModified(sourceFile.lastModified());
			}
		} finally {
			sourceJarFile.close();
		}
	}
	
	public static String getValue(Manifest manifest, String name) {
		String value = manifest.getMainAttributes().getValue(name);
		if (value != null) {
			int index = value.indexOf(';');
			if (index >= 0) {
				value = value.substring(0, index);
			}
		}
		return value;
	}

}
