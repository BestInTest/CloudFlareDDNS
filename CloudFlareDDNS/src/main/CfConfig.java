package main;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CfConfig {
	private String email, key, zoneId, zoneName, wanIp;
	private ArrayList<JSONObject> zoneDnsList;
	private String webUrl = "https://api.cloudflare.com/client/v4/zones/";
	private int maxRetries = 2;

	public String getServerName() {
		return zoneName;
	}

	public CfConfig(String email, String key) {
		this.email = email;
		this.key = key;

		refreshZoneId();
		refreshDnsRecords();
	}

	public ArrayList<JSONObject> getDnsList() {
		return zoneDnsList;
	}

	/**
	 * Fetches and stores DNS records from CloudFlare.
	 */
	public void refreshDnsRecords() {
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
			zoneDnsList = new ArrayList<JSONObject>();

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
		} catch (Exception e) {
			e.printStackTrace();

			if (maxRetries > 0) {
				refreshDnsRecords();
				maxRetries--;
			} else {
				System.exit(1);
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
	private void refreshZoneId() {
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

		} catch (Exception e) {
			e.printStackTrace();

			if (maxRetries > 0) {
				refreshZoneId();
				maxRetries--;
			} else {
				System.exit(1);
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Updates content of the DNS records on CloudFlare. Returns boolean based
	 * on success of API call.
	 * 
	 * @return success of API call
	 */
	private void updateZoneDns(JSONObject data) {
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

		} catch (Exception e) {
			e.printStackTrace();

			if (maxRetries > 0) {
				updateZoneDns(data);
				maxRetries--;
			} else {
				System.exit(1);
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Fetches and stores local IP address using Amazon Web Services.
	 */
	private void refreshWanIp() {
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
		} catch (Exception e) {
			e.printStackTrace();

			if (maxRetries > 0) {
				refreshDnsRecords();
				maxRetries--;
			} else {
				System.exit(1);
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
