package ro.cst.tsearch.connection.http3;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.StringUtils;

public class TXGenericTaxNetAOSite extends HttpSite3 {
	
	public static String[] CALLAHAN_18 = { CountyConstants.TX_Callahan_STRING, CountyConstants.TX_Erath_STRING,
		CountyConstants.TX_Falls_STRING, CountyConstants.TX_Floyd_STRING, CountyConstants.TX_Hartley_STRING,
		CountyConstants.TX_Hopkins_STRING, CountyConstants.TX_Knox_STRING, CountyConstants.TX_Lipscomb_STRING,
		CountyConstants.TX_Mills_STRING, CountyConstants.TX_Montague_STRING, CountyConstants.TX_Morris_STRING,
		CountyConstants.TX_Motley_STRING, CountyConstants.TX_Palo_Pinto_STRING, CountyConstants.TX_Reeves_STRING,
		CountyConstants.TX_Runnels_STRING, CountyConstants.TX_Somervell_STRING, CountyConstants.TX_Stephens_STRING,
		CountyConstants.TX_Van_Zandt_STRING};
	public static List<String>	CALLAHAN_18_LIST	= Arrays.asList(CALLAHAN_18);
	
	public static String[] COLEMAN_1 = {CountyConstants.TX_Coleman_STRING};
	public static List<String>	COLEMAN_1_LIST	= Arrays.asList(COLEMAN_1);
	
	public static String[] ANDREWS_66 = {CountyConstants.TX_Andrews_STRING, CountyConstants.TX_Atascosa_STRING,
		CountyConstants.TX_Bailey_STRING, CountyConstants.TX_Bandera_STRING, CountyConstants.TX_Blanco_STRING,
		CountyConstants.TX_Brewster_STRING, CountyConstants.TX_Brooks_STRING, CountyConstants.TX_Brown_STRING,
		CountyConstants.TX_Caldwell_STRING, CountyConstants.TX_Camp_STRING, CountyConstants.TX_Cass_STRING,
		CountyConstants.TX_Cherokee_STRING, CountyConstants.TX_Colorado_STRING, CountyConstants.TX_Comanche_STRING,
		CountyConstants.TX_Dallam_STRING, CountyConstants.TX_Deaf_Smith_STRING, CountyConstants.TX_Delta_STRING,
		CountyConstants.TX_Dimmit_STRING, CountyConstants.TX_Duval_STRING, CountyConstants.TX_Edwards_STRING,
		CountyConstants.TX_Fannin_STRING, CountyConstants.TX_Fayette_STRING, CountyConstants.TX_Gaines_STRING,
		CountyConstants.TX_Gillespie_STRING, CountyConstants.TX_Hale_STRING, CountyConstants.TX_Hamilton_STRING,
		CountyConstants.TX_Haskell_STRING, CountyConstants.TX_Hill_STRING, CountyConstants.TX_Hockley_STRING,
		CountyConstants.TX_Hudspeth_STRING, CountyConstants.TX_Kenedy_STRING, CountyConstants.TX_Kimble_STRING,
		CountyConstants.TX_Kinney_STRING, CountyConstants.TX_Lamb_STRING, CountyConstants.TX_La_Salle_STRING, 
		CountyConstants.TX_Lavaca_STRING, CountyConstants.TX_Lee_STRING, CountyConstants.TX_Madison_STRING, 
		CountyConstants.TX_Maverick_STRING, CountyConstants.TX_McMullen_STRING, CountyConstants.TX_Milam_STRING, 
		CountyConstants.TX_Moore_STRING, CountyConstants.TX_Navarro_STRING, CountyConstants.TX_Newton_STRING, 
		CountyConstants.TX_Parmer_STRING, CountyConstants.TX_Polk_STRING, CountyConstants.TX_Rains_STRING, 
		CountyConstants.TX_Real_STRING,	CountyConstants.TX_Schleicher_STRING, CountyConstants.TX_Scurry_STRING, 
		CountyConstants.TX_Shackelford_STRING, CountyConstants.TX_Shelby_STRING, CountyConstants.TX_Sutton_STRING, 
		CountyConstants.TX_Swisher_STRING, CountyConstants.TX_Terrell_STRING, CountyConstants.TX_Titus_STRING, 
		CountyConstants.TX_Trinity_STRING, CountyConstants.TX_Upshur_STRING, CountyConstants.TX_Upton_STRING, 
		CountyConstants.TX_Uvalde_STRING, CountyConstants.TX_Val_Verde_STRING, CountyConstants.TX_Willacy_STRING,	
		CountyConstants.TX_Winkler_STRING, CountyConstants.TX_Yoakum_STRING, CountyConstants.TX_Zapata_STRING,	
		CountyConstants.TX_Zavala_STRING};
	public static List<String>	ANDREWS_66_LIST	= Arrays.asList(ANDREWS_66);
	
