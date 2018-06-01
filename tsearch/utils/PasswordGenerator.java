package ro.cst.tsearch.utils;

import java.util.Random;

public class PasswordGenerator {
	private static final String charset = "0123456789ABCDEFGHIJ0123456789KLMNOPQRSTUVWXYZ";
	
	public static String getRandomPassword(int length) {
        Random rand = new Random(System.currentTimeMillis());
        StringBuffer sb = new StringBuffer();
        boolean hasDigit = false;
        boolean hasCapitalCaseLetter = false;
        for (int i = 0; i < length; i++) {
            int pos = rand.nextInt(charset.length());
            char c = charset.charAt(pos);
            hasDigit = hasDigit || Character.isDigit(c);
            hasCapitalCaseLetter = hasCapitalCaseLetter || !Character.isDigit(c);
            sb.append(c);
        }
        if(!hasDigit || !hasCapitalCaseLetter) {
        	return getRandomPassword(length);
        }
        return sb.toString();
    }
	
	public static boolean validatePassword(String password, int length) {
		if(password == null) {
			return false;
		}
		if(password.trim().length() < length) {
			return false;
		}
		boolean hasDigit = false;
        boolean hasCapitalCaseLetter = false;
        for (int i = 0; i < password.trim().length(); i++) {
            char c = password.charAt(i);
            hasDigit = hasDigit || Character.isDigit(c);
            hasCapitalCaseLetter = hasCapitalCaseLetter || Character.isUpperCase(c);
            
        }
        if(!hasDigit || !hasCapitalCaseLetter) {
        	return false;
        }
		
		return true;
	}



}
