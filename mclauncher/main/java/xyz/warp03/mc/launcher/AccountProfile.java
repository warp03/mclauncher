/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher;

import java.util.Objects;

import xyz.warp03.mc.launcher.session.PlayerSession;

public class AccountProfile implements java.io.Serializable {

	private static final long serialVersionUID = 1L;


	private String name;
	private final String authenticator;
	private PlayerSession session;

	public AccountProfile(String name, String authenticator) {
		this.name = Objects.requireNonNull(name);
		this.authenticator = Objects.requireNonNull(authenticator);
	}


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name);
	}

	public String getAuthenticator() {
		return this.authenticator;
	}

	public PlayerSession getSession() {
		return this.session;
	}

	public void setSession(PlayerSession session) {
		this.session = session;
	}


	@Override
	public String toString() {
		return this.getName();
	}
}
