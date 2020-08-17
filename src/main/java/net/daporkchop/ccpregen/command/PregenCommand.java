/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.ccpregen.command;

import net.daporkchop.ccpregen.CCPregen;
import net.daporkchop.ccpregen.PregenState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.server.permission.PermissionAPI;

import java.io.*;


/**
 * @author DaPorkchop_
 */
public class PregenCommand extends CommandBase {


    @Override
    public String getName() {
        return "ccpregen";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ccpregen <regionX> <regionZ>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2)    {
            sender.sendMessage(new TextComponentString(this.getUsage(sender)));
            return;
        }
        Height hg = new Height();
        int X = 512*Integer.parseInt(args[0]);
        int Z = 512*Integer.parseInt(args[1]);
        int String.valueOf(123) = hg.Find(Integer.parseInt(args[0]),Integer.parseInt(args[1]),sender);
        BlockPos min = new BlockPos(X,alt*256,Z);
        BlockPos max = new BlockPos(X+511,(alt*256)+255,Z+511);
        String path = "G:\\test\\pregen\\TerraPreGenerated\\height.txt";
        File Alti = new File(path);
        try {
            if (Alti.createNewFile()){
                System.out.println("File already exist");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Writer wr = null;
        try {
            wr = new FileWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            wr.write(String.valueOf(alt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int dimension = 0;
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ())  {
            sender.sendMessage(new TextComponentString("Min coordinates may not be greater than max coordinates!"));
        } else if (!PregenState.startPregeneration(sender, min, max, dimension))   {
            sender.sendMessage(new TextComponentString("A pregeneration task is already running!"));
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            return PermissionAPI.hasPermission((EntityPlayer) sender, CCPregen.MODID + ".command.ccpregen");
        } else {
            return super.checkPermission(server, sender);
        }
    }
}
