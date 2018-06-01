package ro.cst.tsearch.utils.helpers;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;



public class CensusParser {
	
	public static void main(String[] args) {
		
//		HttpClient httpclient = new HttpClient();
//		
//		HttpMethodParams methodParams = new HttpMethodParams() ;
//		methodParams.setSoTimeout(20000);           		
//		methodParams.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
//		methodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY );
//		methodParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, true));
//
//		GetMethod httpget = new GetMethod("http://quickfacts.census.gov/qfd/index.html");
//		
//		httpget.setParams(methodParams);
//		httpget.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16 ( .NET CLR 3.5.30729; .NET4.0C)");
//		
//		httpclient.executeMethod(httpget);
//		
//		String asString = httpget.getResponseBodyAsString();
		
		try {
			
			String stateStart = "http://quickfacts.census.gov/qfd/states/";
			
			Parser parser = new Parser("http://quickfacts.census.gov/qfd/index.html");
			NodeList nodeList = parser.parse(null);
			
			SelectTag stateSelect = (SelectTag)nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "State"), true)
				.extractAllNodesThatMatch(new TagNameFilter("select"), true).elementAt(0);
			OptionTag[] optionTags = stateSelect.getOptionTags();
			for (OptionTag optionTag : optionTags) {
				String statePage = optionTag.getValue();
				if(!statePage.equals("00000.html")) {
					System.out.println("Found state: " + optionTag.getOptionText());
					
					Parser parserState = new Parser(stateStart + statePage);
					NodeList nodeListState = parserState.parse(null);
					
					SelectTag countySelect = (SelectTag)nodeListState
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "County"), true)
							.extractAllNodesThatMatch(new TagNameFilter("select"), true)
							.elementAt(0);
					
					OptionTag[] countyOptionTags = countySelect.getOptionTags();
					for (OptionTag countyTag : countyOptionTags) {
						String countyTagText = countyTag.getOptionText();
						if(!"Select a county".equals(countyTagText)) {
							String countyName = countyTagText.replaceAll("\\s+County$", "");
							System.out.println("\tFound county: " + countyName);
							Parser parserCounty = new Parser(stateStart + countyTag.getValue());
							NodeList nodeListCounty = parserCounty.parse(null);
							
							TableTag mainTable = (TableTag)nodeListCounty.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("title", "People QuickFacts"))
								.elementAt(0);
							
							TableRow[] rows = mainTable.getRows();
							long popEstimate = 0;
							long houseEstimate = 0;
							for (TableRow tableRow : rows) {
								TableColumn[] columns = tableRow.getColumns();
								if(columns.length == 4 && popEstimate == 0 && columns[1].toPlainTextString().trim().matches("Population, \\d+ estimate")) {
									try {
										popEstimate = Long.parseLong(columns[2].toPlainTextString().trim().replace(",", ""));
									} catch (Exception e) {
										System.err.println("Cannot parse popEstimate for " + optionTag.getOptionText() + "/" + countyName + " because value is " + columns[2].toPlainTextString().trim());
									}
								} else if(columns.length == 4 && houseEstimate == 0 && columns[1].toPlainTextString().trim().matches("Housing units, \\d+")) {
									try {
										houseEstimate = Long.parseLong(columns[2].toPlainTextString().trim().replace(",", ""));
									} catch (Exception e) {
										System.err.println("Cannot parse houseEstimate for " + optionTag.getOptionText() + "/" + countyName + " because value is " + columns[2].toPlainTextString().trim());
									}
								}
								if(popEstimate > 0 && houseEstimate > 0) {
									break;
								}
							}
							
							if(popEstimate == 0) {
								System.err.println("Couldnot get popEstimate for " + optionTag.getOptionText() + "/" + countyName);
							}
							if(houseEstimate == 0) {
								System.err.println("Couldnot get houseEstimate for " + optionTag.getOptionText() + "/" + countyName);
							}
							
							FileUtils.writeStringToFile(new File("D:\\census.txt"), 
									optionTag.getOptionText() + ", " + countyName + "," + popEstimate + "," + houseEstimate + "\n", true);
							
						
						}
					}
					
					
				}
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

}
