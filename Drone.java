package uk.ac.ed.inf.aqmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.mapbox.geojson.Point;
import java.util.Hashtable;

public class Drone {

	private int movesLeft;
	private Position dronePos;
	private Position startingPos;
	private List<Sensor> unvisitedSensors;
	private Random rndSeed;
	private double closestDistance;
	private List<String> flightPath;
	private List<Point> flightPoints;

	private Hashtable<Sensor, Hashtable<Sensor, Double>> distances;
	private boolean failed;
	private List<Sensor> allSensors;

	// Constructor for drone class
	public Drone(Position startPos, List<Sensor> appSensors, Double appClosestDis, Random appRandom) {


		// Check if the starting position is not legal 
		 
		if (!startPos.isInArea()) {
			throw new Error("Start Position needs to be in confinement zone !");
		}

		// Setting the drones attributes

		this.movesLeft = 150;

		this.dronePos = startPos;

		this.startingPos = startPos;

		this.unvisitedSensors = new ArrayList<Sensor>(appSensors);

		this.allSensors = new ArrayList<Sensor>(appSensors);

		this.rndSeed = appRandom;

		this.closestDistance = appClosestDis;

		this.flightPath = new ArrayList<String>();

		this.flightPoints = new ArrayList<Point>();

		// Adding the starting point as a point in the flight path

		flightPoints.add(Point.fromLngLat(startPos.getLng(), startPos.getLat()));

	}

	// This function carries out the moves for Simulated Annealing implementation of Sensor order decision with A-star pathfinding

