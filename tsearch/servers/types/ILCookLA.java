/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_LABEL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;
import static ro.cst.tsearch.utils.XmlUtils.applyTransformation;
import static ro.cst.tsearch.utils.XmlUtils.getChildren;
import static ro.cst.tsearch.utils.XmlUtils.getNodeValue;
import static ro.cst.tsearch.utils.XmlUtils.parseXml;
import static ro.cst.tsearch.utils.XmlUtils.xpathQuery;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface;
import ro.cst.tsearch.connection.dasl.DaslSite;
import ro.cst.tsearch.connection.dasl.ILCookLAQueryBuilder;
import ro.cst.tsearch.dasl.Tp3Record;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ILCookLACombinedFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIteratorILCook;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author radu bacrau
 */
public class ILCookLA extends TSServer implements TSServerROLikeI { 

	public static final long serialVersionUID = 10000000L;
	
	private static final int COMBINED_MODULE_IDX  = 3;
	private static final int COMBINED_MODULE_PID  = 103;
	
    private static final String CLASSES_FOLDER  = BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + File.separator;
    private static final String RESOURCE_FOLDER = CLASSES_FOLDER + "resource" + File.separator + "DASL" + File.separator + "ILCook" + File.separator; 
    private static final String RULES_FOLDER    = CLASSES_FOLDER + "rules";
        
    private static final String TITLEDOC_STYLESHEET_FILE_NAME = RESOURCE_FOLDER + "titledoc_stylesheet.xsl";       
	
    private static final String GI_RULES_FILENAME = RULES_FOLDER + File.separator + "ILCookLA-Title.xml";
	private static final String PI_RULES_FILENAME = RULES_FOLDER + File.separator + "ILCookLA-Title.xml";
	private static final String LC_RULES_FILENAME = RULES_FOLDER + File.separator + "ILCookLA-Title.xml";
	
    private static final int PI_TYPE = 10;
    private static final int GI_TYPE = 20;
    private static final int LC_TYPE = 30;
    private static final int TX_TYPE = 40;

    static Pattern imageLinkPattern = Pattern.compile("Link=look_for_dl_image&documentNumber=([^&]+)&instrumentType=([^&]+)&dateFiled=([^&]+)");

	/**
	 * Parsing rules
	 */
	protected transient Document piRules = null;
	protected transient Document giRules = null;
	protected transient Document lcRules = null;
	
	/**
	 * XSLT Stylesheets 
	 */
	protected transient String titleStyle = null; 

	/**
	 * load rules from disk
	 */
	private void loadRules(){
		
		// do not load twice
		if(piRules != null){ 
			return; 
		}
		
		// load
		try{
			piRules = XMLUtils.read(new File(PI_RULES_FILENAME), RULES_FOLDER);
			giRules = XMLUtils.read(new File(GI_RULES_FILENAME), RULES_FOLDER);	  
			lcRules = XMLUtils.read(new File(LC_RULES_FILENAME), RULES_FOLDER);
			titleStyle = FileUtils.readTextFile(TITLEDOC_STYLESHEET_FILE_NAME);  			
		}catch(Exception e){
			throw new RuntimeException("Error reading rules!");
		}		
	}
	
	/**
	 * Create default values for start dates and end dates
	 * @return
	 */
	private String [] createDates(){		
		String startDateStr = "01/01/1986", endDateStr = "", startDateForUpdate = "";
        try{
            MOClayRO.sdf.applyPattern("MMM d, yyyy");
            Date start = MOClayRO.sdf.parse(getSearchAttribute(SearchAttributes.FROMDATE));	            
            Date end = MOClayRO.sdf.parse(getSearchAttribute(SearchAttributes.TODATE));	            
            MOClayRO.sdf.applyPattern("MM/dd/yyyy");
            startDateStr = MOClayRO.sdf.format(start);
            endDateStr = MOClayRO.sdf.format(end);
            Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
			startDateForUpdate = MOClayRO.sdf.format(cal.getTime());
        }catch (ParseException e){}		
        return new String[]{startDateStr,endDateStr, startDateForUpdate};
	}
	
	/**
	 * Sets the keys for searching by person name or company name for one of the owners or buyers
	 * @param sim server module
	 * @param keyPrefix prefix of SearchAttributes key. Can be OWNER, OWNER_SPOUSE, BUYER, BUYER_SPOUSE
	 * @param fIndex index of first name function
	 * @param mIndex index of middle name function
	 * @param lIndex index of last name function
	 * @param cIndex index of company name function
	 */
    private void setPersonKeys(TSServerInfoModule sim, String keyPrefix, int fIndex, int mIndex, int lIndex, int cIndex, boolean isParentSite){

    	String keyFirst = keyPrefix + "_FNAME";
    	String keyMiddle = keyPrefix + "_MNAME";
    	String keyLast = keyPrefix + "_LNAME";
    			
		String first = getSearchAttribute(keyFirst);
		String middle = getSearchAttribute(keyMiddle);
		String last = getSearchAttribute(keyLast);
		
		boolean emptyLast = StringUtils.isEmpty(last);
		boolean emptyFirst = StringUtils.isEmpty(first);
		boolean emptyMiddle = StringUtils.isEmpty(middle);
	
		if(!emptyLast) {			
			if(!emptyFirst ){
				sim.getFunction(fIndex).setSaKey(keyFirst);
				sim.getFunction(lIndex).setSaKey(keyLast);
				if(isParentSite() && !emptyMiddle){ sim.getFunction(mIndex).setSaKey(keyMiddle);}
			} else {
				sim.getFunction(cIndex).setSaKey(keyLast);
			}
		}
	}

