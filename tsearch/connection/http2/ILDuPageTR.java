package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.webservice.OCRService;
import com.stewart.ats.webservice.ocr.xsd.Image;
import com.stewart.ats.webservice.ocr.xsd.Output;

/**
 * 
 * @author Oprina George
 * 
 *         Jun 8, 2011
 */

public class ILDuPageTR extends HttpSite {
	private static final String[] PARAM_NAMES = { "__EVENTTARGET",
			"__EVENTARGUMENT", "__VIEWSTATE", "__EVENTVALIDATION" };

	private static final String FORM_NAME = "aspnetForm";

	private static final int MAX_RETRY_CAPTCHA = 5;

	private Map<String, String> params = null;

	private Map<String, String> initial_params = null;

	@Override
	public LoginResponse onLogin() {
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		DataSite dataSite = getDataSite();

		HTTPRequest request = new HTTPRequest(dataSite.getLink());

		String responseAsString = execute(request);

		if (responseAsString != null
				&& responseAsString.contains("Property Lookup")) {
			// save initial params
			params = new SimpleHtmlParser(responseAsString).getForm(FORM_NAME).getParams();
			initial_params = new SimpleHtmlParser(responseAsString).getForm(FORM_NAME).getParams();
			return LoginResponse.getDefaultSuccessResponse();
		} else
			return LoginResponse.getDefaultFailureResponse();
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == POST) {
			if (req.getURL().contains("PropertyLookUp.aspx") || req.getURL().contains("PropertyLookup.aspx")) {
				if (initial_params != null) {
					for (String s : PARAM_NAMES) {
						if (initial_params.containsKey(s))
							req.setPostParameter(s, initial_params.get(s));
					}
				}
				
				if (req.getPostFirstParameter("ctl00$pageContent$ctl00$hdn") != null) {
					req.removePostParameters("ctl00$pageContent$ctl00$hdn");
				}
			} else if (req.getURL().contains("PropertyInformation.aspx")) {
				// get capcha
				String PIN = req.getPostFirstParameter("PIN");

				while (true) {
					HTTPRequest request = new HTTPRequest(req.getURL()+ "?PIN=" + PIN, GET);

					String res = execute(request);

					params = new SimpleHtmlParser(res).getForm(FORM_NAME).getParams();

					String captcha = getCapchaAsString();

					captcha = captcha.replaceAll("[:=]", "");

					String sum = GenericFunctions1.sum(captcha, searchId);

					if (!sum.equals("0")) {
						params.put("captcha", sum);
						break;
					}

				}
				// remove other params & putcapcha

				/*
				 * req.removePostParameters("ctl00$pageContent$ctl00$txtParcel");
				 * req
				 * .removePostParameters("ctl00$pageContent$ctl00$txtStreetNumber"
				 * );
				 * req.removePostParameters("ctl00$pageContent$ctl00$txtDirection"
				 * );
				 * req.removePostParameters("ctl00$pageContent$ctl00$txtStreet"
				 * );
				 * req.removePostParameters("ctl00$pageContent$ctl00$txtNumber"
				 * );
				 * req.removePostParameters("ctl00$pageContent$ctl00$txtCity");
				 * req.removePostParameters("ctl00$pageContent$ctl00$hdn");
				 * req.removePostParameters("ctl00$txtSiteSearch");
				 * req.removePostParameters
				 * ("ctl00$pageContent$ctl00$btnSearch");
				 */

				req.setURL(req.getURL() + "?PIN=" + PIN);
				req.removePostParameters("PIN");

				req.setPostParameter("ctl00$pageContent$ctl00$WaterMark_ClientState", "");
				req.setPostParameter("ctl00$pageContent$ctl00$txtCaptcha", params.get("captcha") == null ? "" : params.get("captcha"));
				req.setPostParameter("ctl00$txtSiteSearch", "SEARCH SITE");
				req.setPostParameter("ctl00$pageContent$ctl00$btnSubmit", "Submit");

				if (params != null) {
					for (String s : PARAM_NAMES) {
						if (params.containsKey(s))
							req.setPostParameter(s, params.get(s));
					}
				}
			}

		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (req.getMethod() == GET && req.getURL().contains("PropertyInformation.aspx")) {
			// save capcha get params
		}
	}

