package ro.cst.tsearch.servers.info;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.parentsitedescribe.ComboValue;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI.SearchType;
public class TSServerInfoModule implements Serializable, Cloneable
{
	static final long serialVersionUID = 10000001;
	public static final int idStModule = 0, idFilterModule = 1;//, idArchiveLinkModule = 2;
	/////////////////////////////////////////////////////////////////////////////////
		private String msName = "", // numele modului respectiv
		msDestinationPage = "", //action pt modulul respectiv
	msReferer = "";
	// seteaza pagina de la care se face cererea, unele situri cer asta
	private int miRequestMethod = -1, miParserID = -1, miMouleType = idStModule;
	private String separator = " ";
	private int iteratorType = ModuleStatesIterator.TYPE_DEFAULT;
	// elmarie stie??? 
	@SuppressWarnings("unused")	//kept for deserializing
	private transient int validatorType; //
	@SuppressWarnings("unused")	//kept for deserializing
	private transient Vector<Integer> validatorTypes = null;
	// eu nu cred ca stie!!!!
	@SuppressWarnings("unused")	//kept for deserializing
	private transient Vector<Integer> crossRefValidatorTypes = null;
	
	private boolean stopAfterModule = false;
	private String saObjKey = SearchAttributes.NO_KEY;
	private List<Integer> filterForNextTypes = new ArrayList<Integer>();
	@SuppressWarnings("unused")	//kept for deserializing
	private transient List<Integer> filterTypes = new ArrayList<Integer>();
	private boolean goOnNextLink = true;
	private boolean visible = true;
	private String visibleFor = "";
	
	private int moduleIdx = -1;
	private int moduleOrder = -1;
	
	private String searchType = "";
	
	private boolean multipleYears = false;
	
	private List<TSServerInfoFunction> maFunctions;
	private static final Category logger =
		Category.getInstance(TSServerInfoModule.class.getName());
	
    private SearchAttributes saForComparison = null;
    private int stringCleanerId = -1;
    
    private HTMLObject moduleParentSiteLayout = null;
	private String indexInGB="";
	private int indexNameInGB=0;
	private String typeSearchGB=null;
    //tsinterface of the server currently using this module
    private TSInterface currentSearchingServer = null; 
    private HashMap<String, Object> extraInformation = null;
	/////////////////////////////////////////////////////////////////////////////////
    
    @SuppressWarnings("unused")	//kept for deserializing
    private boolean newFilterSystem = false;
    transient private ArrayList<FilterResponse> filterList		= new  ArrayList<FilterResponse>();
    transient private ArrayList<FilterResponse> filterForNextList		= new  ArrayList<FilterResponse>();
	transient private ArrayList<DocsValidator>  validatorList 	= new ArrayList<DocsValidator>();
	transient private ArrayList<DocsValidator>  crossRefValidatorList 	= new ArrayList<DocsValidator>();
	transient private ArrayList<ModuleStatesIterator> moduleStatesItList=new ArrayList<ModuleStatesIterator>();
	transient private boolean skipModule = false;
	
    
	/**
	 * Constructor for TSServerInfoModule.
	 * @param newFilterSystem TODO
	 */
	public TSServerInfoModule(int iFunctionCount, int moduleIdx)
	{
		int i;
		this.moduleIdx = moduleIdx;
		maFunctions = new ArrayList<TSServerInfoFunction>();
		extraInformation = new HashMap<String, Object>();
		for (i = 0; i <= iFunctionCount - 1; i++)
			maFunctions.add(new TSServerInfoFunction());
	}
	/**
	 * @param orig the original module that will be the source for this new one
	 */
	public TSServerInfoModule(TSServerInfoModule orig)
	{
		this.filterList		= new  ArrayList<FilterResponse>(orig.getFilterList());
		this.filterForNextList		= new  ArrayList<FilterResponse>(orig.getFilterForNextList());
		this.moduleStatesItList=new ArrayList<ModuleStatesIterator>(orig.getModuleStatesItList());
		this.validatorList 	= new ArrayList<DocsValidator>(orig.getValidatorList());
		this.crossRefValidatorList 	= new ArrayList<DocsValidator>(orig.getCrossRefValidatorList());
		this.msName = orig.getName();
		this.msLabel = orig.getLabel();
		this.msDestinationPage = orig.getDestinationPage();
		this.msReferer = orig.getReferer();
		this.miRequestMethod = orig.getRequestMethod();
		this.miParserID = orig.getParserID();
		this.miMouleType = orig.getMouleType();
		this.iteratorType = orig.getIteratorType();
		this.goOnNextLink = orig.isGoOnNextLink();
		this.filterForNextTypes.addAll(orig.getFilterForNextTypes());
		this.separator = orig.getSeparator();
		this.moduleIdx = orig.getModuleIdx();
		this.stopAfterModule = orig.isStopAfterModule();
		this.saObjKey = orig.getSaObjKey();
		this.visible = orig.isVisible();
		this.currentSearchingServer = orig.getTSInterface();
		this.maFunctions = new ArrayList<TSServerInfoFunction>();
		this.extraInformation = new HashMap<String, Object>();
		if(orig.getAllExtraInformation()!=null)
			this.extraInformation.putAll(orig.getAllExtraInformation());
		this.indexInGB=orig.getIndexInGB();                        //helps me determin the instrument of the transfer for gb
		this.typeSearchGB=orig.getTypeSearchGB();           // helps me determin if we search with the Grantor or Grantee for GB
		this.indexNameInGB=orig.getIndexNameInGB();
		this.searchType = orig.getSearchType();
		this.visibleFor = orig.getVisibleFor();
		
		for (int i = 0; i < orig.getFunctionCount(); i++){
			this.maFunctions.add(new TSServerInfoFunction(orig.getFunction(i)));
		}
	}
	
