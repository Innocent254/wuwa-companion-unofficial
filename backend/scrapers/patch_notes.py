"""
backend/scrapers/patch_notes.py

Concrete scraper for patch notes / version-preview / banner-announcement
posts from the official site. This is what the change_detector compares
against to decide whether a "banner changed" or "version changed" event
has happened — which is what ultimately triggers the staged-release +
reviewer-approval flow in .github/workflows/scrape-and-publish.yml.
"""

from __future__ import annotations

import asyncio
import re
from datetime import datetime, timezone

from playwright.async_api import async_playwright
from pydantic import BaseModel, Field, field_validator

from backend.scrapers.base import BaseScraper, RawRecord, TrustTier
from backend.sources.official_site import OfficialSiteAdapter, NewsPost, open_page


class PatchNoteRecord(BaseModel):
    """Validated shape for a single patch-note / banner-announcement post.
    Matches the entity envelope in database-schemas/ — see ARCHITECTURE.md
    Section 3 for the shared 'id / schema_version / source_confidence /
    last_verified / data' wrapper this gets embedded into downstream."""

    post_id: int
    title: str = Field(min_length=1)
    category: str  # "patch_notes" | "banner" | "version_preview" | "general"
    version_tag: str | None = None  # e.g. "3.4" if detected in the title
    source_url: str
    fetched_at: datetime

    @field_validator("title")
    @classmethod
    def title_not_boilerplate(cls, v: str) -> str:
        # The site's own nav/footer text sometimes leaks into a bad selector
        # match — reject anything that's obviously not a real post title.
        if v.strip().lower() in {"wuthering waves official website", ""}:
            raise ValueError("title looks like site boilerplate, not a real post")
        return v


class PatchNotesScraper(BaseScraper):
    entity_type = "patch_notes"
    trust_tier: TrustTier = "official"

    def __init__(self, source_id: str = "kurogames_official", max_posts: int = 20):
        super().__init__(source_id)
        self.max_posts = max_posts
        self._adapter = OfficialSiteAdapter()

    def fetch_raw(self) -> list[RawRecord]:
        # BaseScraper's interface is sync (keeps subclasses simple); Playwright
        # is async, so we bridge once here rather than making every scraper
        # subclass deal with an event loop.
        return asyncio.run(self._fetch_raw_async())

    async def _fetch_raw_async(self) -> list[RawRecord]:
        raw_records: list[RawRecord] = []
        async with async_playwright() as pw:
            browser, context, page = await open_page(pw)
            try:
                post_ids = await self._adapter.list_recent_news_ids(page, max_items=self.max_posts)
                for post_id in post_ids:
                    post = await self._adapter.fetch_news_detail(page, post_id)
                    if post is None:
                        continue  # already logged inside the adapter
                    raw_records.append(
                        RawRecord(
                            source_id=self.source_id,
                            entity_type=self.entity_type,
                            raw_payload=self._post_to_payload(post),
                        )
                    )
            finally:
                await context.close()
                await browser.close()
        return raw_records

    @staticmethod
    def _post_to_payload(post: NewsPost) -> dict:
        return {
            "post_id": post.post_id,
            "title": post.title,
            "body_text": post.body_text,
            "source_url": post.source_url,
        }

    def validate(self, record: RawRecord) -> PatchNoteRecord | None:
        payload = record.raw_payload
        title = payload.get("title", "")

        category = self._classify(title)
        version_tag = self._extract_version(title)

        try:
            return PatchNoteRecord(
                post_id=payload["post_id"],
                title=title,
                category=category,
                version_tag=version_tag,
                source_url=payload["source_url"],
                fetched_at=record.fetched_at,
            )
        except Exception as exc:  # noqa: BLE001 - surfaced to caller as a rejection, not a crash
            self._log.warning("Rejected post %s: %s", payload.get("post_id"), exc)
            return None

    @staticmethod
    def _classify(title: str) -> str:
        t = title.lower()
        if "patch notes" in t or "update maintenance" in t:
            return "patch_notes"
        if "convene" in t or "banner" in t:
            return "banner"
        if "version preview" in t or "scheduled for release" in t:
            return "version_preview"
        return "general"

    @staticmethod
    def _extract_version(title: str) -> str | None:
        match = re.search(r"[Vv]ersion\s+(\d+\.\d+)", title)
        return match.group(1) if match else None


if __name__ == "__main__":
    # Manual smoke test: python -m backend.scrapers.patch_notes
    import logging
    logging.basicConfig(level=logging.INFO)
    scraper = PatchNotesScraper(max_posts=5)
    result, records = scraper.run()
    print(result.model_dump_json(indent=2))
    for r in records:
        print(r.category, "|", r.version_tag, "|", r.title)
