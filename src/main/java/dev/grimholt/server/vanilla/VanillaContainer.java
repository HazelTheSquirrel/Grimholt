package dev.grimholt.server.vanilla;

public final class VanillaContainer {
    private final VanillaInventory inventory;
    public VanillaContainer(int slots){inventory=new VanillaInventory(slots);}
    public VanillaInventory inventory(){return inventory;}
    public boolean insert(VanillaItemStack incoming){
        if(incoming.isEmpty()) return true;
        int remaining=incoming.count();
        for(int i=0;i<inventory.size()&&remaining>0;i++){
            VanillaItemStack current=inventory.get(i);
            if(!current.isEmpty()&&!current.itemId().equals(incoming.itemId())) continue;
            if(current.isEmpty()){int take=Math.min(remaining,incoming.maxStackSize());inventory.set(i,incoming.withCount(take));remaining-=take;}
            else {int free=current.maxStackSize()-current.count();int take=Math.min(free,remaining);if(take>0){inventory.set(i,current.withCount(current.count()+take));remaining-=take;}}
        }
        return remaining==0;
    }
}
