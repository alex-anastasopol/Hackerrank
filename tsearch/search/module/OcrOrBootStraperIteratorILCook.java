package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.ExactNameFilter;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct.StructBookPage;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.dip.bean.CallDipResult;

/**
 * OcrIterator searches in the transfer list from tsr index for the last transfer
 * Gets the last transfer image, parses it using OCR and then searches on RO with all found book-pages
 * Also searches on next sites with Grantor/Grantee from the last transfer if the last transfer is Quit Claim 
 */
public class OcrOrBootStraperIteratorILCook extends OcrOrBootStraperIterator {
    
    private static final long serialVersionUID = 7773839502406621821L;
	private static final Category logger = Logger.getLogger(InstrumentModuleStatesIteratorSearch.class);
	
	private static ArrayList<String> IL_COOK_QUIT_CLAIM_SUBCATEGORIES = new ArrayList<String>();
    static {
    	IL_COOK_QUIT_CLAIM_SUBCATEGORIES.add("Quit Claim Deed");
    	IL_COOK_QUIT_CLAIM_SUBCATEGORIES.add("Quit Claim By Entirety");
    	IL_COOK_QUIT_CLAIM_SUBCATEGORIES.add("QuitClaim Deed");
    	IL_COOK_QUIT_CLAIM_SUBCATEGORIES.add("QuitClaim Deed Joint Tenancy");
    }
        
    public OcrOrBootStraperIteratorILCook(long searchId)
    {
        super(searchId);
    }

    public OcrOrBootStraperIteratorILCook(boolean removeTrailingZeroes, long searchId)
    {
        super(removeTrailingZeroes, searchId);
    }
    
    @Override
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
	      				if(page.contains("-"))
	      					rangeNotExpanded = true;
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
            	if(  lastRealTransfer!=null ){
	 	           PartyI vec= lastRealTransfer.getGrantee();
	 	           if( vec!=null ){
	 	        	   for (NameI name : vec.getNames()) {
		        		   if( !StringUtils.isEmpty(name.getLastName()) ){
		        			   name.setMiddleName(NameCleaner.processMiddleName(name.getMiddleName()));
		        			   name.setSufix(NameCleaner.processNameSuffix(name.getSufix()));
		        			   if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name,0.90)){
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
	 		   	    	    			sa.getOwners().add(name);
	 		        			   }
	 		        		   }
	 	 	        	   }	       
	 	 	           }
	 	 	           //Task 6903
	 	 	           if ( isQuitClaim(lastTransferNotWill) ) {
	 	 	        	 Search search = getSearch();
	 	 	        	 String crtServer = search.getCrtServerName(false);
	 	 	        	 if (crtServer.equals("ILCookLA")) {
	 	 	        		PartyI vectgtor= lastTransferNotWill.getGrantor();
			 	 	        if( vectgtor!=null ){
			 	 	           for (NameI name : vectgtor.getNames()) {
			 		        	   if( !StringUtils.isEmpty(name.getLastName()) ){
			 		        		   name.setMiddleName(NameCleaner.processMiddleName(name.getMiddleName()));
				        			   name.setSufix(NameCleaner.processNameSuffix(name.getSufix()));
			 		        		   if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name,0.90)){
			 		   	        			sa.getOwners().add(name);
			 		        		   }
			 		        	   }
			 	 	           }	       
			 	 	        } 
	 	 	        	 }
	 	 	           }
	            	}
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
	      				if(page.contains("-"))
	      					rangeNotExpanded = true;
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
    
    public boolean isQuitClaim(TransferI transfer) {
    	return IL_COOK_QUIT_CLAIM_SUBCATEGORIES.contains(transfer.getDocSubType());
    }
}
