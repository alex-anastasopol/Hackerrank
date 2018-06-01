package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class TXRockwallTR extends TSServer {

	private static final long serialVersionUID = -483853735425756331L;
	
	private static HashMap<String, Integer> taxYears = new HashMap<String, Integer>();
	
	private static String ALL_SUBDIVISIONS = "";
	private static String ALL_ABSTRACTS = "";


	public TXRockwallTR(long searchId) {
		super(searchId);
	}

	public TXRockwallTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath	+ "] does not exist. Module Information not loaded!");
		}
		try {
			ALL_SUBDIVISIONS = FileUtils.readFileToString(new File(folderPath	+ File.separator + "TXRockwallTRSubdivisionList.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			ALL_ABSTRACTS = FileUtils.readFileToString(new File(folderPath	+ File.separator + "TXRockwallTRAbstractList.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getSubdivisionList() {
		return ALL_SUBDIVISIONS;
	}
	
	public static String getAbstractList() {
		return ALL_ABSTRACTS;
	}
	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX);

		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(6), getAbstractList());
			setupSelectBox(tsServerInfoModule.getFunction(7), getSubdivisionList());
		}

		return msiServerInfoDefault;
	}
	
	
	private void getTaxYears(String county) {
		if (taxYears.containsKey("lastTaxYear" + county)  && taxYears.containsKey("firstTaxYear" + county))
			return;
		
		// Get official site html response.
		String response = getLinkContents(getDataSite().getLink() + "Search/Advanced");
		if(response != null) {
			HtmlParser3 parser = new HtmlParser3(response);
			NodeList selectList = parser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("select"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "Year"));
			if(selectList == null || selectList.size() == 0) {
				// Unable to find the tax year select input.
				logger.error("Unable to parse tax years!");
				return;
			}
			
			// Get the first and last tax years.
			SelectTag selectTag = (SelectTag) selectList.elementAt(0);
			OptionTag[] options = selectTag.getOptionTags();
			try {
				taxYears.put("lastTaxYear"+county, Integer.parseInt(options[0].getChildrenHTML().trim()));
				taxYears.put("firstTaxYear"+county, Integer.parseInt(options[options.length - 1].getChildrenHTML().trim()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getYearSelect(String id, String name){
		// getTaxYears();
		String county = dataSite.getCountyName();
		getTaxYears(county);
		int lastTaxYear = taxYears.get("lastTaxYear" + county);
		int firstTaxYear = taxYears.get("firstTaxYear" + county);
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.
			// This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = 2013;
			firstTaxYear = 2008;
		}
		
		// Generate input.
		StringBuilder select  = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n");
		for (int i = lastTaxYear; i >= firstTaxYear; i--){
			select.append("<option ");
			select.append(i == lastTaxYear ? " selected " : "");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("</select>");
		
		return select.toString();
	}
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
		
		try {	
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();
			
			Node headerTitle = list.extractAllNodesThatMatch(new HasAttributeFilter("id","taxYearHeader"), true).elementAt(0);
			if (headerTitle !=null) {
				String taxYear = headerTitle.getChildren().toHtml().trim();
				taxYear =  taxYear.replaceFirst("(?is).*\\s*Property Details for Tax Year\\s*(\\d+{4}).*", "$1");
				if (StringUtils.isNotEmpty(taxYear)) {
					map.put(TaxHistorySetKey.YEAR.getKeyName(),taxYear);
				}
			}

			TableTag table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","accountInfoTable"), true).elementAt(0);
			if (table != null && table.getRowCount() >= 5) {
				TableRow row =  table.getRow(0); // PROPERTY NO
				if (row.getColumnCount() >= 1) {
					TableColumn col = row.getColumns()[0];
					String apn = col.getChildrenHTML().trim();							
					if (StringUtils.isNotEmpty(apn)) {
						map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn);	
					}
				}
				
				row =  table.getRow(1); // Legal Description
				if (row.getColumnCount() >= 1) {
					TableColumn col = row.getColumns()[0];
					String legalDesc = col.getChildrenHTML().trim();	
					if (StringUtils.isNotEmpty(legalDesc)) {
						map.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDesc);
					}
				}
				
				row =  table.getRow(2); // Geo ID
				if (row.getColumnCount() >= 1) {
					TableColumn col = row.getColumns()[0];
					String GeoID = col.getChildrenHTML().trim();	
					if (StringUtils.isNotEmpty(GeoID)) {
						map.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), GeoID);
					}
				}
				
				row =  table.getRow(4); // Type of property
				if (row.getColumnCount() >= 1) {
					TableColumn col = row.getColumns()[0];
					String propType = col.getChildrenHTML().trim();	
					if (StringUtils.isNotEmpty(propType)) {
						if ("real".equals(propType.toLowerCase()))
							map.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real estate");
						else 
							map.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Other type");
					}
				}
			}
			
			table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","addressInfoTable"), true).elementAt(0);
			if (table != null && table.getRowCount() > 0) {
				TableRow row =  table.getRow(0); // Address of property
				if (row.getColumnCount() >= 1) {
					TableColumn col = row.getColumns()[0];
					String address = col.getChildrenHTML().trim();
					address = address.replaceAll(",", "");
					if (StringUtils.isNotEmpty(address)) {
						map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);	
					}
				}
			}
			
			table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","ownerInfoTable"), true).elementAt(0);
			String names = "";
			if (table != null && table.getRowCount() > 2) {
				TableRow row =  table.getRow(1); // Owner names
				if (row.getColumnCount() >= 1) {
					TableColumn col = row.getColumns()[0];
					names = col.getChildrenHTML().trim();
				}
				
				row =  table.getRow(2); //Mailling address might contain also names
				if (row.getColumnCount() >= 1) {
					TableColumn col = row.getColumns()[0];
					String txt = col.getChildrenHTML().trim();
					txt = txt.replaceFirst("(?is)(?:PO\\s*BOX|PB|PBX)\\s*\\d+.*", "");
					txt = txt.replaceFirst("(?is)\\s*\\d+[^,]+,\\s*\\b[A-Z]{2}\\b\\s*(\\d+\\s*(?:-\\d+)?)", "").trim();
					if (StringUtils.isNotEmpty(txt)) {
						//see PropID 61331, 58581, 60506, 72617, 16569
						String regExp = "(?is)([A-Z]+\\s+(?:[A-Z]+)?\\s+)(?:&|\\bAND\\b)\\s+([A-Z]+\\s+(?:[A-Z]+\\s+)?)([A-Z]+)\\s+TRUSTEES";
						Matcher m = Pattern.compile(regExp).matcher(txt);
						if (m.find()) {
							//PropID 72617 (DONNIE J & DOROTHY JOHNSON WILLIAMS TRUSTEES); 16569 (JOHNNY L AND JANICE W JOHNSON TRUSTEES)
							String lastName = m.group(3).trim();
							txt = " AND " + lastName + " " + m.group(1) + " TRUSTEE" 
								+ " AND " + lastName + " " + m.group(2) + " TRUSTEE";
						} else {
							//PropID 60506 (AUDREY DEANNA DILL); 58581 (SONJA M COBB)
							regExp = "(?is)([A-Z]+\\s+[A-Z]+)\\s+([A-Z]+)";
							m = Pattern.compile(regExp).matcher(txt);
							if (m.find()) {
//								String lastName = m.group(2).trim();
//								txt = lastName + " " + m.group(1);
								txt = "& " + txt;
							} else {
								//PropID 52247 (RUTH A)
								regExp = "(?is)([A-Z]+(?:\\s+[A-Z])?)";
								m = Pattern.compile(regExp).matcher(txt);
								if (m.find()) {
									txt = "& " + txt;
								} 
							}
						}
						txt = txt.replaceAll("(?is)<\\s*br\\s*/?\\s*>", "");
						names += txt;
						names = names.replaceFirst("&\\s*&", " &");
					}
				}
				if (StringUtils.isNotEmpty(names)) {
					map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), names.replaceAll("&amp;", "&"));	
				}
			}
			
			parseOwnerInfo(map, searchId);
			parseLegalDesc(map, searchId);
			parseAddressInfo(map, searchId);
			
			//parse Assessment Info
			table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","assessmentInfoTable"), true).elementAt(0);
			if (table != null) {
				if (table.getRowCount() > 1) {
					TableRow row = table.getRow(1);
					if (row.getColumnCount() == 7) {
						TableColumn col = row.getColumns()[1]; //Improvments
						String info = col.getChildrenHTML().trim().replaceAll("[\\$,]", "");
						if (StringUtils.isNotEmpty(info)) 
							map.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), info);
						
						col = row.getColumns()[2]; //LandMarket
						info = col.getChildrenHTML().trim().replaceAll("[\\$,]", "");
						if (StringUtils.isNotEmpty(info)) 
							map.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), info);
						
						col = row.getColumns()[4]; //Appraised
						info = col.getChildrenHTML().trim().replaceAll("[\\$,]", "");
						if (StringUtils.isNotEmpty(info)) 
							map.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), info);
						
						col = row.getColumns()[6]; //Assesed
						info = col.getChildrenHTML().trim().replaceAll("[\\$,]", "");
						if (StringUtils.isNotEmpty(info)) 
							map.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), info);
					}
					
				}
			}
			
			// parse Cross Ref Info
			table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","crossRefTable"), true).elementAt(0);
			if (table != null) {
				int noOfRows = table.getRowCount();
				if (noOfRows > 1) {
					List<List> body = new ArrayList<List>();
					List<String> line = null;
					
					for (int i=1; i < noOfRows; i++) {
						TableRow row = table.getRow(i);
						
						if (row.getColumnCount() == 7) {
							line = new ArrayList<String>();
							String instr = "";
							String bk = "";
							String pg = "";
							String date = "";
									
							TableColumn col = row.getColumns()[0]; //date
							date = col.getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(date) && date.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) 
								line.add(date);
							else
								line.add("");
							
							col = row.getColumns()[5]; //volume
							instr = col.getChildrenHTML().trim();
							if (instr.contains("-") && instr.matches("\\d{2,4}\\s*-\\s*\\d+")) {
								//eg: PID 16827 -->  Volume = 98-0106906, Page = 4259-2756
								instr = instr.replaceAll("\\s", "");
								instr = instr.substring(instr.indexOf("-") + 1);
								instr = instr.replaceAll("\\b0+(\\d+)\\b", "$1");
								line.add(instr);
							} else {
								line.add("");
								bk = instr;
								bk = bk.replaceAll("\\b0+(\\d+)\\b", "$1");
								if (StringUtils.isNotEmpty(bk))
									line.add(bk);
								else
									line.add("");
							}
							
							
							col = row.getColumns()[6]; //page
							pg = col.getChildrenHTML().trim();
							if (pg.contains("-") && pg.matches("\\d+\\s*-\\s*\\d+")) {
								//eg: PID 16827 -->  Volume = 98-0106906, Page = 4259-2756
								pg = pg.replaceAll("\\s", "");
								bk = pg.substring(0,pg.indexOf("-"));
								bk = bk.replaceAll("\\b0+(\\d+)\\b", "$1");
								if (StringUtils.isNotEmpty(bk)) 
									line.add(bk);
								else
									line.add("");
								
								pg = pg.substring(pg.indexOf("-") + 1);
							}
							pg = pg.replaceAll("\\b0+(\\d+)\\b", "$1");
							if (StringUtils.isNotEmpty(pg)) 
								line.add(pg);
							else
								line.add("");
						}
						
						body.add(line);
					}
					
					if (body != null){
						ResultTable rt = new ResultTable();
						String[] header = { "InstrumentDate", "InstrumentNumber", "Book", "Page" };
						rt = GenericFunctions2.createResultTable(body, header);
						map.put("SaleDataSet", rt);
					}
				}
			}
			
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","taxesDue"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			if (table != null) {
				parseTaxes((String)map.get(TaxHistorySetKey.YEAR.getKeyName()),table, map, searchId);
			}
			
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;
	}
	
	
	public StringBuilder getPrevAndNextLinks (ServerResponse response, HtmlParser3 htmlParser) {
		String url = response.getLastURI().toString().replaceFirst("(?is)https?://[^/]+(/[^\\$]+)", "$1");
		String pagNo = "&page=";
		
		if (url.contains("&page=")) {
			url = url.replaceAll("(?is)&page[=:]\\d+", "");
		}
		
		StringBuilder footer = new StringBuilder("<div style=\"width:998px;\">");
		String links = "";
		String linkN = "";
		int indexN = 0;
		boolean addN = false;
		String linkP = "";
		int indexP = 1;
		boolean addP = false;
		int maxPag = 1;
		
		NodeList nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "result-grid-footer")); 
		
		if (nodeList != null && nodeList.size() > 0) {
			String noOfPages = nodeList.elementAt(0).getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("div"))
				.elementAt(0).getFirstChild().toHtml().trim();
			
			if (StringUtils.isNotEmpty(noOfPages)) {
				Matcher m = Pattern.compile("(?is)\\s*Page (\\d+) of (\\d+).*").matcher(noOfPages);
				if (m.find()) {
					footer.append("<div id=\"pagingInfo\">" + noOfPages + "</div>  <br/> &nbsp; &nbsp; &nbsp;");
					int currentPos = Integer.parseInt(m.group(1).trim());
					indexN = currentPos + 1;
					maxPag = Integer.parseInt(m.group(2).trim());
					
					if (indexN <= maxPag) {
						addN = true;
						linkN = url + pagNo + indexN;
						linkN = CreatePartialLink(TSConnectionURL.idGET) + linkN ;
					}
					
					if (currentPos > indexP) {
						addP = true;
						indexP = currentPos -1;
						linkP = url + pagNo + indexP;
						linkP = CreatePartialLink(TSConnectionURL.idGET) + linkP ;
					}
				}
			}
			
			if (StringUtils.isNotEmpty(linkP)) {
				if (addP) {
					links = links + "<a href=\"" + linkP + "\"> Prev </a> &nbsp; &nbsp;";
				}
			}	
			if (StringUtils.isNotEmpty(linkN)) {
				if (addN) {
					links = links + "&nbsp; &nbsp; <a href=\"" + linkN + "\"> Next </a> &nbsp; &nbsp;";
					response.getParsedResponse().setNextLink("<a href=\"" + linkN + "\">Next</a>");
				}
			}	
			
			footer.append(links);
			footer.append("</div>");
		}

		footer.append("</div>");
		
		return footer;
	}
	
	public static void parseLegalDesc(ResultMap resultMap,long searchId) {
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		if(StringUtils.isNotEmpty(legalDescription))  {
			legalDescription = legalDescription.replaceAll("(?is)BUSINESS PERSONAL PROPERTY.*", "");
			legalDescription = legalDescription.replaceFirst("(?is)\\bADDN\\b\\s+", " ");
			legalDescription = legalDescription.replaceFirst("(?is)\\s*(?:\\()?(?:A|F)\\.?K\\.?A\\.?\\s+[^,]+\\s*", "");
			legalDescription = legalDescription.replaceFirst("(?is)\\bMH\\b\\s+(?:SERIAL\\s*#[^,]+,)?(?:\\s+TITLE\\s*#[^,]+,)?(?:\\s*LABEL\\s*#[\\s\\d[A-Z]/]+)?", "");
			legalDescription = legalDescription.replaceAll("(?is)(?:\\d+)?\\s*(?:\\.\\d+)?\\bAC(?:\\s*\\s*,\\s*[\\d\\.%]+[\\s[A-Z]]+|RES\\s*[\\s\\d\\.,\\(\\)#&/[A-Z]]+)", "");
			legalDescription = legalDescription.replaceFirst("(?is)&(?:amp;)?\\s*P(?:AR)?T OF\\s+", "");
			
			if (legalDescription.contains(" MOBILE HOME") || legalDescription.contains(" TAG #") || legalDescription.contains(" TAG#")) {
				legalDescription = legalDescription.replaceFirst("(?is)(.*),?\\s*SPACE [A-Z\\d-]+\\s*,?\\s*TAG\\s*#\\s*[A-Z\\d]+\\s*,?\\s*(?:IMP ONLY)?[A-Z\\s]+", "$1").trim();
			}
			
			if (legalDescription.contains(" PH") || legalDescription.contains(",PH") || legalDescription.contains(" PHASE")) {
				String phase = legalDescription;
				phase = phase.replaceFirst("(?is).*\\bPH(?:ASE)?\\b\\s*([A-Z\\d-]+).*", "$1").trim();
				if (StringUtils.isNotEmpty(phase)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), Roman.normalizeRomanNumbers(phase));	
					legalDescription = legalDescription.replaceFirst("(?is)(.*)\\bPH(?:ASE)?\\b\\s+(?:[A-Z\\d-]+\\s*,?)(.*)", "$1" + "$2");
					legalDescription = legalDescription.replaceAll(",", "");
				}
			}
			
			if (legalDescription.contains(" TRACT") || legalDescription.contains(",TRACT") || legalDescription.contains(", TRACT")) {
				String tract = legalDescription;
				tract = tract.replaceFirst("(?is).*\\bTRACT\\b\\s+([\\d-]+).*", "$1").trim();
				if (StringUtils.isNotEmpty(tract)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), Roman.normalizeRomanNumbers(tract));	
					legalDescription = legalDescription.replaceFirst("(?is)(.*)\\bTRACT\\b\\s+(?:[\\d-]+\\s*,?)(.*)", "$1" + "$2");
					legalDescription = legalDescription.replaceAll(",", "");
				}
			}
			
			if (legalDescription.contains(" U #") || legalDescription.contains(" U#")) {
				String unit = legalDescription;
				unit = unit.replaceFirst("(?is).*\\bU\\b\\s*#\\s*(\\d+).*", "$1").trim();
				if (StringUtils.isNotEmpty(unit)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), Roman.normalizeRomanNumbers(unit));	
					legalDescription = legalDescription.replaceFirst("(?is)(.*)\\bU\\b\\s*#\\s*(?:\\d+,?)(.*)", "$1" + "$2").trim();
				}
			}
			
			if (legalDescription.contains(" BLD #") || legalDescription.contains(" BLD#") || legalDescription.contains(" BLDG #") || legalDescription.contains(" BLDG#")) {
				String bldg = legalDescription;
				bldg = bldg.replaceFirst("(?is).*\\bBLDG?\\b\\s*#\\s*([A-Z]?-?\\d+).*", "$1").trim();
				if (StringUtils.isNotEmpty(bldg)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), Roman.normalizeRomanNumbers(bldg));	
					legalDescription = legalDescription.replaceFirst("(?is)(.*)\\bBLDG?\\b\\s*#\\s*(?:[A-Z]?-?\\d+,?)(.*)", "$1" + "$2");
				}
			}
			
			if (legalDescription.contains(" BLOCK") || legalDescription.contains(" BLK")) {
				String blk = legalDescription;
				blk = blk.replaceFirst("(?is).*\\bBL(?:OC)K\\b\\s+([[A-Z]\\d-]).*", "$1").trim();
				if (StringUtils.isNotEmpty(blk)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), Roman.normalizeRomanNumbers(blk));	
					legalDescription = legalDescription.replaceFirst("(?is)(.*)\\bBL(?:OC)K\\b\\s+(?:[[A-Z]\\d-],?)(.*)", "$1" +"$2");
				}
			}
			
			
			if (legalDescription.contains(" LT") || legalDescription.contains(" LTS") || legalDescription.contains(" LOT") || legalDescription.contains(" LOTS")) {
				String lot = legalDescription.trim();
				lot = lot.replaceAll("(?is).*\\bLO?TS?\\s*([A-Z\\d\\s,-]+).*", "$1 ");
				if (StringUtils.isNotEmpty(lot)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));
					legalDescription = legalDescription.replaceAll("(?is)(.*)\\bLO?TS?\\s*(?:[A-Z\\d\\s,-]+,?)(.*)", "$1" + "$2").trim();
				}
			}
			
			legalDescription = legalDescription.replaceAll("(?is)\\bOF\\s*\\d\\s*(?:&\\s*ALL\\s*)?", "");
			legalDescription = legalDescription.replaceAll(",\\s*\\.?\\s*"," ");
					
			if (legalDescription.matches("(?is)\\s*([[A-Z]\\d\\s]+(?:CONDO(?:MINIUMS?)?)?)[,\\s]*")) {
				String subdiv = legalDescription.trim();
				Matcher m = Pattern.compile("(?is)\\s*\\bA\\s*0+(\\d+)([A-Z\\s]+)").matcher(subdiv);
				if (m.find()) {
					String abstractNo = m.group(1).trim();
					subdiv = m.group(2).trim();
					if (StringUtils.isNotEmpty(abstractNo))
						resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), abstractNo);
				}
				if (StringUtils.isNotEmpty(subdiv)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);	
				}
			} else if (StringUtils.isNotEmpty(legalDescription)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legalDescription.trim());
			}
		}
		
		return;
	}
	
	
	public int  getIncrementationValue (TableRow[] rows) {
		int noOfRows = rows.length;
		int idx = 0;
		
		if (noOfRows > 1) {
			String regExp = "(?is)\\d{4}\\s*Total\\s*:";
			String text = rows[1].getColumns()[1].getChildrenHTML().trim();
			Matcher m = Pattern.compile(regExp).matcher(text);
			
			while (idx < noOfRows && !m.find()) {
				idx ++;
				text = rows[idx].getColumns()[1].getChildrenHTML().trim();
				m = Pattern.compile(regExp).matcher(text);
			}
		}
		return idx;
	}
	
	public int findNoOfTaxYears (TableRow[] rows) {
		int noOfYears = 0;
		int noOfRows = rows.length;
		noOfYears = noOfRows / getIncrementationValue(rows);
		
		return noOfYears;
	}
	
	public void parseTaxes(String taxYear, TableTag taxTable, ResultMap map, long searchId) {
		String baseAmount = "";
		String amountDue = "";
		String yearInfo = "";
		String amountPaid = "";
		double priorDelinq = 0;
		double ad = 0;
		
		TableTag table = taxTable;
		ResultTable receipts = new ResultTable();
		Map<String, String[]> tmpMap = new HashMap<String, String[]>();
		String[] header = { "ReceiptAmount" , "ReceiptDate" };
		List<List<String>> bodyRT = new ArrayList<List<String>>();
		NumberFormat formatter = new DecimalFormat("#.##");	
		
		if (table != null) {
			TableRow[] rows = table.getRows();
			if (rows.length > 1) {
				//int noOfTaxYears = rows.length / 5;
				int incrementatVal = getIncrementationValue(rows);
				int noOfTaxYears = findNoOfTaxYears(rows);
				
				for (int i = 1; i <= noOfTaxYears; i++ ) {
					//int idx = 5*i;
					int idx = i * incrementatVal;
					TableColumn[] cols = rows[idx].getColumns();
					if (cols.length == 9) {
						yearInfo = cols[1].getChildrenHTML().trim().replaceFirst("(?is)\\s*(\\d{4}).*", "$1");
						if (yearInfo.equals(taxYear)) {
							map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
							
							baseAmount = cols[3].getChildrenHTML().replaceAll("(?is)[\\$,]", "").trim();
							if (StringUtils.isNotEmpty(baseAmount) && !"N\\A".equals(baseAmount)) {
								map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
							}
							
							amountPaid = cols[4].getChildrenHTML().replaceAll("(?is)[\\$,]", "").trim();
							if (StringUtils.isNotEmpty(amountPaid) && !"N\\A".equals(amountPaid)) {
								map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
							}
							
							amountDue = cols[8].getChildrenHTML().replaceAll("(?is)[\\$,]", "").trim();
							if (StringUtils.isNotEmpty(amountDue) && !"N\\A".equals(amountDue)) {
								map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
							}
						}
						try {
							amountPaid = cols[4].getChildrenHTML().replaceAll("(?is)[\\$,]", "").trim();
							amountDue = cols[8].getChildrenHTML().replaceAll("(?is)[\\$,]", "").trim();
							if (!"N\\A".equals(amountDue))
								ad = Double.parseDouble(amountDue);
							
							if (StringUtils.isNotEmpty(amountPaid) && !"N\\A".equals(amountPaid)) {
								map.put(TaxHistorySetKey.RECEIPT_AMOUNT.getKeyName(), amountPaid);
							}

							List<String> paymentRow = new ArrayList<String>();
							if (!"N\\A".equals(amountPaid) && !"0.00".equals(amountPaid) && ad == 0) {
								paymentRow.add(amountPaid);
								paymentRow.add("");
								bodyRT.add(paymentRow);
								
							} else if (ad != 0 && !yearInfo.equals(taxYear)) {
								if (i > 1  &&  Integer.parseInt(yearInfo) < Integer.parseInt(taxYear)) {
									priorDelinq +=  ad;
									priorDelinq = Double.valueOf(priorDelinq);
								}
							}
								
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} 
		
		tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount" , ""});
		tmpMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
		try {
			receipts.setHead(header);
			receipts.setMap(tmpMap);
			receipts.setBody(bodyRT);
			receipts.setReadOnly();
			map.put("TaxHistorySet", receipts);	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), formatter.format(priorDelinq));
	}
	
	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				String pid =  new HtmlParser3(rsResponse).getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "accountInfoTable")).elementAt(0).getChildren().toHtml().trim();
				pid = pid.replaceFirst("(?is).*Property ID\\s*:\\s*</th>\\s*<td>(\\d+).*", "$1").trim();
				accountId.append(pid);
				return rsResponse;
			
			} else {
				try {
					StringBuilder details = new StringBuilder();
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					
					NodeList nodeList = htmlParser.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "content"));
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {
						if (nodeList.size() == 1) {
							Node htmlContent = nodeList.elementAt(0);
							
							String taxYear = "";
							NodeList years = htmlContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id","YearSelector"), true).elementAt(0).getChildren();
								if (years != null) {
									taxYear = years.extractAllNodesThatMatch(new  HasAttributeFilter("selected","selected")).elementAt(0).getText();
									taxYear = taxYear.replaceFirst("(?is).*value\\s*=\\s*\"(\\d{4})\".*", "$1").trim();
								}
							
							NodeList tmpDiv = htmlContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "account-detail"), true);
							
							if (tmpDiv != null && tmpDiv.size() == 2) {
								htmlContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "account-detail"), true).remove(1);
								
								tmpDiv = tmpDiv.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
										.extractAllNodesThatMatch(new HasAttributeFilter("class", "form"), true);
								
								if (tmpDiv != null && tmpDiv.size() == 3) {
									details.append("<div align=\"center\" style=\"width: 990px; height: 25px; valign: middle; background-color: darkblue;\" id=\"taxYearHeader\"> " +
											" <font color = \"white\"> <h2> Property Details for Tax Year " + taxYear + "</h2> </font> </div>");
									TableTag table = (TableTag) tmpDiv.elementAt(0);  //account info
									if (table != null) {
										details.append("<div align = \"left\" style=\"background-color: #E8E8E8; width: 990px;  height: 20px; valign: middle;\"><b> Account </b> </div>");
										table.setAttribute("id", "\"accountInfoTable\"");
										if (table.getRowCount() > 0 && table.getRow(0).getColumnCount() > 0) {
											String apn = table.getRow(0).getColumns()[0].getChildrenHTML().trim();	
											if (StringUtils.isNotEmpty(apn)) {
												accountId.append(apn);		
											}
										}
										details.append(table.toHtml());
									}
									
									table = (TableTag) tmpDiv.elementAt(1);  //address info
									if (table != null) {
										details.append(" <br> <div align = \"left\" style=\"background-color: #E8E8E8; width: 990px;  height: 20px; valign: middle;\"><b> Location </b> </div>");
										table.setAttribute("id", "\"addressInfoTable\"");
										details.append(table.toHtml());
									}
									
									table = (TableTag) tmpDiv.elementAt(2);  //owner info
									if (table != null) {
										details.append(" <br> <div align = \"left\" style=\"background-color: #E8E8E8; width: 990px; height: 20px; valign: middle;\"><b> Owner </b> </div>");
										table.setAttribute("id", "\"ownerInfoTable\"");
										details.append(table.toHtml());
										details.append("<br>");
									}
								}
								
								String htmlDetails = "";
								htmlContent = htmlContent.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "page"), true).elementAt(0);
								
								if (htmlContent.getChildren().size() > 5) {
									htmlContent.getChildren().remove(4);
									htmlContent.getChildren().remove(3);
									htmlContent.getChildren().remove(2);
									htmlContent.getChildren().remove(1);
									htmlContent.getChildren().remove(0);
									
									htmlDetails = htmlContent.toHtml().trim();
									htmlDetails = htmlDetails.replaceAll("(?is)<script[^>]+>.*</script>", "");
									htmlDetails = htmlDetails.replaceAll("(?is)<div class=\"disclaimer\"[^\\.]+[^/]+/div>\\s*", "");
									htmlDetails = htmlDetails.replaceAll("(?is)<div class=\\\"detail-title-right\\\">\\s*<a[^/]+/a>\\s*<span[^>]+>.*</span>\\s*</div>", "");
									htmlDetails = htmlDetails.replaceFirst("(?is)<div class=\"result-grid-tip\">[^/]+/div>", "");
									htmlDetails = htmlDetails.replaceAll("(?is)<div class=\"detail-title-left\">\\s*(Estimated Tax Due)\\s*</div>\\s*(?:<div[^/]+/div>)?", "$1");
									
									String divTitleFormat = "<br> <div align = \"left\" style=\"background-color: #E8E8E8; width: 990px;  height: 20px; valign: middle;\"><b> ";
									String divSubtitleFormat = "<br> <div align = \"left\" style=\"background-color: aliceblue; width: 990px;  height: 20px; valign: middle;\">";
									htmlDetails = htmlDetails.replaceAll("(?is)<div class=\\\"detail-title(?:\\s*print-page-break)?\\\">([^<]+)</div>", divTitleFormat + "$1" + " </b> </div>");
									htmlDetails = htmlDetails.replaceAll("(?is) <div class=\"detail-subtitle\">([^<]+</div>)", divSubtitleFormat + "$1" + " </div>");
									htmlDetails = htmlDetails.replaceFirst("(?is)(<div[^#]+#E8E8E8[^>]+>\\s*<b>\\s*Property Deed History\\s*</b>\\s*</div>\\s*<div[^>]+>\\s*<table[^>]+)", "$1" + " id=\"crossRefTable\"");
									htmlDetails = htmlDetails.replaceFirst("(?is)(<div[^#]+#E8E8E8[^>]+>\\s*<b>\\s*Property Roll Value History\\s*</b>\\s*</div>\\s*<div[^>]+>\\s*<table[^>]+)", "$1" + " id=\"assessmentInfoTable\"");
								}
								details.append(htmlDetails);
							}
						}
					
						return details.toString();
					}
					
				} catch (Throwable t){
					logger.error("Error while parsing details page data", t);
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return rsResponse;
	}

	String getTaxYear (String nodeId, NodeList list) {
		String taxYear = "";
		Node yearNode = list.extractAllNodesThatMatch(new HasAttributeFilter("id", nodeId), true).elementAt(0)
				.getChildren().extractAllNodesThatMatch(new TagNameFilter("h2"), true).elementAt(0);
		
		if (yearNode != null) {
			String tmp = yearNode.getFirstChild().getText();
			tmp = tmp.replaceFirst("(?is).*Tax Year\\s*(\\d{4}).*", "$1");
					
			if (StringUtils.isNotEmpty(tmp)) {
				taxYear = tmp.trim();
			}	
		}
		
		return taxYear;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("Error on loading page")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("No records exist for search criteria. Please try again.") > -1) {
			Response.getParsedResponse().setError(TSServer.NO_DATA_FOUND);
			return;
		}
		
		switch (viParseID) {			
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if (StringUtils.isEmpty(outputTable.toString())){
					outputTable.append(rsResponse);
				}
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }			
				break;
				
			
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:						
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("year", getTaxYear("taxYearHeader", new HtmlParser3(details).getNodeList()));
					
					if (isInstrumentSaved(accountId.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);					
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} else {
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();					
				}
				break;
				
			case ID_GET_LINK :
				if (rsResponse.contains("Property Details For Year")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
				}
				
				break;
				
			default:
				break;
		}
	}	
	
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		try {
			if (table.contains("Geographic ID")){
				HtmlParser3 htmlParser = new HtmlParser3(table);
				NodeList mainTableList = htmlParser.getNodeList();
				NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				
				if (tableList.size() == 0) {
					return intermediaryResponse;
				}
				
				TableTag mainTable = null; 
				
				if (tableList.size() == 2) 
					mainTable = (TableTag) tableList.elementAt(1);
				else if (tableList.size() == 1) 
					mainTable = (TableTag) tableList.elementAt(0);
					
				TableRow[] rows = mainTable.getRows();
				int noOfRows = rows.length;
				if (noOfRows > 1) {
					for (int i=1; i < noOfRows; i++) {
						TableRow row = rows[i];
						if(row.getColumnCount() > 1) {
							TableColumn[] cols = row.getColumns();
							if (cols.length == 9) {
								//row.getChildren().size = 19; we will delete last 2 cols <=> last 4 children
								row.removeChild(18);   
								row.removeChild(17);
								row.removeChild(16);
								row.removeChild(15);
							}
							
							String link = row.getText().trim().replaceFirst("(?is).*=\\\"/([^\\\"]+)\\\"\\s*", "$1");
							
							if (StringUtils.isNotEmpty(link)){
								link = dataSite.getLink() + link;
								link =  CreatePartialLink(TSConnectionURL.idGET) +  link;
								String rowHtml =  row.toHtml();
								rowHtml = rowHtml.replaceFirst("(?is)<td>\\s*(\\d{5,7})\\s*</td>", "<td><a href=\"" + link + "\">$1</a></td>");
								rowHtml = rowHtml.replaceFirst("(?is)\\s*<td\\s*class\\s*=\\s*\\\"result-grid-legal\\\"\\s*>[^<]+</td>\\s*", "\n");
								ParsedResponse currentResponse = new ParsedResponse();

								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
									
								ResultMap m = parseIntermediaryRow(row, searchId);
								m.removeTempDef();
								Bridge bridge = new Bridge(currentResponse, m, searchId);
									
								DocumentI document = (TaxDocumentI)bridge.importData();				
								currentResponse.setDocument(document);
									
								intermediaryResponse.add(currentResponse);
							}
						}
					}
					
					String header = "<table cellpadding=\"6\" cellspacing=\"0\" border=\"1\">"
							+ "<tr bordercolor=\"#000000\">"
							+ "<td><b> Property ID </b></td>"
							+ "<td><b> Geographic ID </b></td>"
							+ "<td><b> Type </b></td>"
							+ "<td><b> Owner </b></td>"
							+ "<td><b> Property Address </b></td>"
//							+ "<td><b> Legal Description </b></td>"
							+ "<td><b> Appraised Value </b></td>"
			                + "</tr>";
					StringBuilder footer =  getPrevAndNextLinks (response, htmlParser); 
					
					response.getParsedResponse().setHeader(header);
					outputTable.append(table);
					response.getParsedResponse().setFooter("</table>" + footer);
					
				} else if (noOfRows == 1) {
					//there is only header row, without any other results
					return intermediaryResponse;
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 7) {	
			String parcelID = "";
			String GeoID = "";
			String propType = "";
			String ownerNames = "";
			String address = "";
			String appraisedValue = "";
			if (cols[0].getChildCount() > 0) {
				parcelID = cols[0].getFirstChild().toHtml().trim();
				if (StringUtils.isNotEmpty(parcelID)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}
			}
				
			if (cols[1].getChildCount() > 0) {
				GeoID = cols[1].getFirstChild().toHtml().trim();
				if (StringUtils.isNotEmpty(GeoID)) {
					resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), GeoID);
				}
			}
				
			if (cols[2].getChildCount() > 0) {
				propType = cols[2].getFirstChild().toHtml().trim();
				if (StringUtils.isNotEmpty(propType)) {
					if ("Real".equals(propType)) 
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real estate");
					else 
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Other type");
				}
			}
				
			if (cols[3].getChildCount() > 0) {
				ownerNames = cols[3].getFirstChild().toHtml().trim().replaceAll("&nbsp;", " ");
				if (StringUtils.isNotEmpty(ownerNames)) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerNames);
					try {
						parseOwnerInfo(resultMap, searchId);
					} catch (Exception e) {
						logger.error("Issue at parsing names; Exception is: ", e);
						e.printStackTrace();
					}
				}
			}
				
			if (cols[4].getChildCount() > 0) {
				address = cols[4].getFirstChild().toHtml().trim().replaceAll("&nbsp;", " ");
				address = address.replaceAll(",", "");
				if (StringUtils.isNotEmpty(address)) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					try {
						parseAddressInfo(resultMap, searchId);
					} catch (Exception e) {
						logger.error("Issue at parsing address; Exception is: ", e);
						e.printStackTrace();
					}
				}
			}
				
