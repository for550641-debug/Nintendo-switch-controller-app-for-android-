package com.example.switchcontroller

/**
 * وصف HID (Report Descriptor) لجهاز تحكم عام (Generic Gamepad) بـ 16 زر
 * + Hat Switch (D-Pad) + عصا تحكم يسار (X,Y) + عصا تحكم يمين (Z,Rz).
 *
 * بنية الـ Input Report (بدون احتساب Report ID) هي 7 بايتات:
 *  Byte 0-1 : 16 bit  -> حالة الأزرار (bit لكل زر)
 *  Byte 2   : 4 bit Hat switch (D-Pad) + 4 bit padding
 *  Byte 3   : محور X لعصا التحكم اليسرى (0-255، 128 = المنتصف)
 *  Byte 4   : محور Y لعصا التحكم اليسرى
 *  Byte 5   : محور X لعصا التحكم اليمنى (Z)
 *  Byte 6   : محور Y لعصا التحكم اليمنى (Rz)
 *
 * ملاحظة: هذا وصف HID عام متوافق مع أغلب الأجهزة التي تتعرف على
 * "USB/BT Gamepad" قياسي. تجربته الفعلية مع السوتش قد تحتاج تعديل
 * دقيق (خصوصًا الأزرار الخاصة بـ Home/Capture) لأن بروتوكول
 * الجويكون الحقيقي أعقد من ذلك (راجع ملاحظات dekuNukem).
 */
object GamepadDescriptor {

    const val REPORT_ID: Byte = 0x01
    const val REPORT_SIZE_BYTES = 8 // يشمل بايت الـ Report ID

    val DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05,    // Usage (Game Pad)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x85.toByte(), REPORT_ID, //   Report ID (1)

        // ----- 16 زر -----
        0x05, 0x09,             //   Usage Page (Button)
        0x19, 0x01,             //   Usage Minimum (Button 1)
        0x29, 0x10,             //   Usage Maximum (Button 16)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95.toByte(), 0x10,    //   Report Count (16)
        0x81.toByte(), 0x02,    //   Input (Data,Var,Abs)

        // ----- Hat Switch (D-Pad) -----
        0x05, 0x01,             //   Usage Page (Generic Desktop)
        0x09, 0x39,             //   Usage (Hat switch)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x07,             //   Logical Maximum (7)
        0x35, 0x00,             //   Physical Minimum (0)
        0x46.toByte(), 0x3B, 0x01, //   Physical Maximum (315)
        0x65, 0x14,             //   Unit (Eng Rot:Angular Pos)
        0x75, 0x04,             //   Report Size (4)
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x81.toByte(), 0x42,    //   Input (Data,Var,Abs,Null State)
        0x75, 0x04,             //   Report Size (4)  -- padding
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x81.toByte(), 0x01,    //   Input (Constant) -- padding

        // ----- عصا يسار + عصا يمين -----
        0x05, 0x01,             //   Usage Page (Generic Desktop)
        0x09, 0x30,             //   Usage (X)
        0x09, 0x31,             //   Usage (Y)
        0x09, 0x32,             //   Usage (Z)
        0x09, 0x35,             //   Usage (Rz)
        0x15, 0x00,             //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00, //   Logical Maximum (255)
        0x75, 0x08,             //   Report Size (8)
        0x95.toByte(), 0x04,    //   Report Count (4)
        0x81.toByte(), 0x02,    //   Input (Data,Var,Abs)

        0xC0.toByte()           // End Collection
    )

    // ترتيب البتّات داخل البايتين الأولين (16 زر)
    object Button {
        const val Y = 0
        const val B = 1
        const val A = 2
        const val X = 3
        const val L = 4
        const val R = 5
        const val ZL = 6
        const val ZR = 7
        const val MINUS = 8
        const val PLUS = 9
        const val L_STICK = 10
        const val R_STICK = 11
        const val HOME = 12
        const val CAPTURE = 13
        // 14, 15 غير مستخدمة حاليًا
    }

    // قيم Hat Switch القياسية (اتجاه الـ D-Pad)
    object Hat {
        const val UP = 0
        const val UP_RIGHT = 1
        const val RIGHT = 2
        const val DOWN_RIGHT = 3
        const val DOWN = 4
        const val DOWN_LEFT = 5
        const val LEFT = 6
        const val UP_LEFT = 7
        const val NEUTRAL = 8 // خارج المجال المنطقي = لا اتجاه
    }
}
