package ro.cst.tsearch.utils.helpers;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.Util;

public class SiteActivator {
	
	
	private List<Integer> commIds = new ArrayList<Integer>();
	private List<Integer> countyIds = new ArrayList<Integer>();
	private int siteType;
	private int enableFlag = 0;
	
	public SiteActivator(int siteType) {
		this.siteType = siteType;
	}
	
	public void enableFullAutomatic() {
		enable(Util.SITE_ENABLED_MASK);
		
		enable(Util.SITE_ENABLED_AUTOMATIC_FULL_SEARCH_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_CRT_OWNER_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_CONSTRUCTION_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_REFINANCE_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_OE_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_LIENS_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_ACREAGE_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_SUBLOT_MASK);
		enable(Util.SITE_ENABLED_AUTOMATIC_UPDATE_MASK);
		
		enable(Util.SITE_ENABLED_NAME_BOOT_STRAP_MASK);
		enable(Util.SITE_ENABLED_ADDRESS_BOOT_STRAP_MASK);
		enable(Util.SITE_ENABLED_LEGAL_BOOT_STRAP_MASK);
		enable(Util.SITE_ENABLED_NAME_DERIVATION_MASK);
		enable(Util.SITE_ENABLED_OCR_MASK);
		enable(Util.SITE_ENABLED_INCLUDE_IN_TSR);
		enable(Util.SITE_ENABLED_INCLUDE_IMAGE_IN_TSR);
	}
	
	public void enableFullParentSite(){
		enableFlag = 0;
		enable(Util.SITE_ENABLED_MASK);
		
		enable(Util.SITE_ENABLED_NAME_BOOT_STRAP_MASK);
		enable(Util.SITE_ENABLED_ADDRESS_BOOT_STRAP_MASK);
		enable(Util.SITE_ENABLED_LEGAL_BOOT_STRAP_MASK);
		enable(Util.SITE_ENABLED_NAME_DERIVATION_MASK);
		enable(Util.SITE_ENABLED_OCR_MASK);
		enable(Util.SITE_ENABLED_INCLUDE_IN_TSR);
		enable(Util.SITE_ENABLED_INCLUDE_IMAGE_IN_TSR);
	}
	
	public void addCommunitiesFL(int ... extraCommIds) {
		clearCommunities();
		addCommunity(3);	//Default
		addCommunity(4);	//CST-TA
		addCommunity(71);	//FTS
		addCommunity(73);	//FTS Training Account
		
		if(extraCommIds != null) {
			for (int i : extraCommIds) {
				if(!hasCommunity(i)) {
					addCommunity(i);
				}
			}
		}
	}
	
	public void addCounty(int countyId) {
		countyIds.add(countyId);
	}
	
	public void addCounties(List<Integer> countyIds) {
		if(countyIds != null) {
			for (int countyId : countyIds) {
				if(!this.countyIds.contains(countyId)) {
					this.countyIds.add(countyId);
				}
			}
			
		}
	}
	
	public int enable(int flag) {
		return enableFlag = flag | enableFlag;
	}
	public int disable(int flag) {
		return enableFlag = (~flag) & enableFlag;
	}
	
	public void clearCommunities() {
		commIds.clear();
	}
	
	public void addCommunity(int commId) {
		commIds.add(commId);
	}
	
	public boolean hasCommunity(int commId) {
		return commIds.contains(commId);
	}
	
	public static void main(String[] args) {
		/*
		List<String> sqls = new ArrayList<String>();
		gammaRestOfATI(sqls);
		
			
		SiteActivator activator = new SiteActivator(GWTDataSite.DG_TYPE);
		activator.enableFullAutomatic();
		activator.disable(Util.SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK);
		activator.addCommunity(4);	//CST-TA
		
		
		for (String sql : sqls) {
			System.out.println(sql);
		}
		*/
		//betaStuff();
		//gammStuff();
//		for (Integer sql : getAtiNotWorkingSites()) {
//			System.out.print(sql + ", ");
//		}
		
		for (Integer sql : getDTGList(true)) {
			System.out.print(sql + ", ");
		}
	}
	
