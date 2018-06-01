package ro.cst.tsearch.search.iterator.instrument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

public class InstrumentAKROIterator extends InstrumentGenericIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public InstrumentAKROIterator(long searchId) {
		super(searchId);
		enableInstrumentNumber();
	}
	/**
	 * Constructor use to enable/disable BookPage while disabling/enabling InstrumentSearch 
	 * @param searchId
	 * @param enableBookPage
	 */
	public InstrumentAKROIterator(long searchId, boolean enableBookPage) {
		super(searchId);
		enableBookPage();
	}
	
	protected void useInstrumentI(List<InstrumentI> result,
			HashSet<String> listsForNow, DocumentsManagerI manager,
			InstrumentI instrumentI) {
		
		String instrumentNo = instrumentI.getInstno();
		String instrumentYear = Integer.toString(instrumentI.getYear());
		if(StringUtils.isNotEmpty(instrumentNo)) {
			if( instrumentI.getYear() !=  SimpleChapterUtils.UNDEFINED_YEAR) {
				instrumentNo = cleanInstrumentNo(instrumentNo, instrumentYear);
				if(instrumentNo.matches("\\d{4}-\\d{4}") ) {
					if(isEnableBookPage()) {
						String book = instrumentNo.substring(0, 4).replaceFirst("^0+", "");
						String page = instrumentNo.substring(5).replaceFirst("^0+", "");
						if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
							String key = "Book=" + book + "_Page=" + page;
							if(!listsForNow.contains(key)) {
								InstrumentI instrumentClone = instrumentI.clone();
								instrumentClone.setBook(book);
								instrumentClone.setPage(page);
								instrumentClone.setInstno("");
								instrumentClone.setYear(SimpleChapterUtils.UNDEFINED_YEAR);
								//if has the same instrumentNumber we will not save it
								List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentClone);
								if(almostLike.isEmpty()) {
									listsForNow.add(key);
									result.add(instrumentClone);
								}
							}
						}
					}
				} else {
					if(isEnableInstrumentNumber() && instrumentNo.matches("\\d+")) {
						String instrumentSuffix = "0";
						String key = "Instrument=" + instrumentNo + "___year=" + instrumentYear + "___suffix=" + instrumentSuffix ;
						if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
							
							String instrumentToCheck = instrumentYear + instrumentNo ;
							if(!"0".equals(instrumentSuffix)) {
								instrumentToCheck += instrumentSuffix;
							}
							
							instrumentI.setInstno(instrumentToCheck);
							List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
			    			if(almostLike.isEmpty()) {
			    				listsForNow.add(key);
			    				instrumentI.setInstno( instrumentYear + "-" + instrumentNo + "-" + instrumentSuffix );
			    				result.add(instrumentI);
			    			}
						}
					}
				}
			} else {
				if(isEnableInstrumentNumber() && instrumentNo.matches("\\d{12}")) {
					String instrumentSuffix = "0";
					if(instrumentNo.length() == 9) {
						instrumentSuffix = instrumentNo.substring(8);
					}
					instrumentYear = instrumentNo.substring(0, 4);
					instrumentNo = instrumentNo.substring(4, 8);
					String key = "Instrument=" + instrumentNo + "___year=" + instrumentYear + "___suffix=" + instrumentSuffix ;
					if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
						
						String instrumentToCheck = instrumentYear + instrumentNo ;
						if(!"0".equals(instrumentSuffix)) {
							instrumentToCheck += instrumentSuffix;
						}
						
						instrumentI.setInstno(instrumentToCheck);
						List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
		    			if(almostLike.isEmpty()) {
		    				listsForNow.add(key);
		    				instrumentI.setInstno( instrumentYear + "-" + instrumentNo + "-" + instrumentSuffix );
		    				result.add(instrumentI);
		    			}
					}
				}
				if(isEnableBookPage() && instrumentNo.matches("\\d{12}")) {
					String book = instrumentNo.substring(0, 4).replaceFirst("^0+", "");
					String page = instrumentNo.substring(4).replaceFirst("^0+", "");
					if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
						String key = "Book=" + book + "_Page=" + page;
						if(!listsForNow.contains(key)) {
							InstrumentI instrumentClone = instrumentI.clone();
							instrumentClone.setBook(book);
							instrumentClone.setPage(page);
							instrumentClone.setInstno("");
							instrumentClone.setYear(SimpleChapterUtils.UNDEFINED_YEAR);
							//if has the same instrumentNumber we will not save it
							List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentClone);
							if(almostLike.isEmpty()) {
								listsForNow.add(key);
								result.add(instrumentClone);
							}
						}
					}
				}
			}
		}
		
		if(isEnableBookPage()) {
			String book = instrumentI.getBook();
			String page = instrumentI.getPage();
			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
				String key = "Book=" + book + "_Page=" + page;
				if(!listsForNow.contains(key)) {
					//if has the same instrumentNumber we will not save it
					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					if(almostLike.isEmpty()) {
						listsForNow.add(key);
						result.add(instrumentI);
					}
				}
			}
		}
	}
	
	private static String cleanInstrumentNo(String instrumentNo, String instrumentYear) {
		if(StringUtils.isNotEmpty(instrumentNo)  && StringUtils.isNotEmpty(instrumentYear)) {
			instrumentNo = instrumentNo.replaceFirst("^0+", "").replace("_", "");
			if(instrumentNo.startsWith(instrumentYear)) {
				instrumentNo = instrumentNo.replaceFirst(instrumentYear, "");
				instrumentNo = instrumentNo.replace("/", "").replaceFirst("^0+", "");
			}
			if(!instrumentNo.matches("\\d+")) {
				return null;
			}
			int length = instrumentNo.length();
			if(length < 6) {
				instrumentNo = org.apache.commons.lang.StringUtils.leftPad(instrumentNo, 6, "0");
			}
			if(length == 7 || length == 8 ) {
				instrumentNo = org.apache.commons.lang.StringUtils.leftPad(instrumentNo, 8, "0");
				instrumentNo = instrumentNo.substring(0, 4) + "-" + instrumentNo.substring(4);
				
			}
		}
		return instrumentNo;
	}
	
	@Override
	public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(state.getInstno().matches("\\d+-\\d+-\\d")) {
			return state.getInstno().replaceAll("\\d+-(\\d+)-.*", "$1");
		}
		return "";
	}
	@Override
	public String getYearFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(state.getInstno().matches("\\d+-\\d+-\\d")) {
			return state.getInstno().replaceAll("(\\d+)-.*", "$1");
		}
		return "";
	}
	@Override
	public String getSuffixFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(state.getInstno().matches("\\d+-\\d+-\\d")) {
			String result = state.getInstno().replaceAll("\\d+-\\d+-(\\d)", "$1");
			if(filterCriteria != null) {
				if("0".equals(result)) {
					filterCriteria.put("InstrumentNumber", state.getInstno().replaceAll("(\\d+)-(\\d+)-.*", "$1_$2"));
				} else {
					filterCriteria.put("InstrumentNumber", state.getInstno().replaceAll("(\\d+)-(\\d+)-(\\d+)", "$1_$2$3"));
				}
			}
			return result;
		}
		return "";
	}
	
	
	public static void main(String[] args) {
		String[] rows = {"JsT0123131","R2132","T21000","F21312"};
		for (int i = 0; i < rows.length; i++) {
			System.out.println(rows[i] + " --- " + rows[i].replaceAll("(?i)^[A-Z]+0*", ""));
		}
		System.out.println("C0361951 - 1998 ==> " + cleanInstrumentNo("C0361951", "1998"));
		System.out.println("C1004235 - 2002 ==> " + cleanInstrumentNo("C1004235", "2002"));
		System.out.println("08000062082 - 2008 ==> " + cleanInstrumentNo("08000062082", "2008"));
	}

	

}
