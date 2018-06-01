package ro.cst.tsearch.titledocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Vector;
import org.apache.log4j.Category;
import ro.cst.tsearch.jsp.utils.TopBar;
import ro.cst.tsearch.utils.FileCopy;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;

@Deprecated
public class FakeDocumentsCreator
{
	protected static final Category logger= Category.getInstance(FakeDocumentsCreator.class.getName());
	public final static String ViewImage = "View Image";
	public final static String NoImage	= "No Image";
	public final static String MultipageProcessing = "Pages: ";
	public final static String NoParseHTML = "_NO_PARSE";
	
	public final static String countyHtmlTemplate =  "<td valign=\"top\" align=\"left\"><font face=\"arial\" size=\"2\">MyTaxYear</font></td>" +
	"<td valign=\"top\" align=\"left\"><font face=\"arial\" size=\"2\">MyCounty</font></td>" +
	"<td valign=\"top\" align=\"left\"><font face=\"arial\" size=\"2\">N/A</font></td>" +
	"<td valign=\"top\" align=\"right\"><font face=\"arial\" size=\"2\">MyTaxDue</font></td>" +
	"<td valign=\"top\" align=right><font face=\"arial\" size=\"2\">N/A</font></td>" +
	"<td valign=\"top\" align=right><font face=\"arial\" size=\"2\">N/A</font></td>" +
	"<td valign=\"top\" align=right><font face=\"arial\" size=\"2\">MyTotal</font></td>" +
	"</tr>";
	
	
	public final static String cityHtmlTemplate =  
	    "	<tr>" +
	    "<td class='table-body' width='30'>MyTaxYear</td>" +
	    "<td align='center' class='table-body' width='30'>REAL</td>" +
	    "<td align='right' class='table-body' width='75'>N/A</td>" +
	    "<td align='center' class='table-body' width='50'>N/A</td>" +
	    "<td align='right' class='table-body' width='58'>N/A</td>" +
	    "<td align='right' class='table-body' width='68'>MyTaxAssesed</td>" +
	    "<td align='right' class='table-body' width='70'>N/A</td>" +
	    "<td align='right' class='table-body' width='55'>N/A</td>" +
	    "<td align='right' class='table-body' width='75'>MyTotalDue</td>" +
	    "</tr>";
	
	private final static String msRegisterHTMLTemplate=
		"<html><head>" + TopBar.getHeaderMetaNoCache() +
		"</head><body><hr>Instrument# "
			+ "MyInstrumentNo"
			+ " . In Year "
			+ "MyInstrumentYear"
			+ "<br>"
			+ "MyHrefToImage"
			+ "MyView</a>"
			+ "<table width=95% >"
			+ "<tr><td width=25 rowspan=3></td><td><table width=100%  border=3><tr><td width=20% bgcolor=white>"
			+ "File Date: "
			+ "MyFileDate"
			+ "</td><td width=20% bgcolor=white>File Time: </td><td width=20% bgcolor=white>Tax$</td><td width=20% bgcolor=white>Trans$ 0.00</td><td width=20%  ><b>NOT VERIFIED</b></td></tr><tr><td colspan=2 bgcolor=white>"
			+ "Doc Type: "
			+ "MyInstrumentType"
			+ "</td><td width=20% bgcolor=white></td><td width=20% bgcolor=white>Mort $ 0.00</td><td width=20%  ><b>"
			+ "MyNoImage"
			+ " IMAGE ON FILE"
			+ "</b></td></tr><tr><td width=20% bgcolor=white>"
			+ "Book # "
			+ "MyBook"
			+ "</td><td width=20% bgcolor=white>"
			+ "Page # "
			+ "MyPage"
			+ "</td><td width=20% bgcolor=white>File # 0</td><td colspan=2 bgcolor=white>" 
			+ "Instrument Date : " 
			+ "MyInstrumentDate </td></tr></table><table width=100% ><tr><td width=30% ALIGN=center bgcolor=silver><font size=3 face=arial color=black><b>"
			+ "Grantor(s)</b></font></td><td width=30% ALIGN=center bgcolor=silver><font size=3 face=arial color=black><b>Grantee(s)</b></font></td><td width=40% ALIGN=center bgcolor=silver><font size=3 face=arial color=black><b>Cross References</b></font></td></tr><tr><td width=30% ALIGN=left bgcolor=white>"
			+ "MyGrantor"
			+ "</td><td width=30% ALIGN=left bgcolor=white>"
			+ "MyGrantee"
			+ "</td><td width=40%  valign=top><table width=95% ><tr><td width=5% align=left bgcolor=silver>" +
			"<font size=2 face=arial color=black>Link</font></td><td width=25% align=left bgcolor=silver>" +
			"<font size=2 face=arial color=black>Inst #</font></td><td width=20% align=left bgcolor=silver>" +
			"<font size=2 face=arial color=black>Year</font></td><td width=25% align=left bgcolor=silver>" +
			"<font size=2 face=arial color=black>XRef Book</font></td><td width=25% align=left bgcolor=silver>" +
			"<font size=2 face=arial color=black>XRef Page</font></td></tr></tr>" +
			"</table></td></tr></table><table width=45% align=left><tr><td width=25% align=left bgcolor=silver>" +
			"<font size=2 face=arial color=black>Parcel ID</font></td>" +
			"<td width=20% align=left bgcolor=silver><font size=2 face=arial color=black>Lot</font></td>" +
			"<td width=55% align=left bgcolor=silver><font size=2 face=arial color=black>Subdivision</font></td></table>" +
			"<table width=55% align=left><tr><td width=10% align=left bgcolor=silver><font size=2 face=arial color=black>Street #</font></td>" +
			"<td width=30% align=left bgcolor=silver><font size=2 face=arial color=black>Street Name</font></td>" +
			"<td width=20% align=left bgcolor=silver><font size=2 face=arial color=black>City</font></td>" +
			"<td width=5% align=left bgcolor=silver><font size=2 face=arial color=black>State</font></td>" +
			"<td width=10% align=left bgcolor=silver><font size=2 face=arial color=black>Zip</font></td></tr>" +
			"</table></td></tr><tr><td bgcolor=silver><font size=2 face=arial color=black>Comment:</font><br></td></tr><tr>" +
			"<td bgcolor=silver><font size=2 face=arial color=black>Legal Desc:</font><br></td></tr>" + 
			"</table></body></html>";			
	
	
	
