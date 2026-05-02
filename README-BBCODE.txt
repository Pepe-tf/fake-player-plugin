[CENTER][SIZE=7][B]ꜰᴀᴋᴇ ᴘʟᴀʏᴇʀ ᴘʟᴜɢɪɴ (FPP)[/B][/SIZE]

[SIZE=5][I]Spawn realistic fake players on your Paper server — with tab list presence, server list count, join/leave messages, in-world bodies, guaranteed skins, chunk loading, bot swap/rotation, fake chat, AI conversations, area mining, block placing, pathfinding, follow-target automation, per-bot settings GUI, per-bot swim AI & chunk-radius overrides, per-bot PvE attack settings, per-bot XP & item pickup control, tab-list ping simulation, NameTag plugin integration, LuckPerms integration, proxy network support, Velocity companion plugin, BungeeCord companion plugin, full Paper 1.21.x compatibility (1.21.0–1.21.11), and full hot-reload.[/I][/SIZE]

[SIZE=4][B]Version:[/B] 1.6.6.8  [B]Minecraft:[/B] 1.21.x  [B]Platform:[/B] Paper / Folia  [B]Java:[/B] 21+[/SIZE]

[URL='https://modrinth.com/plugin/fake-player-plugin-(fpp)'][B][COLOR=#00AF5C]⬇ Download on Modrinth[/COLOR][/B][/URL]  [URL='https://www.spigotmc.org/resources/fake-player-plugin-fpp.133572/'][B][COLOR=#FF6B35]⬇ SpigotMC[/COLOR][/B][/URL]  [URL='https://hangar.papermc.io/Pepe-tf/FakePlayerPlugin'][B][COLOR=#00BFD8]⬇ PaperMC Hangar[/COLOR][/B][/URL]  [URL='https://builtbybit.com/resources/fake-player-plugin.98704/'][B][COLOR=#A855F7]⬇ BuiltByBit[/COLOR][/B][/URL]
[URL='https://discord.gg/QSN7f67nkJ'][B][COLOR=#5865F2]💬 Join Discord[/COLOR][/B][/URL]  [URL='https://fpp.wtf'][B][COLOR=#7B8EF0]📖 Wiki[/COLOR][/B][/URL]  [URL='https://ko-fi.com/fakeplayerplugin'][B][COLOR=#FF5E5B]☕ Support on Ko-fi[/COLOR][/B][/URL]  [URL='https://github.com/sponsors/Pepe-tf'][B][COLOR=#EA4AAA]💖 GitHub Sponsors[/COLOR][/B][/URL]  [URL='https://www.patreon.com/c/F_PP?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink'][B][COLOR=#FF424D]🎗 Patreon[/COLOR][/B][/URL]
[/CENTER]

[HR][/HR]

[SIZE=6][B]✨ What It Does[/B][/SIZE]

FPP adds fake players to your server that look and behave like real ones:

[LIST]
[*]Show up in the [B]tab list[/B] and [B]server list player count[/B]
[*]Broadcast [B]join, leave, and kill messages[/B]
[*]Spawn as [B]physical NMS ServerPlayer entities[/B] — pushable, damageable, solid
[*]Always have a [B]real skin[/B] (guaranteed fallback chain — never Steve/Alex unless you want it)
[*][B]Load chunks[/B] around them exactly like a real player
[*][B]Rotate their head[/B] to face nearby players
[*][B]Swim automatically[/B] in water and lava — mimics a real player holding spacebar
[*][B]Send fake chat messages[/B] from a configurable message pool (with LP prefix/suffix support, typing delays, burst messages, mention replies, and event reactions)
[*][B]Swap in and out[/B] automatically with fresh names and personalities
[*][B]Persist across restarts[/B] — they come back where they left off
[*][B]Freeze[/B] any bot in place with [FONT=monospace]/fpp freeze[/FONT]
[*][B]Open bot inventory[/B] — 54-slot GUI with equipment slots; right-click any bot entity to open
[*][B]Pathfind to players[/B] — A* grid navigation with WALK, ASCEND, DESCEND, PARKOUR, BREAK, PLACE move types
[*][B]Mine blocks[/B] — continuous or one-shot block breaking; area selection with pos1/pos2 cuboid mode
[*][B]Place blocks[/B] — continuous block placing with per-bot supply container support
[*][B]Right-click automation[/B] — assign a command to any bot; right-clicking it runs the command
[*][B]Transfer XP[/B] — drain a bot's entire XP pool to yourself with [FONT=monospace]/fpp xp[/FONT]
[*][B]Named waypoint routes[/B] — save patrol routes; bots walk them on a loop with [FONT=monospace]/fpp move --wp[/FONT]
[*][B]Rename bots[/B] — rename any active bot with full state preservation (inventory, XP, LP group, tasks)
[*][B]Per-bot settings GUI[/B] — shift+right-click any bot to open a 6-row settings chest (General · Chat · PvE · Pathfinding · Danger)
[*][B]AI conversations[/B] — bots respond to [FONT=monospace]/msg[/FONT] with AI-generated replies; 7 providers (OpenAI, Groq, Anthropic, Gemini, Ollama, Copilot, Custom); per-bot personalities via [FONT=monospace]personalities/[/FONT] folder
[*][B]Badword filter[/B] — case-insensitive with leet-speak normalization, auto-rename bad names, remote word list
[*][B]Set bot ping[/B] — simulate realistic tab-list latency per bot with [FONT=monospace]/fpp ping[/FONT]; fixed, random, or bulk modes
[*][B]PvE attack automation[/B] — bots attack nearby entities, auto-target mobs ([FONT=monospace]--mob[/FONT]), pursue targets ([FONT=monospace]--move[/FONT]), or roam-hunt ([FONT=monospace]--hunt[/FONT]) with [FONT=monospace]/fpp attack[/FONT]
[*][B]Per-bot PvE smart attack[/B] — tri-state OFF / ON (still) / ON (move) configurable per-bot via BotSettingGui; mob type selector, range, priority
[*][B]Follow-target automation[/B] — bots continuously follow any online player with [FONT=monospace]/fpp follow[/FONT]; path recalculates as target moves, persists across restarts
[*][B]Skin command[/B] — apply any Mojang skin, URL skin, or reset with [FONT=monospace]/fpp skin <bot> <username|url|reset>[/FONT]
[*][B]Skin persistence[/B] — resolved skins saved to DB and re-applied on restart without a new Mojang API round-trip
[*][B]Per-bot pathfinding overrides[/B] — parkour, break-blocks, place-blocks, nav-avoid-water, nav-avoid-lava configurable per-bot via BotSettingGui
[*][B]Per-bot respawn-on-death[/B] — bots auto-respawn after death instead of being removed
[*][B]Per-bot auto-eat / auto-place-bed[/B] — realistic survival overrides per bot
[*][B]Bot select menu[/B] — [FONT=monospace]/fpp bots[/FONT] opens a paginated GUI of your manageable bots
[*][B]Save command[/B] — [FONT=monospace]/fpp save[/FONT] immediately checkpoints all bot data
[*][B]Set owner[/B] — [FONT=monospace]/fpp setowner <bot> <player>[/FONT] transfers bot ownership
[*][B]NameTag integration[/B] — nick-conflict guard, bot isolation from nick cache, skin sync, auto-rename via nick
[*][B]LuckPerms[/B] — per-bot group assignment, weighted tab-list ordering, prefix/suffix in chat and nametags
[*][B]Proxy/network support[/B] — Velocity & BungeeCord cross-server chat, alerts, and shared database
[*][B]Velocity companion[/B] ([FONT=monospace]fpp-velocity.jar[/FONT]) — drop into your Velocity proxy's [FONT=monospace]plugins/[/FONT] folder to inflate the server-list player count and hover list with FPP bots; includes an anti-scam startup warning
[*][B]BungeeCord companion[/B] ([FONT=monospace]fpp-bungee.jar[/FONT]) — identical feature set for BungeeCord/Waterfall networks; drop into your BungeeCord [FONT=monospace]plugins/[/FONT] folder; no configuration needed
[*][B]Config sync[/B] — push/pull configuration files across your proxy network
[*][B]PlaceholderAPI[/B] — 29+ placeholders including per-world bot counts, network state, proxy-aware counts, and spawn cooldown
[*][B]Extension / Addon API[/B] — drop [FONT=monospace].jar[/FONT] files into [FONT=monospace]plugins/FakePlayerPlugin/extensions/[/FONT] to load third-party addons
[*][B]Random name generator[/B] — [FONT=monospace]bot-name.mode: random[/FONT] generates realistic Minecraft-style usernames on the fly
[*][B]Find command[/B] — bots scan nearby chunks for target blocks and mine them progressively
[*][B]Bot groups[/B] — personal bot groups with GUI management for bulk commands
[*][B]WorldEdit integration[/B] — [FONT=monospace]--wesel[/FONT] flag for mine/place uses your WorldEdit selection
[*][B]Automation[/B] — [FONT=monospace]auto-eat[/FONT] and [FONT=monospace]auto-place-bed[/FONT] defaults for realistic bot survival behaviour
[*][B]Folia support[/B] — compatible with Folia's regionised threading model
[*]Fully [B]hot-reloadable[/B] — no restarts needed
[/LIST]

[HR][/HR]

[SIZE=6][B]📋 Requirements[/B][/SIZE]

[TABLE="width: 100%"]
[TR][TD][B]Requirement[/B][/TD][TD][B]Version[/B][/TD][/TR]
[TR][TD][URL='https://papermc.io/downloads/paper']Paper[/URL][/TD][TD]1.21.x[/TD][/TR]
[TR][TD]Java[/TD][TD]21+[/TD][/TR]
[TR][TD][URL='https://luckperms.net']LuckPerms[/URL][/TD][TD]Optional — auto-detected[/TD][/TR]
[TR][TD][URL='https://www.spigotmc.org/resources/placeholderapi.6245/']PlaceholderAPI[/URL][/TD][TD]Optional — auto-detected (29+ placeholders)[/TD][/TR]
[TR][TD][URL='https://dev.bukkit.org/projects/worldguard']WorldGuard[/URL][/TD][TD]Optional — auto-detected (no-PvP region protection)[/TD][/TR]
[TR][TD][URL='https://enginehub.org/worldedit/']WorldEdit[/URL][/TD][TD]Optional — auto-detected ([FONT=monospace]--wesel[/FONT] flag for mine/place)[/TD][/TR]
[TR][TD][URL='https://lode.gg']NameTag[/URL][/TD][TD]Optional — auto-detected (nick-conflict guard, skin sync)[/TD][/TR]
[/TABLE]

[B]Note:[/B] Supports all Paper 1.21.x versions (1.21.0 through 1.21.11). Check the server console after startup for any version-specific notes.

[I]SQLite is bundled — no database setup required. MySQL is available for multi-server/proxy setups.[/I]

[HR][/HR]

[SIZE=6][B]🚀 Installation[/B][/SIZE]

[LIST=1]
[*]Download the latest [FONT=monospace]fpp-*.jar[/FONT] from [URL='https://modrinth.com/plugin/fake-player-plugin-(fpp)/versions']Modrinth[/URL] and place it in your [FONT=monospace]plugins/[/FONT] folder.
[*]Restart your server — config files are created automatically.
[*]Edit [FONT=monospace]plugins/FakePlayerPlugin/config.yml[/FONT] to your liking.
[*]Run [FONT=monospace]/fpp reload[/FONT] to apply changes at any time.
[/LIST]

[B]Updating?[/B] FPP automatically migrates your config on first start and creates a timestamped backup before changing anything.

[HR][/HR]

[SIZE=6][B]🎮 Commands[/B][/SIZE]

All commands are under [FONT=monospace]/fpp[/FONT] (aliases: [FONT=monospace]/fakeplayer[/FONT], [FONT=monospace]/fp[/FONT]).

[TABLE="width: 100%"]
[TR][TD][B]Command[/B][/TD][TD][B]Description[/B][/TD][/TR]
[TR][TD][FONT=monospace]/fpp[/FONT][/TD][TD]Plugin info — version, active bots, download links[/TD][/TR]
[TR][TD][FONT=monospace]/fpp help [page][/FONT][/TD][TD]Interactive GUI help menu — paginated, permission-filtered, click-navigable[/TD][/TR]
[TR][TD][FONT=monospace]/fpp spawn [amount] [--name <name>][/FONT][/TD][TD]Spawn fake player(s) at your location[/TD][/TR]
[TR][TD][FONT=monospace]/fpp despawn <name|all|--random [n]|--num <n>>[/FONT][/TD][TD]Remove a bot by name, remove all, remove random N, or remove N oldest (blocked during persistence restore)[/TD][/TR]
[TR][TD][FONT=monospace]/fpp list[/FONT][/TD][TD]List all active bots with uptime and location[/TD][/TR]
[TR][TD][FONT=monospace]/fpp freeze <name|all> [on|off][/FONT][/TD][TD]Freeze or unfreeze bots — frozen bots are immovable; shown with ❄ in list/stats[/TD][/TR]
[TR][TD][FONT=monospace]/fpp inventory <bot>[/FONT][/TD][TD]Open the bot's full 54-slot inventory GUI (alias: /fpp inv)[/TD][/TR]
[TR][TD][FONT=monospace]/fpp move <bot> <player>[/FONT][/TD][TD]Navigate a bot to an online player using A* pathfinding[/TD][/TR]
[TR][TD][FONT=monospace]/fpp move <bot> --wp <route>[/FONT][/TD][TD]Patrol a named waypoint route on a loop[/TD][/TR]
[TR][TD][FONT=monospace]/fpp move <bot> --stop[/FONT][/TD][TD]Stop the bot's current navigation[/TD][/TR]
[TR][TD][FONT=monospace]/fpp mine <bot> [once|stop][/FONT][/TD][TD]Continuous or one-shot block mining[/TD][/TR]
[TR][TD][FONT=monospace]/fpp mine <bot> --pos1|--pos2|--start|--status|--stop[/FONT][/TD][TD]Area-selection cuboid mining mode[/TD][/TR]
[TR][TD][FONT=monospace]/fpp place <bot> [once|stop][/FONT][/TD][TD]Continuous or one-shot block placing[/TD][/TR]
[TR][TD][FONT=monospace]/fpp storage <bot> [name|--list|--remove|--clear][/FONT][/TD][TD]Register supply containers for mine/place restocking[/TD][/TR]
[TR][TD][FONT=monospace]/fpp use <bot>[/FONT][/TD][TD]Bot right-clicks / activates the block it's looking at[/TD][/TR]
[TR][TD][FONT=monospace]/fpp waypoint <name> [create|add|remove|list|clear][/FONT][/TD][TD]Manage named patrol route waypoints ([FONT=monospace]add[/FONT] auto-creates the route)[/TD][/TR]
[TR][TD][FONT=monospace]/fpp xp <bot>[/FONT][/TD][TD]Transfer all of a bot's XP to yourself[/TD][/TR]
[TR][TD][FONT=monospace]/fpp cmd <bot> <command>[/FONT][/TD][TD]Execute a command on a bot; --add/--clear/--show manage its stored right-click command[/TD][/TR]
[TR][TD][FONT=monospace]/fpp rename <old> <new>[/FONT][/TD][TD]Rename a bot preserving all state (inventory, XP, LP group, tasks)[/TD][/TR]
[TR][TD][FONT=monospace]/fpp personality <bot> set|reset|show[/FONT][/TD][TD]Assign or clear AI personality per bot[/TD][/TR]
[TR][TD][FONT=monospace]/fpp personality list|reload[/FONT][/TD][TD]List available personality files or reload them[/TD][/TR]
[TR][TD][FONT=monospace]/fpp ping [<bot>] [--ping <ms>|--random] [--count <n>][/FONT][/TD][TD]Set simulated tab-list ping for one or all bots[/TD][/TR]
[TR][TD][FONT=monospace]/fpp attack <bot> [--stop][/FONT][/TD][TD]Bot walks to sender and attacks nearby entities (PvE); --mob for stationary mob-targeting; --mob --move to pursue; --hunt for roaming hunt[/TD][/TR]
[TR][TD][FONT=monospace]/fpp follow <bot|all> <player>[/FONT][/TD][TD]Bot continuously follows an online player; path recalculates as target moves[/TD][/TR]
[TR][TD][FONT=monospace]/fpp follow <bot|all> --stop[/FONT][/TD][TD]Stop the bot's current follow loop[/TD][/TR]
[TR][TD][FONT=monospace]/fpp sleep <bot|all> <x y z> <radius>[/FONT][/TD][TD]Set a sleep-origin so the bot auto-sleeps at night near that location[/TD][/TR]
[TR][TD][FONT=monospace]/fpp sleep <bot|all> --stop[/FONT][/TD][TD]Clear the bot's sleep-origin[/TD][/TR]
[TR][TD][FONT=monospace]/fpp stop [<bot>|all][/FONT][/TD][TD]Cancel all active tasks for a bot (move, mine, place, use, attack, follow, sleep)[/TD][/TR]
[TR][TD][FONT=monospace]/fpp find <bot> <block> [--radius <n>] [--count <n>][/FONT][/TD][TD]Bot scans nearby chunks for target blocks and mines them progressively[/TD][/TR]
[TR][TD][FONT=monospace]/fpp groups [gui|list|create|delete|add|remove][/FONT][/TD][TD]Personal bot groups with GUI management[/TD][/TR]
[TR][TD][FONT=monospace]/fpp save[/FONT][/TD][TD]Immediately save all active bot data to disk[/TD][/TR]
[TR][TD][FONT=monospace]/fpp setowner <bot> <player>[/FONT][/TD][TD]Transfer ownership of a bot to another player[/TD][/TR]
[TR][TD][FONT=monospace]/fpp bots [bot][/FONT][/TD][TD]Open paginated GUI of your manageable bots (aliases: mybots, botmenu)[/TD][/TR]
[TR][TD][FONT=monospace]/fpp skin <bot> <username|url|reset>[/FONT][/TD][TD]Apply or reset a skin for a specific bot[/TD][/TR]
[TR][TD][FONT=monospace]/fpp badword add|remove|list|reload[/FONT][/TD][TD]Manage the runtime badword filter list[/TD][/TR]
[TR][TD][FONT=monospace]/fpp chat [on|off|status][/FONT][/TD][TD]Toggle the fake chat system[/TD][/TR]
[TR][TD][FONT=monospace]/fpp swap [on|off|status|now <bot>|list|info <bot>][/FONT][/TD][TD]Toggle / manage the bot swap/rotation system[/TD][/TR]
[TR][TD][FONT=monospace]/fpp peaks [on|off|status|next|force|list|wake <name>|sleep <name>][/FONT][/TD][TD]Time-based bot pool scheduler[/TD][/TR]
[TR][TD][FONT=monospace]/fpp rank <bot> <group>[/FONT][/TD][TD]Assign a specific bot to a LuckPerms group[/TD][/TR]
[TR][TD][FONT=monospace]/fpp rank random <group> [num|all][/FONT][/TD][TD]Assign random bots to a LuckPerms group[/TD][/TR]
[TR][TD][FONT=monospace]/fpp rank list[/FONT][/TD][TD]List all active bots with their current LuckPerms group[/TD][/TR]
[TR][TD][FONT=monospace]/fpp lpinfo [bot-name][/FONT][/TD][TD]LuckPerms diagnostic info — prefix, weight, rank, ordering[/TD][/TR]
[TR][TD][FONT=monospace]/fpp stats[/FONT][/TD][TD]Live statistics panel — bots, frozen, system status, DB totals, TPS[/TD][/TR]
[TR][TD][FONT=monospace]/fpp info [bot <name> | spawner <name>][/FONT][/TD][TD]Query the session database[/TD][/TR]
[TR][TD][FONT=monospace]/fpp tp <name>[/FONT][/TD][TD]Teleport yourself to a bot[/TD][/TR]
[TR][TD][FONT=monospace]/fpp tph [name][/FONT][/TD][TD]Teleport your bot to yourself[/TD][/TR]
[TR][TD][FONT=monospace]/fpp settings[/FONT][/TD][TD]Open the in-game settings GUI — toggle config values live[/TD][/TR]
[TR][TD][FONT=monospace]/fpp alert <message>[/FONT][/TD][TD]Broadcast an admin message network-wide (proxy)[/TD][/TR]
[TR][TD][FONT=monospace]/fpp sync push [file][/FONT][/TD][TD]Upload config file(s) to the proxy network[/TD][/TR]
[TR][TD][FONT=monospace]/fpp sync pull [file][/FONT][/TD][TD]Download config file(s) from the proxy network[/TD][/TR]
[TR][TD][FONT=monospace]/fpp sync status [file][/FONT][/TD][TD]Show sync status and version info[/TD][/TR]
[TR][TD][FONT=monospace]/fpp sync check [file][/FONT][/TD][TD]Check for local changes vs network version[/TD][/TR]
[TR][TD][FONT=monospace]/fpp migrate[/FONT][/TD][TD]Backup, migration, and export tools[/TD][/TR]
[TR][TD][FONT=monospace]/fpp reload[/FONT][/TD][TD]Hot-reload all config, language, skins, name/message pools[/TD][/TR]
[/TABLE]

[HR][/HR]

[SIZE=6][B]🔑 Permissions[/B][/SIZE]

[SIZE=5][B]Admin[/B][/SIZE] [I](fpp.op — default: op)[/I]

[TABLE="width: 100%"]
[TR][TD][B]Permission[/B][/TD][TD][B]Description[/B][/TD][/TR]
[TR][TD][FONT=monospace]fpp.op[/FONT][/TD][TD]All admin commands (admin wildcard, default: op)[/TD][/TR]
[TR][TD][FONT=monospace]fpp.spawn[/FONT][/TD][TD]Spawn bots (unlimited, supports --name and multi-spawn)[/TD][/TR]
[TR][TD][FONT=monospace]fpp.delete[/FONT][/TD][TD]Remove bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.list[/FONT][/TD][TD]List all active bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.freeze[/FONT][/TD][TD]Freeze / unfreeze any bot or all bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.chat[/FONT][/TD][TD]Toggle fake chat[/TD][/TR]
[TR][TD][FONT=monospace]fpp.swap[/FONT][/TD][TD]Toggle bot swap[/TD][/TR]
[TR][TD][FONT=monospace]fpp.rank[/FONT][/TD][TD]Assign bots to LuckPerms groups[/TD][/TR]
[TR][TD][FONT=monospace]fpp.lpinfo[/FONT][/TD][TD]View LuckPerms diagnostic info for any bot[/TD][/TR]
[TR][TD][FONT=monospace]fpp.stats[/FONT][/TD][TD]View the /fpp stats live statistics panel[/TD][/TR]
[TR][TD][FONT=monospace]fpp.info[/FONT][/TD][TD]Query the database[/TD][/TR]
[TR][TD][FONT=monospace]fpp.reload[/FONT][/TD][TD]Reload configuration[/TD][/TR]
[TR][TD][FONT=monospace]fpp.tp[/FONT][/TD][TD]Teleport to bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.tph[/FONT][/TD][TD]Teleport your own bot to you[/TD][/TR]
[TR][TD][FONT=monospace]fpp.tph.all[/FONT][/TD][TD]Teleport all accessible bots to you at once[/TD][/TR]
[TR][TD][FONT=monospace]fpp.bypass.maxbots[/FONT][/TD][TD]Bypass the global bot cap[/TD][/TR]
[TR][TD][FONT=monospace]fpp.bypass.cooldown[/FONT][/TD][TD]Bypass the per-player spawn cooldown[/TD][/TR]
[TR][TD][FONT=monospace]fpp.peaks[/FONT][/TD][TD]Manage the peak-hours bot pool scheduler[/TD][/TR]
[TR][TD][FONT=monospace]fpp.settings[/FONT][/TD][TD]Open the in-game settings GUI[/TD][/TR]
[TR][TD][FONT=monospace]fpp.inventory[/FONT][/TD][TD]Open any bot's inventory GUI[/TD][/TR]
[TR][TD][FONT=monospace]fpp.move[/FONT][/TD][TD]Navigate bots with A* pathfinding[/TD][/TR]
[TR][TD][FONT=monospace]fpp.cmd[/FONT][/TD][TD]Execute or store commands on bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.mine[/FONT][/TD][TD]Enable/stop bot block mining[/TD][/TR]
[TR][TD][FONT=monospace]fpp.place[/FONT][/TD][TD]Enable/stop bot block placing[/TD][/TR]
[TR][TD][FONT=monospace]fpp.storage[/FONT][/TD][TD]Register supply containers for bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.useitem[/FONT][/TD][TD]Bot right-click / use-item automation[/TD][/TR]
[TR][TD][FONT=monospace]fpp.waypoint[/FONT][/TD][TD]Manage named patrol route waypoints[/TD][/TR]
[TR][TD][FONT=monospace]fpp.rename[/FONT][/TD][TD]Rename any bot (with full state preservation)[/TD][/TR]
[TR][TD][FONT=monospace]fpp.rename.own[/FONT][/TD][TD]Rename only bots the sender personally spawned[/TD][/TR]
[TR][TD][FONT=monospace]fpp.personality[/FONT][/TD][TD]Assign AI personalities to bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.badword[/FONT][/TD][TD]Manage the runtime badword filter list[/TD][/TR]
[TR][TD][FONT=monospace]fpp.ping[/FONT][/TD][TD]View/set simulated tab-list ping for bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.attack[/FONT][/TD][TD]PvE attack automation (classic & mob-targeting modes)[/TD][/TR]
[TR][TD][FONT=monospace]fpp.follow[/FONT][/TD][TD]Follow-target bot automation (persistent across restarts)[/TD][/TR]
[TR][TD][FONT=monospace]fpp.find[/FONT][/TD][TD]Bot block-finding and progressive mining[/TD][/TR]
[TR][TD][FONT=monospace]fpp.sleep[/FONT][/TD][TD]Set bot sleep-origin for night auto-sleep[/TD][/TR]
[TR][TD][FONT=monospace]fpp.stop[/FONT][/TD][TD]Cancel all active tasks for one or all bots[/TD][/TR]
[TR][TD][FONT=monospace]fpp.save[/FONT][/TD][TD]Immediately save all bot data to disk[/TD][/TR]
[TR][TD][FONT=monospace]fpp.setowner[/FONT][/TD][TD]Transfer bot ownership to another player[/TD][/TR]
[TR][TD][FONT=monospace]fpp.skin[/FONT][/TD][TD]Apply or reset per-bot skins[/TD][/TR]
[TR][TD][FONT=monospace]fpp.migrate[/FONT][/TD][TD]Backup, migrate, and export database[/TD][/TR]
[TR][TD][FONT=monospace]fpp.alert[/FONT][/TD][TD]Broadcast network-wide admin alerts[/TD][/TR]
[TR][TD][FONT=monospace]fpp.sync[/FONT][/TD][TD]Push/pull config across proxy network[/TD][/TR]
[/TABLE]

[SIZE=5][B]User[/B][/SIZE] [I](fpp.use — enabled for all players by default)[/I]

[TABLE="width: 100%"]
[TR][TD][B]Permission[/B][/TD][TD][B]Description[/B][/TD][/TR]
[TR][TD][FONT=monospace]fpp.use[/FONT][/TD][TD]All user-tier commands (granted by default)[/TD][/TR]
[TR][TD][FONT=monospace]fpp.spawn.user[/FONT][/TD][TD]Spawn your own bot (limited by fpp.spawn.limit.<num>)[/TD][/TR]
[TR][TD][FONT=monospace]fpp.tph[/FONT][/TD][TD]Teleport your bot to you[/TD][/TR]
[TR][TD][FONT=monospace]fpp.xp[/FONT][/TD][TD]Transfer a bot's XP to yourself[/TD][/TR]
[TR][TD][FONT=monospace]fpp.info.user[/FONT][/TD][TD]View your bot's location and uptime[/TD][/TR]
[/TABLE]

[SIZE=5][B]Bot Limits[/B][/SIZE]

Grant players a [FONT=monospace]fpp.spawn.limit.<num>[/FONT] node to set how many bots they can spawn. FPP picks the highest one they have.

[FONT=monospace]fpp.spawn.limit.1[/FONT]  [FONT=monospace]fpp.spawn.limit.2[/FONT]  [FONT=monospace]fpp.spawn.limit.3[/FONT]  [FONT=monospace]fpp.spawn.limit.5[/FONT]  [FONT=monospace]fpp.spawn.limit.10[/FONT]  [FONT=monospace]fpp.spawn.limit.15[/FONT]  [FONT=monospace]fpp.spawn.limit.20[/FONT]  [FONT=monospace]fpp.spawn.limit.50[/FONT]  [FONT=monospace]fpp.spawn.limit.100[/FONT]

[B]LuckPerms example[/B] — give VIPs 5 bots:
[CODE]
/lp group vip permission set fpp.use true
/lp group vip permission set fpp.spawn.limit.5 true
[/CODE]

[HR][/HR]

[SIZE=6][B]⚙️ Configuration Overview[/B][/SIZE]

Located at [FONT=monospace]plugins/FakePlayerPlugin/config.yml[/FONT]. Run [FONT=monospace]/fpp reload[/FONT] after any change.

[TABLE="width: 100%"]
[TR][TD][B]Section[/B][/TD][TD][B]What it controls[/B][/TD][/TR]
[TR][TD][FONT=monospace]language[/FONT][/TD][TD]Language file to load (language/en.yml)[/TD][/TR]
[TR][TD][FONT=monospace]debug / logging.debug.*[/FONT][/TD][TD]Per-subsystem debug logging (startup, nms, packets, luckperms, network, config-sync, skin, database)[/TD][/TR]
[TR][TD][FONT=monospace]update-checker[/FONT][/TD][TD]Enable/disable startup version check[/TD][/TR]
[TR][TD][FONT=monospace]metrics[/FONT][/TD][TD]Opt-out toggle for anonymous FastStats usage statistics[/TD][/TR]
[TR][TD][FONT=monospace]limits[/FONT][/TD][TD]Global bot cap, per-user limit, spawn tab-complete presets[/TD][/TR]
[TR][TD][FONT=monospace]spawn-cooldown[/FONT][/TD][TD]Seconds between /fpp spawn uses per player (0 = off)[/TD][/TR]
[TR][TD][FONT=monospace]bot-name[/FONT][/TD][TD]Admin/user display name format (admin-format, user-format)[/TD][/TR]
[TR][TD][FONT=monospace]luckperms[/FONT][/TD][TD]default-group — LP group assigned to every new bot at spawn[/TD][/TR]
[TR][TD][FONT=monospace]skin[/FONT][/TD][TD]Skin mode (player/random/none — legacy: auto/custom/off), guaranteed skin, 1000-player fallback pool, DB cache[/TD][/TR]
[TR][TD][FONT=monospace]body[/FONT][/TD][TD]Physical entity (enabled), pushable, damageable, pick-up-items, pick-up-xp, drop-items-on-despawn[/TD][/TR]
[TR][TD][FONT=monospace]persistence[/FONT][/TD][TD]Whether bots rejoin on server restart; task state (mine/place/patrol) also persisted[/TD][/TR]
[TR][TD][FONT=monospace]join-delay / leave-delay[/FONT][/TD][TD]Random delay range (ticks) for natural join/leave timing[/TD][/TR]
[TR][TD][FONT=monospace]messages[/FONT][/TD][TD]Toggle join, leave, kill broadcast messages; admin compatibility notifications[/TD][/TR]
[TR][TD][FONT=monospace]combat[/FONT][/TD][TD]Bot HP and hurt sound[/TD][/TR]
[TR][TD][FONT=monospace]death[/FONT][/TD][TD]Respawn on death, respawn delay, item drop suppression[/TD][/TR]
[TR][TD][FONT=monospace]chunk-loading[/FONT][/TD][TD]Radius, update interval[/TD][/TR]
[TR][TD][FONT=monospace]head-ai[/FONT][/TD][TD]Enable/disable, look range, turn speed[/TD][/TR]
[TR][TD][FONT=monospace]swim-ai[/FONT][/TD][TD]Automatic swimming in water/lava (enabled, default true)[/TD][/TR]
[TR][TD][FONT=monospace]collision[/FONT][/TD][TD]Push physics — walk strength, hit strength, bot separation[/TD][/TR]
[TR][TD][FONT=monospace]pathfinding[/FONT][/TD][TD]A* options — parkour, break-blocks, place-blocks, arrival distances, node limits, max-fall[/TD][/TR]
[TR][TD][FONT=monospace]fake-chat[/FONT][/TD][TD]Enable, chance, interval, typing delays, burst messages, bot-to-bot chat, mention replies, event reactions[/TD][/TR]
[TR][TD][FONT=monospace]ai-conversations[/FONT][/TD][TD]AI DM system — provider config, personality, typing delay, conversation history[/TD][/TR]
[TR][TD][FONT=monospace]badword-filter[/FONT][/TD][TD]Name profanity filter — leet-speak normalization, remote word list, auto-rename[/TD][/TR]
[TR][TD][FONT=monospace]bot-interaction[/FONT][/TD][TD]Right-click / shift-right-click settings GUI toggles[/TD][/TR]
[TR][TD][FONT=monospace]swap[/FONT][/TD][TD]Auto rotation — session length, absence duration, min-online floor, retry-on-fail, farewell/greeting chat[/TD][/TR]
[TR][TD][FONT=monospace]peak-hours[/FONT][/TD][TD]Time-based bot pool scheduler — schedule, day-overrides, stagger-seconds, min-online[/TD][/TR]
[TR][TD][FONT=monospace]performance[/FONT][/TD][TD]Position sync distance culling (position-sync-distance)[/TD][/TR]
[TR][TD][FONT=monospace]tab-list[/FONT][/TD][TD]Show/hide bots in the player tab list[/TD][/TR]
[TR][TD][FONT=monospace]server-list[/FONT][/TD][TD]Whether bots count in the server-list player total; count-bots, include-remote-bots[/TD][/TR]
[TR][TD][FONT=monospace]config-sync[/FONT][/TD][TD]Cross-server config push/pull mode (DISABLED/MANUAL/AUTO_PULL/AUTO_PUSH)[/TD][/TR]
[TR][TD][FONT=monospace]database[/FONT][/TD][TD]mode (LOCAL/NETWORK), server-id, SQLite (default) or MySQL[/TD][/TR]
[TR][TD][FONT=monospace]automation[/FONT][/TD][TD]auto-eat, auto-place-bed — realistic bot survival defaults[/TD][/TR]
[TR][TD][FONT=monospace]attack-mob[/FONT][/TD][TD]PvE auto-targeting defaults (default-range, default-priority, etc.)[/TD][/TR]
[/TABLE]

[HR][/HR]

[SIZE=6][B]🤖 AI Conversations[/B][/SIZE]

Bots can respond to [FONT=monospace]/msg[/FONT], [FONT=monospace]/tell[/FONT], and [FONT=monospace]/whisper[/FONT] with AI-generated replies matching their personality.

[B]Setup:[/B]
[LIST=1]
[*]Edit [FONT=monospace]plugins/FakePlayerPlugin/secrets.yml[/FONT] and add your API key
[*]Set [FONT=monospace]ai-conversations.enabled: true[/FONT] in [FONT=monospace]config.yml[/FONT]
[*]Bots will automatically respond — no restart needed
[/LIST]

[B]Supported Providers[/B] (picked in priority order — first key that works wins):

[TABLE="width: 100%"]
[TR][TD][B]Provider[/B][/TD][TD][B]Key in secrets.yml[/B][/TD][/TR]
[TR][TD]OpenAI[/TD][TD][FONT=monospace]openai-api-key[/FONT][/TD][/TR]
[TR][TD]Anthropic[/TD][TD][FONT=monospace]anthropic-api-key[/FONT][/TD][/TR]
[TR][TD]Groq[/TD][TD][FONT=monospace]groq-api-key[/FONT][/TD][/TR]
[TR][TD]Google Gemini[/TD][TD][FONT=monospace]google-gemini-api-key[/FONT][/TD][/TR]
[TR][TD]Ollama[/TD][TD][FONT=monospace]ollama-base-url[/FONT] (local, no key needed)[/TD][/TR]
[TR][TD]Copilot / Azure[/TD][TD][FONT=monospace]copilot-api-key[/FONT][/TD][/TR]
[TR][TD]Custom OpenAI-compatible[/TD][TD][FONT=monospace]custom-openai-base-url[/FONT][/TD][/TR]
[/TABLE]

[B]Personalities:[/B] Drop [FONT=monospace].txt[/FONT] files into [FONT=monospace]plugins/FakePlayerPlugin/personalities/[/FONT] to create custom personality prompts. Assign per-bot with [FONT=monospace]/fpp personality <bot> set <name>[/FONT]. Bundled personalities: [FONT=monospace]friendly[/FONT] · [FONT=monospace]grumpy[/FONT] · [FONT=monospace]noob[/FONT].

[HR][/HR]

[SIZE=6][B]🎨 Skin System[/B][/SIZE]

Three modes — set with [FONT=monospace]skin.mode[/FONT]:

[TABLE="width: 100%"]
[TR][TD][B]Mode[/B][/TD][TD][B]Behaviour[/B][/TD][/TR]
[TR][TD][FONT=monospace]auto[/FONT] [I](default)[/I][/TD][TD]Fetches a real Mojang skin matching the bot's name[/TD][/TR]
[TR][TD][FONT=monospace]player[/FONT] [I](default)[/I][/TD][TD]Fetches a real Mojang skin matching the bot's name[/TD][/TR]
[TR][TD][FONT=monospace]random[/FONT][/TD][TD]Full control — per-bot overrides, a skins/ PNG folder, and a random pool[/TD][/TR]
[TR][TD][FONT=monospace]none[/FONT][/TD][TD]No skin — bots use the default Steve/Alex appearance[/TD][/TR]
[/TABLE]

[B]Skin fallback[/B] ([FONT=monospace]skin.guaranteed-skin[/FONT], default [FONT=monospace]true[/FONT]) — bots whose name has no matching Mojang account get a random skin from the built-in 1000-player fallback pool. Set to [FONT=monospace]false[/FONT] to use the default Steve/Alex appearance instead.

[B]Legacy aliases:[/B] [FONT=monospace]auto[/FONT] = [FONT=monospace]player[/FONT], [FONT=monospace]custom[/FONT] = [FONT=monospace]random[/FONT], [FONT=monospace]off[/FONT] = [FONT=monospace]none[/FONT] — all still accepted.

In [FONT=monospace]random[/FONT] mode the resolution pipeline is: per-bot override → [FONT=monospace]skins/<name>.png[/FONT] → random PNG from [FONT=monospace]skins/[/FONT] folder → random entry from [FONT=monospace]pool[/FONT] → Mojang API for the bot's own name.

[HR][/HR]

[SIZE=6][B]🔤 LuckPerms Integration[/B][/SIZE]

FPP treats bots as real NMS ServerPlayer entities — LuckPerms detects them as online players automatically.

[LIST]
[*][FONT=monospace]luckperms.default-group[/FONT] — assigns every new bot to an LP group at spawn (blank = LP's built-in default)
[*][FONT=monospace]/fpp rank <bot> <group>[/FONT] — change an individual bot's LP group at runtime, no respawn needed
[*][FONT=monospace]/fpp rank random <group> [num|all][/FONT] — assign a group to random bots
[*][FONT=monospace]/fpp rank list[/FONT] — see each bot's current group at a glance
[*][FONT=monospace]/fpp lpinfo [bot][/FONT] — diagnose prefix, weight, rank index, and packet profile name
[*][B]Tab-list ordering[/B] — ~fpp scoreboard team keeps all bots below real players regardless of LP weight
[*][B]Prefix/suffix[/B] — bots use LuckPerms prefix/suffix automatically (real NMS entities — LP detects them natively)
[/LIST]

[CODE]
luckperms:
  default-group: ""   # e.g. "default", "vip", "admin"
[/CODE]

[HR][/HR]

[SIZE=6][B]🌐 Proxy & Network Support[/B][/SIZE]

FPP supports multi-server [B]Velocity[/B] and [B]BungeeCord[/B] proxy networks.

Enable NETWORK mode on every backend server:

[CODE]
database:
  enabled: true
  mode: "NETWORK"
  server-id: "survival"   # unique per server
  mysql-enabled: true
  mysql:
    host: "mysql.example.com"
    database: "fpp_network"
    username: "fpp_user"
    password: "your_password"
[/CODE]

[B]Cross-server features in NETWORK mode:[/B]
[LIST]
[*]Fake chat messages broadcast to all servers on the proxy
[*][FONT=monospace]/fpp alert <message>[/FONT] — network-wide admin alert
[*]Bot join/leave messages visible network-wide
[*]Remote bot tab-list entries synced across servers
[*]Per-server isolation — each server only manages its own bots
[/LIST]

[HR][/HR]

[SIZE=6][B]🔄 Config Sync[/B][/SIZE]

Keep all servers' configurations in sync automatically:

[CODE]
config-sync:
  mode: "AUTO_PULL"   # DISABLED | MANUAL | AUTO_PULL | AUTO_PUSH
[/CODE]

[TABLE="width: 100%"]
[TR][TD][B]Mode[/B][/TD][TD][B]Behaviour[/B][/TD][/TR]
[TR][TD][FONT=monospace]DISABLED[/FONT][/TD][TD]No syncing (default)[/TD][/TR]
[TR][TD][FONT=monospace]MANUAL[/FONT][/TD][TD]Only sync via /fpp sync commands[/TD][/TR]
[TR][TD][FONT=monospace]AUTO_PULL[/FONT][/TD][TD]Auto-pull latest config on every startup/reload[/TD][/TR]
[TR][TD][FONT=monospace]AUTO_PUSH[/FONT][/TD][TD]Push local changes to the network automatically[/TD][/TR]
[/TABLE]

[B]Files synced:[/B] config.yml, bot-names.yml, bot-messages.yml, language/en.yml

[B]Server-specific keys that NEVER sync:[/B] database.server-id, database.mysql.*, debug

[CODE]
/fpp sync push config.yml        # Upload to network
/fpp sync pull config.yml        # Download from network
/fpp sync status                 # Hash + timestamp per file
/fpp sync check                  # Which files have local changes
[/CODE]

[HR][/HR]

[SIZE=6][B]📊 PlaceholderAPI[/B][/SIZE]

When [URL='https://www.spigotmc.org/resources/placeholderapi.6245/']PlaceholderAPI[/URL] is installed, FPP registers placeholders automatically — no restart needed.

Full documentation available on [URL='https://github.com/Pepe-tf/Fake-Player-Plugin-Public-/blob/main/PLACEHOLDERAPI.md']GitHub[/URL].

FPP provides [B]29+ placeholders[/B] in five categories:

[SIZE=5][B]Server-Wide[/B][/SIZE]

[TABLE="width: 100%"]
[TR][TD][B]Placeholder[/B][/TD][TD][B]Value[/B][/TD][/TR]
[TR][TD][FONT=monospace]%fpp_count%[/FONT][/TD][TD]Active bots (local + remote in NETWORK mode)[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_local_count%[/FONT][/TD][TD]Bots on this server only[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_network_count%[/FONT][/TD][TD]Bots on other proxy servers (NETWORK mode)[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_max%[/FONT][/TD][TD]Global max-bots limit (or ∞)[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_real%[/FONT][/TD][TD]Real (non-bot) players online[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_total%[/FONT][/TD][TD]Total players (real + bots)[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_online%[/FONT][/TD][TD]Alias for %fpp_total%[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_frozen%[/FONT][/TD][TD]Number of currently frozen bots[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_names%[/FONT][/TD][TD]Comma-separated bot display names (local + remote in NETWORK mode)[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_network_names%[/FONT][/TD][TD]Display names of bots on other proxy servers only[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_version%[/FONT][/TD][TD]Plugin version string[/TD][/TR]
[/TABLE]

[SIZE=5][B]Config State[/B][/SIZE]

[TABLE="width: 100%"]
[TR][TD][B]Placeholder[/B][/TD][TD][B]Values[/B][/TD][TD][B]Config Key[/B][/TD][/TR]
[TR][TD][FONT=monospace]%fpp_chat%[/FONT][/TD][TD]on / off[/TD][TD]fake-chat.enabled[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_swap%[/FONT][/TD][TD]on / off[/TD][TD]swap.enabled[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_body%[/FONT][/TD][TD]on / off[/TD][TD]body.enabled[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_pushable%[/FONT][/TD][TD]on / off[/TD][TD]body.pushable[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_damageable%[/FONT][/TD][TD]on / off[/TD][TD]body.damageable[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_tab%[/FONT][/TD][TD]on / off[/TD][TD]tab-list.enabled[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_skin%[/FONT][/TD][TD]auto / custom / off[/TD][TD]skin.mode[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_max_health%[/FONT][/TD][TD]number[/TD][TD]combat.max-health[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_persistence%[/FONT][/TD][TD]on / off[/TD][TD]persistence.enabled[/TD][/TR]
[/TABLE]

[SIZE=5][B]Network / Proxy[/B][/SIZE]

[TABLE="width: 100%"]
[TR][TD][B]Placeholder[/B][/TD][TD][B]Value[/B][/TD][/TR]
[TR][TD][FONT=monospace]%fpp_network%[/FONT][/TD][TD]on when database.mode: NETWORK, otherwise off[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_server_id%[/FONT][/TD][TD]Value of database.server-id[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_spawn_cooldown%[/FONT][/TD][TD]Configured cooldown in seconds (0 = off)[/TD][/TR]
[/TABLE]

[SIZE=5][B]Per-World[/B][/SIZE]

[TABLE="width: 100%"]
[TR][TD][B]Placeholder[/B][/TD][TD][B]Value[/B][/TD][/TR]
[TR][TD][FONT=monospace]%fpp_count_<world>%[/FONT][/TD][TD]Bots in world (e.g. %fpp_count_world_nether%)[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_real_<world>%[/FONT][/TD][TD]Real players in world[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_total_<world>%[/FONT][/TD][TD]Total (real + bots) in world[/TD][/TR]
[/TABLE]

World names are case-insensitive. Use underscores for worlds with spaces.

[SIZE=5][B]Player-Relative[/B][/SIZE]

[TABLE="width: 100%"]
[TR][TD][B]Placeholder[/B][/TD][TD][B]Value[/B][/TD][/TR]
[TR][TD][FONT=monospace]%fpp_user_count%[/FONT][/TD][TD]Bots owned by the player[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_user_max%[/FONT][/TD][TD]Bot limit for the player[/TD][/TR]
[TR][TD][FONT=monospace]%fpp_user_names%[/FONT][/TD][TD]Comma-separated names of player's bots[/TD][/TR]
[/TABLE]

[SIZE=5][B]Quick Examples[/B][/SIZE]

[B]Scoreboard sidebar:[/B]
[CODE]
&7Bots: &b%fpp_count%&7/&b%fpp_max%
&7Real: &a%fpp_real%
&7Total: &e%fpp_total%
[/CODE]

[B]Tab list header:[/B]
[CODE]
&7Server: &bSurvival &8| &7Players: &a%fpp_real% &8| &7Bots: &b%fpp_count%
[/CODE]

[B]Per-world display:[/B]
[CODE]
&7Overworld: &e%fpp_total_world%
&7Nether:    &c%fpp_total_world_nether%
&7End:       &d%fpp_total_world_the_end%
[/CODE]

[HR][/HR]

[SIZE=6][B]📝 Bot Names & Chat[/B][/SIZE]

[TABLE="width: 100%"]
[TR][TD][B]File[/B][/TD][TD][B]Purpose[/B][/TD][/TR]
[TR][TD][FONT=monospace]bot-names.yml[/FONT][/TD][TD]Random name pool. 1–16 chars, letters/digits/underscores. /fpp reload to update.[/TD][/TR]
[TR][TD][FONT=monospace]bot-messages.yml[/FONT][/TD][TD]Random chat messages. Supports {name} and {random_player} placeholders.[/TD][/TR]
[/TABLE]

When the name pool runs out, FPP generates names automatically ([FONT=monospace]Bot1234[/FONT], etc.).

Bot chat uses the server's real chat pipeline, so formatting is handled by your existing chat plugin (LuckPerms, EssentialsX, etc.). For bodyless or proxy-remote bots, the [FONT=monospace]fake-chat.remote-format[/FONT] key controls how messages appear (supports [FONT=monospace]{name}[/FONT] and [FONT=monospace]{message}[/FONT] placeholders).

[HR][/HR]

[SIZE=6][B]🚀 Proxy Companions[/B][/SIZE]

FPP ships two optional companion plugins that inflate the [B]proxy-level[/B] server-list player count to include FPP bots.

[SIZE=5][B]Velocity Companion (fpp-velocity.jar)[/B][/SIZE]

A lightweight standalone Velocity plugin that makes FPP bots count in the [B]proxy[/B] server list — no config required.

[B]What it does:[/B]
[LIST]
[*]Registers the [FONT=monospace]fpp:proxy[/FONT] plugin-messaging channel; listens for [FONT=monospace]BOT_SPAWN[/FONT], [FONT=monospace]BOT_DESPAWN[/FONT], and [FONT=monospace]SERVER_OFFLINE[/FONT] messages from backend servers
[*]Maintains a live bot registry; pings all backend servers every 5 seconds and caches their real+bot player counts
[*]Intercepts [FONT=monospace]ProxyPingEvent[/FONT] to inflate the proxy-level server-list player count and hover sample list (up to 12 bot names shown)
[*]Prints a prominent [B]anti-scam warning[/B] on every startup — this plugin is [B]100% FREE[/B]; if you paid for it, you were scammed
[/LIST]

[B]Installation:[/B]
[LIST=1]
[*]Drop [FONT=monospace]fpp-velocity.jar[/FONT] into your Velocity proxy's [FONT=monospace]plugins/[/FONT] folder — no config file needed
[*]Restart Velocity — the startup banner confirms the channel is registered and ready
[/LIST]

[B]Requirements:[/B] Velocity 3.3.0+

[SIZE=5][B]BungeeCord Companion (fpp-bungee.jar)[/B][/SIZE]

Identical feature set for BungeeCord/Waterfall networks.

[B]What it does:[/B]
[LIST]
[*]Registers the [FONT=monospace]fpp:proxy[/FONT] plugin-messaging channel; listens for [FONT=monospace]BOT_SPAWN[/FONT], [FONT=monospace]BOT_DESPAWN[/FONT], and [FONT=monospace]SERVER_OFFLINE[/FONT] messages from backend servers
[*]Maintains a live bot registry; pings all backend servers every 5 seconds and caches their real+bot player counts
[*]Intercepts [FONT=monospace]ProxyPingEvent[/FONT] to inflate the proxy-level server-list player count and hover sample list (up to 12 bot names shown)
[*]Prints a prominent [B]anti-scam warning[/B] on every startup — this plugin is [B]100% FREE[/B]; if you paid for it, you were scammed
[/LIST]

[B]Installation:[/B]
[LIST=1]
[*]Drop [FONT=monospace]fpp-bungee.jar[/FONT] into your BungeeCord/Waterfall proxy's [FONT=monospace]plugins/[/FONT] folder — no config file needed
[*]Restart BungeeCord
[/LIST]

[B]Requirements:[/B] BungeeCord or any Waterfall fork

[COLOR=#FF4444][B]⚠ FPP and both companion plugins are 100% FREE & open-source.[/B][/COLOR]
[COLOR=#FF4444][B]If you or your server paid money for any of them, you were SCAMMED by a reseller.[/B][/COLOR]

Always download from the official sources:
[LIST]
[*][B]Modrinth:[/B] [URL='https://modrinth.com/plugin/fake-player-plugin-(fpp)']https://modrinth.com/plugin/fake-player-plugin-(fpp)[/URL]
[*][B]GitHub:[/B] [URL='https://github.com/Pepe-tf/fake-player-plugin']https://github.com/Pepe-tf/fake-player-plugin[/URL]
[*][B]Discord:[/B] [URL='https://discord.gg/QSN7f67nkJ']https://discord.gg/QSN7f67nkJ[/URL]
[/LIST]

[HR][/HR]

[SIZE=6][B]📖 Changelog[/B][/SIZE]

[SIZE=5][B]1.6.6.8[/B][/SIZE] [I](2026-05-02)[/I]

[B]Bot Join/Leave Message Overhaul[/B]
[LIST]
[*]Bot join messages now use the custom [FONT=monospace]bot-join[/FONT] lang key from [FONT=monospace]en.yml[/FONT] — fully customizable with MiniMessage formatting
[*]Bot leave messages now use the custom [FONT=monospace]bot-leave[/FONT] lang key and are sent explicitly after despawn/removal — no more missing leave messages
[*]Death-despawn leave messages fire 20 ticks after death (after kill message and entity removal) for proper ordering
[*]Vanilla quit messages are always nulled for bots — the only leave message is the custom broadcast
[/LIST]

[B]Skin System Improvements[/B]
[LIST]
[*]Skin fetch retry count increased from 3 to 5 — bots try up to 5 different pool names before falling back to Steve/Alex
[*]Null/invalid skin results handled gracefully with clear debug messaging
[*]All skin retry/failure messages now use [FONT=monospace]Config.debugSkin()[/FONT] — silent by default, visible only with [FONT=monospace]logging.debug.skin: true[/FONT]
[/LIST]

[B]Ping System[/B]
[LIST]
[*][FONT=monospace]ping.enabled[/FONT] default changed from [FONT=monospace]true[/FONT] to [FONT=monospace]false[/FONT] — ping simulation is now opt-in
[/LIST]

[B]Help Menu[/B]
[LIST]
[*][FONT=monospace]HelpGui[/FONT] now includes [FONT=monospace]ping[/FONT] and [FONT=monospace]skin[/FONT] commands in the Bots category
[/LIST]

[B]DB Schema[/B] v21 → v22 (new columns: [FONT=monospace]auto_milk_enabled[/FONT], [FONT=monospace]prevent_bad_omen[/FONT], [FONT=monospace]ping_user_set[/FONT])
[B]Config[/B] v67 → v70 ([FONT=monospace]ping.enabled[/FONT] default changed to [FONT=monospace]false[/FONT])

[B]Extension Config & Resource System[/B]
[LIST]
[*][FONT=monospace]FppExtension[/FONT] interface now provides 6 convenience methods for extension data/config management:
[LIST]
[*][FONT=monospace]getDataFolder()[/FONT] — returns [FONT=monospace]plugins/FakePlayerPlugin/extensions/<ExtensionName>/[/FONT]
[*][FONT=monospace]getConfig()[/FONT] — lazy-loads config from disk, merges JAR defaults via [FONT=monospace]setDefaults()[/FONT]
[*][FONT=monospace]saveDefaultConfig()[/FONT] — extracts [FONT=monospace]config.yml[/FONT] from JAR (tries root first, then [FONT=monospace]extension-resources/[/FONT]); performs YamlFileSyncer-style key merge on subsequent calls
[*][FONT=monospace]saveDefaultResources()[/FONT] — extracts all files under [FONT=monospace]extension-resources/[/FONT] in the JAR (never overwrites existing files)
[*][FONT=monospace]saveResource(jarPath)[/FONT] — on-demand extraction of a single JAR resource
[*][FONT=monospace]reloadConfig()[/FONT] — reloads config from disk with fresh JAR defaults
[/LIST]
[*][FONT=monospace]FppApi[/FONT] exposes 3 cross-extension methods: [FONT=monospace]getExtensionDataFolder(name)[/FONT], [FONT=monospace]saveDefaultExtensionConfig(name)[/FONT], [FONT=monospace]getExtensionConfig(name)[/FONT]
[*][FONT=monospace]ExtensionLoader[/FONT] creates per-extension data folders automatically on load
[*][FONT=monospace]/fpp reload extensions[/FONT] now also calls [FONT=monospace]reloadExtensionConfigs()[/FONT] to sync config keys after hot-reload
[/LIST]

[HR][/HR]

[SIZE=6][B]❤️ Support the Project[/B][/SIZE]

If you enjoy FPP and want to help keep it going, consider buying me a coffee:

[CENTER][URL='https://ko-fi.com/fakeplayerplugin'][B][COLOR=#FF5E5B]☕ Support on Ko-fi[/COLOR][/B][/URL]  [URL='https://github.com/sponsors/Pepe-tf'][B][COLOR=#EA4AAA]💖 GitHub Sponsors[/COLOR][/B][/URL]  [URL='https://www.patreon.com/c/F_PP?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink'][B][COLOR=#FF424D]🎗 Support on Patreon[/COLOR][/B][/URL][/CENTER]

Donations are completely optional. Every contribution goes directly toward improving the plugin.

Thank you for using Fake Player Plugin. Without you, it wouldn't be where it is today.

[HR][/HR]

[SIZE=6][B]🔗 Links[/B][/SIZE]

[LIST]
[*][URL='https://modrinth.com/plugin/fake-player-plugin-(fpp)']Modrinth[/URL] — download
[*][URL='https://www.spigotmc.org/resources/fake-player-plugin-fpp.133572/']SpigotMC[/URL] — download
[*][URL='https://hangar.papermc.io/Pepe-tf/FakePlayerPlugin']PaperMC Hangar[/URL] — download
[*][URL='https://builtbybit.com/resources/fake-player-plugin.98704/']BuiltByBit[/URL] — download
[*][URL='https://fpp.wtf']Wiki[/URL] — documentation
[*][URL='https://ko-fi.com/fakeplayerplugin']Ko-fi[/URL] — support the project
[*][URL='https://github.com/sponsors/Pepe-tf']GitHub Sponsors[/URL] — support the project
[*][URL='https://www.patreon.com/c/F_PP?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink']Patreon[/URL] — support the project
[*][URL='https://discord.gg/QSN7f67nkJ']Discord[/URL] — support & feedback
[*][URL='https://github.com/Pepe-tf/Fake-Player-Plugin-Public-']GitHub[/URL] — source & issues
[/LIST]

[HR][/HR]

[CENTER][I]Built for Paper 1.21.x (1.21.0–1.21.11) · Java 21 · FPP v1.6.6.8[/I]

[URL='https://modrinth.com/plugin/fake-player-plugin-(fpp)']Modrinth[/URL]  [URL='https://www.spigotmc.org/resources/fake-player-plugin-fpp.133572/']SpigotMC[/URL]  [URL='https://hangar.papermc.io/Pepe-tf/FakePlayerPlugin']PaperMC[/URL]  [URL='https://builtbybit.com/resources/fake-player-plugin.98704/']BuiltByBit[/URL]  [URL='https://fpp.wtf']Wiki[/URL][/CENTER]
