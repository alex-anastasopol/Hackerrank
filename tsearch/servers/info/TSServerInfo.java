package ro.cst.tsearch.servers.info;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.SearchDataWrapper;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author catalin
 */ 
public class TSServerInfo implements Serializable, Cloneable {

	protected static final Logger logger = Logger.getLogger(TSServerInfo.class);
	
    static final long serialVersionUID = 10000000;

    private String msServerAddress = "", msServerIP = "0.0.0.0", msServerLink;

    private int miModuleCount = 0;

    transient private Map<Integer, TSServerInfoModule> maModules;

    protected int filterType = FilterResponse.TYPE_DEFAULT;

    transient private List<TSServerInfoModule> modulesForAutoSearch = null;

    transient private List<TSServerInfoModule> modulesForGoBackOneLevelSearch = new ArrayList<>();

    public static final int NAME_MODULE_IDX = 0;

    public static final int ADDRESS_MODULE_IDX = 1;

    public static final int PARCEL_ID_MODULE_IDX = 2;

    public static final int BOOK_AND_PAGE_MODULE_IDX = 3;

    public static final int INSTR_NO_MODULE_IDX = 4;

    public static final int BGN_END_DATE_MODULE_IDX = 5;

    public static final int TYPE_NAME_MODULE_IDX = 6;

    public static final int SUBDIVISION_MODULE_IDX = 7;

    public static final int BOOK_AND_PAGE_LOCAL_MODULE_IDX = 8;

    public static final int SUBDIVISION_PLAT_MODULE_IDX = 9;

    public static final int TAX_BILL_MODULE_IDX = 10;

    public static final int SORT_TYPE_MODULE_IDX = 11;

    public static final int ADV_SEARCH_MODULE_IDX = 12;
    
    public static final int ARCHIVE_DOCS_MODULE_IDX = 13;
    
    public static final int SECOND_ARCHIVE_DOCS_MODULE_IDX = 14;
    
    public static final int PROP_NO_IDX = 15;
    public static final int TAX_BILL_NO_IDX = 16;
    public static final int ADDRESS_MODULE_IDX2 = 17;
    public static final int GENERIC_MODULE_IDX = 18;
    public static final int CONDOMIN_MODULE_IDX = 19;
    public static final int SECTION_LAND_MODULE_IDX = 20;
    public static final int SURVEYS_MODULE_IDX = 21;
    public static final int MO_JACK_AO_PARCEL_STATUS_FILTER = 22;
    public static final int MO_JACK_AO_PARCEL_ORIGIN_FILTER = 23;
    public static final int MO_JACK_AO_PARCEL_TYPE_FILTER = 24;
    
    public static final int BUSINESS_NAME_MODULE_IDX = 25;
    
    public static final int SERIAL_ID_MODULE_IDX = 26;
    public static final int SALES_MODULE_IDX = 27;
    
    public static final int CASE_NAME_MODULE_IDX      = 28;
    public static final int MALE_NAME_MODULE_IDX      = 29;
    public static final int FEMALE_NAME_MODULE_IDX    = 30;
    public static final int LICENSE_DATE_MODULE_IDX   = 31;
    public static final int MOTHER_NAME_MODULE_IDX    = 34;
    public static final int FATHER_NAME_MODULE_IDX    = 35;
    
    
    public static final int RELATED_MODULE_IDX    = 36;
    public static final int FAKE_MODULE_IDX    = 37;
    
    public static final int MODULE_IDX38 = 38;
    public static final int MODULE_IDX39 = 39;
    public static final int MODULE_IDX40 = 40;
    public static final int MODULE_IDX41 = 41;
    public static final int MODULE_IDX42 = 42;
    public static final int MODULE_IDX43 = 43;
    public static final int MODULE_IDX44 = 44;
    public static final int MODULE_IDX45 = 45;
    
    public static final int ARB_MODULE_IDX           = 55;
    public static final int NEXT_LINK_MODULE_IDX		= 56;
    
    public static final int DATABASE_SEARCH_MODULE_IDX		= 57;
    
    public static final int OLD_ACCOUNT_MODULE_IDX = 103;
    
    public static final int DASL_GENERAL_SEARCH_MODULE_IDX = 144;
    
    public static final int IMG_MODULE_IDX = 155;
    
