package ru.kantser.pineview.domain.port;

import ru.kantser.pineview.domain.model.ReleaseInfo;
import java.io.IOException;

public interface ReleaseInfoPort {
    ReleaseInfo fetchLatestRelease() throws IOException;
}