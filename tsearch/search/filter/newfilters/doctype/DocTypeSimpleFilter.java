package ro.cst.tsearch.search.filter.newfilters.doctype;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

/**
 * Filters documents based only on their DocType allowing those that have the exact same DocumentCategory<br>
 * No other rules are applied just simple and direct matching
 * @author Cristi Stochina
 */
public class DocTypeSimpleFilter extends FilterResponse{

	protected String []docTypes = {
			DocumentTypes.MISCELLANEOUS,
			DocumentTypes.LIEN,
			DocumentTypes.COURT,
			DocumentTypes.AFFIDAVIT};
	
	protected String [] subDocTypes = {};
	
	protected boolean useSubCategory = false;
	
	private static final long serialVersionUID = -9037371961637555360L;

	public DocTypeSimpleFilter(long searchId){
		super(searchId);
		threshold = BigDecimal.ONE;
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {	
		try{
			String docType = null;
			String category = null;
			if(row.getSaleDataSetsCount() > 0) { 
				docType = row.getSaleDataSet(0).getAtribute("DocumentType").trim();
				if(StringUtils.isBlank(docType)) {
			        if(row.getDocument() != null) {
			        	docType = row.getDocument().getServerDocType();
			        }
				}
			} else {
				if(row.getDocument() != null) {
		        	docType = row.getDocument().getServerDocType();
		        	category = row.getDocument().getDocType();
		        }
			}
			
			if(docTypes != null && category != null) {
				for (String permittedCategory : docTypes) {
					if(permittedCategory != null && permittedCategory.equalsIgnoreCase(category)) {
						return BigDecimal.ONE;
					}
				}
			}
			
			if( DocumentTypes.isOfDocType(docType, docTypes,searchId) ){
				if (useSubCategory()){ 
					if (DocumentTypes.isOfSubDocType(docType, subDocTypes, searchId)){
						return BigDecimal.ONE;
					} else{
						return BigDecimal.ZERO;
					}
				}
	        	return BigDecimal.ONE;
	        }
	        return BigDecimal.ZERO;
        }
        catch(Exception e){
        	e.printStackTrace();
        }	
		return BigDecimal.ZERO;
	}

	public String[] getDocTypes() {
		return docTypes;
	}

	public void setDocTypes(String[] docTypes) {
		this.docTypes = docTypes;
	}
	
	
    public String getFilterName(){
    	return "DocType";
    }
    
    
    public String getFilterCriteria(){
    	String ret = "Doctype: ";
		for (int i = 0; i < docTypes.length; i++) {
			if(i == 0) {
				ret += docTypes[i];
			} else {
				ret += ", " + docTypes[i];
			}
		}
		if (useSubCategory() && subDocTypes!= null && subDocTypes.length > 0){
			ret += " and SubCategory: ";
			for (int i = 0; i < subDocTypes.length; i++) {
				if (i == 0) {
					ret += subDocTypes[i];
				} else {
					ret += ", " + subDocTypes[i];
				}
			}
		}
    	return ret;
    }

	/**
	 * @return the subDocTypes
	 */
	public String [] getSubDocTypes() {
		return subDocTypes;
	}

	/**
	 * @param subDocTypes the subDocTypes to set
	 */
	public void setSubDocTypes(String [] subDocTypes) {
		this.subDocTypes = subDocTypes;
	}

	/**
	 * @return the useSubCategory
	 */
	public boolean useSubCategory() {
		return useSubCategory;
	}

	/**
	 * @param useSubCategory the useSubCategory to set
	 */
	public void setUseSubCategory(boolean useSubCategory) {
		this.useSubCategory = useSubCategory;
	}
	
}
