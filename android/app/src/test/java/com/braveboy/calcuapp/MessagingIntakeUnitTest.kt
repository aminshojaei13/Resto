package com.braveboy.calcuapp

import com.braveboy.calcuapp.data.intake.DeterministicMessageParser
import com.braveboy.calcuapp.data.intake.PendingImportManager
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MessagingIntakeUnitTest {

    private val sampleCatalog = listOf(
        Product(
            id = "prod_1",
            orgId = "org_test",
            sku = "SKU-ESP-01",
            barcode = "12345678",
            name = "قهوه اسپرسو",
            description = "قهوه تازه رست شده",
            price = 95000.0,
            costPrice = 60000.0,
            category = "نوشیدنی",
            unit = "بسته",
            imageUrl = ""
        ),
        Product(
            id = "prod_2",
            orgId = "org_test",
            sku = "SKU-TEA-02",
            barcode = "87654321",
            name = "چای لاهیجان",
            description = "چای ممتاز بهاره",
            price = 120000.0,
            costPrice = 80000.0,
            category = "نوشیدنی",
            unit = "بسته",
            imageUrl = ""
        ),
        Product(
            id = "prod_3",
            orgId = "org_test",
            sku = "APX-LAP-001",
            barcode = "99887766",
            name = "Apex Laptop Stand",
            description = "پایه لپ‌تاپ آلومینیومی",
            price = 450000.0,
            costPrice = 300000.0,
            category = "لوازم جانبی",
            unit = "عدد",
            imageUrl = ""
        )
    )

    @Test
    fun testPersianAndArabicNumeralNormalization() {
        val persianNumerals = "۱۲۳۴۵۶۷۸۹۰"
        val arabicNumerals = "١٢٣٤٥٦٧٨٩٠"

        assertEquals("1234567890", DeterministicMessageParser.normalizeDigitsAndChars(persianNumerals))
        assertEquals("1234567890", DeterministicMessageParser.normalizeDigitsAndChars(arabicNumerals))
    }

    @Test
    fun testExtractCustomerDetailsFromPersianMessage() {
        val rawMessage = """
            سلام وقت بخیر
            نام: محمد امینی
            شماره تماس: ۰۹۱۲۳۴۵۶۷۸۹
            آدرس: تهران، میدان ونک، پلاک ۲۰
            سفارش:
            ۲ عدد قهوه اسپرسو
        """.trimIndent()

        val draft = DeterministicMessageParser.parse(
            rawText = rawMessage,
            source = "INSTAGRAM",
            catalog = sampleCatalog
        )

        assertEquals("محمد امینی", draft.customer.name)
        assertEquals("09123456789", draft.customer.phone)
        assertNotNull(draft.customer.address)
        assertTrue(draft.customer.address!!.contains("میدان ونک"))
    }

    @Test
    fun testProductMatchingWithCatalog() {
        val rawMessage = """
            سفارش من:
            ۲ بسته قهوه اسپرسو
            ۱ عدد چای لاهیجان
            ممنون
        """.trimIndent()

        val draft = DeterministicMessageParser.parse(
            rawText = rawMessage,
            source = "WHATSAPP",
            catalog = sampleCatalog
        )

        assertEquals(2, draft.items.size)

        val espresso = draft.items.find { it.productName.contains("اسپرسو") }
        assertNotNull(espresso)
        assertEquals("prod_1", espresso!!.productId)
        assertEquals(2, espresso.quantity)
        assertEquals(95000.0, espresso.catalogPrice, 0.01)
        assertTrue(espresso.isMatched)

        val tea = draft.items.find { it.productName.contains("چای") }
        assertNotNull(tea)
        assertEquals("prod_2", tea!!.productId)
        assertEquals(1, tea.quantity)
        assertEquals(120000.0, tea.catalogPrice, 0.01)
        assertTrue(tea.isMatched)

        assertFalse(draft.hasUnmatchedItems)
    }

    @Test
    fun testUnmatchedItemsDetection() {
        val rawMessage = """
            سفارش:
            ۳ بسته شکلات تلخ بلژیکی
        """.trimIndent()

        val draft = DeterministicMessageParser.parse(
            rawText = rawMessage,
            source = "TELEGRAM",
            catalog = sampleCatalog
        )

        assertEquals(1, draft.items.size)
        val item = draft.items.first()
        assertFalse(item.isMatched)
        assertTrue(draft.hasUnmatchedItems)
        assertNotNull(item.ambiguityReason)
    }

    @Test
    fun testDraftFinancialCalculationsWithTax() {
        val rawMessage = """
            ۲ بسته قهوه اسپرسو
            ۱ بسته چای لاهیجان
        """.trimIndent()

        // 2 * 95000 = 190000
        // 1 * 120000 = 120000
        // Subtotal = 310000
        // Tax 10% = 31000
        // Total = 341000

        val draft = DeterministicMessageParser.parse(
            rawText = rawMessage,
            source = "MANUAL_PASTE",
            catalog = sampleCatalog,
            taxRate = 10.0
        )

        assertEquals(310000.0, draft.subtotal, 0.01)
        assertEquals(31000.0, draft.taxAmount, 0.01)
        assertEquals(341000.0, draft.grandTotal, 0.01)
    }

    @Test
    fun testPendingImportManagerSanitizationAndSourceDetection() {
        val textWithControlChars = "Instagram order from user @tester: \u0000سلام کالا می‌خوام https://instagram.com/p/123"
        PendingImportManager.setPendingImport(textWithControlChars, "share_intent")

        val pending = PendingImportManager.peekPendingImport()
        assertNotNull(pending)
        assertFalse(pending!!.rawText.contains("\u0000"))
        assertEquals("INSTAGRAM", pending.source)

        val consumed = PendingImportManager.consumePendingImport()
        assertEquals(pending, consumed)
        org.junit.Assert.assertNull(PendingImportManager.peekPendingImport())
    }

    @Test
    fun testPendingImportManagerCapsLengthTo8000Chars() {
        val longText = "A".repeat(12000)
        PendingImportManager.setPendingImport(longText, "manual")

        val pending = PendingImportManager.consumePendingImport()
        assertNotNull(pending)
        assertEquals(8000, pending!!.rawText.length)
    }

    @Test
    fun testIdempotencyKeyUniqueness() {
        val key1 = "msg-ord-${UUID.randomUUID()}"
        val key2 = "msg-ord-${UUID.randomUUID()}"

        assertFalse(key1 == key2)
        assertTrue(key1.startsWith("msg-ord-"))
    }
}
