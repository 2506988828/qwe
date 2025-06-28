package com.my.qwe.model;

public class DeviceInfo {
    public String deviceId;
    public String ip;
    public String mac;
    public String username;
    public String version;
    public String model;
    public String deviceName;
    public int state;
    public int rotate;
    public String width;
    public String height;
    public String name;

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", username='" + username + '\'' +
                ", ip='" + ip + '\'' +
                ", state=" + state +
                ", width=" + width +
                ", height=" + height +
                ", name=" + name +
                '}';
    }
}
