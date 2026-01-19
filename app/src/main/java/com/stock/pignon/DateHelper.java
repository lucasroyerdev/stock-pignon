// DateHelper.java
package com.stock.pignon;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {
    /**
     * Return ISO format date (AAAA-MM-JJ)
     */
    public static String getTodayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}