package ro.cst.tsearch.connection.http3;

import static ro.cst.tsearch.connection.http.HTTPRequest.POST;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.w3c.dom.Node;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

@SuppressWarnings("deprecation")
public class GenericCL extends HttpSite3 {
	
	private static final String XML_PATH = BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "resource" + File.separator + "CL" + File.separator;
	private static final String XML_SUBJECT_PATH = XML_PATH + "CLSubjectRequest.xml";
	private static final String XML_RANGE_PATH = XML_PATH + "CLRangeRequest.xml";
	private static final String XML_LIEN_PATH = XML_PATH + "CLLienRequest.xml";
	
	private static String xmlSubjectRequest = "";;
	private static String xmlRangeRequest = "";
	private static String xmlLienRequest = "";
	
	private String user = "";
	private String password = "";
	
	@Override
	public LoginResponse onLogin() {
		
		//get user name and password from database
		user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericCL", "user");
		password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericCL", "password");
		
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		if (StringUtils.isEmpty(xmlSubjectRequest)) {
			xmlSubjectRequest = FileUtils.readFile(XML_SUBJECT_PATH);
			if (StringUtils.isEmpty(xmlSubjectRequest)) {
				return LoginResponse.getDefaultFailureResponse();
			}
		}
		
		if (StringUtils.isEmpty(xmlRangeRequest)) {
			xmlRangeRequest = FileUtils.readFile(XML_RANGE_PATH);
			if (StringUtils.isEmpty(xmlRangeRequest)) {
				return LoginResponse.getDefaultFailureResponse();
			}
		}
		
		if (StringUtils.isEmpty(xmlLienRequest)) {
			xmlLienRequest = FileUtils.readFile(XML_LIEN_PATH);
			if (StringUtils.isEmpty(xmlLienRequest)) {
				return LoginResponse.getDefaultFailureResponse();
			}
		}
		
		return LoginResponse.getDefaultSuccessResponse();
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		String url = req.getURL();
		
		String xmlPostData = "";
		if (url.contains("lien.aspx")) {
			xmlPostData = xmlLienRequest;
			
			url = url.replaceFirst("lien.aspx$", "");
			req.modifyURL(url);
			
			String _StreetAddress = req.getPostFirstParameter("_StreetAddress");
			String _City = req.getPostFirstParameter("_City");
			String _State = req.getPostFirstParameter("_State");
			String _PostalCode = req.getPostFirstParameter("_PostalCode");
			
			String currentState = getDataSite().getStateAbbreviation();
			if ("".equals(_State)) {		//request from parent site
				_State = currentState;
			}
			
			xmlPostData = xmlPostData.replaceFirst("@@@_StreetAddress@@@", _StreetAddress==null?"":_StreetAddress);
			xmlPostData = xmlPostData.replaceFirst("@@@_City@@@", _City==null?"":_City);
			xmlPostData = xmlPostData.replaceFirst("@@@_State@@@", _State==null?"":_State);
			xmlPostData = xmlPostData.replaceFirst("@@@_PostalCode@@@", _PostalCode==null?"":_PostalCode);
		} else if (url.contains("range.aspx")) {
			xmlPostData = xmlRangeRequest;
			
			url = url.replaceFirst("range.aspx$", "");
			req.modifyURL(url);
			
			String _CountyFIPSCode = getDataSite().getStateFIPS() + getDataSite().getCountyFIPS();
			xmlPostData = xmlPostData.replaceFirst("@@@_CountyFIPSCode@@@", _CountyFIPSCode);
			
			String _HouseNumberFrom = req.getPostFirstParameter("_HouseNumberFrom");
			String _HouseNumberTo = req.getPostFirstParameter("_HouseNumberTo");
			String _StreetName = req.getPostFirstParameter("_StreetName");
			String _StreetSuffix = req.getPostFirstParameter("_StreetSuffix");
			
			xmlPostData = xmlPostData.replaceFirst("@@@_HouseNumberFrom@@@", _HouseNumberFrom==null?"":_HouseNumberFrom);
			xmlPostData = xmlPostData.replaceFirst("@@@_HouseNumberTo@@@", _HouseNumberTo==null?"":_HouseNumberTo);
			xmlPostData = xmlPostData.replaceFirst("@@@_StreetName@@@", _StreetName==null?"":_StreetName);
			xmlPostData = xmlPostData.replaceFirst("@@@_StreetSuffix@@@", _StreetSuffix==null?"":_StreetSuffix);
		} else {
			xmlPostData = xmlSubjectRequest;
			
			url = url.replaceFirst("details.aspx$", "");
			req.modifyURL(url);
			
			String _CountyFIPSCode = getDataSite().getStateFIPS() + getDataSite().getCountyFIPS();
			xmlPostData = xmlPostData.replaceFirst("@@@_CountyFIPSCode@@@", _CountyFIPSCode);
			
			String _OwnerFirstName = req.getPostFirstParameter("_OwnerFirstName");
			String _OwnerLastName = req.getPostFirstParameter("_OwnerLastName");
			
			String _StandardizedHouseNumber = req.getPostFirstParameter("_StandardizedHouseNumber");
			String _StreetName = req.getPostFirstParameter("_StreetName");
			String _StreetSuffix = req.getPostFirstParameter("_StreetSuffix");
			String _DirectionPrefix = req.getPostFirstParameter("_DirectionPrefix");
			String _DirectionSuffix = req.getPostFirstParameter("_DirectionSuffix");
			String _ApartmentOrUnit = req.getPostFirstParameter("_ApartmentOrUnit");
			
			String _AssessorsParcelIdentifier = req.getPostFirstParameter("_AssessorsParcelIdentifier");
			
			xmlPostData = xmlPostData.replaceFirst("@@@_OwnerFirstName@@@", _OwnerFirstName==null?"":_OwnerFirstName);
			xmlPostData = xmlPostData.replaceFirst("@@@_OwnerLastName@@@", _OwnerLastName==null?"":_OwnerLastName);
			
			xmlPostData = xmlPostData.replaceFirst("@@@_StandardizedHouseNumber@@@", _StandardizedHouseNumber==null?"":_StandardizedHouseNumber);
			xmlPostData = xmlPostData.replaceFirst("@@@_StreetName@@@", _StreetName==null?"":_StreetName);
			xmlPostData = xmlPostData.replaceFirst("@@@_StreetSuffix@@@", _StreetSuffix==null?"":_StreetSuffix);
			xmlPostData = xmlPostData.replaceFirst("@@@_DirectionPrefix@@@", _DirectionPrefix==null?"":_DirectionPrefix);
			xmlPostData = xmlPostData.replaceFirst("@@@_DirectionSuffix@@@", _DirectionSuffix==null?"":_DirectionSuffix);
			xmlPostData = xmlPostData.replaceFirst("@@@_ApartmentOrUnit@@@", _ApartmentOrUnit==null?"":_ApartmentOrUnit);
			
			xmlPostData = xmlPostData.replaceFirst("@@@_AssessorsParcelIdentifier@@@", _AssessorsParcelIdentifier==null?"":_AssessorsParcelIdentifier);
		}
		
		xmlPostData = xmlPostData.replaceFirst("@@@LoginAccountIdentifier@@@", user);
		xmlPostData = xmlPostData.replaceFirst("@@@LoginAccountPassword@@@", password);
		
		req.setXmlPostData(xmlPostData);
		
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (req.getMethod()==HTTPRequest.POST) {
			String source = req.getPostFirstParameter("source");
			if (source!=null) {										//it is a request for details
				String stringResponse = res.getResponseAsString();
				boolean isAnotherResponse = false;
				Node xmlDoc = null;
				try {
					xmlDoc = XmlUtils.parseXml(stringResponse.replaceFirst("(?is)<!DOCTYPE\\b.*?>", ""));
				} catch (RuntimeException e) {
					logger.error("XML parsing exception", e);
				}
				if (xmlDoc!=null) {
					String responseCode = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PRODUCT[1]/STATUS/@_Code");
					if ("0315".equals(responseCode)) {				//NO RECORDS FOUND FOR SEARCH CRITERIA SUBMITTED.
						String _OwnerLastName = req.getPostFirstParameter("_OwnerLastName");
						String _OwnerFirstName = req.getPostFirstParameter("_OwnerFirstName");
						if (!StringUtils.isEmpty(_OwnerLastName)||!StringUtils.isEmpty(_OwnerFirstName)) {
							//try without name, which may have been parsed wrong
							HTTPRequest reqRepeated = new HTTPRequest(req.getURL(), POST);
							HashMap<String, ParametersVector> params = new HashMap<String, ParametersVector>(req.getPostParameters());
							params.remove("_OwnerLastName");
							params.remove("_OwnerFirstName");
							reqRepeated.setPostParameters(params);
							HTTPResponse resRepeated = process(reqRepeated);
							res.is = new ByteArrayInputStream(resRepeated.getResponseAsByte());
							res.contentLenght= resRepeated.contentLenght;
							res.contentType = resRepeated.contentType;
							res.headers = resRepeated.headers;
							res.returnCode = resRepeated.returnCode;
							res.body = resRepeated.body;
							res.setLastURI(resRepeated.getLastURI());
							isAnotherResponse = true;
						} else {
							String _StreetName = req.getPostFirstParameter("_StreetName");
							String _StandardizedHouseNumber = req.getPostFirstParameter("_StandardizedHouseNumber");
							if (!StringUtils.isEmpty(_StreetName)||!StringUtils.isEmpty(_StandardizedHouseNumber)) {
								//try without address, which may have been parsed wrong
								HTTPRequest reqRepeated = new HTTPRequest(req.getURL(), POST);
								HashMap<String, ParametersVector> params = new HashMap<String, ParametersVector>(req.getPostParameters());
								params.remove("_StreetName");
								params.remove("_StandardizedHouseNumber");
								reqRepeated.setPostParameters(params);
								HTTPResponse resRepeated = process(reqRepeated);
								res.is = new ByteArrayInputStream(resRepeated.getResponseAsByte());
								res.contentLenght= resRepeated.contentLenght;
								res.contentType = resRepeated.contentType;
								res.headers = resRepeated.headers;
								res.returnCode = resRepeated.returnCode;
								res.body = resRepeated.body;
								res.setLastURI(resRepeated.getLastURI());
								isAnotherResponse = true;
							}
						}
					}
				}
				if (!isAnotherResponse) {
					res.is = new ByteArrayInputStream(stringResponse.getBytes());
				}
			}
		}
	}
	
