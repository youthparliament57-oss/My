import sys

db_path = "app/src/main/java/com/example/data/local/NousDatabase.kt"
db_content = open(db_path).read()
db_content = db_content.replace("@androidx.room.TypeConverters(com.example.brain.memory.MemoryTypeConverters::class)\n", "")
open(db_path, "w").write(db_content)

import os
if os.path.exists("app/src/main/java/com/example/brain/memory/MemoryTypeConverters.kt"):
    os.remove("app/src/main/java/com/example/brain/memory/MemoryTypeConverters.kt")
print("Reverted TypeConverters")
