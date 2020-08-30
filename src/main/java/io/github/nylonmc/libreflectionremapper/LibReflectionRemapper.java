package io.github.nylonmc.libreflectionremapper;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.nylonmc.libreflectionremapper.org.msgpack.core.MessagePack;
import io.github.nylonmc.libreflectionremapper.org.msgpack.core.MessageUnpacker;
import io.github.nylonmc.libreflectionremapper.org.msgpack.value.ImmutableMapValue;
import io.github.nylonmc.libreflectionremapper.org.msgpack.value.MapValue;
import io.github.nylonmc.libreflectionremapper.org.msgpack.value.Value;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

public class LibReflectionRemapper {
    private LibReflectionRemapper() {
    }

    private static final Logger LOGGER = LogManager.getLogger("LibReflectionRemapper");
    private static boolean nativelyIntermediary = true;

    public static final Map<String, String> namedClassToIntermediary = new HashMap<>();
    public static final Map<String, Object2ObjectArrayMap<String, String>> classToMethods = new HashMap<>();
    public static final Map<String, Object2ObjectArrayMap<String, String>> classToFields = new HashMap<>();

    static {
        LOGGER.info("Loading LibReflectionRemapper");
        LOGGER.info("Attempting To Load Mappings");
        long startTime = System.nanoTime();
        try {
            Path nano_path = FabricLoader.getInstance().getModContainer("libreflectionremapper").get()
                    .getPath("mappings.nylonnano");
            MessageUnpacker unpacker;
            
            unpacker = MessagePack.newDefaultUnpacker(Files.newInputStream(nano_path));
            ImmutableMapValue imv = unpacker.unpackValue().asMapValue();
            for (Map.Entry<Value, Value> e : imv.entrySet()) {
                MapValue eMap = e.getValue().asMapValue();
                String c = null;
                Value m = null;
                Value f = null;
                for (Map.Entry<Value, Value> e1 : eMap.entrySet()) {
                    String keyString = e1.getKey().asStringValue().asString();
                    if (keyString.equals("c")) {
                        c = e1.getValue().asStringValue().asString();
                    } else if (keyString.equals("m")) {
                        m = e1.getValue();
                    } else if (keyString.equals("f")) {
                        f = e1.getValue();
                    }
                }
                String intermediaryClassName = e.getKey().asStringValue().asString();
                namedClassToIntermediary.put(c, intermediaryClassName);
                if (m != null) {
                    Object2ObjectArrayMap<String, String> methods = new Object2ObjectArrayMap<>();
                    for (Map.Entry<Value, Value> e1 : m.asMapValue().entrySet()) {
                        methods.put(e1.getKey().asStringValue().asString(), e1.getValue().asStringValue().asString());
                    }
                    classToMethods.put(intermediaryClassName, methods);
                }
                if (f != null) {
                    Object2ObjectArrayMap<String, String> fields = new Object2ObjectArrayMap<>();
                    for (Map.Entry<Value, Value> e1 : f.asMapValue().entrySet()) {
                        fields.put(e1.getKey().asStringValue().asString(), e1.getValue().asStringValue().asString());
                    }
                    classToFields.put(intermediaryClassName, fields);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        LOGGER.info("Mappings Loaded In: {}ms", duration / 1000000f);
        MappingResolver mr = FabricLoader.getInstance().getMappingResolver();
        nativelyIntermediary = mr.getCurrentRuntimeNamespace().equals("intermediary");
        LOGGER.info("Current Namespace Is: {}", mr.getCurrentRuntimeNamespace());
    }

    private static String getInterClassName(Class klazz) {
        if (nativelyIntermediary) {
            return klazz.getCanonicalName();
        } else {
            return FabricLoader.getInstance().getMappingResolver().unmapClassName("intermediary", klazz.getCanonicalName());
        }
    }

    /**
     * Gets The Name Of A Method In The Current Namespace
     * @param klazz The Class The Method Belongs To
     * @param unobfname The Human Readable Name In The NylonMC Mappings
     * @return The Method Name
     */
    public static String getMethodName(Class klazz, String unobfname) {
        if (nativelyIntermediary) {
            return classToMethods.get(getInterClassName(klazz).replace('.', '/')).get(unobfname);
        } else {
            LOGGER.warn("Method Remapping Doesn't Work In Dev Currently, Make Sure You Are Using The Same Version of Yarn That The Mappings Were Built With.");
            return unobfname;
        }
    }

    /**
     * Gets The Name Of A Field In The Current Namespace
     * @param klazz The Class The Method Belongs To
     * @param unobfname The Human Readable Name In The NylonMC Mappings
     * @return The Method Name
     */
    public static String getFieldName(Class klazz, String unobfname) {
        if (nativelyIntermediary) {
            return classToFields.get(getInterClassName(klazz).replace('.', '/')).get(unobfname);
        } else {
            LOGGER.warn("Method Remapping Doesn't Work In Dev Currently, Make Sure You Are Using The Same Version of Yarn That The Mappings Were Built With.");
            return unobfname;
        }
    }
}