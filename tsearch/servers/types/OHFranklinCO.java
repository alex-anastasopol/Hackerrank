package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

@SuppressWarnings("deprecation")
public class OHFranklinCO extends TSServer implements TSServerROLikeI {
	
	private static final long serialVersionUID = 1860214563562034490L;
	
	private final static Pattern pathForm = Pattern.compile("(?i)(?s)<form.*?</form>");
	
	private boolean downloadingForSave = false;
	
	private static List<Tuple> recoveryPostProcessingReportsTuples = null;
	
	public static final String DEBTOR_ID = "debtorID";
	public static final String CASE_NUMBER = "caseNumber";
	public static final String DEFENDANT_NAME = "defendantName";
	public static final String DEFENDANT_ALIAS = "defendantAlias";
	
	public static final String CASE_PATTERN = "(?i)^(\\d{0,2})\\s*([A-Z]{2})\\s*(\\d{0,7})$";
	
	public static final String IMAGE_LINK_PATTERN = "(?is)(<tr[^>]*>\\s*<td>\\s*(?:<img[^>]*>)?\\s*</td>\\s*<td>[^<]*</td>\\s*<td>([^<]*)</td>\\s*<td>\\s*)" + 
		"<a[^\"]+href=\"javascript:openImageLink\\('(\\d+)'\\)[^\"]*\">\\s*<img[^>]+>\\s*</a>(\\s*</td>\\s*<td>[^<]*</td>\\s*<td>[^<]*</td>\\s*<td>[^<]*</td>\\s*</tr>)";
	public static final String PRAECIPE_PATTERN = "(?is).*\\bPra?ecipe\\b.*";
	public static final String IMAGE_PART1_PATTERN = "(?is)images\\['";
	public static final String IMAGE_PART2_PATTERN = "'] = encodeURIComponent\\('([^']+)'\\);";
	
	public static final String IMAGE_LINK = "/imageLinkProcessor.pdf?coords=";
	
	public static class Tuple {

		private PartyI party;
		private String caseNumber = "";
		private String line = "";	
		
		public Tuple(PartyI party, String caseNumber, String line) {
			this.party = party;
			this.caseNumber = caseNumber;
			this.line = line;
		}

		public PartyI getParty() {
			return party;
		}

		public void setParty(PartyI party) {
			this.party = party;
		}

		public String getCaseNumber() {
			return caseNumber;
		}

		public void setCaseNumber(String caseNumber) {
			this.caseNumber = caseNumber;
		}

		public String getLine() {
			return line;
		}

		public void setLine(String line) {
			this.line = line;
		}

	}
	
	public OHFranklinCO(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink, long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
			resultType = MULTIPLE_RESULT_TYPE;	
	}
	
	protected void loadDataHash(HashMap<String, String> data, String instrNo) {
		if (data != null) {
			Matcher ma = Pattern.compile(CASE_PATTERN).matcher(instrNo);
			if (ma.find()) {
				data.put("type", ma.group(2));
			}
		}
	}
	
	public static String getParam(String response, String id) {
		String res = "";
		Matcher ma1 = Pattern.compile("(?is)<input[^>]+id=\"" + id + "\"[^>]*>").matcher(response);
		if (ma1.find()) {
			String s = ma1.group(0);
			Matcher ma2 = Pattern.compile("(?is)\\bvalue=\"([^\"]+)\"").matcher(s);
			if (ma2.find()) {
				res = ma2.group(1);
			}
		}
		return res;
	}
	
	public static boolean hasNext_FirstPage(String response, String buttonId) {
		boolean result = false;
		Matcher ma = Pattern.compile("(?is)<button[^>]+id=\"" + buttonId +"\"[^>]*>").matcher(response);
		if (ma.find()) {
			if (!ma.group(0).toLowerCase().contains("disabled")) {
				result = true;
			} 
		}
		return result;
	}
	
	public String addNext(String response, String address, String caseYear, String caseType, String caseSeq, String keyName, String keyValue, String dir, String buttonId, String tableId) {
		
		if (StringUtils.isEmpty(keyValue)) {
			return response;
		}
		if (StringUtils.isEmpty(caseSeq)) {
			return response;
		}
		
		StringBuilder nextPages = new StringBuilder();
		boolean hasNext = hasNext_FirstPage(response, buttonId);
		while (hasNext) {
			String res = "";
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				HTTPRequest req = new HTTPRequest(getBaseLink() + "/" + address, HTTPRequest.POST);
				req.setPostParameter("caseYear", caseYear);
				req.setPostParameter("caseType", caseType);
				req.setPostParameter("caseSeq", caseSeq);
				req.setPostParameter(keyName, keyValue);
				req.setPostParameter(dir, "3");
				try {
					res = site.process(req).getResponseAsString();
				} catch (RuntimeException e){
					logger.warn("Could not bring link", e);
				}
			} finally {
				HttpManager.releaseSite(site);
			}
			if (!StringUtils.isEmpty(res)) {
				JSONObject jsonObject;
				try {
					jsonObject = new JSONObject(res);
					String s = (String)jsonObject.get("data");
					keyValue = (String)jsonObject.get("nextKey");
					nextPages.append(s);
				} catch (JSONException e) {
					keyValue = "";
					e.printStackTrace();
				}
				
			}
			hasNext = !StringUtils.isEmpty(keyValue);
		}
		if (nextPages.length()>0) {
			String nextPagesString = nextPages.toString();
			nextPagesString = nextPagesString.replaceAll("(?is)<img[^>]*>", "");
			Matcher ma = Pattern.compile("(?is)<tbody[^>]+id=\"" + tableId + "\"[^>]*>.*?(</tbody>)").matcher(response);
			if (ma.find()) {
				int index = ma.end(1) - "</tbody>".length();
				if (index!=-1) {
					response = response.substring(0, index) + nextPagesString + response.substring(index);
				}
			}
		}
		
