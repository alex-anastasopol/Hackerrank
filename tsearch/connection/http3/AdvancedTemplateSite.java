package ro.cst.tsearch.connection.http3;

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

public class AdvancedTemplateSite extends HttpSite3 {
	protected String[]	MAIN_PARAMETERS;
	protected String[]	TARGET_ARGUMENT_PARAMETERS;
	protected String	FORM_NAME;
	protected String	MAIN_PARAMETERS_KEY;
	protected String	TARGET_ARGUMENT_MIDDLE_KEY;
	protected boolean	DECODE_PARAM_VALUES;

	/**
	 * login
	 */
	public LoginResponse onLogin() {

		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", easyhttps);

		HTTPRequest request = new HTTPRequest(getSiteLink());
		String response = process(request).getResponseAsString();
		Map<String, String> addParams = fillAndValidateConnectionParams(response, MAIN_PARAMETERS, FORM_NAME);
		if (addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		if (DECODE_PARAM_VALUES) {
			Set<String> keySet = addParams.keySet();
			for (String key : keySet) {
				String value = addParams.get(key);
				try {
					value = URLDecoder.decode(value, "UTF-8");
					addParams.put(key, value);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		setAttribute(MAIN_PARAMETERS_KEY, addParams);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String seq = req.getPostFirstParameter("seq");
		Map<String, String> addParams = null;
		String[] keysToUse = null;
		if (StringUtils.isNotEmpty(seq)) {
			req.removePostParameters("seq");
			addParams = (Map<String, String>) InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext()
					.getAdditionalInfo(getDataSite().getName() + TARGET_ARGUMENT_MIDDLE_KEY + seq);
			keysToUse = addParams.keySet().toArray(new String[addParams.size()]);

		} else {
			addParams = (Map<String, String>) getAttribute(MAIN_PARAMETERS_KEY);
			keysToUse = MAIN_PARAMETERS;
		}

		if (StringUtils.isNotEmpty(req.getPostFirstParameter("noparams"))) {
			req.removePostParameters("noparams");
			return;
		}

		for (int i = 0; i < keysToUse.length; i++) {
			req.removePostParameters(keysToUse[i]);
			req.setPostParameter(keysToUse[i], addParams.get(keysToUse[i]));
		}

	}

	public String[] getMainParameters() {
		return MAIN_PARAMETERS;
	}

	public void setMainParameters(String[] mainParameters) {
		this.MAIN_PARAMETERS = mainParameters;
	}

	public String[] getTargetArgumentParameters() {
		return TARGET_ARGUMENT_PARAMETERS;
	}

	public void setTargetArgumentParameters(String[] targetArgumentParameters) {
		this.TARGET_ARGUMENT_PARAMETERS = targetArgumentParameters;
	}

	public String getFormName() {
		return FORM_NAME;
	}

	public void setFormName(String formName) {
		this.FORM_NAME = formName;
	}

	public String getMainParametersKey() {
		return MAIN_PARAMETERS_KEY;
	}

	public void setMainParametersKey(String mainParametersKey) {
		this.MAIN_PARAMETERS_KEY = mainParametersKey;
	}

	public String getTargetArgumentMiddleKey() {
		return TARGET_ARGUMENT_MIDDLE_KEY;
	}

	public void setTargetArgumentMiddleKey(String targetArgumentMiddleKey) {
		this.TARGET_ARGUMENT_MIDDLE_KEY = targetArgumentMiddleKey;
	}

}
