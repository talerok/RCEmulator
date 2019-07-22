package com.example.rcemulator.Logic.Protocols.TAB4.DTO

open class RFID(val Version: Int, val UID: String) {

}

class RFIDWithStatus(Version: Int, UID: String, val Status_Request: String): RFID(Version, UID) {

}