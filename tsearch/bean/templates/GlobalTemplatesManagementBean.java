package ro.cst.tsearch.bean.templates;
public class GlobalTemplatesManagementBean {
	String error = "";
	String fileName = "";
	String fileList = "";
	String action = "";
	String tempFile = "";
	String content = "";
	String searchId = "";
	
	public void setContent(String content){
		this.content = content;
	}
	
	public String getContent(){
		return content;
	}
	
	public void setTempFile(String tempFile){
		this.tempFile = tempFile;
	}
	
	public String getTempFile(){
		return tempFile;
	}
	
	public void setAction(String action){
		this.action = action;
	}
		
	public String getAction(){
		return action;
	}
	
	public void setFileName(String fileName){
		this.fileName = fileName;
	}
	
	public String getFileName(){
		return fileName;
	}
	
	public void setFileList(String s){
		fileList = s;
	}
	
	public String getFileList(){
		return fileList;
	}
	public void reset(){
		setFileList("");
		setTempFile("");
		setAction("");
		setFileName("");
		setError("");
		setContent("");
		setSearchId("");
	}
	public void setError(String e){
		error = e;
	}
	
	public String getError(){
		return error;
	}

	public String getSearchId() {
		return searchId;
	}

	public void setSearchId(String searchId) {
		this.searchId = searchId;
	}
}
