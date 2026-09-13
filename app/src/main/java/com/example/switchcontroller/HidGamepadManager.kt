package com.example.switchcontroller

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.concurrent.Executor

/**
 * يدير تسجيل الهاتف كجهاز HID (Bluetooth HID Device profile) ويحاكي
 * جويباد عام (Generic Gamepad)، ثم يرسل تقارير الإدخال (Input Reports)
 * عند تغيّر حالة الأزرار / العصي.
 *
 * يتطلب Android 9 (API 28) أو أعلى، وصلاحية BLUETOOTH_CONNECT على
 * Android 12+.
 */
class HidGamepadManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onAppRegistered()
        fun onAppUnregistered()
        fun onConnectionStateChanged(device: BluetoothDevice?, state: Int)
        fun onError(message: String)
    }

    companion object {
        private const val TAG = "HidGamepadManager"
    }

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    // آخر حالة تم إرسالها (buttons16 + hat + 4 محاور = 7 بايت بيانات + 1 Report ID)
    private val reportBuffer = ByteArray(GamepadDescriptor.REPORT_SIZE_BYTES)

    init {
        reportBuffer[0] = GamepadDescriptor.REPORT_ID
        // منتصف العصي الافتراضي = 128
        reportBuffer[4] = 128.toByte() // Left X
        reportBuffer[5] = 128.toByte() // Left Y
        reportBuffer[6] = 128.toByte() // Right X
        reportBuffer[7] = 128.toByte() // Right Y
        setHat(GamepadDescriptor.Hat.NEUTRAL)
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) listener.onAppRegistered() else listener.onAppUnregistered()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            connectedDevice = if (state == BluetoothProfile.STATE_CONNECTED) device else null
            listener.onConnectionStateChanged(device, state)
        }

        override fun onGetReport(
            device: BluetoothDevice?,
            type: Byte,
            id: Byte,
            bufferSize: Int
        ) {
            hidDevice?.replyReport(device, type, id, reportBuffer)
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            // السوتش قد يرسل تقارير Output (مثل الاهتزاز) - نتجاهلها حاليًا
        }
    }

    /** يبدأ الاتصال بخدمة HID في نظام أندرويد (يجب استدعاؤها بعد التأكد من الصلاحيات). */
    fun start() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            listener.onError("لا يوجد دعم بلوتوث على هذا الجهاز")
            return
        }
        if (!adapter.isEnabled) {
            listener.onError("الرجاء تفعيل البلوتوث أولاً")
            return
        }
        val ok = adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        if (!ok) {
            listener.onError("تعذر الحصول على خدمة HID من النظام")
        }
    }

    fun stop() {
        hidDevice?.let { hid ->
            connectedDevice?.let { hid.disconnect(it) }
            hid.unregisterApp()
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null
    }

    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Switch Virtual Gamepad",      // الاسم الذي يظهر عند الإقران
            "Virtual gamepad over BT HID", // الوصف
            "DIY",                          // اسم المزوّد
            BluetoothHidDevice.SUBCLASS1_COMBO, // فئة الجهاز (جويباد/كومبو)
            GamepadDescriptor.DESCRIPTOR
        )

        val executor = Executor { command -> command.run() }

        hidDevice?.registerApp(
            sdp,
            null, // inQos (افتراضي)
            null, // outQos (افتراضي)
            executor,
            callback
        )
    }

    /** يجعل الهاتف قابل للاكتشاف حتى يظهر للسوتش أثناء وضع الإقران. */
    fun requestConnect(device: BluetoothDevice) {
        hidDevice?.connect(device)
    }

    // ---------- تحديث حالة الأزرار والعصي ----------

    fun setButton(bitIndex: Int, pressed: Boolean) {
        val byteIndex = 1 + (bitIndex / 8) // بعد بايت الـ Report ID
        val bitMask = 1 shl (bitIndex % 8)
        val current = reportBuffer[byteIndex].toInt()
        reportBuffer[byteIndex] = if (pressed) {
            (current or bitMask).toByte()
        } else {
            (current and bitMask.inv()).toByte()
        }
        sendReport()
    }

    fun setHat(hatValue: Int) {
        // البايت رقم 3 (index) يحوي Hat في أول 4 بت
        val byteIndex = 3
        val current = reportBuffer[byteIndex].toInt()
        reportBuffer[byteIndex] = ((current and 0xF0) or (hatValue and 0x0F)).toByte()
        sendReport()
    }

    fun setLeftStick(x: Int, y: Int) {
        reportBuffer[4] = x.coerceIn(0, 255).toByte()
        reportBuffer[5] = y.coerceIn(0, 255).toByte()
        sendReport()
    }

    fun setRightStick(x: Int, y: Int) {
        reportBuffer[6] = x.coerceIn(0, 255).toByte()
        reportBuffer[7] = y.coerceIn(0, 255).toByte()
        sendReport()
    }

    private fun sendReport() {
        val device = connectedDevice ?: return
        val hid = hidDevice ?: return
        try {
            hid.sendReport(device, GamepadDescriptor.REPORT_ID.toInt(), reportBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "فشل إرسال التقرير", e)
        }
    }
}
