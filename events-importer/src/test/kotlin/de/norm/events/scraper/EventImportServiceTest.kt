package de.norm.events.scraper

import de.norm.events.artist.ArtistEntity
import de.norm.events.artist.ArtistRepository
import de.norm.events.event.EventArtistEntity
import de.norm.events.event.EventArtistRepository
import de.norm.events.event.EventEntity
import de.norm.events.event.EventPromoterRepository
import de.norm.events.event.EventRepository
import de.norm.events.genretag.EventGenreTagRepository
import de.norm.events.genretag.GenreTagRepository
import de.norm.events.promoter.PromoterRepository
import de.norm.events.venue.VenueEntity
import de.norm.events.venue.VenueRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import java.time.LocalDate

/**
 * Unit tests for [EventImportService].
 *
 * Tests the import pipeline logic (upsert, deduplication, stale event cleanup,
 * artist auto-creation, error handling) in isolation with mocked dependencies.
 * Persistence is delegated to a real [EventUpsertService] backed by a real
 * [AssociationSyncService] with mocked repositories.
 */
class EventImportServiceTest {
    private val eventSourceRepository: EventSourceRepository = mockk(relaxed = true)
    private val eventRepository: EventRepository = mockk(relaxed = true)
    private val eventArtistRepository: EventArtistRepository = mockk(relaxed = true)
    private val eventPromoterRepository: EventPromoterRepository = mockk(relaxed = true)
    private val eventGenreTagRepository: EventGenreTagRepository = mockk(relaxed = true)
    private val artistRepository: ArtistRepository = mockk(relaxed = true)
    private val promoterRepository: PromoterRepository = mockk(relaxed = true)
    private val genreTagRepository: GenreTagRepository = mockk(relaxed = true)
    private val venueRepository: VenueRepository = mockk(relaxed = true)

    // Create a mock importer for CASSIOPEIA
    private val cassiopeiaImporter: EventImporter =
        mockk {
            coEvery { eventSource } returns EventSource.CASSIOPEIA
        }

    // TransactionalOperator that just executes the callback directly (no real transaction).
    // Mocks the underlying execute() method which executeAndAwait delegates to.
    private val transactionalOperator: TransactionalOperator =
        mockk {
            coEvery { execute(any<org.springframework.transaction.reactive.TransactionCallback<Any>>()) } answers {
                val callback = firstArg<org.springframework.transaction.reactive.TransactionCallback<Any>>()
                val reactiveTransaction = mockk<org.springframework.transaction.ReactiveTransaction>(relaxed = true)
                Flux.from(callback.doInTransaction(reactiveTransaction))
            }
        }

    /** Real services backed by mocked repositories — tested indirectly through the import pipeline. */
    private lateinit var associationSyncService: AssociationSyncService
    private lateinit var eventUpsertService: EventUpsertService
    private lateinit var service: EventImportService

    /** Reusable event source entity with sensible defaults. */
    private fun source(
        id: Long = 1L,
        slug: String = "test-source",
        sourceType: String = "CASSIOPEIA",
        venueId: Long = 10L,
        url: String = "https://example.com/events",
        enabled: Boolean = true,
        etag: String? = null,
        lastModified: String? = null
    ) = EventSourceEntity(
        id = id,
        venueId = venueId,
        name = "Test Source",
        slug = slug,
        url = url,
        sourceType = sourceType,
        enabled = enabled,
        etag = etag,
        lastModified = lastModified
    )

    /** Creates a minimal [ScrapedEvent] for testing. */
    private fun scrapedEvent(
        title: String = "Test Event",
        eventDate: LocalDate = LocalDate.of(2026, 6, 15),
        sourceId: String = "cassiopeia:test-event",
        sourceUrl: String = "https://example.com/event/test",
        eventType: String = "CONCERT",
        status: String = "SCHEDULED",
        artists: List<ScrapedArtist> = emptyList()
    ) = ScrapedEvent(
        title = title,
        eventDate = eventDate,
        sourceId = sourceId,
        sourceUrl = sourceUrl,
        eventType = eventType,
        status = status,
        artists = artists
    )

