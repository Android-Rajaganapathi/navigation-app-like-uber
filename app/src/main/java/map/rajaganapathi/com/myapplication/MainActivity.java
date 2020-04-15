package map.rajaganapathi.com.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import map.rajaganapathi.com.myapplication.mapWorkouts.DirectionUtils;
import map.rajaganapathi.com.myapplication.mapWorkouts.DownloadTask;
import map.rajaganapathi.com.myapplication.mapWorkouts.PolyLineListener;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    PolyUtil polyUtil = new PolyUtil();
    private GoogleMap map;
    private Polyline mPolyline;
    private LatLng src = null, dest = null;
    private LatLng start = null, end = null;

    private FirebaseDatabase mDatabase;
    private Marker mMarker;

    private boolean usingFireBase = false;

    private LocationManager locationManager;

    private List<LatLng> polyLine = new ArrayList<>();
    private PolyLineListener mPolyLineListener = new PolyLineListener() {
        @Override
        public void whenDone(PolylineOptions output) {
            map.clear();
            mPolyline = map.addPolyline(output);
            polyLine = output.getPoints();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (LatLng latLng : polyLine) builder.include(latLng);

            map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

            mMarker = map.addMarker(new MarkerOptions().position(src)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.compass)));

            src = null;
            dest = null;
        }

        @Override
        public void whenFail(String status) {
            src = null;
            dest = null;
            System.out.println("RRR whenFail = " + status);
            switch (status) {
                case "NOT_FOUND":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "No road map available...", Toast.LENGTH_SHORT).show();
                    break;
                case "ZERO_RESULTS":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "No road map available ...", Toast.LENGTH_SHORT).show();
                    break;
                case "MAX_WAYPOINTS_EXCEEDED":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "Way point limit exceeded...", Toast.LENGTH_SHORT).show();
                    break;
                case "MAX_ROUTE_LENGTH_EXCEEDED":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "Road map limit exceeded...", Toast.LENGTH_SHORT).show();
                    break;
                case "INVALID_REQUEST":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "Invalid inputs...", Toast.LENGTH_SHORT).show();
                    break;
                case "OVER_DAILY_LIMIT":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "MayBe invalid API/Billing pending/Method Depricated...", Toast.LENGTH_SHORT).show();
                    break;
                case "OVER_QUERY_LIMIT":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "Too many request, limit exceeded...", Toast.LENGTH_SHORT).show();
                    break;
                case "REQUEST_DENIED":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "Directions service not enabled...", Toast.LENGTH_SHORT).show();
                    break;
                case "UNKNOWN_ERROR":
                    System.out.println("RRR status = " + status);
                    Toast.makeText(MainActivity.this, "Server Error...", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(MainActivity.this, status, Toast.LENGTH_SHORT).show();
                    System.out.println("RRR status = " + status);
            }
        }
    };
    private LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            System.out.println("RRR = " + "Lat: " + lat + "Lng: " + lng);


            if (start != null) end = start;
            start = new LatLng(lat, lng);

            if (end != null) {
                CarMoveAnim.carAnim(mMarker, end, start);
                polyLineRerouting(end, polyLine);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mDatabase = FirebaseDatabase.getInstance();

        getProviderLatLng();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        loadSrcDest(latLng);
                    }
                });
                reset(null);
            }
        });
    }

    private void loadSrcDest(LatLng latLng) {
        if (src == null) src = latLng;
        else {
            dest = latLng;
            new DownloadTask(mPolyLineListener).execute(new DirectionUtils()
                    .getDirectionsUrl(src, dest, getString(R.string.google_maps_key)));
        }

        Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 111) src = PlaceAutocomplete.getPlace(this, data).getLatLng();
            if (requestCode == 112) dest = PlaceAutocomplete.getPlace(this, data).getLatLng();

            if (src != null && dest != null)
                new DownloadTask(mPolyLineListener).execute(new DirectionUtils()
                        .getDirectionsUrl(src, dest, getString(R.string.google_maps_key)));
        }
    }

    public void src(View view) {
        src = null;
        try {
            Intent intent = new PlaceAutocomplete
                    .IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setFilter(new AutocompleteFilter.Builder()
                            .setCountry("IN")
                            .build())
                    .build(MainActivity.this);
            startActivityForResult(intent, 111);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    public void dest(View view) {
        dest = null;
        try {
            Intent intent = new PlaceAutocomplete
                    .IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setFilter(new AutocompleteFilter.Builder()
                            .setCountry("IN")
                            .build())
                    .build(MainActivity.this);
            startActivityForResult(intent, 112);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void getProviderLatLng() {
        if (usingFireBase) {
            mDatabase.getReference("mCurrentLocation").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange( DataSnapshot dataSnapshot) {
                    try {

                        //      HashMap<String, HashMap<String, Double>> hashMap;
                        //      hashMap = (HashMap<String, HashMap<String, Double>>) dataSnapshot.getValue();
                        //      HashMap<String, Double> doubleHashMap = hashMap.get("-LU9LbSrFDzou8i2sSgY");
                        //      start = new LatLng(doubleHashMap.get("lat"), doubleHashMap.get("lng"));

                        //      ****** or another method ******     //

                        Map<String, Object> users = (Map<String, Object>) dataSnapshot.getValue();

                        for (Map.Entry<String, Object> entry : users.entrySet()) {
                            Map singleUser = (Map) entry.getValue();
                            if (start != null) end = start;
                            start = new LatLng((Double) singleUser.get("lat"), (Double) singleUser.get("lng"));
                        }

                        if (end != null) {
                            CarMoveAnim.carAnim(mMarker, end, start);
                            polyLineRerouting(end, polyLine);
                        }

                        System.out.println("RRR start = " + start);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.w("RRR ", "Failed to read value.", error.toException());
                }
            });
        } else btFetchLocation();
    }

    private void polyLineRerouting(LatLng point, List<LatLng> polyLine) {
        System.out.println("----->     MainActivity.polyLineRerouting     <-----");
        System.out.println("RRR containsLocation = " + polyUtil.containsLocation(point, polyLine, true));
        System.out.println("RRR isLocationOnEdge = " + polyUtil.isLocationOnEdge(point, polyLine, true, 1));
        System.out.println("RRR locationIndexOnPath = " + polyUtil.locationIndexOnPath(point, polyLine, true, 1));
        System.out.println("RRR locationIndexOnEdgeOrPath = " + polyUtil.locationIndexOnEdgeOrPath(point, polyLine, false, true, 1));

        int index = polyUtil.locationIndexOnEdgeOrPath(point, polyLine, false, true, 1);
        if (index > 0) {
            polyLine.subList(0, index + 1).clear();
            polyLine.add(0, point);
            mPolyline.remove();
            PolylineOptions options = new PolylineOptions();
            options.addAll(polyLine);
            mPolyline = map.addPolyline(options);
            System.out.println("RRR mPolyline = " + polyLine.size());
        } else System.out.println("RRR mPolyline = Failed");
    }

    public void btFetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]
                    {ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 112);
        else {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        2000, 10, locationListenerGPS);
            else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        2000, 10, locationListenerGPS);
            else try {
                    throw (new Exception("No location provider is available!"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    private void isLocationEnabled() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Enable Location");
            alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
            alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 112) if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            btFetchLocation();
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
        } else Toast.makeText(this, "Permission(s) missing", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLocationEnabled();
    }

    public void reset(View view) {
        src = new LatLng(13.058465, 80.253731);
        dest = new LatLng(13.040558, 80.233591);

        new DownloadTask(mPolyLineListener).execute(new DirectionUtils()
                .getDirectionsUrl(src, dest, getString(R.string.google_maps_key)));
    }
}