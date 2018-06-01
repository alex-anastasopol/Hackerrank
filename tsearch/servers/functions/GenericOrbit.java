package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
//import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
//import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;

public class GenericOrbit {
	
	/*@SuppressWarnings("rawtypes")
	public static void parseName(ResultMap m, long searchId) throws Exception {
		
		String grantor = (String) m.get("SaleDataSet.Grantor");
		String grantee = (String) m.get("SaleDataSet.Grantee");
	
		if(StringUtils.isEmpty(grantee) && StringUtils.isNotEmpty((String) m.get("SaleDataSet.GranteeLander"))) {
			grantee = (String) m.get("SaleDataSet.GranteeLander");
		}
		grantor = StringUtils.prepareStringForHTML(grantor);
		grantee = StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);
		
		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();
		
		parseNameInner(m,grantor,grantorList,searchId,false);
		parseNameInner(m,grantee,granteeList,searchId,true);
						
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList));
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	public static void parseNameInner(ResultMap m, String name, ArrayList<List> namesList, long searchId,boolean isGrantee) {
		
		String[] names = { "", "", "", "", "", "" };
		
		boolean isLender = true;
		if(isGrantee) {
			 isLender = !checkGranteeTrustee(m,name,namesList,searchId);
		}
		name = name.replaceAll(",\\s*([A-Z])\\s*,\\s*([A-Z]{1,3})", " $1, $2");//BLACK,JIMMY,D,III, B5471
		name = NameCleaner.cleanNameAndFix(name, new Vector<String>());
		name = name.replaceFirst("\\d+.*?\\bMO\\b.*?$", "");
		name = name.replaceFirst(",\\s*$","");
		
		if (NameUtils.isCompany(name, new Vector<String>(), true)) {
			names[2] = name;
			GenericFunctions.addOwnerNames(name, names, namesList);
		}else {	
			String[] namesArray = name.split("\\&");
			if(namesArray.length<3) {
				names = StringFormats.parseNameNashville(name);
				names = NameCleaner.tokenNameAdjustment(names);
				names = NameCleaner.removeUnderscore(names);
				name = NameCleaner.removeUnderscore(name);
				
				if(StringUtils.isNotEmpty(names[0]) && GenericFunctions1.nameSuffix3.matcher(names[0]).matches()) {
					names[1] = names[0];
					names[0] = "";
				}
				
				GenericFunctions.addOwnerNames(name, names, namesList);
			}else {
				String previousLast = "";
				for(String eachName: namesArray) {
					eachName = eachName.trim();
					if(!eachName.trim().matches("\\w{2,},.*?")) {
						eachName = previousLast + ", " + eachName;	
					}
					names = StringFormats.parseNameNashville(eachName);
					names = NameCleaner.tokenNameAdjustment(names);
					names = NameCleaner.removeUnderscore(names);
					eachName = NameCleaner.removeUnderscore(eachName);
					GenericFunctions.addOwnerNames(eachName, names, namesList);
					if(eachName.matches("\\w{2,},.*?")) {
						previousLast = names[2];
					}
				}
			}
		}	

		if(isLender) {
			for(List<String> l : namesList) {
				l.add("1");
			}
			String grantee =   (String) m.get("SaleDataSet.Grantee");;
			if(StringUtils.isNotEmpty(grantee)) {
				m.put("SaleDataSet.GranteeLander", grantee);
	        	m.put("SaleDataSet.Grantee", "");
			}
		}
	}*/
	
