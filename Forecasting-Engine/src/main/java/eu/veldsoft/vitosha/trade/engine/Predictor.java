package eu.veldsoft.vitosha.trade.engine;

import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.FixedElapsedTime;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.Population;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.apache.commons.math3.genetics.UniformBinaryMutation;
import org.apache.commons.math3.genetics.UniformCrossover;
import org.apache.commons.math3.genetics.WeightsChromosome;
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
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import eu.veldsoft.vitosha.trade.communication.NeuronType;
import eu.veldsoft.vitosha.trade.dummy.InputData;

/**
 * Forecasting engine main class.
 *
 * @author Todor Balabanov
 */
public class Predictor {

    /**
     * Pseudo-random number generator.
     */
    private static final Random PRNG = new Random();

    // TODO Put all colors in the settings dialog.

    /**
     * Colors used in the charts.
     */
    private static final int CHART_COLORS[] = {
            (95 << 32 | 0 << 16 | 255 << 8 | 0),
            (95 << 32 | 255 << 16 | 0 << 8 | 0),
    };

    /**
     * Colors used to visualize neural networks.
     */
    private static final int ANN_COLORS[] = {
            (95 << 32 | 0 << 16 | 255 << 8 | 0),
            (95 << 32 | 255 << 16 | 255 << 8 | 255),
            (95 << 32 | 0 << 16 | 0 << 8 | 255),
            (95 << 32 | 255 << 16 | 255 << 8 | 255),
            (95 << 32 | 255 << 16 | 0 << 8 | 0),
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
     * Training rule object.
     */
    private static Propagation train = null;

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
        double check[] = {
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
         * Soft the result of the activation fuction output.
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
        Map<NeuronType, Integer> counters = new HashMap<NeuronType, Integer>();
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

        double values[] = InputData.RATES[PRNG.nextInt(InputData.RATES.length)];

        /*
         * Data construction.
         */
        MarketMLDataSet data = new MarketMLDataSet(new MarketLoader() {
            @Override
            public Collection<LoadedMarketData> load(TickerSymbol symbol,
                                                     Set<MarketDataType> types, Date start,
                                                     Date end) {
                Collection<LoadedMarketData> result = new
                        ArrayList<LoadedMarketData>();

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
        double range[] = findLowAndHigh(network.getActivation(2));

        /*
         * Prepare training set.
         */
        double input[][] = new double[values.length -
                (inputSize + outputSize)][inputSize];
        double target[][] = new double[values.length -
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

        train = new ResilientPropagation(network, examples);

        /*
         * Switch between backpropagation and genetic algorithm.
         */
        if (PRNG.nextBoolean() == true) {
            train.iteration();
            train.finishTraining();
        } else {
            int populationSize = 4 + PRNG.nextInt(33);
            double elitismRate = PRNG.nextInt(100) / 1000D;
            double crossoverRate = PRNG.nextInt(900) / 1000D;
            double mutationRate = PRNG.nextInt(10) / 1000D;
            int tournamentArity = PRNG.nextBoolean() ? 1 : 2;
            long optimizationTimeout = 1;

            /*
             * Obtain ANN weights.
             */
            List<Double> weights = new ArrayList<Double>();
            for (int l = 0; l < network.getLayerCount()-1; l++) {
                int bias = network.isLayerBiased(l) ? 1 : 0;
                for (int m = 0; m < network.getLayerNeuronCount(l)+bias; m++) {
                    for (int n = 0; n < network.getLayerNeuronCount(l+1); n++) {
                        weights.add(network.getWeight(l, m, n));
                    }
                }
            }

            /*
             * Generate population.
             */
            List<Chromosome> list = new LinkedList<Chromosome>();
            for (int i = 0; i < populationSize; i++) {
                list.add(new WeightsChromosome(weights, true, network, train));
            }
            Population initial = new ElitisticListPopulation(list,
                    2 * list.size(), elitismRate);

            /*
             * Initialize genetic algorithm.
             */
            GeneticAlgorithm algorithm = new GeneticAlgorithm(
                    new UniformCrossover<WeightsChromosome>(0.5),
                    crossoverRate, new UniformBinaryMutation(),
                    mutationRate, new TournamentSelection(tournamentArity));

            /*
             * Run optimization.
             */
            Population optimized = algorithm.evolve(initial,
                    new FixedElapsedTime(optimizationTimeout));

            /*
             * Obtain result.
             */
            weights = ((WeightsChromosome)
                    optimized.getFittestChromosome()).
                    getRepresentation();

            /*
             * Replace ANN weights.
             */
            for (int l = 0, k = 0; l < network.getLayerCount()-1; l++) {
                int bias = network.isLayerBiased(l) ? 1 : 0;
                for (int m = 0; m < network.getLayerNeuronCount(l)+bias; m++) {
                    for (int n = 0; n < network.getLayerNeuronCount(l+1); n++, k++) {
                        network.setWeight(l, m, n, weights.get(k));
                    }
                }
            }
        }
    }

    /**
     * Neural network prediction getter.
     */
    public void predict() {
        if (forecast == null) {
            return;
        }

        output = network.compute(forecast);
    }

    /**
     * Draw forecast.
     *
     * @param pixels Array with ARGB pixels.
     * @param width  Drawing area width.
     * @param height Drawing area height.
     */
    public void drawForecast(int[] pixels, int width, int height) {
        /*
         * Output layer activation function is used because input layer
         * has no activation function.
         */
        double range[] = findLowAndHigh(network.getActivation(2));

        /*
         * Total number of values to be visualized.
         */
        int numberOfValues = network.getLayerNeuronCount(0) +
                network.getLayerNeuronCount(2);

        int x = 0;
        int y = 0;

        /*
         * Visualize past data.
         */
        for (int i = 0; forecast.getData() != null &&
                i < forecast.getData().length; i++) {
            int offset = (int) (height * (forecast.getData()[i] - range[0]) /
                    (range[1] - range[0]));
            for (int dx = 0; dx < width / numberOfValues; dx++) {
                for(y=0; y<offset; y++) {
                    pixels[x*height + y] = CHART_COLORS[0];
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
                for(y=0; y<offset; y++) {
                    pixels[x*height + y] = CHART_COLORS[1];
                }
                x++;
            }
        }
    }

    /**
     * Draw ANN topology.
     *
     * @param pixels Array with ARGB pixels.
     * @param width Drawing area width.
     * @param height Drawing area height.
     */
    public void drawAnn(int []pixels, int width, int height) {

        /*
         * Artificial neural network.
         */
        double topology[][] = {
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
        double range[] = findLowAndHigh(network.getActivation(2));

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

        for (int i = 0, m = 0, n = 0; i < topology[1].length; i++) {
            if (n >= network.getLayerNeuronCount(1)) {
                n = 0;
                m++;
            }
            if (m >= network.getLayerNeuronCount(0)) {
                m = 0;
            }
            topology[1][i] = network.getWeight(0, m, n);
            n++;
        }

        for (int i = 0, m = 0, n = 0; i < topology[3].length; i++) {
            if (n >= network.getLayerNeuronCount(2)) {
                n = 0;
                m++;
            }
            if (m >= network.getLayerNeuronCount(1)) {
                m = 0;
            }
            topology[3][i] = network.getWeight(1, m, n);
            n++;
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
                        pixels[x*height + y] = ANN_COLORS[k];

                        int alpha = (int)(((ANN_COLORS[k]>>32) & 0xFF) * topology[k][l]);
                        int red = (int)(((ANN_COLORS[k]>>16) & 0xFF) * topology[k][l]);
                        int green = (int)(((ANN_COLORS[k]>>8) & 0xFF) * topology[k][l]);
                        int blue = (int)((ANN_COLORS[k] & 0xFF) * topology[k][l]);

                        int color = alpha << 32 | red << 16 | green << 8 | blue;

                        pixels[x*height + y] = color;
                    }
                }
            }
        }
    }
}
