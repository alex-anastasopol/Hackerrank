package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;

public class XXStewartPriorPF {
	
	private static Pattern lotDetailedPattern = Pattern.compile
		("(?is)LotFrom\\s*:\\s*([\\d\\w &]+)\\s*;\\s*LotTo\\s*:\\s*([\\d\\w &]+)\\s*;");
	private static Pattern sectionDetailedPattern = Pattern.compile
		("(?is)Sec(?:tion)?\\s*:?\\s*([\\d\\w]+)[\\s;]+");
	private static Pattern blockDetailedPattern = Pattern.compile
		("(?is)Bl(?:oc)?k\\s*:\\s*([\\d\\w]+)\\s*;");
	private static Pattern phaseDetailedPattern = Pattern.compile
		("(?is)PHASE\\s*:?\\s*([\\d\\w]+)\\s*;");	
	private static Pattern tractDetailedPattern = Pattern.compile
		("(?is)TRACT\\s*:?\\s*([\\d\\w]+)\\s*;");
	private static Pattern sectTownRangesDetailedPattern = Pattern.compile
		("(?is)([\\d]+)\\s*-\\s*([\\d]+)\\s*-\\s*([\\d]+)\\b");
	private static Pattern unitDetailedPattern = Pattern.compile
		("(?is)UnitFrom\\s*:\\s*([\\d\\w-\\s]+);\\s*UnitTo\\s*:\\s*([\\d\\w-\\s]+);");
	private static Pattern unitShortPattern = Pattern.compile
		("(?is)Unit\\s*:?\\s*([\\d\\w-\\s]+);?\\b");
	private static Pattern bldgDetailedPattern = Pattern.compile
		("(?is)Bldg\\s*:?\\s*([a-zA-Z]+)\\s*;");	
	private static Pattern subdivisionNameDetailedPattern = Pattern.compile
		("(?is)Name\\s*:?\\s*([^;]+)\\s*;|$");	
	
	@SuppressWarnings("unchecked")
	public static void partyNames(String rawInfo, ResultMap m) throws Exception {
		
		if (StringUtils.isEmpty(rawInfo)){
			return;
		}
		
		rawInfo = rawInfo.replaceAll("(?is)\\b(Trustee\\s+of\\s+the)\\b", "$1; ");
		rawInfo = rawInfo.replaceAll("(?is),?\\s*as\\s+to\\s+an\\s+undivided(?:\\s*[\\d\\.\\%]+)?\\b", "");
		rawInfo = rawInfo.replaceAll("(?is)\\band\\s+Successor\\s+Trustees\\s+there\\s+under\\b", "");
		rawInfo = rawInfo.replaceAll("(?is)\\bUNMARRIED,?", "");
		rawInfo = rawInfo.replaceAll("(?is)[\\(\\)]+", "");
		String splitOwner[] = rawInfo.split("\n");
		
		
		
		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		for(int i =0; i<splitOwner.length; i++){
			int type = 0;
			if(!splitOwner[i].isEmpty()) {
				String names[] = {"", "", "", "", "", ""};
				String[] suffixes, typeName, otherType;
				
				if(splitOwner[i].startsWith("Seller:")) {
					splitOwner[i] = splitOwner[i].replaceFirst("Seller:", "").trim();
					type = 1;
				} else if(splitOwner[i].startsWith("Buyer:")) {
					splitOwner[i] = splitOwner[i].replaceFirst("Buyer:", "").trim();
					type = 2;
				} else if(splitOwner[i].startsWith("Lender:")) {
					splitOwner[i] = splitOwner[i].replaceFirst("Lender:", "").trim();
					type = 3;
				}
				
				splitOwner[i] = splitOwner[i].replaceAll("(?is),\\s*AKA\\s+([^,]*),\\s*([A-Z]+(?:\\s+[A-Z]\\.)?)", " $2; $1");//HEATH, JR., AKA ROY LOYDE HEATH JR, ROY L.
				String[] nameTokens = splitOwner[i].split(";");
				for (int j = 0; j < nameTokens.length; j++) {
					String cleanedName = cleanName(nameTokens[j].trim());
					if(cleanedName.trim().isEmpty()) {
						continue;
					}
					cleanedName = cleanedName.replaceAll("(?is)\\s+([sjr]{2}\\.?\\,?)\\s+(\\w+)\\s*$", "$2 $1");//Jr Isaac Arnold
					cleanedName = cleanedName.replaceAll("(?is)\\s+((?:[ivX]{1,4}\\,?)|(?:[sjr]{2}[\\,\\.]?))\\s+(\\w+(\\s+\\w\\.?)?)\\s*$", 
																" $2 $1");//Coogan III, George H.; HEATH, JR. ROY L.
					cleanedName = cleanedName.replaceAll("(?is),?\\s*,( Trustee) Of The\\b", " TR");
					if (!cleanedName.contains(",")){
						names = StringFormats.parseNameDesotoRO(cleanedName, true);
					} else {
						names = StringFormats.parseNameNashville(cleanedName, true);
					}
					
					if (type == 3){
						names = StringFormats.parseNameDesotoRO(cleanedName, true);
					}
					
					if(type == 1) {
						suffixes = GenericFunctions.extractNameSuffixes(names);
						typeName = GenericFunctions.extractAllNamesType(names);
						otherType = GenericFunctions.extractAllNamesOtherType(names);
						GenericFunctions.addOwnerNames(nameTokens[j].trim(), names, suffixes[0], suffixes[1], typeName, otherType, 
								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantor);
					} else if(type == 2) {
						suffixes = GenericFunctions.extractNameSuffixes(names);
						typeName = GenericFunctions.extractAllNamesType(names);
						otherType = GenericFunctions.extractAllNamesOtherType(names);
						GenericFunctions.addOwnerNames(nameTokens[j].trim(), names, suffixes[0], suffixes[1], typeName, otherType, 
								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantee);
						grantee.get(grantee.size()-1).add("");
					} else if(type == 3) {
						suffixes = GenericFunctions.extractNameSuffixes(names);
						typeName = GenericFunctions.extractAllNamesType(names);
						otherType = GenericFunctions.extractAllNamesOtherType(names);
						GenericFunctions.addOwnerNames(nameTokens[j].trim(), names, suffixes[0], suffixes[1], typeName, otherType, 
								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantee);
						grantee.get(grantee.size()-1).add("1");
					}
				}
				
				
				
				
			}
		}
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		

	}
	
