package com.example.rcemulator.Logic.Protocols.TAB4.DTO.Request

import com.example.rcemulator.Logic.Protocols.TAB4.DTO.RFID

class RfidReq(EUI: String, sequence: Int, val RFID: RFID): RCRequest(sequence, EUI) {

}