	public static String[] ARMSTRONG_61 = {CountyConstants.TX_Armstrong_STRING, CountyConstants.TX_Bee_STRING,
		CountyConstants.TX_Borden_STRING, CountyConstants.TX_Burleson_STRING, CountyConstants.TX_Carson_STRING,
		CountyConstants.TX_Chambers_STRING, CountyConstants.TX_Childress_STRING, CountyConstants.TX_Clay_STRING,
		CountyConstants.TX_Coke_STRING, CountyConstants.TX_Concho_STRING, CountyConstants.TX_Cottle_STRING,
		CountyConstants.TX_Culberson_STRING, CountyConstants.TX_Dawson_STRING, CountyConstants.TX_DeWitt_STRING,
		CountyConstants.TX_Dickens_STRING, CountyConstants.TX_Donley_STRING, CountyConstants.TX_Eastland_STRING,
		CountyConstants.TX_Foard_STRING, CountyConstants.TX_Franklin_STRING, CountyConstants.TX_Freestone_STRING,
		CountyConstants.TX_Frio_STRING, CountyConstants.TX_Goliad_STRING, CountyConstants.TX_Gonzales_STRING,
		CountyConstants.TX_Hall_STRING, CountyConstants.TX_Hansford_STRING, CountyConstants.TX_Hardin_STRING,
		CountyConstants.TX_Hemphill_STRING, CountyConstants.TX_Houston_STRING, CountyConstants.TX_Hutchinson_STRING,
		CountyConstants.TX_Irion_STRING, CountyConstants.TX_Jack_STRING, CountyConstants.TX_Jeff_Davis_STRING,
		CountyConstants.TX_Jim_Hogg_STRING, CountyConstants.TX_Jones_STRING, CountyConstants.TX_Karnes_STRING,
		CountyConstants.TX_Kent_STRING, CountyConstants.TX_King_STRING, CountyConstants.TX_Lampasas_STRING,
		CountyConstants.TX_Leon_STRING, CountyConstants.TX_Loving_STRING, CountyConstants.TX_Lynn_STRING, 
		CountyConstants.TX_Marion_STRING, CountyConstants.TX_Mason_STRING, CountyConstants.TX_McCulloch_STRING, 
		CountyConstants.TX_Menard_STRING, CountyConstants.TX_Nacogdoches_STRING, CountyConstants.TX_Nolan_STRING, 
		CountyConstants.TX_Panola_STRING, CountyConstants.TX_Pecos_STRING, CountyConstants.TX_Presidio_STRING, 
		CountyConstants.TX_Reagan_STRING, CountyConstants.TX_Refugio_STRING, CountyConstants.TX_Rusk_STRING, 
		CountyConstants.TX_San_Patricio_STRING,	CountyConstants.TX_San_Saba_STRING, CountyConstants.TX_Sherman_STRING, 
		CountyConstants.TX_Starr_STRING, CountyConstants.TX_Sterling_STRING, CountyConstants.TX_Wheeler_STRING, 
		CountyConstants.TX_Wilbarger_STRING, CountyConstants.TX_Young_STRING};
	public static List<String>	ARMSTRONG_61_LIST	= Arrays.asList(ARMSTRONG_61);
	
	public static String[] COCHRAN_10 = {CountyConstants.TX_Cochran_STRING, CountyConstants.TX_Crosby_STRING,
		CountyConstants.TX_Garza_STRING, CountyConstants.TX_Glasscock_STRING, CountyConstants.TX_Howard_STRING,
		CountyConstants.TX_Jasper_STRING, CountyConstants.TX_Live_Oak_STRING, CountyConstants.TX_Red_River_STRING,
		CountyConstants.TX_Robertson_STRING, CountyConstants.TX_Terry_STRING};
	public static List<String> COCHRAN_10_LIST	= Arrays.asList(COCHRAN_10);
	
	public static String[] ANDERSON_5 = {CountyConstants.TX_Anderson_STRING, CountyConstants.TX_Bosque_STRING,
		CountyConstants.TX_Ector_STRING, CountyConstants.TX_Tyler_STRING, CountyConstants.TX_Wharton_STRING};
	public static List<String> ANDERSON_5_LIST	= Arrays.asList(ANDERSON_5);
	
	public static String[] HARRISSON_7 = {CountyConstants.TX_Harrison_STRING, CountyConstants.TX_Henderson_STRING,
		CountyConstants.TX_Hood_STRING, CountyConstants.TX_Midland_STRING, CountyConstants.TX_Parker_STRING,
		CountyConstants.TX_Tom_Green_STRING, CountyConstants.TX_Wise_STRING};
	public static List<String> HARRISSON_7_LIST	= Arrays.asList(HARRISSON_7);
	
