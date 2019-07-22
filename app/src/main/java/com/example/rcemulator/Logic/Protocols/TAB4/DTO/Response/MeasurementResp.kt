package com.example.rcemulator.Logic.Protocols.TAB4.DTO.Response

import com.example.rcemulator.Logic.Protocols.TAB4.DTO.Measurement

class MeasurementResp(EUI: String, sequence: Int, val Measurement: Measurement): RCResponse(sequence, EUI) {

}