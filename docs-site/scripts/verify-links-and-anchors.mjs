#!/usr/bin/env node
// Plain Node verification for internal-link integrity across docs-site
// (spec-legal-disclaimers-page.md finding #5). No new test
// framework/dependency -- walks the real markdown source files and the
// real `.vitepress/config.ts` (imported directly, the same way VitePress
// itself would resolve it) rather than re-deriving their content by hand.
//
// Why this exists: `legal.md` and `license.md` cross-link each other via
// heading anchors (e.g. `/license#third-party-attribution-...`), and
// VitePress auto-generates a heading's anchor id from its text unless an
// explicit `{#id}` override is given. If a heading's wording is ever
// edited without updating (or adding) a matching `{#id}`, the
// auto-generated slug silently changes and any link pointing at the old
// slug breaks -- and `vitepress build`'s own dead-link checker strips URL
// fragments before checking, so a broken anchor still produces a clean,
// passing build. Same risk shape for `config.ts`'s `nav`/`sidebar` arrays:
// a typo'd or stale `link` value points at a page that doesn't exist, and
// nothing in the normal build catches it. This script closes both gaps by
// checking the two things VitePress itself does not:
//   (a) every internal `[text](/path#anchor)` (or same-page `[text](#anchor)`)
//       link's anchor resolves to a real heading id -- explicit `{#id}` or
//       VitePress's own auto-generated slug -- on its target page.
//   (b) every `nav`/`sidebar` `link` value in `config.ts` resolves to an
//       actual page file.
//
// Run directly: `node docs-site/scripts/verify-links-and-anchors.mjs`
// Exits non-zero if any assertion fails.

import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DOCS_ROOT = path.resolve(__dirname, '..');

let failures = 0;

function check(label, condition) {
  if (!condition) {
    failures++;
    console.error(`FAIL: ${label}`);
  } else {
    console.log(`PASS: ${label}`);
  }
}

// --- Discover every markdown source file under docs-site, excluding
// build output/deps/theme-component dirs that never contain page content. ---

const EXCLUDED_DIRS = new Set(['node_modules', 'dist', 'cache', '.vitepress']);

function findMarkdownFiles(dir, out = []) {
  for (const entry of readdirSync(dir)) {
    if (EXCLUDED_DIRS.has(entry)) continue;
    const full = path.join(dir, entry);
    const stat = statSync(full);
    if (stat.isDirectory()) {
      findMarkdownFiles(full, out);
    } else if (entry.endsWith('.md')) {
      out.push(full);
    }
  }
  return out;
}

// .vitepress/config.ts has its own `.md`-free content but its dead-simple
// pages (e.g. VitePress's default theme has no other .md sources outside
// the excluded dirs), so a plain recursive walk from DOCS_ROOT is enough --
// except .vitepress itself is excluded above (it holds config/theme code,
// not page content), so re-add just its would-be page dirs if any existed.
const markdownFiles = findMarkdownFiles(DOCS_ROOT);

// --- Route mapping: mirror VitePress's own file->URL rule for this site
// (`cleanUrls: true`, no custom `base`, no custom rewrites). ---

function fileToRoute(absFile) {
  const rel = path.relative(DOCS_ROOT, absFile).split(path.sep).join('/');
  if (rel === 'index.md') return '/';
  if (rel.endsWith('/index.md')) return '/' + rel.slice(0, -'index.md'.length);
  return '/' + rel.slice(0, -'.md'.length);
}

