package io.itit;

/**
 * 
 * @author skydu
 *
 */
public class ConnectionInfo {
	
	/**
	 * 
	 * @author skydu
	 *
	 */
	public static interface Callback {
		void onReceiveMessage(String message);
	}

	//
	public String name;
	public String host;
	public int port;
	public String user;
	public String password;
	public String privateKey;
	public boolean enableInput;
	public String cmd;//first cmd
	public Callback callback;
	public ChannelListener channelListener;

	//
	@Override
	public String toString() {
		return user + "@" + host + ":" + port + "/" + "input:" + enableInput + "/" + channelListener;
	}
}