	@SuppressWarnings("rawtypes")
	public static void parseCourtNames(ResultMap resultMap) {
		
		String grantor = (String)resultMap.get(SaleDataSetKey.GRANTOR.getKeyName());
		ArrayList<List> grantorList = new ArrayList<List>();
		parseCourtName(grantor, grantorList);
		
		String grantee = (String)resultMap.get(SaleDataSetKey.GRANTEE.getKeyName());
		ArrayList<List> granteeList = new ArrayList<List>();
		parseCourtName(grantee, granteeList);
		
		if (grantorList.size()>0) {
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
			resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), GenericParsingOR.concatenateNames(grantorList));
		}
		
		if (granteeList.size()>0) {
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), GenericParsingOR.concatenateNames(granteeList));
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseCourtName(String name, ArrayList<List> list) {
		if (StringUtils.isNotEmpty(name)) {
			
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes, type, otherType;
			
			String split[] = name.split(" / ");
			
			for (int j=0;j<split.length;j++) {
				
				String each = split[j];
				
				if (!StringUtils.isEmpty(each)) {
					if (NameUtils.isCompany(each)) {
						names = new String[6];
						names[2] = each;
						names[0] = names[1] = names[3] = names[4] = names[5] = ""; 
					} else {
						names = StringFormats.parseNameNashville(each, true);
					}
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(each, names, suffixes[0],
							suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
				}
				
			}
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNameMOPlatteOR(ResultMap m, long searchId) throws Exception{
			
			String names[] = {"", "", "", "", "", ""};
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String tmpPartyGtor = (String)m.get("SaleDataSet.Grantor");
			if (StringUtils.isNotEmpty(tmpPartyGtor)){
				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)\\.\\s*;\\s+", ", &amp; ");
				
				tmpPartyGtor = StringUtils.prepareStringForHTML(tmpPartyGtor);
				tmpPartyGtor = NameCleaner.cleanFreeformName(tmpPartyGtor);

				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)\\s+\\d+\\s+\\w+[^\\*]+\\*", "");//DEVER, ROBERT L. & LINDA JEAN 106 MAIN ST, PARKVILLE, MO 64152 * C/O DANNY J. TINSLEY
				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)(\\w+\\s*,)\\s*([^,]+),\\s*AKA\\b", "$1 $2 & $1 ");//WILLIAMS, DAVID LEE, AKA DAVID L. & CLARA JO
				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is),\\s*FKA\\b", " # ");// FIRST SECURITY BANK OF BROOKFIELD, FKA SECURITY STATE BANK
				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)\\s*&\\s*(\\w+\\s*,)", " # $1");
				tmpPartyGtor = tmpPartyGtor.replaceAll(",?\\s*DBA\\s+", " # ");
				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is),?\\s*SUCCESSOR TO", " # ");
				tmpPartyGtor = tmpPartyGtor.replaceAll("\\b(C/O)\\b", " #$1 ");
				tmpPartyGtor = tmpPartyGtor.replaceAll(",\\s*$", "");
				String[] gtors = tmpPartyGtor.split("#");
				tmpPartyGtor = tmpPartyGtor.replaceAll("\\b(C/O)\\b", "");
				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)#", "&");
				for (String grantorName : gtors){
					grantorName = grantorName.replaceAll("\\bDEC(?:EASE)?D\\b", "");
					names = StringFormats.parseNameNashville(grantorName);
					if (grantorName.indexOf("C/O") != -1){
						grantorName = grantorName.replaceAll("(?is)\\bC/O\\b", "");
						names = StringFormats.parseNameDesotoRO(grantorName);
					}
					GenericFunctions.addOwnerNames(tmpPartyGtor, names, grantor);
				}

				m.put("SaleDataSet.Grantor", tmpPartyGtor);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor));
			}
			
			String tmpPartyGtee = (String)m.get("SaleDataSet.Grantee");
			if (StringUtils.isNotEmpty(tmpPartyGtee)){
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is)\\.\\s*;\\s+", ", &amp; ");
				
				tmpPartyGtee = StringUtils.prepareStringForHTML(tmpPartyGtee);
				tmpPartyGtee = NameCleaner.cleanFreeformName(tmpPartyGtee);
				
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is)\\s+\\d+\\s+\\w+[^\\*]+\\*", "");//DEVER, ROBERT L. & LINDA JEAN 106 MAIN ST, PARKVILLE, MO 64152 * C/O DANNY J. TINSLEY
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is)(\\w+\\s*,)\\s*([^,]+),\\s*AKA\\b", "$1 $2 & $1 ");//WILLIAMS, DAVID LEE, AKA DAVID L. & CLARA JO
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is),\\s*FKA\\b", " # ");// FIRST SECURITY BANK OF BROOKFIELD, FKA SECURITY STATE BANK
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is)\\s*&\\s*(\\w+\\s*,)", " # $1");
				tmpPartyGtee = tmpPartyGtee.replaceAll(",?\\s*DBA\\s+", " # ");
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is),?\\s*SUCCESSOR TO", " # ");
				tmpPartyGtee = tmpPartyGtee.replaceAll("\\b(C/O)\\b", " #$1 ");
				tmpPartyGtee = tmpPartyGtee.replaceAll(",\\s*$", "");
				String[] gtee = tmpPartyGtee.split("#");
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is)#", "&");
				tmpPartyGtee = tmpPartyGtee.replaceAll("\\b(C/O)\\b", "");
				for (String granteeName : gtee){
					granteeName = granteeName.replaceAll("\\bDEC(?:EASE)?D\\b", "");
					names = StringFormats.parseNameNashville(granteeName);
					if (granteeName.indexOf("C/O") != -1){
						granteeName = granteeName.replaceAll("(?is)\\bC/O\\b", "");
						names = StringFormats.parseNameDesotoRO(granteeName);
					}
					GenericFunctions.addOwnerNames(tmpPartyGtee, names, grantee);
				}
				m.put("SaleDataSet.Grantee", tmpPartyGtee);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee));
				GenericFunctions1.setGranteeLanderTrustee2(m, searchId, true);
			}
		
	}
	 
	@SuppressWarnings("rawtypes")
	public static boolean checkGranteeTrustee(ResultMap m, String name, ArrayList<List> namesList, long searchId) {
		  	
		String docType = (String) m.get("SaleDataSet.DocumentType");
		
		if (docType != null){
			if (DocumentTypes.checkDocumentType(docType, DocumentTypes.MORTGAGE_INT,null,searchId)){
				if(name.matches(".*?\\bTR(U(STEE)?)?\\b.*?")) {
					return true;
				}
			}
		}
		return false;        
    
	}
	
	public static String cleanName(String name) {
		name = name.replaceAll("(?is),\\s*(H/W|W/H|A[SM]P|DBA|[FN]KA|SGL|H/H)\\b", " & ");
		name = name.replaceAll("(?is),\\s*AS\\s+(TRUSTEES?)\\s*$", " $1");
		name = name.replaceAll("(?is),\\s*BY\\s+ATTORNEY\\s+IN\\s+FACT\\s*$", "");
		name = name.replaceAll("(?s)\\s*&\\s*$", "");
		return name;
	}
	
	@SuppressWarnings("rawtypes")
	public static ResultMap parseIntermediaryRow(String row, int mode, long searchId, int countyId) {
		ResultMap resultMap = new ResultMap();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(row, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String docNumber = null;
			String docType = null;
			TableTag refTable = null;
			String docAmount = null;
			TableTag grantorTable = null;
			TableTag granteeTable = null;
			String bookPage = null;
			String recorded = null;
			TableTag lotTable = null;
			TableTag blockTable = null;
			TableTag subdivisionTable = null;
			String subdivision = null;
			
			if (mode==ro.cst.tsearch.servers.types.GenericOrbit.INSTRUMENT_SEARCH && nodeList.size()>10) {
				docNumber = nodeList.elementAt(0).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				docType = nodeList.elementAt(1).toPlainTextString();
				refTable = (TableTag)nodeList.elementAt(2).getFirstChild();
				docAmount = nodeList.elementAt(3).toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]", "") .trim();
				grantorTable = (TableTag)nodeList.elementAt(4).getFirstChild();
				granteeTable = (TableTag)nodeList.elementAt(5).getFirstChild();
				bookPage = nodeList.elementAt(6).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				recorded = nodeList.elementAt(7).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				lotTable = (TableTag)nodeList.elementAt(8).getFirstChild();
				blockTable = (TableTag)nodeList.elementAt(9).getFirstChild();
				subdivisionTable = (TableTag)nodeList.elementAt(10).getFirstChild();
			} else if (mode==ro.cst.tsearch.servers.types.GenericOrbit.NAME_SEARCH && nodeList.size()>9) {
				docNumber = nodeList.elementAt(0).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				docType = nodeList.elementAt(1).toPlainTextString();
				refTable = (TableTag)nodeList.elementAt(2).getFirstChild();
				docAmount = nodeList.elementAt(3).toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]", "") .trim();
				grantorTable = (TableTag)nodeList.elementAt(4).getFirstChild();
				granteeTable = (TableTag)nodeList.elementAt(5).getFirstChild();
				recorded = nodeList.elementAt(6).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				lotTable = (TableTag)nodeList.elementAt(7).getFirstChild();
				blockTable = (TableTag)nodeList.elementAt(8).getFirstChild();
				subdivisionTable = (TableTag)nodeList.elementAt(9).getFirstChild();
			} else if (mode==ro.cst.tsearch.servers.types.GenericOrbit.LEGAL_SEARCH_LEVEL1 && nodeList.size()>2) {
				resultMap.put(SaleDataSetKey.BOOK.getKeyName(), nodeList.elementAt(0).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim());
				resultMap.put(SaleDataSetKey.PAGE.getKeyName(), nodeList.elementAt(1).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim());
				subdivision = nodeList.elementAt(3).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			} else if (mode==ro.cst.tsearch.servers.types.GenericOrbit.LEGAL_SEARCH_LEVEL2 && nodeList.size()>9) {
				docNumber = nodeList.elementAt(0).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				docType = nodeList.elementAt(1).toPlainTextString();
				refTable = (TableTag)nodeList.elementAt(2).getFirstChild();
				docAmount = nodeList.elementAt(3).toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]", "") .trim();
				grantorTable = (TableTag)nodeList.elementAt(4).getFirstChild();
				granteeTable = (TableTag)nodeList.elementAt(5).getFirstChild();
				bookPage = nodeList.elementAt(6).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				recorded = nodeList.elementAt(7).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				lotTable = (TableTag)nodeList.elementAt(8).getFirstChild();
				blockTable = (TableTag)nodeList.elementAt(9).getFirstChild();
			} else if (mode==ro.cst.tsearch.servers.types.GenericOrbit.COURT_SEARCH && nodeList.size()>3) {
				granteeTable = (TableTag)nodeList.elementAt(0).getFirstChild();
				docType = nodeList.elementAt(1).toPlainTextString();
				recorded = nodeList.elementAt(2).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
				docNumber = nodeList.elementAt(3).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			}
			
			if (docNumber!=null) {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), docNumber);
			}
			
			if (docType!=null) {
				docType = StringUtils.prepareStringForHTML(docType);
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType.trim());
			}
			
			if (refTable!=null) {
				if (refTable.getRowCount()>0) {
					List<List> tablebodyRef = new ArrayList<List>();
					List<String> list;
					for (int i=0; i<refTable.getRowCount(); i++) {
						String value = refTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						if (!"".equals(value)) {
							list = new ArrayList<String>();
							String[] split = value.split("/");
							if (split.length==2) {		//book-page
								list.add("");
								list.add(split[0]);
								list.add(split[1]);
							} else {					//instrument number
								list.add(value);
								list.add("");
								list.add("");
							}
							tablebodyRef.add(list);
						}
					}
					if (tablebodyRef.size()>0) {
						String[] headerRef = {CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(), CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
						ResultTable crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
						resultMap.put("CrossRefSet", crossRef);
					}
				}
			}
			
			if (docAmount!=null && docType!=null) {
				String docCateg = DocumentTypes.getDocumentCategory(docType, searchId);
				if (docCateg.equals(DocumentTypes.LIEN)) {
					resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), docAmount);
				} else if (docCateg.equals(DocumentTypes.MORTGAGE)) {
					resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), docAmount);
				}
			}
			
			if (grantorTable!=null) {
				if (grantorTable.getRowCount()>0) {
					StringBuilder sb = new StringBuilder();
					for (int i=0; i<grantorTable.getRowCount(); i++) {
						sb.append(cleanName(grantorTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim())).append(" / ");
					}
					String grantor = sb.toString().replaceFirst(" / $", "");
					if (!"".equals(grantor)) {
						resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);
					}
				}
			}
			
			if (granteeTable!=null) {
				if (granteeTable.getRowCount()>0) {
					StringBuilder sb = new StringBuilder();
					for (int i=0; i<granteeTable.getRowCount(); i++) {
						sb.append(cleanName(granteeTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim())).append(" / ");
					}
					String grantee = sb.toString().replaceFirst(" / $", "");
					if (!"".equals(grantee)) {
						resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);
					}
				}
			}
			
			if (bookPage!=null) {
				String[] split = bookPage.split("/");
				if (split.length==2) {
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), split[0].trim());
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), split[1].trim());
				}
			}
			
			if (recorded!=null) {
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded);
			}
			
			if (lotTable!=null && blockTable!=null && subdivisionTable!=null &&
				lotTable.getRowCount()>0 &&
				lotTable.getRowCount()==blockTable.getRowCount() && 
				blockTable.getRowCount()==subdivisionTable.getRowCount()) {
				
				List<List> bodyPIS = new ArrayList<List>();
				List<String> line;
				String section = "";
				String township = "";
				String range = "";
				
				for (int i=0;i<lotTable.getRowCount();i++) {
					line = new ArrayList<String>();
					String lot = lotTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					String block = blockTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					String subdivisionName = subdivisionTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					subdivisionName = StringUtils.prepareStringForHTML(subdivisionName);
					Matcher strMa = Pattern.compile(ro.cst.tsearch.servers.types.GenericOrbit.STR_PATTERN).matcher(subdivisionName);
					if (strMa.matches()) {
						section = strMa.group(3).replaceFirst("^0+", "");
						township = strMa.group(2).replaceFirst("^0+", "");
						range = strMa.group(1).replaceFirst("^0+", "");
//						subdivisionName = "";
					} else {
						subdivisionName = ro.cst.tsearch.servers.types.GenericOrbit.cleanSubdivisionName(subdivisionName, searchId, countyId);
					}
					line.add(subdivisionName);
					line.add(lot);
					line.add(block);
					line.add(section);
					line.add(township);
					line.add(range);
					bodyPIS.add(line);
				}
				
				if (bodyPIS.size() > 0) {
					String[] header = {PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(),
						       		   PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName()};
					ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
					resultMap.put("PropertyIdentificationSet", rt);
				}
			}
			
			if (subdivision!=null) {
				String section = "";
				String township = "";
				String range = "";
				subdivision = StringUtils.prepareStringForHTML(subdivision);
				Matcher strMa = Pattern.compile(ro.cst.tsearch.servers.types.GenericOrbit.STR_PATTERN).matcher(subdivision);
				if (strMa.matches()) {
					section = strMa.group(3).replaceFirst("^0+", "");
					township = strMa.group(2).replaceFirst("^0+", "");
					range = strMa.group(1).replaceFirst("^0+", "");
				} else {
					subdivision = ro.cst.tsearch.servers.types.GenericOrbit.cleanSubdivisionName(subdivision, searchId, countyId);
				}
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
			}
			
			if (mode==ro.cst.tsearch.servers.types.GenericOrbit.COURT_SEARCH) {
				parseCourtNames(resultMap);
			} else {
				GenericFunctions1.parseGrantorGranteeSetOrbit(resultMap, searchId);
				if (CountyConstants.MO_Clay==countyId||CountyConstants.MO_Jackson==countyId||CountyConstants.KS_Johnson==countyId) {
					GenericFunctions1.setGranteeLanderTrustee1(resultMap, searchId);
				}
				if (CountyConstants.MO_Jackson==countyId) {
					GenericFunctions1.correctBookPage(resultMap, searchId);
				}
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultMap;
	}
	
}
