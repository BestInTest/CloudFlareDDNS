package main;

import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class Configuration {

    File file;

    private String key;
    private String record;
    private boolean toTray;
    private boolean autoRefresh;
    private int interval;

    Configuration(String configFile) {
        file = new File(configFile);
        if (!file.exists()) {
            createNewConfig(file);
        }
        loadConfig(file);
    }

    private void loadConfig(File f) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(f);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        // Read values
        key = config.getString("api-key", "your_api_key");
        record = config.getString("domain-record", "your.domain-record.net");
        toTray = config.getBoolean("start-in-background", false);
        autoRefresh = config.getBoolean("automation.enable-auto-refresh", true);
        interval = config.getInt("automation.refresh-interval", 60);
    }

    private void createNewConfig(File f) {
        YamlConfiguration config = new YamlConfiguration();

        config.set("api-key", "your_api_key");

        config.set("domain-record", "your.domain-record.net");
        config.setComments("domain-record", Collections.singletonList("Type A record from your domain"));

        config.set("start-in-background", false);
        config.setComments("start-in-background", Arrays.asList("Set to 'true' only if you added this program to autostart.", "You can also force this option using '--to-tray' if you run this program from terminal"));

        config.set("automation.enable-auto-refresh", true);
        config.setComments("automation.enable-auto-refresh", Collections.singletonList("Enable auto refresh? (true/false)"));

        config.set("automation.refresh-interval", 60);
        config.setComments("automation.refresh-interval", Collections.singletonList("Refresh interval (in minutes)"));

        //Save config to file
        try {
            config.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveChanges() {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        config.set("automation.enable-auto-refresh", autoRefresh);
        config.set("automation.refresh-interval", interval);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getKey() {
        return key;
    }

    public String getRecord() {
        return record;
    }

    public boolean isToTray() {
        return toTray;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
