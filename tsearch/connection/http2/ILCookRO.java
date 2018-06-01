package ro.cst.tsearch.connection.http2;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageAdapter;
import javax.media.jai.RenderedOp;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.TiffConcatenator;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 12, 2012
 */

public class ILCookRO extends HttpSite {

	String[]	aspxParamNames	= new String[] { "__ASYNCPOST", "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__VIEWSTATE" };

	public LoginResponse onLogin() {
		try {
			HTTPRequest getSiteReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx");

			String resp = execute(getSiteReq);

			if (resp.contains("Welcome to Cook County Recorder of Deeds")) {
				return LoginResponse.getDefaultSuccessResponse();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	public void onBeforeRequestExcl(HTTPRequest req) {
		return;
	}

	private HTTPRequest	reqAux	= null;

	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {
			if (req.hasPostParameter("intermediarySearch")) {
				String eventTarget = req.getPostFirstParameter("EVENTTARGET");
				req.clearPostParameters();

				req.setPostParameters(reqAux.getPostParameters());

				req.removePostParameters("__EVENTTARGET");
				req.setPostParameter("__EVENTTARGET", eventTarget);
				req.removePostParameters("ScriptManager1");
				req.setPostParameter("ScriptManager1", "DocList1$UpdatePanel|" + eventTarget);

				req.removePostParameters("SearchFormEx1$btnSearch.x");
				req.removePostParameters("SearchFormEx1$btnSearch.y");
				req.removePostParameters("NameList1$ScrollPosChange");
				
				req.removePostParameters("SearchFormEx1$btnSearch");
			
			} else if (req.hasPostParameter("detailsSearch")) {
				String eventTarget = req.getPostFirstParameter("EVENTTARGET");

				try {
					eventTarget = URLDecoder.decode(eventTarget, "ASCII");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				req.clearPostParameters();

				req.setPostParameters(reqAux.getPostParameters());

				req.removePostParameters("__EVENTTARGET");
				req.setPostParameter("__EVENTTARGET", eventTarget);
				req.removePostParameters("ScriptManager1");
				req.setPostParameter("ScriptManager1", "DocList1$UpdatePanel|" + eventTarget);

				req.removePostParameters("SearchFormEx1$btnSearch.x");
				req.removePostParameters("SearchFormEx1$btnSearch.y");
				req.removePostParameters("NameList1$ScrollPosChange");
				req.removePostParameters("NameList1$ctl05");
				
				req.removePostParameters("SearchFormEx1$btnSearch");
				if (reqAux != null && StringUtils.isNotBlank(reqAux.getHeader("Referer"))){
					req.setHeader("Referer", reqAux.getHeader("Referer"));
				}
			
			} else if (req.hasPostParameter("imageSearch")) {

				req.clearPostParameters();

				req.setPostParameters(reqAux.getPostParameters());

				req.removePostParameters("__EVENTTARGET");
				req.removePostParameters("ScriptManager1");

				req.removePostParameters("SearchFormEx1$btnSearch.x");
				req.removePostParameters("SearchFormEx1$btnSearch.y");
				req.removePostParameters("NameList1$ScrollPosChange");
				req.removePostParameters("NameList1$ctl05");
				
				req.removePostParameters("SearchFormEx1$btnSearch");
				
				req.setPostParameter("ScriptManager1", "TabController1$UpdatePanel1|TabController1$ImageViewertabitem");
				req.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
				req.setPostParameter("SearchFormEx1$ACSDropDownList_DocumentType", "-2");
				req.setPostParameter("SearchFormEx1$ACSTextBox_DateFrom", "1/1/1985");
				req.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
				req.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
				req.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");
				req.setPostParameter("PTAXViewer1$_imgContainerWidth", "0");
				req.setPostParameter("PTAXViewer1$_imgContainerHeight", "0");
				req.setPostParameter("PTAXViewer1$isImageViewerVisible", "true");
				req.setPostParameter("__ASYNCPOST", "true");
				req.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
				req.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
				req.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
				req.setPostParameter("__EVENTTARGET", "TabController1$ImageViewertabitem");
				req.setPostParameter("DocList1$ctl11", "0");
				
				if (reqAux != null && StringUtils.isNotBlank(reqAux.getHeader("Referer"))){
					req.setHeader("Referer", reqAux.getHeader("Referer"));
				}
			}else if (req.hasPostParameter("nameSearch")) {
				HTTPRequest nameReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx", HTTPRequest.POST);

				nameReq.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton01");
				nameReq.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
				nameReq.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
				nameReq.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
				nameReq.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
				nameReq.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
				nameReq.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
				nameReq.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");

				nameReq.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton01");
				nameReq.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST") && !s.equals("__EVENTTARGET")) {
						nameReq.setPostParameter(s, "");
					}
				}

				execute(nameReq);

				req.removePostParameters("nameSearch");

				req.setPostParameter("SearchFormEx1$btnSearch", "Search");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST")) {
						req.setPostParameter(s, "");
					}
				}

				req.setPostParameter("__ASYNCPOST", "true");

				reqAux = req;
			} else if (req.hasPostParameter("fakeParcel")) {

				String parcel = req.getPostFirstParameter("fakeParcel");

				req.removePostParameters("fakeParcel");

				if (StringUtils.isNotBlank(parcel) && parcel.replaceAll("[^\\d]", "").length() == 14) {
					// get parcel form
					HTTPRequest parcelReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx", HTTPRequest.POST);

					parcelReq.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton00");
					parcelReq.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
					parcelReq.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
					parcelReq.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
					parcelReq.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
					parcelReq.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
					parcelReq.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
					parcelReq.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");

					parcelReq.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton00");
					parcelReq.setPostParameter("__ASYNCPOST", "true");

					for (String s : aspxParamNames) {
						if (!s.equals("__ASYNCPOST") && !s.equals("__EVENTTARGET")) {
							parcelReq.setPostParameter(s, "");
						}
					}

					execute(parcelReq);

					parcel = parcel.replaceAll("[^\\d]", "");

					req.setPostParameter("SearchFormEx1$PINTextBox0", parcel.substring(0, 2));
					req.setPostParameter("SearchFormEx1$PINTextBox1", parcel.substring(2, 4));
					req.setPostParameter("SearchFormEx1$PINTextBox2", parcel.substring(4, 7));
					req.setPostParameter("SearchFormEx1$PINTextBox3", parcel.substring(7, 10));
					req.setPostParameter("SearchFormEx1$PINTextBox4", parcel.substring(10));

					req.setPostParameter("SearchFormEx1$btnSearch", "Search");

					req.setPostParameter("__ASYNCPOST", "true");

					for (String s : aspxParamNames) {
						if (!s.equals("__ASYNCPOST")) {
							req.setPostParameter(s, "");
						}
					}

					reqAux = req;
				}
			} else if (req.hasPostParameter("instrNoSearch")) {
				req.removePostParameters("instrNoSearch");

				// get isntrNo form
				HTTPRequest parcelReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx", HTTPRequest.POST);

				parcelReq.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton02");
				parcelReq.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");

				parcelReq.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton02");
				parcelReq.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST") && !s.equals("__EVENTTARGET")) {
						parcelReq.setPostParameter(s, "");
					}
				}

