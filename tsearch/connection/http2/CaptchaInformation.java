package ro.cst.tsearch.connection.http2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.stewart.ats.webservice.OCRService;
import com.stewart.ats.webservice.ocr.xsd.Image;
import com.stewart.ats.webservice.ocr.xsd.Output;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

public class CaptchaInformation extends HttpSite {

	private String	ocrString	= null;
	
	
	/**
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
	
	String getOcrString() {
		return ocrString;
	}


	void setOcrString(String ocrString) {
		this.ocrString = ocrString;
	}
	

	public String getCapchaAsString(int retryNumber, String[] imgFile, int textLength) {
		String capcha = null;
		logger.debug("Trying to get capcha as string...");
		for (int i = 0; i < retryNumber; i++) {
			capcha = getCapchaAsString(imgFile, textLength);
			if (capcha != null && (capcha.length() == 5 || capcha.length() == 7)) {
				break;
			}
		}
		logger.debug("Capcha retrieved as: [" + capcha + "]");
		return capcha;
	}

	

	public String getCapchaAsString(String[] imgFile, int textLength) {
		String infoFromOCR = this.getOcrString();
		
		if (imgFile != null) {
			logger.debug("Downloaded image [" + imgFile[1] + "] ... Trying to ocr");
			// we downloaded the image
			try {
				BufferedImage img = ImageIO.read(new File(imgFile[1]));
				img = removeGrid(img);
				ImageIO.write(img, "tiff", new File(imgFile[1] + ".tiff"));

				if (ServerConfig.getTesseractEnabledForCaptcha()) {
					this.setOcrString(ocrImage(imgFile[1] + ".tiff"));
				} else {
					String fileName = imgFile[1].substring(imgFile[1].lastIndexOf("."));
					this.setOcrString(ocrImage(FileUtils.readBinaryFile(fileName + ".tiff")));
				}
				infoFromOCR = this.getOcrString();
					
				if (StringUtils.isNotEmpty(infoFromOCR) && infoFromOCR.length() == textLength) {
					this.setOcrString(getOcrString().replaceAll("O", "0"));
					infoFromOCR = this.getOcrString();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.debug("Downloaded image [" + imgFile[1] + "] ... Ocr complete");
		}
		
		return infoFromOCR;
	}


	public BufferedImage removeGrid(BufferedImage img){
		ArrayList<Integer> x = new ArrayList<Integer>();
		ArrayList<Integer> y = new ArrayList<Integer>();
		Graphics2D graphic = img.createGraphics();
		graphic.setColor(new Color(255, 255, 255));
		
		//record the horizontal lines
		for(int i=1; i<img.getHeight()-1; i++){
			if (isBlack(img.getRGB(1, i))){
				y.add(i);
			}
		}
		//record the vertical lines
		for(int j =1; j < img.getWidth()-1; j++){
			if (isBlack(img.getRGB(j, 1))){
				x.add(j);
			}
		}
		//delete horizontal lines
		for (int i:y){
			for(int j =1; j < img.getWidth()-1; j++){
				if (!isBlack(img.getRGB(j, i-1) ) && !isBlack(img.getRGB(j, i+1))){
						img.setRGB(j, i, 0x00ffffff);
				}
			}
		}
		//delete vertical lines
		for (int j:x) {
			for (int i = 1; i < img.getHeight()-1; i++){
				if (!isBlack(img.getRGB(j-1, i) ) && !isBlack(img.getRGB(j+1, i))) {
						img.setRGB(j, i, 0x00ffffff);
				}			
			}
		}
		//delete margins
		graphic.drawLine(0, 0, img.getWidth(), 0);
		graphic.drawLine(0, 0, 0, img.getHeight()-1);
		graphic.drawLine(0, img.getHeight()-1, img.getWidth()-1, img.getHeight()-1);
		graphic.drawLine(img.getWidth()-1, 0, img.getWidth()-1, img.getWidth()-1);
		
		int blackInt = Color.BLACK.getRGB();
		int whiteInt = Color.WHITE.getRGB();
		for (int i = 1; i < img.getHeight()-1; i++){
			for(int j =1; j < img.getWidth()-1; j++){
				if (isAlmostBlack(img.getRGB(j, i))){
					img.setRGB(j, i, blackInt);
				} else {
					img.setRGB(j, i, whiteInt);
				}
			}
		}
		return img;
	}
	
	
	public static boolean isAlmostBlack(int rgb) {
		Color middle = new Color(128,128,128);
		if(middle.getRGB() < rgb) {
			return false;
		} 
		return true;
	}

	public boolean isBlack(int c) {
		int u = 50; //the max values of a byte
		int  r = (c & 0x00ff0000) >> 16;
		int  g = (c & 0x0000ff00) >> 8;
		int  b = c & 0x000000ff;
		
		return r < u && g < u && b <u;
	}
	
	
	public String ocrImage(byte[] img){
		Image image = Image.Factory.newInstance();
		image.setDescription("Captcha");
		image.setData(img);
		image.setType("tif");
		try {
			Output out = OCRService.process(image, false);;
			if(out.getSuccess()){							
				return processResult(out);
			}
		} catch (RemoteException e) {
			logger.debug("Error appeared at OCR process: ");
			e.printStackTrace();
		}
		return "";
	}
	
	public String ocrImage(String filename){
		 try {
		   File initialFile = new File(filename);
		   String newFilename = filename.substring(0,filename.indexOf(".")) + ".tif";
		   File tifFile = new File(newFilename);
		   org.apache.commons.io.FileUtils.copyFile(initialFile, tifFile);
		   String textFile = newFilename.replaceAll(".tif", "");
		   String[] exec = new String[3];
		   exec[0] = ServerConfig.getTesseractPath();
		   exec[1] = tifFile.getAbsolutePath();
		   exec[2] = textFile;
		   ClientProcessExecutor cpe = new ClientProcessExecutor( exec, true, true );
		            cpe.start();
		            String ocrdText = FileUtils.readTextFile(textFile+".txt");
		            return ocrdText;
		            
		  } catch (/*Remote*/Exception e) {
			  logger.debug("Error appeared at OCR process: ");
			  e.printStackTrace();
		  }
		  return "";
	 }
	
	
	public String processResult(Output out) {
		try {
			byte[] res = out.getResult();
			String outS = new String(res, 2, res.length -2, "UTF-8").replace(new String(new char[]{(char)0}), "");
			Document result = XmlUtils.parseXml(outS);
			String ocrRes = "";
			NodeList wd = result.getElementsByTagName("wd");
			for(int i =0; i< wd.getLength(); i++){
				if (wd.item(i).getFirstChild() != null){
					String nodeVal = wd.item(i).getFirstChild().getNodeValue();
					ocrRes += nodeVal;
				}
			}
			return ocrRes.replaceAll("\\s", "");
		} catch(Exception e){
			 e.printStackTrace();
			 return "";
		}
	}

}
