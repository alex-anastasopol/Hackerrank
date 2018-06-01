package ro.cst.tsearch.titledocument;

import java.util.LinkedHashMap;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.recoverdocument.RecoverDocumentRow;
import ro.cst.tsearch.database.rowmapper.ModuleMapper;
import ro.cst.tsearch.generic.tag.LoopTag;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.RestoreDocumentData;

public class RecoverDocumentsLoopTag extends LoopTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected long searchId = 0;
	public String getSearchId() {
	    return Long.toString(searchId);
	}
	
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	@Override
	protected Object[] createObjectList() throws Exception {
		
		List<ModuleMapper> allModules = ModuleMapper.getModulesForSearch(searchId);
		List<RestoreDocumentData> allDocuments = RestoreDocumentData.getAllDocumentsForModules(allModules);
		LinkedHashMap<Long, RecoverDocumentRow> allRows = new LinkedHashMap<Long, RecoverDocumentRow>();
		County currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		search.resetRestorableDocuments();
		
		for (ModuleMapper module : allModules) {
			RecoverDocumentRow currentRow = new RecoverDocumentRow(module);
			allRows.put(module.getModuleId(), currentRow);
			
		}
		
		for (RestoreDocumentData restoreDocumentData : allDocuments) {
			restoreDocumentData.setCounty(currentCounty);
			restoreDocumentData.setSearchId(searchId);
			if(restoreDocumentData.isInSearch(search, currentCounty)) {
				restoreDocumentData.deleteFromDatabase();
			} else {
				RecoverDocumentRow row = allRows.get(restoreDocumentData.getModuleId());
				if(row != null) {
					row.addDocument(restoreDocumentData);
				}
				search.addRestorableDocument(restoreDocumentData);
			}
		}
		
		return allRows.values().toArray();
		
	}

}
