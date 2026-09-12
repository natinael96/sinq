# Changelog

All notable changes to Sinq (ስንቅ) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows semantic-style releases (PATCH for fixes, MINOR for
features; `versionCode` increments on every release).

## [2.3.1] — 2026-09-12

_versionCode 71 · A way into the ማኅሌት_

### Added
- **The months of ሥርዓተ ማኅሌት are a pager.** Swiping moves between them and the
  strip above is its tab row, so the two can never disagree about which month is
  showing — the old strip could not say at all, every pill drawn unselected and
  announced that way to a screen reader. It opens on the month the year is in
  rather than መስከረም, and today's order sits on its own month instead of floating
  above all of them.
- **ማኅሌት can be searched.** A feast name is what a reader knows, and this was the
  one large corpus in the app you could not type your way into: 190 feasts
  reachable only by scrolling to the right month. Folded the way the app's own
  search folds, so ሠ and ሰ find each other.
- **The order page has the reader's toolbar.** It had always obeyed the chosen
  text size and never offered the control, so resizing the chant meant leaving,
  changing it in another reader, and coming back — on the longest reading in the
  app. A contents sheet comes with it, because a median order runs five
  screenfuls and the longest twelve with nothing to navigate by.

### Fixed
- **Three orders could not be opened from anywhere.** Month 0 holds the orders no
  book dates, and three of them belong to ዘመነ ጽጌ: the seven-year አቋቋም and the
  ጥቅምት and ኅዳር ones. The list filters every ጽጌ order out on the reasoning that
  the season has a door of its own, and that door read only መስከረም, ጥቅምት and ኅዳር.
  Forty-seven parts of chant with no route in. It reads month 0 now, which also
  settles why the door said ፵፩ and the page behind it said ፴፰.
- **The ዋዜማ and ማኅሌት chips open their own orders.** They read as targets and
  were not: the row always opened the ዋዜማ, because a vigil sorts first, so on the
  fifty feasts that have both, a reader after the morning service always landed
  on last night's.
- **A month with nothing appointed says so.** The sentence for it had been written
  and never used.

### Changed
- **Editions say what they are.** Ninety-five of the 190 orders carry them and
  nothing on the screen said they are another telling of the same order rather
  than more of it. The book's own text was labelled "እትም 0", with an ASCII zero
  in a numbering system that has none.
- **The ዘመነ ጽጌ rows carry their feast.** Thirty-three of them read "ጥቅምት · ፲፯ ክፍል"
  and nothing else, told apart only by a numeral — a wall rather than a list.
- **One chapter stepper, in the app's own gold-ringed doors.** It had been two
  bare text buttons in muted ink at the far corners of the row, copied once in
  the Bible reader and once in the book reader. Each half now names the chapter it
  opens, and holds its side so nothing slides under the thumb at the first chapter.
- **One way to add a name to the prayer list**, on the row itself rather than in a
  full-width form for a single word. The icon and the empty state open the same
  row. ውዳሴ ማርያም can be swiped through, and ንባብ's ቀጥል strip uses the same chips
  the ግጻዌ passage does.

## [2.3.0] — 2026-09-12

_versionCode 70 · The map and the hour_

### Added
- **የመጻሕፍቱ ካርታ.** Every book the app carries, grouped as the Church groups
  them, each one a bar that fills with its chapters as they are read. A
  percentage says how much; this says where, and the gaps read as clearly as
  the gold. A line at the top names the book in your hands, or the group
  nearest to finishing. Tapping a book opens its chapters. It needed no new
  data — ንባብ has always recorded chapters, and this is the first screen to
  draw them.
- **ንባብ's card reports itself.** It was the sixth of seven identical cards in
  መጻሕፍት, saying the same fixed sentence whether you had never started or were
  two hundred days in. It is the one commitment on that shelf, so it is now the
  one card cut from the hero's green: today's passage by name, the week behind
  you, and how far through the books you are.
- **A plan can be finished.** The day number used to stop at the last day and
  sit there for ever. Reading the whole Bible is the largest thing this app
  asks of anyone; it now says so, and offers to begin again. What was read is
  kept whatever happens next.
- **Two plans at once.** የዳዊት ንባብ can be kept beside a Bible plan, a block each
  on the page and a line each on the card. Two plans that read the same corpus
  at different speeds are refused with the reason rather than printed twice.
- **The Fathers, as this Church receives them.** Catena's own Early Fathers
  group — "all fathers venerated in all churches in communion with the Church
  of Alexandria" — is now what the commentary asks for. On ማቴዎስ ፭፥፬ that is
  eleven commentaries rather than nineteen. The ask is a cookie their own
  settings panel writes, so the page moved out of a Custom Tab into a WebView
  with a jar of its own: the preference applies here and never touches what a
  reader has chosen on the site themselves.

### Changed
- **An hour and the time it rings are one page.** They were two screens in two
  branches of ቅንብሮች — whether an hour existed at all under ጸሎት, what time it
  rang four taps away under ማንቂያዎች, beneath a heading about sound — and the
  second silently dropped any hour the first had hidden. A row on ሰዓታት is now
  the whole hour: the time, the name, the days it keeps, one switch. The four
  icon buttons it used to carry are behind the row's own menu.
- **The six day hours ring from first launch.** The built-in mode shipped eight
  hours with every one of them switched off: an app installed to be called to
  prayer called its reader to nothing, and the count on ማንቂያዎች reported
  reminders as on while the hours sat silent. ንዋም and ሌሊት ፱ ሰዓት still do not
  ring unasked — waking a stranger at midnight on the day they install the app
  misreads what was asked for — and both are one switch away. Where nothing
  will ring, the page now says so, with a way straight to the switches.
- **ድምፅና ጸጥታ comes first on ማንቂያዎች**, since how a reminder sounds governs
  every reminder on the page. የዛሬው ቀን folds; shut, its header still says how
  many times the phone will speak today.
- **The ምስባክ on the ግጻዌ passage page is the one the book prints.** The page had
  been slicing whole verses out of the Psalter for a chant three lines long. A
  ምስባክ begins part-way through a verse, ends part-way through another, drops
  the clauses between, and follows its own recension. Across the year the
  sliced range ran half again as long as the chant, and at መዝሙር ፶ it gave 592
  characters where the chant is 68. Only the ምስባክ: the ወንጌል and the deacons'
  readings print an incipit, and for those the cited range is the reading.
- **The ግጻዌ passage's exits are a ቀጥል strip.** Two chips on a psalm and three
  on a gospel, wrapping, rather than full-width rows that on a seven-verse
  passage made a list longer than the reading. Catena is drawn as what it is: a
  link that leaves the app, marked, and named for a screen reader.
- **ንባብ leads with what a day costs** — about eleven minutes for the year,
  twenty-two for the six months — measured in verses, the unit the plans are
  packed in. "Five chapters a day" was the average of a figure that runs from
  one to sixteen.
- **Each passage of a day keeps itself.** A day was recorded chapter by chapter
  but could only be marked whole, so a day of three passages could not be half
  kept.
- **ማኅሌት sits on ሌሎች መጻሕፍት** with the other books of the Church, counted the
  way the shelves count theirs.
- **የጸሎት ዝርዝር is read down.** It was a two-column grid of cards, which is the
  one shape a recited list cannot take: reading order zig-zagged across the
  pair. One column now, one line per name, with the intention beside the name
  rather than under it so a note costs no extra row. A name can be marked among
  the departed, and removing one can be undone to the place it held — a list
  that is recited has an order.
- **ውዳሴ ማርያም opens on ጸሎት ዘዘወትር**, the prayer said every day, with the day's
  own portion a tap away at the foot. Its two editions are the pill the Psalter
  carries, in the same corner, rather than a full-width segmented bar.
- **ጉዞ's journal button has a floating stand-in** while the real one is off
  screen. Any sliver of it counts as reached.
- **Latin prose is set in its own face.** The licence text was 11sp monospace,
  a size and a face meant for code and right for neither; every role in the
  type scale is Noto Sans Ethiopic with letter spacing tuned for Ethiopic.

### Fixed
- **Twenty-eight ምስባክ cited a psalm their chant is not in.** The citation is
  what the passage page's doors and its Catena link are built from, so a reader
  tapping through landed in the wrong psalm. Each chant was looked up in the
  Ge'ez Psalter, folding the homophone series the app's own search folds. Two
  of the twenty-eight came from the verse marker: it is spelled ቍ on almost
  every page of the book and ቄ on five of them, and the importer split on ቍ
  alone — which also gave four other readings a verse range they had been
  shipping without. The chant itself is untouched and will stay so.
- **Repacking a plan left the end of it blank.** The remainder was chunked by a
  rounded-up size, which gives fewer chunks than there are days to fill: a ዳዊት
  reader who fell behind on day 5 and repacked on day 40 was given readings to
  day 112 and nothing at all for the 38 days after — and the screen answered
  those days with "you have not started a plan yet".
- **Hiding or deleting an hour left its reminder armed** until the next app
  launch, so a hidden hour went on ringing and a deleted one rang with a name
  that no longer existed. A pending snooze could not be cancelled at all: it
  outlived the mode being switched and the entry being deleted, and rang ten
  minutes after the reader had moved on.
- **Hours could not be reordered.** The page has offered it since it shipped
  and the repository was written for it, but nothing ever called it — order
  could only be changed by restoring a backup.
- **ሌሊት ፱ ሰዓት shipped hidden**, which filtered it out of the mode editor and
  the scheduler alike: its reminder was stored, counted on the card, and
  impossible to turn on from the page that listed it.
- **The Psalter's Catena action went nowhere.** It built the right URL and then
  called a default no-op, on the book the app reads most.
- **ንባብ's day numbers were in Arabic digits** beside passages numbered in
  Ge'ez, sometimes in the same row, while the reminder that opens the page
  writes ዕለት ፻፳. The page also never named the plan it was keeping.

### Removed
- `CustomizeHoursScreen`, a second hours editor with no callers that the route,
  the package and the title all still pointed at.
- `androidx.browser`, with the Custom Tab it was added for.
- ዘወትር ጸሎት's card in መጻሕፍት, which opened the ውዳሴ ማርያም screen at its daily
  section: one text behind two doors.

## [2.2.0] — 2026-09-11

_versionCode 69 · A light on where you are_

### Added
- **The four tabs are one pager.** Moving between ቤት, መጻሕፍት, ጉዞ and ቅንብር is a
  swipe now, and the bar's light travels with the page under the finger.
- **አሳድር has a length, a voice, and an end.** It was ten minutes, fixed,
  unlimited and silent. It is now a choice of 5, 10, 15 or 20 minutes in the
  ማንቂያ sheet; snoozing leaves a quiet line naming the hour and the time it
  comes back, which clears itself as the alarm returns; and it stops at three.
  A second or third ring says how many times the hour has been pushed away.
- **The ግጻዌ passage has a Catena row**, beside the doors to the chapter and the
  book. It was the one place a reader met scripture with no way through to the
  Fathers, because it renders plain text and has no selection bar.

### Changed
- **The tab bar is cut from the hero's deep green in both themes.** A pale bar
  with a pale gold mark had nothing to be seen against on ivory. The tab you are
  on fills its glyph, lights a lozenge behind it, and opens sideways to say its
  name in full.
- **ቤተ መጻሕፍት is መጻሕፍት**, in the tab and on the page. The old name was the room
  rather than what is in it, and it was the longest label in the bar.
- **ቅንብሮች is a third shorter.** The landing page ran 1,027 dp on a screen that
  shows 540, with eleven of its thirteen rows leading somewhere else. The tour
  of the new version moved into the release notes it describes; ፈቃዶች እና ምንጮች
  became a section of ስለ መተግበሪያው; መዝገብ absorbed መረጃ, which had been opening
  with መዝገብ's own ምልክቶቼ row repeated; the two pickers put their label beside
  the control; and every list row in the app gave back 4 dp of padding. Eleven
  doors down to eight, and nothing removed.
- The verse commentary action is called **Catena** in both languages, naming
  where it goes rather than what it is.

### Fixed
- **መዝገብ's የጸሎት ዝርዝር row did nothing.** It navigated to a route spelled with a
  capital L that was never registered.

### Removed
- `DataSettingsScreen`, whose every row now lives on መዝገብ, and
  `DropdownSetting`, which was fully written and never called.

## [2.1.0] — 2026-09-10

_versionCode 68 · The book beside the channel_

### Added
- **ሥርዓተ ማኅሌት is the merged edition now**: the scanned book set beside the
  editions of the same orders posted on the EOTC Mahlet Telegram channel.
  142 orders to 215, and 275 editions holding 5,422 further parts. An edition
  is a whole alternative text of an order, read in place of the book's and never
  after it — they are pills under the tabs, the book's own text as እትም 0, and
  where the book has no order at all the first edition stands for it and says so.
  A part the book offers *instead of* the one before is marked ወይም, because set
  as one more rubric a choice reads as a sequence. Every edition keeps the link
  to the post it came from.
- **The feasts the book could not date are appointed by the calendar.** ሆሣዕና,
  ትንሣኤ, ዕርገት and ጰራቅሊጦስ with their week, because they move with Fasika; ስብከት,
  ብርሃን, ኖላዊ and the ዘመነ ጽጌ weeks, because each is a Sunday the calendar has
  to find. Twenty-one orders, appointed by the same two calendars the ግጻዌ
  already keeps, so the two agree on which day a feast is by construction. The
  list says when each falls this year.
- **Four kinds of order the book has and the app did not name**: አንገርጋሪ, ዑደት,
  ጸሎት, and one the merge could not classify.
