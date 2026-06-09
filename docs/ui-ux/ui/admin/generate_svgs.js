const fs = require('fs');
const path = require('path');

const outDir = __dirname;

const themes = {
  light: {
    bg: '#f8fbff',
    surface: 'rgba(255, 255, 255, 0.9)',
    surfaceStrong: 'rgba(255, 255, 255, 0.96)',
    shell: 'rgba(248, 251, 255, 0.82)',
    border: '#bfdbfe',
    softBorder: '#dbeafe',
    text: '#0f172a',
    muted: '#475569',
    primaryA: '#0ea5e9',
    primaryB: '#2563eb',
    accent: '#0f766e',
    success: '#059669',
    info: '#0ea5e9',
    warning: '#b45309',
    danger: '#e11d48',
    cancelled: '#64748b',
    headerText: '#1e3a8a',
    sidebarA: 'rgba(255, 255, 255, 0.96)',
    sidebarB: 'rgba(255, 255, 255, 0.9)',
    shadow: '#93c5fd',
    darkPanel: '#020b1a',
    connectedBg: '#ecfdf5',
    connectedStroke: '#34d399',
    connectedText: '#059669',
    input: '#ffffff',
    inputStroke: '#bfdbfe',
    tableHead: 'rgba(239, 246, 255, 0.9)',
    aurora: true,
  },
  dark: {
    bg: '#061426',
    surface: 'rgba(9, 25, 48, 0.68)',
    surfaceStrong: 'rgba(7, 19, 39, 0.86)',
    shell: 'rgba(7, 19, 39, 0.56)',
    border: 'rgba(56, 189, 248, 0.22)',
    softBorder: 'rgba(56, 189, 248, 0.1)',
    text: '#f0f9ff',
    muted: '#94a3b8',
    primaryA: '#38bdf8',
    primaryB: '#60a5fa',
    accent: '#bfdbfe',
    success: '#a78bfa',
    info: '#38bdf8',
    warning: '#fbbf24',
    danger: '#f43f5e',
    cancelled: '#94a3b8',
    headerText: '#e0f2fe',
    sidebarA: 'rgba(7, 19, 39, 0.86)',
    sidebarB: 'rgba(7, 19, 39, 0.42)',
    shadow: '#000000',
    darkPanel: '#020b1a',
    connectedBg: 'rgba(52, 211, 153, 0.1)',
    connectedStroke: 'rgba(52, 211, 153, 0.3)',
    connectedText: '#a78bfa',
    input: 'rgba(7, 19, 39, 0.76)',
    inputStroke: 'rgba(56, 189, 248, 0.2)',
    tableHead: 'rgba(56, 189, 248, 0.08)',
    aurora: false,
  },
};

const icons = {
  projects: '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
  notifications: '<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 0 1-3.46 0" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
  admin: '<path d="M12 2l7 4v5c0 5-3.5 9.5-7 11-3.5-1.5-7-6-7-11V6l7-4z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M9 12l2 2 4-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
  search: '<path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
  chevron: '<path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
};

