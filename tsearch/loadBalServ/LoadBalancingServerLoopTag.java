package ro.cst.tsearch.loadBalServ;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.tag.LoopTag;

public class LoadBalancingServerLoopTag extends LoopTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected Object[] createObjectList() throws Exception {
		return DBManager.getServers();
	}

}
