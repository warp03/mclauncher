/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.omegazero.common.event.Tasks;
import org.omegazero.common.logging.Logger;

public class GameProfileWizard {

	private static final Logger logger = Logger.create();


	private String latestVersion;
	private String latestSnapshot;
	private List<Version> versions;

	private JPanel newInstallPanel;
	private JLabel versionLatestUI;
	private JList<Version> versionListUI;
	private JCheckBox showOldVersionsCB;
	private JCheckBox showSnapshotsCB;
	private JTextField configInstallName;
	private JTextField configInstallDir;
	private JTextField configInstanceDir;
	private Consumer<GameProfile> onInstallComplete;


	public void initNewInstallPanel(JPanel panel) {
		if(this.newInstallPanel != null)
			throw new IllegalStateException("Already has a newInstallPanel");
		this.newInstallPanel = panel;

		int x1 = panel.getWidth() / 3;

		this.versionLatestUI = new JLabel();
		this.versionLatestUI.setBounds(20, 20, x1 * 2, 20);
		panel.add(this.versionLatestUI);

		this.versionListUI = new JList<Version>();
		this.versionListUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane versionListSP = new JScrollPane(this.versionListUI);
		versionListSP.setBounds(20, 40, x1 - 40, panel.getHeight() - 60);
		panel.add(versionListSP);

		this.showOldVersionsCB = new JCheckBox("Show old versions");
		this.showSnapshotsCB = new JCheckBox("Show snapshots");
		this.showOldVersionsCB.setBounds(x1, 40, x1, 25);
		this.showSnapshotsCB.setBounds(x1, 65, x1, 25);
		java.awt.event.ActionListener optionsListener = new java.awt.event.ActionListener(){

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				GameProfileWizard.this.listVersionsFiltered();
			}
		};
		this.showOldVersionsCB.addActionListener(optionsListener);
		this.showSnapshotsCB.addActionListener(optionsListener);
		panel.add(this.showOldVersionsCB);
		panel.add(this.showSnapshotsCB);

		this.configInstallName = new JTextField();
		this.configInstanceDir = new JTextField();
		this.configInstallDir = new JTextField();
		this.configInstallName.setBounds(x1, panel.getHeight() - 245, x1 * 2 - 20, 25);
		this.configInstanceDir.setBounds(x1, panel.getHeight() - 185, x1 * 2 - 20, 25);
		this.configInstallDir.setBounds(x1, panel.getHeight() - 125, x1 * 2 - 20, 25);
		panel.add(this.configInstallName);
		panel.add(this.configInstanceDir);
		panel.add(this.configInstallDir);
		Util.addLabel(panel, "Profile Name", x1, panel.getHeight() - 265, 200);
		Util.addLabel(panel, "Instance directory", x1, panel.getHeight() - 205, 200);
		Util.addLabel(panel, "Install directory", x1, panel.getHeight() - 145, 200);
		String defaultDir = System.getenv("APPDATA");
		if(defaultDir == null)
			defaultDir = System.getProperty("user.home");
		defaultDir += "/.minecraft";
		this.configInstanceDir.setText(defaultDir);
		this.configInstallDir.setText(defaultDir);

