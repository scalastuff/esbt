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
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class ProjectInfo {

	private final IProject project;
	private final BuildSbtFile sbtFile;
	private final ClassPathFile classpathFile;
	private final ManifestFile manifestFile;
	

	public ProjectInfo(IProject project) {
		this.project = project;
		this.sbtFile = new BuildSbtFile(this);
		this.classpathFile = new ClassPathFile(this);
		this.manifestFile = new ManifestFile(this);
	}
	
	public IProject getProject() {
		return project;
	}
	
	public File getProjectDir() {
		return new File(project.getLocationURI());
	}
	
	public BuildSbtFile getSbtFile() {
	  return sbtFile;
  }
	
	public ClassPathFile getClassPathFile() {
	  return classpathFile;
  }
	
	public ManifestFile getManifestFile() {
	  return manifestFile;
  }
	
	public boolean checkUpToDate() {
		if (!sbtFile.isUpToDate() || !classpathFile.isUpToDate() || !manifestFile.isUpToDate()) {
			sbtFile.refresh();
			classpathFile.refresh();
			manifestFile.refresh();
			return false;
		}
		return true;
	}
	
	public void update(InvokeSbt sbt) throws CoreException, IOException {

		// combine sbt project deps with sbt project deps
		Set<ProjectInfo> projectDeps = sbtFile.getProjectDependencies();
		List<Dependency> deps = new ArrayList<Dependency>();
		for (Dependency dep : sbt.getDependencies()) {
			CopyJars.copyJars(dep);
			ProjectInfo depProject = WorkspaceInfo.findProject(dep.organization, dep.name, dep.version);
			if (depProject != null) {
				projectDeps.add(depProject);
			} else {
				deps.add(dep);
			}
		}
		
		classpathFile.write(projectDeps, deps);
		manifestFile.write(projectDeps, deps);
	}
}
