package ro.cst.tsearch.servers.info;


public class TSServerInfoParam {
    
    static final long serialVersionUID = 10000001;
    
    public String name;
    public String value;
    
    public TSServerInfoParam(String name, String value) {
        this.name = name;
        this.value = value;
    }
  
    public boolean equals(Object obj) {

		if (obj instanceof TSServerInfoParam) {
			return name.equals(((TSServerInfoParam) obj).name);
		} else
			return false;
	}
    
    @Override
    public String toString() {
    	return "("+ name + "," + value + ")";
    }
    
}
