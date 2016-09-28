package com.salma.bletest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

  /* Check to make sure BLE is supported */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
  /* Get a Bluetooth Adapter Object */
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (Build.VERSION.SDK_INT < 21)
                    {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                    else
                    {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21)
            {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT < 21)
            {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }


    private ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
   /* Connect to device found */
            Log.i("callbackType", String.valueOf(callbackType));

            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
   /* Process a batch scan results */
            for (ScanResult sr : results)
            {
                Log.i("Scan Item: ", sr.toString());
            }
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback()
            {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device)
    {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, mainGattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback mainGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("mainGattCallback", "CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("mainGattCallback", "DISCONNECTED");
                    break;
                default:
                    Log.e("mainGattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status)
        {
            Log.i("onCharacteristicRead", characteristic.toString());
        }
    };
}