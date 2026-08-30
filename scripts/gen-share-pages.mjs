// Generates static share pages (s/<track-id>/index.html) with per-track
// Open Graph tags so shared links unfurl as rich embeds on Facebook & co.
// Crawlers read the OG tags; humans get redirected to the player deep link.
// Runs in the Pages deploy workflow — output is deployed, never committed.
import { mkdir, writeFile } from 'node:fs/promises';

const SITE = 'https://mickeyperry.github.io/radiovoid-bbs/';
const BASE = 'https://uhcxcnhsujsawgzfaeae.supabase.co';
const KEY = 'sb_publishable_oRCO4ISFS51ebU3-rpyxew_U_ktpJSs';

const MOD_RE = /\.(mod|xm|s3m|it|mptm|mtm|umx|mo3|669|med|far|okt|ptm|stm|ult|dbm|dmf|dsm|gdm|imf|j2b|mdl|psm|amf|ams|nst|wow|digi|dtm)$/i;

const esc = s => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;')
  .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const fmt = d => d ? `${Math.floor(d / 60)}:${String(Math.floor(d % 60)).padStart(2, '0')}` : '';

const res = await fetch(`${BASE}/rest/v1/tracks?select=*&order=created_at.desc`, {
  headers: { apikey: KEY, Authorization: `Bearer ${KEY}` },
});
if (!res.ok) throw new Error(`tracks fetch failed: HTTP ${res.status}`);
const tracks = await res.json();

let n = 0;
for (const t of tracks) {
  if (!t.id) continue;
  const isMod = MOD_RE.test(t.filename || '') || MOD_RE.test((t.url || '').split('?')[0]);
  const title = `${t.title || 'Untitled'}${t.artist && t.artist !== 'Unknown' ? ` — ${t.artist}` : ''}`;
  const bits = [fmt(t.duration), isMod ? 'TRACKER MODULE' : (t.filename || '').split('.').pop()?.toUpperCase(), t.year, t.bpm ? `${t.bpm} BPM` : '']
    .filter(Boolean).join(' · ');
  const desc = `${bits ? `▶ ${bits} · ` : ''}Listen free on RADIOVOID BBS — underground retro music terminal.`;
  const pageUrl = `${SITE}s/${t.id}/`;
  const playUrl = `${SITE}?play=${t.id}`;
  // og:audio only for formats browsers can stream directly.
  const audioTag = !isMod && t.url
    ? `<meta property="og:audio" content="${esc(t.url)}">\n<meta property="og:audio:secure_url" content="${esc(t.url)}">\n<meta property="og:audio:type" content="audio/mpeg">`
    : '';
  const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>${esc(title)} · RADIOVOID BBS</title>
<meta property="og:site_name" content="RADIOVOID BBS">
<meta property="og:type" content="music.song">
<meta property="og:title" content="${esc(title)}">
<meta property="og:description" content="${esc(desc)}">
<meta property="og:url" content="${esc(pageUrl)}">
<meta property="og:image" content="${SITE}share-card.png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
${audioTag}
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${esc(title)}">
<meta name="twitter:description" content="${esc(desc)}">
<meta name="twitter:image" content="${SITE}share-card.png">
<meta name="description" content="${esc(desc)}">
<meta http-equiv="refresh" content="0;url=${esc(playUrl)}">
<style>body{background:#000810;color:#00aaff;font-family:monospace;text-align:center;padding-top:20vh}a{color:#66ccff}</style>
</head>
<body>
<p>[ R A D I O V O I D ]</p>
<p><a href="${esc(playUrl)}">▶ ${esc(title)}</a></p>
<script>location.replace(${JSON.stringify(playUrl)});</script>
</body>
</html>
`;
  await mkdir(`s/${t.id}`, { recursive: true });
  await writeFile(`s/${t.id}/index.html`, html);
  n++;
}
console.log(`generated ${n} share pages`);
