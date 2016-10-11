package net.kaikk.mc.synx;

import java.util.Collection;
import java.util.regex.Pattern;

public class Utils {
	private static Pattern alphanumeric = Pattern.compile("[a-zA-Z0-9]");
	public static boolean isAlphanumeric(String string) {
		return alphanumeric.matcher(string).find();
	}

	/**
	 * Compare collections elements
	 * @param c1 a collection
	 * @param c2 another collection
	 * @return true if c1 and c2 have the same size and c2 contains all elements of c1
	 */
	public static boolean compareCollections(Collection<?> c1, Collection<?> c2) {
		if (c1.size()!=c2.size()) {
			return false;
		}
		
		for (Object o : c1) {
			if (!c2.contains(o)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static String mergeStringArrayFromIndex(String[] arrayString, int i) {
		StringBuilder sb = new StringBuilder();
		
		for(;i<arrayString.length;i++){
			sb.append(arrayString[i]);
			sb.append(' ');
		}
		
		if (sb.length()!=0) {
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}
}
