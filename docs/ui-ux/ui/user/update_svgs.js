const fs = require('fs');
const path = require('path');

const userDir = __dirname;

function updateDark(content) {
    let newContent = content;
    const replacements = [
        [/#040b16|#190b14/g, '#061426'],
        [/#ec4899|#fb7185/g, '#38bdf8'],
        [/#06b6d4|#fbbf24/g, '#2563eb'],
        [/#8b5cf6|#34d399/g, '#a78bfa'],
        [/#0ea5e9/g, '#38bdf8'],
        [/#d946ef|#f59e0b/g, '#60a5fa'],
        [/#e0e7ff|#fff1f2/g, '#e0f2fe'],
        [/#fbcfe8|#fde68a/g, '#bfdbfe'],
        [/#10b981/g, '#34d399'],
        [/#ef4444/g, '#f43f5e'],
        [/rgba\(217, 70, 239,|rgba\(251, 113, 133,/g, 'rgba(56, 189, 248,'],
        [/rgba\(15, 23, 42, 0.45\)|rgba\(49, 18, 32, 0.62\)/g, 'rgba(9, 25, 48, 0.68)'],
        [/rgba\(4, 11, 22, 0.7\)|rgba\(49, 18, 32, 0.82\)/g, 'rgba(7, 19, 39, 0.86)'],
        [/rgba\(4, 11, 22, 0.6\)|rgba\(49, 18, 32, 0.72\)/g, 'rgba(7, 19, 39, 0.76)'],
        [/rgba\(4, 11, 22, 0.3\)|rgba\(49, 18, 32, 0.38\)/g, 'rgba(7, 19, 39, 0.42)'],
        [/rgba\(239, 68, 68,/g, 'rgba(244, 63, 94,'],
        [/rgba\(16, 185, 129,/g, 'rgba(52, 211, 153,'],
        [/stroke: rgba\(251, 113, 133, 0.15\)|stroke: rgba\(56, 189, 248, 0.15\)/g, 'stroke: rgba(56, 189, 248, 0.24)'],
        [/stroke: rgba\(251, 113, 133, 0.22\)/g, 'stroke: rgba(56, 189, 248, 0.24)'],
        [/stroke: rgba\(251, 191, 36, 0.55\)|stroke: rgba\(56, 189, 248, 0.4\)/g, 'stroke: rgba(96, 165, 250, 0.58)'],
        [/flood-opacity="0.4"/g, 'flood-opacity="0.5"'],
        [/Welcome Back/g, 'Welcome Home'],
        [/Sign in to manage AI training models/g, 'A calm place to care for training work'],
        [/Sign In/g, 'Enter Workspace'],
        [/(?:Hopeful )+AI Projects|AI Projects/g, 'Hopeful AI Projects'],
        [/Monitor training runs and manage model configurations\./g, 'Monitor progress, nurture models, and keep every run moving toward better outcomes.'],
        [/Start Training Job/g, 'Start Training With Care'],
        [/rgba\(4, 11, 22, 0.5\)|rgba\(49, 18, 32, 0.52\)/g, 'rgba(7, 19, 39, 0.56)'],
        [/rgba\(15, 23, 42, 0.8\)|rgba\(42, 16, 30, 0.86\)/g, 'rgba(7, 19, 39, 0.88)'],
        [/#020617|#120710/g, '#020b1a']
    ];

    for (const [pattern, replacement] of replacements) {
        newContent = newContent.replace(pattern, replacement);
    }

    return newContent;
}

function updateLight(content) {
    let newContent = content;
    const replacements = [
        [/#f8fafc|#fff8f3/g, '#f8fbff'],
        [/#fce7f3|#ffe4e6/g, '#dbeafe'],
        [/#cffafe|#fef3c7/g, '#e0f2fe'],
        [/#ede9fe|#dcfce7/g, '#eef2ff'],
        [/#0ea5e9|#fb7185/g, '#0ea5e9'],
        [/#d946ef|#f59e0b/g, '#2563eb'],
        [/#1e1b4b|#7f1d1d/g, '#1e3a8a'],
        [/#831843|#be123c/g, '#0f766e'],
        [/#0f172a|#2f1728/g, '#0f172a'],
        [/#64748b|#7c5c67/g, '#475569'],
        [/#334155|#6b3f4a/g, '#334155'],
        [/#cbd5e1|#fecdd3/g, '#bfdbfe'],
        [/#e2e8f0/g, '#dbeafe'],
        [/#eff6ff|#fff1f2/g, '#eff6ff'],
        [/#dbeafe/g, '#dbeafe'],
        [/#2563eb/g, '#2563eb'],
        [/#4f46e5/g, '#3b82f6'],
        [/#3b82f6/g, '#3b82f6'],
        [/#38bdf8/g, '#38bdf8'],
        [/#10b981/g, '#34d399'],
        [/#059669/g, '#059669'],
        [/#dc2626/g, '#e11d48'],
        [/#ef4444/g, '#f43f5e'],
        [/#f1f5f9/g, '#eff6ff'],
        [/#94a3b8|#c08497/g, '#93c5fd'],
        [/rgba\(255, 255, 255, 0.8\)|rgba\(255, 252, 249, 0.88\)/g, 'rgba(255, 255, 255, 0.9)'],
        [/rgba\(255, 255, 255, 0.95\)|rgba\(255, 252, 249, 0.96\)/g, 'rgba(255, 255, 255, 0.96)'],
        [/rgba\(255, 255, 255, 0.7\)|rgba\(255, 252, 249, 0.78\)/g, 'rgba(248, 251, 255, 0.82)'],
        [/rgba\(226, 232, 240, 0.8\)|rgba\(254, 205, 211, 0.9\)/g, 'rgba(191, 219, 254, 0.9)'],
        [/rgba\(148, 163, 184, 0.5\)|rgba\(251, 113, 133, 0.55\)/g, 'rgba(56, 189, 248, 0.55)'],
        [/Welcome Back/g, 'Welcome Home'],
        [/Sign in to manage AI training models/g, 'A calm place to care for training work'],
        [/Sign In/g, 'Enter Workspace'],
        [/(?:Hopeful )+AI Projects|AI Projects/g, 'Hopeful AI Projects'],
        [/Monitor training runs and manage model configurations\./g, 'Monitor progress, nurture models, and keep every run moving toward better outcomes.'],
        [/Start Training Job/g, 'Start Training With Care'],
        [/rgba\(15, 23, 42, 0.8\)|rgba\(42, 16, 30, 0.86\)/g, 'rgba(7, 19, 39, 0.88)'],
        [/#020617|#120710/g, '#020b1a']
    ];

    for (const [pattern, replacement] of replacements) {
        newContent = newContent.replace(pattern, replacement);
    }

    newContent = enhanceLightBackground(newContent);
    
    return newContent;
}

function enhanceLightBackground(content) {
    let newContent = content
        .replace(/\s*<g id="vivid-light-bg">[\s\S]*?<\/g>\n?/g, '')
        .replace(/\s*<linearGradient id="light-aurora"[\s\S]*?<\/linearGradient>\n?/g, '')
        .replace(/\s*<linearGradient id="light-aurora-2"[\s\S]*?<\/linearGradient>\n?/g, '')
        .replace(/\s*<radialGradient id="light-heart-glow"[\s\S]*?<\/radialGradient>\n?/g, '');

    const vividDefs = `
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
            </radialGradient>
`;

    newContent = newContent.replace('            <linearGradient id="primary-grad"', `${vividDefs}            <linearGradient id="primary-grad"`);

    const vividLayer = `<g id="vivid-light-bg">
<path d="M-80 210 C 210 30, 430 130, 640 84 S 1050 -10, 1360 118 L 1360 0 L -80 0 Z" fill="url(#light-aurora)" opacity="0.82"/>
<path d="M-90 675 C 210 475, 395 558, 640 486 S 1030 324, 1370 470 L 1370 800 L -90 800 Z" fill="url(#light-aurora-2)" opacity="0.72"/>
<ellipse cx="676" cy="330" rx="470" ry="230" fill="url(#light-heart-glow)" opacity="0.9"/>
<circle cx="116" cy="116" r="3" fill="#2563eb" opacity="0.34"/>
<circle cx="180" cy="268" r="2" fill="#38bdf8" opacity="0.42"/>
<circle cx="316" cy="104" r="2.5" fill="#60a5fa" opacity="0.36"/>
<circle cx="462" cy="188" r="2" fill="#14b8a6" opacity="0.36"/>
<circle cx="540" cy="78" r="3" fill="#a78bfa" opacity="0.34"/>
<circle cx="724" cy="142" r="2" fill="#0ea5e9" opacity="0.34"/>
<circle cx="870" cy="86" r="3" fill="#38bdf8" opacity="0.34"/>
<circle cx="1010" cy="194" r="2.5" fill="#60a5fa" opacity="0.38"/>
<circle cx="1166" cy="96" r="3" fill="#2563eb" opacity="0.28"/>
<circle cx="1120" cy="382" r="2" fill="#14b8a6" opacity="0.32"/>
<circle cx="986" cy="610" r="2.5" fill="#38bdf8" opacity="0.36"/>
<circle cx="760" cy="704" r="2" fill="#60a5fa" opacity="0.34"/>
<circle cx="438" cy="650" r="2.5" fill="#2563eb" opacity="0.28"/>
<circle cx="218" cy="588" r="2" fill="#a78bfa" opacity="0.32"/>
<path d="M78 430 C 260 360, 378 396, 540 330" fill="none" stroke="#38bdf8" stroke-width="2" stroke-opacity="0.12"/>
<path d="M745 256 C 910 190, 1040 214, 1218 146" fill="none" stroke="#2563eb" stroke-width="2" stroke-opacity="0.12"/>
</g>
`;

    return newContent.replace(
        /(<rect x="0" y="0" width="1280" height="800" class="" fill="url\(#bg-glow3\)"  rx="0"  \/>)/,
        `$1\n${vividLayer}`
    );
}

function processDir(dir, updateFn) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        if (file.endsWith('.svg')) {
            const filePath = path.join(dir, file);
            let content = fs.readFileSync(filePath, 'utf8');
            content = updateFn(content);
            fs.writeFileSync(filePath, content, 'utf8');
            console.log(`Updated ${filePath}`);
        }
    }
}

processDir(path.join(userDir, 'dark'), updateDark);
processDir(path.join(userDir, 'light'), updateLight);
console.log('Update complete.');
