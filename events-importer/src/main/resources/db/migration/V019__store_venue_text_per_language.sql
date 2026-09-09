-- The German half of every venue description, and the language of both (#1210).
--
-- Columns rather than a per-language table, for the reason V017 gives for `event`: the site has two
-- locales, so a venue holds one description per language and one row keeps the detail read a single
-- query. A third locale is the moment to move to rows, and these columns migrate cleanly.
--
-- **No origin column here, unlike `event`.** Both texts are our own prose. The English was written
-- by hand and read against each venue in #1124; the German is the same text in the other language,
-- written the same way. Nothing is machine-translated, so there is nothing to disclose and nothing
-- that goes stale when a source changes. ADR-026 and ADR-027 govern third-party event text and do
-- not reach this table: a venue description has no author to ask.
--
-- **Every UPDATE is guarded on the English wording it belongs to**, as V016 was, so a row an
-- operator has edited since keeps its value rather than gaining a German text for a sentence that
-- no longer stands. The 86 English descriptions in `http/importer/dev-seed.http` were byte-identical
-- to both clusters on 2026-09-08, so every guard below is expected to match.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).
ALTER TABLE venue
    ADD COLUMN description_language     TEXT,
    ADD COLUMN description_alt          TEXT,
    ADD COLUMN description_alt_language TEXT;

-- The same shape as `event`: a language is one of the two locales, and the alt text is
-- all-or-nothing, because a text with no language cannot be served and a language with no text is a
-- stale marker.
ALTER TABLE venue
    ADD CONSTRAINT venue_description_language_valid
        CHECK (description_language IS NULL OR description_language IN ('de', 'en')),
    ADD CONSTRAINT venue_description_alt_language_valid
        CHECK (description_alt_language IS NULL OR description_alt_language IN ('de', 'en')),
    ADD CONSTRAINT venue_description_alt_complete
        CHECK (
            (description_alt IS NULL AND description_alt_language IS NULL)
            OR (description_alt IS NOT NULL AND description_alt_language IS NOT NULL)
        );

-- Every description this table holds is English. A row without one stays unmarked.
UPDATE venue SET description_language = 'en' WHERE description IS NOT NULL;

-- Admiralspalast
UPDATE venue SET description_alt = 'Das Varietétheater in der Friedrichstraße — Musicals, Comedy, Konzerte und Tanz in einem Vergnügungspalast von 1911, seit 1923 ein Revuehaus.', description_alt_language = 'de'
WHERE slug = 'admiralspalast' AND description = 'The Friedrichstrasse variety theatre — musicals, comedy, concerts and dance in an entertainment palace of 1911, a revue house since 1923.';

-- ÆDEN
UPDATE venue SET description_alt = 'Ein Technoclub an der Spree in Kreuzberg mit zwei Floors und Garten, mit House-, Techno- und Trance-Nächten neben queeren und kollektiv organisierten Partys.', description_alt_language = 'de'
WHERE slug = 'aeden' AND description = 'A techno club on the Spree in Kreuzberg with two floors and a garden, programming house, techno and trance nights alongside queer and collective-run parties.';

-- Alte Kantine
UPDATE venue SET description_alt = 'Ein Club und Konzertraum in der Kulturbrauerei in Prenzlauer Berg, mit Livekonzerten, Clubnächten und Mottopartys quer durch viele Stile.', description_alt_language = 'de'
WHERE slug = 'alte-kantine' AND description = 'A club and concert space in the Kulturbrauerei in Prenzlauer Berg, with a programme spanning live gigs, club nights and themed parties across many styles.';

-- AMT
UPDATE venue SET description_alt = 'Ein Technoclub in den S-Bahn-Bögen am Alexanderplatz, mit späten elektronischen Nächten rund um Techno und House.', description_alt_language = 'de'
WHERE slug = 'amt' AND description = 'A techno club tucked into the S-Bahn arches near Alexanderplatz, running late-night electronic nights focused on techno and house.';

-- Arcanoa
UPDATE venue SET description_alt = 'Eine winzige Kreuzberger Bar am Platz der Luftbrücke, seit 1988 in Betrieb und selbst erklärte Entdeckerbar für Musiker: freitags und samstags unabhängige Livekonzerte, in der übrigen Woche offene Bühnen und Jamsessions.', description_alt_language = 'de'
WHERE slug = 'arcanoa' AND description = 'A tiny Kreuzberg bar near Platz der Luftbrücke, running since 1988 and billing itself as the discovery bar for musicians: independent live acts on Fridays and Saturdays plus open stages and jam sessions the rest of the week.';

-- arkaoda
UPDATE venue SET description_alt = 'Der Berliner Ableger von Bar und Club aus Istanbul, am Karl-Marx-Platz in Neukölln. Ein kleiner Kellerraum mit experimentellen und elektronischen Konzerten, dazu DJ-Nächte, Label-Showcases und Plattenmärkte.', description_alt_language = 'de'
WHERE slug = 'arkaoda' AND description = 'The Berlin outpost of the Istanbul bar and club, on Karl-Marx-Platz in Neukölln. A small basement room programming experimental and electronic concerts alongside DJ nights, label showcases and vinyl markets.';

-- Astra Kulturhaus
UPDATE venue SET description_alt = 'Einer der größeren Konzertorte Berlins, in einer umgebauten Halle auf dem RAW-Gelände in Friedrichshain. Hier spielen Rock-, Pop-, Indie- und Elektronik-Acts auf Tour, dazu kommen Clubnächte.', description_alt_language = 'de'
WHERE slug = 'astra-kulturhaus' AND description = 'One of Berlin''s larger concert venues, set in a converted hall on the RAW-Gelände in Friedrichshain. It hosts touring rock, pop, indie and electronic acts alongside club nights.';

