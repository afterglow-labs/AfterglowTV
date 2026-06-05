#!/usr/bin/env python3
"""
Afterglow TV — demo EPG generator.

Reads the public playlist (afterglow_public_live.m3u8), then builds a rolling
XMLTV guide centered on "now" (default: 14 days back, 14 days forward) so the
in-app guide is ALWAYS populated, no matter when the app is opened. Programming
is believable filler, varied per channel theme (derived from group-title).

Usage:
    python3 generate_epg.py [--days-back N] [--days-forward N]

Outputs afterglow_public_live.xml next to the playlist.
"""
import argparse
import html
import os
import random
import re
from datetime import datetime, timedelta, timezone

HERE = os.path.dirname(os.path.abspath(__file__))
M3U = os.path.join(HERE, "afterglow_public_live.m3u8")
OUT = os.path.join(HERE, "afterglow_public_live.xml")

TZ = timezone(timedelta(hours=-5))  # US Central, matches existing -0500 stamps

# Block lengths (minutes) the scheduler picks from, weighted toward 30/60.
DURATIONS = [30, 30, 30, 45, 60, 60, 60, 90, 120]

# ── Themed programming pools, keyed by a normalized group-title ───────────────
# Each entry: (title, description, category). The scheduler also assembles
# day-part-aware blocks (Morning/Afternoon/Prime/Late) for extra variety.
THEMES = {
    "movies": {
        "category": "Movies",
        "dayparts": {
            "morning":  ["Morning Matinee", "Sunrise Cinema", "Early Show Classics"],
            "afternoon": ["Afternoon Feature", "Cinema Encore", "Matinee Double Bill"],
            "prime":    ["Prime Time Premiere", "Featured Film", "Saturday Night Cinema"],
            "late":     ["Late Night Movie", "After Hours Classics", "Midnight Reel"],
        },
        "titles": [
            "Cinema Encore", "Hollywood Golden Age", "Silver Screen Spotlight",
            "Director's Cut", "Foreign Film Showcase", "Western Roundup",
            "Film Noir Theater", "Romance Classics", "Adventure Matinee",
            "Vintage Comedy Hour", "Epic Drama Presentation",
        ],
        "descs": [
            "Classic cinema from the golden age of film.",
            "A timeless feature presentation for film lovers.",
            "Award-winning storytelling from acclaimed directors.",
            "Vintage film programming and classic selections.",
            "A celebrated motion picture, restored and remastered.",
        ],
    },
    "arts": {
        "category": "Arts",
        "dayparts": {
            "morning":  ["Morning Gallery", "Sunrise Sonata", "Studio Sessions"],
            "afternoon": ["Afternoon at the Museum", "Canvas & Color", "Masterworks"],
            "prime":    ["Evening at the Symphony", "Stage & Screen", "Fine Arts Showcase"],
            "late":     ["Nocturne", "Late Night Gallery", "Quiet Hours"],
        },
        "titles": [
            "Classic Arts Showcase", "Ballet in Motion", "Opera Highlights",
            "Modern Sculpture", "Impressionist Masters", "Chamber Music Hour",
            "Photography Today", "Theatre Spotlight", "Jazz on Canvas",
        ],
        "descs": [
            "A curated journey through the visual and performing arts.",
            "Highlights from the world's great galleries and stages.",
            "Fine art, music, and performance, all in one place.",
            "Celebrating creativity across every medium.",
        ],
    },
    "music": {
        "category": "Music",
        "dayparts": {
            "morning":  ["Morning Mix", "Coffeehouse Acoustic", "Wake-Up Sessions"],
            "afternoon": ["Afternoon Grooves", "The Midday Set", "Vinyl Hours"],
            "prime":    ["Prime Time Live", "Concert Series", "Headliners"],
            "late":     ["Late Night Lounge", "After Dark", "Midnight Sessions"],
        },
        "titles": [
            "Boni Records Sessions", "Indie Spotlight", "Soul & Groove",
            "Acoustic Café", "Electronic Frontiers", "Roots & Americana",
            "Live in Studio", "The Mixtape", "Rhythm & Blues Hour",
        ],
        "descs": [
            "Hand-picked tracks and live studio performances.",
            "A curated music block spanning genres and decades.",
            "Discover new artists alongside timeless favorites.",
            "Nonstop music programming for every mood.",
        ],
    },
    "access": {
        "category": "Local",
        "dayparts": {
            "morning":  ["Community Morning", "Local Sunrise", "Town Hall AM"],
            "afternoon": ["Around Town", "Community Spotlight", "Local Voices"],
            "prime":    ["Community Tonight", "Public Forum", "Neighborhood News"],
            "late":     ["Late Local", "Community Bulletin", "After Hours Access"],
        },
        "titles": [
            "Community Bulletin Board", "Local Government Live", "Neighborhood Spotlight",
            "Public Forum", "School District Update", "Arts in the Community",
            "Town Council Meeting", "Local Sports Wrap", "Community Calendar",
        ],
        "descs": [
            "Programming from your local public access channel.",
            "Community events, meetings, and local stories.",
            "Connecting neighbors with local information.",
            "Public affairs and community-produced content.",
        ],
    },
    "weather": {
        "category": "Weather",
        "dayparts": {
            "morning":  ["Morning Forecast", "AM Weather Watch", "Daybreak Outlook"],
            "afternoon": ["Midday Forecast", "Regional Radar", "Afternoon Outlook"],
            "prime":    ["Evening Forecast", "Tonight's Weather", "Storm Watch"],
            "late":     ["Overnight Forecast", "Late Weather Watch", "Extended Outlook"],
        },
        "titles": [
            "Local Forecast", "National Weather Update", "Storm Tracker",
            "7-Day Outlook", "Regional Radar", "Travel Weather",
            "Tropical Update", "Weekend Forecast", "Climate Watch",
        ],
        "descs": [
            "Up-to-the-minute weather for your region and beyond.",
            "Current conditions, radar, and the extended forecast.",
            "Tracking the latest weather across the country.",
            "Your around-the-clock weather authority.",
        ],
    },
    "sample": {
        "category": "Demo",
        "dayparts": {
            "morning":  ["Morning Demo Loop", "Reference Stream AM", "Test Pattern"],
            "afternoon": ["Sample Showcase", "Reference Playback", "Demo Reel"],
            "prime":    ["Featured Sample", "Showcase Stream", "Reference Premiere"],
            "late":     ["Late Demo Loop", "Overnight Reference", "Continuous Playback"],
        },
        "titles": [
            "Reference Stream", "Sample Playback", "Demo Reel",
            "Test Programming", "Continuous Showcase", "Playback Loop",
        ],
        "descs": [
            "A public sample stream for demonstration and testing.",
            "Reference content showcasing playback capabilities.",
            "Continuous demo programming.",
        ],
    },
}


