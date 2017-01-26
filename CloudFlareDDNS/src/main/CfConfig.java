package main;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

class CfConfig {
	private String email, key, zoneId, zoneName, wanIp;
	private ArrayList<JSONObject> zoneDnsList;
	private String webUrl = "https://api.cloudflare.com/client/v4/zones/";

	/**
	 * @return The name of the server
	 */
	String getServerName() {
		return zoneName;
	}

    /**
     * Returns stored WAN IP.
     *
     * @return Wan IP
     */
    String getWanIp() {
        return wanIp;
    }

    /**
     * Returns stored list of type A DNS records.
     *
     * @return List of type A DNS records
     */
    ArrayList<JSONObject> getDnsList() {
        return zoneDnsList;
    }

    CfConfig(String email, String key) throws Exception {
		this.email = email;
		this.key = key;

		refreshZoneId();
		refreshDnsRecords();
	}

	/**
	 * Fetches and stores DNS records from CloudFlare.
	 */
	void refreshDnsRecords() throws Exception {
		refreshWanIp();
		HttpURLConnection connection = null;

		try {
			// Connect to server
			URL url = new URL(webUrl + zoneId + "/dns_records?type=A");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("X-Auth-Email", email);
			connection.setRequestProperty("X-Auth-Key", key);
			connection.setRequestProperty("Content-Type", "application/json");

			// Get server response
			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			JSONParser parser = new JSONParser();
			JSONObject jsonObject = (JSONObject) parser.parse(br.readLine());
			JSONArray result = (JSONArray) jsonObject.get("result");
			br.close();
			zoneDnsList = new ArrayList<>();

			// Check if DNS record content matches WAN IP
			for (int i = 0; i < result.size(); i++) {
				JSONObject tempJson = (JSONObject) result.get(i);
				zoneDnsList.add(tempJson);

				if (!tempJson.get("content").equals(wanIp)) {
					updateZoneDns(tempJson);
					refreshDnsRecords();
					break;
				}
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Fetches and stores Zone details from CloudFlare.
	 */
	private void refreshZoneId() throws Exception {
		HttpURLConnection connection = null;

		try {
			// Connect to server
			URL url = new URL(webUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("X-Auth-Email", email);
			connection.setRequestProperty("X-Auth-Key", key);
			connection.setRequestProperty("Content-Type", "application/json");

			// Get server response
			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			JSONParser parser = new JSONParser();
			JSONObject jsonObject = (JSONObject) parser.parse(br.readLine());
			br.close();
			JSONArray result = (JSONArray) jsonObject.get("result");
			JSONObject zoneInfo = (JSONObject) result.get(0);
			zoneId = (String) zoneInfo.get("id");
			zoneName = (String) zoneInfo.get("name");
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Updates content of the DNS records on CloudFlare. Returns boolean based
	 * on success of API call.
	 */
	private void updateZoneDns(JSONObject data) throws Exception {
		HttpURLConnection connection = null;

		try {
			// Connect to server
			URL url = new URL(webUrl + "" + zoneId + "/dns_records/"
					+ data.get("id"));
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setRequestProperty("X-Auth-Email", email);
			connection.setRequestProperty("X-Auth-Key", key);
			connection.setRequestProperty("Content-Type", "application/json");

			data.replace("content", wanIp);

			OutputStreamWriter osw = new OutputStreamWriter(
					connection.getOutputStream());
			osw.write(data.toString());
			osw.flush();
			osw.close();

			// Get server response
			InputStream is = connection.getInputStream();
			is.close();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Fetches and stores local IP address using Amazon Web Services.
	 */
	private void refreshWanIp() throws Exception {
		HttpURLConnection connection = null;

		try {
			// Connect to server
			URL url = new URL("http://checkip.amazonaws.com/");
			connection = (HttpURLConnection) url.openConnection();

			// Get server response
			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			wanIp = br.readLine();
			br.close();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
