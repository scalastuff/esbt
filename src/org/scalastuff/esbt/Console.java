package org.scalastuff.esbt;

import java.io.IOException;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class Console {

	private MessageConsole console = findConsole("SBT Console");
	private MessageConsoleStream stream;
	
	public void clear() {
		console.clearConsole();
		console.activate();
	}

	public IOConsoleInputStream getInputStream() {
		return console.getInputStream();
	}
	
	public void println(String message) {
		if (stream == null) {
			stream = console.newMessageStream();
		}
		stream.println(message);
	}
	
  private static MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager conMan = plugin.getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for (int i = 0; i < existing.length; i++)
       if (name.equals(existing[i].getName()))
          return (MessageConsole) existing[i];
    //no console found, so create a new one
    MessageConsole myConsole = new MessageConsole(name, null);
    conMan.addConsoles(new IConsole[]{myConsole});
    return myConsole;
 }
  
  public void reveal(IWorkbenchWindow window) throws PartInitException {
    String id = IConsoleConstants.ID_CONSOLE_VIEW;
    IConsoleView view = (IConsoleView) window.getActivePage().showView(id);
    view.display(console);
  }

	public void close() throws IOException {
		if (stream != null) {
			stream.close();
		}
		stream = null;
	}
}
