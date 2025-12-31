package net.montoyo.wd.config.annoconfg;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.montoyo.wd.config.annoconfg.annotation.format.*;
import net.montoyo.wd.config.annoconfg.annotation.value.Default;
import net.montoyo.wd.config.annoconfg.annotation.value.DoubleRange;
import net.montoyo.wd.config.annoconfg.annotation.value.IntRange;
import net.montoyo.wd.config.annoconfg.annotation.value.LongRange;
import net.montoyo.wd.config.annoconfg.handle.UnsafeHandle;
import net.montoyo.wd.config.annoconfg.util.EnumType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class AnnoCFG {
	private ModConfigSpec mySpec;

	private final HashMap<String, ConfigEntry> handles = new HashMap<>();

	private static final ArrayList<AnnoCFG> configs = new ArrayList<>();
	private final Method postInit;

	public AnnoCFG(IEventBus bus, Class<?> clazz) {
		bus.addListener(this::onConfigChange);
		ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();
		setup("", configBuilder, clazz);
		configs.add(this);

		Method m = null;
		try {
			m = clazz.getDeclaredMethod("postLoad");
		} catch (Throwable ignored) {
		}
		postInit = m;

		Config configDescriptor = clazz.getAnnotation(Config.class);
		if (configDescriptor != null) {
			String pth = configDescriptor.path();
			if (!pth.isEmpty()) pth = pth + "/";
			switch (configDescriptor.type()) {
				case SERVER -> create(ModConfig.Type.SERVER, pth + ModLoadingContext.get().getActiveNamespace() + "_server.toml");
				case CLIENT -> create(ModConfig.Type.CLIENT, pth + ModLoadingContext.get().getActiveNamespace() + "_client.toml");
				case COMMON -> create(ModConfig.Type.COMMON, pth + ModLoadingContext.get().getActiveNamespace() + "_common.toml");
				default -> throw new RuntimeException("wat");
			}
		}
	}

	protected void setupCommentsAndTranslations(AnnotatedElement element, ModConfigSpec.Builder builder, String... additionalLines) {
		Translation translation = element.getAnnotation(Translation.class);
		Comment comment = element.getAnnotation(Comment.class);

		StringBuilder builder1 = new StringBuilder();
		if (comment != null) {
			for (int i = 0; i < comment.value().length; i++) {
				String s = comment.value()[i];
				builder1.append(s);
				if (i != comment.value().length - 1)
					builder1.append("\n");
			}
		}
		for (String additionalLine : additionalLines) builder1.append(additionalLine);
		if (!builder1.isEmpty())
			builder.comment(builder1.toString());

		if (translation != null)
			builder.translation(translation.value());
	}

	public void setup(String dir, ModConfigSpec.Builder builder, Class<?> clazz) {
		if (dir.startsWith(".")) dir = dir.substring(1);

		for (Field field : clazz.getFields()) {
			if (field.canAccess(null)) {
				Skip skip = field.getAnnotation(Skip.class);
				if (skip != null) continue;

				Name name = field.getAnnotation(Name.class);

				String nameStr = field.getName();
				if (name != null) nameStr = name.value();

				setupCommentsAndTranslations(field, builder);

				Supplier<?> value;

				Default defaultValue = field.getAnnotation(Default.class);
				try {
					switch (EnumType.forClass(field.getType())) {
						case INT -> {
							IntRange range = field.getAnnotation(IntRange.class);
							int v = defaultValue.valueI();
							if (range != null) {
								int min = range.minV();
								int max = range.maxV();

								value = builder.defineInRange(nameStr, v, min, max);
							} else {
								value = builder.define(nameStr, v);
							}
						}
						case LONG -> {
							LongRange range = field.getAnnotation(LongRange.class);
							long v = defaultValue.valueL();
							if (range != null) {
								long min = range.minV();
								long max = range.maxV();

								value = builder.defineInRange(nameStr, v, min, max);
							} else {
								value = builder.define(nameStr, v);
							}
						}
						case DOUBLE -> {
							DoubleRange range = field.getAnnotation(DoubleRange.class);
							double v = defaultValue.valueD();
							if (range != null) {
								double min = range.minV();
								double max = range.maxV();

								value = builder.defineInRange(nameStr, v, min, max);
							} else {
								value = builder.define(nameStr, v);
							}
						}
						case BOOLEAN -> {
							boolean b = defaultValue.valueBoolean();
							value = builder.define(nameStr, b);
						}
						case OTHER -> {
							Class<?> fieldType = field.getType();
							if (fieldType.equals(String[].class)) {
								Supplier<String> sup = builder.define(nameStr, defaultValue.valueStr());
								value = () -> {
									String v = sup.get();
									return v.split(",");
								};
							} else if (fieldType.equals(String.class)) {
								value = builder.define(nameStr, defaultValue.valueStr());
							} else
								throw new RuntimeException("NYI " + field.getType());
						}
						default -> throw new RuntimeException("NYI " + field.getType());
					}
				} catch (NullPointerException npe) {
					String inf = "";
					if (npe.getMessage().contains("\"value.Default\""))
						inf = " this is likely due to a missing default.";
					throw new RuntimeException("A null pointer occurred on " + field.getName() + inf, npe);
				}

				Object o;
				try {
					// without this line, this system freaks out due to using theUnsafe
					//noinspection UnusedAssignment
					o = field.get(null);
				} catch (Throwable ignored) {
				}
				UnsafeHandle handle = new UnsafeHandle(field);
				o = handle.get();
				handle.set(o);

				//noinspection FunctionalExpressionCanBeFolded
				handles.put(dir + "." + nameStr, new ConfigEntry(
						handle, value::get
				));
			}
		}

		// TODO: check if the nested class is a direct nesting
		for (Class<?> nestMember : clazz.getClasses()) {
			if (nestMember == clazz) continue;
			if (!nestMember.getName().startsWith(clazz.getName())) continue;
			CFGSegment segment = nestMember.getAnnotation(CFGSegment.class);
			if (segment == null) {
				System.out.println(nestMember);
				throw new RuntimeException("NYI: default name");
			}
			String name = segment.value();

			setupCommentsAndTranslations(nestMember, builder);

			builder.push(name);
			setup(dir + "." + name, builder, nestMember);
			builder.pop();
		}

		mySpec = builder.build();
	}

	public void onConfigChange(ModConfigEvent event) {
		if (
				event.getConfig().getSpec().equals(mySpec) ||
						event.getConfig().getSpec() == mySpec
		) {
			for (String s : handles.keySet()) {
				ConfigEntry entry = handles.get(s);
				entry.handle.set(entry.supplier.get());
			}
		}
		try {
			postInit.invoke(null);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}

	public void create(ModConfig.Type type, String file) {
		ModLoadingContext.get().getActiveContainer().registerConfig(type, mySpec, file);
	}
}
