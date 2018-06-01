/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search;


import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.strategy.OneStateIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;
import ro.cst.tsearch.search.strategy.TwoStatesIterator;
import ro.cst.tsearch.search.tokenlist.iterator.LastWordInitialTokenListIterator;
import ro.cst.tsearch.search.tokenlist.iterator.LotIntervalTokenListIterator;
import ro.cst.tsearch.search.tokenlist.iterator.LotTokenListIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 */
public class FunctionStatesIterator extends StrategyBasedIterator implements StatesIterator{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Category logger= Logger.getLogger(FunctionStatesIterator.class);
	
	public static final int ITERATOR_TYPE_DEFAULT = -1;
	public static final int ITERATOR_TYPE_DEFAULT_NOT_EMPTY  = 1;
	public static final int ITERATOR_TYPE_DEFAULT_TWO_STATES = 2;
	public static final int ITERATOR_TYPE_DEFAULT_EMPTY = 3;

	public static final int ITERATOR_TYPE_LAST_WORD_INITIAL = 10;
	public static final int ITERATOR_TYPE_LOT = 11;


	public static final int ITERATOR_TYPE_LAST_NAME_FAKE = 20;
	public static final int ITERATOR_TYPE_FIRST_NAME_FAKE = 21;

	public static final int ITERATOR_TYPE_ST_NAME_FAKE = 22;
	public static final int ITERATOR_TYPE_ST_N0_FAKE= 23;


	public static final int ITERATOR_TYPE_BOOK_FAKE = 24;
	public static final int ITERATOR_TYPE_PAGE_FAKE = 25;

	public static final int ITERATOR_TYPE_INSTRUMENT_LIST_FAKE	= 26;
	public static final int ITERATOR_TYPE_PARCELID_FAKE	= 27;
	
	public static final int ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH = 28;
	public static final int ITERATOR_TYPE_BOOK_SEARCH = 29;
	public static final int ITERATOR_TYPE_PAGE_SEARCH = 30;
	
	public static final int ITERATOR_TYPE_BP_TYPE	= 31;
	
	public static final int ITERATOR_TYPE_PARCEL_MAP 	= 32;
	public static final int ITERATOR_TYPE_PARCEL_SMAP 	= 33;
	public static final int ITERATOR_TYPE_PARCEL_CMAP 	= 34;
	public static final int ITERATOR_TYPE_PARCEL_PN 	= 35;
	public static final int ITERATOR_TYPE_PARCEL_GR 	= 36;
	public static final int ITERATOR_TYPE_PARCEL_DS 	= 37;
	public static final int ITERATOR_TYPE_PARCEL_SI 	= 38;
	public static final int ITERATOR_TYPE_PARCEL_PTYPE 	= 39;
    
    public static final int ITERATOR_TYPE_RUTHERFORD_CLASS = 40;
    
    public static final int ITERATOR_TYPE_MISSOURI_SBDIV_NAME = 41;
    public static final int ITERATOR_TYPE_COMPANY_NAME = 42;

    public static final int ITERATOR_TYPE_LF_NAME_FAKE = 43;
    
    public static final int ITERATOR_TYPE_YEAR = 44;
    public static final int ITERATOR_TYPE_SEQNO = 45;
    
    public static final int ITERATOR_TYPE_DOCTYPE_SEARCH = 46;
    
    public static final int ITERATOR_TYPE_LCF_NAME_FAKE = 47;
    
    public static final int ITERATOR_TYPE_LFM_NAME_FAKE = 48;
    
    public static final int ITERATOR_TYPE_MIDDLE_NAME_FAKE = 49;
    
    public static final int ITERATOR_TYPE_ADDRESS = 50;
    
    public static final int ITERATOR_TYPE_LOT_INTERVAL = 51;
    
    public static final int ITERATOR_TYPE_SCORE = 52;
    
    public static final int ITERATOR_TYPE_SSN = 53;
    
    public static final int ITERATOR_TYPE_NAME_TYPE = 54;
    
