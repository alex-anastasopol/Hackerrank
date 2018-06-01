package ro.cst.tsearch.utils;

import java.util.HashMap;

import com.stewart.ats.base.name.Name;




public class FirstNameEquivalents {
	
	
	private static final HashMap<String,Name> firstNameEquivalent;
	
	static {
		firstNameEquivalent = new HashMap<String,Name>();
		
		firstNameEquivalent.put("ARTHURR", new Name("ARTHUR", "R", ""));
		firstNameEquivalent.put("DAVIDL", new Name("DAVID", "L", ""));
		firstNameEquivalent.put("DANIELJ", new Name("DANIEL", "J", ""));
		firstNameEquivalent.put("ELIZABETHL", new Name("ELIZABETH", "L", ""));
		firstNameEquivalent.put("ERICS", new Name("ERIC", "S", ""));
		firstNameEquivalent.put("ERIKAH", new Name("ERIKA", "H", ""));
		firstNameEquivalent.put("GEORGEM", new Name("GEORGE", "M", ""));
		firstNameEquivalent.put("MARYA", new Name("MARY", "A", ""));
		firstNameEquivalent.put("MARYE", new Name("MARY", "E", ""));
		firstNameEquivalent.put("MARYG", new Name("MARY", "G", ""));
		firstNameEquivalent.put("MICHAELK", new Name("MICHAEL", "K", ""));
		firstNameEquivalent.put("MICHAELZ", new Name("MICHAEL", "Z", ""));
		firstNameEquivalent.put("LILIANEM", new Name("LILIANE", "M", ""));
		firstNameEquivalent.put("SCOTTH", new Name("SCOTT", "H", ""));
		firstNameEquivalent.put("SHIRLEYA", new Name("SHIRLEY", "A", ""));
		firstNameEquivalent.put("ROBERTE", new Name("ROBERT", "E", ""));
		firstNameEquivalent.put("ROBERTF", new Name("ROBERT", "F", ""));
		firstNameEquivalent.put("ROBERTJ", new Name("ROBERT", "J", ""));
		firstNameEquivalent.put("THOMASB", new Name("THOMAS", "B", ""));
		firstNameEquivalent.put("THOMASQ", new Name("THOMAS", "Q", ""));
		firstNameEquivalent.put("TOMMYL", new Name("TOMMY", "L", ""));
		firstNameEquivalent.put("WILLIAMG", new Name("WILLIAM", "G", ""));
		firstNameEquivalent.put("WILLIAMH", new Name("WILLIAM", "H", ""));
		firstNameEquivalent.put("WILLIAML", new Name("WILLIAM", "L", ""));
		firstNameEquivalent.put("WILLIAMR", new Name("WILLIAM", "R", ""));
				
		
	}
	
	public static final boolean needEquivalent(String check) {
		if (check == null)
			return false;
		
		return firstNameEquivalent.containsKey(check.toUpperCase());
	}
	
	public static final Name getEquivalent(String name) {
		if (name == null)
			return new Name("", "" , "");
		
		return firstNameEquivalent.get(name.toUpperCase());
	}

}
