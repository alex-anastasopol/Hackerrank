package ro.cst.tsearch.generic.tag;

//general
//import ro.cst.tsearch.utils.Log;
import org.apache.log4j.Logger;

public abstract class SelectTag extends GenericTag {
    
	private static final Logger logger = Logger.getLogger(SelectTag.class);
    /**
     * Name. Required.
     */
    protected String selectName ; 
    public void setSelectName(String selectName) { 
            this.selectName = selectName; 
    }
    public String getSelectName() { return selectName; }

    /**
     * id
     */    
    protected String selectID = ""; 
    public void setSelectID(String selectID) { 
            this.selectID = selectID; 
    }
    public String getSelectID() { return selectID; }
    
    /**
     * Size. 
     */
    protected String selectSize  = "1"; 
    public void setSelectSize(String selectSize) { 
            this.selectSize = selectSize; 
    }
    public String getSelectSize() { return selectSize; }
    
    /**
     * Style
     */
    private String selectStyle = ""; 
    public void setSelectStyle(String selectStyle) { 
            this.selectStyle = selectStyle; 
    }
    public String getSelectStyle() { return selectStyle; }
    

    /**
     * for multiple select
     */
    private boolean selectMultiple = false;
    public void setSelectMultiple(String str) { selectMultiple = Boolean.valueOf(str).booleanValue(); }
    public String getSelectMultiple() {return new Boolean(selectMultiple).toString(); }
    
    /**
     * if all is true then the select will contain the all option
     * with value -1
     */
    protected boolean all = false;
    public void setAll(String str) { all = Boolean.valueOf(str).booleanValue(); }
    public String getAll() {return new Boolean(all).toString(); }
    
	/**
	 * this param is used to display "None" as an option in the select
	 * the value will be "0" (it should send 0 after submit)
	 */
	protected boolean none = false;
	public void setNone(String none) {this.none= ((none.toUpperCase().equals("TRUE"))?true:false);}
	public String getNone(){return ""+none;}
	
	/**
	 * this param is used to display "No change" as an option in the select the
	 * value will be "0" (it should send 0 after submit)
	 */
	private boolean noChange = false;
	public void setNoChange(String noChange) {this.noChange= ((noChange.toUpperCase().equals("TRUE"))?true:false);}
	public String getNoChange(){return ""+noChange;}
	
	/**
	 * this param is used to display "Other" as an option in the select the
	 * value will be "-1" (it should send -1 after submit)
	 */
	private boolean other = false;
	public void setOther(String other) {this.other= ((other.toUpperCase().equals("TRUE"))?true:false);}
	public String getOther(){return ""+other;}
	
	
	private boolean eventsOff = false;
	public void setEventsOff(String eventsOff) {this.eventsOff= ((eventsOff.toUpperCase().equals("TRUE"))?true:false);}
	public String getEventsOff(){return ""+eventsOff;}   
	
	protected boolean disabled = false;
	public void setDisabled(String disabled) {this.disabled= ((("true".equalsIgnoreCase(disabled)))?true:false);}
	public String getDisabled(){
		if(disabled) {
			return " disabled='disabled' ";
		}
		return "";
	}
    
    
    /**
     * start building the select
     */
    protected  String beginSelect() 
        throws Exception {

        StringBuffer sb = new StringBuffer(1000);
                    
        sb.append("<select name=\""+ selectName 
                    + ("".equals(selectID)?"":"\" id=\"" + selectID + "\" ")
                    + "\" size=\"" + selectSize + "\" "
                    + ((selectMultiple)?"multiple":"")
                    + ("".equals(selectStyle)?"":" style=\"" + selectStyle + "\" ")
                    + getDisabled()
                    + (eventsOff?"":" onblur=\"javascript:" + selectName + "SelectOnBlur();\" "
                    + " onfocus=\"javascript:" + selectName + "SelectOnFocus();\" "
                    + " onchange=\"javascript:" + selectName + "SelectOnChange();\" ") 
					+ " >"
                    );
            
        return sb.toString();                
    }
    
    /**
     * Create all option if necessary
     */
    protected  String allOption(int id) 
            throws Exception {

        if(all) {
            return "<option "+(id==-1?"selected":"")+" value='-1'>All</option>" ;
        } else {
            return "";
        }
    
    }
    
	/**
	 * Create other option if necessary
	 */
	protected  String otherOption(int id)
			throws Exception {

		if(other) {
			return "<option "+(id==-1?"selected":"")+" value='-1'>Other</option>" ;
		} else {
			return "";
		}

	}
    
    
	/**
	 * Create none option if necessary
	 */
	protected  String noneOption(int id)
			throws Exception {

		if(none) {
			return "<option "+(id==0?"selected":"")+" value='0'>None</option>" ;
		} else {
			return "";
		}

	}
	
	/**
	 * Create No Change option if necessary
	 */
	protected  String noChangeOption(int id)
			throws Exception {

		if(noChange) {
			return "<option "+(id==0?"selected":"")+" value='0'>No change</option>" ;
		} else {
			return "";
		}

	}
	
    
    /**
     *
     */
    protected abstract String createOptions() throws Exception ;


    /**
     * start building the select
     */
    protected  String endSelect() 
        throws Exception {

        return "</select>";
    }
    
    public int doStartTag() {
        try {
            //logger.debug("Debug: SelectTag#doStartTag start " + selectName );
            initialize();
                        
            StringBuffer sb = new StringBuffer(1000);
                        
            sb.append( beginSelect() );
            sb.append( createOptions() );
            sb.append( endSelect() );
                        
            pageContext.getOut().print(sb.toString());
			//logger.debug("Debug: SelectTag#doStartTag done " + selectName);
            return(SKIP_BODY);
        }

        catch (Exception e) {
            e.printStackTrace();
			logger.error(this.getClass().toString()+"#doStartTag Exception in Tag " + this.getClass().toString()); 
        }
                
        return(SKIP_BODY);
    }
}
