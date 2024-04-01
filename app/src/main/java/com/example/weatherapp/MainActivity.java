package com.example.weatherapp;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private EditText userField;
    private Button mainBtn;
    private TextView resultInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        ____________________________________________________________________________________________________
        userField = findViewById(R.id.user_field);
        mainBtn = findViewById(R.id.main_btn);
        resultInfo = findViewById(R.id.result_info);

        mainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userField.getText().toString().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.noUserInput, Toast.LENGTH_LONG).show();
                } else {
                    String city = userField.getText().toString();

                    Callable<Coords> callable = () -> getCoordsByCity(city);
                    FutureTask<Coords> future = new FutureTask<>(callable);
                    Thread thread = new Thread(future);

                    thread.start();

                    Coords coords = null;
                    try {
                        coords = future.get(10, TimeUnit.SECONDS);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }

                    String api = "f3f9bfd1bae03924e55f5b4a47302e3c";
                    String URL = "https://api.openweathermap.org/data/2.5/weather?lat=" + coords.getGeo_lat() + "&lon="
                            + coords.getGeo_lon() + "&appid=" + api + "&units=metric&lang=ru";

                    new GetWeatherData().execute(URL);
                }
            }
        });
    }

    protected Coords getCoordsByCity(String city) {
        String cityFinderToken = "cd7774643a3d0d43f92c3a7264a56539793cb0b8";
        String cityFinderSecretToken = "c2669cbc8e23b714396ad101bb357cc4441df857";
        String response = null;

        city = "[\"" + city + "\"]";

        try {
            URL cityFinderURL = new URL("https://cleaner.dadata.ru/api/v1/clean/address");
            HttpURLConnection connection = (HttpURLConnection) cityFinderURL.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Token " + cityFinderToken);
            connection.setRequestProperty("X-Secret", cityFinderSecretToken);

            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(city.getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                response = stringBuilder.substring(1, stringBuilder.length() - 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return coords(response);
    }

    protected Coords coords(String response) {

        JSONObject jsonObject = null;
        Coords coords = null;
        try {
            jsonObject = new JSONObject(response);
            coords = new Coords(jsonObject.getString("geo_lat"), jsonObject.getString("geo_lon"), jsonObject.getInt("qc_geo"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return coords;
    }

    private class GetWeatherData extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            resultInfo.setText("Получение данных...");
        }

        @Override
        protected String doInBackground(String... strings) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                return buffer.toString();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }

                if(reader != null){
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);

            try {
                JSONObject jsonObject = new JSONObject(result);

                resultInfo.setText("Температура: " + jsonObject.getJSONObject("main").getDouble("temp") + "\n" +
                        "Влажность: " + jsonObject.getJSONObject("main").getDouble("humidity") + "\n" +
                        "Давление: " + jsonObject.getJSONObject("main").getDouble("pressure") + "\n" +
                        "Скорость ветра: " +  + jsonObject.getJSONObject("wind").getDouble("speed") + "м/с" + "\n" +
                        "Подробности: " + jsonObject.getJSONArray("weather").getJSONObject(0).getString("description") + "\n");

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    }
}