package dev.grimholt.server.vanilla;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-system vanilla runtime primitives. Each subsystem is deterministic and
 * can be owned by a region; no subsystem exposes Minestom state.
 */
public final class VanillaParityRuntime {
    private VanillaParityRuntime() {}

    public static final class DataComponents {
        private final Map<String,String> values=new HashMap<>();
        public DataComponents set(String key,String value){values.put(Objects.requireNonNull(key),Objects.requireNonNull(value));return this;}
        public DataComponents remove(String key){values.remove(key);return this;}
        public String get(String key){return values.get(key);}
        public boolean has(String key){return values.containsKey(key);}
        public Map<String,String> snapshot(){return Map.copyOf(values);}
    }

    public static final class ContainerProtocol {
        public record Slot(int index,VanillaItemStack item){}
        public record Transaction(int stateId,int slot,VanillaItemStack carried,VanillaItemStack result){}
        private int stateId;
        private VanillaItemStack carried=VanillaItemStack.empty();
        public int stateId(){return stateId;}
        public VanillaItemStack carried(){return carried;}
        public Transaction click(VanillaContainer c,int slot,int button){
            if(slot<0||slot>=c.inventory().size())return new Transaction(stateId,slot,carried,carried);
            VanillaItemStack old=c.inventory().get(slot);
            if(carried.isEmpty()){carried=old;c.inventory().set(slot,VanillaItemStack.empty());}
            else if(old.isEmpty()){c.inventory().set(slot,carried);carried=VanillaItemStack.empty();}
            else if(old.itemId().equals(carried.itemId())){int take=Math.min(carried.count(),old.maxStackSize()-old.count());c.inventory().set(slot,old.withCount(old.count()+take));carried=carried.withCount(carried.count()-take);}
            else {c.inventory().set(slot,carried);carried=old;}
            stateId++; return new Transaction(stateId,slot,carried,c.inventory().get(slot));
        }
    }

    public static final class RecipeSystems {
        public record FurnaceRecipe(String id,String input,String fuel,String output,int cookTicks){}
        public record BrewingRecipe(String input,String ingredient,String output){}
        public record SmithingRecipe(String template,String base,String addition,String result){}
        public record Enchantment(String id,int level,int cost){}
        private final List<FurnaceRecipe> furnaces=new ArrayList<>();
        private final List<BrewingRecipe> brewing=new ArrayList<>();
        private final List<SmithingRecipe> smithing=new ArrayList<>();
        public void furnace(FurnaceRecipe r){furnaces.add(r);} public void brewing(BrewingRecipe r){brewing.add(r);} public void smithing(SmithingRecipe r){smithing.add(r);}
        public Optional<FurnaceRecipe> furnace(String input,String fuel){return furnaces.stream().filter(r->r.input().equals(input)&&r.fuel().equals(fuel)).findFirst();}
        public Optional<BrewingRecipe> brew(String input,String ingredient){return brewing.stream().filter(r->r.input().equals(input)&&r.ingredient().equals(ingredient)).findFirst();}
        public Optional<SmithingRecipe> smith(String template,String base,String addition){return smithing.stream().filter(r->r.template().equals(template)&&r.base().equals(base)&&r.addition().equals(addition)).findFirst();}
        public int enchantingCost(int level){return Math.max(1,level*2+1);}
        public List<FurnaceRecipe> furnaces(){return List.copyOf(furnaces);}
        public List<BrewingRecipe> brewing(){return List.copyOf(brewing);}
        public List<SmithingRecipe> smithing(){return List.copyOf(smithing);}
        public void registerDefaults(){
            furnace(new FurnaceRecipe("iron","minecraft:raw_iron","minecraft:coal","minecraft:iron_ingot",200));
            furnace(new FurnaceRecipe("gold","minecraft:raw_gold","minecraft:coal","minecraft:gold_ingot",200));
            furnace(new FurnaceRecipe("beef","minecraft:beef","minecraft:coal","minecraft:cooked_beef",200));
            brewing(new BrewingRecipe("minecraft:potion","minecraft:nether_wart","minecraft:awkward_potion"));
        }
    }

