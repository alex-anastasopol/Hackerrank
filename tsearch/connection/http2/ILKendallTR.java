package ro.cst.tsearch.connection.http2;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;

public class ILKendallTR extends HttpSite {
	
	public static final String PARCEL_NUMBER_PARAM = "parcel_number";
	public static final String NREL_PARAM = "nrel";
	public static final String OWNTYPE_PARAM = "owntype";
	public static final String TAXCODE_PARAM = "taxcode";
	public static final String TAXDISTRICT_PARAM = "taxdistrict";
	public static final String NEIGHBORHOOD_PARAM = "neighborhood";
	
	public static final ArrayList<String> PARAMS = new ArrayList<String>();
	static {
		PARAMS.add("search_method");
		PARAMS.add(PARCEL_NUMBER_PARAM);
		PARAMS.add("last_name");
		PARAMS.add("first_name");
		PARAMS.add("middle_initial");
		PARAMS.add("name_suffix");
		PARAMS.add("name_address_1");
		PARAMS.add("name_address_2");
		PARAMS.add("name_city");
		PARAMS.add("name_state");
		PARAMS.add("name_zip");
		PARAMS.add(NREL_PARAM);
		PARAMS.add(OWNTYPE_PARAM);
		PARAMS.add("house_number_low");
		PARAMS.add("house_number_high");
		PARAMS.add("prefix_directional");
		PARAMS.add("street_name");
		PARAMS.add("street_suffix");
		PARAMS.add("post_directional");
		PARAMS.add("community_name");
		PARAMS.add("state");
		PARAMS.add("zip");
		PARAMS.add(TAXCODE_PARAM);
		PARAMS.add(TAXDISTRICT_PARAM);
		PARAMS.add(NEIGHBORHOOD_PARAM);
		PARAMS.add("legal_desc");
	}
	
	private ArrayList<String> allNameRelationships = new ArrayList<String>();
	private ArrayList<String> allOwnerTypes = new ArrayList<String>();
	private ArrayList<String> allTaxCodes = new ArrayList<String>();
	private ArrayList<String> allTaxDistricts = new ArrayList<String>();
	private ArrayList<String> allNeighborhoods = new ArrayList<String>();
	
	public void populateList(String s, ArrayList<String> list) {
		String patt = "(?is)<option\\s+value=\"([^\"]+)\">";
		
		if (!ro.cst.tsearch.utils.StringUtils.isEmpty(s)) {
			Matcher ma = Pattern.compile(patt).matcher(s);
			while (ma.find()) {
				list.add(ma.group(1));
			}
		}
	}
	
	@Override
	public LoginResponse onLogin() {
		
		populateList(ro.cst.tsearch.servers.types.ILKendallTR.getALL_NAME_RELATIONSHIPS(), allNameRelationships);
		populateList(ro.cst.tsearch.servers.types.ILKendallTR.getALL_OWNER_TYPES(), allOwnerTypes);
		populateList(ro.cst.tsearch.servers.types.ILKendallTR.getALL_TAX_CODES(), allTaxCodes);
		populateList(ro.cst.tsearch.servers.types.ILKendallTR.getALL_TAX_DISTRICTS(), allTaxDistricts);
		populateList(ro.cst.tsearch.servers.types.ILKendallTR.getALL_NEIGHBORHOODS(), allNeighborhoods);
		
		String resp = "";
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		try {
			resp = exec(req).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if (resp.contains("Select a Search Method")) {
			return LoginResponse.getDefaultSuccessResponse();
		}
		
		return LoginResponse.getDefaultFailureResponse();
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			String url = req.getURL();
			if (url.contains("/search/process")) {
				if (url.contains("/wedge/search/process")) {	//Search by Parcel Number
					String parcelNumber = req.getPostFirstParameter(PARCEL_NUMBER_PARAM);
					if (parcelNumber!=null) {
						String newParcelNumber = parcelNumber.replaceAll("[^0-9]", "");
						if (newParcelNumber.length()>10) {
							newParcelNumber = newParcelNumber.substring(0, 10);
						} else if (newParcelNumber.length()<10) {
							newParcelNumber = StringUtils.rightPad(newParcelNumber, 10, '0');
						}
						if (newParcelNumber.matches("\\d{10}")) {
							newParcelNumber = newParcelNumber.replaceFirst("(\\d{2})(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
						}
						if (!newParcelNumber.equals(parcelNumber)) {
							req.removePostParameters(PARCEL_NUMBER_PARAM);
							req.setPostParameter(PARCEL_NUMBER_PARAM, newParcelNumber);
						}
					}
				} else {										//Advanced Search
					String p = getParams(req);
					req.setPostParameter("searchKey", sha1(p));
				}
			}
			
			//remove empty parameters 
			List<String> paramsToRemove = new ArrayList<String>();
			HashMap<String, ParametersVector> params = req.getPostParameters();
			Iterator<Entry<String, ParametersVector>> it = params.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, ParametersVector> entry = it.next();
				if (entry!=null) {
					String key = entry.getKey();
					ParametersVector value = entry.getValue();
					if (key!=null && value!=null) {
						if (value.size()==1 && "".equals(value.get(0))) {
							paramsToRemove.add(key);
						}
					}
				}
			}
			for (String p: paramsToRemove) {
				req.removePostParameters(p);
			}
			
		}
		
	}	
	
	public void addMultipleParams(HTTPRequest req, String paramName, StringBuilder sb, ArrayList<String> list) {
		ParametersVector vector = req.getPostParameter(paramName);
		if (vector!=null) {
			for (String s: list) {
				//if (s.matches("[a-zA-Z0-9]+")) {
					if (vector.contains(s)) {
						sb.append("{\"name\":\"").append(paramName).append("\",\"value\":\"").append(s).append("\"},");
					}
				//}
			}
		}
	}
	
	/**
	 * method equivalent to JSON.stringify
	 */
	public String getParams(HTTPRequest req) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<PARAMS.size();i++) {
			String name = PARAMS.get(i);
			if (NREL_PARAM.equals(name)) {
				addMultipleParams(req, name, sb, allNameRelationships);
			} else if (OWNTYPE_PARAM.equals(name)) {
				addMultipleParams(req, name, sb, allOwnerTypes);
			} else if (TAXCODE_PARAM.equals(name)) {
				addMultipleParams(req, name, sb, allTaxCodes);
			} else if (TAXDISTRICT_PARAM.equals(name)) {
				addMultipleParams(req, name, sb, allTaxDistricts);
			} else if (NEIGHBORHOOD_PARAM.equals(name)) {
				addMultipleParams(req, name, sb, allNeighborhoods);
			} else {
				String value = req.getPostFirstParameter(name);
				if (value!=null) {
					//if (value.matches("[a-zA-Z0-9]+")) {
						sb.append("{\"name\":\"").append(name).append("\",\"value\":\"").append(value).append("\"},");
					//}
				}
			}
		}
		String result = sb.toString();
		if (result.length()>0) {
			result = "[" + result;
		}
		result = result.replaceFirst(",$", "]");
		
		return result;
	}
	
	public static String sha1(String input) {
		StringBuffer sb = new StringBuffer();
		try {
			MessageDigest mDigest = MessageDigest.getInstance("SHA1");
			byte[] result = mDigest.digest(input.getBytes());
	        
	        for (int i = 0; i < result.length; i++) {
	            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
	        }
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
        return sb.toString();
    }
		
}
