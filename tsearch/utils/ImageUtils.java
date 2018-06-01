package ro.cst.tsearch.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.pdftiff.DP;

import com.stewart.ats.base.document.ImageI.IType;

public class ImageUtils {

	public static File convertTiffToPDFFile(File inputFile) {
		return convertTiffToPDFFile(inputFile, ServerConfig.getTsrCreationTempFolder());
	}
	
	/**
	 * Converts the given input file, which is expected to be tiff to a pdf file with the same name in the given output folder.
	 * @param inputFile
	 * @param outputFolder
	 * @return new created file
	 */
	public static File convertTiffToPDFFile(File inputFile, String outputFolder) {
		DP dp = new DP(	FilenameUtils.getBaseName(inputFile.getName()), outputFolder);
		dp.setOutputType(IType.PDF);
		dp.appendImage(DP.IMAGE_TYPE_TIFF, inputFile.getPath(), false);
		String process = dp.process(-1);
		FileUtils.deleteQuietly(new File(process));
		return new File(dp.getOutputFileName());
	}
	
	/**
	 * returns true if the image is a valid TIFF image
	 */
	public static boolean checkTIFFImage(byte[] image) {
		ImageReader tiffReader = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(image);

		try
		{
			// locate a TIFF reader
			Iterator<ImageReader> tiffReaders = ImageIO.getImageReadersByFormatName("tiff");
			if (!tiffReaders.hasNext()) {
				return false;
			}
			tiffReader = tiffReaders.next();
			
			// point it to our image file
			ImageInputStream tiffStream = ImageIO.createImageInputStream(bais);
			tiffReader.setInput(tiffStream);
		
			// read one page from the TIFF image
			tiffReader.readAll(0, null);
		} catch (Exception e) {
			return false;
		} finally {
			if (tiffReader != null) {
				tiffReader.dispose();
			} 
		}
		return true;
	}
	
	public static void main(String[] args) {
		File f = convertTiffToPDFFile(new File("D:\\work\\tsr related\\images\\4626106_1652592512_0.tiff"));
		System.out.println(f.getAbsolutePath());
	}
	
}