    public static final class EntitySystem {
        public record BoundingBox(double width,double height,double eyeHeight){}
        public record Component(String id,Map<String,String> data){}
        private final Map<UUID,Map<String,Component>> components=new HashMap<>();
        private final Map<String,BoundingBox> boxes=new HashMap<>();
        public EntitySystem(){boxes.put("minecraft:player",new BoundingBox(.6,1.8,1.62));boxes.put("minecraft:zombie",new BoundingBox(.6,1.95,1.74));boxes.put("minecraft:skeleton",new BoundingBox(.6,1.99,1.74));boxes.put("minecraft:creeper",new BoundingBox(.6,1.7,1.445));boxes.put("minecraft:item",new BoundingBox(.25,.25,.125));}
        public BoundingBox box(String type){return boxes.getOrDefault(type,new BoundingBox(.6,1.8,1.5));}
        public void component(UUID id,Component c){components.computeIfAbsent(id,k->new HashMap<>()).put(c.id(),c);}
        public Component component(UUID id,String type){return components.getOrDefault(id,Map.of()).get(type);}
        public void remove(UUID id){components.remove(id);}
        public Map<String,Component> snapshot(UUID id){return Map.copyOf(components.getOrDefault(id,Map.of()));}
    }

    public static final class MobAi {
        public enum Goal { IDLE,WANDER,LOOK_AT_PLAYER,CHASE,ATTACK,FLEE }
        public record Decision(Goal goal,BlockPos target){}
        private final VanillaPathfinder pathfinder=new VanillaPathfinder();
        public Decision decide(VanillaWorldModel world,VanillaEntityState mob,VanillaPlayerState player){
            double dx=player.x()-mob.x(),dz=player.z()-mob.z(),d=Math.sqrt(dx*dx+dz*dz);
            if(d<2.2)return new Decision(Goal.ATTACK,new BlockPos((int)player.x(),(int)player.y(),(int)player.z()));
            if(d<16)return new Decision(Goal.CHASE,new BlockPos((int)player.x(),(int)player.y(),(int)player.z()));
            return new Decision(Goal.WANDER,new BlockPos((int)mob.x()+1,(int)mob.y(),(int)mob.z()));
        }
        public List<BlockPos> path(VanillaWorldModel world,BlockPos a,BlockPos b){return pathfinder.find(world,a,b,4096);}
    }

    public static final class Combat {
        public record Attack(float base,float critical,float armorReduction,float finalDamage){}
        public Attack calculate(float base,boolean critical,int armor,float toughness){
            float damage=critical?base*1.5f:base;
            float reduction=Math.min(20f,Math.max(armor- (armor*2>20?0:0),0));
            float finalDamage=Math.max(0,damage*(1f-reduction/25f));
            return new Attack(base,critical?damage:base,reduction,finalDamage);
        }
        public void melee(VanillaEntityState target,float base,boolean critical,int armor){target.damage(calculate(base,critical,armor,0).finalDamage());}
        public Vec3 knockback(Vec3 direction,float strength){Vec3 d=direction.normalize();return new Vec3(d.x()*strength,.35,d.z()*strength);}
    }

    public static final class PoiAndVillagers {
        public record Poi(BlockPos pos,String type,int freeTickets){}
        public record Trade(VanillaItemStack input,VanillaItemStack secondary,VanillaItemStack output,int maxUses){}
        private final Map<BlockPos,Poi> pois=new HashMap<>();
        private final Map<UUID,List<Trade>> trades=new HashMap<>();
        public void poi(Poi p){pois.put(p.pos(),p);} public Poi poi(BlockPos p){return pois.get(p);}
        public void trade(UUID villager,Trade t){trades.computeIfAbsent(villager,k->new ArrayList<>()).add(t);}
        public List<Trade> trades(UUID villager){return List.copyOf(trades.getOrDefault(villager,List.of()));}
        public Map<BlockPos,Poi> poiSnapshot(){return Map.copyOf(pois);}
    }