    public static final int ITERATOR_TYPE_BLOCK = 55;
    
    public static final int ITERATOR_TYPE_SKLD_INSTRUMENT_FIRST_PART = 56;
    public static final int ITERATOR_TYPE_SKLD_INSTRUMENT_SECOND_PART = 57;
    
    public static final int ITERATOR_TYPE_ARB = 58;
    public static final int ITERATOR_TYPE_TOWNSHIP = 59;
    public static final int ITERATOR_TYPE_RANGE = 60;
    public static final int ITERATOR_TYPE_SECTION = 61;
    public static final int ITERATOR_TYPE_SUFFIX = 62;
    
    public static final int ITERATOR_TYPE_DOCNO_LIST_FAKE = 63;
    
    public static final int ITERATOR_TYPE_OE_RESULT_TYPE = 64;
    /**
     * Should be used on ROlike sites in relation to FROM_DATE key (or related)<br>
     * Necessary for Name Search in relation to UPDATE product<br>
     * Check B6345 for more information
     */
    public static final int ITERATOR_TYPE_FROM_DATE = 65;
    
    public static final int ITERATOR_TYPE_MULTIPLE_YEAR = 66;
    
    public static final int ITERATOR_TYPE_GENERIC_67 = 67;
    public static final int ITERATOR_TYPE_GENERIC_68 = 68;
    public static final int ITERATOR_TYPE_GENERIC_69 = 69;
    public static final int ITERATOR_TYPE_GENERIC_70 = 70;
    public static final int ITERATOR_TYPE_GENERIC_71 = 71;
    public static final int ITERATOR_TYPE_GENERIC_72 = 72;
    public static final int ITERATOR_TYPE_GENERIC_73 = 73;
    public static final int ITERATOR_TYPE_GENERIC_74 = 74;
    public static final int ITERATOR_TYPE_GENERIC_75 = 75;
    public static final int ITERATOR_TYPE_GENERIC_76 = 76;
    
    public static final int ITERATOR_TYPE_BOOK_PAGE_AS_INSTRUMENT_FAKE = 77;
    
    public static final int ITERATOR_TYPE_SERVER_DOCTYPE_SEARCH = 78;
    
    public static final int ITERATOR_TYPE_FML_NAME_FAKE = 79;
    
    
	private TSServerInfoFunction initialState;
	
	public FunctionStatesIterator (TSServerInfoFunction initial){
		this.initialState = new TSServerInfoFunction(initial);
		
		int type = initialState.getIteratorType();
		String str =  initialState.getParamValue();
		
		if  (type == ITERATOR_TYPE_LAST_WORD_INITIAL){
			setStrategy(new LastWordInitialTokenListIterator(str));
		}else if (type == ITERATOR_TYPE_LOT){
			setStrategy(new LotTokenListIterator(str));
		} else if ( type == ITERATOR_TYPE_LOT_INTERVAL ) {
			setStrategy(new LotIntervalTokenListIterator(str));
		} else if (type == ITERATOR_TYPE_DEFAULT_TWO_STATES){
			if (StringUtils.isStringBlank(str)){
				setStrategy( new OneStateIterator(str));
			}else{
				setStrategy( new TwoStatesIterator(str,""));
			}
		}else if (type == ITERATOR_TYPE_DEFAULT_NOT_EMPTY){
			if (StringUtils.isStringBlank(str)){
				setStrategy( new DefaultStatesIterator());
			}else{
				setStrategy( new OneStateIterator(str));
			}
		}else if (type == ITERATOR_TYPE_DEFAULT_EMPTY){
			setStrategy( new OneStateIterator(""));
		}else{//DEFAULT iterator
			setStrategy( new OneStateIterator(str));
		}
	}

	public Object current() {
		TSServerInfoFunction crtState = new TSServerInfoFunction(initialState);
		crtState.setParamValue(((String)super.current()));
		return crtState;
	}
	
	public String toString(){
		return " FunctionStatesIterator = " + initialState ; 
	}

}
