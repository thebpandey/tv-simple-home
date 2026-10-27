package tv.simplehome.launcher

/** Counts the lines [text] wraps to at [sp] in a column [widthDp] wide. The app measures with StaticLayout, the tests with a glyph-width model. */
fun interface LineCounter { fun lines(text: String, sp: Float, widthDp: Float): Int }

/**
 * Fits a quote into the centre: 23sp, then 21, then 19.5sp, each limited to 3 lines; if even 19.5sp needs more, 4 lines at 19.5sp.
 * The author line is never part of the squeeze: it is always shown whole under the quote, so the centre is sized from the result.
 */
object QuoteFit {
    val SIZES_SP = floatArrayOf(23f, 21f, 19.5f)
    const val LINES_AT_SIZE = 3
    const val LINES_AT_SMALLEST = 4
    const val MAX_WIDTH_FRACTION = 0.64f

    data class Result(val sp: Float, val maxLines: Int, val lines: Int)

    fun fit(text: String, widthDp: Float, counter: LineCounter): Result {
        for (sp in SIZES_SP) {
            val n = counter.lines(text, sp, widthDp)
            if (n <= LINES_AT_SIZE) return Result(sp, LINES_AT_SIZE, n)
        }
        val sp = SIZES_SP.last()
        val n = counter.lines(text, sp, widthDp)
        // Beyond 4 lines (not expected: quotes are capped at 160 characters) the quote still shows whole; the centre grows with it.
        return Result(sp, maxOf(LINES_AT_SMALLEST, n), n)
    }

    /** Height of one quote line: Inter's line height (1.21 em) times the 1.3 line-spacing multiplier, with a little spare. */
    fun lineHeightDp(sp: Float): Float = sp * 1.21f * 1.3f * 1.02f

    private const val AUTHOR_SP = 15f
    private const val AUTHOR_TOP_DP = 6f
    private const val BLOCK_PAD_DP = 12f // 2dp above the quote, 10dp below the author

    /** The whole quote block: the quote's lines, the 15sp author line below it and the block's padding. */
    fun blockHeightDp(lines: Int, sp: Float): Float = lines * lineHeightDp(sp) + AUTHOR_TOP_DP + AUTHOR_SP * 1.21f + BLOCK_PAD_DP

    /** The tallest block the fitting can produce: 4 lines at 19.5sp. */
    fun maxBlockDp(): Float = blockHeightDp(LINES_AT_SMALLEST, SIZES_SP.last())
}
