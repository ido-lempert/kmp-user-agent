package site.lempert.useragent

import kotlin.js.JsExport

/**
 * Structured result of parsing a User-Agent string, and input for generating
 * one.
 *
 * Every field is nullable/empty by default: when a piece of data cannot be
 * determined from the User-Agent string (or a given [UserAgentTypePack]
 * simply wasn't asked to detect it), the field is `null` -- never a sentinel
 * value such as `"unknown"`.
 *
 * [browser]/[engine]/[os]/[device] are populated by the built-in
 * [UserAgentBrowserTypes]/[UserAgentEngineTypes]/[UserAgentOsTypes]/
 * [UserAgentDeviceTypes] packs (or [UserAgentAllTypes], which covers all
 * four). [bot]/[aiAgent] are reserved for a deferred follow-up story's
 * `UserAgentBotTypes`/`UserAgentAIAgentTypes` packs -- present now so that
 * story doesn't need another breaking data-model change, but always `null`
 * until those packs exist. [custom] is the extension point a custom
 * [UserAgentTypePack] can use to contribute data that doesn't fit any of the
 * named fields above, keyed by whatever id the pack's author chooses.
 *
 * `@JsExport` here (and on [Component]/[Device] below) only affects the JS
 * target's compiled output -- it makes these existing types visible to JS/TS
 * consumers; it is a no-op on every other target and changes no
 * parsing/generation behavior.
 */
@JsExport
data class UserAgentInfo(
    val browser: Component? = null,
    val engine: Component? = null,
    val os: Component? = null,
    val device: Device? = null,
    val bot: Component? = null,
    val aiAgent: Component? = null,
    val custom: Map<String, Component> = emptyMap(),
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
