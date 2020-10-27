package uk.ac.ed.inf.aqmap;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.json.*;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
//Importing the mapbox and JSON
import com.google.gson.JsonObject;
import com.mapbox.geojson.*;

/**
 * Hello world!
 *
 */
public class App 
{
	
	public static java.util.Random rnd;
	public static Sensor[] sensors;
	public static MultiPolygon restrictedareas;
	public static MultiPolygon confinementarea;
    public static double closestdis; 
	
	public static void main( String[] args ){
    	
    	// Parsing the CL arguments 
    	String port = args[6];
    	
    	String[] startingCoords = {args[3],args[4]};
    	
    	int randomiser = Integer.parseInt(args[5]);
    	
    	rnd = new Random(randomiser);
    	    	
    	sensors = getSensors(port,args);
    	    	
    	restrictedareas = getRestrictedAreas(port);
    	    	
		confinementarea = setConfinementArea();
		
    	Drone theDrone = new Drone(new Position(Double.parseDouble(startingCoords[1]),Double.parseDouble(startingCoords[0])));
    //	Drone theDrone2 = new Drone(new Position(Double.parseDouble(startingCoords[1]),Double.parseDouble(startingCoords[0])));
	//	Drone theDrone = new Drone(new Position(Double.parseDouble("138.6170768737793"),Double.parseDouble("-34.72292001874438")));
    	
//       	System.out.println(theDrone.getDronePosition().isInArea() && !theDrone.getDronePosition().isInRestricted());
//    	
//    	System.out.println(theDrone.getClosestSensor().toString());
// 
   
//    	
//    	theDrone.playRandom();
//    	
//    	List<Position> poss = new ArrayList<Position>();
//    	
//    	
//    	
//    	
//		for(Point p : theDrone.flightPoints) {
//			
//			poss.add(new Position(p.longitude(), p.latitude()));
//			
//		}
//		List<Position> posss = new ArrayList<Position>();
//		for(int j = poss.size() - 1 ; j >= 0 ; j--) {
//			
//			
//			posss.add(poss.get(j));
//			
//		}
//		
//		System.out.println("SIZE : " + poss.size());
//	
//		
//		for(int d : theDrone2.getDirections(posss)){
//			
//			theDrone2.moveDrone(d);
//			
//		}
		
    	
    	//saveToFile("thesensors.geojson",theDrone.getMap().toJson(),true);
//
//    	Position pos1 = new Position(-3.1892,55.9442);
//    	Position pos2 = new Position(-3.1896,55.9442);
//    	
//    	System.out.println(pos1.directionTo(pos2));
    	
    	theDrone.playAstarGreedy();
    	
    	//theDrone.moveDrone(-9);
    	
    	saveToFile("drone1.geojson",theDrone.getMap().toJson(),true);
    //	saveToFile("drone2.geojson",theDrone2.getMap().toJson(),true);
    	
    	
    }
    

    
  private static MultiPolygon setConfinementArea() {
	
	  
	  	Point ForrestHill = Point.fromLngLat(-3.192473,55.946233); 
		Point KFC = Point.fromLngLat(-3.184319,55.946233); 
		Point Meadows= Point.fromLngLat(-3.192473,55.942617); 
		Point BusStop = Point.fromLngLat(-3.184319,55.942617);
  	
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
			content = makeGetRequest(port,"buildings/no-fly-zones.geojson");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	
  	JSONObject Json = new JSONObject(content);
  	
  	
  	
  	JSONArray jsonarr = Json.getJSONArray("features");
  	
  	List<Polygon> Polygons = new ArrayList<Polygon>();
  	
  	List<Position> Poslist = new ArrayList<Position>();
  	
  	for(int i = 0; i < jsonarr.length() ; i++) {
  	
  		JSONObject jsonpoint = new JSONObject(jsonarr.get(i).toString());
  		
  		Polygon restricted = Polygon.fromJson(jsonpoint.get("geometry").toString());
  	
  		
  		//Assuming that these aren't pure mad twisty polygons..
  		for(Point l : restricted.coordinates().get(0)) {
  			Poslist.add(new Position(l.longitude(),l.latitude()));
  		}
  			
  		
  		Polygons.add(restricted);
  	
  		
  	}
  	
  	closestdis = Position.getClosestDistanceFromArray(Poslist);
  	

  	
	return MultiPolygon.fromPolygons(Polygons);
		
	}



private static Sensor[] getSensors(String port, String[] args) {
	  
	  
	  String content = "";
  	
  	try {
			content = makeGetRequest(port,"maps/"+args[2]+"/"+args[1]+"/"+args[0]+"/air-quality-data.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	
  	JSONObject[] aqJsonData = parseinAirQualityJSON(content);
	  
	  
	  JSONObject[] wordsJsonData = new JSONObject[33];
  	
  	//Hashtable<String, double[]> sensors = new Hashtable<String, double[]>();
  	
  	Sensor[] sensors = new Sensor[33];
	  for(int i  = 0; i<33;i++) {
	    	
  		try {
				wordsJsonData[i] = new JSONObject(makeGetRequest(port,"words/" + aqJsonData[i].getString("location").replace(".", "/") + "/details.json"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  		
  		JSONObject coords = new JSONObject(wordsJsonData[i].get("coordinates").toString());
  		
  		
  		
  		String reading = aqJsonData[i].get("reading").toString(); 
  		
 		   		
			double sensorData[] = {coords.getDouble("lng"),coords.getDouble("lat"),aqJsonData[i].getDouble("battery")}; 
  		
			
			
			//System.out.println(wordsJsonData[i].get("words") + " is at " + sensorData[0] + " , " + sensorData[1] + " with battery .. "+ sensorData[2]);
  		
			
			sensors[i] = new Sensor(wordsJsonData[i].get("words").toString(),new Position(sensorData[0],sensorData[1]),sensorData[2],reading);
		
  	}    	 
		return sensors;
	}



private static JSONObject[] parseinAirQualityJSON(String content) {
		

	  
	  String[] jsonStrings = content.substring(1, content.length()-1).split("},");
	  
	  JSONObject[] JSONS = new JSONObject[33];
	  
	  for(int i = 0 ; i < 32 ; i++) {
		  
		
		  JSONS[i] = new JSONObject(jsonStrings[i]+"}");
	  }
	  
	  JSONS[32] = new JSONObject(jsonStrings[32]);
	  
	  return JSONS;
	  
	}

public static String getRGB(double x) {
	
	if (0 <= x && x < 256) { // making sure in range 1..11
	    if (0 <= x && x < 32) {
	        return "#00ff00";
	    } else if ( 32 <= x && x < 64) {
	        return "#40ff00";
	    } else if ( 64 <= x &&  x < 96) {
	        return "#80ff00";
	    }else if ( 96 <= x &&  x < 128) {
	        return "#c0ff00";
	    }else if ( 128 <= x &&  x < 160) {
	        return "#ffc000";
	    }else if ( 160 <= x &&  x < 192) {
	        return "#ff8000";
	    }else if ( 192 <= x &&  x < 224) {
	        return "#ff4000";
	    }else if ( 224 <= x &&  x < 256) {
	        return "#ff0000";
	    }
	} 
	
	return "";
	
}

private static String[] getAllMapPaths(String port) {
		
	  	String rootpath = "maps/";
	  	
		String content = "";
		
		try {
			content  = makeGetRequest(port,rootpath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		
		String[] years = getPathsFromHTML(rootpath,content);
		
		
		
		
		for(int i = 0 ; i < years.length; i++) {
			
			try {
				content  = makeGetRequest(port,years[i]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String[] months = getPathsFromHTML(years[i],content);
			
			for(int j=0; j < months.length ; j++) {
			
				
				try {
					content  = makeGetRequest(port,months[j]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				String[] days = getPathsFromHTML(months[j],content);
				
				
				for(int k=0; k < days.length; k++) {
					

					
					/**TODO :: read course work spec/aim and decide upon what 
					 * data structure would best suit all the days paths
					 * they are all printed here but there is probably a better
					 * way of sorting them than in a 1d array lol..
					**/
					
					System.out.println(days);
					
				}
			}
		}
			
		
		
		
		
		
		return null;
		
	}



private static String getNoFlyZone(String port) {
	  
	  String content = "";
	  
	  try {
			content = makeGetRequest(port, "buildings/no-fly-zones.geojson");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  return content;
	}



private static String[] getAllWordPaths(String port) {
	  
	  	String rootpath = "words/";
  	
		String content = "";
		
		try {
			content  = makeGetRequest(port,rootpath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	
		String[] paths = getPathsFromHTML(rootpath,content);
		
		
			for(int k = 0; k<2;k++) {
				for(int i = 0; i< paths.length; i++) {
				
					try {
						content  = makeGetRequest(port,paths[i]);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					//As there is only one word in each directory..
					paths[i] = getPathsFromHTML(paths[i],content)[0];
					
				}
			}
			
			for(int j = 0; j < paths.length; j++) {
				paths[j] = paths[j] + "details.json";
			}
			
			
			return paths;
		
		
		
	
	}



private static String[] getPathsFromHTML(String parentpath, String content) {
	  Document doc = Jsoup.parse(content);
		
		Elements arefs = doc.select("a");
		String[] paths = new String[arefs.size()-3];
		int j = 0;
		for(int i = 2; i < arefs.size()-1; i++) {
			
			paths[j] = parentpath + arefs.get(i).html().toString();
			j++;
			
		}
		
		
		return paths;
	}



private static String makeGetRequest(String port, String path) throws IOException {
		// TODO Auto-generated method stub
	  URL url = new URL("http://localhost:" + port + "/" + path);
  	HttpURLConnection con = (HttpURLConnection) url.openConnection();
  	try {
			con.setRequestMethod("GET");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	
  	BufferedReader in = new BufferedReader(
  			  new InputStreamReader(con.getInputStream()));
  			String inputLine;
  			StringBuffer content = new StringBuffer();
  			while ((inputLine = in.readLine()) != null) {
  				
  			    content.append(inputLine);
  			    
  			}
  			in.close();
  	con.disconnect();
  	
  	return content.toString();
		
	}



//Function to create/overwrite file
    public static void saveToFile(String filename, String s, boolean isGeoJson) {
    	try {	
    		String saveData = s;
    		//False parameter specifies to overwrite
    		if(isGeoJson) {
    		JSONObject json = new JSONObject(s);  
    		saveData = json.toString();
    		}
    		
	         FileWriter file = new FileWriter(filename,false);
	         file.write(saveData);
	         file.close();
	      } catch (IOException e) {
	         // TODO Auto-generated catch block
	    	  e.printStackTrace();
	    	  throw new Error("L");
	        
	      }
    
    }
}
