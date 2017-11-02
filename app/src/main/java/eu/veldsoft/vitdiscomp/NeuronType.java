package eu.veldsoft.vitdiscomp;

/**
 * @author Todor Balabanov
 */
enum NeuronType {

    /**
     * Regular neuron flag.
     */
    REGULAR(0x00),

    /**
     * Bias neuron flag.
     */
    BIAS(0x01),

    /**
     * Input neuron flag.
     */
    INPUT(0x02),

    /**
     * Input and bias neuron flag.
     */
    INPUT_BIAS(0x03),

    /**
     * Output neuron flag.
     */
    OUTPUT(0x04),

    /**
     * Output and bias neuron flag.
     */
    OUTPUT_BIAS(0x05),

    /**
     * Output and input neuron flag.
     */
    OUTPUT_INPUT(0x06),

    /**
     * Output, input and bias neuron flag.
     */
    OUTPUT_INPUT_BIAS(0x07);

    /*
     * Numeric value representation.
     */
    private final int value;

    /**
     * Value factory function.
     *
     * @param type Numerical type representation.
     * @return Corresponding enumeration or regular if there is no corespondence.
     */
    public static NeuronType valueOf(int type) {
        for (NeuronType item : NeuronType.values()) {
            if (item.value() == type) {
                return item;
            }
        }

        return REGULAR;
    }

    /**
     * Constructor with all parameters.
     *
     * @param value
     */
    private NeuronType(int value) {
        this.value = value;
    }

    /**
     * Value getter.
     *
     * @return Numeric representation of the type.
     */
    public int value() {
        return value;
    }
}
