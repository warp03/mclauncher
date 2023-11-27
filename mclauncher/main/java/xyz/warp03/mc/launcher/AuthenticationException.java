/*
 * Copyright (C) 2021-2023 warp03
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.warp03.mc.launcher;

import java.io.IOException;

public class AuthenticationException extends IOException {

	private static final long serialVersionUID = 1L;


	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
}
