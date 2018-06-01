package ro.cst.tsearch.pdftiff.util;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.IOUtil;
import ro.cst.tsearch.pdftiff.DP;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.replication.tsr.FileInfo;
import ro.cst.tsearch.templates.OfficeDocumentContents;
import ro.cst.tsearch.threads.ProcessStreamReader;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.SimpleBookmark;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.tsrindex.server.InterruptUploadException;
import com.stewart.ats.tsrindex.server.UploadImage;
import com.sun.star.connection.NoConnectException;

public class Util 
{
	private static final Logger logger = Logger.getLogger(Util.class);
	
    public static int MAX_CONVERSION_RETRY = 3;
    
    public static final String A4_SIZE = "a4size";
    
	/**
	 * 
	 * @param dp
	 * @param htmlFile  
	 * @return number of pages the htmlFile will fill if added to the dp document 
	 */
	public static int evalPageCount(DP dp, String htmlFile)
	{
		try
		{		
			String tempPDF = convertToPDF( dp, htmlFile );
			
			
			
			PdfReader pdfReader = new PdfReader(tempPDF);
			
			int pages = pdfReader.getNumberOfPages();
			
			new File(tempPDF).delete();
			
			return pages;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}
	
    public static String convertToPDF( DP dp, String htmlFile )
    {
        //tries MAX_CONVERSION_RETRY times to convert to pdf
        int counter = 0;
        String returnValue = null;
        
        while( counter < MAX_CONVERSION_RETRY ){
            try
            {
                returnValue = convertToPDF( dp, htmlFile, counter );
            }
            catch( Exception e )
            { }
            
            if( returnValue == null )
            {
                counter ++;
            }
            else
            {
                break;
            }
        }
        
        if( returnValue == null ){
            //still error...
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String fileContents = "";
            try
            {
                IOUtil.copy( new FileInputStream( htmlFile ), baos );
                
                fileContents = baos.toString( "UTF-8" );
            }
            catch( Exception e ) 
            {
                Log.sendExceptionViaEmail( MailConfig.getExceptionEmail(), "Error converting html to pdf!!! - " + htmlFile, e );
            }
            
            Log.sendEmail( MailConfig.getExceptionEmail(), "Error converting html to pdf!!! - " + htmlFile, fileContents );
        }
        
        return returnValue;
    }
    
    public static String convertPDFToTIFF(String pdfFileName, String pdfPassword, String tiffFileName){
    	String ret = tiffFileName;
		final ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		String gsCommand = rbc.getString("GS.CommandString").trim();
		try{
			//convert pdf to tiff
			String[] exec ={
				gsCommand,
				"-sPDFPassword=" + pdfPassword,
				"-sDEVICE=tiffg4",
				"-r300x300",
				"-dNOPAUSE",
				"-sOutputFile=" + tiffFileName,
				pdfFileName,
				"-dBATCH"
			};

            ClientProcessExecutor cpe = new ClientProcessExecutor( exec, true, true);
            cpe.start();
			//int k = cpe.getReturnValue();
		}
		catch ( Exception e ){
			e.printStackTrace();
		}
		return ret;
	}
    
    @SuppressWarnings("unchecked")
	public static void concatenatePdfs(String args[],String destFile) {
        if (args.length == 1) {
            FileUtils.copy(args[0], destFile);
        }
        else {
            try {
                int pageOffset = 0;
                ArrayList master = new ArrayList();
                int f = 0;
                Document document = null;
                PdfCopy  writer = null;
                while (f < args.length) {
                    // we create a reader for a certain document
                    PdfReader reader = new PdfReader(args[f]);
                    reader.consolidateNamedDestinations();
                    // we retrieve the total number of pages
                    int n = reader.getNumberOfPages();
                    List bookmarks = SimpleBookmark.getBookmark(reader);
                    if (bookmarks != null) {
                        if (pageOffset != 0)
                            SimpleBookmark.shiftPageNumbers(bookmarks, pageOffset, null);
                        master.addAll(bookmarks);
                    }
                    pageOffset += n;
                    
                    if (f == 0) {
                        // step 1: creation of a document-object
                        document = new Document(reader.getPageSizeWithRotation(1));
                        // step 2: we create a writer that listens to the document
                        writer = new PdfCopy(document, new FileOutputStream(destFile));
                        // step 3: we open the document
                        document.open();
                    }
                    // step 4: we add content
                    PdfImportedPage page;
                    for (int i = 0; i < n; ) {
                        ++i;
                        page = writer.getImportedPage(reader, i);
                        writer.addPage(page);
                    }
                    PRAcroForm form = reader.getAcroForm();
                    if (form != null)
                        writer.copyAcroForm(reader);
                    f++;
                }
                if (!master.isEmpty())
                    writer.setOutlines(master);
                // step 5: we close the document
                document.close();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static String resizePdfToA4(String initialFileName, double minRatio){
    	try {
    		if( initialFileName==null || !new File(initialFileName).exists()) {
    			return initialFileName;
    		}
			PdfReader reader = new PdfReader(initialFileName);
			int numOfPages = reader.getNumberOfPages();
			
			if(numOfPages > 0) {
				if(reader.getPageSize(1).getWidth()/PageSize.A4.getWidth() >= minRatio ||
						reader.getPageSize(1).getHeight()/PageSize.A4.getHeight() >= minRatio) {
					Rectangle rectangle = PageSize.A4;
					Document document = new Document(rectangle, 0,0,0,0);
					String extension = FileUtils.getFileExtension(initialFileName);
			        String tempFileName = initialFileName.replace(extension, "_" + A4_SIZE + extension);
					
					PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(tempFileName));
					document.open();
		
					PdfContentByte cb = writer.getDirectContent();
					
					for (int i = 1; i <= numOfPages; i++) {
						// put the page
						 PdfImportedPage page = writer.getImportedPage(reader,i);
						 double sx = rectangle.getWidth() / reader.getPageSize(i).getWidth();
						 double sy = rectangle.getHeight() / reader.getPageSize(i).getHeight();
						 cb.transform(AffineTransform.getScaleInstance(sx, sy)) ;
						 cb.addTemplate(page, 1, 0, 0, 1, 0, 0);
						 document.newPage();
					}
					document.close();
					if(new File(tempFileName).exists())
						return tempFileName;
				}
			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return initialFileName;
    }
    
    public static void utf8Fix(String fileName){
    	String content = StringUtils.fileReadToString(fileName);
    	if (content != null){
    		content = StringUtils.selectiveHTMLEntityEncode(content, new char[]{(char)176,(char)8217,(char)8221});
    		try {
    			content = content.replaceAll("&#8217;","'").replaceAll("&#8221;","\"");
    		}catch(Exception e) {}
    		FileUtils.writeTextFile(fileName, content);
    	}
    }
    
    public static String convertDocToPDF( String outFolder, String docFile) {
    			
    	try {    		
    	  	File f = new File(docFile);
    		String fileName = f.getName();
    		String pdfFileName = fileName.substring(0,fileName.lastIndexOf("."))+".pdf";

    		OfficeDocumentContents odc = new OfficeDocumentContents(f.getCanonicalPath());
    		try {
    			odc.saveToFile(outFolder + pdfFileName);
    		}catch(Exception e) { 
    			e.printStackTrace();
    		}
    		
    		odc.closeOO();
    		return outFolder + pdfFileName;
    		
    	} catch(NoConnectException nce) {
        	throw new RuntimeException("Cannot process the word document. Please try again later");
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return "";
    }
    
    public static String convertToPDF( String outFolder, String htmlFile, int execCounter, String fileName ){
        
        //stream readers for the created process
        ProcessStreamReader errStreamReader = null;
        ProcessStreamReader outStreamReader = null;
        
		try
		{		
			//htmldoc doesn't support UTF-8
			//we need to replace some characters with html entities
			utf8Fix(htmlFile);
			String namePDF = outFolder + fileName + ".pdf";
			String[] exec = {
					ServerConfig.getHtmlDocCommand(),
					"-f", namePDF,
					"--size", "Letter",
					"--top", "1cm",
					"--bottom", "1cm",
					"--left", "1cm",
					"--right", "1cm",
					"--fontsize", "8",
					"--compression=9",
					"--footer", "...",
					"--header", "...",
					"--charset", "UTF-8",
					"--no-strict",
					"--no-links",
					"--webpage",
					new File(htmlFile).getAbsolutePath()					
				};
		
    		StringBuffer c = new StringBuffer();
    		for (int i = 0; i < exec.length; i++)
    			c.append(" " + exec[i]);
    		
    		
    		logger.debug(c.toString());
    
            ClientProcessExecutor cpe = new ClientProcessExecutor( exec, true, true);
            
            cpe.start();
        
			//int k = cpe.getReturnValue();
        
			return namePDF;
		}
		catch (Exception e)
		{
            if( execCounter == MAX_CONVERSION_RETRY - 1 )
            {
                //send log email with the stacktrace and the output of the process
                String email = "Out String: ";
                
                if( outStreamReader != null )
                {
                    email += outStreamReader.getOutput() + " \n\n\n\n\n";
                }
                else
                {
                    email += "Null output stream reader thread! \n\n\n\n\n";
                }
                
                email += "Err String: ";
                
                if( errStreamReader != null )
                {
                    email += errStreamReader.getOutput() + " \n\n\n\n\n";
                }
                else
                {
                    email += "Null error stream reader thread! \n\n\n\n\n";
                }                
                
                email += "Stack Trace: " + e.getMessage() + " \n\n " + ServerResponseException.getExceptionStackTrace( e ).replaceAll( "<BR>\n", "" );
                
                Log.sendEmail( MailConfig.getExceptionEmail(), "Error converting html to pdf - Stacktrace!!! - " + htmlFile, email );
                
                //print stack trace only if this is the last retry
                e.printStackTrace();
            }
			return null;
		}
	}
    
    public static String convertToPDF( String outFolder, String htmlFile, int execCounter ){
    	return convertToPDF(outFolder,htmlFile,execCounter, new HashMap<String,String>());
    }
    
    public static String convertToPDF( String outFolder, String htmlFile, int execCounter , Map<String,String> properties){
        
        ClientProcessExecutor cpe = null;
		try
		{
			
			boolean links = properties.containsKey("--links");
			
			//htmldoc doesn't support UTF-8
			//we need to replace some characters with html entities
			utf8Fix(htmlFile);
			String tempPDF = Util.tempFileName(outFolder, "pdf");
			String[] exec = {
					ServerConfig.getHtmlDocCommand(),
					"-f", tempPDF,
					"--size", "Letter",
					"--top", "1cm",
					"--bottom", "1cm",
					"--left", "1cm",
					"--right", "1cm",
					"--fontsize", "8",
					"--compression=9",
					"--footer", "...",
					"--header", "...",
					"--charset", "UTF-8",
					"--no-strict",
					links?"--links":"--no-links",
					"--webpage",
					new File(htmlFile).getAbsolutePath()					
				};
		
    		StringBuffer c = new StringBuffer();
    		for (int i = 0; i < exec.length; i++)
    			c.append(" " + exec[i]);
    		
    		
    		logger.debug(c.toString());
    
            cpe = new ClientProcessExecutor( exec, true, true);
            
            cpe.start();
        
			//int k = cpe.getReturnValue();
        
			return tempPDF;
		}
		catch (Exception e)
		{
            if( execCounter == MAX_CONVERSION_RETRY - 1 )
            {
                //send log email with the stacktrace and the output of the process
                String email = "Out String: ";
                
                if( cpe != null )
                {
                    email += cpe.getCommandOutput() + " \n\n\n\n\n";
                }
                else
                {
                    email += "Null output stream reader thread! \n\n\n\n\n";
                }
                
                email += "Err String: ";
                
                if( cpe != null )
                {
                    email += cpe.getErrorOutput() + " \n\n\n\n\n";
                }
                else
                {
                    email += "Null error stream reader thread! \n\n\n\n\n";
                }                
                
                email += "Stack Trace: " + e.getMessage() + " \n\n " + ServerResponseException.getExceptionStackTrace( e ).replaceAll( "<BR>\n", "" );
                
                Log.sendEmail( MailConfig.getExceptionEmail(), "Error converting html to pdf - Stacktrace!!! - " + htmlFile, email );
                
                //print stack trace only if this is the last retry
                e.printStackTrace();
            }
			return null;
		}
	}
    
	/**
	 * converts htmlFile to pdf using input/output settings of document dp<br>
	 * @param dp
	 * @param htmlFile
	 * @return temporary path of the generated pdf document
	 */
	public static String convertToPDF( DP dp, String htmlFile, int execCounter )
	{
		return convertToPDF( dp.getOutputFolder(), htmlFile, execCounter, dp.getHtmldocProperties() );
	}
	
	
	/**
	 * @param folder
	 * @param extension
	 * @return random 10 digits filename created in folder having desired extensin
	 */
	public static String tempFileName(String folder, String extension)
	{
		int digits = 10;
		
		while (true)
		{
			StringBuffer result = new StringBuffer();
	       
			result.append(folder);
			
			for (int i = 0; i < digits; i++) 
	        {
	            result.append((char)('a'+random.nextInt(26)));
	        }
	        
	        result.append(".");
	        result.append(extension);
	        
	        //	        
	        String filePath = result.toString();
	        if ( new File(filePath).exists() )
	        	continue;
	        else
	        	return filePath;
		}        
    }
	
	/**
	 * @param folder
	 * @param extension
	 * @return random 
	 */
	public static String randFileName(String folder, String extension){
		int digits = 10;
		
		while (true)
		{
			StringBuffer result = new StringBuffer();
	       
			result.append(folder);
			
			for (int i = 0; i < digits; i++) 
	        {
	            result.append((char)('a'+random.nextInt(26)));
	        }
	        
			int number = random.nextInt();
			String numStr = "";
			if(number<0){
				numStr = 0+String.valueOf(Math.abs(number));
			}
			else{
				numStr = 1+String.valueOf(Math.abs(number));
			}
			result.append(numStr);
	        result.append(".");
	        result.append(extension);
	                
	        String filePath = result.toString();
	        if ( new File(filePath).exists() )
	        	continue;
	        else
	        	return filePath;
		}        
    }
	
    private static Random random = new Random(System.currentTimeMillis());
	
	
	public static boolean aproxEqual(double a, double b, double err)
	{
		return ( Math.abs(a-b) < err );
	}
	
	/**
	 * Used to return an image name that contains only the first firstPages and the last lastPages from a tif file
	 * @param imageName the original file name
	 * @param firstPages the number of first pages
	 * @param lastPages the number of last pages
	 * @return FileInfo containing file name and file pages as size
	 */
	public static FileInfo getPartOfTheImage(String imageName, int firstPages, int lastPages){
		String resultImageName = imageName;
		int pageNum = 0;
		String motherF = "";
		int numImages = -1;
		if(imageName.endsWith(".tif") || imageName.endsWith(".tiff")){
			try {
				Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("tif");
				ImageReader reader = null;
		        while(readers.hasNext()) 
		        {
		            reader = (ImageReader)readers.next();
		            if(!reader.getClass().getName().startsWith("com.sun.imageio")) {
		                break;
		            }
		        }
		        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tif");
				ImageWriter imageWriter = null;
		        while(writers.hasNext()) 
		        {
		        	imageWriter = (ImageWriter)writers.next();
		            motherF += imageWriter.getClass().getName() + "\n";
		            if(!imageWriter.getClass().getName().startsWith("com.sun.imageio")) {
		                break;
		            }
		            
		        }
		        
		        FileImageInputStream fiis = new FileImageInputStream(
		        		new File(imageName));
		        reader.setInput(fiis);
		        numImages = reader.getNumImages(true);
		        if( numImages <= firstPages ){
		        	resultImageName = imageName;
		        	pageNum = -1;
		        } else {
		        	if(imageWriter == null)
		        		imageWriter = ImageIO.getImageWriter(reader);
			        if(imageWriter == null)
			        	System.err.println("MAJOR PROBLEM, MF");
			        String extension = FileUtils.getFileExtension(imageName);
			        String tempResultImageName = imageName.replace(extension, "_part" + extension);
			        File toWriteTo = new File(tempResultImageName);
			        if(toWriteTo.exists())
			        	toWriteTo.delete();
			        FileImageOutputStream fios = new FileImageOutputStream(toWriteTo);
			        imageWriter.setOutput(fios);
			        imageWriter.prepareWriteSequence(null);
			        for(int i = 0; i< firstPages; i++){
			        	imageWriter.writeToSequence( reader.readAll(i, null), null);
			        	pageNum++;
			        }
			        if(firstPages >= numImages - lastPages) {
			        	for(int i = firstPages; i< numImages; i++){
			        		imageWriter.writeToSequence( reader.readAll(i, null), null);
			        		pageNum++;
				        }	
			        } else {
			        	for(int i = numImages - lastPages; i< numImages; i++){
			        		imageWriter.writeToSequence( reader.readAll(i, null), null);
			        		pageNum++;
				        }
			        }
			        imageWriter.endWriteSequence();
			        imageWriter.dispose();
			        if(toWriteTo.exists()) {
			        	resultImageName = tempResultImageName;			        	
			        } else {
			        	pageNum = -1;
			        }
		        }
			} catch (Exception e) {
				e.printStackTrace();
				pageNum = -1;
				resultImageName = imageName;
				
				String classPath = System.getProperty ("java.class.path");
				String[] cp = classPath.split(";");
				
				
				EmailClient email = new EmailClient();
				try {
				     System.err.println(com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
				}
				catch(Exception ex) { ex.printStackTrace(); }
				
				email.addTo(MailConfig.getExceptionEmail());
				email.setSubject("getPartOfTheImage problem on " + URLMaping.INSTANCE_DIR);
				String content = "General Exception in ro.cst.tsearch.pdftiff.util.Util.getPartOfTheImage(" + 
					imageName + ", " + 
					firstPages + ", " + 
					lastPages + ") \n\n Stack Trace: " + e.getMessage() + " \n\n " + 
					ServerResponseException.getExceptionStackTrace( e, "\n" ) + "\n";
				for (int i = 0; i < cp.length; i++) {
					content += i + ": " + cp[i] + "\n";
				}
				try {
					content += com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader.class.getProtectionDomain().getCodeSource().getLocation().toURI() + "\n";
				} catch (Exception ex) {
					// TODO: handle exception
				}
				content += motherF;
				email.addContent(content);
			
				email.sendAsynchronous();
			}
		}
		return new FileInfo(numImages, resultImageName, pageNum, "");
	}
	
	public static void uploadImageToDocument(DocumentI doc, Search mSearch,long searchId) throws IOException, InterruptUploadException {
		String taxIndex = DBManager.getDocumentIndex(doc.getIndexId());
		
		if(taxIndex == null) return ;
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		Date sdate =mSearch.getStartDate();
		    		
		String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate)+File.separator+searchId;
		File file= new File(basePath);
		if(!file.exists()){
			file.mkdirs();
		}
			    		
		String docId = doc.getId();
		String tiffFileName = docId+".tiff";
		String htmlFileName = docId+".htm";
		String fullHtmlFileName = basePath+File.separator+htmlFileName;
//
		File f = new File(fullHtmlFileName);
		f.createNewFile(); 
		FileUtils.writeTextFile(fullHtmlFileName, taxIndex);
		UploadImage.createTempTIFF(fullHtmlFileName, basePath, null);
		f.delete();
		
		
		String path 	= basePath+File.separator+tiffFileName;
		UploadImage.updateImage(doc, path, tiffFileName, "tiff", searchId);
		
		Set<String> links = new HashSet<String>();
		links.add(path);
		//
		doc.getImage().setLinks( links );
		doc.setIncludeImage(true);
	}
	
	public static void main(String[] args) {
		/*
		FileInfo s = Util.getPartOfTheImage(args[0], 3, 0);
		String classPath = System.getProperty ("java.class.path");
		String[] cp = classPath.split("[;]");
		for (int i = 0; i < cp.length; i++) {
			//System.err.println(i + ": " + cp[i]);
		}
		try {
		     System.err.println(com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch(Exception e) { e.printStackTrace(); }
		System.err.println();
		System.err.println(s.name);
		*/
		utf8Fix("d:\\temp\\titleDocument.html");
		
		
	}
}
