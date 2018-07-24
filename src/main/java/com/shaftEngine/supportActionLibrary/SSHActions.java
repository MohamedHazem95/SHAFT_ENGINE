package com.shaftEngine.supportActionLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.testng.Assert;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.shaftEngine.ioActionLibrary.FileManager;
import com.shaftEngine.ioActionLibrary.ReportManager;

public class SSHActions {

	private static void passAction(String actionName, String testData, String log) {
		String message = "Successfully performed action [" + actionName + "].";
		if (testData != null) {
			message = message + " With the following test data [" + testData + "].";
		}
		ReportManager.log(message);
		if (log != null) {
			ReportManager.attach("SSH Response data", log);
		}
	}

	private static void passAction(String actionName, String testData) {
		passAction(actionName, testData, null);
	}

	private static void failAction(String actionName, String testData, String log) {
		String message = "Failed to perform action [" + actionName + "].";
		if (testData != null) {
			message = message + " With the following test data [" + testData + "].";
		}
		ReportManager.log(message);
		if (log != null) {
			ReportManager.attach("SSH Response data", log);
		}
		Assert.fail(message);
	}

	private static void failAction(String actionName, String testData) {
		failAction(actionName, testData, null);
	}

	private static Session createSSHsession(String hostname, int sshPortNumber, String username,
			String keyFileFolderName, String keyFileName) {
		Session session = null;
		String testData = hostname + ", " + sshPortNumber + ", " + username + ", " + keyFileFolderName + ", "
				+ keyFileName;
		try {
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			JSch jsch = new JSch();

			jsch.addIdentity(FileManager.getAbsoluteFilePath(keyFileFolderName, keyFileName));
			session = jsch.getSession(username, hostname, sshPortNumber);
			session.setConfig(config);

			session.connect();
			// System.out.println("Connected");
			passAction("createSSHsession", testData);
		} catch (JSchException e) {
			e.printStackTrace();
			failAction("createSSHsession", testData);
		}

		return session;
	}

	private static String performSSHcommand(Session session, String sshCommand) {
		String log = "";

		try {

			ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
			channelExec.setCommand(sshCommand);
			channelExec.connect();

			InputStream in = channelExec.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = reader.readLine()) != null) {
				// System.out.println(line);
				// line = line.replace("<br>", System.lineSeparator());
				log = log + System.lineSeparator() + line;
			}

			// Command execution completed here.

			// Retrieve the exit status of the executed command
			int exitStatus = channelExec.getExitStatus();
			if (exitStatus > 0) {
				System.out.println("Remote script exec error! " + exitStatus);
			}
			// Disconnect the Session
			session.disconnect();

			session.disconnect();
			// System.out.println("DONE");
			passAction("performSSHcommand", sshCommand, log);

			return log;
		} catch (JSchException | IOException | NullPointerException e) {
			e.printStackTrace();
			failAction("performSSHcommand", sshCommand, log);
			return log;
		}
	}

	/**
	 * Establish a connection to a remote SSH server using a key file, then perform
	 * a certain command and return its logs.
	 * 
	 * @param hostname
	 *            IP address of the SSH server
	 * @param sshPortNumber
	 *            Port number of the SSH service on the target server
	 * @param username
	 *            User name used to connect to the target server
	 * @param keyFileFolderName
	 *            Name of the folder that contains the key file, relative to the
	 *            project directory
	 * @param keyFileName
	 *            Name of the key file including its extension (if any)
	 * @param sshCommand
	 *            The target commang that should be executed on the SSH server
	 * @return
	 */
	public static String performSSHcommand(String hostname, int sshPortNumber, String username,
			String keyFileFolderName, String keyFileName, String sshCommand) {

		Session session = createSSHsession(hostname, sshPortNumber, username, keyFileFolderName, keyFileName);
		String log = performSSHcommand(session, sshCommand);

		return log;
	}
}
