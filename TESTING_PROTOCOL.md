# 🧪 MetaStream Live - Complete Testing Protocol

## 📋 PRE-DEMO VALIDATION CHECKLIST

### Environment Setup Tests
- [ ] Java 17+ installed (`java -version`)
- [ ] Maven installed (`mvn -version`)
- [ ] Node.js installed (`node -v`)
- [ ] FFmpeg installed (`ffmpeg -version`)
- [ ] OBS Studio installed and configured
- [ ] Port 8080 available (Java backend)
- [ ] Port 8000 available (FFmpeg media server)
- [ ] Port 1935 available (RTMP)

---

## 🏗️ BUILD & COMPILE TESTS

### Test 1: Maven Build
```bash
cd /path/to/MetaStream
mvn clean package
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXX s
```

**✅ PASS Criteria:**
- No compilation errors
- JAR file created in `target/metastream-1.0-SNAPSHOT.jar`

**🔴 FAIL Indicators:**
- Compilation errors
- Missing dependencies
- Test failures

---

### Test 2: FFmpeg Media Server
```bash
cd media-server
npm install
npm start
```

**Expected Output:**
```
✅ FFmpeg found: /path/to/ffmpeg
🚀 MetaStream FFmpeg-Only Server
📡 Waiting for OBS connection...
[Express] Listening on port 8000
```

**✅ PASS Criteria:**
- FFmpeg found
- Server starts on port 8000
- No errors in logs

---

### Test 3: Java Backend Startup
```bash
java -jar target/metastream-1.0-SNAPSHOT.jar
```

**Expected Output:**
```
🌐 MetaStream Live Backend starting on port 8080
✅ FFmpeg media server is running
✅ MetaStream Live Backend ready at http://localhost:8080
🔌 WebSocket endpoint: ws://localhost:8080/ws
```

**✅ PASS Criteria:**
- Both servers start without errors
- No SLF4J binding warnings
- WebSocket endpoint initialized

---

## 🧩 LEARNING OUTCOMES VALIDATION

### LO1: OOP Principles - Encapsulation
**Test:** Examine `User.java` and `StreamSession.java`

**Verification Points:**
- [ ] Private fields with public getters/setters
- [ ] Constructor properly initializes objects
- [ ] toString() methods present
- [ ] No direct field access from external classes

**Demo Talking Points:**
- "Here we have the User class with private fields like username, phone, and ttsEnabled"
- "Notice how we use public getters and setters to control access - this is encapsulation"
- "The StreamSession class demonstrates aggregation by containing a User object"

---

### LO2: Arrays
**Test:** Check `StreamSession.java` line 18

**Verification Points:**
- [ ] `private final List<ChatMessage> messages;` declared
- [ ] Initialized as `new ArrayList<>()` in constructor
- [ ] `addMessage()` method adds to list
- [ ] `getMessages()` returns defensive copy

**Demo Talking Points:**
- "The StreamSession class uses an ArrayList to store ChatMessage objects"
- "This demonstrates working with arrays and collections in Java"
- "We use ArrayList because it's dynamic - can grow as more messages are added"

---

### LO3: Classes & Aggregation
**Test:** Examine `StreamSession.java` lines 16-17

**Verification Points:**
- [ ] `private final User user;` declared
- [ ] User object passed to constructor
- [ ] StreamSession "has-a" User (composition)
- [ ] List of ChatMessage objects (aggregation)

**Demo Talking Points:**
- "StreamSession aggregates both a User object and a List of ChatMessages"
- "This is aggregation - the container holds references to other objects"
- "If the StreamSession ends, the User object still exists independently"

---

### LO4: Inheritance & Polymorphism
**Test:** Check `NotificationService.java`, `SMSNotifier.java`, `TTSNotifier.java`

**Verification Points:**
- [ ] `NotificationService` is an interface
- [ ] `SMSNotifier` implements `NotificationService`
- [ ] `TTSNotifier` implements `NotificationService`
- [ ] Both override `sendNotification(String message)`

