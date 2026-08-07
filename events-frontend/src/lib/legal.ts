/**
 * Shared facts for the legal pages.
 *
 * One module because the imprint and the privacy notice must carry **the same** controller
 * details — §5 DDG and Art. 13 (1) (a) GDPR each require them, and two hand-maintained copies
 * would eventually disagree. See docs/FOOTER_AND_LEGAL_PLAN.md §8.3.
 */

/**
 * The controller / service provider.
 *
 * TODO(imprint-address): the postal address and email are placeholders. Replace both with the
 * rented Postflex address and the real role mailbox once `event-junkie.de` is registered, and
 * set {@link CONTACT_DETAILS_ARE_PROVISIONAL} to `false` in the same commit — a unit test holds
 * the two in step. See docs/FOOTER_AND_LEGAL_PLAN.md §8.3.
 */
export const CONTROLLER = {
  name: 'Norman Lange',
  street: 'Musterstraße 1',
  city: '12345 Musterstadt',
  country: 'Germany',
  email: 'hello@event-junkie.de',
} as const

/**
 * While `true`, the legal pages say so in a banner rather than presenting placeholder details as
 * fact — a legal page that quietly states a false address is worse than one that admits it is not
 * final. Must be `false` before go-live; the guard test in `__tests__/legal.spec.ts` fails if this
 * and the placeholder address ever disagree.
 */
export const CONTACT_DETAILS_ARE_PROVISIONAL = true

/**
 * The deployment the privacy notice describes (Cloudflare in front of Hetzner) is **proposed**,
 * not built — ADR-012 is still in `Proposed` status and nothing is deployed. Until it exists, the
 * notice describes an intent, and says so. Set to `false` when the platform decision is executed
 * and the notice has been re-checked against what actually runs.
 */
export const INFRASTRUCTURE_IS_PROPOSED = true

/** Date the legal pages were last reviewed against what the system actually does (§7.7). */
export const LAST_REVIEWED = '2026-08-07'

/** The supervisory authority for a controller established in Berlin (Art. 13 (2) (d) GDPR). */
export const SUPERVISORY_AUTHORITY = {
  name: 'Berliner Beauftragte für Datenschutz und Informationsfreiheit',
  url: 'https://www.datenschutz-berlin.de/',
} as const
