package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonUniqueFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.name.ExactNameFilter;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct.StructBookPage;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.dip.bean.CallDipResult;

/**
 * OcrIterator searches in the transfer list from tsr index for the last transfer
 * Gets the last transfer image, parses it using OCR and then searches on RO with all found book-pages
 */
public class OcrOrBootStraperIterator extends ModuleStatesIterator {
    private static final long serialVersionUID = -339301067085187188L;
    
	private static final Category logger = Logger.getLogger(InstrumentModuleStatesIteratorSearch.class);
        
    protected boolean removeTrailingZeroes = true;
    
    /**
     * Instrument number list.
     */ 
    protected List<Instrument> instrList = new ArrayList<Instrument>();
    
    /**
     * Default constructor.
     */
    @Deprecated
    public OcrOrBootStraperIterator(long searchId)
    {
        super(searchId);
        this.removeTrailingZeroes = true;
    }
    
    /**
	 * @param searchId
	 * @param dataSite
	 */
	public OcrOrBootStraperIterator(long searchId, DataSite dataSite){
		super(searchId);
		setDataSite(dataSite);
		this.removeTrailingZeroes = true;
	}

    public OcrOrBootStraperIterator( boolean removeTrailingZeroes, long searchId )
    {
        super(searchId);
        this.removeTrailingZeroes = removeTrailingZeroes;
    }
    
