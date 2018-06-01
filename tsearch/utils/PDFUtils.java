package ro.cst.tsearch.utils;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.SimpleBookmark;


public class PDFUtils {
	
	public static String extractTextFromPDF(InputStream stream) throws Exception{
		return extractTextFromPDF(stream, false);
	}
	
	 public static String extractTextFromPDF(InputStream stream, boolean sortByPosition) throws Exception{
			
	        Writer output = null;
	        PDDocument document = null;
	        ByteArrayOutputStream bas = new ByteArrayOutputStream();
	        
	        try{
	            document = PDDocument.load(stream);       
	            output = new OutputStreamWriter(bas);
	            
	            PDFTextStripper stripper = new PDFTextStripper();
	            stripper.setSortByPosition(sortByPosition);
	            stripper.writeText(document, output);
	        }
	        finally{
	            if(output != null){
	                output.close();
	            }
	            if(document != null){
	                document.close();
	            }
	        }   
	        
	        return bas.toString();
		}
	 
	 public static String extractTextFromPDF(String pdfFile) throws Exception{
			
	        Writer output = null;
	        PDDocument document = null;
	        ByteArrayOutputStream bas = new ByteArrayOutputStream();
	        
	        try{
	            document = PDDocument.load(pdfFile);       
	            output = new OutputStreamWriter(bas);
	            
	            PDFTextStripper stripper = new PDFTextStripper();
	            stripper.writeText(document, output);
	        }
	        finally{
	            if(output != null){
	                output.close();
	            }
	            if(document != null){
	                document.close();
	            }
	        }   
	        
	        return bas.toString();
		}
	 
	 /**
	  * merge two .pdf files
	  * @param destinationFile - first file and also destination file
	  * @param documentToAdd   - file to be added to the destination file
	  * @param paginate
	  */
	 
