package dev.grimholt.server.vanilla;

public final class VanillaCombatEngine {
    public void attack(VanillaPlayerState attacker,VanillaEntityState target,float baseDamage){
        if(attacker.gameMode()==VanillaPlayerState.GameMode.SPECTATOR) return;
        target.damage(Math.max(0,baseDamage));
    }
    public void applyDamage(VanillaPlayerState player,float amount,VanillaDamageSource source){player.damage(Math.max(0,amount));}
    public void applyFreezing(VanillaPlayerState player,FreezingState state,int duration){state.apply(duration);}
    public void snowballKnockback(VanillaEntityState target,Vec3 direction){Vec3 d=direction.normalize();target.velocity(d.x()*0.4,0.15,d.z()*0.4);}
}
