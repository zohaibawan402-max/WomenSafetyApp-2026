package com.example.womensafety;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private EditText etContact;
    private Button btnSave, btnSos;
    private TextView tvStatus;
    
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "SafetyPrefs";
    private static final String KEY_CONTACT = "trusted_contact";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accelerationCurrentValue;
    private float accelerationLastValue;
    private float shake;

    private String currentLat = "31.5204"; 
    private String currentLong = "74.3587";
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etContact = findViewById(R.id.etContact);
        btnSave = findViewById(R.id.btnSave);
        btnSos = findViewById(R.id.btnSos);
        tvStatus = findViewById(R.id.tvStatus);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        String savedContact = sharedPreferences.getString(KEY_CONTACT, "");
        if (!savedContact.isEmpty()) {
            etContact.setText(savedContact);
        }

        checkPermissions();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        
        shake = 0.00f;
        accelerationCurrentValue = SensorManager.GRAVITY_EARTH;
        accelerationLastValue = SensorManager.GRAVITY_EARTH;

        setupLocationTracking();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contact = etContact.getText().toString().trim();
                if (!contact.isEmpty()) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_CONTACT, contact);
                    editor.apply();
                    Toast.makeText(MainActivity.this, "Contact Saved Successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerEmergency();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    private void setupLocationTracking() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    currentLat = String.valueOf(location.getLatitude());
                    currentLong = String.valueOf(location.getLongitude());
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(@NonNull String provider) {}
                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            });
        }
    }

    private void triggerEmergency() {
        String contactNumber = sharedPreferences.getString(KEY_CONTACT, "");
        if (contactNumber.isEmpty()) {
            Toast.makeText(this, "Please save an emergency contact first!", Toast.LENGTH_LONG).show();
            return;
        }

        String googleMapsLink = "https://maps.google.com/?q=" + currentLat + "," + currentLong;
        String message = "EMERGENCY! I need help. My current location is: " + googleMapsLink;

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(contactNumber, null, message, null, null);
            tvStatus.setText("Status: SOS Alert Sent!");
            Toast.makeText(this, "Emergency SMS Sent to " + contactNumber, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accelerationLastValue = accelerationCurrentValue;
            accelerationCurrentValue = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = accelerationCurrentValue - accelerationLastValue;
            shake = shake * 0.9f + delta;

            if (shake > 12) {
                triggerEmergency();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}
