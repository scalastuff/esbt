package org.scalastuff.esbt;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class WorkspaceInfo {

	private Map<IProject, ProjectInfo> projects = new HashMap<IProject, ProjectInfo>();

	public static File getMetaDataDir() {
			File dir = new File(new File(ResourcesPlugin.getWorkspace().getRoot().getLocationURI()), ".metadata/sbt-plugin");	
			dir.mkdirs();
			return dir;
	}
	
	public Set<ProjectInfo> getProjects(List<IProject> projects) throws CoreException, IOException {
		updateProjects();
		Set<ProjectInfo> result = new HashSet<ProjectInfo>();
		for (IProject project : projects) {
			ProjectInfo projectInfo = this.projects.get(project);
			if (projectInfo != null) {
				result.add(projectInfo);
			}
		}
		return result;
	}
	
	public Set<ProjectInfo> getModifiedProjects() throws CoreException, IOException {
		updateProjects();
		Set<ProjectInfo> result = new HashSet<ProjectInfo>();
		for (ProjectInfo project : projects.values()) {
			if (!project.checkUpToDate()) {
				result.add(project);
			}
		}
		return result;
	}
	
	public ProjectInfo findProject(String organization, String name, String version) {
		for (ProjectInfo project : projects.values()) {
			if (project.getOrganization().equals(organization)
					&& project.getName().equals(name)
					&& project.getVersion().equals(version)) {
				return project;
			}
		}
		return null;
	}
	
	private void updateProjects() {
		Set<ProjectInfo> result = new HashSet<ProjectInfo>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			ProjectInfo projectInfo = projects.get(project);
			if (projectInfo == null) {
				projectInfo = new ProjectInfo(this, project);
				projects.put(project, projectInfo);
			}
		}
	}
}
