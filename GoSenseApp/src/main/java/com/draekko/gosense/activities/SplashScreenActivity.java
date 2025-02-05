package com.draekko.gosense.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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


    private boolean isBtScanConnectPermissionGranted() {
        boolean canConnectBt = true;
        boolean canScanBt = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            canScanBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            canConnectBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt) {
                requestBtPermission(new String[]{ BLUETOOTH_SCAN, BLUETOOTH_CONNECT });
            }
        }
        return canConnectBt;
    }

    private boolean isBtConnectScanPermissionGranted() {
        boolean canConnectBt = true;
        boolean canScanBt = true;
        boolean canLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            canLocation = ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt || !canLocation) {
                requestBtPermission(new String[]{ ACCESS_FINE_LOCATION });
            }
            return canLocation;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canConnectBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            canScanBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            canLocation = ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt || !canLocation) {
                if ((!canScanBt || !canConnectBt) && canLocation) {
                    isBtScanConnectPermissionGranted();
                } else {
                    requestBtPermission(new String[]{ BLUETOOTH_CONNECT, BLUETOOTH_SCAN, ACCESS_FINE_LOCATION });
                }
            }
        }
        return canConnectBt && canScanBt;
    }

    ActivityResultLauncher<Intent> fileaccessActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Intent goSense = new Intent(mActivity, SplashScreenActivity.class);
                    startActivity(goSense);
                    finish();
                    finishAffinity();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        boolean dosplash = false;

        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                dosplash = true;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean canWriteMedia = ContextCompat.checkSelfPermission(mActivity, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean canConnectBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean canScanBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean canLocation = ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt || !canLocation || !canWriteMedia) {
                dosplash = true;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean canWriteMedia = ContextCompat.checkSelfPermission(mActivity, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean canLocation = ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canLocation || !canWriteMedia) {
                dosplash = true;
            }
        }

        if (dosplash) {
            setContentView(R.layout.splashscreen);
        }
    }

    public void startup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(mActivity).
                        setTitle(R.string.title_file_permission).
                        setMessage(R.string.text_file_permission).
                        setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //request for the permission
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    fileaccessActivityResultLauncher.launch(intent);
                                } else {
                                    startActivityForResult(intent, REQUEST_MANAGE_FILES);
                                }
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
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean canConnectBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean canScanBt = ContextCompat.checkSelfPermission(mActivity, BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean canLocation = ContextCompat.checkSelfPermission(mActivity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!canConnectBt || !canScanBt || !canLocation) {
                requestBtPermission(new String[]{ BLUETOOTH_CONNECT, BLUETOOTH_SCAN, ACCESS_FINE_LOCATION });
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


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        restartSplash();
    }

    private void restartSplash() {
        Intent intent = new Intent(mActivity, SplashScreenActivity.class);
        startActivity(intent);
        finish();
        finishAffinity();
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

    private void requestNotificationPermission(String[] permissions) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0])) {
            new androidx.appcompat.app.AlertDialog.Builder(mActivity).
                    setTitle(R.string.title_notify_permission).
                    setMessage(R.string.text_notify_permission).
                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_POST_PERMISSION);
                            dialog.dismiss();
                        }
                    }).
                    create().
                    show();
        } else {
            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_POST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_PERMISSION) {
            Intent goSense = new Intent(mActivity, GoSensePagerActivity.class);
            startActivity(goSense);
            finish();
            finishAffinity();
        }
    }

}