function routeToFile(route) {
  // Strip any anchor/query defensively (callers already split these off,
  // this is just a safety net).
  let r = route.split('#')[0].split('?')[0];
  if (r === '' || r === '/') return path.join(DOCS_ROOT, 'index.md');
  if (r.endsWith('/')) return path.join(DOCS_ROOT, r.slice(1), 'index.md');
  const direct = path.join(DOCS_ROOT, r.replace(/^\//, '') + '.md');
  if (existsSync(direct)) return direct;
  const asIndex = path.join(DOCS_ROOT, r.replace(/^\//, ''), 'index.md');
  if (existsSync(asIndex)) return asIndex;
  return direct; // doesn't exist -- caller reports this as a broken link
}

// --- VitePress's own heading-slug algorithm (mirrored exactly from
// `vitepress/dist/node` so an auto-generated slug computed here matches
// what a real `vitepress build` would produce), plus its `{#id}` override
// syntax. ---

const rControl = new RegExp('[\\u0000-\\u001f]', 'g');
const rSpecial = /[\s~`!@#$%^&*()\-_+=[\]{}|\\;:"'“”‘’<>,.?/]+/g;
const rCombining = new RegExp('[\\u0300-\\u036F]', 'g');

function slugify(str) {
  return str
    .normalize('NFKD')
    .replace(rCombining, '')
    .replace(rControl, '')
    .replace(rSpecial, '-')
    .replace(/-{2,}/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/^(\d)/, '_$1')
    .toLowerCase();
}

const HEADING_RE = /^#{1,6}\s+(.+?)\s*$/;
const EXPLICIT_ID_RE = /\s*\{#([A-Za-z0-9_-]+)\}\s*$/;
const INLINE_CODE_RE = /`([^`]*)`/g;

function headingIdsForFile(absFile) {
  const content = readFileSync(absFile, 'utf8');
  // Fenced code blocks can contain lines starting with "#" (e.g. shell
  // comments in a ```bash block) that are not markdown headings -- skip
  // their content so we never mis-detect one as a heading.
  const lines = content.split('\n');
  const ids = new Set();
  const usedSlugs = {}; // VitePress dedupes auto-generated slug collisions with -1, -2, ...
  let inFence = false;
  for (const line of lines) {
    if (/^\s*```/.test(line)) {
      inFence = !inFence;
      continue;
    }
    if (inFence) continue;
    const m = HEADING_RE.exec(line);
    if (!m) continue;
    let text = m[1];
    let explicitId = null;
    const idMatch = EXPLICIT_ID_RE.exec(text);
    if (idMatch) {
      explicitId = idMatch[1];
      text = text.slice(0, idMatch.index);
    }
    if (explicitId) {
      ids.add(explicitId);
      continue;
    }
    // VitePress's slugify runs on the heading's rendered text -- strip
    // inline-code backticks and markdown link/emphasis markers so
    // e.g. "## The `UserAgentInfo` model" slugifies from "The UserAgentInfo
    // model" text content, matching markdown-it's token-text extraction.
    const plain = text
      .replace(INLINE_CODE_RE, '$1')
      .replace(/\*\*([^*]+)\*\*/g, '$1')
      .replace(/\*([^*]+)\*/g, '$1')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');
    let slug = slugify(plain);
    if (usedSlugs[slug] !== undefined) {
      usedSlugs[slug] += 1;
      slug = `${slug}-${usedSlugs[slug]}`;
    } else {
      usedSlugs[slug] = 0;
    }
    ids.add(slug);
  }
  return ids;
}

const headingIdsByRoute = new Map();
for (const file of markdownFiles) {
  headingIdsByRoute.set(fileToRoute(file), headingIdsForFile(file));
}

// --- (a) Every internal markdown link's anchor must resolve to a real
// heading id on its target page. ---

// Matches `[text](/some/path#anchor)`, `[text](/some/path)` (no anchor --
// not checked here, only that the target page exists is implied by
// routeToFile below), and same-page `[text](#anchor)`. Deliberately
// excludes `http(s)://...`, `mailto:`, and other external/absolute-URL
// links -- those are out of scope for this check.
const LINK_RE = /\[([^\]]*)\]\((\/[^)\s#]*|)(#[^)\s]+)?\)/g;

for (const file of markdownFiles) {
  const route = fileToRoute(file);
  const content = readFileSync(file, 'utf8');
  let match;
  LINK_RE.lastIndex = 0;
  while ((match = LINK_RE.exec(content))) {
    const [, linkText, linkPath, anchorRaw] = match;
    if (!anchorRaw) continue; // no anchor to validate
    if (!linkPath && anchorRaw === '#') continue; // defensive: empty anchor
    const anchor = anchorRaw.slice(1); // drop leading '#'
    if (!anchor) continue;
    const targetRoute = linkPath || route; // empty path -> same-page link
    const targetFile = routeToFile(targetRoute);
    const targetRouteResolved = fileToRoute(targetFile);
    const ids = headingIdsByRoute.get(targetRouteResolved);
    const label = `${path.relative(DOCS_ROOT, file)}: [${linkText}](${linkPath}#${anchor}) -> ${targetRouteResolved}#${anchor}`;
    if (!ids) {
      check(`${label} (target page exists)`, false);
      continue;
    }
    check(label, ids.has(anchor));
  }
}

// --- (b) Every nav/sidebar `link` in config.ts resolves to an existing
// page file. Imported directly (the same module a real `vitepress build`
// loads) rather than re-parsed by hand, so this check tracks the config's
// actual resolved shape, including nested `items` groups. ---

const configModule = await import(path.join(DOCS_ROOT, '.vitepress', 'config.ts'));
const themeConfig = configModule.default?.themeConfig ?? {};

function collectLinks(nodes, out = []) {
  if (!Array.isArray(nodes)) return out;
  for (const node of nodes) {
    if (node && typeof node.link === 'string') out.push(node.link);
    if (node && Array.isArray(node.items)) collectLinks(node.items, out);
  }
  return out;
}

const navLinks = collectLinks(themeConfig.nav);
const sidebarGroups = Array.isArray(themeConfig.sidebar)
  ? themeConfig.sidebar
  : Object.values(themeConfig.sidebar ?? {}).flat();
const sidebarLinks = collectLinks(sidebarGroups);

for (const [source, links] of [
  ['nav', navLinks],
  ['sidebar', sidebarLinks],
]) {
  check(`config.ts themeConfig.${source} has at least one link`, links.length > 0);
  for (const link of links) {
    // Nav/sidebar links are internal page paths, optionally with an anchor
    // (none currently are, but handle it defensively the same way as (a)).
    const [linkPath, anchor] = link.split('#');
    const targetFile = routeToFile(linkPath);
    const exists = existsSync(targetFile);
    check(`config.ts themeConfig.${source} link "${link}" resolves to an existing page`, exists);
    if (exists && anchor) {
      const ids = headingIdsByRoute.get(fileToRoute(targetFile));
      check(`config.ts themeConfig.${source} link "${link}" anchor resolves to a real heading`, !!ids && ids.has(anchor));
    }
  }
}

console.log('');
if (failures > 0) {
  console.error(`${failures} CHECK(S) FAILED`);
  process.exit(1);
} else {
  console.log('ALL CHECKS PASSED');
  process.exit(0);
}
