import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
import java.util.zip.*

// Apply after the GUI/text fixes. Preserve all resources and original collision boxes.
def source=new ZipFile(args[0]);def entries=[:]
source.entries().each{e->if(!e.directory)entries[e.name]=source.getInputStream(e).bytes};source.close()
String path='com/dankscripts/sgjatlantis_cityshield/block/ShieldControllerBlock.class'
def n=new ClassNode();new ClassReader(entries[path]).accept(n,0)
def outline=n.methods.find{it.name=='m_5940_'}
assert outline!=null && !n.methods.any{it.name=='m_5939_'}
def collision=new MethodNode(outline.access,'m_5939_',outline.desc,outline.signature,outline.exceptions as String[])
outline.accept(collision);collision.name='m_5939_';n.methods.add(collision)
outline.instructions.clear();outline.tryCatchBlocks.clear();outline.localVariables?.clear()
for(int local=0;local<5;local++)outline.instructions.add(new VarInsnNode(Opcodes.ALOAD,local))
outline.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,n.name,'m_5939_',outline.desc,false))
outline.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,'net/minecraft/world/phys/shapes/VoxelShape','m_83215_','()Lnet/minecraft/world/phys/AABB;',false))
outline.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'net/minecraft/world/phys/shapes/Shapes','m_83064_','(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/shapes/VoxelShape;',false))
outline.instructions.add(new InsnNode(Opcodes.ARETURN));outline.maxStack=5;outline.maxLocals=5
n.methods.each{m->if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer(new BasicVerifier()).analyze(n.name,m)}
def w=new ClassWriter(ClassWriter.COMPUTE_MAXS);n.accept(w);entries[path]=w.toByteArray()
def output=new File(args[1]);output.parentFile.mkdirs()
output.withOutputStream{stream->def out=new ZipOutputStream(stream);entries.each{k,v->def e=new ZipEntry(k);e.time=0;out.putNextEntry(e);out.write(v);out.closeEntry()};out.finish()}
println 'PASS: outline returns one enclosing box; original directional shape retained as collision; bytecode verified.'
