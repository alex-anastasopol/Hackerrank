package ro.cst.tsearch.servers.info;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.parentsitedescribe.ComboValue;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.StringUtils;

public class TSServerInfoFunction implements Serializable {

    static final long serialVersionUID = 10000000;

    public static final int idTEXT = 0, idDate = 1, idRadioBotton = 2,
            idSingleselectcombo = 3,
            idMultipleSelectCombo = 6,
            ///////////////////
            idDataStylMMDDYYYY = 10, idDataStylOther = 11,
            idDataStylMMDDYY = 12,idCheckBox= 13, idDataStylMMDDYYYNoSep = 14;

    private int miParamType, miParamDataType = idDataStylOther;

    private String msName = "", msParamName = "", msDefaultValue = "",
            msValue = "", msParamAlias = "";

    private boolean mbHiden = false, mbRequired = false;

    private String htmlformat = "";

    protected static final Category logger = Logger.getLogger(TSServerInfoFunction.class);

    private String paramValue = "";

    private String[] paramValues;
    
	private String saKey = ""; //the corresponding key in SearchAttributes
                               // object

    private int iteratorType = FunctionStatesIterator.ITERATOR_TYPE_DEFAULT;
    private String areaType="";
    /////////////////////////////////////////////////////////////////////////////////
    private boolean fake = false;
    private transient int controlType = 0;

    public TSServerInfoFunction() {
    }

