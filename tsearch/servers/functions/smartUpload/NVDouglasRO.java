package ro.cst.tsearch.servers.functions.smartUpload;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.utils.InstanceManager;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.smartupload.client.FrameContent;
import com.stewart.ats.tsrindex.client.SimpleChapter;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

@SuppressWarnings("deprecation")
public class NVDouglasRO implements SmartUpload{

	// Create an instance of HttpClient.
    final private HttpClient client = new HttpClient();
    {
    	client.setConnectionTimeout(30000);
    	client.setTimeout(30000);
    }
	
	@Override
	public List<ParseResult> parseDocument(FrameContent content) {
		
		String documentContent =content.getHtmlContent(); 
		boolean isMarriageDocument = documentContent.contains("Marriage Document");
		
		if(isMarriageDocument){
			int start = documentContent.indexOf("<table");
			if(start>0){
				int end = documentContent.lastIndexOf("</table>");
				if(end>start){
					documentContent = documentContent.substring(start,end+8);
				}
			}
		}else{
			int start = documentContent.indexOf("<div id=\"content-wrap\">");
			if(start>0){
				int end = documentContent.indexOf("</div>",start+1);
				if(end>start){
					documentContent = documentContent.substring(start,end+6);
				}
			}
		}
		
		String book = "";
		String page = "";
		Date recordedDate = null;
		String imageLink = "";
		String docNo="";
		String grantor = "";
		String grantee = "";
		String serverDocType = "";
		
		try{
			if(isMarriageDocument){
				Pattern patBookPage = Pattern.compile("(?i)<tr>\\s*<td>\\s*Book\\s+and\\s+Page\\s*:\\s*</td>\\s*<td>\\s*([^,]+)\\s*,\\s*([^<]+)\\s*</td>\\s*</tr>");
				Matcher matBook = patBookPage.matcher(documentContent);
				if( matBook.find() ){
					book = matBook.group(1).trim();
					page = matBook.group(2).trim();
				}
				
				Pattern patDoc = Pattern.compile("(?i)<tr>\\s*<td>\\s*Doc\\s+Number\\s*:\\s*</td>\\s*<td>\\s*([^<]+)\\s+</td>");
				Matcher matDoc = patDoc.matcher(documentContent);
				if( matDoc.find() ){
					docNo = matDoc.group(1).trim();
				}
				
				Pattern patRecorded = Pattern.compile("(?i)<tr>\\s*<td>\\s*Date\\s+Recorded\\s*:\\s*</td>\\s*<td>\\s*([^<]+)\\s*</td>\\s*</tr>");
				Matcher matRecorded = patRecorded.matcher(documentContent);
				if( matRecorded.find() ){
					recordedDate = Util.dateParser3(matRecorded.group(1).trim());
				}
				
				Pattern patGrantors = Pattern.compile("(?i)Spouse:</td>\\s*<td[^>]*>([^<]+)</td>\\s*</tr>");
				Matcher matGrantors = patGrantors.matcher(documentContent);
				
				StringBuilder allGrantors = new StringBuilder();
				while(matGrantors.find()){
					allGrantors.append(matGrantors.group(1).trim());
					allGrantors.append(" and ");
				}
				if(allGrantors.length()>6){
					grantor = allGrantors.toString().substring(0,allGrantors.length()-5);
				}
				
				serverDocType = "Marriage";
			}else{
				int imagePos = documentContent.indexOf("Document Image");
				if(imagePos>0){
					Pattern pat = Pattern.compile("(?i)<a\\s+href=\"([^\"]+)\"");
					Matcher mat = pat.matcher(documentContent.substring(imagePos));
				
					if(mat.find()){
						imageLink = "http://recorder.co.douglas.nv.us/docsearch/" + mat.group(1).trim();
					}
				}
				
				Pattern patBook = Pattern.compile("(?i)<td\\s+align=\"right\"\\s*>Book[^<]+</td>\\s+<td\\s+align=\"left\"\\s*>([^<]+)</td>");
				Matcher matBook = patBook.matcher(documentContent);
				if( matBook.find() ){
					book = matBook.group(1).trim();
				}
				
				Pattern patPage = Pattern.compile("(?i)<td\\s+align=\"right\"\\s*>Page[^<]+</td>\\s+<td\\s+align=\"left\"\\s*>([^<]+)</td>");
				Matcher matPage = patPage.matcher(documentContent);
				if( matPage.find() ){
					page = matPage.group(1).trim();
				} 
				
				Pattern patDoc = Pattern.compile("(?i)<td\\s+align=\"right\"\\s*>Document[^<]+</td>\\s+<td\\s+align=\"left\"\\s*>([^<]+)</td>");
				Matcher matDoc = patDoc.matcher(documentContent);
				if( matDoc.find() ){
					docNo = matDoc.group(1).trim();
				}
				
				Pattern patDocType = Pattern.compile("(?i)<tbody>\\s*<tr>\\s*<td[^>]*>([^<&]+)[^<]*</td>");
				Matcher matDocType = patDocType.matcher(documentContent);
				if( matDocType.find() ){
					serverDocType = matDocType.group(1).trim();
				}
				
				//Recorded: 09/01/2005
				Pattern patRecorded = Pattern.compile("(?i)Recorded:\\s+([0-9][0-9][/][0-9][0-9][/][0-9][0-9][0-9][0-9])");
				Matcher matRecorded = patRecorded.matcher(documentContent);
				if( matRecorded.find() ){
					try {
						recordedDate = (new SimpleDateFormat("MM/dd/yyyy")).parse(matRecorded.group(1).trim());
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				
				int startName = documentContent.indexOf("Name");
				if(startName>0){
					startName = documentContent.indexOf("<tr>",startName);
					if(startName>0){
						int stopName = documentContent.indexOf("</tbody>",startName);
						if(startName<stopName){
							String nameTable = documentContent.substring(startName,stopName);
							
							Pattern patGrantors = Pattern.compile("(?i)<td\\s+align=\"center\">1</td>\\s*<td>\\s*([^<]+)\\s*</td>");
							Matcher matGrantors = patGrantors.matcher(nameTable);
							
							StringBuilder allGrantors = new StringBuilder();
							while(matGrantors.find()){
								allGrantors.append(matGrantors.group(1).trim());
								allGrantors.append(" and ");
							}
							if(allGrantors.length()>6){
								grantor = allGrantors.toString().substring(0,allGrantors.length()-5);
							}
							
							Pattern patGrantees = Pattern.compile("(?i)<td\\s+align=\"center\">2</td>\\s*<td>\\s*([^<]+)\\s*</td>");
							Matcher matGrantees = patGrantees.matcher(nameTable);
							
							StringBuilder allGrantees = new StringBuilder();
							while(matGrantees.find()){
								allGrantees.append(matGrantees.group(1).trim());
								allGrantees.append(" and ");
							}
							if(allGrantees.length()>6){
								grantee = allGrantees.toString().substring(0,allGrantees.length()-5);
							}
							
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if( recordedDate!=null && ((StringUtils.isNotBlank(book)&&StringUtils.isNotBlank(page))||StringUtils.isNotBlank(docNo)) ){
			
			SimpleChapter model = new SimpleChapter(DType.ROLIKE, "");
			model.setRecordedDate(recordedDate);
			model.setBook(book);
			model.setPage(page);
			model.setInstno(docNo);
			model.setDataSource("RO");
			model.setDocType("MISCELLANEOUS");
			model.setGrantorFreeForm(grantor);
			model.setGranteeFreeForm(grantee);
			model.setServerDocType(serverDocType);
			ParseResult result = new ParseResult();
			
			result.setSimpleChapter(model);
			result.setIndex(documentContent);
			result.setImageLink(imageLink);
			List<ParseResult> res = new ArrayList<ParseResult>();
			res.add(result);
			return res;
		}
		
		return null;
	}

	@Override
	public boolean downloadAndSetImage(DocumentI doc, FrameContent content, ParseResult result, long searchId) {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		String imageLink = result.getImageLink();
		if(imageLink.length()>0){
			global.addImagesToDocument(doc, imageLink);
			doc.setIncludeImage(true);
			doc.setImageUploaded(true);
			
		     // Create a method instance.
		     GetMethod method = new GetMethod(imageLink);
		     // Provide custom retry handler is necessary
		     method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(2, false));

		     try {
		       // Execute the method.
		       int statusCode = client.executeMethod(method);

		       if (statusCode != HttpStatus.SC_OK) {
		         System.err.println("Method failed: " + method.getStatusLine());
		       }

		       // Read the response body.
		       byte[] responseBody = method.getResponseBody();
		       
		       String mime = "";
		       try{mime = method.getResponseHeader("Content-Type").getValue();}catch(Exception e){}
		       if(mime == null) {
		    	   throw new RuntimeException("Unknown uploaded URL mime/type in null");
		       }
		       int someIndex = mime.indexOf(";");
		       
		       if(someIndex > 0) {
		    	   mime = mime.substring(0, someIndex);
		       }
		       
		       org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(doc.getImage().getPath()), responseBody);
		       
		       return true;
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
		}
		 return false;
	}

}
