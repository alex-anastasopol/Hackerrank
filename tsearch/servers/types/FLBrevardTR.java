package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLBrevardTR extends FLGenericGovernmaxTR {
	
	private static final long serialVersionUID = 6525099447955957961L;
	
	private static final String PIN_FORMAT = "(\\w+)-(\\w+)-(\\w+)-(\\w+)-([\\w.]+)-([\\w.]+)";

	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){
			String linkText = getLinkText(row);
			return linkText.matches("[A-Z]+[0-9-]+.*");
		}
	};
	
	public FLBrevardTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;	
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();

		try {

			TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);

			if (hasPin()) {
				String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
				String cleanedPin = pin.replaceAll("[-,]", "");
				Matcher matcher = Pattern.compile(PIN_FORMAT).matcher(pin);
				if (pin.length() == 22 || matcher.matches()) {
					
					String twp = "";
					String rng = "";
					String sec = "";
					String subn = "";
					String blk = "";
					String lot = "";
					
					if (cleanedPin.length() == 22) {		//PIN bootstrapped from NB, e.g. 253723DN00000.00020.00
						twp = cleanedPin.substring(0, 2);
						rng = cleanedPin.substring(2, 4);
						sec = cleanedPin.substring(4, 6);
						subn = cleanedPin.substring(6, 8);
						blk = cleanedPin.substring(8, 15);
						lot = cleanedPin.substring(15);
					} else if (matcher.matches()) {			//PIN from Search Page, e.g. 20G-34-02-AI-00001.0-0004.01
						twp = matcher.group(1);
						rng = matcher.group(2);
						sec = matcher.group(3);
						subn = matcher.group(4);
						blk = matcher.group(5);
						lot = matcher.group(6);
					}
					
					blk = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(blk);
					blk = blk.replaceAll("\\.0$", "");
					blk = blk.replaceAll("\\.", "");

					lot = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(lot);
					lot = lot.replaceAll("\\.00$", "");
					lot = lot.replaceAll("\\.", "");
					
					String geo_number = twp + rng + sec + subn + blk + lot;
					
					if (!StringUtils.isEmpty(geo_number)) {
						if (tsServerInfoModule != null) {
							PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
							for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
								String paramName = htmlControl.getCurrentTSSiFunc().getParamName();
								if ("geo_number".equals(paramName)) {
									htmlControl.setDefaultValue(geo_number);
								} 
							}
						}
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule module;
			
			String address = getSearchAttribute(SearchAttributes.P_STREET_FULL_NAME_EX);
			FilterResponse rejectNonRealEstate = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
			rejectNonRealEstate.setThreshold(new BigDecimal("0.65"));
			
			
			// P1 : search by PIN - in case the user has input directly the TR PIN		
			/*if(hasPin()){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
				module.addFilter(rejectNonRealEstate);
				modules.add(module);		
			}*/
			
			// PX : search by GEO - from the PIN 
			String geo = getSearchAttribute(SearchAttributes.LD_PARCELNO);		
			if(!StringUtils.isEmpty(geo)){
				
				String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
				String cleanedPin = pin.replaceAll("[-,]", "");
				Matcher matcher = Pattern.compile(PIN_FORMAT).matcher(pin);
				if (pin.length() == 22 || matcher.matches()) {
					
					String twp = "";
					String rng = "";
					String sec = "";
					String subn = "";
					String blk = "";
					String lot = "";
					
					if (cleanedPin.length() == 22) {		//PIN bootstrapped from NB, e.g. 253723DN00000.00020.00
						twp = cleanedPin.substring(0, 2);
						rng = cleanedPin.substring(2, 4);
						sec = cleanedPin.substring(4, 6);
						subn = cleanedPin.substring(6, 8);
						blk = cleanedPin.substring(8, 15);
						lot = cleanedPin.substring(15);
					} else if (matcher.matches()) {			//PIN from Search Page, e.g. 20G-34-02-AI-00001.0-0004.01
						twp = matcher.group(1);
						rng = matcher.group(2);
						sec = matcher.group(3);
						subn = matcher.group(4);
						blk = matcher.group(5);
						lot = matcher.group(6);
					}
					
					blk = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(blk);
					blk = blk.replaceAll("\\.0$", "");
					blk = blk.replaceAll("\\.", "");

					lot = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(lot);
					lot = lot.replaceAll("\\.00$", "");
					lot = lot.replaceAll("\\.", "");
					
					geo = twp + rng + sec + subn + blk + lot;
				}
				
				if (!StringUtils.isEmpty(geo)) {
					PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
					Set<String> pins = new HashSet<String>();
					pins.add(geo);
					pinFilter.setParcelNumber(pins);
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
					module.clearSaKeys();
					module.forceValue(0, geo);  
					module.addFilter(rejectNonRealEstate);
					module.addFilter(pinFilter);
					modules.add(module);
				}
				
			}
			
			// PX : search by Address	
			if(hasAddress()){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).setSaKey(SearchAttributes.P_STREET_FULL_NAME_EX);
				module.addFilter(rejectNonRealEstate);
				module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
				modules.add(module);		
			}
			
			// PX : search by Address; for B 4155	
			if(hasAddress()){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				address = address.replaceAll("(?is)(\\d+)\\s+[SWEN]+\\s+(.*)", "$1 %$2");
				module.setData(0, address);
				module.addFilter(rejectNonRealEstate);
				module.addFilter(new AddressFilterResponse2("",searchId));
				modules.add(module);		
			}
			
			// PX : search by Owner Name	
			if(hasName()){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.addFilter(rejectNonRealEstate);
				
				GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.OWNER_OBJECT, searchId, module);
				nameFilter.setUseSynonymsForCandidates(false);
				module.addFilter(nameFilter);
				
				if(hasLegal()){
					module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
				}
				module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L, F M;;","L, F;;","L, f;;"});
				
				module.addIterator(nameIterator);
				
				modules.add(module);		
			}
		
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	protected String getKeyNumberFromGEONumber(String page, NodeList nodeList, ServerResponse serverResponse) {
		String keyNumber = "";
		// for non real estate properties bootstrapping will be skipped
		// and Account No. will be used instead of GEO No.
		String taxType = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Tax Type"), "", false).trim();
		if (StringUtils.isNotEmpty(taxType) && StringUtils.isEmpty(RegExUtils.getFirstMatch("(?i)\\b(Real\\s+Estate)", taxType, 1))) {
			keyNumber = getKeyNumber(page);
			serverResponse.getParsedResponse().setAttribute(ParsedResponse.SKIP_BOOTSTRAP, true);
		} else {
			keyNumber = super.getKeyNumberFromGEONumber(page, nodeList, serverResponse);
		}
		return keyNumber;
	}
}
