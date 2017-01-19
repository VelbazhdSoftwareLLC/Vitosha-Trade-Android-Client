package eu.veldsoft.vitdiscomp;

import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationSIN;
import org.encog.mathutil.BoundMath;

/**
 * 
 * @author Todor Balabanov
 */
class ActivationFadingSin implements ActivationFunction {
	/**
	 * It is used as base.
	 */
	private final ActivationSIN SIN = new ActivationSIN();

	/**
	 * Should be parameter for sine period width.
	 */
	private double period = 1.0D;

	/**
	 * Constructor with parameters.
	 * 
	 * @param period
	 *            Sin function period width.
	 */
	public ActivationFadingSin(double period) {
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

		if (x < -Math.PI || x > Math.PI) {
			return BoundMath.cos(x) / Math.abs(x) - BoundMath.sin(x) / (x * Math.abs(x));
		} else {
			return BoundMath.cos(x);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivationFunction clone() {
		try {
			return (ActivationFunction) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}

		return null;
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
