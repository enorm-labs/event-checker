/**
 * Spike test — a sudden surge, then back to nothing.
 *
 * This is the shape traffic to an events site actually takes. A lineup announcement, a festival
 * going on sale, or a post that gets picked up sends a large number of people to the *same* few
 * pages within a couple of minutes, and then it stops. Steady load never produces that.
 *
 * What it is looking for is different from the load test, so read the output differently:
 *
 *   - **Recovery**, not peak throughput. Errors during the spike are survivable. Errors that
 *     continue after it are not — that is a pool that never drained or a queue that never emptied.
 *   - **Where it breaks first.** Connection refusals mean the accept queue; 5xx means the app;
 *     timeouts with no errors usually mean the database.
 *
 * Thresholds are therefore deliberately absent by default: this script is diagnostic, and a red
 * threshold would say only "a spike is hard", which was never in question. Set `STRICT=true` to
 * apply the standard budgets once there is a deployed environment worth holding to them.
 *
 *   k6 run perf/spike.js
 *   k6 run -e PEAK=200 -e STRICT=true perf/spike.js
 */
import { sleep } from 'k6'

import { baseThresholds } from './lib/config.js'
import { api, checkOk, checkPage, discover, pick } from './lib/api.js'

const PEAK = Number(__ENV.PEAK || 100)

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: Math.max(1, Math.round(PEAK * 0.05)) }, // quiet baseline
        { duration: '10s', target: PEAK }, // the surge
        { duration: '1m', target: PEAK }, // sustained peak
        { duration: '20s', target: Math.max(1, Math.round(PEAK * 0.05)) }, // it passes
        { duration: '40s', target: Math.max(1, Math.round(PEAK * 0.05)) }, // does it recover?
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
  },
  thresholds: __ENV.STRICT === 'true' ? baseThresholds() : {},
}

export function setup() {
  return discover()
}

export default function (data) {
  // A spike concentrates on a handful of pages rather than spreading across the site — everyone
  // arrives from the same link. Modelled here as the home feeds plus one detail page, which is
  // also the worst case for caching: the same rows, requested by everybody, at once.
  checkOk(api.today(), 'GET /events/today')
  checkPage(api.searchEvents('?size=12'), 'GET /events')

  const event = pick(data.events)
  if (event) checkOk(api.event(event), 'GET /events/{slug}')

  sleep(1)
}
