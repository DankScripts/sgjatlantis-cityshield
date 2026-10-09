import org.objectweb.asm.*
import org.objectweb.asm.commons.*

// Named Java 17 GUI classes -> Forge 1.20.1 runtime names. No compile stubs are shipped.
def methods = [
 init:'m_7856_',containerTick:'m_181908_',renderBg:'m_7286_',renderLabels:'m_280003_',render:'m_88315_',
 renderBackground:'m_280273_',renderTooltip:'m_280072_',addRenderableWidget:'m_142416_',
 fill:'m_280509_',renderOutline:'m_280637_',drawCenteredString:'m_280653_',
 builder:'m_253074_',setMessage:'m_93666_',bounds:'m_252987_',build:'m_253136_',literal:'m_237113_',
 readBlockPos:'m_130135_',getServer:'m_7654_',getBlockEntity:'m_7702_',hasPermissions:'m_20310_',level:'m_9236_',
 getX:'m_123341_',getY:'m_123342_',getZ:'m_123343_',copy:'m_41777_',isEmpty:'m_41619_',
 addDataSlots:'m_38884_',checkContainerDataCount:'m_38886_',addSlot:'m_38897_',quickMoveStack:'m_7648_',
 clickMenuButton:'m_6366_',moveItemStackTo:'m_38903_',distanceToSqr:'m_20275_',handleInventoryButtonClick:'m_105208_',
 hasItem:'m_6657_',mayPlace:'m_5857_',mayPickup:'m_8010_',set:'m_5852_',onTake:'m_142406_',isActive:'m_6659_',
 getContainerSize:'m_6643_',removeItem:'m_7407_',removeItemNoUpdate:'m_8016_',setItem:'m_6836_',clearContent:'m_6211_',
 setChanged:'m_6596_'
]
def fields = [imageWidth:'f_97726_',imageHeight:'f_97727_',titleLabelX:'f_97728_',titleLabelY:'f_97729_',
 inventoryLabelY:'f_97731_',menu:'f_97732_',leftPos:'f_97735_',topPos:'f_97736_',title:'f_96539_',font:'f_96547_',
 minecraft:'f_96541_',active:'f_93623_',visible:'f_93624_',isClientSide:'f_46443_',EMPTY:'f_41583_',
 index:'f_40219_',x:'f_40220_',y:'f_40221_',slots:'f_38839_',containerId:'f_38840_',gameMode:'f_91072_']
def eligible = { String o -> o.startsWith('net/minecraft/') || o.contains('/ShieldControllerScreen') ||
 o.contains('/ShieldControllerMenu') || o.endsWith('/CommunicationCrystalContainer') }
def slotOwner = { String o -> o.endsWith('/Slot') || o.endsWith('Slot') }
def containerOwner = { String o -> o.endsWith('/Container') || o.endsWith('/CommunicationCrystalContainer') }
def mapper = new Remapper() {
 @Override String mapMethodName(String o,String n,String d) {
  if (!eligible(o)) return n
  if (o.endsWith('/ContainerData') || o.endsWith('/ShieldControllerMenu$1'))
   return [get:'m_6413_',set:'m_8050_',getCount:'m_6499_'].get(n,n)
  if (n=='getItem') return slotOwner(o)?'m_7993_':containerOwner(o)?'m_8020_':n
  if (n=='setChanged' && slotOwner(o)) return 'm_6654_'
  if (n=='isEmpty' && containerOwner(o)) return 'm_7983_'
  if (n=='getMaxStackSize') return slotOwner(o)?(d=='()I'?'m_6641_':'m_5866_'):containerOwner(o)?'m_6893_':n
  if (n=='stillValid') return containerOwner(o)?'m_6542_':'m_6875_'
  if (n=='translatable') return d.startsWith('(Ljava/lang/String;)')?'m_237115_':'m_237110_'
  if (n=='drawString') return d.contains('Ljava/lang/String;')?'m_280056_':'m_280614_'
  if (n=='width') return d.contains('Ljava/lang/String;')?'m_92895_':'m_92852_'
  return methods.get(n,n)
 }
 @Override String mapFieldName(String o,String n,String d) {
  if (!eligible(o)) return n
  if (n=='player') return o.endsWith('/Inventory')?'f_35978_':o.endsWith('/Minecraft')?'f_91074_':n
  return fields.get(n,n)
 }
 @Override String mapInvokeDynamicMethodName(String n,String d) {
  return d.endsWith('Lnet/minecraft/client/gui/components/Button$OnPress;')?'m_93750_':n
 }
}
File input=new File(args[0]),output=new File(args[1])
input.eachFileRecurse { f -> if(f.name.endsWith('.class')) {
 def w=new ClassWriter(0);new ClassReader(f.bytes).accept(new ClassRemapper(w,mapper),0)
 def dst=new File(output,input.toPath().relativize(f.toPath()).toString());dst.parentFile.mkdirs();dst.bytes=w.toByteArray()
 println dst
}}
