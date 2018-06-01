package ro.cst.tsearch.servers.bean;

import java.util.LinkedList;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.URLMaping;

public class SearchManager {

	public static final int idText = TSServerInfoFunction.idTEXT,
		idDate = TSServerInfoFunction.idDate,
		idRadioBotton = TSServerInfoFunction.idRadioBotton,
		idSingleCombo = TSServerInfoFunction.idSingleselectcombo,
		idCheckbox = TSServerInfoFunction.idCheckBox,
		idStModule = TSServerInfoModule.idStModule,
		idFilterModule = TSServerInfoModule.idFilterModule;
	public int miServerID;
	public TSServerInfo currentServerInfo;
	private int miFilterModuleCount = 0;
	private int[] maiStModules;
	private int[] maiFltrModules;
	public final static int MAX_MODULES_ON_A_ROW = 2;
	
	public final static String ALL_ADMIN 	= "alladmin";
	public final static String ALL 		= "all";
	public final static String COMMADMIN 	= "comadmin";
	public final static String TSCADMIN 	= "tscadmin";
	public final static String TSADMIN	 	= "tsadmin";

	public boolean IsTSDEmpty(String SessionID, String sSitePath) {

		/*boolean bRtrn= true;
		String[] asDirsList=null;
		File tmpFile=new File(TSDManager.GetDirPath( sSitePath , SessionID));
		if (tmpFile.exists()) 
		{
			asDirsList=tmpFile.list();
			if (asDirsList !=null && asDirsList.length!=0)
			{
				bRtrn=false;
			}
		}
		return bRtrn;*/
		return false;
	}
	private int getModuleIndex(int iModuleType, int moduleIndex) {
		if (iModuleType == idStModule)
			return maiStModules[moduleIndex];
		else
			return maiFltrModules[moduleIndex];
	}
	
	public int getModuleCount(int iModuleType) {
		if (iModuleType == idStModule)
			return (currentServerInfo.getModuleCount() - miFilterModuleCount);
		else
			return miFilterModuleCount;
	}
	public String getModuleTitle(int iModuleType, int moduleIndex) {
		
	    String moduleName = currentServerInfo.getModule(getModuleIndex(iModuleType, moduleIndex)).getName();
	    if(moduleName.indexOf("Browse") != -1)
	        return moduleName;
	    if (currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getMouleType()
			== idStModule)
			return "Search by "
				+ moduleName;
		return moduleName;
	}

	public int getModuleFunctionCount(int iModuleType, int moduleIndex) {
		return currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getFunctionCount();
	}

	public String getFunctionName(
		int iModuleType,
		int moduleIndex,
		int functionIndex) {
		return currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getFunction(functionIndex)
			.getName();
	}
	public boolean isFunctionParameterHiden(
		int iModuleType,
		int moduleIndex,
		int functionIndex) {
		return currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getFunction(functionIndex)
			.isHiden();
	}
	public String getFunctionParameterName(
		int iModuleType,
		int moduleIndex,
		int functionIndex) {
		return currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getFunction(functionIndex)
			.getParamAlias();
	}
	public String getFunctionHtmlformat(
		int iModuleType,
		int moduleIndex,
		int functionIndex) {
		String ht =
			currentServerInfo
				.getModule(getModuleIndex(iModuleType, moduleIndex))
				.getFunction(functionIndex)
				.getHtmlformat();
		ht =
			ht.replaceFirst(
				currentServerInfo
					.getModule(getModuleIndex(iModuleType, moduleIndex))
					.getFunction(functionIndex)
					.getParamName(),
				currentServerInfo
					.getModule(getModuleIndex(iModuleType, moduleIndex))
					.getFunction(functionIndex)
					.getParamAlias());
		return ht;
	}
	public int getFunctionParameterType(
		int iModuleType,
		int moduleIndex,
		int functionIndex) {
		return currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getFunction(functionIndex)
			.getParamType();
	}
	/**
	 * Method getFunctionParameterDefaultValue.
	 * @param iModuleType
	 * @param i
	 * @param j
	 * @return String
	 */
	private String getFunctionParameterDefaultValue(
		int iModuleType,
		int moduleIndex,
		int functionIndex) {
		return currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getFunction(functionIndex)
			.getDefaultValue();
	}
	private String getFunctionParameterValue(
		int iModuleType,
		int moduleIndex,
		int functionIndex) {
		return currentServerInfo
			.getModule(getModuleIndex(iModuleType, moduleIndex))
			.getFunction(functionIndex)
			.getValue();
	}

