package me.darragh.lmc.transpiler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author darraghd493
 * @since 1.0.0
 */
public interface Transpiler {
    void build();

    void save(Path path) throws IOException;
}
