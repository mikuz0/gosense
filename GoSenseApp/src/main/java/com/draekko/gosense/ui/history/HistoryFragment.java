package com.draekko.gosense.ui.history;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

import static com.androidplot.xy.OrderedXYSeries.XOrder.DESCENDING;
import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_HPA;
import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_KPA;
import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_MBAR;
import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_PSI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.androidplot.Plot;
import com.androidplot.PlotListener;
import com.androidplot.ui.Anchor;
import com.androidplot.ui.HorizontalPositioning;
import com.androidplot.ui.Insets;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.VerticalPositioning;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.FastXYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.OrderedXYSeries;
import com.androidplot.xy.RectRegion;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.draekko.gosense.R;
import com.draekko.gosense.activities.GoSensePagerActivity;
import com.draekko.gosense.data.AirPressureData;
import com.draekko.gosense.data.AirPressureDataBuffer;
import com.draekko.gosense.data.RelativeHumidityData;
import com.draekko.gosense.data.RelativeHumidityDataBuffer;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.data.TemperatureData;
import com.draekko.gosense.data.TemperatureDataBuffer;
import com.draekko.gosense.database.GoSenseDb;
import com.draekko.gosense.databinding.FragmentHistoryBinding;
import com.draekko.gosense.misc.Converter;
import com.draekko.gosense.misc.DataBuffer;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.misc.Utils;
import com.draekko.gosense.services.AllDevicesService;
import com.draekko.gosense.ui.devices.DevicesFragment;
import com.draekko.tray.TrayPreferences;
import com.draekko.tray.core.OnTrayPreferenceChangeListener;
import com.draekko.tray.core.TrayItem;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.HciStatus;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    public final static int VIEW_TEMPERATURE = 1;
    public final static int VIEW_RELATIVE_HUMIDITY = 2;
    public final static int VIEW_AIR_PRESSURE = 3;

    public final static int TIME_UNIT_10_MINUTE = 1;
    public final static int TIME_UNIT_60_MINUTE = 2;
    public final static int TIME_UNIT_3_HOURS = 3;
    public final static int TIME_UNIT_6_HOURS = 4;
    public final static int TIME_UNIT_12_HOURS = 5;
    public final static int TIME_UNIT_24_HOURS = 6;
    public final static int TIME_UNIT_48_HOURS = 7;
    public final static int TIME_UNIT_7_DAYS = 8;
    public final static int TIME_UNIT_14_DAYS = 9;

    public static final int SIXTY_SECONDS = 60;
    public static final int SIXTY_MINUTES = 60;
    public static final int ONE_SECOND_MILLIS = 1000;
    public static final int DAY_HOURS = 24;

    private static final int MAX_TIME = 14 * DAY_HOURS * SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;

    private DataBuffer<TemperatureData> tempDataBuffer = new DataBuffer<>(MAX_TIME);
    private DataBuffer<RelativeHumidityData> rhDataBuffer = new DataBuffer<>(MAX_TIME);
    private DataBuffer<AirPressureData> apDataBuffer = new DataBuffer<>(MAX_TIME);

    private LineAndPointFormatter graphLineAndPointFormatter;
    private final Format yAxisFormat_zero = new DecimalFormat("0.0");
    private final Format yAxisFormat_not_zero = new DecimalFormat("0");
    private final Format xAxisFormat = new DecimalFormat("0");
    private FragmentHistoryBinding binding;
    private static HistoryFragment mFragment;
    private long mLastDatabaseQuery = System.currentTimeMillis();
    private int mLastIntervalPosition = 0;
    private int mLastUnitPosition = 0;
    private Context mContext;
    private int default_sensor_view = VIEW_TEMPERATURE;
    private int default_time_unit_view = TIME_UNIT_10_MINUTE;
    private LinearLayout[] sensor_type_txt_frm = new LinearLayout[3];
    private TextView[] sensor_type_txt_textview = new TextView[3];
    private ImageView[] sensor_type_txt_imageview = new ImageView[3];
    private TextView[] sensor_time_unit_txt_textview = new TextView[9];
    private int show_outline_id = R.drawable.history_selection_outline;
    private int show_ripple_outline_id = R.drawable.history_selection_ripple_outline;
    private int hide_outline_id = R.drawable.history_selection_gone;
    private Drawable hideOutline;
    private Drawable showOutline;
    private Drawable showRippleOutline;
    private TextView deviceBar;
    private int ooIndexS = 0;
    private int ooIndexT = 0;
    private float alphaOn = 1.0f;
    private float alphaOff = 0.7f;
    private int iconSizeOn = 18;
    private int iconSizeOff = 16;
    private int textSizeOff = 10;
    private int textSizeOn = 12;
    private int fontStyleOff = Typeface.NORMAL;
    private int fontStyleOn = Typeface.BOLD;
    private AllDevicesService mAllDevicesService;
    private String connectedAddress;
    private XYPlot graphXyPlot;
    private boolean mHidden;
    private GoSenseDb goSenseDb;
    //private SimpleXYSeries valuesSeries;
    private DataXYSeries valuesSeries;
    private int graphUpdateInterval = 1;
    private long currentTime = System.currentTimeMillis();
    private long goBackTime = currentTime - (10 * SIXTY_SECONDS * ONE_SECOND_MILLIS);
    private long graphUnit = 1;

    private int sensor_type_frm_ids[] = new int[]{
            R.id.history_type_temperature_frm,
            R.id.history_type_humidity_frm,
            R.id.history_type_pressure_frm
    };

    private int sensor_type_txt_ids[] = new int[]{
            R.id.history_type_temperature_txt,
            R.id.history_type_humidity_txt,
            R.id.history_type_pressure_txt
    };

    private int sensor_type_icn_ids[] = new int[]{
            R.id.history_type_temperature_ic,
            R.id.history_type_humidity_ic,
            R.id.history_type_pressure_ic
    };

    private int sensor_time_unit_txt_ids[] = new int[]{
            R.id.history_time_unit_10m,
            R.id.history_time_unit_60m,
            R.id.history_time_unit_3h,
            R.id.history_time_unit_6h,
            R.id.history_time_unit_12h,
            R.id.history_time_unit_24h,
            R.id.history_time_unit_48h,
            R.id.history_time_unit_7d,
            R.id.history_time_unit_14d
    };

    private DevicesFragment.OnColorChange mOnColorChange = new DevicesFragment.OnColorChange() {
        @Override
        public void colorChanged(int color) {
            Log.i(TAG, String.format("Change color to %08X", color));
            graphLineAndPointFormatter = getColorFormatter(color);
        }
    };

    private GoSensePagerActivity.OnConnnectionStates mHistoryOnConnnectionStates =
            new GoSensePagerActivity.OnConnnectionStates() {
                @Override
                public void onConnected(@NotNull BluetoothPeripheral peripheral) {
                    connectedAddress = peripheral.getAddress();

                    initData();

                    String name = peripheral.getName();
                    if (name == null || name.isEmpty()) {
                        try {
                            SensorDevice sensorPairListData = goSenseDb.getPairedSensorDevice(connectedAddress);
                            if (sensorPairListData != null) {
                                name = sensorPairListData.getAliasName();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (name == null) {
                        name = getString(R.string.empty_bluetooth_device);
                    }
                    String text = name + " - " + connectedAddress;
                    deviceBar.setText(text);

                    handleDeviceSelection();
                    initPlot();
                }

                @Override
                public void onDisconnected(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
                    tempDataBuffer.clear();
                    rhDataBuffer.clear();
                    apDataBuffer.clear();

                    connectedAddress = null;

                    String text = getString(R.string.empty_bluetooth_device) + " - " +
                            getString(R.string.empty_bluetooth_mac_address);
                    deviceBar.setText(text);

                    default_sensor_view = VIEW_TEMPERATURE;
                    default_time_unit_view = TIME_UNIT_10_MINUTE;

                    try {
                        resetPlot();
                    } catch (Exception e) {
                    }
                    try {
                        handleDeviceSelection();
                    } catch (Exception e) {
                    }
                    try {
                        updateRangeAxis(true);
                    } catch (Exception e) {
                    }
                    try {
                        updateDomainAxis(true);
                    } catch (Exception e) {
                    }
                }
            };


    public static HistoryFragment getInstance() {
        if (mFragment == null) {
            mFragment = new HistoryFragment();
        }
        return mFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    private int screenDataPoints;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        screenDataPoints = displayMetrics.widthPixels;

        if (goSenseDb == null) {
            goSenseDb = new GoSenseDb(mContext);
        }
        if (mAllDevicesService != null) {
            GoSensePagerActivity.registerOnConnnectionStates(mHistoryOnConnnectionStates);
        }
        GoSensePagerActivity.registerOnColorChange(mOnColorChange);
        TrayPreferences trayPreferences = TraySettings.getPreferences();
        trayPreferences.registerOnTrayPreferenceChangeListener(new OnTrayPreferenceChangeListener() {
            @Override
            public void onTrayPreferenceChanged(Collection<TrayItem> collection) {
                if (collection == null || collection.size() == 0) {
                    return;
                }
                for (TrayItem trayItem : collection) {
                    String key = trayItem.key();
                    String value = trayItem.value();
                    if (key.contains(TraySettings.PREF_KEY_IS_IMPERIAL_TEMP)
                            && default_sensor_view == VIEW_TEMPERATURE) {
                        updateRangeAxis(true);
                        handleDeviceSelection();
                    }
                    if (key.contains(TraySettings.PREF_KEY_AIR_PRESSURE_UNIT)
                            && default_sensor_view == VIEW_AIR_PRESSURE) {
                        updateRangeAxis(true);
                        handleDeviceSelection();
                    }
                    if (false) Log.i(TAG, "Preference Changed  = " + key + " " + value);
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        graphXyPlot = root.findViewById(R.id.history_fragment_plot);
        updateDomainAxis(true);
        updateRangeAxis(true);

        deviceBar = root.findViewById(R.id.history_current_device);

        hideOutline = mContext.getDrawable(hide_outline_id);
        showOutline = mContext.getDrawable(show_outline_id);
        showRippleOutline = mContext.getDrawable(show_ripple_outline_id);

        iconSizeOn = (int)Utils.pixelsFromDp(mContext,18);
        iconSizeOff = (int)Utils.pixelsFromDp(mContext,16);

        for (int oo = 0; oo < sensor_type_frm_ids.length; oo++) {
            sensor_type_txt_frm[oo] = root.findViewById(sensor_type_frm_ids[oo]);
            sensor_type_txt_textview[oo] = root.findViewById(sensor_type_txt_ids[oo]);
            sensor_type_txt_imageview[oo] = root.findViewById(sensor_type_icn_ids[oo]);

            if (oo == 2 && !TraySettings.getHasBarometer()) {
                sensor_type_txt_frm[oo].setVisibility(View.GONE);
                continue;
            }

            if (oo == 0) {
                sensor_type_txt_frm[oo].setBackground(showOutline);
                sensor_type_txt_textview[oo].setTypeface(null, fontStyleOn);
                sensor_type_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOn);
                sensor_type_txt_textview[oo].setAlpha(alphaOn);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iconSizeOn, iconSizeOn);
                lp.gravity = Gravity.CENTER;
                sensor_type_txt_imageview[oo].setLayoutParams(lp);
                sensor_type_txt_imageview[oo].setAlpha(alphaOn);
            } else {
                sensor_type_txt_frm[oo].setBackground(hideOutline);
                sensor_type_txt_textview[oo].setTypeface(null, fontStyleOff);
                sensor_type_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOff);
                sensor_type_txt_textview[oo].setAlpha(alphaOff);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iconSizeOff, iconSizeOff);
                lp.gravity = Gravity.CENTER;
                sensor_type_txt_imageview[oo].setLayoutParams(lp);
                sensor_type_txt_imageview[oo].setAlpha(alphaOff);
            }

            sensor_type_txt_frm[oo].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleSensorClick(view);
                }
            });
        }

        for (int oo = 0; oo < sensor_time_unit_txt_textview.length; oo++) {
            sensor_time_unit_txt_textview[oo] = root.findViewById(sensor_time_unit_txt_ids[oo]);
            if (oo == 0) {
                sensor_time_unit_txt_textview[oo].setBackground(showOutline);
                sensor_time_unit_txt_textview[oo].setTypeface(null, fontStyleOn);
                sensor_time_unit_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOn);
                sensor_time_unit_txt_textview[oo].setAlpha(alphaOn);
            } else {
                sensor_time_unit_txt_textview[oo].setBackground(hideOutline);
                sensor_time_unit_txt_textview[oo].setTypeface(null, fontStyleOff);
                sensor_time_unit_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOff);
                sensor_time_unit_txt_textview[oo].setAlpha(alphaOff);
            }

            sensor_time_unit_txt_textview[oo].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleTimeUnitClick(view);
                }
            });
        }

        initPlot();

        return root;
    }

    public void setOnHidden(boolean hidden) {
        mHidden = hidden;
        if (!mHidden) {
            if (mAllDevicesService != null) {
                GoSensePagerActivity.registerOnConnnectionStates(mHistoryOnConnnectionStates);
            }
            if (connectedAddress == null && mAllDevicesService != null) {
                List<BluetoothPeripheral> list = mAllDevicesService.getConnectedPeripherals();
                if (list.size() > 0) {
                    connectedAddress = list.get(0).getAddress();
                }
            }
        }
        if (false) Log.i(TAG, "setOnHidden: " + mHidden);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAllDevicesService == null) {
            mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
            if (mAllDevicesService == null) {
                startAllDevicesServiceWatcher();
            }
        }
        if (mAllDevicesService != null) {
            GoSensePagerActivity.registerOnConnnectionStates(mHistoryOnConnnectionStates);
        }

        if (goSenseDb == null) {
            goSenseDb = new GoSenseDb(mContext);
        }

        sensorFetch();
    }

    private void startAllDevicesServiceWatcher() {
        if (mAllDevicesService == null) {
            mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
            if (mAllDevicesService == null) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAllDevicesServiceWatcher();
                    }
                }, 100);
                return;
            }
            GoSensePagerActivity.registerOnConnnectionStates(mHistoryOnConnnectionStates);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAllDevicesService != null) {
            GoSensePagerActivity.unregisterOnConnnectionStates(mHistoryOnConnnectionStates);
        }
        GoSensePagerActivity.unregisterOnColorChange(mOnColorChange);
        if (goSenseDb != null) {
            goSenseDb.close();
            goSenseDb = null;
        }
        binding = null;
    }

    private void handleSensorClick(View view) {
        for (int oo = 0; oo < sensor_type_frm_ids.length; oo++) {
            if (sensor_type_frm_ids[oo] == view.getId()) {
                sensor_type_txt_frm[oo].setBackground(showRippleOutline);
                sensor_type_txt_textview[oo].setTypeface(null, fontStyleOn);
                sensor_type_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOn);
                sensor_type_txt_textview[oo].setAlpha(alphaOn);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iconSizeOn, iconSizeOn);
                lp.gravity = Gravity.CENTER;
                sensor_type_txt_imageview[oo].setLayoutParams(lp);
                sensor_type_txt_imageview[oo].setAlpha(alphaOn);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sensor_type_txt_frm[ooIndexS].setBackground(showOutline);
                        handleDeviceSelection();
                    }
                }, 340);
                if (default_sensor_view == oo+1) {
                    return;
                }
                default_sensor_view = oo+1;
                ooIndexS = oo;
                updateRangeAxis(true);
            } else {
                sensor_type_txt_frm[oo].setBackground(hideOutline);
                sensor_type_txt_textview[oo].setTypeface(null, fontStyleOff);
                sensor_type_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOff);
                sensor_type_txt_textview[oo].setAlpha(alphaOff);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(iconSizeOff, iconSizeOff);
                lp.gravity = Gravity.CENTER;
                sensor_type_txt_imageview[oo].setLayoutParams(lp);
                sensor_type_txt_imageview[oo].setAlpha(alphaOff);
            }
        }
        graphXyPlot.clear();
        handleDeviceSelection();
        updateRangeAxis(true);
        if (lockUpdatePlot) {
            if (!drawing.isInterrupted()) {
                drawing.interrupt();
                lockUpdatePlot = false;
            }
        }
        updatePlot();
    }

    private void handleTimeUnitClick(View view) {
        for (int oo = 0; oo < sensor_time_unit_txt_ids.length; oo++) {
            if (sensor_time_unit_txt_ids[oo] == view.getId()) {
                sensor_time_unit_txt_textview[oo].setBackground(showRippleOutline);
                sensor_time_unit_txt_textview[oo].setTypeface(null, fontStyleOn);
                sensor_time_unit_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOn);
                sensor_time_unit_txt_textview[oo].setAlpha(alphaOn);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sensor_time_unit_txt_textview[ooIndexT].setBackground(showOutline);
                        handleDeviceSelection();
                    }
                }, 340);
                if (default_time_unit_view == oo+1) {
                    return;
                }
                default_time_unit_view = oo+1;
                ooIndexT = oo;
                updateDomainAxis(true);
            } else {
                sensor_time_unit_txt_textview[oo].setBackground(hideOutline);
                sensor_time_unit_txt_textview[oo].setTypeface(null, fontStyleOff);
                sensor_time_unit_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOff);
                sensor_time_unit_txt_textview[oo].setAlpha(alphaOff);
            }
        }
        graphXyPlot.clear();
        handleDeviceSelection();
        updateDomainAxis(true);
        if (lockUpdatePlot) {
            if (!drawing.isInterrupted()) {
                drawing.interrupt();
                lockUpdatePlot = false;
            }
        }
        updatePlot();
    }

    public void handleDeviceSelection() {
        if (mAllDevicesService != null) {
            if (connectedAddress == null) {
                boolean connected = mAllDevicesService.getConnectedPeripherals().size() > 0;
                if (connected) {
                    if (connectedAddress == null || connectedAddress.isEmpty()) {
                        connectedAddress = mAllDevicesService.getConnectedPeripherals().get(0).getAddress();
                    }
                }
            }

            if (connectedAddress != null) {
                adjustGraph();
                //updateDomainAxis(true);
            }
        }
    }

    private void sleepWait(long waitTime) {
        long timeEnd = System.currentTimeMillis() + waitTime;
        while (System.currentTimeMillis() <= timeEnd);
    }

    private AllDevicesService.OnAirPressureSensorChange mOnAirPressureSensorChange =
            new AllDevicesService.OnAirPressureSensorChange() {
                @Override
                public void airPressure(String address, long time, float airpressure) {
                    adjustGraph();
                    apDataBuffer.setTimeLimit(goBackTime);
                    apDataBuffer.push(new AirPressureData(time, airpressure, -9999, -9999, "", address));
                }
            };

    private AllDevicesService.OnTemperatureSensorChange mOnTemperatureSensorChange =
            new AllDevicesService.OnTemperatureSensorChange() {
                @Override
                public void temperature(String address, long time, float temperature) {
                    adjustGraph();
                    tempDataBuffer.setTimeLimit(goBackTime);
                    tempDataBuffer.push(new TemperatureData(time, temperature, -9999, -9999, "", address));
                }
            };

    private AllDevicesService.OnRelativeHumiditySensorChange mOnRelativeHumiditySensorChange =
            new AllDevicesService.OnRelativeHumiditySensorChange() {
                @Override
                public void relativeHumidity(String address, long time, float humidity) {
                    adjustGraph();
                    rhDataBuffer.setTimeLimit(goBackTime);
                    rhDataBuffer.push(new RelativeHumidityData(time, humidity, -9999, -9999, "", address));
                }
            };

    private void initData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<TemperatureData> tData = goSenseDb.getTemperatureAddressAndRange(connectedAddress, MAX_TIME, currentTime);
                tempDataBuffer = new TemperatureDataBuffer(MAX_TIME, (ArrayList<TemperatureData>) tData.clone());
                tData.clear();
                tData = null;

                ArrayList<RelativeHumidityData> rhData = goSenseDb.getRelativeHumidityAddressAndRange(connectedAddress, MAX_TIME, currentTime);
                rhDataBuffer = new RelativeHumidityDataBuffer(MAX_TIME, (ArrayList<RelativeHumidityData>) rhData.clone());
                rhData.clear();
                rhData = null;

                ArrayList<AirPressureData> apData = goSenseDb.getAirPressureAddressAndRange(MAX_TIME, currentTime);
                apDataBuffer = new AirPressureDataBuffer(MAX_TIME, (ArrayList<AirPressureData>) apData.clone());
                apData.clear();
                apData = null;

                if (mAllDevicesService != null) {
                    mAllDevicesService.registerOnAirPressureSensorChange(mOnAirPressureSensorChange);
                    mAllDevicesService.registerOnTemperatureSensorChange(mOnTemperatureSensorChange);
                    mAllDevicesService.registerOnRelativeHumiditySensorChange(mOnRelativeHumiditySensorChange);
                }
            }
        }).start();
    }

    private void fetchData() {
        if (connectedAddress != null) {
            graphLineAndPointFormatter = getDeviceFormatter();
        }

        switch (default_sensor_view) {
            case VIEW_TEMPERATURE: {
                ArrayList<TemperatureData> tData = goSenseDb.getTemperatureAddressAndRange(connectedAddress, goBackTime, currentTime);
                tempDataBuffer = new TemperatureDataBuffer(MAX_TIME, (ArrayList<TemperatureData>) tData.clone());
                tData.clear();
                tData = null;
            }
            break;
            case VIEW_RELATIVE_HUMIDITY: {
                ArrayList<RelativeHumidityData> rhData = goSenseDb.getRelativeHumidityAddressAndRange(connectedAddress, goBackTime, currentTime);
                rhDataBuffer = new RelativeHumidityDataBuffer(MAX_TIME, (ArrayList<RelativeHumidityData>) rhData.clone());
                rhData.clear();
                rhData = null;
            }
            break;
            case VIEW_AIR_PRESSURE: {
                ArrayList<AirPressureData> apData = goSenseDb.getAirPressureAddressAndRange(goBackTime, currentTime);
                apDataBuffer = new AirPressureDataBuffer(MAX_TIME, (ArrayList<AirPressureData>) apData.clone());
                apData.clear();
                apData = null;
            }
            break;
        }
    }


    private void updateDomainAxis(boolean invalidate) {
        String domainLabel = getString(R.string.elapsed_time);
        double minTimeUnit = 0;
        double maxTimeUnit = 10;
        double valOffset = 10;
        switch (default_time_unit_view) {
            case TIME_UNIT_10_MINUTE:
                minTimeUnit = 0;
                maxTimeUnit = 10;
                domainLabel += "  (Minutes)";
                valOffset = 1;
                break;

            case TIME_UNIT_60_MINUTE:
                minTimeUnit = 0;
                maxTimeUnit = 60;
                domainLabel += "  (Minutes)";
                valOffset = 10;
                break;

            case TIME_UNIT_3_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 3;
                domainLabel += "  (Hours)";
                valOffset = 1;
                break;

            case TIME_UNIT_6_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 6;
                domainLabel += "  (Hours)";
                valOffset = 1;
                break;

            case TIME_UNIT_12_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 12;
                domainLabel += "  (Hours)";
                valOffset = 1;
                break;

            case TIME_UNIT_24_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 24;
                domainLabel += "  (Hours)";
                valOffset = 2;
                break;

            case TIME_UNIT_48_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 48;
                domainLabel += "  (Hours)";
                valOffset = 4;
                break;

            case TIME_UNIT_7_DAYS:
                minTimeUnit = 0;
                maxTimeUnit = 7;
                domainLabel += "  (Days)";
                valOffset = 1;
                break;

            case TIME_UNIT_14_DAYS:
                minTimeUnit = 0;
                maxTimeUnit = 14;
                domainLabel += "  (Days)";
                valOffset = 1;
                break;
        }

        graphXyPlot.getGraph().setMarginTop(Utils.pixelsFromDp(mContext, 5));
        graphXyPlot.getRangeTitle().setMarginLeft(Utils.pixelsFromDp(mContext, 4));
        graphXyPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, valOffset);
        graphXyPlot.setDomainBoundaries(maxTimeUnit, minTimeUnit, BoundaryMode.FIXED);
        graphXyPlot.setDomainLabel(domainLabel.toUpperCase(Locale.US));

        if (invalidate) {
            graphXyPlot.redraw();
        }
    }

    private void updateRangeAxis(boolean invalidate) {
        String rangeLabel = "Environmental Units";
        double minRangeUnits = 0;
        double maxRangeUnits = 100;
        float insetLeft = Utils.pixelsFromDp(mContext, 31);
        double rangeSteps = 10;
        switch (default_sensor_view) {
            case VIEW_TEMPERATURE:
                rangeLabel = getString(R.string.temperature);
                if (TraySettings.isTemperatureUnitFahrenheit()) {
                    minRangeUnits = -10;
                    maxRangeUnits = 140;
                    rangeLabel += "  (" + getString(R.string.unit_fahrenheit) + ")";
                    graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_not_zero);
                    insetLeft = Utils.pixelsFromDp(mContext, 25);
                } else {
                    minRangeUnits = -20;
                    maxRangeUnits = 60;
                    rangeLabel += "  (" + getString(R.string.unit_celsius) + ")";
                    graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_not_zero);
                    insetLeft = Utils.pixelsFromDp(mContext, 21);
                }
                break;

            case VIEW_RELATIVE_HUMIDITY:
                rangeLabel = getString(R.string.relative_humidity) + "  (%)";
                minRangeUnits = 0;
                maxRangeUnits = 100;
                graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_not_zero);
                insetLeft = Utils.pixelsFromDp(mContext, 25);
                break;

            case VIEW_AIR_PRESSURE:
                rangeLabel = getString(R.string.air_pressure);
                int apUnit = TraySettings.getAirPressureUnit();
                switch (apUnit) {
                    case AIR_PRESSURE_MBAR:
                    case AIR_PRESSURE_HPA:
                        minRangeUnits = 840;
                        maxRangeUnits = 1110;
                        rangeSteps = 27;
                        if (apUnit == AIR_PRESSURE_MBAR) {
                            rangeLabel += "  (mbar)";
                        } else {
                            rangeLabel += "  (hPa)";
                        }
                        graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_not_zero);
                        insetLeft = Utils.pixelsFromDp(mContext, 31);
                        break;

                    case AIR_PRESSURE_KPA:
                        minRangeUnits = 84;
                        maxRangeUnits = 111;
                        rangeLabel += "  (kPa)";
                        rangeSteps = 2.7;
                        graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_not_zero);
                        insetLeft = Utils.pixelsFromDp(mContext, 25);
                        break;

                    case AIR_PRESSURE_PSI:
                        minRangeUnits = 11;
                        maxRangeUnits = 16;
                        rangeLabel += "  (Psi)";
                        rangeSteps = 0.5;
                        graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                        insetLeft = Utils.pixelsFromDp(mContext, 29);
                        break;
                }
                break;
        }

        graphXyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, rangeSteps);

        Insets gridInsets = graphXyPlot.getGraph().getGridInsets();
        gridInsets.setLeft(insetLeft);
        graphXyPlot.getGraph().setGridInsets(gridInsets);

        graphXyPlot.setRangeLabel(rangeLabel.toUpperCase(Locale.US));
        graphXyPlot.setRangeBoundaries(minRangeUnits, maxRangeUnits, BoundaryMode.FIXED);
        if (invalidate) {
            graphXyPlot.redraw();
        }
    }

    private ArrayList<Double> xVals = new ArrayList<>();
    private ArrayList<Double> yVals = new ArrayList<>();
    private ArrayList<Double> lxVals = new ArrayList<>();
    private ArrayList<Double> lyVals = new ArrayList<>();

    private class DataXYSeries implements OrderedXYSeries, PlotListener {
        private ArrayList<Double> xVals = new ArrayList<>();
        private ArrayList<Double> yVals = new ArrayList<>();
        private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

        public DataXYSeries(ArrayList<Double> xVals, ArrayList<Double> yVals) {
            clear();
            lock.writeLock().lock();
            this.xVals.addAll(xVals);
            this.yVals.addAll(yVals);
            lock.writeLock().unlock();
        }

        public void clear() {
            lock.writeLock().lock();
            try {
                xVals.clear();
                yVals.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public void onBeforeDraw(Plot source, Canvas canvas) {
            lock.readLock().lock();
        }

        @Override
        public void onAfterDraw(Plot source, Canvas canvas) {
            lock.readLock().unlock();
        }

        @Override
        public XOrder getXOrder() {
            return DESCENDING;
        }

        @Override
        public int size() {
            return xVals.size();
        }

        @Override
        public Number getX(int i) {
            return xVals.get(i);
        }

        @Override
        public Number getY(int i) {
            return yVals.get(i);
        }

        @Override
        public String getTitle() {
            return "";
        }
    }

    private Thread drawing;
    private boolean lockUpdatePlot = false;
    private void updatePlot() {
        //graphUpdateInterval = TraySettings.getGraphUpdateInterval();
        if (!mHidden && connectedAddress != null) {
            currentTime = System.currentTimeMillis();

            if (graphLineAndPointFormatter == null) {
                graphLineAndPointFormatter = getDeviceFormatter();
            }

            if (true) {
                drawing = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lockUpdatePlot = true;
                        switch (default_sensor_view) {
                            case VIEW_TEMPERATURE: {
                                if(lockDrawTemp) return;
                                handleTemperature();
                                break;
                            }
                            case VIEW_RELATIVE_HUMIDITY: {
                                if(lockDrawRH) return;
                                handleRelativeHumidity();
                                break;
                            }
                            case VIEW_AIR_PRESSURE: {
                                if(lockDrawAP) return;
                                handleAirPressure();
                                break;
                            }
                        }
                        lockUpdatePlot = false;
                    }
                });
                drawing.start();
            } else {
                switch (default_sensor_view) {
                    case VIEW_TEMPERATURE: {
                        if(lockDrawTemp) return;
                        handleTemperature();
                        break;
                    }
                    case VIEW_RELATIVE_HUMIDITY: {
                        if(lockDrawRH) return;
                        handleRelativeHumidity();
                        break;
                    }
                    case VIEW_AIR_PRESSURE: {
                        if(lockDrawAP) return;
                        handleAirPressure();
                        break;
                    }
                }
            }
        }
    }

    private boolean lockDrawTemp = false;
    private boolean lockDrawRH = false;
    private boolean lockDrawAP = false;
    private ArrayList<TemperatureData> temperatureData;
    private ArrayList<RelativeHumidityData> relativeHumidityData;
    private ArrayList<AirPressureData> airPressureData;
    private void handleTemperature() {
        lockDrawTemp = true;

        boolean  doAveraging = TraySettings.getUseAverageGraph();

        boolean tempF = TraySettings.isTemperatureUnitFahrenheit();
        String tempUnit = getString(R.string.unit_celsius);
        if (tempF) {
            tempUnit = getString(R.string.unit_fahrenheit);
        }

        double minT = 999999;
        double maxT = -999999;

        if (temperatureData == null || temperatureData.size() == 0) {
            temperatureData = tempDataBuffer.getDataBuffer();
        }

        adjustGraph();

        xVals.clear();
        yVals.clear();

        int tSize = temperatureData.size();
        int startIndex = 0;

        Log.i(TAG, "temperatureData values size " + tSize);
        if (temperatureData != null && tSize > 0) {
            for (int tt = 0; tt < tSize; tt++) {
                if (temperatureData.get(tt).getTimestamp() >= goBackTime) {
                    startIndex = tt;
                    break;
                }
            }
            int maxOffset = 1000;
            int indices = tSize - startIndex;
            float offset = 1;
            if (indices > maxOffset) {
                offset = indices / maxOffset;
            }
            if (TraySettings.getUseAverageGraph() && indices > maxOffset) {
                int lastIndex = startIndex;
                for (int tt = startIndex; tt < tSize; tt+=offset) {
                    double temperature = temperatureData.get(tt).getTemperature();
                    if (doAveraging) {
                        if (lastIndex != startIndex) {
                            for (int ll = lastIndex; ll < tt; ll++) {
                                float temp = temperatureData.get(ll).getTemperature();
                                if (temp != -9999) {
                                    temperature += temperatureData.get(ll).getTemperature();
                                }
                            }
                            temperature /= (tt - lastIndex);
                        }
                    }
                    if (temperature != -9999 && tempF) {
                        temperature = Converter.convertToF(temperatureData.get(tt).getTemperature());
                    }
                    if (doAutoScale()) {
                        if (temperature != -9999 && temperature < minT) {
                            minT = temperature;
                        }
                        if (temperature != -9999 && temperature > maxT) {
                            maxT = temperature;
                        }
                    }
                    double graphTime = (double)(currentTime - temperatureData.get(tt).getTimestamp()) / (double)graphUnit;
                    xVals.add(graphTime);
                    yVals.add(temperature);
                    lastIndex = startIndex;
                }
            } else {
                for (int tt = startIndex; tt < tSize; tt+=offset) {
                    double temperature = temperatureData.get(tt).getTemperature();
                    if (temperature != -9999 && tempF) {
                        temperature = Converter.convertToF(temperatureData.get(tt).getTemperature());
                    }
                    if (doAutoScale()) {
                        if (temperature != -9999 && temperature < minT) {
                            minT = temperature;
                        }
                        if (temperature != -9999 && temperature > maxT) {
                            maxT = temperature;
                        }
                    }
                    double graphTime = (double)(currentTime - temperatureData.get(tt).getTimestamp()) / (double)graphUnit;
                    xVals.add(graphTime);
                    yVals.add(temperature);
                }
            }

            Log.i(TAG, "temp X/Y values size " + xVals.size());

            //graphXyPlot.removeSeries(valuesSeries);
            graphXyPlot.clear();
            //valuesSeries = new SimpleXYSeries(xVals, yVals, "T " + tempUnit);
            valuesSeries = new DataXYSeries(xVals, yVals);
            graphXyPlot.addSeries(valuesSeries, graphLineAndPointFormatter);

            if (doAutoScale()) {
                String dispUnit = getString(R.string.unit_celsius);
                if (TraySettings.isTemperatureUnitFahrenheit()) {
                    dispUnit = getString(R.string.unit_fahrenheit);
                }
                String rangeLabel = getString(R.string.temperature);
                double minRangeUnits = 0;
                double maxRangeUnits = 100;
                float insetLeft = Utils.pixelsFromDp(mContext, 31);
                double rangeSteps = 10;
                minRangeUnits = (int)(minT - 1.0);
                maxRangeUnits = (int)(maxT + 1.0);
                if (TraySettings.isTemperatureUnitFahrenheit()) {
                    rangeLabel += "  (" + getString(R.string.unit_fahrenheit) + ")";
                    graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                    insetLeft = Utils.pixelsFromDp(mContext, 25);
                } else {
                    rangeLabel += "  (" + getString(R.string.unit_celsius) + ")";
                    graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                    insetLeft = Utils.pixelsFromDp(mContext, 21);
                }
                graphXyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, (maxRangeUnits - minRangeUnits) / rangeSteps);
                Insets gridInsets = graphXyPlot.getGraph().getGridInsets();
                gridInsets.setLeft(insetLeft);
                graphXyPlot.getGraph().setGridInsets(gridInsets);
                graphXyPlot.setRangeLabel(rangeLabel.toUpperCase(Locale.US));
                graphXyPlot.setRangeBoundaries(minRangeUnits, maxRangeUnits, BoundaryMode.FIXED);
            }

            graphXyPlot.redraw();
        }

        lockDrawTemp = false;
    }

    private void handleRelativeHumidity() {
        lockDrawRH = true;

        boolean  doAveraging = TraySettings.getUseAverageGraph();

        double minRh = 999999;
        double maxRh = -999999;

        if (relativeHumidityData == null || relativeHumidityData.size() == 0) {
            relativeHumidityData = rhDataBuffer.getDataBuffer();
        }

        adjustGraph();

        xVals.clear();
        yVals.clear();

        int tSize = relativeHumidityData.size();
        int startIndex = 0;

        Log.i(TAG, "relativeHumidityData values size " + tSize);
        if (relativeHumidityData != null && tSize > 0) {
            for (int tt = 0; tt < tSize; tt++) {
                if (relativeHumidityData.get(tt).getTimestamp() >= goBackTime) {
                    startIndex = tt;
                    break;
                }
            }
            int indices = tSize - startIndex;
            float offset = 1;
            if (indices > 120) {
                offset = indices / 120;
            }
            if (TraySettings.getUseAverageGraph() && indices > 120) {
                int lastIndex = startIndex;
                for (int tt = startIndex; tt < tSize; tt+=offset) {
                    double relativeHumidity = relativeHumidityData.get(tt).getRelativeHumidity();
                    if (doAveraging) {
                        if (lastIndex != startIndex) {
                            for (int ll = lastIndex; ll < tt; ll++) {
                                float temp = relativeHumidityData.get(ll).getRelativeHumidity();
                                if (temp != -9999) {
                                    relativeHumidity += relativeHumidityData.get(ll).getRelativeHumidity();
                                }
                            }
                            relativeHumidity /= (tt - lastIndex);
                        }
                    }
                    if (doAutoScale()) {
                        if (relativeHumidity != -9999 && relativeHumidity < minRh) {
                            minRh = relativeHumidity;
                        }
                        if (relativeHumidity != -9999 && relativeHumidity > maxRh) {
                            maxRh = relativeHumidity;
                        }
                    }
                    double graphTime = (double)(currentTime - relativeHumidityData.get(tt).getTimestamp()) / (double)graphUnit;
                    xVals.add(graphTime);
                    yVals.add(relativeHumidity);
                    lastIndex = startIndex;
                }
            } else {
                for (int tt = startIndex; tt < tSize; tt+=offset) {
                    double relativeHumidity = relativeHumidityData.get(tt).getRelativeHumidity();
                    if (doAutoScale()) {
                        if (relativeHumidity != -9999 && relativeHumidity < minRh) {
                            minRh = relativeHumidity;
                        }
                        if (relativeHumidity != -9999 && relativeHumidity > maxRh) {
                            maxRh = relativeHumidity;
                        }
                    }
                    double graphTime = (double)(currentTime - relativeHumidityData.get(tt).getTimestamp()) / (double)graphUnit;
                    xVals.add(graphTime);
                    yVals.add(relativeHumidity);
                }
            }

            Log.i(TAG, "RH X/Y values size " + xVals.size());

            //graphXyPlot.removeSeries(valuesSeries);
            graphXyPlot.clear();
            //valuesSeries = new SimpleXYSeries(xVals, yVals, "RH %");
            valuesSeries = new DataXYSeries(xVals, yVals);
            graphXyPlot.addSeries(valuesSeries, graphLineAndPointFormatter);

            if (doAutoScale()) {
                String rangeLabel = getString(R.string.relative_humidity) + "  (%)";
                float insetLeft = Utils.pixelsFromDp(mContext, 25);
                double rangeSteps = 10;
                double minRangeUnits = (int)(minRh - 1.0);
                double maxRangeUnits = (int)(maxRh + 1.0);
                Insets gridInsets = graphXyPlot.getGraph().getGridInsets();
                gridInsets.setLeft(insetLeft);
                graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                graphXyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, (maxRangeUnits - minRangeUnits) / rangeSteps);
                graphXyPlot.getGraph().setGridInsets(gridInsets);
                graphXyPlot.setRangeLabel(rangeLabel.toUpperCase(Locale.US));
                graphXyPlot.setRangeBoundaries(minRangeUnits, maxRangeUnits, BoundaryMode.FIXED);
            }

            graphXyPlot.redraw();
        }

        lockDrawRH = false;
    }

    private void handleAirPressure() {
        lockDrawAP = true;

        boolean  doAveraging = TraySettings.getUseAverageGraph();

        String unit = "mbar";
        int apUnit = TraySettings.getAirPressureUnit();
        switch (apUnit) {
            case AIR_PRESSURE_HPA:
                unit = "hPa";
                break;
            case AIR_PRESSURE_KPA:
                unit = "kPa";
                break;
            case AIR_PRESSURE_PSI:
                unit = "Psi";
                break;
            case AIR_PRESSURE_MBAR:
                unit = "mbar";
                break;
        }

        double minAp = 999999;
        double maxAp = -999999;

        if (airPressureData == null || airPressureData.size() == 0) {
            airPressureData = apDataBuffer.getDataBuffer();
        }

        adjustGraph();

        xVals.clear();
        yVals.clear();

        int tSize = airPressureData.size();
        int startIndex = 0;

        Log.i(TAG, "airPressureData values size " + tSize);
        if (airPressureData != null && tSize > 0) {
            for (int tt = 0; tt < tSize; tt++) {
                if (airPressureData.get(tt).getTimestamp() >= goBackTime) {
                    startIndex = tt;
                    break;
                }
            }
            int indices = tSize - startIndex;
            float offset = 1;
            if (indices > 120) {
                offset = indices / 120;
            }
            if (TraySettings.getUseAverageGraph() && indices > 120) {
                int lastIndex = startIndex;
                for (int tt = startIndex; tt < tSize; tt+=offset) {
                    double airPressure = airPressureData.get(tt).getAirPressure();
                    if (airPressure != -9999 && apUnit == AIR_PRESSURE_KPA) {
                        airPressure /= 10.0;
                    }
                    if (airPressure != -9999 && apUnit == AIR_PRESSURE_PSI) {
                        airPressure *= Utils.PSI_VALUE_ONE_MILLIBAR;
                    }
                    if (doAveraging) {
                        if (lastIndex != startIndex) {
                            for (int ll = lastIndex; ll < tt; ll++) {
                                float temp = airPressureData.get(ll).getAirPressure();
                                if (temp != -9999) {
                                    airPressure += airPressureData.get(ll).getAirPressure();
                                }
                            }
                            airPressure /= (tt - lastIndex);
                        }
                    }
                    if (doAutoScale()) {
                        if (airPressure != -9999 && airPressure < minAp) {
                            minAp = airPressure;
                        }
                        if (airPressure != -9999 && airPressure > maxAp) {
                            maxAp = airPressure;
                        }
                    }
                    double graphTime = (double)(currentTime - airPressureData.get(tt).getTimestamp()) / (double)graphUnit;
                    xVals.add(graphTime);
                    yVals.add(airPressure);
                    lastIndex = startIndex;
                }
            } else {
                for (int tt = startIndex; tt < tSize; tt+=offset) {
                    double airPressure = airPressureData.get(tt).getAirPressure();
                    if (airPressure != -9999 && apUnit == AIR_PRESSURE_KPA) {
                        airPressure /= 10.0;
                    }
                    if (airPressure != -9999 && apUnit == AIR_PRESSURE_PSI) {
                        airPressure *= Utils.PSI_VALUE_ONE_MILLIBAR;
                    }
                    if (doAutoScale()) {
                        if (airPressure != -9999 && airPressure < minAp) {
                            minAp = airPressure;
                        }
                        if (airPressure != -9999 && airPressure > maxAp) {
                            maxAp = airPressure;
                        }
                    }
                    double graphTime = (double)(currentTime - airPressureData.get(tt).getTimestamp()) / (double)graphUnit;
                    xVals.add(graphTime);
                    yVals.add(airPressure);
                }
            }

            Log.i(TAG, "AP X/Y values size " + xVals.size());

            //graphXyPlot.removeSeries(valuesSeries);
            graphXyPlot.clear();

            //valuesSeries = new SimpleXYSeries(xVals, yVals, "AP " + unit);
            valuesSeries = new DataXYSeries(xVals, yVals);
            graphXyPlot.addSeries(valuesSeries, graphLineAndPointFormatter);

            if (doAutoScale()) {
                String rangeLabel = getString(R.string.air_pressure) + "  ("+unit+")";
                float insetLeft = Utils.pixelsFromDp(mContext, 25);
                double rangeSteps = 10;
                double offsetAp = 5.0;
                switch (apUnit) {
                    case AIR_PRESSURE_HPA:
                        offsetAp = 1.0;
                        insetLeft = Utils.pixelsFromDp(mContext, 39);
                        break;
                    case AIR_PRESSURE_KPA:
                        offsetAp = 1.0;
                        insetLeft = Utils.pixelsFromDp(mContext, 35);
                        break;
                    case AIR_PRESSURE_PSI:
                        offsetAp = 0.5;
                        insetLeft = Utils.pixelsFromDp(mContext, 27);
                        break;
                    case AIR_PRESSURE_MBAR:
                        offsetAp = 1.0;
                        insetLeft = Utils.pixelsFromDp(mContext, 39);
                        break;
                }
                double minRangeUnits = (int)(minAp - offsetAp);
                double maxRangeUnits = (int)(maxAp + offsetAp);
                Insets gridInsets = graphXyPlot.getGraph().getGridInsets();
                gridInsets.setLeft(insetLeft);
                graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                graphXyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, (maxRangeUnits - minRangeUnits) / rangeSteps);
                graphXyPlot.getGraph().setGridInsets(gridInsets);
                graphXyPlot.setRangeLabel(rangeLabel.toUpperCase(Locale.US));
                graphXyPlot.setRangeBoundaries(minRangeUnits, maxRangeUnits, BoundaryMode.FIXED);
            }

            graphXyPlot.redraw();
        }

        lockDrawAP = false;
    }

    private void adjustGraph() {
        currentTime = System.currentTimeMillis();
        switch (default_time_unit_view) {
            case TIME_UNIT_10_MINUTE:
                graphUnit = SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (11 * graphUnit);
                //graphUpdateInterval = TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_60_MINUTE:
                graphUnit = SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (SIXTY_MINUTES * graphUnit);
                //graphUpdateInterval = TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_3_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (3 * graphUnit);
                //graphUpdateInterval = (SIXTY_MINUTES / 2) * TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_6_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (6 * graphUnit);
                //graphUpdateInterval = (SIXTY_MINUTES / 2) * TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_12_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (12 * graphUnit);
                //graphUpdateInterval = (SIXTY_MINUTES / 2) * TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_24_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (DAY_HOURS * graphUnit);
                //graphUpdateInterval = (SIXTY_MINUTES / 2) * TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_48_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (2 * DAY_HOURS * graphUnit);
                //graphUpdateInterval = (SIXTY_MINUTES / 2) * TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_7_DAYS:
                graphUnit = DAY_HOURS * SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (7 * graphUnit);
                //graphUpdateInterval = (SIXTY_MINUTES / 2) * TraySettings.getGraphUpdateInterval();
                break;

            case TIME_UNIT_14_DAYS:
                graphUnit = DAY_HOURS * SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (14 * graphUnit);
                //graphUpdateInterval = (SIXTY_MINUTES / 2) * TraySettings.getGraphUpdateInterval();
                break;
        }
    }

    private void sensorFetch() {
        graphUpdateInterval = TraySettings.getGraphUpdateInterval();

        if (!lockUpdatePlot)  {
            graphXyPlot.clear();
            updatePlot();
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                sensorFetch();
            }
        }, graphUpdateInterval * ONE_SECOND_MILLIS);
    }

    private LineAndPointFormatter getColorFormatter(int lineColor) {
        final Paint fillColor = new Paint();
        fillColor.setColor(lineColor);
        fillColor.setAlpha(45);
        return new LineAndPointFormatter(lineColor, null, fillColor.getColor(), null);
    }

    private LineAndPointFormatter getDefaultFormatter() {
        return getColorFormatter(Utils.DEFAULT_COLOR);
    }

    private LineAndPointFormatter getDeviceFormatter() {
        if (connectedAddress == null) {
            return getDefaultFormatter();
        }
        int devColor = Utils.getGraphColorFromPairedDb(mContext, connectedAddress);
        return getColorFormatter(devColor);
    }

    private void initPlot() {
        graphXyPlot.setMarkupEnabled(false);
        graphXyPlot.getLayoutManager().remove(graphXyPlot.getLegend());
        graphXyPlot.getLayoutManager().remove(graphXyPlot.getTitle());

        updateRangeAxis(false);

        updateDomainAxis(false);
        graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(xAxisFormat);

        graphXyPlot.getDomainTitle().position(
                0, HorizontalPositioning.ABSOLUTE_FROM_CENTER,
                0, VerticalPositioning.ABSOLUTE_FROM_BOTTOM, Anchor.BOTTOM_MIDDLE);

        graphXyPlot.getGraph().position(
                0, HorizontalPositioning.ABSOLUTE_FROM_RIGHT,
                0, VerticalPositioning.ABSOLUTE_FROM_TOP,
                Anchor.RIGHT_TOP);

        float domainHeight = graphXyPlot.getDomainTitle().getHeightPix(0);
        float rangeWidth = graphXyPlot.getRangeTitle().getWidthPix(0);
        graphXyPlot.getGraph().setSize(new Size(
                domainHeight, SizeMode.FILL,
                rangeWidth, SizeMode.FILL));

        int topPadding = 15;
        graphXyPlot.getGraph().setPaddingTop(topPadding);

        graphXyPlot.getDomainTitle().position(
                0, HorizontalPositioning.ABSOLUTE_FROM_CENTER,
                0, VerticalPositioning.ABSOLUTE_FROM_BOTTOM, Anchor.BOTTOM_MIDDLE);

        graphXyPlot.getGraph().position(
                0, HorizontalPositioning.ABSOLUTE_FROM_RIGHT,
                0, VerticalPositioning.ABSOLUTE_FROM_TOP,
                Anchor.RIGHT_TOP);

        domainHeight = graphXyPlot.getDomainTitle().getHeightPix(0);
        rangeWidth = graphXyPlot.getRangeTitle().getWidthPix(0);
        graphXyPlot.getGraph().setSize(new Size(
                domainHeight, SizeMode.FILL,
                rangeWidth, SizeMode.FILL));

        graphXyPlot.getGraph().setPaddingTop(topPadding);
    }

    private void resetPlot() {
        //graphXyPlot.removeSeries(valuesSeries);
        graphXyPlot.clear();
        graphXyPlot.redraw();
    }

    private boolean doAutoScale() {
        return TraySettings.getDoAutoScale();
    }
}