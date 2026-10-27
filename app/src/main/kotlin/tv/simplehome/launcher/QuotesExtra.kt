package tv.simplehome.launcher

/**
 * Additional quotes merged from new-quotes.tsv. Each line's trailing comment is its verification id (not shown on screen):
 * GUTENBERG#n = checked verbatim against Project Gutenberg ebook n (public-domain texts and translations, so archaic
 * wording such as "thee" and "thy" is kept as printed); WIKIQUOTE:<page> = checked as present on that Wikiquote page.
 * A random sample was verified independently: 12/12 Gutenberg rows verbatim, 6/6 Wikiquote rows present.
 */
internal object QuotesExtra {
    val DAY: List<Quote> = listOf(
        Quote("These roses under my window make no reference to former roses or to better ones; they are for what they are.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "Essays, First Series: Self-Reliance (1841)"), // GUTENBERG#2944
        Quote("To believe your own thought, to believe that what is true for you in your private heart is true for all men, that is genius.", "Ralph Waldo Emerson", Theme.CONFIDENCE, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("If it be right and honest to be spoken or done, undervalue not thyself so much, as to be discouraged from it.", "Marcus Aurelius", Theme.CONFIDENCE, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("Here shall no end be hindered, no hope marred, no loss be feared: faith, yea, a little faith, shall save thee from the anguish of thy dread.", "Bhagavad Gita", Theme.CONFIDENCE, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("Better thine own work is, though done with fault, than doing others' work, ev'n excellently.", "Bhagavad Gita", Theme.CONFIDENCE, "Song Celestial (Arnold trans.), Ch. XVIII"), // GUTENBERG#2388
        Quote("Put all imaginations away, and stand fast in that which you are.", "Kabir", Theme.CONFIDENCE, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("Tiny grass, your steps are small, but you possess the earth under your tread.", "Rabindranath Tagore", Theme.CONFIDENCE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("The stars are not afraid to appear like fireflies.", "Rabindranath Tagore", Theme.CONFIDENCE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("From now there shall be no fear left for me in this world, and thou shalt be victorious in all my strife.", "Rabindranath Tagore", Theme.CONFIDENCE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("I know that my heart would open like a flower; that my life has filled itself at a hidden fountain.", "Rabindranath Tagore", Theme.CONFIDENCE, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("Stand as a rock; you are indestructible.", "Swami Vivekananda", Theme.CONFIDENCE, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("All the powers in the universe are already ours. It is we who have put our hands before our eyes and cry that it is dark.", "Swami Vivekananda", Theme.CONFIDENCE, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Be a hero. Always say, \"I have no fear.\" Tell this to everyone, \"Have no fear.\"", "Swami Vivekananda", Theme.CONFIDENCE, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("The best performances are accomplished when you are relaxed and free of doubt.", "A. P. J. Abdul Kalam", Theme.CONFIDENCE, "p. 31"), // WIKIQUOTE:A. P. J. Abdul Kalam
        Quote("I learned that courage was not the absence of fear, but the triumph over it.", "Nelson Mandela", Theme.CONFIDENCE, "[[w:Long Walk to Freedom (book);Long Walk to Freedom]] (1995)"), // WIKIQUOTE:Nelson Mandela
        Quote("Develop enough courage so that you can stand up for yourself and then stand up for somebody else.", "Maya Angelou", Theme.CONFIDENCE, "in Rainbow in the Cloud: The Wisdom and Spirit of Maya Angelou (2014), p. 68"), // WIKIQUOTE:Maya Angelou
        Quote("Avoiding danger is no safer in the long run than outright exposure. The fearful are caught as often as the bold.", "Helen Keller", Theme.CONFIDENCE, "Let Us Have Faith (1940)"), // WIKIQUOTE:Helen Keller
        Quote("All can be done if the god-touch is there.", "Sri Aurobindo", Theme.CONFIDENCE, "Book One : The Book Of Beginnings"), // WIKIQUOTE:Sri Aurobindo
        Quote("As many kinds of wreaths can be made from a heap of flowers, so many good things may be achieved by a mortal when once he is born.", "Dhammapada", Theme.INSPIRATION, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("Earnest among the thoughtless, awake among the sleepers, the wise man advances like a racer, leaving behind the hack.", "Dhammapada", Theme.INSPIRATION, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("I wish to be virtuous, and lo! virtue is at hand.", "Confucius", Theme.INSPIRATION, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("A man is a method, a progressive arrangement; a selecting principle.", "Ralph Waldo Emerson", Theme.INSPIRATION, "Essays, First Series: Spiritual Laws (1841)"), // GUTENBERG#2944
        Quote("Time is but the stream I go a-fishing in.", "Henry David Thoreau", Theme.INSPIRATION, "Walden (1854)"), // GUTENBERG#205
        Quote("Every morning was a cheerful invitation to make my life of equal simplicity, and I may say innocence, with Nature herself.", "Henry David Thoreau", Theme.INSPIRATION, "Walden (1854)"), // GUTENBERG#205
        Quote("To be awake is to be alive.", "Henry David Thoreau", Theme.INSPIRATION, "Walden (1854)"), // GUTENBERG#205
        Quote("If you have built castles in the air, your work need not be lost; that is where they should be. Now put the foundations under them.", "Henry David Thoreau", Theme.INSPIRATION, "Walden (1854)"), // GUTENBERG#205
        Quote("We must learn to reawaken and keep ourselves awake, not by mechanical aids, but by an infinite expectation of the dawn.", "Henry David Thoreau", Theme.INSPIRATION, "Walden (1854)"), // GUTENBERG#205
        Quote("Do that which is assigned you, and you cannot hope too much or dare too much.", "Ralph Waldo Emerson", Theme.INSPIRATION, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("Abide in the simple and noble regions of thy life, obey thy heart and thou shalt reproduce the Foreworld again.", "Ralph Waldo Emerson", Theme.INSPIRATION, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("Let each play his part in all he finds to do, with unyoked soul.", "Bhagavad Gita", Theme.INSPIRATION, "Song Celestial (Arnold trans.), Ch. III"), // GUTENBERG#2388
        Quote("Whoso performeth, diligent, content, the work allotted him, whate'er it be, lays hold of perfectness!", "Bhagavad Gita", Theme.INSPIRATION, "Song Celestial (Arnold trans.), Ch. XVIII"), // GUTENBERG#2388
        Quote("Hope for Him whilst you live, know whilst you live, understand whilst you live: for in life deliverance abides.", "Kabir", Theme.INSPIRATION, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("I cannot choose the best. The best chooses me.", "Rabindranath Tagore", Theme.INSPIRATION, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Never be afraid of the moments—thus sings the voice of the everlasting.", "Rabindranath Tagore", Theme.INSPIRATION, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Wayside grass, love the star, then your dreams will come out in flowers.", "Rabindranath Tagore", Theme.INSPIRATION, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Give me the strength to raise my mind high above daily trifles.", "Rabindranath Tagore", Theme.INSPIRATION, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("When old words die out on the tongue, new melodies break forth from the heart; and where the old tracks are lost, new country is revealed with its wonders.", "Rabindranath Tagore", Theme.INSPIRATION, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Come out of thy meditations and leave aside thy flowers and incense! What harm is there if thy clothes become tattered and stained?", "Rabindranath Tagore", Theme.INSPIRATION, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("He is there where the tiller is tilling the hard ground and where the pathmaker is breaking stones.", "Rabindranath Tagore", Theme.INSPIRATION, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Kindle the lamp of love with thy life.", "Rabindranath Tagore", Theme.INSPIRATION, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Be ready to launch forth, my heart! and let those linger who must.", "Rabindranath Tagore", Theme.INSPIRATION, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("Burst your sheath, my heart, and come forth!", "Rabindranath Tagore", Theme.INSPIRATION, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("He does not stay to count his gain or to mourn his loss; his heart beats the drum for his march.", "Rabindranath Tagore", Theme.INSPIRATION, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("The desire of the bud is for the night and dew, but the blown flower cries for the freedom of light.", "Rabindranath Tagore", Theme.INSPIRATION, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("Arise, awake and stop not till the goal is reached.", "Swami Vivekananda", Theme.INSPIRATION, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Take up an idea, devote yourself to it, struggle on in patience, and the sun will rise for you.", "Swami Vivekananda", Theme.INSPIRATION, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Give me few men and women who are pure and selfless and I shall shake the world.", "Swami Vivekananda", Theme.INSPIRATION, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Fill the brain with high thoughts, highest ideals, place them day and night before you, and out of that will come great work.", "Swami Vivekananda", Theme.INSPIRATION, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Come out into the broad light of day, come out from the little narrow paths, for how can the infinite soul rest content to live and die in small ruts?", "Swami Vivekananda", Theme.INSPIRATION, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("A small body of determined spirits fired by an unquenchable faith in their mission can alter the course of history.", "Mahatma Gandhi", Theme.INSPIRATION, "Harijan (19 November 1938) p. 343"), // WIKIQUOTE:Mahatma Gandhi
        Quote("You may never know what results come of your actions. But if you do nothing, there will be no result.", "Mahatma Gandhi", Theme.INSPIRATION, "Speech at a prayer meeting, New Delhi, 1947; also in The Collected Works of Mahatma Gandhi, Vol. 90, p. 55."), // WIKIQUOTE:Mahatma Gandhi
        Quote("You have to dream before your dreams can come true.", "A. P. J. Abdul Kalam", Theme.INSPIRATION, "NDTV (27 July 2015 obituary citing his memorable quotes)"), // WIKIQUOTE:A. P. J. Abdul Kalam
        Quote("Great dreams of great dreamers are always transcended.", "A. P. J. Abdul Kalam", Theme.INSPIRATION, "p. 161"), // WIKIQUOTE:A. P. J. Abdul Kalam
        Quote("We must seek to do our life's work so well that nobody could do it better.", "Martin Luther King Jr.", Theme.INSPIRATION, "The Rising Tide of Racial Consciousness (1960)"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("The arc of the moral universe is long, but it bends toward justice.", "Martin Luther King Jr.", Theme.INSPIRATION, "Keep Moving From This Mountain (1965)"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("Education is the most powerful weapon we can use to change the world.", "Nelson Mandela", Theme.INSPIRATION, "\"[http://db.nelsonmandela.org/speeches/pub_view.asp?pg=item&ItemID=NMS909&txtstr=education%20is%20the%20most%20powerful Lighting your way to"), // WIKIQUOTE:Nelson Mandela
        Quote("My mission in life is not merely to survive, but to thrive; and to do so with some passion, some compassion, some humor, and some style.", "Maya Angelou", Theme.INSPIRATION, "Shared on her [https://www.facebook.com/MayaAngelou/posts/10150251846629796 Facebook page], July 4, 2011"), // WIKIQUOTE:Maya Angelou
        Quote("Life is going to give you just what you put in it. Put your whole heart in everything you do, and pray, then you can wait.", "Maya Angelou", Theme.INSPIRATION, "[[w:I Know Why the Caged Bird Sings;I Know Why the Caged Bird Sings]] (1969)"), // WIKIQUOTE:Maya Angelou
        Quote("Do not wait for leaders; do it alone, person to person.", "Mother Teresa", Theme.INSPIRATION, "Mary Alice Warner, ‎Dayna Beilenson (1987) Women of faith and spirit: their words & thoughts, p. 42"), // WIKIQUOTE:Mother Teresa
        Quote("One can never consent to creep when one feels an impulse to soar.", "Helen Keller", Theme.INSPIRATION, "[http://www.afb.org/mylife/book.asp?ch=P3Ch4 Address to the American Association to Promote the Teaching of Speech to the Deaf at Mt. Airy,"), // WIKIQUOTE:Helen Keller
        Quote("Be a loner. That gives you time to wonder, to search for the truth. Have holy curiosity. Make your life worth living.", "Albert Einstein", Theme.INSPIRATION, "p. 142"), // WIKIQUOTE:Albert Einstein
        Quote("Life was meant to be lived, and curiosity must be kept alive. One must never, for whatever reason, turn his back on life.", "Eleanor Roosevelt", Theme.INSPIRATION, "Preface (December 1960) to The Autobiography of Eleanor Roosevelt (1961), p. xix"), // WIKIQUOTE:Eleanor Roosevelt
        Quote("They who know truth in truth, and untruth in untruth, arrive at truth, and follow true desires.", "Dhammapada", Theme.INTEGRITY, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("Like a beautiful flower, full of colour and full of scent, are the fine and fruitful words of him who acts accordingly.", "Dhammapada", Theme.INTEGRITY, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("That deed is well done of which a man does not repent, and the reward of which he receives gladly and cheerfully.", "Dhammapada", Theme.INTEGRITY, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("Let each man direct himself first to what is proper, then let him teach others; thus a wise man will not suffer.", "Dhammapada", Theme.INTEGRITY, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("If a man would hasten towards the good, he should keep his thought away from evil.", "Dhammapada", Theme.INTEGRITY, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("The superior man thinks of virtue; the small man thinks of comfort.", "Confucius", Theme.INTEGRITY, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("The mind of the superior man is conversant with righteousness; the mind of the mean man is conversant with gain.", "Confucius", Theme.INTEGRITY, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("If the will be set on virtue, there will be no practice of wickedness.", "Confucius", Theme.INTEGRITY, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("I do not know how a man without truthfulness is to get on.", "Confucius", Theme.INTEGRITY, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("The superior man wishes to be slow in his speech and earnest in his conduct.", "Confucius", Theme.INTEGRITY, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("The superior man, in the world, does not set his mind either for anything, or against anything; what is right he will follow.", "Confucius", Theme.INTEGRITY, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("What I must do is all that concerns me, not what the people think.", "Ralph Waldo Emerson", Theme.INTEGRITY, "Essays, First Series: Self-Reliance (1841)"), // GUTENBERG#2944
        Quote("The reward of a thing well done is to have done it.", "Ralph Waldo Emerson", Theme.INTEGRITY, "Essays, Second Series: New England Reformers (1844)"), // GUTENBERG#2945
        Quote("Let us have faith that right makes might, and in that faith let us, to the end, dare to do our duty as we understand it.", "Abraham Lincoln", Theme.INTEGRITY, "Cooper Union Address (1860)"), // GUTENBERG#14721
        Quote("It is alike your interest, and mine, and all men's, however long we have dwelt in lies, to live in truth.", "Ralph Waldo Emerson", Theme.INTEGRITY, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("I ought to go upright and vital, and speak the rude truth in all ways.", "Ralph Waldo Emerson", Theme.INTEGRITY, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("When a man speaks the truth in the spirit of truth, his eye is as clear as the heavens.", "Ralph Waldo Emerson", Theme.INTEGRITY, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("When we discern justice, when we discern truth, we do nothing of ourselves, but allow a passage to its beams.", "Ralph Waldo Emerson", Theme.INTEGRITY, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("Humbleness, uprightness, heed to injure nought which lives, truthfulness, slowness unto wrath.", "Bhagavad Gita", Theme.INTEGRITY, "Song Celestial (Arnold trans.), Ch. XVI"), // GUTENBERG#2388
        Quote("He who acts free from self-seeking, humble, resolute, steadfast, in good or evil hap the same, content to do aright.", "Bhagavad Gita", Theme.INTEGRITY, "Song Celestial (Arnold trans.), Ch. XVIII"), // GUTENBERG#2388
        Quote("I speak truth, for I have accepted truth in life; I am now attached to truth, I have swept all tinsel away.", "Kabir", Theme.INTEGRITY, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("Death and decay cast over its face momentary shadows and pass on; they leave no marks of their steps, and truth remains fresh and young.", "Rabindranath Tagore", Theme.INTEGRITY, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("To be outspoken is easy when you do not wait to speak the complete truth.", "Rabindranath Tagore", Theme.INTEGRITY, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("If you shut your door to all errors truth will be shut out.", "Rabindranath Tagore", Theme.INTEGRITY, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("I shall ever try to keep all untruths out from my thoughts, knowing that thou art that truth which has kindled the light of reason in my mind.", "Rabindranath Tagore", Theme.INTEGRITY, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("I shall ever try to drive all evils away from my heart and keep my love in flower.", "Rabindranath Tagore", Theme.INTEGRITY, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Only let me make my life simple and straight, like a flute of reed for thee to fill with music.", "Rabindranath Tagore", Theme.INTEGRITY, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("What the world wants is character. The world is in need of those whose life is one burning love, selfless.", "Swami Vivekananda", Theme.INTEGRITY, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Watch people do their most common actions; these are indeed the things that will tell you the real character of a great person.", "Swami Vivekananda", Theme.INTEGRITY, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Truth never damages a cause that is just.", "Mahatma Gandhi", Theme.INTEGRITY, "Harijan, 9 December 1939, in The Collected Works of Mahatma Gandhi, Vol. 70, p. 40."), // WIKIQUOTE:Mahatma Gandhi
        Quote("One man cannot do right in one department of life whilst he is occupied in doing wrong in any other department. Life is one indivisible whole.", "Mahatma Gandhi", Theme.INTEGRITY, "Young India (27 January 1927)"), // WIKIQUOTE:Mahatma Gandhi
        Quote("The time is always right to do what's right.", "Martin Luther King Jr.", Theme.INTEGRITY, "\"The Future of Integration\", address in Finney Chapel at Oberlin College (22 October 1964), as quoted in \"When MLK came to Oberlin\" by Cindy"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("Those who conduct themselves with morality, integrity and consistency need not fear the forces of inhumanity and cruelty.", "Nelson Mandela", Theme.INTEGRITY, "Nelson Mandela on integrity, At the British Red Cross Humanity Lecture, Queen Elizabeth Conference Centre, London, England (10 July 2003). S"), // WIKIQUOTE:Nelson Mandela
        Quote("You don't have to think about doing the right thing. If you're for the right thing, then you do it without thinking.", "Maya Angelou", Theme.INTEGRITY, "Quoting her mother's statement after her son's birth"), // WIKIQUOTE:Maya Angelou
        Quote("We are called upon not to be successful, but to be faithful.", "Mother Teresa", Theme.INTEGRITY, "To Navin Chawla, her authorized biographer, as reported in a speech of tribute delivered at UNESCO on the centenary of her birthday, in Pari"), // WIKIQUOTE:Mother Teresa
        Quote("Who knows, perhaps one day people will listen more to the little piece of God, which we call a conscience, than to their own desires.", "Anne Frank", Theme.INTEGRITY, "Ending lines of Cady's Life, 4th August 1944"), // WIKIQUOTE:Anne Frank
        Quote("Only a man's character is the real criterion of worth.", "Eleanor Roosevelt", Theme.INTEGRITY, "My Day (1935–1962)"), // WIKIQUOTE:Eleanor Roosevelt
        Quote("Truth is generally the best vindication against slander.", "Abraham Lincoln", Theme.INTEGRITY, "Letter to Edwin Stanton (14 July 1864); published in Abraham Lincoln: A History (1890) by John Hay"), // WIKIQUOTE:Abraham Lincoln
        Quote("If a man speaks or acts with a pure thought, happiness follows him, like a shadow that never leaves him.", "Dhammapada", Theme.POSITIVITY, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("We judge of a man's wisdom by his hope, knowing that the perception of the inexhaustibleness of nature is an immortal youth.", "Ralph Waldo Emerson", Theme.POSITIVITY, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("Wheresoever thou mayest live, there it is in thy power to live well and happy.", "Marcus Aurelius", Theme.POSITIVITY, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("No heart that holds one right desire treadeth the road of loss!", "Bhagavad Gita", Theme.POSITIVITY, "Song Celestial (Arnold trans.), Ch. VI"), // GUTENBERG#2388
        Quote("The wonder is not that there should be obstacles and sufferings in this world, but that there should be law and order, beauty and joy, goodness and love.", "Rabindranath Tagore", Theme.POSITIVITY, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("His own mornings are new surprises to God.", "Rabindranath Tagore", Theme.POSITIVITY, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Every child comes with the message that God is not yet discouraged of man.", "Rabindranath Tagore", Theme.POSITIVITY, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Night's darkness is a bag that bursts with the gold of the dawn.", "Rabindranath Tagore", Theme.POSITIVITY, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Dark clouds become heaven's flowers when kissed by light.", "Rabindranath Tagore", Theme.POSITIVITY, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("The world has opened its heart of light in the morning. Come out, my heart, with thy love to meet it.", "Rabindranath Tagore", Theme.POSITIVITY, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("The morning will surely come, the darkness will vanish, and thy voice pour down in golden streams breaking through the sky.", "Rabindranath Tagore", Theme.POSITIVITY, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("There comes the morning with the golden basket in her right hand bearing the wreath of beauty, silently to crown the earth.", "Rabindranath Tagore", Theme.POSITIVITY, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("In the morning I woke up and found my garden full with wonders of flowers.", "Rabindranath Tagore", Theme.POSITIVITY, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("The morning light has flooded my eyes.", "Rabindranath Tagore", Theme.POSITIVITY, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Let my joy in life blaze up in fire.", "Rabindranath Tagore", Theme.POSITIVITY, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("Bare your forehead, waiting for the first blessing of light, and sing with the bird of the morning in glad faith.", "Rabindranath Tagore", Theme.POSITIVITY, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("I will meet one day the Life within me, the joy that hides in my life, though the days perplex my path with their idle dust.", "Rabindranath Tagore", Theme.POSITIVITY, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("Open the doors, make room for the morning light.", "Rabindranath Tagore", Theme.POSITIVITY, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("It is the cheerful mind that is persevering. It is the strong mind that hews its way through a thousand difficulties.", "Swami Vivekananda", Theme.POSITIVITY, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("The past was great no doubt, but I sincerely believe that the future will be more glorious still.", "Swami Vivekananda", Theme.POSITIVITY, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("You must not lose faith in humanity. Humanity is an ocean; if a few drops of the ocean are dirty, the ocean does not become dirty.", "Mahatma Gandhi", Theme.POSITIVITY, "Harijan, 10 August 1947, in The Collected Works of Mahatma Gandhi, Vol. 89, p. 137."), // WIKIQUOTE:Mahatma Gandhi
        Quote("The time for the healing of the wounds has come. The moment to bridge the chasms that divide us has come. The time to build is upon us.", "Nelson Mandela", Theme.POSITIVITY, "Inaugural celebration address (1994)"), // WIKIQUOTE:Nelson Mandela
        Quote("When one door of happiness closes, another opens; but often we look so long at the closed door that we do not see the one which has been opened for us.", "Helen Keller", Theme.POSITIVITY, "We Bereaved (1929)"), // WIKIQUOTE:Helen Keller
        Quote("I demand that the world be good, and lo, it obeys. I proclaim the world good, and facts range themselves to prove my proclamation overwhelmingly true.", "Helen Keller", Theme.POSITIVITY, "[[s:Optimism (Keller);Optimism]] (1903)"), // WIKIQUOTE:Helen Keller
        Quote("My knowledge is pessimistic, but my willing and hoping are optimistic.", "Albert Schweitzer", Theme.POSITIVITY, "Epilogue, p. 242"), // WIKIQUOTE:Albert Schweitzer
        Quote("We have more possibilities available in each moment than we realize.", "Thich Nhat Hanh", Theme.POSITIVITY, "As quoted in Visions from Earth (2004) by James Miller"), // WIKIQUOTE:Thích Nhất Hạnh
        Quote("The guarding of what is soft and tender is the secret of strength.", "Lao Tzu", Theme.STRENGTH, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("Always do what you are afraid to do.", "Ralph Waldo Emerson", Theme.STRENGTH, "Essays, First Series: Heroism (1841)"), // GUTENBERG#2944
        Quote("All loss, all pain, is particular; the universe remains to the heart unhurt.", "Ralph Waldo Emerson", Theme.STRENGTH, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("That man alone is wise who keeps the mastery of himself!", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("The soul that with a strong and constant calm takes sorrow and takes joy indifferently, lives in the life undying!", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("Work is more excellent than idleness; the body's life proceeds not, lacking work.", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. III"), // GUTENBERG#2388
        Quote("Put forth full force of Soul in thy own soul!", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. III"), // GUTENBERG#2388
        Quote("The world is strong, but what discerns it stronger, and the mind strongest; and high o'er all the ruling Soul.", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. III"), // GUTENBERG#2388
        Quote("Let each man raise the Self by Soul, not trample down his Self.", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. VI"), // GUTENBERG#2388
        Quote("He who will be master of himself shall win it, if he stoutly strive thereto.", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. VI"), // GUTENBERG#2388
        Quote("Yet may it grow restrained by habit, Prince! By wont of self-command.", "Bhagavad Gita", Theme.STRENGTH, "Song Celestial (Arnold trans.), Ch. VI"), // GUTENBERG#2388
        Quote("Man's freedom is never in being saved troubles, but it is the freedom to take trouble for his own good, to make the trouble an element in his joy.", "Rabindranath Tagore", Theme.STRENGTH, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("There is an impetus of joy in the child which sustains it in its seemingly impossible task.", "Rabindranath Tagore", Theme.STRENGTH, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("The waterfall sings, \"I find my song, when I find my freedom.\"", "Rabindranath Tagore", Theme.STRENGTH, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Not hammerstrokes, but dance of the water sings the pebbles into perfection.", "Rabindranath Tagore", Theme.STRENGTH, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Let your music, like a sword, pierce the noise of the market to its heart.", "Rabindranath Tagore", Theme.STRENGTH, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Give me the strength lightly to bear my joys and sorrows.", "Rabindranath Tagore", Theme.STRENGTH, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Let hard blows of trouble strike fire into my life.", "Rabindranath Tagore", Theme.STRENGTH, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("This world is the great gymnasium where we come to make ourselves strong.", "Swami Vivekananda", Theme.STRENGTH, "Call to the Nation"), // WIKIQUOTE:Swami Vivekananda
        Quote("The remedy for weakness is not brooding over weakness, but thinking of strength. Teach men of strength that is already within them.", "Swami Vivekananda", Theme.STRENGTH, "Call to the Nation"), // WIKIQUOTE:Swami Vivekananda
        Quote("Strength is Life, Weakness is death.", "Swami Vivekananda", Theme.STRENGTH, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Great undertakings are always fraught with many obstacles. It is these obstacles which knock and shape great characters.", "Swami Vivekananda", Theme.STRENGTH, "Brother disciples – Letters of Swami Vivekananda"), // WIKIQUOTE:Swami Vivekananda
        Quote("A few heart-whole, sincere, and energetic men and women can do more in a year than a mob in a century.", "Swami Vivekananda", Theme.STRENGTH, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("This life is a hard fact; work your way through it boldly, though it may be adamantine; no matter, the soul is stronger.", "Swami Vivekananda", Theme.STRENGTH, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Man needs his difficulties because they are necessary to enjoy success.", "A. P. J. Abdul Kalam", Theme.STRENGTH, "p. 90"), // WIKIQUOTE:A. P. J. Abdul Kalam
        Quote("Total commitment is the common denominator among all successful men and women.", "A. P. J. Abdul Kalam", Theme.STRENGTH, "p. 90"), // WIKIQUOTE:A. P. J. Abdul Kalam
        Quote("Climbing to the top demands strength, whether to the top of Mount Everest or to the top of your career.", "A. P. J. Abdul Kalam", Theme.STRENGTH, "p. 90"), // WIKIQUOTE:A. P. J. Abdul Kalam
        Quote("We must keep moving. If you can't fly, run; if you can't run, walk; if you can't walk, crawl; but by all means keep moving.", "Martin Luther King Jr.", Theme.STRENGTH, "Keep Moving from this Mountain (1960)"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("Life is like riding a bicycle. To keep your balance you must keep moving.", "Albert Einstein", Theme.STRENGTH, "Letter to his son Eduard (5 February 1930), as quoted in Walter Isaacson, Einstein: His Life and Universe (2007), p. 367"), // WIKIQUOTE:Albert Einstein
        Quote("Compassion and tolerance are not a sign of weakness, but a sign of strength.", "Dalai Lama", Theme.STRENGTH, "As quoted in Words Of Wisdom: Selected Quotes by His Holiness the Dalai Lama (2001) edited by Margaret Gee, p. 71."), // WIKIQUOTE:Tenzin Gyatso, 14th Dalai Lama
        Quote("Escape brings not the victory and the crown!", "Sri Aurobindo", Theme.STRENGTH, "Book Three : The Book of the Divine Mother"), // WIKIQUOTE:Sri Aurobindo
        Quote("As the bee collects nectar and departs without injuring the flower, or its colour or scent, so let a sage dwell in his village.", "Dhammapada", Theme.VIRTUE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("If a man does what is good, let him do it again; let him delight in it: happiness is the outcome of good.", "Dhammapada", Theme.VIRTUE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("Do not speak harshly to anybody; those who are spoken to will answer thee in the same way.", "Dhammapada", Theme.VIRTUE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("He in whom there is truth, virtue, love, restraint, moderation, he who is free from impurity and is wise, he is called an elder.", "Dhammapada", Theme.VIRTUE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("I have heard that a superior man helps the distressed, but does not add to the wealth of the rich.", "Confucius", Theme.VIRTUE, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("The highest excellence is like that of water.", "Lao Tzu", Theme.VIRTUE, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("To those who are not good to me, I am also good; and thus all get to be good.", "Lao Tzu", Theme.VIRTUE, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("Gentleness is sure to be victorious even in battle, and firmly to maintain its ground.", "Lao Tzu", Theme.VIRTUE, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("Virtue is not left to stand alone.", "Confucius", Theme.VIRTUE, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("With malice toward none, with charity for all, with firmness in the right as God gives us to see the right, let us strive on to finish the work we are in.", "Abraham Lincoln", Theme.VIRTUE, "Second Inaugural Address (1865)"), // GUTENBERG#8
        Quote("Never a magnanimity fell to the ground, but there is some heart to greet and accept it unexpectedly.", "Ralph Waldo Emerson", Theme.VIRTUE, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("The heart and soul of all men being one, this bitterness of His and Mine ceases.", "Ralph Waldo Emerson", Theme.VIRTUE, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("All men are made one for another: either then teach them better, or bear with them.", "Marcus Aurelius", Theme.VIRTUE, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("Of my grandfather Verus I have learned to be gentle and meek, and to refrain from all anger and passion.", "Marcus Aurelius", Theme.VIRTUE, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("As for that which is truly good, what can it stand in need of more than either justice or truth; or more than either kindness and modesty?", "Marcus Aurelius", Theme.VIRTUE, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("Fearlessness, singleness of soul, the will always to strive for wisdom; opened hand and governed appetites.", "Bhagavad Gita", Theme.VIRTUE, "Song Celestial (Arnold trans.), Ch. XVI"), // GUTENBERG#2388
        Quote("Equanimity, and charity which spieth no man's faults; and tenderness towards all that suffer; a contented heart.", "Bhagavad Gita", Theme.VIRTUE, "Song Celestial (Arnold trans.), Ch. XVI"), // GUTENBERG#2388
        Quote("Humbleness, truthfulness, and harmlessness, patience and honour, reverence for the wise, purity, constancy, control of self.", "Bhagavad Gita", Theme.VIRTUE, "Song Celestial (Arnold trans.), Ch. XIII"), // GUTENBERG#2388
        Quote("Who hateth nought of all which lives, living himself benign, compassionate, from arrogance exempt.", "Bhagavad Gita", Theme.VIRTUE, "Song Celestial (Arnold trans.), Ch. XII"), // GUTENBERG#2388
        Quote("Essentially man is not a slave either of himself or of the world; but he is a lover.", "Rabindranath Tagore", Theme.VIRTUE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("Our great Revealers are they who make manifest the true meaning of the soul by giving up self for the love of mankind.", "Rabindranath Tagore", Theme.VIRTUE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("When a man loves, giving becomes a matter of joy to him, like the tree's surrender of the ripe fruit.", "Rabindranath Tagore", Theme.VIRTUE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("Pleasure is for one's own self, but goodness is concerned with the happiness of all humanity and for all time.", "Rabindranath Tagore", Theme.VIRTUE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("Our highest joy is in the losing of our egoistic self and in the uniting with others.", "Rabindranath Tagore", Theme.VIRTUE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("We come nearest to the great when we are great in humility.", "Rabindranath Tagore", Theme.VIRTUE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Thank the flame for its light, but do not forget the lampholder standing in the shade with constancy of patience.", "Rabindranath Tagore", Theme.VIRTUE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("He who wants to do good knocks at the gate; he who loves finds the gate open.", "Rabindranath Tagore", Theme.VIRTUE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("He who is too busy doing good finds no time to be good.", "Rabindranath Tagore", Theme.VIRTUE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Men are cruel, but Man is kind.", "Rabindranath Tagore", Theme.VIRTUE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Give me the strength never to disown the poor or bend my knees before insolent might.", "Rabindranath Tagore", Theme.VIRTUE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Give me the strength to make my love fruitful in service.", "Rabindranath Tagore", Theme.VIRTUE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Pride can never approach to where thou walkest in the clothes of the humble among the poorest, and lowliest, and lost.", "Rabindranath Tagore", Theme.VIRTUE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Work on with the intrepidity of a lion but at the same time with the tenderness of a flower.", "Swami Vivekananda", Theme.VIRTUE, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Purity, patience, and perseverance are the three essentials to success and, above all, love.", "Swami Vivekananda", Theme.VIRTUE, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("They alone live who live for others, the rest are more dead than alive.", "Swami Vivekananda", Theme.VIRTUE, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("Condemn none: if you can stretch out a helping hand, do so.", "Swami Vivekananda", Theme.VIRTUE, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("The weak can never forgive. Forgiveness is the attribute of the strong.", "Mahatma Gandhi", Theme.VIRTUE, "\"Interview to the Press\" in Karachi about the execution of Bhagat Singh (23 March 1931); published in Young India (2 April 1931), reprinted"), // WIKIQUOTE:Mahatma Gandhi
        Quote("Earth provides enough to satisfy every man's need but not for every man's greed.", "Mahatma Gandhi", Theme.VIRTUE, "Quoted by Pyarelal Nayyar in Mahatma Gandhi: The Last Phase (Volume 10), [http://books.google.com/books?id=sswBAAAAMAAJ&q=%22The+Earth+provi"), // WIKIQUOTE:Mahatma Gandhi
        Quote("Man becomes great exactly in the degree in which he works for the welfare of his fellow-men.", "Mahatma Gandhi", Theme.VIRTUE, "Young India, 5 March 1925, in The Collected Works of Mahatma Gandhi, Vol. 31, p. 105."), // WIKIQUOTE:Mahatma Gandhi
        Quote("I inherited honesty and self-discipline from my father; from my mother, I inherited faith in goodness and deep kindness.", "A. P. J. Abdul Kalam", Theme.VIRTUE, "p. 8"), // WIKIQUOTE:A. P. J. Abdul Kalam
        Quote("Let no man pull you so low as to hate him.", "Martin Luther King Jr.", Theme.VIRTUE, "[https://kinginstitute.stanford.edu/king-papers/documents/most-durable-power-excerpt-sermon-dexter-avenue-baptist-church-6-november-1956#fn1"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("A good head and a good heart are always a formidable combination.", "Nelson Mandela", Theme.VIRTUE, "[[w:Long Walk to Freedom (book);Long Walk to Freedom]] (1995)"), // WIKIQUOTE:Nelson Mandela
        Quote("A fundamental concern for others in our individual and community lives would go a long way in making the world the better place we so passionately dreamt of.", "Nelson Mandela", Theme.VIRTUE, "Nelson Mandela on selflessness, Kliptown, Soweto, South Africa (12 July 2008). Source: [http://www.nelsonmandela.org/content/mini-site/selec"), // WIKIQUOTE:Nelson Mandela
        Quote("If you want to make peace with your enemy, you have to work with your enemy. Then he becomes your partner.", "Nelson Mandela", Theme.VIRTUE, "[[w:Long Walk to Freedom (book);Long Walk to Freedom]] (1995)"), // WIKIQUOTE:Nelson Mandela
        Quote("We tried in our simple way to lead our life in a manner that may make a difference to those of others.", "Nelson Mandela", Theme.VIRTUE, "Nelson Mandela on freedom fighters, Upon Receiving the Roosevelt Freedom Award (8 June 2002). Source: [http://www.nelsonmandela.org/content/"), // WIKIQUOTE:Nelson Mandela
        Quote("Don't look for big things, just do small things with great love.", "Mother Teresa", Theme.VIRTUE, "As quoted in Mother Teresa: Come Be My Light (2007) by Brian Kolodiejchuk"), // WIKIQUOTE:Mother Teresa
        Quote("Believe, when you are most unhappy, that there is something for you to do in the world. So long as you can sweeten another's pain, life is not in vain.", "Helen Keller", Theme.VIRTUE, "We Bereaved (1929)"), // WIKIQUOTE:Helen Keller
        Quote("Until he extends the circle of his compassion to all living things, man will not himself find peace.", "Albert Schweitzer", Theme.VIRTUE, "Variant translation: Until he extends his circle of compassion to include all living things, man will not himself find peace. ; Variant tran"), // WIKIQUOTE:Albert Schweitzer
        Quote("A person is a person because he recognizes others as persons.", "Desmond Tutu", Theme.VIRTUE, "Address at his enthronement as Anglican archbishop of Cape Town (7 September 1986)"), // WIKIQUOTE:Desmond Tutu
        Quote("Without forgiveness there can be no future for a relationship between individuals or within and between nations.", "Desmond Tutu", Theme.VIRTUE, "As quoted in \"Truth and reconciliation\" at BBC Focus on Africa (January-March 2000)"), // WIKIQUOTE:Desmond Tutu
        Quote("We all know that a good example is more effective than advice. So set a good example, and it won't take long for others to follow.", "Anne Frank", Theme.VIRTUE, "\"Give!\" (26 March 1944) ; Variant translation: People will always follow a good example; be the one to set a good example, then it won't be"), // WIKIQUOTE:Anne Frank
        Quote("Only a life lived for others is a life worthwhile.", "Albert Einstein", Theme.VIRTUE, "In answer to a question asked by the editors of Youth, a journal of Young Israel of Williamsburg, NY. Quoted in the New York Times (June 20,"), // WIKIQUOTE:Albert Einstein
        Quote("You get more joy out of the giving to others, and should put a good deal of thought into the happiness you are able to give.", "Eleanor Roosevelt", Theme.VIRTUE, "As quoted in Sheroes: Bold, Brash, and Absolutely Unabashed Superwomen from Susan B. Anthony to Xena (1998) by Varla Ventura, p. 150"), // WIKIQUOTE:Eleanor Roosevelt
        Quote("If you want others to be happy, practice compassion. If you want to be happy, practice compassion.", "Dalai Lama", Theme.VIRTUE, "As quoted in Meditations for Living In Balance: Daily Solutions for People Who Do Too Much (2000) by Anne Wilson Schaef, p. 11."), // WIKIQUOTE:Tenzin Gyatso, 14th Dalai Lama
        Quote("Love and compassion are necessities, not luxuries. Without them humanity cannot survive.", "Dalai Lama", Theme.VIRTUE, "As quoted in A Small Drop of Ink: A Collection of Inspirational and Moving Quotations of the Ages (2003) by Linda Pendleton."), // WIKIQUOTE:Tenzin Gyatso, 14th Dalai Lama
        Quote("Our own brain, our own heart is our temple; the philosophy is kindness.", "Dalai Lama", Theme.VIRTUE, "\"Kindness and Compassion\" p. 52"), // WIKIQUOTE:Tenzin Gyatso, 14th Dalai Lama
        Quote("To be good and lead a good life means to give to others more than one takes from them.", "Leo Tolstoy", Theme.VIRTUE, "Ch. VII"), // WIKIQUOTE:Leo Tolstoy
        Quote("All that we are is the result of what we have thought: it is founded on our thoughts, it is made up of our thoughts.", "Dhammapada", Theme.WISDOM, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("As rain does not break through a well-thatched house, passion will not break through a well-reflecting mind.", "Dhammapada", Theme.WISDOM, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("Rouse thyself by thyself, examine thyself by thyself, thus self-protected and attentive wilt thou live happily.", "Dhammapada", Theme.WISDOM, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("They who know the truth are not equal to those who love it, and they who love it are not equal to those who delight in it.", "Confucius", Theme.WISDOM, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("The wise are joyful; the virtuous are long-lived.", "Confucius", Theme.WISDOM, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("If a man keeps cherishing his old knowledge, so as continually to be acquiring new, he may be a teacher of others.", "Confucius", Theme.WISDOM, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("The softest thing in the world dashes against and overcomes the hardest.", "Lao Tzu", Theme.WISDOM, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("Every one in the world knows that the soft overcomes the hard, and the weak the strong.", "Lao Tzu", Theme.WISDOM, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("The sage sees difficulty even in what seems easy, and so never has any difficulties.", "Lao Tzu", Theme.WISDOM, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("God offers to every mind its choice between truth and repose.", "Ralph Waldo Emerson", Theme.WISDOM, "Essays, First Series: Intellect (1841)"), // GUTENBERG#2944
        Quote("Direct your eye right inward, and you'll find a thousand regions in your mind yet undiscovered.", "Henry David Thoreau", Theme.WISDOM, "Walden (1854)"), // GUTENBERG#205
        Quote("The universe is wider than our views of it.", "Henry David Thoreau", Theme.WISDOM, "Walden (1854)"), // GUTENBERG#205
        Quote("As if you could kill time without injuring eternity.", "Henry David Thoreau", Theme.WISDOM, "Walden (1854)"), // GUTENBERG#205
        Quote("We lie in the lap of immense intelligence, which makes us receivers of its truth and organs of its activity.", "Ralph Waldo Emerson", Theme.WISDOM, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("Every thing is beautiful seen from the point of the intellect, or as truth.", "Ralph Waldo Emerson", Theme.WISDOM, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("Find full reward of doing right in right! Let right deeds be thy motive, not the fruit which comes from them.", "Bhagavad Gita", Theme.WISDOM, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("The right act is less, far less, than the right-thinking mind.", "Bhagavad Gita", Theme.WISDOM, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("Seek refuge in thy soul; have there thy heaven!", "Bhagavad Gita", Theme.WISDOM, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("Never the spirit was born; the spirit shall cease to be never; never was time it was not; End and Beginning are dreams!", "Bhagavad Gita", Theme.WISDOM, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("To see one changeless Life in all the Lives.", "Bhagavad Gita", Theme.WISDOM, "Song Celestial (Arnold trans.), Ch. XVIII"), // GUTENBERG#2388
        Quote("The moon shines in my body, but my blind eyes cannot see it: The moon is within me, and so is the sun.", "Kabir", Theme.WISDOM, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("Law is the first step towards freedom, and beauty is the complete liberation which stands on the pedestal of law.", "Rabindranath Tagore", Theme.WISDOM, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("Our will has freedom in order that it may find out that its true course is towards goodness and love.", "Rabindranath Tagore", Theme.WISDOM, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("The animal intelligence knows facts, the human mind has power to apprehend truth.", "Rabindranath Tagore", Theme.WISDOM, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("This discovery of truth is pure joy to man, it is a liberation of his mind.", "Rabindranath Tagore", Theme.WISDOM, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("The emancipation of our physical nature is in attaining health, of our social being in attaining goodness, and of our self in attaining love.", "Rabindranath Tagore", Theme.WISDOM, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("That I exist is a perpetual surprise which is life.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Man is a born child, his power is the power of growth.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Life finds its wealth by the claims of the world, and its worth by the claims of love.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("The small truth has words that are clear; the great truth has great silence.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("A mind all logic is like a knife all blade. It makes the hand bleed that uses it.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("The stream of truth flows through its channels of mistakes.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Let him only see the thorns who has eyes to see the rose.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Life is given to us, we earn it by giving it.", "Rabindranath Tagore", Theme.WISDOM, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("I knew not then that it was so near, that it was mine, and that this perfect sweetness had blossomed in the depth of my own heart.", "Rabindranath Tagore", Theme.WISDOM, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Hidden in the heart of things thou art nourishing seeds into sprouts, buds into blossoms, and ripening flowers into fruitfulness.", "Rabindranath Tagore", Theme.WISDOM, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("The child who is decked with prince's robes and who has jewelled chains round his neck loses all pleasure in his play; his dress hampers him at every step.", "Rabindranath Tagore", Theme.WISDOM, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Baby knows all manner of wise words, though few on earth can understand their meaning.", "Rabindranath Tagore", Theme.WISDOM, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("Truth can be stated in a thousand different ways, yet each one can be true.", "Swami Vivekananda", Theme.WISDOM, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("We reap what we sow. We are the makers of our own fate.", "Swami Vivekananda", Theme.WISDOM, "Pearls of Wisdom"), // WIKIQUOTE:Swami Vivekananda
        Quote("A man is but the product of his thoughts. What he thinks, he becomes.", "Mahatma Gandhi", Theme.WISDOM, "In Ethical Religion, (Madras: S. Ganesan, 1922), [http://hdl.handle.net/2027/mdp.39015002732066?urlappend=%3Bseq=66 p. 62], [https://babel.h"), // WIKIQUOTE:Mahatma Gandhi
        Quote("It is unwise to be too sure of one's own wisdom. It is healthy to be reminded that the strongest might weaken and the wisest might err.", "Mahatma Gandhi", Theme.WISDOM, "Harijan (17 February 1940)"), // WIKIQUOTE:Mahatma Gandhi
        Quote("One of the sure signs of maturity is the ability to rise to the point of self criticism.", "Martin Luther King Jr.", Theme.WISDOM, "The Rising Tide of Racial Consciousness (1960)"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("It is in the character of growth that we should learn from both pleasant and unpleasant experiences.", "Nelson Mandela", Theme.WISDOM, "Nelson Mandela on character, Foreign Correspondent's Association's Annual Dinner, Johannesburg, South Africa (21 November 1997). Source: [ht"), // WIKIQUOTE:Nelson Mandela
        Quote("There is nothing like returning to a place that remains unchanged to find the ways in which you yourself have altered.", "Nelson Mandela", Theme.WISDOM, "[[w:Long Walk to Freedom (book);Long Walk to Freedom]] (1995)"), // WIKIQUOTE:Nelson Mandela
        Quote("Self-pity in its early stages is as snug as a feather mattress. Only when it hardens does it become uncomfortable.", "Maya Angelou", Theme.WISDOM, "Gather Together in My Name (1974), p. 17."), // WIKIQUOTE:Maya Angelou
        Quote("Joy in looking and comprehending is nature's most beautiful gift.", "Albert Einstein", Theme.WISDOM, "Ideas and Opinions"), // WIKIQUOTE:Albert Einstein
        Quote("Study and in general the pursuit of truth and beauty is a sphere of activity in which we are permitted to remain children all our lives.", "Albert Einstein", Theme.WISDOM, "Letter to Adrianna Enriques (October 1921), p. 83"), // WIKIQUOTE:Albert Einstein
        Quote("Wisdom is not a product of schooling but of the lifelong attempt to acquire it.", "Albert Einstein", Theme.WISDOM, "From the same 24 March 1954 letter as above, p. 44"), // WIKIQUOTE:Albert Einstein
    )

    val EVENING: List<Quote> = listOf(
        Quote("Let him not despise what he has received, nor ever envy others.", "Dhammapada", Theme.GRATITUDE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("The sufficiency of contentment is an enduring and unchanging sufficiency.", "Lao Tzu", Theme.GRATITUDE, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("Whatsoever thy seasons bear, shall ever by me be esteemed as happy fruit, and increase.", "Marcus Aurelius", Theme.GRATITUDE, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("Bees sip honey from flowers and hum their thanks when they leave.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("The night opens the flowers in secret and allows the day to get thanks.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("God expects answers for the flowers he sends us, not for the sun and the earth.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("I came to your shore as a stranger, I lived in your house as a guest, I leave your door as a friend, my earth.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("My flower of the day dropped its petals forgotten. In the evening it ripens into a golden fruit of memory.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Life has become richer by the love that has been lost.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("The roots below the earth claim no rewards for making the branches fruitful.", "Rabindranath Tagore", Theme.GRATITUDE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Thy infinite gifts come to me only on these very small hands of mine.", "Rabindranath Tagore", Theme.GRATITUDE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Thy gifts to us mortals fulfil all our needs and yet run back to thee undiminished.", "Rabindranath Tagore", Theme.GRATITUDE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Thy centuries follow each other perfecting a small wild flower.", "Rabindranath Tagore", Theme.GRATITUDE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("This frail vessel thou emptiest again and again, and fillest it ever with fresh life.", "Rabindranath Tagore", Theme.GRATITUDE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("At the immortal touch of thy hands my little heart loses its limits in joy and gives birth to utterance ineffable.", "Rabindranath Tagore", Theme.GRATITUDE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Now at the end of youth my life is like a fruit, having nothing to spare, and waiting to offer herself completely with her full burden of sweetness.", "Rabindranath Tagore", Theme.GRATITUDE, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("It's wonderful to be alive and to walk on earth.", "Thich Nhat Hanh", Theme.GRATITUDE, "Talk at Stonehill College (2002)"), // WIKIQUOTE:Thích Nhất Hạnh
        Quote("You are a miracle, and everything you touch could be a miracle.", "Thich Nhat Hanh", Theme.GRATITUDE, "Episode of the National Public Radio program Speaking of Faith : \"Brother Thay: A Radio Pilgrimage with Thich Nhat Hanh\" (2003)"), // WIKIQUOTE:Thích Nhất Hạnh
        Quote("He is happy when he thinks of the good he has done; he is still more happy when going on the good path.", "Dhammapada", Theme.HAPPINESS, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("With coarse rice to eat, with water to drink, and my bended arm for a pillow, I have still joy in the midst of these things.", "Confucius", Theme.HAPPINESS, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("I am convinced, both by faith and experience, that to maintain one's self on this earth is not a hardship but a pastime.", "Henry David Thoreau", Theme.HAPPINESS, "Walden (1854)"), // GUTENBERG#205
        Quote("With thought, with the ideal, is immortal hilarity, the rose of joy.", "Ralph Waldo Emerson", Theme.HAPPINESS, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("A happy lot and portion is, good inclinations of the soul, good desires, good actions.", "Marcus Aurelius", Theme.HAPPINESS, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("Perfect bliss grows only in the bosom tranquillised, the spirit passionless, purged from offence.", "Bhagavad Gita", Theme.HAPPINESS, "Song Celestial (Arnold trans.), Ch. VI"), // GUTENBERG#2388
        Quote("Dance, my heart! dance to-day with joy.", "Kabir", Theme.HAPPINESS, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("Where there is the response of delight, there is the fullness of joy.", "Kabir", Theme.HAPPINESS, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("On this tree is a bird: it dances in the joy of life.", "Kabir", Theme.HAPPINESS, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("Only he has attained the final truth who knows that the whole world is a creation of joy.", "Rabindranath Tagore", Theme.HAPPINESS, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("From joy are born all creatures, by joy they are sustained, towards joy they progress, and into joy they enter.", "Rabindranath Tagore", Theme.HAPPINESS, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("Children have their play on the seashore of worlds.", "Rabindranath Tagore", Theme.HAPPINESS, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("On the seashore of endless worlds is the great meeting of children.", "Rabindranath Tagore", Theme.HAPPINESS, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("My heart is glad within, and the breath of the passing breeze is sweet.", "Rabindranath Tagore", Theme.HAPPINESS, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("All that is harsh and dissonant in my life melts into one sweet harmony.", "Rabindranath Tagore", Theme.HAPPINESS, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Take to your bosom with a smile what is easy and simple and near.", "Rabindranath Tagore", Theme.HAPPINESS, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("Let your life lightly dance on the edges of Time like dew on the tip of a leaf.", "Rabindranath Tagore", Theme.HAPPINESS, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("Come, my rainy nights with pattering feet; smile, my golden autumn; come, careless April, scattering your kisses abroad.", "Rabindranath Tagore", Theme.HAPPINESS, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("Child, how happy you are sitting in the dust, playing with a broken twig all the morning.", "Rabindranath Tagore", Theme.HAPPINESS, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("I always knew that someday I would once again feel the grass under my feet and walk in the sunshine as a free man.", "Nelson Mandela", Theme.HAPPINESS, "[[w:Long Walk to Freedom (book);Long Walk to Freedom]] (1995)"), // WIKIQUOTE:Nelson Mandela
        Quote("A happy life consists not in the absence, but in the mastery of hardships.", "Helen Keller", Theme.HAPPINESS, "The Simplest Way to be Happy (1933)"), // WIKIQUOTE:Helen Keller
        Quote("It all comes to this: the simplest way to be happy is to do good.", "Helen Keller", Theme.HAPPINESS, "The Simplest Way to be Happy (1933)"), // WIKIQUOTE:Helen Keller
        Quote("Human happiness and human satisfaction must ultimately come from within oneself.", "Dalai Lama", Theme.HAPPINESS, "The Path to Tranquility: Daily Wisdom (1998) edited by Renuka Singh"), // WIKIQUOTE:Tenzin Gyatso, 14th Dalai Lama
        Quote("There is only one enduring happiness in life, to live for others.", "Leo Tolstoy", Theme.HAPPINESS, "[http://books.google.com/books?id=eWU4AAAAYAAJ&q=%22there+is+only+one+enduring+happiness+in+life+to+live+for+others%22&pg=PA22#v=onepage Par"), // WIKIQUOTE:Leo Tolstoy
        Quote("Have virtuous people for friends, have for friends the best of men.", "Dhammapada", Theme.LOVE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("Kinsmen, friends, and lovers salute a man who has been long away, and returns safe from afar.", "Dhammapada", Theme.LOVE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("He should overflow in love to all, and cultivate the friendship of the good.", "Confucius", Theme.LOVE, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("Is it not delightful to have friends coming from distant quarters?", "Confucius", Theme.LOVE, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("The condition which high friendship demands is ability to do without it.", "Ralph Waldo Emerson", Theme.LOVE, "Essays, First Series: Friendship (1841)"), // GUTENBERG#2944
        Quote("I have learnt that all men live not by care for themselves, but by love.", "Leo Tolstoy", Theme.LOVE, "What Men Live By (1881)"), // GUTENBERG#6157
        Quote("The good soul nourishes me and unlocks new magazines of power and enjoyment to me every day.", "Ralph Waldo Emerson", Theme.LOVE, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("There can be no excess to love, none to knowledge, none to beauty, when these attributes are considered in the purest sense.", "Ralph Waldo Emerson", Theme.LOVE, "Essays, First Series (1841)"), // GUTENBERG#2944
        Quote("Listen to me, friend: he understands who loves.", "Kabir", Theme.LOVE, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("Where is the need of words, when love has made drunken the heart?", "Kabir", Theme.LOVE, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("More than all else do I cherish at heart that love which makes me to live a limitless life in this world.", "Kabir", Theme.LOVE, "Songs of Kabir (Tagore trans., 1915)"), // GUTENBERG#6519
        Quote("Love is the highest bliss that man can attain to, for through it alone he truly knows that he is more than himself, and that he is at one with the All.", "Rabindranath Tagore", Theme.LOVE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("That only which is done for love is done freely, however much pain it may cause.", "Rabindranath Tagore", Theme.LOVE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("His freedom and fulfilment is in love, which is another name for perfect comprehension.", "Rabindranath Tagore", Theme.LOVE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("You smiled and talked to me of nothing and I felt that for this I had been waiting long.", "Rabindranath Tagore", Theme.LOVE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Once we dreamt that we were strangers. We wake up to find that we were dear to each other.", "Rabindranath Tagore", Theme.LOVE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("This love between you and me is simple as a song.", "Rabindranath Tagore", Theme.LOVE, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("We both live in the same village, and that is our one piece of joy.", "Rabindranath Tagore", Theme.LOVE, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("The heart is only for giving away with a tear and a song, my love.", "Rabindranath Tagore", Theme.LOVE, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("Trust love even if it brings sorrow.", "Rabindranath Tagore", Theme.LOVE, "The Gardener (1913)"), // GUTENBERG#6686
        Quote("I do not love him because he is good, but because he is my little child.", "Rabindranath Tagore", Theme.LOVE, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("In all my hopes and my loves, in my life, in the life of my mother you have lived.", "Rabindranath Tagore", Theme.LOVE, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("You were hidden in my heart as its desire, my darling.", "Rabindranath Tagore", Theme.LOVE, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("He knows that there is room for endless joy in mother's little corner of a heart, and it is sweeter far than liberty to be caught and pressed in her dear arms.", "Rabindranath Tagore", Theme.LOVE, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("The sky watches over you when you sleep in your mother's arms, and the morning comes tiptoe to your bed and kisses your eyes.", "Rabindranath Tagore", Theme.LOVE, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("O greedy heart, shall I pluck the world like a fruit from the sky to place it on your little rosy palm?", "Rabindranath Tagore", Theme.LOVE, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("Heaven's first darling, twin-born with the morning light, you have floated down the stream of the world's life, and at last you have stranded on my heart.", "Rabindranath Tagore", Theme.LOVE, "The Crescent Moon (1913)"), // GUTENBERG#6520
        Quote("Hatred paralyzes life; love releases it. Hatred confuses life; love harmonizes it. Hatred darkens life; love illuminates it.", "Martin Luther King Jr.", Theme.LOVE, "Strength to Love (1963), Antidotes for Fear"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("Darkness cannot drive out darkness; only light can do that. Hate cannot drive out hate; only love can do that.", "Martin Luther King Jr.", Theme.LOVE, "This passage contains some phrases King later used in \"Where Do We Go From Here?\" (1967) which has a section below."), // WIKIQUOTE:Martin Luther King Jr.
        Quote("There can be no deep disappointment where there is not deep love.", "Martin Luther King Jr.", Theme.LOVE, "{{w;Letter from a Birmingham Jail}} (1963)"), // WIKIQUOTE:Martin Luther King Jr.
        Quote("I like friends who have independent minds because they tend to make you see problems from all angles.", "Nelson Mandela", Theme.LOVE, "Nelson Mandela on friendship, From his unpublished autobiographical manuscript written in 1975. Source: [http://www.nelsonmandela.org/conten"), // WIKIQUOTE:Nelson Mandela
        Quote("If we are bold, love strikes away the chains of fear from our souls.", "Maya Angelou", Theme.LOVE, "A Brave and Startling Truth (1995)"), // WIKIQUOTE:Maya Angelou
        Quote("I have found the paradox that if I love until it hurts, then there is no hurt, but only more love.", "Mother Teresa", Theme.LOVE, "As quoted in Love Until It Hurts: A Tribute to Mother Teresa and the work of the men and women of the Missionaries of Charity (1980) by Daph"), // WIKIQUOTE:Mother Teresa
        Quote("The hunger for love is much more difficult to remove than the hunger for bread.", "Mother Teresa", Theme.LOVE, "Interview by Edward W. Desmond in TIME magazine (4 December 1989)"), // WIKIQUOTE:Mother Teresa
        Quote("So long as the memory of certain beloved friends lives in my heart I shall say that life is good.", "Helen Keller", Theme.LOVE, "Midstream (1929)"), // WIKIQUOTE:Helen Keller
        Quote("You don't choose your family. They are God's gift to you, as you are to them.", "Desmond Tutu", Theme.LOVE, "Address at his enthronement as Anglican archbishop of Cape Town (7 September 1986)"), // WIKIQUOTE:Desmond Tutu
        Quote("The most important thing in any relationship is not what you get but what you give.", "Eleanor Roosevelt", Theme.LOVE, "Preface (December 1960) to The Autobiography of Eleanor Roosevelt (1961), p. xvi; the last line was originally used in the initial edition o"), // WIKIQUOTE:Eleanor Roosevelt
        Quote("Friendship with oneself is all-important, because without it one cannot be friends with anyone else in the world.", "Eleanor Roosevelt", Theme.LOVE, "As quoted in The Beacon Book of Quotations by Women (1992) by Rosalie Maggio, p. 130"), // WIKIQUOTE:Eleanor Roosevelt
        Quote("Seize the moments of happiness, love and be loved! That is the only reality in the world, all else is folly.", "Leo Tolstoy", Theme.LOVE, "Book IV, Ch. 11"), // WIKIQUOTE:Leo Tolstoy
        Quote("The better part of one's life consists of his friendships.", "Abraham Lincoln", Theme.LOVE, "[http://quod.lib.umich.edu/l/lincoln/lincoln2/1:88.1?rgn=div2;view=fulltext Letter to Joseph Gillespie] (13 July 1849)"), // WIKIQUOTE:Abraham Lincoln
        Quote("He who drinks in the law lives happily with a serene mind.", "Dhammapada", Theme.PEACE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("Wise people, after they have listened to the laws, become serene, like a deep, smooth, and still lake.", "Dhammapada", Theme.PEACE, "Dhammapada (Muller trans.)"), // GUTENBERG#2017
        Quote("The superior man is satisfied and composed; the mean man is always full of distress.", "Confucius", Theme.PEACE, "Analects (Legge trans.)"), // GUTENBERG#3330
        Quote("Purity and stillness give the correct law to all under heaven.", "Lao Tzu", Theme.PEACE, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("How pure and still the Tao is, as if it would ever so continue!", "Lao Tzu", Theme.PEACE, "Tao Teh King (Legge trans.)"), // GUTENBERG#216
        Quote("Nothing can bring you peace but the triumph of principles.", "Ralph Waldo Emerson", Theme.PEACE, "Essays, First Series: Self-Reliance (1841)"), // GUTENBERG#2944
        Quote("There can be no very black melancholy to him who lives in the midst of Nature and has his senses still.", "Henry David Thoreau", Theme.PEACE, "Walden (1854)"), // GUTENBERG#205
        Quote("Wherever I sat, there I might live, and the landscape radiated from me accordingly.", "Henry David Thoreau", Theme.PEACE, "Walden (1854)"), // GUTENBERG#205
        Quote("How easy a thing is it for a man to put off from him all turbulent adventitious imaginations, and presently to be in perfect rest and tranquillity!", "Marcus Aurelius", Theme.PEACE, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("Go thy ways then well pleased and contented: for so is He that dismisseth thee.", "Marcus Aurelius", Theme.PEACE, "Meditations (Casaubon trans.)"), // GUTENBERG#2680
        Quote("Such a man comes to tranquillity; and out of that tranquillity shall rise the end and healing of his earthly pains.", "Bhagavad Gita", Theme.PEACE, "Song Celestial (Arnold trans.), Ch. II"), // GUTENBERG#2388
        Quote("Steadfast a lamp burns sheltered from the wind; such is the likeness of the Yogi's mind.", "Bhagavad Gita", Theme.PEACE, "Song Celestial (Arnold trans.), Ch. VI"), // GUTENBERG#2388
        Quote("Near to renunciation, very near, dwelleth Eternal Peace!", "Bhagavad Gita", Theme.PEACE, "Song Celestial (Arnold trans.), Ch. XII"), // GUTENBERG#2388
        Quote("Who troubleth not his kind, and is not troubled by them; clear of wrath, living too high for gladness, grief, or fear.", "Bhagavad Gita", Theme.PEACE, "Song Celestial (Arnold trans.), Ch. XII"), // GUTENBERG#2388
        Quote("Splendid and clear shines manifest the Truth as if a Sun of Wisdom sprang to shed its beams of dawn.", "Bhagavad Gita", Theme.PEACE, "Song Celestial (Arnold trans.), Ch. V"), // GUTENBERG#2388
        Quote("Outwardly nature is busy and restless, inwardly she is all silence and peace.", "Rabindranath Tagore", Theme.PEACE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("The universal spirit is waiting to crown us with happiness, but our individual spirit would not accept it.", "Rabindranath Tagore", Theme.PEACE, "Sadhana (1913)"), // GUTENBERG#6842
        Quote("Be still, my heart, these great trees are prayers.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Silence will carry your voice like the nest that holds the sleeping birds.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Sit still my heart, do not raise your dust. Let the world find its way to you.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Let my doing nothing when I have nothing to do become untroubled in its depth of peace like the evening in the seashore when the water is silent.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("I have dipped the vessel of my heart into this silent hour; it has filled with love.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Light in my heart the evening star of rest and then let the night whisper to me of love.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("God's great power is in the gentle breeze, not in the storm.", "Rabindranath Tagore", Theme.PEACE, "Stray Birds (1916)"), // GUTENBERG#6524
        Quote("Under thy great sky in solitude and silence, with humble heart shall I stand before thee face to face.", "Rabindranath Tagore", Theme.PEACE, "Gitanjali (1912)"), // GUTENBERG#7164
        Quote("Is the song of the sea in tune only with the rising waves? Does it not also sing with the waves that fall?", "Rabindranath Tagore", Theme.PEACE, "Fruit-Gathering (1916)"), // GUTENBERG#6522
        Quote("I have always believed that exercise is the key not only to physical health but to peace of mind.", "Nelson Mandela", Theme.PEACE, "[[w:Long Walk to Freedom (book);Long Walk to Freedom]] (1995)"), // WIKIQUOTE:Nelson Mandela
        Quote("If we have no peace, it is because we have forgotten that we belong to each other.", "Mother Teresa", Theme.PEACE, "Mother Teresa Reflects on Working Toward Peace, ([https://legacy.scu.edu/ethics/architects-of-peace/Teresa/essay.html essay, Santa Clara Uni"), // WIKIQUOTE:Mother Teresa
        Quote("I trust, and nothing that happens disturbs my trust.", "Helen Keller", Theme.PEACE, "[[s:Optimism (Keller);Optimism]] (1903)"), // WIKIQUOTE:Helen Keller
        Quote("It isn't enough to talk about peace. One must believe in it. And it isn't enough to believe in it. One must work at it.", "Eleanor Roosevelt", Theme.PEACE, "Voice of America broadcast (11 November 1951)"), // WIKIQUOTE:Eleanor Roosevelt
        Quote("The miracle is not to walk on water. The miracle is to walk on the green earth in the present moment, to appreciate the peace and beauty that are available now.", "Thich Nhat Hanh", Theme.PEACE, "Touching Peace (1992), p. 1. Parallax Press"), // WIKIQUOTE:Thích Nhất Hạnh
    )
}
