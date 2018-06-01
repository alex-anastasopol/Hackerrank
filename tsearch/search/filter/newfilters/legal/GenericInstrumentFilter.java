package ro.cst.tsearch.search.filter.newfilters.legal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.InstrumentI;

public class GenericInstrumentFilter extends FilterResponse {
	
	public static final String GENERIC_INSTRUMENT_FILTER_KEY_BOOK = "Book";
	public static final String GENERIC_INSTRUMENT_FILTER_KEY_PAGE = "Page";
	public static final String GENERIC_INSTRUMENT_FILTER_KEY_DOCTYPE = "DocumentType";
	public static final String GENERIC_INSTRUMENT_FILTER_KEY_INSTRUMENT_NUMBER = "InstrumentNumber";
	public static final String GENERIC_INSTRUMENT_FILTER_KEY_MULTIPLE_INSTRUMENT_NUMBER = "MultipleInstrumentNumber";
	
	private static final long serialVersionUID = 123L;
	
	private boolean checkForDoctype = false;
	
	private List<HashMap<String, String>> filters = new ArrayList<HashMap<String, String>>();
	
	public GenericInstrumentFilter(long searchId) {
		super(searchId);
		threshold = ATSDecimalNumberFormat.ONE;
	}
	
	public GenericInstrumentFilter(long searchId, HashMap<String, String> filters) {
		this(searchId);
		threshold = ATSDecimalNumberFormat.ONE;
		this.filters.add(filters);
	}
	
	public void clearFilters() {
		filters.clear();
	}
	
	public int getNoOfFilters() {
		return filters.size();
	}

	public void addDocumentCriteria(HashMap<String, String> filters) {
		if(filters!=null) {
			this.filters.add(filters);
		}
	}
	
	public String getCandidateBook(String book, String refBook){
		return book;
	}
	
	public String getCandidatePage(String page){
		return page;
	}
	
	public String getFilterBook(String book){
		return book;
	}
	
	public String getFilterPage(String page){
		return page;
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row)
	{
		InstrumentI candidate = null;
		if(row.getDocument() != null && filters.size() > 0) {
			candidate = row.getDocument().getInstrument();
		}
		if(candidate == null || filters.size() == 0) {
			return ATSDecimalNumberFormat.ONE;
		}
		boolean testedSomething = false;
		for (HashMap<String, String> filter : filters) {
			String tempValue = getFilterBook(filter.get(GENERIC_INSTRUMENT_FILTER_KEY_BOOK));
			boolean matchBook = false;
			if(StringUtils.isNotEmpty(tempValue) && StringUtils.isNotEmpty(candidate.getBook())) {
				testedSomething = true;
				if(tempValue.equalsIgnoreCase(getCandidateBook(candidate.getBook(), tempValue))) {
					matchBook = true;
				}
			}
			tempValue = getFilterPage(filter.get(GENERIC_INSTRUMENT_FILTER_KEY_PAGE));
			if(StringUtils.isNotEmpty(tempValue) && StringUtils.isNotEmpty(candidate.getPage())) {
				testedSomething = true;
				if(tempValue.equalsIgnoreCase(getCandidatePage(candidate.getPage()))) {
					if(matchBook) {
						if (isCheckForDoctype()){
							String book = getFilterBook(filter.get(GENERIC_INSTRUMENT_FILTER_KEY_BOOK));
							if (book.equalsIgnoreCase(candidate.getBook())){
								return ATSDecimalNumberFormat.ONE;
							}
							if (checkForDoctype(candidate, filter)){
								return ATSDecimalNumberFormat.ONE;
							} else{
								return ATSDecimalNumberFormat.ZERO;
							}
						} else{
							return ATSDecimalNumberFormat.ONE;
						}
					}
				} 
			}
			tempValue = filter.get(GENERIC_INSTRUMENT_FILTER_KEY_INSTRUMENT_NUMBER);
			if(StringUtils.isNotEmpty(tempValue) && StringUtils.isNotEmpty(candidate.getInstno())) {
				testedSomething = true;
				if(tempValue.equalsIgnoreCase(candidate.getInstno())) {
					return ATSDecimalNumberFormat.ONE;
				}
			}
						
			tempValue = filter.get(GENERIC_INSTRUMENT_FILTER_KEY_MULTIPLE_INSTRUMENT_NUMBER);
			if(StringUtils.isNotEmpty(tempValue) && StringUtils.isNotEmpty(candidate.getInstno())) {
				String[] tempValues = tempValue.split("\\s*,\\s*");
				for (String value : tempValues) {
					testedSomething = true;
					
					if(value.equalsIgnoreCase(candidate.getInstno())) {
						return ATSDecimalNumberFormat.ONE;
					}
				}
			}
		}
		if(testedSomething) {
			return ATSDecimalNumberFormat.ZERO;
		} else {
			return ATSDecimalNumberFormat.ONE;
		}
	}

	/**
	 * @param candidate
	 * @param testedSomething
	 * @param filter
	 * @return
	 */
	public boolean checkForDoctype(InstrumentI candidate, HashMap<String, String> filter) {
		
		String tempValue = filter.get(GENERIC_INSTRUMENT_FILTER_KEY_DOCTYPE);
		if (StringUtils.isNotEmpty(tempValue)){
			if (!DocumentTypes.getDocumentCategory(tempValue, searchId).equals(DocumentTypes.MISCELLANEOUS)){
				
				if (StringUtils.isNotEmpty(candidate.getDocType())) {
					if (tempValue.equalsIgnoreCase(candidate.getDocType())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public String getFilterCriteria() {
		return "GenericInstrumentFilter. Allowing documents with the same instrument/book-page";
	}

	@Override
	public String getFilterName() {
		return "Filter by Instrument/Book-page";
	}

	/**
	 * @return the checkForDoctype
	 */
	public boolean isCheckForDoctype() {
		return checkForDoctype;
	}

	/**
	 * @param checkForDoctype the checkForDoctype to set
	 */
	public void setCheckForDoctype(boolean checkForDoctype) {
		this.checkForDoctype = checkForDoctype;
	}
	

}
