package ro.cst.tsearch.reports.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchFlags;
import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBSearch;
import ro.cst.tsearch.database.rowmapper.SearchUserTimeMapper;
import ro.cst.tsearch.user.MyAtsAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameI;

public class DayReportLineData implements Comparable<DayReportLineData>{

	private static final Logger logger = Logger.getLogger(DayReportLineData.class);
	
	private AbstractorWorkedTime abstractorWorkedTime;
	private Map<Long, AbstractorWorkedTime> otherAbstractorWorkedTime;
	
	private String ownerFirstName;
	private String ownerLastName;
	private String agentFirstName;
	private String agentLastName;
	private long agentId;
	
	private String propertyCounty;
	private String propertyState;
	private String propertyStreet;
	private String propertyNo;
	private String propertySuffix;
	
	private Date searchDueDate;
	private Date searchTimeStamp;
	private String fileId;
	private String sendTo;
	private String fileLink;
	private int invoice;
	private long searchId;
	private int paid;
	
	private int invoiced;
	private String wPhone = null;
	private String hPhone = null;
	private String mPhone = null;
	private int confirmed = 0;
	private int archived = 0;
	private String tsrFolder;
	private long id;
	private Date tsrTimeStamp; 
	private Date tsrOpenDate; 
	private Date tsrInitialDate; 
	
	private String note;
    private int productType = 0;
    private String productName = null;
	private int noteStatus;
    private Boolean wasOpened = null;
    private double fee = -1;
    private int duplicate = 0;
    private SearchFlags searchFlags = new SearchFlags();	
    private Vector<NameI> owners = new Vector<NameI>();
    private Map<NameI,String> nameColors = new HashMap<NameI,String>();
    
    private boolean addressBootstrapped = false;
    private int addressBootstrappedCode = 0;
    private final long STREET_NAME_MASK = 2;
    private final long STREET_NO_MASK = 4;
    private final long STREET_SUFFIX_MASK = 16;
    
    private String orderFileLink = null;
    //this has nothing to do with the status field in TS_SEARCH table
    //this status consists of a string containing letters like the describe the status
    private String status = null;			
    private String logFileLink = null;
    private String tsrIndexFileLink = null;
    private float discountRatio = 1f;
    
    private boolean isInvoiceData;
    private int gmtOffset = (TimeZone.getDefault().getRawOffset())/3600000;
	private String gmtOffsetStr = new String(((gmtOffset>=0)?"+":"") + Integer.toString(gmtOffset));
	
	public UserAttributes ua = null;
	public int countyId;
	public String dataSource = "";
	public Integer imageCount = null;
	public int agentTsrNameFormat = 0;
	public int agentTsrUpperLower = 0;
	public int colorCodeStatus = 0;
	
	private String requestCount = null;
	
	private int logOriginalLocation = 0;
	
	public String getAbstractorColumn() {
		
		String result = abstractorWorkedTime.getFormatted();
		
		for (AbstractorWorkedTime other : getOtherAbstractorWorkedTime().values()) {
			result += "<br>(" + other.getFormatted() + ")";
		}
		
		return result;
		
	}
	
	public String getAbstractorName() {
		return getAbstractorFirstName() + " " + getAbstractorLastName();
	}
	
	public String getOwnerNameEscaped() {
		return  getOwnerName();
	}
	
	public String getOwnerNameColored() {
		return  getOwnerName(true);
	}
	
	public String getOwnerName() {
		return getOwnerName(false);
	}
	public String getOwnerName(boolean getColored) {
		StringBuilder sb = new StringBuilder();
		for (NameI name : owners) {
			String fullName = ""; 
			try {
				NameFormaterI agentNf =null;
				if(agentId!=-1) {
					agentNf = MyAtsAttributes.getNameFormater(agentTsrUpperLower,agentTsrNameFormat); 
				}
				fullName = name.getFullName(MyAtsAttributes.getNameFormatterForDashboard(searchId,ua,agentNf));
			}
			catch(Exception e) {
				fullName = name.getFullName();	
			}
			String color = nameColors.get(name);
			if(StringUtils.isEmpty(color)) color="#000000";
			if(getColored) {
				sb.append("<span style='color:"+color+";'>"+StringUtils.HTMLEntityEncode(fullName) + "</span>; ");
			}else {
				sb.append(StringUtils.HTMLEntityEncode(fullName) + "; ");
			}
		}
		if(sb.length()>1)
			return sb.substring(0, sb.length() - 2);
		else
			return "";
	}

