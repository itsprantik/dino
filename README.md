# Run Dino Run

Too bored playing the bland black and white chrome dino, i reimagined it creating new things and developing something better. control the dinasaur like you do when the internet goes off and you have nothing left to do. 

The entire game is a single java file named, "Dino.java"  and uses inbuilt java library. Anyone with java installed can play this game.

## Requirements

You need java and java development kit (JDK) installed ( higher than version 8 )

the JDK uses two programs

| Program  |          Purpose         |
|                |                                |
|"javac"      | Compiles the code   |
|"java"        | Runs the code        |

## Check if whether Java is installed or not

For Windows
Open Windows Powershell or Command Promt and type:

```
javac -version
```

For MacOS and Linux
Open Terminal and type: 
```
javac -version
```

If you see any version number then you have JDK isntalled. Of you see error then JDK is missing.

### Installing a JDK

1. Install java from [Oracle|Java] (https://www.oracle.com/in/java/) and download JDK
2. Run the installer. 
3. And recheck again using the above command.

---

## How to run 
### Get the code from releases
Choose one of the following.
**Option A: Download as a ZIP**
Download source code from releases named source code.zip
and extract it and run Dino.jar or open cmd in that folder and run
```
java -jar Dino.jar
```

**Option B: clone using git**
```
git clone https://github.com/itsprantik/dino.git    
```
open the folder in any code editor supporting java and JDK
and run Dino.java or Dino.jar
```
java Dino.java
```
or
```
java -jar Dino.jar
```

### Run the game
```
java Dino
```

A window titled **Run Dino Run** will open.

## How to Play
### Controls
~~~
  SPACE / UP / W / ENTER - jump (hold for a higher jump)
  DOWN / S - duck (in the air: fast fall)
  P / ESC - pause
  SPACE - start / restart
~~~
**note:** Jump depends on how long you hold the jump key.

### Objective
I dont really want to tell you about the classic , every one knows it and if you dont know it then you are brain dead.

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

## Project Structure
~~~
.
├── build
|     ├──Dino.class
|     ├──Dino$1.class
|     ├──Dino$2.class
|     ├──Dino$Cloud.class
|     ├──Dino$Obstacle.class
|     ├──Dino$Particle.class
|     └──Dino$State.class
├──Dino.jar
├──Dino.java
├──Dino$1.class
├──Dino$2.class
├──Dino$Cloud.class
├──Dino$Obstacle.class
├──Dino$Particle.class
├──Dino$State.class
├──manifest.txt
└──README.md
~~~

### How the Code Is Organised

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
| Entry point | The `main` method creates the window and starts the game|

Made by[@itsprantik](https://github.com/itsprantik) as a fun project for me and my project on stardance