    @BeforeEach
    fun setUp() {
        associationSyncService =
            AssociationSyncService(
                eventArtistRepository = eventArtistRepository,
                eventPromoterRepository = eventPromoterRepository,
                eventGenreTagRepository = eventGenreTagRepository,
                artistRepository = artistRepository,
                promoterRepository = promoterRepository,
                genreTagRepository = genreTagRepository
            )

        eventUpsertService =
            EventUpsertService(
                eventRepository = eventRepository,
                associationSyncService = associationSyncService
            )

        service =
            EventImportService(
                eventSourceRepository = eventSourceRepository,
                eventUpsertService = eventUpsertService,
                eventImporters = listOf(cassiopeiaImporter),
                venueRepository = venueRepository,
                transactionalOperator = transactionalOperator,
                maxConcurrency = EventImportService.DEFAULT_MAX_CONCURRENCY
            )

        // Default stubs: empty collections, save returns input with ID
        coEvery { eventRepository.findBySourceIdIn(any()) } returns emptyFlow()
        coEvery { artistRepository.findBySlugIn(any()) } returns emptyFlow()
        coEvery { eventRepository.findByEventSourceIdAndEventDateBetween(any(), any(), any()) } returns emptyFlow()
        coEvery { eventRepository.deleteByIdIn(any()) } returns Unit
        coEvery { eventArtistRepository.findByEventIdIn(any()) } returns emptyFlow()
        coEvery { eventArtistRepository.deleteAllById(any()) } returns Unit

        // Default venue stub — returns a venue with a known slug for event slug generation
        coEvery { venueRepository.findById(any<Long>()) } returns
            VenueEntity(
                id = 10L,
                name = "Test Venue",
                slug = "test-venue"
            )

        // saveAll() returns a flow of entities with assigned IDs
        coEvery { eventRepository.saveAll(any<Iterable<EventEntity>>()) } answers {
            firstArg<Iterable<EventEntity>>()
                .mapIndexed { index, entity ->
                    entity.copy(id = entity.id ?: (100L + index))
                }.asFlow()
        }
        coEvery { eventSourceRepository.save(any()) } answers {
            firstArg<EventSourceEntity>()
        }
        coEvery { artistRepository.save(any()) } answers {
            firstArg<ArtistEntity>().copy(id = 200L)
        }
        coEvery { eventArtistRepository.saveAll(any<Iterable<EventArtistEntity>>()) } answers {
            firstArg<Iterable<EventArtistEntity>>()
                .mapIndexed { index, entity ->
                    entity.copy(id = (300L + index))
                }.asFlow()
        }
    }

    @Nested
    inner class ImportFromSource {
        @Test
        fun `successful import creates events and returns result`() =
            runTest {
                val src = source()
                val events = listOf(scrapedEvent(title = "Show A", sourceId = "cassiopeia:show-a"))

                coEvery { cassiopeiaImporter.importEvents(src.url, src.etag, src.lastModified) } returns
                    ImportResult.Success(events = events, etag = "\"new-etag\"", lastModified = "Wed, 01 Jan 2026 00:00:00 GMT")

                val result = service.importFromSource(src)

                result.imported shouldBe true
                result.eventCount shouldBe 1
                result.sourceSlug shouldBe "test-source"
                result.error shouldBe null
            }

        @Test
        fun `NotModified result returns imported=false`() =
            runTest {
                val src = source(etag = "\"old-etag\"")

                coEvery { cassiopeiaImporter.importEvents(src.url, src.etag, src.lastModified) } returns
                    ImportResult.NotModified

                val result = service.importFromSource(src)

                result.imported shouldBe false
                result.eventCount shouldBe 0
                result.error shouldBe null
            }

        @Test
        fun `unknown source type records misconfiguration and returns error`() =
            runTest {
                val src = source(sourceType = "NONEXISTENT")

                val result = service.importFromSource(src)

                result.imported shouldBe false
                result.error shouldBe "Unknown source type 'NONEXISTENT'"

                // Should mark the source as MISCONFIGURED (not FAILED) — config errors don't consume retry budget
                coVerify {
                    eventSourceRepository.save(match { it.status == ImportStatus.MISCONFIGURED.name && it.retryCount == 0 })
                }
            }

        @Test
        fun `no importer registered records misconfiguration`() =
            runTest {
                // Create a service with no importers registered
                val emptyService =
                    EventImportService(
                        eventSourceRepository = eventSourceRepository,
                        eventUpsertService = eventUpsertService,
                        eventImporters = emptyList(),
                        venueRepository = venueRepository,
                        transactionalOperator = transactionalOperator,
                        maxConcurrency = EventImportService.DEFAULT_MAX_CONCURRENCY
                    )

                val src = source()
                val result = emptyService.importFromSource(src)

                result.imported shouldBe false
                result.error shouldBe "No importer registered for source type 'CASSIOPEIA'"

                // Should mark as MISCONFIGURED (not FAILED) — missing importer is a config issue
                coVerify {
                    eventSourceRepository.save(match { it.status == ImportStatus.MISCONFIGURED.name && it.retryCount == 0 })
                }
            }

        @Test
        fun `exception during import records failure`() =
            runTest {
                val src = source()
                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } throws
                    RuntimeException("Network timeout")

                val result = service.importFromSource(src)

                result.imported shouldBe false
                result.error shouldBe "Network timeout"

                coVerify {
                    eventSourceRepository.save(match { it.status == ImportStatus.FAILED.name })
                }
            }

