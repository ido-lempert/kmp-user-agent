package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Structured result of parsing a User-Agent string (and, in a future story,
 * input for generating one).
 *
 * Every field is nullable: when a piece of data cannot be determined from the
 * User-Agent string, the field is `null` -- never a sentinel value such as
 * `"unknown"`.
 *
 * `@JsExport` here (and on [Component]/[Device] below) only affects the JS
 * target's compiled output -- it makes these existing types visible to JS/TS
 * consumers (per this story's per-target sample apps); it is a no-op on
 * every other target and changes no parsing/generation behavior.
 */
@JsExport
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
@JsExport
data class Component(
    val name: String?,
    val version: String?,
)

/**
 * A physical device, e.g. a phone or tablet.
 */
@JsExport
data class Device(
    val brand: String?,
    val model: String?,
    val name: String?,
)
