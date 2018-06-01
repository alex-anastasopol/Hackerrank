package ro.cst.tsearch.servers.functions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class TNWilliamsonRO {
	
	public static void reparseDocTypeTNWilliamsonRO(ResultMap m, long searchId) throws Exception {//B 4423
		ResultTable crefInfo = (ResultTable) m.get("tmpCrossRefInfo");
		String docType = (String) m.get("SaleDataSet.DocumentType");
		String recDate = (String) m.get("SaleDataSet.RecordedDate");
		recDate = StringUtils.isEmpty(recDate) ? "" : recDate;
		
		if (StringUtils.isEmpty(docType))
			return;
		DateFormat formatter ; 
	          formatter = new SimpleDateFormat("MM/dd/yyyy");
	    Date recordDate = formatter.parse(recDate);
       
		if (docType.matches("(?is).*RERECORDED INSTRUMENT.*")){
			if (crefInfo != null){
				String[][] body = crefInfo.getBodyRef();
				boolean hasMortgage = false;
				if (body.length != 0){
					 for (int i = 0; i < body.length; i++){
						 if (DocumentTypes.isMortgageDocType(body[i][2], searchId)){
							 hasMortgage = true;
							 break;
						 }
					 }
					 if (hasMortgage){
						 for (int j = 0; j < body.length; j++){
							 if (DocumentTypes.isAssignDocType(body[j][2], searchId)){
								 Date assignDate = formatter.parse((body[j][1]).toString());
								 if (recordDate.after(assignDate)){
									 m.put("SaleDataSet.DocumentType", "ASSIGN + RI");
									 break;
								 } else {
									 m.put("SaleDataSet.DocumentType", "MTG + RI");
									 break;
								 }
							 } else {
								 m.put("SaleDataSet.DocumentType", "MTG + RI");
							 }
						 }
					 } else {
						boolean hasAssign = false, hasTransfer = false, hasEsmt = false, hasAppoint = false, hasOther = false;
					 	for (int k = 0; k < body.length; k++){
					 		if (DocumentTypes.isAssignDocType(body[k][2], searchId)){
								 m.put("SaleDataSet.DocumentType", "ASSIGN + RI");
								 hasAssign = true;
								 break;
							 }
					 	}
					 	
					 	if (!hasAssign){
					 		for (int k = 0; k < body.length; k++){
						 		if (DocumentTypes.isTransferDocType(body[k][2], searchId)){
									 m.put("SaleDataSet.DocumentType", "DEED + RI");
									 hasTransfer = true;
									 break;
								 }
						 	}
					 		if (!hasTransfer){
					 			for (int k = 0; k < body.length; k++){
							 		if (DocumentTypes.isEaseDocType(body[k][2], searchId)){
										 m.put("SaleDataSet.DocumentType", "EASEMENT");
										 hasEsmt = true;
										 break;
									 }
							 	}
					 			if (!hasEsmt){
					 				for (int k = 0; k < body.length; k++){
								 		if (DocumentTypes.isAppointDocType(body[k][2], searchId)){
											 m.put("SaleDataSet.DocumentType", "APPOINTMENT");
											 hasAppoint = true;
											 break;
										 }
								 	}
					 			}
					 			if (!hasAppoint){
					 				for (int k = 0; k < body.length; k++){
								 		if (!DocumentTypes.isModificationDocType(body[k][2], searchId)){
											 m.put("SaleDataSet.DocumentType", body[k][2]);
											 hasOther = true;
											 break;
										 }
								 	}
					 				if (!hasOther){
					 					m.put("SaleDataSet.DocumentType", "MISCELLANEOUS");
					 				}
					 			}
					 		}
					 	}
					 }
				}
			}
		}
		
	}
	
	@SuppressWarnings("rawtypes")
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
				
		name = name.replaceAll("(?is)\\b[FAN]\\s*/\\s*K\\s*/\\s*A\\b", "\n");
		name = name.replaceAll("(?is)\\b(CO\\s+)?EXECU?\\b", "");
		name = name.replaceAll("(?is)\\bWILL\\s+OF\\b", "");
		name = name.replaceAll("(?is)\\bCO\\s+(TR(?:USTEE)?)\\b", "$1");
		name = name.replaceAll("(?is)\\bDECEASED\\b", "");
		name = name.replaceAll("(?is)[\\(\\)]+", "");
		name = name.replaceAll("(?is)\\s*\n\\s*", "/");
		
		String[] nameItems = name.split("\\s*/\\s*");
		for (int i = 0; i < nameItems.length; i++){
			nameItems[i] = nameItems[i].replaceAll("(?is)\\s+(TR(?:USTEE)?S?)(,.*)", "$2 $1");
			names = StringFormats.parseNameNashville(nameItems[i], true);
						
			type = GenericFunctions.extractNameType(names);
			otherType = GenericFunctions.extractNameOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
				
		}

	}	
	
}
