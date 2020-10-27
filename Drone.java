package uk.ac.ed.inf.aqmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.json.JSONObject;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class Drone {

	protected int moves = 150;
	private Position dronePos;
	private Position starting;
	public List<Sensor> unvisited; 
	private List<String> flightpath;
	public List<Point> flightPoints;
	
	public List<Position> flightPath = new ArrayList<Position>();
	
	/**TODO:= the nofly zone doesnt work because if the drone can fly completly through some thing 
	** it doesnt register at all,, also i think the marker recognition when seeing if it was visited or 
	*not is wrong or perhaps the flight path is wrong idk good to look at it...
	*/
	
	public Drone(Position startPos) {
		this.dronePos = startPos;
		this.starting = startPos;
		this.unvisited = new ArrayList<Sensor>();
		for(Sensor s : App.sensors) {
			this.unvisited.add(s);
		}
		this.flightpath = new ArrayList<String>();
		this.flightPoints = new ArrayList<Point>();
		
		flightPoints.add(Point.fromLngLat(startPos.getLng(), startPos.getLat()));
	}
	
	public Position getDronePosition() {
		return this.dronePos;
	}
	
	public void playAstarGreedy() {
		
		
		
		while(true) {
			
			Sensor nextsensor = this.getClosestSensor();
			
//			System.out.println("dronepos = " + this.dronePos.toString() );
//			System.out.println("nextsensor = " + nextsensor.toString());
			
			List<Integer> pos = this.reverse(this.Astar(this.dronePos, nextsensor.getPosition()));
			
			if(pos.size() == 0 && this.dronePos.distanceFrom(nextsensor.getPosition()) > 0.0001) {
				
				//System.out.println("booya" + this.dronePos.directionTo(nextsensor.getPosition()));
				this.moveDrone(this.dronePos.directionTo(nextsensor.getPosition()));
				
			}else {
				if(pos.size() == 0) {
					//System.out.println("oooo");
					pos = this.reverse(this.Astar(this.dronePos, this.getSecondClosestSensor().getPosition()));

				}
			}
			
			//System.out.println("ABOUT TO MOVE");
		
			for(int d : pos) {
				
				if(!this.moveDrone(d)) {
					return;
				}
			}
			
			App.saveToFile("oogabooga.geojson",this.getMap().toJson(),true);
			
			//System.out.println("saved.. " + this.moves + " moves left");
			
		}
		
	}
	
	public List<Integer> getDirections(List<Position> pos) {
		
		Position thepos = pos.get(0);
		
		List<Integer> ret = new ArrayList<Integer>();
		
		for(int i = 1 ; i < pos.size(); i++) {
			
			ret.add(thepos.directionTo(pos.get(i)));
		
			thepos = pos.get(i);
		
			
		}
		
		
		
		return ret;
		
		
	}

	private List<Integer> reverse(List<Integer> astar) {
		
		List<Integer> ret = new ArrayList<Integer>();
		
		
		for(int i = astar.size()-1 ; i >= 0;i--) {
			ret.add(astar.get(i));
		}
		
		
	
		
		
		return ret;
		
	}

	private List<Integer> Astar(Position dronePos, Position senspos) {
		
	
	
		HashSet<Node> open = new HashSet<Node>();
		
		HashSet<Node> closed = new HashSet<Node>();
		
		open.add(new Node(null,dronePos));
		
		while(open.size()>0) { 
			
			
			Node currentNode = new Node(Integer.MAX_VALUE);
			
			for(Node n : open) {
				if (n.getF()<currentNode.getF()) {
					
					currentNode=n;
				}
			}
			
			//System.out.println(currentNode.toString());
			
			open.remove(currentNode);
			
	        closed.add(currentNode);
	        
	        if(currentNode.distanceFrom(senspos) <= 0.0002) {
	          
	            List<Integer> directions = new ArrayList<Integer>();
	            
	            
	            while(currentNode != null) {
	            
	                
	                if(currentNode.getParent() != null) {
	                
	                directions.add(currentNode.getDirection());
	                
	                }
	                
	                currentNode = currentNode.getParent();
	           
	        }
	            return directions;
	       }
	        
			
	        HashSet<Node> children = new HashSet<Node>();
	        
	        for(int i = 0; i < 36; i++) {
	        	
	        	
	        	Position newnode = currentNode.move(i);
	        	
	        	//TODO ITS SAFE OR MOVE!! SAFE IS deifnatly FUCKED FIX IT !! STEP THROUGH CODE 
	        	
	        	if(!safe(newnode,i,App.closestdis)) {
	        		
	        		continue;
	        	}
	        	
	        	Node newnodee = new Node(i,currentNode, newnode);
	        	
	        	children.add(newnodee);
	        	
	        	
	        	
	        }
	        
//	        for(Node n : children) {
//        		System.out.println("Direction = " + n.getDirection());
//        	}
//	        
//	        System.out.println(children.size());
	        
	        for(Node child : children) {
	        	
	        	
	        	
	        	
	        	
	        	if(closed.contains(child)) {
	        		continue;
	        	}
	        	
	        	
	        	
	        	
	                child.setG(currentNode.getG() + 0.0003);  
	                //child.setH(Math.abs(child.getLng()) + Math.abs(child.getLat()));
	                child.setH(heuristic(child,senspos));
	                
	                child.setF(child.getG() + child.getH());
	                
	                Boolean breaker = false;
	                //todo distance from?????????????????????????????
	                for(Node opennode : open) {
		        	
	                	if( Double.compare(child.getLng(),opennode.getLng()) == 0 && Double.compare(child.getLat(),opennode.getLat()) == 0&&  child.getG() > opennode.getG()) {
	                		breaker = true;
	                		//CONTINUE WRONG ?
	        		
	                		break;
		        		}
	                }
	                
	                if(breaker) {
	                	continue;
	                }
	                
		        		
		        	open.add(child);
	               
	        
	        }
		
	}
		
		return null;
		
	}

	private double heuristic(Node child, Position senspos) {
			
		
		double dy = Math.abs(child.getLng()-senspos.getLng());
		double dx = Math.abs(child.getLat()-senspos.getLat());
		
		return 3 * (dy+dx); 
		
	}

	public void playRandom() {
		

    	
		
		int randomint = App.rnd.ints().findFirst().getAsInt()%36;
		
		while(!safe(this.dronePos,randomint,App.closestdis)) {
			randomint =  App.rnd.ints().findFirst().getAsInt()%36;
		}
		
		while (this.moveDrone(randomint)) {
			randomint = App.rnd.ints().findFirst().getAsInt()%36;
			while(!safe(this.dronePos,randomint,App.closestdis)) {
				randomint =  App.rnd.ints().findFirst().getAsInt()%36;
			}
		}
		

    	
		
		
	}
	
	private boolean safe(Position oldPosition,int randomint, double closestdis) {
		
		// This works on a bound is not 100% accurate though better than last time ! 
		
		Position oldPos = oldPosition;
		Position newPos = oldPosition.move(randomint);
		//Position nePos = oldPos.move((randomint + 9)%36, closestdis);
	//	Position swPos = newPos.move((randomint + 9)%36, closestdis);

		
		Point oldPointnw = Point.fromLngLat(oldPos.getLng(), oldPos.getLat());
		
		//Point ne = Point.fromLngLat(nePos.getLng(), nePos.getLat());
		
		Point newPointse = Point.fromLngLat(newPos.getLng(), newPos.getLat());
//		
//		Point sw = Point.fromLngLat(swPos.getLng(), swPos.getLat());
//		
//		List<Point> pathPoints = new ArrayList<Point>();
//		
//		pathPoints.add(oldPointnw);
//		pathPoints.add(ne);
//		pathPoints.add(sw);
//		pathPoints.add(newPointse);
//		pathPoints.add(oldPointnw);

//		BoundingBox dronePath = BoundingBox.fromPoints(oldPointnw, newPointse);
//		
//		System.out.println(dronePath.toJson());
		
		
//		
		for(int i = 1 ; i < 10*(Math.ceil(0.0003 / closestdis))+1;i++) {
			
			if(oldPos.move(randomint,(closestdis/10)*i).isInRestricted() || !oldPos.move(randomint,(closestdis/10)*i).isInArea()) {
				return false;
			}
			
		}
		
		
	
		return (!oldPos.move(randomint).isInRestricted() && oldPos.move(randomint).isInArea());
		
	}

	public List<String> getFlightPath() {
		
		return this.flightpath;
				
	}
	
	public FeatureCollection getMap() {
		
		List<Feature> features = new ArrayList<Feature>();
		
		for(Sensor s : App.sensors) {
		
			
			if(!this.unvisited.contains(s)) {
			
				
				Feature visitedsensor = Feature.fromGeometry(Point.fromLngLat(s.getPosition().getLng(),s.getPosition().getLat()));
				
						
				visitedsensor.addStringProperty("marker-size", "medium");
				visitedsensor.addStringProperty("location", s.getName());
				if(s.getBattery()<10) {
					
					visitedsensor.addStringProperty("marker-symbol", "lighthouse");	
					visitedsensor.addStringProperty("marker-color", "#000000");	
					visitedsensor.addStringProperty("rgb-string", "#000000");	

				}else {
					Double reading = Double.parseDouble(s.getReading());
					visitedsensor.addStringProperty("marker-color", App.getRGB(reading));	
					visitedsensor.addStringProperty("rgb-string", App.getRGB(reading));	
					if( reading > 128) {
						visitedsensor.addStringProperty("marker-symbol", "danger");	
					}else {
						visitedsensor.addStringProperty("marker-symbol", "lighthouse");	
					}
					
					
				}
				
				
				
			features.add(visitedsensor);
				
				
				
			}else {
				
				Feature unvisitedsensor = Feature.fromGeometry(Point.fromLngLat(s.getPosition().getLng(),s.getPosition().getLat()));

				
				unvisitedsensor.addStringProperty("marker-size", "medium");
				unvisitedsensor.addStringProperty("location", s.getName());	
				unvisitedsensor.addStringProperty("marker-color", "#aaaaaa");	
				unvisitedsensor.addStringProperty("rgb-string", "#aaaaaa");	
				
				features.add(unvisitedsensor);
			}
		}
		
		features.add(getLineString());
		
		
		return FeatureCollection.fromFeatures(features);
		
	}
	
	public Feature getLineString() {
		
		return Feature.fromGeometry(LineString.fromLngLats(this.flightPoints));
	
	}
	
	public boolean moveDrone(int direction) {	
		
		if(this.moves < 1 || (this.moves < 150 && this.dronePos.equals(this.starting))) {
			
			return false;
		
		} else {
		
			
			
			Position before = new Position(this.dronePos.getLng(),this.dronePos.getLat());		
					
			this.dronePos = this.dronePos.move(direction);
			
			String sensorname = readNearest();
			
			
			this.flightpath.add(Integer.toString(151-this.moves)+","+Double.toString(before.getLng())+","+Double.toString(before.getLat())+","+this.dronePos.getLng()+","+this.dronePos.getLat()+","+sensorname);
			
			this.flightPoints.add(Point.fromLngLat(this.dronePos.getLng(),this.dronePos.getLat()));
			
			
			
			this.moves--;
			
			return true;
		}
	}

	private String readNearest() {
		
		if (this.getClosestSensor().getPosition().distanceFrom(this.dronePos) <= 0.0002) {
			
			String sensorname = this.getClosestSensor().getName();
			System.out.println("DISTANCE CHECK..");
			System.out.println("DRONE: " +this.dronePos.toString());
			System.out.println("SENSOR: " + this.getClosestSensor().getPosition().toString());
			
			this.unvisited.remove(this.getClosestSensor());
			
			System.out.println(sensorname);
			return sensorname;
		}else {
			return "null";
		}
		
	}
	
	public Sensor getClosestSensor() {
		Position currentPos = new Position(this.dronePos.getLng(), this.dronePos.getLat());
		Sensor closestSensor = new Sensor("null",this.starting, 0.0, "");
		if(unvisited.size() > 0) {
			closestSensor = unvisited.get(0);
		for (Sensor sensor: unvisited) {
			if (currentPos.distanceFrom(sensor.getPosition()) < currentPos.distanceFrom(closestSensor.getPosition())) {
				closestSensor = sensor;
			}
		}
		
		}
		
		System.out.println(closestSensor.toString());

		return closestSensor;
	}
	

	public Sensor getSecondClosestSensor() {
		Position currentPos = new Position(this.dronePos.getLng(), this.dronePos.getLat());
		Sensor closestSensor = unvisited.get(0);
		Sensor secondClosest = unvisited.get(1);
		for (Sensor sensor: unvisited) {
			if (currentPos.distanceFrom(sensor.getPosition()) < currentPos.distanceFrom(closestSensor.getPosition())) {
				secondClosest = closestSensor;
				closestSensor = sensor;
			}
		}
		
		//System.out.println(closestSensor.toString());

		return secondClosest;
	}

	
}
