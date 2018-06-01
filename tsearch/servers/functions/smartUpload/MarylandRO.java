package ro.cst.tsearch.servers.functions.smartUpload;

import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.utils.InstanceManager;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.smartupload.client.FrameContent;
import com.stewart.ats.smartupload.server.SmartUploadServer;
import com.stewart.ats.tsrindex.client.InstrumentG;
import com.stewart.ats.tsrindex.client.SimpleChapter;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;


/**
 * @author cristian stochina
 */
@SuppressWarnings("deprecation")
public class MarylandRO implements SmartUpload{

	private static final String BASE_SITE_LINK = "http://www.mdlandrec.net/msa/stagser/s1700/s1741/cfm/";
	
	// Create an instance of HttpClient.
    final private HttpClient client = new HttpClient();
    {
    	client.setConnectionTimeout(30000);
    	client.setTimeout(30000);
    }
	 
	@Override
	public List<ParseResult> parseDocument(FrameContent content) {
		String htmlContent = content.getHtmlContent();
		 
		List<String> allImageLinks = new ArrayList<String>();
		List<String> allFullInfoLinks = new ArrayList<String>();
		
		if(htmlContent.contains("Selected Records")){
			Pattern patFullInfo = Pattern.compile("(?i)\"(dsp_book.cfm[^\"]+)\"[^']+'(dsp_fullcitation.cfm[^']+)'");
			Matcher matFullInfo = patFullInfo.matcher(htmlContent);
			
			while(matFullInfo.find()){
				allImageLinks.add(matFullInfo.group(1).replaceAll("(?i)[&]amp;", "&"));
				allFullInfoLinks.add(matFullInfo.group(2).replaceAll("(?i)[&]amp;", "&"));
			}
		} 
		
		List<ParseResult> res = new ArrayList<ParseResult>();
		
		if(allFullInfoLinks.size()>0 && allImageLinks.size()==allFullInfoLinks.size()){
			
			for(int i=0;i<allFullInfoLinks.size();i++){
				String fullPage = getHtmlPageUsingLink( BASE_SITE_LINK +allFullInfoLinks.get(i), client , content.getCookie(), buildReferer(content.getCookie()));
				
				if(fullPage.indexOf("Land Records Additional Index")>0){
					
					/*try {
						FileUtils.writeStringToFile(new File("e:/5.html"), fullPage);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}*/
					
					int start = fullPage.indexOf("<html>");
					int stop = fullPage.indexOf("</html>");
					
					if(start>0 && stop>start){
						fullPage = fullPage.substring(start,stop+7);
					}
					
					String book = "";
					String page = "";
					Date recordedDate = null;
					String imageLink = BASE_SITE_LINK + allImageLinks.get(i);
					String docNo="";
					String grantor = "";
					String grantee = "";
					String serverDocType = "";
					ArrayList<InstrumentG> parsedReferences = new ArrayList<InstrumentG>();
					
					try{
						
						Pattern patBookPage = Pattern.compile("(?i)<A[^>]+>([^0-9]+([0-9-]+)[,][^0-9]+([0-9]+)[^<]*)</a>");
						Matcher matBook = patBookPage.matcher(fullPage);
						if( matBook.find() ){
							book = matBook.group(2).trim();
							page = matBook.group(3).trim();
						}
						
						Pattern patRecorded = Pattern.compile("(?i)<b>\\s*Recordation\\s+Date:\\s*</b>[^0-9]+([^<]+)<br>");
						Matcher matRecorded = patRecorded.matcher(fullPage);
						if( matRecorded.find() ){
							recordedDate = Util.dateParser3(matRecorded.group(1).trim());
						}
						
						Pattern patGrantors = Pattern.compile("(?i)<td[^>]+>\\s+<i>\\s*Grantor:\\s*</i>([^<]+)</td>");
						Matcher matGrantors = patGrantors.matcher(fullPage);
						
						StringBuilder allGrantors = new StringBuilder();
						while(matGrantors.find()){
							allGrantors.append(matGrantors.group(1).replaceAll("(?i)[&]nbsp;", " ").replaceAll("\\s+", " ").trim());
							allGrantors.append(" and ");
						}
						if(allGrantors.length()>6){
							grantor = allGrantors.toString().substring(0,allGrantors.length()-5);
						}
						
						Pattern patGrantees = Pattern.compile("(?i)<td[^>]+>\\s+<i>\\s*Grantee:\\s*</i>([^<]+)</td>");
						Matcher matGrantees = patGrantees.matcher(fullPage);
						
						StringBuilder allGrantees = new StringBuilder();
						while(matGrantees.find()){
							allGrantees.append(matGrantees.group(1).replaceAll("(?i)[&]nbsp;", " ").replaceAll("\\s+", " ").trim());
							allGrantees.append(" and ");
						}
						if(allGrantees.length()>6){
							grantee = allGrantees.toString().substring(0, allGrantees.length()-5);
						}
						
						Pattern patDocType = Pattern.compile("(?i)<b>\\s*Instrument:\\s*</b>([^<]+)<br>");
						Matcher matDocType = patDocType.matcher(fullPage);
						if( matDocType.find() ){
							serverDocType = matDocType.group(1).trim();
						}
						
						
						Pattern patCrossRef = Pattern.compile("(?i)<tr>\\s*<td>([0-9]+)</td>\\s*<td>([0-9]+)-?[0-9]*</td>\\s*<td>([0-9]{4}-[0-9]{1,2}-[0-9]{1,2})</td>\\s*<td>[^<]+</td>\\s*<td>[^<]+</td>\\s*</tr>");
						Matcher matCrossRef = patCrossRef.matcher(fullPage);
						while( matCrossRef.find() ){
							InstrumentG inst = new InstrumentG();
							inst.setBook(matCrossRef.group(1).trim());
							inst.setPage(matCrossRef.group(2).trim());
							Date date1 = Util.dateParser3(matCrossRef.group(3).trim());
							if(date1!=null){
								Calendar cal = Calendar.getInstance();
								cal.setTime(date1);
								inst.setYear(cal.get(Calendar.YEAR));
							}
							parsedReferences.add(inst);
						}
						
						fullPage =  fullPage.replaceAll("(?i)<A[^>]+>([^<]+)</a>","$1");
						fullPage =  fullPage.replaceAll("(?i)<input[^>]*>","");
						fullPage =  fullPage.replace("Retrieving Information, Please Wait...", "");
						
					}catch(Exception e){
						e.printStackTrace();
					}
					
					if( recordedDate!=null && ((StringUtils.isNotBlank(book)&&StringUtils.isNotBlank(page))||StringUtils.isNotBlank(docNo)) ){
						
						SimpleChapter model = new SimpleChapter(DType.ROLIKE, "");
						model.setRecordedDate(recordedDate);
						if(recordedDate!=null){
							Calendar cal = Calendar.getInstance();
							cal.setTime(recordedDate);
							model.setYear(cal.get(Calendar.YEAR));
						}
						model.setBook(book);
						model.setPage(page);
						model.setInstno(docNo);
						model.setDataSource("RO");
						model.setDocType("MISCELLANEOUS");
						model.setGrantorFreeForm(grantor);
						model.setGranteeFreeForm(grantee);
						model.setGranteeLender(grantee);
						model.setServerDocType(serverDocType);
						model.setParsedReferences(parsedReferences);
						ParseResult result = new ParseResult();
						
						result.setSimpleChapter(model);
						result.setIndex(fullPage);
						result.setImageLink(imageLink);
						res.add(result);
					}
				}
			}
		}
		
		return res;
	}