        @Test
        fun `source without persisted id throws IllegalArgumentException`() =
            runTest {
                val src = source().copy(id = null)

                val ex =
                    shouldThrow<IllegalArgumentException> {
                        service.importFromSource(src)
                    }
                ex.message shouldBe "Event source must be persisted (have a non-null id) before importing"
            }

        @Test
        fun `marks source as RUNNING before import`() =
            runTest {
                val src = source()
                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = emptyList(), etag = null, lastModified = null)

                service.importFromSource(src)

                // First save should be markRunning
                coVerify {
                    eventSourceRepository.save(match { it.status == ImportStatus.RUNNING.name })
                }
            }

        @Test
        fun `marks source as SUCCESS after successful import`() =
            runTest {
                val src = source()
                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(
                        events = listOf(scrapedEvent()),
                        etag = "\"new-etag\"",
                        lastModified = null
                    )

                service.importFromSource(src)

                coVerify {
                    eventSourceRepository.save(match { it.status == ImportStatus.SUCCESS.name && it.lastEventCount == 1 })
                }
            }
    }

    @Nested
    inner class UpsertAndArtistCreation {
        @Test
        fun `auto-creates unknown artists during import`() =
            runTest {
                val src = source()
                val events =
                    listOf(
                        scrapedEvent(
                            title = "Concert Night",
                            sourceId = "cassiopeia:concert-night",
                            artists = listOf(ScrapedArtist(name = "New Band", role = "HEADLINER"))
                        )
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                service.importFromSource(src)

                // Verify artist was auto-created
                coVerify {
                    artistRepository.save(match { it.name == "New Band" && it.slug == "new-band" })
                }
            }

        @Test
        fun `reuses existing artist by slug instead of creating duplicate`() =
            runTest {
                val src = source()
                val existingArtist = ArtistEntity(id = 50L, name = "Existing Band", slug = "existing-band")

                coEvery { artistRepository.findBySlugIn(any()) } returns listOf(existingArtist).asFlow()

                val events =
                    listOf(
                        scrapedEvent(
                            title = "Show",
                            sourceId = "cassiopeia:show",
                            artists = listOf(ScrapedArtist(name = "Existing Band", role = "HEADLINER"))
                        )
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                service.importFromSource(src)

                // Should NOT create a new artist — reuse existing
                coVerify(exactly = 0) {
                    artistRepository.save(any())
                }

                // Should create the event-artist association using the existing artist's ID
                coVerify {
                    eventArtistRepository.saveAll(
                        match<Iterable<EventArtistEntity>> { entities ->
                            entities.any { it.artistId == 50L }
                        }
                    )
                }
            }

        @Test
        fun `updates existing event instead of creating duplicate`() =
            runTest {
                val src = source()
                val existingEvent =
                    EventEntity(
                        id = 42L,
                        venueId = 10L,
                        title = "Old Title",
                        slug = "old-slug",
                        eventDate = LocalDate.of(2026, 6, 15),
                        sourceId = "cassiopeia:show",
                        eventSourceId = 1L
                    )

                coEvery { eventRepository.findBySourceIdIn(any()) } returns listOf(existingEvent).asFlow()

                val events =
                    listOf(
                        scrapedEvent(
                            title = "Updated Title",
                            sourceId = "cassiopeia:show",
                            eventDate = LocalDate.of(2026, 6, 15)
                        )
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                service.importFromSource(src)

                // Verify saveAll was called with the existing event's ID (update, not insert)
                coVerify {
                    eventRepository.saveAll(
                        match<Iterable<EventEntity>> { entities ->
                            entities.any { it.id == 42L && it.title == "Updated Title" }
                        }
                    )
                }
            }

        @Test
        fun `skips saving unchanged events to avoid unnecessary database writes`() =
            runTest {
                val src = source()
                // Existing event in DB matches exactly what the scraper returns — no changes
                val existingEvent =
                    EventEntity(
                        id = 42L,
                        venueId = 10L,
                        title = "Concert Night",
                        slug = "2026-06-15-test-venue-concert-night",
                        eventDate = LocalDate.of(2026, 6, 15),
                        sourceId = "cassiopeia:show",
                        sourceUrl = "https://example.com/event/test",
                        eventSourceId = 1L,
                        eventType = "CONCERT",
                        status = "SCHEDULED"
                    )

                coEvery { eventRepository.findBySourceIdIn(any()) } returns listOf(existingEvent).asFlow()

                val events =
                    listOf(
                        scrapedEvent(
                            title = "Concert Night",
                            sourceId = "cassiopeia:show",
                            eventDate = LocalDate.of(2026, 6, 15)
                        )
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                val result = service.importFromSource(src)

                result.imported shouldBe true
                result.eventCount shouldBe 1

                // saveAll should NOT be called — the event hasn't changed
                coVerify(exactly = 0) {
                    eventRepository.saveAll(any<Iterable<EventEntity>>())
                }
            }

        @Test
        fun `creates event-artist associations with correct billing order`() =
            runTest {
                val src = source()
                val events =
                    listOf(
                        scrapedEvent(
                            title = "Multi-Artist Show",
                            sourceId = "cassiopeia:multi",
                            artists =
                                listOf(
                                    ScrapedArtist(name = "Headliner", role = "HEADLINER"),
                                    ScrapedArtist(name = "Support Act", role = "SUPPORT")
                                )
                        )
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                val savedAssociations = mutableListOf<EventArtistEntity>()
                coEvery { eventArtistRepository.saveAll(any<Iterable<EventArtistEntity>>()) } answers {
                    firstArg<Iterable<EventArtistEntity>>()
                        .mapIndexed { index, entity ->
                            entity.also { savedAssociations.add(it) }.copy(id = (300L + index))
                        }.asFlow()
                }

                service.importFromSource(src)

                savedAssociations.size shouldBe 2
                savedAssociations[0].billingOrder shouldBe 0
                savedAssociations[0].role shouldBe "HEADLINER"
                savedAssociations[1].billingOrder shouldBe 1
                savedAssociations[1].role shouldBe "SUPPORT"
            }
    }

    @Nested
    inner class Deduplication {
        @Test
        fun `deduplicates scraped events with same date and title`() =
            runTest {
                val src = source()
                val events =
                    listOf(
                        scrapedEvent(title = "Dup Event", eventDate = LocalDate.of(2026, 6, 15), sourceId = "cassiopeia:dup-1"),
                        scrapedEvent(title = "Dup Event", eventDate = LocalDate.of(2026, 6, 15), sourceId = "cassiopeia:dup-2")
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                val result = service.importFromSource(src)

                // Only 1 event should be upserted (duplicate dropped)
                result.eventCount shouldBe 1
            }
    }

    @Nested
    inner class StaleEventCleanup {
        @Test
        fun `removes stale events no longer listed by source`() =
            runTest {
                val src = source()
                val eventDate = LocalDate.of(2026, 6, 15)

                // Currently scraped events
                val scrapedEvents =
                    listOf(
                        scrapedEvent(title = "Active Event", sourceId = "cassiopeia:active", eventDate = eventDate)
                    )

                // Existing events in DB — one is stale (not in scraped list)
                val activeEvent =
                    EventEntity(
                        id = 1L,
                        venueId = 10L,
                        title = "Active Event",
                        slug = "active",
                        eventDate = eventDate,
                        sourceId = "cassiopeia:active",
                        eventSourceId = 1L
                    )
                val staleEvent =
                    EventEntity(
                        id = 2L,
                        venueId = 10L,
                        title = "Removed Event",
                        slug = "removed",
                        eventDate = eventDate,
                        sourceId = "cassiopeia:removed",
                        eventSourceId = 1L
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = scrapedEvents, etag = null, lastModified = null)
                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(any(), any(), any())
                } returns listOf(activeEvent, staleEvent).asFlow()

                service.importFromSource(src)

                // Stale event (id=2) should be deleted
                coVerify {
                    eventRepository.deleteByIdIn(match { 2L in it })
                }
            }

        @Test
        fun `does not delete events when scraped list is empty`() =
            runTest {
                val src = source()

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = emptyList(), etag = null, lastModified = null)

                service.importFromSource(src)

                // No deletion should occur for empty import results
                coVerify(exactly = 0) {
                    eventRepository.deleteByIdIn(any())
                }
            }
    }

    @Nested
    inner class ImportConcurrently {
        @Test
        fun `empty source list returns empty result list`() =
            runTest {
                val results = service.importConcurrently(emptyList())

                results shouldBe emptyList()
            }

        @Test
        fun `invokes importFromSource for each source and preserves result ordering`() =
            runTest {
                val src1 = source(id = 1L, slug = "source-1")
                val src2 = source(id = 2L, slug = "source-2")
                val src3 = source(id = 3L, slug = "source-3")

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = emptyList(), etag = null, lastModified = null)

                val results = service.importConcurrently(listOf(src1, src2, src3))

                results.size shouldBe 3
                // Result ordering must match input source ordering
                results[0].sourceSlug shouldBe "source-1"
                results[1].sourceSlug shouldBe "source-2"
                results[2].sourceSlug shouldBe "source-3"
            }

        @Test
        fun `one source failure does not prevent other sources from completing`() =
            runTest {
                val src1 = source(id = 1L, slug = "success-source", url = "https://example.com/source-1")
                val src2 = source(id = 2L, slug = "failing-source", url = "https://example.com/source-2")
                val src3 = source(id = 3L, slug = "another-success", url = "https://example.com/source-3")

                // src1 and src3 succeed; src2 throws
                coEvery { cassiopeiaImporter.importEvents(src1.url, any(), any()) } returns
                    ImportResult.Success(events = listOf(scrapedEvent(sourceId = "cassiopeia:s1")), etag = null, lastModified = null)
                coEvery { cassiopeiaImporter.importEvents(src2.url, any(), any()) } throws
                    RuntimeException("Connection refused")
                coEvery { cassiopeiaImporter.importEvents(src3.url, any(), any()) } returns
                    ImportResult.Success(events = listOf(scrapedEvent(sourceId = "cassiopeia:s3")), etag = null, lastModified = null)

                val results = service.importConcurrently(listOf(src1, src2, src3))

                // All three results returned — failure is isolated to the individual source
                results.size shouldBe 3
                results[0].imported shouldBe true
                results[1].imported shouldBe false
                results[1].error shouldBe "Connection refused"
                results[2].imported shouldBe true
            }
    }

    @Nested
    inner class ImportAll {
        @Test
        fun `importAll processes all enabled sources`() =
            runTest {
                val src1 = source(id = 1L, slug = "source-1")
                val src2 = source(id = 2L, slug = "source-2")

                coEvery { eventSourceRepository.findByEnabledTrue() } returns listOf(src1, src2).asFlow()
                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = emptyList(), etag = null, lastModified = null)

                val results = service.importAll()

                results.size shouldBe 2
            }

        @Test
        fun `importAll returns empty list when no sources enabled`() =
            runTest {
                coEvery { eventSourceRepository.findByEnabledTrue() } returns emptyFlow()

                val results = service.importAll()

                results.size shouldBe 0
            }
    }

    @Nested
    inner class ImportBySlug {
        @Test
        fun `importBySlug delegates to importFromSource`() =
            runTest {
                val src = source(slug = "my-source")
                coEvery { eventSourceRepository.findBySlug("my-source") } returns src
                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.NotModified

                val result = service.importBySlug("my-source")

                result.sourceSlug shouldBe "my-source"
                result.imported shouldBe false
            }

        @Test
        fun `importBySlug throws EventSourceNotFoundException for unknown slug`() =
            runTest {
                coEvery { eventSourceRepository.findBySlug("unknown") } returns null

                shouldThrow<EventSourceNotFoundException> {
                    service.importBySlug("unknown")
                }
            }
    }

    @Nested
    inner class EnumParsing {
        @Test
        fun `unknown event type falls back to OTHER`() =
            runTest {
                val src = source()
                val events =
                    listOf(
                        scrapedEvent(title = "Weird Type", sourceId = "cassiopeia:weird", eventType = "UNKNOWN_TYPE")
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                val savedEvents = mutableListOf<EventEntity>()
                coEvery { eventRepository.saveAll(any<Iterable<EventEntity>>()) } answers {
                    firstArg<Iterable<EventEntity>>()
                        .map { entity ->
                            entity.copy(id = entity.id ?: 100L).also { savedEvents.add(it) }
                        }.asFlow()
                }

                service.importFromSource(src)

                savedEvents.first().eventType shouldBe "OTHER"
            }

        @Test
        fun `unknown event status falls back to SCHEDULED`() =
            runTest {
                val src = source()
                val events =
                    listOf(
                        scrapedEvent(title = "Bad Status", sourceId = "cassiopeia:bad", status = "INVALID_STATUS")
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                val savedEvents = mutableListOf<EventEntity>()
                coEvery { eventRepository.saveAll(any<Iterable<EventEntity>>()) } answers {
                    firstArg<Iterable<EventEntity>>()
                        .map { entity ->
                            entity.copy(id = entity.id ?: 100L).also { savedEvents.add(it) }
                        }.asFlow()
                }

                service.importFromSource(src)

                savedEvents.first().status shouldBe "SCHEDULED"
            }

        @Test
        fun `unknown artist role falls back to HEADLINER`() =
            runTest {
                val src = source()
                val events =
                    listOf(
                        scrapedEvent(
                            title = "Bad Role",
                            sourceId = "cassiopeia:role",
                            artists = listOf(ScrapedArtist(name = "Band", role = "UNKNOWN_ROLE"))
                        )
                    )

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = events, etag = null, lastModified = null)

                val savedAssociations = mutableListOf<EventArtistEntity>()
                coEvery { eventArtistRepository.saveAll(any<Iterable<EventArtistEntity>>()) } answers {
                    firstArg<Iterable<EventArtistEntity>>()
                        .mapIndexed { index, entity ->
                            entity.also { savedAssociations.add(it) }.copy(id = (300L + index))
                        }.asFlow()
                }

                service.importFromSource(src)

                savedAssociations.first().role shouldBe "HEADLINER"
            }
    }

    @Nested
    inner class ETagAndLastModified {
        @Test
        fun `successful import updates etag and lastModified on source`() =
            runTest {
                val src = source(etag = "\"old\"", lastModified = "Mon, 01 Jan 2026 00:00:00 GMT")

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(
                        events = listOf(scrapedEvent()),
                        etag = "\"new-etag\"",
                        lastModified = "Wed, 15 Jun 2026 00:00:00 GMT"
                    )

                service.importFromSource(src)

                // markSuccess should save with new etag/lastModified
                coVerify {
                    eventSourceRepository.save(
                        match {
                            it.status == ImportStatus.SUCCESS.name &&
                                it.etag == "\"new-etag\"" &&
                                it.lastModified == "Wed, 15 Jun 2026 00:00:00 GMT"
                        }
                    )
                }
            }
    }

    @Nested
    inner class OptimisticLockingRetry {
        @Test
        fun `retries markSuccess with fresh entity on OptimisticLockingFailureException`() =
            runTest {
                val src = source()
                val freshSource = source(id = 1L).copy(version = 5L)

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } returns
                    ImportResult.Success(events = listOf(scrapedEvent()), etag = null, lastModified = null)

                // First save for markRunning succeeds; second save (markSuccess) throws, retry succeeds
                var saveCallCount = 0
                coEvery { eventSourceRepository.save(any()) } answers {
                    saveCallCount++
                    val entity = firstArg<EventSourceEntity>()
                    if (saveCallCount == 2 && entity.status == ImportStatus.SUCCESS.name) {
                        throw OptimisticLockingFailureException("Version conflict")
                    }
                    entity
                }
                coEvery { eventSourceRepository.findById(1L) } returns freshSource

                val result = service.importFromSource(src)

                result.imported shouldBe true
                // Verify re-fetch happened
                coVerify { eventSourceRepository.findById(1L) }
                // Third save should succeed (the retry)
                coVerify(atLeast = 3) { eventSourceRepository.save(any()) }
            }

        @Test
        fun `retries markFailed with fresh entity on OptimisticLockingFailureException`() =
            runTest {
                val src = source()
                val freshSource = source(id = 1L).copy(version = 5L)

                coEvery { cassiopeiaImporter.importEvents(any(), any(), any()) } throws
                    RuntimeException("Network timeout")

                // First save (markRunning) succeeds; second save (markFailed) throws, retry succeeds
                var saveCallCount = 0
                coEvery { eventSourceRepository.save(any()) } answers {
                    saveCallCount++
                    val entity = firstArg<EventSourceEntity>()
                    if (saveCallCount == 2 && entity.status == ImportStatus.FAILED.name) {
                        throw OptimisticLockingFailureException("Version conflict")
                    }
                    entity
                }
                coEvery { eventSourceRepository.findById(1L) } returns freshSource

                val result = service.importFromSource(src)

                result.imported shouldBe false
                result.error shouldBe "Network timeout"
                coVerify { eventSourceRepository.findById(1L) }
            }
    }
}
