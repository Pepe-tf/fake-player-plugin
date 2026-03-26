# 🤖 Bot Behaviour

> **Understanding Bot Physics, AI, and Interactions**  
> **Complete Guide to Mannequin Entities and Bot Intelligence**

---

## 🎯 Overview

FPP bots are sophisticated entities that combine **realistic tab list presence** with **physical world interaction**. Each bot consists of multiple components working together to create a seamless fake player experience.

### 🏗️ **Bot Architecture**

```
🎭 Fake Player (Bot)
├── 📊 Tab List Entry     # Shows in player list
├── 💬 Chat Presence      # Can send messages  
├── 🏃 Physical Body      # Mannequin entity (optional)
│   ├── 🎨 Skin Display   # Visual appearance
│   ├── 👀 Head AI        # Head rotation tracking
│   ├── 🥊 Combat System  # Damage and death
│   └── 📍 Physics       # Movement and collision
└── 🧠 AI Systems        # Behavior and intelligence
    ├── 💭 Chat AI        # Message broadcasting
    ├── 🔄 Swap AI        # Player replacement
    └── 📡 Chunk Loading  # World presence
```

---

## 🏃 **Physical Bodies (Mannequins)**

### 🎮 **Entity Structure**

Each bot's physical presence consists of **two entities**:

1. **👤 Visual Entity (Mannequin)**
   - Player-shaped model with skin
   - Proper hitbox and collision
   - Physics and movement capabilities
   - Tagged with `FakePlayerBody.VISUAL_PDC_VALUE`

2. **📛 Nametag Entity (ArmorStand)**  
   - Invisible ArmorStand riding the Mannequin
   - Displays bot name above head
   - Tagged with `FakePlayerBody.NAMETAG_PDC_VALUE`

### ⚙️ **Body Configuration**

**Main Settings (config.yml):**
```yaml
body:
  spawn-body: true           # Enable physical bodies
  pushable: true             # Players can push bots
  damageable: false          # Bots take no damage
  max-health: 20.0           # Bot health points
```

**Entity Behavior:**
```yaml
body:
  immovable: false           # Bots can be moved by physics
  invulnerable: true         # Immune to all damage when damageable: false
```

### 🎨 **Visual Appearance**

**Skin System Integration:**
- Bots display configured skins from the [Skin System](Skin-System.md)
- Skins update automatically when changed
- Fallback to default Steve/Alex if skin fails

**Display Name:**
- Shows bot name in configurable format
- Supports LuckPerms prefixes/suffixes
- Color codes and formatting supported

---

## 👀 **Head AI System**

### 🧠 **Intelligent Head Tracking**

Bots can **intelligently track nearby players** by rotating their heads, creating realistic interaction behavior.

**Configuration (config.yml):**
```yaml
bot-head-ai:
  enabled: true              # Enable head rotation
  range: 8.0                 # Detection range (blocks)
  update-interval: 10        # Update frequency (ticks)
  max-rotation-speed: 30.0   # Maximum degrees per update
```

### 🎯 **Tracking Behavior**

**Target Priority:**
1. **Closest player** within range
2. **Players moving** (dynamic targeting)
3. **Players looking at bot** (mutual attention)

**Rotation Mechanics:**
- **Smooth rotation** — No instant snapping
- **Natural limits** — Realistic head movement range  
- **Performance optimized** — Updates only when needed
- **Multi-player support** — Tracks multiple players intelligently

**Visual Effect:**
- Bot head follows players naturally
- Creates impression of awareness and intelligence
- Enhances immersion for players interacting with bots

---

## 🥊 **Combat & Damage System**

### ⚔️ **Damage Mechanics**

**Damage Configuration (config.yml):**
```yaml
body:
  damageable: true           # Bots can take damage
  max-health: 20.0           # Health points (default player health)

bot-combat:
  death-enabled: true        # Bots can die
  respawn-enabled: false     # Auto-respawn after death
  damage-sounds: true        # Play hurt/death sounds
```

### 💀 **Death System**

**When Bot Dies:**
1. **Death Animation** — Plays death animation and sound
2. **Entity Cleanup** — Removes both Mannequin and nametag
3. **Tab List Update** — Bot disappears from player list
4. **Event Logging** — Records death in database (if enabled)

