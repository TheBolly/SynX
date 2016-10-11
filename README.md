##SynX
Plugin that provides an easy way to transfer data between servers. Compatible with Bukkit, Sponge, and Bungeecord servers. 

## Download
All builds for my plugins can be found at this link: http://kaikk.net/mc/

## Install
Place the jar in your plugin folder. The jar file is hybrid so the same jar file can be used for Bukkit, Sponge, and Bungeecord. Start the bukkit/sponge/bungeecord server so a default config file is generated. On Bukkit and Bungeecord you'll find the config at /plugins/SynX/config.yml. On Sponge, you'll find the config at /config/synx.conf. Be sure to set a short node name for all your servers. Now you can add any plugin that use SynX to transfer data. If you haven't already, I highly recommend to add [Sync](https://github.com/KaiKikuchi/Sync): it's a plugin that allows running commands from a remote bukkit/sponge/bungeecord server.

####Commands
Currently, commands are not available on Bungeecord.

######/synx nodes
- shows a list with all known nodes

######/synx tags
- shows a list with all known tags 

######/synx reload
- reloads the plugin

####Permissions
- synx.manage - Permission necessary to run all commands (default: op)

####Requirements
- MySQL database

### Developers: How to use
Add SynX to your build path. Maven:  

```
<repository>
  <id>net.kaikk.mc</id>
  <url>http://kaikk.net/mc/repo/</url>
</repository>
<dependency>
  <groupId>net.kaikk.mc</groupId>
  <artifactId>SynX-Core</artifactId>
  <version>0.10</version>
  <type>jar</type>
  <scope>provided</scope>
</dependency>
```
       
Use `SynX.instance().broadcast(String channel, byte[] data)` and `SynX.instance().send(String channel, byte[] data, Node... destination)` to send data.  
I suggest to use a `ByteStreams.newDataOutput()` to help generating a byte array of data to be sent and `ByteStreams.newDataInput()` for received data.  
Your plugin can receive data by implementing the ChannelListener class and using `SynX.instance().register(Plugin instance, String channel, ChannelListener channelListener)` to register it.
