/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher;

import java.lang.Thread.UncaughtExceptionHandler;

import org.omegazero.common.OmzLib;
import org.omegazero.common.logging.Logger;
import org.omegazero.common.logging.LoggerUtil;
import org.omegazero.common.util.Args;

public class Main {

	private static final Logger logger = Logger.create();


	public static void main(String[] pargs) {
		Args args = Args.parse(pargs);

		LoggerUtil.redirectStandardOutputStreams();

		String logFile = args.getValueOrDefault("logFile", "log");
		LoggerUtil.init(LoggerUtil.resolveLogLevel(args.getValue("logLevel")), logFile.equals("null") ? null : logFile);

		OmzLib.printBrand();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){

			@Override
			public void uncaughtException(Thread t, Throwable err) {
				logger.fatal("Uncaught exception in thread '", t.getName(), "': ", err);
				System.exit(3);
			}
		});

		try{
			new MCLauncher(args).init();
		}catch(Exception e){
			logger.fatal("Error during initialization: ", e);
		}
	}
}
