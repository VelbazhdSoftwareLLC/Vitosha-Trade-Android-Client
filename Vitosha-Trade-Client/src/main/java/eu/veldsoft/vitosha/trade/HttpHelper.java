package eu.veldsoft.vitosha.trade;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * It is used for HTTP communication with the remote server.
 *
 * @author Todor Balabanov
 */
class HttpHelper {
    /**
     * Load random ANN remote server script name.
     */
    private final String LOAD_RANDOM_ANN_SCRIPT = "logic/json_load_random_ann.php";

    /**
     * Loading training set for particular ticker and time period.
     */
    private final String LOAD_TRAINING_SET_SCRIPT = "logic/json_load_training_set.php";

    /**
     * Report retrained ANN remote server script name.
     */
    private final String SAVE_RETRAINED_ANN_SCRIPT = "logic/save_retrained_ann.php";

    /**
     * Number of ANN found as response of the request.
     */
    private static final String JSON_SIZE_KEY = "size";

    /**
     * Time series symbol ticker.
     */
    private static final String JSON_SYMBOL_KEY = "symbol";

    /**
     * Time series period as integer number of minutes.
     */
    private static final String JSON_PERIOD_KEY = "period";

    /**
     * Fitness value of the ANN.
     */
    private static final String JSON_FITNESS_KEY = "fitness";

    /**
     * Array with neurons flags.
     */
    private static final String JSON_FLAGS_KEY = "flags";

    /**
     * Array with ANN weights.
     */
    private static final String JSON_WEIGHTS_KEY = "weights";

    /**
     * Array with ANN connections activities.
     */
    private static final String JSON_ACTIVITIES_KEY = "activities";

    /**
     * Number of neurons for the ANN.
     */
    private final String JSON_NUMBER_OF_NEURONS_KEY = "numberOfNeurons";

    /**
     * Number of training examples.
     */
    private static final String JSON_NUMBER_OF_EXAMPLES_KEY = "numberOfExamples";

    /**
     * Time array.
     */
    private static final String JSON_TIME_KEY = "time";

    /**
     * Open array.
     */
    private static final String JSON_OPEN_KEY = "open";

    /**
     * Low array.
     */
    private static final String JSON_LOW_KEY = "low";

    /**
     * High array.
     */
    private static final String JSON_HIGH_KEY = "high";

    /**
     * Close array.
     */
    private static final String JSON_CLOSE_KEY = "close";

    /**
     * Volume array.
     */
    private static final String JSON_VOLUME_KEY = "volume";

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
        String symbol = InputData.SYMBOL;
        int period = InputData.PERIOD;
        int[] flags = InputData.NEURONS;
        double[][] weights = InputData.WEIGHTS;
        double[][] activities = InputData.ACTIVITIES;
        long[] time = InputData.TIME;
        double[] open = InputData.OPEN;
        double[] low = InputData.LOW;
        double[] high = InputData.HIGH;
        double[] close = InputData.HIGH;
        double[] volume = InputData.VOLUME;

        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter("http.protocol.content-charset", "UTF-8");

        /*
         * Load randomly selected ANN.
         */
        HttpPost post = new HttpPost("http://" + url.trim() + "/" + LOAD_RANDOM_ANN_SCRIPT);

