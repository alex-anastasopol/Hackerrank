package ro.cst.tsearch.utils;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Category;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;

/**
 * @author radu bacrau
 */
public class TiffConcatenator {

	protected static final Category logger = Category.getInstance(TiffConcatenator.class.getName());
	private static ResourceBundle rbc = ResourceBundle
			.getBundle(URLMaping.SERVER_CONFIG);
	private final static String tiffcpCommand = rbc.getString(
			"tiffcp.CommandString").trim();;

	// prevent instantiation
	private TiffConcatenator() {
	}

	/**
	 * Concatenate a list of tiff files into a single tiff file
	 * 
	 * @param inputFiles
	 *            array of input file names
	 * @param outputFile
	 *            output file name
	 */
	public static void concatenate(String[] inputFiles, String outputFile) {

		try {
			String exec[] = new String[1 + inputFiles.length + 1];
			int pos = 0;
			exec[pos++] = tiffcpCommand;
			// if you use -c g4 you get Error: Bits/sample must be 1 for Group
			// 3/4 encoding/decoding
			// g4 can only be used with black and white only images
			// http://www.asmail.be/msg0054600137.html
			for (int i = 0; i < inputFiles.length; i++) {
				exec[pos++] = inputFiles[i];
			}
			exec[pos++] = outputFile;
			logger.info("Runnig tiff concatenation : " + exec);

			ClientProcessExecutor cpe = new ClientProcessExecutor(exec, true,
					true);
			cpe.start();
			logger.info(cpe.getCommandOutput());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * Concatenate a list of tiff files into a single tiff file
	 * 
	 * @param inputFiles
	 *            array of input file names
	 * @param outputFile
	 *            output file name
	 */
	public static void concatenate(File[] inputFiles, String outputFile) {
		ClientProcessExecutor cpe = null;
		try {
			String exec[] = new String[1 + inputFiles.length + 1];
			int pos = 0;
			exec[pos++] = tiffcpCommand;
			// if you use -c g4 you get Error: Bits/sample must be 1 for Group
			// 3/4 encoding/decoding
			// g4 can only be used with black and white only images
			// http://www.asmail.be/msg0054600137.html
			for (int i = 0; i < inputFiles.length; i++) {
				exec[pos++] = inputFiles[i].getAbsolutePath();
			}
			exec[pos++] = outputFile;
			logger.info("Runnig tiff concatenation : " + exec);

			cpe = new ClientProcessExecutor(exec, true, true);
			cpe.start();
			logger.info(cpe.getCommandOutput());

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}

	}

	public static byte[] concateTiff(List<byte[]> inputTiffs) {
		return concateTiff(true, inputTiffs.toArray(new byte[inputTiffs.size()][]));
	}
	
	public static byte[] concateTiff(boolean doNotCleanFiles, byte[] ... inputTiffs) {
		List<IIOImage> imageList = new ArrayList<IIOImage>();
		IIOImage tiffImage = null;

		try {
			// locate a TIFF reader
			Iterator<ImageReader> tiffReaders = ImageIO .getImageReadersByFormatName("tiff");
			if (!tiffReaders.hasNext())
				throw new IllegalStateException("No TIFF reader found");
			ImageReader tiffReader = tiffReaders.next();

			for (byte[] b:inputTiffs) {
				
				if(!doNotCleanFiles) {
					//so clean
					b = cleanImage(b);
				}
				
				// point it to our image file
				tiffReader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(b)));
				// read pages until end of image from the TIFF file
				try {
					for (int index=0;;index++) {
						tiffImage = tiffReader.readAll(index, tiffReader.getDefaultReadParam());
						imageList.add(tiffImage);
					}
				} catch (IndexOutOfBoundsException ex) {}
			}

			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
			ImageWriter writer = writers.next();
			
			ByteArrayOutputStream resStream = new ByteArrayOutputStream();
			ImageOutputStream ios = ImageIO.createImageOutputStream(resStream);
			writer.setOutput(ios);

			IIOImage firstIioImage = imageList.remove(0);
			
			try {
				writer.write(firstIioImage);
			} catch (NullPointerException e) {
				logger.error("Error while adding tiff - go clean it", e);
				//we need to clean the file
				if(doNotCleanFiles) {
					return concateTiff(false, inputTiffs);
				} else {
					throw e;
				}
				
			}
			int i = 1;
			for (IIOImage iioImage : imageList) {
				try {
					writer.writeInsert(i++, iioImage, null);
				} catch (NullPointerException e) {
					logger.error("Error while adding tiff - go clean it", e);
					//we need to clean the file
					if(doNotCleanFiles) {
						return concateTiff(false, inputTiffs);
					} else {
						throw e;
					}
					
				}
			}
			ios.close();
			return resStream.toByteArray();
		} catch (IOException e) {
			System.out.println("error merging tiff. Msg=" + e.getMessage());
		}
		return null;
	}
	

	
	public static byte[] concatePngInTiff(List<byte[]> inputPngs) {
	
		return concatenatePngInTiff(inputPngs, "LZW", 0.9f);
	}
	
