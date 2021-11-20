/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher.login;

import java.io.IOException;
import java.util.UUID;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import xyz.user94729.mc.launcher.Util;
import xyz.user94729.mc.launcher.session.OfflinePlayerSession;
import xyz.user94729.mc.launcher.session.PlayerSession;

public class OfflineAuthenticator implements LoginManager {



	@Override
	public PlayerSession doLogin(PlayerSession psession, JPanel ui) throws IOException {
		OfflinePlayerSession session = (OfflinePlayerSession) psession;
		if(session == null)
			session = new OfflinePlayerSession();
		if(session.showEdit){
			if(!this.editPrompt(session, ui))
				return null;
		}
		return session;
	}

	private boolean editPrompt(OfflinePlayerSession session, JPanel ui) {
		int centerX = ui.getWidth() / 2;
		int centerY = ui.getHeight() / 2;
		JTextField playerUUID = new JTextField();
		JTextField playerName = new JTextField();
		JTextField accessToken = new JTextField();
		JCheckBox dontShowAgain = new JCheckBox("Don't show this again");
		Util.addLabel(ui, "Player UUID", centerX - 150, centerY - 100, 300);
		playerUUID.setBounds(centerX - 150, centerY - 80, 300, 20);
		Util.addLabel(ui, "Player Name", centerX - 150, centerY - 50, 300);
		playerName.setBounds(centerX - 150, centerY - 30, 300, 20);
		Util.addLabel(ui, "Access Token", centerX - 150, centerY, 300);
		accessToken.setBounds(centerX - 150, centerY + 20, 300, 20);
		dontShowAgain.setBounds(centerX - 100, centerY + 60, 200, 20);
		ui.add(playerUUID);
		ui.add(playerName);
		ui.add(accessToken);
		ui.add(dontShowAgain);

		if(session != null){
			playerUUID.setText(session.getPlayerUUID());
			playerName.setText(session.getPlayerName());
			accessToken.setText(session.getAccessToken());
		}

		Util.addButton(ui, "Random", centerX + 160, centerY - 80, 50, 20, true, () -> {
			playerUUID.setText(UUID.randomUUID().toString());
		});
		Util.addButton(ui, "Random", centerX + 160, centerY + 20, 50, 20, true, () -> {
			accessToken.setText(org.omegazero.common.util.Util.randomHex(64));
		});

		Object wait = new Object();
		Util.addButton(ui, "OK", centerX - 100, centerY + 90, 80, 30, false, () -> {
			synchronized(wait){
				wait.notify();
			}
		});
		Util.addButton(ui, "Cancel", centerX + 20, centerY + 90, 80, 30, false, () -> {
			playerUUID.setEnabled(false);
			synchronized(wait){
				wait.notify();
			}
		});
		ui.revalidate();
		ui.repaint();
		try{
			synchronized(wait){
				wait.wait();
			}
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		ui.removeAll();
		ui.revalidate();
		ui.repaint();
		if(playerUUID.isEnabled()){
			session.setPlayerUUID(playerUUID.getText());
			session.setPlayerName(playerName.getText());
			session.setAccessToken(accessToken.getText());
			session.showEdit = !dontShowAgain.isSelected();
			return true;
		}else
			return false;
	}


	@Override
	public String toString() {
		return "Offline login (cannot join online servers)";
	}
}
