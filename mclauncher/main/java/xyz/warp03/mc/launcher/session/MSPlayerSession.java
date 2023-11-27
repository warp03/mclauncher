/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher.session;

public class MSPlayerSession extends PlayerSession {

	private static final long serialVersionUID = 1L;

	public static final long refreshTimeout = 43200000; // 12 hours


	public String msAccessToken;
	public String msRefreshToken;
	public String xblToken;
	public String xblUserHash;
	public String xstsToken;
	public long lastRefresh;


	@Override
	public String getUserType() {
		return "msa";
	}
}
