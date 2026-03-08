package fi.dy.masa.litematica.mixin.hud;

@Deprecated
//@Mixin(value = DebugHudEntries.class)
public abstract class MixinDebugHudEntries
{
//	@Shadow @Final public static Map<Identifier, DebugHudEntry> ENTRIES;
//	@Mutable @Shadow @Final public static Map<DebugProfileType, Map<Identifier, DebugHudEntryVisibility>> PROFILES;
//
//	@Inject(method = "<clinit>", at = @At("TAIL"))
//	private static void litematica_registerDebugLines(CallbackInfo ci)
//	{
//		if (LitematicaDebugHud.INSTANCE.getMode() == DebugHudMode.VANILLA)
//		{
//			Map<Identifier, DebugHudEntryVisibility> defMap = PROFILES.get(DebugProfileType.DEFAULT);
//			Map<Identifier, DebugHudEntryVisibility> perfMap = PROFILES.get(DebugProfileType.PERFORMANCE);
//			ImmutableMap.Builder<Identifier, DebugHudEntryVisibility> builder = new ImmutableMap.Builder<>();
//
//			builder.putAll(defMap);
//			builder.put(LitematicaDebugHud.LITEMATICA_DEBUG, DebugHudEntryVisibility.IN_F3);
//			ENTRIES.put(LitematicaDebugHud.LITEMATICA_DEBUG, LitematicaDebugHud.INSTANCE);
//
//			PROFILES = ImmutableMap.of(
//					DebugProfileType.DEFAULT, builder.build(),
//					DebugProfileType.PERFORMANCE, perfMap
//			);
//		}
//	}
}
