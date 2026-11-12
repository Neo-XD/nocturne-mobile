package com.nocturne.music.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Detects AI-generated music tracks using the same AI filter tags, regex keywords,
 * and crowdsourced SoulOverAI / Spotify-AI-Band-Blocker artist dataset.
 *
 * Filter tags supported (from Reginald-Gillespie/Spotify-AI-Band-Blocker):
 * - ai-visuals (AI generated visuals/artwork)
 * - anonymous (Faceless/anonymous AI persona)
 * - high-output (Excessive weekly/daily algorithmic releases)
 * - 2024-onwards (Created 2024+ solely to exploit AI streaming)
 * - inconsistent-style (Drastic multi-genre switching typical of AI prompt generation)
 */
object AiDetector {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val SOUL_OVER_AI_URL =
        "https://raw.githubusercontent.com/xoundbyte/soul-over-ai/main/dist/artists.json"

    val FILTER_TAGS = setOf(
        "ai-visuals",
        "anonymous",
        "high-output",
        "2024-onwards",
        "inconsistent-style"
    )

    private val AI_PATTERN = Regex(
        """(?i)\b(suno(?:\s*ai)?|udio(?:\s*ai)?|ai\s+cover|ai\s+generated|ai\s+remake|ai\s+music|ai\s+vocals|ai\s+song|ai\s+version|ai\s+track|ai\s+artist|ai\s+band|fake\s+drake)\b|\((?:ai|ai\s+cover|suno|udio)\)|\[(?:ai|ai\s+cover|suno|udio)\]"""
    )

    // In-memory artist database with tags: lowercase artist name -> set of markers
    private val knownAiArtists = ConcurrentHashMap<String, Set<String>>()

    init {
        // Load offline builtin baseline of known AI artists
        for (name in BUILTIN_AI_ARTISTS.split(";")) {
            val trimmed = name.trim().lowercase()
            if (trimmed.isNotEmpty()) {
                knownAiArtists[trimmed] = emptySet()
            }
        }
        // Async background sync with SoulOverAI database
        syncDatabase()
    }

