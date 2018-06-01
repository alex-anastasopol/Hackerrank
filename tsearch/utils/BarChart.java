package ro.cst.tsearch.utils;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Category;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;


public class BarChart {
	protected static final Category logger= Category.getInstance(BarChart.class.getName());
	
    public Color backgroundColor = new Color(230, 220, 183);
    public Color backgroundBarColor = new Color(121, 134, 185);    
    public Color drawingColor = new Color(50, 56, 185);
    public Color horizontalBarsColor = new Color(203, 201, 133);
    public BufferedImage image;
    public Graphics2D graphics;
    public int xOrigin, yOrigin, xLength, yLength; 
    public int xScaleMax, yScaleMax;
    public int xScaleGradLength, yScaleGradLength;

    
    /**
    * Set the horizontalBarsColor.
    */
    public void setHorizontalBarsColor( Color horizontalBarsColor ) {
        this.horizontalBarsColor = horizontalBarsColor;
    }
    /**
    * Get the horizontalBarsColor.
    */
    public Color getHorizontalBarsColor() {
        return horizontalBarsColor;
    }
    
    
    /**
    * Set the backgroundColor.
    */  
    public void setBackgroundColor( Color backgroundColor ) {
        this.backgroundColor = backgroundColor;
    }
    /**
    * Get the backgroundColor.
    */
    
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    /**
    * Set the backgroundBarColor.
    */    
    public void setBackgroundBarColor( Color backgroundBarColor ) {
        this.backgroundBarColor = backgroundBarColor;
    }
    /**
    * Get the backgroundBarColor.
    */
    
    public Color getBackgroundBarColor() {
        return backgroundBarColor;
    }
    /**
    * Set the drawingColor.
    */      
    public void setDrawingColor( Color drawingColor ) {
        this.drawingColor = drawingColor;
    }
    /**
    * Get the drawingColor.
    */
    
    public Color getDrawingColor() {
        return drawingColor;
    }
    
    /**
    * The constructor. The parameters are specifying the dimension of the image containing the graph, 
    * NOT INCLUDING THE TITLE BAR. After created, the order the methods should 
    * be called is: "drawAxes", "scale", "drawHorizontalBars", "drawBarAt", "drawTextInTitleBar".
    */
    public BarChart( int width, int height ) {
     try {
        image = new BufferedImage( width, height*7/6, BufferedImage.TYPE_INT_RGB );
        graphics = image.createGraphics();
        graphics.setBackground(Color.white);
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, height/6, width, height);
        graphics.setColor(backgroundBarColor);
        graphics.fillRect(0, 0, width, height/12);    
        graphics.setColor(Color.white);
        graphics.fillRect(0, height/12, width, height/12+1);           
        xOrigin = width*1/8;
        yOrigin = height*25/24;
        xLength = width*6/8;
        yLength = height*6/8;
  

 
     } catch(Exception e) {
         e.printStackTrace();
     }        
    }
    /**
    * Draw the horizontal bars.
    */
    public void drawHorizontalBars() {
        graphics.setColor(horizontalBarsColor);
        for(int i = 1; i < yScaleMax; i=i+2)
            graphics.fillRect(xOrigin +1, yOrigin - (i+1)*yScaleGradLength, xLength, yScaleGradLength);
    }
    /**
    * Scale the axes. It should be called after the "drawAxes". The xScaleMax, yScaleMax
    * represent the maximum number on Ox and Oy axes, reliefateYscale put marks on Oy if true
    * setNumbersOx draw number on Ox if true, oxText, oyText are the texts at the end of
    * axes.
    */
    public void scale( int xScaleMax, int yScaleMax, boolean reliefateYscale,
                       boolean setNumbersOx, String oxText, String oyText ) {
        this.xScaleMax = xScaleMax;
        this.yScaleMax = yScaleMax;
        xScaleGradLength = xLength/xScaleMax;
        yScaleGradLength = yLength/yScaleMax;        
        if( reliefateYscale ) 
            for( int i = 1; i <= yScaleMax; i++ )
                graphics.drawLine( xOrigin, yOrigin - i*yScaleGradLength, 
                                   xOrigin - yScaleGradLength/4, yOrigin - i*yScaleGradLength );   
        if(setNumbersOx) {
            graphics.setFont(new Font("Arial", Font.BOLD, yScaleGradLength/2));
            for( int j = 1; j <= xScaleMax; j++ )
                graphics.drawString(Integer.toString(j), xOrigin + j*xScaleGradLength, yOrigin + yScaleGradLength/2 );
        }
        graphics.drawString(oxText, xOrigin + xScaleMax*xScaleGradLength, yOrigin + yScaleGradLength);
        graphics.rotate(-1.5607);
        graphics.drawString(oyText, -yOrigin+(yScaleMax-1)*yScaleGradLength, xOrigin - yScaleGradLength/2);
        graphics.rotate(1.5607);          
    }
    /**
    * Draw a vertical bar having the yScaleValue Oy magnitude at the xScaleValue position
    * on Ox axe.
    */
    public void drawBarAt(int xScaleValue, int yScaleValue) {
        graphics.setColor(backgroundBarColor);
        graphics.fillRect( xOrigin + xScaleGradLength*xScaleValue - xScaleGradLength/3, yOrigin - yScaleValue*yScaleGradLength, 
                           xScaleGradLength*2/3, yScaleValue*yScaleGradLength );  
    }
    /**
    *  Draw the text in the title bat.
    */
    public void drawTextInTitleBar( String text ) {
        graphics.setColor(drawingColor);
        graphics.setFont(new Font("Arial", Font.BOLD, image.getHeight()*3/56));
        graphics.drawString(text, 0, image.getHeight()*3/54);    

    }
    /**
    * Draw the axes
    */
    public void drawAxes() {
        graphics.setColor(drawingColor);
        graphics.drawLine( xOrigin, yOrigin, xOrigin + xLength, yOrigin );
        graphics.drawLine( xOrigin, yOrigin, xOrigin, yOrigin - yLength );
        
    }
    public void setTheBackgroundColor( Color color ) {
        graphics.setBackground( color );
    }
    /**
    *  Put the JPEG in the out stream.
    */
    public void putInOutputStream(OutputStream out) {
        try {
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            encoder.encode(image);
        } catch( IOException ex) {
            ex.printStackTrace();
        }
        
    }
    
    public static void main(String[] args) {
      try {
        BarChart bc = new BarChart(370, 250);
        bc.drawAxes();
        bc.scale(10, 10, true, true, "month", "oy text");
        bc.drawHorizontalBars();
        bc.drawBarAt( 2, 5 );
        bc.drawBarAt( 8, 2 ); 
        bc.drawTextInTitleBar("test");        
        //bc.drawOyText("oy test");

        FileOutputStream fos = new FileOutputStream("test.jpg");
              
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bc.putInOutputStream(bos);
        logger.info("Here");
        bos.flush();
        bos.close();
      } catch(Exception ex) {
          ex.printStackTrace();
      }
        
    }
}
