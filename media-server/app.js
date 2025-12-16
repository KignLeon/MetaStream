const NodeMediaServer = require('node-media-server');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path'); // Import path module for absolute paths

// 1. Resolve absolute path for media storage
// This guarantees we know EXACTLY where files land
const mediaRoot = path.join(__dirname, 'media');

if (!fs.existsSync(mediaRoot)) {
  fs.mkdirSync(mediaRoot);
}

// 2. Find FFmpeg path dynamically
let ffmpegPath;
try {
  ffmpegPath = execSync('which ffmpeg').toString().trim();
  console.log(`✅ FFmpeg found at: ${ffmpegPath}`);
} catch (e) {
  console.error("❌ FFmpeg not found! Please run 'brew install ffmpeg'");
  process.exit(1);
}

const config = {
  rtmp: {
    port: 1935,
    chunk_size: 60000,
    gop_cache: true,
    ping: 30,
    ping_timeout: 60
  },
  http: {
    port: 8000,
    allow_origin: '*',
    mediaroot: mediaRoot, // Use Absolute Path
  },
  trans: {
    ffmpeg: ffmpegPath,
    tasks: [
      {
        app: 'live',
        // CRITICAL: Force COPY mode. Do not transcode.
        // This is the #1 fix for empty directories.
        vc: 'copy',
        ac: 'copy',
        hls: true,
        hlsFlags: '[hls_time=2:hls_list_size=3:hls_flags=delete_segments]',
        dash: true,
        dashFlags: '[f=dash:window_size=3:extra_window_size=5]'
      }
    ]
  }
};

var nms = new NodeMediaServer(config);
nms.run();

console.log("🚀 MetaStream Media Server is running!");
console.log(`👉 HLS Path: ${path.join(mediaRoot, 'live/stream/index.m3u8')}`);