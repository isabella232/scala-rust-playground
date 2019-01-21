package io.prisma.playground;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.word.PointerBase;
import org.graalvm.word.Pointer;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import java.util.Collections;
import java.util.List;

@CContext(CIntegration.CIntegrationDirectives.class)
public class CIntegration {
    static class CIntegrationDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            /*
             * The header file with the C declarations that are imported. We use a helper class that
             * locates the file in our project structure.
             */
            return Collections.singletonList("\"" + System.getProperty("user.dir") + "/../../playground.h\"");
        }
    }

    @CStruct(value = "ProtoBuf")
    public interface ProtoBuf extends PointerBase {
        @CField("data")
        Pointer getData();

        @CField("len")
        long getLen();

        @CField("data")
        void setData(Pointer pointer);

        @CField("len")
        void setLen(long len);
    }
}
