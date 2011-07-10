/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package tw.maple.generated;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

public enum IncrementType implements org.apache.thrift.TEnum {
  TYPE_POSTFIX(0),
  TYPE_PREFIX(1);

  private final int value;

  private IncrementType(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static IncrementType findByValue(int value) { 
    switch (value) {
      case 0:
        return TYPE_POSTFIX;
      case 1:
        return TYPE_PREFIX;
      default:
        return null;
    }
  }
}