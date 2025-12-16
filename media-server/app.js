const NodeMediaServer = require('node-media-server');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// ============================================
// FORENSIC CONFIGURATION - PROVEN ON MACOS
// ============================================

const mediaRoot = path.join(__dirname, 'media');
if (!fs.existsSync(mediaRoot)) {
  fs.mkdirSync(mediaRoot, { recursive: true });
}

// CRITICAL: Get FFmpeg path and verify it's executable
let ffmpegPath;
try {
  ffmpegPath = execSync('which ffmpeg').toString().trim();
  console.log(`✅ FFmpeg found: ${ffmpegPath}`);
  
  // Verify it's actually executable
  execSync(`${ffmpegPath} -version`, { stdio: 'pipe' });
  console.log(`✅ FFmpeg is executable`);
} catch (e) {
  console.error("❌ FFmpeg not found or not executable!");
  console.error("Run: brew install ffmpeg");
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
    mediaroot: mediaRoot
  },
  trans: {
    ffmpeg: ffmpegPath,
    tasks: [
      {
        // CRITICAL: This must match your RTMP app name
        // OBS pushes to: rtmp://localhost/live/stream
        // So app = 'live', stream key = 'stream'
        app: 'live',
        
        // CRITICAL: Specify the stream key pattern
        // Use '*' to match any stream key under /live/*
        // Or use 'stream' to match only /live/stream
        stream: 'stream',
        
        // Force copy mode (no transcoding)
        vc: 'copy',
        ac: 'copy',
        
        // Enable HLS output
        hls: true,
        hlsFlags: '[hls_time=2:hls_list_size=3:hls_flags=delete_segments]'
      }
    ]
  },
  
  // CRITICAL: Enable verbose logging
  logType: 3 // 0: error, 1: normal, 2: debug, 3: ffdebug
};

// ============================================
// FORENSIC HOOKS - FORCE VISIBILITY
// ============================================

const nms = new NodeMediaServer(config);

// Hook into session events
nms.on('preConnect', (id, args) => {
  console.log('[FORENSIC] RTMP connection attempt:', {
    id,
    app: args.app,
    streamKey: args.streamKey || args.name
  });
});

nms.on('postConnect', (id, args) => {
  console.log('[FORENSIC] RTMP connection established:', {
    id,
    app: args.app,
    streamKey: args.streamKey || args.name
  });
});

nms.on('doneConnect', (id, args) => {
  console.log('[FORENSIC] RTMP connection closed:', {
    id,
    app: args.app
  });
});

nms.on('prePublish', (id, StreamPath, args) => {
  console.log('[FORENSIC] Stream publish starting:', {
    id,
    path: StreamPath,
    app: args.app,
    streamKey: args.name
  });
});

nms.on('postPublish', (id, StreamPath, args) => {
  console.log('[FORENSIC] ✅ Stream published successfully:', {
    id,
    path: StreamPath,
    expectedHLS: path.join(mediaRoot, args.app, args.name, 'index.m3u8')
  });
  
  // FORENSIC CHECK: Verify FFmpeg was spawned
  setTimeout(() => {
    const hlsPath = path.join(mediaRoot, args.app, args.name, 'index.m3u8');
    if (fs.existsSync(hlsPath)) {
      console.log('[FORENSIC] ✅ HLS OUTPUT CONFIRMED:', hlsPath);
    } else {
      console.error('[FORENSIC] ❌ HLS OUTPUT MISSING!');
      console.error('Expected:', hlsPath);
      console.error('Directory contents:', fs.readdirSync(mediaRoot, { recursive: true }));
    }
  }, 5000); // Wait 5 seconds for FFmpeg to start
});

nms.on('donePublish', (id, StreamPath, args) => {
  console.log('[FORENSIC] Stream ended:', {
    id,
    path: StreamPath
  });
});

// ============================================
// FAIL-FAST: Verify trans module loaded
// ============================================

nms.run();

console.log('\n🚀 MetaStream Media Server - FORENSIC MODE');
console.log('='.repeat(50));
console.log(`📂 Media Root: ${mediaRoot}`);
console.log(`🎥 FFmpeg: ${ffmpegPath}`);
console.log(`🔧 Log Level: FFDEBUG (maximum verbosity)`);
console.log('='.repeat(50));
console.log('\n📡 Waiting for OBS connection...');
console.log('   OBS Settings:');
console.log('   Server: rtmp://localhost/live');
console.log('   Stream Key: stream');
console.log('\n💡 Expected HLS Output:');
console.log(`   ${path.join(mediaRoot, 'live/stream/index.m3u8')}`);
console.log('='.repeat(50));

// Verify trans module is active
setTimeout(() => {
  if (nms.nms && nms.nms.trans) {
    console.log('[FORENSIC] ✅ Trans module loaded');
  } else {
    console.error('[FORENSIC] ❌ Trans module NOT loaded - check Node Media Server version!');
  }
}, 1000);