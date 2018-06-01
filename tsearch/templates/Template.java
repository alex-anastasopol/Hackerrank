package ro.cst.tsearch.templates;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.utils.FileUtils;

public class Template {
	long templateId;
	String templateName;
	String templateShortName;
	String templateLastUpdate;
	String templatePath;
	String templateContent;
	String templateFileSystemPath;
	//for backward compatibility with templates
	int templateComm;
	
	public Template(long id, String name, String shortName, String lastUpdate, String path,
					String content, String fsPath, int comm){
		templateId = id;
		templateName = name;
		templateShortName = shortName;
		templateLastUpdate = lastUpdate;
		templatePath = path;
		templateContent = content;
		templateFileSystemPath = fsPath;
		templateComm = comm;
	}
	
	public String getTemplateFileSystemPath(){
		return templateFileSystemPath;
	}
	
	public long getTemplateId(){
		return templateId;
	}
	
	public String getTemplateName(){
		return templateName;
	}
	
	public String getTemplateShortName(){
		return templateShortName;
	}

	public String getTemplateLastUpdate(){
		return templateLastUpdate;
	}
	
	public String getTemplatePath(){
		return templatePath;
	}
	
	public String getTemplateContent(){
		return templateContent;
	}
	
	public int getTemplateComm(){
		return templateComm;
	}
	
	public CommunityTemplatesMapper getTemplateAsCommunityTemplatesMapper(){
		CommunityTemplatesMapper ctm = new CommunityTemplatesMapper();
		ctm.setId(templateId);
		ctm.setCommunityId(templateComm);
		ctm.setShortName(templateShortName);
		ctm.setName(templateName);
		ctm.setLastUpdate(templateLastUpdate);
		ctm.setFileContent(templateContent);
		ctm.setPath(templatePath);
		return ctm;
	} 
	
	public void compile(){
		String extension = templatePath.substring(templatePath.lastIndexOf('.')+1,templatePath.length());
		boolean isDoc = AddDocsTemplates.docDocumentsExtensions.containsKey( extension.toLowerCase() );
		TemplateContents tc = null;
			
		try {
			if(isDoc) {
				
				if(templatePath.equals(templateName)) {
					tc = new OfficeDocumentContents(templateFileSystemPath);
				} else {
					tc = new OfficeDocumentContents(templatePath);
				}
			}else {
				tc = new StringBufferContents(templateContent);
			}
			AddDocsTemplates.addTempFilesNew(tc,
				UploadPolicyDoc.getGeneratedTemplateFileName(templateName, templateId, templateComm),
				UploadPolicyDoc.getJavaFile(templateComm, templateId),
				(int)templateId, FileUtils.getFileExtension(templateName).replaceAll("\\.", ""));
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			if(tc instanceof OfficeDocumentContents) {
				OfficeDocumentContents.closeOO(((OfficeDocumentContents)tc).getXComponent());
			}
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
