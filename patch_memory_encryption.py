import sys

content = open("app/src/main/java/com/example/brain/memory/MemoryEncryption.kt").read()

target1 = """        } catch (e: Exception) {
            plainText // Fallback to plain text in extremely rare Keystore error states
        }"""
replacement1 = """        } catch (e: Exception) {
            throw IllegalStateException("Memory encryption failed — data not stored", e)
        }"""

target2 = """        } catch (e: Exception) {
            encryptedText // Return unmodified on decrypt failures
        }"""
replacement2 = """        } catch (e: Exception) {
            throw IllegalStateException("Memory decryption failed — corrupted data", e)
        }"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)

open("app/src/main/java/com/example/brain/memory/MemoryEncryption.kt", "w").write(content)
