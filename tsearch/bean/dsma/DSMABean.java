package ro.cst.tsearch.bean.dsma;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.DBLoadInfo;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.TimeGraphicMapper;
import ro.cst.tsearch.utils.DBConstants;

public class DSMABean {
	
	private static final Logger logger = Logger.getLogger(DSMABean.class);
	public static final int maxBandwidth = ServerConfig.getInteger("network.bandwidth")/8/1024;
	private static Map<Integer, Map<String, Float>> correctionFactorsCache = 
		new HashMap<Integer, Map<String,Float>>();
	
	public String getDSMALineGraph(HttpServletRequest request, HttpServletResponse response, int chartType){
		
		long startTime = System.currentTimeMillis();
		Dataset dataset = null;
		
		String label_domain = "";
		String label_range = "";
		double lowerLimit = 0;
		double upperLimit = 100;
		String title = "";
		JFreeChart chart = null;

		String zoomTypeString = request.getParameter("zoom");
		int zoomType = 0;
		try {
			zoomType = Integer.parseInt(zoomTypeString);
		} catch (Exception e) {}
		
		if(chartType==DSMAConstants.CPU_MINUTES){
			title = "Last Hour CPU Usage Graph (instant values)";
			label_range = "% of CPU Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.CPU_GRAPH, DSMAConstants.INTERVAL_HOUR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.CPU_DAY){
			title = "Last Day CPU Usage Graph (10 Minutes Average)";
			label_range = "% of CPU Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.CPU_GRAPH, DSMAConstants.INTERVAL_DAY);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.CPU_WEEK){
			title = "Last Week CPU usage Graph (1 Hour Average)";
			label_range = "% of CPU Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.CPU_GRAPH, DSMAConstants.INTERVAL_WEEK);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.CPU_MONTH){
			title = "Last Month CPU Usage Graph (4 Hour Average)";
			label_range = "% of CPU Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.CPU_GRAPH, DSMAConstants.INTERVAL_MONTH);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.CPU_YEAR){
			title = "Last Year CPU Usage Graph (1 Day Average)";
			label_range = "% of CPU Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.CPU_GRAPH, DSMAConstants.INTERVAL_YEAR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.MEM_MINUTES){
			title = "Last Hour Memory Usage Graph (instant values)";
			label_range = "% of Memory Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.MEM_GRAPH, DSMAConstants.INTERVAL_HOUR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.MEM_DAY){
			title = "Last Day Memory Usage Graph (10 Minutes Average)";
			label_range = "% of Memory Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.MEM_GRAPH, DSMAConstants.INTERVAL_DAY);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.MEM_WEEK){
			title = "Last Week Memory Usage Graph (1 Hour Average)";
			label_range = "% of Memory Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.MEM_GRAPH, DSMAConstants.INTERVAL_WEEK);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.MEM_MONTH){
			title = "Last Week Memory Usage Graph (1 Hour Average)";
			label_range = "% of Memory Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.MEM_GRAPH, DSMAConstants.INTERVAL_MONTH);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.MEM_YEAR){
			title = "Last Year Memory Usage Graph (1 Day Average)";
			label_range = "% of Memory Used";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.MEM_GRAPH, DSMAConstants.INTERVAL_YEAR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.LOAD_MINUTES){
			title = "Last Hour Availability Graph (instant values)";
			label_range = "Availability (%)";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.LOAD_GRAPH, DSMAConstants.INTERVAL_HOUR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.LOAD_DAY){
			title = "Last Day Availability Graph (10 Minutes Average)";
			label_range = "Availability (%)";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.LOAD_GRAPH, DSMAConstants.INTERVAL_DAY);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.LOAD_WEEK){
			title = "Last Week Availability Graph (1 Hour Average)";
			label_range = "Availability (%)";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.LOAD_GRAPH, DSMAConstants.INTERVAL_WEEK);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.LOAD_MONTH){
			title = "Last Month Availability Graph (4 Hour Average)";
			label_range = "Availability (%)";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.LOAD_GRAPH, DSMAConstants.INTERVAL_MONTH);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.LOAD_YEAR){
			title = "Last Year Availability Graph (1 Day Average)";
			label_range = "Availability (%)";
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.LOAD_GRAPH, DSMAConstants.INTERVAL_YEAR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.NETWORK_MINUTES){
			title = "Last Hour Network Utilization Graph (instant values)";
			label_range = "% of Bandwidth Used"/*"Free Bandwidth (KB/s)"*/;
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.NETWORK_GRAPH, DSMAConstants.INTERVAL_HOUR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.NETWORK_DAY){
			title = "Last Day Network Utilization Graph (10 Minutes Average)";
			label_range = "% of Bandwidth Used"/*"Free Bandwidth (KB/s)"*/;
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.NETWORK_GRAPH, DSMAConstants.INTERVAL_DAY);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.NETWORK_WEEK){
			title = "Last Week Network Utilization Graph (1 Hour Average)";
			label_range = "% of Bandwidth Used"/*"Free Bandwidth (KB/s)"*/;
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.NETWORK_GRAPH, DSMAConstants.INTERVAL_WEEK);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.NETWORK_MONTH){
			title = "Last Month Network Utilization Graph (4 Hour Average)";
			label_range = "% of Bandwidth Used"/*"Free Bandwidth (KB/s)"*/;
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.NETWORK_GRAPH, DSMAConstants.INTERVAL_MONTH);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(chartType==DSMAConstants.NETWORK_YEAR){
			title = "Last Year Network Utilization Graph (1 Day Average)";
			label_range = "% of Bandwidth Used"/*"Free Bandwidth (KB/s)"*/;
			label_domain = "Time";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.NETWORK_GRAPH, DSMAConstants.INTERVAL_YEAR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		else if(chartType==DSMAConstants.ORDER_MINUTES_DAY){
			title = "Orders Statistics - 'Daily' Graph";
			lowerLimit = 0;
			upperLimit = 0;
			zoomType = 0;
			label_range = "Number of Orders received";
			label_domain = "Time (last 24 hours)";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.ORDER_GRAPH, DSMAConstants.INTERVAL_DAY);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		}
		else if(chartType==DSMAConstants.ORDER_MINUTES_WEEK){
			title = "Orders Statistics - 'Weekly' Graph";
			lowerLimit = 0;
			upperLimit = 0;
			zoomType = 0;
			label_range = "Number of Orders received";
			label_domain = "Time (last week)";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.ORDER_GRAPH, DSMAConstants.INTERVAL_WEEK);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		}
		else if(chartType==DSMAConstants.ORDER_MINUTES_MONTH){
			title = "Orders Statistics - 'Monthly' Graph";
			lowerLimit = 0;
			upperLimit = 0;
			zoomType = 0;
			label_range = "Number of Orders received";
			label_domain = "Time (last month)";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.ORDER_GRAPH, DSMAConstants.INTERVAL_MONTH);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		}
		else if(chartType==DSMAConstants.ORDER_MINUTES_YEAR){
			title = "Orders Statistics - 'Yearly' Graph";
			lowerLimit = 0;
			upperLimit = 0;
			zoomType = 0;
			label_range = "Number of Orders received";
			label_domain = "Time (last year)";
			try {
				dataset = getLoadDatasetForInterval(DSMAConstants.ORDER_GRAPH, DSMAConstants.INTERVAL_YEAR);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		}

		else {
			System.err.println("Type unknown!!!!");
			return "";
		}

		chart = ChartFactory.createTimeSeriesChart(
				title,
				label_domain,
				label_range,
				(XYDataset)dataset, 
				true, 
				true, 
				false);
		
		if(zoomType == 0){	//no type
			XYPlot plot = chart.getXYPlot();
			final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			if(upperLimit < rangeAxis.getUpperBound())
				upperLimit = rangeAxis.getUpperBound();
			rangeAxis.setRange(new Range(lowerLimit,upperLimit));
			ValueAxis v = plot.getDomainAxis();
			v.setUpperMargin(0.02);
			v.setLowerMargin(0.02);
/*			CategoryAxis domainAxis = plot.getDomainAxis();
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions
					.createUpRotationLabelPositions(Math.PI / 6.0));
			
			//marginile din stanga si dreapta pentru grafic (distanta de la axa y la primul bar)
			domainAxis.setLowerMargin(0.02);
			domainAxis.setUpperMargin(0.02);
			*/
		}
		
		ChartRenderingInfo info = null;
		HttpSession session = request.getSession();
		double rnd = Math.random();
		try {

			// Create RenderingInfo object
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart.createBufferedImage(800, 400,800, 400, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			//session.setAttribute("lineChartImage"+chartType, null);
			session.setAttribute("cpuLine"+chartType + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "cpuMapLine"+chartType, info, false);
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
		String chartViewer = pathInfo + "/ChartViewer?chartName=cpuLine"+chartType+rnd;
		
		DBLoadInfo.getLogger().debug("getDSMALineGraph for type " + chartType + " took " + (System.currentTimeMillis()-startTime) + " miliseconds");
		
		return chartViewer;
	}

	

	public String getDSMADiskLineGraph(HttpServletRequest request, HttpServletResponse response, int chartType){
		Dataset dataset = null;
		String label_domain = "";
		String label_range = "";
		String title = "";
		JFreeChart chart = null;
		
		if(chartType==DSMAConstants.DISK_HOURS){
			title = "Disk Utilization (% of used space from each partition)";
			label_range = "% of used space";
			label_domain = "Time (hours graphic)";
			try {
				dataset = getDiskDataset(1, 2);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		} else if(chartType==DSMAConstants.DISK_MONTH){
			title = "Disk Utilization (% of used space from each partition)";
			label_range = "% of used space";
			label_domain = "Time (month graphic)";
			try {
				dataset = getDiskDataset(2, 31);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		} else if(chartType==DSMAConstants.DISK_YEAR){
			title = "Disk Utilization (% of used space from each partition)";
			label_range = "% of used space";
			label_domain = "Time (year graphic)";
			try {
				dataset = getDiskDataset(3, 365);
			} catch (Exception e) {
				e.printStackTrace();
				dataset = null;
			}
		} else {
			System.err.println("Type unknown!!!!");
			return "";
		}
		
		chart = ChartFactory.createTimeSeriesChart(
				title,
				label_domain,
				label_range,
				(XYDataset)dataset, 
				true, 
				true, 
				false);
		
		ChartRenderingInfo info = null;
		
		double lowerLimit = 0;
		double upperLimit = 100;
		
		XYPlot plot = chart.getXYPlot();
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		if(upperLimit < rangeAxis.getUpperBound())
			upperLimit = rangeAxis.getUpperBound();
		rangeAxis.setRange(new Range(lowerLimit,upperLimit));
		ValueAxis v = plot.getDomainAxis();
		v.setUpperMargin(0.02);
		v.setLowerMargin(0.02);
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setLabelFont(rangeAxis.getLabelFont().deriveFont(Font.BOLD));
		
		
		plot.getDomainAxis().setLabelFont(plot.getDomainAxis().getLabelFont().deriveFont(Font.BOLD));
		
		
		HttpSession session = request.getSession();
		double rnd = Math.random();
		try {

			// Create RenderingInfo object
			response.setContentType("text/html");
			info = new ChartRenderingInfo(new StandardEntityCollection());
			BufferedImage chartImage = chart.createBufferedImage(800, 400, 800, 400, info);

			// putting chart as BufferedImage in session,
			// thus making it available for the image reading action Action.
			//session.setAttribute("lineChartImage"+chartType, null);
			session.setAttribute("cpuLine"+chartType + rnd, chartImage);

			PrintWriter writer = new PrintWriter(response.getWriter());
			ChartUtilities.writeImageMap(writer, "cpuMapLine"+chartType, info, false);
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
		String chartViewer = pathInfo + "/ChartViewer?chartName=cpuLine"+chartType+rnd;
		
		//generateReport();
		
		return chartViewer;
	}
	private TimeSeriesCollection getDiskDataset(int type, int days ) {
		String sql = "select " + 
			
			DBConstants.FIELD_USAGE_DISK_DISK1 + " " + DBConstants.TIME_GRAPHIC_VALUE + " , " + 
			DBConstants.FIELD_USAGE_DISK_TIMESTAMP + " "  +DBConstants.TIME_GRAPHIC_TIME + " , " +
			"concat(" + DBConstants.FIELD_USAGE_DISK_SERVER_NAME +",':['," +  DBConstants.FIELD_USAGE_DISK_NAME + ",']')"  + " " + DBConstants.TIME_GRAPHIC_SERVER + ", " +
			DBConstants.FIELD_USAGE_DISK_MAX_VALUE + " " + DBConstants.TIME_GRAPHIC_EXTRA_FIELD + " " +
			
			" FROM " + DBConstants.TABLE_USAGE_DISK + 
			" WHERE " + DBConstants.FIELD_USAGE_DISK_TYPE + " = ? " +
			//" order by timestamp desc limit 200"	//just temporary for local testing
			" AND date_add(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + ", Interval ? day) > (now() )"
			;
		//for now just first type (normal) and just one day
		List<TimeGraphicMapper> databaseList = DBManager.getSimpleTemplate().query(sql, new TimeGraphicMapper(), type , days);
		
		
		HashMap<String, TimeSeries> foundSeries = new HashMap<String, TimeSeries>();
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		DecimalFormat decimalFormat = new DecimalFormat("##.0");
		for (TimeGraphicMapper timeGraphicMapper : databaseList) {
			String currentFolder = timeGraphicMapper.getServer();
			TimeSeries workingSeries = foundSeries.get(currentFolder);
			if(workingSeries == null) {
				String maxValue = "N/A";
				try {
					maxValue = decimalFormat.format(Float.parseFloat(timeGraphicMapper.getExtraField())/1024) + "GB";
				} catch (Exception e) {
					logger.error("Error parsing maxValue");
				}
				workingSeries = new TimeSeries(currentFolder + "(" + maxValue + ")");
				dataset.addSeries(workingSeries);
				foundSeries.put(currentFolder, workingSeries);
			}
			workingSeries.addOrUpdate(new Second(timeGraphicMapper.getTime()), 
					timeGraphicMapper.getValue());
		}
		
		return dataset;
	}
	
	/**
	 * This returns the TimeSeriesCollection used to draw the load graphic for the given days 
	 * @param days
	 * @return
	 */
	private TimeSeriesCollection getLoadDatasetForInterval(int type, int hours) {
		
		long startTime = System.currentTimeMillis();
		
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		Map<String, Float> servers = getCorrectionFactor(type);
		TreeSet<String> serverNames = new TreeSet<String>(servers.keySet());
		
		for (String server : serverNames) {
			TimeSeries series = new TimeSeries( server );
			dataset.addSeries(series);
		}
		List<TimeGraphicMapper> loadInfo = null;
		if(hours < 24)
			loadInfo = DBLoadInfo.getLoadInfoHour(type, hours);
		else
			loadInfo = DBLoadInfo.getLoadInfo(type, hours/24);
		
		for (TimeGraphicMapper timeGraphicMapper : loadInfo) {
			String serverName = timeGraphicMapper.getServer();
			TimeSeries series = dataset.getSeries(serverName);
			if( series == null ) {
				series = new TimeSeries( serverName );
				dataset.addSeries(series);
			}
			try {
				series.addOrUpdate(new Second(timeGraphicMapper.getTime()), 
					timeGraphicMapper.getValue() * servers.get(serverName));
			} catch (NullPointerException npe) {
				if(servers.get(serverName) == null) {
					servers.put(serverName, 1f);
					series.addOrUpdate(new Second(timeGraphicMapper.getTime()), 
						timeGraphicMapper.getValue() * servers.get(serverName));
				} else
					npe.printStackTrace();
			} catch (RuntimeException re) {
				re.printStackTrace();
			}
		}
		if(logger.isDebugEnabled()) {
			logger.debug("getLoadDatasetForInterval for type " + type + " and hours " + hours + " took " + ((System.currentTimeMillis()-startTime)) + " miliseconds");
		}
		return dataset;
	}
	
	private Map<String, Float> getCorrectionFactor(int type) {
		Map<String, Float> cachedValue = correctionFactorsCache.get(type);
		if(cachedValue == null) {
			cachedValue = DBLoadInfo.getCorrectionFactors(type);
			correctionFactorsCache.put(type, cachedValue);
		}
		return cachedValue;
	}

}