package ro.cst.tsearch.reports.throughputs;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;
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
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleEdge;

import ro.cst.tsearch.community.CategoryAttributes;
import ro.cst.tsearch.community.CategoryUtils;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityFilter;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.procedures.GraphicReportProcedure;
import ro.cst.tsearch.database.procedures.ProcedureManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;
import com.stewart.ats.user.UserRestrictionsI;

public class GraphicReportBean {
	
	protected static final Category logger = Logger.getLogger(GraphicReportBean.class);
	
	private HashMap<String, Object> graphicData = null;
	private ThroughputBean throughputBean = null;
	private User currentUser = null;
	private int[] reportState = {-1};
	private int[] reportCounty = {-1};
	private int[] reportAgent = {-1};
	private int[] reportAbstractor = {-1};
	private int[] reportStatus = {-1};
	private int[] reportProducts = {-1};
	private int[] reportCommunities = {-1};
	private String[] reportCompanyAgent = {"-1"}; 
	private String pathInfo = "http://";
	private String tooltipText = null;
	private String argGoToPagePrefix = "javascript:goToPage('";
	private GraphicReportProcedure.CHART_TYPES chartType = null;
	GraphicReportProcedure.INTERVAL_TYPES intervalType = null;
	private String barGraphicLabel = null;
	private String barToolTipGeneratorText = null;
	private String barSeries1 = null;
	private String barSeries2 = null;
	private String barGoToPagePrefix = null;
	
	private String lineGraphicTitle = null;
	private String lineGraphicLabelRange = null;
	private static HashMap<String, String> monthNameMap = new HashMap<String, String>(){
		private static final long serialVersionUID = 1L;

	{
		put("January", "1");
		put("February", "2");
		put("March", "3");
		put("April", "4");
		put("May", "5");
		put("June", "6");
		put("July", "7");
		put("August", "8");
		put("September", "9");
		put("October", "10");
		put("November", "11");
		put("December", "12");
	}};
	
	public GraphicReportBean() {
		graphicData  = new HashMap<String, Object>();
	}
	
	public void loadData (HttpServletRequest request, ThroughputBean throughputBean, 
			GraphicReportProcedure.CHART_TYPES chartType, GraphicReportProcedure.INTERVAL_TYPES intervalType){
		GraphicReportProcedure reportProcedure = (GraphicReportProcedure)ProcedureManager
				.getInstance().getProcedure(GraphicReportProcedure.SP_NAME);
		
		long startLoadTime = System.currentTimeMillis();
		User currentUser = (User) request.getSession().getAttribute(SessionParams.CURRENT_USER);
		
		this.throughputBean = throughputBean;
		this.currentUser = currentUser;
		this.chartType = chartType;
		this.intervalType = intervalType;
		UserAttributes ua = currentUser.getUserAttributes();
		boolean isTSAdmin = ua.isTSAdmin();
		
		loadAttribute(throughputBean, RequestParams.REPORTS_STATE);
		loadAttribute(throughputBean, RequestParams.REPORTS_COUNTY);
		loadAttribute(throughputBean, RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(throughputBean, RequestParams.REPORTS_AGENT);
		loadAttribute(throughputBean, RequestParams.REPORTS_STATUS);
		loadAttribute(throughputBean, RequestParams.REPORTS_COMPANY_AGENT);
		loadAttribute(throughputBean, RequestParams.SEARCH_PRODUCT_TYPE);
		loadAttribute(throughputBean, RequestParams.REPORTS_COMMUNITY);
		
		String abstractorIdString = Util.getStringFromArray(reportAbstractor);
        String agentIdString = Util.getStringFromArray(reportAgent);
        String countyIdString = Util.getStringFromArray(reportCounty);
        
        UserManagerI userManager = UserManager.getInstance();
        try {
			userManager.getAccess();
			UserRestrictionsI userRestrictions  = userManager.getUser(ua.getID().longValue()).getRestriction();
			abstractorIdString = userRestrictions.getAbstractorListForSql(reportAbstractor);
			agentIdString = userRestrictions.getAgentListForSql(reportAgent);
			
		} catch (Throwable t) {
			logger.error("Error while trying to enforce restriction rules for reports for user " + ua.getID(), t);
		} finally {
			userManager.releaseAccess();
		}
		
		if(ua.isTSAdmin() || ua.isCommAdmin()) {
			//we do not care
		} else {
			Vector<County> allAllowedCounties = ua.getAllowedCountyList();
			if(allAllowedCounties.size() > 0) {	//we have something set here
				HashSet<Integer> allAllowedCountyIds = new HashSet<Integer>();
				int[] allAllowedCountiesInt = new int[allAllowedCounties.size()];
				int i = 0;
				for (County county : allAllowedCounties) {
					allAllowedCountyIds.add(county.getCountyId().intValue());
					allAllowedCountiesInt[i++] = county.getCountyId().intValue();
				}
				countyIdString = "";
				for (i = 0; i < reportCounty.length; i++) {
					if(allAllowedCountyIds.contains(reportCounty[i])) {
						countyIdString += reportCounty[i] + ",";
					}
				}
				if(countyIdString.length() > 0) {
					countyIdString = countyIdString.substring(0, countyIdString.length() - 1);
				}
				
				if(countyIdString.length() == 0) {
					//auto select assigned abstractors
					countyIdString = Util.getStringFromArray(allAllowedCountiesInt);
				}
			}
		}
		
		//setting current time
		Calendar fromCalendar = Calendar.getInstance();
		Calendar toCalendar = Calendar.getInstance();
		fromCalendar.set(Calendar.HOUR_OF_DAY, 00);
		fromCalendar.set(Calendar.MINUTE, 00);
		fromCalendar.set(Calendar.SECOND, 00);
		toCalendar.set(Calendar.HOUR_OF_DAY, 23);
		toCalendar.set(Calendar.MINUTE, 59);
		toCalendar.set(Calendar.SECOND, 59);
		
		
		if(GraphicReportProcedure.INTERVAL_TYPES.GENERAL.equals(intervalType)) {
			fromCalendar.set(2000, 0, 1);
			toCalendar.add(Calendar.DAY_OF_MONTH, 1);
			barGraphicLabel = "Years";
			barGoToPagePrefix = "javascript:throughputYear(parseInt("; 
			if(GraphicReportProcedure.CHART_TYPES.THROUGHPUT.equals(chartType)){
				barToolTipGeneratorText = "Go to annual throughput report for year {1}, searches {2}";
			} else if (GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)){
				barToolTipGeneratorText = "Go to annual income report for year {1}, current value $ {2}";
			}
		} else if(GraphicReportProcedure.INTERVAL_TYPES.YEAR.equals(intervalType)){
			int yearReport = Integer.parseInt((String)request.getSession().getAttribute("yearReport"));
			fromCalendar.set(yearReport, 0, 1);
			toCalendar.set(yearReport, 11, 31);
			barGraphicLabel = "Months of " + yearReport;
			barGoToPagePrefix = "javascript:throughputMonth(parseInt("; 
			if(GraphicReportProcedure.CHART_TYPES.THROUGHPUT.equals(chartType)){
				barToolTipGeneratorText = "Go to monthly throughput report for {1}, searches {2}";
			} else if (GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)){
				barToolTipGeneratorText = "Go to monthly income report for {1}, current value $ {2}";
			}
		} else if(GraphicReportProcedure.INTERVAL_TYPES.MONTH.equals(intervalType)){
			int yearReport = Integer.parseInt((String)request.getSession().getAttribute("yearReport"));
			int monthReport = Integer.parseInt((String)request.getSession().getAttribute("monthReport"));
			fromCalendar.set(yearReport, monthReport - 1, 1);
			toCalendar.set(yearReport, monthReport - 1, fromCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
			DateFormatSymbols dfs = new DateFormatSymbols();
			String month = dfs.getMonths()[monthReport-1];
			barGraphicLabel = "Days of " + month + ", " + yearReport;
			barGoToPagePrefix = "javascript:reportDay(parseInt("; 
			if(GraphicReportProcedure.CHART_TYPES.THROUGHPUT.equals(chartType)){
				barToolTipGeneratorText = "Go to daily throughput report for day {1}, searches {2}";
			} else if (GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)){
				barToolTipGeneratorText = "Go to daily income report for day {1}, current value $ {2}";
			}
		}
		
