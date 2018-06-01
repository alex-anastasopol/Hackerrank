package ro.cst.tsearch.connection.http2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import javax.imageio.ImageIO;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.webservice.OCRService;
import com.stewart.ats.webservice.ocr.xsd.Image;
import com.stewart.ats.webservice.ocr.xsd.Output;

public class ILCookAO extends HttpSite {

	public static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	public static final String FORM_NAME = "aspnetForm";
	public static String			CAPTCHA_PAGE		= "http://www.cookcountyassessor.com/Captcha/CaptchaHandler.ashx";
	private static final int		MAX_RETRY_CAPTCHA	= 7;
	private static final int		CAPTCHA_LENGTH	= 6;
	public static final String CAPTCHA_OCR_FAILED_MESSAGE = "We had troubles getting the CAPTCHA image from server or performing OCR on it.";
	/**
     * login
     */
	public LoginResponse onLogin() {
		HTTPRequest request = new HTTPRequest(
				"http://www.cookcountyassessor.com/Property_Search/Property_Search.aspx",
				HTTPRequest.GET);
		String response = process( request ).getResponseAsString();		
		Map<String,String> addParams = fillAndValidateConnectionParams(
				response, new String[] { "__VIEWSTATE" }, FORM_NAME);
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("search.params", addParams);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if(req.getURL().contains("?pin=") && req.getURL().contains("property_details")) {
			req.setHeader("Referer", "http://www.cookcountyassessor.com/Property_Search/Property_Search_Results.aspx");
		} else {
			req.setHeader("Referer", "http://www.cookcountyassessor.com/Property_Search/Property_Search.aspx");
		}
		String seq = req.getPostFirstParameter("seq");
		Map<String,String> addParams = null;
		if(StringUtils.isNotEmpty(seq)) {
			req.removePostParameters("seq");
			req.setHeader("Referer", "http://www.cookcountyassessor.com/Property_Search/Property_Search_Results.aspx");
			addParams = (Map<String,String>)InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext()
				.getAdditionalInfo(getDataSite().getName() + ":params:" + seq);
		} else {
			addParams = (Map<String,String>)getAttribute("search.params");
		}
		
		
		
		for (int i = 0; i < REQ_PARAM_NAMES.length; i++) {
			req.removePostParameters(REQ_PARAM_NAMES[i]);
			req.setPostParameter(REQ_PARAM_NAMES[i], addParams.get(REQ_PARAM_NAMES[i]));
		}
		super.onBeforeRequestExcl(req);
	}

	public String[] getImage(HttpSite site) {
		String[] ret = new String[] { null, null };
		HTTPRequest request = new HTTPRequest(CAPTCHA_PAGE);
		HTTPResponse response = null;
		for (int i = 0; i < 3 && (response == null || response.getReturnCode() != 200); i++) {
			try {
				response = site.process(request);
			} finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}
		}
		// check that we'we got an image
		if (response != null && !response.getContentType().contains("image/gif")) {
			logger.error("Image was not downloaded!");
			return null;
		}
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		long fileId = System.nanoTime();
		String fileName = search.getSearchDir() + "temp" + File.separator + fileId + ".gif";
		ret[0] = String.valueOf(fileId);
		ret[1] = fileName;
		InputStream inputStream = response.getResponseAsStream();
		FileUtils.writeStreamToFile(inputStream, fileName);
		return ret;
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		String captchaParamName = "ctl00$BodyContent$CaptchaControl1$CaptchaTextBox";
		ParametersVector captchaParam = req.getPostParameter(captchaParamName);