	/**
	 * Sets the keys for owner and buyer searches
	 * @param sim
	 */
	private void setOwnerBuyerKeys(TSServerInfoModule sim, boolean isParentSite){		
		
		setPersonKeys(sim, "OWNER",         1,  2,  3, 13, isParentSite);
//		setPersonKeys(sim, "OWNER_SPOUSE",  4,  5,  6, 14, isParentSite);
		setPersonKeys(sim, "BUYER",         7,  8,  9, 15, isParentSite);
//		setPersonKeys(sim, "BUYER_SPOUSE", 10, 11, 12, 16, isParentSite);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo si = new TSServerInfo(1);
		si.setServerAddress("");
		si.setServerIP("");
		si.setServerLink("");
		
		// Combined Search
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(COMBINED_MODULE_IDX, 72);
			sim.setName("CombinedSearch");

			sim.setParserID(COMBINED_MODULE_PID);

			PageZone pz = new PageZone("CombinedSearch", "Combined Party/Property Search", ORIENTATION_HORIZONTAL, null, 850, 50, PIXELS , true);

	        String [] dates = createDates();
	        String startDateStr = dates[0];
	        String endDateStr = dates[1];
	        
			try{				
	            HTMLControl 
	            
	            label1 = new HTMLControl(HTML_LABEL,      1, 1,  1,  1, 15, sim.getFunction(0), "LabelParty",  "<left><b>Party Search</b></left>",   "", searchId),
	            
	            fname1 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2,  2, 20, sim.getFunction(1),  "FirstName1",  "First Name",   "", searchId),
	            mname1 = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2,  2, 15, sim.getFunction(2),  "MiddleName1", "Middle",       "", searchId),
	            lname1 = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  2,  2, 20, sim.getFunction(3),  "LastName1",   "Last Name",    "", searchId),
	                                              
	            
	            fname2 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3,  3, 20, sim.getFunction(4),  "FirstName2",  "First Name",   "", searchId),
	            mname2 = new HTMLControl(HTML_TEXT_FIELD, 2, 3,  3,  3, 15, sim.getFunction(5),  "MiddleName2", "Middle",       "", searchId),
	            lname2 = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  3,  3, 20, sim.getFunction(6),  "LastName2",   "Last Name",    "", searchId),
                                                                   
	            fname3 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4,  4, 20, sim.getFunction(7),  "FirstName3",  "First Name",   "", searchId),
	            mname3 = new HTMLControl(HTML_TEXT_FIELD, 2, 3,  4,  4, 15, sim.getFunction(8),  "MiddleName3", "Middle",       "", searchId),
	            lname3 = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  4,  4, 20, sim.getFunction(9),  "LastName3",   "Last Name",    "", searchId),
	                                                                
	            fname4 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  5,  5, 20, sim.getFunction(10), "FirstName4",  "First Name",   "", searchId),
	            mname4 = new HTMLControl(HTML_TEXT_FIELD, 2, 3,  5,  5, 15, sim.getFunction(11), "MiddleName4", "Middle",       "", searchId),
	            lname4 = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  5,  5, 20, sim.getFunction(12), "LastName4",   "Last Name",    "", searchId),
                                                                   
	            bname1 = new HTMLControl(HTML_TEXT_FIELD, 1, 3,  6,  6, 40, sim.getFunction(13), "FullName1",   "Buss Name",    "", searchId),
	            bname2 = new HTMLControl(HTML_TEXT_FIELD, 1, 3,  7,  7, 40, sim.getFunction(14), "FullName2",   "Buss Name",    "", searchId),
	            bname3 = new HTMLControl(HTML_TEXT_FIELD, 1, 3,  8,  8, 40, sim.getFunction(15), "FullName3",   "Buss Name",    "", searchId),
	            bname4 = new HTMLControl(HTML_TEXT_FIELD, 1, 3,  9,  9, 40, sim.getFunction(16), "FullName4",   "Buss Name",    "", searchId),
                                                                   
	            tssno1 = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 10, 10, 15, sim.getFunction(17), "TPartyID1",    "Trust No",     "", searchId),
	            tsnam1 = new HTMLControl(HTML_TEXT_FIELD, 2, 3, 10, 10, 25, sim.getFunction(18), "TFullName1",   "Trust Name",   "", searchId),
	            tsdat1 = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 10, 10, 10, sim.getFunction(19), "TDate1",       "Trust Date",   "", searchId),
	                                                                
	            tssno2 = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 11, 11, 15, sim.getFunction(20), "TPartyID2",    "Trust No",     "", searchId),
	            tsnam2 = new HTMLControl(HTML_TEXT_FIELD, 2, 3, 11, 11, 25, sim.getFunction(21), "TFullName2",  "Trust Name",    "", searchId),
	            tsdat2 = new HTMLControl(HTML_TEXT_FIELD, 4, 5, 11, 11, 10, sim.getFunction(22), "TDate2",       "Trust Date",   "", searchId),
                                                                   
	            tssno3 = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 12, 12, 15, sim.getFunction(23), "TPartyID3",    "Trust No",     "", searchId),
	            tsnam3 = new HTMLControl(HTML_TEXT_FIELD, 2, 3, 12, 12, 25, sim.getFunction(24), "TFullName3",  "Trust Name",    "", searchId),
	            tsdat3 = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 12, 12, 10, sim.getFunction(25), "TDate3",       "Trust Date",   "", searchId),
	            
	            tssno4 = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 13, 13, 15, sim.getFunction(26), "TPartyID4",    "Trust No",     "", searchId),
	            tsnam4 = new HTMLControl(HTML_TEXT_FIELD, 2, 3, 13, 13, 25, sim.getFunction(27), "TFullName4",  "Trust Name",    "", searchId),
	            tsdat4 = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 13, 13, 10, sim.getFunction(28), "TDate4",       "Trust Date",   "", searchId),
	                                                                
	            fd1    = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 14, 14, 10, sim.getFunction(29), "FromDateParty",     "From Date",   startDateStr, searchId),
	            td1    = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 15, 15, 10, sim.getFunction(30), "ToDateParty",       "To Date",     endDateStr, searchId),
	            
	            label2 = new HTMLControl(HTML_LABEL,      1, 1, 16,16, 15,  sim.getFunction(31), "LabelParty2",  "<left><b>Property Search</b></left>",   "", searchId),
	            
	            // apn
	            apn1   = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 17, 17, 25, sim.getFunction(32),  "APN1",           "PIN",        "", searchId),
	            apn2   = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 18, 18, 25, sim.getFunction(33),  "APN2",           "PIN",        "", searchId),
	            apn3   = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 19, 19, 25, sim.getFunction(34),  "APN3",           "PIN",        "", searchId),
	            apn4   = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 20, 20, 25, sim.getFunction(35),  "APN4",           "PIN",        "", searchId),
	            	                                                                                                                   
	            // legal - a                                        
	            rng1   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 21, 21,  8, sim.getFunction(36), "Range1",         "Range",      "", searchId),
	            sec1   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 21, 21,  8, sim.getFunction(37), "Sections1",      "Sections",   "", searchId),	            
	            twn1   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 21, 21,  4, sim.getFunction(38), "Township1",      "Township",   "", searchId),	            	            
	            qtr1   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 21, 21,  8, sim.getFunction(39), "Quarters1",      "Quarters",   "", searchId),
	            
	            plt1   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 22, 22, 20, sim.getFunction(40), "Plat1",          "Plat",       "", searchId),
	            blk1   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 22, 22,  4, sim.getFunction(41), "Block1",         "Block",      "", searchId),	            
	            lot1   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 22, 22,  4, sim.getFunction(42), "Lot1",           "Lot",        "", searchId),	            
	            unt1   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 22, 22,  8, sim.getFunction(43), "Units1",         "Units",      "", searchId),
	            
	            rng2   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 23, 23,  8, sim.getFunction(44), "Range2",         "Range",      "", searchId),
	            sec2   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 23, 23,  8, sim.getFunction(45), "Sections2",      "Sections",   "", searchId),	            
	            twn2   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 23, 23,  4, sim.getFunction(46), "Township2",      "Township",   "", searchId),	            	            
	            qtr2   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 23, 23,  8, sim.getFunction(47), "Quarters2",      "Quarters",   "", searchId),
                   
	            plt2   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 24, 24, 20, sim.getFunction(48), "Plat2",          "Plat",       "", searchId),
	            blk2   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 24, 24,  4, sim.getFunction(49), "Block2",         "Block",      "", searchId),	            
	            lot2   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 24, 24,  4, sim.getFunction(50), "Lot2",           "Lot",        "", searchId),	            
	            unt2   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 24, 24,  8, sim.getFunction(51), "Units2",         "Units",      "", searchId),
	            
	            rng3   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 25, 25,  8, sim.getFunction(52), "Range3",         "Range",      "", searchId),
	            sec3   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 25, 25,  8, sim.getFunction(53), "Sections3",      "Sections",   "", searchId),	            
	            twn3   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 25, 25,  4, sim.getFunction(54), "Township3",      "Township",   "", searchId),	            
	            qtr3   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 25, 25,  8, sim.getFunction(55), "Quarters3",      "Quarters",   "", searchId),
                                                                   
	            plt3   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 26, 26, 20, sim.getFunction(56), "Plat3",          "Plat",       "", searchId),
	            blk3   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 26, 26,  4, sim.getFunction(57), "Block3",         "Block",      "", searchId),	            
	            lot3   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 26, 26,  4, sim.getFunction(58), "Lot3",           "Lot",        "", searchId),	            
	            unt3   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 26, 26,  8, sim.getFunction(59), "Units3",         "Units",      "", searchId),
	   
	            rng4   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 27, 27,  8, sim.getFunction(60), "Range4",         "Range",      "", searchId),
	            sec4   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 27, 27,  8, sim.getFunction(61), "Sections4",      "Sections",   "", searchId),	            
	            twn4   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 27, 27,  4, sim.getFunction(62), "Township4",      "Township",   "", searchId),	            	            
	            qtr4   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 27, 27,  8, sim.getFunction(63), "Quarters4",      "Quarters",   "", searchId),                                                                  

	            plt4   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 28, 28, 20, sim.getFunction(64), "Plat4",          "Plat",       "", searchId),
	            blk4   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 28, 28,  4, sim.getFunction(65), "Block4",         "Block",      "", searchId),	            	            
	            lot4   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 28, 28,  4, sim.getFunction(66), "Lot4",           "Lot",        "", searchId),	            
	            unt4   = new HTMLControl(HTML_TEXT_FIELD, 4, 4, 28, 28,  8, sim.getFunction(67), "Units4",         "Units",      "", searchId),
                                                                   
	            fd2    = new HTMLControl(HTML_TEXT_FIELD, 1, 4, 29, 29, 10, sim.getFunction(68), "FromDateProperty",      "From Date",  startDateStr, searchId),
	            td2    = new HTMLControl(HTML_TEXT_FIELD, 1, 4, 30, 30, 10, sim.getFunction(69), "ToDateProperty",        "To Date",    endDateStr, searchId),
                
	            label3 = new HTMLControl(HTML_LABEL,      1, 1, 31, 31, 15, sim.getFunction(70), "LabelCommon",  "<left><b>Common Params</b></left>",   "", searchId),
	            doctype= new HTMLControl(HTML_TEXT_FIELD, 1, 4, 32, 32, 35, sim.getFunction(71), "DocTypes",        "Doc Types",    "", searchId);
	            
	            setOwnerBuyerKeys(sim, true);
	            
	            sim.getFunction(32).setSaKey(SearchAttributes.LD_PARCELNO);
	            	            
	            sim.getFunction(36).setSaKey(SearchAttributes.LD_SUBDIV_RNG);
	            sim.getFunction(37).setSaKey(SearchAttributes.LD_SUBDIV_SEC);
	            sim.getFunction(38).setSaKey(SearchAttributes.LD_SUBDIV_TWN);
	            
	            sim.getFunction(40).setSaKey(SearchAttributes.LD_INSTRNO);
	            sim.getFunction(41).setSaKey(SearchAttributes.LD_SUBDIV_BLOCK);
	            sim.getFunction(42).setSaKey(SearchAttributes.LD_LOTNO);
	            sim.getFunction(43).setSaKey(SearchAttributes.LD_SUBDIV_UNIT);	           

	            tsdat1.setFieldNote("eg. 01/24/1990");
	            tsdat2.setFieldNote("eg. 01/24/1990");
	            tsdat3.setFieldNote("eg. 01/24/1990");
	            tsdat4.setFieldNote("eg. 01/24/1990");
	            
	            apn1.setFieldNote("eg. 32-29-103-014-0000 or 32291030140000");
	            apn2.setFieldNote("eg. 32-29-103-014-0000 or 32291030140000");
	            apn3.setFieldNote("eg. 32-29-103-014-0000 or 32291030140000");
	            apn4.setFieldNote("eg. 32-29-103-014-0000 or 32291030140000");
	            
	            unt1.setFieldNote("eg. 1,2");
	            unt2.setFieldNote("eg. 1,2");
	            unt3.setFieldNote("eg. 1,2");
	            unt4.setFieldNote("eg. 1,2");
	            
	            sec1.setFieldNote("eg. 15,16");
	            sec2.setFieldNote("eg. 15,16");
	            sec3.setFieldNote("eg. 15,16");
	            sec4.setFieldNote("eg. 15,16");
	            
	            qtr1.setFieldNote("eg. NE,SW");
	            qtr2.setFieldNote("eg. NE,SW");
	            qtr3.setFieldNote("eg. NE,SW");
	            qtr4.setFieldNote("eg. NE,SW");

	            fd1.setFieldNote("eg. 01/24/1990. From Date is ignored for GI searches");	            
	            td1.setFieldNote("eg. 01/24/2000. To Date is ignored for GI searches");
	            
	            fd2.setFieldNote("eg. 01/24/1990. From Date is ignored for GI searches");
	            td2.setFieldNote("eg. 01/24/2000. To Date is ignored for GI searches");
	            
	            doctype.setFieldNote("e.g. D,W,BK,DV");
	            
	            setRequiredCriticalMulti(true);
	            
	            setJustifyFieldMulti(
            		false,  
            		label1,
            		fname1, mname1, lname1,
            		fname2, mname2, lname2,
            		fname3, mname3, lname3,
            		fname4, mname4, lname4,
            		bname1, 
            		bname2,
            		bname3,
            		bname4,
            		tssno1,tsdat1,tsnam1,
            		tssno2,tsdat2,tsnam2,
            		tssno3,tsdat3,tsnam3,
            		tssno4,tsdat4,tsnam4,
            		fd1,td1,
            		label2,
            		apn1,
            		apn2,
            		apn3,
            		apn4,
            		sec1, twn1, rng1, qtr1,
            		sec2, twn2, rng2, qtr2,
            		sec3, twn3, rng3, qtr3,
            		sec4, twn4, rng4, qtr4,
            		lot1, blk1, unt1, plt1,
            		lot2, blk2, unt2, plt2,
            		lot1, blk1, unt1, plt1,
            		lot2, blk2, unt2, plt2,
            		lot3, blk3, unt3, plt3,
            		lot4, blk4, unt4, plt4,
            		fd2, td2,
            		label3,
            		doctype
	            );
            
	            pz.addHTMLObjectMulti(
	            	label1,
            		fname1, mname1, lname1,
            		fname2, mname2, lname2,
            		fname3, mname3, lname3,
            		fname4, mname4, lname4,
            		bname1, 
            		bname2,
            		bname3,
            		bname4,   
            		tssno1,tsdat1,tsnam1,
            		tssno2,tsdat2,tsnam2,
            		tssno3,tsdat3,tsnam3,
            		tssno4,tsdat4,tsnam4,            		
            		fd1, td1,
            		label2,
            		apn1,
            		apn2,
            		apn3,
            		apn4,
            		sec1, twn1, rng1, qtr1,
            		sec2, twn2, rng2, qtr2,
            		sec3, twn3, rng3, qtr3,
            		sec4, twn4, rng4, qtr4,
            		lot1, blk1, unt1, plt1,
            		lot2, blk2, unt2, plt2,
            		lot1, blk1, unt1, plt1,
            		lot2, blk2, unt2, plt2,
            		lot3, blk3, unt3, plt3,
            		lot4, blk4, unt4, plt4,
            		fd2, td2,
            		label3,
            		doctype
            	);

			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}
		
		{

            TSServerInfoModule simTmp = si.ActivateModule(TSServerInfo.INSTR_NO_MODULE_IDX, 1);
            simTmp.setName("InstrumentSearch");
            simTmp.setParserID(ID_SEARCH_BY_INSTRUMENT_NO);

            try
            {
    			PageZone pz = new PageZone("InstrumentSearch", "Instrument Number (Fake) Search", ORIENTATION_HORIZONTAL, null, 850, 50, PIXELS , false);
    			pz.setVisible(false);
    			pz.setBorder(false);
                
				HTMLControl docType = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "instrNumber", "Instrument Number", 1, 1, 1, 1, 30, null, simTmp.getFunction( 0 ), searchId );
				docType.setHiddenParam(true);
				
				pz.addHTMLObject( docType );
				simTmp.setModuleParentSiteLayout( pz );							
            }
            catch( FormatException e )
            {
                e.printStackTrace();
            }

        
		}
		

		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		setModulesForGoBackOneLevelSearch(si);
		return si;		
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		// determine what we need to search
		boolean searchPins = getCrtTSServerName(miServerID).equals("ILCookLA");
		boolean searchNames = !searchPins;
		
		// determine what criteria we have
		boolean hasPins = hasPin();
		boolean hasNames = getSearchAttributes().getBuyers().size() !=0 || getSearchAttributes().getOwners().size() != 0;
		
		TSServerInfoModule module=null;
		if(searchPins && hasPins) {

			module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));		
			module.clearSaKeys();
			module.setName("automatic pins ");
			
			ILCookLACombinedFilterResponse filter = new ILCookLACombinedFilterResponse(searchId);
			filter.setUsePins(true);
			filter.setUseNames(false);
			module.addFilter(filter);
						
			module.addValidator(new RejectAlreadySavedDocumentsFilterResponse(searchId).getValidator());
			module.addValidator(new BetweenDatesFilterResponse(searchId).getValidator());
			  
			modules.add(module);
			
		}
		
		if(searchNames && hasNames) {

			module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));
			module.setSearchType(DocumentI.SearchType.GI.toString());
			module.clearSaKeys();
			module.setName("automatic names[individual] ");

