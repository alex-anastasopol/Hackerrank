package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;


public class TXEllisTR extends TXGenericACTTR{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6228239512302423449L;

	public TXEllisTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	
	@Override
	protected String cleanIntermediaryResponse(String rsResponse, String linkStart) {
		String contents;
		String delimiterString = "<table width=\"790\">";
		int indexOf = rsResponse.indexOf(delimiterString);
		if (indexOf != -1){
			contents = rsResponse.substring(indexOf);
			contents = contents.replaceFirst("(?is)<table>\\s*<tr>\\s*\\s*<td>\\s*<!--.*-->\\s*</td>\\s*</tr>\\s*</table>", "");
			contents = contents.replaceFirst("(?is)<table width=\"100%\">.*ELLIS/Question.jsp.*", "");
			contents = contents.replaceFirst("(?is)</table>\\s*</td>\\s*</tr>\\s*<tr>.*", "</table>");
			contents = contents.replaceAll("(?is)<caption[^>]+>[^>]+>\\s*", "");
			contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*\\\"showlist.jsp\\?sort[^\\\"]+\\\"[^>]*>(\\s*<b>[^<]+</b>)\\s*</a>", "$1");
			contents = contents.replaceAll("(?is)<h3>\\s*(<b>[^>]+>|<a[^<]+</a>|[\\.\\d\\s\\w,&<>#-/]+)\\s*</h3>", "$1");
			contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*'([^']+)'[^>]*>", "<a href=\"" + linkStart + "/act_webdev/ellis/$1\">");
			contents = contents.replaceAll("(?is)\\s+<table", "<table");
			contents = contents.replaceAll("(?is)onMouse[^\\\"]*\\\"[^\\\"]*\\\"", "");
			contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
			contents = contents.replaceAll("(?is)<tr\\s{2,}", "<tr ");
			contents = contents.replaceAll("(?is)&nbsp;", " ");
			
		} else {
			return rsResponse;
		}
		
		return contents;
	}
	
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		try {
			if (table.contains("Account Number")){
				HtmlParser3 htmlParser = new HtmlParser3(table);
				NodeList mainTableList = htmlParser.getNodeList();
				NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				
				if (tableList.size() == 2) {
					TableTag mainTable = (TableTag)tableList.elementAt(1);
					
					TableRow[] rows = mainTable.getRows();
					int noOfRows = 0;
					if (rows.length > 1) {
						noOfRows = rows.length;
						for (int i=1; i < noOfRows; i++) {
							TableRow row = rows[i];
							
							if(row.getColumnCount() > 1) {
								TableColumn[] cols = row.getColumns();
								NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
								if (aList.size() > 0){
									String link = ((LinkTag) aList.elementAt(0)).extractLink();
									
									String rowHtml =  row.toHtml();
									
									ParsedResponse currentResponse = new ParsedResponse();
									currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
									currentResponse.setOnlyResponse(rowHtml);
									currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
									
									ResultMap m = ro.cst.tsearch.servers.functions.TXBexarTR.parseIntermediaryRowTXBexarTR(row, searchId);
									m.removeTempDef();
									Bridge bridge = new Bridge(currentResponse, m, searchId);
									
									DocumentI document = (TaxDocumentI)bridge.importData();				
									currentResponse.setDocument(document);
									
									intermediaryResponse.add(currentResponse);
								}
							}
						}
					}
					
					String header = "<table width=\"90%\" cellpadding=\"5\" cellspacing=\"0\" border=\"1\">"
									+ "<tr bordercolor=\"#000000\">"
									+ "<td width=\"17%\" height=\"29\"><b>Account Number</b></td>"
									+ "<td width=\"24%\" height=\"29\"><b>Owner's Name &amp; Address</b></td>"
									+ "<td width=\"24%\" height=\"29\"><b>Property Site Address</b></td>"
									+ "<td width=\"19%\" height=\"29\"><b>Legal Description</b></td>"
									+ "<td width=\"19%\" height=\"29\"><b>CAD Reference No.</b></td>"
					                + "</tr>";
					response.getParsedResponse().setHeader(header);
					response.getParsedResponse().setFooter("</table><br><br>");			
					outputTable.append(table);
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	
	@Override
	protected int getResultType(){
		/*
		 * we have multiple results after a multiple PIN filtering
		 * or if we have multiple PINs in the search page
		 * in the latter case we will only search by PIN on TR anyway
		 */ 
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		/*
		 * We will iterate through all PINs from the search page
		 * but we will not issue the rest of the searches after a multiple PIN hit 
		 */
		return  mSearch.getSa().getPins(-1).size() > 1 &&
			    mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
	}  
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		//String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		//pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
		String ownLast = getSearchAttribute(SearchAttributes.OWNER_LNAME);
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			if (hasPin()){
				//Search by PIN
				Collection<String> pins = getSearchAttributes().getPins(-1);
				if (pins.size() >= 1) { //multiple PINS
					for(String pinToUse: pins){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
						module.clearSaKeys();
						module.getFunction(0).forceValue(pinToUse);
						modules.add(module);
							
//						//Search also my Cad reference No when pin is present as NB pin <=> Cad reference no
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
						module.clearSaKeys();
						module.getFunction(0).forceValue(pinToUse);
						modules.add(module);
					}
					if(modules.size() > 1) {
						// set list for automatic search 
						serverInfo.setModulesForAutoSearch(modules);
						resultType = MULTIPLE_RESULT_TYPE;
						return;
					}
				}
			}
		}
		
		if (hasStreet() && hasOwner()) {
			//Search by Property Address and Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo + " " + streetName);
			module.getFunction(1).forceValue(ownLast);
			module.getFunction(2).forceValue("3");
			module.getFunction(4).forceValue("8");
			module.addFilter(addressFilter);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo + " " + streetName);
			module.getFunction(2).forceValue("3");
			module.getFunction(4).forceValue("8");
			module.addFilter(addressFilter);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;m;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	

}
