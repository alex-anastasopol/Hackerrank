package ro.cst.tsearch.bean;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.reports.data.ReportLineData;
import ro.cst.tsearch.reports.throughputs.StandardLinkGenerator;
import ro.cst.tsearch.reports.throughputs.ThroughputBean;
import ro.cst.tsearch.reports.throughputs.ThroughputOpCode;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

import com.mchange.v2.c3p0.impl.NewProxyCallableStatement;

public class BarReportBean {
	
	private static final Logger logger = Logger.getLogger(BarReportBean.class);
	
	private boolean showBoth;
	private int[] reportState = {-1};
	private int[] reportCounty = {-1};
	private int[] reportAgent = {-1};
	private int[] reportAbstractor = {-1};
	private String[] reportCompanyAgent = {"-1"}; 
	
	private int MAX_MIN = 999999;
	private String chartType = "";
	private String beanName = "";
	
	private int invoice = 0;
	public String getInvoice() {
	    return String.valueOf(invoice);
	}
	public void setInvoice(String invoice) {
	    try {
	        this.invoice = Integer.parseInt(invoice);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public BarReportBean(){
	}
	
	private int getNumberOfWorkingHours(int year, int month, int day){
		GregorianCalendar cal = new GregorianCalendar(year,month,day);
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); 
		if(dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)	//weekend
			return 0;			
		else									//normal weekday, future optimization holidays
			return 8;
	}
	private int getNumberOfWorkingHours(int year, int month){
		GregorianCalendar cal = new GregorianCalendar(year,month,1);
		int hours = 0;
		for(int day=1; day<=cal.getActualMaximum(Calendar.DAY_OF_MONTH); day++)
			hours += getNumberOfWorkingHours(year,month,day);
		return hours;
	}
	
	private int getNumberOfWorkingHours(int year){
		int number = 0;
		for(int i=0; i<12; i++)
			number += getNumberOfWorkingHours(year,i);
		return number;
	}
	
	private int getNumberOfHours(int year, int month){
		GregorianCalendar cal = new GregorianCalendar(year,month,1);
		return cal.getActualMaximum(Calendar.DAY_OF_MONTH)*24;	
	}
	
	private int getNumberOfHours(int year){
		if(year%4==0)
			return 365*24;
		else
			return 366*24;
	
	}
	
	private ReportLineData[] getGeneralData(int[] countyId, int[] abstractorId, 
			int[] agentId, int[] stateId,String[] compName,  
			int commId, int groupId, int payrateType, int productId){
		
		String sql = "call getGeneralData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (chartType.contains("Stacked")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, commId);
			call.setInt(8, groupId);
			call.setInt(9, payrateType);
			call.setInt(10, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getGeneralData " + (System.currentTimeMillis()-time) + " millis");
			//System.err.println("nr de randuri " + data.getRowNumber());
			return setReportData(data, productId,-1,-1);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new ReportLineData[0];
	}
	
	private ReportLineData[] getYearlyData(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int commId, int groupId, int payrateType, int productId, int year){
		
		String sql = "call getYearlyData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (chartType.contains("Stacked")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, commId);
			call.setInt(8, groupId);
			call.setInt(9, payrateType);
			call.setInt(10, year);
			call.setInt(11, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getYearlyData " + (System.currentTimeMillis()-time) + " millis");
			
			return setReportData(data, productId,-1,year);
			
		} catch (Exception e){
			logger.error(e);
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new ReportLineData[0];
	}
	
	private ReportLineData[] getMonthlyData(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int commId, int groupId, int payrateType, int invoice, int productId, int year, int month){
		
		String sql = "call getMonthlyData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (chartType.contains("Stacked")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, commId);
			call.setInt(8, groupId);
			call.setInt(9, payrateType);
			call.setInt(10, year);
			call.setInt(11, month);
			call.setInt(12, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getMonthlyData " + (System.currentTimeMillis()-time) + " millis");
			//System.err.println("nr de randuri getYearlyData: " + data.getRowNumber());
			return setReportData(data, productId, month, year);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new ReportLineData[0];
	}
	
	private ReportLineData[] getLineGeneralData(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, 
            int commId, int groupId, int payrateType, int productId, String year, double below, double above){
		
		String sql = "call getLineGeneralData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (chartType.contains("Stacked")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, commId);
			call.setInt(8, groupId);
			call.setInt(9, payrateType);
			call.setInt(10, Integer.parseInt(year));
			call.setDouble(11, below);
			call.setDouble(12, above);
			call.setInt(13, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getLineGeneralData " + (System.currentTimeMillis()-time) + " millis");
			//System.err.println("nr de randuri " + data.getRowNumber());
			return setReportData(data, productId,-1,-1);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new ReportLineData[0];
	}
	
	private ReportLineData[] getLineAnnualData(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int commId, int groupId, int payrateType, int productId, int year, String month, double below, double above){
		
		String sql = "call getLineAnnualData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (chartType.contains("Stacked")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, commId);
			call.setInt(8, groupId);
			call.setInt(9, payrateType);
			call.setInt(10, year);
			call.setInt(11, Integer.parseInt(month));
			call.setDouble(12, below);
			call.setDouble(13, above);
			call.setInt(14,productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getLineAnnualData for year " + year + " and month " + month + " : " + (System.currentTimeMillis()-time) + " millis");
			
			return setReportData(data, productId,-1,year);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
                return null;
            }           
        }
		
		return new ReportLineData[0];
	}
	
	private ReportLineData[] getLineMonthlyData(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int commId, int groupId, int payrateType, int productId, int year, int month, String day, double below, double above){
		
		String sql = "call getLineMonthlyData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (chartType.contains("Stacked")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, commId);
			call.setInt(8, groupId);
			call.setInt(9, payrateType);
			call.setInt(10, year);
			call.setInt(11, month);
			call.setInt(12, Integer.parseInt(day));
			call.setDouble(13, below);
			call.setDouble(14, above);
			call.setInt(15, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getLineMonthlyData " + (System.currentTimeMillis()-time) + " millis");
			//System.err.println("nr de randuri getLineMonthlyData " + data.getRowNumber());
			return setReportData(data, productId,month,year);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
                return null;
            }           
        }
		
		return new ReportLineData[0];
	}
	
	
	private Long[] getMinAndMax(int[] countyId, int[] abstractorId, int[] agentId, int[] stateId, 
			String[] compName, int commId, int groupId, int productId, 
            Vector<Double> min, Vector<Double> max, Vector<String> years, int forYear, int forMonth){
		
		String sql = "call getMinAndMax(?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setString(1, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(2, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(3, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(4, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(6, commId);
			call.setInt(7, groupId);
			call.setInt(8, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getMinAndMax " + (System.currentTimeMillis()-time) + " millis");
			//System.err.println("nr de randuri getMinMax: " + data.getRowNumber());
			return setReportDataMinMax(data, productId, forMonth, forYear, min, max, years);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		return null;
	}
	
	private Long[] getMinAndMaxAnnual(int[] countyId, int[] abstractorId, int[] agentId, 
			int[] stateId, String[] compName, int commId, int groupId, 
			int productId, int year, Vector<Double> min, Vector<Double> max, Vector<String> years, int forMonth){
		
		String sql = "call getMinAndMaxAnnual(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setString(1, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(2, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(3, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(4, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(6, commId);
			call.setInt(7, groupId);
			call.setInt(8, year);
			call.setInt(9, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getMinAndMaxAnnual " + (System.currentTimeMillis()-time) + " millis");
			//System.err.println("nr de randuri getMinMax: " + data.getRowNumber());
			return setReportDataMinMax(data, productId, forMonth, year, min, max, years);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		return null;
	}
	
	private Long[] getMinAndMaxMonthly(int[] countyId, int[] abstractorId, 
			int[] agentId, int[] stateId, String[] compName, int commId, 
			int groupId, int productId, int year, int month, Vector<Double> min, Vector<Double> max, Vector<String> years){
		
		String sql = "call getMinAndMaxMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setString(1, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(2, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(3, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(4, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(6, commId);
			call.setInt(7, groupId);
			call.setInt(8, year);
			call.setInt(9, month);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("BarReportBean: getMinAndMaxMonthly " + (System.currentTimeMillis()-time) + " millis");
			//System.err.println("nr de randuri getMinMax: " + data.getRowNumber());
			return setReportDataMinMax(data, productId, month, year, min, max, years);
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		return null;
	}
	
	private ReportLineData[] setReportData(DatabaseData data, int productId, int month, int year) {
		ReportLineData[] reportLineData = new ReportLineData[0];
		int cnt[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		int reportType = 0;			//using int for future possible customization
		int minim = 999999;
		int maxim = 0;
		if(month<0 || year <0){
			if(year<0 && month<0){
				reportType = 3;			//general
			}
			else {
				reportType = 1;			//annual	
			}
		}
			
		else 
			reportType = 2;			//monthly report
		
		try {
			int resultsLength = data.getRowNumber();
			if(resultsLength>0){
				int nrEntries=0;
				if(reportType == 1 || reportType ==3){
					if(reportType == 3){
						//Vector<String> vaux = new Vector<String>();
						for (int i = 0; i < resultsLength ; i++) {
							String sss = data.getValue(3, i).toString();
							//determining number of entries
							/*if(!vaux.contains(sss)){
								vaux.add(sss);
								nrEntries++;
							}*/
							int intaux = Integer.parseInt(sss); 
							//must also determin minimum
							if(minim > intaux)
								minim = intaux;
							if(maxim < intaux)
								maxim = intaux;
						}
						nrEntries = maxim-minim+1;
					}
					if(reportType == 1){
						nrEntries=12;
						minim = 1;
					}
					
				} else {	//it means that we have a monthly report, so we must generate entries for every day
					Calendar c = new GregorianCalendar(year,month-1,1);
					minim = 1;
					nrEntries = c.getActualMaximum(Calendar.DAY_OF_MONTH);
				}
				reportLineData = new ReportLineData[nrEntries];
				for (int i = 0; i < reportLineData.length; i++) {
					reportLineData[i] = new ReportLineData();
					reportLineData[i].setIntervalName(minim+i+"");
				}
				for (int i = 0; i < resultsLength; i++) {
					int id = Integer.parseInt(data.getValue(1,i).toString());
					if( id ==1 ){
						if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
							cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
						}
						/*
						if(reportType == 1){	//if we have an annual report, we read the month from the database
							cnt[id-1] = Integer.parseInt((String)data.getValue(4, i))-1;
						}*/
						if(reportType == 3){
							cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
						}
						if(data.getValue(2, i)!=null && (productId < 0 || productId == Products.FULL_SEARCH_PRODUCT))
							reportLineData[cnt[id-1]].setIncomeSearches(Double.parseDouble(data.getValue(2,i).toString()));
						else
							reportLineData[cnt[id-1]].setIncomeSearches(0);
						if(data.getColumnNumber()>=4){
							if(data.getValue(4, i)!=null && (productId < 0 || productId == Products.FULL_SEARCH_PRODUCT))
								reportLineData[cnt[id-1]].setNoOfSearches(Long.parseLong(data.getValue(4,i).toString()));
							else
								reportLineData[cnt[id-1]].setNoOfSearches(0);
						}

						
					} else if (id==2 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.CURRENT_OWNER_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeCurrentOwner(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeCurrentOwner(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.CURRENT_OWNER_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfCurrentOwner(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfCurrentOwner(0);
								   }						
						 } else if (id==3 ) {
							 
								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.CONSTRUCTION_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeConstruction(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeConstruction(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.CONSTRUCTION_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfConstruction(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfConstruction(0);
						}
								
					} else if (id==4 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.COMMERCIAL_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeCommercial(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeCommercial(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.COMMERCIAL_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfCommercial(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfCommercial(0);
								}								
						
					}  else if (id==5 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.REFINANCE_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeRefinance(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeRefinance(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && productId < 0 ||(productId == Products.REFINANCE_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfRefinance(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfRefinance(0);
								}								
						
					}  else if (id==6 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.OE_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeOE(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeOE(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.OE_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfOE(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfOE(0);
								}						
						
					}  else if (id==7 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.LIENS_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeLiens(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeLiens(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.LIENS_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfLiens(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfLiens(0);
								}								
						
					}  else if (id==8 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.ACREAGE_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeAcreage(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeAcreage(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.ACREAGE_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfAcreage(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfAcreage(0);
								}
					}  else if (id==9 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.SUBLOT_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeSublot(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeSublot(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.SUBLOT_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfSublot(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfSublot(0);
								}
					}  else if (id==10 ) {

								if(reportType == 3){
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
								}
								if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
									cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
								}
								if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.UPDATE_PRODUCT))
									reportLineData[cnt[id-1]].setIncomeUpdates(Double.parseDouble(data.getValue(2,i).toString()));
								else
									reportLineData[cnt[id-1]].setIncomeUpdates(0);
								if(data.getColumnNumber()>=4){
									if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.UPDATE_PRODUCT))
										reportLineData[cnt[id-1]].setNoOfUpdates(Long.parseLong(data.getValue(4,i).toString()));
									else
										reportLineData[cnt[id-1]].setNoOfUpdates(0);
								}
								
					} else if (id==14 ) {

						if(reportType == 3){
							cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
						}
						if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
							cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
						}
						if(data.getValue(2, i)!=null && (productId < 0 ||productId == Products.UPDATE_PURE_PRODUCT))
							reportLineData[cnt[id-1]].setIncomePureUpdates(Double.parseDouble(data.getValue(2,i).toString()));
						else
							reportLineData[cnt[id-1]].setIncomePureUpdates(0);
						if(data.getColumnNumber()>=4){
							if(data.getValue(4, i)!=null && (productId < 0 ||productId == Products.UPDATE_PURE_PRODUCT))
								reportLineData[cnt[id-1]].setNoOfPureUpdates(Long.parseLong(data.getValue(4,i).toString()));
							else
								reportLineData[cnt[id-1]].setNoOfPureUpdates(0);
						}
						
					} else if (id == 11 ) {

						if(reportType == 3){
							cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-minim;
						}
						if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
							cnt[id-1] = Integer.parseInt(data.getValue(3, i).toString())-1;
						}
						if(data.getValue(2, i)!=null)
							reportLineData[cnt[id-1]].setIncomePaid(new BigDecimal(data.getValue(2,i).toString()).longValue());
						else
							reportLineData[cnt[id-1]].setIncomePaid(0);
						//if(data.getValue(3, i)!=null)
						//	reportLineData[cnt[id-1]].setIntervalName((String)data.getValue(3, i));
						//else
						//	reportLineData[cnt[id-1]].setIntervalName("0");
						//cnt[id-1]++;
						//System.out.println("ID "+id+ ": " + (String)data.getValue(4, i));
					} else if (id == 12) {

						if (reportType == 3){
							cnt[id - 1] = Integer.parseInt(data.getValue(3, i).toString()) - minim;
						}
						if (reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
							cnt[id - 1] = Integer.parseInt(data.getValue(3, i).toString()) - 1;
						}
						if (data.getValue(2, i) != null && (productId < 0 || productId == Products.FVS_PRODUCT)){
							reportLineData[cnt[id - 1]].setIncomeFVS(Double.parseDouble(data.getValue(2, i).toString()));
						} else{
							reportLineData[cnt[id - 1]].setIncomeFVS(0);
						}
						if (data.getColumnNumber() >= 4){
							if (data.getValue(4, i) != null && (productId < 0 || productId == Products.FVS_PRODUCT)){
								reportLineData[cnt[id - 1]].setNoOfFVS(Long.parseLong(data.getValue(4, i).toString()));
							} else{
								reportLineData[cnt[id - 1]].setNoOfFVS(0);
							}
						}
						
			}
					
				}
				
				
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reportLineData;
	}
	
	private Long[] setReportDataMinMax(DatabaseData data, int productId, int month, int year, Vector<Double> min, Vector<Double> max, Vector<String> years) {
		ReportLineData[] reportLineData = new ReportLineData[0];
		Long[] count = null;
		Double dmintemp = new Double(MAX_MIN), dmaxtemp = new Double(0);
		int cnt[] = {0,0,0,0,0,0,0,0,0,0};
		int minim = 999999;
		int maxim = 0;
		
		int reportType = 0;			//using int for future possible customization
		if(month<0 || year <0){
			if(year<0 && month<0){
				reportType = 3;			//general
			}
			else {
				reportType = 1;			//annual	
			}
		}
			
		else 
			reportType = 2;			//monthly report
		
		try {
			int resultsLength = data.getRowNumber();
			if(resultsLength>0){
				int nrEntries=0;
				if(reportType == 1 || reportType ==3){
					if(reportType == 3){
						//Vector<String> vaux = new Vector<String>();
						for (int i = 0; i < resultsLength ; i++) {
							String sss = data.getValue(5, i).toString();
							//determining number of entries
							/*if(!vaux.contains(sss)){
								vaux.add(sss);
								nrEntries++;
							}*/
							int intaux = Integer.parseInt(sss); 
							//must also determin minimum
							if(minim > intaux)
								minim = intaux;
							if(maxim < intaux)
								maxim = intaux;
						}
						nrEntries = maxim-minim+1;
					}
					if(reportType == 1){
						minim = 1;
						nrEntries=12;
					}
					
				} else {	//it means that we have a monthly report, so we must generate entries for every day
					Calendar c = new GregorianCalendar(year,month-1,1);
					minim = 1;
					nrEntries = c.getActualMaximum(Calendar.DAY_OF_MONTH);
				}
				
				reportLineData = new ReportLineData[nrEntries];
				count = new Long[nrEntries];
				for (int i = 0; i < reportLineData.length; i++) {
					reportLineData[i] = new ReportLineData();
					reportLineData[i].setIntervalName(i+minim+"");
					min.add(new Double(MAX_MIN));
					max.add(new Double(0));
					years.add("");
					count[i] = new Long(0);
				}
				for (int i = 0; i < resultsLength; i++) {
					int id = Integer.parseInt(data.getValue(1,i).toString());
					if(id==1){
						if(productId < 0 || productId == 1){
							
							if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
								cnt[id-1] = Integer.parseInt(data.getValue(5, i).toString())-1;
							}
							if(reportType == 3)
								cnt[id-1] = Integer.parseInt(data.getValue(5, i).toString())-minim;
							if(data.getValue(3, i)!=null){
								dmintemp = Double.parseDouble(data.getValue(3, i).toString());
								if(dmintemp<min.get(cnt[id-1])) min.set(cnt[id-1], dmintemp);
							}
							if(data.getValue(4, i)!=null){
								dmaxtemp = Double.parseDouble(data.getValue(4, i).toString());
								if(dmaxtemp>max.get(cnt[id-1])) max.set(cnt[id-1], dmaxtemp);
							}
							if(data.getValue(5, i)!=null){		//will never be null, and most likely will contain every year
								years.set(cnt[id-1], data.getValue(5, i).toString());
							} else {
								years.set(cnt[id-1], "0");
							}
						}
						count[cnt[id-1]] += Long.parseLong(data.getValue(2, i).toString());
						//cnt[id-1]++;
						
					} else { 
						if(productId < 0 || productId == id){
							//while(!((String)data.getValue(5, i)).equalsIgnoreCase(years.get(cnt[id-1]))){
							//	if(cnt[id-1]==nrEntries-1)
							//		break;
							//	cnt[id-1]++;
							//}
							if(reportType == 3){
								cnt[id-1] = Integer.parseInt(data.getValue(5, i).toString())-minim;
							}
							if(reportType == 2 || reportType == 1){	//if we have a monthly report, we read the day/month from the database
								cnt[id-1] = Integer.parseInt(data.getValue(5, i).toString())-1;
							}
							if(data.getValue(3, i)!=null){
								dmintemp = Double.parseDouble(data.getValue(3, i).toString());
								if(dmintemp<min.get(cnt[id-1])) min.set(cnt[id-1], dmintemp);
							}
							if(data.getValue(4, i)!=null){
								dmaxtemp = Double.parseDouble(data.getValue(4, i).toString());
								if(dmaxtemp>max.get(cnt[id-1])) max.set(cnt[id-1], dmaxtemp);
							}
							if(data.getValue(5, i)!=null){		//will never be null, and most likely will contain every year
								years.set(cnt[id-1], data.getValue(5, i).toString());
							} else {
								years.set(cnt[id-1], "0");
							}
							count[cnt[id-1]] += Long.parseLong(data.getValue(2, i).toString());
						}
					}
					
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return count;
	}
	
	private CategoryDataset getCategoryDatasetYear(HttpServletRequest request) throws Exception{
		//row keys...
		String series1 = "Searches Ordered";
		String series2 = "Searches Done";
		if(beanName.equals(ThroughputOpCode.INCOME_BEAN)){
			series2 = "Due";
			series1 = "Paid";
		}
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		HttpSession session = request.getSession();
		
		//getting current user and community
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		//UserAttributes ua = currentUser.getUserAttributes();
		int commId = -1;
		int payrateType = 0;
		boolean isTSAdmin = UserUtils.isTSAdmin(currentUser.getUserAttributes());
		if (isTSAdmin) {
			if (beanName.equals(ThroughputOpCode.INCOME_BEAN)){
				series1 = "Received";
			}
			payrateType = 1;
		}
		
		ReportLineData[] reportLineData = new ReportLineData[0];
		ReportLineData[] reportLineDataAux = new ReportLineData[0];
		
		ThroughputBean thBean = (ThroughputBean)session.getAttribute(beanName);
		if(thBean==null){
			thBean = new ThroughputBean();
		}
		int yearReport = Integer.parseInt((String)request.getSession().getAttribute("yearReport"));
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		if(groupId<0) groupId = -1;				//if we've clicked "Other" this would decrease below -1
		commId = Integer.parseInt(thBean.getSelectCommunities());
		if(commId<0) commId = -1;				//if we've clicked "Other" this would decrease below -1
		
		int productId = Integer.parseInt(thBean.getSelectProducts());
		String[] strArray = null;
		int sel = Integer.parseInt(thBean.getSelectStates());
		if( sel < 0 ){
			strArray = thBean.getMultiStates();			//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportState = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportState[i] = Integer.parseInt(strArray[i]);
				}	
			}
			else {
				reportState = new int[1];
				reportState[0] = sel;
			}
		}
		else {
			reportState = new int[1];
			reportState[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectCounties());
		if( sel < 0 ){
			strArray = thBean.getMultiCounties();					//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportCounty = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportCounty[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportCounty = new int[1];
				reportCounty[0] = sel;
			}
		}
		else {
			reportCounty = new int[1];
			reportCounty[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectAbstractors());
		if( sel < 0 ){
			strArray = thBean.getMultiAbstractors();				//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportAbstractor = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportAbstractor[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportAbstractor = new int[1];
				reportAbstractor[0] = sel;
			}
		}
		else {
			reportAbstractor = new int[1];
			reportAbstractor[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectAgents());
		if( sel < 0 ){
			strArray = thBean.getMultiAgents();			//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportAgent = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportAgent[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportAgent = new int[1];
				reportAgent[0] = sel;
			}
		}
		else {
			reportAgent = new int[1];
			reportAgent[0] = sel;
		}
		Long[] count = null;
		reportCompanyAgent = thBean.getMultiCompaniesAgents();
		if(showBoth){
			reportLineData = getYearlyData(reportCounty, reportAbstractor, reportAgent, reportState, 
					reportCompanyAgent, commId, groupId, payrateType, productId, yearReport);
		} else {
			if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)){
				Vector<Double> min = new Vector<Double>(), max = new Vector<Double>();
				Vector<String> years = new Vector<String>();
				double above, below;	//we will keep data between above and below(below is the lower limit)
				count = getMinAndMaxAnnual(reportCounty, reportAbstractor, reportAgent, reportState, 
						reportCompanyAgent, commId, groupId, productId, yearReport, min, max,years, -1);
				if(count==null)			//defensive
					return dataset;
				//System.err.println("MIN: " + min);
				//System.err.println("MAX: " + max);
				//System.err.println("CNT: " + count);
				
				reportLineData = new ReportLineData[count.length];
				for (int i = 0; i < count.length; i++) {
					//above = max.get(i).doubleValue()-min.get(i).doubleValue();	//calculez intervalul maxim
					//below = above * 5 / 100 + min.get(i).doubleValue();		//sub 5%
					//above = above * 95/ 100 + min.get(i).doubleValue();		//peste 95%
					below = -1.0;
					above = max.get(i).doubleValue()+100.0;
					//below = Double.parseDouble(min.get(i).toString())-0.1;
					//above = Double.parseDouble(max.get(i).toString())+0.1;
					if(above<below){
						double swap = below;
						below = above;
						above = swap;
					}
					if(years.get(i)!=null && !years.get(i).equals("")) {
						reportLineDataAux = getLineAnnualData(reportCounty, reportAbstractor, reportAgent, 
								reportState, reportCompanyAgent,  
								commId, groupId, payrateType, productId,yearReport, years.get(i),below,above);
						if(reportLineDataAux.length>0)
							reportLineData[i] = reportLineDataAux[i];
						else 
							reportLineData[i] = null;
					} else 
						reportLineData[i] = null;		
				}
			} else {
				reportLineData = getLineAnnualData(reportCounty, reportAbstractor, reportAgent, reportState, 
						reportCompanyAgent, commId, groupId, payrateType, productId, yearReport, "0", 0, 0);
			}
	
		}
		if(reportLineData==null || reportLineData.length==0)
			return dataset;
		int reportLength = reportLineData.length;
		//System.err.println("Size " + reportLength);
		Long graphDataAllNoOfSearches[] = new Long[reportLength];		//pentru cautarile cerute
		Long graphDataTotalNo[] = new Long[reportLength];				//pentru cautarile cerute rezolvate
		
		for (int i = 0; i < reportLength; i++) {
			graphDataAllNoOfSearches[i] = new Long(0);
			graphDataTotalNo[i] = new Long(0);
		}
		
		
		DateFormatSymbols dfs = new DateFormatSymbols();
		for (int i = 0; i < reportLength; i++){
			String month = dfs.getMonths()[i];
			if (reportLineData[i]!=null && reportLineData[i].getIntervalName()!=null) {
				if(showBoth){
					
					dataset.addValue(reportLineData[i].getIncomePaid(), series1, dfs.getMonths()[Integer.parseInt(reportLineData[i].getIntervalName())-1]);
					//dataset.addValue(reportLineData[i].getTotalIncome(), series2, dfs.getMonths()[Integer.parseInt(reportLineData[i].getIntervalName())-1]);
					if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)) {
						 dataset.addValue(reportLineData[i].getTotalIncome(), series2, dfs.getMonths()[Integer.parseInt(reportLineData[i].getIntervalName())-1]);
					} else {
						 dataset.addValue(Math.abs(reportLineData[i].getTotalIncome()-reportLineData[i].getIncomePaid()), series2, dfs.getMonths()[Integer.parseInt(reportLineData[i].getIntervalName())-1]);
					}
				} else {
					if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)) {
						//must divide by 3600 to get the number of hours from the number of seconds
						dataset.addValue(reportLineData[i].getTotalIncome()/3600/reportLineData[i].getTotalNo(), series2, month); 
					} else {
						if(reportLineData[i].getTotalNo()==0)
							dataset.addValue(0, series2, month);
						else
							dataset.addValue(reportLineData[i].getTotalIncome()/reportLineData[i].getTotalNo(), series2, month);
					}
				}
			}
			else {
				if(showBoth){
					dataset.addValue(0, series1, month);
					dataset.addValue(0, series2, month);
				} else {
//					must devide by 3600 to get the number of hours from the number of seconds
					if(reportLineData[i]==null)
						dataset.addValue(0, series2, month);
					else {
						if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)) {
							dataset.addValue(reportLineData[i].getTotalIncome()/3600/reportLineData[i].getTotalNo(), series2, month);
						} else {
							if(reportLineData[i].getTotalNo()==0)
								dataset.addValue(0, series2, month);
							else 
								dataset.addValue(reportLineData[i].getTotalIncome()/reportLineData[i].getTotalNo(), series2, month);
						}
					}
						
				}
			}
		}
		return dataset;
	}
	
	private CategoryDataset getCategoryDatasetMonth(HttpServletRequest request) throws Exception{
		//row keys...
		String series1 = "Searches Ordered";
		String series2 = "Searches Done";
		if(beanName.equals(ThroughputOpCode.INCOME_BEAN)){
			series2 = "Due";
			series1 = "Paid";
		}
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		HttpSession session = request.getSession();
		
		//getting current user and community
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		//UserAttributes ua = currentUser.getUserAttributes();
		int commId = -1;
		int payrateType = 0;
		boolean isTSAdmin = UserUtils.isTSAdmin(currentUser.getUserAttributes());
		if (isTSAdmin) {
			if (beanName.equals(ThroughputOpCode.INCOME_BEAN)){
				series1 = "Received";
			}
			payrateType = 1;
		}
		
		ReportLineData[] reportLineData = null;
		ReportLineData[] reportLineDataAux = null;
		
		ThroughputBean thBean = (ThroughputBean)session.getAttribute(beanName);
		if(thBean==null){
			thBean = new ThroughputBean();
		}
		int yearReport = Integer.parseInt((String)request.getSession().getAttribute("yearReport"));
		int monthReport = Integer.parseInt((String)request.getSession().getAttribute("monthReport"));
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		if(groupId<0) groupId = -1;				//if we've clicked "Other" this would decrease below -1
		commId = Integer.parseInt(thBean.getSelectCommunities());
		if(commId<0) commId = -1;				//if we've clicked "Other" this would decrease below -1
		int productId = Integer.parseInt(thBean.getSelectProducts());
		String[] strArray = null;
		int sel = Integer.parseInt(thBean.getSelectStates());
		if( sel < 0 ){
			strArray = thBean.getMultiStates();			//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportState = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportState[i] = Integer.parseInt(strArray[i]);
				}	
			}
			else {
				reportState = new int[1];
				reportState[0] = sel;
			}
		}
		else {
			reportState = new int[1];
			reportState[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectCounties());
		if( sel < 0 ){
			strArray = thBean.getMultiCounties();					//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportCounty = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportCounty[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportCounty = new int[1];
				reportCounty[0] = sel;
			}
		}
		else {
			reportCounty = new int[1];
			reportCounty[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectAbstractors());
		if( sel < 0 ){
			strArray = thBean.getMultiAbstractors();				//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportAbstractor = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportAbstractor[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportAbstractor = new int[1];
				reportAbstractor[0] = sel;
			}
		}
		else {
			reportAbstractor = new int[1];
			reportAbstractor[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectAgents());
		if( sel < 0 ){
			strArray = thBean.getMultiAgents();			//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportAgent = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportAgent[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportAgent = new int[1];
				reportAgent[0] = sel;
			}
		}
		else {
			reportAgent = new int[1];
			reportAgent[0] = sel;
		}
		reportCompanyAgent = thBean.getMultiCompaniesAgents();
		Long[] count = null;
		GregorianCalendar c = new GregorianCalendar(yearReport,monthReport-1,1);
		if(showBoth){
			reportLineData = getMonthlyData(reportCounty, reportAbstractor, reportAgent, reportState, 
					reportCompanyAgent, commId, groupId, payrateType, invoice, productId, yearReport, monthReport);
		} else {
			if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)){
				Vector<Double> min = new Vector<Double>(), max = new Vector<Double>();
				Vector<String> years = new Vector<String>();
				double above, below;	//we will keep data between above and below(below is the lower limit)
				count = getMinAndMaxMonthly(reportCounty, reportAbstractor, reportAgent, reportState, 
						reportCompanyAgent, commId, groupId, productId, yearReport, monthReport,min, max,years);
				if(count==null)			//defensive
					return dataset;
				//System.err.println("MIN: " + min);
				//System.err.println("MAX: " + max);
				//System.err.println("CNT: " + count);
				reportLineData = new ReportLineData[count.length];
				for (int i = 0; i < count.length; i++) {
					//above = max.get(i).doubleValue()-min.get(i).doubleValue();	//calculez intervalul maxim
					//below = above * 5 / 100 + min.get(i).doubleValue();		//sub 5%
					//above = above * 95/ 100 + min.get(i).doubleValue();		//peste 95%
					 below = -1.0;
					 above = max.get(i).doubleValue()+100.0;
					//below = Double.parseDouble(min.get(i).toString())-0.1;
					//above = Double.parseDouble(max.get(i).toString())+0.1;
					if(years.get(i)!=null && !years.get(i).equals("")) {
						reportLineDataAux = getLineMonthlyData(reportCounty, reportAbstractor, reportAgent, 
								reportState, reportCompanyAgent,  
								commId, groupId, payrateType, productId,yearReport,monthReport, years.get(i),below,above);
						if(reportLineDataAux.length>0)
							reportLineData[i] = reportLineDataAux[i];
						else 
							reportLineData[i] = null;
					} else 
						reportLineData[i] = null;	
				}
			} else {
				reportLineData = getLineMonthlyData(reportCounty, reportAbstractor, reportAgent, reportState, 
						reportCompanyAgent, commId, groupId, payrateType, productId, yearReport, monthReport, "0", 0, 0);
			}
		}
		if(reportLineData==null || reportLineData.length==0)
			return dataset;
		int reportLength = reportLineData.length;
		//System.err.println("Size " + reportLength);
		Long graphDataAllNoOfSearches[] = new Long[reportLength];		//pentru cautarile cerute
		Long graphDataTotalNo[] = new Long[reportLength];				//pentru cautarile cerute rezolvate
		
		for (int i = 0; i < reportLength; i++) {
			graphDataAllNoOfSearches[i] = new Long(0);
			graphDataTotalNo[i] = new Long(0);
		}
		
		for (int i = 0; i < c.getActualMaximum(Calendar.DAY_OF_MONTH); i++){
			if (reportLineData[i]!=null) {
				if(showBoth){
					dataset.addValue(reportLineData[i].getIncomePaid(), series1, new Integer(i + 1).toString());
					if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)) {
						 dataset.addValue(reportLineData[i].getTotalIncome(), series2, new Integer(i + 1).toString());
					} else {
						 dataset.addValue(Math.abs(reportLineData[i].getTotalIncome()-reportLineData[i].getIncomePaid()), series2, new Integer(i + 1).toString());
					}
				} else {
					if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)) {
//						must devide by 3600 to get the number of hours from the number of seconds
						dataset.addValue(reportLineData[i].getTotalIncome()/3600/reportLineData[i].getTotalNo(), series2, new Integer(i + 1).toString()); 
					} else {
						if(reportLineData[i].getTotalNo()==0)
							dataset.addValue(0, series2, new Integer(i + 1).toString());
						else
							dataset.addValue(reportLineData[i].getTotalIncome()/reportLineData[i].getTotalNo(), series2, new Integer(i + 1).toString());
					}
					
				}
			}
			else{
				if(showBoth){
					dataset.addValue(0, series1, new Integer(i + 1).toString());
					dataset.addValue(0, series2, new Integer(i + 1).toString());
				} else {
					dataset.addValue(0, series2, new Integer(i + 1).toString());
				}
			}
		}
		return dataset;
	}
	
	private CategoryDataset getCategoryDatasetDirectly(HttpServletRequest request) throws Exception{
		// row keys...
		String series1 = "Searches Ordered";
		String series2 = "Searches Done";
		if(beanName.equals(ThroughputOpCode.INCOME_BEAN)){
			series2 = "Due";
			series1 = "Paid";
		}
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		HttpSession session = request.getSession();
		
//		getting current user and community
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
//		UserAttributes ua = currentUser.getUserAttributes();
		int commId = -1;
		int payrateType = 0;
		boolean isTSAdmin = UserUtils.isTSAdmin(currentUser.getUserAttributes());
		if (isTSAdmin) {
			if (beanName.equals(ThroughputOpCode.INCOME_BEAN)){
				series1 = "Received";
			}
			payrateType = 1;
		}
		
		ReportLineData[] reportLineData = new ReportLineData[0];
		ReportLineData[] reportLineDataAux = new ReportLineData[0];
				
		ThroughputBean thBean = (ThroughputBean)session.getAttribute(beanName);
		if(thBean==null){
			thBean = new ThroughputBean();
		}
		
		int groupId = Integer.parseInt(thBean.getSelectGroups());
		commId = Integer.parseInt(thBean.getSelectCommunities());
		if(groupId<0) groupId = -1;				//if we've clicked "Other" this would decrease below -1
		if(commId<0) commId = -1;				//if we've clicked "Other" this would decrease below -1
		int productId = Integer.parseInt(thBean.getSelectProducts());
		//reportState[0] = Integer.parseInt(thBean.getSelectStates());
		String[] strArray = null;
		int sel = Integer.parseInt(thBean.getSelectStates());
		if( sel < 0 ){
			strArray = thBean.getMultiStates();			//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportState = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportState[i] = Integer.parseInt(strArray[i]);
				}
				
			}
			else {
				reportState = new int[1];
				reportState[0] = sel;
			}
		}
		else {
			reportState = new int[1];
			reportState[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectCounties());
		if( sel < 0 ){
			strArray = thBean.getMultiCounties();					//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportCounty = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportCounty[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportCounty = new int[1];
				reportCounty[0] = sel;
			}
		}
		else {
			reportCounty = new int[1];
			reportCounty[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectAbstractors());
		if( sel < 0 ){
			strArray = thBean.getMultiAbstractors();				//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportAbstractor = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportAbstractor[i] = Integer.parseInt(strArray[i]);
				}
			}
			else {
				reportAbstractor = new int[1];
				reportAbstractor[0] = sel;
			}
		}
		else {
			reportAbstractor = new int[1];
			reportAbstractor[0] = sel;
		}
		sel = Integer.parseInt(thBean.getSelectAgents());
		if( sel < 0 ){
			strArray = thBean.getMultiAgents();			//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportAgent = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportAgent[i] = Integer.parseInt(strArray[i]);
				}
			} else {
				reportAgent = new int[1];
				reportAgent[0] = sel;
			}
		}
		else {
			reportAgent = new int[1];
			reportAgent[0] = sel;
		}
		reportCompanyAgent = thBean.getMultiCompaniesAgents();
		Long[] count = null;
		if(showBoth){
			reportLineData = getGeneralData(reportCounty, reportAbstractor, reportAgent, reportState, 
					reportCompanyAgent, commId, groupId, payrateType, productId);
		} else {
			if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)){
				Vector<Double> min = new Vector<Double>(), max = new Vector<Double>();
				Vector<String> years = new Vector<String>();
				double above, below;	//we will keep data between above and below(below is the lower limit)
				count = getMinAndMax(reportCounty, reportAbstractor, reportAgent, reportState, 
						reportCompanyAgent, commId, groupId, productId, min, max,years,-1,-1);
				if(count==null)			//defensive
					return dataset;
				//System.err.println("MIN: " + min);
				//System.err.println("MAX: " + max);
				//System.err.println("CNT: " + count);
							
				reportLineData = new ReportLineData[count.length];
				
				for (int i = 0; i < count.length; i++) {
					
					//above = max.get(i).doubleValue()-min.get(i).doubleValue();	//calculez intervalul maxim
					//below = above * 5 / 100 + min.get(i).doubleValue();		//sub 5%
					//above = above * 95/ 100 + min.get(i).doubleValue();		//peste 95%
					below = -1.0;
					above = max.get(i).doubleValue()+100.0;
					//below = Double.parseDouble(min.get(i).toString())-0.1;
					//above = Double.parseDouble(max.get(i).toString())+0.1;
					
					if(years.get(i)!=null && !years.get(i).equals("")) {
					reportLineDataAux = getLineGeneralData(reportCounty, reportAbstractor, reportAgent, 
							reportState, reportCompanyAgent, 
							commId, groupId, payrateType, productId,years.get(i),below,above);
					if(reportLineDataAux.length>0)
						reportLineData[i] = reportLineDataAux[0];
					else 
						reportLineData[i] = null;
				} else 
					reportLineData[i] = null;			
				}
			} else {
				reportLineData = getLineGeneralData(reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, commId, groupId, payrateType, productId, "0", 0, 0);
			}
		}
		if(reportLineData==null || reportLineData.length==0)
			return dataset;
		
		int reportLength = reportLineData.length;
		//System.err.println("Size " + reportLength);
		//Long graphDataAllNoOfSearches[] = new Long[reportLength];		//pentru cautarile cerute
		//Long graphDataTotalNo[] = new Long[reportLength];				//pentru cautarile cerute rezolvate
		
		//for (int i = 0; i < reportLength; i++) {
		//	graphDataAllNoOfSearches[i] = new Long(0);
		//	graphDataTotalNo[i] = new Long(0);
		//}
		
		
		
		for (int i = 0; i < reportLength; i++) 
			 if (reportLineData[i]!=null) {
				 //graphDataAllNoOfSearches[i] = new Long(reportLineData[i].getAllNoOfSearches());
				 //graphDataTotalNo[i] = new Long(reportLineData[i].getTotalNo());
				 if(showBoth){
					 dataset.addValue(reportLineData[i].getIncomePaid(), series1, reportLineData[i].getIntervalName());
					 if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)) {
						 dataset.addValue(reportLineData[i].getTotalIncome(), series2, reportLineData[i].getIntervalName());
					 } else {
						 dataset.addValue(Math.abs(reportLineData[i].getTotalIncome()-reportLineData[i].getIncomePaid()), series2, reportLineData[i].getIntervalName());
					 }
				 } else {
					 if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)) {
						 //must devide by 3600 to get the number of hours from the number of seconds
						 int year = Integer.parseInt(reportLineData[i].getIntervalName());
						 dataset.addValue(reportLineData[i].getTotalIncome()/3600/reportLineData[i].getTotalNo(), series2, reportLineData[i].getIntervalName());
					 } else {
						 if(reportLineData[i].getTotalNo()==0)
							 dataset.addValue(0, series2, reportLineData[i].getIntervalName());
						 else
							 dataset.addValue(reportLineData[i].getTotalIncome()/reportLineData[i].getTotalNo(), series2, reportLineData[i].getIntervalName());
					 }
					 
				 }
			 }
		
		return dataset;
	}

	/*
	private void loadAttribute(HttpServletRequest request, String attribute) {
		String aux[];
		
		aux = request.getParameterValues(attribute);
		if(attribute.equals(RequestParams.REPORTS_STATE)){
			if(aux != null){
				reportState = new int[aux.length];
				for (int i = 0; i < aux.length; i++) {
					reportState[i] = Integer.parseInt(aux[i]);
				}
			}
		}
		else if (attribute.equals(RequestParams.REPORTS_COUNTY)){
			if(aux != null){
				reportCounty = new int[aux.length];
				for (int i = 0; i < aux.length; i++) {
					reportCounty[i] = Integer.parseInt(aux[i]);
				}
			}
			
		}
		else if (attribute.equals(RequestParams.REPORTS_AGENT)){
			if(aux != null){
				reportAgent = new int[aux.length];
				for (int i = 0; i < aux.length; i++) {
					reportAgent[i] = Integer.parseInt(aux[i]);
				}
			}
		}
		else if (attribute.equals(RequestParams.REPORTS_ABSTRACTOR)){
			if(aux != null){
				reportAbstractor = new int[aux.length];
				for (int i = 0; i < aux.length; i++) {
					reportAbstractor[i] = Integer.parseInt(aux[i]);
				}
			}
		}
	}
	*/
	
	public String getChartViewerBar(HttpServletRequest request, HttpServletResponse response, String chartType) {
		CategoryDataset dataset = null;
		String label = "";
		this.chartType = chartType;
		if(chartType.contains("Stacked")){
			beanName = ThroughputOpCode.INCOME_BEAN;
			//System.err.println("Mda chartType:" + chartType);
		} else {
			//System.err.println("Mnu chartType:" + chartType);
			beanName = ThroughputOpCode.THROUGHPUT_BEAN;
		}
		showBoth = true;
		if(this.chartType.equals(ThroughputOpCode.TIME_GENERAL) || chartType.equals(ThroughputOpCode.STACKED_TIME_GENERAL)){
			try {
				dataset = getCategoryDatasetDirectly(request);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
			label = "Years";
		} else if(this.chartType.equals(ThroughputOpCode.TIME_YEARS) || chartType.equals(ThroughputOpCode.STACKED_TIME_YEARS)){
			label = "Months of " + request.getParameter(RequestParams.REPORTS_YEAR);
			try {
				dataset = getCategoryDatasetYear(request);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		} else if(this.chartType.equals(ThroughputOpCode.TIME_MONTHS) || chartType.equals(ThroughputOpCode.STACKED_TIME_MONTHS)){
			DateFormatSymbols dfs = new DateFormatSymbols();
			String month = dfs.getMonths()[Integer.parseInt(request.getParameter(RequestParams.REPORTS_MONTH))-1];
			label = "Days of " + month + ", " + request.getParameter(RequestParams.REPORTS_YEAR);
			try {
				dataset = getCategoryDatasetMonth(request);
			} catch (Exception e) {
				dataset = null;
				e.printStackTrace();
			}
		} else
			dataset = null;

		JFreeChart chart = null;
		//System.err.println("BAR: Bean name:" + beanName + " chart Type: " + chartType);
		if(beanName.equals(ThroughputOpCode.INCOME_BEAN)){// create the chart...
			chart = ChartFactory.createStackedBarChart("", // chart title
					label, // range axis label
					"Income ($)", // range axis label
					dataset, // data
					PlotOrientation.VERTICAL, // orientation
					true, // include legend
					true, // tooltips?
					false // URLs?
					);
			
		} else { 
			chart = ChartFactory.createBarChart("", // chart title
					label, // range axis label
					"Number of searches", // range axis label
					dataset, // data
					PlotOrientation.VERTICAL, // orientation
					true, // include legend
					true, // tooltips?
					false // URLs?
					);
		}
		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);
				
		LegendTitle legendTitle = chart.getLegend();
		legendTitle.setBorder(BlockBorder.NONE);
		legendTitle.setItemFont(new Font(null,Font.BOLD,12));
		
		//chart.get
				
		
		// get a reference to the plot for further customisation...
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setDomainGridlinesVisible(true);
		
		
		// set the range axis to display integers only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setLabelFont(rangeAxis.getLabelFont().deriveFont(Font.BOLD));
		
		plot.getDomainAxis().setLabelFont(plot.getDomainAxis().getLabelFont().deriveFont(Font.BOLD));

		// disable bar outlines...
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setDrawBarOutline(false);
		
		if(chartType.equals(ThroughputOpCode.TIME_GENERAL)){
			renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
					"Go to annual throughput report for year {1}, searches {2}",
					NumberFormat.getInstance()));
		} else if(chartType.equals(ThroughputOpCode.TIME_YEARS)){
			renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
					"Go to monthly throughput report for {1}, searches {2}",
					NumberFormat.getInstance()));
		} else if(chartType.equals(ThroughputOpCode.TIME_MONTHS)){
			renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
					"Go to daily throughput report for day {1}, searches {2}",
					NumberFormat.getInstance()));
		} else if(chartType.equals(ThroughputOpCode.STACKED_TIME_GENERAL)){
			renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
					"Go to annual income report for year {1}, current value $ {2}",
					NumberFormat.getInstance()));
		} else if(chartType.equals(ThroughputOpCode.STACKED_TIME_YEARS)){
			renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
					"Go to monthly income report for {1}, current value $ {2}",
					NumberFormat.getInstance()));
		} else if(chartType.equals(ThroughputOpCode.STACKED_TIME_MONTHS)){
			renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
					"Go to daily income report for day {1}, current value $ {2}",
					NumberFormat.getInstance()));
		}
		
		// set up gradient paints for series...
		GradientPaint gp0, gp1;
		if(beanName.equals(ThroughputOpCode.THROUGHPUT_BEAN)){
			gp0 = new GradientPaint(0.0f, 0.0f, new Color(0,107,160), 0.0f,
					0.0f, new Color(0,107,160));
			gp1 = new GradientPaint(0.0f, 0.0f, new Color(93,177,95), 0.0f,
					0.0f, new Color(93,177,95));
		} else {
			gp0 = new GradientPaint(0.0f, 0.0f, new Color(93,177,95), 0.0f,
					0.0f, new Color(93,177,95));
			gp1 = new GradientPaint(0.0f, 0.0f, new Color(255,85,85), 0.0f,
					0.0f, new Color(255,85,85));
			renderer.setDrawBarOutline(true);
		}

		renderer.setSeriesPaint(0, gp0);
		renderer.setSeriesPaint(1, gp1);

		renderer.setItemMargin(0.02); // space between series
		

		if(chartType.equals(ThroughputOpCode.TIME_GENERAL) || chartType.equals(ThroughputOpCode.STACKED_TIME_GENERAL)){
			renderer.setItemURLGenerator(new StandardLinkGenerator("javascript:throughputYear(parseInt(","));",null));
		}
		else if(chartType.equals(ThroughputOpCode.TIME_YEARS) || chartType.equals(ThroughputOpCode.STACKED_TIME_YEARS) ){
			HashMap<String, String> months = new HashMap<String, String>();
			months.put("January", "1");
			months.put("February", "2");
			months.put("March", "3");
			months.put("April", "4");
			months.put("May", "5");
			months.put("June", "6");
			months.put("July", "7");
			months.put("August", "8");
			months.put("September", "9");
			months.put("October", "10");
			months.put("November", "11");
			months.put("December", "12");
			renderer.setItemURLGenerator(new StandardLinkGenerator("javascript:throughputMonth(parseInt(","));",months));
		} else if(chartType.equals(ThroughputOpCode.TIME_MONTHS)|| chartType.equals(ThroughputOpCode.STACKED_TIME_MONTHS)){
			renderer.setItemURLGenerator(new StandardLinkGenerator("javascript:reportDay(parseInt(","));",null));
		} else {
			//System.err.println("This shouldn't be printed");
			renderer.setItemURLGenerator(null);
		}

		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions
				.createUpRotationLabelPositions(Math.PI / 6.0));
		
		//marginile din stanga si dreapta pentru grafic (distanta de la axa y la primul bar)
		domainAxis.setLowerMargin(0.02);
		domainAxis.setUpperMargin(0.02);
		
		
		// OPTIONAL CUSTOMISATION COMPLETED.

		ChartRenderingInfo info = null;
		HttpSession session = request.getSession();
		double rnd = Math.random();
		try {

			// Create RenderingInfo object
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart.createBufferedImage(1200, 500, 1200, 500, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			session.setAttribute("firstChartImage"+chartType + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "imageMap"+chartType, info, false);
			writer.flush();

		} catch (Exception e) {
			// handel your exception here
			e.printStackTrace();
		}

		String pathInfo = "http://";
		pathInfo += request.getServerName();
		int port = request.getServerPort();
		pathInfo += ":" + String.valueOf(port);
		pathInfo += request.getContextPath();
		String chartViewer = pathInfo + "/ChartViewer?chartName=firstChartImage"+chartType+rnd;
		
		//generateReport();
		
		return chartViewer;
	}
	
	public String getChartViewerLine(HttpServletRequest request, HttpServletResponse response, String chartType) {
		
		CategoryDataset dataset;
		this.chartType = chartType;
		String label_domain = "";
		String label_range = "";
		String title = "";
		if(chartType.contains("Stacked")){
			beanName = ThroughputOpCode.INCOME_BEAN;
			label_range = "Search Fee ($)";
			title = "Search Fee Average";
		} else {
			beanName = ThroughputOpCode.THROUGHPUT_BEAN;
			label_range = "Time (in hours)";
			title = "Turn Around Average";
		}
		showBoth = false;
		this.chartType = chartType;
		
		if(chartType.equals(ThroughputOpCode.TIME_GENERAL) || chartType.equals(ThroughputOpCode.STACKED_TIME_GENERAL)){
			label_domain = "Years";
			try {
				dataset = getCategoryDatasetDirectly(request);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		} else if(chartType.equals(ThroughputOpCode.TIME_YEARS) || chartType.equals(ThroughputOpCode.STACKED_TIME_YEARS)){
			label_domain = "Months " + request.getParameter(RequestParams.REPORTS_YEAR);
			try {
				dataset = getCategoryDatasetYear(request);
			} catch (Exception e) {
				dataset = null;
				e.printStackTrace();
			} 
		} else if(chartType.equals(ThroughputOpCode.TIME_MONTHS) || chartType.equals(ThroughputOpCode.STACKED_TIME_MONTHS)){
			DateFormatSymbols dfs = new DateFormatSymbols();
			String month = dfs.getMonths()[Integer.parseInt(request.getParameter(RequestParams.REPORTS_MONTH))-1];
			label_domain = "Days of " + month + ", " + request.getParameter(RequestParams.REPORTS_YEAR);
			try {
				dataset = getCategoryDatasetMonth(request);
			} catch (Exception e) {
				dataset = null;
				e.printStackTrace();
			}
		} else
			dataset = null;
		
		//System.err.println("Bean name:" + beanName + " chart Type: " + chartType);
		// create the chart...
		JFreeChart chart = ChartFactory.createLineChart(title, // chart
				label_domain, // domain axis label
				label_range, // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips?
				false // URLs?
				);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);
				
		LegendTitle legendTitle = chart.getLegend();
		legendTitle.setBorder(BlockBorder.NONE);
		legendTitle.setItemFont(new Font(null,Font.BOLD,12));
		
		if(beanName == ThroughputOpCode.INCOME_BEAN)
			chart.removeLegend();
		//chart.get
				
		
		// get a reference to the plot for further customisation...
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setDomainGridlinesVisible(true);
		
		
		// set the range axis to display integers only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		//rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setLabelFont(rangeAxis.getLabelFont().deriveFont(Font.BOLD));
		
		plot.getDomainAxis().setLabelFont(plot.getDomainAxis().getLabelFont().deriveFont(Font.BOLD));

		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));
		
		//marginile din stanga si dreapta pentru grafic (distanta de la axa y la primul bar)
		domainAxis.setLowerMargin(0.02);
		domainAxis.setUpperMargin(0.02);
		
		LineAndShapeRenderer renderer = (LineAndShapeRenderer)plot.getRenderer();
		renderer.setSeriesStroke(0, new BasicStroke(3.0f));
		GradientPaint gp0 = new GradientPaint(0.0f, 0.0f, new Color(93,177,95), 0.0f,
				0.0f, new Color(93,177,95));
		renderer.setSeriesPaint(0, gp0);

		
		// OPTIONAL CUSTOMISATION COMPLETED.

		ChartRenderingInfo info = null;
		HttpSession session = request.getSession();
		double rnd = Math.random();
		try {

			// Create RenderingInfo object
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart.createBufferedImage(1200, 500, 1200, 500, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			//session.setAttribute("lineChartImage"+chartType, null);
			session.setAttribute("lineChartImage"+chartType + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "imageMapLine"+chartType, info, false);
			writer.flush();

		} catch (Exception e) {
			// handel your exception here
			e.printStackTrace();
		}

		String pathInfo = "http://";
		pathInfo += request.getServerName();
		int port = request.getServerPort();
		pathInfo += ":" + String.valueOf(port);
		pathInfo += request.getContextPath();
		String chartViewer = pathInfo + "/ChartViewer?chartName=lineChartImage"+chartType+rnd;
		
		//generateReport();
		
		return chartViewer;
	}
	
}


