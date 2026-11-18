<!--
MODRINTH PROJECT SETUP
Name ........ Clean Village
Summary ..... Some village houses are filthy and their owner is heartbroken. Grab a soapy sponge, scrub the house spotless and watch the villager celebrate.
Categories .. Adventure, Decoration, Game Mechanics (+ tag: Worldgen)
Environment . Client: required · Server: required
Loaders ..... Fabric (needs Fabric API), NeoForge, Forge · Game version 26.3
Icon ........ media/icon.png
Gallery ..... see docs/gallery.md (titles + descriptions ready to paste, in order; mark 01_dirty_house.jpg as "Featured")
Links ....... Source/Issues: https://github.com/JoelCardona01/clean-village
Trailer ...... media/trailer.mp4 → upload to YouTube and paste the link (Modrinth: "Video" in the description; CurseForge: Images tab › Videos)
-->

![Clean Village](https://raw.githubusercontent.com/JoelCardona01/clean-village/main/media/banner.png)

<p align="center"><b>Some village houses are filthy, and their owner is heartbroken.</b><br>
Grab a soapy sponge, scrub every stain, cobweb and patch of mold, and watch them come home, jump with joy and thank you with a hand-written letter.</p>

![Scrubbing a dirty house clean](https://raw.githubusercontent.com/JoelCardona01/clean-village/main/media/scrubbing.gif)

---

## 📖 What is Clean Village?

Clean Village is a small, self-contained quest that hides inside the villages you already know. Explore a village and you may find a house that looks abandoned: stained walls, moldy corners, fogged windows, cobwebs on the ceiling and the lights out. Inside, its owner is sitting on the edge of the bed, crying.

You clean the house, block by block, with a single tool, the **Soapy Sponge**. There are no menus and no tutorial. When the last stain is gone the villager walks back in, sees their home and celebrates, and everyone who helped is thanked in person.

It is built to feel good more than to be a chore: bubbles, scrubbing sounds, a sparkle for every block that comes clean, and a house that really looks different at the end.

![Celebration](https://raw.githubusercontent.com/JoelCardona01/clean-village/main/media/celebration.gif)

## 🏚️ How it plays

| | |
|---|---|
| **1. Discover** | When a village generates (or when you reach one nobody has spent time in), 1–2 of its houses can be turned into dirty houses. Which houses, and how dirty, is random: some are barely stained, others are a real project. |
| **2. Read their story** | A chest next to the bed holds **two Soapy Sponges** and a book called *What happened to my house*. There are six backstories: a flood, a chimney fire, a spider invasion, a pillager raid and more. |
| **3. Scrub** | Hold right click on any dirt with the sponge (left click works too). A **boss bar** shows how much of the house is still dirty, so you always know how far along you are. |
| **4. They're watching** | The moment you start, the owner stands up and waits by the door. At 50% they peek in to see how it's going. |
| **5. Celebrate** | At 100% the lights come back on, the whole house shines, and the villager walks in, looks around and jumps for joy to a little tune. |
| **6. Get thanked** | Every player who helped gets a signed thank-you letter, loot, a **Family Portrait**, *Hero of the Village* and a lasting discount with that villager. |

## 🧼 What counts as dirt

All of it is cleaned with the same sponge, one layer at a time:

- **Grime.** Thin layers stuck to walls, floors and ceilings (like glow lichen). Thick patches take up to three scrubs.
- **Mold.** Green growths in damp corners, also in layers.
- **Dirty blocks.** Walls, floors and pillars that are visibly filthy, with up to two stages of dirt.
- **Fogged windows.** Glass panes covered in a film you wipe off.
- **Dusty cobwebs.** Hanging from the ceiling. The sponge wipes away ordinary cobwebs too, without dropping string.

The outside is dirty as well, so you can spot a house that needs help from the street.

## 🧽 The Soapy Sponge

There is exactly one tool, and it does everything.

- **Use it:** hold right click (or left click) on dirt. Sweeping over clean blocks just pauses it, so you can scrub in long strokes.
- **It dries out:** every scrub uses some soap. When it is dry you get a message and a sound.
- **Recharge it:** right click on any water source to soak it again.
- **Get more:** two come in the house chest, or craft one from **yellow wool + a slime ball**.

## 🎁 Rewards

- A **hand-written letter** from the owner, signed with their name. Each backstory has its own letter.
- **Emeralds**, plus a random extra such as gold, cake or cookies, and with luck something rarer. Bigger houses give more loot rolls.
- A decorative **Family Portrait** with the owner's name.
- **Hero of the Village** (level I for 40 minutes by default), and a lasting discount with that villager.
- Everyone who helped is rewarded, not only the player who finished the house.

## ✨ Why you'll like it

- **A small story in every village.** Six backstories and letters, translated into five languages.
- **Works with vanilla villages.** All five types (plains, desert, savanna, taiga, snowy). The dirt is added on top of the existing houses.
- **Can't be cheesed.** The dirt can't be mined for free, pushed by pistons or blown up. The only way through is cleaning.
- **Multiplayer friendly.** Anyone can help, and everyone gets rewarded.
- **Lightweight.** Progress is tracked with a counter instead of scanning blocks, and the owner can't die while the quest is active (optional).
- **Languages:** English, Español, Français, Deutsch, Italiano. It uses your game language and falls back to English.

## ⚙️ Configuration

`config/cleanvillage.json` is created on first launch:

| Option | Default | What it does |
|---|---|---|
| `villageChance` | `0.6` | Chance (0 to 1) that a newly generated village gets dirty houses |
| `minHouses` / `maxHouses` | `1` / `2` | Dirty houses per selected village |
| `onlyNewVillages` | `true` | Only villages nobody has spent time in. Set `false` to allow any village |
| `invulnerableOwner` | `true` | The owner can't die while the quest is active |
| `heroLevel` / `heroMinutes` | `1` / `40` | Hero of the Village level (0–5) and duration |
| `unitsPerExtraRoll` / `maxLootRolls` | `40` / `4` | Bigger houses give more loot rolls |
| `ambientSounds` | `true` | Dripping and distant rain inside dirty houses |

The reward is a normal loot table: override `data/cleanvillage/loot_table/gameplay/house_reward.json` with a data pack to change it.

## 📦 Installation

| Loader | You need |
|---|---|
| Fabric | Fabric Loader 0.19+ and **Fabric API** |
| NeoForge | NeoForge 26.3 |
| Forge | Forge 66 (26.3) |

Install it on both the client and the server. Existing worlds work: dirty houses appear in **villages players haven't spent time in yet**.

## ❓ FAQ

**I can't find a dirty house.** About 60% of villages get one (configurable), and only villages nobody has spent more than a few minutes in. Travel to a village you haven't visited. Operators can run `/cleanvillage village` next to a village to prepare its houses.

**Can I add it to an existing world?** Yes. Villages you haven't explored yet can still get dirty houses. Set `onlyNewVillages` to `false` to allow any village.

**Can I change the rewards?** Yes, with a data pack (see above) or the config options.

**Is it compatible with other village mods?** It only touches houses from vanilla village structures. Buildings from other mods are left alone.

**Does it work on servers?** Yes, and it is required on both sides. Several players can clean the same house together.

---

<sub>Made by Joel Cardona · MIT License · All textures and sounds are original or vanilla.</sub>