-- Badehaus
UPDATE venue SET description_alt = 'Ein intimer Livemusik-Ort und Club auf dem RAW-Gelände in Friedrichshain, mit Konzerten, Singer-Songwriter-Sessions und vielseitigen DJ-Nächten.', description_alt_language = 'de'
WHERE slug = 'badehaus' AND description = 'An intimate live-music venue and club on the RAW-Gelände in Friedrichshain, mixing concerts, singer-songwriter sessions and eclectic DJ nights.';

-- Bar jeder Vernunft
UPDATE venue SET description_alt = 'Ein Spiegelzelt von 1912 in Wilmersdorf mit Kabarett, Chanson, Musicalrevuen und Varieté an Tischen bei Kerzenlicht.', description_alt_language = 'de'
WHERE slug = 'bar-jeder-vernunft' AND description = 'A 1912 mirror tent (Spiegelzelt) in Wilmersdorf staging cabaret, chanson, musical revues and variety shows around candlelit café tables.';

-- Berghain / Panorama Bar
UPDATE venue SET description_alt = 'Berlins bekannteste Techno-Institution, in einem früheren Heizkraftwerk am Ostbahnhof. Auf dem Hauptfloor und in der Panorama Bar darüber laufen Wochenendpartys im Marathonformat, vor allem Techno und House.', description_alt_language = 'de'
WHERE slug = 'berghain-panorama-bar' AND description = 'Berlin''s most renowned techno institution, housed in a former power plant near Ostbahnhof. The main floor and the upstairs Panorama Bar host marathon weekend parties centred on techno and house.';

-- Bi Nuu
UPDATE venue SET description_alt = 'Ein Livemusik-Club unter dem U-Bahn-Viadukt am Schlesischen Tor in Kreuzberg, mit Konzerten und Clubnächten zwischen Indie, Rock, Hip-Hop und Elektronik.', description_alt_language = 'de'
WHERE slug = 'bi-nuu' AND description = 'A live-music club beneath the U-Bahn viaduct at Schlesisches Tor in Kreuzberg, presenting concerts and club nights across indie, rock, hip-hop and electronic styles.';

-- Cassiopeia
UPDATE venue SET description_alt = 'Ein Club mit mehreren Räumen und ein Kulturort auf dem RAW-Gelände in Friedrichshain, von Reggae, Hip-Hop und Drum & Bass bis zu Indie- und Elektronik-Nächten.', description_alt_language = 'de'
WHERE slug = 'cassiopeia' AND description = 'A multi-room club and cultural venue on the RAW-Gelände in Friedrichshain, covering everything from reggae, hip-hop and drum & bass to indie and electronic nights.';

-- Clash
UPDATE venue SET description_alt = 'Eine Punk-, Ska- und Rock''n''Roll-Bar mit Konzerten nahe dem Mehringdamm in Kreuzberg, mit lauten Shows ohne Schnickschnack und DJ-Nächten.', description_alt_language = 'de'
WHERE slug = 'clash' AND description = 'A punk, ska and rock''n''roll bar and concert spot near Mehringdamm in Kreuzberg, hosting loud, no-frills live shows and DJ nights.';

-- Club der Visionäre
UPDATE venue SET description_alt = 'Eine winzige Open-Air-Hütte mit Terrasse am Flutgraben, seit vielen Jahren eine Sommerinstitution für House und Minimal Techno, vom Nachmittag bis in den nächsten Morgen.', description_alt_language = 'de'
WHERE slug = 'club-der-visionare' AND description = 'A tiny open-air shack and terrace on the Flutgraben canal, a long-running summer institution for house and minimal techno that runs from the afternoon into the next morning.';

-- Club OST
UPDATE venue SET description_alt = 'Ein Techno- und House-Club an der Spree am Ostkreuz-Ende der Halbinsel Stralau, mit Raves und Clubnächten vom späten Abend bis in den nächsten Morgen.', description_alt_language = 'de'
WHERE slug = 'club-ost' AND description = 'A techno and house club on the Spree at the Ostkreuz end of the Stralau peninsula, programming raves and club nights that run from late evening into the next morning.';

-- Colosseum
UPDATE venue SET description_alt = 'Ein Kino von 1924 in der Gleimstraße in Prenzlauer Berg, das noch Filme zeigt. Seine Säle laufen zugleich als Eventhaus für Lesungen, Buchpremieren, Talks und Live-Podcasts.', description_alt_language = 'de'
WHERE slug = 'colosseum' AND description = 'A Prenzlauer Berg cinema of 1924 on Gleimstraße that still screens films, its halls also run as an event house for readings, book premieres, talks and live podcast recordings.';

-- Columbiahalle
UPDATE venue SET description_alt = 'Ein Konzertsaal für 3.500 Leute am Tempelhofer Feld, direkt neben dem Columbia Theater, mit Rock, Metal, Hip-Hop und Pop auf Tour.', description_alt_language = 'de'
WHERE slug = 'columbiahalle' AND description = 'A 3,500-capacity concert hall at Tempelhofer Feld, next door to the Columbia Theater, hosting touring rock, metal, hip hop and pop acts.';

