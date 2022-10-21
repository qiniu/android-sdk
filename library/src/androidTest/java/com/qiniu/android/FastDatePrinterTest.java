package com.qiniu.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.utils.FastDatePrinter;
import com.qiniu.android.utils.LogUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class FastDatePrinterTest extends BaseTest {

    @Test
    public void testCreate(){

        Date date = new Date(1595474306393l);
        TimeZone timeZone = TimeZone.getTimeZone("GMT"); 
        // year
        FastDatePrinter datePrinter = new FastDatePrinter(
                "yy-MM-dd HH:mm:ss",
                timeZone,
                Locale.getDefault());
        String dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("20-07-23 03:18:26")));

        datePrinter = new FastDatePrinter(
                "yyyy-MM-dd HH:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("2020-07-23 03:18:26")));

        datePrinter = new FastDatePrinter(
                "yyyyy-MM-dd HH:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("02020-07-23 03:18:26")));


        // month
        datePrinter = new FastDatePrinter(
                "yyyy-M-dd HH:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
//        assertTrue(dateString, (dateString.equals("2020-7-23 03:18:26")));

        datePrinter = new FastDatePrinter(
                "yyyy-MMM-dd HH:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
//        assertTrue(dateString, (dateString.equals("2020-Jul-23 03:18:26")));

        datePrinter = new FastDatePrinter(
                "yyyy-MMMM-dd HH:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
//        assertTrue(dateString, (dateString.equals("2020-July-23 03:18:26")));


        // E
        datePrinter = new FastDatePrinter(
                "E yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
//        assertTrue(dateString, (dateString.equals("Thu 2020-07-23 03:18:26")));


        // u
        datePrinter = new FastDatePrinter(
                "u yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("4 2020-07-23 03:18:26")));


        // D
        datePrinter = new FastDatePrinter(
                "D yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("205 2020-07-23 03:18:26")));


        // F
        datePrinter = new FastDatePrinter(
                "F yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("4 2020-07-23 03:18:26")));


        // W
        datePrinter = new FastDatePrinter(
                "W yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("4 2020-07-23 03:18:26")));


        // w
        datePrinter = new FastDatePrinter(
                "w yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("30 2020-07-23 03:18:26")));



        // K
        datePrinter = new FastDatePrinter(
                "K yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("3 2020-07-23 03:18:26")));


        // k
        datePrinter = new FastDatePrinter(
                "k yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("3 2020-07-23 03:18:26")));


        // X
        datePrinter = new FastDatePrinter(
                "X yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("Z 2020-07-23 03:18:26")));


        // Z
        datePrinter = new FastDatePrinter(
                "Z yyyy-MM-dd hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("+0000 2020-07-23 03:18:26")));


        // //
        datePrinter = new FastDatePrinter(
                "yyyy-MM-dd // hh:mm:ss",
                timeZone,
                Locale.getDefault());
        dateString = datePrinter.format(date);
        LogUtil.i("== date:" + dateString);
        assertTrue(dateString, (dateString.equals("2020-07-23 // 03:18:26")));


        // N
        try {
            datePrinter = new FastDatePrinter(
                    "N yyyy-MM-dd hh:mm:ss",
                    timeZone,
                    Locale.getDefault());
        } catch (IllegalArgumentException e){
            assertTrue(true);
        }

    }

}
