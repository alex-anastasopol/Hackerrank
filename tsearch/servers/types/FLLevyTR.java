package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.functions.FLGenericPacificBlueTR.FLGenericPacificBlueTRParseType;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class FLLevyTR extends FLGenericPacificBlueTR {
	
	private static final long serialVersionUID = 148731310746937763L;

	public FLLevyTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected  ro.cst.tsearch.servers.functions.FLGenericPacificBlueTR getParser()  {
		return ro.cst.tsearch.servers.functions.FLGenericPacificBlueTR.getInstance(FLGenericPacificBlueTRParseType.FLLevyTR);
	}

	protected String getLinkForAssessorPage(String pin) {
		return "http://qpublic6.qpublic.net/fl_sdisplay.php?county=fl_levy&KEY=" + pin.replaceAll("-", "");
	}
	
	protected String getPinFromLink(String linkAO) {
		String pin = StringUtils.extractParameterFromUrl(linkAO, "pin");
		return pin;
	}

	protected String addAppraissalDataToContents(String contents, String linkForAssessor, String assessorPage) {
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		HTTPRequest req = new HTTPRequest(linkForAssessor, HTTPRequest.GET);
		HTTPResponse res = null;
		try {
			res = site.process(req);
		} finally {
			HttpManager.releaseSite(site);
		}
		String resp = res.getResponseAsString();
		
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
		NodeList mainList;
		try {
			String sales = "";
			mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "table_class"));
			for (int i=0;i<tables.size();i++)
				if (tables.elementAt(i).toPlainTextString().contains("Sale Information")) {
					sales = tables.elementAt(i).toHtml();
					break;
				}
			sales = sales.replaceAll("(?is)class=\"table_class\"", "id=\"saleTable\" border=1");
			sales = sales.replaceAll("Sale Information", "<b>Sale Information</b>");
			sales = sales.replaceAll("(?is)class=\"[^\"]+\"", "");
			sales = sales.replaceAll("(?is)<font color=\"[^\"]+\">", "");
			sales = sales.replaceAll("\\u0000", "");
			contents += sales;
					
		} catch (ParserException e) {
			logger.error("Error while getting details " + linkForAssessor, e);
		}
		
		return contents;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String address = getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NO_DIR_NAME_POSTDIR);
		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d);
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);

		if (hasPin()) {
			// Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			module.addFilter(new TaxYearFilterResponse(searchId));
			modules.add(module);
		}

		if (hasStreet()) {
			// Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(address);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(new TaxYearFilterResponse(searchId));
			modules.add(module);
		}

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		if (hasOwner()) {
			// Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(new TaxYearFilterResponse(searchId));
			if ("levy".equalsIgnoreCase(crtCounty))
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			else 
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
					module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
