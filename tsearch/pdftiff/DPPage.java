package ro.cst.tsearch.pdftiff;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TransposeDescriptor;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.pdftiff.util.Chronometer;

import com.lowagie.text.Document;
import com.stewart.ats.base.document.ImageI;
import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageMetadata;


public abstract class DPPage 
{	
	private static final Logger logger = Logger.getLogger(DPPage.class);
	
	protected int index, /*type,*/ pageFormat = DP.PAGE_FORMAT_UNKNOWN;
	protected String fileName;
	
	protected boolean infoLoaded = false;
	
	protected DP dp = null;
	protected DPPage next = null;
	
	protected int marginLeft = 100;
	protected int marginRight = 100;
	protected int marginTop = 100;
	protected int marginBottom = 0;
	
	protected int footerMargin = 90;
	protected int footerFontSize = 50;
	
	protected boolean plat = false;
	//
	
	/*
	letter 8.5"/11"
	legal 8.5"/14"
	*/
		
	protected BufferedImage image = null;
	
	protected int numImages = 1;
	protected int idxImage = 0;
	int xDPI = 0, yDPI = 0;
	protected int platIndex = 0;
	protected boolean isWhiteOnBlack = false;
	protected String gsOutputDevice = DP.GS_DEVICE_G4;
	protected String imageFormat;	
	protected String documentName = null;
	
	protected DPPage()
	{
	}
	
	protected DPPage(DP dp, int index, String fileName)
	{
		this.dp = dp;
		this.index = index;
		this.fileName = fileName;
	}	
	
	public int getIndex()
	{
		return index;
	}
	
	public void setIndex(int index)
	{
		this.index = index;
	}
		
	/**
	 * @return source fileName of page
	 */
	public String getFileName() {
		return fileName;
	}
	
	/*public int getType() {
		return type;
	}*/
	
	public boolean isPortrait() 
	{
		return getWidth() <= getHeight();
	}	
	
	int width = -1, height = -1;
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public int getFormat()
	{		
		return pageFormat;					
	}
	
	protected abstract void readInfo() throws Exception;
	protected abstract void readData() throws Exception;
	
	
	protected abstract Object next();
	
	public void invert()
	{
		DPPage p = this;
		
		while (p!=null)
		{
			p.isWhiteOnBlack = !p.isWhiteOnBlack;
			p = p.next;
		}
	}
	
