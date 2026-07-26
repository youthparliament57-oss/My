package com.example.vision

import android.util.Log
import com.example.brain.memory.MemoryInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphRagIntegrator @Inject constructor(
    private val memory: MemoryInterface,
    private val omniSlm: OmniSlmRuntime
) {

    /**
     * Strategy 6.1: Graph-RAG integration.
     * Anchors visual observations into the Knowledge Graph as episodic nodes
     * linked to semantic entities.
     */
    suspend fun integrateVisionResult(result: VisionFacade.VisionSceneResult) {
        val timestamp = System.currentTimeMillis()
        
        // 1. Process Objects
        result.objects.forEach { obj ->
            val nodeId = "obs_${obj.label.lowercase()}_$timestamp"
            val embedding = omniSlm.generateEmbedding(obj.label)
            
            memory.addGraphNode(
                id = nodeId,
                type = "VISUAL_OBSERVATION",
                name = obj.label,
                properties = mapOf(
                    "confidence" to obj.confidence.toString(),
                    "source" to "NOUS_EYES"
                ),
                embedding = embedding
            )
            
            // Link to the generic semantic concept of the object if it exists
            memory.addGraphEdge(
                id = "edge_$timestamp",
                sourceId = "node_system", // Root node or context node
                targetId = nodeId,
                type = "OBSERVED",
                weight = obj.confidence
            )
        }

        // 2. Process Barcodes/UPI
        result.barcodes.forEach { barcode ->
            if (barcode.upiData != null) {
                val upi = barcode.upiData
                Log.i("GraphRAG", "Linking UPI Intent: ${upi.payeeVpa} to Knowledge Graph.")
                
                memory.addGraphNode(
                    id = "upi_${upi.payeeVpa}",
                    type = "PAYMENT_ENDPOINT",
                    name = upi.payeeName ?: upi.payeeVpa,
                    properties = mapOf("vpa" to upi.payeeVpa),
                    embedding = omniSlm.generateEmbedding("Payment to ${upi.payeeName}")
                )
            }
        }
    }
}