-- Columbia Theater
UPDATE venue SET description_alt = 'Ein mittelgroßer Konzertsaal am Tempelhofer Feld, direkt neben der Columbiahalle, mit Rock, Metal, Hip-Hop und Pop auf Tour.', description_alt_language = 'de'
WHERE slug = 'columbia-theater' AND description = 'A mid-sized concert hall at Tempelhofer Feld, next door to the Columbiahalle, programming touring rock, metal, hip hop and pop acts.';

-- Cosmic Comedy Club
UPDATE venue SET description_alt = 'Einer der ältesten englischsprachigen Stand-up-Clubs Berlins, mit Showcase- und Open-Mic-Abenden sowie Comedy-Specials auf Tour.', description_alt_language = 'de'
WHERE slug = 'cosmic-comedy-club' AND description = 'One of Berlin''s longest-running English-language stand-up clubs, running showcase and open-mic nights alongside touring comedy specials.';

-- Crack Bellmer
UPDATE venue SET description_alt = 'Ein Mikroclub und eine Tanzbar auf dem RAW-Gelände, mit House- und Techno-Nächten neben Dragshows, Swing-Partys und Livejazz.', description_alt_language = 'de'
WHERE slug = 'crack-bellmer' AND description = 'A microclub and dance bar on the RAW-Gelände, programming house and techno nights alongside drag shows, swing dance parties and live jazz.';

-- Der Weiße Hase
UPDATE venue SET description_alt = 'Ein Technoclub auf dem RAW-Gelände an der Revaler Straße, mit Raves, DJ-Nächten und einem Open-Air-Garten.', description_alt_language = 'de'
WHERE slug = 'der-weisse-hase' AND description = 'A techno club on the RAW-Gelände on Revaler Straße, running raves, DJ nights and an open-air garden.';

-- Duncker Club
UPDATE venue SET description_alt = 'Ein langjähriger Goth-, Wave- und Indieclub in einem historischen Gebäude in Prenzlauer Berg, bekannt vor allem für seine dunklen Alternative-DJ-Nächte und gelegentliche Konzerte.', description_alt_language = 'de'
WHERE slug = 'duncker-club' AND description = 'A long-running goth, wave and indie club in a historic building in Prenzlauer Berg, best known for its dark-alternative DJ nights and occasional live concerts.';

-- Eschschloraque Rümschrümp
UPDATE venue SET description_alt = 'Ein Künstlerclub und eine Bar im Hof des Haus Schwarzenberg an der Rosenthaler Straße, täglich ab 14 Uhr geöffnet, abends mit DJ-Nächten, Livesets und Varieté.', description_alt_language = 'de'
WHERE slug = 'eschschloraque-rumschrump' AND description = 'A Künstlerclub and bar in the Haus Schwarzenberg courtyard off Rosenthaler Straße, open daily from 14:00, with an evening programme of DJ nights, live sets and variety shows.';

-- Festsaal Kreuzberg
UPDATE venue SET description_alt = 'Ein Konzertsaal und Veranstaltungsort am Flutgraben an der Spree in Treptow, mit einem weiten Programm aus Konzerten, Clubnächten und Kulturveranstaltungen.', description_alt_language = 'de'
WHERE slug = 'festsaal-kreuzberg' AND description = 'A concert hall and event space by the Flutgraben on the Spree in Treptow, with a wide-ranging programme of concerts, club nights and cultural events.';

-- Frannz Club
UPDATE venue SET description_alt = 'Ein Club und Konzertort in der Kulturbrauerei in Prenzlauer Berg, mit Livekonzerten und Partynächten zwischen Pop, Indie, Hip-Hop und Elektronik.', description_alt_language = 'de'
WHERE slug = 'frannz-club' AND description = 'A club and concert venue in the Kulturbrauerei in Prenzlauer Berg, presenting live gigs and party nights across pop, indie, hip-hop and electronic music.';

-- Gärten der Welt
UPDATE venue SET description_alt = 'Ein Marzahner Landschaftspark mit internationalen Themengärten, dessen Open-Air-Arena Konzerte, Parkfestivals und Freiluftkino zeigt.', description_alt_language = 'de'
WHERE slug = 'garten-der-welt' AND description = 'Marzahn landscape park of international themed gardens, whose open-air Arena stages concerts, park festivals and open-air cinema.';

-- gART.n
UPDATE venue SET description_alt = 'Ein Open-Air-Gartenclub an der Rummelsburger Bucht, mit Techno- und House-Partys am Tag, samstags und sonntags durch den Sommer.', description_alt_language = 'de'
WHERE slug = 'gart-n' AND description = 'An open-air garden club on the Rummelsburger Bucht, running daytime techno and house parties on Saturdays and Sundays through the summer.';

-- Golden Gate
UPDATE venue SET description_alt = 'Ein kleiner, alteingesessener Technoclub in den S-Bahn-Bögen an der Jannowitzbrücke, donnerstags bis samstags geöffnet, mit House- und Techno-Nächten und Eintritt nur an der Tür.', description_alt_language = 'de'
WHERE slug = 'golden-gate' AND description = 'A small, long-running techno club in the S-Bahn arches at Jannowitzbrücke, open Thursday to Saturday with house and techno DJ nights and door-only entry.';

-- Gretchen
UPDATE venue SET description_alt = 'Ein Club in den Gewölbeställen einer Kaserne aus dem 19. Jahrhundert in Kreuzberg, bekannt für bassbetonte elektronische Nächte — Drum & Bass, House, Techno und Hip-Hop — dazu Livekonzerte.', description_alt_language = 'de'
WHERE slug = 'gretchen' AND description = 'A club set in the vaulted former stables of a 19th-century barracks in Kreuzberg, known for bass-driven electronic nights — drum & bass, house, techno and hip-hop — plus live shows.';

