package com.dankscripts.sgjatlantis_cityshield.client;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class RendererRegistrationCheck {
    public static void main(String[] args) throws IOException {
        var registration = new RegistrationVisitor();
        try (var stream = RendererRegistrationCheck.class.getResourceAsStream("ShieldPreviewRenderer.class")) {
            if (stream == null) throw new AssertionError("Shield renderer missing from compiled source");
            new ClassReader(stream).accept(registration,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        if (!AtlantisCityShield.MOD_ID.equals(registration.modId)
                || !"GAME".equals(registration.bus)
                || !registration.sides.equals(List.of("CLIENT"))) {
            throw new AssertionError("Shield renderer must auto-register only on the client NeoForge game event bus");
        }
        if (!registration.renderHandler) {
            throw new AssertionError("Shield renderer must subscribe its static level-render handler");
        }
        System.out.println("PASS: shield renderer client event registration");
    }

    private static final class RegistrationVisitor extends ClassVisitor {
        private String modId;
        private String bus;
        private final List<String> sides = new ArrayList<>();
        private boolean renderHandler;

        private RegistrationVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!visible || !descriptor.equals("Lnet/neoforged/fml/common/EventBusSubscriber;")) {
                return null;
            }
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String name, Object value) {
                    if ("modid".equals(name)) modId = (String) value;
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    if ("bus".equals(name)
                            && descriptor.equals("Lnet/neoforged/fml/common/EventBusSubscriber$Bus;")) {
                        bus = value;
                    }
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    if (!"value".equals(name)) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitEnum(String name, String descriptor, String value) {
                            if (descriptor.equals("Lnet/neoforged/api/distmarker/Dist;")) sides.add(value);
                        }
                    };
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            int requiredAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
            if (!name.equals("render") || (access & requiredAccess) != requiredAccess
                    || !descriptor.equals("(Lnet/neoforged/neoforge/client/event/RenderLevelStageEvent;)V")) {
                return null;
            }
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (visible && descriptor.equals("Lnet/neoforged/bus/api/SubscribeEvent;")) {
                        renderHandler = true;
                    }
                    return null;
                }
            };
        }
    }
}