**Demo Talking Points:**
- "NotificationService is our interface - it defines the contract"
- "Both SMSNotifier and TTSNotifier implement this interface"
- "This is polymorphism - same method signature, different implementations"
- "We could swap between SMS and TTS at runtime without changing code"

**Test at Runtime:**
```java
// In Main.java (add for demo):
NotificationService sms = new SMSNotifier();
NotificationService tts = new TTSNotifier();

sms.sendNotification("Stream started!");
tts.sendNotification("Stream started!");
```

---

### LO5: Generic Collections
**Test:** Check `StreamSession.java` line 18 and methods

**Verification Points:**
- [ ] `List<ChatMessage> messages` uses type parameter
- [ ] Type-safe additions: `messages.add(msg);`
- [ ] Generic return type: `public List<ChatMessage> getMessages()`
- [ ] No raw types or unchecked warnings

**Demo Talking Points:**
- "ArrayList is parameterized with ChatMessage - this is a generic collection"
- "Type safety means we can only add ChatMessage objects, not any random object"
- "The compiler checks this at compile-time, preventing runtime errors"

---

### LO6: GUI & Events
**Test:** Check `Main.java` and `livedash.html`

**Verification Points:**
- [ ] HTTP POST to `/api/stream/start` creates session
- [ ] WebSocket connection handles real-time chat
- [ ] Button clicks trigger backend methods
- [ ] Frontend JS event listeners call API endpoints

**Demo Flow:**
1. Open `index.html` in browser
2. Fill form and click "Create Stream Session"
3. Show network tab: POST request sent
4. Show backend logs: "🎬 Stream started by X"
5. Go to dashboard, send chat message
6. Show backend logs: "[CHAT] User: message"

**Demo Talking Points:**
- "When user clicks 'Create Stream', JavaScript sends a POST request"
- "The backend handles this event in the /api/stream/start route"
- "WebSocket provides bidirectional real-time communication for chat"
- "This demonstrates event-driven programming and GUI interactions"

---

### LO7: Exception Handling
**Test:** Check try-catch blocks in multiple files

**Verification Points:**
- [ ] `Main.java` - All routes have try-catch
- [ ] `FileLogger.java` - File I/O wrapped in try-catch
- [ ] `MediaServerClient.java` - HTTP requests wrapped
- [ ] `WebSocketHandler.java` - Message handling wrapped

**Demo Test Cases:**
1. **Test 1:** Start stream without FFmpeg running
   - Expected: 503 error with message "Media server unavailable"
   - Shows exception caught and handled gracefully

2. **Test 2:** Send malformed JSON to chat endpoint
   - Expected: 400 error with "Invalid JSON format"
   - Shows JsonSyntaxException caught

3. **Test 3:** Try to read non-existent log file
   - Expected: Returns "No logs found" message
   - Shows IOException caught in FileLogger

**Demo Talking Points:**
- "Notice the try-catch blocks around file operations"
- "If an error occurs, we catch it and return a meaningful message"
- "Without exception handling, the application would crash"
- "Users see friendly error messages instead of stack traces"

---

### LO8: Text File I/O
**Test:** Check `FileLogger.java` writeLog() and readLog()

**Verification Points:**
- [ ] `writeLog()` uses FileWriter
- [ ] Writes session data to `stream_log.txt`
- [ ] `readLog()` uses Files.readString()
- [ ] File contains formatted session information

**Demo Flow:**
1. Start a stream session
2. Send a few chat messages
3. Stop the stream
4. Open `stream_log.txt` in text editor
5. Show formatted output with session details

**Demo Talking Points:**
- "When a stream ends, FileLogger writes all session data to a text file"
- "We use FileWriter to write and Files.readString to read"
- "This demonstrates persistent storage using basic file I/O"
- "The log includes username, duration, messages - all formatted as text"

---

## 🎬 FUNCTIONAL FEATURE TESTS

