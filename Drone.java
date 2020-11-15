package uk.ac.ed.inf.aqmap;

import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;



public class Drone {

	
	public int moves = 150;
	private Position dronePos;
	private Position starting;
	private List<Sensor> unvisited; 
	private List<String> flightpath;
	private List<Point> flightPoints;
	private double[][] distances;
	
	//Constructor for drone 
	public Drone(Position startPos) {
		
		
		if(!startPos.isInArea()) {
			throw new Error("Start Position needs to be in confinement zone !");
		}
		
		this.dronePos = startPos;
		
		this.starting = startPos;
		
		this.unvisited = new ArrayList<Sensor>(App.sensors);

		this.flightpath = new ArrayList<String>();
		
		this.flightPoints = new ArrayList<Point>();
		
		flightPoints.add(Point.fromLngLat(startPos.getLng(), startPos.getLat()));
		
		this.distances = new double[this.unvisited.size()+1][this.unvisited.size()+1];
		
		List<Position> allpos = new ArrayList<>();
		
		allpos.add(this.starting);
		allpos.addAll(this.unvisited);
		
		for(int i = 0 ; i < allpos.size(); i++) {
			for(int j = 0 ; j < allpos.size(); j++) {
				distances[i][j] = allpos.get(i).distanceFrom(allpos.get(j));
			}
		}
		
	}

	//Greedy implementation of Sensor decision with A-star pathfinding
	
	public void playAstarSimAnneal() {
		
		
		
		 
			List<Sensor> AllPos = new ArrayList<>();
			
			AllPos.add(new Sensor("null",this.starting, 0.0,""));
			AllPos.addAll(this.unvisited);
		
			
			
			List<Sensor> permutation = new ArrayList<>();

			permutation.addAll(AllPos);
			
			Collections.shuffle(permutation);
			
			List<Sensor> best = new ArrayList<>();
			
			best.addAll(permutation);
			
			
			
			double temp = AllPos.size();
			
			double t0 = temp;
			
			int cyc = -1;
			
			while (temp > 1){
				cyc++;
				int i = (int) (AllPos.size()*App.rnd.nextDouble());
				int j = (int) (AllPos.size()*App.rnd.nextDouble());
			
				List<Sensor> swap = new ArrayList<>();
				
				swap.addAll(permutation);
				
				Collections.swap(swap, i, j);
				
				double swapval = 0.0;
				double permval = 0.0;
				
				for(int k = 0 ; k < swap.size() ; k++) {
					swapval += swap.get(k).distanceFrom(swap.get(Math.floorMod(k+1,swap.size())));
					permval += permutation.get(k).distanceFrom(permutation.get(Math.floorMod(k+1,permutation.size())));
				}
				//				
//				permval += permutation.get(i).distanceFrom(permutation.get(Math.floorMod(i-1,AllPos.size())));
//				permval += permutation.get(i).distanceFrom(permutation.get(Math.floorMod(i+1,AllPos.size())));
//				permval += permutation.get(j).distanceFrom(permutation.get(Math.floorMod(j-1,AllPos.size())));
//				permval += permutation.get(j).distanceFrom(permutation.get(Math.floorMod(j+1,AllPos.size())));
//				
//				
//				swapval += swap.get(i).distanceFrom(swap.get(Math.floorMod(i-1,AllPos.size())));
//				swapval += swap.get(i).distanceFrom(swap.get(Math.floorMod(i+1,AllPos.size())));
//				swapval += swap.get(j).distanceFrom(swap.get(Math.floorMod(j-1,AllPos.size())));
//				swapval += swap.get(j).distanceFrom(swap.get(Math.floorMod(j+1,AllPos.size())));
//				
				
				if(acceptanceProbability(permval, swapval, temp)>App.rnd.nextDouble()) {
					permutation = new ArrayList<>();
					permutation.addAll(swap);
				}
				
				permval = 0.0;
				double bestval = 0.0;
				
				for(int k = 0 ; k < permutation.size() ; k++) {
					permval += permutation.get(k).distanceFrom(permutation.get(Math.floorMod(k+1,permutation.size())));
					bestval += best.get(k).distanceFrom(best.get(Math.floorMod(k+1,best.size())));
					
				}
				
				if(permval<bestval) {
					best = new ArrayList<>();
					best.addAll(permutation);
					
				}
				
				temp = t0/(1+(0.999*cyc));
				
			
				
			}
			
		    
	        int val = 0;
	        
	        for(int i = 0 ; i < best.size(); i++) {
	        	
	        	if(best.get(i).getName()=="null") {
	        		System.out.println("Scoop");
	        		val = i;
	        		break;
	        	}
	        	
	        }
	        
	        	
			
			
			
			Collections.rotate(best, -val-1); 
			
			///Check
	        
	        List<Point> points = new ArrayList<>();
	        
	        
	        for(Sensor s : best) {
	        	System.out.println(s.toString());
	        	points.add(Point.fromLngLat(s.getLng(), s.getLat()));
	        	
	        	
	        	
	        }
	       
	        
	        LineString line = LineString.fromLngLats(points);
	        
	        App.saveToFile("lines.geojson", line.toJson(), true);
	   
	        
	        
	        while(true) {
	        
	        	Sensor nextsensor = best.get(0);
	        	best.remove(0);
	        	List<Integer> pos = this.reverse(this.Astar(this.dronePos, nextsensor.getPosition()));
	        	for(int i : pos) {
	        		if(!this.moveDrone(i)) {
	        			return;
	        		}
	        	}
	        }
	        
		 
	        
	        
	}
	
