package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_CHECK_BOX;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_SELECT_BOX;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setHiddenParamMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;

/**
 * @author radu bacrau
 */
public class ILCookIS extends GenericISI {

	public static final long serialVersionUID = 10000000L;
	
	public ILCookIS(long searchId) {
		super(searchId);
	}

	public ILCookIS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo si = new TSServerInfo(2);
		si.setServerAddress("https://reidi.propertyinfo.com");
		si.setServerIP("reidi.propertyinfo.com");
		si.setServerLink("https://reidi.propertyinfo.com");
		
		//SIMPLE SEARCH
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(TSServerInfo.NAME_MODULE_IDX, 12);
			sim.setName("SimpleSearch");
			sim.setDestinationPage("/ISI/search/Search.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_SEARCH_BY_NAME);
			sim.setSearchType("CS");

			PageZone pz = new PageZone("SimpleSearch", "Simple Search", ORIENTATION_HORIZONTAL, null, 850, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            stfr = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 3, sim.getFunction(0), "txtStreetNumberFrom", "St. # From", null, searchId),
	            stto = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1,  1, 3, sim.getFunction(1), "txtStreetNumberTo",   "St. # To", null, searchId),
	            stpr = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  1,  1, 3, sim.getFunction(2), "txtStreetDirPre",     "Pre Dir", null, searchId),
	            
	            stna = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2,  2, 25,sim.getFunction(3), "txtStreetName",       "St. Name", null, searchId),
	            stpo = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2,  2, 3, sim.getFunction(4), "txtStreetDirPost",    "Post Dir", null, searchId),
	            stds = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  2,  2, 3, sim.getFunction(5), "txtStreetDesc",       "Designator", "", searchId),	            
	            unit = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  2,  2, 3, sim.getFunction(6), "txtAddressUnit",      "Unit", null, searchId),
	            
	            pin  = new HTMLControl(HTML_TEXT_FIELD, 1, 4,  3,  3, 18,sim.getFunction(7), "txtPIN",              "PIN", null, searchId),
	            own  = new HTMLControl(HTML_TEXT_FIELD, 1, 4,  4,  4, 35,sim.getFunction(8), "txtOwner",            "Owner", null, searchId),	            
	            ss   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(9), "txtSavedSearch",      "", "", searchId),
	            cnt  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(10),"ddlCounty",           "", "31", searchId),
	            btn  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(11),"btnSearch",           "", "Search", searchId);
	            
	            pin.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            own.setFieldNote("(e.g SMITH JOHN)");
	            
	            setHiddenParamMulti(true, ss, cnt, btn);	
	            setRequiredCriticalMulti(true);
	            //setJustifyFieldMulti(true, stfr, pin, own);
	            setJustifyFieldMulti(false,  stfr, stto, stpr, stna, stpo, stds, unit, pin, own);
	            
	            pz.addHTMLObjectMulti(stfr, stto, stpr, stna, stpo, stds, unit, pin, own, ss, cnt, btn);
	            
	            
	            sim.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
	            sim.getFunction(1).setSaKey(SearchAttributes.P_STREETNO);
	            sim.getFunction(2).setSaKey(SearchAttributes.P_STREETDIRECTION);
	            sim.getFunction(3).setSaKey(SearchAttributes.P_STREETNAME);
	            
	            sim.getFunction(6).setSaKey(SearchAttributes.P_STREETUNIT_CLEANED);
	            sim.getFunction(7).setSaKey(SearchAttributes.LD_PARCELNO);
	            sim.getFunction(8).setSaKey(SearchAttributes.OWNER_LFM_NAME);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);

		}
		// ADVANCED SEARCH
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(TSServerInfo.ADDRESS_MODULE_IDX, 74);
			sim.setName("AdvancedSearch");
			sim.setDestinationPage("/ISI/search/Search.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_SEARCH_BY_NAME);
			sim.setSearchType("CS");

			PageZone pz = new PageZone("AdvancedSearch", "Advanced Search", ORIENTATION_HORIZONTAL, null, 850, 50, PIXELS , true);

			try{			
	            HTMLControl 
	            stfr  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 3, sim.getFunction(0), "txtStreetNumberFrom", "(1) From #", null, searchId),
	            stto  = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1,  1, 3, sim.getFunction(1), "txtStreetNumberTo",   "To #",       null, searchId),
	            stpr  = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  1,  1, 3, sim.getFunction(2), "txtStreetDirPre",     "Pre Dir",    null, searchId),	            
	            stna  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2,  2, 25,sim.getFunction(3), "txtStreetName",       "St. Name",   null, searchId),
	            stpo  = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2,  2, 3, sim.getFunction(4), "txtStreetDirPost",    "Post Dir",   null, searchId),
	            stds  = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  2,  2, 3, sim.getFunction(5), "txtStreetDesc",       "Designator", "",   searchId),	            
	            unit  = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  2,  2, 3, sim.getFunction(6), "txtAddressUnit",      "Unit",null, searchId),

	            stfr2 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3,  3, 3, sim.getFunction(7), "txtStreetNumberFrom2","(2) From #", null, searchId),
	            stto2 = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  3,  3, 3, sim.getFunction(8), "txtStreetNumberTo2",  "To #",       null, searchId),
	            stpr2 = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  3,  3, 3, sim.getFunction(9), "txtStreetDirPre2",    "Pre Dir",    null, searchId),	            
	            stna2 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4,  4, 25,sim.getFunction(10), "txtStreetName2",      "St. Name",   null, searchId),
	            stpo2 = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  4,  4, 3, sim.getFunction(11), "txtStreetDirPost2",   "Post Dir",   null, searchId),
	            stds2 = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  4,  4, 3, sim.getFunction(12), "txtStreetDesc2",      "Designator", "",   searchId),	            
	            unit2 = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  4,  4, 3, sim.getFunction(13), "txtAddressUnit2",     "Unit",null, searchId),

	            stfr3 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  5,  5, 3, sim.getFunction(14), "txtStreetNumberFrom3","(3) From #", null, searchId),
	            stto3 = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  5,  5, 3, sim.getFunction(15), "txtStreetNumberTo3",  "To #",       null, searchId),
	            stpr3 = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  5,  5, 3, sim.getFunction(16), "txtStreetDirPre3",    "Pre Dir",    null, searchId),	            
	            stna3 = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  6,  6, 25,sim.getFunction(17), "txtStreetName3",      "St. Name",   null, searchId),
	            stpo3 = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  6,  6, 3, sim.getFunction(18), "txtStreetDirPost3",   "Post Dir",   null, searchId),
	            stds3 = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  6,  6, 3, sim.getFunction(19), "txtStreetDesc3",      "Designator", "",   searchId),	            
	            unit3 = new HTMLControl(HTML_TEXT_FIELD, 4, 4,  6,  6, 3, sim.getFunction(20), "txtAddressUnit3",     "Unit",null, searchId),
	            
	            city  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  7,  7, 25,sim.getFunction(21), "txtCity",             "City",       null, searchId),
	            zip   = new HTMLControl(HTML_SELECT_BOX, 2, 4,  7,  7, 25,sim.getFunction(22), "ddlZip",              "Zip",        null, searchId),
	            
	            pin   = new HTMLControl(HTML_TEXT_FIELD, 1, 4,  8,  8, 18,sim.getFunction(23), "txtPIN",              "(1) PIN",    null, searchId),
	            pin2  = new HTMLControl(HTML_TEXT_FIELD, 1, 4,  9,  9, 18,sim.getFunction(24), "txtPin2",             "(2) PIN",    null, searchId),
	            pin3  = new HTMLControl(HTML_TEXT_FIELD, 1, 4, 10, 10, 18,sim.getFunction(25), "txtPin3",             "(3) PIN",    null, searchId),
	            pin4  = new HTMLControl(HTML_TEXT_FIELD, 1, 4, 11, 11, 18,sim.getFunction(26), "txtPin4",             "(4) PIN",    null, searchId),
	            pin5  = new HTMLControl(HTML_TEXT_FIELD, 1, 4, 12, 12, 18,sim.getFunction(27), "txtPin5",             "(5) PIN",    null, searchId),
	            pin6  = new HTMLControl(HTML_TEXT_FIELD, 1, 4, 13, 13, 18,sim.getFunction(28), "txtPin6",             "(6) PIN",    null, searchId),

	            own   = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 14, 14, 35,sim.getFunction(29), "txtOwner",            "(1) Owner",  null, searchId),	
	            own2  = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 15, 15, 35,sim.getFunction(30), "txtOwner2",           "(2) Owner",  null, searchId),
	            own3  = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 16, 16, 35,sim.getFunction(31), "txtOwner3",           "(3) Owner",  null, searchId),
	            
	            oos   = new HTMLControl(HTML_CHECK_BOX,  3, 3, 16, 16, 35,sim.getFunction(32), "cbOutOfStateOwners",  "Only_Out_of_State_Owners",  "", searchId),
	            
	            luc   = new HTMLControl(HTML_SELECT_BOX, 1, 4, 17, 17, 35,sim.getFunction(33), "ddlLandUseCodes",     "Ld Use Cod",  null, searchId),
	            sch   = new HTMLControl(HTML_SELECT_BOX, 1, 2, 18, 18, 35,sim.getFunction(34), "ddlSchool",           "School",         null, searchId),
	            ooc   = new HTMLControl(HTML_SELECT_BOX, 3, 3, 15, 15, 35,sim.getFunction(35), "ddlOwnerOcc",         "Owner_Occupied", null, searchId),
	            
	            avf   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 19, 19, 15,sim.getFunction(36), "txtAssessedValueFrom","Ass Val Fr", "", searchId),
	            ivf   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 19, 19, 15,sim.getFunction(37), "txtImprValueFrom",    "Impr Val Fr", "", searchId),
	            ybf   = new HTMLControl(HTML_TEXT_FIELD, 3, 4, 19, 19, 15,sim.getFunction(38), "txtYearBuiltFrom",    "Yr Built Fr", "", searchId),

	            avt   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 20, 20, 15,sim.getFunction(39), "txtAssessedValueTo",  "Ass Val To",   "", searchId),            
	            ivt   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 20, 20, 15,sim.getFunction(40), "txtImprValueTo",      "Impr Val To",   "", searchId),            
	            ybt   = new HTMLControl(HTML_TEXT_FIELD, 3, 4, 20, 20, 15,sim.getFunction(41), "txtYearBuiltTo",      "Yr Built To",   "", searchId),

	            isqf  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 21, 21, 15,sim.getFunction(42), "txtImprSqFtFrom",     "Impr sqft. Fr", "", searchId),
	            tpf   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 21, 21, 15,sim.getFunction(43), "txtTaxPaymentFrom",   "Tax Pay Fr", "", searchId),	            

	            isqt  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 22, 22, 15,sim.getFunction(44), "txtImprSqFtTo",       "Impr sqft To",   "", searchId),
	            tpt   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 22, 22, 15,sim.getFunction(45), "txtTaxPaymentTo",     "Tax Pay To",   "", searchId),
	            
	            mmf   = new HTMLControl(HTML_SELECT_BOX, 1, 1, 23, 23, 15,sim.getFunction(46), "ddlMortMonFrom",      "(1) Mort Fr", "", searchId),
	            mdf   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 23, 23, 15,sim.getFunction(47), "txtMortDayFrom",      "Mort Day Fr",   "", searchId),
	            myf   = new HTMLControl(HTML_TEXT_FIELD, 3, 4, 23, 23, 15,sim.getFunction(48), "txtMortYearFrom",     "Mort Yr Fr",  "", searchId),	            
	            mmt   = new HTMLControl(HTML_SELECT_BOX, 1, 1, 24, 24, 15,sim.getFunction(49), "ddlMortMonTo",        "Mort To", "", searchId),
	            mdt   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 24, 24, 15,sim.getFunction(50), "txtMortDayTo",        "Mort Day To",   "", searchId),
	            myt   = new HTMLControl(HTML_TEXT_FIELD, 3, 4, 24, 24, 15,sim.getFunction(51), "txtMortYearTo",       "Mort Yr To",  "", searchId),	           	            
	            mln   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 25, 25, 15,sim.getFunction(52), "txtLender",           "Lender",  "", searchId),
	            mt    = new HTMLControl(HTML_SELECT_BOX, 2, 4, 25, 25, 15,sim.getFunction(53), "ddlMortType",         "Mort Type",  "", searchId),
	            
	            mmf2  = new HTMLControl(HTML_SELECT_BOX, 1, 1, 26, 26, 15,sim.getFunction(54), "ddlMortMonFrom2",     "(2) Mort Fr", "", searchId),
	            mdf2  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 26, 26, 15,sim.getFunction(55), "txtMortDayFrom2",     "Mort Day Fr",   "", searchId),
	            myf2  = new HTMLControl(HTML_TEXT_FIELD, 3, 4, 26, 26, 15,sim.getFunction(56), "txtMortYearFrom2",    "Mort Yr Fr",  "", searchId),	            
	            mmt2  = new HTMLControl(HTML_SELECT_BOX, 1, 1, 27, 27, 15,sim.getFunction(57), "ddlMortMonTo2",       "Mort To", "", searchId),
	            mdt2  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 27, 27, 15,sim.getFunction(58), "txtMortDayTo2",       "Mort Day To",   "", searchId),
	            myt2  = new HTMLControl(HTML_TEXT_FIELD, 3, 4, 27, 27, 15,sim.getFunction(59), "txtMortYearTo2",      "Mort Yr To",  "", searchId),	           	            
	            mln2  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 28, 28, 15,sim.getFunction(60), "txtLender2",          "Lender",  "", searchId),
	            mt2   = new HTMLControl(HTML_SELECT_BOX, 2, 4, 28, 28, 15,sim.getFunction(61), "ddlMortType2",        "Mort Type",  "", searchId),
	            
	            md    = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 29, 29, 15,sim.getFunction(62), "txtMortDoc",          "Mort Doc",  "", searchId),
	            
	            vlf   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 30, 30, 15,sim.getFunction(63), "txtLandValueFrom",    "Land Val Fr",  "", searchId),
	            acf   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 30, 30, 15,sim.getFunction(64), "txtAcresFrom",        "Acres Fr",  "", searchId),
	            
	            vlt   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 31, 31, 15,sim.getFunction(65), "txtLandValueTo",      "Land Val To",  "", searchId),
	            act   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 31, 31, 15,sim.getFunction(66), "txtAcresTo",          "Acres To",  "", searchId),
	            
	            bdr   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 32, 32, 15,sim.getFunction(67), "txtBedRoom",          "Bedrooms",  "", searchId),
	            bth   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 32, 32, 15,sim.getFunction(68), "txtBathRoom",         "Full Bath",  "", searchId),
	            str   = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 32, 32, 15,sim.getFunction(69), "txtStories",          "Stories",  "", searchId),
	            
	            twn   = new HTMLControl(HTML_SELECT_BOX, 1, 3, 33, 33, 15,sim.getFunction(70), "ddlTownship",         "Township",  "", searchId),
	            
	            ss    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(71), "txtSavedSearch",      "", "",       searchId),
	            cnt   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(72), "ddlCounty",           "", "31",     searchId),
	            btn   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(73), "btnSearch",           "", "Search", searchId);
	            	    	            
	            setupSelectBox(sim.getFunction(22), ZIP_SELECT);	            
	            setupSelectBox(sim.getFunction(33), LAND_USE_SELECT);	            
	            setupSelectBox(sim.getFunction(34), SCHOOL_SELECT);
	            setupSelectBox(sim.getFunction(35), OWNER_OCCUPIED_SELECT);
	            
	            setupSelectBox(sim.getFunction(46), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonFrom"));
	            setupSelectBox(sim.getFunction(49), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonTo"));
	            setupSelectBox(sim.getFunction(53), MORT_TYPE_SELECT.replace("@@NAME@@", "ddlMortType"));
	            
	            setupSelectBox(sim.getFunction(54), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonFrom2"));
	            setupSelectBox(sim.getFunction(57), MONTH_SELECT.replace("@@NAME@@", "ddlMortMonTo2"));
	            setupSelectBox(sim.getFunction(61), MORT_TYPE_SELECT.replace("@@NAME@@", "ddlMortType2"));
	            setupSelectBox(sim.getFunction(70), TOWNSHIP_SELECT);
	            
	            pin.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            pin2.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            pin3.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            pin4.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            pin5.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            pin6.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            
	            own.setFieldNote("(e.g SMITH JOHN)");
	            own2.setFieldNote("(e.g SMITH JOHN)");
	            own3.setFieldNote("(e.g SMITH JOHN)");
	            

	            sim.getFunction(32).setParamType(TSServerInfoFunction.idCheckBox);
	            sim.getFunction(32).setHtmlformat(
						"<INPUT TYPE=\"Checkbox\" NAME=\"parcel_origin\" CHECKED VALUE=\"P\">");

				
	            setHiddenParamMulti(true, ss, cnt, btn);	
	            setRequiredCriticalMulti(true);
	            setJustifyFieldMulti(false,  
	            		stfr, stto, stpr, stna, stpo, stds, unit, 
	            		stfr2, stto2, stpr2, stna2, stpo2, stds2, unit2,
	            		stfr3, stto3, stpr3, stna3, stpo3, stds3, unit3,
	            		city, zip,
	            		pin, pin2, pin3, pin4, pin5, pin6,
	            		own, own2, own3,
	            		oos, ooc, 
	            		luc, sch, 
	            		avf, avt, ivf, ivt, ybf, ybt, isqf, isqt, tpf, tpt,
	            		mmf, mdf, myf, mmt, mdt, myt, mln, mt,
	            		mmf2, mdf2, myf2, mmt2, mdt2, myt2, mln2, mt2,
	            		md,
	    	            vlf, acf, vlt, act, 
	    	            bdr, bth, str, 
	    	            twn,
	            		ss, cnt, btn
	            );
	            
	            pz.addHTMLObjectMulti(
	            		stfr, stto, stpr, stna, stpo, stds, unit, 
	            		stfr2, stto2, stpr2, stna2, stpo2, stds2, unit2,
	            		stfr3, stto3, stpr3, stna3, stpo3, stds3, unit3,
	            		city, zip,
	            		pin, pin2, pin3, pin4, pin5, pin6,
	            		own, own2, own3,
	            		oos, ooc, 
	            		luc, sch, 
	            		avf, avt, ivf, ivt, ybf, ybt, isqf, isqt, tpf, tpt,
	            		mmf, mdf, myf, mmt, mdt, myt, mln, mt,
	            		mmf2, mdf2, myf2, mmt2, mdt2, myt2, mln2, mt2,
	            		md,
	    	            vlf, acf, vlt, act, 
	    	            bdr, bth, str, 
	    	            twn,
	            		ss, cnt, btn);
	            
	            
	            sim.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
	            sim.getFunction(1).setSaKey(SearchAttributes.P_STREETNO);
	            sim.getFunction(2).setSaKey(SearchAttributes.P_STREETDIRECTION_ABBREV);
	            sim.getFunction(3).setSaKey(SearchAttributes.P_STREETNAME);	            
	            sim.getFunction(6).setSaKey(SearchAttributes.P_STREETUNIT_CLEANED);	            
	            sim.getFunction(21).setSaKey(SearchAttributes.P_CITY);
	            sim.getFunction(23).setSaKey(SearchAttributes.LD_PARCELNO);
	            sim.getFunction(29).setSaKey(SearchAttributes.OWNER_LFM_NAME);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);

		}

		
		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		
		return si;	
	}

	private static final String ZIP_SELECT =
		"<select name=\"ddlZip\" size=\"1\">" +
		"<option value=\" No Selection \"> No Selection </option>" +
		"<option value=\"\"></option>" +
		"<option value=\"000\\N\">000\\N</option>" +
		"<option value=\"00000\">00000</option>" +
		"<option value=\"00060\">00060</option>" +
		"<option value=\"000IL\">000IL</option>" +
		"<option value=\"00606\">00606</option>" +
		"<option value=\"00707\">00707</option>" +
		"<option value=\"02052\">02052</option>" +
		"<option value=\"02108\">02108</option>" +
		"<option value=\"06035\">06035</option>" +
		"<option value=\"06152\">06152</option>" +
		"<option value=\"07094\">07094</option>" +
		"<option value=\"07470\">07470</option>" +
		"<option value=\"10017\">10017</option>" +
		"<option value=\"10026\">10026</option>" +
		"<option value=\"10167\">10167</option>" +
		"<option value=\"10580\">10580</option>" +
		"<option value=\"19007\">19007</option>" +
		"<option value=\"19046\">19046</option>" +
		"<option value=\"20165\">20165</option>" +
		"<option value=\"20814\">20814</option>" +
		"<option value=\"20817\">20817</option>" +
		"<option value=\"20910\">20910</option>" +
		"<option value=\"21201\">21201</option>" +
		"<option value=\"23236\">23236</option>" +
		"<option value=\"23606\">23606</option>" +
		"<option value=\"23608\">23608</option>" +
		"<option value=\"25091\">25091</option>" +
		"<option value=\"28753\">28753</option>" +
		"<option value=\"30096\">30096</option>" +
		"<option value=\"30161\">30161</option>" +
		"<option value=\"30193\">30193</option>" +
		"<option value=\"32037\">32037</option>" +
		"<option value=\"32082\">32082</option>" +
		"<option value=\"32507\">32507</option>" +
		"<option value=\"32801\">32801</option>" +
		"<option value=\"32803\">32803</option>" +
		"<option value=\"32826\">32826</option>" +
		"<option value=\"32962\">32962</option>" +
		"<option value=\"33157\">33157</option>" +
		"<option value=\"33166\">33166</option>" +
		"<option value=\"33178\">33178</option>" +
		"<option value=\"33432\">33432</option>" +
		"<option value=\"33477\">33477</option>" +
		"<option value=\"33610\">33610</option>" +
		"<option value=\"35106\">35106</option>" +
		"<option value=\"35222\">35222</option>" +
		"<option value=\"37211\">37211</option>" +
		"<option value=\"38111\">38111</option>" +
		"<option value=\"43082\">43082</option>" +
		"<option value=\"43221\">43221</option>" +
		"<option value=\"44023\">44023</option>" +
		"<option value=\"44122\">44122</option>" +
		"<option value=\"44306\">44306</option>" +
		"<option value=\"45242\">45242</option>" +
		"<option value=\"45865\">45865</option>" +
		"<option value=\"46307\">46307</option>" +
		"<option value=\"46319\">46319</option>" +
		"<option value=\"46368\">46368</option>" +
		"<option value=\"46375\">46375</option>" +
		"<option value=\"46385\">46385</option>" +
		"<option value=\"46403\">46403</option>" +
		"<option value=\"47904\">47904</option>" +
		"<option value=\"48025\">48025</option>" +
		"<option value=\"48047\">48047</option>" +
		"<option value=\"48086\">48086</option>" +
		"<option value=\"48302\">48302</option>" +
		"<option value=\"48326\">48326</option>" +
		"<option value=\"48334\">48334</option>" +
		"<option value=\"48505\">48505</option>" +
		"<option value=\"49103\">49103</option>" +
		"<option value=\"49127\">49127</option>" +
		"<option value=\"49203\">49203</option>" +
		"<option value=\"49428\">49428</option>" +
		"<option value=\"50456\">50456</option>" +
		"<option value=\"50519\">50519</option>" +
		"<option value=\"50613\">50613</option>" +
		"<option value=\"50631\">50631</option>" +
		"<option value=\"50632\">50632</option>" +
		"<option value=\"53125\">53125</option>" +
		"<option value=\"53140\">53140</option>" +
		"<option value=\"53211\">53211</option>" +
		"<option value=\"53402\">53402</option>" +
		"<option value=\"53593\">53593</option>" +
		"<option value=\"53719\">53719</option>" +
		"<option value=\"53946\">53946</option>" +
		"<option value=\"54211\">54211</option>" +
		"<option value=\"54615\">54615</option>" +
		"<option value=\"54703\">54703</option>" +
		"<option value=\"54836\">54836</option>" +
		"<option value=\"55112\">55112</option>" +
		"<option value=\"55364\">55364</option>" +
		"<option value=\"55402\">55402</option>" +
		"<option value=\"55447\">55447</option>" +
		"<option value=\"60002\">60002</option>" +
		"<option value=\"60004\">60004</option>" +
		"<option value=\"60005\">60005</option>" +
		"<option value=\"60006\">60006</option>" +
		"<option value=\"60007\">60007</option>" +
		"<option value=\"60008\">60008</option>" +
		"<option value=\"60009\">60009</option>" +
		"<option value=\"60010\">60010</option>" +
		"<option value=\"60012\">60012</option>" +
		"<option value=\"60013\">60013</option>" +
		"<option value=\"60014\">60014</option>" +
		"<option value=\"60015\">60015</option>" +
		"<option value=\"60016\">60016</option>" +
		"<option value=\"60017\">60017</option>" +
		"<option value=\"60018\">60018</option>" +
		"<option value=\"60019\">60019</option>" +
		"<option value=\"60020\">60020</option>" +
		"<option value=\"60022\">60022</option>" +
		"<option value=\"60023\">60023</option>" +
		"<option value=\"60025\">60025</option>" +
		"<option value=\"60026\">60026</option>" +
		"<option value=\"60027\">60027</option>" +
		"<option value=\"60028\">60028</option>" +
		"<option value=\"60029\">60029</option>" +
		"<option value=\"60030\">60030</option>" +
		"<option value=\"60031\">60031</option>" +
		"<option value=\"60034\">60034</option>" +
		"<option value=\"60035\">60035</option>" +
		"<option value=\"60036\">60036</option>" +
		"<option value=\"60038\">60038</option>" +
		"<option value=\"60039\">60039</option>" +
		"<option value=\"60040\">60040</option>" +
		"<option value=\"60041\">60041</option>" +
		"<option value=\"60042\">60042</option>" +
		"<option value=\"60043\">60043</option>" +
		"<option value=\"60045\">60045</option>" +
		"<option value=\"60046\">60046</option>" +
		"<option value=\"60047\">60047</option>" +
		"<option value=\"60048\">60048</option>" +
		"<option value=\"60050\">60050</option>" +
		"<option value=\"60051\">60051</option>" +
		"<option value=\"60052\">60052</option>" +
		"<option value=\"60053\">60053</option>" +
		"<option value=\"60054\">60054</option>" +
		"<option value=\"60056\">60056</option>" +
		"<option value=\"60057\">60057</option>" +
		"<option value=\"60058\">60058</option>" +
		"<option value=\"60059\">60059</option>" +
		"<option value=\"60060\">60060</option>" +
		"<option value=\"60061\">60061</option>" +
		"<option value=\"60062\">60062</option>" +
		"<option value=\"60064\">60064</option>" +
		"<option value=\"60065\">60065</option>" +
		"<option value=\"60067\">60067</option>" +
		"<option value=\"60068\">60068</option>" +
		"<option value=\"60069\">60069</option>" +
		"<option value=\"60070\">60070</option>" +
		"<option value=\"60072\">60072</option>" +
		"<option value=\"60073\">60073</option>" +
		"<option value=\"60074\">60074</option>" +
		"<option value=\"60075\">60075</option>" +
		"<option value=\"60076\">60076</option>" +
		"<option value=\"60077\">60077</option>" +
		"<option value=\"60078\">60078</option>" +
		"<option value=\"60080\">60080</option>" +
		"<option value=\"60082\">60082</option>" +
		"<option value=\"60083\">60083</option>" +
		"<option value=\"60084\">60084</option>" +
		"<option value=\"60086\">60086</option>" +
		"<option value=\"60087\">60087</option>" +
		"<option value=\"60089\">60089</option>" +
		"<option value=\"60090\">60090</option>" +
		"<option value=\"60091\">60091</option>" +
		"<option value=\"60093\">60093</option>" +
		"<option value=\"60094\">60094</option>" +
		"<option value=\"60095\">60095</option>" +
		"<option value=\"60097\">60097</option>" +
		"<option value=\"60099\">60099</option>" +
		"<option value=\"60101\">60101</option>" +
		"<option value=\"60102\">60102</option>" +
		"<option value=\"60103\">60103</option>" +
		"<option value=\"60104\">60104</option>" +
		"<option value=\"60105\">60105</option>" +
		"<option value=\"60106\">60106</option>" +
		"<option value=\"60107\">60107</option>" +
		"<option value=\"60108\">60108</option>" +
		"<option value=\"60110\">60110</option>" +
		"<option value=\"60112\">60112</option>" +
		"<option value=\"60113\">60113</option>" +
		"<option value=\"60115\">60115</option>" +
		"<option value=\"60116\">60116</option>" +
		"<option value=\"60118\">60118</option>" +
		"<option value=\"60120\">60120</option>" +
		"<option value=\"60121\">60121</option>" +
		"<option value=\"60123\">60123</option>" +
		"<option value=\"60124\">60124</option>" +
		"<option value=\"60126\">60126</option>" +
		"<option value=\"60128\">60128</option>" +
		"<option value=\"60129\">60129</option>" +
		"<option value=\"60130\">60130</option>" +
		"<option value=\"60131\">60131</option>" +
		"<option value=\"60132\">60132</option>" +
		"<option value=\"60133\">60133</option>" +
		"<option value=\"60136\">60136</option>" +
		"<option value=\"60137\">60137</option>" +
		"<option value=\"60139\">60139</option>" +
		"<option value=\"60140\">60140</option>" +
		"<option value=\"60142\">60142</option>" +
		"<option value=\"60143\">60143</option>" +
		"<option value=\"60145\">60145</option>" +
		"<option value=\"60148\">60148</option>" +
		"<option value=\"60149\">60149</option>" +
		"<option value=\"60151\">60151</option>" +
		"<option value=\"60152\">60152</option>" +
		"<option value=\"60153\">60153</option>" +
		"<option value=\"60154\">60154</option>" +
		"<option value=\"60155\">60155</option>" +
		"<option value=\"60156\">60156</option>" +
		"<option value=\"60157\">60157</option>" +
		"<option value=\"60159\">60159</option>" +
		"<option value=\"60160\">60160</option>" +
		"<option value=\"60161\">60161</option>" +
		"<option value=\"60162\">60162</option>" +
		"<option value=\"60163\">60163</option>" +
		"<option value=\"60164\">60164</option>" +
		"<option value=\"60165\">60165</option>" +
		"<option value=\"60166\">60166</option>" +
		"<option value=\"60168\">60168</option>" +
		"<option value=\"60169\">60169</option>" +
		"<option value=\"60170\">60170</option>" +
		"<option value=\"60171\">60171</option>" +
		"<option value=\"60172\">60172</option>" +
		"<option value=\"60173\">60173</option>" +
		"<option value=\"60174\">60174</option>" +
		"<option value=\"60175\">60175</option>" +
		"<option value=\"60176\">60176</option>" +
		"<option value=\"60177\">60177</option>" +
		"<option value=\"60179\">60179</option>" +
		"<option value=\"60181\">60181</option>" +
		"<option value=\"60183\">60183</option>" +
		"<option value=\"60185\">60185</option>" +
		"<option value=\"60186\">60186</option>" +
		"<option value=\"60187\">60187</option>" +
		"<option value=\"60188\">60188</option>" +
		"<option value=\"60189\">60189</option>" +
		"<option value=\"60190\">60190</option>" +
		"<option value=\"60191\">60191</option>" +
		"<option value=\"60192\">60192</option>" +
		"<option value=\"60193\">60193</option>" +
		"<option value=\"60194\">60194</option>" +
		"<option value=\"60195\">60195</option>" +
		"<option value=\"60196\">60196</option>" +
		"<option value=\"60197\">60197</option>" +
		"<option value=\"60198\">60198</option>" +
		"<option value=\"60201\">60201</option>" +
		"<option value=\"60202\">60202</option>" +
		"<option value=\"60203\">60203</option>" +
		"<option value=\"60204\">60204</option>" +
		"<option value=\"60208\">60208</option>" +
		"<option value=\"60209\">60209</option>" +
		"<option value=\"60225\">60225</option>" +
		"<option value=\"60267\">60267</option>" +
		"<option value=\"60287\">60287</option>" +
		"<option value=\"60301\">60301</option>" +
		"<option value=\"60302\">60302</option>" +
		"<option value=\"60303\">60303</option>" +
		"<option value=\"60304\">60304</option>" +
		"<option value=\"60305\">60305</option>" +
		"<option value=\"60307\">60307</option>" +
		"<option value=\"60308\">60308</option>" +
		"<option value=\"60341\">60341</option>" +
		"<option value=\"60363\">60363</option>" +
		"<option value=\"60401\">60401</option>" +
		"<option value=\"60402\">60402</option>" +
		"<option value=\"60403\">60403</option>" +
		"<option value=\"60405\">60405</option>" +
		"<option value=\"60406\">60406</option>" +
		"<option value=\"60407\">60407</option>" +
		"<option value=\"60408\">60408</option>" +
		"<option value=\"60409\">60409</option>" +
		"<option value=\"60411\">60411</option>" +
		"<option value=\"60412\">60412</option>" +
		"<option value=\"60415\">60415</option>" +
		"<option value=\"60416\">60416</option>" +
		"<option value=\"60417\">60417</option>" +
		"<option value=\"60419\">60419</option>" +
		"<option value=\"60420\">60420</option>" +
		"<option value=\"60421\">60421</option>" +
		"<option value=\"60422\">60422</option>" +
		"<option value=\"60423\">60423</option>" +
		"<option value=\"60424\">60424</option>" +
		"<option value=\"60425\">60425</option>" +
		"<option value=\"60426\">60426</option>" +
		"<option value=\"60427\">60427</option>" +
		"<option value=\"60428\">60428</option>" +
		"<option value=\"60429\">60429</option>" +
		"<option value=\"60430\">60430</option>" +
		"<option value=\"60431\">60431</option>" +
		"<option value=\"60432\">60432</option>" +
		"<option value=\"60433\">60433</option>" +
		"<option value=\"60435\">60435</option>" +
		"<option value=\"60436\">60436</option>" +
		"<option value=\"60437\">60437</option>" +
		"<option value=\"60438\">60438</option>" +
		"<option value=\"60439\">60439</option>" +
		"<option value=\"60440\">60440</option>" +
		"<option value=\"60441\">60441</option>" +
		"<option value=\"60442\">60442</option>" +
		"<option value=\"60443\">60443</option>" +
		"<option value=\"60445\">60445</option>" +
		"<option value=\"60446\">60446</option>" +
		"<option value=\"60447\">60447</option>" +
		"<option value=\"60448\">60448</option>" +
		"<option value=\"60449\">60449</option>" +
		"<option value=\"60450\">60450</option>" +
		"<option value=\"60451\">60451</option>" +
		"<option value=\"60452\">60452</option>" +
		"<option value=\"60453\">60453</option>" +
		"<option value=\"60454\">60454</option>" +
		"<option value=\"60455\">60455</option>" +
		"<option value=\"60456\">60456</option>" +
		"<option value=\"60457\">60457</option>" +
		"<option value=\"60458\">60458</option>" +
		"<option value=\"60459\">60459</option>" +
		"<option value=\"60460\">60460</option>" +
		"<option value=\"60461\">60461</option>" +
		"<option value=\"60462\">60462</option>" +
		"<option value=\"60463\">60463</option>" +
		"<option value=\"60464\">60464</option>" +
		"<option value=\"60465\">60465</option>" +
		"<option value=\"60466\">60466</option>" +
		"<option value=\"60467\">60467</option>" +
		"<option value=\"60468\">60468</option>" +
		"<option value=\"60469\">60469</option>" +
		"<option value=\"60471\">60471</option>" +
		"<option value=\"60472\">60472</option>" +
		"<option value=\"60473\">60473</option>" +
		"<option value=\"60474\">60474</option>" +
		"<option value=\"60475\">60475</option>" +
		"<option value=\"60476\">60476</option>" +
		"<option value=\"60477\">60477</option>" +
		"<option value=\"60478\">60478</option>" +
		"<option value=\"60479\">60479</option>" +
		"<option value=\"60480\">60480</option>" +
		"<option value=\"60482\">60482</option>" +
		"<option value=\"60483\">60483</option>" +
		"<option value=\"60485\">60485</option>" +
		"<option value=\"60487\">60487</option>" +
		"<option value=\"60490\">60490</option>" +
		"<option value=\"60491\">60491</option>" +
		"<option value=\"60498\">60498</option>" +
		"<option value=\"60499\">60499</option>" +
		"<option value=\"60501\">60501</option>" +
		"<option value=\"60504\">60504</option>" +
		"<option value=\"60505\">60505</option>" +
		"<option value=\"60506\">60506</option>" +
		"<option value=\"60507\">60507</option>" +
		"<option value=\"60510\">60510</option>" +
		"<option value=\"60513\">60513</option>" +
		"<option value=\"60514\">60514</option>" +
		"<option value=\"60515\">60515</option>" +
		"<option value=\"60516\">60516</option>" +
		"<option value=\"60517\">60517</option>" +
		"<option value=\"60518\">60518</option>" +
		"<option value=\"60521\">60521</option>" +
		"<option value=\"60522\">60522</option>" +
		"<option value=\"60523\">60523</option>" +
		"<option value=\"60524\">60524</option>" +
		"<option value=\"60525\">60525</option>" +
		"<option value=\"60526\">60526</option>" +
		"<option value=\"60527\">60527</option>" +
		"<option value=\"60529\">60529</option>" +
		"<option value=\"60531\">60531</option>" +
		"<option value=\"60532\">60532</option>" +
		"<option value=\"60534\">60534</option>" +
		"<option value=\"60535\">60535</option>" +
		"<option value=\"60537\">60537</option>" +
		"<option value=\"60538\">60538</option>" +
		"<option value=\"60540\">60540</option>" +
		"<option value=\"60542\">60542</option>" +
		"<option value=\"60543\">60543</option>" +
		"<option value=\"60544\">60544</option>" +
		"<option value=\"60546\">60546</option>" +
		"<option value=\"60547\">60547</option>" +
		"<option value=\"60548\">60548</option>" +
		"<option value=\"60549\">60549</option>" +
		"<option value=\"60552\">60552</option>" +
		"<option value=\"60553\">60553</option>" +
		"<option value=\"60555\">60555</option>" +
		"<option value=\"60557\">60557</option>" +
		"<option value=\"60558\">60558</option>" +
		"<option value=\"60559\">60559</option>" +
		"<option value=\"60561\">60561</option>" +
		"<option value=\"60563\">60563</option>" +
		"<option value=\"60564\">60564</option>" +
		"<option value=\"60565\">60565</option>" +
		"<option value=\"60585\">60585</option>" +
		"<option value=\"60600\">60600</option>" +
		"<option value=\"60601\">60601</option>" +
		"<option value=\"60602\">60602</option>" +
		"<option value=\"60603\">60603</option>" +
		"<option value=\"60604\">60604</option>" +
		"<option value=\"60605\">60605</option>" +
		"<option value=\"60606\">60606</option>" +
		"<option value=\"60607\">60607</option>" +
		"<option value=\"60608\">60608</option>" +
		"<option value=\"60609\">60609</option>" +
		"<option value=\"60610\">60610</option>" +
		"<option value=\"60611\">60611</option>" +
		"<option value=\"60612\">60612</option>" +
		"<option value=\"60613\">60613</option>" +
		"<option value=\"60614\">60614</option>" +
		"<option value=\"60615\">60615</option>" +
		"<option value=\"60616\">60616</option>" +
		"<option value=\"60617\">60617</option>" +
		"<option value=\"60618\">60618</option>" +
		"<option value=\"60619\">60619</option>" +
		"<option value=\"60620\">60620</option>" +
		"<option value=\"60621\">60621</option>" +
		"<option value=\"60622\">60622</option>" +
		"<option value=\"60623\">60623</option>" +
		"<option value=\"60624\">60624</option>" +
		"<option value=\"60625\">60625</option>" +
		"<option value=\"60626\">60626</option>" +
		"<option value=\"60627\">60627</option>" +
		"<option value=\"60628\">60628</option>" +
		"<option value=\"60629\">60629</option>" +
		"<option value=\"60630\">60630</option>" +
		"<option value=\"60631\">60631</option>" +
		"<option value=\"60632\">60632</option>" +
		"<option value=\"60633\">60633</option>" +
		"<option value=\"60634\">60634</option>" +
		"<option value=\"60635\">60635</option>" +
		"<option value=\"60636\">60636</option>" +
		"<option value=\"60637\">60637</option>" +
		"<option value=\"60638\">60638</option>" +
		"<option value=\"60639\">60639</option>" +
		"<option value=\"60640\">60640</option>" +
		"<option value=\"60641\">60641</option>" +
		"<option value=\"60642\">60642</option>" +
		"<option value=\"60643\">60643</option>" +
		"<option value=\"60644\">60644</option>" +
		"<option value=\"60645\">60645</option>" +
		"<option value=\"60646\">60646</option>" +
		"<option value=\"60647\">60647</option>" +
		"<option value=\"60648\">60648</option>" +
		"<option value=\"60649\">60649</option>" +
		"<option value=\"60650\">60650</option>" +
		"<option value=\"60651\">60651</option>" +
		"<option value=\"60652\">60652</option>" +
		"<option value=\"60653\">60653</option>" +
		"<option value=\"60654\">60654</option>" +
		"<option value=\"60655\">60655</option>" +
		"<option value=\"60656\">60656</option>" +
		"<option value=\"60657\">60657</option>" +
		"<option value=\"60658\">60658</option>" +
		"<option value=\"60659\">60659</option>" +
		"<option value=\"60660\">60660</option>" +
		"<option value=\"60661\">60661</option>" +
		"<option value=\"60662\">60662</option>" +
		"<option value=\"60663\">60663</option>" +
		"<option value=\"60664\">60664</option>" +
		"<option value=\"60665\">60665</option>" +
		"<option value=\"60666\">60666</option>" +
		"<option value=\"60667\">60667</option>" +
		"<option value=\"60668\">60668</option>" +
		"<option value=\"60669\">60669</option>" +
		"<option value=\"60670\">60670</option>" +
		"<option value=\"60671\">60671</option>" +
		"<option value=\"60672\">60672</option>" +
		"<option value=\"60673\">60673</option>" +
		"<option value=\"60674\">60674</option>" +
		"<option value=\"60675\">60675</option>" +
		"<option value=\"60676\">60676</option>" +
		"<option value=\"60677\">60677</option>" +
		"<option value=\"60678\">60678</option>" +
		"<option value=\"60679\">60679</option>" +
		"<option value=\"60680\">60680</option>" +
		"<option value=\"60682\">60682</option>" +
		"<option value=\"60684\">60684</option>" +
		"<option value=\"60685\">60685</option>" +
		"<option value=\"60686\">60686</option>" +
		"<option value=\"60687\">60687</option>" +
		"<option value=\"60688\">60688</option>" +
		"<option value=\"60689\">60689</option>" +
		"<option value=\"60690\">60690</option>" +
		"<option value=\"60691\">60691</option>" +
		"<option value=\"60692\">60692</option>" +
		"<option value=\"60693\">60693</option>" +
		"<option value=\"60694\">60694</option>" +
		"<option value=\"60695\">60695</option>" +
		"<option value=\"60696\">60696</option>" +
		"<option value=\"60697\">60697</option>" +
		"<option value=\"60701\">60701</option>" +
		"<option value=\"60704\">60704</option>" +
		"<option value=\"60706\">60706</option>" +
		"<option value=\"60707\">60707</option>" +
		"<option value=\"60712\">60712</option>" +
		"<option value=\"60714\">60714</option>" +
		"<option value=\"60717\">60717</option>" +
		"<option value=\"60802\">60802</option>" +
		"<option value=\"60803\">60803</option>" +
		"<option value=\"60804\">60804</option>" +
		"<option value=\"60805\">60805</option>" +
		"<option value=\"60807\">60807</option>" +
		"<option value=\"60809\">60809</option>" +
		"<option value=\"60814\">60814</option>" +
		"<option value=\"60815\">60815</option>" +
		"<option value=\"60824\">60824</option>" +
		"<option value=\"60827\">60827</option>" +
		"<option value=\"60829\">60829</option>" +
		"<option value=\"60837\">60837</option>" +
		"<option value=\"60838\">60838</option>" +
		"<option value=\"60864\">60864</option>" +
		"<option value=\"60871\">60871</option>" +
		"<option value=\"60901\">60901</option>" +
		"<option value=\"60903\">60903</option>" +
		"<option value=\"60909\">60909</option>" +
		"<option value=\"60911\">60911</option>" +
		"<option value=\"60914\">60914</option>" +
		"<option value=\"60915\">60915</option>" +
		"<option value=\"60919\">60919</option>" +
		"<option value=\"60945\">60945</option>" +
		"<option value=\"61032\">61032</option>" +
		"<option value=\"61041\">61041</option>" +
		"<option value=\"61088\">61088</option>" +
		"<option value=\"61104\">61104</option>" +
		"<option value=\"61109\">61109</option>" +
		"<option value=\"61110\">61110</option>" +
		"<option value=\"61354\">61354</option>" +
		"<option value=\"61360\">61360</option>" +
		"<option value=\"61364\">61364</option>" +
		"<option value=\"61445\">61445</option>" +
		"<option value=\"61554\">61554</option>" +
		"<option value=\"61604\">61604</option>" +
		"<option value=\"61615\">61615</option>" +
		"<option value=\"61649\">61649</option>" +
		"<option value=\"61704\">61704</option>" +
		"<option value=\"61728\">61728</option>" +
		"<option value=\"61761\">61761</option>" +
		"<option value=\"61821\">61821</option>" +
		"<option value=\"61874\">61874</option>" +
		"<option value=\"61920\">61920</option>" +
		"<option value=\"62061\">62061</option>" +
		"<option value=\"62107\">62107</option>" +
		"<option value=\"62226\">62226</option>" +
		"<option value=\"62233\">62233</option>" +
		"<option value=\"62234\">62234</option>" +
		"<option value=\"62312\">62312</option>" +
		"<option value=\"62363\">62363</option>" +
		"<option value=\"62610\">62610</option>" +
		"<option value=\"62626\">62626</option>" +
		"<option value=\"62629\">62629</option>" +
		"<option value=\"62702\">62702</option>" +
		"<option value=\"62703\">62703</option>" +
		"<option value=\"62883\">62883</option>" +
		"<option value=\"62918\">62918</option>" +
		"<option value=\"62959\">62959</option>" +
		"<option value=\"63031\">63031</option>" +
		"<option value=\"63101\">63101</option>" +
		"<option value=\"63102\">63102</option>" +
		"<option value=\"63121\">63121</option>" +
		"<option value=\"64402\">64402</option>" +
		"<option value=\"64409\">64409</option>" +
		"<option value=\"64477\">64477</option>" +
		"<option value=\"65115\">65115</option>" +
		"<option value=\"65810\">65810</option>" +
		"<option value=\"66020\">66020</option>" +
		"<option value=\"66030\">66030</option>" +
		"<option value=\"66037\">66037</option>" +
		"<option value=\"66043\">66043</option>" +
		"<option value=\"66063\">66063</option>" +
		"<option value=\"66067\">66067</option>" +
		"<option value=\"66080\">66080</option>" +
		"<option value=\"66458\">66458</option>" +
		"<option value=\"66621\">66621</option>" +
		"<option value=\"69363\">69363</option>" +
		"<option value=\"69629\">69629</option>" +
		"<option value=\"70808\">70808</option>" +
		"<option value=\"75038\">75038</option>" +
		"<option value=\"75093\">75093</option>" +
		"<option value=\"75207\">75207</option>" +
		"<option value=\"75225\">75225</option>" +
		"<option value=\"75232\">75232</option>" +
		"<option value=\"75240\">75240</option>" +
		"<option value=\"75356\">75356</option>" +
		"<option value=\"76304\">76304</option>" +
		"<option value=\"77002\">77002</option>" +
		"<option value=\"77210\">77210</option>" +
		"<option value=\"77584\">77584</option>" +
		"<option value=\"77802\">77802</option>" +
		"<option value=\"80202\">80202</option>" +
		"<option value=\"80228\">80228</option>" +
		"<option value=\"80402\">80402</option>" +
		"<option value=\"85248\">85248</option>" +
		"<option value=\"85254\">85254</option>" +
		"<option value=\"85260\">85260</option>" +
		"<option value=\"86406\">86406</option>" +
		"<option value=\"89014\">89014</option>" +
		"<option value=\"89117\">89117</option>" +
		"<option value=\"89502\">89502</option>" +
		"<option value=\"90024\">90024</option>" +
		"<option value=\"90067\">90067</option>" +
		"<option value=\"90265\">90265</option>" +
		"<option value=\"90302\">90302</option>" +
		"<option value=\"90405\">90405</option>" +
		"<option value=\"90482\">90482</option>" +
		"<option value=\"90625\">90625</option>" +
		"<option value=\"90628\">90628</option>" +
		"<option value=\"90714\">90714</option>" +
		"<option value=\"91101\">91101</option>" +
		"<option value=\"91107\">91107</option>" +
		"<option value=\"91201\">91201</option>" +
		"<option value=\"91203\">91203</option>" +
		"<option value=\"91311\">91311</option>" +
		"<option value=\"91324\">91324</option>" +
		"<option value=\"91761\">91761</option>" +
		"<option value=\"91770\">91770</option>" +
		"<option value=\"91775\">91775</option>" +
		"<option value=\"92014\">92014</option>" +
		"<option value=\"92028\">92028</option>" +
		"<option value=\"92625\">92625</option>" +
		"<option value=\"92660\">92660</option>" +
		"<option value=\"92714\">92714</option>" +
		"<option value=\"92806\">92806</option>" +
		"<option value=\"92807\">92807</option>" +
		"<option value=\"92865\">92865</option>" +
		"<option value=\"93108\">93108</option>" +
		"<option value=\"94545\">94545</option>" +
		"<option value=\"94941\">94941</option>" +
		"<option value=\"95119\">95119</option>" +
		"<option value=\"96205\">96205</option>" +
		"<option value=\"97209\">97209</option>" +
		"<option value=\"97302\">97302</option>" +
		"<option value=\"98531\">98531</option>" +
	    "</select>";
	
	private static final String LAND_USE_SELECT = 
		"<select name=\"ddlLandUseCodes\" size=\"1\">" +
		"<option value=\"\"> No Selection</option>" +
		"<option value=\"1000\">EXEMPT - 1000</option>" +
		"<option value=\"2201\">RESIDENTIAL GARAGE - 2201</option>" +
		"<option value=\"2202\">1 STORY 0-999 SF - 2202</option>" +
		"<option value=\"2203\">1 STORY 1000-1800 SF - 2203</option>" +
		"<option value=\"2204\">1 STORY 1801+ SF - 2204</option>" +
		"<option value=\"2205\">2 STORY 62+ YRS OLD 0-2200 SF - 2205</option>" +
		"<option value=\"2206\">2 STORY 62+ YRS OLD 2201-4999 SF - 2206</option>" +
		"<option value=\"2207\">2 STORY 0-62 YRS OLD 0-2200 SF - 2207</option>" +
		"<option value=\"2208\">2 STORY 0-62 YRS OLD 3801-4999 SF - 2208</option>" +
		"<option value=\"2209\">2 STORY 5000+ SF - 2209</option>" +
		"<option value=\"2210\">ROW HOUSE 62+ YRS OLD - 2210</option>" +
		"<option value=\"2234\">SPLIT LEVEL RESIDENCE - 2234</option>" +
		"<option value=\"2278\">2 STORY 0-62 YRS OLD 2001-3800 SF - 2278</option>" +
		"<option value=\"2288\">HOME IMPROVEMENT EXEMPTION - 2288</option>" +
		"<option value=\"2290\">OTHER MINOR IMPROVEMENTS - 2290</option>" +
		"<option value=\"2295\">ROW HOUSE OR TOWNHOUSE 0-62 YRS OLD - 2295</option>" +
		"<option value=\"2297\">SPECIAL RESIDENTIAL IMPROVEMENTS - 2297</option>" +
		"<option value=\"2299\">CONDOMINIUM - 2299</option>" +
		"<option value=\"3211\">2-6 APARTMENTS 62+ YRS OLD - 3211</option>" +
		"<option value=\"3212\">2-6 APARTMENTS 0-62 YRS OLD - 3212</option>" +
		"<option value=\"3213\">MULTI STORY 2-6 APTS 5000+ SF - 3213</option>" +
		"<option value=\"3218\">COM/RES APTS ABOVE 6 UNITS UP TO 20 - 3218</option>" +
		"<option value=\"3219\">COM/RES APTS ABOVE 6 UNITS UP TO 20 - 3219</option>" +
		"<option value=\"3220\">COM/RES APTS ABOVE 6 UNITS UP TO 20 - 3220</option>" +
		"<option value=\"3221\">COM/RES APTS ABOVE 6 UNITS UP TO 20 - 3221</option>" +
		"<option value=\"3301\">APARTMENT GARAGE - 3301</option>" +
		"<option value=\"3313\">2-3 STORY APARTMENT 7+ UNITS - 3313</option>" +
		"<option value=\"3314\">2-3 STORY APTS EXT. ENTRANCE - 3314</option>" +
		"<option value=\"3315\">2-3 STORY APTS INT. ENTRANCE - 3315</option>" +
		"<option value=\"3390\">MINOR IMPROVEMENTS - RENTAL - 3390</option>" +
		"<option value=\"3391\">3+ STORY APARTMENTS - 3391</option>" +
		"<option value=\"3396\">ROW HOUSES 7+ UNITS - 3396</option>" +
		"<option value=\"3397\">SPECIAL RENTAL IMPROVEMENTS - 3397</option>" +
		"<option value=\"3399\">RENTAL CONDOMINIUMS 7+ UNITS - 3399</option>" +
		"<option value=\"3913\">2 OR 3 STORY BLDG - 3913</option>" +
		"<option value=\"3914\">2 OR 3 STORY APTS - 3914</option>" +
		"<option value=\"3915\">2 OR 3 STORY APTS - 3915</option>" +
		"<option value=\"3919\">2 OR 3 STORY OLD STYLE STORES APTS ABOVE - 3919</option>" +
		"<option value=\"3920\">2 OR 3 STORY MODERN INSIDE STORE APTS ABOVE - 3920</option>" +
		"<option value=\"3921\">CORNER STORE WITH APTS ABOVE - 3921</option>" +
		"<option value=\"3991\">APARTMENT BLDGS 3+ STORIES - 3991</option>" +
		"<option value=\"3996\">RENTED MODERN ROW HOUSES 7+UNITS - 3996</option>" +
		"<option value=\"3997\">SPECIAL RENTAL IMPROVEMENTS - 3997</option>" +
		"<option value=\"4318\">2-3 STORY FRAME STORE W/APTS - 4318</option>" +
		"<option value=\"4319\">2-3 STORY OLD STYLE STORE W/APTS - 4319</option>" +
		"<option value=\"4320\">2-3 STORY MODERN STORE W/APTS - 4320</option>" +
		"<option value=\"4321\">CORNER STORE/OFFICE W/APTS - 4321</option>" +
		"<option value=\"4501\">GARAGE ON COMMERCIAL PROPERTY - 4501</option>" +
		"<option value=\"4516\">APARTMENT HOTEL - 4516</option>" +
		"<option value=\"4517\">1 STORY STORE/MISC - 4517</option>" +
		"<option value=\"4522\">1 STORY PUBLIC BUILDING/GARAGE - 4522</option>" +
		"<option value=\"4523\">GAS STATION - 4523</option>" +
		"<option value=\"4526\">GREENHOUSE - 4526</option>" +
		"<option value=\"4527\">THEATER - 4527</option>" +
		"<option value=\"4528\">BANK BUILDING - 4528</option>" +
		"<option value=\"4529\">MOTEL - 4529</option>" +
		"<option value=\"4530\">SUPERMARKET - 4530</option>" +
		"<option value=\"4531\">SHOPPING CENTER - 4531</option>" +
		"<option value=\"4532\">BOWLING ALLEY - 4532</option>" +
		"<option value=\"4533\">BUTLER TYPE BUILDING - 4533</option>" +
		"<option value=\"4535\">GOLF COURSE LAND - 4535</option>" +
		"<option value=\"4536\">GOLF COURSE IMPROVEMENT - 4536</option>" +
		"<option value=\"4590\">MINOR IMPROVEMENTS - 4590</option>" +
		"<option value=\"4591\">COMMERCIAL BUILDING/OFFICE BLDG. - 4591</option>" +
		"<option value=\"4592\">2-3 STORY RETAIL AND/OR COMMERCIAL SPACE - 4592</option>" +
		"<option value=\"4597\">SPECIAL COMMERCIAL IMPROVEMENTS - 4597</option>" +
		"<option value=\"4599\">COMMERCIAL CONDOMINIUM UNITS - 4599</option>" +
		"<option value=\"4701\">GARAGE USED IN CONJ WITH COMML INCENTIVE IMPROV - 4701</option>" +
		"<option value=\"4716\">NON-FIREPROOF HOTEL OR ROOMING HOUSE (APT. HOTEL) - 4716</option>" +
		"<option value=\"4717\">1-STY RETAIL- REST- MEDICAL BLDG OR MISC COMML USE - 4717</option>" +
		"<option value=\"4722\">GARAGE PUBLIC/SERVICE - 4722</option>" +
		"<option value=\"4723\">GASOLINE STATION WITH/WITHOUT BAYS STORE - 4723</option>" +
		"<option value=\"4726\">COMMERCIAL GREENHOUSE - 4726</option>" +
		"<option value=\"4727\">THEATRES - 4727</option>" +
		"<option value=\"4728\">BANK BUILDINGS - 4728</option>" +
		"<option value=\"4729\">MOTELS - 4729</option>" +
		"<option value=\"4730\">SUPERMARKET - 4730</option>" +
		"<option value=\"4731\">SHOPPING CENTER - 4731</option>" +
		"<option value=\"4732\">BOWLING ALLEY - 4732</option>" +
		"<option value=\"4733\">QUONSET HUTS AND BUTLER TYPE BLDGS - 4733</option>" +
		"<option value=\"4735\">GOLF COURSE IMPROVEMENT - 4735</option>" +
		"<option value=\"4736\">GOLF COURSE LAND - 4736</option>" +
		"<option value=\"4790\">OTHER MINOR IMPROVEMENTS - 4790</option>" +
		"<option value=\"4791\">OFC BLDG (1-STY- LO-RISE- MID-RISE- HI-RISE) - 4791</option>" +
		"<option value=\"4792\">2-3 STORY RETAIL AND/OR COMMERCIAL SPACE - 4792</option>" +
		"<option value=\"4797\">SPECIAL IMPROVEMENTS - 4797</option>" +
		"<option value=\"4799\">COMMERCIAL/INDUSTRIAL-CONDOMINIUM UNITS/GARAGE - 4799</option>" +
		"<option value=\"4817\">ONE STORY RETAIL - 4817</option>" +
		"<option value=\"4823\">GASOLINE STATION WITH/WITHOUT BAY STORE - 4823</option>" +
		"<option value=\"4830\">SUPERMARKET - 4830</option>" +
		"<option value=\"4890\">OTHER MINOR IMPROVEMENTS - 4890</option>" +
		"<option value=\"4891\">OFFICE BUILDING - 4891</option>" +
		"<option value=\"4897\">FACILITIES (TENNIS - 4897</option>" +
		"<option value=\"5580\">OTHER INDUSTRIAL MINOR IMPROVEMENTS - 5580</option>" +
		"<option value=\"5581\">GARAGE USED WITH INDUSTRIAL IMPROVEMENTS - 5581</option>" +
		"<option value=\"5583\">INDUSTRIAL QUONSET HUTS AND BUTLER TYPE BLDGS - 5583</option>" +
		"<option value=\"5587\">SPECIAL INDUSTRIAL IMPROVEMENTS - 5587</option>" +
		"<option value=\"5589\">INDUSTRIAL CONDOMINIUM UNITS - 5589</option>" +
		"<option value=\"5593\">INDUSTRIAL - 5593</option>" +
		"<option value=\"5663\">INDUSTRIAL - 5663</option>" +
		"<option value=\"5670\">OTHER INDUSTRIAL MINOR IMPROVEMENTS - 5670</option>" +
		"<option value=\"5671\">GARAGE USED WITH INDUSTRIAL IMPROVEMENTS - 5671</option>" +
		"<option value=\"5673\">INDUSTRIAL QUONSET HUTS AND BUTLER TYPE BLDGS - 5673</option>" +
		"<option value=\"5677\">SPECIAL INDUSTRIAL IMPROVEMENTS - 5677</option>" +
		"<option value=\"5679\">INDUSTRIAL CONDOMINIUM UNITS - 5679</option>" +
		"<option value=\"5680\">OTHER INDUSTRIAL MINOR IMPROVEMENTS - 5680</option>" +
		"<option value=\"5681\">GARAGE USED WITH INDUSTRIAL IMPROVEMENTS - 5681</option>" +
		"<option value=\"5683\">INDUSTRIAL QUONSET HUTS AND BUTLER TYPE BLDGS - 5683</option>" +
		"<option value=\"5687\">SPECIAL INDUSTRIAL IMPROVEMENTS - 5687</option>" +
		"<option value=\"5689\">INDUSTRIAL CONDOMINIUM UNITS - 5689</option>" +
		"<option value=\"5693\">INDUSTRIAL - 5693</option>" +
		"<option value=\"5880\">OTHER INDUSTRIAL MINOR IMPROVEMENTS - 5880</option>" +
		"<option value=\"5881\">GARAGE USED WITH INDUSTRIAL IMPROVEMENTS - 5881</option>" +
		"<option value=\"5883\">INDUSTRIAL QUONSET HUTS AND BUTLER TYPE BLDGS - 5883</option>" +
		"<option value=\"5887\">SPECIAL INDUSTRIAL IMPROVEMENTS - 5887</option>" +
		"<option value=\"5889\">INDUSTRIAL CONDOMINIUM UNITS - 5889</option>" +
		"<option value=\"5893\">INDUSTRIAL - 5893</option>" +
		"<option value=\"6224\">FARM BUILDINGS - 6224</option>" +
		"<option value=\"6225\">FARM SILOS- MISC FARM IMPROVEMENTS - 6225</option>" +
		"<option value=\"6239\">FARM LAND UNDER USE-VALUE PRICING - 6239</option>" +
		"<option value=\"6240\">FARM LAND UNDER MARKET PRICING - 6240</option>" +
		"<option value=\"7100\">VACANT LAND - 7100</option>" +
		"<option value=\"7190\">OTHER MINOR IMP. WHICH DOES NOT ADD VALUE - 7190</option>" +
		"<option value=\"7200\">RESIDENTIAL LAND - 7200</option>" +
		"<option value=\"7241\">VACANT LAND COMMON OWNER WITH ADJ RESIDENCE - 7241</option>" +
		"<option value=\"7300\">LAND USED IN CONJUNCTION WITH RENTAL APTS - 7300</option>" +
		"<option value=\"7400\">NOT FOR PROFIT LAND - 7400</option>" +
		"<option value=\"7500\">COMMERCIAL LAND - 7500</option>" +
		"<option value=\"7550\">INDUSTRIAL LAND - 7550</option>" +
		"<option value=\"7650\">INDUSTRIAL LAND - 7650</option>" +
		"<option value=\"7651\">INDUSTRIAL LAND - 7651</option>" +
		"<option value=\"7700\">COMMERCIAL LAND - 7700</option>" +
		"<option value=\"7800\">COMMERCIAL LAND - 7800</option>" +
		"<option value=\"8401\">NOT FOR PROFIT GARAGE - 8401</option>" +
		"<option value=\"8415\">NOT FOR PROFIT 2-3 STY NONFIREPROOF INT ENTRANCE - 8415</option>" +
		"<option value=\"8417\">NOT FOR PROFIT 1-STORY STORE - 8417</option>" +
		"<option value=\"8418\">NOT FOR PROFIT 2-3 STORY FRAME STORE WITH APTS - 8418</option>" +
		"<option value=\"8419\">NOT FOR PROFIT 2-3 STORY OLD STYLE STORE WITH APTS - 8419</option>" +
		"<option value=\"8420\">NOT FOR PROFIT 2-3 STY MODERN INSIDE STORE W/ AP - 8420</option>" +
		"<option value=\"8421\">NOT FOR PROFIT CORNER STORE/OFC WITH APT - 8421</option>" +
		"<option value=\"8422\">NOT FOR PROFIT 1-STY NONFIREPROOF PUBLIC BLDG - 8422</option>" +
		"<option value=\"8423\">NOT FOR PROFIT GASOLINE STATION - 8423</option>" +
		"<option value=\"8426\">NOT FOR PROFIT COMMERCIAL GREENHOUSE - 8426</option>" +
		"<option value=\"8427\">NOT FOR PROFIT THEATRES - 8427</option>" +
		"<option value=\"8428\">NOT FOR PROFIT BANK BUILDINGS - 8428</option>" +
		"<option value=\"8429\">NOT FOR PROFIT MOTELS - 8429</option>" +
		"<option value=\"8430\">NOT FOR PROFIT SUPERMARKET - 8430</option>" +
		"<option value=\"8431\">NOT FOR PROFIT SHOPPING CENTER - 8431</option>" +
		"<option value=\"8432\">NOT FOR PROFIT BOWLING ALLEY - 8432</option>" +
		"<option value=\"8433\">NOT FOR PROFIT QUONSET HUTS &amp; BUTLER TYPE BLDGS - 8433</option>" +
		"<option value=\"8435\">NOT FOR PROFIT GOLF COURSE IMPROVEMENT - 8435</option>" +
		"<option value=\"8480\">NOT FOR PROFIT OTHER INDUSTRIAL MINOR IMPROVEMEN - 8480</option>" +
		"<option value=\"8481\">NOT FOR PROFIT GARAGE WITH INDUSTRIAL IMPROVEMEN - 8481</option>" +
		"<option value=\"8483\">NOT FOR PROFIT INDUST. QUONSET HUTS &amp; BUTLER TYP - 8483</option>" +
		"<option value=\"8487\">NOT FOR PROFIT SPECIAL INDUSTRIAL IMPROVEMENTS - 8487</option>" +
		"<option value=\"8489\">NOT FOR PROFIT INDUSTRIAL CONDOMINIUM UNITS - 8489</option>" +
		"<option value=\"8490\">NOT FOR PROFIT OTHER MINOR IMPROVEMENTS - 8490</option>" +
		"<option value=\"8491\">NOT FOR PROFIT IMPROVEMENT OVER THREE STORIES - 8491</option>" +
		"<option value=\"8492\">NOT FOR PROFIT 2-3 STY RETAIL AND/OR COMMERCIAL - 8492</option>" +
		"<option value=\"8493\">NOT FOR PROFIT INDUSTRIAL - 8493</option>" +
		"<option value=\"8496\">NOT FOR PROFIT RENTAL ROW HOUSES 7+ UNITS - 8496</option>" +
		"<option value=\"8497\">NOT FOR PROFIT SPECIAL IMPROVEMENT - 8497</option>" +
		"</select>";
	
	private static final String SCHOOL_SELECT = 
		"<select name=\"ddlSchool\" size=\"1\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"126            \">ALSIP-HAZLGRN-OAKLAWN</option>" +
		"<option value=\"145            \">ARBOR PARK</option>" +
		"<option value=\"25             \">ARLINGTON HEIGHTS</option>" +
		"<option value=\"125            \">ATWOOD HEIGHTS</option>" +
		"<option value=\"37             \">AVOCA</option>" +
		"<option value=\"220            \">BARRINGTON C U</option>" +
		"<option value=\"88             \">BELLWOOD</option>" +
		"<option value=\"87             \">BERKELY</option>" +
		"<option value=\"98             \">BERWYN NORTH</option>" +
		"<option value=\"100            \">BERWYN SOUTH</option>" +
		"<option value=\"130            \">BLUE ISLAND</option>" +
		"<option value=\"95             \">BROOKFIELD</option>" +
		"<option value=\"167            \">BROOKWOOD</option>" +
		"<option value=\"111            \">BURBANK</option>" +
		"<option value=\"155            \">CALUMET CITY</option>" +
		"<option value=\"132            \">CALUMET PUBLIC</option>" +
		"<option value=\"110            \">CENTRAL STICKNEY</option>" +
		"<option value=\"170            \">CHICAGO HEIGHTS</option>" +
		"<option value=\"99             \">CICERO</option>" +
		"<option value=\"168            \">COMM CONS</option>" +
		"<option value=\"59             \">COMM CONS</option>" +
		"<option value=\"160            \">COUNTRY CLUB HILLS</option>" +
		"<option value=\"62             \">DES PLAINES C C</option>" +
		"<option value=\"148            \">DOLTON</option>" +
		"<option value=\"149            \">DOLTON</option>" +
		"<option value=\"300            \">DUNDEE COMM UNIT</option>" +
		"<option value=\"169            \">EAST CHICAGO HEIGHTS</option>" +
		"<option value=\"63             \">EAST MAINE</option>" +
		"<option value=\"73             \">EAST PRAIRIE</option>" +
		"<option value=\"159            \">ELEM SCHOOL DISTRICT</option>" +
		"<option value=\"401            \">ELMWOOD PARK C U</option>" +
		"<option value=\"65             \">EVANSTON C C</option>" +
		"<option value=\"124            \">EVERGREEN PK ELEM</option>" +
		"<option value=\"161            \">FLOSSMOOR</option>" +
		"<option value=\"91             \">FOREST PARK</option>" +
		"<option value=\"142            \">FOREST RIDGE</option>" +
		"<option value=\"84             \">FRANKLIN PARK</option>" +
		"<option value=\"133            \">GEN GEO PATTON</option>" +
		"<option value=\"35             \">GLENCOE</option>" +
		"<option value=\"34             \">GLENVIEW C C</option>" +
		"<option value=\"67             \">GOLF ELEM</option>" +
		"<option value=\"152            \">HARVEY</option>" +
		"<option value=\"93             \">HILLSIDE</option>" +
		"<option value=\"181            \">HINSDALE C C</option>" +
		"<option value=\"153            \">HOMEWOOD</option>" +
		"<option value=\"157            \">HOOVER-SCHRUM MEMORIAL</option>" +
		"<option value=\"109            \">INDIAN SPRINGS</option>" +
		"<option value=\"38             \">KENILWORTH</option>" +
		"<option value=\"140            \">KIRBY</option>" +
		"<option value=\"94             \">KOMAREK</option>" +
		"<option value=\"102            \">LA GRANGE</option>" +
		"<option value=\"105            \">LAGRANGE</option>" +
		"<option value=\"106            \">LAGRANGE HIGHLANDS</option>" +
		"<option value=\"158            \">LANSING</option>" +
		"<option value=\"113            \">LEMONT-BROMBEREK</option>" +
		"<option value=\"156            \">LINCOLN ELEM</option>" +
		"<option value=\"74             \">LINCOLNWOOD</option>" +
		"<option value=\"103            \">LYONS</option>" +
		"<option value=\"83             \">MANNHEIM</option>" +
		"<option value=\"162            \">MATTESON ELEM</option>" +
		"<option value=\"89             \">MAYWOOD-MELROSE-BROADVIEW</option>" +
		"<option value=\"70             \">MORTON GROVE</option>" +
		"<option value=\"57             \">MOUNT PROSPECT</option>" +
		"<option value=\"71             \">NILES ELEM</option>" +
		"<option value=\"180            \">NO SCHOOL LISTED</option>" +
		"<option value=\"80             \">NORRIDGE</option>" +
		"<option value=\"117            \">NORTH PALOS</option>" +
		"<option value=\"28             \">NORTHBROOK</option>" +
		"<option value=\"27             \">NORTHBROOK ELEM</option>" +
		"<option value=\"30             \">NORTHBROOK/GLENVIEW</option>" +
		"<option value=\"123            \">OAK LAWN-HOMETOWN</option>" +
		"<option value=\"97             \">OAK PARK ELEM</option>" +
		"<option value=\"135            \">ORLAND</option>" +
		"<option value=\"15             \">PALATINE C C</option>" +
		"<option value=\"118            \">PALOS COMM CONS</option>" +
		"<option value=\"128            \">PALOS HEIGHTS</option>" +
		"<option value=\"163            \">PARK FOREST</option>" +
		"<option value=\"64             \">PARK RIDGE C C</option>" +
		"<option value=\"79             \">PENNOYER</option>" +
		"<option value=\"107            \">PLEASANTDALE</option>" +
		"<option value=\"143            \">POSEN-ROBBINS EL</option>" +
		"<option value=\"144            \">PRAIRIE-HILLS ELEM</option>" +
		"<option value=\"23             \">PROSPECT HEIGHTS</option>" +
		"<option value=\"122            \">RIDGELAND</option>" +
		"<option value=\"85             \">RIVER GROVE</option>" +
		"<option value=\"26             \">RIVER TRAILS</option>" +
		"<option value=\"96             \">RIVERSIDE</option>" +
		"<option value=\"78             \">ROSEMONT ELEM</option>" +
		"<option value=\"172            \">SANDRIDGE</option>" +
		"<option value=\"54             \">SCHAUMBURG C C</option>" +
		"<option value=\"81             \">SCHILLER PARK</option>" +
		"<option value=\"46             \">SCHOOL DISTRICT 46</option>" +
		"<option value=\"68             \">SKOKIE</option>" +
		"<option value=\"69             \">SKOKIE</option>" +
		"<option value=\"72             \">SKOKIE FAIRVIEW</option>" +
		"<option value=\"150            \">SOUTH HOLLAND</option>" +
		"<option value=\"151            \">SOUTH HOLLAND</option>" +
		"<option value=\"194            \">STEGER</option>" +
		"<option value=\"104            \">SUMMIT</option>" +
		"<option value=\"171            \">SUNNYBROOK</option>" +
		"<option value=\"29             \">SUNSET RIDGE</option>" +
		"<option value=\"154            \">THORNTON</option>" +
		"<option value=\"146            \">TINLEY PARK COMM</option>" +
		"<option value=\"86             \">UNION RIDGE</option>" +
		"<option value=\"147            \">W HARVEY-DIXMOOR PUB</option>" +
		"<option value=\"31             \">WEST NORTHFIELD</option>" +
		"<option value=\"92             \">WESTCHESTER</option>" +
		"<option value=\"101            \">WESTERN SPRINGS</option>" +
		"<option value=\"21             \">WHEELING C C</option>" +
		"<option value=\"108            \">WILLOW SPRINGS</option>" +
		"<option value=\"39             \">WILMETTE</option>" +
		"<option value=\"36             \">WINNETKA</option>" +
		"<option value=\"127            \">WORTH</option>" +
		"</select>";
	
	private static final String OWNER_OCCUPIED_SELECT = 
		"<select name=\"ddlOwnerOcc\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"Y\">Yes</option>" +
		"<option value=\"N\">No</option>" +
		"</select>";

	private static final String MONTH_SELECT = 
		"<select name=\"@@NAME@@\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"01\">January</option>" +
		"<option value=\"02\">February</option>" +
		"<option value=\"03\">March</option>" +
		"<option value=\"04\">April</option>" +
		"<option value=\"05\">May</option>" +
		"<option value=\"06\">June</option>" +
		"<option value=\"07\">July</option>" +
		"<option value=\"08\">August</option>" +
		"<option value=\"09\">September</option>" +
		"<option value=\"10\">October</option>" +
		"<option value=\"11\">November</option>" +
		"<option value=\"12\">December</option>" +
		"</select>";
	
	private static final String MORT_TYPE_SELECT = 
		"<select name=\"@@NAME@@\" size=\"1\" style=\"width:150px;\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"TGCO\">CONST(TGCO)</option>" +
		"<option value=\"TGCA\">CONV ADJ(TGCA)</option>" +
		"<option value=\"MTGC\">CONV(MTGC)</option>" +
		"<option value=\"TGC\">CONV(TGC)</option>" +
		"<option value=\"MTGE\">EQUITY(MTGE)</option>" +
		"<option value=\"TGE\">EQUITY(TGE)</option>" +
		"<option value=\"TGFA\">FHA ADJ(TGFA)</option>" +
		"<option value=\"MTGF\">FHA(MTGF)</option>" +
		"<option value=\"TGF\">FHA(TGF)</option>" +
		"<option value=\"TGVA\">VA ADJ(TGVA)</option>" +
		"<option value=\"MTGV\">VA(MTGV)</option>" +
		"<option value=\"TGV\">VA(TGV)</option>" +
		"</select>";
	
	private static final String TOWNSHIP_SELECT = 
		"<select name=\"ddlTownship\">" +
		"<option value=\"\"> No Selection </option>" +
		"<option value=\"10\">BARRINGTON</option>" +
		"<option value=\"11\">BERWYN</option>" +
		"<option value=\"12\">BLOOM</option>" +
		"<option value=\"13\">BREMEN</option>" +
		"<option value=\"14\">CALUMET</option>" +
		"<option value=\"15\">CICERO</option>" +
		"<option value=\"16\">ELK GROVE</option>" +
		"<option value=\"17\">EVANSTON</option>" +
		"<option value=\"18\">HANOVER</option>" +
		"<option value=\"70\">HYDE PARK</option>" +
		"<option value=\"71\">JEFFERSON</option>" +
		"<option value=\"72\">LAKE</option>" +
		"<option value=\"73\">LAKE VIEW</option>" +
		"<option value=\"19\">LEMONT</option>" +
		"<option value=\"20\">LEYDEN</option>" +
		"<option value=\"21\">LYONS</option>" +
		"<option value=\"22\">MAINE</option>" +
		"<option value=\"23\">NEW TRIER</option>" +
		"<option value=\"24\">NILES</option>" +
		"<option value=\"74\">NORTH CHICAGO</option>" +
		"<option value=\"25\">NORTHFIELD</option>" +
		"<option value=\"26\">NORWOOD PARK</option>" +
		"<option value=\"27\">OAK PARK</option>" +
		"<option value=\"28\">ORLAND</option>" +
		"<option value=\"29\">PALATINE</option>" +
		"<option value=\"30\">PALOS</option>" +
		"<option value=\"31\">PROVISO</option>" +
		"<option value=\"32\">RICH</option>" +
		"<option value=\"33\">RIVER FOREST</option>" +
		"<option value=\"34\">RIVERSIDE</option>" +
		"<option value=\"75\">ROGERS PARK</option>" +
		"<option value=\"35\">SCHAUMBURG</option>" +
		"<option value=\"76\">SOUTH CHICAGO</option>" +
		"<option value=\"36\">STICKNEY</option>" +
		"<option value=\"37\">THORNTON</option>" +
		"<option value=\"77\">WEST CHICAGO</option>" +
		"<option value=\"38\">WHEELING</option>" +
		"<option value=\"39\">WORTH</option>" +
		"</select>";

}
