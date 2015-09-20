package com.lenis0012.bukkit.marriage2.internal;

import java.lang.reflect.Method;
import java.util.List;
import com.google.common.collect.Lists;
import org.bukkit.plugin.java.JavaPlugin;
import com.lenis0012.bukkit.marriage2.Marriage;
import com.lenis0012.bukkit.marriage2.MarriageLog;
import java.lang.reflect.InvocationTargetException;

public class MarriagePlugin extends JavaPlugin {

    private static MarriageCore core;

    public static Marriage getInstance() {
        return core;
    }

    @SuppressWarnings("unchecked")
    private final List<Method>[] methods = new List[Register.Type.values().length];

    public MarriagePlugin() {
        core = new MarriageCore(this);

        //Scan methods
        for (int i = 0; i < methods.length; i++) {
            methods[i] = Lists.newArrayList();
        }
        scanMethods(core.getClass());
    }

    private void scanMethods(Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        // Loop through all methods in class
        for (Method method : clazz.getMethods()) {
            Register register = method.getAnnotation(Register.class);
            if (register != null) {
                methods[register.type().ordinal()].add(method);
            }
        }

        // Scan methods in super class
        scanMethods(clazz.getSuperclass());
    }

    @Override
    public void onLoad() {
        executeMethods(Register.Type.LOAD);
    }

    @Override
    public void onEnable() {
        executeMethods(Register.Type.ENABLE);
    }

    @Override
    public void onDisable() {
        executeMethods(Register.Type.DISABLE);
    }

    private void executeMethods(Register.Type type) {
        List<Method> list = Lists.newArrayList(methods[type.ordinal()]);
        while (!list.isEmpty()) {
            Method method = null;
            int lowestPriority = Integer.MAX_VALUE;
            for (Method m : list) {
                Register register = m.getAnnotation(Register.class);
                if (register.priority() < lowestPriority) {
                    method = m;
                    lowestPriority = register.priority();
                }
            }

            if (method != null) {
                list.remove(method);
                Register register = method.getAnnotation(Register.class);
                MarriageLog.info("Loading " + register.name() + "...");
                try {
                    method.invoke(core);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    MarriageLog.severe("Failed to load " + register.name() + e);
                }
            } else {
                list.clear();
            }
        }

        MarriageLog.info(type.getCompletionMessage());
    }
}
