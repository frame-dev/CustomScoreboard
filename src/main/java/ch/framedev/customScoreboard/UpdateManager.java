package ch.framedev.customScoreboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class UpdateManager {

    private final CustomScoreboard plugin;

    public UpdateManager(CustomScoreboard plugin) {
        this.plugin = plugin;
    }

    private String getLatestVersion() {
        try {
            URL url = URI.create("https://framedev.ch/others/versions/customscoreboard-versions.json").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000); // 5 seconds timeout
            conn.setReadTimeout(5000); // 5 seconds timeout

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Parse the JSON response
                    JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                    return jsonObject.get("latest").getAsString();
                }
            } else {
                plugin.getLogger().warning("Failed to get latest version. Response code: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Error getting latest version: " + e.getMessage());
            return null;
        }
    }

    public File downloadUpdate() {
        try {
            String latestVersion = getLatestVersion();
            if (latestVersion == null) {
                plugin.getLogger().warning("Could not determine the latest version.");
                return null;
            }

            // Get the download URL from the JSON
            URL versionUrl = URI.create("https://framedev.ch/others/versions/customscoreboard-versions.json").toURL();
            HttpURLConnection versionConn = (HttpURLConnection) versionUrl.openConnection();
            versionConn.setRequestMethod("GET");
            versionConn.setRequestProperty("User-Agent", "Mozilla/5.0");
            versionConn.setConnectTimeout(5000); // 5 seconds timeout
            versionConn.setReadTimeout(5000); // 5 seconds timeout

            if (versionConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(versionConn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Parse the JSON response to get the download URL
                    JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                    String downloadUrl = jsonObject.get("download_url").getAsString();

                    // Download the JAR file
                    URL downloadUrlObj = URI.create(downloadUrl).toURL();
                    HttpURLConnection downloadConn = (HttpURLConnection) downloadUrlObj.openConnection();
                    downloadConn.setRequestMethod("GET");
                    downloadConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    downloadConn.setConnectTimeout(10000); // 10 seconds timeout for download
                    downloadConn.setReadTimeout(10000); // 10 seconds timeout for download

                    if (downloadConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        // Create update directory if it doesn't exist
                        File updateDir = new File(plugin.getServer().getUpdateFolder());
                        if (!updateDir.exists()) {
                            updateDir.mkdirs();
                        }

                        // Create the output file
                        File outputFile = new File(updateDir, "CustomScoreboard-" + latestVersion + ".jar");

                        // Download the file
                        try (InputStream in = downloadConn.getInputStream();
                                FileOutputStream out = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }

                        return outputFile;
                    } else {
                        plugin.getLogger()
                                .warning("Failed to download update. Response code: " + downloadConn.getResponseCode());
                        return null;
                    }
                }
            } else {
                plugin.getLogger()
                        .warning("Failed to get version information. Response code: " + versionConn.getResponseCode());
                return null;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Error downloading update: " + e.getMessage());
            return null;
        }
    }

    public boolean hasUpdate() {
        try {
            URL url = URI.create("https://framedev.ch/others/versions/customscoreboard-versions.json").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000); // 5 seconds timeout
            conn.setReadTimeout(5000); // 5 seconds timeout

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String oldVersion = CustomScoreboard.getInstance().getDescription().getVersion();
                    String newVersion = JsonParser.parseReader(br).getAsJsonObject().get("latest").getAsString();
                    return !newVersion.equalsIgnoreCase(oldVersion);
                }
            }
        } catch (IOException ignored) {
            // Silently ignore connection errors
        }
        return false;
    }

    public boolean isUpdateAvailable() {
        String latestVersion = getLatestVersion();
        if (latestVersion == null) {
            return false;
        }

        String currentVersion = plugin.getDescription().getVersion();
        return !currentVersion.equals(latestVersion);
    }

    public void checkForUpdates() {
        if (isUpdateAvailable()) {
            String latestVersion = getLatestVersion();
            plugin.getLogger().info("A new version of CustomScoreboard is available: " + latestVersion);
            plugin.getLogger().info("Current version: " + plugin.getDescription().getVersion());

            // Check if auto-download is enabled in config
            if (plugin.getConfig().getBoolean("autoDownload", false)) {
                plugin.getLogger().info("Auto-downloading update...");
                File updateFile = downloadUpdate();
                if (updateFile != null) {
                    if (installUpdate(updateFile)) {
                        plugin.getLogger()
                                .info("Update downloaded and installed successfully. Please restart the server.");
                    } else {
                        plugin.getLogger().warning("Failed to install the update.");
                    }
                } else {
                    plugin.getLogger().warning("Failed to download the update.");
                }
            } else {
                plugin.getLogger().info("To enable auto-updates, set 'autoDownload: true' in config.yml");
            }
        }
    }

    public boolean installUpdate(File updateFile) {
        if (updateFile == null || !updateFile.exists()) {
            plugin.getLogger().warning("Update file not found or invalid.");
            return false;
        }

        try {
            // Get the current plugin file - use the plugins directory instead of data
            // folder
            File pluginsDir = plugin.getDataFolder().getParentFile();
            File currentJar = new File(pluginsDir, "CustomScoreboard.jar");

            File currentJarRun = new File(pluginsDir,
                    "CustomScoreboard-" + plugin.getDescription().getVersion() + ".jar");
            if (currentJarRun.exists()) {
                currentJarRun.deleteOnExit();
            }
            // Create a backup of the current JAR
            File backupFile = new File(pluginsDir,
                    "CustomScoreboard-" + plugin.getDescription().getVersion() + ".jar.bak");
            if (currentJar.exists()) {
                try (FileInputStream in = new FileInputStream(currentJar);
                        FileOutputStream out = new FileOutputStream(backupFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }

            // Copy the update file to the plugins directory
            try (FileInputStream in = new FileInputStream(updateFile);
                    FileOutputStream out = new FileOutputStream(currentJar)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            plugin.getLogger().info(
                    "Update installed successfully. The server needs to be restarted for the changes to take effect.");
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Error installing update: " + e.getMessage());
            return false;
        }
    }
}
