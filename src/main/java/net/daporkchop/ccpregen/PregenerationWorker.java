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

package net.daporkchop.ccpregen;

//import io.github.ezraen.bedrockheadless.*;

import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.WorldWorkerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.stream.DoubleStream;

import static java.lang.Long.parseUnsignedLong;
import static net.daporkchop.ccpregen.PregenState.*;

/**
 * @author DaPorkchop_
 */
public class PregenerationWorker implements WorldWorkerManager.IWorker {
    private static final Method CUBEPROVIDERSERVER_TRYUNLOADCUBE;
    private static final Method CUBEPROVIDERSERVER_CUBESITERATOR;
    private static final Field CUBEPROVIDERSERVER_CUBEMAP;
    private static final Object[] SINGLETON_ARRAY = new Object[1];

    static {
        try {
            CUBEPROVIDERSERVER_TRYUNLOADCUBE = CubeProviderServer.class.getDeclaredMethod("tryUnloadCube", Cube.class);
            CUBEPROVIDERSERVER_TRYUNLOADCUBE.setAccessible(true);

            CUBEPROVIDERSERVER_CUBESITERATOR = CubeProviderServer.class.getDeclaredMethod("cubesIterator");
            CUBEPROVIDERSERVER_CUBESITERATOR.setAccessible(true);

            CUBEPROVIDERSERVER_CUBEMAP = CubeProviderServer.class.getDeclaredField("cubeMap");
            CUBEPROVIDERSERVER_CUBEMAP.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final ICommandSender sender;
    private long lastMsg = System.currentTimeMillis();
    private final double[] speeds = new double[10];
    private int gennedSinceLastNotification = 0;
    private WorldServer world;
    private boolean keepingLoaded;

    public PregenerationWorker(ICommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean hasWork() {
        return active && parseUnsignedLong(generated) < parseUnsignedLong(total);
    }

    @Override
    public boolean doWork() {
        if (active) {
            if (this.world == null) {
                WorldServer world = DimensionManager.getWorld(dim);
                if (world == null) {
                    DimensionManager.initDimension(dim);
                    world = DimensionManager.getWorld(dim);
                    if (world == null) {
                        this.sender.sendMessage(new TextComponentString("Unable to load dimension " + dim));
                        active = false;
                        return false;
                    }
                }
                this.world = world;
                this.keepingLoaded = DimensionManager.keepDimensionLoaded(dim, true);
            }

            ICubeProviderInternal.Server provider = (ICubeProviderInternal.Server) ((ICubicWorldServer) this.world).getCubeCache();
            int saveQueueSize = provider.getCubeIO().getPendingCubeCount();

            if (this.lastMsg + PregenConfig.notificationInterval < System.currentTimeMillis()) {
                System.arraycopy(this.speeds, 0, this.speeds, 1, this.speeds.length - 1);
                this.speeds[0] = this.gennedSinceLastNotification * 1000.0d / (double) (System.currentTimeMillis() - this.lastMsg);

                this.sender.sendMessage(new TextComponentString(String.format(
                        "Generated %s/%s cubes (%.1f cubes/s), position: (%d, %d, %d), save queue: %d",
                        generated, total, DoubleStream.of(this.speeds).sum() / this.speeds.length, x << 4, y << 4, z << 4, saveQueueSize
                )));

                this.gennedSinceLastNotification = 0;
                this.lastMsg = System.currentTimeMillis();
            }
            if (saveQueueSize > PregenConfig.maxSaveQueueSize) {
                return false;
            }

            if (!paused && this.hasWork()) {
                //generate the chunk at the current position
                Cube cube = (Cube) ((ICubeProviderServer) provider).getCube(x, y, z, PregenConfig.requirement);

                provider.getCubeIO().saveCube(cube);
                if (PregenConfig.immediateCubeUnload) {
                    try {
                        SINGLETON_ARRAY[0] = cube;
                        if ((boolean) CUBEPROVIDERSERVER_TRYUNLOADCUBE.invoke(provider, SINGLETON_ARRAY)) {
                            ((XYZMap<Cube>) CUBEPROVIDERSERVER_CUBEMAP.get(provider)).remove(cube);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        SINGLETON_ARRAY[0] = null;
                    }
                }
                if (parseUnsignedLong(generated) % PregenConfig.unloadCubesInterval == 0L) {
                    if (PregenConfig.unloadColumns) {
                        ((ICubicWorldServer) this.world).unloadOldCubes();
                    } else {
                        try {
                            for (Iterator<Cube> itr = (Iterator<Cube>) CUBEPROVIDERSERVER_CUBESITERATOR.invoke(provider); itr.hasNext(); ) {
                                SINGLETON_ARRAY[0] = itr.next();
                                if ((boolean) CUBEPROVIDERSERVER_TRYUNLOADCUBE.invoke(provider, SINGLETON_ARRAY)) {
                                    itr.remove();
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            SINGLETON_ARRAY[0] = null;
                        }
                    }
                }

                order.next();
                this.gennedSinceLastNotification++;

                generated = String.valueOf(parseUnsignedLong(generated) + 1);
                if (parseUnsignedLong(generated) % PregenConfig.saveStateInterval == 0) {
                    persistState();
                }
            }
        }

        boolean hasWork = active && parseUnsignedLong(generated) < parseUnsignedLong(total);
        if (!hasWork) {
            this.sender.sendMessage(new TextComponentString("Generation complete."));

            if (this.world != null && this.keepingLoaded) {
                DimensionManager.keepDimensionLoaded(dim, false);
            }
            active = false;
            try {
                Runtime rt = Runtime.getRuntime();
                Process pr = rt.exec("java -jar ./cubicchunksconverter-FINAL-all.jar "+".\\" + sender.getServer().getFolderName()+" .\\world");
                //Hijack.main(new String[]{".\\" + sender.getServer().getFolderName(), ".\\world"});
                new Thread(new Runnable() {
                    public void run() {
                        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                        String line = null;

                        try {
                            while ((line = input.readLine()) != null)
                                System.out.println(line);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                pr.waitFor();
                this.sender.getServer().initiateShutdown();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            persistState();
            ((ICubicWorldServer) this.world).unloadOldCubes();


        }
        return !paused && hasWork;
    }
}
