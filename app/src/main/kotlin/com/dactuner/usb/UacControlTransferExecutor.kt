package com.dactuner.usb

import android.hardware.usb.UsbDeviceConnection
import com.dactuner.util.DiagnosticsLogger

class UacControlTransferExecutor(
    private val logger: DiagnosticsLogger
) {
    companion object {
        private const val TIMEOUT_MS = 1000
        
        // USB standard requests
        private const val REQ_DIR_OUT = 0x00
        private const val REQ_DIR_IN = 0x80
        private const val REQ_TYPE_CLASS = 0x20
        private const val REQ_REC_INTERFACE = 0x01
        
        // Request types
        private const val REQ_TYPE_SET = REQ_DIR_OUT or REQ_TYPE_CLASS or REQ_REC_INTERFACE // 0x21
        private const val REQ_TYPE_GET = REQ_DIR_IN or REQ_TYPE_CLASS or REQ_REC_INTERFACE  // 0xA1

        // UAC Control selectors
        private const val CS_VOLUME = 0x02

        // UAC Request codes
        private const val REQ_SET_CUR = 0x01
        private const val REQ_GET_CUR = 0x81
        private const val REQ_GET_MIN = 0x82
        private const val REQ_GET_MAX = 0x83
        private const val REQ_GET_RES = 0x84
    }

    fun setVolume(
        connection: UsbDeviceConnection,
        featureUnitId: Int,
        interfaceNumber: Int,
        channel: Int,
        volume: Int
    ): Boolean {
        val data = ByteArray(2)
        data[0] = (volume and 0xFF).toByte()         // Low byte
        data[1] = ((volume shr 8) and 0xFF).toByte() // High byte
        
        val wValue = (CS_VOLUME shl 8) or channel
        val wIndex = (featureUnitId shl 8) or interfaceNumber

        val result = connection.controlTransfer(
            REQ_TYPE_SET,
            REQ_SET_CUR,
            wValue,
            wIndex,
            data,
            data.size,
            TIMEOUT_MS
        )
        
        logger.logControlTransfer("OUT", "SET_CUR", wValue, wIndex, data, result)
        return result >= 0
    }

    fun getVolume(
        connection: UsbDeviceConnection,
        featureUnitId: Int,
        interfaceNumber: Int,
        channel: Int
    ): Int? {
        val data = ByteArray(2)
        val wValue = (CS_VOLUME shl 8) or channel
        val wIndex = (featureUnitId shl 8) or interfaceNumber

        var result = connection.controlTransfer(
            REQ_TYPE_GET,
            REQ_GET_CUR,
            wValue,
            wIndex,
            data,
            data.size,
            TIMEOUT_MS
        )
        
        logger.logControlTransfer("IN", "GET_CUR_V1", wValue, wIndex, data, result)
        
        if (result < 0) {
            // Try UAC 2.0 CUR request code (0x01) instead of 0x81
            result = connection.controlTransfer(
                REQ_TYPE_GET,
                REQ_SET_CUR, // 0x01 is the CUR request code for both SET and GET in UAC 2.0
                wValue,
                wIndex,
                data,
                data.size,
                TIMEOUT_MS
            )
            logger.logControlTransfer("IN", "GET_CUR_V2", wValue, wIndex, data, result)
        }
        
        if (result >= 2) {
            val volume = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
            return if (volume > 32767) volume - 65536 else volume // Sign extension
        }
        return null
    }

    fun getVolumeRange(
        connection: UsbDeviceConnection,
        featureUnitId: Int,
        interfaceNumber: Int,
        channel: Int
    ): VolumeRange? {
        val min = executeGetRequest(connection, REQ_GET_MIN, featureUnitId, interfaceNumber, channel) ?: return null
        val max = executeGetRequest(connection, REQ_GET_MAX, featureUnitId, interfaceNumber, channel) ?: return null
        val res = executeGetRequest(connection, REQ_GET_RES, featureUnitId, interfaceNumber, channel) ?: 1
        
        return VolumeRange(min, max, res)
    }
    
    private fun executeGetRequest(
        connection: UsbDeviceConnection,
        request: Int,
        featureUnitId: Int,
        interfaceNumber: Int,
        channel: Int
    ): Int? {
        val data = ByteArray(2)
        val wValue = (CS_VOLUME shl 8) or channel
        val wIndex = (featureUnitId shl 8) or interfaceNumber

        val result = connection.controlTransfer(
            REQ_TYPE_GET,
            request,
            wValue,
            wIndex,
            data,
            data.size,
            TIMEOUT_MS
        )
        
        val reqName = when(request) {
            REQ_GET_MIN -> "GET_MIN"
            REQ_GET_MAX -> "GET_MAX"
            REQ_GET_RES -> "GET_RES"
            else -> "GET_UNKNOWN"
        }
        logger.logControlTransfer("IN", reqName, wValue, wIndex, data, result)
        
        if (result >= 2) {
            val volume = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
            return if (volume > 32767) volume - 65536 else volume // Sign extension
        }
        return null
    }
}