		if (!res.getContentType().equals("image/gif") && captchaParam == null) {// if not getting the image or getting the page details(with ocr-ized captcha)
			// get the captcha, clean it, ocr it and get the details page:
			String instrumentNo = "";
			content = res.getResponseAsString();
			res.is = new ByteArrayInputStream(content.getBytes());
			boolean hasCaptchaImage = StringUtils.isNotEmpty(StringUtils.extractParameter(content, "(?is)(<img[^>]*CaptchaHandler\\.ashx[^>]*>)"));

			if (hasCaptchaImage) {
				boolean successCapcha = false;
				for (int j = 0; j < MAX_RETRY_CAPTCHA && !successCapcha; j++) {
					String[] fileName = getImage(this); // download the image
					if (fileName != null) {
						try {
							BufferedImage img = ImageIO.read(new File(fileName[1]));
							if (img != null) {
								// clean image, convert from gif to tif
								img = removeGrid(img);
								String tifFileName = fileName[1].substring(0, fileName[1].lastIndexOf(".")) + ".tif";
								ImageIO.write(img, "tif", new File(tifFileName));

								// do ocr
								String ocrString = "";
								for (int i = 0; i < MAX_RETRY_CAPTCHA && StringUtils.isEmpty(ocrString); i++) {
									if (ServerConfig.getTesseractEnabledForCaptcha()) {
										ocrString = ocrImage(tifFileName).replaceAll("(?i)[^A-Z\\d]", "");
									} else {
										ocrString = ocrImage(FileUtils.readBinaryFile(tifFileName));
									}
								}

								// captchas contain only letters or digits, and always have length= CAPTCHA_LENGTH - now it's 6
								if (!StringUtils.isEmpty(ocrString) && ocrString.length() == CAPTCHA_LENGTH && ocrString.matches("[A-Za-z\\d]+")) {
									// ocrString = ocrString.replaceAll("O", "0");

									// get the detail page
									// new req to http://www.cookcountyassessor.com/Captcha/VerificationPage.aspx?Pin=32291030140000
									String detailsURL = res.getLastURI().toString();
									instrumentNo = StringUtils.extractParameterFromUrl(detailsURL, "Pin");

									HTTPRequest request = new HTTPRequest(res.getLastURI().toString());
									int seq = ro.cst.tsearch.servers.types.ILCookAO.getSeq();
									request.setMethod(HTTPRequest.POST);
									request.setPostParameter("ctl00$BodyContent$CaptchaControl1$btnTest", "Check captcha");
									request.setPostParameter(captchaParamName, ocrString);
									setAttribute("onBeforeRequest", Boolean.FALSE);

									Map<String, String> params = HttpSite.fillAndValidateConnectionParams(
											content, REQ_PARAM_NAMES, FORM_NAME);
									getSearch().setAdditionalInfo(getDataSite().getName() + ":params:" + seq, params);
									request.setPostParameter("seq", String.valueOf(seq));
									HTTPResponse response = null;
									for (int i = 0; i < 3 && (response == null || response.getReturnCode() != 200); i++) {
										response = process(request);
									}

									if (response != null && response.getReturnCode() == 200) {
										content = response.getResponseAsString();
										response.is = new ByteArrayInputStream(content.getBytes());
										if (StringUtils.extractParameter(content, "(?is)(please\\s+enter\\s+the\\s+text\\s+in"
												+ "\\s+the\\s+box\\s+to\\s+the\\s+right\\s+to\\s+verify\\s+you\\s+are\\s+human)").isEmpty()) {
											successCapcha = true;
											res.is = response.is;
											res.returnCode = response.returnCode;
											res.body = response.body;
											res.contentLenght = response.contentLenght;
											res.contentType = response.contentType;
											res.headers = response.headers;
										}
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				if (!successCapcha) {
					String ocrFailedMessage = CAPTCHA_OCR_FAILED_MESSAGE + " Instrument number " + instrumentNo + " was not processed.";
					SearchLogger.logWithServerName(ocrFailedMessage, searchId, SearchLogger.ERROR_MESSAGE, getDataSite());
					res.is = new ByteArrayInputStream(ocrFailedMessage.getBytes());
				}
			}
		}
	}

	public static BufferedImage removeGrid(BufferedImage img) {
		ArrayList<Integer> x = new ArrayList<Integer>();
		ArrayList<Integer> y = new ArrayList<Integer>();
		Graphics2D graphic = img.createGraphics();
		graphic.setColor(new Color(255, 255, 255));

		// record the horizontal lines
		for (int i = 1; i < img.getHeight() - 1; i++) {
			if (isBlack(img.getRGB(1, i))) {
				y.add(i);
			}
		}
		// record the vertical lines
		for (int j = 1; j < img.getWidth() - 1; j++) {
			if (isBlack(img.getRGB(j, 1))) {
				x.add(j);
			}
		}
		// delete horizontal lines
		for (int i : y) {
			for (int j = 1; j < img.getWidth() - 1; j++) {
				if (!isBlack(img.getRGB(j, i - 1)) && !isBlack(img.getRGB(j, i + 1))) {
					img.setRGB(j, i, 0x00ffffff);
				}
			}
		}

		// delete vertical lines
		for (int j : x) {
			for (int i = 1; i < img.getHeight() - 1; i++) {
				if (!isBlack(img.getRGB(j - 1, i)) && !isBlack(img.getRGB(j + 1, i))) {
					img.setRGB(j, i, 0x00ffffff);
				}
			}
		}

		graphic.drawLine(0, 0, img.getWidth(), 0);
		graphic.drawLine(0, 0, 0, img.getHeight() - 1);
		graphic.drawLine(0, img.getHeight() - 1, img.getWidth() - 1,
				img.getHeight() - 1);
		graphic.drawLine(img.getWidth() - 1, 0, img.getWidth() - 1, img.getHeight() - 1);

		// transform image into a bi-level one (i.e. image binarization, fixed threshold)
		int blackInt = Color.BLACK.getRGB();
		int whiteInt = Color.WHITE.getRGB();
		for (int i = 1; i < img.getHeight() - 1; i++) {
			for (int j = 1; j < img.getWidth() - 1; j++) {
				if (isAlmostBlack(img.getRGB(j, i))) {
					img.setRGB(j, i, blackInt);
				} else {
					img.setRGB(j, i, whiteInt);
				}
			}
		}

		// filter isolated pixels
		img = filter(img, 1, 4, blackInt);
		img = filter(img, 1, 4, whiteInt);
		img = filter(img, 3, 6, blackInt);
		img = filter(img, 1, 6, whiteInt);

		return img;
	}

	private static BufferedImage filter(BufferedImage img, int times, int threshold, int colorInt) {
		for (int count = 1; count <= times; count++) {
			BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
			for (int i = 1; i < img.getHeight() - 1; i++) {
				for (int j = 1; j < img.getWidth() - 1; j++) {
					int colorCount = 0;
					if (img.getRGB(j - 1, i - 1) == colorInt)
						colorCount++;
					if (img.getRGB(j, i - 1) == colorInt)
						colorCount++;
					if (img.getRGB(j + 1, i - 1) == colorInt)
						colorCount++;
					if (img.getRGB(j - 1, i) == colorInt)
						colorCount++;
					if (img.getRGB(j + 1, i) == colorInt)
						colorCount++;
					if (img.getRGB(j - 1, i + 1) == colorInt)
						colorCount++;
					if (img.getRGB(j, i + 1) == colorInt)
						colorCount++;
					if (img.getRGB(j + 1, i + 1) == colorInt)
						colorCount++;

					if (colorCount >= threshold)
						newImg.setRGB(j, i, colorInt);
					else
						newImg.setRGB(j, i, img.getRGB(j, i));
				}
			}
			img = newImg;
		}
		return img;
	}

	private static boolean isAlmostBlack(int rgb) {
		Color middle = new Color(128, 128, 128);
		if (middle.getRGB() < rgb) {
			return false;
		}
		return true;
	}

	public static boolean isBlack(int c) {
		int u = 50; // the max values of a byte
		int r = (c & 0x00ff0000) >> 16;
		int g = (c & 0x0000ff00) >> 8;
		int b = c & 0x000000ff;
		return r < u && g < u && b < u;
	}

	public static String ocrImage(byte[] img) {
		Image image = Image.Factory.newInstance();
		image.setDescription("Captcha");
		image.setData(img);
		image.setType("tif");
		try {
			Output out = OCRService.process(image, false);
			;
			if (out.getSuccess()) {
				return processResult(out);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String ocrImage(String filename) {
		try {
			File initialFile = new File(filename);
			String textFile = filename.replaceFirst("\\.tif$", "");
			String[] exec = new String[3];
			exec[0] = ServerConfig.getTesseractPath();
			exec[1] = initialFile.getAbsolutePath();
			exec[2] = textFile;
			ClientProcessExecutor cpe = new ClientProcessExecutor(exec, true, true);
			cpe.start();
			String ocrdText = FileUtils.readTextFile(textFile + ".txt");
			return ocrdText;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String processResult(Output out) {
		try {
			byte[] res = out.getResult();
			String outS = new String(res, 2, res.length - 2, "UTF-8").replace(new String(new char[] { (char) 0 }), "");
			Document result = XmlUtils.parseXml(outS);
			String ocrRes = "";
			NodeList wd = result.getElementsByTagName("wd");
			for (int i = 0; i < wd.getLength(); i++) {
				if (wd.item(i).getFirstChild() != null) {
					String nodeVal = wd.item(i).getFirstChild().getNodeValue();
					ocrRes += nodeVal;
				}
			}
			return ocrRes.replaceAll("\\s", "");
		} catch (Exception e) {
			return "";
		}
	}

	public static void main(String args[]) {
		String CAPCHAS_PLACE = "C:\\Users\\Alex\\Desktop\\Captchas\\";
		BufferedImage img = null;
		long total = 0;
		int i;
		File dir = new File(CAPCHAS_PLACE);
		try {
			String[] fileNames = dir.list();

			for (i = 0; i < fileNames.length; i++) {
				long start = (new Date()).getTime();
				img = ImageIO.read(new File(CAPCHAS_PLACE + fileNames[i]));
				System.out.printf("Processing [ %s ] ...\n", fileNames[i]);
				img = removeGrid(img);
				String newFileName = CAPCHAS_PLACE + fileNames[i].substring(0, fileNames[i].lastIndexOf(".")) + "done.tif";
				ImageIO.write(img, "tif", new File(newFileName));
				// ImageIO.write(img, "tiff", new File(CAPCHAS_PLACE + fileNames[i] + ".tiff"));
				String out = ocrImage(newFileName);
				System.out.println("OCR Result:" + out);
				long end = (new Date()).getTime();
				start = (end - start) / 1000;
				total += start;
				System.out.println(i + " - " + start);
			}
			long average = total / i;
			System.out.println("Average Time:" + average);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(img.getTileWidth() + " x " + img.getTileHeight());
	}
}
