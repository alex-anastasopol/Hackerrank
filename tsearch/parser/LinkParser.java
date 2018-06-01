package ro.cst.tsearch.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;

import ro.cst.tsearch.connection.http.HTTPRequest;

/**
 * 
 * @author Cornel this Class Parse URL Link
 * 
 */
public class LinkParser {
	private String protocol = "";
	private String link = "";
	private String param = "";
	private LinkedList<Para> parameter = null;

	/**
	 * 
	 * @param linkTot
	 *            URL link contains protocol, address and parameter
	 */

	public LinkParser(String linkTot){
		this.parameter = new LinkedList<Para>();
		this.protocol = "";
		this.link = "";
		this.param = "";
		if(linkTot.contains("://")){
			this.protocol = linkTot.substring(0, linkTot.indexOf("://") + 3);
			linkTot = linkTot.substring(linkTot.indexOf("://") + 3, linkTot.length());
		}
		if(linkTot.contains("?")){
			this.link = linkTot.substring(0, linkTot.indexOf("?"));
			this.param = linkTot.substring(linkTot.indexOf("?") + 1, linkTot.length());
			setParameter(param.replaceAll("\\?", "&"));
		} else{
			this.link = linkTot;
		}
		
	}
	//clasa de parsat link
	public String toStringParam(String val1, String val2, String val3) {
		String tmp = "";
		for (int i = 0; i < this.parameter.size(); i++) {
			tmp += val1 + this.parameter.get(i).name + val2 + this.parameter.get(i).value + val3;
		}
		return tmp;
	}

	public String getParamValue(String name) {

		for (int i = 0; i < this.parameter.size(); i++) {
			if (name.equalsIgnoreCase(this.parameter.get(i).name)) {
				return this.parameter.get(i).value;
			}
		}
		return "";
	}

	/**
	 * 
	 * @param name
	 *            Delete parameter From the list parameter
	 */
	public void removeParameter(String name) {
		for (int i = 0; i < this.parameter.size(); i++) {
			if (name.equalsIgnoreCase(this.parameter.get(i).name)) {
				this.parameter.remove(i);
				return;
			}
		}

	}

