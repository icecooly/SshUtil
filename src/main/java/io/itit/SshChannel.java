package io.itit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;

/**
 * 
 * @author skydu
 *
 */
public class SshChannel {
	//
	private static Logger logger = LoggerFactory.getLogger(SshChannel.class);
	//
	String id;
	Date createTime;
	long messageReceivedCount = 0;
	long messageSentCount = 0;
	private ChannelShell shell;
	private OutputStream shellOutputStream;
	private InputStream shellInputStream;
	public ConnectionInfo connectionInfo;

	//
	public SshChannel() {
		createTime = new Date();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void startShell() throws Exception {
		logger.info("startShell connection to {}", connectionInfo.host);
		shell = SshUtil.shell(connectionInfo.host, connectionInfo.port, connectionInfo.user, connectionInfo.password,
				connectionInfo.privateKey, 15000);
		shellInputStream = shell.getInputStream();
		shellOutputStream = shell.getOutputStream();
		startInputReader();
		if (!StringUtil.isEmpty(connectionInfo.cmd)) {
			sendMessageToServer(connectionInfo.cmd + "\n");
		}
		try {
			if (connectionInfo.channelListener != null) {
				connectionInfo.channelListener.onOpen(this);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sendError(e.getMessage());
		}

	}

	/**
	 * @return the connectionInfo
	 */
	public ConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	/**
	 * @param connectionInfo
	 *            the connectionInfo to set
	 */
	public void setConnectionInfo(ConnectionInfo connectionInfo) {
		this.connectionInfo = connectionInfo;
	}
	//
	private void startInputReader() {
		Thread inputReaderThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (shell != null && !shell.isClosed()) {
					try {
						int n = 0;
						byte[] buffer = new byte[4096];
						while (-1 != (n = shellInputStream.read(buffer))) {
							String s = new String(buffer, 0, n);
							receiveServerMessage(s);
						}
					} catch (Exception e) {
						sendError(e.getMessage());
						logger.error(e.getMessage(), e);
					}
				}
				logger.info("ssh connection :" + shell + " stopped");

			}
		}, "SshInputReader-" + connectionInfo.user + "@" + connectionInfo.host + ":" + connectionInfo.port);
		inputReaderThread.start();
	}

	//
	public void sendError(String error) {
		// show red alert info to client
		String rsp = "\033[31m\n\r**************************************************\n\r" + "\n\rerror:" + error
				+ "\n\r" + "\n\r**************************************************\n\r\033[0m";
		sendMessageToClient(rsp);
	}

	//
	private void receiveServerMessage(String s) {
		if (connectionInfo.channelListener != null) {
			connectionInfo.channelListener.onMessage(SshChannel.this, s);
		}
		sendMessageToClient(s);
	}

	//
	StringBuilder inputBuffer = new StringBuilder();

	//
	public void sendMessageToServer(String cmd) {
		try {
			if(logger.isInfoEnabled()) {
				logger.info("sendMessageToServer cmd:{}",cmd);
			}
			shellOutputStream.write((cmd).getBytes());
			shellOutputStream.flush();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}

	//
	public void sendMessageToClient(String msg) {
		messageSentCount++;
	}

	//
	public void closeChannel() {
		if (connectionInfo != null && connectionInfo.channelListener != null) {
			connectionInfo.channelListener.onClose(this);
		}
		if (shell != null && shell.isConnected()) {
			shell.disconnect();
		}
		try {
			if (shell != null && shell.getSession().isConnected()) {
				shell.getSession().disconnect();
			}
		} catch (JSchException e) {
			logger.error(e.getMessage(),e);
		}
		shell = null;
	}
}
