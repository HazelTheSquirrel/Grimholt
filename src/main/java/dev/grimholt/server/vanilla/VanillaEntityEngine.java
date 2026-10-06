package dev.grimholt.server.vanilla;

import java.util.UUID;

public final class VanillaEntityEngine {
    public void tick(VanillaEntityState entity, VanillaFluidState fluid) {
        if(entity.removed()) return;
        Vec3 v=new Vec3(entity.velocityX(),entity.velocityY(),entity.velocityZ());
        v=VanillaPhysics.applyFluidMotion(v,fluid);
        if(fluid.isEmpty()) v=VanillaPhysics.applyAirMotion(v,entity.y()<=0.0);
        entity.velocity(v.x(),v.y(),v.z());
        entity.move(v.x(),v.y(),v.z());
    }
    public void damage(VanillaEntityState entity,float amount,VanillaDamageSource source){entity.damage(amount);}
    public VanillaEntityState spawn(UUID uuid,String type,double x,double y,double z){
        float health=switch(type){case "minecraft:frostbite","minecraft:zombie","minecraft:husk"->20f;case "minecraft:creeper"->20f;case "minecraft:skeleton"->20f;default->20f;};
        return new VanillaEntityState(uuid,type,x,y,z,health);
    }
}
