import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import org.json.*;


public class App {

    public String convertStreamToString(InputStream in) {
        
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder("");

        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return sb.toString();
    }
    public static void main(String[] args) throws Exception {
        App app = new App();
        URL baseURL = new URL("https://api.discogs.com/artists/");

        try {
            
        URL url = new URL("https://api.discogs.com/artists/108713");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MyDiscogsClient/1.0");
        conn.setRequestProperty("Content-Type", "application/json");
        System.out.println(conn.getRequestProperties().toString());

        conn.connect();
        JSONObject json = new JSONObject(app.convertStreamToString(conn.getInputStream()));
        JSONArray members = json.getJSONArray("members");
        conn.disconnect();
        System.out.println();
        for (Object ob : members) {
            url = new URL(baseURL + Integer.valueOf(((JSONObject)ob).getInt("id")).toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            json = new JSONObject(app.convertStreamToString(conn.getInputStream()));
            System.out.println(json.toString());
        }
        

	  } catch (MalformedURLException e) {

		e.printStackTrace();

	  } catch (IOException e) {

		e.printStackTrace();

	  }
    }
}