    private static final HashMap<Integer, SearchType> searchTypeByModuleMap = new HashMap<Integer, SearchType>();
    static {
    	searchTypeByModuleMap.put(NAME_MODULE_IDX, SearchType.GI);
    	searchTypeByModuleMap.put(GENERIC_MODULE_IDX, SearchType.GI);
    	searchTypeByModuleMap.put(TYPE_NAME_MODULE_IDX, SearchType.GI);
    	searchTypeByModuleMap.put(ADDRESS_MODULE_IDX, SearchType.AD);
    	searchTypeByModuleMap.put(PARCEL_ID_MODULE_IDX, SearchType.PN);
    	searchTypeByModuleMap.put(PROP_NO_IDX, SearchType.PN);
    	searchTypeByModuleMap.put(BOOK_AND_PAGE_MODULE_IDX, SearchType.IN);
    	searchTypeByModuleMap.put(BOOK_AND_PAGE_LOCAL_MODULE_IDX, SearchType.IN);
    	searchTypeByModuleMap.put(INSTR_NO_MODULE_IDX, SearchType.IN);
    	searchTypeByModuleMap.put(SUBDIVISION_MODULE_IDX, SearchType.LS);
    	searchTypeByModuleMap.put(SUBDIVISION_PLAT_MODULE_IDX, SearchType.LS);
    	searchTypeByModuleMap.put(BUSINESS_NAME_MODULE_IDX, SearchType.GT);
    	searchTypeByModuleMap.put(SECTION_LAND_MODULE_IDX, SearchType.PI);
    	searchTypeByModuleMap.put(ARB_MODULE_IDX, SearchType.PI);
    	searchTypeByModuleMap.put(IMG_MODULE_IDX, SearchType.IM);
    	searchTypeByModuleMap.put(RELATED_MODULE_IDX, SearchType.RS);
    }
    
    /////////////////////////////////////////////////////////////////////////////////
    public TSServerInfo(int iModuleCount) {
        miModuleCount = iModuleCount;
        if (miModuleCount > 0) {
            maModules = new HashMap<>(miModuleCount);
        } else {
        	maModules = new HashMap<>();
        }
    }

    public TSServerInfoModule ActivateModule(int moduleIndex, int FunctionsCount) {
		TSServerInfoModule module = new TSServerInfoModule(FunctionsCount, moduleIndex);
        maModules.put(new Integer(moduleIndex), module);
        return module;
    }

    /**
     * Method getModule.
     * 
     * @param moduleIndex
     * @return TSServerInfoModule...return module of the specified index
     */
    public TSServerInfoModule getModule(int moduleIndex) {
    	TSServerInfoModule  module= getModule(new Integer(moduleIndex));
        return module;
    }

    public TSServerInfoModule getModule(Integer moduleIndex) {
        return (TSServerInfoModule) maModules.get(moduleIndex);
    }

    public ArrayList<Integer> getKeyList(){
    	ArrayList<Integer> allIdx = new ArrayList<>(maModules.keySet());
        return allIdx;
    }
    
    public Map<Integer, TSServerInfoModule> getAllModules(){
    	return maModules;
    }
    
    /*
     * public void setModule(int moduleIndex, TSServerInfoModule module) {
     * maModules.put(new Integer(moduleIndex), module); }
     */

    public void replaceModule(TSServerInfoModule module) {
        int moduleIndex = module.getModuleIdx();
        maModules.put(new Integer(moduleIndex), module);
    }

