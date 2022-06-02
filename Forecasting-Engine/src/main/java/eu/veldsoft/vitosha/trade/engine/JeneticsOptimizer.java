package eu.veldsoft.vitosha.trade.engine;

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

    private static double fitness(double[] x) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Double> optimize(List<Double> weights) {
        Engine<DoubleGene, Double> engine = Engine
                .builder(
                        JeneticsOptimizer::fitness,
                        Codecs.ofVector(DoubleRange.of(-Double.MAX_VALUE, Double.MAX_VALUE), weights.size()))
                .populationSize(500)
                .optimize(Optimize.MINIMUM)
                .alterers(
                        new Mutator<>(0.01),
                        new UniformCrossover<>(0.5)).build();

        EvolutionStatistics<Double, ?>
                statistics = EvolutionStatistics.ofNumber();

        Phenotype<DoubleGene, Double> best = engine.stream()
                .limit(Limits.bySteadyFitness(1))
                .peek(statistics)
                .collect(EvolutionResult.toBestPhenotype());

        return weights;
    }
}
