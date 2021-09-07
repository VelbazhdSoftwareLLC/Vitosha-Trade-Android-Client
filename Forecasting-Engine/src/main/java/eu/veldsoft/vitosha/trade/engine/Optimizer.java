package eu.veldsoft.vitosha.trade.engine;

import java.util.List;

/**
 * Optimizer interface.
 *
 * @author Todor Balabanov
 */
public interface Optimizer {
    /**
     * Single cycle of optimization.
     *
     * @param weights Initial weights.
     * @return Weights after single cycle of optimization.
     */
    public List<Double> optimize(List<Double> weights);
}
