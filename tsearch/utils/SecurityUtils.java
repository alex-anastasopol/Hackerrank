package ro.cst.tsearch.utils;

import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.loadBalServ.LBNotification;



public class SecurityUtils {
	
    private Cipher ecipher                        = null;
    private Cipher dcipher                        = null; 
    private MessageDigest mdSHA512				  = null;
	
	private SecurityUtils() {
		String encryptionString = DBManager.getConfigByName("lbs.2.ats.encrypt.key");
		if(encryptionString==null){
			String[] entry = new String[2];
			entry[0] = "ERROR - no entry for lbs.2.ats.encrypt.key";
			entry[1] = "NO entry for lbs.2.ats.encrypt.key on " + URLMaping.INSTANCE_DIR;
			LBNotification.sendNotification(LBNotification.MISC_MESSAGE, null, entry);
			encryptionString = "@nDre123"; 
		}
		
		byte[] iv = encryptionString.getBytes();
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        byte[] key = encryptionString.getBytes();
        SecretKeySpec secretKey  = new SecretKeySpec(key, "DES");
        
        try {
            ecipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

            ecipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
            dcipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
            
            mdSHA512 = MessageDigest.getInstance("SHA-512");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private static class SingletonHolder {
		private static SecurityUtils instance = new SecurityUtils();
	} 
	
	public static SecurityUtils getInstance() {
		SecurityUtils crtInstance = SingletonHolder.instance;
		return crtInstance;
	}

	public String encrypt(String value) {
		String result = null;
		try {
			byte[] utf8 = value.getBytes("UTF8");
			byte[]  encrypted = ecipher.doFinal(utf8);
//			result = new sun.misc.BASE64Encoder().encode(encrypted);
			
			result = new String(DatatypeConverter.printBase64Binary(encrypted));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public String decrypt(String value) {
		String result = null;
		try {
			
			if(StringUtils.isEmpty(value)) {
				return "";
			}
			
//			byte[] utf8 = new sun.misc.BASE64Decoder().decodeBuffer(value);
			
			byte[] utf8 = DatatypeConverter.parseBase64Binary(value);
			
			byte[] decrypted = dcipher.doFinal(utf8);
			result = new String(decrypted, "UTF8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public String encryptSHA512(String value){
		String result = null;
		try {
			
//			System.err.println("encryptSHA512 value " + value + " with size " + value.length());
			
			byte[] utf8 = value.getBytes("UTF8");
			
//			for (int i = 0; i < utf8.length; i++) {
//				System.err.println( "utf: " + i + " - " + utf8[i]);
//			}
			
			
//			System.err.println("encryptSHA512 utf8 " + utf8 + " with size " + utf8.length);
			
//			byte[] digested = mdSHA512.digest(utf8);
//			for (int i = 0; i < digested.length; i++) {
//				System.err.println("[09/03/2012 - 07:20:32.668]digested: " + i + " - " + digested[i]);
//			}
			
//			result = new sun.misc.BASE64Encoder().encode(mdSHA512.digest(utf8));
//			System.err.println("encryptSHA512 result " + result + " with size " + result.length());
			
//			result = new String(Base64.encodeBase64(mdSHA512.digest(utf8)));
//			System.err.println("encryptSHA512 result " + result + " with size " + result.length());
			
			result = new String(DatatypeConverter.printBase64Binary(mdSHA512.digest(utf8)));
//			System.err.println("encryptSHA512 result " + result + " with size " + result.length());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static void main(String[] args) {
		System.out.println(SecurityUtils.getInstance().encryptSHA512("1aA12aSa33Dl4lF4ee4cc5uu0_1c4a96461"));
	}

}
