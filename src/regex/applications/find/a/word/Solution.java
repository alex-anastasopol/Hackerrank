package regex.applications.find.a.word;

import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

public class Solution {
	private static int getNoOfOccurrences(String word, String[] sentences) {
		int noOfOccurrences = 0;
		// TODO: implement
		return noOfOccurrences;
	}

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);

		try {
			int sentencesCount = sc.nextInt();
			String[] sentences = new String[sentencesCount];
			for (int i = 0; i < sentencesCount; i++) {
				sentences[i] = sc.nextLine();
			}

			int wordCount = sc.nextInt();
			String[] words = new String[wordCount];
			for (int i = 0; i < sentencesCount; i++) {
				System.out.println(getNoOfOccurrences(words[i], sentences));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sc.close();
		}
	}
}