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

import static org.scalastuff.esbt.Utils.copy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class InvokeSbt {

	private static File launchJar;
	private final Console console;
	private final List<String> result = new ArrayList<String>();
	private boolean error;
	private String command = "update-eclipse";
	private File projectDir;
	
	public InvokeSbt(ProjectInfo project, String command, Console console) {
		this.projectDir = project.getProjectDir();
		this.command = command;
		this.console = console;
	}

	public String getCommand() {
		return command;
	}
	
	public File getProjectDir() {
		return projectDir;
	}
	
	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
	}
	
	public void invokeSbt() throws IOException {
		Process process = createProcess(projectDir);
		readProcessOutput(process);
	}
	
	public boolean hasError() {
		return error;
	}
	
	public List<String> getResult() {
	  return result;
  }

	private Process createProcess(File projectDir) throws IOException  {
		
		// find java executable
		File javaHome = new File(System.getProperty("java.home"));
		File javaExec = new File(javaHome, "bin/java"); 
		if (!javaExec.exists()) {
			javaExec = new File(javaHome, "bin/javaw.exe");
		}
		ArrayList<String> args = new ArrayList<String>();
		args.add(javaExec.toString());
		args.add("-Dsbt.log.noformat=true");
		args.add("-Duser.home=" + CreateSbtPlugin.PLUGIN_HOME);
		args.add("-jar");
		args.add(getLaunchJar().toString());
		args.addAll(parseCommand(command));
		final ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(projectDir);
		pb.redirectErrorStream(true);
		return pb.start();
	}
	
	private List<String> parseCommand(String command) {
		List<String> result = new ArrayList<String>();
		boolean insideString = false;
		StringBuilder out = new StringBuilder();
		for (int pos = 0; pos < command.length(); pos++) {
			char c = command.charAt(pos);
			if (c == '"') {
				if (insideString) {
					result.add(out.toString());
				} else {
					result.add(out.toString().trim());
				}
				out.setLength(0);
				insideString = !insideString;
			} else {
				if (c == ' ') {
					result.add(out.toString().trim());
					out.setLength(0);
				} else {
					out.append(c);
				}
			}
		}
		result.add(out.toString().trim());
		return result;
	}
	
	private void readProcessOutput(final Process process) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;

		result.clear();
		
		// make sure process does not wait for input
		process.getOutputStream().close();
		while ((line = br.readLine()) != null) {
			
			if (line.startsWith("Project loading failed:")) {
			}
			
			// detect result
			else if (line.startsWith("[result]")) {
				result.add(line.substring("[result]".length()).trim());
			} else {
				if (line.startsWith("[error]")) {
					error = true;
				}
			console.println(line);	
			}
		}
	}

	private File getLaunchJar() throws FileNotFoundException, IOException {
		if (launchJar == null) {
			File launchJar = File.createTempFile("sbt-launch", ".jar");
			InputStream is = InvokeSbt.class.getResourceAsStream("sbt-launch.jar");
			if (is == null) {
				console.println("Coulnd't find sbt-launch.jar");
			}
			OutputStream os = new FileOutputStream(launchJar);
			copy(is, os);
			InvokeSbt.launchJar = launchJar;
		}
		return launchJar;
	}
}