	private static String cleanName(String originalName) {
	//stupid fix
		originalName = originalName.replaceAll("FEBRUARY\\s*\\d+\\s*,\\s*\\d+", "");
		originalName = originalName.replaceAll("(?is)\\A\\s*Cash\\s*$", "");
		return originalName;
	}

	public static void parseShortLegal(String rawInfo, ResultMap resultMap) throws Exception {
		String lot = "";
		
		Pattern lotPattern = Pattern.compile("(?i)(?:LOT|LT|LTS)(?:\\s|-)*(\\d+(?:\\s*-\\s*\\d+)?(?:\\s*&\\s*\\d+)?)");
		Matcher lotMatcher = lotPattern.matcher(rawInfo);
		while(lotMatcher.find()) {
			if(!lot.contains(lotMatcher.group(1)))
			lot += lotMatcher.group(1) + " ";
		}
		
		if(StringUtils.isNotEmpty(lot)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		
		
		String block = "";
		Pattern blockPattern = Pattern.compile("(?i)(?:BLOCK)(?:\\s|-)*(\\d+(?:\\s*-\\s*\\d+)?(?:\\s*&\\s*\\d+)?)");
		Matcher blockMatcher = blockPattern.matcher(rawInfo);
		while(blockMatcher.find()) {
			if(!block.contains(blockMatcher.group(1)))
			block += blockMatcher.group(1) + " ";
		}
		if(StringUtils.isNotEmpty(block)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		
		/* Phase */ 
		String phase = "";
		Pattern phasePattern = Pattern.compile("(?i)PH(?:ASE)?\\s*(((?:I|V|X)+)|(?:\\d+(\\-\\w+)?))");
		Matcher phaseMatcher = phasePattern.matcher(rawInfo);
		if(phaseMatcher.find()) {
			phase = phaseMatcher.group(1);
		}
		if(StringUtils.isNotEmpty(phase)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase",phase);
		}
		
		
		/* Tract */
		String tract = "";
		Pattern tractPattern = Pattern.compile("TR(?:ACT)?\\s+((?:\\d|\\w)+(?:\\-\\d+)?)");
		Matcher tractMatcher = tractPattern.matcher(rawInfo);
		if(tractMatcher.find()) {
			tract = tractMatcher.group(1);
			
		}
		if(StringUtils.isNotEmpty(tract)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionTract",tract);
		}
		
		/* Unit */
		String unit = "";
		Matcher unitMatcher = unitShortPattern.matcher(rawInfo);
		if(unitMatcher.find()) {
			unit = unitMatcher.group(1);
		}
		if(StringUtils.isNotEmpty(unit)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit",unit);
		}
				
		
		
		
		resultMap.put("PropertyIdentificationSet.PropertyDescription",rawInfo);
		
	}
	
	public static void parseDetailedLegal(String rawInfo, ResultMap resultMap) throws Exception {
		
		List<String> lots = getLots(rawInfo);
		if( lots!= null && lots.size() > 0) {
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", 
					org.apache.commons.lang.StringUtils.join(lots, " "));
		}
		
		List<String> sections = getSections(rawInfo);
		if( sections!= null && sections.size() > 0) {
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", 
					org.apache.commons.lang.StringUtils.join(sections, " "));
		}
		
		List<String> blocks = getBlocks(rawInfo);
		if( blocks!= null && blocks.size() > 0) {
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", 
					org.apache.commons.lang.StringUtils.join(blocks, " "));
		}
		
