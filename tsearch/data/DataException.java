package ro.cst.tsearch.data;


/**
 *   
 */
public class DataException extends Exception {

    private static final String DEFAULT_MESSAGE = "" ;

    public DataException() { 
    	super(DEFAULT_MESSAGE);
    }

    public DataException(String msg) {
    	super(msg);
    }
}   