	public String getAgentName() {
		return getAgentFirstName() + " " + getAgentLastName();
	}
	
	public String getAgentColumn() {
		
		String agentName = getAgentFirstName() + " " + getAgentLastName();
		if(org.apache.commons.lang.StringUtils.isBlank(agentName)) {
			return agentName;
		}
		
		return getSendTo(agentName, true);
	}
	
	public String getPropertyAddress() {
		return  getPropertyNo() + " " + getPropertyStreet() + " " + getPropertySuffix();
	}
	
	public String getPropertyAddressColored() {
		String color = "#000000";
		String bootStrappedColor ="#FF0000";
		long bootstrapCode = getAddressBootstrappedCode();
		String formattedAddress = "";
		if (bootstrapCode == 1){
			formattedAddress = "<span style=\"color:"
								+ bootStrappedColor 
								+ ";\">" 
								+ getPropertyNo() + " "
								+ getPropertyStreet() + " "
								+ getPropertySuffix() + "</span>";
		} else {
			formattedAddress += "<span style=\"color:"
									+ (((bootstrapCode & STREET_NO_MASK )>0) ? bootStrappedColor : color)
									+ ";\">" + getPropertyNo() + " </span>";
			formattedAddress += "<span style=\"color:"
								+ (((bootstrapCode & STREET_NAME_MASK )>0) ? bootStrappedColor : color)
								+ ";\">" + getPropertyStreet() + " </span>";
			formattedAddress += "<span style=\"color:"
								+ (((bootstrapCode & STREET_SUFFIX_MASK )>0) ? bootStrappedColor : color)
								+ ";\">" + getPropertySuffix() + " </span>";
		}
		return  formattedAddress;
	}
	
	public String getPropertyFullCounty() {
		return StringUtils.HTMLEntityEncode(((propertyState == null || "null".equals(propertyState)) ? "" : propertyState) + " " 
		+ ((propertyCounty == null || "null".equals(propertyCounty)) ? "" : propertyCounty));
	}

	public String getHour() {
		try {
			if (searchTimeStamp == null || "".equals(searchTimeStamp)) {
				return "&nbsp;";
			}
			return (sdf4.format(sdf2.parse((sdf3.format(searchTimeStamp))+" GMT"))).toString();
		} catch (ParseException e) {
			e.printStackTrace();
			return "N/A";
		}
	}

	//private static SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private static SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z");
	private static SimpleDateFormat sdf3 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	private static SimpleDateFormat sdf4 = new SimpleDateFormat("HH:mm:ss");
	//private static SimpleDateFormat sdf5 = new SimpleDateFormat("MM/dd/yyyy");
	//private static SimpleDateFormat sdf6 = new SimpleDateFormat("yyyy");
	private static SimpleDateFormat sdf7 = new SimpleDateFormat("MM/dd/yy");
	private static SimpleDateFormat sdf8 = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	
	
	public String getDateHour() {
		try {
			if (searchTimeStamp == null || "".equals(searchTimeStamp)) {
				return "&nbsp;";
			}
			return (sdf8.format(sdf2.parse((sdf3.format(searchTimeStamp))+" GMT"))).toString();
		} catch (ParseException e) {
			e.printStackTrace();
			return "N/A";
		}
	}
	