	public void playAstarSimAnneal() {

		
		
		// Creates a tour of the sensors including the starting position as a sensor..
		
		distances = new Hashtable<>();

		List<Sensor> allPoints = new ArrayList<>();

		allPoints.add(new Sensor("null", startingPos, 0.0, ""));
		allPoints.addAll(unvisitedSensors);

		// Fills a Hashtable with the distances from each sensor to other sensors for comparison in the main loop

		for (Sensor s : allPoints) {
			distances.put(s, new Hashtable<>());
			for (Sensor r : allPoints) {
				distances.get(s).put(r, s.distanceFrom(r));
			}
		}

		// Creating the original random permutation of sensors 

		List<Sensor> permutation = new ArrayList<>();

		for (Sensor s : allPoints) {
			permutation.add(s);
		}

		Collections.shuffle(permutation);

		// Creating the bestPermutation array

		List<Sensor> bestPermutation = new ArrayList<>();

		for (Sensor s : permutation) {
			bestPermutation.add(s);
		}

		// Setting the initial temperature

		double temp = 1000000;


		// Loop to decrement the temperature and carry out permutation swaps..

		while (temp > 1) {
				
				
			// Choosing two random index's to swap in the permutation
			
			int i = (int) (allPoints.size() * rndSeed.nextDouble());
			int j = (int) (allPoints.size() * rndSeed.nextDouble());


			// Creating a duplicate to the current permutation
			
			List<Sensor> swappedPerm = new ArrayList<>();

			for (Sensor s : permutation) {
				swappedPerm.add(s);
			}
			
			
			// Swapping the random elements of the permutation copy
			
			Collections.swap(swappedPerm, i, j);
			
			// Calculating the tour value of the current permutation and swapped permutation using our pre-calculated distances

			double swappedTourValue = 0.0;
			double permutationTourValue = 0.0;

			for (int k = 0; k < swappedPerm.size(); k++) {
				swappedTourValue += distances.get(swappedPerm.get(k))
						.get(swappedPerm.get(Math.floorMod(k + 1, allPoints.size())));
				permutationTourValue += distances.get(permutation.get(k))
						.get(permutation.get(Math.floorMod(k + 1, allPoints.size())));
			}
			
			// Check to see wether the swapped permutation is accepted if so the current perm = swapped perm

			if (acceptanceProbability(permutationTourValue, swappedTourValue, temp) > rndSeed.nextDouble()) {
				permutation = new ArrayList<>();
				for (Sensor s : swappedPerm) {
					permutation.add(s);
				}
			}


			// Calculating the current permutation tour value and the best permutation tour value
			
			permutationTourValue = 0.0;
			double bestPermutationTourValue = 0.0;

			for (int k = 0; k < swappedPerm.size(); k++) {
				bestPermutationTourValue += distances.get(bestPermutation.get(k))
						.get(bestPermutation.get(Math.floorMod(k + 1, allPoints.size())));
				permutationTourValue += distances.get(permutation.get(k))
						.get(permutation.get(Math.floorMod(k + 1, allPoints.size())));
			}

			// If the current permutation is better than the best permutation then update the best permutation to be the current
			
			if (permutationTourValue < bestPermutationTourValue) {
				bestPermutation = new ArrayList<>();
				for (Sensor s : permutation) {
					bestPermutation.add(s);
				}

			}
				
				
			// Decrement the temperature ..
			temp *= 0.999999;

		}


		// This loop finds the index at which the placeholder starting position sensor is in the best permutation.. 
		
		int startingIndex = 1;

		for (int i = 0; i < bestPermutation.size(); i++) {

			if (bestPermutation.get(i).getName() == "null") {
				startingIndex = i;
				break;
			}

		}

		// Rotating the best permutation so that the starting position is last to be visited.. 

		Collections.rotate(bestPermutation, -startingIndex - 1);

		// Loop to fly the drone towards each sensor in the best permutation

		while (true) {


			// Choosing the next sensor to be flown towards
			
			Sensor nextSensor = new Sensor("null", startingPos, 0.0, "");
			if (bestPermutation.size() > 0) {

				nextSensor = bestPermutation.get(0);
				bestPermutation.remove(0);
			}


			// A* pathfind towards the next sensor in the best permutation
			
			List<Integer> directionPath = Astar(dronePos, nextSensor.getPosition());


			// Check to see if the drone is finished

			if (unvisitedSensors.size() == 0 && dronePos.distanceFrom(startingPos) <= 0.0003) {
				failed = false;
				return;
			}
			
			// Move the drone each direction outlined in the A* directions

			for (int i : directionPath) {
				if (!moveDrone(i)) {
					return;
				}
			}

			// Extra moves required if the A* algorithm hits an edge case..
			
			List<Integer> extraMoves = new ArrayList<>();

			for (Sensor s : unvisitedSensors) {
			
				// This is the case that the A* directions did not actually lead the drone to read the required sensor 	
					
				if (s.equals(nextSensor)) {
						
						
					// Check if trivial move is required to read the required sensor 
						
					if (safe(dronePos, dronePos.directionTo(s), closestDistance)) {
						extraMoves.add(dronePos.directionTo(s));
					} else {
					
						/** 
							If trivial move is not possilbe push the sensor into the 2nd position 
							of the best permutation this means the next sensor in the permutation will be visited 
							then it will try the required sensor again 
						**/

							if (bestPermutation.size() > 1) {
							ArrayList<Sensor> newBest = new ArrayList<>();
							for (int i = 0; i < bestPermutation.size(); i++) {
								if (i == 1) {
									newBest.add(nextSensor);
								}
								newBest.add(bestPermutation.get(i));
							}

							bestPermutation = newBest;

						} else {
						
							/** 
								If the permutation size is not > 1 then this is the case that this is the second last sensor so 								we try fly back to start and try sensor again..
							**/
							
							extraMoves.add(dronePos.directionTo(startingPos));
							bestPermutation.add(nextSensor);
						}
					}
				}
			}

			// Do the moves outlined in extra..
			for (int i : extraMoves) {
				if (!moveDrone(i)) {
					return;
				}

			}

		}

	}