    public void replaceModulesFromList(List modulesList) {
        for (Iterator iter = modulesList.iterator(); iter.hasNext();) {
            replaceModule((TSServerInfoModule) iter.next());
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    /**
     * Returns the moduleCount.
     * 
     * @return int
     */
    public int getModuleCount() {
        return miModuleCount;
    }

    /**
     * Returns the serverAddress.
     * 
     * @return String
     */
    public String getServerAddress() {
        return msServerAddress;
    }

    /**
     * Returns the serverIP.
     * 
     * @return String
     */
    public String getServerIP() {
        return msServerIP;
    }

    /**
     * Sets the moduleCount.
     * 
     * @param moduleCount
     *            The moduleCount to set
     *  
     */
    public int getFilterType() {
        return filterType;
    }

    public void setFilterType(int filter) {
        this.filterType = filter;
    }

    public TSServerInfoModule getModuleForSearch(int ServerInfoModuleID,
            SearchDataWrapper sd) {
        TSServerInfoModule module = new TSServerInfoModule(this
                .getModule(ServerInfoModuleID));
        module.setData(sd);
        return module;
    }

    /////////////////////////////////////////////////////////////////////////////////
    /**
     * Sets the serverAddress.
     * 
     * @param serverAddress
     *            The serverAddress to set
     */
    public void setServerAddress(String serverAddress) {
        msServerAddress = serverAddress;
    }

    /**
     * Sets the serverIP.
     * 
     * @param serverIP
     *            The serverIP to set
     */
    public void setServerIP(String serverIP) {
        msServerIP = serverIP;
    }

    /**
     * Returns the serverLink.
     * 
     * @return String
     */
    public String getServerLink() {
        return msServerLink;
    }

    /**
     * Sets the serverLink.
     * 
     * @param serverLink
     *            The serverLink to set
     */
    public void setServerLink(String serverLink) {
        msServerLink = serverLink;
    }

    public void setDefaultValue(SearchAttributes sa) {
        for (Iterator<Integer> iter = maModules.keySet().iterator(); iter.hasNext();) {
            Integer idx = (Integer) iter.next();
            getModule(idx).setDefaultValue(sa);
        }
    }

    public Map getFilterModuleParamsForQuery(SearchDataWrapper sd) {
        Map rez = new HashMap();
        for (Iterator<Integer> iter = maModules.keySet().iterator(); iter.hasNext();) {
            TSServerInfoModule moduleTmp = getModule((Integer) iter.next());
            if (moduleTmp.getMouleType() == TSServerInfoModule.idFilterModule) {
                moduleTmp.setData(sd);
                rez.putAll(moduleTmp.getParamsForQuery());
            }
        }
        return rez;
    }

    public int[] getModulesIdxs(int type) {
        List returnIdx = new ArrayList();
        List allIdx = new ArrayList(maModules.keySet());
        Collections.sort(allIdx);
        for (Iterator iter = allIdx.iterator(); iter.hasNext();) {
            Integer idx = (Integer) iter.next();
            if (getModule(idx).getMouleType() == type)
                returnIdx.add(idx);
        }
        //logger.debug(" return index = " + returnIdx);
        int[] modulesIdx = new int[returnIdx.size()];
        int i = 0;
        for (Iterator iter = returnIdx.iterator(); iter.hasNext();) {
            modulesIdx[i++] = ((Integer) iter.next()).intValue();
        }
        return modulesIdx;
    }

    public List<TSServerInfoModule> getModules(int type) {
        List<TSServerInfoModule> rez = new ArrayList<TSServerInfoModule>();
        for (Iterator iter = maModules.keySet().iterator(); iter.hasNext();) {
            TSServerInfoModule module = getModule((Integer) iter.next());
            if (module.getMouleType() == type)
                rez.add(module);
        }
        return rez;
    }

    public void setupParameterAliases() {
    	if(maModules == null ) return;
        for (Iterator iter = maModules.keySet().iterator(); iter.hasNext();) {
            getModule((Integer) iter.next()).setupParameterAliases();
        }
    }

    /**
     * @return
     */
    public List<TSServerInfoModule> getModulesForAutoSearch() {
        if (modulesForAutoSearch == null) {
            modulesForAutoSearch = getDefaultModulesForAutoSearch();
        }
        return modulesForAutoSearch;
    }

    /**
     * @return
     */
    public List<TSServerInfoModule> getDefaultModulesForAutoSearch() {
        /*List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        TSServerInfoModule o = this.getModule(PARCEL_ID_MODULE_IDX);
        if(o != null) {
        	l.add(new TSServerInfoModule(o, false) );
        }
        o = this.getModule(ADDRESS_MODULE_IDX);
        if(o != null) {
        	l.add(new TSServerInfoModule(o, false) );
        }
        o = this.getModule(NAME_MODULE_IDX);
        if(o != null) {
        	l.add(new TSServerInfoModule(o, false) );
        }*/

        return new ArrayList<TSServerInfoModule>();
    }

    /**
     * @param list
     */
    public void setModulesForAutoSearch(List list) {
        modulesForAutoSearch = list;
    }

    /**
     * @return
     */
    public List getModulesForGoBackOneLevelSearch() {
        return modulesForGoBackOneLevelSearch;
    }

    /**
     * @param list
     */
    public void setModulesForGoBackOneLevelSearch(List list) {
        modulesForGoBackOneLevelSearch = list;
    }
    
    public static SearchType getDefaultSearchTypeForModule(int moduleIdx, DocumentI doc) {
    	SearchType searchType;
    	
    	// name search for TR and AO corresponds to GT search type
    	if(moduleIdx == NAME_MODULE_IDX && (doc instanceof TaxDocumentI || doc instanceof AssessorDocumentI)) {
    		searchType = SearchType.GT;
    	} else {
    		searchType = searchTypeByModuleMap.get(moduleIdx);
    	}
    	if(searchType != null) {
    		return searchType;
    	}
    	
    	return SearchType.NA;
    }
    
    public static HashMap<Integer, SearchType> getSearchTypeByModuleMap() {
    	return searchTypeByModuleMap;
    }
    
    public synchronized Object clone() {
        
        try {
            
            TSServerInfo serverInfo = (TSServerInfo) super.clone();
            
            try { serverInfo.msServerAddress = new String(msServerAddress); } catch (Exception ignored) {}
            try { serverInfo.msServerIP = new String(msServerIP); } catch (Exception ignored) {}
            try { serverInfo.msServerLink = new String(msServerLink); } catch (Exception ignored) {}

            serverInfo.miModuleCount = miModuleCount;
            serverInfo.filterType = filterType;

            // TODO: de terminat clonarea obiectelor
            serverInfo.maModules = maModules;
            serverInfo.modulesForAutoSearch = modulesForAutoSearch;
            serverInfo.modulesForGoBackOneLevelSearch = modulesForGoBackOneLevelSearch;
            
            return serverInfo;
            
        } catch (CloneNotSupportedException cnse) {
            throw new InternalError();
        }
    }
}