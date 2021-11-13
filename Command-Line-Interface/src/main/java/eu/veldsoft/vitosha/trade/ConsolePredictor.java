package eu.veldsoft.vitosha.trade;

import java.util.Arrays;
import java.util.logging.Logger;

import eu.veldsoft.vitosha.trade.engine.Predictor;

/**
 * Single entry point class for command line application interface.
 */
public class ConsolePredictor {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Application single entry point method.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        LOGGER.info("");

        Predictor predictor = new Predictor();
        predictor.initialize();
        predictor.train();
        double[] forecast = predictor.predict();

        //System.out.println(Arrays.toString(forecast) );

        System.exit(0);
    }
}