	private static String buildReferer(String cookie){
		if(StringUtils.isBlank(cookie)){
			return "";
		}
		String[] cookies = cookie.split(";");
			     
		if(cookies.length==2){
			List<HttpCookie> cookiesList1 = HttpCookie.parse(cookies[0]);
		    List<HttpCookie> cookiesList2 = HttpCookie.parse(cookies[1]);
		    if(cookiesList1 .size()>0&&cookiesList2.size()>0 ){
			    String c1 = cookiesList1.get(0).getValue();
			    String c2 = cookiesList2.get(0).getValue();
			    return "http://www.mdlandrec.net/msa/stagser/s1700/s1741/cfm/act_search1.cfm?CFID="+c1+"&CFTOKEN="+c2+"&select=true";
		    }
		}
		
		return "";
	}
	
	public static String getHtmlPageUsingLink(String link, HttpClient client, String cookie, String referer){
		 
		// Create a method instance.
	     GetMethod method = new GetMethod(link);
	     // Provide custom retry handler is necessary
	     method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(2, false));

	     if(StringUtils.isNotBlank(cookie)){
		     method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
		     method.setRequestHeader("Cookie", cookie);
		     
	     }
	     
	     if(StringUtils.isNotBlank(referer)){
	    	 method.setRequestHeader("Referer", referer);
	     }
	     
	     try {
	       // Execute the method.
	       int statusCode = client.executeMethod(method);

	       if (statusCode != HttpStatus.SC_OK) {
	         System.err.println("Method failed: " + method.getStatusLine());
	       }
	       
	       String mime = "";
	       try{mime = method.getResponseHeader("Content-Type").getValue();}catch(Exception e){}
	       if(mime == null) {
	    	   throw new RuntimeException("Unknown uploaded URL mime/type in null");
	       }
	       int someIndex = mime.indexOf(";");
	       
	       if(someIndex > 0) {
	    	   mime = mime.substring(0, someIndex);
	       }
	       
	      return method.getResponseBodyAsString();
	       
	     } catch (HttpException e) {
	       System.err.println("Fatal protocol violation: " + e.getMessage());
	       e.printStackTrace();
	     } catch (IOException e) {
	       System.err.println("Fatal transport error: " + e.getMessage());
	       e.printStackTrace();
	     } finally {
	       // Release the connection.
	       method.releaseConnection();
	     }  
	     
	     return "";
	}
	
	@Override
	public boolean downloadAndSetImage(DocumentI doc, FrameContent content,	ParseResult result, long searchId) {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		String imageLink = result.getImageLink();
		if(imageLink.length()>0){
			
			String htmlImage = getHtmlPageUsingLink(imageLink, client, content.getCookie(), buildReferer(content.getCookie()));
			Pattern patImageLink = Pattern.compile("(?i)<iframe[^>]+src=\"(/tmp/LandRec/[^\"]+)\"[^>]+>\\s+</iframe>");
			Matcher matImageLink = patImageLink.matcher(htmlImage);
			if( matImageLink.find() ){
				imageLink = "http://www.mdlandrec.net" + matImageLink.group(1).trim();
			}
			
			global.addImagesToDocument(doc, imageLink);
			doc.setIncludeImage(true);
			doc.setImageUploaded(true);
			
			byte []responseBody = SmartUploadServer.downloadByteContentFromLink(imageLink, client);
		    if(responseBody !=null){ 
		    	try {
					org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(doc.getImage().getPath()), responseBody);
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
		    }
		}
		 return false;
	}

	
}
