package com.example.rcemulator.Logic.Protocols.TAB4

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.RFID
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Request.MeasurementReq
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Request.MeteringReq
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Request.RCRequest
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Request.RfidReq
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Response.MeasurementResp
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Response.MeteringResp
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Response.RCResponse
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Response.RfidResp
import com.example.rcemulator.Logic.Protocols.TAB4.DTO.SendMetering
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import com.example.rcemulator.Logic.AdapterChecker
import com.example.rcemulator.Logic.Protocols.Protocol
import com.example.rcemulator.Logic.RemoteDevice
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern


class TAB4ProtocolConnectionReader(private val handler: (data: String) -> Unit, private val closeHandler: () -> Unit, private val socket: BluetoothSocket) {

    private suspend fun _sendData(data: String) {
        withContext(Dispatchers.Main) {
            handler(data);
        }
    }

    init {
        GlobalScope.launch {
            while(socket.isConnected) {
                var info: Int = 0;
                try {
                    info = socket.inputStream.read();
                } catch(ex: Exception) {
                    withContext(Dispatchers.Main) {
                        closeHandler();
                    }
                    break;
                }
                var bytes = ByteArray(socket.inputStream.available());
                socket.inputStream.read(bytes);
                withContext(Dispatchers.Main) {
                    _sendData(info.toChar() + String(bytes, StandardCharsets.US_ASCII));
                }
            }
        }.start();
    }

}

class RequestInfo<R>(val request: RCRequest, val responseClass: Class<R>) where R: RCResponse {

}

class TAB4ProtocolConnection(private val _adapter: BluetoothAdapter, private val _logger: (data: String) -> Any) {
    private var _clientSocket:BluetoothSocket? = null;
    private var _serverAddress:String? = null;
    private var _connectionReader: TAB4ProtocolConnectionReader? = null;

    private val _requestHashMap = HashMap<Int, RequestInfo<*>>();

    private val _responseBroadCast = ConflatedBroadcastChannel<RCResponse>();

    private val _responsePattern = Pattern.compile("(\\w+): +([\\w\\W]+)");

    private val _gson = Gson();

    private fun _createSocket(device: BluetoothDevice, uuid: UUID): BluetoothSocket {
        val constructor = BluetoothSocket::class.java.declaredConstructors.first { x -> x.parameterTypes.count() == 7 };
        constructor.isAccessible = true;
        //BluetoothSocket(int type, int fd, boolean auth, boolean encrypt, BluetoothDevice device, int port, ParcelUuid uuid)
        return constructor?.newInstance(BluetoothSocket.TYPE_RFCOMM, -1, false, false, device, 1, ParcelUuid(uuid)) as BluetoothSocket;
    }

    private fun _createConnection(address: String) {
        if(this._clientSocket != null || address == null)
            return;

        this._logger("Creating connection");

        val device = this._adapter.getRemoteDevice(address);
        this._clientSocket = this._createSocket(device, UUID.fromString("9494ee87-585d-4b80-b971-e32462d0e32a"));
        this._clientSocket?.connect();
        this._connectionReader = TAB4ProtocolConnectionReader(
            ::_onGetData,
            ::closeConnection,
            this._clientSocket!!
        );
        this._serverAddress = address;
    }

    public fun closeConnection() {
        if(this._clientSocket == null)
            return;

        val socket = this._clientSocket!!;
        this._clientSocket = null;
        this._serverAddress = null;
        this._connectionReader = null;

        socket.close();
        this._logger("Connection closed");
    }

    private fun _sendData(data: RequestInfo<*>) {

        val json = "${data.request.javaClass.simpleName}: ${this._gson.toJson(data.request)}";

        this._clientSocket?.outputStream?.write(json.toByteArray());
        this._requestHashMap.put(data.request.sequence, data);
        this._logger("Request: $json");
    }

    private fun _fromJson(data: String): Any? {
        val matcher = _responsePattern.matcher(data);
        if(!matcher.matches())
            return null;

        val cls = Class.forName("com.example.rcemulator.Logic.Protocols.TAB4.DTO.Response.${matcher.group(1)}");
        if(cls == null)
            return null;
        var json = matcher.group(2);
        return this._gson.fromJson(json, cls);
    }

    private fun _onGetData(data: String) {
        try {
            val resp = _fromJson(data);
            if(resp == null || resp !is RCResponse)
                return;
            val req = this._requestHashMap.get(resp.sequence);
            if(req == null)
                return;

            if (req != null) {
                this._requestHashMap.remove(resp.sequence);
                this._logger("Response: $data");
                this._responseBroadCast.offer(resp);
            }
        } catch (ex: Exception) {
            print(ex.message);
        }
    }

    private suspend fun _waitForResponse(request: RCRequest, timeout: Long = 0): RCResponse? {
        var response: RCResponse? = null;
        if(timeout <= 0) {
            _responseBroadCast.consumeEach {
                if (request.sequence === it.sequence) {
                    response = it;
                    return  it;
                }
            }
        } else {
            try{
                withTimeout(timeout) {
                    response = GlobalScope.async {
                        _waitForResponse(request)
                    }.await();
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    _logger(ex.message!!);
                }
            }
        }
        return response;
    }

    fun<R> request(remoteDevice: RemoteDevice, data: RCRequest, responseClass: Class<R>, timeout: Long = 5000): Deferred<R?>
    where R: RCResponse
    {
        try {
            if (this._serverAddress != remoteDevice.address) {
                this.closeConnection();
                this._createConnection(remoteDevice.address);
            }

            this._sendData(RequestInfo(data, responseClass));

            val res = GlobalScope.async {
                _waitForResponse(data, timeout) as R?
            };

            return res;
        } catch (ex: Exception) {
            this.closeConnection();
            return GlobalScope.async {
                null
            }
        }
    }
}

class TAB4Protocol(private val _adapter: BluetoothAdapter, private val _activity: Activity, private val _logger: (data: String) -> Unit): Protocol {

    private var _sequence = 0;
    private val _connection = TAB4ProtocolConnection(_adapter, _logger);
    private val _adapterChecker = AdapterChecker(_adapter, _activity);

    override fun sendId(device: RemoteDevice, id: String): Deferred<Boolean> {
        if(device == null || !_adapterChecker.check())
            return GlobalScope.async {
                false
            }

        val req = this._connection.request(device,
            RfidReq(
                this._adapter.address,
                this._sequence++,
                RFID(1, id)
            ), RfidResp::class.java);

        return GlobalScope.async {
            val res = req.await();
            res != null && res.RFID != null && res.RFID.Status_Request == "Ok"
        }
    }

    override fun sendValue(device: RemoteDevice, value: Double): Deferred<Boolean> {
        if(device == null || !_adapterChecker.check())
            return GlobalScope.async {
                false
            }

        return GlobalScope.async {
            var res = false;
            val measResp = _connection.request(device,
                MeasurementReq(_adapter.address, _sequence++), MeasurementResp::class.java).await();

            if (measResp != null && measResp.Measurement != null) {
                val metering = SendMetering(
                    measResp?.Measurement?.Version!!,
                    measResp?.Measurement?.Date_Time!!,
                    measResp?.Measurement?.Type!!,
                    Array<Double>(measResp?.Measurement.Type.count(), { value })
                );

                val metResp = _connection.request(
                    device,
                    MeteringReq(
                        _adapter.address,
                        _sequence++,
                        metering
                    ), MeteringResp::class.java
                ).await();

                res = metResp != null && metResp.Status_Request == "Ok";
            }

            res
        }
    }
}