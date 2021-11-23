package org.encog.engine.network.activation;

import org.encog.mathutil.BoundMath;

/**
 * The alternative first derivative.
 *
 * @author Todor Balabanov
 */
public class ActivationFadingSin2 implements ActivationFunction {

    /**
     * Original sin activation function.
     */
    private final ActivationSIN SIN = new ActivationSIN();

    /**
     * Sin function period.
     */
    private double period = 1.0D;

    /**
     * Constructor with parameters.
     *
     * @param period Sin function period.
     */
    public ActivationFadingSin2(double period) {
        this.period = period;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activationFunction(double[] values, int start, int size) {
        for (int i = start; i < (start + size) && i < values.length; i++) {
            double x = values[i] / period;

            if (x < -Math.PI || x > Math.PI) {
                values[i] = BoundMath.sin(x) / Math.abs(x);
            } else {
                values[i] = BoundMath.sin(x);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double derivativeFunction(double before, double after) {
        double x = before / period;
        return BoundMath.exp(-(x * x));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivationFunction clone() {
        return new ActivationFadingSin1(period);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFactoryCode() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getParamNames() {
        return SIN.getParamNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParams() {
        return SIN.getParams();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDerivative() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParam(int index, double value) {
        SIN.setParam(index, value);
    }
}