	protected static List<Integer> getDTGList(boolean onlyWithAccess) {
		
		List<Integer> countiesDGCstta = new ArrayList<Integer>();
		
		countiesDGCstta.add(CountyConstants.FL_Alachua);
		
		countiesDGCstta.add(CountyConstants.FL_Baker);
		countiesDGCstta.add(CountyConstants.FL_Bay);
		countiesDGCstta.add(CountyConstants.FL_Bradford);
		countiesDGCstta.add(CountyConstants.FL_Brevard);
		countiesDGCstta.add(CountyConstants.FL_Broward);
		
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Calhoun);
			countiesDGCstta.add(CountyConstants.FL_Charlotte);
		}
		countiesDGCstta.add(CountyConstants.FL_Citrus);
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Clay);
		}
		countiesDGCstta.add(CountyConstants.FL_Collier);
		countiesDGCstta.add(CountyConstants.FL_Columbia);
		
		countiesDGCstta.add(CountyConstants.FL_DeSoto);
		countiesDGCstta.add(CountyConstants.FL_Dixie);
		countiesDGCstta.add(CountyConstants.FL_Duval);
		
		countiesDGCstta.add(CountyConstants.FL_Escambia);
		
		countiesDGCstta.add(CountyConstants.FL_Flagler);
		countiesDGCstta.add(CountyConstants.FL_Franklin);
		
		countiesDGCstta.add(CountyConstants.FL_Gadsden);
		countiesDGCstta.add(CountyConstants.FL_Gilchrist);
		countiesDGCstta.add(CountyConstants.FL_Glades);
		countiesDGCstta.add(CountyConstants.FL_Gulf);
		
		countiesDGCstta.add(CountyConstants.FL_Hamilton);
		countiesDGCstta.add(CountyConstants.FL_Hardee);
		countiesDGCstta.add(CountyConstants.FL_Hendry);
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Hernando);
			countiesDGCstta.add(CountyConstants.FL_Highlands);
		}
		countiesDGCstta.add(CountyConstants.FL_Hillsborough);
		countiesDGCstta.add(CountyConstants.FL_Holmes);
		
		countiesDGCstta.add(CountyConstants.FL_Indian_River);
		
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Jackson);
		}
		countiesDGCstta.add(CountyConstants.FL_Jefferson);
		
		countiesDGCstta.add(CountyConstants.FL_Lafayette);
		countiesDGCstta.add(CountyConstants.FL_Lake);
		countiesDGCstta.add(CountyConstants.FL_Lee);
		countiesDGCstta.add(CountyConstants.FL_Leon);
		countiesDGCstta.add(CountyConstants.FL_Levy);
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Liberty);
		}
		
		countiesDGCstta.add(CountyConstants.FL_Madison);
		countiesDGCstta.add(CountyConstants.FL_Manatee);
		countiesDGCstta.add(CountyConstants.FL_Marion);
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Martin);
		}
		countiesDGCstta.add(CountyConstants.FL_Miami_Dade);
		countiesDGCstta.add(CountyConstants.FL_Monroe);
		
		countiesDGCstta.add(CountyConstants.FL_Nassau);
		
		countiesDGCstta.add(CountyConstants.FL_Okaloosa);
		countiesDGCstta.add(CountyConstants.FL_Okeechobee);
		countiesDGCstta.add(CountyConstants.FL_Orange);
		countiesDGCstta.add(CountyConstants.FL_Osceola);
		
		countiesDGCstta.add(CountyConstants.FL_Palm_Beach);
		countiesDGCstta.add(CountyConstants.FL_Pasco);
		countiesDGCstta.add(CountyConstants.FL_Pinellas);
		countiesDGCstta.add(CountyConstants.FL_Polk);
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Putnam);
		}
		
		countiesDGCstta.add(CountyConstants.FL_Santa_Rosa);
		countiesDGCstta.add(CountyConstants.FL_Sarasota);
		countiesDGCstta.add(CountyConstants.FL_Seminole);
		countiesDGCstta.add(CountyConstants.FL_St_Johns);
		countiesDGCstta.add(CountyConstants.FL_St_Lucie);
		countiesDGCstta.add(CountyConstants.FL_Sumter);
		countiesDGCstta.add(CountyConstants.FL_Suwannee);
		
		countiesDGCstta.add(CountyConstants.FL_Taylor);
		
		countiesDGCstta.add(CountyConstants.FL_Union);
		
		if(!onlyWithAccess) {
			countiesDGCstta.add(CountyConstants.FL_Volusia);
		}
		
		countiesDGCstta.add(CountyConstants.FL_Wakulla);
		countiesDGCstta.add(CountyConstants.FL_Walton);
		countiesDGCstta.add(CountyConstants.FL_Washington);
		
		return countiesDGCstta;
	}
	
	
	protected static void betaStuff() {
		List<Integer> countiesReadyForAtiOnGamma = new ArrayList<Integer>();
		
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Alachua);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_DeSoto);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Duval);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Flagler);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Hernando);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Lee);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Monroe);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Okeechobee);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Orange);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Polk);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_St_Lucie);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Santa_Rosa);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Seminole);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Sumter);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Bay);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Brevard);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Broward);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Charlotte);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Collier);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Escambia);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Hendry);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Highlands);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Hillsborough);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Indian_River);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Lake);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Leon);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Manatee);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Marion);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Martin);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Miami_Dade);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Osceola);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Okaloosa);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Palm_Beach);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Pasco);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Pinellas);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Sarasota);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Volusia);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Walton);
		
		SiteActivator activatorAtiDefault = preloadAtiActivator();
		activatorAtiDefault.clearCommunities();
		activatorAtiDefault.addCommunity(3);
		activatorAtiDefault.addCounties(countiesReadyForAtiOnGamma);
		
		SiteActivator deactivatorDtDefault = preloadDTDeactivator();
		deactivatorDtDefault.clearCommunities();
		deactivatorDtDefault.addCommunity(3);
		deactivatorDtDefault.addCounties(countiesReadyForAtiOnGamma);
		
		List<String> sqls = new ArrayList<String>();
