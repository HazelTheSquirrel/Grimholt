package dev.grimholt.server.vanilla;

public final class VanillaWorldBorder {
    private double centerX,centerZ,size=59_999_968;
    private long warningTime=15, warningBlocks=5;
    public double centerX(){return centerX;} public double centerZ(){return centerZ;} public double size(){return size;}
    public void center(double x,double z){centerX=x;centerZ=z;}
    public void size(double size){if(size<=0)throw new IllegalArgumentException();this.size=size;}
    public void warningTime(long t){if(t<0)throw new IllegalArgumentException();warningTime=t;}
    public void warningBlocks(long b){if(b<0)throw new IllegalArgumentException();warningBlocks=b;}
    public boolean contains(double x,double z){double h=size/2;return x>=centerX-h&&x<=centerX+h&&z>=centerZ-h&&z<=centerZ+h;}
    public long warningTime(){return warningTime;} public long warningBlocks(){return warningBlocks;}
}
