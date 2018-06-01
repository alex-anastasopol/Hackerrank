package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ACCOUNT_NO_KEY;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ACCOUNT_NO_PATTERN;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ACCOUNT_NUMBER_KEY;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ACCOUNT_NUMBER_PATTERN;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ANDERSON_11_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ARANSAS_15_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ARCHER_19_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ARMSTRONG_58_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ATASCOSA_121_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.BAILEY_2_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.BOWIE_12_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.BRISCOE_5_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.DALLAS_3_LIST;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.LONG_ACCOUNT_NUMBER_KEY;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.LONG_ACCOUNT_NUMBER_PATTERN;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.PIDN_KEY;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.PIDN_PATTERN;
import static ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.POTTER_5_LIST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

/**
 * used for counties which have both PIDN and (Long) Account Number
 * 
 * @author mihaib
 * 
 * @author Oprina George
 * 
 *         Jun 8, 2011
 */

public class TXGenericTaxNetAO extends TSServer {

	// used for atascosa, armstrong, bowie, anderson
	// -> see ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
	// ATASCOSA_120, ARMSTRONG_55, BOWIE_12
	
	public static String[] PROPERTY_TYPE_1 = { CountyConstants.TX_Archer_STRING, CountyConstants.TX_Armstrong_STRING,
		CountyConstants.TX_Baylor_STRING, CountyConstants.TX_Bee_STRING, CountyConstants.TX_Borden_STRING,
		CountyConstants.TX_Burleson_STRING, CountyConstants.TX_Carson_STRING, CountyConstants.TX_Castro_STRING,
		CountyConstants.TX_Chambers_STRING, CountyConstants.TX_Childress_STRING, CountyConstants.TX_Clay_STRING,
		CountyConstants.TX_Coke_STRING, CountyConstants.TX_Collingsworth_STRING, CountyConstants.TX_Concho_STRING,
		CountyConstants.TX_Cottle_STRING, CountyConstants.TX_Crane_STRING, CountyConstants.TX_Crockett_STRING,
		CountyConstants.TX_Culberson_STRING, CountyConstants.TX_Dawson_STRING, CountyConstants.TX_DeWitt_STRING,
		CountyConstants.TX_Dickens_STRING, CountyConstants.TX_Donley_STRING, CountyConstants.TX_Eastland_STRING,
		CountyConstants.TX_Fisher_STRING, CountyConstants.TX_Foard_STRING, CountyConstants.TX_Franklin_STRING,
		CountyConstants.TX_Freestone_STRING, CountyConstants.TX_Frio_STRING, CountyConstants.TX_Goliad_STRING,
		CountyConstants.TX_Gonzales_STRING, CountyConstants.TX_Hall_STRING, CountyConstants.TX_Hansford_STRING,
		CountyConstants.TX_Hardeman_STRING, CountyConstants.TX_Hardin_STRING, CountyConstants.TX_Hemphill_STRING,
		CountyConstants.TX_Houston_STRING, CountyConstants.TX_Hutchinson_STRING, CountyConstants.TX_Irion_STRING,
		CountyConstants.TX_Jack_STRING, CountyConstants.TX_Jeff_Davis_STRING, CountyConstants.TX_Jim_Hogg_STRING, 
		CountyConstants.TX_Jones_STRING, CountyConstants.TX_Karnes_STRING, CountyConstants.TX_Kent_STRING, 
		CountyConstants.TX_King_STRING, CountyConstants.TX_Lampasas_STRING, CountyConstants.TX_Leon_STRING, 
		CountyConstants.TX_Loving_STRING, CountyConstants.TX_Lynn_STRING, CountyConstants.TX_Marion_STRING, 
		CountyConstants.TX_Martin_STRING, CountyConstants.TX_Mason_STRING, CountyConstants.TX_McCulloch_STRING, 
		CountyConstants.TX_Menard_STRING,	CountyConstants.TX_Mitchell_STRING, CountyConstants.TX_Nacogdoches_STRING, 
		CountyConstants.TX_Nolan_STRING, CountyConstants.TX_Panola_STRING, CountyConstants.TX_Pecos_STRING, 
		CountyConstants.TX_Presidio_STRING, CountyConstants.TX_Reagan_STRING, CountyConstants.TX_Refugio_STRING,
		CountyConstants.TX_Rusk_STRING, CountyConstants.TX_Sabine_STRING, CountyConstants.TX_San_Augustine_STRING,
		CountyConstants.TX_San_Patricio_STRING, CountyConstants.TX_San_Saba_STRING, CountyConstants.TX_Sherman_STRING,
		CountyConstants.TX_Starr_STRING, CountyConstants.TX_Sterling_STRING, CountyConstants.TX_Stonewall_STRING,
		CountyConstants.TX_Throckmorton_STRING, CountyConstants.TX_Ward_STRING, CountyConstants.TX_Wheeler_STRING,
		CountyConstants.TX_Wilbarger_STRING, CountyConstants.TX_Young_STRING};
	public static List<String> PROPERTY_TYPE_1_LIST	= Arrays.asList(PROPERTY_TYPE_1);
	
	public static String[] PROPERTY_TYPE_2 = {CountyConstants.TX_Aransas_STRING, CountyConstants.TX_Bastrop_STRING,
		CountyConstants.TX_Fort_Bend_STRING, CountyConstants.TX_Galveston_STRING, CountyConstants.TX_Grimes_STRING,
		CountyConstants.TX_Jackson_STRING, CountyConstants.TX_Limestone_STRING,
		CountyConstants.TX_Lubbock_STRING, CountyConstants.TX_Medina_STRING, CountyConstants.TX_Montgomery_STRING,
		CountyConstants.TX_Orange_STRING, CountyConstants.TX_Washington_STRING, CountyConstants.TX_Williamson_STRING};
	public static List<String> PROPERTY_TYPE_2_LIST	= Arrays.asList(PROPERTY_TYPE_2);
	
	public static String[] PROPERTY_TYPE_3 = {CountyConstants.TX_Bowie_STRING, CountyConstants.TX_Cochran_STRING,
		CountyConstants.TX_Crosby_STRING, CountyConstants.TX_Garza_STRING, CountyConstants.TX_Glasscock_STRING,
		CountyConstants.TX_Howard_STRING, CountyConstants.TX_Jasper_STRING, CountyConstants.TX_Live_Oak_STRING,
		CountyConstants.TX_Red_River_STRING, CountyConstants.TX_Robertson_STRING, CountyConstants.TX_Terrell_STRING
		, CountyConstants.TX_Travis_STRING
		, CountyConstants.TX_Burnet_STRING
		, CountyConstants.TX_Hays_STRING
	};
	public static List<String> PROPERTY_TYPE_3_LIST	= Arrays.asList(PROPERTY_TYPE_3);
	
	public static String[] PROPERTY_TYPE_4 = {CountyConstants.TX_Anderson_STRING, CountyConstants.TX_Austin_STRING,
		CountyConstants.TX_Bosque_STRING, CountyConstants.TX_Ector_STRING, CountyConstants.TX_Johnson_STRING,
		CountyConstants.TX_Smith_STRING, CountyConstants.TX_Wharton_STRING};
	public static List<String> PROPERTY_TYPE_4_LIST	= Arrays.asList(PROPERTY_TYPE_4);
	
	public static List<String>		PROPERTY_TYPE_5_LIST	= new ArrayList<>();

	public static List<String>		PROPERTY_TYPE_6_LIST	= new ArrayList<>();

	public static List<String>		PROPERTY_TYPE_7_LIST	= new ArrayList<>();
	
	public static String[] DATA_TO_SEARCH_1 = { CountyConstants.TX_Austin_STRING, CountyConstants.TX_Bastrop_STRING, 
		CountyConstants.TX_Bowie_STRING, CountyConstants.TX_Denton_STRING, CountyConstants.TX_El_Paso_STRING, 
		CountyConstants.TX_Fort_Bend_STRING, CountyConstants.TX_Galveston_STRING, CountyConstants.TX_Harris_STRING, 
		CountyConstants.TX_Harrison_STRING, CountyConstants.TX_Henderson_STRING, CountyConstants.TX_Hood_STRING, 
		CountyConstants.TX_Jefferson_STRING, CountyConstants.TX_Johnson_STRING, CountyConstants.TX_Kendall_STRING, 
		CountyConstants.TX_Kerr_STRING, CountyConstants.TX_Medina_STRING, CountyConstants.TX_Midland_STRING, 
		CountyConstants.TX_Orange_STRING, CountyConstants.TX_Parker_STRING, CountyConstants.TX_Potter_STRING, 
		CountyConstants.TX_Randall_STRING, CountyConstants.TX_San_Jacinto_STRING, CountyConstants.TX_Smith_STRING, 
		CountyConstants.TX_Tarrant_STRING, CountyConstants.TX_Tom_Green_STRING, 
		CountyConstants.TX_Williamson_STRING, CountyConstants.TX_Wise_STRING};
	public static List<String> DATA_TO_SEARCH_1_LIST	= Arrays.asList(DATA_TO_SEARCH_1);
	
	public static Set<String>		DATA_TO_SEARCH_2_LIST	= new HashSet();
	
	
	static {
		PROPERTY_TYPE_5_LIST.add(CountyConstants.TX_Harris_STRING);
		PROPERTY_TYPE_5_LIST.add(CountyConstants.TX_Tarrant_STRING);
		
		PROPERTY_TYPE_6_LIST.add(CountyConstants.TX_Dallas_STRING);
		
		PROPERTY_TYPE_7_LIST.add(CountyConstants.TX_Llano_STRING);
		
		DATA_TO_SEARCH_2_LIST.add(CountyConstants.TX_Hays_STRING);
	}
	
	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave;
	private static final Pattern PREV_PAT = Pattern
			.compile("(?is)<a\\s+class\\s*=\\s*\\\"\\s*results_bold\\s*\\\"\\s*href\\s*=\\s*\\\"([^\\\"]+)\\\">\\s*Prev");
	private static final Pattern NEXT_PAT = Pattern
			.compile("(?is)<a\\s+class\\s*=\\s*\\\"\\s*results_bold\\s*\\\"\\s*href\\s*=\\s*\\\"([^\\\"]+)\\\">\\s*Next");

	private static final Pattern dummyPattern = Pattern
			.compile("&dummy=([0-9]+)&");

	private String county = "";

	public TXGenericTaxNetAO(long searchId) {
		super(searchId);
		county = getDataSite().getCountyName().replace(" ", "").toLowerCase();
	}

