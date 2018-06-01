package ro.cst.tsearch.servlet.dasl;

import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.servers.types.TSServerDASLAdapter;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.TemplatesException;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;

public class ParentSiteLoadTemplate extends BaseServlet{

	private static final long serialVersionUID = 109342678943L;


	public void doRequest(HttpServletRequest request, HttpServletResponse response)  throws Exception
	{
		
		long searchId = Long.parseLong(request.getParameter("searchId"));
		long commId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue();
		String templateCode = request.getParameter("template");
		
		String xmlret = "";
		
		HashMap<String, Object> inputs = TSServerDASLAdapter.emptyDASLTemplatesParameters();
		
		List<CommunityTemplatesMapper> userTemplates = null;
		try {
			userTemplates = UserUtils.getUserTemplates( -1,commId,UserUtils.FILTER_IGNORE_USER, -1 );
		} catch (Exception e) {
			System.err.print(e.getMessage());
		}
		
		CommunityTemplatesMapper templateInfo = getDASLTemplate(userTemplates,templateCode);
		try{
			xmlret =  AddDocsTemplates.completeNewTemplatesV2ForTextFilesOnly(inputs, "", templateInfo,null, false, null, null, new HashMap<String,String>());
		}
		catch(TemplatesException e){
			e.printStackTrace();
		}
		
		response.getOutputStream().write(xmlret.getBytes());
		response.getOutputStream().flush();
		
	}

	public static CommunityTemplatesMapper getDASLTemplate(List<CommunityTemplatesMapper> userTemplates,String templateCode) {

		if (userTemplates != null) {
			for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
				String test = communityTemplatesMapper.getPath();
				if (test == null) {
					test = "";
				}
				if (test.contains(templateCode)) {
					return communityTemplatesMapper;
				}				
			}
			
		}
		return null;
	}

}
