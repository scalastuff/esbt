package org.scalastuff.esbt;

import static org.scalastuff.esbt.Utils.read;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class ExecuteSbtCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
  	try {
  		File file = new File(WorkspaceInfo.getMetaDataDir(), "last-command");
  		List<String> lastCommand = read(file);
  		String value = lastCommand.isEmpty() ? "" : lastCommand.get(0);
  		
  		Shell shell = HandlerUtil.getActiveWorkbenchWindow(event).getShell();
  		InputDialog dialog = new InputDialog(shell, "Execute SBT command", "SBT command: ", value, null);
  		
			if (dialog.open() == InputDialog.OK) {
  			value = dialog.getValue();
  			Utils.write(new FileOutputStream(file), Arrays.asList(value));
  			
  			Processor processor = new Processor();
  			processor.setProjects(WorkspaceInfo.getSelectedProjects(event));
  			processor.setCommand(value);
			  processor.schedule();
  		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
