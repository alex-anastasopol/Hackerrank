/*
 * Created on Jun 9, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Signature;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GenDigitalSign extends Sign{
	public void signFile(String fileDescriptor){
		try{
			
			Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
			Sign sign = new Sign();
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
			/* save the signature in a file */
			FileOutputStream sigfos = new FileOutputStream("signature");
			sigfos.write(realSig);
			sigfos.close();
			/* save the public key in a file */
			byte[] key = sign.getPublicKey().getEncoded();
			FileOutputStream keyfos = new FileOutputStream("publickey");
			keyfos.write(key);
			keyfos.close();
 
		}catch(Exception e){
			
		}
	}
}