	public static String[] ANGELINA_42 = {CountyConstants.TX_Angelina_STRING, CountyConstants.TX_Bell_STRING,
		/*CountyConstants.TX_Bexar_STRING,*/ CountyConstants.TX_Brazoria_STRING, CountyConstants.TX_Brazos_STRING,
		CountyConstants.TX_Burnet_STRING, CountyConstants.TX_Calhoun_STRING, CountyConstants.TX_Cameron_STRING,
		CountyConstants.TX_Collin_STRING, CountyConstants.TX_Comal_STRING, CountyConstants.TX_Cooke_STRING,
		CountyConstants.TX_Coryell_STRING, CountyConstants.TX_Denton_STRING, CountyConstants.TX_Ellis_STRING,
		CountyConstants.TX_El_Paso_STRING, CountyConstants.TX_Grayson_STRING, CountyConstants.TX_Gregg_STRING,
		CountyConstants.TX_Guadalupe_STRING, CountyConstants.TX_Hidalgo_STRING, CountyConstants.TX_Hunt_STRING,
		CountyConstants.TX_Jefferson_STRING, CountyConstants.TX_Kaufman_STRING, CountyConstants.TX_Kendall_STRING,
		CountyConstants.TX_Kerr_STRING, CountyConstants.TX_Kleberg_STRING, CountyConstants.TX_Lamar_STRING,
		CountyConstants.TX_Liberty_STRING, CountyConstants.TX_Llano_STRING, CountyConstants.TX_Matagorda_STRING,
		CountyConstants.TX_McLennan_STRING, CountyConstants.TX_Nueces_STRING, CountyConstants.TX_Rockwall_STRING,
		CountyConstants.TX_San_Jacinto_STRING, CountyConstants.TX_Taylor_STRING, CountyConstants.TX_Travis_STRING,
		CountyConstants.TX_Victoria_STRING, CountyConstants.TX_Walker_STRING, CountyConstants.TX_Waller_STRING,
		CountyConstants.TX_Webb_STRING, CountyConstants.TX_Wichita_STRING, CountyConstants.TX_Wilson_STRING,
		CountyConstants.TX_Wood_STRING};
	public static List<String> ANGELINA_42_LIST	= Arrays.asList(ANGELINA_42);
	
	public static String[] BOWIE_1 = {CountyConstants.TX_Bowie_STRING};
	public static List<String> BOWIE_1_LIST	= Arrays.asList(BOWIE_1);
	
	public static String[] AUSTIN_3 = {CountyConstants.TX_Austin_STRING, /*CountyConstants.TX_Johnson_STRING,*/
		CountyConstants.TX_Smith_STRING};
	public static List<String> AUSTIN_3_LIST	= Arrays.asList(AUSTIN_3);
	
	public static String[] BRISCOE_4 = {CountyConstants.TX_Briscoe_STRING, CountyConstants.TX_Gray_STRING,
		CountyConstants.TX_Oldham_STRING, CountyConstants.TX_Roberts_STRING};
	public static List<String> BRISCOE_4_LIST	= Arrays.asList(BRISCOE_4);
	
	public static String[] JIM_WELLS_1 = {CountyConstants.TX_Jim_Wells_STRING};
	public static List<String> JIM_WELLS_1_LIST	= Arrays.asList(JIM_WELLS_1);
	
	public static String[] OCHILTREE_1 = {CountyConstants.TX_Ochiltree_STRING};
	public static List<String> OCHILTREE_1_LIST	= Arrays.asList(OCHILTREE_1);
	
	public static String[] ARCHER_15 = {CountyConstants.TX_Archer_STRING, CountyConstants.TX_Baylor_STRING,
		CountyConstants.TX_Castro_STRING, CountyConstants.TX_Collingsworth_STRING, CountyConstants.TX_Crane_STRING,
		CountyConstants.TX_Crockett_STRING, CountyConstants.TX_Fisher_STRING, CountyConstants.TX_Hardeman_STRING,
		CountyConstants.TX_Martin_STRING, CountyConstants.TX_Mitchell_STRING, CountyConstants.TX_Sabine_STRING,
		CountyConstants.TX_San_Augustine_STRING, CountyConstants.TX_Stonewall_STRING, CountyConstants.TX_Throckmorton_STRING,
		CountyConstants.TX_Ward_STRING};
	public static List<String> ARCHER_15_LIST	= Arrays.asList(ARCHER_15);
	
	public static String[] GRIMES_3 = {CountyConstants.TX_Grimes_STRING, CountyConstants.TX_Jackson_STRING,
		CountyConstants.TX_Limestone_STRING	
	};
	public static List<String> GRIMES_3_LIST	= Arrays.asList(GRIMES_3);
	
