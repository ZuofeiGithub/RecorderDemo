package com.testopensourceapplication.Bean;

import android.graphics.drawable.Drawable;

/**
 * Created by ZHao on 2016/12/14.
 */
public class AppInfo {
    private String name;
    private Drawable icon;
    private String packageName;
    private String packagePath;
    private String versionName;
    private int versionCode;
    private boolean isSystem;

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setIsSystem(boolean isSystem) {
        this.isSystem = isSystem;
    }

    /**
     * @param name        名称
     * @param icon        图标
     * @param packageName 包名
     * @param packagePath 包路径
     * @param versionName 版本号
     * @param versionCode 版本码
     * @param isSystem    是否系统应用
     */
    public AppInfo(String packageName, String name, Drawable icon, String packagePath,
                   String versionName, int versionCode, boolean isSystem) {
        this.setName(name);
        this.setIcon(icon);
        this.setPackageName(packageName);
        this.setPackagePath(packagePath);
        this.setVersionName(versionName);
        this.setVersionCode(versionCode);
        this.setIsSystem(isSystem);
    }

    @Override
    public String toString() {
        return "App包名：" + getPackageName() +
                "\nApp名称：" + getName() +
                "\nApp图标：" + getIcon() +
                "\nApp路径：" + getPackagePath() +
                "\nApp版本号：" + getVersionName() +
                "\nApp版本码：" + getVersionCode() +
                "\n是否系统App：" + isSystem();
    }
}
