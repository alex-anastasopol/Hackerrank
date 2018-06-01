/*
 * Created on Jun 11, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.Signature;

import org.apache.log4j.Category;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ManipulateKeys {
	
	protected static final Category logger= Category.getInstance(ManipulateKeys.class.getName());
	
	public static Sign signFile(String fileDescriptor){
		Sign sign = null;
		try{
			sign = new Sign();
			Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");			
			dsa.initSign(sign.getPrivateKey());			
			FileInputStream fis = new FileInputStream(fileDescriptor);
			BufferedInputStream bufin = new BufferedInputStream(fis);
			byte[] buffer = new byte[1024];
			int len;
			while (bufin.available() != 0) {
				len = bufin.read(buffer);
				dsa.update(buffer, 0, len);
			};
			bufin.close();			
			byte[] realSig = dsa.sign();
			sign.setSignature(realSig);									
		}catch(Exception e){
			
		}
		return sign;
	}
	
	public static boolean verifyFile(String fileDescriptor, Sign sign){
		try{	
			/* create a Signature object and initialize it with the public key */
			Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
			sig.initVerify(sign.getPublicKey());
			
			/* Update and verify the data */
			FileInputStream datafis = new FileInputStream(fileDescriptor);
			BufferedInputStream bufin = new BufferedInputStream(datafis);
			byte[] buffer = new byte[1024];
			int len;
			while (bufin.available() != 0) {
				len = bufin.read(buffer);
				sig.update(buffer, 0, len);
			};
			bufin.close();			
			return (sig.verify(sign.getSignature()));
			
		}catch(Exception e){
					logger.error("Caught exception " + e.toString());
		}
		return false;
	}	
	
}
