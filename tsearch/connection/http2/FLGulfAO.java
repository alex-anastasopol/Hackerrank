package ro.cst.tsearch.connection.http2;

public class FLGulfAO extends FLGenericQPublicAO  {
	
	@Override
	public String getCounty() {
		return "fl_gulf";		
	}
	
	
	public String getLegalLink() {
		return "/cgi-bin/gulf_legal.cgi?parcel_id=";		
	}
	
	
	

}
