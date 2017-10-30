package eu.veldsoft.vitdiscomp;

/**
 * @author Todor Balabanov
 */
final class NeuronType {
    /**
     * Regular neuron flag.
     */
    final static int REGULAR = 0x00;

    /**
     * Bias neuron flag.
     */
    final static int BIAS = 0x01;

    /**
     * Input neuron flag.
     */
    final static int INPUT = 0x02;

    /**
     * Input and bias neuron flag.
     */
    final static int INPUT_BIAS = 0x03;

    /**
     * Output neuron flag.
     */
    final static int OUTPUT = 0x04;

    /**
     * Output and bias neuron flag.
     */
    final static int OUTPUT_BIAS = 0x05;

    /**
     * Output and input neuron flag.
     */
    final static int OUTPUT_INPUT = 0x06;

    /**
     * Output, input and bias neuron flag.
     */
    final static int OUTPUT_INPUT_BIAS = 0x07;
}