-- Havanna
UPDATE venue SET description_alt = 'Ein großer Latin-Tanzclub in Schöneberg über mehrere Etagen, jede Woche mit denselben Residentnächten — Salsa, Merengue und Bachata neben Reggaeton, Hip-Hop und Charts — dazu Tanzkurse vor der Party.', description_alt_language = 'de'
WHERE slug = 'havanna' AND description = 'A large Latin dance club in Schöneberg spread over several floors, running the same resident nights every week — salsa, merengue and bachata alongside reggaeton, hip hop and chart floors — plus dance lessons before the party.';

-- Heideglühen
UPDATE venue SET description_alt = 'Ein House-Club am Westhafen in Wedding mit großem Garten, mit langen Sessions von Samstag bis Sonntag durch die Saison.', description_alt_language = 'de'
WHERE slug = 'heidegluhen' AND description = 'A house club by the Westhafen in Wedding with a large garden, running long Saturday-into-Sunday sessions through the season.';

-- Heimathafen Neukölln
UPDATE venue SET description_alt = 'Ein Neuköllner Veranstaltungsort mit Saal und Studio in einem historischen Ballhaus, mit Konzerten, Theater, Comedy, Lesungen und eigenen Produktionen.', description_alt_language = 'de'
WHERE slug = 'heimathafen-neukolln' AND description = 'A Neukölln Saal and Studio venue in a historic ballroom, programming concerts, theatre, comedy, readings and its own productions.';

-- Hole 44
UPDATE venue SET description_alt = 'Ein Konzertsaal in Neukölln für Bands auf Tour, mit einem Programm aus Rock, Metal, Punk und Alternative.', description_alt_language = 'de'
WHERE slug = 'hole-44' AND description = 'A concert hall in Neukölln geared towards touring bands, with a programme leaning on rock, metal, punk and alternative live acts.';

-- Humboldthain Club
UPDATE venue SET description_alt = 'Ein Technoclub in Wedding mit Innenfloor und großem Garten, mit kollektiv gebuchten House-, Techno- und Trance-Nächten.', description_alt_language = 'de'
WHERE slug = 'humboldthain-club' AND description = 'A techno club in Wedding with an indoor floor and a large garden, running collective-booked house, techno and trance nights.';

-- Huxleys Neue Welt
UPDATE venue SET description_alt = 'Ein Konzertsaal für 1.600 Leute im früheren Bierpalast Neue Welt an der Hasenheide in Neukölln, mit Rock, Metal, Hip-Hop, Pop und Elektronik auf Tour.', description_alt_language = 'de'
WHERE slug = 'huxleys-neue-welt' AND description = 'A 1,600-capacity concert hall in the former Neue Welt beer palace on the Hasenheide in Neukölln, hosting touring rock, metal, hip hop, pop and electronic acts.';

-- Junction Bar
UPDATE venue SET description_alt = 'Eine Livemusik- und DJ-Bar im Keller nahe dem Mehringdamm in Kreuzberg, mit Konzerten und DJ-Sets an jedem Abend, von Jazz und Soul über Funk bis Rock und Pop.', description_alt_language = 'de'
WHERE slug = 'junction-bar' AND description = 'A basement live-music and DJ bar near Mehringdamm in Kreuzberg, offering nightly concerts and DJ sets across jazz, soul, funk, rock and pop.';

-- Kantine am Berghain
UPDATE venue SET description_alt = 'Der Konzertsaal neben dem Berghain am Ostbahnhof in Friedrichshain, mit Livekonzerten quer durch viele Stile, getrennt von den Technofloors des Clubs.', description_alt_language = 'de'
WHERE slug = 'kantine-am-berghain' AND description = 'The concert hall adjoining Berghain near Ostbahnhof in Friedrichshain, hosting live shows across a broad musical range, separate from the club''s techno floors.';

-- Kater
UPDATE venue SET description_alt = 'Ein Technoclub mit Garten an der Spree am Holzmarkt, früher Kater Blau, mit mehreren Floors und langen Partys über das Wochenende.', description_alt_language = 'de'
WHERE slug = 'kater' AND description = 'A techno club and garden on the Spree at Holzmarkt, formerly Kater Blau, with several floors and long weekend-spanning parties.';

-- Klunkerkranich
UPDATE venue SET description_alt = 'Der Dachgarten über den Neukölln Arcaden: Bar, Bühne und Club mit einem Programm an jedem Abend.', description_alt_language = 'de'
WHERE slug = 'klunkerkranich' AND description = 'The rooftop culture garden above the Neukoelln Arcaden: a bar, stage and club running a nightly programme.';

-- Kulturhaus Insel Berlin
UPDATE venue SET description_alt = 'Das Konzerthaus auf der Insel der Jugend, einer Spreeinsel im Treptower Park, mit Saal, Sommergarten und einer kostenlosen Matineereihe am Sonntag.', description_alt_language = 'de'
WHERE slug = 'kulturhaus-insel-berlin' AND description = 'The concert house on the Insel der Jugend, a Spree island in Treptower Park, with a hall, a summer garden and a free Sunday matinee series.';

