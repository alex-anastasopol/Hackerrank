package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class GenericDTSFunctionsRO {
	
		
	@SuppressWarnings({ "rawtypes" })
	public static void parseNamesRO(ResultMap m, long searchId) throws Exception{
		
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes = {"", ""}, type = {"", ""}, otherType = {"", ""};
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String tmpPartyGtor = (String)m.get("tmpPartyGtor");
			if (StringUtils.isNotEmpty(tmpPartyGtor)){
				tmpPartyGtor = prepareName(tmpPartyGtor);
				
				String[] gtors = tmpPartyGtor.split(" / ");
				for (String grantorName : gtors){					
					names = StringFormats.parseNameNashville(grantorName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					if (NameUtils.isNotCompany(names[2])){
						suffixes = GenericFunctions.extractNameSuffixes(names);
					}
					
					GenericFunctions.addOwnerNames(grantorName, names, suffixes[0],
													suffixes[1], type, otherType,
													NameUtils.isCompany(names[2]),
													NameUtils.isCompany(names[5]), grantor);
				}
				
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			}
			
			String tmpPartyGtee = (String)m.get("tmpPartyGtee");
			if (StringUtils.isNotEmpty(tmpPartyGtee)){
				tmpPartyGtee = prepareName(tmpPartyGtee);
				
				String[] gtee = tmpPartyGtee.split(" / ");
				for (String granteeName : gtee){
		
					names = StringFormats.parseNameNashville(granteeName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					if (NameUtils.isNotCompany(names[2])){
						suffixes = GenericFunctions.extractNameSuffixes(names);
					}
					
					GenericFunctions.addOwnerNames(granteeName, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
				
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
				
			}
			
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		
	}
	
	private static String prepareName(String name){
		
		name = name.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ");
		name = name.replaceAll("\\s*-\\s*(\\(?DECEASED\\)?|EXECUTOR|HEIR|NOMINEE|SUCCESSOR BY MERGER|SUCCESSOR IN INTEREST|THIRD PARTY)\\b", "");
		
		name = name.replaceAll("\\s+&\\s+[HUSB|HUSBAND|WIFE]\\b", "");
		name = name.replaceAll("\\s+&\\s+[A-Z]F?\\b", "");
		name = name.replaceAll("\\s+&\\s+(ETAL|ETUX|ETVIR)\\b", " $1");
		name = name.replaceAll("\\s+-\\s*(?:CO-\\s*)?(TRUSTEES?)", " $1");
		name = name.replaceAll("(?is)\\s+\\bONG\\b\\s*", " ");
		name = name.replaceAll("(\\w+)-\\s*\\d+.*", "$1");//for UCC docs ex: U87000978
		name = name.replaceAll("[\\)\\(]+", "");
		name = name.replaceFirst("^\\s*/\\s*", "");
		name = name.replaceFirst("\\s*/\\s*$", "");
		name = name.replaceAll("(?is)\\s*-\\s*\\b(JR|SR|III|II|I)\\b", " $1");
		name = name.replaceAll("(?is)&nbsp;", " ");
		
		return name;
	}
	
	@SuppressWarnings("unchecked")
	public static void updatePIS(ResultMap m, long searchId){
		boolean needNewPIS = false;
		Vector pisVector = (Vector) m.get("PropertyIdentificationSet");
		PropertyIdentificationSet tmpPis = (PropertyIdentificationSet) m.get("tmpPis");
		
		if (pisVector != null) {
			PropertyIdentificationSet pis = (PropertyIdentificationSet) pisVector.get(0);
			String subdiv = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName());
			String pb = tmpPis.getAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName());
			String pg = tmpPis.getAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName());
			String lot = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName());
			String block = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName());
			String unit = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName());
			String phase = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName());
			String tract = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName());
			String bldg = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName());
			String pid = tmpPis.getAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName());
			String sec = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName());
			String twp = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName());
			String rng = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName());
			
			String pisSubdName = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName());
			String pisPid = pis.getAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName());
			String pisPB = pis.getAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName());
			String pisPNO = pis.getAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName());
			String pisBlock = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName());
			String pisLot = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName());
			
			if (StringUtils.isNotEmpty(pisSubdName) && StringUtils.isNotEmpty(subdiv)) {
				BigDecimal score = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_REGISTER_NAME_NA, pisSubdName, subdiv, searchId)).getScore();
		        if (score.compareTo(new BigDecimal(0.6)) >= 0){
		        	needNewPIS = false;
		        } else {
		        	needNewPIS = true;
		        }
			} else if (StringUtils.isNotEmpty(pisPB) && StringUtils.isNotEmpty(pisPNO)){	
				if (pisPB.equals(pb.trim()) && pisPNO.equals(pg.trim())){
					needNewPIS = false;
				} else {
		        	needNewPIS = true;
		        }
			} else if (StringUtils.isNotEmpty(pisBlock)){
				if (pisBlock.equals(block.trim())){
					needNewPIS = false;
				} else {
		        	needNewPIS = true;
		        }
			} else if (StringUtils.isNotEmpty(pisPid)){
				if (pisPid.equals(pid.trim())){
					needNewPIS = false;
				} else {
		        	needNewPIS = true;
		        }
			}

			if (!needNewPIS){
				lot = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName()) + " " + lot;
				lot = LegalDescription.cleanValues(lot, false, true);
				if (StringUtils.isNotEmpty(lot)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
				}
				
				block = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName()) + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
				if (StringUtils.isNotEmpty(block)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
				}
				
				unit = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName()) + " " + unit;
				unit = LegalDescription.cleanValues(unit, false, true);
				if (StringUtils.isNotEmpty(unit)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
				}
				
				phase = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName()) + " " + phase;
				phase = LegalDescription.cleanValues(phase, false, true);
				if (StringUtils.isNotEmpty(phase)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
				}
				
				tract = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName()) + " " + tract;
				tract = LegalDescription.cleanValues(tract, false, true);
				if (StringUtils.isNotEmpty(tract)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
				}
				
				bldg = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName()) + " " + bldg;
				bldg = LegalDescription.cleanValues(bldg, false, true);
				if (StringUtils.isNotEmpty(bldg)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
				}
				
				if (StringUtils.isNotEmpty(pid)) {
					pis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
				}
				if (StringUtils.isNotEmpty(sec)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
				}
				if (StringUtils.isNotEmpty(twp)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
				}
				if (StringUtils.isNotEmpty(rng)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
				}
				if (StringUtils.isEmpty(pisPB)){
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
				}
				if (StringUtils.isEmpty(pisPNO)){
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg);
				}
			} else {
				PropertyIdentificationSet newPis = new PropertyIdentificationSet();
				if (StringUtils.isNotEmpty(lot)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
				}
				if (StringUtils.isNotEmpty(block)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
				}
				if (StringUtils.isNotEmpty(unit)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
				}
				if (StringUtils.isNotEmpty(phase)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
				}
				if (StringUtils.isNotEmpty(tract)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
				}
				if (StringUtils.isNotEmpty(bldg)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
				}
				if (StringUtils.isNotEmpty(pb)) {
					newPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
				}
				if (StringUtils.isNotEmpty(pg)) {
					newPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
				}
				if (StringUtils.isNotEmpty(subdiv)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
					if (subdiv.matches("(?is).*\\b(CO?NDO(MINIUM)?)\\b.*")) {
						newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_COND.getShortKeyName(), subdiv.trim());
					}
				}
				if (StringUtils.isNotEmpty(pid)) {
					newPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
				}
				if (StringUtils.isNotEmpty(sec)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
				}
				if (StringUtils.isNotEmpty(twp)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
				}
				if (StringUtils.isNotEmpty(rng)) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
				}
				
				pisVector.add(newPis);
				m.put("PropertyIdentificationSet", pisVector);
			}
        } else {
        	if (tmpPis != null){
        		pisVector = new Vector<PropertyIdentificationSet>();
        		pisVector.add(tmpPis);
				m.put("PropertyIdentificationSet", pisVector);
        	}
        }
	}

	
	public static void parseMultipleLegal(ResultMap resultMap, long searchId) throws Exception {
		String legalDesc = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legalDesc))
			return;
				
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legalDesc = Roman.normalizeRomanNumbersExceptTokens(legalDesc, exceptionTokens); // convert roman numbers
		
		String[] ldInfoVector = legalDesc.split("\\s*/\\s*");
		for (int i=0; i<ldInfoVector.length; i++) {
			String ld = ldInfoVector[i].trim();
			PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
			ld = ld.replaceAll("(?is)\\b(UNIT)\\s+NO\\b", "$1 ");
			
			String unitPatt1 = "(?is)\\b(UT?)\\s+([\\d-]+[A-Z]?|[A-Z])\\b";
			String unitPatt2 = "(?is)\\b(UT?)\\s*([\\d-]+)\\b";
			
			if (StringUtils.isNotEmpty(ld)) {
				String legalTemp = ld;
				String subdivName = "", lot = "", blk = "";

				Pattern p = Pattern.compile("(?is)\\b(AddrNo:)\\s+([^,]+)\\b");
				Matcher ma = p.matcher(ld);
				String addressUnit = "";
				if (ma.find()) {
					String addressNo = ma.group(2).trim();
					if (addressNo.matches("(?is)[^-]+-([^$])")){
						addressUnit = addressNo.replaceAll("(?is)[^-]+-([^$])", "$1");
						addressNo = addressNo.replaceAll("(?is)([^-]+)-[^$]", "$1");
					}
					tmpPis.setAtribute(PropertyIdentificationSetKey.STREET_NO.getShortKeyName(), addressNo);
				}
				
				p = Pattern.compile("(?is)\\b(Str1:)\\s+([^,]+)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					String addressName = ma.group(2).trim();
					Matcher ma2 = Pattern.compile("(?is)\\b(Str2:)\\s+([^,]+)\\b").matcher(ld);
					if (ma2.find()) {
						String str2 = ma2.group(2).trim();
						if (str2.matches("(?is)UNIT\\s+\\d+") || str2.matches("#\\s*\\d+")) {
							addressName += " " + str2; 
						}
					}
					addressName = addressName.replaceAll("(?is)\\s*-\\s*([A-Z])\\s*$", " $1");
					addressName = addressName.replaceAll("/(\\d+)\\s*$", " #$1");
					addressName = addressName.replaceAll("(?is)\\bUNIT\\s+(\\d+)\\s*$", " #$1").replaceAll("\\s{2,}", " ");
					tmpPis.setAtribute(PropertyIdentificationSetKey.STREET_NAME.getShortKeyName(), (addressName + " " + addressUnit).trim());
				}
				
				p = Pattern.compile("(?is)\\b(City:)\\s+([^,]+)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					tmpPis.setAtribute(PropertyIdentificationSetKey.CITY.getShortKeyName(), ma.group(2).trim());
				}
				
				p = Pattern.compile("(?is)\\b(Zip:)\\s+([\\d-]+)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					tmpPis.setAtribute(PropertyIdentificationSetKey.ZIP.getShortKeyName(), ma.group(2).trim());
				}
				
				p = Pattern.compile("(?is)\\b(PrpId)\\b\\s*:?\\s*([\\d-\\.]+)");
				ma = p.matcher(ld);
				if (ma.find()) {
					tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), ma.group(2).trim());
				}
					
				p = Pattern.compile("(?is)\\b((?:Lot/Unit|Lt):)\\s+([^,]+)\\b");
				ma = p.matcher(ld);
				while (ma.find()) {
					lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+$", "");
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				}
				if (ld.contains("CONDO")){ 
					if (StringUtils.isNotEmpty(lot.trim())){
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), lot.trim());
						lot = "";
					} else {
						p = Pattern.compile("(?is)\\bExtDesc:\\s*UNIT\\s*(\\d+)\\b");
						ma = p.matcher(ld);
						if (ma.find()){
							tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), ma.group(1));
						}
					}
				}
				if (ld.contains("ExtDesc")){
					ld = ld.replaceAll("(?is)\\b(LOT\\s*\\d+)\\s*&\\s*PRT\\s*(\\d+)", "$1 & LOT $2");
					legalTemp = ld;
					p = Pattern.compile("(?is)\\b(?:PRT\\s+)?(LO?T?S?)\\s+([\\d&\\s-]+)\\b");
					ma = p.matcher(ld);
					while (ma.find()) {
						lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+$", "");
						legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
					}
					ld = legalTemp;
				}
					
				if (lot.trim().length() != 0) {
					lot = lot.replaceAll("\\s*[& *]\\s*", " ");
					lot = LegalDescription.cleanValues(lot, false, true);
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot.trim());
				}
					
				p = Pattern.compile("(?is)\\b(Bl(?:d?g)?:)\\s+([^,]+)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					blk = ma.group(2).trim().replaceAll("\\A0+", "");
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					if (ld.contains("CONDO")) {
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), blk.trim());
						blk = "";
					}
				} else if (ld.contains("ExtDesc")){
					p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d-]+)\\b");
					ma = p.matcher(ld);
					if (ma.find()) {
						blk = blk + " " + ma.group(2).trim().replaceAll("\\A0+", "");
						legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
					}
					ld = legalTemp;
				}
				if (blk.trim().length() != 0) {
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), blk);
				}
					
				p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+(\\d+[A-Z]?)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), ma.group(2).trim());
					ld = ld.replaceFirst(ma.group(0), " ");
				}
					
				p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+(\\d+[A-Z]?|[A-Z][\\d-]?)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), ma.group(2).trim());
					ld = ld.replaceFirst(ma.group(0), " ");
				}
				
				p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+)\\s+([A-Z]+)");
				ma = p.matcher(ld);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), ma.group(2).trim());
					if (ma.group(3) != null){
						tmpPis.setAtribute(PropertyIdentificationSetKey.CITY.getShortKeyName(), ma.group(3).trim());
					}
					ld = ld.replaceFirst(ma.group(0), " ");
				}
				
				p = Pattern.compile(unitPatt1);
				ma = p.matcher(ld);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), ma.group(2).trim());
				} else {
					ma.reset();
					p = Pattern.compile(unitPatt2);
					ma = p.matcher(ld);
					if (ma.find()) {
						legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), ma.group(2).trim());
					}
				}
				
				p = Pattern.compile("(?is)\\b(TwnNotes:)\\s+([^,]+)\\b");
				ma = p.matcher(ld);
				String str = "";
				if (ma.find()) {
					str = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(TwnNotes:)\\s+([^$]+)\\b");
					ma = p.matcher(ld);
					if (ma.find()) {
						str = ma.group(2);
					}
				}
				
				p = Pattern.compile("(?is)\\bSec:\\s*(\\d+)\\s*,\\s*Twn:\\s*(\\d+)\\s*,\\s*Rng:\\s*(\\d+)(?:\\s*,\\s*Q:\\s*([NSWE]{1,2}))?");
				ma = p.matcher(ld);
				if (ma.find()) {
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), ma.group(1).trim());
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ma.group(2).trim());
					tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), ma.group(3).trim());
					if (ma.group(4) != null){
						tmpPis.setAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName(), ma.group(4).trim());
					}
				}
				
				p = Pattern.compile("(?is)\\bB\\s*(\\d+)\\s*P\\s*(\\d+)\\b");
				ma = p.matcher(str);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), ma.group(1).trim());
					tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), ma.group(2).trim());
				}
				
				/*p = Pattern.compile("(?is)\\b(SubCmt:)\\s*#\\s+([\\d-]+)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_INSTR.getShortKeyName(), ma.group(2).trim());
				}*/
				
				p = Pattern.compile("(?is)\\b(Sub:)\\s+([^,]+)\\b");
				ma = p.matcher(ld);
				if (ma.find()) {
					subdivName = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(Sub:)\\s+([^$]+)");
					ma = p.matcher(ld);
					if (ma.find()) {
						subdivName = ma.group(2);
					} else {
						p = Pattern.compile("(?is)\\b(ExtDesc:)\\s+([^,]+)");
						ma = p.matcher(ld);
						if (ma.find()) {
							subdivName = ma.group(2);
						} else {
							p = Pattern.compile("(?is)\\b(ExtDesc:)\\s+([^$]+)");
							ma = p.matcher(ld);
							if (ma.find()) {
								subdivName = ma.group(2);
							}
						}
					}
				}
				if (subdivName.matches("(?is).*\\bINVALID\\s+PIN\\b.*")) {
					subdivName = "";
				}
				if (!"".equals(subdivName.trim())){
					subdivName = subdivName.replaceAll("--", "-");
					p = Pattern.compile("(?is)\\b([\\d\\s&]+[A-Z]?)\\s*[-|T]\\s*(\\d+[A-Z]?)\\s*[-|\\s|R]\\s*(\\d+[A-Z]?)\\b");
					ma.reset();
					ma = p.matcher(subdivName);
					if (ma.find()) {
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), ma.group(1).replaceAll("\\s+&\\s+", " ").trim());
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ma.group(2).trim());
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), ma.group(3).trim());
						subdivName = subdivName.replaceFirst(ma.group(0), "");
					} else {
						p = Pattern.compile("(?is)\\bSEC\\s+([\\d-]+)\\s+TP\\s+([\\d-]+[A-Z]?)\\b");
						ma.reset();
						ma = p.matcher(subdivName);
						if (ma.find()) {
							tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), ma.group(1).replaceAll("\\s*-\\s*", " ").trim());
							tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ma.group(2).replaceAll("\\s*-\\s*", " ").trim());
							subdivName = subdivName.replaceFirst(ma.group(0), "");
						} else {
							p = Pattern.compile("(?is)\\bR(?:n?g)?\\s*([\\d-]+)\\s*T(?:wn?)?\\s*([\\d-]+)\\s*(?:S(?:e?c)?\\s*([\\d-]+))\\s*Q(?:r?t)?([\\d-]+)\\s*(?:Qv(?:al)?([NSWE]{1,2}))?");
							ma.reset();
							ma = p.matcher(subdivName);
							if (ma.find()) {
								tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), ma.group(1).trim());
								tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ma.group(2).replaceAll("\\s*-\\s*", " ").trim());
								tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), ma.group(3).replaceAll("\\s*-\\s*", " ").trim());
								tmpPis.setAtribute(PropertyIdentificationSetKey.QUARTER_ORDER.getShortKeyName(), ma.group(4).trim());
								if (ma.group(5) != null){
									tmpPis.setAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName(), ma.group(5).trim());
								}
								subdivName = subdivName.replaceFirst(ma.group(0), "");
							} else {
								p = Pattern.compile("(?is)\\bR(?:n?g)?\\s*([\\d-]+)\\s*T(?:wn?)?\\s*([\\d-]+)\\s*Q(?:r?t)?([\\d-]+)\\s*(?:Qv(?:al)?([NSWE]{1,2}))?");
								ma.reset();
								ma = p.matcher(subdivName);
								if (ma.find()) {
									tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), ma.group(1).trim());
									tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ma.group(2).replaceAll("\\s*-\\s*", " ").trim());
									tmpPis.setAtribute(PropertyIdentificationSetKey.QUARTER_ORDER.getShortKeyName(), ma.group(3).trim());
									if (ma.group(4) != null){
										tmpPis.setAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName(), ma.group(4).trim());
									}
									subdivName = subdivName.replaceFirst(ma.group(0), "");
								}
							}
						}
					}
					subdivName = subdivName.replaceAll("(?is)\\bGARAGE\\s+U(\\d+|[A-Z])\\b", " UNIT $1");
					p = Pattern.compile("(?is)\\b(UNIT)\\s+#?\\s*(\\d+[A-Z]?|[A-Z])\\b");
					ma = p.matcher(subdivName);
					if (ma.find()) {
						String unit = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName());
						if (StringUtils.isEmpty(unit)) {
							unit = "";
						}
						unit += " " + ma.group(2).trim();
						unit = LegalDescription.cleanValues(unit, false, true);
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
						subdivName = subdivName.replaceAll(ma.group(0), "UNIT");
						subdivName = subdivName.replaceAll(unitPatt1, "UNIT");
						subdivName = subdivName.replaceAll(unitPatt2, "UNIT");
						subdivName = subdivName.replaceAll("(?is)(\\bGARAGE\\b\\s+)?\\bUNIT\\b", "");
					}
					subdivName = subdivName.replaceAll("(?is)\\bREVIEW LEGAL DESCRIPTION\\b", "");
					subdivName = subdivName.replaceFirst("(?is)(.*)\\s+(\\d+)\\s*(?:ST|ND|RD|TH)\\b\\s+(?:DEV)\\b\\s+[A-Z\\s]+(?:TWP|TOWNSHIP)", "$1");
					subdivName = subdivName.replaceAll("(?is)\\bCERT(IFICATE)?\\s+OF\\s+SUR.*", "$1");
					subdivName = subdivName.replaceAll("(?is)\\bBLOCK.*", "");
					subdivName = subdivName.replaceAll("(?is)\\bTRACT\\b", "");
					subdivName = subdivName.replaceAll("(?is)\\s+(?:&\\s+RE)?SUB\\b.*", "");
					subdivName = subdivName.replaceAll("(?is)\\b(?:PRT|ALL)\\s+OF\\b", "");
					subdivName = subdivName.replaceAll("(?is)\\bPRT\\b.*", "");
					subdivName = subdivName.replaceAll("(?is)\\bSECS?\\b", "");
					subdivName = subdivName.replaceAll("(?is)\\s+", " ");
					subdivName = subdivName.replaceAll("(?is)\\s+[\\*&]\\s*$", "");
					subdivName = subdivName.replaceAll("(?is)\\A\\s*[\\*&]\\s*", "");
					subdivName = subdivName.trim();
					if (subdivName.toUpperCase().contains("EPIN")) {
						subdivName = subdivName.replaceAll("(?is)\\s*,?\\s*\\bEPIN\\s*:\\s*\\d+\\s*", "");
					}
					if (!"".equals(subdivName)){
						tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdivName);
						if (subdivName.matches("(?is).*\\bCOND.*")) {
							tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_COND.getShortKeyName(), subdivName);
						}
					}
				}
			}
			
			resultMap.put("tmpPis", tmpPis);
			updatePIS(resultMap, searchId);
		}
	}
	
	
	
	public static void parseLegal(ResultMap resultMap, long searchId) throws Exception{
		
		String unitPatt1 = "(?is)\\b(UT?)\\s+([\\d-]+[A-Z]?|[A-Z])\\b";
		String unitPatt2 = "(?is)\\b(UT?)\\s*([\\d-]+)\\b";
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isNotEmpty(legal)){
					
			legal = GenericFunctions.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

			legal = legal.replaceAll("(?is)\\b(UNIT)\\s+NO\\b", "$1 ");
			String legalTemp = legal;
			String subdivName = "", lot = "", blk = "";//, bg = "", tract = "";

			Pattern p = Pattern.compile("(?is)\\b(AddrNo:)\\s+([^,]+)\\b");
			Matcher ma = p.matcher(legal);
			String addressUnit = "";
			if (ma.find()) {
				String addressNo = ma.group(2).trim();
				if (addressNo.matches("(?is)[^-]+-([^$])")){
					addressUnit = addressNo.replaceAll("(?is)[^-]+-([^$])", "$1");
					addressNo = addressNo.replaceAll("(?is)([^-]+)-[^$]", "$1");
				}
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), addressNo);
			}
			
			p = Pattern.compile("(?is)\\b(Str1:)\\s+([^,]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				String addressName = ma.group(2).trim();
				Matcher ma2 = Pattern.compile("(?is)\\b(Str2:)\\s+([^,]+)\\b").matcher(legal);
				if (ma2.find()) {
					String str2 = ma2.group(2).trim();
					if (str2.matches("(?is)UNIT\\s+\\d+") || str2.matches("#\\s*\\d+")) {
						addressName += " " + str2; 
					}
				}
				addressName = addressName.replaceAll("(?is)\\s*-\\s*([A-Z])\\s*$", " $1");
				addressName = addressName.replaceAll("/(\\d+)\\s*$", " #$1");
				addressName = addressName.replaceAll("(?is)\\bUNIT\\s+(\\d+)\\s*$", " #$1").replaceAll("\\s{2,}", " ");
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), (addressName + " " + addressUnit).trim());
			}
			
			p = Pattern.compile("(?is)\\b(City:)\\s+([^,]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), ma.group(2).trim());
			}
			
			p = Pattern.compile("(?is)\\b(Zip:)\\s+([\\d-]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), ma.group(2).trim());
			}
			
