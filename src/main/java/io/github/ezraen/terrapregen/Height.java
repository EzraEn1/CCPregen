package io.github.ezraen.terrapregen;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.terra121.EarthTerrainProcessor;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

public class Height {

    public int Find(int regionX, int regionZ, ICommandSender sender ){
        //getworld
        World world = sender.getEntityWorld();
        IChunkProvider cp = world.getChunkProvider();


        ICubeGenerator gen = ((CubeProviderServer)cp).getCubeGenerator();

        EarthTerrainProcessor terrain = (EarthTerrainProcessor)gen;

        //convert regions
        int X = 512*regionX;
        int Z = 512*regionZ;
        double[] coord = terrain.projection.toGeo(X,Z);
        double height = terrain.heights.estimateLocal(coord[0],coord[1]);

        int alt = (int) Math.round(height)-40;
        //int alt = (int) height/256; // Swap commenting with line above if increments of 256 are needed

        return alt; // This number is the start of the y-band, will have to offset back in the final world

    }
}