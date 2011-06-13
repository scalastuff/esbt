package org.scalastuff.esbt;

import static org.scalastuff.esbt.Utils.copyStream;

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
	private final List<Dependency> dependencies = new ArrayList<Dependency>();
	private boolean error;
	private String command = "update-eclipse";
	private File projectDir;
	
	public InvokeSbt(ProjectInfo project, Console console) {
		this.console = console;
		this.projectDir = project.getProjectDir();
	}

	public String getCommand() {
		return command;
	}
	
	public void setCommand(String command) {
		this.command = command;
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
	
	public List<Dependency> getDependencies() {
		return dependencies;
	}

	private Process createProcess(File projectDir) throws IOException  {
		
		// find java executable
		File javaHome = new File(System.getProperty("java.home"));
		File javaExec = new File(javaHome, "bin/java"); 
		if (!javaExec.exists()) {
			javaExec = new File(javaHome, "bin/java.exe");
		}
		ArrayList<String> args = new ArrayList<String>();
		args.add(javaExec.toString());
		args.add("-Dsbt.log.noformat=true");
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

		// make sure process does not wait for input
		process.getOutputStream().close();
		while ((line = br.readLine()) != null) {
			
			if (line.startsWith("Project loading failed:")) {
			}
			
			// detect result
			else if (line.startsWith("[result]")) {
				String[] fields = line.split(":");
				if (fields[0].contains("dependency")) {
					Dependency dependency = new Dependency();
					for (int i = 1; i < fields.length; i++) {
						switch (i) {
						case 1: dependency.organization = fields[i].trim(); break;
						case 2: dependency.name = fields[i].trim(); break;
						case 3: dependency.version = fields[i].trim(); break;
						case 4: dependency.jar = fields[i].trim(); break;
						case 5: dependency.srcJar = fields[i].trim(); break;
						}
					}
					dependencies.add(dependency);
				}
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
			copyStream(is, os);
			InvokeSbt.launchJar = launchJar;
		}
		return launchJar;
	}
}
