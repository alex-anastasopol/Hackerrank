package ro.cst.tsearch.reports.throughputs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.ArrayUtils;

import ro.cst.tsearch.community.CategoryManager;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.tags.StatusSelect;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;

public class ThroughputBean {
	
	public final static String INVALID = "-1";
	public final static String OTHER = "-2";
	
	private String name = "Throughput Report";
	
	private boolean showGroups 			= true;
	private boolean showStates 			= true;
	private boolean showProducts 		= true;
	private boolean showCommunities 	= false;
	private boolean showAbstractors 	= false;
	private boolean showAgents 			= false;
	private boolean showCounties 		= false;
	
	private String selectGroups 		= INVALID;
	private String selectStates 		= INVALID;
	private String selectProducts 		= INVALID;
	private String selectCommunities 	= INVALID;
	private String selectAbstractors 	= INVALID;
	private String selectAgents 		= INVALID;
	private String selectCounties 		= INVALID;
		
	private String nameGroups			= "";
	private String nameStates			= "";
	private String nameCommunities		= "";
	
	private Vector<String> oldSelectGroups 		= null;
	private Vector<String> oldSelectStates 		= null;
	private Vector<String> oldSelectProducts 	= null;
	private Vector<String> oldSelectCommunities = null;
	private Vector<String> oldSelectAbstractors = null;
	private Vector<String> oldSelectAgents 		= null;
	private Vector<String> oldSelectCounties 	= null;
	
	private Vector<String> newSelectGroups 		= null;
	private Vector<String> newSelectStates 		= null;
	private Vector<String> newSelectProducts 	= null;
	private Vector<String> newSelectCommunities = null;
	private Vector<String> newSelectAbstractors = null;
	private Vector<String> newSelectAgents 		= null;
	private Vector<String> newSelectCounties 	= null;
	
	private String colorGroups 					= INVALID;
	private String colorCommunities 			= INVALID;
	private String colorProducts 				= INVALID;
	private String colorStates 					= INVALID;
	private String colorCounties 				= INVALID;
	private String colorAbstractors 			= INVALID;
	private String colorAgents 					= INVALID;
		
	private String lastPage = "";
	private String multiCommunities[]			= {INVALID};
	private String multiStates[]				= {INVALID};
	private String multiCounties[]				= {INVALID};
	private String multiProducts[]			    = {INVALID};
	private String multiAgents[]				= {INVALID};
	private String multiAbstractors[]			= {INVALID};
	private String multiCompaniesAgents[]		= {INVALID};
	private String multiStatus[]				= {INVALID};
	
	private String warningMessage				= null;
	
	
	private List<Map.Entry<Long, Long>> stateInfoMap = null;
	private List<Map.Entry<Long, Long>> countyInfoMap = null;
	private List<Map.Entry<Long, Long>> abstractorInfoMap = null;
	private List<Map.Entry<Long, Long>> agentInfoMap = null;
	private List<Map.Entry<Long, Long>> productInfoMap = null;
	

	public ThroughputBean(){
		oldSelectAbstractors 	= new Vector<String>();
		oldSelectAgents 		= new Vector<String>();
		oldSelectCommunities	= new Vector<String>();
		oldSelectCounties		= new Vector<String>();
		oldSelectGroups			= new Vector<String>();
		oldSelectProducts		= new Vector<String>();
		oldSelectStates			= new Vector<String>();
		
		newSelectAbstractors 	= new Vector<String>();
		newSelectAgents 		= new Vector<String>();
		newSelectCommunities	= new Vector<String>();
		newSelectCounties		= new Vector<String>();
		newSelectGroups			= new Vector<String>();
		newSelectProducts		= new Vector<String>();
		newSelectStates			= new Vector<String>();
		
		lastPage = URLMaping.REPORT_THROUGHPUT;
	}

	public String getColorAbstractors() {
		return colorAbstractors;
	}

	public void setColorAbstractors(String colorAbstractors) {
		this.colorAbstractors = colorAbstractors;
	}

