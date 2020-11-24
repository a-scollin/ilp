package uk.ac.ed.inf.aqmap;

public class Node extends Position {

	private double f;
	private double h;
	private double g;
	private Node parent;
	private boolean isEmpty;
	private Integer direction;

	// Constructor for Node object..

	public Node(Integer Direction, Node Parent, Position p) {
		super(p.getLng(), p.getLat());
		this.isEmpty = false;
		this.f = 0.0;
		this.g = 0.0;
		this.h = 0.0;
		this.direction = Direction;
		this.parent = Parent;

	}

	// Function for returning the parent of a Node

	public Node getParent() {

		return this.parent;
	}
	
	// Function for returning the direction taken towards a Node

	public Integer getDirection() {
		return this.direction;
	}
	
	// Function for returning wether the Node is a place holder - Used in testing 
	//TODO: aenfihaiefhiaef	

	public boolean isEmpty() {
		return this.isEmpty;
	}

	// Function for returning the F value of a node
	
	public double getF() {
		return this.f;
	}

	// Function for returning the H value of a node

	public double getH() {
		return this.h;
	}
	
	// Function for returning the G value of a node

	public double getG() {
		return this.g;
	}

	// Function for setting the F value of a node
	
	public void setF(double newf) {
		this.f = newf;
	}

	// Function for setting the G value of a node

	public void setG(double newg) {
		this.g = newg;
	}
	
	// Function for setting the H value of a node

	public void setH(double newh) {
		this.h = newh;
	}

	// Function for representing the node as a String..
	public String toString() {
		return super.toString() + " F : " + this.f + " DIRECTION : " + this.direction;
	}

}
