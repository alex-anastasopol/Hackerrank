package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

	/**
	* @author mihaib
	*/

public class FLHendryTR extends FLGenericGovernmaxTR {
	
	private static final long serialVersionUID = 6525099447955957961L;

	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){			
			String linkText = getLinkText(row);
			return linkText.matches("[A-Z]\\d{6}.+");
		}
	};
	
	public FLHendryTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		
		FilterResponse realEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		realEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		
		FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		// P1 : search by PIN	
		if(hasPin()){
						
			//GEO Number module, witch use the APN from NB
			/*module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.addFilter(realEstateFilter);
			modules.add(module);*/
			
			String pid = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			
			//Account number module
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pid);  
			module.addFilter(realEstateFilter);
			modules.add(module);
		}
		
		// P2 : search by Address				
		if(hasAddress()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREET_FULL_NAME_EX);  
			module.addFilter(realEstateFilter);
			module.addFilter(addressFilter);
			
			if(hasLegal()){
				module.addFilter(legalFilter);
			}
			modules.add(module);		
		}
		
		// P3 : search by Owner Name				
		if(hasName()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(realEstateFilter);
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			if(hasLegal()){
				module.addFilter(legalFilter);
			}
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			
			module.addIterator(nameIterator);
			
			modules.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		if (!table.contains("<HR>")){//for searching by mailing address
			table = table.replaceAll("(?is)(</FONT>\\s*</TD>\\s*</TR>\\s*<TR)", "$1\r\n<HR>\r\n$2");
		}
		String[] rows = table.split("<HR>");

		for (int i = 0; i < rows.length - 1; i++) {
			String row = rows[i];
			ParsedResponse currentResponse = new ParsedResponse();

			ResultMap resultMap = new ResultMap();
			ro.cst.tsearch.servers.functions.FLHendryTR.parseAndFillIntermediaryResultMap(resultMap, row, searchId);
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			resultMap.removeTempDef();
			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			try {
				document = (TaxDocumentI) bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			String link = "";
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(row, null);
				NodeList mainList = htmlParser.parse(null);
				NodeList aList = mainList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
				if (aList.size() > 0){
					link = ((LinkTag) aList.elementAt(0)).extractLink();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row);
			currentResponse.setOnlyResponse(row);
			currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

			intermediaryResponse.add(currentResponse);
		}

		response.getParsedResponse().setHeader("<TABLE WIDTH=100% BORDER=0 BORDERCOLOR=Black VALIGN=TOP CELLSPACING=0 CELLPADDING=3>");
		response.getParsedResponse().setFooter("</table>");
		response.getParsedResponse().setResultRows(intermediaryResponse);
		response.getParsedResponse().setOnlyResultRows(intermediaryResponse);
		response.getParsedResponse().setOnlyResponse(response.getParsedResponse().getHeader() + response.getParsedResponse().getFooter());

		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.FLHendryTR.parseAndFillResultMap(response, detailsHtml, map, searchId);
		return null;
	}

}
