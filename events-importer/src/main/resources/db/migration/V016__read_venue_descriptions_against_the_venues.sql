-- Twenty-one venue descriptions read against the venue itself and found wrong, and one that the
-- seed had corrected without a migration (#1124).
--
-- #986 read the 86 descriptions against the row's own address and found two that quoted a wrong
-- address. This pass read every one against four sources in the issue's order: the venue's own
-- site, its Resident Advisor page, Google's category for it, and Wikipedia where an article exists.
-- Sixty-five stand as written. The rest name a building the venue never occupied, a station that is
-- not the nearest, a genre the house does not play, a sport that left in 2008, a cinema that screens
-- films again, or a year that matches no source.
--
-- **Every replacement is our own prose.** Facts were taken from the sources; no sentence was. The
-- venue's own text is the venue's copyright (#283, #808), and a migration is not the place to start
-- reproducing it.
--
-- **Every UPDATE is guarded on the wording it replaces**, so a row an operator has since edited keeps
-- its value rather than having a fact-checked one overwritten from here. The seeded text and the
-- cluster text are the same for every row but one: Festsaal Kreuzberg's district moved to
-- Treptow-Köpenick in V009 and the seed file's sentence followed, but no migration carried it, so
-- production still reads `in Kreuzberg`. That statement is last, and is the one guarded on the
-- cluster's wording rather than the seed's.
--
-- Unqualified table name, deliberately (ADR-004). Each statement names the source that changed it.

-- The house opened in 1911 and became a revue theatre in 1923; "a 1910 revue house" joined the year
-- the old baths were demolished to the later use.
-- source: the venue's own history page and the German Wikipedia article
UPDATE venue SET description = 'The Friedrichstrasse variety theatre — musicals, comedy, concerts and dance in an entertainment palace of 1911, a revue house since 1923.'
WHERE slug = 'admiralspalast' AND description = 'The Friedrichstrasse variety theatre — musicals, comedy, concerts and dance in a 1910 revue house.';

-- Alt-Stralau 1-2 is the landward end of the peninsula beside Ostkreuz and the Osthafen, on the
-- Spree side. Nothing places the club at a tip or on the Rummelsburger Bucht. The building is left
-- out: the sources call it an industrial hall, a harbour building and a power plant, one each.
-- source: Resident Advisor, visitBerlin and berlin.de, which agree; the venue's own site carries no prose
UPDATE venue SET description = 'A techno and house club on the Spree at the Ostkreuz end of the Stralau peninsula, programming raves and club nights that run from late evening into the next morning.'
WHERE slug = 'club-ost' AND description = 'A techno club at the western tip of the Stralau peninsula on the Rummelsburger Bucht, programming raves and club nights that run from late evening into the next morning.';

-- The cinema subdomain lists a daily film programme and Wikipedia records that screenings resumed
-- after the 2022 sale, so "former cinema" is stale.
-- source: the venue's own cinema programme and the German Wikipedia article
UPDATE venue SET description = 'A Prenzlauer Berg cinema of 1924 on Gleimstraße that still screens films, its halls also run as an event house for readings, book premieres, talks and live podcast recordings.'
WHERE slug = 'colosseum' AND description = 'A former Prenzlauer Berg cinema on Gleimstraße, run as an event house for readings, book premieres, talks and live podcast recordings.';

-- No source describes railway arches. The RAW-Gelände is a former repair works, and the club is
-- described as a two-floor industrial building, so the unverifiable "arches" goes. The row's
-- spelling joins the other RAW rows.
-- source: the venue's own site and berlin.de
UPDATE venue SET description = 'A techno club on the RAW-Gelände on Revaler Straße, running raves, DJ nights and an open-air garden.'
WHERE slug = 'der-weisse-hase' AND description = 'A techno club in the RAW-Gelaende arches on Revaler Strasse, running raves, DJ nights and an open-air garden.';

-- Every source says house, not techno. The weekly cadence and the former nursery are on no source
-- read, so both leave the sentence until the maintainer confirms them.
-- source: the venue's own line-up, Resident Advisor and berlin.de, which all say house
UPDATE venue SET description = 'A house club by the Westhafen in Wedding with a large garden, running long Saturday-into-Sunday sessions through the season.'
WHERE slug = 'heidegluhen' AND description = 'An open-air techno party in a former nursery off Seestraße, running one long Saturday-into-Sunday session a week through the season.';

-- Neither calls the building a ballroom. The Neue Welt was a beer palace, then a variety theatre
-- and a sports palace.
-- source: the venue's own history page and both Wikipedia articles
UPDATE venue SET description = 'A 1,600-capacity concert hall in the former Neue Welt beer palace on the Hasenheide in Neukölln, hosting touring rock, metal, hip hop, pop and electronic acts.'
WHERE slug = 'huxleys-neue-welt' AND description = 'A 1,600-capacity concert hall in a former ballroom on the Hasenheide, hosting touring rock, metal, hip hop, pop and electronic acts.';

-- The site places the house in the Wrangelkiez at the Schlesische Straße corner, blocks from the
-- river. No source puts it on the Spree.
-- source: the venue's own history page
UPDATE venue SET description = 'A former 1950s cinema turned concert venue in Kreuzberg''s Wrangelkiez, staging touring indie, rock and electronic acts as well as club nights.'
WHERE slug = 'lido' AND description = 'A former 1950s cinema turned concert venue on the Spree in Kreuzberg, staging touring indie, rock and electronic acts as well as club nights.';

-- About a hundred concerts a year plus readings and discussions is not "a small programme"; the
-- genres and the non-music formats are now named, and the adjectives go.
-- source: the venue's own site, the Clubcommission profile and miz.org
UPDATE venue SET description = 'A collectively run bar and events space in Friedrichshain, programming punk, rock and hip hop concerts alongside readings, discussions and DJ nights.'
WHERE slug = 'loge' AND description = 'An intimate, collectively run bar and events space in Friedrichshain, with a small programme of concerts, DJ nights and cultural gatherings.';

-- The site names disco as its core with house and hip hop nights. It never mentions a 90s/2000s or
-- pop programme.
-- source: the venue's own parties page
UPDATE venue SET description = 'A party club off the Ku''damm, open every night since 2007 with a DJ programme built around disco, house and hip hop.'
WHERE slug = 'maxxim' AND description = 'A party club off the Ku''damm, open nightly with a 90s/2000s, pop and house DJ programme.';

-- The regular tenants are Füchse Berlin and BR Volleys. Alba's basketball left in 2008.
-- source: the venue's own site
UPDATE venue SET description = 'A multi-purpose arena at Falkplatz in Prenzlauer Berg, hosting arena concerts alongside the handball and volleyball fixtures that are not imported.'
WHERE slug = 'max-schmeling-halle' AND description = 'A multi-purpose arena at Falkplatz in Prenzlauer Berg, hosting arena concerts alongside the handball and basketball fixtures that are not imported.';

-- No source calls it underground or names rock and metal. The venue opened in September 2025 inside
-- the Metropol with concerts, readings and house and techno club nights, none of which the old
-- sentence said.
-- source: the venue's own site and the opening coverage in Groove, radioeins and visitBerlin
UPDATE venue SET description = 'A 250-capacity concert room and club opened in 2025 in the former carriage passage of the Metropol at Nollendorfplatz, with punk, rap and indie concerts, readings and house and techno nights.'
WHERE slug = 'mikropol' AND description = 'A small underground club in Schöneberg with a varied programme of concerts and club nights across rock, metal, electronic and alternative styles.';

-- The site calls it a recording studio with room for an audience plus a project space, not a
-- concert room. Neither "non-Western music" nor "recorded for the label" appears in any source.
-- source: the label's own site, /raum and /events
UPDATE venue SET description = 'The Morphine Records label''s studio and project room on the first floor of a Kreuzberg backyard, staging experimental, improvised and electronic concerts, many played as live recording sessions.'
WHERE slug = 'morphine-raum' AND description = 'The backyard concert room of the Morphine Records label in Kreuzberg, staging experimental, improvised and non-Western music, most of it recorded live for the label.';

-- The site calls itself cinema, art, concerts and pub and never club. The old sentence dropped the
-- cinema, which the venue and Google both put first.
-- source: the venue's own site and the German Wikipedia article on its predecessor
UPDATE venue SET description = 'An arthouse cinema, pub and concert room next door to Renate on the Stralau peninsula in Friedrichshain, the successor of the Zukunft am Ostkreuz, with film, live music, theatre and a summer open-air cinema.'
WHERE slug = 'neue-zukunft' AND description = 'A cultural venue and club on the Stralau peninsula by the Spree in Friedrichshain, blending concerts, club nights and arts events.';

-- The club left the Eisenbahn-Markthalle in 2013 for the old post office at Skalitzer Straße 85-86,
-- which is the stored address. "Below the Markthalle" described the old venue.
-- source: berlin.de and visitBerlin; the venue's own site confirms the address
UPDATE venue SET description = 'A club in the basement of a former post office on Skalitzer Straße near Schlesisches Tor in Kreuzberg, presenting concerts and DJ nights across soul, funk, indie, hip-hop and electronic music.'
WHERE slug = 'privatclub' AND description = 'A club below the Markthalle near Schlesisches Tor in Kreuzberg, presenting concerts and DJ nights across soul, funk, indie, hip-hop and electronic music.';

-- The site names the building: the boiler house of the Königstadt brewery on a listed courtyard,
-- since 2005. "In a courtyard" was true and said less.
-- source: the venue's own site
UPDATE venue SET description = 'A rock''n''roll club in the former boiler house of the old Königstadt brewery in Prenzlauer Berg, specialising in rockabilly, psychobilly, punk and garage live shows and parties.'
WHERE slug = 'roadrunner-s-paradise' AND description = 'A rock''n''roll club in a courtyard in Prenzlauer Berg, specialising in rockabilly, psychobilly, punk and garage live shows and parties.';

-- The hall stands opposite the arena across the square, not next door.
-- source: berlin.de and both Wikipedia articles
UPDATE venue SET description = 'Mid-size concert hall across Uber Platz from the Uber Arena, programming touring bands, comedy and staged shows.'
WHERE slug = 'uber-eats-music-hall' AND description = 'Mid-size concert hall next door to the Uber Arena, programming touring bands, comedy and staged shows.';

-- The site describes several floors plus a separate hall opened in March 2023, so "two-room" was
-- wrong.
-- source: the venue's own site
UPDATE venue SET description = 'A multi-floor techno and bass club in a Lichtenberg backyard, open since 2015, programming drum & bass, hardtechno and trance nights across the VOID Club floors and the VOID Hall added in 2023.'
WHERE slug = 'void-club' AND description = 'A two-room techno club in a Lichtenberg backyard, programming drum & bass, hardtechno, trance and bass nights across the VOID Club and VOID Hall floors.';

-- Wiener Straße 20 is at the Görlitzer Bahnhof end of the street, about 300 m from that station.
-- The venue's directions name both stations; every listing names Görlitzer Bahnhof as the closer
-- one.
-- source: the venue's own directions page and the stored address
UPDATE venue SET description = 'A rock''n''roll bar and live club near Görlitzer Bahnhof in Kreuzberg, hosting raucous concerts and DJ nights spanning rockabilly, punk, garage and surf.'
WHERE slug = 'wild-at-heart' AND description = 'A rock''n''roll bar and live club near Schlesisches Tor in Kreuzberg, hosting raucous concerts and DJ nights spanning rockabilly, punk, garage and surf.';

-- Both date the crematorium's opening to 1912; it was built 1909 to 1910 and cremation became legal
-- in Prussia in 1911. "1911" matched neither date.
-- source: the venue's own history page and the German Wikipedia article
UPDATE venue SET description = 'A cultural quarter in a 1912 crematorium in Wedding, programming experimental concerts, exhibitions, film and talks across its Kuppelhalle, Betonhalle and ateliers.'
WHERE slug = 'silent-green' AND description = 'A cultural quarter in a 1911 crematorium in Wedding, programming experimental concerts, exhibitions, film and talks across its Kuppelhalle, Betonhalle and ateliers.';

-- Spelling. The row's name field already carries the umlaut.
-- source: the venue's own name
UPDATE venue SET description = 'A Neukölln Saal and Studio venue in a historic ballroom, programming concerts, theatre, comedy, readings and its own productions.'
WHERE slug = 'heimathafen-neukolln' AND description = 'A Neukolln Saal and Studio venue in a historic ballroom, programming concerts, theatre, comedy, readings and its own productions.';

-- Spelling.
-- source: the station's name
UPDATE venue SET description = 'A small, long-running techno club in the S-Bahn arches at Jannowitzbrücke, open Thursday to Saturday with house and techno DJ nights and door-only entry.'
WHERE slug = 'golden-gate' AND description = 'A small, long-running techno club in the S-Bahn arches at Jannowitzbrucke, open Thursday to Saturday with house and techno DJ nights and door-only entry.';

-- Festsaal Kreuzberg kept its name when it moved to Am Flutgraben 2, and V009 moved the row's
-- district to Treptow-Köpenick. The seed file's sentence was corrected in the same commit, but the
-- migration only touched the district, so every database seeded before that commit still says the
-- hall is in Kreuzberg. The guard is the cluster's text; a fresh seed already carries the corrected
-- sentence and is a no-op here.
-- source: V009 and the venue's own site, which gives Am Flutgraben 2, 12435 Berlin
UPDATE venue SET description = 'A concert hall and event space by the Flutgraben on the Spree in Treptow, with a wide-ranging programme of concerts, club nights and cultural events.'
WHERE slug = 'festsaal-kreuzberg' AND description = 'A concert hall and event space by the Flutgraben on the Spree in Kreuzberg, with a wide-ranging programme of concerts, club nights and cultural events.';
