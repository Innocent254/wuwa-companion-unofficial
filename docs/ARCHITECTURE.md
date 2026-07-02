# Wuthering Waves Companion Ecosystem — System Architecture (v0.1 / Phase 1)

## 0. Scope note

This is a large, multi-subsystem platform. Building it "complete" in one pass isn't realistic — each of the 8 subsystems below is itself a multi-week project. This document is the **master blueprint**: every module, contract, and schema needed so the system can be built incrementally without rework. Subsequent phases implement each box below one at a time, starting with the Backend Automation Engine (Section 4), since nothing else can be tested without real data flowing.

**Build order (recommended):**
1. Backend scraper framework (1 source adapter, 1 entity type: Characters) → produces real JSON
2. Database generation + versioning system
3. GitHub Actions pipeline (scrape → build → release)
4. Android: Room schema + Update Manager (consumes the release feed)
5. Android: UI feature modules (characters list/detail, search, favorites)
6. Image optimization pipeline
7. AI-assisted parsing layer (patch notes summarization)
8. Hardening: security, rollback, delta patching, telemetry

---

## 1. High-Level System Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TRUSTED SOURCES                              │
│   Official site │ Patch notes │ Wikis │ JSON repos │ Community DBs   │
└───────────────────────────────┬───────────────────────────────────────┘
                                 │ (polite scraping, rate-limited)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    BACKEND AUTOMATION ENGINE (Python)                │
│  ┌───────────┐  ┌────────────┐  ┌─────────────┐  ┌────────────────┐ │
│  │  Source   │→ │   Parser   │→ │  Validator  │→ │   Normalizer   │ │
│  │  Adapters │  │ (per type) │  │  (Pydantic) │  │ (canonical IDs)│ │
│  └───────────┘  └────────────┘  └─────────────┘  └────────┬───────┘ │
│                                                             ▼         │
│  ┌───────────────┐   ┌────────────────┐   ┌───────────────────────┐ │
│  │ Image Pipeline│←──│ Change Detector│──→│  DB Generator          │ │
│  │ (WebP, hash)  │   │ (diff vs prev) │   │ (JSON + SQLite)        │ │
│  └───────────────┘   └────────────────┘   └───────────┬───────────┘ │
└─────────────────────────────────────────────────────────┼───────────┘
                                                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   GITHUB AUTOMATION / DISTRIBUTION                   │
│  Actions (cron) → Build artifacts → Version manifest → Release      │
│  (assets.zip, database.zip, version.json, CHANGELOG.md)             │
└───────────────────────────────┬───────────────────────────────────────┘
                                 │ HTTPS (Releases API / raw CDN)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        ANDROID APPLICATION                           │
│  UpdateManager → checks version.json → downloads delta/full package │
│      → verifies SHA-256 → migrates Room DB → atomic swap + rollback │
│                                                                       │
│  UI: Compose + MVVM + Hilt   Data: Room (offline-first) + DataStore  │
│  Feature modules: characters / weapons / echoes / materials / guides│
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Repository / Monorepo Layout

```
wuwa-ecosystem/
├── android-app/                  # Kotlin/Compose client
│   ├── app/
│   ├── core/
│   │   ├── database/             # Room entities, DAOs, migrations
│   │   ├── network/               # Retrofit/Ktor clients
│   │   ├── update/                # UpdateManager, DownloadWorker
│   │   ├── designsystem/
│   │   └── common/
│   ├── feature/
│   │   ├── characters/
│   │   ├── weapons/
│   │   ├── echoes/
│   │   ├── materials/
│   │   ├── guides/
│   │   ├── search/
│   │   └── settings/
│   ├── gradle/
│   └── build.gradle.kts
│
├── backend/                      # Python automation engine
│   ├── scrapers/
│   │   ├── base.py                # BaseScraper interface
│   │   ├── characters.py
│   │   ├── weapons.py
│   │   ├── echoes.py
│   │   ├── materials.py
│   │   ├── events.py
│   │   ├── quests.py
│   │   ├── patch_notes.py
│   │   ├── banners.py
│   │   └── maps.py
│   ├── sources/
│   │   ├── registry.py            # trusted-source registry + confidence scoring
│   │   ├── official_site.py
│   │   ├── wiki_adapter.py
│   │   └── json_repo_adapter.py
│   ├── pipeline/
│   │   ├── validator.py           # Pydantic schemas
│   │   ├── normalizer.py
│   │   ├── change_detector.py
│   │   ├── db_generator.py
│   │   └── image_pipeline.py
│   ├── ai_parsing/
│   │   ├── interfaces.py
│   │   ├── patch_note_summarizer.py
│   │   └── fallback_parser.py
│   ├── orchestrator.py            # entrypoint: runs full pipeline
│   ├── config.py
│   ├── logging_config.py
│   └── requirements.txt
│
├── database-schemas/              # canonical JSON Schema / Pydantic-derived
│   ├── character.schema.json
│   ├── weapon.schema.json
│   ├── echo.schema.json
│   ├── material.schema.json
│   ├── version.schema.json
│   └── manifest.schema.json
│
├── .github/workflows/
│   ├── scrape-and-publish.yml
│   ├── android-ci.yml
│   └── backend-tests.yml
│
└── docs/
    ├── ARCHITECTURE.md            # this file
    ├── API_CONTRACTS.md
    ├── DEPLOYMENT.md
    └── SECURITY.md
```