	public String getTSRHour() {
		try {
			if (tsrTimeStamp == null || "".equals(tsrTimeStamp)) {
				return "&nbsp;";
			}
			return (sdf4.format(sdf2.parse((sdf3.format(tsrTimeStamp))+" GMT"))).toString();
		} catch (ParseException e) {
			e.printStackTrace();
			return "N/A";
		}
	}
	
	public String getTSRDateHour() {
		try {
			if (tsrTimeStamp == null || "".equals(tsrTimeStamp)) {
				return "&nbsp;";
			}
			return (sdf8.format(sdf2.parse((sdf3.format(tsrTimeStamp))+" GMT"))).toString();
		} catch (ParseException e) {
			e.printStackTrace();
			return "N/A";
		}
	}
	
	public String getTSRDateHourNew(){
		//B 6770
		try {
			if (tsrInitialDate == null) {
				return getTSRDateHour();
			}
			
			String tsrInitalDateString = (sdf8.format(sdf2.parse((sdf3.format(tsrInitialDate))+" GMT"))).toString();
			
			if (tsrTimeStamp == null){
				return "("+ tsrInitalDateString +")";
			}
			
			String tsrTimeStampString = (sdf8.format(sdf2.parse((sdf3.format(tsrTimeStamp))+" GMT"))).toString();
			
			if(tsrTimeStampString.equals(tsrInitalDateString)){
				return tsrTimeStampString;
			}
			
			return tsrTimeStampString + "<br>" + "("+ tsrInitalDateString +")";
		} catch (ParseException e) {
			e.printStackTrace();
			return "N/A";
		}
	}
	
	public String getStatus(){

		if(status!=null)
			return status;
		status = "";
        
        if( wasOpened == null )
        {
            wasOpened = new Boolean( DBManager.getDbSearchWasOpened( searchId ) );
            logger.error("This shouldn't happen. We should have the field by now. Big performance problem");
        }
        
        if( getSearchFlags().getStatus() == Search.SEARCH_STATUS_N  && 
        		(tsrTimeStamp == null ||getSearchFlags().isClosed()) &&
        		!getSearchFlags().isTsrCreated())
        {
        	status = "N";
        	if (getSearchFlags().isClosed()) {
    			status += "K";
    		}
    		if( getSearchFlags().isOld() ) {
	        	status += "O";
	        }
        	return status;
        } else if( getSearchFlags().getStatus() == Search.SEARCH_STATUS_T  ) {
        	status = "T";
        }
        
        if (archived == 1)
			status += "A";
        
		if (confirmed == 1)
			status += "C";
        
        //if( (wasOpened || tsrTimeStamp != null || getSearchFlags().isClosed()) 
        if( wasOpened || getSearchFlags().isTsrCreated()) {
        	status += "D";
        }
        if (getSearchFlags().isForReview()){
        	status += "E";
        }
        if (getSearchFlags().isClosed()) {
			status += "K";
		}
        if (invoiced > 0){
			status += "I";
        }
        
		if (paid == 1) {
			status += "P";
			status = status.replaceAll("D", "");
        }
        
        if( getSearchFlags().isOld() ) {
        	status += "O";
        }
                
        if(getSearchFlags().isBase()) {
        	status += "B";
        }
        
        if(CREATION_SOURCE_TYPES.REOPENED.equals(getSearchFlags().getCreationSourceType())) {
        	status += "R";
        }
        if(CREATION_SOURCE_TYPES.CLONED.equals(getSearchFlags().getCreationSourceType())) {
        	status += "L";
        }
        if (getSearchFlags().isForFVS()){
        	status += "F";
        }
        
		return StringUtils.HTMLEntityEncode(status);
	}
	
    public void setStatus(String status) {
		this.status = status;
	}

	public String getColor(){
		String searchStatus = getStatus();
		String color = "";
		if(searchStatus.contains("N")) {
			color = "#FFADAD"; 
		} else {
			color = DBSearch.getColorCodeFromDatabaseStatus(getColorCodeStatus());
		}
		return color;
	}
	
    public void setColor(String color) {
	}

	public void setWasOpened(Boolean wasOpened) {
		this.wasOpened = wasOpened;
	}

