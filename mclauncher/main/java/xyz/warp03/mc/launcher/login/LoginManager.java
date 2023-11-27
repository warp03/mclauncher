/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher.login;

import java.io.IOException;

import javax.swing.JPanel;

import xyz.warp03.mc.launcher.session.PlayerSession;

public interface LoginManager {

	public PlayerSession doLogin(PlayerSession session, JPanel ui) throws IOException;
}
