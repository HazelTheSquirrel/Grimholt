package dev.grimholt.server.vanilla;
import java.util.Arrays;
public final class VanillaChunkSection{
 public static final int SIZE=16; private final int sectionY; private final int[] states=new int[4096]; private int nonAir;
 public VanillaChunkSection(int sectionY,int airStateId){this.sectionY=sectionY;Arrays.fill(states,airStateId);}
 public int sectionY(){return sectionY;} public int stateId(int x,int y,int z){return states[index(x,y,z)];}
 public void stateId(int x,int y,int z,int id,int air){int i=index(x,y,z),old=states[i];if(old==id)return;if(old==air&&id!=air)nonAir++;if(old!=air&&id==air)nonAir--;states[i]=id;}
 public int nonAirCount(){return nonAir;} public int[] copyStates(){return states.clone();}
 private static int index(int x,int y,int z){if((x|y|z)<0||x>15||y>15||z>15)throw new IndexOutOfBoundsException();return(y<<8)|(z<<4)|x;}
}