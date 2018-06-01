package ro.cst.tsearch;

public class LoadConfig extends ServerConfig {
	
	public static boolean getLoadAverageComputationEnable() {
		return ServerConfig.getBoolean("load.average.computation.enable", false);
	}
	
	public static boolean getLoadInformationEnable() {
		return ServerConfig.getBoolean("load.information.enable", false);
	}
	
	/**
	 * Default value is 300 which means 5 minutes
	 * @return value for key "load.average.computation.period"
	 */
	public static int getLoadAverageComputationPeriod() {
		return ServerConfig.getInteger("load.average.computation.period", 300);
	}
	
	public static int getNetworkBandwidthDefault() {
		return ServerConfig.getInteger("network.bandwidth.default");
	}
	
	public static boolean getLbsEnableLoadAlg() {
		return ServerConfig.getBoolean("lbs.enable.load.alg", true);
	}
	public static boolean getLbsEnableSourceAlg() {
		return ServerConfig.getBoolean("lbs.enable.source.alg", true);
	}

	public static String getLoadBalancingUrl() {
		String lbURL = ServerConfig.getString("lb.url", "ats.advantagetitlesearch.com");
		if (lbURL.endsWith("/")) {
			lbURL = lbURL.substring(0, lbURL.length() - 1);
		}
		return lbURL;
	}

	public static int getLoadBalancingPort() {
		return ServerConfig.getInteger("lb.port", 80);
	}
	
}
