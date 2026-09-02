package site.lempert.kmp_user_agent

import site.lempert.useragent.Component
import site.lempert.useragent.UserAgentGenerator
import site.lempert.useragent.UserAgentInfo
import site.lempert.useragent.UserAgentParser

/**
 * Thin JVM console harness proving `:library` works as a consumed dependency
 * on the JVM target -- not a real application. Parses a representative UA
 * string and generates a UA string from structured data.
 */
fun main() {
    val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/128.0.6613.120 Safari/537.36"

    val parsed = UserAgentParser.parse(userAgent)
    println("Parsed \"$userAgent\" ->")
    println("  browser: ${parsed.browser}")
    println("  engine:  ${parsed.engine}")
    println("  os:      ${parsed.os}")
    println("  device:  ${parsed.device}")

    val info = UserAgentInfo(
        browser = Component("Chrome", "128.0"),
        engine = Component("Blink", "128.0"),
        os = Component("Windows", "10"),
        device = null,
    )
    val generated = UserAgentGenerator.generate(info)
    println("Generated from $info ->")
    println("  $generated")
}
