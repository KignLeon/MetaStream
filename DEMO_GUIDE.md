# 🚀 MetaStream Live - Demo Day Quick Start Guide

## 📋 Pre-Demo Checklist (Print This!)

### 30 Minutes Before Demo:
- [ ] Close all unnecessary applications
- [ ] Clear browser cache (Cmd+Shift+Delete on Mac)
- [ ] Connect iPhone to Mac via USB (for screen mirroring)
- [ ] Test iPhone screen mirroring in QuickTime
- [ ] Verify Instagram app updated on iPhone
- [ ] Check WiFi connection is stable
- [ ] Open terminals in separate windows/tabs

### 10 Minutes Before Demo:
- [ ] Navigate to project directory
- [ ] Run validation script: `./validate.sh`
- [ ] Start FFmpeg server (Terminal 1)
- [ ] Start Java backend (Terminal 2)
- [ ] Test one complete stream session
- [ ] Clear `stream_log.txt` for clean demo

---

## 🎬 Step-by-Step Demo Script

### Setup Phase (2 minutes before demo)

**Terminal 1 - FFmpeg Server:**
```bash
cd media-server
npm start
```
**Expected Output:**
```
✅ FFmpeg found: /opt/homebrew/bin/ffmpeg
🚀 MetaStream FFmpeg-Only Server
📡 Waiting for OBS connection...
```

**Terminal 2 - Java Backend:**
```bash
java -jar target/metastream-1.0-SNAPSHOT.jar
```
**Expected Output:**
```
✅ FFmpeg media server is running
✅ MetaStream Live Backend ready at http://localhost:8080
🔌 WebSocket endpoint: ws://localhost:8080/ws
```

**Browser:**
- Open: `http://localhost:8080`
- Have OBS Studio ready but NOT streaming yet

---

## 🎤 Demo Presentation (5-7 minutes)

### PART 1: Introduction (30 seconds)
**What to say:**
> "Today I'm presenting MetaStream Live - a web application that allows streaming from Ray-Ban Meta smart glasses to a web dashboard. The system demonstrates all eight learning outcomes through a real-world streaming platform."

**What to show:**
- Point to Ray-Ban Meta glasses (if you have them)
- Show the project architecture briefly

---

### PART 2: Learning Outcomes Code Walkthrough (2-3 minutes)

#### LO1: OOP Principles - Encapsulation
**File:** `User.java` (lines 8-25)

**What to say:**
> "Starting with LO1 - OOP Principles. In the User class, we implement encapsulation with private fields and public getters and setters. Notice how username, phone, and ttsEnabled are all private, and we control access through methods like getUsername() and setPreferences()."

**What to show in IDE:**
```java
public class User {
    private final String username;  // ← Point here
    private String phone;
    private boolean ttsEnabled;
    
    public String getUsername() {   // ← Point here
        return username;
    }
}
```

---

#### LO2: Arrays & LO5: Generic Collections
**File:** `StreamSession.java` (line 18)

**What to say:**
> "For LO2 and LO5, we use an ArrayList to store ChatMessage objects. This demonstrates both arrays and generic collections - the List is parameterized with ChatMessage, providing type safety at compile time."

**What to show:**
```java
private final List<ChatMessage> messages;  // ← Point here

public StreamSession(User user) {
    this.messages = new ArrayList<>();     // ← Point here - generic instantiation
}

public void addMessage(ChatMessage msg) {
    messages.add(msg);                     // ← Type-safe addition
}
```

---

#### LO3: Classes & Aggregation
**File:** `StreamSession.java` (lines 16-17)

**What to say:**
> "LO3 - Aggregation is demonstrated here. StreamSession contains a User object and a List of ChatMessage objects. This is a 'has-a' relationship - the session has a user and has messages."

**What to show:**
```java
private final User user;                   // ← Point here - User aggregation
private final List<ChatMessage> messages;  // ← Point here - ChatMessage aggregation
```

---

#### LO4: Inheritance & Polymorphism
**Files:** `NotificationService.java`, `SMSNotifier.java`, `TTSNotifier.java`

**What to say:**
> "For LO4 - Polymorphism, we have the NotificationService interface with two implementations: SMSNotifier and TTSNotifier. Both implement the same sendNotification method, but with different behaviors. This allows us to swap implementations at runtime."

**What to show:**
```java
// NotificationService.java
public interface NotificationService {
    void sendNotification(String message);
}

// SMSNotifier.java
public class SMSNotifier implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("📱 [SMS SIMULATED] " + message);
    }
}
```

**Optional demo in terminal:**
```bash
# You can show the log output when a stream starts
# It will show simulated SMS notifications
```

---

#### LO6: GUI & Events
**Files:** `Main.java` (line 60) and `index.html`