### Feature Test 1: Stream Session Creation
**Steps:**
1. Open `http://localhost:8080`
2. Enter username "TestUser"
3. Check "Enable SMS & TTS"
4. Click "Create Stream Session"

**Expected Results:**
- ✅ Success message appears
- ✅ OBS instructions display
- ✅ RTMP URL shown: `rtmp://localhost/live/stream`
- ✅ Backend logs: "🎬 Stream started by TestUser"

**Backend Verification:**
```bash
curl http://localhost:8080/api/stream/active
```
Should return JSON with session details.

---

### Feature Test 2: OBS → FFmpeg → HLS Pipeline
**Steps:**
1. Open OBS Studio
2. Settings → Stream
3. Server: `rtmp://localhost/live/stream`
4. Stream Key: (leave blank)
5. Click "Start Streaming"

**Expected Results:**
- ✅ OBS shows green streaming indicator
- ✅ FFmpeg logs show:
  ```
  [FFMPEG] Opening 'rtmp://0.0.0.0:1935/live/stream' for reading
  ✅ [FFMPEG] Stream mapping detected - encoding started
  📂 [FFMPEG] Opening 'media/live/stream/index.m3u8' for writing
  ```
- ✅ Files created in `media-server/media/live/stream/`:
  - `index.m3u8`
  - `stream-000.ts`, `stream-001.ts`, etc.

**File Verification:**
```bash
ls -la media-server/media/live/stream/
curl http://localhost:8000/live/stream/index.m3u8
```

---

### Feature Test 3: HLS Video Playback
**Steps:**
1. Navigate to `http://localhost:8080/livedash.html`
2. Wait for video player to load

**Expected Results:**
- ✅ Video player loads
- ✅ Status changes to "✅ Stream connected"
- ✅ Video plays showing OBS source
- ✅ Duration timer increments
- ✅ No buffering or lag (after initial load)

**Browser Console Check:**
```javascript
// Should NOT see:
❌ HLS.js errors
❌ 404 errors for .m3u8 or .ts files
❌ CORS errors
```

---

### Feature Test 4: WebSocket Connection
**Steps:**
1. On dashboard, open browser DevTools (F12)
2. Go to Console tab

**Expected Results:**
- ✅ Log: "✅ WebSocket connected"
- ✅ Green dot indicator shows "Connected"
- ✅ WebSocket URL: `ws://localhost:8080/ws`

**Test Resilience:**
1. Restart Java backend (Ctrl+C, then restart)
2. Dashboard should show "Reconnecting..."
3. After backend restarts: "Connected" again

---

### Feature Test 5: Real-Time Chat (Single User)
**Steps:**
1. In dashboard, type message: "Hello from streamer"
2. Press Enter

**Expected Results:**
- ✅ Message appears instantly in chat panel
- ✅ Avatar circle shows first letter "T"
- ✅ Username "TestUser" displayed
- ✅ Timestamp shows current time
- ✅ Message counter increments to "1"
- ✅ Backend logs: `[CHAT] TestUser: Hello from streamer`

---

### Feature Test 6: Real-Time Chat (Multi-User)
**Steps:**
1. Keep dashboard open in one window
2. Open new **incognito/private window**
3. Navigate to `http://localhost:8080/viewer.html`
4. Enter name: "Viewer1"
5. Send message: "Hi from viewer"

**Expected Results:**
- ✅ Message appears in BOTH windows instantly
- ✅ No page refresh needed
- ✅ Both users see all messages in same order
- ✅ Backend logs show both connections:
  ```
  📡 WebSocket connected: /127.0.0.1:XXXXX (Total: 2)
  [CHAT] Viewer1: Hi from viewer
  ```

---

### Feature Test 7: Stream Statistics
**What to Verify:**
- ✅ Duration timer increments every second (HH:MM:SS)
- ✅ Viewer count shows "2" (dashboard + viewer page)
- ✅ Message count increments with each chat message
- ✅ Stream info displays correct RTMP and HLS URLs