    public TSServerInfoFunction(TSServerInfoFunction orig) {
        this.miParamType = orig.getParamType();
        this.miParamDataType = orig.getParamDataType();
        this.msName = orig.getName();
        this.msParamName = orig.getParamName();
        this.msDefaultValue = orig.getDefaultValue();
        this.msValue = orig.getValue();
        this.msParamAlias = orig.getParamAlias();
        this.mbHiden = orig.isHiden();
        this.mbRequired = orig.isRequired();
        this.paramValue = orig.getParamValue();
        this.iteratorType = orig.getIteratorType();
        this.saKey = orig.getSaKey();
        this.fake = orig.isFake();
        this.loggable = orig.isLoggable();
        this.label = orig.getLabel();
        this.htmlformat = orig.getHtmlformat();
        this.controlType = orig.getControlType();
        this.comboValue = orig.getComboValue();
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    public String getName() {
        return msName;
    }

    /**
     * Returns the paramName.
     * 
     * @return String
     */
    public String getParamName() {
        return msParamName;
    }
    
    
    /*
     * 
     *
     * Returns the parameter value
     *  
     * /
    public 


    /**
     * Returns the hiden.
     * 
     * @return boolean
     */
    public boolean isHiden() {
        return mbHiden;
    }

    /**
     * Returns the defaultValue.
     * 
     * @return String
     */
    public String getDefaultValue() {
        return msDefaultValue;
    }

    /**
     * Returns the value.
     * 
     * @return String
     */
    public String getParamAlias() {
        return msParamAlias;
    }

    /**
     * Returns the required.
     * 
     * @return boolean
     */
    public boolean isRequired() {
        return mbRequired;
    }

    /**
     * Returns the paramType.
     * 
     * @return int
     */
    public int getParamType() {
        return miParamType;
    }

    /////////////////////////////////////////////////////////////////////////////////
    /**
     * Sets the name.
     * 
     * @param name
     *            The name to set
     */
    public void setName(String name) {
        msName = name;
    }

    /**
     * Sets the paramName.
     * 
     * @param paramName
     *            The paramName to set
     */
    public void setParamName(String paramName) {
        msParamName = paramName;
    }

    /**
     * Sets the hiden.
     * 
     * @param hiden
     *            The hiden to set
     */
    public void setHiden(boolean hiden) {
        mbHiden = hiden;
    }

    /**
     * Sets the defaultValue.
     * 
     * @param defaultValue
     *            The defaultValue to set
     */
    public void setDefaultValue(String defaultValue) {
        msDefaultValue = defaultValue;
    }

    /**
     * Sets the value.
     * 
     * @param value
     *            The value to set
     */
    public void setParamAlias(String ParamAlias) {
        msParamAlias = ParamAlias;
    }

    /**
     * Sets the required.
     * 
     * @param required
     *            The required to set
     */
    public void setRequired(boolean required) {
        mbRequired = required;
    }

    /**
     * Sets the paramType.
     * 
     * @param paramType
     *            The paramType to set
     */
    public void setParamType(int paramType) {
        miParamType = paramType;
    }

    /**
     * Returns the value.
     * 
     * @return String
     */
    public String getValue() {
        return msValue;
    }

    /**
     * Sets the value.
     * 
     * @param value
     *            The value to set
     */
    public void setValue(String value) {
        msValue = value;
    }

    /**
     * Returns the paramDataType.
     * 
     * @return int
     */
    public int getParamDataType() {
        return miParamDataType;
    }

    /**
     * Sets the paramDataType.
     * 
     * @param paramDataType
     *            The paramDataType to set
     */
    public void setParamDataType(int paramDataType) {
        miParamDataType = paramDataType;
    }

    /**
     * Method getFormatedData.
     * 
     * @param string
     * @return String
     */
    public String getFormatedData(String sData) {
        String sTmp, sTmp1;
        if ((miParamType == idDate) && sData.equals(""))
            sData = CSTCalendar.getDateFromInt(CSTCalendar
                    .getDefaultInitDate("MDY"), "MDY");
        if (miParamDataType == idDataStylMMDDYYYY) {
            sTmp1 = ""
                    + (Integer.parseInt(CSTCalendar.getMonthFromInput(sData)) + 1);
            if (sTmp1.length() != 2)
                sTmp1 = "0" + sTmp1;
            sTmp = sTmp1 + "/";
            sTmp1 = CSTCalendar.getDayFromInput(sData);
            if (sTmp1.length() != 2)
                sTmp1 = "0" + sTmp1;
            sTmp += sTmp1 + "/";
            sTmp += CSTCalendar.getYearFromInput(sData);
            return sTmp;
        } else if (miParamDataType == idDataStylMMDDYY) {
            sTmp1 = ""
                    + (Integer.parseInt(CSTCalendar.getMonthFromInput(sData)) + 1);
            if (sTmp1.length() != 2)
                sTmp1 = "0" + sTmp1;
            sTmp = sTmp1;
            sTmp1 = CSTCalendar.getDayFromInput(sData);
            if (sTmp1.length() != 2)
                sTmp1 = "0" + sTmp1;
            sTmp += sTmp1;
            sTmp += CSTCalendar.getYearFromInput(sData).substring(2, 4);
            return sTmp;
        }
        else if( miParamDataType == idDataStylMMDDYYYNoSep ) {
            sTmp1 = ""
                + (Integer.parseInt(CSTCalendar.getMonthFromInput(sData)) + 1);
            if (sTmp1.length() != 2)
                sTmp1 = "0" + sTmp1;
            sTmp = sTmp1;
            sTmp1 = CSTCalendar.getDayFromInput(sData);
            if (sTmp1.length() != 2)
                sTmp1 = "0" + sTmp1;
            sTmp += sTmp1;
            sTmp += CSTCalendar.getYearFromInput(sData);
            return sTmp;
        }
        else
            return sData;
    }

    /**
     * @return
     */
    public int getIteratorType() {
        return iteratorType;
    }

    /**
     * @param i
     */
    public void setIteratorType(int i) {
        //	logger.debug("setIteratorType("+i+")");
        iteratorType = i;
    }

    public String toString() {
        String s = "Function(";
        s += msParamName + " = ";
        s += paramValue;
        s += ")";
        return s;
    }

    /**
     * @return
     */
    public String getParamValue() {
        return paramValue;
    }

    /**
     * @param string
     */
    public void setParamValue(String string) {
        paramValue = string;
        //logger.debug( this );
    }
    
    /**
     * @param string
     */
    public void forceValue(String string) {
    	setParamValue(string);
    	setDefaultValue(string);
    	setValue(string);
    }

    /**
     * @return
     */
    public String getSaKey() {
        return saKey;
    }

    /**
     * @param string
     */
    public void setSaKey(String string) {
        saKey = string;
    }

    /**
     * @return
     */
    public boolean isEmpty() {
        return (StringUtils.isStringBlank(getParamValue()));
    }

    public boolean isRadioFct() {
        return (getParamType() == TSServerInfoFunction.idRadioBotton);
    }

    public void setData(String value) {
        //logger.debug("set Data for fct " + this);
        String val = getDefaultValue();
        if (!isHiden()) {
            if (value != null) {
                val = value;
            }
        } else if("".equals(val)) {
        	if (value != null) {
                val = value;
            }
        	
        }
        setParamValue(val);
    }

    public void setData(SearchDataWrapper sd) {
        HttpServletRequest request = sd.getRequest();
        SearchAttributes sa = sd.getSa();
        String val = null;
        String []values = null;
        if (request != null) {
            val = request.getParameter(getParamAlias());
            values = request.getParameterValues(getParamAlias());
            if( val == null ) {
            	val = request.getParameter( msParamName );
            }
            
            if(values==null){
            	values = request.getParameterValues(msParamName);
            }
            
        } else if (sa != null) {
            if (!getSaKey().equals("")) {
                val = sa.getAtribute(getSaKey());
            }
        } else {
        }
        if (val == null && !"".equals(paramValue))
            setData(paramValue);
        else
            setData(val);
        
        if (values != null )
            setParamValues(values);
        
        //logger.debug("getParamAlias() = " + getParamAlias());
        //logger.debug("val = " + val );
        //logger.debug("set data for " + this );
    }

    public void setDefaultValue(SearchAttributes sa) {
        String val = getDefaultValue();
        if ((sa != null) && (!getSaKey().equals(""))) {
            val = sa.getAtribute(getSaKey());
        }
        setDefaultValue(val);
    }

    public String getParamValueForQuery() {
        return getFormatedData(getParamValue());
    }

    public void setupParameterAliases(int moduleIdx, int functionIdx) {
        if (getParamType() == idRadioBotton)
            setParamAlias("param_" + moduleIdx);
        else
            setParamAlias("param_" + moduleIdx + "_" + functionIdx);
    }

    public void setHiddenParam(String name, String value) {
        setParamName(name);
        setDefaultValue(value);
        setHiden(true);
        setLoggable(false); // do not log hidden parameters
        miParamType = idTEXT;
    }

    public boolean isFake() {
        return fake;
    }

    public void setFake(boolean b) {
        fake = b;
    }

    /**
     * @return
     */
    public String getHtmlformat() {
        return htmlformat;
    }

    /**
     * @param string
     */
    public void setHtmlformat(String string) {
        htmlformat = string;
    }
    
    /**
     * this indicates wheter the function is loggable or not
     * if function has an associated HTML control that is hidden, we assume it is not loggable. 
     * see HTMLControl#setHiddenParam()
     */
    private boolean loggable = true;
    public void setLoggable(boolean value){ loggable = value; }
    public boolean isLoggable(){ return loggable; }
    
    /**
     * contais the label used for HTMLControl. Is more user-friendly than the function name or parameter name
     * therefore it is used for logging purposes
     */
    private String label = "";
    private List<ComboValue>	comboValue;
	
    public void setLabel(String value){ label = value; }
    public String getLabel(){ return label; }
    
    /**
     * Setup a select box
     * @param html
     */
    public void setupSelectBox(String html){
    	setHtmlformat(html);
	    setParamType(TSServerInfoFunction.idSingleselectcombo);
    }

	public String getAreaType() {
		return areaType;
	}

	public void setAreaType(String areaType) {
		this.areaType = areaType;
	}
	public int getControlType() {
		return controlType;
	}
	public void setControlType(int controlType) {
		this.controlType = controlType;
	}
	
	public String[] getParamValues() {
		return paramValues;
	}

	public void setParamValues(String[] paramValues) {
		this.paramValues = paramValues;
	}

	public List<ComboValue> getComboValue() {
		return comboValue;
	}

	public void setComboValue(List<ComboValue> comboValue) {
		this.comboValue = comboValue;
	}
}