	public void setLink(String link) {
		this.link = link;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@SuppressWarnings("rawtypes")
	public void AddParameter(HashMap map) {
		Object param[] = map.keySet().toArray();
		for (int k = 0; k < param.length; k++) {

			try {
				this.addParameter(
						URLDecoder.decode(param[k].toString(), "UTF-8"),
						URLDecoder.decode(map
								.get(param[k])
								.toString()
								.replaceAll("(?is)([^0-9]+)(%)([^0-9]*)", "$1" + "%25" + "$3"), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param username
	 *            add parameter in list parameter with remove old parameter if
	 *            it is in list
	 * @param value
	 * 
	 */
	public void addParameter(String name, String value) {
		for (int i = 0; i < this.parameter.size(); i++) {
			if (name.equalsIgnoreCase(this.parameter.get(i).name)) {
				this.parameter.get(i).value = value;
				return;
			}
		}
		Para para = new Para();
		para.name = name;
		para.value = value;
		this.parameter.add(para);
	}

	/**
	 * @param linkCompare
	 *            compare two links based on parameter
	 * @return parameter
	 */
	public String leftCompareToNameAndValueParameter(String linkCompare) {
		String tmp = "";
		String tmpValue = "";
		boolean isInList = false;
		LinkParser linkOther = new LinkParser(linkCompare);
		for (int i = 0; i < this.parameter.size(); i++) {
			isInList = false;
			for (int j = 0; j < linkOther.parameter.size(); j++) {
				if ((this.parameter.get(i).name.equalsIgnoreCase(linkOther.parameter.get(j).name))
						&& (!this.parameter.get(i).value.equalsIgnoreCase(linkOther.parameter.get(j).value))) {
					tmpValue = linkOther.parameter.get(j).name + " =  "
								+ linkOther.parameter.get(j).value;
					isInList = true;
				}
			}
			if (isInList) {
				tmp += "&>>Left Value " + this.parameter.get(i).name + "="
						+ this.parameter.get(i).value + ">>Right Value "
						+ tmpValue;
			}
		}
		if (tmp.length() > 0) {
			return tmp.substring(1);
		} else {
			return "";
		}
	}

	public String leftCompareToNameParameter(String linkCompare) {
		String tmp = "";
		boolean isInList = false;
		LinkParser linkOther = new LinkParser(linkCompare);
		for (int i = 0; i < this.parameter.size(); i++) {
			isInList = false;
			for (int j = 0; j < linkOther.parameter.size(); j++) {
				if (this.parameter.get(i).name.equalsIgnoreCase(linkOther.parameter.get(j).name)) {
					isInList = true;
				}
			}
			if (!isInList) {
				tmp += "&" + this.parameter.get(i).name + "="
						+ this.parameter.get(i).value;
			}
		}
		if (tmp.length() > 0) {
			return tmp.substring(1);
		} else {
			return "";
		}
	}

	public String rightCompareToNameParameter(String linkCompare) {
		String tmp = "";
		boolean isInList = false;
		LinkParser linkOther = new LinkParser(linkCompare);
		for (int i = 0; i < linkOther.parameter.size(); i++) {
			isInList = false;
			for (int j = 0; j < this.parameter.size(); j++) {
				if (this.parameter.get(j).name.equalsIgnoreCase(linkOther.parameter.get(i).name)) {
					isInList = true;
				}
			}
			if (!isInList) {
				tmp += "&" + linkOther.parameter.get(i).name + "="
						+ linkOther.parameter.get(i).value;
			}
		}
		if (tmp.length() > 0) {
			return tmp.substring(1);
		} else {
			return "";
		}
	}

	public void addMultipleParameter(String multipleParam) {
		String[] tmp = multipleParam.split("&");
		for (int i = 0; i < tmp.length; i++) {
			if (tmp[i].contains("=")) {
				addParameter(tmp[i].substring(0, tmp[i].indexOf("=")), tmp[i].substring(tmp[i].indexOf("=") + 1));

			}
		}

	}

	public void setParameter(String paramS) {
		try {
			paramS = URLDecoder.decode(paramS, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String[] tmp = paramS.split("&");
		for (int i = 0; i < tmp.length; i++) {
			if (tmp[i].contains("=")) {

				addParameter(tmp[i].substring(0, tmp[i].indexOf("=")), tmp[i].substring(tmp[i].indexOf("=") + 1));

			}

		}
		for (int i = 0; i < this.parameter.size(); i++) {
			for (int j = i + 1; j < this.parameter.size(); j++) {
				if (this.parameter.get(i).name.equalsIgnoreCase(this.parameter.get(j).name)) {
					this.parameter.remove(j);
					--j;
				}
			}
		}
	}

	public void toHTTPRequestParam(HTTPRequest req) {
		for (int i = 0; i < this.parameter.size(); i++) {
			try {
				if (req.getPostParameter(this.parameter.get(i).name) != null
						&& req.getPostParameter(URLEncoder.encode(this.parameter.get(i).name, "UTF-8")) != null) {
					req.getPostParameters().remove(this.parameter.get(i).name);
					req.getPostParameters().remove(URLEncoder.encode(this.parameter.get(i).name, "UTF-8"));
				}
				req.setPostParameter(this.parameter.get(i).name, this.parameter.get(i).value);

			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public String toStringOnlyParameterEncoding() {
		String tmp = "";
		if (this.parameter.size() > 0) {
			tmp = tmp + this.parameter.get(0).name + "=";
			try {
				tmp = tmp + URLEncoder.encode(this.parameter.get(0).value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (int i = 1; i < this.parameter.size(); i++) {
			tmp = tmp + "&" + this.parameter.get(i).name + "=";
			try {
				tmp = tmp + URLEncoder.encode(this.parameter.get(i).value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return tmp;
	}

	public String toStringOnlyParameter() {
		String tmp = "";
		if (this.parameter.size() > 0) {
			tmp = tmp + this.parameter.get(0).name + "=";
			tmp = tmp + this.parameter.get(0).value;
		}
		for (int i = 1; i < this.parameter.size(); i++) {
			tmp = tmp + "&" + this.parameter.get(i).name + "=";
			tmp = tmp + this.parameter.get(i).value;
		}
		return tmp;
	}

	public String toString() {
		String tmp = "";
		tmp += this.protocol;
		tmp += this.link;
		if (this.parameter.size() > 0) {
			tmp += "?";
			tmp = tmp + this.parameter.get(0).name + "=";
			tmp += this.parameter.get(0).value;
		}
		for (int i = 1; i < this.parameter.size(); i++) {
			tmp = tmp + "&" + this.parameter.get(i).name + "=";
			tmp += this.parameter.get(i).value;
		}
		return tmp;
	}

	public String toStringEncoder() {
		String tmp = "";
		tmp += this.protocol;
		tmp += this.link;

		if (this.parameter.size() > 0) {
			tmp += "?";
			tmp = tmp + this.parameter.get(0).name + "=";
			try {
				tmp += URLEncoder.encode(this.parameter.get(0).value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (int i = 1; i < this.parameter.size(); i++) {
			tmp = tmp + "&" + this.parameter.get(i).name + "=";
			try {
				tmp += URLEncoder.encode(this.parameter.get(i).value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return tmp;
	}

	class Para {
		String name = "";
		String value = "";

		Para() {
			super();
			name = "";
			value = "";
		}
	}
}
