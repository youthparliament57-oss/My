package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeScannerEngine @Inject constructor() {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    data class BarcodeResult(
        val rawValue: String?,
        val type: Int,
        val upiData: UpiData? = null
    )

    data class UpiData(
        val payeeVpa: String,
        val payeeName: String?,
        val amount: String?,
        val transactionId: String?
    )

    suspend fun scanBarcode(bitmap: Bitmap): List<BarcodeResult> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val barcodes = scanner.process(image).await()
            barcodes.map { barcode ->
                val raw = barcode.rawValue
                val upiData = if (raw?.startsWith("upi://pay") == true) parseUpi(raw) else null
                BarcodeResult(raw, barcode.valueType, upiData)
            }
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Scan failed", e)
            emptyList()
        }
    }

    private fun parseUpi(uri: String): UpiData? {
        return try {
            // upi://pay?pa=xxx@bank&pn=Name&am=Amount&tr=TransactionId
            val params = uri.split("?").getOrNull(1)?.split("&")?.associate {
                val parts = it.split("=")
                parts.getOrNull(0) to parts.getOrNull(1)
            } ?: emptyMap()

            val pa = params["pa"] ?: return null
            UpiData(
                payeeVpa = pa,
                payeeName = params["pn"],
                amount = params["am"],
                transactionId = params["tr"]
            )
        } catch (e: Exception) {
            null
        }
    }
}
