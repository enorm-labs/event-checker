/**
 * Smoke test — one virtual user, every endpoint, once each.
 *
 * This is not a load test and is not trying to be. It answers a narrower question: *does the whole
 * read API still work, and is anything catastrophically slow?* That makes it the one script safe
 * to run unattended — on a fresh checkout, after a dependency bump, or in CI — because it puts no
 * meaningful load on anything and its thresholds are loose enough not to be flaky.
 *
 * It also deliberately tolerates an **empty database**: list endpoints are still asserted, detail
 * endpoints are skipped with a warning. A brand-new environment should get a useful answer rather
 * than a setup error.
 *
 *   k6 run perf/smoke.js
 *   k6 run -e BFF_HOST=https://staging.example.com perf/smoke.js
 */
import { sleep } from 'k6'

import { baseThresholds } from './lib/config.js'
import { api, checkOk, checkPage, discover, pick } from './lib/api.js'

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: baseThresholds(),
}

export function setup() {
  return discover({ requireData: false })
}

export default function (data) {
  // Every list endpoint, including the ones the frontend does not have a page for yet
  // (`/artists`, `/promoters`) — they are public API surface and can break unnoticed.
  checkPage(api.searchEvents('?size=20'), 'GET /events')
  checkOk(api.today(), 'GET /events/today')
  checkOk(api.calendar(0, 30), 'GET /events/calendar')
  checkPage(api.listVenues('?size=20'), 'GET /venues')
  checkPage(api.listArtists('?size=20'), 'GET /artists')
  checkPage(api.listPromoters('?size=20'), 'GET /promoters')
  checkOk(api.genres(), 'GET /genres')
  checkOk(api.meta(), 'GET /meta')

  // The filter combinations most likely to hide a query bug: text search, a date window, and a
  // relational filter. Each takes a different path through the query builder.
  checkPage(api.searchEvents('?q=techno&size=20'), 'GET /events?q=')
  checkPage(api.searchEvents('?free=true&size=20'), 'GET /events?free=')
  if (data.venues.length > 0) {
    checkPage(api.searchEvents(`?venue=${data.venues[0]}&size=20`), 'GET /events?venue=')
  }

  // Detail endpoints, only where there is something to look up.
  const event = pick(data.events)
  if (event) checkOk(api.event(event), 'GET /events/{slug}')

  const venue = pick(data.venues)
  if (venue) checkOk(api.venue(venue), 'GET /venues/{slug}')

  const artist = pick(data.artists)
  if (artist) checkOk(api.artist(artist), 'GET /artists/{slug}')

  const promoter = pick(data.promoters)
  if (promoter) checkOk(api.promoter(promoter), 'GET /promoters/{slug}')

  sleep(1)
}
