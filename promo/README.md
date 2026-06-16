# ETF Compass — Promo Video

15-second product launch promo for the ETF Compass Angular app.

## Preview

### Option A: Open directly in browser (simplest)

Open `index.html` in any modern browser. A control bar appears at the bottom with play/pause, progress bar, and time display. Click **▶** to watch the 15-second animation.

```bash
# Using python (no install needed):
python3 -m http.server 3099 --directory promo/
# Then open http://localhost:3099

# Or just open the file directly:
open promo/index.html
```

### Option B: HyperFrames CLI

```bash
# Install hyperframes
npm install -g hyperframes

# Preview in studio
hyperframes preview promo/

# Render to MP4 (requires FFmpeg)
hyperframes render promo/ --output promo/etf-compass-promo.mp4
```

## Current App 20s HyperFrames Video

The current application ad lives at:

```text
promo/current-app-20s.html
```

It is a HyperFrames composition with:

```text
data-composition-id="current-app-20s"
data-width="1920"
data-height="1080"
data-duration="20"
```

Preview:

```bash
npm run promo:preview
```

Render MP4:

```bash
npm run promo:render
```

## Structure

| Time  | Scene                          |
|-------|--------------------------------|
| 0–3s  | Logo + hero title + subtitle  |
| 3–6s  | ETF cards (VOO, SPY, IVV, IWDA) with metrics |
| 6–9s  | Comparison table with best-value highlights |
| 9–12s | Performance chart + filter chips + watchlist |
| 12–15s| Closing call-to-action + logo |

## Tech

- **HyperFrames** — composition format with `data-composition-id`
- **GSAP 3.12** — timeline with `{ paused: true }` for frame-accurate seeking
- **Plus Jakarta Sans** — Google Font for fintech typography
- 1920×1080, dark fintech aesthetic matching the app's color scheme
