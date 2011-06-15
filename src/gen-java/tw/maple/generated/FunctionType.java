/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package tw.maple.generated;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

public enum FunctionType implements org.apache.thrift.TEnum {
  TF_NORMAL(0),
  TF_GETTER(1),
  TF_SETTER(2);

  private final int value;

  private FunctionType(int value) {
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
  public static FunctionType findByValue(int value) { 
    switch (value) {
      case 0:
        return TF_NORMAL;
      case 1:
        return TF_GETTER;
      case 2:
        return TF_SETTER;
      default:
        return null;
    }
  }
}
