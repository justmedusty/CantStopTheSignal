package cantstopthesignal.helper

fun errorSanitization(URI: String): String {

    if (URI.contains("?error")) {
        val ret: String = URI.replaceAfter("?error", "").replace("?error", "")
        return ret
    } else {
        return URI
    }

}

fun successSanitization(URI: String): String {

    if (URI.contains("?success")) {
        val ret: String = URI.replaceAfter("?success", "").replace("?success", "")
        return ret

    } else {
        return URI
    }

}

fun redirectSanitization(URI: String): String {
    var ret: String = errorSanitization(URI)
    ret = successSanitization(ret)

    return ret
}