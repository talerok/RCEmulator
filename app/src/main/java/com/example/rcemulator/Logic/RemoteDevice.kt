package com.example.rcemulator.Logic

class RemoteDevice(val name: String, val address: String) {

    override fun toString(): String {
        return "${this.name} (${this.address})"
    }
}