//			p = Pattern.compile("(?is)\\b(PrpId:)\\s+([\\d-]+)\\b");
//			p = Pattern.compile("(?is)\\b(PrpId|EPIN)\\b:?\\s+([\\d-\\.A-Z]+)");
			p = Pattern.compile("(?is)\\b(PrpId)\\b\\s*:?\\s*([\\d-\\.]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), ma.group(2).trim());
			}
				
			p = Pattern.compile("(?is)\\b(Lot/Unit:)\\s+([^,]+)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+$", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			}
			if (legal.contains("CONDO")){//automatic by 0506014090170000 
				if (StringUtils.isNotEmpty(lot.trim())){
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), lot.trim());
					lot = "";
				} else {
					p = Pattern.compile("(?is)\\bExtDesc:\\s*UNIT\\s*(\\d+)\\b");//R2002124911
					ma = p.matcher(legal);
					if (ma.find()){
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(1));
					}
				}
			}
			if (legal.contains("ExtDesc")){
				legal = legal.replaceAll("(?is)\\b(LOT\\s*\\d+)\\s*&\\s*PRT\\s*(\\d+)", "$1 & LOT $2");//R71026614
				legalTemp = legal;
				p = Pattern.compile("(?is)\\b(?:PRT\\s+)?(LO?T?S?)\\s+([\\d&\\s-]+)\\b");
				ma = p.matcher(legal);
				while (ma.find()) {
					lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+$", "");
					legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				}
				legal = legalTemp;
			}
				
			if (lot.trim().length() != 0) {
				lot = lot.replaceAll("\\s*[\\*&]\\s*", " ");
				lot = LegalDescription.cleanValues(lot, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			}
				
			p = Pattern.compile("(?is)\\b(Bl:)\\s+([^,]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				blk = ma.group(2).trim().replaceAll("\\A0+", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				if (legal.contains("CONDO")){//automatic by 0506014090170000 
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), blk.trim());
					blk = "";
				}
			} else if (legal.contains("ExtDesc")){
				p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s+([\\d-]+)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					blk = blk + " " + ma.group(2).trim().replaceAll("\\A0+", "");
					legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				}
				legal = legalTemp;
			}
			if (blk.trim().length() != 0) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
			}
				
			p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+(\\d+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2).trim());
				legal = legal.replaceFirst(ma.group(0), " ");
			}
				
			p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+(\\d+[A-Z]?|[A-Z][\\d-]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(2).trim());
				legal = legal.replaceFirst(ma.group(0), " ");
			}
				
			p = Pattern.compile(unitPatt1);
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(2).trim());
			} else {
				ma.reset();
				p = Pattern.compile(unitPatt2);
				ma = p.matcher(legal);
				if (ma.find()) {
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(2).trim());
				}
			}
			
			p = Pattern.compile("(?is)\\b(TwnNotes:)\\s+([^,]+)\\b");
			ma = p.matcher(legal);
			String str = "";
			if (ma.find()) {
				str = ma.group(2);
			} else {
				p = Pattern.compile("(?is)\\b(TwnNotes:)\\s+([^$]+)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					str = ma.group(2);
				}
			}
			/*p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\b");
			ma = p.matcher(str);
			if (ma.find()) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),ma.group(1).trim());
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).trim());
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3).trim());
			} else*/ {
				p = Pattern.compile("(?is)\\bSec:\\s*(\\d+)\\s*,\\s*Twn:\\s*(\\d+)\\s*,\\s*Rng:\\s*(\\d+)(?:\\s*,\\s*Q:\\s*([NSWE]{1,2}))?");
				ma = p.matcher(legal);
				if (ma.find()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(1).trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3).trim());
					if (ma.group(4) != null){
						resultMap.put(PropertyIdentificationSetKey.QUARTER_VALUE.getKeyName(), ma.group(4).trim());
					}
				}
			}
			
			p = Pattern.compile("(?is)\\bB\\s*(\\d+)\\s*P\\s*(\\d+)\\b");
			ma = p.matcher(str);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1).trim());
				resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2).trim());
			}
			
			/*p = Pattern.compile("(?is)\\b(SubCmt:)\\s*#\\s+([\\d-]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				resultMap.put(PropertyIdentificationSetKey.PLAT_INSTR.getKeyName(), ma.group(2).trim());
			}*/
			
			p = Pattern.compile("(?is)\\b(Sub:)\\s+([^,]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdivName = ma.group(2);
			} else {
				p = Pattern.compile("(?is)\\b(Sub:)\\s+([^$]+)");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdivName = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(ExtDesc:)\\s+([^,]+)");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdivName = ma.group(2);
					} else {
						p = Pattern.compile("(?is)\\b(ExtDesc:)\\s+([^$]+)");
						ma = p.matcher(legal);
						if (ma.find()) {
							subdivName = ma.group(2);
						}
					}
				}
			}
			if (subdivName.matches("(?is).*\\bINVALID\\s+PIN\\b.*")) {
				subdivName = "";
			}
			if (!"".equals(subdivName.trim())){
				subdivName = subdivName.replaceAll("--", "-");
				p = Pattern.compile("(?is)\\b([\\d\\s&]+[A-Z]?)\\s*[-|T]\\s*(\\d+[A-Z]?)\\s*[-|\\s|R]\\s*(\\d+[A-Z]?)\\b");
				ma.reset();
				ma = p.matcher(subdivName);
				if (ma.find()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(1).replaceAll("\\s+&\\s+", " ").trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3).trim());
					subdivName = subdivName.replaceFirst(ma.group(0), "");
				} else {
					p = Pattern.compile("(?is)\\bSEC\\s+([\\d-]+)\\s+TP\\s+([\\d-]+[A-Z]?)\\b");
					ma.reset();
					ma = p.matcher(subdivName);
					if (ma.find()) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(1).replaceAll("\\s*-\\s*", " ").trim());
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).replaceAll("\\s*-\\s*", " ").trim());
						subdivName = subdivName.replaceFirst(ma.group(0), "");
					}
				}
				subdivName = subdivName.replaceAll("(?is)\\bGARAGE\\s+U(\\d+|[A-Z])\\b", " UNIT $1");
				p = Pattern.compile("(?is)\\b(UNIT)\\s+#?\\s*(\\d+[A-Z]?|[A-Z])\\b");
				ma = p.matcher(subdivName);
				if (ma.find()) {
					String unit = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
					if (StringUtils.isEmpty(unit)) {
						unit = "";
					}
					unit += " " + ma.group(2).trim();
					unit = LegalDescription.cleanValues(unit, false, true);
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
					subdivName = subdivName.replaceAll(ma.group(0), "UNIT");
					subdivName = subdivName.replaceAll(unitPatt1, "UNIT");
					subdivName = subdivName.replaceAll(unitPatt2, "UNIT");
					subdivName = subdivName.replaceAll("(?is)(\\bGARAGE\\b\\s+)?\\bUNIT\\b", "");
				}
				subdivName = subdivName.replaceAll("(?is)\\bREVIEW LEGAL DESCRIPTION\\b", "");
				subdivName = subdivName.replaceAll("(?is)\\bCERT(IFICATE)?\\s+OF\\s+SUR.*", "$1");
				subdivName = subdivName.replaceAll("(?is)\\bBLOCK.*", "");
				subdivName = subdivName.replaceAll("(?is)\\bTRACT\\b", "");
				subdivName = subdivName.replaceAll("(?is)\\s+(?:&\\s+RE)?SUB\\b.*", "");
				subdivName = subdivName.replaceAll("(?is)\\b(?:PRT|ALL)\\s+OF\\b", "");
				subdivName = subdivName.replaceAll("(?is)\\bPRT\\b.*", "");
				subdivName = subdivName.replaceAll("(?is)\\bSECS?\\b", "");
				subdivName = subdivName.replaceAll("(?is)\\s+", " ");
				subdivName = subdivName.replaceAll("(?is)\\s+&\\s*$", "");
				subdivName = subdivName.replaceAll("(?is)\\A\\s*&\\s*", "");
				subdivName = subdivName.trim();
				if (!"".equals(subdivName)){
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName);
					if (subdivName.matches("(?is).*\\bCOND.*")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivName);
					}
				}
			}

		}

	}
	
	public static boolean containsPIS(Vector<PropertyIdentificationSet> pisVector, PropertyIdentificationSet pis, boolean separatelySTR){
		if (pisVector == null){
			pisVector = new Vector<PropertyIdentificationSet>();
		}
		if (pis == null || pis.isEmpty()){
			return false;
		}
		
		for (PropertyIdentificationSet everyPis : pisVector){
			if (separatelySTR){
				if (StringUtils.isNotEmpty(everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName())) 
						&& StringUtils.isNotEmpty(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName()))){
					if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName())
							.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName()))){
						if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName())
								.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName()))){
							if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName())
									.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName()))){
								if (StringUtils.isNotEmpty(everyPis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName())) 
										&& StringUtils.isNotEmpty(pis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName()))){
									if (everyPis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName())
											.equals(pis.getAtribute(PropertyIdentificationSetKey.QUARTER_VALUE.getShortKeyName()))){
											return true;
									} else {
										return false;
									}
								} else {
									return true;
								}
							} else {
								return false;
							}
						} else {
							return false;
						}
					} else {
						return false;
					}
				}
			} else {
				if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName())
						.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName()))){
					if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName())
								.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName()))){
						if (everyPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName())
									.equals(pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName()))){
							return true;
						} else {
							return false;
						}
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}
    
}
