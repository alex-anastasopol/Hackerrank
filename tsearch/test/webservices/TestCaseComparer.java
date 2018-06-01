package ro.cst.tsearch.test.webservices;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import ro.cst.tsearch.utils.FileUtils;

public class TestCaseComparer {

	private static final String CSS =
		
	"<style type=\"text/css\">" + 
	"body { FONT-SIZE: 12px; FONT-FAMILY: Arial, Helvetica, sans-serif; }" +	
	"tr { FONT-SIZE: 12px; FONT-FAMILY: Arial, Helvetica, sans-serif; TEXT-ALIGN: left; BACKGROUND-COLOR: #F0F0F0;}" +
	"</style>";
		
	public static List<String[]> readTestCase(String fileName){

		try {
			
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			try {
			
				List<String[]> tests = new LinkedList<String[]>();
				
				List<String> crtTest = new LinkedList<String>();
				String line = "";
				int cnt = 0;
				while( (line = br.readLine()) != null){
					if("".equals(line)){
						cnt++;
						if(cnt == 2){
							tests.add(crtTest.toArray(new String[crtTest.size()]));
							crtTest = new LinkedList<String>();
						}
					} else {
						if(cnt == 1){
							crtTest.add("");
						}
						crtTest.add(line);
						cnt = 0;
					}
				}		
				
				if(crtTest.size() != 0){
					tests.add(crtTest.toArray(new String[crtTest.size()]));
				}
				
				return tests;
				
			} finally {
				br.close();
			}
			
		} catch (Exception e){
			throw new RuntimeException(e);
		}
				
	}
	
	public static String displayTestCase(List<String[]> tests){
		
		StringBuilder sb = new StringBuilder();
		sb.append(CSS);
		sb.append("<pre>");		
		int i = 1;
		for(String[] test: tests){
			sb.append(i); i++;
			sb.append("<table border=1 cellspacing=0 cellpadding=0 width=100% bgcolor=#EEEEEE>");
			for(String line: test){
				sb.append("<tr>");
				sb.append("<td width=100%>");sb.append(line);sb.append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
		}
		sb.append("</pre>");
		return sb.toString();
	}
	
	public static String displayTestCaseDifference(List<String[]> tests1, List<String[]>tests2){
		
		StringBuilder sb = new StringBuilder();
		int len1 = tests1.size();
		int len2 = tests2.size();

		int maxLen = len1 > len2 ? len1 : len2;
		
		sb.append(CSS);
		
		for(int i=0; i<maxLen; i++){

			String[] test1 = (i < len1) ? tests1.get(i) : new String[0];
			String[] test2 = (i < len2) ? tests2.get(i) : new String[0];
			
			sb.append("" + (i+1));
			sb.append("<table border=1 cellspacing=0 cellpadding=0 width=100% bgcolor=#EEEEEE>");

			int l1 = test1.length;
			int l2 = test2.length;
			int maxl = l1 > l2 ? l1 : l2;
			int t1Idx = 0, t2Idx = 0; 
			while (t1Idx < l1 && t2Idx < l2){
				if(!test1[t1Idx].equals(test2[t2Idx])){
					boolean found = false;
					for (int k=t2Idx+1; k<l2; k++){
						if (test1[t1Idx].trim().equals(test2[k].trim())){
							for (int t=t2Idx; t<k; t++){
								sb.append("<td bgcolor=#EE7777 width=50%>");sb.append("&nbsp;");sb.append("</td>");sb.append("\n");
								sb.append("<td bgcolor=#EE7777 width=50%>");sb.append(test2[t]);sb.append("</td>");sb.append("\n");
								sb.append("</tr>");
							}
							sb.append("<td width=50%>");sb.append(test1[t1Idx]);sb.append("</td>");sb.append("\n");
							sb.append("<td width=50%>");sb.append(test2[k]);sb.append("</td>");sb.append("\n");
							t2Idx = k;
							found = true;
							break;
						} 
					}
					if (!found){
						for (int k=t1Idx+1; k<l1; k++){
							if (test1[k].equals(test2[t2Idx])){
								for (int t=t1Idx; t<k; t++){
									sb.append("<td bgcolor=#EE7777 width=50%>");sb.append(test1[t]);sb.append("</td>");sb.append("\n");
									sb.append("<td bgcolor=#EE7777 width=50%>");sb.append("&nbsp;");sb.append("</td>");sb.append("\n");		
									sb.append("</tr>");
								}
								sb.append("<td width=50%>");sb.append(test1[k]);sb.append("</td>");sb.append("\n");
								sb.append("<td width=50%>");sb.append(test2[t2Idx]);sb.append("</td>");sb.append("\n");
								t1Idx = k;
								found = true;
								break;
							} 
						}
					}
					if (!found){
						sb.append("<td bgcolor=#EE7777 width=50%>");sb.append(test1[t1Idx]);sb.append("</td>");sb.append("\n");
						sb.append("<td bgcolor=#EE7777 width=50%>");sb.append(test2[t2Idx]);sb.append("</td>");sb.append("\n");	
					}
				} else {
					sb.append("<td width=50%>");sb.append(test1[t1Idx]);sb.append("</td>");sb.append("\n");
					sb.append("<td width=50%>");sb.append(test2[t2Idx]);sb.append("</td>");sb.append("\n");					
				}
				t1Idx ++;
				t2Idx ++;
				sb.append("</tr>");
			}
			while (t1Idx < l1){
				sb.append("<td bgcolor=#EE7777 width=50%>");sb.append(test1[t1Idx]);sb.append("</td>");sb.append("\n");
				sb.append("<td bgcolor=#EE7777 width=50%>");sb.append("&nbsp;");sb.append("</td>");sb.append("\n");						
				t1Idx ++;
				sb.append("</tr>");
			}
			while (t2Idx < l2){
				sb.append("<td bgcolor=#EE7777 width=50%>");sb.append("&nbsp;");sb.append("</td>");sb.append("\n");
				sb.append("<td bgcolor=#EE7777 width=50%>");sb.append(test2[t2Idx]);sb.append("</td>");sb.append("\n");					
				t2Idx ++;
				sb.append("</tr>");
			}				
			sb.append("</table>");
		}		
		
		return sb.toString();
	}
	
	
	
	public static void main(String[] args) {

		List<String[]> testCaseRef = readTestCase("d:/CAAlamedaDT_legal_parsed_REF.txt");
		List<String[]> testCaseCand = readTestCase("d:/CAAlamedaDT_legal_parsed_RES.txt");
		
		FileUtils.writeTextFile("d:/CAAlamedaDT_legal_parsed_REF.html", displayTestCase(testCaseRef));
		FileUtils.writeTextFile("d:/CAAlamedaDT_legal_parsed_RES.html", displayTestCase(testCaseCand));
		FileUtils.writeTextFile("d:/CAAlamedaDT_legal_parsed_CMP.html", displayTestCaseDifference(testCaseRef, testCaseCand));
	}

}