		int selectedGroup = -1;
		try {
			if(throughputBean.getSelectGroups() != null)
				selectedGroup = Integer.parseInt(throughputBean.getSelectGroups());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(selectedGroup <= 0) {
			selectedGroup = -1;
		}
		int selectedProduct = -1;
		try {
			if(throughputBean.getSelectProducts() != null)
				selectedProduct = Integer.parseInt(throughputBean.getSelectProducts());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(selectedProduct <= 0) {
			selectedProduct = -1;
		}
		int selectedCommunity = -1;
		try {
			if(throughputBean.getSelectCommunities() != null)
				selectedCommunity = Integer.parseInt(throughputBean.getSelectCommunities());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(selectedCommunity <= 0) {
			selectedCommunity = -1;
		}
		
		graphicData = reportProcedure.execute(chartType, 
				"," + countyIdString + ",", 
				"," + abstractorIdString + ",", 
				"," + agentIdString + ",", 
				"," + Util.getStringFromArray(reportState) + ",", 
				StringUtils.convertStringToHexString("," +  Util.getStringFromStringArray(reportCompanyAgent) + ","),
				"," + Util.getStringFromArray(reportStatus) + ",", 
				"," + Util.getStringFromArray(reportProducts) + ",", 
				isTSAdmin, 
				selectedGroup, 
				"," + Util.getStringFromArray(reportCommunities) + ",", 
				fromCalendar, toCalendar, intervalType, 
				throughputBean.getShowProducts(), isTSAdmin && throughputBean.getShowGroups(), 
				isTSAdmin && throughputBean.getShowCommunities(), 
				throughputBean.getShowStates(), throughputBean.getShowCounties(), 
				throughputBean.getShowAgents(), throughputBean.getShowAbstractors());
		
		pathInfo += request.getServerName();
		int port = request.getServerPort();
		pathInfo += ":" + String.valueOf(port);
		pathInfo += request.getContextPath();
		
		if(GraphicReportProcedure.CHART_TYPES.THROUGHPUT.equals(chartType)){
			tooltipText = "{0} = {1}";
			argGoToPagePrefix += "type=" + ThroughputOpCode.THROUGHPUT_BEAN + "&";
			barSeries1 = "Searches Ordered";
			barSeries2 = "Searches Done";
			lineGraphicTitle = "Turn Around Average";
			lineGraphicLabelRange = "Time (in hours)";
		}
		else if (GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)){
			tooltipText = "{0} = $ {1}";
			argGoToPagePrefix += "type=" + ThroughputOpCode.INCOME_BEAN + "&";
			barSeries1 = "Paid";
			barSeries2 = "Due";
			lineGraphicTitle = "Search Fee Average";
			lineGraphicLabelRange = "Search Fee ($)";
		}
		
		logger.debug("GraphicReportBean construction took:" + (System.currentTimeMillis() - startLoadTime));
	}
	
	@SuppressWarnings("unchecked")
	public String getPie (HttpServletRequest request, HttpServletResponse response, String chartCode) {
		long startLoadTime = System.currentTimeMillis();
		HashMap<String, GraphicInfoStructure> extraInfo = new HashMap<String, GraphicInfoStructure>();
		DefaultPieDataset dataset = getPieDataset(chartCode, extraInfo);
		if(dataset == null) {
			return null;
		}
		String lastColor = getLastColor(chartCode);
		
		// create the chart...
		JFreeChart chart = ChartFactory.createPieChart(null, // chart title
				dataset, // data
				true, // include legend
				false, // tooltips?
				false // URLs?
				);
		
		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);
				
		//LegendTitle legendTitle = chart.getLegend();
		//legendTitle.setBorder(BlockBorder.NONE);
		//legendTitle.setItemFont(new Font(null,Font.PLAIN,10));
		//LegendItemSource[] litem = legendTitle.getSources();
		
		
		
		// get a reference to the plot for further customisation...
		PiePlot plot = (PiePlot)chart.getPlot();
		plot.setNoDataMessage("No data available");
		plot.setBackgroundPaint(Color.white);
		//plot.setLabelLinksVisible(false);		
		plot.setLabelGenerator(null);				//alea galbene care orbiteaza in jurul pie-ului :)
		//plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}"));
		//plot.setLabelGenerator(new ATSPieSectionLabelGenerator(chartCode, "{0}"));
		plot.setLegendLabelGenerator(new GraphicPieSectionLabelGenerator("{0} ~ {1}", extraInfo));
		chart.getLegend().setPosition(RectangleEdge.RIGHT);
		
		
		if(dataset.getItemCount()==1)
			plot.setSectionOutlinesVisible(false);
		
		
		HashMap<String, String> colorHashMap = new HashMap<String, String>();

		
		
		DrawingSupplier ds = plot.getDrawingSupplier();
        if (ds != null) {
        	for (Comparable key : (List<Comparable>)dataset.getKeys()) {
        		Color color = (Color)ds.getNextPaint();
                plot.setSectionPaint(key, color);	
                colorHashMap.put(key.toString(), String.valueOf(color.getRGB()));
        	}
        }
		
		
		//if we have a selected color and only one element, we must use that color
		if(lastColor!=null && dataset.getItemCount()==1 && lastColor.length()!=0 && 
				!lastColor.equals(ThroughputBean.INVALID)){
			plot.setSectionPaint(dataset.getKey(0), new Color(Integer.parseInt(lastColor)));
			colorHashMap.put(dataset.getKey(0).toString(), lastColor);
		}
		
		StandardPieLinkGenerator pieLinkGenerator = getPieLinkGenerator(chartCode, colorHashMap, extraInfo);
		if(pieLinkGenerator != null) {
			plot.setURLGenerator(pieLinkGenerator);
		}
		
		plot.setToolTipGenerator(new GraphicPieToolTipGenerator(tooltipText, extraInfo));


		int size = 0;
		
		if(throughputBean.getShowGroups()){
			size = 400;
		} else if (throughputBean.getShowCommunities()){
			size = 400;
		} else {
			size = 300;
		}
		
		ChartRenderingInfo info = null;
		double rnd = Math.random();
		try {

			// Create RenderingInfo object
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart
					.createBufferedImage(size, 300, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			request.getSession().setAttribute("pieChart"+chartCode + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "imageMap" + chartCode, info, false);
			writer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}

		
		String chartViewer = pathInfo + "/ChartViewer?chartName=pieChart"+chartCode+rnd;
		logger.debug("getPie with chart type" + chartCode + " took:" + (System.currentTimeMillis() - startLoadTime));
		return chartViewer;
		
	}
	
	public String getBar(HttpServletRequest request, HttpServletResponse response, String chartCode) {
		long startLoadTime = System.currentTimeMillis();
		CategoryDataset dataset = getBarDataset(chartCode);
		JFreeChart chart = null;
		GradientPaint gp0, gp1;
		if(GraphicReportProcedure.CHART_TYPES.THROUGHPUT.equals(chartType)) {
			chart = ChartFactory.createBarChart("", // chart title
					barGraphicLabel, // range axis label
					"Number of searches", // range axis label
					dataset, // data
					PlotOrientation.VERTICAL, // orientation
					true, // include legend
					true, // tooltip
					false // URLs?
					);
			gp0 = new GradientPaint(0.0f, 0.0f, new Color(0,107,160), 0.0f,
					0.0f, new Color(0,107,160));
			gp1 = new GradientPaint(0.0f, 0.0f, new Color(93,177,95), 0.0f,
					0.0f, new Color(93,177,95));
		} else if(GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)){
			chart = ChartFactory.createStackedBarChart("", // chart title
					barGraphicLabel, // range axis label
					"Income ($)", // range axis label
					dataset, // data
					PlotOrientation.VERTICAL, // orientation
					true, // include legend
					true, // tooltip
					false // URLs?
					);
			gp0 = new GradientPaint(0.0f, 0.0f, new Color(93,177,95), 0.0f,
					0.0f, new Color(93,177,95));
			gp1 = new GradientPaint(0.0f, 0.0f, new Color(255,85,85), 0.0f,
					0.0f, new Color(255,85,85));
		} else {
			return null;
		}
		
		chart.setBackgroundPaint(Color.white);
		
		LegendTitle legendTitle = chart.getLegend();
		legendTitle.setFrame(BlockBorder.NONE);
		legendTitle.setItemFont(new Font(null,Font.BOLD,12));
		
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
		
		renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
				barToolTipGeneratorText, NumberFormat.getInstance()));
		
		// set up gradient paints for series...
		if(GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)){
			renderer.setDrawBarOutline(true);
		}

		renderer.setSeriesPaint(0, gp0);
		renderer.setSeriesPaint(1, gp1);

		renderer.setItemMargin(0.02); // space between series
		
		if(GraphicReportProcedure.INTERVAL_TYPES.YEAR.equals(intervalType)){
			renderer.setBaseItemURLGenerator(new StandardLinkGenerator(barGoToPagePrefix,"));",monthNameMap));
		} else {
			renderer.setBaseItemURLGenerator(new StandardLinkGenerator(barGoToPagePrefix,"));",null));
		}
		
		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions
				.createUpRotationLabelPositions(Math.PI / 6.0));
		
		//marginile din stanga si dreapta pentru grafic (distanta de la axa y la primul bar)
		domainAxis.setLowerMargin(0.02);
		domainAxis.setUpperMargin(0.02);
		
		ChartRenderingInfo info = null;
		double rnd = Math.random();
		try {
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart.createBufferedImage(1200, 500, 1200, 500, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			request.getSession().setAttribute("firstChartImage"+chartCode + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "imageMapBar"+chartCode, info, false);
			writer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String chartViewer = pathInfo + "/ChartViewer?chartName=firstChartImage"+chartCode+rnd;
		logger.debug("getBar with chart type" + chartCode + " took:" + (System.currentTimeMillis() - startLoadTime));
		return chartViewer;
	}
	
	public String getLine(HttpServletRequest request, HttpServletResponse response, String chartCode) {
		long startLoadTime = System.currentTimeMillis();
		CategoryDataset dataset = getBarDataset(chartCode);
		JFreeChart chart = ChartFactory.createLineChart(lineGraphicTitle, // chart
				barGraphicLabel, // domain axis label
				lineGraphicLabelRange, // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips?
				false // URLs?
				);
		
		chart.setBackgroundPaint(Color.white);
		
		LegendTitle legendTitle = chart.getLegend();
		legendTitle.setFrame(BlockBorder.NONE);
		legendTitle.setItemFont(new Font(null,Font.BOLD,12));
		
		if(GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)) {
			chart.removeLegend();
		}
		
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
		
		
		ChartRenderingInfo info = null;
		double rnd = Math.random();
		try {
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart.createBufferedImage(1200, 500, 1200, 500, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			request.getSession().setAttribute("lineChartImage"+chartCode + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "imageMapLine"+chartCode, info, false);
			writer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String chartViewer = pathInfo + "/ChartViewer?chartName=lineChartImage"+chartCode+rnd;
		logger.debug("getLine with chart type" + chartCode + " took:" + (System.currentTimeMillis() - startLoadTime));
		return chartViewer;
	}
	
	/**
	 * Returns a PieURLGenerator (the link that is followed when pressing the pie)
	 * @param chartCode
	 * @param colorHashMap
	 * @param extraInfo
	 * @return
	 */
	private StandardPieLinkGenerator getPieLinkGenerator(String chartCode,
			HashMap<String, String> colorHashMap,
			HashMap<String, GraphicInfoStructure> extraInfo) {
		StandardPieLinkGenerator standardPieLinkGenerator = null;
		if(chartCode.equals(GraphicReportProcedure.GROUP_DATA)){
			standardPieLinkGenerator = new StandardPieLinkGenerator(
					argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.DRILL_GROUPS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_GROUPS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					"&" + ThroughputOpCode.NAME_OBJECT + "=", 
					extraInfo);
		}
		else if(chartCode.equals(GraphicReportProcedure.COMMUNITY_DATA)){
			standardPieLinkGenerator = new StandardPieLinkGenerator(
					argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.DRILL_COMMUNITIES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_COMMUNITIES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
					"&" + ThroughputOpCode.NAME_OBJECT + "=", 
					extraInfo);
		}
		else if(chartCode.equals(GraphicReportProcedure.STATE_DATA)){
			if(throughputBean.getShowAbstractors()){
				standardPieLinkGenerator = new StandardPieLinkGenerator(
						argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.DRILL_STATES + "&" + ThroughputOpCode.SELECT_OBJECT +"="	,
						argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_STATES + "&" + ThroughputOpCode.SELECT_OBJECT +"=", 
						"&" + ThroughputOpCode.NAME_OBJECT + "=", 
						extraInfo);
			}
			else if(extraInfo!=null && extraInfo.size() > 0) {
				//if we are here it means that we should only be able to filter states
				//and if we have already set a state(which mean selectStates is positive) we shouldn't display any link
				if(Integer.parseInt(throughputBean.getSelectStates())<0){
					standardPieLinkGenerator = new StandardPieLinkGenerator(
							argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_STATES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
							"&" + ThroughputOpCode.COLOR_OBJECT + "=",
							extraInfo, colorHashMap);
				} 
				
			}
		}
		else if(chartCode.equals(GraphicReportProcedure.PRODUCT_DATA)){
			if(extraInfo!=null && extraInfo.size() > 0){	
				standardPieLinkGenerator = new StandardPieLinkGenerator(
						argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_PRODUCTS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "=",
						extraInfo,colorHashMap);
			}
		}
		else if(chartCode.equals(GraphicReportProcedure.COUNTY_DATA)){
			if(extraInfo!=null && extraInfo.size() > 0){	
				standardPieLinkGenerator = new StandardPieLinkGenerator(
						argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_COUNTIES + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "=",
						extraInfo,colorHashMap);
			}
		}
		else if(chartCode.equals(GraphicReportProcedure.ABSTRACTOR_DATA)){
			if(extraInfo!=null && extraInfo.size() > 0){	
				standardPieLinkGenerator = new StandardPieLinkGenerator(
						argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_ABSTRACTORS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "=",
						extraInfo,colorHashMap);
			}
		}
		else if(chartCode.equals(GraphicReportProcedure.AGENT_DATA)){
			if(extraInfo!=null && extraInfo.size() > 0){	
				standardPieLinkGenerator = new StandardPieLinkGenerator(
						argGoToPagePrefix + ThroughputOpCode.OPCODE + "=" + ThroughputOpCode.FILTER_AGENTS + "&" + ThroughputOpCode.SELECT_OBJECT +"=",
						"&" + ThroughputOpCode.COLOR_OBJECT + "=",
						extraInfo,colorHashMap);
			}
		}
		return standardPieLinkGenerator;
	}

	private String getLastColor(String chartCode) {
		String lastColor = null;
		if(chartCode.equals(GraphicReportProcedure.STATE_DATA)){
			lastColor = throughputBean.getColorStates();
		}
		else if(chartCode.equals(GraphicReportProcedure.COUNTY_DATA)){
			lastColor = throughputBean.getColorCounties();
		}
		else if(chartCode.equals(GraphicReportProcedure.PRODUCT_DATA)){
			lastColor = throughputBean.getColorProducts();
		}
		else if(chartCode.equals(GraphicReportProcedure.ABSTRACTOR_DATA)){
			lastColor = throughputBean.getColorAbstractors();
		}
		else if(chartCode.equals(GraphicReportProcedure.AGENT_DATA)){
			lastColor = throughputBean.getColorAgents();
		}
		return lastColor;
	}

	@SuppressWarnings("unchecked")
	private DefaultPieDataset getPieDataset(String chartCode, HashMap<String, GraphicInfoStructure> extraInfo) {
		Object rawDataObject = graphicData.get(chartCode);
		if(rawDataObject == null) {
			return new DefaultPieDataset();		//return null;
		}
		DefaultPieDataset dataset = new DefaultPieDataset();
		if(chartCode.equals(GraphicReportProcedure.GROUP_DATA)){
			CategoryAttributes[] sgas;
			try {
				sgas = CategoryUtils.getCategories(CategoryAttributes.CATEGORY_NAME);
			} catch (BaseException e) {
				e.printStackTrace();
				return null;
			}
			int sel = -1;
			String selectOption =  throughputBean.getSelectGroups();
			if (selectOption!=null)
				sel = Integer.parseInt(selectOption);
			
			HashMap<Long, Long> hmData = (HashMap<Long, Long>)rawDataObject;
			List<Map.Entry<Long, Long>> hmDataList = PieReportBean.getSortedMap(hmData, false);
			HashMap<Long, String> groupData = new HashMap<Long, String>();
			
			for (int i = 0; i < sgas.length; i++) {
				groupData.put(new Long(sgas[i].getID().longValue()), sgas[i].getNAME());
			}
			
			if( sel >=0 ) {
				if(hmData.get(new Long(sel)) != null) {
					dataset.setValue(groupData.get(new Long(sel)), hmData.get(new Long(sel)));
					GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(String.valueOf(sel), hmData.get(new Long(sel)).toString());
					extraInfo.put(groupData.get(new Long(sel)), graphicInfoStructure);
				}
			} else {
				dataset = getDatasetFrom(sel, hmDataList, groupData, extraInfo);
			}
		} else if(chartCode.equals(GraphicReportProcedure.COMMUNITY_DATA)){
			int groupId = Integer.parseInt(throughputBean.getSelectGroups());
			CommunityAttributes[] comm;
			try {
				comm = CommunityUtils.getCommunitiesInCategory(new BigDecimal(groupId), new CommunityFilter());
			} catch (BaseException e) {
				e.printStackTrace();
				return null;
			}
			HashMap<Long, Long> hmData = (HashMap<Long, Long>)rawDataObject;
			List<Map.Entry<Long, Long>> hmDataList = PieReportBean.getSortedMap(hmData, false);
			HashMap<Long, String> commData = new HashMap<Long, String>();
			
			for (int i = 0; i < comm.length; i++) {
				commData.put(new Long(comm[i].getID().longValue()), comm[i].getNAME());
			}
			for (int i = 0; i < hmDataList.size(); i++) {
				Map.Entry<Long, Long> element = hmDataList.get(i);
				dataset.setValue(commData.get(element.getKey()), element.getValue());
				GraphicInfoStructure graphicInfoStructure = 
					new GraphicInfoStructure(element.getKey().toString(), 
						element.getKey().toString());
				extraInfo.put(commData.get(element.getKey()), graphicInfoStructure);
				
			}
			String selectOptionAsString =  throughputBean.getSelectCommunities();
			int selectOption = -1;
			if (selectOptionAsString != null) {
				selectOption = Integer.parseInt(selectOptionAsString);
			}
			
			if( selectOption >=0 ) {
				if(hmData.get(new Long(selectOption)) != null) {
					dataset.setValue(commData.get(new Long(selectOption)), hmData.get(new Long(selectOption)));
					GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(String.valueOf(selectOption), hmData.get(new Long(selectOption)).toString());
					extraInfo.put(commData.get(new Long(selectOption)), graphicInfoStructure);
				}
			} else 
				dataset = getDatasetFrom(selectOption,hmDataList,commData, extraInfo);
			
			
		} else if(chartCode.equals(GraphicReportProcedure.STATE_DATA)){
			GenericState[] states = DBManager.getAllStates();
			
			HashMap<Long, String> stateData = new HashMap<Long, String>();
			for (int i = 0; i < states.length; i++) {
				stateData.put(states[i].getId(), states[i].getStateAbv());
			}
			HashMap<Long, Long> hmData = (HashMap<Long, Long>)rawDataObject;
			List<Map.Entry<Long, Long>> hmDataList = PieReportBean.getSortedMap(hmData, false);
			throughputBean.setStateInfoMap(hmDataList);
			
			int selectOption = -1;
			String selectOptionAsString = throughputBean.getSelectStates();
			if (selectOptionAsString!=null)
				selectOption = Integer.parseInt(selectOptionAsString);
			if( selectOption >=0 ) {
				if(hmData.get(new Long(selectOption)) != null) {
					dataset.setValue(stateData.get(new Long(selectOption)), hmData.get(new Long(selectOption)));
					GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(
							String.valueOf(selectOption), hmData.get(new Long(selectOption)).toString());
					extraInfo.put(stateData.get(new Long(selectOption)), graphicInfoStructure);
				}
			} else {
				dataset = getDatasetFrom(selectOption,hmDataList,stateData, extraInfo);
			}
		} else if(chartCode.equals(GraphicReportProcedure.COUNTY_DATA)){
			
			HashMap<Long, Long> hmData = (HashMap<Long, Long>)rawDataObject;
			List<Map.Entry<Long, Long>> hmDataList = PieReportBean.getSortedMap(hmData, false);
			throughputBean.setCountyInfoMap(hmDataList);
			
			HashMap<Long,String> countiesData = new HashMap<Long, String>();
			Collection<CountyWithState> counties = DBManager.getAllCountiesForState(Integer.parseInt(throughputBean.getSelectStates()));
			for(CountyWithState county: counties){
				countiesData.put((long)county.getCountyId(), county.getCountyName());
			}		
			
			int selectOption = -1;
			String selectOptionAsString = throughputBean.getSelectCounties();
			if (selectOptionAsString!=null)
				selectOption = Integer.parseInt(selectOptionAsString);
			
			if(selectOption >= 0){
				if(hmData.get(new Long(selectOption)) != null) {
					dataset.setValue(countiesData.get(new Long(selectOption)), hmData.get(new Long(selectOption)));
					GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(
							String.valueOf(selectOption), hmData.get(new Long(selectOption)).toString());
					extraInfo.put(countiesData.get(new Long(selectOption)), graphicInfoStructure);
				}
			} else {
				dataset = getDatasetFrom(selectOption,hmDataList,countiesData, extraInfo);
			}
			
		} else if(chartCode.equals(GraphicReportProcedure.PRODUCT_DATA)){
			
			int[] communities = new int[1];
			communities[0] = Integer.parseInt(throughputBean.getSelectCommunities());
			String[] commStr = throughputBean.getMultiCommunities(); 
			if (commStr!=null) {
				communities = new int[commStr.length];
				for (int i=0;i<commStr.length;i++)
					communities[i] = Integer.parseInt(commStr[i]);
			}
			Map<Long,String> productData = Products.getAllProductShortNameLength3ForCommunity(communities);
			HashMap<Long, Long> hmData = (HashMap<Long, Long>)rawDataObject;
			List<Map.Entry<Long, Long>> hmDataList = PieReportBean.getSortedMap(hmData, false);
			throughputBean.setProductInfoMap(hmDataList);
			int selectOption = -1;
			String selectOptionAsString = throughputBean.getSelectProducts();
			if (selectOptionAsString!=null)
				selectOption = Integer.parseInt(selectOptionAsString);
			if(selectOption >= 0) {
				if(hmData.get(new Long(selectOption)) != null) {
					dataset.setValue(productData.get(new Long(selectOption)), hmData.get(new Long(selectOption)));
					GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(
							String.valueOf(selectOption), hmData.get(new Long(selectOption)).toString());
					extraInfo.put(productData.get(new Long(selectOption)), graphicInfoStructure);
				}
			} else {
				dataset = getDatasetFrom(selectOption, hmDataList, productData, extraInfo);
			}
		} else if(chartCode.equals(GraphicReportProcedure.ABSTRACTOR_DATA)){
			HashMap<Long, Long> hmData = (HashMap<Long, Long>)rawDataObject;
			UserAttributes[] users = DBManager.getAllUsersFromCategory(Integer.parseInt(throughputBean.getSelectGroups()));
			HashMap<Long,String> abstrData = new HashMap<Long, String>();
			for (int i = 0; i < users.length; i++) {
				//abstrData.put(users[i].getID().longValue(), users[i].getFIRSTNAME() + " " + users[i].getLASTNAME() + " - " + users[i].getLOGIN());
				abstrData.put(users[i].getID().longValue(), users[i].getLOGIN());
			}
			List<Map.Entry<Long, Long>> hmDataList = PieReportBean.getSortedMap(hmData, false);
			throughputBean.setAbstractorInfoMap(hmDataList);
			
			int selectOption = -1;
			String selectAbstractors = throughputBean.getSelectAbstractors();
			if(selectAbstractors!=null) {
				selectOption = Integer.parseInt(selectAbstractors);
			}
			
			if(selectOption>=0){
				if(hmData.get(new Long(selectOption)) != null) {
					dataset.setValue(abstrData.get(new Long(selectOption)), hmData.get(new Long(selectOption)));
					GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(
							String.valueOf(selectOption), hmData.get(new Long(selectOption)).toString());
					extraInfo.put(abstrData.get(new Long(selectOption)), graphicInfoStructure);
				}
			} else {
				dataset = getDatasetFrom(selectOption,hmDataList,abstrData, extraInfo);
			}
			
		} else if(chartCode.equals(GraphicReportProcedure.AGENT_DATA)){
			HashMap<Long, Long> hmData = (HashMap<Long, Long>)rawDataObject;
			UserAttributes[] users = DBManager.getAllUsersFromCategory(Integer.parseInt(throughputBean.getSelectGroups()));
			HashMap<Long,String> agentsData = new HashMap<Long, String>();
			for (int i = 0; i < users.length; i++) {
				//agentsData.put(users[i].getID().longValue(), users[i].getFIRSTNAME() + " " + users[i].getLASTNAME());
				agentsData.put(users[i].getID().longValue(), users[i].getLOGIN());
			}
			//validateData(hmData,agentsData);

			List<Map.Entry<Long, Long>> hmDataList = PieReportBean.getSortedMap(hmData, false);
			throughputBean.setAgentInfoMap(hmDataList);
			
			int selectOption = -1;
			String selectAgents = throughputBean.getSelectAgents();
			
			if(selectAgents!=null) 
				selectOption = Integer.parseInt(selectAgents);
			
			
			if(selectOption>=0){
				if(hmData.get(new Long(selectOption)) != null) {
					dataset.setValue(agentsData.get(new Long(selectOption)), hmData.get(new Long(selectOption)));
					GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(
							String.valueOf(selectOption), hmData.get(new Long(selectOption)).toString());
					extraInfo.put(agentsData.get(new Long(selectOption)), graphicInfoStructure);
				}
			} else {
				dataset = getDatasetFrom(selectOption,hmDataList,agentsData, extraInfo);
			}
		}
			
		return dataset;
	}
	
	@SuppressWarnings("unchecked")
	private CategoryDataset getBarDataset(String chartCode) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		Object rawDataObject = graphicData.get(chartCode);
		if(rawDataObject == null) {
			return null;
		}
		if(chartCode.equals(GraphicReportProcedure.BAR_REPORT_DATA)){
			Map<String, BarDatasetEntry> barData = (TreeMap<String, BarDatasetEntry>)rawDataObject;
			for (Entry<String, BarDatasetEntry> entry : barData.entrySet()) {
				dataset.addValue(entry.getValue().getValueFirstColumn(), barSeries1, entry.getValue().getKey());
				if(GraphicReportProcedure.CHART_TYPES.THROUGHPUT.equals(chartType)){
					dataset.addValue(entry.getValue().getValueSecondColumn(), 
							barSeries2, entry.getValue().getKey());
				} else if (GraphicReportProcedure.CHART_TYPES.INCOME.equals(chartType)){
					dataset.addValue(entry.getValue().getValueSecondColumn() - entry.getValue().getValueSecondColumn(),
							barSeries2, entry.getValue().getKey());
				}
			}
		} else if(chartCode.equals(GraphicReportProcedure.LINE_REPORT_DATA)){
			Map<String, LineDatasetEntry> lineData = (TreeMap<String, LineDatasetEntry>)rawDataObject;
			for (Entry<String, LineDatasetEntry> entry : lineData.entrySet()) {
				dataset.addValue(entry.getValue().getSimpleLineValue(3600), barSeries2, entry.getValue().getKey());
			}
		}
		return dataset;
	}
	
	private DefaultPieDataset getDatasetFrom(int selected, List<Entry<Long, Long>> hmDataList, 
			Map<Long, String> selectedData, HashMap<String, GraphicInfoStructure> extraInfo) {
		
		DefaultPieDataset dataset = new DefaultPieDataset();
		
		if (selected == -1){
			dataset = getDatasetAux(selected, hmDataList, selectedData, extraInfo);
		} else {
			int aux = 0;
			double prag = 0;
			
			//aici stiu ca am selectat other
			//vreau sa elimin din hmData toate elementele care nu sunt continute de selectia other anterioara
			for(int i=-2; i>=selected; i--){
				prag = 0;
				for (int j = 0; j < hmDataList.size(); j++) {
					prag += hmDataList.get(j).getValue();
				}
				prag /= 36;		//prag = prag * 10 / 360
				//trebuie sa elimin acum toate intrarile din hmData cu value > prag
				aux=0;
				for (int j = 0; j < hmDataList.size(); j++) {
					if(hmDataList.get(j).getValue() <= prag){
						//cand ajung sub prag tin minte cate elemente trebuie sa elimin
						aux = j;
						break;
					}
				}
				hmDataList = hmDataList.subList(aux, hmDataList.size());
			}
			dataset = getDatasetAux(selected, hmDataList, selectedData, extraInfo);
			
		}
		return dataset;
	}
	
	private DefaultPieDataset getDatasetAux(int selectOption, List<Map.Entry<Long, Long>> rawData, 
			Map<Long, String> nameData, HashMap<String, GraphicInfoStructure> extraInfo) {
		DefaultPieDataset dataset = new DefaultPieDataset();
		Vector<String> sectionName = new Vector<String>();
		Vector<Long> data = new Vector<Long>();
		
		double limit = 0;
		for (int i = 0; i < rawData.size(); i++) {
			limit += rawData.get(i).getValue();
		}
		limit /= 36;					//limit = limit * 10 / 360;
		
		double other = 0;
		
		for (int i = 0; i < rawData.size(); i++) {
			if(rawData.get(i).getValue() > limit){
				if(nameData.get(rawData.get(i).getKey())==null)
					continue;
				sectionName.add(nameData.get(rawData.get(i).getKey()));	//getting the name for that id
				data.add(rawData.get(i).getValue());
				GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(
						rawData.get(i).getKey().toString(),
						rawData.get(i).getValue().toString());
				extraInfo.put(nameData.get(rawData.get(i).getKey()), graphicInfoStructure);
			}
			else
				other += rawData.get(i).getValue();
		}
		
		if(other>0){
			//pentru cazul cand avem other, voi seta selectia astfel:
			//primul other are valoare -2, al doilea other are valoare -3...
			//deci voi scade 1 din ultima selectie facuta
			//cum initial am -1 (pt nimic selectat) voi avea -2, si tot asa
			//daca selectia va fi pozitiva e clar ca nu se va pune problema lui other pentru ca deja am selectat ceva valid
			sectionName.add("Other");
			GraphicInfoStructure graphicInfoStructure = new GraphicInfoStructure(
					String.valueOf(selectOption-1), 
					String.valueOf(new Double(other).longValue()));
			//if(other<limit)				//setting other to at least 10 degres
				other = limit;
			data.add(new Double(other).longValue());
			extraInfo.put("Other", graphicInfoStructure );	
		}
		
		for (int i = 0; i < data.size(); i++) {
			/*if(sectionName.elementAt(i)==null)
				System.err.println("sectionName.elementAt(i): " + i);
			if(data.elementAt(i)==null)
				System.err.println("data.elementAt(i): " + i);*/
			dataset.setValue(sectionName.elementAt(i), data.elementAt(i));
		}
		return dataset;
	}

	private void loadAttribute(ThroughputBean thBean, String attributeName){
		int sel = 0;
		String[] strArray;
		
		if(attributeName.equals(RequestParams.REPORTS_COMMUNITY)){
			sel = Integer.parseInt(thBean.getSelectCommunities());
			if(sel>0){
				reportCommunities = new int[1];
				reportCommunities[0] = sel;
			} else {
				strArray = thBean.getMultiCommunities();				//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportCommunities = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportCommunities[i] = Integer.parseInt(strArray[i]);
						if(reportCommunities[i] < -1)
							reportCommunities[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_STATE)){
			sel = Integer.parseInt(thBean.getSelectStates());
			if(sel>0){
				reportState = new int[1];
				reportState[0] = sel;
			} else {
				strArray = thBean.getMultiStates();						//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportState = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportState[i] = Integer.parseInt(strArray[i]);
						if(reportState[i] < -1)
							reportState[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_ABSTRACTOR)){
			sel = Integer.parseInt(thBean.getSelectAbstractors());
			if(sel>0){
				reportAbstractor = new int[1];
				reportAbstractor[0] = sel;
			} else {
				strArray = thBean.getMultiAbstractors();				//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportAbstractor = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportAbstractor[i] = Integer.parseInt(strArray[i]);
						if(reportAbstractor[i] < -1)
							reportAbstractor[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.SEARCH_PRODUCT_TYPE)){
			sel = Integer.parseInt(thBean.getSelectProducts());
			if(sel>0){
				reportProducts = new int[1];
				reportProducts[0] = sel;
			} else {
				strArray = thBean.getMultiProducts();					//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportProducts = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportProducts[i] = Integer.parseInt(strArray[i]);
						if(reportProducts[i] < -1)
							reportProducts[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_AGENT)){
			sel = Integer.parseInt(thBean.getSelectAgents());
			if(sel>0){
				reportAgent = new int[1];
				reportAgent[0] = sel;
			} else {
				strArray = thBean.getMultiAgents();						//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportAgent = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportAgent[i] = Integer.parseInt(strArray[i]);
						if(reportAgent[i] < -1)
							reportAgent[i] = -1;
					}
					
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_COUNTY)){
			sel = Integer.parseInt(thBean.getSelectCounties());
			if(sel>0){
				reportCounty = new int[1];
				reportCounty[0] = sel;
			} else {
				strArray = thBean.getMultiCounties();					//get Multi filter select
				if(strArray!=null && strArray.length>0){				//if we have something selected
					reportCounty = new int[strArray.length];
					for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
						reportCounty[i] = Integer.parseInt(strArray[i]);
						if(reportCounty[i] < -1)
							reportCounty[i] = -1;
					}
					
				}
			}
		} else if(attributeName.equals(RequestParams.REPORTS_STATUS)){
			strArray = thBean.getMultiStatus();					//get Multi filter select
			if(strArray!=null && strArray.length>0){				//if we have something selected
				reportStatus = new int[strArray.length];
				for (int i = 0; i < strArray.length; i++) {			//we copy everything for the sql statement 
					reportStatus[i] = Integer.parseInt(strArray[i]);
					if(reportStatus[i] < -1)
						reportStatus[i] = -1;
				}
			}
			
		} else if(attributeName.equals(RequestParams.REPORTS_COMPANY_AGENT)){
			reportCompanyAgent = thBean.getMultiCompaniesAgents();
		}
	}

}
