package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;

import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

public class GranteeDoctypeFilterResponse extends GenericNameFilter {

	private static final long serialVersionUID = 402721968580035780L;

	public GranteeDoctypeFilterResponse(long searchId) {
		super(SearchAttributes.OWNER_OBJECT, searchId);
		setFilterPartyType(PType.GRANTEE);
		setInitAgain(true);
	}

	public GranteeDoctypeFilterResponse(String key, long searchId, boolean useSubdivisionName,
			TSServerInfoModule module, boolean ignoreSuffix, int stringCleaner) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix, stringCleaner);
		setFilterPartyType(PType.GRANTEE);
	}

	public GranteeDoctypeFilterResponse(String key, long searchId, boolean useSubdivisionName,
			TSServerInfoModule module, boolean ignoreSuffix) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix);
		setFilterPartyType(PType.GRANTEE);
	}

	public GranteeDoctypeFilterResponse(String key, long searchId) {
		super(key, searchId);
		setFilterPartyType(PType.GRANTEE);
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		String docType = null;
		if(row.getSaleDataSetsCount() > 0) { 
			docType = row.getSaleDataSet(0).getAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName()).trim();
			if(StringUtils.isBlank(docType)) {
		        if(row.getDocument() != null) {
		        	docType = row.getDocument().getServerDocType();
		        }
			}
		} else {
			if(row.getDocument() != null) {
	        	docType = row.getDocument().getServerDocType();
	        }
		}
		
		String documentCategory = DocumentTypes.getDocumentCategory( docType,searchId );
        
        if( DocumentTypes.MORTGAGE.equals(documentCategory) || DocumentTypes.ASSIGNMENT.equals(documentCategory))
        {
        	BigDecimal scoreOneRow = super.getScoreOneRow(row);
        	if(scoreOneRow.doubleValue() > getThreshold().doubleValue()) {
        		return ATSDecimalNumberFormat.ZERO;
        	}
        }
		
        return ATSDecimalNumberFormat.ONE; 
		
	}
	
	@Override
	public String getFilterName() {
		return "Grantee DocType";
	}
	
	@Override
	public String getFilterCriteria(){
		String result = "Name='" + getReferenceNameString() + "'";
		String nickResult = getReferenceNickNameString();
		if(StringUtils.isNotEmpty(nickResult)) {
			result += " and NickNames='" + nickResult + "' ";
		}
		
		if(pondereMiddle==0.0) {
			result += "(ignoring middle name)";
		} else {
			if(isIgnoreMiddleOnEmpty() && isIgnoreEmptyMiddleOnCandidat()) {
				result += "(ignoring middle name when empty on reference or candidate)";	
			} else if (isIgnoreMiddleOnEmpty()) {
				result += "(ignoring middle name when empty on reference)";
			} else if (isIgnoreEmptyMiddleOnCandidat()) {
				result += "(ignoring middle name when empty on candidate)";
			}
		}
		
		result = result + (enableTrusteeCheck?"(checking trustee match)":"");
		
		result += " for Doctype " + DocumentTypes.MORTGAGE + "/" + DocumentTypes.ASSIGNMENT + " - invalidate if owner in grantee field";
				
		return  result;
    }

}
