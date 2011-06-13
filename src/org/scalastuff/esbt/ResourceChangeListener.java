package org.scalastuff.esbt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PartInitException;

public class ResourceChangeListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
	  //we are only interested in POST_CHANGE events
    if (event.getType() != IResourceChangeEvent.POST_CHANGE)
       return;
    IResourceDelta rootDelta = event.getDelta();
    //get the delta, if any, for the documentation directory
    final ArrayList<IResource> changed = new ArrayList<IResource>();
    IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
       public boolean visit(IResourceDelta delta) {
          //only interested in changed resources (not added or removed)
          if (delta.getKind() != IResourceDelta.CHANGED)
             return true;
          //only interested in content changes
          if ((delta.getFlags() & IResourceDelta.CONTENT) == 0)
             return true;
          IResource resource = delta.getResource();
          //only interested in files with the "txt" extension
          if (resource.getType() == IResource.FILE && 
		"build.sbt".equals(resource.getName())
		|| "Build.scala".equals(resource.getName())
		|| ".classpath".equals(resource.getName())) {
             changed.add(resource);
          }
          return true;
       }
    };	
	try {
		rootDelta.accept(visitor);
	} catch (CoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	Set<IProject> projects = new HashSet<IProject>(); 
	for (IResource resource : changed) {
		projects.add(resource.getProject());
	}
	try {
		new Processor().schedule();
	} catch (PartInitException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
//	new Console().println(projects.toString());
	}

}