	public static String[] POTTER_2 = {CountyConstants.TX_Potter_STRING, CountyConstants.TX_Randall_STRING};
	public static List<String> POTTER_2_LIST	= Arrays.asList(POTTER_2);
	
	public static String[] ARANSAS_11 = {CountyConstants.TX_Aransas_STRING, CountyConstants.TX_Bastrop_STRING,
		CountyConstants.TX_Fort_Bend_STRING, CountyConstants.TX_Galveston_STRING,
		CountyConstants.TX_Lubbock_STRING, CountyConstants.TX_Medina_STRING, CountyConstants.TX_Montgomery_STRING,
		CountyConstants.TX_Orange_STRING, CountyConstants.TX_Washington_STRING, CountyConstants.TX_Williamson_STRING};
	public static List<String> ARANSAS_11_LIST	= Arrays.asList(ARANSAS_11);
	
	public static String[] HARRIS_1 = {CountyConstants.TX_Harris_STRING};
	public static List<String> HARRIS_1_LIST	= Arrays.asList(HARRIS_1);
	
	public static String[] TARRANT_1 = {CountyConstants.TX_Tarrant_STRING};
	public static List<String> TARRANT_1_LIST	= Arrays.asList(TARRANT_1);
	
	public static String[] DALLAS_1 = {CountyConstants.TX_Dallas_STRING};
	public static List<String> DALLAS_1_LIST	= Arrays.asList(DALLAS_1);
	
	private static String[] COLEMAN_PARAMS_NAME_REPL1 = {"k.prop_name", "k.prop_geoid", "s.prop_id", "k.situs_number", "k.situs_street"};
	private static String[] COLEMAN_PARAMS_NAME_REPL2 = {"k.OwnerName", "k.SecKey", "k.PIDN", "k.SitusNo", "k.SitusStreet"};
	private static String[] COLEMAN_PARAMS_VALUE_REPL1 = {"i.detail", "i.themeFile", "i.where"};
	private static String[] COLEMAN_PARAMS_VALUE_REPL2 = {"detail.php?i_search_form_basket=&whereclause=&i_county_code=&theKey=", "theme.php",
		" where (colemanmain.PIDN like 'R%') and colemanmain.PIDN = colemanvalues.vPIDN"};
	private static String[] COLEMAN_PARAMS_ADD =   {"i.CityCode", "i.ExemptionFlags", "i.ISDCode", "i.MinLandSPTB"};
	
	private static String[] ARMSTRONG_COCHRAN_ANDERSON_PARAMS_NAME_REPL1 = {"k.appr_owner_name", "k.prop_type_cd", "k.geo_id", "k.prop_id", "k.situs_num", "k.situs_street"};
	private static String[] ARMSTRONG_PARAMS_NAME_REPL2 = {"k.owner_name", "k.prop_type", "k.acct", "k.parcel_id", "k.prop_street_num", "k.prop_street"};
	private static String[] COCHRAN_PARAMS_NAME_REPL2 = {"k.owner_name", "n.prop_type", "k.account_no", "e.PIDN", "e.situs_num", "k.situs_street"};
	private static String[] ANDERSON_PARAMS_NAME_REPL2 = {"k.own_name", "k.aay_division_cdx", "k.aay_geo_account_num", "k.account_num", "k.toa_street_num", "k.toa_street_name"};
	private static String[] ARMSTRONG_COCHRAN_ANDERSON_PARAMS_VALUE_REPL1 = {"i.detail", "i.themeFile", "i.where"};
	private static String[] ARMSTRONG_PARAMS_VALUE_REPL2 = {"detail.php?theKey=", "theme.php", " where 1=1 "};
	private static String[] COCHRAN_PARAMS_VALUE_REPL2 = {"", "", " where 1=1 "};
	private static String[] ANDERSON_PARAMS_VALUE_REPL2 = {"", "cert_theme.php", " where own_privacy_ind = 'N' "};
	private static String[] ARMSTRONG_PARAMS_ADD = {"i.cat_code_pri"};
	private static String[] COCHRAN_PARAMS_ADD = {"i.jur_list", "i.jur_list", "i.ov_65_exempt", "i.sptb"};
	private static String[] ANDERSON_PARAMS_ADD = {"i.city_state_cd", "i.hs_ex_ind", "i.isd_state_cd", "i.o65_ex_ind"};
	private static String[] ARMSTRONG_PARAMS_REMOVE = {"b.entities", "i.appr_addr_state", "i.imprv_state_cd", "i.land_state_cd", "i.ov65_exempt"};
	private static String[] COCHRAN_PARAMS_REMOVE = {"b.entities", "i.appr_addr_state", "i.detail", "i.entities", "i.imprv_state_cd", "i.land_state_cd", "i.ov65_exempt", "i.themeFile"};
	private static String[] ANDERSON_PARAMS_REMOVE = {"b.entities", "i.appr_addr_state", "i.detail", "i.entities", "i.hs_exempt", "i.imprv_state_cd", "i.land_state_cd", "i.ov65_exempt"};
	