	public synchronized Object clone()
	{
		
		try
		{
			TSServerInfoModule tssmod = (TSServerInfoModule)super.clone();
			
			try{tssmod.filterList				= new ArrayList<FilterResponse>(filterList);}catch (Exception e){}
			try{tssmod.moduleStatesItList= new ArrayList<ModuleStatesIterator>(moduleStatesItList);}catch (Exception e){}
			try{tssmod.validatorList 			= new ArrayList<DocsValidator>(validatorList);}catch (Exception e){}	
			try{tssmod.crossRefValidatorList 	= new ArrayList<DocsValidator>(crossRefValidatorList);}catch (Exception e){}
			try{tssmod.filterForNextTypes = new ArrayList<Integer>(filterForNextTypes);}catch (Exception e){}
			try{
				tssmod.maFunctions = new ArrayList<TSServerInfoFunction>();
				for (int i = 0; i < getFunctionCount(); i++){
					tssmod.maFunctions.add(new TSServerInfoFunction(getFunction(i)));
				}
			}catch (Exception e){}
			
			return tssmod;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(
				"clone() not supported for " + this.getClass().getName());
		}
	}
	
    
    public void setSaForComparison(SearchAttributes sa)
    {
        saForComparison = sa;
    }
    
    public SearchAttributes getSaForComparison()
    {
        return saForComparison;
    }
    
    public void setStringCleaner(int cleanerId)
    {
        stringCleanerId = cleanerId;
    }
    
    public int getStringCleanerId()
    {
        return stringCleanerId;
    }
    
	/**
	 * Returns the functions.
	 * 
	 * @return TSServerInfoFunction[]
	 */
	public TSServerInfoFunction getFunction(int functionIndex )
	{
		return (TSServerInfoFunction)maFunctions.get(functionIndex);
	}
	
	/**
	 * Finds the function with that parameter name
	 * @param paramName
	 * @return
	 */
	public TSServerInfoFunction getFunction(String paramName) {
		for (TSServerInfoFunction function : maFunctions) {
			if(paramName.equals(function.getName())) {
				return function;
			}
		}
		return null;
	}
	
	public void setData(int functionIndex,String value )
	{
		((TSServerInfoFunction)maFunctions.get(functionIndex)).setData(value);
	}
	
	public void setDefaultValue( int functionIndex, String defaultValue ) {
		((TSServerInfoFunction)maFunctions.get(functionIndex)).setDefaultValue(defaultValue);
    }
	
	public void setValue( int functionIndex,String value ) {
		((TSServerInfoFunction)maFunctions.get(functionIndex)).setValue(value);
    }
	
	public void forceValue( int functionIndex,String value ) {
		((TSServerInfoFunction)maFunctions.get(functionIndex)).forceValue(value);
    }
	
	public String getParamValue(int functionIndex) {
		return ((TSServerInfoFunction)maFunctions.get(functionIndex)).getParamValue();
	}
	
