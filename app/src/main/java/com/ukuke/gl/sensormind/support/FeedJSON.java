package com.ukuke.gl.sensormind.support;

import java.util.ArrayList;
import java.util.List;

public class FeedJSON {


	private String s_uid;
	private String label;
	private String description;  
	private Boolean is_static_located;
	private String measure_unit;
	private int type_id;
	private Double static_altitude; 
	private Double static_latitude; 
	private Double static_longitude;
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String name) {
		this.label = name;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public Boolean isIs_static_located() {
		return is_static_located;
	}


	public void setIs_static_located(Boolean is_static_located) {
		this.is_static_located = is_static_located;
	}


	public String getMeasure_unit() {
		return measure_unit;
	}


	public void setMeasure_unit(String measure_unit) {
		this.measure_unit = measure_unit;
	}


	public String getS_uid() {
		return s_uid;
	}


	public void setS_uid(String s_uid) {
		this.s_uid = s_uid;
	}

	public int getType_id() {
		return type_id;
	}


	public void setType_id(int type_id) {
		this.type_id = type_id;
	}


	public Double getStatic_altitude() {
		return static_altitude;
	}


	public void setStatic_altitude(Double static_altitude) {
		this.static_altitude = static_altitude;
	}


	public Double getStatic_latitude() {
		return static_latitude;
	}


	public void setStatic_latitude(Double static_latitude) {
		this.static_latitude = static_latitude;
	}


	public Double getStatic_longitude() {
		return static_longitude;
	}


	public void setStatic_Longitude(Double static_longitude) {
		this.static_longitude = static_longitude;
	}


	public FeedJSON(String name, Boolean is_static_located,
			String measure_unit, String s_uid,
			int type_id) {
		super();
		this.label = name;
		this.is_static_located = is_static_located;
		this.measure_unit = measure_unit;
		this.s_uid = s_uid;
		this.type_id = type_id;
	}


	public FeedJSON(String name, String description,
			Boolean is_static_located, String measure_unit, String s_uid,
			 int type_id, Double static_altitude,
			Double static_latitude, Double static_longitude) {
		super();
		this.label = name;
		this.description = description;
		this.is_static_located = is_static_located;
		this.measure_unit = measure_unit;
		this.s_uid = s_uid;
		this.type_id = type_id;
		this.static_altitude = static_altitude;
		this.static_latitude = static_latitude;
		this.static_longitude = static_longitude;
	}
}
