package com.example.switchcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), HidGamepadManager.Listener {

    private lateinit var hidManager: HidGamepadManager
    private lateinit var statusText: TextView

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }

    private val permissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        hidManager = HidGamepadManager(this, this)

        setupButtons()

        if (hasAllPermissions()) {
            hidManager.start()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, permissionRequestCode)
        }
    }

    private fun hasAllPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (hasAllPermissions()) {
                hidManager.start()
            } else {
                Toast.makeText(this, "التطبيق يحتاج صلاحيات البلوتوث للعمل", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtons() {
        // D-Pad -> Hat switch
        bindHat(R.id.btnUp, GamepadDescriptor.Hat.UP)
        bindHat(R.id.btnDown, GamepadDescriptor.Hat.DOWN)
        bindHat(R.id.btnLeft, GamepadDescriptor.Hat.LEFT)
        bindHat(R.id.btnRight, GamepadDescriptor.Hat.RIGHT)

        // أزرار الحركة الرئيسية
        bindButton(R.id.btnA, GamepadDescriptor.Button.A)
        bindButton(R.id.btnB, GamepadDescriptor.Button.B)
        bindButton(R.id.btnX, GamepadDescriptor.Button.X)
        bindButton(R.id.btnY, GamepadDescriptor.Button.Y)

        bindButton(R.id.btnL, GamepadDescriptor.Button.L)
        bindButton(R.id.btnR, GamepadDescriptor.Button.R)
        bindButton(R.id.btnZL, GamepadDescriptor.Button.ZL)
        bindButton(R.id.btnZR, GamepadDescriptor.Button.ZR)

        bindButton(R.id.btnMinus, GamepadDescriptor.Button.MINUS)
        bindButton(R.id.btnPlus, GamepadDescriptor.Button.PLUS)
        bindButton(R.id.btnHome, GamepadDescriptor.Button.HOME)
        bindButton(R.id.btnCapture, GamepadDescriptor.Button.CAPTURE)
    }

    /** يربط زر واجهة بزر افتراضي في تقرير HID (ضغط/تحرير حقيقي وليس نقرة). */
    private fun bindButton(viewId: Int, bitIndex: Int) {
        val view = findViewById<Button>(viewId)
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> hidManager.setButton(bitIndex, true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> hidManager.setButton(bitIndex, false)
            }
            true
        }
    }

    /** يربط زر D-Pad بقيمة Hat switch، مع العودة للوضع المحايد عند الرفع. */
    private fun bindHat(viewId: Int, hatValue: Int) {
        val view = findViewById<Button>(viewId)
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> hidManager.setHat(hatValue)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    hidManager.setHat(GamepadDescriptor.Hat.NEUTRAL)
            }
            true
        }
    }

    // ---------- HidGamepadManager.Listener ----------

    override fun onAppRegistered() {
        runOnUiThread { statusText.text = "تم تسجيل الجهاز، بانتظار الإقران من السوتش..." }
    }

    override fun onAppUnregistered() {
        runOnUiThread { statusText.text = "تم إلغاء تسجيل الجهاز" }
    }

    override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        runOnUiThread {
            statusText.text = when (state) {
                BluetoothProfile.STATE_CONNECTED -> "متصل بـ ${device?.name ?: "السوتش"}"
                BluetoothProfile.STATE_CONNECTING -> "جاري الاتصال..."
                BluetoothProfile.STATE_DISCONNECTED -> "غير متصل"
                else -> "حالة غير معروفة"
            }
        }
    }

    override fun onError(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.stop()
    }
}
