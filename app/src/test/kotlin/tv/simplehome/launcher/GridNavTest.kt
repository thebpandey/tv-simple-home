package tv.simplehome.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class GridNavTest {
    private val cols = 6
    private fun go(i: Int, k: GridKey, n: Int = 20) = GridNav.next(i, k, cols, n)

    @Test
    fun `left and right stop at the ends of a row instead of wrapping to the neighbouring row`() {
        assertEquals(6, go(6, GridKey.LEFT))
        assertEquals(5, go(5, GridKey.RIGHT))
        assertEquals(7, go(6, GridKey.RIGHT))
    }

    @Test
    fun `right stops on the last cell of a partial final row`() {
        assertEquals(19, go(19, GridKey.RIGHT)) // 20 apps: last row holds 18,19
    }

    @Test
    fun `up and down keep the column`() {
        assertEquals(9, go(3, GridKey.DOWN))
        assertEquals(3, go(9, GridKey.UP))
    }

    @Test
    fun `down into a shorter last row lands on its nearest column, the last cell`() {
        assertEquals(19, go(17, GridKey.DOWN)) // col 5 of row 2 -> last row has only cols 0..1
    }

    @Test
    fun `up on the first row and down on the last row stay put`() {
        assertEquals(2, go(2, GridKey.UP))
        assertEquals(19, go(19, GridKey.DOWN))
        assertEquals(18, go(18, GridKey.DOWN))
    }

    @Test
    fun `an empty grid or a stale index never moves`() {
        assertEquals(0, GridNav.next(0, GridKey.DOWN, cols, 0))
        assertEquals(50, GridNav.next(50, GridKey.UP, cols, 10))
    }
}
