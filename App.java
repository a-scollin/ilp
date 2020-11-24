package uk.ac.ed.inf.aqmap;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.*;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


//Importing the mapbox and JSON
import com.mapbox.geojson.*;


public class App {

	protected static java.util.Random rnd;
	protected static List<Sensor> sensors;
	protected static MultiPolygon restrictedareas;
	protected static MultiPolygon CONFINEMENT_AREA;
	protected static double closestdis;

	public static void main(String[] args) {

		// Parsing the CL arguments
		String port = args[6];

		String[] startingCoords = { args[3], args[4] };

		int randomiser = Integer.parseInt(args[5]);

		rnd = new Random(randomiser);


		// Setting the restricted areas and closest distance variables..
		restrictedareas = getRestrictedAreas(port);


		// Setting the confinement area that is hardcoded..
		CONFINEMENT_AREA = setConfinement();

		// Saving a copy of the nofly zones to a file for easy comparisons..
		saveToFile("nofly.geojson", App.restrictedareas.toJson(), true);

		// Testing the drone on the map speciified in the CL arguments
		testDrone(startingCoords, "maps/" + args[2] + "/" + args[1] + "/" + args[0], port, false);


	}

	// This function is for generating a GeoJSON map of a drones flight path

	private static FeatureCollection generateMap(Drone testDrone) {


		// Getting the unvisited sensors and flight points from the drone 
		
		List<Feature> features = new ArrayList<Feature>();

		List<Sensor> unvisited = testDrone.getUnvisited();

		List<Point> flightPoints = testDrone.getFlightPoints();


		// Looping through all sensors that the drone should have visited
		
		for (Sensor s : testDrone.getAllSensors()) {

			// If the unvisited doesnt contain the sensor then it must've been visited and is added to feature collection as such.. 

			if (!unvisited.contains(s)) {
			
				// Sensor is visited 

				Feature visitedsensor = Feature
						.fromGeometry(Point.fromLngLat(s.getPosition().getLng(), s.getPosition().getLat()));

				visitedsensor.addStringProperty("marker-size", "medium");
				visitedsensor.addStringProperty("location", s.getName());

				// Condition to check wether the sensor will give an accurate reading based on its battery percentage..

				if (s.getBattery() < 10) {

					visitedsensor.addStringProperty("marker-symbol", "lighthouse");
					visitedsensor.addStringProperty("marker-color", "#000000");
					visitedsensor.addStringProperty("rgb-string", "#000000");

				} else {
				
					// Sensor reading levels are added to the feature..
					
					Double reading = Double.parseDouble(s.getReading());
					visitedsensor.addStringProperty("marker-color", getRGB(reading));
					visitedsensor.addStringProperty("rgb-string", getRGB(reading));
					if (reading > 128) {
						visitedsensor.addStringProperty("marker-symbol", "danger");
					} else {
						visitedsensor.addStringProperty("marker-symbol", "lighthouse");
					}

				}

				features.add(visitedsensor);

			} else {

				// Sensor is not visited

				Feature unvisitedsensor = Feature
						.fromGeometry(Point.fromLngLat(s.getPosition().getLng(), s.getPosition().getLat()));

				unvisitedsensor.addStringProperty("marker-size", "medium");
				unvisitedsensor.addStringProperty("location", s.getName());
				unvisitedsensor.addStringProperty("marker-color", "#aaaaaa");
				unvisitedsensor.addStringProperty("rgb-string", "#aaaaaa");

				features.add(unvisitedsensor);
			}
		}
		
		// Adding the flight points as a linestring to show the drones flight around each sensor..

		features.add(Feature.fromGeometry(LineString.fromLngLats(flightPoints)));

		return FeatureCollection.fromFeatures(features);

	}
	
	// This function is for testing a drone path given a path to a map. Makes it easy for testing singular map instances..

