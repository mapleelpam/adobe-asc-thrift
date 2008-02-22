//[Compiler] Error #1128: Negative value used where a uint (positive) value was expected

class C
{
   function f(v:uint)
   {
      var x:uint = -1;
      x = -1;
      v = -1;
   }
}

(new C).f(-1);