	public int getProductType()
    {
        if( productType == 0 )
        {
        	productType = DBManager.getProductIdFromSearch( searchId );
        	//product = "test";
        }
        
        return productType;
    }
    
    public void setProductType(int productType) {
		this.productType = productType;
	}

    public String getProductName(){
    	if (productName == null)
    	{
    		productName = DBManager.getProductNameFromSearch(searchId);
    	}
    	return productName;
    }
    
    public void setProductName(String productName){
    	this.productName = productName;
    }
    
	public String getFee()
    {
        if(fee>=0)
        	return "$"+fee;
        try
        {
            fee = DBManager.getSearchFee( searchId, getProductType(), UserUtils.isTSAdmin( InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser() ) );
        }
        catch( Exception e )
        { 
        	e.printStackTrace();
        }
        
        return "$" + fee;
    }
	
	public double getFeeAsDouble(){
		return fee;
	}
	public void setFee(double fee){
		this.fee = fee;
	}
    
	public String getTimeZone(){
		return gmtOffsetStr;
	}

	public String getAbstractorFirstName() {
		return StringUtils.HTMLEntityEncode(abstractorWorkedTime.getFirstName()).toString();
	}

	public String getAbstractorLastName() {
		return StringUtils.HTMLEntityEncode(abstractorWorkedTime.getLastName()).toString();
	}

	public String getAgentFirstName() {
		return StringUtils.HTMLEntityEncode(agentFirstName).toString();
	}

	public String getAgentLastName() {
		return StringUtils.HTMLEntityEncode(agentLastName).toString();
	}

	public String getFileId() {
		return StringUtils.HTMLEntityEncode(fileId).toString();
	}

	public String getOwnerFirstName() {
		return StringUtils.HTMLEntityEncode(ownerFirstName).toString();
	}

	public String getOwnerLastName() {
		return StringUtils.HTMLEntityEncode(ownerLastName).toString();
	}

	public String getPropertyCounty() {
		return StringUtils.HTMLEntityEncode(propertyCounty).toString();
	}

	public String getPropertyNo() {
		return StringUtils.HTMLEntityEncode(propertyNo).toString();
	}

	public String getPropertyStreet() {
		return StringUtils.HTMLEntityEncode(propertyStreet).toString();
	}

	public String getPropertySuffix() {
		return StringUtils.HTMLEntityEncode(propertySuffix).toString();
	}

	public String getSendTo() {
		return getSendTo("email", false);
	}
	
	public String getSendTo(String value, boolean forceValue) {
		
		if(org.apache.commons.lang.StringUtils.isBlank(sendTo) || "N/A".equals(sendTo)){
			if(forceValue) {
				return value;
			} else {
				return "N/A";
			}
		}
		
		String phone = "";
		if (wPhone != null && wPhone.length() > 0 && !wPhone.equals("N/A")){
			phone += StringUtils.HTMLEntityEncode("Work Phone:" + wPhone);
		}
		if (hPhone != null && hPhone.length() > 0  && !hPhone.equals("N/A")){
			if (phone.length() > 0) {
				phone += "<BR>";
			}
			phone += StringUtils.HTMLEntityEncode("Home Phone:" + hPhone);
		}
		if (mPhone != null && mPhone.length() > 0 && !mPhone.equals("N/A")){
			if (phone.length() > 0) {
				phone += "<BR>";
			}
			phone += StringUtils.HTMLEntityEncode("Mobile Phone:" + mPhone);
		}
		if (phone.length() == 0){
			phone = StringUtils.HTMLEntityEncode("Phone number: N/A");
		}
		
		
		return "<a onmouseover=\"xstooltip_show('email_" + searchId + "',event,'" + phone + "');\" " +
				"onmouseout=\"xstooltip_hide('email_" + searchId + "');\" id='email_" + searchId + "' " +
				"href=\"mailto:" + StringUtils.HTMLEntityEncode(sendTo) + "\">" + value + "</a>";
	}

