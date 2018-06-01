package ro.cst.tsearch.servers.types;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.RegExUtils;

public class COSanMiguelRO extends GenericCountyRecorderROImage {
	private static final long	serialVersionUID	= 1798193762290705254L;

	public COSanMiguelRO(long searchId) {
		super(searchId);
	}

	public COSanMiguelRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {

		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookupWasDoneWithName, false, getDataSite()) {

			private static final long	serialVersionUID	= -3336972059858512740L;

			@Override
			protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {

				String subdName = str.getAddition();// b9648 - use subdivision name on all legal searches
				subdName = correctSubdivisionName(subdName);
				subdName = getSubdivisionCodeFromName(subdName);

				if (StringUtils.isNotEmpty(subdName)) {
					((TSServerInfoFunction) module.getFunction(13)).setParamValue(subdName);
					for (Object functionObject : module.getFunctionList()) {
						if (functionObject instanceof TSServerInfoFunction) {
							TSServerInfoFunction function = (TSServerInfoFunction) functionObject;

							switch (function.getIteratorType()) {
							case FunctionStatesIterator.ITERATOR_TYPE_LOT:
								function.setParamValue(str.getLot());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_BLOCK:
								function.setParamValue(str.getBlock());
								break;
							// case FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE:
							// String subdName = str.getAddition();
							// subdName = correctSubdivisionName(subdName);
							// subdName = getSubdivisionCodeFromName(subdName);
							// function.setParamValue(subdName);
							// break;
							case FunctionStatesIterator.ITERATOR_TYPE_SECTION:
								function.setParamValue(str.getSection());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_TOWNSHIP:
								function.setParamValue(str.getTownship());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_RANGE:
								function.setParamValue(str.getRange());
								break;
							}
						}
					}
				}
			}
		};

		it.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		it.setEnableSubdividedLegal(false);
		it.setEnableSubdivision(true);
		it.setEnableTownshipLegal(true);

		return it;
	}

	@Override
	public String correctSubdivisionName(String subdivisionName) {
		subdivisionName = super.correctSubdivisionName(subdivisionName);

		if (getSubdivisionCodeFromName(subdivisionName).isEmpty()) {
			if (RegExUtils.getFirstMatch("(?is).*?\\s+(SUB(?:DIVISION)?|CONDO(?:MINIUMS?)?|ADD(?:ITION)?)\\s*$", subdivisionName, 1).isEmpty())
				subdivisionName = subdivisionName + " SUB";// e.g. "SKALLA SUB" on auto search with PIN 202-0022061
		}

		return subdivisionName;
	}
}
