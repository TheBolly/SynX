package net.kaikk.mc.synx;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

public abstract class Config {
	protected String nodeName, dbHostname, dbDatabase, dbUsername, dbPassword;
	protected int interval;
	protected long defaultTTL;
	protected List<String> tags;
	protected boolean debug;

	public int getInterval() {
		return interval;
	}
	public long getDefaultTTL() {
		return defaultTTL;
	}
	public boolean isDebug() {
		return debug;
	}

	public abstract DataSource getDataSource() throws SQLException;
}
