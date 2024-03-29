# Halfminer Bot
TeamSpeak 3 Bot for Minecraft Server [Two and a half Miner](https://halfminer.de).
Connecting to query via [TeamSpeak-3-Java-API](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API).

Current features
-------
- Configurable via YAML based config file
  - Query password can either be passed as command line argument or be set via config
  - Supports connecting to the TeamSpeak query via SSH or Telnet/Raw
- Bot messages are fully configurable/localizable
- Automatic reconnect if connection is lost
- Define channel for bot to join, stays persistent if moved out
- Cold storage as flat file
  - Regulary stores client data to disk, if necessary
  - Bot can be restarted without losing state, like the clients channel id
    - Use command *!admin stop/restart* to save state before shutdown
- Permission system
  - Define what client belongs to which group via their talk power
  - Higher groups automatically inherit all permissions of lower groups
  - Permission changes are always logged
- Command chat interface with !<command>
  - Bot welcomes (server and/or channel) joining clients with private message
    - Server join message can be disabled
  - Command flood protection (optional bypass permission)
    - Custom cooldowns available additionally, for example for *!channel update*
  - Default command configurable via locale file, *!channel create* by default
  - Permission to use command necessary
- **Commands**
  - !admin
    - Lookup client information via username or client id
      - Supports client, database and unique id as parameters
      - Checks database for offline client with given database or unique id, if no player was found
    - Reload the config file
      - Won't reload if file was not modified or if it is in invalid format
    - Restart (full reconnect) or shut the bot down
  - !broadcast
    - Broadcast a given message to all clients
    - Optional talk power requirement can be passed as flag
      - Example: "!broadcast -200 hello" will broadcast "hello" to everybody with at least 200 talk power
    - Broadcast format configurable via locale file
      - Adds server group and broadcaster to message
  - !channel
    - Create channels for users
      - Gives channel admin to the creating user
        - Set group ID via config
          - Custom group for donators assignable
      - Adds username to the channelname
      - Sets the channel as temporary
        - Stays persistent for set amount of seconds (config)
          - Custom time for donators assignable
        - Detect if user already has a channel, automatically move user to his channel on join/chat/move
    - Sets given parameter as password
    - Update a users channel password
      - Will kick all players from channel after changing password
  - !help
    - Sends list containing every available command per client
      - Checks if client has permission
      - Sends correct usage and description
    - When calling !help *querypassword* the highest available group will be granted (can be disabled)
  - !rank (*not on master branch*)
    - Automatic retrieval of ranks after querying Halfminer REST API for privileges
    - Removes old server group, even if a different identity was used
- **Scheduled tasks**
  - *Periodically check for inactive users*
    - Move them into AFK channel
    - If server is full, kick AFK users to make room
      - Configure amount of players to kick at once
    - Exempt clients via permissions
  - *Send current status to API via HTTP PUT* (*not on master branch*)