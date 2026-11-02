package com.braveboy.resto.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReportExportManager {

    /**
     * Exports CSV data with UTF-8 BOM so Persian characters render properly in MS Excel.
     */
    fun exportToCsv(
        context: Context,
        fileName: String,
        csvContent: String
    ): File? {
        return try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(downloadsDir, if (fileName.endsWith(".csv")) fileName else "$fileName.csv")

            FileOutputStream(file).use { out ->
                // Write UTF-8 Byte Order Mark (BOM) for Excel Persian text compatibility
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a native PDF document with header, summary metrics, and data table.
     */
    fun exportToPdf(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        metricsSummary: List<Pair<String, String>>,
        tableHeaders: List<String>,
        tableRows: List<List<String>>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
            }

            // Header Background
            paint.color = Color.parseColor("#1B3B2B") // Dark Forest Green
            canvas.drawRect(0f, 0f, 595f, 70f, paint)

            // Header Title
            textPaint.color = Color.WHITE
            textPaint.textSize = 18f
            textPaint.isFakeBoldText = true
            canvas.drawText("Resto Business OS - $title", 20f, 40f, textPaint)

            // Subtitle
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = false
            canvas.drawText(subtitle, 20f, 58f, textPaint)

            var currentY = 95f

            // Summary Metrics Cards Section
            if (metricsSummary.isNotEmpty()) {
                paint.color = Color.parseColor("#F4F6F4")
                canvas.drawRect(20f, currentY, 575f, currentY + 50f, paint)

                textPaint.color = Color.parseColor("#1B3B2B")
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = true

                var startX = 30f
                metricsSummary.take(4).forEach { (key, value) ->
                    canvas.drawText("$key:", startX, currentY + 20f, textPaint)
                    textPaint.textSize = 12f
                    canvas.drawText(value, startX, currentY + 38f, textPaint)
                    textPaint.textSize = 10f
                    startX += 135f
                }
                currentY += 70f
            }

            // Table Headers
            if (tableHeaders.isNotEmpty()) {
                paint.color = Color.parseColor("#E0E6E1")
                canvas.drawRect(20f, currentY, 575f, currentY + 25f, paint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = true

                val colWidth = 555f / tableHeaders.size
                tableHeaders.forEachIndexed { idx, header ->
                    canvas.drawText(header, 25f + (idx * colWidth), currentY + 17f, textPaint)
                }
                currentY += 30f

                // Table Rows
                textPaint.isFakeBoldText = false
                textPaint.textSize = 9f

                tableRows.take(35).forEachIndexed { rowIndex, row ->
                    if (rowIndex % 2 == 1) {
                        paint.color = Color.parseColor("#F8F9F8")
                        canvas.drawRect(20f, currentY - 12f, 575f, currentY + 12f, paint)
                    }

                    row.forEachIndexed { colIndex, cellText ->
                        val text = if (cellText.length > 25) cellText.take(22) + "..." else cellText
                        canvas.drawText(text, 25f + (colIndex * colWidth), currentY, textPaint)
                    }
                    currentY += 20f

                    if (currentY > 800f) return@forEachIndexed
                }
            }

            pdfDocument.finishPage(page)

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(downloadsDir, if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf")

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share exported file via Intent.
     */
    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "اشتراک‌گذاری گزارش")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
