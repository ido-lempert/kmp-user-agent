package site.lempert.useragent

/**
 * Structured result of parsing a User-Agent string (and, in a future story,
 * input for generating one).
 *
 * Every field is nullable: when a piece of data cannot be determined from the
 * User-Agent string, the field is `null` -- never a sentinel value such as
 * `"unknown"`.
 */
data class UserAgentInfo(
    val browser: Component?,
    val engine: Component?,
    val os: Component?,
    val device: Device?,
)

/**
 * A named thing with an optional version -- used for both [UserAgentInfo.browser]
 * and [UserAgentInfo.engine].
 */
data class Component(
    val name: String?,
    val version: String?,
)

/**
 * A physical device, e.g. a phone or tablet.
 */
data class Device(
    val brand: String?,
    val model: String?,
    val name: String?,
)
