package com.ithetrollidk.playerbalancer.server;

import com.ithetrollidk.playerbalancer.ping.ServerStatus;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;

public class BungeeServer {

    private final String group;
    private final String name;
    // Assume the server is online with room for players until the first real ping
    // completes. Previously this started as null, which crashed the priority
    // handler (NullPointerException) if a player joined before the first ping
    // cycle (~5s after startup) finished, producing "no default server" errors.
    private ServerStatus status = new ServerStatus(0, 9999);

    public BungeeServer(String group, String name) {
        this.group = group;

        this.name = name;
    }

    public ServerGroupStorage getGroup() {
        return ServerStorage.getInstance().getGroup(this.getGroupName());
    }

    public String getGroupName() {
        return this.group;
    }

    public ServerInfo getServerInfo() {
        return ProxyServer.getInstance().getServerInfo(this.getName());
    }

    public String getName() {
        return this.name;
    }

    public ServerStatus getStatus() {
        return this.status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }
}
