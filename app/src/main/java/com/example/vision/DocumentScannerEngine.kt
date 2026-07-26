package com.example.vision

import android.graphics.Bitmap
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentScannerEngine @Inject constructor(
    private val ocrEngine: OcrEngine
) {

    /**
     * Strategy 6.2: Document Scanning.
     * Beyond simple OCR, this engine attempts to reconstruct document structure
     * (headers, paragraphs, tables) for higher-fidelity semantic indexing.
     */
    suspend fun scanDocument(bitmap: Bitmap): ScannedDocument? {
        val rawText = ocrEngine.recognizeText(bitmap) ?: return null
        
        Log.i("DocumentScanner", "Processing raw OCR into structured document.")
        
        val lines = rawText.split("\n")
        val blocks = mutableListOf<DocumentBlock>()
        
        var currentBlockText = StringBuilder()
        var currentType = BlockType.PARAGRAPH

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (currentBlockText.isNotEmpty()) {
                    blocks.add(DocumentBlock(currentType, currentBlockText.toString()))
                    currentBlockText = StringBuilder()
                }
                continue
            }

            // Heuristics for headers (short lines, all caps, or specific keywords)
            val isHeader = trimmed.length < 50 && (trimmed == trimmed.uppercase() || trimmed.contains("Title") || trimmed.contains("Subject"))
            
            if (isHeader) {
                if (currentBlockText.isNotEmpty()) {
                    blocks.add(DocumentBlock(currentType, currentBlockText.toString()))
                    currentBlockText = StringBuilder()
                }
                blocks.add(DocumentBlock(BlockType.HEADER, trimmed))
            } else {
                currentType = BlockType.PARAGRAPH
                currentBlockText.append(trimmed).append(" ")
            }
        }
        
        if (currentBlockText.isNotEmpty()) {
            blocks.add(DocumentBlock(currentType, currentBlockText.toString()))
        }

        return ScannedDocument(
            title = blocks.firstOrNull { it.type == BlockType.HEADER }?.content ?: "Untitled Document",
            blocks = blocks
        )
    }

    data class ScannedDocument(val title: String, val blocks: List<DocumentBlock>)
    data class DocumentBlock(val type: BlockType, val content: String)
    enum class BlockType { HEADER, PARAGRAPH, LIST_ITEM, TABLE_ROW }
}
