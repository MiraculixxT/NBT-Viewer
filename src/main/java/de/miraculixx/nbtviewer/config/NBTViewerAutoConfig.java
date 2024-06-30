package de.miraculixx.nbtviewer.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "nbtviewer")
public class NBTViewerAutoConfig implements ConfigData {
    public String exclude = "";
}
