package uk.ac.ed.inf.aqmap;

import com.mapbox.geojson.*;
import com.mapbox.turf.*;

public class Position {

	private double lng;
	private double lat;

	// Constructor for position from coordinates

	public Position(double lng, double lat) {
		this.lng = lng;
		this.lat = lat;
	}

	// Constructor for position from another position - duplicates position

	public Position(Position copyPosition) {

		this.lng = copyPosition.getLng();
		this.lat = copyPosition.getLat();
	}

	// This function returns a string representing the coordinates of a position

	public String toString() {
		return " LNGLAT : " + this.lng + "," + this.lat;
	}


	// Function for returning the longitude of a position
	
	public double getLng() {
		return this.lng;
	}

	// Function for returning the latitude of a position 

	public double getLat() {
		return this.lat;
	}

	// Function for testing wether the position is in the nofly zones..

	public Boolean isInRestricted() {

		return TurfJoins.inside(Point.fromLngLat(this.lng, this.lat), App.restrictedareas);

	}
	
	// Function for testing wether the position is in the drone confinement zone..

	public Boolean isInArea() {

		return TurfJoins.inside(Point.fromLngLat(this.lng, this.lat), App.CONFINEMENT_AREA);

	}

	// Function for calculating the euclidian distance from this position to another position..

	public double distanceFrom(Position otherPosition) {
		return Math.sqrt(Math.pow((otherPosition.getLng() - this.lng), 2) + Math.pow((otherPosition.getLat() - this.lat), 2));
	}
	
	// Function for testing wether this position equals another position depending on the longitude and latitude

	public boolean equals(Position otherPosition) {
		return (this.lng == otherPosition.getLng() && this.lat == otherPosition.getLat());

	}

	// Function for returning the direction from this position to another using trigonomitry

	public Integer directionTo(Position otherPosition) {

		double a_lat = Math.toRadians(this.lat);

		double delta_lng = Math.toRadians(this.lng) - Math.toRadians(otherPosition.getLng());

		double b_lat = Math.toRadians(otherPosition.getLat());

		double X = Math.cos(b_lat) * Math.sin(delta_lng);

		double Y = Math.cos(a_lat) * Math.sin(b_lat) - Math.sin(a_lat) * Math.cos(b_lat) * Math.cos(delta_lng);

		return Math.floorMod(Math.abs(((int) Math.round(Math.toDegrees(Math.atan2(X, Y))) / 10)),36);

	}

	// This function returns the position calculated from moving this position in a certain direction for 0.0003 .. 

	public Position move(int direction) {

		// 0 is east
		double angle = Math.toRadians((direction) * 10);

		// drone can only move 0.0003 degrees
		double lat = 0.0003 * Math.cos(angle);
		double lng = 0.0003 * Math.sin(angle);

		Position nextPos = new Position(this.lng + lng, this.lat + lat);

		return nextPos;

	}

	// This function returns the position calculated from moving this position in a certain direction for a certain distance .. 

	public Position move(int direction, double dist) {

		// 0 is east
		double angle = Math.toRadians((direction) * 10);

		double lat = dist * Math.cos(angle);
		double lng = dist * Math.sin(angle);

		Position nextPos = new Position(this.lng + lng, this.lat + lat);

		return nextPos;

	}

}