	protected void process_convertToBW() throws IOException
	{
        if ( image.getType() != BufferedImage.TYPE_BYTE_BINARY ) // convertesc la alb-negru daca e cazul
        {        
        	BufferedImage aux = 
        		new BufferedImage( image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        	aux.createGraphics().drawImage(image, 0, 0, null);
        	image = aux;
        	        	
        }
	}
	
	
	
	protected boolean isPlat()
	{
		/*
		if ( xDPI == 0 || yDPI == 0)
			return false;
		
		double inchX = getWidth()/xDPI;
		double inchY = getHeight()/yDPI;
				
		return ( inchX > DP.PLAT_INCH_MIN_SIZE || inchY > DP.PLAT_INCH_MIN_SIZE );*/
		
		return plat;
	}
	

	protected void process_plat()
	{		
		if (!isPlat())
			return;
		
		int pWidth = getWidth();
		int pHeight = getHeight();
		
		
		boolean pPortrait = (getHeight() > getWidth());
		
		
		if ( pPortrait )
		{
		    int ts = getHeight()/2;
		    
			BufferedImage image2 = 
	    		new BufferedImage( 
	    				pWidth, 
						ts, 
						BufferedImage.TYPE_BYTE_BINARY );
	    	
			Graphics2D g = image2.createGraphics();
			
			g.drawImage(image,0,platIndex==1? 0 : -ts,null);
			
			image = image2;
			
			width = pWidth;
			height = ts;
			
		}
		else
		{
		    int ts = getWidth()/2;
		    		    
			BufferedImage image2 = 
	    		new BufferedImage( 
	    				ts, 
	    				pHeight, 
						BufferedImage.TYPE_BYTE_BINARY );
	    	
			Graphics2D g = image2.createGraphics();
			
			g.drawImage(image,platIndex==1? 0 : -ts, 0,null);
			
			image = image2;
			
			width = ts;
			height = pHeight;
		}
		
		
		/*int ts = (int)((double)Math.min(getWidth(),getHeight()) * DP.formatSizes[DP.PAGE_FORMAT_LETTER].getWidth()/DP.formatSizes[DP.PAGE_FORMAT_LETTER].getHeight()); 
		
		if (pPortrait)
		{
			if ( platIndex == 1)
			{
				BufferedImage image2 = 
		    		new BufferedImage( 
		    				pWidth, 
							ts, 
							BufferedImage.TYPE_BYTE_BINARY );
		    	
				Graphics2D g = image2.createGraphics();
				
				g.drawImage(image,0,0,null);
				
				image = image2;
				
				width = pHeight;
				height = ts;
			}
			else
			if ( platIndex == 2)
			{
				BufferedImage image2 = 
		    		new BufferedImage( 
		    				pWidth, 
							pHeight - ts, 
							BufferedImage.TYPE_BYTE_BINARY );
		    	
				Graphics2D g = image2.createGraphics();
				
				g.drawImage(image,0,-ts,null);
				
				image = image2;
				
				width = pWidth;
				height = pHeight - ts;
			}
		}
		else
		{
			if ( platIndex == 1)
			{
				BufferedImage image2 = 
		    		new BufferedImage( 
		    				ts, 
							pHeight, 
							BufferedImage.TYPE_BYTE_BINARY );
		    	
				Graphics2D g = image2.createGraphics();
				
				g.drawImage(image,0,0,null);
				
				image = image2;
				
				width = ts;
				height = pHeight;
			}
			else
			if ( platIndex == 2)
			{
				BufferedImage image2 = 
		    		new BufferedImage( 
		    				pWidth - ts, 
							pHeight, 
							BufferedImage.TYPE_BYTE_BINARY );
		    	
				Graphics2D g = image2.createGraphics();
				
				g.drawImage(image,-ts,0,null);
				
				image = image2;
				
				width = pWidth - ts;
				height = pHeight;
			}
		}*/
	}

	
	protected void process_rotateAndScale()
	{        	
		// portret
    	if ( !isPortrait() && !isPlat() )
    	{    	    
    		RenderedOp aux = JAI.create("transpose", image, TransposeDescriptor.ROTATE_270);
        	image = aux.getAsBufferedImage();        	
    	}

		int pageWidth = 
			DP.formatSizes[dp.getOutputPageFormat()].width;
		
		int pageHeight = 
			DP.formatSizes[dp.getOutputPageFormat()].height;				
		float pageRatio = (float) pageWidth/pageHeight;
    	
		double scaleX = 1.0, scaleY = 1.0;
		
    	/*if (!isPlat())
    	{*/
    		int currentWidth = image.getWidth();
			int currentHeight = image.getHeight();		
    		int newWidth = pageWidth - marginLeft - marginRight;
    		int newHeight = (int)((float)newWidth/pageRatio);
    		scaleX = (float)newWidth/currentWidth;
    		scaleY = (float)newHeight/currentHeight;
    	/*}
    	else
    	{	
			
			if ( getFormat() != DP.PAGE_FORMAT_UNKNOWN )
				if ( getFormat() != dp.getOutputPageFormat() )
				{
					pageWidth = DP.formatSizes[getFormat()].width;
					pageHeight = DP.formatSizes[getFormat()].height;				
				}
			
			
			
			int desiredWidth = pageWidth - marginLeft - marginRight;
			int desiredHeight = (int)((float)desiredWidth/pageRatio);
				
			int currentWidth = image.getWidth();
			int currentHeight = image.getHeight();		
			
			float currentRatio = (float) currentWidth/currentHeight;
					
			int newWidth = desiredWidth;
			int newHeight = desiredHeight;
			
			if ( pageRatio <= currentRatio )
			{
				newWidth = desiredWidth;
				newHeight = Math.round((float)desiredWidth / currentRatio);
			}
			else if ( pageRatio > currentRatio )
			{
				newHeight = desiredHeight;
				newWidth = Math.round((float)desiredHeight * currentRatio);			
			}
			
			if ( newWidth != currentWidth || newHeight != currentHeight )
			{	
				scaleX = (float)newWidth/currentWidth;
				scaleY = scaleX;
			}
    	}*/
		
    	AffineTransform tr = new AffineTransform();

    	tr.setToIdentity();     	
		
    	if ( scaleX != 1.0 || scaleY != 1.0 )
    		tr.scale( scaleX, scaleY );
    	
    	
    	    	
    	if ( !tr.isIdentity() )
    	{    	    		    	
    		RenderedOp aux = JAI.create("affine", image, tr, null);
    	   	image = aux.getAsBufferedImage();   
    	   	aux.dispose();
    	}    	
    	
    	
    	    	
 		/*newWidth = desiredWidth + MARGIN_LEFT+ MARGIN_RIGHT;
 		newHeight = desiredHeight + MARGIN_TOP + MARGIN_BOTTOM;*/
    	BufferedImage image2 = 
    		new BufferedImage( 
    				pageWidth, 
					pageHeight, 
					BufferedImage.TYPE_BYTE_BINARY );
    	
		Graphics2D g = image2.createGraphics();
		
    	g.setColor( new Color(255,255,255 ) );
		
    	g.fillRect( 0, 0, pageWidth, marginTop);
		
    	marginBottom = pageHeight - image.getHeight() - marginTop;
    	g.fillRect( 0, pageHeight - marginBottom, pageWidth, marginBottom);
    	
    	g.fillRect( 0, marginTop, marginLeft, pageHeight - marginTop - marginBottom);
    	g.fillRect( marginLeft + image.getWidth(), marginTop, 
    				pageWidth - marginLeft - image.getWidth(), 
					pageHeight - marginTop - marginBottom);
    	
    	if ( isWhiteOnBlack )
    	{
    		//g.setComposite(new Composite());
    		g.setXORMode(new Color(255,255,255));
    	}
		g.drawImage(image, marginLeft, marginTop, null);
		
		
		
		image = image2;

	}	
	
	protected void process_writeFooter()
	{
		Graphics2D g = image.createGraphics();

		g.setColor( new Color(0,0,0) );			
		Font f = new Font("arial", Font.PLAIN, footerFontSize);
		g.setFont( f );	
						
		String page = (getIndex() + 1) + "/" + dp.getPageCount();
				
		Rectangle2D s = f.getStringBounds( page, g.getFontRenderContext());
		
		g.drawString( page, image.getWidth() - marginRight - (int)s.getWidth(),  
				image.getHeight() -  footerMargin );
		
		
		if ( dp.getDocumentName() != null )
		{
			s = f.getStringBounds( dp.getDocumentName(), g.getFontRenderContext());
			
			g.drawString( dp.getDocumentName(), 
					marginLeft + (image.getWidth() - marginRight - marginLeft - (int)s.getWidth())/2,  
					image.getHeight() -  footerMargin );
		}
	}
		
	protected void cleanUp()
	{
		
	}
	
	protected void output( Document pdfOutput, ImageWriter tiffOutput ) throws Throwable
	{
		readData();				        
		
		Chronometer cc = new Chronometer();
		
		logger.info( "Generez " + toString());
		
		//Chronometer bw = new Chronometer();
		process_convertToBW(); //albnegru
        //logger.info(" bw " + bw.stop().get());
        
		process_plat();
		
        //Chronometer rotate = new Chronometer();
        process_rotateAndScale(); //rotesc
        //logger.info(" rotate and scale " + rotate.stop().get());
        
        
        //Chronometer footer = new Chronometer();
        process_writeFooter(); // gata, pot sa scriu footerul
        //logger.info(" footer " + footer.stop().get());
        
        
        
        //Chronometer save = new Chronometer();
        
        TIFFImageWriteParam writeParam =
			(TIFFImageWriteParam) tiffOutput.getDefaultWriteParam();
		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		writeParam.setCompressionType("CCITT T.6");		//writeParam.setCompressionType("Deflate");
		
		ImageTypeSpecifier its = new ImageTypeSpecifier(image);
		IIOMetadata md = tiffOutput.getDefaultImageMetadata(its, writeParam);
					
		Element root = (Element)md.getAsTree(TIFFImageMetadata.nativeMetadataFormatName);			
		NodeList fields = ((Element)root.getElementsByTagName("TIFFIFD").item(0)).getChildNodes();
		for ( int i = 0; i < fields.getLength(); i++ )
		{
			Element pi = (Element) fields.item(i);
			if ( "ResolutionUnit".equals( pi.getAttribute("name") ) )
			{
				NodeList shorts = pi.getElementsByTagName("TIFFShorts").item(0).getChildNodes();
				for ( int j = 0; j < shorts.getLength(); j++ )
				{
					Element s = (Element)shorts.item(j);
					s.setAttribute("value", "2");
					s.setAttribute("description", "Inch");
				}			
			}
			else
			if ( "XResolution".equals( pi.getAttribute("name") ) || 
					"YResolution".equals( pi.getAttribute("name") ))
			{	
				NodeList rats = pi.getElementsByTagName("TIFFRationals").item(0).getChildNodes();
				for ( int j = 0; j < rats.getLength(); j++ )
				{
					Element rat = (Element)rats.item(j);
					rat.setAttribute("value", "300/1");
				}			
			}
		}			
		md.setFromTree(TIFFImageMetadata.nativeMetadataFormatName, root);
		
					
		IIOImage iioImage = new IIOImage( image, null, md);							
		tiffOutput.writeToSequence(iioImage, writeParam);						

		//logger.info(" save " + save.stop().get());  			

		if(dp.getOutputType().equals(ImageI.IType.PDF))
			dp.writeToPDF(image);
							
		cc.stop();
		
		logger.info(  " - " + cc.get());
		
		image = null;
		
		cleanUp();
	}
	
	public int getNumImages() {
		return numImages;
	}
	
	public String toString()
	{
		return "[" + this.getClass().getName() + "] " + getFileName() + " (" + getWidth() + "x" + getHeight() + ") " +
				 DP.formatNames[getFormat()] + "-" + (isPortrait()?"PORTRAIT":"LANDSCAPE"); 
	}
	/**
	 * @return page name
	 */
	public String getDocumentName() {
		return documentName;
	}
	/**
	 * sets page number<br>
	 * @param documentName
	 */
	public void setDocumentName(String documentName) 
	{		
		DPPage p = this;
		while ( p != null )
		{
			p.documentName = documentName;
			p = (DPPage) p.next();
		}
	}
	
	/**
	 * @return true if the page is first image of a multipage source file (tiff/pdf) or first half of a plat document 
	 */
	public boolean isMain() 
	{
		return idxImage == 0 && platIndex < 2;
	}
}