	private static String[] BOWIE_AUSTIN_PARAMS_NAME_REPL1 = {"k.appr_owner_name", "k.prop_type_cd", "k.geo_id", "k.prop_id", "k.situs_num", "k.situs_street"};
	private static String[] BOWIE_PARAMS_NAME_REPL2 = {"k.owner_name", "n.prop_type", "k.account_no", "e.PIDN", "e.situs_num", "k.situs_street"};
	private static String[] AUSTIN_PARAMS_NAME_REPL2 = {"k.own_name", "k.aay_division_cdx", "k.aay_geo_account_num", "k.account_num", "k.toa_street_num", "k.toa_street_name"};
	private static String[] BOWIE_AUSTIN_PARAMS_VALUE_REPL1 = {"i.where"};
	private static String[] BOWIE_PARAMS_VALUE_REPL2 = {" where 1=1 "};
	private static String[] AUSTIN_PARAMS_VALUE_REPL2 = {" where own_privacy_ind = 'N' "};
	private static String[] BOWIE_PARAMS_ADD = {"i.jur_list", "i.jur_list", "i.ov_65_exempt", "i.sptb"};
	private static String[] AUSTIN_PARAMS_ADD = {"i.city_state_cd", "i.hs_ex_ind", "i.isd_state_cd", "i.o65_ex_ind"};
	private static String[] BOWIE_PARAMS_REMOVE = {"b.entities", "i.appr_addr_state", "i.detail", "i.entities", "i.imprv_state_cd", "i.land_state_cd", "i.ov65_exempt"};
	private static String[] AUSTIN_PARAMS_REMOVE = {"b.entities", "i.appr_addr_state", "i.detail", "i.entities", "i.hs_exempt", "i.imprv_state_cd", "i.land_state_cd", "i.ov65_exempt"};
	
	private static String[] JIMWELLS_OCHILTREE_PARAMS_NAME_REPL1 = {"k.acctno", "k.streetnum", "k.street"};
	private static String[] JIMWELLS_PARAMS_NAME_REPL2 = {"k.acct_num", "k.st_num", "k.st_name"};
	private static String[] OCHILTREE_PARAMS_NAME_REPL2 = {"k.acct_no", "k.situs_number", "k.situs_street"};
	private static String[] JIMWELLS_OCHILTREE_PARAMS_VALUE_REPL1 = {"i.detail", "i.themeFile","i.where"};
	private static String[] JIMWELLS_PARAMS_VALUE_REPL2 = {"jimwellsdetail.php?i_search_form_basket=&whereclause=&i_county_code=&theKey=", "jimwellstheme.php", " where 1=1"};
	private static String[] OCHILTREE_PARAMS_VALUE_REPL2 = {"ochiltreedetail.php?i_search_form_basket=&whereclause=&i_county_code=&theKey=", "ochiltreetheme.php", " where (1=1)"};
	private static String[] JIMWELLS_PARAMS_ADD = {"i.land_cd", "i.tdc03", "i.tdc04", "v.exm_cd"};
	
	private static String[] GRIMES_PARAMS_NAME_REPL1 = {"k.prptyp", "k.name30", "k.parid", "k.loc1a", "k.loc1b"};
	private static String[] GRIMES_PARAMS_NAME_REPL2 = {"j.QuickRefID", "k.OwnerName", "k.QuickRefID","k.Street_Number", "k.Street_Name"};
	private static String[] GRIMES_PARAMS_VALUE_REPL1 = {"i.themeFile","i.where"};
	private static String[] GRIMES_PARAMS_VALUE_REPL2 = {"cert_theme.php", " where ConfidentialOwner!='True' "};
	private static String[] GRIMES_PARAMS_ADD = {"i.Exemption_List", "i.OwnerState", "i.Taxing_Unit_List", "i.Taxing_Unit_List", "i.land_sptb", "i.ov65_exempt"};
	private static String[] GRIMES_PARAMS_REMOVE = {"i.dsn"};
	