function esc(value) {
  return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function icon(name, x, y, color, size = 20) {
  return `<svg x="${x}" y="${y}" width="${size}" height="${size}" viewBox="0 0 24 24" style="color:${color}">${icons[name]}</svg>`;
}

function defs(t) {
  const lightDefs = t.aurora ? `
    <linearGradient id="light-aurora" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#38bdf8" stop-opacity="0.55"/>
      <stop offset="45%" stop-color="#60a5fa" stop-opacity="0.28"/>
      <stop offset="100%" stop-color="#a78bfa" stop-opacity="0.32"/>
    </linearGradient>
    <linearGradient id="light-aurora-2" x1="100%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#22d3ee" stop-opacity="0.36"/>
      <stop offset="55%" stop-color="#93c5fd" stop-opacity="0.22"/>
      <stop offset="100%" stop-color="#14b8a6" stop-opacity="0.2"/>
    </linearGradient>
    <radialGradient id="light-heart-glow" cx="50%" cy="42%" r="52%">
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.85"/>
      <stop offset="42%" stop-color="#bae6fd" stop-opacity="0.38"/>
      <stop offset="100%" stop-color="#f8fbff" stop-opacity="0"/>
    </radialGradient>` : '';

  return `<defs>
    <radialGradient id="bg-glow" cx="85%" cy="15%" r="65%">
      <stop offset="0%" stop-color="${t.primaryA}" stop-opacity="${t.aurora ? '0.24' : '0.25'}"/>
      <stop offset="100%" stop-color="${t.bg}" stop-opacity="${t.aurora ? '0' : '1'}"/>
    </radialGradient>
    <radialGradient id="bg-glow2" cx="15%" cy="85%" r="65%">
      <stop offset="0%" stop-color="${t.primaryB}" stop-opacity="${t.aurora ? '0.18' : '0.2'}"/>
      <stop offset="100%" stop-color="${t.bg}" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="bg-glow3" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#a78bfa" stop-opacity="${t.aurora ? '0.2' : '0.1'}"/>
      <stop offset="100%" stop-color="${t.bg}" stop-opacity="0"/>
    </radialGradient>${lightDefs}
    <linearGradient id="primary-grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${t.primaryA}"/>
      <stop offset="100%" stop-color="${t.primaryB}"/>
    </linearGradient>
    <linearGradient id="text-grad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="${t.headerText}"/>
      <stop offset="100%" stop-color="${t.accent}"/>
    </linearGradient>
    <linearGradient id="sidebar-grad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="${t.sidebarA}"/>
      <stop offset="100%" stop-color="${t.sidebarB}"/>
    </linearGradient>
    <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="12" stdDeviation="24" flood-color="${t.shadow}" flood-opacity="${t.aurora ? '0.15' : '0.7'}"/>
    </filter>
    <filter id="shadow-sm" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="4" stdDeviation="8" flood-color="${t.shadow}" flood-opacity="${t.aurora ? '0.1' : '0.5'}"/>
    </filter>
    <filter id="glow-primary" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="6" stdDeviation="12" flood-color="${t.primaryB}" flood-opacity="0.45"/>
    </filter>
  </defs>`;
}

function style(t) {
  return `<style><![CDATA[
    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap');
    * { font-family: 'Inter', sans-serif; }
    .mono { font-family: 'JetBrains Mono', monospace; }
    .bg-base { fill: ${t.bg}; }
    .card { fill: ${t.surface}; stroke: ${t.border}; stroke-width: 1; }
    .glass { fill: url(#sidebar-grad); }
    .text-primary { fill: ${t.text}; }
    .text-muted { fill: ${t.muted}; }
    .gradient-text { fill: url(#text-grad); }
    .fade-in { animation: fadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards; opacity: 0; }
    .delay-1 { animation-delay: 0.1s; }
    .delay-2 { animation-delay: 0.2s; }
    .delay-3 { animation-delay: 0.3s; }
    .pulse { animation: pulse 2.5s infinite ease-in-out; }
    @keyframes fadeIn { to { opacity: 1; transform: translateY(0); } from { opacity: 0; transform: translateY(20px); } }
    @keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }
  ]]></style>`;
}

function bg(t) {
  const aurora = t.aurora ? `<g id="vivid-light-bg">
    <path d="M-80 210 C 210 30, 430 130, 640 84 S 1050 -10, 1360 118 L 1360 0 L -80 0 Z" fill="url(#light-aurora)" opacity="0.82"/>
    <path d="M-90 675 C 210 475, 395 558, 640 486 S 1030 324, 1370 470 L 1370 800 L -90 800 Z" fill="url(#light-aurora-2)" opacity="0.72"/>
    <ellipse cx="676" cy="330" rx="470" ry="230" fill="url(#light-heart-glow)" opacity="0.9"/>
    <circle cx="116" cy="116" r="3" fill="#2563eb" opacity="0.34"/><circle cx="316" cy="104" r="2.5" fill="#60a5fa" opacity="0.36"/>
    <circle cx="540" cy="78" r="3" fill="#a78bfa" opacity="0.34"/><circle cx="870" cy="86" r="3" fill="#38bdf8" opacity="0.34"/>
    <circle cx="1166" cy="96" r="3" fill="#2563eb" opacity="0.28"/><circle cx="986" cy="610" r="2.5" fill="#38bdf8" opacity="0.36"/>
    <circle cx="438" cy="650" r="2.5" fill="#2563eb" opacity="0.28"/><circle cx="218" cy="588" r="2" fill="#a78bfa" opacity="0.32"/>
    <path d="M78 430 C 260 360, 378 396, 540 330" fill="none" stroke="#38bdf8" stroke-width="2" stroke-opacity="0.12"/>
    <path d="M745 256 C 910 190, 1040 214, 1218 146" fill="none" stroke="#2563eb" stroke-width="2" stroke-opacity="0.12"/>
  </g>` : '';

  return `<rect x="0" y="0" width="1280" height="800" class="bg-base"/>
  <rect x="0" y="0" width="1280" height="800" fill="url(#bg-glow)"/>
  <rect x="0" y="0" width="1280" height="800" fill="url(#bg-glow2)"/>
  <rect x="0" y="0" width="1280" height="800" fill="url(#bg-glow3)"/>
  ${aurora}`;
}

function shell(t, active, crumb) {
  const nav = [
    ['Projects', 'projects', 96],
    ['Notifications', 'notifications', 148],
    ['Admin Console', 'admin', 200],
  ].map(([label, name, y]) => {
    const isActive = label === active;
    return `<g>
      ${isActive ? `<rect x="16" y="${y}" width="228" height="44" fill="${t.aurora ? '#eff6ff' : 'rgba(56, 189, 248, 0.15)'}" rx="12"/>
      <rect x="16" y="${y + 8}" width="4" height="28" fill="${t.primaryB}" rx="2" filter="url(#glow-primary)"/>` : ''}
      ${icon(name, 36, y + 12, isActive ? t.accent : t.muted)}
      <text x="68" y="${y + 27}" class="${isActive ? 'text-primary' : 'text-muted'}" font-size="15" font-weight="${isActive ? '700' : '600'}">${label}</text>
    </g>`;
  }).join('');

  return `<rect x="0" y="0" width="260" height="800" class="glass" filter="url(#shadow)"/>
  <line x1="260" y1="0" x2="260" y2="800" stroke="${t.softBorder}" stroke-width="1"/>
  <svg x="24" y="32" width="32" height="32" viewBox="0 0 24 24"><path d="M12 2l3 7 7 3-7 3-3 7-3-7-7-3 7-3z" fill="url(#primary-grad)"/><circle cx="18" cy="6" r="1.5" fill="${t.primaryA}"/><circle cx="6" cy="18" r="1" fill="${t.primaryB}"/></svg>
  <text x="68" y="54" class="gradient-text" font-size="20" font-weight="800">Future</text>
  ${nav}
  <line x1="24" y1="716" x2="236" y2="716" stroke="${t.softBorder}" stroke-width="1"/>
  <rect x="24" y="732" width="40" height="40" fill="url(#primary-grad)" rx="20"/>
  <text x="44" y="757" fill="#ffffff" font-size="15" font-weight="700" text-anchor="middle">AD</text>
  <text x="76" y="750" class="text-primary" font-size="13" font-weight="700">admin@co.com</text>
  <text x="76" y="768" class="text-muted" font-size="12">Platform Admin</text>
  <rect x="260" y="0" width="1020" height="72" fill="${t.shell}"/>
  <line x1="260" y1="72" x2="1280" y2="72" stroke="${t.softBorder}" stroke-width="1"/>
  <text x="290" y="42" class="text-muted" font-size="14" font-weight="600">${esc(crumb)}</text>
  <rect x="980" y="22" width="128" height="28" fill="${t.connectedBg}" stroke="${t.connectedStroke}" rx="14"/>
  <circle cx="996" cy="36" r="4" class="pulse" fill="${t.connectedText}"/>
  <text x="1010" y="40" font-size="11" font-weight="800" fill="${t.connectedText}">CONNECTED</text>
  ${icon('notifications', 1140, 24, t.muted, 24)}
  <circle cx="1158" cy="26" r="4" fill="${t.danger}"/>`;
}

function pageHeader(t, title, subtitle, action) {
  return `<text x="290" y="130" class="gradient-text fade-in" font-size="32" font-weight="800">${esc(title)}</text>
  <text x="290" y="155" class="text-muted fade-in delay-1" font-size="16">${esc(subtitle)}</text>
  ${action ? `<rect x="1060" y="105" width="180" height="44" fill="url(#primary-grad)" rx="8" filter="url(#glow-primary)"/>
  <text x="1150" y="132" fill="#ffffff" font-size="14" font-weight="700" text-anchor="middle">${esc(action)}</text>` : ''}`;
}

function metric(t, x, y, w, label, value, meta, color) {
  return `<g class="fade-in delay-2">
    <rect x="${x}" y="${y}" width="${w}" height="106" class="card" rx="8" filter="url(#shadow-sm)"/>
    <text x="${x + 20}" y="${y + 34}" class="text-muted" font-size="12" font-weight="800">${esc(label)}</text>
    <text x="${x + 20}" y="${y + 66}" fill="${color}" font-size="30" font-weight="800">${esc(value)}</text>
    <text x="${x + 20}" y="${y + 88}" class="text-muted" font-size="12">${esc(meta)}</text>
  </g>`;
}

function pill(t, x, y, text, color, w = 88) {
  return `<rect x="${x}" y="${y}" width="${w}" height="24" fill="${color}22" stroke="${color}" stroke-opacity="0.35" rx="12"/>
  <text x="${x + w / 2}" y="${y + 16}" fill="${color}" font-size="11" font-weight="800" text-anchor="middle">${esc(text)}</text>`;
}

function table(t, x, y, w, headers, rows, widths) {
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${50 + rows.length * 48}" class="card" rx="8" filter="url(#shadow-sm)"/>
  <rect x="${x}" y="${y}" width="${w}" height="44" fill="${t.tableHead}" rx="8"/>
  <line x1="${x}" y1="${y + 44}" x2="${x + w}" y2="${y + 44}" stroke="${t.softBorder}"/>`;
  let cx = x + 20;
  headers.forEach((h, i) => {
    out += `<text x="${cx}" y="${y + 28}" class="text-muted" font-size="11" font-weight="800">${esc(h)}</text>`;
    cx += widths[i];
  });
  rows.forEach((row, r) => {
    const ry = y + 44 + r * 48;
    out += `<line x1="${x}" y1="${ry + 48}" x2="${x + w}" y2="${ry + 48}" stroke="${t.softBorder}"/>`;
    cx = x + 20;
    row.forEach((cell, i) => {
      if (cell && cell.pill) out += pill(t, cx, ry + 12, cell.text, cell.color, cell.w || 88);
      else {
        const textClass = cell && cell.mono ? 'mono text-primary' : (cell && cell.muted ? 'text-muted' : 'text-primary');
        out += `<text x="${cx}" y="${ry + 30}" class="${textClass}" font-size="${cell && cell.mono ? 12 : 13}" font-weight="${cell && cell.bold ? 800 : 600}">${esc(cell && cell.text ? cell.text : cell)}</text>`;
      }
      cx += widths[i];
    });
  });
  return out;
}

function toolbar(t, placeholder, filters) {
  return `<g class="fade-in delay-2">
    <rect x="290" y="198" width="386" height="44" fill="${t.input}" stroke="${t.inputStroke}" rx="8" filter="url(#shadow-sm)"/>
    ${icon('search', 304, 210, t.muted)}
    <text x="334" y="226" class="text-muted" font-size="14">${esc(placeholder)}</text>
    ${filters.map((f, i) => `<rect x="${700 + i * 148}" y="198" width="132" height="44" fill="${t.input}" stroke="${t.inputStroke}" rx="8" filter="url(#shadow-sm)"/>
    <text x="${716 + i * 148}" y="226" class="text-primary" font-size="13" font-weight="700">${esc(f)}</text>
    ${icon('chevron', 802 + i * 148, 210, t.muted)}`).join('')}
  </g>`;
}

function queue(t) {
  const rows = [
    ['#10482', 'Vision Quality', { pill: true, text: 'RUNNING', color: t.info }, 'admin@co.com', '00:42:18', { text: 'Cancel', muted: true }],
    ['#10483', 'Recommendation Engine', { pill: true, text: 'PENDING', color: t.warning }, 'engineer@co.com', 'pos 1', { text: 'Cancel', muted: true }],
    ['#10484', 'Fraud Detection', { pill: true, text: 'PENDING', color: t.warning }, 'owner@co.com', 'pos 2', { text: 'Cancel', muted: true }],
    ['#10479', 'Churn Prediction', { pill: true, text: 'FAILED', color: t.danger }, 'engineer@co.com', '00:13:04', { text: 'Inspect', muted: true }],
    ['#10476', 'Image Classifier', { pill: true, text: 'SUCCESS', color: t.success }, 'owner@co.com', '01:11:52', { text: 'Open', muted: true }],
  ];
  return pageHeader(t, 'Admin Queue', 'Global capacity, FIFO order, and controlled cancellation for training jobs.', 'Pause Intake')
    + metric(t, 290, 184, 224, 'SERVER CAPACITY', '1 / 1', 'Single training worker active', t.info)
    + metric(t, 534, 184, 224, 'QUEUE DEPTH', '2', 'FIFO jobs waiting', t.warning)
    + metric(t, 778, 184, 224, 'AVG WAIT', '18m', 'Current estimate', t.primaryB)
    + metric(t, 1022, 184, 218, 'FAILURES TODAY', '1', 'Needs admin review', t.danger)
    + table(t, 290, 322, 950, ['JOB', 'PROJECT', 'STATUS', 'OWNER', 'AGE / POSITION', 'ACTION'], rows, [104, 214, 150, 194, 150, 90]);
}

function users(t) {
  const rows = [
    ['Ava Nguyen', 'admin@co.com', { pill: true, text: 'ADMIN', color: t.primaryB }, { pill: true, text: 'ACTIVE', color: t.success, w: 78 }, 'All projects', { text: 'Edit', muted: true }],
    ['Minh Tran', 'engineer@co.com', { pill: true, text: 'AI ENGINEER', color: t.info, w: 112 }, { pill: true, text: 'ACTIVE', color: t.success, w: 78 }, '4 projects', { text: 'Edit', muted: true }],
    ['Linh Pham', 'owner@co.com', { pill: true, text: 'OWNER', color: t.accent }, { pill: true, text: 'ACTIVE', color: t.success, w: 78 }, '2 projects', { text: 'Edit', muted: true }],
    ['Bao Le', 'contractor@co.com', { pill: true, text: 'AI ENGINEER', color: t.info, w: 112 }, { pill: true, text: 'DISABLED', color: t.cancelled, w: 92 }, '0 projects', { text: 'Review', muted: true }],
  ];
  return pageHeader(t, 'Admin Users', 'Manage activation, roles, and project access without exposing cross-project data.', 'Invite User')
    + toolbar(t, 'Search users by name or email...', ['All Roles', 'All States'])
    + metric(t, 290, 270, 224, 'ACTIVE USERS', '24', 'Company-authenticated accounts', t.success)
    + metric(t, 534, 270, 224, 'ADMINS', '3', 'Can access admin console', t.primaryB)
    + metric(t, 778, 270, 224, 'PROJECT OWNERS', '7', 'Can approve access', t.accent)
    + metric(t, 1022, 270, 218, 'DISABLED', '2', 'Blocked from login', t.cancelled)
    + table(t, 290, 418, 950, ['NAME', 'EMAIL', 'ROLE', 'STATE', 'ACCESS', 'ACTION'], rows, [150, 218, 156, 134, 174, 82]);
}

function audit(t) {
  const rows = [
    [{ text: '2026-06-09 09:12', mono: true }, { text: 'AUTH_ROLE_UPDATED', bold: true }, 'admin@co.com', { text: 'corr-7a91e2', mono: true }, { pill: true, text: 'SUCCESS', color: t.success }],
    [{ text: '2026-06-09 09:06', mono: true }, { text: 'QUEUE_JOB_CANCELLED', bold: true }, 'admin@co.com', { text: 'corr-f11c84', mono: true }, { pill: true, text: 'SUCCESS', color: t.success }],
    [{ text: '2026-06-09 08:54', mono: true }, { text: 'PROJECT_ACCESS_DENIED', bold: true }, 'engineer@co.com', { text: 'corr-b49d01', mono: true }, { pill: true, text: 'FAILED', color: t.danger }],
    [{ text: '2026-06-09 08:21', mono: true }, { text: 'JOB_RETRY_REQUESTED', bold: true }, 'owner@co.com', { text: 'corr-33cd10', mono: true }, { pill: true, text: 'SUCCESS', color: t.success }],
    [{ text: '2026-06-09 07:58', mono: true }, { text: 'CONFIG_SNAPSHOT_SAVED', bold: true }, 'engineer@co.com', { text: 'corr-a1029b', mono: true }, { pill: true, text: 'SUCCESS', color: t.success }],
  ];
  return pageHeader(t, 'Admin Audit', 'Trace privileged actions, authorization decisions, and job operations by correlation ID.', 'Export CSV')
    + toolbar(t, 'Search actor, action, or correlation ID...', ['Last 24h', 'All Actions'])
    + `<g class="fade-in delay-2">
      <rect x="290" y="270" width="950" height="76" class="card" rx="8" filter="url(#shadow-sm)"/>
      <text x="314" y="300" class="text-primary" font-size="15" font-weight="800">Focused Review</text>
      <text x="314" y="324" class="text-muted" font-size="13">Cross-project access denial from engineer@co.com is highlighted for security follow-up.</text>
      ${pill(t, 1084, 292, 'NEEDS REVIEW', t.danger, 116)}
    </g>`
    + table(t, 290, 378, 950, ['TIME', 'ACTION', 'ACTOR', 'CORRELATION ID', 'RESULT'], rows, [170, 244, 182, 190, 112]);
}

const screens = [
  ['09-admin-queue.svg', 'Admin Console / Queue', queue],
  ['10-admin-users.svg', 'Admin Console / Users', users],
  ['11-admin-audit.svg', 'Admin Console / Audit', audit],
];

function render(themeName, file, crumb, contentFn) {
  const t = themes[themeName];
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 800" width="1280" height="800">
  ${defs(t)}
  ${style(t)}
  ${bg(t)}
  ${shell(t, 'Admin Console', crumb)}
  ${contentFn(t)}
</svg>
`;
}

for (const [file, crumb, contentFn] of screens) {
  for (const themeName of Object.keys(themes)) {
    const target = path.join(outDir, themeName, file);
    fs.writeFileSync(target, render(themeName, file, crumb, contentFn), 'utf8');
    console.log(`Generated ${target}`);
  }
}
