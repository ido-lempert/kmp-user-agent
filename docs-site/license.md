# License

`kmp-user-agent`'s own code is MIT-licensed. It vendors one piece of
Apache-2.0-licensed third-party data -- browser/OS/device detection rules
from [uap-core](https://github.com/ua-parser/uap-core) -- attributed below.

Both notices are bundled directly inside every published artifact except
one: the Maven Central JAR/AAR and the npm package all embed `LICENSE` and
`NOTICE` under `META-INF`/the package root. The SPM XCFramework is the
exception -- embedding text files inside a compiled binary isn't standard
practice, so it doesn't bundle either file directly; SPM consumers get both
anyway because `swift package resolve` checks out this whole git repository
alongside the binary download.

This page is a convenience summary, not a legal opinion -- see the linked
source files below for the authoritative text, and consult your own counsel
for anything compliance-critical.

## This library (MIT)

`SPDX-License-Identifier: MIT`

```
MIT License

Copyright (c) 2026 Ido Lempert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

The canonical, always-current copy lives at
[`LICENSE`](https://github.com/ido-lempert/kmp-user-agent/blob/master/LICENSE)
in the repository root.

## Third-party attribution: uap-core (Apache License 2.0)

`SPDX-License-Identifier: Apache-2.0` (for the vendored data described here only -- this library's own code stays MIT)

This library's Gradle build vendors a pinned snapshot of uap-core's
`regexes.yaml` and compiles it into a build-time-generated Kotlin rule table
-- it is never loaded as a runtime resource by the published artifact.

```
uap-core
https://github.com/ua-parser/uap-core
Copyright 2009 Google Inc.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

A copy of the License is also included in this repository at
library/vendor/uap-core/LICENSE.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

The full attribution -- including the vendored commit and the additionally
vendored test fixtures -- lives at
[`library/NOTICE`](https://github.com/ido-lempert/kmp-user-agent/blob/master/library/NOTICE)
in the repository, alongside two separate dependency license audits: one
covering the whole published Kotlin/Android/JVM artifact's production, test,
and publish-relevant build tooling, and a second covering specifically the
npm package's own runtime dependency surface. Both audits reached the same
finding: every dependency is MIT- or Apache-2.0-licensed -- no copyleft
(GPL/LGPL/AGPL) dependency is present anywhere in either scope.

## Third-party attribution: docs-site logos (Simple Icons, CC0-1.0) {#third-party-attribution-docs-site-logos-simple-icons-cc0-1-0}

`SPDX-License-Identifier: CC0-1.0` (for the icon artwork only -- see below)

The homepage's ["A sample of what it detects"](/#a-sample-of-what-it-detects)
section references brand logo icons from [Simple Icons](https://simpleicons.org),
loaded live from a version-pinned CDN URL (`cdn.jsdelivr.net`) -- no icon
files are vendored into this repository. Simple Icons itself is released
under CC0-1.0, but as its own
[disclaimer](https://github.com/simple-icons/simple-icons/blob/develop/DISCLAIMER.md)
states, that doesn't extend to the brand marks the icons depict: each logo
remains the trademark of the company it represents. Showing a company's
logo there identifies which company's browser/bot/AI-agent crawler a
detected User-Agent string matches -- it is not an endorsement by, or
partnership with, that company.

## Independently verifiable

The license metadata above isn't only asserted by this repository -- it's
also published as structured metadata you can cross-check on the registries
themselves: the [Maven Central listing](https://central.sonatype.com/artifact/site.lempert/user-agent)'s
license field, and the [npm package page](https://www.npmjs.com/package/@lempert/user-agent)'s
license field, both declare `MIT`.

## Next steps

For trademark/nominative-use, accessibility, and privacy/analytics
disclosures, see [Legal disclaimers](/legal).

For how to actually add and use the library, see the platform guide for
your language: [Browser & Node.js (JS)](./guide/js),
[Android](./guide/android), [iOS](./guide/ios), or [JVM](./guide/jvm).
