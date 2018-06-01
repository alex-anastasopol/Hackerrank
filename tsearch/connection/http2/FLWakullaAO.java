package ro.cst.tsearch.connection.http2;

public class FLWakullaAO extends FLGenericQPublicAO {
	@Override
	public String getCounty() {
		return "fl_wakulla";
	}
	
	@Override
	public String getLegalLink() {
		return "/cgi-bin/wakulla_legal.cgi?parcel_id=";
	}
	
}
