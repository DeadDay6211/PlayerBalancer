package com.ithetrollidk.playerbalancer.ping;

import com.ithetrollidk.playerbalancer.Callback;
import com.ithetrollidk.playerbalancer.PlayerBalancer;
import com.ithetrollidk.playerbalancer.server.BungeeServer;
import com.ithetrollidk.playerbalancer.server.ServerGroupStorage;
import com.ithetrollidk.playerbalancer.server.ServerStorage;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.bedrock.BedrockPong;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StatusStorage {

    private static final StatusStorage instance = new StatusStorage();

    public static StatusStorage getInstance() {
        return instance;
    }

    public void init() {
        ProxyServer.getInstance().getLogger().info(String.format("Starting the ping task, the interval is %s", 100));

        ProxyServer.getInstance().getScheduler().scheduleRepeating(() -> {
            for (ServerGroupStorage groupStorage : ServerStorage.getInstance().getGroups()) {
                for (BungeeServer bungeeServer : groupStorage.getServers().values()) {
                    this.update(bungeeServer);
                }
            }
        }, 100);
    }

    public void update(BungeeServer server) {
        this.ping(server.getServerInfo(), (status, throwable) -> {
            if (status == null) {
                // Log why the ping failed instead of silently marking the server
                // offline/full - this was previously swallowed completely.
                PlayerBalancer.getInstance().getLogger().warning(String.format(
                        "Ping to server %s failed, keeping previous status: %s",
                        server.getName(), throwable
                ));
                // Keep the previous status instead of resetting to a fresh
                // ServerStatus() (which reports maxPlayers=0 / offline). A single
                // failed ping should not instantly make the priority handler think
                // the server has no room.
                return;
            }

            /*PlayerBalancer.getInstance().getLogger().info(String.format(
                    "Updated server %s, status: [Players: %s, Maximum Players: %s, Online: %s]",
                    server.getName(), status.getOnlineCount(), status.getMaxPlayers(), status.isOnline()
            ));*/

            server.setStatus(status);
        });
    }

    public void ping(ServerInfo server, Callback<ServerStatus> callback) {
        if (server == null) return;

        ProxyServer.getInstance().getScheduler().scheduleAsync(() -> {
            try {
                // 5 second timeout instead of 1 second - 1s was too aggressive and
                // could time out under normal load, wrongly marking a healthy
                // server as offline/full.
                BedrockPong rakNetPong = ((BedrockServerInfo) server).ping(5000, TimeUnit.MILLISECONDS).get();
                callback.done(new ServerStatus(rakNetPong.playerCount(), rakNetPong.maximumPlayerCount()), null);
            } catch (InterruptedException | ExecutionException e) {
                callback.done(null, e);
            }
        });
    }
}
