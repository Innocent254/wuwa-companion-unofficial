"""
backend/sources/official_site.py

Adapter for wutheringwaves.kurogames.com — Kuro Games' own site, the
one 'official' trust-tier source that doesn't carry third-party ToS
risk (see docs/ARCHITECTURE.md, Section 5).

The site is a client-rendered SPA (server returns only <meta> tags,
content is injected by JS), so this adapter uses Playwright rather
than requests/BeautifulSoup. Two entry points are implemented:

  - list_news(limit)      -> discovers recent news/patch-note post IDs
  - fetch_news_detail(id) -> renders a single post and extracts content

Both are intentionally conservative: rate-limited, retried, and built
to degrade (log + skip) rather than crash on a single malformed page,
since a layout tweak on Kuro's end should not take down the whole run.
"""

from __future__ import annotations

import asyncio
import logging
import re
from dataclasses import dataclass
from datetime import datetime, timezone

from playwright.async_api import async_playwright, Page, TimeoutError as PlaywrightTimeout

logger = logging.getLogger("wuwa.sources.official_site")

BASE_URL = "https://wutheringwaves.kurogames.com/en/main"
NEWS_LIST_URL = f"{BASE_URL}/news"
NEWS_DETAIL_URL = f"{BASE_URL}/news/detail/{{post_id}}"

# Be polite: this is Kuro's own infrastructure, serving real players too.
MIN_REQUEST_INTERVAL_SECONDS = 3.0
NAVIGATION_TIMEOUT_MS = 15_000


@dataclass
class NewsPost:
    post_id: int
    title: str
    published_at: datetime | None
    body_text: str
    body_html: str
    source_url: str


class OfficialSiteAdapter:
    """
    Thin, purpose-built wrapper around Playwright for this one site.
    Not a general-purpose scraping framework — that's what BaseScraper
    subclasses are for. This class only knows how to talk to
    wutheringwaves.kurogames.com.
    """

    trust_tier = "official"

    def __init__(self):
        self._last_request_at: float = 0.0

    async def _throttle(self):
        now = asyncio.get_event_loop().time()
        elapsed = now - self._last_request_at
        if elapsed < MIN_REQUEST_INTERVAL_SECONDS:
            await asyncio.sleep(MIN_REQUEST_INTERVAL_SECONDS - elapsed)
        self._last_request_at = asyncio.get_event_loop().time()

    async def list_recent_news_ids(self, page: Page, max_items: int = 30) -> list[int]:
        """
        Loads the news listing page and scrolls/waits for the client-side
        render, then extracts detail-page IDs from anchor hrefs matching
        /news/detail/<id>. Returns most-recent-first, deduplicated.
        """
        await self._throttle()
        await page.goto(NEWS_LIST_URL, timeout=NAVIGATION_TIMEOUT_MS, wait_until="networkidle")

        try:
            await page.wait_for_selector("a[href*='/news/detail/']", timeout=NAVIGATION_TIMEOUT_MS)
        except PlaywrightTimeout:
            logger.warning("News list did not render any detail links within timeout — "
                            "site layout may have changed; skipping this run rather than "
                            "guessing at a new selector.")
            return []

        hrefs = await page.eval_on_selector_all(
            "a[href*='/news/detail/']",
            "elements => elements.map(e => e.getAttribute('href'))",
        )

        ids: list[int] = []
        seen: set[int] = set()
        for href in hrefs:
            match = re.search(r"/news/detail/(\d+)", href or "")
            if not match:
                continue
            post_id = int(match.group(1))
            if post_id not in seen:
                seen.add(post_id)
                ids.append(post_id)

        return ids[:max_items]

    async def fetch_news_detail(self, page: Page, post_id: int) -> NewsPost | None:
        """Renders one detail page and extracts title/body. Returns None
        (rather than raising) on a page that doesn't match the expected
        shape, so one bad post doesn't fail the whole scrape run."""
        await self._throttle()
        url = NEWS_DETAIL_URL.format(post_id=post_id)
        await page.goto(url, timeout=NAVIGATION_TIMEOUT_MS, wait_until="networkidle")

        try:
            await page.wait_for_selector("article, .news-detail, .detail-content", timeout=NAVIGATION_TIMEOUT_MS)
        except PlaywrightTimeout:
            logger.warning("Detail page %s did not render expected content container; skipping.", post_id)
            return None

        title = await self._first_text(page, ["h1", ".news-detail-title", ".detail-title"])
        body_html = await self._first_html(page, ["article", ".news-detail-content", ".detail-content"])
        body_text = await self._first_text(page, ["article", ".news-detail-content", ".detail-content"])

        if not title or not body_text:
            logger.warning("Detail page %s missing title or body after render; skipping.", post_id)
            return None

        return NewsPost(
            post_id=post_id,
            title=title.strip(),
            published_at=None,  # populate once the date-element selector is confirmed against a live page
            body_text=body_text.strip(),
            body_html=body_html or "",
            source_url=url,
        )

    @staticmethod
    async def _first_text(page: Page, selectors: list[str]) -> str | None:
        for sel in selectors:
            el = await page.query_selector(sel)
            if el:
                text = await el.inner_text()
                if text and text.strip():
                    return text
        return None

    @staticmethod
    async def _first_html(page: Page, selectors: list[str]) -> str | None:
        for sel in selectors:
            el = await page.query_selector(sel)
            if el:
                html = await el.inner_html()
                if html and html.strip():
                    return html
        return None


async def open_page(playwright) -> tuple:
    """Launches a single shared browser/context for a scrape run. Callers
    are responsible for closing what this returns (browser, context, page)."""
    browser = await playwright.chromium.launch(headless=True)
    context = await browser.new_context(
        user_agent=(
            "Mozilla/5.0 (compatible; WuWaCompanionBot/1.0; "
            "+https://github.com/<org>/<repo>#trusted-source-policy)"
        )
    )
    page = await context.new_page()
    return browser, context, page


async def _demo():
    """Manual smoke-test entry point: `python -m backend.sources.official_site`."""
    logging.basicConfig(level=logging.INFO)
    adapter = OfficialSiteAdapter()
    async with async_playwright() as pw:
        browser, context, page = await open_page(pw)
        try:
            ids = await adapter.list_recent_news_ids(page, max_items=5)
            logger.info("Found news post IDs: %s", ids)
            for post_id in ids[:2]:
                post = await adapter.fetch_news_detail(page, post_id)
                if post:
                    logger.info("Fetched: [%s] %s", post.post_id, post.title)
        finally:
            await context.close()
            await browser.close()


if __name__ == "__main__":
    asyncio.run(_demo())
