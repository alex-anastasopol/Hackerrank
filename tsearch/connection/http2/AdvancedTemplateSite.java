package ro.cst.tsearch.connection.http2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class AdvancedTemplateSite extends HttpSite {
	protected String[] mainParameters;
	protected String[] targetArgumentParameters;
	protected String formName;
	protected String mainParametersKey;
	protected String targetArgumentMiddleKey;
	protected boolean decodeParamValues;
	
	/**
     * login
     */
	public LoginResponse onLogin() {
		
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
		
		HTTPRequest request = new HTTPRequest(getSiteLink());
		String response = process( request ).getResponseAsString();		
		Map<String,String> addParams = fillAndValidateConnectionParams(response, mainParameters, formName);
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		if (decodeParamValues){
			Set<String> keySet = addParams.keySet();
			for (String key : keySet) {
				String value = addParams.get(key);
				try {
					value = URLDecoder.decode(value,"UTF-8");
					addParams.put(key, value);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		setAttribute(mainParametersKey, addParams);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String seq = req.getPostFirstParameter("seq");
		Map<String,String> addParams = null;
		String[] keysToUse = null;
		if(StringUtils.isNotEmpty(seq)) {
			req.removePostParameters("seq");
			addParams = (Map<String,String>)InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext()
				.getAdditionalInfo(getDataSite().getName() + targetArgumentMiddleKey + seq);
			keysToUse = addParams.keySet().toArray(new String[addParams.size()]);
			
		} else {
			addParams = (Map<String,String>)getAttribute(mainParametersKey);
				keysToUse = mainParameters;
		}
		
		if(StringUtils.isNotEmpty(req.getPostFirstParameter("noparams"))) {
			req.removePostParameters("noparams");
			return;
		}
		
		for (int i = 0; i < keysToUse.length; i++) {
			req.removePostParameters(keysToUse[i]);
			req.setPostParameter(keysToUse[i], addParams.get(keysToUse[i]));
		}
		
	}
	

	public String[] getMainParameters() {
		return mainParameters;
	}

	public void setMainParameters(String[] mainParameters) {
		this.mainParameters = mainParameters;
	}

	public String[] getTargetArgumentParameters() {
		return targetArgumentParameters;
	}

	public void setTargetArgumentParameters(String[] targetArgumentParameters) {
		this.targetArgumentParameters = targetArgumentParameters;
	}

	public String getFormName() {
		return formName;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}

	public String getMainParametersKey() {
		return mainParametersKey;
	}

	public void setMainParametersKey(String mainParametersKey) {
		this.mainParametersKey = mainParametersKey;
	}

	public String getTargetArgumentMiddleKey() {
		return targetArgumentMiddleKey;
	}

	public void setTargetArgumentMiddleKey(String targetArgumentMiddleKey) {
		this.targetArgumentMiddleKey = targetArgumentMiddleKey;
	}
	
	
}
