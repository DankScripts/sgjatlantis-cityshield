import java.util.zip.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
def old=new ZipFile(args[0]),now=new ZipFile(args[1])
def changes=[]
now.entries().each{z->if(!z.directory){def o=old.getEntry(z.name);if(o==null || !Arrays.equals(old.getInputStream(o).bytes,now.getInputStream(z).bytes))changes.add(z.name)}}
assert !changes.any{it.contains('/models/')||it.contains('/textures/')||it.endsWith('.obj')||it.endsWith('.mtl')||it.contains('/blockstates/')}
assert !now.entries().any{it.name.startsWith('net/minecraft/')||it.name.startsWith('net/minecraftforge/')}
assert changes.size()==10:changes
int count=0
changes.findAll{it.endsWith('.class')}.each{name->
 def n=new ClassNode();new ClassReader(now.getInputStream(now.getEntry(name)).bytes).accept(n,0)
 assert n.version==Opcodes.V17
 n.methods.each{m->if((m.access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT))==0){
  def frames=new Analyzer(new BasicVerifier()).analyze(n.name,m)
  m.instructions.toArray().eachWithIndex{i,index->
   if(i.opcode==Opcodes.RETURN && frames[index]!=null)assert frames[index].stackSize==0:"${n.name}.${m.name}: stray return stack"
  }
  count++
 }}
}
String pkg='com/dankscripts/sgjatlantis_cityshield/'
def parse={String name->def n=new ClassNode();new ClassReader(now.getInputStream(now.getEntry(pkg+name+'.class')).bytes).accept(n,0);n}
def menu=parse('menu/ShieldControllerMenu'),slot=parse('menu/ShieldControllerMenu$UpgradeSlot'),adapter=parse('menu/CommunicationCrystalContainer')
assert menu.methods.any{it.name=='m_7648_'}
assert menu.methods.any{it.name=='m_6366_'}
assert slot.methods.findAll{it.name in ['m_6641_','m_5866_']}.size()==2
assert slot.methods.any{it.name=='m_5857_'} && slot.methods.any{it.name=='m_8010_'}
assert adapter.methods.any{it.name=='m_8020_'} && adapter.methods.any{it.name=='m_7407_'}
assert adapter.methods.any{it.name=='m_6596_'}
assert menu.methods.any{it.name=='<init>' && it.desc=='(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/inventory/ContainerData;)V'}
println "PASS: ${count} changed methods, Java 17, empty return stacks, slot overrides, menu constructor, no shipped stubs, all geometry unchanged."
println 'Changed entries:';changes.each{println it}
old.close();now.close()
