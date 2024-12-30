package work.licht.music.id.core;

import work.licht.music.id.core.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
