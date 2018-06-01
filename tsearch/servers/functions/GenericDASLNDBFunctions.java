package ro.cst.tsearch.servers.functions;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.types.GenericSKLD;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
*/

public class GenericDASLNDBFunctions {

	public static ResultMap improveCrossRefsParsing(ResultMap resultMap, Search search){
		
		String instrNo = (String) resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());
		String book = (String) resultMap.get(SaleDataSetKey.BOOK.getKeyName());
		String page = (String) resultMap.get(SaleDataSetKey.PAGE.getKeyName());
			
		String finInstNo = (String) resultMap.get(SaleDataSetKey.FINANCE_INST_NO.getKeyName());
		String finRecDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
		if (book == null){
			book = "";
		}
		if (page == null){
			page = "";
		}
		if (instrNo == null){
			instrNo = "";
		}
			
		String crtCounty = search.getSa().getCountyName();
		String crtState = search.getSa().getAtribute(SearchAttributes.P_STATE_ABREV);
			
		if ("MO".equals(crtState)){
			correctCrossRefMO(resultMap, instrNo, book, page, search);
		} else if ("TN".equals(crtState)){
			correctCrossRefTN(resultMap, instrNo, book, page, finInstNo, finRecDate, search);
		} else if ("TX".equals(crtState)){
			correctCrossRefTX(resultMap, instrNo, finInstNo, finRecDate, search);
		} else if ("IL".equals(crtState)){
			correctCrossRefIL(resultMap, instrNo, finInstNo, finRecDate, crtCounty);
		} else if ("AR".equals(crtState)){
			correctCrossRefAR(resultMap, instrNo, finInstNo, finRecDate, book, page, search);
		} else if ("NV".equals(crtState)){
			correctCrossRefNV(resultMap, instrNo, finInstNo, search);
		} else if ("CA".equals(crtState)){
			correctCrossRefCA(resultMap, instrNo, finInstNo, finRecDate, search);
		} else if ("CO".equals(crtState)){
			correctCrossRefCO(resultMap, instrNo, finInstNo, finRecDate, search);
		} else if ("OH".equals(crtState)){
			correctCrossRefOH(resultMap, instrNo, finInstNo, finRecDate, search);
		}
			
