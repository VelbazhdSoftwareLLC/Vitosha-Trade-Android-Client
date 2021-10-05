package eu.veldsoft.vitosha.trade.engine;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.algorithm.single.AggregateObjectiveComparator;
import org.moeaframework.algorithm.single.DifferentialEvolution;
import org.moeaframework.algorithm.single.LinearDominanceComparator;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.operator.InjectedInitialization;
import org.moeaframework.core.operator.real.DifferentialEvolutionSelection;
import org.moeaframework.core.operator.real.DifferentialEvolutionVariation;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.misc.AnnErrorMinimizationProblem;

import java.util.ArrayList;
import java.util.List;

/**
 * MOEA Framework based optimizer.
 *
 * @author Todor Balabanov
 */
public class MoeaOptimizer implements Optimizer {
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
    private final Propagation train;

    /**
     * Evolutionary algorithm population size.
     */
    private final int populationSize;

    /**
     * Crossover rate.
     */
    private final double crossoverRate;

    /**
     * Differential evolution scaling factor.
     */
    private final double scalingFactor;

    /**
     * Constructor with all parameters.
     *
     * @param optimizationTimeout Optimization time.
     * @param network Artificial neural network reference.
     * @param train Training rule reference.
     * @param populationSize Evolutionary algorithm population size.
     * @param crossoverRate Crossover rate.
     * @param scalingFactor Differential evolution scaling factor.
     */
    public MoeaOptimizer(long optimizationTimeout, BasicNetwork network, Propagation train, int populationSize, double crossoverRate, double scalingFactor) {
        this.optimizationTimeout = optimizationTimeout;
        this.network = network;
        this.train = train;
        this.populationSize = populationSize;
        this.crossoverRate = crossoverRate;
        this.scalingFactor = scalingFactor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Double> optimize(List<Double> weights) {
        Problem problem = new AnnErrorMinimizationProblem(weights, network, train);

        List<Solution> solutions = new ArrayList<Solution>();
        for (int i = 0; i < populationSize; i++) {
            Solution solution = problem.newSolution();
            solutions.add(solution);
        }

        AggregateObjectiveComparator comparator = new LinearDominanceComparator();
        Initialization initialization = new InjectedInitialization(problem, populationSize, solutions);
        DifferentialEvolutionSelection selection = new DifferentialEvolutionSelection();
        DifferentialEvolutionVariation variation = new DifferentialEvolutionVariation(crossoverRate, scalingFactor);

        AbstractAlgorithm algorithm = new DifferentialEvolution(problem, comparator, initialization, selection, variation);

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
