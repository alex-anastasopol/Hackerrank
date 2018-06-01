package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.functions.ParseClass;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;

public class FLBakerTR extends TemplatedServer{

	public FLBakerTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		
		int[] intermediary_cases = { ID_INTERMEDIARY, TSServer.ID_SEARCH_BY_NAME};
		setIntermediaryCases(intermediary_cases);
		
		int[] details_cases = { TSServer.ID_DETAILS };
		setDetailsCases(details_cases);
		
		int[] save_cases = { TSServer.ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);
		
		setDetailsMessage("Tax Roll Parcel Information:");	 
	}
	
	protected String getAccountNumber(String serverResult) {
		String firstMatch = RegExUtils.getFirstMatch("(?is)Parcel/Account Number.*?<br>(.*?)</td>", serverResult, 1);
		return firstMatch;
	}
	
	protected void setMessages() {
		System.err.println("Must implement!!!");
	}
	
	
	
	@Override
	protected String clean(String response) {
		//keep only the results table 
		String firstMatch = RegExUtils.getFirstMatch("(?is)<table width=500px.*?</table>", response, 0);
		if (StringUtils.isNotEmpty(firstMatch)){
			response = firstMatch;
		}
		return response;
	}
	
	@Override
	protected String cleanDetails(String response) {
		String firstMatch = RegExUtils.getFirstMatch("(?is)<table cellspacing=0 width=785>.*(</table>|</script>)", response, 0);
		String replaceAll = firstMatch.replaceAll("(?is)<a.*View Printer Friendly Version</a>", "");
		String delinquentCase = "</table><center>$1</center>";
		replaceAll = replaceAll.replaceAll("(?is)<script.*alert\\(\"(.*\")\\).*</script>", delinquentCase);
		replaceAll = replaceAll.replaceAll("<a class=\\\"green\\\".*?>*.?Property.*?</a>", "");
		replaceAll = replaceAll.replaceAll("<img.*?>", "");
		replaceAll = replaceAll.replaceAll("(?is)\\*{3,}\\s*[\\w\\s]+\\*{3,}", "");
		return replaceAll;
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		ParseClass instance = ro.cst.tsearch.servers.functions.FLBakerTR.getInstance();
		Vector<ParsedResponse> parseIntermediary = instance.parseIntermediary(response, table, searchId, createPartialLinkFormat());
		return parseIntermediary;
	}
	
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			try{
	    		String prevSrcType = (String)map.get(OtherInformationSetKey.SRC_TYPE.getKeyName());
	    		if(StringUtils.isEmpty(prevSrcType)){	    			
	    			map.getMap().put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
	    		}
			}catch(Exception e){
				e.printStackTrace();
			}  
			document = bridge.importData();
			if (detailsHtml.indexOf("There are back taxes due") != -1 || detailsHtml.indexOf("Installment Payment, please call Tax Collector") != -1) {
				String note = detailsHtml.replaceFirst("(?is).*<center>([^<]+)</center>", "$1");
				if (StringUtils.isNotEmpty(note)) 
					document.setNote(note);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if(document!=null) {
				response.getParsedResponse().setDocument(document);
			}
		}
		response.getParsedResponse().setSearchId(this.searchId);
		return document;
	}
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ParseClass instance = ro.cst.tsearch.servers.functions.FLBakerTR.getInstance();
		instance.parseDetails(detailsHtml, searchId, map);
		
		return null;
	}
	
	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CNTYTAX");
//		String year = instance.getCurrentYear(serverResult);
//		data.put("year", year);
		return data;
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
//		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		DocsValidator legalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		TaxYearFilterResponse fr = new TaxYearFilterResponse(searchId);
		fr.setThreshold(new BigDecimal("0.95"));

		TSServerInfoModule module = null;
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(1).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			l.add(module);
		}
		
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

//			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);

			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(	module, searchId, new String[] { "L;F;", "L; F M;", "L; f;", "L; m;" });
			module.addIterator(nameIterator);
			module.addValidator(legalValidator);
			l.add(module);
		}

		serverInfo.setModulesForAutoSearch(l);
	}


}
