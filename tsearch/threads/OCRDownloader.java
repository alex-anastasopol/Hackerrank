package ro.cst.tsearch.threads;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameFlagsI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameMortgageGrantee;
import com.stewart.ats.base.name.NameMortgageGranteeI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;
import com.stewart.dip.bean.CallDipResult;

public class OCRDownloader extends Thread {
	
	protected static final Category logger = Logger.getLogger(OCRDownloader.class);

	public static final int DEFAULT_FIRST_PAGES = 3;

	public static final int DEFAULT_LAST_PAGES = 0;
	
	private Search search = null;
	private DocumentI documentI = null;
	private OCRParsedDataStruct ocrData = null;
	private boolean manualOcr = false; 
	
	public OCRDownloader( Search search,DocumentI doc ) {
		
		this.search = search;
		this.documentI = doc;
		setName("OCRDownloader - " + search.getID() + " - " + doc.getNiceFullFileName());
	}
	
	public OCRDownloader( long searchId,DocumentI doc ) {
		
		this.search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		this.documentI = doc;
		setName("OCRDownloader - " + searchId  + " - " + doc.getNiceFullFileName());
	}
	
	
	public boolean isManualOcr() {
		return manualOcr;
	}

	public void setManualOcr(boolean manualOcr) {
		this.manualOcr = manualOcr;
	}

	/**
	 * @return the search
	 */
	public Search getSearch() {
		return search;
	}
	/**
	 * @param search the search to set
	 */
	public void setSearch(Search search) {
		this.search = search;
	}
	/**
	 * @return the documentI
	 */
	public DocumentI getDocumentI() {
		return documentI;
	}
	
