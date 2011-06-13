package org.scalastuff.esbt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

public class SbtUpdateAction implements IObjectActionDelegate{
		 
    private List<IProject> projects = new ArrayList<IProject>();

		public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
            // TODO Auto-generated method stub
    }

    public void run(IAction action) {
    	try {
				Processor processor = new Processor();
				processor.setProjects(projects);
				processor.schedule();
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
       }

  	public void selectionChanged(IAction action, ISelection selection) {
  		projects.clear();
  		if(selection instanceof IProject){
  			projects.add( (IProject) selection );
  		} else if (selection instanceof IStructuredSelection){
  			for (Object project : ((IStructuredSelection)selection).toArray()) {
  				projects.add((IProject) project);
  				
  			}
  		}
  	}

}