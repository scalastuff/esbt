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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class ProjectInfo {

	private File infoFile;
	private IProject project;
	private DotClassPathFile classpathFile;
	private ManifestFile manifestFile;
	private List<FileContent> sbtFiles;

	// info from parse
	private String organization = "";
	private String name = "";
	private String version = "";
	private String scalaVersion = "";
	private final List<String> sourceDirectories = new ArrayList<String>();
	private final List<String> testSourceDirectories = new ArrayList<String>();
	private final List<String> resourceDirectories = new ArrayList<String>();
	private final List<String> testResourceDirectories = new ArrayList<String>();
	private final List<String> classDirectories = new ArrayList<String>();
	private final List<String> testClassDirectories = new ArrayList<String>();
	private final List<Dependency> dependencies = new ArrayList<Dependency>();
	private Set<ProjectInfo> projectDependencies = Collections.emptySet();
	
	public ProjectInfo(IProject project) {
		initialize(project);
	}
	
	private void initialize(IProject project) {
		this.project = project;
		this.infoFile = new File(WorkspaceInfo.getMetaDataDir(), project.getName() + "-lastresult.txt");
		this.classpathFile = new DotClassPathFile(this);
		this.manifestFile = new ManifestFile(this);
		this.sbtFiles = getFileContent(scanSbtFiles());
		if (isSbtProject()) {
			parse(Utils.read(infoFile));
		}
	}
	
	public IProject getProject() {
		return project;
	}
	
	public File getProjectDir() {
		return new File(project.getLocationURI());
	}

	public DotClassPathFile getClassPathFile() {
	  return classpathFile;
  }
	
	public ManifestFile getManifestFile() {
	  return manifestFile;
  }
	
	public boolean isSbtProject() {
		return project.exists() && !scanSbtFiles().isEmpty();
	}
	
	public List<FileContent> getSbtFiles() {
	  return sbtFiles;
  }

	public String getOrganization() {
	  return organization;
  }
	
	public String getName() {
	  return name;
  }
	
	public String getVersion() {
	  return version;
  }
	
	public String getScalaVersion() {
	  return scalaVersion;
  }
	
	public List<String> getSourceDirectories() {
	  return sourceDirectories;
  }
	
	public List<String> getResourceDirectories() {
	  return resourceDirectories;
  }
	
	public List<String> getClassDirectories() {
	  return classDirectories;
  }
	
	public List<String> getTestSourceDirectories() {
	  return testSourceDirectories;
  }
	
	public List<String> getTestResourceDirectories() {
	  return testResourceDirectories;
  }
	
	public List<String> getTestClassDirectories() {
	  return testClassDirectories;
  }
	
	public List<Dependency> getDependencies() {
		return dependencies;
	}
	
	public void updateProject(boolean force, Console console) throws IOException, CoreException {
		long sbtFilesLastModified = getSbtFilesLastModified();
		if (force || infoFile.lastModified() < sbtFilesLastModified) {
			try {
				initialize(project);
				console.activate();
				console.println("");
				console.println("------ Update project: " + project.getProject().getName() + " ------");
				long start = System.currentTimeMillis();
				InvokeSbt invokeSbt = new InvokeSbt(this, "update-eclipse", console);		
				invokeSbt.setProjectDir(getProjectDir());
				invokeSbt.invokeSbt();
				Utils.write(new FileOutputStream(infoFile), invokeSbt.getResult());
				parse(invokeSbt.getResult());
				writeProjectFiles();
				console.println("----- Done (" + (System.currentTimeMillis() - start) + " ms)------");
			} finally {
				infoFile.createNewFile();
				infoFile.setLastModified(sbtFilesLastModified);
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		}			
	}
	
	public void checkProjectDependencies() throws FileNotFoundException, IOException, CoreException {
		if (!this.projectDependencies.equals(findProjectDependencies())) {
			writeProjectFiles();
		}
	}
	
	private long getSbtFilesLastModified() {
		long sbtFileLastModified = 0;
		List<IFile> sbtFiles = scanSbtFiles();
		for (IFile file : sbtFiles) {
			if (sbtFileLastModified < file.getLocalTimeStamp()) {
				sbtFileLastModified = file.getLocalTimeStamp();
			}
		}
		return sbtFileLastModified;
	}
	
	private void writeProjectFiles() throws FileNotFoundException, IOException, CoreException {
		this.projectDependencies = findProjectDependencies();
		
		// combine sbt project deps with sbt project deps
		List<Dependency> deps = new ArrayList<Dependency>();
		for (Dependency dep : getDependencies()) {
			if (findProjectDependency(dep) == null) {
				dep.jar = CopyJars.copyFile(dep, getProjectDir(), dep.jar);
				dep.srcJar = CopyJars.copyFile(dep, getProjectDir(), dep.srcJar);
				deps.add(dep);
			}
		}
		
		classpathFile.write(projectDependencies, deps);
		manifestFile.write(projectDependencies, deps);

		// update eclipse project 
		IProjectDescription desc = project.getProject().getDescription();
		
		// add scala nature
		String javaNatureId = "org.eclipse.jdt.core.javanature";
		String scalaNatureId = "org.scala-ide.sdt.core.scalanature";
		boolean javaNatureFound = false;
		boolean scalaNatureFound = false;
		for (String nature : desc.getNatureIds()) {
			if (nature.equals(scalaNatureId)) {
				scalaNatureFound = true;
			}
			if (nature.equals(javaNatureId)) {
				javaNatureFound = true;
			}
		}
		String[] natureIds = desc.getNatureIds();
		if (!javaNatureFound) {
			natureIds = (String[]) Arrays.copyOf(natureIds, natureIds.length + 1);
			natureIds[natureIds.length - 1] = javaNatureId;
		}
		if (!scalaNatureFound) {
			natureIds = (String[]) Arrays.copyOf(natureIds, natureIds.length + 1);
			natureIds[natureIds.length - 1] = scalaNatureId;
		}
		desc.setNatureIds(natureIds);
		project.setDescription(desc, null);

		// rename project?
//		renameProject();

	}
	
	private void renameProject() throws CoreException, IOException {
		String name = getProjectName();
		if (!name.isEmpty() && !getOrganization().equals("default") && !getName().equals("default")) {
			IProjectDescription desc = project.getProject().getDescription();
			if (!desc.getName().equals(name)) {
				
//				// backup existing target folder
				File newProjectDir = new File(getProjectDir().getParentFile(), name);
//				for (int i = 0; i < 100 && newProjectDir.exists(); i++) {
//					newProjectDir.renameTo(new File(newProjectDir.getPath() + ".old" + i));
//				}
//				
				// rename folder on disk
				if (getProjectDir().renameTo(newProjectDir)) {

					// remove existing .project file
					File projectFile = new File(newProjectDir, ".project");
					projectFile.delete();

					// create a new eclipse project
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IProject newProject = root.getProject(name);
					newProject.create(null);
					newProject.open(null);
					
					// write .project file
					desc.setName(name);
					newProject.setDescription(desc, null);
					
					// remove current eclipse project
					this.project.delete(true, null);

					// move info file
					File oldInfoFile = this.infoFile;
					
					// re-initialize needed
					initialize(newProject);
					
					// copy old info file
					Utils.copy(oldInfoFile, infoFile, true);
				}
			}
		}
	}

	private String getProjectName() {
	  if (getName().equals("") || getOrganization().equals("")) {
	  	return getName();
	  } else {
				return getOrganization() + "." + getName();
	  }
  }

	private Set<ProjectInfo> findProjectDependencies() {
	  Set<ProjectInfo> projectDeps = new LinkedHashSet<ProjectInfo>();
		for (Dependency dep : getDependencies()) {
			ProjectInfo depPrj = findProjectDependency(dep);
			if (depPrj != null) projectDeps.add(depPrj);
		}
	  return projectDeps;
  }
	
	private ProjectInfo findProjectDependency(Dependency dep) {
		String projectName = dep.name;
		String suffix = "_" + scalaVersion;
		if (projectName.endsWith(suffix)) {
			projectName = projectName.substring(0, projectName.length() - suffix.length());
		}
		return WorkspaceInfo.findProject(dep.organization, projectName, dep.version);
	}
	
	public void parse(List<String> lines) {
		organization = "";
		name = "";
		version = "";
		scalaVersion = "";
		sourceDirectories.clear();
		testSourceDirectories.clear();
		resourceDirectories.clear();
		testResourceDirectories.clear();
		classDirectories.clear();
		testClassDirectories.clear();
		dependencies.clear();
		projectDependencies.clear();
		for (String line : lines) {
			String[] fields = line.split("::");
			if (fields[0].trim().equals("dependency")) {
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
			} else if (fields[0].trim().equals("organization")) {
				organization = fields[1].trim();
			} else if (fields[0].trim().equals("name")) {
				name = fields[1].trim();
			} else if (fields[0].trim().equals("version")) {
				version = fields[1].trim();
			} else if (fields[0].trim().equals("scalaVersion")) {
				scalaVersion = fields[1].trim();
			} else if (fields[0].trim().equals("sourceDirectory")) {
				sourceDirectories.add(fields[1].trim());
			} else if (fields[0].trim().equals("testSourceDirectory")) {
				testSourceDirectories.add(fields[1].trim());
			} else if (fields[0].trim().equals("resourceDirectory")) {
				resourceDirectories.add(fields[1].trim());
			} else if (fields[0].trim().equals("testResourceDirectory")) {
				testResourceDirectories.add(fields[1].trim());
			} else if (fields[0].trim().equals("classDirectory")) {
				classDirectories.add(fields[1].trim());
			} else if (fields[0].trim().equals("testClassDirectory")) {
				testClassDirectories.add(fields[1].trim());
			}
		}
	}
	
	private List<FileContent> getFileContent(List<IFile> files) {
		ArrayList<FileContent> list = new ArrayList<FileContent>(files.size());
		for (IFile file : files) {
			list.add(new FileContent(file));
		}
		return list;
	}
	
	private List<IFile> scanSbtFiles() {
		List<IFile> list = new ArrayList<IFile>();
	  IFile buildSbt = project.getProject().getFile("build.sbt");
	  if (buildSbt.exists()) {
	  	list.add(buildSbt);
	  }
	  IFolder projectDir = project.getProject().getFolder("project");
	  try {
	    for (IResource scalaFile : projectDir.members()) {
	    	if (scalaFile instanceof IFile && scalaFile.getFileExtension().equals("scala")) {
	    		list.add((IFile) scalaFile);
	    	}
	    }
    } catch (CoreException e) {
    }
	  return list;
  }
}
