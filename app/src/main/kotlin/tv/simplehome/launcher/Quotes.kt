package tv.simplehome.launcher

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class Theme {
    // Day set
    MOTIVATION, INSPIRATION, POSITIVITY, STRENGTH, RESILIENCE, CONFIDENCE, DETERMINATION, WINNING_ATTITUDE,
    // Evening set
    PEACE, MEDITATION, HEALING, RELAXATION, SELF_ACTUALIZATION, BLISS, GRATITUDE, HAPPINESS,
    // Added with the merged public-domain collection
    VIRTUE, INTEGRITY, WISDOM, // day
    LOVE, // evening
}

/** [source] is for verification only and is never shown on screen. */
class Quote(val text: String, val author: String, val theme: Theme, val source: String)

enum class QuoteSet { DAY, EVENING }

/** Bundled quotes (fully offline) and the deterministic time-to-quote mapping. */
object Quotes {
    // Long quotes would wrap to 4+ lines and collide with the tile rows on a 540dp-tall screen.
    const val MAX_LENGTH = 160 // 3 lines of 23sp at 60% of the width hold about 170 characters
    const val EVENING_START_HOUR = 18
    const val MIN_PER_THEME = 10
    const val SLOT_MINUTES = 5
    const val SLOTS_PER_DAY = 24 * 60 / SLOT_MINUTES // 288
    private const val EVENING_START_SLOT = EVENING_START_HOUR * 60 / SLOT_MINUTES // 216 day slots, then 72 evening slots
    private const val EVENING_SLOTS = SLOTS_PER_DAY - EVENING_START_SLOT

    val DAY_THEMES = listOf(
        Theme.MOTIVATION, Theme.INSPIRATION, Theme.POSITIVITY, Theme.STRENGTH,
        Theme.RESILIENCE, Theme.CONFIDENCE, Theme.DETERMINATION, Theme.WINNING_ATTITUDE,
        Theme.VIRTUE, Theme.INTEGRITY, Theme.WISDOM,
    )
    val EVENING_THEMES = listOf(
        Theme.PEACE, Theme.MEDITATION, Theme.HEALING, Theme.RELAXATION,
        Theme.SELF_ACTUALIZATION, Theme.BLISS, Theme.GRATITUDE, Theme.HAPPINESS, Theme.LOVE,
    )

    fun setAt(now: LocalDateTime): QuoteSet =
        if (now.hour >= EVENING_START_HOUR) QuoteSet.EVENING else QuoteSet.DAY

    /** 0..287: which 5-minute slot of the local day [now] falls in. */
    fun slotOf(now: LocalDateTime): Int = (now.hour * 60 + now.minute) / SLOT_MINUTES

    /**
     * Quote for the 5-minute slot containing [now]: a pure function of (set, local date, slot), so the same slot gives the
     * same quote after a restart. Each set advances through one continuous counter (only that set's slots, across days);
     * counter / poolSize is the cycle number and counter % poolSize the position inside that cycle. Every cycle is a
     * deterministic shuffle, so each quote appears exactly once per cycle before any repeats, and a quote is never shown
     * twice in a row, not even across a cycle boundary.
     */
    fun quoteAt(now: LocalDateTime): Quote {
        val set = setAt(now)
        val pool = if (set == QuoteSet.DAY) DAY else EVENING
        val epochDay = now.toLocalDate().toEpochDay()
        val counter = if (set == QuoteSet.DAY) epochDay * EVENING_START_SLOT + slotOf(now)
        else epochDay * EVENING_SLOTS + (slotOf(now) - EVENING_START_SLOT)
        val cycle = Math.floorDiv(counter, pool.size.toLong())
        val position = Math.floorMod(counter, pool.size.toLong()).toInt()
        return pool[cycleOrder(pool.size, set, cycle)[position]]
    }

    /** Order of one cycle; its first item is swapped with the second if it would repeat the previous cycle's last quote. */
    internal fun cycleOrder(n: Int, set: QuoteSet, cycle: Long): IntArray {
        val order = shuffledOrder(n, seed = cycle * 2 + set.ordinal)
        val previousLast = shuffledOrder(n, seed = (cycle - 1) * 2 + set.ordinal).let { prev ->
            // The previous cycle may itself have swapped its first two items; that never changes its last item for n > 2.
            prev[n - 1]
        }
        if (n > 1 && order[0] == previousLast) { val t = order[0]; order[0] = order[1]; order[1] = t }
        return order
    }

    /** Next moment the displayed quote can change: the next 5-minute boundary (which includes 18:00 and midnight). */
    fun nextBoundary(now: LocalDateTime): LocalDateTime =
        now.toLocalDate().atStartOfDay().plusMinutes((slotOf(now) + 1L) * SLOT_MINUTES)

    /** Deterministic Fisher-Yates driven by a splitmix64 stream. */
    internal fun shuffledOrder(n: Int, seed: Long): IntArray {
        val a = IntArray(n) { it }
        for (i in n - 1 downTo 1) {
            val j = Math.floorMod(splitmix(seed * 1_000_003L + i), (i + 1).toLong()).toInt()
            val tmp = a[i]; a[i] = a[j]; a[j] = tmp
        }
        return a
    }

