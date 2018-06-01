package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.utils.FileUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.datatree.DataTreeImageException;


public class MOPlatteImageRetriever {
	
	private static MOPlatteImageRetriever singletonObject;
	
	private MOPlatteImageRetriever() {
	}
	
	public static synchronized MOPlatteImageRetriever getSingletonObject() {
		if (singletonObject == null) {
			singletonObject = new MOPlatteImageRetriever();
		}
		return singletonObject;
	}
	
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	public DownloadImageResult retrieveImage(DataSite dataSite, DocumentI document, long searchId){
		
		byte[] imageBytes = null;

		MODataTreeImgRetriever mod = new MODataTreeImgRetriever(dataSite);
		
		InstrumentI instrument = document.getInstrument();
		boolean foundOnDataTree = false;
		try {
			foundOnDataTree = mod.retrieveImageFromDataTree(instrument, "", document.getImage().getPath(), "", searchId, dataSite);
		} catch (DataTreeImageException e1) {
			e1.printStackTrace();
		}
		
		if (foundOnDataTree){

			String imageName = document.getImage().getPath();
			if (FileUtils.existPath(imageName)){
				imageBytes = FileUtils.readBinaryFile(imageName);
			   		return new DownloadImageResult( DownloadImageResult.Status.OK, imageBytes, document.getImage().getContentType());
			}
		}
		
		
		
		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], document.getImage().getContentType());
	}
	
}
