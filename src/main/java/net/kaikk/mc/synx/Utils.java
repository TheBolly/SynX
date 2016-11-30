package net.kaikk.mc.synx;

import java.util.Collection;

/**
 * Use SynXUtils instead. This may be removed in future releases.
 * */
@Deprecated
public class Utils {
	@Deprecated
	public static boolean isAlphanumeric(String string) {
		return SynXUtils.isAlphanumeric(string);
	}

	/**
	 * Compare collections elements
	 * @param c1 a collection
	 * @param c2 another collection
	 * @return true if c1 and c2 have the same size and c2 contains all elements of c1
	 */
	@Deprecated
	public static boolean compareCollections(Collection<?> c1, Collection<?> c2) {
		return SynXUtils.compareCollections(c1, c2);
	}
	
	@Deprecated
	public static String mergeStringArrayFromIndex(String[] arrayString, int i) {
		return SynXUtils.mergeStringArrayFromIndex(arrayString, i);
	}
}
