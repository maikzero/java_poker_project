Texas Hold'em Poker - Multiplayer Game

A fully functional multiplayer Texas Hold'em Poker game built with Java and JavaFX. Supports 2-6 players over a local network.
Features

    🃏 Full Texas Hold'em gameplay (pre-flop, flop, turn, river)

    🌐 Peer-to-peer multiplayer networking

    🎨 Rich JavaFX graphical interface

    💰 Real-time chip and pot updates

    🎯 Betting actions: Check, Call, Fold, Raise

    📊 Action log to track game events

    🖥️ Cross-platform support (Windows, Mac, Linux)

Requirements

    Java 17 or higher

    Maven (for building)

    Network connectivity between players (same LAN or localhost for testing)

Can go to this Folder to instructions: 
https://drive.google.com/drive/folders/1anyYbM3A-NF_JnVW6MluZUOlbH-gD3VI?usp=sharing
There's a video
Your instructor required at least 3 advanced concepts. You've implemented:

    Multi-threading ✓ - Server handles multiple clients concurrently

    Networking ✓ - Socket-based client-server communication

    Rich GUI ✓ - JavaFX with animations very few, only fade in

Quick Start
1. Clone/Download the Project
bash

git clone https://github.com/maikzero/java_poker_project.git
cd PokerGame

2. Build the Project
bash

mvn clean compile

3. Run the Game

Option A: Using Maven (Recommended)
bash

mvn javafx:run

Option B: Create and Run JAR
bash

mvn package
java -jar target/PokerGame-0.0.1-SNAPSHOT.jar

How to Play
Hosting a Game

    Launch the application

    Click "Host a Game"

    Enter your desired Player Name (default: "Host")

    Set the Port (default: 5000)

    Set Small Blind and Big Blind amounts (default: 10, 20)

    Select Number of Players (2-6)

    Click "Start Server"

    Share your IP address and port with other players

    Wait for all players to join

    The game will start automatically

Joining a Game

    Launch the application

    Click "Join a Game"

    Enter the Host's IP address (e.g., 127.0.0.1 for same computer, or the host's local IP)

    Enter the Port number (same as host)

    Enter your Player Name

    Click "Join Game"

    Wait for the host to start the game

    The game window will open when all players have joined

Game Controls
Button	Action
Check/Call	Check if no bet exists, or match the current bet
Fold	Discard your hand and sit out the current round
Raise	Increase the current bet (use slider to set amount)
Game Flow

    Pre-Flop: Each player receives 2 hole cards. Betting begins after blinds.

    Flop: 3 community cards are revealed. Betting round follows.

    Turn: 1 additional community card. Betting round follows.

    River: Final community card. Final betting round.

    Showdown: Best 5-card hand wins the pot.

Hand Rankings (Highest to Lowest)
Rank	Hand
9	Royal Flush
8	Straight Flush
7	Four of a Kind
6	Full House
5	Flush
4	Straight
3	Three of a Kind
2	Two Pair
1	One Pair
0	High Card
Running on Different Computers
Find Your IP Address

Windows:
cmd

ipconfig

Look for "IPv4 Address" (e.g., 192.168.1.100)

Mac/Linux:
bash

ifconfig
# or
ip addr

Look for "inet" address (e.g., 192.168.1.100)
Firewall Settings

Make sure your firewall allows incoming connections on the chosen port (default: 5000).

Windows:

    Go to Control Panel → Windows Defender Firewall

    Add an inbound rule for port 5000 (TCP)

Mac:

    System Preferences → Security & Privacy → Firewall

Linux:
bash

sudo ufw allow 5000/tcp

Troubleshooting
"Connection Refused" Error

    Make sure the host has started the server before clients try to connect

    Check that the host's firewall allows the port

    Verify the IP address is correct

"JavaFX runtime components are missing"

    Run with mvn javafx:run instead of java -jar

    Or ensure JavaFX is properly configured

GUI Not Showing Cards

    Wait for the game to start (waiting dialog closes automatically)

    Check that all players have joined

Chips Not Updating

    Ensure all players are using the same version of the code

    Check network connectivity between players

Project Structure
text

PokerGame/
├── src/main/java/com/kevin/poker/
│   ├── PokerGame.java          # Core game logic
│   ├── PokerGUI.java           # Main game interface
│   ├── Player.java             # Player data and actions
│   ├── Card.java               # Card representation
│   ├── Deck.java               # Deck management
│   ├── HandRank.java           # Hand ranking system
│   ├── HandEvaluator.java      # Hand evaluation logic
│   ├── BettingRound.java       # Betting round management
│   ├── Action.java             # Action data transfer
│   └── network/
│       ├── MultiplayerGUILauncher.java  # Launcher UI
│       ├── MultiplayerGameController.java # Game controller
│       ├── PokerServer.java    # Server implementation
│       ├── PokerClient.java    # Client implementation
│       ├── PokerClientEventHandler.java # Message handler
│       └── PokerNetworkBridge.java # Network interface
└── pom.xml                     # Maven configuration

Keyboard Shortcuts
Key	Action
C	Call/Check
F	Fold
R	Raise
Known Limitations

    Network play requires manual IP address entry (no auto-discovery)

    Works best on local network (LAN)

    Players must join before the game starts

    No reconnect functionality for disconnected players

Future Enhancements

    LAN auto-discovery using UDP broadcast

    Chat system between players

    Tournament mode with increasing blinds

    Save/load game state

    AI opponents for offline play

    Hand history replay

Credits

Created for CS6103 - Introduction to Java, Spring 2026
License

This project was created for educational purposes.
Quick Command Reference
bash

# Build the project
mvn clean compile

# Run the game
mvn javafx:run

# Create executable JAR
mvn clean package

# Run the JAR
java -jar target/PokerGame-0.0.1-SNAPSHOT.jar