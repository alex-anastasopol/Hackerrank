package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;


public class TNDavidsonTR extends FLGenericGovernmaxTR {
	private static final long serialVersionUID = 10000000;
	
	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){
			String linkText = getLinkText(row);
			return linkText.matches("[A-Z]+[0-9-]+.*");
		}
	};
	
	public TNDavidsonTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;	
	}

	public void setServerID(int ServerID){
		super.setServerID(ServerID);
	}
	
/*	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)throws ServerResponseException
	{

		boolean bResetQueryParam= true;
		String sParcelNo;
		ServerResponse rtrnResponse= new ServerResponse();
		int ServerInfoModuleID = module.getModuleIdx();

		if (ServerInfoModuleID == 2) //search by parcelID
		{
			sParcelNo= module.getFunction(0).getParamValue();
			//sParcelNo = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getAtribute( SearchAttributes.LD_PARCELNO ); ---bug 1963
			if (StringUtils.isNotEmpty(sParcelNo))
			{
				String cleanRegEx = "\\d{3,3}-\\d{2,2}-\\w{2,2}-\\d{3,3}\\.\\d{2,2}-\\w{1,2}";
				if (sParcelNo.matches(cleanRegEx)){
					sParcelNo.replaceAll(cleanRegEx, "");
				}else{
					sParcelNo=sParcelNo.replaceAll("- -", "-  -");
					sParcelNo=sParcelNo.replaceAll("-", "/");
				}
				
				//131020D015.00C
				module.getFunction(0).setParamValue(sParcelNo);
			}
			else
			{
				if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() != Search.PARENT_SITE_SEARCH) {//cautare automata
					return super.SearchBy(true, module, sd);
				} else {	
					rtrnResponse.getParsedResponse().setError(
						"We could not process your request because you did not enter a complete Parcel ID Number<br>"
							+ "Please go back and fill in the complete Parcel ID Number.");
					throw new ServerResponseException(rtrnResponse);
				}
			}
		}
		else
			bResetQueryParam= true;
		return super.SearchBy(bResetQueryParam, module, sd);
	}
	*/
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
	
		TSServerInfoModule m;
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		
		
		if(hasPin()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}

		if(hasStreet()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS__NUMBER_NOT_EMPTY);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			l.add(m);
		}

		if(hasOwner()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {"L;F;","L;M;"});
			m.addIterator(nameIterator);			
			l.add(m);
		}
			
		serverInfo.setModulesForAutoSearch(l);		
	}
	
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<tr","</table>", linkStart,  action);
        Vector rows = pr.getResultRows(); // remove table header
        if (rows.size() > 1)
            rows.remove(0);
        pr.setResultRows(rows);
    }
}
