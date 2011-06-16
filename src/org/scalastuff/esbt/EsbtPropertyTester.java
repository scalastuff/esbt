package org.scalastuff.esbt;
import org.eclipse.core.expressions.PropertyTester;


public class EsbtPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		ProjectInfo project = WorkspaceInfo.adaptToProject(receiver);
		return project  != null && project.getSbtFile().exists();
	}

}
