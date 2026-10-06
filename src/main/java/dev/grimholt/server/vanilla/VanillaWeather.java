package dev.grimholt.server.vanilla;

public final class VanillaWeather {
    private boolean raining,thundering; private int rainTime=0,thunderTime=0;
    public boolean raining(){return raining;} public boolean thundering(){return thundering;}
    public int rainTime(){return rainTime;} public int thunderTime(){return thunderTime;}
    public void setRain(boolean value,int duration){raining=value;rainTime=Math.max(0,duration);}
    public void setThunder(boolean value,int duration){thundering=value;thunderTime=Math.max(0,duration);}
    public void tick(){if(rainTime>0&&--rainTime==0)raining=!raining;if(thunderTime>0&&--thunderTime==0)thundering=!thundering;if(!raining)thundering=false;}
}
