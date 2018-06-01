package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.types.COGenericTylerTechTR;
import ro.cst.tsearch.utils.StringUtils;

public class COBoulderTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		try {
		TableColumn[] cols = row.getColumns();

		row = (TableRow) cols[1].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("valign", "top"), true).elementAt(0);
		cols = row.getColumns();
		String address = cols[1].getChildren().elementAt(2).toPlainTextString().replaceAll("\\b0*\\b", "").trim();
		if (cols.length == 4) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), cols[0].getChildren().elementAt(2).toPlainTextString().trim());
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			org.htmlparser.Node city = cols[1].getChildren().elementAt(7);
			if (city != null) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.toPlainTextString().replaceAll("_", " ").trim());
			}
		}
				ParseNamesIntermediary(resultMap, cols[2].toPlainTextString().trim());

		GenericFunctions.parseAddressOnServer(resultMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		resultMap.removeTempDef();
		return resultMap;
	}

	public static void ParseNamesIntermediary(ResultMap m, String name) throws Exception {
		if(StringUtils.isNotEmpty(name)) {
			
			name = name.replaceAll("&amp;", "&");
			m.put("PropertyIdentificationSet.NameOnServer", name);
			
			name = name.replaceAll("\\d+\\s+Pct\\s+Each", "");
			name = name.replaceAll("\\bUnd\\s+[\\d/]+\\s+Int\\s+Jt[\\s&]*", "");
			name = name.replaceAll("(?is)\\bM\\s+D\\b", " MD ");
			name = name.replaceAll("(?is)\\bMd(\\s+|$)(?!PC(\\s|$))", " ");
			name = name.replaceAll("(?is)\\bFormerly[^$]+", "");
			name = name.replaceAll("(?is)\\b[F|A]KA\\b", "");
			name = name.replaceAll("(?is)\\b(TRUSTEE)\\s+of\\s+\\w+\\s+\\w+\\b", " $1");
			name = name.replaceAll("(?is)\\bUnd\\s*[\\d\\./]+\\s*(Pct)?\\s*Int\\b", "");
			name = name.replaceAll("(?is)\\b[\\d\\./]+\\s*Pct\\b", "");
			name = name.replaceAll("(?is)\\b([A-Z])\\s+0+\\b", "$1");
			name = name.replaceAll("(?is)\\b(et)\\s+(al|ux|vir)\\b", "$1$2");
			name = name.replaceAll("\\s*&\\s*$", "").trim();
			
			String coName = "";//R0001271    Wilson David S & Valerie L & Peter J
			if (name.matches("\\A\\w+\\s+\\w+\\s+\\w+\\s*&\\s*\\w+\\s+\\w\\s*&\\s*\\w+\\s+\\w$")){
				coName = name.replaceAll("\\A((\\w+)\\s+\\w+\\s+\\w+\\s*&\\s*\\w+\\s+\\w)\\s*&\\s*(\\w+\\s+\\w$)", "$2 $3");
				name = name.replaceAll("\\A((\\w+)\\s+\\w+\\s+\\w+\\s*&\\s*\\w+\\s+\\w)\\s*&\\s*(\\w+\\s+\\w$)", "$1");
			}
			
			if(name.contains("&")) {
				String[] values = name.split("&");
				String lastValueToken = values[values.length - 1].trim();
				String[] lastValueTokenArray =  lastValueToken.split("\\s+");
				if(lastValueTokenArray.length > 2 && NameUtils.isNotCompany(lastValueToken) && lastValueTokenArray[lastValueTokenArray.length-1].length()>1) {
					
					if(lastValueToken.matches("(?is)([A-Z]{1,2}\\.?)\\s+([A-Z]{2,})\\s+([A-Z]{2,})")) {
						name = name.replace(lastValueToken, "") + " " + lastValueToken.replaceAll("(?is)([A-Z]{1,2}\\.?)\\s+([A-Z]{2,})\\s+([A-Z]{2,})", "$3 $2 $1");
					} else {
					
						String newToken = lastValueTokenArray[lastValueTokenArray.length - 1];
						for (int i = 0; i < lastValueTokenArray.length - 1; i++) {
							newToken += " " + lastValueTokenArray[i];
						}
						
						name = name.replace(lastValueToken, "") + " " + newToken;
					}
				} else {
					if(lastValueTokenArray.length == 2) {
						String newToken = lastValueTokenArray[lastValueTokenArray.length - 1];
						if(LastNameUtils.isLastName(newToken)) {
							newToken += " ";
							
							for (int i = 0; i < lastValueTokenArray.length - 1; i++) {
								newToken += " " + lastValueTokenArray[i];
							}
							name = name.replace(lastValueToken, "") + " " + newToken;
							
						}
						
					}
				}
			}
		
			name = name.replaceAll("(\\s)(AND|OR)(\\s)", "$1&$3").replaceAll("/", " & ").replaceFirst("\\s*&$", "");
			
			List<List/*<String>*/> namesList = new ArrayList<List/*<String>*/>();
			
			String[] names = { "", "", "", "", "", "" };
			 String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
			
			if (NameUtils.isCompany(name)) {
				names[2] = name;
			}else {
				name = NameCleaner.cleanNameAndFix(name, new Vector<String>(), true);
				names = StringFormats.parseNameNashville(name, true);
//				StringFormats.parseNameNashville("Wertz Michael P & J David Grauer ");
//				StringFormats.parseNameNashville("Wertz Michael P & Grauer J David ");
//				StringFormats.parseNameNashville("Wertz Michael P & Grauer David J ");
				if(StringUtils.isEmpty(names[4])) {	//no spouse middle initial
					
					if (StringUtils.isNotEmpty(names[5]) && 
							LastNameUtils.isNotLastName(names[5]) && 
							FirstNameUtils.isFirstName(names[5]) &&
							NameUtils.isNotCompany(names[5])){
						names[4] = names[3];
						names[3] = names[5];
						names[5] = names[2];
					}
					
				}
				
				
				names = NameCleaner.tokenNameAdjustment(names);
				names = NameCleaner.removeUnderscore(names);
				
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				name = NameCleaner.removeUnderscore(name);
			}

			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2]), NameUtils
							.isCompany(names[5]), namesList);
			
			if (StringUtils.isNotEmpty(coName)){
				names = StringFormats.parseNameNashville(coName, true);
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
						NameUtils.isCompany(names[2]), NameUtils
								.isCompany(names[5]), namesList);
			}
			
			GenericFunctions.storeOwnerInPartyNames(m, namesList, true);
			
			
			m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
			m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
			m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
			m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
			m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
			m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
			
		}
	}
	
	public static void parseNames(HtmlParser3 parser, ResultMap m, long searchId, COGenericTylerTechTR server) throws Exception {
		Node ownersNode = HtmlParser3.findNode(parser.getNodeList(), "Owners");		
		Node addressNode = HtmlParser3.findNode(parser.getNodeList(), "Address");
		TableTag ownerAndOtherInfoTable = HtmlParser3.getFirstParentTag(ownersNode, TableTag.class);

		Set<String> allNames = new LinkedHashSet<String>();
		boolean moreNames = false;

		for (TableRow row : ownerAndOtherInfoTable.getRows()) {
			if (row.getColumnCount() >= 2) {
				TableColumn firstCol = row.getColumns()[0];
				if (firstCol.getChildren() != null && firstCol.getChildren().contains(ownersNode)) {
					moreNames = true;
				}
				if(firstCol.getChildren()!=null && firstCol.getChildren().contains(addressNode)) {
					break;	
				}
				if (moreNames) {
					TableColumn secondCol = row.getColumns()[1];
					String name = ro.cst.tsearch.servers.functions.COWeldTR.clean(secondCol.getStringText()).trim();
					name = name.replaceAll("[\\d\\./]+(?:\\s+INT)?\\b", "");
					if (name.endsWith("&")) {
						name = name.substring(0, name.length() - 1);
					}
					allNames.add(name.replaceAll("(?i)AKA", "&").trim());
				}
			}
		}
		m.put("PropertyIdentificationSet.NameOnServer", Arrays.toString(allNames.toArray()));
		server.parseName(allNames, m);
	}
}