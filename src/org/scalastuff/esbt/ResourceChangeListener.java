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
		|| (resource.getName().endsWith("scala") && resource.getParent().getName().equals("project"))) {
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
	} 
	catch (Exception e) {
		e.printStackTrace();
	}
	}
}
