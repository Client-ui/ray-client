package dev.ray.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets Fullbright write the brightness (gamma) option directly. SimpleOption#setValue refuses anything above 100%,
 * which is why the in-game slider can't go brighter than "Bright".
 */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor {
	@Accessor("value")
	void ray$setRawValue(Object value);
}
