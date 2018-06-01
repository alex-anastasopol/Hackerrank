package ro.cst.tsearch.connection.http2;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;



/**
 * @author mihaib
*/

public class GenericConnWP extends HttpSite{

	@Override
	public LoginResponse onLogin() {
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, false);
		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		HTTPResponse response = process(req);
		String rsp = response.getResponseAsString();
		
		if (rsp.contains("<title>Alert</title>") && rsp.contains("Please confirm that you are human by typing the numbers shown below into the box")) {
			//we need to add a captcha code to continue searching
			//req: GET http://whitepages.addresses.com/inc/randomimage.php
			boolean successCapcha = false; 
//			CaptchaInformation captchaInfo = new CaptchaInformation();
//			String urlForCaptcha = "http://whitepages.addresses.com/inc/randomimage.php";
//			String imgType = "png";
//			String[] imgFile = getCaptchaImage(urlForCaptcha, imgType);
//			String imgAsString = captchaInfo.getCapchaAsString(imgFile, 6); 
//			try{
//				if (!StringUtils.isEmpty(imgAsString) && imgAsString.length() == 6) {
//					HTTPRequest request = new HTTPRequest(req.getURL());
//					request.setPostParameter("randomText", imgAsString);
//					request.setPostParameter("randomTextButton", "Submit");
//					request.setMethod(HTTPRequest.POST);
//
//					HTTPResponse newResponse = null;
//					for (int i = 0; i < 3 && (newResponse == null || newResponse.getReturnCode() != 200); i++){
//						newResponse = process(request);
//					}
//						
//					if (newResponse != null && newResponse.getReturnCode() == 200) {
//						String content =  newResponse.getResponseAsString();
//						response.is = new ByteArrayInputStream(content.getBytes());
//					}
//				}
//			} catch (Exception e){
//				e.printStackTrace();
//				logger.debug("Captcha image coundln't be sent to official site;  error: " + e);
//			}
			if (!successCapcha){
				SearchLogger.logWithServerName("We had troubles getting the CAPTCHA image from server or OCR-ing it.", searchId, 1, getDataSite());
			}
		}
		
    	return;
	}	
	
	
	/**
	 * 
	 * @param site  - current HttpSite object
	 * @param url   - link to follow to grab captcha image
	 * @param imgType  - type of image (jpg, jpeg, png, bmp,..)
	 * @return String[]  - fileID of search and name of downloaded image file
	 * 
	 */
	public String[] getCaptchaImage(String url, String imgType) {
		String[] ret = new String[] { null, null };
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				
		long fileId = System.nanoTime();
		String fileExt = "";
		if ("jpg".equals(imgType) || "jpg".equals(imgType))
			fileExt = ".jpg";
		else
			fileExt = "." + imgType;
			
		String fileName = search.getSearchDir() + "temp" + File.separator + fileId + fileExt;
		ret[0] = String.valueOf(fileId);
		ret[1] = fileName;
		try {
			BufferedImage img = ImageIO.read(new URL(url));
	        TIFFImageWriterSpi tiffspi = new TIFFImageWriterSpi();
	        ImageWriter writer = tiffspi.createWriterInstance();
	        
	        ImageWriteParam param = writer.getDefaultWriteParam();
	        File fOutputFile = new File(fileName);
	        ImageOutputStream ios = ImageIO.createImageOutputStream(fOutputFile);
	        writer.setOutput(ios);
	        writer.write(null, new IIOImage(img, null, null), param);
	        
		} catch (IOException e){
			e.printStackTrace();
		}
		
    	return ret;
	}
	
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		//if(status != STATUS_LOGGING_IN && (res == null || res.returnCode == 0)){
		destroySession();    		
	}
	
}