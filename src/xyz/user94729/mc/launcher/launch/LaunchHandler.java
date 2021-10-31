/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher.launch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.user94729.mc.launcher.GameProfile;
import xyz.user94729.mc.launcher.MCLauncher;
import xyz.user94729.mc.launcher.session.PlayerSession;

public class LaunchHandler {


	public static Process launchMinecraft(GameInstance instance, GameProfile profile, PlayerSession session) {
		Map<String, String> mcVars = new java.util.HashMap<>();
		mcVars.put("auth_uuid", session.getPlayerUUID());
		mcVars.put("auth_player_name", session.getPlayerName());
		mcVars.put("auth_access_token", session.getAccessToken());
		mcVars.put("user_type", session.getUserType());
		mcVars.put("version_name", profile.versionName);
		mcVars.put("game_directory", profile.gameDir);
		mcVars.put("assets_root", profile.assetsDir);
		mcVars.put("assets_index_name", instance.getAssetsName());
		mcVars.put("version_type", instance.getReleaseType());
		mcVars.put("natives_directory", profile.nativesDir);
		mcVars.put("launcher_name", MCLauncher.BRAND);
		mcVars.put("launcher_version", MCLauncher.VERSION);
		mcVars.put("user_properties", "{}");

		String cpsep = System.getProperty("path.separator");
		mcVars.put("classpath", String.join(cpsep, instance.getLibraries()) + cpsep + profile.gameJar);

		String[] mcArgs = insertArgumentVars(instance.getMinecraftArguments(), mcVars);
		String[] jvmArgsJ = insertArgumentVars(instance.getJvmArguments(), mcVars);

		List<String> args = new ArrayList<String>();
		args.add(profile.jvmCommand);
		for(String p : profile.jvmArgs.split(" ")){
			if(p.length() > 0)
				args.add(p);
		}
		for(String p : jvmArgsJ)
			args.add(p);
		args.add(instance.getMainClass());
		for(String p : mcArgs)
			args.add(p);

		ProcessBuilder pb = new ProcessBuilder(args);
		pb.inheritIO();
		try{
			return pb.start();
		}catch(IOException e){
			throw new UncheckedIOException(e);
		}
	}

	public static String[] insertArgumentVars(String[] args, Map<String, String> vars) {
		for(int i = 0; i < args.length; i++){
			args[i] = insertArgumentVars(args[i], vars);
		}
		return args;
	}

	public static String insertArgumentVars(String arg, Map<String, String> vars) {
		StringBuilder sb = new StringBuilder(arg.length());
		int lastEnd = 0;
		while(true){
			int startIndex = arg.indexOf("${", lastEnd);
			if(startIndex < 0)
				break;
			sb.append(arg.substring(lastEnd, startIndex));
			int endIndex = arg.indexOf('}', startIndex);
			if(endIndex < 0)
				throw new RuntimeException("Missing closing '}' at position " + startIndex);
			lastEnd = endIndex + 1;
			String key = arg.substring(startIndex + 2, endIndex);
			String value = vars.get(key);
			if(value == null)
				throw new RuntimeException("Missing variable value for '" + key + "'");
			sb.append(value);
		}
		sb.append(arg.substring(lastEnd));
		return sb.toString();
	}
}
