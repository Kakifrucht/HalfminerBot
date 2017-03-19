# Halfminer Bot
Teamspeak 3 Bot for Minecraft Server [Two and a half Miner](https://halfminer.de).
Connecting to query via [API](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API).

Current features
-------
- Configurable
- Command interface with !<command>
  - Command flood protection
  - Default command if none supplied is !channelcreate, to make channel creation easier
- **Commands**
  - !channelcreate
    - Create channels for users
    - Gives channel admin to the creating user
    - Adds their username to the channelname
    - Sets the channel as temporary
      - Stays for 3 minutes after leaving
    - Sets given parameter as password
    - Detect if user already has a channel, automatically move user to his channel on join/chat/move
  - !help
    - Sends list containing every command
- **Scheduled tasks**
  - *Periodically check for inactive users*
    - Move them into AFK channel
    - If server is full, kick AFK users to make room
  - *Send current status to API via HTTP PUT*