	@Override
	public void run() {
		
		long ocrRunStartTime = System.currentTimeMillis();
		
		TSInterface tsInterface;
		if(documentI.hasImage() && documentI.getImage().isUploaded()) {
			tsInterface = new TSServer(search.getID());
		}else {
			tsInterface = TSServersFactory.GetServerInstance((int)documentI.getSiteId(), "", "", search.getID());
		}
		
		ImageI image = documentI.getImage();
		try{
			if(image==null || !(documentI instanceof RegisterDocumentI)){
				return;
			}
			if(image.isOcrInProgress() || (!isManualOcr() && image.isOcrDone())){
				return;
			}
			
			try {
				if(!image.isUploaded()) {
					
					DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
							InstanceManager.getManager().getCommunityId(search.getID()),  
							tsInterface.getServerID());
					
		        	if(!isManualOcr() && (!Util.isSiteEnabledOCR(dat.getEnabled(search.getCommId())) )){
		        		logger.info("OCR not enabled on " + dat.getName());
		        		return;
		        	} else {
		        		/*
		        		//ocr enabled and on CA
		        		if(search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)) {
							if(documentI instanceof TransferI) {
								logger.info("OCR not enabled for transfers on California if product type is Refinance");
								return;
							} else if (documentI instanceof MortgageI) {
								MortgageI mortgage = (MortgageI) documentI;
								DocumentsManagerI docManager = search.getDocManager();
								try {
									docManager.getAccess();
									TransferI lastTransfer = docManager.getLastTransfer();
									if(lastTransfer != null) {
										Comparator<DocumentI> dateComparator = DocumentUtils.getDateComparator(true);
										if(dateComparator.compare(mortgage, lastTransfer) < 0) {
											logger.info("OCR not enabled for mortgages before last transfer on California if product type is Refinance");
											return;
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
									docManager.releaseAccess();
								}
							} else {
								logger.info("OCR not enabled for anything else except transfers and mortgages on CA");
				        		return;
							}
						}
						*/
		        	}
		        	
				} 
				/*
				else {
					if(search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)) {
						if(documentI instanceof TransferI) {
							logger.info("OCR not enabled for transfers on California if product type is Refinance");
							return;
						} else if (documentI instanceof MortgageI) {
							MortgageI mortgage = (MortgageI) documentI;
							DocumentsManagerI docManager = search.getDocManager();
							try {
								docManager.getAccess();
								TransferI lastTransfer = docManager.getLastTransfer();
								if(lastTransfer != null) {
									Comparator<DocumentI> dateComparator = DocumentUtils.getDateComparator(true);
									if(dateComparator.compare(mortgage, lastTransfer) < 0) {
										logger.info("OCR not enabled for mortgages before last transfer on California if product type is Refinance");
										return;
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								docManager.releaseAccess();
							}
						} else {
							logger.info("OCR not enabled for anything else except transfers and mortgages on CA");
			        		return;
						}
					}
				}
				*/
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
			try {
				getSearch().addOcrInProgress();
				CallDipResult callDipResult = tsInterface.ocrDownloadAndScanImageIfNeeded(documentI, documentI.getDocType(), isManualOcr(), 3, 5000, false);
				if(callDipResult != null) {
					ocrData = callDipResult.getOcrData();
				}
				if(ocrData == null) {
					Search latestSearchInMemory = InstanceManager.getManager().getCurrentInstance(search.getID()).getCrtSearchContext();
					if(search != latestSearchInMemory) {
						try {
							latestSearchInMemory.getDocManager().getAccess();
							DocumentI latestDocumentInMomory = latestSearchInMemory.getDocManager().getDocument(documentI.getId());
							ImageI latestImageInMomory = latestDocumentInMomory.getImage();
							latestImageInMomory.setOcrDone(false);
							latestImageInMomory.setOcrInProgress(false);
						} finally {
							latestSearchInMemory.getDocManager().releaseAccess();
						}
						
					}
					return;
				} else {
					Search latestSearchInMemory = InstanceManager.getManager().getCurrentInstance(search.getID()).getCrtSearchContext();
					if(search == latestSearchInMemory) {
						Search.saveSearch(search);
						DBManager.zipAndSaveSearchToDB(search);
					} else {
						DocumentsManagerI manager = latestSearchInMemory.getDocManager();
						try {
							manager.getAccess();
							DocumentI latestDocumentInMomory = latestSearchInMemory.getDocManager().getDocument(documentI.getId());
							ImageI latestImageInMomory = latestDocumentInMomory.getImage();
							latestImageInMomory.setOcrDone(true);
							latestImageInMomory.setOcrInProgress(false);
							latestImageInMomory.setPlanedForOCR(false);
							
							if(ocrData != null) {
				    			TransferI lastRealTransfer = manager.getLastRealTransfer();
				    			//update the document Data with what we found from OCR
				    			StringBuilder infoToBeLogged = new StringBuilder();
				    			((TSServer)tsInterface).updateDocumentWithOCRData(manager, latestDocumentInMomory, ocrData, null, latestSearchInMemory, infoToBeLogged, isManualOcr());
				    			
				    			//update Search data using OCR information from Last Real Transfer
				    			if(!isManualOcr() && lastRealTransfer!=null && latestDocumentInMomory.equals(lastRealTransfer) ){
				    				TSServer.bootstrapOCRInfoFromDocument(lastRealTransfer, ocrData , latestSearchInMemory);
				    			}
					    		
					    	}
						}
						catch (Exception e) {
							e.printStackTrace();
						} finally {
							manager.releaseAccess();
						}
						
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Search latestSearchInMemory = InstanceManager.getManager().getCurrentInstance(search.getID()).getCrtSearchContext();
				if(search == latestSearchInMemory) {
					getSearch().removeOcrInProgress();
				} else {
					latestSearchInMemory.removeOcrInProgress();
				}
				
				// this has to be done here, because some information (like instNo or recordedDate) 
				// bootstrapped from OCR data are needed to upload the image to SSF
				try {
					TsdIndexPageServer.uploadImageToSSf(documentI.getId(), search.getID(), true, false);
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("FAIL: OCR Downloader/Service process for " + getName() + " took " + (System.currentTimeMillis() - ocrRunStartTime) + " millis", e);
			throw new RuntimeException(e);
		}
		finally{
			if(image!=null){
				image.setOcrInProgress(false);
				image.setPlanedForOCR(false);
			}
			
		}
		
		logger.info("SUCCESS: OCR Downloader/Service process for " + getName() + " took " + (System.currentTimeMillis() - ocrRunStartTime) + " millis");
	}
	/**
	 * @return the ocrData
	 */
	public OCRParsedDataStruct getOcrData() {
		return ocrData;
	}
	/**
	 * @param ocrData the ocrData to set
	 */
	public void setOcrData(OCRParsedDataStruct ocrData) {
		this.ocrData = ocrData;
	}
	
	/**
	 * Scans the list from ocr and add names into the party if the conditions are meet
	 * @param namesFromOcr
	 * @param document 
	 * @param partyFromDocument
	 * @param infoToBeLogged the place where additional info will be put (ex: for logging purposes)
	 * @return true if the partyFromDocument was modified 
	 */
	public static boolean addNames(List<NameI> namesFromOcr, RegisterDocumentI document, PartyI partyFromDocument, StringBuilder infoToBeLogged){
		if(namesFromOcr.isEmpty()) {
			return false;
		}
		
		boolean modified = false;
		Set<NameI> namesRF = partyFromDocument.getNames();
		Set<NameI> tempNameRF = new LinkedHashSet<NameI>();

		
		
		Set<NameI> candidatesAdded = new LinkedHashSet<NameI>();
		
				
		for (NameI candName : namesFromOcr) {
			if (StringUtils.isNotEmpty(candName.getLastName())){
				String candString =  candName.getFirstName() + candName.getMiddleName() + candName.getSufix() + candName.getLastName();
				boolean mustAdd = true;
				for (NameI reference : namesRF) {
					if(
							GenericNameFilter.isMatchGreaterThenScore(candName, reference, NameFilterFactory.NAME_FILTER_THRESHOLD)
							&&(candName.isCompany()==reference.isCompany())
					){
						String refString = reference.getFirstName() + reference.getMiddleName() + reference.getSufix() + reference.getLastName();
						if(refString.length() <= candString.length()){
							
							//keep ocr and delete 
							if (candName instanceof NameMortgageGranteeI) {
								reference = new NameMortgageGrantee(reference);
								((NameMortgageGranteeI) reference).setTrustee(((NameMortgageGranteeI)candName).isTrustee());
							}
							
							reference.setLastName(candName.getLastName());
							reference.setFirstName(candName.getFirstName());
							reference.setMiddleName(candName.getMiddleName());
							reference.setCompany(candName.isCompany());
							reference.setSufix(candName.getSufix());
							reference.setPrefix(candName.getPrefix());
							
							reference.getNameFlags().getSourceTypes().addAll(candName.getNameFlags().getSourceTypes());
							
							mustAdd = false;
							modified = true;
							infoToBeLogged.append("Saving " + partyFromDocument.getType() + " name from OCR on Document ");
							if(candName.isCompany()) {
								infoToBeLogged.append("(Company: ").append(candName.getLastName()).append(")<br>");
							} else {
								infoToBeLogged.append("(Last: " + candName.getLastName() + 
									", Middle: " + candName.getMiddleName() + 
									", First: " + candName.getFirstName() + 
									", Suffix: " + candName.getSufix() +
									", Prefix: " + candName.getPrefix() + ")<br>");
							}
							candidatesAdded.add(reference);
						}
					}
					
					tempNameRF.add(reference);	//saving the name changed
					
				}
				
				namesRF.clear();//reloading the names changed
				namesRF.addAll(tempNameRF);
				tempNameRF.clear();
				
				if(mustAdd) {
					infoToBeLogged.append("Saving " + partyFromDocument.getType() + " name from OCR on Document ");
					if(candName.isCompany()) {
						infoToBeLogged.append("(Company: ").append(candName.getLastName()).append(")<br>");
					} else {
						infoToBeLogged.append("(Last: " + candName.getLastName() + 
							", Middle: " + candName.getMiddleName() + 
							", First: " + candName.getFirstName() + 
							", Suffix: " + candName.getSufix() +
							", Prefix: " + candName.getPrefix() + ")<br>");
					}
					
					
					namesRF.add(candName);
					candidatesAdded.add(candName);
					modified = true;
				}
				if (modified) { 
					partyFromDocument.setNames(namesRF);
				}
			}
		}
		
		
		/**
		 * A linked set containing names from document already added from the same document by OCR process
		 */
		Set<NameI> namesToBeDeleted = new LinkedHashSet<NameI>();
		
		NameSourceType nameSourceTypeOcr = new NameSourceType(NameSourceType.OCR, document.getId());
		
		for (NameI nameI : namesRF) {
			NameFlagsI nameFlags = nameI.getNameFlags();
			if(nameFlags.isFrom(nameSourceTypeOcr)) {
				if(!candidatesAdded.contains(nameI)) {
					namesToBeDeleted.add(nameI);
				}
			}
		}
		
		if(!namesToBeDeleted.isEmpty()) {
			namesRF.removeAll(namesToBeDeleted);
			partyFromDocument.setNames(namesRF);
		}
		
		
		return modified;
	}
	
}