    public static final class Raids {
        public record Raid(UUID id,BlockPos center,int wave,int totalWaves,boolean active,boolean victory){}
        private final Map<UUID,Raid> raids=new HashMap<>();
        public Raid start(BlockPos center,int waves){UUID id=UUID.randomUUID();Raid r=new Raid(id,center,0,waves,true,false);raids.put(id,r);return r;}
        public Raid advance(UUID id){Raid r=require(id);int w=r.wave()+1;Raid n=w>=r.totalWaves()?new Raid(id,r.center(),w,r.totalWaves(),false,true):new Raid(id,r.center(),w,r.totalWaves(),true,false);raids.put(id,n);return n;}
        public Raid fail(UUID id){Raid r=require(id);Raid n=new Raid(id,r.center(),r.wave(),r.totalWaves(),false,false);raids.put(id,n);return n;}
        public Raid raid(UUID id){return raids.get(id);} public Collection<Raid> snapshot(){return List.copyOf(raids.values());}
        private Raid require(UUID id){Raid r=raids.get(id);if(r==null)throw new IllegalArgumentException("unknown raid");return r;}
    }

    public static final class Dimensions {
        public record Portal(String from, String to, double scale){}
        public Portal portal(VanillaGameplaySystems.Dimension from,VanillaGameplaySystems.Dimension to){
            if(from==to)return new Portal(from.name(),to.name(),1);
            if(from==VanillaGameplaySystems.Dimension.OVERWORLD&&to==VanillaGameplaySystems.Dimension.NETHER)return new Portal(from.name(),to.name(),8);
            if(from==VanillaGameplaySystems.Dimension.NETHER&&to==VanillaGameplaySystems.Dimension.OVERWORLD)return new Portal(from.name(),to.name(),.125);
            return new Portal(from.name(),to.name(),1);
        }
        public BlockPos transform(BlockPos p,VanillaGameplaySystems.Dimension from,VanillaGameplaySystems.Dimension to){
            double s=portal(from,to).scale();return new BlockPos((int)Math.floor(p.x()*s),p.y(),(int)Math.floor(p.z()*s));
        }
        public boolean bedWorks(VanillaGameplaySystems.Dimension d){return d==VanillaGameplaySystems.Dimension.OVERWORLD;}
        public boolean respawnAnchorWorks(VanillaGameplaySystems.Dimension d){return d==VanillaGameplaySystems.Dimension.NETHER;}
    }

    public static final class Loot {
        public record Entry(String item,int min,int max,int weight){}
        private final Map<String,List<Entry>> tables=new HashMap<>();
        public void register(String id,List<Entry> entries){tables.put(id,List.copyOf(entries));}
        public List<VanillaItemStack> roll(String id,long seed){
            List<Entry> e=tables.getOrDefault(id,List.of());if(e.isEmpty())return List.of();
            Random r=new Random(seed);int total=e.stream().mapToInt(Entry::weight).sum(),pick=r.nextInt(Math.max(1,total)),acc=0;
            for(Entry x:e){acc+=x.weight();if(pick<acc)return List.of(new VanillaItemStack(x.item(),x.min()+r.nextInt(Math.max(1,x.max()-x.min()+1)),64,Map.of()));}
            return List.of();
        }
    }

    public static final class Advancements {
        public record Criterion(String id,Set<String> requirements){}
        public record Advancement(String id,String parent,List<Criterion> criteria){}
        private final Map<String,Advancement> defs=new HashMap<>();private final Map<UUID,Set<String>> progress=new HashMap<>();
        public void register(Advancement a){defs.put(a.id(),a);}
        public boolean grant(UUID p,String id){if(!defs.containsKey(id))return false;return progress.computeIfAbsent(p,k->new HashSet<>()).add(id);}
        public boolean has(UUID p,String id){return progress.getOrDefault(p,Set.of()).contains(id);}
        public Map<String,Advancement> definitions(){return Map.copyOf(defs);}
    }

    public static final class DataPacks {
        public record Function(String id,List<String> commands){}
        private final Map<String,Function> functions=new HashMap<>();private final Set<String> enabled=new LinkedHashSet<>();
        public void register(Function f){functions.put(f.id(),f);} public void enable(String id){if(functions.containsKey(id))enabled.add(id);}
        public List<String> tickFunctions(){List<String> out=new ArrayList<>();for(String id:enabled)out.addAll(functions.get(id).commands());return List.copyOf(out);}
        public Function function(String id){return functions.get(id);}
    }

