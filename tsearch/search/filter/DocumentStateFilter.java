package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.Vector;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

public class DocumentStateFilter extends FilterResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2969524211088746309L;
	private DocumentState whatToReject = DocumentState.DELETED;

	public DocumentStateFilter(long searchId, DocumentState rejectable) {
		super(searchId);
		whatToReject = rejectable;
	}

	public DocumentStateFilter(String key, long searchId, DocumentState rejectable) {
		super(key, searchId);
		whatToReject = rejectable;
	}

	public DocumentStateFilter(SearchAttributes sa1, String key, long searchId, DocumentState rejectable) {
		super(sa1, key, searchId);
		whatToReject = rejectable;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score = ATSDecimalNumberFormat.ONE;
		Vector<PropertyIdentificationSet> propertyIdentificationSet = row.getPropertyIdentificationSet();
		for (PropertyIdentificationSet pis : propertyIdentificationSet) {
			String status = pis.getAtribute(PropertyIdentificationSetKey.STATUS.getShortKeyName());
			if (status.toUpperCase().contains(whatToReject.getStatusName())){
				score = ATSDecimalNumberFormat.ZERO;
			}
		}

		return score;
	}

	@Override
	public String getFilterName() {
		return "Filter by Document Status";
	}

	@Override
	public String getFilterCriteria() {
		String returnValue = "Type='" + DocumentState.DELETED.getStatusName() + "'";
		if (whatToReject == DocumentState.INACTIVE) {
			returnValue = "Type='" + DocumentState.INACTIVE.getStatusName() + "'";
		}
		if (whatToReject == DocumentState.ACTIVE) {
			returnValue = "Type='" + DocumentState.ACTIVE.getStatusName() + "'";
		}
		return returnValue;
	}

	public enum DocumentState {
		DELETED("DELETED"), ACTIVE("ACTIVE"), INACTIVE("INACTIVE");

		private String statusName;

		DocumentState(String v) {
			statusName = v;
		}

		public String getStatusName() {
			return statusName;
		}

		public void setStatusName(String statusName) {
			this.statusName = statusName;
		}

	}

}
