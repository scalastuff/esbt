package org.scalastuff.esbt;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class UpdateDependenciesCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			List<ProjectInfo> selectedProjects = WorkspaceInfo.getSelectedProjects(event);
			Processor processor = new Processor();
			processor.setProjects(selectedProjects);
			processor.schedule();
		} catch (Exception e) {
		}
		return null;
	}
}