- **The ማኅሌት opens three times as many hymns on the shelf** — 586 parts name a
  book where 192 did — because the merge puts each መልክእ's own name where the
  part's name is.

### Changed
- **The shelf is thirty-seven books.** The ቅዳሴ, ዚቅ, ዝማሬ, the ድርሳናት, the ገድላት
  and መዝሙር ዘሰናብት came off, and the መልክእ were cut to the thirty-two asked for.
  Deleted from the project, not withheld: nothing on the shelf is a thing the
  app is quietly carrying. Every remaining መልክእ records that it was scanned
  from መልክዐ ጉባኤ; መጽሐፈ ሰዓታት names the Church's own PDF it was scanned from.
- **Orders that stand on a Telegram edition alone stay off the ግጻዌ.** They are
  appointed like any other and remain in the ማኅሌት, labelled; but the merge is
  explicit that they are reported by the channel and not checked against the
  calendar, and the ግጻዌ is the one page that claims to know what today is.

## [2.0.1] — 2026-09-09

_versionCode 67 · The margin and the hand_

### Added
- **ሞትን አስብ on the home screen.** A second widget, and the only one in the app
  that says nothing about today: the phrase the splash opens on, kept in view.
  No date, no reading, no state, and deliberately no refresh — something that
  updated would be a different thing entirely. It is lettered in ዋልድባ
  (ይገዙ ብሥራት ጎፈር, by Abass Alamnehe, under the SIL OFL like every other face
  here), which now sets ሞትን አስብ on the splash as well: an Ethiopic display hand
  for an Ethiopic phrase.
- **The mark down the edge of a reading moves it.** Drag it and a bubble names
  where you would land. It is faint but always drawn now, because a mark that
  fades cannot be grabbed.
- **A reading says how far in you are** — ፲፪ / ፵፭ beside the title, where the
  content is countable. For a hymn of numbered stanzas that beats any bar, and
  unlike the bar it is legible without scrolling.
- **Onboarding asks how much to pray.** The app has had five prayer levels since
  before any of this and opened on the longest without mentioning the other four
  existed. It asks now, preselects መጀመሪያ, and describes each level by how much of
  the hour it keeps.
- **A way to report a wrong word**, in Settings › More, above About.

### Changed
- **Verse numerals moved out of the line and into a margin.** As a 58%
  superscript inside the text, ፳፬ read as debris between words; Ge'ez numerals
  are composed rather than positional and need room to be scanned. In their own
  column they become an index, and the verse keeps a flush left edge — the
  arrangement a printed Psalter uses. Every reader at once.
- **The chapter strip became the title.** It was drawn above the text on every
  screen of every chapter whether or not anyone wanted a different one, and in
  መዝሙረ ዳዊት it showed four of a hundred and fifty. The chapter is now the title's
  second line: it says where you are, and tapping it opens the chapters as a
  list, with each chapter's own name where the book has them.
- **The books reader steps from the foot of a chapter**, as the Bible reader has
  since 1.9.0. ሥርዓተ ቅዳሴ is twenty-three chapters and the end of one was a dead
  end two thousand paragraphs from the strip at the top.
- **ውዳሴ ማርያም ends where it should.** ይወድስዋ መላእክት now follows አንቀጸ ብርሃን rather
  than preceding it, and መልክአ ማርያም and መልክአ ኢየሱስ are reachable from the foot of
  it — linked to the copies on the shelf rather than a second copy of each.
- **Eight books came off the ዜማ shelf** — the አቋቋም and ድጓ books, which are
  learned by ear from a teacher rather than read, and which a scan serves worst.
  95 books to 87.

### Fixed
- **Every update since 1.7.4 showed the 1.7.4 What's New again.** The check asked
  whether you had caught up with the installed version, not whether you had seen
  *that* tour, so a release which shipped no tour of its own re-offered the last
  one that existed — four times over. A tour is shown once now, and a release
  with nothing to say says nothing.

## [2.0.0] — 2026-09-09

_versionCode 66 · In red ink_

### Added
- **The መልክእ are written in red, the way the books write them.** On a መልክእ the
  salutation opening each stanza — ሰላም and the ለ… phrase naming what is greeted —
  and the Name of God wherever it falls are rubricated, and nothing else is. The
  rule was measured against the 2,148 salutation stanzas on the shelf rather than
  guessed: what follows the ለ… word is either a function word beginning a new
  clause or a noun continuing the construct. The hymn's own subject stays in ink,
  because red marks the Name and not the one addressed.
- **A ማኅሌት part opens the hymn at the stanza being sung**, not at its first page —
  which, in a hymn of forty stanzas, was forty scrolls from the answer. Where the
  ማኅሌት and the scan spell a line differently it still opens the book.

### Changed
- **The red is blood red in both themes.** On the dark theme it had been a light
  crimson, nearer salmon than ink.
- **A bilingual book's translation reads as text.** Gold rather than muted grey,
  and without the italic — Ge'ez has no italic form, so that was a synthesised
  slant that read as a rendering fault rather than a voice.
- **The hours line is back on ቤት**, between hairlines under the hero: the hero says
  what is due now, the line says what follows and opens the rest.
- **ቁርባን preparation is no longer offered in ቤተ መጻሕፍት.** The screen stays,
  unreached, until it is finished.

### Fixed
- **An Amharic book is no longer dimmed end to end.** ድርሳነ ሚካኤል is written in
  Amharic rather than translated into it, and its 591 paragraphs were being
  indented and greyed as though the whole book were a footnote to something.

## [1.9.9] — 2026-09-09

_versionCode 65 · What the day already knew_

### Added
- **The reading plan has a daily nudge**, at 06:30 unless changed. It stays
  silent until a plan is actually begun, never asks about a day already read,
  and names the passage when it does speak — "ዕለት ፵፫ · ኦሪት ዘፍጥረት ፩–፫" — because
  a reminder worth opening says what it is for. After a week of being ignored
  it says so once and turns itself off, leaving the switch where you can turn
  it back on. It carries no streak and nothing at stake: the wording has to be
  equally true on a first morning and on the morning someone comes back after a
  month away.
- **The ግጻዌ's መዝሙር opens the whole hymn.** The lectionary only ever gives it by
  its opening words, because a printed ግጻዌ names the chant and the chant lives
  in another book — and that book is on the shelf now. 50 of the 80 Sunday
  incipits resolve.

### Fixed
- **Two of the ግጻዌ widget's tiers had never once been drawn.** The footer and
  the ቅዳሴ line each asked for 128dp of granted height that the default size
  never gave. The card is rebuilt around what the launcher actually grants: one
  to five readings chosen by height, every step up adding a reading rather than
  an ornament. The date owns its line instead of being ellipsized beside the
  wordmark at the size most people leave it at.
- **A reading plan is started deliberately.** Tapping a plan card committed you
  to a six-month track dated from that moment; it asks first now, and says what
  it is signing you up for.
- **ቅዳሴ shows its anaphora whole.** The names rode the end of the header rule
  clipped to one line — exactly where a day naming several ቅዳሴዎች ran off the
  edge with no way to read it.
- **The third share-image shape was the first.** STORY and CARD both capped at
  1920, so any passage long enough to fill the frame produced the same image
  twice. A card stops at 1440 now and a story is the full 9:16.
- **The image ground chips carry a swatch of the ground they stand for.**
  Selected and unselected chips differ too little to tell apart, so ivory and
  green read as one choice offered twice.
- **ዓመታዊ በዓላት and ወርኀዊ በዓላት keep the Church's own words in both languages.**
  They name a kind of feast, not a frequency.
- **ውዳሴ ማርያም and ሥርዓተ ማኅሌት no longer share the music icon** with each other and
  with the rest of the shelf.
- The ስንቅ wordmark is set in the reading face, and larger.
- A ማሳሰቢያ block is dropped from the shelf: it is an editorial notice to the
  singer rather than part of the book, and in የተክሌ አቋቋም ዝማሜ it is a bare heading
  whose text the scan lost entirely.

## [1.9.0] — 2026-09-08

_versionCode 64 · The books behind the books_

### Added
- **ሌሎች መጻሕፍት — 96 scanned church books.** The መልክእ hymns, the ድርሳናት, the
  ገድላት, ሥርዓተ ቅዳሴ, the chant books of ቅዱስ ያሬድ, and መጽሐፈ ሰዓታት. Grouped the way
  the Church groups them, because seventy of the ninety-six are መልክእ and a flat
  list reads as one run of near-identical names. Three million characters,
  2.2 MB in the release. A book loads only when it is opened.
- **መጽሐፈ ሰዓታት, merged from its three scanned copies.** The bilingual copy is
  the spine — 94% of the Ge'ez-only copy is inside it word for word — and the
  Ge'ez copy restores the 54 lines its scan dropped. The twelve offices only
  ዘደብረ ዓባይ carries are labelled with that name. It is not the Agpeya, and it
  is shelved well away from it.
- **ስንክሳር in Ge'ez.** መጽሐፈ ስንክሳር በግእዝ, all 366 days, switched by a pill. The
  two are parallel editions rather than a parallel text — only 30 of the 366
  days have the same paragraph count — so they are read one at a time and never
  set side by side.
- **The day's saints and the day's reading, in ስንክሳር.** 3,558 named
  commemorations across the year, annual and ወርኀዊ, and the scripture each day
  closes on with its citation — neither of which the old text carried.
- **ማኅሌት reaches the whole year.** 142 orders where there were 37: the ግጻዌ's
  own stopped after ሚያዝያ and had nothing for ግንቦት through ጳጉሜን. Today's order
  opens the list when the year appoints one.
- **ዘመነ ጽጌ knows its dates.** The season floats, so the ግጻዌ could only give it
  as an ordinal week. The አቋቋም books give an order for every date its Sundays
  can land on — 41 of them — and the app works out which applies this year.
- **A ማኅሌት part named after a book opens it.** 514 of the book's 2,247 parts
  are named for a hymn on the shelf; the ማኅሌት gives the stanza the feast
  appoints, and the shelf has the whole hymn.
- **ማኅሌተ ጽጌ and ሰቆቃወ ድንግል with their Amharic.** ማኅሌተ ጽጌ set verse against
  verse; ሰቆቃወ ድንግል's Amharic is one verse short of its Ge'ez and nothing says
  which, so it is given whole under its own heading rather than mispaired.
- **ንባብ has a ዳዊት track.** One psalm a day, all 150, about a minute of
  reading. The other two tracks leave the Psalter out because it is "prayed in
  the hours" — but only 77 of the 150 psalms appear whole in an hour, and the
  ግጻዌ cites the rest in ምስባክ fragments of a verse or two. 698 psalm verses were
  in no hour and in no citation; this is the track that reaches them. A psalm
  is never split and never doubled up, so መዝሙር ፻፲፰ is a day and መዝሙር ፻፲፮ is a
  day. The two existing tracks are unchanged, to the byte.

### Fixed
- **The አርኬ has its colour back**, on the label and the verse. It came off with
  the entry colouring, but the red was never entry colouring — Ethiopic
  manuscripts mark a section title in cinnabar and every printed liturgy sets
  the sung text in black and the rubric in red.
- **ስንክሳር reads in one voice.** A day passed through three treatments of the
  same text — a shrunk, muted opening, a full-size commemoration, and prose
  again after the አርኬ — while the አርኬ, the one thing that should differ, did
  not. And the "no ስንክሳር today" panel goes: all 366 days carry entries.
- **The three compact cards on ቤት drew nothing at all.** `SinqCard` lays its
  content out in a column, and the cards put a weighted column inside it, which
  in a column sized by its content measures to zero.
- **ቤት and ጉዞ have the ስንቅ mark back**, as a mark rather than the old
  headline: taking it out bought vertical space and cost both pages their top
  edge.
- **22 broken words in ውዳሴ ማርያም**, checked against an independent
  transcription. Fourteen had a space dropped into the middle of a word — the
  worst split እግዚ አብሔር — five ran two words together, and three had lost a
  letter as well. The ይዌድስዋ መላእክት litany is corrected against the printed book.
- **A ዳዊት day opens the Psalter**, not the Bible reader, which does not carry
  the Psalter at all.
- **Four ማኅሌት of ዘመነ ጽጌ had become unreachable.** The index groups a feast with
  its services, but ጽጌ is not that shape — it holds six numbered weeks, each
  with its own order — so five collapsed into one row and four could be opened
  nowhere. 94 parts of transcribed ማኅሌት were behind that row.
- **A verse with two reference lists lost the second.** The reader hands them to
  the parser joined, and the parser only read from the start of the string, so
  everything after the first citation went — and the bare "chapter፥verse" that
  followed inherited the wrong book, putting መዝሙር ፴፰፥፲፪ under ሚክያስ. The book
  pattern was also matching the ፤ that separates citations.
- **በዓለ ትንሳኤ sat under a heading naming ዘመነ ጽጌ.** The undated group took its
  title from its first row.
- **Three faults in the Psalter.** መዝ ፻፲፰፥፯ ended with the ቤት acrostic letter
  and debris from the scan, sitting inside the verse — all 22 letters are
  already carried as headings. Six verses put a space in front of the Ethiopic
  comma. And the Ge'ez Psalter had swallowed three verses whole: መዝ ፭፥፯ was
  inside ፭፥፮, and መዝ ፻፵፥፭ and ፥፮ inside ፻፵፥፬, each with the verse marker still
  in the text saying where the boundary had been. The ምስባክ is chanted from that
  edition and cites it by verse.
