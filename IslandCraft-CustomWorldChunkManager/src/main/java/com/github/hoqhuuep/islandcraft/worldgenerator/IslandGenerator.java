package com.github.hoqhuuep.islandcraft.worldgenerator;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.bukkit.util.noise.SimplexOctaveGenerator;

import com.github.hoqhuuep.islandcraft.mosaic.Poisson;
import com.github.hoqhuuep.islandcraft.mosaic.Site;

public class IslandGenerator {
    private static final double minDistance = 8;
    private static final double NOISE = 2.7;
    private static final double CIRCLE = 2;
    private static final double SQUARE = 0;
    private static final double THRESHOLD = 2;

    public int[] generate(int xSize, int zSize, long islandSeed) {
        final Poisson poisson = new Poisson(xSize, zSize, minDistance);
        final List<Site> sites = poisson.generate(new Random(islandSeed));

        final SimplexOctaveGenerator simplexOctaveGenerator = new SimplexOctaveGenerator(islandSeed, 2);
        final BiomeSelection biomeSelection = BiomeSelection.select(islandSeed);

        // Find borders
        final Queue<Site> floodFillOcean = new LinkedList<Site>();
        for (final Site p : sites) {
            if (p.x < minDistance * 2 || p.x >= xSize - minDistance * 2 || p.z < minDistance * 2 || p.z >= zSize - minDistance * 2) {
                p.border = true;
                p.ocean = true;
                floodFillOcean.add(p);
            } else {
                final double dx = (double) (p.x - (xSize / 2)) / (double) (xSize / 2);
                final double dz = (double) (p.z - (zSize / 2)) / (double) (zSize / 2);
                p.mountain = (simplexOctaveGenerator.noise(dx, dz, 2, 0.5, true) / 2 + 0.5) * 3 - CIRCLE * circle(dx, dz) > 1;
            }
        }

        final Queue<Site> coastQueue = new LinkedList<Site>();

        // Find oceans and coasts
        while (!floodFillOcean.isEmpty()) {
            final Site polygon = floodFillOcean.remove();
            for (final Site q : polygon.neighbors) {
                final double dx = (double) (q.x - (xSize / 2)) / (double) (xSize / 2);
                final double dz = (double) (q.z - (zSize / 2)) / (double) (zSize / 2);
                if (NOISE * (simplexOctaveGenerator.noise(dx, dz, 2, 0.5, true) / 2 + 0.5) + CIRCLE * circle(dx, dz) + SQUARE * square(dx, dz) > THRESHOLD) {
                    if (!q.ocean) {
                        q.ocean = true;
                        floodFillOcean.add(q);
                    }
                } else {
                    q.coast = true;
                    coastQueue.add(q);
                }
            }
        }

        // Remove derpy coasts
        queue2: while (!coastQueue.isEmpty()) {
            final Site polygon = coastQueue.remove();
            for (final Site q : polygon.neighbors) {
                if (!q.ocean && !q.coast) {
                    continue queue2;
                }
            }
            polygon.ocean = true;
        }

        // Create blank image
        final BufferedImage image = new BufferedImage(xSize, zSize, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setBackground(new Color(biomeSelection.ocean, true));
        graphics.clearRect(0, 0, xSize, zSize);

        // Render island
        for (Site p : sites) {
            if (p.ocean) {
                continue;
            } else if (p.coast) {
                graphics.setColor(new Color(biomeSelection.beach, true));
            } else if (p.mountain) {
                graphics.setColor(new Color(biomeSelection.hills, true));
            } else {
                graphics.setColor(new Color(biomeSelection.normal, true));
            }
            graphics.fillPolygon(p.polygon);
            graphics.drawPolygon(p.polygon);
        }

        // Save result
        graphics.dispose();
        final int[] result = new int[xSize * zSize];
        for (int i = 0; i < result.length; ++i) {
            final int x = i % xSize;
            final int z = i / xSize;
            result[i] = image.getRGB(x, z);
        }
        return result;
    }

    public static double circle(final double dx, final double dz) {
        return (dx * dx + dz * dz) / 2;
    }

    public static double square(final double dx, final double dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }
}