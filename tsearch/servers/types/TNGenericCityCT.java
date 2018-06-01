package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.filter.DuplicateInstrumentFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class TNGenericCityCT extends TNGenericCountyTR {

	static final long serialVersionUID = 10000000;

	private String specificCnty = "";

	public void setServerID(int ServerID) {
		super.setServerID(ServerID);
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId),
				getServerID());
		String countyName = dataSite.getCityName();
		setSpecificCounty(countyName.replaceAll(" ", ""));
	}

	public TNGenericCityCT(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public String getCityName(){
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId),
				getServerID());
		return dataSite.getCityName().toUpperCase();
	}
	
	protected void setSpecificCounty(String cntyName) {
		specificCnty = cntyName;
		super.setSpecificCounty(cntyName);

	}

	protected String getSpecificCntySrvName() {
		return specificCnty;
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.NAME_MODULE_IDX);

		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId),
				getServerID());
		setSpecificCounty(dataSite.getCityName());

		msiServerInfoDefault.setServerAddress("www."
				+ getSpecificCntySrvName().toLowerCase()
				+ ".tennesseetrustee.org/");
		msiServerInfoDefault.setServerIP("www."
				+ getSpecificCntySrvName().toLowerCase()
				+ ".tennesseetrustee.org/");
		msiServerInfoDefault.setServerLink("https://www."
				+ getSpecificCntySrvName().toLowerCase()
				+ ".tennesseetrustee.org");

		if (tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setSaKey(
					SearchAttributes.P_STREET_NO_NAME);
		}

		setModulesForAutoSearch(msiServerInfoDefault);

		return msiServerInfoDefault;
	}

	@Override
	protected String getResultsTable() {
		String dataInfo = "";
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			String link = "https://www."
					+ getSpecificCntySrvName().toLowerCase()
					+ ".tennesseetrustee.org/hits.php";
			dataInfo = ((ro.cst.tsearch.connection.http2.TNGenericCityCT) site)
					.getPage(link);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		return dataInfo;
	}

	@Override
	protected void loadDataHash(HashMap<String, String> data, String year) {
		if (data != null) {
			data.put("type", "CITYTAX");
			data.put("year", year);
		}
	}

	@Override
	public ResultMap parseIntermediary(TableRow row, long searchId)
			throws Exception {
		ResultMap resultMap = super.parseIntermediary(row, searchId);
		resultMap.put("OtherInformationSet.SrcType", "YB");
		resultMap.removeTempDef();
		return resultMap;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);

			TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(
					new HasAttributeFilter("id", "results"), true).elementAt(0);

			if (mainTable == null) {
				return intermediaryResponse;
			}

			TableRow[] rows = mainTable.getRows();

			for (TableRow row : rows) {
				if (row.getColumnCount() > 10) {

					TableColumn[] cols = row.getColumns();
					NodeList aList = cols[11].getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("a"),
									true);
					String link = ((LinkTag) aList.elementAt(0)).extractLink();
					String rowHtml = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap resultMap = parseIntermediary(row, searchId);

					Bridge bridge = new Bridge(currentResponse, resultMap,
							searchId);
					resultMap.removeTempDef();

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}
			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {

		try {
			super.parseAndFillResultMap(response, detailsHtml, map);
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
					InstanceManager.getManager().getCommunityId(searchId),
					getServerID());
			String yx = dataSite.getSiteTypeAbrev();
			map.put("OtherInformationSet.SrcType", yx);
			map.removeTempDef();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		boolean emptyPid = "".equals(sa.getAtribute(SearchAttributes.LD_PARCELNO));
		
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if(!StringUtils.isEmpty(city)){
			if(!city.startsWith(getCityName())){
				return;
			}			
		}
		
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String streetSuffix = sa.getAtribute(SearchAttributes.P_STREETSUFIX);
		if (StringUtils.isNotEmpty(streetSuffix)) {
			streetSuffix = Normalize
					.translateSuffix(streetSuffix.toUpperCase());
		}
		if (StringUtils.isNotEmpty(streetNo)) {
			streetName = streetNo + " " + streetName;
		}
		boolean emptySubdiv = "".equals(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		boolean emptySubdivBlk = "".equals(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
		boolean emptySubdivLot = "".equals(sa.getAtribute(SearchAttributes.LD_LOTNO));
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		TaxYearFilterResponse frYr = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);
		DuplicateInstrumentFilterResponse instrument_year = new DuplicateInstrumentFilterResponse(searchId);

		
		TSServerInfoModule m;

		if (!emptyPid) {// parcel
			m = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			String pid = sa.getAtribute(SearchAttributes.LD_PARCELNO);
			pid = pid.replaceAll(" ", "");

			// map group parcel si
			String[] parts = { "", "", "", "" };

			// without leters
			if (pid.length() == 11) {
				parts[0] = pid.substring(0, 3);
				parts[2] = pid.substring(3, 8);
				parts[3] = pid.substring(8);
			}

			// without group
			if (pid.length() == 12) {
				parts[0] = pid.substring(0, 4);
				parts[2] = pid.substring(4, 9);
				parts[3] = pid.substring(9);
			}
			// 028L D 02900 000
			if (pid.length() == 13) {
				parts[0] = pid.substring(0, 4);
				parts[1] = pid.substring(4, 5);
				parts[2] = pid.substring(5, 10);
				parts[3] = pid.substring(10);
			}

			String map = parts[0];
			String group = parts[1];
			String parcel = parts[2];
			String si = parts[3];

			if (map.matches("000"))
				map = "";
			if (parcel.matches("00000"))
				parcel = "";
			if (si.matches("000"))
				si = "";

			m.getFunction(4).setData(map);
			sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP, map);

			m.getFunction(5).setData(group);
			sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP, group);

			parcel = parcel.replaceAll("\\.", "");
			m.getFunction(6).setData(parcel);
			sa.setAtribute(SearchAttributes.LD_PARCELNO_PARCEL, parcel);

			m.getFunction(7).setData(si);

			m.addFilter(frYr);
			m.addFilter(instrument_year);
			l.add(m);
		}

		if (hasStreet()) {// address
			m = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS_RUTH);
			m.clearSaKeys();
			m.getFunction(1).setData(streetName);

			FilterResponse nameFilterHybrid = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, m);

			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(frYr);
			m.addFilter(instrument_year);
			l.add(m);

		}

		if (!emptySubdiv || !emptySubdivBlk || !emptySubdivLot) {// address
			m = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(8).setSaKey(SearchAttributes.LD_SUBDIV_NAME);// 8.
																		// Subdivision
																		// Name
			m.getFunction(9).setSaKey(SearchAttributes.LD_SUBDIV_BLOCK);// 9.
																		// Subdivision
																		// Block
			m.getFunction(10).setSaKey(SearchAttributes.LD_LOTNO);// 10.
																	// Subdivision
																	// Lot

			m.getFunction(8).setIteratorType(
					FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);

			FilterResponse nameFilterHybrid = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, m);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(frYr);
			m.addFilter(instrument_year);
			l.add(m);
		}

		if (hasOwner()) {// owner
			m = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			FilterResponse nameFilterHybrid = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, m);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(frYr);
			m.addFilter(instrument_year);
			
			m.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {
							"L F M;;", "L F;;", "L f;;" });
			m.addIterator(nameIterator);
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}
}
