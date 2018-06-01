package ro.cst.tsearch.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;

import org.apache.commons.io.FileUtils;

import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;


public class ImageResizer {
	
	public static enum ConversionStatus {
		NO, YES, ERROR;
	}
	
	public static final  class ResizeResult {
		public ConversionStatus status;
		public byte[]	imageData;
	}
	
	private static RenderingHints hints;
	static {
		hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		hints.put(RenderingHints.KEY_COLOR_RENDERING,
				RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		hints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		/*
		 * Properties p = new Properties(System.getProperties());
		 * p.put("com.sun.media.jai.disableMediaLib", "true");
		 * System.setProperties(p);
		 */
	}

	public static BufferedImage getScaledDownInstance(BufferedImage img, int maxWidth, int maxHeight) {
		double scaleWidth = (double) maxWidth / img.getWidth();
		double scaleHeight = (double) maxHeight / img.getHeight();
		double scale = Math.min(scaleHeight, scaleWidth);

		if (scale > 0 && scale < 1.0d) {
			return getScaledDownByGraphics(img, scale);
		}

		return img;
	}

	/**
	 * This method produces high quality images when target scale is greater
	 * than 50% of the original.
	 * 
	 * @param img
	 * @param scale
	 * @return the scaled image
	 */
	private static BufferedImage getScaledDownByGraphics(BufferedImage img, double scale) {
		final float scaleFactor = 0.8f;

		BufferedImage ret = img;
		int w = img.getWidth();
		int h = img.getHeight();

		int targetWidth = (int) (img.getWidth() * scale);
		int targetHeight = (int) (img.getHeight() * scale);

		int loopCount = 0;
		int maxLoopCount = 20;
		BufferedImage tmp;
		do {
			if (w > targetWidth) {
				w *= scaleFactor;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}
			if (h > targetHeight) {
				h *= scaleFactor;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}
			tmp = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
			Graphics2D g2 = tmp.createGraphics();

			g2.addRenderingHints(hints);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
			if (++loopCount > maxLoopCount) {
				throw new RuntimeException("Hit maximum loop count "
						+ maxLoopCount);
			}
		} while (w != targetWidth || h != targetHeight);
		return ret;
	}

	
	protected static byte[] saveTiffToByteArray(BufferedImage image) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedOutputStream bufOut = new BufferedOutputStream( out );
		ImageOutputStream ios = new MemoryCacheImageOutputStream(bufOut);
		ImageWriter writer = null;

		try {
			// find an appropriate writer
			Iterator<ImageWriter> it = ImageIO
					.getImageWritersByFormatName("TIF");
			if (it.hasNext()) {
				writer = (ImageWriter) it.next();
			} else {
				return null;
			}
			
			writer.setOutput(ios);
			TIFFImageWriteParam writeParam = new TIFFImageWriteParam(Locale.ENGLISH);
			writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			
			writeParam.setCompressionType("CCITT T.6");
			writeParam.setCompressionQuality(1.0f);

			// convert to an IIOImage
			IIOImage iioImage = new IIOImage(image, null, null);
			writer.write(null, iioImage, writeParam);
			ios.flush();
			ios.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return out.toByteArray();

	}
	
	protected static boolean saveTiff(String filename, BufferedImage image) {

		File tiffFile = new File(filename);
		ImageOutputStream ios = null;
		ImageWriter writer = null;

		try {

			// find an appropriate writer
			Iterator<ImageWriter> it = ImageIO
					.getImageWritersByFormatName("TIF");
			if (it.hasNext()) {
				writer = (ImageWriter) it.next();
			} else {
				return false;
			}

			// setup writer
			ios = ImageIO.createImageOutputStream(tiffFile);
			writer.setOutput(ios);
			TIFFImageWriteParam writeParam = new TIFFImageWriteParam(
					Locale.ENGLISH);
			writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			/*
			 * for(int t=0;t<writeParam.getCompressionTypes().length;t++){
			 * System.out.println(writeParam.getCompressionTypes()[t]); }
			 */

			writeParam.setCompressionType("CCITT T.6");
			writeParam.setCompressionQuality(1.0f);

			// convert to an IIOImage
			IIOImage iioImage = new IIOImage(image, null, null);

			// write it!
			writer.write(null, iioImage, writeParam);
			ios.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}
	
	/**
	 * When you scale up, there's nothing complicated that will give you a
	 * better image. Just scale it up on a basic
	 * 
	 * @param orig
	 * @param scale
	 * @return
	 */
	/*
	 * private static BufferedImage getScaledUpGraphics(final BufferedImage
	 * orig, double scale) { int scaledWidth = (int)(orig.getWidth() * scale);
	 * int scaledHeight = (int)(orig.getHeight() * scale); BufferedImage
	 * scaledBI = new BufferedImage(scaledWidth, scaledHeight,
	 * BufferedImage.TYPE_INT_RGB); Graphics2D g = scaledBI.createGraphics();
	 * g.setRenderingHints(hints); g.drawImage(orig, 0, 0, scaledWidth,
	 * scaledHeight, null); g.dispose(); return scaledBI; }
	 */

	/**
	 * See
	 * http://www.digitalsanctuary.com/tech-blog/java/how-to-resize-uploaded-
	 * images-using-java-better-way.html This instance seems to produce quality
	 * images ONLY when you are scaling down to something less than 50% of the
	 * original size.
	 * 
	 * @param img
	 * @param scale
	 * @return the scaled image
	 */
	/*
	 * private static BufferedImage getScaledDownByJAI(BufferedImage img, double
	 * scale) { if(scale > 1.0d) { throw new
	 * RuntimeException("Can't scale according to " + scale +
	 * " : This method only scales down."); } PlanarImage originalImage =
	 * PlanarImage.wrapRenderedImage(img); // now resize the image
	 * ParameterBlock paramBlock = new ParameterBlock();
	 * paramBlock.addSource(originalImage); // The source image
	 * paramBlock.add(scale); // The xScale paramBlock.add(scale); // The yScale
	 * paramBlock.add(0.0); // The x translation paramBlock.add(0.0); // The y
	 * translation
	 * 
	 * RenderedOp resizedImage = JAI.create("SubsampleAverage", paramBlock,
	 * hints); return resizedImage.getAsBufferedImage(); }
	 */

	

	public static void main(String[] args) {
		for (int i = 1; i < 10; i++) {
			try {
				ResizeResult result = getBytesScaledDownInstance("e:/temp1/" + i + ".tif", 8400,8400);
				if(result!=null){
					
					if( ConversionStatus.YES.equals(result.status) ){
						FileUtils.writeByteArrayToFile(new File("e:/temp1/" + i + "_" + 0 + "_out.tif"), result.imageData);
					}else if(ConversionStatus.NO.equals(result.status)){
						System.err.println("File : " + "e:/temp1/" + i + ".tif does not need conversion");
					}else{
						System.err.println("Internal error for file : " + "e:/temp1/" + i + ".tif");
					}
					
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static ResizeResult getBytesScaledDownInstance(String fileName, int maxWidth, int maxHeight) {
		ResizeResult results = new ResizeResult();
		
		SeekableStream ss = null;
		try {
			ss = new FileSeekableStream(fileName);
			
			ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", ss, null);

			int numPages = decoder.getNumPages();
			BufferedImage image[] = new BufferedImage[numPages];
			List<byte[]> imagePagesTempContent = new ArrayList<byte[]>();
			
			boolean conversionPerformed = false;
			for (int j = 0; j < numPages; j++) {
				PlanarImage op = new NullOpImage(decoder.decodeAsRenderedImage(j), null,OpImage.OP_IO_BOUND, null);
				BufferedImage original = op.getAsBufferedImage();
				image[j] = getScaledDownInstance(original, 8400,8400);
				
				if(image[j]==null){
					results.status = ConversionStatus.ERROR;
					return results;
				}
				
				conversionPerformed = (conversionPerformed || image[j].getHeight()!=original.getHeight() || image[j].getWidth()!=original.getWidth());
				
				if(conversionPerformed){
					imagePagesTempContent.add( saveTiffToByteArray(image[j]) );
				}
			}
			
			if(imagePagesTempContent.size()>0){
				results.imageData =  TiffConcatenator.concateTiff(imagePagesTempContent);
				results.status = ConversionStatus.YES;
			}else{
				results.status = ConversionStatus.NO;
			}
			
		} catch (Exception e1) {
			results.status = ConversionStatus.ERROR;
			e1.printStackTrace();
		}
		
		return results;
	}
	
}