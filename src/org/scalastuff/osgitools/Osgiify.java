package org.scalastuff.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.scalastuff.esbt.Utils;

public class Osgiify {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public static File osgiify(File sourceFile, String symbolicName, String version, File targetDir) throws IOException {
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		
		// try to get symbolic name from source file
//		JarFile sourceJarFile = new JarFile(sourceFile);
//		try {
//			Manifest manifest = sourceJarFile.getManifest();
//			if (manifest != null) {
//				symbolicName = orElse(getValue(manifest, "Bundle-SymbolicName"), symbolicName);
//				version = orElse(getValue(manifest, "Bundle-Version"), version);
//			}
//		} finally {
//			sourceJarFile.close();
//		}
		
		// construct targetFile
		String sourceDate = dateFormat.format(new Date(sourceFile.lastModified())); 
		String targetFileBaseName = symbolicName + "-" + version + "-";
		File targetFile = new File(targetDir, targetFileBaseName + sourceDate + ".jar");

		// if target file exists, we're done
		if (targetFile.exists()) {
			return targetFile;
		}

		// remove all files with different timestamps
		for (File file : targetDir.listFiles()) {
			if (file.getName().startsWith(targetFileBaseName) && file.getName().endsWith(".jar")) {
				file.delete();
			}
		}
		
		JarFile sourceJarFile = new JarFile(sourceFile);
		try {
			Manifest manifest = sourceJarFile.getManifest();
			if (manifest != null
			&&  symbolicName.equals(getValue(manifest, "Bundle-SymbolicName"))
	  	&&  version.equals(getValue(manifest, "Bundle-Version"))) {
	  		Utils.copyStream(new FileInputStream(sourceFile), new FileOutputStream(targetFile));
	  		return targetFile;
			} else {
				if (manifest == null) {
					manifest = new Manifest();
				}
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
				copyJar(sourceJarFile, targetFile, manifest);
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
	
	private static void copyJar(JarFile sourceJarFile, File targetFile, Manifest manifest) throws IOException {
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
	
//	private static String orElse(String s, String alt) {
//		if (s != null) return s;
//		else return alt;
//	}
}
