const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, '..', 'static');
const destDir = path.join(__dirname, 'build');

console.log(`[*] Building frontend. Copying assets from: ${srcDir} to: ${destDir}`);

// Create build directory
if (!fs.existsSync(destDir)){
    fs.mkdirSync(destDir, { recursive: true });
}

// Copy directory recursively
function copyDir(src, dest) {
    const entries = fs.readdirSync(src, { withFileTypes: true });
    for (let entry of entries) {
        const srcPath = path.join(src, entry.name);
        const destPath = path.join(dest, entry.name);
        if (entry.isDirectory()) {
            if (!fs.existsSync(destPath)) {
                fs.mkdirSync(destPath);
            }
            copyDir(srcPath, destPath);
        } else {
            fs.copyFileSync(srcPath, destPath);
        }
    }
}

try {
    copyDir(srcDir, destDir);
    console.log('✓ Build successful. Frontend files copied to build/');
} catch (err) {
    console.error('[!] Build failed:', err);
    process.exit(1);
}