- **The ግጻዌ page's ⋮ shared in a shape of its own.** Copy and share there
  ignored ቅዳና አጋራ while the very same passage from the selection bar obeyed it.
  Every menu in the app now formats a passage the one way.
- **The Bible catalogue can no longer disagree with the Bible.** It claimed
  2,459 Ge'ez verses for a Psalter that holds 2,462; nothing counted them. The
  content gate does now.
- **Opening notification settings crashed on Android 6 and 7**, which have no
  per-app notification page. Three screens called it unguarded; they fall back
  to the app's own settings page now.

### Changed
- **ስንክሳር is rebuilt from the printed book's own structure.** The አርኬ is a
  block in the source now, so the three hundred lines that inferred it from
  prose are gone — and the hymn is always centred, because there is no longer a
  case where one falls through and is set as running prose. The day's reading
  is one piece with its citation, which could previously be selected and shared
  on its own as though it were a paragraph of the book. No rules across the
  page either.
- **The "check for updates" switch goes.** It was orphaned when the settings
  were regrouped — readable but no longer settable — and it was never the real
  switch anyway: the Play bundle is built without the update notice entirely,
  because a Play build pointing at a page of APKs is a policy breach, and
  someone who installed the APK by hand wants to know when there is a new one.
- **Cross-references are behind a tap and they open.** Selecting a verse gives
  the bar a ማጣቀሻ action with the count, and the list opens as chips that go
  where they point. All 73 abbreviations the edition uses resolve, covering
  55,128 references.
- **ማኅሌት is set as an order of service.** Half the book is three refrains, so
  they are set as refrains; every part carries its ordinal, since one name
  repeats up to thirteen times in a single order; and the index lists 22 feasts
  where it listed 37 services.
- **The ዳዊት habit goes.** የዕለት ንባብ already covers reading, and a fifth daily
  tick for something the app marks on your behalf is a chore nobody chose.
- **ስንክሳር bookmarks point at the commemoration, not at its position.** The id
  was the entry's index within the day, so any change to how a day is cut
  renamed every entry after the first.

## [1.8.1] — 2026-09-08

_versionCode 63 · the chant was in the book all along_

### Added
- **Every Sunday now has its መዝሙር.** The Sunday Gitsawe (ግጻዌ ዘሰናብት ወመዝሙር)
  was reachable for only 47 of its 91 rows: ዘመነ ጽጌ, አስተምህሮ, ስብከት–ብርሃን–ኖላዊ,
  the ልደት Sundays and the whole of ክረምት through ጳጉሜን had no calendar rule, so
  from late መስከረም to ጥር and from ሰኔ ፳፬ to the new year the link never
  appeared. A new calendar counts those seasons from their fixed anchors the way
  the book does, every row carries its selector, and a feast that lands on a
  Sunday (ሐዋርያት, ቂርቆስ, ደብረ ታቦር, ዕንባቆም, ስምዖን, a ጳጉሜን Sunday …) takes its own
  hymn — except inside Great Lent, which outranks them.
- **The hymn is on the page, not behind a link.** The ግጻዌ day screen shows the
  incipit of the Sunday's መዝሙር in a card that opens the full Sunday readings,
  and ቤት names it under the day's reading.
- **The season line knows the whole year.** Above the readings the accent line
  now names ዘመነ ጽጌ, አስተምህሮ, ስብከት, ብርሃን, ኖላዊ, ዘመነ ልደት, the four parts of
  ክረምት, ጾመ ፍልሰታ and ጳጉሜን, not only Lent and the Resurrection.

### Changed
- **ቅዱሳት መጻሕፍት opens the whole canon.** The book list matched a book's
  testament against "old" or "new", and eighteen books are neither: ጦቢት, ዮዲት,
  ጥበብ, ሲራክ, ባሮክ, መቃብያን, ዕዝራ ሱቱኤል, ኩፋሌ, ሄኖክ and the rest had no door in the
  Library at all. ብሉይ now carries them, under their own heading — and every
  heading is in Amharic (ኦሪት, መጻሕፍተ ታሪክ, ዐበይት ነቢያት, መልእክታተ ጳውሎስ …) where the
  page used to print the catalogue's English keys.
- **The book list follows ፍትሐ ነገሥት አንቀጽ ፪.** The bundled catalogue is not the
  Church's: it filed everything past the Hebrew thirty-nine as one
  "deuterocanonical" block, which the EOTC does not have. Every one of those
  books is counted inside a section that already existed — ኩፋሌ with ኦሪት
  ዘፍጥረት, ባሮክ and ተረፈ ኤርምያስ inside ትንቢተ ኤርምያስ, ሶስና and ሠለስቱ ደቂቅ inside
  ትንቢተ ዳንኤል, ጦቢት and ዮዲት and መቃብያን among the histories, ሲራክ and ጥበብ among
  the wisdom books. The headings are the Church's own names for them —
  የሕግ መጻሕፍት, የታሪክ መጻሕፍት, የጥበብና የመዝሙር መጻሕፍት, የሥርዓት መጻሕፍት. ዮሴፍ ወልደ ኮርዮን
  (ዜና አይሁድ) is the last of the seventeen histories, not a ninth book of order,
  so that group is the eight ፍትሐ ነገሥት lists. ወደ ዕብራውያን leads the fourteen
  Pauline epistles instead of sitting with the catholic seven.
- **The ቅዳሴ readings are in the order the liturgy reads them** — ጳውሎስ, ሐዋርያ,
  ግብረ ሐዋርያት, ምስባክ, ወንጌል — and each is named by its text rather than by whose
  turn it is to read it. The page had the Gospel first and ጳውሎስ third.
- **Falling behind on ንባብ offers two answers, and they differ.** Three buttons
  did two things: two of them shifted the plan by the same call, and the third
  marked today read without it having been read. Now: *ካልተነበበው ቀጥል* keeps the
  daily reading and lets the finish date move, *ቀሪውን አከፋፍል* keeps the finish
  date and shares what is left across the days that remain — each saying which
  it costs. The repacking was written and tested long ago and had never been
  connected to anything.
- **Copying no longer signs the clipboard.** A share leaves for someone else and
  says where it came from; a copy is Scripture quoted into one's own notes, and
  "— ስንቅ" in it was only ever something to delete.
- **ፍለጋ searches all of a book, not the first forty chapters of it.** The cap
  cut the *search* off in bundled order, so ኢየሱስ stopped inside ማርቆስ and ሉቃስ,
  ዮሐንስ, ግብረ ሐዋርያት and the epistles could not be reached; እግዚአብሔር never left
  ኦሪት ዘፍጥረት. Every match is found now, each group says how many it has, and a
  long one is drawn a page at a time.
- **ብሉይ ኪዳን** is called that on every screen; the book list said ቀዳማዊ ኪዳን.
- **ጉዞ records what the app can see.** Reaching the foot of an hour marks that
  hour prayed; reading today's ስንክሳር marks ስንክሳር; finishing the day's portion
  of the Psalter marks a new **ዳዊት** habit; and marking a day of ንባብ marks the
  daily reading. The page used to know only what was typed into it, so a
  morning spent in ጸሎተ ነግህ left the day blank unless it was also ticked.
- **"Prayed" means one thing.** ቤት's candle counted prayer hours and ጉዞ counted
  any mark at all, so a day with only ስንክሳር read was prayed on one screen and
  not on the other. Both count the hours now; everything else is counted as
  keeping, which is what it is.
- **Habits can be kept on a cadence.** ስግደት on Wednesday and Friday, ቤተ
  ክርስቲያን on Sunday, anything on the ግእዝ month day it belongs to — the same
  five rhythms ምጽዋት and ስዕለት already used, set under the habit's name in
  ልማዶች አስተካክል. A habit not asked for today is not on today's list, is not
  named as outstanding by the night reminder, and is not counted against the
  day. Habits without a cadence are daily, exactly as before.
- **The heatmap counts what was kept, not a share of what could be.** Level 4
  wanted nine things in one day once the seven hours and five habits were
  showing, so a week of morning prayer never darkened past the palest step and
  adding a habit dimmed the whole past. The scale is absolute now — seven is a
  full day, one is always visible.

- **ቤት fits on one screen.** The page cost 711 dp at the default font size, so
  the ንባብ card and half of ዛሬ sat below the fold on a common phone; it is 460
  dp now, with room to spare up to font scale 1.5. The wordmark went and the
  Ethiopian date became the headline. The hours strip is gone as a separate
  line — it repeated the hero's own hour before saying anything new, so "ቀጥሎ …"
  moved into the hero's foot. The ግጻዌ hero drops the kicker that said "today's
  ግጻዌ" above a title saying the same, and keeps the day's name. ዳዊት, ዘወትር and
  ንባብ share one shape, one line each rather than three. And **ዛሬ moves to the
  foot**, a row between hairlines instead of a card in the middle: it is a
  summary of the page above it, not a task on it. The ten-week heatmap is
  unchanged.
- **ግጻዌ lands the whole ቅዳሴ on the first screen.** 1,296 dp became 940. The
  ስንክሳር hero stays first and full-width, but now carries the day's own name
  instead of saying "ስንክሳር" and "የዕለቱ ስንክሳር" one above the other — and the
  day's title, which used to be printed again a few dp below it, is that name.
  What is left under the hero is one quiet line saying which of the book's
  cycles the day came from and the page it is printed on. A reading row is 60
  dp rather than 92: reference and incipit, one line each, with ምስባክ the
  exception since it is the chant itself and not an incipit. The anaphora moved
  onto the ቅዳሴ header it names, from a section of its own at the foot of the
  page four readings away.
- **ጉዞ shows the day and the year at once.** 767 dp became 571. The page title
  went — the tab below it already says ጉዞ. The seven hours are two hairline
  strips, four names to a line with colour the only state, instead of a ጸሎት row
  that opened seven 48 dp check rows: the page could not show a day's prayer
  and a day's habits at the same time. Habits are a compact list — a small
  hollow ring that fills with a check, the name, and the days kept of the days
  asked for, so a Sunday habit reads ፬/፬ and not ፬/፴. It takes a new habit
  without pushing the year grid off the page. The year switcher folded into the
  section header it belongs to. The hero names which hours were prayed.
- **ማስታወሻ shows the month, not just its entries.** A strip of the month's days
  sits above the list: written days in gold, fast days in the same green wash
  the year grid uses, a ring on today, and a tap scrolls to that day. The page
  is built on the month as the unit of looking back and could not show one.
- **The prayer hero says whether the hour has been prayed.** It carries an አሁን
  chip while the hour is due and a lit candle once it is prayed — which the app
  now records for itself when the hour is read to its foot. The page's first
  glance could not answer the day's first question before.

- **One selection bar, in every reader.** Tap a verse in the Bible, a psalm in
  ዳዊት, a verse inside ጸሎተ ነግህ, a paragraph of ስንክሳር or ውዳሴ ማርያም, and the same
  actions appear in the same order — ምልክት, ማስታወሻ, ቅዳ, አጋራ, ምስል — under the
  citation of what is selected. Readers whose unit is a paragraph simply have no
  colour row. The four highlight colours can be named (ተስፋ, ትእዛዝ, ጸሎት, ማስተዋል,
  or anything else) and the name shows under the swatch.
- **ስንክሳር reads as one page of the book.** An entry of the day was drawn one
  way, but of the 2,308 in the book 852 are a commemoration, 715 are lists of
  feast names, 380 are scripture quotations and 361 are the day's opening
  doxology under a heading naming the month. Each is set as what it is now: a
  life's title opens its first paragraph, because "በዚችም ዕለት ቅዱስ ቲቶ ረድእ አረፈ ።"
  is the sentence the account begins with and centring it in gold broke a
  sentence in half; the month heading is gone, since the page already says the
  day; a list is a list. No entry is headed in centred gold any more, not the
  lists and not the quotations: a centred gold line reads as a title and none of
  them are titles. Colour and a rule no longer carry the structure — the space
  after each entry does. 59 entries with no text are not drawn.
- **ስንክሳር can be read through.** ‹ › and the Ethiopian date picker are in its
  top bar, a contents sheet lists the day's commemorations, and it has a row in
  ቤተ መጻሕፍት — 1.62 million characters whose only doors were today's ግጻዌ and a
  search hit. Any commemoration takes a bookmark now, not only the 380
  scripture quotes.
- **ሥርዓተ ማኅሌት is on the page.** 37 orders of service and 732 sung parts have
  been shipping inside the app since the lectionary was imported, parsed on
  demand and thrown away because no screen ever asked for them. Now a feast day
  offers its ዋዜማ and its ነግሥ on ግጻዌ, and ቤተ መጻሕፍት opens the whole book grouped
  by month. Each order reads as its movements in sung order — ነግሥ, ዚቅ, ወረብ,
  አመላለስ — so the selection bar comes with it and a ዚቅ copies and shares like any
  other passage. Nothing needed a new calendar: 31 of the 37 hang off a fixed
  feast date, five off the ዘመነ ጽጌ week the Sunday calendar already computes, and
  one off ፋሲካ. Three sub-feasts have no order transcribed yet; the list names
  them and says so rather than hiding them.
- **"Change day" opens the Church's calendar.** ግጻዌ is dated ጳጉሜን ፪ and cited
  in Ge'ez numerals, and its date button opened a Gregorian grid of Arabic
  digits — so finding መስከረም ፩ meant knowing it falls on 11 September, which is
  the arithmetic this app exists to do for you. Thirteen months, Ge'ez numerals,
  and ጳጉሜን with its five days or six.
