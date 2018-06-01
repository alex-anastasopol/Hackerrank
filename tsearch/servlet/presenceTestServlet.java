package ro.cst.tsearch.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;

public class presenceTestServlet extends BaseServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		String v = request.getParameter("searchID");
		CurrentInstance curInst = InstanceManager.getManager()
				.getCurrentInstance(Long.parseLong(v));
		List<TSServerInfoFunction> L = curInst.getCrtSearchContext().getSearchRecord().getFunctionList();
		int moduleId = curInst.getCrtSearchContext().getSearchRecord().getModuleID();

		Vector PagesVector = curInst.getCrtSearchContext().getSearchRecord().getPageAndIndex();
		String sir = "";
		try {
			sir = curInst.getCrtSearchContext().getSearchRecord().getPageAndIndexOfLinkLastElement().getPage();
			if (sir == null) {
				sir = "";
			}
		} catch (Exception e) {
			logger.error("Page empty");
		}

		curInst.getCrtSearchContext().getSearchRecord().setSearchResults(sir);

		String p1 = curInst.getCrtSearchContext().getSearchRecord().getParameterP1();
		String p2 = curInst.getCrtSearchContext().getSearchRecord().getParameterP2();

		long SearchID = Long.parseLong(v);
		String classNAME = curInst.getCrtSearchContext().getSearchRecord().getServerName();
		try {
			classNAME = TSServersFactory.getTSServerName(
					curInst.getCommunityId(),
					curInst.getCrtSearchContext().getP1ParentSite(), 
					curInst.getCrtSearchContext().getP2ParentSite());
		} catch (Exception e) {
			e.printStackTrace();
		}
		String msSiteRealPath = curInst.getCrtSearchContext().getSearchRecord().getMsSiteRealPath();

		synchronized (this) {
			WriteXMLDocumentWithTestData.writeTestData(PagesVector, moduleId, L, L.size(), p1, p2, classNAME, msSiteRealPath, SearchID);
		}

		response.sendRedirect(AppLinks.getParentSiteNoSaveHref(SearchID));

	}

}
