package ro.cst.tsearch.servers.parentsite;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.parentsite.ModuleCommentWrapper.TYPE;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StringUtils;

public class ModuleWrapperManager {
	
	private static final Pattern infoLinePattern = 
		Pattern.compile("\\([^\\)]+\\)key_(\\d+)=(.*)");
	private static final Pattern infoModuleLinePattern = 
		Pattern.compile("(?:\\([^\\)]+\\))?\\s*module\\s*_\\s*(\\d+)\\s*");
	private static final Pattern infoComboPattern = 
			Pattern.compile("\\([^\\)]+\\)combo_(\\d+)=(.*)");
	private static final Pattern infoHtmlStringPattern = 
			Pattern.compile("\\([^\\)]+\\)htmlString_(\\d+)=(.*)");
	
	private Map<String, ModuleCommentWrapper> infoWrapper;
	
	private ModuleWrapperManager(){
		
		infoWrapper = new HashMap<String, ModuleCommentWrapper>();
		loadInformation(
				Integer.toString(GWTDataSite.AO_TYPE),    //AO
				Integer.toString(GWTDataSite.NB_TYPE),	  //NB
				Integer.toString(GWTDataSite.PRI_TYPE),   //PRI
				Integer.toString(GWTDataSite.TR2_TYPE),   //TR2
				Integer.toString(GWTDataSite.PF_TYPE), 	  //PF
				Integer.toString(GWTDataSite.TS_TYPE), 	  //TS
				Integer.toString(GWTDataSite.PI_TYPE),    //PI
				Integer.toString(GWTDataSite.DG_TYPE),    //DTG
				Integer.toString(GWTDataSite.R2_TYPE),    //RO2
				Integer.toString(GWTDataSite.ADI_TYPE),  //TAD
				Integer.toString(GWTDataSite.SRC_TYPE),  //SRC
				Integer.toString(GWTDataSite.MERS_TYPE),  //MERS
				Integer.toString(GWTDataSite.RO_TYPE));   //RO
	}
	private void loadInformation(String ... sites) {
		for (int i = 0; i < sites.length; i++) {
			loadInformation(sites[i]);
		}
		
	}
	private void loadInformation(String siteType) {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		
		RandomAccessFile raf = null;
		
		try {
			
			raf = new RandomAccessFile(
					new File(folderPath + File.separator + siteType + ".txt"), "r");
			String line = null;
			String currentKey = null;
			Integer currentModuleId = 0; 
			Matcher matcher = null;
			while ((line = raf.readLine()) != null) {
				if(StringUtils.isNotEmpty(line)) {
					matcher = infoLinePattern.matcher(line);
					if(matcher.find()) {
						Integer functionKey = new Integer(matcher.group(1).trim());
						String functionComment = matcher.group(2).trim();
						String tempKey = currentKey + siteType;
						ModuleCommentWrapper moduleCommentWrapper = infoWrapper.get(tempKey);
						if(moduleCommentWrapper == null) {
							moduleCommentWrapper = new ModuleCommentWrapper();
							infoWrapper.put(tempKey, moduleCommentWrapper);
						}
						moduleCommentWrapper.setValue(currentModuleId, functionKey, functionComment);

					} else {
						matcher = infoComboPattern.matcher(line);
						if (matcher.find()) {
							Integer functionKey = new Integer(matcher.group(1).trim());
							String functionComment = matcher.group(2).trim();
							String tempKey = currentKey + siteType;
							ModuleCommentWrapper moduleCommentWrapper = infoWrapper.get(tempKey);
							if (moduleCommentWrapper == null) {
								moduleCommentWrapper = new ModuleCommentWrapper();
								infoWrapper.put(tempKey, moduleCommentWrapper);
							}
							moduleCommentWrapper.setComboValue(currentModuleId, functionKey, functionComment);

						} else{
							matcher = infoHtmlStringPattern.matcher(line);
							if (matcher.find()) {
								Integer functionKey = new Integer(matcher.group(1).trim());
								String functionComment = matcher.group(2).trim();
								String tempKey = currentKey + siteType;
								ModuleCommentWrapper moduleCommentWrapper = infoWrapper.get(tempKey);
								if (moduleCommentWrapper == null) {
									moduleCommentWrapper = new ModuleCommentWrapper();
									infoWrapper.put(tempKey, moduleCommentWrapper);
								}
								moduleCommentWrapper.setHtmlStringValue(currentModuleId, functionKey, functionComment);
							} else{
								matcher = infoModuleLinePattern.matcher(line);
								if(matcher.find()) {
									currentModuleId = new Integer(matcher.group(1).trim());
								} else {
									currentKey = line.trim();
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			if(raf!=null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * siteKey is formed from state abbreviation + county name + site type like TNShelby25
	 * @param siteKey
	 * @param moduleIndex
	 * @param functionIndex
	 * @return
	 */
	public String getCommentForSiteAndFunction(String siteKey, int moduleIndex, int functionIndex) {
		ModuleCommentWrapper commentWrapper = infoWrapper.get(siteKey);
		if(commentWrapper != null) {
			return commentWrapper.getValueForModuleAndFunction(moduleIndex, functionIndex, TYPE.COMMENT.getValue());
		}
		return null;
	}
	
	public String getComboValuesForSiteAndFunction(String siteKey, int moduleIndex, int functionIndex) {
		ModuleCommentWrapper comboWrapper = infoWrapper.get(siteKey);
		if (comboWrapper != null) {
			return comboWrapper.getValueForModuleAndFunction(moduleIndex, functionIndex, TYPE.COMBO_VALUE.getValue());
		}
		return null;
	}
	
	public String getHtmlStringForSiteAndFunction(String siteKey, int moduleIndex, int functionIndex) {
		ModuleCommentWrapper htmlStringWrapper = infoWrapper.get(siteKey);
		if (htmlStringWrapper != null) {
			return htmlStringWrapper.getValueForModuleAndFunction(moduleIndex, functionIndex, TYPE.HTML_STRING.getValue());
		}
		return null;
	}
	
	private static class SingletonHolder {
		private static ModuleWrapperManager instance = new ModuleWrapperManager();
	}
	
	public static ModuleWrapperManager getInstance(){
		return SingletonHolder.instance;
	}
	
	public static void main(String[] args) {
		ModuleWrapperManager.getInstance();
		
	}
}
