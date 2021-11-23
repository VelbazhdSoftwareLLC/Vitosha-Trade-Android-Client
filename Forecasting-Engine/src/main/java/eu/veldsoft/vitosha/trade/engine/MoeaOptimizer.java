package eu.veldsoft.vitosha.trade.engine;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.algorithm.single.AggregateObjectiveComparator;
import org.moeaframework.algorithm.single.DifferentialEvolution;
import org.moeaframework.algorithm.single.EvolutionStrategy;
import org.moeaframework.algorithm.single.GeneticAlgorithm;
import org.moeaframework.algorithm.single.LinearDominanceComparator;
import org.moeaframework.algorithm.single.SelfAdaptiveNormalVariation;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Selection;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.operator.GAVariation;
import org.moeaframework.core.operator.InjectedInitialization;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.UniformCrossover;
import org.moeaframework.core.operator.UniformSelection;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.core.operator.binary.HUX;
import org.moeaframework.core.operator.real.DifferentialEvolutionSelection;
import org.moeaframework.core.operator.real.DifferentialEvolutionVariation;
import org.moeaframework.core.operator.real.UM;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.misc.AnnErrorMinimizationProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * MOEA Framework based optimizer.
 *
 * @author Todor Balabanov
 */
public class MoeaOptimizer implements Optimizer {
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
     * Crossover rate.
     */
    private final double crossoverRate;

    /**
     * Mutation rate.
     */
    private final double mutationRate;

    /**
     * Differential evolution scaling factor.
     */
    private final double scalingFactor;

    /**
     * Constructor with all parameters.
     *
     * @param optimizationTimeout Optimization time.
     * @param network             Artificial neural network reference.
     * @param propagation         Training rule reference.
     * @param populationSize      Evolutionary algorithm population size.
     * @param crossoverRate       Crossover rate.
     * @param mutationRate        Mutation rate.
     * @param scalingFactor       Differential evolution scaling factor.
     */
    public MoeaOptimizer(long optimizationTimeout, BasicNetwork network, Propagation propagation, int populationSize, double crossoverRate, double mutationRate, double scalingFactor) {
        this.optimizationTimeout = optimizationTimeout;
        this.network = network;
        this.propagation = propagation;
        this.populationSize = populationSize;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
        this.scalingFactor = scalingFactor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Double> optimize(List<Double> weights) {
        Problem problem = new AnnErrorMinimizationProblem(weights, network, propagation);

        List<Solution> solutions = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            Solution solution = problem.newSolution();
            solutions.add(solution);
        }

        AggregateObjectiveComparator comparator = new LinearDominanceComparator();
        Initialization initialization = new InjectedInitialization(problem, populationSize, solutions);

        /* Select a random evolutionary optimizer. */
        Selection[] selections = {new TournamentSelection(), new UniformSelection(), new DifferentialEvolutionSelection(),};
        Variation[] crossovers = {new UniformCrossover(crossoverRate), new HUX(crossoverRate),};
        Variation[] mutations = {new BitFlip(mutationRate), new UM(mutationRate),};
        AbstractAlgorithm[] algorithms = {
                new EvolutionStrategy(problem, comparator, initialization,
                        new SelfAdaptiveNormalVariation()),
                new GeneticAlgorithm(problem, comparator, initialization,
                        selections[PRNG.nextInt(selections.length)],
                        new GAVariation(crossovers[PRNG.nextInt(crossovers.length)], mutations[PRNG.nextInt(mutations.length)])),
                new DifferentialEvolution(problem, comparator, initialization,
                        new DifferentialEvolutionSelection(),
                        new DifferentialEvolutionVariation(crossoverRate, scalingFactor))
        };
        AbstractAlgorithm algorithm = algorithms[PRNG.nextInt(algorithms.length)];

        long stop = System.currentTimeMillis() + optimizationTimeout;
        while (System.currentTimeMillis() < stop) {
            algorithm.step();
        }

        weights = new ArrayList<Double>();
        NondominatedPopulation population = algorithm.getResult();
        if (population.size() > 0) {
            for (Double value : EncodingUtils.getReal(population.get(0))) {
                weights.add(value);
            }
        }

        return weights;
    }
}
