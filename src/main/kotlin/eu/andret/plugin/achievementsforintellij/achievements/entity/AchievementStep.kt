package eu.andret.plugin.achievementsforintellij.achievements.entity

/**
 * A step of an achievement, reached once the achievement's counter is at least [threshold].
 */
data class AchievementStep(
    val threshold: Long,
)
