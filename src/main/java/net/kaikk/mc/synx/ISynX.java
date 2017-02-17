package net.kaikk.mc.synx;

/**
 * Interface used by SynX implementations.<br>
 * It's not used by developers that want to use SynX to transfer data between servers.
 * */
public interface ISynX {
	void log(String message);
	Config loadConfig() throws Exception;
	void startExchanger(DataExchanger exchanger, int interval);
	void stopExchanger(DataExchanger exchanger);
}
