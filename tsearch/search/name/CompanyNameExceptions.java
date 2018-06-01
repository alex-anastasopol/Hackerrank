package ro.cst.tsearch.search.name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.search.module.CompanyNameDerrivator;
import ro.cst.tsearch.search.name.companyex.CompanyNameMarshal;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

public class CompanyNameExceptions {
	
	public static HashMap<String, String> companies;
	private final static ArrayList<String[]> tokensCCN;
	private final static ArrayList<String[]> tokensTBD;
	
	public static final String[] CCN_MESSAGE = {
		"Will not search with ",
		" because it is a Common Company Name"
	};
	
	static {
		companies = new HashMap<String, String>();
		loadCompany();
		
		tokensCCN = new ArrayList<String[]>();
		tokensCCN.add(new String[] {"BANK"});
		tokensCCN.add(new String[] {"BANC"});
		tokensCCN.add(new String[] {"CITY"});
		tokensCCN.add(new String[] {"COUNTY"});
		tokensCCN.add(new String[] {"FIRST"});
		tokensCCN.add(new String[] {"STATE"});
		tokensCCN.add(new String[] {"CREDIT"});
		tokensCCN.add(new String[] {"FINANCE"});
		tokensCCN.add(new String[] {"FINANCIAL"});
		tokensCCN.add(new String[] {"FINANCING"});
		tokensCCN.add(new String[] {"HOUSING"});
		tokensCCN.add(new String[] {"URBAN"});
		tokensCCN.add(new String[] {"HUD"});
		tokensCCN.add(new String[] {"LOAN"});
		tokensCCN.add(new String[] {"MORTGAGE"});
		tokensCCN.add(new String[] {"MORTGAGEE"});
		tokensCCN.add(new String[] {"CITIMORTGAGE"});
		tokensCCN.add(new String[] {"MTG"});
		tokensCCN.add(new String[] {"NATL"});
		tokensCCN.add(new String[] {"NATIONAL"});
		tokensCCN.add(new String[] {"NA"});
		
		tokensCCN.add(new String[] {"SAVINGS"});
		tokensCCN.add(new String[] {"TRUST"});
		tokensCCN.add(new String[] {"CONSTRUCTION"});
		tokensCCN.add(new String[] {"SECRETARY"});
		tokensCCN.add(new String[] {"FNMA"});
		tokensCCN.add(new String[] {"FEDERAL"});
		tokensCCN.add(new String[] {"FREDDIE"});
		tokensCCN.add(new String[] {"FANNIE"});
		tokensCCN.add(new String[] {"FREDIE"});
		tokensCCN.add(new String[] {"CAPITAL"});
		tokensCCN.add(new String[] {"BK"});
		tokensCCN.add(new String[] {"UNLIMITED"});
		tokensCCN.add(new String[] {"GROUP"});
		
		tokensTBD = new ArrayList<String[]>();
		tokensTBD.add(new String[] {"TBD"});
		tokensTBD.add(new String[] {"TO BE "});
		tokensTBD.add(new String[] {"TO BE DETERMINED"});
  	}
		
	public static void loadCompany(){
		CompanyNameMarshal comp = new CompanyMarshal().unmarshal();
		companies=new HashMap<String, String>();
	        for(int i=0;i<comp.getCompanyObject().size();i++){
	        	String c = comp.getCompanyObject().get(i).getCompanyName().replaceAll("'", "").toUpperCase();
	        	String v = comp.getCompanyObject().get(i).getCompanyValue();
	        	List<String> dc = CompanyNameDerrivator.buldCompanyNameList(c, Search.SEARCH_NONE);
	        	dc.add(c);
	        	Iterator<String> j = dc.iterator();
	        	while (j.hasNext()){
	        		companies.put( j.next() , v );
	        	}
	        }
	
	}
	public static boolean allowed(String name,long searchId) {
		DataSite server = null;
		String serverType = "";
		String serverName = "";

		
		Search global = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext();
		
		int searchType = global.getSearchType();
		boolean printMessage = true;
		if(searchType == Search.PARENT_SITE_SEARCH) {
			printMessage = false;
		}
		
		if(printMessage) {
		
			try{
				server = HashCountyToIndex.getCrtServer(searchId, false);
				if (server != null){
					serverName = server.getName();
					if (serverName.length() > 2){
						serverType = server.getSiteTypeAbrev();
					}
				} else {
					Search globalInner = SearchManager.getSearch(searchId);
					if (globalInner != null){
						serverType = globalInner.getCrtServerType(false);
						serverName = globalInner.getCrtServerName(false);
					}
				}
			} catch (BaseException e){
				e.printStackTrace();
			}	
		
		}
		
		List<String> companyNameList = CompanyNameDerrivator.buldCompanyNameList( name.toUpperCase() ,searchId);
		companyNameList.add(name.toUpperCase());
		for (int i = 0; i < companyNameList.size(); i++) {
			
			String companyNameItem = companyNameList.get(i).toString();
			companyNameItem = companyNameItem.replaceAll("\\p{Punct}", "");
			companyNameItem = companyNameItem.replaceAll("\\d+", "");
			companyNameItem = companyNameItem.trim();
			
			if (isTBD(companyNameItem)){
				return false;
			}
			
			for (int j = 0; j < tokensCCN.size(); j++){
				String token = tokensCCN.get(j)[0];
				if (Arrays.asList(companyNameItem.split("\\s+")).contains(token)){
					
					if(printMessage) {
					
					if (!serverType.equals("")){
						global.getSa().setSiteNameSearchSkipped(serverType);
					}
					String warn1 = serverName + " - " + CCN_MESSAGE[0] + name.toUpperCase() + CCN_MESSAGE[1];
					String warn2 = "</div><span class=\"serverName\">" + serverName + "</span> - " + "<font color=\"red\">" + 
						CCN_MESSAGE[0] + "<B>"+ name.toUpperCase() +"</b> because contains <b>" + token + "</b> and may be a Common Company Name.</font><BR>";
					if (!global.warningDisplayed(warn1)){
			     		IndividualLogger.info( warn1 ,searchId);
			     		global.addCompanyNameWarning(warn1);
					}
					if (!global.warningDisplayed(warn2)){
				        SearchLogger.info(/*StringUtils.createCollapsibleHeader() +*/ warn2, searchId);
				        global.addCompanyNameWarning(warn2);
					}
					
					}
					return false;
				}
			}
			if ( companies.get(companyNameList.get(i)) != null || companies.get(companyNameItem) != null ){
				
				if(printMessage) {
				
				if (!serverType.equals("")){
					global.getSa().setSiteNameSearchSkipped(serverType);
				}
				String warn1 = serverName + " - " + CCN_MESSAGE[0] + name.toUpperCase() + CCN_MESSAGE[1];
				String warn2 = "</div><span class=\"serverName\">" + serverName + "</span> - " + "<font color=\"red\">" + 
					CCN_MESSAGE[0] + "<B>"+ name.toUpperCase() +"</b>" + CCN_MESSAGE[1] + ".</font><BR>";
				if (!global.warningDisplayed(warn1)){
		     		IndividualLogger.info( warn1 ,searchId);
		     		global.addCompanyNameWarning(warn1);
				}
				if (!global.warningDisplayed(warn2)){
			        SearchLogger.info(/*StringUtils.createCollapsibleHeader() +*/ warn2, searchId);
			        global.addCompanyNameWarning(warn2);
				}

				}
				
				return false;
			}
		}
		return true;
	}

