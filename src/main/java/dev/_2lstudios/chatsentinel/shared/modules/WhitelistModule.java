package dev._2lstudios.chatsentinel.shared.modules;

import java.util.Collection;
import java.util.regex.Pattern;

import dev._2lstudios.chatsentinel.shared.utils.PatternUtil;

public class WhitelistModule {
    private boolean enabled;
    private Collection<String> whitelistedServers;
    private Pattern pattern;

    public void loadData(boolean enabled, Collection<String> whitelistedServers, String[] patterns) {
        this.enabled = enabled;
        this.whitelistedServers = whitelistedServers;
        this.pattern = PatternUtil.compile(patterns);
    }

    public Collection<String> getWhitelistedServers() {
        return whitelistedServers;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
