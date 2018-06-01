package ro.cst.tsearch.bean.recoverdocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.rowmapper.ModuleMapper;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.parentsite.ParentSiteActions;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RestoreDocumentRecordedDateComparator;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.RestoreDocumentDataI;

public class RecoverDocumentRow {
	
	private ModuleMapper module;
	private List<RestoreDocumentDataI> restoreDocuments;
	private Search search;
	private County county;
	
	public RecoverDocumentRow() {
		restoreDocuments = new ArrayList<RestoreDocumentDataI>();
	}
	
	public RecoverDocumentRow(ModuleMapper module) {
		this.module = module;
		restoreDocuments = new ArrayList<RestoreDocumentDataI>();
	}
	
	public String getRow() {
		StringBuilder html = new StringBuilder("<hr/>")
			.append(module.getDescription());
		
		html.append(StringUtils.createCollapsibleHeader())
			.append("<form name=\"SaveToTSD\" action= \"/title-search" + URLMaping.PARENT_SITE_ACTIONS + "\"" + " method=\"POST\" > ")
			.append("<input type=\"hidden\" name=\""+ TSOpCode.OPCODE + "\" value=\""+ ParentSiteActions.RESTORE_DOCUMENTS + "\">")
			.append("<input type=\"hidden\" name=\"nameForCheckbox\" value=\"docToRestore" + module.getModuleId() + "\">")
			.append("<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + module.getSearchId() + "\"> ")
            .append("<input type=\"submit\" class=\"button\" name=\"Button\" value=\"Move Selected to TSRI\">");
		
		html.append("<table border=\"1\" cellspacing=\"0\" width=\"99%\"><tbody>")
			.append("<tr>")
			.append("<th width=\"2%\"><input type=\"checkbox\" onClick=\"var elems=document.getElementsByName('docToRestore" + module.getModuleId() + "'); for(var i=0; i<elems.length;i++) {elems[i].checked = this.checked;}\"/></th>")
			.append("<th width=\"2%\">DS</th>")
			.append("<th width=\"23%\" align=\"left\">Desc</th>")
			.append("<th width=\"4%\">Date</th>")
			.append("<th width=\"16%\">Grantor</th>")
			.append("<th width=\"16%\">Grantee</th>")
			.append("<th width=\"10%\">Instr Type</th>")
			.append("<th width=\"12%\">Instr</th>")
			.append("<th width=\"14%\">Remarks</th></tr>");
		Collections.sort(restoreDocuments, new RestoreDocumentRecordedDateComparator());
		for (RestoreDocumentDataI restoreDocumentDataI : restoreDocuments) {
			html.append(restoreDocumentDataI.getAsHtmlRow());
		}
		html.append("</tbody></table>")
			.append("<input type=\"submit\" class=\"button\" name=\"Button\" value=\"Move Selected to TSRI \">")
			.append("</form></div>");
		
		return html.toString();
	}
	
	public boolean addDocument(RestoreDocumentDataI document) {
		return restoreDocuments.add(document);
	}

	public int removeAlreadySaved() {
		if(search == null || county == null || restoreDocuments == null) {
			return 0;
		}
		int numberOfRemovedDocuments = 0;
		for (RestoreDocumentDataI restoreDocumentDataI : restoreDocuments) {
			if(restoreDocumentDataI.isInSearch(search, county)) {
				if(restoreDocumentDataI.deleteFromDatabase()) {
					numberOfRemovedDocuments ++;
				}
			}
		}
		return numberOfRemovedDocuments;
	}

	public boolean isEmpty() {
		return restoreDocuments.isEmpty();
	}

	public void setSearch(Search search) {
		this.search = search;
	}

	public ModuleMapper getModule() {
		return module;
	}

	public void setModule(ModuleMapper module) {
		this.module = module;
	}

	public List<RestoreDocumentDataI> getRestoreDocuments() {
		return restoreDocuments;
	}

	public void setRestoreDocuments(List<RestoreDocumentDataI> restoreDocuments) {
		this.restoreDocuments = restoreDocuments;
	}

	public Search getSearch() {
		return search;
	}

	public void setCounty(County currentCounty) {
		this.county = currentCounty;
		
	}

}
