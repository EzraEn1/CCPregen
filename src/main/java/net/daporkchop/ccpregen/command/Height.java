package net.daporkchop.ccpregen.command;

public class Height {

    public int Find(double regionX, double regionZ, ICommandSender sender ){
        //getworld
        World world = sender.getEntityWorld();
        IChunkProvider cp = world.getChunkProvider();

        if(!(cp instanceof CubeProviderServer)) {
            throw new CommandException("terra121.error.notcc", new Object[0]);
        }

        ICubeGenerator gen = ((CubeProviderServer)cp).getCubeGenerator();

        if(!(gen instanceof EarthTerrainProcessor)) {
            throw new CommandException("terra121.error.notterra", new Object[0]);
        }

        EarthTerrainProcessor terrain = (EarthTerrainProcessor)gen;

        //convert regions
        int X = 512*regionX;
        int Z = 512*regionZ;
        double coord[] = terrain.projection.toGeo(X,Z);
        double height = terrain.heights.estimateLocal(coord[0],coord[1]);
        double alt = (int) height/256;
        return alt;

    }
}