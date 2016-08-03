package main;

import org.json.simple.JSONObject;

/**
 * @author Austin Lautissier
 *
 */
public class Main {
	private static String email = "";
	private static String key = "";

	public static void main(String[] args) {
		System.out.println("Fetching details from CloudFlare...");
		CfConfig config = new CfConfig(email, key);
		System.out.println("Records found for " + config.getServerName());

		for (JSONObject dnsJson : config.getDnsList()) {
			System.out.println(dnsJson.get("name") + " " + dnsJson.get("type")
					+ " " + dnsJson.get("content"));
		}

		// Seconds to wait between refresh of DNS records
		int secsToRefresh = 3600;

		// Infinite loop to refresh DNS records
		while (true) {
			try {
				System.out.println("Finished. Refreshing again in "
						+ secsToRefresh + " seconds");
				Thread.sleep(secsToRefresh * 1000);
				System.out.println("Refreshing records...");
				config.refreshDnsRecords();
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				System.out.println("Sleep interupted");
			}
		}
	}
}
