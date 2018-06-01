package ro.cst.tsearch.bean;

import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.URLMaping;

@SuppressWarnings("unchecked")
public class TopframeFunctions {

	private static Vector menuAll = new Vector();
	static {
		menuAll.add(AppLinks.SETTINGS_MENU);
		menuAll.add(AppLinks.REP_MENU);
		menuAll.add(AppLinks.INV_MENU);
		menuAll.add(AppLinks.USER_MENU);
		menuAll.add(AppLinks.COMM_MENU);
	}


	static public void configDashboardPage(Vector menu, List links, long searchId, String formName, UserAttributes ua, int dash){
		if(ua.getDEFAULT_HOMEPAGE().equals(URLMaping.REPORTS_INTERVAL) && dash==1) {	
			menu.addAll(menuAll);
			links.add(AppLinks.getAlternateSearchLink(searchId));
			links.add(AppLinks.getChangeCommLink(searchId));
			links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
			//links.add(AppLinks.getMyATSLink(searchId,ua.getLOGIN()));
			links.add(AppLinks.getLogoutLink());
			return;
		}
		if(ua.getDEFAULT_HOMEPAGE().equals(URLMaping.REPORTS_TABLE_MONTH) && dash==0) {	
			menu.addAll(menuAll);
			links.add(AppLinks.getAlternateSearchLink(searchId));
			links.add(AppLinks.getChangeCommLink(searchId));
			links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
			//links.add(AppLinks.getMyATSLink(searchId,ua.getLOGIN()));
			links.add(AppLinks.getLogoutLink());
			return;
		}
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configSimple(List links){
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configStartTSPage(Vector menu, List links, long searchId, String formName, UserAttributes ua, boolean isHome){
		
		if(ua.getDEFAULT_HOMEPAGE().equals(URLMaping.StartTSPage) && isHome) {
			menu.addAll(menuAll);
			links.add(AppLinks.getAlternateSearchLink(searchId));
			links.add(AppLinks.getTsdIndexLink(formName));
			links.add(AppLinks.getChangeCommLink(searchId));
			links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
			//links.add(AppLinks.getMyATSLink(searchId,ua.getLOGIN()));
			links.add(AppLinks.getLogoutLink());
			return;
		}
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configOrderTSPage(Vector menu, List links, long searchId, String formName, UserAttributes ua) {
		links.add(AppLinks.getSubmitOrderLink());
		links.add(AppLinks.getResetFormLink());
		links.add(AppLinks.getMyProfileLink(searchId, ua.getLOGIN()));
		links.add(AppLinks.getLogoutLink("?" + RequestParams.USER_COMMUNITYID + "=" + ua.getCOMMID().longValue(), false));
		//links.add(AppLinks.getCloseLink("Log Out"));
	}

	static public void configTSDIndexedPageJSP(Vector menu, List links, long searchId, UserAttributes ua){
		//menu.addAll(menuAll);	
		//UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId, "_top"));
		//links.add(AppLinks.getAlternateSearchLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getParentSiteNoSaveLink(searchId, "_top"));
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getDSMAWindowCloseLink());
	} 

	static public void configAttachPage(Vector menu, List links, long searchId){
		//menu.addAll(menuAll);	
		//UserAttributes ua = InstanceManager.getCurrentInstance().getCurrentUser();
		//links.add(AppLinks.getAlternateSearchLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configParentSearch(Vector menu, List links, long searchId){
		//menu.addAll(menuAll);
		//UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		//links.add(AppLinks.getAlternateSearchLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configPARENT_SITE_RESPONSE(Vector menu, List links, long searchId){
		//menu.addAll(menuAll);
		//UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
		//links.add(AppLinks.getLogoutLink());
		//links.add(AppLinks.getWindowCloseLink());
		links.add("<div id='slotIntermediaryResultsButtonsBarUp'></div>");
	}

	static public void configUSER_LIST(Vector menu, List links, long searchId, UserAttributes cua){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,cua.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configUSER_ADD(Vector menu, List links, long searchId, UserAttributes ua){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));	
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configUSER_EDIT(Vector menu, List links, long searchId, UserAttributes ua){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getCloseLink());
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configMY_PROFILE(Vector menu, List links, long searchId, UserAttributes ua){
		//links.add(AppLinks.getChangeCommLink(searchId));
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configUSER_VIEW(Vector menu, List links, long searchId, UserAttributes ua){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
		//links.add(AppLinks.getCloseLink());
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configREPORT_MONTH(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configREPORT_YEAR(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configREPORT_DAY(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configTHROUGHPUT_GENERAL(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
		links.add(AppLinks.getWindowCloseLink());
	}
	static public void configINCOME_GENERAL(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configINVOICE_DAY(Vector menu, List links, long searchId, UserAttributes ua){
		//menu.add(AppLinks.REP_MENU);
		if(ua.getDEFAULT_HOMEPAGE().equals(URLMaping.INVOICE_MONTH)) {	
			menu.addAll(menuAll);
			links.add(AppLinks.getAlternateSearchLink(searchId));
			links.add(AppLinks.getChangeCommLink(searchId));
			links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
			//links.add(AppLinks.getMyATSLink(searchId,ua.getLOGIN()));
			links.add(AppLinks.getLogoutLink());
			return;
		}
		links.add(AppLinks.getSendEmailLink(URLMaping.INVOICE_DAY));
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configINVOICE_MONTH(Vector menu, List links, long searchId, UserAttributes ua){
		//menu.add(AppLinks.REP_MENU);
		if(ua.getDEFAULT_HOMEPAGE().equals(URLMaping.INVOICE_MONTH)) {	
			menu.addAll(menuAll);
			links.add(AppLinks.getAlternateSearchLink(searchId));
			links.add(AppLinks.getChangeCommLink(searchId));
			links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
			//links.add(AppLinks.getMyATSLink(searchId,ua.getLOGIN()));
			links.add(AppLinks.getLogoutLink());
			return;
		}
		links.add(AppLinks.getSendEmailLink(URLMaping.INVOICE_MONTH));
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configINVOICE_MONTH_DETAILED(Vector menu, List links, long searchId, UserAttributes ua){
		if(ua.getDEFAULT_HOMEPAGE().equals(URLMaping.INVOICE_MONTH)) {	
			menu.addAll(menuAll);
			links.add(AppLinks.getAlternateSearchLink(searchId));
			links.add(AppLinks.getChangeCommLink(searchId));
			links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
			//links.add(AppLinks.getMyATSLink(searchId,ua.getLOGIN()));
			links.add(AppLinks.getLogoutLink());
			return;
		}
		links.add(AppLinks.getSendEmailLink(URLMaping.INVOICE_MONTH_DETAILED));
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configINVOICE_INTERVAL(Vector menu, List links, long searchId, UserAttributes ua){
		//menu.add(AppLinks.REP_MENU);
		if(ua.getDEFAULT_HOMEPAGE().equals(URLMaping.INVOICE_MONTH)) {	
			menu.addAll(menuAll);
			links.add(AppLinks.getAlternateSearchLink(searchId));
			links.add(AppLinks.getChangeCommLink(searchId));
			links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN()));
			//links.add(AppLinks.getMyATSLink(searchId,ua.getLOGIN()));
			links.add(AppLinks.getLogoutLink());
			return;
		}
		links.add(AppLinks.getSendEmailLink(URLMaping.INVOICE_INTERVAL));
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configCOMMUNITY_PAGE_VIEW(Vector menu, List links, UserAttributes crtUser, long searchId){
		//menu.addAll(menuAll);//B4190
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,crtUser.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configCOMMUNITY_PAGE_EDIT(Vector menu, List links, UserAttributes crtUser, long searchId){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,crtUser.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configCOMMUNITY_PAGE_ADD(Vector menu, List links, UserAttributes crtUser, long searchId){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));	
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,crtUser.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configCOMMUNITY_PAGE_ADMIN(Vector menu, List links, UserAttributes crtUser, long searchId){
		///menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,crtUser.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configCATEGORY_PAGE_ADMIN(Vector menu, List links, UserAttributes crtUser, long searchId){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));	
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,crtUser.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configCATEGORY_PAGE_ADD(Vector menu, List links, UserAttributes crtUser, long searchId){
		//menu.addAll(menuAll);
		//links.add(AppLinks.getBackToAutoSearchPageLink(searchId));	
		//links.add(AppLinks.getTsdIndexNoSaveLink(searchId));
		//links.add(AppLinks.getMyProfileLink(searchId,crtUser.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getChangeCommLink(searchId));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configCONVERT_TO_PDF_REDIR(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
	}

	static public void configCONVERT_TO_PDF_SHOW(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
	}

	static public void configCREATE_TSD(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
	}

	static public void configPDF_SHOW(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
	}

	static public void configUSER_ADMIN(Vector menu, List links, UserAttributes ua,long searchId){
		//menu.addAll(menuAll);	
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}

	static public void configUSER_SADMIN(Vector menu, List links, UserAttributes ua,long searchId){
		//menu.addAll(menuAll);	
		//links.add(AppLinks.getMyProfileLink(searchId,ua.getLOGIN(),TSOpCode.MY_PROFILE_VIEW));
		//links.add(AppLinks.getLogoutLink());
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configERROR_PAGE(Vector menu, List links){
		//	menu.add(AppLinks.REP_MENU);	
	}
	
	static public void configNOT_IMPLEMENTED_PAGE(Vector menu, List links){
		//	menu.add(AppLinks.REP_MENU);
	}
	
	static public void configDSMA(Vector menu, List links){
		//menu.add(AppLinks.REP_MENU);
		//links.add(AppLinks.getDSMAWindowCloseLink());
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configSETTING_RATES(Vector menu, List links){
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configMY_ATS_VIEW(Vector menu, List links, long searchId, UserAttributes ua){
		links.add(AppLinks.getWindowCloseLink());
	}
	
	static public void configGENERIC_NAME_FILTER_TEST(Vector menu, List links){
		links.add(AppLinks.getWindowCloseLink());
	}
}
