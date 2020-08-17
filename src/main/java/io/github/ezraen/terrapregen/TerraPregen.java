package io.github.ezraen.terrapregen;

import net.daporkchop.ccpregen.CCPregen;
import net.daporkchop.ccpregen.PregenState;
import net.daporkchop.ccpregen.command.PregenCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.permission.PermissionAPI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.createDirectory;

public class TerraPregen {
    public static class TerraPregenCommand extends PregenCommand {
        @Override
        public String getName() {
            return "terrapregen";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/terrapregen <regionX> <regionZ>";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

            if (args.length < 2) {
                sender.sendMessage(new TextComponentString(this.getUsage(sender)));
                return;
            }
            String regionX = args[0];
            String regionZ = args[1];
            Height hg = new Height();
            int X = 512*Integer.parseInt(regionX);
            int Z = 512*Integer.parseInt(regionZ);
            int alt = hg.Find(Integer.parseInt(regionX),Integer.parseInt(regionZ),sender);

            BlockPos min = new BlockPos(X,alt,Z);
            BlockPos max = new BlockPos(X+511,(alt)+255,Z+511);

            //BlockPos min = new BlockPos(X,alt*256,Z); // Swap commenting with lines above if increments of 256 are needed
            //BlockPos max = new BlockPos(X+511,(alt*256)+255,Z+511);


            String path = ".\\world";
            try {
                createDirectory(Paths.get(path));
            } catch (IOException e) {
                e.printStackTrace();
            }

            File Alti = new File(path+"\\height.txt");
            try {
                if (Alti.createNewFile()){
                    System.out.println("File at " +path+ " created");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Writer wr = null;
            try {
                wr = new FileWriter(path+"\\height.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.write(String.valueOf(alt));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int dimension = 0;
            if (!PregenState.startPregeneration(sender, min, max, dimension))   {
                sender.sendMessage(new TextComponentString("A pregeneration task is already running!"));
            }
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            if (sender instanceof EntityPlayer) {
                return PermissionAPI.hasPermission((EntityPlayer) sender, CCPregen.MODID + ".command.terrapregen");
            } else {
                return super.checkPermission(server, sender);
            }
        }
    }
}
