package com.example

import com.example.ocr.SmartReceiptOcr
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun testMultiItemInvoiceSumCalculation() {
        val rawText = """
            SAMPLE BILL
            Zetran Technologies Pvt., Ltd.
            Bill From: Rakesh Enterprises
            
            ITEMS HSN QTY MRP RATE/ITEM DISCOUNT TAX AMOUNT
            Samsung A30 463182 1 ₹17,999 ₹16,999 ₹ 1000 0.00 ₹ 15,999
            Samsung Buds 463125 1 ₹12,999 ₹12,499 ₹ 500 0.00 ₹ 11,999
            Boat Rockers 510 682145 1 ₹2,499 ₹ 500 0.00 ₹ 1,499
        """.trimIndent()

        val lines = rawText.lines()
        val result = SmartReceiptOcr.parseTextToTransaction(rawText, lines)

        // Verifying that it computes the sum of the items (15999 + 11999 + 1499 = 29497)
        assertEquals(29497.0, result.amount, 0.01)
    }

    @Test
    fun testExplicitTotalReceipt() {
        val rawText = """
            D-Mart Supermarket
            Milk: ₹50
            Bread: ₹40
            Total Amount: ₹90.00
        """.trimIndent()

        val lines = rawText.lines()
        val result = SmartReceiptOcr.parseTextToTransaction(rawText, lines)

        assertEquals(90.0, result.amount, 0.01)
    }

    @Test
    fun testOcrConfigManagerLifecycleAndHotFix() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        com.example.ocr.OcrConfigManager.init(context)

        // 1. Check default prompt loaded
        val initialPrompt = com.example.ocr.OcrConfigManager.getSystemPrompt()
        org.junit.Assert.assertTrue("Should contain RECEIPT TOTAL EXTRACTION PROTOCOL", initialPrompt.contains("RECEIPT TOTAL EXTRACTION PROTOCOL"))

        // 2. Queue a pending hot-fix (simulating FCM receipt)
        val hotFixPrompt = "Updated hot-fix prompt for testing spatial rows"
        val isNew = com.example.ocr.OcrConfigManager.setPendingHotFix(context, hotFixPrompt, 2)
        org.junit.Assert.assertTrue("Pending hot fix should be newly queued", isNew)
        org.junit.Assert.assertEquals(hotFixPrompt, com.example.ocr.OcrConfigManager.pendingHotFix.value?.prompt)

        // Verify that prompt is NOT yet applied before user clicks 'Fix Now'
        org.junit.Assert.assertNotEquals(hotFixPrompt, com.example.ocr.OcrConfigManager.getSystemPrompt())

        // 3. User clicks 'Fix Now'
        val applied = com.example.ocr.OcrConfigManager.applyPendingHotFix(context)
        org.junit.Assert.assertTrue("Hot fix should be applied successfully", applied)

        // 4. Verify prompt is now updated in memory and pending is cleared
        org.junit.Assert.assertEquals(hotFixPrompt, com.example.ocr.OcrConfigManager.getSystemPrompt())
        org.junit.Assert.assertNull(com.example.ocr.OcrConfigManager.pendingHotFix.value)
        org.junit.Assert.assertNotNull(com.example.ocr.OcrConfigManager.hotFixTriggerEvent.value)

        // 5. Revert back to default
        com.example.ocr.OcrConfigManager.resetToDefault(context)
        org.junit.Assert.assertTrue(com.example.ocr.OcrConfigManager.getSystemPrompt().contains("RECEIPT TOTAL EXTRACTION PROTOCOL"))
    }

    @Test
    fun testPaymentRouterWithQrShareStrategy() {
        val payload = com.example.payment.UpiPayload(
            rawPayload = "upi://pay?pa=merchant@upi&pn=StoreName&am=150.00",
            pa = "merchant@upi",
            pn = "StoreName",
            am = "150.00"
        )
        val router = com.example.payment.PaymentRouter()
        val context = org.robolectric.RuntimeEnvironment.getApplication()

        // Test forced strategy to MERCHANT_QR_IMAGE_SHARE (the strategy triggered by "Pay with QR Share" button)
        val result = router.routePayment(
            context = context,
            payload = payload,
            amount = 150.0,
            forcedStrategy = com.example.payment.PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE
        )

        org.junit.Assert.assertTrue(
            "Strategy should succeed and launch chooser",
            result is com.example.payment.StrategyResult.Success
        )
        val success = result as com.example.payment.StrategyResult.Success
        org.junit.Assert.assertEquals(com.example.payment.PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE, success.strategy)
    }
}
