package tv.simplehome.launcher

/**
 * Idle-mode logic (pure): after [timeoutMs] without key input the UI fades away; the first key afterwards only wakes
 * the UI and is consumed so it does not trigger an action the user cannot see.
 */
class IdleTracker(private val timeoutMs: Long = DEFAULT_TIMEOUT_MS, var enabled: Boolean = true, startMs: Long = 0L) {
    var lastInputMs: Long = startMs
        private set
    var idle: Boolean = false
        private set

    /** True when this key must be swallowed because it only woke the UI. */
    fun onKey(nowMs: Long): Boolean {
        lastInputMs = nowMs
        if (idle) { idle = false; return true }
        return false
    }

    /** Re-evaluates idleness; returns true exactly when the UI should start fading out now. */
    fun tick(nowMs: Long): Boolean {
        if (!enabled || idle) return false
        if (nowMs - lastInputMs >= timeoutMs) { idle = true; return true }
        return false
    }

    /** Milliseconds until the next possible idle transition (for scheduling), or null when it can't happen. */
    fun untilIdleMs(nowMs: Long): Long? = if (!enabled || idle) null else (lastInputMs + timeoutMs - nowMs).coerceAtLeast(0L)

    /** Used when something other than a key (a panel, All apps) shows: restart the countdown without waking. */
    fun touch(nowMs: Long) { lastInputMs = nowMs }

    fun reset(nowMs: Long) { idle = false; lastInputMs = nowMs }

    companion object { const val DEFAULT_TIMEOUT_MS = 45_000L }
}
