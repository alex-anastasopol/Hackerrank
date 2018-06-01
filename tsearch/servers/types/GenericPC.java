package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.doctype.PacerDoctypeFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.name.NameI;

public class GenericPC extends GenericParserPC implements TSServerROLikeI {

	//update ts_sites set conn_type = 3 where site_type = 8;
	//update ts_sites set link = 'https://pacer.login.uscourts.gov/cgi-bin/login.pl?court_id=00pcl' where site_type = 8;
	
    static final long serialVersionUID = 1217874534L;    
    
    public static final int MODULE_IDX_BANKRUPTCY 	= 19;
    public static final int MODULE_IDX_CIVIL 		= 25;
    
    public GenericPC(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {        
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
    	
    	
    	List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
    	Search search = getSearch();
        int searchType = search.getSearchType();
		if(searchType == Search.AUTOMATIC_SEARCH && getDataSite().isEnabledAutomatic(search.getProductId(), search.getCommId())) {
			
			List<String> regions = getBankruptcyRegionValues();
			if(regions.isEmpty()) {
				SearchLogger.info("<br>WARNING: Automatic mode not implemented for Bankruptcy Module on this county!<br>", searchId);
			} else {
				addAutomaticModule(serverInfo, modules, MODULE_IDX_BANKRUPTCY, regions);
			}
			
			regions = getCivilRegionValues();
			if(regions.isEmpty()) {
				SearchLogger.info("<br>WARNING: Automatic mode not implemented for Civil Module on this county!<br>", searchId);
			} else {
				addAutomaticModule(serverInfo, modules, MODULE_IDX_CIVIL, regions);
			}
			
			
		}
		
        serverInfo.setModulesForAutoSearch(modules);  
    }

	public void addAutomaticModule(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules,
			int moduleIndex, List<String> regions) {
		if(regions.size() > 0) {
			ArrayList<NameI> searchedNames = null;
		    for(String key: new String[] {SearchAttributes.OWNER_OBJECT/*, SearchAttributes.BUYER_OBJECT*/}){
		        TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule( moduleIndex ));		
		        module.clearSaKeys();
		        module.setSaObjKey(key);
		        
		        
		        module.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		        module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		        module.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		        module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);

		        ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[]{"L;F;"});
		        iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
		        iterator.setInitAgain(true);
		        module.addIterator(iterator);
		        
		        if(searchedNames == null) {
		        	searchedNames = iterator.getSearchedNames();
		        } else {
		        	iterator.setSearchedNames(searchedNames);
		        }
		        
		        GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
		        nameFilter.setUseSynonymsForCandidates(true);
		        nameFilter.setIgnoreMiddleOnEmpty(true);
		        module.addFilter(nameFilter);
		        
		        if(moduleIndex == MODULE_IDX_BANKRUPTCY || moduleIndex == MODULE_IDX_CIVIL) {
		        	
		        	module.forceValue(0, regions.get(0));
		        	for (int i = 1; i < regions.size(); i++) {
						int newFctId = module.addFunction();
						TSServerInfoFunction newFct = module.getFunction(newFctId);
						newFct.setParamName(module.getFunction(0).getParamName());
						newFct.setDefaultValue(regions.get(i));
						newFct.setHiden(true);
					}
		        }
		        modules.add(module);
		    }
		}
	}
    
    //keep it at least until July 2011 as reference
    @SuppressWarnings("unused")
	private FilterResponse getDoctypeFilter() {
		if(getServerID() == 462509 || getServerID() == 460109 || getServerID() ==  459609) {	//MOJacksonPC || MOClayPC || MOCassPC
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("mowbke");
			doctypesToPass.add("mowdce");
    		doctypesToPass.add("08ca");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 440709 || getServerID() == 438809) {	//MIWaynePC || MIOaklandPC
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("miebke");
    		doctypesToPass.add("miedce");
    		doctypesToPass.add("06ca");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 402809) {	//KSJohnsonPC
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("ksbke");
    		doctypesToPass.add("ksdce");
    		doctypesToPass.add("10ca");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 559709) {	//TNShelbyPC
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("tnwbke");
    		doctypesToPass.add("tnwdce");
    		doctypesToPass.add("06ca");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 322209 || getServerID() == 325209 || getServerID() == 326109 || getServerID() == 327209 //Florida PC - Escambia, Okaloosa, Santa Rosa, Walton 
				 || getServerID() == 322409 || getServerID() == 320709 || getServerID() == 323709 || getServerID() == 320909) {	//Franklin, Alachua, Jackson, Bay
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("flnbke");
    		doctypesToPass.add("flndce");
    		doctypesToPass.add("11ca");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 321109 || getServerID() == 326309 || getServerID() == 321409 || getServerID() == 321709 //Florida PC - Brevard, Seminole,Charlotte, Collier 
				 || getServerID() == 324109 || getServerID() == 322109 || getServerID() == 325109 || getServerID() == 323209 //Lee, Duval, Nassau,Hernando
				 || getServerID() == 324009 || getServerID() == 326609 || getServerID() == 325709 || getServerID() == 325809 //Lake,Sumter,Pasco,Pinellas
				 || getServerID() == 326409 || getServerID() == 327009 || getServerID() == 325409 || getServerID() == 325509 //St Johns,Volusia,Orange,Osceola
				 || getServerID() == 325909 || getServerID() == 326209 || getServerID() == 323409) {	//Polk,Sarasota,Hillsborough
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("flmbke");
    		doctypesToPass.add("flmdce");
    		doctypesToPass.add("11ca");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 324909 || getServerID() == 325609 || getServerID() == 321209 || getServerID() == 323609 //Florida PC - MiamiDade, Palm Beach,Broward,Indian River 
				 || getServerID() == 325309 || getServerID() == 326509) {	//Okeechobee, St Lucie 
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("flsbke");
    		doctypesToPass.add("flsdce");
    		doctypesToPass.add("11ca");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 370509 || getServerID() == 371109 || getServerID() == 373409 || getServerID() == 378809 //Illinois PC - Cook, DuPage,Kane, Will 
				 || getServerID() == 373909 || getServerID() == 375209) {	//Lake,McHenry
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("ilnbke");
    		doctypesToPass.add("ilndce");
    		doctypesToPass.add("07cae");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		} else if(getServerID() == 339109) {	// ARKANSAS  PC - Pulaski
			Vector<String> doctypesToPass = new Vector<String>();
			doctypesToPass.add("arebke");
    		doctypesToPass.add("aredce");
    		doctypesToPass.add("08cae");
			return new PacerDoctypeFilterResponse(searchId, doctypesToPass);
		}    
		return new PacerDoctypeFilterResponse(searchId);
	}
    
    protected List<String> getCivilRegionValues() {
    	return getBankruptcyRegionValues();	//for now there are the same values
    }
    
    protected List<String> getBankruptcyRegionValues() {
    	List<String> regionValues = new ArrayList<String>();
    	
    	int stateId = Integer.parseInt(getSearchAttributes().getAtribute(SearchAttributes.P_STATE));
    	int countyId = Integer.parseInt(getSearchAttributes().getCountyId());
    	
    	switch (stateId) {
    	case 2:	//AK
		{
			regionValues.add("AK");
		} 
			break;
    	case 4:	//AR
		{
			switch (countyId) {
			case 5597:
				regionValues.add("are");
				//regionValues.add("08");
				break;
			default:
				//regionValues.add("AR");
				//regionValues.add("08");
				break;
			}
		} 
			break;
		case 10:	//Florida
			{
				switch (countyId) {
				case 3222:	//Escambia
				case 3252:	//Okaloosa
				case 3261:	//Santa Rosa
				case 3272: 	//Walton
				case 3224:	//Franklin
				case 3207:	//Alachua
				case 3237:	//Jackson
				case 3209:	//Bay
					//Florida North Region
					regionValues.add("fln");
					//regionValues.add("11");
					break;
				case 3211:	//Brevard
				case 3263:	//Seminole
				case 3214:	//Charlotte
				case 3217:	//Collier
				case 3241:	//Lee
				case 3221:	//Duval
				case 3251:	//Nassau
				case 3232:	//Hernando
				case 3240:	//Lake
				case 3266:	//Sumter
				case 3257:	//Pasco
				case 3258:	//Pinellas
				case 3264:	//St Johns
				case 3270:	//Volusia
				case 3254:	//Orange
				case 3255:	//Osceola
				case 3259:	//Polk
				case 3262:	//Sarasota
				case 3234:	//Hillsborough
					// Florida Middle Region
					regionValues.add("flm");
					//regionValues.add("11");
					break;
				case 3249:	//MiamiDade
				case 3256:	//Palm Beach
				case 3212:	//Broward
				case 3236:	//Indian River
				case 3253:	//Okeechobee
				case 3265:	//St Lucie 
					//Florida South Region
					regionValues.add("fls");
					//regionValues.add("11");
					break;
				default:
					break;
				}
			}
			break;
		case 14:	//IL
		{
			switch (countyId) {
			case 3705:	//Cook
			case 3711:	//DuPage
			case 3734:	//Kane
			case 3788:	//Will
			case 3739:	//Lake
			case 3752:	//McHenry:
				regionValues.add("iln");
				//regionValues.add("07");
				break;
			default:
				//regionValues.add("IL");
				//regionValues.add("07");
				break;
			}
		}
			break;
		case 17:	//KS
		{
			regionValues.add("KS");
//			switch (countyId) {
//			case 4028:	//Johnson
//				regionValues.add("ks");
//				//regionValues.add("10");
//				break;
//			default:
//				//regionValues.add("IL");
//				//regionValues.add("07");
//				break;
//			}
		}
			break;
		case 23:	//MI
		{
			switch (countyId) {
			case 4407:	//Wayne
			case 4388:	//Oakland
				regionValues.add("mie");
				//regionValues.add("06");
				break;
			default:
				//regionValues.add("MI");
				//regionValues.add("06");
				break;
			}
		}
			break;
		case 26:	//MO
		{
			switch (countyId) {
			case 4625:	//Jackson
			case 4601:	//Clay
			case 4596:	//Cass
				//Missouri Western
				regionValues.add("mow");
				//regionValues.add("08");
				break;
			default:
				//regionValues.add("MO");
				//regionValues.add("08");
				break;
			}
		}
			break;
		case 43:	//TN
		{
			switch (countyId) {
			case 5597:
				regionValues.add("tnw");
				//regionValues.add("06");
				break;
			default:
				//regionValues.add("TN");
				//regionValues.add("06");
				break;
			}
		} 
			break;
		default:
			//regionValues.add(FormatSa.getStateNameAbbrev(Integer.toString(stateId)));
			break;
		}
    	
    	
    	return regionValues;
    }

	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
	
		/*
    	ConfigurableNameIterator nameIterator = null;
   	    FilterResponse docTypeFilter = new PacerDoctypeFilterResponse(searchId);
        FilterResponse rejectAlreadyPresentFilter = new RejectAlreadyPresentFilterResponse(searchId);
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	    GenericNameFilter nameFilter=null;
	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	    	module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	    	module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			nameFilter = (GenericNameFilter) NameFilterFactory
					.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
			nameFilter.setIgnoreMiddleOnEmpty(true);
			module.addFilter(nameFilter);
			module.addFilter(docTypeFilter);
			module.addFilter(rejectAlreadyPresentFilter);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L, F;;" });
			module.addIterator(nameIterator);
			

			//modules.add(module);
		    
		     
		    if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				nameFilter=(GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);				
			    nameFilter.setIgnoreMiddleOnEmpty(true);
			    module.addFilter(nameFilter);
			    module.addFilter(docTypeFilter);
				module.addFilter(rejectAlreadyPresentFilter);
				nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L, F;;"} );
				module.addIterator(nameIterator);
				
				//modules.add(module);
		    }
	    }
  		serverInfo.setModulesForGoBackOneLevelSearch(modules);
  		*/
    	
    	
    }
    
    public static void splitResultRows(Parser p, ParsedResponse pr,
            String htmlString, int pageId, String linkStart, int action)
            throws ro.cst.tsearch.exceptions.ServerResponseException {

        Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
        
        int startIdx = 0, endIdx = 0;
        boolean isWithShowTitle = false;
        
        //exista 2 cazuri: intermediare cu show title, si intermediare cu hide title
                
        if (htmlString.indexOf("colspan = 6") != -1)
            isWithShowTitle = true;
        
                
        
        htmlString = htmlString.replaceAll("TR", "tr");
        String vsRowSeparator = "<tr", rowEndSeparator = "</tr>";
        
        startIdx = htmlString.indexOf(vsRowSeparator, 0);
        if (startIdx != -1) {
            
            pr.setHeader(htmlString.substring(0, startIdx));

            endIdx = htmlString.indexOf(vsRowSeparator, startIdx + vsRowSeparator.length());
            String row;
            Pattern patt = Pattern.compile("(?s)<tr><TD>\\s*\\d*.*?</tr>\\s*?<tr><TD colspan.*?</tr>");
            Matcher m = patt.matcher(htmlString);
            while (endIdx != -1) {
                
                
                if (isWithShowTitle) {
                    
                    if (m.find()) {
                        row = m.group(0);
                        row = row.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\n\r", " ");
                        
                        ParsedResponse pResponse = new ParsedResponse();
                        pResponse.setParentSite(pr.isParentSite());
                        p.Parse(pResponse, row, pageId, linkStart, action);                
                        parsedRows.add(pResponse);
                    } else {
                        break;
                    }
                    
                }else {
                    
                    row = htmlString.substring(startIdx, endIdx);
                    row = row.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\n\r", " ");
                    if (row.indexOf("BGCOLOR=#DDDDDD") == -1 
                            && row.indexOf("BGCOLOR=BLACK") == -1) {
                        
                        ParsedResponse pResponse = new ParsedResponse();
                        pResponse.setParentSite(pr.isParentSite());
                        p.Parse(pResponse, row, pageId, linkStart, action);                
                        parsedRows.add(pResponse);
                        
                    }
                }
                 
                startIdx = endIdx;
                endIdx = htmlString.indexOf(vsRowSeparator, startIdx + vsRowSeparator.length());
            }

            endIdx = htmlString.indexOf(rowEndSeparator, startIdx);
            if (endIdx == -1)
                endIdx = htmlString.length();
            endIdx = htmlString.indexOf(rowEndSeparator, endIdx + vsRowSeparator.length());
            if (endIdx == -1)
                endIdx = htmlString.length();

            if (!isWithShowTitle) {
                row = htmlString.substring(startIdx, endIdx).replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\n\r", " ");
                
                ParsedResponse pResponse = new ParsedResponse();
                pResponse.setParentSite(pr.isParentSite());
                p.Parse(pResponse, row, pageId, linkStart, action);                
                parsedRows.add(pResponse);
            }

            pr.setFooter(htmlString.substring(endIdx, htmlString.length()));
            
        } else {
            endIdx = htmlString.indexOf(rowEndSeparator, 0);
            if (endIdx == -1)
                endIdx = htmlString.length();

            pr.setHeader(htmlString.substring(0, endIdx));
            pr.setFooter(htmlString.substring(endIdx, htmlString.length())); 
        }
        pr.setResultRows(parsedRows);
    }

	protected String getFileNameFromLink(String link){
        String instNo = "";
        
        try{
        	int start = link.indexOf( "puid=" );
        	if( start < 0 ){
        		start = link.lastIndexOf("&");
        		start += 1;
        	}
        	else{
        		start += 5;
        	}
        	
        	instNo = link.substring( start );
        }
        catch( Exception e ){
        	e.printStackTrace();
        }
        
		return instNo + ".html";
	}

	/*
    private String getDocketReportTable(String instrNum, String host, String sAction) {
    	String httpsHost = "https://"+ host;
		String link = httpsHost + "/cgi-bin/DktRpt.pl?" + instrNum;
    	HTTPRequest req = new HTTPRequest( link ); 
        req.setHeader("Host", host);
        req.setHeader("Referer", sAction);
        
        String siteResponse = performRequest(req);
        String action = RegExUtils.getFirstMatch("action=\"(.*)\"", siteResponse, 1);
        
        String caseNumber = RegExUtils.getFirstMatch("AddCaseNumberLine\\('(.*)'\\)", siteResponse, 1);
        caseNumber = caseNumber.replaceFirst("-", ":");
        
        req = new HTTPRequest(httpsHost + action);
        req.setMethod(HTTPRequest.POST);
        
        req.setPostParameter("all_case_ids", instrNum);
        req.setPostParameter("case_num", caseNumber);
        req.setPostParameter("date_type", "filed");
        req.setPostParameter("date_from", "");
        req.setPostParameter("date_to", "");
        req.setPostParameter("documents_numbered_from_", "");
        req.setPostParameter("documents_numbered_to_", "");
        req.setPostParameter("terminated_parties", "on");
        req.setPostParameter("output_format","html" );
        req.setPostParameter("sort1", "most recent date first");
        
        req.setHeader("Host", host);
        req.setHeader("Referer", link);
        
        siteResponse = performRequest(req);
        siteResponse = RegExUtils.getFirstMatch("(?is)<TABLE BORDER=1 CELLPADDING=10 CELLSPACING=0>.*?</TABLE>", siteResponse, 0);
        
        //replace the links
        String startLink = CreatePartialLink(TSConnectionURL.idGET);        
        siteResponse = siteResponse.replaceAll("<a href=\\'(.*?')" , "<a href='"+ startLink + "$1" + " target='_blank' "); 
        siteResponse = siteResponse.replaceAll("(?is)<img.*?>", "");
		return siteResponse;
	}
    */

	public static String getInstrumentNumberFromResponse( String httpResponse )
    {
        String court = httpResponse.replaceAll( "\n", " " );

        if (court.indexOf("<td") != -1) {
            court = court.substring(court.indexOf("<td"));
        }
        
        if (court.indexOf("<TABLE") != -1) {
            court = court.substring(0, court.indexOf("<TABLE"));
        }
        
        court = court.replaceAll( "<[^>]*>", "" );
        court = court.replaceAll("\\n|\\r","");
        court = court.replaceAll("\\s{2,}"," ");
        court = court.replaceAll("\\*","");
        
        String instr="";
        int docket=0;
        
        int index;
        if ((index = court.indexOf("Court of Appeals Docket #:")) != -1 ) {
            docket = index + "Court of Appeals Docket #:".length(); 
            court = court.replaceAll(court.substring(0,docket),"");
            court=court.trim();
        }
        
        Pattern p0 = Pattern.compile("(?i)(?s)(\\d{2}-\\d{5})(.*)(Case\\stype:)(.*)(Date[\\s]filed:.)(\\d{1,2}/\\d{1,2}/\\d{4}).*");
        Matcher m0 = p0.matcher(court);
        if (m0.find()) {
            instr=m0.group(1);
        }
        
        Pattern p1 = Pattern.compile("(?i)(?s)(\\d{2}-\\d{5}-[a-z]{3}\\d{0,2})(.*)(Case\\stype:)(.*)(Date[\\s]filed:.)(\\d{1,2}/\\d{1,2}/\\d{4}).*");
        Matcher mm = p1.matcher(court);
        if (mm.find()) {
            instr=mm.group(1);
        }
                
        Pattern p02 = Pattern.compile("(?i)(?s)(\\d{1}:\\d{2}-[a-z]{2}-\\d{5}-[A-Z]{3}-[A-Z]{3})(.*)(,?).*(Date[\\s]filed:)\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*");
        Matcher mmm02 = p02.matcher(court);
        if (mmm02.find()) {
            instr=mmm02.group(1);
        }else {
            Pattern p2 = Pattern.compile("(?i)(?s)(\\d{1}:\\d{2}-[a-z]{2}-\\d{5}-[A-Z]{3})(.*)(,?).*(Date[\\s]filed:)\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*");
            Matcher mmm = p2.matcher(court);
            if (mmm.find()) {
                instr=mmm.group(1);
            }else {
                Pattern p3 = Pattern.compile("(?i)(?s)(\\d{1}:\\d{2}-[a-z]{2}-\\d{5})(.*)(Date[\\s]filed:)\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*");
                Matcher mmmm = p3.matcher(court);
                if (mmmm.find()) {
                    instr=mmmm.group(1);
                }
            }
        }

        Pattern p4 = Pattern.compile("(?i)(?s)(\\d{2}-\\d{4})\\s(Filed:)\\s(\\d{1,2}/\\d{1,2}/\\d{2})(.*)(v.?).*");
        Matcher n = p4.matcher(court);
        if (n.find()) {
            instr=n.group(1);
        }
        
        instr=instr.trim();
        
        return instr;
    }
    
    static final Map<String,String> stateOptions = new HashMap<String,String>();
    static{
    	stateOptions.put("AK","ak,ALASKA,09");
    	stateOptions.put("AL","al,ALABAMA,11");
    	stateOptions.put("AR","ar,ARKANSAS,08");
    	stateOptions.put("AZ","az,ARIZONA,09");
    	stateOptions.put("CA","ca,CALIFORNIA,09");
    	stateOptions.put("CO","co,COLORADO,10");
    	stateOptions.put("CT","ct,CONNECTICUT,02");
    	stateOptions.put("DC","dc,WASHINGTON D.C.,DC");
    	stateOptions.put("DE","de,DELAWARE,03");
    	stateOptions.put("FL","fl,FLORIDA,11");
    	stateOptions.put("GA","ga,GEORGIA,11");
    	stateOptions.put("GU","gu,GUAM,09");
    	stateOptions.put("HI","hi,HAWAII,09");
    	stateOptions.put("IA","ia,IOWA,08");
    	stateOptions.put("ID","id,IDAHO,09");
    	stateOptions.put("IL","il,ILLINOIS,07");
    	stateOptions.put("IN","in,INDIANA,07");
    	stateOptions.put("KS","ks,KANSAS,10");
    	stateOptions.put("KY","ky,KENTUCKY,06");
    	stateOptions.put("LA","la,LOUISIANA,05");
    	stateOptions.put("MA","ma,MASSACHUSETTS,01");
    	stateOptions.put("MD","md,MARYLAND,04");
    	stateOptions.put("ME","me,MAINE,01");
    	stateOptions.put("MI","mi,MICHIGAN,06");
    	stateOptions.put("MN","mn,MINNESOTA,08");
    	stateOptions.put("MO","mo,MISSOURI,08");
    	stateOptions.put("MS","ms,MISSISSIPPI,05");
    	stateOptions.put("MT","mt,MONTANA,09");
    	stateOptions.put("NC","nc,NORTH CAROLINA,04");
    	stateOptions.put("ND","nd,NORTH DAKOTA,08");
    	stateOptions.put("NE","ne,NEBRASKA,08");
    	stateOptions.put("NH","nh,NEW HAMPSHIRE,01");
    	stateOptions.put("NJ","nj,NEW JERSEY,03");
    	stateOptions.put("NM","nm,NEW MEXICO,10");
    	stateOptions.put("NV","nv,NEVADA,09");
    	stateOptions.put("NY","ny,NEW YORK,02");
    	stateOptions.put("OH","oh,OHIO,06");
    	stateOptions.put("OK","ok,OKLAHOMA,10");
    	stateOptions.put("OR","or,OREGON,09");
    	stateOptions.put("PA","pa,PENNSYLVANIA,03");
    	stateOptions.put("PR","pr,PUERTO RICO,01");
    	stateOptions.put("RI","ri,RHODE ISLAND,01");
    	stateOptions.put("SC","sc,SOUTH CAROLINA,04");
    	stateOptions.put("SD","sd,SOUTH DAKOTA,08");
    	stateOptions.put("TN","tn,TENNESSEE,06");
    	stateOptions.put("TX","tx,TEXAS,05");
    	stateOptions.put("UT","ut,UTAH,10");
    	stateOptions.put("VA","va,VIRGINIA,04");
    	stateOptions.put("VI","vi,VIRGIN ISLANDS,03");
    	stateOptions.put("VT","vt,VERMONT,02");
    	stateOptions.put("WA","wa,WASHINGTON,09");
    	stateOptions.put("WI","wi,WISCONSIN,07");
    	stateOptions.put("WV","wv,WEST VIRGINIA,04");
    	stateOptions.put("WY","wy,WYOMING,10");
    }
    
    /**
     * Get current state option
     * @return
     */
    protected String getOptionValue(){
    	String stateAbbrev = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName().substring(0, 2);
    	return stateOptions.get(stateAbbrev);
    }
    
    
}
