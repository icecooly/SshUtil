package io.itit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;

import io.itit.ExpectHolder.ExpectCallback;

/**
 * 
 * @author skydu
 *
 */
public class SshChannel {
	//
	private static Logger logger = LoggerFactory.getLogger(SshChannel.class);
	//
	Date createTime;
	protected ChannelShell shell;
	protected OutputStream shellOutputStream;
	protected InputStream shellInputStream;
	protected ConnectionInfo connectionInfo;
	protected Map<String,ExpectHolder> expectCallbacks;
	protected StringBuilder messageBuffer;
	//
	public SshChannel() {
		createTime = new Date();
		expectCallbacks=new ConcurrentHashMap<>();
		messageBuffer=new StringBuilder();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void startShell() throws Exception {
		logger.info("startShell connection to {}", connectionInfo.host);
		shell = SshUtil.shell(connectionInfo.host, connectionInfo.port, connectionInfo.user, connectionInfo.password,
				connectionInfo.privateKey, connectionInfo.connectTimeout);
		shellInputStream = shell.getInputStream();
		shellOutputStream = shell.getOutputStream();
		startInputReader();
		if (!StringUtil.isEmpty(connectionInfo.cmd)) {
			send(connectionInfo.cmd + "\n");
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
	protected void startInputReader() {
		Thread inputReaderThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (shell != null && !shell.isClosed()) {
					try {
						int n = 0;
						byte[] buffer = new byte[4096];
						while (-1 != (n = shellInputStream.read(buffer))) {
							String s = new String(buffer, 0, n);
							receiveMessage(s);
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
	protected void sendError(String error) {
		if (connectionInfo.channelListener != null) {
			connectionInfo.channelListener.onError(SshChannel.this, error);
		}
	}

	//
	protected void receiveMessage(String message) {
		if (connectionInfo.channelListener != null) {
			connectionInfo.channelListener.onMessage(SshChannel.this, message);
		}
		//
		onMessage(this, message);
	}
	//
	protected void onMessage(SshChannel channel, String message) {
		synchronized (messageBuffer) {
			for(char c:message.toCharArray()){
				messageBuffer.append(c);
				if(c=='\n'||c=='\r'){
					matchMessage(messageBuffer.toString().trim());
					messageBuffer.delete(0, messageBuffer.length());
				}
			}
			if(messageBuffer.length()>0) {
				matchMessage(messageBuffer.toString().trim());
				messageBuffer.delete(0, messageBuffer.length());
			}
		}
	}
	//
	public SshChannel send(String cmd) {
		try {
			if(logger.isInfoEnabled()) {
				logger.info("send cmd:{}",cmd);
			}
			shellOutputStream.write((cmd).getBytes());
			shellOutputStream.flush();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return this;
	}
	//
	public SshChannel expect(String regex,ExpectCallback callback) {
		expect0(regex, false, callback);
		return this;
	}
	//
	public SshChannel expect(String[] regexs,ExpectCallback callback) {
		if(regexs==null||regexs.length==0) {
			return this;
		}
		for (String regex : regexs) {
			expect0(regex, false, callback);
		}
		return this;
	}
	//
	public SshChannel expectOne(String regex,ExpectCallback callback) {
		expect0(regex, true, callback);
		return this;
	}
	//
	public SshChannel expectOne(String[] regexs,ExpectCallback callback) {
		if(regexs==null||regexs.length==0) {
			return this;
		}
		for (String regex : regexs) {
			expect0(regex, true, callback);
		}
		return this;
	}
	//
	protected void expect0(String regex,boolean once,ExpectCallback callback){
		synchronized (expectCallbacks) {
			ExpectHolder holder=new ExpectHolder();
			holder.callback=callback;
			holder.once=once;
			holder.regex=regex;
			expectCallbacks.put(regex, holder);
		}
	}
	//
	public SshChannel expectClear() {
		synchronized (expectCallbacks) {
			expectCallbacks.clear();
		}
		return this;
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
	//
	protected void matchMessage(String line){
		logger.info("matchMessage:{}",line);
		if(line.trim().isEmpty()){
			return;
		}
		List<String>removedCallbacks=new LinkedList<>();
		expectCallbacks.forEach((regex,holder)->{
			if(Pattern.matches(regex, line)){
				holder.callback.invoke(line);
				if(holder.once){
					removedCallbacks.add(holder.regex);
				}
			}
		});
		for(String s:removedCallbacks){
			expectCallbacks.remove(s);
		}
	}
}
