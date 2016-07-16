package rlib.test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;

import rlib.util.array.ArrayFactory;

/**
 * Created by ronn on 29.06.16.
 */
public class MethodHandler {

    public static void main(String[] args) throws Throwable {

        final MethodType constructorMethodType = MethodType.methodType(void.class, ArrayFactory.toGenericArray(int.class));
        final MethodType methodType = MethodType.methodType(boolean.class, ArrayFactory.toGenericArray(Object.class));

        final MethodHandle constructor = MethodHandles.lookup().findConstructor(ArrayList.class, constructorMethodType);
        final Object result = constructor.invokeWithArguments(Arrays.asList(20));
        System.out.println(result);

        final MethodHandle add = MethodHandles.lookup().findVirtual(ArrayList.class, "add", methodType);
        add.invokeWithArguments(Arrays.asList(result, "ASDasdasd"));

        System.out.println(result);
    }
}
