/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher.login;

import java.io.IOException;
import java.net.http.HttpResponse;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.json.JSONObject;
import org.omegazero.common.logging.Logger;

import xyz.user94729.mc.launcher.AuthenticationException;
import xyz.user94729.mc.launcher.Util;
import xyz.user94729.mc.launcher.session.PlayerSession;
import xyz.user94729.mc.launcher.session.YggdrasilPlayerSession;

public class YggdrasilAuthenticator implements LoginManager {

	private static final Logger logger = Logger.create();


	@Override
	public PlayerSession doLogin(PlayerSession psession, JPanel ui) throws IOException {
		if(psession == null){
			logger.info("Creating new session");
			String clientToken = org.omegazero.common.util.Util.randomHex(32);
			JSONObject json = loginPrompt(clientToken, ui);
			if(json == null)
				return null;
			JSONObject profile = json.getJSONObject("selectedProfile");
			psession = new YggdrasilPlayerSession(profile.getString("id"), profile.getString("name"), json.getString("accessToken"), clientToken);
		}
		YggdrasilPlayerSession session = (YggdrasilPlayerSession) psession;
		if(!this.checkTokenValid(session)){
			if(!this.refreshToken(session)){
				logger.info("Asking user to login again");
				JSONObject json = this.loginPrompt(session.getClientToken(), ui);
				if(json == null)
					return null;
				this.refreshSession(session, json);
			}
		}
		return session;
	}

	private JSONObject loginPrompt(String clientToken, JPanel ui) throws IOException {
		int centerX = ui.getWidth() / 2;
		int centerY = ui.getHeight() / 2;
		JTextField username = new JTextField();
		JTextField password = new JPasswordField();
		Util.addLabel(ui, "Username", centerX - 100, centerY - 90, 200);
		username.setBounds(centerX - 100, centerY - 70, 200, 20);
		Util.addLabel(ui, "Password", centerX - 100, centerY - 30, 200);
		password.setBounds(centerX - 100, centerY - 10, 200, 20);
		ui.add(username);
		ui.add(password);
		Object wait = new Object();
		Util.addButton(ui, "OK", centerX - 100, centerY + 50, 80, 30, false, () -> {
			synchronized(wait){
				wait.notify();
			}
		});
		Util.addButton(ui, "Cancel", centerX + 20, centerY + 50, 80, 30, false, () -> {
			username.setEnabled(false);
			synchronized(wait){
				wait.notify();
			}
		});
		ui.repaint();
		try{
			synchronized(wait){
				wait.wait();
			}
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		ui.removeAll();
		ui.repaint();
		if(username.isEnabled())
			return login(username.getText(), password.getText(), clientToken);
		else
			return null;
	}

	private boolean checkTokenValid(YggdrasilPlayerSession session) throws IOException {
		try{
			logger.info("Validating access token");
			validateAccessToken(session.getClientToken(), session.getAccessToken());
			return true;
		}catch(AuthenticationException e){
			logger.warn("Token validation failed: " + e.getMessage());
			return false;
		}
	}

	private boolean refreshToken(YggdrasilPlayerSession session) throws IOException {
		try{
			logger.info("Refreshing access token");
			this.refreshSession(session, refreshAccessToken(session.getClientToken(), session.getAccessToken()));
			return true;
		}catch(AuthenticationException e){
			logger.warn("Token refresh failed: " + e.getMessage());
			return false;
		}
	}

	private void refreshSession(YggdrasilPlayerSession session, JSONObject apiResponse) {
		JSONObject profile = apiResponse.getJSONObject("selectedProfile");
		if(!session.getPlayerUUID().equals(profile.get("id")))
			throw new RuntimeException("UUIDs do not match");
		session.setPlayerName(profile.getString("name"));
		session.setAccessToken(apiResponse.getString("accessToken"));
	}


	/**
	 * Logs in with the given username and password.
	 * 
	 * @param username    The username
	 * @param password    The password
	 * @param clientToken A persistent randomly generated hexadecimal string which will be associated with the access token
	 * @return The returned JSON object, including access token and player account data
	 * @throws IOException             If an IO error occurs
	 * @throws AuthenticationException If login fails, likely because the username or password is incorrect
	 */
	public static JSONObject login(String username, String password, String clientToken) throws IOException {
		String payload = "{\"agent\": {\"name\": \"Minecraft\",\"version\": 1},\"username\": \"" + username + "\",\"password\": \"" + password + "\",\"clientToken\": \""
				+ clientToken + "\",\"requestUser\": true}";
		HttpResponse<String> res = Util.postJson("https://authserver.mojang.com/authenticate", payload);
		JSONObject data = new JSONObject(res.body());
		if(res.statusCode() == 200 && !data.has("error")){
			return data;
		}else
			throw new AuthenticationException("Login failed: " + data.getString("error") + ": " + data.getString("errorMessage"));
	}

	/**
	 * Refreshes the access token for the given user account session.
	 * 
	 * @param clientToken The persistent randomly generated hexadecimal string with which the access token was generated
	 * @param accessToken The current access token
	 * @return The returned JSON object, including refreshed access token and player account data
	 * @throws IOException             If an IO error occurs
	 * @throws AuthenticationException If the refresh fails, likely because the client token has expired
	 */
	public static JSONObject refreshAccessToken(String clientToken, String accessToken) throws IOException {
		String payload = "{\"accessToken\": \"" + accessToken + "\",\"clientToken\": \"" + clientToken + "\"" + "\",\"requestUser\": true}";
		HttpResponse<String> res = Util.postJson("https://authserver.mojang.com/refresh", payload);
		JSONObject data = new JSONObject(res.body());
		if(res.statusCode() == 200 && !data.has("error")){
			return data;
		}else
			throw new AuthenticationException("Invalid token: " + data.getString("error") + ": " + data.getString("errorMessage"));
	}

	/**
	 * Checks if the given access token is valid.
	 * 
	 * @param clientToken The persistent randomly generated hexadecimal string with which the access token was generated
	 * @param accessToken The access token to check for validity
	 * @throws IOException             If an IO error occurs
	 * @throws AuthenticationException If the token is invalid
	 */
	public static void validateAccessToken(String clientToken, String accessToken) throws IOException {
		String payload = "{\"accessToken\": \"" + accessToken + "\",\"clientToken\": \"" + clientToken + "\"}";
		HttpResponse<String> res = Util.postJson("https://authserver.mojang.com/validate", payload);
		if(res.statusCode() != 204){
			JSONObject data = new JSONObject(res.body());
			throw new AuthenticationException("Invalid token: " + data.getString("error") + ": " + data.getString("errorMessage"));
		}
	}


	@Override
	public String toString() {
		return "Yggdrasil login (default until 2021)";
	}
}