	public String getServerLink() {
		return currentServerInfo.getServerLink();
	}

	/**
	 * Sets the serverIndex.
	 * @param serverIndex The serverIndex to set
	 */
	public void setServerInfo(int serverIndex, int serverType,long searchId)
		throws BaseException {
		miServerID =
			HashCountyToIndex.getServerFactoryID(serverIndex, serverType);
		currentServerInfo = TSServersFactory.GetServerInfo(miServerID,searchId);

		if (currentServerInfo.getModuleCount() != 0) {
			maiStModules =
				currentServerInfo.getModulesIdxs(TSServerInfoModule.idStModule);
			maiFltrModules =
				currentServerInfo.getModulesIdxs(
					TSServerInfoModule.idFilterModule);
		}
		if(maiFltrModules != null) {
			miFilterModuleCount = maiFltrModules.length;
		} else {
			miFilterModuleCount = 0;
		}
	}
	
	public static String getCSSForLayout() {
		String cssLink = URLMaping.PS_CSS_LAYOUT;
		String cssTag = "<link rel=\"stylesheet\" type=\"text/css\"href=\"" + cssLink + "\" />";
		return cssTag;
	}
	
	
	public String getModules(int iModuleType, String sFormName, User currentUser){
		String sTmp = "";
		String sDefaultValue;
		int t;

		UserAttributes ua = currentUser.getUserAttributes();
		
		// populate the "formIndex" attribute. This holds the id for mapping to the right module when making the request from parent site		
		for (int i = 0; i < getModuleCount(iModuleType); i++) {
			TSServerInfoModule currentModule = currentServerInfo.getModule(getModuleIndex(iModuleType, i));
			if (currentModule.getModuleParentSiteLayout() != null) {
				currentModule.getModuleParentSiteLayout().setFormIndex( getModuleIndex(iModuleType, i));
			}
		}
		
		TSServerInfoModule[] modulesToPrintInOrder = setModulesInOrder(iModuleType);
		
		for (int i = 0; i < modulesToPrintInOrder.length; i++) {
			
			TSServerInfoModule currentModule = modulesToPrintInOrder[i];
			
			if (ua.isAdmin() && !currentModule.isVisible() && ALL_ADMIN.equals(currentModule.getVisibleFor())){
				currentModule.setVisible(true);
			}
			
			//don't show the "invisible" modules
			if (!currentModule.isVisible()) {
				continue;
			}
			
			
			HTMLObject moduleLayout = currentModule.getModuleParentSiteLayout();
			
			if (moduleLayout != null) { // xml defined parent sites
				try {
					sTmp += "<tr><td width='100%' height='100%'>";
					sTmp += moduleLayout.render();
					sTmp += "</td></tr>";
				}
				catch (FormatException e)
				{
					e.printStackTrace();
				}
			}
			else // java defined parent sites
			{
				//open new row
				if ((i % MAX_MODULES_ON_A_ROW) == 0)
					sTmp += "<tr>";
				//open new table and add in the first row of this table module title
				sTmp += "<td valign=\"top\" >"
					+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"3\"  align=\"center\" style=\"BORDER-COLLAPSE: collapse\" bordercolor=\"#98A8AF\">"
					+ "<tr align=\"center\">"
					+ "<td colspan=\"4\" align=\"left\" nowrap class=\"trpurplenews\">"
					+ getModuleTitle(iModuleType, i)
					+ "</td></tr>";
				//for each visible function add a new row and add input control
				for (int j = 0; j < getModuleFunctionCount(iModuleType, i); j++)
					if (!isFunctionParameterHiden(iModuleType, i, j)) {
						if ((getFunctionParameterType(iModuleType, i, j)
							!= idRadioBotton)
							|| ((j % 2) == 0))
							sTmp += "<tr>";
	
						sTmp += "<td nowrap class=\"trteen\">"
							+ getFunctionName(iModuleType, i, j)
							+ "</td><td width=\"80%\" align=\"left\" class=\"trnocolours\">"
							+ "<FONT size=-2>";
						if (getFunctionParameterType(iModuleType, i, j)
							== idDate) {
							sDefaultValue =
								getFunctionParameterDefaultValue(iModuleType, i, j);
							if (sDefaultValue.length() != 0) {
								sTmp
									+= CSTCalendar.getDateControl(
										"MDY",
										getFunctionParameterName(iModuleType, i, j),
										CSTCalendar.getInitDateFromString(
											sDefaultValue,
											"MDY"),
										null,
										CSTCalendar.getDefaultInitDate("MDY"),
										sFormName);
							} else {
								sTmp
									+= CSTCalendar.getDateControl(
										"MDY",
										getFunctionParameterName(iModuleType, i, j),
										null,
										null,
										CSTCalendar.getDefaultInitDate("MDY"),
										sFormName);
							}
						} else if (
							getFunctionParameterType(iModuleType, i, j)
								== idRadioBotton) {
							sDefaultValue =
								getFunctionParameterDefaultValue(iModuleType, i, j);
							if (sDefaultValue.length() != 0)
								sTmp += "<INPUT TYPE=\"Radio\" name=\""
									+ getFunctionParameterName(iModuleType, i, j)
									+ "\" Value=\""
									+ getFunctionParameterValue(iModuleType, i, j)
									+ "\" Checked>";
							else
								sTmp += "<INPUT TYPE=\"Radio\" name=\""
									+ getFunctionParameterName(iModuleType, i, j)
									+ "\" Value=\""
									+ getFunctionParameterValue(iModuleType, i, j)
									+ "\">";
						} else if (
							getFunctionParameterType(iModuleType, i, j)
								== idSingleCombo) {
							sTmp += getFunctionHtmlformat(iModuleType, i, j);
						}
						else if (
								getFunctionParameterType(iModuleType, i, j)
									== idCheckbox) {
								sTmp += getFunctionHtmlformat(iModuleType, i, j);
							}
						else
							sTmp
								+= "<INPUT size=\"40\" style=\"width:100%\" name=\""
								+ getFunctionParameterName(iModuleType, i, j)
								+ "\" Value=\""
								+ getFunctionParameterDefaultValue(iModuleType, i, j)
								+ "\">";
						sTmp += "</FONT>" + "</td>";
						if ((getFunctionParameterType(iModuleType, i, j)
							!= idRadioBotton)
							|| (((j + 1) % MAX_MODULES_ON_A_ROW) == 0))
							sTmp += "</tr>";
					}
				//add buttons on last row of the table if is not filter module
				if (iModuleType != idFilterModule) {
					sTmp += "<tr>"
						+ "<td nowrap class=\"trteen\">&nbsp; </td>"
						+ "<td width=\"80%\" align=\"right\" class=\"trteen\">"
						+ "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"Submit\" onClick=\"submitForm("
						+ getModuleIndex(iModuleType, i)
						+ ");\">";
					if(getModuleTitle(iModuleType, i).indexOf("Browse") == -1)
					    sTmp += "<input type=\"reset\" class=\"button\" name=\"Clear form\" value=\"Clear form\">";
					else 
					    sTmp += "</td></tr>";
				}
				sTmp += "</table></td>";
				//if we finished a row then close it
				if (((i + 1) % MAX_MODULES_ON_A_ROW) == 0)
					sTmp += "</tr>";
				else if (i == getModuleCount(iModuleType) - 1) {
					//if we did not finished a row but we finished the modules then fill in the table with empty cells
					for (t = (i % MAX_MODULES_ON_A_ROW) + 1;
						t < MAX_MODULES_ON_A_ROW;
						t++)
						sTmp += "<td>&nbsp;</td>";
					sTmp += "</tr>";
				}
			}
		}
		return sTmp;
	}
	
	private TSServerInfoModule[] setModulesInOrder(int iModuleType) {
		int moduleCount = getModuleCount(iModuleType);
		TSServerInfoModule[] modulesToPrintInOrder = new TSServerInfoModule[moduleCount];

		LinkedList<TSServerInfoModule> unsortedModules = new LinkedList<TSServerInfoModule>();

		for (int i = 0; i < moduleCount; i++) {

			// get the module by the default sort
			TSServerInfoModule currentModule = currentServerInfo.getModule(getModuleIndex(iModuleType, i));

			if (currentModule.getModuleOrder() >= 0) {
				modulesToPrintInOrder[currentModule.getModuleOrder()] = currentModule;
			} else {
				// modulesToPrintInOrder[i]=currentModule;
				unsortedModules.add(currentModule);
			}
		}

		// fill in the blanks in the default order
		int j = 0;
		for (int i = 0; i < modulesToPrintInOrder.length; i++) {
			TSServerInfoModule currentModule = modulesToPrintInOrder[i];
			if (currentModule == null) {
				modulesToPrintInOrder[i] = unsortedModules.get(j);
				j++;
			}
		}

		return modulesToPrintInOrder;
	}

}