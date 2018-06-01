/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.RestoreDocumentDataI;

/**
 * @author radu bacrau
 *
 */
public class TNMontgomeryRO extends TNGenericUsTitleSearchDefaultRO {

	private static final long serialVersionUID = 1L;

	/**
	 * @param searchId
	 */
	public TNMontgomeryRO(long searchId) {
		super(searchId);		
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public TNMontgomeryRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		String intrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		TSServerInfoModule module = null;
		
		if(StringUtils.isNotEmpty(intrumentNumber)) {
			
			if(intrumentNumber.contains("-")) {
				if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					
					List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
					
					for(String volumeClass: new String[]{"-1", "3", "2", "1"}){
						module = getDefaultServerInfo().getModule(BP_MODULE_IDX);
						module.forceValue(0, book);
						module.forceValue(1, page);
						module.forceValue(2, "1");
						module.forceValue(3, "1");
						module.forceValue(5, volumeClass);
						list.add(module);
					}
					
					return list;
				}
			} else {
				module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
				module.forceValue(0, intrumentNumber);
				module.forceValue(1, "1");
				module.forceValue(2, "1");
				return module;
			}
			
			
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
			
			for(String volumeClass: new String[]{"-1", "3", "2", "1"}){
				module = getDefaultServerInfo().getModule(BP_MODULE_IDX);
				module.forceValue(0, book);
				module.forceValue(1, page);
				module.forceValue(2, "1");
				module.forceValue(3, "1");
				module.forceValue(5, volumeClass);
				list.add(module);
			}
			
			return list;
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "1");
			module.forceValue(2, "1");
		} else {
			module = null;
		}
		return module;
	}
	
}
