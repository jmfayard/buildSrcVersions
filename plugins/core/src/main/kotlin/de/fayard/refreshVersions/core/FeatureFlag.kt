package de.fayard.refreshVersions.core

import de.fayard.refreshVersions.core.internal.InternalRefreshVersionsApi

/**
 * Since a bug in refreshVersions can break people's builds,
 * we put risky changes and new features behind feature flags.
 *
 * If it starts disabled, it allows us to test it without affecting people that don't opt-in,
 * and in all cases, it allows people to disable it without downgrading if problems are caused.
 *
 * After several releases where the change didn't cause any issue, we might deprecate the flag,
 * ignore it, and remove the dead code.
 *
 * Users can temporarily control the flags from command-line as such:
 *
 * ```bash
 * ./gradle refreshVersions --enable LIBS
 * ./gradle refreshVersions --disable GRADLE_UPDATES
 * ```
 *
 * Or they can permanently control the flags from the Gradle Settings file:
 *
 * ```kotlin
 * refreshVersions {
 *      featureFlags {
 *          enable(LIBS)
 *          disable(GRADLE_UPDATES)
 *      }
 * }
 * ```
 */
enum class FeatureFlag(private val enabledByDefault: Boolean?) {

    // NEVER REMOVE A FLAG HERE since it would break projects using it on upgrade.
    // Instead, mark it as deprecated, like this: @Deprecated("your message here")

    GRADLE_UPDATES(enabledByDefault = true),
    LIBS(enabledByDefault = false),
    NPM_IMPLICIT_RANGE(enabledByDefault = false)
    ;

    companion object {
        /**
         * Where we store the settings coming from the command-line or the Settings file
         */
        @InternalRefreshVersionsApi
        val userSettings: MutableMap<FeatureFlag, Boolean> = mutableMapOf()
    }

    /**
     * Whether the flag is enabled once the user settings are set
     * Intended usage:
     * `if (GRADLE_UPDATES.isEnabled) lookupAvailableGradleVersions() else emptyList()`
     */
    internal val isEnabled: Boolean
        get() = when (enabledByDefault) {
            false -> userSettings[this] == true
            true -> userSettings[this] != false
            null -> false
        }
}
