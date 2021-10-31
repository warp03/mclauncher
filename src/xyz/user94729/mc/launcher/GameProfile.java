/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher;

public class GameProfile implements java.io.Serializable {

	private static final long serialVersionUID = 1L;


	public String name;
	public String versionName;
	public String gameJar;
	public String libraryData;
	public String libraryDir;
	public String assetsDir;
	public String nativesDir;
	public String gameDir;
	public String jvmCommand;
	public String jvmArgs;


	@Override
	public String toString() {
		return this.name + " (" + this.versionName + ")";
	}
}
