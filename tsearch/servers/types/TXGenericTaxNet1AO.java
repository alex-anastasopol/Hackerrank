package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * used for counties which have only one of PIDN and (Long) Account Number
 * 
 * @author Oprina George
 * 
 *         Jun 28, 2011
 */

public class TXGenericTaxNet1AO extends TXGenericTaxNetAO {
	// see -> ro.cst.tsearch.servers.functions.TXGenericTaxNetAO

	// used as parent site for aransas like (aransas_17)
	// used as parent site for archer like (ARCHER_20)
	// used as parent site for briscoe like (BRISCOE_6)

	private static final long serialVersionUID = -1990534660814006953L;

	public TXGenericTaxNet1AO(long searchId) {
		super(searchId);
	}

	public TXGenericTaxNet1AO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.NAME_MODULE_IDX);

		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
				HashCountyToIndex.ANY_COMMUNITY, miServerID);
		String county = dataSite.getCountyName();

		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager
				.getInstance();
		String siteName = StateCountyManager.getInstance().getSTCounty(
				dataSite.getCountyId())
				+ dataSite.getSiteType();
		String countyId = getDataSite().getCountyIdAsString();

		if (tsServerInfoModule != null) {
			PageZone pageZone = (PageZone) tsServerInfoModule
					.getModuleParentSiteLayout();

			boolean aransas = ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ARANSAS_15_LIST
					.contains(county.toLowerCase().replace(" ", ""));

			boolean briscoe = ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.BRISCOE_5_LIST
					.contains(county.toLowerCase().replace(" ", ""));

			boolean tarrant1 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.TARRANT_1_LIST.contains(countyId);
			
			boolean dallas1 = ro.cst.tsearch.connection.http3.TXGenericTaxNetAOSite.DALLAS_1_LIST.contains(countyId);

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

				if ("Owner Name".equals(functionName)) {
					if ("Briscoe".equals(county)) {
						htmlControl.setFieldNote("(e.g. Smith Jeff)");
					} else if ("Jim Wells".equals(county)) {
						htmlControl.setFieldNote("(e.g. Smith Jack)");
					} else if ("Ochiltree".equals(county)) {
						htmlControl.setFieldNote("(e.g. Smith James)");
					} else if ("Grimes".equals(county) || "Aransas".equals(county) || "Tarrant".equals(county)) {
						htmlControl.setFieldNote("(e.g. Smith, John)");
					} else {
						htmlControl.setFieldNote("(e.g. Smith John)");
					} 
				}

				if (/*county.equalsIgnoreCase("POTTER")
						||*/ county.equalsIgnoreCase("RANDALL")
						|| county.equalsIgnoreCase("Zavalla")) {
					if ("PIDN".equals(functionName)) {
						htmlControl.setLabel("Property ID");
					}
				}

				if (tarrant1 || dallas1) {
					if ("PIDN".equals(functionName)) {
						htmlControl.setLabel("Long Account Number");
					}
				}
				
				if (county.equalsIgnoreCase("JEFFERSON")) {
					if ("PIDN".equals(functionName)) {
						htmlControl.setLabel("Long Account Number");
					}
				}
				
				if (county.equals("Ochiltree")) {
					if ("Long Account Number".equals(functionName)) {
						htmlControl.setLabel("Account Number");
					}
				}

				if (briscoe) {
					if ("PIDN".equals(functionName)) {
						htmlControl.setLabel("Account No.");
					}
				}

				if (aransas) {
					if ("PIDN".equals(functionName)) {
						if ("Aransas".equals(county)) {
							htmlControl.setFieldNote("(e.g. R20324)");
						} else if ("Bastrop".equals(county)) {
							htmlControl.setFieldNote("(e.g. R39735)");
						} else if ("Fort Bend".equals(county)) {
							htmlControl.setFieldNote("(e.g. R131604)");
						} else if ("Galveston".equals(county)) {
							htmlControl.setFieldNote("(e.g. R381590)");
						} else if ("Grimes".equals(county)) {
							htmlControl.setFieldNote("(e.g. R13104)");
						} else if ("Hays".equals(county)) {
							htmlControl.setFieldNote("(e.g. R87206)");
						} else if ("Jackson".equals(county)) {
							htmlControl.setFieldNote("(e.g. R23376)");
						} else if ("Limestone".equals(county)) {
							htmlControl.setFieldNote("(e.g. R10799)");
						} else if ("Lubbock".equals(county)) {
							htmlControl.setFieldNote("(e.g. R146471)");
						} else if ("Medina".equals(county)) {
							htmlControl.setFieldNote("(e.g. R15612)");
						} else if ("Montgomery".equals(county)) {
							htmlControl.setFieldNote("(e.g. R70433)");
						} else if ("Newton".equals(county)) {
							htmlControl.setFieldNote("(e.g. R11355)");
						} else if ("Orange".equals(county)) {
							htmlControl.setFieldNote("(e.g. R15368)");
						} else if ("San Patricio".equals(county)) {
							htmlControl.setFieldNote("(e.g. R36697)");
						} else if ("Washington".equals(county)) {
							htmlControl.setFieldNote("(e.g. R21180)");
						} else if ("Williamson".equals(county)) {
							htmlControl.setFieldNote("(e.g. R042779)");
						}
					}
					if ("Street No.".equals(functionName)) {
						if ("Aransas".equals(county)) {
							htmlControl.setFieldNote("(e.g. 2116)");
						} else if ("Bastrop".equals(county)) {
							htmlControl.setFieldNote("(e.g. 111)");
						} else if ("Fort Bend".equals(county)) {
							htmlControl.setFieldNote("(e.g. 33211)");
						} else if ("Galveston".equals(county)) {
							htmlControl.setFieldNote("(e.g. 18811)");
						} else if ("Grimes".equals(county)) {
							htmlControl.setFieldNote("(e.g. 6899)");
						} else if ("Hays".equals(county)) {
							htmlControl.setFieldNote("(e.g. 230)");
						} else if ("Jackson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 510)");
						} else if ("Limestone".equals(county)) {
							htmlControl.setFieldNote("(e.g. 515)");
						} else if ("Lubbock".equals(county)) {
							htmlControl.setFieldNote("(e.g. 303)");
						} else if ("Medina".equals(county)) {
							htmlControl.setFieldNote("(e.g. 609)");
						} else if ("Montgomery".equals(county)) {
							htmlControl.setFieldNote("(e.g. 8829)");
						} else if ("Newton".equals(county)) {
							htmlControl.setFieldNote("(e.g. 613)");
						} else if ("Orange".equals(county)) {
							htmlControl.setFieldNote("(e.g. 3531)");
						} else if ("San Patricio".equals(county)) {
							htmlControl.setFieldNote("(e.g. 01105)");
						} else if ("Washington".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1303)");
						} else if ("Williamson".equals(county)) {
							htmlControl.setFieldNote("(e.g. 1208)");
						}
					}
					if ("Street Name".equals(functionName)) {
						if ("Aransas".equals(county)) {
							htmlControl.setFieldNote("(e.g. LAKEVIEW)");
						} else if ("Bastrop".equals(county)) {
							htmlControl.setFieldNote("(e.g. CLEARVIEW)");
						} else if ("Fort Bend".equals(county)) {
							htmlControl.setFieldNote("(e.g. Forest)");
						} else if ("Galveston".equals(county)) {
							htmlControl.setFieldNote("(e.g. DUNBAR)");
						} else if ("Grimes".equals(county)) {
							htmlControl.setFieldNote("(e.g. BAILEY)");
						} else if ("Hays".equals(county)) {
							htmlControl.setFieldNote("(e.g. MASONWOOD)");
						} else if ("Jackson".equals(county)) {
							htmlControl.setFieldNote("(e.g. SUZANNE)");
						} else if ("Limestone".equals(county)) {
							htmlControl.setFieldNote("(e.g. LEON)");
						} else if ("Lubbock".equals(county)) {
							htmlControl.setFieldNote("(e.g. RAIDER)");
						} else if ("Medina".equals(county)) {
							htmlControl.setFieldNote("(e.g. CURTIS)");
						} else if ("Montgomery".equals(county)) {
							htmlControl.setFieldNote("(e.g. BUFFALO)");
						} else if ("Newton".equals(county)) {
							htmlControl.setFieldNote("(e.g. NEWTON)");
						} else if ("Orange".equals(county)) {
							htmlControl.setFieldNote("(e.g. PARK)");
						} else if ("San Patricio".equals(county)) {
							htmlControl.setFieldNote("(e.g. LAREDO)");
						} else if ("Washington".equals(county)) {
							htmlControl.setFieldNote("(e.g. BRIDGE)");
						} else if ("Williamson".equals(county)) {
							htmlControl.setFieldNote("(e.g. COLLEGE)");
						}
					}
				}
				
				if ("nueces".equalsIgnoreCase(county)) {
					if ("PIDN".equals(functionName)) {
						htmlControl.setFieldNote("(e.g. 271758)");
					} else if ("Street No.".equals(functionName)) {
						htmlControl.setFieldNote("(e.g. 438)");
					} else if ("Street Name".equals(functionName)) {
						htmlControl.setFieldNote("(e.g. Anchor)");
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

		if (hasPin()) {
			// Search by PIN
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.LD_PARCELNO_GENERIC_AO);
			modules.add(module);
		}

		if (hasStreet()) {
			// Search by Property Address
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			String crtCounty = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCurrentCounty().getName();
			if (crtCounty.equalsIgnoreCase("tom green")) {
				String streetNo = getSearchAttribute(SearchAttributes.P_STREETNO);
				module.forceValue(2, "%" + streetNo);
			} else
				module.setSaKey(2, SearchAttributes.P_STREETNO);
			module.setSaKey(3, SearchAttributes.P_STREETNAME);
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
							new String[] { "L F;;"}));
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
