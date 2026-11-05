package kg.bakai.stopfactor.enums;

/**
 * Type of the value of a condition. Keep in sync with ck_stop_factor_condition_value_type.
 */
public enum StopFactorValueTypes {
    /** One loan status. */
    STRING,
    /** A whole number: the highest allowed value. */
    INTEGER,
    /** A number: the highest allowed value. */
    DOUBLE,
    /** A list of loan statuses separated by commas. */
    ARRAY
}
