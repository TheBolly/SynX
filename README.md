##SynX
Plugin that provides an easy way to transfer data between servers. Compatible with Bukkit, Sponge, and Bungeecord servers. 

## Download
All builds for my plugins can be found at this link: http://kaikk.net/mc/

## Install
Place the jar in your plugin folder. The jar file is hybrid so the same jar file can be used for Bukkit, Sponge, and Bungeecord. Start the bukkit/sponge/bungeecord server so a default config file is generated. On Bukkit and Bungeecord you'll find the config at /plugins/SynX/config.yml. On Sponge, you'll find the config at /config/synx.conf. Be sure to set a short node name for all your servers and a shared MySQL database. Now you can add any plugin that use SynX to transfer data. If you haven't already, I highly recommend to add [Sync](https://github.com/KaiKikuchi/Sync): it's a plugin that allows running commands from a remote bukkit/sponge/bungeecord server.

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

### Developers: How to implement easily SynX into your plugin!
Add SynX to your build path. Maven:  
```xml
<repository>
  <id>net.kaikk.mc</id>
  <url>http://kaikk.net/mc/repo/</url>
</repository>
```
```xml
<dependency>
  <groupId>net.kaikk.mc</groupId>
  <artifactId>SynX</artifactId>
  <version>1.1</version>
  <type>jar</type>
  <scope>provided</scope>
</dependency>
```
1. Make a new class that implements Serializable. Add all the attributes that you want to be sent to the other servers. For this example, I'll call this class "SerializableExample".
2. Use `SynX.instance().broadcast("Example", Serializable object)` to send data to all your servers. The parameters of this method are a channel name (you choose one) and an object of the class that you made on step 2 that contains the data you want to be sent to the other servers.
3. Implement the ChannelListener interface to your main plugin class. Implement the `onPacketReceived(Packet packet)` method on your main plugin class. The packet object is the data received from the other servers. You can convert data back to an object by using `SynXUtils.convertFromBytes(SerializableExample.class, packet.getData())`
4. In your plugin onEnable method, register the channel with `SynX.instance().register(this, "Example", this)`. The parameters of this method are the plugin instance, a channel name (the same you choose previously for the broadcast method), and an object that implements the ChannelListener interface (in our example, it's the plugin main class).
