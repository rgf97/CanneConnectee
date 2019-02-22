package com.example.c50.canneconnectee;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Created by rgf97 on 10/12/2018.
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int SENSOR_DELAY = 500 * 1000; // 500ms
    private static final int LOCATION_REQUEST = 500;
    private static final int FROM_RADS_TO_DEGS = -57;
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String instruction;
    private static List<List<LatLng>> start_end_latLngs;
    ArrayList<LatLng> listPoints;
    LatLng latLng_dest;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private List<Double> steps_angle;
    private String distance;
    private String duration;
    private TextToSpeech myTTS;
    private TextView textView;
    private LatLng latLng_org;
    private String[] instruc_lines;
    private GoogleMap mMap;
    private MarkerOptions boss_markerOptions;
    private Marker temp_marker;
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private float azimuth;
    private boolean save = false;
    private boolean save1 = false;
    private boolean save2 = false;
    private Double current_step_angle;
    private LatLng upLatLng;
    private Boolean isLatLngFill = false;
    private int counter;
    private SpeechRecognizer mySR;
    private Double device_road_angle;
    private String live_instruction;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private Set<BluetoothDevice> pairedDevice;
    private String blue_address = null;


    private GoogleApiClient mGoogleApiClient;
    private LocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        initMap();
        try {
            setw();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mySR = SpeechRecognizer.createSpeechRecognizer(this);
        initializeSpeechRecogniser();
        counter = 0;
        Button button = findViewById(R.id.but_dir);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView = findViewById(R.id.svtv);
                textView.setText(instruction);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab2);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                mySR.startListening(intent);

            }
        });


        /*new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            }
        },0,1);*/

        //Aquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            return;

        }


        final TextView live_inst = findViewById(R.id.svtv2);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                //makeUseOfNewLocation(location);
                updateDeviceLocation(location);

                if (start_end_latLngs != null && instruc_lines != null) {
                    LatLng latLng = new LatLng(start_end_latLngs.get(counter).get(1).latitude, start_end_latLngs.get(counter).get(1).longitude);
                    Boolean b = isMarkerInsideCircle(location, latLng);
                    while (myTTS.isSpeaking()) {

                    }
                    speak(live_instruction);

                    if (b) {
                        if (counter < steps_angle.size() - 1) {
                            speak("Prochaine étape : " + instruc_lines[counter + 1] + ". " + live_instruction);
                            counter++;
                            current_step_angle = steps_angle.get(counter);
                        }
                        if (counter == steps_angle.size() - 1) {
                            Log.d("JE ", "onLocationChanged: ");
                            speak("Vous êtes arrivé ! Je rappelle, vous êtes arrivé ! Mode Navigation terminé.");
                            while (myTTS.isSpeaking()) {
                            }
                            startActivity(new Intent(MapsActivity.this, TrajectActivity.class));

                        }
                    }
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        // Register the listener with the Location Manager to receive location updates
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 0, locationListener);

        try {
            mSensorManager = (SensorManager) getSystemService(MapsActivity.SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception e) {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        latLng_dest = getIntent().getExtras().getParcelable("latLng_dest");
        Log.d("LatLng_dest : ", latLng_dest.toString());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        //Call our direction function
        getDeviceLocation();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mRotationSensor) {
            if (event.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                updateSensor(truncatedRotationVector);
            } else {
                updateSensor(event.values);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    private void updateSensor(float[] vectors) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        azimuth = orientation[0] * FROM_RADS_TO_DEGS;
        azimuth = (azimuth + 360) % 360;
        azimuth = 360 - azimuth;

        if (save && save2 && upLatLng != null) {
            temp_marker.remove();
            device_road_angle = (azimuth - current_step_angle + 360) % 360;
            ((TextView) findViewById(R.id.svtv2)).setText("" + device_road_angle);


            boss_markerOptions = new MarkerOptions().icon(bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_navigation_black_24dp));
            Log.d("ICI", "LUL " + counter + " " + steps_angle.size() + " " + start_end_latLngs.size());
            temp_marker = mMap.addMarker(boss_markerOptions.position(upLatLng).rotation(azimuth));
            if (isLatLngFill) {
                for (List<LatLng> list : start_end_latLngs) {
                    Circle circle = mMap.addCircle(new CircleOptions().center(new LatLng(list.get(1).latitude, list.get(1).longitude)).radius(10).fillColor(Color.RED));
                    initializeTextToSpeech();
                    isLatLngFill = false;
                }
            }

            if (save1) {
                if (device_road_angle >= 315 || device_road_angle <= 45) {
                    live_instruction = "continuer";
                    ((TextView) findViewById(R.id.svtv2)).setText("CONTINUER");
                }
            }
            if (device_road_angle > 45 && device_road_angle < 135) {
                live_instruction = "quart de tour à gauche";
                ((TextView) findViewById(R.id.svtv2)).setText("QUART DE TOUR A GAUCHE");
            }
            if (device_road_angle >= 135 && device_road_angle <= 225) {
                live_instruction = "demi-tour";
                ((TextView) findViewById(R.id.svtv2)).setText("DEMI TOUR");
            }
            if (device_road_angle > 225 && device_road_angle < 315) {
                live_instruction = "quart de tour à droite";
                ((TextView) findViewById(R.id.svtv2)).setText("QUART DE TOUR A DROITE");
            }

        }
    }

    //Get angle between 2 points
    private double angleFromCoordinate(double lat1, double long1, double lat2, double long2) {

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        //brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }

    private void getDeviceLocation() {

        Log.d("getDeviceLocation : ", "getting the devices current location");
        try {
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    if (location != null) {

                        latLng_org = new LatLng(location.getLatitude(), location.getLongitude());
                        //Reset Marker when already 2
                        if (listPoints.size() == 2) {
                            listPoints.clear();
                            mMap.clear();
                        }

                        //Save first points
                        listPoints.add(latLng_org);
                        listPoints.add(latLng_dest);

                        //Create Marker
                        MarkerOptions markerOptions_1 = new MarkerOptions();
                        MarkerOptions markerOptions_2 = new MarkerOptions();
                        MarkerOptions markerOptions_3 = new MarkerOptions();

                        markerOptions_1.position(latLng_org).title("org_marker");
                        markerOptions_2.position(latLng_dest).title("dest_marker");
                        markerOptions_3.position(latLng_org).title("device_marker");

                        //Add first marker to the map
                        markerOptions_1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                        //Add second Marker to the map
                        markerOptions_2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                        mMap.addMarker(markerOptions_1);
                        mMap.addMarker(markerOptions_2);
                        temp_marker = mMap.addMarker(markerOptions_3);


                        //Direction Code
                        if (listPoints.size() == 2) {
                            //Create the url to get the request from first marker to second marker
                            String url = getRequestUrl(listPoints.get(0), listPoints.get(1));
                            TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                            taskRequestDirections.execute(url);
                            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(listPoints.get(0), 15);
                            mMap.moveCamera(cu);
                            //mMap.animateCamera(cu);
                            save = true;
                        }


                    }
                }
            });


        } catch (SecurityException e) {

            Log.e("", "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    private void updateDeviceLocation(Location location) {

        upLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        Log.d("XD", "YOUHOU");

    }

    private Boolean isMarkerInsideCircle(Location location, LatLng latLng) {
        Boolean bool = false;
        float[] dist = new float[2];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), latLng.latitude, latLng.longitude, dist);
        if (dist[0] <= 5) {
            bool = true;
        }
        return bool;
    }


    private BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
        Drawable background = ContextCompat.getDrawable(context, R.drawable.ic_navigation_black_24dp);
        background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId);
        vectorDrawable.setBounds(40, 20, vectorDrawable.getIntrinsicWidth() + 40, vectorDrawable.getIntrinsicHeight() + 20);
        Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(), background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        background.draw(canvas);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void initializeSpeechRecogniser() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            mySR = SpeechRecognizer.createSpeechRecognizer(this);
            mySR.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {

                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int error) {

                }

                @Override
                public void onResults(Bundle results) {
                    List<String> res = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    Log.d("", "onResults: " + res.get(0));
                    processResult(res.get(0));

                }

                @Override
                public void onPartialResults(Bundle partialResults) {

                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            });
        }
    }

    private void processResult(String command) {
        command.toLowerCase();
        Log.d("yop", "processResult: " + command);
        if (command.contains("annulation") || command.contains("arrête") || command.contains("annule")) {
            while (myTTS.isSpeaking()) {
            }
            speak("Bien ! Mode Navigation terminé.");
            while (myTTS.isSpeaking()) {
            }
            startActivity(new Intent(MapsActivity.this, TrajectActivity.class));
        } else {
            while (myTTS.isSpeaking()) {

            }
            speak("Désolé, je n'ai pas compris.");
        }
    }

    private String getRequestUrl(LatLng origin, LatLng dest) {

        //Value of origin
        String str_org = "origin=" + origin.latitude + "," + origin.longitude;
        //Value of destination
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        //Set value enable the sensor
        //String sensor = "sensor=false";
        //Mode for find direction
        String mode = "mode=walking";
        String language = "language=fr";
        //Build the full param
        String param = str_org + "&" + str_dest + "&" + mode + "&" + language;
        //Output format
        String output = "json";
        //Create url to request
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param + "&key=" + getString(R.string.google_maps_key);

        Log.d("URL :", url);
        return url;

    }

    private String requestDirection(String reqUrl) throws IOException {

        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();

            //get the response result
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
                break;
        }
    }


    private void init() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).enableAutoManage(this, this).build();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        listPoints = new ArrayList<>();
    }

    private void initializeTextToSpeech() {
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (myTTS.getEngines().size() == 0) {
                    Toast.makeText(MapsActivity.this, "There is no TTS engine on your device", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    myTTS.setLanguage(Locale.FRANCE);
                    String add = "";

                    //Get present location
                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(latLng_org.latitude, latLng_org.longitude, 1);
                        Address obj = addresses.get(0);
                        add = obj.getAddressLine(0);
                        Log.v("New ", "Address :" + add);


                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    String[] adds = add.split(",");
                    instruc_lines = instruction.split("\\r?\\n");
                    speak("Mode navigation activée. Trajet prêt. Distance estimée : " + distance + ". Durée estimée : " + duration + ". " + "Vous êtes actuellement à" + adds[0] + ". " + instruc_lines[0]);
                    save1 = true;


                }
            }

        });
    }

    private void setw() throws IOException {
        bluetoothConnectDevice();
    }

    private void bluetoothConnectDevice() throws IOException {

        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            pairedDevice = bluetoothAdapter.getBondedDevices();

        } catch (Exception e) {
            e.printStackTrace();
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //get mobile bluetooth device
        BluetoothDevice bd = bluetoothAdapter.getRemoteDevice(blue_address);//connect to the device
        bluetoothSocket = bd.createInsecureRfcommSocketToServiceRecord(myUUID); //create a RFCOM (SPP) connexion
        bluetoothSocket.connect();

        /*
        read data sended
         */
        InputStream inputStream = bluetoothSocket.getInputStream();
        byte[] buffer = new byte[1024];
        int bytes;
        // Keep looping to listen for received messages

        while (true) {
            try {
                bytes = inputStream.read(buffer);            //read bytes from input buffer
                String readMessage = new String(buffer, 0, bytes);
                // Send the obtained bytes to the UI Activity via handler
                Log.d("logging", readMessage + "");
                ((TextView) findViewById(R.id.svtv2)).setText(readMessage);
            } catch (IOException e) {
                break;
            }
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        myTTS.shutdown();
        mSensorManager.unregisterListener(this);
    }

    private void speak(String message) {
        if (Build.VERSION.SDK_INT >= 21) {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /*
     ***Step by Step direction
     */

    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            String responseString = "";

            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);

        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            //Get list routes and display into the map
            JSONObject jsonObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                steps_angle = new ArrayList<>();
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);

                instruction = directionsParser.getInstruction();
                Log.d("INSTRUCTION : ", instruction);

                start_end_latLngs = directionsParser.getStart_end_latLngs();
                isLatLngFill = true;
                for (List<LatLng> list : start_end_latLngs) {
                    Log.d("L", list.get(0).toString() + " " + list.get(1).toString());
                    Double step_angle = angleFromCoordinate(list.get(0).latitude, list.get(0).longitude, list.get(1).latitude, list.get(1).longitude);
                    Log.d("A ", "step_angle " + step_angle);
                    steps_angle.add(step_angle);
                }

                distance = directionsParser.getDistance();
                duration = directionsParser.getDuration();
                current_step_angle = steps_angle.get(0);
                save2 = true;

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get list route and display it into the map

            ArrayList points = null;
            PolylineOptions polylineOptions = null;
            for (List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat, lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                //PolylineOptions.geodesic(true);

            }

            if (polylineOptions != null) {
                mMap.addPolyline(polylineOptions);
            } else {
                Toast.makeText(getApplicationContext(), "Direction not found!", Toast.LENGTH_SHORT);
            }
        }
    }
}