	public String getParamAlias(int functionIndex) {
		return ((TSServerInfoFunction)maFunctions.get(functionIndex)).getParamAlias();
	}
	
	/**
	 * Removes a function
	 * @param functionIndex
	 */	
	public void removeFunction(int functionIndex){
		maFunctions.remove(functionIndex);
	}
	/**
	 * Returns the functions.
	 * 
	 * @return TSServerInfoFunction[]
	 */
	public void setFunction(int functionIndex, TSServerInfoFunction fct)
	{
		maFunctions.set(functionIndex, fct);
	}
	
	public int addFunction()
	{
		maFunctions.add(new TSServerInfoFunction());
		return maFunctions.size() - 1;
	}
	/////////////////////////////////////////////////////////////////////////////////
	/**
	 * Returns the functionCount.
	 * 
	 * @return int
	 */
	public int getFunctionCount()
	{
		return maFunctions.size();
	}
	
	/*
	 * returns the list with functions 
	 * */
	public List<TSServerInfoFunction> getFunctionList(){
		
		return maFunctions;
		
	}
	
	/**
	 * Returns the destinationPage.
	 * 
	 * @return String
	 */
	public String getDestinationPage()
	{
		return msDestinationPage;
	}
	/**
	 * Returns the name.
	 * 
	 * @return String
	 */
	public String getName()
	{
		return msName;
	}
	/**
	 * Returns the parserID.
	 * 
	 * @return int
	 */
	public int getParserID()
	{
		return miParserID;
	}
	/**
	 * Returns the referer.
	 * 
	 * @return String
	 */
	public String getReferer()
	{
		return msReferer;
	}
	/**
	 * Returns the mouleType.
	 * 
	 * @return int
	 */
	public int getMouleType()
	{
		return miMouleType;
	}
	///////////////////////////////////////////////////////////
	/**
	 * Sets the destinationPage.
	 * 
	 * @param destinationPage
	 *            The destinationPage to set
	 */
	public void setDestinationPage(String destinationPage)
	{
		msDestinationPage = destinationPage;
	}
	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            The name to set
	 */
	public void setName(String name)
	{
		msName = name;
	}
	/**
	 * Returns the requestMethod.
	 * 
	 * @return int
	 */
	public int getRequestMethod()
	{
		return miRequestMethod;
	}
	/**
	 * Sets the requestMethod.
	 * 
	 * @param requestMethod
	 *            The requestMethod to set
	 */
	public void setRequestMethod(int requestMethod)
	{
		miRequestMethod = requestMethod;
	}
	/**
	 * Sets the parserID.
	 * 
	 * @param parserID
	 *            The parserID to set
	 */
	public void setParserID(int parserID)
	{
		miParserID = parserID;
	}
	/**
	 * Sets the referer.
	 * 
	 * @param referer
	 *            The referer to set
	 */
	public void setReferer(String referer)
	{
		msReferer = referer;
	}
	/**
	 * Sets the mouleType.
	 * 
	 * @param mouleType
	 *            The mouleType to set
	 */
	public void setMouleType(int mouleType)
	{
		miMouleType = mouleType;
	}
	/**
	 * @return
	 */
	public String getSeparator()
	{
		return separator;
	}
	/**
	 * @param string
	 */
	public void setSeparator(String string)
	{
		separator = string;
	}
	/**
	 * @return
	 */
	public int getModuleIdx()
	{
		return moduleIdx;
	}
	
