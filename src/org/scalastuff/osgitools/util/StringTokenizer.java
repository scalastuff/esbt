package org.scalastuff.osgitools.util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class StringTokenizer {

	public enum TokenizeOptions { DETECT_DBLQUOTES, DETECT_QUOTES, DETECT_BRACES, TRIM_TOKENS, DETECT_BRACKETS }
	
	private final String s;
	private final char sep;
	private final EnumSet<TokenizeOptions> options;
	private final StringBuilder out = new StringBuilder();
	private int i = 0;
	
	private StringTokenizer(String s, char sep, EnumSet<TokenizeOptions> options) {
		this.s = s;
		this.sep = sep;
		this.options = options;
	}
	
	public static List<String> tokenize(String s, char sep) {
		return tokenize(s, sep, EnumSet.allOf(TokenizeOptions.class));
	}
	
	public static List<String> tokenize(String s, char sep, EnumSet<TokenizeOptions> options) {
		return new StringTokenizer(s, sep, options).tokenize();
	} 
	
	private List<String> tokenize() {
		List<String> tokens = new ArrayList<String>();
		for (i = 0; i <= s.length(); i++) {
			if (i == s.length() || s.charAt(i) == sep) {
				if (options.contains(TokenizeOptions.TRIM_TOKENS)) {
					tokens.add(out.toString().trim());
				} else {
					tokens.add(out.toString());
				}
				out.setLength(0);
			} else {
				read();
			}
		}
		return tokens;
	}

	private void read() {
		char c = s.charAt(i);
		if (c == '\"' && options.contains(TokenizeOptions.DETECT_DBLQUOTES)) skipQuotes('\"');
		else if (c == '\'' && options.contains(TokenizeOptions.DETECT_QUOTES)) skipQuotes('\"');
		else if (c == '(' && options.contains(TokenizeOptions.DETECT_BRACES)) skipBraces('(', ')');
		else if (c == '{' && options.contains(TokenizeOptions.DETECT_BRACKETS)) skipBraces('{', '}');
		else out.append(c);
	}

	private void skipQuotes(char delim) {
		out.append(delim);
		for (i++; i < s.length(); i++) {
			char c = s.charAt(i);
			out.append(c);
			if (c == delim) break;
		}
	}
	
	private void skipBraces(char left, char right) {
		out.append(left);
		for (i++; i < s.length(); i++) {
			char c = s.charAt(i);
			out.append(c);
			if (c == right) {
				out.append(c);
				return;
			}
			read();
		}
	}
}
