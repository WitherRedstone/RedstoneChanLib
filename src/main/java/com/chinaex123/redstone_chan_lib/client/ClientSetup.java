package com.chinaex123.redstone_chan_lib.client;

import com.chinaex123.redstone_chan_lib.RedstonechanLIB;
import com.chinaex123.redstone_chan_lib.client.particles.HolyMoteParticle;
import com.chinaex123.redstone_chan_lib.init.RCLParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = RedstonechanLIB.MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RCLParticles.HOLY_MOTE.get(), HolyMoteParticle.Provider::new);
    }
}
