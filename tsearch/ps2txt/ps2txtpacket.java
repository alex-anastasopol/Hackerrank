package ro.cst.tsearch.ps2txt;
import java.io.Serializable;
/**
 * @author cozmin
 */
public class ps2txtpacket implements Serializable {
    
    static final long serialVersionUID = 10000000;
    
	public static final String PS_START="PS_START";
	public static final String PS_DATA="PS_DATA";
	public static final String PS_STOP="PS_STOP";
	
	public static final String TXT_START="TXT_START";
    public static final String TXT_DATA="TXT_DATA";
	public static final String TXT_STOP="TXT_STOP";
	
	///message used to stop server 
	public static final String SRV_STOP="SRV_STOP";
	
	private String type;	
	private byte content[];
	/**
	 * @return
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param bs
	 */
	public void setContent(byte[] bs,int sz) {
		content =new byte[sz];
		for(int i=0;i<sz;i++)
		   content[i]=bs[i]; 
	}

	/**
	 * @param string
	 */
	public void setType(String string) {
		type = string;
	}

}