- **One liturgy, one name.** The lectionary writes the anaphora as free text and
  does it 120 ways across the year for thirteen anaphoras — with and without the
  ዘ- prefix, as the council's number or its byname, with the incipit in brackets,
  with ጐ or ጎ, and with half a dozen typos. The ግጻዌ header printed each variant
  verbatim, so one ቅዳሴ appeared under four names in a week. It is named once now,
  with its incipit beside it.
- **ፍለጋ ranks what it finds, and reads Ge'ez numerals.** Corpus order was the
  only order, so a search for a book's own name led with whatever the bundle
  listed first; a title match now leads, then a whole word, then a fragment
  inside one. "መዝሙር ፶" opens psalm 50 — every number this app prints is a Ge'ez
  numeral and the reference parser knew only Arabic digits. The ግዕዝ Psalter is
  searched too, the field takes focus when the screen opens, and a query can be
  cleared without holding backspace.
- **ቅንብሮች has groups, and መዝገብ has a page.** Nine flat rows under no headings
  became four groups. A new **መዝገብ** gathers what the app keeps a record of —
  አስራት, ስዕለት, ቀኖና, ምልክቶቼ, የጸሎት ዝርዝር, አጽዋማት — three of which were children of
  the reminders page (a money ledger under an alarm) and three of which lived
  only behind ቤት's ⋮ and were reachable nowhere else. The reminders page keeps
  only what rings, and is called **ማንቂያዎች**: ማስታወሻ is the journal, and one word
  cannot name a diary and an alarm. Reading mode and the ምስባክ language, which
  existed only inside a reader's ⋮ menu, are on the ንባብ page too.
- **The verse image has three shapes and two grounds.** Card as before, square
  for the previews Telegram and Instagram crop to, story for what people post;
  green or ivory. The typography and the colophon do not change — it is still
  ስንቅ in all three. And the day's ምስባክ has its own button on ግጻዌ, so the chant
  goes out as a card in one tap instead of four.
- **A chapter of ንባብ can be marked from where it is read.** A Bible chapter that
  belongs to today's plan day says so at the foot of the page, offers the next
  one, and takes the tick — the plan's ledger was asking twice.
- **The Bible reader reads like a book.** A chapter steps from the foot of the
  page instead of only from the strip at the top, and a book reopens at the
  chapter it was left on. The edition's own headings are on the page — a psalm's
  superscription, the note that opens ሲኖዶስ — and its cross references can be
  turned on under ቅንብሮች › ንባብ. The parser had been dropping both: 22,905 verses
  carry references and none of them had ever been shown.
- **ምልክቶቼ — one home for every mark.** ዕልባቶች, ማድመቂያዎች and ማስታወሻዎች in three
  tabs, reachable from ቤተ መጻሕፍት, from ቅንብሮች › ውሂብ, and from the ⋮ on ቤት where
  the bookmarks list used to be. A highlight had no list at all before: the only
  way to find one was to remember where it was. Each row is the citation, the
  colour's name, and two lines of the text resolved from the bundle — nothing is
  stored, so a mark cannot go stale. Verses painted in one run show as one mark.
  ወደ ፋይል writes the lot as plain text through the system file picker.
- **ቅዳና አጋራ, in ቅንብሮች › ንባብ.** Whether verse numbers, the chapter and verse,
  and the edition travel with a copied passage is now the reader's choice, shown
  against a live sample. The Psalter names which edition a verse came from, so a
  ግዕዝ verse no longer arrives claiming to be the Amharic.

### Removed
- **The አትናቴዎስ funeral lectionary.** 25 sections of the burial rite were
  bundled, parsed and never shown. It is out of the app, out of the source
  parts, and out of the importer. The licences page credits the same scan by
  the collections that stayed.

### Fixed
- **A bookmark can be a verse.** The Bible reader could only bookmark a whole
  chapter, from a control in its top bar that no other reader had. The selection
  bar marks the verses actually chosen, in every reader, and the route it stores
  reopens them tinted. Chapter bookmarks already saved keep working.
- **Eight books were headed "null".** `canon.json` writes JSON null for the
  section of every book of church order, and reading it as a string yields the
  four characters "null" rather than nothing — so ዲድስቅልያ, ቀሌምንጦስ and the rest
  were grouped under a heading spelling that out.
- **ንባብ recorded day numbers, and a day number means nothing.** Progress was a
  set of numbers per plan, so repacking the plan to finish on time, or a new
  bundled plan, silently re-pointed them: a reader who had read ኦሪት ዘሌዋውያን
  would be asked for it again, and days already read could come back unread.
  What is stored is chapters, and a day counts as read when every chapter it
  asks for has been. Existing progress is converted once at launch.
- **"ስለዚህ ጻፍ" wrote nothing.** The Psalter's menu item was wired to an empty
  callback and only the ግጻዌ passage page ever reached the journal, so writing
  from a verse was impossible in the two places verses are read. The Psalter,
  the Bible reader and the prayer hours all open a note now — anchored to the
  verses selected, or to the chapter or section in view — and an entry written
  about a passage can open that passage again, which the stored route always
  allowed and nothing ever used.
- **A ምስባክ preview could show the prayers for the dead.** The row sliced the
  verses out of the Ge'ez Psalter, which prints each psalm's closing Gloria
  inside its last verse — and at the six section ends a long intercession for
  departed bishops, kings and deacons. 99 of the year's 1,123 daily ምስባክ
  citations reach such a verse. The ግጻዌ prints the chant itself, and that is
  what the row shows. A citation with no closing verse now runs to the end of
  the psalm in the row as it already did on the passage page.
- **Shared prayers carried English references.** A verse from ጸሎተ ነግህ arrived
  as "John 1:1-17" under Amharic text; the hours' references are written in
  Latin by the content pipeline and are turned into the Church's own — Amharic
  book, Ge'ez chapter and verse — when the content loads. The ንባብ day and card
  no longer say "2 Kings 24–25" either.
- **A ስንክሳር search result opens the commemoration it matched.** The index had
  recorded which entry the hit was in all along, and the route threw it away, so
  a hit on a twelve-entry day landed at the top.
- **The reader ⋮ menu copies in the format you set.** It built its own text
  rather than going through ቅዳና አጋራ, so one passage left the app in two
  different shapes depending on which control you reached for.
- **A search result opens the verse it matched**, not the top of its chapter,
  and the first search says it is searching instead of "ምንም አልተገኘም" while the
  bundled books are still being read.
- **The journal's lock can be changed and taken off.** It could only ever be
  set; the icon now says whether one is on, and offers both.
- **ልማዶች አስተካክል** sits in ቅንብሮች beside ሰዓታት አስተካክል rather than at the foot
  of ጉዞ.
- **ታኅሣሥ ፳፰ (መርዓዊ) was keyed to the 23rd.** The scan prints ፳፰ — አማኑኤል's
  monthly day, the eve of ልደት.
- **The seasonal rows from the lectionary package used keys the app never
  resolved** (`genaTsom`, a ሰኔ `astemhro`, `zere_demena`). They now share the
  Sunday calendar's keys, so the same date selects both files.

## [1.7.4] — 2026-09-05

_versionCode 62 · the hours come back to the front_

### Added
- **The seven hours are on ቤት again, as one line.** Under the prayer card, built
  like the update notice above it: what is being prayed now, what follows —
  "ቀጥሎ ጸሎተ ሠለስት" — and ሁሉም for all seven. They had been behind the header menu
  since the shortcut pills were removed, which is a poor place for something used
  every day.

### Changed
- **The version check runs at every launch, not once a day.** The notice is only
  ever read with the app open, so opening it is the one moment the question
  matters; a release published this morning no longer waits until tomorrow. The
  stored ETag keeps the repeat cheap.

### Fixed
- **The tour's "Open it" button is gone.** It had to close the tour to navigate,
  so taking it meant never seeing the pages after it — the button competed with
  the thing it was part of.
- **Settings → የአዲሱ እትም ጉብኝት did nothing.** It read the empty value the content
  loader starts with as "there is no tour" and closed itself on its first frame,
  before the file had loaded.

## [1.7.3] — 2026-09-04

_versionCode 61 · it tells you what it just learned_

### Added
- **A walkthrough after an install or an update.** Six pages on what is new,
  each saying what a thing is for and where it lives rather than that it
  changed — and where one names a screen it offers to open it, so you end up in
  the feature instead of holding a description of it. Shown once, skippable,
  and replayable from Settings → የአዲሱ እትም ጉብኝት.
- **The first-run tour went from three pages to nine**: ግጻዌ, the hours,
  reminders, the Psalter, ንባብ, the journey, ማስታወሻ, አስራት and ስዕለት, and search.
- The opening page now closes with the mark and the version beneath it.
- Releases carry the Play bundle again, named `-play-upload.aab` so it is not
  mistaken for the file a phone installs.

### Changed
- **Checking for a new version is on by default.** Sinq is installed by hand, so
  an update nobody is told about is an update nobody gets — and left opt-in,
  nobody found the switch and the notice never appeared for anyone. One request
  a day, carrying nothing about you, and Settings can turn it off.
- **ዘወትር reads as prose.** It was verse-shaped in the data: 74 entries of which
  only 30 ended in a full stop, the rest being fragments split at the source's
  line breaks. Now 30 paragraphs, and the Ge'ez 17. The weekday ውዳሴ portions
  were already one sentence per line and are untouched.
- The hour shortcut pills are gone from ቤት; the hours moved into the header menu.

## [1.7.2] — 2026-09-04

_versionCode 60 · the home page stops squeezing_

### Fixed
- **ቤት squeezed its cards on small phones.** Every card was pinned to a fixed
  height so the dashboard fitted one screen without scrolling. That held while
  the page ended at መዝሙር and ዘወትር; the ንባብ card added a fourth block below them
  and the reading line was the first thing to lose room. Cards now wrap their
  own text and the page scrolls. The side-by-side pair matches the taller of the
  two rather than a height chosen in advance.
- **The update line's × was too small to hit** — a 36dp target, the only one in
  the app under the 48dp the design system asks for. The strip stays 28dp tall,
  but the × now takes a full 48dp of width and announces itself as a button.

### Changed
- **97 spacings and nine icon sizes across 22 screens are back on the scale.**
  An audit against `docs/DESIGN_SYSTEM.md`, which asks for no raw dp gaps and
  icons at 18/22/26 only. Most were already on the scale and only change name;
  the strays snap to their nearest step. Settings, About, Licenses and the
  battery help page held most of them — screens written before the scale did.
- በቅርቡ now uses the shared `StatePanel`, so "nothing here yet" reads in the same
  voice as every other empty state.

## [1.7.1] — 2026-09-04

_versionCode 59 · ንስሐ and ቁርባን wait for their text_

### Changed
- **ንስሐ ዝግጅት and ቁርባን ዝግጅት now say በቅርቡ.** Both shipped in 1.7.0 with text
  written for the app rather than received from the Church — an examination of
  conscience, the ሥርዓተ መቅረቢያ, and prayers before and after receiving. These are
  the two screens in Sinq that teach rather than record, and that is the wrong
  provenance for them, so the text is removed and the screens wait.
- The entry points stay visible and say so. The state follows the bundled
  content being empty, so adding the text turns the screens on with no code
  change.
- ቀኖና is unaffected and still works: it holds only what its owner enters.

## [1.7.0] — 2026-09-04

_versionCode 58 · what the Church does not read to you_

### Fixed
- **The app would not install on anything below Android 8.0.** `minSdk` was 26,
  held there only by `java.time` (used in some 50 files) and `java.util.Base64`
  in the journal lock. Core library desugaring carries both down, so the floor
  is now 23 — Android 6.0. Notification channels, which do not exist before
  Oreo, are behind version checks; `PBKDF2WithHmacSHA256`, which also needs 26,
  now records which algorithm hashed a passphrase so one set on Android 6 keeps
  verifying after its owner upgrades.
- **Release APKs now carry the v1 JAR signature.** v2 alone covers API 24 and
  up, which would have excluded the Android 6 devices the change is for.
- Two long-standing lint errors that were keeping the CI workflow red.

### Added
- **ንስሐ ዝግጅት** — a guided examination of conscience, walked one section at a
  time. The questions are read, never ticked, and the walk produces a single
  confession draft that "ንስሐ ገብቻለሁ" deletes.
- **ቀኖና** — the penance received from a ንስሐ አባት, with its measure and what has
  been done against it. It reminds until it is finished and then falls silent.
  Kept on the device only: never in a backup, and its label never appears in a
  notification.
- **ቁርባን ዝግጅት** — the order of approach as the tradition states it, with the
  prayers before and after receiving.
- **ንባብ** — a reading plan for the 1,067 chapters of the Old Testament and
  deuterocanon that the ግጻዌ never reaches, over a year or six months. The day's
  ግጻዌ is shown inside the plan, above the plan's own reading and never marked
  done. It is also the first screen to reach ሲራክ, ኩፋሌ, ሄኖክ and መቃብያን, which
  have been bundled all along with no way in.
- **A line on ቤት when a new version is out.** Opt-in and off by default; one
  request a day, the app's only use of the network. It opens the release page
  in the browser and never downloads anything itself.

### Changed
- The Journey page's vertical rhythm is a little tighter again.

## [1.6.1] — 2026-08-31

_versionCode 57 · the journal keeps what you wrote_

### Fixed
- **Journal entries could be lost when the editor closed.** The final save was
  launched from the screen's own `rememberCoroutineScope`, which Compose
  cancels at exactly the moment `onDispose` runs — so the write raced its own
  cancellation and could drop what had just been typed. The last write now goes
  to a process-lived scope, so a save that starts always finishes.
