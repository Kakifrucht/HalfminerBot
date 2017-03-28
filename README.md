# Halfminer Bot
Teamspeak 3 Bot for Minecraft Server [Two and a half Miner](https://halfminer.de).
Connecting to query via [API](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API).

Current features
-------
- Configurable via YAML based config file
  - Query password can either be passed as command line argument or always be set via config
- Define channel for bot to join, stays persistent if moved out
- Permission system
  - Define what client belongs to which group via their talk power
  - Higher groups automatically inherit all permissions of lower groups
  - Permission changes are always logged
- Command chat interface with !<command>
  - Command flood protection
  - Default command if none supplied is !channel, to make channel creation easier
  - Permission to use command necessary
- **Commands**
  - !admin
    - Lookup player information via username or client id
    - Reload the config file
      - Won't reload if file was not modified or if it is in invalid format
    - Restart (full reconnect) or shut the bot down
  - !channel
    - Create channels for users
      - Gives channel admin to the creating user
        - Set group ID via config
      - Adds username to the channelname
      - Sets the channel as temporary
        - Stays persistent for set amount of seconds (config)
        - Detect if user already has a channel, automatically move user to his channel on join/chat/move
    - Update a users channel password
      - Will kick all players from channel after changing password
    - Sets given parameter as password
  - !help
    - Sends list containing every command
- **Scheduled tasks**
  - *Periodically check for inactive users*
    - Move them into AFK channel
    - If server is full, kick AFK users to make room
      - Configure amount of players to kick at once
    - Exempt clients via permissions via permissions
  - *Send current status to API via HTTP PUT*