	private static void testDrone(String[] startingCoords, String path, String port, boolean test) {

		// Sets the apps sensors for the given map path 

		sensors = getSensors(port, path);


		// Initiating the drone to be tested

		Drone testDrone = new Drone(
				new Position(Double.parseDouble(startingCoords[1]), Double.parseDouble(startingCoords[0])), App.sensors,
				App.closestdis, App.rnd);

		System.out.println("Map path : " + path);


		// Attempting greedy A* path 

		testDrone.playAstarGreedy();

		System.out.println(testDrone.getMoves() + " Moves left. GREEDY");
		
		// Conditional testing if the drone failed on greedy if so attempt simulated annealing
		
		if (testDrone.hasFailed()) {
			System.out.println("DRONE FAILED GREEDY, ATTEMPTING SIMULATED ANNEALING (MAY TAKE A WHILE)");
			
			
			// Setting up new drone with same parameters
			
			testDrone = new Drone(
					new Position(Double.parseDouble(startingCoords[1]), Double.parseDouble(startingCoords[0])),
					App.sensors, App.closestdis, App.rnd);
					
			// Attempting simualted annealing A* path
			
			testDrone.playAstarSimAnneal();
			System.out.println(testDrone.getMoves() + " Moves left. SA ");

		}

		// Conditional seeing wether we are running a test, this makes it easier to just change the main function if you are testing a particular instance 

		if (!test) {

			String[] Splitpath = path.split("/");

			saveToFile("flightpath-" + Splitpath[3] + "-" + Splitpath[2] + "-" + Splitpath[1] + ".txt",
					formatFlightPath(testDrone), false);

			saveToFile("readings-" + Splitpath[3] + "-" + Splitpath[2] + "-" + Splitpath[1] + ".geojson",
					generateMap(testDrone).toJson(), true);
		} else {

			saveToFile("testfp.txt", formatFlightPath(testDrone), false);
			saveToFile("testr.geojson", generateMap(testDrone).toJson(), true);
		}

	}


	// Function for formatting the drones flightpath steps, makes drones flightpath ready to be saved to a file..
	
	private static String formatFlightPath(Drone testDrone) {

		String flightpath = "";

		for (String s : testDrone.getFlightPath()) {
			flightpath += s + "\n";
		}

		return flightpath;

	}


	// Function for setting the hardcoded drone confinement area which is a big rectangle over the four points given to us
	
	private static MultiPolygon setConfinement() {

		Point ForrestHill = Point.fromLngLat(-3.192473, 55.946233);
		Point KFC = Point.fromLngLat(-3.184319, 55.946233);
		Point Meadows = Point.fromLngLat(-3.192473, 55.942617);
		Point BusStop = Point.fromLngLat(-3.184319, 55.942617);

		ArrayList<Point> confinementzone = new ArrayList<Point>();

		confinementzone.add(ForrestHill);

		confinementzone.add(KFC);

		confinementzone.add(BusStop);

		confinementzone.add(Meadows);

		List<List<Point>> confinementlist = new ArrayList<List<Point>>();

		confinementlist.add(confinementzone);

		Geometry confinement = Polygon.fromLngLats(confinementlist);

		Polygon feat = Polygon.fromJson(confinement.toJson());

		return MultiPolygon.fromPolygon(feat);

	}


	// Function for retriving the no fly zones specified on the webserver

	private static MultiPolygon getRestrictedAreas(String port) {


		// Attempting a get request to the no fly zones in the webserver
		
		String content = "";
		try {
			content = makeGetRequest(port, "buildings/no-fly-zones.geojson");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Could not access : buildings/no-fly-zones.geojson");
		}

		JSONObject Json = new JSONObject(content);

		JSONArray jsonarr = Json.getJSONArray("features");

		List<Polygon> Polygons = new ArrayList<Polygon>();

		List<Position> Poslist = new ArrayList<Position>();


		// For each specified nofly polygon - add to the multipolygon 

		for (int i = 0; i < jsonarr.length(); i++) {

			JSONObject jsonpoint = new JSONObject(jsonarr.get(i).toString());

			Polygon restricted = Polygon.fromJson(jsonpoint.get("geometry").toString());

			// Assuming that the polygons are simple, ie No 'holes', This is for the closest distance calculation
			for (Point l : restricted.coordinates().get(0)) {
				Poslist.add(new Position(l.longitude(), l.latitude()));
			}

			Polygons.add(restricted);

		}


		// setting the closest distance of all the restricted areas.. 
		
		closestdis = getClosestDistanceFromArray(Poslist);

		return MultiPolygon.fromPolygons(Polygons);

	}


