package com.example.rcemulator.Logic.Protocols.TAB4.DTO

class SendMetering(Version: Int, Date_Time: String, Type: Array<String>, val Value: Array<Double>): Measurement(Version, Date_Time, Type) {

}