	private static String[] HARRIS_TARRANT_DALLAS_PARAMS_NAME_REPL1 = {"j.QuickRefID", "k.OwnerName", "k.QuickRefID", "k.Street_Number", "k.Street_Name"};
	private static String[] HARRIS_PARAMS_NAME_REPL2 = {"i.prop_type_cd", "k.Owner_Name_1", "k.Acct_Num", "e.a_situs_no", "k.a_situs_street"};
	private static String[] TARRANT_PARAMS_NAME_REPL2 = {"i.prop_type_cd", "k.OwnerName", "k.Account_no", "e.situs_no", "k.situs_street"};
	private static String[] DALLAS_PARAMS_NAME_REPL2 = {"k.Division_Cd", "k.Owner_Name1", "k.Account_Num", "e.Street_Num", "w.Full_Street_Name"};
	private static String[] HARRIS_TARRANT_DALLAS_PARAMS_VALUE_REPL1 = {"i.where"};
	private static String[] HARRIS_PARAMS_VALUE_REPL2 = {" where 1=1 "};
	private static String[] TARRANT_PARAMS_VALUE_REPL2 = {" where ( 1 = 1 )"};
	private static String[] DALLAS_PARAMS_VALUE_REPL2 = {" where 1 = 1 AND Division_Cd <> 'BPP' "};
	private static String[] HARRIS_PARAMS_ADD = {"i.Econ_Bld_Class", "i.State_Class", "i.agent_id", "i.entities", "i.entities", "i.exemptionsHS", "i.exemptionsOA", 
		"i.imprv_style", "i.imprv_type", "i.mail_state"};
	private static String[] TARRANT_PARAMS_ADD = {"i.City", "i.STCD", "i.SWPool", "i.School", "i.XMPT", "i.appr_addr_state", "i.class"};
	private static String[] DALLAS_PARAMS_ADD = {"i.Bldg_Class_Desc", "i.Bldg_Class_Desc_com", "i.CDU_Rating_Desc", "i.City_Juris_Desc", "i.ISD_Juris_Desc", 
		"i.Owner_State", "i.Pool_Ind", "i.Property_Qual_Desc", "i.TaxPayer_Rep", "i.hs_exempt", "i.o65_exempt", "i.sptb"};
	private static String[] HARRIS_PARAMS_REMOVE = {"i.Exemption_List", "i.OwnerState", "i.Taxing_Unit_List", "i.detail", "i.labels", "i.land_sptb", "i.ov65_exempt"};
	private static String[] TARRANT_PARAMS_REMOVE = {"i.Exemption_List", "i.OwnerState", "i.Taxing_Unit_List", "i.dbType", "i.detail", "i.labels", "i.land_sptb", "i.ov65_exempt"};
	private static String[] DALLAS_PARAMS_REMOVE = {"i.detail", "i.labels", "i.Exemption_List", "i.OwnerState", "i.Taxing_Unit_List", "i.land_sptb", "i.ov65_exempt"};
	
	public LoginResponse onLogin(DataSite dataSite) {
		
		setDataSite(dataSite);
		
		String cnty_name = getDataSite().getCountyName().replace(" ", "").toLowerCase();
		HTTPRequest req1 = new HTTPRequest("http://www.taxnetusa.com/texas/" + cnty_name + "/");
		String resp1 = execute(req1);
		if (resp1.contains("Sign Out")) {	//already logged in
			return LoginResponse.getDefaultSuccessResponse();
		}
		
		HTTPRequest req = new HTTPRequest("http://www.taxnetusa.com/login.php");
		
		try {
						
			String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TXGenericTaxNetAO", "user");
			String pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TXGenericTaxNetAO", "password");
			if(StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't get credentials from database");
			}
			
			String htmlResult = execute(req);
			if(!htmlResult.contains("TaxNetUSA - Sign In")) {
				logger.error("Cannot get login page for site " + getDataSite().getName() + " and link http://www.taxnetusa.com/login.php");
			}
			
			req = new HTTPRequest("http://www.taxnetusa.com/check_password.php", HTTPRequest.POST);
			req.setPostParameter("is-login-form", "");
			req.setPostParameter("redirectURL", "http://www.taxnetusa.com/texas/" + cnty_name + "/");
			req.setPostParameter("theUserName", user);
			req.setPostParameter("thePassword", pass);
			
			htmlResult = execute(req);
			
			if(htmlResult.contains("Invalid Login")) {
				return LoginResponse.getDefaultInvalidCredentialsResponse();
			}
			
		} catch (Exception e) {
			logger.error("Error logging in for site " + getDataSite().getName(), e);
			return LoginResponse.getDefaultFailureResponse();
		}
		//indicates success
		return LoginResponse.getDefaultSuccessResponse();
	}
	
