package ro.cst.tsearch.connection.http2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;

public class FLGilchristTR extends AdvancedTemplateSite {
	public static final String	MULTIPART_BOUNDARY	= "http.method.multipart.boundary";

	public FLGilchristTR() {
		mainParameters = new String[1];
		mainParameters[0] = "__VIEWSTATE";
		mainParametersKey = "search.params";
		formName = "Form";

		targetArgumentMiddleKey = ":params:";
		targetArgumentParameters = new String[1];
		targetArgumentParameters[0] = "__VIEWSTATE";
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {
			super.onBeforeRequestExcl(req);
			HashMap<String, HTTPRequest.ParametersVector> params = req.getPostParameters();

			if (params != null) {
				List<Part> parts = new ArrayList<Part>();

				for (Map.Entry<String, ParametersVector> entry : params.entrySet()) {
					Object valueObj = entry.getValue();
					String value = "";
					if (valueObj != null) {
						value = valueObj.toString();
					}

					StringPart sp = new StringPart(entry.getKey(), value);
					sp.setTransferEncoding(null);
					parts.add(sp);
				}
				req.setPartPostData(parts.toArray(new Part[0]));
			}
		}
	}
	
	@Override
	protected void setMultipartRequestEntity(HTTPRequest request, HttpMethodBase method) {
		HttpMethodParams methodParams = method.getParams();
		methodParams.setParameter(MULTIPART_BOUNDARY, "------WebKitFormBoundarycetDAGyEJnGR351Z");
		method.setParams(methodParams);
		
		super.setMultipartRequestEntity(request, method);
	}
}