	public static byte[] concatenatePngInTiff(List<byte[]> inputPngs, String compressionType, float compressionQuality) {
		List<IIOImage> imageList = new ArrayList<IIOImage>();
		IIOImage tiffImage = null;

		try {
			// locate a TIFF reader
			Iterator<ImageReader> tiffReaders = ImageIO .getImageReadersByFormatName("tiff");
			if (!tiffReaders.hasNext())
				throw new IllegalStateException("No TIFF reader found");
			ImageReader tiffReader = tiffReaders.next();
			
			for (byte[] b:inputPngs) {
				
				
				BufferedImage img = ImageIO.read(new ByteArrayInputStream(b));
		        TIFFImageWriterSpi tiffspi = new TIFFImageWriterSpi();
		        ImageWriter writer = tiffspi.createWriterInstance();
		        
		        ImageWriteParam param = writer.getDefaultWriteParam();
		        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		        param.setCompressionType(compressionType);
		        param.setCompressionQuality(compressionQuality);
				
		        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(25);
		        ImageOutputStream ios = ImageIO.createImageOutputStream(byteArrayOutputStream);
		        writer.setOutput(ios);
		       
		        writer.write(null, new IIOImage(img, null, null), param);
		        
		        ios.flush();
		        
		        ByteArrayInputStream bai = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		        ImageInputStream tiffStream = ImageIO.createImageInputStream(bai);
				tiffReader.setInput(tiffStream);

				try {
					for (int index=0;;index++) {
						tiffImage = tiffReader.readAll(index, null);
						imageList.add(tiffImage);
					}
				} catch (IndexOutOfBoundsException ex) {}
			}

			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
			ImageWriter writer = writers.next();
			
			ByteArrayOutputStream resStream = new ByteArrayOutputStream();
			ImageOutputStream ios = ImageIO.createImageOutputStream(resStream);
			writer.setOutput(ios);

			IIOImage firstIioImage = imageList.remove(0);
			writer.write(firstIioImage);
			int i = 1;
			for (IIOImage iioImage : imageList) {
				writer.writeInsert(i++, iioImage, null);
			}
			ios.close();
			return resStream.toByteArray();
		} catch (IOException e) {
			System.out.println("error merging tiff. Msg=" + e.getMessage());
		}
		return null;
	}
	
	public static void concateTiff(String[] inputTiffs, String outputTiff) {
		List<IIOImage> imageList = new ArrayList<IIOImage>();
		ImageInputStream tiffStream = null;
		IIOImage tiffImage = null;

		try {
			// locate a TIFF reader
			Iterator<ImageReader> tiffReaders = ImageIO .getImageReadersByFormatName("tiff");
			if (!tiffReaders.hasNext())
				throw new IllegalStateException("No TIFF reader found");
			ImageReader tiffReader = tiffReaders.next();

			for (int i = 0; i < inputTiffs.length; i++) {
				System.out.println("image-" + i);
				// point it to our image file
				tiffStream = ImageIO.createImageInputStream(new File(inputTiffs[i]));
				tiffReader.setInput(tiffStream);
				// read pages until end of image from the TIFF file
				try {
					for (int index=0;;index++) {
						tiffImage = tiffReader.readAll(index, tiffReader.getDefaultReadParam());
						imageList.add(tiffImage);
					}
				} catch (IndexOutOfBoundsException ex) {}
			}

			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
			ImageWriter writer = writers.next();
			ImageOutputStream ios = ImageIO.createImageOutputStream(new File(outputTiff));
			writer.setOutput(ios);

			IIOImage firstIioImage = imageList.remove(0);
			writer.write(firstIioImage);
			int i = 1;
			for (IIOImage iioImage : imageList) {
				writer.writeInsert(i++, iioImage, null);
			}
			ios.close();
			tiffStream.close();
		} catch (IOException e) {
			System.out.println("error merging tiff. Msg=" + e.getMessage());
		}
	}
	
