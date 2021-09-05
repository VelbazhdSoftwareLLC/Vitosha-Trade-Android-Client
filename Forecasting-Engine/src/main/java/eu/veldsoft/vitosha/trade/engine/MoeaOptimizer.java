package eu.veldsoft.vitosha.trade.engine;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;

import java.util.List;

/**
 * MOEA Framework based optimizer.
 *
 * @author Todor Balabanov
 */
public class MoeaOptimizer implements Optimizer {
    /**
     *
     */
    public MoeaOptimizer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Double> optimize(List<Double> weights) {
        return weights;
    }
}
