package eu.veldsoft.vitosha.trade.engine;

import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.data.market.MarketDataDescription;
import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.ml.data.market.TickerSymbol;
import org.encog.ml.data.market.loader.LoadedMarketData;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.manhattan.ManhattanPropagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.networks.training.propagation.scg.ScaledConjugateGradient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import eu.veldsoft.vitosha.trade.communication.NeuronType;
import eu.veldsoft.vitosha.trade.dummy.InputData;

/**
 * Forecasting engine main class.
 *
 * @author Todor Balabanov
 */
public class Predictor {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Pseudo-random number generator.
     */
    private static final Random PRNG = new Random();

    // TODO Put all colors in the settings dialog.

    /**
     * Colors used in the charts.
     */
    private static final int[] CHART_COLORS = {
            (95 << 24 | 0 << 16 | 255 << 8 | 0),
            (95 << 24 | 255 << 16 | 0 << 8 | 0),
    };

    /**
     * Colors used to visualize neural networks.
     */
    private static final int[] ANN_COLORS = {
            (95 << 24 | 0 << 16 | 255 << 8 | 0),
            (95 << 24 | 255 << 16 | 255 << 8 | 255),
            (95 << 24 | 0 << 16 | 0 << 8 | 255),
            (95 << 24 | 255 << 16 | 255 << 8 | 255),
            (95 << 24 | 255 << 16 | 0 << 8 | 0),
    };

    /**
     * Neural network object.
     */
    private static BasicNetwork network = new BasicNetwork();

    /**
     * Training examples data set.
     */
    private static MLDataSet examples = null;

    /**
     * Data of the forecast.
     */
    private static MLData forecast = null;

    /**
     * Calculated output data.
     */
    private static MLData output = null;

    /**
     * Lowest and highest values of particular activation function. It is used for time series scaling.
     *
     * @param activation Activation function object.
     * @return Array with two values - lowest in the first index and highest in the second index.
     */
    private static double[] findLowAndHigh(ActivationFunction activation) {
        /*
         * Use range of double values.
         */
        double[] check = {
                Double.MIN_VALUE, -0.000001, -0.00001, -0.0001,
                -0.001, -0.01, -0.1, -1, -10, -100, -1000,
                -10000, -100000, -1000000, 0, 0.000001, 0.00001,
                0.0001, 0.001, 0.01, 0.1, 1, 10, 100, 1000, 10000,
                100000, 1000000, Double.MAX_VALUE};

        /*
         * Map the range of double values to activation function output.
         */
        activation.activationFunction(check, 0, check.length);

        /*
         * Soft the result of the activation function output.
         */
        Arrays.sort(check);

        /*
         * Return minimum and maximum values of the activation function output.
         */
        return new double[]{check[0], check[check.length - 1]};
    }

