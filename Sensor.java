package uk.ac.ed.inf.aqmap;

public class Sensor extends Position {

	private String name;
	private double battery;
	private String reading;

	
	// Constructor method for Sensor, taking the position battery name and reading ..
	
	public Sensor(String name, Position sensorPosition, double battery, String reading) {
		super(sensorPosition);
		this.name = name;
		this.battery = battery;
		this.reading = reading;
	}

	// Function for returning a string representation of the sensor 

	public String toString() {
		return "SENSOR: " + this.name + ", BATTERY : " + this.battery + " LOCATED @ " + super.toString();
	}

	// Function for returning the battery level of the Sensor

	public double getBattery() {
		return this.battery;
	}
	
	// Function for returning the reading of the sensor

	public String getReading() {
		return this.reading;
	}
	
	// Function for returning the position of the sensor

	public Position getPosition() {
		return this;
	}
	
	// Function returning the name of the sensor

	public String getName() {
		return this.name;
	}

}
