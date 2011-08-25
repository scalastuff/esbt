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

import static org.scalastuff.esbt.Utils.indexOfLineContaining;
import static org.scalastuff.esbt.Utils.read;
import static org.scalastuff.esbt.Utils.write;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CreateSbtPlugin {
	
	public static final File PLUGIN_HOME = Esbt.USER_HOME;
	
	public static void createSbtPlugin() throws IOException {
		File pluginDir = new File(PLUGIN_HOME, ".sbt/plugins");
		pluginDir.mkdirs();
		createBuildSbt(pluginDir);
		createSbtEclipsePluginFile(pluginDir);
	}
	
	private static void createBuildSbt(File pluginDir) throws IOException {
		File file = new File(pluginDir, "build.sbt");
		if (!file.exists()) {
			file.createNewFile();
		}
		List<String> lines = read(file);
		if (indexOfLineContaining(lines, "sbtPlugin := true") < 0) {
			lines.add("");
			lines.add("sbtPlugin := true");
			write(new FileOutputStream(file), lines);
		}
	}
	
	private static void createSbtEclipsePluginFile(File pluginDir) throws IOException {
		InputStream is = CreateSbtPlugin.class.getResourceAsStream("SbtEclipsePlugin.scala.source");
		if (is == null) {
			throw new IOException("Coulnd't find SbtEclipsePlugin.scala.source");
		}
		List<String> content = read(is);
		File destFile = new File(pluginDir, "SbtEclipsePlugin.scala");
		List<String> existingContent = read(destFile);
		if (!content.equals(existingContent)) {
			write(new FileOutputStream(destFile), content);
		}
	}
}
