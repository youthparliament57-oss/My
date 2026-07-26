import sys

content = open("app/src/main/java/com/example/brain/memory/MemoryConsolidationWorker.kt").read()

target = """    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface MemoryWorkerEntryPoint {
        fun memoryInterface(): MemoryInterface
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MemoryWorkerEntryPoint::class.java
            )
            val memory = entryPoint.memoryInterface()
            
            // Perform consolidation (Dream Mode)
            memory.consolidateMemories(localLlm = null)"""

replacement = """    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface MemoryWorkerEntryPoint {
        fun memoryInterface(): MemoryInterface
        fun localLlmLayer(): com.example.brain.LocalLlmLayer
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MemoryWorkerEntryPoint::class.java
            )
            val memory = entryPoint.memoryInterface()
            val localLlm = entryPoint.localLlmLayer()
            
            // Perform consolidation (Dream Mode)
            memory.consolidateMemories(localLlm)"""
            
if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/memory/MemoryConsolidationWorker.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
