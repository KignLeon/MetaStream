const NodeMediaServer = require('node-media-server');
const { execSync } = require('child_process');
const fs = require('fs');

// 1. Ensure 'media' folder exists
const mediaFolder = './media';
if (!fs.existsSync(mediaFolder)) {
  fs.mkdirSync(mediaFolder);
}

// 2. Find FFmpeg
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
    mediaroot: './media', // Explicit path
  },
  trans: {
    ffmpeg: ffmpegPath,
    tasks: [
      {
        app: 'live',
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
console.log("👉 HLS Path: " + __dirname + "/media/live/stream/index.m3u8");