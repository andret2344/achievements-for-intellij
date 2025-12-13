package eu.andret.plugin.achievementsforintellij.achievements.entity

/**
 * Read-only domain model for achievements and their step thresholds.
 * Names and descriptions are localized via message bundles using the provided keys.
 */
data class AchievementStep(
    val threshold: Long,
    val titleKey: String? = null,
)
