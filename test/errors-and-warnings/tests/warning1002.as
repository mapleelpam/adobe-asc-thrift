//[Coach] Warning #1002: Undefined function.  This can throw a 'ReferenceError' in AS3, rather than failing silently as it did in AS2.
package err {
   public class T {
      public function a() {}
   }

   public class E1 {
       public function toString():Object {
           var s:String = new String("test");
           s.notHere();

           var t:T = new T("test");
           t.b();
           delete t.b()

           // Next group of statements should NOT gen a warning
           t.a();
           delete t.a();

           (new Ar());
           (new Array());

           var repeaters:Array = new Array();
           pop();
           // these don't report warnings because Array isDynamic
           repeaters.pop1();
           repeaters.pop1(1);
           delete repeaters.pop1();
           delete repeaters.pop1(1);

           // Next group of statements should NOT gen a warning
           repeaters.pop();
           repeaters.pop(1);
           delete repeaters.pop();
           delete repeaters.pop(1);
           delete (true?repeaters.pop():repeaters.pop());
           (true? delete repeaters.pop():repeaters.pop());

           delete this.missingMethod();
           return ("E1 " + this.missingMethod());
      }
   }
}