	public String getColorAgents() {
		return colorAgents;
	}

	public void setColorAgents(String colorAgents) {
		this.colorAgents = colorAgents;
	}

	public String getColorCounties() {
		return colorCounties;
	}

	public void setColorCounties(String colorCounties) {
		this.colorCounties = colorCounties;
	}

	public String getColorProducts() {
		return colorProducts;
	}

	public void setColorProducts(String colorProducts) {
		this.colorProducts = colorProducts;
	}

	public String getColorStates() {
		return colorStates;
	}

	public void setColorStates(String colorStates) {
		this.colorStates = colorStates;
	}

	public String getColorCommunities() {
		return colorCommunities;
	}

	public void setColorCommunities(String colorCommunities) {
		this.colorCommunities = colorCommunities;
	}

	public String getColorGroups() {
		return colorGroups;
	}

	public void setColorGroups(String colorGroups) {
		this.colorGroups = colorGroups;
	}

	public Vector<String> getOldSelectAbstractors() {
		return oldSelectAbstractors;
	}

	public void setOldSelectAbstractors(Vector<String> oldSelectAbstractors) {
		this.oldSelectAbstractors = oldSelectAbstractors;
	}
	
	public Vector<String> getOldSelectAgents() {
		return oldSelectAgents;
	}

	public void setOldSelectAgents(Vector<String> oldSelectAgents) {
		this.oldSelectAgents = oldSelectAgents;
	}
	
	public Vector<String> getOldSelectCommunities() {
		return oldSelectCommunities;
	}

	public void setOldSelectCommunities(Vector<String> oldSelectCommunities) {
		this.oldSelectCommunities = oldSelectCommunities;
	}
	
	public Vector<String> getOldSelectCounties() {
		return oldSelectCounties;
	}

	public void setOldSelectCounties(Vector<String> oldSelectCounties) {
		this.oldSelectCounties = oldSelectCounties;
	}
	
	public Vector<String> getOldSelectGroups() {
		return oldSelectGroups;
	}

	public void setOldSelectGroups(Vector<String> oldSelectGroups) {
		this.oldSelectGroups = oldSelectGroups;
	}
	
	public Vector<String> getOldSelectProducts() {
		return oldSelectProducts;
	}

	public void setOldSelectProducts(Vector<String> oldSelectProducts) {
		this.oldSelectProducts = oldSelectProducts;
	}
	
	public Vector<String> getOldSelectStates() {
		return oldSelectStates;
	}

	public void setOldSelectStates(Vector<String> oldSelectStates) {
		this.oldSelectStates = oldSelectStates;
	}
	
	public String getSelectAbstractors() {
		return selectAbstractors;
	}

	public void setSelectAbstractors(String selectAbstractors) {
		this.selectAbstractors = selectAbstractors;
	}

	public String getSelectAgents() {
		return selectAgents;
	}

	public void setSelectAgents(String selectAgents) {
		this.selectAgents = selectAgents;
	}

	public String getSelectCommunities() {
		return selectCommunities;
	}

	public void setSelectCommunities(String selectCommunities) {
		this.selectCommunities = selectCommunities;
	}

	public String getSelectCounties() {
		return selectCounties;
	}

	public void setSelectCounties(String selectCounties) {
		this.selectCounties = selectCounties;
	}

	public String getSelectGroups() {
		return selectGroups;
	}

	public void setSelectGroups(String selectGroups) {
		this.selectGroups = selectGroups;
	}

	public String getSelectProducts() {
		return selectProducts;
	}

	public void setSelectProducts(String selectProducts) {
		this.selectProducts = selectProducts;
	}

	public String getSelectStates() {
		return selectStates;
	}

	public void setSelectStates(String selectStates) {
		this.selectStates = selectStates;
	}

	public boolean getShowAbstractors() {
		return showAbstractors;
	}

	public void setShowAbstractors(boolean showAbstractors) {
		this.showAbstractors = showAbstractors;
	}

	public boolean getShowAgents() {
		return showAgents;
	}

