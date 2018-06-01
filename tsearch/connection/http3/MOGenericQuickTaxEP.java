package ro.cst.tsearch.connection.http3;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;

public class MOGenericQuickTaxEP extends HttpSite3 {
	
	private String FAST_VERLAST__ = "";
	private String FAST_VERLAST_SOURCE__ = "";
	
	NodeList inputsReq = null;
	NodeList inputsNotReq = null;
	
//	private static Pattern FAST_PARAM_PAT = Pattern.compile("(?is)FWDC.setVerLast\\('([^']+)'\\s*,\\s*'([^']+)'");
	@Override
	public LoginResponse onLogin() {
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		
		//go to the site
		HTTPRequest request = new HTTPRequest(getSiteLink());
		HTTPResponse resp = null;
		String responseAsString = "";
		int counter = 0;
		while (!responseAsString.contains("Real Property Search") && counter < 3) {
			counter++;
			request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");
			request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			request.setHeader("Accept-Language", "en-US,en;q=0.5");
			request.setHeader("Accept-Encoding", "gzip, deflate");
			request.setHeader("Connection", "keep-alive");
			request.setHeader("Cache-Control", "max-age=0");
			request.removeHeader("Referer");
//			request.noRedirects = false;
			resp = null;
			try {
				resp = process(request);
			} catch (Exception e) {
			}
			if (resp == null) {
//				request.noRedirects = false;
				request.removeHeader("Referer");
				resp = process(request);
			}
			responseAsString = "";
			try {
				responseAsString = resp.getGZipResponseAsString();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (StringUtils.isBlank(responseAsString) || responseAsString.contains("Request Rejected")) {
//				request.noRedirects = true;
				request.setURL(getSiteLink() + "_/");
				resp = process(request);
				try {
					responseAsString = resp.getGZipResponseAsString();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (responseAsString.contains("Real Property Search")){
			//go to search forms page
			request = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/", HTTPRequest.GET);
			request.setHeader("User-Agent", getUserAgentValue());	
			request.setHeader("Accept", getAccept());
			request.setHeader("Accept-Language", getAcceptLanguage());
			request.setHeader("Accept-Encoding", "gzip, deflate");
//			request.setHeader("Referer", getSiteLink());
//			request.noRedirects = false;
			
			resp = process(request);
			try {
				responseAsString = resp.getGZipResponseAsString();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (StringUtils.isBlank(responseAsString) || responseAsString.contains("Request Rejected")){
				request = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/", HTTPRequest.GET);
				request.setHeader("User-Agent", getUserAgentValue());	
				request.setHeader("Accept", getAccept());
				request.setHeader("Accept-Language", getAcceptLanguage());
				request.setHeader("Accept-Encoding", "gzip, deflate");
				request.setHeader("Referer", "https://quicktax.kcmo.org/TAP.ASM/");
//				request.noRedirects = true;
				
				resp = process(request);
				try {
					responseAsString = resp.getGZipResponseAsString();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (responseAsString.contains("Property Search")){
				refreshFAST_VERLASTValues(null, resp);
				
				HtmlParser3 parser = new HtmlParser3(responseAsString);
				inputsReq = parser.getNodeListByTypeAndAttribute("input", "class", "FieldEnabled Field DocControlRadioButton", true);
				inputsNotReq = parser.getNodeListByTypeAndAttribute("input", "class", "FieldEnabled Field DocControlText", true);
				
				return LoginResponse.getDefaultSuccessResponse();
			}
		}
		
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get login page!");		
	}
	 
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if (req.getPostFirstParameter("parcelNo") != null){
			//go to Search By parcel module
			HTTPRequest newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/Recalc", HTTPRequest.POST);
			
			if (inputsReq != null){
				for (int i = 0; i < inputsReq.size(); i++) {
					InputTag input = (InputTag) inputsReq.elementAt(i);
					newReq.setPostParameter(input.getAttribute("name"), input.getAttribute("value"));
				}
			}
			if (inputsNotReq != null){
				for (int i = 0; i < inputsNotReq.size(); i++) {
					InputTag input = (InputTag) inputsNotReq.elementAt(i);
					if (input.getAttribute("name").equals("b-G")
							|| input.getAttribute("name").equals("b-B")
							|| input.getAttribute("name").equals("b-B")
							|| input.getAttribute("name").equals("b-z")
							|| input.getAttribute("name").equals("b-u")
							|| input.getAttribute("name").equals("b-q")){
						continue;
					}
					newReq.setPostParameter(input.getAttribute("name"), input.getAttribute("value"));
				}
			}
			newReq.setPostParameter("LASTFOCUSFIELD__", "b-9");
			newReq.setPostParameter("DOC_MODAL_ID__", "0");
			newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
			newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__);
//			newReq.noRedirects = true;

			HTTPResponse resp = process(newReq);
			String responseAsString = "";
			try {
				responseAsString = resp.getGZipResponseAsString();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/Recalc", HTTPRequest.POST);
			
			if (resp.getContentType().contains("json") && StringUtils.isNotBlank(responseAsString)){
				try {
					JSONObject jsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
					if (jsonObject instanceof JSONObject){
//						if (jsonObject.has("Updates")){
//							JSONObject jsonObjectUpd = jsonObject.getJSONObject("Updates");
//							if (jsonObjectUpd instanceof JSONObject){
//								if (jsonObjectUpd.has("FieldUpdates")){
//									JSONArray jsonArrayFieldUpd = jsonObjectUpd.getJSONArray("FieldUpdates");
//									if (jsonArrayFieldUpd instanceof JSONArray){
//										for (int i = 0; i < jsonArrayFieldUpd.length(); i++) {
//											Object fieldObj = jsonArrayFieldUpd.get(i);
//											if (fieldObj instanceof JSONObject){
//												if (!((JSONObject) fieldObj).has("container")){
//													if (((JSONObject) fieldObj).has("field")){
//														String paramName = ((JSONObject) fieldObj).getString("field");
//														if (((JSONObject) fieldObj).has("value")){
//															newReq.setPostParameter(paramName, ((JSONObject) fieldObj).getString("value"));
//														} else{
//															newReq.setPostParameter(paramName, "");
//														}
//													}
//												}
//											}
//										}
//									}
//								}
//							}
//						}
						refreshFAST_VERLASTValues(jsonObject, resp);
					}
				} catch (JSONException e) {
				}
			}
			if (inputsNotReq != null){
				for (int i = 0; i < inputsNotReq.size(); i++) {
					InputTag input = (InputTag) inputsNotReq.elementAt(i);
					if (input.getAttribute("name").equals("b-t")
							|| input.getAttribute("name").equals("b-u")){

						newReq.setPostParameter(input.getAttribute("name"), input.getAttribute("value"));
					}
				}
			}
			newReq.setPostParameter("b-8", "false");
			newReq.setPostParameter("b-9", "true");
			newReq.setPostParameter("b-a", "false");
			newReq.setPostParameter("b-b", "false");
			newReq.setPostParameter("b-q", req.getPostFirstParameter("parcelNo"));
			newReq.setPostParameter("LASTFOCUSFIELD__", "b-q");
			newReq.setPostParameter("DOC_MODAL_ID__", "0");
			newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__.replaceFirst("(?is)[^,]*", "Recalc:ID").replaceFirst("(?is)([^@]*).*", "$1").trim());
			newReq.setPostParameter("RECALC_SOURCE__", "b-q");
			
			resp = process(newReq);
			try {
				responseAsString = resp.getGZipResponseAsString();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (resp.getContentType().contains("json") && StringUtils.isNotBlank(responseAsString)){
				try {
					JSONObject jsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
					if (jsonObject instanceof JSONObject){
//						if (jsonObject.has("Updates")){
//							JSONObject jsonObjectUpd = jsonObject.getJSONObject("Updates");
//							if (jsonObjectUpd instanceof JSONObject){
//								if (jsonObjectUpd.has("FieldUpdates")){
//									JSONArray jsonArrayFieldUpd = jsonObjectUpd.getJSONArray("FieldUpdates");
//									if (jsonArrayFieldUpd instanceof JSONArray){
//										for (int i = 0; i < jsonArrayFieldUpd.length(); i++) {
//											Object fieldObj = jsonArrayFieldUpd.get(i);
//											if (fieldObj instanceof JSONObject){
//												if (!((JSONObject) fieldObj).has("container")){
//													if (((JSONObject) fieldObj).has("field")){
//														String paramName = ((JSONObject) fieldObj).getString("field");
//														if (((JSONObject) fieldObj).has("value")){
//															newReq.setPostParameter(paramName, ((JSONObject) fieldObj).getString("value"));
//														} else{
//															newReq.setPostParameter(paramName, "");
//														}
//													}
//												}
//											}
//										}
//									}
//								}
//							}
//						}
						refreshFAST_VERLASTValues(jsonObject, resp);
					}
				} catch (JSONException e) {
				}
			}
			
			//search by APN
			newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/EventOccurred", HTTPRequest.POST);
			newReq.setPostParameter("b-8", "false");
			newReq.setPostParameter("b-9", "true");
			newReq.setPostParameter("b-a", "false");
			newReq.setPostParameter("b-b", "false");
			newReq.setPostParameter("b-q", req.getPostFirstParameter("parcelNo"));
			newReq.setPostParameter("b-t", "");
			newReq.setPostParameter("b-u", "");
			newReq.setPostParameter("LASTFOCUSFIELD__", "b-q");
			newReq.setPostParameter("DOC_MODAL_ID__", "0");
			newReq.setPostParameter("EVENT__", "b-v");
			newReq.setPostParameter("DOC_ACTION__", "true");
			newReq.setPostParameter("TYPE__", "0");
			newReq.setPostParameter("CLOSECONFIRMED__", "false");
			newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__.replaceFirst("(?is)[^,]*", "Recalc:APN").replaceFirst("(?is)([^@]*).*", "$1").trim());
			newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
//			newReq.noRedirects = true;
			
			resp = process(newReq);
			try {
				responseAsString = resp.getGZipResponseAsString();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			boolean resultsfound = false;
			if (resp.getContentType().contains("json") && StringUtils.isNotBlank(responseAsString)){
				try {
					JSONObject jsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
					if (jsonObject instanceof JSONObject){
						if (jsonObject.has("result")){
							try {
								String html = jsonObject.getString("result");
								
								if (StringUtils.isNotBlank(html)){
									resultsfound = true;
								}
							} catch (Exception e) { }
						}
						refreshFAST_VERLASTValues(jsonObject, resp);
					}
				} catch (JSONException e) {
				}
			}
			
			if (resultsfound){
				StringBuilder htmlResponse = new StringBuilder();
				//go to details page
				newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/EventOccurred", HTTPRequest.POST);
				newReq.setPostParameter("b-8", "false");
				newReq.setPostParameter("b-9", "true");
				newReq.setPostParameter("b-a", "false");
				newReq.setPostParameter("b-b", "false");
				newReq.setPostParameter("b-q", req.getPostFirstParameter("parcelNo"));
				newReq.setPostParameter("b-t", "");
				newReq.setPostParameter("b-u", "");
				newReq.setPostParameter("LASTFOCUSFIELD__", "b-q");
				newReq.setPostParameter("DOC_MODAL_ID__", "0");
				newReq.setPostParameter("EVENT__", "b-J-1");
				newReq.setPostParameter("DOC_ACTION__", "false");
				newReq.setPostParameter("TYPE__", "0");
				newReq.setPostParameter("CLOSECONFIRMED__", "false");
				newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__);
				newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
//				newReq.noRedirects = true;
				
				resp = process(newReq);
				try {
					responseAsString = resp.getGZipResponseAsString();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (resp.getContentType().contains("json") && StringUtils.isNotBlank(responseAsString)){
					try {
						JSONObject jsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
						if (jsonObject instanceof JSONObject){
							refreshFAST_VERLASTValues(jsonObject, resp);
							if (jsonObject.has("html")){
								try {
									String html = jsonObject.getString("html");
									
									if (StringUtils.isNotBlank(html)){
										HtmlParser3 htmlparser = new HtmlParser3(html);
										String totalBalanceLabel = htmlparser.getNodeById("container_b-8").toHtml();
										if (StringUtils.isNotBlank(totalBalanceLabel)){
											totalBalanceLabel = totalBalanceLabel.replaceAll("(?is)</?label[^>]*>", "");
											totalBalanceLabel = totalBalanceLabel.replaceAll("(?is)<div[^>]*>", "<td>");
											totalBalanceLabel = totalBalanceLabel.replaceAll("(?is)</div[^>]*>", "</td>");
										}
										String totalBalanceValue = htmlparser.getNodeById("container_b-c").toHtml();
										if (StringUtils.isNotBlank(totalBalanceValue)){
											totalBalanceValue = totalBalanceValue.replaceAll("(?is)</?label[^>]*>", "");
											totalBalanceValue = totalBalanceValue.replaceAll("(?is)<div[^>]*>", "<td>");
											totalBalanceValue = totalBalanceValue.replaceAll("(?is)</div[^>]*>", "</td>");
										}
										if (StringUtils.isNotBlank(totalBalanceLabel) && StringUtils.isNotBlank(totalBalanceValue)){
											htmlResponse.append("<br><br><table><tr>").append(totalBalanceLabel).append(totalBalanceValue).append("</tr></table><br><br>");
										}
										
										htmlResponse.append("<br><br><b>Active Special Assessments</b><br>");
										
										boolean hasDataInTable = false;
										Node tableNode = htmlparser.getNodeById("b-J");
										if (tableNode instanceof TableTag){
											TableTag tableTag = (TableTag) tableNode;
											if (tableTag.getRowCount() > 1){
												hasDataInTable = true;
											}
										} 
										if (hasDataInTable){
											htmlResponse.append(tableNode.toHtml());
											
											//go to detail on the link from Active Special Assessments
//											newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/EventOccurred", HTTPRequest.POST);
//											newReq.setPostParameter("LASTFOCUSFIELD__", "");
//											newReq.setPostParameter("DOC_MODAL_ID__", "0");
//											newReq.setPostParameter("EVENT__", "DNCy45__0_0_ClTuJE4__1");
//											newReq.setPostParameter("DOC_ACTION__", "false");
//											newReq.setPostParameter("TYPE__", "0");
//											newReq.setPostParameter("CLOSECONFIRMED__", "false");
//											newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__);
//											newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
//											newReq.noRedirects = true;
//											
//											resp = process(newReq);
//											try {
//												responseAsString = resp.getGZipResponseAsString();
//											} catch (IOException e) {
//												e.printStackTrace();
//											}
//											try {
//												JSONObject detailsJsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
//												if (detailsJsonObject instanceof JSONObject){
//													refreshFAST_VERLASTValues(detailsJsonObject);
//													if (detailsJsonObject.has("html")){
//														try {
//															String detail = detailsJsonObject.getString("html");
//															
//															if (StringUtils.isNotBlank(detail)){
//																htmlparser = new HtmlParser3(detail);
//																Node div1Node = htmlparser.getNodeById("container_DWertC3__0_0_F4amQ8");
//																if (div1Node != null){
//																	String divContent = div1Node.toHtml();
//																	divContent = divContent.replaceAll("(?is)</div>", "</div><br><br>");
//																	htmlResponse.append(divContent);
//																}
//																Node div2Node = htmlparser.getNodeById("container_DWertC3__0_0_FDlMgn2");
//																if (div2Node != null){
//																	htmlResponse.append("<br><br>");
//																	htmlResponse.append(div2Node.toHtml());
//																}
//																Node div3Node = htmlparser.getNodeById("container_DWertC3__0_0_FeAmaB4");
//																if (div3Node != null){
//																	htmlResponse.append("<br><br>");
//																	htmlResponse.append(div3Node.toHtml());
//																}
//																
//															}
//														} catch (Exception e) { }
//													}
//													
//												}
//											} catch (JSONException e) {
//											}
										} else{
											Node spanNode = htmlparser.getNodeById("caption2_b-L");
											if (spanNode != null){
												htmlResponse.append(spanNode.toHtml());
											}
										}
									}
								} catch (Exception e) { }
							}
						}
					} catch (JSONException e) {
					}
				}
				
				//go to link Historical Assessments
				newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/ViewLinkClicked", HTTPRequest.POST);
				newReq.setPostParameter("LASTFOCUSFIELD__", "");
				newReq.setPostParameter("DOC_MODAL_ID__", "0");
				newReq.setPostParameter("EVENT__", "b-r");
				newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__);
				newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
//				newReq.noRedirects = true;
				
				resp = process(newReq);
				try {
					responseAsString = resp.getGZipResponseAsString();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (resp.getContentType().contains("json") && StringUtils.isNotBlank(responseAsString)){
					try {
						JSONObject jsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
						if (jsonObject instanceof JSONObject){
							refreshFAST_VERLASTValues(jsonObject, resp);
							if (jsonObject.has("html")){
								try {
									String html = jsonObject.getString("html");
									
									if (StringUtils.isNotBlank(html)){
										htmlResponse.append("<br><br><b>Historical Assessments</b><br>");
										HtmlParser3 htmlparser = new HtmlParser3(html);
										
										boolean hasDataInTable = false;
										Node tableNode = htmlparser.getNodeById("b-11");
										if (tableNode instanceof TableTag){
											TableTag tableTag = (TableTag) tableNode;
											if (tableTag.getRowCount() > 1){
												hasDataInTable = true;
											}
										} 
										if (hasDataInTable){
											htmlResponse.append(tableNode.toHtml());
											
											//go to detail on the links from Historical Assessments
//											newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/EventOccurred", HTTPRequest.POST);
//											newReq.setPostParameter("LASTFOCUSFIELD__", "");
//											newReq.setPostParameter("DOC_MODAL_ID__", "0");
//											newReq.setPostParameter("EVENT__", "DNCy45__0_0_CkBdop4__1");
//											newReq.setPostParameter("DOC_ACTION__", "false");
//											newReq.setPostParameter("TYPE__", "0");
//											newReq.setPostParameter("CLOSECONFIRMED__", "false");
//											newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__);
//											newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
//											newReq.noRedirects = true;
//											
//											resp = process(newReq);
//											try {
//												responseAsString = resp.getGZipResponseAsString();
//											} catch (IOException e) {
//												e.printStackTrace();
//											}
//											try {
//												JSONObject detailsJsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
//												if (detailsJsonObject instanceof JSONObject){
//													refreshFAST_VERLASTValues(detailsJsonObject);
//													if (detailsJsonObject.has("html")){
//														try {
//															String detail = detailsJsonObject.getString("html");
//															
//															if (StringUtils.isNotBlank(detail)){
//																htmlparser = new HtmlParser3(detail);
//																Node div1Node = htmlparser.getNodeByAttribute("class", "ViewContainer ViewContainerCG_REAL", true);
//																if (div1Node != null){
//																	String divContent = div1Node.toHtml();
//																	htmlResponse.append(divContent);
//																}
//															}
//														} catch (Exception e) { }
//													}
//													
//												}
//											} catch (JSONException e) {
//											}
										} else{
											Node spanNode = htmlparser.getNodeById("caption2_b-31");
											if (spanNode != null){
												htmlResponse.append(spanNode.toHtml());
												htmlResponse.append("<br><br>");
											}
										}
									}
								} catch (Exception e) { }
							}
						}
					} catch (JSONException e) {
					}
				}
				
				//go to link Pending Assessments
				newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/ViewLinkClicked", HTTPRequest.POST);
				newReq.setPostParameter("LASTFOCUSFIELD__", "");
				newReq.setPostParameter("DOC_MODAL_ID__", "0");
				newReq.setPostParameter("EVENT__", "b-i");
				newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__);
				newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
//				newReq.noRedirects = true;
				
				resp = process(newReq);
				try {
					responseAsString = resp.getGZipResponseAsString();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (resp.getContentType().contains("json") && StringUtils.isNotBlank(responseAsString)){
					try {
						JSONObject jsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
						if (jsonObject instanceof JSONObject){
							refreshFAST_VERLASTValues(jsonObject, resp);
							if (jsonObject.has("html")){
								try {
									String html = jsonObject.getString("html");
									
									if (StringUtils.isNotBlank(html)){
										htmlResponse.append("<br><br><b>Pending Assessments</b><br>");
										HtmlParser3 htmlparser = new HtmlParser3(html);
										
										boolean hasDataInTable = false;
										Node tableNode = htmlparser.getNodeById("b-b1");
										if (tableNode instanceof TableTag){
											TableTag tableTag = (TableTag) tableNode;
											if (tableTag.getRowCount() > 1){
												hasDataInTable = true;
											}
										} 
										if (hasDataInTable){
											htmlResponse.append(tableNode.toHtml());
										} else{
											Node spanNode = htmlparser.getNodeById("caption2_b-d1");
											if (spanNode != null){
												htmlResponse.append(spanNode.toHtml());
												htmlResponse.append("<br><br>");
											}
										}
									}
								} catch (Exception e) { }
							}
						}
					} catch (JSONException e) {
					}
				}
				
				//go to link Parcel Description
				newReq = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/ViewLinkClicked", HTTPRequest.POST);
				newReq.setPostParameter("LASTFOCUSFIELD__", "");
				newReq.setPostParameter("DOC_MODAL_ID__", "0");
				newReq.setPostParameter("EVENT__", "b-j");
				newReq.setPostParameter("FAST_VERLAST_SOURCE__", FAST_VERLAST_SOURCE__);
				newReq.setPostParameter("FAST_VERLAST__", FAST_VERLAST__);
//				newReq.noRedirects = true;
				
				resp = process(newReq);
				try {
					responseAsString = resp.getGZipResponseAsString();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (resp.getContentType().contains("json") && StringUtils.isNotBlank(responseAsString)){
					try {
						JSONObject jsonObject = new JSONObject(responseAsString.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "").trim());
						if (jsonObject instanceof JSONObject){
							refreshFAST_VERLASTValues(jsonObject, resp);
							if (jsonObject.has("html")){
								try {
									String html = jsonObject.getString("html");
									
									if (StringUtils.isNotBlank(html)){
										htmlResponse.append("<br><br><b>Parcel Description</b><br>");
										HtmlParser3 htmlparser = new HtmlParser3(html);
										
										Node tableNode = htmlparser.getNodeById("container_b-h1");
										if (tableNode instanceof Div){
											
											try {
												String table = tableNode.toHtml();
												table = table.replaceAll("(?is)<div[^>]*>\\s*</div[^>]*>\\s*(</div[^>]*>)", "");
												table = table.replaceAll("(?is)<div\\s+id=\\\"container_b-h1\\\"[^>]*>\\s*<div[^>]*>\\s*(<div[^>]*>)", "");
												table = table.replaceAll("(?is)<div\\s+id=\\\"container_b[^>]*>", "");
												table = table.replaceAll("(?is)<div[^>]*>(.*?)</div[^>]*>\\s*<div[^>]*>(.*?)</div[^>]*>",
														"<tr><td>$1</td><td>$2</td></tr>");
												table = table.replaceAll("(?is)<label id[^>]*>(.*?)</label[^>]*>\\s*</div[^>]*>\\s*</div[^>]*>", "");
												table = table.replaceAll("(?is)</?textarea[^>]*>", "");
												table = table.replaceAll("(?is)<input .*?value=\\\"([^\\\"]*)\\\"[^>]*>", "$1");
												htmlResponse.append("<table>").append(table).append("</table>");
											} catch (Exception e) {
											}
										}
									}
								} catch (Exception e) { }
							}
						}
					} catch (JSONException e) {
					}
				}
				
				if (htmlResponse.length() > 0){
					htmlResponse.append("<br><br>");
					resp.contentType = "text/html; charset=utf-8";
					// bypass response
					resp.is = IOUtils.toInputStream(htmlResponse);
					resp.body = htmlResponse.toString();
					req.setBypassResponse(resp);
				}
			}
		}
	}

	/**
	 * @param jsonObject
	 * @param httpResp
	 */
	public void refreshFAST_VERLASTValues(JSONObject jsonObject, HTTPResponse httpResp) {
		if (jsonObject != null && jsonObject.has("fastverlastsource")){
			try {
				String newValueFVLS = jsonObject.getString("fastverlastsource");
				
				if (StringUtils.isNotBlank(newValueFVLS)){
					FAST_VERLAST_SOURCE__ = newValueFVLS;
				}
			} catch (Exception e) { }
		} else if (httpResp != null && StringUtils.isNotBlank(httpResp.getHeader("Fast-Ver-Source"))){
			FAST_VERLAST_SOURCE__ = httpResp.getHeader("Fast-Ver-Source");
		}
		if (jsonObject != null && jsonObject.has("fastverlast")){
			try {
				String newValueFVL = jsonObject.getString("fastverlast");
				
				if (StringUtils.isNotBlank(newValueFVL)){
					FAST_VERLAST__ = newValueFVL;
				}
			} catch (Exception e) { }
		} else if (httpResp != null && StringUtils.isNotBlank(httpResp.getHeader("Fast-Ver-Last"))){
			FAST_VERLAST__ = httpResp.getHeader("Fast-Ver-Last");
		}
	}
	
	public String getResponseForPIN(String pin) {
		
		HTTPRequest req = null;
		if (StringUtils.isNotBlank(pin)){
			
			req = new HTTPRequest("https://quicktax.kcmo.org/TAP.ASM/_/EventOccurred", HTTPRequest.POST);
			
			req.setPostParameter("parcelNo", pin);
			
			String response = execute(req);
			destroySession();
			
			return response;
		}
		
		return "";
	}
}
