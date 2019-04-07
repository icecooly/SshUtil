/**
 * 
 */
package io.itit;

/**
 * 
 * @author skydu
 *
 */
public interface ChannelListener {
	/**
	 * 
	 * @param channel
	 */
	void onOpen(SshChannel channel);

	/**
	 * 
	 * @param channel
	 */
	void onClose(SshChannel channel);

	/**
	 * 
	 * @param channel
	 * @param message
	 */
	void onMessage(SshChannel channel, String message);
	
	/**
	 * 
	 * @param channel
	 * @param message
	 */
	void onError(SshChannel channel, String errMessage);

	/**
	 * 
	 * @param channel
	 * @param message
	 */
	void onInput(SshChannel channel, String message);

	/**
	 * 
	 * @return
	 */
	boolean inputSendToServer();

	/**
	 * 
	 * @param channel
	 * @param ticket
	 */
	void onTicket(SshChannel channel, long ticket);
}