	public void setShowAgents(boolean showAgents) {
		this.showAgents = showAgents;
	}

	public boolean getShowCommunities() {
		return showCommunities;
	}

	public void setShowCommunities(boolean showCommunities) {
		this.showCommunities = showCommunities;
	}

	public boolean getShowCounties() {
		return showCounties;
	}

	public void setShowCounties(boolean showCounties) {
		this.showCounties = showCounties;
	}

	public boolean getShowGroups() {
		return showGroups;
	}

	public void setShowGroups(boolean showGroups) {
		this.showGroups = showGroups;
	}

	public boolean getShowProducts() {
		return showProducts;
	}

	public void setShowProducts(boolean showProducts) {
		this.showProducts = showProducts;
	}

	public boolean getShowStates() {
		return showStates;
	}

	public void setShowStates(boolean showStates) {
		this.showStates = showStates;
	}

	public Vector<String> getNewSelectAbstractors() {
		return newSelectAbstractors;
	}

	public void setNewSelectAbstractors(Vector<String> newSelectAbstractors) {
		this.newSelectAbstractors = newSelectAbstractors;
	}

	public Vector<String> getNewSelectAgents() {
		return newSelectAgents;
	}

	public void setNewSelectAgents(Vector<String> newSelectAgents) {
		this.newSelectAgents = newSelectAgents;
	}

	public Vector<String> getNewSelectCommunities() {
		return newSelectCommunities;
	}

	public void setNewSelectCommunities(Vector<String> newSelectCommunities) {
		this.newSelectCommunities = newSelectCommunities;
	}

	public Vector<String> getNewSelectCounties() {
		return newSelectCounties;
	}

	public void setNewSelectCounties(Vector<String> newSelectCounties) {
		this.newSelectCounties = newSelectCounties;
	}

	public Vector<String> getNewSelectGroups() {
		return newSelectGroups;
	}

	public void setNewSelectGroups(Vector<String> newSelectGroups) {
		this.newSelectGroups = newSelectGroups;
	}

	public Vector<String> getNewSelectProducts() {
		return newSelectProducts;
	}

	public void setNewSelectProducts(Vector<String> newSelectProducts) {
		this.newSelectProducts = newSelectProducts;
	}

	public Vector<String> getNewSelectStates() {
		return newSelectStates;
	}

	public void setNewSelectStates(Vector<String> newSelectStates) {
		this.newSelectStates = newSelectStates;
	}

	public String getNameCommunities() {
		return nameCommunities;
	}

	public void setNameCommunities(String nameCommunities) {
		this.nameCommunities = nameCommunities;
	}

	public String getNameGroups() {
		return nameGroups;
	}

	public void setNameGroups(String nameGroups) {
		this.nameGroups = nameGroups;
	}

	public String getNameStates() {
		return nameStates;
	}

	public void setNameStates(String nameStates) {
		this.nameStates = nameStates;
	}
	
	public String getLastPage() {
		return lastPage;
	}

	public void setLastPage(String lastPage) {
		this.lastPage = lastPage;
	}

	/**
	 * Returns an array containing the options from the Abstractor Select object<br>
	 * Can contain multiple values since it's a multiple select<br>
	 * If it contains <b>-1</b> it means the option <b>All Abstractors</b> is selected 
	 * @return an array containing the options from the Abstractor Select object
	 */
	public String[] getMultiAbstractors() {
		return multiAbstractors;
	}

	public void setMultiAbstractors(String[] multiAbstractors) {
		if(multiAbstractors==null){
			this.multiAbstractors = multiAbstractors;
			return;
		}
		this.multiAbstractors = new String[multiAbstractors.length];
		for (int i = 0; i < multiAbstractors.length; i++) {
			if(Integer.parseInt(multiAbstractors[i])<0)
				this.multiAbstractors[i]=INVALID;
			else
				this.multiAbstractors[i]=multiAbstractors[i];
		}
	}

	public String[] getMultiAgents() {
		return multiAgents;
	}

