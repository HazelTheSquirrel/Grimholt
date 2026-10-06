package dev.example.hello;
import dev.grimholt.api.*;
public final class HelloPlugin implements GrimholtPlugin {
 private GrimholtPluginContext context;
 public void onLoad(GrimholtPluginContext context){this.context=context;}
 public void onEnable(){context.logger().info("Hello plugin enabled on Grimholt");}
 public void onDisable(){context.logger().info("Hello plugin disabled");}
}
