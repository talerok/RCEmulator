package com.example.rcemulator.Logic.Protocols.TAB4.DTO.Response

import com.example.rcemulator.Logic.Protocols.TAB4.DTO.RFIDWithStatus

class RfidResp(EUI: String, sequence: Int, val RFID: RFIDWithStatus): RCResponse(sequence, EUI) {

}