**Death Sources:**
- ✅ Player attacks (sword, bow, etc.)
- ✅ Environmental damage (lava, fall damage, drowning)
- ✅ Entity damage (monsters, explosions)
- ❌ Plugin damage (when `damageable: false`)

**Respawn Options:**
```yaml
bot-combat:
  respawn-enabled: true      # Enable auto-respawn
  respawn-delay: 100         # Delay in ticks (5 seconds)
  respawn-location: "spawn"  # "spawn", "death", or "original"
```

---

## 🎾 **Physics & Collision**

### 🏃 **Movement Physics**

Bots support **realistic physics simulation** including collision, pushing, and movement.

**Push Configuration (config.yml):**
```yaml
bot-collision:
  enabled: true              # Enable collision physics

  # Player → Bot collision
  walk-radius: 1.5           # Activation distance  
  walk-strength: 0.3         # Push force multiplier
  
  # Player hits Bot collision  
  hit-strength: 0.8          # Punch force multiplier
  
  # Bot → Bot collision
  bot-radius: 1.0            # Bot separation distance
  bot-strength: 0.2          # Inter-bot push force
  
  # Movement limits
  max-horizontal-speed: 2.0  # Maximum push speed
  vertical-damping: 0.1      # Reduce vertical movement
```

### 🎯 **Collision Types**

**1. Player Walks Into Bot**
- **Trigger:** Player gets within `walk-radius`
- **Effect:** Bot is gently pushed aside with `walk-strength`
- **Use Case:** Natural movement around bots

**2. Player Hits Bot**  
- **Trigger:** Player attacks bot (punch, weapon)
- **Effect:** Stronger push with `hit-strength`
- **Use Case:** Intentional bot positioning

**3. Bot Bumps Bot**
- **Trigger:** Two bots get within `bot-radius` of each other
- **Effect:** Bots push apart with `bot-strength`  
- **Use Case:** Prevents bot clustering

### ⚡ **Performance Optimization**

**Physics Settings:**
```yaml
bot-collision:
  update-interval: 5         # Physics update frequency (ticks)
  max-entities-per-tick: 10  # Limit collision calculations
  enable-optimization: true  # Use spatial optimization
```

**Optimization Features:**
- **Spatial partitioning** — Only check nearby entities
- **Update throttling** — Limit calculations per tick
- **Distance culling** — Skip distant collision checks
- **Smart activation** — Only process when needed

---

## 📡 **Chunk Loading System**

### 🌍 **World Presence**

Bots can **keep chunks loaded** around their location to maintain world presence even when no players are nearby.

**Configuration (config.yml):**
```yaml
chunk-loading:
  enabled: true              # Enable chunk loading
  radius: 2                  # Chunks to keep loaded (radius)
  unload-delay: 300          # Delay before unloading (seconds)
  max-loaded-chunks: 50      # Global limit on bot-loaded chunks
```

### 📊 **Chunk Management**

**Loading Behavior:**
- **Load on spawn** — Bot loads surrounding chunks when created
- **Maintain presence** — Keeps chunks loaded while bot exists
- **Smart unloading** — Releases chunks when bot is removed
- **Memory management** — Respects server chunk limits

**Radius Examples:**
- `radius: 1` — 9 chunks (3x3 area)
- `radius: 2` — 25 chunks (5x5 area)  
- `radius: 3` — 49 chunks (7x7 area)

**Performance Impact:**
```yaml
chunk-loading:
  enabled: false             # Disable if causing lag
  radius: 1                  # Minimize radius for performance
```

---

## 💭 **Chat AI System**

### 🗨️ **Intelligent Messaging**

Bots can **automatically send chat messages** to create realistic server activity and engagement.

**Basic Configuration (config.yml):**
```yaml
fake-chat:
  enabled: true              # Enable fake chat
  interval: 300-900          # Message frequency (seconds)
  chat-format: "&7{prefix}{bot_name}{suffix}: {message}"
```

### 📝 **Message System**

**Message Pool (bot-messages.yml):**
```yaml
messages:
  general:
    - "Hello everyone!"
    - "How's everyone doing?"
    - "Nice weather today"
    
  greetings:
    - "Good morning!"
    - "Hey there!"
    
  questions:
    - "Anyone want to build something?"
    - "What's everyone up to?"
```

