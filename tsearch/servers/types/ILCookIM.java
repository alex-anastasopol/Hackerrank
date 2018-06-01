package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setHiddenParamMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class ILCookIM extends TSServerDASLAdapter {

	public static final long serialVersionUID = 10000000L;
	
	private static final int ORDER_IMAGES_MODULE_IDX = 1;
	private static final int TRACK_ORDER_MODULE_IDX  = 2;
	private static final int TRACK_CONF_MODULE_IDX   = 3;
	
	private static final int ORDER_IMAGES_MODULE_PID = 201;
	private static final int TRACK_ORDER_MODULE_PID  = 202;
	
    private static Pattern imageLinkPattern = Pattern.compile("Link=look_for_dl_image&documentNumber=([^&]+)&instrumentType=([^&]+)&dateFiled=([^&]+)");
    
    protected static final String DASL_TEMPLATE_CODE = "DASL_COOK_IM_IL";
	private static final ServerPersonalData pers = new ServerPersonalData();

	static {
		pers.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		{
			int id = 0;
			pers.addXPath(id, "//Property", ORDER_IMAGES_MODULE_PID);
			pers.addXPath(id, "//TitleDocument", TRACK_ORDER_MODULE_PID);
		}
	}
    
	public ILCookIM(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink, long searchId, int mid) 
	{
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
			resultType = MULTIPLE_RESULT_TYPE;
	}
	
	
	private String getUniqueIdentifier(){
		String order = getSearch().getUniqueIdentifier();
		int orderLength = order.length();
		if(orderLength > 10){
			order = order.substring(0, 10); 
		}
		return order;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo si = new TSServerInfo(2);
		si.setServerAddress("illinois.reidata.com");
		si.setServerIP("illinois.reidata.com");
		si.setServerLink("http://illinois.reidata.com" );

		// get order number
		String order = getUniqueIdentifier();
		
		// get abstractor first email address
		String emailString = getSearchAttribute(SearchAttributes.ABSTRACTOR_EMAIL);
		emailString = emailString.replaceAll("\\s","");
		emailString = emailString.replaceAll(",{2,}",",");
		emailString = emailString.replaceAll("^,","");
		if(emailString.contains(",")){ 
			emailString = emailString.substring(0, emailString.indexOf(",")); 
		}
		
		// Order Images
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(ORDER_IMAGES_MODULE_IDX, 15);
			sim.setName("ORDER_IMAGES");
			sim.setDestinationPage("/nn-img-pr1.asp"); 
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ORDER_IMAGES_MODULE_PID);
			

			PageZone pz = new PageZone("OrderImages", "Order Images", ORIENTATION_HORIZONTAL, null, 710, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            orderNo = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 12, sim.getFunction(0), "WorkOrder",  "Order", order, searchId),
	            email   = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1, 1, 45, sim.getFunction(1), "Email",      "Email", emailString, searchId),	            
	            state   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(2), "State",      "", "Illinois", searchId),
	            county  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(3), "County",     "", "Cook", searchId),
	            b1      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(4), "B1",         "", " e-mail Requested  Document ", searchId),
	            doc1    = new HTMLControl(HTML_TEXT_FIELD, 1, 2,  2, 2, 85, sim.getFunction(5), "DocId11",    "Documents", null, searchId),
	            doc2    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(6), "DocId12",    "", "", searchId),
	            doc3    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(7), "DocId13",    "", "", searchId),
	            doc4    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(8), "DocId14",    "", "", searchId),
	            doc5    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(9), "DocId15",    "", "", searchId),
	            doc6    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(10),"DocId16",    "", "", searchId),
	            doc7    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(11),"DocId17",    "", "", searchId),
	            doc8    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(12),"DocId18",    "", "", searchId),
	            doc9    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(13),"DocId19",    "", "", searchId),
	            doc10   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 1,  sim.getFunction(14),"DocId20",    "", "", searchId);
	            				 
	            setHiddenParamMulti(true, state, county, b1, doc2, doc3, doc4, doc5, doc6, doc7, doc8, doc9, doc10);	            
	            setRequiredCriticalMulti(true, orderNo, email, doc1);
	            setJustifyFieldMulti(false, orderNo, email, doc1);
	            pz.addHTMLObjectMulti(orderNo, email, doc1, doc2, doc3, doc4, doc5, doc6, doc7, doc8, doc9, doc10, state, county, b1);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}
				
		// Track Orders
		/*{
			TSServerInfoModule 		
			sim = si.ActivateModule(TRACK_ORDER_MODULE_IDX, 5);
			sim.setName("TRACK_ORDER");
			sim.setDestinationPage("/nn-ord-pr1.asp");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(TRACK_ORDER_MODULE_PID);
			
			PageZone pz = new PageZone("TrackOrder", "Track Image Order - by Work Order", ORIENTATION_HORIZONTAL, null, 710, 50, PIXELS , true);
			
			try{				
	            HTMLControl 
	            orderNo = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 12, sim.getFunction(0), "workOrder",  "Work Order", order, searchId),
	            email   = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1,  1, 45, sim.getFunction(1), "Email",      "Email", emailString, searchId),	            
	            radio1  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1,  sim.getFunction(2), "radio1",      "", "off", searchId),	            
	            confid  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1,  sim.getFunction(3), "confid",      "", "", searchId),
	            b1      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1,  sim.getFunction(4), "b1",         "", " Show My Orders History ", searchId);
	            				           
	            setHiddenParamMulti(true, radio1, confid, b1);	            
	            setRequiredCriticalMulti(true, orderNo, email);
	            setJustifyFieldMulti(false, orderNo, email);
	            pz.addHTMLObjectMulti(orderNo, email, radio1, confid, b1);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}*/
		
		// Track Orders
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(TRACK_CONF_MODULE_IDX, 5);
			sim.setName("TRACK_CONF");
			sim.setDestinationPage("/nn-ord-pr1.asp");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(TRACK_ORDER_MODULE_PID);

			PageZone pz = new PageZone("TrackConf", "Track Image Order - by Confirmation Number", ORIENTATION_HORIZONTAL, null, 710, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            orderNo = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 12, sim.getFunction(0),"confid",   "Conf. No", null, searchId),
	            email   = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1,  1, 45, sim.getFunction(1), "Email",      "Email", emailString, searchId),	
	            radio1  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1,  sim.getFunction(2),"radio1",   "", "on", searchId),	            
	            confid  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1,  sim.getFunction(3),"workOrder","", "", searchId),
	            b1      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1,  sim.getFunction(4),"b1",       "", " Show My Orders History ", searchId);
	            				           
	            setHiddenParamMulti(true, radio1, confid, b1);	            
	            setRequiredCriticalMulti(true, orderNo, email);
	            setJustifyFieldMulti(false, orderNo, email);
	            setHiddenParamMulti(true, email);
	            pz.addHTMLObjectMulti(orderNo, email, radio1, confid, b1);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz); 
		}
		
		si.setupParameterAliases();
		//setModulesForAutoSearch(si);
		
		return si;
	}
	
	@Override
	Map<Integer, RecordRules> getRecordRules(int parserId) {
			return new HashMap<Integer, RecordRules>();
	}
	
	/**
	 * 
	 * @param docsString
	 * @return
	 */
	private String [] extractDocNos(String docsString){
		
		if(StringUtils.isEmpty(docsString)){
			return null;
		}
		
		// compact white spaces
		docsString = docsString.replaceAll("\\s{2,}", " ");
		// remove wite spaces around
		docsString = docsString.replaceAll("\\s*,\\s*", ",");
		// remove duplicated commas
		docsString = docsString.replaceAll(",{2,}",",");
		// remove commas at start and end of string
		docsString = docsString.replaceFirst("^,","");
		docsString = docsString.replaceFirst(",$","");
		
		// check that it is well-formed
		if(!docsString.matches("(((\\d{8})|(\\d{10})|([A-Z]\\d{7})|([A-Z]\\d{9})),?)+")){
			return null;
		}
		
		// split
		return docsString.split(",");		
	}
	
