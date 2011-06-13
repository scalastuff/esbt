package org.scalastuff.esbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PartInitException;

public class Processor extends Job {

	private static WorkspaceInfo workspaceInfo = new WorkspaceInfo();
	private Console console;
	private List<IProject> projects;
	private String command;
	
	public Processor() throws PartInitException {
		super("SBT Project Update");
	  setPriority(Job.LONG);
		setRule(ResourcesPlugin.getWorkspace().getRoot());
	}
	
	public void setProjects(List<IProject> projects) {
		this.projects = projects;
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			try {
				Set<ProjectInfo> modifiedProjects = 
						this.projects != null ? 
								workspaceInfo.getProjects(this.projects) : 
								workspaceInfo.getModifiedProjects();
				if (modifiedProjects.isEmpty()) return Status.OK_STATUS;
				console = new Console();
				for (ProjectInfo modifiedSbtProject : modifiedProjects) {
					process(modifiedSbtProject);
				}
				return Status.OK_STATUS;
			} catch (Throwable t) {
				return new Status(Status.ERROR, Activator.PLUGIN_ID, t.getMessage(), t);
			}
		} catch (Throwable t) {
			return new Status(Status.ERROR, Activator.PLUGIN_ID, t.getMessage(), t);
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
	
	private void process(ProjectInfo project) throws IOException, CoreException {
		if (project.isUnderSbtControl()) {
			SbtPlugin.createSbtPlugin();
			console.println("*** Processing Project: " + project.getName() + " ***");
			InvokeSbt sbt = new InvokeSbt(project, console);		
			if (command != null) {
				sbt.setCommand(command);
				sbt.setProjectDir(cloneProjectDir(project));
				sbt.invokeSbt();
			} else {
				sbt.invokeSbt();
				project.update(sbt);
			}
		}
	}

	private File cloneProjectDir(ProjectInfo project) throws FileNotFoundException, IOException {

		// create build.sbt
		File dir = new File(WorkspaceInfo.getMetaDataDir(), "tmpprj");
		dir.mkdirs();
		List<String> sbtFile = project.getSbtFileWithoutProjectDependencies();
		Utils.write(new FileOutputStream(new File(dir, "build.sbt")), sbtFile);
		
		// copy Build.scala
		File srcBuildScala = new File(dir, "project/Build.scala");
		File destBuildScala = new File(project.getProjectDir(), "project/Build.scala");
		if (srcBuildScala.exists()) {
			Utils.copyStream(new FileInputStream(srcBuildScala), new FileOutputStream(destBuildScala));
		} else {
			destBuildScala.delete();
		}
		return dir;
	}
}
