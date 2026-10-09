import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.util.zip.ZipFile
import java.lang.reflect.InvocationTargetException

// JVM regression test for the exact Forge 1.20.1 Button.OnPress contract in the crash log.
// Uses an isolated fixture, not Minecraft or the actual screen's handler bodies.
@groovy.transform.CompileStatic
class FixtureLoader extends ClassLoader {
 FixtureLoader(){super(ClassLoader.getPlatformClassLoader())}
 Class install(String name,byte[] bytes){return defineClass(name,bytes,0,bytes.length)}
}
def zip=new ZipFile(args[0]);def n=new ClassNode()
new ClassReader(zip.getInputStream(zip.getEntry('com/dankscripts/sgjatlantis_cityshield/client/ShieldControllerScreen.class')).bytes).accept(n,0);zip.close()
String button='net/minecraft/client/gui/components/Button',iface=button+'$OnPress'
def sites=n.methods.collectMany{m->m.instructions.findAll{it instanceof InvokeDynamicInsnNode && it.desc.endsWith('L'+iface+';')}}
assert sites.size()==11:"Unexpected button callback count: ${sites.size()}"
def loader=new FixtureLoader()
def b=new ClassWriter(0);b.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,button,null,'java/lang/Object',null);b.visitEnd()
loader.install(button.replace('/','.'),b.toByteArray())
def i=new ClassWriter(0);i.visit(Opcodes.V17,Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT|Opcodes.ACC_INTERFACE,iface,null,'java/lang/Object',null)
i.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT,'m_93750_','(L'+button+';)V',null,null).visitEnd();i.visitEnd()
loader.install(iface.replace('/','.'),i.toByteArray())
int passed=0,failed=0
sites.eachWithIndex{site,idx->
 String name='ButtonContract'+idx,desc='(L'+button+';)V'
 def w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,name,null,'java/lang/Object',null)
 def target=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,'accept',desc,null,null)
 target.visitCode();target.visitInsn(Opcodes.RETURN);target.visitMaxs(0,0);target.visitEnd()
 def run=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,'click','()V',null,null);run.visitCode()
 run.visitInvokeDynamicInsn(site.name,'()L'+iface+';',site.bsm,Type.getMethodType(desc),
  new Handle(Opcodes.H_INVOKESTATIC,name,'accept',desc,false),Type.getMethodType(desc))
 run.visitInsn(Opcodes.ACONST_NULL);run.visitMethodInsn(Opcodes.INVOKEINTERFACE,iface,'m_93750_',desc,true)
 run.visitInsn(Opcodes.RETURN);run.visitMaxs(0,0);run.visitEnd();w.visitEnd()
 def cls=loader.install(name,w.toByteArray())
 try{cls.getMethod('click').invoke(null);passed++}
 catch(InvocationTargetException ex){assert ex.cause instanceof AbstractMethodError;failed++}
}
if(args.length>1 && args[1]=='expect-failure'){
 assert failed==sites.size();println "REPRODUCED: ${failed} callbacks throw the reported AbstractMethodError."
}else{assert failed==0;println "PASS: ${passed} callbacks satisfy the Forge 1.20.1 OnPress interface and invoke without AbstractMethodError."}
