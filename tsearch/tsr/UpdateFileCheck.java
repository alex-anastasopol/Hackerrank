package ro.cst.tsearch.tsr;

import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.ConvertToPdf;
import ro.cst.tsearch.utils.FormatDate;

import org.apache.log4j.Category;

/**
 * This class will check all subfolders of "folder"
 * and it will create two lists of files:
 *  - first  list: files that look like: "TSR-<fileNo>_<DateTimeStamp>.pdf"
 *  - second list: files that look like: "TSU-<fileNo>_<DateTimeStamp>_<UpdateNo>.pdf"
 * Also, the following information will be available through this class:
 *  - number of files in each list
 *  - most recent date/timestamp available in the union of the two list.
 * The date/timestamp has the format: "_MMddyyyy_hhmmss".
 * The "folder" is actually the users folder, containing subfolders
 * for each user (subfolder name is application username), and the
 * name of the users folder is read from the properties.
 *
 * The purpose of this class is to identify whether or not <fileNo>
 * exists to be uploaded, what is the upload number and the most
 * recent date/timestamp to update the pdf file from.
**/

public class UpdateFileCheck {

	protected static final Category logger= Category.getInstance(UpdateFileCheck.class.getName());
	
    private String   folder;
    private String   fileNo;
    private String   fileSuffix;
    private int      fileNoLength;
    private int      tsrNo;
    private int      tsuNo;
    private Calendar mostRecentDateTime;
    public String    fileFoundPath;
    

    public UpdateFileCheck(String folder, String fileNo, String suffix) {
        this.folder        = folder;
        this.fileNo        = "-" + fileNo + "_";
        this.fileSuffix    = suffix;
        fileNoLength       = this.fileNo.length();
        mostRecentDateTime = null;
        tsrNo              = 0;
        tsuNo              = 0;
    }

    public void check() throws BaseException {
        File fFolder = null;
        //check a for valid folder
        try {
            fFolder = new File(folder);
        } catch (NullPointerException npe) {
            throw new BaseException("Users PDF folder can't be null!");
        }
        if ( fFolder == null || !fFolder.exists() || !fFolder.isDirectory() )
            //throw new BaseException("Users PDF folder does not exist! Please check properties!");
            return;
        //get all subfolders of folder
        File[] fSubfolders = fFolder.listFiles();
        File[] fPDFFiles;
        UpdateFileCheckFilter ufcf = new UpdateFileCheckFilter( fileNo, fileSuffix );
        String pdfFileName, dateTimeStamp;
        Calendar currentDateTime;
        Date currentDate;
        int underscorePosition;
        for (int i=0; i<fSubfolders.length; i++) {
            if ( fSubfolders[i].exists()                &&
                 fSubfolders[i].isDirectory()           &&
                 !fSubfolders[i].getName().equals(".")  &&
                 !fSubfolders[i].getName().equals("..") ) {
                logger.info(">> (" + fSubfolders[i].getPath() + ") (" + fSubfolders[i].getName() + ")");
                fPDFFiles = fSubfolders[i].listFiles(ufcf);
                for (int j=0; j<fPDFFiles.length; j++) {
                    //file name, in upper case
                    pdfFileName = fPDFFiles[j].getName().toUpperCase();
                    //extract date/timestamp
                    dateTimeStamp = pdfFileName.substring(UpdateFileCheckFilter.TSR_TSU_LEN + fileNoLength);
                    //remove extension
                    dateTimeStamp = dateTimeStamp.substring(0, dateTimeStamp.length() - UpdateFileCheckFilter.PDF_EXT_LEN);
                    underscorePosition = dateTimeStamp.indexOf("_");
                    if (underscorePosition >= 0) {
                        underscorePosition = dateTimeStamp.indexOf("_", underscorePosition + 1);
                        if (underscorePosition >= 0) {
                            //it is a TSU that has the <UpdateNo> after the timestamp,
                            //so we remove the <UpdateNo>
                            dateTimeStamp = dateTimeStamp.substring(0, underscorePosition);
                        }
                    }
                    logger.info("\t>> (" + fPDFFiles[j].getPath() + ") (" + fPDFFiles[j].getName() + ") (" + dateTimeStamp + ")");
                    currentDate = null;
                    try {
                        currentDate = ConvertToPdf.sdf.parse(dateTimeStamp);
                    } catch (ParseException pe) {
                        currentDate = null;
                    }
                    if (currentDate != null) {
                        currentDateTime = Calendar.getInstance();
                        currentDateTime.setTime(currentDate);
                        if ( mostRecentDateTime == null                ||
                             currentDateTime.after(mostRecentDateTime) )
                        {
                            mostRecentDateTime = currentDateTime;
                            fileFoundPath = fPDFFiles[j].getPath();
                        }
                        //count TSR and TSU files
                        if ( !pdfFileName.startsWith( UpdateFileCheckFilter.TSU_PREFIX )){
                            tsrNo++;
                    	}
                        else{
                            tsuNo++;
                        }
                    }
                }
            }
        }
    }

  
    public Calendar getMostRecentDateTime() {
        return mostRecentDateTime;
    }

    public int getTsrNo() {
        return tsrNo;
    }

    public int getTsuNo() {
        return tsuNo;
    }

/*
    public static void main(String args[]) {
        String sFolder = args[0];
        String sFileNo = args[1];
        UpdateFileCheck ufc = new UpdateFileCheck(sFolder, sFileNo);
        try {
            ufc.check();
            Calendar _mostRecentDateTime = ufc.getMostRecentDateTime();
            int _tsrNo = ufc.getTsrNo();
            int _tsuNo = ufc.getTsuNo();
            //FormatDate fd = new FormatDate(FormatDate.DISC_FORMAT_1_DATE);
            FormatDate fd = new FormatDate("MMM d, yyyy");
            logger.info("\n>>>>>>>>>> (" + _mostRecentDateTime + ") (" + fd.getDate(_mostRecentDateTime) + ") (" + _tsrNo + ") (" + _tsuNo + ")");
        } catch (Exception e) {
            logger.error(e.getMessage()+"\n");
            e.printStackTrace();
        }
    }
*/
}