	public String getSearchDueDate() {		
		
		if(searchDueDate == null){
			return "-";
		}
		try {	
			String noteDate=(sdf7.format(sdf2.parse((sdf3.format(searchDueDate))+" GMT"))).toString();
			if (searchDueDate.before(Calendar.getInstance().getTime())&&tsrTimeStamp==null)
				return "<font color='#FF0000'>"+noteDate+"</font>";
			else return noteDate;			
		} catch (ParseException e) {
			e.printStackTrace();
			return "-";
		}		
	}
	
	public Date getSearchTimeStamp() {
		return searchTimeStamp;
	}
	
	public String getNote() {
		return StringUtils.prepareStringForHTML(note);
	}
	
	public int getNoteStatus() {
		return noteStatus;
	}
	
	public String getHasNote() {
		return note != null && !"null".equals(note) && note.length() > 0 ? "false" : "true";
	}
	
	/////////////////////  SET

	public void setAgentFirstName(String string) {
		agentFirstName = StringUtils.transformNull(string);
	}

	public void setAgentLastName(String string) {
		agentLastName = StringUtils.transformNull(string);
	}

	public void setFileId(String string) {
		fileId = StringUtils.transformNull(string);
	}

	public void setOwnerFirstName(String string) {
		ownerFirstName = StringUtils.transformNull(string);
	}

	public void setOwnerLastName(String string) {
		ownerLastName = StringUtils.transformNull(string);
	}

	public void setPropertyCounty(String string) {
		propertyCounty = string;
	}

	public void setPropertyNo(String string) {
		propertyNo = StringUtils.transformNull(string);
		propertyNo = propertyNo.replaceAll("'", ""); 
	}

	public void setPropertyStreet(String string) {
		propertyStreet = StringUtils.transformNull(string);
		propertyStreet = propertyStreet.replaceAll("'", "");
	}

	public void setPropertySuffix(String string) {
		propertySuffix = StringUtils.transformNull(string);
		propertySuffix = propertySuffix.replaceAll("'", "");
	}

	public void setSendTo(String string) {
		sendTo = StringUtils.transformNull(string);
	}

	public void setSearchDueDate(Date date) {
		searchDueDate = date;
	}
	
	public void setSearchTimeStamp(Date date) {
		searchTimeStamp = date;
	}
	
	public void setNote(String string) {
		note = string;
	}
	
	public void setNoteStatus(int noteStatus) {
		this.noteStatus = noteStatus;
	}

	public String getFileLink() {
		return fileLink;
	}

	public void setFileLink(String string) {
		fileLink = StringUtils.transformNull(string);
	}

	public String getPropertyState() {
		return propertyState;
	}

	public void setPropertyState(String string) {
		propertyState = StringUtils.transformNull(string);
	}

	public String getInvoice() {
		return "" + invoice;
	}

	public void setInvoice(int i) {
		invoice = i;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long l) {
		searchId = l;
	}

	public int getPaid() {
		return paid;
	}

	public void setPaid(int i) {
		paid = i;
	}

	public int getInvoiced() {
		return invoiced;
	}

	public void setInvoiced(int i) {
		invoiced = i;
	}

	public int getConfirmed() {
		return confirmed;
	}

	public void setConfirmed(int i) {
		confirmed = i;
	}

	public int getArchived() {
		return archived;
	}
	
	public void setArchived(int i) {
		archived = i;
	}

	public String getTSRFolder() {
	    return tsrFolder;
	}
	
	public void setTSRFolder(String tsrFolder) {
	    this.tsrFolder = tsrFolder;
	}

	/**
	 * @return Returns the entrySearchId.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param entrySearchId The entrySearchId to set.
	 */
	public void setId(long entrySearchId) {
		this.id = entrySearchId;
	}
	/**
	 * @return Returns the tsrTimeStamp.
	 */
	public Date getTsrTimeStamp() {
		return tsrTimeStamp;
	}
	/**
	 * @param tsrTimeStamp The tsrTimeStamp to set.
	 */
	public void setTsrTimeStamp(Date tsrTimeStamp) {
		this.tsrTimeStamp = tsrTimeStamp;
	}
	
