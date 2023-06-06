package ua.tiratore.sensordata;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import ua.tiratore.sensordata.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView temperatureTextView, humidityTextView, co2TextView, coTextView;

    private Handler handler;
    private EditText IPS, PORTS;
    private Runnable dataUpdater;

    private String IP, PORT;

    //Creating the form
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        //Elements of the form
        temperatureTextView = findViewById(R.id.temperatureTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        co2TextView = findViewById(R.id.co2TextView);
        coTextView = findViewById(R.id.coTextView);
        IPS = findViewById(R.id.ipAddressEditText);
        PORTS = findViewById(R.id.portEditText);
        Button startbtn = findViewById(R.id.Startbtn);

        //Click button and run command
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IP = IPS.getText().toString();
                PORT = PORTS.getText().toString();
                handler = new Handler();
                dataUpdater = new Runnable() {
                    //Executing a command after processing each event
                    @Override
                    public void run() {
                        new ArduinoDataTask().execute();
                    }
                };
                handler.postDelayed(dataUpdater, 1000);
            }
        });
    }


    //Data processing handler and converter
    private class ArduinoDataTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... value) {

            //Amount of elements which we receive from Arduino UNO
            String[] data = new String[4];

            try {
                //IP and PORT
                String urls = "http://" + IP + ":" + PORT;
                //Read IP and PORT
                URL url = new URL(urls);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                //Request for data receiving from the server
                connection.setRequestMethod("GET");

                //Processing the data
                StringBuilder response = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                //Closing the server after processing and receiving data
                reader.close();
                connection.disconnect();

                //Parsing the received data
                String[] values = response.toString().split(",");

                if (values.length == 4) {
                    data[0] = values[0]; // Temperature
                    data[1] = values[1]; // Humidity
                    data[2] = values[2]; // CO2
                    data[3] = values[3]; // CO
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Request", "Помилка при обробці запиту: " + e.getMessage());
                data = null; //Setting data as null if error occurs
            }

            //Receiving the data on the app
            return data;
        }

        //Inserting the data to the text
        @Override
        protected void onPostExecute(String[] data) {
            if (data != null && data.length == 4) {
                temperatureTextView.setText("Температура: " + data[0] + " °C");
                humidityTextView.setText("Вологість: " + data[1] + " %");
                co2TextView.setText("CO2 прибл.: " + data[2] + " ppm");
                coTextView.setText("CO прибл.: " + data[3] + " ppm");
            } else {
                showErrorMessage();
            }
        }
    }

    //Error message if the data wasn't received
    private void showErrorMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Не вдалося отримати дані з датчиків. Будь ласка, перевірте правильність введених даних, а також що Ваш телефон та модуль ESP-01 підключені до однієї мережі.")
                .setPositiveButton("OK", null)
                .create()
                .show();
    }
}