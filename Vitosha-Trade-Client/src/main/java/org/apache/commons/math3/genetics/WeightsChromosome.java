package org.apache.commons.math3.genetics;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.Train;

import java.util.List;

/**
 * Chromosome with double values of the weights from the artificial neural
 * network.
 *
 * @author Todor Balabanov
 */
public class WeightsChromosome extends AbstractListChromosome<Double> {

    /**
     * Reference to external neural network object.
     */
    private BasicNetwork network = null;

    /**
     * Reference to external training strategy object.
     */
    private Train train = null;

    /**
     * Constructor used to initialize the chromosome with array of values.
     *
     * @param representation Values as array.
     * @param network        Neural network reference.
     * @param train          Neural network training strategy.
     * @throws InvalidRepresentationException Rise an exception if the values
     *                                        are not valid.
     */
    public WeightsChromosome(Double[] representation,
                             BasicNetwork network, Train train)
            throws InvalidRepresentationException {
        super(representation);
        this.network = network;
        this.train = train;

        if (network == null) {
            throw new RuntimeException("Neural network should be provided for the fitness evaluation.");
        }

        if (train == null) {
            throw new RuntimeException("Training object should be provided for the fitness evaluation.");
        }
    }

    /**
     * Constructor used to initialize the chromosome with list of values.
     *
     * @param representation Values as list.
     * @param network        Neural network reference.
     * @param train          Neural network training strategy.
     * @throws InvalidRepresentationException Rise an exception if the values
     *                                        are not valid.
     */
    public WeightsChromosome(List<Double> representation,
                             BasicNetwork network, Train train)
            throws InvalidRepresentationException {
        super(representation);
        this.network = network;
        this.train = train;

        if (network == null) {
            throw new RuntimeException("Neural network should be provided for the fitness evaluation.");
        }

        if (train == null) {
            throw new RuntimeException("Training object should be provided for the fitness evaluation.");
        }
    }

    /**
     * Constructor used to initialize the chromosome with list of values.
     *
     * @param representation Values as list.
     * @param copy           Deep copy flag.
     * @param network        Neural network reference.
     * @param train          Neural network training strategy.
     */
    public WeightsChromosome(List<Double> representation, boolean copy,
                             BasicNetwork network, Train train) {
        super(representation, copy);
        this.network = network;
        this.train = train;

        if (network == null) {
            throw new RuntimeException("Neural network should be provided for the fitness evaluation.");
        }

        if (train == null) {
            throw new RuntimeException("Training object should be provided for the fitness evaluation.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double fitness() {
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
        List<Double> values = getRepresentation();
        for (int layer = 0, index = 0; layer < network.getLayerCount() - 1; layer++) {
            for (int from = 0; from < network.getLayerNeuronCount(layer); from++) {
                for (int to = 0; to < network.getLayerNeuronCount(layer + 1); to++, index++) {
                    network.setWeight(layer, from, to, values.get(index));
                }
            }
        }

        /*
         * Iterate over the training set in order to calculate network error.
         */
        train.iteration();

        /*
         * Total ANN error is used as fitness value.
         */
        return train.getError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkValidity(List<Double> values)
            throws InvalidRepresentationException {
        /*
         * Length of the values should match the number of weights in the neural
         * network structure.
         */
        int counter = 0;
        for (int layer = 0; layer < network.getLayerCount() - 1; layer++) {
            for (int from = 0; from < network.getLayerNeuronCount(layer); from++) {
                for (int to = 0; to < network.getLayerNeuronCount(layer + 1); to++) {
                    counter++;
                }
            }
        }

        if (values == null || counter != values.size()) {
            // TODO Report the size problem.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractListChromosome<Double> newFixedLengthChromosome(
            List<Double> values) {
        return new WeightsChromosome(values, true, network, train);
    }

    /**
     * Chromosome representation getter.
     *
     * @return List with chromosome values.
     */
    public List<Double> getRepresentation() {
        return getRepresentation();
    }
}
