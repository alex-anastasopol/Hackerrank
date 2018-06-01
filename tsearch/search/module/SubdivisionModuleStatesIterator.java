package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;


public class SubdivisionModuleStatesIterator extends ModuleStatesIterator {
    private static final Category logger = Category.getInstance(InstrumentModuleStatesIteratorSearch.class.getName());
    private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + InstrumentModuleStatesIteratorSearch.class.getName());
    
    private List subdivList = new ArrayList();
    
    public SubdivisionModuleStatesIterator(long searchId)
    {
        super(searchId);
    }

    
    protected void initInitialState(TSServerInfoModule initial){
        super.initInitialState(initial);
        SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
        subdivList = extractSubdivList( sa.getAtribute( SearchAttributes.LD_SUBDIV_NAME ) );
    }

    protected void setupStrategy() {
        StatesIterator si ;
        si = new DefaultStatesIterator(subdivList);
        setStrategy(si);
    }
    
    public Object current(){
        String subdivName = ((String) getStrategy().current());
        TSServerInfoModule crtState = new TSServerInfoModule(initialState);

        for (int i =0; i< crtState.getFunctionCount(); i++){
            TSServerInfoFunction fct = crtState.getFunction(i);
            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_MISSOURI_SBDIV_NAME) {
                fct.setParamValue(subdivName);
            }
        }
        return  crtState ;
    }
    
    private List extractSubdivList( String originalSubdivName )
    {
        List subdivList = new ArrayList();
        String[] tokens = { "1ST", "2ND", "3RD" };
        String[] replacements = { "FIRST", "SECOND", "THIRD" };
        
        if(StringUtils.isStringBlank(originalSubdivName)) {
            return subdivList;
        }

        subdivList.add( originalSubdivName );
        
        
        for( int i = 0 ; i < tokens.length ; i ++ )
        {
            if( (originalSubdivName.toUpperCase()).contains( tokens[i] ) )
            {
                subdivList.add( (originalSubdivName.toUpperCase()).replaceAll( tokens[i], replacements[i] ) );
            }
            else if( (originalSubdivName.toUpperCase()).contains( replacements[i] ) )
            {
                subdivList.add( (originalSubdivName.toUpperCase()).replaceAll( replacements[i], tokens[i] ) );                
            }
        }
        
        return subdivList;
    }
}