- **Nothing was written until the editor closed.** Switching apps or the system
  reclaiming the process left no record at all, despite a comment claiming
  otherwise. Entries now save a second after typing stops, so leaving the
  screen cleanly is no longer what the text depends on.

### Changed
- **The journal says when it has saved.** A tick appears in the entry header
  once the text is on disk. The editor still has no Save button, but "is this
  saved?" was a question the screen previously gave no way to answer.

## [1.6.0] — 2026-08-31

_versionCode 56 · what is owed, what was promised, what was thought_

### Added
- **አስራት.** A ledger for the tithe: record what you receive and the tenth is
  worked out for you; record what you give and it shows what is still owed,
  over an Ethiopian month or year. The share is adjustable for those who keep
  a different fraction, and amounts are counted in a currency you name. Giving
  beyond the tithe is shown as a surplus rather than a negative debt.
- **ስዕለት.** Vows and pledges, each tied to the feast it was promised on, with
  what was promised set against what has been kept. A one-time vow stops
  reminding once it is fulfilled; a standing one keeps its rhythm.
- **Feast-anchored reminders.** Cadences now include a yearly date and a named
  feast, alongside the weekly, every-other-day and monthly ones. The ወርኀዊ
  በዓላት come from the bundled ስንክሳር itself, so choosing "ቀን ፲፱" and choosing
  "ቅዱስ ገብርኤል" are the same act; movable feasts such as ፋሲካ are computed each
  year from the ባሕረ ሓሳብ.
- **ማስታወሻ — a journal.** Write the day down, browsed a ግእዝ month at a time.
  Every entry keeps the Church's day it was written on — the feast, the fast,
  the day's ግጻዌ — so an old entry reads as more than a date. A "write about
  this" action in the Psalter and the ግጻዌ reader starts an entry linked back
  to the passage. Entries can be locked behind a passphrase, and the journal
  never appears in screenshots or the app switcher.
- **Preparing for ንስሐ.** A kind of entry built to be destroyed: it never
  leaves the device, never enters a backup, and "ንስሐ ገብቻለሁ" deletes it rather
  than filing it away.
- **Choose what a backup contains.** Export is now a checklist rather than
  all-or-nothing. The journal is off by default and asks for your passphrase
  before it can be included — the file itself is plain text, so what happens
  to it afterwards is yours to look after.

### Changed
- **Quiet hours now silence every reminder.** They previously applied only to
  the ringing prayer alarms and the ሕሊና prayer; the nightly, ግጻዌ, ምጽዋት and
  ንስሐ notifications ignored them entirely. A silenced reminder still keeps its
  schedule — only that one occurrence is dropped. Because the silence is now
  total, the setting warns when a reminder is timed inside the window and
  would never arrive.
- **The Reminders page is grouped** — daily, giving, and sound — rather than
  one flat run of rows, and the reminder descriptions now speak in one voice
  instead of three.

### Fixed
- **The text-size control was labelled "የንባብ ፊደል"**, the same as the font
  picker directly above it, leaving the size stepper with no label of its own.
- **"Keep screen on" never showed its explanation**, though one was written
  and translated.
- **The last-backup date** was printed as a raw Gregorian date in an app that
  is otherwise ግእዝ-first everywhere.
- **"1 reminders on."** The English count had no singular form.

## [1.5.2] — 2026-08-31

_versionCode 55 · the alarm asks again_

### Fixed
- **"ጨርሰዋል?" comes back after an alarm.** The 1.5.0 notification rework hung
  that follow-up on the notification's delete intent, which only fires when a
  user clears a notification — not on the app-side cancel behind Dismiss, the
  auto-cancel behind Open, or the 60-second timeout. Only swiping the alarm
  away reached it. Every ending now posts the prompt explicitly, with the
  timeout carried by its own alarm; Snooze stays the one ending that asks
  nothing.

### Changed
- **The ጉዞ page is more compact.** About 80dp less chrome — a tighter heading,
  hero, and section rhythm — so more of the day's habits sit on screen. The
  year heatmap and the habit rows' touch targets are untouched.

## [1.5.1] — 2026-08-31

_versionCode 54 · the home page, restored_

### Fixed
- **The daily-psalms and ጸሎት ዘዘወትር cards are back on the home page.** 1.5.0
  removed the upper bound on the cards' heights to stop large text clipping,
  but each card holds an internal `weight()`; unbounded, those weights grew
  into the whole page and pushed the bottom two cards off a dashboard that
  does not scroll at normal text size. Heights are bounded again and now scale
  with the text instead of being outgrown by it.
- **The streak grid grows with its card.** The heatmap and candle are drawn in
  dp, so a card stretched by a large text size left them adrift in a
  half-empty box; they now scale with the card (capped so the grid cannot
  crowd out the reading beside it).

### Changed
- **The ስንክሳር button says which day it opens.** It is now a filled button
  rather than a list row, and outside today it names the day in view —
  "የሐምල ፰ ስንክሳር" — so moving between days relabels it.
- **The ባሕረ ሐሳብ year card matches the rest of the app.** It used the Material
  primary colour, which resolves to a pale mint in dark theme; it now uses
  Sinq's own deep green and muted ivory like every other hero surface.

## [1.5.0] — 2026-08-31

_versionCode 53 · Play-Store readiness: everything from the pre-flight audit
except the Bible-rights question (deliberately held open)._

### Changed
- **Alarms ring without a foreground service.** The prayer alarm is now an
  insistent alarm-channel notification (alarm-stream sound + vibration looping
  up to 60s) — the `systemExempted` service and both foreground-service
  permissions are gone, and `SCHEDULE_EXACT_ALARM` (≤ API 32) fixes silently
  inexact alarms on Android 12/12L. Snooze, dismiss, and the "done?" follow-up
  are unchanged.
- **The ግጻዌ widget shows today, always, at any size.** No more 19:00 flip to
  tomorrow; the card renders directly (no collection service), adapts its
  rows/kidase/footer to the granted size, can shrink to a slim strip, rolls
  over just past midnight, and stops its refresh alarm when removed. The
  widget picker now has English text on English-system devices.
- **All Amharic UI text addresses the user in the polite plural.** Notification,
  backup, reminder, and prayer-list strings no longer slip into familiar
  masculine singular.
- **What's New is bilingual.** Every release entry now has an Amharic
  rendering; the page follows the app language.
- **A Licenses & sources screen.** Settings now carries the full attribution
  record: scripture (corrected scope), ግጻዌ, both Synaxarium sources (with the
  MIT notice), Wudase Maryam, all five fonts with the full OFL 1.1 text, and
  the app's own Apache-2.0.

### Fixed
- **A corrupted backup can no longer plant later crashes.** Restored reminder
  times are clamped and duplicate added-psalms deduplicated.
- **Search no longer permanently retains ~25–35 MB.** Indexing stops pinning
  parsed books, the book cache became an 8-entry LRU, and all content caches
  release under memory pressure.
