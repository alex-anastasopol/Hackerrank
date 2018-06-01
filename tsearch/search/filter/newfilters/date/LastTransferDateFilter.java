package ro.cst.tsearch.search.filter.newfilters.date;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author Cristian Stochina
 * 
 * Generic class for filtering documents using last transfer date 
 * This class  should be used with all grantee/grantor sites (the base search is name search) servers.
 * 
 * Think twice before using this filter with a search different than "name search" and please set  "isNameSearch = false "
 * 
 * Make sure on Grantee/Grantor RO sites you always make the references search first  (AO and TR cross-references) 
 * before using this filter for the name Search.
 * 
 */
public class LastTransferDateFilter extends FilterResponse {

	private static final long serialVersionUID = 8049413332296989430L;
	
	private Set<String> alwaysIgnoredDocTypes = new HashSet<String>();
	
	/** for these documents we will apply special rules and we will not use last transfer filter */
	private static final String[] ignoredDocTypes = { 
		DocumentTypes.TRANSFER 
	};
	
	/** for these documents we will apply special rules and we will not use last transfer filter */
	private static final String[] docTypesThatPassForGoodNameGrantor = { 
		DocumentTypes.MORTGAGE, 
		DocumentTypes.RELEASE 
	};
	
	/** for these documents we will apply special rules and we will not use last transfer filter */
	private static final String[] docTypesThatPassForGoodNameGrantee = { 
		DocumentTypes.RELEASE
	};
	
	private String[] aditionalDocTypesThatPassForGoodName = null;
	
	private static final int lastTransferDaysBack = 20;
	public static final int DO_NOT_CARE			=	0;
	public static final int NAME_MODULE			=	1;
	private int moduleSearchType = DO_NOT_CARE;

	private boolean ignoreMiddleOnEmpty = false;

	private boolean isNameSearch = true;
	
	private boolean useDefaultDocTypeThatPassForGoodName = true;
	
	public LastTransferDateFilter(long searchId) {
		super(searchId);
		super.threshold = BigDecimal.ONE;
		alwaysIgnoredDocTypes.add(DocumentTypes.PLAT);
		alwaysIgnoredDocTypes.add(DocumentTypes.EASEMENT);
		alwaysIgnoredDocTypes.add(DocumentTypes.RESTRICTION);
		alwaysIgnoredDocTypes.add(DocumentTypes.MASTERDEED);
		alwaysIgnoredDocTypes.add(DocumentTypes.CCER);
		alwaysIgnoredDocTypes.add(DocumentTypes.LIEN);
		alwaysIgnoredDocTypes.add(DocumentTypes.COURT);
	}

