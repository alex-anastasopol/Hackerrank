package ro.cst.tsearch.servers.types;
/**
 * @author costin
 *
 * 
 */
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSServer.ADD_DOCUMENT_RESULT_TYPES;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.dip.bean.CallDipResult;
public interface TSInterface
{
	static final long serialVersionUID = 10000000;
	public TSServerInfo getCurrentClassServerInfo();
	public TSServerInfo getDefaultServerInfo();
	public TSServerInfo getDefaultServerInfoWrapper();
	public void setServerForTsd(Search search, String siteRealPath);

	public static class DownloadImageResult{
		
		public DownloadImageResult(){}
		
		public static enum  Status {
			OK,
			ERROR;
		}
		
		private Status status;
		private byte[] imageContent;
		private String contentType ="";
		
		public String getContentType() {
			return contentType;
		}
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}
		public Status getStatus() {
			return status;
		}
		public void setStatus(Status status) {
			this.status = status;
		}
		
		public byte[] getImageContent() {
			return imageContent;
		}
		public void setImageContent(byte[] imagecontent) {
			this.imageContent = imagecontent;
		}
	
		public  DownloadImageResult(Status status, byte[] imageContent, String contentType){
			this.status = status;
			this.imageContent = imageContent;
			this.contentType = contentType;
		}
		
	}
	
	public ServerResponse performAction(int viServerAction, String requestParams, TSServerInfoModule module, Object sd) throws ServerResponseException;
	public ServerResponse performAction(int viServerAction, String requestParams, TSServerInfoModule module, Object sd, Map<String, Object> extraParams) throws ServerResponseException;
	public ServerResponse performLinkInPage(LinkInPage link) throws ServerResponseException;
	public ServerResponse performLinkInPage(LinkInPage link, int newServerAction) throws ServerResponseException;

	public ServerResponse LogIn()  throws ServerResponseException;
	public ServerResponse SearchBy (TSServerInfoModule module,  Object sd) throws ServerResponseException;
	public ServerResponse GetLink (String vsRequest, boolean vbEncoded) throws ServerResponseException;
	public ServerResponse SaveToTSD(String vsRequest, Map<String, Object> extraParams) throws ServerResponseException;
	public ServerResponse recursiveAnalyzeResponse(ServerResponse result) throws ServerResponseException;
	public String getPrettyFollowedLink (String initialFollowedLnk);
	
	public void NewSession();
	public boolean SessionExpired();

	public int getServerID();
	public void setServerID(int ServerID);
	public Search getSearch();
	public int getCommunityId();

	public static String COUNTY_INDEX = "CI";
	public void SetAttribute(String name, Object value);
	public Object GetAttribute(String name);	
	
	public boolean isTimeToSave(ServerResponse sr);
	public boolean anotherSearchForThisServer(ServerResponse sr);
	public boolean mustGoBackToUser(ServerResponse sr);
	public boolean goToNextServer(Search global);
	public String getSaveToTsdButton(String name, String originalLink,int viParseID);
	public void setDocsValidator(int type, DocsValidator defaultOne);
	public void addDocsValidator( int type, DocsValidator defaultOne );
	public void addDocsValidator( DocsValidator type, DocsValidator defaultOne );
	public void setIteratorType( int type );
	public void clearDocsValidators();
	public DocsValidator getDocsValidator();
	public Vector<DocsValidator> getDocsValidators();
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage);
	
	public void addCrossRefDocsValidator(int type, DocsValidator defaultOne);
	public void addCrossRefDocsValidator(DocsValidator type, DocsValidator defaultOne);
	public void clearCrossRefDocsValidators();
	public DocsValidator getCrossRefDocsValidator();
	public Vector<DocsValidator> getCrossRefDocsValidators();
	
    public TSServerInfoModule getSearchByBookPageModule(TSServerInfo serverInfo, String book, String page, String instrumentNo);
    public TSServerInfoModule getLastTransferSearchModule(TSServerInfo serverInfo, String book, String page, String instrumentNo);
    public CallDipResult ocrDownloadAndScanImageIfNeeded(
    		DocumentI lastTransfer,String imageType,int maxErrorCount,long timeToSleepWhenError, 
    		boolean checkNameOnLastRealTransfer);
    public CallDipResult ocrDownloadAndScanImageIfNeeded(
    		DocumentI lastTransfer,String imageType, boolean isManualOcr, int maxErrorCount,long timeToSleepWhenError,
    		boolean checkNameOnLastRealTransfer);
    public void resetServerSession();
    
    ServerResponse removeUnnecessaryResults( ServerResponse sr );
    
    void setStartTime(long l);
    
    String getExtraParameterValueAtTSRImageDownloadLink() ;
    
    String getExtraParameterNameAtTSRImageDownloadLink() ;
    
    boolean isParentSite();
    
    void setParentSite(boolean parentSite);
    
    boolean isRangeNotExpanded();
    
    void setRangeNotExpanded(boolean rangeNotExpanded);
    
    boolean isAoLike();
    
    List<NameI> parseOCRNames(OCRParsedDataStruct ocpds);
    
    List<NameI> parseOCRNames(Vector<String> ocrNames);
	
	DownloadImageResult downloadImageAndUpdateSearchDocument( String documentIdStr)  throws ServerResponseException;
	
	DownloadImageResult downloadImage(ImageI image, String documentIdStr)  throws ServerResponseException;
	
	/**
	 * TSInterface personal processing for the last real transfer data
	 */
	void processLastRealTransfer(TransferI transfer, OCRParsedDataStruct ocrdata);
	
	/**
	 * TSInterface personal processing for the last transfer data
	 */
	void processLastTransfer(TransferI lastTransfer, OCRParsedDataStruct ocrData);
	
	/**
	 * Sets the search to the desired value
	 * @param search
	 */
	public void forceSearch(Search search);
	
	public void addDocumentAdditionalProcessing(DocumentI doc, ServerResponse response);
	
	public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response);
	/**
	 * The server knows it is performing a next link
	 * @return true if this is a next link
	 */
	boolean isInNextLinkSequence();
	/**
	 * Tells the server is performing a next link or not.
	 * Default is false
	 * @param inNextLinkSequence
	 */
	void setInNextLinkSequence(boolean inNextLinkSequence);
	boolean isDoNotLogSearch();
	void setDoNotLogSearch(boolean doNotLogSearch);
	
	/**
	 * Implement this if you need special processing when a document fails validation
	 * @param response
	 */
	void performAdditionalProcessingWhenInvalidatingDocument(ServerResponse response);
	
	void performAdditionalProcessingBeforeRunningAutomatic();
	
	void performAdditionalProcessingAfterRunningAutomatic();
	
	void processInstrumentBeforeAdd(DocumentI doc);
	
	public String getContinueForm(String p1, String p2, long id);
	
	public RestoreDocumentDataI saveInvalidatedDocumentForRestoration(
			FilterResponse filter, ServerResponse response, boolean invalidated);
	public RestoreDocumentDataI saveInvalidatedDocumentForRestoration(
			FilterResponse filter, ParsedResponse parsedResponse);
	public Object getRecoverModuleFrom(RestoreDocumentDataI document);
	void setMsServerId(String msServerId);
	public Object getImageDownloader(RestoreDocumentDataI document);
	
	DownloadImageResult downloadImage(DocumentI document) throws ServerResponseException;
	
	/**
	 * Used to clean the HTML saved when saving a document. Can be used to clean links for example.
	 * @param htmlContent
	 * @return content cleaned
	 */
	String cleanHtmlBeforeSavingDocument(String htmlContent);
	
	DataSite getDataSite();
	
	/**
	 * Used to get the HTML that creates the button for saving search parameters in parent site when nothing is found.
	 * @param response ServerResponse used to get the parameters needed
	 * @return the HTML that describes the button
	 */
	String getSaveSearchParametersButton(ServerResponse response);
	
	public boolean isRepeatDataSource();
	public void setRepeatDataSource(boolean repeatDataSource);
	
	public void modifyDefaultServerInfo();
	public void processOcrInstrumentNumber(long id, InstrumentI in);
	public double parseMortgageAmount(Vector<String> amountVector);
	
	/**
	 * 
	 * @param documentToOcr
	 * @param dipInputFile
	 * @param xmlFileName the name of the file that will be generated (output file name without extension)
	 * @param imageFile
	 * @param infoToBeLogged
	 * @param xmlFromSmartEditor
	 * @return
	 */
	CallDipResult callDip(DocumentI documentToOcr, String dipInputFile,
			String xmlFileName, String imageFile, StringBuilder infoToBeLogged, String xmlFromSmartEditor);
	
	ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(DocumentI doc,
			boolean forceOverritten);
		
	boolean lastAnalysisBeforeSaving(ServerResponse serverResponse);
	DownloadImageResult downloadImageFromDataSource(ImageI image, String documentIdStr) throws ServerResponseException;
	
	void logStartServer(String extraMessage);
	void logFinishServer(String extraMessage);
	
	public String getIncludedScript();
	void countOrder();
	void cleanOCRData(OCRParsedDataStruct ocrData);
	DownloadImageResult downloadImageAndCreateItOnDocument(String documentIdStr) throws Exception;
	
	DownloadImageResult lookupForImage(DocumentI doc) throws ServerResponseException;
	DownloadImageResult lookupForImage(DocumentI doc, String documentIdStr) throws ServerResponseException;
	public void updateDocumentWithOCRData(DocumentsManagerI documentsManager, DocumentI documentI, OCRParsedDataStruct ocrData, Search search,
			StringBuilder infoToBeLogged);
}
