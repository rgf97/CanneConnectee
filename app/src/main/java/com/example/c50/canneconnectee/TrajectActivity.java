package com.example.c50.canneconnectee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.support.design.widget.FloatingActionButton;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class TrajectActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private GoogleApiClient googleApiClient;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private AutoCompleteTextView autoCompleteTextView;
    private LatLng latLng;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private Boolean mLocationPermissionsGranted = true;
    private TextToSpeech myTTS;
    private SpeechRecognizer mySR;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traject);
        mySR = SpeechRecognizer.createSpeechRecognizer(this);
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.actv);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                mySR.startListening(intent);
            }
        });

        initializeTextToSpeech();

        initializeSpeechRecogniser();


        Button button_go = (Button) findViewById(R.id.button_Go);
        button_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocationPermissionsGranted) {

                    //Send destination data
                    Intent intent = new Intent(TrajectActivity.this, MapsActivity.class);
                    intent.putExtra("latLng_dest", latLng);
                    startActivity(intent);
                }

            }
        });

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, googleApiClient, LAT_LNG_BOUNDS, null);

        autoCompleteTextView.setAdapter(placeAutocompleteAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String selection = parent.getItemAtPosition(position).toString();
                Log.d("Selected : ", selection);
                getLocationPermission();
                if (mLocationPermissionsGranted) {
                    //geolocate the destination
                    geoLocate();
                }
            }
        });

    }

    private void initializeSpeechRecogniser() {
        if(SpeechRecognizer.isRecognitionAvailable(this)){
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
                    Log.d("", "onResults: "+res.get(0));
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
        Log.d("JENTRE", "processResult: "+command);
        command.toLowerCase();
        Date date = new Date();
        if(command.contains("quel")){
            Log.d("JENTRE", "Quel: ");
            //Quel est ton nom?
            if(command.indexOf("ton nom") != -1){
                Log.d("JENTRE", "ton nom: ");
                speak("Mon nom est Alice.");
            }
            //Quelle heure est-il?
            if(command.indexOf("heure") != -1){
                String time = DateUtils.formatDateTime(this, date.getTime(), DateUtils.FORMAT_SHOW_TIME);
                speak("Il est "+time+".");
            }
            //Quelle est la date d'aujourd'hui?
            if(command.indexOf("date") != -1){
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                Integer day_of_month = cal.get(Calendar.DAY_OF_MONTH);
                String[] days  = new String[] {"Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
                String day = days[cal.get(Calendar.DAY_OF_WEEK)];
                String[] months = new String[] {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Decembre"};
                String month = months[cal.get(Calendar.MONTH)];
                Integer year = cal.get(Calendar.YEAR);
                speak("La date d'aujourd'hui est "+day+" "+day_of_month+" "+month+" "+year+".");
            }
            //Quel age ton age?
            if(command.indexOf("âge") != -1){
                speak("J'ai 20 ans.");
            }
        }

        if (command.indexOf("raconte") != -1){
            //Raconte moi une blague
            if (command.indexOf("blague") != -1){
                speak("Qu'est ce qui est jaune et qui attend? Jonathan." );
            }
            //Raconte moi une histoire
        }

        //Destination
        if (command.indexOf("destination") != -1){
            speak("En route vers la destination");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        myTTS.shutdown();
    }

    private void getLocationPermission() {

        Log.d("", "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                //;
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }

        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    private void initializeTextToSpeech(){
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (myTTS.getEngines().size() == 0){
                    Toast.makeText(TrajectActivity.this, "There is no TTS engine on your device", Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    myTTS.setLanguage(Locale.FRANCE);
                    speak("Bonjour. Je m'appelle Alice. Je suis prête.");
                }
            }
        });
    }



    private void speak(String message) {
        if (Build.VERSION.SDK_INT >= 21){
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }

    }

    private void geoLocate() {

        Log.d("", "geoLocate: geolocating");
        String searchString = autoCompleteTextView.getText().toString();
        Geocoder geocoder = new Geocoder(TrajectActivity.this);

        List<Address> list = new ArrayList<>();

        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e("", "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {

            Address address = list.get(0);
            Log.d("", "geoLocate: found a location: " + address.toString());

            latLng = new LatLng(address.getLatitude(), address.getLongitude());

        }

    }

}
