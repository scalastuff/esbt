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

	public void activate() {
		console.activate();
		
	}
}
