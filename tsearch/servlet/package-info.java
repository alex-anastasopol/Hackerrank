/** These package contains different servlets, that is they have as base class {@link javax.servlet.http.HttpServlet},  used in the application. 
 *  Most of them extend {@link ro.cst.tsearch.servlet.BaseServlet} in order to manage the authorization process.
 *  Recently {@link ro.cst.tsearch.servlet.BaseServlet} was {@link java.lang.Deprecated}. This is written on 12/11/2010. 
 *  From now on the filtering provided by the servlet-api will be used. In order to make use of that the url-pattern tag should start with "/servlet" : 
 *  <url-pattern>/servlet/AdditionalInformation</url-pattern>, in order to for this filter to be used:
 *  
 *  <filter-mapping>
		<filter-name>Authentication</filter-name>
		<url-pattern>/servlet/*</url-pattern>
	</filter-mapping> 
 *    
 **/
package ro.cst.tsearch.servlet;