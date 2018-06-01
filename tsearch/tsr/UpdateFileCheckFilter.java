package ro.cst.tsearch.tsr;

import java.io.File;
import java.io.FilenameFilter;

public class UpdateFileCheckFilter implements FilenameFilter {

    public static final String PDF_SUFFIX  = ".PDF";
    public static final String TIF_SUFFIX  = ".TIF";
    public static final String XML_SUFFIX  = ".XML";
    public static final String TSU_PREFIX  = "TSU";
    public static final int    PDF_EXT_LEN = 4;
    public static final int    TSR_TSU_LEN = 3;

    private String fileNo;
    private String fileSuffix;
    

    public UpdateFileCheckFilter(String fileNo, String suffix) {
        this.fileNo = fileNo.toUpperCase();
        this.fileSuffix = suffix.toUpperCase();
    }

	public boolean accept(File dir, String name)
	{
	    String upperName     = name.toUpperCase();
	    String remainderName = ((name.length() > TSR_TSU_LEN) ? name.substring(TSR_TSU_LEN) : "");
	    remainderName = remainderName.toUpperCase();
	    //get pdf files starting in "TSR-<fileNo>_" pr "TSU-<fileNo>_"
		return  (
		            upperName.endsWith( fileSuffix )
		        &&
	                remainderName.startsWith( fileNo )
		        );
	}
}
