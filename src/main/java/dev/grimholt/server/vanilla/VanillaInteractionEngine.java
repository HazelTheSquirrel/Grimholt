package dev.grimholt.server.vanilla;
import java.util.*;
public final class VanillaInteractionEngine{
 public enum Hand{MAIN_HAND,OFF_HAND}public enum Result{SUCCESS,PASS,FAIL,CONSUME}
 public Result useItem(VanillaPlayerState p,VanillaItemStack i,Hand h){Objects.requireNonNull(p);Objects.requireNonNull(i);if(i.isEmpty())return Result.PASS;if(i.itemId().equals("minecraft:apple")||i.itemId().equals("minecraft:bread")){if(p.hunger()>=20)return Result.PASS;p.hunger(4);return Result.CONSUME;}return Result.SUCCESS;}
 public Result placeBlock(VanillaWorldState w,BlockPos pos,BlockState s){if(!VanillaBlockBehaviors.isSolid(s)&&!s.id().equals("minecraft:water")&&!s.id().equals("minecraft:lava"))return Result.FAIL;if(!w.getBlock(pos).id().equals("minecraft:air"))return Result.FAIL;w.setBlock(pos,s);return Result.SUCCESS;}
 public Result breakBlock(VanillaWorldState w,BlockPos pos){if(w.getBlock(pos).id().equals("minecraft:air"))return Result.PASS;w.setBlock(pos,BlockState.of("minecraft:air"));return Result.SUCCESS;}
 public VanillaDamage attack(VanillaEntityState a,VanillaEntityState t,float damage){if(damage<0)throw new IllegalArgumentException();t.damage(damage);return VanillaDamage.of(VanillaDamageSource.MOB_ATTACK.name().toLowerCase(Locale.ROOT),damage);}
}