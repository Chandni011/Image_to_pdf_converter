package com.deificdigital.imagetopdfconverter;

import android.app.Application;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static String formatTimestamp(long timestamp) {
        // Create a SimpleDateFormat instance with the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        // Create a Date object from the timestamp
        Date date = new Date(timestamp);
        // Return the formatted date as a string
        return sdf.format(date);
    }
}