		List<String> phases = getPhases(rawInfo);
		if( phases!= null && phases.size() > 0) {
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", 
					org.apache.commons.lang.StringUtils.join(phases, " "));
		}
		
		List<String> tracts = getTracts(rawInfo);
		if( tracts!= null && tracts.size() > 0) {
			resultMap.put("PropertyIdentificationSet.SubdivisionTract", 
					org.apache.commons.lang.StringUtils.join(tracts, " "));
		}
		
		List<String> stws = getSectTownRanges(rawInfo);
		if( stws!= null && stws.size() > 0) {
			String section = (String)resultMap.get("PropertyIdentificationSet.SubdivisionSection");
			if(section == null) {
				section = "";
			} else {
				section += " ";
			}
			String range = "";
			String township = ""; 
			for (String item : stws) {
				String items[] = item.split("-");
				if(items.length == 3) {
					section += items[0] + " ";
					range += items[1] + " ";
					township += items[2] + " ";
				}
			}
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", section);
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", range);
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship", township);
			
		}
		String subdivionName = getSubdivisonName(rawInfo);
		if(StringUtils.isNotEmpty(subdivionName)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionName", subdivionName);
		}
	}
	
	private static String getSubdivisonName(String rawInfo) {
		Matcher matcher = subdivisionNameDetailedPattern.matcher(rawInfo);
		if(matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	private static List<String> getLots(String rawInfo) {
		List<String> lots = new Vector<String>();
		Matcher lotMatcher = lotDetailedPattern.matcher(rawInfo);
		while(lotMatcher.find()) {
			if(lotMatcher.groupCount() == 2) {
				String lotFrom = lotMatcher.group(1);
				String lotTo = lotMatcher.group(2);
				
				
				if(lotFrom.contains("&") || lotTo.contains("&")) {
					String[] lotFromArray = lotFrom.replaceAll(" ", "").split("&");
					String[] lotToArray = lotFrom.replaceAll(" ", "").split("&");
					for (String lotString : lotFromArray) {
						if(!lots.contains(lotString)) {
							lots.add(lotString);
						}
					}
					for (String lotString : lotToArray) {
						if(!lots.contains(lotString)) {
							lots.add(lotString);
						}
					}
				} else {
					try {
						int lotFromInt = Integer.parseInt(lotFrom);
						int lotToInt = Integer.parseInt(lotTo);
						if(lotFromInt == lotToInt) {
							if(!lots.contains(lotFrom)) {
								lots.add(lotFrom);
							} 
						} else {
							if(lotFromInt < lotToInt) {
								for (int i = lotFromInt; i <= lotToInt; i++) {
									if(!lots.contains(String.valueOf(i))) {
										lots.add(String.valueOf(i));
									}
								}
							}
						}
					} catch (NumberFormatException e) {
						if(!lots.contains(lotFrom)) {
							lots.add(lotFrom);
						}
						if(!lots.contains(lotTo)) {
							lots.add(lotTo);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return lots;
	}

	private static List<String> getSections(String rawInfo) {
		List<String> list = new Vector<String>();
		Matcher matcher = sectionDetailedPattern.matcher(rawInfo);
		while(matcher.find()) {
			if(matcher.groupCount() == 1) {
				if(!list.contains(matcher.group(1))) {
					list.add(matcher.group(1));
				} 
			}
		}
		return list;
	}

	private static List<String> getBlocks(String rawInfo) {
		List<String> list = new Vector<String>();
		Matcher matcher = blockDetailedPattern.matcher(rawInfo);
		while(matcher.find()) {
			if(matcher.groupCount() == 1) {
				if(!list.contains(matcher.group(1))) {
					list.add(matcher.group(1));
				} 
			}
		}
		return list;
	}

	private static List<String> getPhases(String rawInfo) {
		List<String> list = new Vector<String>();
		Matcher matcher = phaseDetailedPattern.matcher(rawInfo);
		while(matcher.find()) {
			if(matcher.groupCount() == 1) {
				if(!list.contains(matcher.group(1))) {
					list.add(matcher.group(1));
				} 
			}
		}
		return list;
	}

	private static List<String> getTracts(String rawInfo) {
		List<String> list = new Vector<String>();
		Matcher matcher = tractDetailedPattern.matcher(rawInfo);
		while(matcher.find()) {
			if(matcher.groupCount() == 1) {
				if(!list.contains(matcher.group(1))) {
					list.add(matcher.group(1));
				} 
			}
		}
		return list;
	}

	private static List<String> getSectTownRanges(String rawInfo) {
		List<String> list = new Vector<String>();
		Matcher matcher = sectTownRangesDetailedPattern.matcher(rawInfo);
		while(matcher.find()) {
			if(matcher.groupCount() == 3) {
				String sectTownRanges = matcher.group(1) + 
					"-" + matcher.group(2) + 
					"-" + matcher.group(3);
				if(!list.contains(sectTownRanges)) {
					list.add(sectTownRanges);
				} 
			}
		}
		return list;
	}

	public static void parseCondominium(String rawInfo, ResultMap resultMap) throws Exception {
		List<String> buildings = getBuildings(rawInfo);
		List<String> units = getUnits(rawInfo);
		if( units!= null && units.size() > 0) {
			
			if(buildings.size() == units.size()) {
				String building = buildings.get(0);
				String unit = units.get(0);
				if(!unit.contains(building)) {
					resultMap.put("PropertyIdentificationSet.SubdivisionUnit", 
							(unit + building).replaceAll("\\s", ""));
				} else {
					resultMap.put("PropertyIdentificationSet.SubdivisionUnit", 
							unit.replaceAll("\\s", ""));
				}
			} else if(buildings.size() == 0) {
				resultMap.put("PropertyIdentificationSet.SubdivisionUnit", 
						org.apache.commons.lang.StringUtils.join(units, " "));
				
			}
			
			
		}
	}

	private static List<String> getBuildings(String rawInfo) {
		List<String> list = new Vector<String>();
		Matcher matcher = bldgDetailedPattern.matcher(rawInfo);
		while(matcher.find()) {
			if(matcher.groupCount() == 1) {
				if(!list.contains(matcher.group(1))) {
					list.add(matcher.group(1));
				} 
			}
		}
		return list;
	}

	private static List<String> getUnits(String rawInfo) {
		List<String> list = new Vector<String>();
		Matcher matcher = unitDetailedPattern.matcher(rawInfo);
		while(matcher.find()) {
			if(matcher.groupCount() == 2) {
				String lotFrom = matcher.group(1);
				String lotTo = matcher.group(2);
				if(!list.contains(lotFrom)) {
					list.add(lotFrom);
				}
				if(!list.contains(lotTo)) {
					list.add(lotTo);
				}
				
			}
		}
		return list;
	}
	
	public static String cleanAddress(String rawAddress) {
		rawAddress = rawAddress.replaceFirst("(?is)(.+?)\\bCOND\\.?O?(MINIUM)?.*", "$1");
		//rawAddress = rawAddress.replaceFirst("(?is)\\b(COND\\.?O?(MINIUM)?)\\b", "UNIT");
		rawAddress = rawAddress.replaceFirst("(?is)(.+?)\\bUNIT\\b.*", "$1");
		rawAddress = rawAddress.replaceFirst("(?is)(.+?)\\bAPARTMENT\\b.*", "$1");
		//rawAddress = rawAddress.replaceFirst("(?is)\\bAPARTMENT\\b", "");
		rawAddress = rawAddress.replaceFirst("(?is)(.+?)\\bBUILDING\\b.*", "$1");
		rawAddress = rawAddress.replaceFirst("(?is)(.+?)\\bNUMBER\\b.*", "$1");
		//rawAddress = rawAddress.replaceFirst("(?is)\\bBUILDING\\b", "");
		rawAddress = rawAddress.replaceFirst("(?is)(.+?)\\b[a-z]-?\\d+\\b.*", "$1");
		rawAddress = rawAddress.replaceFirst("(?is)(.+?)\\b\\d+-?[a-z]\\b.*", "$1");
		return rawAddress;
	}
	
}
