package com.example.rcemulator.Logic

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent

class AdapterChecker(private val _adapter: BluetoothAdapter, private val _activity: Activity) {

    fun check(): Boolean {
        _adapter ?: return false;

        if(!_adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this._activity.startActivityForResult(enableBtIntent, 1);
            return false;
        }
        return true;
    }
}