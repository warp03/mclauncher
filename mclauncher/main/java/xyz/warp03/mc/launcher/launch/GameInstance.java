/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher.launch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.omegazero.common.logging.Logger;

import xyz.warp03.mc.launcher.Util;

public class GameInstance {

	private static final Logger logger = Logger.create();

	public static final String OS_NAME_SHORT;
	public static final String OS_VERSION;
	public static final String OS_ARCH;


	private String[] minecraftArguments;
	private String[] jvmArguments;
	private String mainClass;
	private String assetsName;
	private String releaseType;
	private List<String> libraries = new java.util.ArrayList<>();
	private List<String> nativeLibraries = new java.util.ArrayList<>();

	protected GameInstance() {
	}


	public void extractNatives(Path dest) throws IOException {
		for(String njar : this.nativeLibraries){
			extractNativesJar(njar, dest);
		}
	}


	public String[] getMinecraftArguments() {
		return Arrays.copyOf(this.minecraftArguments, this.minecraftArguments.length);
	}

	public String[] getJvmArguments() {
		return Arrays.copyOf(this.jvmArguments, this.jvmArguments.length);
	}

	public String getMainClass() {
		return this.mainClass;
	}

	public String getAssetsName() {
		return this.assetsName;
	}

	public String getReleaseType() {
		return this.releaseType;
	}

	public List<String> getLibraries() {
		return this.libraries;
	}

	public List<String> getNativeLibraries() {
		return this.nativeLibraries;
	}


	public static GameInstance loadFromJSON(String libraryDir, String[] files, BiConsumer<Float, String> progressCallback) throws IOException {
		GameInstance gi = new GameInstance();
		List<JSONObject> libraries = new java.util.ArrayList<JSONObject>();
		for(int fi = 0; fi < files.length; fi++){
			String f = files[fi];
			progressCallback.accept((float) fi / files.length * .1f, "Loading metadata file " + f);
			JSONObject json = new JSONObject(new String(Files.readAllBytes(Paths.get(f))));

			if(json.has("minecraftArguments")){
				gi.minecraftArguments = json.getString("minecraftArguments").split(" ");
				gi.jvmArguments = new String[] { "-Djava.library.path=${natives_directory}", "-cp", "${classpath}" };
			}else if(json.has("arguments")){
				JSONObject args = json.getJSONObject("arguments");
				gi.minecraftArguments = filterArgsList(args.getJSONArray("game"));
				gi.jvmArguments = filterArgsList(args.getJSONArray("jvm"));
			}
			if(json.has("mainClass"))
				gi.mainClass = json.getString("mainClass");
			if(json.has("assets"))
				gi.assetsName = json.getString("assets");
			if(json.has("type"))
				gi.releaseType = json.getString("type");

			JSONArray jlibraries = json.getJSONArray("libraries");
			for(Object jo : jlibraries){
				if(!(jo instanceof JSONObject))
					throw new IOException("libraries array contains non-objects");
				libraries.add((JSONObject) jo);
			}
		}
		int lcount = 0;
		for(JSONObject jlib : libraries){
			String libName = jlib.getString("name");
			float progress = (float) lcount / libraries.size() * 0.9f + .1f;
			progressCallback.accept(progress, "Loading library " + libName);

			if(jlib.has("rules")){
				JSONArray rules = jlib.getJSONArray("rules");
				if(!checkRules(rules))
					continue;
			}

			boolean nativelibs = jlib.has("natives");

			StringBuilder libPath = new StringBuilder();
			String[] libName0 = libName.split(":");
			libPath.append(libName0[0].replace(".", "/"));
			libPath.append("/" + libName0[1] + "/" + libName0[2]);
			libPath.append("/" + libName0[1] + "-" + libName0[2]);
			if(nativelibs){
				if(!jlib.getJSONObject("natives").keySet().contains(OS_NAME_SHORT))
					continue;
				libPath.append("-" + jlib.getJSONObject("natives").getString(OS_NAME_SHORT).replace("${arch}", Util.is64Bit() ? "64" : "32"));
			}
			libPath.append(".jar");

			Path libraryPath = Paths.get(libraryDir, libPath.toString());
			if(!Files.exists(libraryPath)){
				JSONObject artifactDesc = jlib.getJSONObject("downloads").getJSONObject("artifact");
				progressCallback.accept(progress, "Downloading library " + libName + " from '" + artifactDesc.getString("url") + "'");
				byte[] data = Util.downloadAndVerifyArtifact(artifactDesc);
				Files.createDirectories(libraryPath.getParent());
				Files.write(libraryPath, data);
			}

			if(nativelibs)
				gi.nativeLibraries.add(libraryPath.toString());
			else
				gi.libraries.add(libraryPath.toString());

			lcount++;
		}
		progressCallback.accept(1f, "Done loading game metadata");
		return gi;
	}

	private static String[] filterArgsList(JSONArray list) {
		List<String> t = new java.util.LinkedList<>();
		for(Object o : list){
			if(o instanceof JSONObject){
				if(checkRules(((JSONObject) o).getJSONArray("rules"))){
					Object v = ((JSONObject) o).get("value");
					if(v instanceof String)
						t.add((String) v);
					else if(v instanceof JSONArray){
						for(Object v1 : (JSONArray) v)
							t.add((String) v1);
					}else
						throw new RuntimeException("Invalid type for 'value' in argument list");
				}
			}else if(o instanceof String){
				t.add((String) o);
			}else
				throw new RuntimeException("Element in argument list has invalid type");
		}
		return t.toArray(new String[t.size()]);
	}

	private static boolean checkRules(JSONArray rules) {
		for(int i = 0; i < rules.length(); i++){
			JSONObject rule = rules.getJSONObject(i);
			String action = rule.getString("action");
			for(String ruleType : rule.keySet()){
				if("action".equals(ruleType))
					continue;
				if(ruleType.equals("os")){
					JSONObject osr = rule.getJSONObject("os");
					boolean match = (!osr.has("name") || OS_NAME_SHORT.equals(osr.getString("name")))
							&& (!osr.has("version") || Pattern.compile(osr.getString("version")).matcher(OS_VERSION).find())
							&& (!osr.has("arch") || OS_ARCH.equals(osr.getString("arch")));
					if((action.equals("allow") && !match) || (action.equals("disallow") && match))
						return false;
				}else{
					logger.warn("Not matching rule because of unknown rule type '" + ruleType + "'");
					return false;
				}
			}
		}
		return true;
	}


	public static void extractNativesJar(String jarFile, Path dest) throws IOException {
		try(JarFile jar = new JarFile(jarFile)){
			java.util.Enumeration<JarEntry> enumEntries = jar.entries();
			while(enumEntries.hasMoreElements()){
				JarEntry file = enumEntries.nextElement();
				Path destfile = dest.resolve(file.getName());
				if(file.isDirectory()){
					Files.createDirectories(destfile);
				}else{
					Files.copy(jar.getInputStream(file), destfile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}


	static{
		String os = System.getProperty("os.name").toLowerCase();
		if(os.contains("windows"))
			OS_NAME_SHORT = "windows";
		else if(os.contains("mac"))
			OS_NAME_SHORT = "osx";
		else if(os.contains("linux"))
			OS_NAME_SHORT = "linux";
		else
			OS_NAME_SHORT = "(unknown)";
		OS_VERSION = System.getProperty("os.version", "(unknown)");
		OS_ARCH = System.getProperty("os.arch", "(unknown)");
	}
}