				execute(parcelReq);

				req.setPostParameter("SearchFormEx1$btnSearch", "Search");

				req.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST")) {
						req.setPostParameter(s, "");
					}
				}

				reqAux = req;
			} else if (req.hasPostParameter("legalSearch")) {
				req.removePostParameters("legalSearch");

				// get subdisivionSearch form
				HTTPRequest parcelReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx", HTTPRequest.POST);

				parcelReq.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton03");
				parcelReq.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");

				parcelReq.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton03");
				parcelReq.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST") && !s.equals("__EVENTTARGET")) {
						parcelReq.setPostParameter(s, "");
					}
				}

				execute(parcelReq);

				req.setPostParameter("SearchFormEx1$btnSearch", "Search");

				req.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST")) {
						req.setPostParameter(s, "");
					}
				}

				reqAux = req;
			} else if (req.hasPostParameter("doctypeSearch")) {
				req.removePostParameters("doctypeSearch");

				// get subdisivionSearch form
				HTTPRequest parcelReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx", HTTPRequest.POST);

				parcelReq.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton04");
				parcelReq.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");

				parcelReq.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton04");
				parcelReq.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST") && !s.equals("__EVENTTARGET")) {
						parcelReq.setPostParameter(s, "");
					}
				}

				execute(parcelReq);

				req.setPostParameter("SearchFormEx1$btnSearch", "Search");

				req.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST")) {
						req.setPostParameter(s, "");
					}
				}

				reqAux = req;
			} else if (req.hasPostParameter("subdivisionSearch")) {
				req.removePostParameters("subdivisionSearch");

				// get subdisivionSearch form
				HTTPRequest parcelReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx", HTTPRequest.POST);

				parcelReq.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton05");
				parcelReq.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");

				parcelReq.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton05");
				parcelReq.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST") && !s.equals("__EVENTTARGET")) {
						parcelReq.setPostParameter(s, "");
					}
				}

				execute(parcelReq);

				req.setPostParameter("SearchFormEx1$btnSearch", "Search");

				req.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST")) {
						req.setPostParameter(s, "");
					}
				}

				reqAux = req;
			} else if (req.hasPostParameter("trustSearch")) {
				req.removePostParameters("trustSearch");

				// get subdisivionSearch form
				HTTPRequest parcelReq = new HTTPRequest(getDataSite().getServerHomeLink() + "i2/default.aspx", HTTPRequest.POST);

				parcelReq.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton06");
				parcelReq.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
				parcelReq.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
				parcelReq.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");

				parcelReq.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton06");
				parcelReq.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST") && !s.equals("__EVENTTARGET")) {
						parcelReq.setPostParameter(s, "");
					}
				}

				execute(parcelReq);

				req.setPostParameter("SearchFormEx1$btnSearch", "Search");

				req.setPostParameter("__ASYNCPOST", "true");

				for (String s : aspxParamNames) {
					if (!s.equals("__ASYNCPOST")) {
						req.setPostParameter(s, "");
					}
				}

				reqAux = req;
			}
		}
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res != null){
			if (res.getContentType().contains("text/plain")) {
				res.contentType = res.getContentType().replaceAll("text/plain", "text/html");
			}
		}
	}
	
	public byte[] getImage(String docno, String fileName) {
		
		if (this.status != STATUS_LOGGED_IN) {
			onLogin();
		}
		
		HTTPRequest req1 = new HTTPRequest(getSiteLink() + "?AspxAutoDetectCookieSupport=1", HTTPRequest.POST);
		req1.setPostParameter("ScriptManager1", "Navigator1$SearchCriteria1$UpdatePanel|Navigator1$SearchCriteria1$LinkButton02");
		req1.setPostParameter("__EVENTTARGET", "Navigator1$SearchCriteria1$LinkButton02");
		req1.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
		req1.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
		req1.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
		req1.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
		req1.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
		req1.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
		req1.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");
		req1.setPostParameter("PTAXViewer1$_imgContainerWidth", "0");
		req1.setPostParameter("PTAXViewer1$_imgContainerHeight", "0");
		req1.setPostParameter("PTAXViewer1$isImageViewerVisible", "true");
		req1.setPostParameter("__ASYNCPOST", "true");
		req1.setPostParameter("DocList1$ctl11", "");
		
		String[] empty_param1 = new String[]{"BasketCtrl1$ctl01", "BasketCtrl1$ctl03", "CertificateViewer1$DragResizeExtender_ClientState",	"CertificateViewer1$ScrollPos", 
				"CertificateViewer1$ScrollPosChange", "CertificateViewer1$hdnWidgetSize", "DocDetails1$PageIndex", "DocDetails1$PageSize", "DocDetails1$SortExpression", 
				"DocList1$ctl09", "ImageViewer1$DragResizeExtender_ClientState", "ImageViewer1$ScrollPos", "ImageViewer1$ScrollPosChange", 
				"ImageViewer1$hdnWidgetSize", "NameList1$ScrollPos", "NameList1$ScrollPosChange", "NameList1$_SortExpression", "NameList1$ctl03", "NameList1$ctl05", 
				"OrderList1$ctl01", "OrderList1$ctl03", "PTAXViewer1$DragResizeExtender_ClientState", "PTAXViewer1$ScrollPos", "PTAXViewer1$ScrollPosChange", 
				"PTAXViewer1$hdnWidgetSize", "__EVENTARGUMENT", "__LASTFOCUS", "__VIEWSTATE"};
		
		String[] empty_param2 = new String[]{"SearchFormEx1$ACSTextBox_AcresNo", "SearchFormEx1$ACSTextBox_BlockNo", "SearchFormEx1$ACSTextBox_BuildingNo", 
				"SearchFormEx1$ACSTextBox_DeclarationCondoNo", "SearchFormEx1$ACSTextBox_LotNo", "SearchFormEx1$ACSTextBox_OneHalfCode", "SearchFormEx1$ACSTextBox_Part1Code",
				"SearchFormEx1$ACSTextBox_Part2Code", "SearchFormEx1$ACSTextBox_PartOfLotNo", "SearchFormEx1$ACSTextBox_Quarter1", "SearchFormEx1$ACSTextBox_Quarter2", 
				"SearchFormEx1$ACSTextBox_Quarter3", "SearchFormEx1$ACSTextBox_Subdivision", "SearchFormEx1$ACSTextBox_UnitNo", "SearchFormEx1$PINTextBox0", 
				"SearchFormEx1$PINTextBox1", "SearchFormEx1$PINTextBox2", "SearchFormEx1$PINTextBox3", "SearchFormEx1$PINTextBox4"};
		
		for (String s: empty_param1) {
			req1.setPostParameter(s, "");
		}
		for (String s: empty_param2) {
			req1.setPostParameter(s, "");
		}
		
		execute(req1);
		
		HTTPRequest req2 = new HTTPRequest(getSiteLink() + "?AspxAutoDetectCookieSupport=1", HTTPRequest.POST);
		
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");
		String today = sdf.format(date);
		
		req2.setPostParameter("ScriptManager1", "SearchFormEx1$UpdatePanel|SearchFormEx1$btnSearch");
		req2.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
		req2.setPostParameter("SearchFormEx1$ACSTextBox_Document", docno);
		req2.setPostParameter("SearchFormEx1$ACSDropDownList_DocumentType", "-2");
		req2.setPostParameter("SearchFormEx1$ACSTextBox_DateFrom", "1/1/1985");
		req2.setPostParameter("SearchFormEx1$ACSTextBox_DateTo", today);
		req2.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
		req2.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
		req2.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");
		req2.setPostParameter("PTAXViewer1$_imgContainerWidth", "0");
		req2.setPostParameter("PTAXViewer1$_imgContainerHeight", "0");
		req2.setPostParameter("PTAXViewer1$isImageViewerVisible", "true");
		req2.setPostParameter("__ASYNCPOST", "true");
		req2.setPostParameter("SearchFormEx1$btnSearch", "Search");
		req2.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
		req2.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
		req2.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
		req2.setPostParameter("__EVENTTARGET", "");
		req2.setPostParameter("DocList1$ctl11", "");
		
		for (String s: empty_param1) {
			req2.setPostParameter(s, "");
		}
		
		String resp2 = execute(req2);
		
		String eventTarget = "";
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp2, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocList1_GridView_Document"));
			if (tableList.size()>0) {
				TableTag table = (TableTag)tableList.elementAt(0);
				if (table.getRowCount()==2) {	//the header and one result
					Matcher ma1 = Pattern.compile("(?is)<a[^>]+href=\"javascript:__doPostBack\\('([^']+)'[^)]+\\)\"[^>]*>" +  docno + "</a>").matcher(resp2);
					if (ma1.find()) {
						eventTarget = ma1.group(1);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!StringUtils.isEmpty(eventTarget)) {
			HTTPRequest req3 = new HTTPRequest(getSiteLink() + "?AspxAutoDetectCookieSupport=1", HTTPRequest.POST);
			req3.setPostParameter("ScriptManager1", "DocList1$UpdatePanel|" + eventTarget);
			req3.setPostParameter("Navigator1$SearchOptions1$DocImagesCheck", "on");
			req3.setPostParameter("SearchFormEx1$ACSTextBox_Document", docno);
			req3.setPostParameter("SearchFormEx1$ACSDropDownList_DocumentType", "-2");
			req3.setPostParameter("SearchFormEx1$ACSTextBox_DateFrom", "1/1/1985");
			req3.setPostParameter("SearchFormEx1$ACSTextBox_DateTo", today);
			req3.setPostParameter("CertificateViewer1$_imgContainerWidth", "0");
			req3.setPostParameter("CertificateViewer1$_imgContainerHeight", "0");
			req3.setPostParameter("CertificateViewer1$isImageViewerVisible", "true");
			req3.setPostParameter("PTAXViewer1$_imgContainerWidth", "0");
			req3.setPostParameter("PTAXViewer1$_imgContainerHeight", "0");
			req3.setPostParameter("PTAXViewer1$isImageViewerVisible", "true");
			req3.setPostParameter("__ASYNCPOST", "true");
			req3.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
			req3.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
			req3.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
			req3.setPostParameter("__EVENTTARGET", eventTarget);
			req3.setPostParameter("DocList1$ctl11", "0");
			
			for (String s: empty_param1) {
				req3.setPostParameter(s, "");
			}
			
			execute(req3);
			
			return downloadImage(docno, fileName);
		}
		
		return null;
		
	}

	/**
	 * @param docno
	 * @param fileName
	 * @return 
	 */
	public byte[] downloadImage(String docno, String fileName) {
		
		HTTPRequest req4 = new HTTPRequest(getSiteLink(), HTTPRequest.POST);
		req4.setPostParameter("imageSearch", "true");
		
		String resp = execute(req4);
		String sctkey = "";
		String patt1 = "(?is)\\bSCTKEY=([^']+)'";
		Matcher ma2 = Pattern.compile(patt1).matcher(resp);
		if (ma2.find()) {
			sctkey = ma2.group(1);
		} else{
			HTTPRequest req5 = new HTTPRequest(getSiteLink().replaceFirst("(?is)/default.aspx$", "/ImageViewerEx.aspx"), HTTPRequest.GET);
			resp = execute(req5);
			ma2 = Pattern.compile(patt1).matcher(resp);
			if (ma2.find()) {
				sctkey = ma2.group(1);
			}
		}
		
		if (!StringUtils.isEmpty(sctkey)) {
			String url = getSiteLink().replaceFirst("(?is)/default.aspx$", "/ACSResource.axd");
			url += "?SCTTYPE=ENCRYPTED&SCTKEY=" + sctkey + "&CNTWIDTH=1800&CNTHEIGHT=2400&FITTYPE=Page&ZOOM=1";
			HTTPRequest req6 = new HTTPRequest(url, HTTPRequest.GET);
			HTTPResponse resp6 = process(req6);
			
			List<String> pages = new ArrayList<String>();
			String fileNameTemp = getSearch().getImageDirectory() +	File.separator + "tmp" + docno;
			int index = 1;
			convertJpgToTiff(resp6.getResponseAsStream(), fileNameTemp, index, pages);
			
			boolean hasMore = false;
			String patt2 = "(?is)<span[^>]+id=\"imageViewer1_lblPageNum\"[^>]*>(.+)?of(.+)?</span>";
			Matcher ma3 = Pattern.compile(patt2).matcher(resp);
			if (ma3.find()) {
				String s1 = ma3.group(1).trim();
				String s2 = ma3.group(2).trim();
				if (!s1.equals(s2)) {
					hasMore = true;
				}
			}
			try {
				while (hasMore) {
					index++;
					HTTPRequest req7 = new HTTPRequest(getSiteLink().replaceFirst("(?is)/default.aspx$", "/ImageViewerEx.aspx"), HTTPRequest.POST);
					req7.setPostParameter("ScriptManager1", "ImageViewer1$UpdatePanel1|ImageViewer1$BtnNext");
					req7.setPostParameter("__EVENTTARGET", "");
					req7.setPostParameter("__EVENTARGUMENT", "");
					req7.setPostParameter("__VIEWSTATE", "");
					req7.setPostParameter("ImageViewer1$ScrollPos", "");
					req7.setPostParameter("ImageViewer1$ScrollPosChange", "");
					req7.setPostParameter("ImageViewer1$_imgContainerHeight", "0");
					req7.setPostParameter("ImageViewer1$_imgContainerWidth", "0");
					req7.setPostParameter("ImageViewer1$isImageViewerVisible", "true");
					req7.setPostParameter("ImageViewer1$hdnWidgetSize", "");
					req7.setPostParameter("ImageViewer1$tbPageNum", "");
					req7.setPostParameter("ImageViewer1$TextBox_GoTo", "");
					req7.setPostParameter("ImageViewer1$imWidth", "");
					req7.setPostParameter("ImageViewer1$imHeight", "");
					req7.setPostParameter("ImageViewer1$DragResizeExtender_ClientState", "");
					req7.setPostParameter("__ASYNCPOST", "true");
					req7.setPostParameter("ImageViewer1$BtnNext", "");

					String resp7 = execute(req7);

					sctkey = "";
					Matcher ma4 = Pattern.compile(patt1).matcher(resp7);
					if (ma4.find()) {
						sctkey = ma4.group(1);
					}

					if (!StringUtils.isEmpty(sctkey)) {
						url = getSiteLink().replaceFirst("(?is)/default.aspx$", "/ACSResource.axd");
						url += "?SCTTYPE=ENCRYPTED&SCTKEY=" + sctkey + "&CNTWIDTH=1800&CNTHEIGHT=2400&FITTYPE=Page&ZOOM=1";
						HTTPRequest req8 = new HTTPRequest(url, HTTPRequest.GET);
						HTTPResponse resp8 = process(req8);

						convertJpgToTiff(resp8.getResponseAsStream(), fileNameTemp, index, pages);
					}

					hasMore = false;
					Matcher ma5 = Pattern.compile(patt2).matcher(resp7);
					if (ma5.find()) {
						String s1 = ma5.group(1).trim();
						String s2 = ma5.group(2).trim();
						if (!s1.equals(s2)) {
							hasMore = true;
						}
					}
				}
			} catch (Exception e) {
			}
			
			TiffConcatenator.concatenate(pages.toArray(new String[pages.size()]), fileName);
			
			for(int i = 0; i < pages.size(); i++){
			   	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			   	FileUtils.deleteFile(pages.get(i));
			   }
			
			return FileUtils.readBinaryFile(fileName);
			
		}
		return null;
	}
	
	/**
	* Read one page out of the TIFF file and return it.
	*
	* @param imageFile
	* @param pageNumber
	* @return
	* @throws IOException
	*/
	private static IIOImage readDocumentPage(File imageFile) throws IOException
	{
	ImageReader tiffReader = null;

	try
	{
		// locate a TIFF reader
		Iterator<ImageReader> tiffReaders = ImageIO.getImageReadersByFormatName("tiff");
		if (!tiffReaders.hasNext()) throw new IllegalStateException("No TIFF reader found");
			tiffReader = tiffReaders.next();
	
		// point it to our image file
		ImageInputStream tiffStream = ImageIO.createImageInputStream(imageFile);
		tiffReader.setInput(tiffStream);
	
		// read one page from the TIFF image
		return tiffReader.readAll(0, null);
	}
	finally
	{
		if (tiffReader != null) tiffReader.dispose();
	}
	}
	
	/**
	* Rescale the input image to fit the maximum dimensions indicated.
	* Only large images are shrunk; small images are not expanded.
	*
	* @param source
	* @param maxWidth
	* @param maxHeight
	* @return
	*/
	private static RenderedImage scaleImage(IIOImage source, float maxWidth, float maxHeight)
	{
	// shrink but respect the original aspect ratio
	float scaleFactor;
	if (source.getRenderedImage().getHeight() > source.getRenderedImage().getWidth()){
		scaleFactor = maxHeight / source.getRenderedImage().getHeight();
	} else {
		scaleFactor = maxWidth / source.getRenderedImage().getWidth();
	}

	if (scaleFactor >= 1){
		// don't expand small images, only shrink large ones
		return source.getRenderedImage();
	}

	// prepare parameters for JAI function call
	ParameterBlockJAI params = new ParameterBlockJAI("scale");
	params.addSource(source.getRenderedImage());
	params.setParameter("xScale", scaleFactor);
	params.setParameter("yScale", scaleFactor);

	RenderedOp resizedImage = JAI.create("scale", params);

	return resizedImage;
	}
	
	public static BufferedImage convertToBlackAndWhite(BufferedImage image) {
		BufferedImage blackAndWhiteImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D g = (Graphics2D) blackAndWhiteImage.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return blackAndWhiteImage;
	} 
	
	private static void convertJpgToTiff(InputStream inputStream, String fileNameTemp, int index, List<String> pages) {
		try{
			
			BufferedImage img = ImageIO.read(inputStream);
	        TIFFImageWriterSpi tiffspi = new TIFFImageWriterSpi();
	        ImageWriter writer = tiffspi.createWriterInstance();
	        
	        ImageWriteParam param = writer.getDefaultWriteParam();
	        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	        param.setCompressionType("LZW");
	        param.setCompressionQuality(0.9f);
	       
	        File fOutputFile = null;
	        fOutputFile = new File(fileNameTemp + "Page" + index + ".tiff");
	        pages.add(fOutputFile.toString());
	        
	        ImageOutputStream ios = ImageIO.createImageOutputStream(fOutputFile);
	        writer.setOutput(ios);
	        writer.write(null, new IIOImage(img, null, null), param);
	        ios.close();
	        
	        // read one page as image
	        IIOImage pageImage = readDocumentPage(fOutputFile);

	        // rescale image to fit
	        RenderedImage scaledImage = scaleImage(pageImage, 1800, 2400);
	        PlanarImage image= new RenderedImageAdapter (scaledImage) ; 

	        BufferedImage bufImg = image.getAsBufferedImage(); 
	        ios = ImageIO.createImageOutputStream(fOutputFile);
	        writer.setOutput(ios);
	        writer.write(null, new IIOImage(convertToBlackAndWhite(bufImg), null, null), param);
	        ios.close();
	    
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}
