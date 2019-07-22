package com.example.rcemulator.Logic.Protocols.TAB4.DTO.Request

import com.example.rcemulator.Logic.Protocols.TAB4.DTO.SendMetering

class MeteringReq(EUI: String, sequence: Int, val Send_Metering: SendMetering): RCRequest(sequence, EUI) {

}