		return resultMap;
	}

	private static void correctCrossRefOH(ResultMap resultMap, String instrNo, String finInstNo, String finRecDate, Search search) {
		if(search == null) {
			return;
		}
		
		int countyId = Integer.parseInt(search.getCountyId());
		
		switch (countyId) {
			case CountyConstants.OH_Franklin:
			case CountyConstants.OH_Licking:
				// format references to look like on RO (Task 8683)
				String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
				instrNo = DTRecord.formatInstNoForOHFranklin(instrNo, recDate, "yyyy-MM-dd");
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				
				finInstNo = DTRecord.formatInstNoForOHFranklin(finInstNo, finRecDate, "yyyy-MM-dd");
				resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
				
				String page = (String) resultMap.get(SaleDataSetKey.PAGE.getKeyName());
				String book = (String) resultMap.get(SaleDataSetKey.BOOK.getKeyName());
				
				if (org.apache.commons.lang.StringUtils.isNotEmpty(book) 
						&& org.apache.commons.lang.StringUtils.isNotEmpty(page) 
						&& page.trim().matches("(?is)\\d[A-Z]\\d+")){
					book += page.substring(0, 1);
					page = page.substring(1);
					
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book);
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), page);
				}
				
				String financePage = (String) resultMap.get(SaleDataSetKey.FINANCE_PAGE.getKeyName());
				String financeBook = (String) resultMap.get(SaleDataSetKey.FINANCE_BOOK.getKeyName());
				
				if (org.apache.commons.lang.StringUtils.isNotEmpty(financeBook) 
						&& org.apache.commons.lang.StringUtils.isNotEmpty(financePage) 
						&& financePage.trim().matches("(?is)\\d[A-Z]\\d+")){
					financeBook += financePage.substring(0, 1);
					financePage = financePage.substring(1);
					
					resultMap.put(SaleDataSetKey.FINANCE_BOOK.getKeyName(), financeBook);
					resultMap.put(SaleDataSetKey.FINANCE_PAGE.getKeyName(), financePage);
				}
				break;	
		}
	}

	/**
	 * Applies to California counties
	 * 
	 * @param resultMap
	 * @param instrNo
	 * @param finInstNo
	 * @param finRecDate
	 * @param search
	 */
	private static void correctCrossRefCA(ResultMap resultMap, String instrNo, String finInstNo, String finRecDate, Search search) {
		if (search == null) {
			return;
		}
		
		int countyId = Integer.parseInt(search.getCountyId());
		
		switch (countyId) {
		case CountyConstants.CA_San_Francisco:
		{
			List<SaleDataSetKey[]> params = new ArrayList<SaleDataSetKey[]>();
			
			params.add(new SaleDataSetKey[]{SaleDataSetKey.INSTRUMENT_NUMBER, SaleDataSetKey.RECORDED_DATE});
			params.add(new SaleDataSetKey[]{SaleDataSetKey.FINANCE_INST_NO, SaleDataSetKey.FINANCE_RECORDED_DATE});
			
			for (SaleDataSetKey[] saleDataSetKeys : params){
				String instrumentNumber = (String) resultMap.get(saleDataSetKeys[0].getKeyName());
				if (StringUtils.isNotEmpty(instrumentNumber)){
					instrumentNumber = instrumentNumber.trim();
					instrumentNumber = instrumentNumber.replaceFirst("(?is)\\A[A-Z]", "");
					
					resultMap.put(saleDataSetKeys[0].getKeyName(), instrumentNumber);
				}
			}
		}
			break;
		default:
			break;
		}
	}
	
	private static void correctCrossRefCO(ResultMap resultMap, String instrNo, String finInstNo, String finRecDate,
			Search search) {
		if(search == null) {
			return;
		}
		
		int countyId = Integer.parseInt(search.getCountyId());
		
		switch (countyId) {
		case CountyConstants.CO_Larimer:
		{
			List<SaleDataSetKey[]> params = new ArrayList<SaleDataSetKey[]>();
			
			params.add(new SaleDataSetKey[]{SaleDataSetKey.INSTRUMENT_NUMBER, SaleDataSetKey.RECORDED_DATE});
			params.add(new SaleDataSetKey[]{SaleDataSetKey.FINANCE_INST_NO, SaleDataSetKey.FINANCE_RECORDED_DATE});
			
			
			for (SaleDataSetKey[] saleDataSetKeys : params) {
				String instrumentNumber = (String) resultMap.get(saleDataSetKeys[0].getKeyName());
				if(StringUtils.isNotEmpty(instrumentNumber)) {
					instrumentNumber = instrumentNumber.trim();	//let's be sure
					String recordedDate = (String) resultMap.get(saleDataSetKeys[1].getKeyName());
					if(StringUtils.isNotEmpty(recordedDate)) {
						Date date = Util.dateParser3(recordedDate);
						if(date != null) {
							Calendar cal = Calendar.getInstance();
							cal.setTime(date);
							int year = cal.get(Calendar.YEAR);
							
							if(year >= 2003) {
								instrumentNumber = year + org.apache.commons.lang.StringUtils.leftPad(instrumentNumber, 7, "0");
							} else if(year >= 2000) {
								instrumentNumber = year + org.apache.commons.lang.StringUtils.leftPad(instrumentNumber, 6, "0");
							} else if(year >= 1981) {
								instrumentNumber = (year + org.apache.commons.lang.StringUtils.leftPad(instrumentNumber, 6, "0")).substring(2);
							}
							resultMap.put(saleDataSetKeys[0].getKeyName(), instrumentNumber);
							
						} else {
							if(recordedDate.matches("\\d{4}0{4}") ) {
								int year = Integer.parseInt(recordedDate.substring(0,4));
								if(year >= 2003) {
									instrumentNumber = year + org.apache.commons.lang.StringUtils.leftPad(instrumentNumber, 7, "0");
								} else if(year >= 2000) {
									instrumentNumber = year + org.apache.commons.lang.StringUtils.leftPad(instrumentNumber, 6, "0");
								} else if(year >= 1981) {
									instrumentNumber = (year + org.apache.commons.lang.StringUtils.leftPad(instrumentNumber, 6, "0")).substring(2);
								}
								resultMap.put(saleDataSetKeys[0].getKeyName(), instrumentNumber);
								//force year in case no recorded date
								resultMap.put(saleDataSetKeys[1].getKeyName(), Integer.toString(year));
							}
						}
					}
					
				}
			}
		}
			break;
		case CountyConstants.CO_Jefferson:
		{	
			List<SaleDataSetKey[]> params = new ArrayList<SaleDataSetKey[]>();
			
			params.add(new SaleDataSetKey[]{SaleDataSetKey.INSTRUMENT_NUMBER, SaleDataSetKey.RECORDED_DATE});
			params.add(new SaleDataSetKey[]{SaleDataSetKey.FINANCE_INST_NO, SaleDataSetKey.FINANCE_RECORDED_DATE});
			
			
			for (SaleDataSetKey[] saleDataSetKeys : params) {
				String instrumentNumber = (String) resultMap.get(saleDataSetKeys[0].getKeyName());
				String recordedDate = (String) resultMap.get(saleDataSetKeys[1].getKeyName());
				if(StringUtils.isNotEmpty(instrumentNumber)) {
//					String cleanedInstrument = instrumentNumber.replaceAll("(?i)^[A-Z]+0*", "");
					if(StringUtils.isNotEmpty(recordedDate)
						&& recordedDate.matches("\\d{4}0{4}")) {
						resultMap.put(saleDataSetKeys[1].getKeyName(), recordedDate.substring(0, 4));
					}
//					if(!cleanedInstrument.equals(instrumentNumber)) {
//						resultMap.put(saleDataSetKeys[0].getKeyName(), cleanedInstrument);
//					}
				}
				
			}
		}
			break;
		case CountyConstants.CO_Adams:
		case CountyConstants.CO_El_Paso:
		case CountyConstants.CO_Eagle:
		{	
			List<SaleDataSetKey[]> params = new ArrayList<SaleDataSetKey[]>();
			params.add(new SaleDataSetKey[]{SaleDataSetKey.INSTRUMENT_NUMBER, SaleDataSetKey.RECORDED_DATE});
			params.add(new SaleDataSetKey[]{SaleDataSetKey.FINANCE_INST_NO, SaleDataSetKey.FINANCE_RECORDED_DATE});
			
			for (SaleDataSetKey[] saleDataSetKeys : params) {
				instrNo = (String)resultMap.get(saleDataSetKeys[0].getKeyName());
				if (!StringUtils.isEmpty(instrNo)) {
					Object obj = resultMap.get(saleDataSetKeys[1].getKeyName());
					if (obj!=null) {
						String recordedDate = (String)obj;
						Date date = Util.dateParser3(recordedDate);
						if(date != null) {
							instrNo = GenericSKLD.generateSpecificInstrument(instrNo.replaceFirst("(?i)^[A-Z]", "") + "-" + FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(date), 
									date, countyId, search.getSearchID());
							resultMap.put(saleDataSetKeys[0].getKeyName(), instrNo);
						}
					}
				}
			}
			
		}
			break;
		default:
			break;
		}
		
	}

	/**
	 * Applies to Arkansas counties
	 * 
	 * @param resultMap
	 * @param instrNo
	 * @param finInstNo
	 * @param finRecDate
	 * @param search
	 */
	public static void correctCrossRefAR(ResultMap resultMap, String instrNo, String finInstNo, String finRecDate,
			String book, String page, Search search) {
		
		if(search == null) {
			return;
		}
		
		int countyId = Integer.parseInt(search.getCountyId());
		
		switch (countyId) {
		case CountyConstants.AR_Pulaski:
			
			if(StringUtils.isNotEmpty(instrNo)) {
				instrNo = instrNo.trim();	//let's be sure
				
				if(instrNo.length() > 6) {
					String possibleYear = instrNo.substring(0, instrNo.length() - 6);
					if(possibleYear.matches("\\d+")) {
						int year = Integer.parseInt(possibleYear);
						if(year <= 99) {
							year += 1900;
						}
						String recordedDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
						if(StringUtils.isNotEmpty(recordedDate)) {
							Date date = Util.dateParser3(recordedDate);
							if(date != null) {
								Calendar cal = Calendar.getInstance();
								cal.setTime(date);
								if(cal.get(Calendar.YEAR) != year) {
									//force year in case it does not match recorded date (B7241)
									resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), Integer.toString(year));		
								}
							}
						} else {
							//force year in case no recorded date
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), Integer.toString(year));
						}
						//remove year from instrument
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo.substring(possibleYear.length()).replaceFirst("^0+", ""));
					}
					
				}
			}
			if(StringUtils.isNotEmpty(finInstNo)) {
				finInstNo = finInstNo.trim();	//let's be sure
				
				if(finInstNo.length() > 6) {
					String possibleYear = finInstNo.substring(0, finInstNo.length() - 6);
					if(possibleYear.matches("\\d+")) {
						int year = Integer.parseInt(possibleYear);
						if(year <= 99) {
							year += 1900;
						}
						String recordedDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
						if(StringUtils.isNotEmpty(recordedDate)) {
							Date date = Util.dateParser3(recordedDate);
							if(date != null) {
								Calendar cal = Calendar.getInstance();
								cal.setTime(date);
								if(cal.get(Calendar.YEAR) != year) {
									//force year in case it does not match recorded date (B7241)
									resultMap.put(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName(), Integer.toString(year));		
								}
							}
						} else {
							//force year in case no recorded date
							resultMap.put(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName(), Integer.toString(year));
						}
						//remove year from instrument
						resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo.substring(possibleYear.length()).replaceFirst("^0+", ""));
					}
					
				}
			}
			
			break;
			
		case CountyConstants.AR_Benton:
			if(StringUtils.isNotEmpty(instrNo)) {
				instrNo = instrNo.trim();	//let's be sure
				
				if(instrNo.matches("(?is)\\A[A-Z].*")) {
					instrNo = instrNo.substring(1, instrNo.length());
					String recordedDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
					if(StringUtils.isNotEmpty(recordedDate)) {
						String extractYearAndMakeBook = "";
						if (recordedDate.matches("(?is)\\A\\d{4}.*")){
							extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
						} 

						if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), extractYearAndMakeBook);
							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), instrNo.trim());
						}
						
					//remove the instrument
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
					}
				} else {
					if(instrNo.matches("\\d+")) {
						String recordedDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
						if(StringUtils.isNotEmpty(recordedDate)) {
							String extractYearAndMakeBook = "";
							if (recordedDate.matches("(?is)\\A\\d{4}.*")){
								extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
							} 

							if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), extractYearAndMakeBook);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), instrNo.trim());
							}
							
						//remove the instrument
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
						}
					}
				}
			}
			if(StringUtils.isNotEmpty(finInstNo)) {
				finInstNo = finInstNo.trim();	//let's be sure
				
				if(finInstNo.matches("(?is)\\A[A-Z].*")) {
					finInstNo = finInstNo.substring(1, finInstNo.length());
					String recordedDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
					if(StringUtils.isNotEmpty(recordedDate)) {
						String extractYearAndMakeBook = "";
						if (recordedDate.matches("(?is)\\A\\d{4}.*")){
							extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
						} 

						if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
							resultMap.put(SaleDataSetKey.FINANCE_BOOK.getKeyName(), extractYearAndMakeBook);
							resultMap.put(SaleDataSetKey.FINANCE_PAGE.getKeyName(), finInstNo.trim());
						}
						
					//remove the instrument
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
					}
				}
			}
			break;
			
		case CountyConstants.AR_Washington:
			if(StringUtils.isNotEmpty(instrNo)) {
				instrNo = instrNo.trim();	//let's be sure
				
				if(instrNo.matches("(?is)\\A[A-Z].*")) {
					instrNo = instrNo.substring(1, instrNo.length());
					String recordedDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
					if(StringUtils.isNotEmpty(recordedDate)) {
						String extractYearAndMakeBook = "";
						if (recordedDate.matches("(?is)\\A\\d{4}.*")){
							extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
						} 

						if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), extractYearAndMakeBook);
							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), instrNo.trim());
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), 
									extractYearAndMakeBook + "-" + instrNo.replaceFirst("^0+", "") .trim());
						} else {
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
						}
					}
				} else {
					if(instrNo.matches("\\d+")) {
						String recordedDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
						if(StringUtils.isNotEmpty(recordedDate)) {
							String extractYearAndMakeBook = "";
							if (recordedDate.matches("(?is)\\A\\d{4}.*")){
								extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
							} 

							if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), extractYearAndMakeBook);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), instrNo.trim());
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), 
										extractYearAndMakeBook + "-" + instrNo.replaceFirst("^0+", "") .trim());
							} else {
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
							}
						}
					}
				}
			}
			if(StringUtils.isNotEmpty(finInstNo)) {
				finInstNo = finInstNo.trim();	//let's be sure
				
				if(finInstNo.matches("(?is)\\A[A-Z].*")) {
					finInstNo = finInstNo.substring(1, finInstNo.length());
					String recordedDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
					if(StringUtils.isNotEmpty(recordedDate)) {
						String extractYearAndMakeBook = "";
						if (recordedDate.matches("(?is)\\A\\d{4}.*")){
							extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
						} 

						if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
							resultMap.put(SaleDataSetKey.FINANCE_BOOK.getKeyName(), extractYearAndMakeBook);
							resultMap.put(SaleDataSetKey.FINANCE_PAGE.getKeyName(), finInstNo.trim());
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), 
									extractYearAndMakeBook + "-" + finInstNo.replaceFirst("^0+", "") .trim());
						} else {
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
						}
					}
				}
			}
