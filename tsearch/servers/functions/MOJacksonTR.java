package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class MOJacksonTR {
    public static void taxMOJacksonTR(ResultMap m,long searchId, String stateAbbreviation, String countyName, String siteAbbreviation) throws Exception {
     	
    	BigDecimal priorDelinq = new BigDecimal(0.00);
    	BigDecimal baseAmount = new BigDecimal(0.00);
    	ResultTable tmpTaxesDueTable = (ResultTable) m.get("tmpTaxesDueTable");
    	ResultTable tmpTaxHistorySet = (ResultTable) m.get("TaxHistorySet");
    	Date dateNow = new Date();
    	int currentYear;
    	currentYear = Calendar.getInstance().get(Calendar.YEAR);
    	
    	DateFormat myDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    	boolean alreadyTaken = false;
    	
    	int year = Calendar.getInstance().get(Calendar.YEAR);
    	
    	try{//5177
	    	long serverId = TSServersFactory.getSiteId(stateAbbreviation, countyName, siteAbbreviation);
	    	int commId = InstanceManager.getManager().getCurrentInstance(searchId).getCommunityId();
	        DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
	    	Calendar cal = Calendar.getInstance();
	    	cal.setTime(dat.getPayDate());
	        year = cal.get(Calendar.YEAR);
    	}catch(Exception e){}
    	
		String taxYear = (String) m.get(TaxHistorySetKey.YEAR.getKeyName());
		if(taxYear==null){
			m.put(TaxHistorySetKey.YEAR.getKeyName(), "");
			taxYear = "";
		}
		
		try{
			if(Integer.parseInt(taxYear)>year){
				taxYear = year+"";
				m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}
		}catch(Exception e){}
		
    	if (tmpTaxesDueTable == null) {
    		if (tmpTaxHistorySet != null) {
    			String[][] body = tmpTaxHistorySet.getBody();
    			if (body[0][0].contains(taxYear) || body[0][0].contains(String.valueOf(currentYear))) {
    				m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), body[0][2]);
    				m.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(), body[0][1]);
    				m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), body[0][4]);
    				String totalDue = (String) m.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()); 
    				if (StringUtils.isEmpty(totalDue)){
    					try {
    						m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), new BigDecimal(body[0][2]).subtract(new BigDecimal(body[0][4])).toString());
    					} catch (Exception e) {
							e.printStackTrace();
						}
    				}
					   
    			}
    		}
    	} else {
    		String[][] bodyDue = tmpTaxesDueTable.getBody();
	    		for (int i=0; i<bodyDue.length; i++){
	    			if (bodyDue[i][1].contains("Delinquent")){
	    				priorDelinq = priorDelinq.add(new BigDecimal(bodyDue[i][5]));
	    				m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "0");
		    			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), "0");
		    			m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "0");
	    			} else if (bodyDue[i][1].contains("1")){
	    				m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), bodyDue[i][3]);
	    				m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), bodyDue[i][5]);
	    			} else {
	    				baseAmount = baseAmount.add(new BigDecimal(bodyDue[i][3]));
	    				if (dateNow.before(myDateFormat.parse(bodyDue[i][2])) && !alreadyTaken) {
	    					m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), bodyDue[i][5]);
	    					alreadyTaken = true;
	    				}
	    			}
	    		}
	    		if (tmpTaxHistorySet != null) { 
	    			String[][] bodyTaxHist = tmpTaxHistorySet.getBody();
		    		if (bodyTaxHist[0][0].matches(".*12/.*") && bodyTaxHist[0][0].contains(taxYear)){
		    			baseAmount = baseAmount.add(new BigDecimal(bodyTaxHist[0][2]));
		    			m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), bodyTaxHist[0][4]);
		    			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount.toString());
		    		}
	    		}
	    		m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinq.toString());
    	}
    }
    
    @SuppressWarnings("rawtypes")
	public static void partyNamesTokenizerMOJacksonTR(ResultMap m, String s, long searchId) throws Exception {
    	
    	String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
    	
    	// corrections and cleanup
 
    	if (!"platte".equals(crtCounty.toLowerCase())){
    		s = s.replace(",", "&");
    	}
    	s = s.replaceAll("\\b([A-HJ-UW-Z])([IV]{2,}|SR|JR)\\b", "$1 $2");
    	s = s.replace(".", ""); 
    	s = s.replaceAll("\\bAKA\\b", "&");
    	
    	s = s.replaceAll("\\b(FBO|D/?B/?A)\\b", "@@");
        s = s.replaceAll("\\([^\\)]*\\)?", "");
        s = s.replaceAll("\\s*&\\s*(?=$|@@)", "");
        s = s.replaceAll("\\s*&\\s*(WF|WIFE)\\b", "");        
        s = s.replaceAll("\\s*\\b(?:CO )?(TR(?:USTEE)?(?:'?S)?)\\b\\s*", " $1");
        s = s.replaceAll("\\*", "");
        s = s.replaceAll("\\s*-\\s*(?=$|@@)", "");
        s = s.replaceAll("\\s*-([A-Z])\\b", " $1");
        if (s.matches("\\A.*\\b(\\w+)\\s+[A-Z]+(?:\\s*[A-Z])?\\s*(?:&\\s[A-Z]+(?:\\s*[A-Z])?\\s)?(\\1)\\b.+"))  // PID:JA30320151100000000 (MOJacksonEP)
        {   //SMITH PAUL & MILES SMITH JANE K
        	s = s.replaceFirst("\\b(\\w+)(\\s+[A-Z]+(?:\\s*[A-Z])?\\s*(?:&\\s[A-Z]+(?:\\s*[A-Z])?\\s)?)(\\1\\b.+)","$1 $2@@$3");
        }        
        s = s.replaceAll("\\s{2,}", " ").trim();
        String entities[] = s.split("@@");

        List<List> body = new ArrayList<List>();
        
        // parse each entity as L F M
        String[] a = new String[6];
        String[] b = new String[6];
        String[] tokens; 
        String[] addOwners;
        String[] suffixes, type, otherType;
        Matcher ma1, ma21, ma22;
        for (int i=0; i<entities.length; i++){        	
        	if (NameUtils.isCompany(entities[i])){        		
            	a[2] = entities[i]; 
            	a[0] = ""; a[1] = ""; a[3] = ""; a[4] = ""; a[5] = "";
            	
            	type = GenericFunctions.extractAllNamesType(a);
				otherType = GenericFunctions.extractAllNamesOtherType(a);
            	GenericFunctions.addOwnerNames(a, "", "", type, otherType, true, false, body);
        	} else {
        		boolean spouseHasLast = false;
        		boolean spouseIsFL = false;
        		String owner3 = "";
        		String spouseSuffix = "";
        		String spouseType = "";
        		entities[i] = entities[i].replaceAll("\\b([A-Z]{2,}) (MC[A-Z]+)(?! (&|JR|SR|[IV]+))(?!$)\\b", "$1_$2");
	        	ma1 = Pattern.compile("(.+?)\\s*&\\s*([^&]+)&?(.*)").matcher(entities[i]);
	        	if (ma1.find()){
	        		entities[i] = ma1.group(1) + " & " + ma1.group(2);
	        		String spouse = ma1.group(2).trim(); 	        		
	        		owner3 = ma1.group(3).trim();
	        		ma21 =  Pattern.compile("(?is)(.*?)\\s+" + GenericFunctions.nameTypeString).matcher(spouse);
	        		if (ma21.matches()){		// spouse has type => remove it 
	        			spouse = ma21.group(1).trim();
	        			spouseType = ma21.group(2);
	        			entities[i] = ma1.group(1) + " & " + spouse;
	        		}
	        		ma22 = GenericFunctions.nameSuffix.matcher(spouse);
	        		if (ma22.matches()){		// spouse has suffix => remove it 
	        			spouse = ma22.group(1).trim();
	        			spouseSuffix = ma22.group(2);
	        			entities[i] = ma1.group(1) + " & " + spouse;
	        		}        		        		
	        		tokens = spouse.split(" ");
	        		// if spouse contains a double last name (e.g. JACKSON JEREMY & ROSSLYN CRAWFORD-JACKSO) or is F MI L 
	        		// (e.g. JACKSON DAVID W & RUSSELL J HOWERTON-TRS), then parse it separately  
	        		if ((tokens.length >= 2 && tokens[tokens.length-1].contains("-")) || (spouse.matches("[A-Z]{2,} [A-Z] [A-Z']{2,}"))){
	        			spouseHasLast = true;
	        			spouseIsFL = true;
	        		}        		
	        		a = StringFormats.parseNameNashville(entities[i], true);
	        		if (tokens.length == 2 && tokens[1].length() > 1){
	        			if (tokens[0].contains("_") || (spouseSuffix.length() > 0 && spouse.matches("[A-Z']{2,} [A-Z]{2,}"))
	        					|| (spouseType.length() > 0 && spouse.matches("[A-Z']{2,} [A-Z]{2,}"))){
	        				spouseHasLast = true;
	        			} else if (!tokens[0].contains("-") && !tokens[1].contains("-") && !tokens[0].contains(a[2])){
	        				spouse = a[2] + " " + spouse;
	        				spouseHasLast = true;
	        			}  
	        		}else{
	        			//the case in which the spouse has a different last name and has a MI (L F Mi )
	        			if (tokens.length!=3){
	        				spouse = a[2] + " " + spouse;
	        			}
        				spouseHasLast = true;
	        		}
	        		if (spouseHasLast) {
	        			entities[i] = ma1.group(1);
	        			a = StringFormats.parseNameNashville(entities[i], true);
	        			if (!spouseIsFL){
	        				b = StringFormats.parseNameNashville(spouse, true);
	        			} else {
	        				b = StringFormats.parseNameDesotoRO(spouse, true);
	        			}
	        			a[3] = b[0];
	        			a[4] = b[1];
	        			a[5] = b[2];
	        		}   		
	        	} else {
	        		a = StringFormats.parseNameNashville(entities[i], true);
	        	}
                suffixes = GenericFunctions.extractNameSuffixes(a);
                type = GenericFunctions.extractAllNamesType(a);
				otherType = GenericFunctions.extractAllNamesOtherType(a);
				
                if (spouseSuffix.length() != 0){
                	suffixes[1] = spouseSuffix;
                }
                if (spouseType.length() != 0){
                	type[1] = spouseType;
                }
                a[2] = a[2].replace("_", " ");
                a[5] = a[5].replace("_", " ");
                GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);	     
	        	String prevLast = a[5];	       
	        	owner3 = owner3.trim();
	        	if (owner3.length() != 0){
	        		addOwners = owner3.split("\\s?&\\s?");
	        		for (int j=0; j<addOwners.length; j++){
	        			if (!addOwners[j].matches("[A-Z'-]{2,} [A-Z]+ [A-Z]+")){
	        				addOwners[j] = prevLast + " " + addOwners[j];
	        			}
	        			a = StringFormats.parseNameNashville(addOwners[j], true);
	        			prevLast = a[2];
	        			suffixes = GenericFunctions.extractNameSuffixes(a);
	        			type = GenericFunctions.extractAllNamesType(a);
	    				otherType = GenericFunctions.extractAllNamesOtherType(a);
	    				
	        			GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(a[2]), false, body);
	        		}
	        	}
        	}
        }                
        GenericFunctions.storeOwnerInPartyNames(m, body, true);                
    }

    public static void subdivMOJacksonTR(ResultMap m,long searchId)throws Exception {
        // Owner name pt bootstrap
        String s = (String) m.get("SaleDataSet.Grantor");
        String pid = (String) m.get("PropertyIdentificationSet.ParcelID");
        s = s.replaceAll("(?i)-?TRUSTEE","");
      //  String[] u = StringFormats.parseNameDesotoRO(s);
        String[] u = StringFormats.parseNameNashville(s);
        m.put("PropertyIdentificationSet.OwnerFirstName", u[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", u[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", u[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", u[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", u[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", u[5]);
        m.put("SaleDataSet.Grantor", s);
        
        String tmpLegal = (String) m.get("tmpLegal");
        String tmp = tmpLegal;
        String tmpL = tmpLegal; //B2939
        tmp = tmp.toUpperCase();
        //Subdivision Name
//        String tokens[] = {"---"," TRACT"," BEG "," ALL ",
//        		" ALL", /*bug #626*/
//        		"ADD"
//        		,
//        		" ADD "
//        		,
//        		" ADD"/*bug 594*/
//        		,
//        		"ADD "
//        		,
//        		" PT " ," LOT "," LOTS ", " LT ", " CORR ", " E "," W "," N "," S "," SUB ",
//                " COMMON ","---LOT ", " (" , ",", ".", " LTS" , "---UNITS", " UNITS","---UNIT"," UNIT ","BLK" , "BLOCK", " NLY ", " SUBDIVISION", 
//                " SECTION " //Jackson CnT PID 43-230-04-30-00-0-00-000
//                }; // replaced with a regular expression to fix bug #1047
        tmp = tmp.replaceAll("\\n|\\r","");
        tmp = tmp.replaceAll("\\s{2,}"," ");
        tmp = tmp.replaceAll("~"," ");
        
        m.put("PropertyIdentificationSet.PropertyDescription", tmp);
        
        //tmp = tmp.replaceAll("-"," ");
        //tmp = tmp.replaceAll("---"," ");
        tmp = tmp.replaceFirst(".*\\bLEGAL DESCRIPTION\\b.*", ""); // fix for bug #2665
        tmp = tmp.replaceAll("\\b(LOTS|LOT|LTS|LT)\\s+\\d+(\\.\\d+)?'", ""); // Jackson CnT PID 31-740-05-34-00-0-00-000, bug #296        
        tmp = tmp.replaceAll("\\b((LOTS|LOT|LTS|LT)\\s+(\\d+,?\\s*)?)\\d+(\\.\\d+)?'", "$1"); // Jackson CnT PID 31-740-05-34-00-0-00-000, bug #296
        tmp = tmp.replaceAll("P.U.D.", "PUD"); // Jackson Cnt PID 35-510-03-12-00-0-00-000
    	tmp = tmp.replaceAll("(\\b[EWSN]\\s)?\\d+(\\.\\d+)?''?", "");  //PID 30-120-02-14-00-0-00-000	
        tmp = tmp.replaceAll("\\."," ");
        tmp = tmp.replaceAll("\\s{2,}"," ");
        tmp = tmp.replaceAll("(\\d+)BLK\\b","$1"+" BLK");
        tmp = tmp.replaceAll("RANG E","RANGE");
        tmp = tmp.replaceAll("\\bPLA T\\b", "PLAT"); //Jackson CnT PID 13-830-05-08-00-0-00-000, bug 574
        tmp = tmp.replaceAll("\\bOF OF\\b", "OF");	 //Jackson CnT PID 13-830-05-08-00-0-00-000, bug 574
        tmp = tmp.replaceAll("\\b(LO?TS?)(\\d)", "$1 $2"); //Jackson CnT PID 13-830-05-08-00-0-00-000, bug 574
        tmp = tmp.replaceAll("PLATLOT", "PLAT LOT"); // Jackson CnT PID 33-330-14-52-00-0-00-000
        tmp = tmp.replaceAll("BELVIDERE HEIGHTS ADD","BELVIDERE HEIGHTS");
        tmp = tmp.replaceAll("AMEND PLAT OF STONERIDGE","STONERIDGE");
        tmp = tmp.replaceAll("SPRNGBRANCH", "SPRINGBRANCH"); // Jackson CnT PID 25-240-05-20-00-0-00-000
        tmp = tmp.replaceAll("SNI-A-BAR CROSSINGS AT SNI-A-BAR FARMS", "SNI-A-BAR CROSSING AT SNI-A-BAR FARMS"); // Jackson CnT PID 40-310-99-96-00-0-00-000, bug #1374
        if("32-830-10-27-00-0-00-000".equals(pid)){ //fix for bug #472
        	tmp = tmp.replace("(", "");
        	tmp = tmp.replace(")", "");
        } else {
        	tmp = tmp.replaceAll("\\(.+\\)", "");
        }
        tmp = tmp.replaceAll("\\bL OT(S?)\\b","LOT$1"); 
        tmp = tmp.replaceAll("\\bLO T(S?)\\b","LOT$1"); 
        tmp = tmp.replaceAll("\\sTHRU\\s","-");	
        tmp = tmp.replaceAll("LOTS\\s(\\d{2})\\s([^&-])","LOT "+"$1"+" "+"$2");
        tmp = tmp.replaceAll("LOTS\\s(.*)&\\s[A-Z]\\s\\d{2}'\\sOF\\sLOT","LOTS "+"$1"+"&");
        tmp = tmp.replaceAll("LOT TH","");
        tmp = tmp.replaceAll("LOT(\\s+\\d+(\\s+|\\s*[,&]\\s*)\\d+)", "LOTS$1"); // PID 41-440-12-33-00-0-00-000
        
        //cleanup for Jackson CnT PID 67-640-10-75-00-0-00-000, 34-830-07-01-00-0-00-000 
        tmp = tmp.replaceAll("^((?:RES(?:URVEY)?|(?:RE)PL(?:AT)?)(?:\\s+OF)?(?:(?:\\s*LO?TS?\\s+\\d+(?:(?:-\\d+)|(?:\\s*(?:,|&)\\s*\\d+))?\\s*(?:&|,)?)?(?:\\s*+BLO?C?KS?\\s+\\d+(?:(?:-\\d+)|(?:,\\s*\\d+))?)?(?:\\s*TR(?:ACT)?S?\\s+[A-Z](?:-\\d+)?(?:,\\s*[A-Z](?:-\\d+)?)?)?\\s*(?:&|,)?)+)\\s+(.+)", "$2 $1");		        
                
        //cleanup for Jackson CnT PID 43-230-04-30-00-0-00-000
        tmp = tmp.replaceAll("([^\\s])---", "$1 ---");
        
        // cleanup for PID 67-640-09-28-00-0-00-000  Jackson CnT (bug #547)
        tmp = tmp.replaceAll("\\bMINOR SUB(DIVISION)?( OF)?\\b", ""); 
        tmp = tmp.replaceAll("\\bP(AR)?TS? OF\\s+", "").trim();
        tmp = tmp.replaceAll("^(LO?TS? \\d+(?:\\s*(?:-|,|\\bAND\\b|\\bTO\\b|\\s)\\s*\\d+)*)\\s(.+)", "$2 $1").trim();
        
        tmpL = tmpL.replaceAll("(.+[\\d-]+)\\s+(.+)", "$2");
        if (pid.equals("43-820-07-44-01-0-00-000"))  //B2939
        	tmp = tmpL;
        
        //B5822
        String tmpForLot = tmp;
        tmpForLot = tmpForLot.replaceAll("-{3,4}", " ");
        String removeDoubleLotsRegEx = "(?is)\\bLO?TS\\s*[\\w-]+.*(\\s+LOT\\s*\\w+)";
		tmpForLot = tmpForLot.replaceAll(removeDoubleLotsRegEx, "$1");
        
        // Lot
        String lot = "";
		lot = LegalDescription.extractLotFromText(tmpForLot, false, true);
		m.put("PropertyIdentificationSet.SubdivisionLotNumber",lot);
        
		// Section
		String secSeparators[] = {"SECTION ","SEC-"};
		String sec = "";
		int secIndex;
		for (int i=0;i<secSeparators.length;i++) {
		    if (tmp.indexOf(secSeparators[i])!=-1) {
		    	tmp += " "; // PID 26-330-20-07-00-0-00-000 bug #9885
		        secIndex = secSeparators[i].length() + tmp.indexOf(secSeparators[i]);
		        sec= tmp.substring(secIndex, tmp.indexOf(" ",secIndex));
		        sec = sec.trim();
		    }
		}
        /*
         * B427
         */
        if( sec.startsWith( "0" ) )
        {
            sec = sec.substring( 1 );
        }
        
		m.put("PropertyIdentificationSet.SubdivisionSection",sec);
		
		// Township
		String townSeparators[] = {"TOWNSHIP ","TWP-","TWNSHP"};
		String town = "";
		int townIndex;
		for (int i=0;i<townSeparators.length;i++) {
		    if (tmp.indexOf(townSeparators[i])!=-1) {
		        townIndex = townSeparators[i].length() + tmp.indexOf(townSeparators[i]);
		        town= tmp.substring(townIndex, tmp.indexOf(" ",townIndex));
		        town = town.trim();
		    }
		}
		m.put("PropertyIdentificationSet.SubdivisionTownship",town);

		// Range
		String rngSeparators[] = {"RANGE ","RNG-"};
		String rng = "";
		int rngIndex;
		for (int i=0;i<rngSeparators.length;i++) {
		    if (tmp.indexOf(rngSeparators[i])!=-1) {
		        rngIndex = rngSeparators[i].length() + tmp.indexOf(rngSeparators[i]);
		        rng= tmp.substring(rngIndex, tmp.indexOf(" ",rngIndex));
		        rng = rng.trim();
		    }
		}
		m.put("PropertyIdentificationSet.SubdivisionRange",rng);
		
		if ((!sec.equals(""))&& (!town.equals(""))&& (!rng.equals(""))) {
		    m.put("PropertyIdentificationSet.SubdivisionName","");
		}
		
		// Block
        String block = "";
		String blkSeparators[] = {"BLK ","BLKS","BLOCK","BLOCKS"};
		int blkIndex;
		int idxBlock = 0;
		for (int i=0;i<blkSeparators.length;i++) {
		    while (tmp.indexOf(blkSeparators[i], idxBlock)!=-1) {
		        blkIndex = blkSeparators[i].length() + tmp.indexOf(blkSeparators[i], idxBlock);
		        while (tmp.charAt(blkIndex)==' ') blkIndex++;
		        int end = tmp.indexOf(" ",blkIndex);
		        if (end ==-1) end = tmp.length();
		        block = block + " " + tmp.substring(blkIndex, end);
		        block = block.trim();
		        idxBlock = end;
		    }
		}
		Pattern blk = Pattern.compile("\\bBLK(\\d+)\\b");
		Matcher blkM = blk.matcher(tmp);
		if (blkM.find()){
			block += " " + blkM.group(1);
		}
		block = StringFormats.RemoveDuplicateValues(block);
		m.put("PropertyIdentificationSet.SubdivisionBlock",block);
				
		/*B 5822
		tmp = tmp.replaceAll("(.+?)\\s*LOTS?\\s*\\d+.*", "$1");
		
        tmp = tmp.replaceAll(
				"(.*?)(\\d+\\s*ST|\\d+ND|\\d+RD|\\d+TH|FIRST|SECOND|THIRD|FOURTH|FIFTH|SIXTH|SEVENTH|\\WEIGHTH?|NINTH|" +
				"TENTH|TWELFTH|TWENTIETH|EIGHTY-FIFTH|FIFTY-SEVENTH|FIFTY-NINTH)(.*?REPLATS| REPLAT| REPL| REP|.*?PLATS|" +
				".*?PLAT|.*?PLT|.*?PL|.*?UNITS?| ADDITION| ADD| PHASE| RESURVEY| RES)\\b.*", "$1");
        */
        tmp = tmp.replaceAll("\\s*\\bOF\\s*$", ""); // Jack CnT PID 13-830-05-08-00-0-00-000 b574
        
        tmp = tmp.replaceAll("(.*?)\\s(CORRECTED|CORREC|CORR|COR||FINAL|AMENDED|AMEND|" +
        		"AMEN|REVISED)\\s*(PT|PL|PLATS?)?\\s*$", "$1");
        
        tmp = tmp.replaceFirst("^(CORRECTED|CORREC|CORR|COR||FINAL|AMENDED|AMEND|" +
        		"AMEN|REVISED)\\s*(PT|PL|PLATS?)\\s(OF)?\\s*", "");
        
        tmp = tmp.replaceAll("(.*?)\\s(TO|\\d+|ADDITION #\\d*|\\d+\\s*PLA?T?|\\d+P|PHASE.*?|NO|CONTINUATION)$", "$1");
		tmp = tmp.replaceAll("(.*?)\\s(ADDITION)\\s*(CONTINUATION|NO\\d*)?$", "$1");
       
		tmp = tmp.replaceAll("(.*?)\\s*PLAT\\s*(#.*|$)", "$1"); //PID 38-900-45-01-00-0-00-000 
		        
        // cleanup for PID 26-720-09-08-00-0-00-000; 61-340-03-08-00-0-00-000, 41-440-12-33-00-0-00-000
        tmp = tmp.replaceAll("\\bRESURVEY\\b(\\s*OF\\s+(THE\\s+)?)?", "");
        tmp = tmp.replaceAll("\\bRES\\b(\\s*OF\\s+(THE\\s+)?)?", "");
        tmp = tmp.replaceAll("\\bREPLAT\\b(\\s*OF\\s+(THE\\s+)?)?", ""); 
        tmp = tmp.replaceAll("^(BL(?:OC)?K\\s+\\S+)\\s+(.*)", "$2"+" "+"$1");
        
        //cleanup for Jackson CnT PID 61-720-07-28-00-0-00-000
        tmp = tmp.replaceAll("REVISED\\s+(.+)", "$1");
        
        //cleanup for Jackson CnT PID 33-330-14-52-00-0-00-000
        tmp = tmp.replaceAll("^\\bPARTS?(?:\\s+OF)?\\s+(LO?T\\s+\\d+)(.*)", "$2 $1");
        		
        //cleanup for Jackson CnT PID 13-730-09-03-00-0-00-000 (bug #452)
        tmp = tmp.replaceAll("(?i)\\s*(NO|#)\\s?\\d+", "");
        
        tmp = tmp.trim();
        tmp = tmp.replaceAll(","," ");
        tmp = tmp.replaceAll("'","");
        tmp = tmp.replaceAll("\\s{2,}"," ");
               
        if (!tmp.startsWith("THE PADDOCK")) { // fix for bug #1690
        	tmp = tmp.replaceFirst("\\s*\\bTHE\\b", "").trim(); //PID 38-900-45-01-00-0-00-000, 45-830-03-25-00-0-00-000 bug #734
        }
        tmp = tmp.replaceFirst("\\sTH$", ""); // PID 28-540-12-29-00-0-00-000, bug #1334
//        int []indexes = new int[tokens.length];
//		int first = 0;		
//
//		for (int i=0; i<tokens.length;i++) {
//		    indexes[i] =  tmp.indexOf(tokens[i]);
//		    if (indexes[i]==-1) indexes[i]= tmp.length() +1;
//		}
//		
//		for (int i=0; i<tokens.length;i++) {
//		    if (indexes[i]<indexes[first]) first = i;
//		}
		
        tmp = tmp.replaceFirst("(?i)\\bPUD\\s*$", "");
        tmp = tmp.replaceFirst("(?i)\\bSUB\\s*$", ""); // PID 45-220-02-44-00-0-00-000 bug #733
        tmp = tmp.replaceAll("J P W", "JPW");
//		int index = tmp.indexOf(tokens[first]);
        if(tmp.indexOf("RUSKIN HEIGHTS")!=-1){ //Bug 676 PID 63-320-12-07-00-0-00-000
        	tmp = tmp.replaceFirst("SWLY",""); 
        }
        
        if (pid.equals("43-820-07-44-01-0-00-000"))  //B2939
        	tmp = tmpL;
        
		String r = "";  
		tmp = tmp.replaceAll("-{3,4}", " ");;
		tmp = tmp.replaceAll("(?is)\\bLO?TS\\s*[\\w-]+.*(\\s+LOT(\\s*\\w+)?)", "$1");
        Pattern patt = Pattern.compile("(.*?)\\s*(---|\\(|,|\\.|(\\b(TRACT|BEG|ALL|ADD(ITIO)?N?|PT|LOT|LTS?|CORR|E|W|N|S|SUB|COMMON|UNITS?|BL(OC)?K|NLY|SUBDIVISION|SECTION|PH (\\d+|[IVX]+))\\b))");
        Matcher match = patt.matcher(tmp); //fix for bug #1047
        
		if (match.find()) {
			r = match.group(1);	
		    r = r.replaceFirst("CONTINUATION",""); //Bug 637 PID 27-130-18-02-00-0-00-000
		    r = r.replaceFirst("(?is)\\bSEC-\\d+\\b","");
		    r = r.replaceFirst("(?is)\\bTWP-\\d+\\b","");
		    r = r.replaceFirst("(?is)\\bRNG-\\d+\\b","");
		    r = r.replaceFirst("(?is)\\b(NE|NW|SE|SW|N|S|E|W)\\s+\\d+/\\d+\\b","");
		    r = r.replaceFirst("(?is)\\bDAF:","");
		    r = r.trim();
		    if ((r.indexOf(" CONDO ")!=-1)||(r.indexOf(" CONDOS ")!=-1)||
		        (r.indexOf(" CONDOMINIUM ")!=-1)||(r.indexOf(" CONDOMINIUMS")!=-1))
		    {		
		    	
		        m.put("PropertyIdentificationSet.SubdivisionCond",r);
		        m.put("PropertyIdentificationSet.SubdivisionName",r);//ar trebui sa fie vid daca s-ar face cautare dupa Condominium
		        int unitIndex=-1;
		        String [] unitSeparators = {" UNIT "};
				int []indexUnitDelims = new int[unitSeparators.length];
				String unit = "";
				int minUnitIndex = 0;
				for (int i=0; i<unitSeparators.length;i++) {
				    indexUnitDelims[i] =  tmp.indexOf(unitSeparators[i]);
				    if (indexUnitDelims[i]==-1) indexUnitDelims[i]= tmp.length() +1;
				}
				for(int i=0;i<unitSeparators.length;i++) {
				    if (indexUnitDelims[i] < indexUnitDelims[minUnitIndex]) minUnitIndex= i;
				}
				unitIndex = tmp.indexOf(unitSeparators[minUnitIndex]);
				if (unitIndex!=-1) {
				    unitIndex = unitIndex + unitSeparators[minUnitIndex].length();
				    while (tmp.charAt(unitIndex)==' ') unitIndex++;
				
				if (tmp.indexOf(" ",unitIndex)!=-1) {
				    unit = tmp.substring(unitIndex,tmp.indexOf(" ",unitIndex));
				}else {
				    unit = tmp.substring(unitIndex,tmp.length());
				}
				unit=unit.trim();
				if (unit.indexOf("-")!=-1) {
				    String bldg = "";
				    String a = "";
				    String b = "";
				    a = unit.substring(0,unit.indexOf("-"));
				    b = unit.substring(unit.indexOf("-")+1,unit.length());
				    if ((a.matches("[A-Z]+"))&&(b.matches("[0-9]+"))){
				        bldg=a;
				        unit=b;
				    }
				    else {
				        bldg=b;
				        unit=a;
				    }
				    m.put("PropertyIdentificationSet.SubdivisionBldg",bldg);
				}
				}
				m.put("PropertyIdentificationSet.SubdivisionUnit",unit);
				
		    }
		    else {
		        m.put("PropertyIdentificationSet.SubdivisionName",r);
		    }
		    
		}
		else {
			tmp = tmp.replaceAll("(.*) BLKS [\\d&\\s]+", "$1");//B3862
//			tmp = tmp.replaceAll("(.*) LOT\\s*$", "$1");
			tmp = tmp.replaceAll("(.*) \\bLOTS?\\b.*$", "$1");
		    m.put("PropertyIdentificationSet.SubdivisionName",tmp);
		}
		
		String tmpLegalExp = (String) m.get("tmpLegal");
		tmpLegalExp = tmpLegalExp.replaceFirst("CONTINUATION",""); //bug 637 PID 27-130-18-02-00-0-00-000
		tmpLegalExp = tmpLegalExp.replaceFirst("SWLY","");
		if (tmpLegalExp!=null) {
		    if (tmpLegalExp.indexOf("808 W 103RD STREET ADDITION")!=-1) {
		        m.put("PropertyIdentificationSet.SubdivisionName", "808 W 103RD STREET ADDITION");
		    }
		}//pt parcel 48-910-09-65-00-0-00-000
		
		if (tmpLegalExp.contains("SIBLEY NEW TOWN")){
			m.put("PropertyIdentificationSet.SubdivisionName", "SIBLEY TOWN");
		} // PID = 07-220-10-09-00-0-00-000
				
		String tmpAddr = (String) m.get("tmpAddr");
		tmpAddr = tmpAddr.substring(0,tmpAddr.indexOf(","));
		tmpAddr = tmpAddr.replaceAll("\\n|\\r","");
        tmpAddr = tmpAddr.replaceAll("\\s{2,}"," ");
        tmpAddr = tmpAddr.trim();
		m.put("PropertyIdentificationSet.StreetNo",StringFormats.StreetNo(tmpAddr));
		m.put("PropertyIdentificationSet.StreetName",StringFormats.StreetName(tmpAddr));
		
		
		//CrossRefInstrument
		String tmpSaleInstrument = (String) m.get("SaleDataSet.CrossRefInstrument");
		if (tmpSaleInstrument!=null) {
		    tmpSaleInstrument = tmpSaleInstrument.replaceAll("\\s+","");
		    m.put("SaleDataSet.CrossRefInstrument",tmpSaleInstrument);
		}
		
//        System.out.println("Legal description:  "+tmpLegal);
//        System.out.println("Street Number:  "+(String)m.get("PropertyIdentificationSet.StreetNo"));
//        System.out.println("Street Name:  "+(String)m.get("PropertyIdentificationSet.StreetName"));
//        System.out.println("City:  "+(String)m.get("PropertyIdentificationSet.City"));
//        System.out.println("Subdivision Name:  "+(String)m.get("PropertyIdentificationSet.SubdivisionName"));
//        System.out.println("Lot:  "+(String)m.get("PropertyIdentificationSet.SubdivisionLotNumber"));        
//        System.out.println("Section:  "+(String)m.get("PropertyIdentificationSet.SubdivisionSection"));
//        System.out.println("Township:  "+(String)m.get("PropertyIdentificationSet.SubdivisionTownship"));
//        System.out.println("Range:  "+(String)m.get("PropertyIdentificationSet.SubdivisionRange"));
//        System.out.println("Block:  "+(String)m.get("PropertyIdentificationSet.SubdivisionBlock"));
//        System.out.println("CrossRefInstrument:  "+(String)m.get("SaleDataSet.CrossRefInstrument"));
//        System.out.println("Sales Price:  "+(String)m.get("SaleDataSet.SalesPrice"));
    }    
}