	public void setMultiAgents(String[] multiAgents) {
		if(multiAgents==null){
			this.multiAgents = multiAgents;
			return;
		}
		this.multiAgents = new String[multiAgents.length];
		for (int i = 0; i < multiAgents.length; i++) {
			if(Integer.parseInt(multiAgents[i])<0)
				this.multiAgents[i]=INVALID;
			else
				this.multiAgents[i]=multiAgents[i];
		}
	}

	public String[] getMultiCounties() {
		return multiCounties;
	}

	public void setMultiCounties(String[] multiCounties) {
		if(multiCounties==null){
			this.multiCounties = multiCounties;
			return;
		}
		this.multiCounties = new String[multiCounties.length];
		for (int i = 0; i < multiCounties.length; i++) {
			if(Integer.parseInt(multiCounties[i])<0)
				this.multiCounties[i]=INVALID;
			else
				this.multiCounties[i]=multiCounties[i];
		}
	}

	public String[] getMultiStates() {
		return multiStates;
	}

	public void setMultiStates(String[] multiStates) {
		if(multiStates==null){
			this.multiStates = multiStates;
			return;
		}
		if (multiStates.length == 1){
			multiStates = multiStates[0].split(",");
		}
		this.multiStates = new String[multiStates.length];
		for (int i = 0; i < multiStates.length; i++) {
			if(Integer.parseInt(multiStates[i])<0)
				this.multiStates[i]=INVALID;
			else
				this.multiStates[i]=multiStates[i];
		}
	}
	
	public String[] getMultiCommunities() {
		return multiCommunities;
	}

	public void setMultiCommunities(String[] multiArray) {
		if(multiArray==null){
			this.multiCommunities = multiArray;
			return;
		}
		if (multiArray.length == 1){
			multiArray = multiArray[0].split(",");
		}
		this.multiCommunities = new String[multiArray.length];
		for (int i = 0; i < multiArray.length; i++) {
			if(Integer.parseInt(multiArray[i])<0)
				this.multiCommunities[i]=INVALID;
			else
				this.multiCommunities[i]=multiArray[i];
		}
	}
	
	public String[] getMultiProducts() {
		return multiProducts;
	}

	public void setMultiProducts(String[] multiArray) {
		if(multiArray==null){
			this.multiProducts = multiArray;
			return;
		}
		if (multiArray.length == 1){
			multiArray = multiArray[0].split(",");
		}
		this.multiProducts = new String[multiArray.length];
		for (int i = 0; i < multiArray.length; i++) {
			if(Integer.parseInt(multiArray[i])<0)
				this.multiProducts[i]=INVALID;
			else
				this.multiProducts[i]=multiArray[i];
		}
	}
	

	public String[] getMultiCompaniesAgents() {
		return multiCompaniesAgents;
	}

	public void setMultiCompaniesAgents(String[] multiCompaniesAgents) {
		if(multiCompaniesAgents==null){
			this.multiCompaniesAgents = multiCompaniesAgents;
			return;
		}
		this.multiCompaniesAgents = new String[multiCompaniesAgents.length];
		for (int i = 0; i < multiCompaniesAgents.length; i++) {
			this.multiCompaniesAgents[i]=multiCompaniesAgents[i];
		}
		
	}

	/**
	 * @return the multiStatus
	 */
	public String[] getMultiStatus() {
		return multiStatus;
	}

	/**
	 * @param multiStatus the multiStatus to set
	 */
	public void setMultiStatus(String[] multiStatus) {
		if(multiStatus==null){
			this.multiStatus = multiStatus;
			return;
		}
		HashSet<String> allEntries = new HashSet<String>();
		for (int i = 0; i < multiStatus.length; i++) {
			allEntries.addAll(Arrays.asList(multiStatus[i].split(",")));
		}
		this.multiStatus = allEntries.toArray(new String[allEntries.size()]);
		
	}

	public String getWarningMessage() {
		return warningMessage;
	}

	public void setWarningMessage(String warningMessage) {
		this.warningMessage = warningMessage;
	}
	
