package com.draekko.gosense.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.draekko.gosense.R;

public class SplashScreenActivity extends AppCompatActivity {

    public static final int REQUEST_BT_PERMISSION            = 0x3002;
    public static final int REQUEST_ENABLE_BT                = 0x1001;
    public static final int REQUEST_OAUTH_REQUEST_CODE       = 0x1003;
    public static final int REQUEST_PAIRING                  = 0x1002;
    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 0x1004;
    public static final int REQUEST_SETTINGS                 = 0x2000;
    public static final int REQUEST_MANAGE_FILES             = 0x1010;
    public static final int REQUEST_LOCATION_PERMISSION      = 0x2020;
    public static final int REQUEST_POST_PERMISSION          = 0x3030;

    private Activity mActivity;

    private void requestBtPermission(String[] permissions) {
        ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_BT_PERMISSION);
        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0])) {
            new AlertDialog.Builder(mActivity).
                    setTitle(R.string.title_bt_permission).
                    setMessage(R.string.text_bt_permission).
                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_BT_PERMISSION);
                        }
                    }).
                    setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            finishAffinity();
                        }
                    }).
                    create().
                    show();
        } else {
            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_BT_PERMISSION);
        }
    }

    private void requestLocationPermission(String[] permissions) {
        ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_LOCATION_PERMISSION);
        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0])) {
            new AlertDialog.Builder(mActivity).
                    setTitle(R.string.title_location_permission).
                    setMessage(R.string.text_location_permission).
                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_LOCATION_PERMISSION);
                        }
                    }).
                    setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            finishAffinity();
                        }
                    }).
                    create().
                    show();
        } else {
            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);

        boolean dosplash = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean canConnectBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean canScanBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt) {
                dosplash = true;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean canLocation = ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canLocation) {
                dosplash = true;
            }
        }

        if (dosplash) {
            setContentView(R.layout.splashscreen);
        }
    }

    public void startup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean canConnectBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean canScanBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt) {
                requestBtPermission(new String[]{ BLUETOOTH_CONNECT, BLUETOOTH_SCAN });
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean canLocation = ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canLocation) {
                requestLocationPermission(new String[]{ ACCESS_FINE_LOCATION });
                return;
            }
        }

        if (!isPostPermissionGranted()) {
            return;
        }

        Intent goSense = new Intent(mActivity, GoSensePagerActivity.class);
        startActivity(goSense);
        finish();
        finishAffinity();
    }

    @Override
    public void onResume() {
        super.onResume();
        startup();
    }

    private boolean isPostPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{ POST_NOTIFICATIONS }, REQUEST_POST_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSION || requestCode == REQUEST_LOCATION_PERMISSION) {
            startup();
        } else if (requestCode == REQUEST_POST_PERMISSION) {
            Intent goSense = new Intent(mActivity, GoSensePagerActivity.class);
            startActivity(goSense);
            finish();
            finishAffinity();
        }
    }
}