	public String toString()
	{
		String s = "Module ";
		s += msName + " ( ";
		s += maFunctions;
		s += ")";
		return s;
	}
	/**
	 * @return
	 */
	public int getIteratorType()
	{
		return iteratorType;
	}
	/**
	 * @param i
	 */
	public void setIteratorType(int i)
	{
		iteratorType = i;
	}
	public boolean isEmpty()
	{
		for (int i = 0; i < maFunctions.size(); i++)
		{
			TSServerInfoFunction fct = getFunction(i);
			if (!fct.isHiden() && !fct.isEmpty() && !fct.isFake())
			{
				return false;
			}
		}
		return true;
	}
	/**
	 * @return
	 */
	public boolean isRadioModule()
	{
		if (maFunctions.size() == 0)
		{
			return false;
		}
		else
		{
			return (getFunction(0).isRadioFct());
		}
	}
	public void setData(SearchDataWrapper sd)
	{
		HttpServletRequest request = sd.getRequest();
		if (isRadioModule())
		{
			if (request != null)
			{
				getFunction(0).setData(sd);
			}
			else
			{
				setDefaultDataForRadioModule();
			}
		}
		else
		{
			for (int i = 0; i < getFunctionCount(); i++)
			{
				getFunction(i).setData(sd);
			}
		}
	}
	public void setDefaultValue(SearchAttributes sa)
	{
		//logger.debug("sa = " + sa);
		if (isRadioModule())
		{
			//setDefaultValueForRadioModule();
		}
		else
		{
			for (int i = 0; i < getFunctionCount(); i++)
			{
				getFunction(i).setDefaultValue(sa);
			}
		}
	}
	private void setDefaultDataForRadioModule()
	{
		String val = getFunction(0).getValue();
		for (int i = 0; i < getFunctionCount(); i++)
		{
			if (getFunction(i).getDefaultValue().equalsIgnoreCase("checked"))
			{
				val = getFunction(i).getValue();
			}
		}
		getFunction(0).setData(val);
	}
	public Map getParamsForQuery()
	{
		String sParamValue = "";
		Map params = new HashMap();
		if (isRadioModule())
		{
			logger.info("Sunt in if radio");
			TSServerInfoFunction function = getFunction(0);
			String crtParaValue = function.getParamValueForQuery();
			String crtParaName = function.getParamName();
			params.put(crtParaName, crtParaValue);
		}
		else
		{
			for (int i = 0;(i < getFunctionCount()); i++)
			{
				TSServerInfoFunction function = getFunction(i);
				if(function.getControlType() == 4) {	//label so we don't need it
					continue;	
				}
				String crtParaValue = function.getParamValueForQuery();
				String crtParaName = function.getParamName();
				//add space between params
				if ((sParamValue.length() != 0)
					&& (crtParaValue.length() != 0))
				{
				    // adauga virgula intre last_name si first name la cautarea dupa nume pe PATRIOTS site
				    if (crtParaName.equals("TerroristName") || crtParaName.equals("party"))
				        sParamValue += "," +separator + crtParaValue;
				    else
				        sParamValue += separator + crtParaValue;
				}
				else
				{
					sParamValue += crtParaValue;
				}
				//if length is 0 then this param is part of the next function
				// param and we will concat it's value to the next function
				// param value
				//else:
				if (crtParaName.length() != 0)
				{
					//params.put(crtParaName, sParamValue);
				    TSServerInfoParam infoParam = new TSServerInfoParam(crtParaName, sParamValue);
					params.put(infoParam, infoParam);
				    
					sParamValue = "";
				}
			}
		}
		return params;
	}

	/**
	 * @return
	 */
	public int getFilterForNextType(int i)
	{
		return (((Integer)filterForNextTypes.get(i)).intValue());
	}
	public List<Integer> getFilterForNextTypes()
	{
		return filterForNextTypes;
	}
	/**
	 * @param i
	 */
	public void addFilterForNextType(int i)
	{
		filterForNextTypes.add(new Integer(i));
	}
	/**
	 * @return
	 */
	public boolean isStopAfterModule()
	{
		return stopAfterModule;
	}
	/**
	 * @param b
	 */
	public void setStopAfterModule(boolean b)
	{
		stopAfterModule = b;
	}
	/**
	 * @return
	 */
	public String getSaObjKey()
	{
		return saObjKey;
	}
	/**
	 * @param i
	 */
	public void setSaObjKey(String s)
	{
		saObjKey = s;
	}
	
	public void setParamValue(int functionIndex, String string) {
		((TSServerInfoFunction)maFunctions.get(functionIndex)).setParamValue(string);
    }
	
	/**
     * @param string
     */
    public void setSaKey( int functionIndex, String saKey ) {
    	((TSServerInfoFunction)maFunctions.get(functionIndex)).setSaKey(saKey);
    }
	
    public void setIteratorType(int functionIndex,int iteratorType) {
    	((TSServerInfoFunction)maFunctions.get(functionIndex)).setIteratorType(iteratorType);
    }
    