	/**
	 * @return Returns the TSR first open date.
	 */
	public Date getTsrOpenDate() {
		return tsrOpenDate;
	}
	/**
	 * @param tsrOpenDate The tsrOpenDate (TSR first open date) to set.
	 */
	public void setTsrOpenDate(Date tsrOpenDate) {
		this.tsrOpenDate = tsrOpenDate;
	}
	
	/**
	 * @return Returns the first TSR creation date.
	 */
	public Date getTsrInitialDate() {
		return tsrInitialDate;
	}
	/**
	 * @param tsrInitialDate The tsrInitialDate (first TSR creation date) to set.
	 */
	public void setTsrInitialDate(Date tsrInitialDate) {
		this.tsrInitialDate = tsrInitialDate;
	}
    
    /**
     * we have to know if the data will be used for reports, or invoice
     * ICM 8 - 3.5.61
     * 
     * sets the variable isInvoiceData
     * true - data for invoice
     * false - data for reports
     */
    public void setCallFromInvoice( boolean invoiceCall )
    {
        isInvoiceData = invoiceCall;
    }
    
    /**
     * ICM 8 - 3.5.61
     * 
     * gets the variabile isInvoiceData
     * true - data for invoice
     * false - data for reports
     */    
    public boolean getCallFromInvoice()
    {
        return isInvoiceData;
    }
    
    public boolean getWasOpened()
    {
        if( wasOpened != null )
        {
            return wasOpened.booleanValue();
        }
        
        return false;
    }
    
    public String getOrderFileLink()
    {
        if( orderFileLink != null && !"".equals( orderFileLink ) )
        {
            return "<a href=\"#\" onClick=\"javascript:mywindow=window.open('" + StringUtils.HTMLEntityEncode(orderFileLink) + "','mywindow','toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=no,resizable=Yes,width=750,height=520'); mywindow.focus();\" onmouseover=\"stm(78,Style[1]);\" onMouseOUT=\"htm();\"><img width=\"17\" height=\"17\" border=\"0\" align=\"absmiddle\" name=\"orderSwapImage\" src=\"/title-search/web-resources/images/ico_order_1.gif\"/></a>";
        } else {
            return "";
        }
    }
    
    public void setOrderFileLink(String orderFileLink)
    {
        this.orderFileLink = orderFileLink;
    }
    
        
    public String getLogFileLink()
    {
        if( logFileLink != null && !"".equals( logFileLink ) )
        {
            return "<a href=\"#\" onClick=\"javascript:mywindow=window.open('" + StringUtils.HTMLEntityEncode(logFileLink) + "','mywindow','toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=yes,resizable=Yes,width=750,height=520'); mywindow.focus();\" onmouseover=\"stm(96,Style[1]);\" onMouseOUT=\"htm();\"><img width=\"17\" height=\"17\" border=\"0\" align=\"absmiddle\" name=\"orderSwapImage\" src=\"/title-search/web-resources/images/ico_order_2.gif\"/></a>";
        } else {
            return "";
        }    	
    }
    
    public void setLogFileLink(String logFileLink) 
    {
    	this.logFileLink = logFileLink;
    }
    
     public void setTsrIndexFileLink(String tsrIndexFileLink){
    	this.tsrIndexFileLink = tsrIndexFileLink;
    }
    
    /*
    public String getTsrIndexFileLink(){
    	return tsrIndexFileLink;
    }
    */
    
    private static  String escapeAmp(String what, String item){
    	if(what==null)
    		return what;
    	int firstPoz = what.indexOf("&");
    	int lastPoz = what.indexOf(item);
    	if( firstPoz <lastPoz && firstPoz>0 && lastPoz>0 && what.indexOf("?f=")>0 ){
    		what = what.replaceFirst("[&]", "%2526");
    	}
    	return what;
    }
     
