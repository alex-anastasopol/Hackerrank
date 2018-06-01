package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
*/

public class CODenverAO {
		
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId) {
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String instrumentNo = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parcel"),"", true).trim();
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), instrumentNo);
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), "Denver");// Denver is a city-county
			
			String address = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Property Address"),"", true).trim();
			m.put("tmpAddress", address);
			
			NodeList trList = mainList.extractAllNodesThatMatch(new TagNameFilter("tr"), true);		
			TableRow row = (TableRow)trList.elementAt(1);
			TableColumn[] cols = row.getColumns();
			if (cols.length == 1){
				String year = cols[0].getStringText();
				year = year.replaceAll("(?is).*in-progress\\s*(\\d+)\\s*file.*", "$1");
				m.put(TaxHistorySetKey.YEAR.getKeyName(), year.trim());
			}
			if (mainList.size() > 6) {
				TableTag crossTab = (TableTag) mainList.elementAt(6);
				String tabel = crossTab.toHtml();
				tabel = tabel.replaceAll("(?is)<br>", "@@@@@");
				List<List<String>> crossList = HtmlParser3.getTableAsList(tabel, false);;
				
				for(List<String> list : crossList){
					if (list.contains("INSTRUMENT")){
						crossList.remove(0);
						break;
					}
				}

				@SuppressWarnings("rawtypes")
				List<List> body = new ArrayList<List>();
				List<String> line = new ArrayList<String>();
				
				for(List<String> list : crossList){
					if (list.get(0).contains("No Entries Found"))
						break;
					line = new ArrayList<String>();
					int i = 0;
					for (String item : list){
						if (i == 0) {
							//0130404002000
							item = org.apache.commons.lang.StringUtils.stripStart(item.replaceAll("(?is)\\A[A-Z]+", ""), "0");
						}
						if (i == 4) {
							item = item.replaceAll("[,\\$]", "");
						}
						if (i == 5) {
							String[] grTorTee = item.trim().split("@@@@@");
							if (grTorTee.length == 2) {
								line.add(grTorTee[0].trim());
								line.add(grTorTee[1].trim());
							} else {
								line.add(grTorTee[0].trim());
								line.add("");
							}
						} else {
							line.add(item);
						}
						i++;
					}
					body.add(line);
				}
				ResultTable rt = new ResultTable();;
				String[] header = {"InstrumentNumber", "RecordedDate", "DocumentType", "InstrumentDate", "SalesPrice", "Grantor", "Grantee"};
				@SuppressWarnings("rawtypes")
				List<List> newBody = new ArrayList<List>();
				try {
					if (body.size() > 0 && body.get(0).size() == header.length) {
						for (int i = body.size(); i > 0 ; i--){
							newBody.add(body.get(i-1));
						}
						
					}
				} catch (IndexOutOfBoundsException iobe) {
					iobe.printStackTrace();
				}
				rt = GenericFunctions2.createResultTable(newBody, header);
				m.put("SaleDataSet", rt);
			}
			
			row = (TableRow)trList.elementAt(5);
			cols = row.getColumns();
			String owners = "", legal = "";
			if (cols.length == 2){
				owners += cols[0].getStringText().trim();
				legal += cols[1].getStringText().trim();
			}
			
			row = (TableRow)trList.elementAt(6);
			cols = row.getColumns();
			if (StringUtils.isNotEmpty(cols[0].getStringText().trim())){
				owners += "@@@" + cols[0].getStringText().trim();
			}
			
			m.put("tmpOwner", owners);
			m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
			
			try {
				CODenverTR.parseAddressCODenverTR(m, searchId);
				CODenverTR.partyNamesCODenverTR(m, searchId);
				parseLegalCODenverAO(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	

	public static ResultMap parseIntermediaryRowCODenverAO(TableRow row, long searchId) throws Exception {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
		
		TableColumn[] cols = row.getColumns();
		int count = 0;
		for(TableColumn col : cols) {

			String contents = col.getChildren().elementAt(1).getChildren().toHtml();
			switch (count) {
			case 0:
				resultMap.put("tmpOwner", contents.trim());
				break;
			case 1:
				resultMap.put("tmpAddress", contents.trim());
				break;
			case 2:
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), contents.trim());
				break;
			default:
				break;
			}
			count++;
			
		}
		CODenverTR.partyNamesCODenverTR(resultMap, searchId);
		CODenverTR.parseAddressCODenverTR(resultMap, searchId);
		
		return resultMap;
	}
	
	public static void parseLegalCODenverAO(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)<br>", " ");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)&\\s+[NSWE]+\\s*[\\d/]+\\s*OF\\s*(\\d+)", "& $1");
		//legal = legal.replaceAll("(?is)&\\s+[NSWE]+\\s*(\\d+)\\s+", "& $1 ");
		legal = legal.replaceAll("(?is)\\s+[\\d/]+\\s*FT\\s+OF\\s*(\\d+)", " & $1");
		legal = legal.replaceAll("(?is)\\s+[NSEW]\\s*/\\s*\\d+\\s*OF\\s*(\\d+)", " L $1");
		legal = legal.replaceAll("(?is)(\\w),(\\w)\\s+(\\w)", "$1,$2,$3");
		legal = legal.replaceAll("(?is)\\s+&\\s+", "&");
		
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(L|LTS?|LOTS?)\\s*([\\d\\s&,-]+)");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			if (!ma.group(2).matches("[\\s&,-]+")) {
				lot = lot + " " + ma.group(2);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
		}
		p = Pattern.compile("(?is)\\b(L|LTS?|LOTS?)\\s+([A-Z,\\\"&]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("ALL", "").trim();
		lot = lot.replaceAll("\\\"", "").trim();
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(B|BLKS?|BLOCKS?)\\s*([\\d]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U(?:NIT)?)\\s*-?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), "UNIT ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract building from legal description
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s*-?\\s*(\\w+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract section/township/range from legal description
		p = Pattern.compile("(?is)\\bT\\s*(\\d+)\\s*R\\s*(\\d+)\\s*S\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(3));
			line.add(ma.group(1));
			line.add(ma.group(2));
			body.add(line);
		}
		
		try {
			GenericFunctions2.saveSTRInMap(m, body);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\bBLO?C?K\\s+([\\w\\s\\p{Punct}]+)(?:EXC|RES|SUB|$)");
		ma = p.matcher(legal);
		if (ma.find() && !"L".equals(ma.group(1).trim().split(" ")[0])) {
			subdiv = ma.group(1);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\A([\\w\\s\\p{Punct}]+)\\b(?:B|BLK|UNIT)\\b");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				subdiv = legal;
			}
		}
		if (subdiv.length() > 40) {
			subdiv = "";
		}
		if (subdiv.length() < 6){
			ma.reset();
			p = Pattern.compile("(?is)\\A([\\w\\s\\p{Punct}]+)\\b(?:B|BLK)\\b");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		subdiv = subdiv.replaceAll("&", " & ").trim();
		
		if (subdiv.length() > 1) {

			subdiv = subdiv.replaceAll("NO\\s+\\d+", "");
			subdiv = subdiv.replaceFirst("(.*)\\s+EXC\\b.*", "$1");
			subdiv = subdiv.replaceFirst(".*(?:PORTION|PTN)\\s+OF\\s+(.*)", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+B\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(?:PRK|GAR|PU)\\s*-.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(?:FILING|BLDG|PLOT).*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1");
			
			subdiv = subdiv.replaceFirst("(.*)\\s+SUB\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+RES(?:UB)?\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d+[ND|ST|RD|TH]).*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+\\sFLG(?:\\s+#?\\d+)?).*", "$1");
			subdiv = subdiv.replaceFirst("\\b[A-Z]\\b","");
			subdiv = subdiv.replaceFirst("\\bFLG\\b","");
			subdiv = subdiv.replaceAll("\\s+"," ").trim();
			
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			
			if (legal.matches(".*\\bCONDO.*"))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
		}
		
	}
	
}
