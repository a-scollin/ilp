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

	// Constructor for drone
	public Drone(Position startPos, List<Sensor> appSensors, Double appClosestDis, Random appRandom) {

		if (!startPos.isInArea()) {
			throw new Error("Start Position needs to be in confinement zone !");
		}

		this.movesLeft = 150;

		this.dronePos = startPos;

		this.startingPos = startPos;

		this.unvisitedSensors = new ArrayList<Sensor>(appSensors);

		this.allSensors = new ArrayList<Sensor>(appSensors);

		this.rndSeed = appRandom;

		this.closestDistance = appClosestDis;

		this.flightPath = new ArrayList<String>();

		this.flightPoints = new ArrayList<Point>();

		flightPoints.add(Point.fromLngLat(startPos.getLng(), startPos.getLat()));

	}

	// Greedy implementation of Sensor decision with A-star pathfinding

	public void playAstarSimAnneal() {

		distances = new Hashtable<>();

		List<Sensor> allPoints = new ArrayList<>();

		allPoints.add(new Sensor("null", startingPos, 0.0, ""));
		allPoints.addAll(unvisitedSensors);

		for (Sensor s : allPoints) {
			distances.put(s, new Hashtable<>());
			for (Sensor r : allPoints) {
				distances.get(s).put(r, s.distanceFrom(r));
			}
		}

		List<Sensor> permutation = new ArrayList<>();

		for (Sensor s : allPoints) {
			permutation.add(s);
		}

		Collections.shuffle(permutation);

		List<Sensor> bestPermutation = new ArrayList<>();

		for (Sensor s : permutation) {
			bestPermutation.add(s);
		}

		double temp = 1000000;

		while (temp > 1) {

			int i = (int) (allPoints.size() * rndSeed.nextDouble());
			int j = (int) (allPoints.size() * rndSeed.nextDouble());

			List<Sensor> swappedPerm = new ArrayList<>();

			for (Sensor s : permutation) {
				swappedPerm.add(s);
			}
			Collections.swap(swappedPerm, i, j);

			double swappedTourValue = 0.0;
			double permutationTourValue = 0.0;

			for (int k = 0; k < swappedPerm.size(); k++) {
				swappedTourValue += distances.get(swappedPerm.get(k))
						.get(swappedPerm.get(Math.floorMod(k + 1, allPoints.size())));
				permutationTourValue += distances.get(permutation.get(k))
						.get(permutation.get(Math.floorMod(k + 1, allPoints.size())));
			}

			if (acceptanceProbability(permutationTourValue, swappedTourValue, temp) > rndSeed.nextDouble()) {
				permutation = new ArrayList<>();
				for (Sensor s : swappedPerm) {
					permutation.add(s);
				}
			}

			permutationTourValue = 0.0;
			double bestPermutationTourValue = 0.0;

			for (int k = 0; k < swappedPerm.size(); k++) {
				bestPermutationTourValue += distances.get(bestPermutation.get(k))
						.get(bestPermutation.get(Math.floorMod(k + 1, allPoints.size())));
				permutationTourValue += distances.get(permutation.get(k))
						.get(permutation.get(Math.floorMod(k + 1, allPoints.size())));
			}

			if (permutationTourValue < bestPermutationTourValue) {
				bestPermutation = new ArrayList<>();
				for (Sensor s : permutation) {
					bestPermutation.add(s);
				}

			}

			temp *= 0.999999;

		}

		int startingIndex = 1;

		for (int i = 0; i < bestPermutation.size(); i++) {

			if (bestPermutation.get(i).getName() == "null") {
				startingIndex = i;
				break;
			}

		}

		Collections.rotate(bestPermutation, -startingIndex - 1);

		while (true) {

			Sensor nextSensor = new Sensor("null", startingPos, 0.0, "");
			if (bestPermutation.size() > 0) {

				nextSensor = bestPermutation.get(0);
				bestPermutation.remove(0);
			}

			List<Integer> directionPath = Astar(dronePos, nextSensor.getPosition());

			if (unvisitedSensors.size() == 0 && dronePos.distanceFrom(startingPos) <= 0.0003) {
				failed = false;
				return;
			}

			for (int i : directionPath) {
				if (!moveDrone(i)) {
					return;
				}
			}

			List<Integer> extraMoves = new ArrayList<>();

			for (Sensor s : unvisitedSensors) {
				if (s.equals(nextSensor)) {

					if (safe(dronePos, dronePos.directionTo(s), closestDistance)) {
						extraMoves.add(dronePos.directionTo(s));
					} else {

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

							extraMoves.add(dronePos.directionTo(startingPos));
							bestPermutation.add(nextSensor);
						}
					}
				}
			}

			for (int i : extraMoves) {
				if (!moveDrone(i)) {
					return;
				}

			}

		}

	}

	private static double acceptanceProbability(double energy, double newEnergy, double temperature) {
		// If the new solution is better, accept it
		if (newEnergy < energy) {
			return 1.0;
		}

		return Math.exp((energy - newEnergy) / Math.log10(temperature));
	}

	public void playAstarGreedy() {

		while (true) {

			Sensor nextsensor = getClosestSensor();

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

				// This is the case that the sensor is too close to fly directly towards sensor
				// and still read
				// so we take the second closest sensor..
				if (directionPath.size() == 0) {

					directionPath = this.reverse(Astar(dronePos, getSecondClosestSensor().getPosition()));

					if (directionPath.size() == 0) {

						// If this is the case it will be that we have hit this edge in the last sensor
						// therefore we try to move towards again
						// if not go starting and try again..
						if (!safe(dronePos, dronePos.directionTo(nextsensor.getPosition()), closestDistance)) {
							directionPath = Astar(dronePos, startingPos);

							if (directionPath.size() == 0 && dronePos.distanceFrom(startingPos) <= 0.0003) {
								// Must be done !
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

	// A-star algorithm for path finding between two directionPathitions..
	private static List<Integer> reverse(List<Integer> inputList) {

		List<Integer> reversedList = new ArrayList<Integer>();

		for (int i = inputList.size() - 1; i >= 0; i--) {
			reversedList.add(inputList.get(i));
		}

		return reversedList;

	}

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

			// If @ directionPathition then we are done !

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

				// Valid move ?

				if (!safe(currentNode, i, closestDistance)) {

					continue;
				}

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

				// If child directionPathition is already in open list and on shorter path from
				// start don't add to open

				Boolean breaker = false;
				for (Node opennode : open) {

					if (Double.compare(child.getLng(), opennode.getLng()) == 0
							&& Double.compare(child.getLat(), opennode.getLat()) == 0
							&& child.getG() > opennode.getG()) {
						breaker = true;

						break;
					}
				}

				if (breaker) {
					continue;
				}

				open.add(child);

			}

		}

		return null;

	}

	// This is the Manhattan heuristic with a D value of 3 for faster runtime..
	private static double heuristic(Node child, Position targetPosition) {

		double dy = Math.abs(child.getLng() - targetPosition.getLng());
		double dx = Math.abs(child.getLat() - targetPosition.getLat());

		// D * (DY DX) D can = 3 !
		return 3 * (dy + dx);

	}

	// This is the function for moving the drone..

	private boolean safe(Position oldPosition, int direction, double closestdis) {

		// This works on a bound is not 100% accurate though better than last time !

		for (int i = 1; i < 10 * (Math.ceil(0.0003 / closestdis)) + 1; i++) {

			if (oldPosition.move(direction, (closestdis / 10) * i).isInRestricted()
					|| !oldPosition.move(direction, (closestdis / 10) * i).isInArea()) {
				return false;
			}

		}

		return (!oldPosition.move(direction).isInRestricted() && oldPosition.move(direction).isInArea());

	}

	public List<String> getFlightPath() {
		return flightPath;

	}

	private boolean moveDrone(int direction) {

		// If the drone is out of moves break..
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

			// Drone moves therefore decrement moves

			movesLeft--;

			return true;
		}
	}

	// function to "read" the nearest sensor ..

	private String readNearest() {

		// As the start is refered to as a sensor in our algorithm we must account for
		// this in the reading..

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

	// returns the closest sensor to drones directionPathition
	private Sensor getClosestSensor() {

		Position currentPosition = new Position(dronePos.getLng(), dronePos.getLat());

		if (unvisitedSensors.size() > 0) {

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

	private Sensor getSecondClosestSensor() {

		if (unvisitedSensors.size() > 1) {
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

	public boolean hasFailed() {

		return failed;
	}

	public int getMoves() {

		return movesLeft;
	}

	public List<Sensor> getUnvisited() {

		return unvisitedSensors;
	}

	public List<Point> getFlightPoints() {

		return flightPoints;
	}

	public List<Sensor> getAllSensors() {
		return allSensors;
	}

}
