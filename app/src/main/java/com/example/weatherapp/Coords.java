package com.example.weatherapp;

public class Coords {
    String  geo_lat;
    String geo_lon;
    int qc_geo;

    public Coords(String geo_lat, String geo_lon, int qc_geo) {
        this.geo_lat = geo_lat;
        this.geo_lon = geo_lon;
        this.qc_geo = qc_geo;
    }

    public String getGeo_lat() {
        return geo_lat;
    }

    public String getGeo_lon() {
        return geo_lon;
    }

    public int getQc_geo() {
        return qc_geo;
    }
}
