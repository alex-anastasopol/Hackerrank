package ro.cst.tsearch.titledocument.abstracts;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Category;


@Deprecated
/**
 * Keep this class only for deserialization 
 */
public class ChapterUtils implements Serializable, Cloneable {

	public static  final String PATRIOTS_ACT_GRANTEE = "Patriots ACT";
	
    static final long serialVersionUID = 10000000;

    @SuppressWarnings("unchecked")
	public HashMap registerChapters = new HashMap();
    
    @SuppressWarnings("unchecked")
	public Vector chapters = new Vector();

    public static final Category logger = Category.getInstance(ChapterUtils.class.getName());
    
    public static final int CHAPTER_NAME = 0, CHAPTER_LINK = 1,
            CHAPTER_CHECKBOX_VALUE = 2, CHAPTER_FILLED = 3,
            CHAPTER_GRANTOR = 4, CHAPTER_GRANTEE = 5, CHAPTER_INSTTYPE = 6,
            CHAPTER_INSTNO = 7, CHAPTER_BOOK = 8, CHAPTER_PAGE = 9,
            CHAPTER_REMARKS = 10, CHAPTER_CHECKBOX_CHECKED = 11,
            CHAPTER_REFERENCES = 12, CHAPTER_IS_TRANSFER = 13,
            CHAPTER_AMOUNTPAID = 14, CHAPTER_BASEAMOUNT = 15,
            CHAPTER_DELIQUENTAMOUNT = 16, CHAPTER_TAXYEAR = 17,
            CHAPTER_SERVER_INSTTYPE = 18;

   
    public static final int MORTGAGE_AS_BOOK_PAGE = 0;

    public static final int MORTGAGE_AS_VECTOR_GRANTEE = 1;

    public static final int MORTGAGE_AS_VECTOR_GRANTOR = 2;
    
    public static class BookAndPage{
    	private String book = null;
    	private String page = null;
    }
    
    public static int getSubstringOccurance(String input, String searchedString) {
        String REGEX = "\\b" + searchedString + "\\b";
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(input); // get a matcher object
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }
    public static final String CHAPS_BP_MODE = "BP";
	public static final String CHAPS_INSTNO_MODE = "INSTNO";
    
	
}