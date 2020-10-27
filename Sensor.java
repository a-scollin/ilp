package uk.ac.ed.inf.aqmap;

public class Sensor {

	private Position sensorPosition;
	private String name;
	private double battery;
	private String reading;
	
	public Sensor(String name, Position sensorPosition, double battery, String reading) {
		this.sensorPosition = sensorPosition;
		this.name = name;
		this.battery = battery;
		this.reading = reading;
	}
	
	public String toString() {
		return "SENSOR: " + this.name + ", BATTERY : " + this.battery + " LOCATED @ " + this.sensorPosition.toString();
	}

	public double getBattery() {
		return this.battery;
	}
	public String getReading() {
		return this.reading;
	}
	
	public Position getPosition() {
		return this.sensorPosition;
	}
	
	public String getName() {
		return this.name;
	}
	
}