		Util.addButton(panel, "OK", panel.getWidth() - 200, panel.getHeight() - 50, 80, 30, false, () -> {
			GameProfileWizard.this.completeInstall(true);
		});
		Util.addButton(panel, "Cancel", panel.getWidth() - 100, panel.getHeight() - 50, 80, 30, false, () -> {
			GameProfileWizard.this.completeInstall(false);
		});
	}

	public void showNewInstallUI(Consumer<GameProfile> profileCallback) {
		try{
			this.setNewInstallUIState(false);
			this.updateVersions();
		}catch(IOException e){
			logger.error("Error while updating version list: ", e);
			JOptionPane.showMessageDialog(null, "An error occurred while updating the version list: " + e, "Failed to update version list", JOptionPane.ERROR_MESSAGE, null);
		}finally{
			this.setNewInstallUIState(true);
		}
		this.listVersionsFiltered();
		this.onInstallComplete = profileCallback;
	}

	private void setNewInstallUIState(boolean enabled) {
		for(Component c : this.newInstallPanel.getComponents()){
			c.setEnabled(enabled);
		}
	}

	private void updateVersions() throws IOException {
		JSONObject versionMeta = new JSONObject(new String(Util.get200("https://launchermeta.mojang.com/mc/game/version_manifest.json")));
		JSONObject latest = versionMeta.getJSONObject("latest");
		this.latestVersion = latest.getString("release");
		this.latestSnapshot = latest.getString("snapshot");
		JSONArray versions = versionMeta.getJSONArray("versions");
		List<Version> versionList = new java.util.ArrayList<>();
		for(Object o : versions){
			if(!(o instanceof JSONObject))
				throw new JSONException("Expected JSON objects in 'versions' array");
			versionList.add(Version.from((JSONObject) o));
		}
		this.versions = versionList;
	}

	private void listVersionsFiltered() {
		if(this.versions == null)
			return;
		this.versionLatestUI.setText("Latest version/snapshot: " + this.latestVersion + "/" + this.latestSnapshot);
		boolean oldVersions = this.showOldVersionsCB.isSelected();
		boolean snapshots = this.showSnapshotsCB.isSelected();
		DefaultListModel<Version> uiListModel = new DefaultListModel<Version>();
		for(Version v : this.versions){
			if(v.type.equals("release") || (snapshots && v.type.equals("snapshot")) || (oldVersions && v.type.startsWith("old_")))
				uiListModel.addElement(v);
		}
		this.versionListUI.setModel(uiListModel);
	}

	private void completeInstall(boolean confirm) {
		Tasks.timeout((a) -> {
			if(confirm){
				GameProfile gp = GameProfileWizard.this.doInstall();
				if(gp != null){
					GameProfileWizard.this.onInstallComplete.accept(gp);
					GameProfileWizard.this.onInstallComplete = null;
				}
			}else{
				GameProfileWizard.this.onInstallComplete.accept(null);
				GameProfileWizard.this.onInstallComplete = null;
			}
		}, 0);
	}

	private GameProfile doInstall() {
		Version version = this.versionListUI.getSelectedValue();
		String name = this.configInstallName.getText();
		if(version == null || name.length() < 1){
			JOptionPane.showMessageDialog(null, "Name must not be empty and version must be selected", "Invalid configuration", JOptionPane.INFORMATION_MESSAGE, null);
			return null;
		}
		String instanceDir = this.configInstanceDir.getText();
		String installDir = this.configInstallDir.getText();
		logger.info("Installing version: " + version + " in " + installDir);
		this.setNewInstallUIState(false);
		GameProfile gp = null;
		try{
			Files.createDirectories(Paths.get(instanceDir));
			Path versionPath = Paths.get(installDir, "versions", version.name);
			Files.createDirectories(versionPath);

			logger.info("Downloading metadata JSON from '" + version.metaUrl + "'");
			byte[] jsonData = Util.get200(version.metaUrl);
			JSONObject metaJson = new JSONObject(new String(jsonData));
			Path jsonPath = versionPath.resolve(version.name + ".json");
			Files.write(jsonPath, jsonData);

			JSONObject assetDesc = metaJson.getJSONObject("assetIndex");
			String assetsDir = installDir + "/assets";
			Path assetIndexDir = Paths.get(assetsDir, "indexes");
			Path assetIndexFilePath = assetIndexDir.resolve(assetDesc.getString("id") + ".json");
			if(!Files.exists(assetIndexFilePath) || !Util.sha1Hex(Files.readAllBytes(assetIndexFilePath)).equals(assetDesc.getString("sha1"))){
				logger.info("Downloading assets JSON from '" + assetDesc.getString("url") + "'");
				byte[] assetData = Util.downloadAndVerifyArtifact(assetDesc);
				Files.write(assetIndexFilePath, assetData);
			}else{
				logger.info("Assets JSON '" + assetIndexFilePath + "' already exists with correct hash");
			}

			Path jarPath = versionPath.resolve(version.name + ".jar");
			JSONObject clientJarDesc = metaJson.getJSONObject("downloads").getJSONObject("client");
			if(!Files.exists(jarPath) || !Util.sha1Hex(Files.readAllBytes(jarPath)).equals(clientJarDesc.getString("sha1"))){
				logger.info("Downloading client JAR from '" + clientJarDesc.getString("url") + "'");
				byte[] clientJar = Util.downloadAndVerifyArtifact(clientJarDesc);
				Files.write(jarPath, clientJar);
			}else{
				logger.info("Client JAR '" + jarPath + "' already exists with correct hash");
			}

			gp = new GameProfile();
			gp.name = this.configInstallName.getText();
			gp.versionName = version.name;
			gp.gameJar = jarPath.toString();
			gp.libraryData = jsonPath.toString();
			gp.libraryDir = installDir + "/libraries";
			gp.assetsDir = assetsDir;
			gp.nativesDir = null;
			gp.gameDir = instanceDir;
			gp.jvmCommand = "java";
			gp.jvmArgs = "";

			logger.info("Installation of '" + version + "' complete");
			JOptionPane.showMessageDialog(null, "Successfully installed version '" + version + "'", "Installation complete", JOptionPane.INFORMATION_MESSAGE, null);
		}catch(Exception e){
			logger.error("Error while installing new version '", version, "': ", e);
			JOptionPane.showMessageDialog(null, "Error while installing '" + version + "': " + e, "Install failed", JOptionPane.ERROR_MESSAGE, null);
		}finally{
			this.setNewInstallUIState(true);
		}
		return gp;
	}


	private static class Version {

		public final String name;
		public final String type;
		public final String metaUrl;

		public Version(String name, String type, String metaUrl) {
			this.name = name;
			this.type = type;
			this.metaUrl = metaUrl;
		}


		@Override
		public String toString() {
			return this.name + " (" + this.type.replace('_', ' ') + ")";
		}


		public static Version from(JSONObject json) {
			return new Version(json.getString("id"), json.getString("type"), json.getString("url"));
		}
	}
}
