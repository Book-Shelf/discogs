import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Client {
    private URL baseUrl;
    private String userAgent;

    Client() {
        userAgent = "MyDiscogsClient/1.0";
        try {
            baseUrl = new URL("https://api.discogs.com/artists/");
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private HttpURLConnection setUpConnection(String artistId) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + artistId).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Content-Type", "application/json");
        return connection;
    }

    public JSONObject request(String artistId) {
        JSONObject response = null;
        HttpURLConnection connection = null;

        try {
            connection = setUpConnection(artistId);
            connection.connect();

            int responseCode = connection.getResponseCode();;
            
            while (responseCode == 429) {
                System.out.println("Waiting to send request...");

                connection.disconnect();
                Thread.sleep(60000);

                connection = setUpConnection(artistId);
                connection.connect();
                responseCode = connection.getResponseCode();
            }

            if (responseCode == 404) {
                System.err.println("There is no such artist.");
                System.exit(2);
            }

            if (responseCode != 200) {
                System.err.println("Wrong input");
                System.exit(2);
            }

            response = new JSONObject(convertStreamToString(connection.getInputStream()));

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            System.exit(3);

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(3);

        } catch (JSONException ex) {
            ex.printStackTrace();
            System.exit(3);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            connection.disconnect();
        }

        return response;
    }

    private String convertStreamToString(InputStream in) {
        
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

    public Map<Integer, String> getBandMembers(JSONObject band) {
        JSONArray members = null;
        Map<Integer, String> idToName = new HashMap<>();

        try {
            members = band.getJSONArray("members");
        } catch (JSONException ex) {
            System.err.println("Given id is not a band's id");
            System.exit(4);
        }

        for (int i = 0; i < members.length(); i++) {
            idToName.put(members.getJSONObject(i).optInt("id"), members.getJSONObject(i).getString("name"));
        }

        return idToName;
    }

    public Map<String, List<String>> getMembersPlayedTogether(Map<Integer, String> members, Integer bandId) {
        Map<String, List<String>> bandToMembers = new HashMap<>();

        for (Integer memberId : members.keySet()) {
            JSONArray response = request(Integer.toString(memberId)).getJSONArray("groups");
            for (int i = 0; i < response.length(); i++) {
                JSONObject band = response.getJSONObject(i);
                if (band.getInt("id") == bandId) continue;
                String bandName = band.getString("name");
                

                if (!bandToMembers.containsKey(bandName)) {
                    bandToMembers.put(bandName, new ArrayList<>());
                } 

                bandToMembers.get(bandName).add(members.get(memberId));
            }
        }

        bandToMembers.entrySet().removeIf(entry -> entry.getValue().size() <= 1);
        
        return bandToMembers;
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("To run a program artist's id is needed");
            System.exit(1);
        }

        int artistId = 0; 

        try {
            artistId = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            System.err.println("Artist's id should be a number");
            System.exit(1);
        }

        Client client = new Client();

        Map<Integer, String> members = client.getBandMembers(client.request(args[0]));
        Map<String, List<String>> bandToMembers = client.getMembersPlayedTogether(members, artistId);

        if (bandToMembers.isEmpty()) {
            System.out.println("Noone has played together in other band");
            System.exit(0);
        }

        bandToMembers.keySet().stream()
            .sorted()
            .forEach(bandName -> {
            System.out.format("\t%s%n", bandName);
            bandToMembers.get(bandName).forEach(System.out::println);
            System.out.println();
        });

    }

}

