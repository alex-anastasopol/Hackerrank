package ro.cst.tsearch.servers.types;

import java.io.File;

import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * counties which have images on the official site
 * CO Cheyenne, CO Saguache, CO San Miguel
 * AZ Graham, AZ Greenlee, AZ Navajo
 *
 */
@SuppressWarnings("deprecation")
public class GenericCountyRecorderROImage extends GenericCountyRecorderRO {
	
	private static final long serialVersionUID = -7177747140955617588L;

	public GenericCountyRecorderROImage(long searchId) {
		super(searchId);
	}

	public GenericCountyRecorderROImage(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected boolean hasImage() {
		return true;
	}
	
	@Override
	protected void addImageLinkInResponse(ServerResponse Response, String imageLink, String sFileLink) {
		if (!StringUtils.isEmpty(imageLink)) {		//if "View Image" button is not disabled
			ImageLinkInPage ilip = new ImageLinkInPage(imageLink, sFileLink);
			Response.getParsedResponse().addImageLink(ilip);
		}
	}
	
	@Override
	protected String addImageLinkInDetails(String detailsString, String imageLink) {
		if (StringUtils.isEmpty(imageLink)) {		//if "View Image" button is disabled
			detailsString = detailsString.replaceAll("(?is)Page Count", "");
			detailsString = detailsString.replaceAll("(?is)<span[^>]+id=\"MainContent_searchMainContent_ctl00_tbPageCount\"[^>]*>[^<]+</span>", "");
			detailsString = detailsString.replaceAll("(?is)View Image", "");
			detailsString = detailsString.replaceAll("(?is)<input[^>]+id=\"MainContent_searchMainContent_ctl00_btnViewImage\"[^>]*>", "");
		} else {
			detailsString = detailsString.replaceAll("(?is)<input[^>]+id=\"MainContent_searchMainContent_ctl00_btnViewImage\"[^>]*>", 
				"<a href=\"" + imageLink + "\">Image</a>");
		}
		return detailsString;
	}
	
	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {
    	    	
    	String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	String img = StringUtils.extractParameter(vsRequest, "img=([^&?]*)");
    	String instrno = StringUtils.extractParameter(vsRequest, "instrno=([^&?]*)");
    	if (StringUtils.isEmpty(instrno)) {
    		String bookType = StringUtils.extractParameter(vsRequest, "bookType=([^&?]*)");
    		String book = StringUtils.extractParameter(vsRequest, "book=([^&?]*)");
    		String page = StringUtils.extractParameter(vsRequest, "page=([^&?]*)");
    		instrno = bookType + "-" + book + "-" + page;
    	}
    	String type = StringUtils.extractParameter(vsRequest, "type=([^&?]*)");
    	String fileName = instrno +	type;
    	if(StringUtils.isEmpty(link) || StringUtils.isEmpty(img) || StringUtils.isEmpty(fileName)){
    		return super.GetLink(vsRequest, vbEncodedOrIsParentSite); 
    	}
    	
    	return GetImageLink(link, fileName, vbEncodedOrIsParentSite);
    	
    }
	
	public ServerResponse GetImageLink(String link, String name, boolean writeImageToClient) throws ServerResponseException {
    	
		String folderName = getImageDirectory() + File.separator + searchId + File.separator;
		new File(folderName).mkdirs();
    	
		String fileName = folderName + name + ".tiff";
		boolean existTiff = FileUtils.existPath(fileName);
    	
		if(!existTiff){
			retrieveImage(link, fileName);
		}
		
		existTiff = FileUtils.existPath(fileName);
			
    	// write the image to the client web-browser
		boolean imageOK = false;
		if(existTiff){
			imageOK = writeImageToClient(fileName, "image/tiff");
		} 
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		// return solved response
		return ServerResponse.createSolvedResponse();  
    }
    
    protected boolean retrieveImage(String link, String fileName) {
    	
    	byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.GenericCountyRecorderRO)site).getImage(link, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return false;
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, "image/tiff"));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(fileName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), fileName);
		}

		return true;
    }
	
	@Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
    	
		String link = image.getLink().replaceFirst("Link=", "");
    	String fileName = image.getPath();
    	if(retrieveImage(link, fileName)){
    		byte[] b = FileUtils.readBinaryFile(fileName);
    		return new DownloadImageResult(DownloadImageResult.Status.OK, b, image.getContentType());
    	}
    	
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }

}