	public static String createShelbyRegisterDoc(
			String instrumentNo,
			String instrumentYear,
			String fileDate,
			String instrumentType,
			String book,
			String page,
			String grantor,
			String grantee,
			String myView,
			Object tiffPath,
			String destHTML,
			String destTiff,
			String tiffHref,
			String extention) throws IOException
		{
			if (instrumentNo.trim().equals("") /*|| instrumentYear.trim().equals("")*/)
				throw new InvalidParameterException("Instrument No and/or Intrument Year are empty!");
			String sHtml= msRegisterHTMLTemplate;
			sHtml=sHtml.replaceFirst("MyInstrumentNo", instrumentNo);
			sHtml=sHtml.replaceFirst("MyInstrumentYear", instrumentYear);
			sHtml=sHtml.replaceFirst("MyFileDate", fileDate);
			sHtml=sHtml.replaceFirst("MyInstrumentType", instrumentType);
			sHtml=sHtml.replaceFirst("MyInstrumentDate", fileDate);
			sHtml=sHtml.replaceFirst("MyBook", book);
			sHtml=sHtml.replaceFirst("MyPage", page);
			sHtml=sHtml.replaceFirst("MyGrantor", grantor);
			sHtml=sHtml.replaceFirst("MyGrantee", grantee);
			sHtml=sHtml.replaceFirst("MyView", myView);
			File file = null;
			String fileName =null;
			Vector files = null;		
			if(tiffPath instanceof File)
					 file = (File)tiffPath;
			else if (tiffPath instanceof String)
					 fileName = (String)tiffPath;
			else if (tiffPath instanceof Vector)
					files = (Vector)tiffPath;
					
			if(tiffPath instanceof File){
				if (FileUtils.existPath (file))
				{
					FileUtils.CreateOutputDir(destTiff);
					FileCopy.copy(file,	destTiff);
					sHtml=sHtml.replaceFirst("MyHrefToImage", "<A HREF='" + tiffHref + "'>");
					sHtml=sHtml.replaceFirst("MyNoImage", "");
					if(file.delete()==true){
						logger.info("File "+ file.getName() + " succesufully deleted from WEB SERVER!");
					}else{
						logger.info("Cannot delete File "+ file.getName() + " from WEB SERVER!");				
					}
				}
				else
				{
					sHtml=sHtml.replaceFirst("MyHrefToImage", "");
					sHtml=sHtml.replaceFirst("MyNoImage", "NO");
				}
						 
			}else if(tiffPath instanceof String ){
				if (FileUtils.existPath (fileName))
				{
					FileUtils.CreateOutputDir(destTiff);
					FileCopy.copy(fileName,	destTiff);
					sHtml=sHtml.replaceFirst("MyHrefToImage", "<A HREF='" + tiffHref + "'>");
					sHtml=sHtml.replaceFirst("MyNoImage", "");
				}
				else if(fileName.equals("Multipage Procesing...")){
					sHtml=sHtml.replaceFirst("MyHrefToImage", "");
					sHtml=sHtml.replaceFirst("MyNoImage", "Multipage Procesing...");				
				}else	
				{
					sHtml=sHtml.replaceFirst("MyHrefToImage", "");
					sHtml=sHtml.replaceFirst("MyNoImage", "NO");
				}
				
			} else if(tiffPath instanceof Vector){
				int i =0;
				String href="";
				FileUtils.CreateOutputDir(destTiff); 
				for(i=0;i<files.size();i++){
					File currentFile  = (File)files.elementAt(i);
					String currentExtension="";
					if(currentFile!=null)
						currentExtension = (currentFile.getName().substring(currentFile.getName().lastIndexOf(".")+1,currentFile.getName().length())).toLowerCase();
					String parseFlag =(currentExtension.startsWith("htm")?(FakeDocumentsCreator.NoParseHTML):(""));
					String currentTiffName = (destTiff + parseFlag + "_"+i + "." + currentExtension).trim();
					FileCopy.copy((File)files.elementAt(i),currentTiffName.trim());
					tiffHref = tiffHref.replaceAll(NoParseHTML, "");
					href =href + " <A HREF=\""+ /*currentTiffName*/tiffHref.substring(0, tiffHref.lastIndexOf(".")).trim() + parseFlag + "_" + i + "."+currentExtension +  "\"> " + (i + 1) + "</a>";
				}
				// nu este in acelasi for deoarce pot sa am pagini cu acelasi nume si continut...
				for(i=0;i<files.size();i++){
					if(files.elementAt(i)!=null)
						if(((File)files.elementAt(i)).delete()==true){
							logger.info("File "+ ((File)files.elementAt(i)).getName() + " succesufully deleted from WEB SERVER!");
						}else{
							logger.info("Cannot delete File "+ ((File)files.elementAt(i)).getName() + " from WEB SERVER!");
						}
				}			
				sHtml=sHtml.replaceFirst("MyNoImage", "");
				sHtml=sHtml.replaceFirst("</a>", "");
				sHtml=sHtml.replaceFirst(myView, "");
				sHtml=StringUtils.replaceString(sHtml,"MyHrefToImage", "Pages  "+ href);
			} else if(tiffPath ==null){
				sHtml=sHtml.replaceFirst("MyHrefToImage", "");
				sHtml=sHtml.replaceFirst("MyNoImage", "NO");
				sHtml=sHtml.replaceFirst(myView, NoImage);			
			}
			
			FileUtils.CreateOutputDir(destHTML);
			FileOutputStream out= new FileOutputStream(destHTML);
			out.write(sHtml.getBytes());
			out.flush();
			out.close();
			
			return sHtml;
		}
	
