/**
 * Copyright (c) 2011 ScalaStuff.org (joint venture of Alexander Dvorkovyy and Ruud Diterwich)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.scalastuff.esbt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class Utils {
	
	public static List<String> read(IFile file) {
		try {
			return read(file.getContents());
		} catch (CoreException e) {
		}
		return Collections.emptyList();
	}
	
	public static List<String> read(File file) {
		try {
			return read(new FileInputStream(file));
		} catch (FileNotFoundException e) {
		}
		return Collections.emptyList();
	}
	
	public static List<String> read(InputStream is) {
		List<String> lines = new ArrayList<String>();
		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
		return lines;
	}
	
	public static InputStream linesInputStream(List<String> lines) {
		StringBuilder builder = new StringBuilder();
		for (String line : lines) {
			builder.append(line).append("\n");
		}
		return new ByteArrayInputStream(builder.toString().getBytes());
	}

	public static void write(OutputStream os, List<String> lines) throws IOException {
		try {
			for (String line : lines) {
				os.write((line + "\n").getBytes());
			}
		} finally {
			os.close();
		}
	}

	public static void copy(File in, File out, boolean preserveTimestamp) throws IOException {
		if (out.exists() && in.length() == out.length() && in.lastModified() == out.lastModified()) {
			return;
		}
		copy(new FileInputStream(in), new FileOutputStream(out));
		if (preserveTimestamp) {
			out.setLastModified(in.lastModified());
		}
	}
	
	public static void copy(InputStream is, OutputStream os) throws IOException {
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
	
	public static int indexOfLineContaining(List<String> lines, String s) {
		s = stripSpaces(s);
		for (int i = 0; i < lines.size(); i++) {
			if (stripSpaces(lines.get(i)).contains(s)) {
				return i;
			}
		}
		return -1;
	}
	
	public static String stripSpaces(String s) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i))) {
				builder.append(s.charAt(i));
			}
		}
		return builder.toString();
	}
	
	public static String qname(String organization, String name, String version, String qualifier) {
		String common = "";
		for (int i = organization.length() - 1; i >= -1; i--) {
			if (i < 0 || organization.charAt(i) == '.') {
				common = organization.substring(i + 1);
				break;
			}
		}
		if (name.equals(common) || name.startsWith(common + ".")) {
			name = name.substring(common.length());
		}
		if (organization.endsWith("." + common)) {
			organization = organization.substring(0, organization.length() - common.length() - 1);
		}
		if (name.startsWith("-")) {
			name = name.substring(1);
		}
		if (!name.equals("") && !name.startsWith(".")) {
			name = "." + name;
		}
		if (!version.equals("") && !version.startsWith("-")) {
			version = "-" + version;
		}
		if (!qualifier.equals("") && !qualifier.startsWith("-")) {
			qualifier = "-" + qualifier;
		}
		return organization + name + version + qualifier;
	}
	
	public static String orElse(String s, String alt) {
		if (s != null) return s;
		else return alt;
	}
}
