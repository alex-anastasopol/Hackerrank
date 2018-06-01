package ro.cst.tsearch.search.filter.newfilters.date;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocument;

public class DateFilterWithDoctype extends FilterResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected Date referenceDate = null;
	protected Calendar referenceCalendar = null;
	protected boolean allowNewerAndForbidOlder = true;
	protected Set<String> applyToDoctypes = null;

	public DateFilterWithDoctype(long searchId) {
		super(searchId);
		this.threshold = BigDecimal.ONE;
	}

	public Date getReferenceDate() {
		return referenceDate;
	}

	public DateFilterWithDoctype setReferenceDate(Date referenceDate) {
		this.referenceDate = referenceDate;
		if(this.referenceDate == null) {
			this.referenceCalendar = null;
		} else {
			this.referenceCalendar = Calendar.getInstance();
			this.referenceCalendar.setTime(this.referenceDate);
		}
		return this;
	}

	public boolean isAllowNewerAndForbidOlder() {
		return allowNewerAndForbidOlder;
	}

	public DateFilterWithDoctype setAllowNewerAndForbidOlder(boolean allowNewerAndForbidOlder) {
		this.allowNewerAndForbidOlder = allowNewerAndForbidOlder;
		return this;
	}
	
	public Set<String> getApplyToDoctypes() {
		return applyToDoctypes;
	}

	public DateFilterWithDoctype setApplyToDoctypes(Set<String> applyToDoctypes) {
		this.applyToDoctypes = applyToDoctypes;
		return this;
	}

	@Override
	public String getFilterCriteria() {
		if(referenceDate == null) {
			return "Date. No reference date. All will pass";
		}
		String result = "Date. Allowing documents " + (allowNewerAndForbidOlder?"after ":"before ") + SearchAttributes.DEFAULT_DATE_PARSER.format(referenceDate);
		if(applyToDoctypes != null) {
			result += " with category " + Arrays.toString(applyToDoctypes.toArray()) + " and all other categories";
		}
		return result;
	}

	@Override
	public String getFilterName() {
		return "Filter by Date";
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		if(getReferenceDate() == null) {
			return BigDecimal.ONE;
		}
		DocumentI doc = row.getDocument();
		if(doc != null){
			if (doc instanceof RegisterDocument) {
				RegisterDocument registerDocument = (RegisterDocument) doc;
				if(getApplyToDoctypes() != null) {
					if(!getApplyToDoctypes().contains(registerDocument.getDocType())) {
						//this filter will not apply to this kind of document
						return BigDecimal.ONE;
					}
				}
				
				Date recordedDate = registerDocument.getRecordedDate();
				if(recordedDate==null)
					recordedDate = registerDocument.getInstrumentDate();
				if(recordedDate!=null) {
					if(recordedDate.after(getReferenceDate()) && !isAllowNewerAndForbidOlder()) {
						return BigDecimal.ZERO;
					}
					if(recordedDate.before(getReferenceDate()) && isAllowNewerAndForbidOlder()) {
						return BigDecimal.ZERO;
					}
					return BigDecimal.ONE;
				}
			} else {
				if(doc.getYear() > referenceCalendar.get(Calendar.YEAR) && !isAllowNewerAndForbidOlder()) {
					return BigDecimal.ZERO;
				}
				if(doc.getYear() < referenceCalendar.get(Calendar.YEAR) && isAllowNewerAndForbidOlder()) {
					return BigDecimal.ZERO;
				}
				return BigDecimal.ONE;
			}
		} else {
			logger.error("There was an error using DateFilterWithDoctype, document to test is null");
		}
		return BigDecimal.ONE;
	}

}
