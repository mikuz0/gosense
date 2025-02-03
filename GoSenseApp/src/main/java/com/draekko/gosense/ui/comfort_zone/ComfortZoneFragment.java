package com.draekko.gosense.ui.comfort_zone;

import static com.draekko.gosense.misc.Utils.DEFAULT_COLOR;
import static com.draekko.gosense.misc.Utils.pixelsFromDp;
import static com.draekko.gosense.misc.XmlFloatExtractor.getFloatValueFromId;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.draekko.gosense.R;
import com.draekko.gosense.activities.GoSensePagerActivity;
import com.draekko.gosense.data.RemoteRelativeHumidityMeasurement;
import com.draekko.gosense.data.RemoteTemperatureMeasurement;
import com.draekko.gosense.data.SensorDevice;
import com.draekko.gosense.databinding.FragmentComfortZoneBinding;
import com.draekko.gosense.misc.Converter;
import com.draekko.gosense.misc.TraySettings;
import com.draekko.gosense.misc.Utils;
import com.draekko.gosense.services.AllDevicesService;
import com.draekko.gosense.services.AllServicesDeviceDataHandler;
import com.draekko.gosense.ui.devices.DevicesFragment;
import com.draekko.gosense.views.XyPlotView;
import com.draekko.gosense.views.XyPoint;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.HciStatus;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComfortZoneFragment extends Fragment {

    private static final String TAG = "ComfortZoneFragment";

    private boolean firstTempUpdateDone = false;
    private boolean firstRHUpdateDone = false;

    private AllDevicesService mAllDevicesService;
    
    private boolean isConnected = false;

    private int visibility = View.INVISIBLE;

    private GoSensePagerActivity.OnConnnectionStates mCfOnConnnectionStates =
            new GoSensePagerActivity.OnConnnectionStates() {
        @Override
        public void onConnected(@NotNull BluetoothPeripheral peripheral) {
            isConnected = true;

            mAddress = peripheral.getAddress();
            mColor = Utils.getGraphColorFromPairedDb(mContext, peripheral.getAddress());
            mName = Utils.getAliasNameFromPairedDb(mContext, peripheral.getAddress());

            visibility = View.INVISIBLE;

            mSensorNameTextView.setText(mName);
            mSensorAddressTextView.setText(mAddress);
        }

        @Override
        public void onDisconnected(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            isConnected = false;

            mColor = DEFAULT_COLOR;
            mAddress = Utils.DEFAULT_EMPTY_ADDRESS;
            mName = getString(R.string.empty_bluetooth_device);

            visibility = View.INVISIBLE;

            mSensorNameTextView.setText(getString(R.string.empty_bluetooth_device));
            mSensorAddressTextView.setText(Utils.DEFAULT_EMPTY_ADDRESS);
        }
    };

    private final BroadcastReceiver remoteTemperatureDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothPeripheral peripheral = getPeripheral(intent.getStringExtra(AllServicesDeviceDataHandler.MEASUREMENT_EXTRA_PERIPHERAL));
            RemoteTemperatureMeasurement measurement = (RemoteTemperatureMeasurement) intent.getSerializableExtra(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_TEMPERATURE_EXTRA);
            if (measurement == null) {
                return;
            }

            float temp = mTemperature = measurement.temperatureValue;
            if (false) Log.i(TAG, "Temperature = " + mTemperature);

            int tempUnit = R.string.unit_celsius;
            if (TraySettings.isTemperatureUnitFahrenheit()) {
                tempUnit = R.string.unit_fahrenheit;
            }

            float temperature = temp;
            if (TraySettings.isTemperatureUnitFahrenheit()) {
                temperature = Converter.convertToF(temp);
            }
            mSensorAmbientTemperatureTextView.setText(decimalFormat.format(temperature) + getString(tempUnit));

            firstTempUpdateDone = true;
        }
    };

    private final BroadcastReceiver remoteHumidityDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothPeripheral peripheral = getPeripheral(intent.getStringExtra(AllServicesDeviceDataHandler.MEASUREMENT_EXTRA_PERIPHERAL));
            RemoteRelativeHumidityMeasurement measurement = (RemoteRelativeHumidityMeasurement) intent.getSerializableExtra(AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY_EXTRA);
            if (measurement == null) {
                return;
            }

            mHumidity = measurement.humidityValue;
            if (false) Log.i(TAG, "Humidity = " + mHumidity);
            mSensorRelativeHumidity.setText(decimalFormat.format(mHumidity) + "%");

            firstRHUpdateDone = true;
        }
    };

    private FragmentComfortZoneBinding binding;
    private float mTemperature = -9999.0f;
    private float mHumidity = -9999.0f;
    private String mAddress = Utils.DEFAULT_EMPTY_ADDRESS;
    private String mName = Utils.DEFAULT_EMPTY_NAME;
    private int mColor = Utils.DEFAULT_COLOR;
    private DecimalFormat decimalFormat = new DecimalFormat("###0.0");
    private DecimalFormat decimalPsiFormat = new DecimalFormat("###0.0##");
    private Context mContext;
    private int GRAPH_MIN_Y_VALUE;
    private int GRAPH_MAX_Y_VALUE;
    private int GRAPH_Y_GRID_SIZE;
    private int GRAPH_MIN_X_VALUE;
    private int GRAPH_MAX_X_VALUE;
    private int GRAPH_X_GRID_SIZE_CELSIUS;
    private String GRAPH_X_LABEL_CELSIUS;
    private String GRAPH_X_LABEL_FAHRENHEIT;
    private XyPlotView mPlotView;
    private TextView mTextViewLeft;
    private TextView mTextViewTop;
    private TextView mTextViewRight;
    private TextView mTextViewBottom;
    private TextView mSensorNameTextView;
    private TextView mSensorAddressTextView;
    private TextView mSensorAmbientTemperatureTextView;
    private TextView mSensorRelativeHumidity;
    private String DEFAULT_SENSOR_NAME;
    private String EMPTY_TEMPERATURE_STRING;
    private String EMPTY_RELATIVE_HUMIDITY_STRING;
    private ViewGroup mParentFrame;
    private String PERCENTAGE_CHARACTER;
    private int GRAPH_X_GRID_SIZE_FAHRENHEIT;
    private boolean mIsFahrenheit;

    private final Map<String, XyPoint> mActiveSensorViews = Collections.synchronizedMap(new LinkedHashMap<String, XyPoint>());


    private DevicesFragment.OnColorChange mOnColorChange = new DevicesFragment.OnColorChange() {
        @Override
        public void colorChanged(int color) {
            Log.i(TAG, String.format("Change color to %08X", color));
            mColor = color;
            if (mAddress != null) {
                createNewSensorViewFor(mAddress, mColor);
            }
        }
    };

    private static ComfortZoneFragment mFragment;

    public static ComfortZoneFragment getInstance() {
        if (mFragment == null) {
            mFragment = new ComfortZoneFragment();
        }
        return mFragment;
    }

    public void setOnHidden(boolean hidden) {
        if (!hidden) {
            if (mAllDevicesService != null) {
                GoSensePagerActivity.registerOnConnnectionStates(mCfOnConnnectionStates);
            }
        }
        if (false) Log.i(TAG, "setOnHidden: " + hidden);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    private BluetoothPeripheral getPeripheral(String peripheralAddress) {
        BluetoothCentralManager central = mAllDevicesService.getCentral();
        return central.getPeripheral(peripheralAddress);
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
            } else {
                GoSensePagerActivity.registerOnConnnectionStates(mCfOnConnnectionStates);
            }
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAllDevicesService = GoSensePagerActivity.getAllDevicesService();
        if (mAllDevicesService == null) {
            startAllDevicesServiceWatcher();
        } else {
            GoSensePagerActivity.registerOnConnnectionStates(mCfOnConnnectionStates);
        }

        GoSensePagerActivity.registerOnColorChange(mOnColorChange);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Context.RECEIVER_NOT_EXPORTED : 0 ;
            ContextCompat.registerReceiver(mContext, remoteTemperatureDataReceiver, new IntentFilter( AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_TEMPERATURE ), flags);
            ContextCompat.registerReceiver(mContext, remoteHumidityDataReceiver, new IntentFilter( AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY ), flags);
        } else {
            mContext.registerReceiver(remoteTemperatureDataReceiver, new IntentFilter( AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_TEMPERATURE ));
            mContext.registerReceiver(remoteHumidityDataReceiver, new IntentFilter( AllServicesDeviceDataHandler.MEASUREMENT_REMOTE_RELATIVE_HUMIDITY ));
        }

        mActiveSensorViews.clear();

        GRAPH_MIN_Y_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_min_y_axis_value);
        GRAPH_MAX_Y_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_max_y_axis_value);
        GRAPH_Y_GRID_SIZE = mContext.getResources().getInteger(R.integer.comfort_zone_y_axis_grid_size);
        GRAPH_MIN_X_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_min_x_axis_value);
        GRAPH_MAX_X_VALUE = mContext.getResources().getInteger(R.integer.comfort_zone_max_x_axis_value);
        GRAPH_X_GRID_SIZE_CELSIUS = mContext.getResources().getInteger(R.integer.comfort_zone_x_axis_grid_size_celsius);
        GRAPH_X_GRID_SIZE_FAHRENHEIT = mContext.getResources().getInteger(R.integer.comfort_zone_x_axis_grid_size_fahrenheit);
        GRAPH_X_LABEL_CELSIUS = mContext.getResources().getString(R.string.graph_label_temperature_celsius);
        GRAPH_X_LABEL_FAHRENHEIT = mContext.getResources().getString(R.string.graph_label_temperature_fahrenheit);
        DEFAULT_SENSOR_NAME = mContext.getResources().getString(R.string.text_sensor_name_default);
        EMPTY_TEMPERATURE_STRING = mContext.getResources().getString(R.string.label_empty_t);
        EMPTY_RELATIVE_HUMIDITY_STRING = mContext.getResources().getString(R.string.label_empty_rh);
        PERCENTAGE_CHARACTER = mContext.getResources().getString(R.string.char_percent);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentComfortZoneBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mPlotView = root.findViewById(R.id.czplotview);
        mTextViewLeft = root.findViewById(R.id.textview_left);
        mTextViewTop = root.findViewById(R.id.textview_top);
        mTextViewRight = root.findViewById(R.id.textview_right);
        mTextViewBottom = root.findViewById(R.id.textview_bottom);
        mSensorNameTextView = root.findViewById(R.id.devicename);
        mSensorAddressTextView = root.findViewById(R.id.deviceaddress);
        mSensorAmbientTemperatureTextView = root.findViewById(R.id.text_amb_temp);
        mSensorRelativeHumidity = root.findViewById(R.id.text_rh);
        mParentFrame = root.findViewById(R.id.parentframe);

        mSensorNameTextView.setText(getString(R.string.empty_bluetooth_device));
        mSensorAddressTextView.setText(Utils.DEFAULT_EMPTY_ADDRESS);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAllDevicesService != null) {
            GoSensePagerActivity.unregisterOnConnnectionStates(mCfOnConnnectionStates);
        }
        GoSensePagerActivity.unregisterOnColorChange(mOnColorChange);
        try {
            mContext.unregisterReceiver(remoteTemperatureDataReceiver);
        } catch (Exception e) {
        }
        try {
            mContext.unregisterReceiver(remoteHumidityDataReceiver);
        } catch (Exception e) {
        }
        binding = null;
    }


    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //mActiveSensorViews.clear();
        initXyPlotView();
    }

    private void initXyPlotView() {
        mPlotView.setYAxisScale(GRAPH_MIN_Y_VALUE, GRAPH_MAX_Y_VALUE, GRAPH_Y_GRID_SIZE);
        mPlotView.setXAxisScale(GRAPH_MIN_X_VALUE, GRAPH_MAX_X_VALUE, GRAPH_X_GRID_SIZE_CELSIUS);
        mPlotView.setXAxisLabel(GRAPH_X_LABEL_CELSIUS);
        mTextViewLeft.bringToFront();
        mTextViewTop.bringToFront();
        mTextViewRight.bringToFront();
        mTextViewBottom.bringToFront();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAllDevicesService != null) {
            GoSensePagerActivity.registerOnConnnectionStates(mCfOnConnnectionStates);
        }
        updateSensorViews();
        updateViewForSelectedSeason();
        updateViewForSelectedTemperatureUnit();
        updateTextViewName();
        if (firstTempUpdateDone && firstRHUpdateDone && mActiveSensorViews.containsKey(Utils.DEFAULT_EMPTY_ADDRESS)) {
            mActiveSensorViews.remove(Utils.DEFAULT_EMPTY_ADDRESS);
        }

        startUpdates();
    }

    public void startUpdates() {
        if (mAddress != null && !mAddress.isEmpty() &&
                !mAddress.contains(Utils.DEFAULT_EMPTY_ADDRESS) &&
                mHumidity != -9999.0f && mTemperature != -9999.0f) {
            if (mActiveSensorViews != null && mActiveSensorViews.size() == 0){
                createNewSensorViewFor(mAddress, mColor);
            }
            if (mHumidity >= 0 && mHumidity <= 100 &&
                    ((mTemperature >= 15 && mTemperature <= 30) ||
                            (mTemperature >= 59 && mTemperature <= 86))) {
                visibility = View.VISIBLE;
                updateViewValues(mAddress, mTemperature, mHumidity);
            } else {
                hideViewValues(mAddress);
                visibility = View.INVISIBLE;
            }
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startUpdates();
            }
        }, 2000);
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateSensorViews() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (mActiveSensorViews) {
                    for (final String key : mActiveSensorViews.keySet()) {
                        mParentFrame.removeView(mActiveSensorViews.get(key));
                    }
                    mActiveSensorViews.clear();
                }
            }
        });
    }

    private void updateViewForSelectedSeason() {
        final boolean isSeasonWinter = TraySettings.isSeasonWinter();
        Log.i(TAG,String.format("updateViewForSelectedSeason(): Season %s was selected.",
                        isSeasonWinter ? "Winter" : "Summer"));
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlotView.updateComfortZone();
                mPlotView.invalidate();
            }
        });
    }

    @UiThread
    private void updateViewForSelectedTemperatureUnit() {
        final String mAxisLabel;
        final int gridSize;
        float minXAxisValue = GRAPH_MIN_X_VALUE;
        float maxXAxisValue = GRAPH_MAX_X_VALUE;

        mIsFahrenheit = TraySettings.isTemperatureUnitFahrenheit();

        if (mIsFahrenheit) {
            minXAxisValue = Converter.convertToF(minXAxisValue);
            maxXAxisValue = Converter.convertToF(maxXAxisValue);
            gridSize = GRAPH_X_GRID_SIZE_FAHRENHEIT;
            mAxisLabel = GRAPH_X_LABEL_FAHRENHEIT;
        } else {
            gridSize = GRAPH_X_GRID_SIZE_CELSIUS;
            mAxisLabel = GRAPH_X_LABEL_CELSIUS;
        }

        mPlotView.setXAxisLabel(mAxisLabel);
        mPlotView.setXAxisScale(minXAxisValue, maxXAxisValue, gridSize);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlotView.updateComfortZone();
                mPlotView.invalidate();
            }
        });
    }

    private void updateTextViewName() {
        try {
            String selectedAddress = TraySettings.getSelectedAddress();
            SensorDevice model = null;
            //model = BluetoothService.getSensorDeviceData(selectedAddress);
            if (model == null) {
                mActiveSensorViews.remove(selectedAddress);
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    XyPoint selectedPoint = mActiveSensorViews.get(selectedAddress);
                    if (selectedPoint != null) {
                        mSensorNameTextView.setTextColor(selectedPoint.getInnerColor());
                    } else {
                        Log.e(TAG,String.format("updateTextViewName() -> mActiveSensorViews does not contain selected address: %s",selectedAddress));
                        mSensorNameTextView.setTextColor(model.getColor());
                    }

                    //String deviceName = model.getAliasName();
                    //if (deviceName == null || deviceName.isEmpty()) {
                    //    deviceName = model.getName();
                    //}
                    //mSensorNameTextView.setText(deviceName);
                    mSensorNameTextView.setText(mName);
                }
            });
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "updateTextViewName(): The following exception was produced -> ", e);
        }
    }

    private void createNewSensorViewFor(@NonNull String address, int color) {
        try {
            XyPoint sensorPoint = new XyPoint(mContext);
            sensorPoint.setVisibility(visibility);
            sensorPoint.setTag(address);
            float pt1 = 8; //getResources().getInteger(R.integer.comfort_zone_radius_sensor_point);
            sensorPoint.setRadius(pixelsFromDp(mContext, pt1));
            float pt2 = 2;//getFloatValueFromId(getContext(), R.dimen.comfort_zone_outline_radius_offset);
            sensorPoint.setOutlineRadius(pixelsFromDp(mContext, pt1 + pt2));
            sensorPoint.setInnerColor(color);
            sensorPoint.setOutlineColor(color);
            mActiveSensorViews.put(address, sensorPoint);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mParentFrame.addView(sensorPoint);
                }
            });
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "createNewSensorViewFor -> The following exception was thrown: ", e);
        }
    }

    private boolean lastSeason = TraySettings.isSeasonWinter();
    private boolean lastTempUnit = TraySettings.isTemperatureUnitFahrenheit();

    public void hideViewValues(@NonNull final String address) {
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean isClipped = false;
                    XyPoint selectedPoint = mActiveSensorViews.get(address);
                    if (selectedPoint != null) {
                        selectedPoint.setVisibility(visibility);
                    }
                }
            });
        }
    }

    public void updateViewValues(@NonNull final String address,
                                 final float temperature,
                                 final float relativeHumidity) {
        boolean season = TraySettings.isSeasonWinter();
        if (lastSeason != season) {
            updateViewForSelectedSeason();
        }
        lastSeason = season;

        boolean tempUnit = TraySettings.isTemperatureUnitFahrenheit();
        if (lastTempUnit != tempUnit) {
            updateViewForSelectedTemperatureUnit();
        }
        lastTempUnit = tempUnit;

        float temp = temperature;
        if (TraySettings.isTemperatureUnitFahrenheit()) {
            temp = Converter.convertToF(temp);
        }
        final PointF newPos = new PointF(temp, relativeHumidity);

        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean isClipped = false;
                    if (mPlotView.isOutsideGrid(newPos)) {
                        isClipped = true;
                    }
                    XyPoint selectedPoint = mActiveSensorViews.get(address);
                    if (selectedPoint != null) {
                        selectedPoint.setVisibility(visibility);
                        updateViewPositionFor(selectedPoint, newPos, isClipped);
                    } else {
                        if (mAddress != null && !mAddress.isEmpty() &&
                                !mAddress.contains(Utils.DEFAULT_EMPTY_ADDRESS)) {
                            createNewSensorViewFor(mAddress, mColor);
                        }
                        selectedPoint = mActiveSensorViews.get(address);
                        if (selectedPoint != null) {
                            selectedPoint.setVisibility(visibility);
                            updateViewPositionFor(selectedPoint, newPos, isClipped);
                        }
                    }
                }
            });
        }
    }

    private void updateViewPositionFor(@NonNull final XyPoint selectedPoint,
                                       @NonNull final PointF p,
                                       final boolean isClipped) {
        final PointF canvasPosition;
        if (isClipped) {
            final PointF clippedPoint = mPlotView.getClippedPoint();
            if (clippedPoint == null) {
                Log.e(TAG, "updateViewPositionFor -> Cannot obtain the clipped point");
                return;
            } else {
                canvasPosition = mPlotView.coordinates(mPlotView.getClippedPoint());
            }
        } else {
            canvasPosition = mPlotView.coordinates(p);
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            @UiThread
            public void run() {
                selectedPoint.move(canvasPosition.x, canvasPosition.y);
            }
        });
    }

    private void animateSensorViewPointTo(@NonNull final XyPoint selectedPoint, final float x, final float y) {
        final float relativeX =
                x - (getDipFor(getResources().getInteger(R.integer.comfort_zone_radius_sensor_point) +
                        getFloatValueFromId(mContext, R.dimen.comfort_zone_outline_radius_offset)));
        final float relativeY =
                y - (getDipFor(getResources().getInteger(R.integer.comfort_zone_radius_sensor_point) +
                        getFloatValueFromId(mContext, R.dimen.comfort_zone_outline_radius_offset)));
        getActivity().runOnUiThread(new Runnable() {
            @Override
            @UiThread
            public void run() {
                selectedPoint.animateMove(relativeX, relativeY);
            }
        });
    }

    private float getDipFor(final float px) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }
}