**Chat Features:**
- **Random selection** — Messages chosen from pools randomly
- **Timing variation** — Interval ranges prevent predictable patterns  
- **LuckPerms integration** — Prefixes and suffixes in messages
- **Color support** — Full MiniMessage and legacy color codes

### 🎯 **Advanced Chat Behavior**

**Smart Messaging:**
```yaml
fake-chat:
  player-requirement: 2      # Only chat when 2+ real players online
  cooldown: 60               # Minimum delay between bot messages
  max-per-hour: 10           # Limit messages per bot per hour
```

**Contextual Messages:**
- **Time-based** — Different messages for day/night
- **Event-triggered** — Messages on player join/leave
- **Location-aware** — Different messages per world

---

## 🔄 **Swap AI System**

### 🎭 **Player Replacement**

The **Swap System** can **automatically replace offline players** with bots to maintain consistent server population.

**Configuration (config.yml):**
```yaml
swap-system:
  enabled: true              # Enable player swapping
  delay: 300                 # Seconds before swapping offline player
  probability: 0.7           # Chance to swap (70%)
  max-swaps: 10              # Maximum simultaneous swaps
```

### 🧠 **Swap Intelligence**

**Replacement Logic:**
1. **Monitor departures** — Track when players leave
2. **Wait period** — Delay before considering swap
3. **Eligibility check** — Verify player can be swapped
4. **Bot creation** — Spawn bot with similar characteristics
5. **Seamless transition** — Maintain server activity appearance

**Smart Features:**
- **Whitelist protection** — VIP players never swapped
- **Permission respect** — Honors bypass permissions
- **Natural behavior** — Bots act like the replaced player
- **Automatic cleanup** — Removes bots when player returns

### 🎯 **Swap Targeting**

**Player Selection:**
```yaml
swap-system:
  min-playtime: 1800         # Minimum seconds online before swapping
  whitelist-enabled: true    # Use whitelist protection
  respect-permissions: true  # Check swap bypass permissions
```

**Protected Players:**
- Players with `fpp.bypass.swap` permission
- Players in configured whitelist
- Players online less than minimum time
- Staff members (configurable)

---

## 📊 **Performance & Optimization**

### ⚡ **System Performance**

**Bot Limits:**
```yaml
global-bot-limit: 50         # Total bots on server
performance:
  max-ai-updates: 20         # AI updates per tick
  physics-interval: 5        # Physics update frequency
  cleanup-interval: 1200     # Cleanup frequency (ticks)
```

### 📈 **Optimization Strategies**

**1. AI Optimization:**
- **Update throttling** — Spread AI calculations across ticks
- **Distance culling** — Disable AI when no players nearby  
- **Smart activation** — Only run needed AI systems
- **Batch processing** — Group similar calculations

**2. Physics Optimization:**
- **Spatial indexing** — Efficient collision detection
- **Update limiting** — Control physics calculation frequency
- **LOD system** — Reduce detail for distant bots
- **Memory pooling** — Reuse calculation objects

**3. Network Optimization:**
- **Packet batching** — Group network updates
- **Update filtering** — Send only necessary changes
- **Compression** — Optimize packet size
- **Rate limiting** — Control update frequency

### 🎯 **Monitoring Performance**

**Built-in Diagnostics:**
```bash
/fpp stats --detailed       # Comprehensive bot statistics
/timings report             # Server performance analysis
```

**Performance Metrics:**
- **Bot count** — Total active bots
- **AI load** — Processing time per tick
- **Physics load** — Collision calculation time
- **Memory usage** — RAM consumed by bot systems
- **Network usage** — Packets sent for bot updates

---

## 🛠️ **Advanced Configuration**

### 🔧 **Compatibility Mode**

For servers with limited Mannequin support:

```yaml
compatibility:
  restricted-mode: true      # Auto-detected
  disable-bodies: true       # Force disable physical bodies
  disable-physics: true      # Disable collision system
  disable-head-ai: true      # Disable head rotation
```

**Restricted Mode Effects:**
- **Tab-only bots** — Bots appear in tab list only
- **No physical presence** — No entities in world
- **Chat functionality** — Fake chat still works
- **Swap system** — Player replacement still available

