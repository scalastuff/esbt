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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	  	if (prg.isSbtProject()) {
	  		for (FileContent file : prg.getSbtFiles()) {
	  			rules.add(file.getFile());
	  		}
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
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		synchronized (Processor.class) {
			try {
				try {
					boolean force = false;
					Collection<ProjectInfo> projects = this.projects;
					if (projects == null) {
						projects = new ArrayList<ProjectInfo>(WorkspaceInfo.getAllProjects());
					} else {
						force = true;
						projects = new ArrayList<ProjectInfo>(projects);
					}
					CreateSbtPlugin.createSbtPlugin();
					console = new Console();
					for (ProjectInfo project : projects) {
						process(project, force);
					}
					if (command == null) {
						for (ProjectInfo project : WorkspaceInfo.getAllProjects()) {
							if (project.isSbtProject()) {
								project.checkProjectDependencies();
							}
						}
					}
					return Status.OK_STATUS;
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
	
	private void process(ProjectInfo project, boolean force) throws IOException, CoreException {
		if (project.isSbtProject()) {
			long start = System.currentTimeMillis();
			if (command != null) {
				InvokeSbt sbt = new InvokeSbt(project, command, console);		
				console.activate();
				console.println("");
				console.println("------ Processing project: " + project.getProject().getName() + " ------");
				sbt.setProjectDir(project.getProjectDir());
				sbt.invokeSbt();
				console.println("----- Done (" + (System.currentTimeMillis() - start) + " ms)------");
			} else {
				project.updateProject(force, console);
			}
		}
	}
}
