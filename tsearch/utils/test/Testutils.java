package ro.cst.tsearch.utils.test;

public class Testutils {
	
	public  static void generateTestCase(String filename, String county, String state, String comment) {
		if (System.getProperty("debug") != null) {
			if (!comment.equals("")) {
				String completeFilename = filename + state + "_" + county;
				String lineContent = state + "," + county + "," + comment.replace(",", ";") + ",\n";
				ro.cst.tsearch.utils.test.StringUtils.write(completeFilename, lineContent);
			}
		}
	}
}
