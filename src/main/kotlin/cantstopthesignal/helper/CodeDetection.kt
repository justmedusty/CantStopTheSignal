package cantstopthesignal.helper

fun isThisCode(content: String): Boolean {
    return content.contains("{")
            || content.contains("();")
            || content.contains("->")
            || content.contains("    ")
            || content.contains("[")
            || content.contains("]")
}