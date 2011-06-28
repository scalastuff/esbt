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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

public class Processor extends Job {

	// The plug-in ID
	public static final String PLUGIN_ID = "scalastuff.eclipse.sbt"; //$NON-NLS-1$

	private Console console;
	private List<ProjectInfo> projects;
	private String command;
	
	public Processor() throws CoreException, IOException {
		super("SBT Project Update");
	  setPriority(Job.LONG);
	  List<ISchedulingRule> rules = new ArrayList<ISchedulingRule>();
	  for (ProjectInfo prg : WorkspaceInfo.getAllProjects()) {
	  	if (prg.getSbtFile().exists()) {
	  		rules.add(prg.getSbtFile().getFile());
	  	}
	  	if (prg.getClassPathFile().exists()) {
	  		rules.add(prg.getClassPathFile().getFile());
	  	}
	  }
//		setRule(new MultiRule(rules.toArray(new ISchedulingRule[0])));
	}
	
	public void setProjects(List<ProjectInfo> projects) {
		this.projects = projects;
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
	private Collection<ProjectInfo> pullModifiedProjects() throws CoreException, IOException {
		List<ProjectInfo> projects2 = projects;
		projects = null;
		return projects2 != null ? 
				projects2 :
			WorkspaceInfo.pullModifiedProjects();
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		synchronized (Processor.class) {
			try {
				try {
					while (true) {
						Collection<ProjectInfo> modifiedProjects = pullModifiedProjects();
						if (modifiedProjects.isEmpty()) return Status.OK_STATUS;
						console = new Console();
						for (ProjectInfo modifiedSbtProject : modifiedProjects) {
							process(modifiedSbtProject);
						}
					}
				} catch (Throwable t) {
					return new Status(Status.ERROR, PLUGIN_ID, t.getMessage(), t);
				}
			} catch (Throwable t) {
				return new Status(Status.ERROR, PLUGIN_ID, t.getMessage(), t);
			} finally {
				try {
					if (console != null) {
						console.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void process(ProjectInfo project) throws IOException, CoreException {
		if (project.getSbtFile().exists()) {
			console.activate();
			console.println("");
			console.println("------ Processing project: " + project.getSbtFile().getName() + " ------");
			long start = System.currentTimeMillis();
			InvokeSbt sbt = new InvokeSbt(project, console);		
			if (command != null) {
				sbt.setCommand(command);
				sbt.setProjectDir(project.getProjectDir());
				sbt.invokeSbt();
			} else {
				SbtPluginCreator.createSbtPlugin();
				File projectDir = cloneProjectDir(project);
				sbt.setProjectDir(projectDir);
				sbt.invokeSbt();
				project.update(sbt.getDependencies());
			}
			console.println("----- Done (" + (System.currentTimeMillis() - start) + " ms)------");
		}
	}

	private File cloneProjectDir(ProjectInfo project) throws FileNotFoundException, IOException {

		if (!project.getSbtFile().hasProjectDependencies()) {
			return project.getProjectDir();
		}

		// create build.sbt
		File dir = new File(WorkspaceInfo.getMetaDataDir(), "tmpprj");
		copyProjectDir(new File(project.getProjectDir(), "project"), new File(dir, "project"));
		
		// write customized build.sbt
		dir.mkdirs();
		project.getSbtFile().refresh();
		List<String> sbtFile = project.getSbtFile().getContentWithoutProjectDependencies();
		Utils.write(new FileOutputStream(new File(dir, "build.sbt")), sbtFile);
		
		return dir;
	}	
	
	private static void copyProjectDir(File source, File dest) throws IOException {
		if (source.isDirectory()) {
			Set<String> sourceChildren = new HashSet<String>();
			for (File child : source.listFiles()) {
				sourceChildren.add(child.getName());
				copyProjectDir(child, new File(dest, child.getName()));
			}
			if (dest.isDirectory()) {
				for (File destChild : dest.listFiles()) {
					if (!sourceChildren.contains(destChild.getName())) {
						deleteAll(destChild);
					}
				}
			}
		}
		else if (source.isFile()) {
			if (source.getName().endsWith(".sbt") || source.getName().endsWith(".scala") || source.getName().endsWith(".properties") || source.getName().endsWith(".xml")) {
				source.getParentFile().mkdirs();
				copy(source, dest, true);
			}
		}
	}
	
	private static void deleteAll(File file) {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				deleteAll(child);
			}
		} else {
			file.delete();
		}
	}
}