//	/**
//	 * Extract display information for order status
//	 * @param response
//	 * @return
//	 */
//	private String extractOrderStatus(String response){
//		int istart = response.indexOf("Document Order History For");
//		if(istart == -1){ return null; }
//		response = StringUtils.extractTagContents(istart, response, "table");
//		if(response == null){ return null; }		
//		return  "<B>Document Order History:</b><br/>" + response;		
//	}
	
//	/**
//	 * 
//	 * @param response
//	 * @return
//	 */
//	private String addDaslImageLinks(String response){
//		
//		// iterate on all rows
//		for(String row: response.split("<tr>")){
//			String tds [] = row.split("<td");
//			// only for finished docs
//			if(tds.length == 7 && tds[5].contains("Finished")){
//				String docNo = StringUtils.extractParameter(tds[3], ">(\\d+)<");
//				// if we have docNo
//				if(!StringUtils.isEmpty(docNo)){
//					String link = 	"<a href=\"" + 
//									CreatePartialLink(TSConnectionURL.idGET) + "look_for_dl_image" +
//					    			"&documentNumber=" + docNo +
//					    			"&instrumentType=" + "x" +
//					    			"&dateFiled=" + "x" +
//							    	"\"> " + docNo + " </a>";				    			
//					response = response.replaceAll(">" + docNo + "<",">" + link + "<");
//				}
//			}			
//		}
//			
//		// return processed response
//		return response;
//	}
	
	@Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		 String [] docNos = new String[]{};
		
		// split the PIN into its parts
        if(module.getModuleIdx() == ORDER_IMAGES_MODULE_IDX){
        	String docsString = module.getFunction(5).getParamValue();
            docNos = extractDocNos(docsString);
    		if(docNos == null){
    			return ServerResponse.createErrorResponse("No document numbers or invalid doc numbers provided: " + docsString);
    		}
    		if(docNos.length > 10){
    			return ServerResponse.createErrorResponse("Maximum 10 doc numbers can be provided at a time: " + docsString);
    		}
            /*for(int i=0; i<docNos.length; i++){
            	module.getFunction(5 + i).setParamValue(docNos[i]);
            } */
    		
    		getSearch().setAdditionalInfo("docNos", docNos);
        }else if(module.getModuleIdx() == TRACK_CONF_MODULE_IDX){
        	
        }
        
        //lets search by multiple docNos
        if(docNos.length > 1){
        	List<TSServerInfoModule> modules = getMultipleModules(module, sd);
        	List<String> fakeReps = new ArrayList<String>();
        	for(TSServerInfoModule mod : modules)
        		fakeReps.add(mod.toString());
        		
        	return super.searchByMultipleInstrument(modules, sd, null);
        } else {        
        	return super.SearchBy(module, sd);
        }
    }
	
	@Override
	public List<TSServerInfoModule> getMultipleModules(TSServerInfoModule module, Object sd) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		try {

			String[] docNos = new String[]{};

			Object o = getSearch().getAdditionalInfo("docNos");

			if (o != null)
				docNos = (String[]) o;

			if (docNos.length > 0) {
				for (String docNo : docNos) {
					TSServerInfoModule mod = (TSServerInfoModule) module.clone();
					
					if(mod.getFunctionCount() > 5 )
						mod.setData(5, docNo);
					
					modules.add(mod);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// save modules to save search params
		this.getSearch().setAdditionalInfo("modulesSearched", modules);

		return modules;
	}
	
	@Override
	protected boolean verifyModule(TSServerInfoModule mod) {
		if(mod.getFunctionCount() > 5  && StringUtils.isNotEmpty(mod.getFunction(5).getParamValue()))
			return true;		
		return false;
	}
	
	@Override
	public DaslResponse mergeResults(List<DaslResponse> responses) {
		if(responses.isEmpty()) {
			return null;
		}
		
		DaslResponse finalResp = responses.get(0).clone();
		
//		StringBuffer xmlBuf = new StringBuffer();
//		
//		for(DaslResponse resp : responses){
//			xmlBuf.append(resp.xmlResponse);
//		}
//		
//		finalResp.xmlResponse = xmlBuf.toString();
		
		getSearch().setAdditionalInfo("DASLResponses", responses);
		
		return finalResp;
	}


	@Override
	protected ServerPersonalData getServerPersonalData() {
		return pers;
	}
	
	@Override
	protected HashMap<String, Object> fillTemplatesParameters(Map<String, String> params) {

		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		HashMap<String, Object> templateParams = super.fillTemplatesParameters(params);
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCurrentCommunity().getID().intValue(), 
				miServerID);
		String sateName = dat.getName();
		String stateAbrev = sateName.substring(0, 2);

		//County=Cook, State=Illinois, Email=andrei.alecu@cst.ro, B1=e-mail Requested  Document, WorkOrder=3899399389, DocId11=85309139
		
		String email = params.get("Email");
		templateParams.put(AddDocsTemplates.DASLImEmail, email);
		
		String docno = params.get("DocId11");
		templateParams.put(AddDocsTemplates.DASLDocumentNumber, docno);

		String reference = params.get("WorkOrder");
		
		String confid = params.get("confid");
		templateParams.put(AddDocsTemplates.DASLClientReference, confid);
		
		templateParams.put(AddDocsTemplates.DASLClientTransactionReference, reference);
		
		templateParams.put(AddDocsTemplates.DASLStateAbbreviation, stateAbrev);

		templateParams.put(AddDocsTemplates.DASLCountyFIPS, dat.getCountyFIPS());

		templateParams.put(AddDocsTemplates.DASLStateFIPS, dat.getStateFIPS());

		templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericDTSite", "DaslClienId"));
	    
		return templateParams;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String response = Response.getResult();
		String display = "";

		switch (viParseID) {
		case ORDER_IMAGES_MODULE_PID:

			String[] docNos = new String[] {};

			Object o = getSearch().getAdditionalInfo("docNos");
			if (o != null)
				docNos = (String[]) o;

			HashMap<String, DaslResponse> daslResponses = getDaslReponseMap();

			StringBuffer toDisplayBuffer = new StringBuffer();
			
			for (String docNo : docNos) {
				// process answer for it
				DaslResponse daslResponse = daslResponses.get(docNo);

				if(docNos.length == 1){
					daslResponse = new DaslResponse();
					daslResponse.xmlResponse = response;
				}
				
				if (daslResponse != null) {

					String confNo = StringUtils.extractParameter(daslResponse.xmlResponse, "<OrderBusinessId>(\\d+)</OrderBusinessId>");
					String imageAlreadyScanedConfNo = StringUtils.extractParameter(daslResponse.xmlResponse, "OrderBusinessId=\"(\\d+)\"");

					if (StringUtils.isNotEmpty(confNo) || StringUtils.isNotEmpty(imageAlreadyScanedConfNo)) {

						if (StringUtils.isNotEmpty(confNo)) {
							toDisplayBuffer.append("<br/><b> <font color=\"green\"> Order placed successfully!</font></b> For document: " + docNo + ", your Confirmation No. is: " + confNo + "<br/>");
						}

						if (StringUtils.isNotEmpty(imageAlreadyScanedConfNo)) {
							// image already scanned -> returned in the answer
							String docNoFromResp = StringUtils.extractParameter(daslResponse.xmlResponse, "<DocumentNumber>([^<]+)</DocumentNumber>");
							if(StringUtils.isEmpty(docNoFromResp))
								docNoFromResp = docNo;
							toDisplayBuffer.append("<br/><b> <font color=\"green\"> Image available in TSRIndex for document: </font> </b> " + docNoFromResp + "<br/>");
						}

						// create fake doc
						DocumentsManagerI manager = getSearch().getDocManager();

						try {
							manager.getAccess();

							InstrumentI instr = new Instrument();
							String docno = docNo;

							instr.setDocno(docno);
							instr.setInstno(docno);
							DocumentI atsDoc = (RegisterDocumentI) manager.createAndUploadEmptyDocument(DType.ROLIKE, "MISCELLANEOUS", instr, "IM", true, true);

							String fakeLink = "getImageFromLA&docno=" + docno;
							Response.getParsedResponse().addImageLink(new ImageLinkInPage(fakeLink, docno + ".tif"));

							ImageI image = new Image();
							image.addLink(fakeLink);
							image.setType(IType.TIFF);
							image.setFileName(docno + ".tif");

							atsDoc.setSearchType(SearchType.NA);
							atsDoc.setImage(image);
							atsDoc.setSiteId(this.getServerID());
							atsDoc.setDocType("MISCELLANEOUS");
							atsDoc.setDocSubType("MISCELLANEOUS");

							this.addDocumentInATS(atsDoc, true);
							SearchLogger.info(atsDoc.asHtml() + "<br>", getSearch().getID());
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							manager.releaseAccess();
						}
					} else {
						toDisplayBuffer.append("<br/><b> <font color=\"red\"> Error: order not placed for document " + docNo + "!</font></b><br/>");
					}
				} else {
					toDisplayBuffer.append("<br/><b> <font color=\"red\"> Error: order not placed for document " + docNo + "!</font></b><br/>");
				}
			}
			
			display = toDisplayBuffer.toString();
			break;

		case TRACK_ORDER_MODULE_PID:

			String status = StringUtils.extractParameter(response, "<OrderStatusBusinessId>(\\d+)</OrderStatusBusinessId>");
			String maybeImageReady = StringUtils.extractParameter(response, "OrderBusinessId=\"(\\d+)\"");
			String count = StringUtils.extractParameter(response, "<Count>(\\d+)</Count>");

			if (StringUtils.isNotEmpty(status)) {
				display = "<b> <font color=\"green\"> Image is ready! </font> </b>";
			} else {
				if (StringUtils.isNotEmpty(maybeImageReady) && !"0".equals(count) && !StringUtils.isEmpty(count)) {
					display = "<b> <font color=\"green\"> Image is ready! </font> </b>";
				} else {
					display = "<b> <font color=\"red\"> Error: order status cannot be retrieved! </font> </b>";
				}
			}

			break;
		}

		Response.getParsedResponse().setResponse(display);
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<String, DaslResponse> getDaslReponseMap() {
		HashMap<String, DaslResponse> result = new HashMap<String, DaslResponse>();

		try {
			
			List<DaslResponse> responses = new ArrayList<DaslResponse>();

			Object o = getSearch().getAdditionalInfo("DASLResponses");
			if (o != null) {
				responses = (List<DaslResponse>) o;
			}

			for (DaslResponse r : responses) {
				try {
					TSServerInfoModule mod = (TSServerInfoModule) r.getAttribute("TSServerInfoModule");
					
					String key = mod.getFunction(5).getParamValue();
					
					if(StringUtils.isNotEmpty(key))
						result.put(key, r);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}


	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		String link = "";
		if (image.getLink(0).matches("getImageFromLA\\&docno=\\w+")) {
			link = image.getLink(0);
		}
		
		if (StringUtils.isEmpty(link)) {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}
		
		String docNo = link.split("=")[1];
		String fileName = image.getPath();
		
		if(ILCookImageRetriever.INSTANCE.retrieveImage(docNo, fileName, "", "", searchId)){
			byte b[] = FileUtils.readBinaryFile(fileName);
			//already counted is retreiveImage
			//afterDownloadImage(true);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
		}
		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
	}
	
    /**
     * Override GetLink in order to retrieve the image
     */
    @Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)throws ServerResponseException {   
    	
    	/*
    	 * get image from DataTree if necessary
    	 */
    	Matcher matcher = imageLinkPattern.matcher(vsRequest);
    	if(matcher.find()){
    		
    		// extract image info from link
    		final String documentNumber = matcher.group(1);
        	
        	// create filename and its folder
    		String folderName = getCrtSearchDir() + "Register" + File.separator;
    		new File(folderName).mkdirs();
        	String fileName = folderName + documentNumber + ".tiff";
        	
        	// retrieve image
        	ILCookImageRetriever.INSTANCE.retrieveImage(documentNumber, fileName, "", "", searchId);
        				
    		// write the image to the client web-browser
			boolean imageOK = writeImageToClient(fileName, "image/tiff");

			// image not retrieved
			if(!imageOK){   
				
				// mark it as invalid
    			getSearch().setAdditionalInfo("img_" + documentNumber, Boolean.FALSE);	
		        
		        // return error message
    			ParsedResponse pr = new ParsedResponse();
    			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
    			throw new ServerResponseException(pr);
			}
			return ServerResponse.createSolvedResponse();
    	}
    	
    	// default behaviour
    	return super.GetLink(vsRequest, vbEncoded);
    }

	/**
	 * Check if an image exists on Stewart Database (DASL also uses the same database)
	 * @param insNo
	 * @param searchId
	 * @return true if image was found
	 */
	public  boolean imageExists(String insNo, long searchId,int miServerID){
		
		// try n times, maybe connection fails		
		for(int i=0; i<2; i++){
			
			try{
				
				HTTPSiteInterface site = HTTPSiteManager.pairHTTPSiteForTSServer("ILCookIM" , searchId,miServerID);
				
				String link = "http://illinois.reidata.com/eimager/eimagerform1.aspx";
				HTTPRequest request = new HTTPRequest(link);
				request.setMethod(HTTPRequest.POST);
				
		    	request.setPostParameter("account", SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ILCookIM", "user"));
		    	request.setPostParameter("EMail", SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ILCookIM", "email"));    	
				String response = site.process(request).getResponseAsString();		
				String viewStateString = StringUtils.getTextBetweenDelimiters( "\"__VIEWSTATE\" value=\"" , "\"", response);
				
				link = "http://illinois.reidata.com/eimager/eimagerform1.aspx";
				request = new HTTPRequest(link);
				request.setMethod(HTTPRequest.POST);
				request.setPostParameter("__VIEWSTATE", viewStateString);
				request.setPostParameter("radio", "RadioButton1");
				request.setPostParameter("TextBox1", insNo);
				request.setPostParameter("StatusBox","");
				request.setPostParameter("Button1","Wait Please");
				request.setPostParameter("TextBox2", "Searching....");		
				response = site.process(request).getResponseAsString();
				
				return response.contains("Dcument Found") || response.contains("Document Found");				
				
			}catch(RuntimeException e){
				logger.error(e);
			}
			
			// sleep  for two seconds - error appeared
			try{ TimeUnit.SECONDS.sleep(2); } catch(InterruptedException e){}
		}
		
		return false;
			
	}

	@Override
	public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void specificParseAddress(Document doc, ParsedResponse item, ResultMap resultMap) {
		// TODO Auto-generated method stub

	}
}
