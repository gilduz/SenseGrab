package com.ukuke.gl.sensormind.support;

/**
 * Created by gildoandreoni on 30/01/15.
 */
public class DataSample {
    private String feedPath;
    private Long timestamp;
    private float value;
    private Float longitude;
    private Float latitude;

    public DataSample(String feedPath, float value, Long timestamp, Float longitude, Float latitude) {
        this.feedPath = feedPath;
        this.value = value;
        this.timestamp = timestamp;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getFeedPath() { return feedPath; }
    public Long getTimestamp() { return timestamp; }
    public Float getValue() { return value; }
    public Float getLongitude() { return longitude; }
}