	// This function is the acceptance probability used in the simulated annealing algorithm 
	private static double acceptanceProbability(double energy, double newEnergy, double temperature) {
	
		// If the new solution is better, accept it
		if (newEnergy < energy) {
			return 1.0;
		}
		
		// Else return the difference in tour values over the temperature in logarithmic space 

		return Math.exp((energy - newEnergy) / Math.log10(temperature));
	}

	
	
	// This function carries out the moves for Greedy implementation of Sensor order decision with A-star pathfinding
	
	public void playAstarGreedy() {

		while (true) {

			// The next sensor to be visited is retrived.. 

			Sensor nextsensor = getClosestSensor();

			// We attempt to Astar towards this sensor

			List<Integer> directionPath = Astar(dronePos, nextsensor.getPosition());

			// Check to see if the drone is already within distance of sensor

			if (directionPath.size() == 0 && dronePos.distanceFrom(nextsensor.getPosition()) > 0.0001) {

				// This is the case that the drone is far enough to move towards sensor
				// direction and read it

				if (!safe(dronePos, dronePos.directionTo(nextsensor.getPosition()), closestDistance)) {

					// If the move is not a safe one it will give up for the time being on the
					// closest and go for second closest
					if (unvisitedSensors.size() > 1) {
						directionPath = Astar(dronePos, getSecondClosestSensor().getPosition());
					}

				} else {
					// Fly towards the sensor
					directionPath.add(dronePos.directionTo(nextsensor.getPosition()));
				}

			} else {

				/**
				 	This is the case that the sensor is too close to fly directly towards sensor
					and still read
					so we take the second closest sensor..
				**/
				
				if (directionPath.size() == 0) {

					directionPath = this.reverse(Astar(dronePos, getSecondClosestSensor().getPosition()));

					if (directionPath.size() == 0) {

						/** 
							If this is the case it will be that we have hit this edge in the last sensor
							therefore we try to move towards again
						 	if not go starting and try again.. 
						**/
						if (!safe(dronePos, dronePos.directionTo(nextsensor.getPosition()), closestDistance)) {
							directionPath = Astar(dronePos, startingPos);

							if (directionPath.size() == 0 && dronePos.distanceFrom(startingPos) <= 0.0003) {
								
								// Drone must be finished its tour ! 
								
								failed = false;
								
								return;
							}
						
						} else {
			
							directionPath.add(dronePos.directionTo(nextsensor.getPosition()));
						
						}
					}

				}
			}

			// moving the drone , this will break once we have no moves or finish
			for (int d : directionPath) {

				if (!moveDrone(d)) {
					return;
				}
			}

		}

	}

	// Function for reversing the order of a list of integers..

	private static List<Integer> reverse(List<Integer> inputList) {

		List<Integer> reversedList = new ArrayList<Integer>();

		for (int i = inputList.size() - 1; i >= 0; i--) {
			reversedList.add(inputList.get(i));
		}

		return reversedList;

	}
	
	// A-star algorithm for path finding the directions for the drone to take between two positions ..