	public void setupParameterAliases()
	{
		for (int i = 0;(i < getFunctionCount()); i++)
		{
			getFunction(i).setupParameterAliases(moduleIdx, i);
		}
	}
	/**
	 * @return
	 */
	public boolean isGoOnNextLink()
	{
		return goOnNextLink;
	}
	/**
	 * @param b
	 */
	public void setGoOnNextLink(boolean b)
	{
		goOnNextLink = b;
	}
	public boolean hasFakeFunctions()
	{
		for (int i = 0; i < getFunctionCount(); i++)
		{
			if (getFunction(i).isFake())
				return true;
		}
		return false;
	}
	
	
	public boolean isVisible()
	{
		return visible;
	}
	public void setVisible(boolean b)
	{
		visible = b;
	}
	
	/**
	 * make a module visible in parent site only for a category of users
	 * by choosing visible flag to False and visibleFor flag for a category of users
	 * @param visibleFor
	 */
	public void setVisibleFor(String visibleFor){
		this.visibleFor = visibleFor;
	}
	
	public String getVisibleFor(){
		return visibleFor;
	}
	
	/**
	 * @return Returns the moduleParentSiteLayout.
	 */
	public HTMLObject getModuleParentSiteLayout() {
		return moduleParentSiteLayout;
	}

	/**
	 * @param moduleParentSiteLayout The moduleParentSiteLayout to set.
	 */
	public void setModuleParentSiteLayout(HTMLObject moduleParentSiteLayout) {		
		this.moduleParentSiteLayout = moduleParentSiteLayout;
		setLabel(moduleParentSiteLayout.getLabel());		
	}
	
	/**
	 * Set the server that currently uses this module
	 * @param currentServer
	 */
	public void setTSInterface( TSInterface currentServer ){
		this.currentSearchingServer = currentServer; 
	}
	
	/**
	 * 
	 * @return current searching server
	 */
	public TSInterface getTSInterface(){
		return this.currentSearchingServer;
	}
	
	/**
	 * Add a key-value to a map. 
	 * If the key is already present, concatenate adding also a comma in between
	 * @param map
	 * @param key
	 * @param value
	 */
	private static void addVal(Map<String,String> map, String key, String value){
		key = key.trim();
		value = value.trim();
		key = key.replaceAll("(?i)<(br)>"," ");
		String initialValue = map.get(key);
		if(initialValue != null){
			value = initialValue + ", " + value;
		}
		if(!"".equals(value)){
			map.put(key, value);
		}
	}
	
	/**
	 * creates map with loggable parameter-value pairs. 
	 * if a parameter has multiple values, they appear as comma separated
	 * @return map with all loggable paramter-value pairs
	 */	
	public Map<String,String> getParamsForLog(){
		Map<String,String> retVal = new LinkedHashMap<String,String>();		
		String sParamValue = "";		
		if (isRadioModule()) {			
			TSServerInfoFunction function = getFunction(0);
			String crtParaValue = function.getParamValueForQuery();
			String crtParaName = function.getParamName();
			addVal(retVal, crtParaName, crtParaValue);			
		} else {
			for (int i = 0;(i < getFunctionCount()); i++) {
				TSServerInfoFunction function = getFunction(i);
				if(!function.isLoggable()){
					sParamValue = "";
					continue;
				}
				String crtParaValue = function.getParamValueForQuery();
				
				if((function.getControlType() == TSServerInfoFunction.idSingleselectcombo 
						|| function.getControlType() == TSServerInfoFunction.idMultipleSelectCombo)
						&& function.getComboValue() != null) {
					
					for (ComboValue comboValue : function.getComboValue()) {
						if(crtParaValue.equals(comboValue.getName())) {
							crtParaValue = comboValue.getValue();
							break;
						}
					}
					
				} else if((function.getControlType() == 1 &&	//Radio Button
						function.getComboValue() != null)) {
					
					for (ComboValue comboValue : function.getComboValue()) {
						if(crtParaValue.equals(comboValue.getValue())) {
							crtParaValue = comboValue.getName();
							break;
						}
					}
					
				} 
				
				
				String crtParaName = function.getParamName();
				String crtFuncName = function.getName();
				//add space between params
				if ((sParamValue.length() != 0) && (crtParaValue.length() != 0)){
				    // adauga virgula intre last_name si first name la cautarea dupa nume pe PATRIOTS site
				    if (crtParaName.equals("TerroristName") || crtParaName.equals("party")){
				        sParamValue += "," +separator + crtParaValue;
				    } else {
				        sParamValue += separator + crtParaValue;
				    }
				} else {
					sParamValue += crtParaValue;
				}
				//if length is 0 then this param is part of the next function
				// param and we will concat it's value to the next function
				// param value
				//else:
				if (crtParaName.length() != 0) {
					
					// decide name: first nonempty of function label, function name, parameter name
					String name = function.getLabel();
					if("".equals(name)){ name = crtFuncName; }
					if("".equals(name)){ name = crtParaName; }
					
					// decide value: add option description in case of SELECT box
					String value = sParamValue;
					sParamValue = sParamValue.replaceAll("\\|", "\\\\|");
					String htmlFormat = function.getHtmlformat();
					if(htmlFormat.toLowerCase().contains("select")){
						htmlFormat = htmlFormat.replace("\n","").replaceAll("\\s", " ").replace(" <", "<");
						Matcher m = Pattern.compile("(?i)<option value=[\"]?" + sParamValue + "[\"]?(?:\\sselected)>([^<]*)<").matcher(htmlFormat);
						if(m.find()){
							String newValue = m.group(1).trim();
							if(!"".equals(newValue)){
								value = newValue;
							}
						}else {
							Matcher m1 = Pattern.compile("(?ism)<option[^>]*?value=(?:\"|')" + sParamValue + "(?:\"|').*?>(.*?)</option>").matcher(htmlFormat);
							if(m1.find()){
								String newValue = m1.group(1).trim();
								if(!"".equals(newValue)){
									value = newValue;
								}
							}
						}
					}
					// store name-value pair
					addVal(retVal, name, value);
					sParamValue = "";
				}
			}
		}		
		return retVal;
	}
	
