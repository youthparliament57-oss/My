import sys

content = open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt").read()
target = open("evaluate_math.kt").read().strip()
replacement = open("evaluate_math_fixed.kt").read().strip()
if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
