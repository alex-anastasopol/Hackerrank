package ro.cst.tsearch.pdftiff;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.stream.FileImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.utils.TiffConcatenator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class DPImagePage extends DPPage 
{	
	private static final Logger logger = Logger.getLogger(DPImagePage.class);
	
	protected DPImagePage()
	{		
	}
	
	protected DPImagePage(DP dp, int index, 
			String imageFormat, String fileName, boolean plat, 
			int page, int platIndex)
	{
		super(dp, index, fileName);
		
		this.plat = plat;
		this.imageFormat = imageFormat;
		this.idxImage = page;

		try
		{
			readInfo();
			
			if (isPlat())
			{
				this.platIndex = platIndex + 1;
			}
		}
		catch (Exception e)
		{
			
		}
	}
	
	protected Object next()
	{			
		if ( next == null)
		{
			if (isPlat() && platIndex == 1)
				next = new DPImagePage(dp, index+1, imageFormat, fileName, plat, idxImage, 1);
			else		
			if (idxImage == numImages - 1)
				next = null;
			else
				next = new DPImagePage(dp, index+1, imageFormat, fileName, plat, idxImage + 1, 0);
					
		}
		
		return next;
	}
		
	
	
	public void readInfo()
	{
		if ( !infoLoaded )
		{		
			
			try
			{
				Iterator readers = ImageIO.getImageReadersByFormatName(imageFormat);
				ImageReader reader = null;
		        while(readers.hasNext()) 
		        {
		            reader = (ImageReader)readers.next();		 
		            //logger.info( reader.getClass().getName() );
		            if(!reader.getClass().getName().startsWith("com.sun.imageio")) 
		                break;
		        }
		        
		        if ( reader == null )
		        	return;
		        

		        
		        
		        //logger.info("am ales " + reader.getClass().getName());
		        
		        FileImageInputStream fiis = new FileImageInputStream(new File(fileName));
		        
		        reader.setInput( fiis );
		        
		        try {
		        	numImages = reader.getNumImages(true);
		        } catch(IIOException iioException) {
		        	logger.error("Error while trying to getNumImages for: " + fileName + ". Using default value 1", 
		        			iioException);
		        	
		        }
		        		        
		        width = reader.getWidth(idxImage);
		        height = reader.getHeight(idxImage);		       
		        
		        readMetadata(reader.getImageMetadata(idxImage));

		        fiis.close();
				reader.dispose();
								
				if (!isPlat())
					getPageFormat();
				
				infoLoaded = true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}		
	}
	
	protected void getPageFormat()
	{
		double ratio1 = (double)getWidth()/(double)getHeight();
		if ( ratio1 > 1 )
			ratio1 = (double)getHeight()/(double)getWidth();				
		
		if ( Util.aproxEqual(ratio1, 8.5/11, 0.05) )
			pageFormat = DP.PAGE_FORMAT_LETTER;
		else
		if ( Util.aproxEqual(ratio1, 8.5/14, 0.05) )
			pageFormat = DP.PAGE_FORMAT_LEGAL;					
	}
	
	public void readMetadata(IIOMetadata md)
	{
	    try {
			Node n = md.getAsTree( IIOMetadataFormatImpl.standardMetadataFormatName);	
			
			//Util.showMetadata(n);
			
			n = n.getFirstChild();
			
			
			
	        while (n != null) {
	        	/*if (n.getNodeName().equals("Chroma"))
	        	{
	        		Node n2 = n.getFirstChild();
	        		while (n2 != null) 
	        		{
	                    if (n2.getNodeName().equals("BlackIsZero")) 
	                    {
	                    	NamedNodeMap nnm = n2.getAttributes();
	                        Node n3 = nnm.item(0);
	                        
	                        if ("FALSE".equalsIgnoreCase(n3.getNodeValue()))
	                        	isWhiteOnBlack = true;
	                    }
	                    n2 = n2.getNextSibling();
	        		}
	        	}
	        	else*/
	            if (n.getNodeName().equals("Dimension")) {
	
	                Node n2 = n.getFirstChild();
	
	                while (n2 != null) {
	
	                    if (n2.getNodeName().equals("HorizontalPixelSize")) {
	
	                        NamedNodeMap nnm = n2.getAttributes();
	
	                        Node n3 = nnm.item(0);
	
	                        float hps = Float.parseFloat(n3.getNodeValue());
	
	                        xDPI = Math.round(25.4f / hps);
	
	                    }
	
	                    if (n2.getNodeName().equals("VerticalPixelSize")) {
	
	                        org.w3c.dom.NamedNodeMap nnm = n2.getAttributes();
	
	                        org.w3c.dom.Node n3 = nnm.item(0);
	
	                        float vps = Float.parseFloat(n3.getNodeValue());
	
	                        yDPI = Math.round(25.4f / vps);
	
	                    }
	
	                    n2 = n2.getNextSibling();
	
	                }
	
	            }
	
	            n = n.getNextSibling();
	
	        }
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public void readData() throws Exception
	{
		Iterator readers = ImageIO.getImageReadersByFormatName(imageFormat);
		ImageReader reader = null;
        while(readers.hasNext()) 
        {
            reader = (ImageReader)readers.next();		 	         
            if(!reader.getClass().getName().startsWith("com.sun.imageio")) 
                break;
        }	        
        if ( reader == null )
        	return;
        FileImageInputStream fiis = new FileImageInputStream(new File(fileName));	        
        reader.setInput( fiis );
        image = reader.read(idxImage);
        fiis.close();
		reader.dispose();		
	}
	
	public static void main(String[] args) {
		
		try {
		
			
			File staticMap = new File("D:\\bugs\\temp\\staticmap1.jpg");
			
			List<byte[]> inputPngs = new ArrayList<byte[]>();
			inputPngs.add(FileUtils.readFileToByteArray(staticMap));
			byte[] concatePngInTiff = TiffConcatenator.concatePngInTiff(inputPngs);
			
			File staticMapConverted = new File("D:\\bugs\\temp\\staticmap1.tiff");
			
			FileUtils.writeByteArrayToFile(staticMapConverted, concatePngInTiff);
			
			try {
				new DPImagePage(null, 0, "tiff", staticMapConverted.getAbsolutePath(), true, 0, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			
			
			/*
//			byte[] readFileToByteArray = FileUtils.readFileToByteArray(new File("D://myimage.tiff"));
			
//			byte[] cleanedImage = TiffConcatenator.cleanImage(readFileToByteArray);
			
//			FileUtils.writeByteArrayToFile(new File("D://myimage_corrected.tiff"), cleanedImage);
			
			DPImagePage p = null;
			
			try {
				p = new DPImagePage(null, 0, "tiff", "D:\\work\\ATSFolder\\images\\2013_03_14\\4279085\\saved.tiff", true, 0, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				p = new DPImagePage( null, 0, "tiff", "D://myimage_corrected.tiff", true, 0 , 0 );
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
			
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
		
}
