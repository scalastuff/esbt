package org.scalastuff.esbt;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class FileContent {

	private final IFile file;
	private List<String> lines = Collections.emptyList();
	
	protected FileContent(IFile file) {
		this(file, true);
  }
	
	protected FileContent(IFile file, boolean refresh) {
		this.file = file;
		if (refresh) {
			refresh();
		}
	}
	
	public IFile getFile() {
	  return file;
  }
	
	public boolean exists() {
		return file.exists();
	}
	
	public synchronized boolean isUpToDate() {
		try {
	    file.refreshLocal(IFile.DEPTH_INFINITE, null);
    } catch (CoreException e) {
    }
		return Utils.read(file).equals(lines);
	}
	
	public synchronized List<String> refresh() {
		try {
	    file.refreshLocal(IFile.DEPTH_INFINITE, null);
	    setLines(Utils.read(file));
    } catch (CoreException e) {
			setLines(Collections.<String>emptyList());
    }
		return lines;
	}
	
	public synchronized List<String> getContent() {
		return lines;
	}
	
	protected void setLines(List<String> lines) {
		this.lines = lines;
	}
	
	protected final synchronized void doWrite(List<String> lines) throws CoreException {
		refresh();
		if (!this.lines.equals(lines)) {
			if (file.exists()) {
				file.setContents(Utils.linesInputStream(lines), 0, null);
			} else {
				file.create(Utils.linesInputStream(lines), 0, null);
			}
			setLines(lines);
		}
	}
}
