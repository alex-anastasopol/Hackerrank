package ro.cst.tsearch.servers.types;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonUniqueFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.DeathRecordsDocumentI;

public class GenericDR extends TSServer {

	private static final long serialVersionUID = 3832960221478858648L;

	public GenericDR(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public GenericDR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			
			//retain only the div with the table with the first and last name entered 
			try {
				
				String text1 = "id='div_US_records_death_2008Death";
				String text2 = text1 + "Initial";
				
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList nodeList = htmlParser.parse(null);
				NodeList divs = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "network_results bdr_emb"));
				if (divs.size()!=0) {	//first page
					rsResponse = "";
					boolean done = false;
					for (int i=0;i<divs.size()&&!done;i++) {
						if (divs.elementAt(i).toHtml().contains(text1)) {
							done = true;
							if (divs.elementAt(i).toHtml().contains(text2)) {
								rsResponse = "";
							} else {
								rsResponse = divs.elementAt(i).toHtml();
							}
						}
					}
				}
				//otherwise, there is no need to retain only a div, the entire response is kept
				
			} catch (ParserException e) {
				e.printStackTrace();
			}
			
			// no result
			if (rsResponse.indexOf("Results on Multiple Databases") == -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found!</font>");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE,"true");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
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
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(accountId.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse( details );
				
			} else {
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse( details );
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			
			break;
		case ID_GET_LINK :
			ParseResponse(sAction, Response, rsResponse.contains("Save Record")
														? ID_DETAILS
														: ID_SEARCH_BY_NAME);
			break;
		default:
			break;
		}
		
	}
	
	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "DEATH CERTIFICATE");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
						
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<form")){
				NodeList tableList1 = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "560"));
				if(tableList1.size() == 0) {
					return null;
				}
				NodeList tableList2 = tableList1.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"));
				if(tableList1.size() == 0) {
					return null;
				}
				TableTag table = (TableTag)tableList2.elementAt(0);
				int column = 0;
				if (table.getRow(2).getColumnCount()==3) {
					column = 1;
				}
				accountId.append(table.getRow(2).getColumns()[column].toPlainTextString()
						.replaceAll("SSN", "").trim());
				return rsResponse;
			}
			
			NodeList tableList1 = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "560"));
			if(tableList1.size() == 0) {
				return null;
			}
			NodeList tableList2 = tableList1.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"));
			if(tableList1.size() == 0) {
				return null;
			}
			TableTag table = (TableTag)tableList2.elementAt(0);
			int column = 0;
			if (table.getRow(3).getColumnCount()==3) {
				column = 1;
			}
			accountId.append(table.getRow(3).getColumns()[column].toPlainTextString()
					.replaceAll("SSN", "").trim());
			
			details.append("<h2>Death Records</h2>");
			details.append(tableList1.elementAt(0).toHtml().replaceAll("align=\"center\"", "")
					.replaceFirst("(?is)<tr>\\s*</tr>", ""));
			
			return details.toString();
						
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList  = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor", "#CCCCCC"))
				.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"));
			
			if(mainTableList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			for (int i = 1; i < rows.length; i++) {		//row 0 is the header
				TableRow row = rows[i];
				if(row.getColumnCount() == 8) {
	
					String partialLink1 = "";
					String partialLink2 = "";
					Matcher matcher = Pattern.compile("(?is)onclick=\"std_details\\('(.*?)','(.*?)'\\);\"").matcher(row.toHtml());
					if (matcher.find()) {
						partialLink1 = matcher.group(1);
						partialLink2 = matcher.group(2);
					}
					String link = CreatePartialLink(TSConnectionURL.idPOST) + "/searches/US_records_death_2008.php?" 
						+ partialLink1 + "&act=details&rtrdiv=" + partialLink2;
					
					String cleanedRow = row.toHtml().replaceAll("(?is)onclick=\".*?\"", "");
					cleanedRow = cleanedRow.replaceAll("(?is)<tr.*?>", "<tr>");
					cleanedRow = cleanedRow.replaceAll("(?is)<input.*?>", "<a href=\"" + link + "\">details</a>");
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanedRow);
					currentResponse.setOnlyResponse(cleanedRow);
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.GenericDR.parseIntermediaryRow( row, searchId );
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DeathRecordsDocumentI document = (DeathRecordsDocumentI)bridge.importData();
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} 
			}
			response.getParsedResponse().setHeader(processLinks(response, nodeList)+ 
					"<table cellspacing=\"0\" cellpadding=\"5\" border=\"1\">" +
					"<tr><th>Last Name</th><th>First Name</th><th>Middle Name</th><th>Birth</th><th>Death</th>" + "" +
					"<th>SSN</th><th>State</th><th>Details</th></tr>");
			response.getParsedResponse().setFooter("</table>");
			
			outputTable.append(table);
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	protected String processLinks(ServerResponse response, NodeList nodeList) {
		String result = "";
		String pattern = "(?is)<td\\s+nowrap=\"nowrap\".*?>.*?<div>(.*?)</div>.*?<div>(.*?)</div>.*?</td>";
		
		NodeList linksList  = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "2"));
		
		if (linksList.size()>0)
			result = linksList.elementAt(0).toHtml().replaceAll("width=\"160\"", "").replaceAll("width=\"100%\"", "");
		
		String partialLink = CreatePartialLink(TSConnectionURL.idPOST);
		
		result = result.replaceAll("(?is)<div.*?onclick=\"std_results_page\\('(.*?)'.*?>(.*?)</div>", "<div><a href=\"" +
				partialLink + "/searches/US_records_death_2008.php?$1\">$2</a></div>");
		result = result.replaceAll("(?is)<a onclick=\"std_results_page\\('(.*?)'.*?>", "<a href=\"" +
				partialLink + "/searches/US_records_death_2008.php?$1\">");
		
		result = result.replaceAll("(?is)onmouse.*?=\".*?\"", "").replaceAll("(?is)onclick=\".*?\"", "");
		result = result.replaceAll("(?is)style=\".*?\"", "");
		result = result.replaceFirst(pattern, "<td>$2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;$1</td>");
		result = result.replaceFirst(pattern, "<td>$1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;$2</td>");
		
		result += "<br/>";
			
		return result;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		try {
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"NR");
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "DEATH CERTIFICATE");
			
			detailsHtml = detailsHtml.replaceFirst("(?is)<tr>\\s*</tr>", "");			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"));
			if(tableList.size() == 0) {
				return null;
			}
			
			boolean hasMiddle = false;
			String last = "";
			String first = "";
			String middle = "";
			String birthDate = "";
			String deathDate = "";
			String address = "";
			String ssn = "";
			String state = "";
			String pattern = "(?is)<strong>(.*?)</strong>";
			
			TableTag table = (TableTag)tableList.elementAt(0);
			if (table.getRow(2).getColumnCount()==3) {
				hasMiddle = true;
			}
			
			last = table.getRow(0).getColumns()[0].toHtml();
			Matcher ma1 = Pattern.compile(pattern).matcher(last);
			if (ma1.find()) {
				last = ma1.group(1).trim();
			} else {
				last = "";
			}
			first = table.getRow(0).getColumns()[1].toHtml();
			Matcher ma2 = Pattern.compile(pattern).matcher(first);
			if (ma2.find()) {
				first = ma2.group(1).trim();
			} else {
				first = "";
			}
			if (hasMiddle) {
				middle = table.getRow(0).getColumns()[2].toHtml();
				Matcher ma3 = Pattern.compile(pattern).matcher(middle);
				if (ma3.find()) {
					middle = ma3.group(1).trim();
				} else {
					middle = "";
				}
			}
			String name = last + " " + first;
			if (hasMiddle) {
				name += " " + middle;
			}
			name = name.trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			
			if (hasMiddle) {
				birthDate = table.getRow(1).getColumns()[0].toHtml();
			} else {
				birthDate = table.getRow(0).getColumns()[2].toHtml();
			}
			Matcher ma4 = Pattern.compile(pattern).matcher(birthDate);
			if (ma4.find()) {
				birthDate = ma4.group(1).trim();
			} else {
				birthDate = "";
			}
			if (hasMiddle) {
				deathDate = table.getRow(1).getColumns()[1].toHtml();
			} else {
				deathDate = table.getRow(1).getColumns()[0].toHtml();
			}
			Matcher ma5 = Pattern.compile(pattern).matcher(deathDate);
			if (ma5.find()) {
				deathDate = ma5.group(1).trim();
			} else {
				deathDate = "";
			}
			deathDate = deathDate.replaceAll("\\s\\([VP]\\)", "");
			if (!deathDate.substring(0,1).matches("\\d"))
				deathDate = "01 " + deathDate;
			DateFormat df1 = new SimpleDateFormat("dd MMM yyyy");
			DateFormat df2 = new SimpleDateFormat("MM/dd/yyyy");
			try {
				Date birth = df1.parse(birthDate);
				Date death = df1.parse(deathDate);
				birthDate = df2.format(birth);
				deathDate = df2.format(death);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), birthDate);
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), deathDate);
			
			if (hasMiddle) {
				address = table.getRow(1).getColumns()[2].toHtml();
			} else {
				address = table.getRow(1).getColumns()[1].toHtml();
			}
			Matcher ma6 = Pattern.compile(pattern).matcher(address);
			if (ma6.find()) {
				address = ma6.group(1).trim();
			} else {
				address = "";
			}
			if (!address.matches("\\(.*?\\)")) {
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address.replaceAll("[\\(\\)]", ""));
			}
			
			if (hasMiddle) {
				ssn = table.getRow(2).getColumns()[1].toHtml();
			} else {
				ssn = table.getRow(2).getColumns()[0].toHtml();
			}
			Matcher ma7 = Pattern.compile(pattern).matcher(ssn);
			if (ma7.find()) {
				ssn = ma7.group(1).trim();
			} else {
				ssn = "";
			}
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), ssn);
			
			if (hasMiddle) {
				state = table.getRow(2).getColumns()[2].toHtml();
			} else {
				state = table.getRow(2).getColumns()[1].toHtml();
			}
			Matcher ma8 = Pattern.compile(pattern).matcher(state);
			if (ma8.find()) {
				state = ma8.group(1).trim();
			} else {
				state = "";
			}
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), state);
			
			ro.cst.tsearch.servers.functions.GenericDR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.GenericDR.parseAddress(resultMap, searchId);
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		boolean hasOwner = hasOwner();
								
		if(hasOwner) {	//search with name and state, filter by name, save if there is a unique result
			FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			FilterResponse rejectNonUnique = new RejectNonUniqueFilterResponse(searchId);
			nameFilterOwner.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setSaKey(2, SearchAttributes.P_STATE_ABREV);
			module.addFilter(nameFilterOwner);
			module.addFilter(rejectNonUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"F L;;"}));
			moduleList.add(module);
		}
		
		if(hasOwner) {	//search with name, filter by name, save if there is a unique result
			FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			FilterResponse rejectNonUnique = new RejectNonUniqueFilterResponse(searchId);
			nameFilterOwner.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.forceValue(2, "ALL");
			module.addFilter(nameFilterOwner);
			module.addFilter(rejectNonUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"F L;;"}));
			moduleList.add(module);
		}
				
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