	private List<Integer> Astar(Position startingPosition, Position targetPosition) {

		HashSet<Node> open = new HashSet<Node>();

		HashSet<Node> closed = new HashSet<Node>();

		open.add(new Node(null, null, startingPosition));

		while (open.size() > 0) {

			// Making a node that wont be considered just as a place holder in the min
			// comparison..
			Node currentNode = new Node(null, null, startingPosition);

			currentNode.setF(Integer.MAX_VALUE);

			// Getting the minimum value F node..

			for (Node n : open) {
				if (n.getF() < currentNode.getF()) {

					currentNode = n;
				}
			}

			// Updating our open and closed sets

			open.remove(currentNode);

			closed.add(currentNode);

			// If @ targetPosition then we are done !

			if (currentNode.distanceFrom(targetPosition) <= 0.0002
					|| (unvisitedSensors.size() == 0 && currentNode.distanceFrom(startingPos) < 0.0003)) {

				List<Integer> directions = new ArrayList<Integer>();

				while (currentNode != null) {

					// Add the directions to the return list, as we work back from end this will be
					// in reverse

					if (currentNode.getParent() != null) {

						directions.add(currentNode.getDirection());

					}

					currentNode = currentNode.getParent();

				}
				return reverse(directions);
			}

			// testing child nodes for the current node

			HashSet<Node> children = new HashSet<Node>();

			// testing each direction..

			for (int i = 0; i < 36; i++) {

				Position newnode = currentNode.move(i);

				// We check this is a valid move ..

				if (!safe(currentNode, i, closestDistance)) {

					continue;
				}
				
				// We add the canidate to the children of the current Node

				Node newnodee = new Node(i, currentNode, newnode);

				children.add(newnodee);

			}

			for (Node child : children) {

				if (closed.contains(child)) {
					continue;
				}

				// Setting the childs G,H,F value..

				child.setG(currentNode.getG() + 0.0003);
				child.setH(heuristic(child, targetPosition));
				child.setF(child.getG() + child.getH());

				// If child is already in open list and on shorter path from current don't add to open

				Boolean breaker = false;
				for (Node opennode : open) {

					//Comparing if the childs position and open node position are equal.. Double.compare is for accuracy..
					
					if (Double.compare(child.getLng(), opennode.getLng()) == 0
							&& Double.compare(child.getLat(), opennode.getLat()) == 0
							&& child.getG() > opennode.getG()) {
							
						// We dont add.. 
						
						breaker = true;

						break;
					}
				}
				
				// Dont add check .. 
				
				if (breaker) {
					continue;
				}


				// We add the child to the candiates in open list..		
				
				open.add(child);

			}

		}

		return null;

	}

	// This is the Manhattan heuristic for use in the Nodes of our A* with a D value of 3 for faster runtime..
	private static double heuristic(Node child, Position targetPosition) {

		double dy = Math.abs(child.getLng() - targetPosition.getLng());
		double dx = Math.abs(child.getLat() - targetPosition.getLat());

		// D * (DY DX) D can = 3 !
		return 3 * (dy + dx);

	}

	// This is the function for determining if a move is legal given the direction and the position we are moving from.. 

	private boolean safe(Position oldPosition, int direction, double closestdis) {

		/**
			We check the positions along the line drawn from the oldPosition to the place we want to move,
			in each case we will check 10 times the position segments depending on the closest distance of all the fly zones
			this allows a variable amount of percision to be taken in checking each legal move and reduces the bound for error
		**/

		for (int i = 1; i < 10 * (Math.ceil(0.0003 / closestdis)) + 1; i++) {

			// We check if this move segment violates our nofly zones or confinement zone..		
	
			if (oldPosition.move(direction, (closestdis / 10) * i).isInRestricted()
					|| !oldPosition.move(direction, (closestdis / 10) * i).isInArea()) {
				return false;
			}

		}
		
		// Check if the move itself is valid .. 
		
		return (!oldPosition.move(direction).isInRestricted() && oldPosition.move(direction).isInArea());

	}


	// Function for returning the flightpath of the drone for saving..
	
	public List<String> getFlightPath() {
		return flightPath;

	}
	
	
	// This is the function for actually moving the drone, we assume in our checks before that the move is safe ! 
	
	private boolean moveDrone(int direction) {

		// If the drone is out of moves the drone has failed and we terminate our process..
		if (movesLeft < 1) {
			failed = true;
			System.out.println("Finished : Out of moves");
			return false;

		}

		// If the drone has no sensors and is back at start break..
		if (unvisitedSensors.size() == 0 && dronePos.distanceFrom(startingPos) < 0.0003) {
			failed = false;
			System.out.println("Finished : Flight success");
			return false;

		} else {

			// Moving the drone and reading the nearest sensor

			Position positionBefore = new Position(dronePos.getLng(), dronePos.getLat());

			dronePos = dronePos.move(direction);

			String sensorName = readNearest();

			// Adding data for drone flight path and txt output

			flightPath.add(Integer.toString(151 - movesLeft) + "," + Double.toString(positionBefore.getLng()) + ","
					+ Double.toString(positionBefore.getLat()) + "," + dronePos.getLng() + "," + dronePos.getLat() + ","
					+ sensorName);

			flightPoints.add(Point.fromLngLat(dronePos.getLng(), dronePos.getLat()));

			// Drone moves therefore we decrement the moves of the drone..

			movesLeft--;

			return true;
		}
	}

