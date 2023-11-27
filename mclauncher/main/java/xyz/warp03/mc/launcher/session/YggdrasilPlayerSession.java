/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher.session;

public class YggdrasilPlayerSession extends PlayerSession {

	private static final long serialVersionUID = 1L;


	private final String clientToken;

	public YggdrasilPlayerSession(String playerUUID, String playerName, String accessToken, String clientToken) {
		super(playerUUID, playerName, accessToken);
		this.clientToken = clientToken;
	}

	public String getClientToken() {
		return this.clientToken;
	}


	@Override
	public String getUserType() {
		return "mojang";
	}
}