-- Kulturhaus Peter Edel
UPDATE venue SET description_alt = 'Das Bildungs- und Kulturzentrum in Weißensee, mit Konzerten, Comedy, Theater, Lesungen, Kinderprogramm und Tanztees.', description_alt_language = 'de'
WHERE slug = 'kulturhaus-peter-edel' AND description = 'The Bildungs- und Kulturzentrum in Weissensee, programming concerts, comedy, theatre, readings, children''s shows and dance teas.';

-- LARK
UPDATE venue SET description_alt = 'Ein Livemusik-Club in den Bahnbögen an der Holzmarktstraße, mit Indie-, Folk-, Pop- und Singer-Songwriter-Konzerten.', description_alt_language = 'de'
WHERE slug = 'lark' AND description = 'A live-music club in the railway arches on Holzmarktstrasse, programming indie, folk, pop and singer-songwriter shows.';

-- Lido
UPDATE venue SET description_alt = 'Ein früheres Kino der 1950er, heute Konzertort im Kreuzberger Wrangelkiez, mit Indie-, Rock- und Elektronik-Acts auf Tour sowie Clubnächten.', description_alt_language = 'de'
WHERE slug = 'lido' AND description = 'A former 1950s cinema turned concert venue in Kreuzberg''s Wrangelkiez, staging touring indie, rock and electronic acts as well as club nights.';

-- Loge
UPDATE venue SET description_alt = 'Eine kollektiv geführte Bar und ein Veranstaltungsort in Friedrichshain, mit Punk-, Rock- und Hip-Hop-Konzerten neben Lesungen, Diskussionen und DJ-Nächten.', description_alt_language = 'de'
WHERE slug = 'loge' AND description = 'A collectively run bar and events space in Friedrichshain, programming punk, rock and hip hop concerts alongside readings, discussions and DJ nights.';

-- MAAYA
UPDATE venue SET description_alt = 'Ein afrodiasporischer Kulturort auf dem RAW-Gelände rund um ein beheiztes Freibecken, mit Galerie, Garten, Hofmarkt und einem Programm aus Clubnächten, Konzerten und Workshops.', description_alt_language = 'de'
WHERE slug = 'maaya' AND description = 'An Afro-diasporic cultural venue on the RAW-Gelände built around a heated open-air pool, combining a gallery, garden, backyard market and a programme of club nights, concerts and workshops.';

-- Madame Claude
UPDATE venue SET description_alt = 'Eine gemütliche Bar mit kopfüber eingerichtetem Raum und Livebühne nahe dem Schlesischen Tor in Kreuzberg, mit Konzerten bei freiem Eintritt und DJ-Nächten mit experimentellem, indielastigem Einschlag.', description_alt_language = 'de'
WHERE slug = 'madame-claude' AND description = 'A cosy, upside-down-themed bar and live venue near Schlesisches Tor in Kreuzberg, hosting free-entry concerts and DJ nights with an experimental, indie bent.';

-- Matrix
UPDATE venue SET description_alt = 'Ein großer Mainstreamclub über mehrere Floors in den Bahnbögen an der Warschauer Straße, an jedem Abend geöffnet, mit Resident-DJs zwischen Hip-Hop, House, Afrobeats, Reggaeton und Charts.', description_alt_language = 'de'
WHERE slug = 'matrix' AND description = 'A large multi-floor mainstream club in the railway arches at Warschauer Straße, open every night of the week with resident DJs playing hip hop, house, afrobeats, reggaeton and chart music.';

-- Max-Schmeling-Halle
UPDATE venue SET description_alt = 'Eine Mehrzweckarena am Falkplatz in Prenzlauer Berg, mit Arenakonzerten neben den Handball- und Volleyballspielen, die wir nicht importieren.', description_alt_language = 'de'
WHERE slug = 'max-schmeling-halle' AND description = 'A multi-purpose arena at Falkplatz in Prenzlauer Berg, hosting arena concerts alongside the handball and volleyball fixtures that are not imported.';

-- MAXXIM
UPDATE venue SET description_alt = 'Ein Partyclub abseits des Ku''damms, seit 2007 an jedem Abend geöffnet, mit einem DJ-Programm aus Disco, House und Hip-Hop.', description_alt_language = 'de'
WHERE slug = 'maxxim' AND description = 'A party club off the Ku''damm, open every night since 2007 with a DJ programme built around disco, house and hip hop.';

-- Metropol
UPDATE venue SET description_alt = 'Ein historischer Konzertsaal am Nollendorfplatz, mit Konzerten auf Tour und gelegentlichen Clubpartys.', description_alt_language = 'de'
WHERE slug = 'metropol' AND description = 'A historic concert hall at Nollendorfplatz, programming touring concerts alongside occasional club parties.';

-- migas
UPDATE venue SET description_alt = 'Eine Listening Bar in Wedding, in der eingeladene Gäste vor sitzendem Publikum Platten auflegen, dazu Abende, an denen ein einzelnes Album von vorn bis hinten läuft.', description_alt_language = 'de'
WHERE slug = 'migas' AND description = 'A listening bar in Wedding where booked selectors play records to a seated audience, alongside nights spent listening to a single album from start to finish.';

-- Mikropol
UPDATE venue SET description_alt = 'Ein Konzertraum und Club für 250 Leute, 2025 in der früheren Durchfahrt des Metropol am Nollendorfplatz eröffnet, mit Punk-, Rap- und Indiekonzerten, Lesungen sowie House- und Techno-Nächten.', description_alt_language = 'de'
WHERE slug = 'mikropol' AND description = 'A 250-capacity concert room and club opened in 2025 in the former carriage passage of the Metropol at Nollendorfplatz, with punk, rap and indie concerts, readings and house and techno nights.';

