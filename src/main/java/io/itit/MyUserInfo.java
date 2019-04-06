package io.itit;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * 
 * @author skydu
 *
 */
public class MyUserInfo implements UserInfo, UIKeyboardInteractive {
	//
	public String password;

	//
	public MyUserInfo() {

	}

	//
	public MyUserInfo(String password) {
		this.password = password;
	}

	//
	public String getPassword() {
		return password;
	}

	public boolean promptYesNo(String str) {
		return true;
	}

	public String getPassphrase() {
		return null;
	}

	public boolean promptPassphrase(String message) {
		return false;
	}

	public boolean promptPassword(String message) {
		return true;
	}

	public void showMessage(String message) {
	}

	public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
			boolean[] echo) {
		return null;
	}
}
