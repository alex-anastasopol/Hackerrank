package ro.cst.tsearch.templates;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.utils.FileFilterUtil;
import ro.cst.tsearch.utils.FileUtils;

public class GlobalTemplateFactory {
	private HashMap<String, Template> globalTemplates = null;
	static private GlobalTemplateFactory globalTemplateInstance = null;
	static private long maxId = 0;
	final public static int GLOBAL_COMMID = -3;
	final public static FileFilterUtil defaultFileFilter = new FileFilterUtil(new String[]{"xml", "doc", "html"});
	
	public static synchronized GlobalTemplateFactory getInstance(){
		if (globalTemplateInstance == null){
			maxId = 0;
			System.err.println("Creating new GlobalTemplateFactory");
			globalTemplateInstance = new GlobalTemplateFactory();
			globalTemplateInstance.readTemplatesFromSources(ServerConfig.getConnectionTemplatesPath(),
															GlobalTemplateFactory.defaultFileFilter);
		}
		return globalTemplateInstance;
	}
	
	
	public void removeAllTemplates(){
		globalTemplates = new HashMap<String, Template>();
		cleanCompiledTemplates();
	}
	
	public void cleanCompiledTemplates(){
		File templatePath = new File(UploadPolicyDoc.getTemplatesPath(GLOBAL_COMMID));
		FileFilterUtil ffu = new FileFilterUtil(defaultFileFilter);
		ffu.addAcceptedExtensions(new String[]{"java", "class"});
		File[] files = templatePath.listFiles();
		for(File i:files){
			i.delete();
		}
	}
	
	public void readTemplatesFromSources(String path, FileFilter fileFilter){
		if (globalTemplates == null){
			globalTemplates = new HashMap<String, Template>();
		}
		if (path != null){
			System.err.println("Reading global templates from " + path); 
			File templatesPath = new File(path);
			if (templatesPath.exists()){
				File[] files;
				if (fileFilter != null){
					files = templatesPath.listFiles(fileFilter);
				} else {
					files = templatesPath.listFiles();
				}
				for (int i=0; i<files.length; i++){
					if (!files[i].isDirectory()){
						readFileTemplate(files[i]);
					} else {
						System.err.println(">>>>>>>>>" + files[i].getAbsolutePath() + " global template is empty, skipping it<<<<");
					}
				}
			} else{
				System.err.println("I can't open " + path);
			}
		}

	}

	public void readFileTemplate(File file){
		Template template = getTemplateFromFile(file, getNextTemplateId());
		if (template != null){
			addTemplate(template);
			template.compile();
			System.err.println(">>>>>>>>>" + file.getAbsolutePath() + " global template was read from disk<<<<<<<");
		} else {
			System.err.println(">>>>>>>>>Error loading " + file.getAbsolutePath() + " global template from disk<<<<<<<");
		}

	}
	
	public long getNextTemplateId(){
		return ++maxId;
	}
	
	public Template getTemplateFromFile(File file, long id){
		String templateContent = FileUtils.readFile(file.getAbsolutePath());
		if (templateContent.length() > 0){
			Template template = new Template(id, 
									file.getName(),
									file.getName(),
									Long.toString(file.lastModified()),
									file.getName(),
									templateContent,
									file.getAbsolutePath(),
									GLOBAL_COMMID);
			return template;
		}
		return null;
	}
		
	public Template getTemplateByNameExact(String name){
		return globalTemplates.get(name);
	}
	
	public Template getTemplateByName(String name){
		Template template = globalTemplates.get(name);
		if (template != null){
			return template;
		}
		Iterator<String> i = globalTemplates.keySet().iterator();
		while (i.hasNext()){
			template = globalTemplates.get(i.next());
			if (template.getTemplateName().contains(name)){
				return template;
			}

		}
		return null;
	}
	
	public void removeTemplate(String templateName){
		Template template = getTemplateByNameExact(templateName);
		File fileSystemTemplate;
		if (template != null){
			globalTemplates.remove(templateName);
			if (template.getTemplateFileSystemPath().length() > 0){
				fileSystemTemplate = new File(template.getTemplateFileSystemPath());
				if (fileSystemTemplate.exists()){
					fileSystemTemplate.delete();
				}
			} else {
				fileSystemTemplate = new File(ServerConfig.getConnectionTemplatesPath() + File.separator + template.getTemplateName());
				fileSystemTemplate.delete();
			}
		}
	}
	
	//backward compatibility, use getTemplate* instead
	public List<CommunityTemplatesMapper> getTemplates(){
		List<CommunityTemplatesMapper> templates = new ArrayList<CommunityTemplatesMapper>();
		Template template;
		Iterator<String> templateKey = globalTemplates.keySet().iterator();
		while(templateKey.hasNext()){
			template = globalTemplates.get(templateKey.next());
			templates.add(template.getTemplateAsCommunityTemplatesMapper());
		}
		return templates;
	}
	
	public HashMap<String, Template> getTemplatesHash(){
		return globalTemplates;
	}
	
	public void addTemplate(Template template){
		if (template != null && template.getTemplateName() != null){
			globalTemplates.put(template.getTemplateName(), template);
		}
	}
	
 	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//GlobalTemplateFactory f = GlobalTemplateFactory.getInstance();
		//System.out.println("reading from disk");
	}

}