		return response;
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		int istart, iend;
		
		String rsResponce = Response.getResult();
		rsResponce  = Tidy.tidyParse(rsResponce , null);
		String initialResponse = rsResponce;
		// first we verify that the connection is still valid
		if ( rsResponce.matches("(?is).*Internet\\s*Usage\\s*Disclaimer.*")) {		
			try {			
				HTTPSiteInterface s = HTTPManager.getSite( "OHFranklinCO" ,searchId,miServerID);
				s.destroyAllSessions();				
			} catch (Exception e) {
				e.printStackTrace();
			}		
			throw new ServerResponseException( Response.getParsedResponse() );
		}
		
		if (rsResponce.indexOf("NO CASE MATCHED THE SEARCH CRITERIA") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponce.indexOf("Server error") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
				
		switch(viParseID){
		
			case ID_DETAILS:

				String caseYear = "";
            	String caseType = "";
            	String caseSeq = "";
				String keyNumber = HttpUtils.getConparamFromString( Response.getQuerry(), false, "alinkvalue" );
				if(keyNumber == null) {
					keyNumber = HttpUtils.getConparamFromString( Response.getQuerry(), false, "case" );
				}
				
				String nextPlaintiffKey = getParam(rsResponce, "next-plaintiff-key");
				String nextDefendantKey = getParam(rsResponce, "next-defendant-key");
				String nextDocketKey = getParam(rsResponce, "next-docket-key");
				if( keyNumber == null )
                {
					caseYear = HttpUtils.getConparamFromString( Response.getQuerry(), false, "caseYear" ).trim();
	            	caseType = HttpUtils.getConparamFromString( Response.getQuerry(), false, "caseType" ).trim();
	            	caseSeq = HttpUtils.getConparamFromString( Response.getQuerry(), false, "caseSeq" ).trim();
					if( caseSeq != null || caseType != null )
                    {
                        keyNumber = caseSeq + caseType;
                    }
                    else
                    {
                        keyNumber = "none";
                    }
                } else {
                	keyNumber = keyNumber.trim();
                	Matcher ma = Pattern.compile(CASE_PATTERN).matcher(keyNumber);
					if (ma.find()) {
						caseYear = ma.group(1);
						caseType = ma.group(2);
						caseSeq = ma.group(3);
					}
                }
                
                boolean isFakeResponse = false;
                URI lastURI = Response.getLastURI();
                if (lastURI!=null) {
                	String url = "";
					try {
						url = lastURI.getURI();
					} catch (URIException e) {
						e.printStackTrace();
					}
                	if (!StringUtils.isEmpty(url)) {
                    	if (url.contains("fakeDetails.html")) {
                    		isFakeResponse = true; 
                    	}
                    }
                }
                if (!isFakeResponse) {
                	if (Response.getQuerry().contains("fakeDetails.html")) {
                		isFakeResponse = true;
                	}
                }
                
                String rawResponse = rsResponce;
                
				// isolate relevant part
				istart = rsResponce.indexOf("<title>"); if(istart == -1) return; istart += "<title>".length();
				iend = rsResponce.indexOf("</title>"); if(iend == -1) return;
				String title = rsResponce.substring(istart, iend).toUpperCase();
				istart = rsResponce.indexOf(title); if(istart == -1) return;
				istart = rsResponce.lastIndexOf("<table",istart); if(istart == -1) return;
				iend = rsResponce.lastIndexOf("</table>"); if(iend == -1) return; iend += "</table>".length();
				rsResponce = rsResponce.substring(istart, iend);
									
				// put table title in bold
				rsResponce = rsResponce.replace(title, "<b>" + title + "</b>");
				
				// remove buttons
				rsResponce = rsResponce.replaceAll("<input[^>]+>", "");
				
				// remove select docket type
				rsResponce = StringUtils.eliminatePart(rsResponce,"<select","</select>");
				rsResponce = rsResponce.replace("Show All", "");
				rsResponce = rsResponce.replace("Select Docket Category","");
				
				// remove select, images. put divider style 
				rsResponce = rsResponce.replace("class=\"label2\"","style=\"font-weight: bold;\"");
				rsResponce = rsResponce.replace("class=\"label3\"","style=\"font-weight: bold;\"");
				rsResponce = rsResponce.replace("class=\"divider\"", "style=\"background-color: #000000; height: 2px;\"");
				
				StringBuffer sb = new StringBuffer();
				Matcher maLinks = Pattern.compile(IMAGE_LINK_PATTERN).matcher(rsResponce);
				while (maLinks.find()) {
					String description = maLinks.group(2);
					if (description.matches(PRAECIPE_PATTERN)) {
						String coords = "";
						Matcher maCoords = Pattern.compile(IMAGE_PART1_PATTERN + maLinks.group(3) + IMAGE_PART2_PATTERN).matcher(rawResponse);
						if (maCoords.find()) {
							coords = maCoords.group(1);
							try {
								coords = URLEncoder.encode(coords, "UTF-8");
							} catch (UnsupportedEncodingException uee) {}
						}
						maLinks.appendReplacement(sb, maLinks.group(1) + "<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + IMAGE_LINK + coords 
								+ "\">View</a>" + maLinks.group(4));
					} else {
						maLinks.appendReplacement(sb, maLinks.group(1) + maLinks.group(4));
					}
				}
				maLinks.appendTail(sb);
				rsResponce = sb.toString();
				rsResponce = rsResponce.replaceAll("<img[^>]+>", "");
				
				rsResponce = rsResponce.replace("<td", "<td nowrap");
				rsResponce = rsResponce.replaceAll("nowrap\\s+nowrap", "nowrap");
				
				rsResponce = rsResponce.replaceAll("\\s{2,}", " ");
				rsResponce = rsResponce.replaceAll("(?is)<\\s*td[^>]*>\\s*<\\s*a\\s+[^>]+>[^<]*(prev|search|next)[^<]*<\\s*/a\\s*>\\s*<\\s*/td\\s*>", "");
				rsResponce = rsResponce.replaceAll("(?is)<\\s*td[^>]*>\\s*<\\s*script[^>]*>[^<]*<\\s*/script\\s*>\\s*<\\s*/td\\s*>", "");
				rsResponce = rsResponce.replaceAll("&#0;", "");
				
				rsResponce = addNext(rsResponce, "plaintiffs", caseYear, caseType, caseSeq, "plntffkey", nextPlaintiffKey, "pltfdir", "next-plaintiff-button", "plaintiff-body");
				rsResponce = addNext(rsResponce, "defendants", caseYear, caseType, caseSeq, "dffkey", nextDefendantKey, "dfdir", "next-defendant-button", "defendant-body");
				rsResponce = addNext(rsResponce, "docket", caseYear, caseType, caseSeq, "docketdatekey", nextDocketKey, "docketdir", "next-docket-button", "docket-body");
				rsResponce = rsResponce.replaceAll("(?is)<button[^>]*>[^<]*</button>", "");
				
				rsResponce = rsResponce.replaceAll("(?is)(<tbody[^>]+id=\"(?:plaintiff|defendant|docket)-body\"[^>]*>.*?)</tbody>\\s*<tbody>(.*?</tbody>)", "$1$2");
				
				String[] ids = new String[]{"plaintiff-body", "defendant-body", "docket-body"};
				for (String id: ids) {
					Matcher matcher1 = Pattern.compile("(?is)<tbody[^>]+id=\"" + id + "\"[^>]*>.*?</tbody>").matcher(rsResponce);
					if (matcher1.find()) {
						String table = matcher1.group(0);
						table = table.replaceAll("(?is)(<tr[^>]+)class=\"[^\"]*\"([^>]*>)", "$1$2");
						
						Matcher matcher2 = Pattern.compile("(?is)<table[^>]*>.*?</table>").matcher(table);
						sb = new StringBuffer();
						int index = 0;
						List<String> tables = new ArrayList<String>();
						while (matcher2.find()) {
							tables.add(matcher2.group(0));
							matcher2.appendReplacement(sb, "<table" + index + "></table" + index + ">");
							index++;
						}
						matcher2.appendTail(sb);
						table = sb.toString();
						
						Matcher matcher3 = Pattern.compile("(?is)<tr([^>]*)>.*?</tr>").matcher(table);
						sb = new StringBuffer();
						int nr = -1;
						while (matcher3.find()) {
							String s = matcher3.group(0);
							Matcher matcher4 = Pattern.compile("(?is)(<tr)([^>]+style=\"[^\"]*)display\\s*:\\s*none\\s*;([^>]*>.*)").matcher(s);
							if (matcher4.matches()) {
								s = matcher4.group(1);
								if (nr%2==0) {
									s = s + " bgcolor=\"#d9e7e8\"";
								}
								s = s + matcher4.group(2) + matcher4.group(3);
							} else {
								nr++;
								if (nr%2==0) {
									s = s.replaceFirst("(?is)<tr", "<tr bgcolor=\"#d9e7e8\"");
								}
							}
							matcher3.appendReplacement(sb, Matcher.quoteReplacement(s));
						}
						matcher3.appendTail(sb);
						
						table = sb.toString();
						if (tables.size()>0) {
							sb = new StringBuffer();
							Matcher matcher5 = Pattern.compile("(?is)<table(\\d+)></table\\1>").matcher(table);
							index = 0;
							while (matcher5.find()) {
								if (index<tables.size()) {
									matcher5.appendReplacement(sb, Matcher.quoteReplacement(tables.get(index++)));
								}
							}
							matcher5.appendTail(sb);
							table = sb.toString();
						}
						
						rsResponce = rsResponce.substring(0, matcher1.start(0)) + table + rsResponce.substring(matcher1.end(0));
							
					}
				}
				
				String percent = "90%";
				if (isFakeResponse) {
					percent = "50%";
				}
				// put into a table
				rsResponce = "<table border=\"1\" cellspacing=\"0\" width=\"" + percent + "\"><tr><td>" + rsResponce + "</td></tr></table>";
				rsResponce = rsResponce.replaceAll("\0", "");
				
				//add this to the response , when downloadingForSave we don't need to calculate it again
				rsResponce += "<!--"+keyNumber+"-->";
					
				if ( !downloadingForSave ) {
					String qry = Response.getRawQuerry();
					qry = "dummy=" + keyNumber + "&" + qry;
					String originalLink = sAction + "&" + qry;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					loadDataHash(data, keyNumber);
					if (isInstrumentSaved(keyNumber.replaceAll("\\s", ""), null, data, false)) {
						rsResponce += CreateFileAlreadyInTSD();
					} else {
                        mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse/*rsResponce*/);
						rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
					}
					
					Response.getParsedResponse().setPageLink(
						new LinkInPage(
							sSave2TSDLink,
							originalLink,
							TSServer.REQUEST_SAVE_TO_TSD));

					parser.Parse(Response.getParsedResponse(),rsResponce,Parser.NO_PARSE);
				
				} else {
					
					msSaveToTSDFileName = keyNumber + ".html";
					Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
					
					String viewPattern = "(?is)<a[^>]+href=\"[^\"]+\"[^>]*>View</a>";
					Matcher maViewPattern = Pattern.compile(viewPattern).matcher(rsResponce);
					if (maViewPattern.find()) {
						rsResponce = rsResponce.replaceAll(viewPattern, "");
						Response.getParsedResponse().addImageLink(new ImageLinkInPage("/viewImage.asp?caseno=" + caseYear + "-" + caseType + "-" + caseSeq, keyNumber + ".pdf"));
					}
					
					msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
					
					if (isFakeResponse) {
						smartParseDetails(Response, rsResponce);
					} else {
						ParsedResponse pr = Response.getParsedResponse();
						parser.Parse(pr, rsResponce.replaceAll("(?is)<tr[^>]+id=\"(pla|def)detail\\d+\"[^>]*>.*?</tr>", ""), Parser.PAGE_DETAILS,
		                        getLinkPrefix(TSConnectionURL.idGET),
		                        TSServer.REQUEST_SAVE_TO_TSD);
						pr.setResponse(rsResponce);
					}
					
				}
				                
				break;		
			
			case ID_GET_LINK:
				if(rsResponce.matches("(?is).*Enter\\s*record\\s*line\\s*number\\s*and\\s*display\\s*value\\s*for\\s*detailed\\s*case\\s*record.*")){
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				else{
					ParseResponse(sAction, Response, ID_DETAILS);	
				}
				break;
		
			
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
			
			
			case ID_SEARCH_BY_NAME:
				
				PartyI reference = getReference(Response);
				
				// there were no matches
				if(rsResponce.matches("(?is).*Please\\s*enter\\s*your\\s*search\\s*criteria\\s*above.*")){
					rsResponce = "";
				}				
				if(rsResponce.matches("(?is).*NO\\s*CASE[(]S[)]\\s*MATCHED\\s*THE\\s*SEARCH.*")){
					rsResponce = "";
				}				
				if(rsResponce.matches("(?is).*NO\\s*CASE\\s*MATCHED\\s*THE\\s*SEARCH\\s*CRITERIA.*")){
					rsResponce = "";
				}
				if(rsResponce.matches("(?is).*NO\\s*NAMES\\s*MATCHED\\s*THE\\s*SEARCH\\s*CRITERIA.*")){
					rsResponce = "";
				}
				
				if (rsResponce.length()==0) {
					return;
				}
				
				// check if we landed on a single result
				if(rsResponce.matches("(?is).*CASE\\s*DETAIL.*")){
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				processPostProcessingReportsTuples();
				
				StringBuilder additionalRows = new StringBuilder();
				
				for (Tuple tuple: recoveryPostProcessingReportsTuples) {
					if (GenericNameFilter.isMatchGreaterThenScore(reference, tuple.getParty(), NameFilterFactory.NAME_FILTER_THRESHOLD)
							|| GenericNameFilter.isMatchGreaterThenScore(tuple.getParty(), reference, NameFilterFactory.NAME_FILTER_THRESHOLD)) {
						String patt = "(?is)(<tr[^>]*>\\s*<td>\\s*<input[^>]+value=\"\\s*" + tuple.getCaseNumber() + "\\s*\"[^>]*>\\s*</td>(?:\\s*<td>[^<]*</td>){4}\\s*<td>)([^<]*)(</td>)";
						Matcher ma1 = Pattern.compile(patt).matcher(rsResponce);
						if (ma1.find()) {
							String toAdd = "";
							Matcher ma2 = Pattern.compile("(?is)\\bDebtor\\s+ID\\s*:\\s*([^<;]+)[<;]").matcher(tuple.getLine());
							if (ma2.find()) {
								toAdd += "Debtor ID: " + ma2.group(1);
							}
							Matcher ma3 = Pattern.compile("(?is)\\bDEFENDANT\\s+ALIAS\\s*:\\s*([^<]+)<").matcher(tuple.getLine());
							if (ma3.find()) {
								if (toAdd.length()>0) {
									toAdd += "; ";
								}
								toAdd += "Defendant Alias: " + ma3.group(1);
							}
							if (toAdd.length()>0) {
				    			String g1 = ma1.group(1);
								String g2 = ma1.group(2);
								String g3 = ma1.group(3);
								if (StringUtils.isEmpty(g2)) {
									rsResponce = rsResponce.replaceFirst(patt, g1 + toAdd + g3);
								} else {
									rsResponce = rsResponce.replaceFirst(patt, g1 + g2 + "; " + toAdd + g3);
								}
				    		}	
						} else {
							additionalRows.append(tuple.getLine().replaceAll("(?is)(<a[^>]+href=\")([^\"]+\"[^>]*>)", "$1" + CreatePartialLink(TSConnectionURL.idPOST) + "$2"));
						}
					}
				}
				
				if (additionalRows.length()!=0) {
					if (rsResponce.length()==0) {	//add table header
						rsResponce = "<table width=\"100%\" border=\"0\">" +
							"<tr><td style=\"width:8%\"><span>CASE</span></td>" +
							"<td style=\"width:20%\"><span>NAME</span></td>" +
							"<td style=\"width:4%\"><span>M/F</span></td>" +
							"<td style=\"width:4%\"><span>P/D</span></td>" +
							"<td style=\"width:10%\"><span>D.O.B.</span></td>" +
							"<td style=\"width:35%\"><span>DESCRIPTION</span></td>" +
							"<td style=\"width:10%\"><span>FILED</span></td>" +
							"<td style=\"width:5%\"><span>STATUS</span></td></tr>" + 
							additionalRows.toString() + "</table>";
					} else {
						int idx1 = rsResponce.lastIndexOf("Get Email Updates");
						if (idx1!=-1) {
							int idx2 = rsResponce.substring(0,idx1).toLowerCase().lastIndexOf("<tr>");
							rsResponce = rsResponce.substring(0, idx2) + additionalRows + rsResponce.substring(idx2);
						}
					}
				}
				
				HashMap<String,String> formParams = getFormParams(rsResponce);
				
				formParams.remove("alinkvalue");
				formParams.remove("COMMAND");
				formParams.put("recs", "250"); //fix for bug #964
				
				String queryString = "caseSearch";
				
				Set<String> keys = formParams.keySet();
				Iterator<String> iterator = keys.iterator();
				while ( iterator.hasNext() ) {
					
					String key = iterator.next();
					String value = formParams.get(key);
				
					queryString += "&" + key + "=" + value;
				}
				
				istart = rsResponce.indexOf("<table width=\"100%\" border=\"0\">");
				iend = rsResponce.indexOf("</table>", istart);
				
				if (istart > -1 && iend > -1)
					rsResponce  = rsResponce.substring(istart, iend + "</table>".length());
				
				String postLink = CreatePartialLink(TSConnectionURL.idPOST);
				
				rsResponce = rsResponce.replaceAll("(<input.*?value=\"(.*?)\".*?[/>])", 
						"<a href='" + postLink + queryString + "&alinkvalue=$2'>$2</a>");
				
				// eliminate unnecessary table header line
				rsResponce = StringUtils.eliminatePart(rsResponce, "CASE LISTING", "<tr>", "</tr>");

				// isolate and eliminate table header lines
				rsResponce = rsResponce.replaceAll("(?is)<td width=\"172\".*?</td>", "");
				rsResponce = rsResponce.replaceAll("(?is)<td colspan=\"8\"></td>[^<]*<[^<]+<[^<]+[^<]+<[^<]+</td>","");
				rsResponce = rsResponce.replaceAll("(?is)<td[^<]+<[^<]+<[^<]+<[^<]+</td>","");
				rsResponce = rsResponce.replaceAll("(?is)<tr>\\s+</tr>", "");
				
				istart = rsResponce.indexOf("<tr");
				iend = rsResponce.indexOf("</tr>");
				
				String tableHeader = "";
				if (istart > -1 && iend > -1) {
					tableHeader = rsResponce.substring(istart, iend + "</tr>".length());
					tableHeader = tableHeader.replaceAll("(?is)<td style=\"width: 4%\"><span class=\"label2\">SUBSCRIBE</span></td>", "");
					rsResponce = rsResponce.substring(0, istart) + rsResponce.substring(iend + "</tr>".length());
				}
								
				// parse the intermediate results
				parser.Parse(Response.getParsedResponse(), 
						rsResponce,Parser.PAGE_ROWS,
						getLinkPrefix(TSConnectionURL.idPOST), 
						TSServer.REQUEST_SAVE_TO_TSD);
				
				// put back table header, only for display purposes
				Response.getParsedResponse().setHeader(Response.getParsedResponse().getHeader() + tableHeader);
				break;
			
		}
				
	}
	
	private HashMap<String,String> getFormParams(String html) {
		
		Matcher matform = pathForm .matcher(html);
		
		String formString = "";
		if ( matform.find() ) 
		{
			formString = matform.group(0);
		}
		
		return HttpUtils.getFormParams(formString);
	}
	
	protected FilterResponse getIntervalFilter(){
		BetweenDatesFilterResponse filter = new BetweenDatesFilterResponse(searchId);
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
 
		DocsValidator alreadyPresentValidator = new RejectAlreadyPresentFilterResponse(searchId).getValidator();
		
		for(String key: new String[]{SearchAttributes.OWNER_OBJECT, SearchAttributes.BUYER_OBJECT}){
			if(!(SearchAttributes.BUYER_OBJECT.equals(key)&&InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().isProductType(SearchAttributes.SEARCH_PROD_REFINANCE))){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));			
				module.clearSaKeys();
				module.clearIteratorTypes();
				module.setSaObjKey(key);
				module.forceValue(7, Integer.toString(Search.MAX_NO_OF_DOCUMENTS_SAVED_PER_MODULE));
				
				module.setSaKey(8, SearchAttributes.LAST_TRANSFER_DATE_MMDDYYYY);
				module.setSaKey(9, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
	            
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				
				GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
		        nameFilter.setIgnoreMiddleOnEmpty(true);
		        module.addFilter(nameFilter);
		        module.addValidator(alreadyPresentValidator);
		        module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());	        
				ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] {"L;F;"});
				iterator.setInitAgain(true);
				module.addIterator(iterator);						
		        
