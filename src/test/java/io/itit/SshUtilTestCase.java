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
	private String host="xxxx";
	private int port=22;
	private String user="root";
	private String password=null;
	private String privateKey=null;//证书私钥
	private String cmd="pwd\n";//登录后执行的命令
	//
	private SshChannel startShell() throws Exception {
		ConnectionInfo connectionInfo = new ConnectionInfo();
		connectionInfo.host = host;
		connectionInfo.port = port;
		connectionInfo.user = user;
		connectionInfo.password = password;
		connectionInfo.privateKey=privateKey;
		connectionInfo.cmd=cmd;
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
			public void onError(SshChannel channel, String errorMessage) {
				logger.info("onError channel:{} errorMessage:\n{}",channel,errorMessage);
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
		sshChannel.setConnectionInfo(connectionInfo);
		sshChannel.startShell();
		return sshChannel;
	}
	
	//
	public void testLoginWithPassword() throws Exception {
		password="xxxxxx";
		startShell();
		Thread.sleep(2000);
	}
	//
	public void testLoginWithPrivateKey() throws Exception {
		privateKey="-----BEGIN RSA PRIVATE KEY-----\n" + 
				"MIICXAIBAAKBgQCCGV2G5ccTSVqAg5crpSpHRFLTW7GKwz6y+/9dBr9hipCi8t65\n" + 
				"ZNpVqx6nu/Gjnvk0KZNTyujaSinwLQtDh2B69+Y816m5iUFU42BU7hgyl+o9BQw9\n" + 
				"XXXXXXXXXXXXXXXXXX0IY6YnWX5dg6oEoTbNjGaqnMx05TlH9qVj5qfeOwIDAQAB\n" + 
				"AoGAE5hcdOgA/w+qWPb4+vLqlkddLkZ+TEcyF2VLRiixKLEJLfHkyAm/tO2MNXli\n" + 
				"YOGd6VRlw1Ypkk9fV7SBIM+wIT5Luq778GuPhb+gzPuKya+QfjK2WiDjE88sEci4\n" + 
				"8PkUGBUnXRpxP4e0PZ1+LPOKKGBSYlXeuo59BiU/l9P+3JECQQDA/Fz1z5w+ANVR\n" + 
				"AxXxxksUdwP8r4JVZnWvGtJWycJ0aKJwoCqS0IF3se1mvHT8/guPDds+cHhHcjgd\n" + 
				"cy0kBSlTAkEArJRRJVfwO5AipPHwXnz5ycer/scyHsLCXptKwfCqF3IunP5vB0MX\n" + 
				"ryDfYRA7BjtnwDmyjHgkAInpsQ5ldAKSeQJBAJbiuSvXbqlrrVzxtK6cAwe1JgDi\n" + 
				"mFx9B3Yo2lvQ06CATsEP+TlgnFkhXCP/JNjJJ/BpPQnMlb4Gp6ke7CRFhNECQGzM\n" + 
				"RCvquISUZYLfE849s6vFuWSxZ6OE3MyP0h1Z/6EwVrqanJxTa8b4Tlr+xHc1VD8X\n" + 
				"ILz1sJ05dd4tWUA9ruECQD9lVKt9lfyxZC8dF0lJYkX7avuuFpEkby5SsTXJaoRv\n" + 
				"b0otvcMp8UpCCNDjkrI2HqyPbgcDDzj3PSYSgqCDjVo=\n" + 
				"-----END RSA PRIVATE KEY-----\n" + 
				"";
		startShell();
		Thread.sleep(2000);
	}
	//
	public void testLs() throws Exception {
		SshChannel channel=startShell();
		channel.send("ls -lrt\n");
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
		channel.send("md5sum /tmp/b.txt\n");
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
	
	//高级篇
	/**
	 * 新增用户test
	 * @throws Exception
	 */
	public void testAddUser() throws Exception {
		SshChannel channel=startShell();
		channel.send("useradd test\n");
		Thread.sleep(100000);
	}
	/**
	 * 修改用户test的密码
	 * @throws Exception
	 */
	public void testModifyUserPassword() throws Exception {
		SshChannel channel=startShell();
		channel.send("passwd test\n").expect(
				new String[] {"New password:","Retype new password:"},(input)->{
			channel.send("123ABCabc#^!\n");
		});
		Thread.sleep(100000);
	}
}
