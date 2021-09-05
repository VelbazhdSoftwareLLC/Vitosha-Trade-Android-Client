package eu.veldsoft.vitosha.trade.engine;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.FixedElapsedTime;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.Population;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.apache.commons.math3.genetics.UniformBinaryMutation;
import org.apache.commons.math3.genetics.UniformCrossover;
import org.apache.commons.math3.genetics.WeightsChromosome;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;

/**
 * Apache Common Math Genetic Algorithms based optimizer.
 *
 * @author Todor Balabanov
 */
public class ApacheOptimizer implements Optimizer {
    /**
     *
     */
    private final long optimizationTimeout;

    /**
     *
     */
    private final BasicNetwork network;

    /**
     *
     */
    private final Propagation train;

    /**
     *
     */
    private final int populationSize;

    /**
     *
     */
    private final int tournamentArity;

    /**
     *
     */
    private final double crossoverRate;

    /**
     *
     */
    private final double mutationRate;

    /**
     *
     */
    private final double elitismRate;

    /**
     *
     * @param optimizationTimeout
     * @param network
     * @param train
     * @param populationSize
     * @param tournamentArity
     * @param crossoverRate
     * @param mutationRate
     * @param elitismRate
     */
    public ApacheOptimizer(long optimizationTimeout, BasicNetwork network, Propagation train, int populationSize, int tournamentArity, double crossoverRate, double mutationRate, double elitismRate) {
        this.optimizationTimeout = optimizationTimeout;
        this.network = network;
        this.train = train;
        this.populationSize = populationSize;
        this.tournamentArity = tournamentArity;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
        this.elitismRate = elitismRate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Double> optimize(List<Double> weights) {

        /*
         * Generate population.
         */
        List<Chromosome> chromosomes = new LinkedList<Chromosome>();
        for (int i = 0; i < populationSize; i++) {
            chromosomes.add(new WeightsChromosome(weights, true, network, train));
        }
        Population initial = new ElitisticListPopulation(chromosomes,
                2 * chromosomes.size(), elitismRate);

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

        return weights;
    }
}
