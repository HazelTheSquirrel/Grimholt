package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/** Region-file persistence using the Anvil 8 KiB header and 4 KiB sectors. */
public final class VanillaAnvilRegion implements AutoCloseable {
    private static final int SECTOR=4096, HEADER=SECTOR*2, CHUNKS=1024;
    private final Path file;
    private final RandomAccessFile raf;
    private final int[] locations=new int[CHUNKS];
    private final int[] timestamps=new int[CHUNKS];

    public VanillaAnvilRegion(Path file) throws IOException {
        this.file=Objects.requireNonNull(file);
        Files.createDirectories(file.toAbsolutePath().getParent());
        raf=new RandomAccessFile(file.toFile(),"rw");
        if(raf.length()<HEADER){raf.setLength(HEADER);raf.seek(0);for(int i=0;i<CHUNKS;i++)raf.writeInt(0);for(int i=0;i<CHUNKS;i++)raf.writeInt(0);}
        raf.seek(0);for(int i=0;i<CHUNKS;i++)locations[i]=raf.readInt();for(int i=0;i<CHUNKS;i++)timestamps[i]=raf.readInt();
    }

    public synchronized void writeChunk(int chunkX,int chunkZ,byte[] nbt)throws IOException{
        int index=((chunkX&31)+(chunkZ&31)*32);
        byte[] payload=compress(nbt);
        int sectors=(payload.length+5+SECTOR-1)/SECTOR;
        if(sectors>255)throw new IOException("Chunk too large for Anvil location entry");
        int old=locations[index]&0xff;
        int offset=locations[index]>>>8;
        if(old>=sectors && offset>=2) {
            raf.seek((long)offset*SECTOR);
        } else {
            offset=(int)Math.max(2,(raf.length()+SECTOR-1)/SECTOR);
            raf.setLength((long)(offset+sectors)*SECTOR);
        }
        raf.seek((long)offset*SECTOR);
        raf.writeInt(payload.length+1);
        raf.writeByte(2); // zlib
        raf.write(payload);
        int pad=sectors*SECTOR-5-payload.length; if(pad>0)raf.write(new byte[pad]);
        locations[index]=(offset<<8)|sectors;
        timestamps[index]=(int)(System.currentTimeMillis()/1000L);
        raf.seek(index*4L);raf.writeInt(locations[index]);
        raf.seek(SECTOR+index*4L);raf.writeInt(timestamps[index]);
    }

    public synchronized byte[] readChunk(int chunkX,int chunkZ)throws IOException{
        int index=((chunkX&31)+(chunkZ&31)*32),loc=locations[index];
        if(loc==0)return null;
        int offset=loc>>>8,sectors=loc&255;
        raf.seek((long)offset*SECTOR);int length=raf.readInt();int compression=raf.readUnsignedByte();
        if(compression!=2||length<1||length>sectors*SECTOR-4)throw new IOException("Invalid Anvil chunk");
        byte[] payload=new byte[length-1];raf.readFully(payload);return decompress(payload);
    }

    private static byte[] compress(byte[] data)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(var z=new java.util.zip.DeflaterOutputStream(out)){z.write(data);}return out.toByteArray();
    }
    private static byte[] decompress(byte[] data)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(var z=new java.util.zip.InflaterInputStream(new ByteArrayInputStream(data))){z.transferTo(out);}return out.toByteArray();
    }
    @Override public synchronized void close()throws IOException{raf.close();}
    public Path file(){return file;}
}
