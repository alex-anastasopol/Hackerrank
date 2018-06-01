package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;

public class KYJeffersonAOFunctions {
	
	protected static final Category logger = Logger.getLogger(KYJeffersonAOFunctions.class);

	public static ResultMap parseIntermediaryRow(TableRow row, String linkPrefix, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");
		try {
			TableColumn[] cols = row.getColumns();
			if(cols.length == 5) {
				NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				for (int j = 0; j < linkList.size(); j++) {
					LinkTag linkTag = (LinkTag) linkList.elementAt(j);					
					String link = linkPrefix + linkTag.extractLink().trim().replaceAll("\\s", "%20") + "/";
					linkTag.setLink(link);
					resultMap.put("tmpLink", link);
				}
				
				resultMap.put("PropertyIdentificationSet.ParcelID",cols[4].toPlainTextString().trim());

				String[] address = StringFormats.parseAddress(cleanAddress(cols[1].toPlainTextString().trim()));
				resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
				resultMap.put("PropertyIdentificationSet.StreetName", address[1]);
				
				String nameOnServer = cols[2].toPlainTextString().trim();
				nameOnServer = nameOnServer.replace("&amp;", "&");
				
				GenericFunctions2.parseName(resultMap, nameOnServer);
				
				NodeList rowChildrenList = row.getChildren();
				for (int i = 0; i < rowChildrenList.size(); i++) {
					if(rowChildrenList.elementAt(i) instanceof TableColumn ) {
						row.removeChild(i);
						break;
					}
				}
				
			}
		} catch (Exception e) {
			logger.error("Exception when parsing Intermediary Row", e);
		}
		return resultMap;
	}

	public static String cleanAddress(String orignalAddress) {
		orignalAddress = orignalAddress.replaceAll("(?is)^Openspace$", "");
		orignalAddress = orignalAddress.replaceAll("(?is)^Address\\s+Unknown$", "");
		return orignalAddress;
	}

	public static void parseLegalLinesTable(ResultMap map, Node nodeById) {
		try {
			if(nodeById != null) {
				TableTag table = (TableTag)nodeById;
				TableRow[] rows = table.getRows();
				for (int i = 1; i < rows.length; i++) {
					TableColumn[] columns = rows[i].getColumns();
					if(columns.length == 2) {
						map.put("tmpLegal" + i, columns[1].toPlainTextString().trim());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception when parsing the parseSalesHistoryTable", e);
		}
		
	}

	public static void parseAssessmentHistoryTable(ResultMap map, Node nodeById) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("unchecked")
	public static void parseSalesHistoryTable(ResultMap map, Node nodeById) {
		try {
			if(nodeById != null) {
				
				List<List> body = new ArrayList<List>();
				List<String> line = null;
				
				TableTag table = (TableTag)nodeById;
				TableRow[] rows = table.getRows();
				for (int i = 1; i < rows.length; i++) {
					TableColumn[] columns = rows[i].getColumns();
					if(columns.length == 4) {
						line = new ArrayList<String>();
						String bookPage = columns[0].toPlainTextString().trim();
						if(bookPage.matches("\\d+\\s\\d+")) {
							line.add(bookPage.replaceAll("(\\d+)\\s(\\d+)", "$1"));
							line.add(bookPage.replaceAll("(\\d+)\\s(\\d+)", "$2"));
							line.add(columns[2].toPlainTextString().trim());
							line.add(columns[1].toPlainTextString().replaceAll("[,$]", "").trim());
							line.add(columns[3].toPlainTextString().trim());
							body.add(line);	
						}
					}
				}
				//adding all cross references - should contain transfer table and info parsed from legal description
				if(body != null && body.size() > 0) {
					ResultTable rt = new ResultTable();
					String[] header = {"Book", "Page" , "RecordedDate", "SalesPrice", "Grantor"};
					rt = GenericFunctions2.createResultTable(body, header);
					map.put("SaleDataSet", rt);
				}
			}
		} catch (Exception e) {
			logger.error("Exception when parsing the parseSalesHistoryTable", e);
		}
		
	}

	public static void parseRightTable(ResultMap map, Node nodeById) {
		try {
			if(nodeById != null) {
				TableTag table = (TableTag)nodeById;
				for (TableRow row : table.getRows()) {
					TableColumn[] columns = row.getColumns();
					if(columns.length == 2) {
						String key = columns[0].toPlainTextString().trim();
						if(key.equalsIgnoreCase("OWNER")) {
							String nameOnServer = columns[1].toPlainTextString().trim();
							nameOnServer = nameOnServer.replace("&amp;", "&");
							map.put("PropertyIdentificationSet.OwnerLastName", nameOnServer);
							//GenericFunctions2.parseName(map, nameOnServer);
						} else if(key.equalsIgnoreCase("Assessed Value")) {
							map.put("PropertyAppraisalSet.TotalAssessment", columns[1].toPlainTextString().trim().replaceAll("[$,]", ""));
						} else if(key.equalsIgnoreCase("Improvements Value")) {
							map.put("PropertyAppraisalSet.ImprovementAppraisal", columns[1].toPlainTextString().trim().replaceAll("[$,]", ""));
						} else if(key.equalsIgnoreCase("Land Value")) {
							map.put("PropertyAppraisalSet.LandAppraisal", columns[1].toPlainTextString().trim().replaceAll("[$,]", ""));
						} else if(key.equalsIgnoreCase("Property Class")) {
							map.put("tmpPropertyClass", columns[1].toPlainTextString().trim());
						} else if(key.equalsIgnoreCase("Building Type")) {
							map.put("tmpBldgType", columns[1].toPlainTextString().trim());
						} else if(key.equalsIgnoreCase("Building Characteristics")) {
							map.put("tmpBuildingType", columns[1].toPlainTextString().trim());
						} else if(key.equalsIgnoreCase("Old District")) {
							map.put("PropertyIdentificationSet.ParcelIDGroup", columns[1].toPlainTextString().trim());
						} else if(key.equalsIgnoreCase("Satellite City")) {
							map.put("PropertyIdentificationSet.City", columns[1].toPlainTextString().trim());
						} else if(key.equalsIgnoreCase("Parent Parcel")) {
							map.put("PropertyIdentificationSet.ParentParcelID", columns[1].toPlainTextString().trim());
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception when parsing the RightTable (main table)", e);
		}
		
	}

	public static void parseLeftTable(ResultMap map, Node nodeById) {
		try {
			if(nodeById != null) {
				TableTag table = (TableTag)nodeById;
				for (TableRow row : table.getRows()) {
					TableColumn[] columns = row.getColumns();
					if(columns.length == 2) {
						String key = columns[0].toPlainTextString().trim();
						if(key.equalsIgnoreCase("Building Type")) {
							map.put("tmpBldgType", columns[1].toPlainTextString().trim());
						} else if(key.equalsIgnoreCase("Building Characteristics")) {
							map.put("tmpBuildingType", columns[1].toPlainTextString().trim());
						} else if(key.equalsIgnoreCase("Parent Parcel")) {
							map.put("PropertyIdentificationSet.ParentParcelID", columns[1].toPlainTextString().trim());
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception when parsing the RightTable (main table)", e);
		}
		
	}

}
