package org.encog.engine.network.activation;

import org.encog.mathutil.BoundMath;

/**
 * Exponent regulated sin activation function.
 *
 * @author Todor Balabanov
 */
public class ActivationExponentRegulatedSin implements ActivationFunction {
    /**
     * Default lowest value.
     */
    static final double LOW = -0.99;
    /**
     * Default highest value.
     */
    static final double HIGH = +0.99;
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
    public ActivationExponentRegulatedSin(double period) {
        this.period = period;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activationFunction(double[] values, int start, int size) {
        for (int i = start; i < (start + size) && i < values.length; i++) {
            double x = values[i] / period;

            values[i] = Math.PI * BoundMath.sin(x) / BoundMath.exp(Math.abs(x));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double derivativeFunction(double before, double after) {
        double x = before / period;

        if (x == 0) {
            return Double.MAX_VALUE;
        }

        return Math.PI * BoundMath.exp(-Math.abs(x)) * (BoundMath.cos(x) * Math.abs(x) - x * BoundMath.sin(x))
                / Math.abs(x);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivationFunction clone() {
        return new ActivationExponentRegulatedSin(period);
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
