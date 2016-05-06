##SynX
Plugin that provides an easy way to transfer data between servers.

## Download
All builds for my plugins can be found at this link: http://kaikk.net/mc/

####Commands
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
Add SynX to your build path. Use `SynX.getInstance().broadcast(String channel, byte[] data)` and `SynX.getInstance().send(String channel, byte[] data, Node... destination)` to send data. I suggest to use a `ByteStreams.newDataOutput()` to help generating a byte array of data to be sent and `ByteStreams.newDataInput()` for received data. Your plugin can receive data by implementing the ChannelListener class and using `SynX.getInstance().register(Plugin instance, String channel, ChannelListener channelListener)` to register it.
