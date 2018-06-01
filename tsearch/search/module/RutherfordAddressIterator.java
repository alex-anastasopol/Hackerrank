package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;

public class RutherfordAddressIterator extends ModuleStatesIterator {

	private static final long serialVersionUID = 8615525734449389054L;
	
    private List streetStringList = new ArrayList();
    
    public RutherfordAddressIterator(long searchId) {
        super(searchId);
    }
    
    protected void initInitialState(TSServerInfoModule initial){
        super.initInitialState(initial);
        streetStringList = buildAddressList( initial );
    }

    protected void setupStrategy() {
        StatesIterator si ;
        si = new DefaultStatesIterator(streetStringList);
        setStrategy(si);
    }
    
    public Object current(){
        String addressStr = ((String) getStrategy().current());
        
        TSServerInfoModule crtState = new TSServerInfoModule(initialState);
        
        for (int i =0; i< crtState.getFunctionCount(); i++){
            TSServerInfoFunction fct = crtState.getFunction(i);
            if( !"".equals( addressStr ) ){
	            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_ADDRESS) {
	            	fct.setParamValue( addressStr );
	            }
            }
        }
        return  crtState ;
    }
    
    private List buildAddressList( TSServerInfoModule initial ) {
        List<String> addressList = new ArrayList<String>();
        Vector<String> suffixes = null;
        
        Search currentSearch = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext();
        SearchAttributes sa = currentSearch.getSa();
        
        String streetName = sa.getAtribute( SearchAttributes.P_STREETNAME );
        String streetNo = sa.getAtribute( SearchAttributes.P_STREETNO );
        String streetSuffix = sa.getAtribute( SearchAttributes.P_STREETSUFIX );
        
        if ( !"".equals( streetSuffix ) ) {
        	suffixes = new Vector<String>();
        	
        	suffixes.add( streetSuffix );
        	
        	HashMap<String, String> normalizeSuffixes = Normalize.getAllSuffixes();
        	
        	Iterator<String> suffixIterator = normalizeSuffixes.keySet().iterator();
        	while (suffixIterator.hasNext()) {
        		String keySuffix = suffixIterator.next();
        		String valSuffix = normalizeSuffixes.get( keySuffix );
        		
        		if( streetSuffix.equals( keySuffix ) || streetSuffix.equals( valSuffix ) || (streetSuffix+"-R").equals( keySuffix ) ) {
        			
        			if( keySuffix.contains( "-R" ) ) {
        				keySuffix = keySuffix.substring( 0 , keySuffix.length() - 2);
        			}
        			
        			if( !suffixes.contains( keySuffix ) ) {
        				suffixes.add( keySuffix );
        			}
        			
        			if( !suffixes.contains( valSuffix ) ) {
        				suffixes.add( valSuffix );
        			}
        		}
        	}
        }
        
        String addressStr = "";
        
        if ( !"".equals( streetName ) || !"".equals( streetNo ) ) {
        	addressStr = (streetName + " " + streetNo).replaceAll( "\\s+" , " ");
        	addressList.add(addressStr.trim());
        }
        
        if (suffixes != null) {
            if ( !"".equals( streetName )) {
            	for (int i = 0 ; i < suffixes.size() ; i++) {
            		addressStr = (streetName + " " + suffixes.elementAt( i ) + " " + streetNo).replaceAll( "\\s+" , " ");
            		addressList.add(addressStr.trim());
            		
            		addressStr = (streetNo + " " + streetName + " " + suffixes.elementAt( i ) ).replaceAll( "\\s+" , " ");
            		addressList.add(addressStr.trim());
            	}
            }
        } else {
            if ( !"".equals( streetName ) || !"".equals( streetNo ) ) {
            	addressStr = (streetNo + " " + streetName).replaceAll( "\\s+" , " ");
            	addressList.add(addressStr.trim());
            }
        }
        
        return addressList;
    }
}