        try {
            HttpResponse response = client.execute(post);

            JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));

            int size = result.getInt(JSON_SIZE_KEY);

            /*
             * If there is no ANN on the server side nothing can be loaded.
             */
            if (size <= 0) {
                return false;
            }

            /*
             * Extract JSON from HTTP response.
             */
            symbol = result.getString(JSON_SYMBOL_KEY);

            period = result.getInt(JSON_PERIOD_KEY);

            double fitness = result.getDouble(JSON_FITNESS_KEY);

            int numberOfNeurons = result.getInt(JSON_NUMBER_OF_NEURONS_KEY);

            flags = new int[numberOfNeurons];
            JSONArray array1 = result.getJSONArray(JSON_FLAGS_KEY);
            for (int i = 0; i < array1.length(); i++) {
                flags[i] = array1.getInt(i);
            }

            //TODO Matrix transpose is possible.
            weights = new double[numberOfNeurons][numberOfNeurons];
            JSONArray array2 = result.getJSONArray(JSON_WEIGHTS_KEY);
            for (int j = 0; j < array2.length(); j++) {
                JSONArray array3 = array2.getJSONArray(j);
                for (int i = 0; i < array3.length(); i++) {
                    weights[i][j] = array3.getDouble(i);
                }
            }

            //TODO Matrix transpose is possible.
            activities = new double[numberOfNeurons][numberOfNeurons];
            JSONArray array4 = result.getJSONArray(JSON_ACTIVITIES_KEY);
            for (int j = 0; j < array4.length(); j++) {
                JSONArray array5 = array4.getJSONArray(j);
                for (int i = 0; i < array5.length(); i++) {
                    activities[i][j] = array5.getDouble(i);
                }
            }
        } catch (ClientProtocolException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        } catch (JSONException exception) {
            return false;
        } catch (Exception exception) {
            return false;
        }

        /*
         * Load training set for the selected ANN.
         */
        post = new HttpPost("http://" + url.trim() + "/" + LOAD_TRAINING_SET_SCRIPT);
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("symbol", symbol));
        pairs.add(new BasicNameValuePair("period", "" + period));
        try {
            post.setEntity(new UrlEncodedFormEntity(pairs));
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        try {
            HttpResponse response = client.execute(post);

            JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));

            int size = result.getInt(JSON_NUMBER_OF_EXAMPLES_KEY);

            /*
             * If there is no ANN training set on the server side nothing can be loaded.
             */
            if (size <= 0) {
                return false;
            }

            time = new long[size];
            JSONArray array1 = result.getJSONArray(JSON_TIME_KEY);
            for (int i = 0; i < array1.length(); i++) {
                time[i] = array1.getLong(i);
            }

            open = new double[size];
            JSONArray array2 = result.getJSONArray(JSON_OPEN_KEY);
            for (int i = 0; i < array2.length(); i++) {
                open[i] = array2.getDouble(i);
            }

            low = new double[size];
            JSONArray array3 = result.getJSONArray(JSON_LOW_KEY);
            for (int i = 0; i < array3.length(); i++) {
                low[i] = array3.getDouble(i);
            }

            high = new double[size];
            JSONArray array4 = result.getJSONArray(JSON_HIGH_KEY);
            for (int i = 0; i < array4.length(); i++) {
                high[i] = array4.getDouble(i);
            }

            close = new double[size];
            JSONArray array5 = result.getJSONArray(JSON_CLOSE_KEY);
            for (int i = 0; i < array5.length(); i++) {
                close[i] = array5.getDouble(i);
            }

            volume = new double[size];
            JSONArray array6 = result.getJSONArray(JSON_VOLUME_KEY);
            for (int i = 0; i < array6.length(); i++) {
                volume[i] = array6.getDouble(i);
            }
        } catch (ClientProtocolException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        } catch (JSONException exception) {
            return false;
        } catch (Exception exception) {
            return false;
        }

        /*
         * Load data in the global data structure.
         */
        InputData.SYMBOL = symbol;
        InputData.PERIOD = period;
        InputData.NEURONS = flags;
        InputData.WEIGHTS = weights;
        InputData.ACTIVITIES = activities;
        InputData.TIME = time;
        InputData.OPEN = open;
        InputData.LOW = low;
        InputData.HIGH = high;
        InputData.CLOSE = close;
        InputData.VOLUME = volume;
        InputData.RATES = new double[][]{InputData.OPEN, InputData.LOW, InputData.HIGH, InputData.CLOSE};

        return true;
    }

    /**
     * Store calculated ANN weights on the remote web server.
     *
     * @return True if the saving was successful, false otherwise.
     */
    public boolean store() {
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter("http.protocol.content-charset", "UTF-8");

        /*
         * Store retrained ANN.
         */
        HttpPost post = new HttpPost("http://" + url.trim() + "/" + SAVE_RETRAINED_ANN_SCRIPT);
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair("symbol", InputData.SYMBOL));
        pairs.add(new BasicNameValuePair("period", "" + InputData.PERIOD));
        pairs.add(new BasicNameValuePair("fitness", "" + InputData.FITNESS));
        pairs.add(new BasicNameValuePair("number_of_neurons", "" + InputData.NEURONS.length));

        String flags = "";
        for (int i = 0; i < InputData.NEURONS.length; i++) {
            flags += InputData.NEURONS[i] + " ";
        }
        pairs.add(new BasicNameValuePair("flags", "" + flags.trim()));

        //TODO Matrix transpose is possible.
        String weights = "";
        for (int j = 0; j < InputData.NEURONS.length; j++) {
            for (int i = 0; i < InputData.NEURONS.length; i++) {
                weights += InputData.WEIGHTS[i][j] + " ";
            }
            weights = weights.trim() + "\r\n";
        }
        pairs.add(new BasicNameValuePair("weights", "" + weights.trim()));

        //TODO Matrix transpose is possible.
        String activites = "";
        for (int j = 0; j < InputData.NEURONS.length; j++) {
            for (int i = 0; i < InputData.NEURONS.length; i++) {
                activites += InputData.ACTIVITIES[i][j] + " ";
            }
            activites = activites.trim() + "\r\n";
        }
        pairs.add(new BasicNameValuePair("activites", "" + activites.trim()));

        try {
            post.setEntity(new UrlEncodedFormEntity(pairs));
            client.execute(post);
        } catch (UnsupportedEncodingException exception) {
            return false;
        } catch (ClientProtocolException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        }

        return true;
    }
}
