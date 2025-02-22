package com.ion606.installer.util

class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String = ""
) : Comparable<SemanticVersion> {
    companion object {
        fun parse(version: String): SemanticVersion {
            val cleanVersion = version.replace("^v".toRegex(), "")
            val parts = cleanVersion.split("-", limit = 2)
            val numbers = parts[0].split(".").map { it.toIntOrNull() ?: 0 }
            return SemanticVersion(
                major = numbers.getOrElse(0) { 0 },
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 },
                suffix = parts.getOrElse(1) { "" }
            )
        }
    }

    override fun compareTo(other: SemanticVersion): Int {
        return compareValuesBy(this, other,
            { it.major }, { it.minor }, { it.patch }
        )
    }
}