/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher;

public enum State {
	NEW(0), INITIALIZING(1), WAITING(2), LAUNCHING(3), RUNNING(4), CLOSING(5);

	private final int value;

	private State(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}
}
