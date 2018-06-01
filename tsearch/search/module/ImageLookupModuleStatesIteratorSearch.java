package ro.cst.tsearch.search.module;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;

import com.stewart.ats.base.document.DocumentI;
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
import ro.cst.tsearch.titledocument.abstracts.Chapter;
import ro.cst.tsearch.titledocument.abstracts.FormatSa;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * BookAndPageModuleStatesIteratorSearch
 * 
 */
public class ImageLookupModuleStatesIteratorSearch extends ModuleStatesIterator {

    private static final Category logger = Category.getInstance(BookAndPageModuleStatesIteratorSearch.class.getName());

    private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX
            + BookAndPageModuleStatesIteratorSearch.class.getName());

    /**
     * Instrument number list.
     */
    private List instrList = new ArrayList();

    /**
     * Default constructor.
     */
    public ImageLookupModuleStatesIteratorSearch(long searchId) {
        super(searchId);
    }

    protected void initInitialState(TSServerInfoModule initial) {
        super.initInitialState(initial);
        SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
        instrList =( new BookAndPageModuleStatesIteratorSearch(searchId)).extractBookPageList(sa.getAtribute(initial.getSaObjKey()));
    }

    protected void setupStrategy() {
        StatesIterator si;
        
        //eliminate from the list the documents already downloaded
        
        List newList = new ArrayList();
        
        Iterator listIter = instrList.iterator();
        while( listIter.hasNext() )
        {
            Instrument instr = (Instrument) listIter.next();
            
            String book = instr.getBookNo();
            String page = instr.getPageNo();
            
            if( !alreadyDownloadedChapter( book, page ) )
            {
                newList.add( instr );
            }
        }
        
        instrList = newList;
        
        si = new DefaultStatesIterator(instrList);
        setStrategy(si);
    }

    public Object current() {
        Instrument instr = ((Instrument) getStrategy().current());
        TSServerInfoModule crtState = new TSServerInfoModule(initialState);

        Search ss;
        ss = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
        SearchAttributes sa = ss.getSa();

        for (int i = 0; i < crtState.getFunctionCount(); i++) {
            TSServerInfoFunction fct = crtState.getFunction(i);
            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE) {
                String bookNo = instr.getBookNo().matches("0+$") ? "" : instr.getBookNo();
                fct.setParamValue(bookNo);
            }
            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE) {
                String pageNo = instr.getPageNo().matches("0+$") ? "" : instr.getPageNo();
                fct.setParamValue(pageNo);
            }
        }
        return crtState;
    }
    
    private boolean alreadyDownloadedChapter( String book, String page )
    {
    	 Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
         
    	 DocumentsManagerI manager = currentSearch.getDocManager();
         
         try{
         	manager.getAccess();
         	Collection<DocumentI> allChapters =  manager.getDocumentsList( true );
 	        for( DocumentI doc:allChapters){
 	            
 	            if( doc.getBook().equals( book ) && doc.getPage().equals( page ) )
 	            {
 	                //instrument found... test if fake document
 	                if( StringUtils.isEmpty(doc.getGrantorFreeForm()+ doc.getGranteeFreeForm()))
 	                {
 	                    //fake
 	                    return false;
 	                }
 	                else
 	                {
 	                    return true;
 	                }
 	            }
 	        }
         }
         finally{
        	 manager.releaseAccess();
         }
        
        return false;
    }
    
    public static void replaceFakeDocument( String book, String page,long searchId )
    {
        Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
        
        synchronized(s)
        {
        	 DocumentsManagerI manager = s.getDocManager();
             
             try{
             	manager.getAccess();
             	Collection<DocumentI> allChapters =  manager.getDocumentsList( true );
             	DocumentI doc1 = null;
     	        for( DocumentI doc:allChapters){
	                
	                if( doc.getBook().equals( book ) && doc.getPage().equals( page ) )
	                {
	                    //instrument found... test if fake document
	                    if( StringUtils.isEmpty(doc.getGrantorFreeForm()+ doc.getGranteeFreeForm()) )
	                    {
	                        doc1=doc;
	                        break;
	                    }
	                }
     	        }
     	        
     	       if( doc1!=null ){
     	    	  manager.remove(doc1);
               }
             }
             finally{
            	 manager.releaseAccess();
             }
        }
    }
}