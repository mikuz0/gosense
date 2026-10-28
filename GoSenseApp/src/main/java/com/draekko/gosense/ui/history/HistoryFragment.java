package com.draekko.gosense.ui.history;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_HPA;
import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_KPA;
import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_MBAR;
import static com.draekko.gosense.misc.TraySettings.AIR_PRESSURE_PSI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.androidplot.ui.Anchor;
import com.androidplot.ui.HorizontalPositioning;
import com.androidplot.ui.Insets;
import com.androidplot.ui.RenderStack;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.VerticalPositioning;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.draekko.gosense.BuildConfig;
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
import com.draekko.gosense.misc.DataBuffer;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.misc.Utils;
import com.draekko.gosense.services.AllDevicesService;
import com.draekko.gosense.ui.devices.DevicesFragment;
import com.draekko.tray.TrayPreferences;
import com.draekko.tray.core.OnTrayPreferenceChangeListener;
import com.draekko.tray.core.TrayItem;
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
    private CustomAdvancedLineAndPointRenderer customLineAndPointRenderer;
    private Redrawer redrawer;
    private boolean lockDrawTemp = false;
    private boolean lockDrawRH = false;
    private boolean lockDrawAP = false;
    private Thread drawing;
    private boolean lockUpdatePlot = false;
    private double minTimeUnit = 0;
    private double maxTimeUnit = 10;

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
            if (true) {
                graphLineAndPointFormatter = getColorFormatter(color);
                graphLineAndPointFormatter.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
                graphLineAndPointFormatter.getLinePaint().setStrokeWidth(Utils.pixelsFromDp(mContext, 2));
            } else {
                customLineAndPointRenderer.getFormatter().setColor(color);
                customLineAndPointRenderer.getFormatter().setStrokeWidth(Utils.pixelsFromDp(mContext, 2));
            }
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
                    connectedAddress = null;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (lockUpdatePlot);

                            while (lockDrawTemp);
                            tempDataBuffer.clear();

                            while (lockDrawRH);
                            rhDataBuffer.clear();

                            while (lockDrawAP);
                            apDataBuffer.clear();

                            try {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (graphXyPlot != null) {
                                                graphXyPlot.clear();
                                            }

                                            clearValueSeries();

                                            default_sensor_view = VIEW_TEMPERATURE;
                                            default_time_unit_view = TIME_UNIT_10_MINUTE;

                                            String text = getString(R.string.empty_bluetooth_device) + " - " +
                                                    getString(R.string.empty_bluetooth_mac_address);
                                            deviceBar.setText(text);
                                        } catch (Exception e) {
                                        }

                                        try {
                                            redrawPlot();
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
                                });
                            } catch (Exception e) {
                            }
                        }
                    }).start();
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        new Thread(preload).start();
    }

    private Runnable preload = new Runnable() {
        @Override
        public void run() {

        }
    };

    public class CustomAdvancedLineAndPointRenderer extends AdvancedLineAndPointRenderer {
        CustomFormatter customFormatter;

        public class CustomFormatter extends AdvancedLineAndPointRenderer.Formatter {

            private Paint linePaint;
            private Paint fillPaint;

            CustomFormatter(Paint linePaint, Paint fillPaint) {
                this.linePaint = linePaint;
                this.fillPaint = fillPaint;
                setLinePaint(linePaint);
            }

            @Override
            public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
                return getLinePaint();
            }

            public Paint getFillPaint() {
                return fillPaint;
            }

            public void setColor(int color) {
                linePaint.setColor(color);
                fillPaint.setColor(color);
            }

            public void setStrokeWidth(float strokeWidth) {
                linePaint.setStrokeWidth(strokeWidth);
            }
        }

        public void createFormatter(Paint linePaint, Paint fillPaint) {
            this.customFormatter = new CustomFormatter(linePaint, fillPaint);
        }

        public void setFormatter(CustomFormatter customFormatter) {
            this.customFormatter = customFormatter;
        }

        public CustomFormatter getFormatter() {
            return this.customFormatter;
        }

        public CustomAdvancedLineAndPointRenderer(XYPlot plot) {
            super(plot);
        }

        @Override
        protected void onRender(Canvas canvas, RectF plotArea, XYSeries series,
                                AdvancedLineAndPointRenderer.Formatter formatter, RenderStack stack) {
            PointF thisPoint;
            PointF lastPoint = null;
            RectF rect;
            for (int i = 0; i < valuesSeries.size(); i++) {
                Number y = valuesSeries.getY(i);
                Number x = valuesSeries.getX(i);

                if (y != null && x != null) {
                    thisPoint = getPlot().getBounds()
                            .transformScreen(x, y, plotArea);
                } else {
                    thisPoint = null;
                }

                // don't need to do any of this if the line isnt going to be drawn:
                if(customFormatter != null && customFormatter.getLinePaint() != null) {
                    if (thisPoint != null && lastPoint != null) {
                        if (true) {
                            rect = new RectF(lastPoint.x, lastPoint.y, thisPoint.x, 0.0f);
                            if (customFormatter != null && customFormatter.getFillPaint() != null) {
                                canvas.drawRect(rect, customFormatter.getFillPaint());
                            }
                        }
                        if(customFormatter != null && customFormatter.getLinePaint() != null) {
                            canvas.drawLine(lastPoint.x, lastPoint.y, thisPoint.x, thisPoint.y, customFormatter.getLinePaint(i, latestIndex, series.size()));
                        }
                    }
                }
                lastPoint = thisPoint;
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        graphXyPlot = root.findViewById(R.id.history_fragment_plot);
        boolean bgRender = false;
        boolean hwLayerView = true;
        if (bgRender) {
            graphXyPlot.setRenderMode(Plot.RenderMode.USE_BACKGROUND_THREAD);
        } else {
            graphXyPlot.setRenderMode(Plot.RenderMode.USE_MAIN_THREAD);
        }
        if (hwLayerView) {
            graphXyPlot.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            graphXyPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        //graphXyPlot.addSeries(ecgSeries, formatter);

        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(0xFFFF00FF);
        linePaint.setStrokeWidth(Utils.pixelsFromDp(mContext, 2));

        Paint fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xFFFF00FF);
        fillPaint.setAlpha(45);

        customLineAndPointRenderer = new CustomAdvancedLineAndPointRenderer(graphXyPlot);
        customLineAndPointRenderer.createFormatter(linePaint, fillPaint);

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

        if (bgRender) {
            float framerate = 30.0f;
            redrawer = new Redrawer(graphXyPlot, framerate, true);
        }

        redrawPlot();

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
    public void onStop() {
        super.onStop();
        if (redrawer != null) {
            redrawer.finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setOnHidden(false);
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

        fetchUpdates();
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
        setOnHidden(true);
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
        clearValueSeries();

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
                if (default_sensor_view == oo + 1) {
                    return;
                }
                default_sensor_view = oo + 1;
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
        handleDeviceSelection();
        updateRangeAxis(true);
        if (lockUpdatePlot) {
            if (!drawing.isInterrupted()) {
                drawing.interrupt();
                lockUpdatePlot = false;
            }
        }
        clearValueSeries();
    }

    private void handleTimeUnitClick(View view) {
        clearValueSeries();
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
                if (default_time_unit_view == oo + 1) {
                    return;
                }
                default_time_unit_view = oo + 1;
                ooIndexT = oo;
                updateDomainAxis(true);
            } else {
                sensor_time_unit_txt_textview[oo].setBackground(hideOutline);
                sensor_time_unit_txt_textview[oo].setTypeface(null, fontStyleOff);
                sensor_time_unit_txt_textview[oo].setTextSize(COMPLEX_UNIT_SP, textSizeOff);
                sensor_time_unit_txt_textview[oo].setAlpha(alphaOff);
            }
        }
        handleDeviceSelection();
        updateDomainAxis(true);
        if (lockUpdatePlot) {
            if (!drawing.isInterrupted()) {
                drawing.interrupt();
                lockUpdatePlot = false;
            }
        }
        //updatePlot();
        clearValueSeries();
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
            }
        }
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

    private ArrayList<TemperatureData> tData;
    private ArrayList<RelativeHumidityData> rhData;
    private ArrayList<AirPressureData> apData;
    private void initData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (tData != null && tData.size() > 0) {
                        tData.clear();
                    }
                    tData = goSenseDb.getTemperatureAddressAndRange(connectedAddress, MAX_TIME, currentTime);
                    tempDataBuffer = new TemperatureDataBuffer(MAX_TIME, (ArrayList<TemperatureData>) tData);
                } catch (Exception e) {
                }
                try {
                    if (rhData != null && rhData.size() > 0) {
                        rhData.clear();
                    }
                    rhData = goSenseDb.getRelativeHumidityAddressAndRange(connectedAddress, MAX_TIME, currentTime);
                    rhDataBuffer = new RelativeHumidityDataBuffer(MAX_TIME, (ArrayList<RelativeHumidityData>) rhData);
                } catch (Exception e) {
                }
                try {
                    if (apData != null && apData.size() > 0) {
                        apData.clear();
                    }
                    apData = goSenseDb.getAirPressureAddressAndRange(MAX_TIME, currentTime);
                    apDataBuffer = new AirPressureDataBuffer(MAX_TIME, (ArrayList<AirPressureData>) apData);
                } catch (Exception e) {
                }
                try {
                    if (mAllDevicesService != null) {
                        mAllDevicesService.registerOnAirPressureSensorChange(mOnAirPressureSensorChange);
                    }
                } catch (Exception e) {
                }
                try {
                    if (mAllDevicesService != null) {
                        mAllDevicesService.registerOnTemperatureSensorChange(mOnTemperatureSensorChange);
                    }
                } catch (Exception e) {
                }
                try {
                    if (mAllDevicesService != null) {
                        mAllDevicesService.registerOnRelativeHumiditySensorChange(mOnRelativeHumiditySensorChange);
                    }
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void updateDomainAxis(boolean invalidate) {
        String domainLabel = getString(R.string.elapsed_time);
        minTimeUnit = 0;
        maxTimeUnit = 10;
        double valOffset = 10;
        switch (default_time_unit_view) {
            case TIME_UNIT_10_MINUTE:
                minTimeUnit = 0;
                maxTimeUnit = 10;
                domainLabel += " " + getString(R.string.graph_unit_minutes);
                valOffset = 1;
                break;

            case TIME_UNIT_60_MINUTE:
                minTimeUnit = 0;
                maxTimeUnit = 60;
                domainLabel += " " + getString(R.string.graph_unit_minutes);
                valOffset = 10;
                break;

            case TIME_UNIT_3_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 3;
                domainLabel += " " + getString(R.string.graph_unit_hours);
                valOffset = 1;
                break;

            case TIME_UNIT_6_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 6;
                domainLabel += " " + getString(R.string.graph_unit_hours);
                valOffset = 1;
                break;

            case TIME_UNIT_12_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 12;
                domainLabel += " " + getString(R.string.graph_unit_hours);
                valOffset = 1;
                break;

            case TIME_UNIT_24_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 24;
                domainLabel += " " + getString(R.string.graph_unit_hours);
                valOffset = 2;
                break;

            case TIME_UNIT_48_HOURS:
                minTimeUnit = 0;
                maxTimeUnit = 48;
                domainLabel += " " + getString(R.string.graph_unit_hours);
                valOffset = 4;
                break;

            case TIME_UNIT_7_DAYS:
                minTimeUnit = 0;
                maxTimeUnit = 7;
                domainLabel += " " + getString(R.string.graph_unit_days);
                valOffset = 1;
                break;

            case TIME_UNIT_14_DAYS:
                minTimeUnit = 0;
                maxTimeUnit = 14;
                domainLabel += " " + getString(R.string.graph_unit_days);
                valOffset = 1;
                break;
        }

        graphXyPlot.getGraph().setMarginTop(Utils.pixelsFromDp(mContext, 5));
        graphXyPlot.getRangeTitle().setMarginLeft(Utils.pixelsFromDp(mContext, 4));
        graphXyPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, valOffset);
        graphXyPlot.setDomainBoundaries(maxTimeUnit, minTimeUnit, BoundaryMode.FIXED);
        graphXyPlot.setDomainLabel(domainLabel.toUpperCase(Locale.US));

        if (invalidate) {
            redrawPlot();
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
            redrawPlot();
        }
    }

    private class DataXYSeries implements XYSeries {
        private ArrayList<Double> xVals = new ArrayList<>();
        private ArrayList<Double> yVals = new ArrayList<>();
        private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

        public DataXYSeries() {
            clear();
        }

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
        public int size() {
            return xVals.size();
        }

        @Override
        public Number getX(int i) {
            if (size() == 0) {
                return -9999;
            }
            if (i > size() - 1) {
                i--;
            }
            try {
                return xVals.get(i);
            } catch (Exception e) {
            }
            return -9999;
        }

        @Override
        public Number getY(int i) {
            if (size() == 0) {
                return -9999;
            }
            if (i > size() - 1) {
                i--;
            }
            try {
                return yVals.get(i);
            } catch (Exception e) {
            }
            return -9999;
        }

        @Override
        public String getTitle() {
            return "";
        }

        public void add(double valueX, double valueY) {
            xVals.add(valueX);
            yVals.add(valueY);
        }
    }

    private boolean averageGraph = false;
    private void updatePlot(){
        averageGraph  = TraySettings.getUseAverageGraph();
        if (!mHidden && connectedAddress != null) {
            currentTime = System.currentTimeMillis();

            if (graphLineAndPointFormatter == null) {
                graphLineAndPointFormatter = getDeviceFormatter();
                graphLineAndPointFormatter.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            }

            if (true) {
                if (drawing != null && !drawing.isInterrupted()) {
                    drawing.interrupt();
                    drawing = null;
                }
                drawing = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lockUpdatePlot = true;

                        //clearValueSeries();

                        switch (default_sensor_view) {
                            case VIEW_TEMPERATURE: {
                                if(lockDrawTemp) {
                                    return;
                                }
                                try {
                                    handleTemperature();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case VIEW_RELATIVE_HUMIDITY: {
                                if(lockDrawRH) {
                                    return;
                                }
                                try {
                                    handleRelativeHumidity();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case VIEW_AIR_PRESSURE: {
                                if(lockDrawAP) {
                                    return;
                                }
                                try {
                                    handleAirPressure();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
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

    private void handleRelativeHumidity() {
        lockDrawRH = true;

        double minRH = 999999;
        double maxRH = -999999;

        adjustGraph();

        int tSize = rhDataBuffer.getDataBuffer().size();
        int startIndex = 0;

        if (valuesSeries == null) {
            valuesSeries = new DataXYSeries();
            graphXyPlot.addSeries(valuesSeries, graphLineAndPointFormatter);
        }

        if (BuildConfig.DEBUG) Log.i(TAG, "relhum values size " + tSize);
        if (tSize > 0) {
            for (int tt = 0; tt < tSize; tt++) {
                if (rhDataBuffer.getDataBuffer().get(tt).getTimestamp() >= goBackTime) {
                    startIndex = tt;
                    break;
                }
            }

            if (BuildConfig.DEBUG) Log.i(TAG, "relhum X/Y startIndex = " + startIndex);

            double relativeHumidity = 0;
            double graphTime;
            int maxOffset;
            int indices;
            float offset;

            valuesSeries.clear();

            indices = tSize - startIndex;

            int maxIt = 120;

            if (indices <= maxIt) {
                offset = 1;
            } else {
                maxOffset = maxIt;
                if (indices > maxOffset) {
                    offset = indices / maxOffset;
                } else {
                    offset = 1;
                }
            }

            if (indices <= maxIt) {
                for (int tt = startIndex; tt < tSize; tt++) {
                    relativeHumidity = rhDataBuffer.getDataBuffer().get(tt).getRelativeHumidity();
                    graphTime = (double)(currentTime - rhDataBuffer.getDataBuffer().get(tt).getTimestamp()) / (double)graphUnit;
                    valuesSeries.add(graphTime, relativeHumidity);
                    if (doAutoScale()) {
                        if (relativeHumidity != -9999 && relativeHumidity < minRH) {
                            minRH = relativeHumidity;
                        }
                        if (relativeHumidity != -9999 && relativeHumidity > maxRH) {
                            maxRH = relativeHumidity;
                        }
                    }
                }
            } else {
                int lastTT1 = 0;
                for (int tto = startIndex; tto < rhDataBuffer.getDataBuffer().size(); tto+=(int)(offset+0.5f)) {
                    double lRelHum = rhDataBuffer.getDataBuffer().get(tto).getRelativeHumidity();
                    graphTime = (double)(currentTime - rhDataBuffer.getDataBuffer().get(tto).getTimestamp()) / (double)graphUnit;

                    if (averageGraph) {
                        if (tto > 0) {
                            int ttDelta = Math.abs(tto - lastTT1);
                            if (ttDelta > 1) {
                                double lt = 0.0;
                                for (int tt2 = lastTT1; tt2 < tto; tt2++) {
                                    double t = rhDataBuffer.getDataBuffer().get(tt2).getRelativeHumidity();
                                    if (t > 0) {
                                        lt += t;
                                    }
                                }
                                lt += lRelHum;
                                lRelHum = lt / ttDelta;
                            }
                        }
                    }

                    if (doAutoScale()) {
                        if (lRelHum >= 0 && lRelHum < minRH) {
                            minRH = lRelHum;
                        }
                        if (lRelHum >= 0 && lRelHum > maxRH) {
                            maxRH = lRelHum;
                        }
                    }

                    if (lastTT1 != 0) {
                        long start = rhDataBuffer.getDataBuffer().get(lastTT1).getTimestamp();
                        long end = rhDataBuffer.getDataBuffer().get(tto).getTimestamp();
                        ArrayList<RelativeHumidityData> list = goSenseDb.getRelativeHumidityRangeNegative(start, end);
                        if (list != null && list.size() > 0){
                            if (list.size() == 1) {
                                graphTime = (double)(currentTime - list.get(0).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                            } else {
                                graphTime = (double)(currentTime - list.get(0).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                                graphTime = (double)(currentTime - list.get(list.size() - 1).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                            }
                        }
                    }

                    valuesSeries.add(graphTime, lRelHum);
                    lastTT1 = tto;
                }
            }

            if (BuildConfig.DEBUG) Log.i(TAG, "relhum X/Y values size " + valuesSeries.size());

            if (doAutoScale()) {
                float insetLeft = Utils.pixelsFromDp(mContext, 25);
                String rangeLabel = getString(R.string.relative_humidity) + "  (%)";
                double minRangeUnits = 0;
                double maxRangeUnits = 100;
                double rangeSteps = 10;
                minRangeUnits = (int)(minRH - 1.0);
                maxRangeUnits = (int)(maxRH + 1.0);
                graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                graphXyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, (maxRangeUnits - minRangeUnits) / rangeSteps);
                Insets gridInsets = graphXyPlot.getGraph().getGridInsets();
                gridInsets.setLeft(insetLeft);
                graphXyPlot.getGraph().setGridInsets(gridInsets);
                graphXyPlot.setRangeLabel(rangeLabel.toUpperCase(Locale.US));
                graphXyPlot.setRangeBoundaries(minRangeUnits, maxRangeUnits, BoundaryMode.FIXED);
            }
        }

        redrawPlot();

        lockDrawRH = false;
    }

    private void handleAirPressure() {
        lockDrawAP = true;

        double minAP = 999999;
        double maxAP = -999999;

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

        adjustGraph();

        int tSize = apDataBuffer.getDataBuffer().size();
        int startIndex = 0;

        if (valuesSeries == null) {
            valuesSeries = new DataXYSeries();
            graphXyPlot.addSeries(valuesSeries, graphLineAndPointFormatter);
        }

        if (BuildConfig.DEBUG) Log.i(TAG, "airpress values size " + tSize);
        if (tSize > 0) {
            for (int tt = 0; tt < tSize; tt++) {
                if (apDataBuffer.getDataBuffer().get(tt).getTimestamp() >= goBackTime) {
                    startIndex = tt;
                    break;
                }
            }

            if (BuildConfig.DEBUG) Log.i(TAG, "airpress X/Y startIndex = " + startIndex);

            double airpressure = 0;
            double graphTime;
            int maxOffset;
            int indices;
            float offset;

            valuesSeries.clear();

            indices = tSize - startIndex;

            int maxIt = 120;

            if (indices <= maxIt) {
                offset = 1;
            } else {
                maxOffset = maxIt;
                if (indices > maxOffset) {
                    offset = indices / maxOffset;
                } else {
                    offset = 1;
                }
            }

            if (indices <= maxIt) {
                for (int tt = startIndex; tt < tSize; tt++) {
                    airpressure = apDataBuffer.getDataBuffer().get(tt).getAirPressure();

                    if (airpressure > 0 && apUnit == AIR_PRESSURE_KPA) {
                        airpressure /= 10.0;
                    }
                    if (airpressure > 0 && apUnit == AIR_PRESSURE_PSI) {
                        airpressure *= Utils.PSI_VALUE_ONE_MILLIBAR;
                    }

                    graphTime = (double)(currentTime - apDataBuffer.getDataBuffer().get(tt).getTimestamp()) / (double)graphUnit;
                    valuesSeries.add(graphTime, airpressure);
                    if (doAutoScale()) {
                        if (airpressure > 0 && airpressure < minAP) {
                            minAP = airpressure;
                        }
                        if (airpressure  > 0 -9999 && airpressure > maxAP) {
                            maxAP = airpressure;
                        }
                    }
                }
            } else {
                int lastTT1 = 0;
                for (int tto = startIndex; tto < apDataBuffer.getDataBuffer().size(); tto+=(int)(offset+0.5f)) {
                    double lAirPres = apDataBuffer.getDataBuffer().get(tto).getAirPressure();

                    if (lAirPres > 0 && apUnit == AIR_PRESSURE_KPA) {
                        lAirPres /= 10.0;
                    }
                    if (lAirPres > 0 && apUnit == AIR_PRESSURE_PSI) {
                        lAirPres *= Utils.PSI_VALUE_ONE_MILLIBAR;
                    }

                    graphTime = (double)(currentTime - apDataBuffer.getDataBuffer().get(tto).getTimestamp()) / (double)graphUnit;

                    if (averageGraph) {
                        if (tto > 0) {
                            int ttDelta = Math.abs(tto - lastTT1);
                            if (ttDelta > 1) {
                                double lt = 0.0;
                                for (int tt2 = lastTT1; tt2 < tto; tt2++) {
                                    double t = apDataBuffer.getDataBuffer().get(tt2).getAirPressure();
                                    if (t > 0 && apUnit == AIR_PRESSURE_KPA) {
                                        t /= 10.0;
                                    }
                                    if (t > 0 && apUnit == AIR_PRESSURE_PSI) {
                                        t *= Utils.PSI_VALUE_ONE_MILLIBAR;
                                    }
                                    if (t  > 0) {
                                        lt += t;
                                    }
                                }
                                lt += lAirPres;
                                lAirPres = lt / ttDelta;
                            }
                        }
                    }

                    if (doAutoScale()) {
                        if (lAirPres  > 0 && lAirPres < minAP) {
                            minAP = lAirPres;
                        }
                        if (lAirPres  > 0 && lAirPres > maxAP) {
                            maxAP = lAirPres;
                        }
                    }

                    if (lastTT1 != 0) {
                        long start = apDataBuffer.getDataBuffer().get(lastTT1).getTimestamp();
                        long end = apDataBuffer.getDataBuffer().get(tto).getTimestamp();
                        ArrayList<AirPressureData> list = goSenseDb.getAirPressureRangeNegative(start, end);
                        if (list != null && list.size() > 0){
                            if (list.size() == 1) {
                                graphTime = (double)(currentTime - list.get(0).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                            } else {
                                graphTime = (double)(currentTime - list.get(0).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                                graphTime = (double)(currentTime - list.get(list.size() - 1).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                            }
                        }
                    }

                    valuesSeries.add(graphTime, lAirPres);
                    lastTT1 = tto;
                }
            }

            if (BuildConfig.DEBUG) Log.i(TAG, "airpress X/Y values size " + valuesSeries.size());

            if (doAutoScale()) {
                double offsetAp = 5.0;
                float insetLeft = Utils.pixelsFromDp(mContext, 25);
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
                String rangeLabel = getString(R.string.air_pressure) + "  (" + unit  + ")";
                double minRangeUnits = 0;
                double maxRangeUnits = 100;
                double rangeSteps = 10;
                minRangeUnits = (int)(minAP - offsetAp);
                maxRangeUnits = (int)(maxAP + offsetAp);
                graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                graphXyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, (maxRangeUnits - minRangeUnits) / rangeSteps);
                Insets gridInsets = graphXyPlot.getGraph().getGridInsets();
                gridInsets.setLeft(insetLeft);
                graphXyPlot.getGraph().setGridInsets(gridInsets);
                graphXyPlot.setRangeLabel(rangeLabel.toUpperCase(Locale.US));
                graphXyPlot.setRangeBoundaries(minRangeUnits, maxRangeUnits, BoundaryMode.FIXED);
            }
        }

        redrawPlot();

        lockDrawAP = false;
    }

    private void handleTemperature() {
        lockDrawTemp = true;

        boolean tempF = TraySettings.isTemperatureUnitFahrenheit();

        double minT = 999999;
        double maxT = -999999;

        adjustGraph();

        int tSize = tempDataBuffer.getDataBuffer().size();
        int startIndex = 0;

        if (valuesSeries == null) {
            valuesSeries = new DataXYSeries();
            graphXyPlot.addSeries(valuesSeries, graphLineAndPointFormatter);
        }

        if (BuildConfig.DEBUG) Log.i(TAG, "temperatureData values size " + tSize);
        if (tSize > 0) {
            for (int tt = 0; tt < tSize; tt++) {
                if (tempDataBuffer.getDataBuffer().get(tt).getTimestamp() >= goBackTime) {
                    startIndex = tt;
                    break;
                }
            }

            if (BuildConfig.DEBUG) Log.i(TAG, "temp X/Y startIndex = " + startIndex);

            double temperature = 0;
            double graphTime;
            int maxOffset;
            int indices;
            float offset;

            valuesSeries.clear();

            indices = tSize - startIndex;

            int maxIt = 120;

            if (indices <= maxIt) {
                offset = 1;
            } else {
                maxOffset = maxIt;
                if (indices > maxOffset) {
                    offset = indices / maxOffset;
                } else {
                    offset = 1;
                }
            }

            if (indices <= maxIt) {
                for (int tt = startIndex; tt < tSize; tt++) {
                    temperature = tempDataBuffer.getDataBuffer().get(tt).getTemperature();
                    graphTime = (double)(currentTime - tempDataBuffer.getDataBuffer().get(tt).getTimestamp()) / (double)graphUnit;
                    valuesSeries.add(graphTime, temperature);
                    if (doAutoScale()) {
                        if (temperature != -9999 && temperature < minT) {
                            minT = temperature;
                        }
                        if (temperature != -9999 && temperature > maxT) {
                            maxT = temperature;
                        }
                    }
                }
            } else {
                int lastTT1 = 0;
                for (int tto = startIndex; tto < tempDataBuffer.getDataBuffer().size(); tto+=(int)(offset+0.5f)) {
                    double lTemp = tempDataBuffer.getDataBuffer().get(tto).getTemperature();
                    graphTime = (double)(currentTime - tempDataBuffer.getDataBuffer().get(tto).getTimestamp()) / (double)graphUnit;

                    if (averageGraph) {
                        if (tto > 0) {
                            int ttDelta = Math.abs(tto - lastTT1);
                            if (ttDelta > 1) {
                                double lt = 0.0;
                                for (int tt2 = lastTT1; tt2 < tto; tt2++) {
                                    double t = tempDataBuffer.getDataBuffer().get(tt2).getTemperature();
                                    if (t != -9999) {
                                        lt += t;
                                    }
                                }
                                lt += lTemp;
                                lTemp = lt / ttDelta;
                            }
                        }
                    }

                    if (doAutoScale()) {
                        if (lTemp != -9999 && lTemp < minT) {
                            minT = lTemp;
                        }
                        if (lTemp != -9999 && lTemp > maxT) {
                            maxT = lTemp;
                        }
                    }

                    if (lastTT1 != 0) {
                        long start = tempDataBuffer.getDataBuffer().get(lastTT1).getTimestamp();
                        long end = tempDataBuffer.getDataBuffer().get(tto).getTimestamp();
                        ArrayList<TemperatureData> list = goSenseDb.getTemperatureRangeNegative(start, end);
                        if (list != null && list.size() > 0){
                            if (list.size() == 1) {
                                graphTime = (double)(currentTime - list.get(0).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                            } else {
                                graphTime = (double)(currentTime - list.get(0).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                                graphTime = (double)(currentTime - list.get(list.size() - 1).getTimestamp()) / (double)graphUnit;
                                valuesSeries.add(graphTime, -9999);
                            }
                        }
                    }

                    valuesSeries.add(graphTime, lTemp);
                    lastTT1 = tto;
                }
            }

            if (BuildConfig.DEBUG) Log.i(TAG, "temp X/Y values size " + valuesSeries.size());

            if (doAutoScale()) {
                String dispUnit = getString(R.string.unit_celsius);
                if (tempF) {
                    dispUnit = getString(R.string.unit_fahrenheit);
                }
                String rangeLabel = getString(R.string.temperature) + "  (" + dispUnit + ")";
                double minRangeUnits = 0;
                double maxRangeUnits = 100;
                float insetLeft = Utils.pixelsFromDp(mContext, 31);
                double rangeSteps = 10;
                minRangeUnits = (int)(minT - 1.0);
                maxRangeUnits = (int)(maxT + 1.0);
                if (TraySettings.isTemperatureUnitFahrenheit()) {
                    graphXyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(yAxisFormat_zero);
                    insetLeft = Utils.pixelsFromDp(mContext, 25);
                } else {
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
        }

        redrawPlot();

        lockDrawTemp = false;
    }

    private void adjustGraph() {
        currentTime = System.currentTimeMillis();
        switch (default_time_unit_view) {
            case TIME_UNIT_10_MINUTE:
                graphUnit = SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (11 * graphUnit);
                break;

            case TIME_UNIT_60_MINUTE:
                graphUnit = SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (SIXTY_MINUTES * graphUnit);
                break;

            case TIME_UNIT_3_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (3 * graphUnit);
                break;

            case TIME_UNIT_6_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (6 * graphUnit);
                break;

            case TIME_UNIT_12_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (12 * graphUnit);
                break;

            case TIME_UNIT_24_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (DAY_HOURS * graphUnit);
                break;

            case TIME_UNIT_48_HOURS:
                graphUnit = SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (2 * DAY_HOURS * graphUnit);
                break;

            case TIME_UNIT_7_DAYS:
                graphUnit = DAY_HOURS * SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (7 * graphUnit);
                break;

            case TIME_UNIT_14_DAYS:
                graphUnit = DAY_HOURS * SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_MILLIS;
                goBackTime = currentTime - (14 * graphUnit);
                break;
        }
    }

    private LineAndPointFormatter getColorFormatter(int lineColor) {
        final Paint fillColor = new Paint();
        fillColor.setColor(lineColor);
        fillColor.setAlpha(45);
        return new LineAndPointFormatter(lineColor, null, fillColor.getColor(), null);
    }

    private LineAndPointFormatter getDeviceFormatter() {
        LineAndPointFormatter lineAndPointFormatter = null;
        if (connectedAddress == null) {
            lineAndPointFormatter = getColorFormatter(Utils.DEFAULT_COLOR);
        } else {
            int devColor = Utils.getGraphColorFromPairedDb(mContext, connectedAddress);
            lineAndPointFormatter = getColorFormatter(devColor);
        }
        lineAndPointFormatter.getLinePaint().setStrokeWidth(Utils.pixelsFromDp(mContext, 2));
        return lineAndPointFormatter;
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

    private void redrawPlot() {
        try {
            //graphXyPlot.redraw();
            graphXyPlot.invalidate();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean doAutoScale() {
        return TraySettings.getDoAutoScale();
    }

    public void clearValueSeries() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (lockUpdatePlot);
                if (valuesSeries != null) {
                    valuesSeries.clear();
                }
                redrawPlot();
            }
        }).start();
    }

    public void fetchUpdates() {
        if (connectedAddress != null) {
            updatePlot();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(lockUpdatePlot);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchUpdates();
                    }
                }, graphUpdateInterval * ONE_SECOND_MILLIS);
            }
        }).start();
    }
}