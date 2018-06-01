package ro.cst.tsearch.utils;

public class GoogleMapsStructure {
	
	float latitude = -1.0f;
	float longitude = -1.0f;
	int zoom = -1;
	String typeId = "";
	
	public GoogleMapsStructure(float latitude, float longitude, int zoom, String typeId) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoom = zoom;
		this.typeId = typeId;
	}

	public float getLatitude() {
		return latitude;
	}

	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}
	
}
