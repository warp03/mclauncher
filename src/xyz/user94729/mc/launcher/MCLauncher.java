/*
 * Copyright (C) 2021 user94729
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package xyz.user94729.mc.launcher;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import org.omegazero.common.event.Tasks;
import org.omegazero.common.logging.Logger;
import org.omegazero.common.logging.LoggerUtil;
import org.omegazero.common.util.Args;

import xyz.user94729.mc.launcher.launch.GameInstance;
import xyz.user94729.mc.launcher.launch.LaunchHandler;
import xyz.user94729.mc.launcher.login.LoginManager;
import xyz.user94729.mc.launcher.session.PlayerSession;

public final class MCLauncher {

	public static final String BRAND = "U9 MCLauncher";
	public static final String VERSION = "2.1";

	private static final Logger logger = Logger.create();

	private static final int WINDOW_WIDTH = 800;
	private static final int WINDOW_HEIGHT = 500;

	private static final String SETTING_SELECTED_GAME_PROFILE = "prevSelectedGameProfile";
	private static final String SETTING_SELECTED_ACCOUNT_PROFILE = "prevSelectedAccountProfile";


	private State state = State.NEW;

	private final File dataFile;
	private List<LoginManager> loginManagers = new java.util.ArrayList<>();

	private List<GameProfile> profiles = new java.util.ArrayList<>();
	private List<AccountProfile> accounts = new java.util.ArrayList<>();
	private SettingsManager settings = new SettingsManager();

	private JFrame mainFrame;
	private Map<State, JPanel> statePanels = new java.util.HashMap<>();

	private JComboBox<GameProfile> selectGameProfile;
	private JComboBox<AccountProfile> selectAccount;
	private JLabel loadingLabel;
	private JProgressBar loadingBar;
	private GameProfileWizard gpManager;

	public MCLauncher(Args args) {
		this.dataFile = new File(args.getValueOrDefault("stateFile", "mclauncher_data.ser"));
		this.loginManagers.add(new xyz.user94729.mc.launcher.login.YggdrasilAuthenticator());
		this.loginManagers.add(new xyz.user94729.mc.launcher.login.MSAuthenticator());
	}


	@SuppressWarnings("unchecked")
	private void loadState() {
		if(!this.dataFile.canRead())
			return;
		logger.info("Loading data from ", this.dataFile);
		try(ObjectInputStream ois = new ObjectInputStream(new java.io.FileInputStream(this.dataFile))){
			this.profiles = (List<GameProfile>) ois.readObject();
			this.accounts = (List<AccountProfile>) ois.readObject();
			this.settings = (SettingsManager) ois.readObject();
		}catch(Exception e){
			logger.error("Error while loading data from '", this.dataFile, "': ", e);
			this.showError("IO error", "Error while loading data from " + this.dataFile);
		}
	}

	private void saveState() {
		logger.info("Saving data to ", this.dataFile);
		try(ObjectOutputStream oos = new ObjectOutputStream(new java.io.FileOutputStream(this.dataFile))){
			oos.writeObject(this.profiles);
			oos.writeObject(this.accounts);
			oos.writeObject(this.settings);
		}catch(Exception e){
			logger.error("Error while saving data to '", this.dataFile, "': ", e);
			this.showError("IO error", "Error while saving data to " + this.dataFile);
		}
	}


	public void init() {
		this.requireState(State.NEW);
		this.updateState(State.INITIALIZING);
		logger.info(BRAND + " v" + VERSION);
		this.loadState();
		logger.info("Configuration: " + this.profiles.size() + " game profiles, " + this.accounts.size() + " accounts");

		logger.info("Initializing launcher window");
		this.mainFrame = new JFrame(BRAND + " v" + VERSION);
		this.mainFrame.setResizable(false);
		this.mainFrame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		this.mainFrame.setLocationRelativeTo(null);
		this.mainFrame.setLayout(null); // too lazy to use a layout manager
		this.mainFrame.addWindowListener(new WindowAdapter(){

			@Override
			public void windowClosing(WindowEvent event) {
				Tasks.timeout((a) -> {
					try{
						MCLauncher.this.shutdown();
					}catch(Exception e){
						logger.error("Error during shutdown: ", e);
						MCLauncher.this.showError("Error during shutdown",
								"An unexpected error occurred during shutdown: " + e.toString() + "\nLauncher will be forcibly closed");
						System.exit(10);
					}
				}, 0);
			}
		});
		this.mainFrame.setVisible(true);

		this.initLaunchPanel();

		this.gpManager = new GameProfileWizard();
		this.gpManager.initNewInstallPanel(this.newStatePanel(State.INSTALL), this.settings);

		logger.info("Initialization complete");
		this.updateState(State.WAITING);
	}

	private JPanel newStatePanel(State state) {
		if(this.statePanels.containsKey(state))
			return null;
		if(!this.mainFrame.isVisible())
			throw new IllegalStateException("mainFrame must be visible when adding state panels");
		JPanel jp = new JPanel();
		jp.setLayout(null);
		jp.setBounds(0, 0, this.mainFrame.getContentPane().getWidth(), this.mainFrame.getContentPane().getHeight());
		jp.setVisible(false);
		this.mainFrame.add(jp);
		this.statePanels.put(state, jp);
		return jp;
	}

	private void initLaunchPanel() {
		JPanel jp = this.newStatePanel(State.WAITING);
		if(jp == null)
			return;

		Util.addLabel(jp, "Game Profile", 40, jp.getHeight() - 140, 100);
		this.selectGameProfile = new JComboBox<>();
		this.selectGameProfile.setBounds(40, jp.getHeight() - 120, 200, 25);
		jp.add(this.selectGameProfile);
		Util.addButton(jp, "Edit", 250, jp.getHeight() - 120, 50, 25, true, () -> {
			GameProfile profile = (GameProfile) MCLauncher.this.selectGameProfile.getSelectedItem();
			MCLauncher.this.editGameProfilePopup(profile);
		});
		Util.addButton(jp, "x", 310, jp.getHeight() - 120, 25, 25, true, () -> {
			GameProfile profile = (GameProfile) MCLauncher.this.selectGameProfile.getSelectedItem();
			if(JOptionPane.showConfirmDialog(null, "Are you sure you would like to delete profile '" + profile + "'", "Delete game profile",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
				MCLauncher.this.profiles.remove(profile);
				logger.info("Deleted game profile '" + profile + "'");
				if(MCLauncher.this.settings.get(SETTING_SELECTED_GAME_PROFILE) == profile)
					MCLauncher.this.settings.set(SETTING_SELECTED_GAME_PROFILE, null);
				MCLauncher.this.updateComboBoxContents();
			}
		});
		Util.addButton(jp, "+", 345, jp.getHeight() - 120, 25, 25, true, this::addGameProfilePopup);

		Util.addLabel(jp, "Account", jp.getWidth() / 2, jp.getHeight() - 140, 100);
		this.selectAccount = new JComboBox<>();
		this.selectAccount.setBounds(jp.getWidth() / 2, jp.getHeight() - 120, 200, 25);
		jp.add(this.selectAccount);
		Util.addButton(jp, "Edit", jp.getWidth() / 2 + 210, jp.getHeight() - 120, 50, 25, true, () -> {
			AccountProfile account = (AccountProfile) MCLauncher.this.selectAccount.getSelectedItem();
			MCLauncher.this.editAccountPopup(account);
		});
		Util.addButton(jp, "x", jp.getWidth() / 2 + 270, jp.getHeight() - 120, 25, 25, true, () -> {
			AccountProfile account = (AccountProfile) MCLauncher.this.selectAccount.getSelectedItem();
			if(JOptionPane.showConfirmDialog(null, "Are you sure you would like to delete account '" + account + "'", "Delete account",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
				MCLauncher.this.accounts.remove(account);
				logger.info("Deleted account '" + account + "'");
				if(MCLauncher.this.settings.get(SETTING_SELECTED_ACCOUNT_PROFILE) == account)
					MCLauncher.this.settings.set(SETTING_SELECTED_ACCOUNT_PROFILE, null);
				MCLauncher.this.updateComboBoxContents();
			}
		});
		Util.addButton(jp, "+", jp.getWidth() / 2 + 305, jp.getHeight() - 120, 25, 25, true, this::addAccountPopup);

		this.updateComboBoxContents();

		Util.addButton(jp, "Launch", jp.getWidth() / 2 - 150, jp.getHeight() - 70, 300, 50, false, () -> {
			GameProfile profile = (GameProfile) MCLauncher.this.selectGameProfile.getSelectedItem();
			AccountProfile account = (AccountProfile) MCLauncher.this.selectAccount.getSelectedItem();
			if(profile == null || account == null){
				MCLauncher.this.showError("Invalid configuration", "Game Profile and Account must be selected");
				return;
			}
			Tasks.timeout((a) -> {
				try{
					MCLauncher.this.settings.set(SETTING_SELECTED_GAME_PROFILE, profile);
					MCLauncher.this.settings.set(SETTING_SELECTED_ACCOUNT_PROFILE, account);
					MCLauncher.this.saveState();
					MCLauncher.this.launch(profile, account);
				}catch(Exception e){
					logger.error("Error during launch: ", e);
					MCLauncher.this.showError("Launch failed", "An unexpected error occurred while launching: " + e.toString());
					this.updateState(State.WAITING);
				}
			}, 0);
		});
	}

	private void initLoadingPanel() {
		JPanel jp = this.newStatePanel(State.LAUNCHING);
		if(jp == null)
			return;

		this.loadingLabel = Util.addLabel(jp, "", 10, jp.getHeight() - 90, jp.getWidth());
		this.loadingBar = new JProgressBar();
		this.loadingBar.setBounds(10, jp.getHeight() - 70, jp.getWidth() - 20, 20);
		jp.add(this.loadingBar);
	}


	private void addGameProfilePopup() {
		JComboBox<String> selectAction = new JComboBox<>(new String[] { "Install new version", "Manually import existing installation" });
		Object[] message = { "Select action: ", selectAction };
		if(JOptionPane.showConfirmDialog(null, message, "Add game profile", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
			int action = selectAction.getSelectedIndex();
			if(action == 0){
				this.updateState(State.INSTALL);
				Tasks.timeout((a) -> {
					this.gpManager.showNewInstallUI((profile) -> {
						if(profile != null){
							this.profiles.add(profile);
							logger.info("Added newly installed game profile from GameProfileWizard: name='" + profile.name + "' versionName='" + profile.versionName + "'");
							MCLauncher.this.settings.set(SETTING_SELECTED_GAME_PROFILE, profile);
							this.updateComboBoxContents();
						}
						MCLauncher.this.updateState(State.WAITING);
					});
				}, 0);
			}else if(action == 1){
				this.editGameProfilePopup(null);
			}else{
				this.showError("Add game profile", "Invalid action: " + action);
			}
		}
	}

	private void editGameProfilePopup(GameProfile gp) {
		// TODO maybe make this A BIT more user friendly..
		boolean newProfile = gp == null;
		JTextField profileName = new JTextField(gp != null ? gp.name : "");
		JTextField versionName = new JTextField(gp != null ? gp.versionName : "");
		JTextField gameJar = new JTextField(gp != null ? gp.gameJar : "");
		JTextField libraryData = new JTextField(gp != null ? gp.libraryData : "");
		JTextField libraryDir = new JTextField(gp != null ? gp.libraryDir : "");
		JTextField assetsDir = new JTextField(gp != null ? gp.assetsDir : "");
		JTextField nativesDir = new JTextField(gp != null ? gp.nativesDir : "");
		JTextField gameDir = new JTextField(gp != null ? gp.gameDir : "");
		JTextField jvmCommand = new JTextField(gp != null ? gp.jvmCommand : "java");
		JTextField jvmArgs = new JTextField(gp != null ? gp.jvmArgs : "");
		Object[] message = { "Profile Name: ", profileName, "Version name:", versionName, "Game JAR:", gameJar, "Library Data JSON file:", libraryData,
				"Libraries Base Directory:", libraryDir, "Assets Directory:", assetsDir, "Natives Directory:", nativesDir, "Game Directory:", gameDir, "JVM Command:",
				jvmCommand, "JVM Arguments:", jvmArgs };
		if(JOptionPane.showConfirmDialog(null, message, (newProfile ? "Add" : "Edit") + " game profile", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
			if(newProfile)
				gp = new GameProfile();
			gp.name = profileName.getText();
			gp.versionName = versionName.getText();
			gp.gameJar = gameJar.getText();
			gp.libraryData = libraryData.getText();
			gp.libraryDir = libraryDir.getText();
			gp.assetsDir = assetsDir.getText();
			gp.nativesDir = nativesDir.getText();
			gp.gameDir = gameDir.getText();
			gp.jvmCommand = jvmCommand.getText();
			gp.jvmArgs = jvmArgs.getText();
			if(newProfile){
				this.profiles.add(gp);
				logger.info("Added new game profile: name='" + gp.name + "' versionName='" + gp.versionName + "'");
			}else
				logger.info("Edited game profile '" + gp + "'");
			this.settings.set(SETTING_SELECTED_GAME_PROFILE, gp);
			this.updateComboBoxContents();
		}
	}

	private void addAccountPopup() {
		JTextField accountName = new JTextField();
		JComboBox<LoginManager> selectAuthenticator = new JComboBox<>(this.loginManagers.toArray(new LoginManager[this.loginManagers.size()]));
		Object[] message = { "Account Name (need not be player name):", accountName, "Authenticator:", selectAuthenticator, " ",
				"You will be prompted to login when launching with this profile selected" };
		while(JOptionPane.showConfirmDialog(null, message, "Add account", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
			if(accountName.getText().length() < 1){
				this.showError("Invalid name", "Name must be at least 1 character");
				continue;
			}
			AccountProfile np = new AccountProfile(accountName.getText(), selectAuthenticator.getSelectedItem().getClass().getName());
			this.accounts.add(np);
			logger.info("Added new account profile: name='" + np.getName() + "' authenticator='" + np.getAuthenticator() + "'");
			this.settings.set(SETTING_SELECTED_ACCOUNT_PROFILE, np);
			this.updateComboBoxContents();
			break;
		}
	}

	private void editAccountPopup(AccountProfile ap) {
		JTextField accountName = new JTextField(ap.getName());
		JTextField selectAuthenticator = new JTextField(ap.getAuthenticator());
		selectAuthenticator.setEditable(false);
		Object[] message = { "Account Name:", accountName, "Authenticator:", selectAuthenticator };
		while(JOptionPane.showConfirmDialog(null, message, "Edit account", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
			if(accountName.getText().length() > 0){
				ap.setName(accountName.getText());
				this.settings.set(SETTING_SELECTED_ACCOUNT_PROFILE, ap);
				this.updateComboBoxContents();
				break;
			}
			this.showError("Invalid name", "Name must be at least 1 character");
		}
	}

	private void updateComboBoxContents() {
		this.profiles.sort((gp1, gp2) -> {
			return gp1.toString().compareTo(gp2.toString());
		});
		this.accounts.sort((ap1, ap2) -> {
			return ap1.toString().compareTo(ap2.toString());
		});
		this.selectGameProfile.setModel(new DefaultComboBoxModel<GameProfile>(this.profiles.toArray(new GameProfile[this.profiles.size()])));
		this.selectAccount.setModel(new DefaultComboBoxModel<AccountProfile>(this.accounts.toArray(new AccountProfile[this.accounts.size()])));
		if(this.settings.get(SETTING_SELECTED_GAME_PROFILE) != null)
			this.selectGameProfile.setSelectedItem(this.settings.get(SETTING_SELECTED_GAME_PROFILE));
		if(this.settings.get(SETTING_SELECTED_ACCOUNT_PROFILE) != null)
			this.selectAccount.setSelectedItem(this.settings.get(SETTING_SELECTED_ACCOUNT_PROFILE));
	}

	private void updateLoadingState(int percentage, String msg) {
		if(percentage >= 0)
			this.loadingBar.setValue(percentage);
		if(msg != null)
			this.loadingLabel.setText(msg);
		logger.debug("[", percentage, "%] ", msg);
	}


	public void launch(GameProfile profile, AccountProfile account) {
		this.initLoadingPanel();
		this.requireState(State.WAITING);
		this.updateState(State.LAUNCHING);

		logger.info("Logging in with account '", account, "'");
		this.updateLoadingState(2, "Logging in");
		LoginManager authenticator = this.resolveLoginManager(account.getAuthenticator());

		PlayerSession session;
		JPanel loginPanel = new JPanel();
		loginPanel.setLayout(null);
		loginPanel.setBounds(0, 0, this.mainFrame.getContentPane().getWidth(), this.mainFrame.getContentPane().getHeight() - 200);
		this.mainFrame.add(loginPanel);
		try{
			session = authenticator.doLogin(account.getSession(), loginPanel);
			if(session == null)
				throw new NullPointerException("session is null");
			if(session.getAccessToken() == null)
				throw new NullPointerException("session accessToken is null");
		}catch(IOException e){
			logger.warn("Login failed: ", e);
			String emsg = e.getMessage();
			if(emsg == null && e instanceof java.net.SocketException)
				emsg = "Failed to connect to server";
			this.showError("Login failed", "Error while logging in: " + emsg);
			this.updateState(State.WAITING);
			return;
		}finally{
			this.mainFrame.remove(loginPanel);
			this.mainFrame.revalidate();
			this.mainFrame.repaint();
		}
		account.setSession(session); // usually stays the same
		logger.info("Login successful as '", session.getPlayerName(), "' (UUID ", session.getPlayerUUID(), ")");

		logger.info("Launching minecraft with game profile '", profile, "' and player '", session.getPlayerName(), "'");
		try{
			GameInstance instance = GameInstance.loadFromJSON(profile.libraryDir, profile.libraryData.split("::"), (frac, msg) -> {
				MCLauncher.this.updateLoadingState((int) (10 + frac * 80), msg);
			});
			this.updateLoadingState(92, "Launching minecraft");

			Process p = LaunchHandler.launchMinecraft(instance, profile, session);
			logger.info("Minecraft process started");
			this.updateLoadingState(100, "Done");
			this.mainFrame.dispose();
			int status = p.waitFor();
			if(status != 0)
				this.showError("Minecraft exited abnormally", "Minecraft exited with status " + status);
			this.mainFrame.setVisible(true);
			this.updateState(State.WAITING);
		}catch(IOException e){
			logger.error("Error while launching minecraft: ", e);
			this.showError("Launch failed", "Error while launching minecraft: " + e.getMessage());
			this.updateState(State.WAITING);
		}catch(InterruptedException e){
			logger.fatal("Interrupted unexpectedly while waiting for minecraft process to exit");
			System.exit(11);
		}
	}

	private LoginManager resolveLoginManager(String name) {
		LoginManager authenticator = null;
		for(LoginManager lm : this.loginManagers){
			if(name.equals(lm.getClass().getName())){
				authenticator = lm;
				break;
			}
		}
		if(authenticator == null)
			throw new IllegalArgumentException("Authenticator not found: " + name);
		return authenticator;
	}


	public void shutdown() {
		this.requireStateBelow(State.CLOSING);
		this.updateState(State.CLOSING);
		logger.info("Shutting down");
		this.saveState();
		if(this.mainFrame != null){
			this.mainFrame.dispose();
		}
		LoggerUtil.close();
		System.exit(0); // this is fine
	}


	private void showError(String title, String msg) {
		logger.warn("Showing error: " + title + ": " + msg);
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE, null);
	}

	private void updateState(State newState) {
		logger.debug("State change: ", this.state, " -> ", newState);
		this.state = newState;
		if(this.mainFrame != null){
			for(JPanel jp : this.statePanels.values())
				jp.setVisible(false);
			JPanel jp = this.statePanels.get(this.state);
			if(jp != null)
				jp.setVisible(true);
		}
	}

	private void requireState(State state) {
		if(this.state.value() != state.value())
			throw new IllegalStateException("Requires state " + state + " but launcher is in state " + this.state);
	}

	private void requireStateBelow(State state) {
		if(this.state.value() >= state.value())
			throw new IllegalStateException("Requires state before " + state + " but launcher is already in state " + this.state);
	}
}
