package com.htetznaing.adbotg;

import java.util.List;

public class SpywareApp {
    private final String name;
    private final String icon;
    private final String installer;
    private final List<String> permissions;
    private final String flag;

    public SpywareApp(String name, String icon, String installer, List<String> permissions, String flag) {
        this.name = name;
        this.icon = icon;
        this.installer = installer;
        this.permissions = permissions;
        this.flag = flag;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getInstaller() {
        return installer;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getFlag() {
        return flag;
    }

    // ... other getters ...
} 