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
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Apache Common Math Genetic Algorithms based optimizer.
 *
 * @author Todor Balabanov
 */
public class ApacheOptimizer implements Optimizer {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Optimization time.
     */
    private final long optimizationTimeout;

    /**
     * Artificial neural network reference.
     */
    private final BasicNetwork network;

    /**
     * Training rule reference.
     */
    private final Propagation propagation;

    /**
     * Evolutionary algorithm population size.
     */
    private final int populationSize;

    /**
     * Tournament selection arity.
     */
    private final int tournamentArity;

    /**
     * Crossover rate.
     */
    private final double crossoverRate;

    /**
     * Mutation rate.
     */
    private final double mutationRate;

    /**
     * Elitism rate.
     */
    private final double elitismRate;

    /**
     * Constructor with all parameters.
     *
     * @param optimizationTimeout Optimization time.
     * @param network             Artificial neural network reference.
     * @param propagation         Training rule reference.
     * @param populationSize      Evolutionary algorithm population size.
     * @param tournamentArity     Tournament selection arity.
     * @param crossoverRate       Crossover rate.
     * @param mutationRate        Mutation rate.
     * @param elitismRate         Elitism rate.
     */
    public ApacheOptimizer(long optimizationTimeout, BasicNetwork network, Propagation propagation, int populationSize, int tournamentArity, double crossoverRate, double mutationRate, double elitismRate) {
        this.optimizationTimeout = optimizationTimeout;
        this.network = network;
        this.propagation = propagation;
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
            chromosomes.add(new WeightsChromosome(weights, true, network, propagation));
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
