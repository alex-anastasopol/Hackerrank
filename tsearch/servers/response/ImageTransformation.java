package ro.cst.tsearch.servers.response;


import java.util.HashSet;
import java.util.Set;

import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;

public class ImageTransformation {
	
	public static ImageI imageLinkInPageToImage(ImageLinkInPage ilip){
		ImageI image = new Image();
		image.setFileName(ilip.getImageFileName());
		image.setPath(ilip.getPath());
		image.setSaved(ilip.isSolved());
		Set<String> links = new HashSet<String>();
		links.add(ilip.getLink());
		image.setLinks(links);
		image.setContentType(ilip.getContentType());
		image.setUploaded(ilip.isFakeLink());
		image.setSsfLink(ilip.getSsfLink());
		return image;
	}
	
	public static ImageLinkInPage imageToImageLinkInPage(ImageI image){
		ImageLinkInPage ilip = new ImageLinkInPage(image.getPath(), image.getFileName());
		ilip.setSolved(image.isSaved());
		ilip.setLink(image.getLink(0));
		ilip.setPath(image.getPath());
		ilip.setContentType( image.getContentType() );
		ilip.setSsfLink(image.getSsfLink());
		return ilip;
	}
}
