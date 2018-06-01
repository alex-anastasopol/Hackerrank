package ro.cst.tsearch.connection.http2;

public class MIOaklandAO extends MIGenericOaklandAOTR {

	@Override
	protected boolean setupProducts() {

		// residential property profile
		if(!selectProduct(RES_PRODUCT)){
			logger.error("Could not select residential profile product!");
			return false;
		}
		
		// commercial property profile
		if(!selectProduct(COM_PRODUCT)){
			logger.error("Could not select commercial profile product!");
			return false;
		}
		
		return true;
	}
	
}