	// function to "read" the nearest sensor ..

	private String readNearest() {

		// As the start is refered to as a sensor in our algorithm we must account for
		// this when we read a sensor..

		if (unvisitedSensors.size() == 0 && dronePos.distanceFrom(startingPos) < 0.0003) {

			return "null";

		}

		// checking we have right distance to read and marking as visited or returning
		// "null"

		if (getClosestSensor().getPosition().distanceFrom(dronePos) <= 0.0002) {

			String sensorName = getClosestSensor().getName();

			System.out.println("READING..");

			System.out.println("DRONE: " + dronePos.toString());

			System.out.println("SENSOR: " + getClosestSensor().getPosition().toString());

			unvisitedSensors.remove(getClosestSensor());

			return sensorName;
		} else {
			return "null";
		}

	}

	// returns the closest sensor to drones position.. 
	private Sensor getClosestSensor() {

		Position currentPosition = new Position(dronePos.getLng(), dronePos.getLat());


		// If there are no sensors to be visited we return the starting position to return to ..
		
		if (unvisitedSensors.size() > 0) {

			// Loop to find the sensor of smallest distance.. 

			Sensor closestSensor = unvisitedSensors.get(0);

			for (Sensor sensor : unvisitedSensors) {

				if (currentPosition.distanceFrom(sensor.getPosition()) < currentPosition
						.distanceFrom(closestSensor.getPosition())) {

					closestSensor = sensor;

				}
			}
			return closestSensor;
		} else {
		
			// If no sensors left to visit return the start !
		
			return new Sensor("null", startingPos, 0.0, "");

		}

	}
	
	// This is a method for getting the second closest sensor to the drones position, This is used in the Greedy implementation

	private Sensor getSecondClosestSensor() {

		/**
		 If there are <1 sensors to be visited we return the starting position to return to 
		 as we will always come back to the closest sensor in the greedy algorithm..
		**/

		if (unvisitedSensors.size() > 1) {
	
			// Finding the sensor of second smallest distance to drone position..	
		
			Position currentPosition = new Position(dronePos.getLng(), dronePos.getLat());

			Sensor closestSensor = unvisitedSensors.get(0);

			Sensor secondClosestSensor = unvisitedSensors.get(1);

			for (Sensor sensor : unvisitedSensors) {

				if (currentPosition.distanceFrom(sensor.getPosition()) < currentPosition
						.distanceFrom(closestSensor.getPosition())) {
					secondClosestSensor = closestSensor;
					
					closestSensor = sensor;
				
				}
			}

			return secondClosestSensor;
		
		} else {

			return new Sensor("null", startingPos, 0.0, "");
		}

	}


	// Function for returning the drones failed boolean ..
	
	public boolean hasFailed() {

		return failed;
	}

	// Function for returning the drones moves variable ..
	
	public int getMoves() {

		return movesLeft;
	}

	// Function for getting the unvisited sensors for the drone ..

	public List<Sensor> getUnvisited() {

		return unvisitedSensors;
	}

	// Function for getting the flight points from the drone for use in the generation of the GeoJSON map

	public List<Point> getFlightPoints() {

		return flightPoints;
	}

	// Function for getting all the original sensors from the drone for use in the generation of the GeoJSON map

	public List<Sensor> getAllSensors() {
		return allSensors;
	}

}
