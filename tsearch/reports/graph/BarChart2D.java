package ro.cst.tsearch.reports.graph;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.io.*;
import com.sun.image.codec.jpeg.*;

public class BarChart2D {
	public static BufferedImage image;//Ini
	public static Graphics graphics;//Ini
	public static int depth, width, height;//Initialized
	public static int labelsTextSize = 10;
	public static int originsTextSize = 8;

	public static int fm, upSpace, downSpace;//Ini
  
	public int max, numGridLines;//Ini in scaleValues
	public static int leftSpace;
	public int numBars;
	public boolean firstTime;
	public double value[], numGridLinesD;
	public String[] labels, downLabels;
	public String mainLabel;
	public Font font;
    
	private byte rotateType 		=	Constants.NO_ROTATE;
    
	public static Hashtable colors = new Hashtable(); 
  
	/**
	 *   width.....the width of the whole image.
	 *   height....the height of the whole image.
	 *   depth.....the depth of the graph.
	 *   labels....the labels of the sets.
	 */
	public BarChart2D(int width, int height, int depth,
			  String[] labels, String[] downLabels, boolean[] labelsHighlight, 
			  double[] value, String mainLabel, boolean isMoney ) {
	
	setRotateType(Constants.NO_ROTATE);
	this.value = value;                          
	this.width = width;
	this.height = height;
	this.depth = depth;
	this.labels = labels;
	this.downLabels = downLabels;
	this.mainLabel = mainLabel;

	image = new BufferedImage( width*9/8, height*9/8, BufferedImage.TYPE_INT_RGB );
	graphics = image.createGraphics();
	numBars = value.length;
      
	// fills the image with bg color
	graphics.setColor((Color)colors.get("background"));
	graphics.fillRect(0, 0, width*5/4, height*5/4);

	graphics.setFont(new Font("Arial", Font.PLAIN, labelsTextSize));
	upSpace = /*30 +*/ height/16;    
	downSpace = graphics.getFontMetrics().getHeight() + 15 + height/8;
	if (firstTime) scaleValues();
	leftSpace = graphics.getFontMetrics().stringWidth( Integer.toString(max) ) + 15 + width/10;
	scaleValues();        
	
	//draws grid
	graphics.setColor((Color)colors.get("gridcolor"));
	//Oy left
	graphics.drawLine( leftSpace, 
			   upSpace + depth + 1,
			   leftSpace, 
			   upSpace + height - downSpace/*+3*/  );
	//Oy right
	graphics.drawLine( width, 
			   upSpace + depth + 1, 
			   width, 
			   upSpace + height - downSpace/*+3*/  );
	//Ox down
	graphics.drawLine( leftSpace, 
			   upSpace+height-downSpace, 
			   width, 
			   upSpace+height-downSpace );
	//Distance between gridlines
	double RyD = (height-depth-downSpace)/numGridLinesD;
	int Ry;
	if( (RyD - (int)RyD)*10 > 5 )
		Ry = (int)RyD + 1;
	else Ry = (int)RyD;
	
	//bar width
	int Rx = (width-leftSpace/*-10 - depth*/) / (numBars+1);
	int s;

	// Now draw the (top) bars 
	graphics.setColor((Color)colors.get("defaultbarcolor"));
	for(s=0; s<value.length; s++ ) {
		//	    int i = s; 
		graphics.fillRect( Rx*s + leftSpace + 7 + Rx/2, 
				   upSpace+height-downSpace-(new Double(value[s])).intValue(), 
				   Rx , 
				   (new Double(value[s])).intValue() );
	}

	//draws the bar contours
	graphics.setColor((Color)colors.get("textcolor"));
	for(s=0; s<value.length; s++ ) {
		//	    int i = s;
		graphics.drawRect( Rx*s + leftSpace + 7 + Rx/2, 
				   upSpace+height-downSpace-(new Double(value[s])).intValue(), 
				   Rx , 
				   (new Double(value[s])).intValue() );
	}

	// bottom colour
	graphics.setColor((Color)colors.get("secondarybgcolor"));
	graphics.fillRect( leftSpace,
			   upSpace+height-downSpace,
			   width-leftSpace, 
			   downSpace / 2 );

	//draws the grid lines
	for( int i = 0; i<=numGridLines; i++ )   {
		graphics.setColor((Color)colors.get("gridcolor"));
		// horizontal lines
		graphics.drawLine( leftSpace, 
				   upSpace+height-downSpace-(numGridLines-i)*Ry, 
				   width, 
				   upSpace+height-downSpace-(numGridLines-i)*Ry ); 
		graphics.setColor((Color)colors.get("textcolor"));

		// draws the left Oy numbers
		String leftLabel = isMoney?"$" + Integer.toString((numGridLines-i)*max/numGridLines):Integer.toString((numGridLines-i)*max/numGridLines);  
		graphics.drawString( leftLabel,
				 leftSpace-3-graphics.getFontMetrics().stringWidth(leftLabel),
				 upSpace+height-downSpace-(numGridLines-i)*Ry+(graphics.getFontMetrics().getHeight()/2) );
	}
	
	graphics.drawRect( leftSpace,
			   upSpace+height-downSpace,
			   width-leftSpace, 
			   downSpace / 2 );
	
	// draws the bottom labels
	for(int i = 0; i < value.length; i++)  {
		if (labelsHighlight!=null && labelsHighlight[i]) {
		graphics.setColor((Color)colors.get("textcolorhighlight"));
		} else {
		graphics.setColor((Color)colors.get("textcolor"));
		}
		graphics.drawString( labels[i], 
				 Rx*i + Rx/2 + leftSpace + 10, 
				 upSpace+height-downSpace+graphics.getFontMetrics().getHeight());
		// draws the second line of bottom labels, if there is one
		if (downLabels != null) {
		graphics.drawString( downLabels[i], 
					 Rx*i + Rx/2 + leftSpace + 10, 
					 upSpace+height-downSpace+graphics.getFontMetrics().getHeight()
					 + downSpace / 4
					 );
		}
	}

	graphics.setColor((Color)colors.get("textcolor"));
	graphics.drawString( mainLabel, 
				 10, 
				 upSpace+height-downSpace+graphics.getFontMetrics().getHeight()
				 + downSpace / 4
				 );

//		graphics.drawRect( 0,
//				   0,
//				   width *9/8 - 1, 
//				   height *9/8 - 1);	
	}

