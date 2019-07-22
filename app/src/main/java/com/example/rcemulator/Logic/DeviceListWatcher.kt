package com.example.rcemulator.Logic

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class DeviceListWatcher(private val _adapter: BluetoothAdapter, private val _listner: RemoteControllerListner, private val _activity: Activity) {

    private val _adapterChecker = AdapterChecker(_adapter, _activity);

    private val _enabledReciver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if(state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF) {
                _sendDeviceList();
            }
        }
    };

    init {
        val enabledFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this._activity.applicationContext.registerReceiver(this._enabledReciver, enabledFilter);
    }

    private fun _sendDeviceList() {
        if(this._adapter.isEnabled) {
            val devices = _adapter.bondedDevices.map { x ->
                RemoteDevice(
                    x.name,
                    x.address
                )
            }.toTypedArray();
            this._listner.onDeviceListChange(devices);
        } else {
            this._listner.onDeviceListChange(emptyArray());
        }
    }

    fun refreshDeviceList() {
        if(!this._adapterChecker.check())
            return;
        this._sendDeviceList();
    }

}