	public String getUniqueKey() {
		StringBuilder key = new StringBuilder();
		key.append("ModuleId=").append(getModuleIdx());
		key.append("ModuleName=").append(getName());
		for (int i = 0;(i < getFunctionCount()); i++) {
			TSServerInfoFunction function = getFunction(i);
			if(!function.isLoggable()){
				continue;
			}
			key.append(function.getParamName()).append("=").append(function.getParamValue());
			
		}
		return key.toString();
	}
	
	/**
	 * Clear all search attribute keys 
	 */
	@SuppressWarnings("unchecked")
	public void clearSaKeys(){
		for(TSServerInfoFunction function: (List<TSServerInfoFunction>)maFunctions ){
			function.setSaKey("");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void clearIteratorTypes(){
		for(TSServerInfoFunction function: (List<TSServerInfoFunction>)maFunctions ){
			function.setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_DEFAULT);
		}
	}
	
	private String msLabel = "";

	public String getLabel() {
		if(msLabel == null){
			return getName();
		}
		return msLabel;
	}

	public void setLabel(String label) {
		this.msLabel = label;
	}
	public String getIndexInGB() {
		return indexInGB;
	}
	public void setIndexInGB(String indexInGB) {
		this.indexInGB = indexInGB;
	}
	public String getTypeSearchGB() {
		return typeSearchGB;
	}
	public void setTypeSearchGB(String typeSearchGB) {
		this.typeSearchGB = typeSearchGB;
	}
	public int getIndexNameInGB() {
		return indexNameInGB;
	}
	public void setIndexNameInGB(int indexNameInGB) {
		this.indexNameInGB = indexNameInGB;
	}
	
	public Object addExtraInformation(String key, Object info){
		return extraInformation.put(key, info);
	}
	public Object clearExtraInformation(String key){
		return extraInformation.remove(key);
	}
	public Object getExtraInformation(String key){
		return extraInformation.get(key);
	}
	public HashMap<String, Object> getAllExtraInformation(){
		return extraInformation;
	}
	
	public void addFilter(FilterResponse fr) {
		if(fr == null){
			return;
		}
		filterList.add(fr);
	}

	public void addFilters(FilterResponse... filters) {
		for(FilterResponse fr : filters){
			addFilter(fr);
		}
	}

	public void removeFilter(FilterResponse fr) {
		filterList.remove(fr);
	}
	
	public void removeFilter(int index) {
		filterList.remove(index);
	}
	
	public void addValidators(DocsValidator... validators) {
		for(DocsValidator validator: validators){
			validatorList.add(validator);
		}
	}

	public void addValidator(DocsValidator fr) {
		if( fr == null ){
			return;
		}
		validatorList.add(fr);
	}
	
	public void removeValidator(DocsValidator fr) {
		validatorList.remove(fr);
	}
	
	public void removeValidator(int index) {
		validatorList.remove(index);
	}
	
	public void addIterator(ModuleStatesIterator msi) {
		if(msi == null){
			return;
		}
		moduleStatesItList.add((msi));
	}
	public ArrayList<FilterResponse> getFilterList() {
		return filterList;
	}
	
	public void setFilterList(ArrayList<FilterResponse> filterList) {
		this.filterList = filterList;
	}
	
	public ArrayList<DocsValidator> getValidatorList() {
		return validatorList;
	}
	public void setValidatorList(ArrayList<DocsValidator> validatorList) {
		this.validatorList = validatorList;
	}
	public ArrayList<DocsValidator> getCrossRefValidatorList() {
		return crossRefValidatorList;
	}
	
	public void setCrossRefValidatorList(ArrayList<DocsValidator> crossRefValidatorList) {
		this.crossRefValidatorList = crossRefValidatorList;
	}
	
	public void addCrossRefValidator(DocsValidator fr) {
		if(fr == null){
			return;
		}
		crossRefValidatorList.add(fr);
	}
	
	public void addCrossRefValidators(DocsValidator... validators) {
		for(DocsValidator validator: validators){
			crossRefValidatorList.add(validator);
		}
	}
	
	public void removeCrossRefValidator(DocsValidator fr) {
		crossRefValidatorList.remove(fr);
	}
	
	public void removeCrossRefValidator(int index) {
		crossRefValidatorList.remove(index);
	}
	public ArrayList<ModuleStatesIterator> getModuleStatesItList() {
		return moduleStatesItList;
	}
	public void setModuleStatesItList(
			ArrayList<ModuleStatesIterator> moduleStatesItList) {
		this.moduleStatesItList = moduleStatesItList;
	}
	public void clearSaKey(int functionIndex){
		((TSServerInfoFunction)maFunctions.get(functionIndex)).setSaKey("");
	}
	
	public void clearFunction(int functionIndex){
		this.clearSaKey(functionIndex);
        this.setParamValue(functionIndex, "");
        this.setDefaultValue(functionIndex, "");
	}
	
	public void addFilterForNext(FilterResponse fr) {
		if(fr == null){
			return;
		}
		fr.setForNext(true);
		filterForNextList.add(fr);
	}
	
	public void removeFilterForNext(FilterResponse fr) {
		filterForNextList.remove(fr);
	}
	
	public void removeFilterForNext(int index) {
		filterForNextList.remove(index);
	}
	public ArrayList<FilterResponse> getFilterForNextList() {
		return filterForNextList;
	}
	
	public String getMsName() {
		return msName;
	}
	
	public int getModuleOrder() {
		return moduleOrder;
	}
	public void setModuleOrder(int moduleOrder) {
		this.moduleOrder = moduleOrder;
	}
	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}
	public String getSearchType() {
		return searchType;
	}
	
