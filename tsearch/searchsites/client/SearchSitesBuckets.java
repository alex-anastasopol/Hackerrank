package ro.cst.tsearch.searchsites.client;

import static ro.cst.tsearch.searchsites.client.GWTDataSite.AC_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.AD_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.AK_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.AM_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.AOM_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.AO_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.ATI_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.ATS_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.BOR_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.BS_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.BT_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.CC_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.PRI_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.CO_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.COM_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DD_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DG_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DI_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DL_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DMV_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DN_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DR_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.DT_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.FD_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.GM_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.HO_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.IL_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.IM_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.IS_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.LA_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.LN_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.LW_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.MC_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.MERS_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.MH_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.MS_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.NA_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.NB_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.NETR_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.NR_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.NTN_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.OI_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.OR_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.PA_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.PC_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.PD_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.PF_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.PI_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.PR_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.R2_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.RO_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.RV_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.RVI_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.SB_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.SF_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.SK_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.SPS_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.SRC_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.ST_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.ADI_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.TP_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.TR_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.TR2_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.TS_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.TU_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.VU_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.WP_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.YA_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.YB_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.YC_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.YD_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.YE_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.YF_TYPE;
import static ro.cst.tsearch.searchsites.client.GWTDataSite.YG_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SearchSitesBuckets {

	// 0-999
	public static final Integer[]				ASSESSOR_LIKE_SITES		= { AM_TYPE, AO_TYPE, IS_TYPE, NB_TYPE, PRI_TYPE };
	// 1000-1999
	public static final Integer[]				COUNTYTAX_LIKE_SITES	= { TR_TYPE, TR2_TYPE, TU_TYPE, NTN_TYPE, BOR_TYPE };
	// 2000-2999
	public static final Integer[]				CITYTAX_LIKE_SITES		= { YA_TYPE, YB_TYPE, YC_TYPE, YD_TYPE, YE_TYPE, YF_TYPE, YG_TYPE };
	// 3000-3999
	public static final Integer[]				ATS_LIKE_SITES			= { ATS_TYPE };
	// 4000-4999
	public static final Integer[]				SF_LIKE_SITES			= { SF_TYPE };
	// 5000-5999
	public static final Integer[]				PF_LIKE_SITES			= { PF_TYPE };
	// 6000-6999
	public static final Integer[]				RO_LIKE_SITES			= {
																		RO_TYPE,
																		RV_TYPE,
																		DT_TYPE,
																		LA_TYPE,
																		AD_TYPE,
																		TS_TYPE,
																		OR_TYPE,
																		TP_TYPE,
																		ST_TYPE,
																		SK_TYPE,
																		R2_TYPE,
																		PI_TYPE,
																		AC_TYPE,
																		ADI_TYPE,
																		DG_TYPE,
																		ATI_TYPE,
																		SRC_TYPE,
																		IM_TYPE,
																		AK_TYPE,
																		CC_TYPE,
																		DL_TYPE,
																		NA_TYPE,
																		AOM_TYPE,
																		RVI_TYPE
																		};
	// 7000-7999
	public static final Integer[]				PC_LIKE_SITES			= { PC_TYPE };														
	// 8000-8999
	public static final Integer[]				CO_LIKE_SITES			= { CO_TYPE, COM_TYPE, PR_TYPE };		
	// 9000-9999
	public static final Integer[]				NR_LIKE_SITES			= {
																		SB_TYPE,
																		OI_TYPE,
																		HO_TYPE,
																		WP_TYPE,
																		LN_TYPE,
																		NR_TYPE,
																		DD_TYPE,
																		PD_TYPE,
																		DR_TYPE,
																		BS_TYPE,
																		BT_TYPE,
																		DI_TYPE,
																		DN_TYPE,
																		FD_TYPE,
																		GM_TYPE,
																		IL_TYPE,
																		LW_TYPE,
																		MC_TYPE,
																		MH_TYPE,
																		MS_TYPE,
																		MERS_TYPE,
																		NETR_TYPE,
																		SPS_TYPE,
																		VU_TYPE,
																		DMV_TYPE
																		};																	
	// 10000-10999
	public static final Integer[]				PA_LIKE_SITES			= { PA_TYPE };														

	// ---- keep the order and size of ALL_SITES and ALL_SITES_STRING
	public static final Integer[][]				ALL_SITES				= new Integer[][] { ASSESSOR_LIKE_SITES, COUNTYTAX_LIKE_SITES, CITYTAX_LIKE_SITES,
																		ATS_LIKE_SITES, SF_LIKE_SITES, PF_LIKE_SITES, RO_LIKE_SITES, PC_LIKE_SITES,
																		CO_LIKE_SITES,
																		NR_LIKE_SITES, PA_LIKE_SITES };

	private static final String[]				ALL_SITES_STRING		= { "ASSESSOR_LIKE_SITES", "COUNTYTAX_LIKE_SITES", "CITYTAX_LIKE_SITES",
																		"ATS_LIKE_SITES", "SF_LIKE_SITES", "PF_LIKE_SITES", "RO_LIKE_SITES", "PC_LIKE_SITES",
																		"CO_LIKE_SITES", "NR_LIKE_SITES", "PA_LIKE_SITES" };

	// ----

	private HashMap<Integer, SearchSitesBucket>	allSitesBuckets			= new HashMap<Integer, SearchSitesBucket>();

	private List<SearchSitesBucket>				searchSitesBuckets		= new ArrayList<SearchSitesBuckets.SearchSitesBucket>();

	private static SearchSitesBuckets			instance				= null;

	private SearchSitesBuckets() {
		searchSitesBuckets.clear();
		for (int i = 0; i < ALL_SITES.length; i++) {
			searchSitesBuckets.add(new SearchSitesBucket(i * 1000, i * 1000 + 999, ALL_SITES[i], null));
		}

		allSitesBuckets.clear();
		for (SearchSitesBucket sb : searchSitesBuckets) {
			for (Integer i : sb.getSites()) {
				allSitesBuckets.put(i, sb);
			}
		}
	}

	public static SearchSitesBuckets getSearchSitesBucketsInstance() {
		if (instance == null) {
			instance = new SearchSitesBuckets();
		}

		return instance;
	}

	public static SearchSitesBucket getSiteBucket(int siteId) {
		return getSearchSitesBucketsInstance().allSitesBuckets.get(siteId);
	}
	
	/**
	 * 
	 * @param siteId
	 * @param orderIndex
	 * @return -1 if the site is not fitted in a bucket, bucket low index + orderIndex otherwise
	 */
	public static int getSiteOrderIndexInBucket(int siteId, int orderIndex) {
		SearchSitesBucket sb = getSiteBucket(siteId);
		if(sb == null)
			return -1;
		
		return sb.getLowIndex() + orderIndex % 1000;
	}

	public static List<SearchSitesBucket> getSearchSitesBucketsList() {
		return getSearchSitesBucketsInstance().searchSitesBuckets;
	}

	public class SearchSitesBucket {
		private int				low			= 0;
		private int				high		= 0;
		private List<Integer>	sites		= new ArrayList<Integer>();
		private String			bucketName	= "";

		public SearchSitesBucket(int low, int high, Integer[] sites, String bucketName) {
			this.setLowIndex(low);
			this.setHighIndex(high);
			this.setSites(Arrays.asList(sites));
			this.setName(bucketName);
		}

		public SearchSitesBucket(int low, int high, List<Integer> sites, String bucketName) {
			this.setLowIndex(low);
			this.setHighIndex(high);
			this.setSites(new ArrayList<Integer>(sites));
			this.setName(bucketName);
		}

		public List<Integer> getSites() {
			return sites;
		}

		public void setSites(List<Integer> sites) {
			this.sites = sites;
		}

		public int getHighIndex() {
			return high;
		}

		public void setHighIndex(int high) {
			this.high = high;
		}

		public int getLowIndex() {
			return low;
		}

		public void setLowIndex(int low) {
			this.low = low;
		}

		public String getName() {
			return bucketName;
		}

		public void setName(String bucketName) {
			this.bucketName = bucketName;
		}
	}
}