//			String legalDesc = cols[5].getFirstChild().toHtml().trim().replaceAll("&nbsp;", " ");
//			if (StringUtils.isNotEmpty(legalDesc)) {
//			resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDesc);
//			try {
//				parseLegalInfo(resultMap, searchId);
//			} catch (Exception e) {
//				logger.error("Issue at parsing address; Exception is: ", e);
//				e.printStackTrace();
//			}
//		}
			
			if (cols[6].getChildCount() > 0) {
				appraisedValue = cols[6].getFirstChild().toHtml().trim().replaceAll("[\\$,\\s]", "");
				if (StringUtils.isNotEmpty(appraisedValue)) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), appraisedValue);
				}
			}
		}
		
		return resultMap;
	}
	
	
	public static void parseLegalInfo(ResultMap resultMap,long searchId) {
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		if(StringUtils.isNotEmpty(legalDescription))  {
			legalDescription = legalDescription.replaceAll("(?is)-?\\s*[\\d\\.]+\\s*(?:ACS?|SQF|SQ FT|SQ|SF)", "");
			
			if (legalDescription.contains(" LT ") || legalDescription.contains(" LT.") || legalDescription.contains(" LOT ")) {
				String lot = legalDescription.trim();
				lot = lot.replaceAll("(?is).*\\bLO?T\\.?\\s*(\\d+).*", "$1 ");
				legalDescription = legalDescription.trim().replaceAll("(?is)\\bLO?T\\b\\s*\\.?\\d+(.*)", "$1").trim();
				if (StringUtils.isNotEmpty(lot)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));	
				}
			}
			if (legalDescription.contains(" TRACT")) {
				String tract = legalDescription;
				tract = tract.replaceAll("(?is).*TRACT\\s*(\\d+).*", "$1 ");
				legalDescription = legalDescription.replaceAll("(?is)\\bTRACT\\b\\s*\\d+(.*)","$1");
				if (StringUtils.isNotEmpty(tract)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), Roman.normalizeRomanNumbers(tract));	
				}
			}
			if (legalDescription.matches(".*[A-Z\\s]+\\s*(?:\\bSC\\b\\s*\\d+)?")) {
				String section = legalDescription;
				String subdiv = legalDescription;
				if (legalDescription.contains(" SC ")) {
					section = section.replaceFirst("(?is).*\\bSC\\b\\s*(\\d+)", "$1");
				}
				subdiv = subdiv.replaceFirst("(?is)(.*)(?:\\bSC\\b\\s*\\d+)?", "$1");
				if (StringUtils.isNotEmpty(subdiv)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);	
				}
				if (StringUtils.isNotEmpty(section)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);	
				}
			}
			
		}
		
		return;
	}
	
	public static void parseAddressInfo(ResultMap map, long searchId) {
		String address = (String) map.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isNotEmpty(address)) {
			Matcher m = Pattern.compile("(?is)([^\\n]+)\\n([A-Z\\s]+)(\\d+{5}\\s*(?:-\\d+)?)").matcher(address);
			if (m.find()) {
				String cityName = m.group(2).trim();
				String zipCode = m.group(3).trim();
				address = m.group(1).trim();
				if (StringUtils.isNotEmpty(cityName))
					map.put(PropertyIdentificationSetKey.CITY.getKeyName(), cityName);
				if (StringUtils.isNotEmpty(zipCode))
					map.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zipCode);
			}
			String strNo = StringFormats.StreetNo(address);
			String strName = StringFormats.StreetName(address);

			if (StringUtils.isNotEmpty(strNo)) {
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), strNo);
			}
			if (StringUtils.isNotEmpty(strName)) {
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), strName);
			}
		}
	}
	
	public static void parseOwnerInfo(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		List<List> body = new ArrayList<List>();
		
		if(StringUtils.isNotEmpty(unparsedName)) {
			String regExp = "(?is)\\s*([[A-Z]\\s-]+)\\s+\\b&amp;\\b\\s+([A-Z]+\\s+[A-Z])\\s+([A-Z]+(?:\\s+-?[A-Z]+)?)\\s*\\.*";
			Matcher m = Pattern.compile(regExp).matcher(unparsedName);
			if (m.find()) {
		     	// AccountNo 62096: MORGAN KEVIN A & KAREN L JOHNSON MORGAN;  AccountNo 17769: WAITE DENNIS J & WANDA D JOHNSON; 
				unparsedName = m.group(1) + " AND " + m.group(3) + " " + m.group(2);
			} else {
				regExp = "(?is)\\s*([[A-Z]\\s-]+)\\s+\\bAND\\b\\s+([A-Z]+\\s+(?:[A-Z]\\s+)?)([A-Z]+(?:\\s+-?[A-Z]+)?)\\s*\\.*";
				m = Pattern.compile(regExp).matcher(unparsedName);
				if (m.find()) {
					// AccountNo 27791 (BLASINGAME DAVID A AND LISA M HOUCHIN);  AccountNo 55933 (SWINDELL MARK JR AND KELLY JOHNSON ); 
					unparsedName = m.group(1) + " AND " + m.group(3) + " " + m.group(2);
				} else {
					
					regExp = "(?is)([[A-Z]\\s-]+)\\s+\\bAND\\b\\s+\\b([A-Z]+)\\b\\s*\\.*";
					m = Pattern.compile(regExp).matcher(unparsedName);
					if (m.find()) {
						// AccountNo 57553: JOHNSON CHANDLER AND AMBER 
						unparsedName = m.group(1) + " & " + m.group(2);
					} else {
						// AccountNo 58900: THE WANDA S MITCHELL & SHARON C JOHNS... 
						regExp = "(?is)(\\s*(?:\\bTHE\\b\\s*)?[\\s[A-Z]'&\\d,]+(?:LIV(?:ING)?\\s*)?\\bTRUST\\b)\\s*&\\s*((?:[[A-Z]\\s-',]+)\\s*\\bTR(?:USTEE)?\\b)\\s*";
						m = Pattern.compile(regExp).matcher(unparsedName);
						if (m.find()) {
							//Property ID:	61665
							unparsedName = unparsedName.replaceFirst("(\\bTRUST\\b)\\s*&\\s*", "$1" + " AND ");
						}
					}
				}
			}
			
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
			unparsedName = unparsedName.replaceAll("&amp;", "&");
			unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
			unparsedName = unparsedName.replaceAll("&#39;", "'");
			
			String[] mainTokens = unparsedName.split(" AND");
											
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				String[] names = currentToken.split("&");
					
				names = StringFormats.parseNameNashville(currentToken, true);
					
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);												
			}
			
			try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	protected String getCurrentTaxYear(){
		
		try {
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			Calendar cal = Calendar.getInstance();
			if (dataSite != null) {
				cal.setTime(dataSite.getPayDate());
			}
			return Integer.toString(cal.get(Calendar.YEAR));
		} catch (Exception e) {
			logger.error(e);
			return "";
		}
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		//pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		RejectNonRealEstate propertyTypeFilter = new RejectNonRealEstate(searchId);
		propertyTypeFilter.setThreshold(new BigDecimal("0.95"));
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		String selectYearListAsHtml = getYearSelect("param_18_6", "param_18_6");  //for TaxYear select list
		if (Search.AUTOMATIC_SEARCH == searchType) {
			//in automatic, default value = current tax year; 
			//in PS default selection is last value from list
			String currentTaxYear = getCurrentTaxYear();
			String regExp = "(?is)(.*\\\"" + currentTaxYear +"\\\")";
			selectYearListAsHtml = selectYearListAsHtml.replaceFirst("selected ", "");
			selectYearListAsHtml = selectYearListAsHtml.replaceFirst(regExp, "$1 selected");
		}
		
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			if (hasPin()){
				//Search by PIN
				Collection<String> pins = getSearchAttributes().getPins(-1);
				if (pins.size() >= 1) { //multiple PINS
					for(String pinToUse: pins){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
						module.clearSaKeys();
						module.getFunction(0).forceValue(pinToUse);
						//module.getFunction(6).setHtmlformat(getYearSelect("param_18_6", "param_18_6"));
						module.getFunction(6).setHtmlformat(selectYearListAsHtml);
						module.getFunction(6).forceValue(getCurrentTaxYear());
						module.addFilter(propertyTypeFilter);
						modules.add(module);
					}
					if(modules.size() > 1) {
						// set list for automatic search 
						serverInfo.setModulesForAutoSearch(modules);
						resultType = MULTIPLE_RESULT_TYPE;
						return;
					}
				}
			}
			
		} else {
			if (hasPin()){
				String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin);
				//module.getFunction(6).setHtmlformat(getYearSelect("param_18_6", "param_18_6"));
				module.getFunction(6).setHtmlformat(selectYearListAsHtml);
				module.addFilter(propertyTypeFilter);
				modules.add(module);
			}
		}
		
		if (hasStreet()) {
			//Search by Property Address + exactMatch: ON
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).forceValue(streetNo);
			module.getFunction(2).forceValue(streetName);
			//module.getFunction(6).setHtmlformat(getYearSelect("param_18_6", "param_18_6"));
			module.getFunction(6).setHtmlformat(selectYearListAsHtml);
			if (Search.AUTOMATIC_SEARCH == searchType)  {
				module.getFunction(6).forceValue(getCurrentTaxYear());
				module.getFunction(8).forceValue("on");
			}
			module.addFilter(propertyTypeFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			modules.add(module);
		}
		if (hasStreet()) {
			//Search by Property Address + exactMatch: ONFF
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).forceValue(streetNo);
			module.getFunction(2).forceValue(streetName);
			//module.getFunction(6).setHtmlformat(getYearSelect("param_18_6", "param_18_6"));
			module.getFunction(6).setHtmlformat(selectYearListAsHtml);
			if (Search.AUTOMATIC_SEARCH == searchType)  
				module.getFunction(6).forceValue(getCurrentTaxYear());
			module.addFilter(propertyTypeFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner + exactMatch: ON
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			//module.getFunction(6).setHtmlformat(getYearSelect("param_18_6", "param_18_6"));
			module.getFunction(6).setHtmlformat(selectYearListAsHtml);
			if (Search.AUTOMATIC_SEARCH == searchType) {
				module.getFunction(6).forceValue(getCurrentTaxYear());
				module.getFunction(8).forceValue("on");
			}
			module.addFilter(propertyTypeFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.setIteratorType(1,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;m;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		if (hasOwner()){
			//Search by Owner + exactMatch: OFF
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			//module.getFunction(6).setHtmlformat(getYearSelect("param_18_6", "param_18_6"));
			module.getFunction(6).setHtmlformat(selectYearListAsHtml);
			if (Search.AUTOMATIC_SEARCH == searchType) 
				module.getFunction(6).forceValue(getCurrentTaxYear());
			module.addFilter(propertyTypeFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.setIteratorType(1,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;m;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
}
