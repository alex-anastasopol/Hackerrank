package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.ISIConn;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
 */
public class ILKaneIS extends GenericISI {

	public static final long serialVersionUID = 10000000L;
	
	public ILKaneIS(long searchId) {
		super(searchId);
	}

	public ILKaneIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	private FilterResponse getAddressFilter(){
		return new AddressFilterResponse2(searchId);
	}
	
	private FilterResponse getMultiPinFilter(){
		return new MultiplePinFilterResponse(searchId);
	}
	
	private FilterResponse getNameFilter(TSServerInfoModule module){
		return NameFilterFactory.getHybridNameFilter(module.getSaObjKey(), searchId, module);
	}

	private FilterResponse getOwnerCondoFilter(TSServerInfoModule module){
		FilterResponse filter = getNameFilter(module);
		filter.setMinRowsToActivate(10);
		return filter;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
	
		// P0 - search by multiple PINs
		Collection<String> pins = getSearchAttributes().getPins(-1);
		if(pins.size() > 1){			
			for(String pin: pins){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(7).forceValue(pin);
				modules.add(module);	
			}			
			// set list for automatic search 
			serverInfo.setModulesForAutoSearch(modules);
			resultType = MULTIPLE_RESULT_TYPE;
			return;
		}
			
		// load relevant attributes
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);		
		String stNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String stName = getSearchAttribute(SearchAttributes.P_STREETNAME);	
		String stDir = getSearchAttribute(SearchAttributes.P_STREETDIRECTION);					
		String ownLast = getSearchAttribute(SearchAttributes.OWNER_LNAME);
		String city = getSearchAttribute(SearchAttributes.P_CITY);
		
		// make decisions
		boolean hasAddress = !StringUtils.isEmpty(stNo) && !StringUtils.isEmpty(stName);
		boolean hasPin  = !StringUtils.isEmpty(pin);		
		boolean hasOwner = !StringUtils.isEmpty(ownLast);
		
		// count criteria
		int critCount = 0;
		if(hasAddress){ critCount++; }
		if(hasPin){ critCount++; }
		if(hasOwner){ critCount++; }
		
		// P1 - search by everything we've got, no filtering
		if(critCount > 1){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKey(32);
			module.addFilter(getMultiPinFilter());
			module.forceValue(32, ownLast);//B 4554
			modules.add(module);			
		}
		
		// P2 - search by PIN
		if(hasPin){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(7).setSaKey(SearchAttributes.LD_PARCELNO);  
			modules.add(module);		
		}
		
		// P3 - search by address
        if(hasAddress){
        	
        	Collection<String> cities = new LinkedHashSet<String>();
        	cities.add(city); // try first with city
        	cities.add("");   // then without city
        	
        	Collection<String> directions = new LinkedHashSet<String>();
        	directions.add(stDir); // try first with direction
        	if(stDir.length() > 0){
        		directions.add(stDir.substring(0,1));
        	}
        	directions.add("");    // then without direction
        	
        	for(String cit: cities){
        		for(String dir: directions){
        			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
        			module.clearSaKeys();		
        			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
        			module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
        			module.getFunction(1).setSaKey(SearchAttributes.P_STREETNO);       			
        			module.getFunction(3).setSaKey(SearchAttributes.P_STREETNAME);	        
        			module.getFunction(6).setSaKey(SearchAttributes.P_STREETUNIT);
        			module.getFunction(2).forceValue(dir);
        			module.getFunction(21).forceValue(cit);		
        			module.addFilter(getAddressFilter());
        			module.addFilter(getOwnerCondoFilter(module));
        			module.addFilter(getMultiPinFilter());	
        			modules.add(module);        			
        		}
        	}
        }
        