    /**
     * Initialize common class members.
     */
    public void initialize() {
        Map<NeuronType, Integer> counters = new HashMap<>();
        counters.put(NeuronType.REGULAR, 0);
        counters.put(NeuronType.BIAS, 0);
        counters.put(NeuronType.INPUT, 0);
        counters.put(NeuronType.OUTPUT, 0);

        for (int type : InputData.NEURONS) {
            counters.put(NeuronType.valueOf(type),
                    counters.get(NeuronType.valueOf(type)) + 1);
        }

        int inputSize = counters.get(NeuronType.INPUT);
        int hiddenSize = counters.get(NeuronType.REGULAR);
        int outputSize = counters.get(NeuronType.OUTPUT);

        /*
         * Network construction.
         */
        network.addLayer(new BasicLayer(null,
                true, inputSize));
        network.addLayer(new BasicLayer(new ActivationTANH(),
                true, hiddenSize));
        network.addLayer(new BasicLayer(new ActivationTANH(),
                false, outputSize));
        network.getStructure().finalizeStructure();
        network.reset();

        // TODO Load weights to the network.

        double[] values = InputData.RATES[PRNG.nextInt(InputData.RATES.length)];

        /*
         * Data construction.
         */
        MarketMLDataSet data = new MarketMLDataSet(new MarketLoader() {
            @Override
            public Collection<LoadedMarketData> load(TickerSymbol symbol,
                                                     Set<MarketDataType> types, Date start,
                                                     Date end) {
                Collection<LoadedMarketData> result = new
                        ArrayList<>();

                for (int i = 0; i < InputData.TIME.length; i++) {
                    /*
                     * Data outside of the desired time frame are not loaded.
                     */
                    if (InputData.TIME[i] < start.getTime() || end.getTime() < InputData.TIME[i]) {
                        continue;
                    }

                    LoadedMarketData value = new LoadedMarketData(new
                            Date(InputData.TIME[i]), symbol);

                    value.setData(MarketDataType.CLOSE, InputData.CLOSE[i]);
                    value.setData(MarketDataType.HIGH, InputData.HIGH[i]);
                    value.setData(MarketDataType.LOW, InputData.LOW[i]);
                    value.setData(MarketDataType.OPEN, InputData.OPEN[i]);
                    value.setData(MarketDataType.VOLUME, InputData.VOLUME[i]);
                    value.setData(MarketDataType.ADJUSTED_CLOSE, InputData.CLOSE[i]);

                    result.add(value);
                }

                return result;
            }
        }, inputSize, outputSize);

        MarketDataDescription description = new MarketDataDescription(new
                TickerSymbol(InputData.SYMBOL),
                (new MarketDataType[]{MarketDataType.CLOSE, MarketDataType.HIGH,
                        MarketDataType.LOW,
                        MarketDataType.OPEN})[(int) (Math.random() * 4)],
                true, true);
        data.addDescription(description);
        data.load(new Date(InputData.TIME[0]), new
                Date(InputData.TIME[InputData.TIME.length - 1]));
        data.generate();

        /*
         * Normalize data.
         */
        double min = values[0];
        double max = values[0];
        for (double value : values) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }

        /*
         * At the first index is the low value. At the second index is the high
         * value.
         *
         * There is a problem with this approach, because some activation
         * functions are zero if the argument is infinity.
         *
         * The fist layer has no activation function.
         */
        double[] range = findLowAndHigh(network.getActivation(2));

        /*
         * Use only half of the range. It is done just to provide buffer above the central line and
         * below the central line.
         */
        double difference = Math.abs(range[1] - range[0]);
        range[0] -= difference / 2D;
        range[1] -= difference / 2D;
        range[0] /= 2D;
        range[1] /= 2D;
        range[0] += difference / 2D;
        range[1] += difference / 2D;

        /*
         * Prepare training set.
         */
        double[][] input = new double[values.length -
                (inputSize + outputSize)][inputSize];
        double[][] target = new double[values.length -
                (inputSize + outputSize)][outputSize];
        for (int i = 0; i < values.length - (inputSize + outputSize); i++) {
            for (int j = 0; j < inputSize; j++) {
                input[i][j] = range[0] + (range[1] - range[0]) *
                        (values[i + j] - min) / (max - min);
            }
            for (int j = 0; j < outputSize; j++) {
                target[i][j] = range[0] + (range[1] - range[0]) *
                        (values[i + inputSize + j] - min) / (max - min);
            }
        }
        examples = new BasicMLDataSet(input, target);

