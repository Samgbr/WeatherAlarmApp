package example.com.finalproject;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Sam on 02/03/2017.
 */

public class WeatherAlarmService extends IntentService{


    public static final String LAT = "latitude";
    public static final String LONG = "longitude";
    public static final String XML_URL = "xml_url";
    ArrayList<String> stationInfo = new ArrayList<>();
    ArrayList<String> xmlUrlInfo = new ArrayList<>();
    ArrayList<Double> latitudeInfo = new ArrayList<>();
    ArrayList<Double> longitudeInfo = new ArrayList<>();
    ArrayList<Double> locDistanceDiffInfo = new ArrayList<>();
    public static final String STATION_ID = "station_id";
    public double deviceLat = 0;
    public double deviceLng = 0;

    final String TITLE = "title";
    final String STATION_Id = "station_id";
    final String OBSERVATION_TIME = "observation_time";
    final String OBSERVATION_TIME_RFC822 = "observation_time_rfc822";
    final String TEMPERATURE = "temperature_string";
    final String WIND = "wind_string";
    final String DEWPOINT_STRING = "dewpoint_string";
    final String WINDCHILL = "windchill_string";
    final String MEAN_WAVE_DIR = "mean_wave_dir";

    public WeatherAlarmService(){
        super("WeatherAlarmService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("onHandleIntent","Handle Intent called");
        stationParser();
        findLocation();
        locDistanceDifferenceList();
        Log.i("DiffListSize","" + locDistanceDiffInfo.size());
        double minValue = nearestLocationDistance();
        final String urlString = minimumIndex(minValue);
        xmlUrlParser(urlString);
        //displayList();
    }

    public void stationParser(){
        final XmlResourceParser parser = getResources().getXml(R.xml.station_lookup);
        try {
            parser.next();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (parser.getName()) {
                        case STATION_ID:{
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                //Log.i("STATION_ID", parser.getText());
                                stationInfo.add(parser.getText());
                            }
                            break;
                        }
                        case LAT:{
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                //Log.i("LAT", parser.getText());
                                latitudeInfo.add(Double.parseDouble(parser.getText()));
                            }
                            break;
                        }
                        case LONG:{
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                //Log.i("LONG", parser.getText());
                                longitudeInfo.add(Double.parseDouble(parser.getText()));
                            }
                            break;
                        }
                        case XML_URL:{
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                //Log.i("XML_URL", parser.getText());
                                xmlUrlInfo.add(parser.getText());
                            }
                            break;
                        }
                    }

                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findLocation()
    {
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission != -1){
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            deviceLat = location.getLatitude();
            deviceLng = location.getLongitude();
            printLocation(location);
        } //else if location is not permitted
    }

    public void printLocation(Location location){
        if(location != null){
            Log.i("lat","" + location.getLatitude());
            Log.i("lng","" + location.getLongitude());
        } else {
            Log.i("Location","null");
        }
    }

    public void locDistanceDifferenceList(){
        for(int i = 0; i < stationInfo.size(); ++i){
            locDistanceDiffInfo.add(calcDistance(longitudeInfo.get(i),latitudeInfo.get(i),deviceLng,deviceLat));
        }
    }

    public void locDistanceDifferenceListClear(){
        locDistanceDiffInfo.clear();
    }

    public double nearestLocationDistance(){
        double minValue = locDistanceDiffInfo.get(0);
        Log.i("Nearest distance",Collections.min(locDistanceDiffInfo).toString());
        for(int i=0; i < locDistanceDiffInfo.size(); ++i){
            if(minValue > locDistanceDiffInfo.get(i)){
                minValue = locDistanceDiffInfo.get(i);
            }
        }
        Log.i("MinDistance","" + minValue);
        return minValue;
    }

    public String minimumIndex(double minValue){
        int minIndex = 0;
        for(int i=0; i < locDistanceDiffInfo.size(); ++i){
            if(minValue == locDistanceDiffInfo.get(i)){
                minIndex = i;
            }
        }
        Log.i("minIndex","" + minIndex);
        return  xmlUrlInfo.get(minIndex);
    }

    public void xmlUrlParser(String urlX){
        StringBuilder builder = new StringBuilder();
        try {
            URL url = new URL(urlX);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            InputStream weatherData = connection.getInputStream();
            if(connection.getResponseCode() == 200){
                byte[] buffer = new byte[512];
                int nread = 0;
                while((nread = weatherData.read(buffer)) > 0){
                    builder.append(new String(buffer,0,nread));
                }
            }
            else {
                Log.i("Error","Error in connection");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i("WeatherDataXMLParsed",builder.toString());
            WeatherDataXMLParsed(builder.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double calcDistance(double rLong1, double rLat1, double rLong2, double rLat2)
    {

        rLong1 = Math.toRadians(rLong1 );
        rLong2 = Math.toRadians(rLong2 );
        rLat1 = Math.toRadians(rLat1 );
        rLat2 = Math.toRadians(rLat2 );


        double dist = 0;
        double dLong = rLong2 - rLong1;
        double dLat = rLat2 - rLat1;
        double a = Math.sin(dLat/2d) * Math.sin(dLat/2d) + Math.sin(dLong/2d) * Math.sin(dLong/2d) * Math.cos(rLat1) * Math.cos(rLat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        dist = (6371 * c) * 0.621371d;

        return dist;
    }

    public void displayList(){
        int i = 0;
        for(Double num: locDistanceDiffInfo){
            Log.i("Distance Diff","" + num + " " + ++i);
        }
    }

    public void WeatherDataXMLParsed(String xmlUrlToBeParsed) {
        XmlPullParserFactory factory = null;
        PrefSingleton.getInstance().Initialize(getApplicationContext());
        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final XmlPullParser parser = factory.newPullParser();

            parser.setInput(new StringReader(xmlUrlToBeParsed));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (parser.getName()) {
                        case TITLE: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("TITLE", parser.getText());
                                PrefSingleton.getInstance().writePreference(TITLE,parser.getText());
                            }
                            break;
                        }
                        case STATION_Id: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("STATION_Id", parser.getText());
                                PrefSingleton.getInstance().writePreference(STATION_Id,parser.getText());
                            }
                            break;
                        }
                        case OBSERVATION_TIME: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("OBSERVATION TIME", parser.getText());
                                PrefSingleton.getInstance().writePreference(OBSERVATION_TIME,parser.getText());
                            }
                            break;
                        }
                        case OBSERVATION_TIME_RFC822: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("OBSERVATION_TIME_RFC822", parser.getText());
                                PrefSingleton.getInstance().writePreference(OBSERVATION_TIME_RFC822,parser.getText());
                            }
                            break;
                        }
                        case TEMPERATURE: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("TEMPERATURE", parser.getText());
                                PrefSingleton.getInstance().writePreference(TEMPERATURE,parser.getText());
                            }
                            break;
                        }
                        case WIND: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("WIND", parser.getText());
                                PrefSingleton.getInstance().writePreference(WIND,parser.getText());
                            }
                            break;
                        }
                        case DEWPOINT_STRING: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("DEWPOINT_STRING", parser.getText());
                                PrefSingleton.getInstance().writePreference(DEWPOINT_STRING,parser.getText());
                            }
                            break;
                        }
                        case WINDCHILL: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("WINDCHILL", parser.getText());
                                PrefSingleton.getInstance().writePreference(WINDCHILL,parser.getText());
                            }
                            break;
                        }
                        case MEAN_WAVE_DIR: {
                            eventType = parser.next();
                            if (eventType == XmlPullParser.TEXT) {
                                Log.i("MEAN_WAVE_DIR", parser.getText());
                                PrefSingleton.getInstance().writePreference(MEAN_WAVE_DIR,parser.getText());
                            }
                            break;
                        }
                    }
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy","onDestroy Called");
    }
}
