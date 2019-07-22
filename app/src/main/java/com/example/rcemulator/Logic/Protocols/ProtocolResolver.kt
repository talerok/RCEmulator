package com.example.rcemulator.Logic.Protocols

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.example.rcemulator.Logic.Protocols.TAB4.TAB4Protocol
import java.io.File
import org.reflections.Reflections
import org.reflections.serializers.JsonSerializer
import org.reflections.util.ConfigurationBuilder


class ProtocolFactory(val name: String, val build: (BluetoothAdapter, Activity, (data: String) -> Unit) -> Protocol);

class ProtocolResolver(private val _adapter: BluetoothAdapter, private val _activity: Activity, private val _logger: (data: String) -> Unit) {

    private val _protocols = arrayOf(
        ProtocolFactory("TAB4", { adapter, activity, logger -> TAB4Protocol(adapter, activity, logger) })
    );

    fun getAllProtocols(): List<String> {
        return _protocols.map { x -> x.name }
    }

    fun resolve(name: String): Protocol {
        val factory = _protocols.first{ x -> x.name == name };
        if(factory == null)
            throw IllegalArgumentException("protocol ${name} not found");
        return factory.build(_adapter, _activity, _logger);
    }
}