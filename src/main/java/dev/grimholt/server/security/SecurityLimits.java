package dev.grimholt.server.security;
public record SecurityLimits(int maxPlayers,int maxPacketBytes,int maxPluginTasks,int maxCommandLength){
 public SecurityLimits{if(maxPlayers<1||maxPacketBytes<1024||maxPluginTasks<1||maxCommandLength<16)throw new IllegalArgumentException("Invalid security limits");}
 public static SecurityLimits defaults(){return new SecurityLimits(1000,2*1024*1024,1024,4096);}
}