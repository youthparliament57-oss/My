import sys

content = open("app/src/main/java/com/example/brain/memory/MemoryDao.kt").read()
target = """    @Query("SELECT * FROM knowledge_graph_nodes")
    suspend fun getAllGraphNodes(): List<KnowledgeGraphNodeEntity>"""
replacement = """    @Query("SELECT * FROM knowledge_graph_nodes")
    suspend fun getAllGraphNodes(): List<KnowledgeGraphNodeEntity>

    @Query("SELECT * FROM knowledge_graph_nodes WHERE name LIKE '%' || :query || '%'")
    suspend fun searchGraphNodesByQuery(query: String): List<KnowledgeGraphNodeEntity>"""

if target in content:
    content = content.replace(target, replacement)
    
target2 = """    @Query("SELECT * FROM knowledge_graph_edges")
    suspend fun getAllGraphEdges(): List<KnowledgeGraphEdgeEntity>"""
replacement2 = """    @Query("SELECT * FROM knowledge_graph_edges")
    suspend fun getAllGraphEdges(): List<KnowledgeGraphEdgeEntity>

    @Query("SELECT * FROM knowledge_graph_edges WHERE sourceId IN (:nodeIds) OR targetId IN (:nodeIds)")
    suspend fun getEdgesForNodes(nodeIds: List<String>): List<KnowledgeGraphEdgeEntity>"""

if target2 in content:
    content = content.replace(target2, replacement2)
    
open("app/src/main/java/com/example/brain/memory/MemoryDao.kt", "w").write(content)
