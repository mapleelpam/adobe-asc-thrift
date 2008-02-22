// [Coach] Warning #1000: Undeclared property reference.  This can throw a 'ReferenceError' in AS3, rather than evaluating to undefined as it did in AS2.
package {
   import flash.events.*;
   import flash.display.*;

   public class C {
      public static function doCreationComplete(event:Event) {
         // __centerReference doesn't exist
         DisplayObject(event.target).__centerReference; //get
         event.target.__centerReference++; // inc
         event.target.__centerReference = null; // set
      }
   }
}
