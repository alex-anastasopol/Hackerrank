package ro.cst.tsearch.titledocument.abstracts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;

@Deprecated
public class FidelityTSD  {

	@SuppressWarnings("unused")
	private transient String PathLevelTwo = "";
	@SuppressWarnings("unused")
	private transient String mImagesPath = "";
	@SuppressWarnings("unused")
	private transient long searchId=-1;
	
	
	public FidelityTSD(StringBuffer sCOMFileUnused,long searchId) throws TSDException {
		this.searchId = searchId;
	}
	
	public static void writeLogo(long searchId) {
		try{
			byte[] logo = InstanceManager.getManager().getCurrentInstance(searchId)
					.getCurrentCommunity().getLOGO();
			String searchPath = InstanceManager.getManager().getCurrentInstance(searchId)
					.getCrtSearchContext().getSearchDir();
			searchPath += File.separator + "Title Document";
			
			FileUtils.createDirectory(searchPath);
			
			if (!FileUtils.existPath(searchPath + File.separator + "logo.gif")){
				FileOutputStream out;
				try {
					out = new FileOutputStream(searchPath + File.separator + "logo.gif");
					out.write(logo);
					out.flush();
					out.close();
		
				} catch (FileNotFoundException e) {
					e.printStackTrace();
		
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public static CommunityTemplatesMapper getTemplateObject(String templateName,
			List<CommunityTemplatesMapper> userTemplates) {
		if (templateName != null && userTemplates != null) {
			for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
				if(templateName.equals(communityTemplatesMapper.getPath()))
					return communityTemplatesMapper;
			}
			
		}
		return null;
	}

	// return true if agent has TSD template
	public static boolean containsTSDTemplate(List<CommunityTemplatesMapper> userTemplates) {
		if (userTemplates != null) {
			for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
				String test = communityTemplatesMapper.getPath();
				if (test == null) {
					test = "";
				}
				if (test.contains(TemplatesInitUtils.TEMPLATE_TSD_START)) {
					return true;
				}				
			}
		}
		return false;
	}

	// return the TSD template entry
	public static CommunityTemplatesMapper getTSDTemplate(List<CommunityTemplatesMapper> userTemplates) {
		if (userTemplates != null) {
			for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
				String test = communityTemplatesMapper.getPath();
				if (test == null) {
					test = "";
				}
				if (test.contains(TemplatesInitUtils.TEMPLATE_TSD_START)) {
					return communityTemplatesMapper;
				}
			}
		}
		return null;
	}

	
}