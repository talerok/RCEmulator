package com.example.rcemulator.Logic.Protocols

import com.example.rcemulator.Logic.RemoteDevice
import kotlinx.coroutines.*


interface Protocol {
    fun sendId(device: RemoteDevice, id: String): Deferred<Boolean>;
    fun sendValue(device: RemoteDevice, value: Double): Deferred<Boolean>;
}