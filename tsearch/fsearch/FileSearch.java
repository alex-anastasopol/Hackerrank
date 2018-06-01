/*
 * Created on Nov 4, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.fsearch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.utils.BookPageUtil;
import ro.cst.tsearch.utils.StringIntUtil;
import ro.cst.tsearch.utils.StringRet;
import ro.cst.tsearch.utils.URLMaping;
/**
 * @author Alexandru
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FileSearch {

	private static ResourceBundle rbc =	ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	public static final String SEARCH_TO = StringRet.getString(rbc, "filesearch.path") + File.separator;
	private static final Category logger = Category.getInstance(FileSearch.class.getName());
	private static final String DOCUMENT_TYPE = "PLAT";
	private static final String LOCAL_IMAGE_LINK = "localImageLink_";
	private static final String LOCAL_INDEX_LINK = "localIndexLink_";

	public static String searchByInstrBookPage(
		String instrmNumber,
		String bookVar,
		String pageVar,
		SaleDataSet sds) {


		Vector v = new Vector();
		String bookVarUnCh = bookVar;
		String htmlGenerated = new String();

		if (bookVar != null && !bookVar.equals(""))
			bookVar = StringIntUtil.varFormating(bookVar);
		/*
		 *		Instrument number are priorotate la cautare!~!~!~!~!~!~!~ 
		 */
		sds.setAtribute("DocumentType", DOCUMENT_TYPE);
		sds.setAtribute("Grantee", "&nbsp;");
		//logger.debug("Pathul initial de cautare este: " + SEARCH_TO);
		if (instrmNumber != null && !instrmNumber.trim().equals("")) {
			//logger.debug("incep sa caut dupa InstrumentNumber");
			instrmNumber = instrmNumber.toUpperCase();
			sds.setAtribute("InstrumentNumber", instrmNumber);
			String bookNumberReal = new String();
			
			int charNumbers = 0;
			int realBook = 0;

			for (int i = 0; i < instrmNumber.length(); i++) {
				if ((instrmNumber.charAt(i) >= 65)
					&& (instrmNumber.charAt(i) <= 90))
					charNumbers++;
			}

			DBConnection conn = null;
			try {
			    conn = ConnectionPool.getInstance().requestConnection();
				DatabaseData sqlResult = conn.executeSQL( "SELECT BOOK_NR, INIT_INSTR_NUM,"
							+ " FINAL_INSTR_NUM FROM TS_INSTRNUM_BOOK"
							+ " WHERE CHAR_NUM_FINAL = "
							+ charNumbers
							+ " ORDER BY BOOK_NR");
				if(sqlResult.getRowNumber() !=0 ) {
					String finalInstr = null;
					for (int i = 0; i < sqlResult.getRowNumber(); i++) {
						finalInstr = (String) sqlResult.getValue("FINAL_INSTR_NUM", i);
						if (finalInstr.compareTo(instrmNumber) >= 0) {
							realBook = i;
							break;
						}
					}
					bookNumberReal = (String) sqlResult.getValue("BOOK_NR", realBook);
					//logger.debug("Acum caut in directorul: " + SEARCH_TO + bookNumberReal);
					v = FileFindRec.search( new File(SEARCH_TO + bookNumberReal), instrmNumber);
				
					realBook = Integer.parseInt( bookNumberReal.substring(4, bookNumberReal.length()));
				}
				
			} catch (BaseException e) {
				logger.error("Error to get a connection to the database!", e);
			} catch (NullPointerException e) {
				logger.error("InstrumentNumer error!", e);
			}finally{
				try{
				    ConnectionPool.getInstance().releaseConnection(conn);
				}catch(BaseException e){
					logger.error(e);
				}			
			}
			
			//cazul de imbricare al inregistrarilor # se trece la cautarea in bookul urmator
			if (v.size() == 0  && realBook != 0) {
				//logger.debug("caut dupa InstrumentNumber in directoaqrele imbricate");
				int bookErr = Integer.parseInt(	bookNumberReal.substring(4, bookNumberReal.length())) + 1;
				String bookNumberErr = "book" + bookErr;
				//logger.debug("Acum caut in directorul: " + SEARCH_TO + bookNumberErr);
				if (bookErr / 10 < 1)
					bookNumberErr = "book00" + bookErr;
				if (bookErr / 100 < 1 && bookErr / 10 > 1)
					bookNumberErr = "book0" + bookErr;
				v =	FileFindRec.search(new File(SEARCH_TO + bookNumberErr), instrmNumber);
				realBook = bookErr;
			}
			//logger.debug("am gasit " + v.size() + " inregistrari pe local");
			//aici creez raspunsul
			if(realBook != 0 && v.size() != 0){
				sds.setAtribute("Book", new Integer(realBook).toString());
				sds.setAtribute("Page", BookPageUtil.instrumentToPage(v));
				try {
					htmlGenerated =	CreateResponseHTML.createShelbyRegisterDoc(	v, sds,	LOCAL_IMAGE_LINK);
				} catch (Exception e) {
				}
			} else {
				htmlGenerated = "";
			}

		} else {
			//logger.debug("incep sa caut dupa book page");
			v = FileFindBookPage.search(new File(SEARCH_TO + bookVar), pageVar);
			//logger.debug("am gasit " + v.size() + " inregistrari");
			if (v.size() > 0
			&& Integer.parseInt(bookVarUnCh) <= 102) {	//XXX search by book-page for book [1-102], restul de pe document server
				sds.setAtribute("Book", bookVarUnCh);
				sds.setAtribute("Page", pageVar);
/*				if (Integer.parseInt(bookVarUnCh) >= 33) {
					sds.setAtribute( "InstrumentNumber", BookPageUtil.pageToInstrum(v));
				} else { */
					sds.setAtribute( "InstrumentNumber", bookVarUnCh + "_" + pageVar);
//				}
				try {
					htmlGenerated =	CreateResponseHTML.createShelbyRegisterDoc(	v, sds,	LOCAL_IMAGE_LINK);
				} catch (Exception e) {
				}
			} else {
				htmlGenerated = "";
			}
		}
		return htmlGenerated;

	}

	public static RawResponseWrapper getFile(String link) {
		return getFile(link, null);
	}

	public static RawResponseWrapper getFile(String link, SaleDataSet sds) {
		if (link.startsWith(LOCAL_INDEX_LINK)) {
			String instrNo = getInstrNoFromLink(link);
			String bookNo = "";
			String pageNo = "";
			if (instrNo.indexOf("_") > -1) {
				bookNo = instrNo.substring(0, instrNo.indexOf("_"));
				pageNo = instrNo.substring(instrNo.indexOf("_") + "_".length());
				instrNo = "";
			}
			String htmlResponse =
				searchByInstrBookPage(instrNo, bookNo, pageNo, sds);
			return new RawResponseWrapper(htmlResponse);
		} else if (link.startsWith(LOCAL_IMAGE_LINK)) {
			String path = link.substring(LOCAL_IMAGE_LINK.length());
			BufferedInputStream buf = null;
			File file = new File(path);
			int fileLength = 0;
			try {
				buf = new BufferedInputStream(new FileInputStream(file));
				fileLength = (int) file.length();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				logger.error(" Not found local file for display ", e);
			}
			return new RawResponseWrapper("image/tiff", fileLength, buf);
		} else {
			return null;
		}
	}
	
	public static String getIndexLink(String bookNo, String pageNo) {
		return LOCAL_INDEX_LINK + bookNo + "_" + pageNo;
	}

	public static String getIndexLink(String instrNo) {
		return LOCAL_INDEX_LINK + instrNo;
	}

	public static String getInstrNoFromLink(String link) {
		return link.substring(LOCAL_INDEX_LINK.length());
	}
}
