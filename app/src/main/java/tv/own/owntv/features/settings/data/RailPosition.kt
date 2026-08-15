package tv.own.owntv.features.settings.data

/**
 * Where the persistent navigation rail docks on screen: the long-standing [LEFT] sidebar, or
 * collapsed into a [TOP] bar. Read by the shell's floating-rail layout (`features/shell/`) to
 * choose which edge the rail attaches to; this file only holds the persisted choice.
 */
enum class RailPosition {
    LEFT,
    TOP;

    companion object {
        val DEFAULT = LEFT
        fun fromName(name: String?): RailPosition = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
