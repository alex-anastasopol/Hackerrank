package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class TXDallasTR extends TXGenericACTTR{
	
	/**
	 * @author mihaib
	 */
	private static final long serialVersionUID = 3922610441378189743L;

	public TXDallasTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse nameFilterHybridDoNotSkipUnique = null;
		
		if (hasPin()){
			//Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			modules.add(module);
		}
		
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(addressFilter);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1,FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;m;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected String cleanIntermediaryResponse(String rsResponse, String linkStart) {
		String contents = rsResponse;
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList tableList = mainTableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "flextable"), true);
			if (tableList.size() > 0){
				contents = tableList.elementAt(0).toHtml();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
						
		contents = contents.replace("(?im)<caption align=\"right\">Your search took 0 seconds.</caption>.*", "</table>");
		
		//contents = rsResponse.replaceAll("(?is).*To\\s*view\\s*the\\s*full\\s*record[^>]+>[^>]+>\\s*<table[^>]*>\\s*<tr[^>]*>\\s*<td[^>]*>(.*)<caption[^>]*>Your\\s*search\\s*took.*", "$1</table>");
		contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*\\\"showlist.jsp\\?sort[^\\\"]+\\\"[^>]*>", "");
		
		//TO DO for each county change link in a separate method		
		contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*'([^']+)'[^>]*>", "<a href=\"" + linkStart + "/act_webdev/dallas/$1\">");
		
		contents = contents.replaceAll("(?is)\\s+<table", "<table");
		contents = contents.replaceAll("(?is)onMouse[^\\\"]*\\\"[^\\\"]*\\\"", "");
		contents = contents.replaceAll("(?is)</?font[^>]*>", "");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)<tr\\s{2,}", "<tr ");
		contents = contents.replaceAll("(?is)&nbsp;", " ");
		return contents;

	}
	@Override
	protected String cleanDetailsResponse(String response) {
		
		String contents="";
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList nodeList = HtmlParser3.getTag(mainTableList, new TableTag(), true);
			Text tableHeading = HtmlParser3.findNode(mainTableList, "Property Tax Balance");
			
			mainTableList.remove(nodeList.elementAt(1));
			mainTableList.remove(nodeList.elementAt(3));
			nodeList.remove(1);
			nodeList.remove(3);
			contents =
				"<table>" + "\n" +
				"<tr><td><h6>" + tableHeading.toHtml() + "</h6></td></tr>";  
			String detailsTable = "";
			String headerTable = "";
			NodeList tables = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int i = 0; i < tables.size(); i++){
				if (tables.elementAt(i).toHtml().contains("Account Number")){
					detailsTable = tables.elementAt(i).toHtml();
				} else if (tables.elementAt(i).toHtml().contains("Tax Year")){
					headerTable = tables.elementAt(i).toHtml();
				}
			}
			contents += "<tr><td>" + headerTable + "</td></tr><tr><td>" + detailsTable + "</td></tr></table>";
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		contents = contents.replaceAll("(?is)<a[^>]+>[^<]+</a>", "");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)<i>\\s*Make your check.*?</font>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]+>", "");
		contents = contents.replaceAll("(?is)<form[^>]+>.*</form>", "");
		contents = contents.replaceAll("(?is)<font[^>]+>.*</font>", "");
		contents = contents.replaceAll("(?is)<br>\\s*<br>\\s*<br>", "<br>");
		contents = contents.replaceAll("(?is)h6", "h2");
		contents = contents.replaceAll("(?is)(<br>)\\s*(<b>)", "$1 </h3> <h3> $2");
		return contents;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), COMMON_ABBREVIATIONS);
		}
				
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	public static final String COMMON_ABBREVIATIONS = 
		"<select>" +
		"<option value=\"----------\">----------</option>" + 
		"<option value=\"avenue    \">AVENUE    </option>" +
		"<option value=\"boulevard \">BOULEVARD </option>" +
		"<option value=\"causeway  \">CAUSEWAY  </option>" +
		"<option value=\"circle    \">CIRCLE    </option>" +
		"<option value=\"drive \">DRIVE </option>" +
		"<option value=\"freeway  \">FREEWAY  </option>" +
		"<option value=\"heights  \">HEIGHTS  </option>" +
		"<option value=\"highway \">HIGHWAY </option>" +
		"<option value=\"lane \">LANE </option>" +
		"<option value=\"motorway  \">MOTORWAY  </option>" +
		"<option value=\"parkway \">PARKWAY </option>" +
		"<option value=\"road\">ROAD</option>" +
		"<option value=\"route \">ROUTE </option>" +
		"<option value=\"skyway  \">SKYWAY  </option>" +
		"<option value=\"street\">STREET</option>" +
		"<option value=\"----------\">----------</option>" +
		"</select>";
	
		
	public static final String EXTRA_CRITERIA_SELECT = 
		"<select name=\"subsearchby\" size=\"4\">" +
		"<option value=\"1\">Owner Name</option>" +
		"<option value=\"2\">Account No.</option>" +
		"<option value=\"3\">Property Address</option>" +
		"<option value=\"4\">CAD Reference No. </option>" +
		"</select>";

}
