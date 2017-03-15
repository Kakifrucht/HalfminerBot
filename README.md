# Halfminer Bot
Teamspeak 3 Bot for Minecraft Server [Two and a half Miner](https://halfminer.de).
Connecting to query via [API](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API).

Current features
-------
- Configurable
- Create channels for users
  - Gives channel admin to the creating user
  - Adds their username to the channelname
  - Sets the channel as temporary
  - Sets given message as password
  - Floodprotection, limit channel creation
  - Detect if user already has a channel, automatically move user to his channel on join/chat/move
- Command interface with !
  - Default command if none supplied is !channelcreate, to make channel creation easier
- Scheduled tasks
  - Periodically check for inactive users
    - Move them into AFK channel
    - If server is full, kick AFK users to make room
  - Send current status to API via HTTP PUT
