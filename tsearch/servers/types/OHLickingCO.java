package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servlet.BaseServlet;

@SuppressWarnings("deprecation")
public class OHLickingCO extends OHGenericCourtViewCO {
	
	private static final long serialVersionUID = -6155635870050393103L;
	
	private static String LICKING_SUFFIX_SELECT = "";

	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			LICKING_SUFFIX_SELECT = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath + File.separator + "OHLickingCOSuffix.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public OHLickingCO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public OHLickingCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(3), LICKING_SUFFIX_SELECT);
		}
		setModulesForAutoSearch( msiServerInfoDefault );
        setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
        return msiServerInfoDefault;
	}

}