	public void onBeforeRequestExcl(HTTPRequest req, DataSite dataSite) {
		
		setDataSite(dataSite);
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			String cnty_name = getDataSite().getCountyName().replace(" ", "").toLowerCase();

			String url = req.getURL();
			if (url.equals("http://www.taxnetusa.com/texas/")) {
				
				url += cnty_name + "/list.php";
				req.setURL(url);
				
				String countyId = getDataSite().getCountyIdAsString();
				
				replaceInParamValue(req, "i.detail", "(?is)&amp;", "&");
				replaceInParamValue(req, "i.where", "(?is)&apos;", "'");
				
				if (COLEMAN_1_LIST.contains(countyId)) {
					replaceParamNames(req, COLEMAN_PARAMS_NAME_REPL1, COLEMAN_PARAMS_NAME_REPL2);
					replaceParamValues(req, COLEMAN_PARAMS_VALUE_REPL1, COLEMAN_PARAMS_VALUE_REPL2);
					addEmptyParams(req, COLEMAN_PARAMS_ADD);
				} else if (ARMSTRONG_61_LIST.contains(countyId)) {
					replaceParamNames(req, ARMSTRONG_COCHRAN_ANDERSON_PARAMS_NAME_REPL1, ARMSTRONG_PARAMS_NAME_REPL2);
					replaceParamValues(req, ARMSTRONG_COCHRAN_ANDERSON_PARAMS_VALUE_REPL1, ARMSTRONG_PARAMS_VALUE_REPL2);
					addEmptyParams(req, ARMSTRONG_PARAMS_ADD);
					removeParams(req, ARMSTRONG_PARAMS_REMOVE);
				} else if (COCHRAN_10_LIST.contains(countyId)) {
					replaceParamNames(req, ARMSTRONG_COCHRAN_ANDERSON_PARAMS_NAME_REPL1, COCHRAN_PARAMS_NAME_REPL2);
					replaceParamValues(req, ARMSTRONG_COCHRAN_ANDERSON_PARAMS_VALUE_REPL1, COCHRAN_PARAMS_VALUE_REPL2);
					addEmptyParams(req, COCHRAN_PARAMS_ADD);
					removeParams(req, COCHRAN_PARAMS_REMOVE);
				} else if (ANDERSON_5_LIST.contains(countyId)) {
					replaceParamNames(req, ARMSTRONG_COCHRAN_ANDERSON_PARAMS_NAME_REPL1, ANDERSON_PARAMS_NAME_REPL2);
					replaceParamValues(req, ARMSTRONG_COCHRAN_ANDERSON_PARAMS_VALUE_REPL1, ANDERSON_PARAMS_VALUE_REPL2);
					addEmptyParams(req, ANDERSON_PARAMS_ADD);
					removeParams(req, ANDERSON_PARAMS_REMOVE);
					req.setPostParameter("i.themeFile", "theme.php");
				} else if (BOWIE_1_LIST.contains(countyId)) {
					replaceParamNames(req, BOWIE_AUSTIN_PARAMS_NAME_REPL1, BOWIE_PARAMS_NAME_REPL2);
					replaceParamValues(req, BOWIE_AUSTIN_PARAMS_VALUE_REPL1, BOWIE_PARAMS_VALUE_REPL2);
					addEmptyParams(req, BOWIE_PARAMS_ADD);
					removeParams(req, BOWIE_PARAMS_REMOVE);
				} else if (AUSTIN_3_LIST.contains(countyId)) {
					replaceParamNames(req, BOWIE_AUSTIN_PARAMS_NAME_REPL1, AUSTIN_PARAMS_NAME_REPL2);
					replaceParamValues(req, BOWIE_AUSTIN_PARAMS_VALUE_REPL1, AUSTIN_PARAMS_VALUE_REPL2);
					addEmptyParams(req, AUSTIN_PARAMS_ADD);
					removeParams(req, AUSTIN_PARAMS_REMOVE);
					req.setPostParameter("i.themeFile", "theme.php");
				} else if (JIM_WELLS_1_LIST.contains(countyId)) {
					replaceParamNames(req, JIMWELLS_OCHILTREE_PARAMS_NAME_REPL1, JIMWELLS_PARAMS_NAME_REPL2);
					replaceParamValues(req, JIMWELLS_OCHILTREE_PARAMS_VALUE_REPL1, JIMWELLS_PARAMS_VALUE_REPL2);
					addEmptyParams(req, JIMWELLS_PARAMS_ADD);
					req.setPostParameter("i.labels", "Account Number,PIDN,Owner Name, Address, Value");
				} else if (OCHILTREE_1_LIST.contains(countyId)) {
					replaceParamNames(req, JIMWELLS_OCHILTREE_PARAMS_NAME_REPL1, OCHILTREE_PARAMS_NAME_REPL2);
					replaceParamValues(req, JIMWELLS_OCHILTREE_PARAMS_VALUE_REPL1, OCHILTREE_PARAMS_VALUE_REPL2);
				} else if (GRIMES_3_LIST.contains(countyId)) {
					replaceParamNames(req, GRIMES_PARAMS_NAME_REPL1, GRIMES_PARAMS_NAME_REPL2);
					replaceParamValues(req, GRIMES_PARAMS_VALUE_REPL1, GRIMES_PARAMS_VALUE_REPL2);
					addEmptyParams(req, GRIMES_PARAMS_ADD);
					removeParams(req, GRIMES_PARAMS_REMOVE);
					req.setPostParameter("i.labels", "PIDN,Account Number,Owner Name, Address, Value");
				} else if (POTTER_2_LIST.contains(countyId)) {
					req.setPostParameter("i.themeFile", "theme.php");
				} else if (HARRIS_1_LIST.contains(countyId)) {
					replaceParamNames(req, HARRIS_TARRANT_DALLAS_PARAMS_NAME_REPL1, HARRIS_PARAMS_NAME_REPL2);
					replaceParamValues(req, HARRIS_TARRANT_DALLAS_PARAMS_VALUE_REPL1, HARRIS_PARAMS_VALUE_REPL2);
					addEmptyParams(req, HARRIS_PARAMS_ADD);
					removeParams(req, HARRIS_PARAMS_REMOVE);
				} else if (TARRANT_1_LIST.contains(countyId)) {
					replaceParamNames(req, HARRIS_TARRANT_DALLAS_PARAMS_NAME_REPL1, TARRANT_PARAMS_NAME_REPL2);
					replaceParamValues(req, HARRIS_TARRANT_DALLAS_PARAMS_VALUE_REPL1, TARRANT_PARAMS_VALUE_REPL2);
					addEmptyParams(req, TARRANT_PARAMS_ADD);
					removeParams(req, TARRANT_PARAMS_REMOVE);
				} else if (DALLAS_1_LIST.contains(countyId)) {
					replaceParamNames(req, HARRIS_TARRANT_DALLAS_PARAMS_NAME_REPL1, DALLAS_PARAMS_NAME_REPL2);
					replaceParamValues(req, HARRIS_TARRANT_DALLAS_PARAMS_VALUE_REPL1, DALLAS_PARAMS_VALUE_REPL2);
					addEmptyParams(req, DALLAS_PARAMS_ADD);
					removeParams(req, DALLAS_PARAMS_REMOVE);
					req.setPostParameter("i.detail", "dallasdetail.php?i_search_form_basket=&whereclause=&i_county_code=&theKey=");
					req.setPostParameter("i.labels", "Account Number,Owner Name, Address, Value");
					req.setPostParameter("i.dsn", "tax");
					req.setPostParameter("i.themeFile", "cert_theme.php");
				}
				
			}
			
		}

	}
	
	public static void replaceInParamValue(HTTPRequest req, String param, String s1, String s2) {
		String oldValue = req.getPostFirstParameter(param);
		if (StringUtils.isNotEmpty(oldValue)) {
			String value = oldValue.replaceAll(s1, s2);
			if (!value.equals(oldValue)) {
				req.removePostParameters(param);
				req.setPostParameter(param, value);
			}
		}
	}
	
	public static void replaceParamNames(HTTPRequest req, String[] names1, String[] names2) {
		if (names1!=null && names2!=null) {
			int l1 = names1.length;
			int l2 = names2.length;
			if (l1>0 && l2>0 && l1==l2) {
				for (int i=0;i<l1;i++) {
					String value = req.getPostFirstParameter(names1[i]);
					req.removePostParameters(names1[i]);
					req.setPostParameter(names2[i], value);
				}
			}
		}
	}
	
	public static void replaceParamValues(HTTPRequest req, String[] names, String[] values) {
		if (names!=null && values!=null) {
			int l1 = names.length;
			int l2 = values.length;
			if (l1>0 && l2>0 && l1==l2) {
				for (int i=0;i<l1;i++) {
					req.removePostParameters(names[i]);
					req.setPostParameter(names[i], values[i]);
				}
			}
		}
	}
	
	public static void addEmptyParams(HTTPRequest req, String[] names) {
		if (names!=null) {
			for (String s: names) {
				req.setPostParameter(s, "");
			}
		}
	}
	
	public static void removeParams(HTTPRequest req, String[] names) {
		if (names!=null) {
			for (String s: names) {
				req.removePostParameters(s);
			}
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		if (req.getMethod()==HTTPRequest.POST && req.getURL().endsWith("/list.php")) {
			if (res.returnCode==500) {
				res.is = IOUtils.toInputStream("<html>Server error!</html>");
					res.body = "<html>Server error!</html>";
					res.contentLenght = res.body.length();
					res.returnCode = 200;
					return;
			}
		}
		
	}
	
}