//		sqls.addAll(activatorAtiDefault.generateSqls());
//		sqls.addAll(deactivatorDtDefault.generateSqls());
		
		
		
		
		List<Integer> countiesDGCstta = new ArrayList<Integer>();
		
		countiesDGCstta.add(CountyConstants.FL_Alachua);
		
		countiesDGCstta.add(CountyConstants.FL_Baker);
		countiesDGCstta.add(CountyConstants.FL_Bay);
		countiesDGCstta.add(CountyConstants.FL_Bradford);
		countiesDGCstta.add(CountyConstants.FL_Brevard);
		countiesDGCstta.add(CountyConstants.FL_Broward);
		
		countiesDGCstta.add(CountyConstants.FL_Calhoun);
		countiesDGCstta.add(CountyConstants.FL_Charlotte);
		countiesDGCstta.add(CountyConstants.FL_Citrus);
		countiesDGCstta.add(CountyConstants.FL_Clay);
		countiesDGCstta.add(CountyConstants.FL_Collier);
		countiesDGCstta.add(CountyConstants.FL_Columbia);
		
		countiesDGCstta.add(CountyConstants.FL_DeSoto);
		countiesDGCstta.add(CountyConstants.FL_Dixie);
		countiesDGCstta.add(CountyConstants.FL_Duval);
		
		countiesDGCstta.add(CountyConstants.FL_Escambia);
		
		countiesDGCstta.add(CountyConstants.FL_Flagler);
		countiesDGCstta.add(CountyConstants.FL_Franklin);
		
		countiesDGCstta.add(CountyConstants.FL_Gadsden);
		countiesDGCstta.add(CountyConstants.FL_Gilchrist);
		countiesDGCstta.add(CountyConstants.FL_Glades);
		countiesDGCstta.add(CountyConstants.FL_Gulf);
		
		countiesDGCstta.add(CountyConstants.FL_Hamilton);
		countiesDGCstta.add(CountyConstants.FL_Hardee);
		countiesDGCstta.add(CountyConstants.FL_Hendry);
		countiesDGCstta.add(CountyConstants.FL_Hernando);
		countiesDGCstta.add(CountyConstants.FL_Highlands);
		countiesDGCstta.add(CountyConstants.FL_Hillsborough);
		countiesDGCstta.add(CountyConstants.FL_Holmes);
		
		countiesDGCstta.add(CountyConstants.FL_Indian_River);
		
		countiesDGCstta.add(CountyConstants.FL_Jackson);
		countiesDGCstta.add(CountyConstants.FL_Jefferson);
		
		countiesDGCstta.add(CountyConstants.FL_Lafayette);
		countiesDGCstta.add(CountyConstants.FL_Lake);
		countiesDGCstta.add(CountyConstants.FL_Lee);
		countiesDGCstta.add(CountyConstants.FL_Leon);
		countiesDGCstta.add(CountyConstants.FL_Levy);
		countiesDGCstta.add(CountyConstants.FL_Liberty);
		
		countiesDGCstta.add(CountyConstants.FL_Madison);
		countiesDGCstta.add(CountyConstants.FL_Manatee);
		countiesDGCstta.add(CountyConstants.FL_Marion);
		countiesDGCstta.add(CountyConstants.FL_Martin);
		countiesDGCstta.add(CountyConstants.FL_Miami_Dade);
		countiesDGCstta.add(CountyConstants.FL_Monroe);
		
		countiesDGCstta.add(CountyConstants.FL_Nassau);
		
		countiesDGCstta.add(CountyConstants.FL_Okaloosa);
		countiesDGCstta.add(CountyConstants.FL_Okeechobee);
		countiesDGCstta.add(CountyConstants.FL_Orange);
		countiesDGCstta.add(CountyConstants.FL_Osceola);
		
		countiesDGCstta.add(CountyConstants.FL_Palm_Beach);
		countiesDGCstta.add(CountyConstants.FL_Pasco);
		countiesDGCstta.add(CountyConstants.FL_Pinellas);
		countiesDGCstta.add(CountyConstants.FL_Polk);
		countiesDGCstta.add(CountyConstants.FL_Putnam);
		
		countiesDGCstta.add(CountyConstants.FL_Santa_Rosa);
		countiesDGCstta.add(CountyConstants.FL_Sarasota);
		countiesDGCstta.add(CountyConstants.FL_Seminole);
		countiesDGCstta.add(CountyConstants.FL_St_Johns);
		countiesDGCstta.add(CountyConstants.FL_St_Lucie);
		countiesDGCstta.add(CountyConstants.FL_Sumter);
		countiesDGCstta.add(CountyConstants.FL_Suwannee);
		
		countiesDGCstta.add(CountyConstants.FL_Taylor);
		
		countiesDGCstta.add(CountyConstants.FL_Union);
		
		countiesDGCstta.add(CountyConstants.FL_Volusia);
		
		countiesDGCstta.add(CountyConstants.FL_Wakulla);
		countiesDGCstta.add(CountyConstants.FL_Walton);
		countiesDGCstta.add(CountyConstants.FL_Washington);
		
		
		
		SiteActivator activatorDGCstta = new SiteActivator(GWTDataSite.DG_TYPE);
		activatorDGCstta.enableFullAutomatic();
		activatorDGCstta.disable(Util.SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK);
		activatorDGCstta.addCommunity(3);
		activatorDGCstta.addCounties(countiesDGCstta);
		
		SiteActivator deactivatorDtCstta = preloadDTDeactivator();
		deactivatorDtCstta.clearCommunities();
		deactivatorDtCstta.addCommunity(3);
		deactivatorDtCstta.addCounties(countiesDGCstta);
		
		SiteActivator deactivatorAtiCstta = preloadDTDeactivator();
		deactivatorAtiCstta.siteType = GWTDataSite.ATI_TYPE;
		deactivatorAtiCstta.clearCommunities();
		deactivatorAtiCstta.addCommunity(3);
		deactivatorAtiCstta.addCounties(countiesDGCstta);
		
		SiteActivator deactivatorRvCstta = preloadDTDeactivator();
		deactivatorRvCstta.siteType = GWTDataSite.RV_TYPE;
		deactivatorRvCstta.clearCommunities();
		deactivatorRvCstta.addCommunity(3);
		deactivatorRvCstta.addCounties(countiesDGCstta);
		
		sqls.addAll(activatorDGCstta.generateSqls());
		sqls.addAll(deactivatorDtCstta.generateSqls());
		sqls.addAll(deactivatorAtiCstta.generateSqls());
		sqls.addAll(deactivatorRvCstta.generateSqls());
		
		System.err.println("Start betaStuff");
		for (String sql : sqls) {
			System.out.println(sql);
		}
		System.err.println("End betaStuff");		
	}
	
	protected static void gammStuff() {
		
		
		List<Integer> countiesReadyForAtiOnGamma = new ArrayList<Integer>();
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Bay);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Brevard);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Broward);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Charlotte);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Collier);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Escambia);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Hendry);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Highlands);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Hillsborough);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Indian_River);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Lake);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Leon);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Manatee);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Marion);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Martin);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Miami_Dade);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Osceola);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Okaloosa);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Palm_Beach);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Pasco);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Pinellas);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Sarasota);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Volusia);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Walton);
		
		
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Alachua);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_DeSoto);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Duval);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Flagler);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Hernando);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Lee);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Monroe);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Okeechobee);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Orange);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Polk);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_St_Lucie);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Santa_Rosa);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Seminole);
		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Sumter);
		
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Bay);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Okaloosa);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Walton);
		
		
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Duval);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Indian_River);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Leon);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_Marion);
//		countiesReadyForAtiOnGamma.add(CountyConstants.FL_St_Lucie);
		
		
		
		
		
		
		
		
		
