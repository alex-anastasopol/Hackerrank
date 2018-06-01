package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;

public class COGarfieldTR extends COGenericTylerTechTR {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8781152868383139883L;

	public COGarfieldTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	protected ResultMap parseIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.COWeldTR.parseIntermediaryRow(row, this);
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.COWeldTR.parseAndFillResultMap(detailsHtml, map, searchId, this);
		// saveTestDataToFiles(map);
		return null;
	}

	public void parseLegal(String contents, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COGarfieldTR.parseLegal(contents, resultMap);
	}

	public void parseName(HashSet<String> hashSet, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COGarfieldTR.parseName(hashSet, resultMap);
	}

	public void parseName(Set<String> hashSet, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COGarfieldTR.parseName(hashSet, resultMap);
	}

	public void parseAddress(String origAddress, ResultMap resultMap) throws Exception {
		ro.cst.tsearch.servers.functions.COGarfieldTR.parseAddress(resultMap, origAddress);
	}

	public void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) throws Exception {
		ro.cst.tsearch.servers.functions.COGarfieldTR.parseIntermediaryRow(server, resultMap, cols);
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));

		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(10).setSaKey(SearchAttributes.LD_PARCELNO);
			l.add(m);
		}

		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(2).setSaKey(SearchAttributes.LD_PARCELNO);
			l.add(m);
		}

		if (hasStreet()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(4).setSaKey(SearchAttributes.P_STREETNO);
			m.getFunction(6).setSaKey(SearchAttributes.P_STREETNAME);
			m.addFilter(nameFilterHybrid);
			m.addFilter(rejectNonRealEstateFilter);
			m.addValidator(addressFilter.getValidator());
			m.addValidator(defaultLegalValidator);
			l.add(m);
		}

		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT,
					searchId, m);
			defaultNameFilter.setUseSubdivisionNameAsCandidat(false);
			m.addFilter(defaultNameFilter);
			m.addFilter(rejectNonRealEstateFilter);
			m.addValidator(addressFilter.getValidator());
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m,
					searchId, new String[] { "L;F;M", "L;F;" });
			m.addIterator(nameIterator);
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);

	}

}
