package ro.cst.tsearch.extractor.xml;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.xerces.dom.DeferredTextImpl;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.search.address.Normalize;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.GenericISI;
import ro.cst.tsearch.servers.functions.GenericOrbit;
import ro.cst.tsearch.servers.functions.GenericParsingOR;
import ro.cst.tsearch.servers.functions.ILDuPageRO;
import ro.cst.tsearch.servers.functions.ILLakeTR;
import ro.cst.tsearch.servers.functions.ILMcHenryTR;
import ro.cst.tsearch.servers.functions.KSWyandotteOR;
import ro.cst.tsearch.servers.functions.MOClayRO;
import ro.cst.tsearch.servers.functions.MOJacksonEP;
import ro.cst.tsearch.servers.functions.MOJacksonTR;
import ro.cst.tsearch.servers.functions.OHFranklinAO;
import ro.cst.tsearch.servers.functions.TNDavidsonRO;
import ro.cst.tsearch.servers.functions.TNDavidsonTR;
import ro.cst.tsearch.servers.functions.TNGenericAO;
import ro.cst.tsearch.servers.functions.TNGenericTR;
import ro.cst.tsearch.servers.functions.TNGenericUsTitleSearchRO;
import ro.cst.tsearch.servers.functions.TNHamiltonEP;
import ro.cst.tsearch.servers.functions.TNHamiltonRO;
import ro.cst.tsearch.servers.functions.TNHamiltonTR;
import ro.cst.tsearch.servers.functions.TNKnoxEP;
import ro.cst.tsearch.servers.functions.TNKnoxRO;
import ro.cst.tsearch.servers.functions.TNRutherfordEP;
import ro.cst.tsearch.servers.functions.TNShelbyAO;
import ro.cst.tsearch.servers.functions.TNShelbyEP;
import ro.cst.tsearch.servers.functions.TNShelbyTR;
import ro.cst.tsearch.servers.functions.TNWilliamsonRO;
import ro.cst.tsearch.servers.functions.TNWilsonRO;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.KSJohnsonRO;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/* This file should contain general use GenericFunctions methods and those related only to TN, MI, KS, OH, KY, IL counties. 
Methods for new states (e.g. Florida, California) should be included in different GenericFunctions files.
*/ 
public class GenericFunctions1 {

    protected static final Category logger = Logger.getLogger(GenericFunctions1.class);

    public static final Pattern clayROLotPattern = Pattern.compile( "(?i)\\bLT\\s?S?\\b([^a-zA-Z]*)" );
    public static final Pattern clayROBlockPattern = Pattern.compile( "(?i)\\bBLK?S?\\s+([^\\s/]*)" );
    public static final String SIMPLE_CROSSREF = "0";
    public static final String PLAT_CROSSREF = "1";

    public static Pattern poBox = Pattern.compile("\\bP\\.?\\s*O\\.?\\s*BOX\\b");
    
    public static String nameTypeString = "\\b(TR|TRS|(?:CO-?\\s*)?T(?:(?:RU?)?S)?TEE?S?|(?:CO-?\\s*)?TR?TE?ES?|TRE)\\b";
    public static String nameOtherTypeString = "\\b(ET[\\s,;]*UX|ET[\\s,;]*AL|ET[\\s,;]*VIR)\\b";
    public static Pattern nameType = Pattern.compile("(?is)(.*?,?\\s*" + nameTypeString + ".*)");
    public static Pattern nameOtherType = Pattern.compile("(?is)(.*?,?\\s*" + nameOtherTypeString + ".*)");
    public static final Pattern SEC_TOW_RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+[\\dSNEW])-(\\d+[\\dSNEW])");
    public static final Pattern BK_PG_PATTERN = Pattern.compile("BK\\s+(\\d+)\\s+PG\\s+(\\d+)");
    
    public static String nameSuffixString = "\\b(JR|SR|DR|[IV]{2,}|[III]{2,}|\\d+(?:ST|ND|RD|TH))\\b";
    public static Pattern nameSuffix = Pattern.compile("(?is)(.*?),?\\s*"+nameSuffixString+".*");
    public static Pattern nameSuffix2 = Pattern.compile("(\\d+)(ST|ND|RD|TH)");
    public static Pattern nameSuffix3 = Pattern.compile(nameSuffixString);    
    
    public static final Pattern FWFL = Pattern.compile("^(\\w+\\s+(\\w+)?\\s*(" + GenericFunctions.nameSuffixString + "\\s*)?&\\s*\\w+\\s+(\\w+\\s+)?(\\w+\\s+)?)(\\w+)$");
    public static final Pattern FWFL1 = Pattern.compile("^\\w+\\s+(\\w{1}\\s)?" + GenericFunctions.nameSuffixString + "\\s*&\\s*(\\w+\\s+){2}\\w+$"); //EARL SR & CHARLIE ELLEN SMITH
    																																				//EARL J SR & CHARLIE ELLEN SMITH
    public static final Pattern FWFL2 = Pattern.compile("^\\w+\\s+(\\w{1})?\\s*&\\s*\\w+\\s+(\\w{1}\\s+)?(\\w{1}\\s+)?(\\w+)$"); //TERRANCE B & NANCY N WYATT
    																											//RICHARD & ANTIONETTE DULLAGHAN
    public static final Pattern FML = Pattern.compile("^\\w+(\\s+\\w+)?\\s+\\w+(\\s" + GenericFunctions.nameSuffixString + ")?$"); //PATRICIA HANNAN-SMITH
    public static final Pattern FMML = Pattern.compile("^\\w+\\s\\w+(\\s+\\w+)?\\s+\\w+(\\s" + GenericFunctions.nameSuffixString + ")?$"); //LOU ANN G TUZZA
    public static final Pattern FMiL = Pattern.compile("^(\\w+)\\s+(\\w{1})\\s+(\\w+)(\\s" + GenericFunctions.nameSuffixString + ")?$"); //CAROLYN H SMITH    																																	   
    public static final Pattern FMiLFind = Pattern.compile("^(\\w+)\\s+(\\w{1})\\s+(\\w+)(\\s" + GenericFunctions.nameSuffixString + ")?"); //CAROLYN H SMITH    																																	   

    public static final Pattern LLFM = Pattern.compile("^(\\w+)\\s(\\w+)\\s+(\\w+)(\\s+\\w{1})$");
    public static final Pattern LFMi = Pattern.compile("^(\\w+)\\s(\\w+)\\s+(\\w{1})$");
    
    public static final Pattern LFMWFWL1 = Pattern.compile("^\\w+\\s+\\w+(\\s+\\w+)?\\s*&\\s*\\w{1}\\s+(\\w{1}\\s+)?(\\w+)$"); //JOHNSON ALLEN M & J CAMPBELL
    public static final Pattern LFMWFWL2 = Pattern.compile("^\\w+\\s+\\w+(\\s+\\w{1})?(\\s+\\w{1})?\\s*&\\s*((\\w+)\\s+(\\w+\\s+)?)(\\w{2,})$"); //JOHNSON AMY R & ROBERT GRANT
    public static final Pattern LFMWFWL3 = Pattern.compile("^\\w+\\s+\\w+(\\s+\\w{1})?(\\s+\\w{1})?\\s*(" + GenericFunctions.nameSuffixString + ")?\\s*&\\s*((\\w+)\\s+(\\w+\\s+)?)(\\w+)$"); //SMITH GREG C & SHELLEY B
    public static final Pattern LFMWFWM = Pattern.compile("^\\w+\\s+\\w+(\\s+\\w{1})?(\\s+\\w{1})?\\s*&\\s*(\\w{2,})(\\s+\\w+)?$"); //JOHNSON AMY R & ROBERT GRANT
    
    public static final Pattern FMLFML = Pattern.compile("((\\w+)\\s+(\\w+)\\s+(\\w+))\\s+&\\s+((\\w+)\\s+(\\w+)\\s+(\\w+))");
    
    public static final String PLAT_BOOK_PAGE_PATTERN = "(?is)\\bPLAT\\s+B(?:OO)?K\\s+(\\d+)\\s*PA?GE?\\s+(\\d+)\\b";
    
    public static String sum(String s,long searchId) {
        if (s == null)
            return "0.00";
        s = s.replaceAll("\\(((?:\\d|\\.)+)\\)", "-$1");
        logger.info(s);
        String[] a = s.split("\\+");
        BigDecimal rez = ATSDecimalNumberFormat.zeroBD;
        for (int i = 0; i < a.length; i++) {
            if (a[i].length() == 0)
                continue;
            BigDecimal bd;
            try{
            	bd = new BigDecimal(a[i]);
            } catch (NumberFormatException e){   
            	bd = new BigDecimal(0.00);
            }
            rez = rez.add(bd);
        }
        return rez.toString();
    }
    
	public static void addIfNotAlreadyPresent(List<List> body, List line){
		Iterator<List> iter = body.iterator();
		while (iter.hasNext()) {
			List item = (List) iter.next();
			if (item.equals(line))
				return;
        }
		body.add(line);
	}

    //	Format :
    //	public static void <function_name> (ResultMap) throws Exception
	public static void parseNameARPulaskiTR(ResultMap m, long searchId) throws Exception {
		String name = (String) m.get("PropertyIdentificationSet.OwnerLastName");
		GenericFunctions2.parseName(m, name);
	}
	
    public static void taxRutherfordEP(ResultMap m,long searchId) throws Exception {
        
    	TNRutherfordEP.taxRutherfordYA(m, searchId);
      
    }
    public static void parseDeedInformation(ResultMap m, long searchId) throws Exception {
        
    	TNRutherfordEP.parseDeedInformationYA(m, searchId);
      
    }

    public static void taxDavidsonTR(ResultMap m,long searchId) throws Exception {// county    	

    	TNDavidsonTR.taxDavidsonTR(m, searchId);
    }
    
    public static void taxJohnsonTR(ResultMap m,long searchId) throws Exception {// county
        
    	String totalDue = sum((String) m.get("tmpTotalDue"), searchId);
    	m.put("TaxHistorySet.TotalDue", totalDue);
    	
    	String priorDelinquent = sum((String) m.get("tmpTotalDuePriorYears"), searchId); 
    	m.put("TaxHistorySet.PriorDelinquent", priorDelinquent);
    	  	
        String baseTaxDue = (String) m.get("TaxHistorySet.BaseAmount");
        //logger.info("basetaxdue: >" + baseTaxDue + "<");
        String paidAmount = (String) m.get("TaxHistorySet.AmountPaid");        
        String tmpCurrentYearDue = new BigDecimal(baseTaxDue).subtract(new BigDecimal(paidAmount)).toString();
        m.put("TaxHistorySet.CurrentYearDue", tmpCurrentYearDue);
    }
    

    public static void taxShelbyTR(ResultMap m,long searchId) throws Exception {//county
    	TNShelbyTR.taxShelbyTR(m, searchId);
    }

    public static void taxShelbyEP(ResultMap m,long searchId) throws Exception {//city
    	TNShelbyEP.taxShelbyEP(m, searchId);
    }

    public static void taxComputingMobileAO(ResultMap m,long searchId) throws Exception {
        String taxYear = (String) m.get("TaxHistorySet.Year");
        String payRecvYear = (String) m.get("tmpPayRecvYear");
        String baseAmount = (String) m.get("TaxHistorySet.BaseAmount");
        String amountPaid = (String) m.get("tmpPayRecvAmount");
        if (payRecvYear != null && payRecvYear.equals(taxYear)) {
            m.put("TaxHistorySet.TotalDue", new BigDecimal(baseAmount)
                    .subtract(new BigDecimal(amountPaid)).toString());
            m.put("TaxHistorySet.AmountPaid", amountPaid);
        } else {
            m.put("TaxHistorySet.TotalDue", baseAmount);
            m.put("TaxHistorySet.AmountPaid", "0");
        }
    }

    public static void taxKnoxvilleEP(ResultMap m,long searchId) throws Exception {
    	TNKnoxEP.taxKnoxvilleEP(m, searchId);
    }

    public static void taxJeffersonTR(ResultMap m,long searchId) throws Exception {
    	
        String discountAmount = (String) m.get("tmpDiscount");
        String grossAmount = (String) m.get("tmpGross");
        String penalty5Amount = (String) m.get("tmp5Penalty");
        String penalty10Amount = (String) m.get("tmp10Penalty");
        String penalty21Amount = (String) m.get("tmp21Penalty");
        String penalty101035Amount = (String) m.get("tmp101035Penalty");
        String amountPaid = (String) m.get("TaxHistorySet.AmountPaid");
        
        Date dateNow = new Date();
        Date lastPayDate  = new Date();
        String totalDue = (String) m.get("TaxHistorySet.TotalDue");
        
    	DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
    	boolean set = false;
    	
    	if (amountPaid == null || amountPaid.length() == 0 || "0".equals(amountPaid)){ // tax was not paid
    		String strDiscountDate = (String) m.get("tmpDiscountDate"); 
    		if (strDiscountDate != null && strDiscountDate.length() > 0) {
    			Date discountDate = df.parse(strDiscountDate);
	    		if ((dateNow.before(discountDate) || dateNow.equals(discountDate))&& discountAmount != null) {
	    			totalDue = discountAmount;
	    			set = true;
	    		} 
    		}
    		if (!set){
    			String strGrossDate = (String) m.get("tmpGrossDate");
    			if (strGrossDate != null && strGrossDate.length() > 0) {
	    			Date grossDate = df.parse(strGrossDate);
	    			
	    			//due date (last pay date before penalites will be applied)
        			lastPayDate = grossDate;
        			
		    		if ((dateNow.before(grossDate) || dateNow.equals(grossDate)) && grossAmount != null){
		    			totalDue = grossAmount;
		    			set = true;
		    		}
    			}
    		}
		    
    		if (!set){ // penalties appplied 
    			String strPenalty5Date = (String) m.get("tmp5PenaltyDate");
    			if (strPenalty5Date != null && strPenalty5Date.length() > 0){
	    			Date penalty5Date = df.parse(strPenalty5Date);        			
	    			if ((dateNow.before(penalty5Date) || dateNow.equals(penalty5Date)) && penalty5Amount != null){
	        			totalDue = penalty5Amount;
	        			set = true;
	        		} 
    			}
	    			
    			if (!set) {
        			String strPenalty10Date = (String) m.get("tmp10PenaltyDate");
	    			if (strPenalty10Date != null && strPenalty10Date.length() > 0){
		    			Date penalty10Date = df.parse(strPenalty10Date);
	        			if ((dateNow.before(penalty10Date) || dateNow.equals(penalty10Date)) && penalty10Amount != null){					        		
	        				totalDue = penalty10Amount;
		        		} else /*if ((dateNow.before(penalty21Date) || dateNow.equals(penalty21Date)) && penalty21Amount != null)*/{
		        			totalDue = penalty21Amount;
		        		}	        			
	    			}	
        		}   			
    			
    			if (!set) {
        			String strPenalty101035Date = (String) m.get("tmp101035PenaltyDate");
	    			if (strPenalty101035Date != null && strPenalty101035Date.length() > 0){
		    			Date penalty101035Date = df.parse(strPenalty101035Date);
	        			if ((dateNow.before(penalty101035Date) || dateNow.equals(penalty101035Date)) && penalty101035Date != null){					        		
	        				totalDue = penalty101035Amount;
		        		} else /*if ((dateNow.before(penalty21Date) || dateNow.equals(penalty21Date)) && penalty21Amount != null)*/{
		        			totalDue = (String) m.get("TaxHistorySet.TotalDue");
		        		}	        			
	    			}	
        		}
    		}
	    	
    		m.put("TaxHistorySet.AmountPaid", "0");
    		m.put("TaxHistorySet.DatePaid", "");    		
    	} else {
    		String receiptDate = (String) m.get("TaxHistorySet.DatePaid");
    		String crtTaxYear = (String) m.get("TaxHistorySet.Year");
			if (!StringUtils.isEmpty(receiptDate)) {
				ResultTable newRT = new ResultTable();
				List<String> line = new ArrayList<String>();
					line.add(crtTaxYear);
					line.add(amountPaid);
					line.add(receiptDate);

				if (!line.isEmpty()) {
					List<List> bodyRT = new ArrayList<List>();
					bodyRT.add(line);
					String[] header = {"Year", "ReceiptAmount", "ReceiptDate"};
					Map<String,String[]> map = new HashMap<String,String[]>();
					map.put("ReceiptAmount", new String[]{"ReceiptAmount", ""});
					map.put("ReceiptDate", new String[]{"ReceiptDate", ""});
					newRT.setHead(header);
					newRT.setMap(map);
					newRT.setBody(bodyRT);
					newRT.setReadOnly();
					m.put("TaxHistorySet", newRT);
				}
			}
    	}
        
    	if (totalDue != null){
	        m.put("TaxHistorySet.TotalDue", totalDue);
	        m.put("TaxHistorySet.CurrentYearDue", totalDue);
    	}
        m.put("TaxHistorySet.PriorDelinquent", "0.00"); 	// no delinquent amount info for prior years on this site
    }
    
   public static void taxMIMacombTR(ResultMap m, long searchId) throws Exception {
	   
	   ResultTable delinqTable = (ResultTable) m.get("tmpDelinqTable");
	   if (delinqTable == null)
		   return;
	   
	   String[][] body = delinqTable.body;
	   int len = body.length;
	   BigDecimal totalDelinq = new BigDecimal(0);	// delinquent due for all years
	   BigDecimal priorDelinq = new BigDecimal(0); 	// delinquent due for prior years 
	   BigDecimal totalDue = new BigDecimal(0);		// delinquent due for last displayed tax year  
	   String currentTaxYear = "";
	   
	   for (int i=0; i<len; i++){		   
		   if (currentTaxYear.compareTo(body[i][0]) < 0){
			   currentTaxYear = body[i][0];
			   totalDue = new BigDecimal(body[i][1]);
		   }
		   totalDelinq = totalDelinq.add(new BigDecimal(body[i][1]));
	   }
	   
	   if (len > 0){
		   priorDelinq = totalDelinq.subtract(totalDue);
		   m.put("TaxHistorySet.Year", currentTaxYear);	   
		   m.put("TaxHistorySet.PriorDelinquent", priorDelinq.toString());
		   m.put("TaxHistorySet.DelinquentAmount", totalDelinq.toString());
		   m.put("TaxHistorySet.TotalDue", totalDue.toString()); // total due on  currentTaxYear 
		   //it may be possible the real current tax year to be other than currentTaxYear, because in case it is not yet delinquent, it is not recorded on site 
		   m.put("TaxHistorySet.CurrentYearDue", totalDue.toString());
	   }
   }
   
   public static void taxMIWayneTR(ResultMap m, long searchId) throws Exception {
	   
	   String totalDelinq = (String) m.get("TaxHistorySet.DelinquentAmount");
	   if (totalDelinq == null || totalDelinq.length() == 0)
		   return;
	   
	   String lastDelinq = (String) m.get("TaxHistorySet.TotalDue");
	   String lastBaseAmount = (String) m.get("TaxHistorySet.BaseAmount");
	   String lastPenalty = (String) m.get("TaxHistorySet.PenaltyCurrentYear");
	   
	   if (lastDelinq == null || lastDelinq.length() == 0){
		   lastDelinq = "0.00";
	   }
	   
	   if (lastBaseAmount == null || lastBaseAmount.length() == 0){
		   lastBaseAmount = "0.00";
	   }
	   
	   if (lastPenalty == null || lastPenalty.length() == 0){
		   lastPenalty = "0.00";
	   }
	     
	   String priorDelinq = new BigDecimal(totalDelinq).subtract(new BigDecimal(lastDelinq)).toString();
	   String amountPaid = new BigDecimal(lastBaseAmount).add(new BigDecimal(lastPenalty)).subtract(new BigDecimal(lastDelinq)).toString();
	   
	   m.put("TaxHistorySet.PriorDelinquent", priorDelinq);
	   m.put("TaxHistorySet.AmountPaid", amountPaid);	   
	   m.put("TaxHistorySet.CurrentYearDue", lastDelinq);
   }
   
   public static void removeDuplicateElements(String vect[][], int poz1, int poz2, int l) throws Exception
   {
	   int len = vect.length;
	   if (vect == null || len == 0)
		   return;
	   for (int i=0; i<len-1; i++)
	   {  
		   for (int j=i+1; j<len; j++)
		      if ((vect[i][poz1].equals(vect[j][poz1])) && (vect[i][poz2].equals(vect[j][poz2])))
		      {
		    	  for (int k=j; k<len-1; k++)
		    	  {
		    		  for (int ii=0; ii<l; ii++)
		    			  vect[k][ii] = vect[k+1][ii];
		    	  }
		          if (j == len-1)
		        	  for (int ii=0; ii<=l; ii++)
		    			  vect[j][ii] = "";
		    	  len --;
		      }
	   }	   
   }
   
   public static void bookPageTNWilsonAO(ResultMap m,long searchId) throws Exception {
	   TNGenericAO.bookPageTNWilsonAO(m, searchId);
   }
   
    public static void stdPisNashvilleAO(ResultMap m,long searchId) throws Exception {
        String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
        String[] a = StringFormats.parseNameNashville(s);
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
    }
    
    public static void stdShelbyDN(ResultMap m,long searchId) throws Exception {
    	m.put("GrantorSet.OwnerFirstName", m.get("PropertyIdentificationSet.OwnerFirstName"));
        m.put("GrantorSet.OwnerMiddleName", m.get("PropertyIdentificationSet.OwnerMiddleName"));
        m.put("GrantorSet.OwnerLastName", m.get("PropertyIdentificationSet.OwnerLastName"));
        m.put("GrantorSet.SpouseFirstName", m.get("PropertyIdentificationSet.SpouseFirstName"));
        m.put("GrantorSet.SpouseMiddleName", m.get("PropertyIdentificationSet.SpouseMiddleName"));
        m.put("GrantorSet.SpouseLastName", m.get("PropertyIdentificationSet.SpouseLastName"));
    }

    public static void correctZIPCode(ResultMap m,long searchId) throws Exception {
    	String zip = (String) m.get("PropertyIdentificationSet.Zip");
    	final Pattern PAT_WRONG_ZIP = Pattern.compile("([0-9]{5})-([0-9]{1,3})");
    	
     	if(zip!=null){
    		zip = zip.trim().replaceAll("\\s+", "");
    		Matcher mat = PAT_WRONG_ZIP.matcher(zip);
    		if(zip.matches(PAT_WRONG_ZIP.toString()))
	    		if(mat.find()){
	    			String first = mat.group(1);
	    			String last = mat.group(2);
	    			last = "0000".substring(last.length())+last;
	    			zip = first + "-" + last;
	    			zip = zip.replace("-0000", "");
	    			m.put("PropertyIdentificationSet.Zip", zip);
	    		} 
    	}
    }
    
    @SuppressWarnings("rawtypes")
	public static void stdPisWilliamsonAO(ResultMap m,long searchId) throws Exception {
    	/* set acres information */
        String acres = (String)m.get( "tmpCAcres" );
        if( acres == null || "".equals( acres ) ){
        	acres = (String)m.get( "tmpDAcres" );	
        }
        
        if( acres != null ){
        	m.put( "PropertyIdentificationSet.Acres" , acres);
        }
        
    	String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
        if(s == null || s.length() == 0)
        	return;
        
        s = s.replaceAll("(?is)\\bETU\\b", "ETUX");
        
        String[] suffixes, type, otherType;
        List<List> body = new ArrayList<List>();
        
        String[] a = StringFormats.parseNameNashville(s, true);
        
        type = GenericFunctions.extractAllNamesType(a);
		otherType = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractNameSuffixes(a);        
        GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType,
        								NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
	
        GenericFunctions.storeOwnerInPartyNames(m, body, true);
        
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
        

    }
    
    public static void partyNamesILDuPageROinter(ResultMap m,long searchId) throws Exception {
    	ILDuPageRO.partyNamesILDuPageROinter(m, searchId);
    }
    
    public static void partyNamesILDuPageRO(ResultMap m,long searchId) throws Exception {
    	ILDuPageRO.partyNamesILDuPageRO(m, searchId);
    }
    
    public static void reparseDocTypeTNWilliamsonRO(ResultMap m, long searchId) throws Exception {
    	TNWilliamsonRO.reparseDocTypeTNWilliamsonRO(m, searchId);//B 4423
    }
    
    public static void partyNamesTNHamiltonTR(ResultMap m,long searchId) throws Exception {
	     
    	String owner = (String) m.get("PropertyIdentificationSet.OwnerLastName");
		   
		if (StringUtils.isEmpty(owner))
		   return;
	
		TNHamiltonTR.partyNamesTNHamiltonTR(m, searchId);
    }
    
    public static void partyNamesKYJeffersonAO(ResultMap m, long searchId) throws Exception 
    {
 	   String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
 	   String s1 = "", s2 = "";
 	   	
	   	if(s == null || s.length() == 0)
	       	return;
	   	
	   	s = s.replaceAll("\\s+(\\w{1,2})\\s+(\\w+(?:\\s+\\w+)?\\s+\\w)\\s*$", " $1 & $2 ");//GOKE KEVIN R BLANCHARD MEGAN A

	   	//### B 5121 testcase 6)
	   	//s = s.replaceAll("\\s+([A-Z])\\w+\\.{3}\\s*$", " $1");
	   	s = s.replaceAll("\\.{3}\\s*$", "");
	   	//###
	   	
		s = s.replaceAll("\\bDBA\\b", " & ");
		
	   	s = s.replaceAll("(?:&\\s*)?ET AL", "\\s+ETAL\\s+");
	   	s = s.replaceAll("\\s*&\\s*ETAL", "\\s+ETAL\\s+");
	
		s = s.replaceAll("\\bAND\\b","&");
		s = s.replaceAll("&\\s{2}","&");
		s = s.replaceAll("\\s{3,}", " & ");
		
		s = s.replaceFirst("PATRICAI", "PATRICIA");  
		s = s.replaceAll("(.*Y)\\s(YADEN.*)","$1 & $2");
		s = s.replaceFirst("SMITH RANDALL","& SMITH RANDALL");  // 105008610000  ->  SMITH HAROLD E & MARILYN SMITH RANDALL & DONNA  (intre MARILYN si SMITH lipseste ceva sa desparta) !!!
		s = s.replaceFirst("KATRINA BREEDEN","KATRINA & BREEDEN");// 003H00880000  ->  SMITH KATRINA   BREEDEN DANNY  (lipseste "&" dintre Owneri)
		
		if (s.contains("C/O"))
		{
			int start_pos = s.indexOf("C/O")+3;
			s1 = s.substring(s.lastIndexOf(' '));
			s2 = s.substring(start_pos,s.lastIndexOf(' '));
			if (s.contains("INC"))
				s = s.replaceFirst("C/O ","&");
			else
				s = s.replaceFirst("C/O\\s*(.*)","& " + s1 + s2);	
		}

	   	if (!(s.contains("ETAL")) && s.contains(" AND ")) {
	   		s = s.replaceAll(" AND ", " & ");
	   	}

	   	
	   	String[] owners ;
	   	String[] own = null;
	   	owners = s.split("&");
	   	//if (s.trim().matches("\\w+\\s*&\\s*\\w+\\s+\\w+\\s*&\\s*\\w+\\s+\\w+")){
	   		if (owners.length == 3){
	   			if(NameUtils.isCompany(owners[1]) && NameUtils.isCompany(owners[2])){
	   				owners = s.split("@@@@@");
	   			}
	   		} else if (owners.length == 2){
	   			if(NameUtils.isCompany(owners[0]) && NameUtils.isCompany(owners[1])){
	   				owners = s.split("@@@@@");
	   			}
	   		}
	   	//}
	   		
	   		//22003910900000: DAWSON & JOHNSON LLC, 21299300380000
	   		if (org.apache.commons.lang.StringUtils.countMatches(s, "&") == 1 && NameUtils.isCompany(s)
	   				&& s.matches("(?is)\\w+\\s+&\\s+.*")){
	   			owners = s.split("@@@@@");
	   		}
	  
			List<List> body = new ArrayList<List>();
			String[] names = {"", "", "", "", "", ""};
			String[] suffixes, type, otherType;
			
			String ln="";
			String prev_ow_last_name = "";
			for (int i=0; i<owners.length; i++)
			{
				String next_ow = "" ;
				Pattern pa;
				Matcher ma;
				String ow = owners[i];
				
				/*  situatii posibile pt persoana fara LAST NAME
				 * SMITH HAROLD E TR & ELIZABETH D TR   
				 * SMITH ALBERT A &   SMITH TERRY K & C L
				 * SMITH JEFFREY C & ANNE STEINBOCK SMITH
				 * SMITH JOHN R & MARY B & SUPERIOR BUILDERS INC
				 */
				if (i < owners.length-1) next_ow = owners[i+1];

				if (i == 0){
					names = StringFormats.parseNameNashville(ow, true);
					ln = names[2];
					
				}  	
				if (i > 0) {
						names = StringFormats.parseNameNashville(ow, true);
						//names[2] = names[2].replaceFirst("SAVELLA - TERRANCE", "SAVELLA & TERRANCE");
					try {
						for (int j = 0; j < own.length; j++) {
							ow = own[j];
							if (j == 0) {
								names = StringFormats.parseNameNashville(ow, true);
							} else {
								String[] name = StringFormats.parseNameNashville(ow, true);
								names[3] = name[0];
								names[4] = name[1];
								names[5] = name[2];
							}
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
					
					if (LastNameUtils.isNotLastName(names[2]) && NameUtils.isNotCompany(names[2])){
						names = StringFormats.parseNameDesotoRO(ow, true);
					}
					
					if (ow.trim().matches("\\w+\\s+\\w+") && LastNameUtils.isNotLastName(names[2])){
						names[1] = names[0];
						names[0] = names[2];
						names[2] = ln;
					}

				}
				pa = Pattern.compile("\\bTR\\b");
				ma = pa.matcher(names[2]);
				if (i != owners.length-1)
				{
				if (ma.find())
					prev_ow_last_name = names[2].substring(0,names[2].indexOf(' '));
				else
					prev_ow_last_name = names[2];
				}
				if (next_ow != "")
				{
					if (next_ow.contains(prev_ow_last_name))  //e.g.: PID:062800370000  ->  SMITH JEFFREY C & ANNE STEINBOCK SMITH
					{
						//prev_ow_last_name = names[2];
						next_ow = next_ow.replaceFirst("([A-Z\\s]+)\\s" + prev_ow_last_name, prev_ow_last_name + "$1");
						owners[i+1] = next_ow;
					}
					else
					{
						pa = Pattern.compile("[A-Z]+\\s[A-Z]\\sTR");  // cazul: SMITH HAROLD E TR & ELIZABETH D TR   cand la ELIZABETH Last name ar trebui sa fie SMITH
						ma = pa.matcher(next_ow);
						if (ma.find())
						{
							next_ow = prev_ow_last_name + next_ow;
							owners[i+1] = next_ow;							
						}
						else 
						{
						pa = Pattern.compile("[A-Z]+\\s[A-Z][A-Z]+(?:\\s*([A-Z]|[A-Z]+))?");  // SMITH EDWARD JR & MITTEL MARIE H & STACHURA CARLA MITTEL & PATRICAI M
						ma =pa.matcher(next_ow);
						if (!ma.find())
						{
							pa = Pattern.compile("\\A\\s*[A-Z]+(:?\\s[A-Z])?\\s*(:?TR|III|JR|)?\\s*\\Z");
							ma = pa.matcher(next_ow);
							if (ma.find()) 
							{
								//prev_ow_last_name = names[2];
								next_ow = prev_ow_last_name + next_ow;
								owners[i+1] = next_ow;
							}
							
						}
						}
					}
				}	
				else //suntem pe ultimul owner si putem avea un caz de genul: SMITH HAROLD E TR & ELIZABETH D TR    !!! inca nu ai terminat
				{
					pa = Pattern.compile("\\A\\s*[A-Z]+\\s[A-Z]\\s*(:?TR|III|JR|)?\\s*\\Z");
					ma = pa.matcher(next_ow);
					if (ma.find()) 
					{
						owners[i] = prev_ow_last_name + owners[i];
					}
				}
				
				suffixes = extractNameSuffixes(names);
				type = extractAllNamesType(names);
				otherType = extractAllNamesOtherType(names);
		        addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
		        		NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
			storeOwnerInPartyNames(m, body, true);    	
    }
    
    public static void partyNamesTNShelbyAO(ResultMap m, long searchId) throws Exception {
    	
		String s = (String) m.get("tmpOwnerFullName");
		String s1 = (String) m.get("tmpInCareOf");
		if (StringUtils.isNotEmpty(s1))
			s += " AND " + s1;
		if (StringUtils.isEmpty(s))
			return;
		TNShelbyAO.partyNamesTokenizerTNShelbyAO(m, s);
    }
     
    protected static void addOwnerNames (String names[], String suffixOwner, String suffixCoowner, List<List> body){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>(); 
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	line.add(suffixOwner);
        	body.add(line);
        }
        if(names[5].length() != 0){
        	line = new ArrayList<String>();
        	line.add(names[3]);
        	line.add(names[4]);
        	line.add(names[5]);
        	line.add(suffixCoowner);
        	body.add(line);
        }                 
    }
    
    public static List<List> addOwnerNames (String names[], String suffix, boolean isCompany, 
    		List<List> body){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>(); 
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	line.add(suffix);
        	line.add(isCompany ? "yes" :  "");
        	body.add(line);
        }
        return body;
    }    
 
    public static void addOwnerNames (String names[], String suffixOwner, String suffixCoowner, boolean ownerIsCompany, 
    		boolean spouseIsCompany, List<List> body, String[] maiden){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>(); 
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	line.add(suffixOwner);
        	line.add(ownerIsCompany ? "yes" :  "");
        	line.add(maiden[0]);
        	body.add(line);
        }
        if(names[5].length() != 0){
        	line = new ArrayList<String>();
        	line.add(names[3]);
        	line.add(names[4]);
        	line.add(names[5]);
        	line.add(suffixCoowner);
        	line.add(spouseIsCompany ? "yes" :  "");
        	line.add(maiden[1]);
        	body.add(line);
        }                 
    }    
    
    public static void addOwnerNames(String all, String names[], List<List> body){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>(); 
        	line.add(all);
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	body.add(line);
        }
        if(names[5].length() != 0){
        	line = new ArrayList<String>();
        	line.add(all);
        	line.add(names[3]);
        	line.add(names[4]);
        	line.add(names[5]);
        	body.add(line);
        }                 
    }
    
    public static void addOwnerNames(String all, String names[], String suffixOwner, String suffixCoowner, String[] type, 
    									String[] otherType, boolean ownerIsCompany,	boolean spouseIsCompany, List<List> body){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>();
        	line.add(all);
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	line.add(suffixOwner);
        	line.add(type[0]);
        	line.add(otherType[0]);
        	line.add(ownerIsCompany ? "yes" :  "");
        	body.add(line);
        }
        if(names[5].length() != 0){
        	line = new ArrayList<String>();
        	line.add(all);
        	line.add(names[3]);
        	line.add(names[4]);
        	line.add(names[5]);
        	line.add(suffixCoowner);
        	line.add(type[1]);
        	line.add(otherType[1]);
        	line.add(spouseIsCompany ? "yes" :  "");
        	body.add(line);
        }                 
    }
    
    public static void addOwnerNames (String names[], String suffixOwner, String suffixCoowner, boolean ownerIsCompany, 
    		boolean spouseIsCompany, List<List> body){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>(); 
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	line.add(suffixOwner);
        	line.add(ownerIsCompany ? "yes" :  "");
        	body.add(line);
        }
        if(names[5].length() != 0){
        	line = new ArrayList<String>();
        	line.add(names[3]);
        	line.add(names[4]);
        	line.add(names[5]);
        	line.add(suffixCoowner);
        	line.add(spouseIsCompany ? "yes" :  "");
        	body.add(line);
        }                 
    }
    
    public static void addOwnerNames (String names[], String suffixOwner, String suffixCoowner, String[] type, 
    		String[] otherType, boolean ownerIsCompany,	boolean spouseIsCompany, List<List> body){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>(); 
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	line.add(suffixOwner);
        	line.add(type[0]);
        	line.add(otherType[0]);
        	line.add(ownerIsCompany ? "yes" :  "");
        	body.add(line);
        }
        if(names[5].length() != 0){
        	line = new ArrayList<String>();
        	line.add(names[3]);
        	line.add(names[4]);
        	line.add(names[5]);
        	line.add(suffixCoowner);
        	line.add(type[1]);
        	line.add(otherType[1]);
        	line.add(spouseIsCompany ? "yes" :  "");
        	body.add(line);
        }                 
    }
    
    public static void addOwnerNames(String names[], List<List> body){
    	
        List<String> line;       
        if (names[2].length() != 0){
        	line = new ArrayList<String>(); 
        	line.add(names[0]);
        	line.add(names[1]);
        	line.add(names[2]);
        	body.add(line);
        }            
    }
        
    public static void storeOwnerInPartyNames(ResultMap m, List<List> body)  throws Exception {
    	 if (!body.isEmpty()){
 	        String [] header;
 	        
 			Map<String,String[]> map = new HashMap<String,String[]>();
 			map.put("FirstName", new String[]{"FirstName", ""});
 			map.put("MiddleName", new String[]{"MiddleName", ""});
 			map.put("LastName", new String[]{"LastName", ""});
 			map.put("Suffix", new String[]{"Suffix", ""});
 			if (body.get(0).size() == 6){
 				map.put("isCompany", new String[]{"isCompany", ""});
 				map.put("MaidenName", new String[]{"MaidenName", ""});
 				header = new String[6];
 				header[4] = "isCompany";
 				header[5] = "MaidenName";
 			} else if (body.get(0).size() == 5){
 				map.put("isCompany", new String[]{"isCompany", ""});
 				header = new String[5];
 				header[4] = "isCompany";
 			} else {
 				header = new String[4];
 			}
 			header[0] = "FirstName";
 			header[1] = "MiddleName";
 			header[2] = "LastName";
 			header[3] = "Suffix";
 			
 			ResultTable parties = new ResultTable();	
 			parties.setHead(header);
 			parties.setBody(body);
 			parties.setMap(map);
 			m.put("PartyNameSet", parties);
         }  
    }
    
    public static void storeOwnerInPartyNames(ResultMap m, List<List> body, boolean withType)  throws Exception {
   	 if (!body.isEmpty()){
	        String [] header;
	        
			Map<String,String[]> map = new HashMap<String,String[]>();
			map.put("FirstName", new String[]{"FirstName", ""});
			map.put("MiddleName", new String[]{"MiddleName", ""});
			map.put("LastName", new String[]{"LastName", ""});
			map.put("Suffix", new String[]{"Suffix", ""});
			map.put("Type", new String[]{"Type", ""});
			map.put("OtherType", new String[]{"OtherType", ""});
			if (body.get(0).size() == 8){
				map.put("isCompany", new String[]{"isCompany", ""});
				map.put("MaidenName", new String[]{"MaidenName", ""});
				header = new String[6];
				header[6] = "isCompany";
				header[7] = "MaidenName";
			} else if (body.get(0).size() == 7){
				map.put("isCompany", new String[]{"isCompany", ""});
				header = new String[7];
				header[6] = "isCompany";
			} else {
				header = new String[6];
			}
			header[0] = "FirstName";
			header[1] = "MiddleName";
			header[2] = "LastName";
			header[3] = "Suffix";
			header[4] = "Type";
			header[5] = "OtherType";
			
			ResultTable parties = new ResultTable();	
			parties.setHead(header);
			parties.setBody(body);
			parties.setMap(map);
			m.put("PartyNameSet", parties);
        }  
   }
    
    public static ResultTable storeOwnerInSet(ArrayList<List> names) throws Exception {
    	
    	ResultTable rt = new ResultTable();
    	
    	String[] header = {"all", "OwnerFirstName", "OwnerMiddleName", "OwnerLastName", "SpouseFirstName", "SpouseMiddleName", "SpouseLastName", "isLander"};
    	Map<String,String[]> map = new HashMap<String,String[]>();
    	map.put("all", new String[]{"all", ""});
		map.put("OwnerFirstName", new String[]{"OwnerFirstName", ""});
		map.put("OwnerMiddleName", new String[]{"OwnerMiddleName", ""});
		map.put("OwnerLastName", new String[]{"OwnerLastName", ""});
 		map.put("SpouseFirstName", new String[]{"SpouseFirstName", ""});
 		map.put("SpouseMiddleName", new String[]{"SpouseMiddleName", ""});
 		map.put("SpouseLastName", new String[]{"SpouseLastName", ""});
 		map.put("isLander", new String[]{"isLander", ""});
 		String[][] body = new String[names.size()][header.length];
    	for (int i = 0; i < names.size(); i++){
    				body[i][0] = (String)names.get(i).get(0);
    				body[i][1] = (String)names.get(i).get(1);
    				body[i][2] = (String)names.get(i).get(2);
    				body[i][3] = (String)names.get(i).get(3);
    				body[i][4] = "";
    				body[i][5] = "";
    				body[i][6] = "";
    				if(names.get(i).size()>4) {
    					body[i][7] = (String)names.get(i).get(4);;
    				} else {
    					body[i][7] = "";
    				}
    	}
    	rt.setReadWrite();
    	rt.setHead(header);
    	rt.setMap(map);
    	rt.setBody(body);
    	rt.setReadOnly();
    	return rt;
    }
    
    public static ResultTable storeOwnerInSet(ArrayList<List> names, boolean withType) throws Exception {
    	
    	ResultTable rt = new ResultTable();
    	
    	String[] header = {"all", "OwnerFirstName", "OwnerMiddleName", "OwnerLastName", "Suffix", "Type", "OtherType", "isCompany", "isLander"};
    	Map<String,String[]> map = new HashMap<String,String[]>();
    	map.put("all", new String[]{"all", ""});
		map.put("OwnerFirstName", new String[]{"OwnerFirstName", ""});
		map.put("OwnerMiddleName", new String[]{"OwnerMiddleName", ""});
		map.put("OwnerLastName", new String[]{"OwnerLastName", ""});
 		map.put("Suffix", new String[]{"Suffix", ""});
 		map.put("Type", new String[]{"Type", ""});
 		map.put("OtherType", new String[]{"OtherType", ""});
 		map.put("isCompany", new String[]{"isCompany", ""});
 		map.put("isLander", new String[]{"isLander", ""});
 		String[][] body = new String[names.size()][header.length];
    	for (int i = 0; i < names.size(); i++){
    				body[i][0] = (String)names.get(i).get(0);
    				body[i][1] = (String)names.get(i).get(1);
    				body[i][2] = (String)names.get(i).get(2);
    				body[i][3] = (String)names.get(i).get(3);
    				body[i][4] = (String)names.get(i).get(4);
    				body[i][5] = (String)names.get(i).get(5);
    				body[i][6] = (String)names.get(i).get(6);
    				body[i][7] = (String)names.get(i).get(7);
    				if (names.get(i).size() > 8){
    					body[i][8] = (String)names.get(i).get(8);
    				} else {
    					body[i][8] = "";
    				}
    	}
    	rt.setReadWrite();
    	rt.setHead(header);
    	rt.setMap(map);
    	rt.setBody(body);
    	rt.setReadOnly();
    	return rt;
    }

    public static void storeOwnerInPartyNames(ResultMap m, List<List> body, String key)  throws Exception {
   	 if (!body.isEmpty()){
	        String [] header;
	        
			Map<String,String[]> map = new HashMap<String,String[]>();
			map.put("FirstName", new String[]{"FirstName", ""});
			map.put("MiddleName", new String[]{"MiddleName", ""});
			map.put("LastName", new String[]{"LastName", ""});
			map.put("Suffix", new String[]{"Suffix", ""});
			if (body.get(0).size() == 6){
				map.put("isCompany", new String[]{"isCompany", ""});
				map.put("MaidenName", new String[]{"MaidenName", ""});
				header = new String[6];
				header[4] = "isCompany";
				header[5] = "MaidenName";
			} else if (body.get(0).size() == 5){
				map.put("isCompany", new String[]{"isCompany", ""});
				header = new String[5];
				header[4] = "isCompany";
			} else {
				header = new String[4];
			}
			header[0] = "FirstName";
			header[1] = "MiddleName";
			header[2] = "LastName";
			header[3] = "Suffix";
			
			ResultTable parties = new ResultTable();	
			parties.setHead(header);
			parties.setBody(body);
			parties.setMap(map);
			m.put(key, parties);
        }  
   }
    
    /**
     * check if a name particle is a suffix
     *@param  name
     *@return boolean
     */ 
    public static boolean isSuffix(String nameParticle){
    	Matcher ma = nameSuffix3.matcher(nameParticle);
        if (ma.matches()){
        	return true;
        } 
       
        return false;
    }
    
    /** extract the suffix from a name; returns a String vector with 2 elements, last contains the suffix, first contains the rest*/ 
    public static String[] extractSuffix(String name){
    	String[] names = {"", ""};
    	Matcher ma = nameSuffix.matcher(name);
    	Matcher ma2;
        if (ma.matches()){
        	names[0] = ma.group(0).replaceAll("\\b" + ma.group(2) + "\\b", " ").replaceAll("\\s{2,}", " ").trim();
        	String suff = ma.group(2);
        	ma2 = nameSuffix2.matcher(suff);
        	if (ma2.matches()){
        		suff = Roman.arabicToRoman(ma2.group(1));
        	}
        	names[1] = suff;
        } else {
        	names[0] = name;
        }
        return names;
    }
    
    public static String[] extractNameSuffixes(String names[]){
    	String[] suffix = {"", ""};
    	if (names == null || names.length <6)
    		return suffix;
    	
    	// extract from middle
    	String[] n = extractSuffix(names[1]);
    	names[1] = n[0];
    	suffix[0] = n[1];

    	n = extractSuffix(names[4]);
    	names[4] = n[0];
    	suffix[1] = n[1]; 

    	// if no suffix was found in middle, try to extract it from last
    	if (suffix[0].length() == 0 && suffix[1].length() == 0){
    		if (NameUtils.isNotCompany(names[2])){
		    	n = extractSuffix(names[2]);
		    	names[2] = n[0];
		    	suffix[0] = n[1];
    		}
	
    		if (NameUtils.isNotCompany(names[5])){
		    	n = extractSuffix(names[5]);
		    	names[5] = n[0];
		    	suffix[1] = n[1]; 
    		}
    	}    	
        return suffix;
    }
    
    public static String[] extractAllNamesSufixes(String[] names){
    	String[] suffix = extractNameSuffixes(names);
    	// if no suffix was found in middle and last, try to extract it from first
    	if (suffix[0].length() == 0){
	    	String[] n = extractSuffix(names[0]);
	    	names[0] = n[0];
	    	suffix[0] = n[1];
	    	if (names[0].length() == 0){
	    		names[0] = names[1];
	    		names[1] = "";
	    	}
    	}
    	if(suffix[1].length() == 0){
	    	String[] n = extractSuffix(names[3]);
	    	names[3] = n[0];
	    	suffix[1] = n[1];
	    	if (names[3].length() == 0){
	    		names[3] = names[4];
	    		names[4] = "";
	    	}
    	}    	
        return suffix;    	
    }
    
    /** extract the type(all forms of TRUSTEE) from a name; returns a String vector with 2 elements, last contains the type, first contains the rest*/ 
    public static String[] extractType(String name){
    	String[] names = {"", ""};
    	Matcher ma = nameType.matcher(name);
        if (ma.matches()){
        	names[0] = ma.group(0).replaceAll("\\b" + ma.group(2) + "\\b", " ").replaceAll("\\s{2,}", " ").trim();
        	String type = ma.group(2);
        	names[1] = type.trim();
        } else {
        	names[0] = name;
        }
        return names;
    }
    
    public static String[] extractNameType(String names[]){
    	String[] type = {"", ""};
    	if (names == null || names.length <6)
    		return type;
    	
    	// extract from middle
    	String[] n = extractType(names[1]);
    	names[1] = n[0];
    	type[0] = n[1];

    	n = extractType(names[4]);
    	names[4] = n[0];
    	type[1] = n[1]; 

    	// if no type was found in middle, try to extract it from last
    	if (type[0].length() == 0 && type[1].length() == 0){
	    	n = extractType(names[2]);
	    	names[2] = n[0];
	    	type[0] = n[1];
	
	    	n = extractType(names[5]);
	    	names[5] = n[0];
	    	type[1] = n[1]; 
    	}
    	if (type[0].matches(".*(TRU?STEES|TRS|TTEES).*") || type[1].matches(".*(TRU?STEES|TRS|TTEES).*")) {
    		type[0] = "TR";
    		type[1] = "TR";
    	}
        return type;
    }
    
    public static String[] extractAllNamesType(String[] names){
    	String[] type = extractNameType(names);
    	// if no type was found in middle and last, try to extract it from first
    	if (type[0].length() == 0){
	    	String[] n = extractType(names[0]);
	    	names[0] = n[0];
	    	type[0] = n[1];
	    	if (names[0].length() == 0){
	    		names[0] = names[1];
	    		names[1] = "";
	    	}
    	}
    	if(type[1].length() == 0){
	    	String[] n = extractType(names[3]);
	    	names[3] = n[0];
	    	type[1] = n[1];
	    	if (names[3].length() == 0){
	    		names[3] = names[4];
	    		names[4] = "";
	    	}
    	}    	
        return type;    	
    }
    
    /** extract the otherType(ETUX, ETAL, ETVIR etc) from a name; returns a String vector with 2 elements, last contains the otherType, first contains the rest*/ 
    public static String[] extractOtherType(String name){
    	String[] names = {"", ""};
    	Matcher ma = nameOtherType.matcher(name);
        if (ma.matches()){
        	names[0] = ma.group(0).replaceAll("\\b" + ma.group(2) + "\\b", " ").replaceAll("\\s{2,}", " ").trim();
        	String type = ma.group(2);
        	names[1] = type.trim();
        } else {
        	names[0] = name;
        }
        return names;
    }
    
    public static String[] extractNameOtherType(String names[]){
    	String[] otherType = {"", ""};
    	if (names == null || names.length <6)
    		return otherType;
    	
    	// extract from middle
    	String[] n = extractOtherType(names[1]);
    	names[1] = n[0];
    	otherType[0] = n[1];

    	n = extractOtherType(names[4]);
    	names[4] = n[0];
    	otherType[1] = n[1]; 

    	// if no otherType was found in middle, try to extract it from last
    	if (otherType[0].length() == 0 && otherType[1].length() == 0){
	    	n = extractOtherType(names[2]);
	    	names[2] = n[0];
	    	otherType[0] = n[1];
	
	    	n = extractOtherType(names[5]);
	    	names[5] = n[0];
	    	otherType[1] = n[1]; 
    	}    	
        return otherType;
    }
    
    public static String[] extractAllNamesOtherType(String[] names){
    	String[] otherType = extractNameOtherType(names);
    	// if no otherType was found in middle and last, try to extract it from first
    	if (otherType[0].length() == 0){
	    	String[] n = extractOtherType(names[0]);
	    	names[0] = n[0];
	    	otherType[0] = n[1];
	    	if (names[0].length() == 0){
	    		names[0] = names[1];
	    		names[1] = "";
	    	}
    	}
    	if(otherType[1].length() == 0){
	    	String[] n = extractOtherType(names[3]);
	    	names[3] = n[0];
	    	otherType[1] = n[1];
	    	if (names[3].length() == 0){
	    		names[3] = names[4];
	    		names[4] = "";
	    	}
    	}    	
        return otherType;    	
    }
    
    public static void cleanOwnerNameILCookIS(ResultMap m, long searchId) throws Exception {
    	String s = (String) m.get("tmpOwnerFullName"), s1 = "", s2="", s3="";
        if(StringUtils.isEmpty(s))
        	return;
        
        // corrections
        s = s.replaceAll("\\bT\\s?R\\s?US\\s?T\\b", "TRUST");
        s = s.replaceAll("\\bR\\s?E\\s?V\\s?O\\s?K\\s?E\\b", "REVOKE");
        s = s.replaceAll("\\bRE\\s*E\\s?V\\s?O\\s?K\\s?E\\b", "REVOKE");
        s = s.replaceAll("REVOKETRUST", "REVOKE TRUST");
        s = s.replaceAll("IRREV\\b\\s*", "");
        s = s.replaceAll("\\bQUA LIFIED PE", "");
        s = s.replaceAll("\\bL\\s?I\\s?V\\s?I\\s?N\\s?G\\b", "LIVING");
        s = s.replaceAll("LIVINGTRUST","LIVING TRUST");
        s = s.replaceAll("\\bF\\s?I\\s?N\\s?A\\s?N\\s?C\\s?I\\s?A\\s?L\\b", "FINANCIAL");
        s = s.replaceAll("\\bM\\s?A\\s?R\\s?I\\s?T\\s?A\\s?L\\b", "MARITAL");
        s = s.replaceAll("\\bF\\s?A\\s?M\\s?I\\s?L\\s?Y\\b", "FAMILY");
        s = s.replaceAll("(?<=\\b(LIVING|MARITAL|FAMILY|REVOKE) )TR(US?)?\\b", "TRUST");
        s = s.replaceAll("MR & MRS", "");
        s = s.replaceAll("SMITHSOLODOVIENE","SMITH SOLODOVIENE "); 	//PIN:14082030161079, Owner: SMITHSOLODOVIENE RAIMONDA 
        Matcher matcher = Pattern.compile("(?is)(.*?)TRUST TRUSTEE").matcher(s);
	   	if (matcher.find() && !NameUtils.isCompany(matcher.group(1))) s = matcher.group(1) + "TRUST & " + matcher.group(1) + "TRUSTEE";
	   	s = s.replaceFirst("(?is)RE\\z","REV");
	   	s = s.replaceFirst("(?is)JT\\sTE","");
        
        // cleanup
        s = s.replaceFirst("(\\b[SWEN])?\\d+\\s*(?:-\\d+)?$", "");
        s = s.replaceFirst("#.+", "");                
        s = s.replaceFirst("\\bVIR\\s*$", "");
        s = s.replaceFirst("\\bDI\\s*$", "");
        s = s.replaceAll("(\\d+)\\s(\\d+)","$1$2");
        s = s.replaceAll("&([A-Z]+)", "& $1");
        s = s.replaceAll("\\s{2,}", " ").trim();
        
        // cazuri particulare
        s = s.replaceAll("KA REN","KAREN");	//SMITH KENNY SMITH KA REN IRREV
        s = s.replaceAll("JACKSO N","JACKSON");
        s = s.replaceAll("HELE S NE","HELENE S");
        s = s.replaceAll("SMIT\\s*[A-Z] H", "SMITH");
        s = s.replaceAll("\\bLI\\b","LIVING TRUST");
        //s = s.replaceAll("CHE ([A-Z]) RYL JOZ","CHERYL ");
        s = s.replaceAll("\\bSMITHSTAAL\\b","SMITH-STAAL");  //SMITHSTAAL-WEISS L DEBBIE  
	    
        //IL McHenry, PIN 0719203003: DAVIS, A V 2000 LIV TR ET AL*
        s = s.replaceAll("\\*", "");
        Matcher ma = Pattern.compile("([A-Z]+\\s*,\\s*[A-Z]+\\s+[A-Z])\\s+(.+)").matcher(s);
        if (ma.matches()) {
        	if (NameUtils.isCompany(ma.group(2))) {
        		s = ma.group(1) + " & " + ma.group(2);
        	}
        }
        
        if (!NameUtils.isCompany(s)) {
        	
        	s = s.replaceAll("(?is)\\bDR\\b","");
        	
        	//IL Kane, PIN 1511276032: WILSON, DAVID E HEATHER K & HEATHER NICOLE
        	s = s.replaceFirst("^([A-Z]+\\s*,\\s*[A-Z]+\\s+[A-Z])\\s+([A-Z]+(?:\\s+[A-Z])?)\\s*&\\s*([A-Z]+(?:\\s+[A-Z]+)?)$", "$1 & $2 & $3");
	    	
        	//IL McHenry, PIN 0127426002: DAVIS, JUSTIN W DOROTHY B
	    	//IL McHenry, PIN 0412401007: DAVIS, JOHN R MARGARET T
	    	//IL McHenry, PIN 0127426002: DAVIS, EDWARD M VALERIE
	    	s = s.replaceFirst("^([A-Z]+\\s*,\\s*[A-Z]+\\s+[A-Z])\\s+([A-Z]+(?:\\s+[A-Z])?)$", "$1 & $2");
	    	
	    	// SMITH MANUEL J SMITH V CATHERI   sau    SMITH HARVEY J SMITH U GERALDI
	        if (s.indexOf(' ') > 0)
	        {
	        	s1 = s.substring(0, s.indexOf(' '));
	        	s2 = s.substring(s.indexOf(' ')+1);
	        	s2 = s2.replaceAll(s1,"& "+ s1);
	        	s = s1 + " " + s2;
	        }
	        s3 = s;
	        int poz = 0, cont = 0, lngth = s3.length();
	        while (s3 != "")
	        {
	        	while (poz < lngth && s3.charAt(poz)!='&')
	        		poz ++;
	        	if (poz == lngth) break;
	        	else
	        	{
	        		cont ++;
	        		poz ++;
	       			if (poz == lngth) break; 
	        	}
	        }
	        if (cont >=2)  // cazul SMITH ELMER & FLOY & COY
	        {
	        	s = s.replaceAll("(?:&\\s*([A-Z]+)\\s*)","& "+ s1 + " $1 ");
	        }
	        if (!s.contains("TR") || !s.contains("TRU") || !s.contains("TRUS") || !s.contains("TRUST"))
	        {
	        	s = s.replaceAll("([^\\s]+)\\s\\d+(?:-\\d+)?","$1");	// SMITH 1703 MAXINE F  sau SMITH 420 N BERTHA E sau LIZZIE L.LENTON 4709-3 
	        	Matcher mat = Pattern.compile("([^\\s]+)\\s([A-Z])\\s([A-Z][A-Z]+)").matcher(s);
	        	if (mat.find() && !NameFactory.getInstance().isLastOnly(mat.group(3))) {
	        		s = mat.group(1) + " " + mat.group(3) + " " + mat.group(2);		// SMITHSTAAL-WEISS L DEBBIE (middle interschimbam cu first)
	        	}
	        }
	        s = s.replaceAll("([A-Z]+)\\s*JR\\s*([A-Z]+)","$1 $2 JR" );		// SMITH JR WILLIAM &RO
    	}
        m.put("tmpOwnerFullName", s);
    }
    
    public static void cleanGrantorGranteeFromTR(ResultMap m, long searchId){
    	String grantor = (String) m.get("SaleDataSet.Grantor");
    	String grantee = (String) m.get("SaleDataSet.Grantee");
    	    	
    	grantor = grantor==null? "" :grantor.replaceAll("(?i)\\bTR(\\.?\\s?#)[\\s\\d/]+", "");
    	grantee = grantee==null? "" :grantee.replaceAll("(?i)\\bTR(\\.?\\s?#)[\\s\\d/]+", "");
    	
    	m.put("SaleDataSet.Grantor", grantor);
    	m.put("SaleDataSet.Grantee", grantee);
    }
    
    @SuppressWarnings("rawtypes")
	public static void partyNamesILCookIS(ResultMap m, long searchId) throws Exception {

    	String s = (String) m.get("tmpOwnerFullName");
        if(StringUtils.isEmpty(s) || "Owner".equalsIgnoreCase(s))
        	return;
        
        String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
				
		if ("Will".equalsIgnoreCase(crtCounty)){
	        s = s.replaceAll("\\b(MARTH) (A)\\b", "$1$2");
			if (NameUtils.isNotCompany(s)){
				String[] names = s.trim().split("\\s+");
				String nameRepaired = "";
					for (int i = 2; i < names.length; i++) {
						if (i == 2){
							if (names[i].length() == 1 || isSuffix(names[i])){
								if (names.length > (i + 1) && isSuffix(names[i + 1])){
									names[i + 1] = names[i + 1] + " &";
								} else {
									names[i] = names[i] + " &";
								}
							} else if (names[i].length() > 1 && isSuffix(names[i])){
								names[i] = names[i] + " &";
							} else if (names[i].length() >= 1 && (names.length > (i + 1) && isSuffix(names[i + 1]))){
								names[i + 1] = names[i + 1] + " &";
							} else if (NameFactory.getInstance().isMale(names[i])){
								names[i] = names[i] + " & ";
							} else if (NameFactory.getInstance().isFemaleOnly(names[i])){
								if (NameFactory.getInstance().isFemaleOnly(names[i-1])){
									names[i] = names[i] + " & ";
								} else {
									if (names[i-1].length() == 1){
										names[i] = names[i] + " & ";
									} else{
										names[i] = " & " + names[i];
									}
								}
							}  
						}
					}
					for (int i = 0; i < names.length; i++) {
						nameRepaired += names[i] + " ";
					}
					nameRepaired = nameRepaired.trim();
					
					nameRepaired = nameRepaired.replaceAll("\\s+&\\s*$", "").replaceAll("\\s+", " ");
					nameRepaired = nameRepaired.replaceAll("\\b(FAY)\\s+(KATIE)\\s+&\\s+(SUE)\\b", "$1 & $2 $3");
					nameRepaired = nameRepaired.replaceAll("\\s+&\\s+(TR)\\b", " $1");
					
					m.put("tmpOwnerFullName", nameRepaired);
					s = nameRepaired;
			} else {
				s = s.replaceAll("\\bTR\\b", "TRUST");
				m.put("tmpOwnerFullName", s);
			}
			
			List<List> body = new ArrayList<List>();
			String[] nms = { "", "", "", "", "", "" };
			String[] suffixes = { "", ""}, type = { "", ""}, otherType = { "", ""};
			
			nms = StringFormats.parseNameNashville(s, true);
			
			type = GenericFunctions.extractAllNamesType(nms);
			otherType = GenericFunctions.extractAllNamesOtherType(nms);
			if (NameUtils.isNotCompany(s)){
				suffixes = GenericFunctions.extractNameSuffixes(nms);
			}
			GenericFunctions.addOwnerNames(nms, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(nms[2]),
					NameUtils.isCompany(nms[5]), body);
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
		} else {
			cleanOwnerNameILCookIS(m, searchId);
			s = (String) m.get("tmpOwnerFullName");
			GenericISI.partyNamesTokenizerILCookIS(m, s);
		}

    }
    
    
    public static void partyNames2Tokenizer(ResultMap m, String s) throws Exception {
    	
    	String suffix = "";
    	int idx = s.indexOf("@@");
    	if (idx != -1){
    		if (idx < s.length()-2){
    			suffix = s.substring(idx+2);
    		}
    		s = s.substring(0, idx);
    	}
    	String suffixOwner = "", suffixCoowner = "";
        
        List<List> body = new ArrayList<List>();
        String[] a, suffixes, type, otherType;
        
        boolean isMiddleInitial = false;
        if (!NameUtils.isCompany(s)){ //B4280
        	if (s.matches("(?is)^(\\w+)([A-Z])\\s+&\\s+([A-Z\\s]+)")) {
        		s = s.replaceAll("(?is)^(\\w+)([A-Z])\\s+&\\s+([A-Z\\s]+)", "$1 $2 & $3");
        		isMiddleInitial = true;
        	} else if (s.matches("(?is)^(\\w+)([A-Z])\\s*$") && !NameFactory.getInstance().isLast(s)){
        		s = s.replaceAll("(?is)^(\\w+)([A-Z])\\s*$", "$1 $2");
        		isMiddleInitial = true; 
        	}
        }
        
        int ampersandIndex = s.lastIndexOf("&");
        if (ampersandIndex!=-1) {							
        	s = s.replaceFirst("(?is)([A-Z]+\\s+[A-Z]+(?:\\s+[A-Z]+)?)\\s*&\\s\\1\\s*&", "$1 @#@ $1 &"); //B7078, DAWKINS EDGAR L / DAWKINS EDGAR L & JEAN S /LT
        	if (s.indexOf("@#@")==-1) {
        		String part1 = s.substring(0, ampersandIndex).trim();
            	String part2 = s.substring(ampersandIndex+1).trim();
            	
            	// Task 7864 - Leggett & Platt INC
            	if (((!NameUtils.isCompany(part1) && part1.split("\\s+").length > 1) && NameUtils.isCompany(part2)
            			&& !part1.matches("(?is)\\s*\\w+\\s*&\\s*\\w+\\s*"))//010-079702 OH Franklin
            			|| (NameUtils.isCompany(part1) && !NameUtils.isCompany(part2)) ) {	//more names, the last being a company
            		s = part1 + " @#@ " + part2;
            	}
        	}
        }
        
    	if (s.indexOf("@#@")==-1 && NameUtils.isCompany(s)){
    		a = new String[6];
    		if ((s.contains(",") || s.contains("TRUSTEE")) && s.contains("&")){
    			String[] b = s.split("\\s*&\\s*");
    			a = StringFormats.parseNameNashville(b[0], true);
    			
    			suffixes = extractNameSuffixes(a);
    			type = GenericFunctions.extractAllNamesType(a);
    	    	otherType = extractAllNamesOtherType(a);
    		    addOwnerNames(a, suffixes[0], suffixes[0], type, otherType, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
    		    
    		    if (b.length > 1){
	    		    if (b.length == 3){
	    		    	b[1] = b[1] + " & " + b[2];
	    		    }
	    		    a = StringFormats.parseNameNashville(b[1], true);
	    		    
	    		    suffixes = extractNameSuffixes(a);
	    			type = GenericFunctions.extractAllNamesType(a);
	    	    	otherType = extractAllNamesOtherType(a);
	    		    addOwnerNames(a, suffixes[0], suffixes[0], type, otherType, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
    		    }
    		} else {
    			s = s.replaceAll("\\b(INC|LLC)\\s*&", "$1 &&");
    			String[] owners = s.split("&&");
    			for (int i=0;i<owners.length;i++) {
    				a[0] = ""; a[1]=""; a[2]=owners[i].replaceAll("\\s{2,}", " ");
    	    		a[3] = ""; a[4]=""; a[5]="";
    	    		type = GenericFunctions.extractAllNamesType(a);
    	    		otherType = extractAllNamesOtherType(a);
    	    		addOwnerNames(a, "", "", type, otherType, true, false, body);
    			} 
	    	}
    	} else {
	        // parse as L, F M
    		if (!s.matches("(?is)[^&]+&\\s*\\w+\\s+LIVING\\s+TRUST\\s*@#@.*")){
    			s = s.replaceAll("(?is)\\s*&((?:\\s+[A-Z-]{4,}){2,})", "@#@$1");//[A-Z-]{4,} must be >= 4 because JO ANN is not a L F name, it is a F M
    		}
    		//HANLEY MALCOLM R & SHARON E & HANLEY M R & S E TRUSTEE
    		s = s.replaceAll("(?is)\\s*&(\\s*[A-Z-]+\\s+[A-Z-]+\\s[A-Z]\\s*&\\s*[A-Z-]+\\s+[A-Z](?:\\s+(T(?:(?:RU?)?S?)?(?:TE)?E?S?))?)", "@#@$1");
    		//RETHMEIER MELVIN & CAROLYN  & RETHMEIER M K & C R
    		s = s.replaceAll("(?is)\\s*&((?:\\s+[A-Z-]{1,}){2,}(?:\\s*&\\s*(?:\\s+[A-Z-]{1,}){1,}))", "@#@$1");
    		
    		//BURNSIDE BRETT T SHANE K RICHA & BURNSIDE CONNIE G
    		s = s.replaceAll("(?is)\\A((\\w+)\\s+\\w+\\s+[A-Z])\\s+(\\w+\\s+[A-Z])\\s+(\\w+(?:\\s+[A-Z])?)\\s*@#@", "$1 & $3 @#@ $2 $4 @#@");
    		
    		Matcher match = Pattern.compile("(?is)\\A(\\w+\\s+[A-Z]\\s+\\w+)\\s+(\\w+)\\s*(TRUSTEE)").matcher(s);
    		
    		//ZWALLY H JAY JO ANN M & ZWALLY KURT D
    		//RYCHEL B SUSAN W KENT & RYCHEL E LEIGH
    		if(!match.find()){
    			String patt = "(?is)\\A(\\w+\\s+[A-Z]\\s+\\w+)\\s+(\\w+)";
    			Matcher ma = Pattern.compile(patt).matcher(s);
    			if (ma.find()) {
    				String g2 = ma.group(2);
    				if (!g2.matches(nameSuffixString)) {
    					s = s.replaceFirst(patt, "$1 & $2");
    				}
    			}
    		}
    		
    		if (s.indexOf("@#@")==-1)
    			s = s.replaceFirst("&", "@@@").replaceFirst("&", "@#@").replaceFirst("@@@", "&");
    		
    		String[] owners = s.split("@#@");
    		String previousLast = "";
    		for (String owner : owners){
    			//56-43-42-15-07-200-0222 FLPalmBeach,   ROBBINS J A & D ADELBERG EDNA
    			owner = owner.replaceAll("(?is)([^&]+&)\\s*([A-Z])\\s+([A-Z]{2,}\\s+[A-Z]{2,})\\s*$", "$1 $3 $2");
    			//16-26-10-006.D-000.00-106.0 FLPasco, HANLEY MALCOLM R & SHARON E /T / HANLEY M R & S E
    			owner = owner.replaceAll("(?is)/T\\b", "TR");
    			if (!NameUtils.isCompany(owner)) {
    				//CO Huerfano , PIN 4657351
    				owner = owner.replaceAll("(?is)\\bQUALIFIED\\s+PERSONAL\\s*$", "");
    				owner = owner.replaceAll("(?is)\\bSMI\\s*$", "").trim();
    			}
    			a = StringFormats.parseNameNashville(owner, true);
		        
		        Matcher possibleDeSotoMatcher = Pattern.compile("(?is)(\\w+)\\s+[A-Z]\\s+([A-Z]{2,})").matcher(owner);	//(task 7055) COEagleNB R063533: Sarah M Lauterbach
    			boolean isDeSoto = false;																				//(task 9778) but not TXSan PatricioPRI 0973-0002-0023-000: GREEN S WAYNE
		        if (possibleDeSotoMatcher.matches()) {
		        	String g1 = possibleDeSotoMatcher.group(1);
		        	String g2 = possibleDeSotoMatcher.group(2);
		        	NameFactory nameFactory = NameFactory.getInstance();
		        	if (nameFactory.isFirstMiddle(g1) && nameFactory.isLast(g2) && (nameFactory.isFirstMiddleOnly(g1)||nameFactory.isLastOnly(g2))) {
		        		isDeSoto = true;
		        	}
		        }
    			if (isDeSoto || (owner.matches("(?is)[A-Z]\\s+\\w+\\s+[A-Z]{2,}")&&!owner.matches(StringFormats.NAME_WITH_PREPOSITION_PATTERN))){ // CO Eagle R031584 (bug 8762)
		        	a = StringFormats.parseNameDesotoRO(owner, true);
		        }
		        
		        if (owner.contains("LA JAUNESSE"))  //fix for BUG 3519
		        {
		        	a[0] += " " + a[1].substring(0, a[1].lastIndexOf(' '));
	  				a[1] = a[1].substring(a[1].lastIndexOf(' ')+1);
		        }
		        // separate suffix from the middle name
		        // owner name suffix appears next to co-owner name => we need to fix it
		        boolean hasCoownerSuffix = false;
		        if (!StringUtils.isEmpty(suffix)){
		            Pattern p = Pattern.compile("(.*)\\s*\\b"+suffix+"$");
		            Matcher ma = p.matcher(a[4]);
		        	if (!p.matcher(a[1]).matches() && ma.matches()){
		        		//suffixOwner = suffix;  //B 2975  the suffix of coowner appears like owner suffix and it's not correct
		        		a[4] = ma.group(1);
		        		hasCoownerSuffix = true;
		        	}
		        }
		        suffixes = extractNameSuffixes(a);
		        if (suffixes[0].length() != 0){
		        	suffixOwner = suffixOwner + " " + suffixes[0];
		        	suffixOwner = suffixOwner.trim();
		        } else if (suffixOwner.length() == 0 && suffix.length() != 0 && !hasCoownerSuffix){
		        	suffixOwner = suffix;
		        	a[1] = a[1].replaceFirst("\\b"+suffixOwner+"\\b", "").trim();
		        }
		        	
		        if (suffixes[1].length() != 0){
		        	suffixCoowner =  suffixCoowner + " " + suffixes[1];
		        	suffixCoowner = suffixCoowner.trim();
		        } else if (suffixCoowner.length() == 0 && suffix.length() != 0 && hasCoownerSuffix){
		        	suffixCoowner = suffix;
		        	a[1] = a[1].replaceFirst("\\b"+suffixOwner+"\\b", "").trim();
		        }
		
		        idx = a[0].indexOf("-");
		    	if (idx != -1){
		    		if (idx < a[0].length()-1){
		    			a[1] = (a[0].substring(idx+1) + " " + a[1]).trim();
		    		}
		    		a[0] = a[0].substring(0, idx);
		    	}
		    	idx = a[3].indexOf("-");
		    	if (idx != -1){
		    		if (idx < a[3].length()-1){
		    			a[4] = (a[3].substring(idx+1) + " " + a[4]).trim();
		    		}
		    		a[3] = a[3].substring(0, idx);
		    	}
		        
		    	if (isMiddleInitial){
		    		a[1] = a[0];
		    		a[0] = "";
		    	}
		    	type = GenericFunctions.extractAllNamesType(a);
	    		otherType = extractAllNamesOtherType(a);
	    		if (a[3].trim().length()==0 && a[4].trim().length()==0 && a[5].trim().length()==0) {	//there is no coowner
	    			suffixOwner = suffixes[0];
	    			suffixCoowner = "";
	    		}
	    		// Task 7604 - KIRK FAMILY / KIRK ROBERT G & EVELYN L /TE
	    		if(StringUtils.isNotEmpty(previousLast) && a[0].length() == 1 && NameFactory.getInstance().isFirstMiddle(a[2])) {
	    			a[1] = a[0];
	    			a[0] = a[2];
	    			a[2] = previousLast;
	    		}
	    		if(NameUtils.isNotCompany(a[2])) {
	    			previousLast = a[2];
	    		} else {
	    			previousLast = "";
	    		}
		        addOwnerNames(a, suffixOwner, suffixCoowner, type, otherType, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
	    	}
    	}
        storeOwnerInPartyNames(m, body, true);      	
    }
    
    private static final String[] trustListILDuPageAO = {
    	"(?is).*\\bINC\\b.*",
    	"(?is).*\\bBANKERS\\b.*",
    	"(?is).*\\bBANK\\b.*",
    	"(?is).*\\bLAND\\b.*", 
    	"(?is).*\\bMORTGAGE\\b.*",
    	"(?is).*\\bTITLE\\b.*",
    	"(?is).*\\bCITICORP\\b.*",
    	"(?is).*\\bCORP\\b.*",
    	"(?is).*\\bLLC\\b.*",
    	"(?is).*\\bL L C\\b.*",
    	"(?is).*\\bL.L.C.\\b.*",
    	"(?is).*\\bGUARANTEE\\b.*", 
    	"(?is).*\\bSAVINGS\\b.*",
    	"(?is).*\\bSUBURBAN\\b.*",
    	"(?is).*\\bCHARTER\\b.*",
    	"(?is).*\\bNATL\\b.*",
    	"(?is).*\\bNAT�L\\b.*",
    	"(?is).*\\bAMERICA\\b.*",
    	"(?is).*\\bNORTHERN\\b.*",
    	"(?is).*\\bSOUTHERN\\b.*",
    	"(?is).*\\bEASTERN\\b.*",
    	"(?is).*\\bWESTERN\\b.*",
    	"(?is).*\\bMIDWEST\\b.*",
    	"(?is).*\\bSTANDARD\\b.*",
    	"(?is).*\\bUNION\\b.*",
    	"(?is).*\\bSTATE\\b.*",
    	"(?is).*\\bILLINOIS\\b.*",
    	"(?is).*\\bDUPAGE\\b.*",
    	"(?is).*\\bNAPERVILLE\\b.*",
    	"(?is).*\\bNLSB\\b.*"
    };
    
    public static String cleanTrustILDupageNDB(String input) {
    	String tempString = input.toLowerCase();
    	if(tempString.contains("trust")) {
			for (String trustIdentifier : trustListILDuPageAO) {
				if(tempString.matches(trustIdentifier)) {
					return null;
				}
			}
			String[] tokens = tempString.split("[\\s]+");
			for (String token : tokens) {
				if(ro.cst.tsearch.search.address2.Normalize.isSuffix(token)) {
					return null;
				}
				if(ro.cst.tsearch.search.address2.Normalize.isPlainNumber(token)) {
					try {
						int possibleYear = Integer.parseInt(token);
						if(possibleYear < 1900 || possibleYear > 2050) {
							return null;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			tempString = ILDuPageRO.cleanTrust(tempString, "(\\b[0-9]+\\s*)?");
			tokens = tempString.split("[\\s]+");
			for (String token : tokens) {
				if(ro.cst.tsearch.search.address2.Normalize.isPlainNumber(token)) {
					try {
						int possibleYear = Integer.parseInt(token);
						if(possibleYear < 1900 || possibleYear > 2050) {
							return null;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
		}
    	
    	return tempString;
    }
    
    public static void partyNamesMax2(ResultMap m, long searchId) throws Exception {

    	String s = (String) m.get("tmpOwnerFullName");
        if(StringUtils.isEmpty(s))
        	return;
        //pt CA San Diego AO -> SMITH, JOHN M MR (PIN:5130730700), HESTER, JAMES W DCSD (PIN:5570910700), SMITH, CHRISTINE ELIZABETH DP	(PIN:3195802408)
    	if (s.matches("(.*)\\s(MR|DP|DCSD)\\s*$"))
    		s = s.replaceAll("(?:MR|DP|DCSD)","");
    	//pt TN Rutherford AO -> JR este sufix pt sot, nu pt sotie (BROOKE e nume de femeie)
    	if (s.contains("& BROOKE JR"))  //B3430
    	{
    		s = s.replaceFirst("& BROOKE JR", "JR & BROOKE");  
    	}
    	s = s.replaceAll("(?is)(^|\\s)/DC\\b", " ");		//deceased
    	s = s.replaceAll("(?is)\\bJOINT\\b", "");
    	s = s.replaceAll("(?is)&?\\s+VLB\\b", "");
    	s = s.replaceAll("(?is)\\bD/?B/?A\\b", "&");
    	s = s.replaceFirst("(?is)(.*)\\(T\\s*\\z", "$1(TE)");			//B7100
    	s = s.replaceAll("(?is)\\s*\\(\\s*TE\\s*\\)\\s*", " TRUSTEE ");
    	s = s.replaceFirst("(?is)\\s*-\\s*(ETAL)\\s*$", " ETAL");
    	s = NameCleaner.correctName(s);

    	String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
        
        if (crtState.equals("FL")) {
        	if (crtCounty.equals("Hendry") || crtCounty.equals("Levy") || crtCounty.equals("Martin")) {
        		s = s.replaceAll("(?is)\\s+/\\s+", " @#@ ");
        	}
        	if (crtCounty.equalsIgnoreCase("DeSoto")){
        		s = s.replace("& CHILDREN",""); // T8394
        	}
        }
    	
    	//for NV
        s = s.replaceAll("(?is)\\A\\s*(\\w+)\\s*/\\s*(\\1)\\b\\b", "$2");
    	s = s.replaceAll("(?is)\\s*/?\\s*/\\s*TE\\b", " TRUSTEE");
    	s = s.replaceAll("(?is)\\s+/\\s+", " & ");
    	//for NV
    	 
    	s = s.replaceAll("(?is)\\s*/LT\\s*\\z", " LIVING TRUST");
    	s = s.replaceAll("(?is)\\s*/RT\\s*\\z", "");
    	s = s.replaceAll("(?is)\\s*/L\\s*\\z", "");
    	s = s.replaceAll("(?is)\\s*/\\s*\\z", "");
    	s = s.replaceAll("(?is)\\s*/((T(?:(?:RU?)?S?)?(?:TE)?E?S?))\\s*\\z", " $1");
    	s = s.replaceAll("(?is)\\A\\s*CO-TRUSTEES?\\b(.*)", "$1 TRUSTEE");
    	
    	if(crtState.equals("CO")) {
			if(crtCounty.equals("Eagle")) {
				String regExp = "(?is)(\\b[A-Z]\\b)\\s+([A-Z'-\\.]+)\\s+([A-Z'-\\.]+)\\s+&\\s+([A-Z'-\\.]+|[A-Z])\\s+([A-Z'-\\.]+)\\s+([A-Z'-\\.]+)";
				Matcher mat = Pattern.compile(regExp).matcher(s); 
				if (mat.find()) {
					String grp1 = mat.group(3);
					String grp2 = mat.group(6);
					if (grp2.equals(grp1)) { // W ALLAN MACROSSIE  &  MARY LOUISE MACROSSIE, Bug 8762
						s = s.replaceFirst(regExp, "$1 $2 $3 & $6 $4 $5");
					}
				}
			}
    	}
    	
    	/*
    	//pt CA San Diego AO -> BLAKE,SEAN H & JENNIFER M FAMILY TRUST 06-16-03E & NANCY V (PIN:3190703000)
    	if (s.matches("(.*)\\s*FAMILY\\s*TRUST\\s*[-\\dA-Z]+\\s*(&\\s*[A-Z\\s]+)?"))
    		s = s.replaceFirst("(.*)\\s*FAMILY\\s*TRUST\\s*[-\\dA-Z]+\\s*(&\\s*[A-Z\\s]+)", "$1 $2");
    	*/
    	//CA Riverside owner name = PETERS, C RICHARD & E MARIE FRITZB  B 3383
    	s = s.replaceAll(",\\s+([A-Z])\\s+([A-Z]+)\\s+&\\s+([A-Z])\\s+([A-Z]+)\\s+([A-Z]+)", " $2 $1 & $5 $4 $3");
        String suffix = (String) m.get("tmpOwnerSuffix");
        if (suffix != null)
        	s = s.concat("@@").concat(suffix);
        
//        if("TN".equals(crtState)){
//        	if("Davidson".equals(crtCounty)){
//        		
//        	}
//        } else {
        	partyNames2Tokenizer(m, s);
//        }
        
		if(crtState.equals("IL")) {
			if(crtCounty.equals("Du Page")) {
				String tempString = cleanTrustILDupageNDB(s);
				if(StringUtils.isNotEmpty(tempString) && 
						!tempString.trim().equalsIgnoreCase(s.trim())) {
					
					ResultTable parties = (ResultTable)m.get("PartyNameSet");
					
					try {
			 			if(parties != null) {
			 				partyNames2Tokenizer(m, tempString);
			 				ResultTable partiesNew = (ResultTable)m.get("PartyNameSet");
			 				
			 				partiesNew = ResultTable.joinVertical(parties, partiesNew, true);
			 				m.put("PartyNameSet", partiesNew);
			 			} 
		 			} catch (Exception e) {
						e.printStackTrace();
						m.put("PartyNameSet", parties);
					} 
				}
			}
		}
		
		
    }
    
    public static void stdPisTNKnoxEP(ResultMap m,long searchId) throws Exception {
    	TNKnoxEP.stdPisTNKnoxEP(m, searchId);
    }
    
    public static void partyNamesTNRutherfordEP(ResultMap m,long searchId) throws Exception {  
    	
    	String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
    	
    	if(StringUtils.isEmpty(s))
        	return;
    	
    	TNRutherfordEP.partyNamesTNRutherfordYA(m, searchId);
               
    }

    // Owner name can be in L, FM format or in FML format
    public static void stdPisOaklandAO(ResultMap m,long searchId) throws Exception {
        String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
        if(s == null || s.length() == 0)
        	return;
        
        // if the owner was recorded on 2 rows, the separation between rows is 2 spaces; 
        // most of the times, when there are 2 rows, each row contains an owner, but there are cases when there is just one owner with a long name
        if (s.contains("AUTHORITY") || s.contains("DEVELOPMENT") || s.contains("DOWNTOWN")){ // ex. 600 Main
        	s = s.replaceFirst("\\s{2}", " ");
        } else {
        	s = s.replaceFirst("\\s{2}", " & ");
        }
        
        // remove JR suffix is present, to avoid confusing the tokenizer; the suffix will be added to middle name later
        boolean isOwnerJR = (s.matches(".*\\bJR\\b.*") && !s.contains("&")) || s.matches(".*\\bJR\\b.*&.+");
        boolean isSpouseJR = s.matches(".+&.*\\bJR\\b.*");
        if (isOwnerJR || isSpouseJR){
        	s = s.replaceAll("\\s*\\bJR\\b\\s*", " ").trim();
        }
        
        // remove "DR" prefix
        s = s.replaceAll("\\s*\\bDR\\b\\.?\\s*", " ").trim();
        
        boolean isLFM = false;
        s = s.replaceAll("/", " & ");
        isLFM = s.matches(".+ \\w+ & \\w+") || s.matches( "\\w+\\s+\\w+\\s+\\w" ); // fix for PID 14-28-255-008 (HARRIS FREDDIE/JUANITA)
        
        String[] a;
        //if owner string contains ',', then apply L, F M tokenizer , else apply F M L tokenizer
        if (s.contains(",") || isLFM){
        	a = StringFormats.parseNameNashville(s);
        } else {
        	if (s.matches("\\w+ & \\w+ \\w+")){
        		s = s.replaceFirst("(\\w+) & (\\w+) (\\w+)", "$1 $3 & $2"); // fix for strings as OF & WF OL (PID 12-28-451-003, 12-28-451-002)
        	}
        	a = StringFormats.parseNameDesotoRO(s);
        }
        
        if (isOwnerJR){
        	a[1] = a[1].concat(" JR").trim();
        }
        if (isSpouseJR){
        	a[4] = a[4].concat(" JR").trim();
        }
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
    }
    
    public static void partyNamesMIOaklandAO(ResultMap m, long searchId)throws Exception{
        String name = (String) m.get("tmpOwnerFullName");
        if (name == null)
        	return; 
        partyNamesTokenizerMIOaklandAO(m, name);
 	}
    
    public static void partyNamesTokenizerMIOaklandAO(ResultMap m, String s) throws Exception {
    	
    	// cleanup
    	s = s.replaceAll("\\s{2,}", "@@");
    	s = s.replaceFirst("\\bDR\\b\\.?", "");
    	s = s.replaceAll("[\\)\\(]", "");
    	s = s.replaceAll("(?<=\\b[A-Z])\\.(?=[A-Z]\\.|\\s|@@|$)", "");
    	s = s.replaceAll("\\b(AS )?TRST\\b(?!$|@@)", "");
    	s = s.replaceAll("@@(AS )?TRUSTEES?( FOR)?\\b", "@@C/O");
    	s = s.replaceAll("@@%", "@@C/O ");
    	s = s.replaceAll("\\b(AS )?TRUSTEES?( FOR)?\\b", "");
    	s = s.replaceAll("\\b(AS )?TTEE\\b,?", "");
    	s = s.replaceFirst("\\bTRUS$", "TRUST"); // 02-10-426-021
    	s = s.replaceFirst("\\bPOST (NO |#)\\d+(?=$|@@)", "");	// 25-03-152-024
    	s = s.replaceFirst("\\b(RLT|(?<!REAL )ESTATE)$", "");
    	s = s.replaceFirst("-$", "");
    	s = s.replaceAll("\\bREFERENCE SEE:.+", "");
    	s = s.replaceFirst("@@"+nameSuffixString+"$", " $1");
    	s = s.replaceAll("(.+)@@(?:OF )?(?:THE )?SAME ((?:LIV(?:ING)? )?TRUST)", "$1@@$1 $2"); 
 	   	s = s.replaceAll("\\s{2,}", " ").trim();
 	   	
 	   	// temporary remove the suffixes in order to decide the structure of the name
 	   	String sTmp = s.replaceAll(nameSuffixString, "");
 	   	String entitiesTmp[] = sTmp.split("@@");
 	   	String entities[] = s.split("@@");
 	   	
 	   	// in some cases the lines need to be concatenated 
 	   	if (entities.length > 1 && entitiesTmp.length > 1){
 	   		entities[1] = entities[1].replaceFirst("\\b\\d2{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b", "");
 	   		entities[1] = entities[1].replaceFirst("\\s*\\b\\w+: .+", "");
 	   		entities[1] = entities[1].replaceFirst("^\\d+-\\d+$", "");
 	   		entities[1] = entities[1].replaceAll("\\s{2,}", " "); 	   		
 	   		
 	   		if (entities[1].length() != 0){
	 	   		// cases when not to concatenate 
	 	   		if (entitiesTmp[0].matches(".*\\bTRUST\\b.*") && entitiesTmp[1].matches(".*\\bTRUST\\b.*") 
	 	   			|| entitiesTmp[0].matches(".*\\b(DBA|C/O)")
	 	   			|| entitiesTmp[0].matches("[A-Z'-]{2,},\\s?[A-Z]+( [A-Z])?") && entitiesTmp[1].matches("[A-Z'-]{2,},\\s?[A-Z]+( [A-Z])?")
	 	   			|| entitiesTmp[1].matches("C/O\\b.*")){
	 	   			
		 	   	} else {					
		 	   		if (entitiesTmp[1].matches("(BY|OF|(REVOCABLE|REV)( LIV(ING)?)? (TRU(ST)?|TRST|TR)|ASSOCIATES)\\b.*")
		 	   				|| entitiesTmp[1].matches(".*\\b(OF( THE)?|BY)")
		 	   				|| NameUtils.isCompany(entitiesTmp[0]) && NameUtils.isCompany(entitiesTmp[1])
		 	   				|| entitiesTmp[1].matches("[^\\s]+")
		 	   				|| entitiesTmp[0].matches("[^\\s]+( [A-Z])?( &)?")// && entitiesTmp[1].matches("[^\\s]+( [^\\s])?")
		 	   				|| entitiesTmp[1].matches("\\w+(,\\w+){2,}")
		 	   				|| entitiesTmp[0].contains("CHRISTIAN") && entitiesTmp[1].contains("MINISTR")
		 	   				|| entitiesTmp[0].contains("CITY") && entitiesTmp[1].contains("PARK")
		 	   				|| NameUtils.isCompany(entitiesTmp[0]) && entitiesTmp[1].matches(".*\\b\\d+-\\d+\\b.*")
		 	   				){
		 	   			entities[0] = entities[0] + " " + entities[1];
		 	   			entities[1] = "";
		 	   		}
		 	   	}
 	   		}
 	   	}
 	   	
 	   	for (int i=0; i<entities.length; i++){
 	   		entities[i] = entities[i].replaceAll("\\b(C/O|DBA)\\b", "");
 	   		entities[i] = entities[i].replaceAll("\\bAND\\b", " & ");
 	   		entities[i] = entities[i].replaceAll("(?<=[A-Z])/(?=[A-Z]{2})", " & ");
 	   		entities[i] = entities[i].replaceAll("(?<=[A-Z]{2})/(?=[A-Z])", " & ");
 	   		entities[i] = entities[i].replaceFirst("\\s*[&/]$", "");
 	   		entities[i] = entities[i].trim();
 	   	} 
 	   
        List<List> body = new ArrayList<List>();
 	   	String[] a = new String[6];
 	   	Pattern suffOwner = Pattern.compile("(.*)\\s*"+nameSuffixString+"(.*)");
 	   	Pattern middle = Pattern.compile("([^-]+)-(.+)");
 	   	Pattern multipleNames = Pattern.compile("(.*\\s?&\\s?[A-Z'-]{2,} )([A-Z]+(,[A-Z]+){2,})"); 
 	   	Matcher ma;
 	   	
 	   	for (int i=0; i<entities.length; i++){
 	   		
 	   		if (entities[i].length() == 0)
 	   			continue;
 	   		
	 	   	if (NameUtils.isCompany(entities[i])){ 
	  		   a[2] = entities[i]; 
	  		   a[0] = ""; a[1] = ""; a[3] = ""; a[4] = ""; a[5] = "";
	  		   addOwnerNames(a, "", "", true, false, body);
	  		   continue;
	 	   	}
	 	   	
 	   		entities[i] = entities[i].replaceFirst("\\s\\d+$", "");
 	   		entities[i] = entities[i].replaceAll("\\.", ",");					// 08-20-402-021 	   		
 	   		entities[i] = entities[i].replaceFirst("^([A-Z'-]{2,}, [A-Z]+ [A-Z]) ([A-Z'-]{2,} [A-Z]+ [A-Z])$", "$1 & $2"); // 14-09-377-021
	 	   	
 	   		// remove suffixes if present, to avoid confusing the tokenizer
	 	   	boolean hasMultipleNames = false;
 	        ma = multipleNames.matcher(entities[i]);
 	        if (ma.matches()){
 	        	entities[i] = ma.group(1) + ma.group(2).replaceAll(",", " & ");
 	        	hasMultipleNames = true;
 	        }
	 	   	
 	   		String all[] = entities[i].split("\\s*&\\s*"); 	   		
 	   		String suffixes[] = new String[all.length];
 	   		entities[i] = "";
 	   		for (int j=0; j<all.length; j++){
 	   			ma = suffOwner.matcher(all[j]);
 	   			if (ma.matches()){
 	   				suffixes[j] = ma.group(2);
 	   				all[j] = ma.group(1) + ma.group(3);
 	   			} else {
 	   				suffixes[j] = "";
 	   			}
 	   			if (j == 0){
 	   				entities[i] = all[0]; 
 	   			} else {
 	   				entities[i] = entities[i] + " & " + all[j];
 	   			}
 	   		}
 	   		entities[i] = entities[i].replaceAll("\\s+,", ",").trim();
 	   		entities[i] = entities[i].replaceFirst(",\\s*$", "");
 	   			        
 	        boolean isLFM = false;
 	        isLFM = entities[i].matches(".+ [A-Z]+ & [A-Z]+( [A-Z])?") || entities[i].matches("[A-Z'-]{2,} [A-Z]+ [A-Z]"); // for PID 14-28-255-008 (HARRIS FREDDIE/JUANITA)
 	        entities[i] = entities[i].replaceFirst("^([A-Z]+(?: [A-Z])?) & ([A-Z]+(?: [A-Z]+)?) ([A-Z'-]{2,})$", "$1 $3 & $2 $3"); // fix for strings as OF & WF OL (PID 12-28-451-003, 12-28-451-002, 10-28-276-017)
 	        
 	        all = entities[i].split("\\s*&\\s*");
 	        String lastName = "";  
 	        boolean lastWasLFM = false;
 	        for (int j=0; j<all.length; j++){
 	        	if (lastName.length() != 0){
 	        		if ((all[j].matches("[A-Z]+( [A-Z])?") || all[j].matches("[A-Z]+ [A-Z]+") && lastWasLFM && !hasMultipleNames)){
 	        			all[j] = lastName + ", " + all[j];
 	        		} else if (all[j].matches("[A-Z]+-[A-Z]+")) {
 	        			all[j] = all[j].replace('-', ' ');
 	        			all[j] = lastName + ", " + all[j];
 	        		}
 	        	}

	 	        //if name string contains ',', then apply L F M tokenizer , else apply F M L tokenizer
	 	        if (all[j].contains(",") || all[j].matches("[A-Z'-]{2,}( [A-Z]+)? [A-Z]") || s.matches("[A-Z'-]{2,}@@[A-Z]+") || isLFM){
	 	        	a = StringFormats.parseNameNashville(all[j]);
	 	        	lastWasLFM = true;
	 	        } else {
	 	        	a = StringFormats.parseNameDesotoRO(all[j]);
	 	        	lastWasLFM = false;
	 	        }	
	 	        ma = middle.matcher(a[0]);		//AVALLONE, XUAN-DUNG T
	 	        if (ma.matches()){
	 	        	a[0] = ma.group(1);
	 	        	if (a[1].length() == 0){
	 	        		a[1] = ma.group(2);
	 	        	} else { 
	 	        		a[1] = ma.group(2) + " " + a[1];
	 	        	}
	 	        }
	 	        if (a[0].length() != 0){
	 	        	lastName = a[2];
	 	        } else {
	 	        	lastName = "";
	 	        }
	 	        addOwnerNames(a, suffixes[j], "", false, false, body);
 	        }
 	   	}
 	   	 	   	 	              
        storeOwnerInPartyNames(m, body);                
    }
    
    // Owner name can be in L, FM format or in FML format; multiple owners can be listed for a property 
    public static void stdMPisMIWayneTR(ResultMap m, long searchId) throws Exception {
        String s = (String) m.get("tmpName");
        if(s == null || s.length() == 0)
        	return;
        
        //clean names
        s = s.replaceAll("\\bNOT DDA JONES\\b\\s*", "").trim(); // PID 04003542-3
        s = s.replaceAll("\\bCURRENT OWNER\\b\\s*", "").trim(); // PID 32101822700100
        s = s.replaceAll("\\bTAXPAYER\\b\\s*", "").trim(); 		// PID 16021780.
        s = s.replaceAll("\\b([A-Z]+)/([A-Z]+)\\b", "$1 & $2");	// PID 57017160125000, 57014300007000
        s = s.replaceFirst("\\s+([A-Z]?\\d+)$", "");			// PID 16021477.
        s = s.replaceAll(",\\s{2,}", ", ");
        
        // if there are more than one owner (+ spouse), they are separated by <br> in intermediate results (which becomes \s\s after HTML correction) 
        // or by \s/\s in final results 
        String names[] = s.split("(\\s/\\s|\\s\\s)");
        int len = names.length;
        if (len == 0)
        	return;
                         
    	String [][] body = new String[len][6];
        boolean isLFM = false;
        
        for (int i=0; i<len; i++){
        	String name = names[i]; 
        	if (NameUtils.isCompany(name)){		// fix for bug #2259
        		body[i]= new String[] {"", "", "", "", "", ""};
        		body[i][2] = name;
        	} else {
	            isLFM = name.matches(".+ [A-Z]+ & [A-Z]+");            
	            //if owner string contains ',', then apply L, F M tokenizer , else apply F M L tokenizer
	            if (name.contains(",") || isLFM){
	            	body[i] = StringFormats.parseNameNashville(name);
	            } else {
	            	if (name.matches("[A-Z]+ & [A-Z]+ [A-Z]+")){
	            		name = name.replaceFirst("([A-Z]+) & ([A-Z]+) ([A-Z]+)", "$1 $3 & $2"); // fix for strings as OF & WF OL (PID 32101821604400)
	            	}
	            	body[i] = StringFormats.parseNameDesotoRO(name);
	            }            
        	}
        	
            if( i == 0 ){
            	m.put("PropertyIdentificationSet.OwnerFirstName", body[i][0]);
            	m.put("PropertyIdentificationSet.OwnerMiddleName", body[i][1]);
            	m.put("PropertyIdentificationSet.OwnerLastName", body[i][2]);
            	m.put("PropertyIdentificationSet.SpouseFirstName", body[i][3]);
            	m.put("PropertyIdentificationSet.SpouseMiddleName", body[i][4]);
            	m.put("PropertyIdentificationSet.SpouseLastName", body[i][5]);
            }
        }
        
        ResultTable pis = new ResultTable();
    	String [] header = {"OwnerFirstName", "OwnerMiddleName", "OwnerLastName", "SpouseFirstName", "SpouseMiddleName", "SpouseLastName"};
    	
    	Map map = new HashMap();
    	map.put("OwnerFirstName", new String[]{"OwnerFirstName", ""});
    	map.put("OwnerMiddleName", new String[]{"OwnerMiddleName", ""});
    	map.put("OwnerLastName", new String[]{"OwnerLastName", ""});
    	map.put("SpouseFirstName", new String[]{"SpouseFirstName", ""});
    	map.put("SpouseMiddleName", new String[]{"SpouseMiddleName", ""});
    	map.put("SpouseLastName", new String[]{"SpouseLastName", ""});
    	
        pis.setHead(header);
    	pis.setBody(body);
    	pis.setMap(map);
    	m.put("PropertyIdentificationSet", pis);
        
    }
    
    public static void partyNamesMIWayneTR(ResultMap m, long searchId)throws Exception{
        String name = (String) m.get("tmpName");
        if (name == null)
        	return; 
        name = name.replaceAll("\\s{2}", "@@");
        partyNamesTokenizerMIWayneTR(m, name);
 	}
    
    public static void partyNamesTokenizerMIWayneTR(ResultMap m, String s) throws Exception {
    	
    	 //clean names
    	s = s.replaceAll("@@", " / ");
    	s = s.replaceAll("\\bDR\\b\\.?\\s*", "");
        s = s.replaceAll("\\bNOT DDA JONES\\b\\s*", "").trim(); // PID 04003542-3
        s = s.replaceAll("\\bCURRENT OWNER\\b\\s*", "").trim(); // PID 32101822700100
        s = s.replaceAll("\\bTAXPAYER\\b\\s*", "").trim(); 		// PID 16021780.
        s = s.replaceFirst("^/?(HOME)?OWNER/?$", "");
        s = s.replaceFirst("^/?OCCUPANT/?$", "");
        s = s.replaceAll("\\s*\\([^\\)]*\\)?", "").trim();        
        s = s.replaceFirst("\\s(?<!REAL )ESTATE$", "");
        s = s.replaceAll("\\s+#[\\w-]+", "");
        s = s.replaceAll("\\b\\w+#", "");
        s = s.replaceAll("\\bMAIL (CODE|STOP)\\b.*", "").trim();
        s = s.replaceAll("^-.*", "");        
        s = s.replaceAll("\\bATTN:.*", "");
        s = s.replaceAll("\\s*[/-]\\s*$", "");
        s = s.replaceAll("\\[.*\\]", "");
        s = s.replaceAll(",\\s{2,}", ", ");
        s = s.replaceAll("\\s+(?=,)", "");
        s = s.replaceAll("\\bC/O\\b:?", "/");       
    	s = s.replaceFirst("\\s*#\\w+(?=\\s*/\\s*|$)", "");
        s = s.replaceFirst("^([A-Z]+)\\s*/", "$1 & ");        
        Matcher ma = Pattern.compile("\\s*/\\s*([A-Z]+)(?=\\s*/\\s*|$)").matcher(s);	// SMITH, BERNICE FRIEND LIVING TRUST / TRUST  or CURTIS, JAMES & SANDRA / CURTIS
        while (ma.find()){
        	if (NameUtils.isCompany(ma.group(1))){
        		if (s.matches(".*\\b"+ma.group(1)+"\\s*/\\s*"+ma.group(1)+".*")){ 
        			s = s.replaceFirst(ma.group(0), "");
        		} else {
        			s = s.replaceFirst("\\s*/\\s*"+ma.group(1), " "+ma.group(1));
        		}
        	} else {
        		if (s.matches(".*\\b"+ma.group(1)+"\\s*/\\s*"+ma.group(1)+".*")){ 
        			s = s.replaceFirst(ma.group(0), "");
        		} else if (s.matches(ma.group(1)+".*/\\s*"+ma.group(1))){
        			s = s.replaceFirst(ma.group(0), "");
        		} else {
        			s = s.replaceFirst(ma.group(0), "& " + ma.group(1));
        		}
        	}
        }
        s = s.replaceAll("\\s*/,", "&"); // QUIGLEY,JOHN /,KERRY
        s = s.replaceAll("\\bDBA\\b:?\\s*", "/DBA ");        
        s = s.replaceAll("\\s*\\.(?![A-Z])", "");
        s = s.replaceAll("\\s*\\.", " ");
        s = s.replaceAll("\\s-\\s", " & ");
        s = s.replaceFirst("^([A-Z'-]{2,}, [A-Z]+)-([A-Z]+)$", "$1 & $2"); // SMITH, MARK-MARY
        s = s.replaceFirst("^([A-Z'-]{2,}, [A-Z]+ [A-Z]+)-([A-Z]+(?: [A-Z])?)$", "$1 & $2");  // SMITH, MARK GARY-ROXANNE      
        s = s.replaceAll("([A-Z'-]{2,}(?: "+nameSuffixString+")?, [A-Z]+(?: [A-Z])?)[-,&\\s]+(?=[A-Z'-]{2,}(?: "+nameSuffixString+")?, [A-Z]+(?: [A-Z])?\\b)", "$1 /"); // CHARLTON, BRANDON J SMITH, LASHONTA L  or  FLORES, GLORIA-VANDERGRIFF, JANETTE  or  HAWKINS, CORRINE N , BRIGGS, ELISE
        s = s.replaceAll("([A-Z'-]{2,}(?: "+nameSuffixString+")?, [A-Z]+ [A-Z]+)[,&\\s]+([A-Z'-]{2,}(?: "+nameSuffixString+")?, [A-Z]+(?: [A-Z])?)\\b", "$1 / $3"); // SMITH, DOUGLAS JOHN SMITH, KARILYNN        
        s = s.replaceFirst("^([A-Z'-]{2,}, [A-Z]+(?: [A-Z])?)[-,&\\s]+([A-Z]{2,} [A-Z] (?!"+nameSuffixString+")[A-Z'-]{2,})(?=$|\\s?/)", "$1 / $2"); // ROCHON, JOHN T BARBARA A SHELDON
                
        // if there are more than one owner (+ spouse), they are separated by <br> in intermediate results (which becomes \s\s after HTML correction) 
        // or by \s/\s in final results 
        String names[] = s.split("(\\s*/\\s*|@@)");
        int len = names.length;
        if (len == 0)
        	return;
                         
        List<List> body = new ArrayList<List>();
        String a[];
        String suffixes[];
        boolean isLFM = false;
        Pattern middle = Pattern.compile("([^-]+)-(.+)");
        Pattern p = Pattern.compile("(.+?\\s*&\\s*[^&]+)&?\\s*(.*)");
        Pattern dba = Pattern.compile("\\bDBA\\b:?\\s*(.+)");
        
        for (int i=0; i<len; i++){
        	String name = names[i].trim(); 
        	if (name.length() == 0)
        		continue;
    		ma = dba.matcher(name);
    		if (ma.find()){
    			a = new String[] {"", "", ma.group(1), "", "", ""};
        		addOwnerNames(a, "", "", true, false, body);
    		} else if (NameUtils.isCompany(name) || name.matches("\\w+\\s*&\\s*\\w+(\\s\\d.*)?")){		// fix for bug #2259
        		a = new String[] {"", "", name, "", "", ""};
        		addOwnerNames(a, "", "", true, false, body);        		
        	} else {
        		name = name.replaceFirst("\\bP\\s?O BOX .*", "");
                name = name.replaceFirst("\\s*\\b([A-Z]+-?)?#?(\\d+-((\\d+|[A-Z])-)?)?\\d+([A-Z]{1,2})?$", "");			// PID 16021477.
        		name = name.replaceAll("\\s-", " &");
        		name = name.replaceAll("-\\s", "& ");
        		name = name.replaceAll("(?<=\\b[A-Z])-", "&");
        		name = name.replaceAll("-(?=[A-Z]\\b)", "&");
        		name = name.replaceFirst("^([A-Z]+)-([A-Z]+ [A-Z]+)$", "$1 & $2");
        		name = name.replaceFirst("-(?=[A-Z]+ [A-Z]+$)", "/");
        		name = name.replaceAll(" AND ", " & ");
        		name = name.replaceFirst(",?\\s*\\bTR$", "");
                name = name.replaceFirst("\\s*\\b#?\\w*\\d+\\w*\\s*$", "");
                name = name.replaceAll(":", "");
        		isLFM = name.matches(".+ [A-Z]+ & [A-Z]+") || name.matches("[A-Z'-]{2,} [A-Z]+ [A-Z]") || name.matches("[A-Z'-]{2,} [A-Z]");
        		String others = "";
        		String lastLN = "";
        		ma = p.matcher(name);        		
        		if (ma.matches()){
        			name = ma.group(1);
        			others = ma.group(2);
        		}
        		
        		//if owner string contains ',', then apply L, F M tokenizer , else apply F M L tokenizer
	            if (name.contains(",") || isLFM){
	            	a = StringFormats.parseNameNashville(name);
	            } else {
	            	name = name.replaceFirst("^([A-Z]+(?: [A-Z])?(?: "+nameSuffixString+")?)\\s?&\\s?([A-Z]+(?: [A-Z])?) ([A-Z'-]{2,})$", "$1 $4 & $3"); // fix for strings as OF & WF OL (PID 32101821604400) or TIMOTHY J JR & LISA M RAUH
	            	a = StringFormats.parseNameDesotoRO(name);
	            }	 
	            suffixes = extractNameSuffixes(a);	            
	            ma = middle.matcher(a[0]);		
	 	        if (ma.matches()){
	 	        	a[0] = ma.group(1);
	 	        	if (a[1].length() == 0){
	 	        		a[1] = ma.group(2);
	 	        	} else { 
	 	        		a[1] = ma.group(2) + " " + a[1];
	 	        	}
	 	        }
	 	        if (a[0].length() != 0)
	 	        	lastLN = a[2];
	            addOwnerNames(a, suffixes[0], suffixes[1], false, false, body);
	            if (others.length() != 0){
	            	String entities[] = others.split("\\s*&\\s*");
	            	for (int j=0; j<entities.length; j++){
	            		if (!entities[j].contains(" ")){
	            			entities[j] = lastLN + ", " + entities[j];	            			                                
	            		}
		            	if (entities[j].contains(",")){
			            	a = StringFormats.parseNameNashville(entities[j]);
		            	} else {
		            		a = StringFormats.parseNameDesotoRO(entities[j]);
		            	}
		            	if (a[0].length() != 0)
		            		lastLN = a[2];
		            	suffixes = extractNameSuffixes(a);
		            	addOwnerNames(a, suffixes[0], suffixes[1], false, false, body);	
	            	}
	            }
        	}
        } 	   	 	   	 	              
        storeOwnerInPartyNames(m, body);                
    }
    
    public static void stdPisMIWayneEP(ResultMap m,long searchId) throws Exception {
    	String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
        if (s == null || s.length() == 0) 
        	return;
        
        if (s.contains(",")){
        	String[] a = StringFormats.parseNameNashville(s);
            m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
            m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
            m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
            m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
            m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
            m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
        }
    }
    
    public static void stdPisDavidsonAO(ResultMap m,long searchId) throws Exception {
        
        String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
        if (s!=null) {
            
            if (s.indexOf("-TRUSTEE")!=-1) {
                s = s.substring(0,s.indexOf("-TRUSTEE"));
            }
            s = s.trim();
            s = s.replaceAll("\\."," ");
            s = s.replaceAll("\\s{2,}"," ");
        //    String[] a = StringFormats.parseNameDesotoRO(s);
            String[] a = StringFormats.parseNameDavidsonAO(s);
            m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
            m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
            m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
            m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
            m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
            m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
            
        }
        
    }
    
    
    public static void processSubdivisionDavidsonAO(ResultMap m,long searchId) throws Exception{
        String s = (String) m.get("PropertyIdentificationSet.ParcelID");
        if (s!= null)
        {
            if (s.length() == "19103160A01200CO".length())
            {
                s = s.substring(2, "19103160A01200CO".length());
                m.put("PropertyIdentificationSet.ParcelID", s);
            }
        }
        
        String tmp = (String) m.get("PropertyIdentificationSet.SubdivisionName");
        tmp = tmp.replaceAll("\\s-[A-Z]"," ");
        tmp = tmp.replaceFirst("-$", "");	// remove '-' char from the subdiv name end, if exists
        m.put("PropertyIdentificationSet.SubdivisionName",tmp);
    }
    
    public static void subdivisionNameKsJohnsonTR(ResultMap m,long searchId) throws Exception{
        
        String tmp = (String) m.get("tmpLegalDescription");
        if (tmp == null || tmp.length() == 0){
        	return;
        }
        
        tmp = tmp.replaceAll("\\n|\\r","");
        tmp = tmp.replaceAll("\\s{2,}"," ");
        int is=-1, ie=-1;
        is = tmp.indexOf("<TR");
        ie = tmp.indexOf("</TR>");
        if ((is!=-1) && (ie!=-1)) {
            tmp = tmp.replaceAll(tmp.substring(is, ie +"</TR>".length()),"");
        }
        
        tmp = tmp.replaceAll("(?i)<(.*?)>","");
        //  String subd = (String) m.get("PropertyIdentificationSet.SubdivisionName");
        tmp = tmp.replaceAll("\\n|\\r","");
        tmp = tmp.replaceAll("\\s{2,}"," ");
        
        m.put("PropertyIdentificationSet.PropertyDescription", tmp.trim());
        
        tmp = tmp.replaceAll("\\sTHRU\\s","-");
        tmp = tmp.trim();
        
        // subdivision name exceptions
        tmp = tmp.replaceAll("TR LOT","LOT"); //PID=DP30000001 0010B, bug #493
        tmp = tmp.replaceAll(" ST. ANDREWS", " ST ANDREWS");
        if(tmp.contains("RESURVEY AND REPLAT OF LOTS 18,23,24,25,26,27 & 28 CEDAR HILLS")){ //PID=DP13100000 000J, bug #493
        	m.put("PropertyIdentificationSet.SubdivisionName", "RESURVEY AND REPLAT OF LOTS 18, 23, 24, 25, 26, 27");
        } else if(tmp.startsWith("LOUISBURG SQUARE TR")){ //PID=NP39200000 0003B, bug #493
        	m.put("PropertyIdentificationSet.SubdivisionName", "LOUISBURG SQUARE");
        } else if(tmp.contains("OLATHE LTS 24 25 & 26 BLK 37 & S 1/2 VAC ALLEY ADJ LT 26 ON N")){ //PID DP52000037 0024, bug 561
        	m.put("PropertyIdentificationSet.SubdivisionName", "OLATHE");
        } else {
	        String subd = tmp;
	        //subd = subd.replaceAll("(RESURVEY(?: AND REPLAT)? OF LO?TS? (?:\\d+|\\s*(?:,|&|-|\\bAND\\b|\\bTO\\b)\\s*)+)(.+)", "$2 $1"); //PID=DP13100000 000J
	            
	        String[] tokens = {" LT "," LTS "," LOT "," E "," W "," S "," N "," BLK ", " TRACTS ", " TRACT ", " TR ",
	                " BLOCK "," PT "," BG "," EX "," NW "," NW","  CERTIFICATE ", 
	                " CERT ", //PID=NP86640001 0003, bug #620
	                " (" ," 1ST "," 2ND ", "3RD", " FIRST " , " SECOND ", " THIRD ", " SQUARE ", " SLY " , " UNIT ",
	                " ADDITION TO " //PID=CP03000000 0001C, bug #493
	                }; 
		//	String legalRef = (String) m.get("PropertyIdentificationSet.LegalRef");
	        
	        //B3079
	        String lgl = tmp.replaceAll("(?is)(.+) CERTIFICATE.*", "$1");
	        if (lgl.contains("COLONY III OF FOUR COLONIES"))
	        	m.put("PropertyIdentificationSet.SubdivisionName",lgl);
			
	        String r = LegalDescription.extractSubdivisionNameUntilToken(subd, tokens);
			if (!r.equals(subd)){
			    if ((r.indexOf("-")>-1) && (r.lastIndexOf("-")!=(r.indexOf("-")))) {
			        m.put("PropertyIdentificationSet.SubdivisionSection",r.substring(0,r.indexOf("-")));
			        r= "";   
			    }
			    r = r.replaceAll("\\b[EWNS]\\s*\\d+/\\d+", ""); //PID QP26000000 0007A, bug #493
			    r = r.replaceAll("\\bNO\\.?\\s+\\d+", ""); // fix for bug #1330 			    
			    r = r.replaceAll("\\b(ADDITION|(?<!')S|N|E|W|\\d+|REPL?(ATS?)?|PLATS?|PL?T?|II?I?|I?VI?I?I?|I?XI?I?|RES(URVEY)?)\\b", ""); //bug #493
			    r = r.replaceAll(
	    				"\\b(\\d+ST|\\d+ND|\\d+RD|\\d+TH|FOURTH|FIFTH|SIXTH|SEVENTH|\\WEIGHTH?|NINTH|TENTH|TWELFTH|TWENTIETH|EIGHTY-FIFTH|FIFTY-SEVENTH|FIFTY-NINTH)\\b", "");
		        r = r.replaceAll("\\s{2,}"," ");
			    r = r.trim();
			    if (r.equals("TOWN OF OLATHE")) r = "CITY OF OLATHE";
			    r = r.replaceAll(" LOTS TO$", "");
			    if (lgl.contains("COLONY III OF FOUR COLONIES"))
		        	m.put("PropertyIdentificationSet.SubdivisionName",lgl);
			    else
			    	m.put("PropertyIdentificationSet.SubdivisionName",r);
			} else { 
				Pattern p = Pattern.compile("(.+? CONDOMINIUM)\\b.*"); // fix for bug #1905
				Matcher ma = p.matcher(subd);
				if (ma.find()){
					r = ma.group(1);
					m.put("PropertyIdentificationSet.SubdivisionName", r);
					m.put("PropertyIdentificationSet.SubdivisionCond", r);
				}
			}
			
        }
                
		//Lot Number
        
        //lot exceptions
        if(tmp.contains("OLATHE LTS 24 25 & 26 BLK 37 & S 1/2 VAC ALLEY ADJ LT 26 ON N")){ //PID DP52000037 0024, bug 561
        	m.put("PropertyIdentificationSet.SubdivisionLotNumber", "24-26");
        } else {
			int lotIndex=-1;
			
			String [] lotSeparators = {" LT "," LOT ", " UNIT "};
			/*int []indexDelims = new int[lotSeparators.length];
			String lot = "";
				
			int minIndex = 0;
			
			for (int i=0; i<lotSeparators.length;i++) {
			    indexDelims[i] =  subd.indexOf(lotSeparators[i]);
			    if (indexDelims[i]==-1) indexDelims[i]= subd.length() +1;
			}
			for(int i=0;i<lotSeparators.length;i++) {
			    if (indexDelims[i] < indexDelims[minIndex]) minIndex= i;
			}
			lotIndex = subd.indexOf(lotSeparators[minIndex]);
				if (lotIndex !=-1) {
				    lotIndex = lotIndex + lotSeparators[minIndex].length();
				    if ((minIndex==0)&& (subd.charAt(lotIndex)=='S')) lotIndex++;
				    while (subd.charAt(lotIndex)==' ') lotIndex++;	
					lot = subd.substring(lotIndex,subd.indexOf(" ",lotIndex));
					lot=lot.trim();
				}*/
			int []indexDelims = new int[lotSeparators.length];
			String lot = "";
				
			int minIndex = 0;
			
			for (int i=0; i<lotSeparators.length;i++) {
			    indexDelims[i] =  tmp.indexOf(lotSeparators[i]);
			    if (indexDelims[i]==-1) indexDelims[i]= tmp.length() +1;
			}
			for(int i=0;i<lotSeparators.length;i++) {
			    if (indexDelims[i] < indexDelims[minIndex]) minIndex= i;
			}
			
			
			
			lotIndex = tmp.indexOf(lotSeparators[minIndex]);
	
			if (lotIndex !=-1) {
				    lotIndex = lotIndex + lotSeparators[minIndex].length();
				    while (tmp.charAt(lotIndex)==' ') lotIndex++;	
					if (tmp.indexOf(" ",lotIndex)!=-1) {
					    lot = tmp.substring(lotIndex,tmp.indexOf(" ",lotIndex));
					}else {
					    lot = tmp.substring(lotIndex,tmp.length());
					}
					lot = lot.replaceAll(",","");
					lot=lot.trim();
					if (lotSeparators[minIndex].equals(" LT ")) {
					    int lotIndex2;
					    int lotEndIndex;
					    lotIndex2 = tmp.indexOf(" LT ",lotIndex+1);
					    while (lotIndex2!=-1) {
					        lotIndex2 = lotIndex2 + " LT ".length();
					        lotEndIndex = tmp.indexOf(" ",lotIndex2+1);
					        if (lotEndIndex!=-1) {
					            lot = lot + " "+tmp.substring(lotIndex2,lotEndIndex);
					            lotIndex2 = tmp.indexOf(" LT ",lotEndIndex+1);
					        }else {
					            lot = lot + " "+tmp.substring(lotIndex2,tmp.length());
					            lotIndex2 =-1;
					        }
					    }
					}else {
					    if (lotSeparators[minIndex].equals(" LOT ")) {
						    int lotIndex2;
						    int lotEndIndex;
						    lotIndex2 = tmp.indexOf(" LOT ",lotIndex+1);
						    while (lotIndex2!=-1) {
						        lotIndex2 = lotIndex2 + " LOT ".length();
						        lotEndIndex = tmp.indexOf(" ",lotIndex2+1);
						        if (lotEndIndex!=-1) {
						            lot = lot + " "+tmp.substring(lotIndex2,lotEndIndex);
						            lotIndex2 = tmp.indexOf(" LOT ",lotEndIndex+1);
						        }else {
						            lot = lot + " "+tmp.substring(lotIndex2,tmp.length());
						            lotIndex2 =-1;
						        }
						    }
						}
					}
					lot = lot.trim();
				}else {
				    if (tmp.indexOf(" LTS ")!=-1) {
				        lotIndex = tmp.indexOf(" LTS ") + " LTS ".length();
					    
					    while (tmp.charAt(lotIndex)==' ') lotIndex++;	
						if (tmp.indexOf(" ",lotIndex)!=-1) {
						    int lotEndIndex;
						    lotEndIndex = tmp.indexOf(" ",lotIndex);
						    if (tmp.indexOf(" ",lotEndIndex+1)!=-1) {
						        boolean b1=true,b2=true;
						        String s1="";
						        	do
								    {
						            	s1= tmp.substring(lotEndIndex+1,tmp.indexOf(" ",lotEndIndex+1));
						            	b1 = s1.matches("[A-Z&-]{1}");
						            	b2 = s1.matches("\\d+");
								        lotEndIndex = tmp.indexOf(" ",lotEndIndex+1);
								    }while((b1||b2)&&(lotEndIndex!=-1)&&(tmp.indexOf(" ",lotEndIndex+1)!=-1));
						        	
						        	if (b1||b2) {
						        	    if ((lotEndIndex!=-1)&&(tmp.indexOf(" ",lotEndIndex+1)!=-1)) {
									        lot = tmp.substring(lotIndex,lotEndIndex);
									    }else {
									        lot = tmp.substring(lotIndex,tmp.length());
									    }
						        	}else {
						        	    lotEndIndex = lotIndex + tmp.substring(lotIndex,lotEndIndex-1).lastIndexOf(" ");
						        	    if ((lotEndIndex!=-1)&&(tmp.indexOf(" ",lotEndIndex+1)!=-1)) {
									        lot = tmp.substring(lotIndex,lotEndIndex);
									    }else {
									        lot = tmp.substring(lotIndex,tmp.length());
									    }
						        	}
										    
								    
								    
						    }else {
						        lot = tmp.substring(lotIndex,tmp.length());
						    }
						}else {
						    lot = tmp.substring(lotIndex,tmp.length());
						}
						lot=lot.trim();
				    }
				}
			
				if ((lot.indexOf("&")!=-1)&& (lot.indexOf("&")+1)==lot.length()) {
				    lot=lot.replaceAll("&","");
				    lot = lot.trim();
				}
				lot = StringFormats.RemoveDuplicateValues(lot);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber",lot);
        }
		
		//tract
			int tractIndex=-1;
			
			String [] tractSeparators = {" TRACT ", " TR "};
			int []indextractDelims = new int[tractSeparators.length];
			String tract = "";
			
			int minIndx = 0;
			
			for (int i=0; i<tractSeparators.length;i++) {
			    indextractDelims[i] =  tmp.indexOf(tractSeparators[i]);
			    if (indextractDelims[i]==-1) indextractDelims[i]= tmp.length() +1;
			}
			for(int i=0;i<tractSeparators.length;i++) {
			    if (indextractDelims[i] < indextractDelims[minIndx]) minIndx= i;
			}
			tractIndex = tmp.indexOf(tractSeparators[minIndx]);
			if (tractIndex !=-1) {
			    tractIndex = tractIndex + tractSeparators[minIndx].length();
			    tract = tmp.substring(tractIndex,tmp.indexOf(" ",tractIndex));
			    tract=tract.trim();
			}
			if (!tract.matches("[A-Z]{2,}")) {
				m.put("PropertyIdentificationSet.SubdivisionTract",tract);
			}
			//block
	        String block = "";
	        String blkSeparators[] = {"BLK"};
			int blkIndex;
			for (int i=0;i<blkSeparators.length;i++) {
			    if (tmp.indexOf(blkSeparators[i])!=-1) {
			        blkIndex = blkSeparators[i].length() + tmp.indexOf(blkSeparators[i]);
			        while (tmp.charAt(blkIndex)==' ') blkIndex++;
			        int end = tmp.indexOf(" ",blkIndex);
			        if (end ==-1) end = tmp.length();
			        block = tmp.substring(blkIndex, end);
			        block = block.trim();
			    }
			}
			if(block.length() != 0){
				m.put("PropertyIdentificationSet.SubdivisionBlock",block);
			}
			
		// extract UNIT
		Pattern p = Pattern.compile("\\b(?:APT|UNIT) (\\w+)");
		Matcher ma = p.matcher(tmp);
		if (ma.find()){
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1));
		}
		
		// extract BLDG
		p = Pattern.compile("\\bBLDG (\\w+)");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()){
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}
    }
    
    public static void cityKSJohnsonTR(ResultMap m,long searchId) throws Exception{
        String tmp = (String) m.get("PropertyIdentificationSet.City");
        String city = "";
        if (tmp!=null) {
            tmp = tmp.trim();
            int t;
            String tmpCity="";
            if (( t = tmp.indexOf(" "))!=-1) {
                tmpCity = tmp.substring(0,t);
            }else {
                tmpCity = tmp;
            }
            
            if (tmpCity=="DEC") city = "De Soto";
            if (tmpCity=="EDC") city = "Edgerton";
            if (tmpCity=="FAC") city = "Fairway";
            if (tmpCity=="GAC") city = "Gardner";
            if (tmpCity=="LQC") city = "Lake Quivira";
            if (tmpCity=="LWC") city = "Leawood";
            if (tmpCity=="LEC") city = "Lenexa";
            if (tmpCity=="MEC") city = "Merriam";
            if (tmpCity=="MIC") city = "Mission";
            if (tmpCity=="MHC") city = "Mission Hills";
            if (tmpCity=="MWC") city = "Mission Woods";
            if (tmpCity=="OLC") city = "Olathe";
            if (tmpCity=="OPC") city = "Overland Park";
            if (tmpCity=="PVC") city = "Prairie Village";
            if (tmpCity=="RPC") city = "Roeland Park";
            if (tmpCity=="SHC") city = "Shawnee";
            if (tmpCity=="SPC") city = "Spring Hill";
            if (tmpCity=="WWC") city = "Westwood";
            if (tmpCity=="WHC") city = "Westwood Hills";	    
            if (tmpCity=="AU") city = "Aubry";	    
            if (tmpCity=="OX") city = "Oxford";
            if (tmpCity=="LE") city = "Lexington";
            if (tmpCity=="GA") city = "Gardner";
        }
        m.put("PropertyIdentificationSet.City",city);
    }
    
    
    public static void namesKSJohnsonCO(ResultMap m,long searchId) throws Exception {
    	  
        if(m.get("tmpCivilPlaintiff")!=null) {
        	String[] plaintiff = ((String)m.get("tmpCivilPlaintiff")).split("@@");
        	ResultTable parties = addNamesKSJohnsonCO(plaintiff);
        	m.put("GrantorSet",parties);
        	m.put("SaleDataSet.Grantor", cleanupNames((String)m.get("tmpCivilPlaintiff")));
        } else {
        	m.put("SaleDataSet.Grantor", "");
        }
        
        if(m.get("tmpCivilDefendant")!=null) {
        	String[] defendant = ((String)m.get("tmpCivilDefendant")).split("@@");
        	ResultTable parties = addNamesKSJohnsonCO(defendant);
        	m.put("GranteeSet",parties);
        	m.put("SaleDataSet.Grantee", cleanupNames((String)m.get("tmpCivilDefendant")));
        } else {
        	m.put("SaleDataSet.Grantee", "");
        }
    }
    
    public static void courtsJohnsonKS(ResultMap m,long searchId) throws Exception{
        
    	/* Criminal/Juvenile/Traffic */
        if (((String) m.get("tmpDocType1"))!=null) {
            m.put("SaleDataSet.DocumentType","Criminal/Juvenile/Traffic");
            
            String tmpDate2 = (String) m.get("tmpDate2");
            String tmpDate = (String) m.get("tmpDate");
            
            if (tmpDate2!=null && tmpDate2.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
            	m.put("SaleDataSet.RecordedDate",tmpDate2);
            }else if (tmpDate !=null && tmpDate.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            	m.put("SaleDataSet.RecordedDate",tmpDate);
            }
        }
        
        /* Marriage License */
        if (((String) m.get("tmpDocType2"))!=null) {
            m.put("SaleDataSet.DocumentType","Marriage License");
            String tmpDate3 = (String) m.get("tmpDate3");
            if (tmpDate3!=null) {
                tmpDate3 = tmpDate3.replaceAll("Application Date:","").trim();
                if (tmpDate3.matches("\\d{1,2}/\\d{1,2}/\\d{4}"))
                {
                    m.put("SaleDataSet.RecordedDate",tmpDate3);
                }
                else if(tmpDate3.matches("\\d{4}"))
                {
                    m.put( "SaleDataSet.RecordedDate","01/01/" + tmpDate3 );
                }
            }
        }
        /* Probate */
        if (((String) m.get("tmpDocType3"))!=null && m.get("SaleDataSet.DocumentType")!=null) {
        	m.put("SaleDataSet.DocumentType",((String)m.get("SaleDataSet.DocumentType")).replaceFirst("_",""));
        } else {
        	m.put("SaleDataSet.DocumentType",((String)m.get("SaleDataSet.DocumentType")).replaceFirst("(?is)^_",""));
        }
        
        /* Other Dates */
        if ((((String) m.get("tmpDocType1"))==null)&& (((String) m.get("tmpDocType2"))==null)) {
            String tmpDate = (String) m.get("tmpDate");
            if (tmpDate!=null) {
                if (tmpDate.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                    m.put("SaleDataSet.RecordedDate",tmpDate);
                }
            }
        }
        
        if(m.get("tmpCivilPlaintiff")!=null) {
        	String[] plaintiff = ((String)m.get("tmpCivilPlaintiff")).split("@@");
        	ResultTable parties = addNamesKSJohnsonCO(plaintiff);
        	m.put("GrantorSet",parties);
        	m.put("SaleDataSet.Grantor", cleanupNames((String)m.get("tmpCivilPlaintiff")));
        } else {
        	m.put("SaleDataSet.Grantor", "");
        }
        
        if(m.get("tmpCivilDefendant")!=null) {
        	String[] defendant = ((String)m.get("tmpCivilDefendant")).split("@@");
        	ResultTable parties = addNamesKSJohnsonCO(defendant);
        	m.put("GranteeSet",parties);
        	m.put("SaleDataSet.Grantee", cleanupNames((String)m.get("tmpCivilDefendant")));
        } else {
        	m.put("SaleDataSet.Grantee", "");
        }
    }
    
    private static Object cleanupNames(String names) {
    	String cleanedNames = names;
    	cleanedNames = cleanedNames.toUpperCase();
    	cleanedNames = cleanedNames.replaceFirst("(?is),\\s*$", "");
    	cleanedNames = cleanedNames.replaceAll(",?ET AL","")
				   .replaceAll("(AKA)", "");
    	cleanedNames = cleanedNames.replaceAll("(?is)\\bCHANGE OF NAME\\s*(?:/?\\bCOURT\\b)?", "");
    	cleanedNames = cleanedNames.replaceAll("(?is)^@@","");
    	cleanedNames = cleanedNames.replaceAll("@@"," / ");
    	
		return cleanedNames;
	}

	public static ResultTable addNamesKSJohnsonCO(String[] names)  {
    	ResultTable parties = new ResultTable();
    	List<List> body = new ArrayList<List>();
    	String [] header = new String[3];
    	 
		for(String name : names ) {
			name = name.toUpperCase();
			name = name.replaceFirst("(?is),\\s*$", "");
			name = name.replaceAll(",?ET AL","")
					   .replaceAll("(AKA)", "");
			name = name.replaceAll("(?is)\\bCHANGE OF NAME\\s*(?:/?\\bCOURT\\b)?", "");
	       
	        String[] parsed = StringFormats.parseNameNashville(name);
	        /* String[] suffixes = extractNameSuffixes(parsed); */        			
			addOwnerNames(parsed, body);
		}
		        
		Map<String,String[]> map = new HashMap<String,String[]>();
		map.put("OwnerFirstName", new String[]{"OwnerFirstName", ""});
		map.put("OwnerMiddleName", new String[]{"OwnerMiddleName", ""});
		map.put("OwnerLastName", new String[]{"OwnerLastName", ""});
		
		header[0] = "OwnerFirstName";
		header[1] = "OwnerMiddleName";
		header[2] = "OwnerLastName";
		
	   	if (!body.isEmpty()){
	   		try {
				parties.setHead(header);
				parties.setBody(body);
				parties.setMap(map);
	   		 }catch(Exception e) {
	   			 e.printStackTrace();
	   		 }
	    }
	   	
		return parties;
    }
    
    public static void instrumentJohnsonRO(ResultMap m,long searchId) throws Exception{
        String tmp1 = (String) m.get("tmpInstrument1");
        String tmp2 = (String) m.get("tmpInstrument2");
        if ((tmp1==null)||(tmp2==null)) {
            return;
        }
        
        if (tmp1.matches("\\d+")) {
            m.put("SaleDataSet.InstrumentNumber",tmp1);
        }else {
            m.put("SaleDataSet.InstrumentNumber",tmp2);
        }
    }
    
    //  rotherford ro parsing
    //Section Phase Lot Building Unit Acres District Instr.Type Date Class
    // Bk-Pg/File#
    //0- name
    //1- sec
    //2- phase
    //3- lot
    //4- building
    //5- unit
    //6- acres
    //7- district
    //8- REF
    protected static final String hdrsRuthRO[] = { 
    	null, 
    	"(?:SEC(?:TION)?|SC)", 
    	"PHASE",
    	"\\b(?:\\d+\\s+)?L(?:O?)T(?:S?)(?:/?)", 
    	null, 
    	"(?:UNIT|UNITS)",
    	"ACRES?", 
    	"DISTRI(?:C?)(?:T?)", "" };

    protected static final String dataformatRuthRO[] = { 
    	null, 
    	"(?:\\d+[-\\s]?[A-Z]{1}\\b|\\d+|[A-Z]{1,2}\\b|[IVX]+\\b)", 
    	"\\d+(?:-?[A-Z])?\\b|[IVX]+\\b",
    	"((\\d+[A-Z]+|\\d+)(\\s+&\\s+|,|-\\s*|\\s*THRU\\s*|\\s*))*", 
    	null,
    	"((([A-Z])?(-)?\\d*(-)?([A-Z])?)(-|\\s+&\\s*|\\s*))*",
    	"(\\d+(\\s+&\\s+|,|-|\\.|\\s*))*", "\\d*",
    	"((B|A)(-?)(\\d+/\\d+)(\\s+&\\s*|,|\\s*))*" };

    protected static String cleanupruthro(String s,long searchId) {
        String rez = s;
        rez = s.replaceAll("[(),\\&]", " ");
        rez = rez.replaceAll("\\s+", " ");
        rez = rez.trim();
        return rez;
    }

    protected static String cleanupsubdivnameruthro(String s,long searchId) {
        String rez = s;
        String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
        if ("Cheatham".equalsIgnoreCase(county)) {
        	rez = s.replaceAll("[(),]", " ");
        } else {
        	rez = s.replaceAll("[(),\\&]", " ");
        }
        rez = rez
                .replaceAll(
                        "\\s*\\b(CONDOS|PART|OF|DISTRI(C?)(T?)|RESUBD(IVISION)?|CONDOMIN(-?\\s?)IUMS|VILLAGE|ADJ(ACENT)?|PARCEL|TRACT)\\b\\s*",
                        " ");
        rez = rez.replaceAll("\\s*/\\s*", " ");        
        rez = rez.replaceAll("\\s+", " ");
        rez = rez.trim();
        return rez;
    }

    public static String[] SubdivparseRuthRO(String s,long searchId) throws Exception {
        String rez[] = null;
        rez = new String[hdrsRuthRO.length];

        for (int i = 0; i < rez.length; rez[i++] = null)
            ;

        if (s == null || s.trim().length() == 0)
            return rez;

        String tmp = s.toUpperCase().trim();
        ///cleaning
        tmp = tmp.replaceFirst("^\\d+ LOTS?\\b\\s*", ""); // for bug #2245
        tmp = tmp.replaceAll(PLAT_BOOK_PAGE_PATTERN, "");
        tmp = tmp.replaceAll("(\\s+)?REF\\s+", " ");
        tmp = tmp.replaceAll("\\(RESUBD(/?)\\)(\\s+?)(\\d+?)", " ");
        tmp = tmp.replaceAll("\\s*\\b(RE)?SUB(D(IVISION)?)?\\b\\s*", " ");
        tmp = tmp.replaceAll("(\\s+)?S/D\\s+", " ");
        tmp = tmp.replaceAll("(\\s+)?ESTATES\\s+", " ");
        tmp = tmp.replaceAll("(\\s+)?REVISED\\s*", " ");
        tmp = tmp.replaceAll("(\\s+)?ETC\\s*", " ");
        tmp = tmp.replaceAll("(\\s+)?COURT\\s*", " ");
        tmp = tmp.replaceAll("(\\s+)?ANENX\\s*", " ");
        tmp = tmp.replaceAll("(\\s+)?ANNEX\\s*", " ");
        tmp = tmp.replaceAll("\\bREPLAT\\b", " ");
        tmp = tmp.replaceAll("\\b(SECTION)(\\w)\\b", "$1 $2");
        tmp = tmp.replaceAll("\\(RESUBD|CARPORT|AMENDED|REVISED\\)", " ");
        tmp = tmp.replaceAll("CONDOMINIUM(S?)\\s*(\\d+?)", " ");
        tmp = tmp.replaceAll("(\\s+)?\\d+\\s*LOTS$", " ");
        tmp = tmp.replaceAll("\\d+\\s*LOTS/\\d+\\s*UNITS$", " ");
        tmp = tmp.replaceAll("B-", "B");
        tmp = tmp.replaceAll("(?i)\\bS/D\\b", "SD");
        tmp = tmp.replaceAll("(?i)\\bALSO\\b", "");
        
        //indian style coding cause Regex sucks
        //extracting a ref special case
        rez[8] = tmp.replaceAll(".*([AB]\\d+/\\d+)$", "$1");
        if (rez[8] != null && !rez[8].equals(tmp))
            tmp = tmp.replaceAll(rez[8], "");
        else
            rez[8] = null;

        for (int i = 0; i < dataformatRuthRO.length; i++) {
            if ((hdrsRuthRO[i] != null && dataformatRuthRO[i] != null && rez[i] == null)
                    || (i == 8)) //no data parsed yet
            {
                Pattern pt = Pattern.compile(hdrsRuthRO[i] + "\\s+("+ dataformatRuthRO[i] + ")");
                Matcher m = pt.matcher(tmp);
                int correctionLength = 0;
                int sizeBefore = 0;
                if (m.find()) {
                    String t = m.group(1);
                    sizeBefore = tmp.length();
                    if (t != null && t.trim().length() > 0) {
                        int ist, iend;
                        ist = m.start();
                        iend = m.end();
                        tmp = tmp.substring(0, ist) + tmp.substring(iend);
                    } else if (i!=6){		//task 9838, do not remove ACRES from RACHEL ACRES (subdivision name)
                        tmp = tmp.replaceAll(hdrsRuthRO[i], "");
                        
                    }
                    correctionLength += sizeBefore - tmp.length();
                    if (i == 8)
                        t = (rez[i] == null) ? t : (rez[i] + " " + t);
                    rez[i] = cleanupruthro(t,searchId);
                    if("0".equals(rez[i])) {
                    	rez[i] = "";
                    }
                    try {
	                    if(i == 1) {
		                    while(m.find()){
		                        t = m.group(1);
		                        sizeBefore = tmp.length();
		                        if (t != null && t.trim().length() > 0) {
		                            int ist, iend;
		                            ist = m.start() - correctionLength;
		                            iend = m.end() - correctionLength;
		                            tmp = tmp.substring(0, ist) + tmp.substring(iend);
		                        } else {
		                            tmp = tmp.replaceAll(hdrsRuthRO[i], "");
		                        }
		                        correctionLength += sizeBefore - tmp.length();
		                        //if (i == 8)
		                        //    t = (rez[i] == null) ? t : (rez[i] + " " + t);
		                        rez[i] += " " + cleanupruthro(t,searchId);
		                        
		                    }
		                    if("0".equals(rez[i])) {
		                    	rez[i] = "";
		                    }
		                    if(rez[i].matches("\\d+\\s+[a-zA-Z]+")) {
		                    	rez[i] = rez[i].replaceAll("\\s+", "-");
		                    }
	                    }
                    } catch (Exception e) {
						e.printStackTrace();
					}
                } else if (i==6) { //for acres
                	pt = Pattern.compile("(" + dataformatRuthRO[i] + ")\\s+" + Pattern.compile(hdrsRuthRO[i]));
                    m = pt.matcher(tmp);
                    if (m.find()) {
                    	String t = m.group(1);
                    	if (t != null && t.trim().length() > 0) {		//task 9838, do not remove ACRES from RACHEL ACRES (subdivision name)
                    		rez[i] = t;
                        	if("0".equals(rez[i])) {
                            	rez[i] = "";
                            }
                        	tmp = tmp.replace(m.group(0), "");
                    	}
                    }
                }
            }
        }
        rez[0] = cleanupsubdivnameruthro(tmp.trim(),searchId);
        return rez;
    }

    public static void stdPisRutherfordInterRO(ResultMap m,long searchId) throws Exception {
        //PropertyIdentificationSet.SubdivisionLotNumber
        String subd = (String) m.get("PropertyIdentificationSet.SubdivisionName");
        String legalRef = (String) m.get("PropertyIdentificationSet.LegalRef");
        String r[] = SubdivparseRuthRO(subd,searchId);
        String s[] = SubdivparseRuthRO(legalRef,searchId);
        //////////////////////////////////////
        //subdiv name cleanup
        if (r[0] != null) {
            r[0] = r[0].replaceAll("[\\-/]", " ");
            r[0] = r[0].replaceAll("CONDOS", " ");
            r[0] = r[0].replaceAll("DISTRI(C?)(T?)", " ");
            r[0] = r[0].replaceAll("CONDOMINIUM(S?)", " ");
            r[0] = r[0].replaceAll("\\s+", " ").trim();
            m.put("PropertyIdentificationSet.SubdivisionName", r[0]);
        }
        ////section
        String sec = (String) m
                .get("PropertyIdentificationSet.SubdivisionSection");
        if (r[1] != null) {
            if (sec == null
                    || (sec != null && (sec.trim().length() == 0 || sec.trim()
                            .equals("0"))))
                m.put("PropertyIdentificationSet.SubdivisionSection", r[1]);
        }
        ///phase
        if (r[2] != null)
            m.put("PropertyIdentificationSet.SubdivisionPhase", r[2]);
        //Unit
        if (r[5] != null)
            m.put("PropertyIdentificationSet.SubdivisionUnit", r[5]);
        //Lots
        if (r[3] != null) {
            r[3] = r[3].replaceAll("[\\-/]", " ");
            Pattern range = Pattern.compile("(\\d+)\\s*(THRU|TH)\\s*(\\d+)");
            Matcher ma = range.matcher(r[3]);
            if (ma.find()) {
                int start = Integer.parseInt(ma.group(1));
                int end = Integer.parseInt(ma.group(3));
                String rg = "";
                for (int i = start; i <= end; i++) {
                    rg += new Integer(i).toString() + " ";
                }
                r[3] = r[3].substring(0, ma.start()) + " " + rg
                        + r[3].substring(ma.end());
            }
            r[3] = r[3].replaceAll("\\s+", " ").trim();
            m.put("PropertyIdentificationSet.SubdivisionLotNumber", r[3]);
        }
        if (s[8] != null) {
            if (r[8] != null && !s[8].equals(r[8]))
                r[8] = r[8] + " " + s[8];
            if (r[8] == null)
                r[8] = s[8];
        }
        //Refs
        if (r[8] != null)
            m.put("PropertyIdentificationSet.BookPageRefs", r[8]);
        
        if (r[8] != null) {
            
	        // crossRefSet
	        ResultTable rt = (ResultTable) m.get("CrossRefSet");
	        List b2 = new ArrayList();
	        if (rt == null) {
	            
	            rt = new ResultTable();
	            
	            m.put("CrossRefSet", rt);
	            String[] h = new String[] { "Recording Class", "Book & Page/Filing #", "Description", 
	                    "Recording Date & Time", "Instrument Number", "Book", "Page" };
	            rt.setHead(h);
	            
	            HashMap hm = new HashMap();
	            hm.put("InstrumentNumber", new String[] {"Instrument Number", "iw"} );
	            hm.put("Book_Page", new String[] {"Book & Page/Filing #", "iw"} );
	            hm.put("Book", new String[] {"Book", "iw"} );
	            hm.put("Page", new String[] {"Page", "iw"} );
	            
	            rt.setMap(hm);
	        } else {
	            rt.setReadWrite();
	            String[][] bv = rt.getBody();
	            for (int i = 0; i < bv.length; i++)
	                b2.add(Arrays.asList(bv[i]));
	        }
	
	        StringTokenizer st = new StringTokenizer(r[8], " ");
	        while (st.hasMoreTokens()) {
	        
	            String token = st.nextToken();
	            
	            String book = token.replaceAll("(.*)[ /-](.*)", "$1");
	            String page = token.replaceAll("(.*)[ /-](.*)", "$2");
	            
	            if (book.length() != 0 || page.length() != 0) {
	                String[] row = new String[] { "", book + "_" + page, "", "", "", book, page,"" };
	                b2.add(Arrays.asList(row));
	            }
	        }
	        
	        rt.setBody(b2);
	        rt.setReadOnly();
	        
	        m.put("CrossRefSet", rt);
        }
        
        
        composeSubdivision(m,searchId);
    }
    
    public static void stdPisRutherfordAO(ResultMap m,long searchId) throws Exception {
    	
    	String name = (String) m.get("PropertyIdentificationSet.OwnerLastName");
    	if (name == null || name.length() == 0)
    		return;
    	
    	// most of the names are formatted as L F M, but there are some exceptions
    	String names[] = StringFormats.parseNameNashville(name);
    	if (names[0].startsWith("MC")){ // e.g. name = TIMOTHY G MCCLANAHAN and MCCLANAHAN is extracted as first name (bug #1705) => interchange LN with FN 
    		String temp = names[0];
    		names[0] = names[2];
    		names[2] = temp;
    		
    		if (names[5].equals(names[0])){
    			names[5] = names[2];
    		}
    	}
        m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", names[5]);      	
    }
    
    public static void stdPisRutherfordEP(ResultMap m,long searchId) throws Exception {
        String subdivName = (String) m.get("PropertyIdentificationSet.SubdivisionName");
        subdivName = subdivName.replaceFirst("(CONDO(MINI?UMS?)?)", " ");		//Bug 2158
        m.put("PropertyIdentificationSet.SubdivisionName",subdivName);
    }
    
    protected static String cleanSubdivTNRutherfordRO (String subdiv){
    	subdiv = subdiv.replaceAll("[\\-/]", " ");
    	subdiv = subdiv.replaceAll("CONDOS", " ");
    	subdiv = subdiv.replaceAll("DISTRI(C?)(T?)", " ");
    	subdiv = subdiv.replaceAll("CONDOMINIUM(S?)", " ");    	
    	subdiv = subdiv.replaceAll("\\s+", " ").trim();
        
        return subdiv;
    }
    
    protected static String cleanLotTNRutherfordRO (String lot){    	
    	lot = lot.replaceAll("(\\d+/\\d+\\s+)LOT", "$1");	//1/2 LOT 2 (TNRobertsonST, instr# 62397)
    	lot = lot.replaceAll("(\\d+)-(\\d+)", "$1 THRU $2");
    	lot = lot.replaceAll("[\\-/]", " ").replaceAll("\\bLOTS?\\b", "").replaceAll("T R A C T", "");
    	lot = lot.replaceAll("(?is)\\bT\\s*R\\s*A?\\s*C\\s*T\\s*S?\\b", "");
    	lot = lot.replaceAll("(?is)\\bE\\s*A\\s*S\\s*E\\b", "");
    	lot = lot.replaceAll("(?is)\\bR\\s*O\\s*W\\b", "");
    	lot = lot.replaceAll("(?is)PART OF", " ");
    	lot = lot.replaceAll("#", " ");
    	lot = lot.replaceAll("\\s*&\\s*", " ");
	    Pattern range = Pattern.compile("(\\d+)\\s*(THRU|TH)\\s*(\\d+)");
	    Matcher ma = range.matcher(lot);
	    if (ma.find()) {
	        int start = Integer.parseInt(ma.group(1));
	        int end = Integer.parseInt(ma.group(3));
	        String rg = "";
	        for (int j = start; j <= end; j++) {
	            rg += new Integer(j).toString() + " ";
	        }
	        lot = lot.substring(0, ma.start()) + " " + rg + lot.substring(ma.end());
	    }
	    lot = lot.replaceAll("\\s+", " ").trim();
	    lot = LegalDescription.cleanValues(lot, false, true);
	    return lot;
    }
    
    public static boolean areEqual(String[] a, List<String> b) {
    	if (a.length!=b.size()) {
    		return false;
    	}
    	for (int i=0;i<a.length;i++) {
    		String s1 = a[i];
    		String s2 = b.get(i);
    		if (s1==null) {
    			if (s2!=null) {
    				return false;
    			}
    		} else if (!s1.equals(s2)) {
    			return false;
    		} 
    	}
    	return true;
    }
    
    public static void stdPisRutherfordRO(ResultMap m,long searchId) throws Exception {
    	
        String legalRef = (String) m.get("PropertyIdentificationSet.LegalRef");
        String s[] = SubdivparseRuthRO(legalRef,searchId);
        String propertyDescr = legalRef;
        
        ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
        if (pis!=null) {		//remove identical rows from body
        	String[][] body = pis.getBody();
        	
        	for (String[] strings : body) {
				for (int i = 1; i < strings.length; i++) {
					strings[i] = strings[i].replaceFirst("^0+", "");
				}
			}
        	pis.setReadOnly(false);
        	pis.setBody(body);
        	pis.setReadOnly(true);
        	
        	if (body.length>1) {
        		List<List<String>> newBodyList = new ArrayList<List<String>>();
        		
        		for (int i=0;i<body.length;i++) {
        			boolean foundDuplicate = false;
        			
        			for (int j=0;j<newBodyList.size();j++) {
        				if (areEqual(body[i], newBodyList.get(j))) {
        					foundDuplicate = true;
        					break;
        				}
        			}
        			
        			if(!foundDuplicate) {
        				newBodyList.add(java.util.Arrays.asList(body[i]));
        			}
        		}
        		pis.setReadOnly(false);
        		pis.setBody(newBodyList);
        		pis.setReadOnly(true);
        		m.put("PropertyIdentificationSet", pis);
        	}
        }
        
        StringBuilder bprefs = new StringBuilder();
        if (s[8] != null && s[8].length()!=0) {
        	bprefs = bprefs.append(s[8]);
        }
        
        if (pis != null) {
            int len = pis.getLength();
    		
            for (int i=0; i<len; i++) {
            	String subd = pis.body[i][0];
            	
            	if (i == 0 && !"".equals(subd)){
            		propertyDescr =  subd;
            	}
            	
            	String r[] = SubdivparseRuthRO(subd,searchId);            	

                //subdiv name cleanup
                if (r[0] != null) {
                    r[0] = cleanSubdivTNRutherfordRO(r[0]);
                    pis.body[i][0] = r[0];
                }
                
                ////section
                String sec = pis.body[i][1];
                if (r[1] != null) {
                    if (sec == null || (sec != null && (sec.trim().length() == 0 || sec.trim().equals("0")))){
                    	pis.body[i][1] = r[1];
                    }
                }
                String[] exceptionTokens = {"M", "C", "L", "D"};
                pis.body[i][1] = Roman.normalizeRomanNumbersExceptTokens(pis.body[i][1], exceptionTokens); // convert roman numbers to arabics
                
                ///phase
                if (r[2] != null){
                	pis.body[i][2] = r[2];
                }
                
                //Unit
                if (r[5] != null){
                	pis.body[i][5] = r[5];
                }
                //Acres
                if (r[6] != null){
                	if (pis.body[i][6] == null || 
                			(pis.body[i][6] != null && (pis.body[i][6].trim().length() == 0 || pis.body[i][6].trim().equals("0")))){
                		pis.body[i][6] = r[6];
                	}
                }
                //District
                if (r[7] != null){
                	if (pis.body[i][7] == null || 
                			(pis.body[i][7] != null && (pis.body[i][7].trim().length() == 0 || pis.body[i][7].trim().equals("0")))){
                		pis.body[i][7] = r[7];
                	}
                }
                
                //Lots
                if (r[3] != null) {
                	r[3] = cleanLotTNRutherfordRO(r[3]);
                    pis.body[i][3] = Roman.normalizeRomanNumbersExceptTokens(r[3], exceptionTokens);
                } else {
                	pis.body[i][3] = pis.body[i][3]!=null?Roman.normalizeRomanNumbersExceptTokens(cleanLotTNRutherfordRO(pis.body[i][3]), exceptionTokens):"";
                }
                
                if (r[8] != null && !r[8].equals(s[8])) {
                	bprefs = bprefs.append(" ").append(r[8]);
                }                
            }
            
            // if no legal elements were extracted from Property Information section, 
            //try to extract them from grantee field (e.g. Plat Cabinet 14-168) - bug #2007
            boolean foundLegal = false;
            for (int i=0; i<len && !foundLegal; i++) {
            	foundLegal = 
            			(pis.body[i][0].length() != 0) || //name
            			(pis.body[i][1].length() != 0) || //section
            			(pis.body[i][3].length() != 0) || //lot
            			(pis.body[i][5].length() != 0);   //unit    	
            }
            if (!foundLegal){
            	String names = (String) m.get("SaleDataSet.Grantee");
            	if(StringUtils.isNotEmpty(names)) {
            		if(StringUtils.isNotEmpty((String) m.get("SaleDataSet.Grantor"))) {
            			names += "/" + m.get("SaleDataSet.Grantor");
            		}
            	} else {
            		names = (String) m.get("SaleDataSet.Grantor");
            	}
            		
            	if (!StringUtils.isEmpty(names)){
            		// identify the grantee that contain legal descr elements
            		String[ ] nameLines = names.split("/");
            		List<String> uniqueNameLines = new ArrayList<String>();
            		for (String nameLine : nameLines) {
						if(!uniqueNameLines.contains(nameLine.trim())) {
							uniqueNameLines.add(nameLine.trim());
						}
					}
            		
            		

            		List<String[]> rows = new ArrayList<String[]>();
            		
            		for (String uniqueLine : uniqueNameLines) {
						foundLegal = uniqueLine.matches(".*\\b(?:SEC(?:TION)?|SC|PHASE|L(?:O?)T(?:S?)|UNITS?)\\b.*");
            			if(foundLegal) {
            				String[] exceptionTokens = {"M", "C", "L", "D"};
                			String r[] = SubdivparseRuthRO(replaceNumbers(uniqueLine), searchId);
                			// subdivision name
                            if (!StringUtils.isEmpty(r[0])) {
                                r[0] = cleanSubdivTNRutherfordRO(r[0]);
                            }                  
                            // section
                            if (!StringUtils.isEmpty(r[1])) {                        	
                            	r[1] = Roman.normalizeRomanNumbersExceptTokens(r[1], exceptionTokens);
                            }                        
                            // lot
                            if (!StringUtils.isEmpty(r[3])) {
                            	r[3] = Roman.normalizeRomanNumbersExceptTokens(cleanLotTNRutherfordRO(r[3]), exceptionTokens);
                            }
                            /*
                            if (gtees.length > 1 && gtees[1].matches(".*\\b(?:SEC(?:TION)?|SC|PHASE|L(?:O?)T(?:S?)|UNITS?)\\b.*")){                        	
                            	String r2[] = SubdivparseRuthRO(gtees[1], searchId);
                            	if (StringUtils.isEmpty(r[1]) && !StringUtils.isEmpty(r2[1])){
                            		r[1] = Roman.normalizeRomanNumbersExceptTokens(r2[1], exceptionTokens);
                            	}
                            	if (StringUtils.isEmpty(r[3]) && !StringUtils.isEmpty(r2[3])){
                            		r[3] = cleanLotTNRutherfordRO(r2[3]);
                            	}
                            }
                            */
                            if (r[8] != null && !r[8].equals(s[8])) {
                            	bprefs = bprefs.append(" ").append(r[8]);
                            } 
                            
                            rows.add(r);
            			}
            		}
            		
            		if(!rows.isEmpty()) {
            			/*if (pis != null && pis.body.length != 0){
                        	pis.body[0][0] = r[0];
                        	pis.body[0][1] = r[1];
                        	pis.body[0][2] = r[2];
                        	pis.body[0][3] = r[3];
                        	pis.body[0][5] = r[5];
                        } else {
                        */
                    	//ResultTable pisNew = new ResultTable();
                    	String [] header = {"SubdivisionName", "SubdivisionSection", "SubdivisionPhase", "SubdivisionLotNumber", "SubdivisionUnit", "SubdivisionBldg", "Acres", "District"};                        	
                    	String [][] body = new String[rows.size()][8];
                    	for (int i = 0; i < rows.size(); i++) {
                    		String[] r = rows.get(i);
                    		body[i][0] = org.apache.commons.lang.StringUtils.defaultString(r[0]);
                        	body[i][1] = org.apache.commons.lang.StringUtils.defaultString(r[1]);
                        	body[i][2] = org.apache.commons.lang.StringUtils.defaultString(r[2]);
                        	body[i][3] = org.apache.commons.lang.StringUtils.defaultString(r[3]);
                        	body[i][4] = org.apache.commons.lang.StringUtils.defaultString(r[5]);
                        	body[i][5] = org.apache.commons.lang.StringUtils.defaultString(r[4]);
                        	body[i][6] = org.apache.commons.lang.StringUtils.defaultString(r[6]);
                        	body[i][7] = org.apache.commons.lang.StringUtils.defaultString(r[7]);
						}
                    	

                    	Map map = new HashMap();
                    	map.put("SubdivisionName", new String[]{"SubdivisionName", ""});
                    	map.put("SubdivisionPhase", new String[]{"SubdivisionPhase", ""});
                    	map.put("SubdivisionLotNumber", new String[]{"SubdivisionLotNumber", ""});
                    	map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
                    	map.put("SubdivisionUnit", new String[]{"SubdivisionUnit", ""});
                    	map.put("SubdivisionBldg", new String[]{"SubdivisionBldg", ""});
                    	map.put("Acres", new String[]{"Acres", ""});
                    	map.put("District", new String[]{"District", ""});
                    	pis.setReadWrite();
                    	pis.setHead(header);
                    	pis.setBody(body);
                    	pis.setMap(map);
                    	pis.setReadOnly();
                        //}
            		}
            		
            	}
            }
        }
        if (pis != null) {
        	m.put("PropertyIdentificationSet", pis);
        }
	    
	    // PropertyDescription is set with the first Subdivision/Property Address, or with Legal Description field, if Subdivision/Property Address is empty
	    if (propertyDescr != null && propertyDescr.length() > 0){
	    	m.put("PropertyIdentificationSet.PropertyDescription", propertyDescr);
	    }
        
        //////////////////////////////////////

        //Refs
        String bp = bprefs.toString().trim(); 
        if (bp.length() != 0) {
            m.put("PropertyIdentificationSet.BookPageRefs", bp);
            
	        // crossRefSet
	        ResultTable rt = (ResultTable) m.get("CrossRefSet");
	        List b2 = new ArrayList();
	        if (rt == null) {
	            
	            rt = new ResultTable();
	            
	            m.put("CrossRefSet", rt);
	            String[] h = new String[] { "Recording Class", "Book & Page/Filing #", "Description", 
	                    "Recording Date & Time", "Instrument Number", "Book", "Page" };
	            rt.setHead(h);
	            
	            HashMap hm = new HashMap();
	            hm.put("InstrumentNumber", new String[] {"Instrument Number", "iw"} );
	            hm.put("Book_Page", new String[] {"Book & Page/Filing #", "iw"} );
	            hm.put("Book", new String[] {"Book", "iw"} );
	            hm.put("Page", new String[] {"Page", "iw"} );
	            
	            rt.setMap(hm);
	        } else {
	            rt.setReadWrite();
	            String[][] bv = rt.getBody();
	            for (int i = 0; i < bv.length; i++)
	                b2.add(Arrays.asList(bv[i]));
	        }
	
	        StringTokenizer st = new StringTokenizer(bp, " ");
	        while (st.hasMoreTokens()) {
	        
	            String token = st.nextToken();
	            
	            String book = token.replaceAll("(.*)[ /-](.*)", "$1");
	            String page = token.replaceAll("(.*)[ /-](.*)", "$2");
	            
	            if (book.length() != 0 || page.length() != 0) {
	                String[] row = new String[] { "", book + "_" + page, "", "", "", book, page, "" };
	                b2.add(Arrays.asList(row));
	            }
	        }
	        try {
	        	rt.setBody(b2);
	        } catch (Exception e) {
				e.printStackTrace();
				EmailClient email = new EmailClient();
				email.addTo(MailConfig.getExceptionEmail());
				email.setSubject("Error ro.cst.tsearch.extractor.xml.GenericFunctions1.stdPisRutherfordRO(ResultMap, " + searchId + ") on " + URLMaping.INSTANCE_DIR);
				email.addContent("Error ro.cst.tsearch.extractor.xml.GenericFunctions1.stdPisRutherfordRO(ResultMap, " + searchId + ")\n\n Stack Trace: " + e.getMessage() + " \n\n " + 
						ServerResponseException.getExceptionStackTrace( e, "\n" ) + 
						b2.toString());
				email.sendAsynchronous();
			}
	        rt.setReadOnly();
	        
	        m.put("CrossRefSet", rt);
        }
        
        ResultTable newTable = new ResultTable();
        String[] header = {PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()};
        @SuppressWarnings("rawtypes")
		List<List> tablebody = new ArrayList<List>();
        List<String> list;
        if (!StringUtils.isEmpty(legalRef)) {
        	Matcher matcher = Pattern.compile(PLAT_BOOK_PAGE_PATTERN).matcher(legalRef);
            while (matcher.find()) {
            	list = new ArrayList<String>();
            	list.add(matcher.group(1));
				list.add(matcher.group(2));
				tablebody.add(list);
            }
        }
        String comments = (String)m.get("tmpComments");
        if (!StringUtils.isEmpty(comments)) {
        	Matcher matcher = Pattern.compile(PLAT_BOOK_PAGE_PATTERN).matcher(comments);
            while (matcher.find()) {
            	list = new ArrayList<String>();
            	list.add(matcher.group(1));
				list.add(matcher.group(2));
				tablebody.add(list);
            }
        }
        newTable = GenericFunctions2.createResultTable(tablebody, header);
        if (newTable!=null && newTable.getBody().length>0) {
        	pis = (ResultTable) m.get("PropertyIdentificationSet");
        	if (pis==null) {
        		pis = newTable;
        	} else {
        		if (pis.getBody().length==1 && tablebody.size()==1) {
        			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), tablebody.get(0).get(0));
        			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), tablebody.get(0).get(1));
        		} else if (pis.getBody().length>1 && tablebody.size()==1) {
        			String[] plat_book = new String[pis.getBody().length];
        			String[] plat_page = new String[pis.getBody().length];
        			for (int i=0;i<plat_book.length;i++) {
        				plat_book[i] = (String)tablebody.get(0).get(0);
        				plat_page[i] = (String)tablebody.get(0).get(1);
        			}
        			pis.setReadOnly(false);
        			pis.addColumn(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), plat_book);
        			pis.addColumn(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), plat_page);
        			pis.setMap(ResultTable.mergeMapsFromHead(pis.getHead(), pis.getMapRefference(), newTable.getMapRefference()));
        			pis.setReadOnly(true);
        			
        		} else {
        			pis = ResultTable.joinHorizontalWithMap(pis, newTable);
        		}
        	}
        	m.put("PropertyIdentificationSet", pis);
        }
        
    }
    
	@SuppressWarnings("unchecked")
	public static void changeSectionWithBlock(ResultMap m, long searchId) throws Exception {
		ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
		if (pis != null) {
			String[] head = pis.getHead();
			if (head != null) {
				for (int i = 0; i < head.length; i++) {
					if ("SubdivisionSection".equals(head[i]) || "Section".equals(head[i])) {
						head[i] = "SubdivisionBlock";
						
						pis.setReadWrite();
						pis.setHead(head);
						
						String[] blockSomething = {head[i],""};
						pis.getMapRefference().put(head[i], blockSomething);
						pis.getMapRefference().remove("SubdivisionSection");
						
						pis.setReadOnly();
						
						
						break;
					}
				}

			}
		}
	}

    public static void stdPisDesotoRO(ResultMap m,long searchId) throws Exception {
        String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
        String[] a = StringFormats.parseNameDesotoRO(s);
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
      
    }

    public static void taxComputingBaldwinTR(ResultMap m,long searchId) throws Exception {
        String datePaid = (String) m.get("tmpLastUpdate");
        String totalDue = (String) m.get("TaxHistorySet.TotalDue");
        Date lastUpdateDate = new SimpleDateFormat("dd/mm/yyyy")
                .parse(datePaid);
        Date dueDate = DBManager.getDueDateForCountyAndCommunity(
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity()
                        .getID().longValue(), InstanceManager.getManager()
                        .getCurrentInstance(searchId).getCurrentCounty().getCountyId()
                        .longValue());
        if (dueDate.after(lastUpdateDate)) {
            m.put("TaxHistorySet.TotalDue", totalDue);
            m.put("TaxHistorySet.DelinquentAmount", "0");
        } else {
            m.put("TaxHistorySet.TotalDue", "0");
            m.put("TaxHistorySet.DelinquentAmount", totalDue);
        }
    }

    /*
     * public static void stdPisNashvilleAO(ResultMap m) throws Exception {
     * String s=(String)m.get("PropertyIdentificationSet.OwnerLastName");
     * 
     * s=s.replaceAll("\\s+", " "); Matcher
     * ma=Pattern.compile("^\\w+,").matcher(s); if (!ma.find()) return;
     * s=s.replaceAll(" ET ?UXX?\\b", " &"); s=s.replaceAll(" ET ?AL?\\b", "
     * &"); s=s.replaceAll(" ET ?VIR\\b", " &"); s=s.replaceAll(" AND\\b", "
     * &"); s=s.replaceAll("&( &)+", "&"); s=s.replaceAll("& *$", "");
     * ma=Pattern.compile("& ?").matcher(s); String husband, wife=""; if
     * (ma.find()) { husband=s.substring(0, ma.start()).trim();
     * wife=s.substring(ma.end()).trim(); } else { husband=s.trim(); }
     * 
     * int i=husband.indexOf(",");
     * m.put("PropertyIdentificationSet.OwnerLastName", husband.substring(0,
     * i).trim()); husband=husband.substring(i+1).replaceAll("\\.", "
     * ").replaceAll(",", " ").trim(); int j=husband.indexOf(" "); if (j!=-1) {
     * m.put("PropertyIdentificationSet.OwnerFirstName", husband.substring(0,
     * j).trim()); m.put("PropertyIdentificationSet.OwnerMiddleName",
     * husband.substring(j+1).trim()); } else {
     * m.put("PropertyIdentificationSet.OwnerFirstName", husband); }
     * 
     * if (wife.length()>0)
     * wife=wife.replaceAll("\\b"+m.get("PropertyIdentificationSet.OwnerLastName")+",?",
     * "").trim(); ma=Pattern.compile("^\\w+,").matcher(wife); if (ma.find()) { //
     * another last name i=wife.indexOf(",");
     * m.put("PropertyIdentificationSet.SpouseLastName", wife.substring(0,
     * i).trim()); wife=wife.substring(i+1).trim(); } else {
     * m.put("PropertyIdentificationSet.SpouseLastName",
     * m.get("PropertyIdentificationSet.OwnerLastName")); }
     * wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").trim();
     * 
     * j=wife.indexOf(" "); if (j!=-1) {
     * m.put("PropertyIdentificationSet.SpouseFirstName", wife.substring(0,
     * j).trim()); m.put("PropertyIdentificationSet.SpouseMiddleName",
     * wife.substring(j+1).trim()); } else {
     * m.put("PropertyIdentificationSet.SpouseFirstName", wife); } }
     * 
     * public static void stdPisWilliamsonAO(ResultMap m) throws Exception {
     * String s=(String)m.get("PropertyIdentificationSet.OwnerLastName");
     * 
     * s=s.replaceAll("\\s+", " "); s=s.replaceAll(" ET ?UXX?\\b", " &");
     * s=s.replaceAll(" ET ?AL?\\b", " &"); s=s.replaceAll(" ET ?VIR\\b", " &");
     * s=s.replaceAll(" AND\\b", " &"); s=s.replaceAll("%", "&");
     * s=s.replaceAll("&( &)+", "&"); s=s.replaceAll("& *$", ""); Matcher
     * ma=Pattern.compile("& ?").matcher(s); String husband, wife=""; if
     * (ma.find()) { husband=s.substring(0, ma.start()).trim();
     * wife=s.substring(ma.end()).trim(); } else { husband=s.trim(); }
     * 
     * int i=husband.indexOf(" "), j; if (i==-1) {
     * m.put("PropertyIdentificationSet.OwnerLastName", husband); } else {
     * m.put("PropertyIdentificationSet.OwnerLastName", husband.substring(0,
     * i).trim()); husband=husband.substring(i+1).replaceAll("\\.", "
     * ").replaceAll(",", " ").trim(); j=husband.indexOf(" "); if (j!=-1) {
     * m.put("PropertyIdentificationSet.OwnerFirstName", husband.substring(0,
     * j).trim()); m.put("PropertyIdentificationSet.OwnerMiddleName",
     * husband.substring(j+1).trim()); } else {
     * m.put("PropertyIdentificationSet.OwnerFirstName", husband); } }
     * 
     * wife=wife.replaceAll("\\b"+m.get("PropertyIdentificationSet.OwnerLastName")+",?",
     * "").trim(); if (wife.length()>0)
     * m.put("PropertyIdentificationSet.SpouseLastName",
     * m.get("PropertyIdentificationSet.OwnerLastName"));
     * 
     * wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").replaceAll(" &",
     * "").trim();
     * 
     * j=wife.indexOf(" "); if (j!=-1) {
     * m.put("PropertyIdentificationSet.SpouseFirstName", wife.substring(0,
     * j).trim()); m.put("PropertyIdentificationSet.SpouseMiddleName",
     * wife.substring(j+1).trim()); } else {
     * m.put("PropertyIdentificationSet.SpouseFirstName", wife); } }
     */
    protected static String extractField(String s, String head,long searchId) {
        Pattern p = Pattern.compile(" [A-Z][a-z]\\w*:");
        Pattern pa = Pattern.compile("(.+)\\s*(/\\s*Dist[:\\s0-9]+$)"); //B2941
        int i = s.indexOf(head + ":");
        if (i == -1)
            return "";
        int b = i + head.length() + 1;
        Matcher m = p.matcher(s);
        Matcher ma = pa.matcher(s);
        int e = s.length();
        if (!ma.find()) {
        	if (m.find(b))
        		e = m.start();
        }
        s = s.substring(b, e).trim();
        if (s.endsWith(","))
            s = s.substring(0, s.length() - 1).trim();
        return s;
    }

    protected static Pattern bp = Pattern.compile("(\\d\\d\\d+) (\\d+)");

    protected static Pattern lt = Pattern
            .compile("(?i)\\b((LO?TS?)|(PH(ASE)?)|(SEC(TION)?)|(SUBD?)|NO)\\b");

    protected static Pattern pmargparcid = Pattern.compile("(?i).*pcl (.+)");

    public static void legalDescriptionNashvilleRO(ResultMap m,long searchId)
            throws Exception {
    		String tmpParcel = (String) m.get("PropertyIdentificationSet.PropertyDescription");
    	    	if(tmpParcel!=null){
    	    		if(!tmpParcel.equals(tmpParcel.replaceAll("(?is).*?PrpId:\\s*([0-9]{11})[^0-9]+.*", "$1"))){
    	    			tmpParcel=tmpParcel.replaceAll("(?is).*?PrpId:\\s*([0-9]{11})[^0-9]+.*", "$1");
    	    		}
    	    		else{
    	    			tmpParcel="";
    	    		}
    	    		m.put("PropertyIdentificationSet.ParcelID", tmpParcel);
    	       	}

    	CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
    	Search search = currentInstance.getCrtSearchContext();
    	DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, search.getSearchType() == Search.PARENT_SITE_SEARCH);
    	String stateCountySiteType = search.getSa().getStateCounty() + dataSite.getSiteTypeAbrev();
    	String legDesc = (String) m.get("PropertyIdentificationSet.PropertyDescription");
        if (legDesc==null) {
        	legDesc = "";
        } else {
        	legDesc = legDesc.replaceAll("\\bAD\\b", "");
        }
        //        if (legDesc.length()==0 && grantor!=null &&
        // StringFormats.isSubdivision(grantor))
        //            return;
        ResultTable rt = new ResultTable();
        m.put("PropertyIdentificationSet", rt);
        String[] h = { "SubdivisionName", "SubdivisionLotNumber", "PlatBook",
                "PlatNo", "Subdivision", "StreetNo", "StreetName", "City",
                "State", "Zip", "SubdivisionSection", "SubdivisionPhase",
                "SubdivisionUnit", "ParcelID", "SubdivisionBlock", "SubdivisionCond", "SubdivisionBldg", "SubdivisionTract"};
        rt.setHead(h);
        Map hm = new HashMap();
        for (int i = 0; i < h.length; i++)
            hm.put(h[i], new String[] { h[i], "" });
        rt.setMap(hm);
        List b = new ArrayList();

        if (legDesc.length() != 0) {
            String[] st = legDesc.split("\\s*/\\s*(?=(Sub|Lt|Un|PrpId|St|Addr|ExtDesc|Bl))");
            for (int i=0; i<st.length; i++) {
                String s = st[i];
                if (!"KSJohnsonRO".equals(stateCountySiteType)) {
                	s = replaceNumbers(s); 
                }
                s = s.replaceAll("\\b(\\d+(ST|ND|RD|TH)? )?REV(ISED|ISION)?\\b", "");
                s = s.replaceAll("\\bRESERVED?\\b", "");
                s = s.replaceAll("\\bPARCEL (\\d+|[A-Z])\\b", "");
                s = s.replaceAll("\\b(\\d+(ST|ND|RD|TH)? )?ADD(ITION)?\\b", "");
                s = s.replaceAll("\\bZONE\\b", "");
                s= s.replaceAll("\\bDIVISION\\b", "");                
                s = s.replaceAll("\\s*/\\s*$", "");
                s = s.replaceAll("\\s{2,}", " ").trim();
                String[] row = new String[h.length];
                Arrays.fill(row, "");
                String sub = extractField(s, "Sub",searchId);
                String unitLabel = "Un";
                if ("KSJohnsonRO".equals(stateCountySiteType)) {
                	Matcher ma1 = Pattern.compile("\\b(\\d+)\\s+(\\d+)\\s*$").matcher(sub);
                		if (ma1.find()) {
                			row[2] = ma1.group(1);		//plat book
                			row[3] = ma1.group(2);		//plat page
                			sub = sub.replaceFirst(ma1.group(0), "");
                		}
            		unitLabel = "Unit";
                }
                row[0] = StringFormats.SubdivisionNashvilleAO(sub);                
                if (sub.contains(" COND"))
      			   row[15] = row[0];
                row[1] = extractField(s, "Lt",searchId);
                row[10] = StringFormats.SectionNashvilleAO(sub);
                row[11] = StringFormats.PhaseNashvilleAO(sub);
                row[12] = (StringFormats.UnitNashvilleAO(sub) + " " + extractField(
                        s, unitLabel,searchId)).trim();
                row[4] = composeSubdivision2(sub, row[1]);
                row[13] = extractField(s, "PrpId",searchId);
                row[14] = extractField(s, "Bl", searchId);
                if ("KSJohnsonRO".equals(stateCountySiteType)) {
                	row[14] += extractField(s, "Blk", searchId);
                	row[14] = LegalDescription.cleanValues(row[14], false, true);
                	row[16] = extractField(s, "Bldg", searchId);
                } else {
                	row[16] = StringFormats.BuildingNashvilleAO(sub);
                }
                row[17] = StringFormats.TractNashvilleAO(sub);
                String stField = extractField(s, "St",searchId);
                if (stField.length() > 0) {
                    row[5] = StringFormats.StreetNo(stField);
                    row[6] = StringFormats.StreetName(stField);
                } else {
                    stField = extractField(s, "Addr",searchId);
                    if (stField.length() > 0) {
                        // nr addr city, state zip
                        row[5] = stField.replaceFirst("(\\d+).*", "$1");
                        row[6] = stField
                                .replaceFirst("\\d+ (.+) \\w+,.*", "$1");
                        row[7] = stField.replaceFirst(".+ (\\w+),.*", "$1");
                        row[8] = stField.replaceFirst(".+, (\\w\\w).*", "$1");
                        row[9] = stField.replaceFirst(".+ (\\d+)", "$1");
                    }
                }
                if ("KSJohnsonRO".equals(stateCountySiteType)) {
                	correctParsingKSJohnsonRO(sub, row);
                }
                if (row[0].length() != 0 || row[6].length() != 0 || row[13].length() != 0) {
                    if (!isAlreadyAdded(b, row)) {
                    	b.add(Arrays.asList(row));
                        row = new String[h.length];
                        Arrays.fill(row, "");
                    }
                }
                String extDesc = extractField(s, "ExtDesc",searchId);
                
                if(StringUtils.isNotEmpty(extDesc)) {
	                extDesc = " " + extDesc + " ";                
	                extDesc = ro.cst.tsearch.titledocument.abstracts.DocumentTypes
	                        .removeAllDocTypes(extDesc,searchId);
	            	extDesc = extDesc.trim().replaceAll("\\b\\d+(?:ST|ND|RD|TH)? DIST\\b", "");
	            	extDesc = extDesc.replaceAll("\\b\\d+ TRACTS?\\b", "");
	            	extDesc = extDesc.replaceAll("\\b\\d+ PARCEL\\b", "");
	            	extDesc = extDesc.replaceAll("\\s*\\bCORRECT(ION)?\\b\\s*", " ");
	            	extDesc = extDesc.replaceAll("\\s*\\bLAND OTHER COUNTY\\b\\s*", " ");            	
	            	extDesc = extDesc.replaceAll("\\s{2,}", " ").trim();
	                if (extDesc.trim().length() > 0) {
	                    Matcher ma = bp.matcher(extDesc);
	                    if (ma.find()) {
	                        row[2] = ma.group(1);
	                        row[3] = ma.group(2);
	                        String s1 = extDesc;
	                        do {
	                            s1 = s1.substring(ma.end()).trim();
	                            if (s1.startsWith(","))
	                                s1 = s1.substring(1);
	                            ma = bp.matcher(s1);
	                            if (!ma.find())
	                                break;
	                            String[] row1 = new String[h.length];
	                            Arrays.fill(row1, "");
	                            row1[2] = ma.group(1);
	                            row1[3] = ma.group(2);
	                            b.add(Arrays.asList(row1));
	                        } while (true);
	                    } else if (lt.matcher(extDesc).find()) {
	                        row[4] = extDesc.trim();
	                        row[1] = StringFormats.LotNashvilleAO(extDesc);                        
	                        if (!ro.cst.tsearch.search.token.AddressAbrev.containsAbbreviation(extDesc) 
	                        		|| extDesc.matches(lt.pattern()+" "+row[1]+".+")){ // fix for bug #3219
	                        	row[0] = StringFormats.SubdivisionNashvilleAO(extDesc);	// see bug #2103
	                        } else { // fix for bug #2177
	                        	String addr = StringFormats.SubdivisionNashvilleAO(extDesc);
	                        	row[5] = StringFormats.StreetNo(addr);
	                            row[6] = StringFormats.StreetName(addr);
	                        }                        
	                    } else if (ro.cst.tsearch.search.token.AddressAbrev
	                            .containsAbbreviation(extDesc)) {
	                        row[5] = StringFormats.StreetNo(extDesc);
	                        row[6] = StringFormats.StreetName(extDesc);
	                    } else if(extDesc.matches(".+\\bCTY\\b.*")) {
	                    	//row[7] = extDesc.replaceFirst("(.+)\\sCI?TY\\b.*", "$1"); //don't need to extract the county
	                    } else {
	                    	String subdiv = StringFormats.SubdivisionNashvilleAO(extDesc);
	                    	subdiv = subdiv.replaceFirst("^[\\W\\d]+$", "");
	                        row[0] = row[4] = subdiv;
	                    }
	                    if (extDesc.contains(" COND"))
	           			   row[15] = row[0];                    
	                    row[10] = StringFormats.SectionNashvilleAO(extDesc);
	                    row[11] = StringFormats.PhaseNashvilleAO(extDesc);
	                    row[12] = StringFormats.UnitNashvilleAO(extDesc);
	                    row[14] = StringFormats.BlockNashvilleAO(extDesc);
	                    row[16] = StringFormats.BuildingNashvilleAO(extDesc);
	                    row[17] = StringFormats.TractNashvilleAO(extDesc);
	                    b.add(Arrays.asList(row));
	                }
                
                }
            }
        }
        // parse grantor as subdivision
        String grantor = (String) m.get("SaleDataSet.Grantor");
        String grantee = (String) m.get("SaleDataSet.Grantee");
        String[] row = null;
        Boolean isTNKnox = false;
        if (dataSite.getCountyId() == CountyConstants.TN_Knox) {
        	isTNKnox = true;
		}
		if (!StringUtils.isEmpty(grantor)) {
			
			row = parsePISFromString(m, h.length, grantor, isTNKnox ? "" : "*grantor*");
			// add this info only if it was parsed at least one of the following: lot, section, phase, unit , tract
			if (!(row[1].equals("") && row[10].equals("") && row[11].equals("") && row[12].equals("") && row[17].equals("")))
				b.add(Arrays.asList(row));
			else {
				// try to parse the grantee
				if (!StringUtils.isEmpty(grantee)) {
					row = parsePISFromString(m, h.length, grantee, isTNKnox ? "" : "*grantee*");
					if (!(row[1].equals("") && row[10].equals("") && row[11].equals("") && row[12].equals("") && row[17].equals("")))
						b.add(Arrays.asList(row));
				}
			}
		} else if(!StringUtils.isEmpty(grantee)) {
    		row = parsePISFromString(m, h.length, grantee, isTNKnox ? "" : "*grantee*");
    		if(!(row[1].equals("") && row[10].equals("") && row[11].equals("") && row[12].equals("") && row[17].equals("")))
            	b.add(Arrays.asList(row));
    	}
        // parcel ID
        String marg = (String) m.get("tmpMarginal");
        if (marg != null) {
            String[] a = marg.split(",");
            List ret = new ArrayList();
            for (int i = 0; i < a.length; i++) {
                Matcher ma = pmargparcid.matcher(a[i]);
                if (ma.find())
                    ret.add(ma.group(1).replaceAll(" |/", ""));
            }
            Iterator it = ret.iterator();
            while (it.hasNext()) {
                String parcelID = (String) it.next();
                row = new String[h.length];
                Arrays.fill(row, "");
                row[13] = parcelID;
                b.add(Arrays.asList(row));
            }
        }
        rt.setBody(b);
        rt.setReadOnly();

        // cross Reference
        Iterator it = b.iterator();
        while (it.hasNext()) {
            List l = (List) it.next();
            String book = (String) l.get(2);
            String page = (String) l.get(3);
            if (book.length() != 0 || page.length() != 0) {
                m.put("SaleDataSet.CrossRefInstrument", book + " " + page);
                break;
            }
        }

        // crossRefSet
        rt = (ResultTable) m.get("CrossRefSet");
        List b2 = new ArrayList();
        if (rt == null) {
            rt = new ResultTable();
            m.put("CrossRefSet", rt);
            h = new String[] { "all", "InstrumentNumber", "Book", "Page" };
            rt.setHead(h);
            hm = new HashMap();
            for (int i = 1; i < h.length; i++)
                hm.put(h[i], new String[] { h[i], "" });
            rt.setMap(hm);
        } else {
            rt.setReadWrite();
            String[][] bv = rt.getBody();
            for (int i = 0; i < bv.length; i++)
                b2.add(Arrays.asList(bv[i]));
        }

        it = b.iterator();

        while (it.hasNext()) {
            List l = (List) it.next();
            String book = (String) l.get(2);
            String page = (String) l.get(3);
            if (book.length() != 0 || page.length() != 0) {
                if (rt.getHead().length==3) {
                	row = new String[] { "", book, page };
                    b2.add(Arrays.asList(row));
                } else if (rt.getHead().length==4) {
                	row = new String[] { "", "", book, page };
                    b2.add(Arrays.asList(row));
                }
            }
        }
        rt.setBody(b2);
        rt.setReadOnly();
        //System.out.println("Subdivizia   :"+(String) m.get("PropertyIdentificationSet.SubdivisionName"));  
        
        

    }
    
    public static void correctParsingKSJohnsonRO(String subdName, String[] row) {
    	if (row.length==18) {
    		Matcher ma1 = Pattern.compile("(?is)\\bLO?TS?\\s*([A-Z0-9-]+)\\b").matcher(subdName);
    		while (ma1.find()) {
    			row[1] += " " + ma1.group(1);		//lot
    			subdName = subdName.replaceFirst(ma1.group(0), "");
    		}
    		Matcher ma2 = Pattern.compile("(?is)\\bBL(?:OC)?KS?\\s+(\\d+(?:\\s+TO\\s+\\d+(?:\\s+INCLUSIVE)?)?)\\b").matcher(subdName);
    		while (ma2.find()) {
    			String blk = ma2.group(1);
    			blk = blk.replaceAll("(?is)(\\d+)\\s+TO\\s+(\\d+)", "$1-$2");
    			blk = blk.replaceAll("(?is)\\bINCLUSIVE\\b", "");
    			row[14] += " " + blk.trim();		//block
    			subdName = subdName.replaceFirst(ma2.group(0), "");
    		}
    		Matcher ma3 = Pattern.compile("(?is)\\bPH(?:ASE)?\\s+(\\d+)\\b").matcher(subdName);
    		while (ma3.find()) {
    			row[11] += " " + ma3.group(1).replaceAll("&", " ");	//phase
    			subdName = subdName.replaceFirst(ma3.group(0), "");
    		}
    		Matcher ma4 = Pattern.compile("\\bTR\\s+(\\w+(?:&\\w+)*)\\b").matcher(subdName);
    		while (ma4.find()) {
    			row[17] += " " + ma4.group(1).replaceAll("&", " ");	//tract
    			subdName = subdName.replaceFirst(ma4.group(0), "");
    		}
    		row[1] = LegalDescription.cleanValues(row[1], false, true);
    		row[14] = LegalDescription.cleanValues(row[14], false, true);
    		row[11] = LegalDescription.cleanValues(row[11], false, true);
    		row[17] = LegalDescription.cleanValues(row[17], false, true);
    		row[0] = subdName.trim();
    	}
    }
    
    public static boolean isAlreadyAdded(List b, String[] row) {
    	for (int i=0;i<b.size();i++) {
    		List<String> r = (List<String>)b.get(i);
    		if (r.size()==row.length) {
    			boolean equal = true;
    			int len = r.size();
    			for (int j=0;j<len;j++) {
    				if (!r.get(j).equals(row[j])) {
    					equal = false;
    					break;
    				}
    			}
    			if (equal) {
        			return true;
        		}
    		}
    	}
    	return false;
    }

    /**
     * Fills a PIS like structure from a string that will be parsed
     * (example: source can be grantor or grantee)
     * @param m
     * @param size
     * @param toBeParsed
     * @param source
     * @return
     */
    private static String[] parsePISFromString(ResultMap m, int size, String toBeParsed, String source) {
    	String[] row = new String[size];
        int slashIndex;
        Arrays.fill(row, "");
        row[4] = toBeParsed;
        //toBeParsed = toBeParsed.toUpperCase(); 200104230039877  200212110152723 
        slashIndex = toBeParsed.lastIndexOf("/");
        toBeParsed = toBeParsed.replaceAll("\\bZONE\\b", "");		// Instr# 198308155800254 on Davidson RO
        toBeParsed = toBeParsed.replaceAll("\\bDIVISION\\b", "");
        toBeParsed = toBeParsed.replaceFirst("\\s+LOT\\s*$", "");
        if (slashIndex!=-1){
        	String[] candidates = toBeParsed.split("[ ]*/[ ]*");
        	for (int i = 0; i < candidates.length; i++) {
				if(!candidates[i].isEmpty()){
					row[0] = StringFormats.SubdivisionNashvilleAO(candidates[i]);
		            row[1] = StringFormats.LotNashvilleAO(candidates[i]);
		            row[10] = StringFormats.SectionNashvilleAO(candidates[i]);
		            row[11] = StringFormats.PhaseNashvilleAO(candidates[i]);
		            row[12] = StringFormats.UnitNashvilleAO(candidates[i]);
		            row[7] = source;
		            row[17] = StringFormats.TractNashvilleAO(candidates[i]);
		            if ((candidates[i].indexOf("PHASE")!=-1)||(candidates[i].indexOf("LOT")!=-1)) {
		            	try {
		            		m.put("PropertyIdentificationSet.SubdivisionName",row[0]);
		            	} catch (Exception e) {}
		            }
		            if(!(row[1].equals("") && row[10].equals("") && row[11].equals("") && row[12].equals("") && row[17].equals("")))
		            	break;
				}
			}
            
        }else {            	
            row[0] = StringFormats.SubdivisionNashvilleAO(toBeParsed);
            row[1] = StringFormats.LotNashvilleAO(toBeParsed);
            row[10] = StringFormats.SectionNashvilleAO(toBeParsed);
            row[11] = StringFormats.PhaseNashvilleAO(toBeParsed);
            row[12] = StringFormats.UnitNashvilleAO(toBeParsed);
            row[7] = source;
            row[17] = StringFormats.TractNashvilleAO(toBeParsed);
        }
		return row;
	}

	public static void lotClayRO(ResultMap m,long searchId) throws Exception {
    	/*
    	 * parse lot / lots numbers from full subdivision string
    	 */
    	String subdivFullText = (String) m.get( "PropertyIdentificationSet.SubdivisionLotNumber" );
    	if( subdivFullText == null ){
    		subdivFullText = "";
    	}
    	
    	subdivFullText = subdivFullText.replaceAll("(?i)thru", "-");
    	
    	Matcher lotNumberMatcher = clayROLotPattern.matcher( subdivFullText );
    	String commaSepparatedLotNumbers = "";
    	while( lotNumberMatcher.find() ){
    		String possibleLotNumberString = lotNumberMatcher.group( 1 );
    		
    		possibleLotNumberString = possibleLotNumberString.replaceAll( "&" , "," );
    		
    		String[] lotNumbers = possibleLotNumberString.split(",");
    		for( int i = 0 ; i < lotNumbers.length ; i++ ){
    			String lotNumber = lotNumbers[i].trim();
    			if( lotNumber.contains("-") ){
    				//group
    				//must determine the two boundaries and generate all possible variants
    				String[] range = lotNumber.split("-");
    				int rangeBegin = -1;
    				int rangeEnd = -1;
    				try{
    					rangeBegin = Integer.parseInt( range[0].trim() );
    					rangeEnd = Integer.parseInt( range[1].trim() );
    				}catch( Exception e ){
    					
    				}
    				
    				if( rangeBegin != -1 && rangeEnd != -1 ){
    					for( ; rangeBegin <= rangeEnd ; rangeBegin ++ ){
    						commaSepparatedLotNumbers += rangeBegin + ",";
    					}
    				}
    			}
    			else {
    				commaSepparatedLotNumbers += lotNumber + ",";
    			}
    		}
    	}
    	
    	Matcher blockNumberMatcher = clayROBlockPattern.matcher( subdivFullText );
    	String commaSepparatedBlockNumbers = "";
    	while( blockNumberMatcher.find() ){
    		String possibleBlockNumberString = blockNumberMatcher.group( 1 );
    		
    		possibleBlockNumberString = possibleBlockNumberString.replaceAll( "&" , "," );
    		
    		String[] blockNumbers = possibleBlockNumberString.split(",");
    		for( int i = 0 ; i < blockNumbers.length ; i++ ){
    			String blockNumber = blockNumbers[i].trim();
    			if( blockNumber.contains("-") ){
    				//group
    				//must determine the two boundaries and generate all possible variants
    				String[] range = blockNumber.split("-");
    				int rangeBegin = -1;
    				int rangeEnd = -1;
    				try{
    					rangeBegin = Integer.parseInt( range[0].trim() );
    					rangeEnd = Integer.parseInt( range[1].trim() );
    				}catch( Exception e ){
    					
    				}
    				
    				if( rangeBegin != -1 && rangeEnd != -1 ){
    					for( ; rangeBegin <= rangeEnd ; rangeBegin ++ ){
    						commaSepparatedBlockNumbers += rangeBegin + ",";
    					}
    				}
    			}
    			else {
    				commaSepparatedBlockNumbers += blockNumber + ",";
    			}
    		}
    	}    	
    	
    	m.put("PropertyIdentificationSet.SubdivisionLotNumber", commaSepparatedLotNumbers);
    	m.put("PropertyIdentificationSet.SubdivisionBlock", commaSepparatedBlockNumbers);
    }
    
    public static void lotNumberTNShelby(ResultMap m,long searchId) throws Exception {
    	TNShelbyAO.lotNumberTNShelby(m, searchId);
    }
    
    public static void parseAddress(ResultMap m, long searchId) throws Exception {
    	//we must parse here the address because we don't have the city all the time 
    	//0 HOLMES, TN ;	12700 ROLLING LAKE, Arlington TN;  	4880 TUGGLE RD, TN
    	String tmpLocation = (String) m.get("tmpPropLoc");
    	String tempLocation = tmpLocation;
    	String stat = tmpLocation.replaceFirst("(.+) (TN)", "$2");
    	m.put("PropertyIdentificationSet.State", stat);
    	tmpLocation = tempLocation;
    	tmpLocation = tmpLocation.replaceFirst("(.+) TN", "$1");
    	tempLocation = tmpLocation;
    	String address = tmpLocation.replaceAll("(.*),.*", "$1");
    	if (address.length() != 0){
			   m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
			   m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
		}
    	tmpLocation = tempLocation;
    	String city = tmpLocation.replaceAll("(.*),(.*)", "$2").trim();
    	if (city.length() != 0 && !city.equals("Shelby")) {
    		m.put("PropertyIdentificationSet.City", city);
    	}
    }
    
    public static void cleanLotShelby(ResultMap m,long searchId) throws Exception{
    	TNShelbyAO.cleanLotShelby(m, searchId);
    }
    public static void assignBookAndPageToInstrNoShelbyRO(ResultMap m,long searchId) throws Exception{
        String instr = (String) m.get("SaleDataSet.InstrumentNumber");
        /*if (instr==null) {
            String book = (String) m.get("SaleDataSet.Book");
            String page = (String) m.get("SaleDataSet.Page");
            if (book!=null && page!=null) {
                m.put("SaleDataSet.InstrumentNumber",book+"_"+page);
            }
        }*/
        if (instr!=null && instr.indexOf("Instrument\\s?#") != -1) {
            instr=instr.replaceAll("Instrument #","");
            instr=instr.replaceAll("\\s?[*?.]","");
            instr=instr.replaceAll("Pages.*$","");
            instr=instr.replaceAll("In Year (N/A|\\d+)","");
            instr=instr.replaceAll("(?i)(View|NO) Image","");
            instr=instr.trim();
            m.put("SaleDataSet.InstrumentNumber",instr);
        }else {
            instr = (String) m.get("tmpInstrument");
            if (instr!=null) {
                instr=instr.replaceAll("Instrument\\s?#","");
                instr=instr.replaceAll("\\s?[*?.]","");
                instr=instr.replaceAll("In Year (N/A|\\d+)","");
                instr=instr.replaceAll("(?i)(View|NO) Image","");
                instr=instr.trim();
                m.put("SaleDataSet.InstrumentNumber",instr);
            }
            else {
                instr = (String) m.get("tmpInstrument2");
                if (instr!=null) {
                    m.put("SaleDataSet.InstrumentNumber",instr);
                }
                else {
                    m.put("SaleDataSet.InstrumentNumber","");
                }
            }
        }
    }
    
    public static void setBookAndPageTNShelbyRO(ResultMap m,long searchId) throws Exception{
        String book = (String) m.get("tmpBook");
        String page = (String) m.get("tmpPage");
        if (book.equals("0")) {
        	book = "";
        }
        if (page.equals("0")) {
        	page = "";
        }
        if (StringUtils.isEmpty(book))
        	return;
        if (StringUtils.isEmpty(page))
        	return;
        m.put("SaleDataSet.Book", book);
        m.put("SaleDataSet.Page", page);
    }
    
    public static void setAddressDNShelby(ResultMap m,long searchId) throws Exception{
        String tmpAddress = (String) m.get("tmpAddress");
         if ((tmpAddress==null) || (tmpAddress.equals("N/A"))){
             String tmpAddress2 = (String) m.get("tmpAddress2");
             if (tmpAddress2!=null) {
                 String stName = StringFormats.StreetName(tmpAddress2);
                 String stNo = StringFormats.StreetNo(tmpAddress2);
                 m.put("PropertyIdentificationSet.StreetName",stName);
                 m.put("PropertyIdentificationSet.StreetNo",stNo);
             }
         }
         System.out.println("Grantor:  " +(String) m.get("SaleDataSet.Grantor"));
         System.out.println("Grantee:  " +(String) m.get("SaleDataSet.Grantee"));
    }
    
    public static void concatInitialShelbyDN(ResultMap m,long searchId) throws Exception{
        String type = (String) m.get("SaleDataSet.DocumentType");
        String instr = (String) m.get("SaleDataSet.InstrumentNumber");
        	if ((type.equals("Court Filings General Sessions"))||(type.equals("Court Filings"))){
        	    instr = "F"+instr;
        	    m.put("SaleDataSet.InstrumentNumber",instr);
        	}
        	if (type.equals("General Sessions -- Judgments")) {
        	    instr = "J"+instr;
        	    m.put("SaleDataSet.InstrumentNumber",instr);
        	}
    }
    
    public static void bookAndPageShelbyAO(ResultMap m,long searchId) throws Exception{
    	TNShelbyAO.bookAndPageShelbyAO(m, searchId);
    }
    public static void liensDocTypeChangeNashvilleRO(ResultMap m,long searchId)
            throws Exception {
        String debtor = (String) m.get("tmpDebtor");
        String doctype = (String) m.get("SaleDataSet.DocumentType");
        if (debtor != null && debtor.startsWith("Debtor")
                && doctype.length() == 0) {
            m.put("SaleDataSet.DocumentType", "LIEN");
        }
    }

    protected static String composeSubdivision5(String name, String lot,
            String sec, String phase, String unit,long searchId) throws Exception {
    	if (name == null || name.length() == 0)
    		name = "";
        if (sec != null && sec.length() > 0)
            name += " SECTION " + sec;
        if (phase != null && phase.length() > 0)
            name += " PHASE " + phase;
        if (unit != null && unit.length() > 0)
            name += " UNIT " + unit;
        return composeSubdivision2(name, lot);
    }

    protected static String composeSubdivision2(String name, String lot)
            throws Exception {
        String ret;
        if (name == null || name.length() == 0)
            ret = "";
        else if (lot == null || lot.length() == 0)
            ret = name;
        else
            ret = "LOT " + lot + " " + name;
        return ret;
    }

    public static void composeSubdivision(ResultMap m,long searchId) throws Exception {
        String name = (String) m
                .get("PropertyIdentificationSet.SubdivisionName");
        if (name!=null) {
        	name = name.replaceFirst("(?is)([^/]+)/\\s*$","$1").trim();
            m.put("PropertyIdentificationSet.SubdivisionName", name);
        }
        String lot = (String) m
                .get("PropertyIdentificationSet.SubdivisionLotNumber");
        String section = (String) m
                .get("PropertyIdentificationSet.SubdivisionSection");
        String phase = (String) m
                .get("PropertyIdentificationSet.SubdivisionPhase");
        String unit = (String) m
                .get("PropertyIdentificationSet.SubdivisionUnit");
        String ret = composeSubdivision5(name, lot, section, phase, unit,searchId);
        m.put("PropertyIdentificationSet.Subdivision", ret);
        
    }

    public static void composeSubdivisionTmpAndLot(ResultMap m,long searchId)
            throws Exception {
        String name = (String) m.get("tmpSubdivision");
        String lot = (String) m
                .get("PropertyIdentificationSet.SubdivisionLotNumber");
        String ret = composeSubdivision2(name, lot);
        m.put("PropertyIdentificationSet.Subdivision", ret);
    }
    
    public static void setCondoName(ResultMap m,long searchId) throws Exception {
		String name = (String) m.get("PropertyIdentificationSet.SubdivisionName");
		String descr = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (name == null || name.length() == 0 || descr == null || descr.length() == 0)
			return;
		
		if (descr.contains(" CONDO")){
			m.put("PropertyIdentificationSet.SubdivisionCond", name.replaceFirst("(.+?)\\sCONDO.*", "$1"));	
		}				
    }
    
    public static void appendUnitToSection(ResultMap m, long searchId) throws Exception {
       /* B2170 
        * ( ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
        if (pis != null) {
	        for (int i = 0; i < pis.body.length; i++)
	            pis.body[i][10] = (pis.body[i][10] + " " + pis.body[i][12]).trim();
        }
        */
    }

    public static void parcelIDJacksonAO(ResultMap m,long searchId)throws Exception{
    	MOJacksonEP.parcelIDJacksonAO(m, searchId);
    }
    
    public static void legalMOJacksonAO (ResultMap m,long searchId)throws Exception{
    	MOJacksonEP.legalMOJacksonAO(m, searchId);
    }
    
	public static void partyNamesMOClayEP(ResultMap m, long searchId) throws Exception {
		String name = (String) m.get("tmpOwnerFullName");
		if (name == null || name.length() == 0)
			return;
		String addr = (String) m.get("tmpOwnerAddress");
		if (!StringUtils.isEmpty(addr)){
			addr = addr.replaceAll("\\s*\\n[\\s\\n]*", "@@");
			name = name + "@@" + addr;
		}
		partyNamesTokenizerMOClayEP(m, name);
	}

	public static void partyNamesTokenizerMOClayEP(ResultMap m, String s) throws Exception {

		String[] lines = s.split("\\s?@@\\s?");
		for (int i = 0; i < lines.length - 2; i++) {
			String owner = lines[i];
			owner = owner.replaceFirst("\\s*-\\s*$", "");
			if (owner.equals("TRUST") && i > 0) {
				lines[i - 1] = lines[i - 1] + " " + owner;
				owner = "";
			} else {
				owner = owner.replaceFirst("\\bETAL\\b", "");
				owner = owner.replaceFirst("\\bTR(USTEE)?S?\\b", "");				
				owner = owner.replaceAll("\\s{2,}", " ").trim();
			}
			lines[i] = owner;
		}
		
		List<List> body = new ArrayList<List>();
		
		// parse each entity as L F M
		String[] a = new String[6];
		String[] b = new String[6];
		String[] tokens;
		String[] suffixes;
		Matcher ma1, ma2;		
		String owner, spouse;
		int len = lines.length;
		if (len == 1)
			len = 3;
		for (int i = 0; i < len - 2; i++) {
			lines[i] = lines[i].replaceFirst("-$", "");
			lines[i] = StringFormats.unifyNameDelim(lines[i]);
			if (lines[i].length() != 0) {
				if (lines[i].startsWith("&")) {
					lines[i] = lines[i].replaceFirst("&\\s*", "");
					a = StringFormats.parseNameDesotoRO(lines[i]);
					suffixes = extractNameSuffixes(a);
					addOwnerNames(a, suffixes[0], suffixes[1], NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
					continue;
				}
				owner = lines[i];
				spouse = "";
				ma1 = Pattern.compile("(.+?)\\s*&\\s*(.+)").matcher(owner);
				if (ma1.matches()) {
					owner = ma1.group(1);
					spouse = ma1.group(2);
				}
				boolean ownerIsCompany = false;
				if (NameUtils.isCompany(owner)) {
					a[0] = ""; 
					a[1] = ""; 
					a[3] = ""; 
					a[4] = ""; 
					a[5] = "";
	            	if (NameUtils.isCompany(spouse)){
	            		a[2] = lines[i];
	            		addOwnerNames(a, "", "", true, false, body);  
	            		continue;
	            	} else {
	            		a[2] = owner;
	                	addOwnerNames(a, "", "", true, false, body);  
	                	ownerIsCompany = true;
	            	}
				} else if (NameUtils.isCompany(lines[i])) {
					a[2] = lines[i];
					a[0] = "";
					a[1] = "";
					a[3] = "";
					a[4] = "";
					a[5] = "";
					addOwnerNames(a, "", "", true, false, body);
					continue;
				} 
				String spouseSuffix = "";
				if (spouse.length() != 0) {
					boolean spouseIsFL = false;
					ma2 = nameSuffix.matcher(spouse);
					if (ma2.matches()) { // spouse has suffix => remove it
						spouse = ma2.group(1).trim();
						spouseSuffix = ma2.group(2);
						lines[i] = ma1.group(1) + " & " + spouse;
					}
					spouse = spouse.replaceFirst("^([A-Z]+)-([A-Z]+)$", "$1 $2"); // LAWRENCE, DARYL & LAURA-PAYNE
					tokens = spouse.split(" ");
					// if spouse is F MI? L then parse it separately
					if (spouse.matches("[A-Z]{2,} [A-Z]+ [A-Z'-]{2,}")) {
						spouseIsFL = true;
					}
					if (!ownerIsCompany){
						a = StringFormats.parseNameNashville(owner);
						if (!spouse.contains(",")) {
							if (!tokens[0].contains(a[2]) && ((tokens.length == 1)	|| (tokens.length == 2 && spouseSuffix.length() == 0)
									|| (tokens.length == 2 && spouseSuffix.length() != 0 && tokens[1].length() == 1)) 
									|| (tokens.length == 3 && tokens[2].length() == 1 && tokens[1].length() == 1)) { 
								spouse = a[2] + " " + spouse;
							}
						}
						if (!spouseIsFL) {
							b = StringFormats.parseNameNashville(spouse);
						} else {
							b = StringFormats.parseNameDesotoRO(spouse);
						}
						a[3] = b[0];
						a[4] = b[1];
						a[5] = b[2];
					} else {
						a = StringFormats.parseNameNashville(spouse);
						addOwnerNames(a, spouseSuffix, "", NameUtils.isCompany(spouse), false, body);
						continue;
					}
				} else if (!ownerIsCompany){
					a = StringFormats.parseNameNashville(owner);
				}
				if (!ownerIsCompany){
					suffixes = extractNameSuffixes(a);
					if (spouseSuffix.length() != 0) {
						suffixes[1] = spouseSuffix;
					}
					addOwnerNames(a, suffixes[0], suffixes[1], false, NameUtils.isCompany(a[5]), body);
				}
			}
		}
		storeOwnerInPartyNames(m, body);
	}
    
    public static void partyNamesMOClayTR(ResultMap m, long searchId)throws Exception{
        String name = (String) m.get("tmpOwnerFullName");
        if (name == null)
        	return; 
        String addr = (String) m.get("tmpOwnerAddr");
		if (!StringUtils.isEmpty(addr)){
			name = name + "@@" + addr;
		}
        partyNamesTokenizerMOClayTR(m, name); 
	}
    
    public static void partyNamesTokenizerMOClayTR(ResultMap m, String s) throws Exception {
    	
    	// corrections and cleanup
    	s = s.replaceAll("(?<=[A-Z])\\s?-\\s?(?=[A-Z])", "-");
    	s = s.replaceAll("^\\d+(?=$|\\s*@@)", "");
        s = s.replaceAll("\\([^\\)]*\\)?", "");
        s = s.replaceAll("\\{[^\\}]*\\}?", "");
    	s = s.replaceAll("\\s*[/\\,\\s-]+(?=$|\\s*@@)", "");
    	s = s.replaceAll("\\.", " ");
    	s = s.replaceAll("\\bDDS\\b", "");
    	s = s.replaceAll("#\\d+(?=$|\\s*@@)", "");
    	s = s.replaceAll("(.+) \\\\ (TRUST)(?=$|\\s*@@)", "$1 \\\\ $1 $2");  // WILLIAMS, THOMAS S & JANE A \ TRUST
    	s = s.replaceFirst("(?<=& )([A-Z]+)-([A-Z]+)(?=$|\\s*@@)", "$1 $2");
		s = s.replaceFirst("\\bTR(USTEE)?S?\\b", "");	
        s = s.replaceAll("\\s{2,}", " ").trim();
        s = s.replaceAll("\\s(?=,)", "");
        
        String addr = "";
        Matcher ma = Pattern.compile("(.*)\\s*@@\\s*(.*)").matcher(s);
        if (ma.find()){
        	s = ma.group(1);
        	addr = ma.group(2);
        }
        // address might contains at the beginning an additional owner name
        // e.g. owner = BROWN SHARON D; addr = PFEIFFENBERGER, MARTHAINE B 3000 NE 57TH TER, KANSAS CITY, MO 64119 UNITED STATES (PID 13919000602700)
        if (addr.contains(",") || addr.startsWith("%")){
			ma = Pattern.compile("(%?.*?)\\s*\\b(PO BOX|RR \\d+ BOX|\\d+)\\b").matcher(addr);
			if (ma.find()){
				String addOwner = ma.group(1); 
				if (addOwner.length() != 0){
					if (addOwner.equals("TRUST")){
						s = s + " " + addOwner;
					} else {
						s = s + "\\" + addOwner;
					}
				}
			} 
        }
        String entities[] = s.split("\\s?\\\\\\s?");

        List<List> body = new ArrayList<List>();
        
        // parse each entity as L F M
        String[] a = new String[6];
        String[] b = new String[6];
        String[] tokens; 
        String[] addOwners;
        String[] suffixes;
        Matcher ma1, ma2;
        String owner, spouse, owner3;        
        for (int i=0; i<entities.length; i++){
        	boolean ownerIsFL = false;
        	if (entities[i].startsWith("%")){
        		ownerIsFL = true;
    			entities[i] = entities[i].replaceFirst("%\\s*", "");
    		}
        	entities[i] = StringFormats.unifyNameDelim(entities[i]);
        	owner = entities[i]; 
        	spouse = "";
        	owner3 = "";
        	ma1 = Pattern.compile("(.*?)\\s*D/?B/?A/?\\s*(.+)").matcher(owner);
        	if (ma1.matches()){
        		owner = ma1.group(1);
        		a[2] = ma1.group(2); 
            	a[0] = ""; a[1] = ""; a[3] = ""; a[4] = ""; a[5] = "";
            	addOwnerNames(a, "", "", true, false, body);    
        	} 
        	ma1 = Pattern.compile("(.+?)\\s*&\\s*([^&]+)&?(.*)").matcher(owner);
        	if (ma1.matches()){
        		owner = ma1.group(1);
        		spouse = ma1.group(2);
        		owner3 = ma1.group(3);
        	} 
			boolean ownerIsCompany = false;
        	if (NameUtils.isCompany(owner)){        		 
            	a[0] = ""; a[1] = ""; a[3] = ""; a[4] = ""; a[5] = "";
            	if (NameUtils.isCompany(spouse)){
            		a[2] = entities[i];
            		addOwnerNames(a, "", "", true, false, body);  
            		continue;
            	} else {
            		a[2] = owner;
                	addOwnerNames(a, "", "", true, false, body);  
                	ownerIsCompany = true;
            	}
        	} else if (NameUtils.isCompany(entities[i])){
        		a[2] = entities[i]; 
            	a[0] = ""; a[1] = ""; a[3] = ""; a[4] = ""; a[5] = "";
            	addOwnerNames(a, "", "", true, false, body);  
            	continue;
        	}
			String spouseSuffix = "";
    		if (spouse.length() != 0){
        		boolean spouseIsFL = false;
    			ma2 = nameSuffix.matcher(spouse);
        		if (ma2.matches()){		// spouse has suffix => remove it 
        			spouse = ma2.group(1).trim();
        			spouseSuffix = ma2.group(2);
        			entities[i] = ma1.group(1) + " & " + spouse;
        		}        		        		
        		spouse = spouse.replaceFirst("^([A-Z]+)-([A-Z]+ [A-Z']{2,})$", "$1 $2"); // SIMS RICHARD & GARI-DAWN TINGLER        		
        		tokens = spouse.split(" ");
        		// if spouse is F MI? L then parse it separately   
        		if (spouse.matches("[A-Z]{2,} [A-Z]+ [A-Z'-]{2,}")){
        			spouseIsFL = true;
        		} 
        		if (!ownerIsCompany){
        			if (!ownerIsFL){
        				a = StringFormats.parseNameNashville(owner);
        			} else {
        				a = StringFormats.parseNameDesotoRO(owner);
        			}
	        		if (!spouse.contains(",")) {
		        		if (!tokens[0].contains(a[2]) && ((tokens.length == 1) ||  (tokens.length == 2 && spouseSuffix.length() == 0)
		        				|| (tokens.length == 2 && spouseSuffix.length() != 0 && tokens[1].length() == 1)) // DAVIDSON PENNEY & JOSEPH K JR
		        				|| (tokens.length == 3 && tokens[2].length() == 1 && tokens[1].length() == 1)){ // ANDERSON CHARLES ROY III & JENNIFER C U
			        			spouse = spouse.replaceFirst("^([A-Z]+)-([A-Z]+ [A-Z])$", "$1 $2"); // BLACK JOSHUA P & CORA-LEE M
		        				spouse = a[2] + " " + spouse;  
		        		}
	        		}
	    			if (!spouseIsFL){
	    				b = StringFormats.parseNameNashville(spouse);
	    			} else {
	    				b = StringFormats.parseNameDesotoRO(spouse);
	    			}
	    			a[3] = b[0];
	    			a[4] = b[1];
	    			a[5] = b[2];  
        		} else {        			
        			a = StringFormats.parseNameNashville(spouse);
					addOwnerNames(a, spouseSuffix, "", NameUtils.isCompany(spouse), false, body);
					continue; 
				}
			} else if (!ownerIsCompany){
				if (!ownerIsFL){
					a = StringFormats.parseNameNashville(owner);
				} else {
					a = StringFormats.parseNameDesotoRO(owner);
				}
			}
			if (!ownerIsCompany){
				suffixes = extractNameSuffixes(a);
				if (spouseSuffix.length() != 0) {
					suffixes[1] = spouseSuffix;
				}
				addOwnerNames(a, suffixes[0], suffixes[1], false, NameUtils.isCompany(spouse), body);
			}
			String prevLast = a[5];	       
        	if (owner3.length() != 0){
        		addOwners = owner3.split("\\s?&\\s?");
        		for (int j=0; j<addOwners.length; j++){
        			if (!addOwners[j].matches("[A-Z'-]{2,} [A-Z]+ [A-Z]+")){
        				addOwners[j] = prevLast + " " + addOwners[j];
        			}
        			a = StringFormats.parseNameNashville(addOwners[j]);
        			prevLast = a[2];
        			suffixes = extractNameSuffixes(a);
        			addOwnerNames(a, suffixes[0], suffixes[1], NameUtils.isCompany(addOwners[j]), false, body);
        		}
        	}
        }
        storeOwnerInPartyNames(m, body);                
    }
    
    public static void partyNamesTNShelbyTR(ResultMap m, long searchId)throws Exception{
        String name = (String) m.get("tmpOwnerFullName");
        if (name == null)
        	return; 
        TNShelbyTR.partyNamesTokenizerTNShelbyTR(m, name); 
	}
    
    public static void partyNamesMOJacksonTR(ResultMap m, long searchId)throws Exception{
        String name = (String) m.get("tmpOwnerFullName");
        if (name == null)
        	return; 
        MOJacksonTR.partyNamesTokenizerMOJacksonTR(m, name, searchId);
	}    
    
    public static void subdivMOJacksonTR(ResultMap m,long searchId)throws Exception{
    	MOJacksonTR.subdivMOJacksonTR(m, searchId);
    }
    
    //extract the subdivision name from a Jackson RO legal description string that contains one subdivision
    protected static String parseSubdivFromLegalJacksonRO(String subdiv,long searchId){
        String garbage[] = {"LOT(\\d+-\\d+)","[A-Z]+\\d+-\\d+","\\d+-\\d+","\\n|\\r","(?i)\\dTH PL & REPLAT"," KC"};
        String before[] = {"RANG E"," TR([A-Z]) ", "-", "\\s{2,}" , "'"}; //bug #657: don't remove "-", but replace it with space (ex. SNI-A-BAR, DAVIS BLU-RIDGE)
        String after[] = {"RANGE"," TR $1", " ", " ", ""};
//        String tokens[] = {" TRACT"," BEG "," ALL"," PT " ," RES "," BLK ", " BLKS "," BLOCKS ", " LOT "," LOTS "," E "," W "," N "," S "," SUB ",
//                " SUBDIVISION"," COMMON "," ---LOT ","(" , " LTS" , "---Units", " UNITS","---Unit" , "BLK"," AM ", " LT "," LOTS" ," TR ",
//                " CORR PLAT ", " RESURVEY" //PID 26-720-09-08-00-0-00-000
//                };    	
        int nrTok = 0;
			try {
				StringTokenizer tokens = new StringTokenizer(subdiv);
				nrTok = tokens.countTokens();
			} catch (Exception e) {
				e.printStackTrace();
				}
		if (nrTok > 4) //B 2877
			subdiv = subdiv.replaceFirst("(.*)\\s(.*)\\s(.*)", "$1");
    	subdiv = LegalDescription.cleanGarbage(garbage,subdiv);
    	subdiv = LegalDescription.prepare(subdiv, before,after);
    	
    	subdiv = subdiv.replaceFirst("\\bCOS\\b.*", "CO'S"); //fix for B3425
    	
    	//subdiv = LegalDescription.extractSubdivisionNameUntilToken(subdiv,tokens);
        if (!subdiv.contains("CO'S"))
    	subdiv = subdiv.replaceFirst("(?i)(.*?)\\s*(\\b(TR(ACT)?|BEG|ALL|PT|RES(URVEY)?|BL(OC)?KS?|LO?TS?|E|W|N|S|SUB(DIVISION)?|COMMON|UNITS?|AM|CORR PLAT)\\b"+
    			"|\\().*", "$1"); 
    			 //RESURVEY: PID 26-720-09-08-00-0-00-000
                 // replaced extractSubdivisionNameUntilToken with a regular expression for bug #1670
            
    	subdiv = subdiv.replaceAll(
				"(.*?)?(\\&|THE|,|1ST|)( RP| REP| REPLAT| REPLATS)( \\d+| PART| NO?. \\d+|$)", "$1");
    	subdiv = subdiv.replaceAll("(.*?)(-?\\sREPLAT$)", "$1");
    	
    	// '&' is also a token that can be used to delimit the subdivision name, but only if it doesn't occur after the fist word in 'subdiv' string
    	// I will increase the limit to the seccond word, hope I will not damage anything... :)
    	if (subdiv.indexOf('&') != -1){
    		if(!subdiv.matches("\\w+\\s+\\w+\\b\\s*&\\s*.*"))
    			subdiv = subdiv.replaceFirst("\\s*&.*", "");
    	}   
    	
    	subdiv = subdiv.replaceAll("(.*?)\\s*LOTS?\\s*\\d+.*", "$1");
        
    	subdiv = subdiv.replaceAll(
				"(.*?)(\\d+\\s*ST|\\d+ND|\\d+RD|\\d+TH|FIRST|SECOND|THIRD|FOURTH|FIFTH|SIXTH|SEVENTH|\\WEIGHTH?|NINTH|" +
				"TENTH|TWELFTH|TWENTIETH|EIGHTY-FIFTH|FIFTY-SEVENTH|FIFTY-NINTH)(.*?REPLATS| REPLAT| REPL| REP|" +
				" ADDITION| ADD| PHASE| RESURVEY| RES).*", "$1");
        
    	subdiv = subdiv.replaceAll("(.*?)\\s(CORRECTED|CORREC|CORR|COR||FINAL|AMENDED|AMEND|" +
        		"AMEN|REVISED)\\s*(PT|PL|PLATS?)?\\s*$", "$1");
        
        //cleanup for bug #452
    	subdiv = subdiv.replaceAll("(?i)\\s*(NO|#)\\s?\\d+", "");
    	
    	subdiv = subdiv.replaceAll("(.*?)\\s(TO|\\d+|ADDITION #\\d*|\\d+\\s*PLA?T?|\\d+P|PHASE.*?|NO|CONTINUATION)$", "$1");
    	subdiv = subdiv.replaceAll("(.*?)\\s(ADDITION|ADD)\\s*(CONTINUATION|NO\\d*)?$", "$1");
    	
    	subdiv = subdiv.replaceAll("(?<=ANNEXED) PARTITION", ""); // fix for bug #599
    	
    	subdiv = subdiv.replaceFirst("\\s*&\\s*$", "");    	

    	return subdiv;
    }
    
    public static void subdivMOJacksonRO(ResultMap m,long searchId)throws Exception{
    	
        String tmp2 = (String) m.get("tmpLegal2");
        if (tmp2 !=null) {
            tmp2 = tmp2.replaceAll("\\n|\\r","");
            tmp2 = tmp2.replaceAll("\\s{2,}"," ");
            tmp2 = tmp2.replaceAll("<TR.*?>","");
            tmp2 = tmp2.replaceAll("</TR>","");
            int istart = 0, iend = 0;
            
            istart = tmp2.indexOf("</TD>");
            istart = istart + "</TD>".length();
 
            //section
            istart = tmp2.indexOf("<TD>",istart);
            istart = istart + "<TD>".length();
            iend = tmp2.indexOf("<",istart);
            String sec = tmp2.substring(istart,iend);
            m.put("PropertyIdentificationSet.SubdivisionSection",sec);
            
            //township
            istart = tmp2.indexOf("<TD>",istart);
            istart = istart + "<TD>".length();
            iend = tmp2.indexOf("<",istart);
            String town = tmp2.substring(istart,iend);
            m.put("PropertyIdentificationSet.SubdivisionTownship",town);

            //range
            istart = tmp2.indexOf("<TD>",istart);
            istart = istart + "<TD>".length();
            iend = tmp2.indexOf("<",istart);
            String rng = tmp2.substring(istart,iend);
            m.put("PropertyIdentificationSet.SubdivisionRange",rng);

        }
                 
        ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
        if (pis == null)
        	return;
        
	    m.put("PropertyIdentificationSet.PropertyDescription", pis.body[0][1]);
	    
        int len = pis.getLength();

		//Lot Number		
		// if the document legal description table contains only one legal description
        if (len == 1) {
        	String lot = pis.body[0][4];
        	if (lot == null) {
        		lot = "";
        	}
        	// if lot is missing from legal descr        	
        	if (lot.length() == 0) {
	        	//try to parse it from
	        	// first - Grantee
		        String tmpLotArea = (String) m.get("SaleDataSet.Grantee");
		        tmpLotArea = tmpLotArea.replaceAll(","," & ");
		        tmpLotArea = tmpLotArea.replaceAll("\\s{2,}"," ");
		                
		        if ((tmpLotArea.indexOf(" LOT ")!=-1)|| (tmpLotArea.indexOf(" LOTS ")!=-1)||(tmpLotArea.indexOf(" LTS ")!=-1)) {
		        	lot = LegalDescription.extractLotFromText(tmpLotArea);
		        	
	        	// second - from subdivision name		        	
		        } else {
		        	lot = LegalDescription.extractLotFromText(pis.body[0][1].replaceFirst("(.*)\\s(.*)\\s(.*)", "$1"));
		        }
		        
	        	pis.body[0][4] = lot;
	    		m.put("PropertyIdentificationSet", pis);		        
        	} 
        } 

        // try to identify if a legal description row from the legal description table contains 2 legals, actually 2 names for the same subdiv - ex. RO Jackson Instr# 2003I0001654
        Pattern p = Pattern.compile("(.+)\\s+AKA\\s+(.+)");
        int last = len-1;
        
        for (int i=0; i<len; i++) {        	
        	String subdiv = pis.body[i][1];
        	Matcher ma = p.matcher(subdiv);
            // if there are 2 legals in one legal description table row, parse each of them and add them to legals table on separate rows        	
        	if(ma.find()) {
        		String subdiv1 = parseSubdivFromLegalJacksonRO(ma.group(1),searchId);
        		String subdiv2 = parseSubdivFromLegalJacksonRO(ma.group(2),searchId);
        		            		
        		last ++;
        		
        		String[][] new_body = new String[last+1][5];
        		System.arraycopy(pis.body, 0, new_body, 0, last);
        		pis.body = new_body;

        		pis.body[i][1] = subdiv1;
        		pis.body[last][1] = subdiv2;
        		
        		pis.body[last][2] = pis.body[i][2];
        		pis.body[last][3] = pis.body[i][3];
        		pis.body[last][4] = pis.body[i][4];
        		pis.body[last][0] = String.valueOf(last+1);
        	} else {
        		pis.body[i][1] = parseSubdivFromLegalJacksonRO(subdiv,searchId);
        	}
        	pis.body[i][2] = pis.body[i][2].replaceAll("(?is)\\bUNKNOWN\\s+CODES\\b", "");
        }
        		
	    m.put("PropertyIdentificationSet", pis);
    }
    
    public static void subdivMOJacksonOR(ResultMap m,long searchId)throws Exception{
    	
    	ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
    	
        if (pis == null || !pis.head[0].equals("Subdivision")){
        	subdivisionIntermOR(m,searchId);
        	return;
        }
        
        int len = pis.getLength();
        for (int i=0; i<len; i++){
        	pis.body[i][0] = cleanSubdivIntermOR(pis.body[i][0],searchId);
        }
        
        
	    m.put("PropertyIdentificationSet", pis);
    }
    
    public static void convertSubdivisionNameRomanNumbersToArab(ResultMap m, long searchId) throws Exception {

		String keyName = PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName();
		String subdivisionName = (String) m.get(keyName);
		if (StringUtils.isNotEmpty(subdivisionName)) {
			String normalisedSubdivision = convertFromRomanToArab(subdivisionName);
			m.put(keyName, normalisedSubdivision);
		}
	}

	/**
	 * @param subdivisionName
	 * @return
	 */
	public static String convertFromRomanToArab(String subdivisionName) {
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		String normalisedSubdivision = Roman.normalizeRomanNumbersExceptTokens(subdivisionName, exceptionTokens);
		return normalisedSubdivision;
	}

    
    public static void main(String args[]) throws Exception {
        //ResultMap m = new ResultMap();
        //        m.put("tmpLegalDescription", "ExtDesc: BRENTWOOD TRACE NO 5847");
        //        legalDescriptionNashvilleRO(m);
        //m.put("PropertyIdentificationSet.OwnerLastName",
          //      "SMITH, JOHN E. & NIKOLAI, MICHELLE M.");
        //stdPisNashvilleAO(m);
        //logger.info(m);
    }
    
    public static void taxClayTR(ResultMap m,long searchId) throws Exception
    {
    	
    	String priorDelinq =  sum((String)m.get("tmpPriorDelinquent"), searchId);
    	m.put("TaxHistorySet.PriorDelinquent", priorDelinq);
    	
    	String totalDue = (String)m.get("TaxHistorySet.TotalDue");    	
    	if (totalDue == null){
    		totalDue = "0.00";    		
    	}
    	
    	BigDecimal totalDelinq = new BigDecimal(priorDelinq);
    	Date dueDate = DBManager.getDueDateForCountyAndCommunity(
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue(), 
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().longValue());
    	Date now = new Date();
    	if (now.after(dueDate)){
    		totalDelinq = totalDelinq.add(new BigDecimal(totalDue));
    	}
    	m.put("TaxHistorySet.DelinquentAmount", totalDelinq.toString());
    	
        String year = (String) m.get( "TaxHistorySet.Year" );
        if (year == null || year.length() == 0) 
        	return;
                
        ResultTable history = (ResultTable) m.get("TaxHistorySet");
        if (history == null) 
        	return;
        
        String body[][] = history.body;
        int len = body.length;
        BigDecimal amountPaid = new BigDecimal(0);
        String yearStmt = "";
        for (int i=0; i<len; i++){
        	yearStmt = body[i][1].replaceFirst("(\\d{4})-.*", "$1");
        	if (year.equals(yearStmt)){
        		amountPaid = amountPaid.add(new BigDecimal(body[i][7]));
        	}
        }
        m.put( "TaxHistorySet.AmountPaid", amountPaid.toString());

    }
    
    public static void taxMOJacksonTR(ResultMap m,long searchId) throws Exception {
    	MOJacksonTR.taxMOJacksonTR(m, searchId,"MO", "Jackson", "TR");
    }
    
    public static void taxMOClayTR(ResultMap m,long searchId) throws Exception {
    	MOJacksonTR.taxMOJacksonTR(m, searchId,"MO", "Clay", "TR");
    }
    
    public static void parsePCJacksonMO(ResultMap m,long searchId) throws Exception {
        String court = (String) m.get("SaleDataSet.InstrumentNumber");
        int index = -2;
        if (court.indexOf("<table") != -1 
        		&& (index = court.indexOf("Bankruptcy Petition #:")) == -1
        		&& !court.contains("Court of Appeals Docket #:")) {
        	court = court.substring(0, court.indexOf("<table"));
        }
        
        court = court.replaceAll("\\n|\\r","");
        court = court.replaceAll("\\s{2,}"," ");
        court = court.replaceAll("\\*","");
        court = court.replaceAll("</?b>", "");
        court = court.replaceAll("</?font[^>]*>", "");
        
        
        String instr="";
        String grantor="";
        String date = "";
        int docket=0;
        
       
        boolean foundBankruptcy = false;
        if(index == -2) {
        	index = court.indexOf("Bankruptcy Petition #:");
        }
        if(index != -1 ) {
        	Pattern pattern = Pattern.compile(
        			"(?i)(?s)((?:\\d{1}:)?\\d{2}(?:-[a-z]{2})?-\\d{5}-[a-z]{3}\\d{0,2})");
        	Matcher matcher = pattern.matcher(court.substring(court.indexOf("Bankruptcy Petition #:") + "Bankruptcy Petition #:".length()));
        	if(matcher.find()) {
        		foundBankruptcy = true;
        		instr=matcher.group(1);
                m.put("SaleDataSet.DocumentType","Bankruptcy Court");
                m.put("SaleDataSet.Grantee","Bankruptcy Court");
        	} else {
        		pattern = Pattern.compile(
    			"(?i)(?s)((?:\\d{1}:)?\\d{2}(?:-[a-z]{2})?-\\d{5}(?:-\\w+)?)");
        		matcher = pattern.matcher(court.substring(court.indexOf("Bankruptcy Petition #:") + "Bankruptcy Petition #:".length()));
            	if(matcher.find()) {
            		foundBankruptcy = true;
            		instr=matcher.group(1);
            		
                    m.put("SaleDataSet.DocumentType","Bankruptcy Court");
                    m.put("SaleDataSet.Grantee","Bankruptcy Court");
            	}
        	}
        	index = court.indexOf("Date filed:");
    		if(foundBankruptcy && index > 0) {
    			String temp = court.substring(index + "Date filed:".length());
    			temp = temp.replaceAll("<[^>]+>", "");
    			temp = temp.replaceAll("&nbsp;", "");
    			pattern = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
    			matcher = pattern.matcher(temp);
    			if(matcher.find()) {
    				date = matcher.group(1);
    			}
    		}
    		index = court.indexOf("<i>Debtor</i><br>");
    		if(foundBankruptcy && index > 0) {
    			String temp = court.substring(index + "<i>Debtor</i><br>".length());
    			index = temp.indexOf("<br>");
    			grantor = temp.substring(0, index);
    		}
        	
        } else {
        	court = court.replaceAll("</?br>", "");
        }
        
        if ((index = court.indexOf("Court of Appeals Docket #:")) != -1 ) {
            docket = index + "Court of Appeals Docket #:".length(); 
            court = court.replaceAll(court.substring(0,docket),"");
            court=court.trim();
        }
        
        Pattern p0 = Pattern.compile("(?i)(?s)((?:\\d{1}:)?\\d{2}(?:-[a-z]{2})?-\\d{5}(?:-\\w+)?)(.*)(Case\\stype:)(.*)(Date[\\s]filed:.)(\\d{1,2}/\\d{1,2}/\\d{4}).*"); //bug #715 fix
        Matcher m0 = p0.matcher(court);
        if (m0.find()) {
            instr=m0.group(1);
            grantor=m0.group(2);
            date = m0.group(6);
            m.put("SaleDataSet.DocumentType","Bankruptcy Court");
            m.put("SaleDataSet.Grantee","Bankruptcy Court");
            foundBankruptcy = true;
        }
        
        Pattern p1 = Pattern.compile("(?i)(?s)((?:\\d{1}:)?\\d{2}(?:-[a-z]{2})?-\\d{5}-[a-z]{3}\\d{0,2})(.*)(Case\\stype:)(.*)(Date[\\s]filed:.)(\\d{1,2}/\\d{1,2}/\\d{4}).*");
        Matcher mm = p1.matcher(court);
        if (mm.find()) {
            instr=mm.group(1);
            grantor=mm.group(2);
            date = mm.group(6);
            m.put("SaleDataSet.DocumentType","Bankruptcy Court");
            m.put("SaleDataSet.Grantee","Bankruptcy Court");
            foundBankruptcy = true;
        }
        
       
        
        
        
    
        if (!foundBankruptcy){
	        Pattern p02 = Pattern.compile("(?i)(?s)(\\d{1}:\\d{2}-[a-z]{2}-\\d{5}-[A-Z]{3}-[A-Z]{3})(.*)(,?).*(Date[\\s]filed:)\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*");
	        Matcher mmm02 = p02.matcher(court);
	        if (mmm02.find()) {
	            instr=mmm02.group(1);
	            grantor=mmm02.group(2);
	            date = mmm02.group(5);
	            m.put("SaleDataSet.DocumentType","District Court Civil");
	            m.put("SaleDataSet.Grantee","District Court");
	            if (grantor.indexOf(",")!=-1) {
	                grantor = grantor.substring(0,grantor.indexOf(","));
	            }
	        }else {
	            Pattern p2 = Pattern.compile("(?i)(?s)(\\d{1}:\\d{2}-[a-z]{2}-\\d{5}-[A-Z]{3})(.*)(,?).*(Date[\\s]filed:)\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*");
	            Matcher mmm = p2.matcher(court);
	            if (mmm.find()) {
	                instr=mmm.group(1);
	                grantor=mmm.group(2);
	                date = mmm.group(5);
	                if(instr.matches("(\\d{1}:\\d{2}-cr-\\d{5}-[A-Z]{3})")) {
	                	m.put("SaleDataSet.DocumentType","District Court Criminal");
	                } else {
	                	m.put("SaleDataSet.DocumentType","District Court Civil");
	                }
	                m.put("SaleDataSet.Grantee","District Court");
	                if (grantor.indexOf(",")!=-1) {
	                    grantor = grantor.substring(0,grantor.indexOf(","));
	                }
	            }else {
	                Pattern p3 = Pattern.compile("(?i)(?s)(\\d{1}:\\d{2}-[a-z]{2}-\\d{5})(.*)(Date[\\s]filed:)\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*");
	                Matcher mmmm = p3.matcher(court);
	                if (mmmm.find()) {
	                    instr=mmmm.group(1);
	                    grantor=mmmm.group(2);
	                    date = mmmm.group(4);
	                    if(instr.matches("(\\d{1}:\\d{2}-cv-\\d{5})")) {
	                    	m.put("SaleDataSet.DocumentType","District Court Civil");
		                } else {
		                	m.put("SaleDataSet.DocumentType","District Court Criminal");
		                	
		                }
	                    m.put("SaleDataSet.Grantee","District Court");
	                    if (grantor.indexOf(",")!=-1) {
	                        grantor = grantor.substring(0,grantor.indexOf(","));
	                    }
	                }
	            }
	        }
        
        }
       
        Pattern p4 = Pattern.compile("(?i)(?s)(\\d{2}-\\d{4})\\s(Filed:)\\s(\\d{1,2}/\\d{1,2}/\\d{2})(.*)(v.?).*");
        Matcher n = p4.matcher(court);
        if (n.find()) {
            instr=n.group(1);
            grantor=n.group(4);
            date = n.group(3);
            m.put("SaleDataSet.DocumentType","Appelate Court");
            m.put("SaleDataSet.Grantee","Appelate Court");
            grantor=grantor.trim();
            if (grantor.indexOf("v.")!=-1) {
                grantor = grantor.substring(0,grantor.indexOf("v."));
            }
            if (grantor.indexOf(",")!=-1) {
                grantor = grantor.substring(0,grantor.indexOf(","));
            }
            if (grantor.indexOf(":")!=-1) {
                grantor = grantor.replaceAll(grantor.substring(0,grantor.indexOf(":")+1),"");
                grantor = grantor.replaceAll("\\d","");
                grantor=grantor.trim();
            }
        }
        
        instr=instr.trim();
        grantor=grantor.trim();
        date=date.trim();
        m.put("SaleDataSet.InstrumentNumber",instr);
        m.put("SaleDataSet.Grantor",grantor);
        m.put("SaleDataSet.RecordedDate",date);
        String[] u = StringFormats.parseNameNashville(grantor);
        if (u[2].length() > 0 && u[0].length() == 0 && u[1].length() == 0){// for B 4582, il cook ex
        	u[1] = u[2];
        	u[2] = "";
        }
        if (u[0].length() > 1 && u[1].length() == 0){// for B 4582, il kane ex
        	u[1] = u[0];
        	u[0] = "";
        }
        if (u[3].length() > 1 && u[4].length() == 0){// for B 4582, il kane ex
        	u[4] = u[3];
        	u[3] = "";
        }
        m.put("PropertyIdentificationSet.OwnerFirstName", u[2]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", u[0]);
        m.put("PropertyIdentificationSet.OwnerLastName", u[1]);
        m.put("PropertyIdentificationSet.SpouseFirstName", u[5]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", u[3]);
        m.put("PropertyIdentificationSet.SpouseLastName", u[4]);
        ArrayList<String> line = new ArrayList<String>();
        List<List> body = new ArrayList<List>();
        line.add(u[2]);
        line.add(u[0]);
        line.add(u[1]);
        body.add(line);
        line = new ArrayList<String>();
        line.add(u[5]);
        line.add(u[3]);
        line.add(u[4]);
        body.add(line);
        ResultTable rt = addNamesMOJacksonRO(body);
        m.put("GrantorSet", rt);
        String grantee = (String)m.get("SaleDataSet.Grantee");
        if (grantee!=null){
	        line = new ArrayList<String>();
	        line.add("");
	        line.add("");
	       
	        line.add(grantee);
	        body.clear();
	        body.add(line);
	        rt = addNamesMOJacksonRO(body);
	        m.put("GranteeSet", rt);
        }
    }
    
    public static void courtsJacksonMO(ResultMap m,long searchId) throws Exception{
        
        String tmp = (String)m.get("CourtDocumentIdentificationSet.CaseStyle");
    	if (tmp==null) {
    	    return;
    	}
        String tmp2 = (String)m.get("tmpPartiesColumn");
        if (tmp2 == null){
        	tmp2 = "";
        }
        tmp = tmp.replaceAll("(?i)\\bM A C (Builders)", "MAC $1"); // fix for bug #1405
           
        int index, lastIndex=-1, center;
        center = tmp.length()/2;
        String grantor = "";
        String grantee = "";
        if ((index=tmp.indexOf(" VS"))!=-1) {
            grantor = tmp.substring(0,index);
            grantee = tmp.substring(index+" VS.".length());
        }
        else {
            if ((index=tmp.indexOf(" -VS- "))!=-1) {
                grantor = tmp.substring(0,index);
                grantee = tmp.substring(index+" -VS- ".length());
            }
            else {
                lastIndex = tmp.indexOf(" V ");
                if (lastIndex==-1) {
                    grantor = tmp;
                }else {
                    index= lastIndex;
                    do{
                        if (Math.abs(lastIndex-center)< Math.abs(index-center)) {
                            index = lastIndex;
                        }
                    } while ((lastIndex=tmp.indexOf(" V ",lastIndex+" V ".length()))!=-1);
                    grantor = tmp.substring(0,index);
                    grantee = tmp.substring(index+" V ".length());
                }
            }

        }
        HashMap<String, List<List>> parties = courtsJacksonMOTokenizer(tmp2, grantor, grantee);
        if (parties.get("Grantee") != null && parties.get("Grantee").size() > 0){
        	ResultTable gees = addNamesMOJacksonRO(parties.get("Grantee"));
        	m.put("GranteeSet", gees);
        	//storeOwnerInPartyNames(m, parties.get("Grantee"), "GranteeSet");
        }
        if (parties.get("Grantor") != null && parties.get("Grantor").size() > 0){
        	ResultTable gees = addNamesMOJacksonRO(parties.get("Grantor"));
        	m.put("GrantorSet", gees);
        	//storeOwnerInPartyNames(m, parties.get("Grantor"), "GrantorSet");
        }        
        m.put("SaleDataSet.Grantor",grantor);
        m.put("SaleDataSet.Grantee",grantee);
    }
   
    public static HashMap<String, List<List>> courtsJacksonMOTokenizer(String parties, String grantor, String grantee){
    	HashMap<String, List<List>> p = new HashMap<String, List<List>>();
    	List<List> grantorBody = new ArrayList<List>();
    	List<List> granteeBody = new ArrayList<List>();
    	String[] suffixes;
    	if (StringUtils.isEmpty(parties)){
    		String[] names = {"", "", "", "", "", ""};
    		boolean grantorCompany = false;
    		if (NameUtils.isCompany(grantor)){
    			grantorCompany = true;
    			names[2] = grantor;
    		} else {
    			grantor = NameCleaner.cleanName(grantor);
    			names = StringFormats.parseNameFMWFWML(grantor, new Vector<String>());
    		}
			//suffixes = GenericFunctions.extractAllNamesSufixes(names);
    		//GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], grantorCompany, false, grantorBody);
			addOwnerNames(names, grantorBody);
    		//grantee
    		boolean granteeCompany = false;
    		String[] names2 = {"", "", "", "", "", ""};
    		if (NameUtils.isCompany(grantee)){
    			granteeCompany = true;
    			names2[2] = grantee;
    		} else {
    			grantee = NameCleaner.cleanName(grantee);
    			names2 = StringFormats.parseNameFMWFWML(grantee, new Vector<String>());
    		}
			//suffixes = GenericFunctions.extractAllNamesSufixes(names2);
    		//GenericFunctions.addOwnerNames(names2, suffixes[0], suffixes[1], granteeCompany, false, granteeBody);
			addOwnerNames(names2, granteeBody);
    	} else {
    		String[] partiesLines = parties.split("@@");
    		for(int i = 0; i< partiesLines.length; i++){
        		String[] names = {"", "", "", "", "", ""};
    			boolean addToGrantor = false;
    			boolean addToGrantee = false;
    			boolean isCompany = false;
    			Pattern paGrantor = Pattern.compile("\\s*, (MINOR )?(CREDITOR|PLAINTIFF|PETITIONER|INCAPACITATED|APPLICANT).*");
    			Pattern paGrantee = Pattern.compile("\\s*, (DEBTOR|DEFENDANT|RESPONDENT|DECEDENT|GUARDIAN).*");
    			String curLine = partiesLines[i].replaceAll("\\s{2,}", " ").trim().toUpperCase();
    			Matcher maGor = paGrantor.matcher(curLine);
    			Matcher maGee = paGrantee.matcher(curLine);
    			if (partiesLines[i].contains("Petitioner Acting Pro Se")){
    				names = names;
    			}
    			if (maGor.find()){
    				addToGrantor = true;
    				curLine = maGor.replaceAll("");
    			} else if (maGee.find()){
    				addToGrantee = true;
    				curLine = maGee.replaceAll("");
    			}
    			if (addToGrantee || addToGrantor){
    				curLine = curLine.replaceAll(", AS ASSIGNEE", "");
    				curLine = curLine.replaceAll("\\s*\\(ASSIGNEE\\)\\s*", "");
    				curLine = curLine.replaceAll("\\s*\\(ASIG\\)\\s*", "");
    				curLine = curLine.replaceAll("\\*", "");
    				curLine = curLine.replaceAll("(?is)\\bD\\s*/\\s*B\\s*/\\s*A\\s*,", ", ");
    				
    				if (curLine.contains("D/B/A")){
    					curLine = curLine.replaceAll("(?is)(\\w+)\\s+(D/B/A[^,]+),\\s+(\\w+(?:\\s+\\w+)?)", "$3 $1 $2");//BREITENBACH D/B/A DAYLIGHT DONUTS , ELISA MARIE
    					curLine = curLine.replaceAll("(?is)\\bD/B/A\\b", " & ");
    					names = StringFormats.parseNameDesotoRO(curLine);
    				} else if ((NameUtils.isCompany(curLine) && curLine.indexOf(",") == -1)|| curLine.indexOf(",") == -1){
    	    			isCompany = true;
    	    			names[2] = curLine;
    	    		} else {
    	    			curLine = NameCleaner.cleanName(curLine);
    	    			//curLine = curLine.replaceAll(", AS ASSIGNEE", "");
    	    			if (curLine.contains("FORD MOTOR CREDIT COMPANY")){
    	    				curLine = curLine;
    	    			}
    	    			curLine = curLine.replaceAll(nameSuffixString + "\\s*\\.?\\s*,(.*)", ", $2 $1");
    	    			curLine = curLine.replaceFirst("^(\\w{2,}) (\\w{2,})", "$1-$2");
    	    			curLine = curLine.replaceFirst(",\\s*,", ",");
    	    			Vector<String> excludeCompany = new Vector<String>();
    	    		    excludeCompany.add("GRILL");//GRILL , BRADLEY P
    	    		    
    	    			names = StringFormats.parseNameFMWFWML(curLine, excludeCompany);
    	    		}   
    				//suffixes = GenericFunctions.extractAllNamesSufixes(names);
    				if (addToGrantee){
    		    		//GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], isCompany, false, granteeBody);
    					addOwnerNames(names, granteeBody);
    					if (StringUtils.isNotEmpty(names[5])){
    						String[] spouseName = {"", "", ""};
    						spouseName[0] = names[3];
    						spouseName[1] = names[4];
    						spouseName[2] = names[5];
    						addOwnerNames(spouseName, granteeBody);
    					}
    				} else {
    					addOwnerNames(names, grantorBody);
    					if (StringUtils.isNotEmpty(names[5])){
    						String[] spouseName = {"", "", ""};
    						spouseName[0] = names[3];
    						spouseName[1] = names[4];
    						spouseName[2] = names[5];
    						addOwnerNames(spouseName, grantorBody);
    					}
    		    		//GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], isCompany, false, grantorBody);
    				}
    			}
    		}
    	}
    	p.put("Grantee", granteeBody);
    	p.put("Grantor", grantorBody);
    	return p;
    }
    
    public static ResultTable addNamesMOJacksonRO(List<List> body)  {
    	ResultTable parties = new ResultTable();
    	
    	String [] header = new String[3];
    	 
		Map<String,String[]> map = new HashMap<String,String[]>();
		map.put("OwnerFirstName", new String[]{"OwnerFirstName", ""});
		map.put("OwnerMiddleName", new String[]{"OwnerMiddleName", ""});
		map.put("OwnerLastName", new String[]{"OwnerLastName", ""});
		
		header[0] = "OwnerFirstName";
		header[1] = "OwnerMiddleName";
		header[2] = "OwnerLastName";
		
	   	if (!body.isEmpty()){
	   		try {
				parties.setHead(header);
				parties.setBody(body);
				parties.setMap(map);
	   		 }catch(Exception e) {
	   			 e.printStackTrace();
	   		 }
	    }
	   	
		return parties;
    }
    
    public static void grantorsMOJacksonRO(ResultMap m,long searchId) throws Exception{
        
        String tmp = (String) m.get("tmpGrantorsTable");
        tmp = tmp.replaceAll("\\n|\\r","");
        tmp = tmp.replaceAll("\\s{2,}"," ");
        int istart,iend;
        String grantor="";
        tmp = tmp.replaceAll("<TABLE(.*?)>","");
        tmp = tmp.replaceAll("</TABLE>","");
        istart = tmp.indexOf("<TR>");
        iend = tmp.indexOf("</TR>")+"</TR>".length();
        tmp = tmp.replaceAll(tmp.substring(istart,iend),"");
        istart = tmp.lastIndexOf("<TR>");
        iend = tmp.lastIndexOf("</TR>")+"</TR>".length();
        //tmp = tmp.replaceAll(tmp.substring(istart,iend),""); //B 3548
        istart=1;
        iend = 1;
        	while(((istart = tmp.indexOf("<TR><TD>",istart-1))!=-1)&& ((iend = tmp.indexOf("</TD></TR>",iend-1))!=-1)){
        	     if(grantor.equals("")) {
        	         grantor = tmp.substring(istart+"<TR><TD>".length(),iend);
        	         grantor= grantor.trim();
        	  /*       String[] u = StringFormats.parseNameNashville(grantor);
        	         m.put("PropertyIdentificationSet.OwnerFirstName", u[0]);
        	         m.put("PropertyIdentificationSet.OwnerMiddleName", u[1]);
        	         m.put("PropertyIdentificationSet.OwnerLastName", u[2]);
        	         m.put("PropertyIdentificationSet.SpouseFirstName", u[3]);
        	         m.put("PropertyIdentificationSet.SpouseMiddleName", u[4]);
        	         m.put("PropertyIdentificationSet.SpouseLastName", u[5]);
        	    */ }else {
        	         grantor = grantor + " / " + tmp.substring(istart+"<TR><TD>".length(),iend);
        	         grantor= grantor.trim();
        	     }
        	     iend = iend + "</TD></TR>".length();
        	     istart = istart + "<TR><TD>".length();
        	}
        	m.put("SaleDataSet.Grantor",grantor);
    }
    
    public static void granteesMOJacksonRO(ResultMap m,long searchId) throws Exception{
        String tmp = (String) m.get("tmpGranteesTable");
        tmp = tmp.replaceAll("\\n|\\r","");
        tmp = tmp.replaceAll("\\s{2,}"," ");
        int istart,iend;
        String grantee="";
        tmp = tmp.replaceAll("<TABLE(.*?)>","");
        tmp = tmp.replaceAll("</TABLE>","");
        istart = tmp.indexOf("<TR>");
        iend = tmp.indexOf("</TR>")+"</TR>".length();
        tmp = tmp.replaceAll(tmp.substring(istart,iend),"");
        istart = tmp.lastIndexOf("<TR>");
        iend = tmp.lastIndexOf("</TR>")+"</TR>".length();
        //tmp = tmp.replaceAll(tmp.substring(istart,iend),""); //B 3548
        istart=1;
        iend = 1;
        	while(((istart = tmp.indexOf("<TR><TD>",istart-1))!=-1)&& ((iend = tmp.indexOf("</TD></TR>",iend-1))!=-1)){
        	     if(grantee.equals("")) {
        	         grantee = tmp.substring(istart+"<TR><TD>".length(),iend);
        	         grantee= grantee.trim();
        	     }else {
        	    	 String tmp2 = tmp.substring(istart+"<TR><TD>".length(),iend);
        	    	 tmp2 = tmp2.replaceAll("(?is)(?:\\bPAID\\b\\s*,?\\s*)?\\bOTHER\\b\\s+\\bVALUABLE\\b\\s+\\bCONSIDERATION\\b\\s*", "");
        	    	 tmp2 = tmp2.replaceAll("(?is)(?:\\bOF\\b\\s*,?\\s*)?\\bFOR\\b\\s+(?:\\bAND\\b\\s+\\bIN\\b)?\\s+\\bCONSIDERATION\\b\\s*", "");
        	    	 tmp2 = tmp2.replaceAll("(?is)(?:\\bDOLLAR\\b\\s*,?\\s*)?(?:\\bTHE\\b\\s+\\bSUM\\b)?\\s+\\bOF\\b\\s+(?:\\bONE\\b)?", "");
        	    	 tmp2 = tmp2.replaceAll("(?is)(?:\\bOF\\b\\s*,\\s*)?\\bTHE\\s+SAID\\s+PARTIES\\s*", "");
        	    	 tmp2= tmp2.trim();
        	    	 if (StringUtils.isNotEmpty(tmp2)) {
        	    		 grantee = grantee + " / " + tmp2;
            	         grantee= grantee.trim();
        	    	 }
        	     }
        	     iend = iend + "</TD></TR>".length();
        	     istart = istart + "<TR><TD>".length();
        	}
        	m.put("SaleDataSet.Grantee",grantee);
        	//B 3009
        	String block = "";
			  Pattern p = Pattern.compile("\\b(BLOCK)S?\\s*([A-Z\\s&]+)\\b");
			   Matcher ma = p.matcher(grantee);
			   while (ma.find()){
				   block = block + " " + ma.group(2);
			   }
			   block = block.replaceAll("\\s*&\\s*", " ").trim();
			   if (block.length() != 0){
				   block = LegalDescription.cleanValues(block, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			   } 
    }
 /*   
    public static void testJohnson(ResultMap m) throws Exception{
        String tmp = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
        System.out.println(tmp);
    }
    */
    
    public static void deedsKnox(ResultMap m,long searchId) throws Exception{
        String tmp = (String) m.get("tmpDeeds");
        tmp = tmp.replaceAll("\\n|\\r","");
        tmp = tmp.replaceAll("\\s{2,}"," ");
        int istart=0,iend=0;
        istart = tmp.indexOf("<TABLE");
        iend = tmp.indexOf("</TABLE>")+ "</TABLE>".length();
        tmp = tmp.substring(istart,iend);
        tmp = tmp.replaceAll("<TABLE.*?>","");
        tmp = tmp.replaceAll("</TABLE>","");
        istart = tmp.indexOf("<TR");
        iend = tmp.indexOf("</TR>")+ "</TR>".length();
        tmp = tmp.replaceAll(tmp.substring(istart,iend),"");
        istart=-1;
        iend = 0;
        int rows=0;
        while ((istart=tmp.indexOf("<TR",istart+1))!=-1) {
            rows++;
        }
        istart=-1;
        String []tr = new String[rows];
       
        for (int i=0;i<rows;i++) {
            istart = tmp.indexOf("<TR",istart+1);
            iend = tmp.indexOf("</TR>",iend+1) +"</TR>".length();
            tr[i] = tmp.substring(istart,iend);
            tr[i] = tr[i].replaceAll("(?i)<(.*?)>"," ");
            tr[i] = tr[i].replaceAll("\\n|\\r","");
            tr[i] = tr[i].replaceAll("\\s{2,}"," ");
            tr[i] = tr[i].trim();
            if (tr[i].indexOf(" ")!=-1) {
                int is=tr[i].substring(0,tr[i].lastIndexOf(" ")).lastIndexOf(" ");
                if (is!=-1) {
                    tr[i]=tr[i].substring(is+1,tr[i].length());
                }
            }else {
                tr[i]="";
            }
        }
        
        String []h = {"Book","Page","Book_Page"};
        ResultTable rt = new ResultTable();
        rt.setHead(h);
        HashMap hm = new HashMap();
        hm.put("Book_Page", new String[] {"Book_Page", "iw"} );
        hm.put("Book", new String[] {"Book", "iw"} );
        hm.put("Page", new String[] {"Page", "iw"} );
        rt.setMap(hm);
        List b = new ArrayList();
        for (int i=0;i<rows;i++) {
            
            if (tr[i].indexOf(" ")!=-1) {
                String s1 ="";
                String s2 ="";
                
                s1 = tr[i].substring(0,tr[i].indexOf(" "));
                s2= tr[i].substring(tr[i].indexOf(" ")+1,tr[i].length());
                try {
                    Integer.parseInt(s1);
                    Integer.parseInt(s2);
                    String []row = new String[] {s1,s2,s1+"_"+s2};
                    b.add(Arrays.asList(row));
                }catch(Exception e) {
                    
                }
            }
        }
        rt.setBody(b);
        m.put("SaleDataSet",rt);
        
    }
    
    public static void taxWilsonTR (ResultMap m,long searchId) throws Exception{
        String datePaid = (String) m.get("TaxHistorySet.DatePaid");
        String paid = (String) m.get("TaxHistorySet.AmountPaid");
        String baseAmount = (String) m.get("TaxHistorySet.BaseAmount");
        if (baseAmount == null || baseAmount.length() == 0){
        	baseAmount = "0.00";
        }
        if (datePaid == null || datePaid.equals("0000/00/00") || datePaid.trim().equalsIgnoreCase("UnPaid")) {
        	datePaid = "";
        	m.put("TaxHistorySet.DatePaid", datePaid);
        }
        
        if (paid == null || paid.length() == 0 || datePaid.length() == 0){
        	paid = "0.00";
        }
        
        BigDecimal totalDueBD = new BigDecimal(baseAmount).subtract(new BigDecimal(paid));
        String totalDue = totalDueBD.toString();
                 
        m.put("TaxHistorySet.TotalDue",totalDue);
        m.put("TaxHistorySet.CurrentYearDue", totalDue);
        
        String tmpAllDue = sum((String) m.get("tmpAllDue"),searchId);
        String tmpAllPaid = sum((String) m.get("tmpAllPaid"),searchId);
        BigDecimal prevDelinqBD = new BigDecimal(tmpAllDue).subtract(new BigDecimal(tmpAllPaid));
        m.put("TaxHistorySet.PriorDelinquent", prevDelinqBD.toString());
        
        BigDecimal totalDelinqBD = prevDelinqBD;
        Date dueDate = DBManager.getDueDateForCountyAndCommunity(
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue(), 
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().longValue());
        Date now = new Date();
        if (now.after(dueDate)){
        	totalDelinqBD = totalDelinqBD.add(totalDueBD);
        }                
        m.put("TaxHistorySet.DelinquentAmount", totalDelinqBD.toString());        
    }
    
    public static void taxMontgomeryEP( ResultMap m ,long searchId) throws Exception
    {
        String totalDue = (String) m.get( "TaxHistorySet.TotalDue" );
        m.put( "TaxHistorySet.CurrentYearDue", totalDue );
        String delinquent = "0.0";
        
        Date dueDate = DBManager.getDueDateForCity( 
        					InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue(),
        					InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().longValue());
        
        Date current = new Date();

        if( current.after( dueDate ) ){        
            delinquent = totalDue;            
        }
        
        m.put("TaxHistorySet.DelinquentAmount", delinquent);
    }
    
    public static void taxWilliamsonEP (ResultMap m,long searchId) throws Exception{
    	        
        String totalDelinq = "0.00";
        Date dueDate = DBManager.getDueDateForCity(
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue(), 
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().longValue());
        Date now = new Date();
        if (now.after(dueDate)){
            String totalDue = (String) m.get("TaxHistorySet.TotalDue");
            if (totalDue == null || totalDue.length() == 0){
            	totalDue = "0.00";
            }        	
        	totalDelinq = totalDue;
        }                
        m.put("TaxHistorySet.DelinquentAmount", totalDelinq);        
    }    
    
    public static void hamiltonAO(ResultMap m,long searchId) throws Exception{
        
        /*
         * CR 402 
         */
        String PID = (String) m.get( "PropertyIdentificationSet.ParcelID" );
        
        if( PID != null )
        {
            Matcher condoPIDMatcher = Pattern.compile( "([a-zA-Z0-9]*)\\s+([a-zA-Z0-9]*)\\s+([a-zA-Z0-9.]*)\\s+(C[0-9.]*)" ).matcher( PID );
            
            if( condoPIDMatcher.find() )
            {
                m.put( "PropertyIdentificationSet.ParcelIDMap", condoPIDMatcher.group( 1 ) );
                m.put( "PropertyIdentificationSet.ParcelIDGroup", condoPIDMatcher.group( 2 ) );
                m.put( "PropertyIdentificationSet.ParcelIDParcel", condoPIDMatcher.group( 3 ) );
                m.put( "PropertyIdentificationSet.ParcelIDCondo", condoPIDMatcher.group( 4 ) );
            }
            else
            {
                m.put( "PropertyIdentificationSet.ParcelIDCondo", "" );
            }
        }
        
        String tmpLegal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
        tmpLegal = tmpLegal.replaceAll("\\n|\\r","");
        tmpLegal = tmpLegal.replaceAll("\\s{2,}"," ");

        String tmpLegalBPL = (String) m.get("tmpBPLotRow");
        tmpLegalBPL = tmpLegalBPL.replaceAll("\\n|\\r","");
        tmpLegalBPL = tmpLegalBPL.replaceAll("\\s{2,}"," ");
        //book, page, lot,block from table
        String lot1 ="", book1="",page1="",block1="";
       
        int istart, iend;
        //book
        istart = tmpLegalBPL.indexOf("</TD>");
        istart = istart + "</TD>".length();
        istart = tmpLegalBPL.indexOf(">",istart+1) +1;
        iend = tmpLegalBPL.indexOf("<",istart);
        if (istart<iend) {
            book1 = tmpLegalBPL.substring(istart,iend);
        }else {
            book1="";
        }
        
        //page
        istart = tmpLegalBPL.indexOf("<TD>",istart)+"<TD>".length();
        iend = tmpLegalBPL.indexOf("<",istart);
        if (istart<iend) {
            page1 = tmpLegalBPL.substring(istart,iend);
        }else {
            page1="";
        }
        
        m.put("PropertyIdentificationSet.PlatBook",book1);
        m.put("PropertyIdentificationSet.PlatNo",page1);
        //block
        istart = tmpLegalBPL.indexOf("<TD>",istart)+"<TD>".length();
        iend = tmpLegalBPL.indexOf("<",istart);
        if (istart<iend) {
            block1 = tmpLegalBPL.substring(istart,iend);
        }else {
            block1="";
        }
        
        //lot
        istart = tmpLegalBPL.indexOf("<TD>",istart)+"<TD>".length();
        iend = tmpLegalBPL.indexOf("<",istart);
        if (istart<iend) {
            lot1 = tmpLegalBPL.substring(istart,iend);
        }else {
            lot1="";
        }
        
        lot1 = lot1.replaceAll("PT","");
        
        System.out.println("LOT "+lot1);
        System.out.println("BOOK "+book1);
        System.out.println("PAGE "+page1);
        System.out.println("BLOCK "+block1);
        
        ResultTable tmp = (ResultTable) m.get("SaleDataSet");
        if (tmp!=null) {
            String [][]tmpBody = tmp.getBody();
            
                System.out.println(""+tmpBody[tmpBody.length-1][3]);
                System.out.println(""+tmpBody[tmpBody.length-1][4]);
            
            for (int i=0;i<tmpBody.length;i++) {
                String t="";
                t= tmpBody[i][3];
                if (!t.equals("")) {
                    while(t.charAt(0)=='0') {
                        t = t.substring(1,t.length());
                    }
                }
                
                tmpBody[i][3] = t;
                t= tmpBody[i][4];
                if (!t.equals("")) {
    	            while(t.charAt(0)=='0') {
    	                t = t.substring(1,t.length());
    	            }
                }
                tmpBody[i][4] = t;
                
                
            }
            
            List b = new ArrayList();
            for (int i=0;i<tmpBody.length;i++) {
                String r[]= new String[] {"","","","","","",""};
                for (int j=0;j<7;j++)
                {
                    r[j]= tmpBody[i][j];
                }
                b.add(Arrays.asList(r));
            }
            
            String []h = {"descr","Date","null","Book","Page","_date","_"};
            HashMap hm = new HashMap();
            hm.put("Book", new String[] {"Book", "iw"} );
            hm.put("Page", new String[] {"Page", "iw"} );
            ResultTable rt = new ResultTable();
            rt.setHead(h);
            rt.setMap(hm);
            rt.setBody(b);
            m.put("SaleDataSet",rt);
        }
        
        
        
/*        if (!tmpBody[tmpBody.length-1][1].trim().equals("")) {
            String rowToAdd[][] = new String[][] {{"","","","","","",""},}; 
            if (!book1.equals("")) rowToAdd[0][3] = book1;
            if (!page1.equals("")) rowToAdd[0][4] = page1;
            if ((!book1.equals(""))&& (!page1.equals(""))) {
                String [][]tmpBodyNew = new String[tmpBody.length+1][7];
                
                
                
                for (int i=0;i<tmpBody.length;i++)
                    for (int j=0;j<7;j++){
                        tmpBodyNew[i][j]=tmpBody[i][j];
                    }

                
                for (int i=0;i<7;i++) {
                    tmpBodyNew[tmpBody.length][i] = rowToAdd[0][i];
                }
                
                List b = new ArrayList();
                for (int i=0;i<tmpBodyNew.length;i++) {
                    String r[]= new String[] {"","","","","","",""};
                    for (int j=0;j<7;j++)
                    {
                        r[j]= tmpBodyNew[i][j];
                    }
             //       String []row = new String[] {s1,s2,s1+"_"+s2};
                    b.add(Arrays.asList(r));
                    
                }
                
                String []h = {"descr","Date","null","Book","Page","_date","_"};
                HashMap hm = new HashMap();
                hm.put("Book", new String[] {"Book", "iw"} );
                hm.put("Page", new String[] {"Page", "iw"} );
                ResultTable rt = new ResultTable();
                rt.setHead(h);
                rt.setMap(hm);
                rt.setBody(b);
                m.put("SaleDataSet",rt);
            }
        }else {*/
           
        //}

        if (!lot1.trim().equals("")) {
          //  m.put("PropertyIdentificationSet.SubdivisionLotNumber",lot1);
        }else {
//    		Lot Number
    		int lotIndex=-1;
    		
    		String [] lotSeparators = {" LOT "," LT "};
    		int []indexDelims = new int[lotSeparators.length];
    		String lot = "";
    			
    		int minIndex = 0;
    		
    		for (int i=0; i<lotSeparators.length;i++) {
    		    indexDelims[i] =  tmpLegal.indexOf(lotSeparators[i]);
    		    if (indexDelims[i]==-1) indexDelims[i]= tmpLegal.length() +1;
    		}
    		for(int i=0;i<lotSeparators.length;i++) {
    		    if (indexDelims[i] < indexDelims[minIndex]) minIndex= i;
    		}
    		
    		
    		
    		lotIndex = tmpLegal.indexOf(lotSeparators[minIndex]);

    		if (lotIndex !=-1) {
    			    lotIndex = lotIndex + lotSeparators[minIndex].length();
    			    while (tmpLegal.charAt(lotIndex)==' ') lotIndex++;	
    				if (tmpLegal.indexOf(" ",lotIndex)!=-1) {
    				    lot = tmpLegal.substring(lotIndex,tmpLegal.indexOf(" ",lotIndex));
    				}else {
    				    lot = tmpLegal.substring(lotIndex,tmpLegal.length());
    				}
    				lot = lot.replaceAll(",","");
    				lot=lot.trim();
    				if (lotSeparators[minIndex].equals(" LOT ")) {
    				    
    				    int lotIndex2;
    				    int lotEndIndex;
    				    lotIndex2 = tmpLegal.indexOf(" LOT ",lotIndex+1);
    				    while (lotIndex2!=-1) {
    				        lotIndex2 = lotIndex2 + " LOT ".length();
    				        lotEndIndex = tmpLegal.indexOf(" ",lotIndex2+1);
    				        if (lotEndIndex!=-1) {
    				            lot = lot + " "+tmpLegal.substring(lotIndex2,lotEndIndex);
    				            lotIndex2 = tmpLegal.indexOf(" LOT ",lotEndIndex+1);
    				        }else {
    				            lot = lot + " "+tmpLegal.substring(lotIndex2,tmpLegal.length());
    				            lotIndex2 =-1;
    				        }
    				    }
    				}
    				lot = lot.trim();
    			}else {
    			    if (tmpLegal.indexOf(" LOTS ")!=-1) {
    			        lotIndex = tmpLegal.indexOf(" LOTS ") + " LOTS ".length();
    				    
    				    while (tmpLegal.charAt(lotIndex)==' ') lotIndex++;	
    					if (tmpLegal.indexOf(" ",lotIndex)!=-1) {
    					    int lotEndIndex;
    					    lotEndIndex = tmpLegal.indexOf(" ",lotIndex);
    					    if (tmpLegal.indexOf(" ",lotEndIndex+1)!=-1) {
    					        boolean b1=true,b2=true;
    					        String s1="";
    					        
    					        	do
    							    {
    					            	s1= tmpLegal.substring(lotEndIndex+1,tmpLegal.indexOf(" ",lotEndIndex+1));
    					            	b1 = s1.matches("[A-Z&-]{1}");
    					            	b2 = s1.matches("\\d+");
    							        lotEndIndex = tmpLegal.indexOf(" ",lotEndIndex+1);
    							    }while((b1||b2)&&(lotEndIndex!=-1)&&(tmpLegal.indexOf(" ",lotEndIndex+1)!=-1));
    					        	
    							    if ((lotEndIndex!=-1)&&(tmpLegal.indexOf(" ",lotEndIndex+1)!=-1)) {
    							        lot = tmpLegal.substring(lotIndex,lotEndIndex);
    							    }else {
    							        lot = tmpLegal.substring(lotIndex,tmpLegal.length());
    							    }
    					    }else {
    					        lot = tmpLegal.substring(lotIndex,tmpLegal.length());
    					    }
    					    
    					    
    					}else {
    					    lot = tmpLegal.substring(lotIndex,tmpLegal.length());
    					}
    					lot=lot.trim();
    			    }
    			}
    			
    			lot = lot.replaceAll("PT","");
    			//m.put("PropertyIdentificationSet.SubdivisionLotNumber",lot);
            
        }
        if (!block1.trim().equals("")) {
           // m.put("PropertyIdentificationSet.SubdivisionBlock",block1);
        }else {
            String blkSeparators[] = {"BLK","BLOCK"};
    		int blkIndex;
    		String block= "";
    		for (int i=0;i<blkSeparators.length;i++) {
    		    if (tmpLegal.indexOf(blkSeparators[i])!=-1) {
    		        blkIndex = blkSeparators[i].length() + tmpLegal.indexOf(blkSeparators[i]);
    		        while (tmpLegal.charAt(blkIndex)==' ') blkIndex++;
    		        int end = tmpLegal.indexOf(" ",blkIndex);
    		        if (end ==-1) end = tmpLegal.length();
    		        block = tmpLegal.substring(blkIndex, end);
    		        block = block.trim();
    		    }
    		}
    		//m.put("PropertyIdentificationSet.SubdivisionBlock",block);

        }
        
        String platBook = (String) m.get("PropertyIdentificationSet.PlatBook");
        String platNo = (String) m.get("PropertyIdentificationSet.PlatNo");
        if (!platBook.matches("\\d+")) {
            m.put("PropertyIdentificationSet.PlatBook","");
        }
        
        if (!platNo.matches("\\d+")) {
            m.put("PropertyIdentificationSet.PlatNo","");
        }
    }
    
//    public static void hamiltonRO(ResultMap m,long searchId) throws Exception{
//       
//        String tmpDate = (String) m.get("SaleDataSet.RecordedDate");
//        if (tmpDate==null) {
//            tmpDate = "";
//        }else {
//            tmpDate = tmpDate.replaceAll("-JAN-","/01/");
//            tmpDate = tmpDate.replaceAll("-FEB-","/02/");
//            tmpDate = tmpDate.replaceAll("-MAR-","/03/");
//            tmpDate = tmpDate.replaceAll("-APR-","/04/");
//            tmpDate = tmpDate.replaceAll("-MAY-","/05/");
//            tmpDate = tmpDate.replaceAll("-JUN-","/06/");
//            tmpDate = tmpDate.replaceAll("-JUL-","/07/");
//            tmpDate = tmpDate.replaceAll("-AUG-","/08/");
//            tmpDate = tmpDate.replaceAll("-SEP-","/09/");
//            tmpDate = tmpDate.replaceAll("-OCT-","/10/");
//            tmpDate = tmpDate.replaceAll("-NOV-","/11/");
//            tmpDate = tmpDate.replaceAll("-DEC-","/12/");
//            
//            tmpDate = tmpDate.replaceAll("(.*)/(.*)/(.*)","$2"+"/"+"$1"+"/"+"$3");
//            m.put("SaleDataSet.RecordedDate",tmpDate);
//        }        
//    }
    
    public static void setDescrTNHamiltonRO(ResultMap m,long searchId) throws Exception{
    	ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
    	if (pis == null || pis.body.length == 0)
    		return;
    	
    	for (int i=0; i<pis.body.length; i++){
    		String subdiv = pis.body[i][3];
    	    	
	    	if (subdiv == null || subdiv.length() == 0){
	    		String descr = pis.body[i][4];
	    		if (descr != null && descr.length() >0){
	    			m.put("PropertyIdentificationSet.PropertyDescription", descr);
	    			break;
	    		}
	    	} else {
	    		m.put("PropertyIdentificationSet.PropertyDescription", subdiv);
	    		break;
	    	}
    	}    	
    }
    
    public static void taxHamiltonTR(ResultMap m,long searchId) throws Exception{
    	TNHamiltonTR.taxHamiltonTR(m, searchId);
    }
    
    public static void taxHamiltonEP(ResultMap m,long searchId) throws Exception{

    	TNHamiltonEP.taxHamiltonEP(m, searchId);
    }
    
    public static void legalMOClayTR(ResultMap m,long searchId) throws Exception{
        String tmp = (String) m.get("PropertyIdentificationSet.PropertyDescription");
        if (tmp==null) {
            tmp = "";
        }
        String garbage[] = {"LOT(\\d+-\\d+)","[A-Z]\\d+-\\d+","[A-Z]\\d+/\\d+","\\n|\\r","\\s-+","\\s{2,}","(?i)\\dTH PL & REPLAT"," KC"};
        String before[] = {"RANG E","~"," (N)([\\d\\.\']*) "};
        String after[] = {"RANGE"," "," $1 $2"};
        String tokens[] = {" TRACT"," BEG "," ALL"," PT " ," RES "," BLK ", " BLKS "," BLOCKS ", " LOT "," LOTS "," E "," W "," N "," S "," SUB ",
                " COMMON "," ---LOT ","(" , " LTS" , "---Units", " UNITS","---Unit" ,"BLK"," AM ", " LT "," LOTS"};
        tmp = LegalDescription.cleanGarbage(garbage,tmp);
        tmp = LegalDescription.prepare(tmp, before,after);
        
        String subdivName = LegalDescription.extractSubdivisionNameUntilToken(tmp,tokens);
        
        // "&" is also a stopper for extracting subdivizion name, but only when it is not after the first word from legal
        if (subdivName.indexOf("&") != -1 && !subdivName.matches("^\\w+\\s*&.*")){ // PID=16407000200600, bug #554
        	subdivName = subdivName.replaceFirst("(.+)\\s*&.*", "$1");
        }
        
        subdivName = subdivName.replaceAll("(?i)(.*?\\s)(PLAT)$", "$1PL");
        subdivName = subdivName.replaceAll("(.*?)\\s+(\\d+(ST|ND|RD|TH)-)?\\d+(ST|ND|RD|TH)\\s+(RE)?PL(ATS?)?", "$1"); //fix for bug #1101
        
        m.put("PropertyIdentificationSet.SubdivisionName", subdivName);
        
        String lot = LegalDescription.extractLotFromText(tmp, false, true);
        m.put("PropertyIdentificationSet.SubdivisionLotNumber",lot);
        
        
//      block
        String block =LegalDescription.extractBlockFromText(tmp);
		m.put("PropertyIdentificationSet.SubdivisionBlock",block);
		
    }
        
    public static void legalMOClayRO(ResultMap m,long searchId) throws Exception{
        ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
        if (pis == null)
        	return;
	
        MOClayRO.legalMOClayRO(m, searchId);
    }    
    
    public static void test(ResultMap m,long searchId) throws Exception{
      
    }
    
    public static void grantorsMOClayRO(ResultMap m,long searchId) throws Exception{
        String tmp = (String) m.get("tmpGrantorsTable");
        if (tmp==null) {
            tmp = "";
        }else {
	        tmp = tmp.replaceAll("<TD.*?>","");
	        tmp = tmp.replaceAll("</TD>","");
	        tmp = tmp.replaceAll("\\n|\\r","/");
	        tmp = tmp.replaceAll("\\s{2,}"," ");
	        tmp = tmp.trim();
	        tmp = tmp.replaceFirst("/+","");
	        tmp = tmp.replaceAll("/\\s+","/");
	        tmp = tmp.replaceAll("/+$","");
	        tmp = tmp.replaceAll("/+","/");
	        m.put("SaleDataSet.Grantor",tmp.trim());
        }
    }
    
    public static void granteesMOClayRO(ResultMap m,long searchId) throws Exception{
        String tmp = (String) m.get("tmpGranteesTable");
        if (tmp==null) {
            tmp = "";
        }else {
            tmp = tmp.replaceAll("<TD.*?>","");
            tmp = tmp.replaceAll("</TD>","");
            tmp = tmp.replaceAll("\\n|\\r","/");
            tmp = tmp.replaceAll("\\s{2,}"," ");
            tmp = tmp.trim();
            tmp = tmp.replaceFirst("/+","");
            tmp = tmp.replaceAll("/\\s+","/");
            tmp = tmp.replaceAll("/+$","");
            tmp = tmp.replaceAll("/+","/");
            m.put("SaleDataSet.Grantee",tmp.trim());

        }
    }
    		
    
    public static void checkTNCountyROForMERSForMortgage(ResultMap resultMap, long searchId) {
    	String docType = (String) resultMap.get("SaleDataSet.DocumentType");
    	if (docType != null 
    			&& DocumentTypes.checkDocumentType(docType, DocumentTypes.MORTGAGE_INT, null,searchId)){
    		checkTNCountyROForMERSInGranteeSet(resultMap, searchId);
        	checkTNCountyROForMERSInGrantorSet(resultMap, searchId);
    	}
    }
    
    public static void checkTNCountyROForMERSInGrantorSet(ResultMap resultMap, long searchId) {
    	ResultTable rs = (ResultTable) resultMap.get("GrantorSet");
    	String[][] body = rs.getBodyRef();
    	cleanMERS(body);
    }
    
    public static void checkTNCountyROForMERSInGranteeSet(ResultMap resultMap, long searchId) {
    	ResultTable rs = (ResultTable) resultMap.get("GranteeSet");
    	String[][] body = rs.getBodyRef();
    	cleanMERS(body);
    }
    

    public static void cleanMERS(String[][] body){
    	String[][] newBody = new String[body.length][];
    	int i=0;
    	for (String[] strings : body) {
    		String[] newStrings = new String[strings.length];
    		int j=0;
    		for (String string : strings) {
    			string=StringFormats.cleanNameMERS(string);
    			body[i][j]=string;
    			j++;
			}
    		newBody[i]=newStrings;
    		i++;
		}
    }
    
    public static void reparseDocTypeTNDavidsonRO(ResultMap m, long searchId) throws Exception{
    	String docType = (String) m.get("SaleDataSet.DocumentType");
    	String remarks = (String) m.get("tmpRemarks");
    	String marginal = (String) m.get("tmpMarginal");
    	if (StringUtils.isEmpty(docType))
    		return;
    	
    	if (remarks.contains("RERECORD") || marginal.contains("RERECORD")){
    		if (DocumentTypes.isTransferDocType(docType, searchId)){
    			m.put("SaleDataSet.DocumentType", "RE-RECORDED TRANSFER");
    		} else if (DocumentTypes.isMortgageDocType(docType, searchId)){
    			m.put("SaleDataSet.DocumentType", "RE-RECORDED MORTGAGE");
    		} else if (DocumentTypes.isAssignDocType(docType, searchId)){
    			m.put("SaleDataSet.DocumentType", "RE-RECORDED ASSIGNMENT");
    		}
    	}
    }
    
    public static void differentDocNoMOClayRO(ResultMap m,long searchId) throws Exception{
    	String instNo = (String)m.get("SaleDataSet.InstrumentNumber");
    	String docNo = (String)m.get("SaleDataSet.DocumentNumber");
    	if (StringUtils.isEmpty(docNo))
    		return;
    	else
    		if (docNo.equals(instNo))
    		{
    			m.put("SaleDataSet.DocumentNumber","");
    		}
    }
    
    public static void crossrefMOClayRO(ResultMap m,long searchId) throws Exception{
        String tmpRefBy = (String) m.get("tmpRefBy");
        if (tmpRefBy==null) {
            tmpRefBy="";
        }else {
            tmpRefBy = tmpRefBy.replaceAll("\\d+/\\d+/\\d+","");
            tmpRefBy = tmpRefBy.replaceAll("<TR.*?>","");
            tmpRefBy = tmpRefBy.replaceAll("</TR>","");
            tmpRefBy = tmpRefBy.replaceAll("<TD.*?>","");
            tmpRefBy = tmpRefBy.replaceAll("</TD>","");
            tmpRefBy = tmpRefBy.replaceAll("\\n|\\r","/");
            tmpRefBy = tmpRefBy.replaceAll("\\s{2,}"," ");
            tmpRefBy = tmpRefBy.trim();
            tmpRefBy = tmpRefBy.replaceFirst("[/\\s*]+","");
            tmpRefBy = tmpRefBy.replaceAll("/\\s+","/");
            tmpRefBy = tmpRefBy.replaceAll("/+$","/");
            tmpRefBy = tmpRefBy.replaceAll("/+","/");
            tmpRefBy = tmpRefBy.replaceAll("/","\n");
            tmpRefBy = tmpRefBy.replaceAll("Book:\\s*(\\d+|[A-Z])\\s*Page:\\s*(\\d+).*","$1 $2");
            
        }
        
        String tmpRefTo = (String) m.get("tmpRefTo");
        if (tmpRefTo==null) {
            tmpRefTo="";
        }else {
            tmpRefTo = tmpRefTo.replaceAll("\\d+/\\d+/\\d+","");
            tmpRefTo = tmpRefTo.replaceAll("<TR.*?>","");
            tmpRefTo = tmpRefTo.replaceAll("</TR>","");
            tmpRefTo = tmpRefTo.replaceAll("<TD.*?>","");
            tmpRefTo = tmpRefTo.replaceAll("</TD>","");
            tmpRefTo = tmpRefTo.replaceAll("\\n|\\r","/");
            tmpRefTo = tmpRefTo.replaceAll("\\s{2,}"," ");
            tmpRefTo = tmpRefTo.trim();
            tmpRefTo = tmpRefTo.replaceFirst("[/\\s*]+","");
            tmpRefTo = tmpRefTo.replaceAll("/\\s+","/");
            tmpRefTo = tmpRefTo.replaceAll("/+$","/");
            tmpRefTo = tmpRefTo.replaceAll("/+","/");
            tmpRefTo = tmpRefTo.replaceAll("/","\n");
            tmpRefTo = tmpRefTo.replaceAll(".*Book:\\s*(\\d+|[A-Z])\\s*Page:\\s*(\\d+).*","$1 $2");
        }
    
        String tmpCrossRef = tmpRefBy + tmpRefTo;
        int rows=0;
        for (int i =0;i<tmpCrossRef.length();i++) {
            if (tmpCrossRef.charAt(i) =='\n') {
                rows++;
            }
        }
        String tr[] = new String[rows];
        int istart = 0;
        int iend = tmpCrossRef.indexOf("\n");
        for (int i=0;i<rows;i++) {
            tr[i] = tmpCrossRef.substring(istart,iend);
            istart = iend +1;
            iend = tmpCrossRef.indexOf("\n",iend+1);
        }
        
        String []h = {"Book","Page","Book_Page"};
        ResultTable rt = new ResultTable();
        rt.setHead(h);
        HashMap hm = new HashMap();
        
        hm.put("Book", new String[] {"Book", "iw"} );
        hm.put("Page", new String[] {"Page", "iw"} );
        hm.put("Book_Page", new String[] {"Book_Page", "iw"} );
        rt.setMap(hm);
        List b = new ArrayList();
        for (int i=0;i<rows;i++) {
            
            if (tr[i].indexOf(" ")!=-1) {
                String s1 ="";
                String s2 ="";
                
                s1 = tr[i].substring(0,tr[i].indexOf(" "));
                s2= tr[i].substring(tr[i].indexOf(" ")+1,tr[i].length());
                try {
                	if (!s1.matches("[A-Z]"))
                		Integer.parseInt(s1);
                    Integer.parseInt(s2);
                    String []row = new String[] {s1,s2,s1+"_"+s2};
                    b.add(Arrays.asList(row));
                }catch(Exception e) {
                    
                }
            }
        }
        rt.setBody(b);
        m.put("CrossRefSet",rt);
    }

    public static void cleanSubdivDavidsonTR(ResultMap m,long searchId) throws Exception{
        
        TNDavidsonTR.cleanSubdivDavidsonTR(m, searchId);
    }
        
    /**
	 * @author mihaiB 
     * Returns a ResultTable from a List.
     * 
     * @param header The header for the ResultTable
     * @param bodyList The body for the ResultTable
     * @return ResultTable
     */
	
	public static ResultTable createResultTableFromList(String[] header, List<List> bodyList){
		ResultTable newRT = new ResultTable();
		
		Map<String,String[]> map = new HashMap<String,String[]>();
		for (int i = 0; i < header.length; i++){
			map.put(header[i], new String[]{header[i], ""});
		}
		try {
			newRT.setReadWrite();
			newRT.setHead(header);
			newRT.setMap(map);
			newRT.setBody(bodyList);
			newRT.setReadOnly();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return newRT;
	}
	
    public static void checkMortgageAmount(ResultMap m,long searchId) throws Exception{
    	
    	String mortgageAmount = (String) m.get("SaleDataSet.MortgageAmount");    	    	
		String docType = (String) m.get("SaleDataSet.DocumentType");
		
		if (docType != null){
	    	// if RO document is not a mortgage, then force MortgageAmount to "" 
			// because the value of this field is relevant only for mortgages 	
			if (!DocumentTypes.checkDocumentType(docType, DocumentTypes.MORTGAGE_INT,null,searchId)){ 
				if (mortgageAmount != null && mortgageAmount.length() != 0){
					mortgageAmount = "";
				}
			// if RO document is a mortgage but no mortgageAmount info could be parsed from document site, 
			// then set MortgageAmount to zero	
			} else {
		    	if (mortgageAmount == null || mortgageAmount.length() == 0 || mortgageAmount.equals("0")){
		    		mortgageAmount = "0.00";		    		
		    	} else {
		        	//remove spaces and commas from mortgage amount
		    		mortgageAmount = mortgageAmount.replaceAll("\\s", "");
		    		mortgageAmount = mortgageAmount.replaceAll(",", "");
		    	}		
			}									
		}
        
        if( mortgageAmount != null )
        {
            m.put("SaleDataSet.MortgageAmount", mortgageAmount);
        }
    }
    
    // Classifies grantees in Grantee Lander and Grantee Trustee for mortgage documents
    // SaleDataSet.Grantee contains only one Grantee(s) having Trustee type or only Lander type, not mixed 
    public static void setGranteeLanderTrustee1(ResultMap m,long searchId) throws Exception{
    	    	    	
		String docType = (String) m.get("SaleDataSet.DocumentType");
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
    	Search search = currentInstance.getCrtSearchContext();
    	DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, search.getSearchType() == Search.PARENT_SITE_SEARCH);
    	String stateCountySiteType = search.getSa().getStateCounty() + dataSite.getSiteTypeAbrev();
		String crtState = currentInstance.getCurrentState().getStateAbv();
		
		if ("KSJohnsonRO".equals(stateCountySiteType)) {
			KSJohnsonRO.parseNames(m, searchId);
		}
		
		if (docType != null){
			if (DocumentTypes.checkDocumentType(docType, DocumentTypes.MORTGAGE_INT,null,searchId)){
		        String sdsGrantee = (String) m.get("SaleDataSet.Grantee");
		        
		        if (sdsGrantee != null && sdsGrantee.matches("(?s).*\\bTR(U(STEE)?)?\\b.*")){
		        	ResultTable gs = (ResultTable) m.get("GranteeSet");
		        	if (gs == null)
			        	return;
	
		        	if ("TN".equals(crtState)){
		        		gs = StringFormats.reParseGrantorGranteeSet(gs); // B 3846
		        	}
	        }
		        if (sdsGrantee != null && !sdsGrantee.matches("(?s).*\\bTR(U(STEE)?)?\\b.*")){ // it's not trustee => move the info to SDS.GranteeLander
		        	 m.put("SaleDataSet.Grantee", "");
		        	 m.put("SaleDataSet.GranteeLander", sdsGrantee);
		        	 
		        	 ResultTable gs = (ResultTable) m.get("GranteeSet");
		        	 if (gs == null)
			        	return;
				        
				     int len = gs.getLength();
				     if (len != 0){
				    	 String isLanderColName = "isLander";
				    	 int index = -1;
				    	 String[] header = gs.getHead();
				    	 for (int i=0;i<header.length;i++) {
				    		 if (isLanderColName.equals(header[i])) {
				    			 index = i;
				    			 break;
				    		 } 
				    	 }
				    	 if (index!=-1) {
				    		 for (int i=0; i<len; i++){
						        	gs.body[i][index] = "1";
						     }
				    	 } else {
					    		String[] grantee = gs.body[0];
						        int lenGrantee = grantee.length;
						        String[][] newBody = new String[len][lenGrantee+1];
						        for (int i=0; i<len; i++){
						        	System.arraycopy(gs.body[i], 0, newBody[i], 0, lenGrantee);
						        	newBody[i][lenGrantee] = "1";
						        }
						        
						        HashMap map = (HashMap)((ResultMap)gs.getMap()).getMap();
						        map.put(isLanderColName, new String[]{isLanderColName, ""});
						        
						        String[] oldHeader = gs.getHead();
						        String[] newHeader = new String[oldHeader.length+1];
						        System.arraycopy(oldHeader, 0, newHeader, 0, oldHeader.length);
						        newHeader[oldHeader.length] = isLanderColName;
						        
						        gs.setReadWrite();
						        gs.setHead(newHeader);
						        gs.setMap(map);
						        gs.setBody(newBody);
						        if ("TN".equals(crtState)){
						        	gs = StringFormats.reParseGrantorGranteeSet(gs); // B 3846
						        }
						        gs.setReadOnly();
				    	}
				     }
		        }
			}else{//if the Grantor/Granteee  names contain TRUSTEE they sould be cleaned
				ResultTable granteeSet = (ResultTable) m.get("GranteeSet");
				if(granteeSet != null) {
					if ("TN".equals(crtState)){
						StringFormats.reParseGrantorGranteeSet(granteeSet);
					}
				}
				
				ResultTable grantorSet = (ResultTable) m.get("GrantorSet");
				if(grantorSet != null) {
					if ("TN".equals(crtState)){
						StringFormats.reParseGrantorGranteeSet(grantorSet);
					}
				}
				
			}
		}        
    }
    
    
    // Classifies grantees in Grantee Lander and Grantee Trustee for mortgage documents    
    // SaleDataSet.Grantee contains both Grantees Lander and Trustee 
    public static void setGranteeLanderTrustee2(ResultMap m,long searchId, boolean alreadyHasHeader) throws Exception{
    	String docType = (String) m.get("SaleDataSet.DocumentType");
    	String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
    	
		if (docType != null){			
			if (DocumentTypes.checkDocumentType(docType, DocumentTypes.MORTGAGE_INT, null,searchId)){
		        ResultTable gs = (ResultTable) m.get("GranteeSet");
		        if (gs == null)
		        	return;
		        
		        int len = gs.getLength();		        
		        StringBuilder sdsGranteeTR = new StringBuilder();
		        StringBuilder sdsGranteeLander = new StringBuilder();
		        //String [][] newBody = new String[len][];
		        List<String[]> newBody = new ArrayList<String[]>();
		        int gteeLen = 0;
		        if (len != 0){
		        	gteeLen = gs.body[0].length;
		        }
		       
		        for (int i=0; i<len; i++){
		        	String grantee = gs.body[i][0];
		        	if (grantee != null && grantee.length() >0){
		        		
		        		String[] newBodyPart = null;
		        		
		        		if(!alreadyHasHeader) {
		        			newBodyPart = new String[gteeLen + 1];
		        		}else {
		        			newBodyPart = new String[gteeLen];
		        		}
		        		System.arraycopy(gs.body[i], 0, newBodyPart, 0, alreadyHasHeader?gteeLen-1:gteeLen);
			        	if (!grantee.matches(".*(?<!& )\\bTR(U?(STEE))?\\b.*")){ // it's lander			        	 
			        		sdsGranteeLander = sdsGranteeLander.append(" / ").append(grantee);
			        		newBodyPart[alreadyHasHeader?gteeLen-1:gteeLen] = "1";
				        } else { 									   // it's trustee
				        	sdsGranteeTR = sdsGranteeTR.append(" / ").append(grantee);
				        	newBodyPart[alreadyHasHeader?gteeLen-1:gteeLen] = "0";
				        }
			        	newBody.add(newBodyPart);
		        	}		 
		        }
		        if ("TN".equals(crtState)){
		        	gs = StringFormats.reParseGrantorGranteeSet(gs);  //B3846
		        }
		        
		        if (gteeLen != 0){
			        HashMap map = (HashMap)((ResultMap)gs.getMap()).getMap();
			        String isLanderColName = "isLander";
			        map.put(isLanderColName, new String[]{isLanderColName, ""});
			        
			        gs.setReadWrite();
			        
			        String[] oldHeader = gs.getHead();
			        if(!isLanderColName.equals(oldHeader[oldHeader.length-1])) {
				        String[] newHeader = new String[oldHeader.length+1];
				        System.arraycopy(oldHeader, 0, newHeader, 0, oldHeader.length);
				        newHeader[oldHeader.length] = isLanderColName;
				        gs.setHead(newHeader);
			        }else {
			        	gs.setHead(oldHeader);	
			        }
			        
			        gs.setMap(map);
			        gs.setBody(newBody.toArray(new String[0][]));
			        if ("TN".equals(crtState)){
			        	gs = StringFormats.reParseGrantorGranteeSet(gs); // B 3846
			        }
			        gs.setReadOnly();
		        }
		        
	        	m.put("SaleDataSet.GranteeLander", sdsGranteeLander.toString().replaceFirst(" / ", ""));
		        m.put("SaleDataSet.Grantee", sdsGranteeTR.toString().replaceFirst(" / ", ""));
			}
		}
    }
    
    // Classifies grantees in Grantee Lander and Grantee Trustee for mortgage documents    
    // SaleDataSet.Grantee contains both Grantees Lander and Trustee 
    public static void setGranteeLanderTrustee2(ResultMap m,long searchId) throws Exception{
    	boolean hasLender = false;
    	ResultTable gs = (ResultTable) m.get("GranteeSet");
    	
    	if (gs == null)
        	return;
    	
    	for(String str : gs.getHead()) {
    		if("isLander".equals(str)){
    			hasLender = true;
    			break;
    		}
    	}
    	setGranteeLanderTrustee2(m,searchId,hasLender);
    }   
    
    // Consideration Amount info needs to be parsed only for State Tax Lien, Federal Tax Lien and Judgment documents (RO and Orbit)
    public static void checkConsiderationAmount(ResultMap m,long searchId) throws Exception{
    	
    	Object ca = m.get("SaleDataSet.ConsiderationAmount");
    	String considerationAmount;
		if (ca instanceof DeferredTextImpl  ){
    		DeferredTextImpl temp = (DeferredTextImpl) ca;
    		considerationAmount=temp.getData();
    	}else{
    		considerationAmount=(String) ca;
    	}
    	    	    	
		//String docType = (String) m.get("SaleDataSet.DocumentType");
		boolean isParentSite = false;
		try {
			isParentSite = Boolean.parseBoolean((String)m.get("tmp_isParentSite"));
		} catch (Exception e) {}
		
	    if (considerationAmount == null || considerationAmount.length() == 0 || considerationAmount.equals("0")){
	    	considerationAmount = "0.00";		    		
		} else {
		    //remove spaces and commas from consideration amount
		   	considerationAmount = considerationAmount.replaceAll("\\s", "");
		   	considerationAmount = considerationAmount.replaceAll(",", "");
		}		
		        
        if( considerationAmount != null ){
            m.put("SaleDataSet.ConsiderationAmount", considerationAmount);
        }
    }
    
    /*
     * Changes a date that uses "MMM DD YYYY" to MM/DD/YYYY format  
     */
    public static String getStandardDate(String date,long searchId) throws Exception{
        
    	// remove hour:minutes:seconds info
    	date = date.replaceFirst("(?i)\\s+\\d\\d?:\\d\\d?(:\\d\\d?)?\\s*(A|P)M", "");              
    	 
    	date = date.replaceFirst("(?i)JAN","01/");
        date = date.replaceFirst("(?i)FEB","02/");
        date = date.replaceFirst("(?i)MAR","03/");
        date = date.replaceFirst("(?i)APR","04/");
        date = date.replaceFirst("(?i)MAY","05/");
        date = date.replaceFirst("(?i)JUN","06/");
        date = date.replaceFirst("(?i)JUL","07/");
        date = date.replaceFirst("(?i)AUG","08/");
        date = date.replaceFirst("(?i)SEP","09/");
        date = date.replaceFirst("(?i)OCT","10/");
        date = date.replaceFirst("(?i)NOV","11/");
        date = date.replaceFirst("(?i)DEC","12/");
        
        date = date.replaceAll("(.*)/\\s*(\\d\\d?)(?:/|\\s+)(\\d{2,4})","$1"+"/"+"$2"+"/"+"$3");
        
        return date;
    }
    
    public static void checkInstrumentDate(ResultMap m,long searchId) throws Exception{
        
        String tmpDate = (String) m.get("SaleDataSet.InstrumentDate");
        if (tmpDate==null) {
            tmpDate = "";
        }else {
        	if (tmpDate.length() != 0) {
        		tmpDate = getStandardDate(tmpDate,searchId);
        	}
        }    
        m.put("SaleDataSet.InstrumentDate",tmpDate);        
    }    
    
    /* Set PropertyIdentificationSet Plat Book/Page to SaleDataSet Book/Page for plat documents 
     */
    public static void setPlatBookPage(ResultMap m,long searchId) throws Exception{
    	
		String docType = (String) m.get("SaleDataSet.DocumentType");
		
		if (docType != null){ 	
			if (DocumentTypes.checkDocumentType(docType, DocumentTypes.PLAT_INT,null, searchId)){
		    	String instrBook = (String) m.get("SaleDataSet.Book");
		    	String instrPage = (String) m.get("SaleDataSet.Page");
		    	
		    	if (instrBook != null & instrPage != null) {
		    		m.put("PropertyIdentificationSet.PlatBook", instrBook);
		    		m.put("PropertyIdentificationSet.PlatNo", instrPage);
		    	} 
			}									
		}        
    }    
    
    public static void setSearchOrderComment(ResultMap m,long searchId) throws Exception {
    	
    	String dateRequested = (String) m.get("tmpDateRequested");
    	String dateNeeded = (String) m.get("tmpDateNeeded");
    	String antiticipatedClosingDate = (String) m.get("tmpAnticipatedClosingDate");
    	String orderAdditionalInfo = (String) m.get("tmpAdditionalInfo");
    	
    	StringBuilder sb = new StringBuilder();
    	if ((dateRequested != null) && (dateRequested.length() != 0)) {
    		sb.append("Date Requested: ").append(dateRequested).append("\n");
    	}
    	if ((dateNeeded != null) && (dateNeeded.length() != 0)) {
    		sb.append("Date Needed: ").append(dateNeeded).append("\n");
    	}
    	if ((antiticipatedClosingDate != null) && (antiticipatedClosingDate.length() != 0)) {
    		sb.append("Anticipated Closing Date: ").append(antiticipatedClosingDate).append("\n");
    	}
    	if ((orderAdditionalInfo != null) && (orderAdditionalInfo.length() != 0)) {
    		sb.append("Additional Information: ").append(orderAdditionalInfo);
    	}
    	
    	m.put("OtherInformationSet.Comment", sb.toString());
    }
    
    public static void legalShelbyDN(ResultMap m,long searchId) throws Exception {
    	String tmpLegal = (String) m.get("tmpLegal");
    	if (tmpLegal == null || tmpLegal.length() == 0)
    		return;
      	
    	ResultTable pis = new ResultTable();
    	String [] header = {"Parcel", "Subdivision", "Lot"};
    	
    	tmpLegal = tmpLegal.replaceAll("\\n", "");
    	String [] legals = tmpLegal.split("Subdivision Data");
    	String [][] body = new String[legals.length][3];
    	for (int i=0; i<legals.length; i++){
    		String elem = legals[i];
    		String subdiv = "";
    		String lot = "";
    		String parcel = "";
    		if (elem.indexOf("Parcel") != -1){
    			parcel = elem.replaceFirst(".*Parcel</TD><TD>(.*?)</TD>.*", "$1");
    		}
    		if (elem.indexOf("Subdivision") != -1){
    			subdiv = elem.replaceFirst(".*Subdivision</TD><TD>(.*?)</TD>.*", "$1");
    		}
    		if (i == 0){
    			m.put("PropertyIdentificationSet.PropertyDescription", subdiv.trim());
    		}    		
    		if (elem.indexOf("Lot #") != -1){
    			lot = elem.replaceFirst(".*Lot #</TD><TD>(.*?)</TD>.*", "$1");
    		}
    		
    		if ((parcel.length() == 0) && (subdiv.matches(".*/(\\d+|-)+"))){
    			parcel = subdiv.replaceFirst(".*/((?:\\d+|-)+)", "$1");
    			subdiv = subdiv.replaceFirst("(.*)/(?:\\d+|-)+", "$1");
    		}
    		
    		if ((lot.length()== 0) && (subdiv.matches("(?i).*\\bLO?TS?\\b.*"))){
    			lot = LegalDescription.extractLotFromText(subdiv.toUpperCase().replaceAll("\\bLOT", "LT"));
    			lot = lot.replaceAll("(?i)N/A", "").trim();
    		}
    		
    		//subdivision cleanup
    		subdiv = subdiv.replaceFirst("(.+) Sub.*", "$1");
    		subdiv = subdiv.replaceAll("(?i)\\bSEC\\b\\s+(\\d+|[A-Z])", "");
    		subdiv = subdiv.replaceAll("(?i)Various Lots,?", "");    		
    		subdiv = subdiv.replaceAll("(?i)LO?TS?.*,\\s+(?=[A-Z])", "");
    		subdiv = subdiv.replaceAll("(?i)N/A", "");
    		
    		body[i][0] = parcel;
    		body[i][1] = subdiv.trim();
    		body[i][2] = lot;
    	}

    	Map map = new HashMap();
    	map.put("ParcelID", new String[]{"Parcel", ""});
    	map.put("SubdivisionName", new String[]{"Subdivision", ""});
    	map.put("SubdivisionLotNumber", new String[]{"Lot", ""});
    	
    	pis.setHead(header);
    	pis.setBody(body);
    	pis.setMap(map);
    	m.put("PropertyIdentificationSet", pis);
    }   
    
    public static void grantTrAppShelbyDN(ResultMap m,long searchId) throws Exception {
    	String grantor = (String) m.get("SaleDataSet.Grantor");
    	grantor = grantor.replaceAll("\\s*\\b(S|K)? Tr\\b", "").trim();
    	m.put("SaleDataSet.Grantor", grantor);
    	
    	String grantee = (String) m.get("SaleDataSet.Grantee");
    	grantee = grantee.replaceAll("\\s*\\b(S|K)? Tr\\b", "").trim();   
    	m.put("SaleDataSet.Grantee", grantee);    	    	
    }
    
    public static void formatDateProbShelbyDN(ResultMap m,long searchId) throws Exception {
    	String date = (String) m.get("SaleDataSet.RecordedDate");
    	if ((date != null) && (date.length() >0)) {
    		if (!date.matches(".*/.*")){
    			date = date.substring(0, 2)+"/"+date.substring(2, 4)+"/"+date.substring(4);
    			m.put("SaleDataSet.RecordedDate", date);
    		}
    	}
    }
    
    public static String cleanSubdivIntermOR(String subdiv,long searchId){
    	subdiv = subdiv.replaceAll(
				"(.*?)(\\s-|,)?\\s(\\d+\\s*ST|\\d+ND|\\d+RD|\\d+TH|FIRST|SECOND|THIRD|FOURTH|FIFTH|SIXTH|SEVENTH|\\WEIGHTH?|NINTH|" +
				"TENTH|TWELFTH|TWENTIETH|EIGHTY-FIFTH|FIFTY-SEVENTH|FIFTY-NINTH)\\s(REP(L(ATS?)?)?|PL(A?TS?)?|ADD(ITION)?|PHASE|RES(URVEY)?|SUBDIVISION)\\b.*", "$1");
    	subdiv = subdiv.replaceAll("(.*?)((\\s-|,)?\\s|\\b)((REPLAT|(RE)?SURVEY)(\\sOF)?|BL(OC)?K|LOTS?|UNIT)\\b.*", "$1");
    	subdiv = subdiv.replaceAll("(.*?)((\\s-|,)?\\s|\\b)(ANNEX|MINOR SUBDIVISION|RESURVEY|ADDITION|SUBD(IVISION)?)\\b.*$", "$1");
    	subdiv = subdiv.replaceAll("(.*?)((\\s-|,)?\\s|\\b)(SUPPLEMENTAL|CORRECTED|AMENDED)\\sPLAT\\b.*", "$1");
    	subdiv = subdiv.replaceAll("(.*?)\\bOF\\s(LOT|TRACT)\\b.*", "$1");
    	subdiv = subdiv.replaceAll("(.*?)(\\bON I-\\d+|\\(|-K C|\\bNO.\\s\\d+).*", "$1");
    	subdiv = subdiv.replaceAll("(.*?)((\\s-|,)?\\s|\\b)(LOT|TRACT|PH)\\s(II?I?|\\d+).*", "$1");
    	subdiv = subdiv.replaceAll("(.*?)((\\s-|,)?\\s|\\b)PLAT\\s(#\\d+|\\w).*", "$1");
    	subdiv = subdiv.replaceAll("(.*?)\\bOF THE\\s[EWNS]\\s\\d/\\d.*", "$1");
    	
    	return subdiv.trim();
    }
    
    // Cleanup of Jackson Orbit intermediate results
    public static void subdivisionIntermOR(ResultMap m,long searchId) throws Exception {
    	String subdiv = (String) m.get("PropertyIdentificationSet.SubdivisionName");
    	if (subdiv == null || subdiv.length() == 0)
    		return;
    	
    	subdiv = cleanSubdivIntermOR(subdiv,searchId);
		
    	
    	m.put("PropertyIdentificationSet.SubdivisionName", subdiv);    	
    }
    	
    public static void subdivisionClayIntermOR(ResultMap m,long searchId) throws Exception {
    	
    	String subdiv = (String) m.get("PropertyIdentificationSet.SubdivisionName");
    	if (subdiv == null || subdiv.length() == 0)
    		return;
    	
    	subdiv = StringFormats.SubdivisionMOClayOR(subdiv);
    	
    	m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());    	
    }    

	public static Pattern platPatt = Pattern.compile("\\b(?:PB|BK)\\s*(?:\\s|\\.|-)\\s*(\\d+)[,;]?\\s+PGS?\\s*(?:\\s|\\.|-)\\s*(\\d+)"); //bug #627, #664, request 3. from Sept 18 2006
	public static Pattern condoPlatPatt = Pattern.compile("\\bBK\\s*(?:\\s|\\.|-)\\s*(\\d+)[,;]?\\s+PGS?\\s*(?:\\s|\\.|-)\\s*(\\d+)");
    public static void legalJeffersonAO(ResultMap m, long searchId) throws Exception {
    	String legal1 = (String) m.get("tmpLegal1");
    	String legal2 = (String) m.get("tmpLegal2");
    	String legal3 = (String) m.get("tmpLegal3");
    	Boolean platFound = false;
    	Matcher mt = null;
    	
    	String legal = "";
    	
    	if (legal1 != null && legal1.length() > 0){
    		if (legal2 != null && legal2.length() > 0){
    			if (legal3 != null && legal3.length() > 0){
    				legal = legal1 + " " + legal2 + " " + legal3;
    				mt = platPatt.matcher(legal3);
    				platFound = mt.find();
    			} else {
    				legal = legal1 + " " + legal2;
    			}
    			if (!platFound){
    				mt = platPatt.matcher(legal2);
    				platFound = mt.find(); 
    			}
    		} else {
    			legal = legal1;
    		}
			if (!platFound){
				mt = platPatt.matcher(legal1);
				platFound = mt.find();
			}    		
			legal = legal.replaceAll("\\s{2,}", " ").trim();
    		m.put("PropertyIdentificationSet.Subdivision", legal);
    		m.put("PropertyIdentificationSet.PropertyDescription", legal);
    	}
    	
    	// check if the property is a condominium => if yes, try to extract Master Deed B&P from legal description
    	boolean isCondo = false;
    	String tmp = (String)m.get("tmpPropertyClass");
    	if (tmp != null && tmp.length() > 0 && tmp.matches("(?i).*\\b(Condo|Apart(ament)?s?)\\b.*")) {
    		isCondo = true;
    	} else {
    		tmp = (String)m.get("tmpBuildingType");
    		if (tmp != null && tmp.length() > 0 && tmp.matches("(?i).*\\b(Condo|Apart(ament)?s?|General Office)\\b.*")) {
        		isCondo = true;
        	} else {
        		tmp = (String)m.get("tmpBldgType");
        		if (tmp != null && tmp.length() > 0 && tmp.matches("(?i).*\\bCondominium\\b.*")) {
            		isCondo = true;
            	} else if (legal.matches(".*\\b(MASTER DEED|CONDO?S?|LOFTS|APT|AOB|UNIT|LOFTS)\\b.*")){
            		isCondo = true;
            	}
        	}
    	}
    	if (isCondo){
    		mt = condoPlatPatt.matcher(legal);
    		if (mt.find()){
    			m.put("PropertyIdentificationSet.CondominiumPlatBook", mt.group(1));
    			m.put("PropertyIdentificationSet.CondominiumPlatPage", mt.group(2));
    		}
    	} else { 
    		if (platFound){    	
	    		m.put("PropertyIdentificationSet.PlatBook", mt.group(1));
	    		//m.put("PropertyIdentificationSet.PlatNo", StringFormats.ReplaceIntervalWithEnumeration(mt.group(2)));
	    		m.put("PropertyIdentificationSet.PlatNo", mt.group(2)); //request 3. from Sept 18 2006
    		}
    	}
    	
    	String city = (String) m.get("PropertyIdentificationSet.City");
    	if (city != null) {
    		if (city.contains("County") || city.contains("District")) { //B3904
    			city = city.replaceAll(".*", "");
    			m.put("PropertyIdentificationSet.City", city);
    		}
    	}
    }
    
    public static void transfersJeffersonAO(ResultMap m,long searchId) throws Exception {
    	// replaces all "-" elements from SaleDataSet with ""
		ResultTable sds = (ResultTable) m.get("SaleDataSet");
        if (sds != null){        
        	int length = sds.getLength();
        	String body[][] = sds.body;
            for (int i=0; i<length; i++){
            	for (int j=0; j<5; j++){
            		body[i][j] = body[i][j].replace("-", "");
            	}
            }
            
            // remove empty lines from SaleDataSet
            // first check which lines are empty and count them
            Boolean isEmpty;
            int cnt = 0;
            for (int i=0; i<length; i++){
            	isEmpty = true;
	            for (int j=0; j<5; j++){
	        		isEmpty = isEmpty && (body[i][j].length() == 0);
	        	}
	            if (isEmpty){
	            	body[i] = null;
	            	cnt ++;
	            }
            }
            if (cnt >0) { // if there are empty lines
            	if (length > cnt){ // if not all lines are empty
	            	String newBody[][] = new String[length-cnt][5];
	            	int j=0;
	                for (int i=0; i<length; i++){
	                	if (body[i] != null){
	                		System.arraycopy(body[i], 0, newBody[j], 0, 5);
	                		j ++;
	                	}
	                }
	                sds.body = newBody;
	                m.put("SaleDataSet", sds);
            	} else { // if all lines are empty, remove SaleDataSet from ParsedResponse
            		m.remove("SaleDataSet");
            		return;
            	}
            }
            
            m.put("SaleDataSet", sds);
        }        
    }    
    
    protected static String cleanCondoSubdivJeffersonRO(String s,long searchId){
    	s = s.replaceFirst("\\d+(ST|ND|RD|TH)\\s+AMEND\\s+.*MASTER\\s+DEED\\s+DB\\s+\\d+\\s+P\\s+\\d+\\s*", "");    	
    	s = s.replaceFirst("(?i)^The\\b\\s*", "");    	
    	return s;
    }
    
//  extract the subdivision name, lot, block etc from a Jefferson RO property description string that contains one subdivision and one address
    protected static String[] parseSubdivFromLegalJeffersonRO(String legal, ResultMap m, boolean isCondoMDDocType,long searchId)  throws Exception{
    	String a[]=legal.split("///");  
    	legal = legal.replace("///", " ");
    	String pattern;
    	//returns an array with subdivision name, lot, block, section, unit, phase, street name and street number
    	String ret[] = new String[9];    	
    	String subdiv = null;
    	
    	if (isCondoMDDocType){
    		subdiv = legal.replaceFirst("(?i)(.+?)\\b(CONDO?S?|LOFTS|SHEET|PHASE|BLDG|UNIT|TOWN[ ]*HOMES)\\b.*", "$1");
    		if (!subdiv.equals(legal)){
    			subdiv = cleanCondoSubdivJeffersonRO(subdiv,searchId);
    			ret[0] = subdiv;
    			ret[8] = subdiv;
    		}
    	}
    	// parse subdivision name, lot, block, section, unit, phase
    	if (legal.matches("(?i).*\\b(LOTS?|UNIT)\\b.*")){
    		        	
        	// parse lot
    		String lot = "";
    		pattern = "(?i).*\\bLOTS?\\s+(\\d+(\\s*(&|-)\\s*\\d+)?(\\s*\\d+)?).*";
    		if (legal.matches(pattern)) {
	    		lot = legal.replaceAll(pattern, "$1").replaceAll("\\s*&\\s*", " ");
    		} else {
    			pattern = "(?i).*\\bLOT\\s+\\\"(\\w)\\\".*";
    			if (legal.matches(pattern)){
    				lot = legal.replaceAll(pattern, "$1");
    			}
	    	}
    		ret[1] = lot;	
    		
    		// parse block
    		pattern = "(?i).*\\bBLK\\s+(\\d+|\\\"\\w\\\").*";
    		if (legal.matches(pattern)){
    			ret[2] = legal.replaceAll(pattern, "$1").replaceAll("\\\"", "");    		
    		}
    		
    		// parse section
    		pattern = "(?i).*\\bSEC\\s+(\\d+-\\w-\\d+|\\d+-\\w\\s*&\\s*\\d+-\\w|\\d+-\\w|\\d+|\\\"\\w-\\w\\\"|\\\"\\w\\\").*";
    		if (legal.matches(pattern)){
    			ret[3] = legal.replaceAll(pattern, "$1").replaceAll("\\\"", "");
    		}
    		    		
    		// parse unit
    		String unit = "";
    		pattern = "(?i).*\\bUNIT\\s+(\\d?-?\\w-\\d|\\d+-\\w|\\d+-\\d+|\\d+).*";
    		if (legal.matches(pattern)){
    			unit = legal.replaceAll(pattern, "$1");
    		}
    		ret[4] = unit;
    		
    		// parse phase
    		pattern = "(?i).*\\bPHASE\\s+(\\d+).*";
    		if (legal.matches(pattern)){
    			ret[5] = legal.replaceAll(pattern, "$1");
    		} 
    		
    		// parse subdivision name / condominium name, if not already parsed because isCondoMDDocType
    		if (ret[0] == null || ret[0].length() == 0){
	    		subdiv = null;
	    		boolean isCondo = false;
	    		if (lot.length() > 0 || unit.length() > 0){ // fix for bug #625
		    		pattern = "(?i)(.+?)\\s*\\bSUB\\b.*";
		        	if (legal.matches(pattern)){
		        		for (int i=0; i<a.length; i++){
		        			if(a[i].matches(pattern)){
		        				subdiv = legal.replaceAll(pattern, "$1"); // B4048
		        				break;
		        			}
		        		}
		        	} else {
		        		pattern = "(?i)(.*?)\\b(CONDO?S?|UNIT|TOWN[ ]*HOMES|APT|AOB|LOFTS|MASTER DEED)\\b.*";
		        		if (legal.matches(pattern)){
		        			isCondo = true;
		        			subdiv = legal.replaceAll(pattern, "$1");
		        		} else {
			        		pattern = "(?i)(.*?)\\s*\\b(REV|SEC|LOTS?|BLK|BLDG|PRT|PHASE)\\b.*";
			        		if (legal.matches(pattern)){
			        			subdiv = legal.replaceAll(pattern, "$1");
			        		}
		        		}
		        	}
		        		        			        			        		       
		        	if (subdiv != null) {
		        		subdiv = subdiv.replaceFirst("(?i).*\\bBEING\\b\\s*(.*)", "$1");
		        		subdiv = subdiv.replaceFirst("(?i).*\\b(?:LOTS?|BLK|SEC|UNIT|PHASE)\\s+(?:(?:(?:\\d+|\\w)?\\s*(?:&|-|\\s)\\s*)+|\\\"?\\w(?:-\\w)?\\\"?\\s*)(.*)", "$1");
		        		//subdiv = subdiv.replaceFirst("(?i)^The\\b\\s*", ""); //fix for request# 4 from Sept 18, 2006- remove 'The' from Jefferson RO subdiv  
		        		subdiv = subdiv.replaceFirst("\\d\\w{6,}\\s+", ""); // needed for B&P L 00077 0679 or B&P L 00891 0056
		        		subdiv = cleanCondoSubdivJeffersonRO(subdiv,searchId);
		        	}
		        	
		        	ret[0] = subdiv;
		        	if (isCondo){
		        		ret[8] = subdiv;
		        	}
	    		}	        	
    		}
    	// parse street name and street number - bug #745 -> address parsing not needed anymore, since it can be a mailing address (bug #745 comment #3) - fix for bug #972 
    	} else {    	 // subdivision can contain an address, e.g. DN 2001 127778, in which case the address string will not be parsed also as street name/no 
	    	// first typo correction
	    	legal = legal.replaceAll("\\bTA;OTJA\\b", "TALITHA"); //B&P D 08379 0243
//	    	String address = "";
//	    	pattern = "(\\d\\w{6,}\\s+)?((\\d+(-\\w+)?\\s+)?[SWEN]?\\s*([^']*?)\\s+(CT|CIR|CR|AVE|DR|ST|RD|TRACE|TR|LN|WAY|HGWY|BLVD)\\b(\\s+(APT?\\.?\\s+)?\\d{1,4}(\\s|$))?).*";
//	    	if (legal.matches(pattern)){
//	    		address = legal.replaceAll(pattern, "$2");
//	    		String adr[] = StringFormats.parseAddress(address);
//	    		ret[6] = adr[1];
//	    		ret[7] = adr[0];
//	    	}
    	}
    	
       	// parse cross ref deed book & page
    	pattern = "\\bDB\\s+(\\d+)\\s+P\\s+(\\d+)";
    	if (legal.matches(".*"+pattern+".*")){    		
    		Pattern p = Pattern.compile(pattern);
    		Matcher matcher = p.matcher(legal);
    		int count = 0;
    		while(matcher.find())	
    			count++;
    		matcher.reset();
    		
    		ResultTable cr = (ResultTable) m.get("CrossRefSet");
            if (cr == null){
            	cr = new ResultTable();            	
            	String [][] body = new String[count][3];
            	
            	for (int i=0; i<count; i++){
            		matcher.find();
            		body[i][0] = "D";
            		body[i][1] = matcher.group(1);
            		body[i][2] = matcher.group(2);
            	}
            	
            	String [] header = {"Book_Page_Type", "Book", "Page"};
            	Map map = new HashMap();
            	map.put("Book_Page_Type", new String[]{"Book_Page_Type", ""});
            	map.put("Book", new String[]{"Book", ""});
            	map.put("Page", new String[]{"Page", ""});
            	
            	cr.setHead(header);
            	cr.setBody(body);
            	cr.setMap(map);
            	
            } else {            
            	int length = cr.getLength();
            	String body[][] = cr.body;
                String newBody[][]=new String[length+count][3];
                for (int i=0; i<length; i++){
                	System.arraycopy(body[i], 0, newBody[i], 0, 3);
                }
                for (int i=0; i<count; i++){
                	matcher.find();
                	newBody[length+i][0] = "D";
                	newBody[length+i][1] = matcher.group(1);
                	newBody[length+i][2] = matcher.group(2);
            	}
                
                cr.body = newBody;
            }
            
            m.put("CrossRefSet", cr); 
    	}   
    	return ret;
    }
    
    public static void legalJeffersonRO(ResultMap m,long searchId) throws Exception { 
    	String legal = (String) m.get("tmpLegal");
    	String docType = (String)m.get("SaleDataSet.DocumentType");
    	String grantee = (String) m.get("SaleDataSet.Grantee");

    	// adjust EST document type, according to grantee or legal description - KY request
    	String newDocType = null;
    	if ("EST".equals(docType)){    		
    		if (grantee != null && grantee.length() >0){
    			if (grantee.contains("LOUISVILLE GAS & ELECTRIC CO")) {
    				newDocType = "Louisville Gas Electric";
    			} else if (grantee.contains("LOUISVILLE WATER CO")){
    				newDocType = "Louisville Water Company";
    			} else if (grantee.matches(".*\\bMETROPOLITAN SEWER D(,\\s*)?ISTRICT\\b.*")){
    				newDocType = "Metropolitan Sewer District Esmt";
    			}
    			if (legal != null && (legal.contains("ROADWAY ESMT") || legal.contains("DEDICATION FOR PUBLIC USE"))){
    				newDocType = "Roadway Esmt";
    			}
    		}
    	}
    	
    	//adjust AG document type, according to grantee or legal description
    	if ("AG".equals(docType)){    		
    		if (grantee != null && grantee.length() >0 && legal != null && legal.length() > 0){
    			if (grantee.matches(".*\\bLOUISVILLE & JEFFERSON COUNTY, METROPOLITAN SEWER D(,\\s*)?ISTRICT\\b.*") && 
    					legal.matches(".*\\bEXTENSION OF (STORMWATER )?BOUNDAR(Y|IES) AGRE\\b.*")) {
    				newDocType = "MSD ANNEX";
    			}
    		}
    	}
    	
    	// adjust DED document type according to legal descr, if necessary - fix for bug #1002
    	if ("DED".equals(docType) && legal != null){	    	
	    	if (legal.contains("AFFIDAVIT")){
	    		newDocType = "AFFIDAVIT";
	    	} else if (legal.matches("ESMT\\b.*")){
	    		newDocType = "ESMT";
	    	} else if (legal.contains("POWER OF ATTORNEY")){
	    		newDocType = "POWER OF ATTORNEY";
	    	} else if (legal.matches(".*\\bMODIFICATION( AGRE)? MB \\d+.*")){
	    		newDocType = "Modification Mtg Agreement";
	    	} else if (legal.contains("RESTRICTION") || legal.matches(".*\\bAMEND(MENT)? DB \\d+.*")){
	    		newDocType = "RESTRICTION";
	    	}
    	}
    	    	
    	if (newDocType != null){
    		m.put("SaleDataSet.DocumentType", newDocType);
    	}

    	if(legal == null || legal.length() == 0)
    		return;
    	    	
    	boolean  isCondoMDDocType = false;    	
    	if (docType != null && docType.length() > 0){
    		boolean isParentSite = false;
    		try {
    			isParentSite = Boolean.parseBoolean((String)m.get("tmp_isParentSite"));
    		} catch (Exception e) {}
    		isCondoMDDocType = DocumentTypes.isCondominiumDocType(docType,searchId, isParentSite) || DocumentTypes.isMasterDeedDocType(docType,searchId);
    	}
    	
    	//legal descr cleanup
    	legal = legal.replaceAll("(?i)\\bSSUB\\b", "SUB");
    	legal = legal.replaceAll("(\\d) THRU (\\d)", "$1-$2");
    	
    	String a[];
    	// legal description lines were separated with "///"
    	// some docs have more than one legal descr, each legal starts with "index- ", in this case each legal is parsed separately
    	// in some cases property has more lines that start with "index-", but the first line does not=> add "index-" to the first line (for ex. B&P L 00077 0890)
    	if(legal.matches("(\\w+)(///\\d+-\\s.*)+.*")){
    		legal = legal.replaceFirst("(\\w+)", "0- $1"); 
    	}
    	if(legal.matches("((^|///)\\d+-\\s.*)+.*")){
    		a = legal.split("///\\d+-\\s");
    		int len = a.length;
    		a[0] = a[0].replaceFirst("^\\d+-\\s+", "");
    		a[len-1] = a[len-1].replaceFirst("///The maximum .* has been reached", "");
    		
    		// create PropertyInformationSet table
    		ResultTable pis = new ResultTable();
        	String [] header = {"Subdivision", "SubdivisionName", "Lot", "Block", "Section", "Unit", "Phase", "StreetName", "StreetNo", "SubdivisionCond"};
        	String [][] body = new String[len][header.length];
        	
    		for (int i=0; i<len; i++){
    			String ret[] = parseSubdivFromLegalJeffersonRO(a[i], m, isCondoMDDocType,searchId);    
    			body[i][0] = a[i].replaceAll("///", " ");
    			if (ret[0] == null) ret[0] = "";
    			body[i][1] = ret[0];
    			if (ret[1] == null) ret[1] = "";
    	    	body[i][2] = ret[1];
    	    	if (ret[2] == null) ret[2] = "";
    	    	body[i][3] = ret[2];
    	    	if (ret[3] == null) ret[3] = "";
    	    	body[i][4] = ret[3];
    	    	if (ret[4] == null) ret[4] = "";	    	
    	    	body[i][5] = ret[4];
    	    	if (ret[5] == null) ret[5] = "";	    	
    	    	body[i][6] = ret[5];
    	    	if (ret[6] == null) ret[6] = "";	    	
    	    	body[i][7] = ret[6];
    	    	if (ret[7] == null) ret[7] = "";	    	
    	    	body[i][8] = ret[7];
    	    	if (ret[8] == null) ret[8] = "";	    	
    	    	body[i][9] = ret[8];
    		}
    		
    		Map map = new HashMap();
        	map.put("Subdivision", new String[]{"Subdivision", ""});
        	map.put("SubdivisionName", new String[]{"SubdivisionName", ""});
        	map.put("SubdivisionLotNumber", new String[]{"Lot", ""});
        	map.put("SubdivisionBlock", new String[]{"Block", ""});
        	map.put("SubdivisionSection", new String[]{"Section", ""});
        	map.put("SubdivisionUnit", new String[]{"Unit", ""});
        	map.put("SubdivisionPhase", new String[]{"Phase", ""}); 
        	map.put("StreetName", new String[]{"StreetName", ""}); 
        	map.put("StreetNo", new String[]{"StreetNo", ""});
        	map.put("SubdivisionCond", new String[]{"SubdivisionCond", ""});        	
        	        	
        	pis.setHead(header);
        	pis.setBody(body);
        	pis.setMap(map);
        	m.put("PropertyIdentificationSet", pis);
        
        // just one legal descr in the document => no need to have a PIS table	
    	} else {
    	
	    	String ret[] = parseSubdivFromLegalJeffersonRO(legal, m, isCondoMDDocType,searchId);
	    	m.put("PropertyIdentificationSet.Subdivision", legal.replaceAll("///", " "));
	    	if (ret[0] != null && ret[0].length() > 0)
	    		m.put("PropertyIdentificationSet.SubdivisionName", ret[0]);
	    	if (ret[1] != null && ret[1].length() > 0)
	    		m.put("PropertyIdentificationSet.SubdivisionLotNumber", ret[1]);
	    	if (ret[2] != null && ret[2].length() > 0)
	    		m.put("PropertyIdentificationSet.SubdivisionBlock", ret[2]);
	    	if (ret[3] != null && ret[3].length() > 0)
	    		m.put("PropertyIdentificationSet.SubdivisionSection", ret[3]);
	    	if (ret[4] != null && ret[4].length() > 0)	    	
	    		m.put("PropertyIdentificationSet.SubdivisionUnit", ret[4]);
	    	if (ret[5] != null && ret[5].length() > 0)	    	
	    		m.put("PropertyIdentificationSet.SubdivisionPhase", ret[5]);
	    	if (ret[6] != null && ret[6].length() > 0)	    	
	    		m.put("PropertyIdentificationSet.StreetName", ret[6]);
	    	if (ret[7] != null && ret[7].length() > 0)	    	
	    		m.put("PropertyIdentificationSet.StreetNo", ret[7]);
	    	if (ret[8] != null && ret[8].length() > 0)
	    		m.put("PropertyIdentificationSet.SubdivisionCond", ret[8]);
    	} 
    }
    
    public static void crossRefJeffersonRO(ResultMap m,long searchId) throws Exception { 
    	String references = (String) m.get("SaleDataSet.CrossRefInstrument");
    	
    	if(!StringUtils.isEmpty(references)) {
    		
    		try {
	    	   ResultTable cr = new ResultTable();    
			   List<List> bodyCR = new ArrayList<List>();       
			   Pattern p = Pattern.compile("\\w+");
			   Matcher ma = p.matcher(references);
			     
			   while (ma.find()){
				   List<String> ln = new ArrayList<String>();
				   ln.add(ma.group());
				   bodyCR.add(ln);
			   }
			   
			   if (!bodyCR.isEmpty()){	   
				   String [] header = {"InstrumentNumber"};
			       Map<String,String[]> map = new HashMap<String,String[]>();
			       map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
			       	
			       cr.setHead(header);
			       cr.setBody(bodyCR);
			       cr.setMap(map);
			       m.put("CrossRefSet", cr);
			   }
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    public static void legalFranklinAO(ResultMap m,long searchId) throws Exception {
    	OHFranklinAO.legalFranklinAO(m, searchId);
    }    
    
    public static void parseNameOHFranklinAO(ResultMap m,long searchId) throws Exception 
    {
    	OHFranklinAO.parseNameOHFranklinAO(m, searchId);
    }
    
    public static void fixKSJohnsonCOMiddleProblem(ResultMap m,long searchId) throws Exception {
    	String ownerFirstName = (String)m.get("PropertyIdentificationSet.OwnerFirstName");
    	String ownerMiddleName = (String)m.get("PropertyIdentificationSet.OwnerMiddleName");
    	
    	if("".equals(ownerMiddleName)){
    		ownerFirstName = ownerFirstName.replaceAll("\\s+"," ").trim();
    		int idx = ownerFirstName.indexOf(" ");
    		if(idx != -1){
    			ownerMiddleName = ownerFirstName.substring(idx + 1);
    			ownerFirstName = ownerFirstName.substring(0, idx);
    			m.put("PropertyIdentificationSet.OwnerFirstName",  ownerFirstName);
    			m.put("PropertyIdentificationSet.OwnerMiddleName", ownerMiddleName);
    		}
    	}
    }

    protected static Pattern sectionTNSumnerAO = Pattern.compile(".*\\bSEC(?:TION)?\\s+(\\w+).*");
    protected static Pattern phaseTNSumnerAO = Pattern.compile(".*\\bPH(?:ASES?)?\\s*(\\d+).*");
    // Extracts subdivision name, section and phase from TNSumner AO legal descriptions
    public static void legalTNSumnerAO(ResultMap m,long searchId) throws Exception {
    	String legal = (String)m.get("tmpSubdivision");
    	
    	if (legal == null || legal.length() == 0)
    		return;
    	
    	legal = legal.replaceAll("\\b\\.", "");

    	// extract subdivision name
    	String subdiv = legal.replaceFirst("(.*?),?\\s+((RE-?)?SUB(D(IV(ISION)?)?)?|S/D|SEC|,|PH(ASE)?|NO)(\\b|\\d).*", "$1");
        subdiv = subdiv.replaceFirst("(.*?\\s+EST\\b).*", "$1"); //PHILIP DRIVE 1005
    	m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
    	
    	// replace romans numbers with arabics
    	String[] exceptionTokens = {"I", "M", "C", "L", "D"};
    	legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);
    	
    	// extract section
    	String section = null;
    	Matcher match = sectionTNSumnerAO.matcher(legal); 
    	if (match.find()){
    		section = match.group(1);
    	}
    	if (section != null) {
    		m.put("PropertyIdentificationSet.SubdivisionSection", section);
    	}
    	
    	// extract phase
    	String phase = null;
    	match = phaseTNSumnerAO.matcher(legal); 
    	if (match.find()){
    		phase = match.group(1);
    	}
    	if (phase != null) {
    		m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
    	}
    }    
    
    public static void setPropertyDescriptionTNShelbyRO(ResultMap m,long searchId) throws Exception {
    	
    	ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
    	if (pis == null) return;    	
    	if (pis.body.length <1) return;
    	
    	m.put("PropertyIdentificationSet.PropertyDescription", pis.body[0][2]);
    }
    
    public static void setPropertyDescription(ResultMap m,long searchId) throws Exception {
    	
    	String subdiv = (String)m.get("tmpSubdivision");
    	
    	if (subdiv == null || subdiv.length() == 0){
    		String descr = (String)m.get("tmpDescription");
    		if (descr != null && descr.length() >0){
    			m.put("PropertyIdentificationSet.PropertyDescription", descr);
    					
    		}
    	} else {
    		m.put("PropertyIdentificationSet.PropertyDescription", subdiv);
    	}
    }
    
    public static void setReceiptsKiva(ResultMap m,long searchId) throws Exception {
    	
    	ResultTable rebp = (ResultTable) m.get("tmpREBP");
    	ResultTable sap = (ResultTable) m.get("tmpSAP");
    	
    	if (rebp == null && sap == null)
    		return;
    	    	
    	int rebpLen = 0;
    	int sapLen = 0;
    	
    	String[][] rebpBody = null;
    	String[][] sapBody = null;
    	
    	int rebpCount = 0;
    	int sapCount = 0;
    	
    	if (rebp != null){
    		rebpCount = rebp.getLength();
    		rebpBody = rebp.getBody();
    		for (int i=0; i<rebpCount; i++){
    			if (rebpBody[i][0].indexOf("Date") == -1){
    				rebpLen ++;
    			} else {
    				rebpBody[i] = null;
    			}
    		}
    	}
    	if (sap != null){
    		sapCount = sap.getLength();
    		sapBody = sap.getBody();
    		for (int i=0; i<sapCount; i++){
    			if (sapBody[i][0].indexOf("Date") == -1){
    				sapLen ++;
    			} else {
    				sapBody[i] = null;
    			}
    		}
    	}
    	
    	int len = rebpLen+sapLen;
    	if (len == 0)
    		return;
    	
    	String [][] body = new String[len][3];
    	
    	int k = 0;
    	if (rebpLen > 0){
    		for (int i = 0; i<rebpCount; i++){
    			if (rebpBody[i] != null){
    				body[k][0] = StringFormats.MonthNo(rebpBody[i][0]);
    				body[k][1] = rebpBody[i][1];
    				body[k][2] = rebpBody[i][3].replaceAll("[()$,]", "");
    				k ++;      
    			}
    		}
    	}
    	if (sapLen > 0){
    		for (int i=0; i<sapCount; i++){
    			if (sapBody[i] != null){
    				body[k][0] = StringFormats.MonthNo(sapBody[i][0]);
    				body[k][1] = sapBody[i][2];
    				body[k][2] = sapBody[i][4].replaceAll("[()$,]", "");
    				k ++;
    			}
    		}
    	}
    	
    		
        ResultTable ths =  new ResultTable();
    	String [] header = {"Payment Date", "Receipt Number", "Payment Amount"};
    	Map map = new HashMap();
    	map.put("ReceiptNumber", new String[]{"Receipt Number", ""});
    	map.put("ReceiptDate", new String[]{"Payment Date", ""});
    	map.put("ReceiptAmount", new String[]{"Payment Amount", ""});
    	
    	ths.setHead(header);
    	ths.setMap(map);
    	ths.setBody(body);
    	ths.setReadOnly();
    	
    	m.put("TaxHistorySet", ths);
    }   
    
    
    public static void parseTaxesMOClayYA(ResultMap m, long searchId){
    	ResultTable reb = (ResultTable) m.get("tmpREB");
    	
    	if (reb == null)
    		return;
    	
    	StringBuilder delinquentAmount = new StringBuilder();
    	CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
    	Date payDate = HashCountyToIndex.getPayDate(currentInstance.getCommunityId(), "MO", "Clay", DType.CITYTAX);
    	String currentYear = (payDate.getYear()+1900) + "";
    	
    	delinquentAmount.append("0.0");
    	String[][] rebBody = reb.getBody();
    	int len = rebBody.length;
    	for (int i=0;i<len-1;i++) {
    		String[] line = rebBody[i];
    		if (line.length>=4) {
    			String date = line[2].trim();
    			String year = "";
    			if (date.length()>=4)
    				year = date.substring(date.length()-4);
    			if (!currentYear.equals(year))
    				delinquentAmount.append("+").append(line[3].replaceAll("[\\$,]", ""));
    		}
    	}
    	String delqAmt = GenericFunctions.sum(delinquentAmount.toString().replaceAll("[\\(\\)]", ""), searchId);
    	m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), delqAmt);
    	
    	if (rebBody[len-1].length>=4) {
    		String date = rebBody[len-1][2].trim();
    		if (date.length()>=4) {
    			String year = date.substring(date.length()-4);
    			m.put(TaxHistorySetKey.YEAR.getKeyName(), year);
    		}
		}
   }
   
   public static void composeParcelNumber(ResultMap m, long searchId){
	   String parcelNumber = (String) m.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
	   
	   if (org.apache.commons.lang.StringUtils.isNotBlank(parcelNumber) && parcelNumber.contains("SEGMENT PARCEL")){
		   String streetNumber = (String) m.get("tmpStreetNo");
		   if (org.apache.commons.lang.StringUtils.isNotBlank(streetNumber)){
			   parcelNumber += streetNumber;
			   m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNumber);
		   }
	   }
	   
   }
   
   public static void setReceiptsTNHamiltonEP(ResultMap m,long searchId) throws Exception {
    	TNHamiltonEP.setReceiptsTNHamiltonEP(m, searchId);
   }
   
   public static void partyNamesTNHamiltonEP(ResultMap m,long searchId) throws Exception {
   	TNHamiltonEP.partyNamesTNHamiltonEP(m, searchId);
  }

   protected static Pattern lotsPatMIOaklandRO = Pattern.compile( "(\\d+)\\s+-\\s+(\\d+)");
   protected static String processLotMIOaklandRO(String lot,long searchId){
		lot = lot.replaceAll("\\b0+(\\d)", "$1"); // remove leading zeros from lots value
		Matcher match = lotsPatMIOaklandRO.matcher(lot);  // if the interval limits are equal, remove the duplicate lot value
		if (match.find()){
			if (match.group(1).equals(match.group(2)))
				lot = match.group(1);
		}
		return lot;
   }
   
   public static void joinPISMIOaklandRO(ResultMap m,long searchId) throws Exception {
   	
	   	ResultTable subdiv = (ResultTable) m.get("tmpSubdivTable");
	   	ResultTable condo = (ResultTable) m.get("tmpCondoTable");
	   	ResultTable prop = (ResultTable) m.get("tmpPropertyTable");
	   	
	   	if (subdiv != null){
	    	int len = subdiv.getLength();
	    	for (int i=0; i<len; i++){
	    		subdiv.body[i][3] = processLotMIOaklandRO(subdiv.body[i][3],searchId);
	    		subdiv.body[i][2] = subdiv.body[i][2].replaceAll("\\b0+(\\d)", "$1"); // remove leading zeros from block value
	    	}	   		
	   	}
	   	
	   	if (condo != null){
	   		int len = condo.getLength();
	   		// add a new column to condo table, Suddivision name, containing the condo name
	   		String [][] body = new String [len][8];
	    	for (int i=0; i<len; i++){
	    		body[i][0] = processLotMIOaklandRO(condo.body[i][0],searchId);
	    		System.arraycopy(condo.body[i], 1, body[i], 1, 6);
	    		body[i][7] = condo.body[i][3]; 
	    	}
	    	condo.body = body;
	    	String[] header = new String[8];
	    	System.arraycopy(condo.head, 0, header, 0, 7);
	    	header[7] = "Subdivision";
	    	condo.head = header;
	    	condo.map.put("SubdivisionName", new String[]{"Subdivision", ""});
	   	}
	   	
	   	if (subdiv != null && condo == null && prop == null){
	    	m.put("PropertyIdentificationSet", subdiv);
	   		return;
	   	}
	   	
	   	if (subdiv == null && condo != null && prop == null){
	   		m.put("PropertyIdentificationSet", condo);
	   		return;
	   	}

	   	if (subdiv == null && condo == null && prop != null){
	   		m.put("PropertyIdentificationSet", prop);
	   		return;
	   	}
	   	
	   	if (subdiv == null && condo != null && prop != null){ //condo && prop are not null => PIS is formed by condo & prop joined tables (ex. doc #0443911 from 1999)
	   		int condoLen = condo.getLength();
	   		int propLen = prop.getLength();
	   		
	   		int len = condoLen + propLen;
	   		String [] header = {"Condominium", "Town", "Range", "Low Unit - High Unit", "Pin #", "CondominiumPlatBook", "CondominiumPlatPage", "Subdivision", "Section"};
	   		String [][] body = new String[len][header.length];
	   		for (int i=0; i<condoLen; i++){
	   			body [i][0] = condo.body[i][3];
	   			body [i][1] = condo.body[i][1];
	   			body [i][2] = condo.body[i][2]; 
	   			body [i][3] = condo.body[i][0];
	   			body [i][4] = condo.body[i][4];
	   			body [i][5] = condo.body[i][5];
	   			body [i][6] = condo.body[i][6];
	   			body [i][7] = condo.body[i][7];
	   			body [i][8] = "";
	   		}
	   		for (int i=0; i<propLen; i++){
	   			body [condoLen + i][0] = "";
	   			body [condoLen + i][1] = prop.body[i][1];
	   			body [condoLen + i][2] = prop.body[i][2];
	   			body [condoLen + i][3] = "";
	   			body [condoLen + i][4] = prop.body[i][0];
	   			body [condoLen + i][5] = "";
	   			body [condoLen + i][6] = "";
	   			body [condoLen + i][7] = "";
	   			body [condoLen + i][8] = prop.body[i][3];
	   		}
	    	
	    	ResultTable pis =  new ResultTable();
	    	pis.setHead(header);
	    	Map map = condo.getMap().getMap();
	    	map.put("SubdivisionSection", new String[]{"Section", ""});
	    	pis.setMap(map);
	    	pis.setBody(body);
	    	pis.setReadOnly();
	    	m.put("PropertyIdentificationSet", pis);
	   		return;
	   	}
	   	
	   	if (subdiv != null && condo == null && prop != null){ //subdiv && prop are not null => PIS if formed by subdiv & prop joined tables (ex. doc #8957 from 1983)
	   		int subdivLen = subdiv.getLength();
	   		int propLen = prop.getLength();
	   		
	   		int len = subdivLen + propLen;	   		
	       	
	   		String [] header = {"Town", "Range", "Block", "Lot(s)", "Subdivision", "Pin #", "PlatBook", "PlatPage", "Section"};
	   		String [][] body = new String[len][header.length];
	   		for (int i=0; i<subdivLen; i++){
	   			System.arraycopy(subdiv.body[i], 0, body[i], 0, 8);	   			
	   			body [i][8] = "";
	   		}
	   		for (int i=0; i<propLen; i++){
	   			body [subdivLen + i][0] = prop.body[i][1];
	   			body [subdivLen + i][1] = prop.body[i][2];
	   			body [subdivLen + i][2] = "";
	   			body [subdivLen + i][3] = "";
	   			body [subdivLen + i][4] = "";
	   			body [subdivLen + i][5] = prop.body[i][0];
	   			body [subdivLen + i][6] = "";
	   			body [subdivLen + i][7] = "";
	   			body [subdivLen + i][8] = prop.body[i][3];
	   		}
	    	
	    	ResultTable pis =  new ResultTable();
	    	pis.setHead(header);
	    	Map map = subdiv.getMap().getMap();
	    	map.put("SubdivisionSection", new String[]{"Section", ""});
	    	pis.setMap(map);
	    	pis.setBody(body);
	    	pis.setReadOnly();
	    	m.put("PropertyIdentificationSet", pis);
	   		return;
	   	}
	   	
	   	// theoretically there also can be the following cases, but with no examples found yet:
	   	// prop == null && subdiv && condo are not null => PIS if formed by subdiv & cond joined tables
	   	// subdiv && condo && prop are not null => PIS if formed by subdiv & cond & prop joined tables
   }
      
   public static void legalMIOaklandAO(ResultMap m,long searchId) throws Exception {
   		
	   String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
    	
    	if (legal == null || legal.length() == 0)
    		return;
    	
    	String section = legal.replaceFirst(".*\\bSEC(?:TION)? (\\w+) .*", "$1");
    	String twnsh = legal.replaceFirst("^T(\\d+)N,.*", "$1");
    	String range = legal.replaceFirst("^T\\w+, R(\\d+)E, .*", "$1");
    	
    	if (!legal.equals(section) && section.length()>0){
    		m.put("PropertyIdentificationSet.SubdivisionSection", section);
    	}
    	
    	if (!legal.equals(twnsh) && twnsh.length()>0){
    		m.put("PropertyIdentificationSet.SubdivisionTownship", twnsh);
    	}
    	
    	if (!legal.equals(range) && range.length()>0){
    		m.put("PropertyIdentificationSet.SubdivisionRange", range);
    	}
    	
    	String subdivision = legal.replaceFirst("^T\\w+, R\\w+, SEC(?:TION)? \\w+ (.*)", "$1");
    	subdivision = subdivision.replaceAll("\\bTO\\b", "-");
    	subdivision = subdivision.replaceAll("\\s*\\d+-\\d+-\\d+\\s*", " ");
    	subdivision = subdivision.replaceAll("\\s*\\d+/\\d+/\\d+\\s*", " ");
    	subdivision = subdivision.replaceAll("\\bLOTS? SUB\\b", "SUB");
    	subdivision = subdivision.replaceAll("(\\d+ )INCL\\s*", "$1");
    	subdivision = subdivision.replace("CONDOMINIUMUNIT", "CONDOMINIUM UNIT");
   
    	String subdivName = "";
    	String unit = subdivision.replaceFirst(".*\\bUNIT (\\d+).*", "$1");
    	if (!unit.equals(subdivision)){
    		//m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
    		m.put("PropertyIdentificationSet.SubdivisionLotNumber", unit); //parse unit as lot, because Low Unit and High Unit are parsed as Lots on Oakland RO, cf. specs
    	}
    	
    	subdivName = subdivision.replaceFirst(".*\\bOF(?: LOT \\d+)? '(.+) SUB.*'.*", "$1");
    	subdivName = subdivision.replaceFirst(".*\\bOF(?: LOT \\d+)? '(.+ ADD(ITION)?).*'.*", "$1"); // TM's request 02/19/2007 
    	if (subdivName.equals(subdivision)){
			if (subdivision.contains("CONDOMINIUM")){
				subdivName = subdivision.replaceFirst(".*\\bPLAN NO\\.? \\d+ (.+ CONDO(MINIUM)?).*", "$1");
				subdivName = subdivName.replaceFirst(".*\\bPLAN NO\\.? \\d+ (.+) UNIT\\b.*", "$1");
				if (subdivName.length()>0){
					m.put("PropertyIdentificationSet.SubdivisionCond", subdivName);
				}
			} else {
				subdivName = subdivision.replaceFirst("^(RESUB|AMENDED PLAT|REPLAT) OF (LOTS? )?", "");
		    	subdivName = subdivName.replaceFirst("(.*?)\\s*\\b(LOTS?|(PT|PART) (OF)? (LOTS?|[NESW]{1,2} \\d)|SUB(DIVISION|'N)?|[NWES]LY)\\b.*", "$1");
		    	subdivName = subdivName.replaceFirst("(.*?\\s+ADD(ITION)?)\\b.*", "$1"); // TM's request 02/19/2007 (PID 14-28-255-008)
		    	subdivName = subdivName.replaceAll("(?i)^\\w+'S (RE)?PLAT ((NO\\.? )?\\d+|OF)", "");  // PID 08-29-401-012, 23-27-330-004
		    	subdivName = subdivName.replaceFirst("(?i)(.*) [SWNE] \\d+ FT OF\\b", "$1");
		    	subdivName = subdivName.replaceFirst(".+\\d,? (.+)$", "$1");
		    	subdivName = subdivName.replaceFirst("\\s*\\bOF$", "");
			}
    	}
    	
    	if (subdivName.length() > 0){
    		m.put("PropertyIdentificationSet.SubdivisionName", subdivName);
    	}
    	
    	Pattern pat = Pattern.compile("\\bLOTS? ([\\d\\s,&-]+)");
    	Matcher mat = pat.matcher(subdivision);
    	String lot = "";
    	while (mat.find()){
    		lot = lot + " " + mat.group(1);
    	}
    	lot = lot.replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ");
    	if (lot.length()>0){
    		lot = StringFormats.ReplaceIntervalWithEnumeration(lot);
    		lot = StringFormats.RemoveDuplicateValues(lot);
    		lot = StringFormats.ReplaceEnumerationWithInterval(lot);
    		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
    	}
    	
    	pat = Pattern.compile("\\bBL(?:OC)?KS? ([\\d\\s,&]+)");
    	mat = pat.matcher(subdivision);
    	String block = "";
    	while (mat.find()){
    		block = block + " " + mat.group(1);
    	}
    	block = block.replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ");
    	if (block.length()>0){
    		block = StringFormats.RemoveDuplicateValues(block);
    		block = StringFormats.ReplaceEnumerationWithInterval(block);
    		m.put("PropertyIdentificationSet.SubdivisionBlock", block);
    	}
   }
   
   /**
    * Removes the "empty" lines from Oakland AO transfers tables. Empy lines are those with $0 sale amount and no other info. Fix for bug #1166. 
    * @param m
    * @throws Exception
    */
   public static void transfersMIOaklandAO(ResultMap m,long searchId) throws Exception {
	   
	   ResultTable sds = (ResultTable)m.get("SaleDataSet");
	   if (sds == null) 
		   return;
	   
	   int len = sds.getLength();
	   int width = sds.head.length;
	   List newBody = new ArrayList();
	   boolean emptyRow;
	   for (int i=0; i<len; i++){
		   emptyRow = true;
		   for (int j=0; j<width; j++){
			   if (!"-".equals(sds.body[i][j])){
				   emptyRow = false;
				   break;
			   }
		   }
		   if (!emptyRow){
			   newBody.add(Arrays.asList(sds.body[i]));
		   }
	   }
	   if (newBody.size() >0){
		   sds.setReadWrite();
		   sds.setBody(newBody);
		   sds.setReadOnly();
		   m.put("SaleDataSet", sds);
	   }	   
   }
   
   public static void taxMIOaklandTR(ResultMap m,long searchId) throws Exception {
	   
	   String billed1 = (String)m.get("tmpBilled1");
	   String billed2 = (String)m.get("tmpBilled2");
	   String paid1= (String)m.get("tmpPaid1");
	   String paid2= (String)m.get("tmpPaid2");
	   String duePenalty1 = (String)m.get("tmpDuePenalty1");
	   String duePenalty2 = (String)m.get("tmpDuePenalty2");
	   String due1 = (String)m.get("tmpDue1");
	   String due2 = (String)m.get("tmpDue2");	 
	   String prevDelinquent = (String)m.get("tmpPrevDelinquent");

	   // Regular taxes
	   
	   // base amount
	   BigDecimal baseAmountTotal = new BigDecimal(0);
	   if (billed1 != null && billed1.length()>0){
		   baseAmountTotal = baseAmountTotal.add(new BigDecimal(billed1));
       }
	   if (billed2 != null && billed2.length()>0){
		   baseAmountTotal = baseAmountTotal.add(new BigDecimal(billed2));
       }
	   m.put( "TaxHistorySet.BaseAmount", baseAmountTotal.toString());
	   
	   // amount paid
	   BigDecimal amountPaidTotal = new BigDecimal(0);
	   if (paid1 != null && paid1.length()>0){
		   amountPaidTotal = amountPaidTotal.add(new BigDecimal(paid1));
       }
	   if (paid2 != null && paid2.length()>0){
		   amountPaidTotal = amountPaidTotal.add(new BigDecimal(paid2));
       }
	   m.put( "TaxHistorySet.AmountPaid", amountPaidTotal.toString());
	   
	   // amount due
	   BigDecimal amountDueTotal = new BigDecimal(0);
	   if (due1 != null && due1.length()>0){
		   amountDueTotal = amountDueTotal.add(new BigDecimal(due1));
       }
	   if (due2 != null && due2.length()>0){
		   amountDueTotal = amountDueTotal.add(new BigDecimal(due2));
       }
	   String totalDueStr = amountDueTotal.toString();
	   m.put( "TaxHistorySet.TotalDue", totalDueStr);
	   m.put( "TaxHistorySet.CurrentYearDue", totalDueStr);
	   
	   // delinquent amount
       BigDecimal delinquentTotal = new BigDecimal(0);	  
       BigDecimal previousDelinquent = new BigDecimal(0);
       if(prevDelinquent != null && prevDelinquent.length() != 0) {               
	       String[] amounts = prevDelinquent.split("\\+");	       
	       for (int i=0; i<amounts.length; i++){
	           try{
	        	   previousDelinquent = previousDelinquent.add(new BigDecimal(amounts[i]));
	           } catch( NumberFormatException nfe ) {}
	       }	       	      
       }
       delinquentTotal = delinquentTotal.add(previousDelinquent);
       
       if (duePenalty1 != null && duePenalty1.length()>0 && due1 != null && due1.length()>0){
    	   delinquentTotal = delinquentTotal.add(new BigDecimal(due1));
       }
       if (duePenalty2 != null && duePenalty2.length()>0 && due2 != null && due2.length()>0){
    	   delinquentTotal = delinquentTotal.add(new BigDecimal(due2));
       }
       m.put( "TaxHistorySet.DelinquentAmount", delinquentTotal.toString());
       m.put("TaxHistorySet.PriorDelinquent", previousDelinquent.toString());
       
       // Special assessment taxes
       billed1 = (String)m.get("tmpBilledSA1");
	   billed2 = (String)m.get("tmpBilledSA2");
	   paid1= (String)m.get("tmpPaidSA1");
	   paid2= (String)m.get("tmpPaidSA2");
	   String prevBilled = (String)m.get("tmpPrevBilledSA");
	   String prevPaid = (String)m.get("tmpPrevPaidSA");
	   
	   // Special assessment base amount
	   baseAmountTotal = new BigDecimal(0);
	   if (billed1 != null && billed1.length()>0){
		   baseAmountTotal = baseAmountTotal.add(new BigDecimal(billed1));
       }
	   if (billed2 != null && billed2.length()>0){
		   baseAmountTotal = baseAmountTotal.add(new BigDecimal(billed2));
       }
	   m.put( "SpecialAssessmentSet.BaseAmount", baseAmountTotal.toString());

	   // Special assessment amount paid
	   amountPaidTotal = new BigDecimal(0);
	   if (paid1 != null && paid1.length()>0){
		   amountPaidTotal = amountPaidTotal.add(new BigDecimal(paid1));
       }
	   if (paid2 != null && paid2.length()>0){
		   amountPaidTotal = amountPaidTotal.add(new BigDecimal(paid2));
       }
	   m.put( "SpecialAssessmentSet.AmountPaid", amountPaidTotal.toString());
	   
	   // Special assessment total due
	   totalDueStr = (baseAmountTotal.subtract(amountPaidTotal)).toString();
	   m.put( "SpecialAssessmentSet.TotalDue", totalDueStr);
	   m.put( "SpecialAssessmentSet.CurrentYearDue", totalDueStr);
	   
	   // Special assessment delinquent
	   BigDecimal prevBilledTotal = new BigDecimal(0);	   
       if(prevBilled != null && prevBilled.length() != 0) {               
	       String[] amounts = prevBilled.split("\\+");	       
	       for (int i=0; i<amounts.length; i++){
	           try{
	        	   prevBilledTotal = prevBilledTotal.add(new BigDecimal(amounts[i]));
	           } catch( NumberFormatException nfe ) {}
	       }	       	      
       }
       
       BigDecimal prevPaidTotal = new BigDecimal(0);
       if(prevPaid != null && prevPaid.length() != 0) {               
	       String[] amounts = prevPaid.split("\\+");	       
	       for (int i=0; i<amounts.length; i++){
	           try{
	        	   prevPaidTotal = prevPaidTotal.add(new BigDecimal(amounts[i]));
	           } catch( NumberFormatException nfe ) {}
	       }	       	      
       }
       String saDelinq = (prevBilledTotal.subtract(prevPaidTotal)).toString();
       m.put( "SpecialAssessmentSet.PriorDelinquent", saDelinq);
       m.put( "SpecialAssessmentSet.DelinquentAmount", saDelinq);
   }
   
   public static void legalMIMacombTR(ResultMap m, long searchId) throws Exception {
	   
	   String desc = (String)m.get("PropertyIdentificationSet.PropertyDescription");
	   if (desc == null || desc.length() == 0)
		   return;
	   
	   // clean legal description
	   desc = desc.replace("\"", "");
	   desc = desc.replaceAll("'(?=\\d)", ""); // fix for bug #1395 (PID 14-10-403-006) 
	   desc = desc.replaceAll("(?<=\\d)'", ""); 
	   desc = desc.replace(".", "");
	   desc = desc.replaceAll("\\bPART\\b( OF)?\\s*", "");
	   desc = desc.replaceAll("\\(.+\\)", "");
	   desc = desc.replaceAll("\\b(ASSESSOR'?S PLAT|A/P) NO\\s*\\d+( OF)?", "");
	   desc = desc.replaceAll("\\b(SUPERVISOR'?S PLAT|S/P)( NO \\d+)?( OF)?", "");
	   desc = desc.replaceAll("\\bL\\d+(,|\\s)\\s*P\\d+(-\\s*\\d+)?", "");	   
	   
	   // extract subdivision name
	   if (desc.matches(".*\\b(SUB(DIVISION)?|MCCP|LOTS?|ADD(ITION)?)\\b.*")) {
		   String subdiv = desc.replaceFirst("(.+?)\\b(SUB(DIVISION)?|MCCP|LOTS?|ADD(ITION)?|NO\\s*\\d+|(W|S|N|E|WEST|SOUTH|NORTH|EAST) [\\d\\./]+( FT)?|CONDO(MINIUM)?)\\b.*", "$1");
		   if (!subdiv.equals(desc)){
			   subdiv = subdiv.replaceFirst("\\bT\\d+N.*SEC\\s+\\d+", "");
			   subdiv = subdiv.trim().replaceAll("^[,;]", "");
			   subdiv = subdiv.replaceAll("[,;]$", "");
			   subdiv = subdiv.trim();
			   if (subdiv.length() > 0){
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (desc.matches(".*\\bCONDO(MINIUM)?\\b.*")){
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
				   }
			   }
		   }
	   }
	   
	   // extract lot number
	   String lot = "";
	   desc = desc.replaceAll("\\d+[/\\.]\\d+", "");
	   Pattern p = Pattern.compile("\\bLOTS?\\s+([\\d,\\s-&]+)\\b");
	   Matcher match = p.matcher(desc);
	   while (match.find()){
		   lot = lot + " " + match.group(1);
	   }
	   lot = lot.replace(",", " ");
	   lot = lot.replace("&", " ");
	   
	   lot = LegalDescription.cleanValues(lot.trim(), true, true);
	   if (lot.length() > 0){
		   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
	   }
	  
	   // extract unit
	   String unit = desc.replaceFirst(".*\\bUNIT\\s+(\\d+).*", "$1");
	   if (!unit.equals(desc)){
		   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
	   }
	   
	   // extract block
	   String block = desc.replaceFirst(".*\\bBL(?:OC)?K\\s+(\\d+).*", "$1");
	   if (!block.equals(desc)){
		   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
	   } 
	   
	   // extract section, township and range
	   String section = desc.replaceFirst(".*\\bSEC\\s+(\\d+).*", "$1");
	   if (!section.equals(desc)){
		   m.put("PropertyIdentificationSet.SubdivisionSection", section);
	   } 
	   
	   String twn = desc.replaceFirst(".*\\bT(\\d+N)\\b.*", "$1");
	   if (!twn.equals(desc)){
		   m.put("PropertyIdentificationSet.SubdivisionTownship", twn);
	   } 
	   
	   String rng = desc.replaceFirst(".*\\bR(\\d+E)\\b.*", "$1");
	   if (!rng.equals(desc)){
		   m.put("PropertyIdentificationSet.SubdivisionRange", rng);
	   } 
   }
   
   protected static boolean isCondoMIMacombRO(String subdiv,long searchId){
	   return subdiv.matches(".*\\b(CONDO|PLAN)\\b.*");
   }
   
   public static void subdivCondoMIMacombIntermRO(ResultMap m,long searchId) throws Exception {
	   String subdiv = (String)m.get("PropertyIdentificationSet.PropertyDescription");
	   if (subdiv == null || subdiv.length() ==0)
		   return;
	   
	   boolean isCondo = isCondoMIMacombRO(subdiv,searchId);
	   subdiv = StringFormats.SubdivCondoMIMacombRO(subdiv);
	   
	   if (subdiv.length() >0){
		   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		   if (isCondo){
			   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		   }
	   }
   }
   
   public static void legalMIMacombRO(ResultMap m,long searchId) throws Exception {
	   
	   String desc = (String)m.get("PropertyIdentificationSet.PropertyDescription");
	   ResultTable pis = (ResultTable)m.get("PropertyIdentificationSet");
	   
	   // try to extract legal info from PropertyDescription, if PropertyIdentificationSet table was not set with subdiv, lot, etc. info 
	   if (desc != null && desc.length()>0 && pis == null){
		   Pattern p = Pattern.compile("\\bLOTS?\\s+([\\d,\\s-&]+)");
		   Matcher ma = p.matcher(desc);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", ma.group(1));
		   }
		   
		   p = Pattern.compile("\\bL(\\d+)P(\\d+)\\b");
		   ma = p.matcher(desc);
		   if(ma.find()){
			   m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			   m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		   }
	   }
	   
	   
//	   // clean subdivision/condo name 
//	   if (pis != null){
//		   String body[][] = pis.body;
//		   int cnt = body.length;
//		   int len = 0;
//		   if (body[0] != null){
//			   len = body[0].length;
//		   }
//		   String subdiv;
//		   boolean isCondo;
//		   for (int i=0; i<cnt; i++){
//			   subdiv = body[i][0];
//			   isCondo = isCondoMIMacombRO(subdiv,searchId);
//			   subdiv = cleanSubdivCondoMIMacombRO(subdiv,searchId);
//			   body[i][0] = subdiv;
//			   if (isCondo){
//				   body[i][1] = subdiv;
//			   }
//			   // replace '-' with '' in all PIS cells
//			   for (int j=0; j<len; j++){
//				   if (body[i][j].equals("-"))
//					   body[i][j] = "";
//			   }
//		   }
//	   }
   }
   
   public static void setGrantorGranteeMIMacombCO(ResultMap m, long searchId) throws Exception {
	   ResultTable cdif = (ResultTable) m.get("CourtDocumentIdentificationSet");
	   if (cdif == null)
		   return;
	   
	   String body [][] = cdif.getBody();
	   int len = body.length;
	   if (len == 0)
		   return;
	   String partyName = "";
	   StringBuilder grantor = new StringBuilder();
	   StringBuilder grantee = new StringBuilder();
	   
	   for(int i=0; i<len; i++){
		   partyName = body[i][0];
		   if (partyName == null || partyName.length() == 0)
			   continue;
  
		   if (DBManager.getPartyType(body[i][1], DBManager.PartySites.MIMACOMBCO) == 1){
			   grantor.append(" / ").append(partyName);
		   } else {
			   grantee.append(" / ").append(partyName);
		   }
	   }
	   
	   String grantorStr = grantor.toString();
	   grantorStr = grantorStr.replaceFirst("^ / ", "");
	   if (grantorStr.length() > 0){
		   m.put("SaleDataSet.Grantor", grantorStr);
	   }
	   
	   String granteeStr = grantee.toString();
	   granteeStr = granteeStr.replaceFirst("^ / ", "");
	   if (granteeStr.length() > 0){
		   m.put("SaleDataSet.Grantee", granteeStr);
	   }
   }
   
   public static void setGrantorGranteeInterCourts(ResultMap m, DBManager.PartySites site) throws Exception {
	   String partyType = (String)m.get("CourtDocumentIdentificationSet.PartyType");
	   if (partyType == null)
		   return;   
	   String partyName = (String)m.get("CourtDocumentIdentificationSet.PartyName");
	   if (partyName == null)
		   return;
	   if (DBManager.getPartyType(partyType, site) == 1){
		   m.put("SaleDataSet.Grantor", partyName);
	   } else {
		   m.put("SaleDataSet.Grantee", partyName);
	   }
   }
   
   public static void setGrantorGranteeInterMIMacombCO(ResultMap m, long searchId) throws Exception {
	   setGrantorGranteeInterCourts(m, DBManager.PartySites.MIMACOMBCO);
   }
   
   public static void partyNamesMIMacombTR(ResultMap m, long searchId)throws Exception{
       String name = (String) m.get("tmpOwnerFullName");
       if (name == null)
       	return; 
       partyNamesTokenizerMIMacombTR(m, name);
	}
   
   public static void partyNamesTokenizerMIMacombTR(ResultMap m, String s) throws Exception {
   	
	   // cleanup
	   s = s.replaceAll("\\bETAL$", "");
	   s = s.replace("&$", ""); 
	   s = s.replaceAll("\\b(ESTATE|TRUSTEE|JTWFR)S?$", "");
	   s = s.replaceAll("\\s{2,}", " ").trim();

       List<List> body = new ArrayList<List>();
	   String[] a = new String[6];
	   if (NameUtils.isCompany(s)){ 
		   a[2] = s; 
		   a[0] = ""; a[1] = ""; a[3] = ""; a[4] = ""; a[5] = "";
		   addOwnerNames(a, "", "", true, false, body);
		   storeOwnerInPartyNames(m, body);
		   return;
	   }
	   
	   Matcher ma1;
	   String[] suffixes;
	   // names are usually as L F M, with a few exceptions (e.g. GARY J & MARY A DENEVE)
	   ma1 = Pattern.compile("([A-Z]+(?: [A-Z])?)\\s*(&\\s*[A-Z]+(?: [A-Z])?)( [A-Z'-]{2,})").matcher(s);
	   if (ma1.matches()){
		   s = ma1.group(1) + ma1.group(3) + ma1.group(2) + ma1.group(3);
		   a = StringFormats.parseNameDesotoRO(s);
		   suffixes = extractNameSuffixes(a);
		   addOwnerNames(a, suffixes[0], suffixes[1], false, false, body);
		   storeOwnerInPartyNames(m, body);
		   return;
	   }
		   		   
       String entities[] = s.split("\\s*[&/]\\s*");		// ROGGENBUCK FRED/GARY/DAVID/SAMS DAN
       
       String lastName = "";    
       String suff = "";
       for (int i=0; i<entities.length; i++){
    	   if (i == 0){
    		   a = StringFormats.parseNameNashville(entities[i]);    
    		   suffixes = extractSuffix(a[1]);
    		   if (suffixes[1].length() != 0){
    			   suff = suffixes[1];
    			   a[1] = suffixes[0];
    		   } else {
        		   suffixes = extractSuffix(a[0]);
        		   if (suffixes[1].length() != 0){
        			   suff = suffixes[1];
        			   a[0] = suffixes[0];
        			   if (a[0].length() == 0 && a[1].length() != 0){
        				   a[0] = a[1]; 
        				   a[1] = "";
        			   }
        		   }
    		   }
    		   addOwnerNames(a, suff, "", false, false, body);
    	   } else {
    		   if (entities[i].matches("[A-Z]+( [A-Z])?") || entities[i].matches("[A-Z] [A-Z]+")){
    			   entities[i] = lastName + " " + entities[i];
    		   }
    		   ma1 = Pattern.compile("([A-Z'-]{2,}) (" + lastName + " .+)").matcher(entities[i]); // BEEBE DARYL G & GREENE BEEBE SHAUN
    		   boolean has2LastNames = false;
    		   if (ma1.matches()){
    			   entities[i] = ma1.group(1) + "-" + ma1.group(2); 
    			   has2LastNames = true;
    		   }
    		   a = StringFormats.parseNameNashville(entities[i]);
    		   if (has2LastNames){
    			   a[2] = a[2].replaceFirst("-", " ");  
    		   }
        	   suffixes = extractNameSuffixes(a);
    		   addOwnerNames(a, suffixes[0], "", false, false, body);
    	   }
    	   lastName = a[2];
       }                
       storeOwnerInPartyNames(m, body);                
   }
   
   public static void setGrantorGranteeMIWaynePR(ResultMap m, long searchId) throws Exception {
	   ResultTable cdif = (ResultTable) m.get("CourtDocumentIdentificationSet");
	   if (cdif == null)
		   return;
	   
	   String body [][] = cdif.getBody();
	   int len = body.length;
	   if (len == 0)
		   return;
	   
	   String partyName = "";
	   String partyType = "";
	   String lastPartyType = "";
	   StringBuilder grantor = new StringBuilder();
	   StringBuilder grantee = new StringBuilder();
	   
	   for(int i=0; i<len; i++){
		   partyName = body[i][0];
		   partyType = body[i][1];
		   if (partyName == null || partyName.length() == 0)
			   continue;
		   
		   // for party types AKA, NKA or FKA, the actual party type is the previous one (e.g. case #1994-529505-DA)
		   if (partyType.equals("AKA") || partyType.equals("NKA") || partyType.equals("FKA")){
			   partyType = lastPartyType;
		   } else {			   
			   lastPartyType = partyType;
		   }
		   partyName = partyName.replaceFirst("(.+)\\s+(JR|SR|I|II|III)\\b(.*)", "$1$3 $2");
			   
		   if (DBManager.getPartyType(partyType, DBManager.PartySites.MIWAYNEPR) == 1){
			   grantor.append(" / ").append(partyName);
		   } else {
			   grantee.append(" / ").append(partyName);
		   }
	   }
	   
	   String grantorStr = grantor.toString();
	   grantorStr = grantorStr.replaceFirst("^ / ", "");
	   if (grantorStr.length() > 0){
		   m.put("SaleDataSet.Grantor", grantorStr);
	   }
	   	   
	   String granteeStr = grantee.toString();
	   granteeStr = granteeStr.replaceFirst("^ / ", "");
	   if (granteeStr.length() > 0){
		   m.put("SaleDataSet.Grantee", granteeStr);
	   }
   }   
   
   public static void setGrantorGranteeInterMIWaynePR(ResultMap m, long searchId) throws Exception {
	   setGrantorGranteeInterCourts(m, DBManager.PartySites.MIWAYNEPR);
   }
    
   public static void legalMIWayneEP(ResultMap m,long searchId) throws Exception {
	   
	   String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
	   if (legal == null || legal.length() == 0)
		   return;
	   
	   //clean up the legal description
	   legal = legal.replaceAll("(\\bAND\\s|&\\s*)?\\bVAC\\s.+\\sADJ\\b", "");
	   legal = legal.replaceAll("(\\bAND\\s|&\\s*)?\\bVAC(\\sALLEYS?(\\sSUB)?)?\\b", "");	   
	   legal = legal.replaceAll("(\\bAND\\s|&\\s*)?(\\b(W|N|S|E)\\s)?[\\d\\.]+\\sFT(\\sOF)?(\\sLAND IN FRONT)?\\b", "");
	   legal = legal.replaceAll("(?i)\\bEXC\\b", "");
	   legal = legal.replaceAll("(?i)\\bTHEREOF\\b", "");
	   legal = legal.replaceAll("\\s{2,}", " ");
	   
	   // extract plat book & page	   
	   Pattern p = Pattern.compile("\\bL(\\d+)\\s+PG?'?S?(\\d+(?:-\\d+)?)\\s+(?:PLATS|DEEDS)\\b");
	   Matcher ma = p.matcher(legal); 
	   if (ma.find()){
		   m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
		   m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
	   }
	   
	   String subdiv = "";
	   if (!legal.matches(".*\\bCONDO(MINIUM)?\\b.*")){  // not condominium => look for lot and subddivision name
		   // lot
		   String lot = "";
		   p = Pattern.compile(".+?\\b(\\d+(?:\\s*(?:-|THRU|AND|&|,)\\s*\\d+)*)\\s(.*)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   lot = ma.group(1);
			   lot = lot.replaceFirst("\\s*THRU\\s*", "-");
			   lot = lot.replaceAll("\\s*(AND|&|,)\\s*", " ");
			   legal = ma.group(2);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		   }
		   
		   // subdivision name		   
		   p = Pattern.compile("(.*?)\\s(SUB|PLAN|L\\d+)\\b.*");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(1); 
		   }
	   } else {										// condominium => look for unit, building and condominium name
		   // unit
		   p = Pattern.compile("\\b(?:UNIT|CONDO)\\s(\\d+)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1));
		   }
		   
		   // building
		   p = Pattern.compile("\\bBLDG\\s(\\d+)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		   }
		   
		   // condominium name
		   String condo = "";
		   String plan = "";
		   if (legal.contains("\"")){
			   condo = legal.replaceFirst(".*\"(.*)\".*", "$1");
			   plan = legal.replaceFirst(".*\\bPLAN\\s+NO\\.?\\s+(\\d+)\\b.*", "$1");
		   } else {
			   p = Pattern.compile("\\bPLAN\\s+NO\\.?\\s+(\\d+)-?\\s*(.*?)\\s(RECORDED|L\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   plan = ma.group(1);
				   condo = ma.group(2);
			   }
		   }
		   condo = condo.replaceAll("\\b(LOFTS?|CONDOMINIUM)\\b", "").trim();
		   if (condo.length() > 0){
			   condo = condo.replaceAll("[()]", "");
			   subdiv = condo;
			   m.put("PropertyIdentificationSet.SubdivisionCond", condo);			   
		   }
		   if (plan.length() > 0){
			   m.put("PropertyIdentificationSet.PlanNo", plan);
		   }
	   }
	   subdiv = subdiv.trim();
	   if (subdiv.length() > 0){
		   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
	   }	   
   }
   
   public static void transfersMIWayneEP(ResultMap m,long searchId) throws Exception {
	   
	   //replace '-' with '' in all SaleDataSet cells
	   ResultTable sds = (ResultTable)m.get("SaleDataSet");
	   if (sds == null) 
		   return;
	   
	   int len = sds.getLength();
	   int width = sds.head.length;
	   String body[][] = sds.body;
	   for (int i=0; i<len; i++){
		   for (int j=0; j<width; j++){
			   if (body[i][j].equals("-"))
				   body[i][j] = "";
		   }
	   }
	   m.put("SaleDataSet", sds);	   
   }
      
   public static void taxMIWayneEP(ResultMap m,long searchId) throws Exception {
	   
	   String tmpTotalAmt1 = (String)m.get("tmpTotalAmt1");
	   if (tmpTotalAmt1 == null)
		   return;
	   
	   String tmpTotalAmt2 = (String)m.get("tmpTotalAmt2");
	   String tmpTotalPaid1 = (String)m.get("tmpTotalPaid1");
	   String tmpTotalPaid2 = (String)m.get("tmpTotalPaid2");
	   String tmpLastPaid1 = (String)m.get("tmpLastPaid1");
	   String tmpLastPaid2 = (String)m.get("tmpLastPaid2");
	   String tmpTotalDue1 = (String)m.get("tmpTotalDue1");
	   String tmpTotalDue2 = (String)m.get("tmpTotalDue2");	   
	   String tmpInterest1 = (String)m.get("tmpInterest1");
	   String tmpInterest2 = (String)m.get("tmpInterest2");
	   	   
	   // base amount
	   BigDecimal baseAmountTotal = new BigDecimal(0);
	   if (tmpTotalAmt1.length() > 0){
		   baseAmountTotal = new BigDecimal(tmpTotalAmt1);
       }
	   if (tmpTotalAmt2 != null && tmpTotalAmt2.length() > 0){
		   baseAmountTotal = baseAmountTotal.add(new BigDecimal(tmpTotalAmt2));
       }
	   if (tmpInterest1 != null && tmpInterest1.length() > 0){
		   baseAmountTotal = baseAmountTotal.subtract(new BigDecimal(tmpInterest1));
       }
	   if (tmpInterest2 != null && tmpInterest2.length() > 0){
		   baseAmountTotal = baseAmountTotal.subtract(new BigDecimal(tmpInterest2));
	   }
	   
	   m.put( "TaxHistorySet.BaseAmount", baseAmountTotal.toString());
	   
	   // amount paid
	   BigDecimal amountPaidTotal = new BigDecimal(0);
	   if (tmpTotalPaid1 != null && tmpTotalPaid1.length() > 0){
		   amountPaidTotal = new BigDecimal(tmpTotalPaid1);
       }
	   if (tmpTotalPaid2 != null && tmpTotalPaid2.length() > 0){
		   amountPaidTotal = amountPaidTotal.add(new BigDecimal(tmpTotalPaid2));
       }
	   m.put( "TaxHistorySet.AmountPaid", amountPaidTotal.toString());
	   
	   // amount due
	   BigDecimal amountDueTotal = new BigDecimal(0);
	   if (tmpTotalDue1 != null && tmpTotalDue1.length() > 0){
		   amountDueTotal = new BigDecimal(tmpTotalDue1);
       }
	   if (tmpTotalDue2 != null && tmpTotalDue2.length() > 0){
		   amountDueTotal = amountDueTotal.add(new BigDecimal(tmpTotalDue2));
       }
	   String totalDueStr = amountDueTotal.toString();
	   m.put( "TaxHistorySet.TotalDue", totalDueStr);
	   m.put( "TaxHistorySet.CurrentYearDue", totalDueStr);
	   
	   // prior year delinquent - seems there are no prior year delinquent records
	   
	   // delinquent amount
       String delinquentTotal = "0.00";
       Date dueDate = DBManager.getDueDateForCity(
               InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity()
                       .getID().longValue(), InstanceManager.getManager()
                       .getCurrentInstance(searchId).getCurrentCounty().getCountyId()
                       .longValue());
       Date dateNow = new Date();
       if (dateNow.after(dueDate)){ 
    	   delinquentTotal = totalDueStr;
       }       
       m.put( "TaxHistorySet.DelinquentAmount", delinquentTotal);

       // date paid
       String lastPaid = "";
       if (tmpLastPaid1 != null && tmpLastPaid1.length() > 0){
    	   lastPaid = tmpLastPaid1;
       } else if (tmpLastPaid2 != null && tmpLastPaid2.length() > 0){
    	   lastPaid = tmpLastPaid2;
       }
       m.put("TaxHistorySet.DatePaid", lastPaid);
   }   

   protected static String cleanCondoMIWayneRO(String condo){
	   condo = condo.replaceFirst("(.+)\\s+CONDO(MINIUM)?.*", "$1");
	   return condo;
   }
   
   protected static String cleanSubdivMIWayneRO(String subdiv){
	   subdiv = subdiv.replaceFirst("(.+)\\s+(NO\\.?\\s+\\d+).*", "$1");
	   return subdiv;
   }
   
   public static void legalMIWayneRO(ResultMap m, long searchId) throws Exception {
	   ResultTable pis = (ResultTable)m.get("PropertyIdentificationSet");
	   	   
	   // clean subdivision/condo name 
	   if (pis != null){
		   String body[][] = pis.body;
		   int cnt = body.length;
		   int len = 0;
		   if (body[0] != null){
			   len = body[0].length;
		   }

		   String condo;
		   String subdiv;
		   for (int i=0; i<cnt; i++){
			   //replace '-' with '' in all PIS cells
			   for (int j=0; j<len; j++){
				   if (body[i][j].equals("-"))
					   body[i][j] = "";
			   }			   
			   
			   subdiv = body[i][3];
			   if (subdiv != null && subdiv.trim().length() > 0){
				   subdiv = cleanSubdivMIWayneRO(subdiv);
				   body[i][3] = subdiv;
			   }
			   
			   condo = body[i][11];
			   if (condo != null && condo.trim().length() > 0){
				   condo = cleanCondoMIWayneRO(condo);
				   body[i][11] = condo;				   
				   body[i][3] = condo;  // copy condo to subdiv name cell
			   }							 
		   }
	   }	   
   }
   
   public static void legalILCookRO(ResultMap m, long searchId) throws Exception {
	   ResultTable pis = (ResultTable)m.get("PropertyIdentificationSet");
	   	   
	   // clean subdivision/condo name 
	   if (pis != null){
		   String body[][] = pis.body;
		   int cnt = body.length;
		   int len = 0;
		   if (body[0] != null){
			   len = body[0].length;
		   }

		   for (int i=0; i<cnt; i++){
			   //replace '-' with '' in all PIS cells
			   for (int j=0; j<len; j++){
				   if (body[i][j].equals("-"))
					   body[i][j] = "";
			   }			   
		   }
	   }	   
   }
   
   protected static final Pattern intervalPattern = Pattern.compile("([0-9]+)-([0-9]+)");
   
   /**
    * Fix reversed intervals and intervals containing a single vale
    * 3-3 becomes 3
    * 12-10 becomes 10-12
    * @param value
    * @return
    */
   public static String fixInterval(String value){
	   
	   Matcher matcher = intervalPattern.matcher(value);
	   if(matcher.matches()){
		   int val1 = Integer.valueOf(matcher.group(1));
		   int val2 = Integer.valueOf(matcher.group(2));
		   
		   if(val1 < val2){
			   return val1 + "-" + val2;
		   }
		   if(val1 > val2){
			   return val2 + "-" + val1;
		   }
		   return "" + val1;		   
	   } else{
		   return value;
	   }
   }
   
   public static void legalILCookIS(ResultMap m, long searchId) throws Exception {
	   String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
	   if (StringUtils.isEmpty(legal)) {
		   return;
	   }
	
	   GenericISI.legalILCookIS(m, legal);
   }
   
   public static void parseSTR(ResultMap m, long searchId) throws Exception {
	   GenericISI.parseSTR(m);
   }
     
   public static void improveCrossRefsParsing(ResultMap resultMap, long searchId) throws Exception{
	   
	   String src = (String) resultMap.get("OtherInformationSet.SrcType");
	   if (StringUtils.isEmpty(src)){
		   return;
	   }
	   
	   GenericISI.improveCrossRefsParsing(resultMap, src, searchId);
   }
   
   public static void parseAddressIS(ResultMap resultMap, long searchId) throws Exception{
	   
	   String src = (String) resultMap.get("OtherInformationSet.SrcType");
	   if (StringUtils.isEmpty(src)){
		   return;
	   }
	   
	   GenericISI.parseAddressIS(resultMap);
   }
   
   public static void taxILLakeTR(ResultMap m, long searchId) throws Exception {
	   ILLakeTR.taxILLakeTR(m, searchId);
   }
   
   public static void legalILLakeTR(ResultMap m, long searchId) throws Exception {
	   
	   String legal1 = (String) m.get("tmpLegal1");
	   String legal2 = (String) m.get("tmpLegal2");
	   
	   if (StringUtils.isEmpty(legal1))
		   return;
	   
	   String legal = legal1 + " " + legal2;
	   m.put("PropertyIdentificationSet.PropertyDescription", legal);

	   ILLakeTR.legalILLakeTR(m, legal);
   }
   
   public static void partyNamesILLakeTR(ResultMap m, long searchId) throws Exception {
	   
	   String owner = (String) m.get("tmpOwnerName");
	   if (StringUtils.isEmpty(owner))
		   return;
	   
	   ILLakeTR.partyNamesILLakeTR(m, owner);
   }
      
   public static void partyNamesInterILMcHenryTR(ResultMap m, long searchId) throws Exception {
	   
	   String owner = (String) m.get("tmpOwnerName");
	   if (StringUtils.isEmpty(owner))
		   return;
	   
	   ILMcHenryTR.partyNamesInterILMcHenryTR(m, owner);
   }
   
   // Extracts street no, street name and city from an address
   protected static String[] parseAddressILCookLA(String address) throws Exception {
	   
	   String addr[] = {"", "", ""};
	   
	   address = address.replaceAll("\\s{2,}", " ").trim();
	   String addressNorm = Normalize.normalizeString(address);
	   String addressTokens[] = addressNorm.split(" ");
	   
	   boolean foundSuffix = false;
	   String city = "";
	   String street = "";
	   int i;
	   for (i=1; i<addressTokens.length-1; i++){
		   foundSuffix = Normalize.isSuffix(addressTokens[i]);
		   if (foundSuffix) 
			   break;
	   }
	   
	   // city is created with the last tokens of the address, but after street suffix, if street suffix exists
	   if (foundSuffix){
		   addressTokens = address.split(" ");
		   for (int j=i+1; j<addressTokens.length; j++)
			   city += " " + addressTokens[j];
		   city = city.trim();
		   
		   for (int j=0; j<=i; j++)
			   street += " " + addressTokens[j];
		   street = street.trim();
		   
	   } else { // if street suffix doesn't exist, then the last token of address is parsed as city 
//		   city = addressTokens[addressTokens.length-1];
//		   for (int j=0; j<addressTokens.length-1; j++)
//			   street += " " + addressTokens[j];
//		   street = street.trim();
		   Pattern p = Pattern.compile("(.+)\\s(.+)$");
		   Matcher match = p.matcher(address);
		   if (match.find()){
			   city = match.group(2);
			   street = match.group(1);
			   
			   if (city.length() == 1 || city.matches(".*\\d.*")){
				   city = "";
				   street = addressNorm;
			   }
		   }
	   }	   
	   	   
	   if (street.length() != 0){
		   addr[0] = StringFormats.StreetNo(street);
		   addr[1] = StringFormats.StreetName(street);		   
	   }
	   
	   if (city.length() != 0){
		   addr[2] = city;
	   }
	   
	   return addr;
   }
   
   public static void addressTableParserILCookLA(ResultMap m, long searchId) throws Exception {
	   
	   ResultTable addresses = (ResultTable) m.get("PropertyIdentificationSet");	   
       if (addresses == null)
    	   return;
       
       String body[][] = addresses.body;
       int len = body.length;
       if (len == 0)
    	   return;
              
       for (int k = 0; k < len; k ++){        
		   String address = body[k][1];
		   String []tokens = parseAddressILCookLA(address);
		   body[k][2] = tokens[0];
		   body[k][3] = tokens[1];
		   body[k][4] = tokens[2];
       }
       
       m.put("PropertyIdentificationSet", addresses);
   }
   
   public static void addressParserILCookLA(ResultMap m, long searchId) throws Exception {
	   
	   String address = (String) m.get("tmpAddressLine1");	   
       if (address == null || address.length() == 0)
    	   return;
       
	   String []tokens = parseAddressILCookLA(address);
	   
	   m.put("PropertyIdentificationSet.StreetNo", tokens[0]);
	   m.put("PropertyIdentificationSet.StreetName", tokens[1]);
	   m.put("PropertyIdentificationSet.City", tokens[2]);
	  
   }

   public static void legalILDuPageRO(ResultMap m, long searchId) throws Exception{
	   ILDuPageRO.legalILDuPageRO(m, searchId);
   }
   
   public static void legalILCookLA(ResultMap m, long searchId) throws Exception {
	   
	   ResultTable rt = (ResultTable)m.get("PropertyIdentificationSet");
	   if (rt == null)
		   return;
	   String [][]body = rt.body;	 
	   if (body.length == 0)
    	   return;
	   
	   String longLegal, partialLegal, subdiv;
	   for (int i=0; i<body.length; i++){
	   
		   longLegal = body[i][8];
		   partialLegal = body[i][9];	   
		   subdiv = body[i][10];
		   
		   // set PropertyDescription, if not already set
		   if (longLegal == null || longLegal.length() == 0){
			   if (subdiv != null && subdiv.length() > 0){
				   body[i][8] = subdiv;
			   } else if (partialLegal != null && partialLegal.length() > 0){
				   body[i][8] = partialLegal;
			   } 
		   }
		   
		   // set Subdivision Name, if not already set
		   if (subdiv == null || subdiv.length() == 0){
			   if (longLegal != null && longLegal.length() > 0){
				   body[i][10] = longLegal.replaceFirst("(.*)\\sSUBDIVISION\\b.*", "$1");
			   }
		   }		   
	   }
   }
   
   public static void stdPisILCookLA(ResultMap m, long searchId) throws Exception { 
		  
	   String name = (String) m.get("PropertyIdentificationSet.OwnerLastName");
	   if (name == null || name.length() == 0)
		   return;
	   
	   name = name.replaceFirst("\\bTAXPAYER OF\\b.*", "");  	
	   name = name.replaceAll("[\\{\\}]", "");
	   if (!NameUtils.isCompany(name)){		  			  		
  		
		   String tokens[] = StringFormats.parseNameILCookLA(name);
		 
		   m.put("PropertyIdentificationSet.OwnerFirstName", tokens[0]);
		   m.put("PropertyIdentificationSet.OwnerMiddleName", tokens[1]);
		   m.put("PropertyIdentificationSet.OwnerLastName", tokens[2]);
		   m.put("PropertyIdentificationSet.SpouseFirstName", tokens[3]);
		   m.put("PropertyIdentificationSet.SpouseMiddleName", tokens[4]);
		   m.put("PropertyIdentificationSet.SpouseLastName", tokens[5]);
	   }	  
   }
   
   public static void partyNamesTokenizerILCookTU(ResultMap m, String name) throws Exception {
	   
	   List<List> body = new ArrayList<List>();
	   String[] tokens = {"", "", "", "", "", ""};
	   name = name.replaceFirst("\\bTAXPAYER OF\\b.*", "");
	   name = name.replaceAll("[\\{\\}]", "");
	   
	   if (!NameUtils.isCompany(name)){		  			  			  		   
		   tokens = StringFormats.parseNameILCookLA(name);
		   String[]  suffixes = extractNameSuffixes(tokens);
		   addOwnerNames(tokens, suffixes[0], suffixes[1], false, false, body);		 
	   } else {
		   tokens[2] = name;
		   addOwnerNames(tokens, "", "", true, false, body);
	   }
	   storeOwnerInPartyNames(m, body);
   }
   
   public static void partyNamesILCookTU(ResultMap m, long searchId) throws Exception { 
		  
	   String name = (String) m.get("tmpOwnerFullName");
	   if (name == null || name.length() == 0)
		   return;
  	  	
	   partyNamesTokenizerILCookTU(m, name);	  
   }
   
   public static void crossRefsILCookLA(ResultMap m, long searchId) throws Exception {
	   String remarks = (String)m.get("tmpRemarks");
	   
	   if (remarks == null || remarks.length() == 0)
		   return;
	     	
	   String crtInstr = (String)m.get("SaleDataSet.InstrumentNumber");
	   
	   Pattern p = Pattern.compile("\\b[A-Za-z]?\\d{7,}\\b");
	   Matcher ma = p.matcher(remarks);
	   String instrList = "";
	   while (ma.find()){
		   String ref = ma.group();
		   if(!crtInstr.equals(ref)){
			   instrList += " " + ref ;
		   }
	   }
	   ma.reset();
	   p = Pattern.compile("\\b\\d+[A-Za-z]{2}\\d{2,}\\b");
	   ma = p.matcher(remarks);
	   while (ma.find()){
		   String ref = ma.group();
		   if(!crtInstr.equals(ref)){
			   instrList += " " + ref ;
		   }
	   }
	   instrList = instrList.trim();
	   if (instrList.length() == 0)
		   return;
	   
	   String [] instrArray = instrList.split(" ");
	   String [][] body = new String [instrArray.length][1];
	   for (int i=0; i<instrArray.length; i++){
		   body[i][0] = instrArray[i];
	   }
	   	   
	   String [] header = {"InstrumentNumber"};
	   Map<String,String[]> map = new HashMap<String,String[]>();
	   map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
	   
	   ResultTable cr = new ResultTable();	
	   cr.setHead(header);
	   cr.setBody(body);
	   cr.setMap(map);
	   
	   m.put("CrossRefSet", cr);
   }

   private static String checkNegative(String amount){
	   if(amount != null && amount.matches("\\([0-9.]+\\)")){
		   amount = "-" + amount.replace("(", "").replace(")","");
	   }
	   return amount;
   }
   
   public static void taxILCookLA(ResultMap m, long searchId) throws Exception { 
   
	   boolean ignore2ndInstallment = false;
	   
	   String tmpDueDate1 = (String) m.get("tmpDueDate1");
	   String tmpDueDate2 = (String) m.get("tmpDueDate2");
	   
	   String tmpBaseAmount1 = checkNegative((String) m.get("tmpBaseAmount1"));
	   String tmpBaseAmount2 = checkNegative((String) m.get("tmpBaseAmount2"));
	   String tmpAmountPaid1 = checkNegative((String) m.get("tmpAmountPaid1"));
	   String tmpAmountPaid2 = checkNegative((String) m.get("tmpAmountPaid2"));
	   String tmpAmountDue1 = checkNegative((String) m.get("tmpAmountDue1"));
	   String tmpAmountDue2 = checkNegative((String) m.get("tmpAmountDue2"));	   
	   	   	   
	   BigDecimal delinquent = new BigDecimal(0.00);
	   BigDecimal totalDue = new BigDecimal(0.00);
	   BigDecimal baseAmount = new BigDecimal(0.00);
	   BigDecimal amountPaid = new BigDecimal(0.00);
	   
	   List<String> line1 = null, line2 = null;	   
	   if (tmpBaseAmount1 != null && tmpBaseAmount1.length() != 0){
		   baseAmount = new BigDecimal(tmpBaseAmount1);
		   line1 = new ArrayList<String>();
		   line1.add(tmpBaseAmount1);		   
	   }
	   if (!ignore2ndInstallment){
		   if (tmpBaseAmount2 != null && tmpBaseAmount2.length() != 0){
			   baseAmount = baseAmount.add(new BigDecimal(tmpBaseAmount2));
			   line2 = new ArrayList<String>();
			   line2.add(tmpBaseAmount2);		
		   }
	   }
	   m.put("TaxHistorySet.BaseAmount", baseAmount.toString());
	   
	   if (tmpAmountPaid1 != null && tmpAmountPaid1.length() != 0){
		   amountPaid = new BigDecimal(tmpAmountPaid1);		   
	   } else {
		   tmpAmountPaid1 = "0.00";
	   }
	   if (line1 != null){
		   line1.add(tmpAmountPaid1);
	   }
	   if (!ignore2ndInstallment){
		   if (tmpAmountPaid2 != null && tmpAmountPaid2.length() != 0){
			   amountPaid = amountPaid.add(new BigDecimal(tmpAmountPaid2));			   
		   } else {
			   tmpAmountPaid2 = "0.00";
		   }
		   if (line2 != null){
			   line2.add(tmpAmountPaid2);
		   }
	   }
	   m.put("TaxHistorySet.AmountPaid", amountPaid.toString());
	   
	   DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
	   Date dateNow = new Date();
	   	   
	   if (tmpAmountDue1 != null && tmpAmountDue1.length() != 0){
		   totalDue = new BigDecimal(tmpAmountDue1);		  
		   if (tmpDueDate1 != null && tmpDueDate1.length() != 0){
			   try{	
				   Date dueDate1 = df.parse(tmpDueDate1);
				   if (dueDate1 != null && dateNow.after(dueDate1)){
					   delinquent = new BigDecimal(tmpAmountDue1);
				   }
			   } catch (Exception e){}
		   }
	   } else {
		   tmpAmountDue1 = "0.00";
	   }
	   if (line1 != null){
		   line1.add(tmpAmountDue1);
	   }
	   if (!ignore2ndInstallment){
		   if (tmpAmountDue2 != null && tmpAmountDue2.length() != 0){
			   totalDue = totalDue.add(new BigDecimal(tmpAmountDue2));
			   if (tmpDueDate2 != null && tmpDueDate2.length() != 0){
				   try{	
					   Date dueDate2 = df.parse(tmpDueDate2);
					   if (dueDate2 != null && dateNow.after(dueDate2)){
						   delinquent = delinquent.add(new BigDecimal(tmpAmountDue2));
					   }
				   } catch (Exception e){}
			   }
		   } else {
			   tmpAmountDue2 = "0.00";
		   }
		   if (line2 != null){
			   line2.add(tmpAmountDue2);
		   }
	   } 
	   
	   m.put("TaxHistorySet.TotalDue", totalDue.toString());	   
	   m.put("TaxHistorySet.CurrentYearDue", totalDue.toString());
	   m.put("TaxHistorySet.DelinquentAmount", delinquent.toString());
	   m.put("TaxHistorySet.PriorDelinquent", "0.00"); 	// no delinquent amount info for prior years found on this site yet
	   
	   try{
		   if( line1!=null && line1.size()>2 && Double.parseDouble(line1.get(2))<0.5){
			   line1.add("PAID");
		   }else{
			   line1.add("UNPAID");
		   }
		   
		   if( line2!=null && line2.size()>2 && Double.parseDouble(line2.get(2))<0.5){
			   line2.add("PAID");
		   }else{
			   line2.add("UNPAID");
		   }
	   }catch(Exception e){
		   e.printStackTrace();
	   }
	   
	   
	   List<List> bodyInstallments = new ArrayList<List>();
	   if (line1 != null){
		   bodyInstallments.add(line1);
		   if (line2 != null){
			   bodyInstallments.add(line2);
		   }
		   String [] header = {"BaseAmount", "AmountPaid", "TotalDue", "Status"};				   
		   Map<String,String[]> map = new HashMap<String,String[]>();
		   map.put("BaseAmount", new String[]{"BaseAmount", ""});
		   map.put("AmountPaid", new String[]{"AmountPaid", ""});
		   map.put("TotalDue", new String[]{"TotalDue", ""});
		   map.put("Status", new String[]{"Status", ""});
		   
		   ResultTable installments = new ResultTable();	
		   installments.setHead(header);
		   installments.setBody(bodyInstallments);
		   installments.setMap(map);
		   m.put("TaxInstallmentSet", installments);
	   }
	}
   
   public static void taxASK(ResultMap m, long searchId) throws Exception {
	   
	   String baseAmt = (String) m.get("tmpBaseAmount");
	   if (baseAmt != null && baseAmt.length() != 0){	   
		   baseAmt = sum(baseAmt, searchId);
		   m.put("TaxHistorySet.BaseAmount", baseAmt);	   
		   
		   String totalDue = sum((String) m.get("tmpAmountDue"), searchId);
		   m.put("TaxHistorySet.TotalDue", totalDue);
		   
		   // AmountPaid = BaseAmount + (Interest + Penalties) - TotalDue
		   // since we don't have (Interest + Penalties) info, ignoring it may result in an incorrect value (e.g. for delinquent taxes, where TD>BA => AP is negative
		   // comment it if not ok
		   BigDecimal amountPaid = new BigDecimal(baseAmt).subtract(new BigDecimal(totalDue));
		   m.put("TaxHistorySet.AmountPaid", amountPaid.toString());
		   
		   String delinq = sum((String) m.get("tmpDelinquent"), searchId);
		   m.put("TaxHistorySet.DelinquentAmount", delinq);
	   } 
	   
	   // Special Assessment taxes
	   String baseAmtSA = (String) m.get("tmpSABaseAmount");
	   if (baseAmtSA != null && baseAmtSA.length() != 0){
		   baseAmtSA = sum(baseAmtSA, searchId);
		   m.put("SpecialAssessmentSet.BaseAmount", baseAmtSA);	   
		   
		   String totalDueSA = sum((String) m.get("tmpSAAmountDue"), searchId);
		   m.put("SpecialAssessmentSet.TotalDue", totalDueSA);
		   
		   BigDecimal amountPaidSA = new BigDecimal(baseAmtSA).subtract(new BigDecimal(totalDueSA));
		   m.put("SpecialAssessmentSet.AmountPaid", amountPaidSA.toString());
		   
		   String delinqSA = sum((String) m.get("tmpSADelinquent"), searchId);
		   m.put("SpecialAssessmentSet.DelinquentAmount", delinqSA);
		   
		   String year = (String) m.get("tmpYear");
		   if (year != null && year.length() != 0){
			   m.put("SpecialAssessmentSet.Year", year);
		   }
	   }
	   
	   // Prior delinquent - not in XML response, but linked to responnse in a TIF file
   }
   
   public static void ownerTNGenericAO(ResultMap m, long searchId) throws Exception {
	   TNGenericAO.ownerTNGenericAO(m, searchId);
   }
   
   public static void legalTNGenericAO(ResultMap m, long searchId) throws Exception {
	   TNGenericAO.legalTNGenericAO(m, searchId);
   }
   
   public static void addressTNGenericAO(ResultMap m, long searchId) throws Exception {
	   TNGenericAO.addressTNGenericAO(m, searchId);
   }
   
   public static void legalTNGenericTR(ResultMap m, long searchId) throws Exception {
	   
	   String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
	   if (legal == null || legal.length() == 0){
		   return;
	   }
	   
	   legal = legal.replaceAll("(?is)\\b(PH(?:ASE)?|SECT?(?:ION)?)(\\d+|[A-Z])", "$1 $2");
	   	   
	   // extract subdivision/condominium name 
	   String subdivName = legal.replaceFirst("(.*?)(\\b(S/D|SUBD?|SUBDIVISION|SD|RE\\s?SUB?|PH(ASE)?|SECT?(ION)?|CONDO(MINIUM)?S?|U(NIT|-)|ADDN?)\\b|(#|\\bNO)?\\s*\\d).*", "$1");
	   subdivName = subdivName.replaceFirst("[-\\.,]+\\s*$", "");
	   subdivName = subdivName.replaceFirst("^SD\\s+", "");
	   subdivName = subdivName.trim();
	   	   
	   legal = legal.replaceAll("(\\w)\\s+([-&,])\\s+(\\w)", "$1$2$3");
	   legal = legal.replaceAll("\\bTWO\\b", "2");
	   legal = legal.replaceAll("\\bONE\\b", "1");
	   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
	   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
	   legal = legal.replaceAll("(?:\\bNO|#)\\s*(\\d)", "$1");
	   
	   // extract phase from legal description
	   String phase = ""; 
	   Pattern p = Pattern.compile("\\bPH(?:ASE)?\\s+([-&,\\w]+)\\b");
	   Matcher ma = p.matcher(legal);
	   ma.usePattern(p);
	   ma.reset();
	   if (ma.find())
		   phase = ma.group(1);
	   
	   // extract section from legal description
	   String section = "";
	   p = Pattern.compile("\\bSECT?(ION)?\\s+([-&,\\w]+)\\b");
	   ma.usePattern(p);
	   ma.reset();
	   if (ma.find())
		   section = ma.group(2);
	   
	   // extract unit from legal description
	   String unit = "";
	   p = Pattern.compile("\\bU(?:NIT\\s+|-)([-&,\\w]+)\\b");
	   ma.usePattern(p);
	   ma.reset();
	   if (ma.find())
		   unit = ma.group(1);
	   
	   // trim zeros from the beginning of the legal descriptors
	   phase = phase.replaceFirst("^0+(\\w)", "$1");
	   section = section.replace("\"", "").replaceFirst("^0+(\\w)", "$1");
	   unit = unit.replace("\"", "").replaceFirst("^0+(\\w)", "$1");
	   
	   phase = phase.replace("&", ",").replace(",", ", ");
	   section = section.replace("&", ",").replace(",", ", ");
	   unit = unit.replace("&", ",").replace(",", ", ");
	   
	   // save extracted values in InfSets
	   if (subdivName.length() != 0){
		   m.put("PropertyIdentificationSet.SubdivisionName", subdivName);
		   if (legal.matches(".*\\bCONDO(MINIUM)?S?\\b.*"))
			   m.put("PropertyIdentificationSet.SubdivisionCond", subdivName);
	   }
	   if (phase.length() != 0){
		   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
	   }
	   
	   if (section.length() != 0){
		   m.put("PropertyIdentificationSet.SubdivisionSection", section);
	   } 
	   
	   if (unit.length() != 0){
		   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
	   }
   }
   
   protected static boolean middleNameIsNotFull(String middle){
	   if (middle == null)
		   return true;
	   middle = middle.replaceFirst("\\.", "");
	   return (middle.length() <= 1) || middle.equals("II") || middle.equals("III") || middle.equals("JR") || middle.equals("SR");
   }
   
   public static void ownerTNGenericTR(ResultMap m, long searchId) throws Exception {
	   
	   String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
   	
	   	if(StringUtils.isEmpty(s))
	       	return;
	   	
	   	TNGenericTR.ownerTNGenericTR(m, searchId);
   }
   
   public static void partyNamesTNDavidsonTR(ResultMap m, long searchId) throws Exception {
	   
	   String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
	   String c = (String) m.get("tmpCoOwner");
		   	
	   if(StringUtils.isEmpty(s))
		   return;
			   	
	   TNDavidsonTR.partyNamesTNDavidsonTR(m, searchId);
			
   }
   
   public static void parseAddressDavidsonTR(ResultMap m, long searchId) throws Exception {
	   TNDavidsonTR.parseAddressTNDavidsonTR(m, searchId);
   }
   
   public static void taxTNGenericTR(ResultMap m, long searchId) throws Exception {
	   
	   TNGenericTR.taxTNGenericTR(m, searchId);
   }        
   
   public static void setInstrAsBP(ResultMap m, long searchId) throws Exception {
	   
	   String instrNo = (String) m.get("SaleDataSet.InstrumentNumber");
	   if (instrNo == null || instrNo.length() == 0){
		   String book = (String) m.get("SaleDataSet.Book");
		   String page = (String) m.get("SaleDataSet.Page");
		   if (book != null && book.length() != 0 && page != null && page.length() != 0){
			   instrNo = book + "_" + page;
			   //m.put("SaleDataSet.InstrumentNumber", instrNo);// commented because of B 4480
		   }
	   }
   }
   
   public static void removeNullBP(ResultMap m, long searchId) throws Exception {	// fix for bug #2458
	   
	   String book = (String) m.get("SaleDataSet.Book");
	   String page = (String) m.get("SaleDataSet.Page");
	   if ("0".equals(book) || "0".equals(page) ){
		   m.remove("SaleDataSet.Book");
		   m.remove("SaleDataSet.Page");
	   }
   }
   
   public static String replaceOnlyNumbers(String s){
	   s = s.replaceAll("\\bFIRST\\b", "1ST");
	   s = s.replaceAll("\\bSECOND\\b", "2ND");
	   s = s.replaceAll("\\bTHIRD\\b", "3RD");
	   s = s.replaceAll("\\bFOURTH\\b", "4TH");	   
	   s = s.replaceAll("\\bFIFTH\\b", "5TH");
	   s = s.replaceAll("\\bSIXTH\\b", "6TH");
	   s = s.replaceAll("\\bSEVENTH\\b", "7TH");
	   s = s.replaceAll("\\bEIGHTH\\b", "8TH");
	   s = s.replaceAll("\\bNINTH\\b", "9TH");
	   s = s.replaceAll("\\bTENTH\\b", "10TH");
	   s = s.replaceAll("\\bELEVENTH\\b", "11TH");
	   s = s.replaceAll("\\bTWELFTH\\b", "12TH");
	   s = s.replaceAll("\\bTHIRTEENTH\\b", "13TH");
	   s = s.replaceAll("\\bFOURTEENTH\\b", "14TH");
	   s = s.replaceAll("\\bFIFTEENTH\\b", "15TH");
	   s = s.replaceAll("\\bSIXTEENTH\\b", "16TH");
	   s = s.replaceAll("\\bSEVENTEENTH\\b", "17TH");
	   s = s.replaceAll("\\bEIGHTEENTH\\b", "18TH");
	   s = s.replaceAll("\\bNINETEENTH\\b", "19TH");
	   s = s.replaceAll("\\bTWENTIETH\\b", "20TH");
	   
	   s = s.replaceAll("\\bONE\\b", "1");
	   s = s.replaceAll("\\bTWO\\b", "2");
	   s = s.replaceAll("\\bTHREE\\b", "3");
	   s = s.replaceAll("\\bFOUR\\b", "4");
	   s = s.replaceAll("\\bFIVE\\b", "5");
	   s = s.replaceAll("\\bSIX\\b", "6");
	   s = s.replaceAll("\\bSEVEN\\b", "7");
	   s = s.replaceAll("\\bEIGHT\\b", "8");
	   s = s.replaceAll("\\bNINE\\b", "9");
	   s = s.replaceAll("\\bTEN\\b", "10");
	   s = s.replaceAll("\\bELEVEN\\b", "11");
	   s = s.replaceAll("\\bTWELVE\\b", "12");
	   s = s.replaceAll("\\bTHIRTEEN\\b", "13");
	   s = s.replaceAll("\\bFOURTEEN\\b", "14");
	   s = s.replaceAll("\\bFIFTEEN\\b", "15");
	   s = s.replaceAll("\\bSIXTEEN\\b", "16");
	   s = s.replaceAll("\\bSEVENTEEN\\b", "17");
	   s = s.replaceAll("\\bEIGHTEEN\\b", "18");
	   s = s.replaceAll("\\bNINETEEN\\b", "19");
	   s = s.replaceAll("\\bTWENTY\\b", "20");
	   s = s.replaceAll("\\bTHIRTY\\b", "30");
	   s = s.replaceAll("\\bFORTY\\b", "40");
	   s = s.replaceAll("\\bFIFTY\\b", "50");
	   s = s.replaceAll("\\bSIXTY\\b", "60");
	   s = s.replaceAll("\\bSEVENTY\\b", "70");
	   s = s.replaceAll("\\bEIGHTY\\b", "80");
	   s = s.replaceAll("\\bNINETY\\b", "90");
	   	   	   	   	   
	   return s;
   }
   
   public static String switchNumbers(String s, boolean switchBack){
	   if (!switchBack){
		   	s = replaceOnlyNumbers(s);
	   } else {
		   s = s.replaceAll("\\b1ST\\b", "FIRST");
		   s = s.replaceAll("\\b2ND\\b", "SECOND");
		   s = s.replaceAll("\\b3RD\\b", "THIRD");
		   s = s.replaceAll("\\b4TH\\b", "FOURTH");	   
		   s = s.replaceAll("\\b5TH\\b", "FIFTH");
		   s = s.replaceAll("\\b6TH\\b", "SIXTH");
		   s = s.replaceAll("\\b7TH\\b", "SEVENTH");
		   s = s.replaceAll("\\b8TH\\b", "EIGHTH");
		   s = s.replaceAll("\\b9TH\\b", "NINTH");
		   s = s.replaceAll("\\b10TH\\b", "TENTH");
		   s = s.replaceAll("\\b11TH\\b", "ELEVENTH");
		   s = s.replaceAll("\\b12TH\\b", "TWELFTH");
		   s = s.replaceAll("\\b13TH\\b", "THIRTEENTH");
		   s = s.replaceAll("\\b14TH\\b", "FOURTEENTH");
		   s = s.replaceAll("\\b15TH\\b", "FIFTEENTH");
		   s = s.replaceAll("\\b16TH\\b", "SIXTEENTH");
		   s = s.replaceAll("\\b17TH\\b", "SEVENTEENTH");
		   s = s.replaceAll("\\b18TH\\b", "EIGHTEENTH");
		   s = s.replaceAll("\\b19TH\\b", "NINETEENTH");
		   s = s.replaceAll("\\b20TH\\b", "TWENTIETH");
		   
		   s = s.replaceAll("\\b1\\b", "ONE");
		   s = s.replaceAll("\\b2\\b", "TWO");
		   s = s.replaceAll("\\b3\\b", "THREE");
		   s = s.replaceAll("\\b4\\b", "FOUR");
		   s = s.replaceAll("\\b5\\b", "FIVE");
		   s = s.replaceAll("\\b6\\b", "SIX");
		   s = s.replaceAll("\\b7\\b", "SEVEN");
		   s = s.replaceAll("\\b8\\b", "EIGHT");
		   s = s.replaceAll("\\b9\\b", "NINE");
		   s = s.replaceAll("\\b10\\b", "TEN");
		   s = s.replaceAll("\\b11\\b", "ELEVEN");
		   s = s.replaceAll("\\b12\\b", "TWELVE");
		   s = s.replaceAll("\\b13\\b", "THIRTEEN");
		   s = s.replaceAll("\\b14\\b", "FOURTEEN");
		   s = s.replaceAll("\\b15\\b", "FIFTEEN");
		   s = s.replaceAll("\\b16\\b", "SIXTEEN");
		   s = s.replaceAll("\\b17\\b", "SEVENTEEN");
		   s = s.replaceAll("\\b18\\b", "EIGHTEEN");
		   s = s.replaceAll("\\b19\\b", "NINETEEN");
		   s = s.replaceAll("\\b20\\b", "TWENTY");
		   s = s.replaceAll("\\b30\\b", "THIRTY");
		   s = s.replaceAll("\\b40\\b", "FORTY");
		   s = s.replaceAll("\\b50\\b", "FIFTY");
		   s = s.replaceAll("\\b60\\b", "SIXTY");
		   s = s.replaceAll("\\b70\\b", "SEVENTY");
		   s = s.replaceAll("\\b80\\b", "EIGHTY");
		   s = s.replaceAll("\\b90\\b", "NINETY");
	   }
	   	   	   	   	   
	   return s;
   }
   
   public static String replaceNumbers(String s){
	   if(s == null){
		   s = "";
	   }
	   s = s.replaceAll("(?<=\\d)-(?=\\d)", "@@@");
	   s = s.replaceAll("(?<=\\d)\\s", "%%%");
	   s = s.replaceAll("\\s(?=\\d)", "%%%");
	   s = replaceOnlyNumbers(s);
	   s = s.replaceAll("\\b(\\d+)0[\\s-](\\d)((?:ST|ND|RD|TH)?)\\b", "$1$2$3"); // e.g. TWENTY ONE -> 20 1-> 21
	   s = s.replaceAll("@@@", "-");
	   s = s.replaceAll("%%%", " ");

	   return s;
   }
   
   /**
    * switch a legal particle with his value: 1st SEC become SEC 1
    * @param expression
    * @param identifier
    * @return
    */
   public static String switchIdentifierWithNumber(String expression, String identifier){
	   
	   if(StringUtils.isEmpty(expression)){
		   return "";
	   }
	   
	   Matcher m = Pattern.compile("(?is)(\\d+)(?:ST|ND|TH|RD)\\s+(" + identifier + ")\\b").matcher(expression);
	   if (m.find()) {
		   expression = expression.replaceAll("(?is)(\\d+)(?:ST|ND|TH|RD)\\s+(" + identifier + ")\\b", "$2 $1");
	   } 
	   
	   return expression;
   }
   
   public static void setTaxYear(ResultMap m, long searchId) throws Exception {
	   
	   // set the tax year with the current year if unable to extract it from the server response
	   String foundTaxYear = (String) m.get("TaxHistorySet.Year");
	   if (foundTaxYear != null && foundTaxYear.length() != 0)
		   return;
	   
	   int currentYear = Calendar.getInstance().get(Calendar.YEAR);
	   m.put("TaxHistorySet.Year", String.valueOf(currentYear));
	   
   }      
                     
   public static void cleanUCCBook(ResultMap m, long searchId) throws Exception {
	   String book = (String)m.get("SaleDataSet.Book");
	   String page = (String)m.get("SaleDataSet.Page");
	   if (!StringUtils.isEmpty(book) && ! StringUtils.isEmpty(page)){
		   if (book.matches("^UCC$") && page.equals("0")){
			   m.remove("SaleDataSet.Book");
			   m.remove("SaleDataSet.Page");
		   }
	   }
   }
   
   public static void legalWilsonRO(ResultMap m, long searchId) throws Exception {
	   TNWilsonRO.parseLegal(m, searchId);	   	   	   
   	}
   
   public static void partyNamesTNWilsonRO(ResultMap m, long searchId) throws Exception {
	   TNWilsonRO.parseName(m, searchId);
   }
   
   public static void partyNamesTNDavidsonRO(ResultMap m, long searchId) throws Exception {
	   TNDavidsonRO.parseName(m, searchId);
   }
   
   public static void partyNamesTNKnoxRO(ResultMap m, long searchId) throws Exception {
	   TNKnoxRO.parseName(m, searchId);
   }
   
   public static void partyNamesTNHamiltonRO(ResultMap m, long searchId) throws Exception {
	   TNHamiltonRO.parseName(m, searchId);
   }
   
   public static void parseNamesTNWilliamsonRO(ResultMap m, long searchId) throws Exception {
	   TNWilliamsonRO.parseName(m, searchId);
   }
   
   /**
    * Get owner from search attributes and put in SaleDataSet.Grantor
    * @param m
    * @param searchId
    * @throws Exception
    */
   public static void getGrantorFromSa(ResultMap m,long searchId) throws Exception {
	   try{
		   Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		   SearchAttributes sa = search.getSa();	   
		   String owner = sa.getAtribute(SearchAttributes.OWNER_LFM_NAME);	
		   if(!StringUtils.isEmpty(owner)){
			   m.put("SaleDataSet.Grantor", owner);
		   }
	   }catch(RuntimeException e){
		   logger.error(e);
	   }
   }
   
   /**
    * Construct pin by adding block, lot, sublot
    * @param m
    * @param searchId
    * @throws Exception
    */
   public static void pinKYJeffersonMS(ResultMap m,long searchId) throws Exception {
	   
	  String block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock"); 
	  String lot = (String)m.get("tmpLot");
	  String subLot = (String)m.get("tmpSubLot");
	  
	  if(!StringUtils.isEmpty(block) && !StringUtils.isEmpty(lot) && !StringUtils.isEmpty(subLot)){
		  String pin = block + lot + subLot;
		  m.put("PropertyIdentificationSet.ParcelID", pin);
	  }

   }        
      
	public static void crossRefTokenizeOHFranklinRO(ResultMap m, long searchId) throws Exception
	{
		//String crsRef = (String)m.get("CrossRefSet.InstrumentNumber");
		ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
		String crossRefInst = "";
		String crossRefBook = "";
		String crossRefPage = "";
		if (crossRefs != null){
			String[][] body = crossRefs.body;
			for (int i = 0; i < body.length; i++){
				crossRefInst = body[i][1];
				crossRefBook = body[i][1];
				crossRefPage = body[i][1];
				
				if (crossRefInst.matches("\\d{4,5}[A-Z]\\d{2}"))
				{
					crossRefBook = crossRefBook.replaceFirst("(\\d{4,5})[A-Z]\\d{2}","$1");
					crossRefPage = crossRefPage.replaceFirst("\\d{4,5}([A-Z]\\d{2})","$1");
					body[i][2] = crossRefBook;
					body[i][3] = crossRefPage;
					body[i][1] = "";
				}
				if ("/".equals(body[i][2]) && "/".equals(body[i][3])){
					body[i][2] = "";
					body[i][3] = "";
				}
				if (crossRefInst.length() == 15 && body[i].length == 7){
					String date = crossRefInst.substring(0, 8);
					if (org.apache.commons.lang.StringUtils.isNotEmpty(date) && date.length() == 8){
						body[i][4] = date.substring(4, 6);
						body[i][5] = date.substring(6);
						body[i][6] = date.substring(0, 4);
					}
				}
			}
		}
	}
	
	public static void cleanLegalTNWilliamsonRO(ResultMap m, long searchId)
		throws Exception {
		
		ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
		if (pis != null) {
            int len = pis.getLength();
    		
            for (int i=0; i<len; i++) {
            	String subd = pis.body[i][0];
                //subdiv name cleanup
                if (!StringUtils.isEmpty(subd) ) {
                	subd = subd.replaceAll("(?is)\\bAT\\s+.*", "");
                    pis.body[i][0] = subd.trim();
                }
                
                
            }
            
            m.put("PropertyIdentificationSet", pis);
		}
	}
	
	public static void extractLegalDescriptionTNWilliamsonRO(ResultMap m, long searchId) throws Exception {
	
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		
		// extract phase
		Pattern p = Pattern.compile("\\bPHASE (\\d+)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1));	
		}
		
		// extract building
		p = Pattern.compile("\\b(?:BUILDING|BLDG) ([A-Z]\\d*|\\d+[A-Z]?)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()){
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));	
		}
		
		// extract lot, block and section if not already found in Subdivisions table
		ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
		boolean hasLot = false, hasBlk = false, hasSec = false;
		if (pis != null && pis.body != null) {
			for (int i=0; i<pis.body.length; i++){
				if(pis.body[i][1].length() != 0){
					hasLot = true;					
				}
				if(pis.body[i][2].length() != 0){
					hasBlk = true;					
				}
				if(pis.body[i][3].length() != 0){
					hasSec = true;					
				}
				if (hasLot && hasBlk && hasSec){
					break;
				}
			}
		}		
		if (!hasLot) {			
			String lot = "";
			p = Pattern.compile("\\bLOTS? (\\d+(?:[A-Z])?(?:[\\s&\\(\\)-]+\\d+)*(?:[A-Z])?)\\b");
			ma.reset();
			ma.usePattern(p);
			while (ma.find()){
				lot = lot + " " + ma.group(1).replaceAll("\\s*[\\(\\)&]\\s*", " ");
			}
			lot = lot.trim();
			if (lot.length() != 0){
				lot = LegalDescription.cleanValues(lot, true, true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);	
			}        
		}
		
		if (!hasSec) {			
			p = Pattern.compile("\\bSEC(?:TION)? (\\d+(?:[\\s&,-]?[A-Z]?)|[A-Z]\\d*)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()){
				m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1).replaceAll("[&,]", ""));
			}
		}
		
		if (!hasBlk) {			
			p = Pattern.compile("\\bBL(?:OC)?K ([A-Z]|\\d+)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()){
				m.put("PropertyIdentificationSet.SubdivisionBlock", ma.group(1));
			}
		}
		
	}
	//for Bug 2589
	public static void extractLegalSetFromGrantorGranteeTNWilliamsonRO(ResultMap m, long searchId) throws Exception {
		String lgl = (String) m.get("SaleDataSet.Grantee");
		String lglg = (String) m.get("SaleDataSet.Grantor");

		// extract section from Grantee or Grantor - if not already extracted from legal description
		String secFromInfSet = (String)  m.get("PropertyIdentificationSet.SubdivisionSection");
		if (StringUtils.isEmpty(secFromInfSet)){
		   String section = "";
		   Pattern p = Pattern.compile("\\b(SECTION|SEC)\\s+([-&,\\w]+)\\b");
		   Matcher ma = p.matcher(lgl);
		   if (ma.find())
			   section = ma.group(2);
		   else {
			   ma = p.matcher(lglg);
			   if (ma.find())
				   section = ma.group(2);
		   }
		   if (section.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionSection", section);
		   }		   
		}
	}

	/**
	 * Extract section name from name
	 * @param pr
	 * @param name
	 */
    public static void parseLegalFromGrantorShelbyRO(ParsedResponse pr, String name){
    	
    	// extract section from name
    	String section = StringUtils.extractParameter(name, "\\b(?:SEC|SECTION) ([A-Z0-9])\\b");
    	if(StringUtils.isEmpty(section)){
    		return;
    	}
    	
    	// isolate or create PIS
    	PropertyIdentificationSet pis;
    	if(pr.getPropertyIdentificationSetCount() == 0){
    		pis = new PropertyIdentificationSet();
    		pr.addPropertyIdentificationSet(pis);
    	} else {
    		pis = pr.getPropertyIdentificationSet(0);
    	}
    	
    	// set section attribute
    	pis.setAtribute("SubdivisionSection", section);
    	
    } 
    
    //B1695, comment #5
    public static void parseCourtOHFranklinCO(ResultMap m, long searchId) throws Exception {
    	String caseNumber = (String) m.get("tmpCaseNumber");
    	Pattern p = Pattern.compile("(?is)\\d+\\s*-\\s*(..)\\s*-\\s*\\w+");
    	Matcher ma = null;
    	if (caseNumber != null) {
    		m.put("CourtDocumentIdentificationSet.CaseNumber", caseNumber);
    		m.put("SaleDataSet.InstrumentNumber", caseNumber);
    		ma = p.matcher(caseNumber);
    		if (ma.find()) {
    			m.put("CourtDocumentIdentificationSet.CaseType", ma.group(1));
    			m.put("SaleDataSet.DocumentType", ma.group(1));
    		}
    	}
    	
    	String fillingDate = (String) m.get("tmpDateFilled");
    	if (fillingDate != null) {
    		m.put("CourtDocumentIdentificationSet.FillingDate", fillingDate);
    		m.put("SaleDataSet.RecordedDate", fillingDate);
    	}
    	String splitString = " / ";
    	String saleDataSetGrantor = (String) m.get("SaleDataSet.Grantor");
    	if (StringUtils.isNotEmpty(saleDataSetGrantor)){
    		m.put("GrantorSet", parseGrantorGranteeSetOHFranklinCO(saleDataSetGrantor, splitString));
    	}
    	String grantor = (String) m.get("tmpAppellant");
    	if (grantor != null) {
    		m.put("SaleDataSet.Grantor", grantor);
    		if (m.get("GrantorSet") == null){
    			m.put("GrantorSet", StringFormats.parseGrantorGranteeSetFromString(grantor, splitString));
    		}
    	}
    	
    	String saleDataSetGrantee = (String) m.get("SaleDataSet.Grantee");
    	if (StringUtils.isNotEmpty(saleDataSetGrantee)){
    		m.put("GranteeSet", parseGrantorGranteeSetOHFranklinCO(saleDataSetGrantee, splitString));
    	}
    	String grantee = (String) m.get("tmpAppellee");
    	if (grantee != null) {
    		m.put("SaleDataSet.Grantee", grantee);
    		if (m.get("GranteeSet") == null) {
    			m.put("GranteeSet", StringFormats.parseGrantorGranteeSetFromString(grantee, splitString));
    		}
    	}
    }
    
    public static ResultTable parseGrantorGranteeSetOHFranklinCO(String names, String splitString) throws Exception {
    	
    	ResultTable rt = new ResultTable();
    	
    	String[] header = {"all", "OwnerFirstName", "OwnerMiddleName", "OwnerLastName", "SpouseFirstName", "SpouseMiddleName", "SpouseLastName"};
    	Map<String,String[]> map = new HashMap<String,String[]>();
    	map.put("all", new String[]{"all", ""});
		map.put("OwnerFirstName", new String[]{"OwnerFirstName", ""});
		map.put("OwnerMiddleName", new String[]{"OwnerMiddleName", ""});
		map.put("OwnerLastName", new String[]{"OwnerLastName", ""});
 		map.put("SpouseFirstName", new String[]{"SpouseFirstName", ""});
 		map.put("SpouseMiddleName", new String[]{"SpouseMiddleName", ""});
 		map.put("SpouseLastName", new String[]{"SpouseLastName", ""});
    	
 		String[] name = names.split(splitString);
    	String[][] body = new String[name.length][header.length];
    	
    	for (int i = 0; i < name.length; i++){
    		name[i] = name[i].replaceAll("(?is)[\\(\\)]+", "");
    		name[i] = name[i].replaceAll("(?is)(MS\\s+)?EXEC\\b", "").replaceAll("(?is)\\bDECD&?", "");
    		name[i] = name[i].replaceAll("(?is)[\\(\\)]+", "");
//    		if (name[i].trim().matches("(?is)\\A[A-Z]+\\s+[A-Z]\\s+[A-Z].*")){
    			body[i][0] = name[i];
				body[i][1] = StringFormats.OwnerFirstNameDesotoRO(name[i]);
				body[i][2] = StringFormats.OwnerMiddleNameDesotoRO(name[i]);
				body[i][3] = StringFormats.OwnerLastNameDesotoRO(name[i]);
				body[i][4] = StringFormats.SpouseFirstNameDesotoRO(name[i]);
				body[i][5] = StringFormats.SpouseMiddleNameDesotoRO(name[i]);
				body[i][6] = StringFormats.SpouseLastNameDesotoRO(name[i]);
//    		} else {
//    			body[i][0] = name[i];
//    			body[i][1] = StringFormats.OwnerFirstNameNashville(name[i]);
//    			body[i][2] = StringFormats.OwnerMiddleNameNashville(name[i]);
//    			body[i][3] = StringFormats.OwnerLastNameNashville(name[i]);
//    			body[i][4] = StringFormats.SpouseFirstNameNashville(name[i]);
//    			body[i][5] = StringFormats.SpouseMiddleNameNashville(name[i]);
//    			body[i][6] = StringFormats.SpouseLastNameNashville(name[i]);
//    		}
    	}
    	rt.setReadWrite();
    	rt.setHead(header);
    	rt.setMap(map);
    	rt.setBody(body);
    	rt.setReadOnly();
    	return rt;
    }
    
    public static void parseNamesOHFranklinCO(ResultMap m, long searchId) throws Exception {
    	String description = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
    	m.remove(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
    	if (!StringUtils.isEmpty(description)) {
    		Matcher ma = Pattern.compile("(?is)\\bDEFENDANT\\s+ALIAS\\s*:\\s*([^;]+);").matcher(description + ";");
    		if (ma.find()) {
    			String alias = ma.group(1);
    			String names[] = {"", "", "", "", "", ""};
				ArrayList<List> list = new ArrayList<List>();
				names = StringFormats.parseNameDesotoRO(alias, true);
				List<String> line = new ArrayList<String>();
				line.add(alias);
				for (int i=0;i<names.length;i++) {
					line.add(names[i]);
				}
				list.add(line);
				String[] header = {"all",
				           PropertyIdentificationSetKey.OWNER_FIRST_NAME.getShortKeyName(),
				           PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getShortKeyName(),
						   PropertyIdentificationSetKey.OWNER_LAST_NAME.getShortKeyName(),
		           		   PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getShortKeyName(),
		           		   PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getShortKeyName(),
		           		   PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getShortKeyName()};
				ResultTable rt = GenericFunctions2.createResultTable(list, header);
				
				ResultTable existingRT = (ResultTable)m.get("GranteeSet");
				if (existingRT!=null) {
					try {
						rt = ResultTable.joinVertical(rt, existingRT, true);
						m.put("GranteeSet", rt);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					m.put("GranteeSet", rt);
				}
				m.remove(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName());
				m.remove(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName());
				m.remove(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName());
				m.remove(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName());
				m.remove(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName());
				m.remove(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName());
				
				String grantee = (String)m.get(SaleDataSetKey.GRANTEE.getKeyName());
				if (StringUtils.isEmpty(grantee)) {
					grantee = alias;
				} else {
					grantee += " / " + alias;
				}
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);
		   }
	   	}
    }
    
    public static void newParsingTNGenericUsTitleSearchRO(ResultMap m, long searchId) throws Exception {
    	TNGenericUsTitleSearchRO.newParsingTNGenericUsTitleSearchRO(m, searchId);
    	TNGenericUsTitleSearchRO.parseCrossRefSet(m, searchId);
    }
    
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
    
    public static void parseGrantorGranteeSetTNRO(ResultMap m, long searchId) throws Exception {
    	TNGenericUsTitleSearchRO.parseGrantorGranteeSetTNRO(m, searchId);
    }
    
    public static void sdsTNGenericUsTitleSearch(ResultMap m, long searchId) throws Exception {
    	TNGenericUsTitleSearchRO.sdsTNGenericUsTitleSearch(m, searchId);
    }
    
    public static void parseGrantorGranteeSetOrbit(ResultMap m, long searchId) throws Exception {
    	String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
    	String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
    	if("KSWyandotte".equalsIgnoreCase(crtState+crtCounty)){
    		KSWyandotteOR.parseGrantorGranteeSetOR(m, searchId);
    		return;
    	}
    	if("MOPlatte".equalsIgnoreCase(crtState+crtCounty)){
    		GenericOrbit.parseNameMOPlatteOR(m, searchId);
    		return;
    	}
    	GenericParsingOR.parseGrantorGranteeSetOR(m, searchId);
    }
    
    public static void correctBookPage (ResultMap m, long searchId) throws Exception {
    	GenericParsingOR.correctBookPage(m, searchId);
    }

    // Task 7877
    public static void addBookPrefix(ResultMap m,long searchId) throws Exception {
    	String instNo = (String) m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());
    	String book = (String) m.get(SaleDataSetKey.BOOK.getKeyName());
    	
    	if(StringUtils.isNotEmpty(instNo) && StringUtils.isNotEmpty(book)) {
    		if(instNo.matches("\\d{4}[A-Z]\\d+")) {
    			String prefix = instNo.replaceAll("\\d+", "");
    			m.put(SaleDataSetKey.BOOK.getKeyName(), prefix + book);
    		}
    	}
    	
    	ResultTable crTable = (ResultTable) m.get("CrossRefSet");
		if(crTable != null) {
			String[][] body = crTable.getBody();
			
			for(String[] crossRef : body) {
				instNo = crossRef[1];
				book = crossRef[4];
				
				if(StringUtils.isNotEmpty(instNo) && StringUtils.isNotEmpty(book)) {
					if(instNo.matches("\\d{4}[A-Z]\\d+")) {
						String prefix = instNo.replaceAll("\\d+", "");
						crossRef[4] = prefix + book;
					}
				}
			}
			
			boolean readOnly = crTable.isReadOnly();
			crTable.setReadOnly(false);
			crTable.setBody(body);
			crTable.setReadOnly(readOnly);
			
			m.put("CrossRefSet", crTable);
		}
    }
}
