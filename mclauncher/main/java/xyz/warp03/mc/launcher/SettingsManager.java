/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher;

import java.util.Map;

public class SettingsManager implements java.io.Serializable {

	private static final long serialVersionUID = 1L;


	private final Map<String, Object> settings = new java.util.HashMap<>();


	public void set(String name, Object value) {
		this.settings.put(name, value);
	}

	public Object get(String name) {
		return this.settings.get(name);
	}

	public String getString(String name) {
		Object o = this.settings.get(name);
		if(o == null)
			return null;
		else
			return o.toString();
	}

	public int getInt(String name, int def) {
		try{
			return Integer.parseInt(String.valueOf(this.settings.get(name)));
		}catch(NumberFormatException e){
			return def;
		}
	}
}
