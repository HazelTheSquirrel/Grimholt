package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Minimal binary NBT implementation for server-owned persistence. */
public final class VanillaNbt {
    public static final byte END=0, BYTE=1, SHORT=2, INT=3, LONG=4, FLOAT=5, DOUBLE=6,
        BYTE_ARRAY=7, STRING=8, LIST=9, COMPOUND=10, INT_ARRAY=11, LONG_ARRAY=12;
    private VanillaNbt() {}

    public record Tag(byte type, Object value) {}

    public static byte[] write(Tag root) {
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            DataOutputStream d=new DataOutputStream(out);
            writeTag(d, "", root);
            d.flush();
            return out.toByteArray();
        } catch(IOException e){throw new UncheckedIOException(e);}
    }

    public static Tag read(byte[] bytes) {
        try { return readTag(new DataInputStream(new ByteArrayInputStream(bytes))); }
        catch(IOException e){throw new UncheckedIOException(e);}
    }

    public static Tag compound(Map<String,Tag> values){return new Tag(COMPOUND,Map.copyOf(values));}
    public static Tag string(String value){return new Tag(STRING,value);}
    public static Tag integer(int value){return new Tag(INT,value);}
    public static Tag longValue(long value){return new Tag(LONG,value);}
    public static Tag list(byte elementType,List<Tag> values){return new Tag(LIST,new ListValue(elementType,List.copyOf(values)));}

    public record ListValue(byte elementType,List<Tag> values){}

    @SuppressWarnings("unchecked")
    private static void writeTag(DataOutputStream d,String name,Tag tag)throws IOException{
        d.writeByte(tag.type());
        if(tag.type()==END)return;
        d.writeUTF(name);
        switch(tag.type()){
            case BYTE->d.writeByte((Byte)tag.value());
            case SHORT->d.writeShort((Short)tag.value());
            case INT->d.writeInt((Integer)tag.value());
            case LONG->d.writeLong((Long)tag.value());
            case FLOAT->d.writeFloat((Float)tag.value());
            case DOUBLE->d.writeDouble((Double)tag.value());
            case STRING->d.writeUTF((String)tag.value());
            case LIST->{ListValue l=(ListValue)tag.value();d.writeByte(l.elementType());d.writeInt(l.values().size());for(Tag t:l.values())writePayload(d,l.elementType(),t.value());}
            case COMPOUND->{for(var e:((Map<String,Tag>)tag.value()).entrySet())writeTag(d,e.getKey(),e.getValue());d.writeByte(END);}
            case BYTE_ARRAY->{byte[] a=(byte[])tag.value();d.writeInt(a.length);d.write(a);}
            case INT_ARRAY->{int[] a=(int[])tag.value();d.writeInt(a.length);for(int v:a)d.writeInt(v);}
            case LONG_ARRAY->{long[] a=(long[])tag.value();d.writeInt(a.length);for(long v:a)d.writeLong(v);}
            default->throw new IOException("Unsupported NBT type "+tag.type());
        }
    }

    private static void writePayload(DataOutputStream d,byte type,Object v)throws IOException{
        switch(type){
            case BYTE->d.writeByte((Byte)v); case SHORT->d.writeShort((Short)v); case INT->d.writeInt((Integer)v);
            case LONG->d.writeLong((Long)v); case FLOAT->d.writeFloat((Float)v); case DOUBLE->d.writeDouble((Double)v);
            case STRING->d.writeUTF((String)v); case COMPOUND->{for(var e:((Map<String,Tag>)v).entrySet())writeTag(d,e.getKey(),e.getValue());d.writeByte(END);}
            default->throw new IOException("List payload type "+type+" not supported");
        }
    }

    private static Tag readTag(DataInputStream d)throws IOException{
        byte type=d.readByte(); if(type==END)return new Tag(END,null);
        d.readUTF(); return new Tag(type,readPayload(d,type));
    }

    private static Object readPayload(DataInputStream d,byte type)throws IOException{
        return switch(type){
            case BYTE->d.readByte(); case SHORT->d.readShort(); case INT->d.readInt(); case LONG->d.readLong();
            case FLOAT->d.readFloat(); case DOUBLE->d.readDouble(); case STRING->d.readUTF();
            case LIST->{byte t=d.readByte();int n=d.readInt();List<Tag> list=new ArrayList<>(n);for(int i=0;i<n;i++)list.add(new Tag(t,readPayload(d,t)));yield new ListValue(t,list);}
            case COMPOUND->{Map<String,Tag> m=new LinkedHashMap<>();while(true){byte t=d.readByte();if(t==END)break;String n=d.readUTF();m.put(n,new Tag(t,readPayload(d,t)));}yield Map.copyOf(m);}
            case BYTE_ARRAY->{int n=d.readInt();yield d.readNBytes(n);} 
            case INT_ARRAY->{int n=d.readInt();int[] a=new int[n];for(int i=0;i<n;i++)a[i]=d.readInt();yield a;}
            case LONG_ARRAY->{int n=d.readInt();long[] a=new long[n];for(int i=0;i<n;i++)a[i]=d.readLong();yield a;}
            default->throw new IOException("Unsupported NBT type "+type);
        };
    }
}