**Manual Check:**
Watch for 30 seconds - duration should show accurate time elapsed.

---

### Feature Test 8: Stop Stream
**Steps:**
1. In dashboard, click "🛑 Stop Stream"
2. Confirm in dialog

**Expected Results:**
- ✅ Redirects to `summary.html`
- ✅ Summary shows:
  - Duration (e.g., "0h 2m 15s")
  - Peak Viewers count (e.g., "2")
  - Total Messages count
  - Session ID
- ✅ Backend logs:
  ```
  🛑 Stream stopped for TestUser - Duration: 0h 2m 15s, Messages: 3
  ✅ Stream session logged to stream_log.txt
  ```

---

### Feature Test 9: Download Chat Log
**Steps:**
1. On summary page, click "Download Log"

**Expected Results:**
- ✅ Browser downloads file: `metastream-log-[timestamp].txt`
- ✅ File contains:
  ```
  MetaStream Live - Chat Log
  ==================================================
  
  [2025-12-18T10:30:15] TestUser: Hello from streamer
  [2025-12-18T10:30:22] Viewer1: Hi from viewer
  ```
- ✅ All messages with timestamps included

---

### Feature Test 10: Health Check API
**Test:**
```bash
curl http://localhost:8080/api/health
```

**Expected Response:**
```json
{
  "backend": "ok",
  "mediaServer": true,
  "activeSession": true,
  "websocketConnections": 2
}
```

---

### Feature Test 11: Error Handling - No Media Server
**Test:**
1. Stop FFmpeg server (Ctrl+C in media-server terminal)
2. Try to create new stream session

**Expected Results:**
- ✅ Error message: "Media server unavailable"
- ✅ HTTP 503 status
- ✅ Backend doesn't crash
- ✅ User-friendly error, not stack trace

---

### Feature Test 12: Error Handling - Duplicate Session
**Test:**
1. Create first stream session successfully
2. Try to create second session without stopping first

**Expected Results:**
- ✅ Error message: "A stream is already active"
- ✅ HTTP 409 status
- ✅ First session unaffected

---

## 📱 IPHONE SCREEN MIRRORING SETUP

### Test 13: iPhone → OBS Screen Capture
**Goal:** Stream Instagram Live from Ray-Ban Metas via iPhone screen mirror

**Setup Steps:**

#### Option A: USB Cable Connection (Recommended)
1. Connect iPhone to Mac via USB cable
2. Open QuickTime Player on Mac
3. File → New Movie Recording
4. Click dropdown next to record button → Select iPhone
5. iPhone screen appears in QuickTime window

**OBS Configuration:**
1. In OBS, remove current "macOS Screen Capture" source
2. Add Source → "Window Capture"
3. Select "QuickTime Player" window
4. Crop to show only iPhone screen
5. Scale to fit canvas

**Alternative: Use iPhone Camera Capture**
1. OBS → Add Source → "Video Capture Device"
2. Select your iPhone from device list
3. This captures the iPhone camera directly (might not work for Instagram Live)

#### Option B: Wireless Screen Mirroring (AirPlay)
1. Ensure Mac and iPhone on same WiFi
2. Enable AirPlay on Mac: System Preferences → Sharing → AirPlay Receiver
3. On iPhone, open Control Center
4. Tap Screen Mirroring → Select your Mac
5. iPhone screen appears on Mac

**OBS Configuration:**
1. Add Source → "Display Capture"
2. Select the display showing iPhone mirror

### Test Flow:
1. Open Instagram on iPhone
2. Start Instagram Live (will use Ray-Ban Meta glasses camera)
3. Instagram Live should now show in OBS preview
4. OBS → Start Streaming to MetaStream
5. Check dashboard - should see Instagram Live feed

**Expected Results:**
- ✅ iPhone screen visible in OBS
- ✅ Instagram Live playing on iPhone
- ✅ OBS captures Instagram feed
- ✅ MetaStream dashboard shows the Instagram stream
- ✅ No "double encoding" artifacts
- ✅ Acceptable latency (2-5 seconds)

