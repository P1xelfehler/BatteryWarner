package com.laudien.p1xelfehler.batterywarner.database;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.TemperatureConverter;

import java.text.DateFormat;
import java.util.Locale;

/**
 * This Class saves information about the charging curve. It can show an info dialog to the user.
 */
public class GraphInfo {
    private boolean useFahrenheit, reverseCurrent;
    private int maxBatteryLvl, firstBatteryLvl;
    private double timeInMinutes, maxTemp, minTemp, minCurrent, maxCurrent, minVoltage, maxVoltage, chargingSpeed;
    private long startTime, endTime;
    private Dialog dialog;

    /**
     * Constructor of the GraphInfo. All values must be provided.
     *
     * @param endTime       The time the graph was created.
     * @param timeInMinutes The time of charging in minutes.
     * @param maxTemp       The maximal battery temperature while charging.
     * @param minTemp       The minimal battery temperature while charging.
     * @param chargingSpeed The charging speed in percent per hour.
     */
    GraphInfo(long startTime, long endTime, double timeInMinutes, double maxTemp, double minTemp,
              double chargingSpeed, double minCurrent, double maxCurrent, double minVoltage,
              double maxVoltage, int maxBatteryLvl, int firstBatteryLvl, boolean useFahrenheit, boolean reverseCurrent) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeInMinutes = timeInMinutes;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.chargingSpeed = chargingSpeed;
        this.minCurrent = minCurrent;
        this.maxCurrent = maxCurrent;
        this.minVoltage = minVoltage;
        this.maxVoltage = maxVoltage;
        this.maxBatteryLvl = maxBatteryLvl;
        this.firstBatteryLvl = firstBatteryLvl;
        this.useFahrenheit = useFahrenheit;
        this.reverseCurrent = reverseCurrent;
    }

    public GraphInfo(@NonNull DatabaseValue value, boolean useFahrenheit, boolean reverseCurrent) {
        double temperature = useFahrenheit ? value.getTemperatureInFahrenheit() : value.getTemperatureInCelsius();
        double current = value.getCurrentInMilliAmperes(reverseCurrent);
        double voltage = value.getVoltageInVolts();
        int batteryLevel = value.getBatteryLevel();

        this.startTime = value.getGraphCreationTime();
        this.endTime = value.getUtcTimeInMillis();
        this.timeInMinutes = value.getTimeFromStartInMinutes();
        this.maxTemp = temperature;
        this.minTemp = temperature;
        this.chargingSpeed = Double.NaN;
        this.minCurrent = current;
        this.maxCurrent = current;
        this.minVoltage = voltage;
        this.maxVoltage = voltage;
        this.maxBatteryLvl = batteryLevel;
        this.firstBatteryLvl = batteryLevel;
        this.useFahrenheit = useFahrenheit;
        this.reverseCurrent = reverseCurrent;
    }

    private static String[] getTimeFormats(Context context) {
        String[] formats = new String[4];
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String timeFormat = sharedPreferences.getString(context.getString(R.string.pref_time_format), context.getString(R.string.pref_time_format_default));
        switch (timeFormat) {
            case "0":
                formats[0] = "%d h %.0f min";
                formats[1] = "%.0f min";
                formats[2] = "%.0f min";
                formats[3] = "false";
                break;
            case "1":
                formats[0] = "%d h %.1f min";
                formats[1] = "%.1f min";
                formats[2] = "%.1f min";
                formats[3] = "false";
                break;
            case "2":
                formats[0] = "%d h %.0f min %.0f s";
                formats[1] = "%.0f min %.0f s";
                formats[2] = "%.0f s";
                formats[3] = "true";
                break;
        }
        return formats;
    }

    /**
     * Returns the time string for zero seconds. Uses the time format set by the user in the settings.
     *
     * @param context An instance of the Context class.
     * @return Returns the time string for zero seconds.
     */
    public static String getZeroTimeString(Context context) {
        String[] formats = getTimeFormats(context);
        return String.format(Locale.getDefault(), formats[2], 0f);
    }

    public void notifyValueAdded(@NonNull DatabaseValue value, @NonNull Context context) {
        double temp = useFahrenheit ? value.getTemperatureInFahrenheit() : value.getTemperatureInCelsius();
        if (temp > maxTemp) {
            maxTemp = temp;
        }
        if (temp < minTemp) {
            minTemp = temp;
        }
        if (value.getBatteryLevel() > maxBatteryLvl) {
            maxBatteryLvl = value.getBatteryLevel();
            long timeToMaxBatteryLvl = value.getUtcTimeInMillis() - value.getGraphCreationTime();
            double timeInHours = (double) timeToMaxBatteryLvl / (double) (1000 * 60 * 60);
            int percentageDiff = maxBatteryLvl - firstBatteryLvl;
            chargingSpeed = percentageDiff / timeInHours;
        }
        double current = value.getCurrentInMilliAmperes(reverseCurrent);
        if (current > maxCurrent) {
            maxCurrent = current;
        }
        if (current < minCurrent) {
            minCurrent = current;
        }
        double voltage = value.getCurrentInMilliAmperes(reverseCurrent);
        if (voltage > maxVoltage) {
            maxVoltage = voltage;
        }
        if (voltage < minVoltage) {
            minVoltage = voltage;
        }
        timeInMinutes = value.getTimeFromStartInMinutes();
        endTime = value.getUtcTimeInMillis();
        updateDialog(context);
    }

    /**
     * Shows the info dialog with all the information of the charging curve to the user.
     *
     * @param context An instance of the Context class.
     */
    public void showDialog(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_graph_info);
        // close button
        Button btn_close = dialog.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                dialog = null; // make the dialog object ready for garbage collection
            }
        });
        updateDialog(context);
        // show dialog
        dialog.show();
    }

    private void updateDialog(Context context) {
        if (dialog == null) {
            return;
        }
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        TextView textView;
        // start time
        textView = dialog.findViewById(R.id.textView_startTime);
        textView.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_startTime),
                dateFormat.format(startTime)
        ));
        // end time
        textView = dialog.findViewById(R.id.textView_endTime);
        textView.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_endTime),
                dateFormat.format(endTime)
        ));
        // min temperature
        textView = dialog.findViewById(R.id.textView_minTemp);
        if (minTemp != Double.NaN) {
            textView.setText(String.format(
                    Locale.getDefault(),
                    "%s: %s",
                    context.getString(R.string.info_min_temp),
                    TemperatureConverter.getCorrectTemperatureString(context, minTemp))
            );
        } else {
            textView.setVisibility(View.GONE);
        }
        // max temperature
        textView = dialog.findViewById(R.id.textView_maxTemp);
        if (!Double.isNaN(maxTemp)) {
            textView.setText(String.format(
                    Locale.getDefault(),
                    "%s: %s",
                    context.getString(R.string.info_max_temp),
                    TemperatureConverter.getCorrectTemperatureString(context, maxTemp))
            );
        } else {
            textView.setVisibility(View.GONE);
        }
        // min current
        textView = dialog.findViewById(R.id.textView_minCurrent);
        if (!Double.isNaN(minCurrent)) {
            textView.setText(String.format(
                    Locale.getDefault(),
                    "%s: %.1f mAh",
                    context.getString(R.string.info_min_current),
                    minCurrent)
            );
        } else {
            textView.setVisibility(View.GONE);
        }
        // max current
        textView = dialog.findViewById(R.id.textView_maxCurrent);
        if (!Double.isNaN(maxCurrent)) {
            textView.setText(String.format(
                    Locale.getDefault(),
                    "%s: %.1f mAh",
                    context.getString(R.string.info_max_current),
                    maxCurrent)
            );
        } else {
            textView.setVisibility(View.GONE);
        }
        // min voltage
        textView = dialog.findViewById(R.id.textView_minVoltage);
        if (!Double.isNaN(minVoltage)) {
            textView.setText(String.format(
                    Locale.getDefault(),
                    "%s: %.3f V",
                    context.getString(R.string.info_min_voltage),
                    minVoltage)
            );
        } else {
            textView.setVisibility(View.GONE);
        }
        // max voltage
        textView = dialog.findViewById(R.id.textView_maxVoltage);
        if (!Double.isNaN(maxVoltage)) {
            textView.setText(String.format(
                    Locale.getDefault(),
                    "%s: %.3f V",
                    context.getString(R.string.info_max_voltage),
                    maxVoltage)
            );
        } else {
            textView.setVisibility(View.GONE);
        }
        // charging speed
        textView = dialog.findViewById(R.id.textView_speed);
        double speed = chargingSpeed;
        if (!Double.isNaN(speed)) {
            textView.setText(String.format(
                    Locale.getDefault(),
                    "%s: %.2f %%/h",
                    context.getString(R.string.info_charging_speed),
                    speed)
            );
        } else {
            textView.setVisibility(View.GONE);
        }
        // charging time
        textView = dialog.findViewById(R.id.textView_totalTime);
        textView.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_charging_time),
                getTimeString(context))
        );
    }

    public void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    /**
     * Returns the time string with the charging time. The format is defined by the user in the settings.
     *
     * @param context An instance of the Context class.
     * @return Returns the time string with the charging time.
     */
    public String getTimeString(Context context) {
        String[] formats = getTimeFormats(context);
        boolean useSeconds = Boolean.valueOf(formats[3]);
        if (timeInMinutes > 60) { // over an hour
            long hours = (long) timeInMinutes / 60;
            double minutes = (timeInMinutes - hours * 60);
            if (useSeconds) {
                double minutes_floor = Math.floor(minutes);
                double seconds = (minutes - minutes_floor) * 60;
                return String.format(Locale.getDefault(), formats[0], hours, minutes_floor, seconds);
            }
            return String.format(Locale.getDefault(), formats[0], hours, minutes);
        } else if (timeInMinutes > 1) { // under an hour, over a minute
            if (useSeconds) {
                double minutes = Math.floor(timeInMinutes);
                double seconds = (timeInMinutes - minutes) * 60;
                return String.format(Locale.getDefault(), formats[1], minutes, seconds);
            }
            return String.format(Locale.getDefault(), formats[1], timeInMinutes);
        } else { // under a minute
            if (useSeconds) {
                return String.format(Locale.getDefault(), formats[2], timeInMinutes * 60);
            }
            return String.format(Locale.getDefault(), formats[2], timeInMinutes);
        }
    }

    /**
     * Returns the time of charging in minutes.
     *
     * @return Returns the time of charging in minutes.
     */
    public double getTimeInMinutes() {
        return timeInMinutes;
    }
}