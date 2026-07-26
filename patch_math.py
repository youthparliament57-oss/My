import sys

content = open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt").read()
target = """        private fun evaluateSimpleMath(expr: String): Double {
            // Evaluates standard linear operators (+, -, *, /) safely
            var numStr1 = ""
            var op = ' '
            var numStr2 = ""
            for (char in expr) {
                if (char == '+' || char == '-' || char == '*' || char == '/') {
                    op = char
                } else {
                    if (op == ' ') numStr1 += char else numStr2 += char
                }
            }
            if (numStr1.isEmpty() || numStr2.isEmpty() || op == ' ') return expr.toDouble()
            
            return when(op) {
                '+' -> numStr1.toDouble() + numStr2.toDouble()
                '-' -> numStr1.toDouble() - numStr2.toDouble()
                '*' -> numStr1.toDouble() * numStr2.toDouble()
                '/' -> numStr1.toDouble() / numStr2.toDouble()
                else -> expr.toDouble()
            }
        }"""
replacement = open("evaluate_math.kt").read().strip()
if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/brain/AgenticOrchestrator.kt", "w").write(content)
    print("Success")
else:
    print("Target not found")