	public String getPageWithPost(String link) {
		HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
		try {
			String url = req.getURL();
			int pos = url.indexOf("?");
			if (pos!=-1) {
				String param = url.substring(pos+1);
				String[] split = param.split("&");
				for (String s: split) {
					String[] spl = s.split("=");
					if (spl.length==2) {
						req.setPostParameter(spl[0], URLDecoder.decode(spl[1], "UTF-8"));
					} else if (spl.length==1) {
						req.setPostParameter(spl[0], "");
					} 
				}
				url = url.substring(0, pos);
				req.modifyURL(url);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		for(int i=0; i<3; i++){
			try {
				return execute(req);
			} catch (Exception e){
				logger.warn("Could not bring link:" + link, e);
			}
		}
		
		return "";
	}
	
	@Override
	protected void logPostData(HTTPRequest request, StringBuffer sb) {
		logger.info("   POST PARAMS: " + StringUtils.getMaxCharacters(sb.toString(), HTTPSiteInterface.MAX_CHARACTERS_TO_LOG_ON_INFO));
		if (!"".equals(request.getEntity())) {
			logger.info("   POST DATA: " + request.getEntity());
		}
	}
	
	@Override
	protected void addSpecificSiteProxy() {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
        	HttpHost proxy = new HttpHost("192.168.92.55", 8080);
        	getHttpClient().getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
        	
	        /* Trust unsigned ssl certificates when using proxy */
        	Scheme http = new Scheme("http", 80, PlainSocketFactory.getSocketFactory());
    		SchemeRegistry sr = getHttpClient().getConnectionManager().getSchemeRegistry();
    		sr.register(http);
        }
	}
	
}