	 public static byte[] mergePDFs(byte[] destinationFile, byte[] documentToAdd, boolean paginate) {

		    Document document = new Document();
		    ByteArrayOutputStream bas = new ByteArrayOutputStream();
		    try {
		      
		    	ByteArrayInputStream baisDestination = new ByteArrayInputStream(destinationFile);
		    	
		    	ByteArrayInputStream baisDocToAdd = new ByteArrayInputStream(documentToAdd);
		    	
		    	List<PdfReader> readers = new ArrayList<PdfReader>();
		    	int totalPages = 0;

		      // Create Readers for the pdfs.
		    	
		        PdfReader pdfReaderForFile = new PdfReader(baisDestination);
		        readers.add(pdfReaderForFile);
		        totalPages += pdfReaderForFile.getNumberOfPages();
		        
		        pdfReaderForFile = new PdfReader(baisDocToAdd);
		        readers.add(pdfReaderForFile);
		        totalPages += pdfReaderForFile.getNumberOfPages();

		      // Create a writer for the outputstream
		        PdfWriter writer = PdfWriter.getInstance(document, bas);

		        document.open();
		     // BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		        PdfContentByte cb = writer.getDirectContent(); // Holds the PDF
		      // data

		        PdfImportedPage page;
		        int currentPageNumber = 0;
		        int pageOfCurrentReaderPDF = 0;
		        Iterator<PdfReader> iteratorPDFReader = readers.iterator();

		      // Loop through the PDF files and add to the output.
		        while (iteratorPDFReader.hasNext()) {
		        	PdfReader pdfReader = iteratorPDFReader.next();

		        // Create a new page in the target for each source page.
		        	while (pageOfCurrentReaderPDF < pdfReader.getNumberOfPages()) {
		        		document.newPage();
		        		pageOfCurrentReaderPDF++;
		        		currentPageNumber++;
		        		page = writer.getImportedPage(pdfReader, pageOfCurrentReaderPDF);
		        		cb.addTemplate(page, 0, 0);

		          // Code for pagination.
		        		if (paginate) {
		        			//  cb.beginText();
		        			//  cb.setFontAndSize(bf, 9);
		        			//  cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "" + currentPageNumber + " of " + totalPages, 520, 5, 0);
		        			//  cb.endText();
		        		}
		        	}
		        	pageOfCurrentReaderPDF = 0;
		        }
		        bas.flush();
		        document.close();
		        bas.close();
		    } catch (Exception e) {
		      e.printStackTrace();
		    } finally {
		      if (document.isOpen())
		        document.close();
		      try {
		        if (bas != null)
		        	bas.close();
		      } catch (IOException ioe) {
		        ioe.printStackTrace();
		      }
		    }
		    return bas.toByteArray();
		  }
	 /**
	  * concatenates a list of .pdf files
	  * @param streamOfPDFFiles - files to concatenate
	 * @param outputStream
	  * @throws IOException 
	 * @throws DocumentException 
	  */
	 public static void concatenatePDFs(List<InputStream> streamOfPDFFiles, OutputStream outputStream) throws IOException, DocumentException {

		 int pageOffset = 0;
		 ArrayList master = new ArrayList();
		 int f = 0;
		 Document document = null;
		 PdfCopy writer = null;
		 
		 for (InputStream inputStream : streamOfPDFFiles) {
				
			 PdfReader reader = new PdfReader(inputStream);
			 reader.consolidateNamedDestinations();
			 int n = reader.getNumberOfPages();
			 List bookmarks = SimpleBookmark.getBookmark(reader);
			 if (bookmarks != null) {
				 if (pageOffset != 0){
					 SimpleBookmark.shiftPageNumbers(bookmarks, pageOffset, null);
				 }
				 master.addAll(bookmarks);
			 }
			 pageOffset += n;
			 if (f == 0) {
				 document = new Document(reader.getPageSizeWithRotation(1));
				 writer = new PdfCopy(document, outputStream);
				 document.open();
			 }
			 PdfImportedPage page;
			 for (int i = 0; i < n;) {
				 ++i;
				 page = writer.getImportedPage(reader, i);
				 writer.addPage(page);
			 }
			 PRAcroForm form = reader.getAcroForm();
			 if (form != null){
				 writer.copyAcroForm(reader);
			 }
			 f++;
		 }
		 if (!master.isEmpty())
			 writer.setOutlines(master);
		 document.close();
	 }
	 
	 
		public static String getPDFForLink(String currentServerName, long searchId, String urlAsString, Map<String,String> parameters) {
			// get the PDF
			ByteArrayOutputStream bas = new ByteArrayOutputStream();
			HttpSite site = HttpManager.getSite(currentServerName, searchId);
			
			HTTPResponse pdfPage = null;
			try {
				if (StringUtils.isNotEmpty(urlAsString)) {
					HTTPRequest request = new HTTPRequest(urlAsString);
					pdfPage =  site.process(request);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
			if (pdfPage!= null &&  "application/pdf".equalsIgnoreCase(pdfPage.getContentType())) { 
				
				Writer output = null;
				PDDocument document = null;
				try {
					document = PDDocument.load(pdfPage.getResponseAsStream());
					output = new OutputStreamWriter(bas);

					PDFTextStripper stripper = new PDFTextStripper();
					stripper.getTextMatrix();
					// PDFTextStripperByArea textStripperByArea = new
					// PDFTextStripperByArea();
					org.pdfbox.util.PDFText2HTML textStripperByArea = new org.pdfbox.util.PDFText2HTML();
					// textStripperByArea.extractRegions()
//					stripper.setWordSeparator("     ");

//					stripper.setLineSeparator("\n");
//					stripper.setPageSeparator("\n");

					// do not change this. will blow up the output of the pdf and
					// the
					// parsing of him will fail
					stripper.setSortByPosition(true);
					// PDFText2HTML stripper = new PDFText2HTML();
					stripper.writeText(document, output);
					
				} catch (IOException e) {
					e.printStackTrace();
					;
				} finally {
					if (output != null) {
						try {
							output.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (document != null) {
						try {
							document.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			return bas.toString();
		}
	
	/**
	 * Converts a given byte array to a PDF file and returns the content of the generated file as byte array
	 * @param htmlContent
	 * @return content of the generated PDF file as byte array
	 */
	public static byte[] convertToPDFBytes(byte[] htmlContent) {
		return (byte[])convertToPDFInternal(htmlContent, Util.MAX_CONVERSION_RETRY - 1, new HashMap<String,String>(), true);
	}
	/**
	 * Converts a given bytes array to a PDF file and returns the name of the generated file
	 * @param htmlContent
	 * @return name of the generated PDF file
	 */
	public static String convertToPDFFile(byte[] htmlContent) {
		return (String)convertToPDFInternal(htmlContent, Util.MAX_CONVERSION_RETRY - 1, new HashMap<String,String>(), false);
	}
	
	public static Object convertToPDFInternal( byte[] htmlContent, int execCounter , Map<String,String> properties, boolean bytesNotFile){
        
        ClientProcessExecutor cpe = null;
        String inputHtml = null;
        File inputFile = null;
        File outputFile = null;
		try
		{
			
			boolean links = properties.containsKey("--links");
			
			long timeStamp = System.currentTimeMillis();
			long randomLong = RandomGenerator.getInstance().getLong();
			
			inputHtml = ZipUtils.getTempzipfolder() + "zipDocument_" + randomLong + "_" + timeStamp + ".html";
			
			inputFile = new File(inputHtml);
			
			String htmlContentAsString = new String(htmlContent);
			
			htmlContentAsString = htmlContentAsString.replaceAll("<textarea ", "<div ").replaceAll("textarea>", "div>");
			
			FileUtils.writeStringToFile(inputFile, htmlContentAsString);
			
			//htmldoc doesn't support UTF-8
			//we need to replace some characters with html entities
			Util.utf8Fix(inputHtml);
			String tempPDF = Util.tempFileName(ZipUtils.getTempzipfolder(), "pdf");
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
					inputFile.getAbsolutePath()		
				};
		
    		StringBuffer c = new StringBuffer();
    		for (int i = 0; i < exec.length; i++)
    			c.append(" " + exec[i]);
    		
    		
    
            cpe = new ClientProcessExecutor( exec, true, true);
            
            cpe.start();
        
			//int k = cpe.getReturnValue();
        
            if(bytesNotFile) {
            
	            outputFile = new File(tempPDF);
	            byte[] readFileToByteArray = FileUtils.readFileToByteArray(outputFile);
	            
	            if(readFileToByteArray.length == 0) {
	            	return null;
	            }
	            
				return readFileToByteArray;
            } else {
            	return tempPDF;
            }
		}
		catch (Exception e)
		{
            if( execCounter == Util.MAX_CONVERSION_RETRY - 1 )
            {
                //send log email with the stacktrace and the output of the process
                String email = "Out String: ";
				if (cpe != null) {
					email += cpe.getCommandOutput() + " \n\n\n\n\n";
				} else {
					email += "Null output stream reader thread! \n\n\n\n\n";
				}

				email += "Err String: ";
				if (cpe != null) {
					email += cpe.getErrorOutput() + " \n\n\n\n\n";
				} else {
					email += "Null error stream reader thread! \n\n\n\n\n";
				}
                
                email += "Stack Trace: " + e.getMessage() + " \n\n " + ServerResponseException.getExceptionStackTrace( e ).replaceAll( "<BR>\n", "" );
                
                Log.sendEmail( MailConfig.getExceptionEmail(), "Error converting html to pdf on " + URLMaping.INSTANCE_DIR + " - File - " + inputHtml, email );
                
                //print stack trace only if this is the last retry
                e.printStackTrace();
            }
			
		} finally {
			if(inputFile != null && inputFile.exists()) {
				inputFile.delete();
			}
			if(outputFile != null && outputFile.exists()) {
				outputFile.delete();
			}
		}
		
		return null;
	}
	
	public static String resizePdf(String initialFileName, double minRatio, Rectangle pagesize, boolean checkAllPages){
    	try {
    		if( initialFileName==null || !new File(initialFileName).exists()) {
    			return initialFileName;
    		}
			PdfReader reader = new PdfReader(initialFileName);
			int numOfPages = reader.getNumberOfPages();
			
			if(numOfPages > 0) {
				
				if(checkAllPages) {
					Document document = new Document(pagesize, 0,0,0,0);
					String extension = ro.cst.tsearch.utils.FileUtils.getFileExtension(initialFileName);
			        String tempFileName = initialFileName.replace(extension, "_" + System.currentTimeMillis() + extension);
					
					PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(tempFileName));
					document.open();
		
					PdfContentByte cb = writer.getDirectContent();
					
					for (int i = 1; i <= numOfPages; i++) {
						// put the page
						 PdfImportedPage page = writer.getImportedPage(reader,i);
						 double sx = pagesize.getWidth() / reader.getPageSize(i).getWidth();
						 double sy = pagesize.getHeight() / reader.getPageSize(i).getHeight();
						 cb.transform(AffineTransform.getScaleInstance(sx, sy)) ;
						 cb.addTemplate(page, 1, 0, 0, 1, 0, 0);
						 document.newPage();
					}
					
					document.close();
					if(new File(tempFileName).exists()) {
						return tempFileName;
					}
					
					
				} else {
				
					if(reader.getPageSize(1).getWidth()/pagesize.getWidth() >= minRatio ||
							reader.getPageSize(1).getHeight()/pagesize.getHeight() >= minRatio) {

						Document document = new Document(pagesize, 0,0,0,0);
						String extension = ro.cst.tsearch.utils.FileUtils.getFileExtension(initialFileName);
				        String tempFileName = initialFileName.replace(extension, "_" + System.currentTimeMillis() + extension);
						
						PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(tempFileName));
						document.open();
			
						PdfContentByte cb = writer.getDirectContent();
						
						for (int i = 1; i <= numOfPages; i++) {
							// put the page
							 PdfImportedPage page = writer.getImportedPage(reader,i);
							 double sx = pagesize.getWidth() / reader.getPageSize(i).getWidth();
							 double sy = pagesize.getHeight() / reader.getPageSize(i).getHeight();
							 cb.transform(AffineTransform.getScaleInstance(sx, sy)) ;
							 cb.addTemplate(page, 1, 0, 0, 1, 0, 0);
							 document.newPage();
						}
						document.close();
						if(new File(tempFileName).exists())
							return tempFileName;
					}
				
				}
			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return initialFileName;
    }
}