    private fun splitmix(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    // Every quote has a specific, checkable source (work + section/poem/letter); unsourced quotes were dropped.
    // Wording of translated works follows common English translations, so phrasing varies by translator.

    // Motivation, inspiration, positivity, strength, resilience, confidence, determination, winning attitude.
    val DAY: List<Quote> by lazy { CURATED_DAY + QuotesExtra.DAY }

    private val CURATED_DAY: List<Quote> = listOf(
        Quote("Waste no more time arguing about what a good man should be. Be one.", "Marcus Aurelius", Theme.MOTIVATION, "Meditations 10.16"),
        Quote("Very little is needed to make a happy life; it is all within yourself, in your way of thinking.", "Marcus Aurelius", Theme.POSITIVITY, "Meditations 7.67"),
        Quote("If it is not right, do not do it; if it is not true, do not say it.", "Marcus Aurelius", Theme.STRENGTH, "Meditations 12.17"),
        Quote("The best revenge is not to be like your enemy.", "Marcus Aurelius", Theme.STRENGTH, "Meditations 6.6"),
        Quote("It is not death that a man should fear, but he should fear never beginning to live.", "Marcus Aurelius", Theme.INSPIRATION, "Meditations 12.1"),
        Quote("The impediment to action advances action. What stands in the way becomes the way.", "Marcus Aurelius", Theme.RESILIENCE, "Meditations 5.20"),
        Quote("Loss is nothing else but change, and change is Nature's delight.", "Marcus Aurelius", Theme.RESILIENCE, "Meditations 9.35"),
        Quote("Look within. Within is the fountain of good, and it will ever bubble up, if thou wilt ever dig.", "Marcus Aurelius", Theme.CONFIDENCE, "Meditations 7.59"),
        Quote("Dwell on the beauty of life. Watch the stars, and see yourself running with them.", "Marcus Aurelius", Theme.POSITIVITY, "Meditations 7.47"),
        Quote("Never esteem anything as of advantage to you that will make you break your word or lose your self-respect.", "Marcus Aurelius", Theme.STRENGTH, "Meditations 3.7"),
        Quote("Confine yourself to the present.", "Marcus Aurelius", Theme.POSITIVITY, "Meditations 8.36"),
        Quote("Receive without conceit, release without struggle.", "Marcus Aurelius", Theme.RESILIENCE, "Meditations 8.33"),
        Quote("Accept the things to which fate binds you, and love the people with whom fate brings you together, but do so with all your heart.", "Marcus Aurelius", Theme.POSITIVITY, "Meditations 6.39"),
        Quote("The soul is dyed by the thoughts.", "Marcus Aurelius", Theme.POSITIVITY, "Meditations 5.16"),
        Quote("It is not that we have a short time to live, but that we waste much of it.", "Seneca", Theme.MOTIVATION, "On the Shortness of Life 1"),
        Quote("We suffer more often in imagination than in reality.", "Seneca", Theme.RESILIENCE, "Letters to Lucilius 13"),
        Quote("While we are postponing, life speeds by.", "Seneca", Theme.MOTIVATION, "Letters to Lucilius 1"),
        Quote("Begin at once to live, and count each separate day as a separate life.", "Seneca", Theme.INSPIRATION, "Letters to Lucilius 101"),
        Quote("Sometimes even to live is an act of courage.", "Seneca", Theme.STRENGTH, "Letters to Lucilius 78"),
        Quote("It is not because things are difficult that we do not dare; it is because we do not dare that they are difficult.", "Seneca", Theme.CONFIDENCE, "Letters to Lucilius 104"),
        Quote("If one does not know to which port one is sailing, no wind is favorable.", "Seneca", Theme.DETERMINATION, "Letters to Lucilius 71"),
        Quote("He suffers more than necessary, who suffers before it is necessary.", "Seneca", Theme.RESILIENCE, "Letters to Lucilius 98"),
        Quote("Men are disturbed, not by things, but by the principles and notions which they form concerning things.", "Epictetus", Theme.RESILIENCE, "Enchiridion 5"),
        Quote("First say to yourself what you would be; and then do what you have to do.", "Epictetus", Theme.DETERMINATION, "Discourses 3.23"),
        Quote("It is impossible for a man to learn what he thinks he already knows.", "Epictetus", Theme.MOTIVATION, "Discourses 2.17"),
        Quote("Make the best use of what is in your power, and take the rest as it happens.", "Epictetus", Theme.RESILIENCE, "Discourses 1.1"),
        Quote("If you want to improve, be content to be thought foolish and stupid.", "Epictetus", Theme.CONFIDENCE, "Enchiridion 13"),
        Quote("No great thing is created suddenly, any more than a bunch of grapes or a fig.", "Epictetus", Theme.DETERMINATION, "Discourses 1.15"),
        Quote("Only the educated are free.", "Epictetus", Theme.INSPIRATION, "Discourses 2.1"),
        Quote("A journey of a thousand miles begins with a single step.", "Lao Tzu", Theme.MOTIVATION, "Tao Te Ching 64"),
        Quote("He who knows others is wise; he who knows himself is enlightened.", "Lao Tzu", Theme.CONFIDENCE, "Tao Te Ching 33"),
        Quote("Learning without thought is labor lost; thought without learning is perilous.", "Confucius", Theme.MOTIVATION, "Analects 2.15"),
        Quote("To see what is right and not to do it is want of courage.", "Confucius", Theme.STRENGTH, "Analects 2.24"),
        Quote("Is it not pleasant to learn with a constant perseverance and application?", "Confucius", Theme.POSITIVITY, "Analects 1.1"),
        Quote("When you know a thing, to hold that you know it; and when you do not know a thing, to allow that you do not know it; this is knowledge.", "Confucius", Theme.INSPIRATION, "Analects 2.17"),
        Quote("What you do not want done to yourself, do not do to others.", "Confucius", Theme.INSPIRATION, "Analects 15.24"),
        Quote("Nothing great was ever achieved without enthusiasm.", "Ralph Waldo Emerson", Theme.MOTIVATION, "Essays: Circles"),
        Quote("Do the thing and you shall have the power.", "Ralph Waldo Emerson", Theme.DETERMINATION, "Essays: Self-Reliance"),
        Quote("Trust thyself: every heart vibrates to that iron string.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "Essays: Self-Reliance"),
        Quote("The only way to have a friend is to be one.", "Ralph Waldo Emerson", Theme.POSITIVITY, "Essays: Friendship"),
        Quote("Whoso would be a man must be a nonconformist.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "Essays: Self-Reliance"),
        Quote("Nothing can bring you peace but yourself.", "Ralph Waldo Emerson", Theme.STRENGTH, "Essays: Self-Reliance"),
        Quote("Hitch your wagon to a star.", "Ralph Waldo Emerson", Theme.INSPIRATION, "Society and Solitude: Civilization"),
        Quote("Write it on your heart that every day is the best day in the year.", "Ralph Waldo Emerson", Theme.POSITIVITY, "Society and Solitude: Works and Days"),
        Quote("If one advances confidently in the direction of his dreams ... he will meet with a success unexpected in common hours.", "Henry David Thoreau", Theme.INSPIRATION, "Walden, Conclusion"),
        Quote("Our life is frittered away by detail. Simplify, simplify.", "Henry David Thoreau", Theme.INSPIRATION, "Walden, Where I Lived"),
        Quote("Rather than love, than money, than fame, give me truth.", "Henry David Thoreau", Theme.STRENGTH, "Walden"),
        Quote("The sun is but a morning star.", "Henry David Thoreau", Theme.INSPIRATION, "Walden, Conclusion"),
        Quote("Morning is when I am awake and there is a dawn in me.", "Henry David Thoreau", Theme.POSITIVITY, "Walden, Where I Lived"),
        Quote("It is not enough to be industrious; so are the ants. What are you industrious about?", "Henry David Thoreau", Theme.MOTIVATION, "Letter to Harrison Blake, 16 Nov 1857"),
        Quote("Things do not change; we change.", "Henry David Thoreau", Theme.INSPIRATION, "Walden, Conclusion"),
        Quote("Heaven is under our feet as well as over our heads.", "Henry David Thoreau", Theme.POSITIVITY, "Walden, Conclusion"),
        Quote("Life is either a daring adventure or nothing at all.", "Helen Keller", Theme.INSPIRATION, "Let Us Have Faith (1940)"),
        Quote("Optimism is the faith that leads to achievement. Nothing can be done without hope and confidence.", "Helen Keller", Theme.POSITIVITY, "Optimism (1903)"),
        Quote("Although the world is full of suffering, it is full also of the overcoming of it.", "Helen Keller", Theme.RESILIENCE, "Optimism (1903)"),
        Quote("Always bear in mind that your own resolution to succeed is more important than any other one thing.", "Abraham Lincoln", Theme.WINNING_ATTITUDE, "Letter to Isham Reavis, 5 Nov 1855"),
        Quote("I do the very best I know how, the very best I can; and I mean to keep doing so until the end.", "Abraham Lincoln", Theme.DETERMINATION, "Carpenter, Six Months at the White House (1866)"),
        Quote("Well done is better than well said.", "Benjamin Franklin", Theme.WINNING_ATTITUDE, "Poor Richard's Almanack"),
        Quote("Lost time is never found again.", "Benjamin Franklin", Theme.MOTIVATION, "Poor Richard's Almanack"),
        Quote("Diligence is the mother of good luck.", "Benjamin Franklin", Theme.WINNING_ATTITUDE, "Poor Richard's Almanack"),
        Quote("Early to bed and early to rise, makes a man healthy, wealthy and wise.", "Benjamin Franklin", Theme.POSITIVITY, "Poor Richard's Almanack"),
        Quote("He that can have patience can have what he will.", "Benjamin Franklin", Theme.DETERMINATION, "Poor Richard's Almanack"),
        Quote("Dost thou love life? Then do not squander time, for that is the stuff life is made of.", "Benjamin Franklin", Theme.MOTIVATION, "Poor Richard's Almanack"),
        Quote("Little strokes fell great oaks.", "Benjamin Franklin", Theme.WINNING_ATTITUDE, "Poor Richard's Almanack"),
        Quote("Wish not so much to live long as to live well.", "Benjamin Franklin", Theme.INSPIRATION, "Poor Richard's Almanack"),
        Quote("Genius without education is like silver in the mine.", "Benjamin Franklin", Theme.INSPIRATION, "Poor Richard's Almanack"),
        Quote("Be at war with your vices, at peace with your neighbors, and let every new year find you a better man.", "Benjamin Franklin", Theme.MOTIVATION, "Poor Richard's Almanack"),
        Quote("To thine own self be true.", "William Shakespeare", Theme.CONFIDENCE, "Hamlet 1.3"),
        Quote("There is nothing either good or bad, but thinking makes it so.", "William Shakespeare", Theme.POSITIVITY, "Hamlet 2.2"),
        Quote("Our doubts are traitors, and make us lose the good we oft might win, by fearing to attempt.", "William Shakespeare", Theme.CONFIDENCE, "Measure for Measure 1.4"),
        Quote("What's past is prologue.", "William Shakespeare", Theme.INSPIRATION, "The Tempest 2.1"),
        Quote("Make use of time, let not advantage slip.", "William Shakespeare", Theme.MOTIVATION, "Venus and Adonis"),
        Quote("Some are born great, some achieve greatness, and some have greatness thrust upon them.", "William Shakespeare", Theme.CONFIDENCE, "Twelfth Night 2.5"),
        Quote("Courage is resistance to fear, mastery of fear, not absence of fear.", "Mark Twain", Theme.STRENGTH, "Pudd'nhead Wilson (1894)"),
        Quote("The human race has one really effective weapon, and that is laughter.", "Mark Twain", Theme.POSITIVITY, "The Mysterious Stranger (1916)"),
        Quote("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Will Durant", Theme.WINNING_ATTITUDE, "The Story of Philosophy (1926)"),
        Quote("The beginning is the most important part of any work.", "Plato", Theme.MOTIVATION, "Republic, Book 2"),
        Quote("The unexamined life is not worth living.", "Socrates", Theme.INSPIRATION, "Apology 38a"),
        Quote("Better than a thousand useless words is one single word that brings peace.", "Dhammapada", Theme.POSITIVITY, "Dhammapada 100"),
        Quote("Hatred does not cease by hatred at any time; hatred ceases by love. This is an old rule.", "Dhammapada", Theme.STRENGTH, "Dhammapada 5"),
        Quote("Health is the greatest gift, contentment the greatest wealth.", "Dhammapada", Theme.POSITIVITY, "Dhammapada 204"),
        Quote("Even by the falling of water-drops, a water-pot is filled.", "Dhammapada", Theme.DETERMINATION, "Dhammapada 121"),
        Quote("I am larger, better than I thought; I did not know I held so much goodness.", "Walt Whitman", Theme.CONFIDENCE, "Leaves of Grass: Song of Myself"),
        Quote("The world is so full of a number of things, I'm sure we should all be as happy as kings.", "Robert Louis Stevenson", Theme.POSITIVITY, "A Child's Garden of Verses: Happy Thought"),
        Quote("No one is useless in this world who lightens the burden of it for anyone else.", "Charles Dickens", Theme.INSPIRATION, "Our Mutual Friend (1865)"),
        Quote("To live is the rarest thing in the world. Most people exist, that is all.", "Oscar Wilde", Theme.INSPIRATION, "The Soul of Man under Socialism (1891)"),
        Quote("We are all in the gutter, but some of us are looking at the stars.", "Oscar Wilde", Theme.POSITIVITY, "Lady Windermere's Fan, Act 3"),
        Quote("Nothing will ever be attempted, if all possible objections must first be overcome.", "Samuel Johnson", Theme.DETERMINATION, "Rasselas, ch. 13"),
        Quote("Great works are performed not by strength but by perseverance.", "Samuel Johnson", Theme.WINNING_ATTITUDE, "Rasselas, ch. 13"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt", Theme.MOTIVATION, "Autobiography (1913)"),
        Quote("It is hard to fail, but it is worse never to have tried to succeed.", "Theodore Roosevelt", Theme.WINNING_ATTITUDE, "The Strenuous Life (1899)"),
        Quote("The mountains are calling and I must go.", "John Muir", Theme.INSPIRATION, "Letter to Sarah Muir Galloway, 1873"),
        Quote("Climb the mountains and get their good tidings. Nature's peace will flow into you as sunshine flows into trees.", "John Muir", Theme.POSITIVITY, "Our National Parks (1901)"),
        Quote("Genius is one percent inspiration, ninety-nine percent perspiration.", "Thomas Edison", Theme.WINNING_ATTITUDE, "Harper's Monthly, Sept 1932"),
        Quote("You must do the thing you think you cannot do.", "Eleanor Roosevelt", Theme.STRENGTH, "You Learn by Living (1960)"),
        Quote("Great things are done by a series of small things brought together.", "Vincent van Gogh", Theme.DETERMINATION, "Letter to Theo van Gogh, 1882"),
        Quote("If you hear a voice within you say \"you cannot paint,\" then by all means paint, and that voice will be silenced.", "Vincent van Gogh", Theme.CONFIDENCE, "Letter to Theo van Gogh, 1883"),
        Quote("Success is to be measured not so much by the position that one has reached in life as by the obstacles which he has overcome.", "Booker T. Washington", Theme.WINNING_ATTITUDE, "Up from Slavery (1901)"),
        Quote("Be not afraid of life. Believe that life is worth living, and your belief will help create the fact.", "William James", Theme.POSITIVITY, "Is Life Worth Living? (1895)"),
        Quote("A thing of beauty is a joy for ever.", "John Keats", Theme.POSITIVITY, "Endymion, Book 1"),
        Quote("To strive, to seek, to find, and not to yield.", "Alfred, Lord Tennyson", Theme.DETERMINATION, "Ulysses"),
        Quote("A man's reach should exceed his grasp, or what's a heaven for?", "Robert Browning", Theme.INSPIRATION, "Andrea del Sarto"),
        Quote("I am the master of my fate, I am the captain of my soul.", "William Ernest Henley", Theme.STRENGTH, "Invictus"),
        Quote("Hope is the thing with feathers that perches in the soul.", "Emily Dickinson", Theme.POSITIVITY, "Poem 314"),
        Quote("I dwell in Possibility, a fairer House than Prose.", "Emily Dickinson", Theme.INSPIRATION, "Poem 657"),
        Quote("Two roads diverged in a wood, and I took the one less traveled by, and that has made all the difference.", "Robert Frost", Theme.INSPIRATION, "The Road Not Taken (1916)"),
        Quote("Lives of great men all remind us we can make our lives sublime.", "Henry Wadsworth Longfellow", Theme.INSPIRATION, "A Psalm of Life"),
        Quote("Let us, then, be up and doing, with a heart for any fate; still achieving, still pursuing, learn to labor and to wait.", "Henry Wadsworth Longfellow", Theme.DETERMINATION, "A Psalm of Life"),
        Quote("Hope springs eternal in the human breast.", "Alexander Pope", Theme.POSITIVITY, "An Essay on Man, Epistle 1"),
        Quote("He who has a why to live can bear almost any how.", "Friedrich Nietzsche", Theme.RESILIENCE, "Twilight of the Idols, Maxims and Arrows 12"),
        Quote("What does not kill me makes me stronger.", "Friedrich Nietzsche", Theme.RESILIENCE, "Twilight of the Idols, Maxims and Arrows 8"),
        Quote("While there's life, there's hope.", "Cicero", Theme.POSITIVITY, "Letters to Atticus 9.10"),
        Quote("Fortune favors the bold.", "Virgil", Theme.CONFIDENCE, "Aeneid 10.284"),
        Quote("Seize the day, put very little trust in tomorrow.", "Horace", Theme.MOTIVATION, "Odes 1.11"),
        Quote("Dare to be wise.", "Horace", Theme.CONFIDENCE, "Epistles 1.2"),
        Quote("Do the duty which lies nearest thee, which thou knowest to be a duty! Thy second duty will already have become clearer.", "Thomas Carlyle", Theme.DETERMINATION, "Sartor Resartus, Book 2 ch. 9"),
        Quote("I'm not afraid of storms, for I'm learning how to sail my ship.", "Louisa May Alcott", Theme.RESILIENCE, "Little Women"),
        Quote("Isn't it nice to think that tomorrow is a new day with no mistakes in it yet?", "L. M. Montgomery", Theme.POSITIVITY, "Anne of Green Gables, ch. 21"),
        Quote("I can't go back to yesterday, because I was a different person then.", "Lewis Carroll", Theme.POSITIVITY, "Alice's Adventures in Wonderland, ch. 10"),
        Quote("Chance favors only the prepared mind.", "Louis Pasteur", Theme.WINNING_ATTITUDE, "Lecture at Lille, 7 Dec 1854"),
        Quote("If I have seen further, it is by standing on the shoulders of giants.", "Isaac Newton", Theme.INSPIRATION, "Letter to Robert Hooke, 5 Feb 1675"),
        Quote("Imagination is more important than knowledge.", "Albert Einstein", Theme.INSPIRATION, "Saturday Evening Post, 26 Oct 1929"),
        Quote("Try not to become a man of success, but rather try to become a man of value.", "Albert Einstein", Theme.INSPIRATION, "Life magazine, 2 May 1955"),
        Quote("I've failed over and over and over again in my life. And that is why I succeed.", "Michael Jordan", Theme.WINNING_ATTITUDE, "Nike \"Failure\" commercial (1997)"),
        Quote("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs", Theme.INSPIRATION, "Stanford commencement address, 2005"),
        Quote("The only way to do great work is to love what you do.", "Steve Jobs", Theme.MOTIVATION, "Stanford commencement address, 2005"),
        Quote("The credit belongs to the man who is actually in the arena.", "Theodore Roosevelt", Theme.STRENGTH, "Citizenship in a Republic, Sorbonne, 1910"),
        Quote("Who at the best knows in the end the triumph of high achievement, and who at the worst, if he fails, at least fails while daring greatly.", "Theodore Roosevelt", Theme.WINNING_ATTITUDE, "Citizenship in a Republic, Sorbonne, 1910"),
        Quote("Far better is it to dare mighty things, to win glorious triumphs, even though checkered by failure.", "Theodore Roosevelt", Theme.WINNING_ATTITUDE, "The Strenuous Life (1899)"),
        Quote("It is only through labor and painful effort, by grim energy and resolute courage, that we move on to better things.", "Theodore Roosevelt", Theme.DETERMINATION, "The Strenuous Life (1899)"),
        Quote("The highest result of education is tolerance.", "Helen Keller", Theme.INSPIRATION, "Optimism (1903)"),
        Quote("If there is no struggle, there is no progress.", "Frederick Douglass", Theme.RESILIENCE, "West India Emancipation speech, 1857"),
        Quote("Never give in, never give in, never, never, never, never—in nothing, great or small, large or petty.", "Winston Churchill", Theme.DETERMINATION, "Harrow School speech, 29 Oct 1941"),
        Quote("I can accept failure, everyone fails at something. But I can't accept not trying.", "Michael Jordan", Theme.WINNING_ATTITUDE, "I Can't Accept Not Trying (1994)"),
        Quote("Fire tests gold, suffering brave men.", "Seneca", Theme.RESILIENCE, "On Providence 5"),
        Quote("Difficulties are things that show what men are.", "Epictetus", Theme.RESILIENCE, "Discourses 1.24"),
        Quote("If you wish to be a writer, write.", "Epictetus", Theme.MOTIVATION, "Discourses 2.18"),
        Quote("Whatever you would make habitual, practice it.", "Epictetus", Theme.WINNING_ATTITUDE, "Discourses 2.18"),
        Quote("In the morning when thou risest unwillingly, let this thought be present: I am rising to the work of a human being.", "Marcus Aurelius", Theme.MOTIVATION, "Meditations 5.1"),
        Quote("Whatever any one does or says, I must be emerald, and keep my colour.", "Marcus Aurelius", Theme.STRENGTH, "Meditations 7.15"),
        Quote("Do every act of your life as though it were the very last act of your life.", "Marcus Aurelius", Theme.MOTIVATION, "Meditations 2.5"),
        Quote("Nothing happens to any man which he is not formed by nature to bear.", "Marcus Aurelius", Theme.RESILIENCE, "Meditations 5.18"),
        Quote("The art of living is more like the wrestler's art than the dancer's.", "Marcus Aurelius", Theme.RESILIENCE, "Meditations 7.61"),
        Quote("Everything harmonizes with me which is harmonious to thee, O Universe.", "Marcus Aurelius", Theme.POSITIVITY, "Meditations 4.23"),
        Quote("Calamity is virtue's opportunity.", "Seneca", Theme.RESILIENCE, "On Providence 4"),
        Quote("Fate leads the willing and drags along the reluctant.", "Seneca", Theme.RESILIENCE, "Letters to Lucilius 107"),
        Quote("Disease is an impediment to the body, but not to the will.", "Epictetus", Theme.RESILIENCE, "Enchiridion 9"),
        Quote("Remember that it is not he who reviles or strikes you who insults you, but your opinion that these things are insulting.", "Epictetus", Theme.STRENGTH, "Enchiridion 20"),
        Quote("The superior man is modest in his speech, but exceeds in his actions.", "Confucius", Theme.STRENGTH, "Analects 14.27"),
        Quote("The will of even a common man cannot be taken from him.", "Confucius", Theme.DETERMINATION, "Analects 9.26"),
        Quote("Do not be concerned that you are not known; seek to be worthy to be known.", "Confucius", Theme.CONFIDENCE, "Analects 4.14"),
        Quote("He who overcomes others has force; he who overcomes himself is strong.", "Lao Tzu", Theme.STRENGTH, "Tao Te Ching 33"),
        Quote("Let the wise man make an island for himself which no flood can overwhelm.", "Dhammapada", Theme.STRENGTH, "Dhammapada 25"),
        Quote("Conquer anger by non-anger. Conquer wickedness by goodness.", "Dhammapada", Theme.STRENGTH, "Dhammapada 223"),
        Quote("Nothing is at last sacred but the integrity of your own mind.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "Essays: Self-Reliance"),
        Quote("Insist on yourself; never imitate.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "Essays: Self-Reliance"),
        Quote("To be great is to be misunderstood.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "Essays: Self-Reliance"),
        Quote("Fear always springs from ignorance.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "The American Scholar (1837)"),
        Quote("Our strength grows out of our weakness.", "Ralph Waldo Emerson", Theme.RESILIENCE, "Essays: Compensation"),
        Quote("Shallow men believe in luck or in circumstance. Strong men believe in cause and effect.", "Ralph Waldo Emerson", Theme.WINNING_ATTITUDE, "The Conduct of Life: Worship"),
        Quote("Dreams are the touchstones of our characters.", "Henry David Thoreau", Theme.CONFIDENCE, "Journal, 1851"),
        Quote("However mean your life is, meet it and live it; do not shun it and call it hard names.", "Henry David Thoreau", Theme.RESILIENCE, "Walden, Conclusion"),
        Quote("I know of no more encouraging fact than the unquestionable ability of man to elevate his life by a conscious endeavor.", "Henry David Thoreau", Theme.DETERMINATION, "Walden, Where I Lived"),
        Quote("Goodness is the only investment that never fails.", "Henry David Thoreau", Theme.POSITIVITY, "Walden, Higher Laws"),
        Quote("The question is not what you look at, but what you see.", "Henry David Thoreau", Theme.INSPIRATION, "Journal, 1851"),
        Quote("Cowards die many times before their deaths; the valiant never taste of death but once.", "William Shakespeare", Theme.STRENGTH, "Julius Caesar 2.2"),
        Quote("Screw your courage to the sticking-place, and we'll not fail.", "William Shakespeare", Theme.DETERMINATION, "Macbeth 1.7"),
        Quote("Courage mounteth with occasion.", "William Shakespeare", Theme.STRENGTH, "King John 2.1"),
        Quote("Men at some time are masters of their fates.", "William Shakespeare", Theme.CONFIDENCE, "Julius Caesar 1.2"),
        Quote("Our remedies oft in ourselves do lie.", "William Shakespeare", Theme.CONFIDENCE, "All's Well That Ends Well 1.1"),
        Quote("We know what we are, but know not what we may be.", "William Shakespeare", Theme.INSPIRATION, "Hamlet 4.5"),
        Quote("Love all, trust a few, do wrong to none.", "William Shakespeare", Theme.POSITIVITY, "All's Well That Ends Well 1.1"),
        Quote("Defer no time, delays have dangerous ends.", "William Shakespeare", Theme.DETERMINATION, "Henry VI, Part 1, 3.2"),
        Quote("Things won are done; joy's soul lies in the doing.", "William Shakespeare", Theme.MOTIVATION, "Troilus and Cressida 1.2"),
        Quote("Tho' much is taken, much abides.", "Alfred, Lord Tennyson", Theme.RESILIENCE, "Ulysses"),
        Quote("One equal temper of heroic hearts, made weak by time and fate, but strong in will.", "Alfred, Lord Tennyson", Theme.DETERMINATION, "Ulysses"),
        Quote("If you can meet with Triumph and Disaster and treat those two impostors just the same.", "Rudyard Kipling", Theme.WINNING_ATTITUDE, "If— (1910)"),
        Quote("If you can fill the unforgiving minute with sixty seconds' worth of distance run.", "Rudyard Kipling", Theme.WINNING_ATTITUDE, "If— (1910)"),
        Quote("Be not like dumb, driven cattle! Be a hero in the strife!", "Henry Wadsworth Longfellow", Theme.WINNING_ATTITUDE, "A Psalm of Life"),
        Quote("Henceforth I ask not good-fortune, I myself am good-fortune.", "Walt Whitman", Theme.WINNING_ATTITUDE, "Leaves of Grass: Song of the Open Road"),
        Quote("Failing to fetch me at first, keep encouraged; missing me one place, search another.", "Walt Whitman", Theme.RESILIENCE, "Leaves of Grass: Song of Myself"),
        Quote("My head is bloody, but unbowed.", "William Ernest Henley", Theme.RESILIENCE, "Invictus"),
        Quote("The best way out is always through.", "Robert Frost", Theme.RESILIENCE, "A Servant to Servants (1914)"),
        Quote("I am no bird; and no net ensnares me: I am a free human being with an independent will.", "Charlotte Brontë", Theme.CONFIDENCE, "Jane Eyre, ch. 23"),
        Quote("Be patient and tough; some day this pain will be useful to you.", "Ovid", Theme.RESILIENCE, "Amores 3.11"),
    )

    // Peace, meditation, healing, relaxation, self-actualization, bliss, gratitude, happiness.
    val EVENING: List<Quote> by lazy { CURATED_EVENING + QuotesExtra.EVENING }

    private val CURATED_EVENING: List<Quote> = listOf(
        Quote("He who knows he has enough is rich.", "Tao Te Ching", Theme.BLISS, "Tao Te Ching 33"),
        Quote("Nowhere can man find a quieter or more untroubled retreat than in his own soul.", "Marcus Aurelius", Theme.PEACE, "Meditations 4.3"),
        Quote("Tranquillity is nothing else than the good ordering of the mind.", "Marcus Aurelius", Theme.PEACE, "Meditations 4.3"),
        Quote("Be like the promontory against which the waves continually break, but it stands firm and tames the fury of the water around it.", "Marcus Aurelius", Theme.PEACE, "Meditations 4.49"),
        Quote("Take away the complaint \"I have been harmed,\" and the harm is taken away.", "Marcus Aurelius", Theme.HEALING, "Meditations 4.7"),
        Quote("Don't demand that things happen as you wish, but wish that they happen as they do happen, and you will go on well.", "Epictetus", Theme.HAPPINESS, "Enchiridion 8"),
        Quote("Nothing is a better proof of a well-ordered mind than a man's ability to stop just where he is and pass some time in his own company.", "Seneca", Theme.MEDITATION, "Letters to Lucilius 2"),
        Quote("The mind must be given relaxation; it will rise improved and sharper after a good break.", "Seneca", Theme.RELAXATION, "On Tranquility of Mind 17"),
        Quote("To be everywhere is to be nowhere.", "Seneca", Theme.MEDITATION, "Letters to Lucilius 2"),
        Quote("Withdraw into yourself, as far as you can.", "Seneca", Theme.MEDITATION, "Letters to Lucilius 7"),
        Quote("Let us go to sleep with joy and gladness; let us say: \"I have lived; the course which Fortune set for me is finished.\"", "Seneca", Theme.RELAXATION, "Letters to Lucilius 12"),
        Quote("Happy indeed we live, friendly amidst the hostile. Amidst hostile men we dwell free from hatred.", "Dhammapada", Theme.HAPPINESS, "Dhammapada 197"),
        Quote("Victory breeds hatred. The defeated live in pain. Happily the peaceful live, giving up victory and defeat.", "Dhammapada", Theme.PEACE, "Dhammapada 201"),
        Quote("Just as a solid rock is not shaken by the storm, even so the wise are not affected by praise or blame.", "Dhammapada", Theme.PEACE, "Dhammapada 81"),
        Quote("It is good to tame the mind, which is difficult to hold in and flighty, rushing wherever it listeth; a tamed mind brings happiness.", "Dhammapada", Theme.MEDITATION, "Dhammapada 35"),
        Quote("Though one should conquer in battle a thousand times a thousand men, he who conquers himself is the greatest conqueror.", "Dhammapada", Theme.SELF_ACTUALIZATION, "Dhammapada 103"),
        Quote("Self is the lord of self, who else could be the lord?", "Dhammapada", Theme.SELF_ACTUALIZATION, "Dhammapada 160"),
        Quote("Irrigators channel waters; fletchers straighten arrows; carpenters bend wood; the wise master themselves.", "Dhammapada", Theme.MEDITATION, "Dhammapada 80"),
        Quote("Happy indeed we live, free from ailments among the ailing.", "Dhammapada", Theme.HEALING, "Dhammapada 198"),
        Quote("Let yourself be silently drawn by the strange pull of what you really love. It will not lead you astray.", "Rumi", Theme.SELF_ACTUALIZATION, "The Essential Rumi (Coleman Barks, 1995)"),
        Quote("Out beyond ideas of wrongdoing and rightdoing, there is a field. I'll meet you there.", "Rumi", Theme.PEACE, "The Essential Rumi (Coleman Barks, 1995)"),
        Quote("Don't grieve. Anything you lose comes round in another form.", "Rumi", Theme.HEALING, "The Essential Rumi (Coleman Barks, 1995)"),
        Quote("I love to be alone. I never found the companion that was so companionable as solitude.", "Henry David Thoreau", Theme.PEACE, "Walden, Solitude"),
        Quote("Be rather a Columbus to whole new continents and worlds within you.", "Henry David Thoreau", Theme.SELF_ACTUALIZATION, "Walden, Conclusion"),
        Quote("In wildness is the preservation of the world.", "Henry David Thoreau", Theme.RELAXATION, "Walking (1862)"),
        Quote("I went to the woods because I wished to live deliberately, to front only the essential facts of life.", "Henry David Thoreau", Theme.MEDITATION, "Walden, Where I Lived"),
        Quote("Let us spend one day as deliberately as Nature.", "Henry David Thoreau", Theme.MEDITATION, "Walden, Where I Lived"),
        Quote("To affect the quality of the day, that is the highest of arts.", "Henry David Thoreau", Theme.SELF_ACTUALIZATION, "Walden, Where I Lived"),
        Quote("If a man does not keep pace with his companions, perhaps it is because he hears a different drummer.", "Henry David Thoreau", Theme.SELF_ACTUALIZATION, "Walden, Conclusion"),
        Quote("How vain it is to sit down to write when you have not stood up to live.", "Henry David Thoreau", Theme.SELF_ACTUALIZATION, "Journal, 1851"),
        Quote("I would rather sit on a pumpkin and have it all to myself, than be crowded on a velvet cushion.", "Henry David Thoreau", Theme.HAPPINESS, "Walden, Economy"),
        Quote("Nature never wears a mean appearance.", "Ralph Waldo Emerson", Theme.PEACE, "Nature (1836)"),
        Quote("The Earth laughs in flowers.", "Ralph Waldo Emerson", Theme.HAPPINESS, "Hamatreya (1847)"),
        Quote("In the woods, we return to reason and faith.", "Ralph Waldo Emerson", Theme.RELAXATION, "Nature (1836)"),
        Quote("I loafe and invite my soul, I lean and loafe at my ease observing a spear of summer grass.", "Walt Whitman", Theme.BLISS, "Leaves of Grass: Song of Myself"),
        Quote("I believe a leaf of grass is no less than the journey-work of the stars.", "Walt Whitman", Theme.MEDITATION, "Leaves of Grass: Song of Myself"),
        Quote("I am satisfied. I see, dance, laugh, sing.", "Walt Whitman", Theme.HAPPINESS, "Leaves of Grass: Song of Myself"),
        Quote("Afoot and light-hearted I take to the open road, healthy, free, the world before me.", "Walt Whitman", Theme.BLISS, "Leaves of Grass: Song of the Open Road"),
        Quote("To me every hour of the light and dark is a miracle.", "Walt Whitman", Theme.GRATITUDE, "Leaves of Grass: Miracles"),
        Quote("Every cubic inch of space is a miracle.", "Walt Whitman", Theme.GRATITUDE, "Leaves of Grass: Miracles"),
        Quote("Let me not pray to be sheltered from dangers but to be fearless in facing them.", "Rabindranath Tagore", Theme.SELF_ACTUALIZATION, "Fruit-Gathering (1916)"),
        Quote("The butterfly counts not months but moments, and has time enough.", "Rabindranath Tagore", Theme.HAPPINESS, "Fireflies (1928)"),
        Quote("Clouds come floating into my life, no longer to carry rain or usher storm, but to add color to my sunset sky.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"),
        Quote("Faith is the bird that feels the light and sings when the dawn is still dark.", "Rabindranath Tagore", Theme.HEALING, "Stray Birds (1916)"),
        Quote("The same stream of life that runs through my veins night and day runs through the world and dances in rhythmic measures.", "Rabindranath Tagore", Theme.BLISS, "Gitanjali 69"),
        Quote("Let life be beautiful like summer flowers and death like autumn leaves.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"),
        Quote("The world speaks to me in pictures, my soul answers in music.", "Rabindranath Tagore", Theme.BLISS, "Stray Birds (1916)"),
        Quote("Trees are the earth's endless effort to speak to the listening heaven.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"),
        Quote("I have had my invitation to this world's festival, and thus my life has been blessed.", "Rabindranath Tagore", Theme.GRATITUDE, "Gitanjali 16"),
        Quote("Where the mind is without fear and the head is held high; where knowledge is free.", "Rabindranath Tagore", Theme.SELF_ACTUALIZATION, "Gitanjali 35"),
        Quote("Light, my light, the world-filling light, the eye-kissing light, heart-sweetening light!", "Rabindranath Tagore", Theme.BLISS, "Gitanjali 57"),
        Quote("Mist, like love, plays upon the heart of the hills and brings out surprises of beauty.", "Rabindranath Tagore", Theme.RELAXATION, "Stray Birds (1916)"),
        Quote("Your pain is the breaking of the shell that encloses your understanding.", "Kahlil Gibran", Theme.HEALING, "The Prophet: On Pain"),
        Quote("Your joy is your sorrow unmasked.", "Kahlil Gibran", Theme.HEALING, "The Prophet: On Joy and Sorrow"),
        Quote("Work is love made visible.", "Kahlil Gibran", Theme.SELF_ACTUALIZATION, "The Prophet: On Work"),
        Quote("Forget not that the earth delights to feel your bare feet and the winds long to play with your hair.", "Kahlil Gibran", Theme.RELAXATION, "The Prophet: On Clothes"),
        Quote("Say not, \"I have found the truth,\" but rather, \"I have found a truth.\"", "Kahlil Gibran", Theme.SELF_ACTUALIZATION, "The Prophet: On Self-Knowledge"),
        Quote("The deeper that sorrow carves into your being, the more joy you can contain.", "Kahlil Gibran", Theme.HEALING, "The Prophet: On Joy and Sorrow"),
        Quote("In the dew of little things the heart finds its morning and is refreshed.", "Kahlil Gibran", Theme.GRATITUDE, "The Prophet: On Friendship"),
        Quote("Let there be spaces in your togetherness, and let the winds of the heavens dance between you.", "Kahlil Gibran", Theme.PEACE, "The Prophet: On Marriage"),
        Quote("Beauty is eternity gazing at itself in a mirror.", "Kahlil Gibran", Theme.BLISS, "The Prophet: On Beauty"),
        Quote("The soul unfolds itself, like a lotus of countless petals.", "Kahlil Gibran", Theme.SELF_ACTUALIZATION, "The Prophet: On Self-Knowledge"),
        Quote("Say not, \"I have found the path of the soul.\" Say rather, \"I have met the soul walking upon my path.\"", "Kahlil Gibran", Theme.SELF_ACTUALIZATION, "The Prophet: On Self-Knowledge"),
        Quote("Love one another, but make not a bond of love: let it rather be a moving sea between the shores of your souls.", "Kahlil Gibran", Theme.HAPPINESS, "The Prophet: On Marriage"),
        Quote("You have the right to work, but never to the fruit of work.", "Bhagavad Gita", Theme.PEACE, "Bhagavad Gita 2.47"),
        Quote("Let a man lift himself by his own self; let him not lower himself.", "Bhagavad Gita", Theme.SELF_ACTUALIZATION, "Bhagavad Gita 6.5"),
        Quote("Yoga is skill in action.", "Bhagavad Gita", Theme.MEDITATION, "Bhagavad Gita 2.50"),
        Quote("Perform your duty and abandon all attachment to success or failure. Such evenness of mind is called yoga.", "Bhagavad Gita", Theme.MEDITATION, "Bhagavad Gita 2.48"),
        Quote("Going to the mountains is going home.", "John Muir", Theme.PEACE, "Our National Parks (1901)"),
        Quote("The clearest way into the Universe is through a forest wilderness.", "John Muir", Theme.MEDITATION, "John of the Mountains (1938)"),
        Quote("Nature never did betray the heart that loved her.", "William Wordsworth", Theme.HEALING, "Tintern Abbey"),
        Quote("Come forth into the light of things, let Nature be your teacher.", "William Wordsworth", Theme.RELAXATION, "The Tables Turned"),
        Quote("My heart leaps up when I behold a rainbow in the sky.", "William Wordsworth", Theme.GRATITUDE, "My Heart Leaps Up"),
        Quote("Beauty is truth, truth beauty; that is all ye know on earth, and all ye need to know.", "John Keats", Theme.BLISS, "Ode on a Grecian Urn"),
        Quote("To see a world in a grain of sand and a heaven in a wild flower, hold infinity in the palm of your hand and eternity in an hour.", "William Blake", Theme.BLISS, "Auguries of Innocence"),
        Quote("What is this life if, full of care, we have no time to stand and stare?", "W. H. Davies", Theme.RELAXATION, "Leisure (1911)"),
        Quote("That it will never come again is what makes life so sweet.", "Emily Dickinson", Theme.GRATITUDE, "Poem 1741"),
        Quote("Forever is composed of Nows.", "Emily Dickinson", Theme.MEDITATION, "Poem 624"),
        Quote("To make a prairie it takes a clover and one bee, and revery.", "Emily Dickinson", Theme.RELAXATION, "Poem 1755"),
        Quote("Gratitude is not only the greatest of virtues, but the parent of all the others.", "Cicero", Theme.GRATITUDE, "Pro Plancio 80"),
        Quote("Do not spoil what you have by desiring what you have not; remember that what you now have was once among the things you only hoped for.", "Epicurus", Theme.GRATITUDE, "Vatican Sayings 35"),
        Quote("We can only be said to be alive in those moments when our hearts are conscious of our treasures.", "Thornton Wilder", Theme.GRATITUDE, "The Skin of Our Teeth (1942)"),
        Quote("The world will never starve for want of wonders, but only for want of wonder.", "G. K. Chesterton", Theme.GRATITUDE, "Tremendous Trifles (1909)"),
        Quote("Reckon up the chief of the blessings you do possess, and then thankfully remember how you would crave for them if they were not yours.", "Marcus Aurelius", Theme.GRATITUDE, "Meditations 7.27"),
        Quote("There is no duty we so much underrate as the duty of being happy.", "Robert Louis Stevenson", Theme.HAPPINESS, "Virginibus Puerisque (1881)"),
        Quote("Oh be swift to love, make haste to be kind.", "Henri-Frédéric Amiel", Theme.HAPPINESS, "Journal intime (1882)"),
        Quote("Happy indeed we live, free from avarice among the avaricious.", "Dhammapada", Theme.HAPPINESS, "Dhammapada 199"),
        Quote("I'm so glad I live in a world where there are Octobers.", "L. M. Montgomery", Theme.HAPPINESS, "Anne of Green Gables"),
        Quote("It's been my experience that you can nearly always enjoy things if you make up your mind firmly that you will.", "L. M. Montgomery", Theme.HAPPINESS, "Anne of Green Gables"),
        Quote("There is nothing like staying at home for real comfort.", "Jane Austen", Theme.RELAXATION, "Emma"),
        Quote("There is nothing—absolutely nothing—half so much worth doing as simply messing about in boats.", "Kenneth Grahame", Theme.RELAXATION, "The Wind in the Willows, ch. 1"),
        Quote("And I shall have some peace there, for peace comes dropping slow, dropping from the veils of the morning to where the cricket sings.", "W. B. Yeats", Theme.RELAXATION, "The Lake Isle of Innisfree"),
        Quote("Season of mists and mellow fruitfulness, close bosom-friend of the maturing sun.", "John Keats", Theme.BLISS, "To Autumn"),
        Quote("I have a great deal of company in my house; especially in the morning, when nobody calls.", "Henry David Thoreau", Theme.RELAXATION, "Walden, Solitude"),
        Quote("Come, read to me some poem, some simple and heartfelt lay, that shall soothe this restless feeling, and banish the thoughts of day.", "Henry Wadsworth Longfellow", Theme.RELAXATION, "The Day Is Done"),
        Quote("And the cares that infest the day shall fold their tents, like the Arabs, and as silently steal away.", "Henry Wadsworth Longfellow", Theme.RELAXATION, "The Day Is Done"),
        Quote("They flash upon that inward eye which is the bliss of solitude.", "William Wordsworth", Theme.BLISS, "I Wandered Lonely as a Cloud"),
        Quote("There is a pleasure in the pathless woods, there is a rapture on the lonely shore.", "Lord Byron", Theme.BLISS, "Childe Harold's Pilgrimage, Canto 4"),
        Quote("He who binds to himself a joy does the winged life destroy; but he who kisses the joy as it flies lives in eternity's sun rise.", "William Blake", Theme.BLISS, "Eternity"),
        Quote("Tread softly because you tread on my dreams.", "W. B. Yeats", Theme.PEACE, "Aedh Wishes for the Cloths of Heaven"),
        Quote("The woods are lovely, dark and deep, but I have promises to keep, and miles to go before I sleep.", "Robert Frost", Theme.PEACE, "Stopping by Woods on a Snowy Evening (1923)"),
        Quote("I think I could turn and live with animals, they are so placid and self-contained.", "Walt Whitman", Theme.PEACE, "Leaves of Grass: Song of Myself"),
        Quote("Think only of the past as its remembrance gives you pleasure.", "Jane Austen", Theme.HEALING, "Pride and Prejudice, ch. 58"),
        Quote("If Winter comes, can Spring be far behind?", "Percy Bysshe Shelley", Theme.HEALING, "Ode to the West Wind"),
        Quote("Nature always wears the colors of the spirit.", "Ralph Waldo Emerson", Theme.MEDITATION, "Nature (1836)"),
        Quote("Heard melodies are sweet, but those unheard are sweeter.", "John Keats", Theme.MEDITATION, "Ode on a Grecian Urn"),
        Quote("Who can make the muddy water clear? Let it be still, and it will gradually become clear.", "Lao Tzu", Theme.PEACE, "Tao Te Ching 15"),
        Quote("Half of what I say is meaningless, but I say it so that the other half may reach you.", "Kahlil Gibran", Theme.MEDITATION, "Sand and Foam (1926)"),
        Quote("If you shed tears when you miss the sun, you also miss the stars.", "Rabindranath Tagore", Theme.HEALING, "Stray Birds (1916)"),
        Quote("Sorrow is hushed into peace in my heart like the evening among the trees.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"),
        Quote("Do not say, \"It is morning,\" and dismiss it with a name of yesterday.", "Rabindranath Tagore", Theme.MEDITATION, "Stray Birds (1916)"),
        Quote("All good things are wild and free.", "Henry David Thoreau", Theme.RELAXATION, "Walking (1862)"),
        Quote("Why should we be in such desperate haste to succeed and in such desperate enterprises?", "Henry David Thoreau", Theme.RELAXATION, "Walden, Conclusion"),
        Quote("Home is the sailor, home from sea, and the hunter home from the hill.", "Robert Louis Stevenson", Theme.PEACE, "Requiem (Underwoods, 1887)"),
        Quote("If I can stop one heart from breaking, I shall not live in vain.", "Emily Dickinson", Theme.HEALING, "Poem 919"),
        Quote("Look'd up in perfect silence at the stars.", "Walt Whitman", Theme.MEDITATION, "When I Heard the Learn'd Astronomer"),
        Quote("Getting and spending, we lay waste our powers.", "William Wordsworth", Theme.MEDITATION, "The World Is Too Much with Us"),
    )
}
