# Tippmix API discovery

Status: reverse-engineered from the Tippmix frontend on **2026-08-16**.

This is **not** an official public developer API. It appears to be an internal/frontend HTTP API and must be treated as **unstable**. SafeEdge must isolate it behind a provider adapter. Do not treat Tippmix field names, IDs, or market codes as SafeEdge domain types.

Facts below are split into **Verified**, **Inferred**, and **Unknown**.

---

## Verified

### Base URL and compatibility

- Base URL: `https://api.tippmix.hu`
- Query parameter `compatibility=v1` was present on both discovered endpoints.

### Authentication

Both discovered endpoints could be invoked during manual testing **without** authentication or session credentials.

This does not prove that the API is public, unauthenticated in all environments, or stable. Do not assume cookies, tokens, or headers will never be required.

### Event search

```http
POST https://api.tippmix.hu/v2/tippmix/events?compatibility=v1
Content-Type: application/json
```

Observed football request body:

```json
{
  "search": "",
  "sportId": 1,
  "competitionGroupId": null,
  "competitionOrAliasId": null,
  "eventTypes": [],
  "marketTypes": [],
  "maxDate": null,
  "maxOdds": null,
  "minDate": null,
  "minOdds": null,
  "page": 1,
  "pageSize": 20
}
```

Response structure:

- `events[]`
- `_meta.totalCount`
- `_meta.pageCount`
- `_meta.currentPage`
- `_meta.pageSize`

The list payload may include only a main/summary market even when `totalMarketCount` is much larger. **Do not treat search results as the complete market source for an event.**

### Full event offer

```http
GET https://api.tippmix.hu/v2/tippmix/event/{eventId}/ungrouped?compatibility=v1
```

Response structure:

- `event`
- `event.markets[]`
- `event.marketGroups[]`

This endpoint returned the full visible Tippmix betting offer for the requested event.

A pre-match event was observed with:

- `isLive = 0`
- `hasVisiblePrematchMarket = true`
- `remainingPrematchMarketCount = 87`

and its ungrouped response contained the complete pre-match market list.

### Important event fields

Observed on event records:

- `eventId`
- `betradarId`
- `eventDate` (example: `2026-08-16T14:00:00+02:00`, offset present)
- `eventName`
- `eventParticipants`
- `competitionGroupId`
- `competitionGroupName`
- `competitionId`
- `competitionName`
- `sportId`
- `sportName`
- `isLive`
- `isOutright`
- `hasVisibleLiveMarket`
- `hasVisiblePrematchMarket`
- `remainingLiveMarketCount`
- `remainingPrematchMarketCount`
- `totalMarketCount`
- `bettingPhase`
- `bettingStatus`
- `eventVersion`
- `markets`
- `marketGroups` (full-event response)

Also represented in the client DTOs because they appeared in payloads: `competitionGroupRefId`, `competitionRefId`.

### Important market fields

- `marketId`
- `marketName`
- `marketRealNo`
- `marketStatus`
- `marketType`
- `marketSubType`
- `marketTypePriority`
- `marketVersion`
- `mainMarket`
- `outcomeCount`
- `specialOddsValue` (string; meaning depends on market family)
- `marketGroupIds`
- `outcomes`

### Important outcome fields

- `outcomeNo`
- `outcomeName`
- `outcomeRealNo`
- `fixedOdds`
- `outcomeResult`
- `isCustomBet`

### Observed market-group IDs

- `5` = Hendikep
- `6017` = Ázsiai

A market group object may be `{ "type": "all" }` with no `id` / `name`.

### European / 3-way handicap (example, not a global mapping)

- `marketName` = `Hendikep 0:1`
- `marketSubType` = `100`
- `marketType` = `2`
- `outcomeCount` = `3`
- `marketGroupIds` contains `5` (`Hendikep`)
- `specialOddsValue` = `"-1"`
- outcomes: home / draw / away

This is **not** Asian handicap.

### Pre-match Asian handicap (example)

- `marketName` = `Ázsiai Hendikep -1`
- `marketSubType` = `96`
- `marketType` = `2`
- `specialOddsValue` = `"-1"`
- `marketGroupIds` = `[5, 6017]`
- example outcomes: `Djurgarden -1` @ `1.57`, `AIK Stockholm +1` @ `2.12`

Quarter-lines were represented as decimals, not split strings:

- `Ázsiai Hendikep -1,25`
- `specialOddsValue` = `"-1.25"`
- outcomes used `1,25` in names; the line value was `"-1.25"`

Do **not** invent representations such as `"1/1.5"`.

### Pre-match Asian totals (example)

Names such as `Ázsiai Gólszám 1,75`, `2`, `2,25`, `2,75`, `3`, `3,25`, …

- `marketSubType` = `97`
- `marketGroupIds` contains `6017`
- `specialOddsValue` = decimal line as a string

### Live vs pre-match provider codes

A live Asian-handicap market was observed with:

- `marketType` = `4`
- `marketSubType` = `234`

Pre-match Asian handicap used:

- `marketType` = `2`
- `marketSubType` = `96`

**Do not** assume one Tippmix `marketSubType` is globally equivalent to one SafeEdge market type. Normalization is a later task.

### Betradar / Sportradar identifier

For one observed event:

- Tippmix `eventId` = `5311343`
- `betradarId` = `72409632`
- Tippmix also loaded Sportradar match data using `match_info/72409632`

This is a verified identifier relationship **for that event**. Do not assume every event has a usable `betradarId`. Sportradar integration is out of scope.

---

## Inferred

- `sportId = 1` is football in the observed Tippmix catalog.
- Search `page` is 1-based.
- `specialOddsValue` is a shared string field reused by handicap, totals, and other families.
- List `markets` is a summary subset; ungrouped `event.markets` is the offer used for a full read.
- `betradarId` is a Sportradar match id when present.
- Flag-like fields may mix JSON number (`isLive = 0`) and JSON boolean (`hasVisiblePrematchMarket = true`) in observed payloads.

---

## Unknown / needs further verification

- Official meaning of `eventTypes` and `marketTypes` (element type and allowed values).
- Complete enumeration of `bettingPhase`, `bettingStatus`, `marketStatus`, `outcomeResult`.
- Whether `isLive` / `isOutright` are always integers (`0`/`1`) or sometimes booleans.
- Whether `competitionGroupRefId` / `competitionRefId` are always present.
- Rate limits, required headers, cookies, or geo restrictions in other environments.
- Whether `compatibility=v1` is required forever, or what `v2` would change.
- Full live-market catalog and whether live `marketSubType` values collide with pre-match ones.
- Historical odds, results, or statistics endpoints (not discovered here).
- Whether every event has `betradarId`, and whether it is unique across sports.
- Pagination maximum `pageSize` and total-count accuracy.
- Push / websocket offer updates (not verified).
