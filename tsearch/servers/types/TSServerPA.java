package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;

public class TSServerPA extends TSServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5572274824893769127L;
	public static final String PA_RECORD = "PA_RECORD";

	public TSServerPA(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public TSServerPA(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		return super.SearchBy(module, sd);
	}

	protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResult) throws ServerResponseException {
		 return new ServerResponse();
	}
	protected ServerResponse searchByMultipleInstrument(List<TSServerInfoModule> modules, Object sd) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		if (modules.isEmpty()) {
			return ServerResponse.createErrorResponse("No modules to search by");
		}

		List<ServerResponse> serverResponses = new ArrayList<ServerResponse>();

		for (int i = 0; i < modules.size(); i++) {
			try {
				TSServerInfoModule mod = modules.get(i);
				global.clearClickedDocuments();

				if (verifyModule(mod)) {
					serverResponses.add(searchBy(mod, sd, null));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ServerResponse serverResponse = mergeResults(serverResponses);
		return serverResponse;
	}

	protected boolean isEmptySR(ServerResponse response) {
		return false;
	}

	protected boolean verifyModule(TSServerInfoModule mod) {
		return true;
	}

	protected ServerResponse mergeResults(List<ServerResponse> serverResponses) {
		ServerResponse response = new ServerResponse();
		response = serverResponses.get(0);
		Vector<ParsedResponse> rows = new Vector<ParsedResponse>();
		for (ServerResponse res : serverResponses) {
			try {
				removeSaveToTSDForm(res);
				
				ParsedResponse parsedResponse = res.getParsedResponse();
				String result = parsedResponse.getResponse();
				result = "<tr><td>" + result + "</td></tr>";
				parsedResponse.setResponse(result);
				res.setResult(result);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String header = CreateSaveToTSDFormHeader(
				URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
				+ "<TABLE cellspacing=\"1\" cellpadding=\"1\" border=\"1\" width=\"100%\">"
				+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
				+ "<TH ALIGN=Left>"
				+ SELECT_ALL_CHECKBOXES
				+ "</TH>"
				+ "<TH ALIGN=Left><FONT SIZE=-1><B>Document</B></FONT></TH>"
				+ "</TR>";

		int nrUnsavedDoc = rows.size();

		String footer = "\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, nrUnsavedDoc);

		response.getParsedResponse().setHeader(header);
		response.getParsedResponse().setFooter(footer);

		return response;
	}

	protected void removeSaveToTSDForm(ServerResponse sResponse) {
		ParsedResponse pResp = sResponse.getParsedResponse();
		try {
			String sResp = pResp.getResponse();
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(sResp, null);

			NodeList nodeList = htmlParser.parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("name", SAVE_TO_TSD_PARAM_NAME), true);

			if (nodeList.size() > 0) {
				StringBuilder html = new StringBuilder();

				NodeList children = nodeList.elementAt(0).getChildren();

				for (int i = 0; i < children.size(); i++) {
					if (!children.elementAt(i).toHtml().contains("<input"))
						html.append(children.elementAt(i).toHtml());
				}
				mSearch.addInMemoryDoc(pResp.getPageLink().getLink(), html.toString());
				 
				String checkBox = "<input type='checkbox' name='docLink' value='" + pResp.getPageLink().getLink() + "'>";
				String row = "<tr><td>" + checkBox + "</td><td>" + html.toString() + "</td>" + "</tr>";

				pResp.setResponse(row);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
