package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Category;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.TSDIndexPage;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.ParsedResponseData;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.titledocument.abstracts.FormatSa;
import ro.cst.tsearch.titledocument.abstracts.RegisterDocumentsInfo;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

public class DeedBookPageIterator extends ModuleStatesIterator {

    private static final long serialVersionUID = -5818733701509304522L;

    private static final Category logger = Category.getInstance(BookAndPageModuleStatesIteratorSearch.class.getName());

    /**
     * Instrument number list.
     */
    private List instrList = new ArrayList();

    /**
     * Default constructor.
     */
    public DeedBookPageIterator(long searchId) {
        super(searchId);
    }

    protected void initInitialState(TSServerInfoModule initial) {
        super.initInitialState(initial);
        instrList = extractBookPageList();
    }

    protected void setupStrategy() {
        StatesIterator si;
        si = new DefaultStatesIterator(instrList);
        setStrategy(si);
    }

    public Object current() {
        Instrument instr = ((Instrument) getStrategy().current());
        TSServerInfoModule crtState = new TSServerInfoModule(initialState);

        Search ss;
        ss = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
        SearchAttributes sa = ss.getSa();
        String cntyName = FormatSa.getPCountyName(sa);

        for (int i = 0; i < crtState.getFunctionCount(); i++) {
            TSServerInfoFunction fct = crtState.getFunction(i);
            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH) {
                String bookNo = instr.getBookNo();
                fct.setParamValue(bookNo);
            }
            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH) {
                String pageNo = instr.getPageNo();
                fct.setParamValue(pageNo);
            }
        }
        return crtState;
    }

    public  List extractBookPageList() {
        
        Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
        
        List instrList = new ArrayList();
        
        DocumentsManagerI manager = currentSearch.getDocManager();
        
        try{
        	manager.getAccess();
        	Collection<DocumentI> allChapters =  manager.getDocumentsList( true );
	        for( DocumentI doc:allChapters)
	        {
	            
	            if (DocumentTypes.checkDocumentType(doc.getDocType(), DocumentTypes.TAX_TYPE, DocumentTypes.CITY_TAX_SUBTYPE,searchId) ||
	                    DocumentTypes.checkDocumentType(doc.getDocType(), DocumentTypes.TAX_TYPE, DocumentTypes.COUNTY_TAX_SUBTYPE,searchId) ||
	                    DocumentTypes.checkDocumentType(doc.getDocType(), DocumentTypes.TAX_TYPE, DocumentTypes.ASSESSOR_TAX_SUBTYPE,searchId) )
	            {
	                continue;
	            }
	            
	            
	            Set<InstrumentI> references = doc.getParsedReferences();
	            if(references  == null || references.size()==0){
	            	continue;
	            }
	            
	            for( InstrumentI instr:references )
	            {
	                String book = instr.getBook();
	                String page = instr.getPage();
	                if( !"".equals( book ) && !"".equals( page ) )
	                {
	                    Instrument crtInst = new Instrument();
	                    crtInst.setBookNo( book );
	                    crtInst.setPageNo( page );
	                    instrList.add(crtInst);
	                }
	            }
	        }
        }
        finally{
        	manager.releaseAccess();
        }
    
        return instrList;
    }
}