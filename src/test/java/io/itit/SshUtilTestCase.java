package io.itit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * 
 * @author skydu
 *
 */
public class SshUtilTestCase extends TestCase{
	//
	private static Logger logger=LoggerFactory.getLogger(SshUtilTestCase.class);
	//
	private String host="xxxxxx";
	private int port=22;
	private String user="xxxx";
	private String password="xxxxx";
	//
	private SshChannel startShell() throws Exception {
		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.host = host;
		connectionInfo.port = port;
		connectionInfo.user = user;
		connectionInfo.password = password;
		connectionInfo.cmd="pwd\n";
		connectionInfo.channelListener=new ChannelListener() {
			@Override
			public void onTicket(SshChannel channel, long ticket) {
				logger.info("onTicket ticket:{}",ticket);
			}
			
			@Override
			public void onOpen(SshChannel channel) {
				logger.info("onOpen channel:{}",channel);
			}
			
			@Override
			public void onMessage(SshChannel channel, String message) {
				logger.info("onMessage channel:{} message:\n{}",channel,message);
			}
			
			@Override
			public void onInput(SshChannel channel, String message) {
				logger.info("onInput channel:{} message:{}",channel,message);
			}
			
			@Override
			public void onClose(SshChannel channel) {
				logger.info("onClose channel:{}",channel);
			}
			
			@Override
			public boolean inputSendToServer() {
				return true;
			}
		};
		SshChannel sshChannel = new SshChannel();
		sshChannel.connectionInfo = connectionInfo;
		sshChannel.startShell();
		return sshChannel;
	}
	
	//
	public void testLogin() throws Exception {
		startShell();
		Thread.sleep(2000);
	}
	//
	public void testLs() throws Exception {
		SshChannel channel=startShell();
		channel.sendMessageToServer("ls -lrt\n");
		Thread.sleep(2000);
	}
	//
	public void testScpTo() throws InterruptedException {
		SshUtil.scpTo(host, port, user, password,null,"/tmp/a.txt","/tmp/b.txt",(fileSize,sendSize)->{
			logger.info("progress:{}/{} {}%",sendSize,fileSize,sendSize*100/fileSize);
		});
		Thread.sleep(100000);
	}
	//
	public void testMd5() throws Exception {
		SshChannel channel=startShell();
		channel.sendMessageToServer("md5sum /tmp/b.txt\n");
		Thread.sleep(2000);
	}
	//
	public void testScpFrom() throws InterruptedException {
		SshUtil.scpFrom(host, port, user, password,"/tmp/b.txt","/tmp/c.txt");
		Thread.sleep(100000);
	}
	
	/**
	 * 正向代理(开启正向代理，可以通过连接127.0.0.1:3307访问远程MYSQL)
	 */
	public void testPortForwardingL() throws Exception {
		String remoteBHost="x.x.x.x";
		SshUtil.setPortForwardingL(user, password, host, port, "127.0.0.1", 3307, remoteBHost, 3306);
		Thread.sleep(100000);
	}
	
	/**
	 * 反向代理，把本地22端口代理到主机host的8022端口（这样在主机host上，就可以通过ssh localhost -p 8022访问到本机）
	 * @throws Exception
	 */
	public void testPortForwardingR() throws Exception {
		SshUtil.setPortForwardingR(user, password, host, port, 22, "localhost", 8022);
		Thread.sleep(100000);
	}
}