---

## 3. Data Model (canonical entities)

All entities share a common envelope so the Android app can generically diff/migrate them:

```json
{
  "id": "string (stable slug, e.g. 'jinhsi')",
  "schema_version": 1,
  "source_confidence": 0.0-1.0,
  "last_verified": "ISO-8601 timestamp",
  "data": { "...entity-specific fields..." }
}
```

Example — `character.schema.json` (abridged):

```json
{
  "id": "jinhsi",
  "name": "Jinhsi",
  "rarity": 5,
  "element": "Spectro",
  "weapon_type": "Broadsword",
  "region": "Rinascita",
  "release_date": "2024-07-19",
  "stats_curve_ref": "curves/jinhsi_stats.json",
  "skills": [
    {"type": "normal_attack", "name": "...", "description": "..."},
    {"type": "resonance_skill", "name": "...", "description": "..."},
    {"type": "forte_circuit", "name": "...", "description": "..."},
    {"type": "resonance_liberation", "name": "...", "description": "..."},
    {"type": "intro_outro", "name": "...", "description": "..."}
  ],
  "ascension_materials": ["material_id_1", "material_id_2"],
  "recommended_echoes": ["echo_id_1"],
  "recommended_weapons": ["weapon_id_1"],
  "assets": {
    "icon": "assets/characters/jinhsi/icon.webp",
    "splash": "assets/characters/jinhsi/splash.webp",
    "thumb": "assets/characters/jinhsi/thumb_128.webp"
  },
  "tags": ["dps", "spectro", "5-star"]
}
```

Full JSON Schemas for all entity types go in `database-schemas/` and are the single source of truth — the Pydantic models in the backend and the Room entities in Android are both generated/checked against these, so they never drift apart.

---

## 4. Update / Version Manifest Contract

`version.json` (published at the root of each GitHub Release, and mirrored at a stable "latest" URL):

```json
{
  "manifest_version": 3,
  "generated_at": "2026-07-02T10:00:00Z",
  "database": {
    "version": "1.4.2",
    "full_package_url": "https://github.com/<org>/<repo>/releases/download/v1.4.2/database-full.zip",
    "full_package_sha256": "…",
    "full_package_size_bytes": 4821932,
    "delta_from": {
      "1.4.1": {
        "url": "https://.../delta-1.4.1-to-1.4.2.zip",
        "sha256": "…",
        "size_bytes": 182044
      }
    }
  },
  "assets": {
    "version": "1.4.2",
    "manifest_url": "https://.../assets/manifest.json"
  },
  "changelog_url": "https://.../CHANGELOG.md",
  "min_supported_app_version": "1.0.0"
}
```

**Android update flow:**
1. `UpdateWorker` (WorkManager, periodic + on-app-open) fetches `version.json`.
2. Compares `database.version` (semver) against local `DataStore`-persisted version.
3. If local version has a matching entry in `delta_from`, downloads the delta package (small); otherwise downloads `full_package_url`.
4. Verifies SHA-256 before touching anything on disk.
5. Applies to a **staging** Room DB file, runs migrations, validates row counts/foreign keys.
6. Atomically renames staging → active DB file. Old DB is kept as `*.bak` for one cycle → enables rollback if the app crashes on next boot (checked via a "boot canary" flag).

