package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupMultipleSelectBox;

import java.io.File;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servlet.BaseServlet;

/**
 * @author mihaib
 * @author l
 **/

public class ARPulaskiAO extends ro.cst.tsearch.servers.types.ARGenericCountyDataAOTR {

	protected static String SUB_SELECT = "";

	static {
		SUB_SELECT = getHtmlSelectFromFile("ARPulaskiAOSubdivisionList.xml");
	}

	public ARPulaskiAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);

		initTemplatedServerData();

	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);

		if (tsServerInfoModule != null) {
			setupMultipleSelectBox(tsServerInfoModule.getFunction(20), SUB_SELECT);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	@Override
	protected ConfigurableNameIterator getConfigurableNameIterator(TSServerInfoModule module) {
		return (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] { "L;f;" });
	}

	@Override
	public int getSiteType() {
		return GWTDataSite.AO_TYPE;
	}

}