	public void setAllowedCounties(UserAttributes userAttributes){
		if (StringUtils.indexOf(this.getMultiCounties(), "-1") >= 0){
			Vector<County> cl = userAttributes.getAllowedCountyList();
			if (cl != null && cl.size() > 0){
				String[] aa = new String[cl.size()];
				Iterator<County> icl = cl.iterator();
				int i = 0;
				while (icl.hasNext()){
					aa[i] = icl.next().getCountyId().toString();
					i++;
				}
	 			this.setMultiCounties(aa);
			}
		}
	}

	public List<Map.Entry<Long, Long>> getStateInfoMap() {
		return stateInfoMap;
	}

	public void setStateInfoMap(List<Map.Entry<Long, Long>> stateInfoMap) {
		this.stateInfoMap = stateInfoMap;
	}

	public List<Map.Entry<Long, Long>> getCountyInfoMap() {
		return countyInfoMap;
	}

	public void setCountyInfoMap(List<Map.Entry<Long, Long>> countyInfoMap) {
		this.countyInfoMap = countyInfoMap;
	}

	public List<Map.Entry<Long, Long>> getAbstractorInfoMap() {
		return abstractorInfoMap;
	}

	public void setAbstractorInfoMap(List<Map.Entry<Long, Long>> abstractorInfoMap) {
		this.abstractorInfoMap = abstractorInfoMap;
	}

	public List<Map.Entry<Long, Long>> getAgentInfoMap() {
		return agentInfoMap;
	}

	public void setAgentInfoMap(List<Map.Entry<Long, Long>> agentInfoMap) {
		this.agentInfoMap = agentInfoMap;
	}

	public List<Map.Entry<Long, Long>> getProductInfoMap() {
		return productInfoMap;
	}

