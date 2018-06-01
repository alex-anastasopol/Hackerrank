package ro.cst.tsearch.titledocument;
import ro.cst.tsearch.Search;

public class TSDManager
{
	public static final String TSDDir= "TSD";
	
	public static String GetPdfFile(Search search)
	{
		return search.getSearchTempDir()+ "TSD.pdf";
	}
	
}
