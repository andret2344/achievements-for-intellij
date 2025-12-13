package eu.andret.plugin.achievementsforintellij.achievements.entity

data class AchievementDefinition(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val steps: List<AchievementStep>,
    /**
     * Whether progress towards the next step is gradual and should be displayed as a counter/percentage.
     * If false, the UI should not show partial progress (e.g., for one-shot or max-in-one-action achievements).
     */
    val progressive: Boolean = true,
    /**
     * If true, the achievement should be hidden (name/description masked) until at least the first step is reached.
     */
    val hidden: Boolean = false,
)
