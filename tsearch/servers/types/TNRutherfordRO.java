/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import org.gwtwidgets.client.util.SimpleDateFormat;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;

/**
 * @author radu bacrau
 * 
 */
public class TNRutherfordRO extends TNGenericUsTitleSearchDefaultRO {

	private static final long serialVersionUID = 1L;
	private static final String	instNoParamName		= "InstrumentNumber";

	/**
	 * @param searchId
	 */
	public TNRutherfordRO(long searchId) {
		super(searchId);
		setRangeNotExpanded(true);
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public TNRutherfordRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		setRangeNotExpanded(true);
	}

	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, "1");
			module.forceValue(2, "1");
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			
			List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
			
			for(String volumeClass: new String[]{ "105", "101", "10", "1", "110", "-1" }){
				module = getDefaultServerInfo().getModule(BP_MODULE_IDX);
				module.forceValue(0, book);
				module.forceValue(1, page);
				module.forceValue(2, "1");
				module.forceValue(3, "1");	
				module.forceValue(5, volumeClass);
				list.add(module);
			}
			
			return list;
			
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "1");
			module.forceValue(2, "1");
		} else {
			module = null;
		}
		return module;
	}
	
	@Override
	protected String specificInstrumentNo(InstrumentI instrument) {
		String instrNo = instrument.getInstno();
		if(instrNo.length() > 7 && instrument.getDate() != null) {
			String dateToString = new SimpleDateFormat("yyyyMMdd").format(instrument.getDate());
			if(instrNo.startsWith(dateToString)) {
				return instrNo.replaceFirst(dateToString, "").replaceFirst("^0+", "");
			}
		}
		return super.specificInstrumentNo(instrument);
	}
	
	@Override
	protected void ParseResponse(String action, ServerResponse Response, int viParseID) throws ServerResponseException {
		// b10011
		String query;
		if ((query = Response.getQuerry()) != null && query.contains(instNoParamName)) {
			String instNoParam = StringUtils.extractParameterFromUrl(query, instNoParamName);
			query = query.replaceFirst(instNoParam, instNoParam.trim());
			Response.setQuerry(query);
		}
		super.ParseResponse(action, Response, viParseID);
	}
}
