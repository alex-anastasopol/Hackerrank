package ro.cst.tsearch.utils;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * @author nae
 */
public class Sign implements Serializable {
    
    static final long serialVersionUID = 10000000;
    
	private PrivateKey privateKey = null; 
	private PublicKey publicKey = null;
	
	private byte[] signature = null;
	private byte[] encriptedPublicKey = null;
	public void init(){
		generateKey();
	}
	public void generateKey() {		
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			keyGen.initialize(1024, random);
			KeyPair pair = keyGen.generateKeyPair();
			this.privateKey  = pair.getPrivate();
			this.publicKey   = pair.getPublic();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public Sign() {
		init();
	}


	/**
	 * @return
	 */
	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	/**
	 * @return
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}

	/**
	 * @param key
	 */
	public void setPrivateKey(PrivateKey key) {
		privateKey = key;
	}

	/**
	 * @param key
	 */
	public void setPublicKey(PublicKey key) {
		publicKey = key;
	}
	
	/**
	 * @return
	 */
	public byte[] getSignature() {
		return signature;
	}

	/**
	 * @param bs
	 */
	public void setSignature(byte[] bs) {
		signature = bs;
	}

	/**
	 * @return
	 */
	public byte[] getEncriptedPublicKey() {
		return encriptedPublicKey;
	}

	/**
	 * @param bs
	 */
	public void setEncriptedPublicKey(byte[] bs) {
		encriptedPublicKey = bs;
	}

}
