package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstonechanLIB;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface RCLParticles {
    DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, RedstonechanLIB.MODID);

    /** 圣土光尘 */
    DeferredHolder<ParticleType<?>, SimpleParticleType> HOLY_MOTE = PARTICLE_TYPES.register("holy_mote",
            () -> new SimpleParticleType(false));

    static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
