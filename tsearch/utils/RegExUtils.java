package ro.cst.tsearch.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.ResultMap;

public class RegExUtils {
	public static String getPropertyValueForPattern(String pattern, String mapValue, String textToParse) {
		Pattern subd = Pattern.compile(pattern);
		String returnValue = null;
		if (StringUtils.isEmpty(mapValue)) {
			Matcher matcher = subd.matcher(textToParse);
			while (matcher.find()) {
				if (!StringUtils.isEmpty(returnValue)) {
					returnValue += " ";
				} else {
					returnValue = "";
				}
				returnValue = returnValue + matcher.group();
			}
		} else {
			returnValue = mapValue;
		}
		return returnValue;
	}

	/**
	 * @param tmp
	 * @param regex
	 * @return
	 */
	public static String parseValuesForRegEx(String tmp, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(tmp);
		String result = null;
		while (matcher.find()) {
			if (result == null)
				result = "";
			result = result + " " + matcher.group();
		}
		return result;
	}

	/**
	 * tmp - the string to be parsed by the regex. The result is returned into
	 * an linkedList with each found group in a position in the list.
	 * 
	 * If deleteWhatWasFound is true then all the found regex values will be
	 * deleted from the provided string and the remainder will be put as te last
	 * element of the list.
	 * 
	 * @param tmp
	 * @param regex
	 * @param deleteWhatWasFound
	 * @return
	 */
	public static List<String> parseValuesForRegEx(String tmp, String regex, boolean deleteWhatWasFound) {
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(tmp);
		List<String> result = new LinkedList<String>();
		while (matcher.find()) {
			result.add(matcher.group());
		}
		if (deleteWhatWasFound) {
			tmp = matcher.replaceAll("");
			result.add(tmp);
		}
		return result;
	}

	/**
	 * Parses a inputString by the provided regex. Retrieves an array with the
	 * length equal with groups. In groups are kept the group indexes that are
	 * to be kept after the parse in the resulting array
	 * 
	 * @param inputString
	 * @param regex
	 * @param groups
	 * @return
	 */
	public static String[] parseByRegEx(String inputString, String regex, int[] groups) {
		Pattern compile = Pattern.compile(regex);
		Matcher matcher = compile.matcher(inputString);
		String[] string = new String[groups.length];

		if (matcher.find()) {
			for (int i = 0; i < groups.length; i++) {
				string[i] = matcher.group(groups[i]);
			}
		}
		return string;
	}

	public static List<String> getMatches(String regEx, String text, int group) {
		Pattern pattern = Pattern.compile(regEx);
		List<String> matches = new ArrayList<String>();
		Matcher m = pattern.matcher(text);
		while (m.find()) {
			matches.add(m.group(group));
		}
		return matches;
	}

	public static List<String> getMatches(String regEx, String text) {
		Pattern pattern = Pattern.compile(regEx);
		List<String> matches = new ArrayList<String>();
		if (StringUtils.isNotEmpty(text)) {
			Matcher m = pattern.matcher(text);
			if (m.find()) {
				for (int i = 1; i <= m.groupCount(); i++) {
					matches.add(m.group(i));
				}
			}
		}
		return matches;
	}

	/**
	 * It retrieves the values for the groups specified in @param group as
	 * String separted by a space. All the encountered values for the specified
	 * regex pattern are added to a list as specified.
	 * 
	 * @param regEx
	 * @param text
	 * @param group
	 * @return
	 */
	public static List<String> getMatches(String regEx, String text, int... group) {
		Pattern pattern = Pattern.compile(regEx);
		List<String> matches = new ArrayList<String>();
		Matcher m = pattern.matcher(text);
		while (m.find()) {
			StringBuilder parsedValue = new StringBuilder("");
			for (int i : group) {
				if (m.groupCount() >= i) {
					parsedValue.append(m.group(i) + " ");
				}
			}
			matches.add(parsedValue.toString().trim());
		}
		return matches;
	}