    public static final class Commands {
        public record Command(String literal,String permission){}
        private final Map<String,Command> commands=new TreeMap<>();
        public void register(String literal,String permission){commands.put(literal,new Command(literal,permission));}
        public boolean canExecute(String literal,String permission){Command c=commands.get(literal);return c!=null&&(c.permission().equals("minecraft:all")||c.permission().equals(permission)||permission.equals("*"));}
        public Set<String> literals(){return Collections.unmodifiableSet(commands.keySet());}
        public void registerVanillaRoots(){for(String c:List.of("advancement","attribute","ban","ban-ip","clear","clone","data","datapack","debug","defaultgamemode","difficulty","effect","enchant","execute","experience","fill","gamemode","give","gamerule","item","kill","list","locate","loot","me","particle","playsound","recipe","reload","say","schedule","scoreboard","setblock","spawnpoint","spreadplayers","stop","summon","tag","team","teleport","time","title","trigger","weather","whitelist"))register(c,"minecraft:all");}
    }

    public static final class Persistence {
        public record WorldHeader(int format,String snapshot,long seed,String dimension){}
        public WorldHeader header(VanillaWorldModel world){return new WorldHeader(2,VanillaSnapshot.VERSION,world.seed(),world.dimension().name());}
        public byte[] checksum(byte[] data){try{return MessageDigest.getInstance("SHA-256").digest(data);}catch(Exception e){throw new IllegalStateException(e);}}
        public String encodeBlockState(BlockState s){return s.id()+"|"+s.properties();}
        public String encodeItem(VanillaItemStack s){return s.itemId()+"|"+s.count()+"|"+s.maxStackSize()+"|"+s.components();}
    }

    public static final class NetworkParity {
        public enum Phase { HANDSHAKE, STATUS, LOGIN, CONFIGURATION, PLAY, DISCONNECT }
        public record PacketContract(String name,Phase phase,int maxBytes){}
        private final Map<String,PacketContract> packets=new ConcurrentHashMap<>();
        public NetworkParity(){for(String p:List.of("minecraft:configuration_start","minecraft:registry_data","minecraft:finish_configuration","minecraft:login","minecraft:player_position","minecraft:set_container_content","minecraft:container_click","minecraft:chunk_data","minecraft:block_update","minecraft:entity_position","minecraft:game_event"))register(new PacketContract(p,p.contains("configuration")||p.equals("minecraft:registry_data")?Phase.CONFIGURATION:Phase.PLAY,2097152));}
        public void register(PacketContract p){packets.put(p.name(),p);}
        public PacketContract packet(String name){return packets.get(name);}
        public boolean accepts(String name,Phase phase,int bytes){PacketContract p=packet(name);return p!=null&&p.phase()==phase&&bytes>=0&&bytes<=p.maxBytes();}
    }

    public static final class ReferenceTests {
        public record Case(String name,boolean passed,String detail){}
        private final List<Case> cases=new ArrayList<>();
        public void assertCase(String name,boolean condition,String detail){cases.add(new Case(name,condition,detail));if(!condition)throw new AssertionError(name+": "+detail);}
        public List<Case> run(VanillaWorldModel world){
            assertCase("snapshot",VanillaSnapshot.VERSION.equals("26.4 Snapshot 3"),VanillaSnapshot.VERSION);
            assertCase("air",world.blockRegistry().contains("minecraft:air"),"air registry");
            assertCase("chunk-section",world.chunk(0,0).sectionCount()==0,"lazy sections");
            assertCase("fluid-state",world.getFluid(new BlockPos(0,0,0)).isEmpty(),"empty fluid");
            return List.copyOf(cases);
        }
    }

    public static final class Stress {
        public record Result(int regions,int operations,long nanos){}
        public Result run(VanillaWorldModel world,int operations){
            long start=System.nanoTime();for(int i=0;i<operations;i++){int x=i&255,z=(i>>>8)&255;world.setBlock(new BlockPos(x,64,z),world.blockRegistry().defaultState("minecraft:stone"));world.getBlock(new BlockPos(x,64,z));}
            return new Result(1,operations,System.nanoTime()-start);
        }
    }
}
