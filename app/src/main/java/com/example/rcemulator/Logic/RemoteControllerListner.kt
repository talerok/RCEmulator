package com.example.rcemulator.Logic

interface RemoteControllerListner {
    fun onDeviceListChange(devices: Array<RemoteDevice>);
}