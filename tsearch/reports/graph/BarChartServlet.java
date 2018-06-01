package ro.cst.tsearch.reports.graph;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.CSTCalendar;

public class BarChartServlet extends BaseServlet{

	public void doRequest(HttpServletRequest request, HttpServletResponse res) 
				throws ServletException, IOException
	{
		int noOfSeries = 0;
		int imageWidth = 340;
		int imageHeight = 240;
		
		Double dataView[]	= null;
		PrintWriter out = null;
		HttpSession session = request.getSession( true );
		dataView = (Double[])session.getAttribute("graphData");

		String intervalName	= "";//( request.getParameter( "" ) );
		byte graphType 		= new Byte( Constants.INCOME_GRAPHTYPE ).byteValue();
		byte rotateType		= Byte.parseByte( String.valueOf(Constants.NO_ROTATE) ) ;
			
		if (dataView!=null) {				
			noOfSeries 	= (dataView==null)?0:dataView.length;
			imageWidth = (noOfSeries>15)?(imageWidth+(noOfSeries-15)*20):imageWidth;
		    
			String[] labels = new String[noOfSeries];
			String[] downLabels = new String[noOfSeries];
			boolean[] highlight = new boolean[noOfSeries];
			double[] values = new double[noOfSeries];
			String mainLabel= "";
			boolean isMoney = false;

			int i;
			switch(graphType) {
				case Constants.INCOME_GRAPHTYPE:
				isMoney = true;
				for (i=0;i<noOfSeries;i++) {
					values[i] = (dataView[i]).doubleValue();
				}
				break;
				case Constants.NR_SEARCHES_GRAPHTYPE:
				isMoney = false;
				for (i=0;i<noOfSeries;i++) {
					values[i] = (dataView[i]).doubleValue();
				}
			}

			if (noOfSeries == 12) {
				intervalName	= ( request.getParameter( "yearReport" ) );
				mainLabel = "Year " + intervalName;
				downLabels = null;
				for (i=0;i<noOfSeries;i++) {
					labels[i] = CSTCalendar.MONTHS[i];
				}
			}
			else {
				int year, month, day;
	
				intervalName	= ( request.getParameter( "monthReport" ) );
				mainLabel = "Month " + CSTCalendar.MONTHS[Integer.parseInt(intervalName)-1];
				month = Integer.parseInt( intervalName );
				intervalName	= ( request.getParameter( "yearReport" ) );
				year = Integer.parseInt( intervalName );
				mainLabel += " " + intervalName;

				Calendar c = Calendar.getInstance();
				String dayShort = "";

				for (i=0;i<noOfSeries;i++) {
					labels[i] = String.valueOf(i+1);

					c.set( year, month - 1, i+1 );
					day = c.get( Calendar.DAY_OF_WEEK );

					switch(day) {
						case Calendar.MONDAY:
							dayShort = "M";
							break;
						case Calendar.TUESDAY:
							dayShort = "T";
							break;
						case Calendar.WEDNESDAY:
							dayShort = "W";
							break;
						case Calendar.THURSDAY:
							dayShort = "T";
							break;
						case Calendar.FRIDAY:
							dayShort = "F";
							break;
						case Calendar.SATURDAY:
							dayShort = "S";
							break;
						case Calendar.SUNDAY:
							dayShort = "S ";  // "S " to be different from "S" of Saturday
					}

					downLabels[i] = dayShort ;
					if (downLabels[i].equals("S "))			
						highlight[i] = true;
				}
			}
			mainLabel += " ";

			BarChart2D bc = new BarChart2D(imageWidth, imageHeight, 10, 
						   labels, downLabels, highlight, values, 
						   mainLabel, isMoney
						   ); 

			//bc.setRotateType(rotateType);
			bc.setOriginsTextSize(12);
			res.setContentType("image/jpeg");
			bc.putInOutputStream(res.getOutputStream());
			res.getOutputStream().close();
		}	
	}

}
