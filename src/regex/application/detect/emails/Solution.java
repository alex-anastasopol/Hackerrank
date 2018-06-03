package regex.application.detect.emails;

import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

public class Solution {
	private static int getNoOfOccurrences(String word, String[] sentences) {
		int noOfOccurrences = 0;
		Pattern pat = Pattern.compile("\\b" + word + "\\b");
		for (int i = 0; i < sentences.length; i++) {
			Matcher mat = pat.matcher(sentences[i]);
			while (mat.find()) {
				noOfOccurrences++;
			}
		}

		return noOfOccurrences;
	}

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		try {
			int sentencesCount = Integer.valueOf(sc.nextLine());
			String[] sentences = new String[sentencesCount];
			for (int i = 0; i < sentencesCount; i++) {
				sentences[i] = sc.nextLine();
			}

			int wordCount = Integer.valueOf(sc.nextLine());
			String[] words = new String[wordCount];
			for (int i = 0; i < wordCount; i++) {
				words[i] = sc.nextLine();
			}
			
			for (int i = 0; i < wordCount; i++) {
				System.out.println(getNoOfOccurrences(words[i], sentences));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sc.close();
		}
	}
}