//			ILCookLACombinedFilterResponse filter = new ILCookLACombinedFilterResponse(searchId);
//			filter.setUsePins(false);
//			filter.setUseNames(true);
//			module.addFilter(filter);
//						
//			module.addValidator(new RejectAlreadySavedDocumentsFilterResponse(searchId).getValidator());
//			module.addValidator(new BetweenDatesFilterResponse(searchId).getValidator());
			modules.add(module);
			
			module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));		
			module.setSearchType(DocumentI.SearchType.GI.toString());
			module.clearSaKeys();
			module.setName("automatic names[business] ");

//			filter = new ILCookLACombinedFilterResponse(searchId);
//			filter.setUsePins(false);
//			filter.setUseNames(true);
//			module.addFilter(filter);
						
			//module.addValidator(new RejectAlreadySavedDocumentsFilterResponse(searchId).getValidator());
			//module.addValidator(new BetweenDatesFilterResponse(searchId).getValidator());
			modules.add(module);
			
			if(isUpdate()) {	//only for update
				module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));		
				module.setSearchType(DocumentI.SearchType.GI.toString());
				module.clearSaKeys();
				module.setName("automatic names[individual] (from order) ");
				
				module.forceValue(29, createDates()[2]);
				
//				filter = new ILCookLACombinedFilterResponse(searchId);
//				filter.setUsePins(false);
//				filter.setUseNames(true);
//				module.addFilter(filter);
				module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));

				//module.addValidator(new RejectAlreadySavedDocumentsFilterResponse(searchId).getValidator());
				//module.addValidator(new BetweenDatesFilterResponse(searchId).setForceFromDateOrderUpdate(true).getValidator());
				
				modules.add(module);
				
				module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));	
				module.setSearchType(DocumentI.SearchType.GI.toString());
				module.clearSaKeys();
				module.setName("automatic names[business] (from order) ");
				
				module.forceValue(29, createDates()[2]);
				
