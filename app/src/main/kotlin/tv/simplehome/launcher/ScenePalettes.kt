package tv.simplehome.launcher

/**
 * Brightest (night) and darkest (day) colour of each scene in the two text bands: the sky behind the header (~27-151dp)
 * and the sky/far-ridge colour behind the centred quote (~205-295dp). Kept next to the scene code so the scrim contrast
 * test can check every scene; keep in sync when a scene's palette changes.
 */
object ScenePalettes {
    data class Entry(val scene: String, val header: Int, val quote: Int)

    private fun e(name: String, header: Long, quote: Long) = Entry(name, header.toInt(), quote.toInt())

    val NIGHT_BRIGHTEST = listOf(
        e("forest", 0xFF2A5058, 0xFF3A5E58), e("jungle", 0xFF1A4A44, 0xFF1E4A44), e("beach", 0xFF3A3A78, 0xFF3A3A78),
        e("mountains", 0xFF4A3A8C, 0xFF9A4F8E), e("snow_peaks", 0xFF2A4A80, 0xFF5A86B8), e("autumn_valley", 0xFF3A2A58, 0xFF6A3A5A),
        e("diwali", 0xFF3B0F3A, 0xFF7A2A22), e("holi", 0xFF4A2A78, 0xFF9A4A8A), e("navratri_dussehra", 0xFF8C2F4A, 0xFFB5402F),
        e("janmashtami", 0xFF0F3A56, 0xFF1B6B73), e("ganesh", 0xFF2A1A58, 0xFFC05A48), e("raksha_bandhan", 0xFF4A2A78, 0xFF9A5A8A),
        e("shivaratri", 0xFF141B45, 0xFF2B3A78), e("sankranti_kites", 0xFF2A3A78, 0xFF5A5A98), e("lohri", 0xFF2A2A5E, 0xFF8A4A58),
        e("vasant", 0xFF1E2C5A, 0xFF4A5A70), e("karwa_chauth", 0xFF2A2060, 0xFFE0906A), e("chhath", 0xFF1A2058, 0xFF6A5A98),
        e("halloween", 0xFF1E1946, 0xFF4A2A58), e("thanksgiving", 0xFF3A2A50, 0xFF7A4A48), e("christmas", 0xFF16284A, 0xFF2E4A72),
        e("new_year", 0xFF0E1236, 0xFF2A2656),
    )

    val DAY_DARKEST = listOf(
        e("forest", 0xFF3C7FCB, 0xFF9ACBE6), e("jungle", 0xFF2E86B8, 0xFF7CC4C0), e("beach", 0xFF2A8AE0, 0xFF6CBCF0),
        e("mountains", 0xFF2F6FD0, 0xFF6FB0EC), e("snow_peaks", 0xFF2B7BD8, 0xFF74B8F0), e("autumn_valley", 0xFF4A90D0, 0xFFA8D0E8),
        e("diwali", 0xFF3A8AD8, 0xFF8AC8F0), e("holi", 0xFF5AAEF0, 0xFFFFD0E8), e("navratri_dussehra", 0xFF2E78D0, 0xFF86C0EC),
        e("janmashtami", 0xFF2A78C8, 0xFF6EC0D8), e("ganesh", 0xFF4A8CD8, 0xFFFFC890), e("raksha_bandhan", 0xFF7EB8F0, 0xFFFFD0D8),
        e("shivaratri", 0xFF2670C8, 0xFF6EB4EC), e("sankranti_kites", 0xFF3C8CE0, 0xFF9CCDF0), e("lohri", 0xFF4A90D8, 0xFF9CCCEC),
        e("vasant", 0xFF5AAEEE, 0xFFD6EBF2), e("karwa_chauth", 0xFF3A80D0, 0xFF9ACAE8), e("chhath", 0xFF3A88D8, 0xFF8AD0F0),
        e("halloween", 0xFF4A8CD0, 0xFFA8D0E8), e("thanksgiving", 0xFF4A90D0, 0xFFA8D0E8), e("christmas", 0xFF3A88D8, 0xFF9AD0F0),
        e("new_year", 0xFF3A88D8, 0xFF9AD0F0),
    )
}
