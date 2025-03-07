package com.draekko.gosense.ui.history;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.draekko.gosense.R;
import com.draekko.gosense.activities.GoSensePagerActivity;
import com.draekko.gosense.database.GoSenseDb;
import com.draekko.gosense.databinding.FragmentHistoryBinding;
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

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Collection;
import java.util.List;

public class HistoryFragmentTest extends Fragment {


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

    // ===========================================================================================
    // ===========================================================================================
    // ===========================================================================================

    private AllDevicesService mAllDevicesService;
    private static HistoryFragmentTest mFragment;
    private Context mContext;
    private GoSenseDb goSenseDb;
    private final Format yAxisFormat_zero = new DecimalFormat("0.0");
    private final Format yAxisFormat_not_zero = new DecimalFormat("0");
    private final Format xAxisFormat = new DecimalFormat("0");
    private FragmentHistoryBinding binding;
    private String connectedAddress;
    private XYPlot graphXyPlot;
    private boolean mHidden;
    private int default_sensor_view = VIEW_TEMPERATURE;
    private int default_time_unit_view = TIME_UNIT_10_MINUTE;
    private int screenDataPoints;

    // ===========================================================================================
    // ===========================================================================================
    // ===========================================================================================

    private GoSensePagerActivity.OnConnnectionStates mHistoryOnConnnectionStates =
            new GoSensePagerActivity.OnConnnectionStates() {
                @Override
                public void onConnected(@NotNull BluetoothPeripheral peripheral) {
                }

                @Override
                public void onDisconnected(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
                }
            };

    private DevicesFragment.OnColorChange mOnColorChange = new DevicesFragment.OnColorChange() {
        @Override
        public void colorChanged(int color) {
            Log.i(TAG, String.format("Change color to %08X", color));
        }
    };

    // ===========================================================================================
    // ===========================================================================================
    // ===========================================================================================

    public static class ECGModel implements XYSeries {

        private final Number[] data;
        private final long delayMs;
        private final int blipInteral;
        private final Thread thread;
        private boolean keepRunning;
        private int latestIndex;

        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        /**
         *
         * @param size Sample size contained within this model
         * @param updateFreqHz Frequency at which new samples are added to the model
         */
        ECGModel(int size, int updateFreqHz) {
            data = new Number[size];
            for(int i = 0; i < data.length; i++) {
                data[i] = 0;
            }

            // translate hz into delay (ms):
            delayMs = 1000 / updateFreqHz;

            // add 7 "blips" into the signal:
            blipInteral = size / 7;

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (keepRunning) {
                            if (latestIndex >= data.length) {
                                latestIndex = 0;
                            }

                            // generate some random data:
                            if (latestIndex % blipInteral == 0) {
                                // insert a "blip" to simulate a heartbeat:
                                data[latestIndex] = (Math.random() * 10) + 3;
                            } else {
                                // insert a random sample:
                                data[latestIndex] = Math.random() * 2;
                            }

                            if(latestIndex < data.length - 1) {
                                // null out the point immediately following i, to disable
                                // connecting i and i+1 with a line:
                                data[latestIndex +1] = null;
                            }

                            if(rendererRef.get() != null) {
                                rendererRef.get().setLatestIndex(latestIndex);
                                Thread.sleep(delayMs);
                            } else {
                                keepRunning = false;
                            }
                            latestIndex++;
                        }
                    } catch (InterruptedException e) {
                        keepRunning = false;
                    }
                }
            });
        }

        void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
            keepRunning = true;
            thread.start();
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return data[index];
        }

        @Override
        public String getTitle() {
            return "Signal";
        }
    }

    public static HistoryFragmentTest getInstance() {
        if (mFragment == null) {
            mFragment = new HistoryFragmentTest();
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
                        //updateRangeAxis(true);
                        //handleDeviceSelection();
                    }
                    if (key.contains(TraySettings.PREF_KEY_AIR_PRESSURE_UNIT)
                            && default_sensor_view == VIEW_AIR_PRESSURE) {
                        //updateRangeAxis(true);
                        //handleDeviceSelection();
                    }
                    if (false) Log.i(TAG, "Preference Changed  = " + key + " " + value);
                }
            }
        });
    }

    Redrawer redrawer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        graphXyPlot = root.findViewById(R.id.history_fragment_plot);
        if (false) {
            graphXyPlot.setRenderMode(Plot.RenderMode.USE_BACKGROUND_THREAD);
        }

        Paint linepaint = new Paint();
        linepaint.setColor(0xFFFFFF00);
        ECGModel ecgSeries = new ECGModel(2000, 200);
        MyFadeFormatter formatter = new MyFadeFormatter(2000, linepaint);
        //LineAndPointFormatter formatter = getDefaultFormatter();
        formatter.setLegendIconEnabled(false);
        graphXyPlot.addSeries(ecgSeries, formatter);
        graphXyPlot.setRangeBoundaries(0, 10, BoundaryMode.FIXED);
        graphXyPlot.setDomainBoundaries(0, 2000, BoundaryMode.FIXED);
        graphXyPlot.setLinesPerRangeLabel(3);
        ecgSeries.start(new WeakReference<>(graphXyPlot.getRenderer(AdvancedLineAndPointRenderer.class)));
        redrawer = new Redrawer(graphXyPlot, 30, true);

        return root;
    }

    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

        private int trailSize;
        private Paint linePaint;

        MyFadeFormatter(int trailSize, Paint linePaint) {
            this.trailSize = trailSize;
            this.linePaint = linePaint;
            setLinePaint(linePaint);
        }

        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            // offset from the latest index:
            int offset;
            if(thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            } else {
                offset =  latestIndex - thisIndex;
            }

            float scale = 255f / trailSize;
            int alpha = (int) (255 - (offset * scale));
            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
            return getLinePaint();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        redrawer.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
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

}