    public static double acceptanceProbability(double energy, double newEnergy, double temperature) {
        // If the new solution is better, accept it
        if (newEnergy < energy) {
            return 1.0;
        }
        // If the new solution is worse, calculate an acceptance probability
        return Math.exp((energy - newEnergy) / temperature);
    }
	

	public void playAstarGreedy() {
		
		
		
		
		while(true) {
			
			Sensor nextsensor = this.getClosestSensor();
			
		
		//	Sensor nextsensor = this.getNextSensor();
			
		
			List<Integer> pos = this.reverse(this.Astar(this.dronePos, nextsensor.getPosition()));

			
			//Check to see if the drone is already within distance of sensor 
			
			if(pos.size() == 0 && this.dronePos.distanceFrom(nextsensor.getPosition()) > 0.0001) {

				
				
				//This is the case that the drone is far enough to move towards sensor direction and read it
				
				if(!safe(this.dronePos,this.dronePos.directionTo(nextsensor.getPosition()),App.closestdis)) {
					
					//If the move is not a safe one it will give up for the time being on the closest and go for second closest
					if(this.unvisited.size() > 1) {
						pos = this.reverse(this.Astar(this.dronePos, this.getSecondClosestSensor().getPosition()));
					}
					
				}else{
					//Fly towards the sensor 
					pos.add(this.dronePos.directionTo(nextsensor.getPosition()));
				}
				
				
			} else {
				
				// This is the case that the sensor is too close to fly directly towards sensor and still read 
				// so we take the second closest sensor..
				if(pos.size() == 0) {
					
					pos = this.reverse(this.Astar(this.dronePos, this.getSecondClosestSensor().getPosition()));
					

					
					if(pos.size() == 0) {
						
						//If this is the case it will be that we have hit this edge in the last sensor therefore we try to move towards again 
						//if not go starting and try again..
						if(!safe(this.dronePos,this.dronePos.directionTo(nextsensor.getPosition()),App.closestdis)) {
							pos = this.reverse(this.Astar(this.dronePos, this.starting));
							
							if(pos.size() == 0) {
								//Must be done ! 
								return;
							}
						}else {
							pos.add(this.dronePos.directionTo(nextsensor.getPosition()));
						}
					}
					
				}
			}
			
			//moving the drone , this will break once we have no moves or finish 
			for(int d : pos) {
				
				if(!this.moveDrone(d)) {
					return;
				}
			}
			
			
			
			App.saveToFile("test.geojson", this.getMap().toJson(), true);
	
		}
		
	}

	
	//A-star algorithm for path finding between two positions..
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
		
