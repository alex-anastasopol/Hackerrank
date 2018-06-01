package ro.cst.tsearch.servers.types;

import java.io.File;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * counties which don't have images on the official site and images are taken from TS
 * CO Huerfano
 */
@SuppressWarnings("deprecation")
public class GenericCountyRecorderROImageTS extends GenericCountyRecorderRO {
	
	private static final long serialVersionUID = 5879565982377079549L;

	public GenericCountyRecorderROImageTS(long searchId) {
		super(searchId);
	}

	public GenericCountyRecorderROImageTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected boolean hasImageFromTS() {
		return true;
	}
	
	@Override
	protected void addImageLinkInResponse(ServerResponse Response, String imageLink, String sFileLink) {
		ImageLinkInPage ilip = new ImageLinkInPage(imageLink, sFileLink);
		Response.getParsedResponse().addImageLink(ilip);
	}
	
	@Override
	protected String addImageLinkInDetails(String detailsString, String imageLink) {
		detailsString = detailsString.replaceAll("(?is)Page Count", "");
		detailsString = detailsString.replaceAll("(?is)<span[^>]+id=\"MainContent_searchMainContent_ctl00_tbPageCount\"[^>]*>[^<]+</span>", "");
		String partialLink = CreatePartialLink(TSConnectionURL.idGET);
		detailsString = detailsString.replaceAll("(?is)Image Not Available", "<a href=\"" + partialLink + imageLink + "\">Image</a>");
		return detailsString;
	}
	
	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {
    	    	
    	String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	String fileName = StringUtils.extractParameter(vsRequest, "instrno=([^&?]*)") + 
    		StringUtils.extractParameter(vsRequest, "type=([^&?]*)");
    	if(StringUtils.isEmpty(link) || StringUtils.isEmpty(fileName)){
    		return super.GetLink(vsRequest, vbEncodedOrIsParentSite); 
    	}
    	
    	return GetImageLink(link, fileName, vbEncodedOrIsParentSite);
    	
    }
	
	public ServerResponse GetImageLink(String link, String name, boolean writeImageToClient) throws ServerResponseException {
    	
		String folderName = getImageDirectory() + File.separator + searchId + File.separator;
		new File(folderName).mkdirs();
    	
		String fileName = folderName + name + ".tiff";
		String fileNamePdf = fileName.replaceAll("\\.tiff", ".pdf");
    	boolean existTiff = FileUtils.existPath(fileName);
    	boolean existPDF = FileUtils.existPath(fileNamePdf);
    	
		if(!existTiff && !existPDF){
			retrieveImage(link, fileName);
		}
		
		existTiff = FileUtils.existPath(fileName);
		existPDF = FileUtils.existPath(fileNamePdf);
			
    	// write the image to the client web-browser
		boolean imageOK = false;
		if(existTiff){
			imageOK = writeImageToClient(fileName, "image/tiff");
		} else {
			imageOK = writeImageToClient(fileNamePdf, "application/pdf");			
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
    	
    	String book = link.replaceAll(".*book=([^&?]*).*", "$1");
    	String page = link.replaceAll(".*page=([^&?]*).*", "$1");
    	String instrumentNumber = link.replaceAll(".*instrno=([^&?]*).*", "$1");
    	
    	return GenericDASLTS.retrieveImage(book, page, instrumentNumber, fileName, mSearch, msSiteRealPath, false);
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