	public void setProductInfoMap(List<Map.Entry<Long, Long>> productInfoMap) {
		this.productInfoMap = productInfoMap;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSelectionsFullStatus() {
		StringBuilder sb = new StringBuilder();
		if( INVALID.equals(getSelectGroups())) {
			sb.append("All Groups\n");
		} else {
			
			String name = getNameGroups();
			if(StringUtils.isEmpty(name)) {
				try {
					name = CategoryManager.getCategory(Long.parseLong(getSelectGroups())).getNAME();
				} catch (BaseException e) {
					e.printStackTrace();
				}
			}
			
			
			sb.append("Group: ").append(name).append("\n");
		}
		if(!INVALID.equals(getSelectCommunities())) {
			String name = getNameCommunities();
			if(StringUtils.isEmpty(name)) {
				try {
					name = CommunityManager.getCommunity(Long.parseLong(getSelectCommunities())).getNAME();
				} catch (BaseException e) {
					e.printStackTrace();
				}
			}
			sb.append("Communities: ").append(name).append("\n");
		} else {
			String[] strArray = getMultiCommunities();
			if (ArrayUtils.contains(strArray, "-1")) {
				sb.append("All Communities\n");
			} else {
				sb.append("Communities: ");
				try {
					sb.append(CommunityManager.getCommunity(Long.parseLong(strArray[0])).getNAME());
					for (int i = 1; i < strArray.length; i++) {
						sb.append(", ").append(CommunityManager.getCommunity(Long.parseLong(strArray[i])).getNAME());	
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				sb.append("\n");
			} 
		}
		
		String[] strArray = getMultiStates();
		if((strArray == null || strArray.length == 0 || ArrayUtils.contains(strArray, "-1")) 
				&& (INVALID.equals(getSelectStates()) || OTHER.equals(getSelectStates()))) {
			sb.append("All States\n");
		} else {
			sb.append("States: ");
			
			if(INVALID.equals(getSelectStates()) || OTHER.equals(getSelectStates())) {
			
				try {
					sb.append(State.getState(Integer.parseInt(strArray[0])).getStateAbv());
					for (int i = 1; i < strArray.length; i++) {
						sb.append(", ").append(State.getState(Integer.parseInt(strArray[i])).getStateAbv());	
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					sb.append(State.getState(Integer.parseInt(getSelectStates())).getStateAbv());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sb.append("\n");
		}
		
		// counties
		strArray = getMultiCounties();
		if((strArray == null || strArray.length == 0 || ArrayUtils.contains(strArray, "-1")) 
				&& (INVALID.equals(getSelectCounties()) || INVALID.equals(getSelectCounties()))) {
			sb.append("All Counties\n");
		} else {
			sb.append("Counties: ");
			
			if(INVALID.equals(getSelectCounties()) || OTHER.equals(getSelectCounties())) {
			
				try {
					sb.append(StateCountyManager.getInstance().getSTCounty(Integer.parseInt(strArray[0])));
					for (int i = 1; i < strArray.length; i++) {
						sb.append(", ").append(StateCountyManager.getInstance().getSTCounty(Integer.parseInt(strArray[i])));	
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					sb.append(StateCountyManager.getInstance().getSTCounty(Integer.parseInt(getSelectCounties())));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sb.append("\n");
		}
		
		// abstractors
		strArray = getMultiAbstractors();
		if((strArray == null || strArray.length == 0 || ArrayUtils.contains(strArray, "-1")) 
				&& (INVALID.equals(getSelectAbstractors()) || OTHER.equals(getSelectAbstractors()))) {
			sb.append("All Abstractors\n");
		} else {
			sb.append("Abstractors: ");
			
			UserManagerI userManager = UserManager.getInstance();
			if(INVALID.equals(getSelectAbstractors()) || OTHER.equals(getSelectAbstractors())) {
				try {
					userManager.getAccess();
					UserI user = userManager.getUser(Long.parseLong(strArray[0]));
					sb.append(user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());
					for (int i = 1; i < strArray.length; i++) {
						user = userManager.getUser(Long.parseLong(strArray[i]));
						sb.append(", ").append(user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());	
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					userManager.releaseAccess();
				}
			} else {
				try {
					userManager.getAccess();
					UserI user = userManager.getUser(Long.parseLong(getSelectAbstractors()));
					sb.append(user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					userManager.releaseAccess();
				}
			}
			sb.append("\n");
		}
		
		// agents
		strArray = getMultiAgents();
		if((strArray == null || strArray.length == 0 || ArrayUtils.contains(strArray, "-1")) 
				&& (INVALID.equals(getSelectAgents()) || OTHER.equals(getSelectAgents()))) {
			sb.append("All Agents\n");
		} else {
			sb.append("Agents: ");
			UserManagerI userManager = UserManager.getInstance();
			if(INVALID.equals(getSelectAgents()) || OTHER.equals(getSelectAgents())) {
				try {
					userManager.getAccess();
					UserI user = userManager.getUser(Long.parseLong(strArray[0]));
					sb.append(user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());
					for (int i = 1; i < strArray.length; i++) {
						user = userManager.getUser(Long.parseLong(strArray[i]));
						sb.append(", ").append(user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());	
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					userManager.releaseAccess();
				}
			} else {
				try {
					userManager.getAccess();
					UserI user = userManager.getUser(Long.parseLong(getSelectAgents()));
					sb.append(user.getFirstName() + " " + user.getLastName() + " - " + user.getUserName());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					userManager.releaseAccess();
				}
			}
			sb.append("\n");
		}
		
		// statuses
		strArray = getMultiStatus();
		if(strArray == null || strArray.length == 0 || ArrayUtils.contains(strArray, "-1")) {
			sb.append("All Statuses\n");
		} else {
			sb.append("Status: ");
			Map<Integer, String> allStatusesMapById = StatusSelect.getAllStatusesMapById();
			try {
				sb.append(allStatusesMapById.get(Integer.parseInt(strArray[0])));
				for (int i = 1; i < strArray.length; i++) {
					sb.append(", ").append(allStatusesMapById.get(Integer.parseInt(strArray[i])));	
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			sb.append("\n");
		}
		
		
		return sb.toString();
	}	

}
