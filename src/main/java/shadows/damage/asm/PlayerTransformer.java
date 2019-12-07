package shadows.damage.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class PlayerTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (!transformedName.equals("net.minecraft.entity.player.EntityPlayer")) return basicClass;
		BDCore.LOG.info("Transforming {}...", transformedName);
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		MethodNode getTeam = null;
		for (MethodNode m : classNode.methods) {
			if (BDCore.isGetTeam(m)) {
				getTeam = m;
				break;
			}
		}
		if (getTeam != null) {
			InsnList list = new InsnList();
			list.add(new VarInsnNode(Opcodes.ALOAD, 0));
			list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "shadows/damage/BlockDamage", "getTeam", "(Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/scoreboard/Team;", false));
			list.add(new InsnNode(Opcodes.ARETURN));
			getTeam.instructions.insert(list);
			CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(writer);
			BDCore.LOG.info("Successfully transformed {}!", transformedName);
			return writer.toByteArray();
		}
		BDCore.LOG.info("Failed transforming {}.", transformedName);
		return basicClass;
	}

}
