package eu.veldsoft.vitdiscomp;

import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationSIN;
import org.encog.mathutil.BoundMath;

/**
 * 
 * @author Todor Balabanov
 */
class ActivationExponentRegulatedSin implements ActivationFunction {
	/**
	 * It is used as base.
	 */
	private final ActivationSIN SIN = new ActivationSIN();

	/**
	 * Should be parameter for sine period width.
	 */
	private double period = 1.0D;

	// TODO Get activation function minimum and maximum in some better way.

	/**
	 * Constructor with parameters.
	 * 
	 * @param period
	 *            Sin function period width.
	 */
	public ActivationExponentRegulatedSin(double period) {
		this.period = period;
	}

	/**
	 * y = pi * sin( x ) / exp( abs(x) )
	 *
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
	 * y' = y( x + pi/2 ) - y( x ) , x > 0 y' = y( x + pi/2 ) + y( x ) , x < 0
	 * y' = +inf , x = 0
	 *
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
		/*
		 * Return null if you do not care to be support for creating of your
		 * activation through factory.
		 */
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