				modules.add(module);
			}
		}
		serverInfo.setModulesForAutoSearch(modules);
		
    }
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
	   
		ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			 module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			 module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
		 	 module.addIterator(nameIterator);

             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				 module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
				 module.addIterator(nameIterator);
	
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
			
	
			 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	    
    }
	
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart,
            int action) throws ro.cst.tsearch.exceptions.ServerResponseException {

        p.splitResultRows(
                pr,
                htmlString,
                pageId,
                "<tr",
                "</table>", linkStart, action);
    }
	
	/* For most common names the search is set to use First Name + Middle Name or Initial in first name field, which doesn't return correct results (bug #964, 2nd issue)
	 * => in these cases only First Name info must be used in first name field 
	**/
	public ServerResponse SearchBy( TSServerInfoModule module, Object sd) throws ServerResponseException
    {           
        if( module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX )
        {        	
        	String value = module.getFunction(1).getParamValue();
        	value = value.replaceFirst("^(\\w+)\\s.+", "$1");	        	
        	module.getFunction(1).setParamValue(value);
        }        
        return super.SearchBy(module, sd);
    }
	
	public PartyI getReference(ServerResponse Response) {
		
		String lastName = HttpUtils.getConparamFromString(Response.getQuerry(), false, "lname");
		String firstName = HttpUtils.getConparamFromString(Response.getQuerry(), false, "fname");
		String middleInit = HttpUtils.getConparamFromString(Response.getQuerry(), false, "mint");
		
		NameI name = new Name();
		if (!StringUtils.isEmpty(lastName)) {
			name.setLastName(lastName);
			if (NameUtils.isCompany(lastName)) {
				name.setCompany(true);
			}
		}
		if (!StringUtils.isEmpty(firstName)) {
			name.setFirstName(firstName);
		}
		if (!StringUtils.isEmpty(middleInit)) {
			name.setMiddleName(middleInit);
		}
		
		PartyI reference = new Party(PType.GRANTEE);
		reference.add(name);
		
		return reference;
	}
	
	public synchronized void processPostProcessingReportsTuples() {
		
		if (recoveryPostProcessingReportsTuples==null) {
			
			recoveryPostProcessingReportsTuples = new ArrayList<Tuple>();
			
			List<String> recoveryPostProcessingReports = new ArrayList<String>();
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				recoveryPostProcessingReports = ((ro.cst.tsearch.connection.http2.OHFranklinCO)site).getRecoveryPostProcessingReports(); 
			} catch (RuntimeException e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
			
			for (String text: recoveryPostProcessingReports) {
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(text, null);
					NodeList nodeList = htmlParser.parse(null);
					NodeList list = HtmlParser3.getNodeListByType(nodeList, "table", true);
					if (list.size()>0) {
						Node node = list.elementAt(0);
						if (node instanceof TableTag) {
							TableTag table = (TableTag)node;
							boolean start = false;
							for (int i=0;i<table.getRowCount();i++) {
								TableRow row = table.getRow(i);
								if (row.getColumnCount()==4) {
									String row0 = row.getColumns()[0].toPlainTextString().trim();
									String row1 = row.getColumns()[1].toPlainTextString().trim();
									String row2 = row.getColumns()[2].toPlainTextString().replaceAll("(?is)&amp;", "&").trim();
									String row3 = row.getColumns()[3].toPlainTextString().replaceAll("(?is)&amp;", "&").trim();
									if (start) {
										PartyI party = parseNames(row2, row3);
										String caseNumber = row1;
										caseNumber = caseNumber.replaceFirst("(?i)^(\\d{0,2})([A-Z]{2})(\\d{0,7})$", "$1 $2 $3");
										String line = "<tr><td><a href=\"fakeDetails.html?case=" + row1 + "\">" + 
											caseNumber + "</a></td><td>" + row2 + "</td><td></td><td>DF</td><td></td><td>Debtor ID: " + row0;
										if (!StringUtils.isEmpty(row3)) {
											line += "; Defendant Alias: " + row3;
										}
										line += "</td><td></td><td></td></tr>";
										Tuple tuple = new Tuple(party, caseNumber, line);
										recoveryPostProcessingReportsTuples.add(tuple);
									} else {
										if (row0.matches("\\*+") && row1.matches("\\*+") && row2.matches("\\*+") && row3.matches("\\*+")) {
											start = true;
										}
									}
								}
							}
						}
					}
				} catch (ParserException e) {
					e.printStackTrace();
				}
			}
		}
	
	}
	
	public PartyI parseNames(String name, String alias) {
		
		PartyI party = new Party(PType.GRANTEE);
		
		if (!StringUtils.isEmpty(name)) {
			parseName(name, party);
		}
		if (!StringUtils.isEmpty(alias)) {
			parseName(alias, party);
		}
		
		return party;
		
	}
	
	public void parseName(String name, PartyI party) {
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		names = StringFormats.parseNameNashville(name, true);
		type = GenericFunctions.extractAllNamesType(names);
		otherType = GenericFunctions.extractAllNamesOtherType(names);
		suffixes = GenericFunctions.extractNameSuffixes(names);
		
		if (!StringUtils.isEmpty(names[2])) {
			NameI nameI = buildName(names[2], names[0], names[1], suffixes[0], type[0], otherType[0], NameUtils.isCompany(names[2]));
			party.add(nameI);
		}
		
		if (!StringUtils.isEmpty(names[5])) {
			NameI nameI = buildName(names[5], names[3], names[4], suffixes[1], type[1], otherType[1], NameUtils.isCompany(names[5]));
			party.add(nameI);
		}
	}
	
	public NameI buildName(String last, String first, String middle, String suffix, String type, String otherType, boolean isCompany) {
		Name name = new Name();	    					    			
		name.setLastName(last);
		name.setFirstName(first);
		name.setMiddleName(middle);
		name.setSufix(suffix);
		name.setNameType(type);
		name.setNameOtherType(otherType);
		name.setCompany(isCompany);
		return name;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CO");
			
			String caseNumber = HtmlParser3.getNodeValueByID(CASE_NUMBER, nodeList, true);
			String defendantName = HtmlParser3.getNodeValueByID(DEFENDANT_NAME, nodeList, true);
			String defendantAlias = HtmlParser3.getNodeValueByID(DEFENDANT_ALIAS, nodeList, true);
			
			map.put("tmpCaseNumber", caseNumber);
			
			String grantee = defendantName;
			if (!StringUtils.isEmpty(defendantAlias)) {
				grantee += " / " + defendantAlias;
			}
			map.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);
			
			Matcher ma = Pattern.compile(CASE_PATTERN).matcher(caseNumber);
			if (ma.find()) {
				map.put(CourtDocumentIdentificationSetKey.CASE_TYPE.getKeyName(), ma.group(2));
    			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), ma.group(2));
			}
			
			GenericFunctions1.parseCourtOHFranklinCO(map, searchId);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
		msLastLink = vsRequest;
		String sAction = GetRequestSettings(vbEncoded, vsRequest);
	
		String link = ro.cst.tsearch.utils.StringUtils.extractParameter(vsRequest, "Link=(.*)");
		if (link.contains("fakeDetails.html")) {
			
			String caseNumber = ro.cst.tsearch.utils.StringUtils.extractParameter(vsRequest, "case=([^&?]*)");
			String newCaseNumber = "";
			
			//try on the official site first
			String newLink = "";
			Matcher ma1 = Pattern.compile(CASE_PATTERN).matcher(caseNumber);
			if (ma1.find()) {
				newCaseNumber = ma1.group(1) + " " + ma1.group(2) + " " + ma1.group(3);
				newLink = "caseSearch&alinkvalue=" + newCaseNumber;
				ServerResponse officialResponse = GetLink(vsRequest.replaceFirst("(?is)\\b(Link=).*", "$1" + newLink), vbEncoded);
				if (StringUtils.isEmpty(officialResponse.getParsedResponse().getError()) && officialResponse.getParsedResponse().getResultsCount()==1) {
					return officialResponse;
				}
			}
			 
			String defendantName = "";
			String defendantAlias = "";
			String debtorID = "";
			String line = "";
			for (Tuple t: recoveryPostProcessingReportsTuples) {
				if (newCaseNumber.equals(t.getCaseNumber())) {
					line = t.getLine();
					break;
				}
			}
			if (!StringUtils.isEmpty(line))
			{
				List<String> list = new ArrayList<String>();
				Matcher ma2 = Pattern.compile("(?is)<td>(.*?)</td>").matcher(line);
				while (ma2.find()) {
					list.add(ma2.group(1));
				}
				if (list.size()>0) {
					caseNumber = list.get(0).replaceAll("(?is)</?a.*?>", "");
				}
				if (list.size()>1) {
					defendantName = list.get(1);
				}
				if (list.size()>5) {
					Matcher ma3 = Pattern.compile("(?is)\\bDebtor\\s+ID\\s*:(.*?);").matcher(list.get(5) + ";");
					if (ma3.find()) {
						debtorID = ma3.group(1).trim();
					}
					Matcher ma4 = Pattern.compile("(?is)\\bDefendant\\s+Alias\\s*:(.*?);").matcher(list.get(5) + ";");
					if (ma4.find()) {
						defendantAlias = ma4.group(1).trim();
					}
				}
			}
			String fakeResponse = "<html><head><title>Case Detail</title></head>" + 
					"<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%\" id=\"main\"><tbody>" +
					"<tr><td nowrap=\"\" align=\"center\" colspan=\"4\"><span><b>CASE DETAIL</b></span></td></tr>" +
					"<tr><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%\"><tbody>" +
					"<tr><td nowrap=\"\" style=\"width: 1%\"></td><td nowrap=\"\" style=\"width:20%\"><span style=\"font-weight:bold;\">CASE NUMBER</span></td>" +
					"<td nowrap=\"\" style=\"width:1%\"></td><td>DEBTOR ID</td></tr>" +
					"<tr><td nowrap=\"\" colspan=\"5\"></td></tr>" +
					"<tr><td nowrap=\"\"></td><td id=\"caseNumber\" nowrap=\"\">" + caseNumber + "</td><td nowrap=\"\"><td id=\"debtorID\">" + debtorID + "</td></tr>" +
					"</tbody></table>" +
					"<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%\"><tbody>" +
					"<tr class=\"spacer\"></tr>" +
					"<tr><td nowrap=\"\" colspan=\"2\"><span style=\"font-weight:bold;\">DEFENDANT(S)</span></td></tr>" +
					"<tr style=\"background-color:#000000; height:2px;\"><td nowrap=\"\" colspan=\"4\"></td></tr>" +
					"<tr><td nowrap=\"\"></td><td nowrap=\"\"><span style=\"font-weight:bold;\">Name</span></td></tr>" +
					"<tr><td nowrap=\"\"></td><td id=\"defendantName\" nowrap=\"\">" + defendantName + "</td></tr>";
			if (!StringUtils.isEmpty(defendantAlias)) {
				fakeResponse += "<tr><td nowrap=\"\"></td><td id=\"defendantAlias\" nowrap=\"\">" + defendantAlias + "</td></tr>";
			}
			fakeResponse+= "</tbody></table></td></tr></tbody></table></html>";
			        	
			ServerResponse response = new ServerResponse();
			response.setCheckForDocType(false);
			String query = link;
			if (query.indexOf("parentSite=true") >= 0 || isParentSite()) {
				response.setParentSiteSearch(true);
			 }
			response.setQuerry(query);
			ParsedResponse parsedResponse = response.getParsedResponse();
			parsedResponse.setResponse(fakeResponse);
			if((parsedResponse.getPageLink() == null || StringUtils.isNotEmpty(parsedResponse.getPageLink().getLink())) && StringUtils.isNotEmpty(vsRequest)) {
				parsedResponse.setPageLink(new LinkInPage(vsRequest,vsRequest));
			} 
			solveResponse(sAction, ID_DETAILS, "GetLink", response, new RawResponseWrapper(fakeResponse), null);
			return response;
		 }
		        
		 return performRequest(sAction, miGetLinkActionType, "GetLink", ID_GET_LINK, null, vsRequest, null);
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		
		DownloadImageResult dir = null;
		
		if (image.getLinks().size()>0) {
			
			String link = image.getLink(0);

			byte[] imageBytes = null;

			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				imageBytes = ((ro.cst.tsearch.connection.http2.OHFranklinCO)site).getImage(link);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}

			ServerResponse resp = new ServerResponse();

			if (imageBytes != null) {
				afterDownloadImage(true);
			} else {
				return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
			}

			String imageName = image.getPath();
			if (ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
				imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imageName);
				return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
			}

			resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType()));

			if (!ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
				FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), image.getPath());
			}

			dir = resp.getImageResult();
			
		}
		
		return dir;
	}

}
