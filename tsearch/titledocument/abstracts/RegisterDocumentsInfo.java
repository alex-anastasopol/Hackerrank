package ro.cst.tsearch.titledocument.abstracts;
import java.util.Vector;
import org.apache.log4j.Category;

@Deprecated
public class RegisterDocumentsInfo
{
	private static final Category logger= Category.getInstance(RegisterDocumentsInfo.class.getName());
	public final static int 
				DOC_PATH= 0, 
				DOC_TYPE= 1, 
				DOC_BOOK= 2, 
				DOC_PAGE= 3, 
				DOC_INSTRUMENT_NO= 4, 
				DOC_DATE= 5, 
				DOC_GRANTOR= 6, 
				DOC_NAME= 7, 
				DOC_GRANTEE= 8,
				DOC_REFERENCE=9, 
				DOC_SERVER_TYPE= 10, 
				DOC_INSTRUMENT_DATE=11,
				DOC_GRANTEE_TR=12,
				DOC_GRANTEE_LANDER=13,
				DOC_MORTGAGE_AMOUNT=14,
				DOC_DOCUMENT_NUMBER=15,
				DOC_TIME=16,
				DOC_TYPE_ABBREV=17,
				DOC_TAXYEAR=18,
				DOC_ADDRESS=19,
				DOC_LEGAL_DESC=20,
				DOC_TRANSFERS=21,
				DOC_SUBTYPE=22,
				DOC_SRCTYPE=23,
				DOC_FIELDS_COUNT=24;
	
	public Vector mRecordedTitleDocuments;
	public Vector mUnsatisfiedDeeds;
	public Vector mConvenants;
	public Vector mAditionalMatters;
	private long searchId=-1;
	public RegisterDocumentsInfo(long searchId){
		this.searchId = searchId;
		mRecordedTitleDocuments= new Vector(5);
		mUnsatisfiedDeeds= new Vector(5);
		mConvenants= new Vector(5);
		mAditionalMatters= new Vector(5);
	}
	
}
