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
import javax.swing.JProgressBar;
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

	private static final String SETTING_INSTANCE_DIR = "GameProfileWizard.instanceDir";
	private static final String SETTING_INSTALL_DIR = "GameProfileWizard.installDir";


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
	private JProgressBar installProgress;
	private JLabel installProgressLabel;
	private Consumer<GameProfile> onInstallComplete;


	public void initNewInstallPanel(JPanel panel, SettingsManager settings) {
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
		versionListSP.setBounds(20, 40, x1 - 40, panel.getHeight() - 70);
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
		String instanceDir = settings.getString(SETTING_INSTANCE_DIR);
		if(instanceDir == null)
			instanceDir = defaultDir;
		String installDir = settings.getString(SETTING_INSTALL_DIR);
		if(installDir == null)
			installDir = defaultDir;
		this.configInstanceDir.setText(instanceDir);
		this.configInstallDir.setText(installDir);

		this.installProgressLabel = Util.addLabel(panel, "", 20, panel.getHeight() - 30, panel.getWidth() - 40);
		this.installProgressLabel.setVisible(false);
		this.installProgress = new JProgressBar();
		this.installProgress.setBounds(20, panel.getHeight() - 15, panel.getWidth() - 40, 10);
		this.installProgress.setVisible(false);
		panel.add(this.installProgress);

		Util.addButton(panel, "OK", panel.getWidth() - 200, panel.getHeight() - 60, 80, 30, false, () -> {
			settings.set(SETTING_INSTANCE_DIR, GameProfileWizard.this.configInstanceDir.getText());
			settings.set(SETTING_INSTALL_DIR, GameProfileWizard.this.configInstallDir.getText());
			GameProfileWizard.this.completeInstall(true);
		});
		Util.addButton(panel, "Cancel", panel.getWidth() - 100, panel.getHeight() - 60, 80, 30, false, () -> {
			GameProfileWizard.this.completeInstall(false);
		});
	}

	public void showNewInstallUI(Consumer<GameProfile> profileCallback) {
		try{
			this.setNewInstallUIProgress(0, "Loading metadata");
			this.setNewInstallUIState(false);
			this.updateVersions();
		}catch(IOException e){
			logger.error("Error while updating version list: ", e);
			JOptionPane.showMessageDialog(null, "An error occurred while updating the version list: " + e, "Failed to update version list", JOptionPane.ERROR_MESSAGE, null);
		}finally{
			this.setNewInstallUIProgress(100, "Done");
			this.setNewInstallUIState(true);
		}
		this.listVersionsFiltered();
		this.onInstallComplete = profileCallback;
	}

	private void setNewInstallUIState(boolean idle) {
		for(Component c : this.newInstallPanel.getComponents()){
			if(c == this.installProgress || c == this.installProgressLabel)
				continue;
			c.setEnabled(idle);
		}
		this.versionListUI.setEnabled(idle);
		this.installProgress.setVisible(!idle);
		this.installProgressLabel.setVisible(!idle);
	}

	private void setNewInstallUIProgress(int progress, String msg) {
		if(progress >= 0)
			this.installProgress.setValue(progress);
		if(msg != null)
			this.installProgressLabel.setText(msg);
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
		this.setNewInstallUIProgress(1, "Starting installation");
		this.setNewInstallUIState(false);
		GameProfile gp = null;
		try{
			Files.createDirectories(Paths.get(instanceDir));
			Path versionPath = Paths.get(installDir, "versions", version.name);
			Files.createDirectories(versionPath);

			this.setNewInstallUIProgress(5, "Downloading version JSON");
			logger.info("Downloading metadata JSON from '" + version.metaUrl + "'");
			byte[] jsonData = Util.get200(version.metaUrl);
			JSONObject metaJson = new JSONObject(new String(jsonData));
			Path jsonPath = versionPath.resolve(version.name + ".json");
			Files.write(jsonPath, jsonData);

			this.setNewInstallUIProgress(10, "Downloading asset index");
			JSONObject assetDesc = metaJson.getJSONObject("assetIndex");
			Path assetsDir = Paths.get(installDir, "assets");
			Path assetIndexDir = assetsDir.resolve("indexes");
			Files.createDirectories(assetIndexDir);
			Path assetIndexFilePath = assetIndexDir.resolve(assetDesc.getString("id") + ".json");
			byte[] assetData;
			if(!Files.exists(assetIndexFilePath) || !Util.sha1Hex(assetData = Files.readAllBytes(assetIndexFilePath)).equals(assetDesc.getString("sha1"))){
				logger.info("Downloading assets JSON from '" + assetDesc.getString("url") + "'");
				assetData = Util.downloadAndVerifyArtifact(assetDesc);
				Files.write(assetIndexFilePath, assetData);
			}else{
				logger.info("Assets JSON '" + assetIndexFilePath + "' already exists with correct hash");
			}

			JSONObject objects = new JSONObject(new String(assetData)).getJSONObject("objects");
			java.util.Set<String> objectNames = objects.keySet();
			int objectCount = objectNames.size();
			logger.info("Downloading asset objects (" + objectCount + ")");
			int objectP = 0;
			int objectDL = 0;
			for(String path : objectNames){
				String hash = objects.getJSONObject(path).getString("hash");
				String progstr = " assets [" + objectP + "/" + objectCount + "] " + path + " (" + hash + ")";
				this.setNewInstallUIProgress(15 + (objectP * 70 / objectCount), "Processing" + progstr);
				String opath = hash.substring(0, 2) + "/" + hash;
				Path destPath = assetsDir.resolve("objects/" + opath);
				if(!Files.exists(destPath)){
					this.setNewInstallUIProgress(-1, "Downloading" + progstr);
					byte[] data = Util.get200("http://resources.download.minecraft.net/" + opath);
					Files.createDirectories(destPath.getParent());
					Files.write(destPath, data);
					objectDL++;
				}
				objectP++;
			}
			logger.info("Finished processing " + objectCount + " asset objects (" + objectDL + " downloaded)");

			this.setNewInstallUIProgress(85, "Downloading game JAR");
			Path jarPath = versionPath.resolve(version.name + ".jar");
			JSONObject clientJarDesc = metaJson.getJSONObject("downloads").getJSONObject("client");
			if(!Files.exists(jarPath) || !Util.sha1Hex(Files.readAllBytes(jarPath)).equals(clientJarDesc.getString("sha1"))){
				logger.info("Downloading client JAR from '" + clientJarDesc.getString("url") + "'");
				byte[] clientJar = Util.downloadAndVerifyArtifact(clientJarDesc);
				Files.write(jarPath, clientJar);
			}else{
				logger.info("Client JAR '" + jarPath + "' already exists with correct hash");
			}

			this.setNewInstallUIProgress(99, "Finishing up");
			gp = new GameProfile();
			gp.name = this.configInstallName.getText();
			gp.versionName = version.name;
			gp.gameJar = jarPath.toString();
			gp.libraryData = jsonPath.toString();
			gp.libraryDir = installDir + "/libraries";
			gp.assetsDir = assetsDir.toString();
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
			this.setNewInstallUIProgress(100, "Done");
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
