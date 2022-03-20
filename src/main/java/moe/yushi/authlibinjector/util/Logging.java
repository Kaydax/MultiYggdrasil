/*
 * Copyright (C) 2021  Haowei Wen <yushijinhun@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package moe.yushi.authlibinjector.util;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static moe.yushi.authlibinjector.util.Logging.Level.INFO;
import static moe.yushi.authlibinjector.util.Logging.Level.WARNING;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import moe.yushi.authlibinjector.Config;

public final class Logging {
	private Logging() {}

	private static final PrintStream out = System.err;
	private static final FileChannel logfile = openLogFile();

	private static FileChannel openLogFile() {
		if (System.getProperty("authlibinjector.noLogFile") != null) {
			log(INFO, "Logging to file is disabled");
			return null;
		}

		Path logfilePath = Paths.get("authlib-injector.log").toAbsolutePath();
		try {
			FileChannel channel = FileChannel.open(logfilePath, CREATE, WRITE);
			if (channel.tryLock() == null) {
				log(WARNING, "Couldn't lock log file [" + logfilePath + "]");
				return null;
			}
			channel.truncate(0);
			String logHeader = "Logging started at " + Instant.now() + System.lineSeparator();
			channel.write(Charset.defaultCharset().encode(logHeader));
			log(INFO, "Logging file: " + logfilePath);
			return channel;
		} catch (IOException e) {
			log(WARNING, "Couldn't open log file [" + logfilePath + "]");
			return null;
		}
	}

	public static enum Level {
		DEBUG, INFO, WARNING, ERROR
	}

	public static void log(Level level, String message) {
		log(level, message, null);
	}

	public static void log(Level level, String message, Throwable e) {
		if (level == Level.DEBUG && !Config.verboseLogging) {
			return;
		}
		String log = "[authlib-injector] [" + level + "] " + message;
		if (e != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			pw.println();
			e.printStackTrace(pw);
			pw.close();
			log += sw.toString();
		}
		// remove control characters to prevent messing up the console
		log = log.replaceAll("[\\p{Cc}&&[^\r\n\t]]", "");
		out.println(log);

		if (logfile != null) {
			try {
				logfile.write(Charset.defaultCharset().encode(log + System.lineSeparator()));
				logfile.force(true);
			} catch (IOException ex) {
				out.println("[authlib-injector] [ERROR] Error writing to log file: " + ex);
			}
		}
	}
}