    @SuppressWarnings("unchecked")
	protected void initInitialState(TSServerInfoModule initial){
        super.initInitialState(initial);
        instrList = extractInstrumentNoList( initial );
        try{
	        List tempList = new ArrayList();
	        String platBook = getSearch().getSa().getAtribute(SearchAttributes.LD_BOOKNO_1);
	        String platPage = getSearch().getSa().getAtribute(SearchAttributes.LD_PAGENO_1);
	        for (Object inst:instrList){
	        	Instrument instrument = (Instrument )inst;

	        	if(instrument.getInstrumentType() == Instrument.TYPE_BOOK_PAGE) {
		        	if(		!(instrument .getBookNo().equals(platBook) &&
		        			instrument.getPageNo().equals(platPage))){
		        		tempList.add(instrument);
		        	}
	        	}
		        else {
		        	tempList.add(instrument);
		        }
	        	
	        }
	        instrList= tempList;
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        
		if(!searchIfPresent){
			instrList = ModuleStatesIterator.removeAlreadyRetrieved(instrList, searchId);
		}
		
    }

    protected void setupStrategy() {
        StatesIterator si ;
        si = new DefaultStatesIterator(instrList);
        setStrategy(si);
    }
    
    public Object current(){
        Instrument instr = ((Instrument) getStrategy().current());
        
        TSServerInfoModule crtState = new TSServerInfoModule(initialState);
        
        List<FilterResponse> allFilters = crtState.getFilterList();
		GenericInstrumentFilter gif = null;
		HashMap<String, String> filterCriteria = null;
		if(allFilters != null) {
			for (FilterResponse filterResponse : allFilters) {
				if (filterResponse instanceof GenericInstrumentFilter) {
					gif = (GenericInstrumentFilter) filterResponse;
					filterCriteria = new HashMap<String, String>();
					gif.clearFilters();
				}
			}
		}
		
		//force add unique filter when no validators are present
		ArrayList<DocsValidator> validatorList = crtState.getValidatorList();
		if(validatorList.isEmpty()) {
			if(allFilters != null) {
				boolean found = false;
				for (FilterResponse filterResponse : allFilters) {
					if (filterResponse instanceof RejectNonUniqueFilterResponse) {
						found = true;
						break;
					}
				}
				if(!found) {
					allFilters.add(new RejectNonUniqueFilterResponse(searchId));
					initFilter(crtState);
				}
			}
		}
        
        for (int i =0; i< crtState.getFunctionCount(); i++){
            TSServerInfoFunction fct = crtState.getFunction(i);
            if( "".equals( instr.getInstrumentNo() ) ){
	            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH) {
	                
	                fct.setParamValue( instr.getBookNo() );
	                if(filterCriteria != null) {
						filterCriteria.put("Book", instr.getBookNo());
					}
	            }
	            else if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH) {
	                fct.setParamValue( instr.getPageNo() );
	                if(filterCriteria != null) {
						filterCriteria.put("Page", instr.getPageNo());
					}
	            }
	            else if(fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_DOCTYPE_SEARCH) {
	            	fct.setParamValue( instr.getRealdoctype() );
	            }
            }
            else{
	            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH) {
	                
	                fct.setParamValue( instr.getInstrumentNo() );
	                if(filterCriteria != null) {
						filterCriteria.put("InstrumentNumber", instr.getInstrumentNo());
					}
	            } else if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_SKLD_INSTRUMENT_FIRST_PART) {
	            	String instrument = instr.getInstrumentNo();
	            	
					if(instrument.matches("\\d+-\\d{4}")) {
						String realInstrument = instrument.replaceAll("-\\d+", "");
						fct.setParamValue( realInstrument );
					}
					
	            	
	            } else if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_SKLD_INSTRUMENT_SECOND_PART) {
	            	String instrument = instr.getInstrumentNo();
	            	
					if(instrument.matches("\\d+-\\d{4}")) {
						String realYear = instrument.replaceAll("\\d+-", "");
						if (!realYear.matches("0{4}")){
							fct.setParamValue( realYear );
						}
					}
	            	
	            }
            }
        }
        if(gif != null) {
			gif.addDocumentCriteria(filterCriteria);
		}
        return  crtState ;
    }
    
    public List<Instrument> extractInstrumentNoList( TSServerInfoModule initial ){
        List<Instrument> instrList = new ArrayList<Instrument>();
        OCRParsedDataStruct ocrRealData = null;
        OCRParsedDataStruct ocrData = null;
        CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
        Search currentSearch = currentInstance.getCrtSearchContext();
        SearchAttributes sa = currentSearch.getSa();
        
        String p1 =currentSearch.getP1();
        String p2 = currentSearch.getP2();
        
        boolean ocrEnable = false;
        boolean nameBootstrapEnabled = false;
        long  serverId = -1;
        try{
        	DataSite dat = HashCountyToIndex.getDataSite(currentInstance.getCommunityId(), Integer.parseInt(p1), Integer.parseInt(p2));
        	ocrEnable = Util.isSiteEnabledOCR(dat.getEnabled(currentInstance.getCommunityId()));
        	nameBootstrapEnabled = Util.isSiteEnabledNameBootstrap(dat.getEnabled(currentInstance.getCommunityId()));
        }
        catch(Exception e){
        	logger.error("Error while checking if OCR is enabled on SearchId: " + searchId + ", siteType = " + p2, e);
        	e.printStackTrace();
        	serverId = -1;
        }
        
        boolean hasBookPageSearch = false;
        boolean hasInstrumentSearch = false;

        if(ocrEnable ){
	        IndividualLogger.info( "OCR enabled, search for last transfer..." ,searchId);
	        
	        for( int j = 0 ; j < initial.getFunctionCount() ; j ++ ){
	        	int functionIteratorType = initial.getFunction( j ).getIteratorType();
	        	switch( functionIteratorType ){
		        	case FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH:
		        	case FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH:
		        		hasBookPageSearch = true;
		        		break;
		        	case FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH:
		        	case FunctionStatesIterator.ITERATOR_TYPE_SKLD_INSTRUMENT_FIRST_PART:
		        		hasInstrumentSearch = true;
		        		break;
	        	}
	        }
	        
	        if ( currentSearch.getOcrData() != null ){
	        	IndividualLogger.info("Utilize data from last OCR", searchId);
	        	ocrRealData = currentSearch.getOcrData();
	        	if( hasBookPageSearch ){            	
	      			Vector<StructBookPage> bookPages = ocrRealData.getBookPageVector();
	      			Iterator<StructBookPage> ocrBookPagesIterator = bookPages.iterator();
	      			while( ocrBookPagesIterator.hasNext() ){
	      				StructBookPage ocrBookPageStruct = (StructBookPage) ocrBookPagesIterator.next();
	      				String page = ocrBookPageStruct.getPage();
	      				boolean rangeNotExpanded = false;
	      				if(page.contains("-") && page.trim().matches("(?is)\\d+-\\d+")){
	      					rangeNotExpanded = true;
	      				}
	      				if(rangeNotExpanded){
	      					page = page.replaceAll("-.*", "");
	      				}
	      				
	      				Instrument newInstrument = new Instrument();
	      				newInstrument.setInstrumentType(Instrument.TYPE_BOOK_PAGE);
	
	      				if( removeTrailingZeroes ){
	      					newInstrument.setBookNo( ocrBookPageStruct.book.replaceAll("^0+(.*)$", "$1") );
	      					newInstrument.setPageNo( page.replaceAll("^0+(.*)$", "$1") );
	      					newInstrument.setRealdoctype( ocrBookPageStruct.getType());
	      				}
	      				else{
	      					newInstrument.setBookNo( ocrBookPageStruct.book );
	      					newInstrument.setPageNo( page ); 
	      					newInstrument.setRealdoctype( ocrBookPageStruct.getType()); 
	      				}
	      				instrList.add( newInstrument );
	      			}
            	}
   			
	   			if( hasInstrumentSearch ){
	      			Vector<Instrument> instruments = ocrRealData.getInstrumentVector();
	      			Iterator<Instrument> ocrInstrumentIterator = instruments.iterator();
	      			while( ocrInstrumentIterator.hasNext() ){
	      				Instrument ocrInstrument = (Instrument) ocrInstrumentIterator.next();
	      				
	      				IndividualLogger.info( "OCR: Adding instrument=" + ocrInstrument.getInstrumentNo() ,searchId);
	      				Instrument newInstrument = new Instrument();
	      				
	      				newInstrument.setInstrumentType(Instrument.TYPE_INSTRUMENT_NO);
	      				
	      				if( removeTrailingZeroes ){
	      					newInstrument.setInstrumentNo( ocrInstrument.getInstrumentNo().replaceAll("^0+(.*)$", "$1") );
	      				}
	      				else {
	      					newInstrument.setInstrumentNo( ocrInstrument.getInstrumentNo() );
	      				}
	      				instrList.add( newInstrument );
	      			}
	   			}
	   			return instrList;
	        }
        }
        DocumentsManagerI docManager = currentSearch.getDocManager();
        TransferI lastRealTransfer = null;
        TransferI lastTransferNotWill = null;
               
        ImageI lastRealTransferImage = null;
        ImageI lastTransferImage = null;
        
        if(ocrEnable) {
        	try{
            	docManager.getAccess();
            	lastRealTransfer = getLastRealTransfer(docManager);
            	lastTransferNotWill = getLastTransferNotWill(docManager);
            	
            	if( lastRealTransfer == null && lastTransferNotWill == null ){
                	return instrList;
                }
            	
            	if(  lastRealTransfer!=null && lastRealTransfer.hasImage()  ){
            		lastRealTransferImage = lastRealTransfer.getImage().clone();
            	}
            	
            	if( lastTransferNotWill!=null && !lastTransferNotWill.equals(lastRealTransfer)  && lastTransferNotWill.hasImage() ){
            		lastTransferImage = lastTransferNotWill.getImage().clone();
            	}
            	
        	} catch( Exception e ) {
            	e.printStackTrace();
            } finally{
            	docManager.releaseAccess();
            }
        }
        
        if(lastRealTransferImage != null) {
        	try {
        		DownloadImageResult downloadImageResult = initial.getTSInterface().downloadImage(lastRealTransfer);
        		if(downloadImageResult == null || downloadImageResult.getStatus() != DownloadImageResult.Status.OK) {
        			lastRealTransferImage = null;
        		} 
        	} catch (Exception e) {
				lastRealTransferImage = null;
			}
        	
        }
        
        if(lastTransferImage != null) {
        	try {
        		DownloadImageResult downloadImageResult = initial.getTSInterface().downloadImage(lastTransferNotWill);
        		if(downloadImageResult == null || downloadImageResult.getStatus() != DownloadImageResult.Status.OK) {
        			lastTransferImage = null;
        		} 
        	} catch (Exception e) {
				lastTransferImage = null;
			}
        }
        
        
        try{
        	docManager.getAccess();
        	lastRealTransfer = getLastRealTransfer(docManager);
        	lastTransferNotWill = getLastTransferNotWill(docManager);
        	
        	if( lastRealTransfer == null && lastTransferNotWill == null ){
            	return instrList;
            }
        	
            if( nameBootstrapEnabled ){
            	
            	StringBuilder infoToBeLogged = new StringBuilder();
            	
            	if(  lastRealTransfer!=null ){
	 	           PartyI vec= lastRealTransfer.getGrantee();
	 	           if( vec!=null ){
	 	        	   for (NameI name : vec.getNames()) {
		        		   if( !StringUtils.isEmpty(name.getLastName()) ){
		        			   name.setMiddleName(NameCleaner.processMiddleName(name.getMiddleName()));
		        			   name.setSufix(NameCleaner.processNameSuffix(name.getSufix()));
		        			   if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name,0.90)){
									name.getNameFlags().addSourceType(new NameSourceType(NameSourceType.DOCUMENT, lastRealTransfer.getId()));
									infoToBeLogged.append("Saving name from Last Real Transfer to Search Page[Owners] ");
									if(name.isCompany()) {
										infoToBeLogged.append("(Company: ").append(name.getLastName()).append(")");
									} else {
										infoToBeLogged.append("(Last: " + name.getLastName() +
			       							", Middle: " + name.getMiddleName() + 
			       							", First: " + name.getFirstName() + 
			       							", Suffix: " + name.getSufix() +
			       							", Prefix: " + name.getPrefix() + ")");
									}
									infoToBeLogged.append("<br>");
		   	    	    			sa.getOwners().add(name);
		        			   }
		        		   }
	 	        	   }	       
	 	           }
            	}
            	//B 3965
            	if(isBootstrapNameLastTransferNotWill()) {
	            	if(  lastTransferNotWill!=null ){
	 	 	           PartyI vect= lastTransferNotWill.getGrantee();
	 	 	           if( vect!=null ){
	 	 	        	   for (NameI name : vect.getNames()) {
	 		        		   if( !StringUtils.isEmpty(name.getLastName()) ){
	 		        			   name.setMiddleName(NameCleaner.processMiddleName(name.getMiddleName()));
			        			   name.setSufix(NameCleaner.processNameSuffix(name.getSufix()));
	 		        			   if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name,0.90)){
										name.getNameFlags().addSourceType(new NameSourceType(NameSourceType.DOCUMENT, lastTransferNotWill.getId()));
										infoToBeLogged.append("Saving name from Last Transfer to Search Page[Owners] ");
										if(name.isCompany()) {
											infoToBeLogged.append("(Company: ").append(name.getLastName()).append(")");
										} else {
											infoToBeLogged.append("(Last: " + name.getLastName() +
				       							", Middle: " + name.getMiddleName() + 
				       							", First: " + name.getFirstName() + 
				       							", Suffix: " + name.getSufix() +
				       							", Prefix: " + name.getPrefix() + ")");
										}
										infoToBeLogged.append("<br>");
	 		   	    	    			sa.getOwners().add(name);
	 		   	    	    			
	 		        			   }
	 		        		   }
	 	 	        	   }	       
	 	 	           }
	            	}
             	}
            	
            	if(infoToBeLogged.length() > 0) {
            		SearchLogger.info(infoToBeLogged.toString(), searchId);
            	}
            	
            }
            
            if(ocrEnable){   
		            if(  lastRealTransfer!=null && lastRealTransfer.hasImage() && !lastRealTransfer.getImage().isOcrDone() && lastRealTransferImage != null){
		                IndividualLogger.info( "OCR: Download and parse last transfer image..." ,searchId);
		                currentSearch.addOcrInProgress();
		                try {
		                	if(lastRealTransfer.getSiteId() != serverId && serverId!=-1) {
		                		//override used for ILCookLA
		                		lastRealTransfer.setSiteId((int)serverId);
		                	}
		                	CallDipResult callDipResult = initial.getTSInterface().ocrDownloadAndScanImageIfNeeded(
		                			lastRealTransfer, "transfer",3,5000, isCheckNameOnLastRealTransfer());
		                	if(callDipResult != null) {
		                		ocrRealData = callDipResult.getOcrData();
		                	}
		                } finally {
		                	currentSearch.removeOcrInProgress();
		                }
		        	 
			            IndividualLogger.info( "OCR End",searchId );
		            }
		            
		        //if(isBootstrapNameLastTransferNotWill()) {
		            if( lastTransferNotWill!=null && lastTransferNotWill.hasImage() && !lastTransferNotWill.equals(lastRealTransfer) 
		            		&& !lastTransferNotWill.getImage().isOcrDone() ){
			            if (lastTransferImage != null){
			                IndividualLogger.info( "OCR: Download and parse last transfer image..." ,searchId);
			                currentSearch.addOcrInProgress();
			                try {
			                	if(lastTransferNotWill.getSiteId() != serverId && serverId!=-1) {
			                		//override used for ILCookLA
			                		lastTransferNotWill.setSiteId((int)serverId);
			                	}
			                	CallDipResult callDipResult = initial.getTSInterface().ocrDownloadAndScanImageIfNeeded(
			                			lastTransferNotWill, "transfer",3,5000, isCheckNameOnLastRealTransfer());
			                	if(callDipResult != null) {
			                		ocrData = callDipResult.getOcrData();
			                	}
			                } finally {
			                	currentSearch.removeOcrInProgress();
			                }
			            	
			        	} 
		            }
		         //}
            }

            //particular processing in TSServer
            if(  lastRealTransfer!=null){
            	initial.getTSInterface().processLastRealTransfer( lastRealTransfer, ocrRealData );
            }
            if( lastTransferNotWill!=null ){
            	initial.getTSInterface().processLastTransfer( lastTransferNotWill, ocrData);
            }
            
            if(ocrRealData!=null){
            	currentSearch.setOcrData(ocrRealData);
            	if( hasBookPageSearch ){            	
	      			Vector<StructBookPage> bookPages = ocrRealData.getBookPageVector();
	      			Iterator<StructBookPage> ocrBookPagesIterator = bookPages.iterator();
	      			while( ocrBookPagesIterator.hasNext() ){
	      				StructBookPage ocrBookPageStruct = (StructBookPage) ocrBookPagesIterator.next();
	      				String page = ocrBookPageStruct.getPage();
	      				boolean rangeNotExpanded = false;
	      				if(page.contains("-") && page.trim().matches("(?is)\\d+-\\d+")){
	      					rangeNotExpanded = true;
	      				}
	      				if(rangeNotExpanded){
	      					page = page.replaceAll("-.*", "");
	      				}
	      				
	      				Instrument newInstrument = new Instrument();
	      				newInstrument.setInstrumentType(Instrument.TYPE_BOOK_PAGE);
	
	      				if( removeTrailingZeroes ){
	      					newInstrument.setBookNo( ocrBookPageStruct.book.replaceAll("^0+(.*)$", "$1") );
	      					newInstrument.setPageNo( page.replaceAll("^0+(.*)$", "$1") );
	      					newInstrument.setRealdoctype( ocrBookPageStruct.getType());
	      				}
	      				else{
	      					newInstrument.setBookNo( ocrBookPageStruct.book );
	      					newInstrument.setPageNo( page ); 
	      					newInstrument.setRealdoctype( ocrBookPageStruct.getType()); 
	      				}
	      				instrList.add( newInstrument );
	      			}
            	}
   			
	   			if( hasInstrumentSearch ){
		      			Vector<Instrument> instruments = ocrRealData.getInstrumentVector();
		      			Iterator<Instrument> ocrInstrumentIterator = instruments.iterator();
		      			while( ocrInstrumentIterator.hasNext() ){
		      				Instrument ocrInstrument = (Instrument) ocrInstrumentIterator.next();
		      				
		      				IndividualLogger.info( "OCR: Adding instrument=" + ocrInstrument.getInstrumentNo() ,searchId);
		      				Instrument newInstrument = new Instrument();
		      				
		      				newInstrument.setInstrumentType(Instrument.TYPE_INSTRUMENT_NO);
		      				
		      				if( removeTrailingZeroes ){
		      					newInstrument.setInstrumentNo( ocrInstrument.getInstrumentNo().replaceAll("^0+(.*)$", "$1") );
		      				}
		      				else {
		      					newInstrument.setInstrumentNo( ocrInstrument.getInstrumentNo() );
		      				}
		      				instrList.add( newInstrument );
		      			}
	   			}
            }
        }
        catch( Exception e ) {
        	e.printStackTrace();
        }
        finally{
        	docManager.releaseAccess();
        }
        return instrList;
    }

	protected boolean isCheckNameOnLastRealTransfer() {
		return false;
	}

	protected boolean isBootstrapNameLastTransferNotWill() {
		return true;
	}

	protected TransferI getLastRealTransfer(DocumentsManagerI docManager) {
		return docManager.getLastRealTransfer();
	}
	
	protected TransferI getLastTransferNotWill(DocumentsManagerI docManager) {
		return docManager.getLastTransferNotWill();
	}
}
