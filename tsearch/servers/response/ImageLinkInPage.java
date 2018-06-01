package ro.cst.tsearch.servers.response;

import java.io.Serializable;

import ro.cst.tsearch.utils.StringUtils;

@Deprecated
public class ImageLinkInPage implements Serializable, Cloneable {

	static final long serialVersionUID = 10000001;

	private String link = "";
	private String ssfLink = "";
	
	private String imageFileName = "";
	private String path = "";
	private String contentType ="";
	
	private boolean solved = false;
	private boolean fakeLink = false;

	private String downloadStatus = "NOT_DOWNLOADED";

	private boolean justImageLookUp;

	public ImageLinkInPage( boolean justImageLookUp ){
		this.justImageLookUp = justImageLookUp;
	}
	
	public ImageLinkInPage(String link, String imageFileName) {
		this.link = link;
		this.imageFileName = imageFileName;
		solved = false;
		downloadStatus = "NOT_DOWNLOADED";
	}

	public String getLink() {
		return link;
	}
	
	public void setLink(String link) {
		this.link = link;
	}

	public String getImageFileName() {
		return imageFileName;
	}

	public String toString() {
		return "ImageLinkInPage[ fileName= "
			+ imageFileName
			+ " :: link= "
			+ link
			+ " :: solved= "
			+ solved
			+ " :: downloadStatus= "
			+ downloadStatus
			+ " ]";
	}

	public boolean isSolved() {
		return solved;
	}

	public void setSolved(boolean b) {
		solved = b;
	}

	public String getInstrumentNo() {	
		String inst = imageFileName.substring(0, imageFileName.lastIndexOf("."));
		inst = inst.replaceFirst("(?i)^patriots_[a-z0-9]+_", "");
		return inst;
	}

	public String getInstrumentNoYear() {
		if(imageFileName.matches("(?i)patriots_[a-z0-9]+_.*")){
			return getInstrumentNo();
		}
		if (imageFileName.lastIndexOf("_") > 0) {
			return imageFileName.substring(0, imageFileName.lastIndexOf("_"));
		}
		return "NA_INSTR_NO";
	}
	
	
	public boolean isDocInstrumentNo(String givenInstrNo){
		return imageFileName.startsWith(givenInstrNo);
	}

	public boolean isFakeLink() {
		return fakeLink;
	}

	public void setFakeLink(boolean b) {
		fakeLink = b;
	}

	public boolean equals(Object obj) {

		if (obj instanceof ImageLinkInPage) {
			return imageFileName.equals(
				((ImageLinkInPage) obj).getImageFileName());
		} else
			return false;
	}

	public int hashCode() {
		return imageFileName.hashCode();
	}

	public String getDownloadStatus() {
		return downloadStatus;
	}

	public void setDownloadStatus(String s) {
		downloadStatus = s;
	}
	
	public synchronized Object clone() {
	    
	    try {
	    
	        ImageLinkInPage newImageLinkInPage = (ImageLinkInPage) super.clone();
	        
	        newImageLinkInPage.link = new String(link);
	        newImageLinkInPage.solved = solved;
	        newImageLinkInPage.fakeLink = fakeLink;
	        newImageLinkInPage.imageFileName = new String(imageFileName);			
	        newImageLinkInPage.downloadStatus = new String(downloadStatus);
			
			return newImageLinkInPage;
		
	    } catch (CloneNotSupportedException cnse) {
	        throw new InternalError();
	    }
		
	}

	public void setImageFileName(String imageFileName) {
		this.imageFileName = imageFileName;
	}
	
	public ImageLinkInPage(ImageLinkInPage ilip){
		this.downloadStatus = ilip.downloadStatus;
		this.fakeLink = ilip.fakeLink;
		this.imageFileName  = ilip.imageFileName;
		this.link = ilip.link;
		this.solved = ilip.solved;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public boolean isJustImageLookUp() {
		return justImageLookUp;
	}

	public void setJustImageLookUp(boolean justImageLookUp) {
		this.justImageLookUp = justImageLookUp;
	}
	
	public String getSsfLink() {
		if(StringUtils.isEmpty(ssfLink)){
			ssfLink = "";
		}
		return ssfLink;
	}

	public void setSsfLink(String ssfLink) {
		this.ssfLink = ssfLink;
	}
}
