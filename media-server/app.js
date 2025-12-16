const { spawn } = require('child_process');
const { execSync } = require('child_process');
const express = require('express');
const fs = require('fs');
const path = require('path');
const cors = require('cors');

// ============================================
// FFMPEG-ONLY RTMP → HLS PIPELINE
// Zero dependencies on buggy libraries
// ============================================

const RTMP_PORT = 1935;
const HTTP_PORT = 8000;
const mediaRoot = path.join(__dirname, 'media');

// Ensure media directory exists
if (!fs.existsSync(mediaRoot)) {
  fs.mkdirSync(mediaRoot, { recursive: true });
}

// Verify FFmpeg
let ffmpegPath;
try {
  ffmpegPath = execSync('which ffmpeg').toString().trim();
  console.log(`✅ FFmpeg found: ${ffmpegPath}`);
  execSync(`${ffmpegPath} -version`, { stdio: 'pipe' });
} catch (e) {
  console.error("❌ FFmpeg not found! Run: brew install ffmpeg");
  process.exit(1);
}

// ============================================
// FFMPEG RTMP SERVER WITH HLS OUTPUT
// ============================================

let ffmpegProcess = null;

function startRTMPServer() {
  const hlsOutputPath = path.join(mediaRoot, 'live/stream/index.m3u8');
  const hlsDir = path.dirname(hlsOutputPath);
  
  // Ensure output directory exists
  if (!fs.existsSync(hlsDir)) {
    fs.mkdirSync(hlsDir, { recursive: true });
  }

  console.log('\n🎬 Starting FFmpeg RTMP Server...\n');

  const args = [
    // RTMP Input (listen mode)
    '-listen', '1',
    '-f', 'flv',
    '-i', `rtmp://0.0.0.0:${RTMP_PORT}/live/stream`,
    
    // Copy codecs (no transcoding)
    '-c:v', 'copy',
    '-c:a', 'copy',
    
    // HLS Output Settings
    '-f', 'hls',
    '-hls_time', '2',                    // 2-second segments
    '-hls_list_size', '3',               // Keep 3 segments in playlist
    '-hls_flags', 'delete_segments',     // Auto-delete old segments
    '-hls_segment_filename', path.join(hlsDir, 'stream-%03d.ts'),
    
    hlsOutputPath
  ];

  console.log('📡 FFmpeg Command:');
  console.log(`   ${ffmpegPath} ${args.join(' ')}\n`);

  ffmpegProcess = spawn(ffmpegPath, args);

  // Capture stdout
  ffmpegProcess.stdout.on('data', (data) => {
    console.log(`[FFMPEG OUT] ${data.toString().trim()}`);
  });

  // Capture stderr (FFmpeg logs to stderr by default)
  ffmpegProcess.stderr.on('data', (data) => {
    const output = data.toString().trim();
    
    // Highlight important messages
    if (output.includes('Stream mapping:')) {
      console.log('✅ [FFMPEG] Stream mapping detected - encoding started');
    } else if (output.includes('Opening')) {
      console.log(`📂 [FFMPEG] ${output}`);
    } else if (output.includes('frame=')) {
      // Progress updates (frame count)
      process.stdout.write(`\r⏱️  [FFMPEG] ${output.split('\n')[0]}`);
    } else {
      console.log(`[FFMPEG] ${output}`);
    }
  });

  // Handle process exit
  ffmpegProcess.on('close', (code) => {
    console.log(`\n⚠️  FFmpeg process exited with code ${code}`);
    ffmpegProcess = null;
  });

  ffmpegProcess.on('error', (err) => {
    console.error('❌ FFmpeg error:', err);
  });

  console.log('✅ FFmpeg RTMP server listening on port', RTMP_PORT);
  console.log(`📺 OBS Settings:`);
  console.log(`   Server: rtmp://localhost/live/stream`);
  console.log(`   Stream Key: (leave blank)`);
  console.log(`\n💡 HLS Output: http://localhost:${HTTP_PORT}/live/stream/index.m3u8\n`);
}

// ============================================
// HTTP SERVER FOR HLS DELIVERY
// ============================================

const app = express();

// Enable CORS for browser access
app.use(cors());

// Serve HLS files from media directory
app.use(express.static(mediaRoot, {
  setHeaders: (res, filepath) => {
    // Set proper MIME types for HLS
    if (filepath.endsWith('.m3u8')) {
      res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
    } else if (filepath.endsWith('.ts')) {
      res.setHeader('Content-Type', 'video/mp2t');
    }
    // Disable caching for live streams
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  }
}));

// Health check endpoint
app.get('/health', (req, res) => {
  const hlsPath = path.join(mediaRoot, 'live/stream/index.m3u8');
  const isStreaming = fs.existsSync(hlsPath);
  
  res.json({
    status: 'ok',
    ffmpeg: ffmpegProcess ? 'running' : 'stopped',
    streaming: isStreaming,
    hlsPath: isStreaming ? `/live/stream/index.m3u8` : null
  });
});

app.listen(HTTP_PORT, () => {
  console.log(`✅ HTTP server listening on http://localhost:${HTTP_PORT}`);
  console.log(`📊 Health check: http://localhost:${HTTP_PORT}/health\n`);
});

// ============================================
// STARTUP
// ============================================

console.log('\n🚀 MetaStream FFmpeg-Only Server');
console.log('='.repeat(50));
console.log(`📂 Media Root: ${mediaRoot}`);
console.log(`🎥 FFmpeg: ${ffmpegPath}`);
console.log(`🔌 RTMP Port: ${RTMP_PORT}`);
console.log(`🌐 HTTP Port: ${HTTP_PORT}`);
console.log('='.repeat(50) + '\n');

startRTMPServer();

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n\n🛑 Shutting down...');
  if (ffmpegProcess) {
    ffmpegProcess.kill('SIGTERM');
  }
  process.exit(0);
});

// Auto-restart FFmpeg if it crashes
setInterval(() => {
  if (!ffmpegProcess || ffmpegProcess.exitCode !== null) {
    console.log('\n⚠️  FFmpeg not running, restarting...');
    startRTMPServer();
  }
}, 5000); // Check every 5 seconds