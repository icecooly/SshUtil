package io.itit;

/**
 * 
 * @author skydu
 *
 */
public class ExpectHolder {
	//
	public static interface ExpectCallback {
		void invoke(String input);
	}
	//
	public String regex;
	public boolean once;
	public ExpectCallback callback;
}