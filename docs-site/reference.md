# Type Reference

Every name each of the library's six built-in `UserAgentTypePack`s can
produce, taken directly from the library's own source rather than
hand-transcribed prose. Last refreshed 2026-09-07.

**A note on freshness -- read this before the Bots/AI agents tables below.**
Every other section on this page (Engines, Browsers, Operating systems,
Device brands) is accurate against the currently published
`site.lempert:user-agent`/`@lempert/user-agent` package, pinned at `0.2.0`
across this whole site (the same version every platform guide installs, and
the one this site's own live demos run against). The
[Bots](#bots)/[AI agents](#ai-agents) tables are different: they reflect the
library's current source, which is **ahead of what's published**. Per the
[CHANGELOG](https://github.com/ido-lempert/kmp-user-agent/blob/master/CHANGELOG.md),
`0.2.0` shipped the `bot`/`aiAgent` fields but no pack that populates
them; `UserAgentBotTypes`/`UserAgentAIAgentTypes` exist only in an
unreleased `0.3.0`, and the broader roster shown below only in an also
unreleased `0.4.0`. **If you install `0.2.0` today, `info.bot` and
`info.aiAgent` will always be `null`** -- this page documents what those
packs will detect once a release that includes them is actually published,
not what you get today.

This page is otherwise a manually-refreshed snapshot, not generated at
build time -- the same convention this site already uses for pinned
dependency versions. The [Browsers](#browsers), [Operating systems](#operating-systems),
and [Device brands](#device-brands) lists were extracted from the Kotlin
rule tables the `:library:generateUserAgentRules` Gradle task compiles from
the vendored
[`uap-core/regexes.yaml`](https://github.com/ido-lempert/kmp-user-agent/blob/master/library/vendor/uap-core/regexes.yaml)
(see [License](/license) for that attribution) -- to refresh them, run that
task and re-extract the `familyReplacement`/`osReplacement`/
`brandReplacement` string literals from its generated output. The
[Bots](#bots)/[AI agents](#ai-agents)/[Engines](#engines) tables are
hand-authored source of truth (see their KDoc for citations) and only
change when this page is refreshed alongside a real library change.

## Bots

`UserAgentBotTypes` -- 26 entries, first match wins, table order:

| Name | Operator | Version |
| --- | --- | --- |
| Googlebot | Google | Required |
| Bingbot | Microsoft | Required |
| DuckDuckBot | DuckDuckGo | Required |
| YandexBot | Yandex | Optional |
| Baiduspider | Baidu | Required |
| Bytespider | ByteDance | None |
| UptimeRobot | UptimeRobot | Required |
| Pingdom | Pingdom (SolarWinds) | Required |
| StatusCake | StatusCake | None |
| facebookexternalhit | Meta | Required |
| Slackbot | Slack | None |
| PostmanRuntime | Postman | Required |
| AhrefsBot | Ahrefs | Required |
| SemrushBot | Semrush | Required |
| AdsBot-Google | Google | None |
| Mediapartners-Google | Google | Required |
| GoogleOther | Google | None |
| YandexAdditionalBot | Yandex | None |
| MJ12bot | Majestic | Required |
| DotBot | Moz | Required |
| Twitterbot | Twitter/X | None |
| LinkedInBot | LinkedIn | None |
| Discordbot | Discord | None |
| PetalBot | Huawei | None |
| Diffbot | Diffbot | None |
| ImagesiftBot | ImageSift | None |

"Version" is whether the bot's own documented UA token carries a version
number: **Required** means `.bot.version` is always set on a match,
**Optional** means it's set when present in the UA string, **None** means
the bot's token never carries one. The "Name" column is the display name
`.bot.name` is set to, which isn't always identical to the literal token
matched in the UA string (e.g. Bingbot's real token is lowercase
`bingbot`; Pingdom's has no `/` separator before the version) -- see
[`UserAgentBotTypePack.kt`](https://github.com/ido-lempert/kmp-user-agent/blob/master/library/src/commonMain/kotlin/site/lempert/useragent/UserAgentBotTypePack.kt)
for the exact matched pattern per entry before hand-constructing a UA string.

This is an intentionally non-exhaustive starter list (see
[Core Concepts](/guide/core-concepts)) -- add your own via a custom
`UserAgentTypePack` for anything not covered here.

## AI agents

`UserAgentAIAgentTypes` -- 20 entries, first match wins, table order:

| Name | Operator | Version |
| --- | --- | --- |
| GPTBot | OpenAI | Required |
| ChatGPT-User | OpenAI | Required |
| OAI-SearchBot | OpenAI | Required |
| PerplexityBot | Perplexity | Required |
| Perplexity-User | Perplexity | Required |
| ClaudeBot | Anthropic | None |
| Claude-User | Anthropic | None |
| Claude-SearchBot | Anthropic | None |
| CCBot | Common Crawl | Required |
| Amazonbot | Amazon | Required |
| Meta-ExternalAgent | Meta | Required |
| Meta-ExternalFetcher | Meta | Required |
| MistralAI-User | Mistral AI | Required |
| DuckAssistBot | DuckDuckGo | Required |
| Google-CloudVertexBot | Google | None |
| Kimi-User | Moonshot AI (Kimi) | Required |
| Timpibot | Timpi | Required |
| omgili | Webz.io | Required |
| YouBot | You.com | Required |
| Amzn-User | Amazon | Required |

Same non-exhaustive-starter-list caveat as bots above. Most entries are
sourced directly from that operator's own public documentation; a few
(`Google-CloudVertexBot`, `Kimi-User`, `Timpibot`, `omgili`, `YouBot`,
`Amzn-User`) are instead corroborated from multiple independent,
clearly-attributed sources because no first-party page could be found
quickly -- see
[`UserAgentAIAgentTypePack.kt`](https://github.com/ido-lempert/kmp-user-agent/blob/master/library/src/commonMain/kotlin/site/lempert/useragent/UserAgentAIAgentTypePack.kt)
for the full sourcing notes, including a few well-known agents that were
investigated and deliberately excluded (no distinct UA string to detect by,
or deprecated).

## Engines

`UserAgentEngineTypes` -- 5 entries, hand-derived from well-known engine UA
tokens (`uap-core` has no engine-parser section to vendor this from):

`Trident`, `Blink`, `Gecko`, `Presto`, `WebKit`

## Browsers

`UserAgentBrowserTypes` -- 191 named families below, out of 433 total
vendored detection rules. The remainder derive their family name directly
from the matched text itself (e.g. an app name embedded in the UA string)
rather than a fixed label, so they aren't individually listable here:

`115 Browser, 360 Secure Browser, AOL Desktop Gold Browser, AOL Shield Browser, AVG, Adobe CEP, Adobe CreativeCloud, Aloha Browser, Amarok, Amazon Silk, AntennaPod, Apple Mail, ArcGIS Earth, ArcMap, Atom Browser, AudioBoom, Avast Secure Browser, Avira, Baidu Browser, Baidu Explorer, Barca, Basilisk, BlackBerry, BlackBerry WebKit, Bon Echo, Brave, CCleaner, CFNetwork, Chrome Frame, Chrome Mobile, Chrome Mobile WebView, Chrome Mobile iOS, Chromium GOST Browser, CloudFoundry, Coc Coc, Collector for ArcGIS, Comodo Dragon, Conkeror, Craving Explorer Browser, Decentr Web3 Browser, DuckDuckGo, DuckDuckGo Mobile, ESPN, Ecosia Android, Ecosia iOS, Edge, Edge Mobile, Explorer for ArcGIS, Facebook, Facebook Messenger, FacebookBot, FancyMusic, Firefox Alpha, Firefox Beta, Firefox Mobile, Firefox iOS, GmailImageProxy, Google, GooglePlusBot, HTTPClient, HeyTap Browser, HiBrowser, HipChat Desktop Client, Hola Browser, Honor Browser, Huawei Browser, IE, IE Large Screen, IE Mobile, Iron, JiSu Browser, KakaoTalk, Konqueror, Kurio App, LINE, Lite Browser, Lotus Notes, LuaKit, MSIECrawler, Mail.ru Chromium Browser, Maxthon, MicroB, Microsoft Teams, Mint Browser, MiuiBrowser, Mobile Safari, Mobile Safari UI/WKWebView, Monitis, My Internet Browser, Mypal Browser, NCSA Mosaic, NetCast Smart TV, NetFront, NetFront NX, Netscape, NewRelicPingerBot, Nokia Browser, Nokia OSS Browser, Nokia Services (WAP) Browser, Norton, OBS Studio, ONE Browser, Obigo, Oculus Browser, Odin, Open Wave, OpenVAS Scanner, Opera, Opera Coast, Opera GX, Opera Mini, Opera Mobile, Opera Neon, Opera Touch, Operations Dashboard for ArcGIS, Outlook, Ovi Browser, Owncloud, Pale Moon, Palm Blazer, Palm Pre, Phantom, Phantom Browser, Phoenix Browser, PingdomBot, Pinterestbot, PodcastAddict, Podkicker, Polaris, Postbox, Python Requests, Python aiohttp, QAX Browser, QQ Browser, QQ Browser Mini, QQ Browser Mobile, Qt Web Engine, Quark, Quark PC, RSSRadio, RackspaceBot, Rekonq, Roblox App, Ruxit Synthetic, Safari, Sailfish Browser, Samsung Internet, Sber Browser, Seznam prohlížeč, Skype, Slack Desktop Client, Smart Lenovo Browser, SmartTV WebBrowser, Sogou Explorer, Sparrow Browser, Spider, StatusCakeBot, Steam Client, Steam Deck, Steam GameOverlay, Superhuman, Surveyon, Swiftfox, Tableau, Talon Cyber Security Browser, Teleca Browser, Telegram, Tenta Browser, Thunderbird, TikTok, Tizen Browser, TopBuzz, Twitter, Twitterbot, UC Browser, VLC, Vewd Browser, ViaFree, WMPlayer, WeChat Browser, WebKit Nightly, WebPageTest.org bot, Weibo, Whale, Windows Live Mail, Wolvic Browser, Workforce for ArcGIS, YahooMailProxy, Yandex Browser, iBrowser Mini, webOS Browser`

Notably absent from this list by name (but still detected, just without a
fixed label above): Chrome, Firefox, and a number of other major browsers
match via a capture-group-derived name rather than a `family_replacement`
override in the vendored data. Parse a real Chrome or Firefox UA string
with [the live demo](/#parse-a-user-agent) to see this in action.

## Operating systems

`UserAgentOsTypes` -- 36 named OS families below, out of 204 total vendored
detection rules (the remaining rules, like a portion of browsers/devices,
derive the OS name directly from the matched text itself):

`ATV OS X, Android, BlackBerry OS, BlackBerry Tablet OS, Brew MP, Chrome OS, Chromecast Android, Chromecast Fuchsia, Chromecast Linux, Chromecast SmartSpeaker, Debian, FireHbbTV, Firefox OS, FreeBSD, Gentoo, KaiOS, Linux, Mac OS, Mac OS X, Nokia Series 30 Plus, Nokia Series 40, Other, Red Hat, Samsung, Solaris, Symbian OS, Symbian^3, Symbian^3 Anna, Symbian^3 Belle, WatchOS, Windows, Windows Mobile, Windows Phone, iOS, tvOS, webOS`

## Device brands

`UserAgentDeviceTypes` -- 227 named device brands below, out of 633 total
vendored detection rules. Unlike the categories above, most device rules
(about 4 in 5) derive the specific **model** name dynamically from the
matched UA text rather than a fixed label, so only the more stable
**brand** names are listed here as a reference:

`3Q, Acer, Advent, Ainol, Airis, Airpad, Alcatel, Allfine, Allview, Allwinner, Amaway, Amazon, Amoi, Aoc, Aoson, Apanda, Apple, Archos, Arival, Arnova, Assistant, Asus, Attab, Audiosonic, Axioo, Azend, Bak, Bedove, Benss, Bird, BlackBerry, Blackberry, Blaupunkt, Blu, Blusens, Bmobile, Braun, Captiva, Casio, Cat, Celkon, Cellular, ChangJia, Cloudfone, Cmx, CobyKyros, Coolpad, Cube, Cubot, DNS, DOOV, Danew, Dell, Denver, Dex, DoCoMo, Enot, Evercoss, Explay, Fly, Freescale, Fujitsu, Galapad, Garmin-Asus, Generic, Generic_Android, Generic_Android_Tablet, Generic_Inettv, Gfive, Gigabyte, Gionee, GoClever, Google, HCLme, HP, HTC, Haier, Haipad, Hannspree, Hena, Hero, Hisense, Huawei, Hyundai, IMO, IconBIT, Impression, Infinix, Informer, Intenso, Intex, Iru, Itel, Ivio, JXD, Jaytech, Jiayu, KDDI, KTtech, Karbonn, Kingcom, Kobo, Ktouch, Kyocera, LG, LYF, Lava, Lemon, Lenco, Lenovo, Lexibook, Malata, Manta, Match, Maxx, Mediacom, Medias, Medion, Meizu, Meta, Micromax, Microsoft, Mito, Mobistel, Modecom, Motorola, Mpman, Msi, Multilaser, MyPhone, Mytab, Nabi, Nec, Nextbook, Nintendo, Nokia, Nook, Odys, Olivetti, Omega, OnePlus, Openpeak, Oppo, Orion, POV, PackardBell, Palm, Panasonic, Pantech, Papyre, Pearl, Phicomm, Philips, Pipo, Ployer, Polaroid, Pomp, Positivo, Prestigio, Proscan, Qmobile, Qmobilevn, Quanta, RCA, Regza, Rockchip, SKtelesys, Samsung, Sega, Sharp, Siemens, Simvalley, Skytex, Smartbitt, Softbank, Sony, SonyEricsson, Spice, Spider, Sprint, Tagi, Tecmobile, Tecno, Telstra, Terra, Tesla, Texet, Thalia, Thl, Thomson, Tmobile, Tomtec, Tooky, Toshiba, Touchmate, Trekstor, Treq, Umeox, Vernee, Versus, Vertu, Videocon, Viewsonic, Walton, WeTab, Wellcom, Wiko, Wolfgang, Woxter, Xianghe, XiaoMi, Xolo, Xoro, Yarvik, Yifang, ZTE, ZiiLabs, Zopo, Zync, bq, hitech, i-mate, iBall, iOCEAN, imobile, ionik, vivo`

(A few generic sentinel values appear in this list too -- `Generic`,
`Generic_Android`, `Generic_Android_Tablet`, `Generic_Inettv`, `Spider` --
from catch-all rules that don't identify a real brand, e.g. a bot/crawler UA
matched by device detection.)

## Where to go next

For how these packs compose and how to extend them yourself, see
[Core Concepts](/guide/core-concepts).