        // P4 - search by Owner
        if(hasOwner && hasAddress ){
					
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			FilterResponse nameFilterHybridDoNotSkipUnique = getNameFilter(module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			module.clearSaKeys();
			module.addFilter(getAddressFilter());
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(getMultiPinFilter());	
			module.getFunction(3).setSaKey(SearchAttributes.P_STREETNAME);	
			module.forceValue(8, ownLast);
			
			modules.add(module);	
		}

        // P5 - search by Owner
        if(hasOwner && hasAddress ){
					
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			FilterResponse nameFilterHybridDoNotSkipUnique = getNameFilter(module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			module.clearSaKeys();
			module.addFilter(getAddressFilter());
			
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(getMultiPinFilter());	
			module.setIteratorType(8,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);	
		}
		
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch(modules);		
	}	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		String county = getSearch().getSa().getCountyName();
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
			
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
			tsServerInfoModule.setData(10, ISIConn.DDL_COUNTY.get(county));
			tsServerInfoModule.setDefaultValue(10, ISIConn.DDL_COUNTY.get(county));
		}
			
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(22), ZIP_SELECT);	            
	        setupSelectBox(tsServerInfoModule.getFunction(38), LAND_USE_SELECT);	            
	        setupSelectBox(tsServerInfoModule.getFunction(39), SCHOOL_SELECT);
	        setupSelectBox(tsServerInfoModule.getFunction(40), EXEMPTION_SELECT);
	        tsServerInfoModule.getFunction(35).setParamType(TSServerInfoFunction.idCheckBox);
	        tsServerInfoModule.getFunction(35).setHtmlformat(
	        	"<INPUT TYPE=\"Checkbox\" NAME=\"cbOutOfStateOwners\" CHECKED VALUE=\"P\">");
	        setupSelectBox(tsServerInfoModule.getFunction(50), MONTH_SELECT.replace("@@NAME@@", "ddlRecMonthFrom"));
	        setupSelectBox(tsServerInfoModule.getFunction(51), MONTH_SELECT.replace("@@NAME@@", "ddlRecMonthTo"));
	        setupSelectBox(tsServerInfoModule.getFunction(56), MONTH_SELECT.replace("@@NAME@@", "ddlSaleDateFrom"));
	        setupSelectBox(tsServerInfoModule.getFunction(57), MONTH_SELECT.replace("@@NAME@@", "ddlSaleDateTo"));
	        setupSelectBox(tsServerInfoModule.getFunction(67), MORT_TYPE_SELECT.replace("@@NAME@@", "ddlMortType"));
	        setupSelectBox(tsServerInfoModule.getFunction(68), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonFrom"));
	        setupSelectBox(tsServerInfoModule.getFunction(69), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonTo"));
	        setupSelectBox(tsServerInfoModule.getFunction(79), MORT_TYPE_SELECT.replace("@@NAME@@", "ddlMortType2"));
	        setupSelectBox(tsServerInfoModule.getFunction(80), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonFrom2"));
	        setupSelectBox(tsServerInfoModule.getFunction(81), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonTo2"));
	        setupSelectBox(tsServerInfoModule.getFunction(99), TOWNSHIP_SELECT);
	        tsServerInfoModule.setData(101, ISIConn.DDL_COUNTY.get(county));
	        tsServerInfoModule.setDefaultValue(101, ISIConn.DDL_COUNTY.get(county));
	
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	private static final String ZIP_SELECT =
		"<select name=\"ddlZip\" size=\"1\">" +
		"<option value=\" No Selection \"> No Selection </option>" +
		"<option value=\"33487\">33487</option>" +
		"<option value=\"60002\">60002</option>" +
		"<option value=\"60005\">60005</option>" +
		"<option value=\"60007\">60007</option>" +
		"<option value=\"60010\">60010</option>" +
		"<option value=\"60013\">60013</option>" +
		"<option value=\"60014\">60014</option>" +
		"<option value=\"60015\">60015</option>" +
		"<option value=\"60025\">60025</option>" +
		"<option value=\"60030\">60030</option>" +
		"<option value=\"60031\">60031</option>" +
		"<option value=\"60035\">60035</option>" +
		"<option value=\"60041\">60041</option>" +
		"<option value=\"60045\">60045</option>" +
		"<option value=\"60047\">60047</option>" +
		"<option value=\"60048\">60048</option>" +
		"<option value=\"60050\">60050</option>" +
		"<option value=\"60060\">60060</option>" +
		"<option value=\"60070\">60070</option>" +
		"<option value=\"60073\">60073</option>" +
		"<option value=\"60085\">60085</option>" +
		"<option value=\"60089\">60089</option>" +
		"<option value=\"60093\">60093</option>" +
		"<option value=\"60099\">60099</option>" +
		"<option value=\"60101\">60101</option>" +
		"<option value=\"60102\">60102</option>" +
		"<option value=\"60103\">60103</option>" +
		"<option value=\"60106\">60106</option>" +
		"<option value=\"60107\">60107</option>" +
		"<option value=\"60108\">60108</option>" +
		"<option value=\"60109\">60109</option>" +
		"<option value=\"60110\">60110</option>" +
		"<option value=\"60115\">60115</option>" +
		"<option value=\"60118\">60118</option>" +
		"<option value=\"60119\">60119</option>" +
		"<option value=\"60120\">60120</option>" +
		"<option value=\"60121\">60121</option>" +
		"<option value=\"60123\">60123</option>" +
		"<option value=\"60124\">60124</option>" +
		"<option value=\"60126\">60126</option>" +
		"<option value=\"60130\">60130</option>" +
		"<option value=\"60133\">60133</option>" +
		"<option value=\"60134\">60134</option>" +
		"<option value=\"60135\">60135</option>" +
		"<option value=\"60136\">60136</option>" +
		"<option value=\"60139\">60139</option>" +
		"<option value=\"60140\">60140</option>" +
		"<option value=\"60142\">60142</option>" +
		"<option value=\"60144\">60144</option>" +
		"<option value=\"60147\">60147</option>" +
		"<option value=\"60148\">60148</option>" +
		"<option value=\"60151\">60151</option>" +
		"<option value=\"60152\">60152</option>" +
		"<option value=\"60154\">60154</option>" +
		"<option value=\"60156\">60156</option>" +
		"<option value=\"60170\">60170</option>" +
		"<option value=\"60172\">60172</option>" +
		"<option value=\"60174\">60174</option>" +
		"<option value=\"60175\">60175</option>" +
		"<option value=\"60177\">60177</option>" +
		"<option value=\"60178\">60178</option>" +
		"<option value=\"60183\">60183</option>" +
		"<option value=\"60184\">60184</option>" +
		"<option value=\"60185\">60185</option>" +
		"<option value=\"60187\">60187</option>" +
		"<option value=\"60188\">60188</option>" +
		"<option value=\"60190\">60190</option>" +
		"<option value=\"60192\">60192</option>" +
		"<option value=\"60305\">60305</option>" +
		"<option value=\"60402\">60402</option>" +
		"<option value=\"60410\">60410</option>" +
		"<option value=\"60411\">60411</option>" +
		"<option value=\"60433\">60433</option>" +
		"<option value=\"60435\">60435</option>" +
		"<option value=\"60436\">60436</option>" +
		"<option value=\"60439\">60439</option>" +
		"<option value=\"60441\">60441</option>" +
		"<option value=\"60446\">60446</option>" +
		"<option value=\"60447\">60447</option>" +
		"<option value=\"60453\">60453</option>" +
		"<option value=\"60490\">60490</option>" +
		"<option value=\"60491\">60491</option>" +
		"<option value=\"60502\">60502</option>" +
		"<option value=\"60503\">60503</option>" +
		"<option value=\"60504\">60504</option>" +
		"<option value=\"60505\">60505</option>" +
		"<option value=\"60506\">60506</option>" +
		"<option value=\"60507\">60507</option>" +
		"<option value=\"60510\">60510</option>" +
		"<option value=\"60511\">60511</option>" +
		"<option value=\"60512\">60512</option>" +
		"<option value=\"60515\">60515</option>" +
		"<option value=\"60516\">60516</option>" +
		"<option value=\"60517\">60517</option>" +
		"<option value=\"60518\">60518</option>" +
		"<option value=\"60520\">60520</option>" +
		"<option value=\"60532\">60532</option>" +
		"<option value=\"60534\">60534</option>" +
		"<option value=\"60537\">60537</option>" +
		"<option value=\"60538\">60538</option>" +
		"<option value=\"60539\">60539</option>" +
		"<option value=\"60540\">60540</option>" +
		"<option value=\"60542\">60542</option>" +
		"<option value=\"60543\">60543</option>" +
		"<option value=\"60544\">60544</option>" +
		"<option value=\"60545\">60545</option>" +
		"<option value=\"60549\">60549</option>" +
		"<option value=\"60550\">60550</option>" +
		"<option value=\"60554\">60554</option>" +
		"<option value=\"60555\">60555</option>" +
		"<option value=\"60559\">60559</option>" +
		"<option value=\"60560\">60560</option>" +
		"<option value=\"60561\">60561</option>" +
		"<option value=\"60563\">60563</option>" +
		"<option value=\"60564\">60564</option>" +
		"<option value=\"60585\">60585</option>" +
		"<option value=\"60586\">60586</option>" +
		"<option value=\"60607\">60607</option>" +
		"<option value=\"60608\">60608</option>" +
		"<option value=\"60612\">60612</option>" +
		"<option value=\"60617\">60617</option>" +
		"<option value=\"60626\">60626</option>" +
		"<option value=\"60655\">60655</option>" +
		"<option value=\"60657\">60657</option>" +
		"<option value=\"60660\">60660</option>" +
		"<option value=\"60661\">60661</option>" +
		"<option value=\"60712\">60712</option>" +
		"<option value=\"60804\">60804</option>" +
		"<option value=\"60901\">60901</option>" +
		"<option value=\"61016\">61016</option>" +
		"<option value=\"61061\">61061</option>" +
		"<option value=\"61356\">61356</option>" +
		"<option value=\"61358\">61358</option>" +
		"<option value=\"61401\">61401</option>" +
		"<option value=\"61428\">61428</option>" +
		"<option value=\"61604\">61604</option>" +
		"<option value=\"61920\">61920</option>" +
		"<option value=\"62054\">62054</option>" +
		"<option value=\"62634\">62634</option>" +
	    "</select>";
	
	private static final String LAND_USE_SELECT = 
		"<select name=\"ddlLandUseCodes\" size=\"1\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"''\">NO CODE - ''</option>" +
		"<option value=\"0011\">RURAL PROP IMP W/BLDGS - 0011</option>" +
		"<option value=\"0021\">RURAL PROP NOT IMPR W/BLDGS - 0021</option>" +
		"<option value=\"0030\">RESIDENTIAL VACANT LAND - 0030</option>" +
		"<option value=\"0032\">RESIDENTIAL VACANT LAND (20G - 0032</option>" +
		"<option value=\"0040\">RESIDENTIAL 6 OR LESS UNITS - 0040</option>" +
		"<option value=\"0041\">RESIDENTIAL MODEL HOME - 0041</option>" +
		"<option value=\"0050\">COMML RESIDENT 6+ UNITS (20G - 0050</option>" +
		"<option value=\"0052\">COMML VAC LAND 6+ UNITS (20G - 0052</option>" +
		"<option value=\"0060\">COMMERCIAL BUSINESS - 0060</option>" +
		"<option value=\"0062\">COMML VAC LAND FOR BUS (20G4 - 0062</option>" +
		"<option value=\"0070\">COMMERCIAL OFFICE - 0070</option>" +
		"<option value=\"0072\">COMML VAC LAND FOR OFC (20G4 - 0072</option>" +
		"<option value=\"0080\">INDUSTRIAL PROPERTY - 0080</option>" +
		"<option value=\"0082\">INDUSTRIAL VACANT LAND (20G4 - 0082</option>" +
		"<option value=\"5040\">NON-CARRIER RAILROAD - 5040</option>" +
		"<option value=\"5050\">NON-CARRIER RAILROAD - 5050</option>" +
		"<option value=\"5060\">NON-CARRIER RAILROAD - 5060</option>" +
		"<option value=\"5070\">NON-CARRIER RAILROAD - 5070</option>" +
		"<option value=\"5080\">NON-CARRIER RAILROAD - 5080</option>" +
		"<option value=\"8000\">EXEMPT - 8000</option>" +
		"<option value=\"8011\">PARTIAL EXEMPT - 8011</option>" +
		"<option value=\"8021\">PARTIAL EXEMPT - 8021</option>" +
		"<option value=\"8030\">PARTIAL EXEMPT - 8030</option>" +
		"<option value=\"8040\">PARTIAL EXEMPT - 8040</option>" +
		"<option value=\"8050\">PARTIAL EXEMPT - 8050</option>" +
		"<option value=\"8060\">PARTIAL EXEMPT - 8060</option>" +
		"<option value=\"8070\">PARTIAL EXEMPT - 8070</option>" +
		"<option value=\"8080\">PARTIAL EXEMPT - 8080</option>" +
		"<option value=\"9000\">RAILROAD - 9000</option>"+
		"</select>";
	
	private static final String SCHOOL_SELECT = 
		"<select name=\"ddlSchool\" size=\"1\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"131\">AURORA EAST U SCH DIS 131</option>" +
		"<option value=\"129\">AURORA WEST U SCH DIS 129</option>" +
		"<option value=\"220\">BARRINGTON C U SCH DIS 220</option>" +
		"<option value=\"101\">BATAVIA UNIT SCH DIS 101</option>" +
		"<option value=\"301\">CENTRAL COMM U SCH DIS 301</option>" +
		"<option value=\"300\">COMM UNIT SCH DIS 300</option>" +
		"<option value=\"304\">GENEVA COMM UNIT SCH DIS 304</option>" +
		"<option value=\"429\">HINKLEY BIG ROCK C U SD 429</option>" +
		"<option value=\"158\">HUNTLEY CONS SCH DIS 158</option>" +
		"<option value=\"302\">KANELAND C U SCH DIS 302</option>" +
		"<option value=\"308\">OSWEGO SCH DIS 308</option>" +
		"<option value=\"U46\">SCH DIS 46</option>" +
		"<option value=\"303\">ST CHARLES C U SCH DIS 303</option>" +
		"<option value=\"427\">SYCAMORE C U SCH DIS 427</option>" +
		"<option value=\"115\">YORKVILLE C U SCH DIS 115</option>" +
		"</select>";
	
	private static final String TOWNSHIP_SELECT = 
		"<select name=\"ddlTownship\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"AU\">AURORA</option>" +
		"<option value=\"BA\">BATAVIA</option>" +
		"<option value=\"BR\">BIG ROCK</option>" +
		"<option value=\"BB\">BLACKBERRY</option>" +
		"<option value=\"BU\">BURLINGTON</option>" +
		"<option value=\"CA\">CAMPTON</option>" +
		"<option value=\"DU\">DUNDEE</option>" +
		"<option value=\"EL\">ELGIN</option>" +
		"<option value=\"GE\">GENEVA</option>" +
		"<option value=\"HA\">HAMPSHIRE</option>" +
		"<option value=\"KA\">KANEVILLE</option>" +
		"<option value=\"PL\">PLATO</option>" +
		"<option value=\"RU\">RUTLAND</option>" +
		"<option value=\"SC\">ST CHARLES</option>" +
		"<option value=\"SG\">SUGAR GROVE</option>" +
		"<option value=\"VI\">VIRGIL</option>" +
		"</select>";
	
}