        /*
         * Prepare forecast set.
         */
        input = new double[1][inputSize];
        for (int j = 0; j < inputSize; j++) {
            input[0][j] = range[0] + (range[1] - range[0]) *
                    (values[values.length - inputSize + j] - min) / (max - min);
        }
        forecast = new BasicMLData(input[0]);
    }

    /**
     * Single neural network training cycle.
     */
    public void train() {
        if (network == null) {
            return;
        }
        if (examples == null) {
            return;
        }

        /* Select a random gradient-based training. */
        Propagation[] propagations = {
                new Backpropagation((BasicNetwork) network, examples),
                new ResilientPropagation((BasicNetwork) network, examples),
                new QuickPropagation((BasicNetwork) network, examples),
                new ScaledConjugateGradient((BasicNetwork) network, examples),
                new ManhattanPropagation((BasicNetwork) network, examples, PRNG.nextDouble())
        };


        /* Training rule object. */
        Propagation propagation = propagations[PRNG.nextInt(propagations.length)];

        /*
         * Switch between gradient methods and evolutionary methods.
         */
        switch (PRNG.nextInt(2) + 1) {
            case 1:
                propagation.iteration();
                propagation.finishTraining();
                break;
            case 2:
                int populationSize = 4 + PRNG.nextInt(33);
                double elitismRate = PRNG.nextInt(100) / 1000D;
                double crossoverRate = PRNG.nextInt(900) / 1000D;
                double mutationRate = PRNG.nextInt(10) / 1000D;
                int tournamentArity = PRNG.nextBoolean() ? 1 : 2;
                double scalingFactor = PRNG.nextInt(500) / 1000D;
                long optimizationTimeout = 60000;

                /*
                 * Obtain ANN weights.
                 */
                List<Double> weights = new ArrayList<>();
                for (int layer = 0; layer < network.getLayerCount() - 1; layer++) {
                    int bias = network.isLayerBiased(layer) ? 1 : 0;
                    for (int from = 0; from < network.getLayerNeuronCount(layer) + bias; from++) {
                        for (int to = 0; to < network.getLayerNeuronCount(layer + 1); to++) {
                            weights.add(network.getWeight(layer, from, to));
                        }
                    }
                }

                /* Do evolutionary optimization. */
                Optimizer optimizer = new MoeaOptimizer(optimizationTimeout, network, propagation, populationSize, crossoverRate, mutationRate, scalingFactor);
                weights = optimizer.optimize(weights);

                /*
                 * Replace ANN weights.
                 */
                for (int layer = 0, index = 0; layer < network.getLayerCount() - 1; layer++) {
                    int bias = network.isLayerBiased(layer) ? 1 : 0;
                    for (int from = 0; from < network.getLayerNeuronCount(layer) + bias; from++) {
                        for (int to = 0; to < network.getLayerNeuronCount(layer + 1); to++, index++) {
                            network.setWeight(layer, from, to, weights.get(index));
                        }
                    }
                }
                break;
        }
    }

    /**
     * Neural network prediction getter.
     *
     * @return Forecasted values.
     */
    public double[] predict() {
        if (network == null) {
            return new double[0];
        }
        if (forecast == null) {
            return new double[0];
        }

        output = network.compute(forecast);

        return output.getData();
    }

    /**
     * Draw forecast.
     *
     * @param pixels Array with ARGB pixels.
     * @param width  Drawing area width.
     * @param height Drawing area height.
     */
    public void drawForecast(int[] pixels, int width, int height) {
        if (network == null) {
            return;
        }
        if (forecast == null) {
            return;
        }
        if (output == null) {
            return;
        }

        /*
         * Output layer activation function is used because input layer
         * has no activation function.
         */
        double[] range = findLowAndHigh(network.getActivation(2));

        /*
         * Total number of values to be visualized.
         */
        int numberOfValues = network.getLayerNeuronCount(0) +
                network.getLayerNeuronCount(2);

        int x = 0;
        int y;

        /*
         * Visualize past data.
         */
        for (int i = 0; forecast.getData() != null &&
                i < forecast.getData().length; i++) {
            int offset = (int) (height * (forecast.getData()[i] - range[0]) /
                    (range[1] - range[0]));
            for (int dx = 0; dx < width / numberOfValues; dx++) {
                for (y = height - offset; y < height; y++) {
                    pixels[x + y * width] = CHART_COLORS[0];
                }
                x++;
            }
        }

        /*
         * Visualize future data.
         */
        for (int i = 0; output.getData() != null &&
                i < output.getData().length; i++) {
            int offset = (int) (height * (output.getData()[i] - range[0]) /
                    (range[1] - range[0]));
            for (int dx = 0; dx < width / numberOfValues; dx++) {
                for (y = height - offset; y < height; y++) {
                    pixels[x + y * width] = CHART_COLORS[1];
                }
                x++;
            }
        }
    }

    /**
     * Draw ANN topology.
     *
     * @param pixels Array with ARGB pixels.
     * @param width  Drawing area width.
     * @param height Drawing area height.
     */
    public void drawAnn(int[] pixels, int width, int height) {
        if (network == null) {
            return;
        }
        if (forecast == null) {
            return;
        }
        if (output == null) {
            return;
        }

        /*
         * Artificial neural network.
         */
        double[][] topology = {
                forecast.getData(),
                new double[network.getLayerNeuronCount(0) *
                        network.getLayerNeuronCount(1)],
                new double[network.getLayerNeuronCount(1)],
                new double[network.getLayerNeuronCount(1) *
                        network.getLayerNeuronCount(2)],
                output.getData()
        };

        /*
         * At the first index is the low value. At the second index is
         * the high value.
         *
         * There is a problem with this approach, because some activation
         * functions are zero if the argument is infinity.
         *
         * The fist layer has no activation function.
         */
        double[] range = findLowAndHigh(network.getActivation(2));

        /*
         * Scale input layer data.
         */
        for (int i = 0; i < topology[0].length; i++) {
            topology[0][i] = (topology[0][i] - range[0]) /
                    (range[1] - range[0]);
        }

        /*
         * Scale output layer data.
         */
        for (int i = 0; i < topology[4].length; i++) {
            topology[4][i] = (topology[4][i] - range[0]) /
                    (range[1] - range[0]);
        }

        for (int i = 0, from = 0, to = 0, bias = network.isLayerBiased(0) ? 1 : 0; i < topology[1].length; i++) {
            if (to >= network.getLayerNeuronCount(1)) {
                to = 0;
                from++;
            }
            if ((from + bias) >= network.getLayerNeuronCount(0)) {
                from = 0;
            }
            topology[1][i] = network.getWeight(0, from, to);
            to++;
        }

        for (int i = 0, from = 0, to = 0, bias = network.isLayerBiased(1) ? 1 : 0; i < topology[3].length; i++) {
            if (to >= network.getLayerNeuronCount(2)) {
                to = 0;
                from++;
            }
            if ((from + bias) >= network.getLayerNeuronCount(1)) {
                from = 0;
            }
            topology[3][i] = network.getWeight(1, from, to);
            to++;
        }

        /*
         * Hidden layer values. Activation function of the second layer
         * is used for scaling.
         */
        range = findLowAndHigh(network.getActivation(1));
        for (int i = 0; i < topology[2].length; i++) {
            topology[2][i] = (network.getLayerOutput(1, i) - range[0]) /
                    (range[1] - range[0]);
        }

        /*
         * Weights normalization.
         */
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double value : topology[1]) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        for (double value : topology[3]) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        for (int i = 0; i < topology[1].length; i++) {
            topology[1][i] = (topology[1][i] - min) / (max - min);
        }
        for (int i = 0; i < topology[3].length; i++) {
            topology[3][i] = (topology[3][i] - min) / (max - min);
        }

        for (int x = 0, k = 0; k < ANN_COLORS.length;
             x += width / ANN_COLORS.length, k++) {
            for (int dx = 0; dx < width / ANN_COLORS.length; dx++) {
                for (int y = 0, l = 0; y < height &&
                        l < topology[k].length; y += height / topology[k].length, l++) {
                    for (int dy = 0; dy < height / topology[k].length; dy++) {
                        int alpha = (int) (((ANN_COLORS[k] >> 24) & 0xFF) * topology[k][l]);
                        int red = (int) (((ANN_COLORS[k] >> 16) & 0xFF) * topology[k][l]);
                        int green = (int) (((ANN_COLORS[k] >> 8) & 0xFF) * topology[k][l]);
                        int blue = (int) ((ANN_COLORS[k] & 0xFF) * topology[k][l]);

                        int color = alpha << 24 | red << 16 | green << 8 | blue;

                        pixels[(x + dx) + (y + dy) * width] = color;
                    }
                }
            }
        }
    }
}