//		Duval, Indian River, Leon, Marion, Saint Lucie
		
		
		List<Integer> countiesReadyForDTGOnGamma = new ArrayList<Integer>();
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Baker);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Bay);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Bradford);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Citrus);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Columbia);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Dixie);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Duval);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Franklin);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Gadsden);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Gilchrist);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Glades);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Gulf);
		
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Holmes);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Indian_River);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Leon);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Levy);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Madison);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Marion);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Nassau);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Okaloosa);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_St_Johns);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_St_Lucie);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Suwannee);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Taylor);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Union);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Wakulla);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Walton);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Washington);
		
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Hamilton);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Hardee);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Jefferson);
		countiesReadyForDTGOnGamma.add(CountyConstants.FL_Lafayette);
		
		
		for (Integer integer : countiesReadyForAtiOnGamma) {
			if(!countiesReadyForDTGOnGamma.contains(integer)) {
				System.out.print(integer + ",");
			}
		}
		
		System.out.println();
		
		
		SiteActivator activatorAti = preloadAtiActivator();
		activatorAti.addCounties(countiesReadyForDTGOnGamma);
		
		SiteActivator activator = preloadAtiActivator();
				
		new SiteActivator(GWTDataSite.DG_TYPE);
		activator.enableFullAutomatic();
		activator.disable(Util.SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK);
		activator.addCommunitiesFL(null);
		activator.addCounties(countiesReadyForDTGOnGamma);
		
		SiteActivator deactivatorDt = preloadDTDeactivator();
		deactivatorDt.addCounties(countiesReadyForDTGOnGamma);
		
		List<String> sqls = new ArrayList<String>();
		//sqls.addAll(activatorAti.generateSqls());
		sqls.addAll(activator.generateSqls());
		sqls.addAll(deactivatorDt.generateSqls());
		System.err.println("Start gammStuff");
		for (String sql : sqls) {
			System.out.println(sql);
		}
		System.err.println("End gammStuff");		
	}

	/*
	protected static void gammaRestOfATI(List<String> sqls) {
		SiteActivator activatorAti = preloadAtiActivator();
		
		activatorAti.addCounty(CountyConstants.FL_Alachua);
		activatorAti.addCounty(CountyConstants.FL_DeSoto);
		activatorAti.addCounty(CountyConstants.FL_Duval);
		activatorAti.addCounty(CountyConstants.FL_Flagler);
		activatorAti.addCounty(CountyConstants.FL_Hernando);
		activatorAti.addCounty(CountyConstants.FL_Lee);
		activatorAti.addCounty(CountyConstants.FL_Monroe);
		activatorAti.addCounty(CountyConstants.FL_Okeechobee);
		activatorAti.addCounty(CountyConstants.FL_Orange);
		activatorAti.addCounty(CountyConstants.FL_Polk);
		activatorAti.addCounty(CountyConstants.FL_St_Lucie);
		activatorAti.addCounty(CountyConstants.FL_Santa_Rosa);
		activatorAti.addCounty(CountyConstants.FL_Seminole);
		activatorAti.addCounty(CountyConstants.FL_Sumter);
		
		sqls.addAll(activatorAti.generateSqls());
		
		SiteActivator deactivatorDt = preloadDTDeactivator();
		deactivatorDt.addCounty(CountyConstants.FL_Alachua);
		deactivatorDt.addCounty(CountyConstants.FL_DeSoto);
		deactivatorDt.addCounty(CountyConstants.FL_Duval);
		deactivatorDt.addCounty(CountyConstants.FL_Flagler);
		deactivatorDt.addCounty(CountyConstants.FL_Hernando);
		deactivatorDt.addCounty(CountyConstants.FL_Lee);
		deactivatorDt.addCounty(CountyConstants.FL_Monroe);
		deactivatorDt.addCounty(CountyConstants.FL_Okeechobee);
		deactivatorDt.addCounty(CountyConstants.FL_Orange);
		deactivatorDt.addCounty(CountyConstants.FL_Polk);
		deactivatorDt.addCounty(CountyConstants.FL_St_Lucie);
		deactivatorDt.addCounty(CountyConstants.FL_Santa_Rosa);
		deactivatorDt.addCounty(CountyConstants.FL_Seminole);
		deactivatorDt.addCounty(CountyConstants.FL_Sumter);
		sqls.addAll(deactivatorDt.generateSqls());
	}
	*/

	private List<String> generateSqls() {
		List<String> sqls = new ArrayList<String>();
		
		if (countyIds.isEmpty()) {
			System.err.println("NO COUNTY to enable on");
			return sqls;
		}
		if(commIds.isEmpty()) {
			System.err.println("NO COMMUNITIES to enable on");
			return sqls;
		}
		
		try {
			
			String counties = "";
			for (Integer countyId : countyIds) {
				if(counties.isEmpty()) {
					counties += countyId;
				} else {
					counties += ", " + countyId;
				}
			}
			
			for (Integer commId : commIds) {
				sqls.add("update ts_community_sites set enableStatus = " + enableFlag + " where site_type = " + siteType + " and community_id = " + commId + " and county_id in (" + counties + ");");
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sqls;
	}

	public static SiteActivator preloadAtiActivator(){
		SiteActivator activator = new SiteActivator(GWTDataSite.ATI_TYPE);
		activator.enableFullAutomatic();
		activator.disable(Util.SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK);
		activator.addCommunitiesFL(null);
		return activator;
	}
	
	
	
	public static SiteActivator preloadDTDeactivator(){
		SiteActivator activator = new SiteActivator(GWTDataSite.DT_TYPE);
		activator.enableFullParentSite();
		activator.addCommunitiesFL(null);
		return activator;
	}
	
	public static List<Integer> getAtiNotWorkingSites() {
		List<Integer> counties = new ArrayList<Integer>();
		
		counties.add(CountyConstants.FL_Baker);
		counties.add(CountyConstants.FL_Calhoun);
		counties.add(CountyConstants.FL_Citrus);
		counties.add(CountyConstants.FL_Clay);
		counties.add(CountyConstants.FL_Columbia);
		counties.add(CountyConstants.FL_Dixie);
		counties.add(CountyConstants.FL_Franklin);
		counties.add(CountyConstants.FL_Gadsden);
		counties.add(CountyConstants.FL_Gilchrist);
		counties.add(CountyConstants.FL_Glades);
		counties.add(CountyConstants.FL_Gulf);
		counties.add(CountyConstants.FL_Hamilton);
		counties.add(CountyConstants.FL_Hardee);
		counties.add(CountyConstants.FL_Holmes);
		counties.add(CountyConstants.FL_Jackson);
		counties.add(CountyConstants.FL_Jefferson);
		counties.add(CountyConstants.FL_Lafayette);
		counties.add(CountyConstants.FL_Levy);
		counties.add(CountyConstants.FL_Liberty);
		counties.add(CountyConstants.FL_Madison);
		counties.add(CountyConstants.FL_Nassau);
		counties.add(CountyConstants.FL_Putnam);
		counties.add(CountyConstants.FL_St_Johns);
		counties.add(CountyConstants.FL_Suwannee);
		counties.add(CountyConstants.FL_Taylor);
		counties.add(CountyConstants.FL_Union);
		counties.add(CountyConstants.FL_Wakulla);
		counties.add(CountyConstants.FL_Washington);
		
		return counties;
	}
	
}

