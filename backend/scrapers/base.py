"""
backend/scrapers/base.py

Base interfaces for the scraper framework. Every entity scraper (characters,
weapons, echoes, ...) subclasses BaseScraper and only implements the
source-specific extraction logic — retry, logging, validation hand-off,
and change detection are handled once, here.
"""

from __future__ import annotations

import abc
import hashlib
import logging
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Literal

from pydantic import BaseModel, ValidationError

logger = logging.getLogger("wuwa.scrapers")


# --------------------------------------------------------------------------
# Shared data contracts
# --------------------------------------------------------------------------

TrustTier = Literal["official", "trusted_wiki", "structured_repo", "community"]


@dataclass
class RateLimitPolicy:
    min_interval_seconds: float = 2.0
    max_retries: int = 3
    backoff_factor: float = 2.0


@dataclass
class RawRecord:
    """A single scraped record before validation/normalization."""
    source_id: str
    entity_type: str
    raw_payload: dict[str, Any]
    fetched_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    def content_hash(self) -> str:
        """Stable hash of the payload, used for change detection."""
        import json
        blob = json.dumps(self.raw_payload, sort_keys=True, default=str)
        return hashlib.sha256(blob.encode("utf-8")).hexdigest()


class ScrapeResult(BaseModel):
    entity_type: str
    source_id: str
    records_fetched: int
    records_valid: int
    records_rejected: int
    duration_seconds: float
    errors: list[str] = []


# --------------------------------------------------------------------------
# Base scraper
# --------------------------------------------------------------------------

class BaseScraper(abc.ABC):
    """
    Subclass this for each entity type (characters, weapons, echoes, ...).

    Required overrides:
      - entity_type: str
      - trust_tier: TrustTier
      - fetch_raw() -> list[RawRecord]
      - validate(record: RawRecord) -> BaseModel | None
          Return a validated Pydantic model, or None to reject the record.

    Everything else (retries, rate limiting, logging, result aggregation)
    is handled by run().
    """

    entity_type: str
    trust_tier: TrustTier
    rate_limit: RateLimitPolicy = RateLimitPolicy()

    def __init__(self, source_id: str):
        self.source_id = source_id
        self._log = logger.getChild(f"{source_id}.{self.entity_type}")

    @abc.abstractmethod
    def fetch_raw(self) -> list[RawRecord]:
        """Pull raw records from the source. Must implement its own
        pagination/traversal. Raise on hard failure; return [] on
        'no new data' (not an error)."""
        raise NotImplementedError

    @abc.abstractmethod
    def validate(self, record: RawRecord) -> BaseModel | None:
        """Validate + coerce a raw record into a typed Pydantic model.
        Return None (and log a warning) to reject a malformed record
        without failing the whole run."""
        raise NotImplementedError

    def run(self) -> tuple[ScrapeResult, list[BaseModel]]:
        start = time.monotonic()
        errors: list[str] = []
        valid_records: list[BaseModel] = []

        raw_records = self._fetch_with_retry()

        for raw in raw_records:
            try:
                validated = self.validate(raw)
                if validated is not None:
                    valid_records.append(validated)
                else:
                    errors.append(f"rejected record (validate() returned None): "
                                  f"hash={raw.content_hash()[:12]}")
            except ValidationError as e:
                errors.append(f"validation error: {e}")
                self._log.warning("Validation failed for record: %s", e)

        result = ScrapeResult(
            entity_type=self.entity_type,
            source_id=self.source_id,
            records_fetched=len(raw_records),
            records_valid=len(valid_records),
            records_rejected=len(raw_records) - len(valid_records),
            duration_seconds=round(time.monotonic() - start, 3),
            errors=errors[:50],  # cap so logs/reports stay readable
        )
        self._log.info(
            "Scrape complete: %s/%s valid in %.2fs",
            result.records_valid, result.records_fetched, result.duration_seconds,
        )
        return result, valid_records

    def _fetch_with_retry(self) -> list[RawRecord]:
        last_exc: Exception | None = None
        for attempt in range(1, self.rate_limit.max_retries + 1):
            try:
                return self.fetch_raw()
            except Exception as exc:  # noqa: BLE001 - deliberately broad, we retry
                last_exc = exc
                wait = self.rate_limit.min_interval_seconds * (
                    self.rate_limit.backoff_factor ** (attempt - 1)
                )
                self._log.warning(
                    "fetch_raw() attempt %s/%s failed (%s); retrying in %.1fs",
                    attempt, self.rate_limit.max_retries, exc, wait,
                )
                time.sleep(wait)
        self._log.error("fetch_raw() failed after %s attempts", self.rate_limit.max_retries)
        raise RuntimeError(f"Scrape failed for {self.source_id}/{self.entity_type}") from last_exc