//			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
//				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), 
//						book + "-" + page.replaceFirst("^0+", "") .trim());
//			}
			break;	

		case CountyConstants.AR_Saline:
			if(StringUtils.isNotEmpty(instrNo)) {
				instrNo = instrNo.trim();	//let's be sure
				String recordedDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
				if(StringUtils.isNotEmpty(recordedDate)) {
					String extractYearAndMakeBook = "";
					if (recordedDate.matches("(?is)\\A\\d{4}.*")){
						extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
					 
						if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), extractYearAndMakeBook);
							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), instrNo);
						}
						//remove the instrument
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
					}
				}
				
			}
			if(StringUtils.isNotEmpty(finInstNo)) {
				finInstNo = finInstNo.trim();	//let's be sure
				String recordedDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
				if(StringUtils.isNotEmpty(recordedDate)) {
					String extractYearAndMakeBook = "";
					if (recordedDate.matches("(?is)\\A\\d{4}.*")){
						extractYearAndMakeBook = recordedDate.substring(0, 4).trim();
					} 

					if(StringUtils.isNotEmpty(extractYearAndMakeBook)){
						resultMap.put(SaleDataSetKey.FINANCE_BOOK.getKeyName(), extractYearAndMakeBook);
						resultMap.put(SaleDataSetKey.FINANCE_PAGE.getKeyName(), finInstNo.trim());
					}
					
					//remove the instrument
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
				}
			}
			break;
			
		default:
			break;
		}
		
		

	}

	/**
	 * Applies to Nevada counties
	 * 
	 * @param resultMap
	 * @param instrNo
	 * @param finInstNo
	 * @param finRecDate
	 * @param search
	 */
	public static void correctCrossRefNV(ResultMap resultMap, String instrNo, String finInstNo, Search search) {
		
		if(search == null) {
			return;
		}
		
		int countyId = Integer.parseInt(search.getCountyId());
		
		switch (countyId) {
		case CountyConstants.NV_Clark:
			
			if(StringUtils.isNotEmpty(instrNo)) {
				instrNo = instrNo.trim();	//let's be sure
				
				if(instrNo.length() > 6) {
					resultMap.remove(SaleDataSetKey.RECORDED_DATE.getKeyName());		//8182
					String recordedDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
					// if recorded date is empty then try to extract it from instr_no or when recorded date it is wrongly typed
					if (StringUtils.isEmpty(recordedDate) || recordedDate.endsWith("00")){
						String recDate = instrNo.substring(0, instrNo.length() - 6);
						if (StringUtils.isNotEmpty(recDate)){
							if (recDate.length() == 5){
								recDate = "0" + recDate;
							}
							String year = recDate.substring(0, 2);
							try {
								int intYear = Integer.parseInt(year);
								
								if (intYear <= 20) {
									recDate = "20" + recDate;
								} else if (intYear > 20) {
									recDate = "19" + recDate;
								}
								recDate = recDate.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");
								resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
							} catch (Exception e) {
							}	
						}
					}
					String book = (String) resultMap.get(SaleDataSetKey.BOOK.getKeyName());
					String page = (String) resultMap.get(SaleDataSetKey.PAGE.getKeyName());
					if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
						if (finInstNo.equals(book + org.apache.commons.lang.StringUtils.leftPad(page, 6, '0'))){
							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), "");
							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), "");
						}
					}
					instrNo = instrNo.substring(instrNo.length() - 6, instrNo.length());
					instrNo = org.apache.commons.lang.StringUtils.stripStart(instrNo, "0");
					
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				}
			}
			if(StringUtils.isNotEmpty(finInstNo)) {
				finInstNo = finInstNo.trim();	//let's be sure
				
				if(finInstNo.length() > 6) {
					String finRecordedDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
					//if recorded date is empty then try to extract it from instr_no or when recorded date it is wrongly typed
					if (StringUtils.isEmpty(finRecordedDate)  || finRecordedDate.endsWith("00")){
						String finRecDate = finInstNo.substring(0, finInstNo.length() - 6);
						if (StringUtils.isNotEmpty(finRecDate)){
							if (finRecDate.length() == 5){
								finRecDate = "0" + finRecDate;
							}
							String year = finRecDate.substring(0, 2);
							try {
								int intYear = Integer.parseInt(year);
								
								if (intYear <= 20) {
									finRecDate = "20" + finRecDate;
								} else if (intYear > 20) {
									finRecDate = "19" + finRecDate;
								}
								finRecDate = finRecDate.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");
								resultMap.put(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName(), finRecDate);
							} catch (Exception e) {
							}	
						}
					}
					String finBook = (String) resultMap.get(SaleDataSetKey.FINANCE_BOOK.getKeyName());
					String finPage = (String) resultMap.get(SaleDataSetKey.FINANCE_PAGE.getKeyName());
					if (StringUtils.isNotEmpty(finBook) && StringUtils.isNotEmpty(finPage)){
						if (finInstNo.equals(finBook + org.apache.commons.lang.StringUtils.leftPad(finPage, 6, '0'))){
							resultMap.put(SaleDataSetKey.FINANCE_BOOK.getKeyName(), "");
							resultMap.put(SaleDataSetKey.FINANCE_PAGE.getKeyName(), "");
						}
					}
					finInstNo = finInstNo.substring(finInstNo.length() - 6, finInstNo.length());
					finInstNo = org.apache.commons.lang.StringUtils.stripStart(finInstNo, "0");
						
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
				}
			}
			
			break;
			
		default:
			break;
		}
		
		

	}
	/**
	 * Applies to Illinois counties
	 * @param resultMap
	 * @param instrNo
	 * @param finInstNo
	 * @param finRecDate
	 * @param crtCounty
	 */
	public static void correctCrossRefIL(ResultMap resultMap, String instrNo,
			String finInstNo, String finRecDate, String crtCounty) {
		if ("Dekalb".equalsIgnoreCase(crtCounty)){
			String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
			if (StringUtils.isNotEmpty(instrNo) && StringUtils.isNotEmpty(recDate)){
				
				String year = recDate.replaceAll("([^-]+)-.*", "$1");
				if (!recDate.contains("-") && recDate.length() > 3){
					year = recDate.substring(0, 4);
				}
					
				if (year.length() == 4) {
					instrNo = year.concat(org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0"));
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				}
					
										
			} else {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
			}
			if (StringUtils.isNotEmpty(finInstNo) && StringUtils.isNotEmpty(finRecDate)){
				
				String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
				if (!finRecDate.contains("-") && finRecDate.length() > 3){
					year = finRecDate.substring(0, 4);
				}
				
				if (year.length() == 4) {
					finInstNo = year.concat(org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0"));
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
				}
										
			} else{
				resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
			}
		} else if ("Du Page".equalsIgnoreCase(crtCounty)){
			String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
			if (StringUtils.isNotEmpty(instrNo)){
				if (StringUtils.isNotEmpty(recDate)){
			
					instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					instrNo = org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
					String year = recDate.replaceAll("([^-]+)-.*", "$1");
					if (!recDate.contains("-") && recDate.length() > 3){
						year = recDate.substring(0, 4);
					}
					
					if (year.length() == 4) {
						instrNo = year.concat(instrNo);
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "R" + instrNo);
					}
				} else {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
				}
										
			}
			if (StringUtils.isNotEmpty(finInstNo)){ 
				if (StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0");
					String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
					if (!finRecDate.contains("-") && finRecDate.length() > 3){
						year = finRecDate.substring(0, 4);
					}
	
					if (year.length() == 4) {
						finInstNo = year.concat(finInstNo);
						resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "R" + finInstNo);
					}
				} else{
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
				}
										
			}
		} else if ("Kane".equalsIgnoreCase(crtCounty)){
			String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
			if (StringUtils.isNotEmpty(instrNo) && StringUtils.isNotEmpty(recDate)){
				
				String recordYear = recDate.replaceAll("([^-]+)-.*", "$1");
				if (!recDate.contains("-") && recDate.length() > 3){
					recordYear = recDate.substring(0, 4);
				}
				try {
					int year = Integer.parseInt(recordYear);
					
					if (year >= 1999) {
						instrNo = year + "K" + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");

					} else if (year > 1993 && year < 1999){
						instrNo = recordYear.substring(2) + "K" + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
					} else if (year < 1994){//e.g. 1223253004
						instrNo = recordYear.substring(2) + "K" + instrNo;
					}
					
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
						
					
				} catch (Exception e) {
				}						
			} else {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
			}
			
			if (StringUtils.isNotEmpty(finInstNo) && StringUtils.isNotEmpty(finRecDate)){
				
				String finRecYear = finRecDate.replaceAll("([^-]+)-.*", "$1");
				if (!finRecDate.contains("-") && finRecDate.length() > 3){
					finRecYear = finRecDate.substring(0, 4);
				}
				try {
					int year = Integer.parseInt(finRecYear);
					
					if (year >= 1999) {
						finInstNo = year + "K" + org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0");

					} else if (year > 1993 && year < 1999){
						finInstNo = finRecYear.substring(2) + "K" + org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0");
					} else if (year < 1994){//e.g. 1223253004
						finInstNo = finRecYear.substring(2) + "K" + finInstNo;
					}
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
						
				} catch (Exception e) {
				}						
			} else{
				resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
			}
		} else if ("Kendall".equalsIgnoreCase(crtCounty)){
			String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
			if (StringUtils.isNotEmpty(instrNo) && StringUtils.isNotEmpty(recDate)){
				
				String year = recDate.replaceAll("([^-]+)-.*", "$1");
				if (!recDate.contains("-") && recDate.length() > 3){
					year = recDate.substring(0, 4);
				}
				try {
					int intYear = Integer.parseInt(year);
					
					if (intYear <= 1999) {
						if (year.length() == 4) {
							//year = year.substring(2, year.length());
							instrNo = year.concat(org.apache.commons.lang.StringUtils.leftPad(instrNo, 5, "0"));
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
						}
					} else if (intYear >= 2000) {
						if (year.length() == 4) {
							instrNo = year.concat(org.apache.commons.lang.StringUtils.leftPad(instrNo, 8, "0"));
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
						}
					}
					
				} catch (Exception e) {
				}						
			} else {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
			}
			if (StringUtils.isNotEmpty(finInstNo) && StringUtils.isNotEmpty(finRecDate)){
				
				String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
				if (!finRecDate.contains("-") && finRecDate.length() > 3){
					year = finRecDate.substring(0, 4);
				}
				try {
					int intYear = Integer.parseInt(year);
					
					if (intYear <= 1999) {
						if (year.length() == 4) {
							//year = year.substring(2, year.length());
							finInstNo = year.concat(org.apache.commons.lang.StringUtils.leftPad(finInstNo, 5, "0"));
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
						}
					} else if (intYear >= 2000) {
						if (year.length() == 4) {
							finInstNo = year.concat(org.apache.commons.lang.StringUtils.leftPad(finInstNo, 8, "0"));
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
						}
					}
				} catch (Exception e) {
				}						
			} else{
				resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
			}
		} else if ("McHenry".equalsIgnoreCase(crtCounty)){
			String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
			if (StringUtils.isNotEmpty(instrNo)){
				if (StringUtils.isNotEmpty(recDate)){
			
					instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					instrNo = org.apache.commons.lang.StringUtils.leftPad(instrNo, 7, "0");
					String year = recDate.replaceAll("([^-]+)-.*", "$1");
					if (!recDate.contains("-") && recDate.length() > 3){
						year = recDate.substring(0, 4);
					}
					
					if (year.length() == 4) {
						instrNo = year.concat("R").concat(instrNo);
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
					}
				} else {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
				}
										
			}
			if (StringUtils.isNotEmpty(finInstNo)){ 
				if (StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 7, "0");
					String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
					if (!finRecDate.contains("-") && finRecDate.length() > 3){
						year = finRecDate.substring(0, 4);
					}
	
					if (year.length() == 4) {
						finInstNo = year.concat("R").concat(finInstNo);
						resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
					}
				} else{
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "");
				}
										
			}
		} else if ("Will".equalsIgnoreCase(crtCounty)){
			String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
			if (StringUtils.isNotEmpty(instrNo) && StringUtils.isNotEmpty(recDate)){
				instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
				instrNo = org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
				String year = recDate.replaceAll("([^-]+)-.*", "$1");
				if (!recDate.contains("-") && recDate.length() > 3){
					year = recDate.substring(0, 4);
				}
				try {
					int intYear = Integer.parseInt(year);
					
					if (intYear <= 1999) {
						if (year.length() == 4) {
							year = year.substring(2, year.length());
							instrNo = year.concat(instrNo);
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "R" + instrNo);
						}
					} else if (intYear >= 2000) {
						if (year.length() == 4) {
							instrNo = year.concat(instrNo);
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "R" + instrNo);
						}
					}
					
				} catch (Exception e) {
				}						
			}
			if (StringUtils.isNotEmpty(finInstNo) && StringUtils.isNotEmpty(finRecDate)){
				finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
				finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0");
				String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
				if (!finRecDate.contains("-") && finRecDate.length() > 3){
					year = finRecDate.substring(0, 4);
				}
				try {
					int intYear = Integer.parseInt(year);
					
					if (intYear <= 1999) {
						if (year.length() == 4) {
							year = year.substring(2, year.length());
							finInstNo = year.concat(finInstNo);
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "R" + finInstNo);
						}
					} else if (intYear >= 2000) {
						if (year.length() == 4) {
							finInstNo = year.concat(finInstNo);
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), "R" + finInstNo);
						}
					}
					
				} catch (Exception e) {
				}						
			}
		} 
	}

	public static void correctCrossRefTX(ResultMap resultMap, String instrNo, String finInstNo, String finRecDate, Search search){
		
		int countyId = Integer.parseInt(search.getCountyId());
		
		switch (countyId){
			case CountyConstants.TX_Bexar:
			{
				String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
				if (StringUtils.isNotEmpty(instrNo) && StringUtils.isNotEmpty(recDate)){
					instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					instrNo = org.apache.commons.lang.StringUtils.leftPad(instrNo, 7, "0");
					String year = recDate.replaceAll("([^-]+)-.*", "$1");
					try {
						int intYear = Integer.parseInt(year);
						
						if (intYear >= 1970 && intYear <= 1993) {
							
						} else if (intYear >= 1994 && intYear <= 1999) {
							if (year.length() == 4) {
								year = year.substring(2, year.length());
								instrNo = year.concat(instrNo);
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
							}
						} else if (intYear >= 2000) {
							if (year.length() == 4) {
								instrNo = year.concat(instrNo);
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
							}
						}
						
					} catch (Exception e) {
					}						
				}
				if (StringUtils.isNotEmpty(finInstNo) && StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 7, "0");
					String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
					try {
						int intYear = Integer.parseInt(year);
						
						if (intYear >= 1970 && intYear <= 1993) {
							
						} else if (intYear >= 1994 && intYear <= 1999) {
							if (year.length() == 4) {
								year = year.substring(2, year.length());
								finInstNo = year.concat(finInstNo);
								resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
							}
						} else if (intYear >= 2000) {
							if (year.length() == 4) {
								finInstNo = year.concat(finInstNo);
								resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
							}
						}
						
					} catch (Exception e) {
					}						
				}
			}
				break;
			case CountyConstants.TX_Travis:
			{	
				String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
				if (StringUtils.isNotEmpty(instrNo) && StringUtils.isNotEmpty(recDate)){
					instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					String year = recDate.replaceAll("([^-]+)-.*", "$1");
					if (year.length() > 4){
						year = year.substring(0, 4);
					}
					try {
						int intYear = Integer.parseInt(year);
						
						if (intYear != -1){
							if (intYear > 1999){
								if (instrNo.length() > 6){
									instrNo = instrNo.substring(instrNo.length() - 6);
								}
								instrNo = (year + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0"));
							} else{
								if (intYear > 1990 && intYear < 2000){
									year = year.substring(2);
								}
								instrNo = (year + instrNo);
							}
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
						}
					} catch (Exception e) {
					}						
				}
				if (StringUtils.isNotEmpty(finInstNo) && StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia

					String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
					if (year.length() > 4){
						year = year.substring(0, 4);
					}
					try {
						int intYear = Integer.parseInt(year);
						
						if (intYear != -1){
							if (intYear > 1999){
								if (finInstNo.length() > 6){
									finInstNo = finInstNo.substring(finInstNo.length() - 6);
								}
								finInstNo = (year + org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0"));
							} else{
								if (intYear > 1990 && intYear < 2000){
									year = year.substring(2);
								}
								finInstNo = (year + finInstNo);
							}
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
						}
					} catch (Exception e) {
					}						
				}
			}
				break;
			case CountyConstants.TX_Williamson:
			{	
				String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
				if (StringUtils.isNotEmpty(instrNo) && StringUtils.isNotEmpty(recDate)){
					instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					String year = recDate.replaceAll("([^-]+)-.*", "$1");
					if (year.length() > 4){
						year = year.substring(0, 4);
					}
					try {
						int intYear = Integer.parseInt(year);
						
						if (intYear != -1){
							if (intYear > 1999){
								if (instrNo.length() > 6){
									instrNo = instrNo.substring(instrNo.length() - 6);
								}
								instrNo = (year + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0"));
							} else{
								if (intYear > 1990 && intYear < 2000){
									year = year.substring(2);
								}
								instrNo = (year + instrNo);
							}
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
						}
					} catch (Exception e) {
					}						
				}
				if (StringUtils.isNotEmpty(finInstNo) && StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia

					String year = finRecDate.replaceAll("([^-]+)-.*", "$1");
					if (year.length() > 4){
						year = year.substring(0, 4);
					}
					try {
						int intYear = Integer.parseInt(year);
						
						if (intYear != -1){
							if (intYear > 1999){
								if (finInstNo.length() > 6){
									finInstNo = finInstNo.substring(finInstNo.length() - 6);
								}
								finInstNo = (year + org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0"));
							} else{
								if (intYear > 1990 && intYear < 2000){
									year = year.substring(2);
								}
								finInstNo = (year + finInstNo);
							}
							resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
						}
					} catch (Exception e) {
					}						
				}
			}
				break;
		}
	}

	public static void correctCrossRefTN(ResultMap resultMap, String instrNo, String book, String page, 
			String finInstNo, String finRecDate, Search search) {
		
		int countyId = Integer.parseInt(search.getCountyId());
		
		String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
		
		switch (countyId) {
		
		case CountyConstants.TN_Davidson:
		case CountyConstants.TN_Knox:
		case CountyConstants.TN_Rutherford:
			
			if ((StringUtils.isNotEmpty(instrNo) && instrNo.length() <= 7) && StringUtils.isNotEmpty(recDate)){
				instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
				instrNo = org.apache.commons.lang.StringUtils.leftPad(instrNo, 7, "0");
				instrNo = recDate.replaceAll("\\-", "").concat(instrNo);
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);						
			}
			if (StringUtils.isNotEmpty(finInstNo) &&finInstNo.startsWith("M")){
				resultMap.put("SaleDataSet.InstrumentNumber", finInstNo);//see GenericFunctions2 line 16851 --missing rawdoc number
				finInstNo = finInstNo.replaceAll("(?is)M0+", "");
			}
			if (countyId == CountyConstants.TN_Rutherford){ //task 7670
				if ((StringUtils.isNotEmpty(finInstNo) && finInstNo.length() <= 7) && StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 7, "0");
//					finInstNo = finRecDate.replaceAll("\\-", "").concat(finInstNo);
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);	
				}
			}
			
			if (countyId == CountyConstants.TN_Davidson){
				if ((StringUtils.isNotEmpty(finInstNo) && finInstNo.length() <= 7) && StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 7, "0");
					finInstNo = finRecDate.replaceAll("\\-", "").concat(finInstNo);
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);	
				}
			}
			if (countyId == CountyConstants.TN_Knox){
				if ((StringUtils.isNotEmpty(finInstNo) && finInstNo.length() <= 7) && StringUtils.isNotEmpty(finRecDate)){
					finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
					finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 7, "0");
					finInstNo = finRecDate.replaceAll("\\-", "").concat(finInstNo);
					resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);	
				}
			}
			break;
		case CountyConstants.TN_Montgomery:
			if(org.apache.commons.lang.StringUtils.isNotBlank(instrNo) 
					&& org.apache.commons.lang.StringUtils.isNotBlank(book) 
					&& org.apache.commons.lang.StringUtils.isNotBlank(page)) {
				if(org.apache.commons.lang.StringUtils.leftPad(instrNo, 12, "0").equals(
						org.apache.commons.lang.StringUtils.leftPad(book, 7, "0") + org.apache.commons.lang.StringUtils.leftPad(page, 5, "0"))) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "");
				}
			}
			if(org.apache.commons.lang.StringUtils.isNotBlank(book) && book.matches("[A-Z]\\d+")) {
				resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book.substring(1));
			}
			break;
			//for SRC
		case CountyConstants.TN_Anderson:
		case CountyConstants.TN_Bedford:
		case CountyConstants.TN_Bledsoe:
		case CountyConstants.TN_Bradley:
		case CountyConstants.TN_Campbell:
		case CountyConstants.TN_Carter:
		case CountyConstants.TN_Claiborne:
		case CountyConstants.TN_Clay:
		case CountyConstants.TN_Cocke:
		case CountyConstants.TN_Coffee:
		case CountyConstants.TN_Cumberland:
		case CountyConstants.TN_Decatur:
		case CountyConstants.TN_Fayette:
		case CountyConstants.TN_Fentress:
		case CountyConstants.TN_Franklin:
		case CountyConstants.TN_Giles:
		case CountyConstants.TN_Grainger:
		case CountyConstants.TN_Greene:
		case CountyConstants.TN_Hamblen:
		case CountyConstants.TN_Hawkins:
		case CountyConstants.TN_Hickman:
		case CountyConstants.TN_Humphreys:
		case CountyConstants.TN_Jackson:
		case CountyConstants.TN_Jefferson:
		case CountyConstants.TN_Johnson:
		case CountyConstants.TN_Lawrence:
		case CountyConstants.TN_Lincoln:
		case CountyConstants.TN_Loudon:
		case CountyConstants.TN_Macon:
		case CountyConstants.TN_Madison:
		case CountyConstants.TN_Marion:
		case CountyConstants.TN_Maury:
		case CountyConstants.TN_Monroe:
		case CountyConstants.TN_Moore:
		case CountyConstants.TN_Perry:
		case CountyConstants.TN_Pickett:
		case CountyConstants.TN_Polk:
		case CountyConstants.TN_Rhea:
		case CountyConstants.TN_Roane:
		case CountyConstants.TN_Sequatchie:
		case CountyConstants.TN_Sevier:
		case CountyConstants.TN_Shelby:
		case CountyConstants.TN_Smith:
		case CountyConstants.TN_Sullivan:
		case CountyConstants.TN_Unicoi:
		case CountyConstants.TN_Union:
		case CountyConstants.TN_Van_Buren:
		case CountyConstants.TN_Washington:
		case CountyConstants.TN_Weakley:
		case CountyConstants.TN_White:
		case CountyConstants.TN_Williamson:		

			if (StringUtils.isNotEmpty(instrNo)){
				instrNo = instrNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
				if (instrNo.matches("\\d+")){
					if (instrNo.length() < 7){
						if (StringUtils.isNotEmpty(recDate)){
							instrNo = recDate.substring(2, 4) + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
						}
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
					} else if (instrNo.length() == 7){
						instrNo = org.apache.commons.lang.StringUtils.leftPad(instrNo, 8, "0");
						
					}
				}
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
			}
			
			if (StringUtils.isNotEmpty(finInstNo)){
				finInstNo = finInstNo.replaceAll("(?is)\\A0+", "");//daca deja lipsesc zerourile se poate sterge linia
				if (finInstNo.matches("\\d+")){
					if (finInstNo.length() < 7){
						if (StringUtils.isNotEmpty(finRecDate)){
							finInstNo = finRecDate.substring(2, 4) + org.apache.commons.lang.StringUtils.leftPad(finInstNo, 6, "0");
						}
						
					} else if (finInstNo.length() == 7){
						finInstNo = org.apache.commons.lang.StringUtils.leftPad(finInstNo, 8, "0");
					}
				}
				resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstNo);
			}
			break;
		}
	}

	public static void correctCrossRefMO(ResultMap resultMap, String instrNo, String book, String page, Search search){

		int countyId = Integer.parseInt(search.getCountyId());
		
		switch (countyId) {
			case CountyConstants.MO_Platte:
			{
				if (!"".equals(instrNo) && instrNo.length()>6 && "".equals(book) && "".equals(page)){
					book = instrNo.substring(0, 6).replaceAll("(?is)\\A0+", "");
					page = instrNo.substring(6, instrNo.length()).replaceAll("(?is)\\A0+", "");
					instrNo = "";
					
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book);
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), page);
				}
				
				String finInstrNo = (String) resultMap.get(SaleDataSetKey.FINANCE_INST_NO.getKeyName());
				String finRecDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
				if (org.apache.commons.lang.StringUtils.isNotEmpty(finInstrNo) && org.apache.commons.lang.StringUtils.isNotBlank(finRecDate)){
					if (finRecDate.length() > 4 && (finRecDate.startsWith("1") || finRecDate.startsWith("2"))){
						finInstrNo = finRecDate.substring(0, 4) + org.apache.commons.lang.StringUtils.leftPad(finInstrNo, 6, "0");
						resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), finInstrNo);
					}
				}
			}
				break;
			case CountyConstants.MO_St_Louis_City:
			{
				String recDate = (String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName());
				if (StringUtils.isNotEmpty(recDate) && StringUtils.isNotEmpty(instrNo)){
					recDate = recDate.replaceAll("[-]+", "");
					if (recDate.length() == 8){
						recDate = recDate.substring(2);
						recDate = org.apache.commons.lang.StringUtils.stripStart(recDate, "0");

						instrNo = instrNo.replaceAll("\\A" + recDate, "");
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), org.apache.commons.lang.StringUtils.stripStart(instrNo, "0"));
					}
				}
				
				String recFinDate = (String) resultMap.get(SaleDataSetKey.FINANCE_RECORDED_DATE.getKeyName());
				String financeInstr = (String) resultMap.get(SaleDataSetKey.FINANCE_INST_NO.getKeyName());
				
				if (StringUtils.isNotEmpty(recFinDate) && StringUtils.isNotEmpty(financeInstr)){
					recFinDate = recFinDate.replaceAll("[-]+", "");
					if (recFinDate.length() == 8){
						recFinDate = recFinDate.substring(2);
						recFinDate = org.apache.commons.lang.StringUtils.stripStart(recFinDate, "0");
						
						financeInstr = financeInstr.replaceAll("\\A" + recFinDate, "");
						resultMap.put(SaleDataSetKey.FINANCE_INST_NO.getKeyName(), org.apache.commons.lang.StringUtils.stripStart(financeInstr, "0"));
					}
				}
			}
				break;
		}
	}
	
	//	B 6590
	public static void removeCityAndZip(ResultMap resultMap, long searchId) throws Exception {
		
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		
		if ("CO".equals(crtState)){
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), "");
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), "");
		}
	}
}
