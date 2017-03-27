package example.com.finalproject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity{

    public static final int WEATHER_ALARM_REQUEST_CODE = 1;

    final String TITLE = "title";
    final String STATION_Id = "station_id";
    final String OBSERVATION_TIME = "observation_time";
    final String OBSERVATION_TIME_RFC822 = "observation_time_rfc822";
    final String TEMPERATURE = "temperature_string";
    final String WIND = "wind_string";
    final String DEWPOINT_STRING = "dewpoint_string";
    final String WINDCHILL = "windchill_string";
    final String MEAN_WAVE_DIR = "mean_wave_dir";

    TextView titleText,stationIdText,observationTimeText,observationTimeRFCText,temperatureText,windText,dewpointText,windChillText,meaWaveDirText,progressInfoText;
    ProgressBar progressBar;
    TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titleText = (TextView)findViewById(R.id.title);
        stationIdText = (TextView)findViewById(R.id.station_id);
        observationTimeText = (TextView)findViewById(R.id.observation_time);
        observationTimeRFCText = (TextView)findViewById(R.id.observation_time_rfc);
        temperatureText = (TextView)findViewById(R.id.temp);
        windText = (TextView)findViewById(R.id.wind);
        dewpointText = (TextView)findViewById(R.id.dewpoint);
        windChillText = (TextView)findViewById(R.id.windchill);
        meaWaveDirText = (TextView)findViewById(R.id.mean_wave);
        progressBar = (ProgressBar)findViewById(R.id.progress);
        progressInfoText = (TextView)findViewById(R.id.progress_info);
        tableLayout = (TableLayout)findViewById(R.id.table);

        new task().execute();

    }

    public class task extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            progressInfoText.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Intent intent = new Intent(MainActivity.this,WeatherAlarmService.class);
            startService(intent);
            if(!isAlarmSet()){
                setAlarm();
            }
            else{
                Log.i("Alarm","set");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.GONE);
            progressInfoText.setVisibility(View.GONE);
            titleText.setVisibility(View.VISIBLE);
            tableLayout.setVisibility(View.VISIBLE);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("onResume","onResume Called");
        PrefSingleton.getInstance().Initialize(getApplicationContext());
        titleText.setText(PrefSingleton.getInstance().getPreference(TITLE));
        stationIdText.setText(PrefSingleton.getInstance().getPreference(STATION_Id));
        observationTimeText.setText(PrefSingleton.getInstance().getPreference(OBSERVATION_TIME));
        observationTimeRFCText.setText(PrefSingleton.getInstance().getPreference(OBSERVATION_TIME_RFC822));
        temperatureText.setText(PrefSingleton.getInstance().getPreference(TEMPERATURE));
        windText.setText(PrefSingleton.getInstance().getPreference(WIND));
        dewpointText.setText(PrefSingleton.getInstance().getPreference(DEWPOINT_STRING));
        windChillText.setText(PrefSingleton.getInstance().getPreference(WINDCHILL));
        meaWaveDirText.setText(PrefSingleton.getInstance().getPreference(MEAN_WAVE_DIR));
        //Log.i("StationId",PrefSingleton.getInstance().getPreference("station_id"));
    }

    public boolean isAlarmSet(){
        PendingIntent pendingIntent = createPendingResult(WEATHER_ALARM_REQUEST_CODE,new Intent(),PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

      public void setAlarm() {
        final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent aIntent = new Intent(this, WeatherAlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, WEATHER_ALARM_REQUEST_CODE, aIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 60);
        long frequency = 3600 * 1000;  //ms
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), frequency, pendingIntent);
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

}