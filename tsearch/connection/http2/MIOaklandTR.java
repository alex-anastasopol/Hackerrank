package ro.cst.tsearch.connection.http2;

public class MIOaklandTR extends MIGenericOaklandAOTR {

	@Override
	protected boolean setupProducts() {

		// tax product
		if(!selectProduct(TAX_PRODUCT)){
			logger.error("Could not select tax profile product!");
			return false;
		}
		
		return true;
	}
	
}
