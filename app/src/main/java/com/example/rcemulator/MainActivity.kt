package com.example.rcemulator

import android.bluetooth.BluetoothAdapter
import android.graphics.Color
import android.graphics.PorterDuff
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.rcemulator.Logic.DeviceListWatcher
import com.example.rcemulator.Logic.RemoteControllerListner
import com.example.rcemulator.Logic.RemoteDevice
import com.example.rcemulator.Logic.Protocols.Protocol
import com.example.rcemulator.Logic.Protocols.ProtocolResolver
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class IndicatorState(val color: Int, val name: String) {

}

enum class InitcatorStates(val state: IndicatorState) {
    OK(IndicatorState(Color.GREEN, "Зеленый")),
    FAIL(IndicatorState(Color.RED, "Красный")),
    NEUTRAL(IndicatorState(Color.WHITE, "Выключен"))
}

class MainActivity : AppCompatActivity(), RemoteControllerListner {

    private var _mainLayout: View? = null;

    private var _idInput: EditText? = null;
    private var _valInput: EditText? = null;
    private var _deviceList: Spinner? = null;
    private var _protocolList: Spinner? = null;
    private var _indicator: ImageView? = null;
    private var _logs: TextView? = null;
    private val _logsDateFormat = SimpleDateFormat("hh:mm:ss");

    private val _adapter = BluetoothAdapter.getDefaultAdapter();
    private var _deviceListWatcher: DeviceListWatcher? = null;

    private var _protocolResolver: ProtocolResolver? = null;
    private val _protocols = HashMap<String, Protocol>();

    private fun _init() {

        this._mainLayout = findViewById(R.id.mainLayout);

        this._idInput = findViewById(R.id.rfid_input);
        this._valInput = findViewById(R.id.value_input);
        this._indicator = findViewById(R.id.indicator);

        this._deviceList = findViewById(R.id.device_list);
        this._protocolList = findViewById(R.id.protocol_list);
        this._logs = findViewById(R.id.logs_text_view);

        this._protocolResolver = ProtocolResolver(this._adapter, this, { x-> this._writeToLog(x) });
        this._deviceListWatcher = DeviceListWatcher(_adapter,this, this);
        this._deviceListWatcher?.refreshDeviceList();
        this._refreshProtocolList();
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this._init();
        this._writeToLog("Приложение инициализировано")
    }

    private fun _refreshProtocolList() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, this._protocolResolver!!.getAllProtocols());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this._protocolList?.adapter = adapter;
    }

    private fun _writeToLog(text: String) {
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                runBlocking {
                    _logs?.setText("${_logsDateFormat.format(Calendar.getInstance().time)}: ${text}\n${_logs?.text}");
                }
            }
        }
    }

    private fun _showToast(text: String) {
        val toast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private fun _setIndicatorColor(state: InitcatorStates, log: Boolean = true) {
        this._indicator?.drawable?.setColorFilter(state.state.color, PorterDuff.Mode.MULTIPLY)
        if(log)
            this._writeToLog("Цвет индикатора изменен на ${state.state.name}")
    }

    suspend fun _flashIndicator(state: InitcatorStates) {
        withContext(Dispatchers.Main) {
        _writeToLog("Инидкатор мигает [${state.state.name}]")
            for(i in 0..5) {
                delay(150);
                _setIndicatorColor(state, false);
                delay(150);
                _setIndicatorColor(InitcatorStates.NEUTRAL, false);
            }
        }
    }

    private fun _validateValue(): Boolean {
        if(this._valInput?.text == null || this._valInput?.text?.length == 0) {
            this._showToast("Поле значение не может быть пустым");
            return false;
        } else {
            return true;
        }
    }

    private fun _block(block: Boolean, view: View? = this._mainLayout) {
        view?.setEnabled(!block);
        if (view is ViewGroup) {
            for (idx in 0..view.getChildCount())
               this._block(block, view.getChildAt(idx));
        }
    }

    private val _protocol: Protocol
        get() {
            val protocolName = this._protocolList?.selectedItem as String;
            if(this._protocols.containsKey(protocolName))
                return this._protocols.get(protocolName)!!;
            val protocol = this._protocolResolver!!.resolve(protocolName);
            this._protocols.put(protocolName, protocol);
            return protocol;
        };

    fun sendIdButtonClick(view: View) {

        if(this._deviceList?.selectedItem == null)
            return;

        this._writeToLog("Id отправлен");
        this._block(true);

        val selectedDevice = this._deviceList?.selectedItem as RemoteDevice;

        GlobalScope.launch(Dispatchers.Main) {
            val res = _protocol.sendId(selectedDevice, _idInput?.text.toString())!!.await();
            _writeToLog(if (res) "Успех" else "Неудача");
            if (res)
                _flashIndicator(InitcatorStates.OK);
            else
                _flashIndicator(InitcatorStates.FAIL);
            _block(false);
        }
    }

    override fun onDeviceListChange(devices: Array<RemoteDevice>) {
        val adapter = ArrayAdapter<RemoteDevice>(this, android.R.layout.simple_spinner_item, devices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this._deviceList?.adapter = adapter;
    }

    fun sendValueButtonClick(view: View) {
        if(!this._validateValue() ||  this._deviceList?.selectedItem == null)
            return;

        val selectedDevice = this._deviceList?.selectedItem as RemoteDevice;

        GlobalScope.launch(Dispatchers.Main) {
            val res = _protocol.sendValue(selectedDevice, _valInput?.text.toString().toDouble())!!.await();
            _writeToLog(if (res) "Успех" else "Неудача");
            if (res)
                _flashIndicator(InitcatorStates.OK);
            else
                _flashIndicator(InitcatorStates.FAIL);
            _block(false);
        }

        this._writeToLog("Значение отправлено");
        this._block(true);

    }
}
