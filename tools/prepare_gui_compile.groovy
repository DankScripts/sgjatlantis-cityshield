import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.util.jar.JarFile

// Compile-only compatibility overlay. Never include these classes in the mod.
def root = new File(args[0]); def out = new File(args[1])
def write = { ClassNode node ->
    def writer = new ClassWriter(ClassWriter.COMPUTE_MAXS)
    node.accept(writer)
    def dest = new File(out, node.name + '.class'); dest.parentFile.mkdirs(); dest.bytes = writer.toByteArray()
}
def controller = new ClassNode()
new ClassReader(new File(root, 'alpha31-compile-named6/com/dankscripts/sgjatlantis_cityshield/block/ShieldControllerBlockEntity.class').bytes).accept(controller, 0)
controller.methods.find { it.name == 'communicationRangeIncrease' }.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
def accessor = new MethodNode(Opcodes.ACC_PUBLIC, 'communicationInventory', '()Lnet/minecraft/world/Container;', null, null)
accessor.instructions.add(new InsnNode(Opcodes.ACONST_NULL)); accessor.instructions.add(new InsnNode(Opcodes.ARETURN))
controller.methods.add(accessor); write(controller)
def jar = new JarFile(new File(root, 'audit-runtime/minecraft-1.21.1-neoforge-compile-v2.jar'))
def screen = new ClassNode()
new ClassReader(jar.getInputStream(jar.getJarEntry('net/minecraft/client/gui/screens/Screen.class'))).accept(screen, 0)
def background = new MethodNode(Opcodes.ACC_PUBLIC, 'renderBackground', '(Lnet/minecraft/client/gui/GuiGraphics;)V', null, null)
background.instructions.add(new InsnNode(Opcodes.RETURN)); screen.methods.add(background); write(screen)
jar.close()