	// Function for returning the smallest distance between two positions in an array.. only used for closest distance calculation 	
		
	private static double getClosestDistanceFromArray(List<Position> positionList) {

		double closestDistance = Double.MAX_VALUE;

		for (int i = 0; i < positionList.size(); i++) {

			double distance = positionList.get(i).distanceFrom(positionList.get(Math.floorMod(i + 1,positionList.size())));

			if (distance < closestDistance) {
				closestDistance = distance;
			}

		}

		return closestDistance;

	}
	
	// Function for getting the data for all the sensors from a specified map path

	private static List<Sensor> getSensors(String port, String path) {

		String content = "";

		//Attempting to make a get request to the webserver
		
		try {
			content = makeGetRequest(port, path + "/air-quality-data.json");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Could not access : " + path + "/air-quality-data.json");
		}


		// Parsing in the airquality JSON from the webserver

		JSONObject[] aqJsonData = parseinAirQualityJSON(content);


		// From specification there are always 33 sensors

		JSONObject[] wordsJsonData = new JSONObject[33];

		List<Sensor> sensors = new ArrayList<Sensor>();
		
		
		// For each of the 33 sensors instanciate a Sensor object with the required data..
		
		for (int i = 0; i < 33; i++) {

			// Attempt a get request to the sensors W3W details on the webserver 

			try {
				wordsJsonData[i] = new JSONObject(makeGetRequest(port,
						"words/" + aqJsonData[i].getString("location").replace(".", "/") + "/details.json"));
			} catch (JSONException e) {
				e.printStackTrace();
				throw new Error("Could not parse malformed JSON located : words/" + aqJsonData[i].getString("location").replace(".", "/") + "/details.json");

			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Could not access : words/" + aqJsonData[i].getString("location").replace(".", "/") + "/details.json");
			}

			// Get the coordinates from the words json that was retrived..

			JSONObject coords = new JSONObject(wordsJsonData[i].get("coordinates").toString());

			// Get the reading from the maps json that was retrived..

			String reading = aqJsonData[i].get("reading").toString();

			double sensorData[] = { coords.getDouble("lng"), coords.getDouble("lat"),
					aqJsonData[i].getDouble("battery") };

			// Use this data to construct a Sensor object.. 
			
			sensors.add(new Sensor(wordsJsonData[i].get("words").toString(), new Position(sensorData[0], sensorData[1]),
					sensorData[2], reading));

		}
		
		return sensors;
		
		
	}
	
	// This function is used for parsing in the air-quality-json files found on the webserver 

	private static JSONObject[] parseinAirQualityJSON(String content) {

		// Splits each json object representing a sensor at the closing curly bracket  

		String[] jsonStrings = content.substring(1, content.length() - 1).split("},");

		JSONObject[] aqJsonArray = new JSONObject[33];

		// Loops over all the sensors and formats them to be added as JSONObjects 

		for (int i = 0; i < 32; i++) {

			aqJsonArray[i] = new JSONObject(jsonStrings[i] + "}");
		}
		
		// Last sensor needs no extra formatting

		aqJsonArray[32] = new JSONObject(jsonStrings[32]);

		return aqJsonArray;

	}

	// This function gets the RGB code for the specified sensor reading..

