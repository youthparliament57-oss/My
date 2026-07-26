import sys

content = open("app/src/main/java/com/example/brain/memory/MemoryInterfaceImpl.kt").read()

target = """        // 2. Semantic Similarity Search
        val allNodes = memoryDao.getAllGraphNodes()
        val semanticMatches = allNodes.map { node ->
            val nodeVector = VectorMath.jsonToByteVector(node.quantizedEmbeddingJson)
            val similarity = VectorMath.cosineSimilarity(queryEmbedding, nodeVector)
            node.name to similarity
        }.filter { it.second > 0.4f }
         .sortedByDescending { it.second }
         .map { it.first }

        // 3. Knowledge Graph Traversal
        val graphInsights = mutableListOf<String>()
        val matchedNodes = allNodes.filter { query.contains(it.name, ignoreCase = true) }
        val allEdges = memoryDao.getAllGraphEdges()
        for (node in matchedNodes) {
            val properties = try { JSONObject(node.propertiesJson) } catch (e: Exception) { JSONObject() }
            val edges = allEdges.filter { it.sourceId == node.id || it.targetId == node.id }
            for (edge in edges) {
                val targetId = if (edge.sourceId == node.id) edge.targetId else edge.sourceId
                val targetNode = allNodes.find { it.id == targetId }
                if (targetNode != null) {
                    graphInsights.add("Relation: ${node.name} --(${edge.type})--> ${targetNode.name}")
                }
            }
            if (properties.length() > 0) graphInsights.add("Properties of ${node.name}: $properties")
        }"""

replacement = """        // 2. Semantic Similarity Search
        val matchedNodes = memoryDao.searchGraphNodesByQuery(query)
        val semanticMatches = matchedNodes.map { node ->
            val nodeVector = VectorMath.jsonToByteVector(node.quantizedEmbeddingJson)
            val similarity = VectorMath.cosineSimilarity(queryEmbedding, nodeVector)
            node.name to similarity
        }.filter { it.second > 0.4f }
         .sortedByDescending { it.second }
         .map { it.first }

        // 3. Knowledge Graph Traversal
        val graphInsights = mutableListOf<String>()
        val nodeIds = matchedNodes.map { it.id }
        val allEdges = if (nodeIds.isNotEmpty()) memoryDao.getEdgesForNodes(nodeIds) else emptyList()
        for (node in matchedNodes) {
            val properties = try { JSONObject(node.propertiesJson) } catch (e: Exception) { JSONObject() }
            val edges = allEdges.filter { it.sourceId == node.id || it.targetId == node.id }
            for (edge in edges) {
                val targetId = if (edge.sourceId == node.id) edge.targetId else edge.sourceId
                val targetNode = memoryDao.getGraphNodeById(targetId)
                if (targetNode != null) {
                    graphInsights.add("Relation: ${node.name} --(${edge.type})--> ${targetNode.name}")
                }
            }
            if (properties.length() > 0) graphInsights.add("Properties of ${node.name}: $properties")
        }"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/memory/MemoryInterfaceImpl.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