	public static boolean isTBD(String companyName){
		for (int j = 0; j < tokensTBD.size(); j++){
			String token = tokensTBD.get(j)[0];
			if (companyName.equals(token.trim()) || companyName.startsWith(token)){
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean allowed(String name) {
		List<String> companyNameList = CompanyNameDerrivator.buldCompanyNameList( name.toUpperCase() ,Search.SEARCH_NONE);
		companyNameList.add(name.toUpperCase());

		for (int i = 0; i < companyNameList.size(); i++) {
			
			String companyNameItem = companyNameList.get(i).toString();
			companyNameItem = companyNameItem.replaceAll("\\p{Punct}", "");
			companyNameItem = companyNameItem.replaceAll("\\d+", "");
			companyNameItem = companyNameItem.trim();
			
			for (int j = 0; j < tokensCCN.size(); j++){
				String token = tokensCCN.get(j)[0];
				if (Arrays.asList(companyNameItem.split("\\s+")).contains(token)){
					return false;
				}
			}
			if ( companies.get(companyNameList.get(i)) != null || companies.get(companyNameItem) != null){
				return false;
			}
		}
		return true;
	}

	public static boolean allowed(String name,long searchId, boolean printLog) {
		
		List<String> companyNameList = CompanyNameDerrivator.buldCompanyNameList( name.toUpperCase() ,searchId);
		companyNameList.add(name.toUpperCase());

		for (int i = 0; i < companyNameList.size(); i++) {
			
			String companyNameItem = companyNameList.get(i).toString();
			companyNameItem = companyNameItem.replaceAll("\\p{Punct}", "");
			companyNameItem = companyNameItem.replaceAll("\\d+", "");
			companyNameItem = companyNameItem.trim();
			
			for (int j = 0; j < tokensCCN.size(); j++){
				String token = tokensCCN.get(j)[0];
				if (Arrays.asList(companyNameItem.split("\\s+")).contains(token)){
					if(printLog) {
			     		IndividualLogger.info( "Will not search with "+ name+" because contains <b>"
			     							+ token + "</b> and may be a Common Company Name", searchId);
				        SearchLogger.info("<font color=\"red\"><BR>Will not search with <B>"+ name+"</b> because contains <b>"
				        					+ token + "</b> and may be a Common Company Name.</font><BR>", searchId);
					}
					return false;
				}
			}
			
			if ( companies.get(companyNameList.get(i)) != null || companies.get(companyNameItem) != null ){
				if(printLog) {
		     		IndividualLogger.info( "Will not search with "+ name+" because it is a Common Company Name",searchId);
			        SearchLogger.info("<font color=\"red\"><BR>Will not search with <B>"+ name+"</b> because it is a Common Company Name.</font><BR>", searchId);
				}
			    return false;
			}
		}
		return true;
	}

	public static boolean allowed(String name, long searchId, TSServer site){
		if (allowed(name, searchId))
			return true;
		return false;
	}
	
	public static void main(String[] args) {
		
		/*CompanyNameDerrivator.loadAbbv();
		
		if (allowed("FEDERAL NATIONAL MORTGAGE ASSOC"))
			System.err.println("merge");
		else
			System.err.println("nu merge");
		*/
	}
    
}
