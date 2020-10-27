package uk.ac.ed.inf.aqmap;

import java.util.List;

import com.mapbox.geojson.*;
import com.mapbox.turf.*;

public class Position {

	private double lng;
	private double lat;

	public Position(double lng, double lat) {
		this.lng = lng;
		this.lat = lat;
	}
	

	
	public Position(Position move) {
		
		this.lng = move.getLng();
		this.lat = move.getLat();
	}



	public String toString() {
		return " LNGLAT : " + this.lng + ","+this.lat;
	}

	public Boolean isInRestricted() {
		
		return TurfJoins.inside(Point.fromLngLat(lng,lat),App.restrictedareas);
		
	}
	public Boolean isInArea() {
		
		return TurfJoins.inside(Point.fromLngLat(lng,lat),App.confinementarea);
		
		
	}
	
	

	public double distanceFrom(Position position) {
		return Math.sqrt(Math.pow((position.getLng() - this.lng), 2) + Math.pow((position.getLat() - this.lat), 2));
	}
	public double getLng() {
		return this.lng;
	}
	public double getLat() {
		return this.lat;
	}
	
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
		double angle = Math.toRadians(direction * 10);
		
		//drone can only move 0.0003 degrees
		double lat = dist * Math.cos(angle);
		double lng = dist * Math.sin(angle);
		
		Position nextPos = new Position(this.lng + lng, this.lat + lat);

		return nextPos;
	

}

	public static double getClosestDistanceFromArray(List<Position> poslist) {
		
		double ret = 100000000000.00;
		
		for(int i = 0 ; i < poslist.size() ; i++) {
			
			double dis = poslist.get(i).distanceFrom(poslist.get((i+1)%poslist.size()));
			
			if(dis < ret) {
				ret = dis;
			}
			
			
		}
		
		return ret;

		
	}
	public boolean equals(Position other) {
		return (this.lng == other.getLng() && this.lat == other.getLat());
		
	}



	public Integer directionTo(Position position) {
		
		
		double alat = Math.toRadians(this.lat);
		
		double dellng = Math.toRadians(this.lng) - Math.toRadians(position.getLng());
		
		double blat = Math.toRadians(position.getLat());
		
		System.out.println(alat);
		System.out.println(dellng);
		System.out.println(blat);
		
		double X = Math.cos(blat) * Math.sin(dellng);
		
		double Y = Math.cos(alat)*Math.sin(blat) - Math.sin(alat)*Math.cos(blat)*Math.cos(dellng);
	
		return ((int) Math.round(Math.toDegrees(Math.atan2(X, Y)))/10 + 27)%36;
		
		
	}
}