    public String getLogFiles(){
    	String retVal = "&nbsp;";
    	orderFileLink  = escapeAmp(orderFileLink, ".html");
    	logFileLink = escapeAmp(logFileLink, ".html");
    	tsrIndexFileLink = escapeAmp(tsrIndexFileLink, ".html");
    	
    	if(orderFileLink != null && !"".equals(orderFileLink)){
    		retVal += "<span class='submitLinkBlue' onClick=\"javascript:orderWindow=window.open('" + StringUtils.HTMLEntityEncode(orderFileLink) + "','orderWindow','toolbar=yes,location=yes,directories=yes,status=yes,menubar=yes,scrollbars=yes,resizable=yes,width=1024,height=768,left=0,top=0,screenX=0,screenY=0'); orderWindow.focus();\" onmouseover=\"stm(78,Style[1]);\" onMouseOUT=\"htm();\">O</span>&nbsp;";
    	}
    	if(logFileLink != null && !"".equals(logFileLink)){
    		retVal += "<span class='submitLinkBlue' onClick=\"javascript:logWindow=window.open('" + StringUtils.HTMLEntityEncode(logFileLink) + "','logWindow','toolbar=yes,location=yes,directories=yes,status=yes,menubar=yes,scrollbars=yes,resizable=yes,width=1024,height=768,left=0,top=0,screenX=0,screenY=0'); logWindow.focus();\" onmouseover=\"stm(96,Style[1]);\" onMouseOUT=\"htm();\">S</span>&nbsp;"; 
    	}
    	if(tsrIndexFileLink != null && !"".equals(tsrIndexFileLink)){
    		retVal += "<span class='submitLinkBlue' onClick=\"javascript:tsrWindow=window.open('" + StringUtils.HTMLEntityEncode(tsrIndexFileLink)+ "','tsrWindow','toolbar=yes,location=yes,directories=yes,status=yes,menubar=yes,scrollbars=yes,resizable=yes,width=1024,height=768,left=0,top=0,screenX=0,screenY=0'); tsrWindow.focus(); \" onmouseover=\"stm(97,Style[1]);\" onMouseOUT=\"htm();\">T</span>&nbsp;";
    	}
    	retVal += "";    	
    	return retVal;
    }

	public int getDuplicate() {
		return duplicate;
	}

	public void setDuplicate(int duplicate) {
		this.duplicate = duplicate;
	}

	public int compareTo(DayReportLineData o) {
		if(this.tsrTimeStamp == null) {
			if(o.tsrTimeStamp == null){
				return 0;
			} else {
				return this.searchTimeStamp.compareTo(o.tsrTimeStamp);
			}
		} else {
			if(o.tsrTimeStamp == null){
				return this.tsrTimeStamp.compareTo(o.searchTimeStamp);
			} else {
				return this.tsrTimeStamp.compareTo(o.tsrTimeStamp);
			}
		}
	}

	public void setWPhone(String s){
		wPhone = s;
	}

	public void setHPhone(String s){
		hPhone = s;
	}	

	public void setMPhone(String s){
		mPhone = s;
	}

	public SearchFlags getSearchFlags() {
		if(searchFlags == null)
			searchFlags = new SearchFlags();
		return searchFlags;
	}

	public void setSearchFlags(SearchFlags searchFlags) {
		this.searchFlags = searchFlags;
	}	
	
	public void setAgentId(String s){
		try {
			agentId = Long.parseLong(s);
		}catch(Exception e)  { 
			agentId = -1; 
		}
	}	
	
	public void setAgentId(Long s){
		agentId = s;
	}	
	
	public long getAgentId() {
		return agentId;
	}
	
	public void addOwner(NameI name){
		owners.add(name);
	}

	public UserAttributes getUa() {
		return ua;
	}

	public void setUa(UserAttributes ua) {
		this.ua = ua;
	}

	public Map<NameI, String> getNameColors() {
		return nameColors;
	}