def theme_for_group(group: str) -> str:
    g = (group or "").lower()
    if "movie" in g:
        return "movies"
    if "art" in g:
        return "arts"
    if "music" in g:
        return "music"
    if "weather" in g:
        return "weather"
    if "sample" in g or "test" in g:
        return "sample"
    return "access"  # Public Access + anything else


def parse_m3u(path):
    """Return list of dicts: {id, name, group}."""
    channels = []
    with open(path, encoding="utf-8") as f:
        lines = [l.rstrip("\n") for l in f]
    for line in lines:
        if not line.startswith("#EXTINF"):
            continue
        tvg_id = re.search(r'tvg-id="([^"]*)"', line)
        tvg_name = re.search(r'tvg-name="([^"]*)"', line)
        group = re.search(r'group-title="([^"]*)"', line)
        # display name is the text after the last comma
        disp = line.split(",", 1)[1].strip() if "," in line else ""
        cid = (tvg_id.group(1) if tvg_id else "").strip()
        name = (tvg_name.group(1) if tvg_name else "") or disp
        if not cid:
            # fall back to a slug of the name so the channel still appears
            cid = re.sub(r"[^A-Za-z0-9]+", "", name) or f"channel{len(channels)+1}"
        channels.append({"id": cid, "name": name.strip(), "group": (group.group(1) if group else "").strip()})
    return channels


def daypart(hour: int) -> str:
    if 5 <= hour < 12:
        return "morning"
    if 12 <= hour < 17:
        return "afternoon"
    if 17 <= hour < 23:
        return "prime"
    return "late"


def fmt(dt: datetime) -> str:
    return dt.strftime("%Y%m%d%H%M%S %z")


def gen_programmes(channel, start, end, rng):
    theme = THEMES[theme_for_group(channel["group"])]
    out = []
    cursor = start
    while cursor < end:
        dur = rng.choice(DURATIONS)
        stop = cursor + timedelta(minutes=dur)
        if stop > end:
            stop = end
        dp = daypart(cursor.hour)
        # 50/50 between a day-part-flavored title and a general theme title
        if rng.random() < 0.5:
            title = rng.choice(theme["dayparts"][dp])
        else:
            title = rng.choice(theme["titles"])
        desc = rng.choice(theme["descs"])
        out.append((cursor, stop, title, desc, theme["category"]))
        cursor = stop
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--days-back", type=int, default=14)
    ap.add_argument("--days-forward", type=int, default=14)
    ap.add_argument("--now", default=None, help="override 'now' as YYYYMMDD (testing)")
    args = ap.parse_args()

    if args.now:
        base = datetime.strptime(args.now, "%Y%m%d").replace(tzinfo=TZ)
    else:
        base = datetime.now(TZ)
    # snap to midnight so blocks line up cleanly
    base = base.replace(hour=0, minute=0, second=0, microsecond=0)
    start = base - timedelta(days=args.days_back)
    end = base + timedelta(days=args.days_forward)

    channels = parse_m3u(M3U)
    if not channels:
        raise SystemExit("No channels parsed from m3u8")

    parts = []
    parts.append('<?xml version="1.0" encoding="UTF-8"?>')
    parts.append('<tv generator-info-name="AfterglowTV Public Guide" source-info-name="AfterglowTV">')

    for ch in channels:
        parts.append(f'  <channel id="{html.escape(ch["id"])}">')
        parts.append(f'    <display-name>{html.escape(ch["name"])}</display-name>')
        parts.append('  </channel>')

    total = 0
    for ch in channels:
        # per-channel seed -> stable but different per channel
        rng = random.Random(f'{ch["id"]}|{start.date()}|{end.date()}')
        for (s, e, title, desc, cat) in gen_programmes(ch, start, end, rng):
            parts.append(
                f'  <programme start="{fmt(s)}" stop="{fmt(e)}" channel="{html.escape(ch["id"])}">'
            )
            parts.append(f'    <title>{html.escape(title)}</title>')
            parts.append(f'    <desc>{html.escape(desc)}</desc>')
            parts.append(f'    <category>{html.escape(cat)}</category>')
            parts.append('  </programme>')
            total += 1

    parts.append('</tv>')
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(parts) + "\n")

    print(f"Wrote {OUT}")
    print(f"  channels:   {len(channels)}")
    print(f"  programmes: {total}")
    print(f"  window:     {start.date()} -> {end.date()}")


if __name__ == "__main__":
    main()
