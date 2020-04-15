package map.rajaganapathi.com.myapplication.mapWorkouts;

import com.google.android.gms.maps.model.PolylineOptions;

public interface PolyLineListener {

    void whenDone(PolylineOptions output);

    void whenFail(String statusCode);
}