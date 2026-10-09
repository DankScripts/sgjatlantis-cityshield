import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
import java.util.zip.*

// Run after patch_selection_outline.groovy. No collision/model/selection changes.
def zip=new ZipFile(args[0]);def entries=[:]
zip.entries().each{e->if(!e.directory)entries[e.name]=zip.getInputStream(e).bytes};zip.close()
String path='com/dankscripts/sgjatlantis_cityshield/block/ShieldControllerBlock.class'
def n=new ClassNode();new ClassReader(entries[path]).accept(n,0)
assert n.methods.any{it.name=='m_5939_'} && !n.methods.any{it.name=='m_7952_'}
def methodBytes={m->def w=new ClassWriter(0);w.visit(61,1,'Test',null,'java/lang/Object',null)
 m.accept(w.visitMethod(m.access,m.name,m.desc,m.signature,m.exceptions as String[]));w.toByteArray()}
def original=n.methods.collectEntries{m->[(m.name+m.desc):methodBytes(m)]}
def m=new MethodNode(Opcodes.ACC_PUBLIC,'m_7952_',
 '(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;',null,null)
m.visitCode();m.visitMethodInsn(Opcodes.INVOKESTATIC,'net/minecraft/world/phys/shapes/Shapes','m_83040_','()Lnet/minecraft/world/phys/shapes/VoxelShape;',false)
m.visitInsn(Opcodes.ARETURN);m.visitMaxs(1,4);m.visitEnd();n.methods.add(m)
def w=new ClassWriter(0);n.accept(w);entries[path]=w.toByteArray()
def verify=new ClassNode();new ClassReader(entries[path]).accept(verify,0)
verify.methods.each{v->
 if(original.containsKey(v.name+v.desc))assert Arrays.equals(original[v.name+v.desc],methodBytes(v))
 if((v.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer(new BasicVerifier()).analyze(verify.name,v)
}
assert verify.methods.size()==original.size()+1
def output=new File(args[1]);output.parentFile.mkdirs()
output.withOutputStream{stream->def out=new ZipOutputStream(stream);entries.each{k,v->def e=new ZipEntry(k);e.time=0;out.putNextEntry(e);out.write(v);out.closeEntry()};out.finish()}
println 'PASS: empty occlusion override added; every existing method including outline and collision is unchanged; bytecode verified.'