	public TXGenericTaxNetAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
		county = getDataSite().getCountyName().replace(" ", "").toLowerCase();
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)
			throws ServerResponseException {

		return super.SearchBy(module, sd);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.NAME_MODULE_IDX);

		String county = dataSite.getCountyName();
		String countyId = getDataSite().getCountyIdAsString();

		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		String siteName = StateCountyManager.getInstance().getSTCounty(
				dataSite.getCountyId())
				+ dataSite.getSiteType();

		if (tsServerInfoModule != null) {
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();

			boolean atascosa = ATASCOSA_121_LIST.contains(county.toLowerCase());

			boolean armstrong = ARMSTRONG_58_LIST.contains(county.toLowerCase());

			boolean armstrong61 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.ARMSTRONG_61_LIST.contains(countyId);
			
			boolean cochran10 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.COCHRAN_10_LIST.contains(countyId);
			
			boolean anderson5 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.ANDERSON_5_LIST.contains(countyId);
			
			boolean bowie1 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.BOWIE_1_LIST.contains(countyId);
			
			boolean austin3 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.AUSTIN_3_LIST.contains(countyId);
			
			boolean dallas1 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.DALLAS_1_LIST.contains(countyId);
			
			boolean propertyType1 = PROPERTY_TYPE_1_LIST.contains(countyId);
			
			boolean propertyType2 = PROPERTY_TYPE_2_LIST.contains(countyId);
			
			boolean propertyType3 = PROPERTY_TYPE_3_LIST.contains(countyId);
			
			boolean propertyType4 = PROPERTY_TYPE_4_LIST.contains(countyId);
			
			boolean propertyType5 = PROPERTY_TYPE_5_LIST.contains(countyId);
			
			boolean propertyType6 = PROPERTY_TYPE_6_LIST.contains(countyId);
			
			boolean propertyType7 = PROPERTY_TYPE_7_LIST.contains(countyId);
			
			boolean dataToSearch1 = DATA_TO_SEARCH_1_LIST.contains(countyId);
			
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);

			}

			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc()
						.getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager
							.getCommentForSiteAndFunction(siteName,
									TSServerInfo.NAME_MODULE_IDX,
									nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
				
				try {
					if (armstrong61 || anderson5) {
						if ("Property Type".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 1, 1);
						} else if ("Owner Name".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 2, 2);
						}
					} else if (cochran10) {
						if ("Property Type".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 1, 1);
						} else if ("Owner Name".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 2, 2);
						} else if ("PIDN".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 3, 3);
						} else if ("Long Account Number".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 4, 4);
						}
					} else if (bowie1) {
						if ("Property Type".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 2, 2);
						} else if ("Owner Name".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 3, 3);
						} else if ("PIDN".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 4, 4);
						} else if ("Long Account Number".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 5, 5);
						}
					} else if (austin3) {
						if ("Property Type".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 2, 2);
						} else if ("Owner Name".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 3, 3);
						}
					} else if (dallas1) {
						if ("Property Type".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 1, 1);
						} else if ("Data to Search".equals(functionName)) {
							htmlControl.setColAndRow(1, 1, 2, 2);
						}
					}
				} catch (FormatException e) {
					e.printStackTrace();
				}
				
				if (propertyType1) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Property Type".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select id=\"k.prptyp\" name=\"k.prptyp\">" + 
							"<option value=\"\">All Types</option><option value=\"R\">Real</option><option value=\"P\">Business Personal</option></select>");
					}
				} else if (propertyType2) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Property Type".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select id=\"j.QuickRefID\" name=\"j.QuickRefID\">" + 
							"<option value=\"\">All Types</option><option value=\"R\">Real</option><option value=\"P\">Business Personal</option><option value=\"N\">Minerals" + 
							"</option><option value=\"M\">Mobile Homes</option></select>");
					}
				} else if (propertyType3) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Property Type".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select id=\"n.prop_type\" name=\"n.prop_type\">" + 
							"<option value=\"\">All Types</option><option value=\"REAL\">Real</option><option value=\"PERS,INDS\">Business Personal</option><option value=\"MINR\">Minerals</option></select>");
					}
				} else if (propertyType4) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Property Type".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select name=\"k.aay_division_cdx\" name=\"k.aay_division_cdx\">" + 
							"<option value=\"\">All Types</option><option value=\"R\">Real</option><option value=\"B\">Business Personal</option></select>");
					}
				} else if (propertyType5) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Property Type".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select name=\"i.prop_type_cd\" id=\"i.prop_type_cd\">" + 
							"<option value=\"R\" selected=\"selected\">Real</option><option value=\"P\">Business Personal</option></select>");
						htmlControl.getCurrentTSSiFunc().setDefaultValue("R");
					}
				} else if (propertyType6) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Property Type".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select name=\"k.Division_Cd\" id=\"k.Division_Cd\">" + 
							"<option value=\"\">All Real</option><option value=\"res\">Real Residential</option><option value=\"com\">Real Commercial</option>" + 
							"<option value=\"bpp\">Business Personal</option></select>");
					}
				} else if (propertyType7) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Property Type".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select name=\"i.prop_type_cd\" id=\"i.prop_type_cd\">" + 
							"<option value=\"R\" selected=\"selected\">Real</option><option value=\"P\">Business Personal</option><option value=\"MM\">Minerals</option></select>");
						htmlControl.getCurrentTSSiFunc().setDefaultValue("R");
					}
				}
				
				if (dataToSearch1) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Data to Search".equals(functionName)) {
						htmlControl.getCurrentTSSiFunc().setHtmlformat("<select name=\"i.themeFile\" id=\"i.themeFile\">" + 
							"<option value=\"cert_theme.php\">2012 Certified</option><option value=\"prelim_theme.php\" selected=\"selected\">2013 Preliminary</option></select>");
					}
				} else if(DATA_TO_SEARCH_2_LIST.contains(countyId)) {
					if (HTMLControl.HTML_SELECT_BOX==htmlControl.getControlType() && "Data to Search".equals(functionName)) {
						htmlControl.setHiddenParam(true);
						htmlControl.getCurrentTSSiFunc().setDefaultValue("cert_theme.php");
					}
				}
				
				if ("Owner Name".equals(functionName)) {
					if ("Brooks".equals(county) || "Somervell".equals(county)) {
						htmlControl.setFieldNote("(e.g. Smith Jack)");
					} else if ("Brown".equals(county) || "Coleman".equals(county) || "Bowie".equals(county)) {
						htmlControl.setFieldNote("(e.g. Smith, John)");
					} else if ("Hartley".equals(county) || "Lipscomb".equals(county)) {
						htmlControl.setFieldNote("(e.g. Smith James)");
					} else {
						htmlControl.setFieldNote("(e.g. Smith John)");
					}
				}

				if (armstrong) {
					if ("GEO Account".equals(functionName)) {
						htmlControl.setLabel("Account No.");
						if(county.equals("Gonzales") || county.equals("Coke")) {
							htmlControl.setLabel("GEO Number");	
						}
						if ("Armstrong".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 03900-01900-00100-000000)");
						} else if ("Bee".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 58150-01020-07000-000000)");
						} else if ("Borden".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 05000-00076-00001-000001)");
						} else if ("Burleson".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 2519-000-001-24400)");
						} else if ("Carson".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 02250-00210-00000-000000)");
						} else if ("Chambers".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 67000-00014-00700-000400)");
						} else if ("Childress".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 11000-01327-00000-000000)");
						} else if ("Concho".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1028000)");
						} else if ("Cottle".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 02060-00009-00005-000000)");
						} else if ("Culberson".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 01380-00360-00000-000000)");
						} else if ("Dawson".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 10114-02050-00000-000000)");
						} else if ("DeWitt".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 48050-00002-00060-000000)");
						} else if ("Dickens".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 200-0033-0006-000000)");
						} else if ("Donley".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 11-01-0300-0010-0002)");
						} else if ("Falls".equals(county)) {
							htmlControl.setFieldNote("(e.g. 11324001 )");
						} else if ("Foard".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00637-00000-00000-001769)");
						} else if ("Franklin".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00070-00000-00032-000000)");
						} else if ("Freestone".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 35013-00004-00000-000000)");
						} else if ("Frio".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00164-00105-00410-000000)");
						} else if ("Goliad".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 1001-053095-034038 )");
						} else if ("Hall".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 11140-00020-00001-008102)");
						}

					}
					if ("PIDN".equals(functionName)) {
						if ("Armstrong".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3478)");
						} else if ("Bee".equals(county)) {
							htmlControl.setFieldNote("(e.g. 5431)");
						} else if ("Borden".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3803)");
						} else if ("Burleson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 27260)");
						} else if ("Carson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 5697)");
						} else if ("Chambers".equals(county)) {
							htmlControl.setFieldNote("(e.g. 48087)");
						} else if ("Childress".equals(county)) {
							htmlControl.setFieldNote("(e.g. 995)");
						} else if ("Concho".equals(county)) {
							htmlControl.setFieldNote("(e.g. 565)");
						} else if ("Cottle".equals(county)) {
							htmlControl.setFieldNote("(e.g. 5362)");
						} else if ("Culberson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 551)");
						} else if ("Dawson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 5633)");
						} else if ("DeWitt".equals(county)) {
							htmlControl.setFieldNote("(e.g. 19876)");
						} else if ("Dickens".equals(county)) {
							htmlControl.setFieldNote("(e.g. 4896)");
						} else if ("Donley".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3874)");
						} else if ("Falls".equals(county)) {
							htmlControl.setFieldNote("(e.g. 12345 )");
						} else if ("Foard".equals(county)) {
							htmlControl.setFieldNote("(e.g. 5419)");
						} else if ("Franklin".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1337)");
						} else if ("Freestone".equals(county)) {
							htmlControl.setFieldNote("(e.g. 22384)");
						} else if ("Frio".equals(county)) {
							htmlControl.setFieldNote("(e.g. 14997)");
						} else if ("Goliad".equals(county)) {
							htmlControl.setFieldNote("(e.g. 902 )");
						} else if ("Hall".equals(county)) {
							htmlControl.setFieldNote("(e.g. 204560)");
						}
					}
					if ("Street No.".equals(functionName)) {
						if ("Armstrong".equals(county)) {
							htmlControl.setFieldNote("(e.g. 301)");
						} else if ("Bee".equals(county)) {
							htmlControl.setFieldNote("(e.g. 809 )");
						} else if ("Borden".equals(county)) {
							htmlControl.setFieldNote("(e.g. 608 )");
						} else if ("Burleson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 606 )");
						} else if ("Carson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 405 )");
						} else if ("Chambers".equals(county)) {
							htmlControl.setFieldNote("(e.g. 415)");
						} else if ("Childress".equals(county)) {
							htmlControl.setFieldNote("(e.g. 906 )");
						} else if ("Concho".equals(county)) {
							htmlControl.setFieldNote("(e.g. 802)");
						} else if ("Cottle".equals(county)) {
							htmlControl.setFieldNote("(e.g. 214)");
						} else if ("Culberson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1604)");
						} else if ("Dawson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 501)");
						} else if ("DeWitt".equals(county)) {
							htmlControl.setFieldNote("(e.g. 609)");
						} else if ("Dickens".equals(county)) {
							htmlControl.setFieldNote("(e.g. 520)");
						} else if ("Donley".equals(county)) {
							htmlControl.setFieldNote("(e.g. 210)");
						} else if ("Falls".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1007 )");
						} else if ("Foard".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Franklin".equals(county)) {
							htmlControl.setFieldNote("(e.g. 103 )");
						} else if ("Freestone".equals(county)) {
							htmlControl.setFieldNote("(e.g. 304)");
						} else if ("Frio".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1020)");
						} else if ("Goliad".equals(county)) {
							htmlControl.setFieldNote("(e.g. 319 )");
						} else if ("Hall".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1103)");
						}
					}
					if ("Street Name".equals(functionName)) {
						if ("Armstrong".equals(county)) {
							htmlControl.setFieldNote("(e.g. COLLINS)");
						} else if ("Bee".equals(county)) {
							htmlControl.setFieldNote("(e.g. RANDALL)");
						} else if ("Borden".equals(county)) {
							htmlControl.setFieldNote("(e.g. LOVE)");
						} else if ("Burleson".equals(county)) {
							htmlControl.setFieldNote("(e.g. CREEK )");
						} else if ("Carson".equals(county)) {
							htmlControl.setFieldNote("(e.g. GRIMES)");
						} else if ("Chambers".equals(county)) {
							htmlControl.setFieldNote("(e.g. MAGNOLIA)");
						} else if ("Childress".equals(county)) {
							htmlControl.setFieldNote("(e.g. HILLCREST)");
						} else if ("Concho".equals(county)) {
							htmlControl.setFieldNote("(e.g. BROADWAY)");
						} else if ("Cottle".equals(county)) {
							htmlControl.setFieldNote("(e.g. LAWRENCE AVE)");
						} else if ("Culberson".equals(county)) {
							htmlControl.setFieldNote("(e.g. BROADWAY)");
						} else if ("Dawson".equals(county)) {
							htmlControl.setFieldNote("(e.g. DALLAS)");
						} else if ("DeWitt".equals(county)) {
							htmlControl.setFieldNote("(e.g. THOMAS )");
						} else if ("Dickens".equals(county)) {
							htmlControl.setFieldNote("(e.g. CALVERT)");
						} else if ("Donley".equals(county)) {
							htmlControl.setFieldNote("(e.g. JEFFERSON)");
						} else if ("Falls".equals(county)) {
							htmlControl.setFieldNote("(e.g. COLEMAN )");
						} else if ("Foard".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Franklin".equals(county)) {
							htmlControl.setFieldNote("(e.g. HUNNICUTT)");
						} else if ("Freestone".equals(county)) {
							htmlControl.setFieldNote("(e.g. PEASE)");
						} else if ("Frio".equals(county)) {
							htmlControl.setFieldNote("(e.g. LEONA)");
						} else if ("Goliad".equals(county)) {
							htmlControl.setFieldNote("(e.g. LAKESHORE DR)");
						} else if ("Hall".equals(county)) {
							htmlControl.setFieldNote("(e.g. ROBERTSON)");
						}
					}
				}

				if (atascosa) {
					if ("Owner Name".equals(functionName)) {
						if ("Kenedy".equals(county)) {
							htmlControl.setFieldNote("(e.g.	David Salazar)");
						}
					}
					if ("Long Account Number".equals(functionName)) {
						if ("Andrews".equals(county)) {
							htmlControl.setFieldNote("(e.g. 01750-00030-0000)");
						} else if ("Angelina".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 0010-043-038-001-00)");
						} else if ("Bandera".equals(county)) {
							htmlControl.setFieldNote("(e.g.	30040-01450-0006)");
						} else if ("Bell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	0543990201)");
						} else if ("Bexar".equals(county)) {
							htmlControl.setFieldNote("(e.g.	14923-008-0030)");
						} else if ("Blanco".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	26870000001701001)");
						} else if ("Brazoria".equals(county)) {
							htmlControl.setFieldNote("(e.g.	8181-0105-000)");
						} else if ("Brazoria".equals(county)) {
							htmlControl.setFieldNote("(e.g.	150500-0103-0080)");
						} else if ("Brooks".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	00097-0000-449-02)");
						} else if ("Brown".equals(county)) {
							htmlControl.setFieldNote("(e.g.	R7841-0149-00)");
						} else if ("Burnet".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	05220-K070-07022-000)");
						} else if ("Caldwell".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	0003550-001-003-00)");
						} else if ("Calhoun".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	S0265-00070-0016-00)");
						} else if ("Callahan".equals(county)) {
							htmlControl.setFieldNote("(e.g.	6621000300900)");
						} else if ("Cameron".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	74-7970-0130-0101-00)");
						} else if ("Camp".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 13000-00900-00100-000009)");
						} else if ("Cherokee".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	000041-34140-0009000)");
						} else if ("Collin".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	R-0478-003-0200-1)");
						} else if ("Colorado".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1062008701100)");
						} else if ("Comal".equals(county)) {
							htmlControl.setFieldNote("(e.g.	130335335500)");
						} else if ("Comanche".equals(county)) {
							htmlControl.setFieldNote("(e.g.	CCO-05-0238)");
						} else if ("Coryell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	001910020)");
						} else if ("Dallam".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 121200301840010000000)");
						} else if ("Deaf Smith".equals(county)) {
							htmlControl.setFieldNote("(e.g.	SWEWH-7-68-69)");
						} else if ("Delta".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	2070-0004-0001-01)");
						} else if ("Denton".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. SC0002A-000001-0000-0090-0000)");
						} else if ("Dimmit".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 9100000-1450020000)");
						} else if ("Duval".equals(county)) {
							htmlControl.setFieldNote("(e.g. 110260-111-0050)");
						} else if ("Edwards".equals(county)) {
							htmlControl.setFieldNote("(e.g. 0718E-06-00000)");
						} else if ("El Paso".equals(county)) {
							htmlControl.setFieldNote("(e.g. I25699904700300)");
						} else if ("Ellis".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 62.9222.002.008.00.111)");
						} else if ("Erath".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. R.2500.01900.00.0)");
						} else if ("Fannin".equals(county)) {
							htmlControl.setFieldNote("(e.g. 9500-016-0050-05)");
						} else if ("Floyd".equals(county)) {
							htmlControl.setFieldNote("(e.g. 11110.006.0070.0)");
						} else if ("Gillespie".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. A0612-0028-000000-00)");
						} else if ("Grayson".equals(county)) {
							htmlControl.setFieldNote("(e.g. D024 3227004)");
						} else if ("Gregg".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 0023023100-068-00-I3)");
						} else if ("Guadalupe".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 1G2030-1050-01502-0-00)");
						} else if ("Hale".equals(county)) {
							htmlControl.setFieldNote("(e.g. S1028 -006-014)");
						} else if ("Hamilton".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 25650000003674001)");
						} else if ("Harrison".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 05120.00120.00000.000000)");
						} else if ("Hartley".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1103014003807)");
						} else if ("Haskell".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 0011-05011-00002-000200)");
						} else if ("Henderson".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	2390.0005.2620.30)");
						} else if ("Hidalgo".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	F0770-00-000-0061-00)");
						} else if ("Hill".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 11610-75000-00000-505000)");
						} else if ("Hockley".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 11310-00040-00080-00000)");
						} else if ("Hood".equals(county)) {
							htmlControl.setFieldNote("(e.g. 11367.000.0475.0)");
						} else if ("Hopkins".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 40.0036.003.001.00)");
						} else if ("Hudspeth".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	A070-000-00A0-0320)");
						} else if ("Hunt".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	5274-0180-0130-91)");
						} else if ("Kaufman".equals(county)) {
							htmlControl.setFieldNote("(e.g.	S3640001200)");
						} else if ("Kendall".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1-5175-0002-0040)");
						} else if ("Kenedy".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 131-1000-090-15-0-0-0-00)");
						} else if ("Kerr".equals(county)) {
							htmlControl.setFieldNote("(e.g.	6475-0000-092000)");
						} else if ("Kimble".equals(county)) {
							htmlControl.setFieldNote("(e.g.	3690-0040-007000)");
						} else if ("Kinney".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 000-0734-0103-0004-00)");
						} else if ("Kleberg".equals(county)) {
							htmlControl.setFieldNote("(e.g. 164001115000192)");
						} else if ("Knox".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00300.00030.00040.000000)");
						} else if ("La Salle".equals(county)) {
							htmlControl.setFieldNote("(e.g. 07131757010B0100)");
						} else if ("Lamar".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 020200-00020-0080)");
						} else if ("Lamb".equals(county)) {
							htmlControl.setFieldNote("(e.g. 00000-18620-001)");
						} else if ("Lavaca".equals(county)) {
							htmlControl.setFieldNote("(e.g. 292000)");
						} else if ("Lee".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Liberty".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 005640-000019-010)");
						} else if ("Lipscomb".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 041370000000000000)");
						} else if ("Llano".equals(county)) {
							htmlControl.setFieldNote("(e.g. 13370-0F3-0004-6)");
						} else if ("Matagorda".equals(county)) {
							htmlControl.setFieldNote("(e.g. 4081-0060-000A00)");
						} else if ("Madison".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. R-0231-001-0050-901)");
						} else if ("Maverick".equals(county)) {
							htmlControl.setFieldNote("(e.g. C1006170000300)");
						} else if ("McLennan".equals(county)) {
							htmlControl.setFieldNote("(e.g. 360060000014002)");
						} else if ("McMullen".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00000001570500011)");
						} else if ("Midland".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00057420.003.0100)");
						} else if ("Milam".equals(county)) {
							htmlControl.setFieldNote("(e.g. S11600-001-06-02)");
						} else if ("Mills".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1003000100100)");
						} else if ("Montague".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 10500.0016.0001.0000)");
						} else if ("Moore".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 30000-02921-06720-000000)");
						} else if ("Morris".equals(county)) {
							htmlControl.setFieldNote("(e.g. 16000052000030)");
						} else if ("Navarro".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 10334.00.00220.000.00.0)");
						} else if ("Parker".equals(county)) {
							htmlControl.setFieldNote("(e.g. 18240.005.029.00)");
						} else if ("Parmer".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 6-CFL-044-008-000)");
						} else if ("Polk".equals(county)) {
							htmlControl
								.setFieldNote("(e.g. 10056003611)");
						} else if ("Rains".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 0131-0058-0000-43)");
						} else if ("Real".equals(county)) {
							htmlControl.setFieldNote("(e.g. A167-3-11-00004)");
						} else if ("Rockwall".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 5021-0000-0022-00-0R)");
						} else if ("Runnels".equals(county)) {
							htmlControl.setFieldNote("(e.g. 36200014001100)");
						} else if ("San Jacinto".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2151-000-0360)");
						} else if ("Schleicher".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Scurry".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 01-0208-0395-0053-0010)");
						} else if ("Shackelford".equals(county)) {
							htmlControl.setFieldNote("(e.g. 00025302050)");
						} else if ("Shelby".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 19-0573-0043-0009-00)");
						} else if ("Somervell".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. E30000000010000000)");
						} else if ("Stephens".equals(county)) {
							htmlControl.setFieldNote("(e.g. 22019.000.004.00)");
						} else if ("Sterling".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00000008769000021)");
						} else if ("Swisher".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 35-1000-0001-0800)");
						} else if ("Taylor".equals(county)) {
							htmlControl.setFieldNote("(e.g. 68400004800)");
						} else if ("Terrell".equals(county)) {
							htmlControl.setFieldNote("(e.g. A0287-152-15)");
						} else if ("Titus".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 00409-00000-00430)");
						} else if ("Travis".equals(county)) {
							htmlControl.setFieldNote("(e.g. 0303000788)");
						} else if ("Upshur".equals(county)) {
							htmlControl.setFieldNote("(e.g. 440U-S06-000-014)");
						} else if ("Upton".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 0510SS-006-007-000)");
						} else if ("Uvalde".equals(county)) {
							htmlControl.setFieldNote("(e.g. 08800-0008-00)");
						} else if ("Val Verde".equals(county)) {
							htmlControl.setFieldNote("(e.g. 5010-0060-0070)");
						} else if ("Van Zandt".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 014.0198.0750.0000.0000)");
						} else if ("Victoria".equals(county)) {
							htmlControl.setFieldNote("(e.g. 33620-007-05000)");
						} else if ("Walker".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2150-111-0-00200)");
						} else if ("Webb".equals(county)) {
							htmlControl.setFieldNote("(e.g. 554-01546-060)");
						} else if ("Wichita".equals(county)) {
							htmlControl.setFieldNote("(e.g. 27250360000)");
						} else if ("Willacy".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. Q0100-14-00000-006-0A-0)");
						} else if ("Wilson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 5000-00041-00100)");
						} else if ("Winkler".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 4180-0007-0005001)");
						} else if ("Wise".equals(county)) {
							htmlControl.setFieldNote("(e.g. R1660.0151.00)");
						} else if ("Wood".equals(county)) {
							htmlControl
									.setFieldNote("(e.g.	5305-0082-0032-50)");
						} else if ("Yoakum".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 1SH10050060000000)");
						} else if ("Zapata".equals(county)) {
							htmlControl.setFieldNote("(e.g. 003201780037000)");
						} else if ("Atascosa".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 01840-00-000-003500)");
						} else if ("Sutton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 	01MC 2 28 0001)");
						} else if ("Brewster".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 972200010009000000)");
						} else if ("Cass".equals(county)) {
							htmlControl
									.setFieldNote("(e.g. 40250-00130-00080-000000)");
						}
					}
					if ("PIDN".equals(functionName)) {
						if ("Andrews".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1138)");
						} else if ("Angelina".equals(county)) {
							htmlControl.setFieldNote("(e.g. 12514)");
						} else if ("Bandera".equals(county)) {
							htmlControl.setFieldNote("(e.g. 177605)");
						} else if ("Bell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	107368)");
						} else if ("Bexar".equals(county)) {
							htmlControl.setFieldNote("(e.g.	563638)");
						} else if ("Blanco".equals(county)) {
							htmlControl.setFieldNote("(e.g.	10562)");
						} else if ("Brazoria".equals(county)) {
							htmlControl.setFieldNote("(e.g.	262935)");
						} else if ("Brazos".equals(county)) {
							htmlControl.setFieldNote("(e.g.	100789)");
						} else if ("Brooks".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1943)");
						} else if ("Brown".equals(county)) {
							htmlControl.setFieldNote("(e.g.	45427)");
						} else if ("Burnet".equals(county)) {
							htmlControl.setFieldNote("(e.g.	21669)");
							
							htmlControl.setName("9.prop_id");
							tsServerInfoModule.getFunction(2).setParamName("9.prop_id");
							
						} else if ("Caldwell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	19773)");
							
							htmlControl.setName("9.prop_id");
							tsServerInfoModule.getFunction(2).setParamName("9.prop_id");
							
						} else if ("Calhoun".equals(county)) {
							htmlControl.setFieldNote("(e.g.	22072)");
						} else if ("Callahan".equals(county)) {
							htmlControl.setFieldNote("(e.g.	R000012886)");
						} else if ("Cameron".equals(county)) {
							htmlControl.setFieldNote("(e.g.	134905)");
						} else if ("Camp".equals(county)) {
							htmlControl.setFieldNote("(e.g.	2053)");
						} else if ("Cherokee".equals(county)) {
							htmlControl.setFieldNote("(e.g.	106982000)");
						} else if ("Collin".equals(county)) {
							htmlControl.setFieldNote("(e.g.	252014)");
						} else if ("Colorado".equals(county)) {
							htmlControl.setFieldNote("(e.g.	11561)");
						} else if ("Comal".equals(county)) {
							htmlControl.setFieldNote("(e.g.	14831)");
						} else if ("Comanche".equals(county)) {
							htmlControl.setFieldNote("(e.g.	3023)");
						} else if ("Coryell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	134168)");
						} else if ("Dallam".equals(county)) {
							htmlControl.setFieldNote("(e.g.	11631)");
						} else if ("Deaf Smith".equals(county)) {
							htmlControl.setFieldNote("(e.g.	3239)");
						} else if ("Delta".equals(county)) {
							htmlControl.setFieldNote("(e.g.	5536)");
						} else if ("Denton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 47865)");
						} else if ("Dimmit".equals(county)) {
							htmlControl.setFieldNote("(e.g. 19137)");
						} else if ("Duval".equals(county)) {
							htmlControl.setFieldNote("(e.g. 12150)");
						} else if ("Edwards".equals(county)) {
							htmlControl.setFieldNote("(e.g. 16392)");
						} else if ("El Paso".equals(county)) {
							htmlControl.setFieldNote("(e.g. 337373)");
						} else if ("Ellis".equals(county)) {
							htmlControl.setFieldNote("(e.g. 167885)");
						} else if ("Erath".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000029121)");
						} else if ("Fannin".equals(county)) {
							htmlControl.setFieldNote("(e.g. 92334)");
						} else if ("Floyd".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000001987)");
						} else if ("Gillespie".equals(county)) {
							htmlControl.setFieldNote("(e.g. 74577)");
						} else if ("Grayson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 145726)");
						} else if ("Guadalupe".equals(county)) {
							htmlControl.setFieldNote("(e.g. 133595)");
						} else if ("Gregg".equals(county)) {
							htmlControl.setFieldNote("(e.g. 202113)");
						} else if ("Hale".equals(county)) {
							htmlControl.setFieldNote("(e.g. 23737)");
						} else if ("Hamilton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 17099)");
						} else if ("Harrison".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000015354)");
						} else if ("Hartley".equals(county)) {
							htmlControl.setFieldNote("(e.g.	R000001153)");
						} else if ("Haskell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	5396)");
						} else if ("Henderson".equals(county)) {
							htmlControl.setFieldNote("(e.g.	R000037756)");
						} else if ("Hidalgo".equals(county)) {
							htmlControl.setFieldNote("(e.g.	685186)");
						} else if ("Hill".equals(county)) {
							htmlControl.setFieldNote("(e.g.	135175)");
						} else if ("Hockley".equals(county)) {
							htmlControl.setFieldNote("(e.g. 24654)");
						} else if ("Hood".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000004867)");
						} else if ("Hopkins".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000003206)");
						} else if ("Hudspeth".equals(county)) {
							htmlControl.setFieldNote("(e.g.	15260)");
						} else if ("Hunt".equals(county)) {
							htmlControl.setFieldNote("(e.g.	212847)");
						} else if ("Kaufman".equals(county)) {
							htmlControl.setFieldNote("(e.g.	39835)");
						} else if ("Kendall".equals(county)) {
							htmlControl.setFieldNote("(e.g.	21610)");
						} else if ("Kenedy".equals(county)) {
							htmlControl.setFieldNote("(e.g.	15953)");
						} else if ("Kerr".equals(county)) {
							htmlControl.setFieldNote("(e.g.	63726)");
						} else if ("Kimble".equals(county)) {
							htmlControl.setFieldNote("(e.g.	13788)");
						} else if ("Kinney".equals(county)) {
							htmlControl.setFieldNote("(e.g.	21316)");
						} else if ("Kleberg".equals(county)) {
							htmlControl.setFieldNote("(e.g. 12622)");
						} else if ("Knox".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000003573)");
						} else if ("La Salle".equals(county)) {
							htmlControl.setFieldNote("(e.g. 12284)");
						} else if ("Lamar".equals(county)) {
							htmlControl.setFieldNote("(e.g. 20235)");
						} else if ("Lamb".equals(county)) {
							htmlControl.setFieldNote("(e.g. 12007)");
						} else if ("Lavaca".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1605)");
						} else if ("Lee".equals(county)) {
							htmlControl.setFieldNote("(e.g. 15073)");
						} else if ("Liberty".equals(county)) {
							htmlControl.setFieldNote("(e.g. 54036)");
						} else if ("Lipscomb".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000154776)");
						} else if ("Llano".equals(county)) {
							htmlControl.setFieldNote("(e.g. 40436)");
							
							htmlControl.setName("9.prop_id");
							tsServerInfoModule.getFunction(2).setParamName("9.prop_id");
							
						} else if ("Matagorda".equals(county)) {
							htmlControl.setFieldNote("(e.g. 45932)");
						} else if ("Madison".equals(county)) {
							htmlControl.setFieldNote("(e.g. 23454)");
						} else if ("Maverick".equals(county)) {
							htmlControl.setFieldNote("(e.g. 6448)");
						} else if ("McLennan".equals(county)) {
							htmlControl.setFieldNote("(e.g. 139296)");
						} else if ("McMullen".equals(county)) {
							htmlControl.setFieldNote("(e.g. 4636)");
						} else if ("Midland".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000030116)");
						} else if ("Milam".equals(county)) {
							htmlControl.setFieldNote("(e.g. 12717)");
						} else if ("Mills".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000000380)");
						} else if ("Montague".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000004946)");
						} else if ("Moore".equals(county)) {
							htmlControl.setFieldNote("(e.g. 13379)");
						} else if ("Morris".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000011977)");
						} else if ("Navarro".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000014834)");
						} else if ("Parker".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000028022)");
						} else if ("Parmer".equals(county)) {
							htmlControl.setFieldNote("(e.g. 205696)");
						}  else if ("Polk".equals(county)) {
							htmlControl.setFieldNote("(e.g. 14817)");
						} else if ("Rains".equals(county)) {
							htmlControl.setFieldNote("(e.g. 205696)");
						} else if ("Real".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1750)");
						} else if ("Rockwall".equals(county)) {
							htmlControl.setFieldNote("(e.g. 22854)");
						} else if ("Runnels".equals(county)) {
							htmlControl.setFieldNote("(e.g. 22854)");
						} else if ("San Jacinto".equals(county)) {
							htmlControl.setFieldNote("(e.g. 94083)");
						} else if ("Schleicher".equals(county)) {
							htmlControl.setFieldNote("(e.g. 11656)");
						} else if ("Scurry".equals(county)) {
							htmlControl.setFieldNote("(e.g. 14300)");
						} else if ("Shackelford".equals(county)) {
							htmlControl.setFieldNote("(e.g. 15576)");
						} else if ("Shelby".equals(county)) {
							htmlControl.setFieldNote("(e.g. 22774)");
						} else if ("Somervell".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000003930)");
						} else if ("Stephens".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000003930)");
						} else if ("Sterling".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3229)");
						} else if ("Swisher".equals(county)) {
							htmlControl.setFieldNote("(e.g. 18841)");
						} else if ("Taylor".equals(county)) {
							htmlControl.setFieldNote("(e.g. 11125)");
						} else if ("Terrell".equals(county)) {
							htmlControl.setFieldNote("(e.g. 11546)");
						} else if ("Titus".equals(county)) {
							htmlControl.setFieldNote("(e.g. 6878)");
						} else if ("Travis".equals(county)) {
							htmlControl.setFieldNote("(e.g. 284423)");

							htmlControl.setName("9.prop_id");
							tsServerInfoModule.getFunction(2).setParamName("9.prop_id");
						} else if ("Upshur".equals(county)) {
							htmlControl.setFieldNote("(e.g. 37403)");
						} else if ("Upton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2993)");
						} else if ("Uvalde".equals(county)) {
							htmlControl.setFieldNote("(e.g. 13041)");
						} else if ("Val Verde".equals(county)) {
							htmlControl.setFieldNote("(e.g. 19993)");
						} else if ("Van Zandt".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000048471)");
						} else if ("Victoria".equals(county)) {
							htmlControl.setFieldNote("(e.g. 48651)");
						} else if ("Walker".equals(county)) {
							htmlControl.setFieldNote("(e.g. 21930)");
						} else if ("Webb".equals(county)) {
							htmlControl.setFieldNote("(e.g. 178488)");
						} else if ("Wichita".equals(county)) {
							htmlControl.setFieldNote("(e.g. 137414)");
						} else if ("Willacy".equals(county)) {
							htmlControl.setFieldNote("(e.g. 18423)");
						} else if ("Wilson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 28898)");
						} else if ("Winkler".equals(county)) {
							htmlControl.setFieldNote("(e.g. 8290)");
						} else if ("Wise".equals(county)) {
							htmlControl.setFieldNote("(e.g. R000031261)");
						} else if ("Wood".equals(county)) {
							htmlControl.setFieldNote("(e.g.	40206)");
						} else if ("Yoakum".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2604)");
						} else if ("Zapata".equals(county)) {
							htmlControl.setFieldNote("(e.g. 6108)");
						} else if ("Atascosa".equals(county)) {
							htmlControl.setFieldNote("(e.g. 18719)");
						} else if ("Sutton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 100546)");
						} else if ("Brewster".equals(county)) {
							htmlControl.setFieldNote("(e.g. 11125)");
						} else if ("Cass".equals(county)) {
							htmlControl.setFieldNote("(e.g. 19769)");
						}
					}
					if ("Street No.".equals(functionName)) {
						if ("Andrews".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1224)");
						} else if ("Angelina".equals(county)) {
							htmlControl.setFieldNote("(e.g. 813)");
						} else if ("Bandera".equals(county)) {
							htmlControl.setFieldNote("(e.g. 495)");
						} else if ("Bell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	402)");
						} else if ("Bexar".equals(county)) {
							htmlControl.setFieldNote("(e.g.	212)");
						} else if ("Blanco".equals(county)) {
							htmlControl.setFieldNote("(e.g.	223)");
						} else if ("Brazoria".equals(county)) {
							htmlControl.setFieldNote("(e.g.	831)");
						} else if ("Brazos".equals(county)) {
							htmlControl.setFieldNote("(e.g.	3211)");
						} else if ("Brooks".equals(county)) {
							htmlControl.setFieldNote("(e.g.	202)");
						} else if ("Brown".equals(county)) {
							htmlControl.setFieldNote("(e.g.	7038)");
						} else if ("Burnet".equals(county)) {
							htmlControl.setFieldNote("(e.g.	100)");
						} else if ("Caldwell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	905)");
						} else if ("Calhoun".equals(county)) {
							htmlControl.setFieldNote("(e.g.	332)");
						} else if ("Callahan".equals(county)) {
							htmlControl.setFieldNote("(e.g.	443)");
						} else if ("Cameron".equals(county)) {
							htmlControl.setFieldNote("(e.g.	123)");
						} else if ("Camp".equals(county)) {
							htmlControl.setFieldNote("(e.g.	112)");
						} else if ("Cherokee".equals(county)) {
							htmlControl.setFieldNote("(e.g.	412)");
						} else if ("Collin".equals(county)) {
							htmlControl.setFieldNote("(e.g.	4121)");
						} else if ("Colorado".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1130)");
						} else if ("Comal".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1095)");
						} else if ("Comanche".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1200)");
						} else if ("Coryell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	800)");
						} else if ("Dallam".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1518)");
						} else if ("Deaf Smith".equals(county)) {
							htmlControl.setFieldNote("(e.g.	223)");
						} else if ("Delta".equals(county)) {
							htmlControl.setFieldNote("(e.g.	801)");
						} else if ("Denton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3447)");
						} else if ("Dimmit".equals(county)) {
							htmlControl.setFieldNote("(e.g. 703)");
						} else if ("Duval".equals(county)) {
							htmlControl.setFieldNote("(e.g. 210)");
						} else if ("Edwards".equals(county)) {
							htmlControl.setFieldNote("(e.g. 309)");
						} else if ("El Paso".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3460)");
						} else if ("Ellis".equals(county)) {
							htmlControl.setFieldNote("(e.g. 618)");
						} else if ("Erath".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1331)");
						} else if ("Fannin".equals(county)) {
							htmlControl.setFieldNote("(e.g. 201)");
						} else if ("Floyd".equals(county)) {
							htmlControl.setFieldNote("(e.g. 907)");
						} else if ("Gillespie".equals(county)) {
							htmlControl.setFieldNote("(e.g. 213)");
						} else if ("Grayson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1331)");
						} else if ("Guadalupe".equals(county)) {
							htmlControl.setFieldNote("(e.g. 319)");
						} else if ("Gregg".equals(county)) {
							htmlControl.setFieldNote("(e.g. 206)");
						} else if ("Hale".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1406)");
						} else if ("Hamilton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1007)");
						} else if ("Harrison".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1806)");
						} else if ("Hartley".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1109)");
						} else if ("Haskell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1109)");
						} else if ("Henderson".equals(county)) {
							htmlControl.setFieldNote("(e.g.	131)");
						} else if ("Hidalgo".equals(county)) {
							htmlControl.setFieldNote("(e.g.	719)");
						} else if ("Hill".equals(county)) {
							htmlControl.setFieldNote("(e.g.	505)");
						} else if ("Hockley".equals(county)) {
							htmlControl.setFieldNote("(e.g. 410)");
						} else if ("Hood".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2615)");
						} else if ("Hopkins".equals(county)) {
							htmlControl.setFieldNote("(e.g. 908)");
						} else if ("Hudspeth".equals(county)
								|| "Knox".equals(county)
								|| "La Salle".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Hunt".equals(county)) {
							htmlControl.setFieldNote("(e.g.	1316)");
						} else if ("Kaufman".equals(county)) {
							htmlControl.setFieldNote("(e.g.	110)");
						} else if ("Kendall".equals(county)) {
							htmlControl.setFieldNote("(e.g.	602)");
						} else if ("Kenedy".equals(county)) {
							htmlControl.setFieldNote("(e.g.	423)");
						} else if ("Kerr".equals(county)) {
							htmlControl.setFieldNote("(e.g.	266)");
						} else if ("Kimble".equals(county)) {
							htmlControl.setFieldNote("(e.g.	806)");
						} else if ("Kinney".equals(county)) {
							htmlControl.setFieldNote("(e.g.	88)");
						} else if ("Kleberg".equals(county)) {
							htmlControl.setFieldNote("(e.g. 820)");
						} else if ("Lamar".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1055)");
						} else if ("Lamb".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1410)");
						} else if ("Lavaca".equals(county)) {
							htmlControl.setFieldNote("(e.g. 102)");
						} else if ("Lee".equals(county)) {
							htmlControl.setFieldNote("(e.g. 736)");
						} else if ("Liberty".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2519)");
						} else if ("Lipscomb".equals(county)) {
							htmlControl.setFieldNote("(e.g. 302)");
						} else if ("Llano".equals(county)) {
							htmlControl.setFieldNote("(e.g. 409)");
						} else if ("Matagorda".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1309)");
						} else if ("Madison".equals(county)) {
							htmlControl.setFieldNote("(e.g. 8777)");
						} else if ("Maverick".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1047)");
						} else if ("McLennan".equals(county)) {
							htmlControl.setFieldNote("(e.g. 659)");
						} else if ("McMullen".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Midland".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3215)");
						} else if ("Milam".equals(county)) {
							htmlControl.setFieldNote("(e.g. 724)");
						} else if ("Mills".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1602)");
						} else if ("Montague".equals(county)) {
							htmlControl.setFieldNote("(e.g. 107)");
						} else if ("Moore".equals(county)) {
							htmlControl.setFieldNote("(e.g. 219)");
						} else if ("Morris".equals(county)) {
							htmlControl.setFieldNote("(e.g. 104)");
						} else if ("Navarro".equals(county)) {
							htmlControl.setFieldNote("(e.g. 527)");
						} else if ("Parker".equals(county)) {
							htmlControl.setFieldNote("(e.g. 416)");
						} else if ("Parmer".equals(county)) {
							htmlControl.setFieldNote("(e.g. 610)");
						} else if ("Rains".equals(county)) {
							htmlControl.setFieldNote("(e.g. 8559)");
						} else if ("Polk".equals(county)) {
							htmlControl.setFieldNote("(e.g. 109)");
						} else if ("Real".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Rockwall".equals(county)) {
							htmlControl.setFieldNote("(e.g. 22)");
						} else if ("Runnels".equals(county)) {
							htmlControl.setFieldNote("(e.g. 207 )");
						} else if ("San Jacinto".equals(county)) {
							htmlControl.setFieldNote("(e.g. 90)");
						} else if ("Schleicher".equals(county)) {
							htmlControl.setFieldNote("(e.g. 803 )");
						} else if ("Scurry".equals(county)) {
							htmlControl.setFieldNote("(e.g. 00355 )");
						} else if ("Shackelford".equals(county)) {
							htmlControl.setFieldNote("(e.g. 800 )");
						} else if ("Shelby".equals(county)) {
							htmlControl.setFieldNote("(e.g. 407 )");
						} else if ("Somervell".equals(county)) {
							htmlControl.setFieldNote("(e.g. 909 )");
						} else if ("Stephens".equals(county)) {
							htmlControl.setFieldNote("(e.g. 13935 )");
						} else if ("Sterling".equals(county)) {
							htmlControl.setFieldNote("(e.g. 809 )");
						} else if ("Swisher".equals(county)) {
							htmlControl.setFieldNote("(e.g. 19)");
						} else if ("Taylor".equals(county)) {
							htmlControl.setFieldNote("(e.g. 659 )");
						} else if ("Terrell".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Titus".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1803 )");
						} else if ("Travis".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2215 )");
						} else if ("Upshur".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3741 )");
						} else if ("Upton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1311 )");
						} else if ("Uvalde".equals(county)) {
							htmlControl.setFieldNote("(e.g. 508 )");
						} else if ("Val Verde".equals(county)) {
							htmlControl.setFieldNote("(e.g. 113 )");
						} else if ("Van Zandt".equals(county)) {
							htmlControl.setFieldNote("(e.g. 505)");
						} else if ("Victoria".equals(county)) {
							htmlControl.setFieldNote("(e.g. 304)");
						} else if ("Walker".equals(county)) {
							htmlControl.setFieldNote("(e.g. 624)");
						} else if ("Webb".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2616)");
						} else if ("Wichita".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1644)");
						} else if ("Willacy".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Wilson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 102)");
						} else if ("Winkler".equals(county)) {
							htmlControl.setFieldNote("(e.g. 912)");
						} else if ("Wise".equals(county)) {
							htmlControl.setFieldNote("(e.g. 239)");
						} else if ("Wood".equals(county)) {
							htmlControl.setFieldNote("(e.g.	311)");
						} else if ("Yoakum".equals(county)) {
							htmlControl.setFieldNote("(e.g. 109)");
						} else if ("Zapata".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1107)");
						} else if ("Atascosa".equals(county)) {
							htmlControl.setFieldNote("(e.g. 112)");
						} else if ("Sutton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 101)");
						} else if ("Brewster".equals(county)) {
							htmlControl.setFieldNote("(e.g. 614)");
						} else if ("Cass".equals(county)) {
							htmlControl.setFieldNote("(e.g. 204)");
						}
					}
					if ("Street Name".equals(functionName)) {
						if ("Andrews".equals(county)) {
							htmlControl.setFieldNote("(e.g. Alpine)");
						} else if ("Angelina".equals(county)) {
							htmlControl.setFieldNote("(e.g. Augusta)");
						} else if ("Bandera".equals(county)) {
							htmlControl.setFieldNote("(e.g. Rainbow)");
						} else if ("Bell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Tower)");
						} else if ("Bexar".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Hermosa)");
						} else if ("Blanco".equals(county)
								|| "Cameron".equals(county)
								|| "Kinney".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Mesquite)");
						} else if ("Brazoria".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Marshall)");
						} else if ("Brazos".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Broadmoor)");
						} else if ("Brooks".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Cedar)");
						} else if ("Brown".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Shamrock)");
						} else if ("Burnet".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Water)");
						} else if ("Caldwell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Medina)");
						} else if ("Calhoun".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Seadrift)");
						} else if ("Callahan".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Ridgeway)");
						} else if ("Camp".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Parkland)");
						} else if ("Cherokee".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Angelina)");
						} else if ("Collin".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Nightfall)");
						} else if ("Colorado".equals(county)) {
							htmlControl.setFieldNote("(e.g.	BONHAM)");
						} else if ("Comal".equals(county)) {
							htmlControl.setFieldNote("(e.g.	HARRIET)");
						} else if ("Comanche".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Austin)");
						} else if ("Coryell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	River)");
						} else if ("Dallam".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Dodson)");
						} else if ("Deaf Smith".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Ironwood)");
						} else if ("Delta".equals(county)) {
							htmlControl.setFieldNote("(e.g.	First)");
						} else if ("Denton".equals(county)) {
							htmlControl.setFieldNote("(e.g. LIVINGSTON)");
						} else if ("Dimmit".equals(county)) {
							htmlControl.setFieldNote("(e.g. MARGUERITE DR)");
						} else if ("Duval".equals(county)) {
							htmlControl.setFieldNote("(e.g. Victoria)");
						} else if ("Edwards".equals(county)) {
							htmlControl.setFieldNote("(e.g. Pepper)");
						} else if ("El Paso".equals(county)) {
							htmlControl.setFieldNote("(e.g. PENDLETON)");
						} else if ("Ellis".equals(county)) {
							htmlControl.setFieldNote("(e.g. Cardinal)");
						} else if ("Erath".equals(county)) {
							htmlControl.setFieldNote("(e.g. GARFIELD)");
						} else if ("Fannin".equals(county)) {
							htmlControl.setFieldNote("(e.g. CHERRY)");
						} else if ("Floyd".equals(county)) {
							htmlControl.setFieldNote("(e.g. GARRISON)");
						} else if ("Gillespie".equals(county)) {
							htmlControl.setFieldNote("(e.g. RANCH ROAD)");
						} else if ("Grayson".equals(county)) {
							htmlControl.setFieldNote("(e.g. HULL)");
						} else if ("Guadalupe".equals(county)) {
							htmlControl.setFieldNote("(e.g. Moore)");
						} else if ("Gregg".equals(county)) {
							htmlControl.setFieldNote("(e.g. SMITH)");
						} else if ("Hale".equals(county)) {
							htmlControl.setFieldNote("(e.g. FLOYDADA)");
						} else if ("Hamilton".equals(county)) {
							htmlControl.setFieldNote("(e.g. ROSS)");
						} else if ("Harrison".equals(county)) {
							htmlControl.setFieldNote("(e.g. GATEWOOD)");
						} else if ("Hartley".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Main)");
						} else if ("Haskell".equals(county)) {
							htmlControl.setFieldNote("(e.g.	MCCARTY)");
						} else if ("Henderson".equals(county)) {
							htmlControl.setFieldNote("(e.g.	SPEARMAN)");
						} else if ("Hidalgo".equals(county)) {
							htmlControl.setFieldNote("(e.g.	DARLENE)");
						} else if ("Hill".equals(county)) {
							htmlControl.setFieldNote("(e.g.	CRAIG)");
						} else if ("Hockley".equals(county)) {
							htmlControl.setFieldNote("(e.g. GRANT)");
						} else if ("Hood".equals(county)) {
							htmlControl.setFieldNote("(e.g. AUSTIN)");
						} else if ("Hopkins".equals(county)) {
							htmlControl.setFieldNote("(e.g. FISHER)");
						} else if ("Hudspeth".equals(county)
								|| "Knox".equals(county)
								|| "La Salle".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Hunt".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Alder)");
						} else if ("Kaufman".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Jeter)");
						} else if ("Kendall".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Main)");
						} else if ("Kenedy".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Garcia)");
						} else if ("Kerr".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Honor)");
						} else if ("Kimble".equals(county)) {
							htmlControl.setFieldNote("(e.g.	Pecan)");
						} else if ("Kleberg".equals(county)) {
							htmlControl.setFieldNote("(e.g. ALEXANDER)");
						} else if ("Lamar".equals(county)) {
							htmlControl.setFieldNote("(e.g. LAUREL)");
						} else if ("Lamb".equals(county)) {
							htmlControl.setFieldNote("(e.g. DELANO)");
						} else if ("Lavaca".equals(county)) {
							htmlControl.setFieldNote("(e.g. Rick DR)");
						} else if ("Lee".equals(county)) {
							htmlControl.setFieldNote("(e.g. HOUSTON)");
						} else if ("Liberty".equals(county)) {
							htmlControl.setFieldNote("(e.g. WEBSTER)");
						} else if ("Lipscomb".equals(county)) {
							htmlControl.setFieldNote("(e.g. Main)");
						} else if ("Llano".equals(county)) {
							htmlControl.setFieldNote("(e.g. GRANITE)");
						} else if ("Matagorda".equals(county)) {
							htmlControl.setFieldNote("(e.g. DUNCAN)");
						} else if ("Madison".equals(county)) {
							htmlControl.setFieldNote("(e.g. WOOD)");
						} else if ("Maverick".equals(county)) {
							htmlControl.setFieldNote("(e.g. CAMARINOS)");
						} else if ("McLennan".equals(county)) {
							htmlControl.setFieldNote("(e.g. DOSHER)");
						} else if ("McMullen".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Midland".equals(county)) {
							htmlControl.setFieldNote("(e.g. SHELL)");
						} else if ("Milam".equals(county)) {
							htmlControl.setFieldNote("(e.g. COPELAND)");
						} else if ("Mills".equals(county)) {
							htmlControl.setFieldNote("(e.g. FISHER)");
						} else if ("Montague".equals(county)) {
							htmlControl.setFieldNote("(e.g. CRUMP)");
						} else if ("Moore".equals(county)) {
							htmlControl.setFieldNote("(e.g. MEREDITH)");
						} else if ("Morris".equals(county)) {
							htmlControl.setFieldNote("(e.g. CORNETT )");
						} else if ("Navarro".equals(county)) {
							htmlControl.setFieldNote("(e.g. LEXINGTON )");
						} else if ("Parker".equals(county)) {
							htmlControl.setFieldNote("(e.g. ELMWOOD)");
						} else if ("Parmer".equals(county)) {
							htmlControl.setFieldNote("(e.g. FIFTH ST)");
						}  else if ("Polk".equals(county)) {
							htmlControl.setFieldNote("(e.g. WINDSOR)");
						} else if ("Rains".equals(county)) {
							htmlControl.setFieldNote("(e.g. FM 779)");
						} else if ("Real".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Rockwall".equals(county)) {
							htmlControl.setFieldNote("(e.g. SMITH)");
						} else if ("Runnels".equals(county)) {
							htmlControl.setFieldNote("(e.g. HAMILTON )");
						} else if ("San Jacinto".equals(county)) {
							htmlControl.setFieldNote("(e.g. Tobago)");
						} else if ("Schleicher".equals(county)) {
							htmlControl.setFieldNote("(e.g. HIGHLAND )");
						} else if ("Scurry".equals(county)) {
							htmlControl.setFieldNote("(e.g. GERMAN )");
						} else if ("Shackelford".equals(county)) {
							htmlControl.setFieldNote("(e.g. HERRON )");
						} else if ("Shelby".equals(county)) {
							htmlControl.setFieldNote("(e.g. TRAVIS )");
						} else if ("Somervell".equals(county)) {
							htmlControl.setFieldNote("(e.g. HEREFORD )");
						} else if ("Stephens".equals(county)) {
							htmlControl.setFieldNote("(e.g. HILL )");
						} else if ("Sterling".equals(county)) {
							htmlControl.setFieldNote("(e.g. JACKSON )");
						} else if ("Swisher".equals(county)) {
							htmlControl.setFieldNote("(e.g. NORFLEET)");
						} else if ("Taylor".equals(county)) {
							htmlControl.setFieldNote("(e.g. WESTWOOD )");
						} else if ("Terrell".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Titus".equals(county)) {
							htmlControl.setFieldNote("(e.g. FLOREY )");
						} else if ("Travis".equals(county)) {
							htmlControl.setFieldNote("(e.g. POST)");
						} else if ("Upshur".equals(county)) {
							htmlControl.setFieldNote("(e.g. HAWK)");
						} else if ("Upton".equals(county)) {
							htmlControl.setFieldNote("(e.g. HOUSTON)");
						} else if ("Uvalde".equals(county)) {
							htmlControl.setFieldNote("(e.g. MINTER)");
						} else if ("Val Verde".equals(county)) {
							htmlControl.setFieldNote("(e.g. FIESTA)");
						} else if ("Van Zandt".equals(county)) {
							htmlControl.setFieldNote("(e.g. FORREST)");
						} else if ("Victoria".equals(county)) {
							htmlControl.setFieldNote("(e.g. GEORGIA)");
						} else if ("Walker".equals(county)) {
							htmlControl.setFieldNote("(e.g. LOWRY )");
						} else if ("Webb".equals(county)) {
							htmlControl.setFieldNote("(e.g. OKANE)");
						} else if ("Wichita".equals(county)) {
							htmlControl.setFieldNote("(e.g. CONKLING)");
						} else if ("Willacy".equals(county)) {
							htmlControl.setFieldNote("");
						} else if ("Wilson".equals(county)) {
							htmlControl.setFieldNote("(e.g. SALMON)");
						} else if ("Winkler".equals(county)) {
							htmlControl.setFieldNote("(e.g. JEFFEE)");
						} else if ("Wise".equals(county)) {
							htmlControl.setFieldNote("(e.g. LAKE)");
						} else if ("Wood".equals(county)) {
							htmlControl.setFieldNote("(e.g.	BLACKJACK)");
						} else if ("Yoakum".equals(county)) {
							htmlControl.setFieldNote("(e.g. JAYCEE)");
						} else if ("Zapata".equals(county)) {
							htmlControl.setFieldNote("(e.g. FALCON)");
						} else if ("Atascosa".equals(county)) {
							htmlControl.setFieldNote("(e.g. HIDDEN)");
						} else if ("Sutton".equals(county)) {
							htmlControl.setFieldNote("(e.g. HILLSIDE)");
						} else if ("Brewster".equals(county)) {
							htmlControl.setFieldNote("(e.g. CHERRY)");
						} else if ("Cass".equals(county)) {
							htmlControl.setFieldNote("(e.g. TIPTON)");
						}
					}
				}
			}
		}

		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		GenericAddressFilter addressFilter = AddressFilterFactory
				.getGenericAddressHighPassFilter(searchId, 0.8d);

		addPinModules(serverInfo, modules);

		if (hasStreet()) {
			// Search by Property Address
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();

			String streetNo = getSearchAttribute(SearchAttributes.P_STREETNO);

			if ("ochiltree".equals(county)) {
				int len = streetNo.length();
				if (len < 5)
					for (int i = len; i < 6; i++)
						streetNo = "0" + streetNo; // add leading zeroes for
													// street numbers with less
													// than 5 digits
			}
			module.forceValue(3, streetNo);
			if (BAILEY_2_LIST.contains(county))
				module.setSaKey(4, SearchAttributes.P_STREET_FULL_NAME);
			else
				module.setSaKey(4, SearchAttributes.P_STREETNAME);
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getHybridNameFilter(
					SearchAttributes.OWNER_OBJECT, searchId, module));
			modules.add(module);
		}

		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L F;;", "L, F;;" }));
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	protected void addPinModules(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		
		TSServerInfoModule module = null;
		
		if (hasPin()) {
			if (!"Guadalupe".equals( dataSite.getCountyName())) {
				module = new TSServerInfoModule(
						serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(2, SearchAttributes.LD_PARCELNO_GENERIC_AO);	//search with PIDN
				modules.add(module);
			}
			
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.LD_PARCELNO_GENERIC_AO);	//backup search with GEO Account
			modules.add(module);
			
			if ("Guadalupe".equals( dataSite.getCountyName())) { //task 9212
				module = new TSServerInfoModule(
						serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(2, SearchAttributes.LD_PARCELNO_GENERIC_AO);	//search with PIDN
				modules.add(module);
			}
		}
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();
		// String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		switch (viParseID) {

		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:

			StringBuilder outputTable = new StringBuilder();

			if (rsResponse.indexOf("Server error!") != -1) {
				Response.getParsedResponse().setError("Server error!");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			if (rsResponse.indexOf("Your search did not match any records") != -1) {
				Response.getParsedResponse().setError(NO_DATA_FOUND);
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			if (rsResponse.indexOf("Your search returned too many results") != -1) {
				Response.getParsedResponse().setError("Your search returned too many results.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			contents = cleanIntermediaryResponse(rsResponse);
			try {
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
						Response, contents, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(
							smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE,
							outputTable.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case ID_DETAILS:

			String details = getDetails(rsResponse, county);
			String pid = "";
			if (county.equals("jimwells") || county.equals("jefferson"))
				pid = getPID(details, LONG_ACCOUNT_NUMBER_KEY, LONG_ACCOUNT_NUMBER_PATTERN);
			else if (ARMSTRONG_58_LIST.contains(county)
					|| ARCHER_19_LIST.contains(county)
					|| (!county.equals("bosque") && !county.equals("coleman") && ANDERSON_11_LIST.contains(county))
					|| DALLAS_3_LIST.contains(county) 
					|| county.equals("harris") 
					|| county.equals("sanpatricio"))
				pid = getPID(details, ACCOUNT_NUMBER_KEY, ACCOUNT_NUMBER_PATTERN);
			else if (BRISCOE_5_LIST.contains(county)) {
				pid = getPID(details, ACCOUNT_NO_KEY, ACCOUNT_NO_PATTERN);
			} else {
				pid = getPID(details, PIDN_KEY, PIDN_PATTERN);
			}

			if ((!downloadingForSave)) {
				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + pid + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "ASSESSOR");
				data.put("dataSource", "AO");

				if (isInstrumentSaved(pid, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							viParseID);
				}

				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details,
						Parser.NO_PARSE);
			} else {
				smartParseDetails(Response, details);
				msSaveToTSDFileName = pid + ".html";
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;

		case ID_GET_LINK:
			if (sAction.indexOf("detail.php") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else if (sAction.indexOf("list.php") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}

			break;

		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			try{
	    		String prevSrcType = (String)map.get(OtherInformationSetKey.SRC_TYPE.getKeyName());
	    		if(StringUtils.isEmpty(prevSrcType)){	    			
	    			map.getMap().put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
	    		}
			}catch(Exception e){
				e.printStackTrace();
			}  
			document = bridge.importData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		detailsHtml = correctHtml(detailsHtml);
		if(fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if(document!=null) {
				response.getParsedResponse().setDocument(document);
			}
		}
		response.getParsedResponse().setSearchId(this.searchId);
		return document;
	}
	
	public String correctHtml(String html) {
		html = html.replaceAll("(?is)<td class=\"reports_blacktxt\">", "<td width=\"600px\">");
		html = html.replaceAll("(?is)(</td>)\\s*(<td[^>]*>)\\s*(<table[^>]*>)", "$1</tr><tr>$2$3");
		html = html.replaceAll("(?is)width=\"\\d+%\"", "");
		return html;
	}
	
	protected String cleanIntermediaryResponse(String rsResponse) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true);
//			if (tables.size() > 3) {
				for (int i = 0; i < tables.size(); i++) {
					if (tables.elementAt(i).toHtml().contains("Owner Name")
							&& !(tables.elementAt(i).toHtml())
									.contains("Displaying Records")) {
						rsResponse = tables.elementAt(i).toHtml();
						break;
					}
				}
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		rsResponse = rsResponse.replaceAll(
				"(?is)(<td[^>]*)(>)\\s*<a\\s+class=\\\"results_white_col[^>]*>([^<]*)</a>\\s*(</td>)", "$1 style=\"color:white\"$2$3$4");
		return rsResponse;
	}

	protected String getDetails(String response, String county) {

		// if from memory - use it as is
		if (!response.contains("<html")) {
			return response;
		}
		
		response = response.replaceAll("(?is)<script.*?</script>", "");
		response = response.replaceAll("(?is)<header.*?</header>", "");
		response = response.replaceAll("(?is)<footer.*?</footer>", "");

		//FileUtils.writeStringToFile(new File("D://gg.html"), response);
		
		String contents = "";

		try {
			HtmlParser3 htmlParser = new HtmlParser3(Tidy.tidyParse(response, null));
			Node mainContentInner = htmlParser.getNodeById("main_content_inner");
			if (mainContentInner == null){
				mainContentInner = htmlParser.getNodeById("content-body");
			}
			if(mainContentInner instanceof Div) {
//				Div myDiv = (Div)mainContentInner;
//				myDiv.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true)
//				myDiv.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "sketch_container"), true)
//				Node myNode = mainContentInner.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "sketch_container"), true);
				//getNodeByAttribute("class", "sketch_container", true);
				contents = getDetailsInternal(mainContentInner);
			} else {
				NodeList noAttributesTables = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("width")))
						.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("border")))
						.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("cellpadding")));
				if(noAttributesTables.size() > 0) {
//					TableTag myTable = (TableTag) noAttributesTables.elementAt(0);
					contents = getDetailsInternal(noAttributesTables.elementAt(0));
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!"tarrant".equalsIgnoreCase(county)) {
			// check this on atascosa
//			contents = contents.replaceAll("(?is)(</tr>)\\s*</table></td>\\s*(<tr)", "$1 $2");
			//
		}
		
		contents = contents.replaceAll("(?is)<form.*?</form>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)(<td\\s+class=\"reports_blacktxt\"\\s*>.*?)<p>.*?(</td>)", "$1$2");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");

		return contents;
	}

	protected String getDetailsInternal(Node mainContentInner) {
		String contents;
		NodeList myNodes = mainContentInner.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "sketch_item"), true);
		Node myNode = null;
		if(myNodes.size() > 0) {
			myNode = myNodes.elementAt(0);
		}
		Node previousNode = null;
		if(myNode instanceof Div) {
			while(myNode != null) {
				previousNode = myNode;
				myNode = myNode.getParent();
				if(previousNode instanceof TableRow) {
					break;
				}
			}
			if(previousNode != null) {
				if(myNode instanceof Div || myNode instanceof TableTag) {
					for (int i = 0; i < myNode.getChildren().size(); i++) {
						if(((CompositeTag)myNode).getChild(i).equals(previousNode)) {
							((CompositeTag)myNode).removeChild(i);
							break;
						}
					}
				}
			} else {
				
			}
		} else {
			NodeList tdClassReportHeadList = mainContentInner.getChildren().extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "report_head"));
			for (int indexTd = 0; indexTd < tdClassReportHeadList.size(); indexTd++) {
				if("Improvement Sketch".equals(tdClassReportHeadList.elementAt(indexTd).toPlainTextString().trim())) {
					TableColumn myTd = (TableColumn) tdClassReportHeadList.elementAt(indexTd);
					
					TableTag table = HtmlParser3.findTableFromColumn(myTd);
					myNode = table;
					while(myNode != null) {
						previousNode = myNode;
						myNode = myNode.getParent();
						if(previousNode instanceof TableRow) {
							break;
						}
					}
					if(previousNode != null) {
						if(myNode instanceof Div || myNode instanceof TableTag) {
							for (int i = 0; i < myNode.getChildren().size(); i++) {
								if(((CompositeTag)myNode).getChild(i).equals(previousNode)) {
									((CompositeTag)myNode).removeChild(i);
									break;
								}
							}
						}
					} else {
						
					}
				}
			}
			
		}
		
		NodeList childrenAsNodeArray = mainContentInner.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "breadcrumbs gradient-lt"));
		if(childrenAsNodeArray.size() > 0) {
			Div breadcrumbsDiv = (Div)childrenAsNodeArray.elementAt(0);
			breadcrumbsDiv.getChildren().keepAllNodesThatMatch(new TagNameFilter("div"));
			NodeList extractAllNodesThatMatch = breadcrumbsDiv.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"));
			for (int i = 0; i < extractAllNodesThatMatch.size(); i++) {
				Div div = (Div)extractAllNodesThatMatch.elementAt(i);
				div.getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("a")));
				div.getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("span")));
			}
		}
		
		String divEntityItemHtml = "";
		String taxEstimatorHtml = "";
		boolean toReplace = false; 	
		
		//click on "View Tax Estimator"
		NodeList divEntityItemList = mainContentInner.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "entity_item"));
		if (divEntityItemList.size()>0) {
			Node divEntityItem = divEntityItemList.elementAt(0);
			divEntityItemHtml = divEntityItem.toHtml();
			NodeList formList = divEntityItem.getChildren().extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", " ajaxit inline-block"));
			if (formList.size()>0) {
				Node form = formList.elementAt(0);
				String formHtml = form.toHtml();
				
				Map<String, String> params = new SimpleHtmlParser.Form(formHtml).getParams();
				String cnty_name = getDataSite().getCountyName().replace(" ", "").toLowerCase();
				String link = getBaseLink() + cnty_name + "/EntityItem.php";
				HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
				Iterator<Entry<String, String>> it = params.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> entry = it.next();
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
				boolean found = false;
				
				// acquire a HttpSite
				HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
				try {
					for(int i=0; i<3&&!found; i++){
						try {
							taxEstimatorHtml = site.process(req).getResponseAsString();
							if (taxEstimatorHtml.contains("Information is only available to signed-in users")) {
								req.setPostParameter("needsCookie", "true");
							} else {
								found = true;
							}
						} catch (RuntimeException e){
							logger.warn("Could not bring link:" + link, e);
						}
					}
				} finally {
					// always release the HttpSite
					HttpManager3.releaseSite(site);
				}
				
				if (!StringUtils.isEmpty(taxEstimatorHtml)) {
					toReplace = true;
				}
				
			}
		}
		
		contents = mainContentInner.toHtml();
		if (toReplace) {
			contents = contents.replace(divEntityItemHtml, taxEstimatorHtml);
		}
		contents = contents.replaceFirst("(?is)<div[^>]+id=\"est_buttons\"[^>]*>.*?</div>", "");
		
		return contents;
	}

	public String getPID(String details, String text, String regex) {
		String pid = "";
		try {
			HtmlParser3 htmlParser = new HtmlParser3(details);
			pid = ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.getPidLike(htmlParser.getNodeList(), text, regex);
		} catch (Exception e) {
			logger.error("Cannot parse " + text + " for " + getDataSite().getName(), e);
		}
		return org.apache.commons.lang.StringUtils.strip(pid);
	}

	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			if (table.contains("Owner Name")) {
				table = table.replaceAll("(?is)<a[^>]*>\\s*<img[^>]*>\\s*</a>", "");
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser
						.createParser(table, null);
				NodeList mainTableList = htmlParser.parse(null);
				NodeList tableList = mainTableList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true);
				if (tableList.size() > 0) {
					TableTag mainTable = (TableTag) tableList.elementAt(0);

					TableRow[] rows = mainTable.getRows();
					String[] headers = null;
					String crtCounty = this.county;
					
					for (TableRow row : rows) {
						if (row.getColumnCount() > 1) {

							TableColumn[] cols = row.getColumns();
							NodeList aList = cols[0].getChildren()
									.extractAllNodesThatMatch(
											new TagNameFilter("a"), true);
							if (aList.size() > 0) {
								String link = CreatePartialLink(TSConnectionURL.idGET)
										+ "/texas/"
										+ crtCounty
										+ "/"
										+ ((LinkTag) aList.elementAt(0))
												.extractLink();
								String rowHtml = row.toHtml();
								rowHtml = rowHtml.replaceAll("<a[^>]*>", "<a href=\"" + link + "\">");
								
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(
										ParsedResponse.SERVER_ROW_RESPONSE,
										rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(
										link, link,
										TSServer.REQUEST_SAVE_TO_TSD));

								ResultMap m = parseIntermediaryRow(row, headers, searchId);
								m.removeTempDef();
								Bridge bridge = new Bridge(currentResponse, m,
										searchId);

								DocumentI document = (AssessorDocumentI) bridge
										.importData();
								currentResponse.setDocument(document);

								intermediaryResponse.add(currentResponse);
							} else {
								if(headers == null) {
									headers = new String[cols.length];
									for (int i = 0; i < cols.length; i++) {
										headers[i] = cols[i].toPlainTextString().trim();
									}
								}
							}
						} else if(row.getHeaderCount() > 0) {
							headers = new String[row.getHeaderCount()];
							TableHeader[] tableHeaders = row.getHeaders();
							for (int i = 0; i < tableHeaders.length; i++) {
								headers[i] = tableHeaders[i].toPlainTextString().trim();
							}
						}
					}
					String footer = proccessLinks(response, crtCounty) + "<br><br><br>";
					response.getParsedResponse().setHeader(
							table.substring(table.indexOf("<table"), table.indexOf(">") + 1) + rows[0].toHtml());
					response.getParsedResponse().setFooter("</table><br><br>" + footer);

					outputTable.append(table);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	protected ResultMap parseIntermediaryRow(TableRow row, String[] headers, long searchId)
			throws Exception {

		// atascosa like
		if (ATASCOSA_121_LIST.contains(county) || BAILEY_2_LIST.contains(county) || county.equals("nueces"))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXAtascosaAO(row, headers, county, searchId);
		else if (ARMSTRONG_58_LIST.contains(county))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXArmstrongAO(row, county, searchId);
		else if (ARANSAS_15_LIST.contains(county))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXAransasAO(row, county, searchId);
		else if (ARCHER_19_LIST.contains(county))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXArcherAO(row, county, searchId);
		else if (BOWIE_12_LIST.contains(county))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXBowieAO(row, county, searchId);
		else if (ANDERSON_11_LIST.contains(county) || county.equals("ochiltree") ||  county.equals("tarrant") )
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXAndersonAO(row, county, searchId);
		else if (BRISCOE_5_LIST.contains(county))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXBriscoeAO(row, county, searchId);
		else if (DALLAS_3_LIST.contains(county))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXDallasAO(row, county, searchId);
		else if (POTTER_5_LIST.contains(county))
			return ro.cst.tsearch.servers.functions.TXGenericTaxNetAO
					.parseIntermediaryRowTXPotterAO(row, county, searchId);
		else
			return null;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.parseAndFillResultMap(detailsHtml, map, county, searchId);
		return null;
	}

	private String proccessLinks(ServerResponse response, String crtCounty) {
		String nextLink = "", prevLink = "";
		String footer = "";

		try {
			// String qry = response.getQuerry();
			String rsResponse = response.getResult();
			Matcher priorMat = PREV_PAT.matcher(rsResponse);
			if (priorMat.find()) {
				prevLink = CreatePartialLink(TSConnectionURL.idGET) + "/texas/"
						+ crtCounty + "/" + priorMat.group(1);
			}

			Matcher nextMat = NEXT_PAT.matcher(rsResponse);
			if (nextMat.find()) {
				nextLink = CreatePartialLink(TSConnectionURL.idGET) + "/texas/"
						+ crtCounty + "/" + nextMat.group(1);
			}

			if (StringUtils.isNotEmpty(prevLink)) {
				footer = "&nbsp;&nbsp;&nbsp;<a href=\"" + prevLink
						+ "\">Prev</a>&nbsp;&nbsp;&nbsp;";
			}
			if (StringUtils.isNotEmpty(nextLink)) {
				footer += "&nbsp;&nbsp;&nbsp;<a href=\"" + nextLink
						+ "\">Next</a>";

				response.getParsedResponse().setNextLink(
						"<a href='" + nextLink + "'>Next</a>");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return footer;
	}

	/**
	 * get file name from link
	 */
	@Override
	protected String getFileNameFromLink(String link) {
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if (dummyMatcher.find()) {
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
		return fileName;
	}

}