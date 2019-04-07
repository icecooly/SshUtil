package io.itit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * 
 * @author skydu
 *
 */
public class SshUtil {
	//
	public static int DEFAULT_TIMEOUNT=15000;
	//
	private static Logger logger=LoggerFactory.getLogger(SshUtil.class);
	//
	private static File createPrivateKeyFile(String privateKey) {
		try {
			File tmpFile=File.createTempFile(UUID.randomUUID().toString(), "key");
			FileUtil.saveContent(privateKey.getBytes(), tmpFile);
			return  tmpFile;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}	
	}
	
	/**
	 * 密码登录
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public static ChannelShell shell(
			String host, 
			int port, 
			String user, 
			String password) throws Exception {
			return shell(host, port, user, password, null, DEFAULT_TIMEOUNT);
	}
	
	/**
	 * 证书登录
	 * @param privateKey
	 * @param host
	 * @param port
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static ChannelShell shell(
			String privateKey,
			String host, 
			int port, 
			String user
			) throws Exception {
			return shell(host, port, user, null, privateKey, DEFAULT_TIMEOUNT);
	}
	/**
	 * 
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @param privateKey
	 * @param connectTimeout
	 * @return
	 * @throws Exception
	 */
	public static ChannelShell shell(
			String host, 
			int port, 
			String user, 
			String password,
			String privateKey,
			int connectTimeout) throws Exception {
			Session session=getSession(host, port, user, password, privateKey);
			ChannelShell channel = (ChannelShell) session.openChannel("shell");
			channel.connect(connectTimeout);
			return channel;
	}
	//
	private static Session getSession(String host, 
			int port, 
			String user, 
			String pwd,
			String privateKey) {
		return getSession(host, port, user, pwd, privateKey, DEFAULT_TIMEOUNT);
	}
	//
	private static Session getSession(String host, 
			int port, 
			String user, 
			String pwd,
			String privateKey,
			int connectTimeout) {
		File privateKeyFile=null;
		try {
			JSch jsch = new JSch();
			if(privateKey!=null) {
				privateKeyFile=createPrivateKeyFile(privateKey);
				jsch.addIdentity(privateKeyFile.getAbsolutePath());
			}
			Session session = jsch.getSession(user, host, port);
			if(logger.isDebugEnabled()){
				logger.debug("getSession {}@{}:{}",user,host,port);
			}
			if(pwd!=null) {
				session.setPassword(pwd);
			}
			Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			UserInfo ui = new MyUserInfo();
		    session.setUserInfo(ui);
			session.connect(connectTimeout);
			return session;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException("连接失败");
		}finally {
			if(privateKeyFile!=null) {
				privateKeyFile.delete();
			}
		}
	}
	//
	public static int execute(
			String host, 
			int port, 
			String user, 
			String pwd,
			String privateKey,
			String cmd,
			boolean pty,
			BiConsumer<String,String>callback){
		try {
			Session session = getSession(host, port, user, pwd, privateKey, DEFAULT_TIMEOUNT);
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			if(pty){
				channel.setPty(true);
			}
			channel.setCommand(cmd);
			channel.setInputStream(null);
			InputStream in = channel.getInputStream();
			InputStream err = channel.getErrStream();
			channel.connect(DEFAULT_TIMEOUNT);
			int exitCode=0;
			String outResult="";
			String errResult="";
			long totalExecuteTime=0;
			while (true) {
				outResult=getResult(in);
				errResult=getResult(err);
				if(!outResult.isEmpty()||!errResult.isEmpty()){
					callback.accept(outResult,errResult);
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					exitCode=channel.getExitStatus();
					break;
				}
				try {
					Thread.sleep(100);
				} catch (Exception ee) {}
				totalExecuteTime+=100;
				//
				if(totalExecuteTime>=1000*30*60){
					break;
				}
			}
			channel.disconnect();
			session.disconnect();
			return exitCode;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e.getMessage());
		}
		
	}
	//
	private static String getResult(InputStream in)throws Exception{
		byte[] tmp = new byte[1024];
		StringBuilder resultOut=new StringBuilder();
		int available=0;
		while ((available=in.available()) > 0) {
			if(available>1024) {
				available=1024;
			}
			int i = in.read(tmp, 0, available);
			if (i < 0)
				break;
			resultOut.append(new String(tmp, 0, i));
		}
		return resultOut.toString();
	}
	
	/**
	 * 
	 * @param remoteHost
	 * @param remotePort
	 * @param remoteUser
	 * @param remotePwd
	 * @param remoteFile
	 * @param localFile
	 */
	public static void scpFrom(
			String remoteHost,
			int remotePort,
			String remoteUser,
			String remotePwd,
			String remoteFile,
			String localFile) {
		scpFrom(remoteHost, remotePort, remoteUser, remotePwd, null, remoteFile, localFile);
	}
	
	/**
	 * 传输文件
	 * @param remoteHost
	 * @param remotePort
	 * @param remoteUser
	 * @param remotePwd
	 * @param privateKey
	 * @param remoteFile
	 * @param localFile
	 */
	public static void scpFrom(
			String remoteHost,
			int remotePort,
			String remoteUser,
			String remotePwd,
			String privateKey,
			String remoteFile,
			String localFile) {
		FileOutputStream fos = null;
		if(logger.isInfoEnabled()) {
			logger.info("scpFrom remoteHost:{} remotePort:{} remoteUser:{} "
				+ "remoteFile:{} remoteFile:{} localFile:{}",
				remoteHost,remotePort,remoteUser,remoteFile,localFile);
		}
		try {
			String prefix = null;
			if (new File(localFile).isDirectory()) {
				prefix = localFile + File.separator;
			}
			Session session = getSession(remoteHost, remotePort, remoteUser, remotePwd, privateKey, DEFAULT_TIMEOUNT);
			String command = "scp -f " + remoteFile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();
			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				// System.out.println("filesize="+filesize+", file="+file);

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				fos = new FileOutputStream(prefix == null ? localFile : prefix + file);
				int foo;
				long receiveLen=0;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					receiveLen+=foo;
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				if (checkAck(in) != 0) {
					System.exit(0);
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
				//
				if(logger.isInfoEnabled()) {
					logger.info("scp finished receiveLen:{}",receiveLen);
				}
			}
			session.disconnect();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * @param host
	 * @param port
	 * @param user
	 * @param pwd
	 * @param localFile
	 * @param remoteFile
	 * @throws RuntimeException
	 */
	public static void scpTo(
			String host, 
			int port, 
			String user, 
			String pwd,
			String localFile,
			String remoteFile) throws RuntimeException {
		scpTo(host, port, user, pwd, null, localFile, remoteFile,null);
	}
	
	/**
	 * 
	 * @param host
	 * @param port
	 * @param user
	 * @param pwd
	 * @param privateKey
	 * @param localFile
	 * @param remoteFile
	 * @param progress 上传进度 文件总大小,已上传大小
	 * @throws RuntimeException
	 */
	public static void scpTo(
			String host, 
			int port, 
			String user, 
			String pwd,
			String privateKey,
			String localFile,
			String remoteFile,
			BiConsumer<Long,Long> progress) throws RuntimeException {
		try {
			Session session = getSession(host, port, user, pwd, privateKey);
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand("scp -t "+remoteFile);
			channel.setInputStream(null);
			InputStream in = channel.getInputStream();
			OutputStream out = channel.getOutputStream();
			channel.connect(DEFAULT_TIMEOUNT);
			checkAck(in);
			File lFile = new File(localFile);
			long filesize=lFile.length();
			String  command="C0644 "+filesize+" ";
			if (localFile.lastIndexOf('/') > 0) {
				command += localFile.substring(localFile.lastIndexOf('/') + 1);
			} else {
				command += localFile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			checkAck(in);
			//
			FileInputStream fis = new FileInputStream(lFile);
			byte[] buf = new byte[1024*500];
			long sendLen=0;
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0) {
					break;
				}
				sendLen+=len;
				if(progress!=null) {
					progress.accept(filesize,sendLen);
				}
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			checkAck(in);
			out.close();
			channel.disconnect();
			session.disconnect();
			if(logger.isInfoEnabled()) {
				logger.info("scp finished sendLen:{}",sendLen);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
	} 
	//
	private static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				logger.error(sb.toString());
				throw new RuntimeException(sb.toString());
			}
			if (b == 2) { // fatal error
				logger.error(sb.toString());
				throw new RuntimeException(sb.toString());
			}
		}
		return b;
	}
	//
	/**
	 * 正向代理	ssh -L lHost:lPort:rHost:rPort userName@host
	 * @param userName
	 * @param password
	 * @param host
	 * @param port
	 * @param lHost
	 * @param lPort
	 * @param rHost
	 * @param rPort
	 */
	public static void setPortForwardingL(String userName, String password, String host, int port, String lHost,
			int lPort, String rHost, int rPort) {
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(userName, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			int assingedPort = session.setPortForwardingL(lHost, lPort, rHost, rPort);// ssh -L lHost:lPort:rHost:rPort userName@host 正向代理
			logger.info("assingedPort:{}",assingedPort);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}
	}
	
	/**
	 * 反向代理
	 * @param userName
	 * @param password
	 * @param host
	 * @param port
	 * @param lPort
	 * @param rHost
	 * @param rPort
	 */
	public static void setPortForwardingR(String userName, String password, String host, int port,
			int lPort, String rHost, int rPort) {
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(userName, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			session.setPortForwardingR(rPort, rHost, lPort);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}
	}
}
