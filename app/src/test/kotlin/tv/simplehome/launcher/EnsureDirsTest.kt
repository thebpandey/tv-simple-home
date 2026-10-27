package tv.simplehome.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EnsureDirsTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `a fresh install gets photos and scenes directories so files can be pushed without creating them by hand`() {
        val base = tmp.newFolder("files")
        assertTrue(PhotoStore.ensureDirs(base))
        assertTrue(java.io.File(base, "photos").isDirectory)
        assertTrue(java.io.File(base, "scenes").isDirectory)
        assertTrue("calling it again must be harmless", PhotoStore.ensureDirs(base))
    }

    @Test
    fun `no external storage reports failure instead of throwing`() {
        assertFalse(PhotoStore.ensureDirs(null))
    }
}