-- Modus Berlin
UPDATE venue SET description_alt = 'Ein Kreuzberger Club und Konzertraum auf dem Gelände des Ritter Butzke, mit Acts auf Tour und Spoken-Word-Abenden.', description_alt_language = 'de'
WHERE slug = 'modus-berlin' AND description = 'A Kreuzberg club and concert room on the Ritter Butzke grounds, programming touring acts and spoken-word nights.';

-- Monarch
UPDATE venue SET description_alt = 'Eine Bar und ein Club mit Blick auf das Kottbusser Tor in Kreuzberg. Hinter einem unscheinbaren Eingang laufen DJ-Nächte und Livesets zwischen elektronischer, Indie- und experimenteller Musik.', description_alt_language = 'de'
WHERE slug = 'monarch' AND description = 'A bar and club overlooking Kottbusser Tor in Kreuzberg. Behind an unassuming entrance it hosts DJ nights and live sets spanning electronic, indie and experimental music.';

-- Monster Ronson's Ichiban Karaoke
UPDATE venue SET description_alt = 'Eine Karaokebar in Friedrichshain mit großer offener Bühne und privaten Karaokeboxen. Drag- und Varieté-MCs moderieren an jedem Abend der Woche eine andere Singalong-Nacht.', description_alt_language = 'de'
WHERE slug = 'monster-ronson-s-ichiban-karaoke' AND description = 'A Friedrichshain karaoke bar with a big open stage and private karaoke boxes. Drag and cabaret MCs host a different singalong night every evening of the week.';

-- Morphine Raum
UPDATE venue SET description_alt = 'Studio und Projektraum des Labels Morphine Records im ersten Stock eines Kreuzberger Hinterhauses, mit experimentellen, improvisierten und elektronischen Konzerten, viele davon als Live-Aufnahmesessions.', description_alt_language = 'de'
WHERE slug = 'morphine-raum' AND description = 'The Morphine Records label''s studio and project room on the first floor of a Kreuzberg backyard, staging experimental, improvised and electronic concerts, many played as live recording sessions.';

-- MS Hoppetosse
UPDATE venue SET description_alt = 'Ein festgemachtes Salonschiff auf der Spree, das der Club der Visionäre als Winterort betreibt, mit Clubfloor im Unterdeck und Bar im Oberdeck, dazu House und Minimal Techno.', description_alt_language = 'de'
WHERE slug = 'ms-hoppetosse' AND description = 'A moored Spree salon boat run by Club der Visionäre as its winter location, with a lower-deck club floor and an upper-deck bar programming house and minimal techno.';

-- Neue Zukunft
UPDATE venue SET description_alt = 'Ein Programmkino, eine Kneipe und ein Konzertraum neben der Renate auf der Halbinsel Stralau in Friedrichshain, Nachfolger der Zukunft am Ostkreuz, mit Film, Livemusik, Theater und Freiluftkino im Sommer.', description_alt_language = 'de'
WHERE slug = 'neue-zukunft' AND description = 'An arthouse cinema, pub and concert room next door to Renate on the Stralau peninsula in Friedrichshain, the successor of the Zukunft am Ostkreuz, with film, live music, theatre and a summer open-air cinema.';

-- OHM
UPDATE venue SET description_alt = 'Ein kleiner Bass- und Technoclub im Kraftwerkskomplex des Tresor, mit einem kurzen, rollierenden Fenster an DJ-Nächten.', description_alt_language = 'de'
WHERE slug = 'ohm' AND description = 'A small bass and techno club in the Tresor power-station complex, programming a short rolling window of DJ nights.';

-- Panke Culture
UPDATE venue SET description_alt = 'Ein Club, Café und eine Galerie in einem Hinterhof in Wedding, mit Clubnächten, Livemusik, Märkten und Ausstellungen.', description_alt_language = 'de'
WHERE slug = 'panke-culture' AND description = 'A club, café and gallery in a Wedding backyard, programming club nights, live music, markets and exhibitions.';

-- Parkbühne Wuhlheide
UPDATE venue SET description_alt = 'Ein großes Open-Air-Amphitheater im Park Wuhlheide, mit einem saisonalen Sommerprogramm aus Konzerten auf Tour.', description_alt_language = 'de'
WHERE slug = 'parkbuhne-wuhlheide' AND description = 'A large open-air amphitheatre in the Wuhlheide park, running a seasonal summer programme of touring concerts.';

-- Privatclub
UPDATE venue SET description_alt = 'Ein Club im Keller eines früheren Postamts an der Skalitzer Straße nahe dem Schlesischen Tor in Kreuzberg, mit Konzerten und DJ-Nächten zwischen Soul, Funk, Indie, Hip-Hop und Elektronik.', description_alt_language = 'de'
WHERE slug = 'privatclub' AND description = 'A club in the basement of a former post office on Skalitzer Straße near Schlesisches Tor in Kreuzberg, presenting concerts and DJ nights across soul, funk, indie, hip-hop and electronic music.';

-- Quasimodo
UPDATE venue SET description_alt = 'Berlins ältester Jazzkeller, abseits des Ku''damms, mit Jazz-, Blues- und Soulkonzerten sowie thematischen DJ-Nächten.', description_alt_language = 'de'
WHERE slug = 'quasimodo' AND description = 'Berlin''s oldest jazz cellar, off the Ku''damm, programming jazz, blues and soul concerts alongside themed DJ nights.';

