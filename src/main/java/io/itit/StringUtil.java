package io.itit;

/**
 * 
 * @author skydu
 *
 */
public class StringUtil {

	/**
	 * 
	 * @param input
	 * @return
	 */
	public static boolean isEmpty(String input){
		if(input==null||input.length()==0){
			return true;
		}
		return false;
	}
}
