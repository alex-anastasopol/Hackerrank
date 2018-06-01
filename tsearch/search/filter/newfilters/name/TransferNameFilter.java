package ro.cst.tsearch.search.filter.newfilters.name;

import java.math.BigDecimal;
import java.util.Vector;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.GrantorSet;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;

public class TransferNameFilter extends GenericNameFilter {
	private static final long serialVersionUID = 1000000000L;

	public TransferNameFilter(long searchId) {
		super("", searchId);
	}

	public TransferNameFilter(String key, long searchId, boolean useSubdivisionName, TSServerInfoModule module) {
		super(key, searchId, useSubdivisionName, module, true);
	}

	//
	// This is a filter that must be used with a low pass strategy, it spits out
	// transfers where the grantor was searched previously;
	//
	@SuppressWarnings("unchecked")
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		double scoreMax = 0;
		double score = 0;
		String docType = null;
		Vector grantors = row.getGrantorNameSet();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		if (row.getSaleDataSetsCount() > 0) {
			docType = row.getSaleDataSet(0).getAtribute("DocumentType").trim();
		} else {
			if(row.getDocument() != null) {
				docType = row.getDocument().getServerDocType();
			}
		}
		if (DocumentTypes.checkDocumentType(docType, DocumentTypes.TRANSFER_INT,null, searchId)) {
			if("transfer".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType,  searchId))){
				for (NameI element:gbm.getGBHistoryNames(searchId) ) {
					for (int i = 0; i < row.getGrantorNameSetCount(); i++) {
						Object obj = grantors.get(i);
						if (obj instanceof GrantorSet) {
							GrantorSet element2 = (GrantorSet) obj;
							score = calculateScore(new Name(element2.getAtribute("OwnerFirstName"), element2.getAtribute("OwnerMiddleName"), element2.getAtribute("OwnerLastName") ),element);//calculateScore(ref, cand, true);
							if (score > scoreMax)
								scoreMax = score;
						}
					}
				}
			}
			return new BigDecimal(scoreMax);
		} else
			return new BigDecimal(0.0);
	}

	@Override
	public String getFilterName() {
		return "Filter Transfers by Grantors that were already searched with";
	}

	@Override
	public String getFilterCriteria() {
		return super.getFilterCriteria() + "'Filter Transfers by Grantors that were already searched with";
	}

}