	/**
	 * set if a certain module must search an retain documents for more than one year
	 * @param multipleYears
	 */
	public void setMutipleYears(boolean multipleYears){
		this.multipleYears = multipleYears;
	}
	
	public boolean getMultipleYears(){
		return multipleYears;
	}
	
	/**
	 * Tries to get the current SearchType if this module is used in automatic<br>
	 * If the <code>currentSearchingServer</code> is not available it will return null<br>
	 * Also tries to determine what is the default value in case there is nothing specific available
	 * @return current SearchType or null if not in automatic
	 */
	public SearchType getSearchTypeCompleteInAutomatic() {
		if(currentSearchingServer == null) {
			return null;
		}
		if(StringUtils.isNotEmpty(getSearchType())) {
			return SearchType.valueOf(getSearchType());
		}
		DataSite dataSite = currentSearchingServer.getDataSite();
		if(getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && !Bridge.isRegisterLikeSite(dataSite.getSiteTypeAbrev())) {
			return SearchType.GT;
		} else {
			SearchType type = TSServerInfo.getSearchTypeByModuleMap().get(getModuleIdx());
			if(type != null) {
				return type;
			}
		}
		
		return SearchType.NA;
	}
		
	/**
	 * if is true, the module is skipped in automatic search
	 * @param skipModule
	 */
	public void setSkipModule(boolean skipModule) {
		this.skipModule = skipModule;
	}
	
	public boolean isSkipModule() {
		return skipModule;
	}
}
