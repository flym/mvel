package org.mvel2.util;

import java.lang.reflect.*;

public class Varargs {

  /** 将可变的参数信息转换为正常的参数 */
    public static Object[] normalizeArgsForVarArgs(Class<?>[] parameterTypes, Object[] args, boolean isVarArgs) {
        if (!isVarArgs) return args;
        Object lastArgument = args.length > 0 ? args[args.length - 1] : Array.newInstance(parameterTypes[parameterTypes.length-1].getComponentType(), 0);
        if (parameterTypes.length == args.length && (lastArgument == null || lastArgument.getClass().isArray())) return args;

        int varargLength = args.length - parameterTypes.length + 1;
        Object vararg = Array.newInstance(parameterTypes[parameterTypes.length-1].getComponentType(), varargLength);
        for (int i = 0; i < varargLength; i++) Array.set(vararg, i, args[parameterTypes.length - 1 + i]);

        Object[] normalizedArgs = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length - 1; i++) normalizedArgs[i] = args[i];
        normalizedArgs[parameterTypes.length - 1] = vararg;
        return normalizedArgs;
    }

  /** 获取指定参数的类型信息,并且保证是变参安全的,如果是变参，则获取相应的组件类型 */
    public static Class<?> paramTypeVarArgsSafe(Class<?>[] parameterTypes, int i, boolean isVarArgs) {
      if (!isVarArgs) return parameterTypes[i];
      if (i < parameterTypes.length-1) return parameterTypes[i];
      return parameterTypes[parameterTypes.length-1].getComponentType();
    }
}
