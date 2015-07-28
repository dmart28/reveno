package org.reveno.atp.utils;

public abstract class MeasureUtils {

	public static int kb(int number) {
		return 1024 * number;
	}
	
	public static int mb(int number) {
		return 1024 * kb(number);
	}
	
	public static long gb(int number) {
		return 1024L * mb(number);
	}
	
}
