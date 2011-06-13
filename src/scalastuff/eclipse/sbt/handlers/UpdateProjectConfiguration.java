package scalastuff.eclipse.sbt.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.scalastuff.esbt.Processor;


/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class UpdateProjectConfiguration extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public UpdateProjectConfiguration() {
	}
	
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//		MessageDialog.openInformation(
//				window.getShell(),
//				"Sbt Eclipse Plugin",
//				"Hello, Eclipse world");
	   IWorkbenchPage page = window.getActivePage();
	   try {
			new Processor().schedule();
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//			InvokeSbt.invokeSbt(new File("."));
		return null;
	}
}