-- Renate
UPDATE venue SET description_alt = 'Ein verwinkelter Technoclub in einem verfallenen Friedrichshainer Wohnhaus, mit mehreren benannten Floors und einem Garten, der im Sommer öffnet.', description_alt_language = 'de'
WHERE slug = 'renate' AND description = 'A warren of a techno club in a derelict Friedrichshain apartment house, with several named floors and a garden that opens in summer.';

-- Ritter Butzke
UPDATE venue SET description_alt = 'Ein alteingesessener Kreuzberger Technoclub in einer früheren Fabrik, mit mehreren Floors pro Nacht.', description_alt_language = 'de'
WHERE slug = 'ritter-butzke' AND description = 'A long-running Kreuzberg techno club in a former factory, running several floors a night.';

-- Roadrunner's Paradise
UPDATE venue SET description_alt = 'Ein Rock''n''Roll-Club im früheren Kesselhaus der alten Königstadt-Brauerei in Prenzlauer Berg, spezialisiert auf Rockabilly, Psychobilly, Punk und Garage, live und als Party.', description_alt_language = 'de'
WHERE slug = 'roadrunner-s-paradise' AND description = 'A rock''n''roll club in the former boiler house of the old Königstadt brewery in Prenzlauer Berg, specialising in rockabilly, psychobilly, punk and garage live shows and parties.';

-- Säälchen
UPDATE venue SET description_alt = 'Der Konzertsaal auf dem Holzmarkt-Gelände am Ufer, mit Konzerten und Kulturveranstaltungen direkt an der Spree.', description_alt_language = 'de'
WHERE slug = 'saalchen' AND description = 'The concert hall on the Holzmarkt riverside grounds, programming concerts and cultural events beside the Spree.';

-- Schokoladen
UPDATE venue SET description_alt = 'Ein historischer, kollektiv geführter Kulturort in Mitte mit Wurzeln in der Hausbesetzerszene nach der Wende, mit intimen Konzerten, Lesungen und Clubnächten quer durch viele Stile.', description_alt_language = 'de'
WHERE slug = 'schokoladen' AND description = 'A historic, collectively run cultural venue in Mitte with roots in the post-Wende squat scene, hosting intimate concerts, readings and club nights across many styles.';

-- silent green
UPDATE venue SET description_alt = 'Ein Kulturquartier in einem Krematorium von 1912 in Wedding, mit experimentellen Konzerten, Ausstellungen, Film und Talks in Kuppelhalle, Betonhalle und Ateliers.', description_alt_language = 'de'
WHERE slug = 'silent-green' AND description = 'A cultural quarter in a 1912 crematorium in Wedding, programming experimental concerts, exhibitions, film and talks across its Kuppelhalle, Betonhalle and ateliers.';

-- SO36
UPDATE venue SET description_alt = 'Ein legendärer Kreuzberger Club an der Oranienstraße, seit den späten 1970ern zentral für Berlins Punk- und New-Wave-Geschichte. Heute läuft hier ein vielfältiges Programm aus Punk, Rock, queeren Partys und Clubnächten.', description_alt_language = 'de'
WHERE slug = 'so36' AND description = 'A legendary Kreuzberg club on Oranienstraße, central to Berlin''s punk and new-wave history since the late 1970s. Today it runs a diverse programme of punk, rock, queer parties and club nights.';

-- Soda Club
UPDATE venue SET description_alt = 'Eine große Diskothek in der Kulturbrauerei in Prenzlauer Berg, mit Residentnächten auf fünf Floors — R''n''B, Urban Dance, Charts und 90er/2000er — dazu Salsaabende und Open Airs im Sommer.', description_alt_language = 'de'
WHERE slug = 'soda-club' AND description = 'A large discotheque in the Kulturbrauerei in Prenzlauer Berg, running resident nights across five floors — R''n''B, urban dance, charts and 90s/2000s — plus salsa evenings and summer open airs.';

-- Sonnenraum
UPDATE venue SET description_alt = 'Der Innenkonzertraum des Club der Visionäre, mit der Livebandnacht am Montag als Resident sowie gelegentlichen Jazzkonzerten und Labelpartys.', description_alt_language = 'de'
WHERE slug = 'sonnenraum' AND description = 'The indoor concert room run by Club der Visionäre, hosting the resident Monday live-band night alongside occasional jazz concerts and label parties.';

-- Supamolly
UPDATE venue SET description_alt = 'Ein früher besetztes Haus, heute ein kollektiv geführter Ort nahe dem Traveplatz in Friedrichshain, mit einem Programm auf Spendenbasis aus Punk-, Ska-, Hardcore- und experimentellen Konzerten neben Theater, Film und Kiezabenden.', description_alt_language = 'de'
WHERE slug = 'supamolly' AND description = 'A former squat turned collectively run venue near Traveplatz in Friedrichshain, with a donation-based programme of punk, ska, hardcore and experimental concerts alongside theatre, film and neighbourhood socials.';

-- Tempodrom
UPDATE venue SET description_alt = 'Ein Konzertzelt am früheren Anhalter Bahnhof, mit Großer und Kleiner Arena für Konzerte, Comedy, Shows und Kongresse.', description_alt_language = 'de'
WHERE slug = 'tempodrom' AND description = 'A tented concert hall by the former Anhalter Bahnhof, with a Grosse and a Kleine Arena hosting concerts, comedy, shows and congresses.';