---

## 🚨 CRITICAL PRE-DEMO CHECKS

### 24 Hours Before Demo:
- [ ] Run full test suite end-to-end
- [ ] Verify all LO demonstrations work
- [ ] Test iPhone screen mirroring setup
- [ ] Practice demo flow 2-3 times
- [ ] Prepare talking points for each LO
- [ ] Have backup plan if WiFi fails

### 1 Hour Before Demo:
- [ ] Start FFmpeg server
- [ ] Start Java backend
- [ ] Verify both servers running
- [ ] Test one complete stream session
- [ ] Clear browser cache
- [ ] Close unnecessary applications
- [ ] Set OBS to "Studio Mode" for clean transitions

### During Demo:
- [ ] Show code files for each LO
- [ ] Demonstrate live features (chat, video)
- [ ] Show log files for LO8
- [ ] Highlight exception handling in action
- [ ] Keep terminal windows visible for logs

---

## 🛠️ TROUBLESHOOTING GUIDE

### Issue: FFmpeg not found
**Solution:**
```bash
# macOS
brew install ffmpeg

# Verify
which ffmpeg
```

### Issue: Port 8080 already in use
**Solution:**
```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Issue: WebSocket won't connect
**Solution:**
1. Check firewall settings
2. Verify Java backend started with "🔌 WebSocket endpoint" log
3. Clear browser cache
4. Try different browser

### Issue: Video won't play
**Solution:**
1. Verify OBS is streaming (green indicator)
2. Check FFmpeg logs for errors
3. Verify .m3u8 file exists:
   ```bash
   ls media-server/media/live/stream/index.m3u8
   ```
4. Try opening HLS URL directly in Safari

### Issue: Chat messages not appearing
**Solution:**
1. Check WebSocket connection status (green dot)
2. Verify backend logs show "[CHAT]" messages
3. Check browser console for errors
4. Restart WebSocket connection (refresh page)

---

## ✅ FINAL VALIDATION

**ALL SYSTEMS GO Checklist:**
- [ ] Maven build: SUCCESS
- [ ] FFmpeg server: RUNNING
- [ ] Java backend: RUNNING
- [ ] Video playback: WORKING
- [ ] WebSocket chat: WORKING
- [ ] All 8 LOs: DEMONSTRATED
- [ ] No errors in logs
- [ ] iPhone mirroring: TESTED
- [ ] Demo rehearsed: 2-3 times

---

## 📊 DEMO SUCCESS METRICS

**You're ready for demo when:**
✅ Can create stream session in <30 seconds
✅ Video loads and plays smoothly
✅ Chat works in real-time (no lag)
✅ Can explain each LO with code examples
✅ No errors or crashes during 5-minute test run
✅ iPhone screen mirroring works
✅ Can recover gracefully from any hiccup

**Time Benchmarks:**
- Full setup (start servers): 30 seconds
- Stream session creation: 15 seconds
- OBS connection: 10 seconds
- Video playback start: 5-10 seconds
- Total demo time: 5-7 minutes

---

## 🎤 DEMO SCRIPT OUTLINE

1. **Introduction (30 sec)**
   - "MetaStream Live - stream from Ray-Ban Meta glasses to web"
   
2. **Code Walkthrough (2 min)**
   - Show each LO in code
   - Highlight key design patterns
   
3. **Live Demo (2 min)**
   - Create stream session
   - Start OBS/Instagram feed
   - Show video playback
   - Demonstrate chat
   - Stop stream and show logs
   
4. **Q&A (1 min)**
   - Address professor questions
   - Highlight technical challenges solved

**TOTAL: 5-6 minutes**

---

**🎯 Remember: A working demo is better than a perfect demo. Focus on:**
- No crashes
- Clear LO demonstrations
- Smooth flow
- Confident delivery

**Good luck! 🚀**