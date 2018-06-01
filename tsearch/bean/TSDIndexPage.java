package ro.cst.tsearch.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

@Deprecated
/**
 * Keep this class only for reopen searches
 */
public class TSDIndexPage implements Serializable, Cloneable {
	
	private static final long serialVersionUID = -1727090000058353208L;

	public static final int SEND_BY_MAIL = 0;

	public static final String PARAM_CHAPTER = "chapter";
	
	public static final int CHAPTER_NAME = 0, CHAPTER_LINK = 1,
	CHAPTER_CHECKBOX_VALUE = 2, CHAPTER_FILLED = 3,
	CHAPTER_GRANTOR = 4, CHAPTER_GRANTEE = 5, CHAPTER_INSTTYPE = 6,
	CHAPTER_INSTNO = 7, CHAPTER_BOOK = 8, CHAPTER_PAGE = 9,
	CHAPTER_REMARKS = 10, CHAPTER_CHECKBOX_CHECKED = 11,
	CHAPTER_REFERENCES = 12, CHAPTER_IS_TRANSFER = 13,
	CHAPTER_AMOUNTPAID = 14, CHAPTER_BASEAMOUNT = 15,
	CHAPTER_DELIQUENTAMOUNT = 16, CHAPTER_TAXYEAR = 17,
	CHAPTER_SERVER_INSTTYPE = 18, CHAPTER_IMAGE_PATH = 19,
	CHAPTER_UPLOADED = 20, CHAPTER_MANUAL_CHECK = 21,
	CHAPTER_TYPE = 22, CHAPTER_INCLUDE_IMAGE = 23,

	CHAPTER_INSTRUMENT_DATE = 24, CHAPTER_GRANTEE_TR = 25,
	CHAPTER_GRANTEE_LANDER = 26, CHAPTER_MORTGAGE_AMOUNT = 27,
	CHAPTER_DOCUMENT_NUMBER = 28, CHAPTER_FILLED_TIME = 29,

	CHAPTER_DOCTYPE_ABBREV = 30, CHAPTER_ADDRESS = 31,
	CHAPTER_TRANSFERS = 32, CHAPTER_SUBTYPE = 33,
	CHAPTER_LEGAL_DESC = 34, CHAPTER_SRCTYPE = 35,CHAPTER_isDONE=36,CHAPTER_isCHANGED=37,
	CHAPTER_CONSIDERATION_AMOUNT=38;
	
	public HashMap<String,String> tsdColorsMap = new HashMap<String, String>();
	
	@SuppressWarnings({ "unchecked" })
	public Vector mChapters;

	@SuppressWarnings({"unchecked" })
	public Vector mRegister;

	@SuppressWarnings({ "unchecked" })
	public Vector mAssesor;

	@SuppressWarnings({ "unchecked" })
	public Vector mCity;

	@SuppressWarnings({"unchecked" })
	public Vector mCounty;

	@SuppressWarnings({ "unchecked" })
	public Vector mOther;

	public long searchId = -1;
	

}
