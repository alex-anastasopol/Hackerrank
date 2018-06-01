package ro.cst.tsearch.bean;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

@Deprecated
public class HTMLIndex implements Serializable {
	
	private static final long serialVersionUID = 10000000L;
	public static final int PROCESS_TIME_OUT = 30 * 60 * 1000; // 30 minutes
    public static final int UPLOAD_TIME_OUT = 60 * 1000; // 1 minute
    
    private String htmlIndexKey = null;
    long searchId=-1;
	private boolean readyToUpload = false;
	private boolean readyToProcess = true;
	private long uploadTotalSize = 0;
	private boolean hasImage = false;
	private boolean updated = false;
	private String references = "";
	private String instrumentDate = "";
	private String recordedDate = "";
	private String recordedTime = "";
	private Vector receiptDate = new Vector();
	private String grantor = "";
	private String granteeTR = "";
	private String granteeLander = "";
	private String mortgageAmount = "";
	private String considerationAmount = "";
	private String book = "";
	private String page = "";
	private String instrumentNumber = "";
	private String documentNumber = "";
	private String instrumentType = "";	
	private String srcType = "";
	private Hashtable<String, String> params = new Hashtable<String, String>();
	private String grantee = "";
	private String amountPaid = "";
	private String baseAmount = "";
	private String delinquentAmount = "";
	private Vector  receiptNumber= new Vector();
	private Vector  receiptAmount= new Vector();
	private String taxYear = "";
	private String serverInstrumentType = "";
	
	
}
