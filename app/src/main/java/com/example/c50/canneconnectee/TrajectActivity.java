package com.example.c50.canneconnectee;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class TrajectActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private static final String country = "France";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final String city = "Le Mans";
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static LatLng latLng;
    private static String dest_address;
    private static Address address;
    private static String cur_location;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private GoogleApiClient googleApiClient;
    private AutoCompleteTextView autoCompleteTextView;
    private Boolean mLocationPermissionsGranted = true;
    private TextToSpeech myTTS;
    private SpeechRecognizer mySR;
    private String dest_address_form;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private Set<BluetoothDevice> pairedDevice;
    private TextView blue_tv;
    private TextView blue_tv2;
    private String blue_address = null;
    private String blue_name = null;
    private ArrayList<String> joke_list;
    private ArrayList<String> proverb_list;
    private FusedLocationProviderClient mFusedLocationClient;
    private String blue_status;
    final int handlerState = 0;
    private volatile boolean stopWorker;
    private InputStream inputStream;
    private Handler bluetoothIn;
    private StringBuilder sb = new StringBuilder();
    private ConnectedThread mConnectedThread;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        //do something
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traject);
        mySR = SpeechRecognizer.createSpeechRecognizer(this);
        autoCompleteTextView = findViewById(R.id.actv);
        addJoke();
        addProverb();
        dest_address = "";
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        blue_status = "La canne n'est pas connectée.";

        try {
            setw();
        } catch (Exception e) {
            e.printStackTrace();
        }

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    byte[] readBuff = (byte[]) msg.obj;
                    String print = "";
                    for (int i = 0; i < readBuff.length; i++) {
                        print += " " + readBuff[i];
                    }
                    String readMessage = new String(readBuff, 0, msg.arg1);

                    blue_tv2.setText("Data Received = " + readMessage + "\n" + "Data Length = " + readMessage.length());

                    speak("Obstacle détecté");
                    /*if(readMessage.length() == 6){
                        speak("Obstacle à gauche");
                        Log.d("", "handleMessage: Obstacle à gauche");
                    }
                    if(readMessage.length() == 1) {
                        speak("Obstacle à droite");
                        Log.d("", "handleMessage: Obstacle à gauche");
                    }
                    if(readMessage.length() == 2) {
                        speak("Obstacle devant");
                        Log.d("", "handleMessage: Obstacle à gauche");
                    }*/

                }
            }
        };



        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                mySR.startListening(intent);

            }
        });

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            return;

        }

        LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                getDeviceLoc();
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
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 2, locationListener);


        initializeTextToSpeech();

        initializeSpeechRecogniser();

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, googleApiClient, LAT_LNG_BOUNDS, null);

        autoCompleteTextView.setAdapter(placeAutocompleteAdapter);
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
        Date date = new Date();
        if (command.contains("quel")) {
            //Quel est ton nom?
            if (command.contains("ton nom") || command.contains("votre nom")) {
                speak("Mon nom est Alice.");
            }
            //Quelle heure est-il?
            if (command.contains("heure")) {
                String time = DateUtils.formatDateTime(this, date.getTime(), DateUtils.FORMAT_SHOW_TIME);
                speak("Il est " + time + ".");
            }
            //Quelle est la date d'aujourd'hui?
            if (command.contains("date")) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                Integer day_of_month = cal.get(Calendar.DAY_OF_MONTH);
                String[] days = new String[]{"Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
                String day = days[cal.get(Calendar.DAY_OF_WEEK) - 1];
                String[] months = new String[]{"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Decembre"};
                String month = months[cal.get(Calendar.MONTH)];
                Integer year = cal.get(Calendar.YEAR);
                speak("La date d'aujourd'hui est " + day + " " + day_of_month + " " + month + " " + year + ".");
            }
            //Quel age ton age?
            if (command.contains("âge")) {
                speak("J'ai 20 ans.");
            }
        }

        if (command.contains("raconte")) {
            //Raconte moi une blague
            if (command.contains("blague")) {
                int joke_lenght = joke_list.size();
                int joke_num = new Random().nextInt(joke_lenght - 1);
                speak(joke_list.get(joke_num));
            }
            //Raconte moi un proverbe
            if (command.contains("proverbe")) {
                int proverb_lenght = proverb_list.size();
                int proverb_num = new Random().nextInt(proverb_lenght - 1);
                speak(proverb_list.get(proverb_num));
            }
        }

        //Où suis-je?
        if (command.contains("position")) {
            speak("Vous êtes actuellement à " + cur_location);
        }
        if (command.contains("où suis-je") || command.contains("où je suis")) {
            speak("Vous êtes actuellement à " + cur_location);
        }

        //Destination
        if (command.contains("destination")) {

            dest_address = command.substring(command.indexOf(" "));
            dest_address_form = dest_address + ", " + city + ", " + country;

            getDeviceLoc();
            String[] cur_locs = cur_location.split(",");
            String cur_loc_format = cur_locs[0];
            if (cur_loc_format.contains(dest_address)) {
                speak("Vous êtes déjà à destination !");
            } else {
                autoCompleteTextView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        autoCompleteTextView.setText(dest_address_form);
                        //autoCompleteTextView.showDropDown();
                        getLocationPermission();
                        if (mLocationPermissionsGranted) {
                            //geolocate the destination
                            geoLocate();
                            //Send destination data
                            try {
                                bluetoothSocket.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent(TrajectActivity.this, MapsActivity.class);
                            intent.putExtra("latLng_dest", latLng);
                            startActivity(intent);
                        }

                    }
                }, 10);
                speak("En route vers " + dest_address);
                while (myTTS.isSpeaking()) {

                }
            }


        }

        if (command.contains("guide")) {
            speak("Bienvenue dans le manuel d'utilisation. L'objectif principal de cette application est de vous guider" +
                    " dans vos déplacements, en vous conduisant à un endroit souhaité tout en vous évitant les obstacles" +
                    " sur votre trajet. Dîtes Destination suivi de l'endroit où vous desiriez aller et le mode Navigation sera " +
                    "activé. Puis, vous n'avez plus qu'à vous laisser guider. Vous pouvez néanmoins annuler à tout moment le mode" +
                    " Navigation en disant Annulation ou Arrête ou Annule. A noter par ailleurs que l'application vous evite des obstacles même" +
                    " en étant pas en mode Navigation, simplement si la canne est connectée. L'application contient également d'autres " +
                    "fonctionnalités telles que : Quelle heure est-il? Quelle est la date d'aujourd'hui? Raconte moi un proverve, raconte moi " +
                    "une blaque. Quel est ton nom?, Quel âge as-tu?, seulement disponibles en mode non Navigation, pour vous faire passer du bon temps. " +
                    "A vous de jouer !");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        myTTS.shutdown();
        mySR.stopListening();
        /*try {
            bluetoothSocket.close();
        }catch (Exception e){
            e.printStackTrace();
        }*/

    }

    private void getDeviceLoc() {

        try {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {

                        Geocoder geocoder = new Geocoder(TrajectActivity.this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            Address obj = addresses.get(0);
                            cur_location = obj.getAddressLine(0);
                            Log.d("J", "rentreee ");

                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }
            });
        } catch (SecurityException e) {

            Log.e("", "getDeviceLocation: SecurityException: " + e.getMessage());
        }

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

    private void initializeTextToSpeech() {
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (myTTS.getEngines().size() == 0) {
                    Toast.makeText(TrajectActivity.this, "There is no TTS engine on your device", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    myTTS.setLanguage(Locale.FRANCE);
                    speak("Bonjour ! Mon nom est Alice. \n" + blue_status + "\n Veillez appuyer pour parler.\n Pour obtenir de l'aide, dîtes Guide d'utilisation. ");
                }
            }
        });
    }


    private void speak(String message) {
        if (Build.VERSION.SDK_INT >= 21) {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
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

            address = list.get(0);
            Log.d("", "geoLocate: found a location: " + address.toString());

            latLng = new LatLng(address.getLatitude(), address.getLongitude());

        }

    }

    private void addJoke() {
        joke_list = new ArrayList<>();
        joke_list.add("Comment appelle-t-on un lapin sourd ? laaaapin");
        joke_list.add("Que demande un douanier à un cochon qui passe la frontière ? Son passe-porc.");
        joke_list.add("Que crie un escargot sur le dos d'une tortue ? Youhooooooouuuuuuu !");
        joke_list.add("Quelle est la puissance d´un coton-tige ? 2 ouates");
        joke_list.add("Qu'est-ce qui peut être servi mais jamais mangé ? Une balle de tennis.");
        joke_list.add("Qu'est ce qui est jaune et qui attend ? Jonathan.");
        joke_list.add("Un chameau dit à un dromadaire : - Comment ça va ? - Bien. Je bosse. Et toi ? - Je bosse, je bosse !");
        joke_list.add("Pourquoi les lapins jouent-ils avec 46 cartes au lieu de 52 ? Parce qu'ils ont mangé les trèfles !");
        joke_list.add("Une jeune fille se plaint à son amie : - À tous nos rendez-vous, il m'offre des fleurs fanées. - Eh bien, essaye d'arriver à l'heure...");
        joke_list.add("Quel est le comble pour un Geek ? Etre privé de sortie...");
        joke_list.add("Quelle heure est-il lorsqu’un éléphant s’assied sur une clôture ? L’heure de la changer !");
        joke_list.add("Quel est le comble pour un mathématicien ? Se faire voler sa moitié par un tiers dans un car.");
        joke_list.add("Pourquoi les plongeurs plongent-ils en arrière ? Parce que sinon ils tomberaient dans le bateau.");
        joke_list.add("Deux poissons croisent une étoile de mer : \"Attention, voilà le shérif !\"");
        joke_list.add("Quel animal a trois bosses ? Un chameau qui s'est cogné.");
        joke_list.add("Un coq rentre au poulailler avec un oeuf d’autruche : \"Mesdames, je ne voudrais pas vous vexer mais regardez ce que produit la concurrence…\"");
        joke_list.add("2 geeks discutent le 2 janvier : \"Quelle est ta résolution cette année ?\" \"1280 x 768\".");
        joke_list.add("Pourquoi les aiguilles sont-elles moins intelligentes que les épingles ? Parce qu'elles n'ont pas de tête.");
        joke_list.add("Quel est le numéro de téléphone de la poule ? \"4 4 4 7 1 9 !\"");
        joke_list.add("Quel est le fruit que les poissons détestent ? La pêche.");
        joke_list.add("Un vrai geek, c’est celui qui croit que, dans 1 km, il y a 1 024 mètres.");
        joke_list.add("Que s'est-il passé en 1 111 ? L'invasion des Uns !");
        joke_list.add("Quel est l'animal qui n'a jamais soif ? Le zébu, car quand zébu z'ai plus soif !");
        joke_list.add("Comment appelle-t-on un ascenseur en Chine ? En appuyant sur le bouton...");
        joke_list.add("Un 0 rencontre un 8 : \"Eh ! Elle est chouette ta ceinture !\"");
        joke_list.add("Une femme discute avec une amie :\n" + "– « J’ai un mari en or. »\n" + "L’autre lui répond :\n" + "« Moi, le mien, il est en taule. »");
        joke_list.add("Docteur, je perd la mémoire, que dois-je faire ? Commencez déjà par me payer");
        joke_list.add("Comment est-ce que la chouette sait que son mari fait la gueule ? Parce qu’hiboude");
        joke_list.add("Pourquoi est-ce qu'on met tous les crocos en prison ? Parce que les crocos dil.");
        joke_list.add("Quel est le bar préféré des espagnols ? Le Bar-celone");
        joke_list.add("D'où viennent les gens les plus dangereux ? D’Angers");
        joke_list.add("Quelle est la fée que les enfants détestent ? La fée C");
        joke_list.add("Toto, quelle planète vient après Mars ? Avril");
        joke_list.add("Tu as passé une bonne nuit, Toto ? Je ne sais pas, j'ai dormi tout le temps");
        joke_list.add("Qu'est ce que c'est cette page blanche, Toto ? Une page de calcul mentale");
        joke_list.add("Toto, tu devrais tout de même essayer d'écrire plus lisiblement. Ah ben, sûrement pas ! Un jour j'ai essayé et la maîtresse s'est aperçue que je faisais des fautes d'orthographes...");
        joke_list.add("[Dans une salle d'accouchement]. Mais où est le cordon ombilical ??!! C'est une génération Wifi, sans fil !");
        joke_list.add("Pourquoi tu fais cette grimace ? Pour faire peur aux ours. Il y en a pas ici ! Tu vois ça marche !");
        joke_list.add("Tu connais la blague de la chaise? Elle est pliante");
        joke_list.add("Quelle est la seule et unique différence entre un séducteur et un violeur ? La Patience…");

    }

    private void addProverb() {
        proverb_list = new ArrayList<>();
        proverb_list.add("\"On a deux vies. La deuxième commence le jour où l'on réalise qu'on n'en a juste une.\" Confucius");
        proverb_list.add("\"Tout le monde peut être important car tout le monde peut servir à quelque chose.\" Martin Luther King");
        proverb_list.add("\"Le véritable voyage de découverte ne consiste pas à chercher de nouveaux paysages, mais à avoir de nouveaux yeux.\" Marcel Proust");
        proverb_list.add("\"Le plus sage des hommes chérit la bêtise de temps à autre.\" Roald Dahl");
        proverb_list.add("\"L’avenir à chaque instant presse le présent d’être un souvenir.\" Louis Aragon");
        proverb_list.add("\"Je suis un lutteur et un gagneur. Je refais cent fois et j'apprends tous les jours quelque chose de nouveau.\" Yves Saint Laurent");
        proverb_list.add("\"Le bonheur est la plus grande des conquêtes, celle qu'on fait contre le destin qui nous est imposé.\" Albert Camus");
    }

    /*
     **Android Bluetooth connexion part
     */
    private void setw() throws IOException {

        blue_tv = findViewById(R.id.blue_tv);
        blue_tv2 = findViewById(R.id.blue_tv2);
        bluetoothConnectDevice();
        mConnectedThread = new ConnectedThread(bluetoothSocket);
        mConnectedThread.start();
    }


    private void bluetoothConnectDevice() throws IOException {

        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            blue_address = bluetoothAdapter.getAddress();
            pairedDevice = bluetoothAdapter.getBondedDevices();
            if (pairedDevice.size() > 0) {
                for (BluetoothDevice bt : pairedDevice) {
                    blue_address = bt.getAddress();
                    blue_name = bt.getName();
                    Toast.makeText(getApplicationContext(), "Cane connected", Toast.LENGTH_SHORT).show();
                    blue_status = "La canne est connectée !";

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //get mobile bluetooth device
        BluetoothDevice bd = bluetoothAdapter.getRemoteDevice(blue_address);//connect to the device
        bluetoothSocket = bd.createInsecureRfcommSocketToServiceRecord(myUUID); //create a RFCOM (SPP) connexion
        bluetoothSocket.connect();
        try {
            blue_tv.setText("Bluetooth Name : " + blue_name + "\nBluetooth Adress : " + blue_address);
        } catch (Exception e) {
        }


    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
        }


        public void run() {
            byte[] buffer = new byte[8];
            int bytes;
            while (true) {
                try {
                    if (mmInStream.available() > 0) {
                        bytes = mmInStream.read(buffer);//read bytes from input buffer
                        bluetoothIn.obtainMessage(handlerState, bytes, -1, buffer).sendToTarget();
                    } else {

                    }


                } catch (IOException e) {
                    break;
                }
            }
        }

    }

}


