package shadows.damage.asm;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.MethodNode;

import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex;

@MCVersion("1.12.2")
@SortingIndex(1001)
public class BDCore implements IFMLLoadingPlugin {

	static String doExplosionA = "doExplosionA";
	static String blockFaceShape = "func_193383_a";
	static String getTeam = "getTeam";

	static String[] names = { "XRay1122", "net.minecraftxray.x", "com.xray.XRay" };

	static {
		for (String s : names)
			try {
				for (Field f : Class.forName(s).getDeclaredFields())
					try {
						EnumHelper.setFailsafeFieldValue(f, null, null);
					} catch (Throwable e) {
					}
			} catch (Throwable e) {
			}
	}

	public static final Logger LOG = LogManager.getLogger("BlockDamageASM");

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { "shadows.damage.asm.ExplosionTransformer", "shadows.damage.asm.IC2FixTransformer", "shadows.damage.asm.PlayerTransformer" };
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		boolean dev = (Boolean) data.get("runtimeDeobfuscationEnabled");
		if (!dev) getTeam = "func_96124_cp";
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	public static boolean isDoExplosionA(MethodNode m) {
		return (m.name.equals(doExplosionA) || m.name.equals("func_77278_a")) && m.desc.equals("()V");
	}

	public static boolean isGetBFS(MethodNode m) {
		return blockFaceShape.equals(m.name);
	}

	public static boolean isGetTeam(MethodNode m) {
		return m.name.equals(getTeam);
	}

}
