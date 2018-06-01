/*
 * Created on Nov 8, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.fsearch;


import java.io.IOException;
import java.util.Vector;

import ro.cst.tsearch.servers.response.SaleDataSet;

//import ro.cst.fsearch.util.Log;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CreateResponseHTML {

	private final static String msRegisterHTMLTemplate=
		"<html><body><hr>Instrument # "
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
	
	public static String createShelbyRegisterDoc( Vector vFile, SaleDataSet sds, String linkPrefix) throws IOException {
		
			//String hrefFormat = new String();
		
			String sHtml= msRegisterHTMLTemplate;
			sHtml=sHtml.replaceFirst("MyInstrumentNo", sds.getAtribute("InstrumentNumber"));
			sHtml=sHtml.replaceFirst("MyInstrumentYear", "");
			sHtml=sHtml.replaceFirst("In Year", "");
			sHtml=sHtml.replaceFirst("MyFileDate", "N/A");
			sHtml=sHtml.replaceFirst("MyInstrumentType", sds.getAtribute("DocumentType"));
			sHtml=sHtml.replaceFirst("MyInstrumentDate", "N/A");
			sHtml=sHtml.replaceFirst("MyBook", sds.getAtribute("Book"));
			sHtml=sHtml.replaceFirst("MyPage", sds.getAtribute("Page"));
			sHtml=sHtml.replaceFirst("MyGrantor", sds.getAtribute("Grantor"));
			sHtml=sHtml.replaceFirst("MyGrantee", sds.getAtribute("Grantee"));
			//sHtml=sHtml.replaceFirst("MyView", "N/A");
			
			/*
			if( vFile.size() == 0){
				
				sHtml = sHtml.replaceFirst("MyHrefToImage", "");
				sHtml = sHtml.replaceFirst("</a>","");
				sHtml = sHtml.replaceFirst("MyView", "No Image");
				sHtml = sHtml.replaceFirst("MyNoImage","NO");
			} else if( vFile.size() == 1){
				hrefFormat = "<a href=\"" + linkPrefix + vFile.elementAt(0).toString() + "\">";
				hrefFormat = hrefFormat.replaceAll(File.separator+ File.separator, File.separator+ File.separator+File.separator+ File.separator);
				sHtml = sHtml.replaceFirst("MyHrefToImage", hrefFormat);
				sHtml = sHtml.replaceFirst("MyView", "View Image");
				sHtml = sHtml.replaceFirst("MyNoImage","");
			} else if( vFile.size() > 1) {
				hrefFormat = "Pages: ";
				sHtml = sHtml.replaceFirst("</a>","");
				
				for( int i = 0; i< vFile.size(); i++){
					hrefFormat = hrefFormat + " <a href=\"" + linkPrefix  
							+ ((File)vFile.elementAt(i)).toString() + "\"> " 
							+ new Integer(i+1).toString() + "</a>"; 
				}
				hrefFormat = hrefFormat.replaceAll(File.separator+ File.separator, File.separator+ File.separator+File.separator+ File.separator);
				sHtml = sHtml.replaceFirst("MyHrefToImage", hrefFormat);
				sHtml = sHtml.replaceFirst("MyView","");
				sHtml = sHtml.replaceFirst("MyNoImage","");
			}
			*/
			sHtml = sHtml.replaceFirst("MyHrefToImage", "");
			sHtml = sHtml.replaceFirst("</a>","");
			sHtml = sHtml.replaceFirst("MyView", "");
			sHtml = sHtml.replaceFirst("MyNoImage","");
			
			return sHtml;
	}

}