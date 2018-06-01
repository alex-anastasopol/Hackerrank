package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PersonalDataStruct;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.name.NameI;

public interface DTLikeAutomatic {
	boolean addAoLookUpSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate, boolean isTimeShare);

	void addIteratorModule(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules, int subdivisionModuleIdx,
			long searchId, boolean isUpdate, boolean isTimeShare);

	ArrayList<NameI> addNameSearch(List<TSServerInfoModule> modules,
			TSServerInfo serverInfo, String ownerObject,  ArrayList<NameI> object,
			List<FilterResponse> filtersOList);

	void addOCRSearch(List<TSServerInfoModule> modules,
			TSServerInfo serverInfo, FilterResponse ... legalOrDocTypeFilter);

	void addAssesorMapSearch(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules, boolean isUpdate);

	void addPlatMapSearch(TSServerInfoModule module, PersonalDataStruct str);

	void addSubdivisionMapSearch(TSServerInfoModule module,PersonalDataStruct str);

	void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str);

	void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str);

	void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str,	String extraStringForsection);

	String prepareApnPerCounty(long searchId);

	GenericRuntimeIterator<InstrumentI> getInstrumentIterator(boolean reference);

	void addGrantorGranteeSearch(List<TSServerInfoModule> modules,
			TSServerInfo serverInfo, String ownerObject,
			List<FilterResponse> filtersOList);
	
	void addRelatedSearch(TSServerInfo serverInfo, 
			List<TSServerInfoModule> modules) ;

}
