package eu.veldsoft.vitosha.trade.engine;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;
import org.moeaframework.core.variable.EncodingUtils;

import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

import io.jenetics.Mutator;
import io.jenetics.Optimize;
import io.jenetics.Phenotype;
import io.jenetics.DoubleGene;
import io.jenetics.MeanAlterer;
import io.jenetics.UniformCrossover;
import io.jenetics.engine.Limits;
import io.jenetics.engine.Codecs;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.DoubleRange;

/**
 * Jenetics Framework based optimizer.
 *
 * @author Todor Balabanov
 */
public class JeneticsOptimizer implements Optimizer {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Optimization time.
     */
    private final long optimizationTimeout;

    /**
     * Artificial nerural network reference.
     */
    private static BasicNetwork network;

    /**
     * Training rule reference.
     */
    private static Propagation propagation;

    /**
     * Evolutionary algorithm population size.
     */
    private final int populationSize;

    /**
     * Crossover rate.
     */
    private final double crossoverRate;

    /**
     * Mutation rate.
     */
    private final double mutationRate;

    /**
     * Fitness function.
     * @param weights Input vector.
     * @return Fitness value.
     */
    private static double fitness(double[] weights) {
        /*
         * Load weights from the internal representation into the network
         * structure.
         */
        for (int layer = 0, index = 0; layer < network.getLayerCount() - 1; layer++) {
            int bias = network.isLayerBiased(layer) ? 1 : 0;
            for (int from = 0; from < network.getLayerNeuronCount(layer) + bias; from++) {
                for (int to = 0; to < network.getLayerNeuronCount(layer + 1); to++, index++) {
                    network.setWeight(layer, from, to, weights[index]);
                }
            }
        }

        /*
         * Iterate over the training set in order to calculate network error.
         */
        propagation.iteration();

        /*
         * Total ANN error is used as fitness value. The bigger the fitness, the better the
         * chromosome. If the error go to zero it can lead to division by zero run-time exception.
         */
        return propagation.getError();
    }

    /**
     * Constructor with all parameters.
     *
     * @param optimizationTimeout Optimization time.
     * @param network             Artificial neural network reference.
     * @param propagation         Training rule reference.
     * @param populationSize      Evolutionary algorithm population size.
     * @param crossoverRate       Crossover rate.
     * @param mutationRate        Mutation rate.
     */
    public JeneticsOptimizer(long optimizationTimeout, BasicNetwork network, Propagation propagation, int populationSize, double crossoverRate, double mutationRate) {
        this.optimizationTimeout = optimizationTimeout;
        JeneticsOptimizer.network = network;
        JeneticsOptimizer.propagation = propagation;
        this.populationSize = populationSize;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Double> optimize(List<Double> weights) {
        //TODO Initialize population.

        Engine<DoubleGene, Double> engine = Engine
                .builder(
                        JeneticsOptimizer::fitness,
                        Codecs.ofVector(DoubleRange.of(-Double.MAX_VALUE, Double.MAX_VALUE), weights.size()))
                .populationSize(populationSize)
                .optimize(Optimize.MINIMUM)
                .alterers(
                        new Mutator<>(mutationRate),
                        new UniformCrossover<>(crossoverRate)).build();

        EvolutionStatistics<Double, ?>
                statistics = EvolutionStatistics.ofNumber();

        Phenotype<DoubleGene, Double> best = engine.stream()
                .limit(Limits.byExecutionTime(Duration.ofMillis(optimizationTimeout)))
                .peek(statistics)
                .collect(EvolutionResult.toBestPhenotype());

        //TODO Obtain best found solution.

        return weights;
    }
}
