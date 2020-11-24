package uk.ac.ed.inf.aqmap;

public class Sensor extends Position {

	private String name;
	private double battery;
	private String reading;

	public Sensor(String name, Position sensorPosition, double battery, String reading) {
		super(sensorPosition);
		this.name = name;
		this.battery = battery;
		this.reading = reading;
	}

	// Getters and toString

	public String toString() {
		return "SENSOR: " + this.name + ", BATTERY : " + this.battery + " LOCATED @ " + super.toString();
	}

	public double getBattery() {
		return this.battery;
	}

	public String getReading() {
		return this.reading;
	}

	public Position getPosition() {
		return this;
	}

	public String getName() {
		return this.name;
	}

}
