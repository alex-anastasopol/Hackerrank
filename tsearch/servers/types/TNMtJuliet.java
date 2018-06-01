package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * Implementation for TN Mt. Juliet (TN Wilson YC)
 */

public class TNMtJuliet extends TNGenericCityCT {
	
	private static final long serialVersionUID = -1722396832459461042L;

	public TNMtJuliet(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}
	
	protected String getSpecificCntySrvName() {
		return super.getSpecificCntySrvName().replaceFirst("(?i)^m(oun)?t\\.?( )?juliet$", "cityofmtjuliet");
	}
	
	@Override
	public ResultMap parseIntermediary(TableRow row, long searchId)
			throws Exception {
		ResultMap resultMap = super.parseIntermediary(row, searchId);
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "YC");
		resultMap.removeTempDef();
		return resultMap;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		boolean emptyPid = "".equals(sa.getAtribute(SearchAttributes.LD_PARCELNO));
		
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		city = city.replaceFirst("(?i)^M(oun)?t\\.?( )?Juliet$", "MT. JULIET");
		if(!StringUtils.isEmpty(city)){
			if(!city.startsWith(getCityName())){
				return;
			}			
		}
		
		String streetDir = sa.getAtribute(SearchAttributes.P_STREETDIRECTION);
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String streetSuffix = sa.getAtribute(SearchAttributes.P_STREETSUFIX);
		if (StringUtils.isNotEmpty(streetDir)) {
			streetName = streetDir + " " + streetName;
		}
		if (StringUtils.isNotEmpty(streetSuffix)) {
			streetSuffix = Normalize.translateSuffix(streetSuffix.toUpperCase());
			streetName = streetName + " " + streetSuffix;
		}
		if (StringUtils.isNotEmpty(streetNo)) {
			streetName = streetName + " " + streetNo;
		}
		boolean emptySubdiv = "".equals(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		boolean emptySubdivBlk = "".equals(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
		boolean emptySubdivLot = "".equals(sa.getAtribute(SearchAttributes.LD_LOTNO));
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

		TSServerInfoModule m;
		TaxYearFilterResponse yearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);

		//parcel
		if (!emptyPid) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			String pid = sa.getAtribute(SearchAttributes.LD_PARCELNO);
			pid = pid.replaceAll(" ", "");

			//map group parcel si
			String[] parts = { "", "", "", "" };

			//PID from AO, with group, e.g. 050N-F-050N-004.00--000
			if (pid.length() == 23) {	
				parts[0] = pid.substring(0, 4);
				parts[1] = pid.substring(5, 6);
				parts[2] = pid.substring(12, 18);
				parts[3] = pid.substring(20);
			}

			//PID from AO, without group, e.g. 074--074-001.02--009
			if (pid.length() == 20) {
				parts[0] = pid.substring(0, 3);
				parts[2] = pid.substring(9, 15);
				parts[3] = pid.substring(17);
			}
			
			//PID from TR, with group, e.g. 050NF00400000
			if (pid.length() == 13) {	
				parts[0] = pid.substring(0, 4);
				parts[1] = pid.substring(4, 5);
				parts[2] = pid.substring(5, 10);
				parts[3] = pid.substring(10);
			}

			//PID from TR, without group, e.g. 07400102009
			if (pid.length() == 11) {
				parts[0] = pid.substring(0, 3);
				parts[2] = pid.substring(3, 8);
				parts[3] = pid.substring(8);
			}
			
			String map = parts[0];
			String group = parts[1];
			String parcel = parts[2];
			String si = parts[3];

			if (map.matches("000"))
				map = "";
			if (parcel.matches("00000"))
				parcel = "";
			if (si.matches("000"))
				si = "";

			m.getFunction(4).setData(map);
			sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP, map);

			m.getFunction(5).setData(group);
			sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP, group);

			parcel = parcel.replaceAll("\\.", "");
			m.getFunction(6).setData(parcel);
			sa.setAtribute(SearchAttributes.LD_PARCELNO_PARCEL, parcel);

			m.getFunction(7).setData(si);
			m.addFilter(yearFilter);

			l.add(m);
		}

		//address
		if (hasStreet()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.forceValue(1, streetName); 

			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,	searchId, m);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(yearFilter);
			l.add(m);

		}

		//subdivision
		if (!emptySubdiv || !emptySubdivBlk || !emptySubdivLot) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(8).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
			m.getFunction(9).setSaKey(SearchAttributes.LD_SUBDIV_BLOCK);
			m.getFunction(10).setSaKey(SearchAttributes.LD_LOTNO);
			
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(yearFilter);
			l.add(m);
		}

		//owner
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {"L F M;;", "L F;;", "L f;;" });
			m.addIterator(nameIterator);
			m.addFilter(yearFilter);
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

}
