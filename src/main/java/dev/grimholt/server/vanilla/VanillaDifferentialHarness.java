package dev.grimholt.server.vanilla;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Differential harness for a real Mojang reference process. It is intentionally
 * external: Grimholt never embeds or redistributes the proprietary server jar.
 */
public final class VanillaDifferentialHarness {
    public record Result(boolean executed,int exitCode,long durationMs,String stdout,String stderr){}
    private final Path referenceJar;

    public VanillaDifferentialHarness(Path referenceJar){this.referenceJar=Objects.requireNonNull(referenceJar);}

    public Result run(List<String> args,Path workDir,long timeoutSeconds) throws IOException,InterruptedException{
        if(!Files.isRegularFile(referenceJar))throw new FileNotFoundException(referenceJar.toString());
        List<String> command=new ArrayList<>();
        command.add(System.getProperty("java.home")+File.separator+"bin"+File.separator+"java");
        command.add("-jar"); command.add(referenceJar.toString()); command.addAll(args);
        Process p=new ProcessBuilder(command).directory(workDir.toFile()).redirectErrorStream(false).start();
        boolean done=p.waitFor(timeoutSeconds,TimeUnit.SECONDS);
        if(!done){p.destroyForcibly();return new Result(true,-1,timeoutSeconds*1000,p.getInputStream().readAllBytes().toString(),p.getErrorStream().readAllBytes().toString());}
        String out=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        String err=new String(p.getErrorStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        return new Result(true,p.exitValue(),0,out,err);
    }

    public static void requireReference(String property){
        String p=System.getenv(property);
        if(p==null||p.isBlank())throw new IllegalStateException("Set "+property+" to a Minecraft 26.4 Snapshot 3 server jar");
    }
}
