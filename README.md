# Run Dino Run

A side-scrolling endless runner written in Java. Control a dinosaur, jump over cacti, duck under flying birds, and survive for as long as you can while the game gets faster and the world shifts from day to night.

The entire game lives in a single file, `Dino.java`, and uses only the Java standard library. There is nothing extra to download or install apart from Java itself.

---

## Table of Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [Getting Started](#getting-started)
4. [How to Play](#how-to-play)
5. [Game Rules](#game-rules)
6. [Project Structure](#project-structure)
7. [How the Code Is Organised](#how-the-code-is-organised)
8. [Customising the Game](#customising-the-game)
9. [Troubleshooting](#troubleshooting)
10. [Contributing](#contributing)
11. [License](#license)

---

## Features

- Smooth, time-based movement that runs at the same speed on any computer
- Hand-drawn vector graphics: a shaded dinosaur, detailed cacti, and an animated flapping bird
- Day and night cycle with a sun, moon, twinkling stars, drifting clouds, and scrolling hills
- Variable jump height: tap for a short hop, hold for a full jump
- Ducking and fast falling
- Obstacles of different sizes, spacing, and heights, with difficulty that increases over time
- Dust particles and a screen shake when you crash
- Start, pause, and game-over screens
- High score that is saved between sessions

---

## Requirements

You need the **Java Development Kit (JDK)**, version 8 or newer. Version 17 or 21 is recommended.

The JDK includes two programs you will use:

| Program | Purpose |
| ------- | ------- |
| `javac` | Compiles (translates) your Java source code into a program the computer can run |
| `java`  | Runs the compiled program |

### Checking whether Java is already installed

Open a terminal (Command Prompt or PowerShell on Windows, Terminal on macOS and Linux) and type:

```
javac -version
```

If you see a version number such as `javac 21.0.2`, you are ready to go. If you see an error such as "command not found" or "not recognized", install a JDK using the steps below.

### Installing a JDK

1. Go to [adoptium.net](https://adoptium.net) and download the latest **Temurin** JDK for your operating system.
2. Run the installer. On Windows, tick the option that adds Java to your `PATH` if it is offered.
3. Close and reopen your terminal, then run `javac -version` again to confirm the installation.

---

## Getting Started

### Step 1: Get the code

Choose one of the following.

**Option A: Download as a ZIP**

1. Click the green **Code** button at the top of this repository page.
2. Choose **Download ZIP**.
3. Extract the ZIP file to a folder of your choice.

**Option B: Clone with Git**

```
git clone https://github.com/YOUR-USERNAME/YOUR-REPOSITORY.git
```

Replace `YOUR-USERNAME` and `YOUR-REPOSITORY` with the actual values for this repository.

### Step 2: Open a terminal in the project folder

Navigate to the folder that contains `Dino.java`:

```
cd path/to/your/folder
```

You can check you are in the right place by listing the files. On Windows use `dir`; on macOS and Linux use `ls`. You should see `Dino.java` in the list.

### Step 3: Compile the game

```
javac Dino.java
```

This creates several `.class` files in the same folder. Nothing is printed when compilation succeeds.

### Step 4: Run the game

```
java Dino
```

A window titled **Run Dino Run** will open. Press **Space** to start.

### Running from Visual Studio Code

If you prefer an editor:

1. Install the **Extension Pack for Java** from the Extensions panel.
2. Open the project folder with **File > Open Folder**.
3. Open `Dino.java` and click the **Run** button that appears above the `main` method.

---

## How to Play

### Controls

| Key | Action |
| --- | ------ |
| `Space`, `Up Arrow`, `W`, or `Enter` | Start the game, jump, and restart after game over |
| `Down Arrow` or `S` | Duck while running; fall faster while in the air |
| `P` or `Esc` | Pause and resume |

**Tip:** Jump height depends on how long you hold the jump key. A quick tap gives a short hop; holding it gives a full jump. This is useful for clearing low obstacles without flying into birds.

### Objective

Run as far as possible. Your score increases the longer you survive, and the game ends when you touch a cactus or a bird. Try to beat your high score.

---

## Game Rules

| Element | Behaviour |
| ------- | --------- |
| Speed | Starts at a steady pace and gradually increases up to a maximum |
| Score | Based on the distance travelled |
| Cacti | Appear in groups of one to three. Groups of three only appear after a score of 150 |
| Birds | Begin appearing after a score of 250 and fly at one of three heights: low, middle, or high |
| Ducking | Lets you pass under high birds. You can only duck while standing on the ground |
| Day and night | The scene switches between day and night every 600 points |
| Milestones | The score flashes every 100 points |
| High score | Saved automatically when you beat it |
| Auto-pause | The game pauses if the window loses focus |

### Where the high score is stored

The high score is saved in a small text file named `.dino_highscore` inside your user home folder, for example:

- Windows: `C:\Users\YourName\.dino_highscore`
- macOS: `/Users/YourName/.dino_highscore`
- Linux: `/home/yourname/.dino_highscore`

To reset your high score, delete this file.

---

## Project Structure

```
.
├── Dino.java      The complete game source code
└── README.md      This documentation
```

After compiling, you will also see `.class` files. These are generated automatically and do not need to be committed to version control. If you use Git, add the following line to a file named `.gitignore`:

```
*.class
```

---

## How the Code Is Organised

`Dino.java` is a single class that extends `JPanel`, which is a drawing surface from Java's Swing library. It follows a standard game structure.

| Section | What it does |
| ------- | ------------ |
| Configuration | Constants such as window size, gravity, jump strength, and speed limits |
| Game objects | Small classes for obstacles, clouds, and dust particles |
| State | Variables that track the dinosaur, score, speed, and current screen |
| Input | Handles keyboard presses and releases |
| Game loop | A timer that calls `update` and `repaint` about 60 times per second |
| Update | Moves everything, applies gravity, spawns obstacles, and checks for collisions |
| Collision | Compares the dinosaur's hit box against each obstacle's hit box |
| Drawing | Separate methods for the sky, hills, ground, cactus, bird, dinosaur, score, and screens |
| Entry point | The `main` method creates the window and starts the game |

### Key ideas for beginners

- **Game loop:** A game is a loop that repeats many times per second: read input, update the world, draw the result. Here, a `javax.swing.Timer` triggers the loop roughly every 16 milliseconds.
- **Delta time:** Movement is multiplied by the time since the last frame (`dt`). This keeps the game speed consistent even if a computer is slow.
- **Game states:** The `State` enum (`READY`, `RUNNING`, `PAUSED`, `OVER`) decides which screen is shown and which parts of the code run.
- **Hit boxes:** Collisions are checked using simple rectangles that are slightly smaller than the pictures, which makes crashes feel fair.

---

## Customising the Game

All of the main tuning values are near the top of `Dino.java`. After changing any value, recompile with `javac Dino.java` and run again.

| Constant | Default | Effect |
| -------- | ------- | ------ |
| `W`, `H` | `900`, `400` | Window width and height in pixels |
| `GRAVITY` | `2600` | How quickly the dinosaur falls. Higher values give a snappier jump |
| `JUMP_V` | `820` | Upward speed at the start of a jump. Higher values jump higher |
| `SHORT_HOP_V` | `520` | Upward speed cap when the jump key is released early |
| `START_SPEED` | `380` | Running speed at the start of a game |
| `MAX_SPEED` | `900` | The fastest the game will ever go |
| `ACCEL` | `9` | How quickly the game speeds up |

To make the game easier, try lowering `MAX_SPEED` or `ACCEL`. To make it harder, raise them.

---

## Troubleshooting

**`'javac' is not recognized` or `javac: command not found`**
Java is not installed, or it is not on your system `PATH`. Install a JDK as described above, then close and reopen your terminal.

**`Error: Could not find or load main class Dino`**
Make sure you compiled first with `javac Dino.java`, and that you are running `java Dino` from the same folder (without the `.java` ending).

**`class Dino is public, should be declared in a file named Dino.java`**
The file name must exactly match the class name, including capital letters. Do not rename the file.

**The window opens but the keys do nothing**
Click once inside the game window to give it focus, then try again. The game pauses when the window loses focus, so press `P` or `Space` to resume.

**The game runs choppily**
Close other heavy applications. On Linux, you can try running with `java -Dsun.java2d.opengl=true Dino`.

---

## Contributing

Contributions are welcome. To suggest a change:

1. Fork this repository.
2. Create a branch for your change, for example `git checkout -b add-new-obstacle`.
3. Make your changes and test them by running the game.
4. Commit with a clear message and push the branch to your fork.
5. Open a pull request describing what you changed and why.

Ideas for improvement:

- Sound effects and background music
- A second character or colour themes
- Gamepad or touch support
- Additional obstacle types
- A pause menu with settings

---