	public static String createTNWilsonBackScannedRegisterDoc(
			String instrumentNo,
			String instrumentYear,
			String fileDate,
			String instrumentType,
			String book,
			String page,
			String grantor,
			String grantee,
			String myView,
			String destHTML,
			String tiffHref,
			String extention) throws IOException
		{
			if (instrumentNo.trim().equals("") /*|| instrumentYear.trim().equals("")*/)
				throw new InvalidParameterException("Instrument No and/or Intrument Year are empty!");
			String sHtml= msRegisterHTMLTemplate;
			sHtml=sHtml.replaceFirst("MyInstrumentNo", instrumentNo);
			sHtml=sHtml.replaceFirst("MyInstrumentYear", instrumentYear);
			sHtml=sHtml.replaceFirst("MyFileDate", fileDate);
			sHtml=sHtml.replaceFirst("MyInstrumentType", instrumentType);
			sHtml=sHtml.replaceFirst("MyInstrumentDate", fileDate);
			sHtml=sHtml.replaceFirst("MyBook", book);
			sHtml=sHtml.replaceFirst("MyPage", page);
			sHtml=sHtml.replaceFirst("MyGrantor", grantor);
			sHtml=sHtml.replaceFirst("MyGrantee", grantee);
			sHtml=sHtml.replaceFirst("MyView", myView);
			sHtml=sHtml.replaceFirst("MyHrefToImage", "<A HREF='" + tiffHref + "'>");
			sHtml=sHtml.replaceFirst("MyNoImage", "");			

			
			FileUtils.CreateOutputDir(destHTML);
			FileOutputStream out= new FileOutputStream(destHTML);
			out.write(sHtml.getBytes());
			out.flush();
			out.close();
			
			return sHtml;
		}



	public static final boolean isNumber(String string){
		if(string.equals("") || string.equals("N/A")){
			return false;
		}
		return true;
	}											   											   
}