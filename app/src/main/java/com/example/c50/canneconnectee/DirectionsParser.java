package com.example.c50.canneconnectee;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by rgf97 on 10/12/2018.
 */

public class DirectionsParser {
    /**
     * Returns a list of lists containing latitude and longitude from a JSONObject
     */
    private static String instruction = "";
    private static List<List<LatLng>> start_end_latLngs;
    private  static String duration;
    private  static String distance;

    public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

        List<List<HashMap<String, String>>> routes = new ArrayList<>();
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;
        JSONObject jStart_location;
        JSONObject jEnd_location;
        JSONObject jDuration;
        JSONObject jDistance;
        start_end_latLngs = new ArrayList<>();

        try {

            jRoutes = jObject.getJSONArray("routes");

            // Loop for all routes
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                List path = new ArrayList<HashMap<String, String>>();

                //Loop for all legs
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    jDistance = ((JSONObject) jLegs.get(j)).getJSONObject("distance");
                    distance = jDistance.getString("text");

                    jDuration = ((JSONObject) jLegs.get(j)).getJSONObject("duration");
                    duration = jDuration.getString("text");

                    //instruction = "";
                    //Loop for all steps
                    for (int k = 0; k < jSteps.length(); k++) {
                        String instruction_brut;
                        instruction_brut = (String) ((JSONObject) jSteps.get(k)).get("html_instructions");
                        instruction = instruction + instruction_brut.replaceAll("\\<.*?>", "") + "\n";

                        jStart_location = ((JSONObject) jSteps.get(k)).getJSONObject("start_location");
                        Double start_lat = Double.valueOf(jStart_location.getString("lat"));
                        Double start_lng = Double.valueOf(jStart_location.getString("lng"));
                        LatLng start_latLng = new LatLng(start_lat, start_lng);

                        jEnd_location = ((JSONObject) jSteps.get(k)).getJSONObject("end_location");
                        Double end_lat = Double.valueOf(jEnd_location.getString("lat"));
                        Double end_lng = Double.valueOf(jEnd_location.getString("lng"));
                        LatLng end_latLng = new LatLng(end_lat, end_lng);

                        List<LatLng> start_end_latLng = new ArrayList<>();
                        start_end_latLng.add(start_latLng);
                        start_end_latLng.add(end_latLng);
                        start_end_latLngs.add(start_end_latLng);

                        String polyline;
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List list = decodePolyline(polyline);

                        //Loop for all points
                        for (int l = 0; l < list.size(); l++) {
                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                            hm.put("lon", Double.toString(((LatLng) list.get(l)).longitude));
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return routes;
    }

    /**
     * Method to decode polyline
     * decoding polylines from google maps direction api with-java
     */
    private List decodePolyline(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    public String getInstruction() {
        return instruction;
    }

    public List<List<LatLng>> getStart_end_latLngs() {
        List<List<LatLng>> ll = start_end_latLngs;
        return ll;
    }

    public String getDuration(){
        return duration;
    }

    public String getDistance(){
        return distance;
    }

}