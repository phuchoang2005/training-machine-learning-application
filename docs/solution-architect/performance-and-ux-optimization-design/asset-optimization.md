# Asset Optimization

## Build Output

| Area | Strategy |
| --- | --- |
| JavaScript | Use Vite production build with tree shaking and route chunks. |
| CSS | Keep Tailwind output small through content scanning and avoid unused custom layers. |
| Source maps | Generate for non-production debugging or upload privately; do not expose sensitive production source maps publicly unless approved. |
| Compression | Serve static assets with Brotli and gzip where supported. |
| Cache headers | Use hashed filenames with long-lived immutable caching for built assets. |
| HTML | Keep `index.html` short and avoid blocking third-party scripts. |

## JavaScript Budget

| Bundle | Budget |
| --- | ---: |
| Initial unauthenticated shell | 150 KB gzip target |
| Initial authenticated dashboard | 250 KB gzip target |
| Project detail route chunk | 150 KB gzip target |
| Job detail route chunk excluding log viewer | 180 KB gzip target |
| Log viewer chunk | 150 KB gzip target |
| Admin chunk | 150 KB gzip target |

Budgets should be reviewed after dependency selection. Exceeding a budget requires a documented reason and mitigation plan.

## CSS Optimization

* Use Tailwind theme tokens and shadcn/ui CSS variables for design tokens.
* Avoid additional global CSS frameworks beyond TailwindCSS.
* Remove unused Tailwind utilities during production build through correct content configuration.
* Prefer stable layout primitives over deeply nested decorative wrappers.
* Keep status colors and tokens centralized to prevent repeated CSS rules.
* Configure dark-mode variants for system-resolved `.dark` root class behavior.

## Font Optimization

| Rule | Requirement |
| --- | --- |
| Font count | Use one sans font family and one monospace font family. |
| Loading | Prefer system fonts first; if custom fonts are used, self-host and preload only critical weights. |
| Weights | Limit to regular, medium, and semibold. |
| Rendering | Use `font-display: swap` for custom fonts. |
| Logs and YAML | Use monospace with readable line height and no layout shift when loaded. |

## Icon Optimization

* Use tree-shakeable icon imports.
* Import individual icons rather than whole icon packs.
* Prefer existing icon library components over custom inline SVG duplication.
* Use accessible labels for icon-only buttons.

## Image and Static Asset Optimization

This platform is an operational tool and should not rely on large decorative images. If images are introduced later:

| Asset Type | Strategy |
| --- | --- |
| Product screenshots | Use compressed WebP or AVIF with PNG fallback only if required. |
| Avatars | Use small square images, lazy-loaded outside first viewport. |
| Empty-state illustrations | Keep optional and lightweight; do not block first render. |
| Favicons and app icons | Provide minimal required sizes. |

## Download Handling

Log and artifact files may be large and must be downloaded through authorized backend endpoints.

* Do not load artifact content into Redux state.
* Do not persist downloaded logs or artifacts in browser storage.
* Show progress only when browser and backend support it safely.
* Failed downloads should show a retry action and correlation ID when available.

## Acceptance Checklist

* Built assets use hashed filenames.
* Static assets are compressed and cacheable.
* Initial dashboard bundle excludes job log viewer, YAML editor, and admin code.
* Fonts do not block first meaningful render.
* Icon imports are tree-shakeable.
* Downloads bypass client-side caching of sensitive file contents.
