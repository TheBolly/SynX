package net.kaikk.mc.synx;

import java.util.List;

public abstract class Config {
	protected String nodeName, dbHostname, dbDatabase, dbUsername, dbPassword;
	protected int waitTime;
	protected long defaultTTL;
	protected List<String> tags;
	protected boolean debug;
	
	public int getWaitTime() {
		return waitTime;
	}
	public long getDefaultTTL() {
		return defaultTTL;
	}
	public boolean isDebug() {
		return debug;
	}
}
