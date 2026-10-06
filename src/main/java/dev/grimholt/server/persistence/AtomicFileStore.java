package dev.grimholt.server.persistence;
import java.io.*; import java.nio.file.*; import java.util.Objects;
public final class AtomicFileStore {
 private final Path root;
 public AtomicFileStore(Path root){this.root=Objects.requireNonNull(root).toAbsolutePath().normalize();}
 public Path root(){return root;}
 public void write(String relative,byte[] data)throws IOException{Path target=root.resolve(relative).normalize();if(!target.startsWith(root))throw new SecurityException("Path escapes persistence root");Files.createDirectories(target.getParent());Path tmp=target.resolveSibling(target.getFileName()+".tmp");Files.write(tmp,data,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}}
 public byte[] read(String relative)throws IOException{Path target=root.resolve(relative).normalize();if(!target.startsWith(root))throw new SecurityException("Path escapes persistence root");return Files.readAllBytes(target);}
}