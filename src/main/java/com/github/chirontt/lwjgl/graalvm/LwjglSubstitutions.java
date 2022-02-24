package com.github.chirontt.lwjgl.graalvm;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.*;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.lwjgl.system.ThreadLocalUtil.class)
final class Target_org_lwjgl_system_ThreadLocalUtil {

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias, isFinal = true)
    private static long JNI_NATIVE_INTERFACE;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias, isFinal = true)
    private static long FUNCTION_MISSING_ABORT;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias, isFinal = true)
    private static int CAPABILITIES_OFFSET;

    @Substitute
    public static void setFunctionMissingAddresses(int functionCount) {
        long ptr = JNI_NATIVE_INTERFACE + CAPABILITIES_OFFSET;
        if (functionCount == 0) {
            long missingCaps = memGetAddress(ptr);
            if (missingCaps != NULL) {
                getAllocator().free(missingCaps);
                memPutAddress(ptr, NULL);
            }
        } else {
            long missingCaps = getAllocator().malloc(Integer.toUnsignedLong(functionCount) * POINTER_SIZE);
            for (int i = 0; i < functionCount; i++) {
                memPutAddress(missingCaps + Integer.toUnsignedLong(i) * POINTER_SIZE, FUNCTION_MISSING_ABORT);
            }

            //the whole purpose of substituting this method is just to remove the following line
            //(which causes the generated native image to crash!)
            //memPutAddress(ptr, missingCaps);
        }
    }

}

/** Dummy class with the file's name. */
public class LwjglSubstitutions {
}