	private static String getRGB(double x) {

		if (0 <= x && x < 256) { // making sure in range 1..11
			if (0 <= x && x < 32) {
				return "#00ff00";
			} else if (32 <= x && x < 64) {
				return "#40ff00";
			} else if (64 <= x && x < 96) {
				return "#80ff00";
			} else if (96 <= x && x < 128) {
				return "#c0ff00";
			} else if (128 <= x && x < 160) {
				return "#ffc000";
			} else if (160 <= x && x < 192) {
				return "#ff8000";
			} else if (192 <= x && x < 224) {
				return "#ff4000";
			} else if (224 <= x && x < 256) {
				return "#ff0000";
			}
		}

		return "";

	}
	
	//Used to return the strings of each map in your server for testing purposes..

	private static List<String> getAllMapPaths(String port) {

		String rootpath = "maps/";

		String content = "";

		// Attempting a get request to the maps/ path this returns a HTML document that we parse 
		
		try {
			content = makeGetRequest(port, rootpath);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Could not make get request to : " + rootpath);
		}

		// Parsing the HTML returned by webserver to get all paths representing the years in the maps/ folder 

		String[] years = getPathsFromHTML(rootpath, content);

		List<String> mapPaths = new ArrayList<String>();

		for (int i = 0; i < years.length; i++) {

			// Attempting a get request to the maps/$years$ folder to get the months in the year
		
			try {
				content = makeGetRequest(port, years[i]);
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Could not make get request to : " + years[i]);
			}
			
			// Parsing the HTML to get paths for maps/$years$/$months$


			String[] months = getPathsFromHTML(years[i], content);

			for (int j = 0; j < months.length; j++) {

				// Attempting a get request to the maps/$years$/$months$ folder to get the days of the month in the year
				try {
					content = makeGetRequest(port, months[j]);
				} catch (IOException e) {
					e.printStackTrace();
					throw new Error("Could not make get request to : " + months[i]);
				}
				
				// Parsing the final paths from HTML for a specified month

				String[] days = getPathsFromHTML(months[j], content);

				for (int k = 0; k < days.length; k++) {
					
					// Adding the final paths to the return array..
					
					mapPaths.add(days[k]);

				}
			}
		}

		return mapPaths;

	}

	// Parses the HTML from the webserver specifically to return the paths for each folder that is contained within the requested path..  

	private static String[] getPathsFromHTML(String parentpath, String content) {
	
		Document doc = Jsoup.parse(content);

		Elements arefs = doc.select("a");
	
		String[] paths = new String[arefs.size() - 3];
	
		int j = 0;
		
		// i = 2 as we ignore the /.. folder and header
		
		for (int i = 2; i < arefs.size() - 1; i++) {

			paths[j] = parentpath + arefs.get(i).html().toString();
			j++;

		}

		return paths;
	}
	
	// Function for making a get request to a specified path and port on a locally hosted webserver

	private static String makeGetRequest(String port, String path) throws IOException {
		
		
		
		URL url = new URL("http://localhost:" + port + "/" + path);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		
		// Attempt to set the connection type get request..
		try {
			con.setRequestMethod("GET");
		} catch (ProtocolException e) {
			e.printStackTrace();
			throw new Error("Error with protocol");
		}
		
		// Attempt to make get request itself and parse in the content returned by it as a string
		try {
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		
		
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {

			content.append(inputLine);

		}
		in.close();
		con.disconnect();

		return content.toString();
		
		}catch(ConnectException e) {
			e.printStackTrace();
			throw new Error("Error with connection, Check your port is correct");
		
		}

	}

//Function to create/overwrite file
	
	private static void saveToFile(String filename, String content, boolean isGeoJson) {
		try {
			String saveData = content;
			
			// False parameter specifies to overwrite
			
			// isGeoJson specifies wether to parse the content for saving as json first or not..
			
			if (isGeoJson) {
				JSONObject json = new JSONObject(content);
				saveData = json.toString();
			}
			
			
			// Saving the file..

			FileWriter file = new FileWriter(filename, false);
			file.write(saveData);
			file.close();
		} catch (IOException e) {
		
			e.printStackTrace();
			throw new Error("Could not save to specified file : " + filename);

		}

	}
}
		