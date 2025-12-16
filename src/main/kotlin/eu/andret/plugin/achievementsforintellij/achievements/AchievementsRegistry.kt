package eu.andret.plugin.achievementsforintellij.achievements

import eu.andret.plugin.achievementsforintellij.achievements.entity.AchievementDefinition
import eu.andret.plugin.achievementsforintellij.achievements.entity.AchievementStep

/**
 * Simple in-code registry of achievements. Can be extended later to load from resources.
 */
object AchievementsRegistry {
    private val map: MutableMap<String, AchievementDefinition> = LinkedHashMap()

    init {
        register(
            AchievementDefinition(
                id = AchievementIds.EASTER_EGG,
                nameKey = "achievement.easter-egg.name",
                descriptionKey = "achievement.easter-egg.description",
                steps = listOf(
                    AchievementStep(threshold = 1)
                ),
                progressive = false,
                hidden = true
            )
        )

        register(
            AchievementDefinition(
                id = AchievementIds.OPENED_LONG_FILE_1000,
                nameKey = "achievement.long-file.name",
                descriptionKey = "achievement.long-file.description",
                steps = listOf(
                    AchievementStep(threshold = 1)
                ),
                progressive = false
            )
        )

        register(
            AchievementDefinition(
                id = AchievementIds.SUPER_EASTER_EGG,
                nameKey = "achievement.super-easter-egg.name",
                descriptionKey = "achievement.super-easter-egg.description",
                steps = listOf(
                    AchievementStep(threshold = 1)
                ),
                progressive = false,
                hidden = true
            )
        )

        register(
            AchievementDefinition(
                id = AchievementIds.OPEN_TABS_CONCURRENT,
                nameKey = "achievement.tabs-opened-bulk.name",
                descriptionKey = "achievement.tabs-opened-bulk.description",
                steps = listOf(
                    AchievementStep(10),
                    AchievementStep(25),
                    AchievementStep(50),
                    AchievementStep(100),
                ),
                progressive = false
            )
        )

        register(
            AchievementDefinition(
                id = AchievementIds.CLOSED_TABS_BULK,
                nameKey = "achievement.tabs-closed-bulk.name",
                descriptionKey = "achievement.tabs-closed-bulk.description",
                steps = listOf(
                    AchievementStep(10),
                    AchievementStep(25),
                    AchievementStep(50),
                    AchievementStep(100),
                ),
                progressive = false
            )
        )

        register(
            AchievementDefinition(
                id = AchievementIds.CODE_NECROMANCER,
                nameKey = "achievement.code-necromancer.name",
                descriptionKey = "achievement.code-necromancer.description",
                steps = listOf(
                    AchievementStep(threshold = 1)
                ),
                progressive = false,
                hidden = false
            )
        )

        register(
            AchievementDefinition(
                id = AchievementIds.FILE_VOYAGER,
                nameKey = "achievement.file-voyager.name",
                descriptionKey = "achievement.file-voyager.description",
                steps = listOf(
                    AchievementStep(5),
                    AchievementStep(10),
                    AchievementStep(20),
                    AchievementStep(50),
                    AchievementStep(100),
                ),
                progressive = true
            )
        )
    }

    fun register(definition: AchievementDefinition) {
        map[definition.id] = definition
    }

    fun get(id: String): AchievementDefinition? = map[id]

    fun all(): Collection<AchievementDefinition> = map.values
}
