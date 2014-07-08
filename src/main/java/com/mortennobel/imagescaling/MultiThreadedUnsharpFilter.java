package com.mortennobel.imagescaling;

import java.awt.image.BufferedImage;
import java.awt.image.Kernel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.jhlabs.image.PixelUtils;
import com.jhlabs.image.UnsharpFilter;

public class MultiThreadedUnsharpFilter extends UnsharpFilter {
    private final ExecutorService threadPool;
    private final int numThreads;
    public MultiThreadedUnsharpFilter(ExecutorService threadPool, int numThreads) {
        this.threadPool = threadPool;
        this.numThreads = numThreads;
    }
    public BufferedImage filter( BufferedImage src, BufferedImage dst ) {
        final int width = src.getWidth();
        final int height = src.getHeight();

        if ( dst == null )
            dst = createCompatibleDestImage( src, null );

        final int[] inPixels = new int[width*height];
        final int[] outPixels = new int[width*height];
        src.getRGB( 0, 0, width, height, inPixels, 0, width );

        Future<?>[] results = new Future<?>[numThreads];
        if ( radius > 0 ) {
            for (int i=0;i<numThreads;i++){
                final int finalI = i;
                results[i] = threadPool.submit(new Runnable() {
                    public void run() {
                        convolveAndTransposeThread(finalI, numThreads, kernel, inPixels, outPixels, width, height, alpha, CLAMP_EDGES);
                    }
                });
            }
            waitForAllThreads(results);
            for (int i=0;i<numThreads;i++){
                final int finalI = i;
                results[i] = threadPool.submit(new Runnable() {
                    public void run() {
                        convolveAndTransposeThread(finalI, numThreads, kernel, outPixels, inPixels, height, width, alpha, CLAMP_EDGES);
                    }
                });
            }
            waitForAllThreads(results);
        }

        src.getRGB( 0, 0, width, height, outPixels, 0, width );

        float a = 4*getAmount();

        for (int i=0;i<numThreads;i++){
            results[i] = threadPool.submit(new UnsharpFilterThread(i, numThreads, height, width, inPixels, outPixels, a));
        }
        waitForAllThreads(results);

        dst.setRGB( 0, 0, width, height, inPixels, 0, width );
        return dst;
    }
    
    public void convolveAndTransposeThread(int start, int delta, Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, boolean alpha, int edgeAction) {
        float[] matrix = kernel.getKernelData( null );
        int cols = kernel.getWidth();
        int cols2 = cols/2;

        for (int y = start; y < height; y+=delta) {
            int index = y;
            int ioffset = y*width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                int moffset = cols2;
                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[moffset+col];

                    if (f != 0) {
                        int ix = x+col;
                        if ( ix < 0 ) {
                            if ( edgeAction == CLAMP_EDGES )
                                ix = 0;
                            else if ( edgeAction == WRAP_EDGES )
                                ix = (x+width) % width;
                        } else if ( ix >= width) {
                            if ( edgeAction == CLAMP_EDGES )
                                ix = width-1;
                            else if ( edgeAction == WRAP_EDGES )
                                ix = (x+width) % width;
                        }
                        int rgb = inPixels[ioffset+ix];
                        a += f * ((rgb >> 24) & 0xff);
                        r += f * ((rgb >> 16) & 0xff);
                        g += f * ((rgb >> 8) & 0xff);
                        b += f * (rgb & 0xff);
                    }
                }
                int ia = alpha ? PixelUtils.clamp((int)(a+0.5)) : 0xff;
                int ir = PixelUtils.clamp((int)(r+0.5));
                int ig = PixelUtils.clamp((int)(g+0.5));
                int ib = PixelUtils.clamp((int)(b+0.5));
                outPixels[index] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                index += height;
            }
        }
    }    
    private void waitForAllThreads(Future<?>[] results) {
        try {
            for (Future<?> f:results){
                f.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public class UnsharpFilterThread implements Runnable {
        int start;
        int delta;
        int height;
        int width;
        int[] inPixels;
        int[] outPixels;
        float a;
        public UnsharpFilterThread(int start, int delta,  int height, int width, int[] inPixels,  int[] outPixels, float a) {
            this.start = start;
            this.delta = delta;
            this.height = height;
            this.width = width;
            this.inPixels = inPixels;
            this.outPixels = outPixels;
            this.a = a;
        }

        public void run() {
            int index = 0;
            for ( int y = start; y < height; y+=delta ) {
                index = y * width;
                for ( int x = 0; x < width; x++ ) {
                    int rgb1 = outPixels[index];
                    int r1 = (rgb1 >> 16) & 0xff;
                    int g1 = (rgb1 >> 8) & 0xff;
                    int b1 = rgb1 & 0xff;

                    int rgb2 = inPixels[index];
                    int r2 = (rgb2 >> 16) & 0xff;
                    int g2 = (rgb2 >> 8) & 0xff;
                    int b2 = rgb2 & 0xff;

                    if ( Math.abs( r1 -  r2 ) >= getThreshold() )
                        r1 = PixelUtils.clamp( (int)((a+1) * (r1-r2) + r2) );
                    if ( Math.abs( g1 -  g2 ) >= getThreshold() )
                        g1 = PixelUtils.clamp( (int)((a+1) * (g1-g2) + g2) );
                    if ( Math.abs( b1 -  b2 ) >= getThreshold() )
                        b1 = PixelUtils.clamp( (int)((a+1) * (b1-b2) + b2) );

                    inPixels[index] = (rgb1 & 0xff000000) | (r1 << 16) | (g1 << 8) | b1;
                    index++;
                }
            }
            
        }
        
    }

}
