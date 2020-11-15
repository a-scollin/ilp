package uk.ac.ed.inf.aqmap;

public class Node extends Position {

	private double f;
	private double h;
	private double g;
	private Node Parent;
	private boolean isEmpty;
	private Integer direction;
	

	
	public Node(Integer Direction, Node Parent, Position p) {
		super(p.getLng(), p.getLat());
		this.isEmpty = false;
		this.f = 0.0;
		this.g = 0.0;
		this.h = 0.0;
		this.direction = Direction;
		this.Parent = Parent;
				
	}
	
	//Getters 
	
	public Node getParent() {
		// TODO Auto-generated method stub
		return this.Parent;
	}
	
	
	public Integer getDirection() {
		return this.direction;
	}
	
	public boolean isEmpty() {
		return this.isEmpty;
	}
	
	public double getF() {
		return this.f;
	}

	public double getH() {
		return this.h;
	}

	public double getG() {
		return this.g;
	}
	
	//Setters
	public void setF(double newf) {
		this.f = newf;
	}
	
	public void setG(double newg) {
		this.g = newg;
	}
	
	public void setH(double newh) {
		this.h = newh;
	}

	//toString
	public String toString() {
		return super.toString() + " F : " + this.f + " DIRECTION : " + this.direction;
	}


	
	
}
