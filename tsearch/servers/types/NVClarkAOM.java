package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.iterator.MapTypeBookPageIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.connection.clarkimages.NvClarkAOMConn;
import com.stewart.ats.connection.clarkimages.NvClarkAOMConn.MapType;

public class NVClarkAOM extends FLGenericDASLDT{

	private static final long serialVersionUID = -8757748707578944329L;
	private static final String DT_FAKE_RESPONSE = ro.cst.tsearch.utils.StringUtils.fileReadToString(FAKE_FOLDER+"DASLFakeResponse.xml");
	
	NvClarkAOMConn conn = new NvClarkAOMConn();
	
	public NVClarkAOM(long searchId) {
		super(searchId);
	}

	public NVClarkAOM(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	
	@SuppressWarnings("deprecation")
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		int functionBook = 0;
		int functionPage = 1;
		int functionType = 2;
		
		TSServerInfo info = getDefaultServerInfo();
		TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
		String bookAlias = module.getFunction(functionBook/* Book */).getParamName();
		String pageAlias = module.getFunction(functionPage/* Page */).getParamName();
		
		String book = "";
		String page = "";
		
		String link = image.getLink();
		int poz = link.indexOf("?");
		
		if(poz>0){
			link = link.substring(poz+1);
		}
		
		String[] allParameters = link.split("[&=]");
		
		for(int i=0;i<allParameters.length-1;i+=2){
			if(bookAlias.equalsIgnoreCase(allParameters[i])){
				book = allParameters[i+1];
			}
			else if(pageAlias.equalsIgnoreCase(allParameters[i])){
				page = allParameters[i+1];
			}
		}
		
		module.getFunction(functionBook).forceValue(book);
		module.getFunction(functionPage).forceValue(page);
		String typeAlias = module.getFunction(functionType/* Book */).getParamName();
		String type = link.replaceAll(".*" + typeAlias + "=([^&]*)", "$1");
		poz = type.indexOf("&");
		if (poz > 0) {
			type = type.substring(0, poz);
		}
		module.getFunction(functionType).forceValue(type);
		
		String imageName = image.getPath();
    	if(FileUtils.existPath(imageName)){
    		byte b[] = FileUtils.readBinaryFile(imageName);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
    	}
		
		return SearchBy(module, image).getImageResult();
	}

	public boolean isEnabledAlreadyFollowed(){
		return false;
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		if(mSearch==null){
			mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		}
		
		Map<String, String> newParam = 	getNonEmptyParams(module, null );
		
		String lstMapType =  newParam.get("lstMapType");
		String txtMapFile =  newParam.get("txtMapFile");
		String txtMapPage =  newParam.get("txtMapPage");
		
		MapType type =  MapType.valueOf(lstMapType);
		
		byte[] image = null;
		
		try{image = conn.downloadMap(txtMapFile, txtMapPage, type);}catch(Exception e){}
		
		
		if (TSServerInfo.IMG_MODULE_IDX == module.getModuleIdx()) {
			boolean imageDownloaded = image!=null && image.length>0;
			if (imageDownloaded) {
				ServerResponse sr = new ServerResponse();
				sr.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK,image,"application/pdf"));
				return sr;
			}
		} else if ( TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX == module.getModuleIdx() ) {
			mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
			String book = module.getParamValue( 0 );
			String page = module.getParamValue( 1 );
			String docNo = "";
			String year = "";
			
			String dataTreeType = lstMapType + "_MAP";
	    	
			int yearI =-1; try{yearI=Integer.parseInt(year);}catch(Exception e){}
			InstrumentI i = new Instrument(book,page,"","",yearI);
			i.setInstno(docNo);
			i.setYear(yearI);
			i.setDocType(dataTreeType);
	    	
	    	logSearchBy(module);
	    	
			if(image!=null && image.length>0 ){
				SearchLogger.info( "<b><font color=\"red\">Image " +i.prettyPrint()+ " Downloaded from Assesor </font></b><br>", searchId );  

				String grantor  = "County of "+InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
				grantor=grantor==null?"":grantor;
				String grantee = mSearch.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
				grantee=grantee==null?"":grantee;
				
				grantee = ro.cst.tsearch.utils.StringUtils.HTMLEntityEncode(grantee);
				grantor = ro.cst.tsearch.utils.StringUtils.HTMLEntityEncode(grantor);
				
				String doc = DT_FAKE_RESPONSE.replace("@@Grantee@@", grantee);
				doc = doc.replace("@@Grantor@@", grantor);
				doc = doc.replace("@@Date@@", "01/01/1960");
				doc = doc.replace("@@Type@@-@@Type@@", dataTreeType);
				
				
				doc = doc.replace("@@Book@@", book==null?"":book);
				doc = doc.replace("@@Page@@", page==null?"":page);
				doc = doc.replace("@@DocNo@@", docNo==null?"":docNo);
				
				
				return searchBy(getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX), sd, doc);
			}
		}else{
				String mess = "</br><font color=\"red\">Not enough data entered for a search to be performed!</font></br>";
				SearchLogger.info(mess, searchId);
				return ServerResponse.createErrorResponse("Not enough data entered for a search to be performed!");
		}
		return ServerResponse.createWarningResponse("No data found!");
	}
	
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten) {
		DocumentI doc = response.getParsedResponse().getDocument();
		if(doc!=null){
			doc.setDataSource("AOM");
			doc.setSiteId(miServerID);
		}
		if(doc.hasImage()){
			ImageI docImage =   doc.getImage();
			docImage.setContentType("application/pdf");
			docImage.setPath(docImage.getPath().replace(".tiff", ".pdf").replace(".tiff", ".pdf"));
		}
		
		return super.addDocumentInATS(response, htmlContent, forceOverritten);
	}
	
	protected String getFileName(String instrNo) {
		return instrNo + ".pdf" ;
	}
	
	protected String createLinkForImage(HashMap<String, String> value) {
		HashMap<String, String> map = (HashMap<String, String>) value;
		String book = map.get("book");
		String page = map.get("page");
		String type = map.get("type");
		type = type.replace("_MAP", "").replace("MAP", "");
		
		
		TSServerInfoModule imgModule = getDefaultServerInfoWrapper().getModule(TSServerInfo.IMG_MODULE_IDX);
		StringBuilder build = new StringBuilder("");// <a href=\"
		build.append(createPartialLink(TSConnectionURL.idDASL, TSServerInfo.IMG_MODULE_IDX));
		build.append("DASLIMAGE&");

		build.append(imgModule.getFunction(0/* book */).getParamName());
		build.append("=");
		build.append(book);
		build.append("&");

		build.append(imgModule.getFunction(2/* type */).getParamName());
		build.append("=");
		build.append(type);
		build.append("&");

		build.append(imgModule.getFunction(1/* page */).getParamName());
		build.append("=");
		build.append(page);
		
		return build.toString();
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search search = getSearch();
		
		int searchType = search.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
		
			String mapBook = getSearchAttribute(SearchAttributes.LD_BOOKNO);
			String mapPage = getSearchAttribute(SearchAttributes.LD_PAGENO);
			
			if(StringUtils.isNotBlank(mapBook) && StringUtils.isNotBlank(mapPage)) {			
				
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
				module.clearSaKeys();
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
				module.addIterator(new MapTypeBookPageIterator(searchId));
				modules.add(module);	
			}
		
		}
		
		serverInfo.setModulesForAutoSearch( modules );
	}

	
}
