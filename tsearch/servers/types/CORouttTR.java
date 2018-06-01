package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;


public class CORouttTR extends COGenericTylerTechTR {
	
	private static final long serialVersionUID = 1L;
//	private static String SUBDIVISION_CODE_SELECT = "";
//	
//	static {
//		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
//		File folder = new File(folderPath);
//		if(!folder.exists() || !folder.isDirectory()) {
//			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
//		}
//		try {
//			SUBDIVISION_CODE_SELECT = FileUtils.readFileToString(new File(folderPath + File.separator + "CORouttTRsubdivisionCode.xml"));
//			
//		} catch (Exception e) {
//			e.printStackTrace();	
//		}
//	}
	
	public CORouttTR(long searchId){
		super(searchId);
	}
	
	public CORouttTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) 
	{
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		// search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if(StringUtils.isNotEmpty(pin)) {
		
			String[] pins = pin.split("[\\,/]");
			
			String validPin = null;
			
			if(pins.length > 0) {
				if (pins.length == 1){
					validPin = pin;
				} else{
					for (String littlePin : pins) {
						if(littlePin.startsWith("R")) {
							validPin = littlePin;
							break;
						}
					}
				}
			} else {
				validPin = pin;
			}
			
			
			if(!isEmpty(validPin)){
				if (validPin.matches("[A-Z]\\d+")){
					TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));			
					module.clearSaKeys();
					module.getFunction(0).forceValue(validPin);  
					modules.add(module);
				} else {
					TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));			
					module.clearSaKeys();
					module.getFunction(1).forceValue(validPin);
					modules.add(module);
				}
			}
		
		}
		
		// search by Address
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		if(hasStreet() && hasStreetNo()){
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
			
			
			module.clearSaKeys();
			 
			module.getFunction(4).forceValue(strNo);
			module.getFunction(6).forceValue(strName);
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module);
			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(addressFilter);
			
			module.setIteratorType(2,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	protected String getSaleInfoTable(String accountId) {
		// make a request to get Appraissal info for SaleDataSet
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		String assessorPage = "";
		try {
			if (StringUtils.isNotEmpty(accountId)) {
				assessorPage = ((ro.cst.tsearch.connection.http2.CORouttTR) site).getAssessorPage(accountId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		HtmlParser3 parser = new HtmlParser3(assessorPage);
		Node saleDataNode = parser.getNodeByAttribute("class", "accountSummary", true);
		NodeList tableList = saleDataNode.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
		
		String html = "";
		
		TableTag saleDataTable = null;
		if (tableList != null){
			for(int i = 0; i < tableList.size(); i++){
				if (tableList.elementAt(i).toHtml().contains("Doc Type")){
					saleDataTable = (TableTag) tableList.elementAt(i);
					break;
				}
			}
		}
		
		if (saleDataTable != null){
			saleDataTable.setAttribute("id", "gdv_SalesDeeds");
			html = saleDataTable.toHtml();
			// clean it
			html = html.replaceAll("<u>\\(Click for Recorded Document\\)", "");
			html = html.replaceAll("(?is)\\([^<]*", "");
			html = html.replaceAll("(?is)</?a[^>]*>", "");
		}
		
		return html;
	}
	
	public static void parseSaleDataInfo( HtmlParser3 parser, ResultMap m, long searchId) throws Exception {
		//Node nodeById = parser.getNodeById("");
		Node saleDataTable = parser.getNodeById("gdv_SalesDeeds");
		
		if (saleDataTable != null){
			List<List<String>> tableAsListMap = HtmlParser3.getTableAsList(saleDataTable.toHtml(), false);
			
			String[] header= new String[]{"InstrumentDate", "SalesPrice", "DocumentType", "InstrumentNumber", "Book", "Page", "Grantor"};
			 
			List<List> body = new ArrayList<List>();
			List<String> line = null;
			
			for (List<String> list : tableAsListMap) {
				line = new ArrayList<String>();
				line.add(list.get(0));
				line.add(list.get(1).replaceAll("(?is)[,\\$]+", ""));
				line.add(list.get(2));
				line.add(list.get(3));
				String bookpage = list.get(4).trim();
				bookpage = bookpage.replaceFirst("(?is)\\AB:", "").trim();
				String[] bp = bookpage.split("\\s*P:\\s*");
				if (bp.length == 2){
					line.add(bp[0].trim());
					line.add(bp[1].trim());
				} else{
					line.add("");
					line.add("");
				}
				line.add(list.get(5));
				body.add(line);
			}

			// adding all cross references - should contain transfer table and info
			// parsed from legal description
			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}
		}
	}
}
