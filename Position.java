package uk.ac.ed.inf.aqmap;

import com.mapbox.geojson.*;
import com.mapbox.turf.*;

public class Position {

	private double lng;
	private double lat;

	
	//Constructors
	
	public Position(double lng, double lat) {
		this.lng = lng;
		this.lat = lat;
	}
	
	public Position(Position move) {
		
		this.lng = move.getLng();
		this.lat = move.getLat();
	}


	// Getters, toString method

	public String toString() {
		return " LNGLAT : " + this.lng + ","+this.lat;
	}

	public double getLng() {
		return this.lng;
	}
	
	public double getLat() {
		return this.lat;
	}

	//Comparison methods
	
	public Boolean isInRestricted() {
		
		return TurfJoins.inside(Point.fromLngLat(this.lng,this.lat),App.restrictedareas);
		
	}
	public Boolean isInArea() {
		
		return TurfJoins.inside(Point.fromLngLat(this.lng,this.lat),App.CONFINEMENT_AREA);
		
		
	}
	
	public double distanceFrom(Position position) {
		return Math.sqrt(Math.pow((position.getLng() - this.lng), 2) + Math.pow((position.getLat() - this.lat), 2));
	}
	

	public boolean equals(Position other) {
		return (this.lng == other.getLng() && this.lat == other.getLat());
		
	}
	
	public Integer directionTo(Position position) {
		
		
		double alat = Math.toRadians(this.lat);
		
		double dellng = Math.toRadians(this.lng) - Math.toRadians(position.getLng());
		
		double blat = Math.toRadians(position.getLat());
		
	
		
		double X = Math.cos(blat) * Math.sin(dellng);
		
		double Y = Math.cos(alat)*Math.sin(blat) - Math.sin(alat)*Math.cos(blat)*Math.cos(dellng);
	
		return Math.abs(((int) Math.round(Math.toDegrees(Math.atan2(X, Y)))/10))%36;
		
		
	}
	
	//Move methods
	
	public Position move(int direction) {
		
			//0 is east 
			double angle = Math.toRadians((direction) * 10);
			
			//drone can only move 0.0003 degrees
			double lat = 0.0003 * Math.cos(angle);
			double lng = 0.0003 * Math.sin(angle);
			
			Position nextPos = new Position(this.lng + lng, this.lat + lat);

			return nextPos;
		
	
	}
	
	public Position move(int direction, double dist) {
		
		//0 is east 
		double angle = Math.toRadians((direction) * 10);
		

		double lat = dist * Math.cos(angle);
		double lng = dist * Math.sin(angle);
		
		Position nextPos = new Position(this.lng + lng, this.lat + lat);

		return nextPos;
	

}

	




}
