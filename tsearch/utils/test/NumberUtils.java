package ro.cst.tsearch.utils.test;

import junit.framework.TestCase;

public class NumberUtils extends TestCase{
	
	public void convertNumberToEnglishWordsListTest(){
		String numberEnumeration= "20 20A 20B 20-35";
		String convertNumberToEnglishWordsList = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(numberEnumeration, "\\s");
		System.out.println(convertNumberToEnglishWordsList);
		assertEquals("twenty,twenty A,twenty B,twenty to thirty five", convertNumberToEnglishWordsList);
		
		
		System.out.println("---------------");
		
		numberEnumeration= "20.21";
		convertNumberToEnglishWordsList = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(numberEnumeration, "\\s");
		System.out.println(convertNumberToEnglishWordsList);
		assertEquals("twenty,twenty one", convertNumberToEnglishWordsList);
		
		
//		"18 , 20".split(",|\\s*|\\s*,\\s*")
//		numberEnumeration.split(",|\\s*")
//		numberEnumeration.split("\\s*?,?\\s*?")
//		numberEnumeration.split("\\s+?,?\\s+?")
//		numberEnumeration.split("\\s+?,?\\.?\\s*?")
		
		numberEnumeration= "18 , 20";
		convertNumberToEnglishWordsList = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(numberEnumeration, "\\s");
		System.out.println(convertNumberToEnglishWordsList);
		assertEquals("eighteen,twenty", convertNumberToEnglishWordsList);
		
		numberEnumeration= "18 ,20";
		convertNumberToEnglishWordsList = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(numberEnumeration, "\\s");
		System.out.println(convertNumberToEnglishWordsList);
		assertEquals("eighteen,twenty", convertNumberToEnglishWordsList);
		
		numberEnumeration= "18 ,20";
		convertNumberToEnglishWordsList = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(numberEnumeration, "\\s");
		System.out.println(convertNumberToEnglishWordsList);
		assertEquals("eighteen,twenty", convertNumberToEnglishWordsList);
		
		
	}
	
}
