package com.alzikr.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class QuickParseTest {
    @Test fun phrases() {
        val cases = listOf("اتصل بأمي غدًا بعد العصر", "اجتماع الخميس الساعة 5 مساء", "الوتر كل يوم", "صيام الاثنين والخميس كل أسبوع",
            "زيارة جدتي الجمعة بعد العصر", "دواء الساعة 9:30", "شراء خبز", "مراجعة الدرس بعد غد", "كل اثنين درس", "دفع الفاتورة كل شهر")
        for (c in cases) { val p = QuickParse.parse(c); println("$c  =>  text='${p.text}' due=${p.due} after=${p.after} min=${p.minute} rule=${p.rule}") }
        val a = QuickParse.parse("اتصل بأمي غدًا بعد العصر")
        assertEquals("اتصل بأمي", a.text); assertEquals(LocalDate.now().plusDays(1), a.due); assertEquals(Prayer.ASR, a.after)
        val b = QuickParse.parse("اجتماع الخميس الساعة 5 مساء")
        assertEquals("اجتماع", b.text); assertEquals(17 * 60, b.minute); assertEquals(4, b.due!!.dayOfWeek.value)
        assertEquals("daily", QuickParse.parse("الوتر كل يوم").rule)
        assertEquals("days:1,4", QuickParse.parse("صيام الاثنين والخميس كل أسبوع").rule)
        assertEquals("شراء خبز", QuickParse.parse("شراء خبز").text)
    }
}
