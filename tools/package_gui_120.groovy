import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
import java.util.zip.*
import groovy.json.*

File root=new File(args[0]), baseline=new File(args[1]), output=new File(args[2])
def zip=new ZipFile(baseline)
def entries=[:];zip.entries().each{e->if(!e.directory)entries[e.name]=zip.getInputStream(e).bytes};zip.close()
String pkg='com/dankscripts/sgjatlantis_cityshield/',c=pkg+'block/ShieldControllerBlockEntity',e=pkg+'block/ShieldEmitterBlockEntity'
def node={String name->def n=new ClassNode();new ClassReader(entries[name+'.class']).accept(n,0);n}
def previous={i->def p=i.previous;while(p!=null && p.opcode<0)p=p.previous;p}
def controller=node(c)
controller.methods.find{it.name=='communicationRangeIncrease'}.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC
assert !controller.methods.any{it.name=='communicationInventory'}
def accessor=new MethodNode(Opcodes.ACC_PUBLIC,'communicationInventory','()Lnet/minecraft/world/Container;',null,null)
accessor.visitCode();accessor.visitTypeInsn(Opcodes.NEW,pkg+'menu/CommunicationCrystalContainer');accessor.visitInsn(Opcodes.DUP)
accessor.visitVarInsn(Opcodes.ALOAD,0);accessor.visitFieldInsn(Opcodes.GETFIELD,c,'communicationCrystals','Lnet/minecraftforge/items/ItemStackHandler;')
accessor.visitVarInsn(Opcodes.ALOAD,0)
accessor.visitMethodInsn(Opcodes.INVOKESPECIAL,pkg+'menu/CommunicationCrystalContainer','<init>',
 '(Lnet/minecraftforge/items/ItemStackHandler;Lnet/minecraft/world/level/block/entity/BlockEntity;)V',false)
accessor.visitInsn(Opcodes.ARETURN);accessor.visitMaxs(4,1);accessor.visitEnd();controller.methods.add(accessor)
def create=controller.methods.find{it.name=='m_7208_'}
def ctor=create.instructions.find{it instanceof MethodInsnNode && it.owner==pkg+'menu/ShieldControllerMenu' && it.name=='<init>'}
assert ctor.desc.endsWith('Lnet/minecraftforge/items/ItemStackHandler;)V')
def field=previous(ctor),load=previous(field)
assert field instanceof FieldInsnNode && field.name=='communicationCrystals' && load.opcode==Opcodes.ALOAD
create.instructions.remove(field);create.instructions.remove(load)
ctor.desc=ctor.desc.replace('Lnet/minecraftforge/items/ItemStackHandler;','')
def tick=controller.methods.find{it.name=='serverTick'}
def checks=tick.instructions.findAll{it instanceof MethodInsnNode && it.owner==c && it.name=='hasZeroPointPower'}
assert checks.size()==2
tick.instructions.insertBefore(checks[1],new VarInsnNode(Opcodes.ALOAD,3))
def emitter=node(e),etick=emitter.methods.find{it.name=='serverTick'}
def constant=etick.instructions.find{it.opcode==Opcodes.ICONST_1 && previous(it) instanceof VarInsnNode &&
 previous(it).var==3 && it.next instanceof JumpInsnNode && it.next.opcode==Opcodes.IFEQ}
assert constant!=null
previous(constant).var=7
etick.instructions.set(constant,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,c,'hasLargeNaquadahCable','()Z',false))
def block=node(pkg+'block/ShieldControllerBlock'),use=block.methods.find{it.name=='m_6227_'}
def gate=use.instructions.find{it instanceof MethodInsnNode && it.name=='m_20310_'}
if(gate!=null){
 // Remove only the GUI-entry permission gate. Packet handlers retain operator checks.
 def replacement=new InsnList();replacement.add(new InsnNode(Opcodes.POP2));replacement.add(new InsnNode(Opcodes.ICONST_1))
 use.instructions.insertBefore(gate,replacement);use.instructions.remove(gate)
}

// Resolve superclass metadata without loading Minecraft classes into the JVM.
def mc=new ZipFile(new File(root.parentFile,'audit-runtime/minecraft-1.21.1-neoforge-compile-v2.jar'))
def metadata=[:]
def info={String name->
 if(metadata.containsKey(name))return metadata[name]
 byte[] bytes=entries[name+'.class']
 if(bytes==null){def z=mc.getEntry(name+'.class');if(z!=null)bytes=mc.getInputStream(z).bytes}
 if(bytes==null){def stream=ClassLoader.getSystemResourceAsStream(name+'.class');if(stream!=null){bytes=stream.bytes;stream.close()}}
 if(bytes==null)throw new IllegalStateException('Missing hierarchy: '+name)
 def r=new ClassReader(bytes);metadata[name]=[parent:r.superName,interfaces:r.interfaces as List,iface:(r.access&Opcodes.ACC_INTERFACE)!=0]
}
def assign
assign={String target,String type->target==type || (type!=null &&
 (info(type).interfaces.any{assign(target,it)} || (info(type).parent!=null && assign(target,info(type).parent))))}
def write={ClassNode n->
 def w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){
  @Override protected String getCommonSuperClass(String a,String b){
   if(assign(a,b))return a;if(assign(b,a))return b
   if(info(a).iface || info(b).iface)return 'java/lang/Object'
   do{a=info(a).parent}while(a!=null && !assign(a,b));return a?:'java/lang/Object'
  }
 }
 n.accept(w);entries[n.name+'.class']=w.toByteArray()
}
[controller,emitter,block].each{write(it)}
mc.close()
def runtime=new File(root,'build-gui/runtime')
runtime.eachFileRecurse{f->if(f.name.endsWith('.class'))entries[runtime.toPath().relativize(f.toPath()).toString()]=f.bytes}
String lang='assets/sgjatlantis_cityshield/lang/en_us.json'
def language=new JsonSlurper().parse(entries[lang])
def sourceLang=new JsonSlurper().parse(new File(root,'src/main/resources/'+lang))
sourceLang.each{k,v->if(k.startsWith('screen.sgjatlantis_cityshield.'))language[k]=v}
entries[lang]=(JsonOutput.prettyPrint(JsonOutput.toJson(language))+'\n').getBytes('UTF-8')
int count=0
entries.each{name,bytes->if(name.endsWith('.class')){
 def n=new ClassNode();new ClassReader(bytes).accept(n,0)
 n.methods.each{m->if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0){
  new Analyzer(new BasicVerifier()).analyze(n.name,m);count++
 }}
}}
output.parentFile.mkdirs()
output.withOutputStream{stream->def out=new ZipOutputStream(stream);entries.each{name,bytes->
 def z=new ZipEntry(name);z.time=0;out.putNextEntry(z);out.write(bytes);out.closeEntry()
};out.finish()}
println "Validated ${count} methods; wrote ${output}"