	@SuppressWarnings("unchecked")
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		try{
			
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
			String docType = null;
			
			if(row.getSaleDataSetsCount() > 0) {
				docType = row.getSaleDataSet(0).getAtribute("DocumentType").trim();
			} else {
				if(row.getDocument() != null) {
					docType = row.getDocument().getServerDocType();
				}
			}
			
			if(StringUtils.isBlank(docType)) {
				return BigDecimal.ONE;
			}
			
			if(DocumentTypes.isOfDocType(docType, alwaysIgnoredDocTypes.toArray(new String[0]),searchId)){
				return BigDecimal.ONE;
			}
			
			
			Set<NameI> refList = search.getSa().getOwners().getNames();
			
			Vector<NameSet> grantors = (Vector<NameSet>) row.getGrantorNameSet();
			Vector<NameSet> grantees = (Vector<NameSet>) row.getGranteeNameSet();
			
			
			if(	moduleSearchType==DO_NOT_CARE	) {
				/* let it pass if it is a MORTGAGE or LIEN or RELEASE with good names */
				if ((isUseDefaultDocTypeThatPassForGoodName() && DocumentTypes.isOfDocType(docType, docTypesThatPassForGoodNameGrantor, searchId)) 
						|| (aditionalDocTypesThatPassForGoodName!=null && DocumentTypes.isOfDocType(docType, aditionalDocTypesThatPassForGoodName, searchId)) ) {
					for (NameSet grantor : grantors) {
						NameI cand = new Name(
								grantor.getAtribute("OwnerFirstName"),
								grantor.getAtribute("OwnerMiddleName"),
								grantor.getAtribute("OwnerLastName"));
						
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ONE;
						}
						
						cand = new Name(
								grantor.getAtribute("SpouseFirstName"),
								grantor.getAtribute("SpouseMiddleName"),
								grantor.getAtribute("SpouseLastName"));
						
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ONE;
						}
						
					}
				}
				if (isUseDefaultDocTypeThatPassForGoodName() && DocumentTypes.isOfDocType(docType, docTypesThatPassForGoodNameGrantee, searchId)  ) {
					for (NameSet grantee : grantees) {
						NameI cand = new Name(
								grantee.getAtribute("OwnerFirstName"), 
								grantee.getAtribute("OwnerMiddleName"),
								grantee.getAtribute("OwnerLastName"));
				
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ONE;
						}
						
						cand = new Name(
								grantee.getAtribute("SpouseFirstName"), 
								grantee.getAtribute("SpouseMiddleName"),
								grantee.getAtribute("SpouseLastName"));
						
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ONE;
						}
						
					}
				}
			}
			
			boolean test = !DocumentTypes.isOfDocType(docType, ignoredDocTypes, searchId);
			
			if ( test || isNameSearch) {
				return calculateScore(search, row);
			}else {
				if ( calculateScore(search, row).equals(BigDecimal.ZERO)  ) {
					
					/* reject old transfers that have grantee or grantor equals with current owner */
					for (NameSet grantee : grantees) {
						
						
						NameI cand = new Name(
								grantee.getAtribute("OwnerFirstName"), 
								grantee.getAtribute("OwnerMiddleName"),
								grantee.getAtribute("OwnerLastName"));
				
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ZERO;
						}
						
						cand = new Name(
								grantee.getAtribute("SpouseFirstName"), 
								grantee.getAtribute("SpouseMiddleName"),
								grantee.getAtribute("SpouseLastName"));
						
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ZERO;
						}
					}
	
					for (NameSet grantor : grantors) {
						NameI cand = new Name(
								grantor.getAtribute("OwnerFirstName"),
								grantor.getAtribute("OwnerMiddleName"),
								grantor.getAtribute("OwnerLastName"));
						
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ZERO;
						}
						
						cand = new Name(
								grantor.getAtribute("SpouseFirstName"),
								grantor.getAtribute("SpouseMiddleName"),
								grantor.getAtribute("SpouseLastName"));
						
						if(!cand.isEmpty() && GenericNameFilter.isMatchGreaterThenScore(refList, cand, 0.8, ignoreMiddleOnEmpty)) {
							return BigDecimal.ZERO;
						}
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace() ;
		}

		return BigDecimal.ONE;
	}

	@SuppressWarnings("unchecked")
	private BigDecimal calculateScore(Search search, ParsedResponse row) {
		
		
		DocumentsManagerI manager = search.getDocManager();
		Date lastTransferDate = null;
		try{
			manager.getAccess();
			//TransferI tr = manager.getLastRealTransferWithGoodName(false);
			TransferI tr = manager.getLastRealTransfer();
			if(tr!=null){
				lastTransferDate = (Date)tr.getRecordedDate().clone();
			}
		}
		finally{
			manager.releaseAccess();
		}
		
		
		if (lastTransferDate == null) {
			return BigDecimal.ONE;
		}

		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		cal.setTime(lastTransferDate);
		cal.add(GregorianCalendar.DAY_OF_MONTH, -lastTransferDaysBack);

		Date candRecordedDate = null;
		
		if(row.getSaleDataSetsCount() > 0) {
			Vector<SaleDataSet> saleDataSets = (Vector<SaleDataSet>) (row.infVectorSets.get("SaleDataSet"));
			SaleDataSet saleDataSetInfo = (SaleDataSet) saleDataSets.elementAt(0);
			String candRecordedDateString = saleDataSetInfo.getAtribute("RecordedDate");
			
			try {
				candRecordedDate = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).parse(candRecordedDateString);
			} catch (Exception e) {
			}
			
		} else {
			if(row.getDocument() != null && row.getDocument() instanceof RegisterDocumentI) {
				candRecordedDate = ((RegisterDocumentI)row.getDocument()).getRecordedDate();
			}
		}

		if (candRecordedDate == null) {
			return BigDecimal.ONE;
		}

		if (candRecordedDate.before(cal.getTime())) {
			return BigDecimal.ZERO;
		} else {
			return BigDecimal.ONE;
		}
	}

	@Override
	public String getFilterCriteria() {
		DocumentsManagerI manager = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
		Date lastTransferDate = null;
		try{
			manager.getAccess();
			//TransferI tr = manager.getLastRealTransferWithGoodName(false);
			TransferI tr = manager.getLastRealTransfer();
			if(tr!=null){
				lastTransferDate = (Date)tr.getRecordedDate().clone();
			}
		}
		finally{
			manager.releaseAccess();
		}
		
		if (lastTransferDate == null) {
			return "LAST TRANSFER DATE (ACCEPT ALL, COULD NOT DETECT LAST GOOD TRANSFER)";
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		return "Last Transfer Date: " + sdf.format(lastTransferDate);
	}

	public int getModuleSearchType() {
		return moduleSearchType;
	}

	public void setModuleSearchType(int moduleSearchType) {
		this.moduleSearchType = moduleSearchType;
	}

	public String[] getAditionalDocTypesThatPassForGoodName() {
		return aditionalDocTypesThatPassForGoodName;
	}

	public void setAditionalDocTypesThatPassForGoodName(String[] aditionalDocTypesThatPassForGoodName) {
		this.aditionalDocTypesThatPassForGoodName = aditionalDocTypesThatPassForGoodName;
	}

	public boolean isIgnoreMiddleOnEmpty() {
		return ignoreMiddleOnEmpty;
	}

	public void setIgnoreMiddleOnEmpty(boolean ignoreMiddleOnEmpty) {
		this.ignoreMiddleOnEmpty = ignoreMiddleOnEmpty;
	}

	public Set<String> getAlwaysIgnoredDocTypes() {
		return alwaysIgnoredDocTypes;
	}

	public void setAlwaysIgnoredDocTypes(Set<String> alwaysIgnoredDocTypes) {
		this.alwaysIgnoredDocTypes = alwaysIgnoredDocTypes;
	}
	
	public boolean isNameSearch() {
		return isNameSearch;
	}

	public void setNameSearch(boolean isNameSearch) {
		this.isNameSearch = isNameSearch;
	}

	/**
	 * @return the useDefaultDocTypeThatPassForGoodName
	 */
	public boolean isUseDefaultDocTypeThatPassForGoodName() {
		return useDefaultDocTypeThatPassForGoodName;
	}

	/**
	 * @param useDefaultDocTypeThatPassForGoodName the useDefaultDocTypeThatPassForGoodName to set
	 */
	public void setUseDefaultDocTypeThatPassForGoodName(boolean useDefaultDocTypeThatPassForGoodName) {
		this.useDefaultDocTypeThatPassForGoodName = useDefaultDocTypeThatPassForGoodName;
	}
	
}