	/**
	 * returns every match as a list o maps which have as the key the group
	 * number and as value what the expressions group number value.
	 * 
	 * @param regEx
	 * @param text
	 * @param group
	 * @return
	 */
	public static List<HashMap<String, String>> getMatchesAsMap(String regEx, String text, int... group) {
		Pattern pattern = Pattern.compile(regEx);
		List<HashMap<String, String>> matches = new ArrayList<HashMap<String, String>>();
		Matcher m = pattern.matcher(text);
		while (m.find()) {
			HashMap parsedValue = new HashMap<String, String>();
			for (int i : group) {
				if (m.groupCount() >= i) {
					parsedValue.put("" + i, org.apache.commons.lang.StringUtils.defaultIfEmpty(m.group(i), ""));
				}
			}
			matches.add(parsedValue);
		}
		return matches;
	}
	
	/**
	 * Used for regex that provide a groupcount of 2: e.g. <input type='hidden' name='(.*?)'  value='(.*?)'/> which will provide a map 
	 * with the key provided by the group represented by keyIndex and value with the group represented by valueIndex.
	 *  
	 * @param regEx
	 * @param text
	 * @return
	 */
	public static Map<String, List<String>> getMatchesAsMap(String regEx, String text, byte keyIndex, byte valueIndex) {
		Pattern pattern = Pattern.compile(regEx);
		Matcher m = pattern.matcher(text);
		Map<String, List<String>> parsedValue = new HashMap<String, List<String>>();

		while (m.find()) {
			if (m.groupCount() >= keyIndex && m.groupCount() >= valueIndex) {
				String key = m.group(1);
				String value = org.apache.commons.lang.StringUtils.defaultIfEmpty(m.group(valueIndex), "");
				if (parsedValue.get(key) == null) {
					LinkedList<String> l = new LinkedList<String>();
					l.add(value);
					parsedValue.put(key, l);
				} else {
					List<String> list = parsedValue.get(key);
					list.add(value);
				}
			}
		}

		return parsedValue;
	}

	/**
	 * The regEx parameter contains the regular expresion that should extract
	 * from the provided text the group indicated in the regEX. For ex: text =
	 * "aa aaa aaaa aa " and regex "\baa\b" should return "aa", if the provided
	 * group is set with 0. The
	 * 
	 * @param regEx
	 * @param text
	 * @param group 
	 * @return
	 * 		first matching group or ""(empty string) 
	 * @throws
	 * Throws: IndexOutOfBoundsException - If there is no capturing
	 *         group in the pattern with the given index
	 */
	public static String getFirstMatch(String regEx, String text, int group) {
		Pattern pattern = Pattern.compile(regEx);
		Matcher m = pattern.matcher(text);
		String match = "";
		if (m.find() && m.groupCount() >= group) {
			match = m.group(group);
		}
		return match;
	}

	/**
	 * Gets the first match encountered by the defined @param regEx into the @param
	 * text. If there are multiple groups you want to extract add the, to the
	 * group parameter. The list retrieves the parsed groups defined in the
	 * parameter group.
	 * 
	 * @param regEx
	 * @param text
	 * @param group
	 * @return
	 */
	public static List<String> getFirstMatch(String regEx, String text, int... group) {
		Pattern pattern = Pattern.compile(regEx);
		Matcher m = pattern.matcher(text);
		List<String> match = new LinkedList<String>();
		if (m.find()) {
			for (int i : group) {
				match.add(m.group(i));
			}
		}
		return match;
	}

	/**
	 * Does what "".matches and Pattern.matches() suppossed to do.
	 * 
	 * @param regEx
	 * @param text
	 * @return
	 */
	public static boolean matches(String regEx, String text) {
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(text);
		boolean returnValue = false;
		if (matcher.find()) {
			returnValue = true;
		}
		return returnValue;
	}

	public static boolean extractInfoIntoMap(ResultMap m, String extractFrom, String regex, String... keys) {

		try {
			Pattern p = Pattern.compile(regex);
			Matcher ma = p.matcher(extractFrom);
			if (ma.find()) {
				for (int i = 0; i < keys.length; i++) {
					try {
						m.put(keys[i], ma.group(i + 1).trim());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
