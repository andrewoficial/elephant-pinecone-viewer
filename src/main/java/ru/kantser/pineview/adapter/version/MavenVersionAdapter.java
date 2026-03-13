package ru.kantser.pineview.adapter.version;

import ru.kantser.pineview.domain.port.VersionPort;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MavenVersionAdapter implements VersionPort {
    private static final String VERSION_FILE = "/version.properties";
    private String version;

    @Override
    public String getCurrentVersion() {
        if (version == null) {
            try (InputStream is = getClass().getResourceAsStream(VERSION_FILE)) {
                if (is == null) {
                    version = "unknown";
                } else {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("app.version", "unknown");
                }
            } catch (IOException e) {
                version = "unknown";
            }
        }
        return version;
    }
}