---

## 5. Trusted Source & Confidence Scoring

Each source adapter implements:

```python
class SourceAdapter(Protocol):
    source_id: str
    trust_tier: Literal["official", "trusted_wiki", "structured_repo", "community"]
    def fetch(self, entity_type: str) -> list[RawRecord]: ...
    def rate_limit(self) -> RateLimitPolicy: ...
```

Confidence score = weighted function of `trust_tier`, historical accuracy (tracked via manual corrections logged over time), and freshness. Records below a configurable confidence threshold are flagged for manual review rather than auto-published — this is the main defense against a source's layout change silently corrupting data.

**Important scope/legal note:** scraping wikis and official sites must respect `robots.txt`, ToS, and rate limits — this architecture includes the hooks for that (Section 4 of backend: `sources/registry.py`), but I have not built scrapers targeting any specific live site in this phase. That's a deliberate boundary: some wikis explicitly disallow automated scraping in their ToS even when `robots.txt` allows it, so before wiring up a real adapter for a specific site, it's worth checking that site's terms — happy to help review a specific source's policy if you tell me which one you're targeting.

---

## 5b. Two-Decision Governance Model

There are two separate questions in this system, on two separate schedules, and conflating them is what makes update pipelines either too slow or too risky:

**Decision 1 — "Is this source trustworthy at all?"** (one-time, per-source)
Made once when a new `SourceAdapter` is added to `sources/registry.py`. This is a human judgment call about the source itself (see Section 5's trust-tier discussion) — it doesn't repeat every scrape.

**Decision 2 — "Is this specific update safe to ship?"** (recurring, every time content changes)
The scraper runs on its normal cron schedule regardless. When `change_detector.py` finds a real diff against the last published version, the pipeline **stages** a release (builds it, generates a changelog) but does **not** publish it automatically. A human reviews the diff and explicitly approves or rejects. Only an approval triggers the actual GitHub Release + `version.json` bump that the Android app's `UpdateManager` will detect on its next check-in.

```
cron trigger → scrape → validate → diff vs last published
                                        │ changes found
                                        ▼
                         STAGE the release (built, not published)
                                        │
                         reviewer notified, sees changelog diff
                                        │
                    ┌───────────────────┴───────────────────┐
                approve                                   reject
                    │                                        │
        publish release, bump version.json          discard staged build
                    │
        Android app's next UpdateManager check
        sees the new version → "Update available"
```

**Implementation:** GitHub Environments with required reviewers. The `await-review` job in `scrape-and-publish.yml` runs under the `production-database` environment; configure that environment once in repo **Settings → Environments** and add whoever owns content decisions as a required reviewer. GitHub then pauses the workflow at that job and notifies the reviewer, who sees the generated changelog in the job's step summary before approving or rejecting — no extra infrastructure beyond what GitHub Actions already provides. This is deliberately simpler than a custom review dashboard: the diff is just the changelog Markdown the `db_generator` already produces, and the approve/reject buttons are native to Actions.

---

## 6. Next Phases (what I'll build when you say go)

| Phase | Deliverable | Depends on |
|---|---|---|
| 2 | `backend/scrapers/base.py` + one working scraper against a **structured JSON repo** source (safest legal starting point) + Pydantic validators | none |
| 3 | `pipeline/db_generator.py` — turns validated records into `characters.json` + SQLite + `version.json` | Phase 2 |
| 4 | `.github/workflows/scrape-and-publish.yml` — cron job, diff detection, GitHub Release publishing | Phase 3 |
| 5 | Android `core/database` (Room schema matching Section 3) + `core/update` (UpdateManager per Section 4) | Phase 4 (needs a real manifest to test against) |
| 6 | `feature/characters` (list/detail/search/favorites UI) | Phase 5 |
| 7 | Image pipeline (WebP conversion, perceptual-hash dedup, thumbnailing) | Phase 2 |
| 8 | AI patch-note summarizer (Claude API call, structured JSON output) | Phase 4 |

I've started Phase 1 with runnable skeletons for the two riskiest/most foundational pieces so you can see the shape of the code immediately — see `backend/scrapers/base.py`, `.github/workflows/scrape-and-publish.yml`, and `android-app/core/update/UpdateManager.kt` in this delivery.
