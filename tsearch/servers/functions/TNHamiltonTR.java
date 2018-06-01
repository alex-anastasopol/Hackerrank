package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class TNHamiltonTR {
	public static void partyNamesTNHamiltonTR(ResultMap m, long searchId) throws Exception {

		String owner = (String) m.get("PropertyIdentificationSet.OwnerLastName");

		if (StringUtils.isEmpty(owner))
			return;

		if ((owner.contains("TR") || owner.contains("TRS"))
				&& owner.contains("TRUST")) {
			owner = owner.replaceAll("\\bTRS?\\b", "");
		}
		owner = owner.replaceAll("\\s+CO-(TRS)", " $1");
		owner = owner.replaceAll("&\\s+(\\w+\\s+\\w+)\n(MC CRONE)", "& $2 $1");
		owner = owner.replaceAll("(\\w+)\\s+(C/O)", "$1\n$2");
		owner = owner.replaceAll("\n(REVOC.*)", " $1");
		owner = owner.replaceAll("\\bATTY\\b", "").trim();
		owner = owner.replaceAll("(?is)\\s*&\\s*(ETAL)\\b", " $1").trim();
		owner = owner.replaceAll("\n(CO INC)", " $1");
		owner = owner.replaceAll("&\n(\\w+(?:\\s+\\w+)?)\\z", "& $1");
		owner = owner.replaceAll("\n(\\w+)\\z", " $1");
		owner = owner.replaceAll("(C/O\\s+\\w+(?:\\s+[A-Z]+)?\\s+)&(\\s+[A-Z]+\\s+[A-Z]+)\\s+([A-Z]+)",	"$1 $3 & $2 $3");
		owner = owner.replaceAll("(SMITH L) (P & BETTY)", "$1# $2");
		owner = owner.replaceAll("\n", "   ");
		owner = owner.replaceAll("\\s{2,}([A-Z]+)\\s*$", " $1");

		String[] owners = owner.split("\\s{2,}");

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		//String[] namesAux = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			//String own = ow;
			names = StringFormats.parseNameNashville(ow, true);
			if (ow.contains("C/O")) {
				ow = ow.replaceAll("\\bC/O\\b", "");
				names = StringFormats.parseNameDesotoRO(ow, true);
			}
			if (ow.matches("(.+)\\s+&\\s+\\w+\\s+[A-Z]{3,}") && !(ow.trim().toLowerCase()).endsWith("etal")) {
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			}
			names[0] = names[0].replaceAll("#", "");

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		String[] a = StringFormats.parseNameNashville(owner, true);
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}
	
	public static void taxHamiltonTR(ResultMap m,long searchId) throws Exception{
    	String tmpTotalDue = org.apache.commons.lang.StringUtils.defaultString((String) m.get("TaxHistorySet.TotalDue")).trim();
        
    	if (tmpTotalDue.isEmpty() || !tmpTotalDue.matches("^\\d+(\\.\\d+)?")) {
        	tmpTotalDue = "0.00";
        }
        
        m.put("TaxHistorySet.CurrentYearDue", tmpTotalDue);
                	       
        String delinc = (String) m.get("tmpPriorDelinquent");
    	if ((delinc != null)){
    	    delinc = GenericFunctions.sum(delinc, searchId);
    	}else {
    	    delinc = "0.00";
    	}
    	m.put("TaxHistorySet.PriorDelinquent", delinc);
    	BigDecimal totalDelinquent = new BigDecimal(delinc);
    	
    	Date dueDate = DBManager.getDueDateForCountyAndCommunity(
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity()
                        .getID().longValue(), InstanceManager.getManager()
                        .getCurrentInstance(searchId).getCurrentCounty().getCountyId()
                        .longValue());
        
	    Date now = new Date();
	        
	    if (now.after(dueDate)) {
	    	totalDelinquent = totalDelinquent.add(new BigDecimal(tmpTotalDue));
	    }
	        
        m.put("TaxHistorySet.DelinquentAmount", totalDelinquent.toString());
     
       
        String baseAmount = (String) m.get("TaxHistorySet.BaseAmount");
        if (baseAmount != null) {
            baseAmount = GenericFunctions.sum(baseAmount, searchId);
        }else {
            baseAmount = "0.00";
        }
        
        m.put("TaxHistorySet.BaseAmount", baseAmount);
                        
        String amountPaid = (String) m.get("TaxHistorySet.AmountPaid");
        if (amountPaid != null) {
        	amountPaid = GenericFunctions.sum(amountPaid, searchId);
        }else {
        	amountPaid = "0.00";
        }
        
        m.put("TaxHistorySet.AmountPaid",amountPaid);
                
        //Legal description
        String tmpLegal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
        if (tmpLegal == null) {
            tmpLegal = "";
        }
        
        String tmpSubdiv = tmpLegal.replaceAll("(?s).*2\\.(.*)4\\..*","$1");
        
        String garbageSubiv[] = {"\\n|\\r","\\d\\.\\s*\\d+\\-\\d+\\s*$","\\d\\.","(PT\\s*){0,1}LOT(?:S?)\\s*[\\d,&-]+", "LTS\\s*[\\d,&-]+" , 
        		"LT\\s*[\\d,&-]+", "LOT(?:S?)\\s*(PT\\s*){0,1}[\\d,&]+","PB\\s*[\\d,-]+", "PG\\s*[\\d,]+","BLK\\s*[A-Z0-9,]+", "PHASE \\d+",
        		"UNIT(\\s+NO)?\\s*[\\d,]+(-[A-Z]?\\d+[A-Z]?)?", "[A-Z]+\\sST\\s" , "ER\\d+" ,"TOWN OF SODDY","SUB\\s+\\d+-\\d+","RESUB","REV\\s+[\\d-]+",
        		"\\s*& PT \\d+", "\\d+(?:-|\\s)\\d+(?:-|\\s)\\d+", "[NESW]+\\s*\\d+/\\d+", "SEC\\s*[\\dA-Z]+\\s+TWP\\s*[\\dA-Z]+\\s+R[\\dA-Z]+"};
        tmpSubdiv = LegalDescription.cleanGarbage(garbageSubiv, tmpSubdiv);
        tmpSubdiv = tmpSubdiv.trim();
        if (!"".equals(tmpSubdiv)) {
        	m.put("PropertyIdentificationSet.SubdivisionName", tmpSubdiv);
        }
        
        String garbage[] = {"\\n|\\r","\\d\\.","PT"};
        tmpLegal = LegalDescription.cleanGarbage(garbage, tmpLegal);
        String lot = LegalDescription.extractLotFromText(tmpLegal);
        m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
        
        //extract section, township, range
        @SuppressWarnings("rawtypes")
		List<List> body = GenericFunctions2.getSTRFromMap(m);
        List<String> line;
        Pattern p = Pattern.compile("(?is)\\bSEC\\s*([\\dA-Z]+)\\s+TWP\\s*([\\dA-Z]+)\\s+R\\s*-?\\s*([\\dA-Z]+)\\b");	
        Matcher ma = p.matcher(tmpLegal);
        while (ma.find()){		   
        	line = new ArrayList<String>(); 	
        	line.add(ma.group(1));
        	line.add(ma.group(2));
        	line.add(ma.group(3)); 
        	body.add(line);
        }
        GenericFunctions2.saveSTRInMap(m, body);
        
        //block
        String block = "";
        String blkSeparators[] = {"BLK"};
		int blkIndex;
		for (int i = 0; i < blkSeparators.length; i++) {
		    if (tmpLegal.indexOf(blkSeparators[i]) != -1) {
		        blkIndex = blkSeparators[i].length() + tmpLegal.indexOf(blkSeparators[i]);
		        while (tmpLegal.charAt(blkIndex) == ' ') blkIndex++;
		        int end = tmpLegal.indexOf(" ", blkIndex);
		        if (end == -1) end = tmpLegal.length();
		        block = tmpLegal.substring(blkIndex, end);
		        block = block.trim();
		    }
		}
		m.put("PropertyIdentificationSet.SubdivisionBlock",block);
        
		//unit
        String unit = "";
        String unitSeparators[] = {"UNIT", "UNIT NO"};
		int unitIndex;
		for (int i = 0; i < unitSeparators.length; i++) {
		    if (tmpLegal.indexOf(unitSeparators[i]) != -1) {
		        unitIndex = unitSeparators[i].length() + tmpLegal.indexOf(unitSeparators[i]);
		        while (tmpLegal.charAt(unitIndex) == ' ') unitIndex++;
		        int end = tmpLegal.indexOf(" ", unitIndex);
		        if (end == -1) end = tmpLegal.length();
		        unit = tmpLegal.substring(unitIndex, end);
		        unit = unit.trim();
		    }
		}
		m.put("PropertyIdentificationSet.SubdivisionUnit",unit);
		
		//Plat Book
		String platBook = tmpLegal.replaceAll(".*PB\\s*(\\d+)\\s.*", "$1");
		if (platBook.matches("\\d+")) {
		    m.put("PropertyIdentificationSet.PlatBook", platBook);
		}else {
		    m.put("PropertyIdentificationSet.PlatBook", "");
		}
			
		
		//Plat Number
		String platNo = tmpLegal.replaceAll(".*PG\\s*(\\d+)\\s.*", "$1");
		if (platNo.matches("\\d+")) {
		    m.put("PropertyIdentificationSet.PlatNo", platNo);
		}else {
		    m.put("PropertyIdentificationSet.PlatNo", "");
		}
		
    }
}
