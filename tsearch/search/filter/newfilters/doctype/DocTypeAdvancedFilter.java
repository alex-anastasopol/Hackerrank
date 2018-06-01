package ro.cst.tsearch.search.filter.newfilters.doctype;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.ArrayUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * Validates documents with the specified types<br> 
 * Pass RELEASE, ASSIGNMENT, MODIFICATION, SUBORDINATION  documents if they refer a document validated in this search(search module) or already saved in TSRIndex<br>
 * Pass LIEN related documents if they refer a LIEN validated in this search(search module) or already saved in TSRIndex<br>
 * Should be used by default on GI name search with owner names
 * @author AndreiA
 *
 */
public class DocTypeAdvancedFilter extends DocTypeSimpleFilter {
	
	private static final long serialVersionUID = -903737192342L;

	protected String [] docTypesForGoodDocuments = {
			DocumentTypes.RELEASE,
			DocumentTypes.ASSIGNMENT,
			DocumentTypes.MODIFICATION,
			DocumentTypes.SUBORDINATION};
	protected String [] docTypesWithGoodAssociations = {
			DocumentTypes.LIEN,
			DocumentTypes.MORTGAGE};
	
	private List<RegisterDocumentI> allDocsToFilter = new ArrayList<RegisterDocumentI>();
	protected boolean forcePassIfNoReferences = false;
	
	protected boolean isUpdate = false;
	
	protected final DocumentsManagerI m;
	protected DocumentsManagerI mForUpdate;
	
	public DocTypeAdvancedFilter(long searchId){
		super(searchId);
		m = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
		setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		setThreshold(new BigDecimal("0.8"));
	}
	
	public void computeScores(@SuppressWarnings("rawtypes") Vector rows)
	{
		allDocsToFilter.clear();
		for (int i = 0; i < rows.size(); i++){
			DocumentI doc = ((ParsedResponse)rows.get(i)).getDocument();
			if(doc instanceof RegisterDocumentI){
				allDocsToFilter.add((RegisterDocumentI)doc);
			}
		}
		super.computeScores(rows);
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {	
		BigDecimal  ret = super.getScoreOneRow(row);
		
		if( ret.compareTo(threshold)<0 ){
			DocumentI doc  = row.getDocument();
			if(doc instanceof RegisterDocumentI){
				RegisterDocumentI regDoc = (RegisterDocumentI)doc;
				if(ArrayUtils.contains(docTypesForGoodDocuments, regDoc.getDocType())){ 
					Set<InstrumentI> references = regDoc.getParsedReferences();
					for(InstrumentI instr:references){
						for(RegisterDocumentI cur:allDocsToFilter){
							if(cur.flexibleEquals(instr,true)&& cur.isOneOf(docTypes) ){
								return BigDecimal.ONE;
							}
						}
						try{
							m.getAccess();
							if(m.getRegisterDocuments(instr, true).size()>0){
								return BigDecimal.ONE;
							}
						}finally{
							m.releaseAccess();
						}
						if (mForUpdate != null){
							try{
								mForUpdate.getAccess();
								if(mForUpdate.getRegisterDocuments(instr, true).size() > 0){
									return BigDecimal.ONE;
								}
							}finally{
								mForUpdate.releaseAccess();
							}
						}
					}
					
					if(forcePassIfNoReferences && references.size() == 0 && DocumentTypes.RELEASE.equals(regDoc.getDocType())) {
						return BigDecimal.ONE;
					}
				}
				Set<InstrumentI> references = regDoc.getParsedReferences();
				for(InstrumentI instr:references){
					for(RegisterDocumentI cur:allDocsToFilter){
						if(cur.flexibleEquals(instr,true)&& cur.isOneOf(docTypesWithGoodAssociations) ){
							return BigDecimal.ONE;
						}
					}
					try{
						m.getAccess();
						List<RegisterDocumentI> referencesInTsri = m.getRegisterDocuments(instr, true);
						if(referencesInTsri.size() > 0) {
							for (RegisterDocumentI cur : referencesInTsri) {
								if(cur.isOneOf(docTypesWithGoodAssociations)) {
									return BigDecimal.ONE;
								}
							}
						}
					}finally{
						m.releaseAccess();
					}
				}
			}
		}
		return ret;
	}
	
	 public String getFilterCriteria() {
		String ret = "Doctype: ";
		for (int i = 0; i < docTypes.length; i++) {
			if(i == 0) {
				ret += docTypes[i];
			} else {
				ret += ", " + docTypes[i];
			}
		}
		if(docTypesForGoodDocuments != null && docTypesForGoodDocuments.length > 0) {
			ret += ", keep good ";
			for (int i = 0; i < docTypesForGoodDocuments.length; i++) {
				if (i == 0) {
					ret += docTypesForGoodDocuments[i];
				} else {
					ret += ", " + docTypesForGoodDocuments[i];
				}
			}
		}
		
		if(docTypesWithGoodAssociations != null && docTypesWithGoodAssociations.length > 0) {
			if(docTypesForGoodDocuments != null && docTypesForGoodDocuments.length > 0) {
				ret += " and good documents associated with ";
			} else {
				ret += " and keep good documents associated with ";
			}
			for (int i = 0; i < docTypesWithGoodAssociations.length; i++) {
				if (i == 0) {
					ret += docTypesWithGoodAssociations[i];
				} else {
					ret += ", " + docTypesWithGoodAssociations[i];
				}
			}
		}
		
		return ret;
	}


	public DocTypeAdvancedFilter setForcePassIfNoReferences(boolean forcePassIfNoReferences) {
		this.forcePassIfNoReferences = forcePassIfNoReferences;
		return this;
	}

	public String[] getDocTypesForGoodDocuments() {
		return docTypesForGoodDocuments;
	}

	public DocTypeAdvancedFilter setIsUpdate(boolean isUpdate){
		this.isUpdate = isUpdate;
		if (isUpdate){
			long originalSearchId = getSearch().getSa().getOriginalSearchId();
			if (originalSearchId != -1){
				Search originalSearch = SearchManager.getSearchFromDisk(originalSearchId);
				this.mForUpdate = originalSearch.getDocManager();
			}
		}
		return this;
	}
	
	public boolean isUpdate(){
		return isUpdate;
	}
	/**
	 * If a document with one of these <b>docTypesForGoodDocuments</b> is found it will be saved<br>
	 * Good document means that it refers an already saved document
	 * @param docTypesForGoodDocuments
	 */
	public DocTypeAdvancedFilter setDocTypesForGoodDocuments(String[] docTypesForGoodDocuments) {
		this.docTypesForGoodDocuments = docTypesForGoodDocuments;
		return this;
	}

	public String[] getDocTypesWithGoodAssociations() {
		return docTypesWithGoodAssociations;
	}

	/**
	 * If a document found has a reference to an already saved document with one of these <b>docTypesWithGoodAssociations</b> it will be saved 
	 * @param docTypesWithGoodAssociations
	 */
	public void setDocTypesWithGoodAssociations(
			String[] docTypesWithGoodAssociations) {
		this.docTypesWithGoodAssociations = docTypesWithGoodAssociations;
	}
	
}

