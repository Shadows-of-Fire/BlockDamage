package shadows.damage.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class IC2FixTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (transformedName.equals("ic2.core.block.BlockTileEntity")) return transformTE(transformedName, basicClass);
		if ("ic2.core.ExplosionIC2".equals(transformedName)) return transformEx(transformedName, basicClass);
		return basicClass;
	}

	static byte[] transformTE(String transformedName, byte[] basicClass) {
		BDCore.LOG.info("Transforming {}...", transformedName);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode getBFS = null;
		for (MethodNode m : classNode.methods) {
			if (BDCore.isGetBFS(m)) {
				getBFS = m;
				break;
			}
		}
		if (getBFS != null) {
			InsnList list = new InsnList();
			for (int i = 1; i < 5; i++)
				list.add(new VarInsnNode(Opcodes.ALOAD, i));
			list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "shadows/damage/IC2Handler", "getBlockFaceShape", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/block/state/BlockFaceShape;", false));
			list.add(new InsnNode(Opcodes.ARETURN));
			getBFS.instructions.insert(list);
			CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(writer);
			BDCore.LOG.info("Successfully transformed {}!", transformedName);
			return writer.toByteArray();
		}
		BDCore.LOG.info("Failed transforming {}.", transformedName);
		return basicClass;
	}

	static byte[] transformEx(String transformedName, byte[] basicClass) {
		BDCore.LOG.info("Transforming {}...", transformedName);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode shootRay = null;
		for (MethodNode m : classNode.methods) {
			if (m.name.equals("shootRay")) {
				shootRay = m;
			}
		}

		for (AbstractInsnNode n : shootRay.instructions.toArray()) {
			if (n.getOpcode() == Opcodes.DLOAD && n.getNext().getOpcode() == Opcodes.DLOAD && ((VarInsnNode) n).var == 27) {
				VarInsnNode nxt = (VarInsnNode) n.getNext();
				if (nxt.var == 11) {
					AbstractInsnNode inject = nxt.getNext().getNext(); //This is the IFLE 160 node.  We inject after it to ensure we always break one block.
					InsnList list = new InsnList();
					list.add(new VarInsnNode(Opcodes.ALOAD, 0));
					list.add(new VarInsnNode(Opcodes.ILOAD, 23));
					list.add(new VarInsnNode(Opcodes.ILOAD, 22));
					list.add(new VarInsnNode(Opcodes.ILOAD, 24));
					list.add(new InsnNode(Opcodes.ICONST_0));
					list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "ic2/core/ExplosionIC2", "destroyUnchecked", "(IIIZ)V", false));
					shootRay.instructions.insertBefore(inject, list);
					break;
				}
			}
		}

		InsnList list = new InsnList();
		list.add(new VarInsnNode(Opcodes.ALOAD, 0));
		list.add(new VarInsnNode(Opcodes.ILOAD, 13));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "shadows/damage/IC2Handler", "doDamage", "(Lic2/core/ExplosionIC2;Z)Z", false));
		list.add(new VarInsnNode(Opcodes.ISTORE, 13));
		shootRay.instructions.insert(list);

		CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		BDCore.LOG.info("Successfully transformed {}!", transformedName);
		return writer.toByteArray();
	}

}
