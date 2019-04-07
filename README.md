# SshUtil

- 支持免输入密码SSH登录服务器
- 支持SSH证书登录服务器
- 支持远程运行指定命令
- 支持端口转发，X11转发，文件传输


Download
--------

Download Jar or grab via Maven:
```xml
<dependency>
  <groupId>com.github.icecooly</groupId>
  <artifactId>SshUtil</artifactId>
  <version>1.0</version>
</dependency>
```

简单的例子
==============
1.免密SSH登录服务器,并运行ls -lrt命令
```java
SshChannel channel=startShell();
channel.sendMessageToServer("ls -lrt\n");
```

2.免密传输本地文件a.txt到远程服务器b.txt,并且打印上传进度
```java
SshUtil.scpTo(host, port, user, password,null,"/tmp/a.txt","/tmp/b.txt",(fileSize,sendSize)->{
	logger.info("progress:{}/{} {}%",sendSize,fileSize,sendSize*100/fileSize);
});
```
3.免密传输远程服务器文件b.txt到本地c.txt
```java
SshUtil.scpFrom(host, port, user, password,"/tmp/b.txt","/tmp/c.txt");
```

4.开启正向代理，可以通过连接127.0.0.1:3307访问远程MYSQL
```java
String remoteBHost="x.x.x.x";
SshUtil.setPortForwardingL(user, password, host, port, "127.0.0.1", 3307, remoteBHost, 3306);
```
5.开启反向代理，把本地22端口代理到主机host的8022端口（这样在主机host上，就可以通过ssh localhost -p 8022访问到本机）
```java
SshUtil.setPortForwardingR(user, password, host, port, 22, "localhost", 8022);
```
6.新增用户test，并且设置test密码
```java
SshChannel channel=startShell();
channel.send("useradd test\n");
channel.send("passwd test\n").expect(
		new String[] {"New password:","Retype new password:"},(input)->{
	channel.send("123ABCabc#^!\n");
});
```
