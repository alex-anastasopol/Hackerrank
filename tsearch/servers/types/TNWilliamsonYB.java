package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;

/**
 * 
 * @author Olivia
 *
 * May 22, 2014
 */

public class TNWilliamsonYB extends TNWilliamsonTR {
	//Brentwood
	

	private static final long	serialVersionUID	= -3766868339757574564L;

	public TNWilliamsonYB(long searchId) {
		super(searchId);
		
		if (numberOfYearsAllowed > 1) {
			resultType = MULTIPLE_RESULT_TYPE;
		}
	}
	
	public TNWilliamsonYB(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		
		if (numberOfYearsAllowed > 1) {
			resultType = MULTIPLE_RESULT_TYPE;
		}
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		
		if(StringUtils.isEmpty(city))
			return;
		
		if (!StringUtils.isEmpty(city)) {
			if (!city.startsWith(getDataSite().getCityName().toUpperCase())) {
				return;
			}
		}

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String selectYearListAsHtml = getYearSelect("param_0_4", "param_0_4");  //for TaxYear select list
			
		String address = getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME);
		DocsValidator addressValidator = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d).getValidator();
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
			
		if (Search.AUTOMATIC_SEARCH == searchType) {
			// in automatic, default value = All; 
			// in PS default selection is 2013, as it is on official site
			String regExp = "(?is)(.*\\\"All\\\")";
			selectYearListAsHtml = selectYearListAsHtml.replaceFirst("selected ", "");
			selectYearListAsHtml = selectYearListAsHtml.replaceFirst(regExp, "$1 selected");
		}
			
		if (hasPin()) {
			// Search by PIN
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKey(9);
				module.getFunction(9).forceValue("All");
				module.addFilter(taxYearFilter);
				modules.add(module);
			}
			{
				String alternatePinFromPRI = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO2_ALTERNATE);
				if (StringUtils.isNotBlank(alternatePinFromPRI)) {
					String sidValue = alternatePinFromPRI.substring(0, alternatePinFromPRI.indexOf("-"));
					sidValue = StringUtils.leftPad(sidValue, 3, "0");
					String idValue = getSearch().getSa().getAtribute(SearchAttributes.LD_TN_WILLIAMSON_YB_Id);
					
					{
						//if no SI value is available, take it from alternate APN, 
						// e.g: PRI doc has APN/PIN: 054B-E-050.00, Alternate APN/PIN: 15-054B-E-054B-050.00  => use 015 as SI
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
						module.clearSaKey(9);
						module.getFunction(9).forceValue("All");
						module.clearSaKey(5);
						module.getFunction(5).forceValue(sidValue);
						if (StringUtils.isBlank(idValue)) {
							module.clearSaKey(4);
							module.getFunction(4).forceValue("000");
						}
						module.addFilter(taxYearFilter);
						modules.add(module);
					}
					
					{
						//special cases when ID should take SI value, e.g: PRI Alternate APN = 08-053D-A-053C-001.00-C-131
						Matcher altAPNmatch = Pattern.compile("(\\d+)-(\\d{3}[A-Z]?)-([A-Z])-(\\d{3}[A-Z]?)-(\\d{3}\\.\\d{2})-([A-Z])-(\\d{1,3})")
							.matcher(alternatePinFromPRI);
						if (altAPNmatch.matches()) {
							String newIDvalue = altAPNmatch.group(7);
							newIDvalue = StringUtils.leftPad(newIDvalue, 3, "0");
							module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
							module.clearSaKey(9);
							module.getFunction(9).forceValue("All");
							module.clearSaKey(4);
							module.getFunction(4).forceValue(newIDvalue);
							module.clearSaKey(5);
							module.getFunction(5).forceValue(sidValue);
							module.addFilter(taxYearFilter);
							modules.add(module);
						}
					}
					
				}
			}
			
		}
				
		if (hasStreet()) {
			// Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(4).forceValue("All");
			module.getFunction(0).forceValue(address);
			module.addFilter(defaultNameFilter);
			module.addFilter(taxYearFilter);
			module.addValidator(addressValidator);
			modules.add(module);
		}
				
		if (hasOwner()) {
			// Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();
			module.getFunction(4).forceValue("All");
			module.addFilter(defaultNameFilter);
			module.addFilter(taxYearFilter);
			module.addValidator(addressValidator);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}
				
		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Collection<ParsedResponse> intermediaryResponse = super.smartParseIntermediary(response, table, outputTable);

		return intermediaryResponse;
	}

	
	@Override
	public String getYearSelect(String id, String name) {
		// getTaxYears();
		String county = dataSite.getCountyName();
		super.getTaxYears(county);
		int lastTaxYear = taxYears.get("lastTaxYear" + county) - 1;
		int firstTaxYear = taxYears.get("firstTaxYear" + county);
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.
			// This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = 2013;
			firstTaxYear = 2009;
		}
		
		// Generate input.
		StringBuilder select  = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n");
		for (int i = lastTaxYear; i >= firstTaxYear; i--){
			select.append("<option ");
			select.append(i == lastTaxYear ? " selected " : "");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("<option value=\"All\">All</option>");
		select.append("</select>");
		
		return select.toString();
	}
	
	public static String[] splitAPN(String pin) {
		String parts[] = new String[6];
		for (int i = 0; i < parts.length; i++)
			parts[i] = "";

		if (StringUtils.isEmpty(pin))
			return parts;
		
		if (StringUtils.isNotEmpty(pin)) {
			pin = pin.replaceAll("-", " ");
			String ctrl1 = "", ctrl2 = "", group = "", parcel = "", id = "", si = "";
			
			String regExp = "(?is)(.*\\d+\\.\\d+)([A-Z])(\\d+)";
			Matcher m = Pattern.compile(regExp).matcher(pin);
			if (m.find()) {
				// 108 17.00P1 or 62 P C 30.00P1
				pin = pin.replaceFirst("(?is)(\\d+)([A-Z])\\s([A-Z])", "$1 $2 $3");
			}
			
			regExp = "(?is)(\\d+)\\s*([A-Z])\\s*([A-Z])\\s*(\\d{1,3}\\.\\d{2})\\s*([A-Z])\\s*(\\d{1,3})";
			m = Pattern.compile(regExp).matcher(pin);
			if (m.matches()) {
				//cases like: 62 P C 30.00P1
				ctrl1 = m.group(1);
				ctrl2 = m.group(2);
				group = m.group(3);
				parcel = m.group(4);
				id = m.group(5);
				si = m.group(6).trim();

			} else {
				// 054 J B 015.00000015
				regExp = "(?is)(\\d+)\\s*([A-Z])\\s*([A-Z])\\s*(\\d{1,3}\\.\\d{2})\\s*(\\d{1,3}|[A-Z])\\s*(\\d{3})";
				m = Pattern.compile(regExp).matcher(pin);
				
				if (m.matches()) {
					ctrl1 = m.group(1);
					ctrl2 = m.group(2);
					group = m.group(3);
					parcel = m.group(4).trim();
					id = m.group(5).trim();
					si =  m.group(6).trim();
					
				} else {
					regExp = "(?is)(\\d+)\\s*(\\d{1,3}\\.\\d{2})\\s*([A-Z])\\s*(\\d{1,3})";
					m = Pattern.compile(regExp).matcher(pin);
					if (m.matches()) {
						ctrl1 = m.group(1);
						parcel = m.group(2);
						id = m.group(3);
						si =  m.group(4).trim();
					
					} else {
						regExp = "(?is)(\\d+)\\s*([A-Z])\\s*([A-Z])\\s*(\\d{1,3}\\.\\d{2})\\s*(\\d{1,3})\\Z";
						m = Pattern.compile(regExp).matcher(pin);
						if (m.matches()) {
							ctrl1 = m.group(1);
							ctrl2 = m.group(2);
							group = m.group(3);
							parcel = m.group(4);
							si =  m.group(5).trim();
						} else {
							regExp = "(?is)(\\d+)\\s*([A-Z])\\s*([A-Z])\\s*(\\d{1,3}\\.\\d{2})";
							m = Pattern.compile(regExp).matcher(pin);
							if (m.matches()) {
								// cases like 012 E C 008.00
								ctrl1 = m.group(1);
								ctrl2 = m.group(2);
								group = m.group(3);
								parcel = m.group(4);
							} else {
								regExp = "(?is)(\\d+)\\s*(\\d{1,3}\\.\\d{2})\\s*(\\d{1,3})";
								m = Pattern.compile(regExp).matcher(pin);
								if (m.matches()) {
									ctrl1 = m.group(1);
									parcel = m.group(2);
									si =  m.group(3).trim();
								} else {
									regExp = "(?is)(\\d+)\\s*(\\d{1,3}\\.\\d{2})";
									m = Pattern.compile(regExp).matcher(pin);
									if (m.matches()) {
										ctrl1 = m.group(1);
										parcel = m.group(2);
									}
								}
							}
						}
					}
				}
			}	
				
			
			if (StringUtils.isNotEmpty(ctrl1)) {
				parts[0] = ctrl1;				
			}
			if (StringUtils.isNotEmpty(ctrl2)) {
				parts[1] = ctrl2;			
			}
			if (StringUtils.isNotEmpty(group)) {
				parts[2] = group;				
			}
			if (StringUtils.isNotEmpty(parcel)) {
				parts[3] = parcel;				
			}
			if (StringUtils.isNotEmpty(id) && !"0".equals(id)) {
				parts[4] = id;				
			}
			if (StringUtils.isNotEmpty(si) && !("0".equals(si) || "000".equals(si))) {
				parts[5] = si;				
			}
		} 
		
		return parts;
	}
	
	protected void loadDataHash(HashMap<String, String> data, String taxYear) {
		if (data != null) {
			data.put("type", "CITYTAX");
			data.put("year", taxYear);
		}
	}

	protected void putSrcType(ResultMap m) {
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "YB");
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		return super.parseAndFillResultMap(response, detailsHtml, map);
	}

}
