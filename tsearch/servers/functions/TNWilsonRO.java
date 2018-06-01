package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class TNWilsonRO {
	
	@SuppressWarnings("rawtypes")
	public static void parseName(ResultMap m, long searchId) throws Exception {
		
		String grantor = (String) m.get(SaleDataSetKey.GRANTOR.getKeyName());
		String grantee = (String) m.get(SaleDataSetKey.GRANTEE.getKeyName());
	
		if(StringUtils.isEmpty(grantee) && StringUtils.isNotEmpty((String) m.get(SaleDataSetKey.GRANTEE_LANDER.getKeyName()))) {
			grantee = (String) m.get(SaleDataSetKey.GRANTEE_LANDER.getKeyName());
		}
		grantor = StringUtils.prepareStringForHTML(grantor);
		grantee = StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);
		
		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();
		
		parseNameInner(m, grantor, grantorList, searchId, false);
		parseNameInner(m, grantee, granteeList, searchId, true);
				
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		
		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNameInner(ResultMap m, String name, ArrayList<List> namesList, long searchId, boolean isGrantee) {
		
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;
				
		if (NameUtils.isNotCompany(name)){
			if (name.matches("(?is)(\\w+)\\s+(\\w+\\s+\\w)\\s+\\1\\s+(\\w+\\s+\\w+)")){
				name = name.replaceAll("(?is)(\\w+)\\s+(\\w+\\s+\\w)\\s+(\\1\\s+\\w+\\s+\\w+)", "$1 $2 & $3");
			}
		}

		name = NameCleaner.cleanNameAndFix(name, new Vector<String>(), true);
		name = name.replaceAll("(?is)\\b[FAN]\\s*/\\s*K\\s*/\\s*A\\b", "\n");
		name = name.replaceAll("(?is)\\bADM\\b", "");//means ADMINISTRATOR
		name = name.replaceAll("(?is)\\bSUC\\s+(TRUSTEE)\\b", " $1");//means ADMINISTRATOR
		
		String[] nameItems = name.split("\n");
		for (int i = 0; i < nameItems.length; i++){
			names = StringFormats.parseNameNashville(nameItems[i], true);
			
			NameCleaner.removeUnderscore(names);
			
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractNameType(names);
			otherType = GenericFunctions.extractNameOtherType(names);
			GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
				
		}
	}
	
	public static void parseLegal(ResultMap m, long searchId) throws Exception {
		   String legal = (String)m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		   
		   ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
//		   System.out.println("PropertyDesc: " + legal);
		   
		   if (org.apache.commons.lang.StringUtils.isEmpty(legal)){
			   legal = (String) m.get(SaleDataSetKey.GRANTOR.getKeyName());
		   }
		   String phaseText = null;
		   String sectionText = null;
		   String lotText = null;
		   String tractText = null;
		   
		   Pattern p = Pattern.compile("\\bL(?:OT)?-?\\s*(\\d+\\w*)");
		   Matcher match = p.matcher(legal);
		   if (match.find()){
			   lotText = match.group(0);
			   String lot = match.group(1);
			   if (!StringUtils.isEmpty(lot)){
				   // check first if the lot is not already in PIS
				   if (pis != null && pis.getBody() != null){
					   String[][] body = pis.getBody();
					   int len = body.length;
					   boolean foundLot = false;
					   for (int i = 0; i < len && !foundLot; i++){
						   foundLot = lot.equals(body[i][1]);
					   }
					   if (!foundLot){
						   m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
					   }
				   }			   
			   }
		   }
		   
		   p = Pattern.compile("\\bPH(?:ASE)?\\b-?\\s*(-?\\d+|\\w+)");
		   match.usePattern(p);
		   match.reset();
		   if (match.find()){
			   phaseText = match.group(0);
			   String phase = match.group(1);
			   phase = Roman.normalizeRomanNumbers(phase);
			   if (phase != null && phase.length() > 0){
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
				}
		   }
		   
		   p = Pattern.compile("\\bTR(?:ACT)?\\s+(\\d+|[A-Z])\\b");
		   match.usePattern(p);
		   match.reset();
		   if (match.find()){
			   tractText = match.group(0);
			   String tract = match.group(1);
			   if (tract != null && tract.length() > 0){
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
			   }
		   }
		   
		   p = Pattern.compile("\\b(SECT|SECTION|SEC|SC|S)(-|\\s+)([-&,\\w]+)\\b");
		   match.usePattern(p);
		   match.reset();
		   if (match.find()){
			   sectionText = match.group(0);
			   String section = match.group(3);
			   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
			   section = Roman.normalizeRomanNumbersExceptTokens(section, exceptionTokens);
			   if(!StringUtils.isEmpty(section)){
				   // check first if the section is not already in PIS
				   if (pis != null && pis.getBody() != null){
					   String[][] body = pis.getBody();
					   int len = body.length;
					   boolean foundSec = false;
					   for (int i = 0; i < len && !foundSec; i++){
						   foundSec = section.equals(body[i][3]);
					   }
					   if (!foundSec){
						   m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
					   }
				   }	
			   }
		   }
		   
		   if ((sectionText != null  && legal.startsWith(sectionText))
				   || (phaseText != null && legal.startsWith(phaseText)) 
				   || (lotText != null && legal.startsWith(lotText))
				   || (tractText != null && legal.startsWith(tractText))){
			   if (sectionText != null){
				   legal = legal.replace(sectionText, "");
			   }
			   if( phaseText != null){
				   legal = legal.replace(phaseText, "");
			   }
			   if (lotText != null){
				   legal = legal.replace(lotText, "");
			   }
			   if (tractText != null){
				   legal = legal.replace(tractText, "");
			   }
		   }
		   
		   if (sectionText == null && lotText == null && lotText == null
				   && legal.equals((String) m.get(SaleDataSetKey.GRANTOR.getKeyName()))){
			   return;
		   }
		   String subdivName = legal.replaceFirst("(.*?)(\\b(S/D|SUBD?|SUBDIVISION|SD|RE\\s?SUB?|BLOCK|LOTS?|UNIT|PH(ASE)?|SECT?(ION)?|CONDO(MINIUM)?S?|BK|TRACT)\\b|(#|\\bNO)?\\s*\\d).*", "$1");
		   subdivName = subdivName.replaceFirst("[-\\.,]+\\s*$", "");
		   subdivName = subdivName.replaceFirst("^[-\\.,]+\\s*", "");
		   subdivName = subdivName.replaceFirst("^SD\\s+", "");
		   subdivName = subdivName.trim();
		   
		   if (subdivName.length() > 0){
			  if (pis != null){
				   String[][] body = pis.getBody();
				   boolean addBody = false;
				   for (int i = 0; i < body.length; i++){
					   body[i][0] = subdivName;
					   addBody = true;
				   }
				   if (addBody){
//					   pis.setReadWrite();
					   pis.setBody(body);
					   pis.setReadOnly();
				   } else{
					   m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName);
				   }
			  } else{
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName);
			  }
		   }
	   	}
}
