/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.swing.JButton;
import javax.swing.JLabel;

public final class Util {

	private Util() {
	}


	public static HttpResponse<String> postJson(String url, String payload) throws IOException {
		return post(url, "application/json", "application/json", payload);
	}

	public static HttpResponse<String> post(String url, String contentType, String payload) throws IOException {
		return post(url, contentType, null, payload);
	}

	public static HttpResponse<String> post(String url, String contentType, String accept, String payload) throws IOException {
		try{
			HttpRequest.Builder hb = HttpRequest.newBuilder(new URI(url));
			hb.header("Content-Type", contentType);
			if(accept != null)
				hb.header("Accept", accept);
			HttpRequest request = hb.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
			return HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
		}catch(InterruptedException | URISyntaxException e){
			throw new RuntimeException(e);
		}
	}


	public static JLabel addLabel(java.awt.Container parent, String text, int x, int y, int width) {
		JLabel label = new JLabel(text);
		label.setBounds(x, y, width, 15);
		parent.add(label);
		return label;
	}

	public static JButton addButton(java.awt.Container parent, String text, int x, int y, int width, int height, boolean compact, Runnable onClick) {
		JButton button = new JButton(text);
		button.setBounds(x, y, width, height);
		button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		if(compact){
			button.setBorder(null);
			button.setBackground(java.awt.Color.LIGHT_GRAY);
		}
		if(onClick != null){
			button.addActionListener(new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent e) {
					onClick.run();
				}
			});
		}
		parent.add(button);
		return button;
	}


	public static boolean is64Bit() {
		if(System.getProperty("os.name").toLowerCase().contains("windows")){
			String arch = System.getenv("PROCESSOR_ARCHITECTURE");
			String archW = System.getenv("PROCESSOR_ARCHITEW6432");
			return arch != null && arch.endsWith("64") || archW != null && archW.endsWith("64");
		}else{
			return System.getProperty("os.arch").contains("64");
		}
	}
}
