package ro.cst.tsearch.servlet;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import ro.cst.tsearch.search.name.CompanyMarshal;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.search.name.companyex.CompanyCom;
import ro.cst.tsearch.search.name.companyex.CompanyNameMarshal;



public class CompanyExceptionServlet extends HttpServlet{
	
	//CompanyException url
private static final long serialVersionUID = 1L;
	
	public void doPost(HttpServletRequest request,
			  HttpServletResponse response)
	throws IOException, ServletException
	{
	    doRequest(request, response);
	}
	

	public void doGet(HttpServletRequest request,
			  HttpServletResponse response)
	throws IOException, ServletException
	{
	    doRequest(request, response);
	}
	public void doRequest(HttpServletRequest request,
						  HttpServletResponse response)
			throws IOException, ServletException
	  {
		PrintWriter writer = null;
		CompanyNameMarshal comp = new CompanyMarshal().unmarshal();
		if(request.getParameter("postparam")!=null){
			if(!request.getParameter("postparam").equals("")){
				CompanyCom co = CompanyCom.unmarshal(request.getParameter("postparam"));
				if(co.getCommand().equalsIgnoreCase("delete")){
					response.setHeader("Content-Type", "text/xml");
					
					co.setStatus("UNSUCCES");
			        for(int i=0;i<comp.getCompanyObject().size();i++){
			        	if(comp.getCompanyObject().get(i).getCompanyName().equalsIgnoreCase(co.getCompanyObject().getCompanyName())){
			        		comp.getCompanyObject().remove(i);
							co.setStatus("SUCCES");
							new CompanyMarshal().marshal(comp);
							CompanyNameExceptions.loadCompany();
							
			        	}
			        }

			        if(co.getStatus().equals("SUCCES")){
			        	new CompanyMarshal().marshal(comp);
			        }
					response.setContentType("xml");
					response.containsHeader("xml");
					response.setContentType("xml");
					writer=response.getWriter();
					writer.println(CompanyCom.marshal(co));
					writer.close();
		
							
				}
				if(co.getCommand().equalsIgnoreCase("edit")){
					//response.setHeader("Content-Type", "text/xml");
					co.setStatus("SUCCES");
					writer=response.getWriter();
					co.getCompanyObject().setCompanyValue(CompanyNameExceptions.companies.get(co.getCompanyObject().getCompanyName()));
					new CompanyMarshal().marshal(comp);
					writer.println(CompanyCom.marshal(co));
					writer.close();
				}
				if(co.getCommand().equalsIgnoreCase("insert")){
					//response.setHeader("Content-Type", "text/xml");
					
					comp.getCompanyObject().add(co.getCompanyObject());
					String temp=co.getCompanyObject().getCompanyName();
					temp=temp.toUpperCase();
					temp=temp.replaceAll("[^0-9A-Z\\s]+", " ");
					co.getCompanyObject().setCompanyName(temp);
					comp.addCompanyObject(co.getCompanyObject());
					new CompanyMarshal().marshal(comp);
					CompanyNameExceptions.loadCompany();
					co.setStatus("SUCCES");
					writer=response.getWriter();
					writer.println(CompanyCom.marshal(co));
					writer.close();
				}
				if(co.getCommand().equalsIgnoreCase("rewrite")){
					//response.setHeader("Content-Type", "text/xml");

			        for(int i=0;i<comp.getCompanyObject().size();i++){
			        	if(comp.getCompanyObject().get(i).getCompanyName().equalsIgnoreCase(co.getCompanyObject().getCompanyName())){
							String temp=co.getCompanyObject().getCompanyName();
							temp=temp.toUpperCase();
							temp=temp.replaceAll("[^0-9A-Z\\s]+", "");
			        		comp.getCompanyObject().get(i).setCompanyName(temp);
			        		comp.getCompanyObject().get(i).setCompanyValue(co.getCompanyObject().getCompanyValue());
							new CompanyMarshal().marshal(comp);
							CompanyNameExceptions.loadCompany();
			        	}
			        }
			        CompanyNameExceptions.companies.remove(co.getCompanyObject().getCompanyName());
			        CompanyNameExceptions.companies.put(co.getCompanyObject().getCompanyName(),co.getCompanyObject().getCompanyValue());
			    	new CompanyMarshal().marshal(comp);
					co.setStatus("SUCCES");
					writer=response.getWriter();
					writer.println(CompanyCom.marshal(co));
					writer.close();
				}
			
			}
		}
		}

}
