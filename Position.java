package uk.ac.ed.inf.aqmap;

import com.mapbox.geojson.*;
import com.mapbox.turf.*;

public class Position {

	private double lng;
	private double lat;

	// Constructors

	public Position(double lng, double lat) {
		this.lng = lng;
		this.lat = lat;
	}

	public Position(Position copyPosition) {

		this.lng = copyPosition.getLng();
		this.lat = copyPosition.getLat();
	}

	// Getters, toString method

	public String toString() {
		return " LNGLAT : " + this.lng + "," + this.lat;
	}

	public double getLng() {
		return this.lng;
	}

	public double getLat() {
		return this.lat;
	}

	// Comparison methods

	public Boolean isInRestricted() {

		return TurfJoins.inside(Point.fromLngLat(this.lng, this.lat), App.restrictedareas);

	}

	public Boolean isInArea() {

		return TurfJoins.inside(Point.fromLngLat(this.lng, this.lat), App.CONFINEMENT_AREA);

	}

	public double distanceFrom(Position otherPosition) {
		return Math.sqrt(Math.pow((otherPosition.getLng() - this.lng), 2) + Math.pow((otherPosition.getLat() - this.lat), 2));
	}

	public boolean equals(Position otherPosition) {
		return (this.lng == otherPosition.getLng() && this.lat == otherPosition.getLat());

	}

	public Integer directionTo(Position otherPosition) {

		double a_lat = Math.toRadians(this.lat);

		double delta_lng = Math.toRadians(this.lng) - Math.toRadians(otherPosition.getLng());

		double b_lat = Math.toRadians(otherPosition.getLat());

		double X = Math.cos(b_lat) * Math.sin(delta_lng);

		double Y = Math.cos(a_lat) * Math.sin(b_lat) - Math.sin(a_lat) * Math.cos(b_lat) * Math.cos(delta_lng);

		return Math.floorMod(Math.abs(((int) Math.round(Math.toDegrees(Math.atan2(X, Y))) / 10)),36);

	}

	// Move methods

	public Position move(int direction) {

		// 0 is east
		double angle = Math.toRadians((direction) * 10);

		// drone can only move 0.0003 degrees
		double lat = 0.0003 * Math.cos(angle);
		double lng = 0.0003 * Math.sin(angle);

		Position nextPos = new Position(this.lng + lng, this.lat + lat);

		return nextPos;

	}

	public Position move(int direction, double dist) {

		// 0 is east
		double angle = Math.toRadians((direction) * 10);

		double lat = dist * Math.cos(angle);
		double lng = dist * Math.sin(angle);

		Position nextPos = new Position(this.lng + lng, this.lat + lat);

		return nextPos;

	}

}
