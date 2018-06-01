package ro.cst.tsearch.servlet.utils;

/**
 * TSDIndexError
 *
 */
public class TSDIndexError
{
	public static final int ERROR_NULL_TSR = -1, // TSR fara documente
							ERROR_TRANSFER_NOT_FOUND = 0, // nu exista transfer
							ERROR_PLAT_NOT_FOUND = 1,// nu exista plat
							ERROR_TRANSFER_CHAIN_BROKEN = 2,// lantul de transferuri este invalid
							ERROR_INSTR_NOT_CONSECUTIVE = 3,// nr. de inreg.(TR->MO) nu sunt consecutive
							ERROR_AO_NOT_MATCH = 4,//grantorul de la AO si granteee-ul de la ultimul TR nu sunt aceiasi
							ERROR_TR_MORTGAGE_NOT_MATCH = 5,//grantorul de la ultimul TR si granteee-ul de la MO nu sunt aceiasi
							ERROR_MORTGAGE_RELEASED = 6,
							ERROR_AO_NOT_FOUND = 7,// AO nu s-a gasit
							ERROR_REL_ASG_NOT_FOUND = 8,
							ERROR_TR_MO_NOT_SAME_DAY = 9, // daca transf si mortgage-ul nu sunt in acceasi zi inregeistate
							ERROR_TR_MO_NOT_CONSEC = 10, // daca transf si mortgage-ul nu au instr# consecutive
							ERROR_TR_NOT_FOUND = 11, // nu s-a gasit document pe Tax Register
							ERROR_MISSING_RELEASED_INSTR = 12,
							ERROR_MISSING_RELEASING_INSTR = 13,
							ERROR_MISSING_ASSIGNED_INSTR = 14,
							ERROR_MISSING_ASSIGNING_INSTR = 15,
							ERROR_OUT_OF_SEQ = 16,
                            ERROR_IMPROPER_RELEASE = 17,
                            ERROR_TAX_RESEARCH_REQUIRED = 18,
                            ERROR_MANUAL_RESEARCH_REQUIRED = 19,
                            ERROR_OUTSTANDING_MORTGAGE = 20, //an outstanding mortgage older than 30 years
							ERROR_MISSING_INITIAL_OWNER_NAME = 21, //In the search order form, the middle name is missing
							ERROR_MISSING_INITIAL_COOWNER_NAME = 22, //but was found by automatic.
							ERROR_MISSING_INITIAL_BUYER_NAME = 23,
							ERROR_MISSING_INITIAL_COBUYER_NAME= 24,
							ERROR_SITE_WITH_NAME_SEARCH_SKIPPED = 25,//for common company names
							ERROR_POOR_DATA = 26,
							ERROR_TRANSFER_NOT_NEWEST = 27,
							STARTER_MESSAGE = 28;

// useful chapter data	
	private int errorIndex;
	private String grantor;
	private String grantee;
	private String instrumentType;
	private String instrumentNo;
	private String errorMsg;
	private int chapterNo;
/////////////////////////////////	
	
	TSDIndexError(int index, String msg)
	{
		setErrorIndex(index);
		setErrorMsg(msg);
	}
	/**
	 * @return
	 */
	public int getErrorIndex()
	{
		return errorIndex;
	}

	/**
	 * @return
	 */
	public String getGrantee()
	{
		return grantee;
	}

	/**
	 * @return
	 */
	public String getGrantor()
	{
		return grantor;
	}

	/**
	 * @return
	 */
	public String getInstrumentTypr()
	{
		return instrumentType;
	}

	/**
	 * @param i
	 */
	public void setErrorIndex(int i)
	{
		errorIndex = i;
	}

	/**
	 * @param string
	 */
	public void setGrantee(String string)
	{
		grantee = string;
	}

	/**
	 * @param string
	 */
	public void setGrantor(String string)
	{
		grantor = string;
	}

	/**
	 * @param string
	 */
	public void setInstrumentType(String string)
	{
		instrumentType = string;
	}

	/**
	 * @return
	 */
	public String getErrorMsg()
	{
		return errorMsg;
	}

	/**
	 * @param string
	 */
	public void setErrorMsg(String string)
	{
		errorMsg = string;
	}

	/**
	 * @return
	 */
	public int getChapterNo()
	{
		return chapterNo;
	}

	/**
	 * @param i
	 */
	public void setChapterNo(int i)
	{
		chapterNo = i;
	}

	/**
	 * @return
	 */
	public String getInstrumentNo()
	{
		return instrumentNo;
	}

	/**
	 * @param i
	 */
	public void setInstrumentNo(String i)
	{
		instrumentNo = i;
	}

}
