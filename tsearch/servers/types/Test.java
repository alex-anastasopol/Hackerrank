package ro.cst.tsearch.servers.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	
	public static void main(String[] args) {
		
		String reg = "(.*)(?:AC|RD|DR|BV|SD|EP|CR|CIR|DF|GRW)(.*)";
		Pattern p = Pattern.compile(reg);
		
		String test = "GEN GEORGE PATTON RD NASHVILLE";
		
		long time = System.currentTimeMillis();
		for (int i = 0; i < 10000000; i++) {
			
			/*Matcher m = p.matcher(test);
			if (m.find())
				m.group(1);*/
			
			test.replaceAll(reg, "$1");
		}
		System.out.println("Executed in " + (System.currentTimeMillis() - time) + " ms");
	}
}
