package org.moeaframework.problem.misc;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

import java.util.List;

/**
 * ANN error minimization problem.
 *
 * @author Todor Balabanov
 */
public class AnnErrorMinimizationProblem extends AbstractProblem {

    /**
     *
     */
    private BasicNetwork network;

    /**
     *
     */
    private Propagation train;

    /**
     *
     */
    private List<Double> initial;

    /**
     * @param solution
     * @param network
     * @param train
     */
    public AnnErrorMinimizationProblem(List<Double> solution, BasicNetwork network, Propagation train) {
        this(solution.size(), 1);
        initial = solution;
        this.network = network;
        this.train = train;
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

        if (train == null) {
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
        train.iteration();

        /*
         * Total ANN error is used as fitness value. The bigger the fitness, the better the chromosome.
         */
        solution.setObjective(0, -train.getError());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Solution newSolution() {
        Solution solution = new Solution(initial.size(), 1, 0);
        for (int i = 0; i < initial.size(); i++) {
            solution.setVariable(i, new RealVariable(initial.get(i), -Double.MAX_VALUE + 1, Double.MAX_VALUE - 1));
        }
        return solution;
    }
}
