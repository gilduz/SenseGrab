package com.ukuke.gl.sensormind.support;

/**
 * Created by gildoandreoni on 30/01/15.
 */
public class DataSample {
    private String feedPath;
    private Long timestamp;
    private Float value_1;
    private Float value_2;
    private Float value_3;
    private Double longitude;
    private Double latitude;

    public DataSample(String feedPath, Float value_1, Float value_2, Float value_3, Long timestamp, Double longitude, Double latitude) {
        this.feedPath = feedPath;
        this.value_1 = value_1;
        this.value_2 = value_2;
        this.value_3 = value_3;
        this.timestamp = timestamp;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getFeedPath() { return feedPath; }
    public Long getTimestamp() { return timestamp; }
    public float getValue_1() { return value_1; }
    public float getValue_2() { return value_2; }
    public float getValue_3() { return value_3; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}