	@SuppressWarnings("unused")
	private String getCapchaAsString(int retryNumber) {
		String capcha = null;
		logger.debug("Trying to get capcha as string...");
		for (int i = 0; i < retryNumber; i++) {
			capcha = getCapchaAsString();
			if (capcha != null && capcha.length() == 5) {
				break;
			}
		}
		logger.debug("Capcha retrieved as: [" + capcha + "]");
		return capcha;
	}

	private String getCapchaAsString() {
		String ocrString = null;

		String[] fileName = getImage(this);
		if (fileName != null) {
			logger.debug("Downloaded image [" + fileName[1]
					+ "] ... Trying to ocr");
			// we downloaded the image
			try {
				BufferedImage img = ImageIO.read(new File(fileName[1]));
				ImageIO.write(img, "tiff", new File(fileName[1] + ".tiff"));
				ImageIO.write(img, "tiff", new File("test.tiff"));

				for (int i = 0; i < MAX_RETRY_CAPTCHA
						&& StringUtils.isEmpty(ocrString); i++) {
					if (ServerConfig.getTesseractEnabledForCaptcha()) {
						ocrString = ocrImage(fileName[1] + ".tiff");
					} else {
						ocrString = ocrImage(FileUtils
								.readBinaryFile(fileName[1] + ".tiff"));
					}
				}
				if (!StringUtils.isEmpty(ocrString) && ocrString.length() == 5) {
					ocrString = ocrString.replaceAll("O", "0");

				}
				// do requests
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.debug("Downloaded image [" + fileName[1]
					+ "] ... Ocr complete");
		}
		return ocrString;
	}

	public String[] getImage(HttpSite site) {
		String[] ret = new String[] { null, null };
		HTTPRequest request = new HTTPRequest(
				"http://www.dupageco.org/Captcha.aspx");
		HTTPResponse response = null;
		for (int i = 0; i < 3
				&& (response == null || response.getReturnCode() != 200); i++) {
			try {
				response = site.process(request);
			} finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}
		}
		// check that we'we got an image
		if (response != null
				&& !response.getContentType().contains("Image/jpeg")) {
			logger.error("Image was not downloaded!");
			return null;
		}
		Search search = InstanceManager.getManager()
				.getCurrentInstance(searchId).getCrtSearchContext();

		long fileId = System.nanoTime();
		String fileName = search.getSearchDir() + "temp" + File.separator
				+ fileId + ".jpg";
		ret[0] = String.valueOf(fileId);
		ret[1] = fileName;
		InputStream inputStream = response.getResponseAsStream();
		FileUtils.writeStreamToFile(inputStream, fileName);
		return ret;
	}

	public static String ocrImage(byte[] img) {
		Image image = Image.Factory.newInstance();
		image.setDescription("Captcha");
		image.setData(img);
		image.setType("tif");
		try {
			Output out = OCRService.process(image, false);
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
			String newFilename = filename.substring(0, filename.indexOf("."))
					+ ".tif";
			File tifFile = new File(newFilename);
			org.apache.commons.io.FileUtils.copyFile(initialFile, tifFile);
			String textFile = newFilename.replaceAll(".tif", "");
			String[] exec = new String[3];
			exec[0] = ServerConfig.getTesseractPath();
			exec[1] = tifFile.getAbsolutePath();
			exec[2] = textFile;
			ClientProcessExecutor cpe = new ClientProcessExecutor(exec, true,
					true);
			cpe.start();
			String ocrdText = FileUtils.readTextFile(textFile + ".txt");
			return ocrdText;
		} catch (/* Remote */Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String processResult(Output out) {
		try {
			byte[] res = out.getResult();
			String outS = new String(res, 2, res.length - 2, "UTF-8").replace(
					new String(new char[] { (char) 0 }), "");
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
}