-- Theater im Delphi
UPDATE venue SET description_alt = 'Ein Stummfilmkino von 1929 in Weißensee, heute Theater und Konzertsaal für Tanz, Musiktheater, Kammermusik und Gespräche.', description_alt_language = 'de'
WHERE slug = 'theater-im-delphi' AND description = 'A 1929 silent-cinema building in Weißensee, run as a theatre and concert hall for dance, music theatre, chamber music and talks.';

-- Tresor
UPDATE venue SET description_alt = 'Die Techno-Institution in einem stillgelegten Kraftwerk an der Köpenicker Straße, mit dem Tresor-Floor im Gewölbe, dem Globus darüber und der Aurora Bar.', description_alt_language = 'de'
WHERE slug = 'tresor' AND description = 'The techno institution in a disused power plant on Koepenicker Strasse, with the Tresor vault floor, the Globus floor above it and the Aurora Bar.';

-- Uber Arena
UPDATE venue SET description_alt = 'Berlins größte Halle an der Spree, mit Konzerten, Shows und Comedy auf Tour neben den Spielen der ansässigen Sportteams.', description_alt_language = 'de'
WHERE slug = 'uber-arena' AND description = 'Berlin''s largest indoor arena on the Spree, hosting touring concerts, shows and comedy alongside its resident sport teams.';

-- Uber Eats Music Hall
UPDATE venue SET description_alt = 'Ein mittelgroßer Konzertsaal gegenüber der Uber Arena am Uber Platz, mit Bands auf Tour, Comedy und Bühnenshows.', description_alt_language = 'de'
WHERE slug = 'uber-eats-music-hall' AND description = 'Mid-size concert hall across Uber Platz from the Uber Arena, programming touring bands, comedy and staged shows.';

-- UFO im Velodrom
UPDATE venue SET description_alt = 'Der kleinere Saal im Velodrom, genutzt für mittelgroße Konzerte und im Velomax-Programm als eigener Ort geführt.', description_alt_language = 'de'
WHERE slug = 'ufo-im-velodrom' AND description = 'The smaller hall configured inside the Velodrom, used for mid-sized concerts and listed as its own venue on the Velomax programme.';

-- Urania
UPDATE venue SET description_alt = 'Ein Haus für Wissenschaft und Kultur in Schöneberg, mit Vorträgen, Podiumsgesprächen und Talks im Humboldtsaal und im Kleistsaal.', description_alt_language = 'de'
WHERE slug = 'urania' AND description = 'A science and culture house in Schöneberg, programming lectures, panel discussions and talks across its Humboldtsaal and Kleistsaal.';

-- Urban Spree
UPDATE venue SET description_alt = 'Ein Künstlerort auf dem RAW-Gelände aus Galerie, Laden und Biergarten, dazu ein Konzertsaal mit Post-Punk, Dark Wave, Metal und Indie neben Ausstellungen.', description_alt_language = 'de'
WHERE slug = 'urban-spree' AND description = 'An artist space on the RAW-Gelände combining a gallery, shop and beer garden with a concert hall, programming post-punk, dark wave, metal and indie shows alongside exhibitions.';

-- Velodrom
UPDATE venue SET description_alt = 'Eine Radrennbahn am S-Bahnhof Landsberger Allee, die zugleich einer der größeren Konzertorte Berlins ist.', description_alt_language = 'de'
WHERE slug = 'velodrom' AND description = 'A cycling arena at S-Bahnhof Landsberger Allee that doubles as one of Berlin''s larger concert venues.';

-- VOID Club
UPDATE venue SET description_alt = 'Ein Techno- und Bassclub über mehrere Floors in einem Lichtenberger Hinterhof, seit 2015 geöffnet, mit Drum & Bass, Hardtechno und Trance auf den VOID-Club-Floors und in der 2023 ergänzten VOID Hall.', description_alt_language = 'de'
WHERE slug = 'void-club' AND description = 'A multi-floor techno and bass club in a Lichtenberg backyard, open since 2015, programming drum & bass, hardtechno and trance nights across the VOID Club floors and the VOID Hall added in 2023.';

-- Wild at Heart
UPDATE venue SET description_alt = 'Eine Rock''n''Roll-Bar und ein Liveclub nahe dem Görlitzer Bahnhof in Kreuzberg, mit lauten Konzerten und DJ-Nächten zwischen Rockabilly, Punk, Garage und Surf.', description_alt_language = 'de'
WHERE slug = 'wild-at-heart' AND description = 'A rock''n''roll bar and live club near Görlitzer Bahnhof in Kreuzberg, hosting raucous concerts and DJ nights spanning rockabilly, punk, garage and surf.';

-- Zenner
UPDATE venue SET description_alt = 'Ein Ort am Wasser im Treptower Park aus historischem Saal, Club, Biergarten und Weingarten, mit elektronischen Konzerten, Clubnächten und Open-Air-Partys am Tag.', description_alt_language = 'de'
WHERE slug = 'zenner' AND description = 'A riverside venue in Treptower Park combining a historic ballroom (Saal), a club, a beer garden and a wine garden, programming electronic concerts, club nights and open-air day parties.';

-- Zitadelle
UPDATE venue SET description_alt = 'Eine Renaissancefestung in Spandau, in deren Hof das Citadel Music Festival stattfindet, eine Open-Air-Konzertreihe durch den Sommer.', description_alt_language = 'de'
WHERE slug = 'zitadelle' AND description = 'Renaissance fortress in Spandau whose courtyard hosts the Citadel Music Festival, an open-air concert series running through the summer.';