    fun syncDatabase() {
        scope.launch {
            try {
                val req = okhttp3.Request.Builder()
                    .url(SOUL_OVER_AI_URL)
                    .header("User-Agent", "NocturneMusic/0.4.0")
                    .build()
                com.nocturne.music.data.Http.client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@use
                        val array = json.parseToJsonElement(body).jsonArray
                        for (el in array) {
                            val obj = el.jsonObject
                            if (obj["removed"]?.jsonPrimitive?.booleanOrNull == true) continue
                            val name = obj["name"]?.jsonPrimitive?.content?.trim()?.lowercase() ?: continue
                            val markers = mutableSetOf<String>()
                            obj["markers"]?.jsonArray?.forEach { m ->
                                markers.add(m.jsonPrimitive.content.trim())
                            }
                            obj["tags"]?.jsonArray?.forEach { t ->
                                markers.add(t.jsonPrimitive.content.trim())
                            }
                            knownAiArtists[name] = markers
                        }
                    }
                }
            } catch (_: Exception) {
                // Silently fallback to builtin baseline
            }
        }
    }

    /**
     * Checks if a track is AI-generated based on its title, artist name,
     * regex markers, or presence in the Spotify-AI-Band-Blocker database.
     */
    fun isAiTrack(title: String?, artist: String?): Boolean {
        if (title.isNullOrBlank() && artist.isNullOrBlank()) return false

        // 1. Check title regex
        if (title != null && AI_PATTERN.containsMatchIn(title)) return true

        // 2. Check artist regex
        if (artist != null && AI_PATTERN.containsMatchIn(artist)) return true

        // 3. Check known AI artist blocklist (supporting multiple artists separated by commas/features)
        if (artist != null) {
            val normalized = artist.trim().lowercase()
            if (knownAiArtists.containsKey(normalized)) return true

            // Check split artist components: "Artist A, Artist B" or "Artist A feat. Artist B"
            val splits = normalized.split(Regex("[,&/]|\\bfeat\\.?\\b|\\bft\\.?\\b"))
            for (part in splits) {
                val clean = part.trim()
                if (clean.isNotEmpty() && knownAiArtists.containsKey(clean)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Returns matching AI markers (e.g. "ai-visuals", "high-output") for an artist.
     */
    fun getAiMarkers(artist: String?): Set<String> {
        if (artist.isNullOrBlank()) return emptySet()
        val normalized = artist.trim().lowercase()
        knownAiArtists[normalized]?.let { if (it.isNotEmpty()) return it }

        val splits = normalized.split(Regex("[,&/]|\\bfeat\\.?\\b|\\bft\\.?\\b"))
        for (part in splits) {
            val clean = part.trim()
            val markers = knownAiArtists[clean]
            if (!markers.isNullOrEmpty()) return markers
        }

        return emptySet()
    }

    private const val BUILTIN_AI_ARTISTS =
        "10mm;1950 SoulClub;1960 SoulClub;19s Soulers;326 YASUBE-;404 Breakcore;5 Days Left;50 Mark Club;723ChillBeats;7even Sins;7th Swing;88DS;9 Runes;A Million Sparks;A Shrine to Failure;A Wildheart's Tale;A7M;Abel Abaddon;Abercrombie Music;abscond;Absolute Introspect;Action Cop;Aeriel;Aesir Aria;Aetherea;After Death;After the Fire;Afterthought;Aged in Wood;Agony and Acceptance;AI AI;Ai Alice;Ai Amy;AI Ash;Ai Austin;Ai Bae;Ai Band;Ai Beats;Ai Benny;Ai Bob;AI Boris;Ai Boy;Ai Brad;Ai Brody;AI Broz;Ai Bruno;AI Calm;Ai Chloe;AI Chris;AI Cover;Ai Daisy;Ai Dave;Ai David;Ai Derek;Ai Devin;Ai Devon;Ai Diddy;AI Dillon;Ai Draco;AI Drake;Ai Dream;Ai Dylan;Ai Elena;Ai Ellie;Ai Elvis;Ai Ethan;Ai Faith;Ai Fiona;Ai Frank;Ai Grace;Ai Green;Ai Hailey;Ai Haley;Ai Hanna;Ai Harry;Ai Hazel;Ai Henry;Ai Homer;Ai Isaac;Ai James;Ai Jamie;Ai Jason;Ai Jayce;Ai Jesus;Ai Jimmy;Ai Jonah;Ai Jonas;Ai Jorge;Ai Juice;Ai Julia;Ai Kanye;Ai Karen;Ai Katie;Ai Kayla;Ai Kevin;Ai Kylie;Ai Lamar;Ai Laura;Ai Layla;Ai Lewis;Ai Logan;Ai Lucas;Ai Lucca;Ai Lucia;Ai Lyric;Ai Manny;Ai Mateo;Ai Megan;Ai Miles;Ai Molly;Ai Naomi;Ai Nelly;Ai Nolan;Ai Oasis;Ai Paige;Ai Peter;Ai Piper;Ai Queen;Ai Riley;Ai Roman;Ai Romeo;Ai Ryder;Ai Sadie;Ai Sarah;Ai Sasha;Ai Scott;Ai Shane;Ai Simon;Ai Steve;Ai Storm;Ai Tessa;Ai Tyler;Ai Tyson;Ai Vance;Ai Voice;Ai Wayne;Ai Wendy;Ai Wyatt;Ailuv;AIMUSIC;AINT NUTHIN;Airbound;Alabaster Sons;Alex Rivera;All of You;All That Remains;Along for the Ride;Alpha & Omega;Alpha Omega;Alpha State;Alpine Glow;Alt Soul;Altar of Bones;Altar of Silence;Amber Wave;Ambient AI;Amethyst Veil;Amity Lost;Amnesia;Ancient Echoes;Ancient Paths;Ancient Skies;Angelica;Angry Birds;Anima Mundi;Animal Instinct;Another Dimension;Another Horizon;Anthem of the Free;Anti Hero;Antidote;Apex Predator;Apollo's Chariot;Arcane Echoes;Arcadia;Archangel;Arctic Bloom;Arctic Drift;Area 51;Aria;Aria AI;Aria Vance;Aries;Ark of the Covenant;Armageddon;Arrival;Arrowhead;Arsenal;Art of Noise;Artisan;As the Crow Flies;Ashes to Embers;Ashes to Gold;Astral Drift;Astral Echoes;Astral Gate;Astral Plane;Astral Projection;Astral Travel;Atlas Shrugged;Atomic Blonde;Aura;Aura AI;Auric;Aurora;Aurora Borealis;Autumn Leaves;Autumn Skies;Avalanche;Avatar;Avenue of the Giants;Aviary;Awaken;Awakening;B-Side;Babylon;Back to Basics;Bad Karma;Bad Medicine;Ballet Dancer;Bamboo Forest;Band of Brothers;Barefoot;Baroque;Bastion;Battle Cry;Beacon;Beast Mode;Beat Street;Beautiful Mind;Before the Dawn;Beggar's Opera;Behind Closed Doors;Beneath the Surface;Bespoke;Beyond the Horizon;Beyond the Pale;Beyond the Stars;Beyond the Veil;Binary;Binary Sunset;Bird of Prey;Bitter End;Black Dawn;Black Gold;Black Hole;Black Ice;Black Mirror;Black Pearl;Black Rose;Black Velvet;Blackout;Blank Slate;Blaze;Blind Faith;Blind Spot;Bliss;Blood Moon;Blue Horizon;Blue Lagoon;Blue Skies;Blueprint;Blush;Body and Soul;Bohemian;Bones;Bonsai;Book of Life;Borderline;Botanical;Boundless;Bounty Hunter;Bravo;Break of Dawn;Breakaway;Breaking Point;Breakthrough;Breathe;Breeze;Bridge of Sighs;Bright Eyes;Bright Lights;Brighter Days;Brimstone;Broken Arrow;Broken Glass;Broken Silence;Bronze;Brotherhood;Bubblegum;Bulletproof;Burn;Burning Bright;Butterfly Effect;By the Sea;Cabin Fever;Cache;Cadence;Cafe Noir;Calm Waters;Campfire;Candlelight;Canvas;Canyon;Capsule;Caravan;Carbon;Carnival;Cascade;Catalyst;Cathedral;Celestial;Celestial Drift;Centurion;Ceremony;Chameleon;Champion;Chaos Theory;Charcoal;Chariot;Chasing Shadows;Chill AI;Chill Beats;Chillhop AI;Chroma;Chronicle;Cinder;Cipher;Citadel;City Lights;Cityscape;Clarity;Clear Skies;Cloud 9;Cloud Nine;Cloudburst;Cobalt;Cold Front;Colossus;Comet;Compass;Concord;Concrete Jungle;Continuum;Copper;Coral;Cosmic Drift;Cosmic Dust;Cosmic Ray;Cosmic Wind;Cosmos;Counterpoint;Courage;Covenant;Covert;Cradle;Crimson;Crimson Tide;Crossroads;Crown;Crucible;Crusader;Crystal;Curfew;Current;Curtain Call;Cyan;Cyclone;Cypher;Daily Bread;Daisy Chain;Damage Control;Dark Ages;Dark Matter;Dark Nebula;Dark Star;Darkside;Dawn Patrol;Daydream;Daylight;Dead Reckoning;Deep Blue;Deep Dive;Deep Freeze;Deep Space;Deja Vu;Delta;Demon Hunter;Departure;Desert Bloom;Desert Fox;Desert Rose;Desert Wind;Destiny;Detroit Soul;Deus Ex;Devotion;Diamond;Digital Age;Digital Dream;Digital Ghost;Digital Rain;Dimension;Discovery;Distant Memories;Divided Sky;DNA;Dog Days;Dominion;Double Agent;Downpour;Downtown;Dragonfly;Drift;Drifting;Drive;Drop Zone;Dry Spell;Duality;Dusk to Dawn;Dust Devil;Dust to Dust;Dynamo;Early Bird;Earthbound;East Coast;Easy Street;Ebony;Echo;Echo AI;Echo Beach;Echo Chamber;Echoes;Eclipse;Eden;Edge of Tomorrow;El Dorado;Electric Blue;Electric Dreams;Element;Elemental;Elevation;Elite;Elixir;Elysium;Ember;Embrace;Emerald;Empathy;Empire;Encore;End of Time;Endgame;Endless;Endless Night;Endless Summer;Energy;Enigma;Epoch;Equinox;Escape;Essence;Eternal;Eternal Flame;Ether;Euphoria;Everest;Evergreen;Evolution;Exodus;Exploration;Fable;Facade;Fairytale;Falcon;Fallout;Far Away;Fate;Father Figure;Fearless;Feather;Feedback;Fiction;Final Call;Final Chapter;Final Frontier;Fire and Ice;Firefly;First Contact;First Light;Flame;Flashback;Flight;Float;Floodgates;Flora;Flow;Flying High;Focus;Fool's Gold;Footprints;Foreplay;Forever;Forgiven;Fortress;Forward;Found;Foxhole;Fracture;Fragment;Freefall;Frequency;Frontier;Frost;Full Circle;Full Moon;Fuse;Fusion;Future;Future AI;Future Echoes;Future Past;Galactic;Galactic Drift;Galaxy;Genesis;Ghost;Ghost Town;Ghostwriter;Gilded;Glacier;Gleam;Glitch;Glow;Golden Age;Golden Hour;Good Morning;Good Vibrations;Goodnight;Grace;Gravity;Green Light;Grayscale;Ground Zero;Guardian;Halcyon;Halfway;Halo;Hardwire;Harbor;Harmony;Havana;Haven;Heart of Gold;Heartbeat;Heatwave;Heavy Metal AI;Heirloom;Helios;High Tide;Highland;Highway;Horizon;Horizons;Hotline;Hourglass;Human Touch;Humble;Hummingbird;Hurricane;Hybrid;Hyperdrive;Hypnotic;Icebound;Icon;Identity;Ignite;Illusion;Illumination;Imagine;Impact;Impulse;Incognito;Indigo;Infinite;Infinity;Ingenium;Inheritance;Ink;Inner Peace;Inner Sanctum;Insight;Instant;Instinct;Interlude;Interstellar;Intrepid;Invincible;Ion;Iron;Ironclad;Island;Isotope;Ivory;Jade;Journey;Joyride;Judas;Junction;Jupiter;Kaleidoscope;Karma;Keepsake;Kingdom;Kinetic;Knight;Komorebi;Labyrinth;Lagoon;Landing;Lantern;Last Call;Last Stand;Latitude;Lavender;Legacy;Legend;Lemonade;Level Up;Liberation;Liberty;Lifeboat;Light and Shadow;Light Years;Lighthouse;Lightning;Limbo;Limelight;Liminal;Limitless;Line in the Sand;Lionheart;Liquid;Liquid Gold;Live Wire;Locomotive;Logbook;Logic;Long Way Home;Lookout;Loom;Loop;Lost and Found;Lost City;Lost in Time;Lotus;Low Tide;Lucid;Lullaby;Luminescence;Lunar;Lunar Drift;Lustre;Machine;Maelstrom;Magnetic;Magnolia;Maiden;Majestic;Manhattan;Manifest;Mansion;Mantra;Marathon;Marble;Marina;Maritime;Market;Marquee;Masquerade;Masterpiece;Matrix;Mayday;Maze;Meadow;Memento;Memory Lane;Mercenary;Meridian;Metropolis;Midnight;Midnight Echoes;Midnight Sun;Milestone;Mindseye;Mirage;Mirror;Mission;Mist;Modem;Momentum;Monarch;Monolith;Moonbeam;Moonlight;Moons;Morning Light;Mosaic;Motherland;Motion;Motorway;Mountain;Muse;Myth;Nebula;Nemesis;Neon;Neon Lights;Neon Rain;Neptune;Nerve;Nest;Network;New Day;New Era;New Horizons;New Wave;Nexus;Night Drive;Night Owl;Nightfall;Nightingale;Nimbus;Nomad;Northern Lights;Northstar;Nova;Nostalgia;Nuclear;Nucleus;Oasis;Oblivion;Obsidian;Ocean;Ocean Breeze;Odyssey;Offshore;Omega;Omni;On the Run;One Love;One Way;Only One;Onward;Onyx;Open Road;Open Sea;Optic;Oracle;Orbit;Orchard;Origin;Outbound;Outlaw;Outpost;Outrun;Overdrive;Overhaul;Overland;Overview;Pacifica;Palace;Pandora;Panorama;Paradigm;Paradise;Paradox;Paragon;Parallel;Paranoid;Passage;Pastel;Pathfinder;Patriot;Pavilion;Peak;Pedestal;Pendulum;Perception;Perfection;Permafrost;Phantasm;Phantom;Phase;Phenomenon;Phoenix;Photonic;Picnic;Pilgrim;Pillar;Pioneer;Pipeline;Pixel;Platform;Platinum;Plaza;Pleiades;Plutonium;Polar;Polaris;Porcelain;Port;Portal;Postcard;Powerline;Prelude;Prism;Profile;Promenade;Prophecy;Prospect;Prosperity;Proto;Proton;Pulse;Pure;Purge;Pyramid;Quantum;Quantum Leap;Quarantine;Quarry;Quartz;Quasar;Quickstep;Radiance;Radiant;Radical;Radio;Radioactive;Raindance;Raindrop;Rainforest;Rampart;Range;Rapture;Ravine;Ray;Reaching;Reactor;Real Time;Rebound;Reckoning;Recreation;Redux;Reflect;Reflection;Refuge;Regatta;Regent;Relic;Remedy;Reminiscence;Renaissance;Renegade;Replica;Resonance;Restless;Retrospect;Return;Reunite;Revelation;Reverie;Revival;Rhapsody;Rhythm;Ricochet;Riddle;Ridge;Riptide;Rise;Rising;Ritual;Rival;Riverbed;Roadmap;Rococo;Romance;Roosevelt;Rosewood;Roulette;Roundtrip;Rover;Royal;Rubble;Ruby;Runaway;Runner;Rupture;Rust;Sabotage;Safari;Safehouse;Sage;Sahara;Sailor;Salvation;Samurai;Sanctuary;Sandcastles;Sandstorm;Sapphire;Satellite;Saturn;Savage;Savanna;Scale;Scan;Scarab;Scenic;Scent;Scepter;Schism;Scorpio;Screenplay;Scripture;Sea Breeze;Seafarer;Seagull;Seashell;Second Wind;Secret;Section;Sedona;Seeker;Semaphor;Senate;Sentinel;Sequence;Serenade;Serendipity;Serenity;Serpentine;Shadow;Shadowbox;Shadowfax;Shaman;Shelter;Shift;Shimmer;Shipwreck;Shoreline;Showdown;Shrine;Shuttle;Sierra;Signals;Signature;Silhouette;Silica;Silk;Silmaril;Silver;Silver Lining;Silver Screen;Simulacrum;Sinclair;Siren;Sirens;Skyline;Skyscraper;Sleepwalker;Snapshot;Snowdrift;Snowfall;Solar;Solar Flare;Solar Wind;Solarium;Solitude;Sonata;Sonic;Soulmate;Soundscape;Southbound;Specter;Spectrum;Spellbound;Sphere;Spillover;Spire;Spirit;Splendor;Springtime;Starlight;Static;Status;Stealth;Stellar;Sublime;Summit;Sundown;Sunlight;Sunrise;Sunset;Supernova;Surreal;Synergy;Synthetic;Talisman;Tapestry;Tempest;Terminal;Terra;Threshold;Thunder;Tidal;Timeless;Titan;Tranquil;Transcend;Twilight;Typhoon;Ultraviolet;Underworld;Unified;Universal;Universe;Uptown;Valhalla;Valiant;Valley;Valkyrie;Vanity;Vector;Velocity;Velvet;Ventura;Venture;Venus;Verdant;Vessel;Vibration;Viceroy;Victor;Victory;Vigil;Vintage;Virtual;Visage;Vision;Vortex;Voyage;Vulcan;Wanderer;Warmth;Waterfall;Waves;Wayfarer;Whirlwind;Whisper;Wildflower;Wilderness;Windmill;Winter;Wisdom;Wonder;Woodland;Worldview;Zenith;Zephyr;Zero;Zodiac;Zone"
}
