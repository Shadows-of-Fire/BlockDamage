package shadows.damage.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex;

@SortingIndex(5000)
public class ExplosionTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if ("net.minecraft.world.Explosion".equals(transformedName) || "org.spongepowered.common.mixin.core.world.MixinExplosion".equals(transformedName)) return transform(transformedName, basicClass);
		if ("com.vicmatskiv.weaponlib.Explosion".equals(transformedName)) return transformVic(transformedName, basicClass);
		if ("ic2.core.ExplosionIC2".equals(transformedName)) return transformIC2(transformedName, basicClass);
		if (transformedName.equals("icbm.classic.content.explosive.blast.threaded.BlastThreaded")) return transformICBM(transformedName, basicClass);
		if (transformedName.equals("icbm.classic.content.explosive.blast.BlastTNT")) return transformICBMBlast(transformedName, basicClass);
		if (transformedName.equals("icbm.classic.content.explosive.blast.BlastBreech")) return transformICBMBlast(transformedName, basicClass);
		return basicClass;
	}

	static boolean run = false;

	static byte[] transform(String name, byte[] basicClass) {
		if (run) return basicClass;
		run = true;
		BDCore.LOG.info("Transforming {}...", name);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode doExplosionA = null;
		for (MethodNode m : classNode.methods) {
			if (BDCore.isDoExplosionA(m)) {
				doExplosionA = m;
				break;
			}
		}
		if (doExplosionA != null) {
			int count = 0;
			for (AbstractInsnNode n : doExplosionA.instructions.toArray()) {
				if (n.getOpcode() == Opcodes.FCONST_0) {
					if (++count == 2) {
						doExplosionA.instructions.set(n, new LdcInsnNode(-Float.MAX_VALUE));
						CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
						classNode.accept(writer);
						BDCore.LOG.info("Successfully transformed {}!", name);
						return writer.toByteArray();
					}
				}
			}
		}
		BDCore.LOG.info("Failed transforming {}.", name);
		return basicClass;
	}

	static byte[] transformVic(String name, byte[] basicClass) {
		BDCore.LOG.info("Transforming {}...", name);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode doExplosionA = null;
		for (MethodNode m : classNode.methods) {
			if (m.name.equals("doExplosionA")) {
				doExplosionA = m;
				break;
			}
		}

		if (doExplosionA != null) {
			int count = 0;
			for (AbstractInsnNode n : doExplosionA.instructions.toArray()) {
				if (n.getOpcode() == Opcodes.FCONST_0) {
					if (++count == 2) {
						doExplosionA.instructions.set(n, new LdcInsnNode(-Float.MAX_VALUE));
					}
				}
			}
		}

		if (doExplosionA != null) {
			InsnList insns = new InsnList();
			insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
			insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "shadows/damage/VicHandler", "handle", "(Lcom/vicmatskiv/weaponlib/Explosion;)V", false));
			AbstractInsnNode insertAt = null;
			for (AbstractInsnNode n : doExplosionA.instructions.toArray()) {
				if (n.getOpcode() == Opcodes.NEW && ((TypeInsnNode) n).desc.equals("com/vicmatskiv/weaponlib/compatibility/CompatibleVec3")) {
					insertAt = n;
					break;
				}
			}
			doExplosionA.instructions.insert(insertAt, insns);
			CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(writer);
			BDCore.LOG.info("Successfully transformed {}!", name);
			return writer.toByteArray();

		}
		BDCore.LOG.info("Failed transforming {}.", name);
		return basicClass;
	}

	static byte[] transformIC2(String name, byte[] basicClass) {
		BDCore.LOG.info("Transforming {}...", name);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode doExplosion = null;
		for (MethodNode m : classNode.methods) {
			if (m.name.equals("doExplosion")) {
				doExplosion = m;
				break;
			}
		}
		if (doExplosion != null) {
			MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "shadows/damage/IC2Handler", "handle", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z", false);
			AbstractInsnNode replace = null;
			for (AbstractInsnNode n : doExplosion.instructions.toArray()) {
				if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) n).name.equals("onBlockExploded")) {
					replace = n;
					break;
				}
			}

			if (replace == null) throw new RuntimeException("Failed to find replace node!");
			doExplosion.instructions.set(replace, node);
			InsnList list = new InsnList();
			list.add(new VarInsnNode(Opcodes.ISTORE, 11));
			list.add(new VarInsnNode(Opcodes.ALOAD, 18));
			list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "clear", "()V", true));
			doExplosion.instructions.insert(node, list);
			CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(writer);
			BDCore.LOG.info("Successfully transformed {}!", name);
			return writer.toByteArray();

		}
		BDCore.LOG.info("Failed transforming {}.", name);
		return basicClass;
	}

	static byte[] transformICBM(String name, byte[] basicClass) {
		BDCore.LOG.info("Transforming {}...", name);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode destroyBlock = null;
		for (MethodNode m : classNode.methods) {
			if (m.name.equals("destroyBlock")) {
				destroyBlock = m;
				break;
			}
		}
		if (destroyBlock != null) {
			MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "shadows/damage/IC2Handler", "handle", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z", false);
			AbstractInsnNode replace = null;
			for (AbstractInsnNode n : destroyBlock.instructions.toArray()) {
				if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) n).name.equals("onBlockExploded")) {
					replace = n;
					break;
				}
			}
			if (replace == null) throw new RuntimeException("Failed to find replace node!");
			destroyBlock.instructions.set(replace, node);
			destroyBlock.instructions.insert(node, new InsnNode(Opcodes.POP));
			CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(writer);
			BDCore.LOG.info("Successfully transformed {}!", name);
			return writer.toByteArray();
		}
		BDCore.LOG.info("Failed transforming {}.", name);
		return basicClass;
	}

	static byte[] transformICBMBlast(String name, byte[] basicClass) {
		BDCore.LOG.info("Transforming {}...", name);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode calculateDamage = null;
		for (MethodNode m : classNode.methods) {
			if (m.name.equals("calculateDamage")) {
				calculateDamage = m;
				break;
			}
		}
		if (calculateDamage != null) {
			int count = 0;
			for (AbstractInsnNode n : calculateDamage.instructions.toArray()) {
				if (n.getOpcode() == Opcodes.FCONST_0) {
					if (++count == 2) {
						calculateDamage.instructions.set(n, new LdcInsnNode(-Float.MAX_VALUE));
						CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
						classNode.accept(writer);
						BDCore.LOG.info("Successfully transformed {}!", name);
						return writer.toByteArray();
					}
				}
			}
		}
		BDCore.LOG.info("Failed transforming {}.", name);
		return basicClass;
	}

}
