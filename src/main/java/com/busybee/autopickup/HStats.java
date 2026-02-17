package com.busybee.autopickup;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HStats {

    private final String URL_BASE = "https://api.hstats.dev/api/";

    private final String modUUID;
    private final String modVersion;
    private final String serverUUID;

    public HStats(String modUUID, String modVersion) {
        this.modUUID = modUUID;
        this.modVersion = modVersion;

        this.serverUUID = getServerUUID();
        if (this.serverUUID == null) {
            System.out.println("[HStats] Metrics are disabled on this server.");
            return;
        }

        logMetrics();
        addModToServer();
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::logMetrics, 1, 1, TimeUnit.MINUTES);
    }

    public HStats(String modUUID) {
        this(modUUID, "Unknown");
    }

    private void logMetrics() {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("server_uuid", this.serverUUID);
        arguments.put("players_online", String.valueOf(getOnlinePlayerCount()));
        arguments.put("os_name", System.getProperty("os.name"));
        arguments.put("os_version", System.getProperty("os.version"));
        arguments.put("java_version", System.getProperty("java.version"));
        arguments.put("cores", String.valueOf(Runtime.getRuntime().availableProcessors()));

        sendRequest(URL_BASE + "server/update-server", arguments);
    }

    private void addModToServer() {
        System.out.println("[HStats] Adding mod to server metrics...");

        Map<String, String> arguments = new HashMap<>();
        arguments.put("server_uuid", this.serverUUID);
        arguments.put("plugin_uuid", this.modUUID);
        arguments.put("plugin_version", this.modVersion);

        boolean success = sendRequest(URL_BASE + "server/add-plugin", arguments);
        if (success) {
            System.out.println("[HStats] Successfully registered mod with metrics server.");
        } else {
            System.out.println("[HStats] Failed to register mod with metrics server.");
        }
    }

    private String getServerUUID() {
        Path serverUUIDFile = Paths.get("hstats-server-uuid.txt");
        try {
            if (Files.exists(serverUUIDFile)) {
                String content = Files.readString(serverUUIDFile);
                content = content.trim();
                String[] lines = content.split("\n");
                if (lines.length < 5)
                    return null;
                String enabled = lines[3].split("=")[1].trim();
                if (!enabled.equalsIgnoreCase("true"))
                    return null;
                return lines[4];
            } else {
                String uuid = UUID.randomUUID().toString();
                Files.writeString(serverUUIDFile, "HStats - Hytale Mod Metrics (hstats.dev)\nHStats is a simple metrics system for Hytale mods. This file is here because one of your mods/plugins uses it, please do not modify the UUID. HStats will apply little to no effect on your server and analytics are anonymous, however you can still disable it.\n\nenabled=true\n" + uuid);
                return uuid;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private boolean sendRequest(String urlString, Map<String, String> arguments) {
        try {
            URL url = URI.create(urlString).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            StringJoiner sj = new StringJoiner("&");
            for (Map.Entry<String, String> entry : arguments.entrySet()) {
                sj.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" +
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
            http.setFixedLengthStreamingMode(out.length);

            try (OutputStream os = http.getOutputStream()) {
                os.write(out);
            }

            int code = http.getResponseCode();
            http.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private int getOnlinePlayerCount() {
        return Universe.get().getPlayerCount();
    }

}