//				filter = new ILCookLACombinedFilterResponse(searchId);
//				filter.setUsePins(false);
//				filter.setUseNames(true);
//				module.addFilter(filter);
				module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));

				//module.addValidator(new RejectAlreadySavedDocumentsFilterResponse(searchId).getValidator());
				//module.addValidator(new BetweenDatesFilterResponse(searchId).setForceFromDateOrderUpdate(true).getValidator());
				
				modules.add(module);
			}
			
		}
		
		module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));
        module.clearSaKeys();
        module.setIteratorType( ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
        OcrOrBootStraperIterator iterator = new OcrOrBootStraperIteratorILCook(searchId);
        iterator.setInitAgain(true);
        module.addIterator(iterator);
		modules.add(module);		
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch(modules);	
		serverInfo.setModulesForGoBackOneLevelSearch(modules);	
	}
	
	public ILCookLA(long searchId){
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 */
	public ILCookLA(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
		if ("ILCookLA".equals(getCrtTSServerName(miServerID))){
			setRepeatDataSource(true);
		}
	}

	/**
	 * Remove formatting from html row displayed in intermediate results
	 * @param html
	 * @return
	 */
	private static String removeFormatting(String html){
		int istart = html.indexOf("<td");
		if(istart == -1){ return html; }
		istart = html.indexOf("<td", istart +1);
		if(istart == -1){ return html; }
		istart = html.indexOf("<td", istart +1);
		if(istart == -1){ return html; }
		istart = html.indexOf(">", istart);
		if(istart == -1){ return html; }
		istart += 1;
		int iend = html.lastIndexOf("</td");
		if(iend == -1){ return html; }
		if(istart > iend){ return html; }
		return html.substring(istart, iend);
	}
	
	/**
	 * Fill a parsed response's infsets, create HTML representation
	 * @param tp3Docs
	 * @return retVal[0] - html format, retVal[1] - short type description
	 */
	private void parse (DocGroup tp3Docs){
    	          
    	String style;
    	Document rules = null;
    	switch(tp3Docs.type){
    	case PI_TYPE:
    		style = titleStyle;
    		tp3Docs.shortType = "PI";
    		tp3Docs.longType = "Property Index Document";
    		rules = piRules;
    		break;
    	case GI_TYPE:
    		style = titleStyle;
    		tp3Docs.shortType = "GI";
    		tp3Docs.longType = "General Index Document";
    		rules = giRules;
    		break;
    	case LC_TYPE:
    		style = titleStyle;
    		tp3Docs.shortType = "LC";
    		tp3Docs.longType = "Locates Index Document";
    		rules = lcRules;
    		break;
    	default:
    		throw new RuntimeException("type: " + tp3Docs.type + " not known!");
    	}
		
    	// use only first node
    	Document doc;
    	if(tp3Docs.nodes.size() == 1){    		
			doc = XmlUtils.createDocument(tp3Docs.nodes.get(0));
    	}else {
    		Tp3Record record = null;
    		for(Node node: tp3Docs.nodes){
    			record = Tp3Record.parseDocument(node, record);
    		}
    		doc = parseXml("<?xml version=\"1.0\"?>" + record.toString());
    	}
		
    	// create and set html response
    	String html = "<b><u>" + tp3Docs.longType + "</u></b><br/>" + applyTransformation(doc, style);  
    	tp3Docs.pr.setResponse(html);
    	tp3Docs.html = html;

    	// fill infsets
    	XMLExtractor.parseXmlDoc(tp3Docs.pr, doc, rules, searchId); 
    	
	}
	
    /**
     * get file name from link
     */
	@Override
	protected String getFileNameFromLink(String link){
		
		// try the tax link pattern 
		Pattern taxLinkPattern = Pattern.compile("DL___(\\d+)_tax");
		Matcher taxLinkMatcher = taxLinkPattern.matcher(link);
		if(taxLinkMatcher.find()){
			serverTypeDirectoryOverride = "County Tax";
			return taxLinkMatcher.group(1) + "_tax" + ".html";
		}
		
		// try the normal link pattern
		Pattern roLinkPattern = Pattern.compile("DL___([^&]+)&?");
		Matcher roLinkMatcher = roLinkPattern.matcher(link);
		if(roLinkMatcher.find()){
			serverTypeDirectoryOverride = null;
			return roLinkMatcher.group(1)+ ".html";
		}
		
		throw new RuntimeException("Unknown Link Type: " + link);
    }
	
	/**
	 * Get instrument number
	 * @param node
	 * @return
	 */
	private String [] getDocNo(Node node){
		serverTypeDirectoryOverride = null;
		for(Node n1: getChildren(node)){
			String name1 = n1.getNodeName();
			if("Instrument".equals(name1)){
				String docNo = "";
				String caseNo = "";
				for(Node n2: getChildren(n1)){
					String name2 = n2.getNodeName();
					if("DocumentNumber".equals(name2)){
						docNo = getNodeValue(n2).replaceAll("\\s+", "");
					}else if("CaseNumber".equals(name2)){
						caseNo = getNodeValue(n2).replaceAll("\\s+", "");
					}
				}
				if(!"".equals(caseNo) && !"".equals(docNo)){
					return new String[]{caseNo, docNo};	
				} else if(!"".equals(caseNo)){
					return new String[]{caseNo};
				} else if(!"".equals(docNo)){
					return new String[]{docNo, docNo};
				} else {
					return new String[0];
				}				
				
			} else if("PropertyAPN".equals(name1)){
				serverTypeDirectoryOverride = "County Tax";
				return new String[] {getNodeValue(n1).replace("-", "") + "_tax"};
			}
		}
		return new String[0];
	}
	
	private static class DocGroup{
		public int type;
		boolean hasImage;
		public String html = "";
		public String shortType = "";
		public String longType = "";
		public ParsedResponse pr;
		public List<Node> nodes  = new LinkedList<Node>();
		public String imgDocNo = "";
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		ParsedResponse pr = Response.getParsedResponse();
		
		switch(viParseID){
		case COMBINED_MODULE_PID:
			
			// load rules
			loadRules();
			
			Map<String,DocGroup> docs = new LinkedHashMap<String,DocGroup>();
			
	    	// iterate through all records to group duplicates together
			// notice that each TP3Docs instance remembers the first ParsedResponse, not to create another one
            for (ParsedResponse item: (Vector<ParsedResponse>)pr.getResultRows()) {
            	
            	// get node and docNo
            	Node doc = (Node) item.getAttribute(ParsedResponse.DASL_RECORD);            	
            	String [] nos = getDocNo(doc);          	            	
            	if(nos.length == 0){
            		logger.warn(searchId + ": Document from ILCookLA  has NO Instrument number. It has been skipped!");   
            		continue;
            	}            	            	

            	String docNo = nos[0];
            	boolean hasImage = (nos.length == 2);
            	String imgDocNo = hasImage ? nos[1] : "";
            	
            	item.setAttribute(ParsedResponse.DASL_DOCNO, docNo);            	
            	
            	// add doc to the group or create new group
            	DocGroup docGroup = docs.get(docNo); // has "doc:" in case the number comes from a document number
            	if(docGroup == null){
            		docGroup = new DocGroup();
            		docGroup.hasImage = hasImage;
            		docGroup.type = (Integer) item.getAttribute(ParsedResponse.DASL_TYPE);
            		docGroup.pr = item;
            		docGroup.imgDocNo = imgDocNo;
            		docs.put(docNo, docGroup);
            	}
            	docGroup.nodes.add(doc);

            }
            HashMap<String, String> data = new HashMap<String, String>();
            // parse the document groups
            Vector<ParsedResponse> newResultRows = new Vector<ParsedResponse>();
            for(Map.Entry<String, DocGroup> entry: docs.entrySet()){
            	
            	String docNo = entry.getKey();
            	DocGroup docGroup = entry.getValue();
            	
            	serverTypeDirectoryOverride = (docGroup.type == TX_TYPE) ? "County Tax" : null;    			
            	docGroup.pr.setParentSite(pr.isParentSite());
            	// parse
            	parse(docGroup);
            	
            	// add row
            	newResultRows.add(docGroup.pr);
            	
            	// create links
            	String originalLink = "DL___" + docNo;            	
            	String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;   
                
            	RegisterDocumentI doc = null;
            	
            	try{doc = (RegisterDocumentI)docGroup.pr.getDocument();}catch(Exception e){};
            	
            	String link = null;
            	
            	if(Boolean.FALSE != getSearch().getAdditionalInfo("img_" + docNo) && docGroup.hasImage){
            		link = createImageLink(docGroup.imgDocNo, doc);
            	}                                
                if(link != null){
                	docGroup.html += "<a href=\"" + link + "\">View Image</a>";
                	if(docGroup.pr.getImageLinksCount() == 0){
                		docGroup.pr.addImageLink(new ImageLinkInPage (link, docNo + ".tiff" ));
                	}                	
                }
                boolean isTSRIRestore = false;
                try {
					isTSRIRestore = (Boolean) this.GetAttribute("TSRIE_Restore");
				} catch (Exception e){}
                
				String checkbox = "";
            	data.put("type", (doc!=null?doc.getServerDocType():"N/A"));
                if (isInstrumentSaved(docNo, null, data, false) && !isTSRIRestore) {
                	checkbox = "saved";                	           	
                } else {
                	mSearch.addInMemoryDoc(sSave2TSDLink, docGroup.pr);
                	checkbox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
                }
                docGroup.html =	"<tr>" +
    								"<td valign=\"center\" align=\"center\">" + checkbox + "</td>" +
    								"<td align=\"center\"><b>" + docGroup.shortType + "</b></td>" +
    								"<td>" + docGroup.html + "</td>" +
    								"<tr>";   
                
                docGroup.pr.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));                

                parser.Parse(docGroup.pr, docGroup.html, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
     
            	
            }

            // set the result rows - does not contain instruments without instr no
            pr.setResultRows(newResultRows);
            	            
            // set proper header and footer for parent site search
            if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH) {
            	String header = pr.getHeader();
               	String footer = pr.getFooter();                           	
            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">" +
	            			"<tr bgcolor=\"#cccccc\">" +	            			    
		            			"<th width=\"1%\"><div>" + SELECT_ALL_CHECKBOXES + "</div></th>" +
		            			"<th width=\"1%\">Type</th>" +
		            			"<th width=\"98%\" align=\"left\">Document</th>" +
	            			"</tr>";
            	footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);            	
            	pr.setHeader(header);
            	pr.setFooter(footer);
            }
            
			break;
			
		case ID_SAVE_TO_TSD:
			
			DocumentI document = pr.getDocument();
			
			if(document!= null && pr.getAttribute(ParsedResponse.DASL_DOCNO) == null) {
				msSaveToTSDFileName = document.getId() + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
			} else {
			
				// set directory override
				Integer type = (Integer) pr.getAttribute(ParsedResponse.DASL_TYPE);
				serverTypeDirectoryOverride = (type == TX_TYPE) ? "County Tax" : null;   
							
				// determine instrument number
				String docNo = (String)pr.getAttribute(ParsedResponse.DASL_DOCNO);
				
				// determine html
	        	String html = removeFormatting(pr.getResponse());
	        	
	        	// set file name
	            msSaveToTSDFileName = docNo + ".html";            
	            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	            
	            // save to TSD
	            msSaveToTSDResponce = html + CreateFileAlreadyInTSD(true);            
	            parser.Parse(pr, html, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
			}
			break;
		}
	}

	public String createImageLink(String instrumentNumber, RegisterDocumentI doc) {
		return CreatePartialLink(TSConnectionURL.idGET) + "look_for_dl_image" +
			"&documentNumber=" + instrumentNumber +
			"&instrumentType=" +(doc!=null?doc.getServerDocType():"N/A") +
			"&dateFiled="+(doc!=null && doc.getRecordedDate() != null?new SimpleDateFormat(FormatDate.PATTERN_MM_MINUS_dd_MINUS_yyyy).format(doc.getRecordedDate()):"N/A");
	}
	
	/**
	 * Check that whenever a last name is specifies, a first name is specified too
	 * @param params search parameters
	 * @return error message or empty string if no error
	 */
	private String verifyNames(Map<String,String> params){		
		for(int i=1; i<=4; i++){
			String last = params.get("LastName" + i);
			String first = params.get("FirstName" + i);			
			if(!StringUtils.isEmpty(last) && StringUtils.isEmpty(first)){
				return "Error: cannot search person by last name only, without having a first name! "; 				 
			}			
		}
		return "";
	}
	
	private static String cleanPin(String pin){
		pin = pin.trim();
		Pattern pidPattern = Pattern.compile("(\\d{2})-?(\\d{2})-?(\\d{3})-?(\\d{3})(?:-?(\\d{4}))?");
		Matcher pidMatcher = pidPattern.matcher(pin);
		if(pidMatcher.matches()){
			pin = 	pidMatcher.group(1) + "-" +
    				pidMatcher.group(2) + "-" +
    				pidMatcher.group(3) + "-" +
    				pidMatcher.group(4) + "-";
			if(pidMatcher.group(5) == null){
				pin += "0000";
			} else {
				pin += pidMatcher.group(5);
			}
		}
		return pin;
	}
	
    @Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
    	
    	
    	if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
    		ServerResponse sr = new ServerResponse();
    		RestoreDocumentDataI restoreDocumentDataI = (RestoreDocumentDataI)module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE);
			if(restoreDocumentDataI != null) {
				Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
				
				RegisterDocumentI docR = restoreDocumentDataI.toRegisterDocument(getSearch(), getDataSite());
				
				LinkInPage linkInPage = new LinkInPage(
						getLinkPrefix(TSConnectionURL.idPOST) + "DL___" + docR.getId(), 
						getLinkPrefix(TSConnectionURL.idPOST) + "DL___" + docR.getId(), 
    					TSServer.REQUEST_SAVE_TO_TSD);
				
				ParsedResponse pr = new ParsedResponse();
				pr.setDocument(docR);
				
				if(pr.getImageLinksCount() == 0){
            		pr.addImageLink(new ImageLinkInPage (createImageLink(docR.getInstno(), docR), docR.getInstno() + ".tiff" ));
            	} 
				
				String asHtml = docR.asHtml(); 
				pr.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
				pr.setOnlyResponse((String)pr.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE));
				pr.setSearchId(searchId);
				pr.setUseDocumentForSearchLogRow(true);
				pr.setPageLink(linkInPage);
				getSearch().addInMemoryDoc(linkInPage.getLink(), pr);
				parsedRows.add(pr);
				sr.getParsedResponse().setResultRows(parsedRows );
		        sr.setResult("");
			}
    		return sr;
    	} 
    	
    	
    	
    	boolean automatic = module.getName().contains("automatic");
    	
    	Map<String,String> unescapedParams;
    	
    	if(automatic){
    		
    		boolean usePins = module.getName().contains("pins");
    		boolean useNames = module.getName().contains("names");
    		boolean useNamesFromOrder = module.getName().contains("from order");
    		Search search = getSearch();
    		boolean companySearch =  module.getName().contains("[business]");
    		

			String dates [] = createDates();
			
    		HashMap<String,String> params = new HashMap<String,String>();

    		Map<String,String> paramsForLog = new LinkedHashMap<String,String>(); 
    		paramsForLog.put("FromDate", dates[0]);
			paramsForLog.put("ToDate", dates[1]);
    		
    		
    		boolean logSearchBy = false;
    		// add PIN parameters
    		if(usePins){
    			String pinsAS = (String) mSearch.getAdditionalInfo("ALREADY_SEARCHED_PINS_LA");
    			if (pinsAS == null){
    				pinsAS = "";
    			}
    			params.put("FromDateProperty", dates[0]);
    			params.put("ToDateProperty", dates[1]);
    			Collection<String> pins = getSearchAttributes().getPins(4);    			
    			int i = 1;
    			String logPins = "";
    			for(String pin: pins){
    				pin = cleanPin(pin);
    				if (!pinsAS.contains(pin) && !params.containsValue(pin)){
	    				params.put("APN" + i, pin);
	    				if(i !=1 ){
	    					logPins += ",";
	    				}
	    				logPins += pin + " ";
	    				i++;
	    				logSearchBy = true;
	    			}
    			}
    			if (StringUtils.isEmpty(logPins)){
    				return new ServerResponse();
    			}
    			params.put(ILCookLAQueryBuilder.PIN_COUNT, "" + pins.size());
    			paramsForLog.put("PINs", logPins);
    			
    			mSearch.setAdditionalInfo("ALREADY_SEARCHED_PINS_LA", pinsAS + " " + logPins);
    		} 
    		
    		// add names
    		if(useNames){
    			if (isUpdate()) {
    				if(useNamesFromOrder) {
    					params.put("FromDateParty", dates[2]);
    	    			params.put("ToDateParty", dates[1]);
    	    			paramsForLog.put("FromDate", dates[2]);
    	    			
    	    			int indivCount = 0;
    	    			int busCount = 0;
    	 
    	    			String logNames = "";
    	    			Set<String> testDuplicityMap = new HashSet<String>();    			
    	    			for(PartyI party: new PartyI[] {mSearch.getSa().getOwners(), mSearch.getSa().getBuyers()} ){
    	    				for(NameI name: party.getNames()){
    	    					if((companySearch && name.isCompany()) 
    	    							|| (!companySearch && !name.isCompany())) {
	    	    					if(name.getNameFlags().isNewFromOrder()) {
	    		    					String last = name.getLastName();
	    		    					String first = name.getFirstName();
	    		    					String testValue = (last+ "______" + first).toLowerCase();
	    		    					if (!testDuplicityMap.contains(testValue)){
	    			    					if(!StringUtils.isEmpty(last)){
	    			    						if(!StringUtils.isEmpty(first)){
	    										
	    										
	    											indivCount += 1;
	    											params.put("LastName" + indivCount, last);    							
	    			    							params.put("FirstName" + indivCount, first);
	    			    							logNames += last + ", " + first + "; ";
	    			    						
	    			    							logSearchBy = true;
	    			    						} else {
	    			    							if (CompanyNameExceptions.allowed(last, searchId)){
	    			    								busCount += 1;
	    			    								params.put("FullName" + busCount, last);
	    			    								logNames += last + "; ";
	    			    								logSearchBy = true;
	    			    							}
	    			    						}
	    				    					
	    									}
	    			    					testDuplicityMap.add(testValue);
	    		    					}
	    	    					}
    	    					}
    	    				}
    	    			}
    	    			
    	   				params.put(ILCookLAQueryBuilder.IND_COUNT, "" + indivCount);
    	   				params.put(ILCookLAQueryBuilder.BUS_COUNT, "" + busCount);
    	   				if(logNames.endsWith("; ")){
    	   					logNames = logNames.substring(0, logNames.lastIndexOf("; "));
    	   				}
    	   				paramsForLog.put("Names", logNames);
    	   				
    	   				
    	   				DocumentsManagerI documentsManagerI = search.getDocManager();
    	   				
    	   				try {
    	   					documentsManagerI.getAccess();
    	   					if(!documentsManagerI.isFieldModified(DocumentsManager.Fields.START_VIEW_DATE)) {
    	   						Date newDate = ro.cst.tsearch.generic.Util.dateParser3(dates[2]);
    	   						if(newDate != null && documentsManagerI.getStartViewDate().after(newDate)) {
        	   						documentsManagerI.setStartViewDate(newDate);
        	   					}
    	   					}
    	   				} finally {
    	   					documentsManagerI.releaseAccess();
    	   				}
    	   				
    	   				
    				} else {
    					params.put("FromDateParty", dates[0]);
    	    			params.put("ToDateParty", dates[1]);
    	
    	    			int indivCount = 0;
    	    			int busCount = 0;
    	 
    	    			String logNames = "";
    	    			Set<String> testDuplicityMap = new HashSet<String>();    			
    	    			for(PartyI party: new PartyI[] {mSearch.getSa().getOwners(), mSearch.getSa().getBuyers()} ){
    	    				for(NameI name: party.getNames()){
    	    					
    	    					if((companySearch && name.isCompany()) 
    	    							|| (!companySearch && !name.isCompany())) {
    	    					
	    	    					if(!name.getNameFlags().isNewFromOrder()) {
	    		    					String last = name.getLastName();
	    		    					String first = name.getFirstName();
	    		    					String testValue = (last+ "______" + first).toLowerCase();
	    		    					if (!testDuplicityMap.contains(testValue)){
	    			    					if(!StringUtils.isEmpty(last)){
	    			    						if(!StringUtils.isEmpty(first)){
	    										
	    										
	    											indivCount += 1;
	    											params.put("LastName" + indivCount, last);    							
	    			    							params.put("FirstName" + indivCount, first);
	    			    							logNames += last + ", " + first + "; ";
	    			    						
	    			    							logSearchBy = true;
	    			    						} else {
	    			    							if (CompanyNameExceptions.allowed(last, searchId)){
	    			    								busCount += 1;
	    			    								params.put("FullName" + busCount, last);
	    			    								logNames += last + "; ";
	    			    								logSearchBy = true;
	    			    							}
	    			    						}
	    				    					
	    									}
	    			    					testDuplicityMap.add(testValue);
	    		    					}
	    	    					}
	    	    				}
    	    				}
    	    			}
    	    			
    	   				params.put(ILCookLAQueryBuilder.IND_COUNT, "" + indivCount);
    	   				params.put(ILCookLAQueryBuilder.BUS_COUNT, "" + busCount);
    	   				if(logNames.endsWith("; ")){
    	   					logNames = logNames.substring(0, logNames.lastIndexOf("; "));
    	   				}
    	   				paramsForLog.put("Names", logNames);
    				}
    				
    			} else {
    				params.put("FromDateParty", dates[0]);
	    			params.put("ToDateParty", dates[1]);
	
	
	    			int indivCount = 0;
	    			int busCount = 0;
	 
	    			String logNames = "";
	    			Set<String> testDuplicityMap = new HashSet<String>();    			
	    			for(PartyI party: new PartyI[] {mSearch.getSa().getOwners(), mSearch.getSa().getBuyers()} ){
	    				for(NameI name: party.getNames()){
	    					if((companySearch && name.isCompany()) 
	    							|| (!companySearch && !name.isCompany())) {
		    					String last = name.getLastName();
		    					String first = name.getFirstName();
		    					String testValue = (last+ "______" + first).toLowerCase();
		    					if (!testDuplicityMap.contains(testValue)){
			    					if(!StringUtils.isEmpty(last)){
			    						if(!StringUtils.isEmpty(first)){
										
										
											indivCount += 1;
											params.put("LastName" + indivCount, last);    							
			    							params.put("FirstName" + indivCount, first);
			    							logNames += last + ", " + first + "; ";
			    						
			    							logSearchBy = true;
			    						} else {
			    							if (CompanyNameExceptions.allowed(last, searchId)){
			    								busCount += 1;
			    								params.put("FullName" + busCount, last);
			    								logNames += last + "; ";
			    								logSearchBy = true;
			    							}
			    						}
				    					
									}
			    					testDuplicityMap.add(testValue);
		    					}
	    					}
	    				}
	    			}
	    			
	   				params.put(ILCookLAQueryBuilder.IND_COUNT, "" + indivCount);
	   				params.put(ILCookLAQueryBuilder.BUS_COUNT, "" + busCount);
	   				if(logNames.endsWith("; ")){
	   					logNames = logNames.substring(0, logNames.lastIndexOf("; "));
	   				}
	   				paramsForLog.put("Names", logNames);
    			}
    			

	    			
    		}

			
			if (logSearchBy){
				logSearchBy(module, paramsForLog);
			}
	    	
	    	unescapedParams = params;
	    	
    	} else {
	    
    		// perform the translation of PINs    			
	    	for(int i=32; i<=35; i++){
	    		String pin = module.getFunction(i).getParamValue();
	    		pin = cleanPin(pin);
	    		module.getFunction(i).setParamValue(pin);
	    	}
	    	
	    	// log the search in the SearchLogger
	    	logSearchBy(module);
	    	
	    	// get search parameters
	    	unescapedParams = getNonEmptyParams(module, null);
	    	
	    	// check the last names not to be alone
	    	String message = verifyNames(unescapedParams);
	    	if(!StringUtils.isEmpty(message)){
	    		SearchLogger.info("<font color=\"red\">" + message + "</font>", searchId);
	    		return ServerResponse.createErrorResponse(message);    		
	    	}

    	}
            		    	
    	// xml escape all params and make them uppercase
    	Map<String,String> params  = new HashMap<String,String>();
    	for(String key: unescapedParams.keySet()){
    		String val = unescapedParams.get(key);
    		if(!StringUtils.isEmpty(val)){
    			params.put(key, StringEscapeUtils.escapeXml(val.toUpperCase()));
    		}
    	}
    	    	
    	// create XML query
    	String xmlQuery = ILCookLAQueryBuilder.buildSearchQuery(params, searchId);
    	if("".equals(xmlQuery)){
    		if (params.containsKey("BUS_COUNT") && params.containsKey("IND_COUNT"))
    			if ("0".equals(params.get("BUS_COUNT")) && "0".equals(params.get("IND_COUNT"))) {
    				return ServerResponse.createEmptyResponse();
    			}
    		String msg = "</br><font color=\"red\">Not enough data entered for a search to be performed!</font></br>";
    		SearchLogger.info("Error: " + msg, searchId);
    		return ServerResponse.createErrorResponse("Not enough data entered for a search to be performed!");
    	}
    	
    	// query DASL
    	ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse daslResponse = 	getDaslSite(miServerID, searchId)
    			.performSearch(xmlQuery, searchId);
    	
    	// order not received in time
    	if(daslResponse.status == DaslConnectionSiteInterface.ORDER_PLACED){
    		SearchLogger.info("<font color=\"red\">DASL order not completed in time! Gave up for Order:</font>" + daslResponse.id, searchId);
    		return ServerResponse.createErrorResponse("Order (" + daslResponse.id + ") Not Completed in Time!");
    	}

    	// error appeared with order
    	if(daslResponse.status == DaslConnectionSiteInterface.ORDER_ERROR){
    		if(daslResponse.id > 0){
    			SearchLogger.info("<font color=\"red\">DASL order error! Gave up for Order:</font>" + daslResponse.id , searchId);    		
	    		return ServerResponse.createErrorResponse("DASL response for Order "+ daslResponse.id +":");
    		} else {
    			SearchLogger.info("<font color=\"red\">DASL order error! Gave up.</font>", searchId);    		
	    		return ServerResponse.createErrorResponse("DASL response:");
    		}
    	}
    	
    	// treat other error situations that might appear, like exceptions embedded in the XML response
    
    	// extract useful information from the received XML    	
    	NodeList giNodes = null;
    	NodeList piNodes = null;
    	NodeList lcNodes = null;
    	//NodeList txNodes = null;    
    	String rawResponse = "";
    	try{
    		Node xmlDoc = parseXml(daslResponse.xmlResponse);
        	
    		// try to set certification date
    		try{
    			String date = findNodeValue(xmlDoc, "//PlantEffectiveDates/GeneralIndex/ToValue/Date");
    			if(!StringUtils.isEmpty(date)){
    				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
    			}
    		}catch(RuntimeException e){
    			logger.error("Could not obtain certificatin date!");
    		}
    		
    		piNodes = xpathQuery(xmlDoc, "//PropertyIndexTitleData/TitleSearchReport/TitleData/TitleRecord/TitleDocument");
    		giNodes = xpathQuery(xmlDoc, "//GeneralIndexTitleData/TitleSearchReport/TitleData/TitleRecord/TitleDocument");
        	lcNodes = xpathQuery(xmlDoc, "//LocatesIndexTitleData /TitleSearchReport/TitleData/TitleRecord/TitleDocument");
        	//txNodes = xpathQuery(xmlDoc, "//TaxData/TitleSearchTaxReport");

        	try{
	        	NodeList nl = xpathQuery(xmlDoc, "//ReportBinary");
	        	if(nl.getLength() > 0){
	        		String coded = XmlUtils.getNodeCdataOrText(nl.item(nl.getLength() - 1));        		
	        		rawResponse = XmlUtils.decodeBase64(coded);
	        		rawResponse = rawResponse.replace((char)0xc, ' ');
	        	}
        	}catch(RuntimeException e){
        		SearchLogger.info("<font color=\"red\">DASL raw response extraction exception!</font>", searchId);
        		logger.error("DASL raw response extraction exception!",e);
        	}			
			
    	}catch(RuntimeException e){
    		logger.error("DL parsing exception", e);
    		SearchLogger.info("DASL raw response parse exception", searchId);
    		return ServerResponse.createErrorResponse("Error parsing the response received from DASL/TP3");
    	}
    	
    	// create & populate server response
    	ServerResponse sr = new ServerResponse();                    	
        Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
        NodeList[] nlists = new NodeList[]{piNodes, giNodes, lcNodes}; //, txNodes};
        Integer types[] = new Integer[]{PI_TYPE, GI_TYPE, LC_TYPE, TX_TYPE};
        for(int i=0; i<nlists.length; i++){
        	Integer type  = types[i];
        	NodeList nl = nlists[i];
	        for(int j=0; j<nl.getLength(); j++){ 
	        	Node node = nl.item(j);
	            ParsedResponse parsedResponse = new ParsedResponse();
	            parsedResponse.setAttribute(ParsedResponse.DASL_RECORD, node);
	            parsedResponse.setAttribute(ParsedResponse.DASL_TYPE, type);
	            parsedRows.add(parsedResponse );
	        }
        }
        sr.getParsedResponse().setResultRows(parsedRows);
        sr.setResult("");

        // solve response
        solveHtmlResponse("", module.getParserID(), "SearchBy", sr, sr.getResult());
        
        // log number of results found
        SearchLogger.info("Found <span class='number'>" + sr.getParsedResponse().getResultsCount() + "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId);
        SearchLogger.info("<b>Raw response:</b>", searchId);        
        SearchLogger.info("<pre width=\"80\" style=\"color: #080808; background: #dddddd\">" + StringEscapeUtils.escapeHtml(rawResponse) + "</pre>", searchId);

        // fix daslResponse.id in case of using cache
        if(daslResponse.id == 0 || daslResponse.id == -1){
        	try{
        		daslResponse.id = Integer.parseInt(StringUtils.extractParameter(rawResponse, "Order Number:\\s+(\\d+)"));
        	}catch(RuntimeException e){
        		daslResponse.id = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
        	}
        }
        
        boolean useNames = false;
        
		if(automatic){
			if (module.getName().contains("names")){
				useNames = true;
			} else if (module.getName().contains("pins")){
				useNames = false;
			}
		} else {
			List<TSServerInfoFunction> functionList = module.getFunctionList();
			for (TSServerInfoFunction tsServerInfoFunction : functionList) {
				if (tsServerInfoFunction.getName().toLowerCase().contains("name")){
					if (StringUtils.isNotEmpty(tsServerInfoFunction.getParamValue())){
						useNames = true;
						break;
					}
				}
			}
		}
        		
        // save raw response
        try{
            new File(getSearch().getSearchDir() + "Register").mkdirs();
            String rawFileName = getSearch().getSearchDir() + "Register" + File.separator 
            		+ daslResponse.id + (useNames ? "namechain" : "propertychain") + ".doc";
            FileUtils.writeTextFile(rawFileName, rawResponse);
        }catch(RuntimeException e){
        	logger.error(e);
        }
        
        // return response
        return sr;
        
        
    }

    /**
     * Save an image. Called only during TSR creation
     */
    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException{
    	
    	Matcher matcher = imageLinkPattern.matcher(image.getLink());
    	if(matcher.find()){
    		//extract image info from link
    		String inst = matcher.group(1);
    		String type = matcher.group(2);
    		String dateStr = matcher.group(3);
    		
    		String year="";
    		if(dateStr!=null){
    			int posYear = dateStr.lastIndexOf('-');
    			if(posYear>0 && posYear<dateStr.length()-1){
    				year = dateStr.substring(posYear+1);
    			}
    		}
    		
        	// create filename and its folder
    		String fileName = image.getPath();
        	
        	// retrieve image
        	if(ILCookImageRetriever.INSTANCE.retrieveImage(inst, fileName, type, year, searchId)){
        		//already counted as retrieved 
        		//afterDownloadImage(true); 
        		byte b[] = FileUtils.readBinaryFile(fileName);
        		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
        	}
    	}
    	
    	return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );

    } 
    
    /**
     * Override GetLink in order to retrieve the image
     */
    @Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)throws ServerResponseException {   
    	
    	/*
    	 * get image from DataTree if necessary
    	 */
    	Matcher matcher = imageLinkPattern.matcher(vsRequest);
    	if(matcher.find()){
    		
    		// extract image info from link
    		final String inst = matcher.group(1);
        	
    		String type = matcher.group(2);
    		String dateStr = matcher.group(3);
    		
    		String year="";
    		if(dateStr!=null){
    			int posYear = dateStr.lastIndexOf('-');
    			if(posYear>0 && posYear<dateStr.length()-1){
    				year = dateStr.substring(posYear+1);
    			}
    		}
    		
        	// create filename and its folder
    		String folderName = getCrtSearchDir() + "Register" + File.separator;
    		new File(folderName).mkdirs();
        	String fileName = folderName + inst + ".tiff";
        	
        	// retrieve image        	
        	ILCookImageRetriever.INSTANCE.retrieveImage(inst, fileName, type, year,searchId);
        				
    		// write the image to the client web-browser
			boolean imageOK = writeImageToClient(fileName, "image/tiff");

			// image not retrieved
			if(!imageOK){   
				
				// mark it as invalid
    			getSearch().setAdditionalInfo("img_" + inst, Boolean.FALSE);	
		        
		        // return error message
    			ParsedResponse pr = new ParsedResponse();
    			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
    			throw new ServerResponseException(pr);
			}
			return ServerResponse.createSolvedResponse();
    	}
    	
    	// default behaviour
    	return super.GetLink(vsRequest, vbEncoded);
    }
	
    /**
     * Retrieve an image from Dasl TP3
     * @param inst instrument number
     * @param fileName file name where the doc needs to be saved
     */
    public static boolean retrieveImage(String documentNumber, String fileName, int miServerID, long searchId){
    	
    	// check if the file is already present
    	if(FileUtils.existPath(fileName)){ 
    		return true; 
    	}         
    	
    	// we need the document number
    	if(StringUtils.isEmpty(documentNumber)){ 
    		return false; 
    	}
    	
    	// retrieve image
    	Map<String,String> params = new LinkedHashMap<String,String>();    
    	params.put("DocumentNumber", documentNumber); 

    	try{
    		String query = ILCookLAQueryBuilder.buildImageQuery(params, searchId);
    		String result = getDaslSite(miServerID, searchId).performImageSearch(query);
    		if(result == null){
    			return false;
    		}
	    	Node doc = XmlUtils.parseXml(result);
			NodeList nl = xpathQuery(doc, "//Content");
			if(nl.getLength() == 0){
				return false;
			}
			String coded = XmlUtils.getNodeCdataOrText(nl.item(nl.getLength() - 1));
			XmlUtils.decodeBase64(coded, fileName);
    	}catch(RuntimeException e){
    		logger.error(e);
    	}
    	
    	return FileUtils.existPath(fileName);
    }
    
    /**
     * Find a node and return its value or CDATA value
     * if several nodes found, first one is taken into consideration
     * @param doc DOM node
     * @param xpath node selection expression
     * @return value of that node - empty string if not found
     */
    private static String findNodeValue(Node doc, String xpath){
    	
    	// find doc node
    	NodeList docNoNodes = xpathQuery(doc, xpath);
    	if(docNoNodes.getLength() == 0){
    		return "";
    	}    	
    	
    	// find doc text
    	String inst = XmlUtils.getNodeCdataOrText(docNoNodes.item(0));
    	if("".equals(inst)){
    		return "";
    	}
    	
    	return inst;
    }
    
	protected static DaslConnectionSiteInterface getDaslSite(int miServerID, long searchId){				
		return new DaslSite(miServerID, searchId);
	}
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
 
		ConfigurableNameIterator nameIterator = null;
		DocsValidator doctypeValidator = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId).getValidator();
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
        if(!getCrtTSServerName(miServerID).equals("ILCookLA")){
	     for (String id : gbm.getGbTransfers()) {
	  		 module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));		
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     module.setIteratorType( 1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			 module.setIteratorType( 2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE );
			 module.setIteratorType( 3, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			 module.setIteratorType( 13, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME );
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		     if (date!=null) {
		    	 module.getFunction(29).setData(date);
				 module.getFunction(68).setData(date);
		     }
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		  	 module.addValidator(doctypeValidator);
		  	 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
		 	 module.addIterator(nameIterator);
		 	 
		  	 
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	 module = new TSServerInfoModule(serverInfo.getModule(COMBINED_MODULE_IDX));		
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
			     module.setIteratorType( 1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
				 module.setIteratorType( 2, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE );
				 module.setIteratorType( 3, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
				 module.setIteratorType( 13, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME );
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				 if (date!=null) {
					 module.getFunction(29).setData(date);
				     module.getFunction(68).setData(date);
				 }
				 module.addValidator(doctypeValidator);
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
				 module.addIterator(nameIterator);
				 
				 modules.add(module);
		     }
	     }
        }
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	     
		     		     
    	
    }
	
	@Override
	public void modifyDefaultServerInfo(){
		if (getCrtTSServerName(miServerID).equals("ILCookLA")){
			TSServerInfo serverInfo = defaultServerInfo;
			if (serverInfo != null){
				ArrayList<TSServerInfoModule> modules = (ArrayList<TSServerInfoModule>) serverInfo.getModulesForAutoSearch();
				if (modules != null){
					for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
						TSServerInfoModule tsServerInfoModule = (TSServerInfoModule) iterator.next();
						if (tsServerInfoModule != null){
							if (tsServerInfoModule.getModuleIdx() == COMBINED_MODULE_IDX){
								if ("automatic pins ".equals(tsServerInfoModule.getMsName())){
									tsServerInfoModule.addFilter(LegalFilterFactory.getDefaultUnitFilter(searchId));
									modules.remove(tsServerInfoModule);
									modules.add(tsServerInfoModule);
									break;
								}
							}
						}
						
					}
					defaultServerInfo.setModulesForAutoSearch(modules);
				}
			}
		}
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public TSServerInfoModule getILCookLARecoverModuleFrom(String pin,  HashMap<Long, RestoreDocumentDataI> allRestorableDocuments, List<Long> documentIds) {
		
		if (documentIds == null || allRestorableDocuments == null){
			return null;
		}
		TSServerInfoModule module = null;
		
		List<String> instrumentNumbers = new ArrayList<String>();
		for (Long documentId : documentIds){
			RestoreDocumentDataI document = allRestorableDocuments.get(documentId);
			
			if (document == null){
				continue;
			} else{
				instrumentNumbers.add(document.getInstrumentNumber());
			}
		}
		
		if(StringUtils.isNotEmpty(pin) && instrumentNumbers.size() > 0) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			String [] dates = createDates();
			
			if (dates != null && dates.length > 1){
//				module.forceValue(29, dates[0]);
//				module.forceValue(30, dates[1]);
				
				module.forceValue(68, dates[0]);
				module.forceValue(69, dates[1]);
			}
			module.forceValue(32, pin);
			
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("MultipleInstrumentNumber", instrumentNumbers.toString().replaceAll("[\\]\\[]+", ""));
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
		} 
		
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
	
	public String getPinFromDescription(RestoreDocumentDataI document){
		
		if (document == null){
			return "";
		}
		
		Pattern pat = Pattern.compile("(?is)\\bPIN:\\s*([\\d-]+)\\b");
		Matcher mat = pat.matcher(document.getDescription());
		
		if (mat.find()){
			return mat.group(1);
		} else{
			return "";
		}
	}
	
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
    				return true;
    			} else if(!checkMiServerId) {
    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    				if(almostLike != null && !almostLike.isEmpty()) {
    					return true;
    				}
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentCategory("MISC", searchId));
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(almostLike.isEmpty()){
	    				instr.setInstno(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(instr.getInstno()));
	    				almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			}
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
}