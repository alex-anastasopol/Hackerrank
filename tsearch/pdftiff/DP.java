package ro.cst.tsearch.pdftiff;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.SearchLogger;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.stewart.ats.base.document.ImageI;

public class DP 
{
	private static final Logger logger = Logger.getLogger(DP.class);
	
	private String fileName, outputFolder;
	
	private int index = 0;
	
	public static final String GS_DEVICE_G4 = "tiffg4";
	public static final String GS_DEVICE_GRAY = "tiffgray";
	
	public static final String IMAGE_TYPE_JPEG = "jpeg";
	public static final String IMAGE_TYPE_BMP = "bmp";
	public static final String IMAGE_TYPE_GIF = "gif";
	public static final String IMAGE_TYPE_PNG = "png";
	public static final String IMAGE_TYPE_TIFF = "tiff";
	
	public static final int PAGE_FORMAT_UNKNOWN = 0;	
	public static final int PAGE_FORMAT_LEGAL = 1;
	public static final int PAGE_FORMAT_LETTER = 2;
	
	private Map<String,String> htmldocProperties = new HashMap<String, String>(); 
	
	public static final String[] formatNames = 
		{
			"UNKNOWN", 
			"LEGAL", 
			"LETTER"
		};
	public static final Dimension[] formatSizes = 
		{
			null,
			new Dimension(2550, 4200),
			new Dimension(2550, 3300)
		};
	
	public static double PLAT_INCH_MIN_SIZE = 14.5;
	
	private int outputPageFormat = PAGE_FORMAT_LETTER;
	
	private String documentName = null;
	
	private Vector<DPPage> pages = new Vector<DPPage>();
	private Vector<String> pdfs = new Vector<String>();
	
	private ImageI.IType outputType = ImageI.IType.TIFF;
	private String outputFileName = null;
	
	public DP( String fileName, String outputFolder)
	{		
		setFileName( fileName );
		setOutputFolder( outputFolder );		
	}
	
	public DP( String fileName, String outputFolder, String documentName)
	{		
		setFileName( fileName );
		setOutputFolder( outputFolder );
		
		setDocumentName(documentName);
	}
	
	
	/**
	 * moves the document pointer to the specified position. new pages will be added at the new position
	 *  
	 * @param pos - position to seek to
	 */
	public void seek( int pos )
	{
		index = pos;
	}
	
	private void storePage( DPPage page )
	{			
		if ( pages.size() < index + 1)
			pages.setSize(index + 1);				
		
		pages.set(index, page );
		
		index += 1;
	}
	
	
	/**
	 * adds a html page to the document
	 * @param fileName html file to be appended
	 * @return DPPage instance of the appended page
	 */
	public DPPage appendHTML( String fileName, long searchId ){ 
		return appendHTML(fileName, false, false, searchId);
	}

	/**
	 * adds a html page to the document
	 * @param fileName html file to be appended
	 * @return DPPage instance of the appended page
	 */
	public DPPage appendHTML( String fileName, boolean tax, boolean keepPdf, long searchId )
	{		
		DPPage p = null;
        
        try
        {
    		String pdfTemp = Util.convertToPDF(this, fileName);
    		
    		p = appendPDF( pdfTemp, -1, tax );
    		if(keepPdf) {
    			pdfs.add(pdfTemp);
    		}else {
    			new File(pdfTemp).delete();
    		}
        }
        catch( Exception e )
        {
            p = new DPImagePage( this, 0, DP.IMAGE_TYPE_TIFF, 
                    BaseServlet.REAL_PATH + File.separator + "web-resources" + File.separator 
                    + "images" + File.separator + "errorPage.tiff", false, 0, 0 );
            SearchLogger.info("<br/><span class='error'>An error occured while processing page #" + (p.getIndex() + 1) + 
					" from TSR.</span><br/>", searchId);
        }
		return p;

	}	
	
	/**
	 * adds all pages of a pdf file to the document
	 * 
	 * @param pdfPath
	 * @return DPPDFPage instance of added pdf
	 */
	public DPPDFPage appendPDF( String pdfPath )
	{		
		return appendPDF( pdfPath, -1 );
	}

	/**
	 * adds the specified subpage of a pdf file to the document
	 * 
	 * @param pdfPath
	 * @param page subpage to be added
	 * @param tax the document is the tax document. this means the output device is DP.GS_DEVICE_GRAY
	 * @return DPPDFPage instance of added pdf
	 */
	public DPPDFPage appendPDF( String pdfPath, int page, boolean tax )
	{
		DPPDFPage p = new DPPDFPage( this, index, pdfPath, page == -1 ? 0 : page, 0, tax );
		return processAppendPDF(p, page);
	}	
	
