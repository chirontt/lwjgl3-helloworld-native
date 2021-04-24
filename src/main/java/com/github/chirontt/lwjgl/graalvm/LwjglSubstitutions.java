package com.github.chirontt.lwjgl.graalvm;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.lwjgl.system.APIUtil;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.lwjgl.system.APIUtil.class)
final class Target_org_lwjgl_system_APIUtil {

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias, isFinal = true)
    public static PrintStream DEBUG_STREAM;

    @Substitute
    public static Optional<String> apiGetManifestValue(String attributeName) {
        if (attributeName == null || attributeName.isEmpty()) return Optional.empty();

        Package currentPackage = APIUtil.class.getPackage();
        switch (attributeName.toLowerCase()) {
            case "implementation-title":
                return Optional.ofNullable(currentPackage.getImplementationTitle());
            case "implementation-vendor":
                return Optional.ofNullable(currentPackage.getImplementationVendor());
            case "implementation-version":
                return Optional.ofNullable(currentPackage.getImplementationVersion());
            case "specification-title":
                return Optional.ofNullable(currentPackage.getSpecificationTitle());
            case "specification-vendor":
                return Optional.ofNullable(currentPackage.getSpecificationVendor());
            case "specification-version":
                return Optional.ofNullable(currentPackage.getSpecificationVersion());
            default:
                ClassLoader loader = APIUtil.class.getClassLoader();
                try {
                    //find a MANIFEST.MF resource from one of the LWJGL jars
                    for (Enumeration<URL> e = loader.getResources(JarFile.MANIFEST_NAME); e.hasMoreElements(); ) {
                        URL url = e.nextElement();
                        try (InputStream stream = url.openStream()) {
                            Attributes attributes = new Manifest(stream).getMainAttributes();
                            //is this manifest resource from LWJGL?
                            if ("lwjgl.org".equals(attributes.getValue("Implementation-Vendor")))
                                return Optional.ofNullable(attributes.getValue(attributeName));
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace(DEBUG_STREAM);
                }

                return Optional.empty();
        }
    }
}

@TargetClass(org.lwjgl.system.ThreadLocalUtil.class)
final class Target_org_lwjgl_system_ThreadLocalUtil {

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias, isFinal = true)
    private static long JNI_NATIVE_INTERFACE;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias, isFinal = true)
    private static long FUNCTION_MISSING_ABORT;

    @Substitute
    public static void setFunctionMissingAddresses(Class<?> capabilitiesClass, int index) {
        if (capabilitiesClass == null) {
            long missingCaps = memGetAddress(JNI_NATIVE_INTERFACE + Integer.toUnsignedLong(index) * POINTER_SIZE);
            if (missingCaps != NULL) {
                getAllocator().free(missingCaps);
                memPutAddress(JNI_NATIVE_INTERFACE + Integer.toUnsignedLong(index) * POINTER_SIZE, NULL);
            }
        } else {
            int functionCount = getFieldsFromCapabilities(capabilitiesClass).size();

            long missingCaps = getAllocator().malloc(Integer.toUnsignedLong(functionCount) * POINTER_SIZE);
            for (int i = 0; i < functionCount; i++) {
                memPutAddress(missingCaps + Integer.toUnsignedLong(i) * POINTER_SIZE, FUNCTION_MISSING_ABORT);
            }

            //the whole purpose of substituting this method is just to remove the following line
            //(which causes the resulting native image to crash!)
            //memPutAddress(JNI_NATIVE_INTERFACE + Integer.toUnsignedLong(index) * POINTER_SIZE, missingCaps);
        }
    }

    //copied verbatim from the original class
    @Substitute
    private static List<Field> getFieldsFromCapabilities(Class<?> capabilitiesClass) {
        List<Field> fields = new ArrayList<>();
        for (Field field : capabilitiesClass.getFields()) {
            if (field.getType() == long.class) {
                fields.add(field);
            }
        }
        return fields;
    }
}

/** Dummy class with the file's name. */
public class LwjglSubstitutions {
}
