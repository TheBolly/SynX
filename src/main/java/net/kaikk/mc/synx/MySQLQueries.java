package net.kaikk.mc.synx;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import net.kaikk.mc.kaiscommons.mysql.AMySQLQueries;
import net.kaikk.mc.kaiscommons.mysql.MySQLConnection;
import net.kaikk.mc.synx.packets.Node;

public class MySQLQueries extends AMySQLQueries {
	private PreparedStatement createTableData, createTableServers, createTableTransfers, getNodes, insertNode, dataReceiverSelect, deleteTransfer, dataSenderInsertData, dataSenderInsertTransfer, updateLastActivity, purgeTransfers1, purgeTransfers2;
	private MySQLConnection<? extends AMySQLQueries> connection;
	private String currentRegisteredChannelsSQLList;

	@Override
	public void init(MySQLConnection<? extends AMySQLQueries> connection) throws SQLException {
		this.connection = connection;
		this.createTableData = connection.prepareStatement("CREATE TABLE IF NOT EXISTS synx_data (id bigint(20) unsigned NOT NULL AUTO_INCREMENT,req int(11) NOT NULL,channel char(16) NOT NULL,tod bigint(20) NOT NULL,dat varbinary(32767) NOT NULL,PRIMARY KEY (id),KEY channel (channel));");
		this.createTableServers = connection.prepareStatement("CREATE TABLE IF NOT EXISTS synx_servers (id int(11) NOT NULL AUTO_INCREMENT,name char(8) NOT NULL,lastaction bigint(20) NOT NULL,tags varchar(255) NOT NULL,PRIMARY KEY (id),UNIQUE KEY name (name));");
		this.createTableTransfers = connection.prepareStatement("CREATE TABLE IF NOT EXISTS synx_transfers (id bigint(20) NOT NULL AUTO_INCREMENT,dest int(11) NOT NULL,dataid bigint(20) unsigned NOT NULL,PRIMARY KEY (id),KEY dest (dest));");

		this.getNodes = connection.prepareStatement("SELECT * FROM synx_servers WHERE lastaction > ?");
		this.insertNode = connection.prepareStatement("INSERT INTO synx_servers (name, lastaction, tags) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE lastaction = VALUES(lastaction), tags = VALUES(tags)", Statement.RETURN_GENERATED_KEYS);

		this.deleteTransfer = connection.prepareStatement("DELETE FROM synx_transfers WHERE id = ?");
		this.dataSenderInsertData = connection.prepareStatement("INSERT INTO synx_data (req, channel, tod, dat) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		this.dataSenderInsertTransfer = connection.prepareStatement("INSERT INTO synx_transfers (dest, dataid) VALUES (?, ?)");

		this.updateLastActivity = connection.prepareStatement("UPDATE synx_servers SET lastaction = ? WHERE id = ?");
		this.purgeTransfers1 = connection.prepareStatement("DELETE FROM synx_transfers WHERE dataid NOT IN (SELECT id FROM synx_data)");
		this.purgeTransfers2 = connection.prepareStatement("DELETE d, t FROM synx_data AS d LEFT OUTER JOIN synx_transfers AS t ON t.dataid = d.id WHERE d.tod < ?");
	}

	public void createTables() throws SQLException {
		this.createTableData.executeUpdate();
		this.createTableServers.executeUpdate();
		this.createTableTransfers.executeUpdate();
	}

	public ResultSet getNodes() throws SQLException {
		getNodes.setLong(1, System.currentTimeMillis()-86400000);
		return getNodes.executeQuery();
	}

	public ResultSet insertNode(String nodeName, String tags) throws SQLException {
		insertNode.setString(1, nodeName);
		insertNode.setLong(2, System.currentTimeMillis());
		insertNode.setString(3, tags);
		insertNode.executeUpdate();
		return insertNode.getGeneratedKeys();
	}

	public void initChannelsPreparedStatements() throws SQLException {
		if (this.currentRegisteredChannelsSQLList != SynX.instance().registeredChannelsSQLList) {
			this.currentRegisteredChannelsSQLList = SynX.instance().registeredChannelsSQLList;
			this.dataReceiverSelect = connection.prepareStatement("SELECT t.id, d.req, d.channel, d.dat, d.tod FROM synx_transfers AS t, synx_data AS d WHERE t.dest = ? AND t.dataid = d.id AND d.channel IN ("+currentRegisteredChannelsSQLList+")");
		}
	}

	public ResultSet dataReceiverSelect(int id) throws SQLException {
		this.initChannelsPreparedStatements();

		this.dataReceiverSelect.setInt(1, id);
		return this.dataReceiverSelect.executeQuery();
	}

	public void deleteTransfers(Collection<Long> ids) throws SQLException {
		for (Long id : ids) {
			this.deleteTransfer.setLong(1, id);
			this.deleteTransfer.addBatch();
		}
		this.deleteTransfer.executeBatch();
	}

	public ResultSet insertData(int packetId, String channel, long timeOfDeath, Serializable data) throws SQLException {
		this.dataSenderInsertData.setInt(1, packetId);
		this.dataSenderInsertData.setString(2, channel);
		this.dataSenderInsertData.setLong(3, timeOfDeath);
		this.dataSenderInsertData.setObject(4, data);
		this.dataSenderInsertData.executeUpdate();
		return this.dataSenderInsertData.getGeneratedKeys();
	}

	public void insertTransfers(Collection<Node> nodes, int dataId) throws SQLException {
		for (Node node : nodes) {
			this.dataSenderInsertTransfer.setInt(1, node.getId());
			this.dataSenderInsertTransfer.setInt(2, dataId);
			this.dataSenderInsertTransfer.addBatch();
		}
		this.dataSenderInsertTransfer.executeBatch();
	}

	public void updateLastActivity(int serverId) throws SQLException {
		this.updateLastActivity.setLong(1, System.currentTimeMillis());
		this.updateLastActivity.setInt(2, serverId);
		this.updateLastActivity.executeUpdate();
	}

	public void purgeTransfers() throws SQLException {
		this.purgeTransfers1.executeUpdate();
		this.purgeTransfers2.setLong(1, System.currentTimeMillis());
		this.purgeTransfers2.executeUpdate();
	}
}