	/**
	 * adds the specified subpage of a pdf file to the document
	 * 
	 * @param pdfPath
	 * @param page subpage to be added
	 * @return DPPDFPage instance of added pdf
	 */
	public DPPDFPage appendPDF( String pdfPath, int page )
	{
		DPPDFPage p = new DPPDFPage( this, index, pdfPath, page == -1 ? 0 : page, 0);
		return processAppendPDF(p, page);
	}
	
	private DPPDFPage processAppendPDF(DPPDFPage p, int page){
		storePage(p);
		
		if ( page == -1 )
		{		
			DPPDFPage sp = p;
			while ( (sp = (DPPDFPage) sp.next()) != null )
				storePage(sp);
		}
		
		return p;		
	}
	
	/**
	 * adds image to the document. if image is multipage all subpages will be added
	 * 
	 * @param imageFormat image type
	 * @param imagePath path to the image file
	 * @param plat true if image has to be split
	 * @return DPImagePage instance of added image
	 * <br><br>
	 * image types: DP.IMAGE_TYPE_JPEG, DP.IMAGE_TYPE_BMP, DP.IMAGE_TYPE_GIF, DP.IMAGE_TYPE_PNG, IMAGE_TYPE_TIFF
	 */
	public DPImagePage appendImage(String imageFormat, String imagePath, boolean plat)
	{
		boolean  multiple = DP.IMAGE_TYPE_TIFF.equals(imageFormat );
		
		return appendImage(imageFormat, imagePath, plat, multiple ? -1 : 0);
	}

	
	/**
	 * adds specified subpage of a multipage image to the document
	 * 
	 * @param imageFormat image type
	 * @param imagePath path to the image file
	 * @param plat true if image has to be split
	 * @param page index of multipage document subpage to be added
	 * @return DPImagePage instance of added image
	 * <br><br>
	 * image types: DP.IMAGE_TYPE_JPEG, DP.IMAGE_TYPE_BMP, DP.IMAGE_TYPE_GIF, DP.IMAGE_TYPE_PNG, IMAGE_TYPE_TIFF
	 */	
	public DPImagePage appendImage(String imageFormat, String imagePath, boolean plat, int page)
	{		
		DPImagePage p = new DPImagePage( this, index, imageFormat, imagePath, plat, page == -1 ? 0 : page, 0 );		
		storePage(p);	
		
		if ( page == -1 )
		{		
			DPImagePage sp = p;
			while ( (sp = (DPImagePage) sp.next()) != null )
				storePage(sp);
		}
		
		return p;
	}

	
	/**
	 * @return fileName (no extension) of the output file
	 */
	public String getFileName() {
		return fileName;
	}
	
	
	/**
	 * sets output fileName. output files will be fileName.tif and fileName.pdf<br>
	 * 
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	
	/**
	 * @return folder for output files
	 */
	public String getOutputFolder() {
		return outputFolder;
	}
	
	
	/**
	 * @param outputFolder folder where output files will be created, if folder does not exist it will be created
	 */
	public void setOutputFolder(String outputFolder) 
	{
		try
		{		
			File f = new File(outputFolder);
			f.mkdirs();
			
			this.outputFolder = f.getAbsolutePath();
			
			if ( !this.outputFolder.endsWith(File.separator) )
				this.outputFolder += File.separator;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		//this.outputFolder = outputFolder;
	}
	
	
	/**
	 * @return document name (footer text)
	 */
	public String getDocumentName() {
	    return documentName;
	}


	/**
	 * sets document footer text<br>
	 * @param documentName 
	 */
	public void setDocumentName(String documentName) {
	    this.documentName = documentName;
	}
	
	
	private Document pdfOutput = null;
	
	
	/**
	 * generates output files
	 */
	public String process(long searchId)
	{
		ImageWriter tiffOutput = null;
		ImageOutputStream ios = null;
		String tiffFileNameGenerated = null;
		try
		{
			setOutputFileName(getOutputFolder() + getFileName() + "." + getOutputType().toString().toLowerCase());
			File tiffFile = new File( outputFolder + getFileName() + ".tiff");
			
			tiffFile.delete();		
			Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("tiff");
			tiffOutput = it.next();
			ios = ImageIO.createImageOutputStream( tiffFile ); 			
			tiffOutput.setOutput( ios );
			tiffOutput.prepareWriteSequence(null);
			tiffFileNameGenerated = tiffFile.getAbsolutePath();
			logger.debug(tiffFile.getName());
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
		
		for ( int i = 0; i < pages.size(); i++ )
		{
			DPPage p = (DPPage) pages.get(i);
			if ( p!=null ) {
				
				try {
					
					p.output( pdfOutput, tiffOutput );
					
				} catch (Throwable t) {
					
					try {
						getErrorPage(p).output( pdfOutput, tiffOutput );
						SearchLogger.info("<br/><span class='error'>An error occured while processing page #" + (p.getIndex() + 1) + 
								" from TSR.</span><br/>", searchId);
					} catch (Throwable ignored) {}
				}
			}
		}
				
		try
		{
			tiffOutput.endWriteSequence();
			ios.close();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
		
		if ( pdfOutput != null )
		{
			try
			{
				pdfOutput.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return tiffFileNameGenerated;
	}

	public String concatenatePdfs() {
		setOutputFileName(getOutputFolder() + getFileName() + "." + getOutputType().toString().toLowerCase());
		File pdfFile = new File( outputFolder + getFileName() + ".pdf");
		String pdfFileName= pdfFile.getAbsolutePath();
		Collections.reverse(this.pdfs);
		Util.concatenatePdfs(this.pdfs.toArray(new String[0]),pdfFileName);
		
		return pdfFileName;
	}
	
	public DPPage getErrorPage(DPPage page) {
		
		
		DPPage errorPage = new DPImagePage( this, 0, DP.IMAGE_TYPE_TIFF, 
				BaseServlet.REAL_PATH + File.separator + "web-resources" + File.separator 
				+ "images" + File.separator + "errorPage.tiff", false, 0, 0 );
		
		errorPage.setIndex(page.getIndex());
		
		return errorPage;
	}
	
	/**
	 * @return page format of output document
	 * <br>
	 * can be DP.PAGE_FORMAT_LEGAL or DP.PAGE_FORMAT_LETTER
	 */
	public int getOutputPageFormat() 
	{	    
		return outputPageFormat;
	}
	

	/**
	 * @param outputPageFormat - default page format for this document<br>
	 * 			Pages with unknown format will be resized to this format<br>
	 * 			- can be DP.PAGE_FORMAT_LETTER or DP.PAGE_FORMAT_LEGAL<br>
	 * 			- default is DP.PAGE_FORMAT_LETTER<br>
	 */
	public void setOutputPageFormat(int outputPageFormat) 
	{
		this.outputPageFormat = outputPageFormat;
	}
	
	/**
	 * @return document page count 
	 */
	public int getPageCount()
	{
		return pages.size();
	}

	/**
	 * @param i
	 *  - page index
	 * @return
	 * 	DPPage at index i
	 */
	public DPPage getPage(int i)
	{
		return (DPPage) pages.get(i);
	}

	protected void writeToPDF(BufferedImage image) throws Exception 
	{		
		String tmpTiff = Util.tempFileName(getOutputFolder(), "tiff");
		File of = new File( tmpTiff );
		ImageOutputStream ios = ImageIO.createImageOutputStream(of);		
		ImageIO.write(image, "tiff", ios);
		ios.close();
		
		Rectangle ps = ( image.getHeight() ==  4200 ? PageSize.LEGAL : PageSize.LETTER);	
	
		
		if (pdfOutput == null)
		{
			pdfOutput = new Document(ps,0,0,0,0);				
			PdfWriter.getInstance(pdfOutput, new FileOutputStream(getOutputFolder() + getFileName() + ".pdf"));				
			pdfOutput.open();
		}
		else
		{			
			pdfOutput.setPageSize(ps);
			pdfOutput.newPage();
		}
	
		Image img = Image.getInstance( tmpTiff );
		img.scaleToFit(pdfOutput.getPageSize().getWidth() - 1,pdfOutput.getPageSize().getHeight() - 1);
		pdfOutput.add( img );	
				
		of.delete(); //sterg temporarul
	}

	/**
	 * shifts existing pages by specified amount<br>  
	 * (pages at index i will then be found at position i + val)
	 * @param val amount to shift
	 */
	public void shift(int val) 
	{
		for ( int i = 0; i < pages.size(); i++ )
		{
			DPPage p = (DPPage) pages.get(i);
			p.index += val;
		}
		for ( int i = 0; i < val; i++ )
			pages.add(0, null);
	}
	
	/**
	 * Opposite of shift method
	 * @param val
	 */
	public void shiftBack(int val){
		for (int i = val - 1; i >= 0; i--)
			pages.remove(i);
		for ( int i = 0; i < pages.size(); i++ )
		{
			DPPage p = (DPPage) pages.get(i);
			if(p!= null)
				p.index -= val;
		}
	}

	/**
	 * @return the outputType
	 */
	public ImageI.IType getOutputType() {
		return outputType;
	}

	/**
	 * @param outputType the outputType to set
	 */
	public void setOutputType(ImageI.IType outputType) {
		this.outputType = outputType;
	}

	/**
	 * @return the outputFileName
	 */
	public String getOutputFileName() {
		return outputFileName;
	}

	/**
	 * @param outputFileName the outputFileName to set
	 */
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}
	
	/**
	 * Reads the output file and returns a byte array with it's content
	 * @return the content of the file
	 */
	public byte[] getOutputBytes(){
		try {
			return FileUtils.readFileToByteArray(new File(getOutputFileName()));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Map<String, String> getHtmldocProperties() {
		return htmldocProperties;
	}

	public void setHtmldocProperties(Map<String, String> htmldocProperties) {
		this.htmldocProperties = htmldocProperties;
	}
	
	
}
