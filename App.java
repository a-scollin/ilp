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

		restrictedareas = getRestrictedAreas(port);

		CONFINEMENT_AREA = setConfinement();

		saveToFile("nofly.geojson", App.restrictedareas.toJson(), true);

		testDrone(startingCoords, "maps/" + args[2] + "/" + args[1] + "/" + args[0], port, false);


	}

	private static FeatureCollection generateMap(Drone testDrone) {

		List<Feature> features = new ArrayList<Feature>();

		List<Sensor> unvisited = testDrone.getUnvisited();

		List<Point> flightPoints = testDrone.getFlightPoints();

		for (Sensor s : testDrone.getAllSensors()) {

			if (!unvisited.contains(s)) {

				Feature visitedsensor = Feature
						.fromGeometry(Point.fromLngLat(s.getPosition().getLng(), s.getPosition().getLat()));

				visitedsensor.addStringProperty("marker-size", "medium");
				visitedsensor.addStringProperty("location", s.getName());
				if (s.getBattery() < 10) {

					visitedsensor.addStringProperty("marker-symbol", "lighthouse");
					visitedsensor.addStringProperty("marker-color", "#000000");
					visitedsensor.addStringProperty("rgb-string", "#000000");

				} else {
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

				Feature unvisitedsensor = Feature
						.fromGeometry(Point.fromLngLat(s.getPosition().getLng(), s.getPosition().getLat()));

				unvisitedsensor.addStringProperty("marker-size", "medium");
				unvisitedsensor.addStringProperty("location", s.getName());
				unvisitedsensor.addStringProperty("marker-color", "#aaaaaa");
				unvisitedsensor.addStringProperty("rgb-string", "#aaaaaa");

				features.add(unvisitedsensor);
			}
		}

		features.add(Feature.fromGeometry(LineString.fromLngLats(flightPoints)));

		return FeatureCollection.fromFeatures(features);

	}

	private static void testDrone(String[] startingCoords, String path, String port, boolean test) {

		sensors = getSensors(port, path);

		Drone testDrone = new Drone(
				new Position(Double.parseDouble(startingCoords[1]), Double.parseDouble(startingCoords[0])), App.sensors,
				App.closestdis, App.rnd);

		System.out.println("Map path : " + path);

		testDrone.playAstarGreedy();

		System.out.println(testDrone.getMoves() + " Moves left. GREEDY");

		if (testDrone.hasFailed()) {
			System.out.println("DRONE FAILED GREEDY, ATTEMPTING SIMULATED ANNEALING (MAY TAKE A WHILE)");
			testDrone = new Drone(
					new Position(Double.parseDouble(startingCoords[1]), Double.parseDouble(startingCoords[0])),
					App.sensors, App.closestdis, App.rnd);
			testDrone.playAstarSimAnneal();
			System.out.println(testDrone.getMoves() + " Moves left. SA ");

		}

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

	private static String formatFlightPath(Drone testDrone) {

		String flightpath = "";

		for (String s : testDrone.getFlightPath()) {
			flightpath += s + "\n";
		}

		return flightpath;

	}


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

	private static MultiPolygon getRestrictedAreas(String port) {

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

		for (int i = 0; i < jsonarr.length(); i++) {

			JSONObject jsonpoint = new JSONObject(jsonarr.get(i).toString());

			Polygon restricted = Polygon.fromJson(jsonpoint.get("geometry").toString());

			// Assuming that these aren't pure mad twisty polygons..
			for (Point l : restricted.coordinates().get(0)) {
				Poslist.add(new Position(l.longitude(), l.latitude()));
			}

			Polygons.add(restricted);

		}

		closestdis = getClosestDistanceFromArray(Poslist);

		return MultiPolygon.fromPolygons(Polygons);

	}

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

	private static List<Sensor> getSensors(String port, String path) {

		String content = "";

		try {
			content = makeGetRequest(port, path + "/air-quality-data.json");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Could not access : " + path + "/air-quality-data.json");
		}

		JSONObject[] aqJsonData = parseinAirQualityJSON(content);

		JSONObject[] wordsJsonData = new JSONObject[33];

		List<Sensor> sensors = new ArrayList<Sensor>();
		
		for (int i = 0; i < 33; i++) {

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

			JSONObject coords = new JSONObject(wordsJsonData[i].get("coordinates").toString());

			String reading = aqJsonData[i].get("reading").toString();

			double sensorData[] = { coords.getDouble("lng"), coords.getDouble("lat"),
					aqJsonData[i].getDouble("battery") };

			sensors.add(new Sensor(wordsJsonData[i].get("words").toString(), new Position(sensorData[0], sensorData[1]),
					sensorData[2], reading));

		}
		
		return sensors;
	}

	private static JSONObject[] parseinAirQualityJSON(String content) {

		String[] jsonStrings = content.substring(1, content.length() - 1).split("},");

		JSONObject[] aqJsonArray = new JSONObject[33];

		for (int i = 0; i < 32; i++) {

			aqJsonArray[i] = new JSONObject(jsonStrings[i] + "}");
		}

		aqJsonArray[32] = new JSONObject(jsonStrings[32]);

		return aqJsonArray;

	}

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

		try {
			content = makeGetRequest(port, rootpath);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Could not make get request to : " + rootpath);
		}

		String[] years = getPathsFromHTML(rootpath, content);

		List<String> mapPaths = new ArrayList<String>();

		for (int i = 0; i < years.length; i++) {

			try {
				content = makeGetRequest(port, years[i]);
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Could not make get request to : " + years[i]);
			}

			String[] months = getPathsFromHTML(years[i], content);

			for (int j = 0; j < months.length; j++) {

				try {
					content = makeGetRequest(port, months[j]);
				} catch (IOException e) {
					e.printStackTrace();
					throw new Error("Could not make get request to : " + months[i]);
				}

				String[] days = getPathsFromHTML(months[j], content);

				for (int k = 0; k < days.length; k++) {
					
					mapPaths.add(days[k]);

				}
			}
		}

		return mapPaths;

	}


	private static String[] getPathsFromHTML(String parentpath, String content) {
		Document doc = Jsoup.parse(content);

		Elements arefs = doc.select("a");
		String[] paths = new String[arefs.size() - 3];
		int j = 0;
		for (int i = 2; i < arefs.size() - 1; i++) {

			paths[j] = parentpath + arefs.get(i).html().toString();
			j++;

		}

		return paths;
	}

	private static String makeGetRequest(String port, String path) throws IOException {
		
		URL url = new URL("http://localhost:" + port + "/" + path);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		try {
			con.setRequestMethod("GET");
		} catch (ProtocolException e) {
			e.printStackTrace();
			throw new Error("Error with protocol");
		}
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
			if (isGeoJson) {
				JSONObject json = new JSONObject(content);
				saveData = json.toString();
			}

			FileWriter file = new FileWriter(filename, false);
			file.write(saveData);
			file.close();
		} catch (IOException e) {
		
			e.printStackTrace();
			throw new Error("Could not save to specified file : " + filename);

		}

	}
}
