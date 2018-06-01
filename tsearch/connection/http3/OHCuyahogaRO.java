package ro.cst.tsearch.connection.http3;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class OHCuyahogaRO extends HttpSite3 {
	
	public static final String EVENT_TARGET_PARAM = "__EVENTTARGET";
	public static final String EVENT_ARGUMENT_PARAM = "__EVENTARGUMENT";
	public static final String VIEW_STATE_PARAM = "__VIEWSTATE";
	public static final String EVENT_VALIDATION_PARAM = "__EVENTVALIDATION";
	
	public static final String SEQ_PARAM = "seq";
	public static final String DETAILS_PARAM = "det";
	public static final String REF_INST_PARAM = "refInst";
	public static final String REF_BP_PARAM = "refBP";
	
	public static final String SERVER_ERROR_MESSAGE = "Server Error!";
	
	public static final String GENERAL_SEARCH_ADDRESS = "/Searchs/generalsearchs.aspx";
	public static final String PARCEL_SEARCH_ADDRESS = "/Searchs/parcelsearchs.aspx";
	public static final String GENERAL_INTERM_ADDRESS = "/Searchs/Doclist.aspx";
	public static final String PARCEL_INTERM_ADDRESS = "/Searchs/Parcellist.aspx";
	public static final String DETAILS_ADDRESS = "/Searchs/Docindex.aspx";
	public static final String IMAGE_ADDRESS = "/Searchs/DisplayImg.aspx";
	
	public static final int COMPLETE = 0;			//take into account all information (instrument number, book, page, date and type)
	public static final int ONLY_BP_AND_YEAR2 = 1;	//take into account only book, page and the last 2 digits of the year
	public static final int ONLY_BP = 2;			//take into account only book and page
	public static final int IGNORE_TYPE = 3;		//ignore only type (used when searching for images from DTG)
	
	private static final String INSTR_NO_PARAM = "txtAFN";
	private static final String BOOK_PARAM = "txtBook";
	private static final String PAGE_PARAM = "txtPage";
	private static final String START_DATE_PARAM = "txtRecStart";
	private static final String END_DATE_PARAM = "txtRecEnd";
	private static final String LAST_NAME_PARAM = "txtLName";
	private static final String FIRST_NAME_PARAM = "txtFName";
	private static final String COMPANY_NAME_PARAM = "txtCoName";
	private static final String DOC1_PARAM = "doc1";
	private static final String DOC2_PARAM = "doc2";
	private static final String DOC3_PARAM = "doc3";
	private static final String DOC4_PARAM = "doc4";
	private static final String DOC5_PARAM = "doc5";
	private static final String SORT_BY_PARAM = "lstQuery";
	private static final String VALIDATE_BUTTON_PARAM = "ValidateButton";
	private static final String VALIDATE_BUTTON_IMAGE_PARAM = "ctl00$ContentPlaceHolder1$ValidateButton";
	private static final String REFERENCE_DOCUMENT_ID_PARAM = "ctl00$ContentPlaceHolder1$reference$ctl02$txtDocumentID";
	private static final String AFN_PARAM = "ctl00$ContentPlaceHolder1$GridView1$ctl02$txtAFN";
	
	private static final String DOCUMENT_ID_PATT = "(?is)ctl00\\$ContentPlaceHolder1\\$GridView1\\$ctl\\d+\\$txtDocumentID";
	
	private static List<String> PARAMS;
	static {
		PARAMS = new ArrayList<String>();
		PARAMS.add(EVENT_TARGET_PARAM);
		PARAMS.add(EVENT_ARGUMENT_PARAM);
		PARAMS.add(VIEW_STATE_PARAM);
		PARAMS.add(EVENT_VALIDATION_PARAM);
		PARAMS.add(AFN_PARAM);
	}
	
	public static String getParameterValueFromHtml(String parameter, String resp) {
		String result = "";
		
		Matcher matcher = Pattern.compile("<input.+?id=\"" + parameter + "\".+value=\"(.+?)\"").matcher(resp);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	public void makeFakeSearch(HTTPRequest req) {
		req.setURL(getSiteLink().replaceFirst("/$", "") + GENERAL_SEARCH_ADDRESS);
		req.setMethod(HTTPRequest.POST);
		req.setPostParameter(INSTR_NO_PARAM, "");
		req.setPostParameter(BOOK_PARAM, "");
		req.setPostParameter(PAGE_PARAM, "");
		req.setPostParameter(START_DATE_PARAM, "");
		req.setPostParameter(END_DATE_PARAM, "");
		req.setPostParameter(LAST_NAME_PARAM, "");
		req.setPostParameter(FIRST_NAME_PARAM, "");
		req.setPostParameter(COMPANY_NAME_PARAM, "");
		req.setPostParameter(DOC1_PARAM, "-1");
		req.setPostParameter(DOC2_PARAM, "-1");
		req.setPostParameter(DOC3_PARAM, "-1");
		req.setPostParameter(DOC4_PARAM, "-1");
		req.setPostParameter(DOC5_PARAM, "-1");
		req.setPostParameter(SORT_BY_PARAM, "1");
		req.setPostParameter(VALIDATE_BUTTON_PARAM, "Begin Search");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			String url = req.getURL();
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(url, GENERAL_SEARCH_ADDRESS) || org.apache.commons.lang.StringUtils.containsIgnoreCase(url, PARCEL_SEARCH_ADDRESS)) {
				HTTPRequest req1 = new HTTPRequest(url, HTTPRequest.GET);
				String resp1 = execute(req1);
				String viewState = getParameterValueFromHtml(VIEW_STATE_PARAM, resp1);
				String eventValidation = getParameterValueFromHtml(EVENT_VALIDATION_PARAM, resp1);
				req.setPostParameter(VIEW_STATE_PARAM, viewState);
				req.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
			} else if (org.apache.commons.lang.StringUtils.containsIgnoreCase(url, GENERAL_INTERM_ADDRESS) || org.apache.commons.lang.StringUtils.containsIgnoreCase(url, PARCEL_INTERM_ADDRESS)
					|| org.apache.commons.lang.StringUtils.containsIgnoreCase(url, DETAILS_ADDRESS)) {
				Map<String, String> params = new HashMap<String, String>();
				String seqParameter = req.getPostFirstParameter(SEQ_PARAM);
				if (seqParameter!=null)	{
					params = (Map<String, String>) getTransientSearchAttribute("params:" + seqParameter);
					req.removePostParameters(SEQ_PARAM);
					String detailsParameter = req.getPostFirstParameter(DETAILS_PARAM);
					String refInstParameter = req.getPostFirstParameter(REF_INST_PARAM);
					String refBPParameter = req.getPostFirstParameter(REF_BP_PARAM);
					req.removePostParameters(DETAILS_PARAM);
					req.removePostParameters(REF_INST_PARAM);
					req.removePostParameters(REF_BP_PARAM);
					if ("true".equals(detailsParameter)) {	//the link is document details
						
						String instrno = params.get(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName());
						String book = params.get(SaleDataSetKey.BOOK.getShortKeyName());
						String page = params.get(SaleDataSetKey.PAGE.getShortKeyName());
						String date = params.get(SaleDataSetKey.RECORDED_DATE.getShortKeyName());
						String type = params.get(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName());
						
						StringBuilder viewState = new StringBuilder();
						StringBuilder eventValidation = new StringBuilder();
						StringBuilder eventTarget = new StringBuilder();
						StringBuilder eventArgument = new StringBuilder();
						
						boolean found = getDetailsParams(instrno, book, page, date, type, true, false, viewState, eventValidation, eventTarget, eventArgument);
						
						if (found) {
							req.modifyURL(getSiteLink().replaceFirst("/$", "") + GENERAL_INTERM_ADDRESS);
							req.clearPostParameters();
							req.setPostParameter(VIEW_STATE_PARAM, viewState.toString());
							req.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation.toString());
							req.setPostParameter(EVENT_TARGET_PARAM, eventTarget.toString());
							req.setPostParameter(EVENT_ARGUMENT_PARAM, eventArgument.toString());
						}
						
					} else if ("true".equals(refInstParameter)) {	//the link is to a cross-reference as instrument number
						boolean found = false;
						String instrno = params.get(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName());
						String book = params.get(SaleDataSetKey.BOOK.getShortKeyName());
						String page = params.get(SaleDataSetKey.PAGE.getShortKeyName());
						String date = params.get(SaleDataSetKey.RECORDED_DATE.getShortKeyName());
						String type = params.get(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName());
						String refInstno = params.get(SaleDataSetKey.CROSS_REF_INSTRUMENT.getShortKeyName());
						String detaisPage = getDetailsPage(instrno, book, page, date, type, true, false);
						if (detaisPage!=null) {
							String eventTarget = "";
							String eventArgument = "";
							Matcher ma = Pattern.compile(ro.cst.tsearch.servers.types.OHCuyahogaRO.CROSS_REF_PATT).matcher(detaisPage);
							while (ma.find()&&!found) {
								String et = ma.group(2);
								String ea = ma.group(3);
								String foundInsto = ma.group(4);
								if (foundInsto.equals(refInstno)) {
									eventTarget = et;
									eventArgument = ea;
									found = true;
								}
							}
							if (found) {
								String viewState = getParameterValueFromHtml(VIEW_STATE_PARAM, detaisPage);
								String eventValidation = getParameterValueFromHtml(EVENT_VALIDATION_PARAM, detaisPage);
								req.clearPostParameters();
								req.setPostParameter(VIEW_STATE_PARAM, viewState);
								req.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
								req.setPostParameter(EVENT_TARGET_PARAM, eventTarget);
								req.setPostParameter(EVENT_ARGUMENT_PARAM, eventArgument);
							}
						}
						if (!found) {
							makeFakeSearch(req);
						}
					} else if ("true".equals(refBPParameter)) {	//the link is to a cross-reference as book-page
						boolean found = false;
						String book = params.get(CrossRefSetKey.BOOK.getShortKeyName());
						String page = params.get(CrossRefSetKey.PAGE.getShortKeyName());
						String year = params.get(CrossRefSetKey.YEAR.getShortKeyName());
						int mode = ONLY_BP_AND_YEAR2;
						if ("-1".equals(year)) {
							mode = ONLY_BP;
						}
						
						Date today = new Date();
						String date = (new SimpleDateFormat("M/d/yyyy")).format(today);
						
						String resp = searchByBookPage("", book, page, date, "", true);
						
						if (resp!=null) {
							String detailsRow = getDetailsRowFromIntermediary(resp, "", book, page, year, "", mode);
							if (detailsRow!=null) {
								Matcher ma = Pattern.compile(ro.cst.tsearch.servers.types.OHCuyahogaRO.DETAILS_LINK_PATT).matcher(detailsRow);
								if (ma.find()) {
									req.setURL(getSiteLink().replaceFirst("/$", "") + GENERAL_INTERM_ADDRESS);
									req.setMethod(HTTPRequest.POST);
									req.setPostParameter(EVENT_TARGET_PARAM, ma.group(1));
									req.setPostParameter(EVENT_ARGUMENT_PARAM, ma.group(2));
									String viewState = getParameterValueFromHtml(VIEW_STATE_PARAM, resp);
									String eventValidation = getParameterValueFromHtml(EVENT_VALIDATION_PARAM, resp);
									req.setPostParameter(VIEW_STATE_PARAM, viewState);
									req.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
									found = true;
								}
							}
						}
						if (!found) {
							makeFakeSearch(req);
						}
					} else {
						for (Map.Entry<String, String> entry : params.entrySet()) {
							String key = entry.getKey();
							if (PARAMS.contains(key) || key.startsWith(REFERENCE_DOCUMENT_ID_PARAM) || key.matches(DOCUMENT_ID_PATT)) {
								String value = entry.getValue();
								if (value!=null) {
									if (key.startsWith(REFERENCE_DOCUMENT_ID_PARAM)) {
										req.setPostParameter(REFERENCE_DOCUMENT_ID_PARAM, value);
									} else {
										req.setPostParameter(key, value);
									}
								}
							}
						}
					}
				}
			}
		}	
		
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		String url = req.getURL();
		if (org.apache.commons.lang.StringUtils.containsIgnoreCase(url, GENERAL_SEARCH_ADDRESS) || org.apache.commons.lang.StringUtils.containsIgnoreCase(url, PARCEL_SEARCH_ADDRESS)
				|| org.apache.commons.lang.StringUtils.containsIgnoreCase(url, DETAILS_ADDRESS)) {
			String stringResponse = res.getResponseAsString();
			if (stringResponse.contains("Server Error in '/' Application.")) {
				res.is = IOUtils.toInputStream("<html>" + SERVER_ERROR_MESSAGE + "</html>");
				res.body = "<html>" + SERVER_ERROR_MESSAGE + "</html>";
				res.contentLenght = res.body.length();
				res.returnCode = 200;
				return;
			} else {
				res.is = new ByteArrayInputStream(stringResponse.getBytes());
			}
		}
			
	}
	
	public static String getDetailsRowFromIntermediary(String resp, String instrno, String book, String page, String date, String type, int mode) {
		String result = null;
		Set<String> ids = new HashSet<String>();
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView1"), true);
			
			if (mainTableList.size()==0) {
				return null;
			}
			
			date = date.replaceAll("-", "/");
			date = date.replaceFirst("^0+", "");
			date = date.replaceFirst("/0+", "/");
			
			TableTag tbl = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tbl.getRows();

			int matches = 0;
			for (int i=1;i<rows.length&&matches<2;i++) {
				TableRow row = rows[i];
				if (row.getColumnCount()<9) {
					continue;
				}
				String id = "";
				if (row.getColumnCount()>9) {
					String s = row.getColumns()[9].toHtml();
					Matcher ma = Pattern.compile("(?is)<input[^>]+value=\"([^\"]+)\"[^>]*>").matcher(s);
					if (ma.find()) {
						id = ma.group(1);
					}
				}
				ResultMap m = ro.cst.tsearch.servers.functions.OHCuyahogaRO.parseIntermediaryRow(row, ro.cst.tsearch.servers.functions.OHCuyahogaRO.PARTIAL_PARSING);
				String parsedInstrno = org.apache.commons.lang.StringUtils.defaultIfEmpty((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()), "");
				String parsedBook = org.apache.commons.lang.StringUtils.defaultIfEmpty((String)m.get(SaleDataSetKey.BOOK.getKeyName()), "");
				String parsedPage = org.apache.commons.lang.StringUtils.defaultIfEmpty((String)m.get(SaleDataSetKey.PAGE.getKeyName()), "");
				String parsedDate = org.apache.commons.lang.StringUtils.defaultIfEmpty((String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName()), "");
				String parsedType = org.apache.commons.lang.StringUtils.defaultIfEmpty((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()), "");
				parsedDate = parsedDate.replaceFirst("^0+", "");
				parsedDate = parsedDate.replaceFirst("/0+", "/");
				boolean found = false;
				if (mode==COMPLETE) {
					found = instrno.equals(parsedInstrno) && book.equals(parsedBook) && page.equals(parsedPage)	&& date.equals(parsedDate) && type.equals(parsedType); 
				} else if  (mode==IGNORE_TYPE) {
					found = instrno.equals(parsedInstrno) && book.equals(parsedBook) && page.equals(parsedPage)	&& date.equals(parsedDate);
				} else if (mode==ONLY_BP_AND_YEAR2) {
					if (!StringUtils.isEmpty(parsedDate) && parsedDate.length()>1) {
						parsedDate = parsedDate.substring(parsedDate.length()-2);
					}
					found = book.equals(parsedBook) && page.equals(parsedPage) && date.equals(parsedDate); 
				} else if (mode==ONLY_BP) {
					found = book.equals(parsedBook) && page.equals(parsedPage); 
				} 
				if (found && !ids.contains(id)) {
					matches++;
					result = row.toHtml();
					ids.add(id);
				}
			}
			if (matches!=1) {
				return null;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		return result;
	}
	
	public String searchByBookPage(String instrno, String book, String page, String date, String type, boolean fromOnBeforeRequest) {
		String resp = "";
		
		if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
			HTTPRequest req = new HTTPRequest(getSiteLink().replaceFirst("/$", "") + GENERAL_SEARCH_ADDRESS, HTTPRequest.POST);
			req.setPostParameter(INSTR_NO_PARAM, "");
			req.setPostParameter(BOOK_PARAM, book);
			req.setPostParameter(PAGE_PARAM, page);
			req.setPostParameter(START_DATE_PARAM, date);
			req.setPostParameter(END_DATE_PARAM, date);
			req.setPostParameter(LAST_NAME_PARAM, "");
			req.setPostParameter(FIRST_NAME_PARAM, "");
			req.setPostParameter(COMPANY_NAME_PARAM, "");
			req.setPostParameter(DOC1_PARAM, "-1");
			req.setPostParameter(DOC2_PARAM, "-1");
			req.setPostParameter(DOC3_PARAM, "-1");
			req.setPostParameter(DOC4_PARAM, "-1");
			req.setPostParameter(DOC5_PARAM, "-1");
			req.setPostParameter(SORT_BY_PARAM, "1");
			req.setPostParameter(VALIDATE_BUTTON_PARAM, "Begin Search");
			if (fromOnBeforeRequest) {
				setAttribute("onBeforeRequest", Boolean.FALSE);
			}
			resp = execute(req);
			if (fromOnBeforeRequest) {
				setAttribute("onBeforeRequest", Boolean.TRUE);
			}
		}
		
		return resp;
	}
	
	public String searchByInstNo(String instrno, String book, String page, String date, String type, boolean fromOnBeforeRequest) {
		String resp = "";
		
		if (!StringUtils.isEmpty(instrno)) {
			HTTPRequest req = new HTTPRequest(getSiteLink().replaceFirst("/$", "") + GENERAL_SEARCH_ADDRESS, HTTPRequest.POST);
			req.setPostParameter(INSTR_NO_PARAM, instrno);
			req.setPostParameter(BOOK_PARAM, "");
			req.setPostParameter(PAGE_PARAM, "");
			req.setPostParameter(START_DATE_PARAM, date);
			req.setPostParameter(END_DATE_PARAM, date);
			req.setPostParameter(LAST_NAME_PARAM, "");
			req.setPostParameter(FIRST_NAME_PARAM, "");
			req.setPostParameter(COMPANY_NAME_PARAM, "");
			req.setPostParameter(DOC1_PARAM, "-1");
			req.setPostParameter(DOC2_PARAM, "-1");
			req.setPostParameter(DOC3_PARAM, "-1");
			req.setPostParameter(DOC4_PARAM, "-1");
			req.setPostParameter(DOC5_PARAM, "-1");
			req.setPostParameter(SORT_BY_PARAM, "1");
			req.setPostParameter(VALIDATE_BUTTON_PARAM, "Begin Search");
			if (fromOnBeforeRequest) {
				setAttribute("onBeforeRequest", Boolean.FALSE);
			}
			resp = execute(req);
			if (fromOnBeforeRequest) {
				setAttribute("onBeforeRequest", Boolean.TRUE);
			}
		}
		
		return resp;
		
	}
	
	public boolean getDetailsParams(String instrno, String book, String page, String date, String type, boolean fromOnBeforeRequest, boolean ignoreType,
			StringBuilder viewState, StringBuilder eventValidation, StringBuilder eventTarget, StringBuilder eventArgument) {
		
		int mode = COMPLETE;
		if (ignoreType) {
			mode = IGNORE_TYPE;
		}
		
		String detailsRow = null;
		String resp = "";
		
		//try with book and page
		resp = searchByBookPage(instrno, book, page, date, type, fromOnBeforeRequest);
		
		if (!StringUtils.isEmpty(resp)) {
			detailsRow = getDetailsRowFromIntermediary(resp, instrno, book, page, date, type, mode);
		}
		
		if (detailsRow==null) {
			//try with instrument number
			resp = searchByInstNo(instrno, book, page, date, type, fromOnBeforeRequest);
			if (!StringUtils.isEmpty(resp)) {
				detailsRow = getDetailsRowFromIntermediary(resp, instrno, book, page, date, type, mode);
			}
		}
		
		if (detailsRow!=null) {
			viewState.append(getParameterValueFromHtml(VIEW_STATE_PARAM, resp));
			eventValidation.append(getParameterValueFromHtml(EVENT_VALIDATION_PARAM, resp));
			Matcher ma1 = Pattern.compile(ro.cst.tsearch.servers.types.OHCuyahogaRO.DETAILS_LINK_PATT).matcher(detailsRow);
			if (ma1.find()) {
				eventTarget.append(ma1.group(1));
				eventArgument.append(ma1.group(2));
				return true;
			}
		}
		
		return false;
	}
	
	public String getDetailsPage(String instrno, String book, String page, String date, String type, boolean fromOnBeforeRequest, boolean ignoreType) {
		
		StringBuilder viewState = new StringBuilder();
		StringBuilder eventValidation = new StringBuilder();
		StringBuilder eventTarget = new StringBuilder();
		StringBuilder eventArgument = new StringBuilder();
		
		boolean found = getDetailsParams(instrno, book, page, date, type, fromOnBeforeRequest, ignoreType, viewState, eventValidation, eventTarget, eventArgument);
		
		if (found) {
			HTTPRequest req3 = new HTTPRequest(getSiteLink().replaceFirst("/$", "") + GENERAL_INTERM_ADDRESS, HTTPRequest.POST);
			req3.setPostParameter(VIEW_STATE_PARAM, viewState.toString());
			req3.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation.toString());
			req3.setPostParameter(EVENT_TARGET_PARAM, eventTarget.toString());
			req3.setPostParameter(EVENT_ARGUMENT_PARAM, eventArgument.toString());
			String resp3 = execute(req3);
			if (resp3!=null) {
				return resp3;
			}
		}
		
		return null;
	}
	
	public byte[] getImage(String link, String fileName) {
		
		String instrno = StringUtils.extractParameter(link, "instrno=([^&?]*)");
		String book = StringUtils.extractParameter(link, "book=([^&?]*)");
		String page = StringUtils.extractParameter(link, "page=([^&?]*)");
		String date = StringUtils.extractParameter(link, "date=([^&?]*)");
		String type = StringUtils.extractParameter(link, "type=([^&?]*)");
		date = date.replaceAll("-", "/");
		
		boolean ignoreType = false;
		String ignoreTypeString = StringUtils.extractParameter(link, "ignoreType=([^&?]*)");
		if ("true".equals(ignoreTypeString)) {
			ignoreType = true;
		}
		
		String detailsPage = getDetailsPage(instrno, book, page, date, type, false, ignoreType);
		
		if (detailsPage!=null) {
			
			String viewState = "";
			String eventValidation = "";
			
			Matcher ma = Pattern.compile(ro.cst.tsearch.servers.types.OHCuyahogaRO.IMAGE_LINK_PATT).matcher(detailsPage);
			if (!ma.find()) {
				return null;
			}
			
			//get page which contains the parameters needed to get the image
			HTTPRequest req1 = new HTTPRequest(getSiteLink().replaceFirst("/$", "") + IMAGE_ADDRESS, HTTPRequest.GET);
			String resp4 = execute(req1);
			viewState = getParameterValueFromHtml(VIEW_STATE_PARAM, resp4);
			eventValidation = getParameterValueFromHtml(EVENT_VALIDATION_PARAM, resp4);
			
			//get image
			HTTPRequest req2 = new HTTPRequest(getSiteLink().replaceFirst("/$", "") + IMAGE_ADDRESS, HTTPRequest.POST);
			req2.setPostParameter(VIEW_STATE_PARAM, viewState);
			req2.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
			req2.setPostParameter(EVENT_TARGET_PARAM, "");
			req2.setPostParameter(EVENT_ARGUMENT_PARAM, "");
			req2.setPostParameter(VALIDATE_BUTTON_IMAGE_PARAM, "View Image");
			
			HTTPResponse resp5 = process(req2);
			return resp5.getResponseAsByte();
			
		}
		
		return null;
	}

}
