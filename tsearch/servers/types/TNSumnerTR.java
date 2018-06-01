package ro.cst.tsearch.servers.types;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.MultipleYearIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class TNSumnerTR extends TNGenericEgovTR
{
	private static final long serialVersionUID = 1L;

	public TNSumnerTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.TNSumnerTR.parseIntermediaryRow(row);
	}
	
	@Override
	protected void parseSpecificDetails(ResultMap resultMap) {
		try {
			ro.cst.tsearch.servers.functions.TNSumnerTR.parseAddress(resultMap);
//			ro.cst.tsearch.servers.functions.TNSumnerTR.parseNames(resultMap);
			ro.cst.tsearch.servers.functions.TNSumnerTR.parseNames(resultMap);
		} catch (Exception e) {
			logger.error("Error while parsing details");
		}
	}
	
	protected void addPinModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, FilterResponse billNumberFilter, FilterResponse addressFilter,
			SearchAttributes sa) {
		TSServerInfoModule module;
		// search by account number
		if(hasPin()){
			
			boolean found = false;
			String controlMap = "";
        	String group = "";
        	String parcel = "";
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
        	Matcher ma1 = Pattern.compile("([^-]+)-([^-]+)").matcher(pin);
        	Matcher ma2 = Pattern.compile("([^-]+)-([^-]+)-([^-]+)").matcher(pin);
        	Matcher ma3 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{3}\\.\\d{2})").matcher(pin);
        	Matcher ma4 = Pattern.compile("([^-]+)-([^-]*)-\\1-([^-]+)-[^-]*-[^-]+").matcher(pin);
        	Matcher ma5 = Pattern.compile("([^\\s]+) ([^\\s]*) ([^\\s]+) [^\\s]+").matcher(pin);
        	Matcher ma6 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{5})(\\d{3})").matcher(pin);
        	if (ma1.matches()) {			//PIN from NB with dashes without group
        		controlMap = ma1.group(1);
        		parcel = ma1.group(2);
        		found = true;
        	} else if (ma2.matches()) {		//PIN from NB with dashes with group
        		controlMap = ma2.group(1);
        		group = ma2.group(2);
        		parcel = ma2.group(3);
        		found = true;
        	} else if (ma3.matches()) {		//PIN from NB without dashes
        		controlMap = ma3.group(1);
        		group = ma3.group(2);
        		parcel = ma3.group(3);
        		found = true;
        	} else if (ma4.matches()) {		//PIN from AO
        		controlMap = ma4.group(1);
        		group = ma4.group(2);
        		parcel = ma4.group(3);
        		found = true;
        	} else if (ma5.matches()) {		//PIN from TR without si
        		controlMap = ma5.group(1);
        		group = ma5.group(2);
        		parcel = ma5.group(3);
        		parcel = parcel.replaceFirst("([^\\s]{3})([^\\s]{2})", "$1.$2");
        		found = true;
        	} else if (ma6.matches()) {		//PIN from TR with si
        		controlMap = ma6.group(1);
        		group = ma6.group(2);
        		parcel = ma6.group(3);
        		parcel = parcel.replaceFirst("([^\\s]{3})([^\\s]{2})", "$1.$2");
        		found = true;
        	}
			
			if (found)
	    	{
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(1, SearchAttributes.CURRENT_TAX_YEAR);
				
				module.getFunction(6).setData (controlMap.replaceAll("^0+", ""));
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP, controlMap);
		    	
		    	module.getFunction(7).setData (group.replaceAll("^0+", ""));
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP, group);

		    	module.getFunction(8).setData (parcel.replaceAll("^0+", ""));
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_PARCEL, parcel);
		    	
		    	module.addFilter(billNumberFilter);
		    	module.addFilter(addressFilter);
		    	
		    	module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory.getMultipleYearIterator(module, searchId, numberOfYearsAllowed, getCurrentTaxYear());
				module.addIterator(yearIterator);
				
				modules.add(module);	
		    	
	    	}
					
		}
	}
		
}