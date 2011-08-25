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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class WorkspaceInfo {

	private static final Map<IProject, ProjectInfo> projects = new HashMap<IProject, ProjectInfo>();

	public static File getMetaDataDir() {
			File dir = new File(new File(ResourcesPlugin.getWorkspace().getRoot().getLocationURI()), ".metadata/sbt-plugin");	
			dir.mkdirs();
			return dir;
	}
	
	public static Collection<ProjectInfo> getAllProjects() throws CoreException, IOException {
		updateProjects();
		return projects.values();
	}
	
	public static ProjectInfo getProject(IProject project) throws CoreException, IOException {
		updateProjects();
		return projects.get(project);
	}
	
	public static Set<ProjectInfo> getProjects(List<IProject> projects) throws CoreException, IOException {
		updateProjects();
		Set<ProjectInfo> result = new HashSet<ProjectInfo>();
		for (IProject project : projects) {
			ProjectInfo projectInfo = WorkspaceInfo.projects.get(project);
			if (projectInfo != null) {
				result.add(projectInfo);
			}
		}
		return result;
	}
	
	public static ProjectInfo findProject(String organization, String name, String version) {
		for (ProjectInfo project : projects.values()) {
			if (project.getOrganization().equals(organization)
					&& project.getName().equals(name)
					&& project.getVersion().equals(version)) {
				return project;
			}
		}
		return null;
	}
	
	public static ProjectInfo adaptToProject(Object obj)  {
		if (obj instanceof IAdaptable) {
			IProject project = (IProject) ((IAdaptable) obj).getAdapter(IProject.class);
			if (project != null) {
				try {
					return getProject(project);
				} catch (Exception e) {
				}
			}
		}
		 return null;
	}
	
	public static List<ProjectInfo> getSelectedProjects(ExecutionEvent event) {
		List<ProjectInfo> projects = new ArrayList<ProjectInfo>();
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		if (selection != null & selection instanceof IStructuredSelection) {
			IStructuredSelection strucSelection = (IStructuredSelection) selection;
			for (Iterator<?> iterator = strucSelection.iterator(); iterator.hasNext();) {
				ProjectInfo project = WorkspaceInfo.adaptToProject(iterator.next());
				if (project != null && project.isSbtProject()) {
					projects.add(project);
				}
			}
		}
		return projects;

	}
	
	private static void updateProjects() {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			ProjectInfo projectInfo = projects.get(project);
			if (projectInfo == null) {
				projectInfo = new ProjectInfo(project);
				projects.put(project, projectInfo);
			}
		}
	}
}
