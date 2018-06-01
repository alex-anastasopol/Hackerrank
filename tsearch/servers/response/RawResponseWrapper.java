package ro.cst.tsearch.servers.response;

import java.io.BufferedInputStream;

import org.apache.commons.httpclient.HttpMethod;

import ro.cst.tsearch.servers.TSConnectionURL;

public class RawResponseWrapper {


	private String contentType = TSConnectionURL.HTML_CONTENT_TYPE;
	private int contentLength = 0;
	private String textResponse = "";
	private BufferedInputStream binaryResponse = null;
	private HttpMethod hm; 

	public RawResponseWrapper(String contentType, int contentLength, String textResponse, BufferedInputStream binaryResponse,HttpMethod h){
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.textResponse =  textResponse;
		this.binaryResponse =  binaryResponse;
		this.hm=h;
	}

	public RawResponseWrapper(String contentType, int contentLength, BufferedInputStream binaryResponse){
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.binaryResponse =  binaryResponse;
	}
	
	public RawResponseWrapper(String textResponse){
		if (textResponse == null) {
		    this.contentLength = 0;
		    this.textResponse =  "";
		} else {
		    this.contentLength = textResponse.length();
		    this.textResponse =  textResponse;
		}
	}

	public BufferedInputStream getBinaryResponse() {
		return binaryResponse;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType.toLowerCase();
	}

	public String getTextResponse() {
		return textResponse;
	}

	/**
	 * @return
	 */
	public HttpMethod getHm() {
		return hm;
	}

	/**
	 * @param method
	 */
	public void setHm(HttpMethod method) {
		hm = method;
	}

}