		open.add(new Node(null,null,dronePos));
		
		while(open.size()>0) { 
			
			// Making a node that wont be considered just as a place holder in the min comparison..
			Node currentNode = new Node(null,null,dronePos);
			
			currentNode.setF(Integer.MAX_VALUE);
			
			//Getting the minimum value F node..
			
			for(Node n : open) {
				if (n.getF()<currentNode.getF()) {
					
					currentNode=n;
				}
			}
			
			//Updating our open and closed sets 
			
			open.remove(currentNode);
			
	        closed.add(currentNode);
	        
	        
	        //If @ position then we are done ! 
	        
	        if(currentNode.distanceFrom(senspos) <= 0.0002 || (this.unvisited.size()==0 && currentNode.distanceFrom(this.starting) < 0.0003)){
	          
	            List<Integer> directions = new ArrayList<Integer>();
	            
	            
	            while(currentNode != null) {
	            
	                
	            	//Add the directions to the return list, as we work back from end this will be in reverse
	            	
	                if(currentNode.getParent() != null) {
	                
	                directions.add(currentNode.getDirection());
	                
	                }
	                
	                currentNode = currentNode.getParent();
	           
	        }
	            return directions;
	       }
	        
			//testing child nodes for the current node
	        
	        HashSet<Node> children = new HashSet<Node>();
	        
	        //testing each direction..
	        
	        for(int i = 0; i < 36; i++) {
	        	
	        	
	        	Position newnode = currentNode.move(i);
	        	
	        	//Valid move ?
	        	
	        	if(!safe(currentNode,i,App.closestdis)) {
	        		
	        		continue;
	        	}
	        	
	        	Node newnodee = new Node(i,currentNode, newnode);
	        	
	        	children.add(newnodee);
	        	
	        	
	        	
	        }
	        

	        for(Node child : children) {
	        	
	        	
	        	if(closed.contains(child)) {
	        		continue;
	        	}
	        	
	        	// Setting the childs G,H,F value..
	        	
	                child.setG(currentNode.getG() + 0.0003);  
	                child.setH(heuristic(child,senspos));
	                child.setF(child.getG() + child.getH());
	                
	                //If child position is already in open list and on shorter path from start don't add to open
	                
	                Boolean breaker = false;
	                for(Node opennode : open) {
		        	
	                	if( Double.compare(child.getLng(),opennode.getLng()) == 0 && Double.compare(child.getLat(),opennode.getLat()) == 0&&  child.getG() > opennode.getG()) {
	                		breaker = true;
	                			        		
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

	
	//This is the Manhattan heuristic with a D value of 3 for faster runtime..
	private double heuristic(Node child, Position senspos) {
	
		double dy = Math.abs(child.getLng()-senspos.getLng());
		double dx = Math.abs(child.getLat()-senspos.getLat());
			
		//D * (DY DX) D can = 3 !
		return 3*(dy+dx); 
		
	}
	
	//This is the function for moving the drone..
	
	private boolean safe(Position oldPosition,int randomint, double closestdis) {
		
		// This works on a bound is not 100% accurate though better than last time ! 
		
		for(int i = 1 ; i < 10*(Math.ceil(0.0003 / closestdis))+1;i++) {
			
			if(oldPosition.move(randomint,(closestdis/10)*i).isInRestricted() || !oldPosition.move(randomint,(closestdis/10)*i).isInArea()) {
				return false;
			}
			
		}
	
		return (!oldPosition.move(randomint).isInRestricted() && oldPosition.move(randomint).isInArea());
		
	}

	public String getFlightPath() {
		
		String flightpath = "";
    	
		for(String s : this.flightpath){
    		flightpath += s + "\n";
    	}
    	
		return flightpath;
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
		
		features.add(Feature.fromGeometry(LineString.fromLngLats(this.flightPoints)));
		
		
		return FeatureCollection.fromFeatures(features);
		
	}
	
	private boolean moveDrone(int direction) {	
		
		
		//If the drone is out of moves break.. 
		if(this.moves < 1) {
			
			System.out.println("Finished : Out of moves");
			return false;
			
		}
		
		
		//If the drone has no sensors and is back at start break..
		if(this.unvisited.size() == 0 && this.dronePos.distanceFrom(this.starting) < 0.0003) {
				System.out.println("Finished : Flight success");
				return false;
		
		} else {
		
			//Moving the drone and reading the nearest sensor 
			
			Position before = new Position(this.dronePos.getLng(),this.dronePos.getLat());		
					
			this.dronePos = this.dronePos.move(direction);
			
			String sensorname = readNearest();
			
			//Adding data for drone flight path and txt output
			
			this.flightpath.add(Integer.toString(151-this.moves)+","+Double.toString(before.getLng())+","+Double.toString(before.getLat())+","+this.dronePos.getLng()+","+this.dronePos.getLat()+","+sensorname);
			
			this.flightPoints.add(Point.fromLngLat(this.dronePos.getLng(),this.dronePos.getLat()));
			
			//Drone moves therefore decrement moves
			
			this.moves--;
			
			return true;
		}
	}

	//function to "read" the nearest sensor ..
	
	private String readNearest() {
		
		//As the start is refered to as a sensor in our algorithm we must account for this in the reading..
		
		if(this.unvisited.size() == 0 && this.dronePos.distanceFrom(this.starting) < 0.0003) {
			
			return "null";
	
		}
		
		//checking we have right distance to read and marking as visited or returning "null"
		
		if (this.getClosestSensor().getPosition().distanceFrom(this.dronePos) <= 0.0002) {
			
			String sensorname = this.getClosestSensor().getName();
			
//			System.out.println("READING..");
//			
//			System.out.println("DRONE: " +this.dronePos.toString());

//			System.out.println("SENSOR: " + this.getClosestSensor().getPosition().toString());
			
			this.unvisited.remove(this.getClosestSensor());
			
			return sensorname;
		}else {
			return "null";
		}
		
	}
	
	
	
	//returns the closest sensor to drones position 
	private Sensor getClosestSensor() {
		Position currentPos = new Position(this.dronePos.getLng(), this.dronePos.getLat());		
		
		if(unvisited.size() > 0) {
			
			Sensor closestSensor = unvisited.get(0);
		for (Sensor sensor: unvisited) {
			if (currentPos.distanceFrom(sensor.getPosition()) < currentPos.distanceFrom(closestSensor.getPosition())) {
				closestSensor = sensor;
			}
		}
		return closestSensor;
		}else {
			//If no sensors left to visit return the start ! 
			return new Sensor("null",this.starting, 0.0, "");
		
		}
		
		

		
	}
	
	private Sensor getSecondClosestSensor() {
		
		
		if(this.unvisited.size() > 1) {
			Position currentPos = new Position(this.dronePos.getLng(), this.dronePos.getLat());
			Sensor closestSensor = unvisited.get(0);
			Sensor secondClosest = unvisited.get(1);
			for (Sensor sensor: unvisited) {
				if (currentPos.distanceFrom(sensor.getPosition()) < currentPos.distanceFrom(closestSensor.getPosition())) {
					secondClosest = closestSensor;
					closestSensor = sensor;
				}
			}
			

			return secondClosest;
		}else {
			
			return new Sensor("null",this.starting, 0.0,"");
		}
		
	}

	
}