	public static byte[] cleanImage(byte[] image) {
		
		long timeStamp = System.currentTimeMillis();
		long randomLong = RandomGenerator.getInstance().getLong();
		String file1 = ZipUtils.getTempzipfolder() + "zipDocument_" + randomLong + "_" + timeStamp + ".tiff";
		String file2 = ZipUtils.getTempzipfolder() + "zipDocument_" + randomLong + "_" + timeStamp + "_better.tiff";
		File fileF1, fileF2 = null;
		try {
			FileUtils.writeByteArrayToFile(new File(file1), image);
			
			String exec[] = new String[3];
			exec[0] = tiffcpCommand;
			exec[1] = file1;
			exec[2] = file2;
			
			logger.info("Running tiff conversion");

			ClientProcessExecutor cpe = new ClientProcessExecutor(exec, true, true);
			cpe.start();
			
			fileF2 = new File(file2);
			
			if(fileF2.exists()) {
				return FileUtils.readFileToByteArray(fileF2);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(fileF2 != null) {
				fileF2.delete();
			}
			fileF1 = new File(file1);
			if(fileF1.exists()) {
				fileF1.delete();
			}
		}
		
		
		return image;
	}

	public static void main(String[] args) throws IOException {
		String[] inputFiles = new String[] { 
				"C:/Users/Andrei/Desktop/Arapahoe-40056-2010.001.tiff", 
				"C:/Users/Andrei/Desktop/Denver-70040-2007.001.tiff", 
				"C:/Users/Andrei/Desktop/Arapahoe-40056-2010.001-ok.tiff",
				"D:/bugs/6971/Arapahoe-40056-2010.001_2.tiff"};
		//concatenate(inputFiles, "D:\\Imagini\\result1.tiff");
		byte[] b1 = org.apache.commons.io.FileUtils.readFileToByteArray(new File(inputFiles[3]));
		//byte[] b2 = FileUtils.readFileToByteArray(new File("c:\\2.TIF"));
		
		//FileUtils.writeByteArrayToFile(new File("c:\\4.tiff"), concateTiff(b1,b2));
		
//		Iterator<ImageReader> tiffReaders = ImageIO .getImageReadersByFormatName("tiff");
//		if (!tiffReaders.hasNext())
//			throw new IllegalStateException("No TIFF reader found");
//		ImageReader tiffReader = tiffReaders.next();
//		IIOImage tiffImage = null;
//		
//		tiffReader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(b1)));
//		tiffImage = tiffReader.readAll(0, tiffReader.getDefaultReadParam());
//		
//		
//		
//		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
//		ImageWriter writer = writers.next();
//		
//		ByteArrayOutputStream resStream = new ByteArrayOutputStream();
//		ImageOutputStream ios = ImageIO.createImageOutputStream(resStream);
//		writer.setOutput(ios);
//		
//		dumpMetadata(tiffImage.getMetadata(), true);
//		
//		
//		writer.write(tiffImage);
		
		
		
		
		
		System.out.println("Done");
		
	}
	
	public static void dumpMetadata(IIOMetadata meta, boolean nativeFormat) {
        String format;
        if (nativeFormat) {
            format = meta.getNativeMetadataFormatName();
        } else {
            format = IIOMetadataFormatImpl.standardMetadataFormatName;
        }
        Node node = meta.getAsTree(format);
        
        
        dumpNode(node);
        
        
        
    }
    
    public static void dumpNode(Node node) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            Source src = new DOMSource(node);
            Result res = new StreamResult(System.out);
            
            t.transform(src, res);
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
	/**
	 * Read one page out of the TIFF file and return it.
	 *
	 * @param imageFile
	 * @param pageNumber
	 * @return
	 * @throws IOException
	 */
	public static IIOImage readDocumentPage(File imageFile) throws IOException
	{
		ImageReader tiffReader = null;

		try
		{
			// locate a TIFF reader
			Iterator<ImageReader> tiffReaders = ImageIO.getImageReadersByFormatName("tiff");
			if (!tiffReaders.hasNext())
				throw new IllegalStateException("No TIFF reader found");
			tiffReader = tiffReaders.next();

			// point it to our image file
			ImageInputStream tiffStream = ImageIO.createImageInputStream(imageFile);
			tiffReader.setInput(tiffStream);

			// read one page from the TIFF image
			return tiffReader.readAll(0, null);
		} finally
		{
			if (tiffReader != null)
				tiffReader.dispose();
		}
	}

	/**
	 * Rescale the input image to fit the maximum dimensions indicated.
	 * Only large images are shrunk; small images are not expanded.
	 *
	 * @param source
	 * @param maxWidth
	 * @param maxHeight
	 * @return
	 */
	public static RenderedImage scaleImage(IIOImage source, float maxWidth, float maxHeight)
	{
		// shrink but respect the original aspect ratio
		float scaleFactor;
		if (source.getRenderedImage().getHeight() > source.getRenderedImage().getWidth()) {
			scaleFactor = maxHeight / source.getRenderedImage().getHeight();
		} else {
			scaleFactor = maxWidth / source.getRenderedImage().getWidth();
		}

		if (scaleFactor >= 1) {
			// don't expand small images, only shrink large ones
			return source.getRenderedImage();
		}

		// prepare parameters for JAI function call
		ParameterBlockJAI params = new ParameterBlockJAI("scale");
		params.addSource(source.getRenderedImage());
		params.setParameter("xScale", scaleFactor);
		params.setParameter("yScale", scaleFactor);

		RenderedOp resizedImage = JAI.create("scale", params);

		return resizedImage;
	}

}
