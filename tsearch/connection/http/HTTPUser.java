package ro.cst.tsearch.connection.http;

public class HTTPUser 
{
	public HTTPUser(String name, String password)
	{
		this.name = name;
		this.password = password;
	}
	
	String name;
	String password;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String toString()
	{
		return "User name=" + name + " password=" + password;
	}
}
