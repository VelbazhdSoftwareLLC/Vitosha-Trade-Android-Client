package org.moeaframework.problem.misc;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * ANN error minimization problem.
 *
 * @author Todor Balabanov
 */
public class AnnErrorMinimizationProblem extends AbstractProblem {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Pseudo-random number generator.
     */
    private static final Random PRNG = new Random();

    /**
     * Artificial neural network reference.
     */
    private BasicNetwork network;

    /**
     * Training rule reference.
     */
    private Propagation propagation;

    /**
     * Initial values for artificial neural network weights.
     */
    private List<Double> initial;

    /**
     * Constructor with all parameters.
     *
     * @param solution    Initial weights.
     * @param network     Artificial neural network reference.
     * @param propagation Training rule reference.
     */
    public AnnErrorMinimizationProblem(List<Double> solution, BasicNetwork network, Propagation propagation) {
        this(solution.size(), 1);
        initial = solution;
        this.network = network;
        this.propagation = propagation;
    }

    /**
     * {@inheritDoc}
     */
    private AnnErrorMinimizationProblem(int numberOfVariables, int numberOfObjectives) {
        super(numberOfVariables, numberOfObjectives);
    }

    /**
     * {@inheritDoc}
     */
    private AnnErrorMinimizationProblem(int numberOfVariables, int numberOfObjectives, int numberOfConstraints) {
        super(numberOfVariables, numberOfObjectives, numberOfConstraints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evaluate(Solution solution) {
        if (network == null) {
            throw new RuntimeException("Neural network should be provided for the fitness evaluation.");
        }

        if (propagation == null) {
            throw new RuntimeException("Training object should be provided for the fitness evaluation.");
        }

        /*
         * Load weights from the internal representation into the network
         * structure.
         */
        double[] weights = EncodingUtils.getReal(solution);
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
        solution.setObjective(0, propagation.getError());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Solution newSolution() {
        Solution solution = new Solution(initial.size(), 1, 0);
        for (int i = 0; i < initial.size(); i++) {
            /* Without random noise, some of the population-based optimizers do not perform at all. */
            solution.setVariable(i, new RealVariable(initial.get(i) + PRNG.nextDouble() - 0.5D, -(Double.MAX_VALUE / 2D), +(Double.MAX_VALUE / 2D)));
        }
        return solution;
    }
}
