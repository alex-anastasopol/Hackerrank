package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;


/**
 * This class should be used for All AOLike and TaxLike(county tax or city tax) sites that allows multiple years searches<br>
 * 
 * @author mihaib
 *
 */
public abstract class TSServerAssessorandTaxLike extends TSServer implements TSServerAssessorAndTaxLikeI{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//protected int numberOfYearsAllowed = 0;
	
	public TSServerAssessorandTaxLike(long searchId){
		super(searchId);
		if (numberOfYearsAllowed > 1){
			resultType = MULTIPLE_RESULT_TYPE;
		}
//		try {
//			numberOfYearsAllowed = dataSite.getNumberOfYears();
//					//HashCountyToIndex.getDataSite(getCommunityId(), Integer.parseInt(getSearch().getP1()), Integer.parseInt(getSearch().getP2())).getNumberOfYears();
//		} catch (NumberFormatException e) {
//			e.printStackTrace();
//		} /*catch (BaseException e) {
//			e.printStackTrace();
//		}*/
	}

	public TSServerAssessorandTaxLike(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int miServerID){
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		if (numberOfYearsAllowed > 1){
			resultType = MULTIPLE_RESULT_TYPE;
		}
//		try {
//			numberOfYearsAllowed = dataSite.getNumberOfYears();
//					//HashCountyToIndex.getDataSite(getCommunityId(), Integer.parseInt(getSearch().getP1()), Integer.parseInt(getSearch().getP2())).getNumberOfYears();
//		} catch (NumberFormatException e) {
//			e.printStackTrace();
//		} /*catch (BaseException e) {
//			e.printStackTrace();
//		}*/
	}
	
//	public List<TSServerInfoModule> multiplyModule(TSServerInfoModule module, List<TSServerInfoModule> modules){
//		if (module.getMultipleYears()){
//			if(module != null) {
//				
//				List<TSServerInfoFunction> functionList = module.getFunctionList();
//				String year = "";
//				int functionIndex = 0;
//
//				for (Iterator iterator = functionList.iterator(); iterator.hasNext();) {
//					TSServerInfoFunction tsServerInfoFunction = (TSServerInfoFunction) iterator.next();
//					
//					functionIndex++;
//					if (SearchAttributes.CURRENT_TAX_YEAR.equals(tsServerInfoFunction.getSaKey())){
//						year = tsServerInfoFunction.getDefaultValue();						
//						break;
//					}
//				}
//				
//				if (StringUtils.isNotEmpty(year)){
//					int intYear = Integer.parseInt(year);
//					for (int i = 1; i < numberOfYearsAllowed; i++){
//						intYear--;
//						TSServerInfoModule newModule = new TSServerInfoModule(module);
//						newModule.getFunction(functionIndex - 1).setDefaultValue(Integer.toString(intYear));
//						modules.add(newModule);
//					}
//				}
//			}
//		}
//		
//		return modules;
//	}
	
	protected int getCurrentTaxYear(){
		
		try {
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			Calendar cal = Calendar.getInstance();
			if (dataSite != null) {
				cal.setTime(dataSite.getPayDate());
			}
			return cal.get(Calendar.YEAR);
		} catch (Exception e) {
			return -1;
		}
	}
	
	@Override
	public boolean lastAnalysisBeforeSaving(ServerResponse serverResponse){
		
		List<Integer> listWithYears = new ArrayList<Integer>();
		
		Vector<ParsedResponse> rows = serverResponse.getParsedResponse().getResultRows();
		for (ParsedResponse parsedResponse : rows) {
			int year = parsedResponse.getTaxYear();
			listWithYears.add(year);
		}
		
		if (listWithYears.size() > 0){
			Set<Integer> checkForDuplicates = new HashSet<Integer>(listWithYears);
			if (listWithYears.size() > checkForDuplicates.size()){
				return true;
			}

		}
		
		return false;
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr){
		if (numberOfYearsAllowed > 1){
			if (sr.getParsedResponse().getResultsCount() == numberOfYearsAllowed){
				return false;
			} else{
				numberOfYearsAllowed--;
				return true;
			}
		} else{
			return false;
		}
	} 
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten) {
		
		ADD_DOCUMENT_RESULT_TYPES result = super.addDocumentInATS(response, htmlContent, forceOverritten);
		
		if (result == ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS){
			result = ADD_DOCUMENT_RESULT_TYPES.OVERWRITTEN;
			
			DocumentsManagerI manager = mSearch.getDocManager();
	    	ParsedResponse pr = response.getParsedResponse();
			DocumentI doc = pr.getDocument();
	        try{
	        	manager.getAccess();
				if (doc != null){
					if (manager.contains(doc)){
						manager.remove(doc);
						doc.setSavedFrom(SavedFromType.AUTOMATIC);
						manager.add(doc);
					}
				}
	        } catch(Exception e){  
	        	e.printStackTrace(); 
	        } finally{
	        	manager.releaseAccess();
	        }
		}
		
		return result;
	}

}