**What to say:**
> "LO6 - GUI and Events. When a user clicks 'Create Stream Session' in the browser, JavaScript sends a POST request to this endpoint. The backend handles the event and creates a StreamSession object. This is event-driven programming - the GUI event triggers backend logic."

**What to show:**
```java
post("/api/stream/start", (req, res) -> {
    // Event handler triggered by GUI button click
    User user = new User(username);
    activeSession = new StreamSession(user);
    activeSession.startSession();
    return response;
});
```

---

#### LO7: Exception Handling
**File:** `FileLogger.java` (lines 16-30)

**What to say:**
> "LO7 - Exception Handling. When writing to files, IO operations can fail. We wrap them in try-catch blocks to handle errors gracefully. If an IOException occurs, we catch it and provide a meaningful error message instead of crashing."

**What to show:**
```java
public void writeLog(StreamSession session) {
    try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
        writer.write("Stream Session Log\n");
        // ... write data
    } catch (IOException e) {         // ← Point here
        System.err.println("⚠️ Error writing log: " + e.getMessage());
        throw new RuntimeException("Failed to write log file", e);
    }
}
```

---

#### LO8: Text File I/O
**File:** `FileLogger.java` (lines 9-30)

**What to say:**
> "Finally, LO8 - File I/O. The FileLogger writes session data to a text file when a stream ends. It uses FileWriter for writing and Files.readString for reading. This demonstrates persistent storage."

**What to show in code:**
```java
public void writeLog(StreamSession session) {
    try (FileWriter writer = new FileWriter(LOG_FILE, true)) {  // ← Write
        writer.write("User: " + session.getUser().getUsername() + "\n");
        writer.write("Duration: " + session.getDuration() + "\n");
    }
}

public String readLog() {
    return Files.readString(Paths.get(LOG_FILE));  // ← Read
}
```

**What to show in terminal after demo:**
```bash
cat stream_log.txt
```

---

### PART 3: Live Feature Demonstration (2-3 minutes)

#### Step 1: Create Stream Session
**Action:**
1. Go to `http://localhost:8080` in browser
2. Enter username: "DemoUser"
3. Check "Enable SMS & TTS"
4. Click "Create Stream Session"

**What to say:**
> "Now let's see it in action. I'm creating a stream session..."

**Expected result:**
- Success message appears
- OBS instructions display
- Backend logs: `🎬 Stream started for DemoUser`

---

#### Step 2: Start OBS Stream
**Action:**
1. Open OBS Studio
2. Show Settings → Stream configuration
3. Point out RTMP URL: `rtmp://localhost/live/stream`
4. Click "Start Streaming"

**What to say:**
> "I'm now streaming to our local media server via RTMP. OBS captures my screen (or iPhone screen mirror with Instagram Live from Ray-Ban Metas), and FFmpeg converts it to HLS for web playback."

**Expected result:**
- OBS shows green streaming indicator
- FFmpeg logs show encoding activity
- Dashboard updates

---

#### Step 3: Show Dashboard
**Action:**
1. Click "Go to Dashboard"
2. Wait for video to load

**What to say:**
> "Here's the dashboard. The video player uses HLS.js for low-latency streaming. Notice the real-time statistics - duration timer, viewer count, and message count."

**Expected result:**
- Video plays showing OBS source
- Duration timer increments
- Stats update

---

#### Step 4: Demonstrate Real-Time Chat
**Action:**
1. Type message: "Hello from the demo!"
2. Press Enter
3. Open viewer page in another tab
4. Show message appears in both places

**What to say:**
> "The chat uses WebSockets for real-time communication. When I send a message, it appears instantly for all connected viewers without refreshing the page."

**Expected result:**
- Message appears immediately
- Backend logs: `[CHAT] DemoUser: Hello from the demo!`

---

#### Step 5: Stop Stream & Show Log
**Action:**
1. Click "Stop Stream"
2. Show summary page with statistics
3. Open terminal and run: `cat stream_log.txt`

**What to say:**
> "When I stop the stream, FileLogger writes all session data to a text file. This demonstrates persistent storage using basic file I/O. Here's the log file with the complete session history."

**Expected result:**
- Summary page shows duration, messages, etc.
- Log file displays formatted session data

---

## 🛟 Emergency Backup Plans

### If Video Won't Load:
**Fallback 1:**
- Open `http://localhost:8000/live/stream/index.m3u8` directly in Safari
- Shows raw HLS stream without dashboard

**Fallback 2:**
- Show pre-recorded video/screenshot of working system
- Focus more on code walkthrough

**What to say:**
> "I'm experiencing a network delay issue, but let me show you the code architecture instead, which demonstrates all the learning outcomes..."

