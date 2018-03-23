package eu.veldsoft.vitdiscomp;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * It is used for HTTP communication with the remote server.
 *
 * @author Todor Balabanov
 */
class HttpHelper {
	/**
	 * Load random ANN remote server script name.
	 */
	private final String LOAD_RANDOM_ANN_SCRIPT = "load_random_ann.pnp";

	/**
	 * Report retrained ANN remote server script name.
	 */
	private final String REPORT_RETRAINED_ANN_SCRIPT = "rport_retrained_ann.pnp";

	/**
	 * Remote server URl address.
	 */
	private final String url;

	/**
	 * Constructor with all parameters needed.
	 *
	 * @param url Remote server URL address.
	 */
	public HttpHelper(String url) {
		this.url = url;
	}

	/*
	 * Load remote data into input data structure.
	 *
	 * @return True if the loading was successful, false otherwise.
	 */
	public boolean load() {
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(
				  "http.protocol.content-charset", "UTF-8");
		HttpPost post = new HttpPost("http://" + url + "/" + LOAD_RANDOM_ANN_SCRIPT);

		try {
			HttpResponse response = client.execute(post);

			JSONObject result = new JSONObject(EntityUtils.toString(
					  response.getEntity(), "UTF-8"));

			result.getString("");
			//TODO Read the response.
		} catch (ClientProtocolException exception) {
			return false;
		} catch (IOException exception) {
			return false;
		} catch (JSONException exception) {
			return false;
		}

		return true;
	}
}
