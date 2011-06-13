package org.scalastuff.esbt;

import static org.scalastuff.esbt.Utils.read;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class SbtCommandAction implements IObjectActionDelegate{
		 
    private List<IProject> projects = new ArrayList<IProject>();

		public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
            // TODO Auto-generated method stub
    }

    public void run(IAction action) {
    	try {
    		File file = new File(WorkspaceInfo.getMetaDataDir(), "last-command");
    		List<String> lastCommand = read(file);
    		String value = lastCommand.isEmpty() ? "" : lastCommand.get(0);
    		
    		Shell shell = new Shell();
    		InputDialog dialog = new InputDialog(shell, "Execute SBT command", "SBT command: ", value, null);
    		
				if (dialog.open() == InputDialog.OK) {
    			value = dialog.getValue();
    			Utils.write(new FileOutputStream(file), Arrays.asList(value));
    			
    			Processor processor = new Processor();
    			processor.setProjects(projects);
    			processor.setCommand(value);
				  processor.schedule();
    		}
			} catch (Exception e) {
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