### 🎮 **Advanced Physics**

**Detailed Physics Control:**
```yaml
bot-collision:
  # Advanced collision settings
  friction: 0.8              # Surface friction coefficient
  restitution: 0.1           # Bounce factor
  gravity-factor: 1.0        # Gravity strength multiplier
  
  # Collision layers
  player-layer: true         # Collision with players
  entity-layer: false        # Collision with mobs
  block-layer: true          # Collision with blocks
  
  # Performance tuning  
  spatial-hash-size: 16      # Spatial optimization grid size
  max-iterations: 10         # Physics solver iterations
```

### 🧠 **AI Behavior Tuning**

**Head AI Advanced Settings:**
```yaml
bot-head-ai:
  # Targeting behavior
  player-priority: true      # Prefer players over other targets
  movement-attraction: 1.5   # Attraction to moving targets
  look-attraction: 2.0       # Attraction when looked at
  
  # Rotation mechanics
  smoothing: 0.8             # Rotation smoothing factor
  prediction: true           # Predict player movement
  max-angle: 90              # Maximum head rotation (degrees)
  
  # Performance
  update-distance: 32        # Maximum tracking distance
  cpu-budget: 0.5            # Milliseconds per tick budget
```

---

## 🎯 **Use Cases & Examples**

### 🏢 **Server Population Management**

**Scenario:** Maintain active appearance during off-peak hours
```yaml
# Configuration for population bots
body:
  spawn-body: true           # Visible presence in world
  pushable: false            # Prevent griefing
  damageable: false          # Immortal population bots

fake-chat:
  enabled: true              # Create chat activity
  interval: 600-1800         # Moderate message frequency
  
swap-system:
  enabled: true              # Replace offline players
  delay: 180                 # Quick replacement
```

### 🎮 **Minigame NPCs**

**Scenario:** Create interactive NPCs for game lobbies
```yaml
# NPC-like behavior
bot-head-ai:
  enabled: true              # Acknowledge players
  range: 5.0                 # Close interaction range
  
body:
  pushable: false            # Static positioning
  damageable: false          # Immortal NPCs
  
chunk-loading:
  enabled: true              # Always loaded
  radius: 1                  # Minimal impact
```

### 🏰 **Roleplay Characters**

**Scenario:** Populate towns with realistic inhabitants
```yaml
# Realistic town folk
fake-chat:
  enabled: true              # Roleplay conversations
  chat-format: "&f{prefix}{bot_name}: {message}"
  
bot-collision:
  enabled: true              # Natural movement
  walk-strength: 0.1         # Gentle displacement
  
bot-head-ai:
  enabled: true              # Social awareness
  range: 6.0                 # Natural interaction range
```

---

## 🔍 **Troubleshooting**

### ❌ **Common Issues**

**Bots Not Moving/Rotating:**
- ✅ Check `bot-head-ai.enabled: true`
- ✅ Verify `body.spawn-body: true`
- ✅ Ensure players are within range
- ✅ Check compatibility mode status

**Performance Problems:**
- ✅ Reduce bot limits in config
- ✅ Disable unnecessary AI systems
- ✅ Increase update intervals
- ✅ Monitor with `/fpp stats --detailed`

**Physics Not Working:**
- ✅ Verify `bot-collision.enabled: true`
- ✅ Check `body.pushable: true`  
- ✅ Ensure physical bodies are enabled
- ✅ Test collision ranges and strengths

### 🛠️ **Diagnostic Commands**

```bash
/fpp stats --detailed       # Performance and behavior stats
/fpp info <botname>         # Individual bot status
/fpp freeze <botname>       # Test bot responsiveness
/fpp list --frozen          # Check frozen bot status
```

---

## 📚 **Related Documentation**

- **[Skin System](Skin-System.md)** — Bot visual appearance
- **[Fake Chat](Fake-Chat.md)** — Chat AI configuration  
- **[Swap System](Swap-System.md)** — Player replacement details
- **[Configuration](Configuration.md)** — All behavior settings
- **[Performance Guide](FAQ.md#performance-issues)** — Optimization tips

---

**🤖 Master bot behavior to create the most realistic and engaging fake players!**
