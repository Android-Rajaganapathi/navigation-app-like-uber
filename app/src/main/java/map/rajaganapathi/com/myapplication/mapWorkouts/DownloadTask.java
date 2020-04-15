package map.rajaganapathi.com.myapplication.mapWorkouts;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

public class DownloadTask extends AsyncTask<String, Void, String> {

    private PolyLineListener mPolyLineListener;

    public DownloadTask(PolyLineListener mPolyLineListener) {
        this.mPolyLineListener = mPolyLineListener;
    }

    @Override
    protected String doInBackground(String... url) {
        String data = "";
        try {
            data = new DirectionUtils().downloadUrl(url[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try {
            JSONObject object = new JSONObject(result);
            if (object.has("error_message")) {
                mPolyLineListener.whenFail(object.optString("error_message"));
            } else if (object.optString("status").equals("OK"))
                new ParserTask(mPolyLineListener).execute(result);
            else mPolyLineListener.whenFail(object.optString("status"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}