	public void setNameColors(Map<NameI, String> colors) {
		this.nameColors = colors;
	}

	public boolean isAddressBootstrapped() {
		return addressBootstrapped;
	}

	public void setAddressBootstrapped(boolean isAddressBootstrapped) {
		this.addressBootstrapped = isAddressBootstrapped;
	}
	
	public int getAddressBootstrappedCode() {
		return addressBootstrappedCode;
	}

	public void setAddressBootstrappedCode(int addressBootstrappedCode) {
		this.addressBootstrappedCode = addressBootstrappedCode;
	}

	/**
	 * @return the discountRatio
	 */
	public float getDiscountRatio() {
		return discountRatio;
	}

	/**
	 * @param discountRatio the discountRatio to set
	 */
	public void setDiscountRatio(float discountRatio) {
		this.discountRatio = discountRatio;
	}

	/**
	 * @return the countyId
	 */
	public int getCountyId() {
		return countyId;
	}

	/**
	 * @param countyId the countyId to set
	 */
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getImageCount() {
		if(imageCount == null) {
			return "N/A";
		}
		return imageCount.toString();
	}

	public void setImageCount(Integer imageCount) {
		this.imageCount = imageCount;
	}

	public int getAgentTsrNameFormat() {
		return agentTsrNameFormat;
	}

	public void setAgentTsrNameFormat(int agentTsrNameFormat) {
		this.agentTsrNameFormat = agentTsrNameFormat;
	}

	public int getAgentTsrUpperLower() {
		return agentTsrUpperLower;
	}

	public void setAgentTsrUpperLower(int agentTsrUpperLower) {
		this.agentTsrUpperLower = agentTsrUpperLower;
	}

	public int getColorCodeStatus() {
		return colorCodeStatus;
	}

	public void setColorCodeStatus(int colorCodeStatus) {
		this.colorCodeStatus = colorCodeStatus;
	}

	public String getRequestCountDescription() {
		if(requestCount == null)
			return "N/A";
		return requestCount;
	}

	public void setRequestCountDescription(String requestCount) {
		this.requestCount = requestCount;
	}

	public boolean isLogInTable() {
		return getLogOriginalLocation() == ServerConfig.getLogInTableVersion();
	}
	
	public int getLogOriginalLocation() {
		return logOriginalLocation;
	}

	public void setLogOriginalLocation(int logOriginalLocation) {
		this.logOriginalLocation = logOriginalLocation;
	}
	
	public void setAbstractorWorkedTime(AbstractorWorkedTime abstractorWorkedTime) {
		this.abstractorWorkedTime = abstractorWorkedTime;
	}
	
	public AbstractorWorkedTime getAbstractorWorkedTime() {
		return abstractorWorkedTime;
	}
	
	public Map<Long, AbstractorWorkedTime> getOtherAbstractorWorkedTime() {
		if(otherAbstractorWorkedTime == null) {
			otherAbstractorWorkedTime = new LinkedHashMap<>();
		}
		return otherAbstractorWorkedTime;
	}
	
	public AbstractorWorkedTime addOtherAbstractorWorkedTime(AbstractorWorkedTime abstractorWorkedTime) {
		if(abstractorWorkedTime == null) {
			return null;
		}
		return getOtherAbstractorWorkedTime().put(abstractorWorkedTime.getUserId(), abstractorWorkedTime);
	}

	/**
	 * @return formatted totalTimeWorked
	 */
	public String getTotalTimeWorkedFormatted(){
		
		long totalTimeWorked = abstractorWorkedTime.getWorkedTime();
		
		for (AbstractorWorkedTime other : getOtherAbstractorWorkedTime().values()) {
			totalTimeWorked += other.getWorkedTime();
		}
		
		SearchUserTimeMapper sutm = new SearchUserTimeMapper();
		sutm.setWorkedTime(totalTimeWorked);
		
		return sutm.getWorkedTimeFormatted();
	}
	
}
