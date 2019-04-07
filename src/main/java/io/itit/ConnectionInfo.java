package io.itit;

/**
 * 
 * @author skydu
 *
 */
public class ConnectionInfo {
	//
	public String name;
	public String host;
	public int port;
	public String user;
	public String password;
	public String privateKey;
	public String cmd;//first cmd
	public ChannelListener channelListener;
	public int connectTimeout=15000;//default 15s

	//
	@Override
	public String toString() {
		return user + "@" + host + ":" + port +  "/" + channelListener;
	}
}