- **Small hardening and polish.** Distinct alarm-notification request codes
  (taps can't misroute), negative-numeral and empty-book guards, home cards
  grow instead of clipping at large font scales, Synaxarium red meets light
  contrast, 48dp touch targets for the week-day toggle, memento-mori text
  follows the app language, and "coming soon" wording states facts instead.
- **Dead weight removed:** the Se'atat settings remnants, an unreferenced
  legacy settings screen, and an unused asset file.
- **The version is visible.** The What's New row shows the installed version
  (read from the package, never hardcoded again) and the Settings page closes
  with a quiet version footer. A hosted privacy policy now backs the Play
  listing, and the licenses screen records the Gitsawe transcription's
  open-content release and the Font.et provenance of all three local fonts.

## [1.4.0] — 2026-08-31

_versionCode 52 · the Psalter and the ግጻዌ agree_

### Changed
- **The Psalter now uses the Ge'ez (LXX) psalm numbering everywhere.** Both
  bundled editions were renumbered (`tools/renumber_psalms_geez.py`) so መዝሙር ፶
  is the Miserere and the numbers match every ግጻዌ citation — misbak links no
  longer land one psalm off (the reported 150/151 mismatch). Existing psalm
  bookmarks/highlights keyed by number will shift accordingly.
- **The ግጻዌ page turns with a swipe.** Sliding the page horizontally moves to
  the neighbouring day, and a permanent "ዛሬ" pill button returns to (and marks)
  today's ግጻዌ.
- **Manage-hours no longer offers manual reordering.** The up/down arrows are
  gone; hours keep their canonical order.

### Fixed
- **Prayer-list Marian conclusion spelling.** ጠጣሳት/ጠጣስ corrected to ጳጳሳት/ጳጳስ.
- **Toggling "Full Psalms" in an hour no longer crashes.** The paged reader
  guarded against the section list changing size mid-frame.
- **Release page only offers the installable APK.** The `.aab` (not installable
  on phones) moved off the public release assets into a workflow artifact.

## [1.3.2] — 2026-08-28

_versionCode 51 · Bahre Hasab renders correctly_

### Fixed
- **The Bahre Hasab year card shows real values.** The hero card no longer
  displays raw template text in place of the year, evangelist, and Fasika date.
- **Ge'ez numerals are correct beyond 199.** Years and ዓመተ ዓለም now render in
  proper positional notation (e.g. ፳፻፲፰ for 2018) instead of a run of repeated
  ፻ marks, across the year rail and cycle-value chips.

## [1.3.1] — 2026-08-28

_versionCode 50 · a focused, more accessible reading experience_

### Changed
- **The Gitsawe widget follows the day automatically.** It shows today's
  readings during the day and tomorrow's from 19:00, with a single uncluttered
  page and the restored dawn-and-cross Sinq mark.
- **Long-form readers are easier to use.** Scripture, Synaxarium, and Wudase
  Maryam now share the reading-alignment setting, readable tablet-width limits,
  explicit retry states, and clearer accessibility selection state.
- **Bahre Hasab is now a live year explorer.** A highlighted current year and
  horizontal year rail expose computus values and movable observances for the
  current Ethiopian year plus the next 25 years.
- **Incomplete specialist readers have been removed from the interface.** The
  dedicated Se'atat reader and Athanasius funeral collection no longer appear;
  the app's canonical prayer hours remain unchanged.

### Fixed
- **Small UI controls and widget text are more accessible.** The prayer-list
  remove action now has a full touch target, and compact widget labels meet the
  minimum supported text size.

## [1.3.0] — 2026-08-28

_versionCode 49 · the complete source-backed Gitsawe_

### Added
- **Movable weekday Gitsawe.** Readings for Nineveh, Heraclius, Great Lent,
  Rikbe Kahnat, Ascension, and the Apostles' Fast now follow the Ethiopian
  computus instead of being limited to the fixed calendar.
- **Sunday Gitsawe and mezmur.** Sundays with an unambiguous printed date or
  movable-season rule expose their additional readings and hymns from the daily
  Gitsawe, with valid citations opening directly in Scripture.
- **Athanasius funeral and memorial lectionary.** The Library now includes the
  source's funeral, burial, supplication, and memorial collections.
- **Bahre Hasab reference.** The printed 2001–2015 EC table is available in the
  Library as a historical reference, while live dates continue to use the
  app's computus.
- **Text alignment controls.** Reading settings now offer justified, left,
  right, and centred text across the app's reading surfaces.

### Changed
- **A new Sinq launcher mark.** Seven pieces form the Provision cross, tying
  the app icon to the seven canonical prayer hours and Sinq's name.
- **The complete licensed Gitsawe source is reproducible.** Parts 1–5 retain
  scan provenance, source-preserving splits, importers, and validation tests.
- **Content rights are explicit.** Repository and in-app asset notices now
  distinguish the separately licensed Gitsawe transcription from Apache-2.0
  code and CC-licensed Scripture.

## [1.2.0] — 2026-08-28

_versionCode 48 · the complete fixed-cycle Gitsawe_

### Added
- **Every Ethiopian calendar day now has a fixed-cycle Gitsawe entry.** The 65
  previously missing dates are bundled, bringing coverage to all 366 possible
  month-days, including leap-year Pagumen 6.
- **The evening office is now visible.** Source-backed ሠርክ readings appear after
  ነግህ and ቅዳሴ and are included when sharing the day's Gitsawe. Hidar 28
  remains without ሠርክ because the transcribed source explicitly omits it.

### Changed
- **Printed but malformed citations remain readable without becoming broken
  links.** Sinq preserves their source text while only making validated chapter
  references tappable.
- **The fixed calendar import is reproducible.** A checked-in importer merges
  newly transcribed days and evening offices without replacing existing
  translations or synaxarium notes.

## [1.0.5] — 2026-08-24

_versionCode 45 · quieter alarms, daytime breath prayer_

### Changed
- **Prayer alarms stay in the notification shade.** Ringing reminders no longer
  launch or wake a full-screen activity; Open, Snooze, and Dismiss remain
  available directly on the ongoing notification.
- **Breath prayer follows the waking day.** Its single daily time is now chosen
  randomly from the active mode's Morning prayer time through 21:00, without
  being re-rolled by completed prayers or constrained by later prayer alarms.

## [1.0.4] — 2026-08-24

_versionCode 44 · a clearer Library, an accessible Journey_

### Changed
- **The year's journey can be explored without tiny touch targets.** Every
  available heatmap day now has a spoken date, habit count, fasting context,
  and selected state, while full-size previous and next controls provide a
  comfortable way to move through the calendar.
- **Journey controls explain their state.** The prayer-hours disclosure now
  announces whether it is expanded or collapsed, and the heatmap legend and
  monthly summaries adapt more safely to narrow screens and larger text.
- **Library descriptions follow the interface language.** Wudase Maryam and
  Zewotr supporting copy now appears in Amharic or English as selected.

### Fixed
- **Journey stays on the right day.** Its date advances at local midnight and
  refreshes when the app resumes, preventing an overnight session from writing
  prayer records to yesterday.
- **Year and day selection remain consistent.** Switching heatmap years now
  selects a valid day in the displayed year, and dates before Sinq's data epoch
  are no longer interactive.

## [1.0.3] — 2026-08-24

_versionCode 43 · today's path, given room to be seen_

### Changed
- **Today's progress has a row of its own.** The Home dashboard now gives the
  completion count, candle, Journey status, and ten-week heatmap the full screen
  width instead of sharing a half-width card.
- **Daily readings sit together.** Today's Psalms and ዘወትር ጸሎት / ውዳሴ
  ማርያም appear as compact companion cards, with the latter opening directly to
  the existing daily Wudase reading.
- **The new reading row remains adaptive.** Its cards sit side by side on normal
  phones and stack on narrow screens or with larger accessibility text.

## [1.0.2] — 2026-08-24

_versionCode 42 · the day at a glance, settings with room to breathe_

### Added
- **Focused Settings pages.** Reading, prayer, reminders, and local data now
  have dedicated screens, while the Settings landing page keeps only the eight
  choices people need to scan.
- **Reading comfort controls.** A real prayer-text preview now accompanies a
  safely migrated 16–28sp size scale, four Ethiopic font previews, and compact,
  normal, or relaxed line spacing shared by the app's readers.
- **Editable quiet hours and reminder health warnings.** Start and end times can
  be changed directly, and notification or background restrictions are shown
  while relevant and rechecked when the app resumes.
- **Backup recency.** Successful local backups record their time so Settings can
  show whether the last backup was today, yesterday, or earlier.

### Changed
- **Home is a glanceable dashboard.** Current and next prayer shortcuts replace
  the expanding hour list, all hours remain available in a sheet, and the full
  Gitsawe feast and reading card stays visible alongside today's heatmap and
  Psalter portion.
- **Reminder controls are coherent.** Prayer level uses an explanatory radio
  sheet, alert behavior and sound share one sheet, and the randomized reminder
  is now named የሕሊና ጸሎት (Prayer of the heart).
- **Settings remain green and gold.** The visual identity, selected checks, and
  gold accents are preserved while spacing and navigation become calmer.

### Fixed
- **Home stays current across time boundaries.** The date, suggested prayer,
  daily Gitsawe, progress, and Psalter portion refresh while Home is visible.
- **Settings adapt safely.** Narrow screens, large accessibility text, and wide
  displays reflow instead of truncating localized choices or stretching cards.

## [1.0.1] — 2026-08-23

_versionCode 41 · prayer that meets people where they are_

### Added
- **Five selectable Agpeya prayer levels.** Settings now offers መዝሙር ፶,
  መጀመሪያ, እድገት, ጽናት, and ሙሉ, with a curated, progressive Psalm
  priority for every hour and the larger 7/14/24/full progression at Midnight.
- **Continue with the complete hour.** A reader using a shorter level can reveal
  all Psalms for the current hour without changing the saved default.

### Changed
- **The Gospel remains foundational at every level.** Shorter levels affect
  only the Psalms; every Gospel reading remains present and concludes its hour
  or Midnight watch. The complete level preserves all bundled prayer content.
- **Prayer-level allocation is offline and data-driven.** Psalm priorities and
  the Psalm 50-only choice live in bundled configuration rather than UI code.

## [1.0] — 2026-08-23

_versionCode 40 · the first stable Sinq release_

### Added
- **Persistent Bible highlighting and dependable verse sharing.** Bible verses
  can now keep one of four highlight colours, selected ranges update in one
  atomic operation, and copied/shared passages retain their book or reference.
  Existing Psalm highlights migrate safely to the Amharic 1980 edition.
- **A complete daily checklist.** Sinkisar joins church and prostrations as a
  trackable practice, and the nightly reminder names what remains without being
  suppressed when an hour of prayer has already been marked.
- **Creator attribution.** About now links “Built by Natinael M.” directly to
  `@natinael96` on Telegram.

### Changed
- **The offline Bible now uses Amharic 1980 throughout.** The unused Amharic
  2000 bundle and non-Psalm Ge'ez books have been removed; Ge'ez 1980 remains
  available specifically for Psalms. This cuts the optimized APK substantially
  while keeping the requested reading library offline.
- **Home makes the day's paths visible.** Hours is an early, compact expandable
  card, and Today's Gitsawe uses the same devotional green-and-gold hero
  treatment as Prayer for now.
- **Prayer List is denser and easier to scan.** Prayers use an adaptive two-column
  card grid, with the Marian prayer presented as a distinct concluding element.
- **Reader and Settings headers adapt to narrow screens.** Long titles ellipsize
  cleanly, secondary reader actions live in a compact tools menu, and Settings
  rows no longer crowd or clip their current value.
- **Selection actions are responsive and accessible.** Highlight colours and
  copy/share actions use an inset-safe two-row panel, announce individual colour
  names to screen readers, and dismiss after completing an action.

### Fixed
- **Fasting boundaries are exact.** ጾመ ሐዋርያት ends on ሐምሌ 4 and ጾመ
  ፍልሠታ ends on ነሐሴ 15 without an added day; ጾመ ነቢያት and ዐቢይ ጾም
  retain their existing calculations.
- **Sinkisar search no longer crashes on valid entries.** Same-day results have
  stable unique identities, and Ge'ez text, Ethiopic punctuation, empty queries,
  no-result queries, and result navigation are covered by regression tests.
- **Psalm highlights cannot cross translations incorrectly.** Amharic and Ge'ez
  use separate identities, and switching language clears any pending selection
  before copy, share, or highlight can target different versification.
- **Reminder chains recover reliably.** Breath-prayer and daily reminder alarms
  re-arm independently after firing and after reboot, update, time, or timezone
  changes.
- **Image shares cannot overwrite one another.** Each generated passage card has
  a unique cache file and sharing failures are handled without crashing.

## [0.10] — 2026-08-22

_versionCode 39 · one Scripture, one day's Gitsawe_

### Added
- **The complete Ethiopian Orthodox Bible is now in the Library.** Old and New
  Testaments read from the Amharic 2000 edition, fully offline. Scripture has a
  single home with Bible and Psalms as clear categories instead of two competing
  Library entries.
- **Psalms now carry their proper editions.** Amharic 1980 is the default and
  Ge'ez 1980 is available from the Psalm reader. Misbak remembers its own
  language choice without changing the rest of Scripture.

### Changed
- **የዕለቱ ግጻዌ now reads as a day, not a directory.** The selected Ethiopian
  and Gregorian date leads the page, previous and next days sit within reach,
  and a historical day returns to Today in one tap. Misbak and Scripture are
  distinct devotional preview cards that open into the focused cited passage.
- **Gitsawe continuity is explicit.** Misbak opens in Ge'ez by default and
  carries that choice into its Psalm; Scripture remains Amharic 2000 and leads
  naturally from passage to chapter to book. Existing bookmark and Gitsawe
  routes remain compatible.

### Removed
- The duplicated legacy `psalms.json` and New-Testament-only `content/scripture`
  bundle. All canonical reading content now comes from the unified, reproducible
  80-weahadu edition bundle.

## [0.9.9.8] — 2026-08-15

_versionCode 38 · the whole week fits_

### Fixed
- **The "how often" dialog no longer cuts off half the week.** Saturday and
  Sunday were being clipped off the right edge of the day row, so they could not
  be picked at all, and "Monthly" was squeezed into a column one letter tall.
  The cadence chips now wrap to a second line instead of being crushed, and each
  day takes an equal share of the row — all seven are visible and tappable on
  any screen width. Affects both the ምጽዋት and ንስሐ reminders, which share the
  dialog.

## [0.9.9.7] — 2026-08-15

_versionCode 37 · the reminder writes itself_

### Changed
- **The opening spinner is gone; the reminder is written instead.** Where the
  splash used to mark its pause with a small spinning ring, *Memento Mori* is
  now inked onto the screen letter by letter under a soft gold nib, then signed
  off with a hand-drawn underline; **Remember Death** and **ሞትን አስብ** settle
  beneath it. The words are drawn in the reading serif, scaled to fit narrow
  screens, and a tap still moves on early. Nothing spins — the wait reads as a
  moment of attention rather than a loading state. With animations turned off
  system-wide the words simply appear already written.

## [0.9.9.6] — 2026-08-15

_versionCode 36 · a reminder for every intention_

### Added
- **ምጽዋት and ንስሐ can hold many reminders now.** Each intention used to be a
  single nudge; now it is a list. Keep as many as you like, each with its own
  name (which rides in the notification so they read apart in the tray), its own
  cadence (weekly, every other day, or monthly on an Ethiopian day) and its own
  time. Your existing reminder migrates in as the first entry, unchanged. They
  are still intentions, not habits — nothing is recorded or shown as done.

### Changed
- **ሰዓታት is tucked away for now.** The bundled digitization is still partial
  (መሐረነ አብ and other portions are missing), so the Library card and its search
  entries are hidden until the text is whole. The reader, route and data stay
  intact for when it returns.

### Fixed
- **Search no longer goes blank behind a stale filter.** A source filter (say
  መጻሕፍት) chosen for one query stayed on for the next; if the new query didn't
  match that source, the results vanished even though other sources had matches.
  The filter now falls back to "All" whenever it names a source the current
  query didn't hit.

## [0.9.9.5] — 2026-08-15

_versionCode 35 · the widget shows where it stands_

### Changed
- **The ግጻዌ widget got its polish.** Each card now carries a page indicator —
  gold for the shown day, muted for the other — baked into the card itself so
  the dots always agree with what's visible; with only one day available they
  disappear along with the swipe. Today is explicitly labelled ዛሬ (symmetric
  with ነገ), a gold "ግጻዌውን ክፈት →" action line names what a tap does, and every
  refresh snaps the stack back to Today so a browse to tomorrow never lingers
  overnight. Tomorrow's card only exists when tomorrow actually has readings;
  an empty today says so honestly instead of letting tomorrow pose as today.

## [0.9.9.4] — 2026-08-15

_versionCode 34 · the real መጽሐፈ ሰዓታት_

### Changed
- **ሰዓታት is now the real book.** The starter text is replaced by a full
  digitization of መጽሐፈ ሰዓታት ዘሌሊት ወዘነግህ (Tinsae Ze-Gubae edition, Addis
  Ababa, ፲፱፻፶፯ ዓ.ም.): the መቅድም on አባ ጊዮርጊስ ዘጋሥጫ, then all 42 sections in
  printed order — the fifteen ስብሐት parts of the night office alternating with
  the biblical canticles and the ምስለ intercessions — 199 Ge'ez–Amharic paired
  lines. It is one office, not four hours, so the reader now reads as one
  continuous scroll: the hour chips are gone, and a ይዘት (contents) sheet
  jumps to any section. Line pairing, the three language modes, tap-to-select
  sharing and the red typo-review flag all carry over; search now normalizes
  the traditional ፡ word separators, so queries typed with plain spaces match
  the printed text in either language.

## [0.9.9.3] — 2026-08-14

_versionCode 33 · ሰዓታት in the Library, and pages of their own_

### Added
- **ሰዓታት (Seatat) in the Library.** The prayers of the hours, Ge'ez-first with
  a line-by-line Amharic translation: each Amharic line sits directly under its
  Ge'ez line — smaller, italic, visually attached — so the page reads as one
  continuous prayer with an inline translation. Three persisted language modes
  (ግዕዝና አማርኛ · ግዕዝ · አማርኛ); hour chips (ጠዋት · ቀትር · ማታ · ሌሊት) that keep
  each hour's reading position; tap-to-select sharing and font sizing like the
  other readers; searchable in both languages from the unified search. A word
  marked `*` in the bundled text renders a small red asterisk — an in-app
  review flag for a spelling that still needs checking (stripped from shares
  and search).
- **Dedicated pages for ምጽዋት and ንስሐ.** Each scheduled intention now opens its
  own page from Settings — toggle, cadence, time, and the next due day in the
  Ethiopian calendar — instead of expanding rows inline.
- **A focused page for every ግጻዌ section.** Tapping ምስባክ, ወንጌል or any reading
  opens only the cited verses in the reading face, with the lectionary role in
  the header and two doors out: open the book, or open the chapter that holds
  the passage.

### Changed
- **Open-ended ግጻዌ citations read to the chapter's end.** A reference with a
  start verse and no end is not a single verse: the passage page shows from
  the start through the chapter's last verse, across all Gitsawe types.

## [0.9.9.2] — 2026-08-13

_versionCode 32 · the streak becomes a journey_

### Changed
- **ጉዞ (Journey) replaces the streak.** The tab, formerly Streak, keeps its
  Amharic name and gains it in English, under a path icon instead of a flame.
  The hero is today's candle — lit when prayer is recorded, waiting when not,
  never sized by history — beside the period's count of distinct prayer days:
  "በዚህ ወር ፳፫ ቀን ጸልየዋል" / "23 days of prayer this month", or during a fast
  "የዐቢይ ጾም ፲፰ኛ ቀን — ፲፮ ቀን ጸልየዋል" / "Day 18 of ዐቢይ ጾም — prayed 16 days".
  Missing a day changes nothing but that day; coming back after a gap shows
  only "ተመልሰዋል — ዛሬ ይጀምሩ" / "You're back — begin today". Home's Today card
  speaks the same line from the same source of truth. Prayer records and
  backups are untouched — the new count derives from the same day records.
- **The year heatmap is the historical view.** Promoted under its own header,
  with a quiet liturgical-green wash on unprayed fasting days and the fast
  named in the tapped-day readout — prayer history read inside the Church's
  year. Per-habit rows now count distinct days this month; current/longest
  runs are gone.
- **The nightly nudge stops carrying a number.** "ሰርክ ደርሷል — ዕለቱን በጸሎት ዝጉ"
  every night; "ጾሙ በጸሎት ይታጀብ" on a fasting day; the feast named on a feast.
  The wording never depends on history — nothing at stake, nothing to lose.
- **The ringing alarm names its hour** — "ሰርክ ደርሷል" over the generic
  "ጊዜው ደርሷል", on the full-screen alarm and the notification title.

### Added
- **Passage sharing from every reader.** A tap anchors a verse run, the next
  tap moves its end; a slide-up bar offers copy, share as text, or share as an
  image card — now also in ስንክሳር, ውዳሴ ማርያም and the New Testament reader.
- **የመሃል ጸሎት — the in-between prayer.** Once a day, at a random moment
  between the last recorded prayer and the next scheduled one, a notification
  carrying one short prayer in full. Praying, not reading: nothing to open.
- **ምጽዋት and ንስሐ reminders.** Scheduled intentions, deliberately not habits —
  the app reminds and then looks away; weekly, every other day, or monthly on
  an Ethiopian month day.
- **The ግጻዌ widget carries tomorrow.** A two-card stack: today's readings,
  and tomorrow's for preparing.

### Removed
- **The streak, everywhere.** No current or longest streak, no flame icon, no
  streak share card (passage sharing carries the meaning now), no "streak
  lost" state — and no setting to bring any of it back.

## [0.9.9.1] — 2026-08-13

_versionCode 31 · the streak nudge fires every night_

### Fixed
- **The nightly streak reminder now behaves like the morning ግጻዌ one.** It fires
  every night while enabled — it used to skip any day that already had a log,
  which meant it stayed silent on exactly the nights you were paying attention
  and so read as broken. And where the ግጻዌ nudge carries the day's reading, this
  one now carries the streak at stake: "የ5 ቀን ጉዞህ እንዳይቋረጥ — የዛሬን መዝግብ" /
  "Keep your 5-day streak going — log today."

## [0.9.9] — 2026-08-13

_versionCode 30 · prayer list, sharing, and the day's psalm on Home_

### Added
- **የጸሎት ዝርዝር — a prayer list.** The people you remember in prayer, each with an
  optional intention, reached from the Home header. Plain rows, local like
  everything else, included in backups (older backup files still restore).
- **Sharing from the ግጻዌ and ስንክሳር readers.** Copy, share as text, or share as a
  rendered image card carrying the passage with its source and date.
- **The day's ቅዳሴ on the widget**, one quiet line under the readings.
- **Choose the streak reminder's time.** A ሰዓት row under the toggle in Settings
  opens the same clock the prayer-mode editor uses; saving re-arms the alarm
  immediately. Default unchanged at 21:30.
- **Two smaller reading sizes** — 13 and 15sp below the old floor of 17. Your
  saved size is unchanged.

### Changed
- **Home names the day's psalm.** የዕለቱ መዝሙረ ዳዊት sits beside Today as two equal
  cards, naming today's portion by the traditional weekday division; Sunday,
  which has no fixed portion, offers the whole Psalter. The library shortcut
  row is gone — መዝሙረ ዳዊት, ዘወትር ጸሎት and ውዳሴ ማርያም live on the Library tab. The
  Today card keeps the count, flame and heatmap; the per-habit dots now live
  only on the Streak screen it opens.

### Fixed
- **Prose no longer renders inside the አርኬ.** The mirror of 0.9.8's fix: after
  an explicit አርኬ line, everything to the end of the entry stayed hymn verse —
  22 lines across the year, including an entire second commemoration on ጥር ፲.
  Lines opening with formulas only prose uses (በዚችም ቀን…, ለእግዚአብሔርም ምስጋና…,
  the month colophon) now return to narrative, and a following ሰላም salutation
  reopens the hymn. All 366 days verified: no prose inside hymns, no hymn left
  in prose, and the 11 days without a hymn confirmed hymn-less in the source.

## [0.9.8] — 2026-08-13

_versionCode 29 · the አርኬ reads as a hymn again_

### Fixed
- **The ስንክሳር's አርኬ ran on as part of the saint's life.** The hymn was only set
  apart when the source wrote the word አርኬ on its own line, and much of the data
  never does — the entry goes straight from the closing benediction into the
  salutation. Those hymns were rendered as one more paragraph of narrative. It
  is worst in the last two months of the year: ነሐሴ labelled 12 of its hymns and
  left 44 unlabelled, and ጳጉሜን labelled none of its 13. A salutation opening
  with ሰላም and carrying the Ge'ez clause stops ፡፡ or ። now begins the hymn and is
  given the heading the source omitted. 815 → 880 entries across the year read
  correctly; ነሐሴ 12 → 53 and ጳጉሜን 0 → 12. Ordinary prose is untouched — the
  clause stops are what distinguish the sung salutation from the plain word ሰላም.

## [0.9.7] — 2026-08-13

_versionCode 28 · the nightly nudge actually arrives_

### Fixed
- **The nightly streak reminder never appeared.** The schedule was never the
  problem — it is armed on app open, on the toggle, and on boot, and the alarm
  fired on time. The notification was dropped by the system: `POST_NOTIFICATIONS`
  is a runtime grant on Android 13+, and the app asked for it in exactly one
  place, the prayer *mode* editor. Switching the reminder on from Settings armed
  an alarm that fired into nothing. This is why it looked selective — a prayer
  alarm rings and shows a full-screen intent without the grant, so only the
  notification-only reminders (the streak nudge and the ግጻዌ nudge) vanished.
  Switching either on now asks for the permission, and a banner above the toggles
  links to the system settings page when notifications are blocked outright —
  which the prompt alone cannot fix, since Android stops offering it after two
  denials.

## [0.9.6] — 2026-08-13

_versionCode 27 · a quieter Home_

### Removed
- **"እንደተነበበ ምልክት አድርግ" (Mark as read), and the prayer progress it fed.** Marking
  each section off by hand turned praying into a checklist. Gone with it: the
  progress bar in the reader, the "Resume where you left off" row on Home, the
  "3 of 12" counts on the hours list, and the ✓ on a finished hour. Streaks are
  unaffected — the "ጨርሰዋል?" prompt after an alarm still records the hour.
- **The attribution line under ውዳሴ ማርያም.** The page carries the prayer and
  nothing else. The digitization is still credited in NOTICE and README.

### Changed
- **The hours list on Home starts collapsed.** The prayer for now is already on
  screen as a card; the full list is something you go looking for.
- **The Today block is smaller** — smaller dots, a tighter heatmap over ten weeks
  instead of fourteen, less space around both.
- **Section headings are legible as headings** — 15sp bold rather than a 13sp
  label, and collapsible headings now match plain ones instead of reading as a
  lesser rank with a chevron.

### Fixed
- **Three light-theme contrast failures.** The unchecked habit dot (2.89:1) and
  the unset bookmark icon (2.29:1) sat under the 3:1 an icon control needs, and
  the current-hour badge's gold-on-gold reached only 4.20:1 against a 4.5:1 bar.
  Now 3.19, 3.33 and 4.57. Dark theme already passed throughout.
- **The ስንክሳር data test.** It asserted 365 days and 1817 entries against data
  that 0.9.1 re-extracted to 366 and 2308, so master and the v0.9.1 tag both
  shipped a failing build. The totals were stale, not the data: 366 is the whole
  fixed-calendar book — twelve months of thirty days plus all six of ጳጉሜን. The
  test now also checks the manifest agrees with each month file, and that a month
  covers its days with no gap and no repeat.

### Internal
- CI no longer runs on every push and pull request; it is manual-only from the
  Actions tab. Releases are unaffected and still gate themselves on content
  validation, the unit tests and lint before building anything.

## [0.9.1] — 2026-08-12

_versionCode 26 · a ስንክሳር for the whole year_

### Added
- **ስንክሳር for the seven months that had none.** ጥር, የካቲት, መጋቢት, ሚያዝያ, ግንቦት, ሰኔ
  and ሐምሌ carried only a day heading and a feast list — over half the year opened
  to nothing to read. They now hold the full daily commemorations — some 650 new
  entries, re-extracted from the `Nexuss0781/synaxarium` dataset by the new
  `tools/extract_sinksar_hf.py`.
- **The closing ጸሎት reads in Amharic.** Tapping the salutation that ends each
  day's ስንክሳር switches between the Ge'ez verses and their Amharic rendering. The
  last two stanzas have no Amharic counterpart and stay in Ge'ez. The choice is a
  reading aid for the moment, not a saved setting.

### Changed
- መስከረም, ጥቅምት, ታኅሣሥ, ነሐሴ and ጳጉሜን now come from the same source as the rest of
  the year, for one consistent voice throughout. Their readings are more
  condensed than in 0.9.0: several saints who had a full entry of their own are
  now commemorated within the day's narrative instead.

### Known issues
- Four months repeat a day from the source data: ጥር 21 repeats ጥር 20, መጋቢት 29
  repeats መጋቢት 28, ሚያዝያ 4 and 6 repeat 3 and 5, and ሐምሌ 21 repeats ሐምሌ 20.
- ሕዳር is untouched: it keeps the original extraction rather than the new source,
  because that source is not the daily synaxarium for this month but a single
  ድርሳነ መስቀል homily repeated across all 30 days. (Corrected in 0.9.6: this entry
  previously implied the homily had shipped. It never did — the bundled ሕዳር is
  and always was the daily synaxarium.)

## [0.9.0] — 2026-08-12

_versionCode 25 · prayer progress, search, quiet hours, and a design system_

### Added
- **Your place in an hour is kept.** Each section in the prayer reader has a
  read toggle, a thin bar under the app bar shows how far through the hour you
  are, and Home offers to resume an hour you started but didn't finish. Marks
  are held against the section itself, so reordering or hiding sections never
  loses them, and the day resets at midnight — an hour is prayed anew each day.
- **Home marks the current hour** with a "now" badge and shows how much of each
  hour you have read.
- **Search understands references.** Typing "መዝሙር 23", "ሉቃስ 10", "Luke 4" or
  just "23" jumps straight to the page. Results are grouped by where they came
  from, in a fixed order, with a filter row showing each source's match count —
  so one prayer match is no longer buried under scripture hits.
- **Restoring a backup shows you what will happen first.** Choosing a file now
  reports when it was made, what it holds, and how much of that is actually new
  on this device, before anything is written. A file from a newer version of the
  app is refused rather than half-applied.
- **Quiet hours** silence reminders overnight without switching them off. The
  alarm still re-arms while silent, so tomorrow's reminder is never lost.
- **Today's ግጻዌ on Home** now names the day's reading and feast, instead of
  being a label with an arrow on it.

### Changed
- **A shared design system.** One set of spacing, corner radii, icon sizes,
  cards, rows, headers and app bars across every screen, so the prayers,
  Psalter, scripture, ግጻዌ, ስንክሳር and ውዳሴ ማርያም read as one application rather
  than as neighbours. Recorded in `docs/DESIGN_SYSTEM.md`.
- **The readers are quieter.** Section titles are properly centred, bookmarking
  sits at the head of a section and marking-as-read at its foot, so nothing
  stands between the title and the first verse. Verse spacing now grows with the
  text instead of collapsing at the largest size, and lines stop widening past a
  comfortable measure on a tablet.
- **The dark theme was designed on its own terms** rather than inverted: a
  warmer, lower-glare ground for night prayer, surfaces that step up gently, and
  highlight colours retuned so they stay distinguishable over green.
- **Motion throughout** — expanding sections, selection, sheets, page
  transitions — kept short and small, and switched off entirely when the system
  animation setting is off.
- **Settings is grouped** into Reading, Prayer and reminders, Your data, and
  More, instead of one long list.
- **Backup and restore failures explain themselves**, including whether your own
  data was affected. They no longer report a bare "failed".
- The gold accent is deeper on the light theme. It carries nearly every small
  label in the app and only reached 3.1:1 on the ivory ground; it now clears
  WCAG AA.

### Fixed
- **Amharic labels across Home, ግጻዌ and search were rendering in the device's
  font**, not the bundled one, and with Latin letter spacing that pulled Ethiopic
  syllables apart. Four undefined text roles were falling through to Material's
  defaults.
- **Dark-theme controls were painted from Material's stock palette** — switches,
  chips, bottom sheets, dropdown menus and the date picker all drew from colours
  the app had never defined, and switches were dark-on-dark.
- The heatmap's year arrows were unlabelled; a screen reader announced two
  anonymous buttons.
- The bookmark remove button was labelled in Amharic even in English.

## [0.8.3] — 2026-08-10

_versionCode 24_

### Changed
- **The opening is quicker** — memento mori now holds for about 1.3 seconds
  instead of 2.2, with a faster fade, and a small spinner sits beneath the
  words. A tap still skips straight through.

## [0.8.2] — 2026-08-10

_versionCode 23 · backup & restore_

### Added
- **Backup and restore** in Settings. Your streak history, bookmarks and
  highlights save to a file through the system file picker and come back on
  restore. Restoring **merges** rather than replaces — a day marked done in
  either the file or on the device stays done — so bringing back an old backup
  can never erase newer progress. Nothing is uploaded anywhere; the app still
  has no internet permission.

## [0.8.1] — 2026-08-10

_versionCode 22 · አጽዋማት_

### Added
- **The fasting calendar**, from a button beside search on Home. It shows what's
  in effect today — a named fast with which day of it you're on, or the ረቡዕ/ዓርብ
  rule — then every fast of the Ethiopian year with its dates and length:
  ጾመ ነነዌ, ዐቢይ ጾም, ጾመ ሐዋርያት, ጾመ ፍልሰታ, ጾመ ነቢያት and ጾመ ገሃድ.

## [0.8.0] — 2026-08-10

_versionCode 21 · search everywhere & reading fixes_

### Added
- **Search now reaches the whole app** — the New Testament, all 1,817 ስንክሳር
  commemorations, and ውዳሴ ማርያም join the prayer hours and Psalter.
- **Copy and share** — a tapped verse can be copied or shared, a ግጻዌ day shares
  its ነግህ and ቅዳሴ readings, and a scripture chapter shares in full. Shared text
  is signed "— ስንቅ".

### Changed
- **A ግጻዌ citation now highlights as one block** instead of a separate box per
  verse, in both the prayer/Psalter reader and the scripture reader.
- **The ስንክሳር closing ጸሎት reads as a coda** — smaller and tightly set, rather
  than claiming a screenful at the end of every day.
- **The About page is tighter**, and the Menbere font is removed.

### Fixed
- Bookmarks open the right section again after you reorder or hide sections in
  an hour; they now follow the section itself rather than its position.
- Finishing the first-run intro no longer re-anchors the navigation graph.

## [0.7.1] — 2026-08-09

_versionCode 20 · memento mori & a roomier Home_

### Added
- **The app opens on _memento mori_** (ሞትን አስብ) — the monastic reminder, held
  for a moment, tap to move on.

### Changed
- **The hours list is collapsible** from its header on Home, with the count
  shown beside it. Expanded by default.
- **The "prayer for now" card is about a third shorter**, and the "Continue"
  strip is gone — so the hours start much higher up the screen.

## [0.7.0] — 2026-08-09

_versionCode 19 · የዕለቱ ግጻዌ widget & typography polish_

### Added
- **የዕለቱ ግጻዌ home-screen widget.** Today's ምስባክ and ወንጌል with the Ethiopian
  date, at a glance; tapping it opens the ግጻዌ screen. It rolls over to the new
  day's readings at midnight, and adds no weight to the app.

### Changed
- **The reading fonts now sit together properly.** The bundled faces are not the
  same size at a given setting — x-heights differ by up to 19% — so pages looked
  bigger, smaller, or unevenly spaced depending on which font was chosen. Every
  face now carries a size correction, and line spacing is applied consistently
  rather than being left to each font's own metrics.
- **Tighter line spacing** on every reading page — prayers, Psalter, scripture,
  ስንክሳር, ውዳሴ ማርያም — so more text fits on screen without crowding it. The አርኬ
  hymn keeps its wider spacing, being verse rather than prose.
- **The font picker is collapsed by default** in Settings, with the current face
  named in the header.

## [0.6.0] — 2026-08-09

_versionCode 18 · choose your reading font_

### Added
- **Reading font setting.** Settings now offers four bundled Ethiopic faces
  beside the Abyssinica SIL default — **Ethiopic Abay Light**, **Bela Bereka**,
  **Zemenay**, and **Menbere**, all from [Font.et](https://www.font.et/) under
  the SIL Open Font License. Each row in the picker is rendered in its own face,
  and the choice applies to every prayer, Psalter, scripture, ስንክሳር, and
  ውዳሴ ማርያም page. Designers are credited in the README and `docs/fonts/`.

### Notes
- The APK grows from 4.3 MB to 6.2 MB for the bundled faces.

## [0.5.2] — 2026-08-08

_versionCode 17 · audit fixes & ዘወትር ጸሎት_

### Added
- **ዘወትር ጸሎት.** The daily opening prayer (ጸሎት ዘዘወትር) — no longer "coming
  soon". It opens from Home and the library as the first section of the
  ውዳሴ ማርያም reader, Amharic with the same Ge'ez toggle, from the same
  credited source.

### Fixed
Five high-severity bugs found by a full feature audit:
- The Bookmarks screen crashed when one group carried two names (hour
  rename, or bookmarks created in both app languages).
- Eight ስንክሳር entries whose አርኬ marker was glued to its verse rendered
  the hymn as numbered prose instead of the red centered verse style.
- Tapping the nightly streak notification could land on Home instead of
  the Streak screen after any prayer alarm re-armed.
- ግጻዌ citations naming chapters that don't exist (e.g. Mark 17) showed
  chapter 1 under the cited title and highlighted the wrong verses.
- Yekatit 3 showed Hamle 29's commemorations — the source carried a
  mislabeled duplicate day; it now honestly shows no entry until a
  trusted text for the day is found.

And a batch of smaller ones, including: downgrade no longer wipes
bookmarks; a psalm bookmarked in the Psalter and inside an hour toggle
independently; the ስንክሳር keeps the source's own list numbering, drops
leftover `<b>` markup, and keys its bookmarks to the Ethiopian date;
scripture citations landing past a chapter's end or on merged verses now
highlight the nearest real verse; the ውዳሴ ማርያም reader scrolls to the
top when switching days; and a failed content load is retried instead of
sticking as an empty screen until restart.

## [0.5.1] — 2026-08-07

_versionCode 16_

### Fixed
- **Synaxarium paragraphs are centered again.** The Ge'ez paragraph numeral sat
  in a left-only gutter that pushed the prose off-center; it now renders inline
  at the head of the paragraph, so the text spans the full width.
- **ውዳሴ ማርያም opens from the Home screen.** The Home library row still showed
  the disabled "coming soon" stub even though the reader shipped in 0.5.0.

## [0.5.0] — 2026-08-07

_versionCode 15 · ውዳሴ ማርያም, synaxarium reader reformat & bookmarkable scripture_

### Added
- **ውዳሴ ማርያም (Wudase Maryam).** The daily Praise of Mary opens from the library
  — no longer "coming soon". One portion per weekday (defaulting to today) plus
  ይወድስዋ መላእክት and አንቀጸ ብርሃን, in Amharic by default with a Ge'ez toggle on top.
  Text from the community dataset credited in-app and in the README.
- **Bookmark scripture.** The scripture-quote entries within the ስንክሳር and the
  library's Bible reader now carry a bookmark toggle; saved passages land in the
  existing Bookmarks screen and reopen where they came from.
- **Closing ጸሎት.** The fixed synaxarium closing prayer is now appended once at
  the end of every day, set apart in its own card, with the holy names in red.

### Changed
- **Synaxarium reader reformatted.** Each commemoration's paragraphs are numbered
  with Ge'ez numerals and given real spacing; the **አርኬ** hymn is set apart as
  centered red italic verse; the editorial emojis that prefixed lines in the
  source (❖ ✍️ 📌 📖 …) are stripped at render time, leaving the Ge'ez untouched.

## [0.4.0] — 2026-08-07

_versionCode 14 · ስንክሳር & daily reminder_

### Added
- **ስንክሳር (Synaxarium).** The full Amharic synaxarium — the daily commemorations
  of saints and events (367 days, 1,822 entries) — opens from a card on the ግጻዌ
  screen and follows the day picker, so any day's synaxarium is a tap away.
- **Daily ግጻዌ reminder.** An optional morning notification with today's ግጻዌ
  reading heading; tapping it opens the day's readings. Toggle it in Settings
  (on by default), scheduled with an exact, doze-exempt alarm.

## [0.3.1] — 2026-08-07

_versionCode 13_

### Added
- Browse the ግጻዌ readings for **any day** — a date picker on the Gitsawe screen
  reloads that day's daily, seasonal, and monthly readings.

### Changed
- The A− / A+ **font-size controls now appear on every reader** (the scripture
  reader included) and stay legible in dark mode — previously they used a deep
  green that vanished on the dark background.

### Fixed
- Opening a psalm from a ግጻዌ reading now **highlights the cited verses** in the
  Psalter, with a stronger, more visible tint on both readers.

## [0.3.0] — 2026-08-07

_versionCode 12 · ግጻዌ, Scriptures library & Bahre Hasab_

### Added
- **ግጻዌ (Gitsawe) — the daily lectionary.** A "የዕለቱ ግጻዌ" card on the home screen
  opens the day's readings — the synaxarium plus the ነግህ and ቅዳሴ offices — with
  every scripture reference tappable through to the text.
- **Daily, seasonal, and monthly offices.** When a date carries more than one
  office, a switcher lets you move between the daily reading, the movable-season
  reading, and the monthly reading.
- **ባሕረ ሓሳብ (Bahre Hasab).** The Ethiopian computus now locates the movable feasts
  and fasts (ጾመ ነነዌ, ዐቢይ ጾም, ትንሣኤ, ዕርገት …), so the correct seasonal readings
  appear during Lent and the Resurrection season.
- **ቤተ መጻሕፍት — a new Library tab** in the bottom navigation, holding the Psalter
  and the full **New Testament** (27 books) with a chapter-by-chapter reader in
  Ge'ez numerals. Gitsawe references open the exact passage and highlight the
  cited verses.

### Fixed
- **Prayer alarms could go silent for days at a time.** The schedule was rebuilt
  only on boot, update, clock change, or a mode edit; an aggressive battery
  manager force-stopping the app left the reminder chain broken until the next
  reboot. Opening the app now re-arms the full schedule, self-healing on launch.
- **The nightly 9:30 PM streak reminder often failed to fire.** It was an inexact,
  doze-deferrable alarm that App-Standby throttling suppressed once the app had
  gone unopened for a day or two. It now uses an exact, doze-exempt alarm.

## [0.2.6] — 2026-07-28

_versionCode 11_

### Added
- Guided tutorial for new users covering reminders, streaks, and the Psalter.
- Settings option to replay the tutorial.

### Changed
- About screen shown in English; recorded CC BY-NC-ND terms for the prayer text.

## [0.2.5] — 2026-07-24

### Added
- Unified search across the prayer hours and the Psalter.
- Nightly streak reminder.
- In-app guidance for granting the notification permission.

## [0.2.3] — 2026-07-24

### Fixed
- Four reminder and content bugs, including alarm deep-linking, crash safety, and
  main-thread I/O.
- Release crash caused by R8 stripping DataStore protobuf fields.

## [0.2.0] — 2026-07-16

### Added
- Full Psalter (መዝሙረ ዳዊት) screen and navigation.
- Share your streak as an image card.

## [0.1.0] — 2026-07-16

_versionCode 1 to 4_

### Note
- **Reconstructed from the commit history**, which this file did not yet cover.
  It gathers 60 commits between 29 May and 16 July 2026, versions 0.1.0 through
  0.1.3. None of them were tagged or published: the first release anyone could
  install was 0.2.2.

### Added
- **The seven hours.** ጸሎተ ነግህ, ሠለስት, ቀትር, ተሰዓት, ሰርክ and ንዋም, with መንፈቀ ሌሊት
  and its three watches and the Veil prayer. Each was assembled from a psalm
  and gospel mapping rather than typed out: `content/hour_mapping.json` and
  `tools/extract_content.py` date from here, and so does the rule that bundled
  JSON is generated and never hand-edited.
- **The reading screen**, with verse numbers set in Ge'ez numerals.
- **The home screen**, listing the hours and opening the one the time suggests.
- **Prayer reminders.** Prayer modes with weekday selection, an alarm that
  rings from a foreground service with a full-screen activity behind it,
  snooze, rescheduling after a reboot or a clock change, and a follow-up
  asking whether the hour was actually prayed.
- **Amharic search that forgives spelling**, treating ሀ/ሐ/ኀ, ሰ/ሠ and ጸ/ፀ as
  equal.
- **Bookmarks, recents, and verse highlighting.**
- **The full Psalter, bundled**, so that any psalm could be added to any hour.
  It had no screen of its own yet; that came with 0.2.0.
- **Hours you shape yourself**: show, hide and reorder the sections of an hour,
  and add a psalm to any of them.
- **Habit tracking**, with streaks and a heatmap laid out on the Ethiopian
  calendar. It had no name of its own yet.
- **A local profile**, a name and a baptismal name, and a greeting on the home
  screen.
- **A first-launch introduction**, settings and about screens, and help for
  getting past a battery manager aggressive enough to silence the alarms.
- **Amharic and English throughout**, with Noto Sans Ethiopic and Abyssinica
  SIL bundled for reading.

### Changed
- **The app became ስንቅ.** It was built as ጸሎት and renamed on 18 June, together
  with the green and gold it still wears. The package id `com.agpeya.app` was
  older than both and was kept then, because changing it would make every phone
  that already had the app treat an update as a different one. (It did change
  later, before the first Play listing existed and while the only installs were
  by hand: the app registers as `com.sinq.app`. The Kotlin still lives under
  `com/agpeya/app`, which nothing outside the source tree ever sees.)
- **The home screen was rebuilt** around a card for the day, with an hour
  switcher inside the reader.
- **The Veil prayer became ሌሊት ፱ ሰዓት** and now ships hidden, for those who do
  not pray it.
- **Search and bookmarks moved** off the bottom bar and into the home header.
