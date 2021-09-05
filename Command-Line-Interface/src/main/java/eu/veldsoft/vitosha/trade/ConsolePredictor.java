package eu.veldsoft.vitosha.trade;

import eu.veldsoft.vitosha.trade.dummy.InputData;
import eu.veldsoft.vitosha.trade.engine.Predictor;

/**
 * Single entry point class for command line application interface.
 */
public class ConsolePredictor {
    public static void main(String[] args) {
        Predictor predictor = new Predictor();
        predictor.initialize();
        predictor.train();
        predictor.predict();
    }
}
