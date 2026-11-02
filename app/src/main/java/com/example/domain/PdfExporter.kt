package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.AcademicNoteEntity
import com.example.data.TopicChecklistEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportEngine {

    /**
     * Generates a multi-page PDF document summarizing notes, topics, or revision guides,
     * and returns the file path.
     */
    fun exportToPdf(
        context: Context,
        title: String,
        subject: String,
        notes: List<AcademicNoteEntity>,
        topics: List<TopicChecklistEntity>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 points (72 dpi)
            val pageHeight = 842
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(10, 25, 47)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(0, 150, 180)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(20, 35, 60)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(40, 45, 55)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val metaPaint = Paint().apply {
                color = Color.rgb(120, 130, 145)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            var y = 50f
            val xMargin = 40f
            val contentWidth = pageWidth - 80f

            fun checkNewPage(neededSpace: Float) {
                if (y + neededSpace > pageHeight - 50f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
            }

            // Draw Header Banner
            canvas.drawText("JAMESON ACADEMIC REVISION GUIDE", xMargin, y, subtitlePaint)
            y += 24f
            canvas.drawText(title.uppercase(Locale.getDefault()), xMargin, y, titlePaint)
            y += 18f
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            canvas.drawText("Subject: $subject | Generated: ${dateFormat.format(Date())}", xMargin, y, metaPaint)
            y += 25f

            // Separator Line
            val linePaint = Paint().apply {
                color = Color.rgb(200, 215, 230)
                strokeWidth = 1.5f
            }
            canvas.drawLine(xMargin, y, pageWidth - xMargin, y, linePaint)
            y += 25f

            // Section 1: Topics & Mastery
            if (topics.isNotEmpty()) {
                checkNewPage(40f)
                canvas.drawText("Syllabus Mastery Checklist", xMargin, y, headerPaint)
                y += 18f

                for (topic in topics) {
                    checkNewPage(24f)
                    val statusText = if (topic.isCompleted) "[COMPLETED]" else "[PENDING]"
                    val text = "• ${topic.topicName} — Mastery: ${topic.masteryScore}% $statusText"
                    canvas.drawText(text, xMargin + 10f, y, bodyPaint)
                    y += 16f
                }
                y += 15f
            }

            // Section 2: Notes & Summary Content
            if (notes.isNotEmpty()) {
                checkNewPage(40f)
                canvas.drawText("Summary Notes & Study Cards", xMargin, y, headerPaint)
                y += 20f

                for (note in notes) {
                    checkNewPage(35f)
                    canvas.drawText(note.title, xMargin + 8f, y, headerPaint)
                    y += 16f
                    canvas.drawText("Folder: ${note.folder} | Tags: ${note.tags}", xMargin + 8f, y, metaPaint)
                    y += 16f

                    // Wrap note content text
                    val lines = note.content.split("\n")
                    for (line in lines) {
                        var remaining = line
                        while (remaining.isNotEmpty()) {
                            checkNewPage(16f)
                            val count = bodyPaint.breakText(remaining, true, contentWidth - 16f, null)
                            val segment = remaining.substring(0, count)
                            canvas.drawText(segment, xMargin + 8f, y, bodyPaint)
                            remaining = remaining.substring(count)
                            y += 14f
                        }
                    }
                    y += 18f
                }
            }

            pdfDocument.finishPage(page)

            // Save PDF to cache/files dir
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val fileName = "Jameson_${subject.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(exportDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a clean Markdown export string from notes and topics.
     */
    fun exportToMarkdown(
        title: String,
        subject: String,
        notes: List<AcademicNoteEntity>,
        topics: List<TopicChecklistEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("# $title\n\n")
        sb.append("**Subject:** $subject  \n")
        sb.append("**Generated:** ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}  \n\n")
        sb.append("---\n\n")

        if (topics.isNotEmpty()) {
            sb.append("## Syllabus & Topic Mastery\n\n")
            for (t in topics) {
                val mark = if (t.isCompleted) "[x]" else "[ ]"
                sb.append("- $mark **${t.topicName}** (Mastery: ${t.masteryScore}%, Folder: `${t.folder}`)\n")
            }
            sb.append("\n---\n\n")
        }

        if (notes.isNotEmpty()) {
            sb.append("## Study Notes\n\n")
            for (n in notes) {
                sb.append("### ${n.title}\n")
                sb.append("*Folder: ${n.folder} | Tags: ${n.tags}*\n\n")
                sb.append("${n.content}\n\n")
            }
        }

        return sb.toString()
    }

    /**
     * Triggers the Android system Sharesheet to share the generated PDF or text.
     */
    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share via...").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
