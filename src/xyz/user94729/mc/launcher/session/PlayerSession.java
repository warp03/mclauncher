/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher.session;

public abstract class PlayerSession implements java.io.Serializable {

	private static final long serialVersionUID = 1L;


	private String playerUUID;
	private String playerName;
	private String accessToken;

	public PlayerSession() {
	}

	public PlayerSession(String playerUUID, String playerName, String accessToken) {
		this.playerUUID = playerUUID;
		this.playerName = playerName;
		this.accessToken = accessToken;
	}


	public String getPlayerUUID() {
		return this.playerUUID;
	}

	public void setPlayerUUID(String playerUUID) {
		this.playerUUID = playerUUID;
	}

	public String getPlayerName() {
		return this.playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getAccessToken() {
		return this.accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}


	public abstract String getUserType();
}