	/**
	 *  Set a user defined color. The label specify the name of the color.
	 */
	public static void setLabelsTextSize(int labelsTextSize) {
	BarChart2D.labelsTextSize = labelsTextSize;
	}

	public static void setOriginsTextSize(int originsTextSize) {
	BarChart2D.originsTextSize = originsTextSize;
	}

	public static void setColor(String label, Color color) {
	colors.put(label, color);
	}

	public void setValue(double[] value) {
	this.value = value;
	}

	public double[] getValue() { 
	return value;
	}
    
	public void drawAxesText( String OxText, String OyText ) 
	{
//		graphics.setFont(new Font("Arial", Font.PLAIN, originsTextSize));
//		graphics.drawString(OxText, width + 3, upSpace+height-downSpace + 3);
//		graphics.drawString(OyText, 
//					leftSpace + depth - graphics.getFontMetrics().stringWidth(OyText)/2, 
//					upSpace - 4 );
	}
    
	private void scaleValues()
	{
		double maxValue = 0.0;
		for(int i=0; i<value.length; i++ ) {
		if (maxValue<value[i]) 
		maxValue = value[i];
	}    
		max = Integer.parseInt((Integer.toString((new Double(maxValue)).intValue())).substring(0,1)) + 1;
		numGridLines = max;
		numGridLinesD = max;        

		for( int i = (Integer.toString((new Double(maxValue)).intValue())).length()-1; i>0; i-- )
		{
			max *= 10;
		}
        
		double ratio = (double)(height-depth-downSpace)/(double)max;

		for(int i=0; i<value.length; i++ )   {
		value[i] = value[i]*ratio;
	}
            
		firstTime = false;    
	}

	public void putJpegInOutputStream(OutputStream out) 
	{
	JpegEncoder jpg = new JpegEncoder(image, 100, out);
	jpg.Compress();
	}
   
    
	public void putInOutputStream(OutputStream out) {
		try {
	
		BufferedImage  dstImage   = getFilteredImage();			
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam   param=encoder.getDefaultJPEGEncodeParam(dstImage);    
		param.setQuality(1, true);
		param.setDensityUnit( JPEGEncodeParam.DENSITY_UNIT_DOTS_INCH);             
		int xDensity = 12000;                
		param.setXDensity(xDensity);                    
		int yDensity =12000;
		param.setYDensity(yDensity);     
		encoder.setJPEGEncodeParam(param);
			encoder.encode(dstImage);
		//            out.close();
		} catch( IOException ex) {
			ex.printStackTrace();
		}
	}
    
	static {
	colors.put("green", new Color(0, 255, 0));
	colors.put("red", new Color(255, 0, 0));
	colors.put("blue", new Color(0, 0, 255));
	colors.put("yellow", new Color(255, 255, 0));
	colors.put("cyan", new Color(98, 193, 189));
	colors.put("black", new Color(0, 0, 0));    
	colors.put("magenta", new Color(0, 192, 192));
	colors.put("orange", new Color(255, 127, 0));
	colors.put("pink", new Color(255, 0, 255));
	colors.put("white",new Color(255, 255, 255));
	colors.put("background", new Color(255, 255, 255));
//		colors.put("titlecolor", Color.white);
//		colors.put("titletextcolor", Color.black);      
	colors.put("textcolor", Color.black);      
	colors.put("textcolorhighlight", Color.red);      
	colors.put("gridcolor", Color.black);      
	colors.put("defaultbarcolor", new Color(100, 170, 210));
	colors.put("secondarybgcolor", new Color(220, 220, 195));
	}
 
	public byte getRotateType() {
		return rotateType;
	}

	public void setRotateType(byte rotateType) {
		this.rotateType = rotateType;
	}
    
	/**
	 * get the filtered image
	 * if no rotate then no filter is aplied 
	 */
	 //XXX - See Graphics2D#rotate 
	private BufferedImage getFilteredImage() {
		if (rotateType==Constants.NO_ROTATE  ) {
			return image ;
		} else {

			BufferedImage dst = new BufferedImage( image.getHeight(), image.getWidth(), BufferedImage.TYPE_INT_RGB );;	
	    
			AffineTransform affineTransform = new AffineTransform();
			//translate
			affineTransform.translate( 0, image.getWidth());	    	
			//rotate
			affineTransform.rotate(3 * Math.PI/2);
			AffineTransformOp transformOp = new AffineTransformOp( affineTransform , AffineTransformOp.TYPE_BILINEAR ); 
	    
				//apply filter
				transformOp.filter(image, dst);
            	
				return dst;
    	  		
		}
	}

}