---

### If WebSocket Won't Connect:
**Fallback:**
- Show REST API endpoints instead
- Demonstrate with `curl` commands

**What to say:**
> "The WebSocket is having connectivity issues, but the REST API backend is working. Let me demonstrate the HTTP endpoints..."

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/stream/active
```

---

### If OBS Won't Stream:
**Fallback:**
- Skip live streaming
- Focus on code and architecture
- Show log files from previous test run

**What to say:**
> "Rather than troubleshoot OBS during demo time, let me show you the system architecture and the log files from a previous successful run..."

---

## 📊 Talking Points for Q&A

### If Asked: "How does this demonstrate real-world application?"
**Answer:**
> "This project simulates a real live-streaming platform like Twitch or YouTube Live. The architecture handles video ingestion, transcoding, and delivery - all core concepts in modern streaming services. The Ray-Ban Meta glasses add a unique wearable computing aspect."

### If Asked: "What was the biggest technical challenge?"
**Answer:**
> "The biggest challenge was integrating RTMP ingestion with HLS delivery while maintaining low latency. I initially used Node Media Server, but it wasn't compatible with macOS Apple Silicon, so I built a direct FFmpeg-based pipeline instead. This taught me to adapt when libraries don't work as expected."

### If Asked: "How did you ensure exception safety?"
**Answer:**
> "I wrapped all external operations - file I/O, HTTP requests, WebSocket messages - in try-catch blocks. For example, in FileLogger, if writing fails, we catch IOException and provide a meaningful error message. This prevents crashes and gives users feedback."

### If Asked: "Could this scale to multiple users?"
**Answer:**
> "The current architecture supports multiple viewers via WebSocket, but only one active streamer. To scale, I'd add: (1) database for session persistence, (2) load balancer for multiple backend instances, (3) CDN for HLS delivery, (4) user authentication. The OOP design with encapsulation makes these additions easier."

---

## ⏱️ Time Management

**Target: 5-7 minutes total**

- Introduction: 30 seconds
- Code walkthrough: 2 minutes (30 sec per major LO)
- Live demo: 2.5 minutes
- Stop + show logs: 30 seconds
- Q&A buffer: 1-2 minutes

**If running short on time:**
- Skip detailed code explanations for LOs 2 & 5 (similar concepts)
- Show fewer chat messages
- Skip viewer page demo

**If running over time:**
- Pause video at critical moments
- Ask professor: "Should I continue or answer questions?"

---

## ✅ Final Pre-Demo Checklist

**5 Minutes Before:**
- [ ] Both servers running without errors
- [ ] Browser tab open to localhost:8080
- [ ] OBS configured (but not streaming)
- [ ] QuickTime/iPhone mirror ready (if demoing Ray-Ban Meta feed)
- [ ] Terminal windows visible in background
- [ ] IDE open with code files ready
- [ ] `stream_log.txt` cleared or contains demo data

**During Demo:**
- [ ] Speak clearly and at moderate pace
- [ ] Point to code while explaining
- [ ] Show terminal logs for real-time validation
- [ ] Make eye contact with professor
- [ ] Anticipate questions (pause for breath)

**After Demo:**
- [ ] Thank professor for their time
- [ ] Offer to answer additional questions
- [ ] Provide GitHub link if requested

---

## 🎯 Success Indicators

**You nailed it if:**
✅ All 8 LOs clearly demonstrated in code  
✅ Live features work (video + chat)  
✅ No crashes or major errors  
✅ Confident, clear explanations  
✅ Professor nods/takes notes  

**Good enough if:**
✅ 7/8 LOs demonstrated  
✅ Code is clean even if live demo has issues  
✅ You recover gracefully from hiccups  
✅ Professor understands your design choices  

---

## 🚨 Last-Minute Sanity Checks

**Run these commands 2 minutes before demo:**

```bash
# Test 1: Backend health
curl http://localhost:8080/api/health

# Test 2: Media server health  
curl http://localhost:8000/health

# Test 3: Quick stream test
# (Create session, start OBS for 10 seconds, stop)

# Test 4: Log file exists
cat stream_log.txt
```

**All green? You're ready to go! 🚀**

---

## 💡 Pro Tips

1. **Breathe**: Pause between sentences. Silence is okay.
2. **Point**: Use your cursor to highlight code sections.
3. **Narrate**: Say what you're doing before you do it.
4. **Recover**: If something fails, stay calm and move to next point.
5. **Confidence**: You built this. You know it better than anyone.

**Remember:** The professor wants you to succeed. They're evaluating your understanding, not looking for perfection. A working demo with clear explanations of the LOs will earn full marks.

**GOOD LUCK! 🎓🚀**