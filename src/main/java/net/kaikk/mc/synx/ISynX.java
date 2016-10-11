package net.kaikk.mc.synx;

import java.sql.SQLException;

import javax.sql.DataSource;

/** 
 * Interface used by SynX implementations.<br>
 * It's not used by developers that want to use SynX to transfer data between servers.
 * */
public interface ISynX {
	void log(String message);
	Config loadConfig() throws Exception;
	DataSource getDataSource(String hostname, String username, String password, String database) throws SQLException;
}
