import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Lambda
{
    public static final int ITERATIONS = 100;
    public static final int REPETITIONS = 3;
    public static final int src[] = new int [10000];
    
    static interface Test
    {
        public abstract int test();
    }
    
    static class Test_Direct implements Test
    {
        private final int[] x;
        
        public Test_Direct (int [] x)
        {
            this.x = x;
        }
        
        @Override
        public int test()
        {
            int res = 0;
            for (int i = 0; i < x.length; i++) res += i*x[i];
            return res;
        }
    }
    
    static interface F_Interface
    {
        public int f (int i);
    }
    
    static abstract class F_Abstract_Class
    {
        public abstract int f (int i);
    }
    
    static int sum_interface (int lo, int hi, F_Interface f)
    {
        int res = 0;
        for (int i = 0; i < hi; i++) res += f.f(i);
        return res;
    }

    static int sum_abstract_class (int lo, int hi, F_Abstract_Class f)
    {
        int res = 0;
        for (int i = 0; i < hi; i++) res += f.f(i);
        return res;
    }
    
    static class Test_Interface implements F_Interface, Test
    {
        private final int[] x;
        
        public Test_Interface (int [] x)
        {
            this.x = x;
        }
    
        @Override
        public final int f (int i)
        {
            return i * x[i];
        }
        
        @Override
        public int test()
        {
            return sum_interface (0, x.length, this);
        }
    }

    static class Test_Abstract_Class extends F_Abstract_Class implements Test
    {
        private final int[] x;
        
        public Test_Abstract_Class (int [] x)
        {
            this.x = x;
        }
    
        @Override
        public final int f (int i)
        {
            return i * x[i];
        }
        
        @Override
        public int test()
        {
            return sum_abstract_class (0, x.length, this);
        }
    }

    static abstract class Sum_Inherited
    {
        abstract int f (int i);
        
        int sum (int lo, int hi)
        {
            int res = 0;
            for (int i = 0; i < hi; i++) res += f(i);
            return res;
        }
    }
    
    static class Test_Inherited extends Sum_Inherited implements Test
    {
        private final int[] x;
        
        public Test_Inherited (int [] x)
        {
            this.x = x;
        }
    
        @Override
        public final int f (int i)
        {
            return i * x[i];
        }
        
        @Override
        public int test()
        {
            return sum (0, x.length);
        }
    }

    static int sum_static_reflection (int lo, int hi, Method f)
    {
        try {
            int res = 0;
            for (int i = 0; i < hi; i++) res += (Integer) f.invoke (null, i);
            return res;
        } catch (Exception e) {
            throw new AssertionError ();
        }
        
    }
    
    static class Test_Static_Reflection implements Test
    {
        private final Method m;
    
        public Test_Static_Reflection ()
        {
            try {
                m = this.getClass ().getMethod ("f", int.class);
            } catch (NoSuchMethodException e) {
                throw new AssertionError ();
            }
        }
        
        public static int f (int i)
        {
            return i * src[i];
        }
        
        @Override
        public int test()
        {
            return sum_static_reflection (0, src.length, m);
        }
    }
    
    static int sum_reflection (int lo, int hi, Object obj, Method f)
    {
        try {
            int res = 0;
            for (int i = 0; i < hi; i++) res += (Integer) f.invoke (obj, i);
            return res;
        } catch (Exception e) {
            throw new AssertionError ();
        }
    }
    
    static class Test_Reflection implements Test
    {
        private final int[] x;
        private final Method m;
    
        public Test_Reflection (int [] x)
        {
            this.x = x;
            try {
                m = this.getClass ().getMethod ("f", int.class);
            } catch (NoSuchMethodException e) {
                throw new AssertionError ();
            }
        }
        
        public final int f (int i)
        {
            return i * x[i];
        }
        
        @Override
        public int test()
        {
            return sum_reflection (0, x.length, this, m);
        }
    }

    static int sum_handle (int lo, int hi, MethodHandle f)
    {
        try {
            int res = 0;
            for (int i = 0; i < hi; i++) res += (Integer) f.invoke (i);
            return res;
        } catch (Throwable e) {
            throw new AssertionError ();
        }
    }
    
    static class Test_MethodHandle implements Test
    {
        private final MethodHandle m;
    
        public Test_MethodHandle ()
        {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodType t = MethodType.methodType (int.class, int.class);
                m = lookup.findStatic (this.getClass (), "f", t);
            } catch (Exception e) {
                e.printStackTrace ();
                throw new AssertionError ();
            }
        }
        
        public static int f (int i)
        {
            return i * src[i];
        }
        
        @Override
        public int test()
        {
            return sum_handle (0, src.length, m);
        }
    }

    static class Test_DynamicProxy implements Test, InvocationHandler
    {
        private final int[] x;
        private final F_Interface i;
    
        public Test_DynamicProxy (int[] x)
        {
            this.x = x;
            i = (F_Interface) Proxy.newProxyInstance (this.getClass ().getClassLoader (), new Class<?> [] {F_Interface.class}, this);
        }
        
        private int f (int i)
        {
            return i * x[i];
        }
    
        @Override
        public Object invoke (Object proxy, Method method, Object[] args) throws Throwable
        {
            return f ((Integer) args[0]);
        }
        
        @Override
        public int test()
        {
            return sum_interface (0, x.length, i);
        }
    }
    
  /*  
    static class Test_Lambda implements Test
    {
        public static int f (int i)
        {
            return i * src[i];
        }
        
        @Override
        public int test()
        {
            return sum_interface (0, src.length, new F_Interface () {
                public int f (int i)
                {
                    return i * src[i];
                }
            });
        }
    }
*/    
    
    static class Test_Lambda implements Test
    {
        @Override
        public int test()
        {
            return sum_interface (0, src.length, i -> i * src[i]);
        }
    }

    static class Test_Lambda_Capture implements Test
    {
        private final int[] x;
    
        public Test_Lambda_Capture (int [] x)
        {
            this.x = x;
        }
    
        @Override
        public int test()
        {
            return sum_interface (0, x.length, i -> i * x[i]);
        }
    }

    static class Test_Lambda_Capture_2 implements Test
    {
        @Override
        public int test()
        {
            int[] y = src;
            return sum_interface (0, y.length, i -> i * y[i]);
        }
    }

    static void measure (Test test) 
    {
        System.out.print (test.getClass() + ": ");
        long sum = 0;
        for (int j = 0; j < REPETITIONS; j++) {
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                sum += test.test ();
            }
            long t = System.currentTimeMillis() - t0;
            System.out.print (" " + t);
        }
        System.out.println ("; sum = " + sum);
    }

    public static void main (String [] args)
    {
        for (int i = 0; i < src.length; i++) {
            src [i] = i;
        }
        measure (new Test_Direct (src));
        measure (new Test_Interface (src));
        measure (new Test_Abstract_Class (src));
        measure (new Test_Inherited (src));
        measure (new Test_DynamicProxy (src));
        measure (new Test_Static_Reflection ());
        measure (new Test_Reflection (src));
        measure (new Test_MethodHandle ());
        measure (new Test_Lambda ());
        measure (new Test_Lambda_Capture (src));
        measure (new Test_Lambda_Capture_2 ());
    }
}
